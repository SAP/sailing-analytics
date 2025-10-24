package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.selenium.api.core.ApiContext.SECURITY_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static com.sap.sailing.selenium.api.core.ApiContext.createApiContext;
import static com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage.goToPage;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.event.EventApi;
import com.sap.sailing.selenium.api.event.SecurityApi;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO.EventEntryPO;
import com.sap.sailing.selenium.pages.adminconsole.security.AclActionInputPO;
import com.sap.sailing.selenium.pages.adminconsole.security.AclPopupPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class AclRevokeAnonymousTest extends AbstractSeleniumTest {

    private static final String DONALDS_PASSWORD = "daisy_LUYjl82.0815";

    private ApiContext ownerCtx;

    private final SecurityApi securityApi = new SecurityApi();
    private final EventApi eventApi = new EventApi();
    
    private static final String EVENT_NAME = "Super exclusive regatta - invited competitors only";
    private static final String BOAT_CLASS = "GC 32";
    private AdminConsolePage adminConsole;
    
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
        final ApiContext adminSecurityCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);
        securityApi.createUser(adminSecurityCtx, "donald", "Donald Duck", null, DONALDS_PASSWORD);
        ownerCtx = createApiContext(getContextRoot(), SERVER_CONTEXT, "donald", DONALDS_PASSWORD);
        adminConsole = goToPage(getWebDriver(), getContextRoot());
        adminConsole.goToLocalServerPanel().setSelfServiceServer(true);
        eventApi.createEvent(ownerCtx, EVENT_NAME, BOAT_CLASS, CompetitorRegistrationType.CLOSED, "Some special place");
    }

    @SeleniumTestCase
    public void test() {
        EventConfigurationPanelPO eventPanel = adminConsole.goToEvents();
        eventPanel.refreshEvents();
        EventEntryPO eventEntry = eventPanel.getEventEntry(EVENT_NAME);
        AclPopupPO aclPopup = eventEntry.openAclPopup();
        aclPopup.addUserGroup(""); // add empty user group -> anonymous group
        AclActionInputPO deniedInput = aclPopup.getDeniedActionsInput();
        AclActionInputPO allowedInput = aclPopup.getAllowedActionsInput();
        assertThat(deniedInput.isEnabled(), Matchers.equalTo(false));
        assertThat(allowedInput.isEnabled(), Matchers.equalTo(true));
        
        aclPopup.addUserGroup("admin-tenant");
        assertThat(deniedInput.isEnabled(), Matchers.equalTo(true));
        assertThat(allowedInput.isEnabled(), Matchers.equalTo(true));
    }

}
