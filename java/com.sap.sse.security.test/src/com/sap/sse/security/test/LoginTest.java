package com.sap.sse.security.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.SecurityUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;
import com.sap.sse.common.Util;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.impl.Activator;
import com.sap.sse.security.impl.SecurityServiceImpl;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.shared.AdminRole;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.PermissionChecker;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.LockingAndBanningImpl;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.QualifiedObjectIdentifierImpl;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.impl.UserGroupImpl;
import com.sap.sse.security.shared.subscription.SSESubscriptionPlan;
import com.sap.sse.security.userstore.mongodb.AccessControlStoreImpl;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;
import com.sap.sse.security.userstore.mongodb.impl.CollectionNames;

public class LoginTest {
    private static final String DEFAULT_TENANT_NAME = "TestDefaultTenant";
    private UserStoreImpl userStore;
    private AccessControlStore accessControlStore;
    private SecurityService securityService;

    @BeforeEach
    public void setUp() throws UnknownHostException, MongoException, UserGroupManagementException, UserManagementException {
        final MongoDBConfiguration dbConfiguration = MongoDBConfiguration.getDefaultTestConfiguration();
        final MongoDBService service = dbConfiguration.getService();
        MongoDatabase db = service.getDB();
        db.getCollection(CollectionNames.USERS.name()).drop();
        db.getCollection(CollectionNames.USER_GROUPS.name()).drop();
        db.getCollection(CollectionNames.ROLES.name()).drop();
        db.getCollection(CollectionNames.SETTINGS.name()).drop();
        db.getCollection(CollectionNames.PREFERENCES.name()).drop();
        db.getCollection(com.sap.sse.security.persistence.impl.CollectionNames.SESSIONS.name()).drop();
        userStore = new UserStoreImpl(DEFAULT_TENANT_NAME);
        userStore.ensureDefaultRolesExist();
        userStore.ensureServerGroupExists();
        accessControlStore = new AccessControlStoreImpl(userStore);
        Activator.setTestStores(userStore, accessControlStore);
        // enables shiro to find classes from com.sap.sse.security
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        securityService = new SecurityServiceImpl(/* mailServiceTracker */ null, /* corsFilterConfigurationTracker */ null, userStore,
                accessControlStore, SecuredSecurityTypes::getAllInstances, SSESubscriptionPlan::getAllInstances);
        Activator.setSecurityService(securityService);
    }

    @Test
    public void testGetUsersWithPermission() throws UserManagementException, UserGroupManagementException, MailException {
        final String username = "TheNewUser";
        final String specialUserGroupName1 = "TheSpecialUserGroup1";
        final String specialUserGroupName2 = "TheSpecialUserGroup2";
        final User user = securityService.createSimpleUser(username, "u@a.b", "Humba", "The New User", /* company */ null, /* locale */ null, /* validationBaseURL */ null, /* owning group */ null, /* clientIP */ null, /* enforce strong password */ false);
        final TypeRelativeObjectIdentifier myServerTypeRelativeObjectIdentifier = new TypeRelativeObjectIdentifier("myserver");
        final WildcardPermission createObjectPermissionOnMyserver = SecuredSecurityTypes.SERVER.getPermissionForTypeRelativeIdentifier(ServerActions.CREATE_OBJECT,
                myServerTypeRelativeObjectIdentifier);
        assertFalse(Util.contains(securityService.getUsersWithPermissions(createObjectPermissionOnMyserver), user));
        securityService.addPermissionForUser(username, createObjectPermissionOnMyserver);
        assertTrue(Util.contains(securityService.getUsersWithPermissions(createObjectPermissionOnMyserver), user));
        securityService.removePermissionFromUser(username, createObjectPermissionOnMyserver);
        assertFalse(Util.contains(securityService.getUsersWithPermissions(createObjectPermissionOnMyserver), user));
        final UserGroup specialUserGroup1 = securityService.createUserGroup(UUID.randomUUID(), specialUserGroupName1);
        final RoleDefinition roleGrantingPermission = securityService.createRoleDefinition(UUID.randomUUID(), "roleGrantingPermission");
        roleGrantingPermission.setPermissions(Collections.singleton(createObjectPermissionOnMyserver));
        securityService.updateRoleDefinition(roleGrantingPermission);
        specialUserGroup1.put(roleGrantingPermission, /* forAll */ false);
        final QualifiedObjectIdentifier myServerObjectIdentifier = SecuredSecurityTypes.SERVER.getQualifiedObjectIdentifier(myServerTypeRelativeObjectIdentifier);
        securityService.setOwnership(myServerObjectIdentifier, /* user */ null, specialUserGroup1);
        assertFalse(Util.contains(securityService.getUsersWithPermissions(createObjectPermissionOnMyserver), user));
        securityService.addUserToUserGroup(specialUserGroup1, user);
        // now since the user belongs to the group and the group implies the permission and the myserver objetc is owned by the group, this shall imply the permission for user
        assertTrue(Util.contains(securityService.getUsersWithPermissions(createObjectPermissionOnMyserver), user));
        final UserGroup specialUserGroup2 = securityService.createUserGroup(UUID.randomUUID(), specialUserGroupName2);
        // change group ownership to a group that doesn't imply the permission:
        securityService.setOwnership(myServerObjectIdentifier, /* user */ null, specialUserGroup2);
        assertFalse(Util.contains(securityService.getUsersWithPermissions(createObjectPermissionOnMyserver), user));
    }
    
