package com.sap.sse.replication.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.util.tracker.ServiceTracker;

import com.rabbitmq.client.AMQP.Exchange;
import com.rabbitmq.client.AMQP.Queue.DeleteOk;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.sap.sse.ServerInfo;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.operationaltransformation.Operation;
import com.sap.sse.replication.OperationExecutionListener;
import com.sap.sse.replication.OperationWithResult;
import com.sap.sse.replication.OperationsToMasterSender;
import com.sap.sse.replication.OperationsToMasterSendingQueue;
import com.sap.sse.replication.RabbitMQConnectionFactoryHelper;
import com.sap.sse.replication.ReplicaDescriptor;
import com.sap.sse.replication.Replicable;
import com.sap.sse.replication.ReplicablesProvider;
import com.sap.sse.replication.ReplicablesProvider.ReplicableLifeCycleListener;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.replication.ReplicationReceiver;
import com.sap.sse.replication.ReplicationService;
import com.sap.sse.replication.ReplicationStatus;
import com.sap.sse.replication.interfaces.impl.ReplicationStatusImpl;
import com.sap.sse.replication.persistence.MongoObjectFactory;
import com.sap.sse.util.HttpUrlConnectionHelper;

import net.jpountz.lz4.LZ4BlockInputStream;

/**
 * Manages a set of observers of {@link Replicable}, receiving notifications for the operations they perform that
 * require replication. This service provides the connectivity and central management including central keeping of
 * replication statistics for this server instance. Triggering the broadcast of an operation notified by a
 * {@link Replicable} is in the responsibility of each individual observer.
 * <p>
 * 
 * The observers are registered only when there are replicas registered. If the last replica is de-registered, the
 * service stops observing the {@link Replicable}. Operations received that require replication are sent to the
 * {@link Exchange} to which replica queues can bind, using a {@link ReplicationReceiverImpl}. By prefixing each message
 * with the {@link Object#toString()} representation of the {@link Replicable}'s {@link Replicable#getId() ID} the
 * receiver can determine to which {@link Replicable} to forward the operation. As such, this service multiplexes the
 * replication channels for potentially many {@link Replicable}s living in this server instance.
 * <p>
 * 
 * The exchange name and connectivity information for the message queuing system are provided to this service during
 * construction.
 * <p>
 * 
 * This service object {@link Replicable#addOperationExecutionListener(OperationExecutionListener) registers} individual
 * listeners at the {@link Replicable}s so it {@link #executed(OperationWithResult) receives} notifications about
 * operations executed by the {@link Replicable} that require replication if and only if there is at least one replica
 * registered.
 * 
 * @author Frank Mittag, Axel Uhl (d043530)
 * 
 */
public class ReplicationServiceImpl implements ReplicationService, OperationsToMasterSendingQueue, ReplicationMessageSender {
    private static final Logger logger = Logger.getLogger(ReplicationServiceImpl.class.getName());

    private final ReplicationInstancesManager replicationInstancesManager;

    private final ReplicablesProvider replicablesProvider;

    /**
     * <code>null</code>, if this instance is not currently replicating from some master; the master's descriptor
     * otherwise; note that partial replication is supported, meaning that only operations for a subset of the
     * {@link Replicable}s running on this server will be considered when received from the master.
     */
    private ReplicationMasterDescriptor replicatingFromMaster;

    /**
     * The UUIDs with which this replica is registered by the master identified by the corresponding key
     */
    private final ConcurrentMap<ReplicationMasterDescriptor, String> replicaUUIDs;

    /**
     * Channel used by a master server to publish replication operations; <code>null</code> in servers that don't have
     * replicas registered
     */
    private Channel masterChannel;

    /**
     * The name of the RabbitMQ exchange to which this replication service sends its replication operations in
     * serialized form. Clients need to know this name to be able to bind their queues to the exchange.
     */
    private final String exchangeName;

    /**
     * The host on which the RabbitMQ server runs
     */
    private final String exchangeHost;

    /**
     * The port on which to reach the RabbitMQ server, or 0 for the RabbitMQ default port
     */
    private final int exchangePort;

    /**
     * UUID that identifies this server
     */
    private final UUID serverUUID;

    /**
     * For this instance running as a replica, the replicator receives messages from the master's queue and applies them
     * to the local replica.
     */
    private ReplicationReceiverImpl replicator;

    private final ConcurrentMap<String, ReplicationServiceExecutionListener<?>> executionListenersByReplicableIdAsString;

    private Thread replicatorThread;

    /**
     * Allow queued outbound replication messages to consume at most 1/4 of the maximum amount of memory available to this VM.
     */
    private static final long SEND_JOB_QUEUE_SIZE_THRESHOLD_IN_BYTES = Runtime.getRuntime().maxMemory() / 4;
    
