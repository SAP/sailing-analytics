package com.sap.sse.security.ui.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;

import com.sap.sse.ServerInfo;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.common.media.TakedownNoticeRequestContext;
import com.sap.sse.security.Action;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.PermissionChecker;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.UnauthorizedException;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.OwnershipDTO;
import com.sap.sse.security.shared.dto.RoleDefinitionDTO;
import com.sap.sse.security.shared.dto.StrippedUserDTO;
import com.sap.sse.security.shared.dto.StrippedUserGroupDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.shared.dto.UserGroupDTO;
import com.sap.sse.security.shared.dto.WildcardPermissionWithSecurityDTO;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.PermissionAndRoleAssociation;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.UserActions;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.impl.UserGroupImpl;
import com.sap.sse.security.ui.client.UserManagementWriteService;
import com.sap.sse.security.ui.oauth.client.CredentialDTO;
import com.sap.sse.security.ui.oauth.shared.OAuthException;
import com.sap.sse.security.ui.shared.SuccessInfo;
import com.sap.sse.util.HttpRequestUtils;

public class UserManagementWriteServiceImpl extends UserManagementServiceImpl implements UserManagementWriteService {
    private static final long serialVersionUID = -8123229851467370537L;
    private static final Logger logger = Logger.getLogger(UserManagementWriteServiceImpl.class.getName());

    @Override
    public OwnershipDTO setOwnership(final String username, final UUID userGroupId,
            final QualifiedObjectIdentifier idOfOwnedObject, final String displayNameOfOwnedObject) {
        SecurityUtils.getSubject()
                .checkPermission(idOfOwnedObject.getStringPermission(DefaultActions.CHANGE_OWNERSHIP));
        final User user = getSecurityService().getUserByName(username);
        // no security check if current user can see the user associated with the given username
        final Ownership result = getSecurityService().setOwnership(idOfOwnedObject, user,
                getSecurityService().getUserGroup(userGroupId), displayNameOfOwnedObject);
        return securityDTOFactory.createOwnershipDTO(result, new HashMap<>(), new HashMap<>());
    }

    @Override
    public RoleDefinitionDTO createRoleDefinition(String roleDefinitionIdAsString, String name) {
        RoleDefinition role = getSecurityService().setOwnershipWithoutCheckPermissionForObjectCreationAndRevertOnError(
                SecuredSecurityTypes.ROLE_DEFINITION, new TypeRelativeObjectIdentifier(roleDefinitionIdAsString), name,
                new Callable<RoleDefinition>() {
                    @Override
                    public RoleDefinition call() throws Exception {
                        return getSecurityService().createRoleDefinition(UUID.fromString(roleDefinitionIdAsString),
                                name);
                    }
                });
        return securityDTOFactory.createRoleDefinitionDTO(role, getSecurityService());
    }

    @Override
    public void deleteRoleDefinition(String roleIdAsString) {
        RoleDefinition role = getSecurityService().getRoleDefinition(UUID.fromString(roleIdAsString));
        if (role != null) {
            getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(role, new Action() {
                @Override
                public void run() throws Exception {
                    for (User user : getSecurityService().getUserList()) {
                        HashSet<Role> nonConcurrentModificationCopy = new HashSet<>();
                        Util.addAll(user.getRoles(), nonConcurrentModificationCopy);
                        for (Role roleInstance : nonConcurrentModificationCopy) {
                            if (roleInstance.getRoleDefinition().equals(role)) {
                                TypeRelativeObjectIdentifier associationTypeIdentifier = PermissionAndRoleAssociation.get(roleInstance, user);
                                QualifiedObjectIdentifier qualifiedTypeIdentifier = SecuredSecurityTypes.ROLE_ASSOCIATION
                                        .getQualifiedObjectIdentifier(associationTypeIdentifier);
                                getSecurityService().deleteAllDataForRemovedObject(qualifiedTypeIdentifier);
                                getSecurityService().removeRoleFromUser(user, roleInstance);
                            }
                        }
                    }
                    for (UserGroup group : getSecurityService().getUserGroupList()) {
                        if (group.isRoleAssociated(role)) {
                            getSecurityService().removeRoleDefintionFromUserGroup(group, role);
                        }
                    }
                    getSecurityService().deleteRoleDefinition(role);
                }
            });
        }
    }

