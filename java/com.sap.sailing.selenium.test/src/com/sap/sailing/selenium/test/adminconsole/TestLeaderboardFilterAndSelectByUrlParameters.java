package com.sap.sailing.selenium.test.adminconsole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardConfigurationPanelPO.LeaderboardEntryPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

/**
 * <p>Tests for filtering and selection by Url-Parameters in leaderboards panel.</p>
 */
public class TestLeaderboardFilterAndSelectByUrlParameters extends AbstractSeleniumTest {
    
    private static final String PARAM_FILTER = "filter";
    private static final String PARAM_SELECT = "select";
    private static final String PARAM_SELECT_EXACT = "selectExact";
    private static final String PARAM_FILTER_AND_SELECT = "filterAndSelect";
    
    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
        createTestLeaderboards();
    }
    
    @SeleniumTestCase
    public void testUrlParameterFilter() {        
        List<String> availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_FILTER, "505");
        assertEquals(4, availableLeaderboards.size()); 
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_FILTER, "2017");
        assertEquals(2, availableLeaderboards.size());
        assertEquals("505 World 2017", availableLeaderboards.get(0));
        assertEquals("505 Pre-World 2017", availableLeaderboards.get(1));
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_FILTER, "\"\"");
        assertEquals(4, availableLeaderboards.size());        
    }
    
    @SeleniumTestCase
    public void testUrlParameterSelect() {  
        LeaderboardConfigurationPanelPO leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT, "505");        
        List<String> availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();      
        assertEquals(4, availableLeaderboards.size()); 
        List<LeaderboardEntryPO> selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(4, selectedEntries.size());
        
        leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT, "2017");        
        availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();      
        assertEquals(4, availableLeaderboards.size()); 
        selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(2, selectedEntries.size());
        assertEquals("505 World 2017", selectedEntries.get(0).getName());
        assertEquals("505 Pre-World 2017", selectedEntries.get(1).getName());
    }
    
    @SeleniumTestCase
    public void testUrlParameterFilterAndSelect() {
        LeaderboardConfigurationPanelPO leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_FILTER_AND_SELECT, "505");        
        List<String> availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();      
        assertEquals(4, availableLeaderboards.size()); 
        List<LeaderboardEntryPO> selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(4, selectedEntries.size());
        
        leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_FILTER_AND_SELECT, "505 World 2017");        
        availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();      
        assertEquals(2, availableLeaderboards.size()); 
        selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(2, selectedEntries.size());
        assertEquals("505 World 2017", selectedEntries.get(0).getName());
        assertEquals("505 Pre-World 2017", selectedEntries.get(1).getName());      
    }
    
    @SeleniumTestCase
    public void testUrlParameterSelectExact() { 
        LeaderboardConfigurationPanelPO leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT_EXACT, "505");        
        List<String> availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();      
        assertEquals(4, availableLeaderboards.size()); 
        List<LeaderboardEntryPO> selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(0, selectedEntries.size());
        
        leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT_EXACT, "505 World 2017");        
        availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();      
        assertEquals(4, availableLeaderboards.size()); 
        selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(1, selectedEntries.size());
        assertEquals("505 World 2017", selectedEntries.get(0).getName());
    }
    
    @SeleniumTestCase
    public void testUrlParametersSelectExactAndSelect() {     
        LeaderboardConfigurationPanelPO leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT_EXACT, 
                "505 World 2017", PARAM_SELECT, "2017");        
        List<String> availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();
        assertEquals(4, availableLeaderboards.size());
        List<LeaderboardEntryPO> selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(2, selectedEntries.size());
        assertEquals("505 World 2017", selectedEntries.get(0).getName());
        assertEquals("505 Pre-World 2017", selectedEntries.get(1).getName());
        
        leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT_EXACT, "505", PARAM_SELECT, "2017");        
        availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();      
        assertEquals(4, availableLeaderboards.size()); 
        selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(2, selectedEntries.size());
        assertEquals("505 World 2017", selectedEntries.get(0).getName());
        assertEquals("505 Pre-World 2017", selectedEntries.get(1).getName());
    }
    
    @SeleniumTestCase
    public void testUrlParametersSelectExactAndFilterAndSelect() {
        LeaderboardConfigurationPanelPO leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT_EXACT, 
                "505", PARAM_FILTER_AND_SELECT, "2017");        
        List<String> availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();      
        assertEquals(2, availableLeaderboards.size()); 
        List<LeaderboardEntryPO> selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(2, selectedEntries.size());
        
        leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT_EXACT, "505 World 2017", 
                PARAM_FILTER_AND_SELECT, "2017");        
        availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();
        assertEquals(2, availableLeaderboards.size());
        selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(2, selectedEntries.size());
        assertEquals("505 World 2017", selectedEntries.get(0).getName());  
        assertEquals("505 Pre-World 2017", selectedEntries.get(1).getName());   
    }
    
    @SeleniumTestCase
    public void testAllSelectionUrlParameters() {
        LeaderboardConfigurationPanelPO leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT_EXACT, "505", 
                PARAM_FILTER_AND_SELECT, "2017", PARAM_SELECT, "2015");        
        List<String> availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();      
        assertEquals(2, availableLeaderboards.size()); 
        List<LeaderboardEntryPO> selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(2, selectedEntries.size());
        
        leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT_EXACT, "505 World 2017", 
                PARAM_FILTER_AND_SELECT, "2017", PARAM_SELECT, "World");        
        availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();
        assertEquals(2, availableLeaderboards.size());
        selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(2, selectedEntries.size());
        assertEquals("505 World 2017", selectedEntries.get(0).getName());
        assertEquals("505 Pre-World 2017", selectedEntries.get(1).getName());
        
        leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT_EXACT, "505 World 2017", 
                PARAM_FILTER_AND_SELECT, "2017", PARAM_SELECT, "2015");        
        availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();
        assertEquals(2, availableLeaderboards.size());
        selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(2, selectedEntries.size());
        assertEquals("505 World 2017", selectedEntries.get(0).getName());
        assertEquals("505 Pre-World 2017", selectedEntries.get(1).getName());
    }
    
    @SeleniumTestCase
    public void testAllSelectionAndFilterUrlParameters() {
        LeaderboardConfigurationPanelPO leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT_EXACT, "505", 
                PARAM_FILTER_AND_SELECT, "2017", PARAM_SELECT, "2015", PARAM_FILTER, "2017");
        List<String> availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();
        assertEquals(2, availableLeaderboards.size());
        List<LeaderboardEntryPO> selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(2, selectedEntries.size());
        
        leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT_EXACT, "505 World 2017", 
                PARAM_FILTER_AND_SELECT, "2017", PARAM_SELECT, "World", PARAM_FILTER, "505");
        availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();
        assertEquals(2, availableLeaderboards.size());
        selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(2, selectedEntries.size());
        assertEquals("505 World 2017", selectedEntries.get(0).getName());
        
        leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT_EXACT, "505 World 2017", 
                PARAM_FILTER_AND_SELECT, "2015", PARAM_SELECT, "World", PARAM_FILTER, "2017");
        availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();
        assertEquals(0, availableLeaderboards.size());
        selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(0, selectedEntries.size());
        
        leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT_EXACT, "505 World 2017", 
                PARAM_FILTER_AND_SELECT, "2015", PARAM_SELECT, "World", PARAM_FILTER, "2015");
        availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();
        assertEquals(1, availableLeaderboards.size());
        selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(1, selectedEntries.size());  
        
        leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_SELECT_EXACT, "505 World 2017", 
                PARAM_FILTER_AND_SELECT, "2017", PARAM_SELECT, "2015", PARAM_FILTER, "505");
        availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();
        assertEquals(2, availableLeaderboards.size());
        selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(2, selectedEntries.size()); 
    }
    
    @SeleniumTestCase
    public void testUrlParameterDecoding() {
        LeaderboardConfigurationPanelPO leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams();                    
        leaderboardConfigurationPanelPO.createFlexibleLeaderboard("Sailing & 2011");
        leaderboardConfigurationPanelPO.createFlexibleLeaderboard("Sailing&2015");
        leaderboardConfigurationPanelPO.createFlexibleLeaderboard("&");
        
        List<String> availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_FILTER, "2017 %26");
        assertEquals(0, availableLeaderboards.size());
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_FILTER, "2011 %26", PARAM_SELECT, "505");
        assertEquals(1, availableLeaderboards.size());
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_FILTER, "Sailing %26 2011");
        assertEquals(1, availableLeaderboards.size());
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_FILTER, "%26", PARAM_SELECT, "%26");
        assertEquals(3, availableLeaderboards.size());
        List<LeaderboardEntryPO> selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(3, selectedEntries.size()); 
        assertTrue(selectedEntries.stream().anyMatch(se -> "&".equals(se.getName())));
        assertTrue(selectedEntries.stream().anyMatch(se -> "Sailing & 2011".equals(se.getName())));
        assertTrue(selectedEntries.stream().anyMatch(se -> "Sailing&2015".equals(se.getName())));


        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_FILTER, "%26", PARAM_SELECT_EXACT, "%26");
        assertEquals(3, availableLeaderboards.size());
        selectedEntries = leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries();
        assertEquals(1, selectedEntries.size()); 
        assertEquals("&", selectedEntries.get(0).getName());
    }
    
    @SeleniumTestCase
    public void testResetFilterWhenSelectionUrlParametersOnly() {  
        List<String> availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_FILTER, "2017");
        assertEquals(2, availableLeaderboards.size()); 
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_SELECT, "505");
        assertEquals(4, availableLeaderboards.size()); 
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_FILTER_AND_SELECT, "2017");
        assertEquals(2, availableLeaderboards.size());
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_SELECT_EXACT, "505");
        assertEquals(4, availableLeaderboards.size()); 
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_FILTER_AND_SELECT, "x");
        assertEquals(0, availableLeaderboards.size());        
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_SELECT_EXACT, "505 World 2017");
        assertEquals(4, availableLeaderboards.size()); 
    }
    
    @SeleniumTestCase
    public void testResetSelectWhenFilterUrlParametersOnly() {  
        LeaderboardConfigurationPanelPO leaderboardConfigurationPanelPO = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(PARAM_FILTER_AND_SELECT, "2017");             
        List<String> availableLeaderboards = leaderboardConfigurationPanelPO.getAvailableLeaderboards();
        assertEquals(2, availableLeaderboards.size()); 
        assertEquals(2, leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries().size()); 
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_FILTER, "505");
        assertEquals(4, availableLeaderboards.size());
        assertEquals(0, leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries().size());
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_SELECT, "2017");     
        assertEquals(4, availableLeaderboards.size());
        assertEquals(2, leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries().size()); 
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_FILTER, "505");
        assertEquals(4, availableLeaderboards.size());
        assertEquals(0, leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries().size());
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_SELECT_EXACT, "505 World 2017");     
        assertEquals(4, availableLeaderboards.size());
        assertEquals(1, leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries().size()); 
        
        availableLeaderboards = getLeaderboardsOfPageWithUrlParams(PARAM_FILTER, "505");
        assertEquals(4, availableLeaderboards.size());
        assertEquals(0, leaderboardConfigurationPanelPO.getLeaderboardTable().getSelectedEntries().size());
    }
    
    private void createTestLeaderboards() {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        LeaderboardConfigurationPanelPO leaderboardConfiguration = adminConsole.goToLeaderboardConfiguration();
        
        leaderboardConfiguration.createFlexibleLeaderboard("505 World 2017");
        leaderboardConfiguration.createFlexibleLeaderboard("505 Pre-World 2017");
        leaderboardConfiguration.createFlexibleLeaderboard("505 World 2015");
        leaderboardConfiguration.createFlexibleLeaderboard("505 World 2011");
        //leaderboardConfiguration.refreshLeaderboard();
    }
    
    private List<String> getLeaderboardsOfPageWithUrlParams(String ...keysValues) {
        LeaderboardConfigurationPanelPO leaderboardConfiguration = getLeaderboardConfigurationPanelPoOfPageWithUrlParams(keysValues);
        return leaderboardConfiguration.getAvailableLeaderboards();
    }
    
    private LeaderboardConfigurationPanelPO getLeaderboardConfigurationPanelPoOfPageWithUrlParams(String ...keysValues) {
        AdminConsolePage adminConsole = AdminConsolePage.goToLeaderboardConfigurationPlace(getWebDriver(), getContextRoot(), keysValues);
        return adminConsole.getLeaderboardConfigurationPanelPO();
    }
}
