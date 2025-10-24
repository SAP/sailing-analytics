package com.sap.sailing.server.gateway.jaxrs.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.shiro.SecurityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardEntryDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.dto.LegEntryDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.sharding.ShardingType;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.CompetitorJsonConstants;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.sharding.ShardingContext;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.InvalidDateException;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;

@Path("/v2/leaderboards")
public class LeaderboardsResourceV2 extends AbstractLeaderboardsResource {

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{name}")
    public Response getLeaderboard(@PathParam("name") String leaderboardName,
            @DefaultValue("Live") @QueryParam("resultState") ResultStates resultState,
            @QueryParam("columnNames") final List<String> raceColumnNames,
            @QueryParam("raceDetails") final List<String> raceDetails,
            @QueryParam("time") String time, @QueryParam("timeasmillis") Long timeasmillis,
            @QueryParam("maxCompetitorsCount") Integer maxCompetitorsCount,
            @QueryParam("secret") String regattaSecret,
            @DefaultValue("false") @QueryParam("competitorAndBoatIdsOnly") boolean competitorAndBoatIdsOnly,
            @QueryParam("showOnlyActiveRacesForCompetitorIds") List<String> showOnlyActiveRacesForCompetitorIds,
            @DefaultValue("False") @QueryParam("showOnlyCompetitorsWithIdsProvided") Boolean showOnlyCompetitorsWithIdsProvided) {
        ShardingContext.setShardingConstraint(ShardingType.LEADERBOARDNAME, leaderboardName);
        try {
            Response response;
            Leaderboard leaderboard = getService().getLeaderboardByName(leaderboardName);
            if (leaderboard == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("Could not find a leaderboard with name '" + StringEscapeUtils.escapeHtml(leaderboardName) + "'.")
                        .type(MediaType.TEXT_PLAIN).build();
            } else {
                try {
                    boolean skip = getService().skipChecksDueToCorrectSecret(leaderboardName, regattaSecret);
                    if (!skip) {
                        getSecurityService().checkCurrentUserReadPermission(leaderboard);
                    }
                    TimePoint timePoint;
                    try {
                        timePoint = parseTimePoint(time, timeasmillis, calculateTimePointForResultState(leaderboard, resultState));
                    } catch (InvalidDateException e1) {
                        return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Could not parse the time.")
                                .type(MediaType.TEXT_PLAIN).build();
                    }
                    JSONObject jsonLeaderboard;
                    if (timePoint != null || resultState == ResultStates.Live) {
                        jsonLeaderboard = getLeaderboardJson(leaderboard, timePoint, resultState, maxCompetitorsCount,
                                raceColumnNames, raceDetails, competitorAndBoatIdsOnly,
                                showOnlyActiveRacesForCompetitorIds, skip, showOnlyCompetitorsWithIdsProvided);
                    } else {
                        jsonLeaderboard = createEmptyLeaderboardJson(leaderboard, resultState, maxCompetitorsCount, skip);
                    }
                    response = Response.ok(streamingOutput(jsonLeaderboard)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
                } catch (NoWindException | InterruptedException | ExecutionException e) {
                    response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage())
                            .type(MediaType.TEXT_PLAIN).build();
                }
            }
            return response;
        } finally {
            ShardingContext.clearShardingConstraint(ShardingType.LEADERBOARDNAME);
        }
    }

