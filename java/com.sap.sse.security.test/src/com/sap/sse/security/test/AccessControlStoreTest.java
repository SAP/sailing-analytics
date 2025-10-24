package com.sap.sse.security.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserImpl;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.LockingAndBanningImpl;
import com.sap.sse.security.shared.impl.QualifiedObjectIdentifierImpl;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.impl.UserGroupImpl;
import com.sap.sse.security.userstore.mongodb.AccessControlStoreImpl;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;
import com.sap.sse.security.userstore.mongodb.impl.CollectionNames;

/**
 * Tests that the MongoDB persistence is always in sync with the {@link UserStore}.
 */
public class AccessControlStoreTest {
    private static final String DEFAULT_TENANT_NAME = "TestDefaultTenant";
    private final QualifiedObjectIdentifier testId = new QualifiedObjectIdentifierImpl("Test", new TypeRelativeObjectIdentifier("test"));
    private final String testDisplayName = "testDN";

    private final UserGroup testTenantOwner = new UserGroupImpl(UUID.randomUUID(), "test-tenant");
    private final UUID testRoleId = UUID.randomUUID();

    private UserStore userStore;
    private AccessControlStore accessControlStore;
    private User testOwner;
    private UserGroup adminTenant;

    @BeforeEach
    public void setUp() throws UnknownHostException, MongoException, UserGroupManagementException {
        final MongoDBConfiguration dbConfiguration = MongoDBConfiguration.getDefaultTestConfiguration();
        final MongoDBService service = dbConfiguration.getService();
        MongoDatabase db = service.getDB();
        db.getCollection(CollectionNames.ACCESS_CONTROL_LISTS.name()).drop();
        db.getCollection(CollectionNames.OWNERSHIPS.name()).drop();
        db.getCollection(CollectionNames.ROLES.name()).drop();
        db.getCollection(CollectionNames.USER_GROUPS.name()).drop();
        db.getCollection(CollectionNames.USERS.name()).drop();
        newStores();
        adminTenant = new UserGroupImpl(UUID.randomUUID(), "admin-tenant");
        Map<String, UserGroup> defaultTenantForUser = new HashMap<>();
        defaultTenantForUser.put("dummyServer", adminTenant);
        testOwner = new UserImpl("admin", "admin@sapsailing.com", defaultTenantForUser,
                /* userGroupProvider */ null, new LockingAndBanningImpl());
    }

    private void newStores() {
        try {
            userStore = new UserStoreImpl(DEFAULT_TENANT_NAME);
            userStore.loadAndMigrateUsers();
            userStore.ensureDefaultRolesExist();
            userStore.ensureServerGroupExists();
        } catch (UserStoreManagementException e) {
            throw new RuntimeException(e);
        }
        accessControlStore = new AccessControlStoreImpl(userStore);
        accessControlStore.loadACLsAndOwnerships();
    }

    @Test
    public void testCreateAccessControlList() throws UserGroupManagementException {
        accessControlStore.setEmptyAccessControlList(testId, testDisplayName);
        assertNotNull(accessControlStore.getAccessControlList(testId));
        newStores();
        assertNotNull(accessControlStore.getAccessControlList(testId));
    }
    
    @Test
    public void testCreateSimpleAccessControlListForAll() throws UserGroupManagementException {
        accessControlStore.addAclPermission(testId, null, "READ");
        assertNotNull(accessControlStore.getAccessControlList(testId));
        assertTrue(accessControlStore.getAccessControlList(testId).getAnnotation().getActionsByUserGroup().get(null).contains("READ"));
        newStores();
        assertNotNull(accessControlStore.getAccessControlList(testId));
        assertTrue(accessControlStore.getAccessControlList(testId).getAnnotation().getActionsByUserGroup().get(null).contains("READ"));
    }
    
