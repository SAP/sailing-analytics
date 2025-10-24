package com.sap.sse.replication.impl;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rabbitmq.client.ShutdownSignalException;
import com.sap.sse.ServerInfo;
import com.sap.sse.common.Named;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.i18n.impl.ResourceBundleStringMessagesImpl;
import com.sap.sse.operationaltransformation.Operation;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.replication.OperationWithResult;
import com.sap.sse.replication.Replicable;
import com.sap.sse.replication.ReplicablesProvider;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.replication.ReplicationReceiver;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.util.ThreadPoolUtil;

/**
 * Receives {@link OperationWithResult} objects from a message queue and {@link Replicable#apply(OperationWithResult)
 * applies} them to the {@link Replicable} objects that can be found through the {@link ReplicablesProvider} passed to
 * this replicator at construction, based on the {@link Replicable#getId() replicable's ID} which is used in the stream
 * to prefix the operations.
 * <p>
 * 
 * The corresponding writer for this protocol is implemented by
 * {@link ReplicationServiceImpl#broadcastOperation(OperationWithResult, Replicable)}.<p>
 * 
 * When started in suspended mode, messages received will be turned into {@link OperationWithResult} objects and then
 * queued until {@link #setSuspended(boolean) setSuspended(false)} is invoked which applies all queued operations before
 * applying the ones received later.
 * <p>
 * 
 * The receiver takes care of synchronizing receiving, suspending/resuming and queuing. Waiters are notified whenever
 * the result of {@link #isQueueEmpty} changes.<p>
 * 
 * Clients can {@link Object#wait()} on this object and will be {@link Object#notify() notified} when one of the queues
 * for one {@link Replicable} has been consumed so that it is empty. As new operations may arrive at any time, this is
 * no guarantee for the queue remaining empty; however, it is a possible way to get informed about this interesting change
 * in state, particularly in case it was really the last operation that was received.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class ReplicationReceiverImpl implements ReplicationReceiver, Runnable {
    private final static Logger logger = Logger.getLogger(ReplicationReceiverImpl.class.getName());
    
    private static final String STRING_MESSAGES_BASE_NAME = "stringmessages/Replication_StringMessages";
    
    private static final long CHECK_INTERVAL_MILLIS = 5000; // how long (milliseconds) to pause before checking connection again
    private static final int CHECK_COUNT = Integer.MAX_VALUE; // how long to check, value is CHECK_INTERVAL second steps
    
    /**
     * descriptor of the master server from which this replicator receives messages
     */
    private final ReplicationMasterDescriptor master;
    
    private final ReplicablesProvider replicableProvider;
    
    /**
     * Keys are the {@link Replicable}s' IDs as string; values are the operation queues for the replicable identified by the
     * key.
     */
    private final Map<String, LinkedBlockingQueue<Pair<String, OperationWithResult<?, ?>>>> queueByReplicableIdAsString;
    
    private QueueingConsumer consumer;
    
    /**
     * How many checks have been performed due to a failing connection?
     */
    private int checksPerformed = 0;
    
    /**
     * If the replicator is suspended, messages received are queued.
     */
    private boolean suspended;
    
    private boolean stopped = false;

    /**
     * If permitted by the security manager, this is the <code>_queue</code> field accessor for the {@link QueueingConsumer}
     * class, enabling an inspection of the message queueing system's queue size of unprocessed messages.
     */
    private Field _queue;
    
    private int operationCounter;
    
    /**
     * Keeps the maximum {@link Envelope#getDeliveryTag() delivery tags} received on the {@link Channel} to which the
     * {@link #consumer} is attached so far. This way, messages can be acknowledged after they have been delivered to
     * the {@link #run()} method in the background. The background task that is actually sending the acknowledgement
     * to the RabbitMQ server must {@code synchronize} on this {@link AtomicLong} before reading its value and until
     * having cleared the {@link #acknowledgeTaskScheduled} flag. Likewise, the {@link #acknowledgeAsync(Channel, Delivery)}
     * method must synchronize on this {@link AtomicLong} before setting its value to a new delivery tag and until after
     * having checked the {@link #acknowledgeTaskScheduled} flag and possible having scheduled a new task for sending the
     * acknowledgement.
     */
    private final AtomicLong maximumDeliveryTagSoFar;
    
    long maximumDeliveryTagAcknowledged;
    
    /**
     * @see #maximumDeliveryTagSoFar
     * @see #acknowledgeAsync(Channel, Delivery)
     */
    private final AtomicInteger acknowledgeTaskScheduledButNotYetStarted;

    /**
     * The monitor of this object is acquired by the tasks reading the {@link #maximumDeliveryTagSoFar} value
     * and sending the acknowledgement for that delivery tag. This way, although an executor is used with these
     * fine-grained acknowledgement tasks, these tasks cannot run concurrently. The tasks record the last delivery tag
     * acknowledged so that if a 
     */
    private static final Object acknowledgementTaskLock = new Object();
    
    /**
     * Used for the parallel execution of operations that don't
     * {@link RacingEventServiceOperation#requiresSynchronousExecution() require synchronous execution}.
     */
    private final static Executor executor = ThreadPoolUtil.INSTANCE
            .createForegroundTaskThreadPoolExecutor(ReplicationReceiverImpl.class.getName());

    /**
     * Generates a queue per distinct {@link Operation#getKeyForAsynchronousExecution() key} and schedules tasks to work
     * on those queues. This way, only operations with different keys will be scheduled for parallel execution,
     * therefore not blocking each other. For example, operations for fix insertions into the same track should have
     * equal keys, thus getting scheduled through the same queue and hence not being applied in parallel to each other
     * which would only let them wait for each other without creating any real parallelism.
     */
    private final OperationQueueByKeyExecutor operationQueueByKeyExecutor;

    private FullyInitializedReplicableTracker<SecurityService> securityServiceTracker;
    
    /**
     * @param master
     *            descriptor of the master server from which this replicator receives messages
     * @param replicableProvider
     *            OSGi service tracker for the replica to which to apply the messages received
     * @param startSuspended
     *            decides whether to stars the replicator immediately, not holding back messages received but forwarding
     *            them directly.
     * @param consumer
     *            the RabbitMQ consumer from which to load messages
     */
    public ReplicationReceiverImpl(ReplicationMasterDescriptor master, ReplicablesProvider replicableProvider, boolean startSuspended, QueueingConsumer consumer) {
        this.queueByReplicableIdAsString = new HashMap<>();
        this.maximumDeliveryTagSoFar = new AtomicLong();
        this.acknowledgeTaskScheduledButNotYetStarted = new AtomicInteger();
        this.master = master;
        this.replicableProvider = replicableProvider;
        this.suspended = startSuspended;
        this.consumer = consumer;
        this.operationQueueByKeyExecutor = new OperationQueueByKeyExecutor(executor);
        try {
            _queue = QueueingConsumer.class.getDeclaredField("_queue");
            _queue.setAccessible(true);
        } catch (Exception e) {
            _queue = null;
        }
        securityServiceTracker = Activator.getDefaultContext() == null ? null : FullyInitializedReplicableTracker.createAndOpen(Activator.getDefaultContext(), SecurityService.class);
    }
    
    /**
     * Starts fetching messages from the {@link #consumer}, uncompresses and de-serializes {@link Operation} objects from
     * the stream and applies them to local {@link Replicable}s.<p>
     * 
     * Two protocol versions are supported:
     * <ol>
     * <li>For backward compatibility, if the compressed stream starts with a regular replicable ID as a string, operations
     * are expected to be encoded one by one as {@code byte[]} objects to be de-serialized from an {@link ObjectInputStream}.
     * Each such {@code byte[]} is then assumed to represents objects serialized by an {@link ObjectOutputStream}, so they
     * can be read by an {@link ObjectInputStream}. Typically, the legacy format has exactly one operation per {@code byte[]}
     * (which is partly explaining why performance was not very good). The stream for reading the nested {@code byte[]} is
     * produced by the {@link Replicable#createObjectInputStreamResolvingAgainstCache(InputStream, Map)} method in order
     * to avoid duplicates and for setting up proper class loading for the {@link Replicable}'s context.</li>
     * <li></li>
     * </ol>
     * After receiving a single message, assumes it's a
     * {@link ReplicationServiceImpl#createUncompressingInputStream(InputStream) compressed} stream that first
     * {@link DataInputStream#readUTF() encodes a UTF string} representing the {@link Replicable#getId() replicable ID},
     * followed by a sequence of serialized {@code byte[]} objects which each can be {@link Replicable#readOperation(InputStream, Map) de-serialized}
     * by the receiving {@link Replicable} identified by the ID received as a prefix. This method then applies these operations to the
     * {@link Replicable} identified by the ID, retrieved through the {@link #replicableProvider}.
     * 
     * @see ReplicationServiceExecutionListener#executed(OperationWithResult)
     */
    @Override
    public void run() {
        long messageCount = 0;
        long operationCount = 0;
        final boolean logsFine = logger.isLoggable(Level.FINE);
        while (!isBeingStopped()) {
           try {
                Delivery delivery = consumer.nextDelivery();
                messageCount++;
                acknowledgeAsync(consumer.getChannel(), delivery);
                if (_queue != null) {
                    synchronized (this) {
                        if (getInboundMessageQueue().isEmpty()) {
                            notifyAll(); // wake up anyone waiting for isQueueEmpty()
                        }
                    }
                    if (logsFine || messageCount % 10l == 0) {
                        try {
                            logger.log(messageCount%10l==0 ? Level.INFO : Level.FINE,
                                    "Received "+messageCount+" replication messages with "+operationCount+" operations in total. Inbound replication queue size: "+getMessageQueueSize());
                        } catch (Exception e) {
                            // it didn't work; but it's a log message only...
                            logger.info("Received "+messageCount+" replication messages with "+operationCount+" operations in total.");
                        }
                    }
                }
                final byte[] bytesFromMessage = delivery.getBody();
                checksPerformed = 0;
                final InputStream uncompressingInputStream = ReplicationServiceImpl.createUncompressingInputStream(new ByteArrayInputStream(bytesFromMessage));
                final DataInputStream dataInputStream = new DataInputStream(uncompressingInputStream);
                final String replicableIdAsStringOrVersionIndicator = dataInputStream.readUTF();
                final String replicableIdAsString;
                final boolean legacyVersion;
                if (replicableIdAsStringOrVersionIndicator.equals(VERSION_INDICATOR)) {
                    legacyVersion = false;
                    final int protocolVersion = dataInputStream.read();
                    logger.fine(()->"Found protocol version "+protocolVersion);
                    replicableIdAsString = dataInputStream.readUTF();
                } else {
                    legacyVersion = true;
                    logger.fine("No protocol version indicator found; using legacy protocol");
                    replicableIdAsString = replicableIdAsStringOrVersionIndicator;
                }
                // TODO bug 2465: decide based on the master descriptor whether we want to process this message; if it's for a Replicable we're not replicating from that master, drop the message
                Replicable<?, ?> replicable = replicableProvider.getReplicable(replicableIdAsString, /* wait */ false);
                if (replicable != null) {
                    final Map<String, Class<?>> classLoaderCache = new HashMap<>();
                    final ObjectInputStream ois = legacyVersion
                            ? new ObjectInputStream(uncompressingInputStream) // no special stream required; only reading a generic byte[]
                            : replicable.createObjectInputStreamResolvingAgainstCache(uncompressingInputStream, classLoaderCache);
                    int operationsInMessage = 0;
                    try {
                        while (true) {
                            if (legacyVersion) {
                                byte[] serializedOperation = (byte[]) ois.readObject();
                                if (Util.contains(master.getReplicables(), replicable)) {
                                    readLegacyOperationAndApplyOrQueueIt(replicable, serializedOperation, classLoaderCache);
                                }
                            } else {
                                readOperationAndApplyOrQueueIt(replicable, ois);
                            }
                            if (Util.contains(master.getReplicables(), replicable)) {
                                operationCount++;
                                operationsInMessage++;
                                if (operationCount % 10000l == 0) {
                                    logger.info("Received " + operationCount + " operations so far");
                                }
                            } else {
                                logger.fine("Dropping operation for non-replicated replicable "+replicable.getId());
                            }
                        }
                    } catch (EOFException eof) {
                        logger.fine("Reached EOF on replication message after having read " + operationsInMessage
                                + " operations");
                        // reached EOF; expected
                    }
                } else {
                    // otherwise, we don't know the replicable and simply drop the message with all the operations for the unknown recipient
                    Stream<Named> sn = StreamSupport.stream(replicableProvider.getReplicables().spliterator(), /* parallel */ false)
                        .map(r->(() -> r.getId().toString()));
                    logger.warning("received replication message for replicable with ID "+replicableIdAsString+" which is unknnown by this replicator "+
                            "which only knows "+
                            Util.join(", ", sn::iterator));
                }
            } catch (ConsumerCancelledException cce) {
                logger.info("Consumer has been shut down properly.");
                break;
            } catch (InterruptedException irr) {
                logger.info("Application requested shutdown.");
                break;
            } catch (ShutdownSignalException sse) {
                /* make sure to respond to a stop event without waiting */
                if (isBeingStopped()) {
                    break;
                }
                if (sse.isInitiatedByApplication()) {
                    logger.severe("Application shut down messaging queue for " + this.toString()+
                            ", indicated by exception "+sse.getMessage());
                    sendMailAboutShutdownSignalException(sse);
                    break;
                }
                logger.severe("RabbitMQ channel was shut down: "+sse.getMessage()+"; trying to re-connect");
                if (checksPerformed <= CHECK_COUNT) {
                    try {
                        Thread.sleep(CHECK_INTERVAL_MILLIS);
                        /* isOpen() will return false if the channel has been closed. This
                         * does not hold when the connection is dropped.
                         */
                        if (!this.consumer.getChannel().isOpen()) {
                            maximumDeliveryTagAcknowledged = 0;
                            maximumDeliveryTagSoFar.set(0);
                            /* for a reconnection we need to instantiate a new consumer */
                            try {
                                logger.info("Channel seems to be closed. Trying to reconnect consumer queue...");
                                this.consumer = master.getConsumer();
                                logger.info("OK - channel reconnected!");
                                Thread.sleep(CHECK_INTERVAL_MILLIS);
                                checksPerformed += 1;
                            } catch (IOException | TimeoutException eio) {
                                // do not print exceptions known to occur
                            }
                        }
                    } catch (InterruptedException eir) {
                        logger.log(Level.WARNING, "Interrupted while trying to re-connect", eir);
                    }
                    checksPerformed += 1;
                    continue;
                } else {
                    logger.severe("Grace time (" + CHECK_COUNT*(CHECK_INTERVAL_MILLIS/1000) + "secs) is over. Terminating replication listener " + this.toString());
                    // XXX: Also make sure that all handlers get notifications about this
                    break;
                }
            } catch (Exception e) {
                logger.info("Exception while processing replica: "+e.getMessage());
                logger.log(Level.SEVERE, "run", e);
            }
        }
        logger.info("Stopped replicator thread. This server will no longer receive events from a master.");
        synchronized (this) {
            stopped = true;
            notifyAll();
        }
    }

    private void sendMailAboutShutdownSignalException(ShutdownSignalException sse) {
        final ResourceBundleStringMessages stringMessages = new ResourceBundleStringMessagesImpl(STRING_MESSAGES_BASE_NAME, getClass().getClassLoader(), StandardCharsets.UTF_8.name());
        final SecurityService securityService = getSecurityService();
        if (securityService == null) {
            logger.warning("No security service available; cannot send mail about shutdown signal exception");
        } else {
            final String serverName = ServerInfo.getName();
            for (final User userToSendMailTo : securityService.getUsersToInformAboutReplicaSet(serverName, Optional.of(DefaultActions.UPDATE))) {
                if (userToSendMailTo.isEmailValidated()) {
                    final String subject = stringMessages.get(userToSendMailTo.getLocaleOrDefault(), "MailSubjectShutdownSignalException", serverName);
                    final String body = stringMessages.get(userToSendMailTo.getLocaleOrDefault(), "MailBodyShutdownSignalException", serverName, sse.getMessage());
                    try {
                        securityService.sendMail(userToSendMailTo.getName(), subject, body);
                    } catch (MailException e) {
                        logger.log(Level.WARNING, "Error sending mail to "+userToSendMailTo.getName(), e);
                    }
                }
            }
        }
    }

    private SecurityService getSecurityService() {
        try {
            return securityServiceTracker == null ? null : securityServiceTracker.getInitializedService(0);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Error getting security service", e);
            return null;
        }
    }

    private <S, O extends OperationWithResult<S, ?>> void readLegacyOperationAndApplyOrQueueIt(
            Replicable<S, O> replicable, byte[] serializedOperation, Map<String, Class<?>> classLoaderCache)
            throws ClassNotFoundException, IOException {
        O operation = replicable.readOperation(new ByteArrayInputStream(serializedOperation), classLoaderCache);
        applyOrQueue(operation, replicable);
    }

    /**
     * Acknowledges a delivery asynchronously in the background by scheduling a task that will call {@link Channel#basicAck(long, boolean)}.
     * If further calls to this method occur, the maximum delivery tag value is recorded such that if the task gets its turn, a cumulative
     * acknowledgement takes place.<p>
     * 
     * Acknowledgements must be sent in strictly monotonous order. Therefore, task scheduling must be synchronized with incrementing
     * the delivery tag number to acknowledge. With a thread pool, task execution order is generally unpredictable. At the same time,
     * the synchronous part of acknowledgement handling must stay short in order not to hold up the actual payload processing.
     * The synchronous part therefore shall only check quickly for the presence of a yet unlaunched task, and this check will have to
     * be synchronized with the beginning of each task before it reads the latest delivery tag to acknowledge. An {@link AtomicInteger}
     * is used to count the number of not yet started tasks. This method will first increment the delivery tag to confim, then check
     * the atomic integer for a yet unstarted task. Since {@link AtomicInteger} guarantees {@code volatile} semantics, if this value is
     * greater than zero, a task will definitely pick up the new delivery tag. Otherwise, the AtomicInteger is incremented (it may in the
     * meantime have been incremented by another thread), and a corresponding task is scheduled.<p>
     * 
     * The tasks themselves run {@code synchronized} on the {@link #acknowledgementTaskLock}. Therefore, only one can run at a time.
     * The first thing a task does is decrement the atomic integer counting the tasks yet unstarted. Then it reads the delivery tag to
     * acknowledge and compares it with the {@link #maximumDeliveryTagAcknowledged deliver tag last acknowledged}. Only if
     * {@link #maximumDeliveryTagSoFar} is greater than that, an acknowledgement is sent.
     */
    private void acknowledgeAsync(final Channel channel, final Delivery delivery) {
        final long deliveryTag = delivery.getEnvelope().getDeliveryTag();
        maximumDeliveryTagSoFar.accumulateAndGet(deliveryTag, (lastMaxTag, newTag)->newTag>lastMaxTag?newTag:lastMaxTag);
        logger.finer(()->"Asynchronously acknowledging message with delivery tag "+deliveryTag);
        if (acknowledgeTaskScheduledButNotYetStarted.get() == 0) {
            acknowledgeTaskScheduledButNotYetStarted.incrementAndGet();
            logger.finer(()->"Scheduling background task to acknowledge delivery tag "+deliveryTag);
            ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor().submit(()->{
                synchronized (acknowledgementTaskLock) {
                    acknowledgeTaskScheduledButNotYetStarted.decrementAndGet();
                    final long deliveryTagToAcknowledge = maximumDeliveryTagSoFar.get();
                    if (deliveryTagToAcknowledge > maximumDeliveryTagAcknowledged) {
                        try {
                            logger.fine(()->"Sending acknowledgement for delivery tag "+deliveryTagToAcknowledge+
                                    ", scheduled at delivery tag "+deliveryTag);
                            channel.basicAck(deliveryTagToAcknowledge, /* multiple */ true);
                            maximumDeliveryTagAcknowledged = deliveryTagToAcknowledge;
                        } catch (IOException e) {
                            logger.log(Level.WARNING, "Acknowledging message with delivery tag "+deliveryTagToAcknowledge+
                                    " cumulatively failed", e);
                        }
                    } else {
                        logger.fine(()->"Not acknowledging "+deliveryTagToAcknowledge+" again because "+
                                maximumDeliveryTagAcknowledged+" has already been acknowledged");
                    }
                }
            });
        }
    }

    @Override
    public int getMessageQueueSize() throws IllegalAccessException {
        return getInboundMessageQueue().size();
    }
    
    @Override
    public Map<String, Integer> getOperationQueueSizes() {
        final Map<String, Integer> result = new HashMap<>();
        for (final Entry<String, LinkedBlockingQueue<Pair<String, OperationWithResult<?, ?>>>> e : queueByReplicableIdAsString.entrySet()) {
            result.put(e.getKey(), e.getValue().size());
        }
        return result;
    }

    /**
     * @return the message queueing system's message queue from which this replicator reads messages; can be used
     * to check if the queue is empty or to determine the number of elements in the queue
     */
    private BlockingQueue<?> getInboundMessageQueue() throws IllegalAccessException {
        return (BlockingQueue<?>) _queue.get(consumer);
    }
    
    private <S, O extends OperationWithResult<S, ?>> void readOperationAndApplyOrQueueIt(Replicable<S, O> replicable,
            ObjectInputStream ois) throws ClassNotFoundException, IOException {
        O operation = replicable.readOperationFromObjectInputStream(ois);
        if (Util.contains(master.getReplicables(), replicable)) {
            applyOrQueue(operation, replicable);
        }
    }

    /**
     * If the replicator is currently {@link #suspended}, the <code>operation</code> is queued, otherwise immediately
     * applied to the receiving replica.
     * 
     * @param replicable
     *            the replicable to which to apply or for which to queue the operation
     */
    private synchronized <S, O extends OperationWithResult<S, ?>> void applyOrQueue(O operation, Replicable<S, O> replicable) {
        if (isSuspended()) {
            queue(operation, replicable);
        } else {
            apply(operation, replicable);
        }
    }

    /**
     * {@link Operation#requiresSynchronousExecution() Synchronous operations} are executed immediately by the current
     * thread.
     * <p>
     * 
     * For asynchronous operations we must make sure that those are keyed such that operations with different keys don't
     * block each other. Otherwise, tasks that may end up in different threads of the {@link #executor} may need to wait
     * for each other, such as when trying to insert many fixes into the track of the same object (see also bug 5518).
     * With this, asynchronous operations with equal keys are added to a queue that is specific to that key, and a task per
     * such queue is created.<p>
     * 
     * The handling of those queues for asynchronous operations makes sure that a task processing the queue is scheduled
     * with the executor or already running if and only if there are operations in the queue. When the task removes the
     * last operation from the queue, the queue is removed and the task decides to finish. From this point on, if another
     * asynchronous operation with that same key arrives, a new queue and subsequently a new task will be created, and the
     * task will be scheduled with the executor. These steps are performed under the necessary synchronization so that
     * no race conditions can occur (such as the task deciding to finish, yet another operation getting added to the queue
     * which hence gets dropped because then the queue would be deleted; or similar "desasters").
     */
    private synchronized <S, O extends OperationWithResult<S, ?>> void apply(final O operation, Replicable<S, O> replicable) {
        final int operationCount = ++operationCounter;
        logger.finer(()->""+operationCount+": Applying "+operation);
        Runnable runnable = () -> replicable.applyReceivedReplicated(operation);
        if (operation.requiresSynchronousExecution()) {
            runnable.run();
        } else {
            operationQueueByKeyExecutor.schedule(operation.getKeyForAsynchronousExecution(), runnable);
        }
        logger.finer(()->""+operationCount+": Done applying "+operation);
    }
    
    private void queue(OperationWithResult<?, ?> operation, Replicable<?, ?> replicable) {
        final String replicableIdAsString = replicable.getId().toString();
        LinkedBlockingQueue<Pair<String, OperationWithResult<?, ?>>> queue = queueByReplicableIdAsString.get(replicableIdAsString);
        if (queue == null) {
            queue = new LinkedBlockingQueue<>();
            queueByReplicableIdAsString.put(replicableIdAsString, queue);
        }
        queue.add(new Pair<String, OperationWithResult<?, ?>>(replicable.getId().toString(), operation));
        assert !queue.isEmpty();
    }
    
    public synchronized void setSuspended(final boolean suspended) {
        if (this.suspended != suspended) {
            if (!suspended) {
                applyQueues(/* resumeWhenDone */ true); // apply queues before setting suspended to false; further incoming operations have to stand in line
            } else {
                this.suspended = true;
            }
        }
    }
    
    private void applyQueues(boolean resumeWhenDone) {
        logger.info("Applying queued replication messages received");
        for (Entry<String, LinkedBlockingQueue<Pair<String, OperationWithResult<?, ?>>>> r : queueByReplicableIdAsString.entrySet()) {
            Replicable<?, ?> replicable = replicableProvider.getReplicable(r.getKey(), /* wait */ false);
            final LinkedBlockingQueue<Pair<String, OperationWithResult<?, ?>>> queue = r.getValue();
            boolean queueEmpty = false;
            Pair<String, OperationWithResult<?, ?>> replicableIdAsStringAndOperation;
            do {
                synchronized (this) {
                    replicableIdAsStringAndOperation = queue.poll();
                    if (resumeWhenDone && replicableIdAsStringAndOperation == null) {
                        queueEmpty = true;
                        suspended = false;
                        notifyAll();
                    }
                }
                if (replicableIdAsStringAndOperation != null) {
                    try {
                        applyWithCast(replicableIdAsStringAndOperation.getB(), replicable);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error applying queued, replicated operation "
                                + replicableIdAsStringAndOperation + ". Continuing with next queued operation.", e);
                    }
                }
            } while (!queueEmpty);
            assert queue.isEmpty();
        }
        if (resumeWhenDone) {
            suspended = false;
        }
        notifyAll();
    }

    @SuppressWarnings("unchecked")
    private <S, O extends OperationWithResult<S, ?>> void applyWithCast(O operation, Replicable<?, ?> replicable) {
        Replicable<S, O> castReplicable = (Replicable<S, O>) replicable;
        apply(operation, castReplicable);
    }

    @Override
    public synchronized boolean isSuspended() {
        return suspended;
    }
    
    public synchronized void stop(boolean applyQueuedMessages) {
        if (applyQueuedMessages) {
            if (isSuspended()) {
                /* make sure to apply everything in queue before stopping this thread */
                applyQueues(/* resumeWhenDone */ false);
            }
        } else {
            logger.info("Discarding queued replication messages received");
            for (LinkedBlockingQueue<Pair<String, OperationWithResult<?, ?>>> queue : queueByReplicableIdAsString.values()) {
                queue.clear(); // discard queue contents if they shall not be applied
            }
        }
        logger.info("Signaled Replicator thread to stop asap.");
        stopped = true;
        master.stopConnection(/* deleteExchange */ false);
        notifyAll(); // notify those waiting for stopped
    }
    
    @Override
    public synchronized boolean isBeingStopped() {
        return stopped;
    }

    @Override
    public String toString() {
        long queueSize = queueByReplicableIdAsString.values().stream().mapToLong(l->l.size()).sum();
        return "Replicator for master "+master+", queue size: "+queueSize;
    }

    @Override
    public boolean isQueueEmptyOrStopped() throws IllegalAccessException {
        return isBeingStopped() ||
                (_queue == null || getInboundMessageQueue().isEmpty()) && !queueByReplicableIdAsString.values().stream().anyMatch(q->!q.isEmpty());
    }

}