    @Override
    protected JSONObject getLeaderboardJson(Leaderboard leaderboard, TimePoint resultTimePoint,
            ResultStates resultState, Integer maxCompetitorsCount, List<String> raceColumnNames,
            List<String> raceDetailNames, boolean competitorAndBoatIdsOnly,
            List<String> showOnlyActiveRacesForCompetitorIds, boolean userPresentedValidRegattaSecret, boolean showOnlyCompetitorsWithIdsProvided)
            throws NoWindException, InterruptedException, ExecutionException {
        List<String> raceColumnsToShow = calculateRaceColumnsToShow(leaderboard, raceColumnNames, showOnlyActiveRacesForCompetitorIds, resultTimePoint);
        List<DetailType> raceDetailsToShow = calculateRaceDetailTypesToShow(raceDetailNames);
        LeaderboardDTO leaderboardDTO = leaderboard.getLeaderboardDTO(
                resultTimePoint, raceColumnsToShow, /* addOverallDetails */
                hasOverallDetail(raceDetailsToShow),
                getService(), getService().getBaseDomainFactory(),
                /* fillTotalPointsUncorrected */false);
        final MillisecondsTimePoint leaderboardTimePoint = new MillisecondsTimePoint(leaderboardDTO.getTimePoint());
        JSONObject jsonLeaderboard = new JSONObject();
        writeCommonLeaderboardData(jsonLeaderboard, leaderboard, resultState, leaderboardDTO.getTimePoint(), maxCompetitorsCount);
        Map<String, Map<String, Map<CompetitorDTO, Integer>>> competitorRanksPerRaceColumnsAndFleets = new HashMap<>();
        for (String raceColumnName : raceColumnsToShow) {
            List<CompetitorDTO> competitorsFromBestToWorst = leaderboardDTO.getCompetitorsFromBestToWorst(raceColumnName);
            Map<String, Map<CompetitorDTO, Integer>> competitorsOrderedByFleets = new HashMap<>();
            List<CompetitorDTO> filteredCompetitorsFromBestToWorst = new ArrayList<>();
            competitorsFromBestToWorst.forEach(competitor -> {
                if (userPresentedValidRegattaSecret || SecurityUtils.getSubject().isPermitted(competitor.getIdentifier()
                        .getStringPermission(SecuredSecurityTypes.PublicReadableActions.READ_PUBLIC))
                        || SecurityUtils.getSubject()
                                .isPermitted(competitor.getIdentifier().getStringPermission(DefaultActions.READ))) {
                    filteredCompetitorsFromBestToWorst.add(competitor);
                }
            });
            for (CompetitorDTO competitor : filteredCompetitorsFromBestToWorst) {
                LeaderboardRowDTO row = leaderboardDTO.rows.get(competitor);
                LeaderboardEntryDTO leaderboardEntry = row.fieldsByRaceColumnName.get(raceColumnName);
                FleetDTO fleetOfCompetitor = leaderboardEntry==null?null:leaderboardEntry.fleet;
                if (fleetOfCompetitor != null && fleetOfCompetitor.getName() != null) {
                    Map<CompetitorDTO, Integer> competitorsOfFleet = competitorsOrderedByFleets.get(fleetOfCompetitor.getName());
                    if (competitorsOfFleet == null) {
                        competitorsOfFleet = new HashMap<>();
                        competitorsOrderedByFleets.put(fleetOfCompetitor.getName(), competitorsOfFleet);
                    }
                    competitorsOfFleet.put(competitor, competitorsOfFleet.size() + 1);
                }
            }
            competitorRanksPerRaceColumnsAndFleets.put(raceColumnName, competitorsOrderedByFleets);
        }
        JSONArray jsonCompetitorEntries = new JSONArray();
        jsonLeaderboard.put("competitors", jsonCompetitorEntries);
        final int[] regattaRankCounter = new int[] { 1 };
        // Remark: leaderboardDTO.competitors are ordered by total rank
        List<CompetitorDTO> filteredCompetitors = new ArrayList<>();
        final Map<CompetitorDTO, Integer> ranks = new HashMap<>(); // holds ranks, regardless of permission-based filtering
        leaderboardDTO.competitors.forEach(competitor -> {
            ranks.put(competitor, regattaRankCounter[0]++);
            if (SecurityUtils.getSubject()
                    .isPermitted(competitor.getIdentifier()
                            .getStringPermission(SecuredSecurityTypes.PublicReadableActions.READ_PUBLIC))
                    || SecurityUtils.getSubject()
                            .isPermitted(competitor.getIdentifier().getStringPermission(DefaultActions.READ))) {
                // add competitor if all shall be added or else if the competitor ID's string representation was provided
                // in showOnlyActiveRacesForCompetitorIds for the active race selection:
                if (!showOnlyCompetitorsWithIdsProvided || showOnlyActiveRacesForCompetitorIds.contains(competitor.getId().toString())) {
                    filteredCompetitors.add(competitor);
                }
            }
        });
        int competitorCounter = 0;
        for (CompetitorDTO competitor : filteredCompetitors) {
            LeaderboardRowDTO leaderboardRowDTO = leaderboardDTO.rows.get(competitor);
            if (maxCompetitorsCount != null && competitorCounter >= maxCompetitorsCount) {
                break;
            }
            JSONObject jsonCompetitor = new JSONObject();
            writeCompetitorBaseData(jsonCompetitor, competitor, leaderboardDTO, competitorAndBoatIdsOnly);
            BoatDTO rowBoatDTO = leaderboardRowDTO.boat;
            if (rowBoatDTO != null) {
                JSONObject jsonBoat = new JSONObject();
                writeBoatData(jsonBoat, rowBoatDTO, competitorAndBoatIdsOnly);
                jsonCompetitor.put("boat", jsonBoat);
            }
            jsonCompetitor.put("rank", ranks.get(competitor));
            jsonCompetitor.put("carriedPoints", leaderboardRowDTO.carriedPoints);
            jsonCompetitor.put("netPoints", leaderboardRowDTO.netPoints);
            jsonCompetitor.put("overallRank", leaderboardDTO.getTotalRank(competitor));
            jsonCompetitorEntries.add(jsonCompetitor);
            JSONObject jsonRaceColumns = new JSONObject();
            jsonCompetitor.put("columns", jsonRaceColumns);
            for (String raceColumnName : raceColumnsToShow) {
                JSONObject jsonEntry = new JSONObject();
                jsonRaceColumns.put(raceColumnName, jsonEntry);
                LeaderboardEntryDTO leaderboardEntry = leaderboardRowDTO.fieldsByRaceColumnName.get(raceColumnName);
                if (leaderboardEntry != null) {
                    BoatDTO entryBoatDTO = leaderboardEntry.boat;
                    if (entryBoatDTO != null) {
                        JSONObject jsonBoat = new JSONObject();
                        writeBoatData(jsonBoat, entryBoatDTO, competitorAndBoatIdsOnly);
                        jsonEntry.put("boat", jsonBoat);
                    }
                    final FleetDTO fleetOfCompetitor = leaderboardEntry.fleet;
                    jsonEntry.put("fleet", fleetOfCompetitor == null ? "" : fleetOfCompetitor.getName());
                    jsonEntry.put("totalPoints", leaderboardEntry.totalPoints);
                    jsonEntry.put("uncorrectedTotalPoints", leaderboardEntry.totalPoints);
                    jsonEntry.put("netPoints", leaderboardEntry.netPoints);
                    MaxPointsReason maxPointsReason = leaderboardEntry.reasonForMaxPoints;
                    jsonEntry.put("maxPointsReason", maxPointsReason != null ? maxPointsReason.toString() : null);
                    jsonEntry.put("isDiscarded", leaderboardEntry.discarded);
                    jsonEntry.put("isCorrected", leaderboardEntry.hasScoreCorrection());
                    // if we have no fleet information there is no way to know in which fleet the competitor was racing
                    Integer rank = null;
                    if (fleetOfCompetitor != null && fleetOfCompetitor.getName() != null) {
                        Map<String, Map<CompetitorDTO, Integer>> rcMap = competitorRanksPerRaceColumnsAndFleets.get(raceColumnName);
                        if (rcMap != null && !rcMap.isEmpty()) {
                            Map<CompetitorDTO, Integer> rankMap = rcMap.get(fleetOfCompetitor.getName());
                            if (rankMap != null && !rankMap.isEmpty()) {
                                rank = rankMap.get(competitor);
                            }
                        }
                    }
                    jsonEntry.put("rank", rank);
                    LegEntryDTO detailsOfLastAvailableLeg =  getDetailsOfLastAvailableLeg(leaderboardEntry);
                    jsonEntry.put("trackedRank", detailsOfLastAvailableLeg != null ? detailsOfLastAvailableLeg.rank : null);
                    boolean finished = false;
                    LegEntryDTO detailsOfLastCourseLeg = getDetailsOfLastCourseLeg(leaderboardEntry);
                    if (detailsOfLastCourseLeg != null) {
                        finished = detailsOfLastCourseLeg.finished;
                    }
                    jsonEntry.put("finished", finished);
                    if (!raceDetailsToShow.isEmpty() && leaderboardEntry.race != null) {
                        LegEntryDTO currentLegEntry = null;
                        int currentLegNumber = leaderboardEntry.getOneBasedCurrentLegNumber();
                        if (leaderboardEntry.legDetails != null && currentLegNumber > 0 && currentLegNumber <= leaderboardEntry.legDetails.size()) {
                            currentLegEntry = leaderboardEntry.legDetails.get(currentLegNumber-1);
                            if (currentLegEntry != null) {
                                jsonEntry.put("trackedRank", currentLegEntry.rank);
                            }
                        }
                        JSONObject jsonRaceDetails = new JSONObject();
                        jsonEntry.put("data", jsonRaceDetails);
                        for (DetailType type : raceDetailsToShow) {
                            final RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
                            final Competitor c = getService().getCompetitorAndBoatStore().getExistingCompetitorByIdAsString(competitor.getIdAsString());
                            final TrackedRace trackedRace = c == null || raceColumn == null ? null : raceColumn.getTrackedRace(c);
                            Pair<String, Object> valueForRaceDetailType = getValueForRaceDetailType(leaderboard,
                                    type, leaderboardRowDTO, leaderboardEntry, currentLegEntry, trackedRace,
                                    raceColumn, c, leaderboardTimePoint);
                            if (valueForRaceDetailType != null && valueForRaceDetailType.getA() != null && valueForRaceDetailType.getB() != null) {
                                jsonRaceDetails.put(valueForRaceDetailType.getA(), valueForRaceDetailType.getB());
                            }
                        }
                    }
                }
            }
            competitorCounter++;
        }
        return jsonLeaderboard;
    }

