package com.sap.sse.security.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.shiro.subject.PrincipalCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sse.security.AbstractCompositeAuthorizingRealm;
import com.sap.sse.security.UsernamePasswordRealm;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.AdminRole;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.PermissionChecker;
import com.sap.sse.security.shared.PermissionChecker.AclResolver;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.RoleDefinitionImpl;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.HasPermissionsImpl;
import com.sap.sse.security.shared.impl.LockingAndBanningImpl;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.userstore.mongodb.AccessControlStoreImpl;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;

public class PermissionCheckerTest {
    private final AclResolver<AccessControlList, Ownership> noopAclResolver = new AclResolver<AccessControlList, Ownership>() {
        @Override
        public Iterable<AccessControlList> resolveDenyingAclsAndCheckIfAnyMatches(Ownership ownershipOrNull,
                String type, Iterable<String> objectIdentifiersAsStringOrNull, Predicate<AccessControlList> filterCondition,
                Iterable<AccessControlList> allAclsForTypeAndObjectIdsOrNull) {
            return Collections.emptySet(); // assuming an empty ACL set
        }
    };
    private final UUID eventId = UUID.randomUUID();
    private final WildcardPermission eventReadPermission = SecuredDomainType.EVENT
            .getPermissionForTypeRelativeIdentifier(DefaultActions.READ,
                    new TypeRelativeObjectIdentifier(eventId.toString()));
    private final UUID userTenantId = UUID.randomUUID();
    private UserGroup adminTenant;
    private User adminUser;
    private UserGroup userTenant;
    private User user;
    private ArrayList<UserGroup> tenants;
    private Ownership ownership;
    private Ownership adminOwnership;
    private AccessControlList acl;
    private final UUID globalRoleId = UUID.randomUUID();
    private RoleDefinition globalRoleDefinition;
    private AbstractCompositeAuthorizingRealm realm;
    private UserStore userStore;
    private AccessControlStore accessControlStore;
    private PrincipalCollection principalCollection;
    private HasPermissions type1 = new HasPermissionsImpl("DEMO", DefaultActions.READ, DefaultActions.UPDATE);
    private HasPermissions type2 = new HasPermissionsImpl("TEST", DefaultActions.READ, DefaultActions.DELETE);
    private Iterable<HasPermissions> allHasPermissions = Arrays.asList(type1, type2);
    
    @BeforeEach
    public void setUp() throws UserGroupManagementException, UserManagementException {
        final String adminTenantName = "admin-tenant";
        userStore = new UserStoreImpl(adminTenantName);
        userStore.ensureDefaultRolesExist();
        userStore.ensureServerGroupExists();
        accessControlStore = new AccessControlStoreImpl(userStore);
        AbstractCompositeAuthorizingRealm.setTestStores(userStore, accessControlStore);
        realm = new UsernamePasswordRealm();
        adminUser = userStore.getUserByName("admin");
        adminTenant = userStore.getUserGroupByName(adminTenantName);
        if (userStore.getUserByName("jonas") != null) {
            userStore.deleteUser("jonas");
        }
        userTenant = userStore.createUserGroup(userTenantId, "jonas-tenant");
        user = userStore.createUser("jonas", "jonas@dann.io", new LockingAndBanningImpl());
        userTenant.add(user);
        userStore.updateUserGroup(userTenant);
        ownership = new Ownership(user, userTenant);
        adminTenant.add(adminUser);
        adminOwnership = new Ownership(adminUser, adminTenant);
        tenants = new ArrayList<>();
        tenants.add(userTenant);
        tenants.add(adminTenant);
        acl = new AccessControlList();
        Set<WildcardPermission> permissionSet = new HashSet<>();
        permissionSet.add(eventReadPermission);
        globalRoleDefinition = new RoleDefinitionImpl(globalRoleId, "event", permissionSet);
        principalCollection = mock(PrincipalCollection.class);
        when(principalCollection.getPrimaryPrincipal()).thenReturn(user.getName());
    }
    
