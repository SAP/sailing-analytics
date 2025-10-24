package com.sap.sailing.selenium.pages.adminconsole.leaderboard;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.common.DataEntryDialogPO;

public class FlexibleLeaderboardCreateDialogPO extends DataEntryDialogPO {
    @FindBy(how = BySeleniumId.class, using = "NameTextBox")
    private WebElement nameTextBox;
    
    @FindBy(how = BySeleniumId.class, using = "DisplayNameTextBox")
    private WebElement displayNameTextBox;

    public FlexibleLeaderboardCreateDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public void setName(String name) {
        this.nameTextBox.clear();
        this.nameTextBox.sendKeys(name);
    }
    
    public void setDisplayName(String name) {
        this.displayNameTextBox.clear();
        this.displayNameTextBox.sendKeys(name);
    }
}
