package com.sap.sse.replication;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sap.sse.operationaltransformation.Operation;
import com.sap.sse.operationaltransformation.OperationWithTransformationSupport;
import com.sap.sse.replication.interfaces.impl.AbstractReplicableWithObjectInputStream;
import com.sap.sse.util.ObjectInputStreamResolvingAgainstCache;
import com.sap.sse.util.ThreadLocalTransporter;

/**
 * Represents a replicable part of an application. Such a replicable part is usually holder of application state and a
 * transaction / operation processing unit. Processing {@link Operation operations} generally happens by
 * {@link Operation#applyTo(Object) applying} operations to this replicable. As such, this replicable plays the role of
 * the <code>toState</code> argument in the {@link Operation#applyTo(Object)} method calls.
 * <p>
 * 
 * In addition to being the operation processor, a replicable must be able to serialize its state for the initial load
 * of a replica, and it must be able to initialize its state from this serialized form. In doing so it is important to
 * not include state that is controlled by another replicable. References to such objects need to be <em>transient</em>
 * which means they are not serialized together with the initial load of this replica. If such references need to be
 * re-established after de-serialization then they need to represented by some key which can later be looked up in the
 * other replicable which can be discovered through the OSGi service registry on the receiving end.
 * <p>
 * 
 * For operation and initial load serialization, a replicable is provided an {@link InputStream} from which to read the
 * operations and the initial load. A typical implementation for reading the stream's contents will use a specialized
 * {@link ObjectInputStream} such as a specialization of {@link ObjectInputStreamResolvingAgainstCache} that ensures
 * that on the receiving end there are no two Java object copies representing the same domain object where this matters.
 * This is particularly important for objects with mutable state that otherwise may run inconsistent when only one of
 * the multiple copies gets modified. Typically, the object input stream uses some sort of cache and overrides the
 * {@link ObjectInputStream#resolveObject} method, and the types whose objects are de-serialized need to co-operate by
 * implementing a marker interface telling the specialized object input stream to use the cache when resolving objects
 * of that type.
 * <p>
 * 
 * Several {@link Replicable} instances can share the same replication service and the same set of replication channels.
 * In order to participate in replication, {@link Replicable} objects need to be registered as OSGi services for the
 * {@link Replicable} interface. The replication service will dynamically discover the {@link Replicable} objects when
 * initial load is requested and when an {@link Operation} is received. The initial load streams as well as each
 * replication operation need to identify the {@link Replicable} they refer to. This identification is added as an OSGi
 * service registry parameter that is used during service discovery.
 * <p>
 * 
 * To implement a new replicable, consider the following steps:
 * <ul>
 * <li>Consider using {@link AbstractReplicableWithObjectInputStream} as an abstract base class for your
 * {@link Replicable} implementation.</li>
 * <li>Implement the necessary methods, in particular ensuring that your
 * {@link ReplicableWithObjectInputStream#createObjectInputStreamResolvingAgainstCache(InputStream, Map)} implementation
 * returns an object whose class is loaded by your replicable's class loader, such as an in-place anonymous inner class
 * instantiation of the {@link ObjectInputStreamResolvingAgainstCache} class.</li>
 * <li>In your bundle's {@link BundleActivator activator} create your replicable instance and
 * {@link BundleContext#registerService(Class, Object, java.util.Dictionary) register it with the OSGi service registry}
 * under the {@link Replicable} service interface, like this:
 * <pre>
 *     final Dictionary&lt;String, String&gt; replicableServiceProperties = new Hashtable&lt;&gt;();
 *     replicableServiceProperties.put(Replicable.OSGi_Service_Registry_ID_Property_Name, service.getId().toString());
 *     registrations.add(context.registerService(Replicable.class.getName(), service, replicableServiceProperties));</pre></li>
 * <li>Add your replicable's fully-qualified class name to the {@code env-default-rules.sh} file where the
 * {@code REPLICATE_ON_START} variable has its default value defined.</li>
 * </ul>
 * 
 * The {@link #getId() ID} must be different from {@link ReplicationReceiver#VERSION_INDICATOR}.<p>
 * 
 * @param <S>
 *            the type of state to which the operations are applied; usually this will be set to the implementing
 *            subclass
 * @param <O>
 *            type of operation that the replicable accepts; it is good practice to declare a dedicated interface for
 *            the operation type of each {@link Replicable} implementation class. This helps to avoid accidentally
 *            passing an operation intended for a different replicable service. Make sure to implement operation types
 *            as dedicated classes and withstand the temptation to use lambdas. While lambdas may work at first and look
 *            nice and compact, two major problems emerge (see also
 *            <a href="https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5197">bug 5197</a>): you may
 *            unintentionally reference comprehensive contextual state in your lambda which then needs to get serialized
 *            with it; and by using {@link Serializable} lambdas, serialization compatibility is brittle because it
 *            depends on the lambda's position in its type, and hence code insertions, removals or changes in method
 *            ordering can already cause unnecessary incompatibilities. Specifically for {@link Replicable} services
 *            shared by many instances this should be avoided at any cost because incompatible changes then require
 *            upgrading the entire landscape.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface Replicable<S, O extends OperationWithResult<S, ?>>
extends OperationsToMasterSender<S, O>, Replicator<S, O> {
    static final Logger logger = Logger.getLogger(Replicable.class.getName());
    
    /**
     * The name of the property to use in the <code>properties</code> dictionary in a call to
     * {@link BundleContext#registerService(Class, Object, java.util.Dictionary)} when registering a {@link Replicable}.
     * The value to provide for this property is the {@link Object#toString()} serialization of the {@link #getId()}
     * result for the {@link Replicable} object.
     */
    final String OSGi_Service_Registry_ID_Property_Name = "ID";
    
    /**
     * If this object is not a replica, executes the <code>operation</code>. By
     * {@link OperationExecutionListener#executed(OperationWithTransformationSupport) notifying} all registered
     * operation execution listeners about the execution of the operation, the <code>operation</code> will in particular
     * be replicated to all replicas registered, unless its
     * {@link OperationWithResult#isRequiresExplicitTransitiveReplication()} method returns {@code false}.
     * <p>
     * 
     * If this object is a replica, the operation will be executed locally and will then be forwarded to the
     * master server for execution from where it is expected to replicate to all replicas including this object where
     * the {@link #applyReplicated(OperationWithResult)} method will identify it as the one that originated here and
     * ignore it (see also {@link OperationWithResultWithIdWrapper}).
     * <p>
     * 
     * To determine whether this {@link Replicable} is a replica, this method uses the
     * {@link ReplicationService#getReplicatingFromMaster()} method which also provides the master server's connectivity
     * information required to forward the <code>operation</code>.
     * 
     * @return the result of applying the operation locally; results of applying the operation on other nodes of the
     *        replica set are not considered
     */
    <T> T apply(O operation);

    /**
     * Executes an operation received from another (usually "master") server where this object lives on a replica. The
     * <code>operation</code>'s effects also need to be replicated to any replica of this service known, so this method
     * {@link OperationExecutionListener#executed(OperationWithTransformationSupport) notifies} all registered operation
     * execution listeners about the execution of the operation, unless its
     * {@link OperationWithResult#isRequiresExplicitTransitiveReplication()} method returns {@code false}.
     * <p>
     * 
     * One important difference to {@link #apply(OperationWithResult)} is that the operation will be applied immediately
     * in any case whereas {@link #apply(OperationWithResult)} will check first if this is a replica and in that case
     * forward the operation to the master for first execution instead of initiating the execution on the replica.
     * 
     * @return the result of applying the operation locally; results of applying the operation on other nodes of the
     *         replica set are not considered
     */
    <T> T applyReplicated(O operation);
    
    void startedReplicatingFrom(ReplicationMasterDescriptor master);
    
    void stoppedReplicatingFrom(ReplicationMasterDescriptor master);

    /**
     * An operation execution listener must be able to process notifications of operations being executed that have
     * type <code>S</code> or any more specific type. The listener can achieve this also by accepting any type more general than
     * <code>S</code>.
     */
    void addOperationExecutionListener(OperationExecutionListener<S> listener);

    void removeOperationExecutionListener(OperationExecutionListener<S> listener);

    /**
     * Before {@link #initiallyFillFrom(ObjectInputStream) initially loading a replica's state from a master instance},
     * the replica's old state needs to be "detached". This method clears all top-level in-memory data structures and stops all
     * tracking currently going on. It may choose to leave any persistent content unchanged as the persistence layer is in
     * an undefined state on a replica anyhow. This seems like the safer bet, particularly in case of an accidental
     * mis-configuration of the replica's DB connection parameters which may lead to an inadvertent overwriting of the
     * master's DB contents.<p>
     * 
     * The reason this operation needs to be callable separate from {@link #initiallyFillFrom(ObjectInputStream)} is that
     * it needs to happen before subscribing to the operation feed received from the master instance through the message bus
     * which in turn needs to happen before receiving the initial load.
     */
    void clearReplicaState() throws MalformedURLException, IOException, InterruptedException;

    /**
     * Produces an object input stream that can choose to resolve objects against a cache so that duplicate instances
     * are avoided.
     */
    ObjectInputStream createObjectInputStreamResolvingAgainstCache(InputStream is, Map<String, Class<?>> classLoaderCache) throws IOException;

    /**
     * Dual, reading operation for {@link #serializeForInitialReplication(OutputStream)}. In other words, when this
     * operation returns, this service instance is in a state "equivalent" to that of the service instance that produced
     * the stream contents in its {@link #serializeForInitialReplication(OutputStream)}. "Equivalent" here means that a
     * replica will have equal sets of objects serialized in the initial load but will usually not have any transient
     * processes replicated that are responsible for building and maintaining the state on the master side because it
     * relies on these elements to be sent through the replication channel.
     * <p>
     * 
     * <b>Caution:</b> All relevant contents of this service instance needs to be cleared before by a call to
     * {@link #clearReplicaState()}. It will be replaced by the stream contents.
     */
    void initiallyFillFrom(InputStream is) throws IOException, ClassNotFoundException, InterruptedException;

    /**
     * Produces a one-shot serializable copy of those elements required for replication into <code>os</code> by wrapping
     * that stream by an {@link ObjectOutputStream} so that afterwards the {@link OperationWithResult}s can be
     * {@link #apply(OperationWithResult) applied} to maintain consistency with the master copy of the service. The dual
     * operation is {@link #initiallyFillFrom}.
     */
    void serializeForInitialReplication(OutputStream os) throws IOException;

    /**
     * The class loader to use for de-serializing objects. By default, this object's class's class loader is used.
     */
    default ClassLoader getDeserializationClassLoader() {
        return getClass().getClassLoader();
    }
    
    /**
     * Implementation of {@link #readOperation(InputStream, Map)}, using the {@link ObjectInputStream} created by
     * {@link #createObjectInputStreamResolvingAgainstCache(InputStream, Map)}. Before actually reading an operation
     * object, the current thread's context class loader is set to the {@link #getDeserializationClassLoader() class
     * loader for de-serialization} and restored to its previous value in the {@code finally} clause.
     */
    @SuppressWarnings("unchecked")
    default O readOperationFromObjectInputStream(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ClassLoader oldContextClassloader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getDeserializationClassLoader());
        try {
            return (O) ois.readObject();
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextClassloader);
        }
    }

    /**
     * From an input stream, reads an operation that can be {@link #apply(OperationWithResult) applied} to this object.
     * Separating reading and applying gives clients an opportunity to queue operations, e.g., in order to wait until
     * receiving and {@link #initiallyFillFrom(InputStream) filling} the initial load has completed.
     */
    O readOperation(InputStream inputStream, Map<String, Class<?>> classLoaderCache) throws IOException, ClassNotFoundException;

    /**
     * Checks if {@link #hasSentOperationToMaster(OperationWithResultWithIdWrapper) the operation was previously
     * sent to the master}. If so, the operation is ignored because it has been applied before to this replica.
     * Otherwise, it is locally applied and replicated, using a call to {@link #applyReplicated(OperationWithResult)}. 
     */
    default void applyReceivedReplicated(O operation) {
        if (!hasSentOperationToMaster(operation)) {
            assert !isCurrentlyFillingFromInitialLoadOrApplyingOperationReceivedFromMaster();
            try {
                setCurrentlyApplyingOperationReceivedFromMaster(true);
                applyReplicated(operation);
            } finally {
                setCurrentlyApplyingOperationReceivedFromMaster(false);
            }
        } else {
            logger.fine("Ignoring operation "+operation+" received back from master after having sent it there for execution and replication earlier");
        }
    }

    /**
     * Responds with what has been passed to the last invocation to {@link #setCurrentlyFillingFromInitialLoad(boolean)}
     * and {@link #setCurrentlyFillingFromInitialLoad(boolean)}, respectively, in the calling thread; the
     * default is <code>false</code>. This is required in order to not replicate operations triggered on the replica
     * while receiving the initial load back to the master.
     */
    default boolean isCurrentlyFillingFromInitialLoadOrApplyingOperationReceivedFromMaster() {
        return isCurrentlyFillingFromInitialLoad() || isCurrentlyApplyingOperationReceivedFromMaster();
    }
    
    /**
     * Responds with what has been passed to the last invocation to
     * {@link #setCurrentlyApplyingOperationReceivedFromMaster(boolean)} in the calling thread;
     * the default is <code>false</code>. This is required in order to not replicate operations triggered on the replica
     * while receiving the initial load back to the master.
     */
    boolean isCurrentlyFillingFromInitialLoad();
    
    /**
     * Responds with what has been passed to the last invocation to
     * {@link #setCurrentlyFillingFromInitialLoad(boolean)} in the calling thread;
     * the default is <code>false</code>. This is required in order to not replicate operations triggered on the replica
     * while receiving the initial load back to the master.
     */
    boolean isCurrentlyApplyingOperationReceivedFromMaster();
    
    /**
     * {@link #isCurrentlyFillingFromInitialLoad} responds with what has been passed to the last invocation to this
     * method in the calling thread; the default is <code>false</code>. This is required in order to not replicate
     * operations triggered on the replica while receiving the initial load back to the master.
     */
    void setCurrentlyFillingFromInitialLoad(boolean b);

    /**
     * {@link #isCurrentlyFillingFromInitialLoad} responds with what has been passed to the last invocation to this
     * method in the calling thread; the default is <code>false</code>. This is required in order to not replicate
     * operations triggered on the replica while receiving the initial load back to the master.
     */
    void setCurrentlyApplyingOperationReceivedFromMaster(boolean b);

    default ThreadLocalTransporter getThreadLocalTransporterForCurrentlyFillingFromInitialLoadOrApplyingOperationReceivedFromMaster() {
        return new ThreadLocalTransporter() {
            private boolean currentlyFillingFromInitialLoad;
            private boolean currentlyFillingFromInitialLoadAtBeginningOfTask;
            private boolean currentlyApplyingOperationReceivedFromMaster;
            private boolean currentlyApplyingOperationReceivedFromMasterAtBeginningOfTask;
            
            @Override
            public void rememberThreadLocalStates() {
                currentlyFillingFromInitialLoad = isCurrentlyFillingFromInitialLoad();
                currentlyApplyingOperationReceivedFromMasterAtBeginningOfTask = isCurrentlyApplyingOperationReceivedFromMaster();
            }

            @Override
            public void pushThreadLocalStates() {
                currentlyFillingFromInitialLoadAtBeginningOfTask = isCurrentlyFillingFromInitialLoad();
                setCurrentlyFillingFromInitialLoad(currentlyFillingFromInitialLoad);
                currentlyApplyingOperationReceivedFromMasterAtBeginningOfTask = isCurrentlyFillingFromInitialLoad();
                setCurrentlyFillingFromInitialLoad(currentlyApplyingOperationReceivedFromMaster);
            }

            @Override
            public void popThreadLocalStates() {
                setCurrentlyFillingFromInitialLoad(currentlyFillingFromInitialLoadAtBeginningOfTask);
                setCurrentlyApplyingOperationReceivedFromMaster(currentlyApplyingOperationReceivedFromMasterAtBeginningOfTask);
            }
        };
    }
    
    /**
     * Injects a service into this replicable that this instance of {@link OperationsToMasterSendingQueue} can use
     * as a delegate to implement the {@link OperationsToMasterSendingQueue#scheduleForSending(OperationWithResult, OperationsToMasterSender)}
     * method. This replicable may not be able to enqueue operations for re-trying sending to master after
     * an error occurred unless this method has been used to announce the service.
     */
    void setUnsentOperationToMasterSender(OperationsToMasterSendingQueue service);
}