    /**
     * All outbound sending jobs go through this queue where a thread removing from this queue picks them up and sends
     * them out. The order is first-in, first-out (FIFO). The total size of the {@code byte[]} objects contained in the
     * queue is counted in {@link #totalSendJobsSize}. If the {@link #SEND_JOB_QUEUE_SIZE_THRESHOLD_IN_BYTES} is
     * exceeded, messages are dropped, which is logged as a {@link Level#SEVERE} error.
     * <p>
     * 
     * Being a {@link BlockingDeque}, the queue is thread-safe. Multiple threads can {@link BlockingDeque#add(Object)
     * add} to it without problems, and the single thread created in {@link #createSendJob()} will continue to
     * {@link BlockingDeque#take() take} elements from it as they become available. This way, this queue serves as a
     * serialization and synchronization element for sending out replication operations to a message queue.
     * <p>
     * 
     * Any significant queue-up may be expected if...
     * <ul>
     * <li>...writing to the RabbitMQ queue connected to the fanout exchange is slower than new replication content is
     * produced; in this case the network simply is too slow to support the replication scenario at hand.</li>
     * <li>...the connection to the RabbitMQ queue connected to the fanout exchange is temporarily or permanently
     * interrupted; for temporary interruptions there is hope that re-connecting attempts succeed before the buffer runs
     * over and that after successfully re-connecting the buffer can be send out message by message. For connections
     * interrupted permanently a buffer overrun has to be expected at some point, and it will be better to at least keep
     * the master healthy instead of letting it run into an {@link OutOfMemoryError} because the buffer eats up all
     * available heap space.</li>
     * </ul>
     */
    private final BlockingDeque<Pair<byte[], List<Class<?>>>> sendJobQueue;
    
    /**
     * Each time a {@code byte[]} is added to the {@link #sendJobQueue}, this value is increased by the length of that array;
     * conversely, each type a {@code byte[]} is removed from the {@link #sendJobQueue}, this value is decreased accordingly.
     * This value can be compared to {@link #SEND_JOB_QUEUE_SIZE_THRESHOLD_IN_BYTES} to decide whether or not more send jobs
     * can be accepted for queuing.
     */
    private final AtomicLong totalSendJobsSize;
    
    /**
     * The time between attempts to re-send outbound messages to the RabbitMQ exchange including re-connecting to the
     * fanout exchange with a new channel.
     */
    private static final Duration DURATION_BETWTEEN_SEND_RETRIES = Duration.ONE_SECOND.times(5);

    /**
     * A buffer for operations that need to be sent out to replicas through a fan-out exchange and that require
     * {@link Operation#requiresSynchronousExecution() synchronous execution}.
     */
    private final OperationSerializerBufferImpl outboundBufferForOperationsRequiringSynchronousExecution;

    /**
     * A buffer pool for all operations to be sent out to replicas through a fan-out exchange for which replicas may
     * choose to apply them asynchronously. See {@link Operation#requiresSynchronousExecution()}. After construction,
     * the pool is in the "stopped" state. It needs to be {@link OperationSerializerBufferPool#start}ed to 
     */
    private final OperationSerializerBufferPool outboundBufferPoolForOperationsAllowingForAsynchronousExecution;

    /**
     * Used to schedule the sending of all operations in {@link #outboundBuffer} using the {@link #sendingTask}.
     */
    private final Timer timer;

    private final UnsentOperationsSenderJob unsentOperationsSenderJob;

    /**
     * Defines for how many milliseconds the {@link #timer} will wait since the first operation has been added to an
     * empty {@link #outboundBuffer} until it carries out the actual transmission task. The longer this duration, the
     * more operations are likely to be sent per message transmitted, reducing overhead but correspondingly increasing
     * latency.
     */
    private static final Duration TRANSMISSION_DELAY = Duration.ofMillis(100);

    /**
     * Defines at which message size in bytes the message will be sent regardless the {@link #TRANSMISSION_DELAY}
     * .
     */
    private static final int TRIGGER_MESSAGE_SIZE_IN_BYTES = 1024 * 1024;

    /**
     * Counts the messages sent out by this replicator
     */
    private long messageCount;

    /**
     * Before receiving the initial load from a master is started, a message queue channel is opened through which the
     * initial load data stream is received. These channels are stored here for two reasons. If a master appears as key
     * here, an initial load process receiving from that master is currently running, and no second concurrent attempt
     * to replicate from that master should be started.
     */
    private final Map<ReplicationMasterDescriptor, InitialLoadRequest> initialLoadChannels;

    /**
     * Will be set
     */
    private volatile boolean replicationStarting;

    /**
     * An optional link to a persistence layer that allows this service to record replicas
     * registered at this server, so that optionally during a re-start those replica
     * links can be re-established.
     */
    private final Optional<MongoObjectFactory> mongoObjectFactory;
    
    private final Set<ReplicationStartingListener> replicationStartingListeners;

    private static class InitialLoadRequest {
        private final Channel channelForInitialLoad;
        private final Iterable<Replicable<?, ?>> replicables;
        private final String queueName;

        public InitialLoadRequest(Channel channelForInitialLoad, Iterable<Replicable<?, ?>> replicables, String queueName) {
            super();
            this.channelForInitialLoad = channelForInitialLoad;
            this.replicables = replicables;
            this.queueName = queueName;
        }

        public Channel getChannelForInitialLoad() {
            return channelForInitialLoad;
        }

        public Iterable<Replicable<?, ?>> getReplicables() {
            return replicables;
        }

        protected String getQueueName() {
            return queueName;
        }
    }

    private class LifeCycleListener implements ReplicableLifeCycleListener {
        @Override
        public void replicableAdded(Replicable<?, ?> replicable) {
            // add a replication listener to the new replicable only if there are replicas currently registered...
            synchronized (replicationInstancesManager) {
                // .. and at least one of them wants to replicate the replicable with that ID
                if (Util.contains(replicationInstancesManager.getAllReplicableIdsAtLeastOneReplicaIsReplicating(), replicable.getId().toString())) {
                    ensureOperationExecutionListener(replicable);
                }
            }
        }

