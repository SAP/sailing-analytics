package com.sap.sailing.domain.igtimiadapter.gateway.impl;

import java.io.IOException;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.http.client.ClientProtocolException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.igtimi.IgtimiData.DataPoint.DataCase;
import com.igtimi.IgtimiStream.Msg;
import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;
import com.sap.sailing.domain.igtimiadapter.Device;
import com.sap.sailing.domain.igtimiadapter.IgtimiConnection;
import com.sap.sailing.domain.igtimiadapter.datatypes.Type;
import com.sap.sailing.domain.igtimiadapter.server.riot.RiotServer;
import com.sap.sse.common.MultiTimeRange;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;

@Path(RestApiApplication.API + RestApiApplication.V1 + RiotResourcesResource.RESOURCES)
public class RiotResourcesResource extends AbstractRiotServerResource {
    protected static final String RESOURCES = "/resources";
    private static final String DATA = "/data";
    private static final String LATEST = "/latest";
    
    /**
     * Output is the same format as for {@link #getResourcesData(UriInfo)}
     */
    @Path(DATA+LATEST)
    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getLatestDatum(
            @QueryParam("serial_numbers[]") Set<String> serialNumbers,
            @QueryParam("type") int type) throws ParseException, IOException {
        final RiotServer riot = Activator.getInstance().getRiotServer();
        final JSONObject result = new JSONObject();
        final Subject subject = SecurityUtils.getSubject();
        final Iterable<String> serialNumbersToUse = serialNumbers==null || serialNumbers.isEmpty()
                ? Util.map(getRiotService().getDevices(), d->d.getSerialNumber())
                : serialNumbers;
        final Iterable<DataAccessWindow> daws = getDataAccessWindowsReadableBySubject(MultiTimeRange.of(TimeRange.create(null, null)), serialNumbersToUse);
        final Map<String, MultiTimeRange> multiTimeRangesPerDeviceSerialNumber = getTimeRangesByDeviceSerialNumber(daws);
        for (final String serialNumber : serialNumbersToUse) {
            final Device device = getRiotService().getDeviceBySerialNumber(serialNumber);
            if (device != null && subject.isPermitted(
                        device.getIdentifier().getStringPermission(DefaultActions.READ))) {
                final Msg lastMessage = riot.getLastMessage(serialNumber, DataCase.forNumber(type), multiTimeRangesPerDeviceSerialNumber.get(serialNumber));
                if (lastMessage != null) {
                    final JSONArray singleMessage = new JSONArray();
                    singleMessage.add(Base64.getEncoder().encodeToString(lastMessage.toByteArray()));
                    result.put(serialNumber, singleMessage);
                }
            }
        }
        return Response.ok(streamingOutput(result)).build();
    }

