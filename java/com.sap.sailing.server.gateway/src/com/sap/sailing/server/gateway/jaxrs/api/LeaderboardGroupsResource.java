package com.sap.sailing.server.gateway.jaxrs.api;

import java.io.Serializable;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
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
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.MetaLeaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.SettableScoreCorrection;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.gateway.serialization.LeaderboardGroupConstants;
import com.sap.sailing.server.hierarchy.SailingHierarchyOwnershipUpdater;
import com.sap.sailing.server.operationaltransformation.CreateLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardGroup;
import com.sap.sailing.server.util.RaceBoardLinkFactory;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
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
    protected static final String NAME_QUERY_PARAM = "name";
    protected static final String DESCRIPTION_QUERY_PARAM = "description";
    protected static final String DISPLAY_NAME_QUERY_PARAM = "displayName";
    protected static final String DISPLAY_GROUPS_IN_REVERSE_ORDER_QUERY_PARAM = "displayGroupsInReverseOrder";
    protected static final String OVERALL_LEADERBOARD_SCORING_SCHEME_QUERY_PARAM = "overallLeaderboardScoringScheme";
    protected static final String OVERALL_LEADERBOARD_DISCARD_THRESHOLDS_QUERY_PARAM = "overallLeaderboardDiscardThresholds";
    protected static final String LEADERBOARD_NAME_QUERY_PARAM = "leaderboardName";
    // Preserve the Date.toString() format, using UTC independently of the runtime's time zone and locale.
    private static final DateTimeFormatter LAST_SCORING_UPDATE_FORMATTER = DateTimeFormatter
            .ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).withZone(ZoneId.of("UTC"));

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
        final LeaderboardGroup leaderboardGroup = getLeaderboardGroupByNameOrId(leaderboardGroupName);
        if (leaderboardGroup == null) {
            response = getLeaderboardGroupNotFoundResponse(leaderboardGroupName);
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
                            final TimePoint lastUpdateTimepoint = scoreCorrection.getTimePointOfLastCorrectionsValidity();
                            jsonLeaderboard.put(LeaderboardNameConstants.LASTSCORINGUPDATE,
                                    lastUpdateTimepoint != null
                                            ? LAST_SCORING_UPDATE_FORMATTER.format(lastUpdateTimepoint.asDate().toInstant())
                                            : null);
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

    /**
     * Creates an empty leaderboard group, the way the AdminConsole does it. Leaderboards are added to it with
     * {@link #addLeaderboardToLeaderboardGroup(String, String)}. This lets a client build what a series of regattas
     * (e.g., one season of a league) usually is: one leaderboard group holding the regatta leaderboards of all its
     * events, optionally with an overall leaderboard, referenced by each of the events.
     * <p>
     * 
     * Security-wise, the calling subject needs the {@code LEADERBOARD_GROUP:CREATE} permission for the new group, and
     * if an overall leaderboard is requested, the {@code LEADERBOARD:CREATE} permission for the overall leaderboard.
     * 
     * @param leaderboardGroupName
     *            the unique name of the new group; required
     * @param description
     *            the group's description; defaults to the empty string
     * @param displayName
     *            the group's display name; optional
     * @param displayGroupsInReverseOrder
     *            whether the group's leaderboards are shown in reverse order; defaults to {@code false}
     * @param overallLeaderboardScoringSchemeName
     *            name of a {@link ScoringSchemeType}; if provided, an overall leaderboard is created for the group;
     *            if a leaderboard with the overall leaderboard's name exists already, {@link Status#CONFLICT} is
     *            returned and nothing is created
     * @param overallLeaderboardDiscardThresholds
     *            the discard thresholds of the overall leaderboard; optional
     * @return a JSON object with the new group's {@code id}, {@code name}, {@code displayName}, {@code description},
     *         {@code hasOverallLeaderboard}, {@code overallLeaderboardName} (if any) and {@code leaderboards}
     */
    @POST
    @Produces("application/json;charset=UTF-8")
    public Response createLeaderboardGroup(@QueryParam(NAME_QUERY_PARAM) final String leaderboardGroupName,
            @QueryParam(DESCRIPTION_QUERY_PARAM) final String description,
            @QueryParam(DISPLAY_NAME_QUERY_PARAM) final String displayName,
            @QueryParam(DISPLAY_GROUPS_IN_REVERSE_ORDER_QUERY_PARAM) final Boolean displayGroupsInReverseOrder,
            @QueryParam(OVERALL_LEADERBOARD_SCORING_SCHEME_QUERY_PARAM) final String overallLeaderboardScoringSchemeName,
            @QueryParam(OVERALL_LEADERBOARD_DISCARD_THRESHOLDS_QUERY_PARAM) final List<Integer> overallLeaderboardDiscardThresholds) {
        final Response response;
        if (leaderboardGroupName == null || leaderboardGroupName.isEmpty()) {
            response = getBadRequestResponse("Parameter " + NAME_QUERY_PARAM + " is required.");
        } else if (getService().getLeaderboardGroupByName(leaderboardGroupName) != null) {
            response = Response.status(Status.CONFLICT)
                    .entity("A leaderboard group with name '" + StringEscapeUtils.escapeHtml(leaderboardGroupName)
                            + "' already exists.")
                    .type(MediaType.TEXT_PLAIN).build();
        } else if (overallLeaderboardScoringSchemeName != null && !overallLeaderboardScoringSchemeName.isEmpty()
                && !isValidScoringSchemeType(overallLeaderboardScoringSchemeName)) {
            response = getBadRequestResponse("Unknown scoring scheme '"
                    + StringEscapeUtils.escapeHtml(overallLeaderboardScoringSchemeName) + "'; use one of "
                    + Arrays.toString(ScoringSchemeType.values()) + ".");
        } else if (overallLeaderboardScoringSchemeName != null && !overallLeaderboardScoringSchemeName.isEmpty()
                && getService().getLeaderboardByName(
                        LeaderboardGroupMetaLeaderboard.getOverallLeaderboardName(leaderboardGroupName)) != null) {
            // the overall leaderboard is named after the group; creating it would replace an existing leaderboard
            // of that name, so refuse before anything is created
            response = Response.status(Status.CONFLICT)
                    .entity("A leaderboard with name '" + StringEscapeUtils.escapeHtml(
                            LeaderboardGroupMetaLeaderboard.getOverallLeaderboardName(leaderboardGroupName))
                            + "' already exists; it would be replaced by the group's overall leaderboard.")
                    .type(MediaType.TEXT_PLAIN).build();
        } else {
            final ScoringSchemeType overallLeaderboardScoringSchemeType = overallLeaderboardScoringSchemeName == null
                    || overallLeaderboardScoringSchemeName.isEmpty() ? null
                            : ScoringSchemeType.valueOf(overallLeaderboardScoringSchemeName);
            final int[] overallLeaderboardDiscardThresholdsAsArray = overallLeaderboardDiscardThresholds == null
                    ? new int[0] : overallLeaderboardDiscardThresholds.stream().mapToInt(i -> i).toArray();
            final UUID leaderboardGroupId = UUID.randomUUID();
            final Callable<LeaderboardGroup> createLeaderboardGroupOperation = () -> getService()
                    .apply(new CreateLeaderboardGroup(leaderboardGroupId, leaderboardGroupName,
                            description == null ? "" : description, displayName,
                            displayGroupsInReverseOrder != null && displayGroupsInReverseOrder,
                            new ArrayList<String>(), overallLeaderboardDiscardThresholdsAsArray,
                            overallLeaderboardScoringSchemeType));
            final Callable<LeaderboardGroup> createLeaderboardGroupWithOwnership = () -> getSecurityService()
                    .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.LEADERBOARD_GROUP,
                            LeaderboardGroupImpl.getTypeRelativeObjectIdentifier(leaderboardGroupId),
                            leaderboardGroupName, createLeaderboardGroupOperation);
            final LeaderboardGroup leaderboardGroup;
            // if an overall leaderboard is requested, establish its ownership and check its create permission, too,
            // around the group's creation, the same way the AdminConsole does it
            if (overallLeaderboardScoringSchemeType != null) {
                final String overallLeaderboardName = LeaderboardGroupMetaLeaderboard
                        .getOverallLeaderboardName(leaderboardGroupName);
                leaderboardGroup = getSecurityService().setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                        SecuredDomainType.LEADERBOARD, new TypeRelativeObjectIdentifier(overallLeaderboardName),
                        overallLeaderboardName, createLeaderboardGroupWithOwnership);
            } else {
                try {
                    leaderboardGroup = createLeaderboardGroupWithOwnership.call();
                } catch (RuntimeException e) {
                    // rethrown unchanged so that, e.g., an AuthorizationException maps to 401 as in the other branch
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            response = Response.ok(streamingOutput(getLeaderboardGroupSummaryAsJson(leaderboardGroup))).build();
        }
        return response;
    }

    /**
     * Adds an existing leaderboard to an existing leaderboard group, at the end of the group's leaderboards, using the
     * same {@link UpdateLeaderboardGroup} operation as the AdminConsole, so persistence and replication work as there.
     * The group's name, description, display name and overall leaderboard remain unchanged. Adding a leaderboard that
     * the group already contains changes nothing.
     * <p>
     * 
     * Security-wise, the calling subject needs the {@code UPDATE} permission for the group, the {@code UPDATE}
     * permission for the group's overall leaderboard if it has one, and the {@code READ} permission for the leaderboard
     * to add.
     * 
     * @param leaderboardGroupNameOrId
     *            the group's name or ID
     * @param leaderboardName
     *            the name of the leaderboard to add; required
     * @return the group's summary as for {@link #createLeaderboardGroup}
     */
    @POST
    @Produces("application/json;charset=UTF-8")
    @Path("{" + NAME_OR_UUID_PATH_PARAM + "}/leaderboards")
    public Response addLeaderboardToLeaderboardGroup(
            @PathParam(NAME_OR_UUID_PATH_PARAM) final String leaderboardGroupNameOrId,
            @QueryParam(LEADERBOARD_NAME_QUERY_PARAM) final String leaderboardName) {
        final Response response;
        final LeaderboardGroup leaderboardGroup = getLeaderboardGroupByNameOrId(leaderboardGroupNameOrId);
        final Leaderboard leaderboard = leaderboardName == null ? null
                : getService().getLeaderboardByName(leaderboardName);
        if (leaderboardGroup == null) {
            response = getLeaderboardGroupNotFoundResponse(leaderboardGroupNameOrId);
        } else if (leaderboardName == null || leaderboardName.isEmpty()) {
            response = getBadRequestResponse("Parameter " + LEADERBOARD_NAME_QUERY_PARAM + " is required.");
        } else if (leaderboard == null) {
            response = Response.status(Status.NOT_FOUND)
                    .entity("Could not find a leaderboard with name '" + StringEscapeUtils.escapeHtml(leaderboardName)
                            + "'.")
                    .type(MediaType.TEXT_PLAIN).build();
        } else {
            getSecurityService().checkCurrentUserUpdatePermission(leaderboardGroup);
            getSecurityService().checkCurrentUserReadPermission(leaderboard);
            final Leaderboard overallLeaderboard = leaderboardGroup.getOverallLeaderboard();
            if (overallLeaderboard != null) {
                getSecurityService().checkCurrentUserUpdatePermission(overallLeaderboard);
            }
            // There is no replicable operation that appends a single leaderboard to a group; UpdateLeaderboardGroup
            // replaces the group's membership with the list it is given. Reading the membership and applying the
            // update while holding the group's monitor keeps two concurrent calls of this endpoint from losing one
            // another's leaderboard. This does not serialize against other writers of the group's membership (e.g.,
            // the AdminConsole's updateLeaderboardGroup), which use the same snapshot-replacing operation.
            synchronized (leaderboardGroup) {
                if (!Util.contains(leaderboardGroup.getLeaderboards(), leaderboard)) {
                    final List<String> leaderboardNames = new ArrayList<>();
                    for (final Leaderboard leaderboardInGroup : leaderboardGroup.getLeaderboards()) {
                        leaderboardNames.add(leaderboardInGroup.getName());
                    }
                    leaderboardNames.add(leaderboard.getName());
                    final int[] overallLeaderboardDiscardThresholds = overallLeaderboard != null && overallLeaderboard
                            .getResultDiscardingRule() instanceof ThresholdBasedResultDiscardingRule
                                    ? ((ThresholdBasedResultDiscardingRule) overallLeaderboard
                                            .getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces()
                                    : new int[0];
                    getService().apply(new UpdateLeaderboardGroup(leaderboardGroup.getId(), leaderboardGroup.getName(),
                            leaderboardGroup.getDescription() == null ? "" : leaderboardGroup.getDescription(),
                            leaderboardGroup.getDisplayName(), leaderboardNames, overallLeaderboardDiscardThresholds,
                            overallLeaderboard == null ? null : overallLeaderboard.getScoringScheme().getType()));
                }
            }
            response = Response.ok(streamingOutput(getLeaderboardGroupSummaryAsJson(leaderboardGroup))).build();
        }
        return response;
    }

    private LeaderboardGroup getLeaderboardGroupByNameOrId(final String leaderboardGroupNameOrId) {
        final Serializable uuid = UUIDHelper.tryUuidConversion(leaderboardGroupNameOrId);
        final LeaderboardGroup leaderboardGroup;
        if (uuid != leaderboardGroupNameOrId) {
            leaderboardGroup = getService().getLeaderboardGroupByID((UUID) uuid);
        } else {
            leaderboardGroup = getService().getLeaderboardGroupByName(leaderboardGroupNameOrId);
        }
        return leaderboardGroup;
    }

    private JSONObject getLeaderboardGroupSummaryAsJson(final LeaderboardGroup leaderboardGroup) {
        final JSONObject result = new JSONObject();
        result.put(LeaderboardGroupConstants.ID, leaderboardGroup.getId().toString());
        result.put(LeaderboardGroupConstants.NAME, leaderboardGroup.getName());
        result.put(LeaderboardGroupConstants.DISPLAYNAME, leaderboardGroup.getDisplayName());
        result.put(LeaderboardGroupConstants.DESCRIPTION, leaderboardGroup.getDescription());
        result.put(LeaderboardGroupConstants.HAS_OVERALL_LEADERBOARD, leaderboardGroup.hasOverallLeaderboard());
        if (leaderboardGroup.hasOverallLeaderboard()) {
            result.put(LeaderboardGroupConstants.OVERALL_LEADERBOARD_NAME, leaderboardGroup.getOverallLeaderboardName());
        }
        final JSONArray leaderboardNames = new JSONArray();
        for (final Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
            leaderboardNames.add(leaderboard.getName());
        }
        result.put(LeaderboardGroupConstants.LEADERBOARDS, leaderboardNames);
        return result;
    }

    private boolean isValidScoringSchemeType(final String scoringSchemeTypeName) {
        boolean valid;
        try {
            ScoringSchemeType.valueOf(scoringSchemeTypeName);
            valid = true;
        } catch (IllegalArgumentException e) {
            valid = false;
        }
        return valid;
    }

    private Response getLeaderboardGroupNotFoundResponse(final String leaderboardGroupNameOrId) {
        return Response.status(Status.NOT_FOUND)
                .entity("Could not find a leaderboard group with name '"
                        + StringEscapeUtils.escapeHtml(leaderboardGroupNameOrId) + "'.")
                .type(MediaType.TEXT_PLAIN).build();
    }

    private Response getBadRequestResponse(final String message) {
        return Response.status(Status.BAD_REQUEST).entity(message).type(MediaType.TEXT_PLAIN).build();
    }
}
