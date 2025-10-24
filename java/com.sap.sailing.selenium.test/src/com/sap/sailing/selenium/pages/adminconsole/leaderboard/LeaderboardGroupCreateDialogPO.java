package com.sap.sailing.selenium.pages.adminconsole.leaderboard;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.common.DataEntryDialogPO;
import com.sap.sailing.selenium.pages.gwt.CheckBoxPO;

public class LeaderboardGroupCreateDialogPO extends DataEntryDialogPO {

    @FindBy(how = BySeleniumId.class, using = "NameTextBox")
    private WebElement nameField;

    @FindBy(how = BySeleniumId.class, using = "DescriptionTextArea")
    private WebElement descriptionField;

    public LeaderboardGroupCreateDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public void setName(String name) {
        this.nameField.clear();
        this.nameField.sendKeys(name);
    }

    public void setDescription(String description) {
        this.descriptionField.clear();
        this.descriptionField.sendKeys(description);
    }
    
    public void setUseOverallLeaderboard(boolean selected) {
        WebElement element = findElementBySeleniumId("UseOverallLeaderboardCheckBox");
        CheckBoxPO checkbox = new CheckBoxPO(driver, element);
        checkbox.setSelected(selected);
    }
}