    private boolean hasOverallDetail(List<DetailType> raceDetailsToShow) {
        final HashSet<DetailType> availableOverallDetailTypeNames = new HashSet<>(
                Arrays.asList(getAvailableOverallDetailColumnTypes()).stream().collect(Collectors.toSet()));
        // returns true if the set changed, meaning there was an overall detail type in the raceDetailNames
        return availableOverallDetailTypeNames.removeAll(raceDetailsToShow);
    }

    private List<DetailType> calculateRaceDetailTypesToShow(List<String> raceDetailTypesNames) {
        List<DetailType> result = new ArrayList<>();
        if (raceDetailTypesNames.size() == 0) {
            result = Arrays.asList(getDefaultRaceDetailColumnTypes());
        } else if (raceDetailTypesNames.size() == 1 && raceDetailTypesNames.get(0).equals("ALL")) {
            result = Arrays.asList(getAvailableRaceDetailColumnTypes());
        } else {
            for (String raceDetailTypeName : raceDetailTypesNames) {
                DetailType value = DetailType.valueOf(raceDetailTypeName);
                if (!Arrays.asList(getAvailableRaceDetailColumnTypes()).contains(value)) {
                    throw new IllegalArgumentException(raceDetailTypeName + " is not a supported DetailType");
                }
                result.add(value);
            }
        }
        return result;
    }

