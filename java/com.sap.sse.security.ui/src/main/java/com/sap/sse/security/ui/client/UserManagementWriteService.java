package com.sap.sse.security.ui.client;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sap.sse.common.mail.MailException;
import com.sap.sse.common.media.TakedownNoticeRequestContext;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.UnauthorizedException;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.OwnershipDTO;
import com.sap.sse.security.shared.dto.RoleDefinitionDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.shared.dto.UserGroupDTO;
import com.sap.sse.security.shared.dto.WildcardPermissionWithSecurityDTO;
import com.sap.sse.security.ui.oauth.client.CredentialDTO;
import com.sap.sse.security.ui.oauth.shared.OAuthException;
import com.sap.sse.security.ui.shared.SuccessInfo;

public interface UserManagementWriteService extends UserManagementService {

    OwnershipDTO setOwnership(String username, UUID userGroupId, QualifiedObjectIdentifier idOfOwnedObject,
            String displayNameOfOwnedObject) throws org.apache.shiro.authz.UnauthorizedException;

    UserGroupDTO createUserGroup(String name)
            throws UserGroupManagementException, UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    SuccessInfo deleteUserGroup(String userGroupIdAsString)
            throws UserGroupManagementException, UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    void addUserToUserGroup(String userGroupIdAsString, String username)
            throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException, UserManagementException;

    void removeUserFromUserGroup(String userGroupIdAsString, String user)
            throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    void putRoleDefintionToUserGroup(String userGroupIdAsString, String roleDefinitionIdAsString, boolean forAll)
            throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    void removeRoleDefinitionFromUserGroup(String userGroupIdAsString, String roleDefinitionIdAsString)
            throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    RoleDefinitionDTO createRoleDefinition(String roleDefinitionIdAsString, String name)
            throws org.apache.shiro.authz.UnauthorizedException;

    void deleteRoleDefinition(String roleDefinitionIdAsString) throws org.apache.shiro.authz.UnauthorizedException;

    void updateRoleDefinition(RoleDefinitionDTO roleWithNewProperties)
            throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    UserDTO createSimpleUser(String name, String email, String password, String fullName, String company,
            String localeName, String validationBaseURL) throws UserManagementException, MailException,
            UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    /**
     * Either <code>oldPassword</code> or <code>passwordResetSecret</code> need to be provided, or the current user
     * needs to have the {@link DefaultRoles#ADMIN} role to be able to set the new password.
     */
    void updateSimpleUserPassword(String name, String oldPassword, String passwordResetSecret, String newPassword)
            throws UserManagementException, org.apache.shiro.authz.UnauthorizedException;

    void updateSimpleUserEmail(String username, String newEmail, String validationBaseURL)
            throws UserManagementException, MailException, org.apache.shiro.authz.UnauthorizedException;

    UserDTO updateUserProperties(String username, String fullName, String company, String localeName,
            String defaultTenantIdAsString)
            throws UserManagementException, org.apache.shiro.authz.UnauthorizedException;

    void resetPassword(String username, String eMailAddress, String baseURL)
            throws UserManagementException, MailException, org.apache.shiro.authz.UnauthorizedException;

    boolean validateEmail(String username, String validationSecret)
            throws UserManagementException, org.apache.shiro.authz.UnauthorizedException;

    SuccessInfo deleteUser(String username) throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    Set<SuccessInfo> deleteUsers(Set<String> usernames)
            throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    void setSetting(String key, String clazz, String setting) throws org.apache.shiro.authz.UnauthorizedException;

    void addSetting(String key, String clazz, String setting) throws org.apache.shiro.authz.UnauthorizedException;

    /**
     * Permitted only for users with role {@link DefaultRoles#ADMIN} or when the subject's user name matches
     * <code>username</code>.
     * 
     * @param key
     *            must not be <code>null</code>
     * @param value
     *            must not be <code>null</code>
     * @throws UserManagementException
     */
    void setPreference(String username, String key, String value)
            throws UserManagementException, UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    void setPreferences(String username, Map<String, String> keyValuePairs)
            throws UserManagementException, UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    /**
     * Permitted only for users with role {@link DefaultRoles#ADMIN} or when the subject's user name matches
     * <code>username</code>.
     */
    void unsetPreference(String username, String key)
            throws UserManagementException, UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;
    
    void setCORSFilterConfigurationToWildcard();

    void setCORSFilterConfigurationAllowedOrigins(ArrayList<String> allowedOrigins);

    void fileTakedownNotice(TakedownNoticeRequestContext takedownNoticeRequestContext) throws MailException;
    
    // ------------------------------------------------ OAuth Interface
    // --------------------------------------------------------------
    public String getAuthorizationUrl(CredentialDTO credential)
            throws OAuthException, org.apache.shiro.authz.UnauthorizedException;

    SuccessInfo addRoleToUser(String username, String userQualifierName, UUID roleDefinitionId,
            String tenantQualifierName, boolean transitive)
            throws UserManagementException, UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    SuccessInfo removeRoleFromUser(String username, String userQualifierName, UUID roleDefinitionId,
            String tenantQualifierName, Boolean isTransitive)
            throws UserManagementException, UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    SuccessInfo addPermissionForUser(String username, WildcardPermission permissions)
            throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    SuccessInfo removePermissionFromUser(String username, WildcardPermissionWithSecurityDTO permissions)
            throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    AccessControlListDTO overrideAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject,
            AccessControlListDTO acl) throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;
}
