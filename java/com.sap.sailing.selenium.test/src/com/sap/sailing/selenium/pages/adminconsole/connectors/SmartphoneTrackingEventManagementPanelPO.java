package com.sap.sailing.selenium.pages.adminconsole.connectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.adminconsole.tracking.RaceColumnTableWrapperPO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.pages.gwt.GenericCellTablePO;
import com.sap.sailing.selenium.test.adminconsole.smartphonetracking.MapDevicesDialogPO;
import com.sap.sailing.selenium.test.adminconsole.smartphonetracking.RegisterCompetitorsDialogPO;

public class SmartphoneTrackingEventManagementPanelPO extends PageArea {
    
    private static final String ACTION_COMPETITOR_REGISTRATIONS = "ACTION_COMPETITOR_REGISTRATIONS";
    private static final String ACTION_MAP_DEVICES = "ACTION_MAP_DEVICES";
    private static final String ACTION_DENOTE_FOR_RACELOG_TRACKING = "ACTION_DENOTE_FOR_RACELOG_TRACKING";
    private static final String ACTION_START_TRACKING = "ACTION_START_TRACKING";
    private static final String ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING";
    private static final String ACTION_REMOVE_DENOTATION = "ACTION_REMOVE_DENOTATION";

    @FindBy(how = BySeleniumId.class, using = "AvailableLeaderboardsTable")
    private WebElement leaderboardTable;
    
    @FindBy(how = BySeleniumId.class, using = "RaceColumnTable")
    private WebElement raceColumnTableWrapper;
    
    @FindBy(how = BySeleniumId.class, using = "TrackedRacesListComposite")
    private WebElement trackedRacesListComposite;
    
    @FindBy(how = BySeleniumId.class, using = "LeaderboardRefreshButton")
    private WebElement leaderboardRefreshButton;
    
    @FindBy(how = BySeleniumId.class, using = "StartTrackingButton")
    private WebElement startTrackingButton;
    
    public SmartphoneTrackingEventManagementPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public CellTablePO<DataEntryPO> getLeaderboardTable() {
        return new GenericCellTablePO<>(this.driver, this.leaderboardTable, DataEntryPO.class);
    }
    
    public RaceColumnTableWrapperPO getRaceColumnTableWrapper() {
        return new RaceColumnTableWrapperPO(this.driver, this.raceColumnTableWrapper);
    }
    
    public TrackedRacesListPO getTrackedRaceListComposite() {
        return new TrackedRacesListPO(this.driver, this.trackedRacesListComposite);
    }
    
    public void refreshLeaderboardTable() {
        leaderboardRefreshButton.click();
        waitForAjaxRequests();
    }
    
    public RegisterCompetitorsDialogPO pushCompetitorRegistrationsActionButton(DataEntryPO aLeaderboard) {
        aLeaderboard.clickActionImage(ACTION_COMPETITOR_REGISTRATIONS);
        return this.waitForPO(RegisterCompetitorsDialogPO::new, "registerCompetitorsDialog");
    }
    
    public MapDevicesDialogPO pushMapDevicesActionButton(DataEntryPO aLeaderboard) {
        aLeaderboard.clickActionImage(ACTION_MAP_DEVICES);
        return this.waitForPO(MapDevicesDialogPO::new, "regattaLogTrackingDeviceMappingsDialog");
    }

    public void denoteLeaderboardForRaceLogTracking(DataEntryPO aLeaderboard) {
        aLeaderboard.clickActionImage(ACTION_DENOTE_FOR_RACELOG_TRACKING);
        findElementBySeleniumId(this.driver, "OkButton").click();
        waitForAjaxRequests();
    }

    public void startTrackingForRace(DataEntryPO aRaceRow) {
        // Notifications from previous actions may cover the button on CI
        dismissAllExistingNotifications();
        aRaceRow.clickActionImage(ACTION_START_TRACKING);
        waitForAjaxRequests();
    }

    public void stopTrackingForRace(DataEntryPO aRaceRow) {
        // Notifications from previous actions may cover the button on CI
        dismissAllExistingNotifications();
        aRaceRow.clickActionImage(ACTION_STOP_TRACKING);
        waitForAjaxRequests();
    }

    public void clickStartTrackingButton() {
        // Notifications from previous actions may cover the button on CI
        dismissAllExistingNotifications();
        this.startTrackingButton.click();
        waitForAjaxRequests();
    }

    public void removeDenotationForRace(DataEntryPO aRaceRow) {
        // Notifications from previous actions may cover the button on CI
        dismissAllExistingNotifications();
        aRaceRow.clickActionImage(ACTION_REMOVE_DENOTATION);
        waitForAjaxRequests();
    }

    // Waits for a leaderboard row with the given name to be stably present and returns it.
    // Retries on stale/transient DOM state caused by GWT re-renders after Ajax.
    public DataEntryPO waitForLeaderboardEntry(final String leaderboardName) {
        return waitForTableEntry(getLeaderboardTable(), "Name", leaderboardName);
    }

    // Waits for a race row with the given name to be stably present and returns it.
    // Retries on stale/transient DOM state caused by GWT re-renders after Ajax.
    public DataEntryPO waitForRaceRow(final String raceName) {
        return waitForTableEntry(getRaceColumnTableWrapper().getRaceColumnTable(), "Race", raceName);
    }

    // Selects exactly the named rows in the race table, deselecting all others.
    // Retries one row at a time to handle GWT re-renders between clicks.
    public void selectRaceRowsByName(String... raceNames) {
        final List<String> namesToSelect = Arrays.asList(raceNames);
        boolean changed = true;
        while (changed) {
            changed = false;
            try {
                final CellTablePO<DataEntryPO> raceTable = getRaceColumnTableWrapper().getRaceColumnTable();
                for (final DataEntryPO entry : raceTable.getEntries()) {
                    final boolean shouldBeSelected = namesToSelect.contains(entry.getColumnContent("Race"));
                    final boolean isSelected = entry.isSelected();
                    if (shouldBeSelected != isSelected) {
                        final WebElement checkboxDiv = entry.getWebElement().findElement(By.xpath("./td[1]/div"));
                        scrollToView(checkboxDiv);
                        new Actions(this.driver).moveToElement(checkboxDiv).click().build().perform();
                        changed = true;
                        break;
                    }
                }
            } catch (StaleElementReferenceException e) {
                changed = true;
            }
        }
    }

    private DataEntryPO waitForTableEntry(final CellTablePO<DataEntryPO> table, final String column, final String value) {
        final List<DataEntryPO> result = new ArrayList<>();
        waitUntil(() -> {
            boolean found = false;
            try {
                for (final DataEntryPO entry : table.getEntries()) {
                    if (value.equals(entry.getColumnContent(column))) {
                        result.clear();
                        result.add(entry);
                        found = true;
                        break;
                    }
                }
            } catch (StaleElementReferenceException e) {
                found = false;
            } catch (RuntimeException e) {
                // createDataEntry throws RuntimeException when the table is mid-re-render
                // (transient row visible that cannot be reflected into a DataEntryPO); retry
                found = false;
            }
            return found;
        });
        return result.get(0);
    }
}
