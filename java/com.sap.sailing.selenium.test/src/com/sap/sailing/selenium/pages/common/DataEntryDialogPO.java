package com.sap.sailing.selenium.pages.common;

import java.time.Duration;

import org.openqa.selenium.Alert;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;

public abstract class DataEntryDialogPO extends PageArea {
    
    private static final String ID_MAKE_DEFAULT_BUTTON = "MakeDefaultButton";
    
    @FindBy(how = BySeleniumId.class, using = "StatusLabel")
    private WebElement statusLabel;

    @FindBy(how = BySeleniumId.class, using = "OkButton")
    private WebElement okButton;
    
    @FindBy(how = BySeleniumId.class, using = "CancelButton")
    private WebElement cancelButton;
    
    protected DataEntryDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
        
        // This ensures that we wait until the dialog is opened and not just attached to the DOM
        waitUntil(element::isDisplayed);
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
        pressOk(false, true);
    }
    
    public void pressOk(boolean acceptAlert, boolean waitForAjaxRequests) {
        // This generically triggers revalidation in dialogs to ensure that the ok button gets enabled
        ((JavascriptExecutor) driver).executeScript("!!document.activeElement ? document.activeElement.blur() : 0");
        Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        // click OK
        wait.until(ExpectedConditions.elementToBeClickable(okButton)).click();        
        if (acceptAlert) {
            final Alert alert = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_TIMEOUT_SECONDS))
                    .until(ExpectedConditions.alertIsPresent());
            alert.accept();
        }
        if (waitForAjaxRequests) {
            // Wait, since we do a callback usually
            waitForAjaxRequests();
        }
    }
    
    public void pressCancel() {
        this.cancelButton.click();
    }
    
    public void pressMakeDefault() {
        WebElement element = findElementBySeleniumId(ID_MAKE_DEFAULT_BUTTON);
        element.click();
        waitForNotificationAndDismiss();
    }
    
    public boolean isMakeDefaultButtonVisible() {
        return !driver.findElements(new BySeleniumId(ID_MAKE_DEFAULT_BUTTON)).isEmpty();
    }
    
    public void clickOkButtonOrThrow() {
        waitUntil(okButton::isEnabled);
        okButton.click();
    }
}
