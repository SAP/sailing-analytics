package com.sap.sailing.selenium.pages.adminconsole.igtimi;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.common.DataEntryDialogPO;

public class AddIgtimiDeviceDialogPO extends DataEntryDialogPO {
    @FindBy(how = BySeleniumId.class, using = "igtimiAccountEmail")
    private WebElement igtimiAccountEmail;
    
    @FindBy(how = BySeleniumId.class, using = "igtimiAccountPassword")
    private WebElement igtimiAccountPassword;
    
    public AddIgtimiDeviceDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public void setEmail(String email) {
        igtimiAccountEmail.sendKeys(email);
    }
    
    public void setPassword(String password) {
        igtimiAccountPassword.sendKeys(password);
    }
}
