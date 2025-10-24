package com.sap.sse.replication.testsupport;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.replication.ReplicaDescriptor;
import com.sap.sse.replication.Replicable;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.replication.ReplicationReceiver;
import com.sap.sse.replication.ReplicationService;
import com.sap.sse.replication.ReplicationServletActions.Action;
import com.sap.sse.replication.impl.Activator;
import com.sap.sse.replication.impl.RabbitOutputStream;
import com.sap.sse.replication.impl.ReplicationInstancesManager;
import com.sap.sse.replication.impl.ReplicationMasterDescriptorImpl;
import com.sap.sse.replication.impl.ReplicationReceiverImpl;
import com.sap.sse.replication.impl.ReplicationServiceImpl;
import com.sap.sse.replication.impl.ReplicationServlet;
import com.sap.sse.replication.impl.SingletonReplicablesProvider;
import com.sap.sse.replication.interfaces.impl.ReplicaDescriptorImpl;
import com.sap.sse.security.SecurityService;
import net.jpountz.lz4.LZ4BlockOutputStream;

@Timeout(value=5, unit=TimeUnit.MINUTES) // timeout after 5 minutes
public abstract class AbstractServerReplicationTestSetUp<ReplicableInterface extends Replicable<?, ?>, ReplicableImpl extends ReplicableInterface> {
    private static final Logger logger = Logger.getLogger(AbstractServerReplicationTestSetUp.class.getName());
    
    protected static final int DEFAULT_SERVLET_PORT = 9990;
    
    private int servletPort;
    protected ReplicableImpl replica;
    protected ReplicableImpl master;
    protected ReplicationServiceTestImpl<ReplicableInterface> replicaReplicator;
    private ReplicaDescriptor replicaDescriptor;
    private ReplicationServiceImpl masterReplicator;
    protected ReplicationMasterDescriptor masterDescriptor;
    private Thread initialLoadTestServerThread;
    
    private final FullyInitializedReplicableTracker<SecurityService> securityServiceTrackerMock;
    
    protected AbstractServerReplicationTestSetUp() {
        this(/* securityServiceTrackerMock*/ null);
    }
    
    protected AbstractServerReplicationTestSetUp(int servletPort) {
        this(/* securityServiceTrackerMock*/ null);
        this.servletPort = servletPort;
    }
    
    protected AbstractServerReplicationTestSetUp(final FullyInitializedReplicableTracker<SecurityService> securityServiceTrackerMock) {
        this.securityServiceTrackerMock = securityServiceTrackerMock;
    }
    
    public FullyInitializedReplicableTracker<SecurityService> getSecurityServiceTrackerMock() {
        return securityServiceTrackerMock;
    }
    
    /**
     * Sets up master and replica, starts the JMS message broker and registers the replica with the master. If you want
     * to drop the DB in your particular test case first, override {@link #persistenceSetUp(boolean)}. If you don't want
     * replication to start right away for your test, override this method, execute only {@link #basicSetUp(boolean, Replicable, Replicable)},
     * do what you need to do and then explicitly call {@link ReplicationServiceTestImpl#startToReplicateFrom(ReplicationMasterDescriptor)}
     * or {@link ReplicationServiceTestImpl#startToReplicateFromButDontYetFetchInitialLoad(ReplicationMasterDescriptor, boolean)}
     * on the first component returned by {@link #basicSetUp(boolean, Replicable, Replicable)}.
     */
    public void setUp() throws Exception {
        try {
            setUpSecurity();
            Pair<ReplicationServiceTestImpl<ReplicableInterface>, ReplicationMasterDescriptor> result = setUpWithoutStartingToReplicateYet();
            result.getA().startToReplicateFrom(result.getB());
        } catch (Exception e) {
            tearDown();
            throw e;
        }
    }
    
    protected void setUpSecurity() {
        SecurityManager securityManager = Mockito.mock(org.apache.shiro.mgt.SecurityManager.class);
        Subject fakeSubject = Mockito.mock(Subject.class);
        SecurityUtils.setSecurityManager(securityManager);
        Mockito.doReturn(fakeSubject).when(securityManager).createSubject(Mockito.any());
        Mockito.doReturn(true).when(fakeSubject).isAuthenticated();
        Mockito.doNothing().when(fakeSubject).checkPermission(Mockito.anyString());
    }

