package com.sap.sailing.selenium.pages.adminconsole.usergroups;

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
import com.sap.sailing.selenium.pages.gwt.TextBoxPO;

public class UserGroupUserPanelPO extends PageArea {
    
    public static class UserEntryPO extends DataEntryWithSecurityActionsPO {

        @FindBy(how = ByName.class, using = "DELETE")
        private WebElement deleteButton;
        

        public UserEntryPO(CellTablePO<?> table, WebElement element) {
            super(table, element);
        }
        
        public void deleteUser() {
            deleteButton.click();
        }
    }
    
    private static final String TABLE_PERMISSION_COLUMN = "User name";
    @FindBy(how = BySeleniumId.class, using = "AddUserButton")
    private WebElement addButton;
    @FindBy(how = BySeleniumId.class, using = "UserSuggestion")
    private WebElement permissionInput;
    @FindBy(how = BySeleniumId.class, using = "UserGroupUserDTOTable")
    private WebElement permissionTable;
    
    public UserGroupUserPanelPO(WebDriver driver, WebElement element) {
        super(driver, element);
    }

    private CellTablePO<UserEntryPO> getUserTable() {
        return new GenericCellTablePO<>(this.driver, this.permissionTable, UserEntryPO.class);
    }

    public UserEntryPO findUser(final String userName) {
        final CellTablePO<UserEntryPO> table = getUserTable();
        for (UserEntryPO entry : table.getEntries()) {
            final String name = entry.getColumnContent(TABLE_PERMISSION_COLUMN);
            if (userName.equals(name)) {
                return entry;
            }
        }
        return null;
    }
    
    public void addUser(String name) {
        enterNewUser(name);
        clickAddButtonOrThrow();
        waitUntil(() -> findUser(name) != null);
    }

    public void enterNewUser(String name) {
        TextBoxPO.create(driver, permissionInput).appendText(name);
    }
    
    public void clickAddButtonOrThrow() {
        if (!addButton.isEnabled()) {
            throw new ElementNotInteractableException("Add Button was disabled");
        } else {
            addButton.click();
        }
    }
    
    public void clickAddButtonAndExpectPermissionError() {
        if (!addButton.isEnabled()) {
            throw new ElementNotInteractableException("Add Button was disabled");
        } else {
            addButton.click();
        }
        waitForAlertContainingMessageAndAccept("Current user does not have all the meta permissions of the user group the user would be added to");
    }

    public void removeUserFromGroup(String name) {
        final UserEntryPO findUser = findUser(name);
        findUser.deleteUser();
    }
    
    public void removeUserFromGroupAndExpectPermissionError(String name) {
        removeUserFromGroup(name);
        waitForAlertContainingMessageAndAccept("Current user does not have all the meta permissions of the user group the user would be removed from");
    }
}
