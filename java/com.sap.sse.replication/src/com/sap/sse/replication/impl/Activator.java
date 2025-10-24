package com.sap.sse.replication.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.sap.sse.common.Util;
import com.sap.sse.replication.ReplicaDescriptor;
import com.sap.sse.replication.Replicable;
import com.sap.sse.replication.ReplicablesProvider;
import com.sap.sse.replication.ReplicationService;
import com.sap.sse.replication.persistence.PersistenceFactory;
import com.sap.sse.security.util.RemoteServerUtil;

/**
 * Initializes the replication sub-system. A hierarchy of parameter specifications is evaluated to configure this
 * server's behavior as a master as well as for configuring this server as a replica that automatically starts
 * replicating from another master.
 * <p>
 * 
 * Properties to control replication when this instance is the master:
 * <ul>
 * <li><code>replication.exchangeHost</code>: name of the host running the RabbitMQ messaging server through which to
 * send out the replication messages to the replicas. If this property is not defined, the value of the environment
 * variable <code>REPLICATION_HOST</code> is used instead. If that isn't defined either, <code>"localhost"</code> will
 * be used as the default value.</li>
 * <li><code>replication.exchangePort</code>: port to use to reach the RabbitMQ messaging server; use 0 for RabbitMQ's
 * default port. If this property isn't defined, the value of the environment variable <code>REPLICATION_PORT</code>
 * will be used instead. If that isn't defined either, 0 will be used as the default.</li>
 * <li><code>replication.exchangeName</code>: name of the fan-out exchange to which to write the replication messages.
 * Replicas connect to this exchange using a queue. If this property is not found, the value of the environment variable
 * <code>REPLICATION_CHANNEL</code> is used instead. If that variable isn't defined either, the default exchange name is
 * set to <code>"sapsailinganalytics"</code>.</li>
 * </ul>
 * <p>
 * 
 * Properties to control replication when replication shall automatically be started, replicating from a specific master
 * server:
 * <ul>
 * <li><code>replicate.on.start</code>: use a comma-separated list of fully-qualified class names of the
 * {@link Replicable} objects you want to replicate from a master server to start replication from a specific master
 * when this instance is started; the following parameters are only evaluated if this property is present and not
 * empty.</li>
 * <li><code>replicate.master.servlet.host</code>: the host name to use for the HTTP connection through which the
 * request to register this replica with the master is sent to the master and the queue name for receiving the initial
 * load is requested</li>
 * <li><code>replicate.master.servlet.port</code>: the port for the HTTP connection described above</li>
 * <li><code>replicate.master.queue.host</code>: the name of the host running the RabbitMQ server; this replica will
 * connect to a fan-out exchange running on that server to receive the replication messages from the master</li>
 * <li><code>replicate.master.queue.port</code>: the port for connecting to the RabbitMQ server; use 0 to connect to
 * Rabbit's default port</li>
 * <li><code>replicate.master.exchange.name</code>: name of the fan-out exchange that the remote master has created on
 * the RabbitMQ messaging system to which this replica connects. If missing, defaults to what this instance uses as its
 * own master exchange name, as described above, based on the <code>replication.exchangeName</code> property, the
 * <code>REPLICATION_CHANNEL</code> environment variable and the ultimate default name
 * <code>"sapsailinganalytics"</code>.</li>
 * <li><code>replicate.master.username</code> and <code>replicate.master.password</code>: define the credentials to
 * login to the master instance. The given user needs to have the permission {@code SERVER:REPLICATE:<server-name>}
 * granted to be able to initiate the replication.
 * </ul>
 * Note that there are no default values for the properties that control automatic replication. If you provide the
 * <code>replicate.on.start</code> property with <code>true</code> as the value, all other
 * <code>replicate.master.*</code> properties must have a non-empty value defined.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class Activator implements BundleActivator {
    private static final Logger logger = Logger.getLogger(Activator.class.getName());
    
    private static final String PROPERTY_NAME_EXCHANGE_HOST = "replication.exchangeHost";
    private static final String PROPERTY_NAME_EXCHANGE_PORT = "replication.exchangePort";
    private static final String PROPERTY_NAME_EXCHANGE_NAME = "replication.exchangeName";
    
    public static final String ENV_VAR_NAME_REPLICATION_CHANNEL = "REPLICATION_CHANNEL";
    public static final String ENV_VAR_NAME_REPLICATION_HOST = "REPLICATION_HOST";
    public static final String ENV_VAR_NAME_REPLICATION_PORT = "REPLICATION_PORT";
    
    public static final String PROPERTY_NAME_REPLICATE_ON_START = "replicate.on.start";
    public static final String PROPERTY_NAME_RESTORE_REPLICAS = "replicate.restore.replicas";
    public static final String PROPERTY_NAME_REPLICATE_MASTER_SERVLET_HOST = "replicate.master.servlet.host";
    public static final String PROPERTY_NAME_REPLICATE_MASTER_SERVLET_PORT = "replicate.master.servlet.port";
    public static final String PROPERTY_NAME_REPLICATE_MASTER_QUEUE_HOST = "replicate.master.queue.host";
    public static final String PROPERTY_NAME_REPLICATE_MASTER_QUEUE_PORT = "replicate.master.queue.port";
    public static final String PROPERTY_NAME_REPLICATE_MASTER_EXCHANGE_NAME = "replicate.master.exchange.name";
    public static final String PROPERTY_NAME_REPLICATE_MASTER_USERNAME = "replicate.master.username";
    public static final String PROPERTY_NAME_REPLICATE_MASTER_PASSWORD = "replicate.master.password";
    public static final String PROPERTY_NAME_REPLICATE_MASTER_BEARER_TOKEN = "replicate.master.bearer_token";

    private ReplicationInstancesManager replicationInstancesManager;

    private ReplicationServiceImpl serverReplicationMasterService;
    
    private static BundleContext defaultContext;
    
    public void start(BundleContext bundleContext) throws Exception {
        defaultContext = bundleContext;
        String exchangeName = bundleContext.getProperty(PROPERTY_NAME_EXCHANGE_NAME);
        String exchangeHost = bundleContext.getProperty(PROPERTY_NAME_EXCHANGE_HOST);
        String exchangePortAsString = bundleContext.getProperty(PROPERTY_NAME_EXCHANGE_PORT);
        if (exchangeName == null) {
            if (System.getenv(ENV_VAR_NAME_REPLICATION_CHANNEL) == null) {
                exchangeName = "sapsailinganalytics";
            } else {
                exchangeName = System.getenv(ENV_VAR_NAME_REPLICATION_CHANNEL);
            }
        }
        if (exchangeHost == null) {
            if (System.getenv(ENV_VAR_NAME_REPLICATION_HOST) == null) {
                exchangeHost = "localhost";
            } else {
                exchangeHost = System.getenv(ENV_VAR_NAME_REPLICATION_HOST);
            }
        }
        if (exchangePortAsString == null) {
            if (System.getenv(ENV_VAR_NAME_REPLICATION_PORT) == null) {
                exchangePortAsString = "0";
            } else {
                exchangePortAsString = System.getenv(ENV_VAR_NAME_REPLICATION_PORT);
            }
        }
        int exchangePort = 0;
        try {
            exchangePort = Integer.valueOf(exchangePortAsString);
        } catch (NumberFormatException nfe) {
            logger.severe("Couldn't parse the replication port specification \""+exchangePortAsString+"\". Using default.");
        }
        final Iterable<ReplicaDescriptor> replicasToAssumeConnectedToThisMaster;
        if (Boolean.valueOf(System.getProperty(PROPERTY_NAME_RESTORE_REPLICAS, "true"))) {
            replicasToAssumeConnectedToThisMaster = PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory().loadReplicaDescriptors();
        } else {
            replicasToAssumeConnectedToThisMaster = null;
        }
        replicationInstancesManager = new ReplicationInstancesManager();
        final OSGiReplicableTracker replicablesProvider = new OSGiReplicableTracker(bundleContext);
        serverReplicationMasterService = new ReplicationServiceImpl(replicasToAssumeConnectedToThisMaster,
                Optional.of(PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory()),
                exchangeName, exchangeHost, exchangePort, replicationInstancesManager, replicablesProvider);
        final String replicateOnStart = System.getProperty(PROPERTY_NAME_REPLICATE_ON_START);
        final boolean autoReplicationRequested = replicateOnStart != null && !replicateOnStart.isEmpty();
        if (autoReplicationRequested) {
            // set before registering the service; clients discovering it therefore cannot accidentally
            // discover it before this flag has been set.
            serverReplicationMasterService.setReplicationStarting(true);
        }
        bundleContext.registerService(ReplicationService.class, serverReplicationMasterService, null);
        logger.info("Registered replication service "+serverReplicationMasterService+" using exchange name "+exchangeName+" on host "+exchangeHost);
        if (autoReplicationRequested) {
            triggerAutomaticReplication(serverReplicationMasterService, replicateOnStart, exchangeName, replicablesProvider);
        }
    }
    
    /**
     * Assumes that automatic replication was requested and that the {@code serverReplicationMasterService} has already been
     * marked as {@link ReplicationService#setReplicationStarting(boolean) setReplicationStarting(true)}. When replication
     * has been requested from all replicables, {@link ReplicationService#setReplicationStarting(boolean) setReplicationStarting(false)}
     * will be called by this method to indicate that the start-up sequence for the service has completed; now clients may need
     * to wait for the individual replicables to finish their initial load.
     */
    private void triggerAutomaticReplication(ReplicationService serverReplicationMasterService, String replicateOnStart, String masterExchangeName, final ReplicablesProvider replicablesProvider) {
        assert replicateOnStart != null && !replicateOnStart.isEmpty();
        serverReplicationMasterService.setReplicationStarting(true);
        final String[] replicableIdsAsStrings = replicateOnStart.split(",");
        new Thread("ServiceTracker waiting for Replicables "+Arrays.toString(replicableIdsAsStrings)) {
            @Override
            public void run() {
                try {
                    logger.info("Waiting for Replicables " + Arrays.toString(replicableIdsAsStrings) +
                            " before firing up replication automatically...");
                    final List<Replicable<?, ?>> replicables = new ArrayList<>();
                    for (String replicableIdAsString : replicableIdsAsStrings) {
                        logger.info("Trying to obtain Replicable " + replicableIdAsString+", waiting if necessary");
                        Replicable<?, ?> replicable = replicablesProvider.getReplicable(replicableIdAsString, /* wait */true);
                        logger.info("Obtained Replicable " + replicableIdAsString);
                        replicables.add(replicable);
                    }
                    logger.info("Configuration requested automatic replication for replicables "+
                            Arrays.toString(replicableIdsAsStrings)+". Starting it up...");
                    String replicateFromExchangeName = System.getProperty(PROPERTY_NAME_REPLICATE_MASTER_EXCHANGE_NAME);
                    if (replicateFromExchangeName == null) {
                        replicateFromExchangeName = masterExchangeName;
                    }
                    final String servletHost = System.getProperty(PROPERTY_NAME_REPLICATE_MASTER_SERVLET_HOST);
                    final int servletPort = Integer.valueOf(System.getProperty(PROPERTY_NAME_REPLICATE_MASTER_SERVLET_PORT).trim());
                    final String bearerToken;
                    if (Util.hasLength(System.getProperty(PROPERTY_NAME_REPLICATE_MASTER_BEARER_TOKEN))) {
                        bearerToken = System.getProperty(PROPERTY_NAME_REPLICATE_MASTER_BEARER_TOKEN).trim();
                    } else {
                        bearerToken = RemoteServerUtil.resolveBearerTokenForRemoteServer(servletHost, servletPort,
                                System.getProperty(PROPERTY_NAME_REPLICATE_MASTER_USERNAME),
                                System.getProperty(PROPERTY_NAME_REPLICATE_MASTER_PASSWORD));
                    }
                    ReplicationMasterDescriptorImpl master = new ReplicationMasterDescriptorImpl(
                            System.getProperty(PROPERTY_NAME_REPLICATE_MASTER_QUEUE_HOST),
                            replicateFromExchangeName,
                            Integer.valueOf(System.getProperty(PROPERTY_NAME_REPLICATE_MASTER_QUEUE_PORT).trim()), 
                            serverReplicationMasterService.getServerIdentifier().toString(), 
                            servletHost, servletPort, bearerToken, replicables);
                    try {
                        serverReplicationMasterService.startToReplicateFrom(master);
                        serverReplicationMasterService.setReplicationStarting(false);
                        logger.info("Automatic replication has been started.");
                    } catch (ClassNotFoundException | IOException | InterruptedException e) {
                        logger.log(Level.SEVERE, "Error with automatic replication from "+master, e);
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error starting automatic replication", e);
                }
            }
        }.start();
    }

    public void stop(BundleContext bundleContext) throws Exception {
        // stop replicating from a master server
        if (serverReplicationMasterService.getReplicatingFromMaster() != null) {
            serverReplicationMasterService.stopToReplicateFromMaster();
        }
        // stop sending stuff to the exchange for other replicas (if this is a master)
        serverReplicationMasterService.stopAllReplicas();
        serverReplicationMasterService.stop();
    }
    
    public static BundleContext getDefaultContext() {
        return defaultContext;
    }
}