    /**
     * Sets up master and replica, starts the JMS message broker and registers the replica with the master. If you want
     * to drop the DB in your particular test case first, override {@link #persistenceSetUp(boolean)}. If you don't want
     * replication to start right away for your test, override this method, execute only
     * {@link #basicSetUp(boolean, Replicable, Replicable)}, do what you need to do and then explicitly call
     * {@link ReplicationServiceTestImpl#startToReplicateFrom(ReplicationMasterDescriptor)} or
     * {@link ReplicationServiceTestImpl#startToReplicateFromButDontYetFetchInitialLoad(ReplicationMasterDescriptor, boolean)}
     * on the first component returned by {@link #basicSetUp(boolean, Replicable, Replicable)}.
     * 
     * @return callers may call <code>result.getA().startToReplicateFrom(result.getB())</code> on the result to actually
     *         start replication, e.g., after having done more set-up on the master
     */
    protected Pair<ReplicationServiceTestImpl<ReplicableInterface>, ReplicationMasterDescriptor> setUpWithoutStartingToReplicateYet() throws Exception {
        try {
            Pair<ReplicationServiceTestImpl<ReplicableInterface>, ReplicationMasterDescriptor> result = basicSetUp(
                    /* dropDB */true, /* master=null means create a new one */null,
                    /* replica=null means create a new one */null);
            return result;
        } catch (Exception e) {
            tearDown();
            throw e;
        }
    }

    /**
     * Gives subclasses a hook to set up a persistence component, such as a DB. This implementation does nothing.
     * 
     * @param dropDB if <code>true</code> the contents of the persistence layers are to be cleared
     */
    protected void persistenceSetUp(boolean dropDB) {
    }

    /**
     * Calls {@link #persistenceSetUp(boolean)} first. Doesn't {@link
     * ReplicationServiceImpl#registerReplica(ReplicaDescriptor) register the replica} yet with the master.
     * 
     * @param master
     *            if not <code>null</code>, the value will be used for {@link #master}; otherwise, a new racing event
     *            service will be created as master
     * @param replica
     *            if not <code>null</code>, the value will be used for {@link #replica}; otherwise, a new racing event
     *            service will be created as replica
     */
    protected Pair<ReplicationServiceTestImpl<ReplicableInterface>, ReplicationMasterDescriptor> basicSetUp(boolean dropDB,
            ReplicableImpl master, ReplicableImpl replica) throws Exception {
        logger.info("basicSetUp for test class "+getClass().getName());
        persistenceSetUp(dropDB);
        String exchangeName = "test-sapsailinganalytics-exchange-"+new Random().nextInt();
        String exchangeHost = "localhost";
        if (System.getenv(Activator.ENV_VAR_NAME_REPLICATION_HOST) != null) {
            exchangeHost = System.getenv(Activator.ENV_VAR_NAME_REPLICATION_HOST);
        }
        final UUID serverUuid = UUID.randomUUID();
        if (master != null) {
            this.master = master;
        } else {
            this.master = createNewMaster(securityServiceTrackerMock);
        }
        if (replica != null) {
            this.replica = replica;
        } else {
            this.replica = createNewReplica(securityServiceTrackerMock);
        }
        ReplicationInstancesManager rim = new ReplicationInstancesManager();
        masterReplicator = new ReplicationServiceImpl(exchangeName, exchangeHost, 0, rim, new SingletonReplicablesProvider(this.master));
        replicaDescriptor = new ReplicaDescriptorImpl(InetAddress.getLocalHost(), 8888, serverUuid, "", new String[] { this.master.getId().toString() });
        // connect to exchange host and local server running as master
        // master server and exchange host can be two different hosts
        ReplicationServiceTestImpl<ReplicableInterface> replicaReplicator = new ReplicationServiceTestImpl<ReplicableInterface>(exchangeName, exchangeHost, rim, replicaDescriptor,
                this.replica, this.master, masterReplicator);
        masterDescriptor = replicaReplicator.getMasterDescriptor();
        servletPort = masterDescriptor.getServletPort();
        Pair<ReplicationServiceTestImpl<ReplicableInterface>, ReplicationMasterDescriptor> result = new Pair<>(replicaReplicator, masterDescriptor);
        logger.info("starting initial load transmission servlet for "+getClass().getName());
        initialLoadTestServerThread = replicaReplicator.startInitialLoadTransmissionServlet();
        this.replicaReplicator = replicaReplicator; 
        logger.info("basicSetUp for test class "+getClass().getName()+" finished");
        return result;
    }

