package com.sap.sailing.selenium.test.adminconsole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardGroupConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardGroupDetailsPanelPO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestLeaderboardFilterInLeaderboardGroupConfig extends AbstractSeleniumTest {
    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
    }
    
    @SeleniumTestCase
    public void testLeaderboardFilterInLeaderboardGroupConfig() {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        
        LeaderboardGroupConfigurationPanelPO leaderboardGroupConfiguration = adminConsole.goToLeaderboardGroupConfiguration();
        leaderboardGroupConfiguration.createLeaderboardGroup("Test", "Test");
        List<DataEntryPO> selectedEntries = leaderboardGroupConfiguration.getLeaderboardGroupsTable().getSelectedEntries();
        assertEquals(1, selectedEntries.size());
        assertEquals("Test", selectedEntries.get(0).getColumnContent("Name"));
        
        LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
        assertFalse(leaderboardConfiguration.getAvailableLeaderboards().contains("Test"));
        leaderboardConfiguration.createFlexibleLeaderboard("Test");
        assertTrue(leaderboardConfiguration.getAvailableLeaderboards().contains("Test"));

        leaderboardGroupConfiguration = adminConsole.goToLeaderboardGroupConfiguration();
        LeaderboardGroupDetailsPanelPO leaderboardGroupDetails = leaderboardGroupConfiguration.getLeaderboardGroupDetails("Test");
        leaderboardGroupDetails.refreshLeaderboards();
        DataEntryPO leaderboardEntry = leaderboardGroupDetails.findLeaderboardEntry("Test");
        assertNotNull(leaderboardEntry);
        
        leaderboardGroupDetails.filterLeaderboards("Test");
        assertNotNull(leaderboardGroupDetails.findLeaderboardEntry("Test"));
    }
}
