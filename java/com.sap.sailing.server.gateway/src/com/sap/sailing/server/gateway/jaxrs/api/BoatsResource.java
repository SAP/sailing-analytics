package com.sap.sailing.server.gateway.jaxrs.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.common.tracking.impl.BoatJsonConstants;
import com.sap.sailing.server.gateway.jaxrs.AbstractSailingServerResource;

@Path("/v1/boats")
public class BoatsResource extends AbstractSailingServerResource {
    
    public static JSONObject getBoatJSON(Boat boat) {
        JSONObject json = new JSONObject();
        json.put(BoatJsonConstants.FIELD_ID, boat.getId().toString());
        json.put(BoatJsonConstants.FIELD_NAME, boat.getName());
        json.put(BoatJsonConstants.FIELD_SAIL_ID, boat.getSailID());
        json.put(BoatJsonConstants.FIELD_BOAT_CLASS_NAME, boat.getBoatClass().getName());
        json.put(BoatJsonConstants.FIELD_COLOR, boat.getColor() != null ? boat.getColor().getAsHtml() : null);
        return json;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{boatId}")
    public Response getBoat(@PathParam("boatId") String boatIdAsString) {
        Response response;
        Boat boat = getService().getCompetitorStore().getExistingBoatByIdAsString(boatIdAsString);
        if (boat == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a boat with id '" + StringEscapeUtils.escapeHtml(boatIdAsString) + "'.")
                    .type(MediaType.TEXT_PLAIN).build();
        } else {
            String jsonString = getBoatJSON(boat).toJSONString();
            response = Response.ok(jsonString).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
        }
        return response;
    }
}
