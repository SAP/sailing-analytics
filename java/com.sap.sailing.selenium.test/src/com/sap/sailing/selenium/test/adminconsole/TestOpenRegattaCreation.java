package com.sap.sailing.selenium.test.adminconsole;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaStructureManagementPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestOpenRegattaCreation extends AbstractSeleniumTest {

    private static final String REGATTA = "The BMW Cup"; //$NON-NLS-1$
    private static final String BOAT_CLASS = "J80"; //$NON-NLS-1$
    private static final String REGISTRATION_LINK_SECRET = "ABCD"; //$NON-NLS-1$

    private RegattaDescriptor regatta;

    @Override
    @BeforeEach
    public void setUp() {
        this.regatta = new RegattaDescriptor(REGATTA, BOAT_CLASS, CompetitorRegistrationType.OPEN_UNMODERATED,
                REGISTRATION_LINK_SECRET);
        clearState(getContextRoot());
        super.setUp();
        configureRegattaAndLeaderboard();
    }

    @SeleniumTestCase
    public void test() {

    }

    private void configureRegattaAndLeaderboard() {
        AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        RegattaStructureManagementPanelPO regattaStructure = adminConsole.goToRegattaStructure();
        regattaStructure.createRegatta(this.regatta, /* withDefaultLeaderboard */ false);
    }
}
