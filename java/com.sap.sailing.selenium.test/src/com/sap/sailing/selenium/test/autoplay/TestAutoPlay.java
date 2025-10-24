package com.sap.sailing.selenium.test.autoplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.xml.bind.DatatypeConverter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO.RaceDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO.Status;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO.TrackedRaceDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.TracTracEventManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.TracTracEventManagementPanelPO.TrackableRaceDescriptor;
import com.sap.sailing.selenium.pages.autoplay.AutoPlayConfiguration;
import com.sap.sailing.selenium.pages.autoplay.AutoPlayLeaderboardView;
import com.sap.sailing.selenium.pages.autoplay.AutoPlayPage;
import com.sap.sailing.selenium.pages.autoplay.AutoPlayUpcomingView;
import com.sap.sailing.selenium.pages.leaderboard.LeaderboardTablePO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestAutoPlay extends AbstractSeleniumTest {
    private static final String BMW_CUP_JSON_URL = "http://event2.tractrac.com/events/event_20120803_BMWCup/jsonservice.php"; //$NON-NLS-1$
    private static final String BMW_CUP_EVENT = "BMW Cup";
    private static final String BMW_CUP_BOAT_CLASS = "J80";
    private static final String BMW_CUP_REGATTA = "BMW Cup (J80)"; //$NON-NLS-1$
    private static final String BMW_RACE = "BMW Cup Race %d";
    private static final String BMW_CUP_EVENTS_DESC = "BMW Cup Description";
    private static final String BMW_VENUE = "Somewhere";
    private static final Date BMW_START_EVENT_TIME = DatatypeConverter.parseDateTime("2012-04-08T10:09:00-05:00")
            .getTime();
    private static final Date BMW_STOP_EVENT_TIME = DatatypeConverter.parseDateTime("2017-04-08T10:50:00-05:00")
            .getTime();
    private static final String BMW_CUP_RACE_NAME = "R1";

    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
    }

    private void initTrackingForBmwCupRace(AdminConsolePage adminConsole) {
        TrackableRaceDescriptor trackableRace = new TrackableRaceDescriptor(BMW_CUP_EVENT, String.format(BMW_RACE, 1),
                BMW_CUP_BOAT_CLASS);
        TrackedRaceDescriptor trackedRace = new TrackedRaceDescriptor(BMW_CUP_REGATTA, BMW_CUP_BOAT_CLASS,
                String.format(BMW_RACE, 1));

        TracTracEventManagementPanelPO tracTracEvents = adminConsole.goToTracTracEvents();
        tracTracEvents.addConnectionAndListTrackableRaces(BMW_CUP_JSON_URL);
        RegattaDescriptor bmwCupDescriptor = new RegattaDescriptor(BMW_CUP_EVENT, BMW_CUP_BOAT_CLASS);
        tracTracEvents.setReggataForTracking(bmwCupDescriptor);
        tracTracEvents.setTrackSettings(true, false, false);
        tracTracEvents.startTrackingForRace(trackableRace);
        TrackedRacesListPO trackedRacesList = tracTracEvents.getTrackedRacesList();
        trackedRacesList.waitForTrackedRace(trackedRace, Status.FINISHED, 600); // with the TracAPI, REPLAY races
        // status FINISHED when done loading

        LeaderboardConfigurationPanelPO leaderboard = adminConsole.goToLeaderboardConfiguration();
        LeaderboardDetailsPanelPO details = leaderboard.getLeaderboardDetails(BMW_CUP_REGATTA);
        Assertions.assertTrue(details != null);
        details.linkRace(new RaceDescriptor(BMW_CUP_RACE_NAME, "Default", false, false, 0), trackedRace);
    }

    @SeleniumTestCase
    public void testSixtyInchAutoPlayStartup() {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        EventConfigurationPanelPO events = adminConsole.goToEvents();
        events.createEventWithDefaultLeaderboardGroupRegattaAndDefaultLeaderboard(BMW_CUP_EVENT, BMW_CUP_EVENTS_DESC,
                BMW_VENUE, BMW_START_EVENT_TIME, BMW_STOP_EVENT_TIME, true, BMW_CUP_REGATTA, BMW_CUP_BOAT_CLASS,
                BMW_START_EVENT_TIME, BMW_STOP_EVENT_TIME, false);

        initTrackingForBmwCupRace(adminConsole);

        AutoPlayPage page = AutoPlayPage.goToPage(getWebDriver(), getContextRoot());
        AutoPlayConfiguration autoPlayConfiguration = page.getAutoPlayConfiguration();
        assertNotNull(autoPlayConfiguration);
        autoPlayConfiguration.select("Sixty Inch Autoplay", BMW_CUP_EVENT, BMW_CUP_REGATTA);
        String url = autoPlayConfiguration.getConfiguredUrl();
        // eventid and server change, better only check arguments
        assertTrue(url.contains("autoplayType=SIXTYINCH"), "Url does not contain proper type " + url);
        assertTrue(url.contains("name=BMW+Cup+(J80)"), "Url does not contain proper name " + url);
        AutoPlayUpcomingView autoplayPage = page.goToAutoPlaySixtyInchUrl(getWebDriver(), url);
        String nextUpText = autoplayPage.getText();
        assertEquals("There are no further races starting soon", nextUpText);
    }

    @SeleniumTestCase
    public void testClassicAutoPlayStartup() {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        EventConfigurationPanelPO events = adminConsole.goToEvents();
        events.createEventWithDefaultLeaderboardGroupRegattaAndDefaultLeaderboard(BMW_CUP_EVENT, BMW_CUP_EVENTS_DESC,
                BMW_VENUE, BMW_START_EVENT_TIME, BMW_STOP_EVENT_TIME, true, BMW_CUP_REGATTA, BMW_CUP_BOAT_CLASS,
                BMW_START_EVENT_TIME, BMW_STOP_EVENT_TIME, false);

        initTrackingForBmwCupRace(adminConsole);

        AutoPlayPage page = AutoPlayPage.goToPage(getWebDriver(), getContextRoot());
        AutoPlayConfiguration autoPlayConfiguration = page.getAutoPlayConfiguration();
        assertNotNull(autoPlayConfiguration);
        autoPlayConfiguration.select("Classic Autoplay", BMW_CUP_EVENT, BMW_CUP_REGATTA);
        String url = autoPlayConfiguration.getConfiguredUrl();
        // eventid and server change, better only check arguments
        assertTrue(url.contains("lbwh.saph.title=Leaderboard%3A+BMW+Cup+(J80)"), "Url does not contain proper title " + url);
        assertTrue(url.contains("name=BMW+Cup+(J80)"), "Url does not contain proper name " + url);
        AutoPlayLeaderboardView autoplayPage = page.goToAutoPlayClassicUrl(getWebDriver(), url);
        LeaderboardTablePO leaderBoard = autoplayPage.getLeaderBoardWithData();
        final List<String> races = new WebDriverWait(getWebDriver(), 20).until(new Function<WebDriver, List<String>>() {
            @Override
            public List<String> apply(WebDriver arg0) {
                try {
                    final List<String> races = leaderBoard.getRaceNames();
                    final List<String> result;
                    if (races != null && !races.isEmpty()) {
                        result = races;
                    } else {
                        // The table may seem to be empty while the leaderboard updates itself
                        result = null;
                    }
                    return result;
                } catch (Exception e) {
                    // While the leaderboard updates its contents, errors may occur while reading data via selenium
                    // In this case we need to try this in the next loop again
                    return null;
                }
            }
        });
        Assertions.assertTrue(races.contains("R1"));
        Assertions.assertTrue(races.contains("R2"));
        Assertions.assertTrue(races.contains("R3"));
    }

}