        @Override
        public void replicableRemoved(String replicableIdAsString) {
            ReplicationServiceExecutionListener<?> listener = executionListenersByReplicableIdAsString
                    .remove(replicableIdAsString);
            if (listener != null) {
                listener.unsubscribe();
            }
        }
    }

    /**
     * @param exchangeName
     *            name of the fan-out exchange to which replication operations will be sent
     * @param exchangeHost
     *            name of the host under which the RabbitMQ server can be reached
     * @param exchangePort
     *            port of the RabbitMQ server, or 0 for default port
     * @param replicablesProvider
     *            lets this service request the currently known {@link Replicable} objects to be observed for
     *            replication; it also offers this service the possibility to register for life cycle events of those
     *            {@link Replicable} objects so that this service can stop observing them for operations to be
     *            replicated when they have been removed, or start observing new {@link Recpliable} objects that were
     *            added.
     */
    public ReplicationServiceImpl(String exchangeName, String exchangeHost, int exchangePort,
            final ReplicationInstancesManager replicationInstancesManager, ReplicablesProvider replicablesProvider)
            throws Exception {
        this(/* defaultMongoObjectFactory */ Optional.empty(), exchangeName, exchangeHost, exchangePort,
                replicationInstancesManager, replicablesProvider);
    }

    public ReplicationServiceImpl(Optional<MongoObjectFactory> optionalMongoObjectFactory, String exchangeName,
            String exchangeHost, int exchangePort, ReplicationInstancesManager replicationInstancesManager,
            ReplicablesProvider replicablesProvider) throws Exception {
        this(/* replicasToAssumeConnectedToThisMaster */ null, optionalMongoObjectFactory,
                exchangeName, exchangeHost, exchangePort, replicationInstancesManager, replicablesProvider);
    }

    public ReplicationServiceImpl(Iterable<ReplicaDescriptor> replicasToAssumeConnectedToThisMaster,
            Optional<MongoObjectFactory> optionalMongoObjectFactory, String exchangeName, String exchangeHost, int exchangePort,
            ReplicationInstancesManager replicationInstancesManager, ReplicablesProvider replicablesProvider) throws Exception {
        this.sendJobQueue = new LinkedBlockingDeque<>();
        timer = new Timer("ReplicationServiceImpl timer for delayed task sending", /* isDaemon */ true);
        this.outboundBufferForOperationsRequiringSynchronousExecution = new OperationSerializerBufferImpl(/* sender */ this, TRANSMISSION_DELAY, TRIGGER_MESSAGE_SIZE_IN_BYTES, timer);
        this.outboundBufferPoolForOperationsAllowingForAsynchronousExecution = new OperationSerializerBufferPool(/* sender */ this, TRANSMISSION_DELAY, TRIGGER_MESSAGE_SIZE_IN_BYTES, timer);
        this.totalSendJobsSize = new AtomicLong(0);
        createSendJob().start();
        this.mongoObjectFactory = optionalMongoObjectFactory;
        this.replicationStartingListeners = new HashSet<>();
        unsentOperationsSenderJob = new UnsentOperationsSenderJob();
        executionListenersByReplicableIdAsString = new ConcurrentHashMap<>();
        initialLoadChannels = new ConcurrentHashMap<>();
        this.replicationInstancesManager = replicationInstancesManager;
        replicaUUIDs = new ConcurrentHashMap<ReplicationMasterDescriptor, String>();
        this.replicablesProvider = replicablesProvider;
        this.exchangeName = exchangeName;
        this.exchangeHost = exchangeHost;
        this.exchangePort = exchangePort;
        replicablesProvider.addReplicableLifeCycleListener(new LifeCycleListener());
        replicator = null;
        serverUUID = UUID.randomUUID();
        logger.info("Setting " + serverUUID.toString() + " as unique replication identifier.");
        if (replicasToAssumeConnectedToThisMaster != null) {
            for (final ReplicaDescriptor replica : replicasToAssumeConnectedToThisMaster) {
                registerReplica(replica);
            }
        }
    }

    protected ServiceTracker<Replicable<?, ?>, Replicable<?, ?>> getReplicableTracker() {
        return new ServiceTracker<Replicable<?, ?>, Replicable<?, ?>>(Activator.getDefaultContext(),
                Replicable.class.getName(), null);
    }

    protected ReplicablesProvider getReplicablesProvider() {
        return replicablesProvider;
    }

    @Override
    public ReplicationReceiver getReplicator() {
        return replicator;
    }

    private Channel createMasterChannelAndDeclareFanoutExchange() throws Exception {
        Channel result = createMasterChannel();
        if (!Util.hasLength(exchangeName)) {
            logger.severe("Replica seems registering at this master, but this master's exchange name is \""+exchangeName+
                    "\". Failing with an exception.");
            throw new IllegalStateException("Master's outbound replication exchange name is "+exchangeName+" but must be non-empty; "+
                    "consider setting the REPLICATION_CHANNEL environment variable.");
        }
        result.exchangeDeclare(exchangeName, "fanout");
        logger.info("Created fanout exchange " + exchangeName + " successfully.");
        return result;
    }

