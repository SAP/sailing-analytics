package com.sap.sailing.domain.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CompetitorWithBoatImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.LeaderboardType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.RegattaLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Offline, corrected-score replay. Exported points are weighted; score corrections take unweighted points.
 * Expected totals, ranks and discard flags are deliberately never used to construct the leaderboard.
 * Fleet membership is registered on race logs without fabricating GPS tracks or uncorrected finish ranks.
 */
final class ProductionScoringFixture {
    final JSONObject data;
    final Leaderboard leaderboard;
    final TimePoint timePoint;
    final Map<String, Competitor> competitors = new LinkedHashMap<>();
    final List<ProductionScoringFixture> children = new ArrayList<>();
    private final Map<String, Double> factors = new LinkedHashMap<>();
    private final Map<String, Double> offsets = new LinkedHashMap<>();

    ProductionScoringFixture(final String resource, final boolean reverseInsertionOrder) throws Exception {
        this(resource, reverseInsertionOrder, new LinkedHashMap<>());
    }

    private ProductionScoringFixture(final String resource, final boolean reverseInsertionOrder,
            final Map<String, Competitor> competitorPool) throws Exception {
        data = read(resource);
        timePoint = new MillisecondsTimePoint(((Number) data.get("timePoint")).longValue());
        leaderboard = createLeaderboard(reverseInsertionOrder, competitorPool);
        leaderboard.setDisplayName((String) data.get("displayName"));
        for (final JSONObject definition : objects(data, "columns")) {
            final RaceColumn column = leaderboard.getRaceColumnByName((String) definition.get("name"));
            factors.put(column.getName(), ((Number) definition.get("factor")).doubleValue());
            offsets.put(column.getName(), (boolean) definition.get("oneAlwaysStaysOne") ? 1.0 : 0.0);
            final Number explicitFactor = (Number) definition.get("explicitFactor");
            if (explicitFactor != null) {
                column.setFactor(explicitFactor.doubleValue());
            }
        }
        final List<JSONObject> rows = objects(data, "competitors");
        if (reverseInsertionOrder) {
            Collections.reverse(rows);
        }
        for (final JSONObject row : rows) {
            final DynamicBoat boat = new BoatImpl(row.get("id").toString(), (String) row.get("name"),
                    DomainFactory.INSTANCE.getOrCreateBoatClass("Fixture", /* typicallyStartsUpwind */ true), (String) row.get("sailID"));
            final Competitor competitor = competitorPool.computeIfAbsent(row.get("id").toString(), (final String id) ->
                    (boolean) data.get("canBoatsOfCompetitorsChangePerRace")
                    ? new CompetitorImpl(row.get("id").toString(), (String) row.get("name"), /* shortName */ null,
                            /* color */ null, /* email */ null, /* flagImageURL */ null, /* team */ null,
                            /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, /* searchTag */ null)
                    : new CompetitorWithBoatImpl(row.get("id").toString(), (String) row.get("name"), /* shortName */ null,
                            /* color */ null, /* email */ null, /* flagImageURL */ null, /* team */ null,
                            /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, /* searchTag */ null, boat));
            competitors.put((String) row.get("id"), competitor);
            leaderboard.setDisplayName(competitor, (String) row.get("displayName"));
            loadCells(row, competitor, boat);
        }
        if (data.containsKey("suppressedIds")) {
            for (final Object id : (JSONArray) data.get("suppressedIds")) {
                leaderboard.setSuppressed(competitorPool.get(id), /* suppressed */ true);
            }
        }
    }

