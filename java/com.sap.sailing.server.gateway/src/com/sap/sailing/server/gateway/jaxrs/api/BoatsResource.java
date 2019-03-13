package com.sap.sailing.server.gateway.jaxrs.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringEscapeUtils;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sailing.server.gateway.serialization.impl.BoatJsonSerializer;
import com.sap.sse.common.Util;

@Path("/v1/boats")
public class BoatsResource extends AbstractSailingServerResource {
    
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{boatId}")
    public Response getBoat(@PathParam("boatId") String boatIdAsString, @PathParam("secret") String regattaSecret,
            @QueryParam("leaderboardname") String leaderboardName) {
        Response response;
        Boat boat = getService().getCompetitorAndBoatStore().getExistingBoatByIdAsString(boatIdAsString);
        if (boat == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a boat with id '" + StringEscapeUtils.escapeHtml(boatIdAsString) + "'.")
                    .type(MediaType.TEXT_PLAIN).build();
        } else {
            boolean skip = skipChecksDueToCorrectSecret(leaderboardName, regattaSecret);
            boolean boatInRegatta = false;
            if (skip) {
                Regatta regatta = getService().getRegattaByName(leaderboardName);
                boatInRegatta = Util.contains(regatta.getAllBoats(), boat);
            }
            if (!(skip && boatInRegatta)) {
                getSecurityService().checkCurrentUserReadPermission(boat);
            }
            BoatJsonSerializer boatJsonSerializer = BoatJsonSerializer.create();
            String jsonString = boatJsonSerializer.serialize(boat).toJSONString();
            response = Response.ok(jsonString).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
        }
        return response;
    }
}
