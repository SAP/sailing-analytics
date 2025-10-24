package com.sap.sse.replication;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sse.common.WithID;
import com.sap.sse.operationaltransformation.Operation;

public interface ReplicableWithObjectInputStream<S, O extends OperationWithResult<S, ?>> extends Replicable<S, O> {
    static final Logger logger = Logger.getLogger(ReplicableWithObjectInputStream.class.getName());
    
    /**
     * When an operation is applied that is an {@link OperationWithResultWithIdWrapper} then its ID is stored
     * in this thread local and removed when the local execution of that operation has finished. When during the
     * execution of the operation another operation is fired to the replicas registered, it will be wrapped
     * by an {@link OperationWithResultWithIdWrapper} that has this ID. This will let the replica that originally
     * passed this operation to this master recognize that it is receiving the operation it triggered itself, so
     * it can safely be ignored on that replica. This procedure is important for operations that have
     * {@link OperationWithResult#isRequiresExplicitTransitiveReplication()} return <code>false</code> because they
     * implicitly replicate their effects while being executed.
     */
    static final ThreadLocal<Serializable> idOfOperationBeingExecuted = new ThreadLocal<Serializable>();
    
    static final AtomicInteger operationCounter = new AtomicInteger(0);
    
    /**
     * Implementation of {@link #initiallyFillFrom(InputStream)} which receives an {@link ObjectInputStream} instead of
     * an {@link InputStream}. The {@link ObjectInputStream} is expected to have been produced by
     * {@link #createObjectInputStreamResolvingAgainstCache(InputStream, Map)}.
     */
    void initiallyFillFromInternal(ObjectInputStream is) throws IOException, ClassNotFoundException, InterruptedException;

    /**
     * Implementation of {@link #serializeForInitialReplication(OutputStream)}, using an {@link ObjectOutputStream}.
     */
    void serializeForInitialReplicationInternal(ObjectOutputStream objectOutputStream) throws IOException;

    @Override
    default void initiallyFillFrom(InputStream is) throws IOException, ClassNotFoundException, InterruptedException {
        assert !isCurrentlyFillingFromInitialLoadOrApplyingOperationReceivedFromMaster(); // no nested receiving of initial load
        setCurrentlyFillingFromInitialLoad(true);
        try {
            final ObjectInputStream objectInputStream = createObjectInputStreamResolvingAgainstCache(is, /* classLoaderCache */ new HashMap<>());
            ClassLoader oldContextClassloader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(getDeserializationClassLoader());
            try {
                initiallyFillFromInternal(objectInputStream);
            } finally {
                Thread.currentThread().setContextClassLoader(oldContextClassloader);
            }
        } finally {
            setCurrentlyFillingFromInitialLoad(false);
        }
    }
    
    /**
     * Wraps <code>os</code> by an {@link ObjectOutputStream} and invokes
     * {@link #serializeForInitialReplicationInternal(ObjectOutputStream)}.
     */
    @Override
    default void serializeForInitialReplication(OutputStream os) throws IOException {
        final ObjectOutputStream objectOutputStream = new ObjectOutputStream(os);
        serializeForInitialReplicationInternal(objectOutputStream);
        objectOutputStream.flush();
    }
    
    @Override
    default O readOperation(InputStream inputStream, Map<String, Class<?>> classLoaderCache) throws IOException, ClassNotFoundException {
        return readOperationFromObjectInputStream(createObjectInputStreamResolvingAgainstCache(inputStream, classLoaderCache));
    }

