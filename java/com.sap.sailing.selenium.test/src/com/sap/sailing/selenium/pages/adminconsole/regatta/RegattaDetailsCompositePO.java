package com.sap.sailing.selenium.pages.adminconsole.regatta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.adminconsole.ActionsHelper;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.pages.gwt.GenericCellTablePO;

// TODO: Implement if needed
public class RegattaDetailsCompositePO extends PageArea {
    @FindBy(how = BySeleniumId.class, using = "NameLabel")
    private WebElement regattaNameLabel;
    
    @FindBy(how = BySeleniumId.class, using = "BoatClassLabel")
    private WebElement boatClassLabel;
    
    @FindBy(how = BySeleniumId.class, using = "CourseAreaLabel")
    private WebElement courseAreaLabel;
    
    @FindBy(how = BySeleniumId.class, using = "ScoringSystemLabel")
    private WebElement scoringSystemLabel;
    
    @FindBy(how = BySeleniumId.class, using = "SeriesCellTable")
    private WebElement seriesTable;
    
    public RegattaDetailsCompositePO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public String getRegattaName() {
        return this.regattaNameLabel.getText();
    }
    
    public String getBoatClass() {
        return this.boatClassLabel.getText();
    }
    
    public String getCouresArea() {
        return this.courseAreaLabel.getText();
    }
    
    public String getScoringSystem() {
        return this.scoringSystemLabel.getText();
    }
    
    public SeriesEditDialogPO editSeries(String series) {
        DataEntryPO entry = findSeries(series);
        if (entry != null) {
            WebElement action = ActionsHelper.findEditAction(entry.getWebElement());
            action.click();
            return waitForPO(SeriesEditDialogPO::new, "SeriesEditDialog", 5);
        }
        return null;
    }

    public void deleteSeries(String series) {
        DataEntryPO entry = findSeries(series);
        if (entry != null) {
            WebElement removeAction = ActionsHelper.findRemoveAction(entry.getWebElement());
            removeAction.click();
             ActionsHelper.acceptAlert(this.driver);
             waitForAjaxRequests();
        }
    }
    
    private DataEntryPO findSeries(String series) {
        CellTablePO<DataEntryPO> table = getSeriesTable();
        for (DataEntryPO entry : table.getEntries()) {
            String name = entry.getColumnContent("Series");
            if (series.equals(name)) {
                return entry;
            }
        }
        return null;
    }
    
    private CellTablePO<DataEntryPO> getSeriesTable() {
        return new GenericCellTablePO<>(this.driver, this.seriesTable, DataEntryPO.class);
    }
    
    public List<String> getRaceNames(String seriesName) {
        final DataEntryPO seriesEntry = findSeries(seriesName);
        final String racesColumnContent = seriesEntry.getColumnContent("Races");
        if (racesColumnContent != null && ! racesColumnContent.isEmpty()) {
            return Arrays.asList(racesColumnContent.split(", "));
        }
        return Collections.emptyList();
    }
    
    public void waitForRacesOfSeries(final String series, final List<String> races) {
        waitUntil(() -> getRaceNames(series).equals(races));
    }
}
