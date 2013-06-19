package com.sap.sailing.server.replication.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.osgi.util.tracker.ServiceTracker;

import com.rabbitmq.client.AMQP.Exchange;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.sap.sailing.server.OperationExecutionListener;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;
import com.sap.sailing.server.replication.ReplicationMasterDescriptor;
import com.sap.sailing.server.replication.ReplicationService;

/**
 * Can observe a {@link RacingEventService} for the operations it performs that require replication. Only observes as
 * long as there are replicas registered. If the last replica is de-registered, the service stops observing the
 * {@link RacingEventService}. Operations received that require replication are sent to the {@link Exchange} to which
 * replica queues can bind. The exchange name is provided to this service during construction.
 * <p>
 * 
 * This service object {@link RacingEventService#addOperationExecutionListener(OperationExecutionListener) registers} as
 * listener at the {@link RacingEventService} so it {@link #executed(RacingEventServiceOperation) receives}
 * notifications about operations executed by the {@link RacingEventService} that require replication if and only if
 * there is at least one replica registered.
 * 
 * @author Frank Mittag, Axel Uhl (d043530)
 * 
 */
public class ReplicationServiceImpl implements ReplicationService, OperationExecutionListener, HasRacingEventService {
    private static final Logger logger = Logger.getLogger(ReplicationServiceImpl.class.getName());
    
    private final ReplicationInstancesManager replicationInstancesManager;
    
    private ServiceTracker<RacingEventService, RacingEventService> racingEventServiceTracker;
    
    private final RacingEventService localService;
    
    /**
     * <code>null</code>, if this instance is not currently replicating from some master; the master's descriptor otherwise
     */
    private ReplicationMasterDescriptor replicatingFromMaster;
    
    /**
     * The UUIDs with which this replica is registered by the master identified by the corresponding key
     */
    private final Map<ReplicationMasterDescriptor, String> replicaUUIDs;
    
    /**
     * Channel used by a master server to publish replication operations; <code>null</code> in servers that don't have replicas registered
     */
    private Channel masterChannel;
    
    /**
     * The name of the RabbitMQ exchange to which this replication service sends its replication operations in
     * serialized form. Clients need to know this name to be able to bind their queues to the exchange.
     */
    private final String exchangeName;
    
    public ReplicationServiceImpl(String exchangeName, final ReplicationInstancesManager replicationInstancesManager) throws IOException {
        this.replicationInstancesManager = replicationInstancesManager;
        replicaUUIDs = new HashMap<ReplicationMasterDescriptor, String>();
        racingEventServiceTracker = new ServiceTracker<RacingEventService, RacingEventService>(
                Activator.getDefaultContext(), RacingEventService.class.getName(), null);
        racingEventServiceTracker.open();
        localService = null;
        this.exchangeName = exchangeName;
    }
    
    /**
     * Like {@link #ReplicationServiceImpl(String, ReplicationInstancesManager)}, only that instead of using
     * an OSGi service tracker to discover the {@link RacingEventService}, the service to replicate is "injected" here.
     * @param exchangeName the name of the exchange to which replicas can bind
     */
    public ReplicationServiceImpl(String exchangeName,
            final ReplicationInstancesManager replicationInstancesManager, RacingEventService localService) throws IOException {
        this.replicationInstancesManager = replicationInstancesManager;
        replicaUUIDs = new HashMap<ReplicationMasterDescriptor, String>();
        this.localService = localService;
        this.exchangeName = exchangeName;
    }
    
    private Channel createMasterChannel(String exchangeName) throws IOException {
        final ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost"); // ...and use default port
        
        Channel result = null;
        try {
            result = connectionFactory.newConnection().createChannel();
        } catch (ConnectException ex) {
            // make sure to log something meaningful
            logger.severe("Could not connect to messaging queue on " + connectionFactory.getHost() + ":" + connectionFactory.getPort() + "/" + exchangeName);
            throw ex;
        }
        
        logger.info("Connected to " + connectionFactory.getHost() + ":" + connectionFactory.getPort() + "/" + exchangeName);
        result.exchangeDeclare(exchangeName, "fanout");
        return result;
    }

    @Override
    public RacingEventService getRacingEventService() {
        RacingEventService result;
        if (localService != null) {
            result = localService;
        } else {
            result = racingEventServiceTracker.getService();
        }
        return result;
    }

