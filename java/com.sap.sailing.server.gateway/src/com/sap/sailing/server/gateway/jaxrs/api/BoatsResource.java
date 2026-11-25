package com.sap.sailing.server.gateway.jaxrs.api;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringEscapeUtils;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.server.gateway.serialization.impl.BoatJsonSerializer;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.RGBColor;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.shared.util.impl.UUIDHelper;

@Path("/v1/boats")
public class BoatsResource extends AbstractSailingServerResource {
    
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{boatId}")
    public Response getBoat(@PathParam("boatId") String boatIdAsString, @QueryParam("secret") String regattaSecret,
            @QueryParam("leaderboardName") String leaderboardName) {
        Response response;
        Boat boat = getService().getCompetitorAndBoatStore().getExistingBoatByIdAsString(boatIdAsString);
        if (boat == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a boat with id '" + StringEscapeUtils.escapeHtml(boatIdAsString) + "'.")
                    .type(MediaType.TEXT_PLAIN).build();
        } else {
            boolean skip = getService().skipChecksDueToCorrectSecret(leaderboardName, regattaSecret);
            boolean boatInRegatta = false;
            if (skip) {
                Regatta regatta = getService().getRegattaByName(leaderboardName);
                boatInRegatta = Util.contains(regatta.getAllBoats(), boat);
            }
            if (!(skip && boatInRegatta)) {
                getSecurityService().checkCurrentUserHasOneOfExplicitPermissions(boat,
                        SecuredSecurityTypes.PublicReadableActions.READ_AND_READ_PUBLIC_ACTIONS);
            }
            BoatJsonSerializer boatJsonSerializer = BoatJsonSerializer.create();
            response = Response.ok(streamingOutput(boatJsonSerializer.serialize(boat))).build();
        }
        return response;
    }

    /**
     * Creates a boat on its own, without a competitor, persistently in the competitor and boat store, the same way the
     * boat table of the AdminConsole does it. This is what a regatta with
     * boats changing per race ({@code canBoatsOfCompetitorsChangePerRace}) needs, where a set of boats is shared
     * by all competitors and assigned to them per race.
     * <p>
     *
     * Security-wise, the calling subject needs the {@code BOAT:CREATE} permission for the new boat; the boat's
     * ownership is set to the current user and default tenant.
     *
     * @param boatName
     *            the boat's name; defaults to {@code sailId} if not provided; one of the two is required
     * @param sailId
     *            the boat's sail number; optional
     * @param displayColor
     *            the boat's color as an HTML color such as {@code #ff0000}; optional
     * @param boatClassName
     *            the name of the boat class; the boat class is created if it does not exist yet; required
     * @param boatIdAsString
     *            optional ID for the new boat; if it parses as a UUID, a {@link UUID} is used, else the string itself;
     *            if omitted, a random UUID is used. If a boat with this ID exists already, {@link Status#CONFLICT} is
     *            returned.
     * @return the boat created, serialized with {@link BoatJsonSerializer}
     */
    @POST
    @Produces("application/json;charset=UTF-8")
    public Response createBoat(@QueryParam("name") final String boatName, @QueryParam("sailid") final String sailId,
            @QueryParam("displayColor") final String displayColor, @QueryParam("boatclass") final String boatClassName,
            @QueryParam("boatId") final String boatIdAsString) {
        final Response response;
        final String effectiveBoatName = boatName == null || boatName.isEmpty() ? sailId : boatName;
        final Serializable boatId = boatIdAsString == null || boatIdAsString.isEmpty() ? UUID.randomUUID()
                : UUIDHelper.tryUuidConversion(boatIdAsString);
        if (boatClassName == null || boatClassName.isEmpty()) {
            response = getBadRequestResponse("Parameter boatclass is required.");
        } else if (effectiveBoatName == null || effectiveBoatName.isEmpty()) {
            response = getBadRequestResponse("Provide a name or a sailid for the boat.");
        } else if (!isValidColor(displayColor)) {
            response = getBadRequestResponse(
                    "Invalid displayColor '" + StringEscapeUtils.escapeHtml(displayColor) + "'; use #rrggbb.");
        } else if (getService().getCompetitorAndBoatStore().getExistingBoatByIdAsString(boatId.toString()) != null) {
            response = Response.status(Status.CONFLICT)
                    .entity("A boat with id '" + StringEscapeUtils.escapeHtml(boatId.toString()) + "' already exists.")
                    .type(MediaType.TEXT_PLAIN).build();
        } else {
            final Color color = displayColor == null || displayColor.isEmpty() ? null : new RGBColor(displayColor);
            // The boat class is resolved (and possibly created) only inside the job, which runs after the
            // permission check, so that an unauthorized request leaves no new boat class behind.
            final Callable<DynamicBoat> createBoatJob = () -> {
                final BoatClass boatClass = getService().getBaseDomainFactory().getOrCreateBoatClass(boatClassName);
                return getService().getBaseDomainFactory().getOrCreateBoat(boatId, effectiveBoatName, boatClass,
                        sailId, color, /* storePersistently */ true);
            };
            final DynamicBoat boat = getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                    SecuredDomainType.BOAT, BoatImpl.getTypeRelativeObjectIdentifier(boatId), effectiveBoatName,
                    createBoatJob);
            response = Response.ok(streamingOutput(BoatJsonSerializer.create().serialize(boat))).build();
        }
        return response;
    }

    private boolean isValidColor(final String htmlColor) {
        boolean valid;
        if (htmlColor == null || htmlColor.isEmpty()) {
            valid = true;
        } else {
            try {
                new RGBColor(htmlColor);
                valid = true;
            } catch (IllegalArgumentException e) {
                valid = false;
            }
        }
        return valid;
    }

    private Response getBadRequestResponse(final String message) {
        return Response.status(Status.BAD_REQUEST).entity(message).type(MediaType.TEXT_PLAIN).build();
    }
}
