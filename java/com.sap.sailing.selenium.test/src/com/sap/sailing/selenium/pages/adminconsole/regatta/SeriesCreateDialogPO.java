package com.sap.sailing.selenium.pages.adminconsole.regatta;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.common.DataEntryDialogPO;

public class SeriesCreateDialogPO extends DataEntryDialogPO {
    @FindBy(how = BySeleniumId.class, using = "NameTextBox")
    private WebElement nameTextBox;

//    @FindBy(how = BySeleniumId.class, using = "MedalSeriesCheckbox")
//    private WebElement medalSeriesCheckbox;

//    @FindBy(how = BySeleniumId.class, using = "StartsWithZeroScoreCheckbox")
//    private WebElement startsWithZeroScoreCheckbox;

//    @FindBy(how = BySeleniumId.class, using = "StartsWithNonDiscardableCarryForwardCheckbox")
//    private WebElement startsWithNonDiscardableCarryForwardCheckbox;

//    @FindBy(how = BySeleniumId.class, using = "DefinesResultDiscardingRulesCheckbox")
//    private WebElement definesResultDiscardingRulesCheckbox;

//    @FindBy(how = BySeleniumId.class, using = "FleetListEditorComposite")
//    private WebElement fleetsPanel;

//    @FindBy(how = BySeleniumId.class, using = "HasSplitFleetContiguousScoringCheckbox")
//    private WebElement hasSplitFleetContiguousScoringCheckbox;

//    @FindBy(how = BySeleniumId.class, using = "HasCrossFleetMergedRankingCheckbox")
//    private WebElement hasCrossFleetMergedRankingCheckbox;

    public SeriesCreateDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public void setSeriesName(String name) {
        this.nameTextBox.clear();
        this.nameTextBox.sendKeys(name);
    }

    // TODO: Checkboxes and Fleets
}
