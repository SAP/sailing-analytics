package com.sap.sailing.selenium.test.adminconsole;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.FlexibleLeaderboardCreateDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

/**
 * <p>Tests for creation of leader boards.</p>
 * 
 * @author
 *   D049941
 */
public class TestLeaderboardCreation extends AbstractSeleniumTest {
    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
    }
    
    @SeleniumTestCase
    public void testCreateFlexibleLeaderboardWithDuplicateName() {
        FlexibleLeaderboardCreateDialogPO dialog;
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
        dialog = leaderboardConfiguration.startCreatingFlexibleLeaderboard();
        dialog.setName("Humba Humba");
        assertTrue(dialog.isOkButtonEnabled());
        dialog.pressOk();
        dialog = leaderboardConfiguration.startCreatingFlexibleLeaderboard();
        dialog.setName("Humba Humba");
        assertFalse(dialog.isOkButtonEnabled());
        String errorMessage = dialog.getStatusMessage();
        assertTrue(errorMessage.length() > 0);
    }
}
