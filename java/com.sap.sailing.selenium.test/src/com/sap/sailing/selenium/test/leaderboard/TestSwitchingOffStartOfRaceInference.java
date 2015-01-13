package com.sap.sailing.selenium.test.leaderboard;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.selenium.core.WebDriverWindow;
import com.sap.sailing.selenium.core.WindowManager;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO.RaceDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaDetailsCompositePO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaEditDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaStructureManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.SeriesEditDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO.Status;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO.TrackedRaceDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.TracTracEventManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.TracTracEventManagementPanelPO.TrackableRaceDescriptor;
import com.sap.sailing.selenium.pages.leaderboard.LeaderboardPage;
import com.sap.sailing.selenium.pages.leaderboard.LeaderboardTablePO;
import com.sap.sailing.selenium.pages.leaderboard.LeaderboardTablePO.LeaderboardEntry;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestSwitchingOffStartOfRaceInference extends AbstractSeleniumTest {
    private static final String BMW_CUP_JSON_URL =
            "http://kml.skitrac.traclive.dk/events/event_20120803_BMWCup/jsonservice.php"; //$NON-NLS-1$
    private static final String REGATTA = "The BMW Cup"; //$NON-NLS-1$
    private static final String LEADERBOARD = "The BMW Cup (J80)"; //$NON-NLS-1$
    private static final String EVENT = "BMW Cup"; //$NON-NLS-1$
    private static final String BOAT_CLASS = "J80"; //$NON-NLS-1$
    private static final String RACE = "BMW Cup Race 3"; //$NON-NLS-1$
    
    private RegattaDescriptor regatta;
    private TrackableRaceDescriptor trackableRace;
    private TrackedRaceDescriptor trackedRace;
    private RaceDescriptor raceColumn;
    private AdminConsolePage adminConsole;
    
    @Override
    @Before
    public void setUp() {
        this.regatta = new RegattaDescriptor(REGATTA, BOAT_CLASS);
        this.trackableRace = new TrackableRaceDescriptor(EVENT,  RACE, BOAT_CLASS);
        this.trackedRace = new TrackedRaceDescriptor(this.regatta.toString(), BOAT_CLASS, RACE);
        this.raceColumn = new RaceDescriptor("R3", "Default", false, false, 0.0);
        clearState(getContextRoot());
        super.setUp();
        adminConsole = configureRegattaAndLeaderboard();
    }
    
    @Test
    public void testCorrectDisplayOfRaceColumnWithAndWithoutStartTimeInference() {
        WindowManager manager = this.environment.getWindowManager();
        WebDriverWindow adminConsoleWindow = manager.getCurrentWindow();
        WebDriverWindow leaderboardWindow = manager.openNewWindow();
        leaderboardWindow.switchToWindow();
        LeaderboardPage leaderboard = LeaderboardPage.goToPage(getWebDriver(), getContextRoot(), LEADERBOARD, /* race details */ false);
        LeaderboardTablePO leaderboardTable = leaderboard.getLeaderboardTable();
        List<String> races = leaderboardTable.getRaceNames();
        assertThat("Expected only R3", races, equalTo(Arrays.asList("R3")));
        int r3ColumnIndex = leaderboardTable.getColumnIndex("R3");
        for (LeaderboardEntry e : leaderboardTable.getEntries()) {
            String raceColumnContent = e.getColumnContent(r3ColumnIndex);
            assertTrue(Integer.parseInt(raceColumnContent) > 0); // all competitors have a positive score in R3
        }
        adminConsoleWindow.switchToWindow();
        // Go to the administration console and unset the "useStartTimeInference" flag
        RegattaStructureManagementPanelPO regattaManagementPanel = adminConsole.goToRegattaStructure();
        RegattaListCompositePO regattaList = regattaManagementPanel.getRegattaList();
        RegattaEditDialogPO regattaEditDialog = regattaList.editRegatta(regatta);
        regattaEditDialog.setUseStartTimeInference(false);
        regattaEditDialog.pressOk();
        leaderboardWindow.switchToWindow();
        leaderboard.refresh();
        for (LeaderboardEntry e : leaderboardTable.getEntries()) {
            String raceColumnContent = e.getColumnContent(r3ColumnIndex);
            assertEquals("", raceColumnContent); // all competitors have an empty score because there is no start time anymore
        }
        leaderboardWindow.close();
    }
    
    private AdminConsolePage configureRegattaAndLeaderboard() {
        // Open the admin console for some configuration steps
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        // Create a regatta with 1 series and 5 races as well as a leaderboard
        RegattaStructureManagementPanelPO regattaStructure = adminConsole.goToRegattaStructure();
        regattaStructure.createRegatta(this.regatta);
        RegattaDetailsCompositePO regattaDetails = regattaStructure.getRegattaDetails(this.regatta);
        SeriesEditDialogPO seriesDialog = regattaDetails.editSeries(RegattaStructureManagementPanelPO.DEFAULT_SERIES_NAME);
        seriesDialog.addRaces(3, 3);
        seriesDialog.pressOk();
        // Start the tracking for the races and wait until they are ready to use
        TracTracEventManagementPanelPO tracTracEvents = adminConsole.goToTracTracEvents();
        tracTracEvents.listTrackableRaces(BMW_CUP_JSON_URL);
        tracTracEvents.setReggataForTracking(this.regatta);
        tracTracEvents.setTrackSettings(false, false, false);
        tracTracEvents.startTrackingForRace(this.trackableRace);
        TrackedRacesListPO trackedRacesList = tracTracEvents.getTrackedRacesList();
        trackedRacesList.waitForTrackedRace(this.trackedRace, Status.FINISHED); // TracAPI puts REPLAY races into FINISHED mode when done loading
        trackedRacesList.stopTracking(this.trackedRace);
        LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
        leaderboardConfiguration.createRegattaLeaderboard(this.regatta);
        LeaderboardDetailsPanelPO leaderboardDetails = leaderboardConfiguration.getLeaderboardDetails(LEADERBOARD);
        leaderboardDetails.linkRace(this.raceColumn, this.trackedRace);
        return adminConsole;
    }
}
