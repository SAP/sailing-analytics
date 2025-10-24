package com.sap.sailing.selenium.pages.home.event;

import java.util.List;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.gwt.ListBoxPO;

public class MediaUploadDialogPO extends PageArea {
    @FindBy(how = BySeleniumId.class, using = "urlInput")
    private WebElement urlTextBox;
    
    @FindBy(how = BySeleniumId.class, using = "cancelButton")
    private WebElement cancelButton;
    
    @FindBy(how = BySeleniumId.class, using = "mimeTypeListBox")
    private List<WebElement> mimeTypeListBoxes;

    public MediaUploadDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public void enterUrl(String url) {
        urlTextBox.clear();
        urlTextBox.sendKeys(url);
        urlTextBox.sendKeys(Keys.TAB);
    }
    
    public void getMimeTypeString(int index, String expectedValue) {
        waitUntil(() -> mimeTypeListBoxes.size() > index);
        waitUntil(() -> expectedValue.equals(ListBoxPO.create(driver, mimeTypeListBoxes.get(index)).getSelectedOptionValue()));
    }
}