    @Override
    public Channel createMasterChannel() throws Exception {
        final ConnectionFactory connectionFactory = RabbitMQConnectionFactoryHelper.getConnectionFactory();
        connectionFactory.setHost(exchangeHost);
        if (exchangePort != 0) {
            connectionFactory.setPort(exchangePort);
        }
        try {
            final Channel result = connectionFactory.newConnection().createChannel();
            logger.info("Connected to " + connectionFactory.getHost() + ":" + connectionFactory.getPort());
            return result;
        } catch (ConnectException | TimeoutException ex) {
            // make sure to log something meaningful
            logger.severe("Could not connect to messaging queue on " + connectionFactory.getHost() + ":"
                    + connectionFactory.getPort() + "/" + exchangeName);
            throw ex;
        }
    }

    private Iterable<Replicable<?, ?>> getReplicables() {
        return replicablesProvider.getReplicables();
    }
    
    private Replicable<?, ?> getReplicable(String replicableIdAsString, boolean wait) {
        return replicablesProvider.getReplicable(replicableIdAsString, wait);
    }
    
    @Override
    public void registerReplica(ReplicaDescriptor replica) throws Exception {
        // due to different replicables to be replicated for replica, ensure that all
        // replicables to be replicated to replica are actually observed:
        addAsListenerToReplicables(replica.getReplicableIdsAsStrings());
        synchronized (replicationInstancesManager) {
            // need to establish the outbound messaging channel only when this is the first replica to be added:
            if (!replicationInstancesManager.hasReplicas()) {
                openOutboundReplicationChannel();
            }
            replicationInstancesManager.registerReplica(replica);
            recordRegisteredReplicaPersistently(replica);
        }
        logger.info("Registered replica " + replica);
    }

    private synchronized void openOutboundReplicationChannel() throws Exception {
        if (masterChannel == null) {
            masterChannel = createMasterChannelAndDeclareFanoutExchange();
        }
        outboundBufferPoolForOperationsAllowingForAsynchronousExecution.start();
    }

    private void recordRegisteredReplicaPersistently(ReplicaDescriptor replica) {
        mongoObjectFactory.ifPresent(mof->mof.storeReplicaDescriptor(replica));
    }

    private void removeRegisteredReplicaPersistently(ReplicaDescriptor replica) {
        mongoObjectFactory.ifPresent(mof->mof.removeReplicaDescriptor(replica));
    }

    private void addAsListenerToReplicables(String[] replicableIdsAsStringForReplicablesToReplicate) {
        for (final String replicableIdAsStringForReplicableToReplicate : replicableIdsAsStringForReplicablesToReplicate) {
            Replicable<?, ?> replicable = getReplicable(replicableIdAsStringForReplicableToReplicate, /* wait */ true);
            ensureOperationExecutionListener(replicable);
        }
    }

    /**
     * If no listener exists yet for {@code replicable}, a new one is created, registered as listener on
     * {@code replicable} and remembered in {@link #executionListenersByReplicableIdAsString}. Otherwise, this method is
     * a no-op.
     */
    private <S> void ensureOperationExecutionListener(Replicable<S, ?> replicable) {
        if (!executionListenersByReplicableIdAsString.containsKey(replicable.getId().toString())) {
            final ReplicationServiceExecutionListener<S> listener = new ReplicationServiceExecutionListener<S>(this, replicable);
            executionListenersByReplicableIdAsString.put(replicable.getId().toString(), listener);
        }
    }

    @Override
    public ReplicaDescriptor unregisterReplica(UUID replicaUuid) throws IOException {
        logger.info("Unregistering replica with ID " + replicaUuid);
        synchronized (replicationInstancesManager) {
            final boolean hadReplicas = replicationInstancesManager.hasReplicas();
            final Iterable<String> oldReplicablesInReplication = replicationInstancesManager.getAllReplicableIdsAtLeastOneReplicaIsReplicating();
            removeRegisteredReplicaPersistently(replicationInstancesManager.getReplicaDescriptor(replicaUuid));
            final ReplicaDescriptor unregisteredReplica = replicationInstancesManager.unregisterReplica(replicaUuid);
            final Iterable<String> newReplicablesInReplication = replicationInstancesManager.getAllReplicableIdsAtLeastOneReplicaIsReplicating();
            for (final String idAsStringOfReplicableNoReplicaIsInterestedInAnymore : Util.removeAll(newReplicablesInReplication, Util.addAll(oldReplicablesInReplication, new HashSet<>()))) {
                removeAsListenerFromReplicable(idAsStringOfReplicableNoReplicaIsInterestedInAnymore);
            }
            if (hadReplicas && !replicationInstancesManager.hasReplicas()) {
                logger.info("Last replica got unregistered. Stopping to send out operations and closing outbound queue.");
                closeOutboundReplicationChannel();
            }
            return unregisteredReplica;
        }
    }
    
    @Override
    public void unregisterReplica(ReplicaDescriptor replica) throws IOException {
        logger.info("Unregistering replica " + replica);
        unregisterReplica(replica.getUuid());
    }

    private void removeAsListenerFromReplicable(String idAsStringOfReplicableNoReplicaIsInterestedInAnymore) {
        ReplicationServiceExecutionListener<?> listener = executionListenersByReplicableIdAsString.remove(idAsStringOfReplicableNoReplicaIsInterestedInAnymore);
        if (listener != null) {
            logger.info("Unsubscribed replication listener from replicable with ID "+idAsStringOfReplicableNoReplicaIsInterestedInAnymore);
            listener.unsubscribe();
        } else {
            logger.warning("Couldn't find a replication listener on replicable with ID "+idAsStringOfReplicableNoReplicaIsInterestedInAnymore);
        }
    }

