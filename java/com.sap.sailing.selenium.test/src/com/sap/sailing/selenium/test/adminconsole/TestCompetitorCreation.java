package com.sap.sailing.selenium.test.adminconsole;

import static com.sap.sailing.selenium.pages.PageObject.DEFAULT_POLLING_INTERVAL;
import static com.sap.sailing.selenium.pages.PageObject.DEFAULT_WAIT_TIMEOUT_SECONDS;
import static com.sap.sailing.selenium.pages.PageObject.createFluentWait;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.support.ui.FluentWait;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesBoatEditDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesBoatTablePO.BoatEntry;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesBoatsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesCompetitorEditDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesCompetitorTablePO.CompetitorEntry;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesCompetitorsPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestCompetitorCreation extends AbstractSeleniumTest {
    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
    }
    
    @SeleniumTestCase
    public void testOpenCreateCompetitorDialog() {
        final TrackedRacesCompetitorsPanelPO competitorsPanel = goToCompetitorsPanel();
        final TrackedRacesCompetitorEditDialogPO dialog = competitorsPanel.pushAddCompetitorButton(); // fails with an exception if the dialog is not found
        assertNotNull(dialog);
    }

    private TrackedRacesCompetitorsPanelPO goToCompetitorsPanel() {
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        final TrackedRacesCompetitorsPanelPO competitorsPanel = adminConsole.goToTrackedRacesCompetitors();
        return competitorsPanel;
    }

    @SeleniumTestCase
    public void testCompetitorCreation() {
        final TrackedRacesCompetitorsPanelPO competitorsPanel = goToCompetitorsPanel();
        final TrackedRacesCompetitorEditDialogPO dialog = competitorsPanel.pushAddCompetitorButton();
        final String name = ""+System.currentTimeMillis();
        final String shortName = "NRV";
        dialog.setNameTextBox(name);
        dialog.setShortNameTextBox(shortName);
        dialog.setWithBoat(false);
        dialog.pressOk();
        FluentWait<TrackedRacesCompetitorsPanelPO> wait = createFluentWait(competitorsPanel, DEFAULT_WAIT_TIMEOUT_SECONDS,
                DEFAULT_POLLING_INTERVAL);
        wait.until(trackedRaceCompetitorsPanel -> {
            boolean found = false;
            for (final CompetitorEntry it : trackedRaceCompetitorsPanel.getCompetitorTable().getEntries()) {
                String itName = it.getName();
                if (itName.equals(name)) {
                    found = true;
                    // found a candidate:
                    assertEquals(shortName, it.getShortName());
                }
            }
            return found;
        });
    }
    
    @SeleniumTestCase
    public void testCompetitorCreationWithBoat() {
        AdminConsolePage adminConsole = createBoatAndReturnAdminConsolePage();
        final TrackedRacesCompetitorsPanelPO competitorsPanel =  adminConsole.goToTrackedRacesCompetitors();
        final TrackedRacesCompetitorEditDialogPO dialog = competitorsPanel.pushAddCompetitorButton();
        final String name = "" + System.currentTimeMillis();
        final String shortName = "NRV";
        dialog.setNameTextBox(name);
        dialog.setShortNameTextBox(shortName);
        dialog.setWithBoat(true);
        
        final String boatName = "boat" + System.currentTimeMillis();
        dialog.setBoatNameText(boatName);
        String boadSailId = "boat" + System.currentTimeMillis();
        dialog.setBoatSailIdText(boadSailId);
        final String boatClassName = "49er";
        dialog.setBoatClassNameSuggest(boatClassName);
        
        dialog.pressOk();
        FluentWait<TrackedRacesCompetitorsPanelPO> wait = createFluentWait(competitorsPanel, DEFAULT_WAIT_TIMEOUT_SECONDS,
                DEFAULT_POLLING_INTERVAL);
        wait.until(trackedRaceCompetitorsPanel -> {
            boolean found = false;
            for (final CompetitorEntry it : trackedRaceCompetitorsPanel.getCompetitorTable().getEntries()) {
                String itName = it.getName();
                if (itName.equals(name)) {
                    found = true;
                    // found a candidate:
                    assertEquals(shortName, it.getShortName());
                }
            }
            return found;
        });
        // test refresh boat data on boats page
        final TrackedRacesBoatsPanelPO boatsPanel = adminConsole.goToTrackedRacesBoats();
        BoatEntry boatEntry = boatsPanel.waitForBoatEntry(boatName, boadSailId, boatClassName);
        TrackedRacesBoatEditDialogPO boatEditDialog = boatEntry.clickEditButton();
        final String newBoatName = "newBoat" + System.currentTimeMillis();
        boatEditDialog.setNameTextBox(newBoatName);
        boatEditDialog.clickOkButtonOrThrow();
        boatsPanel.waitForBoatEntry(newBoatName, boadSailId, boatClassName);
        
        final TrackedRacesCompetitorsPanelPO competitorsPanelPO = adminConsole.goToTrackedRacesCompetitors();
        CompetitorEntry competitorEntry = competitorsPanelPO.getCompetitorTable().getEntries().get(0);
        TrackedRacesCompetitorEditDialogPO competitorEditDialog = competitorEntry.clickEditWithBoatButton();
        assertEquals(newBoatName, competitorEditDialog.getBoatNameText());
    }
    

    public AdminConsolePage createBoatAndReturnAdminConsolePage() {
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        final TrackedRacesBoatsPanelPO boatsPanel = adminConsole.goToTrackedRacesBoats();
        final TrackedRacesBoatEditDialogPO dialog = boatsPanel.pushAddButton();
        final String name = "" + System.currentTimeMillis();
        dialog.setNameTextBox(name);
        String sailId = "" + System.currentTimeMillis();
        dialog.setSailIdTextBox(sailId);
        final String boatClassName = "49er";
        dialog.setBoatClassNameSuggestBox(boatClassName);
        dialog.pressOk();
        boatsPanel.waitForBoatEntry(name, sailId, boatClassName);
        return adminConsole;
    }

}