    private DetailType[] getDefaultRaceDetailColumnTypes() {
        return new DetailType[] { DetailType.RACE_GAP_TO_LEADER_IN_SECONDS,
                DetailType.RACE_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS,
                DetailType.RACE_CURRENT_SPEED_OVER_GROUND_IN_KNOTS,
                DetailType.RACE_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD_IN_METERS, 
                DetailType.RACE_CURRENT_LEG };
    }

    private DetailType[] getAvailableRaceDetailColumnTypes() {
        return new DetailType[] { DetailType.RACE_GAP_TO_LEADER_IN_SECONDS,
                DetailType.PERCENT_TARGET_BOAT_SPEED,
                DetailType.RACE_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS,
                DetailType.RACE_DISTANCE_TRAVELED,
                DetailType.RACE_TIME_TRAVELED,
                DetailType.RACE_IMPLIED_WIND,
                DetailType.RACE_CURRENT_SPEED_OVER_GROUND_IN_KNOTS,
                DetailType.RACE_CURRENT_COURSE_OVER_GROUND_IN_TRUE_DEGREES,
                DetailType.RACE_CURRENT_POSITION_LAT_DEG,
                DetailType.RACE_CURRENT_POSITION_LNG_DEG,
                DetailType.RACE_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD_IN_METERS, 
                DetailType.NUMBER_OF_MANEUVERS,
                DetailType.RACE_CURRENT_LEG,
                DetailType.OVERALL_MAXIMUM_SPEED_OVER_GROUND_IN_KNOTS,
                DetailType.LEG_VELOCITY_MADE_GOOD_IN_KNOTS,
                DetailType.LEG_WINDWARD_DISTANCE_TO_GO_IN_METERS,
                DetailType.LEG_CURRENT_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS,
                DetailType.LEG_CURRENT_SIGNED_CROSS_TRACK_ERROR_IN_METERS,
                DetailType.LEG_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS,
                DetailType.OVERALL_TIME_ON_TIME_FACTOR,
                DetailType.OVERALL_TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE };
    }

