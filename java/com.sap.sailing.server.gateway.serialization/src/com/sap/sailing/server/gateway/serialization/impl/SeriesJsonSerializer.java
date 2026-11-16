package com.sap.sailing.server.gateway.serialization.impl;

import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.ReadonlyRaceStateImpl;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonSerializer;

public class SeriesJsonSerializer implements JsonSerializer<Series> {
    public static final String FIELD_NAME = "name";
    public static final String FIELD_FLEETS = "fleets";
    public static final String FIELD_RACES = "races"; // TODO: 'races' is wrong here... it's actually 'raceColumns'
    public static final String FIELD_TRACKED_RACES = "trackedRaces";
    public static final String FIELD_STARTS_WITH_ZERO_SCORE = "startsWithZeroScore";
    public static final String FIELD_HAS_SPLIT_FLEET_CONTIGUOUS_SCORING = "hasSplitFleetContiguousScoring";
    public static final String FIELD_IS_MEDAL_SERIES = "isMedalSeries";
    public static final String FIELD_FLEETS_CAN_RUN_IN_PARALLEL = "fleetsCanRunInParallel";
    public static final String FIELD_MAXIMUM_NUMBER_OF_DISCARDS = "maximumNumberOfDiscards";
    public static final String FIELD_DISCARD_THRESHOLDS = "discardThresholds";
    public static final String FIELD_ONE_ALWAYS_STAYS_ONE = "oneAlwaysStaysOne";
    public static final String FIELD_HAS_CROSS_FLEET_MERGED_RANKING = "hasCrossFleetMergedRanking";
    public static final String FIELD_FIRST_COLUMS_IS_NON_DISCARDABLE_CARRY_FORWARD = "firstColumnIsNonDiscardableCarryForward";

    private final JsonSerializer<Fleet> fleetSerializer;
    private final RaceLogResolver raceLogResolver;

    public SeriesJsonSerializer(JsonSerializer<Fleet> fleetSerializer, RaceLogResolver raceLogResolver) {
        this.fleetSerializer = fleetSerializer;
        this.raceLogResolver = raceLogResolver;
    }

    public JSONObject serialize(final Series series) {
        final JSONObject result = new JSONObject();
        
        result.put(FIELD_NAME, series.getName());
        result.put(FIELD_IS_MEDAL_SERIES, series.isMedal());
        result.put(FIELD_FLEETS_CAN_RUN_IN_PARALLEL, series.isFleetsCanRunInParallel());
        result.put(FIELD_STARTS_WITH_ZERO_SCORE, series.isStartsWithZeroScore());
        result.put(FIELD_HAS_SPLIT_FLEET_CONTIGUOUS_SCORING, series.hasSplitFleetContiguousScoring());
        result.put(FIELD_MAXIMUM_NUMBER_OF_DISCARDS, series.getMaximumNumberOfDiscards());
        result.put(FIELD_ONE_ALWAYS_STAYS_ONE, series.isOneAlwaysStaysOne());
        result.put(FIELD_HAS_CROSS_FLEET_MERGED_RANKING, series.hasCrossFleetMergedRanking());
        final ThresholdBasedResultDiscardingRule discardingRule = series.getResultDiscardingRule();
        final JSONArray discardThresholds;
        if (discardingRule == null) {
            discardThresholds = null;
        } else {
            discardThresholds = new JSONArray();
            for (final int threshold : discardingRule.getDiscardIndexResultsStartingWithHowManyRaces()) {
                discardThresholds.add(threshold);
            }
        }
        result.put(FIELD_DISCARD_THRESHOLDS, discardThresholds);
        result.put(FIELD_FIRST_COLUMS_IS_NON_DISCARDABLE_CARRY_FORWARD, series.isFirstColumnNonDiscardableCarryForward());
        JSONArray fleetsJson = new JSONArray();
        for (Fleet fleet : series.getFleets()) {
            fleetsJson.add(fleetSerializer.serialize(fleet));
        }
        result.put(FIELD_FLEETS, fleetsJson);
        JSONArray racesJson = new JSONArray();
        for (RaceColumn raceColumn : series.getRaceColumns()) {
            racesJson.add(raceColumn.getName());
        }
        result.put(FIELD_RACES, racesJson);
        JSONObject trackedRacesJson = new JSONObject();
        JSONArray trackedFleetsJson = new JSONArray();
        for (Fleet fleet : series.getFleets()) {
            JSONObject trackedFleetJson = new JSONObject();
            trackedFleetJson.put(FIELD_NAME, fleet.getName());
            JSONArray racesPerFleetJson = new JSONArray();
            for (RaceColumn raceColumn : series.getRaceColumns()) {
                final RaceLog raceLog = raceColumn.getRaceLog(fleet);
                final UUID courseAreaId;
                if (raceLog != null) {
                    final ReadonlyRaceState raceState = ReadonlyRaceStateImpl.getOrCreate(raceLogResolver, raceLog);
                    courseAreaId = raceState.getCourseAreaId();
                } else {
                    courseAreaId = null;
                }
                TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                JSONObject raceColumnJson = new JSONObject();
                raceColumnJson.put(FIELD_NAME, raceColumn.getName());
                raceColumnJson.put("isMedalRace" , raceColumn.isMedalRace());
                raceColumnJson.put("factor", series.getRegatta().getScoringScheme().getScoreFactor(raceColumn));
                raceColumnJson.put("explicitFactor", raceColumn.getExplicitFactor());
                raceColumnJson.put(FIELD_ONE_ALWAYS_STAYS_ONE, raceColumn.isOneAlwaysStaysOne());
                raceColumnJson.put("courseAreaId", courseAreaId==null?null:courseAreaId.toString());
                if (trackedRace != null) {
                    raceColumnJson.put("isLive", trackedRace.isLive(MillisecondsTimePoint.now()));
                    raceColumnJson.put("isTracked", true);
                    raceColumnJson.put("trackedRaceName", trackedRace.getRace().getName());
                    raceColumnJson.put("raceId", trackedRace.getRace().getId().toString());
                    raceColumnJson.put("hasGpsData", trackedRace.hasGPSData());
                    raceColumnJson.put("hasWindData", trackedRace.hasWindData());
                } else {
                    raceColumnJson.put("isLive", false);
                    raceColumnJson.put("isTracked", false);
                    raceColumnJson.put("trackedRaceName", null);
                    raceColumnJson.put("hasGpsData", false);
                    raceColumnJson.put("hasWindData", false);
                }
                racesPerFleetJson.add(raceColumnJson);
            }
            trackedFleetJson.put(FIELD_RACES, racesPerFleetJson);
            trackedFleetsJson.add(trackedFleetJson);
        }
        trackedRacesJson.put(FIELD_FLEETS, trackedFleetsJson);        
        result.put(FIELD_TRACKED_RACES, trackedRacesJson);        
        return result;
    }
}
