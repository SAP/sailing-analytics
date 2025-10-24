package com.sap.sse.security.ui.client;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.common.media.TakedownNoticeRequestContext;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.OwnershipDTO;
import com.sap.sse.security.shared.dto.RoleDefinitionDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.shared.dto.UserGroupDTO;
import com.sap.sse.security.shared.dto.WildcardPermissionWithSecurityDTO;
import com.sap.sse.security.ui.oauth.client.CredentialDTO;
import com.sap.sse.security.ui.shared.SuccessInfo;

public interface UserManagementWriteServiceAsync extends UserManagementServiceAsync {
    void setOwnership(String username, UUID userGroupId,
            QualifiedObjectIdentifier idOfOwnedObject, String displayNameOfOwnedObject,
            AsyncCallback<OwnershipDTO> callback);

    void overrideAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject, AccessControlListDTO acl,
            AsyncCallback<AccessControlListDTO> updateAclAsyncCallback);

    void createUserGroup(String name, AsyncCallback<UserGroupDTO> callback);
    
    void deleteUserGroup(String userGroupIdAsString, AsyncCallback<SuccessInfo> asyncCallback);

    void addUserToUserGroup(String userGroupIdAsString, String username, AsyncCallback<Void> asyncCallback);

    void removeUserFromUserGroup(String tenantIdAsString, String username, AsyncCallback<Void> asyncCallback);

    void putRoleDefintionToUserGroup(String userGroupIdAsString, String roleDefinitionIdAsString, boolean forAll,
            AsyncCallback<Void> callback);

    void removeRoleDefinitionFromUserGroup(String userGroupIdAsString, String roleDefinitionIdAsString,
            AsyncCallback<Void> callback);

    void createSimpleUser(String name, String email, String password, String fullName, String company,
            String localeName, String validationBaseURL, AsyncCallback<UserDTO> callback);

    void updateSimpleUserPassword(String name, String oldPassword, String passwordResetSecret, String newPassword, AsyncCallback<Void> callback);

    void resetPassword(String username, String eMailAddress, String baseURL, AsyncCallback<Void> callback);
    
    void validateEmail(String username, String validationSecret, AsyncCallback<Boolean> markedAsyncCallback);

    void updateSimpleUserEmail(String username, String newEmail, String validationBaseURL, AsyncCallback<Void> callback);

    void updateUserProperties(String username, String fullName, String company, String localeName,
            String defaultTenantIdAsString, AsyncCallback<UserDTO> callback);

    void createRoleDefinition(String roleDefinitionIdAsString, String name, AsyncCallback<RoleDefinitionDTO> callback);

    void deleteRoleDefinition(String roleDefinitionIdAsString, AsyncCallback<Void> callback);

    void updateRoleDefinition(RoleDefinitionDTO roleWithNewProperties, AsyncCallback<Void> callback);

    void deleteUser(String username, AsyncCallback<SuccessInfo> callback);
    
    void deleteUsers(Set<String> usernames, AsyncCallback<Set<SuccessInfo>> callback);

    void setSetting(String key, String clazz, String setting, AsyncCallback<Void> callback);

    void addSetting(String key, String clazz, String setting, AsyncCallback<Void> callback);

    void setPreference(String username, String key, String value, AsyncCallback<Void> callback);
    
    void setPreferences(String username, Map<String, String> keyValuePairs, AsyncCallback<Void> callback);

    void unsetPreference(String username, String key, AsyncCallback<Void> callback);

    //------------------------------------------------ OAuth Interface ----------------------------------------------------------------------
    void getAuthorizationUrl(CredentialDTO credential, AsyncCallback<String> callback);

    /**
     * Grants the role associated with the given {@code roleDefinitionId}, {@code userQualifierName} and
     * {@code tenantQualifierName} to the given {@code username} if the current user has the required permissions
     */
    void addRoleToUser(String username, String userQualifierName, UUID roleDefinitionId, String tenantQualifierName, boolean transitive,
            AsyncCallback<SuccessInfo> callback);

    /**
     * Revokes the role associated with the given {@code roleDefinitionId}, {@code userQualifierName} and
     * {@code tenantQualifierName} for the given {@code username} if the current user has the required permissions
     */
    void removeRoleFromUser(String username, String userQualifierName, UUID roleDefinitionId,
            String tenantQualifierName,
            Boolean isTransitive, AsyncCallback<SuccessInfo> callback);

    /**
     * Grants the given {@code permission} to the given {@code username} if the current user has the required
     * permissions
     */
    void addPermissionForUser(String username, WildcardPermission permissions,
            AsyncCallback<SuccessInfo> callback);

    /**
     * Revokes the given {@code permission} for the given {@code username} if the current user has the required
     * permissions
     */
    void removePermissionFromUser(String username, WildcardPermissionWithSecurityDTO permission,
            AsyncCallback<SuccessInfo> callback);

    
    /**
     * For the application replica set to which this request is sent, configure its CORS filter such that
     * REST requests are allowed from <em>any</em> origin (*).
     */
    void setCORSFilterConfigurationToWildcard(AsyncCallback<Void> callback);
    
    /**
     * For the application replica set to which this request is sent, configure its CORS filter such that
     * REST requests are allowed from the origins listed in {@code allowedOrigins}. The list may be empty
     * but must not be {@code null}.
     */
    void setCORSFilterConfigurationAllowedOrigins(ArrayList<String> allowedOrigins, AsyncCallback<Void> callback);

    void fileTakedownNotice(TakedownNoticeRequestContext takedownNoticeRequestContext, AsyncCallback<Void> callback);
}
