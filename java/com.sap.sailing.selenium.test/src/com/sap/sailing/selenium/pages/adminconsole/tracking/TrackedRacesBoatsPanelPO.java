package com.sap.sailing.selenium.pages.adminconsole.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesBoatTablePO.BoatEntry;

public class TrackedRacesBoatsPanelPO extends PageArea {    
    @FindBy(how = BySeleniumId.class, using = "RefreshButton")
    private WebElement refreshButton;
    
    @FindBy(how = BySeleniumId.class, using = "AddBoatButton")
    private WebElement addButton;
    
    @FindBy(how = BySeleniumId.class, using = "BoatsTable")
    private WebElement boatsTable;
    
    public final WebDriver driver;

    public TrackedRacesBoatsPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
        this.driver = driver;
    }
    
    public TrackedRacesBoatEditDialogPO pushAddButton() {
        this.addButton.click();
        waitForAjaxRequests();
        WebElement dialog = findElementBySeleniumId(this.driver, "BoatEditDialog");
        return new TrackedRacesBoatEditDialogPO(this.driver, dialog);
    }
    
    public void pushRefreshButton() {
        this.refreshButton.click();
        waitForAjaxRequests();
    }
    
    public TrackedRacesBoatTablePO getBoatsTable() {
        return new TrackedRacesBoatTablePO(this.driver, this.boatsTable);
    }
    
    public BoatEntry waitForBoatEntry(String name, String sailId, String boatClassName) {
        final List<BoatEntry> findings = new ArrayList<TrackedRacesBoatTablePO.BoatEntry>();
        waitUntil(() -> {
            boolean boatFound = false;
            for (final BoatEntry it : getBoatsTable().getEntries()) {
                String itName = it.getName();
                if (itName.equals(name)) {
                    boatFound = true;
                    findings.add(it);
                    // found a candidate:
                    assertEquals(sailId, it.getSailId());
                    assertEquals(boatClassName, it.getBoatClassName());
                }
            }
            return boatFound;
        });
        BoatEntry result = null;
        if (findings.size() > 0) {
            result = findings.get(0);
        }
        return result;
    }
}
