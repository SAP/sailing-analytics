package com.sap.sailing.selenium.pages.adminconsole.tracking;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.adminconsole.ActionsHelper;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesCompetitorTablePO.CompetitorEntry;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;

public class TrackedRacesCompetitorTablePO extends CellTablePO<CompetitorEntry> {
    public static class CompetitorEntry extends DataEntryPO {
        @FindBy(how = BySeleniumId.class, using = "ACTION_EDIT")
        private WebElement editButton;

        private static final String NAME_COLUMN = "Name";
        private static final String SHORT_NAME_COLUMN = "Short name";
        private static final String SAILID_COLUMN = "Sail number";
        private static final String BOATCLASSNAME_COLUMN = "Boat Class";
        private static final String ID_COLUMN = "ID";

        protected CompetitorEntry(TrackedRacesCompetitorTablePO table, WebElement element) {
            super(table, element);
        }

        @Override
        public String getIdentifier() {
            return getId();
        }

        public String getId() {
            return getColumnContent(ID_COLUMN);
        }

        public String getName() {
            return getColumnContent(NAME_COLUMN);
        }

        public String getShortName() {
            return getColumnContent(SHORT_NAME_COLUMN);
        }

        public String getSailId() {
            return getColumnContent(SAILID_COLUMN);
        }

        public String getBoatClassName() {
            return getColumnContent(BOATCLASSNAME_COLUMN);
        }

        public TrackedRacesCompetitorEditDialogPO clickEditButton() {
            WebElement action = ActionsHelper.findUpdateAction(getWebElement());
            action.click();
            WebElement dialog = findElementBySeleniumId(this.driver, "CompetitorEditDialog");
            return new TrackedRacesCompetitorEditDialogPO(this.driver, dialog);
        }

        public TrackedRacesCompetitorWithBoatEditDialogPO clickEditWithBoatButton() {
            WebElement action = ActionsHelper.findUpdateAction(getWebElement());
            action.click();
            WebElement dialog = findElementBySeleniumId(this.driver, "CompetitorWithBoatEditDialog");
            return new TrackedRacesCompetitorWithBoatEditDialogPO(this.driver, dialog);
        }
}

    public TrackedRacesCompetitorTablePO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    @Override
    protected CompetitorEntry createDataEntry(WebElement element) {
        return new CompetitorEntry(this, element);
    }
}