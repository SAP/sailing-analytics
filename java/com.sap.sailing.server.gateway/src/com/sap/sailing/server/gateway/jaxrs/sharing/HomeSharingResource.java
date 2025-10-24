package com.sap.sailing.server.gateway.jaxrs.sharing;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.security.SecurityService;

@Path("/home")
public class HomeSharingResource extends AbstractSailingServerResource {
    
    @Context
    UriInfo uriInfo;

    public HomeSharingResource() {
    }

    @GET
    @Path("/events/{eventId}")
    @Produces("text/html")
    public String getSharedEvent(@HeaderParam("user-agent") String userAgent, @PathParam("eventId") String eventId) {
        RacingEventService eventService = getService();
        UUID uuid = UUID.fromString(eventId);
        final Event event = eventService.getEvent(uuid);
        if (event != null) {
            SecurityService securityService = getSecurityService();
            securityService.checkCurrentUserReadPermission(event);
            final String title = event.getName();
            final String description = HomeSharingUtils.findDescription(event);
            final String imageUrl = HomeSharingUtils.findTeaserImageUrl(event);
            String placeUrl = new TokenizedHomePlaceUrlBuilder(uriInfo).asEventPlaceLink(eventId);
            final Map<String, String> replacementMap = HomeSharingUtils.createReplacementMap(title,
                    description, imageUrl, placeUrl, userAgent);
            String content = HomeSharingUtils.loadSharingHTML(this.getClass().getClassLoader(), uriInfo);
            return HomeSharingUtils.replaceMetatags(content, replacementMap);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @GET
    @Path("/events/{eventId}/regattas/{regattaId}")
    @Produces("text/html")
    public String getSharedRegatta(@PathParam("eventId") String eventId, @PathParam("regattaId") String regattaId,
            @HeaderParam("user-agent") String userAgent) throws UnsupportedEncodingException {
        RacingEventService eventService = getService();
        SecurityService securityService = getSecurityService();
        UUID uuid = UUID.fromString(eventId);
        final Event event = eventService.getEvent(uuid);
        if (event != null) {
            securityService.checkCurrentUserReadPermission(event);
            final String description = HomeSharingUtils.findDescription(event);
            final String imageUrl = HomeSharingUtils.findTeaserImageUrl(event);
            TokenizedHomePlaceUrlBuilder baseUrl = new TokenizedHomePlaceUrlBuilder(uriInfo);
            String placeUrl = baseUrl.getBaseUrl().toString();
            String title;
            String decodedRegattaId;
            try {
                decodedRegattaId = URLDecoder.decode(regattaId, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException();
            }
            Leaderboard leaderboardByName = eventService.getLeaderboardByName(decodedRegattaId);
            if (leaderboardByName != null) {
                securityService.checkCurrentUserReadPermission(leaderboardByName);
                title = HomeSharingUtils.findTitle(leaderboardByName);
                placeUrl = baseUrl.asRegattaPlaceLink(eventId, decodedRegattaId);
            } else {
                Regatta regattaByName = eventService.getRegattaByName(decodedRegattaId);
                if (regattaByName != null) {
                    securityService.checkCurrentUserReadPermission(regattaByName);
                    title = HomeSharingUtils.findTitle(leaderboardByName);
                    placeUrl = baseUrl.asRegattaPlaceLink(eventId, decodedRegattaId);
                } else {
                    throw new IllegalArgumentException();
                }
            }
            final Map<String, String> replacementMap = HomeSharingUtils.createReplacementMap(title,
                    description, imageUrl, placeUrl, userAgent);
            final String content = HomeSharingUtils.loadSharingHTML(this.getClass().getClassLoader(), uriInfo);
            return HomeSharingUtils.replaceMetatags(content, replacementMap);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @GET
    @Path("/series/{seriesId}")
    @Produces("text/html")
    public String getSharedSeries(@PathParam("seriesId") String seriesId, @HeaderParam("user-agent") String userAgent) {
        RacingEventService eventService = getService();
        SecurityService securityService = getSecurityService();
        final UUID leaderboardGroupId = UUID.fromString(seriesId);
        final LeaderboardGroup leaderboardGroup = eventService.getLeaderboardGroupByID(leaderboardGroupId);
        if (leaderboardGroup != null) {
            securityService.checkCurrentUserReadPermission(leaderboardGroup);
            final String description = HomeSharingUtils.findDescription(leaderboardGroup);
            final String imageUrl = HomeSharingUtils.findTeaserImageUrl(leaderboardGroup, eventService);
            final String title = HomeSharingUtils.findTitle(leaderboardGroup);
            final String placeUrl = new TokenizedHomePlaceUrlBuilder(uriInfo).asSeriesPlaceLink(seriesId);
            final Map<String, String> replacementMap = HomeSharingUtils.createReplacementMap(title,
                    description, imageUrl, placeUrl, userAgent);
            final String content = HomeSharingUtils.loadSharingHTML(this.getClass().getClassLoader(), uriInfo);
            return HomeSharingUtils.replaceMetatags(content, replacementMap);
        }else {
            throw new IllegalArgumentException();
        }
    }

}