    /**
     * Creates a new master replicable. This method is called only after {@link #persistenceSetUp(boolean)} has been
     * called. Therefore, any initialization that {@link #persistenceSetUp(boolean)} may have performed can be assumed
     * to have taken place in the implementations of this method.
     * 
     * @param securityServiceTrackerMock
     *            an optional mock for the security service tracker; may be <code>null</code>
     */
    protected abstract ReplicableImpl createNewMaster(FullyInitializedReplicableTracker<SecurityService> securityServiceTrackerMock) throws Exception;

    /**
     * Creates a new replica replicable instance. This method is called only after {@link #persistenceSetUp(boolean)}
     * has been called. Therefore, any initialization that {@link #persistenceSetUp(boolean)} may have performed can be
     * assumed to have taken place in the implementations of this method.
     * 
     * @param securityServiceTrackerMock
     *            an optional mock for the security service tracker; may be <code>null</code>
     */
    protected abstract ReplicableImpl createNewReplica(FullyInitializedReplicableTracker<SecurityService> securityServiceTrackerMock) throws Exception;

    public void tearDown() throws Exception {
        logger.info("starting to tearDown() test");
        persistenceTearDown();
        if (masterReplicator != null) {
            logger.info("before unregisterReplica...");
            masterReplicator.unregisterReplica(replicaDescriptor);
        }
        if (masterDescriptor != null) {
            logger.info("before stopConnection...");
            masterDescriptor.stopConnection(/* deleteExchange */ true);
        }
        try {
            if (initialLoadTestServerThread != null) {
                logger.info("found non-null initialLoadTestServerThread that we'll now try to stop...");
                URLConnection urlConnection = new URL("http://localhost:"+servletPort+"/STOP").openConnection(); // stop the initial load test server thread
                urlConnection.getInputStream().close();
                logger.info("sent and closed STOP request");
                initialLoadTestServerThread.join(10000 /* wait 10s */);
                logger.info("joined servlet thread");
                assertFalse(initialLoadTestServerThread.isAlive(), "Expected initial load test server thread to die");
            }
        } catch (ConnectException ex) {
            /* do not make tests fail because of a server that has been shut down
             * or when an exception occured (see setUp()) - let the
             * original exception propagate */
            logger.log(Level.SEVERE, "Exception trying to connect to initial load test servlet to STOP it", ex);
        }
        final ReplicationReceiver replicaReplicatorReplicator = replicaReplicator.getReplicator();
        if (replicaReplicatorReplicator != null) {
            synchronized (replicaReplicatorReplicator) {
                while (!replicaReplicatorReplicator.isQueueEmptyOrStopped()) {
                    logger.info("Waiting for replication queue to drain...");
                    replicaReplicatorReplicator.wait();
                }
            }
        }
        logger.info("Replication queue has been drained...");
    }
    
    /**
     * Gives subclasses a hook to tear down a persistence component, such as a DB. This implementation does nothing.
     */
    protected void persistenceTearDown() {
    }

    public void stopReplicatingToMaster() throws IOException {
        replicaReplicator.stopToReplicateFromMaster();
    }
    
    public static class ReplicationServiceTestImpl<ReplicableInterface extends Replicable<?, ?>> extends ReplicationServiceImpl {
        protected static final int INITIAL_LOAD_PACKAGE_SIZE = 1024*1024;
        private final ReplicableInterface master;
        private final ReplicaDescriptor replicaDescriptor;
        private final ReplicationService masterReplicationService;
        private final ReplicationMasterDescriptor masterDescriptor;
        private final ServerSocket ss;
        
