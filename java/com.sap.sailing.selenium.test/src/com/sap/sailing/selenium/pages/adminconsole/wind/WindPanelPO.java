package com.sap.sailing.selenium.pages.adminconsole.wind;

import java.util.function.Function;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;

public class WindPanelPO extends PageArea {

    @FindBy(how = BySeleniumId.class, using = "ImportWindFromIgtimi")
    private WebElement importWindFromIgtimi;
    
    @FindBy(how = BySeleniumId.class, using = "IgtimiImportResultReport")
    private WebElement igtimiImportResultReport;
    
    @FindBy(how = BySeleniumId.class, using = "ImportWindFromRouteconverterSubmit")
    private WebElement importWindFromRouteconverterSubmitButton;
    
    @FindBy(how = BySeleniumId.class, using = "ImportWindFromRouteconverterUpload")
    private WebElement importWindFromRouteconverterFileUpload;
    
    @FindBy(how = BySeleniumId.class, using = "ImportWindFromRouteconverterResults")
    private WebElement importWindFromRouteconverterResultPanel;
    
    
    public WindPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public void importWindFromIgtimi(int timeoutInSeconds) throws InterruptedException {
        importWindFromIgtimi.click();
        waitForAlertAndAccept(10);
        FluentWait<String> wait = createFluentWait("The following number of fixes were imported for the listed races:", timeoutInSeconds, DEFAULT_POLLING_INTERVAL);
        wait.until(new Function<String, Object>() {
            @Override
            public Object apply(String message) {
                try {
                    String statusText = igtimiImportResultReport.getText();
                    return statusText.contains(message);
                } catch (TimeoutException exception) {
                    return Boolean.FALSE;
                }
            }
        });
    }
    
    public void importWindFromRouteconverter(String filename, int timeoutInSeconds) throws InterruptedException {
        importWindFromRouteconverterFileUpload.sendKeys(filename);
        importWindFromRouteconverterSubmitButton.click();
        waitForAlertAndAccept(10);
        FluentWait<String> wait = createFluentWait("Imported wind from: ", timeoutInSeconds, DEFAULT_POLLING_INTERVAL);
        wait.until(new Function<String, Object>() {
            @Override
            public Object apply(String message) {
                try {
                    String statusText = importWindFromRouteconverterResultPanel.getText();
                    return statusText.contains(message);
                } catch (TimeoutException exception) {
                    return Boolean.FALSE;
                }
            }
        });
    }
}
