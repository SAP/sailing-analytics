package com.sap.sailing.server.replication.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;

import com.rabbitmq.client.QueueingConsumer;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.media.MediaDBFactory;
import com.sap.sailing.mongodb.MongoDBService;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.replication.ReplicationMasterDescriptor;
import com.sap.sailing.server.replication.ReplicationService;
import com.sap.sailing.server.replication.impl.ReplicaDescriptor;
import com.sap.sailing.server.replication.impl.ReplicationInstancesManager;
import com.sap.sailing.server.replication.impl.ReplicationMasterDescriptorImpl;
import com.sap.sailing.server.replication.impl.ReplicationServiceImpl;
import com.sap.sailing.server.replication.impl.Replicator;

public abstract class AbstractServerReplicationTest {
    protected static final int SERVLET_PORT = 9990;
    private DomainFactory resolveAgainst;
    protected RacingEventServiceImpl replica;
    protected RacingEventServiceImpl master;
    protected ReplicationServiceTestImpl replicaReplicator;
    private ReplicaDescriptor replicaDescriptor;
    private ReplicationServiceImpl masterReplicator;
    private ReplicationMasterDescriptor  masterDescriptor;
    
    /**
     * Drops the test DB. Sets up master and replica, starts the JMS message broker and registers the replica with the master.
     */
    @Before
    public void setUp() throws Exception {
        try {
            Pair<ReplicationServiceTestImpl, ReplicationMasterDescriptor> result = basicSetUp(
                    true, /* master=null means create a new one */ null,
            /* replica=null means create a new one */null);
            result.getA().startToReplicateFrom(result.getB());
        } catch (Exception e) {
            tearDown();
            throw e;
        }
    }

    /**
     * Drops the test DB.
     * 
     * @param master
     *            if not <code>null</code>, the value will be used for {@link #master}; otherwise, a new racing event
     *            service will be created as master
     * @param replica
     *            if not <code>null</code>, the value will be used for {@link #replica}; otherwise, a new racing event
     *            service will be created as replica
     */
    protected Pair<ReplicationServiceTestImpl, ReplicationMasterDescriptor> basicSetUp(
            boolean dropDB, RacingEventServiceImpl master, RacingEventServiceImpl replica) throws IOException, InterruptedException {
        final String exchangeName = "test-sapsailinganalytics-exchange";
        final String exchangeHost = "localhost";
        final UUID serverUuid = UUID.randomUUID();
        final MongoDBService mongoDBService = MongoDBService.INSTANCE;
        if (dropDB) {
            mongoDBService.getDB().dropDatabase();
        }
        resolveAgainst = DomainFactory.INSTANCE;
        if (master != null) {
            this.master = master;
        } else {
            this.master = new RacingEventServiceImpl(PersistenceFactory.INSTANCE.getDomainObjectFactory(mongoDBService, DomainFactory.INSTANCE), PersistenceFactory.INSTANCE
                    .getMongoObjectFactory(mongoDBService), MediaDBFactory.INSTANCE.getMediaDB(mongoDBService));
        }
        if (replica != null) {
            this.replica = replica;
        } else {
            this.replica = new RacingEventServiceImpl(PersistenceFactory.INSTANCE.getDomainObjectFactory(mongoDBService, DomainFactory.INSTANCE), PersistenceFactory.INSTANCE
                    .getMongoObjectFactory(mongoDBService), MediaDBFactory.INSTANCE.getMediaDB(mongoDBService));
        }
        ReplicationInstancesManager rim = new ReplicationInstancesManager();
        masterReplicator = new ReplicationServiceImpl(exchangeName, exchangeHost, rim, this.master);
        replicaDescriptor = new ReplicaDescriptor(InetAddress.getLocalHost(), serverUuid, "");
        masterReplicator.registerReplica(replicaDescriptor);
        masterDescriptor = new ReplicationMasterDescriptorImpl("localhost", exchangeName, SERVLET_PORT, 0, UUID.randomUUID().toString());
        ReplicationServiceTestImpl replicaReplicator = new ReplicationServiceTestImpl(exchangeName, exchangeHost, resolveAgainst, rim,
                replicaDescriptor, this.replica, this.master, masterReplicator, masterDescriptor);
        Pair<ReplicationServiceTestImpl, ReplicationMasterDescriptor> result = new Pair<>(replicaReplicator, masterDescriptor);
        replicaReplicator.startInitialLoadTransmissionServlet();
        this.replicaReplicator = replicaReplicator; 
        return result;
    }
    
    @After
    public void tearDown() throws Exception {
        masterReplicator.unregisterReplica(replicaDescriptor);
        masterDescriptor.stopConnection();
        try {
            URLConnection urlConnection = new URL("http://localhost:"+SERVLET_PORT+"/STOP").openConnection(); // stop the initial load test server thread
            urlConnection.getInputStream().close();
        } catch (ConnectException ex) {
            /* do not make tests fail because of a server that has been shut down
             * or when an exception occured (see setUp()) - let the
             * original exception propagate */
            ex.printStackTrace();
        }
    }
    