    private DetailType[] getAvailableOverallDetailColumnTypes() {
        return new DetailType[] { DetailType.OVERALL_MAXIMUM_SPEED_OVER_GROUND_IN_KNOTS };
    }

    private Pair<String, Object> getValueForRaceDetailType(Leaderboard leaderboard, DetailType type,
            LeaderboardRowDTO leaderboardRowDTO, LeaderboardEntryDTO entry, LegEntryDTO currentLegEntry, TrackedRace trackedRace,
            RaceColumn raceColumn, Competitor competitor, TimePoint timePoint) {
        String name;
        Object value = null;
        Pair<String, Object> result = null;
        if (type.getPremiumAction() == null
                || SecurityUtils.getSubject().isPermitted(SecuredDomainType.LEADERBOARD.getStringPermissionForObject(type.getPremiumAction(), leaderboard))) {
            switch (type) {
                case RACE_GAP_TO_LEADER_IN_SECONDS:
                    name = "gapToLeader-s";
                    if (entry.gapToLeaderInOwnTime != null) {
                        value = entry.gapToLeaderInOwnTime.asSeconds();
                    }
                    break;
                case RACE_DISTANCE_TO_COMPETITOR_FARTHEST_AHEAD_IN_METERS:
                    name = "gapToLeader-m";
                    if (entry.windwardDistanceToCompetitorFarthestAheadInMeters != null) {
                        value = roundDouble(entry.windwardDistanceToCompetitorFarthestAheadInMeters, 2);
                    }
                    break;
                case RACE_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS:
                    name = "averageSpeedOverGround-kts";
                    Distance distance = entry.getDistanceTraveled();
                    Duration timeSailed = entry.getTimeSailed();
                    if (distance != null && timeSailed != null && timeSailed.compareTo(Duration.NULL)>0) {
                        value = roundDouble(distance.inTime(timeSailed).getKnots(), 2);
                    }
                    break;
                case RACE_DISTANCE_TRAVELED:
                    name = "distanceTraveled-m";
                    Distance distanceTraveled = entry.getDistanceTraveled();
                    if (distanceTraveled != null) {
                        value = roundDouble(distanceTraveled.getMeters(), 2);
                    }
                    break;
                case RACE_TIME_TRAVELED:
                    name = "timeTraveled-s";
                    Duration timeTraveled = entry.getTimeSailed();
                    if (timeTraveled != null) {
                        value = timeTraveled.asSeconds();
                    }
                    break;
                case RACE_IMPLIED_WIND:
                    name = "impliedWind-kts";
                    if (entry.impliedWind != null) {
                        value = entry.impliedWind.getKnots();
                    }
                    break;
                case RACE_CURRENT_SPEED_OVER_GROUND_IN_KNOTS:
                    name = "currentSpeedOverGround-kts";
                    if (entry.currentSpeedAndCourseOverGround != null) {
                        value = roundDouble(entry.currentSpeedAndCourseOverGround.getKnots(), 2);
                    }
                    break;
                case RACE_CURRENT_COURSE_OVER_GROUND_IN_TRUE_DEGREES:
                    name = "currentCourseOverGround-deg";
                    if (entry.currentSpeedAndCourseOverGround != null) {
                        value = roundDouble(entry.currentSpeedAndCourseOverGround.getBearing().getDegrees(), 2);
                    }
                    break;
                case RACE_CURRENT_POSITION_LAT_DEG:
                    name = "currentPositionLatitude-deg";
                    if (trackedRace != null) {
                        final GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
                        if (track != null) {
                            final Position position = track.getEstimatedPosition(timePoint, /* extrapolated */ true);
                            if (position != null) {
                                value = roundDouble(position.getLatDeg(), 10);
                            }
                        }
                    }
                    break;
                case RACE_CURRENT_POSITION_LNG_DEG:
                    name = "currentPositionLongitude-deg";
                    if (trackedRace != null) {
                        final GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
                        if (track != null) {
                            final Position position = track.getEstimatedPosition(timePoint, /* extrapolated */ true);
                            if (position != null) {
                                value = roundDouble(position.getLngDeg(), 10);
                            }
                        }
                    }
                    break;
                case NUMBER_OF_MANEUVERS:
                    name = "numberOfManeuvers";
                    Integer numberOfManeuvers = null;
                    Map<ManeuverType, Integer> tacksJibesAndPenalties = getTotalNumberOfTacksJibesAndPenaltyCircles(entry);
                    for (Integer maneuverCount : tacksJibesAndPenalties.values()) {
                        if (maneuverCount != null) {
                            if (numberOfManeuvers == null) {
                                numberOfManeuvers = maneuverCount;
                            } else {
                                numberOfManeuvers += maneuverCount;
                            }
                        }
                    }
                    value = numberOfManeuvers;
                    break;
                case RACE_CURRENT_LEG:
                    name = "currentLeg";
                    int currentLegNumber = entry.getOneBasedCurrentLegNumber();
                    if (currentLegNumber > 0) {
                        value = currentLegNumber;
                    }
                    break;
                case OVERALL_MAXIMUM_SPEED_OVER_GROUND_IN_KNOTS:
                    name = "maxSpeedOverGroundInKnots";
                    value = leaderboardRowDTO.maximumSpeedOverGroundInKnots==null?null:roundDouble(leaderboardRowDTO.maximumSpeedOverGroundInKnots, 2);
                    break;
                case LEG_WINDWARD_DISTANCE_TO_GO_IN_METERS:
                    name = "legWindwardDistanceToGoInMeters";
                    if (currentLegEntry != null && currentLegEntry.windwardDistanceToGoInMeters != null) {
                        value = currentLegEntry.windwardDistanceToGoInMeters;
                    }
                    break;
                case LEG_VELOCITY_MADE_GOOD_IN_KNOTS:
                    name = "legVelocityMadeGoodInKnots";
                    if (currentLegEntry != null && currentLegEntry.velocityMadeGoodInKnots != null) {
                        value = currentLegEntry.velocityMadeGoodInKnots;
                    }
                    break;
                case LEG_CURRENT_ABSOLUTE_CROSS_TRACK_ERROR_IN_METERS:
                    name = "legCurrentAbsoluteCrossTrackErrorInMeters";
                    if (currentLegEntry != null && currentLegEntry.currentOrAverageAbsoluteCrossTrackErrorInMeters != null) {
                        value = currentLegEntry.currentOrAverageAbsoluteCrossTrackErrorInMeters;
                    }
                    break;
                case LEG_CURRENT_SIGNED_CROSS_TRACK_ERROR_IN_METERS:
                    name = "legCurrentSignedCrossTrackErrorInMeters";
                    if (currentLegEntry != null && currentLegEntry.currentOrAverageSignedCrossTrackErrorInMeters != null) {
                        value = currentLegEntry.currentOrAverageSignedCrossTrackErrorInMeters;
                    }
                    break;
                case LEG_AVERAGE_SPEED_OVER_GROUND_IN_KNOTS:
                    name = "currentLegAverageSpeedOverGround-kts";
                    if (currentLegEntry != null && currentLegEntry.averageSpeedOverGroundInKnots != null) {
                        value = currentLegEntry.averageSpeedOverGroundInKnots;
                    }
                    break;
                case OVERALL_TIME_ON_TIME_FACTOR:
                    name = CompetitorJsonConstants.FIELD_TIME_ON_TIME_FACTOR;
                    value = leaderboardRowDTO.effectiveTimeOnTimeFactor;
                    break;
                case OVERALL_TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE:
                    name = CompetitorJsonConstants.FIELD_TIME_ON_DISTANCE_ALLOWANCE_IN_SECONDS_PER_NAUTICAL_MILE;
                    value = leaderboardRowDTO.effectiveTimeOnDistanceAllowancePerNauticalMile == null ? null : leaderboardRowDTO.effectiveTimeOnDistanceAllowancePerNauticalMile.asSeconds();
                    break;
                default:
                    name = null;
                    break;
            }
            if (name != null && value != null) {
                result = new Pair<>(name, value);
            }
        }
        return result;
    }

