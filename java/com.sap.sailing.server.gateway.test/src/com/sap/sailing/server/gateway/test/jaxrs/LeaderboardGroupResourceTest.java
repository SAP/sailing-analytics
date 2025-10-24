package com.sap.sailing.server.gateway.test.jaxrs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.json.simple.JSONArray;
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
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithStartTimeAndRanks;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;
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
}