        public ReplicationServiceTestImpl(String exchangeName, String exchangeHost, ReplicationInstancesManager replicationInstancesManager,
                ReplicaDescriptor replicaDescriptor, ReplicableInterface replica,
                ReplicableInterface master, ReplicationService masterReplicationService)
                throws Exception {
            super(exchangeName, exchangeHost, 0, replicationInstancesManager, new SingletonReplicablesProvider(replica));
            this.replicaDescriptor = replicaDescriptor;
            this.master = master;
            ServerSocket mySs;
            do {
                mySs = new ServerSocket(0); // bind to any free port
            } while (!ReplicationMasterDescriptor.getHttpRequestProtocol(mySs.getLocalPort()).equals("http")); // unless it is considered an https port by internal heuristics; then try again...
            ss = mySs;
            this.masterReplicationService = masterReplicationService;
            final List<Replicable<?, ?>> replicablesToReplicate = new ArrayList<>();
            for (final String replicableIdAsString : replicaDescriptor.getReplicableIdsAsStrings()) {
                replicablesToReplicate.add(getReplicablesProvider().getReplicable(replicableIdAsString, /* wait */ false));
            }
            this.masterDescriptor = new ReplicationMasterDescriptorImpl(exchangeHost, exchangeName, /* messagingPort */ 0,
                    UUID.randomUUID().toString(), "localhost", ss.getLocalPort(), /* bearerToken */ null, replicablesToReplicate);
        }
        
        ReplicationMasterDescriptor getMasterDescriptor() {
            return masterDescriptor;
        }
        
        @Override
        public ReplicationReceiver getReplicator() {
            return super.getReplicator();
        }

