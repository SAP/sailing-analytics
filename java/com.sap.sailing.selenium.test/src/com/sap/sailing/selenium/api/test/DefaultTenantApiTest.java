package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage.goToPage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.selenium.api.event.EventApi;
import com.sap.sailing.selenium.api.event.SecurityApi;
import com.sap.sailing.selenium.api.event.UserGroupApi;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class DefaultTenantApiTest extends AbstractSeleniumTest {

    private static final String DONALDS_PASSWORD = "dais2097430*:JH['y0815";
    private static final String TENANT_GROUP_NAME = "NewGroup";
    private static final String EVENT_NAME = "TestEvent2";
    private AdminConsolePage adminConsole;
    private UUID tenantGroupId;
    private String userToken;

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
        userToken = SecurityApi.createUser("donald", DONALDS_PASSWORD).run().getAccessToken();
        adminConsole = goToPage(getWebDriver(), getContextRoot());
        adminConsole.goToLocalServerPanel().setSelfServiceServer(true);
        tenantGroupId = UserGroupApi.createUserGroup(TENANT_GROUP_NAME).auth(userToken).run().getGroupId();
    }

    @SeleniumTestCase
    public void testEventApiWithUserTenant() {
        EventApi.create(EVENT_NAME, "GC 32", CompetitorRegistrationType.CLOSED, "somewhere").auth(userToken).run();
        EventConfigurationPanelPO configurationPanelPO =  adminConsole.goToEvents();
        configurationPanelPO.refreshEvents();
        String groupColumnContent = configurationPanelPO.getEventEntry(EVENT_NAME).getColumnContent("Group");
        assertEquals("donald-tenant", groupColumnContent);
    }

    @SeleniumTestCase
    public void testEventApiWithDefaultTenant() {
        EventApi.create(EVENT_NAME, "GC 32", CompetitorRegistrationType.CLOSED, "somewhere")
                .header("tenantGroupId", tenantGroupId.toString()).auth(userToken).run();
        EventConfigurationPanelPO configurationPanelPO =  adminConsole.goToEvents();
        configurationPanelPO.refreshEvents();
        String groupColumnContent = configurationPanelPO.getEventEntry(EVENT_NAME).getColumnContent("Group");
        assertEquals(TENANT_GROUP_NAME, groupColumnContent);
    }
}
