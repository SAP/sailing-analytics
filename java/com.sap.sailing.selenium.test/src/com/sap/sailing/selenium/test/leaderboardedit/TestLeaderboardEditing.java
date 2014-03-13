package com.sap.sailing.selenium.test.leaderboardedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO.RaceDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO.TrackedRaceDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.TracTracEventManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.TracTracEventManagementPanelPO.TrackableRaceDescriptor;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.pages.leaderboardedit.LeaderboardEditingPage;
import com.sap.sailing.selenium.pages.leaderboardedit.LeaderboardTable;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestLeaderboardEditing extends AbstractSeleniumTest {
    private static final String LEADERBOARD = "BMW Cup - J80";
    
    private static final String BMW_CUP_JSON_URL = "http://kml.skitrac.traclive.dk/events/event_20120803_BMWCup/jsonservice.php"; //$NON-NLS-1$
    private static final String BMW_CUP_RACE_8 = "BMW Cup Race 8";
    private static final String BMW_CUP_REGATTA = "BMW Cup";
    private static final String BMW_CUP_BOAT_CLASS = "J80";
    
    private static final TrackableRaceDescriptor TRACKABLE_RACE = new TrackableRaceDescriptor(
            BMW_CUP_REGATTA, BMW_CUP_RACE_8, BMW_CUP_BOAT_CLASS);
    private static final TrackedRaceDescriptor TRACKED_RACE = new TrackedRaceDescriptor(
            BMW_CUP_REGATTA + " (" + BMW_CUP_BOAT_CLASS + ")", BMW_CUP_BOAT_CLASS, BMW_CUP_RACE_8);
    private static final RaceDescriptor RACE_COLUMN = new RaceDescriptor("R1", "Default", false, false, 0.0);
    
    @Before
    public void clearDatabase() {
        clearState(getContextRoot());
    }
    
    @Test
    public void testSimpleLeaderboardEditing() {
        createNewLeaderboardLoadRaceAndLink(LEADERBOARD);
        
        LeaderboardEditingPage page = LeaderboardEditingPage.goToPage(LEADERBOARD, getWebDriver(), getContextRoot());
        LeaderboardTable table = page.getLeaderboardTable();
        assertNotNull(table);
        Iterable<DataEntryPO> rows = table.getRows();
        assertEquals("Expected 6 rows for the six competitors but found only "+Util.size(rows), 6, Util.size(rows));
    }
    
    private void createNewLeaderboardLoadRaceAndLink(String leaderboardName) {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        
        createLeaderboard(leaderboardName, adminConsole);
        startTrackingRaceAndWait(adminConsole, BMW_CUP_JSON_URL, TRACKABLE_RACE, TRACKED_RACE, 30000 /* 30s timeout */);
        // now create two race columns
        LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
        LeaderboardDetailsPanelPO leaderboardDetails = leaderboardConfiguration.getLeaderboardDetails(leaderboardName);
        leaderboardDetails.addRacesToFlexibleLeaderboard(2);
        leaderboardDetails.linkRace(RACE_COLUMN, TRACKED_RACE);
    }

    private void startTrackingRaceAndWait(AdminConsolePage adminConsole, String jsonUrl, TrackableRaceDescriptor trackableRace,
            TrackedRaceDescriptor trackedRace, long waitingTime) {
        TracTracEventManagementPanelPO tracTracEvents = adminConsole.goToTracTracEvents();
        tracTracEvents.listTrackableRaces(jsonUrl);
        captureScreenshot("beforeStartTracking");
        tracTracEvents.setTrackSettings(false, true, false);
        tracTracEvents.startTrackingForRace(trackableRace);
        captureScreenshot("afterStartTracking");
        TrackedRacesListPO trackedRacesList = tracTracEvents.getTrackedRacesList();
        trackedRacesList.waitForTrackedRace(trackedRace);
        trackedRacesList.stopTracking(trackedRace);
    }
    
    private void createLeaderboard(String leaderboardName, AdminConsolePage adminConsole) {
        LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
        leaderboardConfiguration.createFlexibleLeaderboard(leaderboardName);
    }
}
