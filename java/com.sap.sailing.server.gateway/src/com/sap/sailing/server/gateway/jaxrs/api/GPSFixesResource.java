package com.sap.sailing.server.gateway.jaxrs.api;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.common.racelog.tracking.TransformationException;
import com.sap.sailing.domain.racelogtracking.DeviceIdentifier;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneUUIDIdentifierImpl;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sailing.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.Util.Pair;

@Path("/v1/gps_fixes")
public class GPSFixesResource extends AbstractSailingServerResource {
    private static final Logger logger = Logger.getLogger(GPSFixesResource.class.getName());
    private final JsonDeserializer<Pair<UUID, List<GPSFixMoving>>> deserializer =
            new FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postFixes(String json) {
        Pair<UUID, List<GPSFixMoving>> data = null;
        try {
            logger.fine("Post issued to " + this.getClass().getName());
            Object requestBody = JSONValue.parseWithException(json);
            JSONObject requestObject = Helpers.toJSONObjectSafe(requestBody);
            logger.fine("JSON requestObject is: " + requestObject.toString());
            data = deserializer.deserialize(requestObject);
        } catch (ParseException | JsonDeserializationException e) {
            logger.warning(String.format("Exception while parsing post request:\n%s", e.toString()));
            return Response.status(Status.BAD_REQUEST).entity("Invalid JSON body in request").type(MediaType.TEXT_PLAIN).build();
        }
        
        DeviceIdentifier device = new SmartphoneUUIDIdentifierImpl(data.getA());
        List<GPSFixMoving> fixes = data.getB();

        try {
            for (GPSFixMoving fix : fixes) {
                getService().getGPSFixStore().storeFix(device, fix);
            }
            logger.log(Level.INFO, "Added " + fixes.size() + " fixes for device " + device.toString()  + " to store");
        } catch (TransformationException | NoCorrespondingServiceRegisteredException e) {
            logger.log(Level.WARNING, "Could not store fix for device " + device);
        }

        return Response.ok().build();
    }
}
 