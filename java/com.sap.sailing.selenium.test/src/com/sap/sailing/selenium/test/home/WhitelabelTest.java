package com.sap.sailing.selenium.test.home;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardDetailsPanelPO.RaceDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaDetailsCompositePO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaEditDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaStructureManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.regatta.SeriesEditDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO.Status;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO.TrackedRaceDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.TracTracEventManagementPanelPO;
import com.sap.sailing.selenium.pages.home.HomePage;
import com.sap.sailing.selenium.pages.raceboard.RaceBoardPage;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class WhitelabelTest extends AbstractSeleniumTest {

    private static final String JSON_URL = "http://event.tractrac.com/events/event_20150616_KielerWoch/jsonservice.php"; //$NON-NLS-1$
    private static final String EVENT = "Kieler Woche 2015"; //$NON-NLS-1$
    private static final String BOAT_CLASS_49ER = "49er"; //$NON-NLS-1$
    private static final String REGATTA_49ER = "KW 2015 Olympic - 49er"; //$NON-NLS-1$
    private static final String REGATTA_49ER_WITH_SUFFIX = REGATTA_49ER + " ("+BOAT_CLASS_49ER+")"; //$NON-NLS-1$
    private static final String RACE_N_49ER = "R%d (49er)"; //$NON-NLS-1$
    private static final String MEDAL_RACE_49ER = "R12-M Medal (49er)"; //$NON-NLS-1$
    private static final String EVENT_DESC = "Kieler Woche 2015"; //$NON-NLS-1$
    private static final String VENUE = "Kieler F�rde"; //$NON-NLS-1$
    private static final Date EVENT_START_TIME = DatatypeConverter.parseDateTime("2015-06-20T08:00:00-00:00")
            .getTime();
    private static final Date EVENT_END_TIME = DatatypeConverter.parseDateTime("2015-06-28T20:00:00-00:00")
            .getTime();
    private static final String DEFAULT_FLEET = "Default";
    private static final String SERIES_DEFAULT = "Default";
    private static final String SERIES_QUALIFICATION = "Qualification";
    private static final String SERIES_MEDALS = "Medals";
    
    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
        final RegattaDescriptor regattaDescriptor = new RegattaDescriptor(REGATTA_49ER, BOAT_CLASS_49ER);
        {
            final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
            final EventConfigurationPanelPO events = adminConsole.goToEvents();
            events.createEventWithDefaultLeaderboardGroupRegattaAndDefaultLeaderboard(EVENT, EVENT_DESC,
                    VENUE, EVENT_START_TIME, EVENT_END_TIME, true, REGATTA_49ER_WITH_SUFFIX, BOAT_CLASS_49ER,
                    EVENT_START_TIME, EVENT_END_TIME, false);
            final RegattaStructureManagementPanelPO regattaStructurePanel = adminConsole.goToRegattaStructure();
            final RegattaDetailsCompositePO regattaDetails = regattaStructurePanel.getRegattaDetails(regattaDescriptor);
            regattaDetails.deleteSeries(SERIES_DEFAULT);
            RegattaEditDialogPO editRegatta = regattaStructurePanel.getRegattaList().editRegatta(regattaDescriptor);
            editRegatta.addSeries(SERIES_QUALIFICATION);
            editRegatta.addSeries(SERIES_MEDALS);
            editRegatta.pressOk();
            final SeriesEditDialogPO editSeriesQualification = regattaDetails.editSeries(SERIES_QUALIFICATION);
            editSeriesQualification.addRaces(1, 11, "Q");
            editSeriesQualification.pressOk();
            final SeriesEditDialogPO editSeriesMedals = regattaDetails.editSeries(SERIES_MEDALS);
            editSeriesMedals.setMedalSeries(true);
            editSeriesMedals.addSingleRace("M");
            editSeriesMedals.pressOk();
            trackRacesFor49er(regattaDescriptor, adminConsole.goToTracTracEvents());
            final LeaderboardConfigurationPanelPO leaderboard = adminConsole.goToLeaderboardConfiguration();
            leaderboard.refreshLeaderboard();
            final LeaderboardDetailsPanelPO details = leaderboard.getLeaderboardDetails(REGATTA_49ER_WITH_SUFFIX);
            
            for(int i = 1; i<=11; i++) {
                details.linkRace(new RaceDescriptor("Q" + i, DEFAULT_FLEET, false, false, 0), new TrackedRaceDescriptor(REGATTA_49ER_WITH_SUFFIX, BOAT_CLASS_49ER, String.format(RACE_N_49ER, i)));
            }
            details.linkRace(new RaceDescriptor("M", DEFAULT_FLEET, true, false, 0), new TrackedRaceDescriptor(REGATTA_49ER_WITH_SUFFIX, BOAT_CLASS_49ER, MEDAL_RACE_49ER));
        }
        setWhitelabel(true, getContextRoot());
    }

    @SeleniumTestCase
    public void testHomepageWhitelabel() throws UnsupportedEncodingException {
        HomePage homePage = HomePage.goToPage(getWebDriver(), getContextRoot());
        assertThat(homePage.getPageTitle(), not(containsString("SAP")));
        validateIsDisplayed(homePage.getFavicon(), false);
        validateIsDisplayed(homePage.getSolutionsPageLink(), false);
        validateIsDisplayed(homePage.getSapSailingHeaderImage(), false);
        assertThat(homePage.getLogoAnchor().getAttribute("target"), equalTo(""));
        validateIsDisplayed(homePage.getSocialmediaFooter(), false);
        validateIsDisplayed(homePage.getCopyrightDiv(), false);
        validateIsDisplayed(homePage.getImprintLink(), false);
        validateIsDisplayed(homePage.getPrivacyLink(), false);
        validateIsDisplayed(homePage.getSupportLink(), false);
        validateIsDisplayed(homePage.getNewsLink(), false);
        validateIsDisplayed(homePage.getLanguageSelectionLabel(), true);
        assertThat(homePage.getLanguageSelectionLabel().getText(), not(containsString("SAP")));
        RaceBoardPage raceboardPage = RaceBoardPage.goToRaceboardUrl(getWebDriver(), getContextRoot(), REGATTA_49ER_WITH_SUFFIX,
                REGATTA_49ER_WITH_SUFFIX, MEDAL_RACE_49ER, true);
        assertThat(raceboardPage.isRaceBoardLogoExisting(),equalTo(false));
        validateIsDisplayed(raceboardPage.getDataByContainer(), false);

    }
    
    private void trackRacesFor49er(final RegattaDescriptor regattaDescriptor, final TracTracEventManagementPanelPO tracTracEvents) {
        tracTracEvents.addConnectionAndListTrackableRaces(JSON_URL);
        tracTracEvents.setReggataForTracking(regattaDescriptor);
        tracTracEvents.setTrackSettings(true, false, false);
        tracTracEvents.setFilterForTrackableRaces("(49er)");
        // races are filtered so that all shown entries belong to the correct regatta
        tracTracEvents.startTrackingForAllRaces();
        final TrackedRacesListPO trackedRacesList = tracTracEvents.getTrackedRacesList();
        final List<TrackedRaceDescriptor> racesToWaitLoadingFor = new ArrayList<>();
        for(int i = 1; i<=11; i++) {
            racesToWaitLoadingFor.add(new TrackedRaceDescriptor(REGATTA_49ER_WITH_SUFFIX, BOAT_CLASS_49ER, String.format(RACE_N_49ER, i)));
        }
        racesToWaitLoadingFor.add(new TrackedRaceDescriptor(REGATTA_49ER_WITH_SUFFIX, BOAT_CLASS_49ER, MEDAL_RACE_49ER));
        trackedRacesList.waitForTrackedRaces(racesToWaitLoadingFor, Status.FINISHED, 600);
    }
    
    private void validateIsDisplayed(WebElement element, boolean displayed) {
        assertThat(element.isDisplayed(),equalTo(displayed));
    }
}
