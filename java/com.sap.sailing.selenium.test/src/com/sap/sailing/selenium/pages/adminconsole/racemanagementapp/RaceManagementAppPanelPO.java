package com.sap.sailing.selenium.pages.adminconsole.racemanagementapp;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.adminconsole.racemanagementapp.DeviceConfigurationsTablePO.DeviceConfigurationEntryPO;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.GenericCellTablePO;

public class RaceManagementAppPanelPO extends PageArea {
    
    private static final String DEVICE_CONGIGURATION_DETAILS_AREA_ID = "DeviceConfigurationDetailComposite";
    private static final String NEW_DEVICE_CONFIGURATION_DIALOG_ID = "SelectNameForNewDeviceConfigurationDialog";
    
    @FindBy(how = BySeleniumId.class, using = "addDeviceConfigurationButton")
    private WebElement addDeviceConfigurationButton;
    
    @FindBy(how = BySeleniumId.class, using = "DeviceConfigurationList")
    private WebElement deviceConfigurationListTable;

    public RaceManagementAppPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public DeviceConfigurationCreateDialogPO createDeviceConfiguration() {
        addDeviceConfigurationButton.click();
        return getPO(DeviceConfigurationCreateDialogPO::new, NEW_DEVICE_CONFIGURATION_DIALOG_ID);
    }
    
    public DeviceConfigurationDetailsAreaPO getDeviceConfigurationDetails() {
        final CellTablePO<DeviceConfigurationEntryPO> deviceConfigurationTable = getLeaderboardTable();
        waitUntil(() -> !deviceConfigurationTable.getEntries().isEmpty());
        final DeviceConfigurationEntryPO deviceConfiguration = deviceConfigurationTable.getEntries().get(0);
        deviceConfiguration.select();
        return getPO(DeviceConfigurationDetailsAreaPO::new, DEVICE_CONGIGURATION_DETAILS_AREA_ID);
    }

    
    public CellTablePO<DeviceConfigurationEntryPO> getLeaderboardTable() {
        return new GenericCellTablePO<>(this.driver, this.deviceConfigurationListTable, DeviceConfigurationEntryPO.class);
    }

}
