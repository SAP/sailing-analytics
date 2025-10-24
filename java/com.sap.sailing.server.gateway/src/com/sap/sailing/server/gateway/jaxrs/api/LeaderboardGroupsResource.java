package com.sap.sailing.server.gateway.jaxrs.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.MetaLeaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.SettableScoreCorrection;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.gateway.serialization.LeaderboardGroupConstants;
import com.sap.sailing.server.hierarchy.SailingHierarchyOwnershipUpdater;
import com.sap.sailing.server.util.RaceBoardLinkFactory;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.util.impl.UUIDHelper;

@Path(LeaderboardGroupsResource.V1_LEADERBOARDGROUPS)
public class LeaderboardGroupsResource extends AbstractSailingServerResource {
    public static final String IDENTIFIABLE = "/identifiable";
    public static final String V1_LEADERBOARDGROUPS = "/v1/leaderboardgroups";
    protected static final String NAME_OR_UUID_PATH_PARAM = "nameOrUUID";
    protected static final String COPY_MEMBERS_AND_ROLES_QUERY_PARAM = "copyMembersAndRoles";
    protected static final String MIGRATE_BOATS_QUERY_PARAM = "migrateBoats";
    protected static final String MIGRATE_COMPETITORS_QUERY_PARAM = "migrateCompetitors";
    protected static final String NEW_GROUP_NAME_QUERY_PARAM = "newGroupName";
    protected static final String EXISTING_GROUP_ID_QUERY_PARAM = "existingGroupId";
    protected static final String CREATE_NEW_GROUP_QUERY_PARAM = "createNewGroup";
    protected static final String NAME_PATH_PARAM = "name";

