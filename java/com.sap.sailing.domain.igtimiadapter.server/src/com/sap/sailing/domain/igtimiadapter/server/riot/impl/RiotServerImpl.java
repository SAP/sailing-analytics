package com.sap.sailing.domain.igtimiadapter.server.riot.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Thread.State;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.parser.ParseException;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.igtimi.IgtimiData.DataMsg;
import com.igtimi.IgtimiData.DataPoint;
import com.igtimi.IgtimiData.DataPoint.DataCase;
import com.igtimi.IgtimiDevice.DeviceManagement;
import com.igtimi.IgtimiDevice.DeviceManagementResponse;
import com.igtimi.IgtimiStream.Msg;
import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;
import com.sap.sailing.domain.igtimiadapter.DataPointTimePointExtractor;
import com.sap.sailing.domain.igtimiadapter.DataPointVisitor;
import com.sap.sailing.domain.igtimiadapter.Device;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnection;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnectionFactory;
import com.sap.sailing.domain.igtimiadapter.datatypes.Type;
import com.sap.sailing.domain.igtimiadapter.persistence.DomainObjectFactory;
import com.sap.sailing.domain.igtimiadapter.persistence.MongoObjectFactory;
import com.sap.sailing.domain.igtimiadapter.server.Activator;
import com.sap.sailing.domain.igtimiadapter.server.RiotWebsocketHandler;
import com.sap.sailing.domain.igtimiadapter.server.replication.ReplicableRiotServer;
import com.sap.sailing.domain.igtimiadapter.server.replication.RiotReplicationOperation;
import com.sap.sailing.domain.igtimiadapter.server.riot.RiotConnection;
import com.sap.sailing.domain.igtimiadapter.server.riot.RiotMessageListener;
import com.sap.sailing.domain.igtimiadapter.server.riot.RiotServer;
import com.sap.sailing.domain.igtimiadapter.server.riot.RiotStandardCommand;
import com.sap.sse.common.Duration;
import com.sap.sse.common.MultiTimeRange;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.replication.ReplicationService;
import com.sap.sse.replication.interfaces.impl.AbstractReplicableWithObjectInputStream;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.SessionUtils;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.PermissionChecker;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.shared.util.Wait;
import com.sap.sse.util.ObjectInputStreamResolvingAgainstCache;
import com.sap.sse.util.ServiceTrackerFactory;
import com.sap.sse.util.ThreadPoolUtil;

public class RiotServerImpl extends AbstractReplicableWithObjectInputStream<ReplicableRiotServer, RiotReplicationOperation<?>> implements RiotServer, ReplicableRiotServer, Runnable {
    private static final Logger logger = Logger.getLogger(RiotServerImpl.class.getName());

    private final ConcurrentMap<Long, DataAccessWindow> dataAccessWindows;
    private final ConcurrentMap<Long, Device> devices;
    private final ConcurrentMap<String, Device> devicesBySerialNumber;
    private final Set<RiotMessageListener> listeners;
    private final Selector socketSelector;
    private final ServerSocketChannel serverSocketChannel;
    private final Thread communicatorThread;
    private boolean running;
    private final MongoObjectFactory mongoObjectFactory;
    private final DomainObjectFactory domainObjectFactory;
    private final Set<RiotWebsocketHandler> liveWebSocketConnections;
    
    /**
     * The active connections managed by this server. Heartbeats will be sent into the channels found in the key set on
     * a regular basis. The {@link SocketChannel} keys are registered with the {@link #socketSelector}. Data sent by the
     * devices is read by the {@link #communicatorThread} and {@link RiotConnection#dataReceived(java.nio.ByteBuffer)
     * forwarded} to the {@link RiotConnection} object associated with the key socket channel. It is the connection's
     * responsibility to respond to messages that require a response, such as an authentication request.
     */
    private final ConcurrentMap<SocketChannel, RiotConnection> connections;

    private final ServiceTracker<ReplicationService, ReplicationService> replicationServiceTracker;
    
    private final ExecutorService executorForNotifyingListeners;

    /**
     * Like {@link #RiotServerImpl(int)}, only that an available port is selected automatically.
     * The port can be obtained by calling {@link #getPort}.
     */
    public RiotServerImpl(DomainObjectFactory domainObjectFactory, MongoObjectFactory mongoObjectFactory, BundleContext context) throws Exception {
        this(0, domainObjectFactory, mongoObjectFactory, context);
    }
    