    @Test
    public void testDeleteUser() throws UserManagementException, MailException, UserGroupManagementException {
        final String username = "TheNewUser";
        final String specialUserGroupName1 = "TheSpecialUserGroup1";
        final String specialUserGroupName2 = "TheSpecialUserGroup2";
        final User user = securityService.createSimpleUser(username, "u@a.b", "Humba", "The New User", /* company */ null, /* locale */ null, /* validationBaseURL */ null, /* owning group */ null, /* clientIP */ null, /* enforce strong password */ false);
        final UserGroup defaultUserGroup = securityService.getUserGroupByName(username+SecurityService.TENANT_SUFFIX);
        final UserGroup specialUserGroup1 = securityService.createUserGroup(UUID.randomUUID(), specialUserGroupName1);
        final UserGroup specialUserGroup2 = securityService.createUserGroup(UUID.randomUUID(), specialUserGroupName2);
        securityService.addUserToUserGroup(specialUserGroup1, user);
        securityService.addUserToUserGroup(specialUserGroup2, user);
        assertNotNull(defaultUserGroup);
        assertTrue(Util.contains(defaultUserGroup.getUsers(), user));
        securityService.deleteUser(username);
        assertNull(securityService.getUserByName(username));
        assertFalse(Util.contains(specialUserGroup1.getUsers(), user));
        assertFalse(Util.contains(specialUserGroup2.getUsers(), user));
    }

    @Test
    public void testAclAnonUserGroup() throws UserManagementException, MailException, UserGroupManagementException {
        final String username = "TheNewUser";
        securityService.createSimpleUser(username, "u@a.b", "Humba", username, /* company */ null,
                /* locale */ null, /* validationBaseURL */ null, /* owning group */ null, /* clientIP */ null, /* enforce strong password */ false);
        final UserGroup defaultUserGroup = securityService.getUserGroupByName(username + SecurityService.TENANT_SUFFIX);
        Map<UserGroup, Set<String>> permissionMap = new HashMap<>();
        permissionMap.put(defaultUserGroup, new HashSet<>(Arrays.asList(new String[] { "!READ", "UPDATE" })));
        permissionMap.put(null, new HashSet<>(Arrays.asList(new String[] { "!READ", "UPDATE" })));
        AccessControlList acl = securityService.overrideAccessControlList(
                QualifiedObjectIdentifierImpl.fromDBWithoutEscaping("someid/more"), permissionMap);
        Map<UserGroup, Set<String>> result = acl.getActionsByUserGroup();
        assertThat(result.get(defaultUserGroup), Matchers.contains("!READ", "UPDATE"));
        assertThat(result.get(null), Matchers.contains("UPDATE"));
    }
    