    @Override
    public void updateRoleDefinition(RoleDefinitionDTO roleDefinitionWithNewProperties) throws UnauthorizedException {
        SecurityUtils.getSubject().checkPermission(SecuredSecurityTypes.ROLE_DEFINITION.getStringPermissionForObject(
                DefaultActions.UPDATE, roleDefinitionWithNewProperties));
        RoleDefinition existingRole = getSecurityService().getRoleDefinition(roleDefinitionWithNewProperties.getId());
        if (existingRole == null) {
            throw new UnauthorizedException("Role does not exist");
        }
        Set<WildcardPermission> addedPermissions = new HashSet<>(roleDefinitionWithNewProperties.getPermissions());
        addedPermissions.removeAll(existingRole.getPermissions());
        if (!getSecurityService().hasUserAllWildcardPermissionsForAlreadyRealizedQualifications(existingRole, addedPermissions)) {
            throw new UnauthorizedException("Not permitted to grant permissions for role "
                    + roleDefinitionWithNewProperties.getName());
        }
        Set<WildcardPermission> removedPermissions = new HashSet<>(existingRole.getPermissions());
        removedPermissions.removeAll(roleDefinitionWithNewProperties.getPermissions());
        if (!getSecurityService().hasUserAllWildcardPermissionsForAlreadyRealizedQualifications(existingRole, removedPermissions)) {
            throw new UnauthorizedException("Not permitted to revoke permissions for role "
                    + roleDefinitionWithNewProperties.getName());
        }
        getSecurityService().updateRoleDefinition(roleDefinitionWithNewProperties);
    }

    @Override
    public UserGroupDTO createUserGroup(String name) throws UnauthorizedException, UserGroupManagementException {
        UUID newTenantId = UUID.randomUUID();
        final UserGroup userGroupByName = getSecurityService().getUserGroupByName(name);
        if (userGroupByName != null) {
            throw new UserGroupManagementException(
                    String.format("A user group with the name '%s' already exists.", name));
        }
        UserGroup group = getSecurityService().setOwnershipWithoutCheckPermissionForObjectCreationAndRevertOnError(
                SecuredSecurityTypes.USER_GROUP, UserGroupImpl.getTypeRelativeObjectIdentifier(newTenantId), name, () -> {
                    UserGroup userGroup;
                    try {
                        userGroup = getSecurityService().createUserGroup(newTenantId, name);
                    } catch (UserGroupManagementException e) {
                        throw new UserGroupManagementException(e.getMessage());
                    }
                    return userGroup;
                });
        Map<User, StrippedUserDTO> fromOriginalToStrippedDownUser = new HashMap<>();
        Map<UserGroup, StrippedUserGroupDTO> fromOriginalToStrippedDownUserGroup = new HashMap<>();
        return securityDTOFactory.createUserGroupDTOFromUserGroup(group, fromOriginalToStrippedDownUser,
                fromOriginalToStrippedDownUserGroup, getSecurityService());
    }

    @Override
    public SuccessInfo deleteUserGroup(String userGroupIdAsString) throws UnauthorizedException {
        final UUID userGroupId = UUID.fromString(userGroupIdAsString);
        final UserGroup userGroup = getSecurityService().getUserGroup(userGroupId);
        if (userGroup != null) {
            return getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(userGroup, () -> {
                try {
                    getSecurityService().deleteUserGroup(userGroup);
                    return new SuccessInfo(true, "Deleted user group: " + userGroup.getName() + ".",
                            /* redirectURL */ null, null);
                } catch (UserGroupManagementException e) {
                    return new SuccessInfo(false, "Could not delete user group.", /* redirectURL */ null, null);
                }
            });
        } else {
            return new SuccessInfo(false, "Could not delete user group.", /* redirectURL */ null, null);
        }
    }