    private void removeAsListenerFromReplicables() {
        for (ReplicationServiceExecutionListener<?> listener : executionListenersByReplicableIdAsString.values()) {
            listener.unsubscribe();
        }
        executionListenersByReplicableIdAsString.clear();
    }

    /**
     * Schedules a single operation for broadcast. The operation is added to {@link #outboundBuffer}, and if not already
     * scheduled, a {@link #timer} is created and scheduled to send in {@link #TRANSMISSION_DELAY} milliseconds.
     * 
     * @param replicable
     *            the replicable by which the operation was executed that now will be broadcast to all replicas; the
     *            {@link Replicable#getId() ID} of this replica in its string form will be used to prefix the operation
     */
    <S, O extends OperationWithResult<S, ?>> void broadcastOperation(OperationWithResult<?, ?> operation,
            Replicable<S, O> replicable) throws IOException {
        final OperationSerializerBuffer bufferToWriteTo = operation.requiresSynchronousExecution()
                ? outboundBufferForOperationsRequiringSynchronousExecution
                : outboundBufferPoolForOperationsAllowingForAsynchronousExecution;
        bufferToWriteTo.write(operation, replicable);
        if (++messageCount % 10000l == 0) {
            logger.info("Handled " + messageCount
                    + " messages for replication. Current outbound replication queue size: "
                    + outboundBufferForOperationsRequiringSynchronousExecution.size());
        }
    }
    
    @Override
    public void send(byte[] message, List<Class<?>> typesInMessage) {
        if (totalSendJobsSize.get() < SEND_JOB_QUEUE_SIZE_THRESHOLD_IN_BYTES) {
            sendJobQueue.add(new Pair<>(message, typesInMessage));
            final long newTotalSendJobsSize = totalSendJobsSize.addAndGet(message.length);
            logger.fine("Successfully handed " + typesInMessage.size() +
                    " operations to broadcaster; new outbound send queue length "+sendJobQueue.size()+
                    " ("+newTotalSendJobsSize+"B)");
        } else {
            logger.severe("Queue for outbound replication operations full ("+totalSendJobsSize.get()+
                    "B. Dropping operations buffer with size "+message.length+"B");
        }
    }

    static InputStream createUncompressingInputStream(InputStream streamToWrap) {
        return new LZ4BlockInputStream(streamToWrap);
    }

    /**
     * Constructs the thread assigned to {@link #sendJob}, which is responsible for watching the {@link #sendJobQueue},
     * {@link BlockingDeque#take() taking} element from the queue and trying to send them to the {@link #exchangeHost}/{@link #exchangePort}
     * to the exchange named as specified by the field {@link #exchangeName}, using the {@link #masterChannel}. If this fails,
     * a re-try strategy is applied which includes trying to re-establish a connection to the fanout exchange using the
     * {@link #createMasterChannelAndDeclareFanoutExchange()} method.<p>
     * 
     * If sending a message succeeded, the {@link #totalSendJobsSize} counter is decreased by the length of the message sent.
     * Furthermore, the {@link #replicationInstancesManager} is {@link ReplicationInstancesManager#log(List, long) updated} with
     * the statistics about the types of operations sent.
     * 
     * @return the thread to assign to {@link #sendJob}
     */
    private Thread createSendJob() {
        final Thread result = new Thread("Replicator send job for exchange "+exchangeHost+":"+exchangePort+"/"+exchangeName) {
            @Override
            public void run() {
                logger.info("Thread "+getName()+" started.");
                while (true) {
                    try {
                        logger.fine("Taking a message from the sendJobQueue");
                        final Pair<byte[], List<Class<?>>> messageAndTypesOfOperations = sendJobQueue.take();
                        logger.fine(()->"Took a message with size "+messageAndTypesOfOperations.getA().length+"B from the sendJobQueue");
                        boolean delivered = false;
                        do {
                            try {
                                logger.fine(()->"Trying to send message with size "+messageAndTypesOfOperations.getA().length+"B");
                                broadcastOperations(messageAndTypesOfOperations.getA(), messageAndTypesOfOperations.getB());
                                delivered = true;
                                final long newTotalSendJobsSize = totalSendJobsSize.addAndGet(-messageAndTypesOfOperations.getA().length);
                                logger.fine(()->"New send queue size "+sendJobQueue.size()+" ("+newTotalSendJobsSize+"B)");
                            } catch (Exception ioe) {
                                logger.log(Level.WARNING, "Exception trying to send out replication operations to RabbitMQ exchange "+
                                        exchangeHost+":"+exchangePort+"/"+exchangeName+
                                        "; trying to re-establish a channel to the fanout exchange in "+DURATION_BETWTEEN_SEND_RETRIES,
                                        ioe);
                                Thread.sleep(DURATION_BETWTEEN_SEND_RETRIES.asMillis());
                                logger.info("Creating new channel to exchange in order to re-try sending");
                                try {
                                    masterChannel = createMasterChannelAndDeclareFanoutExchange();
                                    logger.info("Channel established; retrying now.");
                                } catch (Exception e) {
                                    logger.log(Level.SEVERE, "Re-establishing a connection to the fan-out exchange at "+
                                            exchangeHost+":"+exchangePort+"/"+exchangeName+
                                            " didn't work. Will try to send again through old channel which will probably fail, then sleep and try again.",
                                            e);
                                }
                            }
                        } while (!delivered);
                    } catch (InterruptedException e) {
                        logger.log(Level.WARNING, "Outbound replication message sender interrupted. Continuing...");
                    }
                }
            }
        };
        result.setDaemon(true);
        return result;
    }

