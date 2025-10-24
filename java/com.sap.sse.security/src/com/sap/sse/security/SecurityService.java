package com.sap.sse.security;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.osgi.framework.BundleContext;

import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.http.HttpHeaderUtil;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.common.media.TakedownNoticeRequestContext;
import com.sap.sse.replication.ReplicableWithObjectInputStream;
import com.sap.sse.security.impl.ReplicableSecurityService;
import com.sap.sse.security.impl.SecurityServiceImpl;
import com.sap.sse.security.interfaces.Credential;
import com.sap.sse.security.interfaces.PreferenceConverter;
import com.sap.sse.security.interfaces.UserImpl;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.operations.SecurityOperation;
import com.sap.sse.security.persistence.PersistenceFactory;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.BasicUserStore;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.HasPermissionsProvider;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.PermissionChecker;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.RolePrototype;
import com.sap.sse.security.shared.SubscriptionPlanProvider;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.LockingAndBanning;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.subscription.Subscription;
import com.sap.sse.security.shared.subscription.SubscriptionPlan;
import com.sap.sse.shared.classloading.ClassLoaderRegistry;
import com.sap.sse.util.HttpRequestUtils;

/**
 * A service interface for security management. Intended to be used as an OSGi service that can be registered, e.g., by
 * {@link BundleContext#registerService(Class, Object, java.util.Dictionary)} and can be discovered by other bundles.<p>
 * 
 * Permission checks will throw a {@link org.apache.shiro.authz.AuthorizationException} in case the check fails.
 * 
 * @author Axel Uhl (D043530)
 * @author Benjamin Ebling
 *
 */
public interface SecurityService extends ReplicableWithObjectInputStream<ReplicableSecurityService, SecurityOperation<?>> {
    interface RoleCopyListener {
        void onRoleCopy(User a, Role existingRole, Role copyRole);
    }

    String ALL_USERNAME = "<all>";
    String TENANT_SUFFIX = "-tenant";
    String REPLICABLE_FULLY_QUALIFIED_CLASSNAME = SecurityServiceImpl.class.getName();
    /**
     * The default locking duration per client IP address for user creation.
     * 
     * @see HttpRequestUtils#getClientIP(HttpServletRequest)
     */
    Duration DEFAULT_CLIENT_IP_BASED_USER_CREATION_LOCKING_DURATION = Duration.ONE_MINUTE;

    SecurityManager getSecurityManager();

    /**
     * Return the ownership information for the object identified by {@code idOfOwnedObject}. If there is no ownership
     * information for that object {@code null} is returned.
     */
    OwnershipAnnotation getOwnership(QualifiedObjectIdentifier idOfOwnedObject);
    
    Iterable<AccessControlListAnnotation> getAccessControlLists();

