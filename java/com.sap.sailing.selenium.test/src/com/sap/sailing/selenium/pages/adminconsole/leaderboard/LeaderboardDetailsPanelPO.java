package com.sap.sailing.selenium.pages.adminconsole.leaderboard;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.adminconsole.ActionsHelper;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO.TrackedRaceDescriptor;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.pages.gwt.GenericCellTablePO;
import com.sap.sse.common.Util;

public class LeaderboardDetailsPanelPO extends PageArea {
    public static class RaceDescriptor {
        private final String name;
        private final String fleet;
        private final boolean medalRace;
        private final boolean linked;
        private final double factor;
                
        public RaceDescriptor(String name, String fleet, boolean medalRace, boolean linked, double factor) {
            this.name = name;
            this.fleet = fleet;
            this.medalRace = medalRace;
            this.linked = linked;
            this.factor = factor;
        }
        
        public String getName() {
            return this.name;
        }
        
        public String getFleet() {
            return this.fleet;
        }
        
        public boolean isMedalRace() {
            return this.medalRace;
        }
        
        public boolean isLinked() {
            return this.linked;
        }
        
        public double getFactor() {
            return this.factor;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.fleet);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object)
                return true;
            
            if (object == null)
                return false;
            
            if (getClass() != object.getClass())
                return false;
            
            RaceDescriptor other = (RaceDescriptor) object;
            
            if(!Objects.equals(this.name, other.name))
                return false;
            
            if(!Objects.equals(this.fleet, other.fleet))
                return false;
            
            return true;
        }
    }
    
    @FindBy(how = BySeleniumId.class, using = "RacesCellTable")
    private WebElement racesCellTable;
    
    // TODO: Only available for flexible leader boards
    @FindBy(how = BySeleniumId.class, using = "AddRacesButton")
    private WebElement addRacesButton;
    
    @FindBy(how = BySeleniumId.class, using = "TrackedRacesListComposite")
    private WebElement trackedRacesListComposite;
    
    @FindBy(how = BySeleniumId.class, using = "TrackedRacesFilterTextBox")
    private WebElement trackedRacesFilterTextBox;
    
    @FindBy(how = BySeleniumId.class, using = "RefreshButton")
    private WebElement refreshButton;

    public LeaderboardDetailsPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
//    /**
//     * Assuming that the leaderboard is already selected, clicks the "Add Races..." button 
//     * @param i
//     */
//    // TODO: Must be a flexible leaderboard
    public void addRacesToFlexibleLeaderboard(int i) {
        addRacesToFlexibleLeaderboard(i, "R");
    }
    
    public void addRacesToFlexibleLeaderboard(int i, String s) {
        this.addRacesButton.click();
        WebElement dialog = findElementBySeleniumId(this.driver, "RaceColumnsInLeaderboardDialog");
        RaceColumnsInLeaderboardDialog raceColumnsInLeaderboardDialog = new RaceColumnsInLeaderboardDialog(this.driver, dialog);
        raceColumnsInLeaderboardDialog.addRaces(i, s);
        //selectRaceColumn("R1", "Default");
    }
//    
//    public void selectRaceColumn(String raceColumnName, String fleetName) {
//        CellTable table = getRaceColumnsTable();
//        for (WebElement row : table.getRows()) {
//            List<WebElement> fields = row.findElements(By.tagName("td"));
//            if (raceColumnName.equals(fields.get(0).getText()) && fleetName.equals(fields.get(1).getText())) {
//                fields.get(1).click();
//                break;
//            }
//        }
//    }
    
    public List<RaceDescriptor> getRaces() {
        List<RaceDescriptor> result = new ArrayList<>();
        LeaderboardRacesTablePO racesTable = getRacesTable();
        RaceDescriptorFactory raceDescriptorFactory = new RaceDescriptorFactory(racesTable);
        for(DataEntryPO entry : racesTable.getEntries()) {
            result.add(raceDescriptorFactory.createRaceDescriptor(entry));
        }
        return result;
    }
    
