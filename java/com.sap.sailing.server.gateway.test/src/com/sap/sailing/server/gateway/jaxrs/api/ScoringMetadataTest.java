package com.sap.sailing.server.gateway.jaxrs.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.RegattaLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.server.gateway.jaxrs.api.AbstractLeaderboardsResource.ResultStates;
import com.sap.sailing.server.gateway.serialization.impl.ColorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.FleetJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.RegattaJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.SeriesJsonSerializer;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.rest.StreamingOutputUtil;

public class ScoringMetadataTest {
    @BeforeAll
    public static void requireAssertions() {
        assertTrue(RegattaImpl.class.desiredAssertionStatus(), "Run scoring metadata tests with -ea");
    }

    @Test
    public void testFlexibleLowPointMedalDefaultAndOverride() throws Exception {
        assertFlexibleMetadata(ScoringSchemeType.LOW_POINT, /* expectedMedalFactor */ 2.0);
    }

    @Test
    public void testFlexibleHighPointMedalDefaultAndOverride() throws Exception {
        assertFlexibleMetadata(ScoringSchemeType.HIGH_POINT, /* expectedMedalFactor */ 2.0);
    }

    @Test
    public void testFirstToTwoMedalDefaultAndOverride() throws Exception {
        assertFlexibleMetadata(ScoringSchemeType.LOW_POINT_FIRST_TO_WIN_TWO_RACES, /* expectedMedalFactor */ 1.0);
    }

    private void assertFlexibleMetadata(final ScoringSchemeType scheme, final double expectedMedalFactor) throws Exception {
        final FlexibleLeaderboardImpl leaderboard = new FlexibleLeaderboardImpl("Flexible",
                new ThresholdBasedResultDiscardingRuleImpl(new int[] { 5 }),
                DomainFactory.INSTANCE.createScoringScheme(scheme), /* courseArea */ null);
        leaderboard.addRaceColumn("R1", /* medalRace */ false);
        final RaceColumn medal = leaderboard.addRaceColumn("M", /* medalRace */ true);
        assertLeaderboardResponses(leaderboard, expectedMedalFactor, /* explicitFactor */ null,
                /* oneAlwaysStaysOne */ false);
        medal.setFactor(0.5);
        assertLeaderboardResponses(leaderboard, /* expectedMedalFactor */ 0.5, /* explicitFactor */ 0.5,
                /* oneAlwaysStaysOne */ false);
    }

    private void assertLeaderboardResponses(final Leaderboard leaderboard, final double expectedMedalFactor,
            final Double explicitFactor, final boolean oneAlwaysStaysOne) throws Exception {
        final RacingEventService service = mock(RacingEventService.class);
        when(service.getLeaderboardByName(leaderboard.getName())).thenReturn(leaderboard);
        when(service.getBaseDomainFactory()).thenReturn(DomainFactory.INSTANCE);
        when(service.skipChecksDueToCorrectSecret(leaderboard.getName(), "secret")).thenReturn(/* value */ true);
        final LeaderboardsResource v1 = spy(new LeaderboardsResource());
        final LeaderboardsResourceV2 v2 = spy(new LeaderboardsResourceV2());
        doReturn(service).when(v1).getService();
        doReturn(service).when(v2).getService();
        // Exercise both endpoint versions and the distinct live/empty-final response paths.
        for (final ResultStates state : ResultStates.values()) {
            final Response responseV1 = v1.getLeaderboard(leaderboard.getName(), state,
                    /* maxCompetitorsCount */ null, "secret", /* competitorAndBoatIdsOnly */ false);
            final Response responseV2 = v2.getLeaderboard(leaderboard.getName(), state,
                    Collections.emptyList(), Collections.emptyList(), /* time */ null, /* timeasmillis */ null,
                    /* maxCompetitorsCount */ null, "secret", /* competitorAndBoatIdsOnly */ false,
                    Collections.emptyList(), /* showOnlyCompetitorsWithIdsProvided */ false);
            for (final Response response : Arrays.asList(responseV1, responseV2)) {
                final String body = StreamingOutputUtil.getEntityAsString(response.getEntity());
                assertEquals(/* expected */ 200, response.getStatus(), body);
                final JSONObject json = (JSONObject) JSONValue.parse(body);
                assertEquals(leaderboard.getScoringScheme().getType().name(), json.get("scoringScheme"));
                assertEquals(Arrays.asList("R1", "M"), json.get("columnNames"));
                final JSONArray columns = (JSONArray) json.get("trackedRacesInfo");
                final JSONObject ordinary = (JSONObject) columns.get(/* index */ 0);
                assertEquals(Boolean.FALSE, ordinary.get("isMedalRace"));
                assertEquals(/* expected */ 1.0, ordinary.get("factor"));
                final JSONObject medal = (JSONObject) columns.get(/* index */ 1);
                assertEquals("M", medal.get("raceColumnName"));
                assertEquals(Boolean.TRUE, medal.get("isMedalRace"));
                assertEquals(expectedMedalFactor, medal.get("factor"));
                assertTrue(medal.containsKey("explicitFactor"));
                assertEquals(explicitFactor, medal.get("explicitFactor"));
                assertEquals(oneAlwaysStaysOne, medal.get("oneAlwaysStaysOne"));
                assertEquals(state.name(), json.get("resultState"));
            }
        }
    }

