package com.sap.sailing.server.gateway.test.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.authz.UnauthorizedException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithStartTimeAndRanks;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.rest.StreamingOutputUtil;

public class LeaderboardGroupResourceTest extends AbstractJaxRsApiTest {
    private Regatta regatta;
    private LeaderboardGroup leaderboardGroup;
    private String boatClassName = "49er";
    private String regattaName = "TestRegatta";
    private String leaderboardGroupName = "TestLeaderboardGroup";

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        List<Series> series = new ArrayList<Series>();
        List<Fleet> fleets = new ArrayList<Fleet>();
        List<String> raceColumnNames = new ArrayList<String>();
        fleets.add(new FleetImpl("Fleet1"));
        fleets.add(new FleetImpl("Fleet2"));
        Series testSeries = new SeriesImpl("TestSeries", /* isMedal */false, /* isFleetsCanRunInParallel */ true, fleets,
                raceColumnNames, /* trackedRegattaRegistry */null);
        series.add(testSeries);
        regatta = racingEventService.createRegatta(RegattaImpl.getDefaultName(regattaName, boatClassName), boatClassName, 
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
        TrackedRace r1Fleet1 = new MockedTrackedRaceWithStartTimeAndRanks(now, fleet1Competitors);
        TrackedRace r1Fleet2 = new MockedTrackedRaceWithStartTimeAndRanks(now, fleet2Competitors);
        r1Column.setTrackedRace(r1Column.getFleetByName("Fleet1"), r1Fleet1);
        r1Column.setTrackedRace(r1Column.getFleetByName("Fleet2"), r1Fleet2);
        racingEventService.addRegattaLeaderboard(regatta.getRegattaIdentifier(), "Testregatta displayName", new int[] { 3, 5 });
        List<String> leaderboardNames = new ArrayList<String>();
        leaderboardNames.add(regatta.getName());
        leaderboardGroup = racingEventService.addLeaderboardGroup(UUID.randomUUID(), leaderboardGroupName, "description", 
                "The LG displayName", /* displayGroupsInReverseOrder */ false,
                leaderboardNames, /* overallLeaderboardDiscardThresholds */ null, /* overallLeaderboardScoringSchemeType */ null);
    }

    @Test
    public void testExportLeaderboardGroupsAsJson() throws Exception {
        Response leaderboardGroupsResponse = leaderboardGroupsResource.getLeaderboardGroups();
        String jsonString = StreamingOutputUtil.getEntityAsString(leaderboardGroupsResponse.getEntity());
        Object obj = JSONValue.parse(jsonString);
        JSONArray array= (JSONArray) obj;
        assertTrue(array.size() == 1);
        String jsonFirstLeaderboardGroup = (String) array.get(0);
        assertTrue(leaderboardGroup.getName().equals(jsonFirstLeaderboardGroup));
    }

    @Test
    public void testCreateLeaderboardGroupAndAddLeaderboard() throws Exception {
        final String seasonName = "Season 2026";
        final Response createResponse = leaderboardGroupsResource.createLeaderboardGroup(seasonName, "The season",
                "Season", /* displayGroupsInReverseOrder */ null, /* overallLeaderboardScoringScheme */ null,
                /* overallLeaderboardDiscardThresholds */ null);
        assertEquals(Status.OK.getStatusCode(), createResponse.getStatus());
        final JSONObject createdJson = (JSONObject) JSONValue.parse(StreamingOutputUtil.getEntityAsString(createResponse.getEntity()));
        assertEquals(seasonName, createdJson.get("name"));
        assertEquals(Boolean.FALSE, createdJson.get("hasOverallLeaderboard"));
        assertTrue(((JSONArray) createdJson.get("leaderboards")).isEmpty());
        final LeaderboardGroup season = racingEventService.getLeaderboardGroupByName(seasonName);
        assertNotNull(season);
        assertEquals(season.getId().toString(), createdJson.get("id"));
        final Response addResponse = leaderboardGroupsResource.addLeaderboardToLeaderboardGroup(season.getId().toString(), regatta.getName());
        assertEquals(Status.OK.getStatusCode(), addResponse.getStatus());
        final JSONObject addedJson = (JSONObject) JSONValue.parse(StreamingOutputUtil.getEntityAsString(addResponse.getEntity()));
        assertEquals(1, ((JSONArray) addedJson.get("leaderboards")).size());
        assertEquals(1, Util.size(season.getLeaderboards()));
        assertEquals(regatta.getName(), season.getLeaderboards().iterator().next().getName());
        assertEquals("The season", season.getDescription());
        assertEquals("Season", season.getDisplayName());
        // adding the same leaderboard again changes nothing
        assertEquals(Status.OK.getStatusCode(), leaderboardGroupsResource.addLeaderboardToLeaderboardGroup(seasonName, regatta.getName()).getStatus());
        assertEquals(1, Util.size(season.getLeaderboards()));
    }

