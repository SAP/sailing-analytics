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
import com.sap.sailing.selenium.pages.gwt.TextBoxPO;

public class UserRoleDefinitionPanelPO extends PageArea {
    
    public static class RoleEntryPO extends DataEntryWithSecurityActionsPO {

        @FindBy(how = ByName.class, using = "DELETE")
        private WebElement deleteButton;
        

        public RoleEntryPO(CellTablePO<?> table, WebElement element) {
            super(table, element);
        }
        
        public void deleteRole() {
            deleteButton.click();
        }
        
        public void deleteRoleAndExpectPermissionError() {
            deleteRole();
            waitForAlertContainingMessageAndAccept("You are not allowed to revoke this role from user");
        }
    }
    
    private static final String TABLE_ROLE_NAME_COLUMN = "Role Name";
    @FindBy(how = BySeleniumId.class, using = "addRoleButton")
    private WebElement addRoleButton;
    @FindBy(how = BySeleniumId.class, using = "suggestRole")
    private WebElement roleNameInput;
    @FindBy(how = BySeleniumId.class, using = "tenantInput")
    private WebElement tenantInput;
    @FindBy(how = BySeleniumId.class, using = "userInput")
    private WebElement userInput;
    @FindBy(how = BySeleniumId.class, using = "RoleWithSecurityDTOTable")
    private WebElement roleTable;
    
    public UserRoleDefinitionPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    private CellTablePO<RoleEntryPO> getRoleTable() {
        return new GenericCellTablePO<>(this.driver, this.roleTable, RoleEntryPO.class);
    }

    public RoleEntryPO findRole(final String roleName) {
        final CellTablePO<RoleEntryPO> table = getRoleTable();
        for (RoleEntryPO entry : table.getEntries()) {
            final String name = entry.getColumnContent(TABLE_ROLE_NAME_COLUMN);
            if (roleName.equals(name)) {
                return entry;
            }
        }
        return null;
    }

    public void addRole(String rolename, String groupname, String username) {
        enterNewRoleValues(rolename, groupname, username);
        clickAddButtonOrThrow();
        waitUntil(() -> findRole(getRoleName(rolename, groupname, username)) != null);
    }

    private String getRoleName(String rolename, String groupname, String username) {
        StringBuilder sb = new StringBuilder(rolename);
        if (!groupname.isEmpty() || !username.isEmpty()) {
            sb.append(":" + groupname);
        }
        if (!username.isEmpty()) {
            sb.append(":" + username);
        }
        return sb.toString();
    }
    
    public void enterNewRoleValues(String rolename, String groupname, String username) {
        SuggestBoxPO.create(driver, roleNameInput).appendText(rolename);
        TextBoxPO.create(driver, tenantInput).appendText(groupname);
        TextBoxPO.create(driver, userInput).appendText(username);
    }
    
    public void clickAddButtonOrThrow() {
        if (!addRoleButton.isEnabled()) {
            throw new ElementNotInteractableException("Add Button was disabled");
        } else {
            addRoleButton.click();
        }
    }
    
    public void clickAddButtonAndExpectPermissionError() {
        if (!addRoleButton.isEnabled()) {
            throw new ElementNotInteractableException("Add Button was disabled");
        } else {
            addRoleButton.click();
        }
        waitForAlertContainingMessageAndAccept("You are not allowed to grant this role");
    }

    public void deleteEntry(String name) {
        final RoleEntryPO role = findRole(name);
        role.deleteRole();
    }
}
