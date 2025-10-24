package com.sap.sailing.selenium.test.adminconsole;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.connectors.ResultImportUrlsPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestConnectorsResultImportUrls extends AbstractSeleniumTest {

    private final static String TEST_URL = "https://www.example.org";
    private final static String TEST_URL_PROVIDER_LABEL = "FREG HTML Score Importer";

    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
    }

    private ResultImportUrlsPanelPO goToResultImportUrlsPanel() {
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        final ResultImportUrlsPanelPO resultImportUrlsPanel = adminConsole.goToResultImportUrlsPanel();
        return resultImportUrlsPanel;
    }

    @SeleniumTestCase
    public void testUrlCreationAndDeletion() {
        final ResultImportUrlsPanelPO resultImportUrlsPanel = goToResultImportUrlsPanel();
        resultImportUrlsPanel.selectUrlProviderByLabel(TEST_URL_PROVIDER_LABEL);
        // add
        resultImportUrlsPanel.addUrl(TEST_URL);
        resultImportUrlsPanel.removeUrl(TEST_URL);
    }

    @SeleniumTestCase
    public void testUrlInlineDeletion() {
        final ResultImportUrlsPanelPO resultImportUrlsPanel = goToResultImportUrlsPanel();
        resultImportUrlsPanel.selectUrlProviderByLabel(TEST_URL_PROVIDER_LABEL);
        // add
        resultImportUrlsPanel.addUrl(TEST_URL);
        resultImportUrlsPanel.removeWithInlineButton(TEST_URL);
    }
}
