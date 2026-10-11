package com.sap.sailing.selenium.pages.adminconsole.leaderboard;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.adminconsole.ActionsHelper;
import com.sap.sailing.selenium.pages.adminconsole.regatta.RegattaListCompositePO.RegattaDescriptor;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.pages.gwt.GenericCellTablePO;
import com.sap.sailing.selenium.pages.leaderboard.PairingListCreationSetupDialogPO;

public class LeaderboardConfigurationPanelPO extends PageArea {
    public static class LeaderboardEntryPO extends DataEntryPO {

        private static final String ACTION_NAME_CONFIGURE_URL = "ACTION_CONFIGURE_URL";
        private static final String ACTION_NAME_CALC_PAIRINGLIST = "ACTION_CREATE_PAIRINGLIST";

        public LeaderboardEntryPO(CellTablePO<?> table, WebElement element) {
            super(table, element);
        }
        
        protected LeaderboardEntryPO() {
            super();
        }
        
        @Override
        public String getIdentifier() {
            return getName();
        }
        
        public String getName() {
            return getColumnContent("Name");
        }
        
        public String getLeaderboardURL() {
            WebElement link = this.context.findElement(By.xpath(".//td/div/a"));
            return link.getAttribute("href");
        }

        public LeaderboardUrlConfigurationDialogPO getLeaderboardPageUrlConfigurationDialog() {
            clickActionImage(ACTION_NAME_CONFIGURE_URL);
            return waitForPO(LeaderboardUrlConfigurationDialogPO::new, "LeaderboardPageUrlConfigurationDialog", 60);
        }
        public PairingListCreationSetupDialogPO getLeaderboardPairingListCreationSetupDialog(){
            clickActionImage(ACTION_NAME_CALC_PAIRINGLIST);
            return waitForPO(PairingListCreationSetupDialogPO::new, "PairingListCreationSetupDialog", 60);
        }
    }
    
    @FindBy(how = BySeleniumId.class, using = "CreateFlexibleLeaderboardButton")
    private WebElement createFlexibleLeaderboardButton;
    
    @FindBy(how = BySeleniumId.class, using = "CreateRegattaLeaderboardButton")
    private WebElement createRegattaLeaderboardButton;
    
    @FindBy(how = BySeleniumId.class, using = "RemoveLeaderboardsButton")
    private WebElement removeLeaderboardsButton;
    
    @FindBy(how = BySeleniumId.class, using = "LeaderboardsFilterTextBox")
    private WebElement leaderboardsFilterTextBox;
    
    @FindBy(how = BySeleniumId.class, using = "LeaderboardsCellTable")
    private WebElement leaderboardsCellTable;
    
    @FindBy(how = BySeleniumId.class, using = "LeaderboardDetailsPanel")
    private WebElement leaderboardDetailsPanel;
    
    @FindBy(how = BySeleniumId.class, using = "LeaderboardRefreshButton")
    private WebElement leaderboardRefreshButton;
    
    public LeaderboardConfigurationPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public void refreshLeaderboard() {
        this.leaderboardRefreshButton.click();
        waitForAjaxRequests();        
    }
    
    public FlexibleLeaderboardCreateDialogPO startCreatingFlexibleLeaderboard() {
        this.createFlexibleLeaderboardButton.click();
        // Wait, since we trigger an AJAX-request to get the available events
        waitForAjaxRequests();
        WebElement dialog = findElementBySeleniumId(this.driver, "FlexibleLeaderboardCreateDialog");
        return new FlexibleLeaderboardCreateDialogPO(this.driver, dialog);
    }
    
    // TODO: Extend with display name, scoring system, event, course area and discars
    public void createFlexibleLeaderboard(String name) {
        FlexibleLeaderboardCreateDialogPO dialog = startCreatingFlexibleLeaderboard();
        dialog.setName(name);
        dialog.pressOk();
        waitUntil(() -> findLeaderboard(name) != null);
    }
    
    public RegattaLeaderboardCreateDialogPO startCreatingRegattaLeaderboard() {
        this.createRegattaLeaderboardButton.click();
        WebElement dialog = findElementBySeleniumId(this.driver, "RegattaLeaderboardCreateDialog");
        return new RegattaLeaderboardCreateDialogPO(this.driver, dialog);
    }
    
    public void createRegattaLeaderboard(RegattaDescriptor regatta) {
        RegattaLeaderboardCreateDialogPO dialog = startCreatingRegattaLeaderboard();
        dialog.selectRegatta(regatta);
        dialog.pressOk();
        // The regatta leaderboard is named after the regatta (see RegattaDescriptor.toString()). Wait until the new row
        // has been rendered into the leaderboards cell table so that a subsequent getLeaderboardDetails() call does not
        // race the AJAX round-trip and return null. This mirrors createFlexibleLeaderboard() above.
        waitUntil(() -> findLeaderboard(regatta.toString()) != null);
    }
    
    public void deleteLeaderboard(String leaderboard) {
        LeaderboardEntryPO entry = findLeaderboard(leaderboard);
        if (entry != null) {
            WebElement removeAction = ActionsHelper.findDeleteAction(entry.getWebElement());
            removeAction.click();
            ActionsHelper.acceptAlert(this.driver);
            waitForAjaxRequests();
        }
    }
    
    public String getLeaderboardURL(String leaderboard) {
        LeaderboardEntryPO entry = findLeaderboard(leaderboard);
        if (entry != null) {
            return entry.getLeaderboardURL();
        }
        return null;
    }
    
    public List<String> getAvailableLeaderboards() {
        List<String> leaderboards = new ArrayList<>();
        CellTablePO<LeaderboardEntryPO> table = getLeaderboardTable();
        List<LeaderboardEntryPO> entries = table.getEntries();
        for (LeaderboardEntryPO entry : entries) {
            leaderboards.add(entry.getIdentifier());
        }
        return leaderboards;
    }
    
    public LeaderboardDetailsPanelPO getLeaderboardDetails(String leaderboard) {
        waitForAjaxRequests();
        LeaderboardEntryPO entry = findLeaderboard(leaderboard);
        if (entry == null) {
            return null;
        }
        entry.select();
        return new LeaderboardDetailsPanelPO(this.driver, this.leaderboardDetailsPanel);
    }
    
    public CellTablePO<LeaderboardEntryPO> getLeaderboardTable() {
        return new GenericCellTablePO<>(this.driver, this.leaderboardsCellTable, LeaderboardEntryPO.class);
    }
    
    private LeaderboardEntryPO findLeaderboard(String name) {
        CellTablePO<LeaderboardEntryPO> table = getLeaderboardTable();
        return table.getEntry(name);
    }
}
