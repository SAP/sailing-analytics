package com.sap.sailing.server.gateway.jaxrs.api;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.impl.EventBaseImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.NotFoundException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RegattaCreationParametersDTO;
import com.sap.sailing.domain.common.dto.SeriesCreationParametersDTO;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.ResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.geocoding.ReverseGeocoder;
import com.sap.sailing.server.gateway.jaxrs.exceptions.ExceptionManager;
import com.sap.sailing.server.gateway.serialization.impl.CourseAreaJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.EventBaseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.EventRaceStatesSerializer;
import com.sap.sailing.server.gateway.serialization.impl.LeaderboardGroupBaseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.TrackingConnectorInfoJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.VenueJsonSerializer;
import com.sap.sailing.server.hierarchy.SailingHierarchyOwnershipUpdater;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.AddColumnToSeries;
import com.sap.sailing.server.operationaltransformation.AddCourseAreas;
import com.sap.sailing.server.operationaltransformation.AddSpecificRegatta;
import com.sap.sailing.server.operationaltransformation.CreateEvent;
import com.sap.sailing.server.operationaltransformation.CreateLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.CreateRegattaLeaderboard;
import com.sap.sailing.server.operationaltransformation.RemoveEvent;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboard;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.RemoveRegatta;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.UpdateSeries;
import com.sap.sailing.server.security.SailingViewerRole;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.InvalidDateException;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.impl.WildcardPermissionEncoder;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonSerializer;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;
import com.sap.sse.shared.util.impl.UUIDHelper;

@Path(EventsResource.V1_EVENTS)
public class EventsResource extends AbstractSailingServerResource {
    public static final String V1_EVENTS = "/v1/events";
    private static final Logger logger = Logger.getLogger(EventsResource.class.getName());
    private static final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static enum ErrorCodes {
        NO_VENUE(1), NO_BOAT_CLASS(2), LEADERBOARD_GROUP_ALREADY_EXISTS(3), REGATTA_ALREADY_EXISTS(4), LEADERBOARD_ALREADY_EXISTS(5);

        private ErrorCodes(int errorCode) {
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }

        private final int errorCode;
    }

    public EventsResource() {
    }


    /**
     * Method to delete a specified {@link Event}.
     *
     * @param eventId - The UUID of the event to delete
     * @param withLeaderboardGroups - Boolean whether to delete all associated {@link LeaderboardGroup}s or not. Doing so will also delete the overall {@link Leaderboard}s of each group.
     * @param withLeaderboards - Boolean whether to delete all associated {@link Leaderboard}s.
     * @param withRegattas - Boolean whether to delete all associated {@link Regatta}s. Doing so will also delete all their {@link RegattaLeaderboard}s and {@link TrackedRace}s.
     * @return A 200 response, if the delete was successful or a 404 response, if the eventId was not found.
     * @throws ParseException
    */
    @DELETE
    @Path("/{eventId}/")
    public Response delete(@PathParam("eventId") String eventId,
            @QueryParam("withLeaderboardGroups") Boolean withLeaderboardGroups,
            @QueryParam("withLeaderboards") Boolean withLeaderboards, @QueryParam("withRegattas") Boolean withRegattas)
            throws ParseException {
        final Serializable eventUUID = UUIDHelper.tryUuidConversion(eventId);
        final RacingEventService racingEventService = getService();
        final Event event = racingEventService.getEvent(eventUUID);
        if (event != null) {
            final boolean deleteLeaderboardGroups = withLeaderboardGroups == Boolean.TRUE;
            final boolean deleteLeaderboards = withLeaderboards == Boolean.TRUE;
            final boolean deleteRegattas = withRegattas == Boolean.TRUE;
            final SecurityService securityService = getSecurityService();
            securityService.checkPermissionAndDeleteOwnershipForObjectRemoval(event, () -> {
                racingEventService.apply(new RemoveEvent(event.getId()));
                if (deleteLeaderboards || deleteLeaderboardGroups || deleteRegattas) {
                    for (final LeaderboardGroup group : event.getLeaderboardGroups()) {
                        if (deleteLeaderboards || deleteRegattas) {
                            for (final Leaderboard leaderboard : group.getLeaderboards()) {
                                if (deleteRegattas && leaderboard instanceof RegattaLeaderboard) {
                                    deleteReferencedRegatta(securityService, racingEventService, leaderboard);
                                }
                                if (deleteLeaderboards) {
                                    securityService.checkPermissionAndDeleteOwnershipForObjectRemoval(leaderboard,
                                            () -> {
                                                racingEventService.apply(new RemoveLeaderboard(leaderboard.getName()));
                                            });
                                }
                            }
                        }
                        if (deleteLeaderboardGroups) {
                            securityService.checkPermissionAndDeleteOwnershipForObjectRemoval(group, () -> {
                                if (!deleteLeaderboards) {
                                    deleteReferencedOverallLeaderboard(deleteRegattas, securityService,
                                            racingEventService, group);
                                }
                                racingEventService.apply(new RemoveLeaderboardGroup(group.getId()));
                            });
                        }
                    }
                }
            });
        } else {
            return getBadEventErrorResponse(eventId);
        }
        return Response.ok().build();
    }

    private void deleteReferencedOverallLeaderboard(final boolean deleteRegattas, final SecurityService securityService,
            final RacingEventService racingEventService, final LeaderboardGroup group) {
        final Leaderboard overallLeaderboard = group.getOverallLeaderboard();
        if (overallLeaderboard != null) {
            if (deleteRegattas && overallLeaderboard instanceof RegattaLeaderboard) {
                deleteReferencedRegatta(securityService, racingEventService, overallLeaderboard);
            }
            securityService.checkPermissionAndDeleteOwnershipForObjectRemoval(overallLeaderboard, () -> {
                racingEventService.apply(new RemoveLeaderboard(overallLeaderboard.getName()));
            });
        }
    }

