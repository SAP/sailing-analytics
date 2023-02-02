package com.sap.sailing.selenium.test.adminconsole;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaStructureManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO.Status;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO.TrackedRaceDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.TracTracEventManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.TracTracEventManagementPanelPO.TrackableRaceDescriptor;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

/**
 * <p>Test for starting and stopping the tracking of TracTrac races.</p>
 * 
 * @author
 *   D049941
 */
public class TestStartAndStopTrackingForTracTracEvents extends AbstractSeleniumTest {
    private static final String BMW_CUP_JSON_URL = "http://event2.tractrac.com/events/event_20120803_BMWCup/jsonservice.php"; //$NON-NLS-1$
    
    private static final String BMW_CUP_EVENT = "BMW Cup";
    private static final String IDM_2013_EVENT = "IDM 5O5 2013";
    
    private static final String BMW_CUP_BOAT_CLASS = "J80";
    private static final String IDM_2013_BOAT_CLASS = "5O5";
    
    private static final String DEFAULT_REGATTA = "Default regatta";
    private static final String BMW_CUP_REGATTA = "BMW Cup (J80)";  //$NON-NLS-1$
    
    private static final String RACE = "BMW Cup Race %d";
    
    private TrackableRaceDescriptor trackableRace;
    private TrackedRaceDescriptor trackedRace;
    
    // TODO see below: Complete the test case
//    private static final String NO_REGATTA = "No regatta";
    
//    private static final String BMW_CUP_RACE_8 = "BMW Cup Race 8";
    
    @Override
    @Before
    public void setUp() {
        this.trackableRace = new TrackableRaceDescriptor(BMW_CUP_EVENT, String.format(RACE, 1), BMW_CUP_BOAT_CLASS);
        this.trackedRace = new TrackedRaceDescriptor(BMW_CUP_REGATTA, BMW_CUP_BOAT_CLASS, String.format(RACE, 1));
        clearState(getContextRoot());
        super.setUp();
    }
    
    /**
     * <p>Test for the correct start and stop of a tracking.</p>
     */
    @Test
    public void testStartAndStopTrackingWithCorrectRegatta() {
        RegattaDescriptor bmwCupDescriptor = new RegattaDescriptor(BMW_CUP_EVENT, BMW_CUP_BOAT_CLASS);
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        RegattaStructureManagementPanelPO regattaStructure = adminConsole.goToRegattaStructure();
        regattaStructure.createRegatta(bmwCupDescriptor);
        TracTracEventManagementPanelPO tracTracEvents = adminConsole.goToTracTracEvents();
        tracTracEvents.addConnectionAndListTrackableRaces(BMW_CUP_JSON_URL);
        tracTracEvents.setReggataForTracking(bmwCupDescriptor);
        tracTracEvents.setTrackSettings(false, false, false);
        tracTracEvents.startTrackingForRace(this.trackableRace);
        TrackedRacesListPO trackedRacesList = tracTracEvents.getTrackedRacesList();
        trackedRacesList.waitForTrackedRace(this.trackedRace, Status.FINISHED); // with the TracAPI, REPLAY races reach status FINISHED when done loading
        assertThat(trackedRacesList.getStatus(this.trackedRace), equalTo(Status.FINISHED));
        trackedRacesList.stopTracking(this.trackedRace);
        trackedRacesList.waitForTrackedRace(this.trackedRace, Status.FINISHED);
        assertThat(trackedRacesList.getStatus(this.trackedRace), equalTo(Status.FINISHED));
    }
    
    @Test
    public void testStartTrackingWithDefaultReggataWhileReggataForBoatClassExists() {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        RegattaStructureManagementPanelPO regattaStructure = adminConsole.goToRegattaStructure();
        regattaStructure.createRegatta(new RegattaDescriptor(BMW_CUP_EVENT, BMW_CUP_BOAT_CLASS));
        TracTracEventManagementPanelPO tracTracEvents = adminConsole.goToTracTracEvents();
        tracTracEvents.addConnectionAndListTrackableRaces(BMW_CUP_JSON_URL);
        tracTracEvents.setReggataForTracking(DEFAULT_REGATTA);
        tracTracEvents.setTrackSettings(false, false, false);
        tracTracEvents.startTrackingForRacesAndAcceptDefaultRegattaWarning(this.trackableRace);
    }
    
    @Test
    public void testStartTrackingWithReggataAndNoneMatchingBoatClass() {
        RegattaDescriptor bmwCupDescriptor = new RegattaDescriptor(BMW_CUP_EVENT, BMW_CUP_BOAT_CLASS);
        RegattaDescriptor idm2013Descriptor = new RegattaDescriptor(IDM_2013_EVENT, IDM_2013_BOAT_CLASS);
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        RegattaStructureManagementPanelPO regattaStructure = adminConsole.goToRegattaStructure();
        regattaStructure.createRegatta(bmwCupDescriptor);
        regattaStructure.createRegatta(idm2013Descriptor);
        TracTracEventManagementPanelPO tracTracEvents = adminConsole.goToTracTracEvents();
        tracTracEvents.addConnectionAndListTrackableRaces(BMW_CUP_JSON_URL);
        tracTracEvents.setReggataForTracking(idm2013Descriptor);
        tracTracEvents.setTrackSettings(false, false, false);
        tracTracEvents.startTrackingForRaceAndAwaitBoatClassError(this.trackableRace, IDM_2013_BOAT_CLASS);
    }
}
