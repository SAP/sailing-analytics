package com.sap.sse.security.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.interfaces.UserImpl;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.RoleDefinitionImpl;
import com.sap.sse.security.shared.SecurityUser;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.impl.LockingAndBanningImpl;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.impl.UserGroupImpl;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;
import com.sap.sse.security.userstore.mongodb.impl.CollectionNames;

/**
 * Tests that the MongoDB persistence is always in sync with the {@link UserStore}.
 */
public class UserStoreWithPersistenceTest {
    private final String username = "abc";
    private final String fullName = "Arno Nym";
    private final String company = "SAP SE";
    private final String email = "anonymous@sapsailing.com";
    private final String serverName = "dummyServer";
    private final String prefKey = "pk";
    private final String prefValue = "pv";

    private final UUID userGroupId = UUID.randomUUID();
    private final String userGroupName = "usergroup";

    private UserStoreImpl store;

    @BeforeEach
    public void setUp() throws UnknownHostException, MongoException, UserGroupManagementException {
        final MongoDBConfiguration dbConfiguration = MongoDBConfiguration.getDefaultTestConfiguration();
        final MongoDBService service = dbConfiguration.getService();
        MongoDatabase db = service.getDB();
        db.getCollection(CollectionNames.ROLES.name()).drop();
        db.getCollection(CollectionNames.USERS.name()).drop();
        db.getCollection(CollectionNames.USER_GROUPS.name()).drop();
        db.getCollection(CollectionNames.ACCESS_CONTROL_LISTS.name()).drop();
        db.getCollection(CollectionNames.OWNERSHIPS.name()).drop();
        db.getCollection(CollectionNames.SETTINGS.name()).drop();
        db.getCollection(CollectionNames.PREFERENCES.name()).drop();
        newStore();
    }

    private UserGroupImpl createUserGroup() throws UserGroupManagementException {
        return store.createUserGroup(userGroupId, userGroupName);
    }