    @Override
    public void addUserToUserGroup(String userGroupIdAsString, String username)
            throws UnauthorizedException, UserManagementException {
        final UserGroup tenant = getSecurityService().getUserGroup(UUID.fromString(userGroupIdAsString));
        if (SecurityUtils.getSubject().isPermitted(SecuredSecurityTypes.USER_GROUP.getStringPermissionForObject(DefaultActions.UPDATE, tenant))) {
            final User userByName = getSecurityService().getUserByName(username);
            if (userByName == null) {
                throw new UserManagementException("user '" + username + "' not found.");
            }
            if (getSecurityService().hasCurrentUserMetaPermissionsOfRoleDefinitionsWithQualification(
                    tenant.getRoleDefinitionMap().keySet(), new Ownership(null, tenant))) {
                getSecurityService().addUserToUserGroup(tenant, userByName);
            } else {
                throw new UnauthorizedException(
                        "Current user does not have all the meta permissions of the user group the user would be added to");
            }
        } else {
            throw new UnauthorizedException("Not permitted to add user to group");
        }
    }

    @Override
    public void removeUserFromUserGroup(String userGroupIdAsString, String username)
            throws UnauthorizedException {
        final UserGroup userGroup = getSecurityService().getUserGroup(UUID.fromString(userGroupIdAsString));
        if (SecurityUtils.getSubject().isPermitted(
                SecuredSecurityTypes.USER_GROUP.getStringPermissionForObject(DefaultActions.UPDATE, userGroup))) {
            if (getSecurityService().hasCurrentUserMetaPermissionsOfRoleDefinitionsWithQualification(
                    userGroup.getRoleDefinitionMap().keySet(), new Ownership(null, userGroup))) {
                getSecurityService().removeUserFromUserGroup(userGroup, getSecurityService().getUserByName(username));
            } else {
                throw new UnauthorizedException(
                        "Current user does not have all the meta permissions of the user group the user would be removed from");
            }
        } else {
            throw new UnauthorizedException("Not permitted to remove user from group");
        }
    }

    @Override
    public void putRoleDefintionToUserGroup(String userGroupIdAsString, String roleDefinitionIdAsString,
            boolean forAll) throws UnauthorizedException {
        final UserGroup userGroup = getSecurityService().getUserGroup(UUID.fromString(userGroupIdAsString));
        if (SecurityUtils.getSubject().isPermitted(
                SecuredSecurityTypes.USER_GROUP.getStringPermissionForObject(DefaultActions.UPDATE, userGroup))) {
            final RoleDefinition roleDefinition = getSecurityService()
                    .getRoleDefinition(UUID.fromString(roleDefinitionIdAsString));
            if (roleDefinition != null) {
                if (!getSecurityService().hasCurrentUserMetaPermissionsOfRoleDefinitionWithQualification(roleDefinition,
                        new Ownership(null, userGroup))) {
                    throw new UnauthorizedException("Not permitted to add role definition to group");
                }
                logger.info(SecurityUtils.getSubject().getPrincipal().toString()+" added role "+roleDefinition.getName()+
                        " to user group "+userGroup.getName());
                getSecurityService().putRoleDefinitionToUserGroup(userGroup, roleDefinition, forAll);
            }
        } else {
            throw new UnauthorizedException("Not permitted to add role definition to group");
        }
    }

