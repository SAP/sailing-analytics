package com.sap.sailing.selenium.test.adminconsole.smartphonetracking;

import java.time.Duration;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesCompetitorTablePO;
import com.sap.sailing.selenium.pages.common.DataEntryDialogPO;

public class RegisterCompetitorsDialogPO extends DataEntryDialogPO {

    @FindBy(how = BySeleniumId.class, using = "registerCompetitorsDialog")
    private WebElement dialog;

    @FindBy(how = BySeleniumId.class, using = "addCompetitorWithBoatButton")
    private WebElement addCompetitorWithBoatButton;

    public RegisterCompetitorsDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);

    }

    public AddCompetitorWithBoatDialogPO openAddCompetitorWithBoatDialog() {
        this.addCompetitorWithBoatButton.click();
        return getPO(AddCompetitorWithBoatDialogPO::new, "CompetitorWithBoatEditDialog");
    }

    public TrackedRacesCompetitorTablePO getCompetitorTable() {
        Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement competitorTable = wait
                .until(ExpectedConditions.presenceOfElementLocated(new BySeleniumId("CompetitorsTable")));
        return new TrackedRacesCompetitorTablePO(this.driver, competitorTable);
    }

}
