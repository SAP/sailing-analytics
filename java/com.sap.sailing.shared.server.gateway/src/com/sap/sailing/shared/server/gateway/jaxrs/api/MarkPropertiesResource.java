package com.sap.sailing.shared.server.gateway.jaxrs.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.coursetemplate.MarkProperties;
import com.sap.sailing.domain.coursetemplate.MarkPropertiesBuilder;
import com.sap.sailing.domain.coursetemplate.Positioning;
import com.sap.sailing.domain.coursetemplate.impl.FixedPositioningImpl;
import com.sap.sailing.domain.coursetemplate.impl.TrackingDeviceBasedPositioningImpl;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneUUIDIdentifierImpl;
import com.sap.sailing.server.gateway.serialization.impl.DeviceIdentifierJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.MarkPropertiesJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.DeviceIdentifierJsonHandler;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.impl.PlaceHolderDeviceIdentifierJsonHandler;
import com.sap.sailing.shared.server.gateway.jaxrs.SharedAbstractSailingServerResource;
import com.sap.sse.common.Color;
import com.sap.sse.common.Position;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.RGBColor;
import com.sap.sse.shared.json.JsonSerializer;
import com.sun.jersey.api.client.ClientResponse.Status;

@Path("/v1/markproperties")
public class MarkPropertiesResource extends SharedAbstractSailingServerResource {
    private JsonSerializer<MarkProperties> markPropertiesSerializer;
    
    public MarkPropertiesResource() {
    }

    private Response getBadMarkPropertiesValidationErrorResponse(String errorText) {
        return Response.status(Status.BAD_REQUEST).entity(StringEscapeUtils.escapeHtml(errorText) + ".")
                .type(MediaType.TEXT_PLAIN).build();
    }

