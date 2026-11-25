package com.sap.sailing.selenium.pages.adminconsole.tracking;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.pages.adminconsole.ActionsHelper;
import com.sap.sailing.selenium.pages.adminconsole.tracking.TrackedRacesBoatTablePO.BoatEntry;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;

public class TrackedRacesBoatTablePO extends CellTablePO<BoatEntry> {
    public static class BoatEntry extends DataEntryPO {
        private static final String NAME_COLUMN = "Name";
        private static final String SAILID_COLUMN = "Sail number";
        private static final String BOATCLASSNAME_COLUMN = "Boat Class";
        private static final String ID_COLUMN = "ID";

        protected BoatEntry(TrackedRacesBoatTablePO table, WebElement element) {
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

        public String getSailId() {
            return getColumnContent(SAILID_COLUMN);
        }

        public String getBoatClassName() {
            return getColumnContent(BOATCLASSNAME_COLUMN);
        }

        public TrackedRacesBoatEditDialogPO clickEditButton() {
            clickActionImage(ActionsHelper.UPDATE_ACTION);
            WebElement dialog = findElementBySeleniumId(this.driver, "BoatEditDialog");
            return new TrackedRacesBoatEditDialogPO(this.driver, dialog);
        }
    }

    public TrackedRacesBoatTablePO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    @Override
    protected BoatEntry createDataEntry(WebElement element) {
        return new BoatEntry(this, element);
    }
}