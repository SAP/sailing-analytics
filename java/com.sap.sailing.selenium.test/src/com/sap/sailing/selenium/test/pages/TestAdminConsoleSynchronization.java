package com.sap.sailing.selenium.test.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.sap.sailing.selenium.core.AjaxCallsComplete;
import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.AddTracTracConnectionDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.tractrac.TracTracEventManagementPanelPO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.pages.gwt.GenericCellTablePO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;
import com.sap.sailing.selenium.test.pages.GwtRpcResponseProxy.ResponseGate;

/** Real-browser regressions with controlled response timing, using the actual GWT client and SailingServer. */
public class TestAdminConsoleSynchronization extends AbstractSeleniumTest {
    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
    }

    @SeleniumTestCase
    public void waitsForDelayedWhatsNewDecisionAndPreferenceSave() throws Exception {
        try (final GwtRpcResponseProxy proxy = new GwtRpcResponseProxy(getContextRoot())) {
            final ResponseGate changelog = proxy.delay("getAdminConsoleChangeLogSize", /* requiredText */ "",
                    /* delayMillis */ 20000);
            final ResponseGate preference = proxy.delay("getPreference", "sailing.whatsnew.admin", /* delayMillis */ 2000);
            final ResponseGate save = proxy.delay("setPreference", "sailing.whatsnew.admin", /* delayMillis */ 2000);
            final AdminConsolePage page = AdminConsolePage.goToPage(getWebDriver(), proxy.getRoot());
            assertTrue(changelog.isDelivered(), "Page readiness must include the delayed changelog decision");
            assertTrue(preference.isDelivered(), "Page readiness must include the delayed preference lookup");
            assertTrue(save.isDelivered(), "Dismissing What's New must wait for its preference to be saved");
            assertTrue(getWebDriver().findElements(new BySeleniumId("AdminWhatsNewDialog")).isEmpty());
            page.goToRegattaStructure();
            proxy.assertHealthy();
        }
    }

    @SeleniumTestCase
    public void ignoresAnOlderEmptyConnectionListDeliveredAfterCreation() throws Exception {
        try (final GwtRpcResponseProxy proxy = new GwtRpcResponseProxy(getContextRoot())) {
            final AdminConsolePage page = AdminConsolePage.goToPage(getWebDriver(), proxy.getRoot());
            final TracTracEventManagementPanelPO panel = page.goToTracTracEvents();
            final ResponseGate olderSnapshot = proxy.hold("getPreviousTracTracConfigurations");
            getWebDriver().findElement(new BySeleniumId("RefreshTracTracConnectionsButton")).click();
            olderSnapshot.awaitResponse();
            assertFalse(new AjaxCallsComplete().apply(getWebDriver()),
                    "An outstanding connection refresh must remain visible to AJAX readiness checks");
            // The older response contains no configurations; keep it held until the post-create result is rendered.
            final String url = "http://localhost/bug6355/connection";
            final AddTracTracConnectionDialogPO dialog = panel.addConnection();
            dialog.setJsonUrl(url);
            dialog.pressOk(/* acceptAlert */ false, /* waitForAjaxRequests */ false);
            panel.waitForConnectionEntry(url);
            olderSnapshot.release();
            new WebDriverWait(getWebDriver(), Duration.ofSeconds(/* seconds */ 60)).until(new AjaxCallsComplete());
            final GenericCellTablePO<DataEntryPO> table = new GenericCellTablePO<>(getWebDriver(),
                    getWebDriver().findElement(new BySeleniumId("TracTracConfigurationWithSecurityDTOTable")), DataEntryPO.class);
            assertEquals(/* expected */ 1, table.getEntries().size(), "The stale empty snapshot must not erase the new connection");
            assertEquals(url, table.getEntries().get(/* index */ 0).getColumnContent("JSON URL"));
            assertTrue(olderSnapshot.isDelivered());
            proxy.assertHealthy();
        }
    }
}
