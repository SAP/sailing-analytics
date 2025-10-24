package com.sap.sailing.selenium.test.adminconsole;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.TracTracEventManagementPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

/**
 * <p>Tests for filtering of trackable TracTrac races.</p>
 * 
 * @author
 *   D049941
 */
public class TestRaceFilterForTracTracEvents extends AbstractSeleniumTest {
    private static final String BMW_CUP_JSON_URL =
            "http://event2.tractrac.com/events/event_20120803_BMWCup/jsonservice.php"; //$NON-NLS-1$
    
    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
    }
    
    /**
     * <p>Test with no filter at all, which means we expect all TracTrac races are displayed for a given event.</p>
     */
    @SeleniumTestCase
    public void testNoFilter() {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        TracTracEventManagementPanelPO tracTracEvents = adminConsole.goToTracTracEvents();
        tracTracEvents.addConnectionAndListTrackableRaces(BMW_CUP_JSON_URL);
        assertThat(tracTracEvents.getTrackableRaces().size(), is(12));
    }
    
    /**
     * <p>Test with partial filter, which should show multiple races but not all.</p>
     */
    @SeleniumTestCase
    public void testPartialFilter() {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        TracTracEventManagementPanelPO tracTracEvents = adminConsole.goToTracTracEvents();
        tracTracEvents.addConnectionAndListTrackableRaces(BMW_CUP_JSON_URL);
        tracTracEvents.setFilterForTrackableRaces("BMW Cup Race 1"); //$NON-NLS-1$
        assertThat(tracTracEvents.getTrackableRaces().size(), is(5));
    }
    
    /**
     * <p>Test with exact filter which should match only 1 races.</p>
     */
    @SeleniumTestCase
    public void testExactFilter() {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        TracTracEventManagementPanelPO tracTracEvents = adminConsole.goToTracTracEvents();
        tracTracEvents.addConnectionAndListTrackableRaces(BMW_CUP_JSON_URL);
        tracTracEvents.setFilterForTrackableRaces("BMW Cup Race 12"); //$NON-NLS-1$
        assertThat(tracTracEvents.getTrackableRaces().size(), is(1));
    }
    
    /**
     * <p>Test with filter which does not match anything.</p>
     */
    @SeleniumTestCase
    public void testNoneMatchingFilter() {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        TracTracEventManagementPanelPO tracTracEvents = adminConsole.goToTracTracEvents();
        tracTracEvents.addConnectionAndListTrackableRaces(BMW_CUP_JSON_URL);
        tracTracEvents.setFilterForTrackableRaces("BMW Cup Race 18"); //$NON-NLS-1$
        assertThat(tracTracEvents.getTrackableRaces().size(), is(0));
    }
}
