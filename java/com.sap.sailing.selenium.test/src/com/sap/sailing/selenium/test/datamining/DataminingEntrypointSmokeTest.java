package com.sap.sailing.selenium.test.datamining;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.datamining.DataMiningPage;
import com.sap.sailing.selenium.pages.datamining.DataMiningPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class DataminingEntrypointSmokeTest extends AbstractSeleniumTest {
    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
    }

    @SeleniumTestCase
    public void testDataminingEntrypoint() {
        final DataMiningPage page = DataMiningPage.goToPage(getWebDriver(), getContextRoot());
        final DataMiningPanelPO dataMiningPanel = page.getDataMiningPanel();
        assertTrue(dataMiningPanel.isRunButtonAvailable());
        assertTrue(dataMiningPanel.isExtractionFunctionSuggestBoxAvailable());
    }
}
