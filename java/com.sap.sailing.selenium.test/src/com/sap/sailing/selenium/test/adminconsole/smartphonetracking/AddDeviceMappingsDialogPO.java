package com.sap.sailing.selenium.test.adminconsole.smartphonetracking;

import java.time.Duration;
import java.util.regex.Pattern;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesBoatTablePO;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesCompetitorTablePO;
import com.sap.sailing.selenium.pages.common.DataEntryDialogPO;

public class AddDeviceMappingsDialogPO extends DataEntryDialogPO {
    
    Logger logger = LoggerFactory.getLogger(AddDeviceMappingsDialogPO.class);

    @FindBy(how = BySeleniumId.class, using = "QRIdentifierURL")
    private WebElement qrCodeUrl;
    
    public AddDeviceMappingsDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public TrackedRacesBoatTablePO getBoatsTable() {
        Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement boatsTable = wait.until(ExpectedConditions.presenceOfElementLocated(new BySeleniumId("BoatsTable")));
        return new TrackedRacesBoatTablePO(this.driver, boatsTable);
    }
    
    public TrackedRacesCompetitorTablePO getCompetitorTable() {
        Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement competitorTable = wait.until(ExpectedConditions.presenceOfElementLocated(new BySeleniumId("CompetitorsTable")));
        return new TrackedRacesCompetitorTablePO(this.driver, competitorTable);
    }
    
    public String getQrCodeUrl(String matcherPattern) {
        waitUntil(webDriver -> Pattern.compile(matcherPattern).matcher(qrCodeUrl.getText()).matches());
        return qrCodeUrl.getText();
    }    
    
}
