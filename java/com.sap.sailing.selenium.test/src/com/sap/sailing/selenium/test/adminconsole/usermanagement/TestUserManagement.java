package com.sap.sailing.selenium.test.adminconsole.usermanagement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.usermanagement.ChangePasswordDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.usermanagement.CreateUserDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.usermanagement.EditUserDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.usermanagement.UserManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.usermanagement.UserRoleDefinitionPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.usermanagement.WildcardPermissionPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestUserManagement extends AbstractSeleniumTest {
    private static final String TEST_USER_PASSWORD = "test1";
    private static final String TEST_USER_MAIL = "";
    private static final String TEST_USER_NAME = "testUser";
    private static final String TEST_ROLE = "spectator";
    private static final String TEST_GROUP = "";
    private static final String TEST_PERMISSION = "USER:READ";

    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
    }

    private void createUser(final UserManagementPanelPO userManagementPanel) {
        final CreateUserDialogPO createUserdialog = userManagementPanel.getCreateUserDialog();
        assertNotNull(createUserdialog);
        createUserdialog.setValues(TEST_USER_NAME, TEST_USER_MAIL, TEST_USER_PASSWORD+UserManagementPanelPO.PASSWORD_COMPLEXITY_SALT, TEST_USER_PASSWORD+UserManagementPanelPO.PASSWORD_COMPLEXITY_SALT);
        createUserdialog.clickOkButtonOrThrow();
        // wait until user is displayed
        userManagementPanel.waitUntilUserFound(TEST_USER_NAME);
    }

    @SeleniumTestCase
    public void testUserCreation() {
        final UserManagementPanelPO userManagementPanel = goToUserManagementPanel();
        assertNull(userManagementPanel.findUser(TEST_USER_NAME));
        createUser(userManagementPanel);
        assertNotNull(userManagementPanel.findUser(TEST_USER_NAME));
    }

    @SeleniumTestCase
    public void testRoleCreation() {
        final UserManagementPanelPO userManagementPanel = goToUserManagementPanel();
        createUser(userManagementPanel);
        final UserRoleDefinitionPanelPO userRolesPO = userManagementPanel.getUserRoles();
        assertNull(userRolesPO.findRole(TEST_ROLE));
        createRole(userRolesPO);
        userManagementPanel.selectUser(TEST_USER_NAME);
        assertNotNull(userRolesPO.findRole(TEST_ROLE + ":"+ TEST_GROUP + ":" + TEST_USER_NAME));
    }

    private void createRole(final UserRoleDefinitionPanelPO userRolesPO) {
        userRolesPO.enterNewRoleValues(TEST_ROLE, null, TEST_USER_NAME);
        userRolesPO.clickAddButtonOrThrow();
    }

    @SeleniumTestCase
    public void testPermissionCreation() {
        final UserManagementPanelPO userManagementPanel = goToUserManagementPanel();
        createUser(userManagementPanel);
        final WildcardPermissionPanelPO wildcardPermissionPanelPO = userManagementPanel.getUserPermissions();
        assertNull(wildcardPermissionPanelPO.findPermission(TEST_PERMISSION));
        createPermission(wildcardPermissionPanelPO);
        userManagementPanel.selectUser(TEST_USER_NAME);
        assertNotNull(wildcardPermissionPanelPO.findPermission(TEST_PERMISSION));
    }

    private void createPermission(final WildcardPermissionPanelPO wildcardPermissionPanelPO) {
        wildcardPermissionPanelPO.enterNewPermissionValue(TEST_PERMISSION);
        wildcardPermissionPanelPO.clickAddButtonOrThrow();
    }

    @SeleniumTestCase
    public void testChangePassword() {
        final UserManagementPanelPO userManagementPanel = goToUserManagementPanel();
        final EditUserDialogPO editUserDialog = userManagementPanel.getEditUserDialog("admin");
        assertNotNull(editUserDialog);
        editUserDialog.clickChangePasswordButton();
        final ChangePasswordDialogPO changePasswordDialog = userManagementPanel.getChangePasswordDialog();
        changePasswordDialog.setNewPassword("supersecure");
        changePasswordDialog.clickOkButtonOrThrow();
    }

    @SeleniumTestCase
    public void testRemoveUser() {
        final UserManagementPanelPO userManagementPanel = goToUserManagementPanel();
        createUser(userManagementPanel);
        // get cell of test user name before removing
        userManagementPanel.deleteUser(TEST_USER_NAME);
        // double check and assert if test user is really removed from table
        assertNull(userManagementPanel.findUser(TEST_USER_NAME));
    }

    @SeleniumTestCase
    public void testRemoveUserPermission() {
        final UserManagementPanelPO userManagementPanel = goToUserManagementPanel();
        createUser(userManagementPanel);
        final WildcardPermissionPanelPO wildcardPermissionPanelPO = userManagementPanel.getUserPermissions();
        createPermission(wildcardPermissionPanelPO);
        userManagementPanel.selectUser(TEST_USER_NAME);
        wildcardPermissionPanelPO.deleteEntry(TEST_PERMISSION);
        userManagementPanel.selectUser(TEST_USER_NAME);
        assertNull(wildcardPermissionPanelPO.findPermission(TEST_PERMISSION));
    }

    @SeleniumTestCase
    public void testRemoveRole() {
        final UserManagementPanelPO userManagementPanel = goToUserManagementPanel();
        createUser(userManagementPanel);
        final UserRoleDefinitionPanelPO userRolesPO = userManagementPanel.getUserRoles();
        createRole(userRolesPO);
        userManagementPanel.selectUser(TEST_USER_NAME);
        userRolesPO.deleteEntry(TEST_ROLE + ":"+ TEST_GROUP + ":" + TEST_USER_NAME);
        userManagementPanel.selectUser(TEST_USER_NAME);
        assertNull(userRolesPO.findRole(TEST_ROLE + ":"+ TEST_GROUP + ":" + TEST_USER_NAME));
    }

    private UserManagementPanelPO goToUserManagementPanel() {
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        return adminConsole.goToUserManagement();
    }
}
