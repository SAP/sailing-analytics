package com.sap.sailing.domain.igtimiadapter.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.google.protobuf.InvalidProtocolBufferException;
import com.igtimi.IgtimiStream.Msg;
import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.domain.igtimiadapter.BulkFixReceiver;
import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;
import com.sap.sailing.domain.igtimiadapter.Device;
import com.sap.sailing.domain.igtimiadapter.FixFactory;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnection;
import com.sap.sailing.domain.igtimiadapter.LiveDataConnection;
import com.sap.sailing.domain.igtimiadapter.Permission;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.datatypes.Type;
import com.sap.sailing.domain.igtimiadapter.shared.IgtimiWindReceiver;
import com.sap.sailing.domain.igtimiadapter.websocket.LiveDataConnectionFactory;
import com.sap.sailing.domain.igtimiadapter.websocket.LiveDataConnectionFactoryImpl;
import com.sap.sailing.domain.tracking.DynamicTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.DynamicTrackImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.HttpRequestHeaderConstants;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.security.util.impl.SecuredServerImpl;
import com.sun.jersey.core.util.Base64;

public class IgtimiConnectionImpl extends SecuredServerImpl implements IgtimiConnection {
    private static final Logger logger = Logger.getLogger(IgtimiConnectionImpl.class.getName());

    /**
     * name of the JSON property indicating an error reason in a response document
     */
    private static final String REASON = "reason";

    /**
     * name of the JSON property indicating an error condition in a response document
     */
    private static final String ERROR = "error";

    private final LiveDataConnectionFactory liveDataConnectionFactory;
    
    public IgtimiConnectionImpl(final URL baseUrl, final String bearerToken) {
        super(baseUrl, bearerToken);
        liveDataConnectionFactory = new LiveDataConnectionFactoryImpl(this);
    }
    
    @Override
    public Iterable<Fix> getLatestFixes(Iterable<String> deviceSerialNumbers, Type type) throws IllegalStateException, ClientProtocolException, IOException, ParseException {
        final HttpGet getLatestData = new HttpGet(getLatestDatumUrl(deviceSerialNumbers, type));
        final JSONObject latestDataJson = (JSONObject) getJsonParsedResponse(getLatestData).getA();
        return new FixFactory().createFixes(latestDataJson);
    }
    
    @Override
    public Msg getLastMessage(String serialNumber, Type type) throws ClientProtocolException, IOException, ParseException {
        final HttpGet getLatestData = new HttpGet(getLatestDatumUrl(Collections.singleton(serialNumber), type));
        final JSONObject latestDataJson = (JSONObject) getJsonParsedResponse(getLatestData).getA();
        final JSONArray messages = (JSONArray) latestDataJson.get(serialNumber);
        final Msg result;
        if (messages == null || messages.isEmpty()) {
            result = null;
        } else {
            result = Msg.parseFrom(Base64.decode((String) messages.get(0)));
        }
        return result;
    }