    @Test
    public void testSetEmptyAccessControlListToReplaceExistingPositive() throws UserGroupManagementException {
        accessControlStore.addAclPermission(testId, adminTenant, "READ"); // a positive, allowing ACL entry
        // expecting not to find anything in the denying structure:
        assertTrue(accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()) == null || accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()).isEmpty());
        // but a record should exist in the per-group ACLs:
        assertNotNull(accessControlStore.getAccessControlListsForGroup(adminTenant));
        assertEquals(1, accessControlStore.getAccessControlListsForGroup(adminTenant).size());
        accessControlStore.setEmptyAccessControlList(testId, "The test object's ACL");
        assertTrue(accessControlStore.getAccessControlListsForGroup(adminTenant) == null || accessControlStore.getAccessControlListsForGroup(adminTenant).isEmpty());
        assertTrue(accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()) == null || accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()).isEmpty());
    }
    
    @Test
    public void testSetActionsOnAclToReplaceExistingPositive() throws UserGroupManagementException {
        accessControlStore.addAclPermission(testId, adminTenant, "READ"); // a positive, allowing ACL entry
        // expecting not to find anything in the denying structure:
        assertTrue(accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()) == null || accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()).isEmpty());
        // but a record should exist in the per-group ACLs:
        assertNotNull(accessControlStore.getAccessControlListsForGroup(adminTenant));
        assertEquals(1, accessControlStore.getAccessControlListsForGroup(adminTenant).size());
        accessControlStore.setAclPermissions(testId, adminTenant, Collections.singleton("!READ"));
        // now we should still see a per-group entry, but now additionally the denial-by-type-and-group should have an entry, too:
        assertNotNull(accessControlStore.getAccessControlListsForGroup(adminTenant));
        assertEquals(1, accessControlStore.getAccessControlListsForGroup(adminTenant).size());
        final Set<QualifiedObjectIdentifier> objectIdsWithNegativeACL = accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()).get(adminTenant);
        assertEquals(1, objectIdsWithNegativeACL.size());
        assertEquals(testId, objectIdsWithNegativeACL.iterator().next());
    }
    
    @Test
    public void testSetEmptyAccessControlListToReplaceExistingNegative() throws UserGroupManagementException {
        accessControlStore.addAclPermission(testId, adminTenant, "!READ"); // a negative, denying ACL entry
        // expecting to find something in the denying structure:
        assertNotNull(accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()));
        final Set<QualifiedObjectIdentifier> objectIdsWithNegativeACL = accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()).get(adminTenant);
        assertEquals(1, objectIdsWithNegativeACL.size());
        assertEquals(testId, objectIdsWithNegativeACL.iterator().next());
        assertEquals("!READ", accessControlStore.getAccessControlList(testId).getAnnotation().getActionsByUserGroup().get(adminTenant).iterator().next());
        // a record should also exist in the per-group ACLs:
        assertNotNull(accessControlStore.getAccessControlListsForGroup(adminTenant));
        assertEquals(1, accessControlStore.getAccessControlListsForGroup(adminTenant).size());
        accessControlStore.setEmptyAccessControlList(testId, "The test object's ACL");
        assertTrue(accessControlStore.getAccessControlListsForGroup(adminTenant) == null || accessControlStore.getAccessControlListsForGroup(adminTenant).isEmpty());
        assertTrue(accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()) == null || accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()).isEmpty());
    }
    
    @Test
    public void testSetActionsToReplaceExistingNegative() throws UserGroupManagementException {
        accessControlStore.addAclPermission(testId, adminTenant, "!READ"); // a negative, denying ACL entry
        // expecting to find something in the denying structure:
        assertNotNull(accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()));
        final Set<QualifiedObjectIdentifier> objectIdsWithNegativeACL = accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()).get(adminTenant);
        assertEquals(1, objectIdsWithNegativeACL.size());
        assertEquals(testId, objectIdsWithNegativeACL.iterator().next());
        assertEquals("!READ", accessControlStore.getAccessControlList(testId).getAnnotation().getActionsByUserGroup().get(adminTenant).iterator().next());
        // a record should also exist in the per-group ACLs:
        assertNotNull(accessControlStore.getAccessControlListsForGroup(adminTenant));
        assertEquals(1, accessControlStore.getAccessControlListsForGroup(adminTenant).size());
        accessControlStore.setAclPermissions(testId, adminTenant, Collections.singleton("READ"));
        // still expect a record for the group
        assertNotNull(accessControlStore.getAccessControlListsForGroup(adminTenant));
        assertEquals(1, accessControlStore.getAccessControlListsForGroup(adminTenant).size());
        // but not for the denials
        assertTrue(accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()) == null || accessControlStore.getAccessControlListsWithDenials(testId.getTypeIdentifier()).isEmpty());
    }
    
    @Test
    public void testCreateSimpleAccessControlListForGroup() throws UserGroupManagementException {
        final UserGroup group = userStore.getServerGroup();
        accessControlStore.addAclPermission(testId, group, "READ");
        assertNotNull(accessControlStore.getAccessControlList(testId));
        assertTrue(accessControlStore.getAccessControlList(testId).getAnnotation().getActionsByUserGroup().get(group).contains("READ"));
        newStores();
        assertNotNull(accessControlStore.getAccessControlList(testId));
        final UserGroup newGroup = userStore.getServerGroup();
        assertTrue(accessControlStore.getAccessControlList(testId).getAnnotation().getActionsByUserGroup().get(newGroup).contains("READ"));
    }
    
    @Test
    public void testDeleteAccessControlList() throws UserGroupManagementException {
        accessControlStore.setEmptyAccessControlList(testId, testDisplayName);
        accessControlStore.removeAccessControlList(testId);
        assertNull(accessControlStore.getAccessControlList(testId));
        newStores();
        assertNull(accessControlStore.getAccessControlList(testId));
    }
    
    @Test
    public void testSetOwnership() throws UserGroupManagementException {
        accessControlStore.setOwnership(testId, testOwner, testTenantOwner, testDisplayName);
        assertNotNull(accessControlStore.getOwnership(testId));
        newStores();
        assertNotNull(accessControlStore.getOwnership(testId));
    }
    
    @Test
    public void testDeleteOwnership() throws UserGroupManagementException {
        accessControlStore.setOwnership(testId, testOwner, testTenantOwner, testDisplayName);
        accessControlStore.removeOwnership(testId);
        // expecting to be unowned
        assertTrue(accessControlStore.getOwnership(testId) == null);
        newStores();
        assertTrue(accessControlStore.getOwnership(testId) == null);
    }

    @Test
    public void testCreateRole() throws UserGroupManagementException {
        userStore.createRoleDefinition(testRoleId, testDisplayName, new HashSet<WildcardPermission>());
        assertNotNull(userStore.getRoleDefinition(testRoleId));
        newStores();
        assertNotNull(userStore.getRoleDefinition(testRoleId));
    }

    @Test
    public void testDeleteRole() throws UserGroupManagementException {
        final RoleDefinition role = userStore.createRoleDefinition(testRoleId, testDisplayName,
                new HashSet<WildcardPermission>());
        userStore.removeRoleDefinition(role);
        assertNull(userStore.getRoleDefinition(testRoleId));
        newStores();
        assertNull(userStore.getRoleDefinition(testRoleId));
    }
}