//    public RaceEditDialog editRace(RaceDescriptor race) {
//        
//        WebElement action = Actions.findEditAction(findRace(race));
//        
//        action.click();
//    }
    
    public void unlinkRace(RaceDescriptor race) {
        DataEntryPO entry = findRace(race);
        if (entry != null) {
            WebElement action = ActionsHelper.findUnlinkRaceAction(entry.getWebElement());
            action.click();
            waitForAjaxRequests(); // unlinking update the leaderboard details table; wait for callback to finish
        }
    }
    
    public void refreshRaceLog(RaceDescriptor race) {
        DataEntryPO entry = findRace(race);
        
        if(entry != null) {
            WebElement action = ActionsHelper.findRefreshAction(entry.getWebElement());
            
            action.click();
            
            // QUESTION [D049941]: What else can happen?
            ActionsHelper.acceptAlert(this.driver);
        }
    }
    
    public void linkRace(RaceDescriptor race, TrackedRaceDescriptor tracking) {
        WebDriverWait webDriverWait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_LOOKUP_TIMEOUT));
        DataEntryPO raceRow = webDriverWait.until(new Function<WebDriver, DataEntryPO>() {
            @Override
            public DataEntryPO apply(WebDriver t) {
                return findRace(race);
            }
        });
        DataEntryPO trackingRow;

        try {
            trackingRow = findTrackingRow(tracking, webDriverWait);
        } catch (TimeoutException e) {
            // click the refresh button to refresh the tracking grid
            refreshButton.click();
            trackingRow = findTrackingRow(tracking, webDriverWait);
        }

        raceRow.select();
        trackingRow.select();
        waitForAjaxRequests(); // the selection will update elements of the cell table; wait until the callback has been
                               // received
    }

    private DataEntryPO findTrackingRow(TrackedRaceDescriptor tracking, WebDriverWait webDriverWait)
            throws TimeoutException {
        return webDriverWait.until(new Function<WebDriver, DataEntryPO>() {
            @Override
            public DataEntryPO apply(WebDriver t) {
                return findTracking(tracking);
            }
        });
    }
    
    private DataEntryPO findRace(RaceDescriptor race) {
        LeaderboardRacesTablePO racesTable = getRacesTable();
        RaceDescriptorFactory raceDescriptorFactory = new RaceDescriptorFactory(racesTable);
        for(DataEntryPO entry : racesTable.getEntries()) {
            RaceDescriptor descriptor = raceDescriptorFactory.createRaceDescriptor(entry);
            if (descriptor.equals(race)) {
                return entry;
            }
        }
        return null;
    }
    
    private DataEntryPO findTracking(TrackedRaceDescriptor tracking) {
        CellTablePO<DataEntryPO> trackingTable = getTrackedRacesTable();
        
        final int regattaColumnIndex = trackingTable.getColumnIndex("Regatta");
        final int boatClassColumnIndex = trackingTable.getColumnIndex("Boat Class");
        final int raceColumnIndex = trackingTable.getColumnIndex("Race");
        for(DataEntryPO entry : trackingTable.getEntries()) {
            String regatta = entry.getColumnContent(regattaColumnIndex);
            String boatClass = entry.getColumnContent(boatClassColumnIndex);
            String raceName = entry.getColumnContent(raceColumnIndex);
            
            TrackedRaceDescriptor descriptor = new TrackedRaceDescriptor(regatta, boatClass, raceName);
            
            if(descriptor.equals(tracking))
                return entry;
        }
        
        return null;
    }
    
    public LeaderboardRacesTablePO getRacesTable() {
        return new LeaderboardRacesTablePO(driver, this.racesCellTable);
    }
    
    public void filter(String s) {
        trackedRacesFilterTextBox.sendKeys(s);
    }
    
    private CellTablePO<DataEntryPO> getTrackedRacesTable() {
        WebElement table = findElementBySeleniumId(this.trackedRacesListComposite, "TrackedRacesCellTable");
        
        return new GenericCellTablePO<>(this.driver, table, DataEntryPO.class);
    }
    
    /**
     * Helps to reduce redundant getColumnIndex calls by reusing the indexes values for several rows. Should only be
     * used in one operation that iterates the table's rows. Caching is not intended sue to possible column changes.
     */
    private class RaceDescriptorFactory {
        
        private final int raceColumnIndex;
        private final int fleetColumnIndex;
        private final int medalRaceColumnIndex;
        private final int linkedColumnIndex;
        private final int factorColumnIndex;


        public RaceDescriptorFactory(LeaderboardRacesTablePO racesTable) {
            raceColumnIndex = racesTable.getColumnIndex("Race");
            fleetColumnIndex = racesTable.getColumnIndex("Fleet");
            medalRaceColumnIndex = racesTable.getColumnIndex("Medal Race");
            linkedColumnIndex = racesTable.getColumnIndex("Is linked");
            factorColumnIndex = racesTable.getColumnIndex("Factor");
        }
        

        private RaceDescriptor createRaceDescriptor(DataEntryPO entry) {
            String name = entry.getColumnContent(raceColumnIndex);
            String fleet = entry.getColumnContent(fleetColumnIndex);
            String medalRace = entry.getColumnContent(medalRaceColumnIndex);
            String linked = entry.getColumnContent(linkedColumnIndex);
            String factorColumnContent = entry.getColumnContent(factorColumnIndex);
            double factor = factorColumnContent.trim().isEmpty() ? 0.0 : Double.valueOf(factorColumnContent);
            return new RaceDescriptor(name, fleet, Util.equalsWithNull("x", medalRace), Util.equalsWithNull("Yes", linked), factor);
        }
    }
}
