package com.sap.sailing.selenium.pages.common;

import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;

public abstract class DataEntryDialogPO extends PageArea {
    @FindBy(how = BySeleniumId.class, using = "StatusLabel")
    private WebElement statusLabel;

    @FindBy(how = BySeleniumId.class, using = "OkButton")
    private WebElement okButton;
    
    @FindBy(how = BySeleniumId.class, using = "CancelButton")
    private WebElement cancelButton;
    

    public DataEntryDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    @Override
    protected void verify() {
        WebElement element = (WebElement) this.context;
        String cssClasses = element.getAttribute("class");
        
        if(!cssClasses.contains("gwt-DialogBox"))
            throw new IllegalStateException("This is not a dialog");
    }
    
    public String getStatusMessage() {
        if(this.statusLabel.isDisplayed())
            return this.statusLabel.getText();
        
        return null;
    }
    
    public boolean isOkButtonEnabled() {
        return this.okButton.isEnabled();
    }
    
    public boolean isCancelButtonEnabled() {
        return this.cancelButton.isEnabled();
    }
    
    public void pressOk() {
        pressOk(false);
    }
    
    public void pressOk(boolean accept) {
        this.okButton.click();
        
        ExpectedCondition<Alert> condition = ExpectedConditions.alertIsPresent();
        Alert alert = condition.apply(this.driver);
        
        if(alert != null && accept) {
            alert.accept();
        }
        
        if(alert == null || accept) {
            // Wait, since we do a callback usually
            waitForAjaxRequests();
        }
    }
    
    public void pressCancel() {
        this.cancelButton.click();
    }
}
