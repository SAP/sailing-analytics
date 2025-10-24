package com.sap.sailing.selenium.test.adminconsole.roles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.roles.RoleDefinitionCreationAndUpdateDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.roles.RoleDefinitionsPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class TestRoleDefinitionCreation extends AbstractSeleniumTest {
    private static final String TEST_ROLE = "test";
    private static final String TEST_PERMISSION = "USER:READ";

    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
    }

    @SeleniumTestCase
    public void testRoleCreation() throws InterruptedException {
        final RoleDefinitionsPanelPO roleManagementPanel = goToRoleDefinitionPanel();
        assertNull(roleManagementPanel.findRole(TEST_ROLE));
        createRole(roleManagementPanel);
        assertNotNull(roleManagementPanel.findRole(TEST_ROLE));
    }
    
    @SeleniumTestCase
    public void testRoleDeletion() throws InterruptedException {
        final RoleDefinitionsPanelPO roleManagementPanel = goToRoleDefinitionPanel();
        createRole(roleManagementPanel);
        roleManagementPanel.deleteRole(TEST_ROLE);
        getWebDriver().switchTo().alert().accept();
        assertNull(roleManagementPanel.findRole(TEST_ROLE));
    }
    
    // FIXME: Disabled due to inconsistencies in handling of UpdateRoleDialog. See bug5364
    // @SeleniumTestCase
    // public void testRolePermissionRemoval() throws InterruptedException {
    // final RoleDefinitionsPanelPO roleManagementPanel = goToRoleDefinitionPanel();
    // createRole(roleManagementPanel);
    // final RoleEntryPO findRole = roleManagementPanel.findRole(TEST_ROLE);
    // final RoleDefinitionCreationAndUpdateDialogPO openUpdateDialog = findRole.openUpdateDialog();
    // // The Test Permission has been added in the default role creation
    // openUpdateDialog.removePermission(TEST_PERMISSION);
    // assertFalse(openUpdateDialog.isPermissionPresent(TEST_PERMISSION));
    // openUpdateDialog.clickOkButtonOrThrow();
    // final RoleEntryPO secondFindRole = roleManagementPanel.findRole(TEST_ROLE);
    // final RoleDefinitionCreationAndUpdateDialogPO secondOpenUpdateDialog = secondFindRole.openUpdateDialog();
    // assertFalse(secondOpenUpdateDialog.isPermissionPresent(TEST_PERMISSION));
    // }

    private void createRole(final RoleDefinitionsPanelPO roleManagementPanel) {
        final RoleDefinitionCreationAndUpdateDialogPO createRoleDialog = roleManagementPanel.getCreateRoleDialog();
        assertNotNull(createRoleDialog);
        createRoleDialog.setName(TEST_ROLE);
        createRoleDialog.addPermission(TEST_PERMISSION);
        createRoleDialog.clickOkButtonOrThrow();
    }

    private RoleDefinitionsPanelPO goToRoleDefinitionPanel() {
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        return adminConsole.goToRoleDefinitions();
    }
}
