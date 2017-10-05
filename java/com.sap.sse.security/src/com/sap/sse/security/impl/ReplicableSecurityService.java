package com.sap.sse.security.impl;

import java.util.Set;

import com.sap.sse.security.SecurityService;
import com.sap.sse.security.User;
import com.sap.sse.security.UserGroup;
import com.sap.sse.security.shared.TenantManagementException;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;

/**
 * Publishes those methods of {@link SecurityServiceImpl} that are required by operations implemented as lambda
 * expressions to fulfill their tasks. These operations should not be invoked by external service clients.
 * {@link SecurityService} is the one registered with the OSGi registry and thus the publicly-visible
 * interface.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface ReplicableSecurityService extends SecurityService {
    Void internalCreateAcl(String id);
    
    Void internalAclPutPermissions(String id, String group, Set<String> permissions);
    
    Void internalAclAddPermission(String id, String group, String permission);
    
    Void internalAclRemovePermission(String id, String group, String permission);
    
    Void internalCreateOwnership(String id, String owner, String tenantOwner);
    
    Void internalCreateUserGroup(String name) throws UserGroupManagementException;
    
    Void internalCreateTenant(String name) throws TenantManagementException, UserGroupManagementException;
    
    Void internalUpdateUserGroup(UserGroup group);
    
    Void internalDeleteUserGroup(String name) throws UserGroupManagementException;
    
    Void internalDeleteTenant(String name) throws TenantManagementException;
    
    Void internalStoreUser(User user);

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

    Void internalAddRoleForUser(String username, String role) throws UserManagementException;

    Void internalRemoveRoleFromUser(String username, String role) throws UserManagementException;

    Void internalAddPermissionForUser(String username, String permissionToAdd) throws UserManagementException;

    Void internalRemovePermissionForUser(String username, String permissionToRemove) throws UserManagementException;

    Void internalDeleteUser(String username) throws UserManagementException;

}
