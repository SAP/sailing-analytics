package com.sap.sse.security.ui.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.security.Credential;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.UserImpl;
import com.sap.sse.security.shared.AccessControlList;
import com.sap.sse.security.shared.Role;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.RoleImpl;
import com.sap.sse.security.shared.Tenant;
import com.sap.sse.security.shared.TenantManagementException;
import com.sap.sse.security.shared.UnauthorizedException;
import com.sap.sse.security.shared.User;
import com.sap.sse.security.shared.UserGroup;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.ui.client.UserManagementService;
import com.sap.sse.security.ui.oauth.client.CredentialDTO;
import com.sap.sse.security.ui.oauth.shared.OAuthException;
import com.sap.sse.security.ui.shared.SuccessInfo;
import com.sap.sse.security.ui.shared.UserDTO;

public class UserManagementServiceImpl extends RemoteServiceServlet implements UserManagementService {
    private static final long serialVersionUID = 4458564336368629101L;
    
    private static final Logger logger = Logger.getLogger(UserManagementServiceImpl.class.getName());

    private final BundleContext context;
    private final FutureTask<SecurityService> securityService;
    final SecurityDTOFactory securityDTOFactory;

    public UserManagementServiceImpl() {
        context = Activator.getContext();
        securityDTOFactory = new SecurityDTOFactory();
        final ServiceTracker<SecurityService, SecurityService> tracker = new ServiceTracker<>(context, SecurityService.class, /* customizer */ null);
        tracker.open();
        securityService = new FutureTask<SecurityService>(new Callable<SecurityService>() {
            @Override
            public SecurityService call() {
                SecurityService result = null;
                try {
                    logger.info("Waiting for SecurityService...");
                    result = tracker.waitForService(0);
                    logger.info("Obtained SecurityService "+result);
                    return result;
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Interrupted while waiting for UserStore service", e);
                }
                return result;
            }
        });
        new Thread("ServiceTracker in bundle com.sap.sse.security.ui waiting for SecurityService") {
            @Override
            public void run() {
                securityService.run();
                SecurityUtils.setSecurityManager(getSecurityService().getSecurityManager());
            }
        }.start();
    }

    @Override
    public RoleDefinition createRoleDefinition(String roleDefinitionIdAsString, String name) {
        return getSecurityService().createRoleDefinition(UUID.fromString(roleDefinitionIdAsString), name);
    }

    @Override
    public void deleteRoleDefinition(String roleIdAsString) {
        getSecurityService().deleteRoleDefinition(getSecurityService().getRoleDefinition(UUID.fromString(roleIdAsString)));
    }

    @Override
    public void updateRoleDefinition(RoleDefinition roleDefinitionWithNewProperties) {
        getSecurityService().updateRoleDefinition(roleDefinitionWithNewProperties);
    }

    @Override
    public ArrayList<RoleDefinition> getRoleDefinitions() {
        final ArrayList<RoleDefinition> result = new ArrayList<>();
        Util.addAll(getSecurityService().getRoleDefinitions(), result);
        return result;
    }

