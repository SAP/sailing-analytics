package com.sap.sailing.selenium.api.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.event.EventApi;
import com.sap.sailing.selenium.api.event.LeaderboardApi;
import com.sap.sailing.selenium.api.event.RegattaApi;
import com.sap.sailing.selenium.api.event.RegattaApi.Competitor;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class LeaderBoardApiTest extends AbstractSeleniumTest {

    private static final String leaderboardName = "loggingsession";

    @Before
    public void setUp() {
        clearState(getContextRoot());
    }

    @Test
    public void testGetLeaderboardForCreatedEvent() {
        ApiContext ctx = ApiContext.createApiContext(getContextRoot(), ApiContext.SERVER_CONTEXT, "admin", "admin");

        EventApi eventApi = new EventApi();
        LeaderboardApi leaderboardApi = new LeaderboardApi();

        eventApi.createEvent(ctx, leaderboardName, "75QMNATIONALEKREUZER", CompetitorRegistrationType.CLOSED,
                "default");

        JSONObject leaderBoard = leaderboardApi.getLeaderboard(ctx, leaderboardName);
        assertEquals("read: leaderboard.name is different", leaderboardName, leaderBoard.get("name"));
        assertEquals("read: leaderboard.displayName is different", leaderboardName, leaderBoard.get("displayName"));
        assertNotNull("read: leaderboard.resultTimepoint is missing", leaderBoard.get("resultTimepoint"));
        assertEquals("read: leaderboard.resultState is different", "Live", leaderBoard.get("resultState"));
        assertEquals("read: leaderboard.type is different", "RegattaLeaderboard", leaderBoard.get("type"));
        assertEquals("read: leaderboard.canBoatsOfCompetitorsChangePerRace is different", false,
                leaderBoard.get("canBoatsOfCompetitorsChangePerRace"));
        assertNull("read: leaderboard.maxCompetitorsCount should be null", leaderBoard.get("maxCompetitorsCount"));
        assertNull("read: leaderboard.scoringComment should be null", leaderBoard.get("scoringComment"));
        assertNull("read: leaderboard.lastScoringUpdate should be null", leaderBoard.get("lastScoringUpdate"));
        assertEquals("read: leaderboard.columnNames should be empty", 0,
                ((JSONArray) leaderBoard.get("columnNames")).size());
        assertEquals("read: leaderboard.competitors should be empty", 0,
                ((JSONArray) leaderBoard.get("competitors")).size());
        assertEquals("read: leaderboard.ShardingLeaderboardName is different",
                "/leaderboard/" + leaderboardName.replaceAll(" ", "_").replaceAll("<", "_").replaceAll(">", "_"),
                leaderBoard.get("ShardingLeaderboardName"));
    }

    @Test
    public void testDeviceMappingStartAndEnd() throws Exception {
        ApiContext ctx = ApiContext.createApiContext(getContextRoot(), ApiContext.SERVER_CONTEXT, "admin", "admin");
        EventApi eventApi = new EventApi();
        RegattaApi regattaApi = new RegattaApi();
        LeaderboardApi leaderboardApi = new LeaderboardApi();

        eventApi.createEvent(ctx, leaderboardName, "75QMNATIONALEKREUZER", CompetitorRegistrationType.CLOSED,
                "default");
        Competitor competitor = regattaApi.createAndAddCompetitor(ctx, leaderboardName, "75QMNATIONALEKREUZER",
                "test@de", "Max Mustermann", "USA");
        UUID competitorId = competitor.getId();

        UUID boatId = UUID.randomUUID();
        UUID markId = UUID.randomUUID();
        UUID deviceUuid = UUID.randomUUID();
        String secret = "dskjshfkdjhfksh";
        Long fromMillis = System.currentTimeMillis();
        @SuppressWarnings("unused") // TODO: check result
        JSONObject dmStart = leaderboardApi.deviceMappingsStart(ctx, leaderboardName, competitorId, boatId, markId,
                deviceUuid, secret, fromMillis);
        @SuppressWarnings("unused") // TODO: check result
        JSONObject dmEnd = leaderboardApi.deviceMappingsEnd(ctx, leaderboardName, competitorId, boatId, markId,
                deviceUuid, secret, fromMillis);
    }
}
