package com.sap.sailing.server.gateway.test.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.ConfirmedFinishPositioningListFinder;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.racelog.AuthorPriority;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithStartTimeAndRanks;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.rest.StreamingOutputUtil;

/**
 * Tests {@code POST /v1/leaderboards/{name}/finishpositions}, which enters a race's results as the race committee
 * app's confirmed finish list.
 */
public class LeaderboardsResourceFinishPositionsTest extends AbstractJaxRsApiTest {
    private static final String RACE_COLUMN_NAME = "F1";
    private static final String FLEET1_NAME = "Fleet1";
    private static final String FLEET2_NAME = "Fleet2";
    private Regatta regatta;
    private RegattaLeaderboard regattaLeaderboard;
    private RaceColumn raceColumn;
    private List<Competitor> fleet1Competitors;
    private List<Competitor> fleet2Competitors;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        final List<Fleet> fleets = new ArrayList<>();
        fleets.add(new FleetImpl(FLEET1_NAME));
        fleets.add(new FleetImpl(FLEET2_NAME));
        final Series series = new SeriesImpl("Flights", /* isMedal */ false, /* isFleetsCanRunInParallel */ true,
                fleets, new ArrayList<String>(), /* trackedRegattaRegistry */ null);
        regatta = racingEventService.createRegatta(RegattaImpl.getDefaultName("League", "J/70"), "J/70",
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */ null, /* endDate */ null, UUID.randomUUID(),
                Collections.singletonList(series), /* persistent */ true,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                /* course area ID */ (Serializable) null, /* buoyZoneRadiusInHullLengths */ 2.0,
                /* useStartTimeInference */ true, /* controlTrackingFromStartAndFinishTimes */ false,
                /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
        series.addRaceColumn(RACE_COLUMN_NAME, /* trackedRegattaRegistry */ null);
        raceColumn = series.getRaceColumnByName(RACE_COLUMN_NAME);
        final List<Competitor> competitors = createStoredCompetitors(6);
        fleet1Competitors = competitors.subList(0, 3);
        fleet2Competitors = competitors.subList(3, 6);
        final TimePoint now = MillisecondsTimePoint.now();
        raceColumn.setTrackedRace(raceColumn.getFleetByName(FLEET1_NAME),
                new MockedTrackedRaceWithStartTimeAndRanks(now, fleet1Competitors, regatta));
        raceColumn.setTrackedRace(raceColumn.getFleetByName(FLEET2_NAME),
                new MockedTrackedRaceWithStartTimeAndRanks(now, fleet2Competitors, regatta));
        regattaLeaderboard = racingEventService.addRegattaLeaderboard(regatta.getRegattaIdentifier(), "League",
                new int[0]);
    }

    private List<Competitor> createStoredCompetitors(final int numberOfCompetitors) {
        final List<Competitor> result = new ArrayList<>();
        for (int i = 1; i <= numberOfCompetitors; i++) {
            final String competitorName = "C" + i;
            result.add(racingEventService.getBaseDomainFactory().getCompetitorAndBoatStore().getOrCreateCompetitor(
                    UUID.randomUUID(), competitorName, "c" + i, Color.RED, /* email */ null,
                    /* flagImageURI */ null,
                    new TeamImpl("Team " + competitorName, Collections.singleton(new PersonImpl(competitorName,
                            new NationalityImpl("GER"), /* dateOfBirth */ null, competitorName)),
                            /* coach */ null),
                    /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null,
                    /* searchTag */ null, /* storePersistently */ true));
        }
        return result;
    }

    private JSONObject finishPosition(final Competitor competitor, final Integer rank, final String maxPointsReason) {
        final JSONObject result = new JSONObject();
        result.put("competitorId", competitor.getId().toString());
        if (rank != null) {
            result.put("rank", rank);
        }
        if (maxPointsReason != null) {
            result.put("maxPointsReason", maxPointsReason);
        }
        return result;
    }

