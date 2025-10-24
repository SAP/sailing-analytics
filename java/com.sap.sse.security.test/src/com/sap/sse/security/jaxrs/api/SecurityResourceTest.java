package com.sap.sse.security.jaxrs.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Locale;

import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.rest.StreamingOutputUtil;
import com.sap.sse.security.BearerAuthenticationToken;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.impl.Activator;
import com.sap.sse.security.impl.SecurityServiceImpl;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.subscription.SSESubscriptionPlan;
import com.sap.sse.security.userstore.mongodb.AccessControlStoreImpl;
import com.sap.sse.security.userstore.mongodb.PersistenceFactory;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;

public class SecurityResourceTest {
    private static final String USERNAME = "user-123";
    private static final String PASSWORD = "pass-234";
    private SecurityResource servlet;
    private SecurityService service;
    private Subject authenticatedAdmin;
    private UserStore store;
    private AccessControlStore accessControlStore;

    @BeforeEach
    public void setUp() throws UserManagementException, MailException, UserGroupManagementException {
        PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory().getDatabase().drop();
        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            store = new UserStoreImpl("TestDefaultTenant");
            store.ensureDefaultRolesExist();
            store.ensureServerGroupExists();
            accessControlStore = new AccessControlStoreImpl(store);
            Activator.setTestStores(store, accessControlStore);
            service = new SecurityServiceImpl(/* mailServiceTracker */ null, /* corsFilterConfigurationTracker */ null, store,
                    accessControlStore,
                    /* hasPermissionsProvider */SecuredSecurityTypes::getAllInstances, SSESubscriptionPlan::getAllInstances);
            service.initialize();
            Activator.setSecurityService(service);
            SecurityUtils.setSecurityManager(service.getSecurityManager());
            service.createSimpleUser(USERNAME, "a@b.c", PASSWORD, "The User", "SAP SE",
                    /* validation URL */ Locale.ENGLISH, null, null, /* clientIP */ null, /* enforce strong password */ false);
            authenticatedAdmin = SecurityUtils.getSubject();
            authenticatedAdmin.login(new UsernamePasswordToken(USERNAME, PASSWORD));
            Session session = authenticatedAdmin.getSession();
            assertNotNull(session);
            servlet = new SecurityResource() {
                @Override
                public SecurityService getSecurityService() {
                    return service;
                }
            };
            store.addPermissionForUser(USERNAME, new WildcardPermission("can do")); // equivalent to "can do:*:*"
            store.addPermissionForUser(USERNAME, new WildcardPermission("event:view:*"));
            store.addPermissionForUser(USERNAME, new WildcardPermission("event:edit:123"));
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        }
    }
    
    @Test
    public void testNullClientIP() {
        assertFalse(service.isClientIPLockedForBearerTokenAuthentication(null)); // ensure there is no exception being thrown
        service.failedBearerTokenAuthentication(null);
        assertTrue(service.isClientIPLockedForBearerTokenAuthentication(null));
        service.successfulBearerTokenAuthentication(null);
        assertFalse(service.isClientIPLockedForBearerTokenAuthentication(null));
    }
    
    @Test
    public void testCheckCurrentUserAnyExplicitPermissionsForNullObject() {
        assertTrue(authenticatedAdmin.execute(()->service.hasCurrentUserOneOfExplicitPermissions(/* object */ null, HasPermissions.DefaultActions.READ)));
        authenticatedAdmin.execute(()->service.checkCurrentUserHasOneOfExplicitPermissions(/* object */ null, HasPermissions.DefaultActions.READ));
    }
    
    @Test
    public void testCheckCurrentUserAnyExplicitPermissionsForEmptyActionsList() {
        final MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.dbId = "Humba";
        assertFalse(authenticatedAdmin.execute(()->service.hasCurrentUserOneOfExplicitPermissions(mediaTrack /* and an empty list of actions */)));
        try {
            authenticatedAdmin.execute(()->service.checkCurrentUserHasOneOfExplicitPermissions(mediaTrack /* and an empty list of actions */));
            fail("Expected AuthorizationException due to empty actions list; user cannot have any of no exceptions");
        } catch (AuthorizationException e) {
            // expected
        }
    }
    
    @Test
    public void testCheckCurrentUserAnyExplicitPermissionsForLoggedOutUser() {
        final MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.dbId = "Humba";
        authenticatedAdmin.logout();
        assertFalse(authenticatedAdmin.execute(()->service.hasCurrentUserOneOfExplicitPermissions(mediaTrack, HasPermissions.DefaultActions.READ)));
        try {
            authenticatedAdmin.execute(()->service.checkCurrentUserHasOneOfExplicitPermissions(mediaTrack, HasPermissions.DefaultActions.READ));
            fail("Expected AuthorizationException");
        } catch (AuthorizationException e) {
            // expected
        }
    }
    
    @Test
    public void testCheckCurrentUserAnyExplicitPermissionsForLoggedOutUserOnNullObject() {
        authenticatedAdmin.logout();
        assertTrue(authenticatedAdmin.execute(()->service.hasCurrentUserOneOfExplicitPermissions(null, HasPermissions.DefaultActions.READ)));
        authenticatedAdmin.execute(()->service.checkCurrentUserHasOneOfExplicitPermissions(null, HasPermissions.DefaultActions.READ));
    }
    
    @Test
    public void testCheckCurrentUserExplicitPermissionsForNullObject() {
        assertTrue(authenticatedAdmin.execute(()->service.hasCurrentUserExplicitPermissions(/* object */ null, HasPermissions.DefaultActions.READ)));
        authenticatedAdmin.execute(()->service.checkCurrentUserExplicitPermissions(/* object */ null, HasPermissions.DefaultActions.READ));
    }
    
    @Test
    public void testCheckCurrentUserExplicitPermissionsForEmptyActionsList() {
        final MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.dbId = "Humba";
        // doing nothing is always allowed
        assertTrue(authenticatedAdmin.execute(()->service.hasCurrentUserExplicitPermissions(mediaTrack /* and an empty list of actions */)));
        authenticatedAdmin.execute(()->service.checkCurrentUserExplicitPermissions(mediaTrack /* and an empty list of actions */));
    }

    @Test
    public void testCheckCurrentUserExplicitPermissionsForLoggedOutUserOnNullObject() {
        authenticatedAdmin.logout();
        assertTrue(authenticatedAdmin.execute(()->service.hasCurrentUserExplicitPermissions(null, HasPermissions.DefaultActions.READ)));
        authenticatedAdmin.execute(()->service.checkCurrentUserExplicitPermissions(null, HasPermissions.DefaultActions.READ));
    }

    @Test
    public void testCheckCurrentUserExplicitPermissionsForLoggedOutUser() {
        final MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.dbId = "Humba";
        authenticatedAdmin.logout();
        assertFalse(authenticatedAdmin.execute(()->service.hasCurrentUserExplicitPermissions(mediaTrack, HasPermissions.DefaultActions.READ)));
        try {
            authenticatedAdmin.execute(()->service.checkCurrentUserExplicitPermissions(mediaTrack, HasPermissions.DefaultActions.READ));
            fail("Expected AuthorizationException");
        } catch (AuthorizationException e) {
            // expected
        }
    }

    @Test
    public void testCheckCurrentUserDefaultPermissionsForLoggedOutUserOnNullObject() {
        authenticatedAdmin.logout();
        assertTrue(authenticatedAdmin.execute(()->service.hasCurrentUserDeletePermission((WithQualifiedObjectIdentifier) null)));
        assertTrue(authenticatedAdmin.execute(()->service.hasCurrentUserReadPermission(null)));
        assertTrue(authenticatedAdmin.execute(()->service.hasCurrentUserUpdatePermission(null)));
        authenticatedAdmin.execute(()->service.checkCurrentUserDeletePermission((WithQualifiedObjectIdentifier) null));
        authenticatedAdmin.execute(()->service.checkCurrentUserDeletePermission((QualifiedObjectIdentifier) null));
        authenticatedAdmin.execute(()->service.checkCurrentUserReadPermission(null));
        authenticatedAdmin.execute(()->service.checkCurrentUserUpdatePermission(null));
    }

    @Test
    public void testCheckCurrentUserDefaultPermissions() throws UserManagementException {
        final MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.dbId = "Humba";
        store.addPermissionForUser(USERNAME, mediaTrack.getPermissionType().getPermissionForObject(DefaultActions.READ, mediaTrack));
        store.addPermissionForUser(USERNAME, mediaTrack.getPermissionType().getPermissionForObject(DefaultActions.UPDATE, mediaTrack));
        assertTrue(authenticatedAdmin.execute(()->service.hasCurrentUserReadPermission(mediaTrack)));
        assertTrue(authenticatedAdmin.execute(()->service.hasCurrentUserUpdatePermission(mediaTrack)));
        assertFalse(authenticatedAdmin.execute(()->service.hasCurrentUserDeletePermission(mediaTrack)));
        authenticatedAdmin.execute(()->service.checkCurrentUserReadPermission(mediaTrack));
        authenticatedAdmin.execute(()->service.checkCurrentUserUpdatePermission(mediaTrack));
        try {
            authenticatedAdmin.execute(()->service.checkCurrentUserDeletePermission(mediaTrack.getIdentifier()));
            fail("AuthorizationException expected");
        } catch (AuthorizationException e) {
            // expected because no delete permission has been granted to user
        }
        try {
            authenticatedAdmin.execute(()->service.checkCurrentUserDeletePermission(mediaTrack));
            fail("AuthorizationException expected");
        } catch (AuthorizationException e) {
            // expected because no delete permission has been granted to user
        }
    }

    @Test
    public void testCheckCurrentUserDefaultPermissionsForLoggedOutUser() {
        final MediaTrack mediaTrack = new MediaTrack();
        mediaTrack.dbId = "Humba";
        authenticatedAdmin.logout();
        assertFalse(authenticatedAdmin.execute(()->service.hasCurrentUserDeletePermission(mediaTrack)));
        try {
            authenticatedAdmin.execute(()->service.checkCurrentUserDeletePermission(mediaTrack));
            fail("Expected AuthorizationException");
        } catch (AuthorizationException e) {
            // expected
        }
        try {
            authenticatedAdmin.execute(()->service.checkCurrentUserDeletePermission(mediaTrack.getIdentifier()));
            fail("Expected AuthorizationException");
        } catch (AuthorizationException e) {
            // expected
        }
        assertFalse(authenticatedAdmin.execute(()->service.hasCurrentUserReadPermission(mediaTrack)));
        try {
            authenticatedAdmin.execute(()->service.checkCurrentUserReadPermission(mediaTrack));
            fail("Expected AuthorizationException");
        } catch (AuthorizationException e) {
            // expected
        }
        assertFalse(authenticatedAdmin.execute(()->service.hasCurrentUserUpdatePermission(mediaTrack)));
        try {
            authenticatedAdmin.execute(()->service.checkCurrentUserUpdatePermission(mediaTrack));
            fail("Expected AuthorizationException");
        } catch (AuthorizationException e) {
            // expected
        }
    }

    private String getOrCreateAccessToken() throws ParseException, IOException {
        String responseJsonString = StreamingOutputUtil.getEntityAsString(servlet.respondWithAccessTokenForUser(USERNAME).getEntity());
        JSONObject responseJson = (JSONObject) new JSONParser().parse(responseJsonString);
        assertEquals(USERNAME, responseJson.get("username"));
        String accessToken = (String) responseJson.get("access_token");
        assertNotNull(accessToken);
        return accessToken;
    }

    private String createAccessToken() throws ParseException, IOException {
        assertEquals(Response.Status.OK.getStatusCode(), servlet.respondToRemoveAccessTokenForUser(USERNAME).getStatus());
        String responseJsonString = StreamingOutputUtil.getEntityAsString(servlet.respondWithAccessTokenForUser(USERNAME).getEntity());
        JSONObject responseJson = (JSONObject) new JSONParser().parse(responseJsonString);
        assertEquals(USERNAME, responseJson.get("username"));
        String accessToken = (String) responseJson.get("access_token");
        assertNotNull(accessToken);
        return accessToken;
    }

    private void removeAccessToken() {
        assertEquals(Response.Status.OK.getStatusCode(), servlet.respondToRemoveAccessTokenForUser(USERNAME).getStatus());
    }

    @Test
    public void createAccessTokenAndAuthenticate() throws ParseException, UserManagementException, IOException {
        String accessToken = getOrCreateAccessToken();
        User user = service.getUserByAccessToken(accessToken);
        assertNotNull(user);
        assertEquals(USERNAME, user.getName());
        final Subject subject = SecurityUtils.getSubject();
        subject.login(new BearerAuthenticationToken(accessToken, /* clientIP */ null, /* userAgent */ null));
        assertTrue(subject.isAuthenticated());
        assertEquals(USERNAME, subject.getPrincipal());
        assertTrue(subject.isPermitted("can do"));
        assertFalse(subject.isPermitted("can't do"));
        service.addPermissionForUser(USERNAME, new WildcardPermission("can't do"));
        assertTrue(subject.isPermitted("can't do"));

        assertTrue(subject.isPermitted("event:view:999"));
        assertTrue(subject.isPermitted("event:edit:123"));
        assertFalse(subject.isPermitted("event:edit:234"));
        subject.logout();
        assertFalse(subject.isAuthenticated());
    }

    @Test
    public void ensureOldBearerTokenIsInvalidatedByObtainingNewOne() throws ParseException, IOException {
        String accessToken = getOrCreateAccessToken();
        createAccessToken();
        User user = service.getUserByAccessToken(accessToken);
        assertNull(user); // the old access token is expected to have been obsoleted by obtaining a new one
    }

    @Test
    public void ensureOldBearerTokenIsInvalidatedByRequestingItsRemoval() throws ParseException, IOException {
        String accessToken = getOrCreateAccessToken();
        removeAccessToken();
        User user = service.getUserByAccessToken(accessToken);
        assertNull(user); // the old access token is expected to have been obsoleted by obtaining a new one
    }
}