    private Response getMarkPropertiesNotFoundErrorResponse() {
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getMarkProperties(@QueryParam("tags") List<String> tags) throws Exception {
        Iterable<MarkProperties> markPropertiesList = getSharedSailingData().getAllMarkProperties(tags);
        JSONArray result = new JSONArray();
        for (MarkProperties markProperties : markPropertiesList) {
            result.add(getMarkPropertiesSerializer().serialize(markProperties));
        }
        return Response.ok(streamingOutput(result)).build();
    }

    @GET
    @Path("{markPropertiesId}")
    @Produces("application/json;charset=UTF-8")
    public Response getMarkProperties(@PathParam("markPropertiesId") String markPropertiesId) throws Exception {
        MarkProperties markProperties = getSharedSailingData().getMarkPropertiesById(UUID.fromString(markPropertiesId));
        if (markProperties == null) {
            return getMarkPropertiesNotFoundErrorResponse();
        }
        final JSONObject serializedMarkProperties = getMarkPropertiesSerializer().serialize(markProperties);
        return Response.ok(streamingOutput(serializedMarkProperties)).build();
    }

    @POST
    @Produces("application/json;charset=UTF-8")
    public Response createMarkProperties(@FormParam("name") final String name,
            @FormParam("shortName") final String shortName, @FormParam("deviceUuid") String deviceUuid,
            @FormParam("color") String rgbColor, @FormParam("shape") String shape, @FormParam("pattern") String pattern,
            @FormParam("markType") final String markType, @FormParam("tags") List<String> tags,
            @FormParam("latDeg") Double latDeg, @FormParam("lonDeg") Double lonDeg) throws Exception {
        if (name == null || name.isEmpty()) {
            return getBadMarkPropertiesValidationErrorResponse("name must be given");
        }
        final String effectiveShortName;
        if (shortName == null || shortName.isEmpty()) {
            effectiveShortName = name;
        } else {
            effectiveShortName = shortName;
        }
        
        Color color = null;
        if (rgbColor != null && rgbColor.length() > 0) {
            try {
                color = new RGBColor(rgbColor);
            } catch (IllegalArgumentException iae) {
                return getBadMarkPropertiesValidationErrorResponse(String.format("invalid color %s", iae.getMessage()));
            }
        }
        MarkType type = null;
        if (markType != null && markType.length() > 0) {
            type = MarkType.valueOf(markType);
        }
        if (deviceUuid != null && deviceUuid.length() > 0 && (latDeg != null || lonDeg != null)) {
            return getBadMarkPropertiesValidationErrorResponse("deviceUuid and fixed positioning cannot be used together");
        }
        if (latDeg != null && lonDeg == null || latDeg == null && lonDeg != null) {
            return getBadMarkPropertiesValidationErrorResponse("incomplete positioning");
        }
        final MarkPropertiesBuilder markPropertiesBuilder = new MarkPropertiesBuilder(/* id */ null, name, effectiveShortName,
                color, shape, pattern, type);
        final MarkProperties createdMarkProperties = getSharedSailingData()
                .createMarkProperties(markPropertiesBuilder.build(), tags, Optional.empty());
        if (deviceUuid != null && deviceUuid.length() > 0) {
            final DeviceIdentifier device = new SmartphoneUUIDIdentifierImpl(UUID.fromString(deviceUuid));
            getSharedSailingData().setTrackingDeviceIdentifierForMarkProperties(createdMarkProperties, device);
        } 
        if (latDeg != null && lonDeg != null) {
            final Position fixedPosition = new DegreePosition(latDeg, lonDeg);
            getSharedSailingData().setFixedPositionForMarkProperties(createdMarkProperties, fixedPosition);
        }
        final JSONObject serializedMarkProperties = getMarkPropertiesSerializer().serialize(createdMarkProperties);
        return Response.ok(streamingOutput(serializedMarkProperties)).build();
    }

    @PUT
    @Path("{markPropertiesId}/positioning")
    @Produces("application/json;charset=UTF-8")
    public Response updateMarkPropertiesPositioning(@PathParam("markPropertiesId") String markPropertiesId,
            @FormParam("deviceUuid") String deviceUuid, @FormParam("latDeg") Double latDeg,
            @FormParam("lonDeg") Double lonDeg) throws Exception {
        final MarkProperties markProperties = getSharedSailingData().getMarkPropertiesById(UUID.fromString(markPropertiesId));
        if (markProperties == null) {
            return getMarkPropertiesNotFoundErrorResponse();
        }
        if (deviceUuid != null && deviceUuid.length() > 0) {
            final DeviceIdentifier device = new SmartphoneUUIDIdentifierImpl(UUID.fromString(deviceUuid));
            getSharedSailingData().setTrackingDeviceIdentifierForMarkProperties(markProperties, device);
        } else if (latDeg != null && lonDeg != null) {
            final Position fixedPosition = new DegreePosition(latDeg, lonDeg);
            getSharedSailingData().setFixedPositionForMarkProperties(markProperties, fixedPosition);
        } else {
            getSharedSailingData().clearPositioningForMarkProperties(markProperties);
        }
        final JSONObject serializedMarkProperties = getMarkPropertiesSerializer().serialize(markProperties);
        return Response.ok(streamingOutput(serializedMarkProperties)).build();
    }

    @PUT
    @Path("{markPropertiesId}")
    @Produces("application/json;charset=UTF-8")
    public Response updateMarkProperties(@PathParam("markPropertiesId") String markPropertiesId,
            @FormParam("name") final String name,
            @FormParam("shortName") final String shortName,
            @FormParam("color") String rgbColor, @FormParam("shape") String shape, @FormParam("pattern") String pattern,
            @FormParam("markType") final String markType, @FormParam("tags") List<String> tags,
            @FormParam("deviceUuid") String deviceUuid, @FormParam("latDeg") Double latDeg,
            @FormParam("lonDeg") Double lonDeg) throws Exception {
        final UUID markPropertiesUUID = UUID.fromString(markPropertiesId);
        final MarkProperties markProperties = getSharedSailingData().getMarkPropertiesById(markPropertiesUUID);
        if (markProperties == null) {
            return getMarkPropertiesNotFoundErrorResponse();
        }
        if (name == null || name.isEmpty()) {
            return getBadMarkPropertiesValidationErrorResponse("name must be given");
        }
        final String effectiveShortName;
        if (shortName == null || shortName.isEmpty()) {
            effectiveShortName = name;
        } else {
            effectiveShortName = shortName;
        }
        Color color = null;
        if (rgbColor != null && rgbColor.length() > 0) {
            try {
                color = new RGBColor(rgbColor);
            } catch (IllegalArgumentException iae) {
                return getBadMarkPropertiesValidationErrorResponse(String.format("invalid color %s", iae.getMessage()));
            }
        }
        MarkType type = null;
        if (markType != null && markType.length() > 0) {
            type = MarkType.valueOf(markType);
        }
        final MarkPropertiesBuilder markPropertiesBuilder = new MarkPropertiesBuilder(/* id */ null, name, effectiveShortName,
                color, shape, pattern, type);
        final Positioning positioningInformation;
        if (deviceUuid != null && deviceUuid.length() > 0) {
            positioningInformation = new TrackingDeviceBasedPositioningImpl(new SmartphoneUUIDIdentifierImpl(UUID.fromString(deviceUuid)));
        } else if (latDeg != null && lonDeg != null) {
            final Position fixedPosition = new DegreePosition(latDeg, lonDeg);
            positioningInformation = new FixedPositioningImpl(fixedPosition);
        } else {
            positioningInformation = null;
        }
        getSharedSailingData().updateMarkProperties(markPropertiesUUID, markPropertiesBuilder.build(), positioningInformation, tags);
        final JSONObject serializedMarkProperties = getMarkPropertiesSerializer().serialize(markProperties);
        return Response.ok(streamingOutput(serializedMarkProperties)).build();
    }

    @DELETE
    @Path("{markPropertiesId}")
    public Response deleteMarkProperties(@PathParam("markPropertiesId") String markPropertiesId) throws Exception {
        MarkProperties markProperties = getSharedSailingData().getMarkPropertiesById(UUID.fromString(markPropertiesId));
        if (markProperties == null) {
            return getMarkPropertiesNotFoundErrorResponse();
        }
        getSharedSailingData().deleteMarkProperties(markProperties);
        return Response.ok().build();
    }

    private synchronized JsonSerializer<MarkProperties> getMarkPropertiesSerializer() {
        if (markPropertiesSerializer == null) {
            final TypeBasedServiceFinder<DeviceIdentifierJsonHandler> deviceJsonServiceFinder = getServiceFinderFactory()
                    .createServiceFinder(DeviceIdentifierJsonHandler.class);
            deviceJsonServiceFinder.setFallbackService(new PlaceHolderDeviceIdentifierJsonHandler());
            markPropertiesSerializer = new MarkPropertiesJsonSerializer(new DeviceIdentifierJsonSerializer(deviceJsonServiceFinder));
        }
        return markPropertiesSerializer;
    }
}