    private LegEntryDTO getDetailsOfLastCourseLeg(LeaderboardEntryDTO entry) {
        LegEntryDTO lastLegDetail = null;
        if (entry != null && entry.legDetails != null) {
            int lastLegIndex = entry.legDetails.size() - 1;
            if (lastLegIndex >= 0) {
                lastLegDetail = entry.legDetails.get(lastLegIndex);
            }
        }
        return lastLegDetail;
    }

    private LegEntryDTO getDetailsOfLastAvailableLeg(LeaderboardEntryDTO entry) {
        LegEntryDTO lastAvailableLegDetail = null;
        if (entry != null && entry.legDetails != null) {
            for (int i = entry.legDetails.size() - 1; i >= 0; i--) {
                lastAvailableLegDetail = entry.legDetails.get(i);
                if (lastAvailableLegDetail != null) {
                    break;
                }
            }
        }
        return lastAvailableLegDetail;
    }

    private Map<ManeuverType, Integer> getTotalNumberOfTacksJibesAndPenaltyCircles(LeaderboardEntryDTO entry) {
        Map<ManeuverType, Integer> totalNumberOfManeuvers = new HashMap<>();
        for (ManeuverType maneuverType : new ManeuverType[] { ManeuverType.TACK, ManeuverType.JIBE, ManeuverType.PENALTY_CIRCLE }) {
            totalNumberOfManeuvers.put(maneuverType, 0);
        }
        if (entry.legDetails != null) {
            for (LegEntryDTO legDetail : entry.legDetails) {
                if (legDetail != null) {
                    for (ManeuverType maneuverType : new ManeuverType[] { ManeuverType.TACK, ManeuverType.JIBE, ManeuverType.PENALTY_CIRCLE }) {
                        if (legDetail.numberOfManeuvers != null && legDetail.numberOfManeuvers.get(maneuverType) != null) {
                            totalNumberOfManeuvers.put(maneuverType,
                                    totalNumberOfManeuvers.get(maneuverType) + legDetail.numberOfManeuvers.get(maneuverType));
                        }
                    }
                }
            }
        }
        return totalNumberOfManeuvers;
    }

