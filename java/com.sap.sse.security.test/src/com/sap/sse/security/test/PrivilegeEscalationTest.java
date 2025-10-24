package com.sap.sse.security.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.security.SecurityService;
import com.sap.sse.security.impl.SecurityServiceImpl;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.PermissionChecker;
import com.sap.sse.security.shared.PermissionChecker.AclResolver;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.RoleDefinitionImpl;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.HasPermissionsImpl;
import com.sap.sse.security.shared.impl.LockingAndBanningImpl;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroupImpl;
import com.sap.sse.security.shared.subscription.SSESubscriptionPlan;
import com.sap.sse.security.userstore.mongodb.AccessControlStoreImpl;
import com.sap.sse.security.userstore.mongodb.PersistenceFactory;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;

public class PrivilegeEscalationTest {
    private final AclResolver<AccessControlList, Ownership> noopAclResolver = new AclResolver<AccessControlList, Ownership>() {
        @Override
        public Iterable<AccessControlList> resolveDenyingAclsAndCheckIfAnyMatches(Ownership ownershipOrNull,
                String type, Iterable<String> objectIdentifiersAsStringOrNull, Predicate<AccessControlList> filterCondition,
                Iterable<AccessControlList> allAclsForTypeAndObjectIdsOrNull) {
            return Collections.emptySet(); // assuming an empty ACL set
        }
    };
    private static final UUID USER_GROUP_UUID = UUID.randomUUID();
    private static final String TEST_DEFAULT_TENANT = "TestDefaultTenant";
    private static final String USER_USERNAME = "user";
    private static final String USER2_USERNAME = "user2";
    private UserStoreImpl userStore;
    private AccessControlStoreImpl accessControlStore;
    private HasPermissions type1 = new HasPermissionsImpl("DEMO", DefaultActions.READ, DefaultActions.UPDATE);
    private User user;
    private UserGroupImpl userGroup;
    private User user2;
    private SecurityService securityService;
    private RoleDefinition rd;

    @BeforeEach
    public void setup() throws UserStoreManagementException {
        userStore = new UserStoreImpl(PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory(),
                PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), TEST_DEFAULT_TENANT);
        userStore.ensureDefaultRolesExist();
        userStore.loadAndMigrateUsers();
        user = userStore.createUser(USER_USERNAME, null, new LockingAndBanningImpl());
        user2 = userStore.createUser(USER2_USERNAME, null, new LockingAndBanningImpl());
        userGroup = userStore.createUserGroup(USER_GROUP_UUID, USER_USERNAME+"-tenant");
        userGroup.add(user);
        userGroup.add(user2);
        userStore.updateUserGroup(userGroup);
        accessControlStore = new AccessControlStoreImpl(userStore);
        securityService = new SecurityServiceImpl(null, /* corsFilterConfigurationTracker */ null, userStore,
                accessControlStore, SecuredSecurityTypes::getAllInstances, SSESubscriptionPlan::getAllInstances);
        securityService.initialize();
        rd = new RoleDefinitionImpl(UUID.randomUUID(), "some_role",
                Collections.singleton(type1.getPermission(DefaultActions.READ, DefaultActions.UPDATE)));
    }
    
    @Test
    public void testMetaPermissionAfterUserDeletion() throws UserGroupManagementException, UserManagementException {
        Role role = new Role(rd, /* group qualification */ null, user, /* transitive */ true);
        user2.addRole(role);
        WildcardPermission permissionToCheck = type1.getPermissionForTypeRelativeIdentifier(DefaultActions.READ,
                new TypeRelativeObjectIdentifier("someid"));
        assertTrue(PermissionChecker.checkMetaPermission(permissionToCheck, Collections.singleton(type1), user2, null,
                new Ownership(user, null), noopAclResolver));
        securityService.deleteUser(USER_USERNAME);
        assertFalse(PermissionChecker.checkMetaPermission(permissionToCheck, Collections.singleton(type1), user2, null,
                new Ownership(user, null), noopAclResolver));
        assertFalse(PermissionChecker.checkMetaPermission(permissionToCheck, Collections.singleton(type1), user2, null,
                new Ownership(null, null), noopAclResolver));
    }
    
    @Test
    public void testMetaPermissionAfterGroupDeletion() throws UserGroupManagementException {
        user.addRole(new Role(rd, userGroup, /* user qualification */ null, /* transitive */ true));
        WildcardPermission permissionToCheck = type1.getPermissionForTypeRelativeIdentifier(DefaultActions.READ,
                new TypeRelativeObjectIdentifier("someid"));
        assertTrue(PermissionChecker.checkMetaPermission(permissionToCheck, Collections.singleton(type1), user, null,
                new Ownership(null, userGroup), noopAclResolver));
        securityService.deleteUserGroup(userGroup);
        assertFalse(PermissionChecker.checkMetaPermission(permissionToCheck, Collections.singleton(type1), user, null,
                new Ownership(null, userGroup), noopAclResolver));
        assertFalse(PermissionChecker.checkMetaPermission(permissionToCheck, Collections.singleton(type1), user, null,
                new Ownership(null, null), noopAclResolver));
    }

    @AfterEach
    public void cleanup() {
        userStore.clear();
        accessControlStore.clear();
    }
}
