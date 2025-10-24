package com.sap.sailing.selenium.test.adminconsole.usermanagement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.advanced.LocalServerPO;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO.EventEntryPO;
import com.sap.sailing.selenium.pages.adminconsole.roles.RoleDefinitionCreationAndUpdateDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.roles.RoleDefinitionsPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.security.AclActionInputPO;
import com.sap.sailing.selenium.pages.adminconsole.security.AclPopupPO;
import com.sap.sailing.selenium.pages.adminconsole.usergroups.UserGroupManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.usergroups.UserGroupRoleDefinitionPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.usergroups.UserGroupUserPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.usermanagement.EditRolesAndPermissionsForUserDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.usermanagement.UserManagementPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.usermanagement.UserRoleDefinitionPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.usermanagement.WildcardPermissionPanelPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

/**
 * Test cases to ensure that a negative ACL prevents a permission to be given if the ACL affects the permission in
 * question. There are several cases where such a negative ACL may affect a meta permission check.
 */
public class TestNegativeAcls extends AbstractSeleniumTest {
    private static final String USER1_NAME = "user1";
    private static final String USER2_NAME = "user2";
    private static final String USER3_NAME = "user3";
    private static final String USER4_NAME = "user4";
    private static final String USER1_TENANT = USER1_NAME + "-tenant";
    private static final String USER2_TENANT = USER2_NAME + "-tenant";
    private static final String EVENT_NAME = "Demo event";
    private static final String USER_ROLE = "user";
    private static final String UPDATE_ACTION = "UPDATE";
    private static final String READ_ACTION = "READ";
    private static final String DELETE_ACTION = "DELETE";
    private static final String USER_GROUP_READ_PERMISSION = "USER_GROUP:READ:*";
    private static final String EVENT_ALL_PERMISSION_PREFIX = "EVENT:*:";
    private static final String EVENT_UPLOAD_MEDIA_PERMISSION = "EVENT:UPLOAD_MEDIA:*";
    private static final String CUSTOM_ROLE = "custom-role";

    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        final LocalServerPO localServerPanel = adminConsole.goToLocalServerPanel();
        // deactivates the public server warning when opening the AdminConsole as simple user
        localServerPanel.setPublicServer(false);
        // ensures that our users can create objects on this server
        localServerPanel.setSelfServiceServer(true);
        final UserManagementPanelPO userManagementPanel = adminConsole.goToUserManagement();
        userManagementPanel.createUserWithEqualUsernameAndPassword(USER1_NAME);
        userManagementPanel.createUserWithEqualUsernameAndPassword(USER2_NAME);
        userManagementPanel.createUserWithEqualUsernameAndPassword(USER3_NAME);
        userManagementPanel.createUserWithEqualUsernameAndPassword(USER4_NAME);
        // user1 may only add other users' tenants to an ACL if this group is readable
        // adding permission USER_GROUP:READ:* just makes all groups readable to user 1
        userManagementPanel.grantPermissionToUser(USER1_NAME, USER_GROUP_READ_PERMISSION);
        userManagementPanel.grantPermissionToUser(USER1_NAME, EVENT_UPLOAD_MEDIA_PERMISSION);
    }

    @SeleniumTestCase
    public void testUserWithNegativeAclCantGiveARoleToAnotherUser() {
        // user1 creates an event
        AdminConsolePage adminConsole = changeUserAndReloadAdminConsole(USER1_NAME);
        addEventWithNegativeAclForGroup(adminConsole, USER2_TENANT);
        // user1 gives user2 and user3 the user role for objects owned by user1
        UserManagementPanelPO userManagement = adminConsole.goToUserManagement();
        userManagement.grantRoleToUserWithUserQualification(USER2_NAME, USER_ROLE, USER1_NAME);
        userManagement.grantRoleToUserWithUserQualification(USER3_NAME, USER_ROLE, USER1_NAME);
        // user2 tries to give the user role for objects owned by user1 to user4
        userManagement = changeUserAndReloadAdminConsole(USER2_NAME).goToUserManagement();
        UserRoleDefinitionPanelPO userRoles = userManagement.openEditRolesAndPermissionsDialogForUser(USER4_NAME).getUserRoles();
        userRoles.enterNewRoleValues(USER_ROLE, "", USER1_NAME);
        // this is expected to fail because the negative ACL on one of user1's events
        // causes user 2 to not have all permissions implied by the user role
        userRoles.clickAddButtonAndExpectPermissionError();
        // user3 grants the user role for objects owned by user1 to user4
        // in this case, it works because user3 isn't affected by the negative ACL
        userManagement = changeUserAndReloadAdminConsole(USER3_NAME).goToUserManagement();
        userManagement.grantRoleToUserWithUserQualification(USER4_NAME, USER_ROLE, USER1_NAME);
        String qualifiedRoleName = USER_ROLE + "::" + USER1_NAME;
        userRoles = userManagement.openEditRolesAndPermissionsDialogForUser(USER4_NAME).getUserRoles();
        addRUDForAllToAcl(userRoles.findRole(qualifiedRoleName).openAclPopup());
        // user2 tries to remove the user role for objects owned by user1 from user4
        userManagement = changeUserAndReloadAdminConsole(USER2_NAME).goToUserManagement();
        userRoles = userManagement.openEditRolesAndPermissionsDialogForUser(USER4_NAME).getUserRoles();
        // this is expected to fail because the negative ACL on one of user1's events
        // causes user 2 to not have all permissions implied by the user role
        userRoles.findRole(qualifiedRoleName).deleteRoleAndExpectPermissionError();
        // user3 removes the user role for objects owned by user1 from user4
        // in this case, it works because user3 isn't affected by the negative ACL
        userManagement = changeUserAndReloadAdminConsole(USER3_NAME).goToUserManagement();
        userRoles = userManagement.openEditRolesAndPermissionsDialogForUser(USER4_NAME).getUserRoles();
        userRoles.findRole(qualifiedRoleName).deleteRole();
        assertNull(userRoles.findRole(qualifiedRoleName));
    }
    
    @SeleniumTestCase
    public void testUserWithNegativeAclCantGiveAPermissionToAnotherUser() {
        // user1 creates an event
        AdminConsolePage adminConsole = changeUserAndReloadAdminConsole(USER1_NAME);
        String eventId = addEventWithNegativeAclForGroup(adminConsole, USER2_TENANT);
        String eventAllPermission = EVENT_ALL_PERMISSION_PREFIX + eventId;
        // user1 gives user2 and user3 the permission "EVENT:*:<event-id>"
        UserManagementPanelPO userManagement = adminConsole.goToUserManagement();
        userManagement.grantPermissionToUser(USER2_NAME, eventAllPermission);
        userManagement.grantPermissionToUser(USER3_NAME, eventAllPermission);
        // user2 tries to give the permission "EVENT:*:<event-id>" to user4
        userManagement = changeUserAndReloadAdminConsole(USER2_NAME).goToUserManagement();
        EditRolesAndPermissionsForUserDialogPO editRolesAndPermissionsDialogForUser = userManagement.openEditRolesAndPermissionsDialogForUser(USER4_NAME);
        WildcardPermissionPanelPO userPermissions = editRolesAndPermissionsDialogForUser.getUserPermissions();
        userPermissions.enterNewPermissionValue(eventAllPermission);
        // this is expected to fail because the negative ACL on the event
        // causes user2 to not have all permissions implied by the wildcard permission
        userPermissions.clickAddButtonAndExpectPermissionError(USER2_NAME);
        // user3 grants permission "EVENT:*:<event-id>" to user4
        // in this case, it works because user3 isn't affected by the negative ACL
        userManagement = changeUserAndReloadAdminConsole(USER3_NAME).goToUserManagement();
        editRolesAndPermissionsDialogForUser = userManagement.openEditRolesAndPermissionsDialogForUser(USER4_NAME);
        userPermissions = editRolesAndPermissionsDialogForUser.getUserPermissions();
        userPermissions.addPermission(eventAllPermission);
        addRUDForAllToAcl(userPermissions.findPermission(eventAllPermission).openAclPopup());
        // user2 tries to remove the permission "EVENT:*:<event-id>" from user4
        userManagement = changeUserAndReloadAdminConsole(USER2_NAME).goToUserManagement();
        editRolesAndPermissionsDialogForUser = userManagement.openEditRolesAndPermissionsDialogForUser(USER4_NAME);
        userPermissions = editRolesAndPermissionsDialogForUser.getUserPermissions();
        userPermissions.findPermission(eventAllPermission).deletePermissionAndExpectPermissionError();
        // user3 removes permission "EVENT:*:<event-id>" from user4
        // in this case, it works because user3 isn't affected by the negative ACL
        userManagement = changeUserAndReloadAdminConsole(USER3_NAME).goToUserManagement();
        editRolesAndPermissionsDialogForUser = userManagement.openEditRolesAndPermissionsDialogForUser(USER4_NAME);
        userPermissions = editRolesAndPermissionsDialogForUser.getUserPermissions();
        userPermissions.findPermission(eventAllPermission).deletePermission();
        assertNull(userPermissions.findPermission(eventAllPermission));
    }
    
    @SeleniumTestCase
    public void testUserWithNegativeAclCantAddUserToGroup() {
        // user1 creates an event
        AdminConsolePage adminConsole = changeUserAndReloadAdminConsole(USER1_NAME);
        addEventWithNegativeAclForGroup(adminConsole, USER2_TENANT);
        // user1 adds the user role to his tenant and adds user2 and user3 as members
        UserGroupManagementPanelPO userGroupManagement = adminConsole.goToUserGroupDefinitions();
        userGroupManagement.findGroup(USER1_TENANT).select();
        userGroupManagement.getUserGroupRoles().addRole(USER_ROLE);
        userGroupManagement.getUserGroupUsers().addUser(USER2_NAME);
        userGroupManagement.getUserGroupUsers().addUser(USER3_NAME);
        // user2 tries to add user4 as member to user1's tenant
        userGroupManagement = changeUserAndReloadAdminConsole(USER2_NAME).goToUserGroupDefinitions();
        UserGroupUserPanelPO userGroupUsers = userGroupManagement.getUserGroupUsers();
        userGroupManagement.findGroup(USER1_TENANT).select();
        userGroupUsers.enterNewUser(USER4_NAME);
        // this is expected to fail because the negative ACL on the event
        // causes user2 to not have all permissions of the user role for user1-tenant
        userGroupUsers.clickAddButtonAndExpectPermissionError();
        userGroupManagement = changeUserAndReloadAdminConsole(USER3_NAME).goToUserGroupDefinitions();
        userGroupUsers = userGroupManagement.getUserGroupUsers();
        userGroupManagement.findGroup(USER1_TENANT).select();
        // user3 adds user4 to user1-tenant
        // in this case, it works because user3 isn't affected by the negative ACL
        userGroupUsers.addUser(USER4_NAME);
        assertNotNull(userGroupUsers.findUser(USER4_NAME));
        // user2 tries to remove user4 from user1's tenant
        userGroupManagement = changeUserAndReloadAdminConsole(USER2_NAME).goToUserGroupDefinitions();
        userGroupUsers = userGroupManagement.getUserGroupUsers();
        userGroupManagement.findGroup(USER1_TENANT).select();
        // this is expected to fail because the negative ACL on the event
        // causes user2 to not have all permissions of the user role for user1-tenant
        userGroupUsers.removeUserFromGroupAndExpectPermissionError(USER4_NAME);
        userGroupManagement = changeUserAndReloadAdminConsole(USER3_NAME).goToUserGroupDefinitions();
        userGroupUsers = userGroupManagement.getUserGroupUsers();
        userGroupManagement.findGroup(USER1_TENANT).select();
        // user3 adds user4 to user1-tenant
        // in this case, it works because user3 isn't affected by the negative ACL
        userGroupUsers.removeUserFromGroup(USER4_NAME);
        assertNull(userGroupUsers.findUser(USER4_NAME));
    }
    
    @SeleniumTestCase
    public void testUserWithNegativeAclCantAddRoleToGroup() {
        // user1 creates an event
        AdminConsolePage adminConsole = changeUserAndReloadAdminConsole(USER1_NAME);
        addEventWithNegativeAclForGroup(adminConsole, USER2_TENANT);
        UserGroupManagementPanelPO userGroupManagement = adminConsole.goToUserGroupDefinitions();
        userGroupManagement.findGroup(USER1_TENANT).select();
        userGroupManagement.getUserGroupUsers().addUser(USER4_NAME);
        // user1 adds the user role for his tenant to user2 and user3
        final UserManagementPanelPO userManagement = adminConsole.goToUserManagement();
        userManagement.grantRoleToUserWithGroupQualification(USER2_NAME, USER_ROLE, USER1_TENANT);
        userManagement.grantRoleToUserWithGroupQualification(USER3_NAME, USER_ROLE, USER1_TENANT);
        // user2 tries to add the user role to user1's tenant
        userGroupManagement = changeUserAndReloadAdminConsole(USER2_NAME).goToUserGroupDefinitions();
        userGroupManagement.findGroup(USER1_TENANT).select();
        UserGroupRoleDefinitionPanelPO userGroupRoles = userGroupManagement.getUserGroupRoles();
        userGroupRoles.enterNewRoleName(USER_ROLE);
        // this is expected to fail because the negative ACL on the event
        // causes user2 to not have all permissions of the user role for user1-tenant
        userGroupRoles.clickAddButtonAndExpectPermissionError();
        userGroupManagement = changeUserAndReloadAdminConsole(USER3_NAME).goToUserGroupDefinitions();
        userGroupManagement.findGroup(USER1_TENANT).select();
        userGroupRoles = userGroupManagement.getUserGroupRoles();
        // user3 adds user4 to user1-tenant
        // in this case, it works because user3 isn't affected by the negative ACL
        userGroupRoles.addRole(USER_ROLE);
        assertNotNull(userGroupRoles.findRole(USER_ROLE));
        // user2 tries to remove the user role from user1's tenant
        userGroupManagement = changeUserAndReloadAdminConsole(USER2_NAME).goToUserGroupDefinitions();
        userGroupManagement.findGroup(USER1_TENANT).select();
        userGroupRoles = userGroupManagement.getUserGroupRoles();
        // this is expected to fail because the negative ACL on the event
        // causes user2 to not have all permissions of the user role for user1-tenant
        userGroupRoles.removeRoleAndExpectPermissionError(USER_ROLE);
        userGroupManagement = changeUserAndReloadAdminConsole(USER3_NAME).goToUserGroupDefinitions();
        userGroupManagement.findGroup(USER1_TENANT).select();
        userGroupRoles = userGroupManagement.getUserGroupRoles();
        // user3 removes user4 from user1-tenant
        // in this case, it works because user3 isn't affected by the negative ACL
        userGroupRoles.removeRole(USER_ROLE);
        assertNull(userGroupRoles.findRole(USER_ROLE));
    }
    
    @SeleniumTestCase
    public void testUserWithNegativeAclCantAddPermissionToRoleThatIsAssociatedToAUserGroup() {
        // user1 creates an event
        AdminConsolePage adminConsole = changeUserAndReloadAdminConsole(USER1_NAME);
        String eventId = addEventWithNegativeAclForGroup(adminConsole, USER2_TENANT);
        String eventAllPermission = EVENT_ALL_PERMISSION_PREFIX + eventId;
        UserManagementPanelPO userManagement = adminConsole.goToUserManagement();
        // user1 grants EVENT:*:<event-id> permission to user2 and user3 for objects owned by user1
        userManagement.grantPermissionToUser(USER2_NAME, eventAllPermission);
        userManagement.grantPermissionToUser(USER3_NAME, eventAllPermission);
        createCustomRoleWithReadAndUpdateAclForAll(adminConsole);
        UserGroupManagementPanelPO userGroupManagement = adminConsole.goToUserGroupDefinitions();
        userGroupManagement.findGroup(USER1_TENANT).select();
        userGroupManagement.getUserGroupRoles().addRole(CUSTOM_ROLE);
        // user2 tries to add a permission to custom-role
        RoleDefinitionsPanelPO roleDefinitions = changeUserAndReloadAdminConsole(USER2_NAME).goToRoleDefinitions();
        RoleDefinitionCreationAndUpdateDialogPO updateDialog = roleDefinitions.findRole(CUSTOM_ROLE).openUpdateDialog();
        updateDialog.addPermission(eventAllPermission);
        // this is expected to fail because the negative ACL on the event
        // causes user2 to not have all permissions implied by the permission
        updateDialog.clickOkButtonAndExpectPermissionError();
        roleDefinitions = changeUserAndReloadAdminConsole(USER3_NAME).goToRoleDefinitions();
        updateDialog = roleDefinitions.findRole(CUSTOM_ROLE).openUpdateDialog();
        updateDialog.addPermission(eventAllPermission);
        // in this case, it works because user3 isn't affected by the negative ACL
        updateDialog.clickOkButtonOrThrow();
        assertTrue(roleDefinitions.findRole(CUSTOM_ROLE).getPermissions().contains(eventAllPermission));
        // FIXME: Disabled due to inconsistencies in handling of UpdateRoleDialog. See bug5364
        // // user2 tries to remove a permission from custom-role
        // roleDefinitions = changeUserAndReloadAdminConsole(USER2_NAME).goToRoleDefinitions();
        // updateDialog = roleDefinitions.findRole(CUSTOM_ROLE).openUpdateDialog();
        // updateDialog.removePermission(eventAllPermission);
        // // this is expected to fail because the negative ACL on the event
        // // causes user2 to not have all permissions implied by the permission
        // updateDialog.clickOkButtonAndExpectPermissionError();
        //
        // roleDefinitions = changeUserAndReloadAdminConsole(USER3_NAME).goToRoleDefinitions();
        // updateDialog = roleDefinitions.findRole(CUSTOM_ROLE).openUpdateDialog();
        // updateDialog.removePermission(eventAllPermission);
        // // in this case, it works because user3 isn't affected by the negative ACL
        // updateDialog.clickOkButtonOrThrow();
        // assertFalse(roleDefinitions.findRole(CUSTOM_ROLE).getPermissions().contains(eventAllPermission));
    }
    
    @SeleniumTestCase
    public void testUserWithNegativeAclCantAddPermissionToRoleThatIsAssociatedToAUser() {
        // user1 creates an event
        AdminConsolePage adminConsole = changeUserAndReloadAdminConsole(USER1_NAME);
        String eventId = addEventWithNegativeAclForGroup(adminConsole, USER2_TENANT);
        String eventAllPermission = EVENT_ALL_PERMISSION_PREFIX + eventId;
        createCustomRoleWithReadAndUpdateAclForAll(adminConsole);
        UserManagementPanelPO userManagement = adminConsole.goToUserManagement();
        // user1 grants EVENT:*:<event-id> permission to user2 and user3 for objects owned by user1
        userManagement.grantPermissionToUser(USER2_NAME, eventAllPermission);
        userManagement.grantPermissionToUser(USER3_NAME, eventAllPermission);
        // user1 grants the custom-role to user4 for objects owned by user1
        // this role does not grant any permissions initially
        userManagement.grantRoleToUserWithUserQualification(USER4_NAME, CUSTOM_ROLE, USER1_NAME);
        // user2 tries to add a permission to custom-role
        RoleDefinitionsPanelPO roleDefinitions = changeUserAndReloadAdminConsole(USER2_NAME).goToRoleDefinitions();
        RoleDefinitionCreationAndUpdateDialogPO updateDialog = roleDefinitions.findRole(CUSTOM_ROLE).openUpdateDialog();
        updateDialog.addPermission(eventAllPermission);
        // this is expected to fail because the negative ACL on the event
        // causes user2 to not have all permissions implied by the permission
        updateDialog.clickOkButtonAndExpectPermissionError();
        roleDefinitions = changeUserAndReloadAdminConsole(USER3_NAME).goToRoleDefinitions();
        updateDialog = roleDefinitions.findRole(CUSTOM_ROLE).openUpdateDialog();
        updateDialog.addPermission(eventAllPermission);
        // in this case, it works because user3 isn't affected by the negative ACL
        updateDialog.clickOkButtonOrThrow();
        assertTrue(roleDefinitions.findRole(CUSTOM_ROLE).getPermissions().contains(eventAllPermission));
        // FIXME: Disabled due to inconsistencies in handling of UpdateRoleDialog. See bug5364
        // // user2 tries to remove a permission from custom-role
        // roleDefinitions = changeUserAndReloadAdminConsole(USER2_NAME).goToRoleDefinitions();
        // updateDialog = roleDefinitions.findRole(CUSTOM_ROLE).openUpdateDialog();
        // updateDialog.removePermission(eventAllPermission);
        // // this is expected to fail because the negative ACL on the event
        // // causes user2 to not have all permissions implied by the permission
        // updateDialog.clickOkButtonAndExpectPermissionError();
        //
        // roleDefinitions = changeUserAndReloadAdminConsole(USER3_NAME).goToRoleDefinitions();
        // updateDialog = roleDefinitions.findRole(CUSTOM_ROLE).openUpdateDialog();
        // updateDialog.removePermission(eventAllPermission);
        // // in this case, it works because user3 isn't affected by the negative ACL
        // updateDialog.clickOkButtonOrThrow();
        // assertFalse(roleDefinitions.findRole(CUSTOM_ROLE).getPermissions().contains(eventAllPermission));
    }
    
    private void addRUDForAllToAcl(AclPopupPO aclPopup) {
        aclPopup.addUserGroup("");
        aclPopup.getAllowedActionsInput().addAction(READ_ACTION);
        aclPopup.getAllowedActionsInput().addAction(UPDATE_ACTION);
        aclPopup.getAllowedActionsInput().addAction(DELETE_ACTION);
        aclPopup.clickOkButtonOrThrow();
    }
    
    private AdminConsolePage changeUserAndReloadAdminConsole(String usernameAndPassword) {
        clearSession(getWebDriver());
        setUpAuthenticatedSession(getWebDriver(), usernameAndPassword, usernameAndPassword+UserManagementPanelPO.PASSWORD_COMPLEXITY_SALT);
        return AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
    }
    
    private void createCustomRoleWithReadAndUpdateAclForAll(AdminConsolePage adminConsole) {
        RoleDefinitionsPanelPO roleDefinitions = adminConsole.goToRoleDefinitions();
        RoleDefinitionCreationAndUpdateDialogPO createRoleDialog = roleDefinitions.getCreateRoleDialog();
        createRoleDialog.setName(CUSTOM_ROLE);
        createRoleDialog.clickOkButtonOrThrow();
        AclPopupPO aclPopup = roleDefinitions.findRole(CUSTOM_ROLE).openAclPopup();
        aclPopup.addUserGroup("");
        AclActionInputPO allowedActionsInput = aclPopup.getAllowedActionsInput();
        // adding this ACL ensures that other users may read and update the custom role
        allowedActionsInput.addAction(READ_ACTION);
        allowedActionsInput.addAction(UPDATE_ACTION);
        aclPopup.clickOkButtonOrThrow();
    }

    /**
     * @return the UUID of the newly created event.
     */
    private String addEventWithNegativeAclForGroup(AdminConsolePage adminConsole, String groupToUseForNegativeAcl) {
        final EventConfigurationPanelPO eventsPanel = adminConsole.goToEvents();
        eventsPanel.createEmptyEvent(EVENT_NAME, EVENT_NAME, EVENT_NAME,
                Date.from(ZonedDateTime.now().minus(Duration.ofDays(1l)).toInstant()),
                Date.from(ZonedDateTime.now().plus(Duration.ofDays(1l)).toInstant()), true);
        EventEntryPO eventEntry = eventsPanel.getEventEntry(EVENT_NAME);
        final String eventUUID = eventEntry.getUUID();
        final AclPopupPO aclPopup = eventEntry.openAclPopup();
        aclPopup.addUserGroup(groupToUseForNegativeAcl);
        aclPopup.getDeniedActionsInput().addAction(UPDATE_ACTION);
        aclPopup.clickOkButtonOrThrow();
        return eventUUID;
    }
}