    private void deleteReferencedRegatta(final SecurityService securityService,
            final RacingEventService racingEventService, final Leaderboard leaderboard) {
        final RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
        final Regatta regatta = regattaLeaderboard.getRegatta();
        securityService.checkPermissionAndDeleteOwnershipForObjectRemoval(regatta, () -> {
            racingEventService.apply(new RemoveRegatta(regatta.getRegattaIdentifier()));
        });
    }

    @POST
    @Path("/{eventId}/migrate")
    public Response migrateOwnershipForEvent(@PathParam("eventId") UUID eventId,
            @QueryParam("createNewGroup") Boolean createNewGroup,
            @QueryParam("existingGroupId") UUID existingGroupIdOrNull, @QueryParam("newGroupName") String newGroupName,
            @QueryParam("migrateCompetitors") Boolean migrateCompetitors,
            @QueryParam("migrateBoats") Boolean migrateBoats,
            @QueryParam("copyMembersAndRoles") Boolean copyMembersAndRoles)
            throws ParseException, JsonDeserializationException {
        Event event = getService().getEvent(eventId);
        SailingHierarchyOwnershipUpdater updater = SailingHierarchyOwnershipUpdater.createOwnershipUpdater(
                createNewGroup, existingGroupIdOrNull, newGroupName, migrateCompetitors, migrateBoats,
                copyMembersAndRoles == null ? true : copyMembersAndRoles, getService());
        updater.updateGroupOwnershipForEventHierarchy(event);
        return Response.ok().build();
    }

    @POST
    @Path("/createEvent")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response createEvent(
            @Context UriInfo uriInfo,
            @FormParam("eventName") String eventNameParam,
            @FormParam("eventdescription") String eventDescriptionParam,
            @FormParam("startdate") String startDateParam,
            @FormParam("startdateasmillis") Long startDateAsMillis,
            @FormParam("enddate") String endDateParam,
            @FormParam("enddateasmillis") Long endDateAsMillis,
            @FormParam("venuename") String venueNameParam, // takes precedence over lat/lng used for reverse geo-coding
            @FormParam("venuelat") String venueLat,
            @FormParam("venuelng") String venueLng,
            @FormParam("ispublic") String isPublicParam,
            @FormParam("officialwebsiteurl") String officialWebsiteURLParam,
            @FormParam("baseurl") String baseURLParam,
            @FormParam("leaderboardgroupids") List<String> leaderboardGroupIdsListParam,
            @FormParam("createleaderboardgroup") String createLeaderboardGroupParam,
            @FormParam("createregatta") String createRegattaParam,
            @FormParam("boatclassname") String boatClassNameParam,
            @FormParam("numberofraces") String numberOfRacesParam,
            @FormParam("canBoatsOfCompetitorsChangePerRace") boolean canBoatsOfCompetitorsChangePerRace,
            @FormParam("competitorRegistrationType") String competitorRegistrationType,
            @FormParam("secret") String competitorRegistrationSecret,
            @FormParam("rankingMetric") String rankingMetricParam,
            @FormParam("scoringScheme") String scoringSchemeParam,
            @FormParam("leaderboardDiscardThresholds") List<Integer> leaderboardDiscardThresholdsParam) throws ParseException, NotFoundException,
            NumberFormatException, IOException, org.json.simple.parser.ParseException, InvalidDateException {
        final Response response;
        if (venueNameParam == null && (venueLat == null || venueLng == null)) {
            response = createErrorResponse(Status.PRECONDITION_FAILED, "No venue specified; provide either venuename or venuelat/venuelng", ErrorCodes.NO_VENUE);
        } else {
            Triple<Event, LeaderboardGroup, RegattaLeaderboard> eventAndLeaderboardGroupAndLeaderboard = validateAndCreateEvent(uriInfo, eventNameParam, eventDescriptionParam, startDateParam,
                    startDateAsMillis, endDateParam, endDateAsMillis, venueNameParam,
                    /* venue latitude */ venueLat, /* venue longitude */ venueLng, isPublicParam, officialWebsiteURLParam,
                    baseURLParam, leaderboardGroupIdsListParam, createLeaderboardGroupParam, createRegattaParam,
                    boatClassNameParam, numberOfRacesParam, canBoatsOfCompetitorsChangePerRace, competitorRegistrationType, competitorRegistrationSecret,
                    rankingMetricParam, scoringSchemeParam, leaderboardDiscardThresholdsParam);
            final JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("eventid", eventAndLeaderboardGroupAndLeaderboard.getA().getId().toString());
            jsonResponse.put("eventname", eventAndLeaderboardGroupAndLeaderboard.getA().getName());
            jsonResponse.put("eventstartdate", eventAndLeaderboardGroupAndLeaderboard.getA().getStartDate()==null?null:eventAndLeaderboardGroupAndLeaderboard.getA().getStartDate().asMillis());
            jsonResponse.put("eventenddate", eventAndLeaderboardGroupAndLeaderboard.getA().getEndDate()==null?null:eventAndLeaderboardGroupAndLeaderboard.getA().getEndDate().asMillis());
            if (eventAndLeaderboardGroupAndLeaderboard.getB() != null) {
                jsonResponse.put("leaderboardgroupid", eventAndLeaderboardGroupAndLeaderboard.getB().getId().toString());
            }
            if (eventAndLeaderboardGroupAndLeaderboard.getC() != null) {
                jsonResponse.put("regatta", eventAndLeaderboardGroupAndLeaderboard.getC().getRegatta().getName());
                jsonResponse.put("registrationSecret",
                        eventAndLeaderboardGroupAndLeaderboard.getC().getRegatta().getRegistrationLinkSecret());
                jsonResponse.put("leaderboard", eventAndLeaderboardGroupAndLeaderboard.getC().getName());
            }
            response = Response.ok(streamingOutput(jsonResponse)).build();
        }
        return response;
    }

