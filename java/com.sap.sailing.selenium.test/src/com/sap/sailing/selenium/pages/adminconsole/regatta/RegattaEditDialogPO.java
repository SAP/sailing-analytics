package com.sap.sailing.selenium.pages.adminconsole.regatta;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.common.DataEntryDialogPO;
import com.sap.sailing.selenium.pages.gwt.CheckBoxPO;

public class RegattaEditDialogPO extends DataEntryDialogPO {
    @FindBy(how = BySeleniumId.class, using = "RacingProcedureConfigurationCheckBox")
    private WebElement regattaConfigurationCheckbox;
    
    @FindBy(how = BySeleniumId.class, using = "RacingProcedureConfigurationEditButton")
    private WebElement regattaConfigurationButton;

    @FindBy(how = BySeleniumId.class, using = "EventListBox")
    private WebElement eventListBox;

    @FindBy(how = BySeleniumId.class, using = "UseStartTimeInferenceCheckBox")
    private WebElement useStartTimeInferenceCheckbox;

    @FindBy(how = BySeleniumId.class, using = "CourseAreaListBox")
    private WebElement courseAreaListBox;

    public RegattaEditDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public void setUseStartTimeInference(boolean b) {
        CheckBoxPO checkbox = new CheckBoxPO(driver, useStartTimeInferenceCheckbox);
        checkbox.setSelected(b);
    }
}
