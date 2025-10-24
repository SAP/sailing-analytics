package com.sap.sse.security.shared;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sap.sse.common.Named;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.shared.impl.LockingAndBanning;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.impl.UserGroupImpl;

/**
 * Keeps track of all {@link User}, {@link UserGroupImpl} and {@link Role}
 * objects persistently; furthermore, aspects such as user access tokens, preferences and
 * settings are stored durably.<p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface BasicUserStore extends UserGroupProvider, Named {
    Iterable<UserGroup> getUserGroups();
    
    UserGroup getUserGroup(UUID groupId);
    
    UserGroup getUserGroupByName(String name);
    
    /**
     * Obtains all user groups that have {@code roleDefinition} as key in their {@link UserGroup#getRoleDefinitionMap() role definition map},
     * regardless of whether for all users or only the members of the group.
     */
    Iterable<UserGroup> getUserGroupsWithRoleDefinition(RoleDefinition roleDefinition);
    
    UserGroup createUserGroup(UUID groupId, String name) throws UserGroupManagementException;

    void addUserGroup(UserGroup group) throws UserGroupManagementException;
    
    void updateUserGroup(UserGroup userGroup);
    
    /**
     * A new, non-live copy, as a snapshot, of the set of users known to this user store
     */
    Iterable<User> getUsers();
    
    boolean hasUsers();

    /**
     * The user with that {@link UserImpl#getName() name} or {@code null} if no such user exists
     */
    User getUserByName(String username);

    /**
     * The user with that {@link UserImpl#getEmail() email} or {@code null} if no such user exists
     */
    User getUserByEmail(String email);
    
    User getUserByAccessToken(String accessToken);

    User createUser(String name, String email, LockingAndBanning lockingAndBanning, Account... accounts)
            throws UserManagementException;

    void addUser(User user) throws UserManagementException;

    void updateUser(User user);

    Iterable<Role> getRolesFromUser(String username) throws UserManagementException;

    void addRoleForUser(String username, Role role) throws UserManagementException;

    void removeRoleFromUser(String username, Role role) throws UserManagementException;

    void removePermissionFromUser(String username, WildcardPermission permission) throws UserManagementException;

    void addPermissionForUser(String username, WildcardPermission permission) throws UserManagementException;

    void deleteUser(String username) throws UserManagementException;

    Iterable<RoleDefinition> getRoleDefinitions();
    RoleDefinition getRoleDefinition(UUID roleDefinitionId);

    RoleDefinition createRoleDefinition(UUID roleDefinitionId, String displayName,
            Iterable<WildcardPermission> permissions);

    void setRoleDefinitionPermissions(UUID roleDefinitionId, Set<WildcardPermission> permissions);
    void addRoleDefinitionPermission(UUID roleDefinitionId, WildcardPermission permission);
    void removeRoleDefinitionPermission(UUID roleDefinitionId, WildcardPermission permission);
    void setRoleDefinitionDisplayName(UUID roleDefinitionId, String displayName);
    void removeRoleDefinition(RoleDefinition roleDefinition);

    /**
     * Registers a settings key together with its type. Calling this method is necessary for {@link #setSetting(String, Object)}
     * to have an effect for <code>key</code>. Calls to {@link #setSetting(String, Object)} will only accept values whose type
     * is compatible with <code>type</code>. Note that the store implementation may impose constraints on the types supported.
     * All store implementations are required to support at least {@link String} and {@link UUID} as types.
     */
    void addSetting(String key, Class<?> type);
    
    void setPreference(String username, String key, String value);
    
    /**
     * Always returns a valid map which may be empty.
     */
    Map<String, String> getAllPreferences(String username);
    
    void unsetPreference(String username, String key);

    String getPreference(String username, String key);
    
    /**
     * Sets a value for a key if that key was previously added to this store using {@link #addSetting(String, Class)}.
     * For user store implementations that maintain their data persistently and make it available after a server
     * restart, it is sufficient to register the settings key once because these registrations will be stored
     * persistently, too.
     * <p>
     * 
     * If the <code>key</code> was not registered before by a call to {@link #addSetting(String, Class)}, or if the
     * <code>setting</code> object does not conform with the type passed to {@link #addSetting(String, Class)}, a call
     * to this method will have no effect and return <code>false</code>.
     * 
     * @Return whether applying the setting was successful; <code>false</code> means that no update was performed to the
     * setting because either the key was not registered before by {@link #addSetting(String, Class)} or the type of the
     * <code>setting</code> object does not conform to the type used in {@link #addSetting(String, Class)}
     */
    boolean setSetting(String key, Object setting);

    <T> T getSetting(String key, Class<T> clazz);

    Map<String, Object> getAllSettings();
    
    Map<String, Class<?>> getAllSettingTypes();

    /**
     * Removes all users and all their preferences and all settings from this store's in-memory representation.
     * For safety reasons and because a replica's DB state is undefined anyhow, leaves persistent content in place.
     * Registered listeners will not be removed automatically.
     * Use with due care.
     */
    void clear();

    /**
     * Stores an access token that can be used to authenticate the user identified by <code>username</code>.
     * If there is no user by that name, calling this method has no effect and it will return <code>false</code>.
     * 
     * @return whether a user could be identified by <code>username</code>
     */
    boolean setAccessToken(String username, String accessToken);

    void removeAccessToken(String username);

    /**
     * The owner and any subject having the {@link DefaultRoles#ADMIN} role can retrieve an existing
     * authentication token for the user. {@code null} may result in case for the user identified by
     * {@code username} no access token has previously been {@link #setAccessToken(String, String) set}.
     */
    String getAccessToken(String username);
    
    /**
     * If a valid default tenant name was passed to the constructor, this field will contain a valid
     * {@link UserGroupImpl} object whose name equals that of the default tenant name. It will have been used
     * during role migration where string-based roles are mapped to a corresponding {@link RoleDefinition}
     * and the users with the original role will obtain a corresponding {@link Role} with this default
     * tenant as the {@link Role#getQualifiedForTenant() tenant qualifier}. It is by default used as the
     * group owner of the {@link SecuredSecurityTypes#SERVER} object for the local server/replica set.
     */
    UserGroup getServerGroup();
    
    /**
     * For use after replica initialization / initial load, when the server group has been resolved against
     * the initial load received from the master instance.
     */
    void setServerGroup(UserGroup newServerGroup);
    
    /**
     * The name of the {@link #getServerGroup() server group}.
     */
    String getServerGroupName();

    /**
     * Ensures that the predefined role definitions, particularly the "admin" and the "user" role, exist.
     */
    void ensureDefaultRolesExist();

    /**
     * @return a pair with: <br/>
     *         If A is true, at least one user has an unqualified version of the {@link #roleToCheck} (without tenant or
     *         user qualification). In this case, B is null.<br/>
     *         If A is false, B contains all the ownerships of {@link #roleToCheck}
     */
    Pair<Boolean, Set<Ownership>> getExistingQualificationsForRoleDefinition(RoleDefinition roleToCheck);

    Set<Pair<User, Role>> getRolesQualifiedByUserGroup(UserGroup groupQualification);
}