    /**
     * A response of a successful request contains a {@link JSONObject} where the keys are the device serial numbers,
     * and where the values are arrays of {@link String}s representing Base64-encoded binary protobuf messages that are
     * expected to parse as {@link Msg} objects. There is no guarantee for these messages to be in any specific order.<p>
     * 
     * Should this request have arrived on a replica (although our own client implementation {@link IgtimiConnection} would
     * explicitly address the master/primary instance), a remote request using an {@link IgtimiConnection} is issued to
     * the primary/master to obtain and forward the data requested.
     */
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path(DATA)
    public Response getResourcesData(@Context UriInfo ui) throws IllegalStateException, ClientProtocolException, IOException, ParseException {
        final MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        final String startTimeString = queryParams.getFirst("start_time");
        final TimePoint startTime = startTimeString == null ? null : TimePoint.of(Long.valueOf(startTimeString));
        final String endTimeString = queryParams.getFirst("end_time");
        final TimePoint endTime = endTimeString == null ? null : TimePoint.of(Long.valueOf(endTimeString));
        final Iterable<String> serialNumbers = queryParams.get("serial_numbers[]") == null ?
                Util.map(getRiotService().getDevices(), d->d.getSerialNumber()) : queryParams.get("serial_numbers[]");
        // The "restore_archives" parameter was part of the original Riot API, but we currently don't implement any archiving
        // procedure for the resource messages, so we ignore it.
        // final Boolean restoreArchives = queryParams.containsKey("restore_archives") ? Boolean.valueOf(queryParams.getFirst("restore_archives")) : null;
        final Map<Type, Double> typesAndCompression = new HashMap<>();
        final Set<DataCase> dataCases = new HashSet<>();
        for (final DataCase dataCase : DataCase.values()) {
            final String typesAndCompressionKey = "types["+dataCase.getNumber()+"]";
            if (queryParams.containsKey(typesAndCompressionKey)) {
                dataCases.add(dataCase);
                final Type type;
                if ((type = Type.valueOf(dataCase.getNumber())) != null) {
                    typesAndCompression.put(type, Double.valueOf(queryParams.getFirst(typesAndCompressionKey)));
                }
            }
        }
        final TimeRange requestedTimeRange = TimeRange.create(startTime, endTime);
        final Subject subject = SecurityUtils.getSubject();
        final Iterable<String> serialNumbersOfDevicesUserCanRead =
                Util.filter(serialNumbers, serialNumber->subject.isPermitted(
                        getRiotService().getDeviceBySerialNumber(serialNumber).getIdentifier().getStringPermission(DefaultActions.READ)));
        final Iterable<DataAccessWindow> daws = getDataAccessWindowsReadableBySubject(MultiTimeRange.of(requestedTimeRange), serialNumbersOfDevicesUserCanRead);
        final Map<String, MultiTimeRange> multiTimeRangesPerDeviceSerialNumber = getTimeRangesByDeviceSerialNumber(daws);
        // assemble results from DAWs readable by the user:
        final JSONObject result = new JSONObject();
        final Encoder base64Encoder = Base64.getEncoder();
        for (final Entry<String, MultiTimeRange> deviceSerialNumberAndMultiTimeRange : multiTimeRangesPerDeviceSerialNumber.entrySet()) {
            final MultiTimeRange timeRangeIntersectionBetweenRequestAndDAWs = deviceSerialNumberAndMultiTimeRange.getValue().intersection(requestedTimeRange);
            if (!timeRangeIntersectionBetweenRequestAndDAWs.isEmpty()) {
                final Iterable<Msg> messagesForDAW = getRiotService().getMessages(deviceSerialNumberAndMultiTimeRange.getKey(), timeRangeIntersectionBetweenRequestAndDAWs, dataCases);
                final JSONArray arrayForDevice = (JSONArray) result.computeIfAbsent(deviceSerialNumberAndMultiTimeRange.getKey(), d->new JSONArray());
                for (final Msg message : messagesForDAW) {
                    arrayForDevice.add(new String(base64Encoder.encode(message.toByteArray())));
                }
            }
        }
        return Response.ok(streamingOutput(result)).build();
    }

    private Map<String, MultiTimeRange> getTimeRangesByDeviceSerialNumber(Iterable<DataAccessWindow> daws) {
        final Map<String, MultiTimeRange> result = new HashMap<>();
        for (final DataAccessWindow daw : daws) {
            result.compute(daw.getDeviceSerialNumber(),
                    (serialNumber, multiTimeRange)->multiTimeRange == null
                        ? MultiTimeRange.of(daw.getTimeRange())
                        : multiTimeRange.union(daw.getTimeRange()));
        }
        return result;
    }

    private Iterable<DataAccessWindow> getDataAccessWindowsReadableBySubject(MultiTimeRange timeRanges, Iterable<String> serialNumbers) {
        final Subject subject = SecurityUtils.getSubject();
        return Util.filter(getRiotService().getDataAccessWindows(serialNumbers, timeRanges),
                daw->{
                    return subject.isPermitted(daw.getIdentifier().getStringPermission(DefaultActions.READ));
                });
    }
}
