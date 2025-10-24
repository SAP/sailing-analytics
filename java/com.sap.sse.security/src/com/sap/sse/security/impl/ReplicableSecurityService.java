package com.sap.sse.security.impl;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.session.Session;

import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.Account;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.LockingAndBanning;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.subscription.Subscription;

/**
 * Publishes those methods of {@link SecurityServiceImpl} that are required by operations implemented as lambda
 * expressions to fulfill their tasks. These operations should not be invoked by external service clients.
 * {@link SecurityService} is the one registered with the OSGi registry and thus the publicly-visible interface.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public interface ReplicableSecurityService extends SecurityService {
    Void internalSetEmptyAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject, String displayName);
    
    Void internalAclPutPermissions(QualifiedObjectIdentifier idOfAccessControlledObject, UUID groupId, Set<String> actions);
    
    Void internalAclAddPermission(QualifiedObjectIdentifier idOfAccessControlledObject, UUID groupId, String action);
    
    Void internalAclRemovePermission(QualifiedObjectIdentifier idOfAccessControlledObject, UUID groupId, String action);
    
    Void internalDeleteAcl(QualifiedObjectIdentifier idOfAccessControlledObject);
    
    Ownership internalSetOwnership(QualifiedObjectIdentifier idOfOwnedObject, String owningUsername, UUID tenantOwnerId, String displayNameOfOwnedObject);
    
    Void internalDeleteOwnership(QualifiedObjectIdentifier idOfOwnedObject);
    
    Void internalCreateUserGroup(UUID groupId, String name) throws UserGroupManagementException;
    
    Void internalDeleteUserGroup(UUID groupId) throws UserGroupManagementException;
    
    Void internalAddUserToUserGroup(UUID groupId, String username) throws UserGroupManagementException;

    Void internalRemoveUserFromUserGroup(UUID groupId, String username) throws UserGroupManagementException;
    
    Void internalPutRoleDefinitionToUserGroup(UUID groupId, UUID roleDefinitionId, boolean forAll)
            throws UserGroupManagementException;

    Void internalRemoveRoleDefinitionFromUserGroup(UUID groupId, UUID roleDefinitionId)
            throws UserGroupManagementException;

    /**
     * Creates and stores a <em>new</em> user in the system. <em>Don't</em> use this to update an existing user.
     * Use other {@code internal...} methods to update individual user properties instead.
     */
    User internalCreateUser(String username, String email, Account... accounts) throws UserManagementException;

    Void internalUpdateSimpleUserEmail(String username, String newEmail, String validationSecret);

    Void internalUpdateSimpleUserPassword(String username, byte[] salt, String hashedPasswordBase64);

    Void internalUpdateUserProperties(String username, String fullName, String company, Locale locale);

    Boolean internalValidateEmail(String username, String validationSecret);

    Void internalSetPreference(String username, String key, String value);

    /**
     * @return the {@link String}-ified preference object value
     */
    String internalSetPreferenceObject(String username, String key, Object value);

    Void internalUnsetPreference(String username, String key);
    
    Void internalSetAccessToken(String username, String accessToken);

    Void internalRemoveAccessToken(String username);

    Boolean internalSetSetting(String key, Object setting);

    Void internalAddSetting(String key, Class<?> clazz);

    Void internalAddRoleForUser(String username, UUID roleDefinitionId, UUID idOfTenantQualifyingRole, String nameOfUserQualifyingRole, Boolean transitive) throws UserManagementException;

    Void internalRemoveRoleFromUser(String username, UUID roleDefinitionId, UUID idOfTenantQualifyingRole, String nameOfUserQualifyingRole, Boolean transitive) throws UserManagementException;

    Void internalAddPermissionForUser(String username, WildcardPermission permissionToAdd) throws UserManagementException;

    Void internalRemovePermissionForUser(String username, WildcardPermission permissionToRemove) throws UserManagementException;

    Void internalDeleteUser(String username) throws UserManagementException;

    RoleDefinition internalCreateRoleDefinition(UUID roleDefinitionId, String name);
    
    Void internalDeleteRoleDefinition(UUID roleDefinitionId);
    
    Void internalUpdateRoleDefinition(RoleDefinition roleDefinitionWithNewProperties);

    void storeSession(String cacheName, Session value);

    void removeSession(String cacheName, Session result);

    void removeAllSessions(String cacheName);

    Void internalSetDefaultTenantForServerForUser(String username, UUID defaultTenantId, String serverName);

    Void internalResetPassword(String username, String passwordResetSecret);

    Void internalUpdateSubscription(String username, Subscription newSubscription) throws UserManagementException;
    
    Void internalUpdateSubscriptionPlanPrices(Map<String, BigDecimal> updatedItemPrices);

    Void internalSetCORSFilterConfigurationToWildcard(String serverName);

    Void internalSetCORSFilterConfigurationAllowedOrigins(String serverName, String... allowedOrigins);

    LockingAndBanning internalFailedPasswordAuthentication(String username);

    Boolean internalSuccessfulPasswordAuthentication(String username);

    Boolean internalSuccessfulBearerTokenAuthentication(String clientIP);

    LockingAndBanning internalFailedBearerTokenAuthentication(String clientIP);

    LockingAndBanning internalRecordUserCreationFromClientIP(String clientIP);
}