    /**
     * If {@code raceColumnNames} is empty or {@code null}, return the names of all {@link raceColumnsOfLeaderboard};
     * otherwise return those race column names from {@code raceColumnsOfLeaderboard} that are also in
     * {@code raceColumnNames}.
     * @param showOnlyActiveRacesForCompetitorIds
     *            {@code null}, or specifies zero or more competitor IDs, requesting that the result contained the race
     *            column holding that competitor's "active" race at the time requested; see also the
     *            {@code resultTimePoint} parameter. A race is active if it is currently live or is the last race for
     *            which the competitor has a result. In case multiple competitor IDs are given, all race columns that
     *            contain an active race of any of the competitors specified will be added to the result. It is possible
     *            that no such race is found, e.g., because racing has not started yet and the leaderboard is empty. No
     *            race column will be delivered then. In case the {@code raceColumnNames} parameter is used, too, the
     *            combined set of race columns from the columnNames parameter and this parameter will be delivered.
     * @param resultTimePoint
     *            used together with {@code showOnlyActiveRacesForCompetitorIds} to determine which races are "active"
     *            for the competitors whose IDs are specified in {@link showOnlyActiveRacesForCompetitorIds} at that
     *            time
     */
    private List<String> calculateRaceColumnsToShow(Leaderboard leaderboard,
            List<String> raceColumnNames, Iterable<String> showOnlyActiveRacesForCompetitorIds,
            TimePoint resultTimePoint) {
        final Set<String> raceColumnNamesAsSet = new HashSet<>();
        if (raceColumnNames != null) {
            raceColumnNamesAsSet.addAll(raceColumnNames);
        }
        if (showOnlyActiveRacesForCompetitorIds != null) {
            Util.addAll(getRaceColumnNamesOfActiveRaceColumnsForCompetitorIds(showOnlyActiveRacesForCompetitorIds,
                    leaderboard, resultTimePoint), raceColumnNamesAsSet);
        }
        // Calculates the race columns to retrieve data for
        final List<String> raceColumnsToShow = new ArrayList<>();
        for (final RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            // if showOnlyActiveRacesForCompetitorIds specifies at least one ID, don't default to delivering
            // all columns, even if raceColumnNames is empty, but only show what resulted from finding the
            // active races combined with columns explicitly specified in raceColumnNames.
            
            if ((raceColumnNamesAsSet.isEmpty() && (showOnlyActiveRacesForCompetitorIds==null || Util.isEmpty(showOnlyActiveRacesForCompetitorIds)))
                    || raceColumnNamesAsSet.contains(raceColumn.getName())) {
                raceColumnsToShow.add(raceColumn.getName());
            }
        }
        return raceColumnsToShow;
    }

