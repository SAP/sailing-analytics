package com.sap.sailing.selenium.test.leaderboard;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sap.sailing.selenium.core.WebDriverWindow;
import com.sap.sailing.selenium.core.WindowManager;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO.RaceDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaDetailsCompositePO;
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

public class TestLeaderboardConfiguration extends AbstractSeleniumTest {
    private static final String IDM_5O5_2013_JSON_URL =
            "http://traclive.dk/events/event_20130917_IDMO/jsonservice.php"; //$NON-NLS-1$
    
    private static final String REGATTA = "IDM 2013"; //$NON-NLS-1$
    
    private static final String LEADERBOARD = "IDM 2013 (5O5)"; //$NON-NLS-1$
    
    private static final String EVENT = "IDM 5O5 2013"; //$NON-NLS-1$
    private static final String BOAT_CLASS = "5O5"; //$NON-NLS-1$
    private static final String RACE = "Race %d"; //$NON-NLS-1$
    
    private RegattaDescriptor regatta;
    
    private List<TrackableRaceDescriptor> trackableRaces;
    private List<TrackedRaceDescriptor> trackedRaces;
    private List<RaceDescriptor> leaderboardRaces;
    
    @Override
    @Before
    public void setUp() {
        this.regatta = new RegattaDescriptor(REGATTA, BOAT_CLASS);
        this.trackableRaces = new ArrayList<>();
        this.trackedRaces = new ArrayList<>();
        this.leaderboardRaces = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String raceName = String.format(RACE, i);
            TrackableRaceDescriptor trackableRace = new TrackableRaceDescriptor(EVENT,  raceName, BOAT_CLASS);
            TrackedRaceDescriptor trackedRace = new TrackedRaceDescriptor(this.regatta.toString(), BOAT_CLASS, raceName);
            RaceDescriptor leaderboardRace = new RaceDescriptor(String.format("D%s", i), "Default", false, false, 0);
            
            this.trackableRaces.add(trackableRace);
            this.trackedRaces.add(trackedRace);
            this.leaderboardRaces.add(leaderboardRace);
        }
        clearState(getContextRoot());
        super.setUp();
        configureLeaderboard();
    }
    
    @Test
    public void testDynamicRaceLinking() {
        withExtraWindow((leaderboardWindow, adminConsoleWindow) -> {
            // Open the leaderboard and check for "empty" leaderboard
            LeaderboardPage leaderboard = LeaderboardPage.goToPage(getWebDriver(), getContextRoot(), LEADERBOARD, false);
            LeaderboardTablePO table = leaderboard.getLeaderboardTable();
            List<String> races = table.getRaceNames();
            
            assertThat(races.size(), equalTo(5));
            assertThat(table.getEntries().size(), equalTo(28)); // the regatta already has the races linked; regatta leaderboard obtains competitors from regatta 
            adminConsoleWindow.switchToWindow();
            AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
            LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
            LeaderboardDetailsPanelPO leaderboardDetails = leaderboardConfiguration.getLeaderboardDetails(this.regatta.toString());
            
            Integer[] expectedPointsForFindelJens = new Integer[] {2, 18, 12, 4, 7};
            Integer[] expectedRankForFindelJens = new Integer[] {2, 9, 11, 7, 6}; // Bogacki with no score in R1 expected to end up at end of leaderboard
            
            // Link the races and check the leaderboard again
            for (int i = 0; i < 5; i++) {
                leaderboardDetails.linkRace(this.leaderboardRaces.get(i), this.trackedRaces.get(i));
                leaderboardWindow.switchToWindow();
                leaderboard.refresh();
                List<LeaderboardEntry> allEntries = table.getEntries();
                LeaderboardEntry findelJens = table.getEntry("8875");
                Integer points = findelJens.getPointsForRace(String.format("D%s", i + 1));
                Integer rank = findelJens.getTotalRank();
                // Assertions
                assertThat("Number of competitors does not match",
                        allEntries.size(), equalTo(28));
                assertThat("Points for race 'D" + (i + 1) + "' do not match for competitor '8875' (Findel, Jens)",
                        points, equalTo(expectedPointsForFindelJens[i]));
                assertThat("Total rank after " + (i + 1) + " race(s) does not match for competitor '8875' (Findel, Jens)",
                        rank, equalTo(expectedRankForFindelJens[i]));
                adminConsoleWindow.switchToWindow();
            }
        });
    }
    
    @Test
    public void testDynamicRaceDeletion() {
        withExtraWindow((adminConsoleWindow, leaderboardWindow) -> {
            // Go to the administration console and link all 5 races
            AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
            LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
            LeaderboardDetailsPanelPO leaderboardDetails = leaderboardConfiguration.getLeaderboardDetails(this.regatta.toString());
            leaderboardDetails.linkRace(this.leaderboardRaces.get(0), this.trackedRaces.get(0));
            leaderboardDetails.linkRace(this.leaderboardRaces.get(1), this.trackedRaces.get(1));
            leaderboardDetails.linkRace(this.leaderboardRaces.get(2), this.trackedRaces.get(2));
            leaderboardDetails.linkRace(this.leaderboardRaces.get(3), this.trackedRaces.get(3));
            leaderboardDetails.linkRace(this.leaderboardRaces.get(4), this.trackedRaces.get(4));
            // Open the leaderboard in our second window
            leaderboardWindow.switchToWindow();
            LeaderboardPage leaderboard = LeaderboardPage.goToPage(getWebDriver(), getContextRoot(), LEADERBOARD, false);
            LeaderboardTablePO table = leaderboard.getLeaderboardTable();
            // Go back to the administration console and delete third race
            adminConsoleWindow.switchToWindow();
            RegattaStructureManagementPanelPO regattaStructure = adminConsole.goToRegattaStructure();
            RegattaDetailsCompositePO regattaDetails = regattaStructure.getRegattaDetails(this.regatta);
            SeriesEditDialogPO seriesDialog = regattaDetails.editSeries(RegattaStructureManagementPanelPO.DEFAULT_SERIES_NAME);
            seriesDialog.deleteRace("D3");
            seriesDialog.pressOk(true);
            final List<String> expectedRaces = Arrays.asList("D1", "D2", "D4", "D5");
            regattaDetails.waitForRacesOfSeries(RegattaStructureManagementPanelPO.DEFAULT_SERIES_NAME, expectedRaces);
            // Now we can check the result with our expectation
            leaderboardWindow.switchToWindow();
            leaderboard.refresh();
            assertThat("Race names do not match after deletion of race 'D3'", table.getRaceNames(),
                    equalTo(expectedRaces));
        });
    }
    
    @Ignore("This test belongs to bug 1892 and currently fails. It is currently enabled on branch bug1892.")
    @Test
    public void testDynamicRenamingOfRace() {
        withExtraWindow((adminConsoleWindow, leaderboardWindow) -> {
            // Go to the administration console and link all 5 races
            AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
            LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
            LeaderboardDetailsPanelPO leaderboardDetails = leaderboardConfiguration.getLeaderboardDetails(this.regatta.toString());
            
            leaderboardDetails.linkRace(this.leaderboardRaces.get(0), this.trackedRaces.get(0));
            leaderboardDetails.linkRace(this.leaderboardRaces.get(1), this.trackedRaces.get(1));
            leaderboardDetails.linkRace(this.leaderboardRaces.get(2), this.trackedRaces.get(2));
            leaderboardDetails.linkRace(this.leaderboardRaces.get(3), this.trackedRaces.get(3));
            leaderboardDetails.linkRace(this.leaderboardRaces.get(4), this.trackedRaces.get(4));
            
            // Open the leaderboard in our second window
            leaderboardWindow.switchToWindow();
            
            LeaderboardPage leaderboard = LeaderboardPage.goToPage(getWebDriver(), getContextRoot(), LEADERBOARD, false);
            LeaderboardTablePO table = leaderboard.getLeaderboardTable();
            
            // Go back to the administration console and rename the first race
            adminConsoleWindow.switchToWindow();
            
            RegattaStructureManagementPanelPO regattaStructure = adminConsole.goToRegattaStructure();
            RegattaDetailsCompositePO regattaDetails = regattaStructure.getRegattaDetails(this.regatta);
            SeriesEditDialogPO seriesDialog = regattaDetails.editSeries(RegattaStructureManagementPanelPO.DEFAULT_SERIES_NAME);
            seriesDialog.renameRace("D1", "Q");
            seriesDialog.pressOk(true);
            
            // Now we can check the result with our expectation
            leaderboardWindow.switchToWindow();
            leaderboard.refresh();
            List<String> races = table.getRaceNames();
            assertThat("Race names do not match after renaming race 'D1' to 'Q'",
                    races, equalTo(Arrays.asList("Q", "D2", "D3", "D4", "D5")));
        });
    }
    
    private void withExtraWindow(BiConsumer<WebDriverWindow, WebDriverWindow> defaultAndExtraWindow) {
        final WindowManager manager = this.environment.getWindowManager();
        final WebDriverWindow defaultWindow = manager.getCurrentWindow();
        final WebDriverWindow extraWindow = manager.openNewWindow();
        try {
            extraWindow.switchToWindow();
            // On WebDriver implementations that support it, the window is maximized
            // This makes our Tests work much better on Windows
            getWebDriver().manage().window().maximize();
        } catch (Exception e) {
            // maximizing isn't supported on all Systems / WebDriver implementations
        } finally {
            defaultWindow.switchToWindow();
        }
        try {
            defaultAndExtraWindow.accept(defaultWindow, extraWindow);
        } finally {
            extraWindow.close();
            defaultWindow.switchToWindow();
        }
    }
    
    private void configureLeaderboard() {
        // Open the admin console for some configuration steps
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        
        // Create a regatta with 1 series and 5 races as well as a leaderborad
        RegattaStructureManagementPanelPO regattaStructure = adminConsole.goToRegattaStructure();
        regattaStructure.createRegatta(this.regatta);
        
        RegattaDetailsCompositePO regattaDetails = regattaStructure.getRegattaDetails(this.regatta);
        SeriesEditDialogPO seriesDialog = regattaDetails.editSeries(RegattaStructureManagementPanelPO.DEFAULT_SERIES_NAME);
        seriesDialog.addRaces(1, 5);
        seriesDialog.pressOk();
        
        regattaDetails.deleteSeries("Default");
        
        LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
        leaderboardConfiguration.createRegattaLeaderboard(this.regatta);
        
        // Start the tracking for the races and wait until they are ready to use
        TracTracEventManagementPanelPO tracTracEvents = adminConsole.goToTracTracEvents();
        tracTracEvents.listTrackableRaces(IDM_5O5_2013_JSON_URL);
        tracTracEvents.setReggataForTracking(this.regatta);
        tracTracEvents.setTrackSettings(false, false, false);
        // TODO: There exists a bug in Selenium with key modifiers (Issue 3734 and 6817), so we can't use multi
        //       selection (Firefox on Windows)
        //tracTracEvents.startTrackingForRaces(this.trackableRaces);
        for(TrackableRaceDescriptor race : this.trackableRaces) {
            tracTracEvents.startTrackingForRace(race);
        }
        
        TrackedRacesListPO trackedRacesList = tracTracEvents.getTrackedRacesList();
        trackedRacesList.waitForTrackedRaces(this.trackedRaces, Status.FINISHED); // TracAPI puts REPLAY races into FINISHED mode when done loading
        // TODO: There exists a bug in Selenium with key modifiers (Issue 3734 and 6817), so we can't use multi
        //       selection (Firefox on Windows)
        //trackedRacesList.stopTracking(this.trackedRaces);
        for (TrackedRaceDescriptor race : this.trackedRaces) {
            trackedRacesList.stopTracking(race);
        }
    }
}
