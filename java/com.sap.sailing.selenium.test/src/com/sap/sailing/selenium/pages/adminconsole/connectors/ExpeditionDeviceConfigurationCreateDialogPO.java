package com.sap.sailing.selenium.pages.adminconsole.connectors;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.common.DataEntryDialogPO;

public class ExpeditionDeviceConfigurationCreateDialogPO extends DataEntryDialogPO {
    @FindBy(how = BySeleniumId.class, using = "expeditionBoatName")
    private WebElement expeditionBoatNameTextBox;
    
    @FindBy(how = BySeleniumId.class, using = "expeditionBoatId")
    private WebElement expeditionBoatIdTextBox;

    public ExpeditionDeviceConfigurationCreateDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public void setName(String name) {
        this.expeditionBoatNameTextBox.clear();
        this.expeditionBoatNameTextBox.sendKeys(name);
    }
    
    public void setExpeditionBoatId(String id) {
        this.expeditionBoatIdTextBox.clear();
        this.expeditionBoatIdTextBox.sendKeys(id);
    }
}