    private Response createErrorResponse(Status responseStatus, String message, ErrorCodes errorCode) {
        final JSONObject responseBody = new JSONObject();
        responseBody.put("message", message);
        responseBody.put("errorCodeName", errorCode.name());
        responseBody.put("errorCode", errorCode.getErrorCode());
        return Response.status(responseStatus).entity(responseBody.toString()).build();
    }

    @PUT
    @Path("/{eventId}/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response updateEvent(
            @Context UriInfo uriInfo,
            @PathParam("eventId") String eventId,
            @FormParam("eventName") String eventNameParam,
            @FormParam("eventdescription") String eventDescriptionParam,
            @FormParam("startdate") String startDateParam,
            @FormParam("startdateasmillis") Long startDateAsMillis,
            @FormParam("enddate") String endDateParam,
            @FormParam("enddateasmillis") Long endDateAsMillis,
            @FormParam("venuename") String venueNameParam, // takes precedence over lat/lng used for reverse geo-coding
            @FormParam("venuelat") String venueLat,
            @FormParam("venuelng") String venueLng,
            @FormParam("ispublic") String isPublicParam,
            @FormParam("officialwebsiteurl") String officialWebsiteURLParam,
            @FormParam("baseurl") String baseURLParam,
            @FormParam("leaderboardgroupids") List<String> leaderboardGroupIdsListParam,
            @FormParam("createleaderboardgroup") String createLeaderboardGroupParam,
            @FormParam("createregatta") String createRegattaParam,
            @FormParam("boatclassname") String boatClassNameParam,
            @FormParam("numberofraces") String numberOfRacesParam) throws ParseException, NotFoundException,
            NumberFormatException, IOException, org.json.simple.parser.ParseException, InvalidDateException {
        final Response response;
        UUID id;
        try {
            id = toUUID(eventId);
        } catch (IllegalArgumentException e) {
            return getBadEventErrorResponse(eventId);
        }
        Event event = getService().getEvent(id);
        SecurityUtils.getSubject()
                .checkPermission(SecuredDomainType.EVENT.getStringPermissionForObject(DefaultActions.UPDATE, event));
        if (event == null) {
            response = getBadEventErrorResponse(eventId);
        } else {
            final String eventName, eventDescription, venueName;
            final TimePoint startDate, endDate;
            final URL officialWebsiteURL, baseURL;
            final boolean isPublic;
            final Iterable<UUID> leaderboardGroupIds;
            eventName = eventNameParam != null ? eventNameParam : event.getName();
            eventDescription = eventDescriptionParam != null ? eventDescriptionParam : event.getDescription();
            if (startDateParam != null || startDateAsMillis != null) {
                startDate = parseTimePoint(startDateParam, startDateAsMillis, null);
            } else {
                startDate = event.getStartDate();
            }
            if (endDateParam != null || endDateAsMillis != null) {
                endDate = parseTimePoint(endDateParam, endDateAsMillis, null);
            } else {
                endDate = event.getEndDate();
            }
            venueName = venueNameParam != null ? venueNameParam : (venueLat != null && venueLng != null) ? getDefaultVenueName(venueLat, venueLng) : event.getVenue().getName();
            isPublic = isPublicParam != null ? Boolean.valueOf(isPublicParam) : event.isPublic();
            if (leaderboardGroupIdsListParam.isEmpty()) {
                // nothing has been provided, not even an empty value; leave unchanged:
                leaderboardGroupIds = Util.map(event.getLeaderboardGroups(), lg->lg.getId());
            } else if (leaderboardGroupIdsListParam.size() == 1 && leaderboardGroupIdsListParam.get(0).isEmpty()) {
                // one empty occurrence of the sort "leaderboardgroupids=" means clear the value
                leaderboardGroupIds = Collections.emptyList();
            } else {
                leaderboardGroupIds = toUUIDList(leaderboardGroupIdsListParam);
            }
            officialWebsiteURL = officialWebsiteURLParam != null ? new URL(officialWebsiteURLParam) : event.getOfficialWebsiteURL();
            baseURL = baseURLParam != null ? new URL(baseURLParam) : event.getBaseURL();
            getService().updateEvent(id, eventName, eventDescription, startDate, endDate, venueName, isPublic,
                    leaderboardGroupIds, officialWebsiteURL, baseURL, event.getSailorsInfoWebsiteURLs(),
                    event.getImages(), event.getVideos(), event.getWindFinderReviewedSpotsCollectionIds());
            response = Response.ok().build();
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getEvents(@QueryParam("showNonPublic") String showNonPublic, @QueryParam("include") @DefaultValue("false") Boolean include,
            @QueryParam("id") List<UUID> eventIds) {
        JsonSerializer<EventBase> eventSerializer = new EventBaseJsonSerializer(
                new VenueJsonSerializer(new CourseAreaJsonSerializer()), new LeaderboardGroupBaseJsonSerializer(),
                new TrackingConnectorInfoJsonSerializer());
        JSONArray result = new JSONArray();
        Iterable<Event> events = getService().getEventsSelectively(include, eventIds);
        for (Event event : events) {
            if (getSecurityService().hasCurrentUserReadPermission(event)
                    && ((showNonPublic != null && Boolean.valueOf(showNonPublic)) || event.isPublic())) {
                result.add(eventSerializer.serialize(event));
            }
        }
        return Response.ok(streamingOutput(result)).build();
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{eventId}")
    public Response getEvent(@PathParam("eventId") String eventId, @QueryParam("secret") String secret) {
        Response response;
        UUID eventUuid;
        try {
            eventUuid = toUUID(eventId);
        } catch (IllegalArgumentException e) {
            return getBadEventErrorResponse(eventId);
        }
        Event event = getService().getEvent(eventUuid);
        if (event == null) {
            response = getBadEventErrorResponse(eventId);
        } else {
            boolean skip = false;
            for (LeaderboardGroup lg : event.getLeaderboardGroups()) {
                for (Leaderboard l : lg.getLeaderboards()) {
                    if (getService().skipChecksDueToCorrectSecret(l.getName(), secret)) {
                        skip = true;
                    }
                }
            }
            if (!skip) {
                getSecurityService().checkCurrentUserReadPermission(event);
            }
            JsonSerializer<EventBase> eventSerializer = new EventBaseJsonSerializer(
                    new VenueJsonSerializer(new CourseAreaJsonSerializer()), new LeaderboardGroupBaseJsonSerializer(),
                    new TrackingConnectorInfoJsonSerializer());
            JSONObject eventJson = eventSerializer.serialize(event);
            response = Response.ok(streamingOutput(eventJson)).build();
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{eventId}/racestates")
    public Response getRaceStates(@PathParam("eventId") String eventId,
            @QueryParam("filterByLeaderboard") String filterByLeaderboard,
            @QueryParam("filterByCourseArea") String filterByCourseArea,
            @QueryParam("filterByDayOffset") String filterByDayOffset,
            @QueryParam("clientTimeZoneOffsetInMinutes") Integer clientTimeZoneOffsetInMinutes) {
        Response response;
        UUID eventUuid;
        try {
            eventUuid = toUUID(eventId);
        } catch (IllegalArgumentException e) {
            return getBadEventErrorResponse(eventId);
        }
        Event event = getService().getEvent(eventUuid);
        if (event == null) {
            response = getBadEventErrorResponse(eventId);
        } else {
            if (getSecurityService().hasCurrentUserReadPermission(event)) {
                final Duration clientTimeZoneOffset;
                if (filterByDayOffset != null) {
                    if (clientTimeZoneOffsetInMinutes != null) {
                        clientTimeZoneOffset = new MillisecondsDurationImpl(1000 * 60 * clientTimeZoneOffsetInMinutes);
                    } else {
                        clientTimeZoneOffset = Duration.NULL;
                    }
                } else {
                    clientTimeZoneOffset = null;
                }
                EventRaceStatesSerializer eventRaceStatesSerializer = new EventRaceStatesSerializer(filterByCourseArea,
                        filterByLeaderboard, filterByDayOffset, clientTimeZoneOffset, getService());
                JSONObject raceStatesJson = eventRaceStatesSerializer.serialize(
                        new Pair<Event, Iterable<Leaderboard>>(event, getService().getLeaderboards().values()));
                response = Response.ok(streamingOutput(raceStatesJson)).build();
            } else {
                response = Response.status(Status.FORBIDDEN).build();
            }
        }
        return response;
    }

    private RegattaLeaderboard validateAndCreateRegatta(String regattaNameParam, String boatClassNameParam,
            String scoringSchemeParam, UUID courseAreaId, String buoyZoneRadiusInHullLengthsParam,
            String useStartTimeInterferenceParam, String controlTrackingFromStartAndFinishTimesParam,
            String autoRestartTrackingUponCompetitorSetChangeParam, String rankingMetricParam, List<Integer> leaderboardDiscardThresholdsParam,
            String numberOfRacesParam, boolean canBoatsOfCompetitorsChangePerRace,
            CompetitorRegistrationType competitorRegistrationType, String competitorRegistrationSecret) throws ParseException, NotFoundException {
        boolean controlTrackingFromStartAndFinishTimes = controlTrackingFromStartAndFinishTimesParam == null ? false
                : Boolean.parseBoolean(controlTrackingFromStartAndFinishTimesParam);
        boolean autoRestartTrackingUponCompetitorSetChange = autoRestartTrackingUponCompetitorSetChangeParam == null ? false
                : Boolean.parseBoolean(autoRestartTrackingUponCompetitorSetChangeParam);
        boolean useStartTimeInterference = useStartTimeInterferenceParam == null ? true
                : Boolean.parseBoolean(useStartTimeInterferenceParam);
        double buoyZoneRadiusInHullLengths = buoyZoneRadiusInHullLengthsParam == null ? 3.0
                : Double.parseDouble(buoyZoneRadiusInHullLengthsParam);
        if (regattaNameParam == null) {
            throw new IllegalArgumentException(ExceptionManager.parameterRequiredMsg("regattaName"));
        }
        if (competitorRegistrationSecret == null) {
            throw new IllegalArgumentException(ExceptionManager.parameterRequiredMsg("competitorRegistrationSecret"));
        }
        if (boatClassNameParam == null) {
            throw new IllegalArgumentException(ExceptionManager.parameterRequiredMsg("boatClassName"));
        }
        String regattaName = regattaNameParam;
        String boatClassName = boatClassNameParam;
        ScoringScheme scoringScheme = scoringSchemeParam == null ? createScoringScheme("LOW_POINT")
                : createScoringScheme(scoringSchemeParam);
        RankingMetrics rankingMetric = rankingMetricParam == null ? createRankingMetric("ONE_DESIGN")
                : createRankingMetric(rankingMetricParam);
        int[] leaderboardDiscardThresholds = leaderboardDiscardThresholdsParam == null ? new int[0]
                : leaderboardDiscardThresholdsParam.stream().mapToInt(i -> i).toArray();
        int numberOfRaces = numberOfRacesParam == null ? 0 : Integer.parseInt(numberOfRacesParam);
        RegattaCreationParametersDTO regattaCreationParametersDTO = new RegattaCreationParametersDTO(
                createDefaultSeriesCreationParameters(regattaName, numberOfRaces));
        UUID regattaId = UUID.randomUUID();
        Regatta regatta = getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.REGATTA, Regatta.getTypeRelativeObjectIdentifier(regattaName),
                regattaName, new Callable<Regatta>() {
                    @Override
                    public Regatta call() throws Exception {
                        return getService().apply(new AddSpecificRegatta(regattaName, boatClassName,
                                canBoatsOfCompetitorsChangePerRace, competitorRegistrationType, competitorRegistrationSecret, null, null, regattaId, regattaCreationParametersDTO,
                                /* isPersistent */ true, scoringScheme,
                                courseAreaId==null?Collections.emptySet():Collections.singleton(courseAreaId), buoyZoneRadiusInHullLengths,
                                useStartTimeInterference, controlTrackingFromStartAndFinishTimes, autoRestartTrackingUponCompetitorSetChange, rankingMetric));
                    }
                });
        final RegattaLeaderboard leaderboard = addLeaderboard(regattaName, leaderboardDiscardThresholds);
        SeriesCreationParametersDTO defaultSeries = regattaCreationParametersDTO.getSeriesCreationParameters()
                .get("Default");
        addRaceColumns(regattaName, "Default", numberOfRaces);
        updateSeries(regatta, defaultSeries);
        return leaderboard;
    }

    /**
     * @param canBoatsOfCompetitorsChangePerRace
     * @return the event created as first component; if a leaderboard group was to be created, the leaderboard group
     *         created as the second component or {@code null} otherwise; the regatta leaderboard as the third component
     *         in case a regatta was to be created, or {@code null} otherwise
     */
    private Util.Triple<Event, LeaderboardGroup, RegattaLeaderboard> validateAndCreateEvent(UriInfo uriInfo,
            String eventNameParam, String eventDescriptionParam, String startDateParam, Long startDateAsMillis,
            String endDateParam, Long endDateAsMillis, String venueNameParam, String venueLat, String venueLng,
            String isPublicParam, String officialWebsiteURLParam, String baseURLParam,
            List<String> leaderboardGroupIdsListParam, String createLeaderboardGroupParam, String createRegattaParam,
            String boatClassName, String numberOfRacesParam, boolean canBoatsOfCompetitorsChangePerRace,
            String competitorRegistrationTypeString, String competitorRegistrationSecret, String rankingMetric,
            String scoringScheme, List<Integer> leaderboardDiscardThresholdsParam)
            throws ParseException, NotFoundException, NumberFormatException, IOException,
            org.json.simple.parser.ParseException, InvalidDateException {
        boolean isPublic = isPublicParam == null ? false : Boolean.parseBoolean(isPublicParam);
        boolean createRegatta = createRegattaParam == null ? true : Boolean.parseBoolean(createRegattaParam);
        boolean createLeaderboardGroup = createLeaderboardGroupParam == null ? true : Boolean.parseBoolean(createLeaderboardGroupParam);
        String eventName = eventNameParam == null ? getDefaultEventName() : eventNameParam;
        String venueName = venueNameParam == null ? getDefaultVenueName(venueLat, venueLng) : venueNameParam;
        String leaderboardGroupName = eventName;
        String regattaAndLeaderboardName = eventName;
        if (createRegatta && boatClassName == null) {
            throw new WebApplicationException(createErrorResponse(Status.BAD_REQUEST, ExceptionManager.parameterRequiredMsg("boatClassName"),
                    ErrorCodes.NO_BOAT_CLASS));
        }
        if (createLeaderboardGroup && getService().getLeaderboardGroupByName(leaderboardGroupName) != null) {
            throw new WebApplicationException(createErrorResponse(Status.BAD_REQUEST,
                    ExceptionManager.objectAlreadyExists("leaderboard group", leaderboardGroupName), ErrorCodes.LEADERBOARD_GROUP_ALREADY_EXISTS));
        }
        if (createRegatta) {
            if (getService().getRegattaByName(regattaAndLeaderboardName) != null) {
                throw new WebApplicationException(createErrorResponse(Status.BAD_REQUEST,
                        ExceptionManager.objectAlreadyExists("regatta", regattaAndLeaderboardName), ErrorCodes.REGATTA_ALREADY_EXISTS));
            }
            if (getService().getLeaderboardByName(regattaAndLeaderboardName) != null) {
                throw new WebApplicationException(createErrorResponse(Status.BAD_REQUEST,
                        ExceptionManager.objectAlreadyExists("leaderboard", regattaAndLeaderboardName), ErrorCodes.LEADERBOARD_ALREADY_EXISTS));
            }
        }
        String eventDescription = eventDescriptionParam == null ? eventName : eventDescriptionParam;
        final TimePoint startDate = parseTimePoint(startDateParam, startDateAsMillis, now());
        final TimePoint endDate = parseTimePoint(endDateParam, endDateAsMillis, new MillisecondsTimePoint(addOneWeek(startDate.asDate())));
        URL officialWebsiteURL = officialWebsiteURLParam == null ? null : toURL(officialWebsiteURLParam);
        final URI baseUri = uriInfo.getBaseUri();
        final int port = baseUri.getPort();
        // guess the protocol; when behind an SSL-offloading reverse proxy or load balancer, all we'll see is a regular HTTP
        // request; yet, we'd likely want the client to make requests using HTTPS, unless we see a dedicated port in the request
        // that is not the HTTPS default port 443:
        final String scheme = (port >= 0 && port != 443) ? "http" : "https";
        URL baseURL = baseURLParam == null ? new URL(scheme+":"+baseUri.getSchemeSpecificPart().
                substring(0, baseUri.getSchemeSpecificPart().length()-baseUri.getPath().length())) : toURL(baseURLParam);
        List<UUID> leaderboardGroupIds = leaderboardGroupIdsListParam == null ? new ArrayList<UUID>() : toUUIDList(leaderboardGroupIdsListParam);
        UUID eventId = UUID.randomUUID();
        // ignoring sailorsInfoWebsiteURLs, images, videos
        Map<Locale, URL> sailorsInfoWebsiteURLs = new HashMap<Locale,URL>();
        Iterable<ImageDescriptor> images = Collections.<ImageDescriptor> emptyList();
        Iterable<VideoDescriptor> videos = Collections.<VideoDescriptor> emptyList();

        final CompetitorRegistrationType competitorRegistrationType;
        try {
            competitorRegistrationType = CompetitorRegistrationType.valueOfOrDefault(competitorRegistrationTypeString, /* failForUnknown */ true);
        } catch (IllegalArgumentException iae) {
            throw new IllegalArgumentException(ExceptionManager.incorrectParameterValue(competitorRegistrationTypeString,
                    StringUtils.join(CompetitorRegistrationType.values(), ", ")));
        }
        Callable<Util.Triple<Event, LeaderboardGroup, RegattaLeaderboard>> doCreationAction = new Callable<Util.Triple<Event, LeaderboardGroup, RegattaLeaderboard>>() {
            @Override
            public Util.Triple<Event, LeaderboardGroup, RegattaLeaderboard> call() throws Exception {
                logger.info("User "+SecurityUtils.getSubject().getPrincipal()+" is trying to create event "+eventName);
                Event event = getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                        SecuredDomainType.EVENT, EventBaseImpl.getTypeRelativeObjectIdentifier(eventId),
                        eventName, ()->getService().apply(new CreateEvent(eventName, eventDescription, startDate, endDate, venueName, isPublic, eventId,
                                        officialWebsiteURL, baseURL, sailorsInfoWebsiteURLs, images, videos, leaderboardGroupIds)));
                CourseArea courseArea = addCourseArea(event, "Default");
                final LeaderboardGroup leaderboardGroup;
                if (createLeaderboardGroup) {
                    leaderboardGroup = validateAndAddLeaderboardGroup(event.getId(), leaderboardGroupName, event.getDescription(), /* leaderboardGroupDisplayNameParam */ null,
                            /* displayGroupsInReverseOrderParam */ false, /* leaderboardNamesParam */ null,
                            /* overallLeaderboardDiscardThresholdsParam */ null, /* overallLeaderboardScoringSchemeTypeParam */ null);
                } else {
                    leaderboardGroup = null;
                }
                final RegattaLeaderboard leaderboard;
                if (createRegatta) {
                    String localCompetitorRegistrationSecret = competitorRegistrationSecret;
                    if (localCompetitorRegistrationSecret == null) {
                        localCompetitorRegistrationSecret = UUID.randomUUID().toString();
                        logger.warning(
                                "Got request that created a new Regatta without a registrationSecret, generated a new one");
                    }
                    leaderboard = validateAndCreateRegatta(regattaAndLeaderboardName, boatClassName,
                            /* scoringSchemeParam */ scoringScheme, courseArea.getId(), /* buoyZoneRadiusInHullLengthsParam */ null,
                            /* useStartTimeInterferenceParam */ null, /* controlTrackingFromStartAndFinishTimesParam */ null,
                            /* autoRestartTrackingUponCompetitorSetChangeParam */ null, /* rankingMetricParam */ rankingMetric,
                            /* leaderboardDiscardThresholdsParam */ leaderboardDiscardThresholdsParam, numberOfRacesParam, canBoatsOfCompetitorsChangePerRace,
                            competitorRegistrationType, localCompetitorRegistrationSecret);
                    if (leaderboardGroup != null) {
                        // no security checks for any overall leaderboard required here as we don't manipulate it
                        // through this service. leaderboardGroup.getOverallLeaderboard() == null will be true, due to
                        // overallLeaderboardScoringSchemeTypeParam being null in validateAndAddLeaderboardGroup call.
                        getService().apply(new UpdateLeaderboardGroup(leaderboardGroup.getId(),
                                leaderboardGroup.getName(),
                                leaderboardGroup.getDescription(), leaderboardGroup.getDisplayName(),
                                Collections.singletonList(leaderboard.getName()),
                                leaderboardGroup.getOverallLeaderboard() == null ? null
                                        : ((ThresholdBasedResultDiscardingRule) leaderboardGroup.getOverallLeaderboard()
                                                .getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces(),
                                leaderboardGroup.getOverallLeaderboard() == null ? null
                                        : leaderboardGroup.getOverallLeaderboard().getScoringScheme().getType()));
                    }
                } else {
                    leaderboard = null;
                }
                return new Util.Triple<>(event, leaderboardGroup, leaderboard);
            }
        };

        try {
            if (competitorRegistrationType == CompetitorRegistrationType.OPEN_UNMODERATED) {
                UUID newTenantId = UUID.randomUUID();
                String escapedName = WildcardPermissionEncoder.encode(eventName) + "-owner";
                UserGroup ownerGroup = getSecurityService().createUserGroup(newTenantId, escapedName);
                getSecurityService().setDefaultOwnershipIfNotSet(ownerGroup.getIdentifier());
                RoleDefinition roleDef = getSecurityService()
                        .getRoleDefinition(SailingViewerRole.getInstance().getId());
                getSecurityService().putRoleDefinitionToUserGroup(ownerGroup, roleDef, true);
                getSecurityService().addUserToUserGroup(ownerGroup, getCurrentUser());
                return getSecurityService().doWithTemporaryDefaultTenant(ownerGroup, doCreationAction);
            } else {
                return doCreationAction.call();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LeaderboardGroup validateAndAddLeaderboardGroup(UUID eventId, String leaderboardGroupName,
            String leaderboardGroupDescription, String leaderboardGroupDisplayName,
            boolean displayGroupsInReverseOrder, List<String> leaderboardNamesParam,
            List<Integer> overallLeaderboardDiscardThresholdsParam, String overallLeaderboardScoringSchemeTypeParam)
            throws NotFoundException {
        final UUID leaderboardGroupId = UUID.randomUUID();
        final ScoringSchemeType overallLeaderboardScoringSchemeType = overallLeaderboardScoringSchemeTypeParam == null
                ? null : getScoringSchemeType(overallLeaderboardScoringSchemeTypeParam);
        final Callable<LeaderboardGroup> createLeaderboardGroup = ()->{
            int[] overallLeaderboardDiscardThresholds = overallLeaderboardDiscardThresholdsParam == null ? new int[0] : overallLeaderboardDiscardThresholdsParam.stream().mapToInt(i -> i).toArray();
            List<String> leaderboardNames = leaderboardNamesParam == null ? new ArrayList<String>() : leaderboardNamesParam;
            return getService().apply(new CreateLeaderboardGroup(
                    leaderboardGroupId, leaderboardGroupName, leaderboardGroupDescription, leaderboardGroupDisplayName, displayGroupsInReverseOrder,
                    leaderboardNames, overallLeaderboardDiscardThresholds, overallLeaderboardScoringSchemeType));
        };
        final LeaderboardGroup leaderboardGroup;
        // if an overall leaderboard is requested, establish ownership and check create permission; roll back if not possible;
        // otherwise, try to establish the leaderboard group's ownership; if that fails with an AuthorizationException,
        // the overall leaderboard's ownership is removed again, too.
        if (overallLeaderboardScoringSchemeType != null) {
            final String overallLeaderboardName = LeaderboardGroupMetaLeaderboard.getOverallLeaderboardName(leaderboardGroupName);
            leaderboardGroup = getSecurityService()
                .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.LEADERBOARD,
                    new TypeRelativeObjectIdentifier(overallLeaderboardName), overallLeaderboardName,
                    ()->createLeaderboardGroup.call());
        } else {
            leaderboardGroup = getSecurityService()
                .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.LEADERBOARD_GROUP,
                        LeaderboardGroupImpl.getTypeRelativeObjectIdentifier(leaderboardGroupId), leaderboardGroupName,
                        createLeaderboardGroup);
        }
        updateEvent(getEvent(eventId), leaderboardGroup);
        return leaderboardGroup;
    }

    private String getDefaultEventName() {
        final String username;
        username = getCurrentUser().getName();
        synchronized (dateTimeFormat) {
        return "Session "+username+" "+dateTimeFormat.format(new Date());
    }
    }

    private User getCurrentUser() {
        return getSecurityService().getCurrentUser();
    }

    private String getDefaultVenueName(String lat, String lng) throws NumberFormatException, IOException, org.json.simple.parser.ParseException {
        return ReverseGeocoder.INSTANCE.getPlacemarkNearest(new DegreePosition(Double.valueOf(lat), Double.valueOf(lng))).getName();
    }

    private void updateSeries(Regatta regatta, SeriesCreationParametersDTO defaultSeries) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.REGATTA.getStringPermissionForObject(DefaultActions.UPDATE, regatta));
        getService().apply(new UpdateSeries(new RegattaName(regatta.getName()), "Default", "Default", defaultSeries.isMedal(),
                defaultSeries.isFleetsCanRunInParallel(), defaultSeries.getDiscardingThresholds(),
                defaultSeries.isStartsWithZero(), defaultSeries.isFirstColumnIsNonDiscardableCarryForward(),
                defaultSeries.hasSplitFleetContiguousScoring(), defaultSeries.hasCrossFleetMergedRanking(), defaultSeries.getMaximumNumberOfDiscards(),
                defaultSeries.isOneAlwaysStaysOne(), defaultSeries.getFleets()));
    }

    private void addRaceColumns(String regattaName, String seriesName, int numberOfRaces) {
        final Regatta regatta = findRegattaByName(regattaName);
        if (regatta == null) {
            throw new IllegalArgumentException(ExceptionManager.objectNotFoundMsg("regatta", regattaName));
        }
        int oneBasedNumberOfNextRace = Util.size(regatta.getRaceColumns())+1;
        for (int i = 1; i <= numberOfRaces; i++) {
            addRaceColumn(regatta, seriesName, "R"+oneBasedNumberOfNextRace++);
        }
    }

    private RaceColumnInSeries addRaceColumn(Regatta regatta, String seriesName, String columnName) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.REGATTA.getStringPermissionForObject(DefaultActions.UPDATE, regatta));
        return getService().apply(new AddColumnToSeries(new RegattaName(regatta.getName()), seriesName, columnName));
    }

    private void updateEvent(Event event, LeaderboardGroup leaderboardGroup){
        getSecurityService().checkCurrentUserUpdatePermission(event);
        List<UUID> newLeaderboardGroupIds = new ArrayList<>();
        StreamSupport.stream(event.getLeaderboardGroups().spliterator(), false)
                .forEach(lg -> newLeaderboardGroupIds.add(lg.getId()));
        newLeaderboardGroupIds.add(leaderboardGroup.getId());
        getService().updateEvent(event.getId(), event.getName(), event.getDescription(), event.getStartDate(),
                event.getEndDate(), event.getVenue().getName(), event.isPublic(), newLeaderboardGroupIds,
                event.getOfficialWebsiteURL(), event.getBaseURL(), event.getSailorsInfoWebsiteURLs(), event.getImages(),
                event.getVideos(), event.getWindFinderReviewedSpotsCollectionIds());
    }

    private CourseArea addCourseArea(Event event, String courseAreaName) {
        SecurityUtils.getSubject().checkPermission(
                SecuredDomainType.EVENT.getStringPermissionForObject(DefaultActions.UPDATE, event));
        String[] courseAreaNames = new String[] { courseAreaName };
        UUID[] courseAreaIds = new UUID[] { UUID.randomUUID() };
        final CourseArea[] courseAreas = getService().apply(
                new AddCourseAreas(event.getId(), courseAreaNames, courseAreaIds,
                        /* centerPositions */ new Position[] { null }, /* radiuses */ new Distance[] { null }));
        return courseAreas[0];
    }

    private void addLeaderboardToDefaultLeaderboardGroup(final RegattaLeaderboard leaderboard) {
        LeaderboardGroup defaultLeaderboardGroup = null;
        for (Event event : getService().getAllEvents()) {
            Iterable<CourseArea> courseAreas = event.getVenue().getCourseAreas();
            for (CourseArea courseArea : courseAreas) {
                if (Util.contains(leaderboard.getRegatta().getCourseAreas(), courseArea.getId())) {
                    for (LeaderboardGroup lg : event.getLeaderboardGroups()) {
                        // if leaderboard group is default leaderboard group, then add leaderboard
                        if (lg.getName().equals(event.getName())) {
                            defaultLeaderboardGroup = lg;
                        }
                    }
                }
            }
        }
        if (defaultLeaderboardGroup != null) {
            defaultLeaderboardGroup.addLeaderboard(leaderboard);
            List<String> leaderboards = stream(defaultLeaderboardGroup.getLeaderboards().spliterator()).map(lg -> lg.getName())
                    .collect(Collectors.toList());

            ResultDiscardingRule rule = defaultLeaderboardGroup.getOverallLeaderboard()==null?null:defaultLeaderboardGroup.getOverallLeaderboard().getResultDiscardingRule();
            int[] overallLeaderboardDiscardThresholds = null;
            if(rule instanceof ThresholdBasedResultDiscardingRule){
                ThresholdBasedResultDiscardingRule resultDiscardingRule = (ThresholdBasedResultDiscardingRule) rule;
                overallLeaderboardDiscardThresholds = resultDiscardingRule.getDiscardIndexResultsStartingWithHowManyRaces();
            }
            getService().updateLeaderboardGroup(defaultLeaderboardGroup.getId(), defaultLeaderboardGroup.getName(), defaultLeaderboardGroup.getDescription(),
                    defaultLeaderboardGroup.getDisplayName(), leaderboards, overallLeaderboardDiscardThresholds,
                    defaultLeaderboardGroup.getOverallLeaderboard()==null?null:defaultLeaderboardGroup.getOverallLeaderboard().getScoringScheme().getType());
        }
    }

    private RegattaLeaderboard addLeaderboard(String regattaName, int[] discardThresholds) {
        RegattaLeaderboard leaderboard = null;
        try {
            leaderboard = createRegattaLeaderboard(regattaName, discardThresholds);
        } catch (IllegalArgumentException e) {
            throw e;
        }
        addLeaderboardToDefaultLeaderboardGroup(leaderboard);
        return leaderboard;
    }

    private RegattaLeaderboard createRegattaLeaderboard(String regattaName, int[] discardThresholds) {
        return getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                SecuredDomainType.LEADERBOARD, Leaderboard.getTypeRelativeObjectIdentifier(regattaName), regattaName,
                new Callable<RegattaLeaderboard>() {
                    @Override
                    public RegattaLeaderboard call() throws Exception {
                        return getService()
                                .apply(new CreateRegattaLeaderboard(new RegattaName(regattaName), regattaName, discardThresholds));
                    }
                });
    }

