package com.sap.sailing.selenium.pages.adminconsole.advanced;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;

public class IpBlocklistPanelPO extends PageArea {
    static class TablePO extends CellTablePO<IPLockEntry> {
        public TablePO(WebDriver driver, WebElement element) {
            super(driver, element);
        }

        @Override
        protected IPLockEntry createDataEntry(WebElement element) {
            return new IPLockEntry(this, element);
        }

    }

    public static class IPLockEntry extends DataEntryPO {
        private static final String IP_COLUMN = "IP Address";
        private static final String LOCKED_UNTIL_COLUMN = "Locked until";

        protected IPLockEntry(CellTablePO<IPLockEntry> table, WebElement element) {
            super(table, element);
        }

        @Override
        public String getIdentifier() {
            return getIp();
        }

        public String getIp() {
            return getColumnContent(IP_COLUMN);
        }

        public String getLockedUntil() {
            return getColumnContent(LOCKED_UNTIL_COLUMN);
        }
    }

    public IpBlocklistPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
        final WebElement cellTableWebElement = driver.findElement(new BySeleniumId("cellTable"));
        this.cellTable = new TablePO(driver, cellTableWebElement);
    }

    @FindBy(how = BySeleniumId.class, using = "refreshButton")
    private WebElement refreshButton;

    @FindBy(how = BySeleniumId.class, using = "unlockButton")
    private WebElement unlockButton;

    private final TablePO cellTable;

    public void refresh() {
        refreshButton.click();
    }

    /**
     * @return true if IP was found, false if not found
     */
    public boolean expectIpInTable(final String ip) {
        final IPLockEntry entry = cellTable.getEntry(ip);
        final boolean wasFound = entry != null;
        return wasFound;
    }

    public void unblockIP(String ip) {
        cellTable.getEntry(ip).select();
        unlockButton.click();
    }
}
