package com.sap.sailing.selenium.pages.adminconsole.common;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;

public class ErrorDialogPO extends PageArea {

    @FindBy(how = BySeleniumId.class, using = "ErrorDialogTitle")
    private WebElement title;
    
    @FindBy(how = BySeleniumId.class, using = "ErrorDialogServerResponse")
    private WebElement serverResponse;
    
    @FindBy(how = BySeleniumId.class, using = "ErrorDialogCloseButton")
    private WebElement close;

    public ErrorDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public String getTitle() {
        return title.getText();
    }
    
    public String getServerResponse() {
        return serverResponse.getText();
    }

    public void assertTitleContainsTextAndClose(String titlePart) {
        final String titleText = getTitle();
        if (!titleText.contains(titlePart)) {
            throw new RuntimeException("The expected title '" + titlePart + "' does not match the actual title '"
                    + titleText + "' in the error box.");
        }
        close();
    }
    
    public void assertServerResponseContainsTextAndClose(String serverResponsePart) {
        final String serverResponseText = getServerResponse();
        if (!serverResponseText.contains(serverResponsePart)) {
            throw new RuntimeException("The expected server response '" + serverResponsePart + "' does not match the actual server response '"
                    + serverResponseText + "' in the error box.");
        }
        close();
    }

    private void close() {
        clickWhenInteractable(() -> findElementBySeleniumId("ErrorDialogCloseButton"));
        waitForElementNotExistsBySeleniumId(driver, "ErrorDialog");
    }

}
