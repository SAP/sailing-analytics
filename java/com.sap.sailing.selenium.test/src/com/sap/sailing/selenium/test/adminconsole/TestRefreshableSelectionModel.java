package com.sap.sailing.selenium.test.adminconsole;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.selenium.core.WebDriverWindow;
import com.sap.sailing.selenium.core.WindowManager;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.connectors.SmartphoneTrackingEventManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO.RaceDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaDetailsCompositePO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaStructureManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.SeriesEditDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.tracking.RaceColumnTableWrapperPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesCompetitorEditDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesCompetitorsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesCompetitorTablePO.CompetitorEntry;
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
    private static final String IDM_5O5_2013_JSON_URL = "http://traclive.dk/events/event_20130917_IDMO/jsonservice.php"; //$NON-NLS-1$

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
        clearState(getContextRoot());
        super.setUp();
    }

    private TrackedRacesCompetitorsPanelPO goToCompetitorsPanel() {
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        final TrackedRacesCompetitorsPanelPO competitorsPanel = adminConsole.goToTrackedRacesCompetitors();
        return competitorsPanel;
    }

    @Test
    public void testMaintenanceOfSelectionAfterDataChanges() {
        final TrackedRacesCompetitorsPanelPO competitorsPanel = goToCompetitorsPanel();

        for (int i = 0; i < 2; i++) {
            TrackedRacesCompetitorEditDialogPO dialog = competitorsPanel.pushAddButton();
            dialog.setNameTextBox("" + System.currentTimeMillis());
            dialog.setSailIdTextBox("" + System.currentTimeMillis());
            dialog.setBoatClassNameSuggestBox("RS-X");
            dialog.pressOk();
        }

        TrackedRacesCompetitorEditDialogPO dialog = competitorsPanel.pushAddButton();
        final String name = "" + System.currentTimeMillis();
        dialog.setNameTextBox(name);
        final String sailId = "" + System.currentTimeMillis();
        dialog.setSailIdTextBox(sailId);
        final String boatClassName = "Laser Int.";
        dialog.setBoatClassNameSuggestBox(boatClassName);
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

        // Open a second window & setup second window
        WindowManager manager = this.environment.getWindowManager();
        WebDriverWindow windowForEdit = manager.getCurrentWindow();
        WebDriverWindow windowForSelection = manager.openNewWindow();
        windowForSelection.switchToWindow();
        TrackedRacesCompetitorsPanelPO competitorPanelForSelection = goToCompetitorsPanel();
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

        assertTrue(competitorPanelForSelection.getCompetitorTable().getSelectedEntries().size() == 1);
        assertTrue(competitorPanelForSelection.getCompetitorTable().getSelectedEntries().get(0).getName()
                .equals(competitorEntryToSelect.getName()));
        assertTrue(competitorPanelForSelection.getCompetitorTable().getSelectedEntries().get(0).getSailId()
                .equals(competitorEntryToSelect.getSailId()));
        assertTrue(competitorPanelForSelection.getCompetitorTable().getSelectedEntries().get(0).getBoatClassName()
                .equals(competitorEntryToSelect.getBoatClassName()));
        // change competitor
        windowForEdit.switchToWindow();
        dialog = competitorEntry.clickEditButton();
        final String changedName = "" + System.currentTimeMillis();
        dialog.setNameTextBox(changedName);
        final String changedSailId = "" + System.currentTimeMillis();
        dialog.setSailIdTextBox(changedSailId);
        dialog.pressOk();

        // assert selection
        windowForSelection.switchToWindow();
        competitorPanelForSelection.pushRefreshButton();
        assertTrue(competitorPanelForSelection.getCompetitorTable().getSelectedEntries().size() == 0);
        for (final CompetitorEntry it : competitorPanelForSelection.getCompetitorTable().getEntries()) {
            String itName = it.getName();
            if (itName.equals(changedName)) {
                found = true;
                competitorEntryToSelect = it;
                break;
            }
        }
        assertTrue(found);
        assertTrue(competitorPanelForSelection.getCompetitorTable().getSelectedEntries().get(0).getName()
                .equals(competitorEntryToSelect.getName()));
        assertTrue(competitorPanelForSelection.getCompetitorTable().getSelectedEntries().get(0).getSailId()
                .equals(competitorEntryToSelect.getSailId()));
        assertTrue(competitorPanelForSelection.getCompetitorTable().getSelectedEntries().get(0).getBoatClassName()
                .equals(competitorEntryToSelect.getBoatClassName()));
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
            RaceDescriptor leaderboardRace = new RaceDescriptor(String.format("R%s", i), "Default", false, false, 0);

            this.trackableRaces.add(trackableRace);
            this.trackedRaces.add(trackedRace);
            this.leaderboardRaces.add(leaderboardRace);
        }
        clearState(getContextRoot());
        super.setUp();
        configureLeaderboard();
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
    }
    @Test
    public void testRefreshOfDependingUIElements() {
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
        assertTrue(5 == anzRaceColumns);
        
        // Open a second window & setup second window
        WindowManager manager = this.environment.getWindowManager();
        WebDriverWindow windowForSelection = manager.getCurrentWindow();
        WebDriverWindow windowForEdit = manager.openNewWindow();
        windowForEdit.switchToWindow();
        
        AdminConsolePage adminConsoleForEdit = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        RegattaStructureManagementPanelPO regattaStructure = adminConsoleForEdit.goToRegattaStructure();
        
        RegattaDetailsCompositePO regattaDetails = regattaStructure.getRegattaDetails(this.regatta);
        SeriesEditDialogPO seriesDialog = regattaDetails.editSeries(RegattaStructureManagementPanelPO.DEFAULT_SERIES_NAME);
        seriesDialog.addRaces(6, 7);
        seriesDialog.pressOk();
        
        windowForSelection.switchToWindow();
        adminConsole.goToTracTracEvents();
        smartphoneTrackingPanel = adminConsole.goToSmartphoneTrackingPanel();
        leaderboards = smartphoneTrackingPanel.getLeaderboardTable();
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
        assertTrue(7 == newAnzRaceColumns);
    }
}
