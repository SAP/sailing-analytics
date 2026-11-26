package com.sap.sailing.selenium.pages.adminconsole.leaderboard;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.adminconsole.ActionsHelper;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.pages.gwt.GenericCellTablePO;

public class LeaderboardGroupConfigurationPanelPO extends PageArea {
    @FindBy(how = BySeleniumId.class, using = "CreateLeaderboardGroupButton")
    private WebElement createLeaderboardGroupButton;

    @FindBy(how = BySeleniumId.class, using = "RemoveLeaderboardsButton")
    private WebElement removeButton;
    
    @FindBy(how = BySeleniumId.class, using = "LeaderboardGroupsCellTable")
    private WebElement leaderboardGroupsTable;
    
    @FindBy(how = BySeleniumId.class, using = "LeaderboardGroupDetailsPanel")
    private WebElement leaderboardGroupDetailsPanel;
    
    public LeaderboardGroupConfigurationPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public LeaderboardGroupCreateDialogPO startCreatingLeaderboardGroup() {
        this.createLeaderboardGroupButton.click();
        
        WebElement dialog = findElementBySeleniumId(this.driver, "LeaderboardGroupCreateDialog");
        
        return new LeaderboardGroupCreateDialogPO(this.driver, dialog);
    }
    
    public void createLeaderboardGroup(String name, String description) {
        LeaderboardGroupCreateDialogPO dialog = startCreatingLeaderboardGroup();
        dialog.setName(name);
        dialog.setDescription(description);
        dialog.pressOk();
    }
    
    public CellTablePO<DataEntryPO> getLeaderboardGroupsTable() {
        return new GenericCellTablePO<DataEntryPO>(this.driver, this.leaderboardGroupsTable, DataEntryPO.class);
    }
    
    public void deleteLeaderboardGroup(String name) {
        DataEntryPO result = findLeaderboardGroup(name);
        
        if (result != null) {
            result.clickActionImage(ActionsHelper.DELETE_ACTION);
            ActionsHelper.acceptAlert(this.driver);
            
            waitForAjaxRequests();
        }
    }
    
    public LeaderboardGroupDetailsPanelPO getLeaderboardGroupDetails(String name) {
        DataEntryPO result = findLeaderboardGroup(name);
        
        if (result != null) {
            result.select();
        }
        
        return getLeaderboardGroupDetails();
    }
    
    public LeaderboardGroupDetailsPanelPO getLeaderboardGroupDetails() {
        if(!this.leaderboardGroupDetailsPanel.isDisplayed()) {
            return null; 
        }
        
        return new LeaderboardGroupDetailsPanelPO(this.driver, this.leaderboardGroupDetailsPanel);
    }
    
    private DataEntryPO findLeaderboardGroup(String name) {
        final CellTablePO<DataEntryPO> table = getLeaderboardGroupsTable();
        return table.getEntries().stream().filter(e->e.getColumnContent("Name").equals(name)).findFirst().orElse(null);
    }
}
