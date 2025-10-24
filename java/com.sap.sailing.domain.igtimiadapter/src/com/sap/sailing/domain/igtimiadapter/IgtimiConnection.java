package com.sap.sailing.domain.igtimiadapter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.igtimi.IgtimiStream.Msg;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.datatypes.Type;
import com.sap.sailing.domain.shared.tracking.Track;
import com.sap.sailing.domain.tracking.DynamicTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;

/**
 * A connection to an Igtimi Riot system
 * 
 * @author Axel Uhl (d043530)
 */
public interface IgtimiConnection {
    /**
     * All arguments are mandatory.
     * 
     * @param deviceSerialNumbers
     *            the serial numbers of the devices for which to return data; these numbers can be obtained, e.g., from
     *            {@link #getDevices()}.{@link Device#getSerialNumber() getSerialNumber()} or from
     *            {@link #getDataAccessWindows(Permission, TimePoint, TimePoint, Iterable)}.
     *            {@link DataAccessWindow#getDeviceSerialNumber() getDeviceSerialNumber()}.
     * @param typeAndCompression
     *            for each data type to be obtained, tells the compression level; <code>0.0</code> is a good default,
     *            meaning "no compression". Compression is currently only supported for type {@link Type#gps_latlong} where
     *            the number provided represents a maximum error in degrees of latitude and longitude.
     */
    Iterable<Fix> getResourceData(TimePoint startTime, TimePoint endTime, Iterable<String> deviceSerialNumbers,
            Map<Type, Double> typeAndCompression) throws IllegalStateException, ClientProtocolException, IOException,
            ParseException;
    
    /**
     * Shorthand for {@link #getResourceData(TimePoint, TimePoint, Iterable, Map)} where no compression is requested for
     * any type.
     */
    Iterable<Fix> getResourceData(TimePoint startTime, TimePoint endTime, Iterable<String> deviceSerialNumbers,
            Type... types) throws IllegalStateException, ClientProtocolException, IOException,
            ParseException;

    /**
     * Reads data from the remote Riot server, in case of replication explicitly addressing the request to the master/primary
     */
    Iterable<Msg> getMessages(TimePoint startTime, TimePoint endTime, Iterable<String> deviceSerialNumbers, Type[] types) throws IllegalStateException, ClientProtocolException, IOException, ParseException;

    /**
     * Same as {@link #getResourceData(TimePoint, TimePoint, Iterable, Type...)}, but the resulting {@link Fix}es are
     * grouped into {@link Track}s per fix type and per device.
     * 
     * @return a map whose keys are the devices' serial numbers and whose values are the fixes produced by the device
     *         identified by the key, grouped in a map with the fix {@link Type} as its key. Note that if a device
     *         didn't produce any fixes at all under the requested parameters, its serial number may not appear as a key
     *         in the map. Note further that should a device not have produced fixes of a given type, that type won't
     *         appear as a key in the map for that device.
     */
    Map<String, Map<Type, DynamicTrack<Fix>>> getResourceDataAsTracks(TimePoint startTime, TimePoint endTime,
            Iterable<String> deviceSerialNumbers, Type... types) throws IllegalStateException, ClientProtocolException,
            IOException, ParseException;

    /**
     * For the devices specified by <code>deviceSerialNumbers</code>, creates a live data connection. The
     * <code>account</code> needs to be authorized to access the devices' data for the current time window. Fixes
     * received through this connection are forwarded in the batches in which they are received to the listeners that
     * can be added to the live connection using {@link LiveDataConnection#addListener(BulkFixReceiver)}.
     * 
     * @return a connection that the caller can use to stop the live feed by calling {@link LiveDataConnection#stop()},
     *         or {@code null} if the collection of device serial numbers is empty or {@code null}.
     */
    LiveDataConnection getOrCreateLiveConnection(Iterable<String> deviceSerialNumbers) throws Exception;
    
    /**
     * Returns the devices that the requesting user authenticated through this connection can {@link DefaultActions#READ read}.
     * Note that this doesn't necessarily imply the user can also read <em>data</em> from this device. Only the device
     * properties such as the serial number and the device itself will then be returned.
     * 
     * TODO we could also add a Permission parameter here or use {@link HasPermissions.Action}.
     */
    Iterable<Device> getDevices() throws IllegalStateException, ClientProtocolException, IOException, ParseException;
    
