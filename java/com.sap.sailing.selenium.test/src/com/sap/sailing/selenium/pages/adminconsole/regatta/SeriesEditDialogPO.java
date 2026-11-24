package com.sap.sailing.selenium.pages.adminconsole.regatta;

import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.common.DataEntryDialogPO;

public class SeriesEditDialogPO extends DataEntryDialogPO {

    @FindBy(how = BySeleniumId.class, using = "MedalSeriesCheckbox")
    private WebElement medalSeriesCheckbox;

    @FindBy(how = BySeleniumId.class, using = "StartsWithZeroScoreCheckbox")
    private WebElement startsWithZeroScoreCheckbox;

    @FindBy(how = BySeleniumId.class, using = "StartsWithNonDiscardableCarryForwardCheckbox")
    private WebElement startsWithNonDiscardableCarryForwardCheckbox;

    @FindBy(how = BySeleniumId.class, using = "DefinesResultDiscardingRulesCheckbox")
    private WebElement definesResultDiscardingRulesCheckbox;

    @FindBy(how = BySeleniumId.class, using = "AddRacesFromListBox")
    private WebElement addRacesFromListBox;

    @FindBy(how = BySeleniumId.class, using = "AddRacesToListBox")
    private WebElement addRacesToListBox;

    @FindBy(how = BySeleniumId.class, using = "RaceNamePrefixTextBox")
    private WebElement raceNamePrefixTextField;

    @FindBy(how = BySeleniumId.class, using = "AddRacesButton")
    private WebElement addRacesButton;

    @FindBy(how = BySeleniumId.class, using = "HasSplitFleetContiguousScoringCheckbox")
    private WebElement hasSplitFleetContiguousScoringCheckbox;

    @FindBy(how = BySeleniumId.class, using = "HasCrossFleetMergedRankingCheckbox")
    private WebElement hasCrossFleetMergedRankingCheckbox;

    public SeriesEditDialogPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    public int getFromNumberOfRacesToAdd() {
        Select select = new Select(this.addRacesFromListBox);
        WebElement option = select.getFirstSelectedOption();

        return Integer.parseInt(option.getAttribute("value"));
    }

    public void setFromNumberOfRacesToAdd(int races) {
        Select select = new Select(this.addRacesFromListBox);
        select.selectByValue(Integer.toString(races));
    }

    public int getToNumberOfRacesToAdd() {
        Select select = new Select(this.addRacesToListBox);
        WebElement option = select.getFirstSelectedOption();

        return Integer.parseInt(option.getAttribute("value"));
    }

    public void setToNumberOfRacesToAdd(int races) {
        Select select = new Select(this.addRacesToListBox);
        select.selectByValue(Integer.toString(races));
    }

    public String getRaceNamePrefix() {
        return this.raceNamePrefixTextField.getText();
    }

    public void setRaceNamePrefix(String prefix) {
        this.raceNamePrefixTextField.clear();
        this.raceNamePrefixTextField.sendKeys(prefix);
    }

    public void pressAddRaces() {
        this.addRacesButton.click();
    }

    public void addRaces(int from, int to) {
        setFromNumberOfRacesToAdd(from);
        setToNumberOfRacesToAdd(to);
        pressAddRaces();
    }

    public void addRaces(int from, int to, String prefix) {
        setFromNumberOfRacesToAdd(from);
        setToNumberOfRacesToAdd(to);
        setRaceNamePrefix(prefix);
        pressAddRaces();
    }

    public void addSingleRace(String raceName) {
        setFromNumberOfRacesToAdd(1);
        setToNumberOfRacesToAdd(1);
        pressAddRaces();
        final WebElement raceNameElement = findElementBySeleniumId("ValueTextBox");
        raceNameElement.clear();
        raceNameElement.sendKeys(raceName);
    }

    public void renameRace(String race, String newName) {
        for(WebElement editorRow : getRaceNameEditors()) {
            WebElement valueTextBox = findElementBySeleniumId(editorRow, "ValueTextBox");
            String raceName = valueTextBox.getAttribute("value");

            if(raceName.equals(race)) {
                valueTextBox.clear();
                valueTextBox.sendKeys(newName);
            }
        }
    }

    public void deleteRace(String race) {
        deleteRaces(Arrays.asList(race));
    }

    public void deleteRaces(List<String> races) {
        for(WebElement editorRow : getRaceNameEditors()) {
            WebElement valueTextBox = findElementBySeleniumId(editorRow, "ValueTextBox");

            if (races.contains(valueTextBox.getAttribute("value"))) {
                WebElement removeButton = findElementBySeleniumId(editorRow, "RemoveButton");
                // A simple "removeButton.click();" does not work on GeckoDriver here.
                // This only presses the button down but a click isn't detected.
                // It seems that the mouseup event isn't correctly fired, why we use this workaround.
                new Actions(driver).sendKeys(removeButton, "\13").perform();
            }
        }
    }

    private List<WebElement> getRaceNameEditors() {
        WebElement raceNamesEditor = findElementBySeleniumId("RaceNamesStringListEditorComposite");
        WebElement raceNamesGrid = findElementBySeleniumId(raceNamesEditor, "ExpandedValuesGrid");

        return raceNamesGrid.findElements(By.tagName("tr"));
    }

    public void setMedalSeries(final boolean medal) {
        setCheckboxSelected(() -> medalSeriesCheckbox, medal);
    }

    public void setCrossFleetMergedRankingCheckboxStatus(final boolean status) {
        setCheckboxSelected(() -> hasCrossFleetMergedRankingCheckbox, status);
    }

    public void setSplitFleetContiguousScoringCheckbox(final boolean status) {
        setCheckboxSelected(() -> hasSplitFleetContiguousScoringCheckbox, status);
    }
}