    @Test
    public void testOwnership() throws UserManagementException {
        assertFalse(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                null, acl, null));
        assertFalse(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                adminOwnership, acl, null));
        // being the owning user does not imply any permissions per se
        assertFalse(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                ownership, acl, null));
        userStore.addRoleForUser(user.getName(),
                new Role(userStore.getRoleDefinitionByPrototype(AdminRole.getInstance()),
                        /* qualified for userTenant */ null, /* qualified for user */ user, true));
        // having the admin role qualified for objects owned by user should help
        assertTrue(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                ownership, acl, null));
    }
    
    /**
     * {@link SecuredDomainType} objects may specify an object ID in their third part. When checking a permission,
     * ownership information needs to be obtained for the object(s) in question because it affects the
     * permission check. For example, a user may have a role that applies its permissions only to objects
     * that the user owns or where the user belongs to the group that owns the object. Therefore, it must be
     * possible to look up the ownership information based on the object ID provided in the third part of the
     * {@link SecuredDomainType} object. This test sets up objects of different kinds, specifies ownerships for them,
     * sets up users and roles with qualifications and then validates that the correct permissions emerge based
     * on a successful ownership lookup with the object ID provided by the permission.
     */
    @Test
    public void testPermissionsImpliedByOwnershipConstrainedRole() throws UserManagementException {
        final String leaderboardName = "My:Leaderboard, the only one ";
        TypeRelativeObjectIdentifier leaderboardIdentifier = new TypeRelativeObjectIdentifier(leaderboardName);
        final String regattaName = " My:Regatta, the only one ";
        TypeRelativeObjectIdentifier regattaIdentifier = new TypeRelativeObjectIdentifier(regattaName);
        WildcardPermission leaderboardPermission = SecuredDomainType.LEADERBOARD
                .getPermissionForTypeRelativeIdentifier(DefaultActions.READ, leaderboardIdentifier);
        WildcardPermission regattaPermission = SecuredDomainType.REGATTA.getPermissionForTypeRelativeIdentifier(
                DefaultActions.READ, regattaIdentifier);
        assertFalse(realm.isPermitted(principalCollection, leaderboardPermission.toString()));
        assertFalse(realm.isPermitted(principalCollection, regattaPermission.toString()));
        // let leaderboard be owned by user
        accessControlStore.setOwnership(SecuredDomainType.LEADERBOARD.getQualifiedObjectIdentifier(leaderboardIdentifier), user,
                /* tenantOwner */ null, leaderboardName);
        // let regatta be owned by admin
        accessControlStore.setOwnership(SecuredDomainType.REGATTA.getQualifiedObjectIdentifier(regattaIdentifier), adminUser,
                /* tenantOwner */ null, regattaName);
        // grant user the admin role, but only for objects owned by the user (leaderboard, but not regatta)
        userStore.addRoleForUser(user.getName(),
                new Role(userStore.getRoleDefinitionByPrototype(AdminRole.getInstance()), /* qualifiedForTenant */ null,
                        /* qualifiedForUser */ user, true));
        assertTrue(realm.isPermitted(principalCollection, leaderboardPermission.toString()));
        assertFalse(realm.isPermitted(principalCollection, regattaPermission.toString()));
        accessControlStore.setOwnership(SecuredDomainType.REGATTA.getQualifiedObjectIdentifier(regattaIdentifier), /* userOwner */ null,
                /* groupOwner */ userTenant, leaderboardName);
        assertTrue(realm.isPermitted(principalCollection, leaderboardPermission.toString()));
        // only adding the group owner doesn't grant permission yet:
        assertFalse(realm.isPermitted(principalCollection, regattaPermission.toString()));
        // but now we assign the admin role to the user, qualified for objects owned by the group owner:
        userStore.addRoleForUser(user.getName(),
                new Role(userStore.getRoleDefinitionByPrototype(AdminRole.getInstance()),
                        /* qualifiedForTenant */ userTenant, /* qualifiedForUser */ null, true));
        assertTrue(realm.isPermitted(principalCollection, leaderboardPermission.toString()));
        // now the user should be granted permission because admin gets *, and the user gets admin on all objects owned by userTenant
        assertTrue(realm.isPermitted(principalCollection, regattaPermission.toString()));
    }
    
    @Test
    public void testAccessControlList() {
        assertFalse(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                adminOwnership, null, null));
        assertFalse(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                adminOwnership, acl, null));
        acl.addPermission(userTenant, DefaultActions.READ.name());
        assertTrue(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                adminOwnership, acl, null));
        // ensure that anonymous users don't have access because they don't belong to any group
        assertFalse(PermissionChecker.isPermitted(eventReadPermission, /* user */ null, /* groups */ new HashSet<>(),
                null, null, adminOwnership, acl, null));
        user.addPermission(eventReadPermission);
        assertTrue(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                adminOwnership, acl, null));
        final Set<String> permissionSet = new HashSet<>();
        permissionSet.add("!" + DefaultActions.READ.name());
        acl.setPermissions(userTenant, permissionSet);
        assertFalse(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                adminOwnership, acl, null));
        // User ownership shall NOT imply permissions; the revoking ACL still takes precedence
        assertFalse(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                ownership, acl, null));
        // now add "public read" permission to ACL:
        acl.addPermission(null, DefaultActions.READ.name());
        assertTrue(PermissionChecker.isPermitted(eventReadPermission, /* user */ null, /* groups */ new HashSet<>(),
                null, null, adminOwnership, acl, null));
        // now deny "public read" permission in ACL which is expected to supersede the granting from above:
        acl.addPermission(null, "!"+DefaultActions.READ.name());
        assertFalse(PermissionChecker.isPermitted(eventReadPermission, /* user */ null, /* groups */ new HashSet<>(),
                null, null, adminOwnership, acl, null));
    }
    
    @Test
    public void testDirectPermission() {
        assertFalse(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                adminOwnership, acl, null));
        user.addPermission(eventReadPermission);
        assertTrue(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                adminOwnership, acl, null));
    }
    
    @Test
    public void testRole() {
        assertFalse(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                adminOwnership, acl, null));
        final Role globalRole = new Role(globalRoleDefinition, true);
        user.addRole(globalRole);
        assertTrue(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                adminOwnership, acl, null));
        user.removeRole(globalRole);
        user.addRole(new Role(globalRoleDefinition, this.userTenant, /* user qualifier */ null, true));
        assertFalse(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                adminOwnership, acl, null));
        Ownership testOwnership = new Ownership(adminUser, userTenant);
        assertTrue(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                testOwnership, acl, null));
        assertFalse(PermissionChecker.isPermitted(eventReadPermission, user, tenants, null, null,
                null, acl, null));
    }
    
    @Test
    public void testRoleAssociatedToGroup() {
        final RoleDefinition roleDefinition = new RoleDefinitionImpl(UUID.randomUUID(), "some_role",
                Arrays.asList(WildcardPermission.builder().withTypes(type1).build()));
        final Ownership foo = new Ownership(null, userTenant);
        Supplier<Boolean> permissionCheckGranted = () -> PermissionChecker.isPermitted(
                WildcardPermission.builder().withTypes(type1).withActions(DefaultActions.READ).withIds("abc").build(),
                user, tenants, null, null, foo, null, null);
        Supplier<Boolean> permissionCheckNotGranted = () -> PermissionChecker.isPermitted(
                WildcardPermission.builder().withTypes(type2).withActions(DefaultActions.READ).withIds("abc").build(),
                user, tenants, null, null, foo, null, null);
        assertFalse(permissionCheckGranted.get());
        userTenant.put(roleDefinition, false);
        assertTrue(permissionCheckGranted.get());
        assertFalse(permissionCheckNotGranted.get());
        userTenant.remove(roleDefinition);
        assertFalse(permissionCheckGranted.get());
        userTenant.put(roleDefinition, true);
        assertTrue(permissionCheckGranted.get());
        assertFalse(permissionCheckNotGranted.get());
    }

    @Test
    public void testMetaPermissionCheck() {
        final WildcardPermission allPermission = WildcardPermission.builder().build();
        final WildcardPermission singleTypePermission = type1.getPermission();
        assertFalse(checkMetaPermissionWithGrantedUserPermissions(singleTypePermission));
        assertTrue(checkMetaPermissionWithGrantedUserPermissions(singleTypePermission, allPermission));
        assertTrue(checkMetaPermissionWithGrantedUserPermissions(singleTypePermission, type1.getPermission()));
        assertFalse(checkMetaPermissionWithGrantedUserPermissions(singleTypePermission, type2.getPermission()));
        assertFalse(checkMetaPermissionWithGrantedUserPermissions(singleTypePermission,
                type1.getPermission(DefaultActions.READ)));
        assertTrue(checkMetaPermissionWithGrantedUserPermissions(singleTypePermission,
                type1.getPermission(DefaultActions.READ, DefaultActions.UPDATE)));
        final WildcardPermission combinedTypePermission = WildcardPermission.builder().withTypes(type1, type2).build();
        assertFalse(checkMetaPermissionWithGrantedUserPermissions(combinedTypePermission));
        assertFalse(checkMetaPermissionWithGrantedUserPermissions(combinedTypePermission, type1.getPermission()));
        assertTrue(checkMetaPermissionWithGrantedUserPermissions(combinedTypePermission, allPermission));
        assertTrue(checkMetaPermissionWithGrantedUserPermissions(combinedTypePermission, combinedTypePermission));
        assertTrue(checkMetaPermissionWithGrantedUserPermissions(combinedTypePermission, type1.getPermission(),
                type2.getPermission()));
        assertFalse(checkMetaPermissionWithGrantedUserPermissions(combinedTypePermission, type1.getPermission(),
                type2.getPermission(DefaultActions.READ)));
        assertTrue(checkMetaPermissionWithGrantedUserPermissions(combinedTypePermission, type1.getPermission(),
                type2.getPermission(DefaultActions.READ, DefaultActions.DELETE)));
        final WildcardPermission combinedTypeWithDistinctActionPermission = WildcardPermission.builder()
                .withTypes(type1, type2).withActions(DefaultActions.READ).build();
        assertFalse(checkMetaPermissionWithGrantedUserPermissions(combinedTypeWithDistinctActionPermission));
        assertFalse(checkMetaPermissionWithGrantedUserPermissions(combinedTypeWithDistinctActionPermission,
                type1.getPermission()));
        assertTrue(
                checkMetaPermissionWithGrantedUserPermissions(combinedTypeWithDistinctActionPermission, allPermission));
        assertTrue(checkMetaPermissionWithGrantedUserPermissions(combinedTypeWithDistinctActionPermission,
                type1.getPermission(), type2.getPermission()));
        assertTrue(checkMetaPermissionWithGrantedUserPermissions(combinedTypeWithDistinctActionPermission,
                type1.getPermission(DefaultActions.READ), type2.getPermission()));
        assertFalse(checkMetaPermissionWithGrantedUserPermissions(combinedTypeWithDistinctActionPermission,
                type1.getPermission(DefaultActions.READ), type2.getPermission(DefaultActions.DELETE)));
        assertTrue(checkMetaPermissionWithGrantedUserPermissions(combinedTypeWithDistinctActionPermission,
                type1.getPermission(DefaultActions.READ), type2.getPermission(DefaultActions.READ)));
    }

    private boolean checkMetaPermissionWithGrantedUserPermissions(WildcardPermission permissionToCheck,
            WildcardPermission... grantedPermissions) {
        for (WildcardPermission p : grantedPermissions) {
            user.addPermission(p);
        }
        boolean result = PermissionChecker.checkMetaPermission(permissionToCheck, allHasPermissions, user, null, null, noopAclResolver);
        for (WildcardPermission p : grantedPermissions) {
            user.removePermission(p);
        }
        return result;
    }

    @Test
    public void testMetaPermissionWithOwnership() {
        RoleDefinition rd = new RoleDefinitionImpl(UUID.randomUUID(), "some_role",
                Collections.singleton(type1.getPermission(DefaultActions.READ, DefaultActions.UPDATE)));
        user.addRole(new Role(rd, userTenant, null, true));
        WildcardPermission permissionToCheck = type1.getPermissionForTypeRelativeIdentifier(DefaultActions.READ,
                new TypeRelativeObjectIdentifier("someid"));
        // The assigned role is qualified by the tenant. This makes a check without ownership fail
        assertFalse(PermissionChecker.checkMetaPermission(permissionToCheck, allHasPermissions, user, null, null, noopAclResolver));
        // In addition a check with ownership without tentant will also fail
        assertFalse(PermissionChecker.checkMetaPermission(permissionToCheck, allHasPermissions, user, null,
                new Ownership(user, null), noopAclResolver));
        // A check with the wrong tentant owner will also fail
        assertFalse(PermissionChecker.checkMetaPermission(permissionToCheck, allHasPermissions, user, null,
                new Ownership(user, adminTenant), noopAclResolver));
        // Only an ownership with a tenant owner matching the roles qualification makes the check succeed
        assertTrue(PermissionChecker.checkMetaPermission(permissionToCheck, allHasPermissions, user, null,
                new Ownership(null, userTenant), noopAclResolver));
        assertTrue(PermissionChecker.checkMetaPermission(permissionToCheck, allHasPermissions, user, null,
                new Ownership(user, userTenant), noopAclResolver));
    }
    
    @Test
    public void testMetaPermissionWithOwnershipandWildcardAction() {
        RoleDefinition rd = new RoleDefinitionImpl(UUID.randomUUID(), "admin",
                Collections.singleton(WildcardPermission.builder().build()));
        user.addRole(new Role(rd, userTenant, null, true));
        final String objectId = "someid";
        // wildcard for the action part
        assertTrue(PermissionChecker.checkMetaPermissionWithOwnershipResolution(
                WildcardPermission.builder().withTypes(type1).withIds(objectId).build(), allHasPermissions, user, null,
                id -> new Ownership(null, userTenant), noopAclResolver));
    }
    
    @Test
    public void testMetaPermissionWithOwnershipResolutionForOneId() {
        RoleDefinition rd = new RoleDefinitionImpl(UUID.randomUUID(), "some_role",
                Collections.singleton(type1.getPermission(DefaultActions.READ, DefaultActions.UPDATE)));
        final String objectId = "someid";
        WildcardPermission permissionToCheck = type1.getPermissionForTypeRelativeIdentifier(DefaultActions.READ,
                new TypeRelativeObjectIdentifier(objectId));
        Function<QualifiedObjectIdentifier, Ownership> ownershipResolver = id -> {
            final String typeRelativeIdentifierString = id.getTypeRelativeObjectIdentifier().toString();
            if (objectId.equals(typeRelativeIdentifierString)) {
                return new Ownership(null, userTenant);
            }
            return null;
        };
        BooleanSupplier permissionCheck = () -> PermissionChecker.checkMetaPermissionWithOwnershipResolution(permissionToCheck, allHasPermissions,
                user, null, ownershipResolver, noopAclResolver);
        assertFalse(permissionCheck.getAsBoolean());
        // Not the right qualification -> check still fails
        user.addRole(new Role(rd, adminTenant, null, true));
        assertFalse(permissionCheck.getAsBoolean());
        // The right qualification
        user.addRole(new Role(rd, userTenant, null, true));
        assertTrue(permissionCheck.getAsBoolean());
    }
    
    @Test
    public void testMetaPermissionWithOwnershipResolutionForTwoIds() {
        RoleDefinition rd = new RoleDefinitionImpl(UUID.randomUUID(), "some_role",
                Collections.singleton(type1.getPermission(DefaultActions.READ, DefaultActions.UPDATE)));
        final String id1 = "id1";
        final String id2 = "id2";
        WildcardPermission permissionToCheck = WildcardPermission.builder().withTypes(type1)
                .withActions(DefaultActions.READ)
                .withIds(new TypeRelativeObjectIdentifier(id1), new TypeRelativeObjectIdentifier(id2)).build();
        
        Function<QualifiedObjectIdentifier, Ownership> ownershipResolver = id -> {
            final String typeRelativeIdentifierString = id.getTypeRelativeObjectIdentifier().toString();
            if (id1.equals(typeRelativeIdentifierString)) {
                return new Ownership(null, userTenant);
            }
            if (id2.equals(typeRelativeIdentifierString)) {
                return new Ownership(null, adminTenant);
            }
            return null;
        };
        
        BooleanSupplier permissionCheck = () -> PermissionChecker.checkMetaPermissionWithOwnershipResolution(permissionToCheck, allHasPermissions,
                user, null, ownershipResolver, noopAclResolver);
        
        assertFalse(permissionCheck.getAsBoolean());
        user.addRole(new Role(rd, userTenant, null, true));
        assertFalse(permissionCheck.getAsBoolean());
        user.addRole(new Role(rd, adminTenant, null, true));
        assertTrue(permissionCheck.getAsBoolean());
    }

    @Test
    public void testAnyPermissionCheck() {
        final WildcardPermission allPermission = WildcardPermission.builder().build();
        final WildcardPermission singleTypePermission = type1.getPermission();
        assertFalse(checkAnyPermissionWithGrantedUserPermissions(singleTypePermission));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(singleTypePermission, allPermission));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(singleTypePermission, type1.getPermission()));
        assertFalse(checkAnyPermissionWithGrantedUserPermissions(singleTypePermission, type2.getPermission()));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(singleTypePermission,
                type1.getPermission(DefaultActions.READ)));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(singleTypePermission,
                type1.getPermission(DefaultActions.READ, DefaultActions.UPDATE)));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(singleTypePermission,
                WildcardPermission.builder().withActions(DefaultActions.READ, DefaultActions.UPDATE).build()));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(singleTypePermission,
                WildcardPermission.builder().withActions(DefaultActions.READ, DefaultActions.UPDATE)
                        .withIds(WildcardPermission.WILDCARD_TOKEN).build()));
        
        final WildcardPermission singleTypePermissionWithAction = type1.getPermission(DefaultActions.READ);
        assertFalse(checkAnyPermissionWithGrantedUserPermissions(singleTypePermissionWithAction));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(singleTypePermissionWithAction, allPermission));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(singleTypePermissionWithAction, type1.getPermission()));
        assertFalse(checkAnyPermissionWithGrantedUserPermissions(singleTypePermissionWithAction, type2.getPermission()));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(singleTypePermissionWithAction,
                type1.getPermission(DefaultActions.READ)));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(singleTypePermissionWithAction,
                type1.getPermission(DefaultActions.READ, DefaultActions.UPDATE)));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(singleTypePermissionWithAction,
                WildcardPermission.builder().withActions(DefaultActions.READ, DefaultActions.UPDATE).build()));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(singleTypePermissionWithAction,
                WildcardPermission.builder().withActions(DefaultActions.READ, DefaultActions.UPDATE)
                .withIds(WildcardPermission.WILDCARD_TOKEN).build()));

        final WildcardPermission combinedTypePermission = WildcardPermission.builder().withTypes(type1, type2).build();
        assertFalse(checkAnyPermissionWithGrantedUserPermissions(combinedTypePermission));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(combinedTypePermission, type1.getPermission()));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(combinedTypePermission, allPermission));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(combinedTypePermission, combinedTypePermission));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(combinedTypePermission, type1.getPermission(),
                type2.getPermission()));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(combinedTypePermission,
                type1.getPermission(DefaultActions.READ)));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(combinedTypePermission,
                type1.getPermission(DefaultActions.UPDATE), type2.getPermission(DefaultActions.READ)));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(combinedTypePermission,
                type1.getPermission(DefaultActions.READ, DefaultActions.UPDATE)));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(combinedTypePermission,
                WildcardPermission.builder().withActions(DefaultActions.READ, DefaultActions.UPDATE).build()));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(combinedTypePermission,
                WildcardPermission.builder().withActions(DefaultActions.READ, DefaultActions.UPDATE)
                .withIds(WildcardPermission.WILDCARD_TOKEN).build()));

        final WildcardPermission combinedTypeWithDistinctActionPermission = WildcardPermission.builder()
                .withTypes(type1, type2).withActions(DefaultActions.READ).build();
        assertFalse(checkAnyPermissionWithGrantedUserPermissions(combinedTypeWithDistinctActionPermission));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(combinedTypeWithDistinctActionPermission,
                type1.getPermission()));
        assertTrue(
                checkAnyPermissionWithGrantedUserPermissions(combinedTypeWithDistinctActionPermission, allPermission));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(combinedTypeWithDistinctActionPermission,
                type1.getPermission(), type2.getPermission()));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(combinedTypeWithDistinctActionPermission,
                type1.getPermission(DefaultActions.READ)));
        assertTrue(checkAnyPermissionWithGrantedUserPermissions(combinedTypeWithDistinctActionPermission,
                type1.getPermission(DefaultActions.READ), type2.getPermission(DefaultActions.DELETE)));
    }

    @Test
    public void testPermissionCheckWithTransientRole() {
        final WildcardPermission permissionToCheck = new WildcardPermission("a:b:c");
        final RoleDefinition transientRoleDefinition = new RoleDefinitionImpl(UUID.randomUUID(), "transientRole", Collections.singleton(permissionToCheck));
        final Role transientRole = new Role(transientRoleDefinition, null, null, false);
        user.addRole(transientRole);
        boolean metaPermitted = PermissionChecker.checkMetaPermission(permissionToCheck, allHasPermissions, user, null, null, noopAclResolver);
        assertFalse(metaPermitted);
        boolean permitted = PermissionChecker.isPermitted(permissionToCheck, user, null, ownership, acl);
        assertTrue(permitted);
    }
    
    @Test
    public void testPermissionCheckWithTransientRoleThroughGroup() {
        final WildcardPermission permissionToCheck = new WildcardPermission("a:b:c");
        // No non-transitivity flag defined on role definition level
        final RoleDefinition transientRoleDefinition = new RoleDefinitionImpl(UUID.randomUUID(), "transientRole", Collections.singleton(permissionToCheck));
        userTenant.put(transientRoleDefinition, false);
        boolean metaPermitted = PermissionChecker.checkMetaPermission(permissionToCheck, allHasPermissions, user, null, null, noopAclResolver);
        assertFalse(metaPermitted);
        boolean permitted = PermissionChecker.isPermitted(permissionToCheck, user, null, ownership, acl);
        assertTrue(permitted);
    }

    private boolean checkAnyPermissionWithGrantedUserPermissions(WildcardPermission permissionToCheck,
            WildcardPermission... grantedPermissions) {
        for (WildcardPermission p : grantedPermissions) {
            user.addPermission(p);
        }
        boolean result = PermissionChecker.hasUserAnyPermission(permissionToCheck, allHasPermissions, user, null, null);
        for (WildcardPermission p : grantedPermissions) {
            user.removePermission(p);
        }
        return result;
    }
}