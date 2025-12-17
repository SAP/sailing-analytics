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
        final boolean didSucceed = authenticationMenu.attemptLogin("admin", "admin");
        assertTrue(didSucceed);
    }

    // test logic is brittle, it is expected that
    // login in test environment takes reliably under
    // 4 seconds
    @SeleniumTestCase
    public void testOffendingSignInWithTimedLock() throws InterruptedException {
        AdminConsolePage adminConsolePage = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        AuthenticationMenuPO authenticationMenu = adminConsolePage.getAuthenticationMenu();
        assertFalse(authenticationMenu.isOpen());
        assertFalse(authenticationMenu.isLoggedIn());
        // wrong login 1
        boolean didSucceed = authenticationMenu.attemptLogin("admin", "wrongPassword");
        assertFalse(didSucceed);
        Thread.sleep(2000); // wait for 2s lock to pass
        didSucceed = authenticationMenu.attemptLogin("admin", "wrongPassword");
        assertFalse(didSucceed);
        Thread.sleep(4000); // wait for 4s lock to pass
        didSucceed = authenticationMenu.attemptLogin("admin", "wrongPassword");
        assertFalse(didSucceed);
        // 4s lock window in place now
        // we assert that even correct credentials
        // attempted within this lock window will fail.
        didSucceed = authenticationMenu.attemptLogin("admin", "admin");
        assertFalse(didSucceed);
        Thread.sleep(8000); // wait for 8s lock to pass (inefficient, but correct)
        // correct credentials should work after lock window lapses
        didSucceed = authenticationMenu.attemptLogin("admin", "admin");
        assertTrue(didSucceed);
    }

}
