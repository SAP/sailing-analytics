package com.sap.sailing.selenium.test.integration.state;

import static com.sap.sailing.selenium.pages.common.DateHelper.getFutureDate;
import static com.sap.sailing.selenium.pages.common.DateHelper.getPastDate;
import static com.sap.sailing.selenium.pages.common.DateHelper.getPastTime;

import java.util.Date;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.SetStartTimeDialogPO;
import com.sap.sailing.selenium.pages.common.LabelTypePO;
import com.sap.sailing.selenium.pages.common.RaceStatusEventHelper;
import com.sap.sailing.selenium.pages.home.event.RegattaListItemPO;
import com.sap.sailing.selenium.pages.home.event.regatta.RegattaEventPage;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestStateVisualizationRegatta extends AbstractSeleniumTest {

    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
    }
    
    @SeleniumTestCase
    public void testStateFlagOfUpcomingRegatta() {
        TestCase testCase = new TestCase();
        testCase.createTestEventWithRegatta("TestUpcomingRegatta", getFutureDate(3), getFutureDate(7));
        testCase.assertStateFlagOfRegatta("TestUpcomingRegatta", LabelTypePO::isUpcoming);
    }

    @SeleniumTestCase
    public void testStateFlagOfInProgressRegatta() {
        TestCase testCase = new TestCase();
        testCase.createTestEventWithRegatta("TestInProgressRegatta", getPastDate(3), getFutureDate(3));
        testCase.setLeaderboardsRaceStartTime("TestInProgressRegatta", "R1", getPastTime(20));
        RaceStatusEventHelper.get(getContextRoot(),"TestInProgressRegatta", "R1", "Default").finishRace(getPastTime(5));
        testCase.assertStateFlagOfRegatta("TestInProgressRegatta", LabelTypePO::isInProgress);
    }

    @SeleniumTestCase
    public void testStateFlagOfLiveRegatta() {
        TestCase testCase = new TestCase();
        testCase.createTestEventWithRegatta("TestLiveRegatta", getPastDate(3), getFutureDate(3));
        testCase.setLeaderboardsRaceStartTime("TestLiveRegatta", "R1", getPastTime(30));
        testCase.assertStateFlagOfRegatta("TestLiveRegatta", LabelTypePO::isLive);
    }

    @SeleniumTestCase
    public void testStateFlagOfFinishedRegatta() {
        TestCase testCase = new TestCase();
        testCase.createTestEventWithRegatta("TestFinishedRegatta", getPastDate(7), getPastDate(3));
        testCase.assertStateFlagOfRegatta("TestFinishedRegatta", LabelTypePO::isFinished);
    }
    
    private class TestCase {
        private final AdminConsolePage adminConsolePage = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        private String eventUrl; 
        
        private void createTestEventWithRegatta(String regattaName, Date regattaStartDate, Date regattaEndDate) {
            EventConfigurationPanelPO eventConfigPanel = adminConsolePage.goToEvents();
            final String eventName = "RegattaStateTestEvent", venue = "Anywhere", boatClass = "J-22";
            eventConfigPanel.createEventWithDefaultLeaderboardGroupRegattaAndDefaultLeaderboard(eventName, null, venue,
                    getPastDate(3), getFutureDate(3), true, regattaName, boatClass, regattaStartDate, regattaEndDate, false);
            this.eventUrl = eventConfigPanel.getEventEntry(eventName).getEventURL();
        }
        
        private void setLeaderboardsRaceStartTime(String leaderboard, String race, Date startTime) {
            LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsolePage.goToLeaderboardConfiguration();
            LeaderboardDetailsPanelPO leaderboardDetails = leaderboardConfiguration.getLeaderboardDetails(leaderboard);
            SetStartTimeDialogPO startTimeDialog = leaderboardDetails.getRacesTable().getEntry(race).clickSetStartTime();
            startTimeDialog.setStartTimeValue(startTime);
            startTimeDialog.pressSetStartTime();
        }
        
        private void assertStateFlagOfRegatta(String regattaName, Predicate<LabelTypePO> expectedLabelType) {
            RegattaEventPage regattaEventPage = RegattaEventPage.goToRegattaEventUrl(getWebDriver(), eventUrl);
            RegattaListItemPO regattaListItem = regattaEventPage.getRegattaListItem(regattaName);
            Assertions.assertTrue(expectedLabelType.test(regattaListItem.getRegattaHeader().getRegattaStateLabel()));
        }
    }
}
