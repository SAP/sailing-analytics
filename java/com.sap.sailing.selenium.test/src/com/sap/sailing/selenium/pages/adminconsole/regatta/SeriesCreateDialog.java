package com.sap.sailing.selenium.pages.adminconsole.regatta;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;

import com.sap.sailing.selenium.pages.common.DataEntryDialog;

public class SeriesCreateDialog extends DataEntryDialog {
    @FindBy(how = BySeleniumId.class, using = "NameTextField")
    private WebElement seriesNameTextField;
    
//    @FindBy(how = BySeleniumId.class, using = "MedalSeriesCheckbox")
//    private WebElement medalSeriesCheckbox;
    
//    @FindBy(how = BySeleniumId.class, using = "StartsWithZeroScoreCheckbox")
//    private WebElement startsWithZeroScoreCheckbox;
    
//    @FindBy(how = BySeleniumId.class, using = "StartsWithNonDiscardableCarryForwardCheckbox")
//    private WebElement startsWithNonDiscardableCarryForwardCheckbox;
    
//    @FindBy(how = BySeleniumId.class, using = "DefinesResultDiscardingRulesCheckbox")
//    private WebElement definesResultDiscardingRulesCheckbox;
    
//    @FindBy(how = BySeleniumId.class, using = "FleetListEditorComposite")
//    private WebElement fleetsPanel;
    
    public SeriesCreateDialog(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public void setSeriesName(String name) {
        this.seriesNameTextField.clear();
        this.seriesNameTextField.sendKeys(name);
    }
    
    // TODO: Checkboxes and Fleets
}