    @Override
    default void writeOperation(OperationWithResult<?, ?> operation, OutputStream outputStream, boolean closeStream) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        oos.writeObject(operation);
        oos.flush();
        if (closeStream) {
            oos.close();
        }
    }
    
    default <T> void replicate(O operation) {
        // if this is a replica and this replicable is not currently in the process of handling replication data (either an operation
        // or the initial load) coming from the master, send the operation back to the master
        if (getMasterDescriptor() != null && !isCurrentlyFillingFromInitialLoadOrApplyingOperationReceivedFromMaster()) {
            scheduleForSending(operation, this);
        }
        replicateReplicated(operation); // this anticipates receiving the operation back from master; then, the operation will be ignored;
        // see also addOperationSentToMasterForReplication
    }

    /**
     * Replicates <code>operation</code> to any replica registered. This is different from {@link #replicate(OperationWithResult)}
     * which would also send the operation to a master if this is a replica. If on the current call stack there is an operation
     * being {@link #apply(OperationWithResult) applied} that was received from a replica (indicated by the operation
     * being a {@link WithID} instance) and the operation to be sent to the replicas does not {@link WithID carry an ID},
     * a new {@link OperationWithResultWithIdWrapper ID decorator} is wrapped around the <code>operation</code> with the ID
     * of the call stack's operation currently being {@link #apply(OperationWithResult) applied}. This will tell the replicas
     * that <code>operation</code> is just replicating the effects of another operation, and the replica where the original
     * operation was initiated can therefore safely ignore <code>operation</code> when receiving it.
     */
    default <T> void replicateReplicated(O operation) {
        @SuppressWarnings("unchecked")
        final OperationWithResult<S, T> owr = (OperationWithResult<S, T>) operation;
        final Serializable idOfCurrentlyExecutingOperation = idOfOperationBeingExecuted.get();
        final OperationWithResult<S, T> operationToNotify;
        if (!(owr instanceof OperationWithResultWithIdWrapper<?, ?>) && idOfCurrentlyExecutingOperation != null) {
            operationToNotify = new OperationWithResultWithIdWrapper<S, T>(owr, idOfCurrentlyExecutingOperation);
        } else {
            operationToNotify = owr;
        }
        for (OperationExecutionListener<S> listener : getOperationExecutionListeners()) {
            final int operationCount = operationCounter.incrementAndGet();
            logger.fine(()->""+operationCount+": Replicating "+operation);
            try {
                listener.executed(operationToNotify);
            } catch (Exception e) {
                // don't risk the master's operation only because replication to a listener/replica doesn't work
                logger.severe("Error replicating operation " + operationToNotify + " to replication listener " + listener);
                logger.log(Level.SEVERE, "replicate", e);
            }
        }
    }

    Iterable<OperationExecutionListener<S>> getOperationExecutionListeners();

    /**
     * Checks whether this replicable is a replica. If yes, the operation is executed locally and sent to the master
     * server for execution. If sending the operation fails with an {@link IOException}, the operation will be enqueued
     * for a later re-try using the
     * {@link #scheduleForSending(OperationWithResultWithIdWrapper, OperationsToMasterSender)} method. Note that this may
     * also happen while in a resend attempt. Otherwise, {@link #applyReplicated(OperationWithResult)} is invoked which
     * executes and replicates the operation immediately.
     */
    default <T> T apply(O operation) {
        boolean needToRemoveThreadLocal = false;
        if (operation instanceof OperationWithResultWithIdWrapper<?, ?>) {
            idOfOperationBeingExecuted.set(((OperationWithResultWithIdWrapper<?, ?>) operation).getId());
            needToRemoveThreadLocal = true;
        }
        final T result = applyReplicated(operation);
        ReplicationMasterDescriptor masterDescriptor = getMasterDescriptor();
        if (masterDescriptor != null) {
            scheduleForSending(operation, this);
        }
        if (needToRemoveThreadLocal) {
            idOfOperationBeingExecuted.remove();
        }
        return result;
    }

    /**
     * The operation is executed by immediately {@link Operation#internalApplyTo(Object) applying} it to this service
     * object. It is then replicated to all replicas if and only if the operation is marked as
     * {@link OperationWithResult#isRequiresExplicitTransitiveReplication()}.
     * 
     * @return the result of applying the operation locally; results of applying the operation on other nodes of the
     *         replica set are not considered
     * @see {@link #replicate(RacingEventServiceOperation)}
     */
    default <T> T applyReplicated(O operation) {
        @SuppressWarnings("unchecked")
        OperationWithResult<S, T> reso = (OperationWithResult<S, T>) operation;
        try {
            setCurrentlyApplyingOperationReceivedFromMaster(true);
            @SuppressWarnings("unchecked")
            S replicable = (S) this;
            T result = reso.internalApplyTo(replicable);
            if (operation.isRequiresExplicitTransitiveReplication()) {
                replicateReplicated(operation);
            }
            return result;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "apply", e);
            throw new RuntimeException(e);
        } finally {
            setCurrentlyApplyingOperationReceivedFromMaster(false);
        }
    }
}
