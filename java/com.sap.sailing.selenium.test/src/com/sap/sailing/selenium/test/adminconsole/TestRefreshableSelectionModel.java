package com.sap.sailing.selenium.test.adminconsole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.connectors.SmartphoneTrackingEventManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO.RaceDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaDetailsCompositePO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaStructureManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.SeriesEditDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.RaceColumnTableWrapperPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesCompetitorEditDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesCompetitorTablePO.CompetitorEntry;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesCompetitorsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO.TrackedRaceDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.TracTracEventManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.TracTracEventManagementPanelPO.TrackableRaceDescriptor;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestRefreshableSelectionModel extends AbstractSeleniumTest {
    // TestMaintenanceOfSelectionAfterDataChanges
    private CompetitorEntry competitorEntry;
    private CompetitorEntry competitorEntryToSelect;

    // TestRefreshOfDependingUIElements
    private static final String REGATTA = "IDM 2013"; //$NON-NLS-1$

    private static final String LEADERBOARD = "IDM 2013 (5O5)"; //$NON-NLS-1$

    private static final String EVENT = "IDM 5O5 2013"; //$NON-NLS-1$
    private static final String BOAT_CLASS = "5O5"; //$NON-NLS-1$
    private static final String RACE = "Race %d"; //$NON-NLS-1$
    
    private static final String IDM_5O5_2013_JSON_URL =
            "https://event.tractrac.com/events/event_20130917_IDMO/jsonservice.php"; //$NON-NLS-1$

    private RegattaDescriptor regatta;

    private List<TrackableRaceDescriptor> trackableRaces;
    private List<TrackedRaceDescriptor> trackedRaces;
    private List<RaceDescriptor> leaderboardRaces;

    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
    }

    private TrackedRacesCompetitorsPanelPO goToCompetitorsPanel(WebDriver driver) {
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(driver, getContextRoot());
        final TrackedRacesCompetitorsPanelPO competitorsPanel = adminConsole.goToTrackedRacesCompetitors();
        return competitorsPanel;
    }

    @SeleniumTestCase
    public void testMaintenanceOfSelectionAfterDataChanges() {
        this.environment.getWindowManager().withExtraWindow(getWebDriver(), (windowForSelection, windowForEdit) -> {
            final WebDriver windowForEditDriver = windowForEdit.switchToWindow();
            setUpAuthenticatedSession(windowForEditDriver);
            final TrackedRacesCompetitorsPanelPO competitorsPanel = goToCompetitorsPanel(
                    windowForEditDriver);
            for (int i = 0; i < 2; i++) {
                TrackedRacesCompetitorEditDialogPO dialog = competitorsPanel.pushAddCompetitorButton();
                dialog.setNameTextBox("" + System.currentTimeMillis());
                dialog.setShortNameTextBox("" + System.currentTimeMillis());
                dialog.setWithBoat(false);
                dialog.pressOk();
            }
            TrackedRacesCompetitorEditDialogPO dialog = competitorsPanel.pushAddCompetitorButton();
            final String name = "" + System.currentTimeMillis();
            dialog.setNameTextBox(name);
            final String shortName = "" + System.currentTimeMillis();
            dialog.setShortNameTextBox(shortName);
            dialog.setWithBoat(false);
            dialog.pressOk();
            boolean found = false;
            for (final CompetitorEntry it : competitorsPanel.getCompetitorTable().getEntries()) {
                String itName = it.getName();
                if (itName.equals(name)) {
                    found = true;
                    competitorEntry = it;
                    break;
                }
            }
            assertTrue(found);
            TrackedRacesCompetitorsPanelPO competitorPanelForSelection = goToCompetitorsPanel(
                    windowForSelection.switchToWindow());
            found = false;
            for (final CompetitorEntry it : competitorPanelForSelection.getCompetitorTable().getEntries()) {
                String itName = it.getName();
                if (itName.equals(name)) {
                    found = true;
                    competitorEntryToSelect = it;
                    break;
                }
            }
            assertTrue(found);
            competitorPanelForSelection.getCompetitorTable().selectEntry(competitorEntryToSelect);
            assertEquals(1, competitorPanelForSelection.getCompetitorTable().getSelectedEntries().size());
            assertEquals(name, competitorPanelForSelection.getCompetitorTable().getSelectedEntries().get(0).getName());
            assertEquals(shortName, competitorPanelForSelection.getCompetitorTable().getSelectedEntries().get(0).getShortName());
            // change competitor
            windowForEdit.switchToWindow();
            dialog = competitorEntry.clickEditButton();
            final String changedName = "" + System.currentTimeMillis();
            dialog.setNameTextBox(changedName);
            final String changedShortName = "" + System.currentTimeMillis();
            dialog.setShortNameTextBox(changedShortName);
            dialog.pressOk();
            // assert selection
            windowForSelection.switchToWindow();
            competitorPanelForSelection.pushRefreshButton();
            WebDriverWait waitTimer = new WebDriverWait(competitorPanelForSelection.driver, 10);
            ExpectedCondition<Boolean> condition = new ExpectedCondition<Boolean>() {
                @Override
                public Boolean apply(WebDriver arg0) {
                    return competitorPanelForSelection.getCompetitorTable().getSelectedEntries().size() == 1;
                }
            };
            waitTimer.until(condition);
            assertEquals(1, competitorPanelForSelection.getCompetitorTable().getSelectedEntries().size());
            for (final CompetitorEntry it : competitorPanelForSelection.getCompetitorTable().getEntries()) {
                String itName = it.getName();
                if (itName.equals(changedName)) {
                    found = true;
                    competitorEntryToSelect = it;
                    break;
                }
            }
            assertTrue(found);
            assertEquals(changedName, competitorPanelForSelection.getCompetitorTable().getSelectedEntries().get(0).getName());
            assertEquals(changedShortName, competitorPanelForSelection.getCompetitorTable().getSelectedEntries().get(0).getShortName());
        });
    }

    private void setUpTestRefreshOfDependingUIElements() {
        this.regatta = new RegattaDescriptor(REGATTA, BOAT_CLASS);
        this.trackableRaces = new ArrayList<>();
        this.trackedRaces = new ArrayList<>();
        this.leaderboardRaces = new ArrayList<>();
        for (int i = 1; i < 8; i++) {
            String raceName = String.format(RACE, i);
            TrackableRaceDescriptor trackableRace = new TrackableRaceDescriptor(EVENT, raceName, BOAT_CLASS);
            TrackedRaceDescriptor trackedRace = new TrackedRaceDescriptor(this.regatta.toString(), BOAT_CLASS,
                    raceName);
            RaceDescriptor leaderboardRace = new RaceDescriptor(String.format("D%s", i), "Default", false, false, 0);

            this.trackableRaces.add(trackableRace);
            this.trackedRaces.add(trackedRace);
            this.leaderboardRaces.add(leaderboardRace);
        }
        configureLeaderboard();
    }

    private void configureLeaderboard() {
        // Open the admin console for some configuration steps
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        
        // Create a regatta with 1 series and 5 races as well as a leaderborad
        RegattaStructureManagementPanelPO regattaStructure = adminConsole.goToRegattaStructure();
        regattaStructure.createRegatta(this.regatta, /* withDefaultLeaderboard */ false);
        
        RegattaDetailsCompositePO regattaDetails = regattaStructure.getRegattaDetails(this.regatta);
        SeriesEditDialogPO seriesDialog = regattaDetails.editSeries(RegattaStructureManagementPanelPO.DEFAULT_SERIES_NAME);
        seriesDialog.addRaces(1, 5);
        seriesDialog.pressOk();
        
        regattaDetails.deleteSeries("Default");
        
        LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
        leaderboardConfiguration.createRegattaLeaderboard(this.regatta);
    }

    @SeleniumTestCase
    public void testRefreshOfDependingUIElements() {
        this.environment.getWindowManager().withExtraWindow(getWebDriver(), (windowForSelection, windowForEdit) -> {
            setUpTestRefreshOfDependingUIElements();
            AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
            SmartphoneTrackingEventManagementPanelPO smartphoneTrackingPanel = adminConsole.goToSmartphoneTrackingPanel();
            CellTablePO<DataEntryPO> leaderboards = smartphoneTrackingPanel.getLeaderboardTable();
            // select leaderboard
            DataEntryPO entryToSelect = null;
            for(DataEntryPO entry : leaderboards.getEntries()) {
                if(entry.getColumnContent("Name").equals(LEADERBOARD)) {
                    entryToSelect = entry;
                    break;
                }
            }
            assertNotNull(entryToSelect);
            leaderboards.selectEntry(entryToSelect);
            
            RaceColumnTableWrapperPO raceColumnTableWrapper = smartphoneTrackingPanel.getRaceColumnTableWrapper();
            CellTablePO<DataEntryPO> raceColumnTable = raceColumnTableWrapper.getRaceColumnTable();
            final int anzRaceColumns = raceColumnTable.getEntries().size();
            assertEquals(5, anzRaceColumns);
            
            // Open a second window & setup second window
            final WebDriver windowForEditDriver = windowForEdit.switchToWindow();
            setUpAuthenticatedSession(windowForEditDriver);
            AdminConsolePage adminConsoleForEdit = AdminConsolePage.goToPage(windowForEditDriver, getContextRoot());
            RegattaStructureManagementPanelPO regattaStructure = adminConsoleForEdit.goToRegattaStructure();
            
            RegattaDetailsCompositePO regattaDetails = regattaStructure.getRegattaDetails(this.regatta);
            SeriesEditDialogPO seriesDialog = regattaDetails.editSeries(RegattaStructureManagementPanelPO.DEFAULT_SERIES_NAME);
            seriesDialog.addRaces(6, 7);
            seriesDialog.pressOk();
            
            windowForSelection.switchToWindow();
            adminConsole.goToTracTracEvents();
            smartphoneTrackingPanel = adminConsole.goToSmartphoneTrackingPanel();
            leaderboards = smartphoneTrackingPanel.getLeaderboardTable();
            smartphoneTrackingPanel.refreshLeaderboardTable();
            // select leaderboard
            entryToSelect = null;
            for(DataEntryPO entry : leaderboards.getEntries()) {
                if(entry.getColumnContent("Name").equals(LEADERBOARD)) {
                    entryToSelect = entry;
                    break;
                }
            }
            assertNotNull(entryToSelect);
            leaderboards.selectEntry(entryToSelect);
            
            raceColumnTableWrapper = smartphoneTrackingPanel.getRaceColumnTableWrapper();
            raceColumnTable = raceColumnTableWrapper.getRaceColumnTable();
            final int newAnzRaceColumns = raceColumnTable.getEntries().size();
            assertEquals(7, newAnzRaceColumns);
        });
    }
    
    @SeleniumTestCase
    public void testMaintenanceOfSelectionAfterFilteringTrackedracesOnLeaderboardConfigPanel() {
        setUpTestRefreshOfDependingUIElements();
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
        leaderboardConfiguration.getLeaderboardDetails(LEADERBOARD);
        
        // Start the tracking for the races and wait until they are ready to use
        TracTracEventManagementPanelPO tracTracEvents = adminConsole.goToTracTracEvents();
        tracTracEvents.addConnectionAndListTrackableRaces(IDM_5O5_2013_JSON_URL);
        tracTracEvents.setReggataForTracking(this.regatta);
        tracTracEvents.setTrackSettings(false, false, false);
        tracTracEvents.startTrackingForRaces(this.trackableRaces);
        
        leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
        LeaderboardDetailsPanelPO leaderboardDetails = leaderboardConfiguration.getLeaderboardDetails(this.regatta.toString());
        
        leaderboardDetails.linkRace(this.leaderboardRaces.get(0), this.trackedRaces.get(0));
        assertEquals("Yes", leaderboardDetails.getRacesTable().getEntries().get(0).getColumnContent(3));
        
        leaderboardDetails.filter("sgfoganafen");
        assertEquals("Yes", leaderboardDetails.getRacesTable().getEntries().get(0).getColumnContent(3));
    }
}
