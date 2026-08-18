package com.sap.sailing.selenium.test.adminconsole;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.connectors.SmartphoneTrackingEventManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaStructureManagementPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

/**
 * Selenium test for bug6113 + bug6251: verifies the start/stop tracking cycle for Race Log /
 * Smartphone Tracking. Runs two full denote → start → stop → undenote cycles to ensure that
 * button states reset correctly after the first cycle (bug6251). Tests both global and local start/stop tracking buttons.
 */
public class TestStartStopTrackingCycleForRaceLog extends AbstractSeleniumTest {
    private static final String REGATTA_NAME = "TestRegatta6113";
    private static final String BOAT_CLASS = "Laser";
    private static final String R1 = "R1";
    private static final String R2 = "R2";
    private static final String R3 = "R3";

    private RegattaDescriptor regatta;

    @Override
    @BeforeEach
    public void setUp() {
        this.regatta = new RegattaDescriptor(REGATTA_NAME, BOAT_CLASS);
        clearState(getContextRoot());
        super.setUp();
        configureRegattaAndLeaderboard();
    }

    @SeleniumTestCase
    public void testStartStopTrackingCycle() {
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        final SmartphoneTrackingEventManagementPanelPO smartphonePanel = adminConsole.goToSmartphoneTrackingPanel();
        smartphonePanel.getLeaderboardTable().selectEntry(smartphonePanel.waitForLeaderboardEntry(this.regatta.toString()));
        performTrackingCycle(smartphonePanel);
        performTrackingCycle(smartphonePanel);
    }

    private void configureRegattaAndLeaderboard() {
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        final RegattaStructureManagementPanelPO regattaStructure = adminConsole.goToRegattaStructure();
        regattaStructure.createRegatta(this.regatta, /* withDefaultLeaderboard */ true);
    }

    private void performTrackingCycle(final SmartphoneTrackingEventManagementPanelPO smartphonePanel) {
        // denote all races for tracking, re-fetching the leaderboard entry each cycle
        smartphonePanel.denoteLeaderboardForRaceLogTracking(smartphonePanel.waitForLeaderboardEntry(this.regatta.toString()));
        // start R1 via local button, R2+R3 via global toggle button
        smartphonePanel.startTrackingForRace(smartphonePanel.waitForRaceRow(R1));
        smartphonePanel.selectRaceRowsByName(R2, R3);
        smartphonePanel.clickStartTrackingButton();
        // stop R3 via local button, R1+R2 via global toggle button
        smartphonePanel.stopTrackingForRace(smartphonePanel.waitForRaceRow(R3));
        smartphonePanel.selectRaceRowsByName(R1, R2);
        smartphonePanel.clickStartTrackingButton();
        // undenote all races to reset state for next cycle (PO dismisses notifications before each click to avoid CI overlay interception)
        smartphonePanel.removeDenotationForRace(smartphonePanel.waitForRaceRow(R1));
        smartphonePanel.removeDenotationForRace(smartphonePanel.waitForRaceRow(R2));
        smartphonePanel.removeDenotationForRace(smartphonePanel.waitForRaceRow(R3));
    }
}