    @Override
    public void registerReplica(ReplicaDescriptor replica) throws IOException {
        if (!replicationInstancesManager.hasReplicas()) {
            addAsListenerToRacingEventService();
            synchronized (this) {
                if (masterChannel == null) {
                    masterChannel = createMasterChannel(exchangeName);
                }
            }
        }
        replicationInstancesManager.registerReplica(replica);
    }
    
    private void addAsListenerToRacingEventService() {
        getRacingEventService().addOperationExecutionListener(this);
    }

    @Override
    public void unregisterReplica(ReplicaDescriptor replica) throws IOException {
        replicationInstancesManager.unregisterReplica(replica);
        if (!replicationInstancesManager.hasReplicas()) {
            removeAsListenerFromRacingEventService();
            synchronized (this) {
                masterChannel.close();
                masterChannel = null;
            }
        }
    }

    private void removeAsListenerFromRacingEventService() {
        getRacingEventService().removeOperationExecutionListener(this);
    }

    private void broadcastOperation(RacingEventServiceOperation<?> operation) throws Exception {
        // serialize operation into message
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(operation);
        oos.close();
        if (masterChannel != null) {
            masterChannel.basicPublish(exchangeName, /* routingKey */"", /* properties */null, bos.toByteArray());
            replicationInstancesManager.log(operation);
        }
    }

    @Override
    public Iterable<ReplicaDescriptor> getReplicaInfo() {
        return replicationInstancesManager.getReplicaDescriptors();
    }

    @Override
    public ReplicationMasterDescriptor isReplicatingFromMaster() {
        return replicatingFromMaster;
    }

    @Override
    public void startToReplicateFrom(ReplicationMasterDescriptor master) throws IOException, ClassNotFoundException, InterruptedException {
        logger.info("Starting to replicate from "+master);
        replicatingFromMaster = master;
        registerReplicaWithMaster(master);
        QueueingConsumer consumer = master.getConsumer();
        URL initialLoadURL = master.getInitialLoadURL();
        logger.info("Initial load URL is "+initialLoadURL);
        final Replicator replicator = new Replicator(master, this, /* startSuspended */ true, consumer);
        // start receiving messages already now, but start in suspended mode
        new Thread(replicator, "Replicator receiving from "+master.getHostname()+"/"+master.getExchangeName()).start();
        logger.info("Started replicator thread");
        InputStream is = initialLoadURL.openStream();
        final RacingEventService racingEventService = getRacingEventService();
        ObjectInputStream ois = racingEventService.getBaseDomainFactory().createObjectInputStreamResolvingAgainstThisFactory(is);
        logger.info("Starting to receive initial load");
        racingEventService.initiallyFillFrom(ois);
        logger.info("Done receiving initial load");
        replicator.setSuspended(false); // apply queued operations
    }

    /**
     * @return the UUID that the master generated for this client which is also entered into {@link #replicaUUIDs}
     */
    private String registerReplicaWithMaster(ReplicationMasterDescriptor master) throws IOException, ClassNotFoundException {
        URL replicationRegistrationRequestURL = master.getReplicationRegistrationRequestURL();
        final URLConnection registrationRequestConnection = replicationRegistrationRequestURL.openConnection();
        registrationRequestConnection.connect();
        InputStream content = (InputStream) registrationRequestConnection.getContent();
        StringBuilder uuid = new StringBuilder();
        byte[] buf = new byte[256];
        int read = content.read(buf);
        while (read != -1) {
            uuid.append(new String(buf, 0, read));
            read = content.read(buf);
        }
        String replicaUUID = uuid.toString();
        registerReplicaUuidForMaster(replicaUUID, master);
        return replicaUUID;
    }

    @Override
    public <T> void executed(RacingEventServiceOperation<T> operation) {
        try {
            broadcastOperation(operation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void registerReplicaUuidForMaster(String uuid, ReplicationMasterDescriptor master) {
        replicaUUIDs.put(master, uuid);
    }

    @Override
    public Map<Class<? extends RacingEventServiceOperation<?>>, Integer> getStatistics(ReplicaDescriptor replicaDescriptor) {
        return replicationInstancesManager.getStatistics(replicaDescriptor);
    }

}
