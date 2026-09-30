package com.sap.sailing.selenium.pages.adminconsole.security;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.gwt.SuggestBoxPO;

public class AclActionInputPO extends PageArea {
    @FindBy(how = BySeleniumId.class, using = "InputSuggestBox")
    private WebElement inputSuggestBox;
    
    @FindBy(how = BySeleniumId.class, using = "AddButton")
    private WebElement addButton;
    
    AclActionInputPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public void addAction(String name) {
        waitUntil(() -> inputSuggestBox.isDisplayed() && inputSuggestBox.isEnabled());
        SuggestBoxPO.create(driver, inputSuggestBox).appendText(name);
        addButton.click();
    }
    
    public boolean isEnabled() {
        return inputSuggestBox.isEnabled();
    }
}