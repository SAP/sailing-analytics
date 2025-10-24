package com.sap.sailing.selenium.test.adminconsole;

import static com.sap.sailing.selenium.pages.common.DateHelper.getFutureDate;
import static com.sap.sailing.selenium.pages.common.DateHelper.getPastDate;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO.EventEntryPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardGroupConfigurationPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestEventCreationWithPredefinedLeaderboards extends AbstractSeleniumTest {
    private AdminConsolePage adminConsolePage;
    
    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
        adminConsolePage = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
    }
    
    /**
     * See bug 3693: this test asserts that when specifying leaderboard groups already in the event creation
     * dialog, those leaderboard groups can be expected to be linked to the event when confirming the
     * creation operation.
     */
    @SeleniumTestCase
    public void createTestEventLinkingToExistingLeaderboardGroups() {
        LeaderboardGroupConfigurationPanelPO leaderboardGroupsConfigPanel = adminConsolePage.goToLeaderboardGroupConfiguration();
        leaderboardGroupsConfigPanel.createLeaderboardGroup("A", "A");
        leaderboardGroupsConfigPanel.createLeaderboardGroup("B", "B");
        leaderboardGroupsConfigPanel.createLeaderboardGroup("C", "C");
        EventConfigurationPanelPO eventConfigPanel = adminConsolePage.goToEvents();
        eventConfigPanel.createEventWithExistingLeaderboardGroups("My Event", "My Description", "My Venue", getPastDate(3), getFutureDate(3), true, "A", "C");
        EventConfigurationPanelPO eventsPanel = adminConsolePage.goToEvents();
        EventEntryPO eventEntry = eventsPanel.getEventEntry("My Event");
        String leaderboardGroupsListedInEventsTable = eventEntry.getColumnContent("Leaderboard groups");
        assertTrue(leaderboardGroupsListedInEventsTable.contains("A"));
        assertTrue(leaderboardGroupsListedInEventsTable.contains("C"));
        assertFalse(leaderboardGroupsListedInEventsTable.contains("B"));
    }
}
