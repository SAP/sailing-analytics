package com.sap.sailing.selenium.pages.adminconsole.usermanagement;

import org.openqa.selenium.By.ByName;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.adminconsole.security.DataEntryWithSecurityActionsPO;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.GenericCellTablePO;
import com.sap.sailing.selenium.pages.gwt.SuggestBoxPO;

public class WildcardPermissionPanelPO extends PageArea {
    
    public static class PermissionEntryPO extends DataEntryWithSecurityActionsPO {

        @FindBy(how = ByName.class, using = "DELETE")
        private WebElement deleteButton;
        

        public PermissionEntryPO(CellTablePO<?> table, WebElement element) {
            super(table, element);
        }
        
        public void deletePermission() {
            deleteButton.click();
        }
        
        public void deletePermissionAndExpectPermissionError() {
            deletePermission();
            waitForAlertContainingMessageAndAccept("Could not remove permission");
        }
    }
    
    private static final String TABLE_PERMISSION_COLUMN = "Permission";
    @FindBy(how = BySeleniumId.class, using = "addPermissionButton")
    private WebElement addButton;
    @FindBy(how = BySeleniumId.class, using = "suggestPermission")
    private WebElement permissionInput;
    @FindBy(how = BySeleniumId.class, using = "WildcardPermissionWithSecurityDTOTable")
    private WebElement permissionTable;
    
    public WildcardPermissionPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    private CellTablePO<PermissionEntryPO> getPermissionTable() {
        return new GenericCellTablePO<>(this.driver, this.permissionTable, PermissionEntryPO.class);
    }

    public PermissionEntryPO findPermission(final String permissionName) {
        final CellTablePO<PermissionEntryPO> table = getPermissionTable();
        for (PermissionEntryPO entry : table.getEntries()) {
            final String name = entry.getColumnContent(TABLE_PERMISSION_COLUMN);
            if (permissionName.equals(name)) {
                return entry;
            }
        }
        return null;
    }
    
    public void addPermission(String permissionName) {
        enterNewPermissionValue(permissionName);
        clickAddButtonOrThrow();
        waitUntil(() -> findPermission(permissionName) != null);
    }

    public void enterNewPermissionValue(String permissionName) {
        SuggestBoxPO.create(driver, permissionInput).appendText(permissionName);
    }
    
    public void clickAddButtonOrThrow() {
        if (!addButton.isEnabled()) {
            throw new ElementNotInteractableException("Add Button was disabled");
        } else {
            addButton.click();
        }
    }
    
    public void clickAddButtonAndExpectPermissionError(String username) {
        if (!addButton.isEnabled()) {
            throw new ElementNotInteractableException("Add Button was disabled");
        } else {
            addButton.click();
        }
        waitForAlertContainingMessageAndAccept("User "+username+" is not permitted to grant permission");
    }
    
    public void deleteEntry(String name){
        final PermissionEntryPO permission = findPermission(name);
        permission.deletePermission();
    }
}