    public void stopReplicatingToMaster() throws IOException {
        replicaReplicator.stopToReplicateFromMaster();
    }
    
    static class ReplicationServiceTestImpl extends ReplicationServiceImpl {
        private final DomainFactory resolveAgainst;
        private final RacingEventService master;
        private final ReplicaDescriptor replicaDescriptor;
        private final ReplicationService masterReplicationService;
        private final ReplicationMasterDescriptor masterDescriptor;
        
        public ReplicationServiceTestImpl(String exchangeName, String exchangeHost, DomainFactory resolveAgainst,
                ReplicationInstancesManager replicationInstancesManager, ReplicaDescriptor replicaDescriptor,
                RacingEventService replica, RacingEventService master, ReplicationService masterReplicationService,
                ReplicationMasterDescriptor masterDescriptor)
                throws IOException {
            super(exchangeName, exchangeHost, replicationInstancesManager, replica);
            this.resolveAgainst = resolveAgainst;
            this.replicaDescriptor = replicaDescriptor;
            this.master = master;
            this.masterReplicationService = masterReplicationService;
            this.masterDescriptor = masterDescriptor;
        }
        
        private void startInitialLoadTransmissionServlet() throws InterruptedException {
            final boolean[] listening = new boolean[] { false };
            new Thread("Replication initial load test server") {
                public void run() {
                    ServerSocket ss;
                    try {
                        ss = new ServerSocket(SERVLET_PORT);
                        synchronized (listening) {
                            listening[0] = true;
                            listening.notifyAll();
                        }
                        boolean stop = false;
                        while (!stop) {
                            Socket s = ss.accept();
                            String request = new BufferedReader(new InputStreamReader(s.getInputStream())).readLine();
                            PrintWriter pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
                            pw.println("HTTP/1.1 200 OK");
                            pw.println("Content-Type: text/plain");
                            pw.println();
                            pw.flush();
                            if (request.contains("DEREGISTER")) {
                                // assuming that it is safe to unregister all replicas for tests
                                for (ReplicaDescriptor descriptor : getReplicaInfo()) {
                                    unregisterReplica(descriptor);
                                }
                            } else if (request.contains("REGISTER")) {
                                final String uuid = UUID.randomUUID().toString();
                                registerReplicaUuidForMaster(uuid, masterDescriptor);
                                pw.print(uuid.getBytes());
                            } else if (request.contains("INITIAL_LOAD")) {
                                master.serializeForInitialReplication(new ObjectOutputStream(s.getOutputStream()));
                            } else if (request.contains("STOP")) {
                                stop = true;
                            }
                            pw.close();
                            s.close();
                        }
                        ss.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.start();
            synchronized (listening) {
                while (!listening[0]) {
                    listening.wait();
                }
            }
        }

        /**
         * Ignore the master descriptor and replicate from the local master passed to the constructor instead.
         */
//        @Override
//        public void startToReplicateFrom(ReplicationMasterDescriptor master) throws IOException,
//                ClassNotFoundException {
//            Replicator replicator = startToReplicateFromButDontYetFetchInitialLoad(master, /* startReplicatorSuspended */ true);
//            initialLoad();
//            replicator.setSuspended(false); // resume after initial load
//        }

        protected Replicator startToReplicateFromButDontYetFetchInitialLoad(ReplicationMasterDescriptor master, boolean startReplicatorSuspended)
                throws IOException {
            masterReplicationService.registerReplica(replicaDescriptor);
            registerReplicaUuidForMaster(replicaDescriptor.getUuid().toString(), master);
            QueueingConsumer consumer = master.getConsumer();
            final Replicator replicator = new Replicator(master, this, startReplicatorSuspended, consumer, DomainFactory.INSTANCE);
            new Thread(replicator).start();
            return replicator;
        }
        
        /**
         * Clones the {@link #master}'s state to the {@link #replica} using
         * {@link RacingEventServiceImpl#serializeForInitialReplication(ObjectOutputStream)} and
         * {@link RacingEventServiceImpl#initiallyFillFrom(ObjectInputStream)} through a piped input/output stream.
         * @throws InterruptedException 
         */
        protected void initialLoad() throws IOException, ClassNotFoundException, InterruptedException {
            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            final ObjectOutputStream oos = new ObjectOutputStream(pos);
            new Thread("clone writer") {
                public void run() {
                    try {
                        master.serializeForInitialReplication(oos);
                        oos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            }.start();
            ObjectInputStream dis = resolveAgainst.createObjectInputStreamResolvingAgainstThisFactory(pis);
            getRacingEventService().initiallyFillFrom(dis);
            dis.close();
        }
    }
}