    /**
     * Bytes arriving here have gone through Java object serialization and compression, probably also grouping based on
     * time delays, and are supposed to be sent out immediately. During sending, exceptions may occur, e.g., due to an
     * interrupted connection, the message queuing system being temporarily unavailable or some form of re-configuration
     * or scaling that is taking place. In order not to lose such messages, a FIFO queue exists which keeps track of all
     * the {@code bytesToSend / classesOfOperationsToSend} pairs passed as arguments to this method, with a separate
     * thread responsible for sending those messages out.
     * <p>
     * 
     * The queue/thread combination may reject the submission of a message because the messages queued in it already
     * consume too much memory. In this case, outbound replication has to be considered stopped. We can log this state as a
     * {@code SEVERE} exception, and we can keep trying to re-establish connectivity, but the question is how useful it will
     * be if connectivity can eventually be restored after so many messages have been lost. Replicas will have to be considered
     * inconsistent from this moment on and should be re-started, at least again receiving a new initial load, to transition
     * into a consistent state again.
     * <p>
     * 
     * <b>Precondition:</b> The caller must have obtained the object monitor on {@link #outboundBufferMonitor} using a
     * {@code synchronized} block.
     */
    private void broadcastOperations(byte[] bytesToSend, List<Class<?>> classesOfOperationsToSend) throws IOException {
        logger.fine("broadcasting " + classesOfOperationsToSend.size() + " operations as " + bytesToSend.length
                + " bytes");
        if (masterChannel != null) {
            logger.fine("buffer to broadcast has " + bytesToSend.length + " bytes ("
                    + (bytesToSend.length / 1024 / 1024) + "MB)");
            long startTime = System.currentTimeMillis();
            masterChannel.basicPublish(exchangeName, /* routingKey */"", /* properties */null, bytesToSend);
            logger.fine("successfully published " + bytesToSend.length + " bytes, taking "
                    + (System.currentTimeMillis() - startTime) + "ms");
            replicationInstancesManager.log(classesOfOperationsToSend, bytesToSend.length);
        }
    }

    @Override
    public Iterable<ReplicaDescriptor> getReplicaInfo() {
        return replicationInstancesManager.getReplicaDescriptors();
    }

    @Override
    public ReplicationMasterDescriptor getReplicatingFromMaster() {
        return replicatingFromMaster;
    }

