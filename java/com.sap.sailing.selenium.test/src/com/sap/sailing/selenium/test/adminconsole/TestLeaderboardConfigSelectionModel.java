package com.sap.sailing.selenium.test.adminconsole;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO.LeaderboardEntryPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardRacesTablePO.RaceEntryPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestLeaderboardConfigSelectionModel extends AbstractSeleniumTest {
    private static final String LEADERBOARDNAME = "Test";
    
    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
        leaderboardConfiguration.createFlexibleLeaderboard(LEADERBOARDNAME);
        LeaderboardDetailsPanelPO leaderboardDetails = leaderboardConfiguration.getLeaderboardDetails(LEADERBOARDNAME);
        leaderboardDetails.addRacesToFlexibleLeaderboard(5);
    }
    
    @SeleniumTestCase
    public void testBehaviorOfRaceSelectionWithTabChange() {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
        LeaderboardDetailsPanelPO leaderboardDetails = leaderboardConfiguration.getLeaderboardDetails(LEADERBOARDNAME);

        RaceEntryPO selected = leaderboardDetails.getRacesTable().getEntries().iterator().next();
        Object id = selected.getIdentifier();
        leaderboardDetails.getRacesTable().selectEntry(selected);

        adminConsole.goToLeaderboardGroupConfiguration();
        leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();

        List<LeaderboardEntryPO> selectedLeaderboards = leaderboardConfiguration.getLeaderboardTable()
                .getSelectedEntries();
        assertEquals(1, selectedLeaderboards.size());
        assertEquals(LEADERBOARDNAME, selectedLeaderboards.get(0).getName());

        leaderboardDetails = leaderboardConfiguration.getLeaderboardDetails(LEADERBOARDNAME);
        assertEquals(1, leaderboardDetails.getRacesTable().getSelectedEntries().size());
        assertEquals(id, leaderboardDetails.getRacesTable().getSelectedEntries().get(0).getIdentifier());
    }

    @SeleniumTestCase
    public void testBehaviorOfRaceSelectionWithLeaderboardChange() {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
        leaderboardConfiguration.createFlexibleLeaderboard("TEST2");
        LeaderboardDetailsPanelPO leaderboardDetails = leaderboardConfiguration.getLeaderboardDetails("TEST2");
        leaderboardDetails.addRacesToFlexibleLeaderboard(5);

        leaderboardDetails = leaderboardConfiguration.getLeaderboardDetails(LEADERBOARDNAME);

        RaceEntryPO selected = leaderboardDetails.getRacesTable().getEntries().iterator().next();
        leaderboardDetails.getRacesTable().selectEntry(selected);

        leaderboardDetails = leaderboardConfiguration.getLeaderboardDetails("TEST2");
        assertEquals(0, leaderboardDetails.getRacesTable().getSelectedEntries().size());
    }
}
