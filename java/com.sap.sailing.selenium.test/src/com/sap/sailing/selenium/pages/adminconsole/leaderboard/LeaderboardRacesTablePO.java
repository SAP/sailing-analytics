package com.sap.sailing.selenium.pages.adminconsole.leaderboard;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.pages.adminconsole.ActionsHelper;
import com.sap.sailing.selenium.pages.adminconsole.leaderboard.LeaderboardRacesTablePO.RaceEntryPO;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;

public class LeaderboardRacesTablePO extends CellTablePO<RaceEntryPO> {

    public static class RaceEntryPO extends DataEntryPO {
        
        private RaceEntryPO(CellTablePO<?> table, WebElement element) {
            super(table, element);
        }
        
        @Override
        public Object getIdentifier() {
            return getColumnContent("Race");
        }
        
        public SetStartTimeDialogPO clickSetStartTime() {
            clickActionImage(ActionsHelper.SET_START_TIME_ACTION);
            return getPO(SetStartTimeDialogPO::new, "SetStartTimeDialog");
        }
        
    }
    
    public LeaderboardRacesTablePO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    @Override
    protected RaceEntryPO createDataEntry(WebElement element) {
        return new RaceEntryPO(this, element);
    }

}