    private Leaderboard createLeaderboard(final boolean reverseInsertionOrder,
            final Map<String, Competitor> competitorPool) throws Exception {
        final Leaderboard result;
        final ThresholdBasedResultDiscardingRuleImpl discards =
                new ThresholdBasedResultDiscardingRuleImpl(thresholds(data.get("discardThresholds")));
        if (data.containsKey("children")) {
            final List<Leaderboard> childBoards = new ArrayList<>();
            for (final JSONObject child : objects(data, "children")) {
                final ProductionScoringFixture fixture = new ProductionScoringFixture((String) child.get("resource"),
                        reverseInsertionOrder, competitorPool);
                children.add(fixture);
                childBoards.add(fixture.leaderboard);
            }
            final String groupName = (String) data.get("groupName");
            result = new LeaderboardGroupMetaLeaderboard(new LeaderboardGroupImpl(groupName, groupName,
                    /* displayName */ null, /* displayGroupsInReverseOrder */ false, childBoards),
                    DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.valueOf((String) data.get("scheme"))), discards);
        } else if (LeaderboardType.FlexibleLeaderboard.name().equals(data.get("type"))) {
            final FlexibleLeaderboardImpl flexible = new FlexibleLeaderboardImpl((String) data.get("name"), discards,
                    DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.valueOf((String) data.get("scheme"))),
                    /* courseArea */ null);
            for (final JSONObject definition : objects(data, "columns")) {
                flexible.addRaceColumn((String) definition.get("name"), (boolean) definition.get("medal"));
            }
            result = flexible;
        } else {
            final List<Series> series = new ArrayList<>();
            for (final JSONObject definition : objects(data, "series")) {
                series.add(createSeries(definition));
            }
            final String name = (String) data.get("name");
            final RegattaImpl regatta = new RegattaImpl(name,
                    DomainFactory.INSTANCE.getOrCreateBoatClass("Fixture", /* typicallyStartsUpwind */ true),
                    (boolean) data.get("canBoatsOfCompetitorsChangePerRace"), CompetitorRegistrationType.CLOSED,
                    /* startDate */ null, /* endDate */ null, series, /* persistent */ false,
                    DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.valueOf((String) data.get("scheme"))),
                    name, /* courseArea */ null, OneDesignRankingMetric::new, /* registrationLinkSecret */ null);
            result = new RegattaLeaderboardImpl(regatta, discards);
        }
        return result;
    }

    private Series createSeries(final JSONObject definition) {
        final List<Fleet> fleets = new ArrayList<>();
        for (final JSONObject fleet : objects(definition, "fleets")) {
            fleets.add(new FleetImpl((String) fleet.get("name"), ((Number) fleet.get("ordering")).intValue()));
        }
        final List<String> columns = new ArrayList<>();
        for (final Object column : (JSONArray) definition.get("columns")) {
            columns.add((String) column);
        }
        final Series result = new SeriesImpl((String) definition.get("name"), (boolean) definition.get("medal"),
                (boolean) definition.get("parallel"), fleets, columns, /* trackedRegattaRegistry */ null);
        result.setStartsWithZeroScore((boolean) definition.get("reset"));
        result.setFirstColumnIsNonDiscardableCarryForward((boolean) definition.get("carry"));
        result.setSplitFleetContiguousScoring((boolean) definition.get("contiguous"));
        if (definition.containsKey("crossFleetMergedRanking")) {
            result.setCrossFleetMergedRanking((boolean) definition.get("crossFleetMergedRanking"));
        }
        if (definition.containsKey("oneAlwaysStaysOne")) {
            result.setOneAlwaysStaysOne((boolean) definition.get("oneAlwaysStaysOne"));
        }
        final Number maximumDiscards = (Number) definition.get("maximumDiscards");
        if (maximumDiscards != null) {
            result.setMaximumNumberOfDiscards(maximumDiscards.intValue());
        }
        if (definition.containsKey("discardThresholds")) {
            result.setResultDiscardingRule(new ThresholdBasedResultDiscardingRuleImpl(thresholds(definition.get("discardThresholds"))));
        }
        for (final JSONObject column : objects(data, "columns")) {
            if (columns.contains(column.get("name"))) {
                result.setOneAlwaysStaysOne((boolean) column.get("oneAlwaysStaysOne"));
            }
        }
        return result;
    }

    private void loadCells(final JSONObject row, final Competitor competitor, final Boat boat) throws Exception {
        for (final JSONObject cell : objects(row, "cells")) {
            final RaceColumn column = leaderboard.getRaceColumnByName((String) cell.get("column"));
            final String fleetName = (String) cell.get("fleet");
            if (!data.containsKey("children") && fleetName != null && !fleetName.isEmpty()) {
                final Fleet fleet = column.getFleetByName(fleetName);
                if (fleet == null) {
                    throw new IllegalArgumentException("Unknown fleet " + fleetName + " in " + column.getName());
                }
                column.enableCompetitorRegistrationOnRaceLog(fleet);
                if ((boolean) data.get("canBoatsOfCompetitorsChangePerRace")) {
                    column.registerCompetitor(competitor, boat, fleet);
                } else {
                    column.registerCompetitor((CompetitorWithBoat) competitor, fleet);
                }
            }
            final Number points = (Number) cell.get("points");
            if (points != null && (!data.containsKey("children") || (boolean) cell.get("isCorrected"))) {
                // Invert only the export's column weighting, independently of the scoring implementation.
                final double offset = offsets.get(column.getName());
                final double unweightedPoints = (points.doubleValue() - offset) / factors.get(column.getName()) + offset;
                leaderboard.getScoreCorrection().correctScore(competitor, column, unweightedPoints);
                leaderboard.getScoreCorrection().setMaxPointsReason(competitor, column,
                        MaxPointsReason.valueOf((String) cell.get("reason")));
            }
        }
    }

    static JSONObject read(final String resource) throws IOException, ParseException {
        final InputStream input = ProductionScoringFixture.class.getResourceAsStream("/scoring/" + resource + ".json");
        if (input == null) {
            throw new IOException("Missing scoring fixture: " + resource);
        }
        final JSONObject result;
        try (final InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            result = (JSONObject) new JSONParser().parse(reader);
        }
        return result;
    }

    static List<JSONObject> objects(final JSONObject object, final String key) {
        final List<JSONObject> result = new ArrayList<>();
        for (final Object value : (JSONArray) object.get(key)) {
            result.add((JSONObject) value);
        }
        return result;
    }

    private static int[] thresholds(final Object value) {
        final JSONArray array = (JSONArray) value;
        final int[] result = new int[array == null ? 0 : array.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = ((Number) array.get(i)).intValue();
        }
        return result;
    }
}
