package com.sap.sailing.selenium.pages.adminconsole.advanced;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;

public class IpBlocklistPanelPO extends PageArea {
    static class IPBlocklistTablePO extends CellTablePO<IPLockEntry> {
        public IPBlocklistTablePO(WebDriver driver, WebElement element) {
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
        final WebElement cellTableWebElement = this.findElementBySeleniumId("cellTable");
        this.cellTable = new IPBlocklistTablePO(driver, cellTableWebElement);
    }

    @FindBy(how = BySeleniumId.class, using = "refreshButton")
    private WebElement refreshButton;

    @FindBy(how = BySeleniumId.class, using = "unlockButton")
    private WebElement unlockButton;

    private final IPBlocklistTablePO cellTable;

    public void refresh() {
        refreshButton.click();
        waitForAjaxRequests();
    }

    public boolean isIpInTable(final String ip) {
        final IPLockEntry entry = cellTable.getEntry(ip);
        final boolean wasFound = entry != null;
        return wasFound;
    }

    /**
     * Unblocks the given IP address and blocks until the server-side release is confirmed by the row disappearing from
     * a freshly reloaded table. The unlock GWT-RPC does not register with the pending-AJAX tracker (it uses a plain,
     * unmarked {@code AsyncCallback}), so {@link #waitForAjaxRequests()} cannot observe it and the removed row alone
     * only proves the client-side row was dropped, not that the lock was released. Refreshing (which does register)
     * and waiting for the row to be gone therefore ties completion to a server round-trip, preventing a subsequent
     * user creation from racing an as-yet-unreleased lock.
     */
    public void unblockIP(String ip) {
        cellTable.getEntry(ip).select();
        unlockButton.click();
        waitUntil(() -> {
            refresh();
            return !isIpInTable(ip);
        });
    }

    /**
     * Returns the IP addresses currently shown in the blocklist table, exactly as rendered in the "IP Address" column
     * (including any formatting such as brackets around an IPv6 literal). This avoids guessing the textual form under
     * which the server recorded a lock: the returned strings round-trip through {@link #unblockIP(String)} because they
     * are the very identifiers {@link IPLockEntry#getIdentifier()} compares against. The table is refreshed first so
     * that a lock recorded by a preceding operation is reflected before it is read.
     */
    public List<String> getBlockedIps() {
        refresh();
        final List<String> result = new ArrayList<>();
        for (final IPLockEntry entry : cellTable.getEntries()) {
            result.add(entry.getIp());
        }
        return result;
    }

    /**
     * Removes every lock currently present in the blocklist table by unblocking each shown IP address. Because a
     * successful unblock removes the corresponding row and thereby invalidates the previously resolved page objects,
     * the set of present IPs is read once up front via {@link #getBlockedIps()} and each address is then unblocked by
     * re-resolving it against the (refreshed) table. Addresses that are no longer present when their turn comes are
     * skipped, so this is safe even if an unblock removes more than the targeted row.
     */
    public void unblockAllPresentIps() {
        for (final String ip : getBlockedIps()) {
            if (isIpInTable(ip)) {
                unblockIP(ip);
            }
        }
    }
}
