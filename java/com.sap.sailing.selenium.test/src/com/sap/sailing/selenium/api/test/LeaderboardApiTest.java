package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.domain.common.CompetitorRegistrationType.CLOSED;
import static com.sap.sailing.selenium.api.core.ApiContext.SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.json.simple.JSONArray;
import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.event.EventApi;
import com.sap.sailing.selenium.api.event.LeaderboardApi;
import com.sap.sailing.selenium.api.event.LeaderboardApi.DeviceMappingRequest;
import com.sap.sailing.selenium.api.event.LeaderboardApi.Leaderboard;
import com.sap.sailing.selenium.api.event.LeaderboardApi.StartTime;
import com.sap.sailing.selenium.api.event.LeaderboardApi.TrackingTimes;
import com.sap.sailing.selenium.api.regatta.Competitor;
import com.sap.sailing.selenium.api.regatta.RaceColumn;
import com.sap.sailing.selenium.api.regatta.RegattaApi;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class LeaderboardApiTest extends AbstractSeleniumTest {

    private static final String BOATCLASSNAME = "75QMNATIONALEKREUZER";
    private static final String LEADERBOARD_NAME = "loggingsession";

    private final EventApi eventApi = new EventApi();
    private final RegattaApi regattaApi = new RegattaApi();
    private final LeaderboardApi leaderboardApi = new LeaderboardApi();

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot(), /* headless */ true);
    }

    @SeleniumTestCase
    public void testGetLeaderboardForCreatedEvent() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        eventApi.createEvent(ctx, LEADERBOARD_NAME, BOATCLASSNAME, CLOSED, "default");
        Leaderboard leaderBoard = leaderboardApi.getLeaderboard(ctx, LEADERBOARD_NAME);
        assertEquals(LEADERBOARD_NAME, leaderBoard.getName(), "read: leaderboard.name is different");
        assertEquals(LEADERBOARD_NAME, leaderBoard.getDisplayName(), "read: leaderboard.displayName is different");
        assertNotNull(leaderBoard.getResultTimePoint(), "read: leaderboard.resultTimepoint is missing");
        assertEquals("Live", leaderBoard.getResultState(), "read: leaderboard.resultState is different");
        assertEquals("RegattaLeaderboard", leaderBoard.getType(), "read: leaderboard.type is different");
        assertEquals(false, leaderBoard.canBoatsOfCompetitorsChangePerRace(),
                "read: leaderboard.canBoatsOfCompetitorsChangePerRace is different");
        assertNull(leaderBoard.getMaxCompetitorsCount(), "read: leaderboard.maxCompetitorsCount should be null");
        assertNull(leaderBoard.getScoringComment(), "read: leaderboard.scoringComment should be null");
        assertNull(leaderBoard.getLastScoringUpdate(), "read: leaderboard.lastScoringUpdate should be null");
        assertEquals(0, leaderBoard.getColumnNames().length, "read: leaderboard.columnNames should be empty");
        assertEquals(0, ((JSONArray) leaderBoard.get("competitors")).size(),
                "read: leaderboard.competitors should be empty");
        assertEquals("/leaderboard/" + LEADERBOARD_NAME.replaceAll(" ", "_").replaceAll("<", "_").replaceAll(">", "_"),
                leaderBoard.get("shardingLeaderboardName"),
                "read: leaderboard.shardingLeaderboardName is different");
    }

    @SeleniumTestCase
    public void testUpdatingLeaderboard() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        eventApi.createEvent(ctx, LEADERBOARD_NAME, BOATCLASSNAME, CLOSED, "default");
        final Leaderboard leaderboardOriginal = leaderboardApi.getLeaderboard(ctx, LEADERBOARD_NAME);

        // updating resultDiscardingThresholds
        final int[] newResultDiscardingThresholds = new int[] { 1, 2, 3, 4, 5 };
        leaderboardApi.updateLeaderboard(ctx, LEADERBOARD_NAME, leaderboardOriginal.getDisplayName(),
                newResultDiscardingThresholds);

        // reloading and check if updated
        Leaderboard leaderboardReloaded = leaderboardApi.getLeaderboard(ctx, LEADERBOARD_NAME);
        assertArrayEquals(newResultDiscardingThresholds,
                leaderboardReloaded.getDiscardIndexResultsStartingWithHowManyRaces());

        // remove and check again
        final int[] newEmptyResultDiscardingThresholds = new int[] {};
        leaderboardApi.updateLeaderboard(ctx, LEADERBOARD_NAME, leaderboardOriginal.getDisplayName(),
                newEmptyResultDiscardingThresholds);
        leaderboardReloaded = leaderboardApi.getLeaderboard(ctx, LEADERBOARD_NAME);
        assertArrayEquals(newEmptyResultDiscardingThresholds,
                leaderboardReloaded.getDiscardIndexResultsStartingWithHowManyRaces());

        // passing null should not change anything
        leaderboardApi.updateLeaderboard(ctx, LEADERBOARD_NAME, leaderboardOriginal.getDisplayName(),
                newResultDiscardingThresholds);
        leaderboardReloaded = leaderboardApi.getLeaderboard(ctx, LEADERBOARD_NAME);
        assertArrayEquals(newResultDiscardingThresholds,
                leaderboardReloaded.getDiscardIndexResultsStartingWithHowManyRaces());
        leaderboardApi.updateLeaderboard(ctx, LEADERBOARD_NAME, leaderboardOriginal.getDisplayName(), null);
        assertArrayEquals(newResultDiscardingThresholds,
                leaderboardReloaded.getDiscardIndexResultsStartingWithHowManyRaces());

        // check that no other atributes have been changed
        assertEquals(leaderboardOriginal.getName(), leaderboardReloaded.getName());
        assertEquals(leaderboardOriginal.getDisplayName(), leaderboardReloaded.getDisplayName());
        
        // update leaderboardDisplayName
        leaderboardApi.updateLeaderboard(ctx, LEADERBOARD_NAME, "another display name", null);
        leaderboardReloaded = leaderboardApi.getLeaderboard(ctx, LEADERBOARD_NAME);
        assertEquals("another display name", leaderboardReloaded.getDisplayName());
        
        // check that no other atributes have been changed
        assertEquals(leaderboardOriginal.getName(), leaderboardReloaded.getName());
        assertArrayEquals(newResultDiscardingThresholds,
                leaderboardReloaded.getDiscardIndexResultsStartingWithHowManyRaces());
        
        // update leaderboardDisplayName and resultDiscardingThresholds
        final int[] newResultDiscardingThresholds2 = new int[] {7, 8};
        leaderboardApi.updateLeaderboard(ctx, LEADERBOARD_NAME, "another display name 2", newResultDiscardingThresholds2);
        leaderboardReloaded = leaderboardApi.getLeaderboard(ctx, LEADERBOARD_NAME);
        assertEquals("another display name 2", leaderboardReloaded.getDisplayName());
        assertArrayEquals(newResultDiscardingThresholds2, newResultDiscardingThresholds2);

        // update leaderboardDisplayName to null, this result in the name of the leaderboard
        leaderboardApi.updateLeaderboard(ctx, LEADERBOARD_NAME, /* leaderboardDisplayName */ null, newResultDiscardingThresholds2);
        leaderboardReloaded = leaderboardApi.getLeaderboard(ctx, LEADERBOARD_NAME);
        // if display name is null, the name of the leaderboard is returned
        assertEquals(leaderboardReloaded.getName(), leaderboardReloaded.getDisplayName());
    }

    @SeleniumTestCase
    public void testUpdatingLeaderboardDisplayNameToNull() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        eventApi.createEvent(ctx, LEADERBOARD_NAME, BOATCLASSNAME, CLOSED, "default");
        
        // update to not null values
        final int[] newResultDiscardingThresholds = new int[] { 1, 2, 3, 4, 5 };
        leaderboardApi.updateLeaderboard(ctx, LEADERBOARD_NAME, "newLeaderboardDisplayName",
                newResultDiscardingThresholds);
        Leaderboard leaderboardReloaded = leaderboardApi.getLeaderboard(ctx, LEADERBOARD_NAME);
        assertEquals("newLeaderboardDisplayName", leaderboardReloaded.getDisplayName());
        assertArrayEquals(newResultDiscardingThresholds,
                leaderboardReloaded.getDiscardIndexResultsStartingWithHowManyRaces());

        // update to display name to null
        leaderboardApi.updateLeaderboardDisplayName(ctx, LEADERBOARD_NAME, null);
        leaderboardReloaded = leaderboardApi.getLeaderboard(ctx, LEADERBOARD_NAME);
        assertEquals(LEADERBOARD_NAME, leaderboardReloaded.getDisplayName());
        assertArrayEquals(newResultDiscardingThresholds,
                leaderboardReloaded.getDiscardIndexResultsStartingWithHowManyRaces());
    }

    @SeleniumTestCase
    public void testDeviceMappingStartAndEnd() throws Exception {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        eventApi.createEvent(ctx, LEADERBOARD_NAME, BOATCLASSNAME, CLOSED, "default");
        final Competitor competitor = regattaApi.createAndAddCompetitor(ctx, LEADERBOARD_NAME, BOATCLASSNAME, "test@de",
                "Max Mustermann", "USA");
        final DeviceMappingRequest request = leaderboardApi.createDeviceMappingRequest(ctx, LEADERBOARD_NAME)
                .forCompetitor(competitor.getId()).withDeviceUuid(randomUUID());
        request.startDeviceMapping(currentTimeMillis());
        request.endDeviceMapping(currentTimeMillis());
    }

    @SeleniumTestCase
    public void testSetTrackingTimes() throws Exception {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        eventApi.createEvent(ctx, LEADERBOARD_NAME, BOATCLASSNAME, CLOSED, "default");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, LEADERBOARD_NAME, null, 1)[0];
        final Long startTime = currentTimeMillis();
        final Long endTime = currentTimeMillis() + 10000L;
        leaderboardApi.startRaceLogTracking(ctx, LEADERBOARD_NAME, race.getRaceName(), "Default");
        TrackingTimes trackingTimes = leaderboardApi.setTrackingTimes(ctx, LEADERBOARD_NAME, race.getRaceName(),
                "Default", startTime, endTime);
        assertEquals(startTime, trackingTimes.getStartOfTracking(), "read: startTime is different");
        assertEquals(endTime, trackingTimes.getEndOfTracking(), "read: endTime is different");
    }

    @SeleniumTestCase
    public void testRaceStartTime() throws Exception {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        eventApi.createEvent(ctx, LEADERBOARD_NAME, BOATCLASSNAME, CLOSED, "default");
        final Integer passID = 1;
        final RaceColumn[] races = regattaApi.addRaceColumn(ctx, LEADERBOARD_NAME, null, 2);
        final RaceColumn race1 = races[0], race2 = races[1];
        final Long startTimeRace1 = currentTimeMillis();
        final Long startTimeRace2 = currentTimeMillis() + 10000;
        final Long effectiveStartTimeRace1 = leaderboardApi.setStartTime(ctx, LEADERBOARD_NAME, race1.getRaceName(),
                "Default", startTimeRace1, passID, RacingProcedureType.BASIC, "Administrator", 1);
        final Long effectiveStartTimeRace2 = leaderboardApi.setStartTime(ctx, LEADERBOARD_NAME, race2.getRaceName(),
                "Default", startTimeRace2, passID, RacingProcedureType.BASIC, "Administrator", 1);
        assertNotNull(effectiveStartTimeRace1);
        final StartTime reloadedStartTimeRace1 = leaderboardApi.getStartTime(ctx, LEADERBOARD_NAME, race1.getRaceName(),
                "Default");
        final StartTime reloadedStartTimeRace2 = leaderboardApi.getStartTime(ctx, LEADERBOARD_NAME, race2.getRaceName(),
                "Default");
        assertEquals(reloadedStartTimeRace1.getStartTimeAsMillis(), effectiveStartTimeRace1);
        assertEquals(reloadedStartTimeRace2.getStartTimeAsMillis(), effectiveStartTimeRace2);
        assertEquals(reloadedStartTimeRace1.getPassId(), passID);
        assertEquals(reloadedStartTimeRace2.getPassId(), passID);
        assertEquals(reloadedStartTimeRace1.getRacingProcedureType(), RacingProcedureType.BASIC);
        assertEquals(reloadedStartTimeRace2.getRacingProcedureType(), RacingProcedureType.BASIC);
    }
}