    private Response getBadEventErrorResponse(String eventId) {
        return Response.status(Status.NOT_FOUND)
                .entity("Could not find an event with id '" + StringEscapeUtils.escapeHtml(eventId) + "'.")
                .type(MediaType.TEXT_PLAIN).build();
    }

    private MillisecondsTimePoint now() {
        return new MillisecondsTimePoint(new Date());
    }

    private Date addOneWeek(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.WEEK_OF_MONTH, 1);
        return c.getTime();
    }

    private URL toURL(String url) throws MalformedURLException{
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new MalformedURLException(ExceptionManager.invalidURLFormatMsg(url));
        }
    }

    private List<UUID> toUUIDList(List<String> list){
        return list.stream().map(id -> toUUID(id))
                .collect(Collectors.toList());
    }

    private UUID toUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(ExceptionManager.invalidIdFormatMsg(id));
        }
    }

    private LinkedHashMap<String, SeriesCreationParametersDTO> createDefaultSeriesCreationParameters(String regattaName,int numberOfRaces ) {
        final LinkedHashMap<String, SeriesCreationParametersDTO> series = new LinkedHashMap<>();
        series.put(LeaderboardNameConstants.DEFAULT_SERIES_NAME, new SeriesCreationParametersDTO(
                Arrays.asList(new FleetDTO(LeaderboardNameConstants.DEFAULT_FLEET_NAME, 0, null)),
                /* isMedal */ false, /* isFleetsCanRunInParallel */ false, /* isStartsWithZeroScore */ false, /* firstColumnIsNonDiscardableCarryForward */ false,
                /* discardingThresholds */ null, /* hasSplitFleetContiguousScoring */ false, /* hasCrossFleetMergedRanking */ false, /* maximumNumberOfDiscards */ null, /* oneAlwaysStaysOne */ false));
        return series;
    }

    private RankingMetrics createRankingMetric(String rankingMetricParam) {
        try {
            return RankingMetrics.valueOf(rankingMetricParam);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(ExceptionManager.incorrectParameterValue(rankingMetricParam,
                    getEnumValuesAsString(RankingMetrics.class)));
        }
    }

    private ScoringScheme createScoringScheme(String scoringSchemeParam) {
        ScoringScheme scoringScheme = getService().getBaseDomainFactory().createScoringScheme(getScoringSchemeType(scoringSchemeParam));
        return scoringScheme;
    }

    private <E extends Enum<E>> String getEnumValuesAsString(Class<E> e) {
        return EnumSet.allOf(e).stream().map(en -> en.name()).collect(Collectors.joining(", "));
    }

    private ScoringSchemeType getScoringSchemeType(String scoringSchemeTypeParam) {
        try {
            return ScoringSchemeType.valueOf(scoringSchemeTypeParam);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(ExceptionManager.incorrectParameterValue(scoringSchemeTypeParam,
                    getEnumValuesAsString(ScoringSchemeType.class)));
        }
    }

    private Event getEvent(UUID eventId) throws NotFoundException {
        Event event = getService().getEvent(eventId);
        if (event != null) {
            return event;
        }
        throw new NotFoundException(ExceptionManager.objectNotFoundMsg(Event.class.getSimpleName(), eventId));
    }

    private <T> Stream<T> stream(Spliterator<T> spliterator) {
        return StreamSupport.stream(spliterator, false);
    }
}
