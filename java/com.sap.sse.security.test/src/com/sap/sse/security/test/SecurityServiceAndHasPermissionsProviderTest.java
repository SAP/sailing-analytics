package com.sap.sse.security.test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.security.SecurityService;
import com.sap.sse.security.impl.SecurityServiceImpl;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.subscription.SSESubscriptionPlan;
import com.sap.sse.security.userstore.mongodb.AccessControlStoreImpl;
import com.sap.sse.security.userstore.mongodb.PersistenceFactory;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;

public class SecurityServiceAndHasPermissionsProviderTest {

    private static final String TEST_DEFAULT_TENANT = "TestDefaultTenant";
    private static final String ADMIN_USERNAME = "admin";
    private static final String REALM = "myRealm";
    private UserStoreImpl userStore;
    private AccessControlStoreImpl accessControlStore;
    private final WildcardPermission permission = new WildcardPermission("USER:READ:*");

    @BeforeEach
    public void setup() throws UserStoreManagementException {
        new UserStoreImpl(PersistenceFactory.INSTANCE.getDefaultMajorityDomainObjectFactory(),
                PersistenceFactory.INSTANCE.getDefaultMajorityMongoObjectFactory(), TEST_DEFAULT_TENANT).clear();
        userStore = new UserStoreImpl(PersistenceFactory.INSTANCE.getDefaultMajorityDomainObjectFactory(),
                PersistenceFactory.INSTANCE.getDefaultMajorityMongoObjectFactory(), TEST_DEFAULT_TENANT);
        userStore.ensureDefaultRolesExist();
        userStore.loadAndMigrateUsers();
        new AccessControlStoreImpl(PersistenceFactory.INSTANCE.getDefaultMajorityDomainObjectFactory(),
                PersistenceFactory.INSTANCE.getDefaultMajorityMongoObjectFactory(), userStore).clear();
        accessControlStore = new AccessControlStoreImpl(PersistenceFactory.INSTANCE.getDefaultMajorityDomainObjectFactory(),
                PersistenceFactory.INSTANCE.getDefaultMajorityMongoObjectFactory(), userStore);
    }

    @AfterEach
    public void cleanup() {
        userStore.clear();
        accessControlStore.clear();
    }

    private SecurityService createSecurityServiceWithoutHasPermissionsProvider() {
        SecurityService securityService = new SecurityServiceImpl(/* mailServiceTracker */ null, /* corsFilterConfigurationTracker */ null,
                userStore, accessControlStore, /* HasPermissionsProvider */ null, SSESubscriptionPlan::getAllInstances);
        securityService.initialize();
        return securityService;
    }

    private SecurityService createSecurityServiceWithHasPermissionsProvider() {
        final SecurityService securityService = new SecurityServiceImpl(/* mailServiceTracker */ null, /* corsFilterConfigurationTracker */ null,
                userStore, accessControlStore, SecuredSecurityTypes::getAllInstances, SSESubscriptionPlan::getAllInstances);
        securityService.initialize();
        return securityService;
    }

    private boolean excecutePermissionCheckUnderAdminSubject(SecurityService securityService, Function<SecurityService, Boolean> callable) {
        PrincipalCollection principals = new SimplePrincipalCollection(ADMIN_USERNAME, REALM);
        Subject subject = new Subject.Builder().principals(principals).authenticated(true).buildSubject();
        return subject.execute(()->callable.apply(securityService));
    }

    @Test
    public void testHasCurrentUserAnyPermissionExpectException() {
        assertThrows(IllegalArgumentException.class, () -> {
            createSecurityServiceWithoutHasPermissionsProvider().hasCurrentUserAnyPermission(new WildcardPermission("LEADERBOARD:READ:Humba"));
        });
    }
    
    @Test
    public void testHasCurrentUserMetaPermissionExpectException() {
        assertThrows(IllegalArgumentException.class, () -> {
            createSecurityServiceWithoutHasPermissionsProvider().hasCurrentUserMetaPermission(new WildcardPermission("LEADERBOARD:READ:Humba"), /* ownership */ null);
        });
    }

    @Test
    public void testHasCurrentUserMetaPermissionWithOwnershipLookupExpectException() {
        assertThrows(IllegalArgumentException.class, () -> {
            createSecurityServiceWithoutHasPermissionsProvider().hasCurrentUserMetaPermissionWithOwnershipLookup(new WildcardPermission("LEADERBOARD:READ:Humba"));
        });
    }
    
    @Test
    public void testHasCurrentUserAnyPermission() {
        assertTrue(excecutePermissionCheckUnderAdminSubject(createSecurityServiceWithHasPermissionsProvider(),
                securityService->securityService.hasCurrentUserAnyPermission(permission)));
    }

    @Test
    public void testHasCurrentUserMetaPermission() {
        assertTrue(excecutePermissionCheckUnderAdminSubject(createSecurityServiceWithHasPermissionsProvider(),
                securityService->securityService.hasCurrentUserMetaPermission(permission, /* ownership */ null)));
    }

    @Test
    public void testHasCurrentUserMetaPermissionWithOwnershipLookup() {
        assertTrue(excecutePermissionCheckUnderAdminSubject(createSecurityServiceWithHasPermissionsProvider(),
                securityService->securityService.hasCurrentUserMetaPermissionWithOwnershipLookup(permission)));
    }

}
