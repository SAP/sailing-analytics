package com.sap.sailing.selenium.test.adminconsole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.connectors.ExpeditionDeviceConfigurationsPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

/**
 * <p>Tests for single selection table filtering and selection by Url-Parameters in expeditionDeviceConfiguratinos panel.</p>
 */
public class TestExpeditionDeviceConfigurationsFilterAndSelectByUrlParameters extends AbstractSeleniumTest {
    
    private static final String PARAM_FILTER = "filter";
    private static final String PARAM_SELECT = "select";
    private static final String PARAM_SELECT_EXACT = "selectExact";
    private static final String PARAM_FILTER_AND_SELECT = "filterAndSelect";
    
    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
        createTestExpeditionDeviceConfigurations();
    }
    
    @SeleniumTestCase
    public void testUrlParameterFilter() {        
        ExpeditionDeviceConfigurationsPanelPO configurationPanelPO = getExpeditionDeviceConfigurationsPanelPoOfPageWithUrlParams(); 
        List<String> availableConfigurations = configurationPanelPO.getAvailableExpeditionDeviceConfigurations();
        assertEquals(3, availableConfigurations.size()); 
        assertEquals(0, configurationPanelPO.getSelectedEntries().size());
        
        configurationPanelPO = getExpeditionDeviceConfigurationsPanelPoOfPageWithUrlParams(PARAM_FILTER, "Configuration");
        availableConfigurations = configurationPanelPO.getAvailableExpeditionDeviceConfigurations();
        assertEquals(2, availableConfigurations.size());
        assertEquals(0, configurationPanelPO.getSelectedEntries().size());
        
        configurationPanelPO = getExpeditionDeviceConfigurationsPanelPoOfPageWithUrlParams(PARAM_FILTER_AND_SELECT, "Test");
        availableConfigurations = configurationPanelPO.getAvailableExpeditionDeviceConfigurations();
        assertEquals(1, availableConfigurations.size());
        assertEquals("Test Configuration", availableConfigurations.get(0));
        assertEquals(1, configurationPanelPO.getSelectedEntries().size());
        assertEquals("Test Configuration", configurationPanelPO.getSelectedEntries().get(0).getName());
    }
    
    @SeleniumTestCase
    public void testUrlParameterSelect() {       
        ExpeditionDeviceConfigurationsPanelPO configurationPanelPO = getExpeditionDeviceConfigurationsPanelPoOfPageWithUrlParams(PARAM_SELECT, "Test"); 
        List<String> availableConfigurations = configurationPanelPO.getAvailableExpeditionDeviceConfigurations();
        assertEquals(3, availableConfigurations.size()); 
        assertEquals(1, configurationPanelPO.getSelectedEntries().size());
        assertEquals("Test Configuration", configurationPanelPO.getSelectedEntries().get(0).getName());
        
        configurationPanelPO = getExpeditionDeviceConfigurationsPanelPoOfPageWithUrlParams(PARAM_FILTER_AND_SELECT, "Configuration", PARAM_SELECT, "Configuration", PARAM_SELECT_EXACT, "Test");
        availableConfigurations = configurationPanelPO.getAvailableExpeditionDeviceConfigurations();
        assertEquals(2, availableConfigurations.size());
        assertEquals(1, configurationPanelPO.getSelectedEntries().size());
        assertTrue(configurationPanelPO.getSelectedEntries().get(0).getName().contains("Configuration"));
        
        configurationPanelPO = getExpeditionDeviceConfigurationsPanelPoOfPageWithUrlParams(PARAM_SELECT, "Configuration", PARAM_SELECT_EXACT, "Expedition Device Configuration");
        availableConfigurations = configurationPanelPO.getAvailableExpeditionDeviceConfigurations();
        assertEquals(3, availableConfigurations.size());
        assertEquals(1, configurationPanelPO.getSelectedEntries().size());
        assertEquals("Expedition Device Configuration", configurationPanelPO.getSelectedEntries().get(0).getName());
        
        configurationPanelPO = getExpeditionDeviceConfigurationsPanelPoOfPageWithUrlParams(PARAM_FILTER_AND_SELECT, "Configuration", PARAM_SELECT, "Configuration", PARAM_SELECT_EXACT, "Expedition Device Configuration");
        availableConfigurations = configurationPanelPO.getAvailableExpeditionDeviceConfigurations();
        assertEquals(2, availableConfigurations.size());
        assertEquals(1, configurationPanelPO.getSelectedEntries().size());
        assertEquals("Expedition Device Configuration", configurationPanelPO.getSelectedEntries().get(0).getName());
        
        configurationPanelPO = getExpeditionDeviceConfigurationsPanelPoOfPageWithUrlParams(PARAM_FILTER_AND_SELECT, "Configuration", PARAM_SELECT_EXACT, "Expedition Device Configuration");
        availableConfigurations = configurationPanelPO.getAvailableExpeditionDeviceConfigurations();
        assertEquals(2, availableConfigurations.size());
        assertEquals(1, configurationPanelPO.getSelectedEntries().size());
        assertEquals("Expedition Device Configuration", configurationPanelPO.getSelectedEntries().get(0).getName());
        
        configurationPanelPO = getExpeditionDeviceConfigurationsPanelPoOfPageWithUrlParams(PARAM_FILTER, "Configuration", PARAM_SELECT, "Test");
        availableConfigurations = configurationPanelPO.getAvailableExpeditionDeviceConfigurations();
        assertEquals(2, availableConfigurations.size());
        assertEquals(1, configurationPanelPO.getSelectedEntries().size());
        assertEquals("Test Configuration", configurationPanelPO.getSelectedEntries().get(0).getName());
        
        configurationPanelPO = getExpeditionDeviceConfigurationsPanelPoOfPageWithUrlParams(PARAM_FILTER_AND_SELECT, "Configuration", PARAM_SELECT, "Configuration");
        availableConfigurations = configurationPanelPO.getAvailableExpeditionDeviceConfigurations();
        assertEquals(2, availableConfigurations.size());
        assertEquals(1, configurationPanelPO.getSelectedEntries().size());
        assertTrue(configurationPanelPO.getSelectedEntries().get(0).getName().contains("Configuration"));
        
        configurationPanelPO = getExpeditionDeviceConfigurationsPanelPoOfPageWithUrlParams(PARAM_FILTER_AND_SELECT, "Configurations", PARAM_SELECT, "abc");
        availableConfigurations = configurationPanelPO.getAvailableExpeditionDeviceConfigurations();
        assertEquals(0, availableConfigurations.size());
        assertEquals(0, configurationPanelPO.getSelectedEntries().size());      
    }    
    
    private void createTestExpeditionDeviceConfigurations() {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        ExpeditionDeviceConfigurationsPanelPO expeditionDeviceConfigurations = adminConsole.goToExpeditionDeviceConfigurationsPanel();
        
        expeditionDeviceConfigurations.createExpeditionDeviceConfiguration("505 World 2017", "0");
        expeditionDeviceConfigurations.createExpeditionDeviceConfiguration("Expedition Device Configuration", "1");
        expeditionDeviceConfigurations.createExpeditionDeviceConfiguration("Test Configuration", "2");
    }
    
    private ExpeditionDeviceConfigurationsPanelPO getExpeditionDeviceConfigurationsPanelPoOfPageWithUrlParams(String ...keysValues) {
        AdminConsolePage adminConsole = AdminConsolePage.goToExpeditionDeviceConfigurationsPlace(getWebDriver(), getContextRoot(), keysValues);
        return adminConsole.getExpeditionDeviceConfigurationsPanelPO();
    }
}