    @Test
    public void testDenialInAclAffectsMetaPermissionCheck() throws UserManagementException, MailException, UserGroupManagementException {
        final WithQualifiedObjectIdentifier my = new WithQualifiedObjectIdentifier() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getName() {
                return "my";
            }
            
            @Override
            public HasPermissions getPermissionType() {
                return SecuredSecurityTypes.SERVER;
            }
            
            @Override
            public QualifiedObjectIdentifier getIdentifier() {
                return getPermissionType().getQualifiedObjectIdentifier(new TypeRelativeObjectIdentifier(getName()));
            }
        };
        securityService.initialize(); // create admin user/tenant
        final String username = "TheNewUser";
        final String password = "Humba";
        final User admin = securityService.getUserByName("admin");
        final RoleDefinition adminRoleDefinition = securityService.getOrCreateRoleDefinitionFromPrototype(AdminRole.getInstance(), /* makeReadableForAll */ true);
        final UserGroup adminTenant = securityService.getUserGroupByName(admin.getName()+SecurityService.TENANT_SUFFIX);
        securityService.createSimpleUser(username, "u@a.b", password, username, /* company */ null,
                /* locale */ null, /* validationBaseURL */ null, /* owning group */ null, /* clientIP */ null, /* enforce strong password */ false);
        final UserGroup defaultUserGroup = securityService.getUserGroupByName(username + SecurityService.TENANT_SUFFIX);
        final QualifiedObjectIdentifier myId = my.getIdentifier();
        // grant admin role to user unqualified, implying READ on all objects including the "my" SERVER
        securityService.addRoleForUser(username, new Role(adminRoleDefinition, true));
        securityService.login(username, password);
        securityService.setOwnership(myId, admin, adminTenant);
        // check explicit permission:
        assertTrue(securityService.hasCurrentUserExplicitPermissions(my, ServerActions.READ_REPLICATOR));
        // check for meta-permission SERVER:*:*
        assertTrue(securityService.hasCurrentUserMetaPermissionWithOwnershipLookup(
                WildcardPermission.builder().withTypes(SecuredSecurityTypes.SERVER).build()));
        // now add an ACL to the "my" server that disallows READ_REPLICATOR for the user's default group
        Map<UserGroup, Set<String>> permissionMap = new HashMap<>();
        permissionMap.put(defaultUserGroup, new HashSet<>(Arrays.asList(new String[] { "!READ_REPLICATOR" })));
        securityService.overrideAccessControlList(myId, permissionMap);
        // now the user is expected to have lost the READ_REPLICATOR permission on "my"
        assertFalse(securityService.hasCurrentUserExplicitPermissions(my, ServerActions.READ_REPLICATOR));
        // and therefore no meta-permission (permission to grant) for SERVER:*:* anymore:
        assertFalse(securityService.hasCurrentUserMetaPermissionWithOwnershipLookup(
                WildcardPermission.builder().withTypes(SecuredSecurityTypes.SERVER).build()));
        // and therefore no meta-permission (permission to grant) for SERVER:*:my anymore:
        assertFalse(securityService.hasCurrentUserMetaPermissionWithOwnershipLookup(
                WildcardPermission.builder().withTypes(SecuredSecurityTypes.SERVER).withIds(myId.getTypeRelativeObjectIdentifier()).build()));
        // The current user belongs to defaultUserGroup for which READ_REPLICATOR has been denied on SERVER/my. However,
        // the SERVER/my object is owned by the admin-tenant group. Wanting to grant a role qualified to the defaultUserGroup
        // would entail permissions only for objects owned by the defaultUserGroup, hence not for SERVER/my; so the
        // denial of READ_REPLICATOR shouldn't matter for granting the "admin" role qualified to objects owned by
        // group defaultUserGroup:
        assertTrue(securityService.hasCurrentUserMetaPermissionsOfRoleDefinitionWithQualification(
                adminRoleDefinition, new Ownership(/* user */ null, defaultUserGroup)));
        // granting role "admin" qualified to the "admin-tenant" group, however, should not be allowed
        // because a denying ACL entry exists for the SERVER/my object owned by the "admin-tenant" group that
        // denies READ_REPLICATOR for users who belong to group defaultUserGroup.
        // (As a matter of fact, granting this role would be possible if the receiver of the role did *not*
        // belong to the defaultUserGroup...)
        assertFalse(securityService.hasCurrentUserMetaPermissionsOfRoleDefinitionWithQualification(
                adminRoleDefinition, new Ownership(/* user */ null, adminTenant)));
    }
    
    @Test
    public void testGetUser() {
        assertNotNull(SecurityUtils.getSubject(), "Subject should not be null: ");
    }
    
    @Test
    public void setPreferencesTest() throws UserStoreManagementException {
        userStore.setPreference("me", "key", "value");
        UserStoreImpl store2 = new UserStoreImpl(DEFAULT_TENANT_NAME);
        assertEquals("value", store2.getPreference("me", "key"));
    }

    @Test
    public void setAndUnsetPreferencesTest() throws UserStoreManagementException {
        userStore.setPreference("me", "key", "value");
        userStore.unsetPreference("me", "key");
        UserStoreImpl store2 = new UserStoreImpl(DEFAULT_TENANT_NAME);
        assertNull(store2.getPreference("me", "key"));
    }

    @Test
    public void rolesTest() throws UserStoreManagementException {
        userStore.createUser("me", "me@sap.com", new LockingAndBanningImpl());
        RoleDefinition testRoleDefinition = userStore.createRoleDefinition(UUID.randomUUID(), "testRole",
                Collections.emptySet());
        final Role testRole = new Role(testRoleDefinition, true);
        userStore.addRoleForUser("me", testRole);
        UserStoreImpl store2 = createAndLoadUserStore();
        assertTrue(Util.contains(store2.getUserByName("me").getRoles(), testRole));
    }

    @Test
    public void roleWithQualifiersTest() throws UserStoreManagementException {
        UserGroupImpl userDefaultTenant = userStore.createUserGroup(UUID.randomUUID(), "me-tenant");
        User meUser = userStore.createUser("me", "me@sap.com", new LockingAndBanningImpl());
        RoleDefinition testRoleDefinition = userStore.createRoleDefinition(UUID.randomUUID(), "testRole",
                Collections.emptySet());
        final Role testRole = new Role(testRoleDefinition, userDefaultTenant, meUser, true);
        userStore.addRoleForUser("me", testRole);
        UserStoreImpl store2 = createAndLoadUserStore();
        assertTrue(Util.contains(store2.getUserByName("me").getRoles(), testRole));
        Role role2 = store2.getUserByName("me").getRoles().iterator().next();
        assertSame(store2.getUserGroupByName("me-tenant"), role2.getQualifiedForTenant());
        assertSame(store2.getUserByName("me"), role2.getQualifiedForUser());
    }

    @Test
    public void permissionsTest() throws UserStoreManagementException {
        userStore.createUser("me", "me@sap.com", new LockingAndBanningImpl());
        userStore.addPermissionForUser("me", new WildcardPermission("a:b:c"));
        UserStoreImpl store2 = createAndLoadUserStore();
        User allUser = userStore.getUserByName(SecurityService.ALL_USERNAME);
        User user = store2.getUserByName("me");
        assertTrue(PermissionChecker.isPermitted(new WildcardPermission("a:b:c"), user, allUser, null, null));
    }

    private UserStoreImpl createAndLoadUserStore() throws UserStoreManagementException {
        final UserStoreImpl store = new UserStoreImpl(DEFAULT_TENANT_NAME);
        store.loadAndMigrateUsers();
        return store;
    }

}
