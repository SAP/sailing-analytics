package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.selenium.api.core.ApiContext.SECURITY_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static com.sap.sailing.selenium.api.core.ApiContext.createApiContext;
import static com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage.goToPage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.concurrent.atomic.LongAdder;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.core.HttpException;
import com.sap.sailing.selenium.api.event.EventApi;
import com.sap.sailing.selenium.api.event.EventApi.Event;
import com.sap.sailing.selenium.api.event.RoleApi;
import com.sap.sailing.selenium.api.event.RoleApi.RoleDefinition;
import com.sap.sailing.selenium.api.event.SecurityApi;
import com.sap.sailing.selenium.api.event.UserGroupApi;
import com.sap.sailing.selenium.api.event.UserGroupApi.UserGroup;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;
import com.sap.sse.common.Util;

public class UserGroupApiTest extends AbstractSeleniumTest {

    private static final String ANOTHER_TEST_PASSWORD = "pa0984<><;''ssword";
    private static final String DONALDS_PASSWORD = "daisy9874kl.]]*#0815";
    private final UserGroupApi userGroupApi = new UserGroupApi();
    private final RoleApi roleApi = new RoleApi();
    private final SecurityApi securityApi = new SecurityApi();

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot(), /* headless */ true);
        super.setUp();
    }

    @SeleniumTestCase
    public void testCreateAndGetAndDeleteUserGroup() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);

        // create user group
        final String groupName = "test-group-01";
        final UserGroup userGroupCreated = userGroupApi.createUserGroup(adminCtx, groupName);

        assertEquals(groupName, userGroupCreated.getGroupName(), "Responded username of createUserGroup is different!");
        assertNotNull(userGroupCreated.getGroupId(), "GroupId is missing in reponse!");
        assertEquals(1, Util.size(userGroupCreated.getUsers()), "Wrong username count in response!");

        // check, if user group was created
        final UserGroup userGroupGet = userGroupApi.getUserGroup(adminCtx, userGroupCreated.getGroupId());

        assertEquals(userGroupCreated.getGroupId(), userGroupGet.getGroupId(),
                "Responded user group ids are not the same!");
        assertEquals(userGroupCreated.getGroupName(), userGroupGet.getGroupName(),
                "Responded user group names are not the same!");
        assertSameElements(userGroupCreated.getUsers(), userGroupGet.getUsers());
        assertSameElements(userGroupCreated.getRoles(), userGroupGet.getRoles());

        // check getByName
        final UserGroup userGroupGetByName = userGroupApi.getUserGroupByName(adminCtx, groupName);

        assertEquals(userGroupCreated.getGroupId(), userGroupGetByName.getGroupId(),
                "Responded user group ids are not the same!");

        // delete user group
        userGroupApi.deleteUserGroup(adminCtx, userGroupCreated.getGroupId());

        // check, if user group was deleted
        try {
            userGroupApi.getUserGroup(adminCtx, userGroupCreated.getGroupId());
            fail("Expected parsing error since user group should be null");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Usergroup with this id does not exist"), "Unrelated exception.");
        }
    }

    @SeleniumTestCase
    public void testMetaPermissionCheck() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);

        // add test users
        final SecurityApi securityApi = new SecurityApi();
        final String addingUsername = "adding user";
        final String addedUsername = "added user";

        securityApi.createUser(adminCtx, addingUsername, "test", "company", ANOTHER_TEST_PASSWORD);
        securityApi.createUser(adminCtx, addedUsername, "test", "company", ANOTHER_TEST_PASSWORD);

        final ApiContext userCtx = ApiContext.createApiContext(getContextRoot(), SECURITY_CONTEXT, addingUsername,
                ANOTHER_TEST_PASSWORD);

        // create user group
        final UserGroup userGroupCreated = userGroupApi.createUserGroup(adminCtx, "test-group-01");
        assertNotNull(userGroupCreated.getGroupId(), "GroupId is missing in reponse!");

        // add new role to group with admin user
        final RoleDefinition createdRole = roleApi.createRole(adminCtx, "My-Epic-Role");
        roleApi.updateRole(adminCtx, createdRole.getId(), Arrays.asList("*"), createdRole.getName());
        userGroupApi.addRoleToGroup(adminCtx, userGroupCreated.getGroupId(), createdRole.getId(), false);

        try {

            // add user to group with a user who does not have one of the roles implicated by the group
            userGroupApi.addUserToGroup(userCtx, userGroupCreated.getGroupId(), addedUsername);

            fail("Expected unauthorized exception.");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("failed (rc=401)"), "Expected unauthorized exception");
        }
    }

    @SeleniumTestCase
    public void testGetReadableUserGroups() {
        final ApiContext adminSecurityCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);
        final String user1Name = "user1";
        securityApi.createUser(adminSecurityCtx, user1Name, "test", "company", ANOTHER_TEST_PASSWORD);
        final ApiContext user1Ctx = ApiContext.createApiContext(getContextRoot(), SECURITY_CONTEXT, user1Name,
                ANOTHER_TEST_PASSWORD);
        final ApiContext user1SecurityCtx = ApiContext.createApiContext(getContextRoot(), SECURITY_CONTEXT, user1Name,
                ANOTHER_TEST_PASSWORD);
        assertEquals(1, Util.size(userGroupApi.getReadableGroupsOfUser(user1Ctx, user1Name)));

        // admin creates new group and adds user -> does not mean the user is allowed to read the group
        final LongAdder counter = new LongAdder();
        final UserGroup group1ToAdd = userGroupApi.createUserGroup(adminSecurityCtx, "group1ToAdd");
        userGroupApi.addUserToGroup(adminSecurityCtx, group1ToAdd.getGroupId(), user1Name);
        userGroupApi.getReadableGroupsOfUser(user1Ctx, user1Name).forEach(ug -> {
            if (group1ToAdd.getGroupId().equals(ug.getGroupId())) {
                counter.increment();
            }
        });
        assertEquals(0, counter.intValue());

        // user1 creates new group -> this group is readable by user1
        counter.reset();
        final UserGroup group2ToAdd = userGroupApi.createUserGroup(user1SecurityCtx, "group2ToAdd");
        userGroupApi.getReadableGroupsOfUser(user1Ctx, user1Name).forEach(ug -> {
            if (group2ToAdd.getGroupId().equals(ug.getGroupId())) {
                counter.increment();
            }
        });
        assertEquals(1, counter.intValue());

        // if passing null for the username, implying the current user, should return the same result
        counter.reset();
        userGroupApi.getReadableGroupsOfCurrentUser(user1Ctx).forEach(ug -> {
            if (group2ToAdd.getGroupId().equals(ug.getGroupId())) {
                counter.increment();
            }
        });
        assertEquals(1, counter.intValue());
    }

    @SeleniumTestCase
    public void testChangeUsersInUserGroup() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);

        // create user group
        final String groupName = "test-group-01";
        final UserGroup userGroupCreated = userGroupApi.createUserGroup(adminCtx, groupName);
        assertNotNull(userGroupCreated.getGroupId(), "GroupId is missing in reponse!");

        // add test user
        final SecurityApi securityApi = new SecurityApi();
        final String username = "epicTestUser1337";
        try {
            securityApi.getUser(adminCtx, username);

        } catch (RuntimeException e) {
            securityApi.createUser(adminCtx, username, "test", "company", ANOTHER_TEST_PASSWORD);
        }

        // add user to group
        userGroupApi.addUserToGroup(adminCtx, userGroupCreated.getGroupId(), username);

        // check, if user was properly added and group was not broken
        final UserGroup userGroupAfterUserAdd = userGroupApi.getUserGroup(adminCtx, userGroupCreated.getGroupId());
        assertNotNull(userGroupAfterUserAdd.getGroupId(), "GroupId is missing in reponse!");
        assertEquals(groupName, userGroupAfterUserAdd.getGroupName(),
                "Responded username of createUserGroup is different!");
        assertSameElements(userGroupCreated.getRoles(), userGroupAfterUserAdd.getRoles());
        assertTrue(Util.contains(userGroupAfterUserAdd.getUsers(), username), "Added user is missing!");

        // remove user from group
        userGroupApi.removeUserFromGroup(adminCtx, userGroupCreated.getGroupId(), username);

        // check, if user was properly removed and group is the same as before adding
        final UserGroup userGroupAfterUserRemove = userGroupApi.getUserGroup(adminCtx, userGroupCreated.getGroupId());
        assertNotNull(userGroupAfterUserRemove.getGroupId(), "GroupId is missing in reponse!");
        assertEquals(groupName, userGroupAfterUserRemove.getGroupName(),
                "Responded username of createUserGroup is different!");
        assertSameElements(userGroupCreated.getRoles(), userGroupAfterUserRemove.getRoles());
        assertSameElements(userGroupCreated.getUsers(), userGroupAfterUserRemove.getUsers());
    }

    @SeleniumTestCase
    public void setDefaultTenantForCurrentServerAndUserTest() {
        final String defaultTenantGroup = "NewGroup";
        final String eventName = "testevent";
        final String eventName2 = "testevent2";
        final ApiContext adminSecurityCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);
        securityApi.createUser(adminSecurityCtx, "donald", "Donald Duck", null, DONALDS_PASSWORD);
        final ApiContext ownerCtx = createApiContext(getContextRoot(), SERVER_CONTEXT, "donald", DONALDS_PASSWORD);
        final ApiContext ownerSecurityCtx = createApiContext(getContextRoot(), SECURITY_CONTEXT, "donald", DONALDS_PASSWORD);
        final AdminConsolePage adminConsole = goToPage(getWebDriver(), getContextRoot());
        adminConsole.goToLocalServerPanel().setSelfServiceServer(true);

        final EventApi eventApi = new EventApi();
        final Event eventCreatedWithDefaultTenant = eventApi.createEvent(ownerCtx, eventName, "GC 32",
                CompetitorRegistrationType.CLOSED, "somewhere");
        assertEquals("testevent", eventCreatedWithDefaultTenant.getName());

        final UserGroup newUserGroup = userGroupApi.createUserGroup(adminSecurityCtx, defaultTenantGroup);
        userGroupApi.setDefaultTenantForCurrentServerAndUser(ownerSecurityCtx, newUserGroup.getGroupId());

        final Event eventCreatedWithNewGroupTenant = eventApi.createEvent(ownerCtx, eventName2, "GC 32",
                CompetitorRegistrationType.CLOSED, "somewhere");
        assertEquals(eventName2, eventCreatedWithNewGroupTenant.getName());
        final String groupOfEvent = adminConsole.goToEvents()
                .refreshEvents()
                .getEventEntry(eventName2)
                .getColumnContent("Group");
        assertEquals(defaultTenantGroup, groupOfEvent);
    }

    @SeleniumTestCase
    public void addUserToOwnGroupWithoutPermissionOnUserTest() {
        final ApiContext adminSecurityCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);
        final String userToAdd = "usertoadd";
        securityApi.createUser(adminSecurityCtx, "groupowner", "groupowner", null, DONALDS_PASSWORD);
        securityApi.createUser(adminSecurityCtx, userToAdd, "", null, DONALDS_PASSWORD);

        final ApiContext groupownerSecurityCtx = createApiContext(getContextRoot(), SECURITY_CONTEXT, "groupowner",
                DONALDS_PASSWORD);
        // create group owned by "groupowner"
        final UserGroup privateUserGroup = userGroupApi.createUserGroup(groupownerSecurityCtx, "mygroup");

        // add user "usertoadd" to private user group
        final ApiContext groupownerCtx = createApiContext(getContextRoot(), SECURITY_CONTEXT, "groupowner", DONALDS_PASSWORD);
        userGroupApi.addUserToUserGroupWithoutPermissionOnUser(groupownerCtx, userToAdd,
                privateUserGroup.getGroupId());
        final UserGroup privateUserGroupToCheck = userGroupApi.getUserGroup(adminSecurityCtx,
                privateUserGroup.getGroupId());
        boolean userExistsInGroup = false;
        for (final String user : privateUserGroupToCheck.getUsers()) {
            userExistsInGroup = userExistsInGroup || user.equals(userToAdd);
        }
        assertTrue(userExistsInGroup, "User does not exist in group");

        // create group by admin and try to add user to it
        final UserGroup adminUserGroup = userGroupApi.createUserGroup(adminSecurityCtx, "admingroup");
        try {
            userGroupApi.addUserToUserGroupWithoutPermissionOnUser(groupownerCtx, "usertoadd",
                    adminUserGroup.getGroupId());
            fail();
        } catch (HttpException he) {
            assertEquals(401, he.getHttpStatusCode());
        }

        // try to add user again to privateUserGroup
        try {
            userGroupApi.addUserToUserGroupWithoutPermissionOnUser(groupownerCtx, "usertoadd",
                    privateUserGroup.getGroupId());
            fail();
        } catch (HttpException he) {
            assertEquals(400, he.getHttpStatusCode());
        }
    }

    @SeleniumTestCase
    public void changeRolesAssociatedToUserGroup() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);

        // create user group
        final String groupName = "test-group-01";
        final UserGroup userGroupCreated = userGroupApi.createUserGroup(adminCtx, groupName);
        assertNotNull(userGroupCreated.getGroupId(), "GroupId is missing in reponse!");

        // create test role
        final RoleApi roleApi = new RoleApi();
        final RoleDefinition role = roleApi.createRole(adminCtx, "test-role-01");
        assertNotNull(role.getId(), "RoleId is missing in response!");

        // add role to group
        userGroupApi.addRoleToGroup(adminCtx, userGroupCreated.getGroupId(), role.getId(), true);

        // check, if role was added properly and that the group was not broken in the process
        final UserGroup userGroupAfterRoleAdd = userGroupApi.getUserGroup(adminCtx, userGroupCreated.getGroupId());
        assertNotNull(userGroupAfterRoleAdd.getGroupId(), "GroupId is missing in reponse!");
        assertEquals(groupName, userGroupAfterRoleAdd.getGroupName(),
                "Responded username of createUserGroup is different!");
        assertSameElements(userGroupCreated.getUsers(), userGroupAfterRoleAdd.getUsers());
        assertTrue(Util.contains(userGroupAfterRoleAdd.getRoles(), role), "Added user is missing!");

        // remove the role from the group
        userGroupApi.removeRoleFromGroup(adminCtx, userGroupCreated.getGroupId(), role.getId());
        roleApi.deleteRole(adminCtx, role.getId());

        // check, if role was properly removed and group is the same as before adding
        final UserGroup userGroupAfterRoleRemove = userGroupApi.getUserGroup(adminCtx, userGroupCreated.getGroupId());
        assertNotNull(userGroupAfterRoleRemove.getGroupId(), "GroupId is missing in reponse!");
        assertEquals(groupName, userGroupAfterRoleRemove.getGroupName(),
                "Responded username of createUserGroup is different!");
        assertSameElements(userGroupCreated.getRoles(), userGroupAfterRoleRemove.getRoles());
        assertSameElements(userGroupCreated.getUsers(), userGroupAfterRoleRemove.getUsers());
    }

    /** |A| = |B| ∧ ∀a∈ A: a∈ B --> A and B have the same elements, ignoring order. */
    private <T> void assertSameElements(Iterable<T> a, Iterable<T> b) {
        assertEquals(Util.size(a), Util.size(b), "Element count changed!");
        for (T elem : b) {
            assertTrue(Util.contains(a, elem), "Element is missing!");
        }
    }
}