    public RiotServerImpl(int port, DomainObjectFactory domainObjectFactory, MongoObjectFactory mongoObjectFactory, BundleContext context) throws Exception {
        this(new InetSocketAddress("0.0.0.0", port), domainObjectFactory, mongoObjectFactory, context);
    }
    
    /**
     * @param localAddress
     *            if {@code null}, select a local port to listen on automatically
     * @param context
     *            OSGi bundle context; if set, the {@link ReplicationService} will be tracked and used determine whether
     *            replication is currently starting, to avoid asking for other replicables such as the
     *            {@link SecurityService}, which would cause a deadlock. If {@code null}, no such tracking will
     *            happen, assuming we're not in a production and particularly no replicated environment.
     */
    public RiotServerImpl(SocketAddress localAddress, DomainObjectFactory domainObjectFactory,
            MongoObjectFactory mongoObjectFactory, BundleContext context) throws Exception {
        this.replicationServiceTracker = context == null ? null :
            ServiceTrackerFactory.createAndOpen(context, ReplicationService.class);
        this.listeners = ConcurrentHashMap.newKeySet();
        this.executorForNotifyingListeners = ThreadPoolUtil.INSTANCE.createForegroundTaskThreadPoolExecutor("Riot Listener Notifier");
        this.dataAccessWindows = new ConcurrentHashMap<>();
        this.devices = new ConcurrentHashMap<>();
        this.devicesBySerialNumber = new ConcurrentHashMap<>();
        this.connections = new ConcurrentHashMap<>();
        this.domainObjectFactory = domainObjectFactory;
        this.mongoObjectFactory = mongoObjectFactory;
        this.liveWebSocketConnections = ConcurrentHashMap.newKeySet();
        for (final Device device : domainObjectFactory.getDevices(/* clientSessionOrNull */ null)) {
            devices.put(device.getId(), device);
            devicesBySerialNumber.put(device.getSerialNumber(), device);
        }
        for (final DataAccessWindow daw : domainObjectFactory.getDataAccessWindows(/* clientSessionOrNull */ null)) {
            dataAccessWindows.put(daw.getId(), daw);
        }
        this.socketSelector = Selector.open();
        this.serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(localAddress);
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
        this.running = true;
        this.communicatorThread = new Thread(this, "Riot thread listening on port "+getPort());
        communicatorThread.setDaemon(true);
        communicatorThread.start();
        Wait.wait(()->communicatorThread.getState() != State.NEW, Optional.of(Duration.ONE_SECOND.times(5)), Duration.ONE_SECOND);
        logger.info("Riot thread is running and listening for new connections on port "+getPort());
    }
    
    @Override
    public int getPort() throws IOException {
        return ((InetSocketAddress) serverSocketChannel.getLocalAddress()).getPort();
    }
    
    @Override
    public Iterable<RiotConnection> getLiveConnections() {
        return Collections.unmodifiableCollection(connections.values());
    }
    