    /**
     * The peer for this method is
     * {@link ReplicationServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     * which implements the initial load sending process. This method will return only after the initial load for all
     * replicas described in the {@code master} descriptor has completed.
     */
    @Override
    public void startToReplicateFrom(final ReplicationMasterDescriptor master) throws Exception {
        if (initialLoadChannels.containsKey(master)) {
            logger.warning("An initial load from "+master+" is already running, replicating the following replicables: "+
                            initialLoadChannels.get(master).getReplicables()+". Not starting a second time.");
        } else {
            final Iterable<Replicable<?, ?>> replicables = master.getReplicables();
            logger.info("Starting to replicate from " + master);
            try {
                registerReplicaWithMaster(master);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "ERROR", ex);
                throw ex;
            }
            replicatingFromMaster = master;
            logger.info("Registered replica with master.");
            QueueingConsumer consumer = null;
            // logging exception here because it will not propagate
            // thru the client with all details
            final Timer timer = new Timer("RabbitMQ Connection Timeout Logger", /* isDaemon */ true);
            final int LOGGING_TIMEOUT_IN_SECONDS = 10;
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    logger.warning("RabbitMQ connection to "+master+
                            " was not obtained in "+LOGGING_TIMEOUT_IN_SECONDS+"s. Keeping trying...");
                }
            }, LOGGING_TIMEOUT_IN_SECONDS*1000, LOGGING_TIMEOUT_IN_SECONDS*1000);
            logger.info("Connecting to message queue " + master);
            try {
                consumer = master.getConsumer();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "ERROR", ex);
                replicatingFromMaster = null;
                throw ex;
            } finally {
                timer.cancel();
            }
            logger.info("Connection to exchange successful.");
            final URL initialLoadURL = master.getInitialLoadURL(replicables);
            logger.info("Initial load URL is " + initialLoadURL);
            // start receiving messages already now, but start in suspended mode
            replicator = new ReplicationReceiverImpl(master, replicablesProvider, /* startSuspended */ true, consumer);
            // clear Replicable state here, before starting to receive and de-serialize operations which builds up
            // new state, e.g., in competitor store
            for (Replicable<?, ?> r : replicables) {
                r.clearReplicaState();
                r.setUnsentOperationToMasterSender(this);
                r.startedReplicatingFrom(master);
            }
            replicatorThread = new Thread(replicator, "Replicator receiving from " + master.getMessagingHostname() + "/"
                    + master.getExchangeName());
            replicatorThread.start();
            logger.info("Started replicator thread");
            final URLConnection initialLoadConnection = HttpUrlConnectionHelper
                    .redirectConnectionWithBearerToken(initialLoadURL, /* HTTP request method */ "POST", master.getBearerToken());
            final InputStream is = (InputStream) initialLoadConnection.getContent();
            final String queueName;
            try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, HttpUrlConnectionHelper.getCharsetFromConnectionOrDefault(initialLoadConnection, "UTF-8")))) {
                queueName = bufferedReader.readLine();
            }
            final Channel channel = master.createChannel();
            initialLoadChannels.put(master, new InitialLoadRequest(channel, replicables, queueName));
            final RabbitInputStreamProvider rabbitInputStreamProvider = new RabbitInputStreamProvider(channel, queueName);
            try {
                final LZ4BlockInputStream uncompressingInputStream = new LZ4BlockInputStream(rabbitInputStreamProvider.getInputStream());
                for (Replicable<?, ?> replicable : replicables) { // absolutely make sure to use the same sequence of
                                                                  // replicables as for URL (bug 3015)
                    logger.info("Starting to receive initial load for " + replicable.getId());
                    try {
                        replicable.initiallyFillFrom(uncompressingInputStream);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Exception trying to receive initial load for " + replicable.getId(), e);
                        throw e;
                    }
                    logger.info("Done receiving initial load for " + replicable.getId());
                }
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Error while receiving initial load from "+master+". Cleaning up.", e);
                deregisterReplicaWithMaster(master);
                replicator.stop(/* applyQueuedMessages */ false);
                throw e;
            } finally {
                logger.info("Closing channel " + channel + "'s connection " + channel.getConnection());
                channel.getConnection().close();
                logger.info("Resuming replicator to apply queues");
                replicator.setSuspended(false); // apply queued operations
                // delete initial load queue
                DeleteOk deleteOk = consumer.getChannel().queueDelete(queueName);
                logger.info("Deleted queue " + queueName + " used for initial load: " + deleteOk.toString());
                initialLoadChannels.remove(master);
            }
        }
    }

    /**
     * @return the UUID that the master generated for this client which is also entered into {@link #replicaUUIDs}
     */
    private String registerReplicaWithMaster(ReplicationMasterDescriptor master) throws Exception {
        URL replicationRegistrationRequestURL = master.getReplicationRegistrationRequestURL(getServerIdentifier(), ServerInfo.getBuildVersion());
        logger.info("Replication registration request URL: "+replicationRegistrationRequestURL);
        final URLConnection registrationRequestConnection = HttpUrlConnectionHelper
                .redirectConnectionWithBearerToken(replicationRegistrationRequestURL, /* HTTP method */ "POST", master.getBearerToken());
        final InputStream content = (InputStream) registrationRequestConnection.getContent();
        final StringBuilder uuid = new StringBuilder();
        final byte[] buf = new byte[256];
        int read = content.read(buf);
        while (read != -1) {
            uuid.append(new String(buf, 0, read));
            try {
                read = content.read(buf);
            } catch (SocketException e) {
                // the connection may have been closed already; interpret this as the end of the stream
                read = -1;
            }
        }
        final String replicaUUID = uuid.toString();
        logger.info("Obtained replica UUID "+replicaUUID+" from master");
        registerReplicaUuidForMaster(replicaUUID, master);
        return replicaUUID;
    }

    protected void deregisterReplicaWithMaster(ReplicationMasterDescriptor master) {
        try {
            URL replicationDeRegistrationRequestURL = master
                    .getReplicationDeRegistrationRequestURL(getServerIdentifier());
            logger.info("Unregistering replica from master "+master+" using URL "+replicationDeRegistrationRequestURL);
            final URLConnection deregistrationRequestConnection = HttpUrlConnectionHelper
                    .redirectConnectionWithBearerToken(replicationDeRegistrationRequestURL, /* HTTP method */ "POST", master.getBearerToken());
            StringBuilder uuid = new StringBuilder();
            InputStream content = (InputStream) deregistrationRequestConnection.getContent();
            byte[] buf = new byte[256];
            int read = content.read(buf);
            while (read != -1) {
                uuid.append(new String(buf, 0, read));
                read = content.read(buf);
            }
            content.close();
            for (Replicable<?, ?> r : getReplicables()) {
                logger.info("Telling replicable "+r+" that it no longer replicates from master "+master);
                r.stoppedReplicatingFrom(master);
            }
        } catch (Exception ex) {
            // ignore exceptions here - they will mostly be caused by an incompatible server
            // it is also not problematic if the server does not get this deregistration
            // a new registration will overwrite the current one
        }
    }

    protected void registerReplicaUuidForMaster(String uuid, ReplicationMasterDescriptor master) {
        replicaUUIDs.put(master, uuid);
    }

    @Override
    public Map<Class<? extends OperationWithResult<?, ?>>, Integer> getStatistics(ReplicaDescriptor replicaDescriptor) {
        return replicationInstancesManager.getStatistics(replicaDescriptor);
    }

    @Override
    public double getAverageNumberOfOperationsPerMessage(ReplicaDescriptor replicaDescriptor) {
        return replicationInstancesManager.getAverageNumberOfOperationsPerMessage(replicaDescriptor);
    }

    @Override
    public long getNumberOfMessagesSent(ReplicaDescriptor replica) {
        return replicationInstancesManager.getNumberOfMessagesSent(replica);
    }

    @Override
    public long getNumberOfBytesSent(ReplicaDescriptor replica) {
        return replicationInstancesManager.getNumberOfBytesSent(replica);
    }

    @Override
    public double getAverageNumberOfBytesPerMessage(ReplicaDescriptor replica) {
        return replicationInstancesManager.getAverageNumberOfBytesPerMessage(replica);
    }

    @Override
    public Iterable<Replicable<?, ?>> getAllReplicables() {
        return getReplicablesProvider().getReplicables();
    }

    @Override
    public void stopToReplicateFromMaster() throws IOException {
        ReplicationMasterDescriptor descriptor = getReplicatingFromMaster();
        if (descriptor != null) {
            final InitialLoadRequest initialLoad = initialLoadChannels.get(descriptor);
            if (initialLoad != null) {
                try {
                    final Channel channelForInitialLoad = initialLoad.getChannelForInitialLoad();
                    // delete initial load queue
                    DeleteOk deleteOk = channelForInitialLoad.queueDelete(initialLoad.getQueueName());
                    logger.info("Deleted queue " + initialLoad.getQueueName()+ " used for initial load: " + deleteOk.toString());
                    initialLoadChannels.remove(descriptor);
                    channelForInitialLoad.getConnection().close();
                    channelForInitialLoad.close();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error trying to close initial load channel / connection to message queueing system", e);
                    // let's continue trying to de-register the replica from master; re-throwing the
                    // exception wouldn't make much sense here; rather try to clean up as much as
                    // possible
                }
            }
            synchronized (replicaUUIDs) {
                if (replicator != null) {
                    replicator.stop(/* applyQueuedMessages */ true);
                    deregisterReplicaWithMaster(descriptor);
                    replicatingFromMaster = null;
                    replicaUUIDs.clear();

                    // this is needed because QueuingConsumer.nextDelivery() won't unblock
                    // if the connection is closed by application.
                    replicatorThread.interrupt();
                    replicator = null;
                }
            }
        }
    }

    @Override
    public void stopAllReplicas() throws IOException {
        if (replicationInstancesManager.hasReplicas()) {
            replicationInstancesManager.removeAll();
            removeAsListenerFromReplicables();
            closeOutboundReplicationChannel();
            logger.info("Unregistered all replicas from this server!");
        }
    }

    private synchronized void closeOutboundReplicationChannel() throws IOException {
        if (masterChannel != null) {
            masterChannel.getConnection().close();
            masterChannel = null;
        }
        outboundBufferPoolForOperationsAllowingForAsynchronousExecution.stop();
    }

    @Override
    public UUID getServerIdentifier() {
        return serverUUID;
    }

    @Override
    public ReplicationMasterDescriptor createReplicationMasterDescriptor(String messagingHostname, String hostname,
            String exchangeName, int servletPort, int messagingPort, String queueName, String bearerToken,
            Iterable<Replicable<?, ?>> replicables) {
        return new ReplicationMasterDescriptorImpl(messagingHostname, exchangeName, messagingPort, queueName, hostname,
                servletPort, bearerToken, replicables);
    }

    
    @Override
    public void addReplicationStartingListener(ReplicationStartingListener listener) {
        synchronized (replicationStartingListeners) {
            replicationStartingListeners.add(listener);
        }
    }

    @Override
    public void removeReplicationStartingListener(ReplicationStartingListener listener) {
        synchronized (replicationStartingListeners) {
            replicationStartingListeners.remove(listener);
        }
    }

    @Override
    public void setReplicationStarting(boolean newReplicationStarting) {
        synchronized (replicationStartingListeners) {
            if (this.replicationStarting != newReplicationStarting) {
                this.replicationStarting = newReplicationStarting;
                for (final ReplicationStartingListener listener : replicationStartingListeners) {
                    listener.onReplicationStartingChanged(newReplicationStarting);
                }
            }
        }
    }
    
    @Override
    public boolean isReplicationStarting() {
        return this.replicationStarting;
    }

    @Override
    public <S, O extends OperationWithResult<S, ?>, T> void scheduleForSending(O operationWithResult, OperationsToMasterSender<S, O> sender) {
        unsentOperationsSenderJob.scheduleForSending(operationWithResult, sender);
    }

    @Override
    public synchronized ReplicationStatus getStatus() {
        final ReplicationReceiver replicationReceiver = getReplicator();
        final boolean isReplicationStarting = isReplicationStarting();
        final boolean isReplica = isReplicationStarting || replicationReceiver != null;
        final boolean suspended = replicationReceiver == null ? false : replicationReceiver.isSuspended();
        final boolean stopped = replicationReceiver == null ? false : replicationReceiver.isBeingStopped();
        long messageQueueLength;
        if (replicationReceiver == null) {
            messageQueueLength = 0;
        } else {
            try {
                messageQueueLength = replicationReceiver.getMessageQueueSize();
            } catch (IllegalAccessException e) {
                logger.warning("Unable to access replication message queue size: "+e.getMessage()+". Reporting as -1");
                messageQueueLength = -1;
            }
        }
        final Map<String, Integer> operationQueueLengths = replicationReceiver == null ? new HashMap<>() : replicationReceiver.getOperationQueueSizes();
        final Map<String, Boolean> isInitialLoadRunning = new HashMap<>();
        for (final Replicable<?, ?> replicable : getAllReplicables()) {
            isInitialLoadRunning.put(replicable.getId().toString(), replicable.isCurrentlyFillingFromInitialLoad());
        }
        return new ReplicationStatusImpl(isReplica, ServerInfo.getName(), isReplicationStarting, suspended, stopped,
                messageQueueLength, isInitialLoadRunning, operationQueueLengths, getReplicatingFromMaster(), getReplicaInfo(), exchangeName, exchangePort);
    }

    public void stop() {
        logger.info("terminating timer " + timer);
        timer.cancel();
    }
}