        private Thread startInitialLoadTransmissionServlet() throws InterruptedException {
            final boolean[] listening = new boolean[] { false };
            Thread initialLoadTestServerThread = new Thread("Replication initial load test server") {
                public void run() {
                    try {
                        synchronized (listening) {
                            listening[0] = true;
                            listening.notifyAll();
                        }
                        boolean stop = false;
                        while (!stop) {
                            final Socket s = ss.accept();
                            final InputStream inputStream = s.getInputStream();
                            String request = readLine(inputStream);
                            logger.info("received request "+request);
                            PrintWriter pw = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
                            // Note the trailing space in the following prefix comparison;
                            // It matches, e.g., "POST /replication/replication HTTP/1.1"
                            if (request.startsWith("POST /replication/replication ")) {
                                final ReplicationServlet servlet = new ReplicationServlet(new SingletonReplicablesProvider(master), /* replicationServiceTracker */ null);
                                final HttpServletRequest requestMock = Mockito.mock(HttpServletRequest.class);
                                Mockito.when(requestMock.getInputStream()).thenReturn(new ServletInputStream() {
                                    @Override
                                    public boolean isFinished() {
                                        try {
                                            return inputStream.available() <= 0;
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }

                                    @Override
                                    public boolean isReady() {
                                        try {
                                            return inputStream.available() > 0;
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }

                                    @Override
                                    public void setReadListener(ReadListener readListener) {
                                    }

                                    @Override
                                    public int read() throws IOException {
                                        return inputStream.read();
                                    }
                                });
                                final HttpServletResponse responseMock = Mockito.mock(HttpServletResponse.class);
                                final boolean[] error = { false };
                                Mockito.doAnswer(new Answer<Void>() {
                                    @Override
                                    public Void answer(InvocationOnMock invocation) throws Throwable {
                                        pw.println("HTTP/1.1 500 Bad Request");
                                        pw.println("Content-Type: text/plain");
                                        pw.println();
                                        pw.println(invocation.getArgument(1, String.class));
                                        pw.flush();
                                        error[0] = true;
                                        return null;
                                    }
                                }).when(responseMock).sendError(ArgumentMatchers.anyInt(), ArgumentMatchers.isA(String.class));
                                while (!readLine(inputStream).isEmpty());
                                servlet.doPost(requestMock, responseMock);
                                if (!error[0]) {
                                    pw.println("HTTP/1.1 200 OK");
                                    pw.println("Content-Type: text/plain");
                                    pw.println();
                                    pw.flush();
                                }
                            } else {
                                pw.println("HTTP/1.1 200 OK");
                                pw.println("Content-Type: text/plain");
                                pw.println();
                                pw.flush();
                                if (request.contains(Action.DEREGISTER.name())) {
                                    // assuming that it is safe to unregister all replicas for tests
                                    for (ReplicaDescriptor descriptor : getReplicaInfo()) {
                                        unregisterReplica(descriptor);
                                    }
                                } else if (request.contains(Action.REGISTER.name())) {
                                    final String uuid = UUID.randomUUID().toString();
                                    registerReplicaUuidForMaster(uuid, masterDescriptor);
                                    masterReplicationService.registerReplica(replicaDescriptor);
                                    pw.print(uuid);
                                } else if (request.contains(Action.INITIAL_LOAD.name())) {
                                    Channel channel = masterReplicationService.createMasterChannel();
                                    RabbitOutputStream ros = new RabbitOutputStream(INITIAL_LOAD_PACKAGE_SIZE, channel,
                                            /* queueName */ "initial-load-for-TestClient-"+UUID.randomUUID(), /* syncAfterTimeout */ false);
                                    pw.println(ros.getQueueName());
                                    final LZ4BlockOutputStream compressingOutputStream = new LZ4BlockOutputStream(ros);
                                    master.serializeForInitialReplication(compressingOutputStream);
                                    compressingOutputStream.finish();
                                    ros.close();
                                } else if (request.contains("STOP")) {
                                    stop = true;
                                    logger.info("received STOP request");
                                }
                            }
                            pw.flush(); // ensure response is delivered
                            s.close();
                            logger.info("Request handled successfully.");
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Exception in test replication servlet", e);
                        throw new RuntimeException(e);
                    } finally {
                        logger.info("replication servlet emulation done");
                        try {
                            if (ss != null) {
                                ss.close();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                private String readLine(final InputStream inputStream) throws IOException {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    int b;
                    while ((b=inputStream.read()) != -1 && b != '\n') {
                        if (b != '\r') { // ignore CR
                            bos.write(b);
                        }
                    }
                    return new String(bos.toByteArray());
                }
            };
            initialLoadTestServerThread.start();
            synchronized (listening) {
                while (!listening[0]) {
                    listening.wait();
                }
            }
            return initialLoadTestServerThread;
        }

        public ReplicationReceiverImpl startToReplicateFromButDontYetFetchInitialLoad(ReplicationMasterDescriptor master, boolean startReplicatorSuspended)
                throws Exception {
            masterReplicationService.registerReplica(replicaDescriptor);
            registerReplicaUuidForMaster(replicaDescriptor.getUuid().toString(), master);
            QueueingConsumer consumer = master.getConsumer();
            final ReplicationReceiverImpl replicator = new ReplicationReceiverImpl(master, getReplicablesProvider(), startReplicatorSuspended, consumer);
            new Thread(replicator).start();
            return replicator;
        }
        
        /**
         * Clones the {@link #master}'s state to the {@link #replica} using
         * {@link RacingEventServiceImpl#serializeForInitialReplication(ObjectOutputStream)} and
         * {@link RacingEventServiceImpl#initiallyFillFrom(ObjectInputStream)} through a piped input/output stream.
         * @throws InterruptedException 
         */
        public void initialLoad() throws IOException, ClassNotFoundException, InterruptedException {
            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            new Thread("clone writer") {
                public void run() {
                    try {
                        master.serializeForInitialReplication(pos);
                        pos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            }.start();
            for (Replicable<?, ?> replicable : getReplicablesProvider().getReplicables()) {
                replicable.clearReplicaState();
                replicable.initiallyFillFrom(pis);
            }
            pis.close();
        }
        
        public void waitUntilQueueIsEmpty() throws InterruptedException, IllegalAccessException {
            synchronized (getReplicator()) {
                while (!getReplicator().isQueueEmptyOrStopped()) {
                    getReplicator().wait();
                }
            }
        }
    }

    public ReplicableImpl getReplica() {
        return replica;
    }

    public ReplicableImpl getMaster() {
        return master;
    }

    public ReplicationServiceTestImpl<ReplicableInterface> getReplicaReplicator() {
        return replicaReplicator;
    }

    public ReplicationMasterDescriptor getMasterDescriptor() {
        return masterDescriptor;
    }
}