    @Override
    public Iterable<Fix> getResourceData(final TimePoint startTime, final TimePoint endTime,
            Iterable<String> deviceSerialNumbers, Map<Type, Double> typeAndCompression) throws IllegalStateException, ClientProtocolException, IOException, ParseException {
        return getResourceContent(startTime, endTime, deviceSerialNumbers, typeAndCompression,
                resourceDataJson->{
                    try {
                        return new FixFactory().createFixes(resourceDataJson);
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * Reads data from the remote Riot server, in case of replication addressing the request to the master/primary
     */
    private <T> Iterable<T> getResourceContent(final TimePoint startTime, final TimePoint endTime,
            Iterable<String> deviceSerialNumbers, Map<Type, Double> typeAndCompression,
            Function<JSONObject, Iterable<T>> messageParser) throws IllegalStateException, ClientProtocolException, IOException, ParseException {
        logger.info("Requested resource data from "+startTime+" to "+endTime+" for devices "+deviceSerialNumbers+" for types "+typeAndCompression);
        final List<T> result = new ArrayList<>(); 
        // Cut interval into slices that are at most one week long. See also the discussion at
        // http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=2002 that talks about a one-month limitation
        // imposed by the Igtimi API
        TimePoint windowStartTime = startTime;
        while (!windowStartTime.after(endTime)) {
            TimePoint windowEndTime = windowStartTime.plus(Duration.ONE_WEEK);
            if (windowEndTime.after(endTime)) {
                windowEndTime = endTime;
            }
            logger.info("Obtaining resource data from "+windowStartTime+" to "+windowEndTime+" for devices "+deviceSerialNumbers+" for types "+typeAndCompression);
            final HttpGet getResourceData = new HttpGet(getResourceDataUrl(windowStartTime, windowEndTime,
                    deviceSerialNumbers, typeAndCompression));
            // resource data can be found only on the primary/master instance, so add the corresponding header to the request:
            getResourceData.addHeader(HttpRequestHeaderConstants.HEADER_FORWARD_TO_MASTER.getA(), HttpRequestHeaderConstants.HEADER_FORWARD_TO_MASTER.getB());
            final JSONObject resourceDataJson = (JSONObject) getJsonParsedResponse(getResourceData).getA();
            final String error = (String) resourceDataJson.get(ERROR);
            if (error != null) {
                String reason = (String) resourceDataJson.get(REASON);
                throw new ClientProtocolException("Error trying to obtain Igtimi resource data from " + windowStartTime
                        + " to " + windowEndTime + " from devices " + deviceSerialNumbers + ": " + error
                        + (reason == null ? "" : ". Reason: " + reason));
            }
            Util.addAll(messageParser.apply(resourceDataJson), result);
            windowStartTime = windowEndTime.plus(1);
        }
        return result;
    }

    @Override
    public Iterable<Fix> getResourceData(TimePoint startTime, TimePoint endTime, Iterable<String> deviceSerialNumbers,
            Type... types) throws IllegalStateException, ClientProtocolException, IOException, ParseException {
        Map<Type, Double> typeAndCompression = new HashMap<>();
        for (Type type : types) {
            typeAndCompression.put(type, 0.0);
        }
        return getResourceData(startTime, endTime, deviceSerialNumbers, typeAndCompression);
    }
    
    @Override
    public Iterable<Msg> getMessages(TimePoint startTime, TimePoint endTime, Iterable<String> deviceSerialNumbers, Type[] types) throws IllegalStateException, ClientProtocolException, IOException, ParseException {
        final Map<Type, Double> typeAndCompression = new HashMap<>();
        for (final Type type : types) {
            typeAndCompression.put(type, 0.0);
        }
        return getResourceContent(startTime, endTime, deviceSerialNumbers, typeAndCompression,
                resourceDataJson->{
                    final List<Msg> messages = new ArrayList<>();
                    for (final Entry<Object, Object> e : resourceDataJson.entrySet()) {
                        final JSONArray messagesAsBase64 = (JSONArray) e.getValue();
                        for (final Object msgAsBase64 : messagesAsBase64) {
                            try {
                                messages.add(Msg.parseFrom(Base64.decode(msgAsBase64.toString())));
                            } catch (InvalidProtocolBufferException e1) {
                                throw new RuntimeException(e1);
                            }
                        }
                    }
                    return messages;
                });
    }

    /**
     * Shorthand for {@link #getResourceData(TimePoint, TimePoint, Iterable, Map)} where no compression is requested for
     * any type. The fixes received are forwarded to the {@link BulkFixReceiver} <code>bulkFixReceiver</code> in one call.
     */
    private Iterable<Fix> getAndNotifyResourceData(TimePoint startTime, TimePoint endTime,
            Iterable<String> deviceSerialNumbers, BulkFixReceiver bulkFixReceiver, Type... types)
            throws IllegalStateException, ClientProtocolException, IOException, ParseException {
        Iterable<Fix> result = getResourceData(startTime, endTime, deviceSerialNumbers, types);
        logger.info("Received "+Util.size(result)+" fixes; will pass them to BulkFixReceiver for further processing now.");
        bulkFixReceiver.received(result);
        return result;
    }

    @Override
    public Map<String, Map<Type, DynamicTrack<Fix>>> getResourceDataAsTracks(TimePoint startTime, TimePoint endTime, Iterable<String> deviceSerialNumbers,
            Type... types) throws IllegalStateException, ClientProtocolException, IOException, ParseException {
        final Iterable<Fix> fixes = getResourceData(startTime, endTime, deviceSerialNumbers, types);
        final Map<String, Map<Type, DynamicTrack<Fix>>> result = getFixesAsTracks(fixes);
        return result;
    }

    @Override
    public Map<String, Map<Type, DynamicTrack<Fix>>> getFixesAsTracks(final Iterable<Fix> fixes) {
        final Map<String, Map<Type, DynamicTrack<Fix>>> result = new HashMap<>();
        for (Fix fix : fixes) {
            String deviceSerialNumber = fix.getSensor().getDeviceSerialNumber();
            Type type = fix.getType();
            DynamicTrack<Fix> track = (DynamicTrack<Fix>) getOrCreateTrack(result, deviceSerialNumber, type);
            track.add(fix);
        }
        return result;
    }
    

    @Override
    public LiveDataConnection getOrCreateLiveConnection(Iterable<String> deviceSerialNumbers) throws Exception {
        return liveDataConnectionFactory.getOrCreateLiveDataConnection(deviceSerialNumbers);
    }

    private DynamicTrack<Fix> getOrCreateTrack(Map<String, Map<Type, DynamicTrack<Fix>>> result,
            String deviceSerialNumber, Type type) {
        Map<Type, DynamicTrack<Fix>> mapForDevice = result.get(deviceSerialNumber);
        if (mapForDevice == null) {
            mapForDevice = new HashMap<>();
            result.put(deviceSerialNumber, mapForDevice);
        }
        DynamicTrack<Fix> track = mapForDevice.get(type);
        if (track == null) {
            track = new DynamicTrackImpl<Fix>("Track for Igtimi fixes of type "+type.name()+" for "+this);
            mapForDevice.put(type, track);
        }
        return track;
    }

    @Override
    public Iterable<Device> getDevices() throws IllegalStateException, ClientProtocolException, IOException, ParseException {
        final HttpGet getResources = new HttpGet(getDevicesUrl());
        final JSONObject devicesJson = (JSONObject) getJsonParsedResponse(getResources).getA();
        final List<Device> result = new ArrayList<>();
        for (Object deviceJson : (JSONArray) devicesJson.get("devices")) {
            Device device = new DeviceDeserializer().createDeviceFromJson((JSONObject) deviceJson);
            result.add(device);
        }
        return result;
    }

    @Override
    public void removeDevice(Device existingDevice) throws ClientProtocolException, IOException, ParseException {
        final HttpDelete getResources = new HttpDelete(getDeleteDeviceUrl(existingDevice.getId()));
        if (getJsonParsedResponse(getResources).getB() >= 400) {
            throw new RuntimeException("Error deleting device with ID "+existingDevice.getId());
        }
    }

    private String getDeleteDeviceUrl(long id) {
        return getApiV1BaseUrl()+"devices/"+id;
    }

    @Override
    public Iterable<DataAccessWindow> getDataAccessWindows(Permission permission, TimePoint startTime,
            TimePoint endTime, Iterable<String> deviceSerialNumbers) throws IllegalStateException, ClientProtocolException, IOException, ParseException {
        HttpGet getDataAccessWindows = new HttpGet(getDataAccessWindowsUrl(permission, startTime, endTime, deviceSerialNumbers));
        JSONObject dataAccessWindowsJson = (JSONObject) getJsonParsedResponse(getDataAccessWindows).getA();
        final List<DataAccessWindow> result = new ArrayList<>();
        for (Object dataAccessWindowJson : (JSONArray) dataAccessWindowsJson.get("data_access_windows")) {
            DataAccessWindow dataAccessWindow = new DataAccessWindowDeserializer().createDataAccessWindowFromJson(
                    (JSONObject) ((JSONObject) dataAccessWindowJson).get("data_access_window"));
            result.add(dataAccessWindow);
        }
        return result;
    }

    @Override
    public DataAccessWindow createDataAccessWindow(String deviceSerialNumber, TimePoint startTime, TimePoint endTime) throws ClientProtocolException, IOException, ParseException {
        HttpPost createDataAccessWindows = new HttpPost(getCreateDataAccessWindowsUrl(deviceSerialNumber, startTime, endTime));
        createDataAccessWindows.setEntity(new StringEntity(new DataAccessWindowSerializer().createJsonFromDataAccessWindow(
                DataAccessWindow.create(0, startTime, endTime, deviceSerialNumber)).toJSONString(), ContentType.APPLICATION_JSON));
        final JSONObject dataAccessWindowsJson = (JSONObject) getJsonParsedResponse(createDataAccessWindows).getA();
        DataAccessWindow dataAccessWindow = new DataAccessWindowDeserializer().createDataAccessWindowFromJson(dataAccessWindowsJson);
        return dataAccessWindow;
    }

    private String getCreateDataAccessWindowsUrl(String deviceSerialNumber, TimePoint startTime, TimePoint endTime) {
        StringBuilder url = new StringBuilder(getApiV1BaseUrl());
        url.append("data_access_windows");
        return url.toString();
    }

    @Override
    public Map<TrackedRace, Integer> importWindIntoRace(Iterable<DynamicTrackedRace> trackedRaces,
            boolean correctByDeclination) throws IllegalStateException, ClientProtocolException, IOException, ParseException {
        logger.info("Importing Igtimi wind for tracked races " + trackedRaces);
        TimePoint startOfWindow = new MillisecondsTimePoint(Long.MAX_VALUE);
        TimePoint endOfWindow = new MillisecondsTimePoint(0);
        for (DynamicTrackedRace trackedRace : trackedRaces) {
            startOfWindow = Collections.min(Arrays.asList(startOfWindow, IgtimiWindTracker.getReceivingStartTime(trackedRace)));
            final TimePoint receivingEndTime = IgtimiWindTracker.getReceivingEndTime(trackedRace);
            endOfWindow = Collections.max(Arrays.asList(endOfWindow, receivingEndTime==null?MillisecondsTimePoint.now():receivingEndTime));
        }
        final Iterable<DataAccessWindow> daws = getDataAccessWindows(Permission.read, startOfWindow, endOfWindow, /* find all deviceSerialNumbers for window */ null);
        logger.info("Found "+Util.size(daws)+" data access windows.");
        final Set<String> deviceSerialNumbers = Util.asSet(Util.map(daws, DataAccessWindow::getDeviceSerialNumber));
        final Map<TrackedRace, Integer> result;
        if (!deviceSerialNumbers.isEmpty()) {
            IgtimiWindReceiver windReceiver = new IgtimiWindReceiver(correctByDeclination ? DeclinationService.INSTANCE : null);
            final WindListenerSendingToTrackedRace windListener = new WindListenerSendingToTrackedRace(
                    trackedRaces, Activator.getInstance().getWindTrackerFactory());
            windReceiver.addListener(windListener);
            getAndNotifyResourceData(startOfWindow, endOfWindow, deviceSerialNumbers, windReceiver,
                    windReceiver.getFixTypes());
            result = windListener.getFixesAppliedPerTrackedRace();
            logger.info("Imported the following number of wind fixes for the following list of races: "+result);
        } else {
            logger.info("No Igtimi devices that measure wind found for time window "+startOfWindow+".."+endOfWindow);
            result = new HashMap<>();
        }
        return result;
    }

    @Override
    public Iterable<String> getWindDevices() throws IllegalStateException, ClientProtocolException, IOException, ParseException {
        // find all the devices from which we may read
        Iterable<DataAccessWindow> dataAccessWindows = getDataAccessWindows(
                Permission.read, /* start time */ null, /* end time */ null,
                /* get data for all available deviceSerialNumbers */null);
        Set<String> deviceSerialNumbersWeCanRead = new HashSet<>();
        for (DataAccessWindow daw : dataAccessWindows) {
            deviceSerialNumbersWeCanRead.add(daw.getDeviceSerialNumber());
        }
        // find all that haven't even sent GPS; those may never have sent ever, so we need to listen to them for new stuff; they could be wind sensors
        Iterable<Fix> gpsFixes = getLatestFixes(deviceSerialNumbersWeCanRead, Type.gps_latlong);
        Set<String> devicesWithGps = getDeviceSerialNumbers(gpsFixes);
        Iterable<Fix> awsFixes = getLatestFixes(deviceSerialNumbersWeCanRead, Type.AWS); // look for latest fixes with apparent wind speed in the fix
        Set<String> devicesWithWind = getDeviceSerialNumbers(awsFixes);
        Set<String> devicesThatHaveNeverSentGpsNorWind = new HashSet<>(deviceSerialNumbersWeCanRead);
        devicesThatHaveNeverSentGpsNorWind.removeAll(devicesWithGps);
        devicesThatHaveNeverSentGpsNorWind.removeAll(devicesWithWind);
        Set<String> devicesWeShouldListenTo = new HashSet<>();
        devicesWeShouldListenTo.addAll(devicesWithWind);
        devicesWeShouldListenTo.addAll(devicesThatHaveNeverSentGpsNorWind);
        logger.info("Wind devices identified: "
                + devicesWeShouldListenTo
                + " because from all devices "+deviceSerialNumbersWeCanRead+" for "
                + devicesThatHaveNeverSentGpsNorWind
                + " we don't know what they are as they never sent anything we can access, and for "
                + devicesWithWind + " we know they sent wind");
        return devicesWeShouldListenTo;
    }
    
    private Set<String> getDeviceSerialNumbers(Iterable<Fix> fixes) {
        Set<String> deviceSerialNumbers = new HashSet<>();
        for (Fix fix : fixes) {
            deviceSerialNumbers.add(fix.getSensor().getDeviceSerialNumber());
        }
        return deviceSerialNumbers;
    }

    /**
     * @return trailing slash
     */
    private String getApiV1BaseUrl() {
        return getBaseUrl()+"/igtimi/api/v1/";
    }

    /**
     * Retrieves the JSON object to send in its string-serialized form to a web socket connection in order to receive
     * live data from the units whose IDs are specified by <code>deviceIds</code>. The sending units are expected to
     * belong to the user account to which this factory's application client has been granted permission.
     * 
     * @param deviceIds
     *            IDs of the transmitting units expected to be visible to the requesting user
     */
    @Override
    public JSONObject getWebSocketConfigurationMessage(Iterable<String> deviceIds) {
        JSONObject result = new JSONObject();
        JSONArray deviceIdsJson = new JSONArray();
        result.put("devices", deviceIdsJson);
        for (String deviceId : deviceIds) {
            deviceIdsJson.add(deviceId);
        }
        return result;
    }
    
    private String getDevicesUrl() {
        return getApiV1BaseUrl()+"devices/";
    }

    private String getDataAccessWindowsUrl(Permission permission, TimePoint startTime, TimePoint endTime,
            Iterable<String> deviceSerialNumbers) {
        StringBuilder url = new StringBuilder(getApiV1BaseUrl());
        url.append("data_access_windows?type=");
        url.append(permission.name());
        if (startTime != null) {
            url.append("&start_time=");
            url.append(startTime.asMillis());
        }
        if (endTime != null) {
            url.append("&end_time=");
            url.append(endTime.asMillis());
        }
        if (deviceSerialNumbers != null) {
            for (String serialNumber : deviceSerialNumbers) {
                url.append("&serial_numbers[]=");
                url.append(serialNumber);
            }
        }
        return url.toString();
    }

    private String getLatestDatumUrl(Iterable<String> deviceSerialNumbers, Type type) {
        final StringBuilder url = new StringBuilder(getApiV1BaseUrl());
        url.append("resources/data/latest?type=");
        url.append(type.getCode());
        for (String deviceSerialNumber : deviceSerialNumbers) {
            url.append("&serial_numbers[]=");
            url.append(deviceSerialNumber);
        }
        return url.toString();
    }

    private String getResourceDataUrl(TimePoint startTime, TimePoint endTime, Iterable<String> serialNumbers,
            Map<Type, Double> typeAndCompression) {
        final StringBuilder url = new StringBuilder(getApiV1BaseUrl());
        url.append("resources/data?start_time=");
        url.append(startTime.asMillis());
        url.append("&end_time=");
        url.append(endTime.asMillis());
        for (String serialNumber : serialNumbers) {
            url.append("&serial_numbers[]=");
            url.append(serialNumber);
        }
        for (Entry<Type, Double> e : typeAndCompression.entrySet()) {
            url.append("&types["+e.getKey().getCode()+"]="+e.getValue());
        }
        url.append("&restore_archives=true");
        return url.toString();
    }

    @Override
    public Iterable<URI> getWebsocketServers() throws IllegalStateException, ClientProtocolException, IOException, ParseException, URISyntaxException {
        final HttpGet getWebsocketServers = new HttpGet(getApiV1BaseUrl()+"server_listers/web_sockets");
        final JSONObject serversJson = (JSONObject) getJsonParsedResponse(getWebsocketServers).getA();
        final List<URI> result = new ArrayList<>();
        for (Object serverUrl : (JSONArray) serversJson.get("web_socket_servers")) {
            URI uri = new URI((String) serverUrl);
            result.add(uri);
        }
        Collections.shuffle(result); // shuffle as a failover strategy
        logger.info("Trying Igtimi WebSocket servers in the following order: "+result);
        return result;
    }
    
    @Override
    public int getRiotPort() throws ClientProtocolException, IOException, ParseException {
        final HttpGet getServer = new HttpGet(getApiV1BaseUrl()+"server");
        final JSONObject serverJson = (JSONObject) getJsonParsedResponse(getServer).getA();
        return Integer.valueOf(serverJson.get("port").toString());
    }

    @Override
    public void authenticate(ClientUpgradeRequest websocketUpgradeRequest) {
        super.authenticate(websocketUpgradeRequest);
    }
}