    @POST
    @Path("/{"+NAME_PATH_PARAM+"}/migrate")
    public Response migrateOwnershipForLeaderboardGroup(@PathParam(NAME_PATH_PARAM) String leaderboardGroupName,
            @QueryParam(CREATE_NEW_GROUP_QUERY_PARAM) Boolean createNewGroup,
            @QueryParam(EXISTING_GROUP_ID_QUERY_PARAM) UUID existingGroupIdOrNull, @QueryParam(NEW_GROUP_NAME_QUERY_PARAM) String newGroupName,
            @QueryParam(MIGRATE_COMPETITORS_QUERY_PARAM) Boolean migrateCompetitors,
            @QueryParam(MIGRATE_BOATS_QUERY_PARAM) Boolean migrateBoats,
            @QueryParam(COPY_MEMBERS_AND_ROLES_QUERY_PARAM) Boolean copyMembersAndRoles)
            throws ParseException, JsonDeserializationException {
        LeaderboardGroup leaderboardGroup = getService().getLeaderboardGroupByName(leaderboardGroupName);
        SailingHierarchyOwnershipUpdater updater = SailingHierarchyOwnershipUpdater.createOwnershipUpdater(
                createNewGroup, existingGroupIdOrNull, newGroupName, migrateCompetitors, migrateBoats,
                copyMembersAndRoles == null ? true : copyMembersAndRoles, getService());
        updater.updateGroupOwnershipForLeaderboardGroupHierarchy(leaderboardGroup);
        return Response.ok().build();
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getLeaderboardGroups() {
        final JSONArray jsonLeaderboardGroups = getLeaderboardGroups(leaderboardGroup->leaderboardGroup.getName());
        // header option is set to allow communication between two sapsailing servers, especially for
        // the master data import functionality
        return Response.ok(streamingOutput(jsonLeaderboardGroups)).build();
    }

    private JSONArray getLeaderboardGroups(Function<LeaderboardGroup, Object> resultObjectSupplier) {
        JSONArray jsonLeaderboardGroups = new JSONArray();
        for (final LeaderboardGroup leaderboardGroupEntry : getService().getLeaderboardGroups().values()) {
            if (getSecurityService().hasCurrentUserReadPermission(leaderboardGroupEntry)) {
                jsonLeaderboardGroups.add(resultObjectSupplier.apply(leaderboardGroupEntry));
            }
        }
        // header option is set to allow communication between two sapsailing servers, especially for
        // the master data import functionality
        return jsonLeaderboardGroups;
    }

    @GET
    @Path(IDENTIFIABLE)
    @Produces("application/json;charset=UTF-8")
    public Response getLeaderboardGroupsIdentifiable() {
        final JSONArray jsonLeaderboardGroups = getLeaderboardGroups(leaderboardGroup->{
            JSONObject leaderboardGroupObject = new JSONObject();
            leaderboardGroupObject.put(LeaderboardGroupConstants.ID, leaderboardGroup.getId().toString());
            leaderboardGroupObject.put(LeaderboardGroupConstants.NAME, leaderboardGroup.getName());
            return leaderboardGroupObject;
        });
        // header option is set to allow communication between two sapsailing servers, especially for
        // the master data import functionality
        return Response.ok(streamingOutput(jsonLeaderboardGroups)).build();
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{"+NAME_OR_UUID_PATH_PARAM+"}")
    public Response getLeaderboardGroup(@PathParam(NAME_OR_UUID_PATH_PARAM) String leaderboardGroupName) {
        Response response;
        final Serializable uuid = UUIDHelper.tryUuidConversion(leaderboardGroupName);
        final LeaderboardGroup leaderboardGroup;
        if (uuid != leaderboardGroupName) {
            leaderboardGroup = getService().getLeaderboardGroupByID((UUID) uuid);
        } else {
            leaderboardGroup = getService().getLeaderboardGroupByName(leaderboardGroupName);
        }
        if (leaderboardGroup == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a leaderboard group with name '"
                            + StringEscapeUtils.escapeHtml(leaderboardGroupName) + "'.")
                    .type(MediaType.TEXT_PLAIN).build();
        } else {
            if (getSecurityService().hasCurrentUserReadPermission(leaderboardGroup)) {
                final TimePoint timePoint = MillisecondsTimePoint.now();
                final JSONObject jsonLeaderboardGroup = new JSONObject();
                jsonLeaderboardGroup.put(LeaderboardGroupConstants.NAME, leaderboardGroup.getName());
                jsonLeaderboardGroup.put(LeaderboardGroupConstants.ID, leaderboardGroup.getId().toString());
                jsonLeaderboardGroup.put(LeaderboardGroupConstants.DISPLAYNAME, leaderboardGroup.getDisplayName());
                jsonLeaderboardGroup.put(LeaderboardGroupConstants.DESCRIPTION, leaderboardGroup.getDescription());
                jsonLeaderboardGroup.put(LeaderboardGroupConstants.TIMEPOINT, timePoint.toString());
                jsonLeaderboardGroup.put(LeaderboardGroupConstants.TIMEPOINT_MILLIS, timePoint.asMillis());
                jsonLeaderboardGroup.put(LeaderboardGroupConstants.HAS_OVERALL_LEADERBOARD, leaderboardGroup.hasOverallLeaderboard());
                if (leaderboardGroup.hasOverallLeaderboard()) {
                    jsonLeaderboardGroup.put(LeaderboardGroupConstants.OVERALL_LEADERBOARD_NAME, leaderboardGroup.getOverallLeaderboardName());
                }
                final Set<Event> eventsReferencingLeaderboardGroup = new HashSet<>();
                final JSONArray idsOfEventsReferencingLeaderboardGroup = new JSONArray();
                for (final Event event : getService().getAllEvents()) {
                    if (Util.contains(event.getLeaderboardGroups(), leaderboardGroup)) {
                        eventsReferencingLeaderboardGroup.add(event);
                        idsOfEventsReferencingLeaderboardGroup.add(event.getId().toString());
                    }
                }
                Collections.sort(idsOfEventsReferencingLeaderboardGroup, (id1, id2)->id1.toString().compareTo(id2.toString()));
                jsonLeaderboardGroup.put(LeaderboardGroupConstants.EVENTS, idsOfEventsReferencingLeaderboardGroup);
                JSONArray jsonLeaderboardEntries = new JSONArray();
                jsonLeaderboardGroup.put(LeaderboardGroupConstants.LEADERBOARDS, jsonLeaderboardEntries);
                for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                    if (getSecurityService().hasCurrentUserReadPermission(leaderboard)) {
                        boolean isMetaLeaderboard = leaderboard instanceof MetaLeaderboard ? true : false;
                        boolean isRegattaLeaderboard = leaderboard instanceof RegattaLeaderboard ? true : false;
                        JSONObject jsonLeaderboard = new JSONObject();
                        jsonLeaderboard.put(LeaderboardNameConstants.NAME, leaderboard.getName());
                        jsonLeaderboard.put(LeaderboardNameConstants.DISPLAYNAME, leaderboard.getDisplayName());
                        jsonLeaderboard.put(LeaderboardNameConstants.ISMETALEADERBOARD, isMetaLeaderboard);
                        jsonLeaderboard.put(LeaderboardNameConstants.ISREGATTALEADERBOARD, isRegattaLeaderboard);
                        final JSONArray discardIndices = AbstractLeaderboardsResource.getDiscardingRuleAsJson(leaderboard);
                        if (discardIndices != null) {
                            jsonLeaderboard.put(LeaderboardNameConstants.DISCARDS, discardIndices);
                        }
                        jsonLeaderboardEntries.add(jsonLeaderboard);
                        SettableScoreCorrection scoreCorrection = leaderboard.getScoreCorrection();
                        if (scoreCorrection != null) {
                            jsonLeaderboard.put(LeaderboardNameConstants.SCORINGCOMMENT, scoreCorrection.getComment());
                            TimePoint lastUpdateTimepoint = scoreCorrection.getTimePointOfLastCorrectionsValidity();
                            jsonLeaderboard.put(LeaderboardNameConstants.LASTSCORINGUPDATE,
                                    lastUpdateTimepoint != null ? lastUpdateTimepoint.asDate().toString() : null);
                            jsonLeaderboard.put(LeaderboardNameConstants.LASTSCORINGUPDATE_MILLIS,
                                    lastUpdateTimepoint != null ? lastUpdateTimepoint.asMillis() : null);
                        } else {
                            jsonLeaderboard.put(LeaderboardNameConstants.SCORINGCOMMENT, null);
                            jsonLeaderboard.put(LeaderboardNameConstants.LASTSCORINGUPDATE, null);
                        }
                        final List<Triple<String, Iterable<Fleet>, Iterable<? extends RaceColumn>>> seriesNameAndFleetsAndRaceColumnsOfSeries = new ArrayList<>();
                        final Map<String, Boolean> medalSeriesNames = new HashMap<>();
                        if (isRegattaLeaderboard) {
                            RegattaLeaderboard regattaLeaderboard = (RegattaLeaderboard) leaderboard;
                            Regatta regatta = regattaLeaderboard.getRegatta();
                            jsonLeaderboard.put(LeaderboardNameConstants.SCORINGSCHEME,
                                    leaderboard.getScoringScheme().getType());
                            jsonLeaderboard.put(LeaderboardNameConstants.REGATTANAME, regatta.getName());
                            for (final Series series : regatta.getSeries()) {
                                List<Fleet> fleets = new ArrayList<>();
                                Util.addAll(series.getFleets(), fleets);
                                seriesNameAndFleetsAndRaceColumnsOfSeries
                                        .add(new Triple<String, Iterable<Fleet>, Iterable<? extends RaceColumn>>(
                                                series.getName(), fleets, series.getRaceColumns()));
                                medalSeriesNames.put(series.getName(), series.isMedal());
                            }
                        } else {
                            jsonLeaderboard.put(LeaderboardNameConstants.SCORINGSCHEME,
                                    leaderboard.getScoringScheme().getType());
                            jsonLeaderboard.put(LeaderboardNameConstants.REGATTANAME, null);
                            // write a 'default' series to conform with our common regatta structure
                            seriesNameAndFleetsAndRaceColumnsOfSeries
                                    .add(new Triple<String, Iterable<Fleet>, Iterable<? extends RaceColumn>>(
                                            LeaderboardNameConstants.DEFAULT_SERIES_NAME,
                                            Collections.singleton(
                                                    leaderboard.getFleet(LeaderboardNameConstants.DEFAULT_FLEET_NAME)),
                                            leaderboard.getRaceColumns()));
                        }
                        JSONArray jsonSeriesEntries = new JSONArray();
                        jsonLeaderboard.put(LeaderboardNameConstants.SERIES, jsonSeriesEntries);
                        for (final Triple<String, Iterable<Fleet>, Iterable<? extends RaceColumn>> e : seriesNameAndFleetsAndRaceColumnsOfSeries) {
                            JSONObject jsonSeries = new JSONObject();
                            jsonSeriesEntries.add(jsonSeries);
                            jsonSeries.put(NAME_PATH_PARAM, e.getA());
                            jsonSeries.put(LeaderboardNameConstants.ISMEDALSERIES, medalSeriesNames.get(e.getA()));
                            JSONArray jsonFleetsEntries = new JSONArray();
                            jsonSeries.put(LeaderboardNameConstants.FLEETS, jsonFleetsEntries);
                            for (final Fleet fleet : e.getB()) {
                                if (fleet != null) {
                                    JSONObject jsonFleet = new JSONObject();
                                    jsonFleet.put(NAME_PATH_PARAM, fleet.getName());
                                    jsonFleet.put(LeaderboardNameConstants.COLOR,
                                            fleet.getColor() != null ? fleet.getColor().getAsHtml() : null);
                                    jsonFleet.put(LeaderboardNameConstants.ORDERING, fleet.getOrdering());
                                    jsonFleetsEntries.add(jsonFleet);
                                    JSONArray jsonRacesEntries = new JSONArray();
                                    jsonFleet.put(LeaderboardNameConstants.RACES, jsonRacesEntries);
                                    for (RaceColumn raceColumn : e.getC()) {
                                        JSONObject jsonRaceColumn = new JSONObject();
                                        jsonRaceColumn.put(NAME_PATH_PARAM, raceColumn.getName());
                                        jsonRaceColumn.put(LeaderboardNameConstants.ISMEDALRACE,
                                                raceColumn.isMedalRace());
                                        TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                                        if (trackedRace != null) {
                                            jsonRaceColumn.put(LeaderboardNameConstants.ISTRACKED, true);
                                            jsonRaceColumn.put(LeaderboardNameConstants.REGATTANAME,
                                                    trackedRace.getTrackedRegatta().getRegatta().getName());
                                            jsonRaceColumn.put(LeaderboardNameConstants.TRACKEDRACENAME,
                                                    trackedRace.getRace().getName());
                                            jsonRaceColumn.put(LeaderboardNameConstants.TRACKINGPROVIDERTYPE,
                                                    trackedRace.getTrackingConnectorInfo().getTrackingConnectorName());
                                            jsonRaceColumn.put(LeaderboardNameConstants.RACEID,
                                                    trackedRace.getRace().getId().toString());
                                            final JSONObject raceBoardURLsByEventID = new JSONObject();
                                            for (Event event : eventsReferencingLeaderboardGroup) {
                                                if (Util.containsAny(event.getVenue().getCourseAreas(),
                                                        leaderboard.getCourseAreas())) {
                                                    raceBoardURLsByEventID.put(event.getId().toString(),
                                                            RaceBoardLinkFactory.createRaceBoardLink(trackedRace,
                                                                    leaderboard, event, leaderboardGroup, "PLAYER",
                                                                    /* locale */ null));
                                                }
                                            }
                                            jsonRaceColumn.put(LeaderboardNameConstants.RACEVIEWERURLS,
                                                    raceBoardURLsByEventID);
                                            jsonRaceColumn.put(LeaderboardNameConstants.HASGPSDATA,
                                                    trackedRace.hasGPSData());
                                            jsonRaceColumn.put(LeaderboardNameConstants.HASWINDDATA,
                                                    trackedRace.hasWindData());
                                        } else {
                                            jsonRaceColumn.put(LeaderboardNameConstants.ISTRACKED, false);
                                            jsonRaceColumn.put(LeaderboardNameConstants.TRACKEDRACENAME, null);
                                            jsonRaceColumn.put(LeaderboardNameConstants.TRACKINGPROVIDERTYPE, null);
                                            jsonRaceColumn.put(LeaderboardNameConstants.RACEID, null);
                                            jsonRaceColumn.put(LeaderboardNameConstants.HASGPSDATA, false);
                                            jsonRaceColumn.put(LeaderboardNameConstants.HASWINDDATA, false);
                                        }
                                        jsonRacesEntries.add(jsonRaceColumn);
                                    }
                                }
                            }
                        }
                    }
                }
                response = Response.ok(streamingOutput(jsonLeaderboardGroup)).build();
            } else {
                response = Response.status(Status.FORBIDDEN).build();
            }
        }
        return response;
    }
}