    private Response post(final String fleetName, final JSONObject... finishPositions) {
        final JSONArray body = new JSONArray();
        for (final JSONObject finishPosition : finishPositions) {
            body.add(finishPosition);
        }
        return leaderboardsResource.setFinishPositions(regattaLeaderboard.getName(), RACE_COLUMN_NAME, fleetName,
                body.toJSONString());
    }

    private RaceLog getRaceLog(final String fleetName) {
        return raceColumn.getRaceLog(raceColumn.getFleetByName(fleetName));
    }

    /**
     * @return the confirmed competitor results in the race log of {@code fleetName} by competitor ID, or {@code null}
     *         if there are none; the finder merges all confirmed lists of the pass, the latest result per competitor
     *         winning, in no particular order
     */
    private Map<Serializable, CompetitorResult> getConfirmedResults(final String fleetName) {
        final CompetitorResults competitorResults = new ConfirmedFinishPositioningListFinder(getRaceLog(fleetName))
                .analyze().getCompetitorResults();
        final Map<Serializable, CompetitorResult> result;
        if (competitorResults == null) {
            result = null;
        } else {
            result = new HashMap<>();
            for (final CompetitorResult competitorResult : competitorResults) {
                result.put(competitorResult.getCompetitorId(), competitorResult);
            }
        }
        return result;
    }

    @Test
    public void testEnterRanksAndPenalty() throws Exception {
        final Response response = post(FLEET1_NAME, finishPosition(fleet1Competitors.get(1), 1, null),
                finishPosition(fleet1Competitors.get(2), 2, "NONE"),
                finishPosition(fleet1Competitors.get(0), null, "DSQ"));
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        final JSONObject json = (JSONObject) JSONValue.parse(StreamingOutputUtil.getEntityAsString(response.getEntity()));
        final RaceLog raceLog = getRaceLog(FLEET1_NAME);
        final Map<Serializable, CompetitorResult> confirmed = getConfirmedResults(FLEET1_NAME);
        assertNotNull(confirmed);
        assertEquals(3, confirmed.size());
        final CompetitorResult first = confirmed.get(fleet1Competitors.get(1).getId());
        assertEquals(fleet1Competitors.get(1).getName(), first.getName());
        assertEquals(1, first.getOneBasedRank());
        assertEquals(MaxPointsReason.NONE, first.getMaxPointsReason());
        assertNull(first.getScore());
        assertEquals(CompetitorResult.MergeState.OK, first.getMergeState());
        final CompetitorResult disqualified = confirmed.get(fleet1Competitors.get(0).getId());
        assertEquals(0, disqualified.getOneBasedRank());
        assertEquals(MaxPointsReason.DSQ, disqualified.getMaxPointsReason());
        RaceLogFinishPositioningConfirmedEvent event = null;
        raceLog.lockForRead();
        try {
            for (final RaceLogEvent raceLogEvent : raceLog.getUnrevokedEvents()) {
                if (raceLogEvent instanceof RaceLogFinishPositioningConfirmedEvent) {
                    event = (RaceLogFinishPositioningConfirmedEvent) raceLogEvent;
                }
            }
        } finally {
            raceLog.unlockAfterRead();
        }
        assertNotNull(event);
        assertEquals(AuthorPriority.OFFICER_ON_VESSEL.getPriority(), event.getAuthor().getPriority());
        assertEquals(event.getId().toString(), json.get("eventId"));
        final TimePoint now = MillisecondsTimePoint.now();
        assertEquals(1.0, regattaLeaderboard.getScoreCorrection()
                .getExplicitScoreCorrection(fleet1Competitors.get(1), raceColumn), 0.0001);
        assertEquals(2.0, regattaLeaderboard.getScoreCorrection()
                .getExplicitScoreCorrection(fleet1Competitors.get(2), raceColumn), 0.0001);
        assertEquals(MaxPointsReason.DSQ, regattaLeaderboard.getMaxPointsReason(fleet1Competitors.get(0), raceColumn, now));
        final JSONArray jsonFinishPositions = (JSONArray) json.get("finishPositions");
        assertEquals(3, jsonFinishPositions.size());
        final JSONObject jsonDisqualified = (JSONObject) jsonFinishPositions.get(2);
        assertEquals(fleet1Competitors.get(0).getId().toString(), jsonDisqualified.get("competitorId"));
        assertEquals("DSQ", jsonDisqualified.get("maxPointsReason"));
        assertNull(getConfirmedResults(FLEET2_NAME));
    }

