package com.sap.sailing.domain.igtimiadapter.server.riot;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.parser.ParseException;
import org.osgi.framework.BundleContext;

import com.google.protobuf.InvalidProtocolBufferException;
import com.igtimi.IgtimiData.DataMsg;
import com.igtimi.IgtimiData.DataPoint;
import com.igtimi.IgtimiData.DataPoint.DataCase;
import com.igtimi.IgtimiStream.Msg;
import com.sap.sailing.domain.igtimiadapter.BulkFixReceiver;
import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;
import com.sap.sailing.domain.igtimiadapter.Device;
import com.sap.sailing.domain.igtimiadapter.FixFactory;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnection;
import com.sap.sailing.domain.igtimiadapter.IgtimiWindListener;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.datatypes.Type;
import com.sap.sailing.domain.igtimiadapter.persistence.DomainObjectFactory;
import com.sap.sailing.domain.igtimiadapter.persistence.MongoObjectFactory;
import com.sap.sailing.domain.igtimiadapter.server.RiotWebsocketHandler;
import com.sap.sailing.domain.igtimiadapter.server.replication.ReplicableRiotServer;
import com.sap.sailing.domain.igtimiadapter.server.replication.RiotReplicationOperation;
import com.sap.sailing.domain.igtimiadapter.server.riot.impl.RiotServerImpl;
import com.sap.sailing.domain.igtimiadapter.shared.IgtimiWindReceiver;
import com.sap.sse.common.Duration;
import com.sap.sse.common.MultiTimeRange;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.replication.Replicable;