    @Override
    public void removeRoleDefinitionFromUserGroup(String userGroupIdAsString, String roleDefinitionIdAsString)
            throws UnauthorizedException {
        final UserGroup userGroup = getSecurityService().getUserGroup(UUID.fromString(userGroupIdAsString));
        if (SecurityUtils.getSubject().isPermitted(
                SecuredSecurityTypes.USER_GROUP.getStringPermissionForObject(DefaultActions.UPDATE, userGroup))) {
            final RoleDefinition roleDefinition = getSecurityService()
                    .getRoleDefinition(UUID.fromString(roleDefinitionIdAsString));
            if (roleDefinition != null) {
                if (!getSecurityService().hasCurrentUserMetaPermissionsOfRoleDefinitionWithQualification(roleDefinition,
                        new Ownership(null, userGroup))) {
                    throw new UnauthorizedException("Not permitted to remove role definition from group");
                }
                logger.info(SecurityUtils.getSubject().getPrincipal().toString()+" removed role "+roleDefinition.getName()+
                        " from user group "+userGroup.getName());
                getSecurityService().removeRoleDefintionFromUserGroup(userGroup, roleDefinition);
            }
        } else {
            throw new UnauthorizedException("Not permitted to remove role definition from group");
        }
    }

    @Override
    public UserDTO createSimpleUser(final String username, final String email, final String password,
            final String fullName, final String company, final String localeName, final String validationBaseURL)
            throws UserManagementException, MailException, UnauthorizedException {
        final String clientIP = HttpRequestUtils.getClientIP(getThreadLocalRequest());
        User user = getSecurityService().checkPermissionForUserCreationAndRevertOnErrorForUserCreation(username,
                new Callable<User>() {
                    @Override
                    public User call() throws Exception {
                        if (userGroupExists(username + SecurityService.TENANT_SUFFIX)) {
                            throw new UserManagementException(
                                    "User " + username + " already exists, please chose a different username!");
                        }
                        try {
                            User newUser = getSecurityService().createSimpleUser(username, email, password, fullName,
                                    company, getLocaleFromLocaleName(localeName), validationBaseURL,
                                    getSecurityService().getDefaultTenantForCurrentUser(), clientIP,
                                    /* enforce strong password */ true);
                            return newUser;
                        } catch (UserManagementException e) {
                            logger.severe("Error creating user " + username+": "+e.getMessage());
                            throw e;
                        } catch (UserGroupManagementException e) {
                            logger.severe("Error creating user " + username+": "+e.getMessage());
                            throw new UserManagementException(e.getMessage());
                        }
                    }
                });
        return securityDTOFactory.createUserDTOFromUser(user, getSecurityService());
    }

    @Override
    public void updateSimpleUserPassword(final String username, String oldPassword, String passwordResetSecret, String newPassword) throws UserManagementException {
        final User user = getSecurityService().getUserByName(username);
        // Is, e.g., admin is allowed to update the password without knowing the old password and/or secret?
        if (getSecurityService().hasCurrentUserOneOfExplicitPermissions(user, UserActions.FORCE_OVERWRITE_PASSWORD)
        || ((
               // someone knew a username and the correct password for that user
            oldPassword != null && getSecurityService().checkPassword(username, oldPassword)
            || // someone provided the correct password reset secret for the correct username
            (passwordResetSecret != null && getSecurityService().checkPasswordResetSecret(username, passwordResetSecret)))
               // but in any case the user as which they authenticate in one of these ways has to have the UPDATE permission
          && PermissionChecker.isPermitted(user.getPermissionType().getPermission(DefaultActions.UPDATE),
                user, getSecurityService().getAllUser(),
                getSecurityService().getOwnership(user.getIdentifier())==null?null:getSecurityService().getOwnership(user.getIdentifier()).getAnnotation(),
                getSecurityService().getAccessControlList(user.getIdentifier())==null?null:getSecurityService().getAccessControlList(user.getIdentifier()).getAnnotation()))) {
            getSecurityService().updateSimpleUserPassword(username, newPassword);
            sendPasswordChangedMailAsync(username);
        } else {
            throw new UserManagementException(UserManagementException.INVALID_CREDENTIALS);
        }
    }