    @Override
    public Collection<AccessControlList> getAccessControlListList() throws UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("access_control:manage")) {
            List<AccessControlList> acls = new ArrayList<>();
            for (AccessControlList acl : getSecurityService().getAccessControlListList()) {
                acls.add(securityDTOFactory.createAccessControlListDTO(acl));
            }
            return acls;
        } else {
            throw new UnauthorizedException("Not permitted to manage access control");
        }
    }

    @Override
    public AccessControlList getAccessControlList(String idOfAccessControlledObjectAsString) {
        return securityDTOFactory.createAccessControlListDTO(getSecurityService().getAccessControlList(idOfAccessControlledObjectAsString));
    }

    @Override
    public AccessControlList updateACL(String idOfAccessControlledObjectAsString, Map<String, Set<String>> permissionStrings) throws UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("tenant:grant_permission,revoke_permission")) {
            Map<UserGroup, Set<String>> permissionMap = new HashMap<>();
            for (String group : permissionStrings.keySet()) {
                permissionMap.put(getSecurityService().getUserGroupByName(group), permissionStrings.get(group));
            }
            return securityDTOFactory.createAccessControlListDTO(getSecurityService().updateACL(idOfAccessControlledObjectAsString, permissionMap));
        } else {
            throw new UnauthorizedException("Not permitted to grant and revoke permissions for user");
        }
    }

    @Override
    public AccessControlList addToACL(String idAsString, String groupOrTenantIdAsString, String action) throws UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("tenant:grant_permission:" + groupOrTenantIdAsString)) {
            UserGroup userGroup = getTenantOrUserGroup(groupOrTenantIdAsString);
            return securityDTOFactory.createAccessControlListDTO(getSecurityService().addToACL(idAsString, userGroup, action));
        } else {
            throw new UnauthorizedException("Not permitted to grant permission for user");
        }
    }

    private UserGroup getTenantOrUserGroup(String groupOrTenantIdAsString) {
        UUID groupOrTenantId = UUID.fromString(groupOrTenantIdAsString);
        UserGroup userGroup = getSecurityService().getTenant(groupOrTenantId);
        if (userGroup == null) {
            userGroup = getSecurityService().getUserGroup(groupOrTenantId);
        }
        return userGroup;
    }

    @Override
    public AccessControlList removeFromACL(String idAsString, String groupOrTenantIdAsString, String permission) throws UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("tenant:revoke_permission:" + groupOrTenantIdAsString)) {
            UserGroup userGroup = getTenantOrUserGroup(groupOrTenantIdAsString);
            return securityDTOFactory.createAccessControlListDTO(getSecurityService().removeFromACL(idAsString, userGroup, permission));
        } else {
            throw new UnauthorizedException("Not permitted to revoke permission for user");
        }
    }

    @Override
    public Collection<Tenant> getTenants() throws UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("tenants:manage")) {
            List<Tenant> tenants = new ArrayList<>();
            for (Tenant t : getSecurityService().getTenants()) {
                Tenant tenantDTO = securityDTOFactory.createTenantDTOFromTenant(t);
                tenants.add(tenantDTO);
            }
            return tenants;
        } else {
            throw new UnauthorizedException("Not permitted to manage tenants");
        }
    }

    @Override
    public Tenant createTenant(String name, String tenantOwner) throws TenantManagementException, UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("tenant:create")) {
            UUID newTenantId = UUID.randomUUID();
            Tenant tenant;
            try {
                tenant = getSecurityService().createTenant(newTenantId, name);
            } catch (UserGroupManagementException e) {
                throw new TenantManagementException(e.getMessage());
            }
            getSecurityService().createOwnership(newTenantId.toString(),
                    getSecurityService().getCurrentUser(), getSecurityService().getTenantByName(tenantOwner), name);
            return securityDTOFactory.createTenantDTOFromTenant(tenant);
        } else {
            throw new UnauthorizedException("Not permitted to create tenants");
        }
    }

    @Override
    public void addUserToTenant(String tenantIdAsString, String username) throws UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("tenant:add_user:" + tenantIdAsString)) {
            final Tenant tenant = getSecurityService().getTenant(UUID.fromString(tenantIdAsString));
            getSecurityService().addUserToTenant(tenant, getSecurityService().getUserByName(username));
        } else {
            throw new UnauthorizedException("Not permitted to add user to tenant");
        }
    }

    @Override
    public void removeUserFromTenant(String tenantIdAsString, String username) throws UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("tenant:remove_user:" + tenantIdAsString)) {
            final Tenant tenant = getSecurityService().getTenant(UUID.fromString(tenantIdAsString));
            getSecurityService().removeUserFromTenant(tenant, getSecurityService().getUserByName(username));
        } else {
            throw new UnauthorizedException("Not permitted to remove user from tenant");
        }
    }

    @Override
    public SuccessInfo deleteTenant(String tenantIdAsString) throws UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("tenant:delete:" + tenantIdAsString)) {
            try {
                UUID tenantId = UUID.fromString(tenantIdAsString);
                final Tenant tenant = getSecurityService().getTenant(tenantId);
                getSecurityService().deleteTenant(tenant);
                getSecurityService().deleteACL(tenantIdAsString);
                getSecurityService().deleteOwnership(tenantIdAsString);
                return new SuccessInfo(true, "Deleted tenant: " + tenant.getName() + ".", /* redirectURL */ null, null);
            } catch (UserGroupManagementException e) {
                return new SuccessInfo(false, "Could not delete tenant.", /* redirectURL */ null, null);
            }
        } else {
            throw new UnauthorizedException("Not permitted to delete tenant");
        }
    }

    @Override
    public Collection<UserDTO> getUserList() throws UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("users:manage")) {
            List<UserDTO> users = new ArrayList<>();
            for (UserImpl u : getSecurityService().getUserList()) {
                UserDTO userDTO = securityDTOFactory.createUserDTOFromUser(u, getSecurityService());
                users.add(userDTO);
            }
            return users;
        } else {
            throw new UnauthorizedException("Not permitted to manage users");
        }
    }

    @Override
    public UserDTO getCurrentUser() throws UnauthorizedException {
        logger.fine("Request: " + getThreadLocalRequest().getRequestURL());
        User user = getSecurityService().getCurrentUser();
        if (user == null) {
            return null;
        }
        if (SecurityUtils.getSubject().isPermitted("user:view:" + user.getName())) {
            return securityDTOFactory.createUserDTOFromUser(user, getSecurityService());
        } else {
            throw new UnauthorizedException("Not permitted to view current user");
        }
    }

    @Override
    public SuccessInfo login(String username, String password) {
        try {
            String redirectURL = getSecurityService().login(username, password);
            return new SuccessInfo(true, "Success. Redirecting to "+redirectURL, redirectURL,
                    securityDTOFactory.createUserDTOFromUser(getSecurityService().getUserByName(username), getSecurityService()));
        } catch (UserManagementException | AuthenticationException e) {
            return new SuccessInfo(false, SuccessInfo.FAILED_TO_LOGIN, /* redirectURL */ null, null);
        }
    }

    @Override
    public SuccessInfo logout() {
        logger.info("Logging out user: " + SecurityUtils.getSubject());
        getSecurityService().logout();
        getHttpSession().invalidate();
        final Cookie cookie = new Cookie(UserManagementConstants.LOCALE_COOKIE_NAME, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        getThreadLocalResponse().addCookie(cookie);
        logger.info("Invalidated HTTP session");
        return new SuccessInfo(true, "Logged out.", /* redirectURL */ null, null);
    }

    @Override
    public UserDTO createSimpleUser(String username, String email, String password, String fullName, String company, String localeName, String validationBaseURL, String tenantOwner) throws UserManagementException, MailException, UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("user:create")) {
            User currentUser = getSecurityService().getCurrentUser();
            UserImpl u = null;
            try {
                u = getSecurityService().createSimpleUser(username, email, password, fullName, company, getLocaleFromLocaleName(localeName), validationBaseURL);
                getSecurityService().createOwnership(username, currentUser, getSecurityService().getTenantByName(tenantOwner));
            } catch (UserManagementException | UserGroupManagementException e) {
                logger.log(Level.SEVERE, "Error creating user "+username, e);
                throw new UserManagementException(e.getMessage());
            }
            if (u == null) {
                return null;
            }
            return securityDTOFactory.createUserDTOFromUser(u, getSecurityService());
        } else {
            throw new UnauthorizedException("Not permitted to create user");
        }
    }


    @Override
    public void updateSimpleUserPassword(final String username, String oldPassword, String passwordResetSecret, String newPassword) throws UserManagementException {
        if (SecurityUtils.getSubject().isPermitted("user:edit:" + username)
            // someone knew a username and the correct password for that user
         || (oldPassword != null && getSecurityService().checkPassword(username, oldPassword))
            // someone provided the correct password reset secret for the correct username
         || (passwordResetSecret != null && getSecurityService().checkPasswordResetSecret(username, passwordResetSecret))) {
            getSecurityService().updateSimpleUserPassword(username, newPassword);
            new Thread("sending updated password to user "+username+" by e-mail") {
                @Override public void run() {
                    try {
                        getSecurityService().sendMail(username, "Password Changed", "Somebody changed your password for your user named "+username+".\nIf that wasn't you, please contact sailing_analytics@sap.com via email.");
                    } catch (MailException e) {
                        logger.log(Level.SEVERE, "Error sending new password to user "+username+" by e-mail", e);
                    }
                }
            }.start();
        } else {
            throw new UserManagementException(UserManagementException.INVALID_CREDENTIALS);
        }
    }

    private void ensureThatUserInQuestionIsLoggedInOrCurrentUserIsAdmin(String username) throws UserManagementException {
        final Subject subject = SecurityUtils.getSubject();
        // the signed-in subject has all permissions or is changing own user
        if (SecurityUtils.getSubject().isPermitted("*") &&
                (subject.getPrincipal() == null
                || !username.equals(subject.getPrincipal().toString()))) {
            throw new UserManagementException(UserManagementException.INVALID_CREDENTIALS);
        }
    }
    
    @Override
    public void updateUserProperties(final String username, String fullName, String company, String localeName) throws UserManagementException {
        ensureThatUserInQuestionIsLoggedInOrCurrentUserIsAdmin(username);
        getSecurityService().updateUserProperties(username, fullName, company,
                getLocaleFromLocaleName(localeName));
    }

    private Locale getLocaleFromLocaleName(String localeName) {
        try {
            return localeName == null || localeName.isEmpty() ? null : Locale.forLanguageTag(localeName);
        } catch (Exception e) {
            logger.log(Level.WARNING, e, () -> "Error while parsing locale with name '" + localeName + "'");
            return null;
        }
    }
    
    @Override
    public void updateSimpleUserEmail(String username, String newEmail, String validationBaseURL) throws UserManagementException, MailException {
        ensureThatUserInQuestionIsLoggedInOrCurrentUserIsAdmin(username);
        getSecurityService().updateSimpleUserEmail(username, newEmail, validationBaseURL);
    }
    
    @Override
    public void resetPassword(String username, String email, String passwordResetBaseURL) throws UserManagementException, MailException {
        if (username == null || username.isEmpty()) {
            username = getSecurityService().getUserByEmail(email).getName();
        }
        getSecurityService().resetPassword(username, passwordResetBaseURL);
    }

    @Override
    public boolean validateEmail(String username, String validationSecret) throws UserManagementException {
        return getSecurityService().validateEmail(username, validationSecret);
    }

    @Override
    public Collection<UserDTO> getFilteredSortedUserList(String filter) throws UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("users:manage")) {
            List<UserDTO> users = new ArrayList<>();
            for (UserImpl u : getSecurityService().getUserList()) {
                if (filter != null && filter.isEmpty()) {
                    if (u.getName().contains(filter)) {
                        users.add(securityDTOFactory.createUserDTOFromUser(u, getSecurityService()));
                    }
                } else {
                    users.add(securityDTOFactory.createUserDTOFromUser(u, getSecurityService()));
                }
            }
            Collections.sort(users, new Comparator<UserDTO>() {
                private final NaturalComparator naturalComparator = new NaturalComparator(/* caseSensitive */ false);
                @Override
                public int compare(UserDTO u1, UserDTO u2) {
                    return naturalComparator.compare(u1.getName(), u2.getName());
                }
            });
            return users;
        } else {
            throw new UnauthorizedException("Not permitted to manage users");
        }
    }

    @Override
    public SuccessInfo setRolesForUser(String username,
            Iterable<Triple<UUID, UUID, String>> roleDefinitionIdAndTenantQualifierIdAndUsernames)
            throws UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("user:grant_permission,revoke_permission:" + username)) {
            User u = getSecurityService().getUserByName(username);
            if (u == null) {
                return new SuccessInfo(false, "User does not exist.", /* redirectURL */null, null);
            }
            Set<Role> rolesToSet = new HashSet<>();
            for (final Triple<UUID, UUID, String> roleDefinitionIdAndTenantQualifierIdAndUsernameOfRoleToSet : roleDefinitionIdAndTenantQualifierIdAndUsernames) {
                rolesToSet.add(createRoleFromIDs(roleDefinitionIdAndTenantQualifierIdAndUsernameOfRoleToSet.getA(),
                        roleDefinitionIdAndTenantQualifierIdAndUsernameOfRoleToSet.getB(),
                        roleDefinitionIdAndTenantQualifierIdAndUsernameOfRoleToSet.getC()));
            }
            Set<Role> roleDefinitionsToRemove = new HashSet<>();
            Util.addAll(u.getRoles(), roleDefinitionsToRemove);
            Util.removeAll(rolesToSet, roleDefinitionsToRemove);
            for (Role roleToRemove : roleDefinitionsToRemove) {
                getSecurityService().removeRoleFromUser(u, roleToRemove);
            }
            Set<Role> rolesToAdd = new HashSet<>();
            Util.addAll(rolesToSet, rolesToAdd);
            Util.removeAll(u.getRoles(), rolesToAdd);
            for (Role roleToAdd : rolesToAdd) {
                getSecurityService().addRoleForUser(u, roleToAdd);
            }
            logger.info("Set roles "+roleDefinitionIdAndTenantQualifierIdAndUsernames+" for user "+username);
            return new SuccessInfo(true, "Set roles " + roleDefinitionIdAndTenantQualifierIdAndUsernames + " for user " + username, /* redirectURL */null,
                    securityDTOFactory.createUserDTOFromUser(u, getSecurityService()));
        } else {
            throw new UnauthorizedException("Not permitted to grant permissions to user");
        }
    }
    
    private Role createRoleFromIDs(UUID roleDefinitionId, UUID qualifyingTenantId, String qualifyingUsername) {
        return new RoleImpl(
                getSecurityService().getRoleDefinition(roleDefinitionId),
                qualifyingTenantId == null ? null : getSecurityService().getTenant(qualifyingTenantId),
                        qualifyingUsername == null ? null : getSecurityService().getUserByName(qualifyingUsername));
    }

    @Override
    public SuccessInfo setPermissionsForUser(String username, Iterable<WildcardPermission> permissions) throws UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("user:grant_permission,revoke_permission:" + username)) {
            UserImpl u = getSecurityService().getUserByName(username);
            if (u == null) {
                return new SuccessInfo(false, "User does not exist.", /* redirectURL */null, null);
            }
            Set<WildcardPermission> permissionsToRemove = new HashSet<>();
            Util.addAll(u.getPermissions(), permissionsToRemove);
            Util.removeAll(permissions, permissionsToRemove);
            for (WildcardPermission permissionToRemove : permissionsToRemove) {
                getSecurityService().removePermissionFromUser(username, permissionToRemove);
            }
            Set<WildcardPermission> permissionsToAdd = new HashSet<>();
            Util.addAll(permissions, permissionsToAdd);
            Util.removeAll(u.getPermissions(), permissionsToAdd);
            for (WildcardPermission permissionToAdd : permissionsToAdd) {
                getSecurityService().addPermissionForUser(username, permissionToAdd);
            }
            return new SuccessInfo(true, "Set roles " + permissions + " for user " + username, /* redirectURL */null,
                    securityDTOFactory.createUserDTOFromUser(u, getSecurityService()));
        } else {
            throw new UnauthorizedException("Not permitted to grant or revoke permissions for user");
        }
    }

    @Override
    public SuccessInfo deleteUser(String username) throws UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("user:delete:" + username)) {
        try {
            getSecurityService().deleteUser(username);
            getSecurityService().deleteACL(username);
            getSecurityService().deleteOwnership(username);
            return new SuccessInfo(true, "Deleted user: " + username + ".", /* redirectURL */ null, null);
        } catch (UserManagementException e) {
            return new SuccessInfo(false, "Could not delete user.", /* redirectURL */ null, null);
        }
        } else {
            throw new UnauthorizedException("Not permitted to delete user");
        }
    }

    @Override
    public Map<String, String> getSettings() {
        Map<String, String> settings = new TreeMap<String, String>();
        for (Entry<String, Object> e : getSecurityService().getAllSettings().entrySet()){
            settings.put(e.getKey(), e.getValue().toString());
        }
        return settings;
    }

    @Override
    public void setSetting(String key, String clazz, String setting) {
        if (clazz.equals(Boolean.class.getName())){
            getSecurityService().setSetting(key, Boolean.parseBoolean(setting));
        }
        else if (clazz.equals(Integer.class.getName())){
            getSecurityService().setSetting(key, Integer.parseInt(setting));
        }
        else {
            getSecurityService().setSetting(key, setting);
        }
        getSecurityService().refreshSecurityConfig(getServletContext());
    }

    @Override
    public Map<String, String> getSettingTypes() {
        Map<String, String> settingTypes = new TreeMap<String, String>();
        for (Entry<String, Class<?>> e : getSecurityService().getAllSettingTypes().entrySet()) {
            settingTypes.put(e.getKey(), e.getValue().getName());
        }
        return settingTypes;
    }
    
    //--------------------------------------------------------- OAuth Implementations -------------------------------------------------------------------------

    @Override
    public String getAuthorizationUrl(CredentialDTO credential) throws OAuthException {
        logger.info("callback url: " + credential.getRedirectUrl());
        String authorizationUrl = null;
        try {
            authorizationUrl = getSecurityService().getAuthenticationUrl(createCredentialFromDTO(credential));
        } catch (UserManagementException e) {
            throw new OAuthException(e.getMessage());
        }
        return authorizationUrl;
    }

    @Override
    public UserDTO verifySocialUser(CredentialDTO credentialDTO) {
        UserImpl user = null;
        try {
            user = getSecurityService().verifySocialUser(createCredentialFromDTO(credentialDTO));
        } catch (UserManagementException e) {
            e.printStackTrace();
        }
        return securityDTOFactory.createUserDTOFromUser(user, getSecurityService());
    }

    private HttpSession getHttpSession() {
        return getThreadLocalRequest().getSession();
    }

    private Credential createCredentialFromDTO(CredentialDTO credentialDTO){
        Credential credential = new Credential();
        credential.setAuthProvider(credentialDTO.getAuthProvider());
        credential.setAuthProviderName(credentialDTO.getAuthProviderName());
        credential.setEmail(credentialDTO.getEmail());
        credential.setLoginName(credentialDTO.getLoginName());
        credential.setPassword(credentialDTO.getPassword());
        credential.setRedirectUrl(credentialDTO.getRedirectUrl());
        credential.setState(credentialDTO.getState());
        credential.setVerifier(credentialDTO.getVerifier());
        credential.setOauthToken(credentialDTO.getOauthToken());
        return credential;
    }
    
    @Override
    public void addSetting(String key, String clazz, String setting) {
        try {
            getSecurityService().addSetting(key, Class.forName(clazz));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (UserManagementException e) {
            e.printStackTrace();
        }
        if (clazz.equals(Boolean.class.getName())){
            getSecurityService().setSetting(key, Boolean.parseBoolean(setting));
        }
        else if (clazz.equals(Integer.class.getName())){
            getSecurityService().setSetting(key, Integer.parseInt(setting));
        }
        else {
            getSecurityService().setSetting(key, setting);
        }
        getSecurityService().refreshSecurityConfig(getServletContext());
    }

    SecurityService getSecurityService() {
        try {
            return securityService.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setPreference(String username, String key, String value) throws UserManagementException, UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("user:edit:" + username)) {
            try {
                getSecurityService().setPreference(username, key, value);
            } catch (AuthorizationException e) {
                throw new UserManagementException(UserManagementException.USER_DOESNT_HAVE_PERMISSION);
            }
        } else {
            throw new UnauthorizedException("Not permitted to edit user");
        }
    }
    
    @Override
    public void setPreferences(String username, Map<String, String> keyValuePairs) throws UserManagementException, UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("user:edit:" + username)) {
            try {
                for (Entry<String, String> entry : keyValuePairs.entrySet()) {
                    getSecurityService().setPreference(username, entry.getKey(), entry.getValue());
                }
            } catch (AuthorizationException e) {
                throw new UserManagementException(UserManagementException.USER_DOESNT_HAVE_PERMISSION);
            }
        } else {
            throw new UnauthorizedException("Not permitted to edit user");
        }
    }

    @Override
    public void unsetPreference(String username, String key) throws UserManagementException, UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("user:edit:" + username)) {
            try {
                getSecurityService().unsetPreference(username, key);
            } catch (AuthorizationException e) {
                throw new UserManagementException(UserManagementException.USER_DOESNT_HAVE_PERMISSION);
            }
        } else {
            throw new UnauthorizedException("Not permitted to edit user");
        }
    }

    @Override
    public String getPreference(String username, String key) throws UserManagementException, UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("user:view:" + username)) {
            try {
                return getSecurityService().getPreference(username, key);
            } catch (AuthorizationException e) {
                throw new UserManagementException(UserManagementException.USER_DOESNT_HAVE_PERMISSION);
            }
        } else {
            throw new UnauthorizedException("Not permitted to view user");
        }
    }
    
    @Override
    public Map<String, String> getPreferences(String username, List<String> keys) throws UserManagementException, UnauthorizedException {
        Map<String, String> requestedPreferences = new HashMap<>();
        for (String key : keys) {
            requestedPreferences.put(key, getPreference(username, key));
        }
        return requestedPreferences;
    }
    
    @Override
    public Map<String, String> getAllPreferences(String username) throws UserManagementException, UnauthorizedException {
        if (SecurityUtils.getSubject().isPermitted("user:view:" + username)) {
            try {
                final Map<String, String> allPreferences = getSecurityService().getAllPreferences(username);
                final Map<String, String> result = new HashMap<>();
                for (Map.Entry<String, String> entry : allPreferences.entrySet()) {
                    if(!entry.getKey().startsWith("_")) {
                        result.put(entry.getKey(), entry.getValue());
                    }
                }
                return result;
            } catch (AuthorizationException e) {
                throw new UserManagementException(UserManagementException.USER_DOESNT_HAVE_PERMISSION);
            }
        } else {
            throw new UnauthorizedException("Not permitted to view user");
        }
    }

    @Override
    public String getAccessToken(String username) {
        return getSecurityService().getAccessToken(username);
    }

    @Override
    public String getOrCreateAccessToken(String username) {
        return getSecurityService().getOrCreateAccessToken(username);
    }

}
