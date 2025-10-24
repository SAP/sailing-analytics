package com.sap.sailing.selenium.test.adminconsole;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesBoatEditDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesBoatsPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestBoatCreation extends AbstractSeleniumTest {
    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
    }
    
    @SeleniumTestCase
    public void testOpenCreateBoatDialog() {
        final TrackedRacesBoatsPanelPO competitorsPanel = goToBoatsPanel();
        final TrackedRacesBoatEditDialogPO dialog = competitorsPanel.pushAddButton(); // fails with an exception if the dialog is not found
        assertNotNull(dialog);
    }

    private TrackedRacesBoatsPanelPO goToBoatsPanel() {
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        final TrackedRacesBoatsPanelPO boatsPanel = adminConsole.goToTrackedRacesBoats();
        return boatsPanel;
    }
    
    @SeleniumTestCase
    public void testBoatCreation() {
        final TrackedRacesBoatsPanelPO boatsPanel = goToBoatsPanel();
        final TrackedRacesBoatEditDialogPO dialog = boatsPanel.pushAddButton();
        final String name = ""+System.currentTimeMillis();
        dialog.setNameTextBox(name);
        String sailId = ""+System.currentTimeMillis();
        dialog.setSailIdTextBox(sailId);
        final String boatClassName = "49er";
        dialog.setBoatClassNameSuggestBox(boatClassName);
        dialog.pressOk();
        boatsPanel.waitForBoatEntry(name, sailId, boatClassName);
    }
}
