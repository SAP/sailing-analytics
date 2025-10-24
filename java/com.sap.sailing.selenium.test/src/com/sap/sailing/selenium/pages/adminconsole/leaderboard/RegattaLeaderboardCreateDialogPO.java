package com.sap.sailing.selenium.pages.adminconsole.leaderboard;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;
import com.sap.sailing.selenium.pages.common.DataEntryDialogPO;

public class RegattaLeaderboardCreateDialogPO extends DataEntryDialogPO {
    @FindBy(how = BySeleniumId.class, using = "NameTextBox")
    private WebElement nameTextField;

    @FindBy(how = BySeleniumId.class, using = "RegattaListBox")
    private WebElement regattaDropDown;
    
    public RegattaLeaderboardCreateDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public void selectRegatta(RegattaDescriptor regatta) {
        waitUntil(() -> regattaDropDown.isDisplayed());
        Select dropDown = new Select(this.regattaDropDown);
        dropDown.selectByValue(regatta.toString());
    }
    
}