    private Iterable<String> getRaceColumnNamesOfActiveRaceColumnsForCompetitorIds(
            Iterable<String> showOnlyActiveRacesForCompetitorIds, Leaderboard leaderboard, TimePoint timePoint) {
        final Set<String> result = new HashSet<>();
        for (final String showOnlyActiveRacesForCompetitorId : showOnlyActiveRacesForCompetitorIds) {
            final String nameOfActiveRaceColumnForCompetitor = getNameOfActiveRaceColumnForCompetitor(showOnlyActiveRacesForCompetitorId, leaderboard, timePoint);
            if (nameOfActiveRaceColumnForCompetitor != null) {
                result.add(nameOfActiveRaceColumnForCompetitor);
            }
        }
        return result;
    }

    private String getNameOfActiveRaceColumnForCompetitor(String showOnlyActiveRacesForCompetitorId, Leaderboard leaderboard, TimePoint timePoint) {
        final String result;
        final TimePoint timePointOrLiveTimeIfNull = timePoint != null ? timePoint : leaderboard.getNowMinusDelay();
        final Competitor competitor = leaderboard.getCompetitorByIdAsString(showOnlyActiveRacesForCompetitorId);
        if (competitor != null) {
            RaceColumn lastRaceColumnWithResultForCompetitorAtTimePoint = null;
            for (final RaceColumn raceColumn : leaderboard.getRaceColumns()) {
                if (leaderboard.getTotalPoints(competitor, raceColumn, timePointOrLiveTimeIfNull) != null) {
                    lastRaceColumnWithResultForCompetitorAtTimePoint = raceColumn;
                }
            }
            if (lastRaceColumnWithResultForCompetitorAtTimePoint != null) {
                result = lastRaceColumnWithResultForCompetitorAtTimePoint.getName();
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }
}
