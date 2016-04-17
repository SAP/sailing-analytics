package com.sap.sailing.selenium.pages.adminconsole.regatta;

import java.util.Date;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.common.DataEntryDialogPO;
import com.sap.sailing.selenium.pages.gwt.BetterDateTimeBoxPO;
import com.sap.sailing.selenium.pages.gwt.TextBoxPO;

public class RegattaCreateDialogPO extends DataEntryDialogPO {
    @FindBy(how = BySeleniumId.class, using = "NameTextBox")
    private WebElement nameTextBox;
    
    @FindBy(how = BySeleniumId.class, using = "BoatClassTextBox")
    private WebElement boatClassTextBox;
    
    @FindBy(how = BySeleniumId.class, using = "StartDateTimeBox")
    private WebElement startDateTimeBox;
    
    @FindBy(how = BySeleniumId.class, using = "EndDateTimeBox")
    private WebElement endDateTimeBox;
    
//    @FindBy(how = BySeleniumId.class, using = "ScoringSchemeListBox")
//    private WebElement scoringSystemDropDown;
//    @FindBy(how = BySeleniumId.class, using = "EventListBox")
//    private WebElement eventDropDown;
//    @FindBy(how = BySeleniumId.class, using = "CourseAreaListBox")
//    private WebElement courseAreaDropDown;
    
    @FindBy(how = BySeleniumId.class, using = "AddSeriesButton")
    private WebElement addSeriesButton;
    
    public RegattaCreateDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public void setRegattaName(String name) {
        TextBoxPO.create(driver, nameTextBox).setText(name);
    }
    
    public void setBoatClass(String boatClass) {
        this.boatClassTextBox.clear();
        this.boatClassTextBox.sendKeys(boatClass);
    }
    
    public void setValues(String name, String boatClass, Date startDate, Date endDate) {
        this.setRegattaName(name);
        this.setBoatClass(boatClass);
        BetterDateTimeBoxPO.create(driver, startDateTimeBox).setDate(startDate);
        BetterDateTimeBoxPO.create(driver, endDateTimeBox).setDate(endDate);
    }
    
    // TODO: Scoring System, Event and Course Area
    
    public SeriesCreateDialogPO addSeries() {
        this.addSeriesButton.click();
        
        WebElement dialog = findElementBySeleniumId(this.driver, "SeriesCreateDialog");
        
        return new SeriesCreateDialogPO(this.driver, dialog);
    }
}
