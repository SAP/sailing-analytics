package com.sap.sailing.server.gateway.test.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.SecurityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithStartTimeAndRanks;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.gateway.jaxrs.api.AbstractLeaderboardsResource;
import com.sap.sailing.server.gateway.jaxrs.api.AbstractLeaderboardsResource.ResultStates;
import com.sap.sailing.server.gateway.jaxrs.api.LeaderboardsResource;
import com.sap.sailing.server.gateway.jaxrs.api.LeaderboardsResourceV2;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.rest.StreamingOutputUtil;
import com.sap.sse.security.SecurityService;

public class LeaderboardsResourceTest extends AbstractJaxRsApiTest {
    private Regatta regatta;
    private RegattaLeaderboard regattaLeaderboard;
    private String boatClassName = "49er";
    private String regattaBaseName = "TestRegatta";
    
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        dropAndCreateRegatta();
    }
    
    private void dropAndCreateRegatta() {
        service.getDB().drop();
        List<Series> series = new ArrayList<Series>();
        List<Fleet> fleets = new ArrayList<Fleet>();
        List<String> raceColumnNames = new ArrayList<String>();
        fleets.add(new FleetImpl("Fleet1"));
        fleets.add(new FleetImpl("Fleet2"));
        Series testSeries = new SeriesImpl("TestSeries", /* isMedal */false, /* isFleetsCanRunInParallel */ true, fleets,
                raceColumnNames, /* trackedRegattaRegistry */null);
        series.add(testSeries);
        regatta = racingEventService.createRegatta(RegattaImpl.getDefaultName(regattaBaseName, boatClassName), boatClassName, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* registrationLinkSecret */ null, /* startDate */ null, /* endDate */ null, UUID.randomUUID(), series,
                /* persistent */ true, DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), /* course area ID */ (Serializable) null,
                /* buoyZoneRadiusInHullLengths */2.0, /* useStartTimeInference */ true,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false, OneDesignRankingMetric::new);
        testSeries.addRaceColumn("R1", /* trackedRegattaRegistry */ null);
        testSeries.addRaceColumn("R2", /* trackedRegattaRegistry */ null);
        List<Competitor> competitors = createCompetitors(4);
        List<Competitor> fleet1Competitors = competitors.subList(0, 2);
        List<Competitor> fleet2Competitors = competitors.subList(2, 4);
        TimePoint now = MillisecondsTimePoint.now();
        RaceColumn r1Column = series.get(0).getRaceColumnByName("R1");
        final TrackedRace r1Fleet1 = createTrackedRaceWithEmptyDetails(now, fleet1Competitors);
        final TrackedRace r1Fleet2 = createTrackedRaceWithEmptyDetails(now, fleet2Competitors);
        r1Column.setTrackedRace(r1Column.getFleetByName("Fleet1"), r1Fleet1);
        r1Column.setTrackedRace(r1Column.getFleetByName("Fleet2"), r1Fleet2);
        regattaLeaderboard = racingEventService.addRegattaLeaderboard(regatta.getRegattaIdentifier(), "Testregatta displayName", new int[] { 3, 5 });
        
    }
    
    @Test
    public void testExportEmptyLeaderboardAsJson() throws Exception {
        Response leaderboardReponse = leaderboardsResource.getLeaderboard(regatta.getName(),
                AbstractLeaderboardsResource.ResultStates.Final, null, null,
                /* competitorAndBoatIdsOnly */ false, /* includeUncorrectedTotalPoints */ false);
        String jsonString = StreamingOutputUtil.getEntityAsString(leaderboardReponse.getEntity());
        Object obj= JSONValue.parse(jsonString);
        JSONObject jsonObject = (JSONObject) obj;
        String jsonLeaderboardName = (String) jsonObject.get("name");
        String jsonResultState = (String) jsonObject.get("resultState");
        assertTrue(regattaLeaderboard.getName().equals(jsonLeaderboardName));
        assertTrue(AbstractLeaderboardsResource.ResultStates.Final.name().equals(jsonResultState));
        // resultTimepoint should be null if there are no 'final' results yet 
        TimePoint resultTimePoint = parseTimepointFromJsonNumber((Long) jsonObject.get("resultTimepoint"));
        assertNull(resultTimePoint);
        JSONArray jsonCompetitors = (JSONArray) jsonObject.get("competitors");
        assertTrue(jsonCompetitors.size() == 4);
    }
    
    @Test
    public void testExportLeaderboardWithFinalResultStateAsJson() throws Exception {
        Response leaderboardReponse = leaderboardsResource.getLeaderboard(regatta.getName(),
                AbstractLeaderboardsResource.ResultStates.Final, null, null,
                /* competitorAndBoatIdsOnly */ false, /* includeUncorrectedTotalPoints */ false);
        String jsonString = StreamingOutputUtil.getEntityAsString(leaderboardReponse.getEntity());
        Object obj= JSONValue.parse(jsonString);
        JSONObject jsonObject = (JSONObject) obj;
        String jsonLeaderboardName = (String) jsonObject.get("name");
        String jsonResultState = (String) jsonObject.get("resultState");
        assertEquals(regattaLeaderboard.getName(), jsonLeaderboardName);
        assertEquals(AbstractLeaderboardsResource.ResultStates.Final.name(), jsonResultState);
        TimePoint resultTimePoint = parseTimepointFromJsonNumber((Long) jsonObject.get("resultTimepoint"));
        assertNull(resultTimePoint);
        regattaLeaderboard.getScoreCorrection().setTimePointOfLastCorrectionsValidity(MillisecondsTimePoint.now());
        Response leaderboardReponse2 = leaderboardsResource.getLeaderboard(regatta.getName(),
                AbstractLeaderboardsResource.ResultStates.Final, null, null,
                /* competitorAndBoatIdsOnly */ false, /* includeUncorrectedTotalPoints */ false);
        String jsonString2 = StreamingOutputUtil.getEntityAsString(leaderboardReponse2.getEntity());        
        obj= JSONValue.parse(jsonString2);
        jsonObject = (JSONObject) obj;
        resultTimePoint = parseTimepointFromJsonNumber((Long) jsonObject.get("resultTimepoint"));
        assertNotNull(resultTimePoint);
     }

    @Test
    public void testExportLeaderboardWithLiveResultStateAsJson() throws Exception {
        Response leaderboardReponse = leaderboardsResource.getLeaderboard(regatta.getName(),
                AbstractLeaderboardsResource.ResultStates.Live, null, null,
                /* competitorAndBoatIdsOnly */ false, /* includeUncorrectedTotalPoints */ false);
        String jsonString = StreamingOutputUtil.getEntityAsString(leaderboardReponse.getEntity());
        Object obj= JSONValue.parse(jsonString);
        JSONObject jsonObject = (JSONObject) obj;
        String jsonLeaderboardName = (String) jsonObject.get("name");
        String jsonResultState = (String) jsonObject.get("resultState");
        assertTrue(regattaLeaderboard.getName().equals(jsonLeaderboardName));
        assertTrue(AbstractLeaderboardsResource.ResultStates.Live.name().equals(jsonResultState));
        TimePoint resultTimePoint = parseTimepointFromJsonNumber((Long) jsonObject.get("resultTimepoint"));
        assertNotNull(resultTimePoint);
        JSONArray jsonCompetitors = (JSONArray) jsonObject.get("competitors");
        assertTrue(jsonCompetitors.size() == 4);
    }
    
    @Test
    public void testSmartFutureCacheInExportLeaderboardAsJson() throws Exception {
        TimePoint now = MillisecondsTimePoint.now();
        regattaLeaderboard.getScoreCorrection().setTimePointOfLastCorrectionsValidity(now);    
        Thread.sleep(100);
        // call the servlet for the first time
        Response leaderboardReponse = leaderboardsResource.getLeaderboard(regatta.getName(),
                AbstractLeaderboardsResource.ResultStates.Final, null, null,
                /* competitorAndBoatIdsOnly */ false, /* includeUncorrectedTotalPoints */ false);
        String jsonString = StreamingOutputUtil.getEntityAsString(leaderboardReponse.getEntity());
        Object obj= JSONValue.parse(jsonString);
        JSONObject jsonObject = (JSONObject) obj;
        String jsonLeaderboardName = (String) jsonObject.get("name");
        String jsonResultState = (String) jsonObject.get("resultState");
        assertTrue(regattaLeaderboard.getName().equals(jsonLeaderboardName));
        assertTrue(AbstractLeaderboardsResource.ResultStates.Final.name().equals(jsonResultState));
        TimePoint resultTimePoint = parseTimepointFromJsonNumber((Long) jsonObject.get("resultTimepoint"));
        assertNotNull(resultTimePoint);
        // second call - it's expected to get the same result from the cache
        Thread.sleep(100);
        Response leaderboardReponse2 = leaderboardsResource.getLeaderboard(regatta.getName(),
                AbstractLeaderboardsResource.ResultStates.Final, null, null,
                /* competitorAndBoatIdsOnly */ false, /* includeUncorrectedTotalPoints */ false);
        String jsonString2 = StreamingOutputUtil.getEntityAsString(leaderboardReponse2.getEntity());
        Object obj2= JSONValue.parse(jsonString2);
        JSONObject jsonObject2 = (JSONObject) obj2;
        String jsonLeaderboardName2 = (String) jsonObject2.get("name");
        String jsonResultState2 = (String) jsonObject2.get("resultState");
        assertTrue(jsonLeaderboardName.equals(jsonLeaderboardName2));
        assertTrue(jsonResultState.equals(jsonResultState2));
        TimePoint resultTimePoint2 = parseTimepointFromJsonNumber((Long) jsonObject.get("resultTimepoint"));
        assertNotNull(resultTimePoint2);
        assertTrue(resultTimePoint.equals(resultTimePoint2));
    }

    @Test
    public void testExportLeaderboardWithMaxCompetitorsCountAsJson() throws Exception {
        Integer maxCompetitorsCount = 2;
        Response leaderboardResponse = leaderboardsResource.getLeaderboard(regatta.getName(),
                AbstractLeaderboardsResource.ResultStates.Final, null, null,
                /* competitorAndBoatIdsOnly */ false, /* includeUncorrectedTotalPoints */ false);
        String jsonString = StreamingOutputUtil.getEntityAsString(leaderboardResponse.getEntity());
        Object obj = JSONValue.parse(jsonString);
        JSONObject jsonObject = (JSONObject) obj;
        String jsonLeaderboardName = (String) jsonObject.get("name");
        String jsonResultState = (String) jsonObject.get("resultState");
        assertTrue(regattaLeaderboard.getName().equals(jsonLeaderboardName));
        assertTrue(AbstractLeaderboardsResource.ResultStates.Final.name().equals(jsonResultState));
        TimePoint resultTimePoint = parseTimepointFromJsonNumber((Long) jsonObject.get("resultTimepoint"));
        assertNull(resultTimePoint);
        regattaLeaderboard.getScoreCorrection().setTimePointOfLastCorrectionsValidity(MillisecondsTimePoint.now());    
        // first call for 'all' competitors
        Response leaderboardReponse2 = leaderboardsResource.getLeaderboard(regatta.getName(),
                AbstractLeaderboardsResource.ResultStates.Final, null, null,
                /* competitorAndBoatIdsOnly */ false, /* includeUncorrectedTotalPoints */ false);
        jsonString = StreamingOutputUtil.getEntityAsString(leaderboardReponse2.getEntity());
        obj= JSONValue.parse(jsonString);
        jsonObject = (JSONObject) obj;
        JSONArray jsonCompetitors = (JSONArray) jsonObject.get("competitors");
        assertEquals(jsonCompetitors.size(), 4);
        Thread.sleep(100);
        // second call with maxCompetitorsCount set
        Response leaderboardReponse3 = leaderboardsResource.getLeaderboard(regatta.getName(),
                AbstractLeaderboardsResource.ResultStates.Final, maxCompetitorsCount, null,
                /* competitorAndBoatIdsOnly */ false, /* includeUncorrectedTotalPoints */ false);
        jsonString = StreamingOutputUtil.getEntityAsString(leaderboardReponse3.getEntity());
        obj= JSONValue.parse(jsonString);
        jsonObject = (JSONObject) obj;
        jsonCompetitors = (JSONArray) jsonObject.get("competitors");
        assertEquals((Integer) jsonCompetitors.size(), maxCompetitorsCount);
        Thread.sleep(100);
        // third call for 'all' competitors
        Response leaderboardReponse4 = leaderboardsResource.getLeaderboard(regatta.getName(),
                AbstractLeaderboardsResource.ResultStates.Final, null, null,
                /* competitorAndBoatIdsOnly */ false, /* includeUncorrectedTotalPoints */ false);
        jsonString = StreamingOutputUtil.getEntityAsString(leaderboardReponse4.getEntity());
        obj= JSONValue.parse(jsonString);
        jsonObject = (JSONObject) obj;
        jsonCompetitors = (JSONArray) jsonObject.get("competitors");
        assertEquals(jsonCompetitors.size(), 4);
     }
    
    private TrackedRace createTrackedRaceWithEmptyDetails(final TimePoint startTime, final List<Competitor> competitors) {
        final TrackedRace race = spy(new MockedTrackedRaceWithStartTimeAndRanks(startTime, competitors, regatta));
        doReturn(Collections.emptyList()).when(race).getTrackedLegs();
        doReturn(Collections.emptyNavigableSet()).when(race).getMarkPassings(any(Competitor.class));
        doReturn(new OneDesignRankingMetric(race)).when(race).getRankingMetric();
        return race;
    }

    @Test
    public void testV1LiveUncorrectedScoresAreOptIn() throws Exception {
        checkUncorrectedScoresAreOptIn(/* v2 */ false, ResultStates.Live);
    }

    @Test
    public void testV2LiveUncorrectedScoresAreOptIn() throws Exception {
        checkUncorrectedScoresAreOptIn(/* v2 */ true, ResultStates.Live);
    }

    @Test
    public void testV1FinalUncorrectedScoresAreOptIn() throws Exception {
        checkUncorrectedScoresAreOptIn(/* v2 */ false, ResultStates.Final);
    }

    @Test
    public void testV2FinalUncorrectedScoresAreOptIn() throws Exception {
        checkUncorrectedScoresAreOptIn(/* v2 */ true, ResultStates.Final);
    }

    private void checkUncorrectedScoresAreOptIn(final boolean v2, final ResultStates resultState) throws Exception {
        final Competitor competitor = regattaLeaderboard.getCompetitorByIdAsString("1");
        regattaLeaderboard.getScoreCorrection().correctScore(competitor,
                regattaLeaderboard.getRaceColumnByName("R1"), /* correctedScore */ 7.0);
        regattaLeaderboard.getScoreCorrection().setTimePointOfLastCorrectionsValidity(MillisecondsTimePoint.now());
        final RegattaLeaderboard leaderboard = spy(regattaLeaderboard);
        doReturn(leaderboard).when(racingEventService).getLeaderboardByName(regatta.getName());
        // Also request false after true, so populated DTOs cannot leak the optional field through the cache.
        for (final boolean includeUncorrectedTotalPoints : new boolean[] { getDefaultIncludeUncorrectedTotalPoints(v2), false, true, false }) {
            clearInvocations(leaderboard);
            final JSONObject json = getLeaderboardWithUncorrectedPointsOption(v2, resultState, includeUncorrectedTotalPoints);
            final JSONObject scores = getRaceScores(json, v2 ? "columns" : "raceScores", competitor.getId().toString());
            final JSONObject corrected = (JSONObject) scores.get("R1");
            assertEquals(/* expected */ 7.0, corrected.get("totalPoints"));
            assertEquals(/* expected */ 7.0, corrected.get("netPoints"));
            assertEquals(Boolean.TRUE, corrected.get("isCorrected"));
            assertEquals(includeUncorrectedTotalPoints, corrected.containsKey("uncorrectedTotalPoints"));
            final JSONObject unavailable = (JSONObject) scores.get("R2");
            assertNull(unavailable.get("totalPoints"));
            assertNull(unavailable.get("netPoints"));
            assertEquals(includeUncorrectedTotalPoints, unavailable.containsKey("uncorrectedTotalPoints"));
            if (includeUncorrectedTotalPoints) {
                assertEquals(/* expected */ 1.0, corrected.get("uncorrectedTotalPoints"));
                assertNull(unavailable.get("uncorrectedTotalPoints"));
            }
            verify(leaderboard).getLeaderboardDTO(any(), any(), anyBoolean(), any(), any(), eq(includeUncorrectedTotalPoints));
            verify(leaderboard, never()).getLeaderboardDTO(any(), any(), anyBoolean(), any(), any(), eq(!includeUncorrectedTotalPoints));
        }
    }

    @Test
    public void testV1UncorrectedScoresInEmptyFinalResults() throws Exception {
        checkUncorrectedScoresInEmptyFinalResults(/* v2 */ false);
    }

    @Test
    public void testV2UncorrectedScoresInEmptyFinalResults() throws Exception {
        checkUncorrectedScoresInEmptyFinalResults(/* v2 */ true);
    }

    private void checkUncorrectedScoresInEmptyFinalResults(final boolean v2) throws Exception {
        final RegattaLeaderboard leaderboard = spy(regattaLeaderboard);
        doReturn(leaderboard).when(racingEventService).getLeaderboardByName(regatta.getName());
        for (final boolean includeUncorrectedTotalPoints : new boolean[] { getDefaultIncludeUncorrectedTotalPoints(v2), false, true }) {
            final JSONObject json = getLeaderboardWithUncorrectedPointsOption(v2, ResultStates.Final, includeUncorrectedTotalPoints);
            // Both API versions use the shared empty-result representation.
            final JSONObject scores = getRaceScores(json, "raceScores", "1");
            for (final Object value : scores.values()) {
                final JSONObject score = (JSONObject) value;
                assertNull(score.get("totalPoints"));
                assertNull(score.get("netPoints"));
                assertEquals(includeUncorrectedTotalPoints, score.containsKey("uncorrectedTotalPoints"));
                assertNull(score.get("uncorrectedTotalPoints"));
            }
        }
        verify(leaderboard, never()).getLeaderboardDTO(any(), any(), anyBoolean(), any(), any(), anyBoolean());
    }

    private boolean getDefaultIncludeUncorrectedTotalPoints(final boolean v2) {
        final Class<?> resourceClass = v2 ? LeaderboardsResourceV2.class : LeaderboardsResource.class;
        String defaultValue = null;
        for (final Method method : resourceClass.getDeclaredMethods()) {
            if (method.getName().equals("getLeaderboard")) {
                for (final Parameter parameter : method.getParameters()) {
                    final QueryParam queryParam = parameter.getAnnotation(QueryParam.class);
                    if (queryParam != null && queryParam.value().equals("includeUncorrectedTotalPoints")) {
                        defaultValue = parameter.getAnnotation(DefaultValue.class).value();
                    }
                }
            }
        }
        assertEquals("false", defaultValue);
        return Boolean.parseBoolean(defaultValue);
    }

    private JSONObject getLeaderboardWithUncorrectedPointsOption(final boolean v2, final ResultStates resultState,
            final boolean includeUncorrectedTotalPoints) throws Exception {
        doReturn(true).when(SecurityUtils.getSubject()).isPermitted(anyString());
        final Response response;
        if (v2) {
            final TestLeaderboardsResourceV2 resource = spyResource(new TestLeaderboardsResourceV2());
            doReturn(getSecurityService()).when(resource).getSecurityService();
            response = resource.getLeaderboard(regatta.getName(), resultState,
                    Collections.emptyList(), Collections.emptyList(), /* time */ null, /* timeasmillis */ null,
                    /* maxCompetitorsCount */ null, /* regattaSecret */ null, /* competitorAndBoatIdsOnly */ false,
                    Collections.emptyList(), /* showOnlyCompetitorsWithIdsProvided */ false, includeUncorrectedTotalPoints);
        } else {
            response = leaderboardsResource.getLeaderboard(regatta.getName(), resultState,
                    /* maxCompetitorsCount */ null, /* regattaSecret */ null, /* competitorAndBoatIdsOnly */ false,
                    includeUncorrectedTotalPoints);
        }
        assertEquals(Status.OK.getStatusCode(), response.getStatus(), () -> String.valueOf(response.getEntity()));
        return (JSONObject) JSONValue.parseWithException(StreamingOutputUtil.getEntityAsString(response.getEntity()));
    }

    private JSONObject getRaceScores(final JSONObject leaderboard, final String scoreProperty, final String competitorId) {
        JSONObject scores = null;
        for (final Object entry : (JSONArray) leaderboard.get("competitors")) {
            final JSONObject competitor = (JSONObject) entry;
            if (competitorId.equals(competitor.get("id"))) {
                scores = (JSONObject) competitor.get(scoreProperty);
            }
        }
        assertNotNull(scores);
        assertEquals(/* expected */ 2, scores.size());
        return scores;
    }

    public static class TestLeaderboardsResourceV2 extends LeaderboardsResourceV2 {
        @Override
        protected SecurityService getSecurityService() {
            return super.getSecurityService();
        }
    }

    @Test
    public void testXrrScoreImport() throws Exception {
        final TimePoint validityTimePoint = MillisecondsTimePoint.now();
        final String comment = "A Comment";
        final boolean allowRaceDefaultsByOrder = true;
        final boolean allowPartialImport = true;
        Response leaderboardResponse = importScores(validityTimePoint, comment, allowRaceDefaultsByOrder, allowPartialImport);
        assertEquals(Status.OK.getStatusCode(), leaderboardResponse.getStatus());
        final JSONObject jsonObject = (JSONObject) JSONValue.parse(StreamingOutputUtil.getEntityAsString(leaderboardResponse.getEntity()));
        assertNotNull(jsonObject);
        assertFalse((Boolean) jsonObject.get("complete"));
        assertEquals(comment, regattaLeaderboard.getScoreCorrection().getComment());
        assertEquals(validityTimePoint, regattaLeaderboard.getScoreCorrection().getTimePointOfLastCorrectionsValidity());
        assertTrue(((JSONArray) jsonObject.get("matchedRaceNumbers")).stream().filter(o->((JSONObject) o).get("raceNumber").equals("2")).findAny().isPresent());
        assertEquals(11.0, regattaLeaderboard.getTotalPoints(regattaLeaderboard.getCompetitorByIdAsString("1"),
                regattaLeaderboard.getRaceColumnByName("R2"), validityTimePoint), 0.000001);
    }

    @Test
    public void testFailingXrrScoreImport() throws Exception {
        final TimePoint validityTimePoint = null;
        final String comment = null;
        final boolean allowRaceDefaultsByOrder = true;
        final boolean allowPartialImport = false;
        Response leaderboardResponse = importScores(validityTimePoint, comment, allowRaceDefaultsByOrder, allowPartialImport);
        assertEquals(Status.CONFLICT.getStatusCode(), leaderboardResponse.getStatus());
        final JSONObject jsonObject = (JSONObject) JSONValue.parse(StreamingOutputUtil.getEntityAsString(leaderboardResponse.getEntity()));
        assertNotNull(jsonObject);
        assertFalse((Boolean) jsonObject.get("complete"));
        assertEquals(null, regattaLeaderboard.getScoreCorrection().getComment());
        assertNull(regattaLeaderboard.getScoreCorrection().getTimePointOfLastCorrectionsValidity());
        assertNull(regattaLeaderboard.getTotalPoints(regattaLeaderboard.getCompetitorByIdAsString("1"),
                regattaLeaderboard.getRaceColumnByName("R2"), validityTimePoint));
    }

    protected Response importScores(final TimePoint validityTimePoint, final String comment,
            final boolean allowRaceDefaultsByOrder, final boolean allowPartialImport) throws Exception {
        Response leaderboardResponse = leaderboardsResource.uploadResults(regatta.getName(),
                /* scoreCorrectionProviderName */ "ISAF XML Regatta Result (XRR) Importer",
                /* validity time point */ validityTimePoint==null?null:validityTimePoint.asMillis(),
                /* comment */ comment,
                /* allowRaceDefaultsByOrder */ allowRaceDefaultsByOrder,
                /* sailIds */ Collections.singletonList("GER 2098"),
                /* competitorIdAsStringForSailId */ Collections.singletonList("1"),
                /* raceNumber */ null, /* raceColumnNameForRaceNumber */ null,
                /* allowPartialImport */ allowPartialImport,
                getClass().getClassLoader().getResourceAsStream("YES_29er_XRR.xml"));
        assertNotNull(leaderboardResponse);
        return leaderboardResponse;
    }
}