    void removeDevice(Device existingDevice) throws ClientProtocolException, IOException, ParseException;
    
    /**
     * Returns all devices that this connection has access to with the requested <code>permission</code>. Note that
     * these don't necessarily need to be devices owned by the user to which this connection belongs. The user only
     * needs to have been authorized by the owner of the data to access the respective window of data.
     * 
     * @param startTime
     *            optional; may be <code>null</code>. If provided, only data access windows whose time frame has a
     *            non-empty range after this time will be returned.
     * @param endTime
     *            optional; may be <code>null</code>. If provided, only data access windows whose time frame has a
     *            non-empty range before this time will be returned.
     * @param deviceSerialNumbers
     *            optional; if not <code>null</code> and not empty, only data access windows for the devices identified
     *            by these serial numbers will be returned
     *            
     * TODO consider replacing Permission by {@link HasPermissions.Action}
     */
    Iterable<DataAccessWindow> getDataAccessWindows(Permission permission, TimePoint startTime, TimePoint endTime,
            Iterable<String> deviceSerialNumbers) throws IllegalStateException, ClientProtocolException, IOException,
            ParseException;
    
    DataAccessWindow createDataAccessWindow(String deviceSerialNumber, TimePoint startTime, TimePoint endTime) throws ClientProtocolException, IOException, ParseException;

    /**
     * Finds al@Override
    l data access windows that have wind data for the time span around the race, loads their wind data and
     * {@link DynamicTrackedRace#recordWind(com.sap.sailing.domain.tracking.Wind, com.sap.sailing.domain.common.WindSource)
     * records it} in the tracked races.
     * 
     * @return the number of wind fixes imported per tracked race; contains an entry for all elements in
     *         <code>trackedRaces</code>
     */
    Map<TrackedRace, Integer> importWindIntoRace(Iterable<DynamicTrackedRace> trackedRaces, boolean correctByDeclination) throws IllegalStateException,
            ClientProtocolException, IOException, ParseException;
    
    /**
     * Find all the devices from which we may read and which have logged GPS positions and apparent wind speed (AWS) or that
     * have never logged GPS nor wind (probably new sensors)
     * 
     * @return the serial numbers as {@link String}s for those devices that may send wind data; see also {@link Device#getSerialNumber()}.
     */
    Iterable<String> getWindDevices() throws IllegalStateException, IOException, ParseException;

    /**
     * Returns the latest datum for the specified devices that contains a fix of the <code>type</code> requested. The
     * result contains entries only for those devices that have actually produced a fix of the <code>type</code> requested
     * that is readable by the {@link #getAccount()} used by this connection.
     */
    Iterable<Fix> getLatestFixes(Iterable<String> deviceSerialNumbers, Type type) throws IllegalStateException, ClientProtocolException, IOException, ParseException;

    Msg getLastMessage(String serialNumber, Type type) throws ClientProtocolException, IOException, ParseException;
    
    Iterable<URI> getWebsocketServers() throws IllegalStateException, ClientProtocolException, IOException, ParseException, URISyntaxException;

    /**
     * Retrieves the JSON object to send in its string-serialized form to a web socket connection in order to receive
     * live data from the units whose IDs are specified by <code>deviceIds</code>. This connection's authentication
     * information is used, and data will be received only from devices that the user has {@link DefaultActions#READ}
     * permission for and a {@link DataAccessWindow} must exist for the device, spanning the current time point and with
     * the user authenticated having {@link DefaultActions#READ} permission for.
     * 
     * @param deviceIds
     *            IDs of the transmitting units expected to be visible to the requesting user
     */
    JSONObject getWebSocketConfigurationMessage(Iterable<String> deviceIds);
    
    /**
     * If this connection has a bearer token set, it will be used to authenticate the web socket
     * upgrade request passed as argument. Otherwise, this is a no-op.
     */
    void authenticate(ClientUpgradeRequest websocketUpgradeRequest);

    Map<String, Map<Type, DynamicTrack<Fix>>> getFixesAsTracks(Iterable<Fix> fixes);

    /**
     * The TCP port the Riot server is listening on for device connections
     */
    int getRiotPort() throws ClientProtocolException, IOException, ParseException;
}