    @Test
    public void testCreateLeaderboardGroupWithOverallLeaderboard() throws Exception {
        final String seasonName = "Season with overall leaderboard";
        final Response createResponse = leaderboardGroupsResource.createLeaderboardGroup(seasonName, null, null,
                /* displayGroupsInReverseOrder */ null, ScoringSchemeType.LOW_POINT.name(), /* overallLeaderboardDiscardThresholds */ null);
        assertEquals(Status.OK.getStatusCode(), createResponse.getStatus());
        final LeaderboardGroup season = racingEventService.getLeaderboardGroupByName(seasonName);
        assertTrue(season.hasOverallLeaderboard());
        assertEquals(Status.OK.getStatusCode(), leaderboardGroupsResource.addLeaderboardToLeaderboardGroup(seasonName, regatta.getName()).getStatus());
        assertEquals(1, Util.size(season.getLeaderboards()));
        assertTrue(season.hasOverallLeaderboard());
        assertEquals(ScoringSchemeType.LOW_POINT, season.getOverallLeaderboard().getScoringScheme().getType());
    }

    @Test
    public void testCreateLeaderboardGroupRejectsDuplicateNameAndBadParameters() throws Exception {
        assertEquals(Status.CONFLICT.getStatusCode(), leaderboardGroupsResource.createLeaderboardGroup(leaderboardGroupName,
                null, null, null, null, null).getStatus());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), leaderboardGroupsResource.createLeaderboardGroup(null,
                null, null, null, null, null).getStatus());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), leaderboardGroupsResource.createLeaderboardGroup("New group",
                null, null, null, "NO_SUCH_SCHEME", null).getStatus());
        assertNull(racingEventService.getLeaderboardGroupByName("New group"));
    }

    @Test
    public void testAddUnknownLeaderboardOrToUnknownGroup() throws Exception {
        assertEquals(Status.NOT_FOUND.getStatusCode(), leaderboardGroupsResource.addLeaderboardToLeaderboardGroup(leaderboardGroupName,
                "No such leaderboard").getStatus());
        assertEquals(Status.NOT_FOUND.getStatusCode(), leaderboardGroupsResource.addLeaderboardToLeaderboardGroup("No such group",
                regatta.getName()).getStatus());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), leaderboardGroupsResource.addLeaderboardToLeaderboardGroup(leaderboardGroupName,
                null).getStatus());
    }

    @Test
    public void testCreateLeaderboardGroupRejectsOverallLeaderboardNameCollision() throws Exception {
        final String seasonName = "Season 2027";
        final String overallLeaderboardName = LeaderboardGroupMetaLeaderboard.getOverallLeaderboardName(seasonName);
        final Leaderboard existing = racingEventService.addFlexibleLeaderboard(overallLeaderboardName, null, new int[0],
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT),
                /* courseAreaIds */ Collections.<Serializable> emptyList());
        assertEquals(Status.CONFLICT.getStatusCode(), leaderboardGroupsResource.createLeaderboardGroup(seasonName,
                null, null, null, ScoringSchemeType.LOW_POINT.name(), null).getStatus());
        assertNull(racingEventService.getLeaderboardGroupByName(seasonName));
        assertSame(existing, racingEventService.getLeaderboardByName(overallLeaderboardName));
        // without an overall leaderboard there is no collision
        assertEquals(Status.OK.getStatusCode(), leaderboardGroupsResource.createLeaderboardGroup(seasonName,
                null, null, null, null, null).getStatus());
        assertSame(existing, racingEventService.getLeaderboardByName(overallLeaderboardName));
    }

    @Test
    public void testUnauthorizedCreateLeaderboardGroupIsNotWrapped() throws Exception {
        final String seasonName = "Season nobody may create";
        Mockito.doThrow(new UnauthorizedException("not permitted")).when(securityService)
                .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                        Mockito.eq(SecuredDomainType.LEADERBOARD_GROUP), Mockito.any(), Mockito.any(),
                        getAnyCallable());
        // the exception must reach the JAX-RS exception mapping unchanged (401), not wrapped in a RuntimeException
        assertThrows(UnauthorizedException.class, () -> leaderboardGroupsResource.createLeaderboardGroup(seasonName,
                null, null, null, null, null));
        assertNull(racingEventService.getLeaderboardGroupByName(seasonName));
    }
}
