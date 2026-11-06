package com.sap.sailing.selenium.test.adminconsole;

import static com.sap.sailing.selenium.api.core.ApiContext.SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.regatta.Regatta;
import com.sap.sailing.selenium.api.regatta.RegattaApi;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.connectors.SmartphoneTrackingEventManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaDetailsCompositePO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaEditDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaStructureManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.SeriesEditDialogPO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

/**
 * Verifies that applying the automatic-tracking-times suggestion offered when denoting a leaderboard for smartphone
 * tracking also clears the mutually-exclusive start-time-inference flag on the regatta.
 */
public class TestAutomaticTrackingTimesSuggestion extends AbstractSeleniumTest {
    private static final String REGATTA = "IDM 2013"; //$NON-NLS-1$
    private static final String LEADERBOARD = "IDM 2013 (5O5)"; //$NON-NLS-1$
    private static final String BOAT_CLASS = "5O5"; //$NON-NLS-1$

    private final RegattaApi regattaApi = new RegattaApi();
    private RegattaDescriptor regatta;

    @Override
    @BeforeEach
    public void setUp() {
        this.regatta = new RegattaDescriptor(REGATTA, BOAT_CLASS);
        clearState(getContextRoot());
        super.setUp();
        configureRegattaAndLeaderboard();
    }

    @SeleniumTestCase
    public void testApplyingAutomaticTrackingTimesClearsStartTimeInference() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        // Enable start-time inference on the freshly created regatta through the regatta edit dialog.
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        final RegattaStructureManagementPanelPO regattaStructure = adminConsole.goToRegattaStructure();
        final RegattaEditDialogPO regattaEditDialog = regattaStructure.getRegattaList().editRegatta(this.regatta);
        regattaEditDialog.setUseStartTimeInference(true);
        regattaEditDialog.pressOk();
        final Regatta regattaBeforeDenotation = regattaApi.getRegatta(ctx, this.regatta.toString());
        assertTrue(regattaBeforeDenotation.isUseStartTimeInference(),
                "Expected start-time inference to be enabled after setting it in the regatta edit dialog");
        // Switch to the Smartphone Tracking panel and denote all races of the leaderboard; the suggestion popup must
        // appear because the regatta does not yet control tracking from start and finish times.
        final SmartphoneTrackingEventManagementPanelPO smartphoneTrackingPanel = adminConsole.goToSmartphoneTrackingPanel();
        smartphoneTrackingPanel.refreshLeaderboardTable();
        final DataEntryPO leaderboardEntry = smartphoneTrackingPanel.waitForLeaderboardEntry(LEADERBOARD);
        smartphoneTrackingPanel.clickDenoteForRaceLogTracking(leaderboardEntry);
        assertTrue(smartphoneTrackingPanel.isAutomaticTrackingTimesDialogShown(),
                "Expected the automatic-tracking-times suggestion dialog to appear");
        // Confirm the dialog, which enables automatic tracking times and must therefore clear start-time inference.
        smartphoneTrackingPanel.applyAutomaticTrackingTimesAndFinishDenotation();
        final Regatta regattaAfterDenotation = regattaApi.getRegatta(ctx, this.regatta.toString());
        assertFalse(regattaAfterDenotation.isUseStartTimeInference(),
                "Expected start-time inference to be cleared after applying automatic tracking times");
        assertTrue(regattaAfterDenotation.isControlTrackingFromStartAndFinishTimes(),
                "Expected automatic tracking times to be enabled after confirming the suggestion");
    }

    private void configureRegattaAndLeaderboard() {
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        final RegattaStructureManagementPanelPO regattaStructure = adminConsole.goToRegattaStructure();
        regattaStructure.createRegatta(this.regatta, /* withDefaultLeaderboard */ false);
        final RegattaDetailsCompositePO regattaDetails = regattaStructure.getRegattaDetails(this.regatta);
        final SeriesEditDialogPO seriesDialog = regattaDetails.editSeries(RegattaStructureManagementPanelPO.DEFAULT_SERIES_NAME);
        seriesDialog.addRaces(1, 5);
        seriesDialog.pressOk();
        final LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
        leaderboardConfiguration.createRegattaLeaderboard(this.regatta);
    }
}
