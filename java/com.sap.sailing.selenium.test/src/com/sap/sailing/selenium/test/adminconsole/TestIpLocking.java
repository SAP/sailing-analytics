package com.sap.sailing.selenium.test.adminconsole;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.core.HttpException.Unauthorized;
import com.sap.sailing.selenium.api.event.PreferencesApi;
import com.sap.sailing.selenium.api.event.SecurityApi;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.advanced.IpBlocklistPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestIpLocking extends AbstractSeleniumTest {
    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
    }

    @SeleniumTestCase
    public void testUnlockingForBearerTokenAbuser() throws InterruptedException {
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        final IpBlocklistPanelPO tablePO = adminConsole.goToLocalServerPanel().getBearerTokenAbusePO();
        attemptBearerTokenAbuse(5);
        tablePO.refresh();
        final String ip = "127.0.0.1";
        assertTrue(tablePO.expectIpInTable(ip));
        tablePO.unblockIP(ip);
        assertFalse(tablePO.expectIpInTable(ip));
        attemptValidBearerTokenUse();
    }

    private void attemptValidBearerTokenUse() {
        // prepare api
        final ApiContext ctx = ApiContext.createAdminApiContext(getContextRoot(), ApiContext.SECURITY_CONTEXT);
        final Map<String, String> prefObjectAttr = new HashMap<String, String>();
        prefObjectAttr.put("key1", "value1");
        PreferencesApi preferencesApi = new PreferencesApi();
        preferencesApi.createPreference(ctx, "pref1", prefObjectAttr);
    }

    private void attemptBearerTokenAbuse(final int attempts) throws InterruptedException {
        // prepare api
        final ApiContext wrongCtx = ApiContext.createApiContextWithInvalidToken(getContextRoot(),
                ApiContext.SECURITY_CONTEXT);
        final Map<String, String> prefObjectAttr = new HashMap<String, String>();
        prefObjectAttr.put("key1", "value1");
        final PreferencesApi preferencesApi = new PreferencesApi();
        for (int i = 0; i < attempts; i++) {
            // call api
            try {
                preferencesApi.createPreference(wrongCtx, "pref1", prefObjectAttr);
            } catch (Unauthorized e) {
                // do nothing as this is expected
            }
            // wait for lock to expire
            long lockDuration = (long) Math.pow(2, i) * 1000;
            boolean isFinalAttempt = i == (attempts - 1);
            if (!isFinalAttempt) {
                Thread.sleep(lockDuration);
            }
        }
    }

    @SeleniumTestCase
    public void testUnlockingForUserCreationAbuser() throws InterruptedException {
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        final IpBlocklistPanelPO tablePO = adminConsole.goToLocalServerPanel().getUserCreationAbusePO();
        attemptUserCreationAbuse(5);
        tablePO.refresh();
        final String ip = "127.0.0.1";
        assertTrue(tablePO.expectIpInTable(ip));
        tablePO.unblockIP(ip);
        assertFalse(tablePO.expectIpInTable(ip));
        attemptValidBearerTokenUse();
    }

    private void attemptUserCreationAbuse(final int attempts) throws InterruptedException {
        for (int i = 0; i < attempts; i++) {
            attemptUserCreation(String.valueOf(i));
            // wait for lock to expire
            long lockDuration = (long) Math.pow(2, i) * 1000;
            boolean isFinalAttempt = i == (attempts - 1);
            if (!isFinalAttempt) {
                Thread.sleep(lockDuration);
            }
        }
    }

    private boolean attemptUserCreation(String seed) {
        try {
            SecurityApi.createUser("USERNAME" + seed, "PASSWORD").run();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