    @Override
    public UserDTO updateUserProperties(final String username, String fullName, String company, String localeName,
            String defaultTenant) throws UserManagementException {
        getSecurityService().checkCurrentUserUpdatePermission(getSecurityService().getCurrentUser());
        getSecurityService().updateUserProperties(username, fullName, company,
                getLocaleFromLocaleName(localeName));
        getSecurityService().setDefaultTenantForCurrentServerForUser(username, UUID.fromString(defaultTenant));
        return securityDTOFactory.createUserDTOFromUser(getSecurityService().getUserByName(username),
                getSecurityService());
    }

    @Override
    public void updateSimpleUserEmail(String username, String newEmail, String validationBaseURL) throws UserManagementException, MailException {
        getSecurityService().checkCurrentUserUpdatePermission(getSecurityService().getCurrentUser());
        getSecurityService().updateSimpleUserEmail(username, newEmail, validationBaseURL);
    }
    
    @Override
    public void resetPassword(String username, String email, String passwordResetBaseURL)
            throws UserManagementException, MailException {
        getSecurityService().checkCurrentUserUpdatePermission(getSecurityService().getCurrentUser());
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
    public SuccessInfo deleteUser(String username) throws UnauthorizedException {
        User user = getSecurityService().getUserByName(username);
        if (user != null) {
            if (!getSecurityService().hasCurrentUserExplicitPermissions(user, DefaultActions.DELETE)) {
                return new SuccessInfo(false, "You are not permitted to delete user " + username,
                        /* redirectURL */ null, null);
            }
            try {
                return getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(user, () -> {
                    try {
                        getSecurityService().deleteUser(username);
                        return new SuccessInfo(true, "Deleted user: " + username + ".", /* redirectURL */ null, null);
                    } catch (UserManagementException e) {
                        return new SuccessInfo(false, "Could not delete user.", /* redirectURL */ null, null);
                    }
                });
            } catch (AuthorizationException e) {
                return new SuccessInfo(false, "You are not permitted to delete user " + username,
                        /* redirectURL */ null, null);
            }
        } else {
            return new SuccessInfo(false, "Could not delete user.", /* redirectURL */ null, null);
        }
    }
    
    @Override
    public Set<SuccessInfo> deleteUsers(Set<String> usernames) throws UnauthorizedException {
        final Set<SuccessInfo> result = new HashSet<>();
        for (String username : usernames) {
            result.add(deleteUser(username));
        }
        return result;
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

    @Override
    public void setPreference(String username, String key, String value) throws UserManagementException, UnauthorizedException {
        getSecurityService().checkCurrentUserUpdatePermission(getSecurityService().getUserByName(username));
        getSecurityService().setPreference(username, key, value);
    }

    @Override
    public void setPreferences(String username, Map<String, String> keyValuePairs)
            throws UserManagementException, UnauthorizedException {
        getSecurityService().checkCurrentUserUpdatePermission(getSecurityService().getUserByName(username));
        for (Entry<String, String> entry : keyValuePairs.entrySet()) {
            getSecurityService().setPreference(username, entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void unsetPreference(String username, String key) throws UserManagementException, UnauthorizedException {
        getSecurityService().checkCurrentUserUpdatePermission(getSecurityService().getUserByName(username));
        getSecurityService().unsetPreference(username, key);
    }


    @Override
    public SuccessInfo addRoleToUser(String username, String userQualifierName, UUID roleDefinitionId,
            String tenantQualifierName, boolean transitive) throws UserManagementException, UnauthorizedException {
        SuccessInfo successInfo;
        try {
            // get user for which to add a role
            final User user = getOrThrowUser(username);
            // get user for which the role is qualified, if one exists
            getOrThrowQualifiedUser(userQualifierName);
            // get the group tenant the role is qualified for if one exists
            final UserGroup tenant = getOrThrowTenant(tenantQualifierName);
            final Role role = getSecurityService().getOrThrowRoleFromIDsAndCheckMetaPermissions(roleDefinitionId, tenant == null ? null : tenant.getId(), userQualifierName, transitive);
            final TypeRelativeObjectIdentifier associationTypeIdentifier = PermissionAndRoleAssociation.get(role, user);
            final String message = "User "+SecurityUtils.getSubject().getPrincipal()+" added role " + role.getName() + " for user " + username;
            getSecurityService().setOwnershipWithoutCheckPermissionForObjectCreationAndRevertOnError(
                    SecuredSecurityTypes.ROLE_ASSOCIATION, associationTypeIdentifier,
                    associationTypeIdentifier.toString(), new Action() {
                        @Override
                        public void run() throws Exception {
                            final QualifiedObjectIdentifier qualifiedObjectAssociationIdentifier = SecuredSecurityTypes.ROLE_ASSOCIATION
                                    .getQualifiedObjectIdentifier(associationTypeIdentifier);
                            getSecurityService().addToAccessControlList(qualifiedObjectAssociationIdentifier,
                                    null, DefaultActions.READ.name());
                            getSecurityService().addRoleForUser(user, role);
                            logger.info(message);
                        }
                    });
            final UserDTO userDTO = securityDTOFactory.createUserDTOFromUser(user, getSecurityService());
            successInfo = new SuccessInfo(true, message, /* redirectURL */null,
                    new Triple<>(userDTO, getAllUser(), getServerInfo()));
        } catch (UserManagementException e) {
            successInfo = new SuccessInfo(false,
                    "You are not allowed to grant this role for user " + username
                            + " or the username, group name or role name did not exist.",
                    /* redirectURL */ null, /* userDTO */ null);
        }
        return successInfo;
    }

    @Override
    public SuccessInfo removeRoleFromUser(String username, String userQualifierName, UUID roleDefinitionId,
            String tenantQualifierName, Boolean isTransitive) throws UserManagementException, UnauthorizedException {
        SuccessInfo successInfo;
        try {
            // get user for which to remove role
            final User user = getOrThrowUser(username);
            // get user for which the role is qualified, if one exists
            getOrThrowQualifiedUser(userQualifierName);
            // get the group tenant the role is qualified for if one exists
            UserGroup tenant = getOrThrowTenant(tenantQualifierName);
            Role role = getSecurityService().getOrThrowRoleFromIDsAndCheckMetaPermissions(roleDefinitionId, tenant == null ? null : tenant.getId(), userQualifierName, isTransitive);
            final String message = SecurityUtils.getSubject().getPrincipal().toString()+" removed role " + role.getName() + " for user " + username;
            final TypeRelativeObjectIdentifier associationTypeIdentifier = PermissionAndRoleAssociation.get(role, user);
            final QualifiedObjectIdentifier qualifiedTypeIdentifier = SecuredSecurityTypes.ROLE_ASSOCIATION
                    .getQualifiedObjectIdentifier(associationTypeIdentifier);
            getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(qualifiedTypeIdentifier,
                    new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            getSecurityService().removeRoleFromUser(user, role);
                            logger.info(message);
                            return null;
                        }
                    });
            final UserDTO userDTO = securityDTOFactory.createUserDTOFromUser(user, getSecurityService());
            successInfo = new SuccessInfo(true, message, /* redirectURL */null,
                    new Triple<>(userDTO, getAllUser(), getServerInfo()));
        } catch (UserManagementException e) {
            successInfo = new SuccessInfo(false,
                    "You are not allowed to revoke this role from user " + username
                            + " or the username, group name or role name did not exist.",
                    /* redirectURL */ null, /* userDTO */ null);
        }
        return successInfo;
    }

    @Override
    public SuccessInfo addPermissionForUser(String username, WildcardPermission permission)
            throws UnauthorizedException {
        SuccessInfo successInfo;
        try {
            // check if user exists
            User user = getOrThrowUser(username);
            // check permissions
            if (!getSecurityService().hasCurrentUserMetaPermissionWithOwnershipLookup(permission)) {
                throw new UnauthorizedException(
                        "Not permitted to grant/revoke permission " + permission + " for user " + user.getName());
            }
            // grant permission
            final TypeRelativeObjectIdentifier associationTypeIdentifier = PermissionAndRoleAssociation.get(permission, user);
            final String message = "User "+SecurityUtils.getSubject().getPrincipal()+" added permission " + permission + " for user " + username;
            getSecurityService().setOwnershipWithoutCheckPermissionForObjectCreationAndRevertOnError(
                    SecuredSecurityTypes.PERMISSION_ASSOCIATION, associationTypeIdentifier,
                    associationTypeIdentifier.toString(), new Action() {
                        @Override
                        public void run() throws Exception {
                            final QualifiedObjectIdentifier qualifiedObjectAssociationIdentifier = SecuredSecurityTypes.PERMISSION_ASSOCIATION
                                    .getQualifiedObjectIdentifier(associationTypeIdentifier);
                            getSecurityService().addToAccessControlList(qualifiedObjectAssociationIdentifier,
                                    null, DefaultActions.READ.name());
                            getSecurityService().addPermissionForUser(username, permission);
                            logger.info(message);
                        }
                    });
            final UserDTO userDTO = securityDTOFactory.createUserDTOFromUser(user, getSecurityService());
            successInfo = new SuccessInfo(true, message, /* redirectURL */null,
                    new Triple<>(userDTO, getAllUser(), getServerInfo()));
        } catch (UserManagementException | UnauthorizedException e) {
            successInfo = new SuccessInfo(false, "User "+SecurityUtils.getSubject().getPrincipal()+
                    " is not permitted to grant permission " + permission + " to user "
                    + username + " or the user or permission did not exist.", /* redirectURL */null, null);
        }
        return successInfo;
    }

    @Override
    public SuccessInfo removePermissionFromUser(String username, WildcardPermissionWithSecurityDTO permission)
            throws UnauthorizedException {
        SuccessInfo successInfo;
        try {
            // check if user exists
            User user = getOrThrowUser(username);
            // check permissions
            if (!getSecurityService().hasCurrentUserMetaPermissionWithOwnershipLookup(permission)) {
                throw new UnauthorizedException(
                        "Not permitted to grant/revoke permission " + permission + " for user " + user.getName());
            }
            // revoke permission
            final String message = "User "+SecurityUtils.getSubject().getPrincipal()+" revoked permission " + permission + " for user " + username;
            final TypeRelativeObjectIdentifier associationTypeIdentifier = PermissionAndRoleAssociation.get(permission,
                    user);
            final QualifiedObjectIdentifier qualifiedTypeIdentifier = SecuredSecurityTypes.PERMISSION_ASSOCIATION
                    .getQualifiedObjectIdentifier(associationTypeIdentifier);
            getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(qualifiedTypeIdentifier,
                    new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            getSecurityService().removePermissionFromUser(username, permission);
                            logger.info(message);
                            return null;
                        }
                    });
            final UserDTO userDTO = securityDTOFactory.createUserDTOFromUser(user, getSecurityService());
            successInfo = new SuccessInfo(true, message, /* redirectURL */null,
                    new Triple<>(userDTO, getAllUser(), getServerInfo()));
        } catch (UserManagementException e) {
            successInfo = new SuccessInfo(false, "Not permitted to revoke permission " + permission + " for user "
                    + username + " or the user or permission did not exist", /* redirectURL */null, null);
        }
        return successInfo;
    }

    protected void sendPasswordChangedMailAsync(final String username) {
        new Thread("sending updated password to user "+username+" by e-mail") {
            @Override public void run() {
                try {
                    getSecurityService().sendMail(username, "Password Changed", "Somebody changed your password for your user named "+username+".\nIf that wasn't you, please contact support@sapsailing.com via email.");
                } catch (MailException e) {
                    logger.log(Level.SEVERE, "Error sending new password to user "+username+" by e-mail", e);
                }
            }
        }.start();
    }

    protected Locale getLocaleFromLocaleName(String localeName) {
        try {
            return localeName == null || localeName.isEmpty() ? null : Locale.forLanguageTag(localeName);
        } catch (Exception e) {
            logger.log(Level.WARNING, e, () -> "Error while parsing locale with name '" + localeName + "'");
            return null;
        }
    }

    /**
     * @returns the user associated with the userQualifierName or null
     * @throws UserManagementException
     *             if the userQualifierName is not empty or null but no user was found.
     */
    protected User getOrThrowQualifiedUser(String userQualifierName) throws UserManagementException {
        User user = getSecurityService().getUserByName(userQualifierName);
        if (userQualifierName != null && !userQualifierName.isEmpty() && user == null) {
            throw new UserManagementException("User " + userQualifierName + " not found.");
        }
        return user;
    }

    /**
     * @return the user group associated with the tenantQualifierName
     * @throws UserManagementException,
     *             if the tenantQualifierName was not empty or null but did not yield a valid user group
     */
    protected UserGroup getOrThrowTenant(String tenantQualifierName) throws UserManagementException {
        final UserGroup tenant;
        if (tenantQualifierName == null || tenantQualifierName.trim().isEmpty()) {
            tenant = null;
        } else {
            tenant = getSecurityService().getUserGroupByName(tenantQualifierName);
            if (tenant == null) {
                throw new UserManagementException("Tenant not found: " + tenantQualifierName);
            }
        }
        return tenant;
    }

    /**
     * @return the User associated with the username or a {@link UserManagementException}, if the user is null
     */
    protected User getOrThrowUser(String username) throws UserManagementException {
        final User user = getSecurityService().getUserByName(username);
    
        if (user == null) {
            throw new UserManagementException("user " + username + " not found.");
        }
        return user;
    }

    @Override
    public AccessControlListDTO overrideAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject,
            AccessControlListDTO acl) throws UnauthorizedException {
        if (SecurityUtils.getSubject()
                .isPermitted(idOfAccessControlledObject.getStringPermission(DefaultActions.CHANGE_ACL))) {
            final Map<UserGroup, Set<String>> aclActionsByGroup = new HashMap<>();
            for (Entry<StrippedUserGroupDTO, Set<String>> entry : acl.getActionsByUserGroup().entrySet()) {
                final StrippedUserGroupDTO groupDTO = entry.getKey();
                final UserGroup userGroup;
                if (groupDTO == null) {
                    userGroup = null;
                } else {
                    userGroup = getSecurityService().getUserGroup(groupDTO.getId());
                }
                aclActionsByGroup.put(userGroup, entry.getValue());
            }
            return securityDTOFactory.createAccessControlListDTO(getSecurityService()
                    .overrideAccessControlList(idOfAccessControlledObject, aclActionsByGroup));
        } else {
            throw new UnauthorizedException("Not permitted to update the ACL for a user");
        }
    }

    @Override
    public void setCORSFilterConfigurationToWildcard() {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CONFIGURE_CORS_FILTER);
        getSecurityService().setCORSFilterConfigurationToWildcard(ServerInfo.getName());
    }

    @Override
    public void setCORSFilterConfigurationAllowedOrigins(ArrayList<String> allowedOrigins) {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CONFIGURE_CORS_FILTER);
        getSecurityService().setCORSFilterConfigurationAllowedOrigins(ServerInfo.getName(), allowedOrigins.toArray(new String[0]));
    }
    
    @Override
    public void fileTakedownNotice(TakedownNoticeRequestContext takedownNoticeRequestContext) throws MailException {
        getSecurityService().fileTakedownNotice(takedownNoticeRequestContext);
    }
}
