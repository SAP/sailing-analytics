package com.sap.sailing.selenium.pages.adminconsole.tractrac;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesListPO;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.pages.gwt.GenericCellTablePO;

/**
 * <p>The page object representing the TracTrac Events tab.</p>
 * 
 * @author
 *   D049941
 */
public class TracTracEventManagementPanelPO extends PageArea {
    public static class TrackableRaceDescriptor {
        public final String eventName;
        public final String raceName;
        public final String boatClass;
        //public Object startTime;
        //public Object raceStatus;
        
        public TrackableRaceDescriptor(String eventName, String raceName, String boatClass) {
            this.eventName = eventName;
            this.raceName = raceName;
            this.boatClass = boatClass;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(this.boatClass, this.eventName, this.raceName);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null) {
                return false;
            }
            if (getClass() != object.getClass()) {
                return false;
            }
            TrackableRaceDescriptor other = (TrackableRaceDescriptor) object;
            if (!Objects.equals(BoatClassMasterdata.resolveBoatClass(this.boatClass), BoatClassMasterdata.resolveBoatClass(other.boatClass))) {
                return false;
            }
            if (!Objects.equals(this.eventName, other.eventName)) {
                return false;
            }
            if (!Objects.equals(this.raceName, other.raceName)) {
                return false;
            }
            return true;
        }
    }
    
    // TODO [D049941]: Prefix the Ids with the component (e.g. "Button")
    @FindBy(how = BySeleniumId.class, using = "LiveURITextBox")
    private WebElement liveURITextBox;
    
    @FindBy(how = BySeleniumId.class, using = "StoredURITextBox")
    private WebElement storedURITextBox;
    
    @FindBy(how = BySeleniumId.class, using = "JsonURLTextBox")
    private WebElement jsonURLTextBox;
    
    @FindBy(how = BySeleniumId.class, using = "ListRacesButton")
    private WebElement listRacesButton;
    
    @FindBy(how = BySeleniumId.class, using = "AvailableRegattasListBox")
    private WebElement availableRegattasListBox;
    
    @FindBy(how = BySeleniumId.class, using = "TrackWindCheckBox")
    private WebElement trackWindCheckBox;

    @FindBy(how = BySeleniumId.class, using = "CorrectWindCheckBox")
    private WebElement correctWindCheckBox;
    
    @FindBy(how = BySeleniumId.class, using = "SimulateWithStartTimeNowCheckBox")
    private WebElement simulateWithStartTimeNowCheckBox;
    
    @FindBy(how = BySeleniumId.class, using = "TrackableRacesFilterTextBox")
    private WebElement trackableRacesFilterTextBox;
    
    @FindBy(how = BySeleniumId.class, using = "TrackableRacesCellTable")
    private WebElement trackableRacesCellTable;
    
    @FindBy(how = BySeleniumId.class, using = "StartTrackingButton")
    private WebElement startTrackingButton;
    
    @FindBy(how = BySeleniumId.class, using = "TrackedRacesListComposite")
    private WebElement trackedRacesListComposite;
    
    /**
     * <p></p>
     * 
     * @param driver
     *   
     * @param element
     *   
     */
    public TracTracEventManagementPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    
    /**
     * <p>Lists all available trackable races for the given URL. The list of the races can be obtained via
     *   {@link #getTrackableRaces()}.</p>
     * 
     * @param url
     *   The URL for which the races are to list.
     */
    public void listTrackableRaces(String url) {
        listTrackableRaces("", "", url);  //$NON-NLS-1$//$NON-NLS-2$
    }
    
    /**
     * <p>Lists all available trackable races for the given URL. The list of the races can be obtained via
     *   {@link #getTrackableRaces()}.</p>
     * 
     * @param url
     *   The URL for which the races are to list.
     */
    public void listTrackableRaces(String liveURI, String storedURI, String jsonURL) {
        this.liveURITextBox.clear();
        this.liveURITextBox.sendKeys(liveURI);
        
        this.storedURITextBox.clear();
        this.storedURITextBox.sendKeys(storedURI);
        
        this.jsonURLTextBox.clear();
        this.jsonURLTextBox.sendKeys(jsonURL);
        
        this.listRacesButton.click();
        
        waitForAjaxRequests();
    }
    
    /**
     * <p>Returns the list of all available trackable races. This list will be empty if no race is available or if no
     *   race was specified before.</p>
     * 
     * @return
     *   The list of all available trackable races.
     */
    public List<TrackableRaceDescriptor> getTrackableRaces() {
        List<TrackableRaceDescriptor> descriptors = new LinkedList<>();
        CellTablePO<DataEntryPO> table = getTrackableRacesTable();
        
        for(DataEntryPO entry : table.getEntries()) {
            String event = entry.getColumnContent("Event");
            String race = entry.getColumnContent("Race");
            String boatClass = entry.getColumnContent("Boat Class");
            
            descriptors.add(new TrackableRaceDescriptor(event, race, boatClass));
        }
        
        return descriptors;
    }
    
    public List<RegattaDescriptor> getAvailableReggatasForTracking() {
        List<RegattaDescriptor> result = new ArrayList<>();
        
        Select select = new Select(this.availableRegattasListBox);
        
        for(WebElement option : select.getOptions()) {
            RegattaDescriptor regatta = RegattaDescriptor.fromString(option.getAttribute("value"));
            
            result.add(regatta);
        }
        
        return result;
    }
    
    public RegattaDescriptor getReggataForTracking() {
        Select select = new Select(this.availableRegattasListBox);
        WebElement option = select.getFirstSelectedOption();
        RegattaDescriptor regatta = RegattaDescriptor.fromString(option.getAttribute("value"));
        
        return regatta;
    }
    
    public void setReggataForTracking(String regatta) {
        Select select = new Select(this.availableRegattasListBox);
        select.selectByValue(regatta);
    }
    
    public void setReggataForTracking(RegattaDescriptor regatta) {
        setReggataForTracking(regatta.toString());
    }
    
    /**
     * <p>Sets the filter for the trackable races. After the filter is set you can obtain the new resulting list via
     *   {@link #getTrackableRaces}</p>
     * 
     * @param filter
     *   The filter to apply to the trackable races.
     */
    public void setFilterForTrackableRaces(String filter) {
        this.trackableRacesFilterTextBox.clear();
        this.trackableRacesFilterTextBox.sendKeys(filter);
    }
    
    public void setTrackSettings(boolean trackWind, boolean correctWind, boolean simulateWithNow) {
        setSelection(this.trackWindCheckBox, trackWind);
        setSelection(this.correctWindCheckBox, correctWind);
        setSelection(this.simulateWithStartTimeNowCheckBox, simulateWithNow);
    }
    
    public void startTrackingForRace(TrackableRaceDescriptor race) {
        //startTrackingForRaces(Arrays.asList(race));
        CellTablePO<DataEntryPO> table = getTrackableRacesTable();
        DataEntryPO entryToSelect = null;
        for (DataEntryPO entry : table.getEntries()) {
            TrackableRaceDescriptor entryDiscribtor = new TrackableRaceDescriptor(entry.getColumnContent("Event"),
                    entry.getColumnContent("Race"), entry.getColumnContent("Boat Class"));
            if (race.equals(entryDiscribtor)) {
                entryToSelect = entry;
                break;
            }
        }
        table.selectEntry(entryToSelect);
        startTrackingAndWaitForAjaxRequests();
    }
    
    public void startTrackingForRaces(List<TrackableRaceDescriptor> races) {
        CellTablePO<DataEntryPO> table = getTrackableRacesTable();
        table.selectEntries(() -> table.getEntries().stream().filter(e -> races.contains(
                new TrackableRaceDescriptor(e.getColumnContent("Event"), e.getColumnContent("Race"), e.getColumnContent("Boat Class")))));
        startTrackingAndWaitForAjaxRequests();
        }
    }
    
    private void startTrackingAndWaitForAjaxRequests() {
        this.startTrackingButton.click();
        ExpectedCondition<Alert> condition = ExpectedConditions.alertIsPresent();
        if (condition.apply(this.driver) == null) {
            waitForAjaxRequests();
        }
    }
    
    public TrackedRacesListPO getTrackedRacesList() {
        return new TrackedRacesListPO(this.driver, this.trackedRacesListComposite);
    }
    
    private CellTablePO<DataEntryPO> getTrackableRacesTable() {
        return new GenericCellTablePO<>(this.driver, this.trackableRacesCellTable, DataEntryPO.class);
    }
    
    private void setSelection(WebElement checkbox, boolean selected) {
        WebElement input = checkbox.findElement(By.tagName("input"));
        
        if(input.isSelected() != selected)
            input.click();
    }
}
