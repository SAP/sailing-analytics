package com.sap.sailing.selenium.test.authentication;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.authentication.AuthenticationMenuPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestAuthenticationSignIn extends AbstractSeleniumTest {
    
    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        getWebDriver().manage().deleteCookieNamed("JSESSIONID");
    }
    
    @SeleniumTestCase
    public void testSignInWithExistingUserAdmin() {
        AdminConsolePage adminConsolePage = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        AuthenticationMenuPO authenticationMenu = adminConsolePage.getAuthenticationMenu();
        assertFalse(authenticationMenu.isOpen());
        assertFalse(authenticationMenu.isLoggedIn());
        authenticationMenu.doLogin("admin", "admin");
        assertTrue(authenticationMenu.isOpen());
        assertTrue(authenticationMenu.isLoggedIn());
    }

}
