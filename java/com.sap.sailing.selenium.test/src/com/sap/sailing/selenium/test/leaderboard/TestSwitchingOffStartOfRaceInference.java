package com.sap.sailing.selenium.test.leaderboard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;

import com.sap.sailing.selenium.core.SeleniumTestCase;
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
            "http://event2.tractrac.com/events/event_20120803_BMWCup/jsonservice.php"; //$NON-NLS-1$
    private static final String REGATTA = "The BMW Cup"; //$NON-NLS-1$
    private static final String LEADERBOARD = "The BMW Cup (J80)"; //$NON-NLS-1$
    private static final String EVENT = "BMW Cup"; //$NON-NLS-1$
    private static final String BOAT_CLASS = "J80"; //$NON-NLS-1$
    private static final String RACE = "BMW Cup Race 3"; //$NON-NLS-1$
    
    private RegattaDescriptor regatta;
    private TrackableRaceDescriptor trackableRace;
    private TrackedRaceDescriptor trackedRace;
    private RaceDescriptor raceColumn;
    
    @Override
    @BeforeEach
    public void setUp() {
        this.regatta = new RegattaDescriptor(REGATTA, BOAT_CLASS);
        this.trackableRace = new TrackableRaceDescriptor(EVENT,  RACE, BOAT_CLASS);
        this.trackedRace = new TrackedRaceDescriptor(this.regatta.toString(), BOAT_CLASS, RACE);
        this.raceColumn = new RaceDescriptor("D3", "Default", false, false, 0.0);
        clearState(getContextRoot());
        super.setUp();
        configureRegattaAndLeaderboard();
    }
    
    @SeleniumTestCase
    public void testCorrectDisplayOfRaceColumnWithAndWithoutStartTimeInference() {
        this.environment.getWindowManager().withExtraWindow(getWebDriver(), (adminConsoleWindow, leaderboardWindow) -> {
            final WebDriver leaderboardWindowDriver = leaderboardWindow.switchToWindow();
            setUpAuthenticatedSession(leaderboardWindowDriver);
            LeaderboardPage leaderboard = LeaderboardPage.goToPage(leaderboardWindowDriver, getContextRoot(), LEADERBOARD, /* race details */ false);
            LeaderboardTablePO leaderboardTable = leaderboard.getLeaderboardTable();
            List<String> races = leaderboardTable.getRaceNames();
            assertThat("Expected only D3", races, equalTo(Arrays.asList("D3")));
            int d3ColumnIndex = leaderboardTable.getColumnIndex("D3");
            for (LeaderboardEntry e : leaderboardTable.getEntries()) {
                String raceColumnContent = e.getColumnContent(d3ColumnIndex);
                assertTrue(Integer.parseInt(raceColumnContent) > 0); // all competitors have a positive score in R3
            }

            WebDriver driver = adminConsoleWindow.switchToWindow();
            AdminConsolePage adminConsole = AdminConsolePage.goToPage(driver, getContextRoot());
            // Go to the administration console and unset the "useStartTimeInference" flag
            RegattaStructureManagementPanelPO regattaManagementPanel = adminConsole.goToRegattaStructure();
            RegattaListCompositePO regattaList = regattaManagementPanel.getRegattaList();
            RegattaEditDialogPO regattaEditDialog = regattaList.editRegatta(regatta);
            regattaEditDialog.setUseStartTimeInference(false);
            regattaEditDialog.pressOk();
            
            leaderboardWindow.switchToWindow();
            leaderboard.refresh();
            for (LeaderboardEntry e : leaderboardTable.getEntries()) {
                String raceColumnContent = e.getColumnContent(d3ColumnIndex);
                assertEquals("", raceColumnContent); // all competitors have an empty score because there is no start time anymore
            }
        });
    }
    
    private void configureRegattaAndLeaderboard() {
        // Open the admin console for some configuration steps
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        // Create a regatta with 1 series and 5 races as well as a leaderboard
        RegattaStructureManagementPanelPO regattaStructure = adminConsole.goToRegattaStructure();
        regattaStructure.createRegatta(this.regatta, /* withDefaultLeaderboard */ false);
        RegattaDetailsCompositePO regattaDetails = regattaStructure.getRegattaDetails(this.regatta);
        SeriesEditDialogPO seriesDialog = regattaDetails.editSeries(RegattaStructureManagementPanelPO.DEFAULT_SERIES_NAME);
        seriesDialog.addRaces(3, 3);
        seriesDialog.pressOk();
        regattaDetails.deleteSeries("Default");
        // Start the tracking for the races and wait until they are ready to use
        TracTracEventManagementPanelPO tracTracEvents = adminConsole.goToTracTracEvents();
        tracTracEvents.addConnectionAndListTrackableRaces(BMW_CUP_JSON_URL);
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
    }
}