    private void newStore() {
        try {
            store = new UserStoreImpl("TestDefaultTenant");
            store.ensureDefaultRolesExist();
            store.ensureServerGroupExists();
            store.loadAndMigrateUsers();
        } catch (UserStoreManagementException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCreateUser() throws UserManagementException {
        store.createUser(username, email, new LockingAndBanningImpl());
        assertNotNull(store.getUserByName(username));
        assertNotNull(store.getUserByEmail(email));

        newStore();
        assertNotNull(store.getUserByName(username));
        assertNotNull(store.getUserByEmail(email));
    }

    @Test
    public void testMasterdataIsSaved() throws UserStoreManagementException {
        store.createUser(username, email, new LockingAndBanningImpl());
        UserGroupImpl defaultTenant = createUserGroup();
        HashMap<String, UserGroup> defaultTenantForServers = new HashMap<>();
        defaultTenantForServers.put(serverName, defaultTenant);
        store.updateUser(new UserImpl(username, email, fullName, company, Locale.GERMAN, false, null, null,
                defaultTenantForServers, Collections.emptySet(), /* userGroupProvider */ null, new LockingAndBanningImpl()));
        newStore();
        User savedUser = store.getUserByName(username);
        assertEquals(username, savedUser.getName());
        assertEquals(email, savedUser.getEmail());
        assertEquals(company, savedUser.getCompany());
        assertEquals(Locale.GERMAN, savedUser.getLocale());
    }

    /**
     * There was a {@link NullPointerException} thrown if calling getUserByEmail for an email address for which there
     * wasn't an associated user.
     */
    @Test
    public void testDeleteUser() throws UserManagementException {
        store.createUser(username, email, new LockingAndBanningImpl());
        store.deleteUser(username);
        assertNull(store.getUserByName(username));
        assertNull(store.getUserByEmail(email));

        newStore();
        assertNull(store.getUserByName(username));
        assertNull(store.getUserByEmail(email));
    }

    @Test
    public void testSetPreferences() throws UserManagementException {
        store.createUser(username, email, new LockingAndBanningImpl());
        store.setPreference(username, prefKey, prefValue);
        assertEquals(prefValue, store.getPreference(username, prefKey));
        newStore();
        assertEquals(prefValue, store.getPreference(username, prefKey));
    }

    @Test
    public void testUnsetPreferences() throws UserManagementException {
        store.createUser(username, email, new LockingAndBanningImpl());
        store.setPreference(username, prefKey, prefValue);
        store.unsetPreference(username, prefKey);
        assertNull(store.getPreference(username, prefKey));
        newStore();
        assertNull(store.getPreference(username, prefKey));
    }

    /**
     * There was a bug that caused the preferences not to be removed when a user was deleted.
     */
    @Test
    public void testDeleteUserWithPreferences() throws UserManagementException {
        store.createUser(username, email, new LockingAndBanningImpl());
        store.setPreference(username, prefKey, prefValue);
        store.deleteUser(username);
        assertNull(store.getPreference(username, prefKey));
        newStore();
        assertNull(store.getPreference(username, prefKey));
    }

    @Test
    public void testCreateUserGroup() throws UserGroupManagementException, UserManagementException {
        final User user = store.createUser(username, email, new LockingAndBanningImpl());
        UserGroupImpl createUserGroup = createUserGroup();
        createUserGroup.add(user);
        store.updateUserGroup(createUserGroup);
        assertNotNull(store.getUserGroup(userGroupId));
        assertNotNull(store.getUserGroupByName(userGroupName));

        newStore();
        assertNotNull(store.getUserGroup(userGroupId));
        assertNotNull(store.getUserGroupByName(userGroupName));
        final User loadedUser = store.getUserByName(username);
        assertTrue(Util.contains(Util.map(loadedUser.getUserGroups(), g -> g.getName()), userGroupName));
    }

    @Test
    public void testDeleteUserGroup() throws UserGroupManagementException {
        UserGroupImpl createUserGroup = createUserGroup();
        store.deleteUserGroup(createUserGroup);
        assertNull(store.getUserGroup(userGroupId));
        assertNull(store.getUserGroupByName(userGroupName));

        newStore();
        assertNull(store.getUserGroup(userGroupId));
        assertNull(store.getUserGroupByName(userGroupName));
    }

    @Test
    public void testTenantUsers() throws UserManagementException, UserGroupManagementException {
        UserGroupImpl defaultTenant = createUserGroup();
        final User user = store.createUser(username, email, new LockingAndBanningImpl());
        defaultTenant.add(user);
        store.updateUserGroup(defaultTenant);
        user.getDefaultTenantMap().put(serverName, defaultTenant);
        store.updateUser(user);
        assertSame(defaultTenant, user.getDefaultTenant(serverName));
        assertEquals(1, Util.size(defaultTenant.getUsers()));
        assertSame(user, defaultTenant.getUsers().iterator().next());
        assertEquals(1, Util.size(store.getUserGroupsOfUser(user)));
        assertSame(defaultTenant, store.getUserGroupsOfUser(user).iterator().next());
        newStore();
        final UserGroup loadedDefaultTenant = store.getUserGroupByName(defaultTenant.getName());
        final User loadedUser = store.getUserByName(username);
        assertSame(loadedDefaultTenant, loadedUser.getDefaultTenant(serverName));
        assertEquals(1, Util.size(loadedDefaultTenant.getUsers()));
        assertSame(loadedUser, loadedDefaultTenant.getUsers().iterator().next());
        assertEquals(1, Util.size(store.getUserGroupsOfUser(loadedUser)));
        assertSame(loadedDefaultTenant, store.getUserGroupsOfUser(loadedUser).iterator().next());
    }

    @Test
    public void testUserGroups() throws UserManagementException, UserGroupManagementException {
        final User user = store.createUser(username, email, new LockingAndBanningImpl());
        final String GROUP_NAME = "group";
        final UserGroupImpl group = store.createUserGroup(UUID.randomUUID(), GROUP_NAME);
        group.add(user);
        store.updateUserGroup(group);
        assertEquals(1, Util.size(group.getUsers()));
        assertEquals(1, Util.size(user.getUserGroups()));
        assertSame(group, user.getUserGroups().iterator().next());
        assertEquals(1, Util.size(store.getUserGroupsOfUser(user)));
        assertSame(group, store.getUserGroupsOfUser(user).iterator().next());
        newStore();
        final UserGroup loadedGroup = store.getUserGroupByName(GROUP_NAME);
        final User loadedUser = store.getUserByName(username);
        assertEquals(1, Util.size(loadedUser.getUserGroups()));
        assertSame(loadedGroup, loadedUser.getUserGroups().iterator().next());
        assertEquals(1, Util.size(loadedGroup.getUsers()));
        assertSame(loadedUser, loadedGroup.getUsers().iterator().next());
        assertEquals(1, Util.size(store.getUserGroupsOfUser(loadedUser)));
        assertSame(loadedGroup, store.getUserGroupsOfUser(loadedUser).iterator().next());
    }

    @Test
    public void testGetExistingQualificationsForRoleDefinition()
            throws UserManagementException, UserGroupManagementException {
        User user = store.createUser("def", "d@test.de", new LockingAndBanningImpl());
        RoleDefinitionImpl roleDefinition = new RoleDefinitionImpl(UUID.randomUUID(), "My-Test-Role");
        store.createRoleDefinition(roleDefinition.getId(), roleDefinition.getName(), new ArrayList<>());
        UserGroupImpl userGroup = store.createUserGroup(UUID.randomUUID(), "Test-Usergroup");

        // tenant is null
        testWithTenantNull(user, roleDefinition, userGroup);

        // user is null
        testWithUserNull(user, roleDefinition, userGroup);

        // tenant and user are both given (both not null)
        testWithTenantAndUserNotNull(user, roleDefinition, userGroup);

        // neither tenant nor user are given (both null)
        testWithTenantAndUserNull(user, roleDefinition);
    }

    /** Test getExistingQualificationsForRoleDefinition with both tenant and user null. */
    private void testWithTenantAndUserNull(User user, RoleDefinitionImpl roleDefinition)
            throws UserManagementException {
        Role role = new Role(roleDefinition, null, null, true);
        store.addRoleForUser(user.getName(), role);

        Iterable<Role> rolesFromUser = store.getRolesFromUser(user.getName());
        assertFalse(Util.size(rolesFromUser) == 0);
        assertTrue(Util.contains(rolesFromUser, role));
        Pair<Boolean, Set<Ownership>> result = store.getExistingQualificationsForRoleDefinition(roleDefinition);
        assertTrue(result.getA());
        assertNull(result.getB());

        store.removeRoleFromUser(user.getName(), role);
    }

    /** Test getExistingQualificationsForRoleDefinition with both tenant and user not null. */
    private void testWithTenantAndUserNotNull(User user, RoleDefinitionImpl roleDefinition, UserGroupImpl userGroup)
            throws UserManagementException {
        Role role = new Role(roleDefinition, userGroup, user, true);
        store.addRoleForUser(user.getName(), role);
        assertThatNoUserHasWildcardRole(userGroup, user, roleDefinition);
        store.removeRoleFromUser(user.getName(), role);
    }

    /** Test getExistingQualificationsForRoleDefinition with user null. */
    private void testWithUserNull(User user, RoleDefinitionImpl roleDefinition, UserGroupImpl userGroup)
            throws UserManagementException {
        Role role = new Role(roleDefinition, userGroup, null, true);
        store.addRoleForUser(user.getName(), role);
        assertThatNoUserHasWildcardRole(userGroup, user, roleDefinition);
        store.removeRoleFromUser(user.getName(), role);
    }

    /** Test getExistingQualificationsForRoleDefinition with tenant null. */
    private void testWithTenantNull(User user, RoleDefinitionImpl roleDefinition, UserGroupImpl userGroup)
            throws UserManagementException {
        Role role = new Role(roleDefinition, null, user, true);
        store.addRoleForUser(user.getName(), role);
        assertThatNoUserHasWildcardRole(userGroup, user, roleDefinition);
        store.removeRoleFromUser(user.getName(), role);
    }

    /**
     * assert that no user has a wildcard role getExistingQualificationsForRoleDefinition with both tenant and user not
     * null.
     */
    private void assertThatNoUserHasWildcardRole(UserGroupImpl userGroup, User user, RoleDefinition roleDefinition)
            throws UserManagementException {
        Iterable<Role> rolesFromUser = store.getRolesFromUser(user.getName());
        // check that role was added correctly
        assertFalse(Util.size(rolesFromUser) == 0);
        boolean containsRole = false;
        for (Role role : rolesFromUser) {
            containsRole |= role.getRoleDefinition().equals(roleDefinition);
        }
        assertTrue(containsRole);

        // check if other users have the role
        Pair<Boolean, Set<Ownership>> result = store.getExistingQualificationsForRoleDefinition(roleDefinition);
        assertFalse(result.getA());
        assertNotNull(result.getB());

        // This stream effectively executes B.getUserOwner.getName for each ownership and filters null values to avoid
        // exceptions.
        List<String> usernamesWithRole = result.getB().stream().map(Ownership::getUserOwner).filter(uo -> uo != null)
                .map(SecurityUser::getName).filter(su -> su != null).collect(Collectors.toList());

        // Similar to above just with B.getTenantOwner.getId.
        List<UUID> groupNamesWithRole = result.getB().stream().map(Ownership::getTenantOwner).filter(uo -> uo != null)
                .map(UserGroup::getId).filter(su -> su != null).collect(Collectors.toList());

        // check that either one is not false
        assertFalse(usernamesWithRole.isEmpty() && groupNamesWithRole.isEmpty());

        if (!usernamesWithRole.isEmpty()) {
            // list of usernames is not empty -> check if user name is contained in it
            assertTrue(usernamesWithRole.contains(user.getName()));
        } else if (!groupNamesWithRole.isEmpty()) {
            // list of groupids is not empty -> check if group id is contained in it
            assertTrue(groupNamesWithRole.contains(userGroup.getId()));
        }
    }
}