    @Test
    public void testLaterListReplacesEarlierOne() throws Exception {
        assertEquals(Status.OK.getStatusCode(), post(FLEET1_NAME, finishPosition(fleet1Competitors.get(0), 1, null),
                finishPosition(fleet1Competitors.get(1), 2, null)).getStatus());
        assertEquals(Status.OK.getStatusCode(), post(FLEET1_NAME, finishPosition(fleet1Competitors.get(0), null, "DSQ"),
                finishPosition(fleet1Competitors.get(1), 1, null)).getStatus());
        final Map<Serializable, CompetitorResult> confirmed = getConfirmedResults(FLEET1_NAME);
        assertEquals(MaxPointsReason.DSQ, confirmed.get(fleet1Competitors.get(0).getId()).getMaxPointsReason());
        assertEquals(0, confirmed.get(fleet1Competitors.get(0).getId()).getOneBasedRank());
        assertEquals(1, confirmed.get(fleet1Competitors.get(1).getId()).getOneBasedRank());
        assertEquals(MaxPointsReason.DSQ, regattaLeaderboard.getMaxPointsReason(fleet1Competitors.get(0), raceColumn,
                MillisecondsTimePoint.now()));
    }

    @Test
    public void testUnknownLeaderboardRaceColumnOrFleet() {
        assertEquals(Status.NOT_FOUND.getStatusCode(), leaderboardsResource
                .setFinishPositions("No such leaderboard", RACE_COLUMN_NAME, FLEET1_NAME, "[]").getStatus());
        assertEquals(Status.NOT_FOUND.getStatusCode(), leaderboardsResource
                .setFinishPositions(regattaLeaderboard.getName(), "F99", FLEET1_NAME, "[]").getStatus());
        assertEquals(Status.NOT_FOUND.getStatusCode(), leaderboardsResource
                .setFinishPositions(regattaLeaderboard.getName(), RACE_COLUMN_NAME, "No such fleet", "[]").getStatus());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), leaderboardsResource
                .setFinishPositions(regattaLeaderboard.getName(), RACE_COLUMN_NAME, null, "[]").getStatus());
    }

    @Test
    public void testCompetitorNotInRace() {
        final Response response = post(FLEET1_NAME, finishPosition(fleet2Competitors.get(0), 1, null));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity().toString().contains(fleet2Competitors.get(0).getId().toString()));
        assertNull(getConfirmedResults(FLEET1_NAME));
    }

    @Test
    public void testInvalidBodies() {
        assertEquals(Status.BAD_REQUEST.getStatusCode(), post(FLEET1_NAME,
                finishPosition(fleet1Competitors.get(0), null, "XYZ")).getStatus());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), post(FLEET1_NAME,
                finishPosition(fleet1Competitors.get(0), null, null)).getStatus());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), post(FLEET1_NAME,
                finishPosition(fleet1Competitors.get(0), 1, null), finishPosition(fleet1Competitors.get(1), 1, null))
                .getStatus());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), post(FLEET1_NAME,
                finishPosition(fleet1Competitors.get(0), 1, null), finishPosition(fleet1Competitors.get(0), 2, null))
                .getStatus());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), post(FLEET1_NAME).getStatus());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), leaderboardsResource
                .setFinishPositions(regattaLeaderboard.getName(), RACE_COLUMN_NAME, FLEET1_NAME, "[{").getStatus());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), leaderboardsResource
                .setFinishPositions(regattaLeaderboard.getName(), RACE_COLUMN_NAME, FLEET1_NAME, "{}").getStatus());
        assertNull(getConfirmedResults(FLEET1_NAME));
    }
}