/**
 * A server implementation according to the protocol specification found
 * <a href="https://support.yacht-bot.com/YachtBot%20Products/Riot%20Protocol/">here</a>.
 * An instance can be created using the {@link #create(int)} method, returning a
 * server listening for incoming connections on the TCP port specified.<p>
 * 
 * Connections will be tracked and managed. When a device has connected, heartbeats
 * are sent to the device approximately every 15s until the device disconnects
 * or, due to missing heartbeat messages sent by the device, the connection is deemed
 * dead.<p>
 * 
 * {@link DataMsg} messages {@link DataMsg#getDataList() containing} {@link DataPoint}s are
 * converted to lists of {@link Fix}es which are forwarded to {@link BulkFixReceiver}s that
 * can be registered with this server using the {@link #addListener(BulkFixReceiver)} method.
 * <p>
 * 
 * When combined with {@link IgtimiWindReceiver} (which is such a {@link BulkFixReceiver}),
 * {@link IgtimiWindListener}s can be registered on the wind receiver, just as they can
 * for a websocket connection.<p>
 * 
 * The server uses <tt>java.nio</tt> and {@link ServerSocketChannel}s, avoiding the creation
 * of a thread per connection.<p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface RiotServer extends Replicable<ReplicableRiotServer, RiotReplicationOperation<?>> {
    /**
     * Created a {@link RiotServer} listening on the {@code port} specified. If the port is not
     * available, an {@link IOException} will be thrown.
     */
    static RiotServer create(int port, DomainObjectFactory domainObjectFactory, MongoObjectFactory mongoObjectFactory, BundleContext context) throws Exception {
        return new RiotServerImpl(port, domainObjectFactory, mongoObjectFactory, context);
    }
    
    /**
     * Creates a {@link RiotServer} listening on an available local TCP port selected automatically.
     */
    static RiotServer create(DomainObjectFactory domainObjectFactory, MongoObjectFactory mongoObjectFactory, BundleContext context) throws Exception {
        return new RiotServerImpl(domainObjectFactory, mongoObjectFactory, context);
    }
    
    void addListener(RiotMessageListener listener);
    
    void removeListener(RiotMessageListener listener);

    /**
     * Stops this server and frees its socket resources. If the server is not running, e.g., because it was already
     * stopped by an earlier call to this method, calling this method has no effect.
     */
    void stop() throws IOException;

    /**
     * @return the IP port this server is listening on for new connections
     */
    int getPort() throws IOException;

    Iterable<Device> getDevices();
    
    Device getDeviceById(long id);

    Device getDeviceBySerialNumber(String deviceSerialNumber);

    /**
     * Assigns a unique ID and leaves service tag and name {@code null}
     */
    Device createDevice(String deviceSerialNumber);
    
    void removeDevice(long deviceId);
    
    void updateDeviceName(long deviceId, String name);
    
    void updateDeviceLastHeartbeat(long deviceId, TimePoint timePointOfLastHeartbeat, String remoteAddress);
    
    Iterable<DataAccessWindow> getDataAccessWindows();
    
    DataAccessWindow getDataAccessWindowById(long id);
    
    /**
     * @param serialNumbers must not be {@link null}; if empty, no results will be delivered
     */
    Iterable<DataAccessWindow> getDataAccessWindows(Iterable<String> serialNumbers, MultiTimeRange timeRanges);
    
    DataAccessWindow createDataAccessWindow(String deviceSerialNumber, TimePoint startTime, TimePoint endTime);
    
    void removeDataAccessWindow(long dawId);

    /**
     * Tries to read the data requested from this Riot server's persistent store. However, should this be a replica, an
     * {@link IgtimiConnection} is established to the REST API end point of the primary/master instance of this replica
     * to obtain the content requested.
     */
    Iterable<Msg> getMessages(String deviceSerialNumber, MultiTimeRange timeRanges, Set<DataCase> dataCases)
            throws MalformedURLException, IllegalStateException, ClientProtocolException, IOException, ParseException;

    void addWebSocketClient(RiotWebsocketHandler riotWebsocketHandler);

    void removeWebSocketClient(RiotWebsocketHandler riotWebsocketHandler);

    /**
     * From all messages received and recorded from the device identified by {@code serialNumber},
     * find the last one that contains a {@link DataPoint} which has a {@link DataPoint#getDataCase() data case}
     * as specified by {@code dataCase}. If no such message can be found, {@code null} is returned.
     * 
     * @return {@code null} if no message from the device identified by {@code serialNumber} is found that has
     * a {@link DataPoint} with {@link DataPoint#getDataCase() data case} {@code dataCase}; otherwise that last
     * message stripped down to the last data point with the requested data case.
     */
    Msg getLastMessage(String serialNumber, DataCase dataCase, MultiTimeRange timeRanges) throws InvalidProtocolBufferException, ParseException, IOException;
    
    default <T extends Fix> T getLastFix(String serialNumber, Class<T> type, MultiTimeRange timeRanges) throws InvalidProtocolBufferException, ParseException, IOException {
        final Msg lastMessage = getLastMessage(serialNumber, DataCase.forNumber(Type.getType(type).getCode()), timeRanges);
        final T result;
        if (lastMessage != null) {
            final Iterable<Fix> fixes = new FixFactory().createFixes(lastMessage);
            if (fixes != null) {
                @SuppressWarnings("unchecked")
                final T tResult = (T) Util.first(fixes);
                result = tResult;
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    /**
     * @return the inbound connections from devices currently tracked; they identify the device
     *         {@link RiotConnection#getSerialNumber() serial number} which may be used, e.g., to resolve the device
     *         with {@link #getDeviceBySerialNumber(String)}, as well as the
     *         {@link RiotConnection#getLastHeartbeatReceivedAt() last heart beat}.
     */
    Iterable<RiotConnection> getLiveConnections();
    
    /**
     * Returns {@code true} if and only if a {@link #getLiveConnections() live connection} was found that belongs to the
     * device identified by {@code deviceSerialNumber}.
     */
    boolean sendStandardCommand(String deviceSerialNumber, RiotStandardCommand command) throws IOException;
    
    /**
     * Returns {@code true} if and only if a {@link #getLiveConnections() live connection} was found that belongs to the
     * device identified by {@code deviceSerialNumber}.
     */
    boolean sendFreestyleCommand(String deviceSerialNumber, String command) throws IOException;

    Iterable<Pair<TimePoint, String>> getDeviceLogs(String serialNumber, Duration duration) throws ParseException, IOException;

    boolean enableOverTheAirLog(String deviceSerialNumber, boolean enable)
            throws IOException, InterruptedException, ExecutionException;
}
