package com.sap.sailing.selenium.test.autoplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
    private static final String BMW_CUP_JSON_URL = "http://kml.skitrac.traclive.dk/events/event_20120803_BMWCup/jsonservice.php"; //$NON-NLS-1$
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
    @Before
    public void setUp() {
        super.setUp();
        clearState(getContextRoot());
    }

    private void initTrackingForBmwCupRace(AdminConsolePage adminConsole) {

        TrackableRaceDescriptor trackableRace = new TrackableRaceDescriptor(BMW_CUP_EVENT, String.format(BMW_RACE, 1),
                BMW_CUP_BOAT_CLASS);
        TrackedRaceDescriptor trackedRace = new TrackedRaceDescriptor(BMW_CUP_REGATTA, BMW_CUP_BOAT_CLASS,
                String.format(BMW_RACE, 1));

        TracTracEventManagementPanelPO tracTracEvents = adminConsole.goToTracTracEvents();
        tracTracEvents.listTrackableRaces(BMW_CUP_JSON_URL);
        RegattaDescriptor bmwCupDescriptor = new RegattaDescriptor(BMW_CUP_EVENT, BMW_CUP_BOAT_CLASS);
        tracTracEvents.setReggataForTracking(bmwCupDescriptor);
        tracTracEvents.setTrackSettings(true, false, false);
        tracTracEvents.startTrackingForRace(trackableRace);
        TrackedRacesListPO trackedRacesList = tracTracEvents.getTrackedRacesList();
        trackedRacesList.waitForTrackedRace(trackedRace, Status.FINISHED, 600); // with the TracAPI, REPLAY races
        // status FINISHED when done loading

        LeaderboardConfigurationPanelPO leaderboard = adminConsole.goToLeaderboardConfiguration();
        LeaderboardDetailsPanelPO details = leaderboard.getLeaderboardDetails(BMW_CUP_REGATTA);
        Assert.assertTrue(details != null);
        details.linkRace(new RaceDescriptor(BMW_CUP_RACE_NAME, "Default", false, false, 0), trackedRace);
    }

    @Test
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
        assertTrue("Url does not contain proper type " + url, url.contains("autoplayType=SIXTYINCH"));
        assertTrue("Url does not contain proper name " + url, url.contains("name=BMW+Cup+(J80)"));
        AutoPlayUpcomingView autoplayPage = page.goToAutoPlaySixtyInchUrl(getWebDriver(), url);
        // give some extra time to load the leaderboard and finish the animation
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        String nextUpText = autoplayPage.getText();
        assertEquals("There are no further races starting soon", nextUpText);
    }

    @Test
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
        assertTrue("Url does not contain proper title " + url, url.contains("lbwh.saph.title=Leaderboard%3A+BMW+Cup+(J80)"));
        assertTrue("Url does not contain proper name " + url, url.contains("name=BMW+Cup+(J80)"));
        AutoPlayLeaderboardView autoplayPage = page.goToAutoPlayClassicUrl(getWebDriver(), url);
        // give some extra time to load the leaderboard and finish the animation
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        LeaderboardTablePO leaderBoard = autoplayPage.getLeaderBoard();
        List<String> races = leaderBoard.getRaceNames();
        Assert.assertTrue(races.contains("R1"));
        Assert.assertTrue(races.contains("R2"));
        Assert.assertTrue(races.contains("R3"));
    }

}