    @Test
    public void testRegattaSeriesConfigurationAndLocalDiscardStates() throws Exception {
        final Series ordinary = new SeriesImpl("Opening", /* isMedal */ false, /* isFleetsCanRunInParallel */ true,
                Collections.singleton(new FleetImpl("Default")), Collections.singleton("R1"),
                /* trackedRegattaRegistry */ null);
        final Series medal = new SeriesImpl("Final", /* isMedal */ true, /* isFleetsCanRunInParallel */ true,
                Arrays.asList(new FleetImpl("Gold"), new FleetImpl("Silver")), Collections.singleton("M"),
                /* trackedRegattaRegistry */ null);
        medal.setOneAlwaysStaysOne(true);
        medal.setCrossFleetMergedRanking(true);
        medal.getRaceColumnByName("M").setFactor(0.5);
        final Regatta regatta = new RegattaImpl("Regatta", /* boatClass */ null,
                /* canBoatsOfCompetitorsChangePerRace */ false, CompetitorRegistrationType.CLOSED,
                /* startDate */ null, /* endDate */ null, Arrays.asList(ordinary, medal), /* persistent */ false,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), UUID.randomUUID(),
                /* courseArea */ null, OneDesignRankingMetric::new, /* registrationLinkSecret */ null);
        final SeriesJsonSerializer seriesSerializer = new SeriesJsonSerializer(
                new FleetJsonSerializer(new ColorJsonSerializer()), mock(RaceLogResolver.class));
        final RegattaJsonSerializer serializer = new RegattaJsonSerializer(seriesSerializer,
                /* competitorSerializer */ null, /* boatSerializer */ null, /* securityService */ null);
        final JSONObject json = serializer.serialize(regatta);
        assertEquals("LOW_POINT", json.get("scoringSystem"));
        final JSONObject series = (JSONObject) ((JSONArray) json.get("series")).get(/* index */ 1);
        assertTrue(series.containsKey("discardThresholds"));
        assertNull(series.get("discardThresholds"));
        assertEquals(Boolean.TRUE, series.get("oneAlwaysStaysOne"));
        assertEquals(Boolean.TRUE, series.get("hasCrossFleetMergedRanking"));
        final JSONArray fleets = (JSONArray) ((JSONObject) series.get("trackedRaces")).get("fleets");
        for (final Object fleet : fleets) {
            final JSONObject column = (JSONObject) ((JSONArray) ((JSONObject) fleet).get("races")).get(/* index */ 0);
            assertEquals(Boolean.TRUE, column.get("isMedalRace"));
            assertEquals(/* expected */ 0.5, column.get("factor"));
            assertEquals(/* expected */ 0.5, column.get("explicitFactor"));
            assertEquals(Boolean.TRUE, column.get("oneAlwaysStaysOne"));
        }
        medal.setResultDiscardingRule(new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
        assertEquals(Collections.emptyList(), seriesSerializer.serialize(medal).get("discardThresholds"));
        ordinary.setResultDiscardingRule(new ThresholdBasedResultDiscardingRuleImpl(new int[] { 1, 5 }));
        assertEquals(Arrays.asList(/* a */ 1, /* a */ 5), seriesSerializer.serialize(ordinary).get("discardThresholds"));
        final Leaderboard leaderboard = new RegattaLeaderboardImpl(regatta,
                new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
        assertLeaderboardResponses(leaderboard, /* expectedMedalFactor */ 0.5, /* explicitFactor */ 0.5,
                /* oneAlwaysStaysOne */ true);
    }
}