    @Override
    public void stop() throws IOException {
        if (running) {
            logger.info("Stopping Riot server listening on address "+serverSocketChannel.getLocalAddress());
            running = false; // cause the thread to exit latest 1s after this call
            serverSocketChannel.keyFor(socketSelector).cancel();
            serverSocketChannel.close();
            for (final SocketChannel deviceChannel : connections.keySet()) {
                logger.info("Closing active device connection "+connections.get(deviceChannel)+" from "+deviceChannel.getRemoteAddress());
                deviceChannel.keyFor(socketSelector).cancel();
            }
            connections.clear();
            for (final RiotWebsocketHandler liveConnection : liveWebSocketConnections) {
                liveConnection.close(1000, "Riot Server Stopped");
            }
        } else {
            logger.info("Trying to stop a Riot server that is not running. Ignoring.");
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                socketSelector.select(1000 /* ms timeout; e.g., in order to stop 1s after stop() was called */);
                for (final SelectionKey selectedKey : socketSelector.selectedKeys()) {
                    if (selectedKey.isAcceptable()) {
                        final SocketChannel deviceChannel = serverSocketChannel.accept();
                        if (deviceChannel != null) { // may be null because the server socket is in non-blocking mode
                            deviceChannel.configureBlocking(false);
                            deviceChannel.register(socketSelector, SelectionKey.OP_READ);
                            connections.put(deviceChannel, new RiotConnectionImpl(deviceChannel, this));
                            logger.info("New device connection from "+deviceChannel.getRemoteAddress());
                        }
                    } else if (selectedKey.isReadable()) {
                        final SocketChannel deviceChannel = (SocketChannel) selectedKey.channel();
                        final ByteBuffer buffer = ByteBuffer.allocate(4096);
                        final int numberOfBytesRead = deviceChannel.read(buffer);
                        final RiotConnection connection = connections.get(deviceChannel);
                        if (numberOfBytesRead > 0) {
                            if (connection != null) {
                                connection.dataReceived(buffer);
                            } else {
                                logger.warning("Data received from a socket with address "+deviceChannel.getRemoteAddress()
                                              +" that we don't have a managed connection for");
                            }
                        } else if (numberOfBytesRead == -1) { // EOF
                            logger.info("Device channel from "+deviceChannel.getRemoteAddress()+" closed");
                            if (connection != null) {
                                logger.info("The device was handled by connection "+connection);
                                try {
                                    connection.close(); // will also remove the connection from this server through connectionClosed(RiotConnection)
                                } catch (Exception e) {
                                    logger.warning("Trying to properly close a connection for which we read EOF from its channel threw an exception: "+e.getMessage());
                                }
                            }
                            deviceChannel.close();
                        }
                    }
                }
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Exception trying to read or accept new connection. Continuing...", e);
            }
        }
        try {
            logger.info("Riot server listening on "+serverSocketChannel.getLocalAddress()+" stops running");
        } catch (IOException e) {
            logger.info("Riot server listening on "+serverSocketChannel+" stops running");
        }
    }

    /**
     * Removes the {@link RiotConnection} connected to the {@code deviceChannel} in {@link #connections} from this
     * server. The {@code deviceChannel} has its key for the {@link #socketSelector} {@link SelectionKey#cancel()
     * cancelled}.
     */
    void connectionClosed(SocketChannel deviceChannel) {
        connections.remove(deviceChannel);
        final SelectionKey selectionKey = deviceChannel.keyFor(socketSelector);
        if (selectionKey != null) {
            selectionKey.cancel();
        }
    }
    
    @Override
    public void addListener(RiotMessageListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(RiotMessageListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Forwards the {@code message} to all {@link #addListener(RiotMessageListener) registered} listeners and sends it
     * to all {@link #liveWebSocketConnections} that are authorized to {@link DefaultActions#READ READ} from the
     * {@link Device} from which the message originated and for which a READable {@link DataAccessWindow} exists for the
     * web socket connection's authenticated user for the time points of the data points in the message.
     * <p>
     * 
     * This is a replicating operation which only {@link #apply(RiotReplicationOperation) applies} this request to the
     * replicable; if running as primary/master, the request will be applied locally and replicated to all replicas
     * which will forward the {@code message} to their respective live websocket connections. If on a replica, the
     * {@code message} will be applied on the replica and sent to the master/primary for forwarding to the primary's web
     * socket listeners. The persistent storage of the message on the primary/master is essential for retrieving
     * resource data at a later point, e.g., for "replay" purposes. The replicas' persistence is to be ignored.
     * <p>
     * 
     * We expect listeners to perform DB and other network interactions which may block for I/O.. Therefore, the method
     * uses the {@link #executorForNotifyingListeners} executor to notify the listeners in a separate thread. This will
     * allow the calling thread to return quickly, even if the listeners take a long time to process the message.
     * <p>
     * 
     * @param message
     *            the message received from the device
     * @param deviceSerialNumber
     *            the serial number of the device from which the message originated
     */
    void notifyListeners(Msg message, String deviceSerialNumber) {
        executorForNotifyingListeners.execute(()->apply(s->s.internalNotifyListeners(message, deviceSerialNumber)));
    }
    
    @Override
    public Void internalNotifyListeners(Msg message, String deviceSerialNumber) {
        try {
            if (replicationServiceTracker == null || replicationServiceTracker.getService() == null ||
              !replicationServiceTracker.getService().isReplicationStarting()) {
                if (deviceSerialNumber != null) {
                    forwardMessageToEligibleWebSocketClients(message, deviceSerialNumber);
                }
            }
            for (final RiotMessageListener listener : listeners) {
                listener.onMessage(message);
            }
        } finally {
            // store the message, regardless of how badly the client forwarding or any listeners fail
            storeMessage(message, deviceSerialNumber);
        }
        return null;
    }

    private void storeMessage(Msg message, String deviceSerialNumber) {
        mongoObjectFactory.storeMessage(deviceSerialNumber, message, /* clientSessionOrNull */ null);
    }

    /**
     * Requires the {@link SecurityService} to be available, at least when one or more {@link #liveWebSocketConnections}
     * are registered. This means that this method must not be invoked, e.g., when {@link #replicationServiceTracker the
     * replication service} is still {@link ReplicationService#isReplicationStarting() starting}.
     * 
     * @param deviceSerialNumber
     *            must not be {@code null}
     */
    private void forwardMessageToEligibleWebSocketClients(Msg message, String deviceSerialNumber) {
        final Device device = getDeviceBySerialNumber(deviceSerialNumber);
        if (device == null) {
            logger.info("Received message from unknown devices "+deviceSerialNumber+"; creating Device");
            final Device newDevice = createDevice(deviceSerialNumber);
            getSecurityService().setOwnership(newDevice.getIdentifier(), /* user owner */ null,
                    getSecurityService().getServerGroup(), newDevice.getName());
        } else {
            final byte[] messageAsBytes = message.toByteArray();
            final TimeRange messageDataTimeRange = getTimeRange(message);
            final Iterable<DataAccessWindow> daws = getDataAccessWindows(Collections.singleton(deviceSerialNumber), MultiTimeRange.of(messageDataTimeRange));
            // obtain the securityService only if connections exist; otherwise, we may still be in start-up mode
            final SecurityService securityService = liveWebSocketConnections.isEmpty() ? null : getSecurityService();
            for (final RiotWebsocketHandler webSocketClient : liveWebSocketConnections) {
                if (webSocketClient.getDeviceSerialNumbers().contains(deviceSerialNumber)) {
                    final User user = webSocketClient.getAuthenticatedUser();
                    final OwnershipAnnotation deviceOwnership = securityService.getOwnership(device.getIdentifier());
                    final AccessControlListAnnotation deviceAccessControlList = securityService.getAccessControlList(device.getIdentifier());
                    if (!Util.isEmpty(Util.filter(daws, daw->{
                        final OwnershipAnnotation dawOownership = securityService.getOwnership(daw.getIdentifier());
                        final AccessControlListAnnotation dawAccessControlList = securityService.getAccessControlList(daw.getIdentifier());
                        return PermissionChecker.isPermitted(
                                daw.getIdentifier().getPermission(DefaultActions.READ),
                                user, securityService.getAllUser(),
                                dawOownership==null?null:dawOownership.getAnnotation(),
                                dawAccessControlList==null?null:dawAccessControlList.getAnnotation());
                    }))
                    && PermissionChecker.isPermitted(
                            device.getIdentifier().getPermission(DefaultActions.READ),
                            user, securityService.getAllUser(),
                            deviceOwnership==null?null:deviceOwnership.getAnnotation(),
                            deviceAccessControlList==null?null:deviceAccessControlList.getAnnotation()) ) {
                        webSocketClient.sendBytesByFuture(ByteBuffer.wrap(messageAsBytes));
                    }
                }
            }
        }
    }

    private SecurityService getSecurityService() {
        return Activator.getInstance().getSecurityService();
    }

    /**
     * Returns a time range spanning the time point of the first and the last of the {@link DataPoint}s inside
     * 
     * @return {@code null} in case there is nothing inside the message that specifies a time point, otherwise a time
     *         range that includes the time points of all elements from the message that have a time stamp and that
     *         cannot be made any smaller while still containing all those elements.
     */
    private TimeRange getTimeRange(Msg message) {
        TimePoint from = null;
        TimePoint to = null;
        if (message.hasData()) {
            for (final DataMsg data : message.getData().getDataList()) {
                for (final DataPoint dataPoint : data.getDataList()) {
                    final TimePoint dataPointTimePoint = DataPointVisitor.accept(dataPoint, new DataPointTimePointExtractor());
                    if (from==null || dataPointTimePoint.before(from)) {
                        from = dataPointTimePoint;
                    }
                    if (to==null || dataPointTimePoint.after(to)) {
                        to = dataPointTimePoint;
                    }
                }
            }
        } else if (message.hasDeviceManagement()) {
            final DeviceManagement deviceManagement = message.getDeviceManagement();
            if (deviceManagement.hasResponse()) {
                final DeviceManagementResponse response = deviceManagement.getResponse();
                final TimePoint timePoint = TimePoint.of(response.getTimestamp());
                if (from==null || timePoint.before(from)) {
                    from = timePoint;
                }
                if (to==null || timePoint.after(to)) {
                    to = timePoint;
                }
            }
        }
        return TimeRange.create(from, to==null?null:to.plus(Duration.ONE_MILLISECOND));
    }

    @Override
    public Iterable<Device> getDevices() {
        return Collections.unmodifiableCollection(devices.values());
    }
    
    @Override
    public Device getDeviceById(long id) {
        return devices.get(id);
    }
    
    @Override
    public Device getDeviceBySerialNumber(String deviceSerialNumber) {
        return devicesBySerialNumber.get(deviceSerialNumber);
    }

    @Override
    public Device createDevice(final String deviceSerialNumber) {
        return apply(s->s.internalCreateDevice(deviceSerialNumber));
    }
    
    @Override
    public Device internalCreateDevice(String deviceSerialNumber) {
        final long id = devices.isEmpty() ? 1 : Collections.max(devices.keySet()) + 1;
        final Device device = Device.create(id, deviceSerialNumber);
        devices.put(device.getId(), device);
        devicesBySerialNumber.put(device.getSerialNumber(), device);
        mongoObjectFactory.storeDevice(device, /* clientSessionOrNull */ null);
        return device;
    }

    @Override
    public void removeDevice(long deviceId) {
        apply(s->s.internalRemoveDevice(deviceId));
    }
    
    @Override
    public Void internalRemoveDevice(long deviceId) {
        final Device device = devices.remove(deviceId);
        devicesBySerialNumber.remove(device.getSerialNumber());
        mongoObjectFactory.removeDevice(deviceId, /* clientSessionOrNull */ null);
        return null;
    }

    @Override
    public void updateDeviceName(long deviceId, String name) {
        apply(s->s.internalUpdateDeviceName(deviceId, name));
    }

    @Override
    public Void internalUpdateDeviceName(long deviceId, String name) {
        final Device existingDevice = devices.get(deviceId);
        if (existingDevice != null) {
            existingDevice.setName(name);
            mongoObjectFactory.storeDevice(existingDevice, /* clientSessionOrNull */ null);
        }
        return null;
    }
    
    @Override
    public void updateDeviceLastHeartbeat(long deviceId, TimePoint timePointOfLastHeartbeat, String remoteAddress) {
        apply(s->s.internalUpdateDeviceLastHeartbeat(deviceId, timePointOfLastHeartbeat, remoteAddress));
    }
    
    @Override
    public Void internalUpdateDeviceLastHeartbeat(long deviceId, TimePoint timePointOfLastHeartbeat, String remoteAddress) {
        final Device existingDevice = devices.get(deviceId);
        if (existingDevice != null) {
            existingDevice.setLastHeartbeat(timePointOfLastHeartbeat, remoteAddress);
            mongoObjectFactory.storeDevice(existingDevice, /* clientSessionOrNull */ null);
        }
        return null;
    }

    @Override
    public Iterable<DataAccessWindow> getDataAccessWindows() {
        return Collections.unmodifiableCollection(dataAccessWindows.values());
    }
    
    @Override
    public DataAccessWindow getDataAccessWindowById(long id) {
        return dataAccessWindows.get(id);
    }

    @Override
    public Iterable<DataAccessWindow> getDataAccessWindows(Iterable<String> deviceSerialNumbers, MultiTimeRange timeRanges) {
        final Set<DataAccessWindow> result = new HashSet<>();
        final Set<String> deviceSerialNumbersAsSet = Util.asSet(deviceSerialNumbers);
        // TODO provide a more efficient implementation if this turns out to become a performance bottleneck, e.g., by keeping the DataAccessWindows in a time-sorted TreeSet
        for (final DataAccessWindow daw : getDataAccessWindows()) {
            if (deviceSerialNumbersAsSet.contains(daw.getDeviceSerialNumber()) && timeRanges.intersects(daw.getTimeRange())) {
                result.add(daw);
            }
        }
        return result;
    }

    @Override
    public DataAccessWindow createDataAccessWindow(String deviceSerialNumber, TimePoint from, TimePoint to) {
        return apply(s->s.internalCreateDataAccessWindow(deviceSerialNumber, from, to));
    }

    @Override
    public DataAccessWindow internalCreateDataAccessWindow(String deviceSerialNumber, TimePoint from, TimePoint to) {
        final long newId = dataAccessWindows.isEmpty() ? 1 : Collections.max(dataAccessWindows.keySet()) + 1;
        final DataAccessWindow daw = DataAccessWindow.create(newId, from, to, deviceSerialNumber);
        dataAccessWindows.put(daw.getId(), daw);
        mongoObjectFactory.storeDataAccessWindow(daw, /* clientSessionOrNull */ null);
        return daw;
    }
    
    @Override
    public void removeDataAccessWindow(long dawId) {
        apply(s->s.internalRemoveDataAccessWindow(dawId));
    }

    @Override
    public Void internalRemoveDataAccessWindow(long dawId) {
        dataAccessWindows.remove(dawId);
        mongoObjectFactory.removeDataAccessWindow(dawId, /* clientSessionOrNull */ null);
        return null;
    }

    /**
     * Use this method only for testing/debugging. It clears the transient state of this service,in particular
     * all resources and data access windows.
     */
    public void clear() {
        devices.clear();
        devicesBySerialNumber.clear();
        dataAccessWindows.clear();
        connections.clear();
        listeners.clear();
        mongoObjectFactory.clear(/* clientSessionOrNull */ null);
    }

    @Override
    public void clearReplicaState() throws MalformedURLException, IOException, InterruptedException {
        clear();
    }

    @Override
    public ObjectInputStream createObjectInputStreamResolvingAgainstCache(InputStream is,
            Map<String, Class<?>> classLoaderCache) throws IOException {
        return new ObjectInputStreamResolvingAgainstCache<Object>(is, /* dummy cache */ new Object(), /* resolve listener */ null, classLoaderCache) {
        }; // use anonymous inner class in this class loader to see all that this class sees
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initiallyFillFromInternal(ObjectInputStream is)
            throws IOException, ClassNotFoundException, InterruptedException {
        devices.putAll((Map<Long, Device>) is.readObject());
        for (final Device device : devices.values()) {
            devicesBySerialNumber.put(device.getSerialNumber(), device);
        }
        dataAccessWindows.putAll((Map<Long, DataAccessWindow>) is.readObject());
    }

    @Override
    public void serializeForInitialReplicationInternal(ObjectOutputStream objectOutputStream) throws IOException {
        objectOutputStream.writeObject(new HashMap<>(devices));
        objectOutputStream.writeObject(new HashMap<>(dataAccessWindows));
    }

    @Override
    public Iterable<Msg> getMessages(String deviceSerialNumber, MultiTimeRange timeRanges, Set<DataCase> dataCases) throws IllegalStateException, ClientProtocolException, IOException, ParseException {
        final Iterable<Msg> result;
        if (getMasterDescriptor() == null) {
            result = domainObjectFactory.getMessages(deviceSerialNumber, timeRanges, dataCases, /* clientSessionOrNull */ null);
        } else {
            final Type[] types = new Type[dataCases.size()];
            int i=0;
            for (final DataCase dataCase : dataCases) {
                types[i++] = Type.valueOf(dataCase.getNumber());
            }
            result = Util.concat(Util.map(timeRanges, timeRange->{
                try {
                    return getFromPrimary(deviceSerialNumber, types, (c, dsn, ts)->c.getMessages(timeRange.from(), timeRange.to(), Collections.singleton(dsn), ts));
                } catch (ParseException | IOException e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        return result;
    }
    
    private <T> T getFromPrimary(String deviceSerialNumber, Type[] types, MessageLoader<T> resultProvider) throws ParseException, IOException {
        final int masterPort = getMasterDescriptor().getServletPort();
        final String masterHostname = getMasterDescriptor().getHostname();
        final SecurityService securityService = getSecurityService();
        final String currentUserBearerToken = securityService == null ? null
                : securityService.getCurrentUser() == null ? null
                        : securityService.getAccessToken(securityService.getCurrentUser().getName());
        try {
            final URL baseUrl = new URL(masterPort==443?"https":"http", masterHostname, masterPort, "/");
            final IgtimiConnectionFactory igtimiConnectionFactory =
                    IgtimiConnectionFactory.create(baseUrl, currentUserBearerToken);
            final IgtimiConnection connection = igtimiConnectionFactory.getOrCreateConnection();
            return resultProvider.getResult(connection, deviceSerialNumber, types);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    
    @FunctionalInterface
    private static interface MessageLoader<T> {
        T getResult(IgtimiConnection connection, String deviceSerialNumber, Type[] types) throws IOException, ParseException;
    }

    @Override
    public Iterable<Pair<TimePoint, String>> getDeviceLogs(String serialNumber, Duration duration) throws ParseException, IOException {
        final TimePoint endTime = TimePoint.now();
        final TimePoint startTime = endTime.minus(duration);
        final Iterable<Msg> messages;
        if (getMasterDescriptor() == null) {
            messages = domainObjectFactory.getMessages(serialNumber, MultiTimeRange.of(TimeRange.create(startTime, endTime)),
                    Collections.singleton(DataCase.LOG), /* clientSessionOrNull */ null);
        } else {
            messages = getFromPrimary(serialNumber, new Type[] { Type.valueOf(DataCase.LOG.getNumber()) },
                    (c, dsn, ts)->c.getMessages(startTime, endTime, Collections.singleton(dsn), ts));
        }
        final List<Pair<TimePoint, String>> logMessages = new ArrayList<>();
        for (final Msg message : messages) {
            for (final DataMsg data : message.getData().getDataList()) {
                for (final DataPoint dataPoint : data.getDataList()) {
                    if (dataPoint.hasLog()) {
                        logMessages.add(new Pair<>(TimePoint.of(dataPoint.getLog().getTimestamp()), dataPoint.getLog().getMessage()));
                    }
                }
            }
        }
        return logMessages;
    }

    @Override
    public void addWebSocketClient(RiotWebsocketHandler riotWebsocketHandler) {
        liveWebSocketConnections.add(riotWebsocketHandler);
    }
    
    @Override
    public void removeWebSocketClient(RiotWebsocketHandler riotWebsocketHandler) {
        liveWebSocketConnections.remove(riotWebsocketHandler);
    }

    @Override
    public Msg getLastMessage(String serialNumber, DataCase dataCase, MultiTimeRange timeRanges) throws ParseException, IOException {
        final Msg result;
        if (getMasterDescriptor() == null) {
            result = domainObjectFactory.getLatestMessage(serialNumber, dataCase, timeRanges, /* clientSessionOrNull */ null);
        } else {
            result = getFromPrimary(serialNumber, new Type[] { Type.valueOf(dataCase.getNumber()) },
                    (c, dsn, ts)->c.getLastMessage(dsn, ts[0]));
        }
        return result;
    }

    @Override
    public boolean sendStandardCommand(String deviceSerialNumber, RiotStandardCommand command) throws IOException {
        logger.info("User "+SessionUtils.getPrincipal()+" requests sending standard command "+command+" to device with serial number "+deviceSerialNumber);
        return apply(s->s.internalSendCommand(deviceSerialNumber, command.getCommand()));
    }

    @Override
    public boolean sendFreestyleCommand(String deviceSerialNumber, String command) throws IOException {
        logger.info("User "+SessionUtils.getPrincipal()+" requests sending freestyle command "+command+" to device with serial number "+deviceSerialNumber);
        return apply(s->s.internalSendCommand(deviceSerialNumber, command));
    }

    @Override
    public boolean internalSendCommand(String deviceSerialNumber, String command) throws IOException, InterruptedException, ExecutionException {
        boolean foundConnection = false;
        for (final RiotConnection connection : getLiveConnections()) {
            if (Util.equalsWithNull(connection.getSerialNumber(), deviceSerialNumber)) {
                connection.sendCommand(command); // see bug6140: could try to use log output returned as a queue
                foundConnection = true;
                break;
            }
        }
        return foundConnection;
    }
    
    @Override
    public boolean enableOverTheAirLog(String deviceSerialNumber, boolean enable) throws IOException, InterruptedException, ExecutionException {
        final Device device = getDeviceBySerialNumber(deviceSerialNumber);
        if (device == null) {
            throw new IllegalArgumentException("No device with serial number "+deviceSerialNumber+" found");
        }
        return apply(s->s.internalEnableOverTheAirLog(deviceSerialNumber, enable));
    }

    @Override
    public boolean internalEnableOverTheAirLog(String deviceSerialNumber, boolean enable) throws IOException {
        boolean foundConnection = false;
        for (final RiotConnection connection : getLiveConnections()) {
            if (Util.equalsWithNull(connection.getSerialNumber(), deviceSerialNumber)) {
                if (enable) {
                    connection.enableOverTheAirLog();
                } else {
                    connection.disableOverTheAirLog();
                }
                foundConnection = true;
                break;
            }
        }
        return foundConnection;
    }
}
