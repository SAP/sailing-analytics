package com.sap.sailing.selenium.pages.adminconsole.igtimi;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;

/**
 * <p>The page object representing the TracTrac Events tab.</p>
 * 
 * @author
 *   D049941
 */
public class IgtimiDevicesManagementPanelPO extends PageArea {
    
    @FindBy(how = BySeleniumId.class, using = "addIgtimiDevice")
    private WebElement addIgtimiDevice;
    
    /**
     * <p></p>
     * 
     * @param driver
     *   
     * @param element
     *   
     */
    public IgtimiDevicesManagementPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public AddIgtimiDeviceDialogPO openAddIgtimiDeviceDialog() {
        addIgtimiDevice.click();
        return getPO(AddIgtimiDeviceDialogPO::new, "AddIgtimiDeviceDialog");
    }
    
    public void addDevice(String email, String password) throws InterruptedException {
        AddIgtimiDeviceDialogPO addIgtimiDeviceDialog = openAddIgtimiDeviceDialog();
        addIgtimiDeviceDialog.setEmail(email);
        addIgtimiDeviceDialog.setPassword(password);
        addIgtimiDeviceDialog.pressOk();
        waitForNotificationAndDismiss();
    }
}