    AccessControlListAnnotation getAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject);

    AccessControlList overrideAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject,
            Map<UserGroup, Set<String>> permissionMap);

    AccessControlList overrideAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject,
            Map<UserGroup, Set<String>> permissionMap, String displayNameOfAccessControlledObject);

    /**
     * @param name The name of the user group to add
     */
    AccessControlList addToAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject, UserGroup userGroup,
            String action);

    /**
     * @param name The name of the user group to remove
     */ 
    AccessControlList removeFromAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject, UserGroup group,
            String action);

    void deleteAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject);

    /**
     * Same as {@link #setOwnership(String, UserImpl, Tenant, String)}, leaving the display name
     * of the object owned undefined.
     */
    Ownership setOwnership(QualifiedObjectIdentifier idOfOwnedObject, User userOwner, UserGroup tenantOwner);

    /**
     * @param idOfOwnedObject
     *            the ID of the object for which ownership is declared
     * @param userOwner
     *            the user to become the owning user of the object with ID
     *            {@code idOfOwnedObject}
     * @param tenantOwner
     *            the tenant to become owning tenant of the object with ID {@code idOfOwnedObject}
     * @param displayNameOfOwnedObject
     *            a display name that this store can use to produce a user-readable hint regarding the ownership
     *            definition that this call creates; there is no guarantee that the display name will remain up to date
     *            as the object identified by {@link idOfOwnedObject} may change its name without notifying this
     *            store
     */
    Ownership setOwnership(QualifiedObjectIdentifier idOfOwnedObject, User userOwner, UserGroup tenantOwner,
            String displayNameOfOwnedObject);

    void deleteOwnership(QualifiedObjectIdentifier idOfOwnedObject);

    Iterable<UserGroup> getUserGroupList();

    /**
     * @see BasicUserStore#getUserGroupsWithRoleDefinition(RoleDefinition)
     */
    Iterable<UserGroup> getUserGroupsWithRoleDefinition(RoleDefinition roleDefinition);
    
    UserGroup getUserGroup(UUID id);

    UserGroup getUserGroupByName(String name);

    Iterable<UserGroup> getUserGroupsOfUser(User user);

    /**
     * Creates a user group with the given {@code id} and {@code name} and makes the calling subject
     * its owner and a member of the group and assigns the {@code user} role qualified to the new
     * group to the calling subject ({@code user:{name}}) as a "transitive" role assignment, allowing
     * the user to grant that role also to other users, in turn.
     */
    UserGroup createUserGroup(UUID id, String name) throws UserGroupManagementException;
    
    void addUserToUserGroup(UserGroup group, User user);
    
    void removeUserFromUserGroup(UserGroup group, User user);

    void putRoleDefinitionToUserGroup(UserGroup group, RoleDefinition roleDefinition, boolean forAll);

    void removeRoleDefintionFromUserGroup(UserGroup group, RoleDefinition roleDefinition);

    void deleteUserGroup(UserGroup userGroup) throws UserGroupManagementException;

    Iterable<User> getUserList();

    User getUserByName(String username);

    User getUserByEmail(String email);
    
    /**
     * Finds all users that have the {@code permission}. This doesn't have to be an explicit permission assignment on
     * the {@link User} object (see {@link User#getPermissions()}) but can also be implied, e.g., by a
     * {@link User#getRoles() role assignment} that the user has, or by a group membership of the user where the group
     * {@link UserGroup#getRoleDefinitionMap() has roles assigned for members of the group}.
     */
    Iterable<User> getUsersWithPermissions(WildcardPermission permission);

    User getCurrentUser();

    /**
     * Returns the redirect URL
     */
    String login(String username, String password) throws UserManagementException;

    String getAuthenticationUrl(Credential credential) throws UserManagementException;

    User verifySocialUser(Credential credential) throws UserManagementException;

    void logout();

    /**
     * This version should only be used for tests, normally the defaultTenand handling should be used
     * @param validationBaseURL
     *            if <code>null</code>, no validation will be attempted
     * @param requestClientIP
     *            used for throttling user creation requests coming from the same IP address
     */
    User createSimpleUser(String username, String email, String password, String fullName, String company,
            Locale locale, String validationBaseURL, UserGroup userOwner, String requestClientIP, boolean enforceStrongPassword)
            throws UserManagementException, MailException, UserGroupManagementException;

    void updateSimpleUserPassword(String name, String newPassword) throws UserManagementException;

    void updateSimpleUserEmail(String username, String newEmail, String validationBaseURL) throws UserManagementException;
    
    void updateUserProperties(String username, String fullName, String company, Locale locale) throws UserManagementException;

    void deleteUser(String username) throws UserManagementException;

    /**
     * Creates a new role with initially empty {@link RoleDefinition#getPermissions() permissions}.
     */
    RoleDefinition createRoleDefinition(UUID id, String name);

    /**
     * Deletes the {@code roleDefinition} from this service persistently.
     */
    void deleteRoleDefinition(RoleDefinition roleDefinition);
    
    /**
     * The {@code roleDefinitionWithNewProperties} object represents an updated version, maybe a duplicate, of what we would get
     * when asking {@link #getRoleDefinition(UUID) this.getRole(roleWithNewProperties.getId())}. It may have changed compared to
     * what this service has in store. This service's representation (if not the same) and in particular the persistent
     * representation that this service will load upon its next start-up will be updated to match
     * {@code roleDefinitionWithNewProperties}'s state.
     */
    void updateRoleDefinition(RoleDefinition roleDefinitionWithNewProperties);
    
    Iterable<RoleDefinition> getRoleDefinitions();

    RoleDefinition getRoleDefinition(UUID idOfRoleDefinition);
    
    void addRoleForUser(User user, Role role);

    void addRoleForUser(String username, Role role);

    void removeRoleFromUser(User user, Role role);
    
    void removeRoleFromUser(String username, Role role);

    void removePermissionFromUser(String username, WildcardPermission permissionToRemove);

    void addPermissionForUser(String username, WildcardPermission permissionToAdd);

    /**
     * Registers a settings key together with its type. Calling this method is necessary for {@link #setSetting(String, Object)}
     * to have an effect for <code>key</code>. Calls to {@link #setSetting(String, Object)} will only accept values whose type
     * is compatible with <code>type</code>. Note that the store implementation may impose constraints on the types supported.
     * All store implementations are required to support at least {@link String} and {@link UUID} as types.
     */
    void addSetting(String key, Class<?> clazz) throws UserManagementException;

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

    Map<String, Object> getAllSettings();

    Map<String, Class<?>> getAllSettingTypes();

    void refreshSecurityConfig(ServletContext context);

    CacheManager getCacheManager();
    
    /**
     * Sends mail to the user identified by {@code username} if that user is found and has a non-{@code null}
     * {@link User#getEmail() e-mail address}. Note that this method does <em>not</em> check whether that
     * e-mail address is validated. This way this method can also be used to, e.g., send an e-mail in order
     * to validate an e-mail address.
     */
    void sendMail(String username, String subject, String body) throws MailException;

    /**
     * Checks whether <code>password</code> is the correct password for the user identified by <code>username</code>
     * 
     * @throws UserManagementException
     *             in a user by that name does not exist
     */
    boolean checkPassword(String username, String password) throws UserManagementException;

    boolean checkPasswordResetSecret(String username, String passwordResetSecret) throws UserManagementException;

    /**
     * Generates a new random password for the user identified by <code>username</code> and sends it
     * to the user's e-mail address.
     */
    void resetPassword(String username, String baseURL) throws UserManagementException, MailException;

    boolean validateEmail(String username, String validationSecret) throws UserManagementException;

    /**
     * Permitted only for users with role {@link DefaultRoles#ADMIN} or when the subject's user name matches
     * <code>username</code>.
     * 
     * @param key must not be <code>null</code>
     * @param value must not be <code>null</code>
     */
    void setPreference(String username, String key, String value);

    /**
     * @see UserStore#setPreferenceObject(String, String, Object)}
     */
    void setPreferenceObject(String name, String preferenceKey, Object preference);

    /**
     * Permitted only for users with role {@link DefaultRoles#ADMIN} or when the subject's user name matches
     * <code>username</code>.
     */
    void unsetPreference(String username, String key);

    /**
     * @return <code>null</code> if no preference for the user identified by <code>username</code> is found
     */
    String getPreference(String username, String key);
    
    /**
     * Gets a preference object. Always returns null if there is no converter associated with the given key -> see
     * {@link UserStore#registerPreferenceConverter(String, PreferenceConverter)}.
     */
    <T> T getPreferenceObject(String username, String key);
    
    /**
     * Gets all preference objects resolving to a certain key. Always returns a valid map. May be empty.
     * {@link UserStore#registerPreferenceConverter(String, PreferenceConverter)}.
     */
    <T> Map<String, T> getPreferenceObjectsByKey(String key);

    /**
     * @return all preferences of the given user
     */
    Map<String, String> getAllPreferences(String username);
    
    /**
     * Issues a new access token and remembers it so that later the user identified by <code>username</code> can be
     * authenticated using the token. Any access token previously created for same user will be invalidated by this
     * call.
     * 
     * @return a new access token if <code>username</code> identifies a known user, <code>null</code> otherwise
     */
    String createAccessToken(String username);

    /**
     * May be invoked by users with role {@link DefaultRoles#ADMIN} or the user identified by {@code username}. Returns
     * the last access token previously created by {@link #createAccessToken(String)} or {@code null} if no such access
     * token was created before for user {@code username} or was {@link #removeAccessToken(String)}.
     */
    String getAccessToken(String username);
    
    /**
     * Like {@link #getAccessToken(String)} only that instead of returning {@code null}, a new access token will
     * be created and returned instead (see {@link #createAccessToken(String)}.
     */
    String getOrCreateAccessToken(String username);

    /**
     * Constructs a Bearer token for a given remote Server, either using a given username and password, or a given
     * bearer token. If neither of those are provided the current user will be used to create a bearer token. Provide
     * only username and password or bearer token, not the three of them. If none is provided but there is no user
     * currently authenticated, {@code null} will be returned.<p>
     */
    String getOrCreateTargetServerBearerToken(String targetServerUrlAsString, String targetServerUsername,
            String targetServerPassword, String targetServerBearerToken);

    /**
     * Looks up a user by an access token that was created before using {@link #createAccessToken(String)} for same user name.
     * 
     * @return <code>null</code> in case the access token is unknown or was deleted / invalidated
     */
    User getUserByAccessToken(String accessToken);

    void removeAccessToken(String username);

    /**
     * Returns the group owning this server/replicaset {@link UserStore#getServerGroup()}. This group is used as default
     * owner if default objects such as role definitions or the admin user have to be created outside of any user
     * session and a default ownership is required. It is the group owner of the {@link SecuredSecurityTypes#SERVER}
     * object.<p>
     * 
     * During replication, a replica's user store contents are replaced by the master's user store contents. However,
     * a replica's {@link UserStore#getServerGroup()} will be resolved by the server group name provided to the
     * {@link UserStore} at its construction time. If such a group is not provided by the master, it is created. This
     * creation will be executed as a replicable operation that hence will be sent back to the master where the group
     * is then known. This new group is then used to try to set the server's group ownership, after checking that such an
     * ownership doesn't exist yet.
     */
    UserGroup getServerGroup();

    /**
     * For the current session's {@link Subject} an ownership for an object of type {@code type} and with type-relative
     * object identifier {@code typeRelativeObjectIdentifier} is created with the subject's default creation group if no
     * ownership for that object is found yet. Otherwise, the existing ownership is left untouched.
     * <p>
     * 
     * The {@link ServerActions#CREATE_OBJECT} permission is checked for the executing server. Then, the
     * {@link DefaultActions#CREATE} permission is checked for the {@code type}/{@code typeRelativeObjectIdentifier}
     * object.
     * <p>
     * 
     * If any of these permission checks fails and the ownership has not been found but created, the ownership is
     * removed again, and the authorization exception is thrown by this method. Otherwise, the subject is considered to
     * have the permission to create the object, the {@code actionWithResult} is invoked, and the object returned by the
     * action is the result of this method.
     */
    <T> T setOwnershipCheckPermissionForObjectCreationAndRevertOnError(HasPermissions type,
            TypeRelativeObjectIdentifier typeRelativeObjectIdentifier,
            String securityDisplayName, Callable<T> createActionReturningCreatedObject);

    /**
     * Like
     * {@link #setOwnershipCheckPermissionForObjectCreationAndRevertOnError(HasPermissions, TypeRelativeObjectIdentifier, String, Callable)},
     * only that the action does not return an object, so this method does not either.
     */
    void setOwnershipCheckPermissionForObjectCreationAndRevertOnError(HasPermissions type,
            TypeRelativeObjectIdentifier typeRelativeObjectIdentifier, String securityDisplayName, Action actionToCreateObject);

    User getAllUser();

    void checkPermissionAndDeleteOwnershipForObjectRemoval(WithQualifiedObjectIdentifier object,
            Action actionToDeleteObject);

    <T> T checkPermissionAndDeleteOwnershipForObjectRemoval(WithQualifiedObjectIdentifier object,
            Callable<T> actionToDeleteObject);
    
    /**
     * Deletes {@link #deleteOwnership(QualifiedObjectIdentifier) ownership} and
     * {@link #deleteAccessControlList(QualifiedObjectIdentifier) ACLs} for the object identified by {@code identifier}
     */
    void deleteAllDataForRemovedObject(QualifiedObjectIdentifier identifier);

    <T extends WithQualifiedObjectIdentifier> void filterObjectsWithPermissionForCurrentUser(
            com.sap.sse.security.shared.HasPermissions.Action action, Iterable<T> objectsToFilter,
            Consumer<T> filteredObjectsConsumer);

    /**
     * Filters objects with any of the given permissions for the current user.
     */
    <T extends WithQualifiedObjectIdentifier> void filterObjectsWithAnyPermissionForCurrentUser(
            com.sap.sse.security.shared.HasPermissions.Action[] actions,
            Iterable<T> objectsToFilter, Consumer<T> filteredObjectsConsumer);

    <T extends WithQualifiedObjectIdentifier, R> List<R> mapAndFilterByReadPermissionForCurrentUser(
            Iterable<T> objectsToFilter, Function<T, R> filteredObjectsMapper);

    /**
     * Maps and filters by any of the given permissions for the current user.
     */
    <T extends WithQualifiedObjectIdentifier, R> List<R> mapAndFilterByAnyExplicitPermissionForCurrentUser(
            HasPermissions permittedObject, HasPermissions.Action[] actions, Iterable<T> objectsToFilter,
            Function<T, R> filteredObjectsMapper);

    /**
     * Checks if the current user has the {@link DefaultActions#READ READ} permission on the {@code object} identified.
     * If {@code object} is {@code null}, the check will always pass.
     * 
     * @return {@code true} if and only if the user has the permission or the {@code object} is {@code null}
     */
    boolean hasCurrentUserReadPermission(WithQualifiedObjectIdentifier object);

    /**
     * Checks if the current user has the {@link DefaultActions#UPDATE UPDATE} permission on the {@code object} identified.
     * If {@code object} is {@code null}, the check will always pass.
     * 
     * @return {@code true} if and only if the user has the permission or the {@code object} is {@code null}
     */
    boolean hasCurrentUserUpdatePermission(WithQualifiedObjectIdentifier object);

    /**
     * Checks if the current user has the {@link DefaultActions#DELETE DELETE} permission on the {@code object} identified.
     * If {@code object} is {@code null}, the check will always pass.
     * 
     * @return {@code true} if and only if the user has the permission or the {@code object} is {@code null}
     */
    boolean hasCurrentUserDeletePermission(WithQualifiedObjectIdentifier object);

    /**
     * @return true, if all of the given actions are permitted for the current user; if no action is provided
     *         ({@code actions} is an empty array), {@code true} is returned because the user always has permission
     *         "to do nothing."
     */
    boolean hasCurrentUserExplicitPermissions(WithQualifiedObjectIdentifier object, HasPermissions.Action... actions);

    /**
     * @return true, if any of the given actions is permitted for the current user; if no action is provided
     *         ({@code actions} is an empty array), {@code false} is returned because the user has none of the
     *         permissions from this empty set.
     */
    boolean hasCurrentUserOneOfExplicitPermissions(WithQualifiedObjectIdentifier object, HasPermissions.Action... actions);

    /**
     * Checks if the current user has the {@link DefaultActions#READ READ} permission on the {@code object} identified.
     * If {@code object} is {@code null}, the check will always pass.
     */
    void checkCurrentUserReadPermission(WithQualifiedObjectIdentifier object);

    /**
     * Checks if the current user has the {@link DefaultActions#UPDATE UPDATE} permission on the {@code object} identified.
     * If {@code object} is {@code null}, the check will always pass.
     * 
     * @throws AuthorizationException in case the current user is not permitted to update {@code object}
     */
    void checkCurrentUserUpdatePermission(WithQualifiedObjectIdentifier object);

    /**
     * Checks if the current user has the {@link DefaultActions#DELETE DELETE} permission on the {@code object} identified.
     * If {@code object} is {@code null}, the check will always pass.
     * 
     * @throws AuthorizationException in case the current user is not permitted to delete {@code object}
     */
    void checkCurrentUserDeletePermission(WithQualifiedObjectIdentifier object);

    /**
     * Checks if the current user has the {@link DefaultActions#DELETE DELETE} permission on the {@code object} identified.
     * If {@code object} is {@code null}, the check will always pass.
     * 
     * @throws AuthorizationException in case the current user is not permitted to delete {@code object}
     */
    void checkCurrentUserDeletePermission(QualifiedObjectIdentifier object);

    /**
     * Checks if the current user has the permission to perform all {@code actions} requested on the {@code object}
     * identified. Throws an {@link AuthorizationException} if not. If {@code object} is {@code null}, everything is
     * allowed. If the list of {@code actions} is empty, the method will return without exception (rationale: doing
     * nothing is always allowed).
     */
    void checkCurrentUserExplicitPermissions(WithQualifiedObjectIdentifier object, HasPermissions.Action... actions);

    /**
     * Checks if the current user has permission any of the given actions. If the {@code actions} list is empty,
     * the current user formally has no permission for any action provided, so consequently an {@link AuthorizationException}
     * results. Checks for a {@code null} value for {@code object} always pass without exception.
     */
    void checkCurrentUserHasOneOfExplicitPermissions(WithQualifiedObjectIdentifier object, HasPermissions.Action... actions);

    /**
     * Since there are some HasPermission objects, that have no Ownership, this method is used to explicitly mention
     * that they are to be assumed as migrated.
     */
    void assumeOwnershipMigrated(String typeName);

    /**
     * If {@code object} doesn't have an ownership then it will be assigned the {@link #getServerGroup() server group}
     * as its group owner.
     * 
     * @return {@code true} if the object required ownership migration
     */
    boolean migrateOwnership(WithQualifiedObjectIdentifier object);

    /**
     * If {@code object} doesn't have an ownership then it will be assigned the {@link #getServerGroup() server group}
     * as its group owner.
     * 
     * @return {@code true} if the object required ownership migration
     */
    boolean migrateOwnership(QualifiedObjectIdentifier object, String displayName);

    void migrateUser(User user);

    void migratePermission(User user, WildcardPermission permissionToMigrate,
            Function<WildcardPermission, WildcardPermission> permissionReplacement);

    /**
     * If the {@link SecuredSecurityTypes#SERVER} object has a group ownership. If not, it is set to the
     * {@link #getServerGroup() server group}. The {@link SecuredSecurityTypes#SERVER} type is then marked
     * as migrated (see {@link #checkMigration(Iterable)}).
     */
    void migrateServerObject();
    
    void checkMigration(Iterable<? extends HasPermissions> allInstances);

    boolean hasCurrentUserMetaPermission(WildcardPermission permissionToCheck, Ownership ownership);
    
    boolean hasCurrentUserMetaPermissionWithOwnershipLookup(WildcardPermission permissionToCheck);

    void setOwnershipIfNotSet(QualifiedObjectIdentifier identifier, User userOwner, UserGroup defaultTenant);

    UserGroup getDefaultTenantForCurrentUser();

    public void setTemporaryDefaultTenant(final UUID tenantGroupId);

    /**
     * When a user adds permissions to a role, he needs to hold the permissions for all existing qualifications. This
     * method checks all given permissions for all existing qualifications of the given role.
     * 
     * @return {@code true} if the current user holds all given meta permissions for all existing qualifications of the
     *         given role.
     */
    boolean hasUserAllWildcardPermissionsForAlreadyRealizedQualifications(RoleDefinition role,
            Iterable<WildcardPermission> permissionsToCheck);

    void setDefaultTenantForCurrentServerForUser(String username, UUID defaultTenantId);
    
    void copyUsersAndRoleAssociations(UserGroup source, UserGroup destination, RoleCopyListener callback);

    User checkPermissionForUserCreationAndRevertOnErrorForUserCreation(String username,
            Callable<User> createActionReturningCreatedObject) throws UserManagementException;

    /**
     * Do only use this, if it is not possible to get the actual instance of the object to delete using the
     * WithQualifiedObjectIdentifier variant
     */
    <T> T checkPermissionAndDeleteOwnershipForObjectRemoval(QualifiedObjectIdentifier identifier,
            Callable<T> actionToDeleteObject);
    
    /**
     * Do only use this, if it is not possible to get the actual instance of the object to delete using the
     * WithQualifiedObjectIdentifier variant
     */
    void checkPermissionAndDeleteOwnershipForObjectRemoval(QualifiedObjectIdentifier identifier,
            Action actionToDeleteObject);
    
    <T> T doWithTemporaryDefaultTenant(UserGroup tenant, Callable<T> action);

    /**
     * Before using a SecuritySystem, it is necessary to initialize the service, to ensure roles and acls are correctly
     * setup. CALL THIS AFTER: the role prototypes exist, the default roles exist, users are loaded (in case of stored
     * default ones)
     */
    void initialize();

    boolean hasCurrentUserMetaPermissionsOfRoleDefinitionWithQualification(RoleDefinition roleDefinition,
            Ownership qualificationForGrantedPermissions);

    boolean hasCurrentUserMetaPermissionsOfRoleDefinitionsWithQualification(Set<RoleDefinition> roleDefinitions,
            Ownership qualificationForGrantedPermissions);

    /**
     * @return {@code true} if the {@link UserStore} is initial or permission vertical migration is necessary.
     */
    boolean isInitialOrMigration();
    
    /**
     * @return {@code true} if the server is a newly set up instance. While {@link #isInitialOrMigration()} defines if
     *         the {@link SecurityService} is initially set up, this method distincts a server connected to a central
     *         {@link SecurityService}. In case, the {@link SecurityService} is initial (defined by
     *         {@link #isInitialOrMigration()}) it is also a new server.
     */
    boolean isNewServer();

    /**
     * Tries to find a {@link RoleDefinition} whose ID equals that of the {@link RolePrototype} passed. If found, the
     * permission set of the {@link RoleDefition} that was found is compared to that of the {@link RolePrototype}, and
     * in case of differences the permission set of the {@link RolePrototype} is copied into the {@link RoleDefinition}.
     * If no {@link RoleDefinition} by the ID specified by the {@link RolePrototype} is found, a new
     * {@link RoleDefinition} is created. If {@code makeReadableForAll} is {@code true} and this server is just
     * {@link #isInitialOrMigration() being initialized or migrating} then the new role definition will be made readable
     * for all users by adding an ACL for the {@code null} group that grants the {@link DefaultActions#READ} permission.
     */
    RoleDefinition getOrCreateRoleDefinitionFromPrototype(RolePrototype rolePrototype, boolean makeReadableForAll);

    /** Sets the default ownership based on the current user. */
    void setDefaultOwnership(QualifiedObjectIdentifier identifier, String description);

    void setDefaultOwnershipIfNotSet(QualifiedObjectIdentifier identifier);

    /**
     * Checks if a user has at least one permission implied by the given {@link WildcardPermission}.
     * 
     * @see PermissionChecker#hasUserAnyPermission(WildcardPermission, Iterable,
     *      com.sap.sse.security.shared.SecurityUser, com.sap.sse.security.shared.SecurityUser,
     *      com.sap.sse.security.shared.AbstractOwnership)
     */
    boolean hasCurrentUserAnyPermission(WildcardPermission permissionToCheck);

    /**
     * Like {@link #setOwnershipCheckPermissionForObjectCreationAndRevertOnError(HasPermissions, TypeRelativeObjectIdentifier, String, Callable)},
     * only that no check is performed for {@link ServerActions#CREATE_OBJECT} in addition to the {@code type}-specific
     * {@link DefaultActions#CREATE} check.
     */
    <T> T setOwnershipWithoutCheckPermissionForObjectCreationAndRevertOnError(HasPermissions type,
            TypeRelativeObjectIdentifier typeIdentifier, String securityDisplayName, Callable<T> actionWithResult);

    /**
     * Like {@link #setOwnershipCheckPermissionForObjectCreationAndRevertOnError(HasPermissions, TypeRelativeObjectIdentifier, String, Action)},
     * only that no check is performed for {@link ServerActions#CREATE_OBJECT} in addition to the {@code type}-specific
     * {@link DefaultActions#CREATE} check.
     */
    void setOwnershipWithoutCheckPermissionForObjectCreationAndRevertOnError(HasPermissions type,
            TypeRelativeObjectIdentifier typeRelativeObjectIdentifier, String securityDisplayName,
            Action actionToCreateObject);

    /**
     * Returns if the current user is granted the permission defined by the {@link SecuredSecurityTypes#SERVER server
     * type} and the given {@link ServerActions action}.
     */
    boolean hasCurrentUserServerPermission(ServerActions action);

    /**
     * Checks if the current user is granted the permission defined by the {@link SecuredSecurityTypes#SERVER server
     * type} and the given {@link ServerActions action}.
     */
    void checkCurrentUserServerPermission(ServerActions action);

    /**
     * In case this security service is shared across multiple replica sets that are published under different
     * sub-domains, session cookies and the local store shall be arranged such that sessions and content are identified
     * regardless the sub-domain used. For example, if there are two events, A, and B, published through
     * {@code a.sapsailing.com} and {@code b.sapsailing.com}, respectively, by default the full sub-domain name will be
     * used for the {@code JSESSIONID} cookie as well as for the identification of content in the browser's local store.
     */
    String getSharedAcrossSubdomainsOf();

    /**
     * In the browser client, a {@code CrossDomainStorage} implementation is used to either run requests for a local
     * browser storage against the application's local storage under the URL/origin used to access the application, or a
     * shared local storage across several sub-domains may be configured so that data stored while in the context of one
     * sub-domain is also available to the application when loaded from a different sub-domain.
     * 
     * @return the base URL where the {@code /gwt-base/StorageMessaging.html} entry point can be found; if {@code null},
     *         clients should configure their {@code CrossDomainStorage} such that the application's origin is used for
     *         isolated local storage; otherwise, the result should be used to get a {@code CrossDomainStorage}
     *         implementation that accesses a local store shared across all application instances that use the same base
     *         URL for cross-domain storage.
     */
    String getBaseUrlForCrossDomainStorage();

    void registerCustomizer(SecurityInitializationCustomizer customizer);

    /**
     * Persist user subscription data
     */
    void updateUserSubscription(String username, Subscription subscription) throws UserManagementException;
    
    /**
     * Adds a listener that will be invoked each time the set of users having {@code permission} may have changed. When
     * the {@code listener}'s
     * {@link PermissionChangeListener#setOfUsersWithPermissionChanged(WildcardPermission, Iterable)
     * setOfUsersWithPermissionChanged} is called, the listener can invoke
     * {@link #getUsersWithPermissions(WildcardPermission)} to check whether there actually was a change relevant to the
     * listener. There is a possibility of "false positive" listener invocations, but each time the set of users having
     * {@code permission} changes, the listener <em>will</em> be notified.
     * 
     * @param permission
     *            must have a valid, non-empty {@link WildcardPermission#getQualifiedObjectIdentifiers() set of object
     *            identifiers}, therefore identifying one or more permissions on a non-empty list of specific objects.
     *            No {@link WildcardPermission#WILDCARD_TOKEN wildcards} are allowed in any part of the permission.
     */
    void addPermissionChangeListener(WildcardPermission permission, PermissionChangeListener listener);
    
    /**
     * Removes the {@code listener} from this service if it was registered for a permission equal to {@code permission}
     * before with the {@link #addPermissionChangeListener(WildcardPermission, PermissionChangeListener)} method. Otherwise,
     * invoking the method has no effect.
     */
    void removePermissionChangeListener(WildcardPermission permission, PermissionChangeListener listener);

    /**
     * Obtains all {@link HasPermissions} secured types managed by this security service. In an OSGi environment, those
     * are obtained from all {@link HasPermissionsProvider}s registered as an OSGi service by any active bundle.
     */
    Iterable<? extends HasPermissions> getAllHasPermissions();
    
    /**
     * Obtains all {@link SubscriptionPlan} subscription plans managed by this security service. In an OSGi environment, those
     * are obtained from all {@link SubscriptionPlanProvider}s registered as an OSGi service by any active bundle.
     */
    Map<Serializable, SubscriptionPlan> getAllSubscriptionPlans();
    
    SubscriptionPlan getSubscriptionPlanById(String planId);
    
    /**
     * Tries to find a {@link HasPermissions secured type} in the {@link #getAllHasPermissions() set of secured types
     * known by this security service} based on its name, like it is used in the first
     * {@link WildcardPermission#getParts() part} of a permission specification, as in {@code USER:READ:*}.
     * 
     * @param securedTypeName
     *            must not be {@code null}
     * @return may be {@code null} in case a secured type by that {@link HasPermissions#getName() name} is not found
     */
    default HasPermissions getHasPermissionsByName(String securedTypeName) {
        return Util.first(Util.filter(getAllHasPermissions(), hp->hp.getName().equals(securedTypeName)));
    }
    
    ClassLoaderRegistry getInitialLoadClassLoaderRegistry();

    /*
     * Will resolve potential roles, which a user would inherit, when given a specific subscription plan.
     */
    Role[] getSubscriptionPlanUserRoles(User user, SubscriptionPlan plan);

    SubscriptionPlan getSubscriptionPlanByItemPriceId(String itemPriceId);

    /**
     * Updates the currently held SubscriptionPlanPrices for all known SubscriptionPlans
     * 
     * @param itemPrices
     */
    void updateSubscriptionPlanPrices(Map<String, BigDecimal> itemPrices);

    Role createRoleFromIDs(UUID roleDefinitionId, UUID qualifyingTenantId, String qualifyingUsername, boolean transitive) throws UserManagementException;

    /**
     * @return the role associated with the given IDs and qualifiers
     * @throws UserManagementException
     *             if the current user does not have the meta permission to give this specific, qualified role in this
     *             context.
     */
    Role getOrThrowRoleFromIDsAndCheckMetaPermissions(UUID roleDefinitionId, UUID qualifyingGroupId, String userQualifierName,
            boolean transitive) throws UserManagementException;

    /**
     * When updating a user's {@link User#getSubscriptions() subscriptions}, use this method to obtain a write lock. For
     * complex operations, such as first reading the current subscriptions, then manipulating them and writing them back
     * to the user object, ensure the lock is held throughout the complex operation sequence.
     * <p>
     * 
     * Unlock again using {@ink #unlockSubscriptionsForUser(User)}, always in a {@code finally} clause, to avoid hanging
     * locks.
     * <p>
     * 
     * The lock will also be obtained upon processing a replication operation (see, e.g.,
     * {@link ReplicableSecurityService#internalUpdateSubscription(String, Subscription)}) internally. This way,
     * compound operations are also protected against concurrent manipulation through replicated transactions.
     */
    void lockSubscriptionsForUser(User user);

    /**
     * Releases the lock obtained with {@link #lockSubscriptionsForUser(User)}. Always call this in a {@code finally} clause
     * that follows the {@link #lockSubscriptionsForUser(User)} call, so a lock can never hang.
     */
    void unlockSubscriptionsForUser(User user);

    /**
     * For the application replica set identified by {@code serverName} sets the CORS filter to allow REST requests from
     * all origins (*). The change is made persistent through the {@link PersistenceFactory} and will hence be restored
     * when this process instance is a primary/master node upon initialization.
     */
    void setCORSFilterConfigurationToWildcard(String serverName);

    /**
     * For the application replica set identified by {@code serverName} sets the CORS filter to allow REST requests from
     * the origins named in {@code allowedOrigins}. Passing {@code null} for {@code allowedOrigins} is equivalent to
     * passing an empty array. The change is made persistent through the {@link PersistenceFactory} and will hence be
     * restored when this process instance is a primary/master node upon initialization.
     * 
     * @param allowedOrigins
     *            the origins, including their scheme (e.g., {@code https://www.example.com}) from which REST requests
     *            coming from that browser origin are to be permitted; {@code null} is handled like an empty array
     * @throws IllegalArgumentException
     *             in case any of the {@code allowedOrigins} does not fulfill the criteria of
     *             {@link HttpHeaderUtil#isValidOriginHeaderValue(String)}.
     */
    void setCORSFilterConfigurationAllowedOrigins(String serverName, String... allowedOrigins) throws IllegalArgumentException;

    Pair<Boolean, Set<String>> getCORSFilterConfiguration(String serverName);

    LockingAndBanning failedPasswordAuthentication(User user);

    void successfulPasswordAuthentication(User user);

    /**
     * Records failure to authenticate with a bearer token, with the request coming from {@code clientIP} and the user
     * agent as provided by argument {@code userAgent}. The locking will start with a minimum locking/banning duration
     * (typically one second). After the locking duration expires, the record of the {@code clientIP/userAgent}
     * combination is kept around for another locking duration, so that if another call to this method with an equal
     * combination of {@code clientIP} and {@code userAgent} is made, that combination will remain
     * {@link #isClientIPLockedForBearerTokenAuthentication(String) locked}, and the locking duration is increased.
     * <p>
     * 
     * If two locking durations have expired without this method being invoked for equal {@cod eclientIP} and
     * {@code userAgent}, the locking record including its last locking duration is expunged from the internal data
     * structures, avoiding garbage piling up.
     */
    LockingAndBanning failedBearerTokenAuthentication(String clientIP);

    /**
     * Call this when the combination of {@code clientIP} and {@code userAgent} was not
     * {@link #isClientIPLockedForBearerTokenAuthentication(String) locked} and a successful bearer token-based
     * authentication attempt was made. This will remove the locking record for the combination,
     * and in case of an unsuccessful future attempt the locking duration will start at
     * the low default.
     */
    void successfulBearerTokenAuthentication(String clientIP);

    /**
     * Used in conjunction with {@link #failedBearerTokenAuthentication(String)} and
     * {@link #successfulBearerTokenAuthentication(String)}. If and only if a locking state for the combination
     * of {@code clientIP} and {@code userAgent} is known and still locked, {@code true} is returned. Unlocking
     * will bappen by calling {@link #successfulBearerTokenAuthentication(String)} with an equal combination
     * of {@code clientIP} and {@code userAgent}. Invoking {@link #failedBearerTokenAuthentication(String)}
     * will establish (if not yet locked) or extend the locking duration for the combination.
     */
    boolean isClientIPLockedForBearerTokenAuthentication(String clientIP);

    void fileTakedownNotice(TakedownNoticeRequestContext takedownNoticeRequestContext) throws MailException;
    
    /**
     * For a {@link SecuredSecurityTypes#SERVER SERVER} object identified by {@code serverName}, determines the user set
     * as the server's owner, plus additional users that have the permission to execute
     * {@code alsoSendToAllUsersWithThisPermissionOnReplicaSet} on that server.
     * 
     * @param serverName
     *            identifies the server object; for the local server that would, e.g., be {@link ServerInfo#getName()}.
     *            For replica sets, this is the name of the replica set.
     * @param alsoSendToAllUsersWithThisPermissionOnReplicaSet
     *            when not empty, all users that have permission to this {@link SecuredSecurityTypes#SERVER SERVER}
     *            action on the {@code replicaSet} will receive the e-mail in addition to the server owner. No user will
     *            receive the e-mail twice.
     */
    Iterable<User> getUsersToInformAboutReplicaSet(String serverName,
            Optional<com.sap.sse.security.shared.HasPermissions.Action> alsoSendToAllUsersWithThisPermissionOnReplicaSet);
}
