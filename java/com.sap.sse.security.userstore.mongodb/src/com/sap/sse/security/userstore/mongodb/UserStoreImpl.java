package com.sap.sse.security.userstore.mongodb;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.security.interfaces.PreferenceConverter;
import com.sap.sse.security.interfaces.PreferenceObjectListener;
import com.sap.sse.security.interfaces.SocialSettingsKeys;
import com.sap.sse.security.interfaces.UserImpl;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.Account;
import com.sap.sse.security.shared.AdminRole;
import com.sap.sse.security.shared.PredefinedRoles;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.RoleDefinitionImpl;
import com.sap.sse.security.shared.RolePrototype;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.UserRole;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.LockingAndBanning;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.impl.UserGroupImpl;
import com.sap.sse.security.userstore.mongodb.impl.FieldNames.Tenant;

/**
 * An implementation of the {@link UserStore} interface, intended to store its state durably in a MongoDB instance. A
 * de-serialized copy, however, will have its {@link #mongoObjectFactory} field set to <code>null</code> and will
 * therefore not perform any changes to the database. This is also the reason why all access to the
 * {@link #mongoObjectFactory} field needs to be <code>null</code>-safe.
 * <p>
 * 
 * The storage pattern for {@link UserGroupImpl} and {@link Tenant} objects deserves some explanation. As a
 * {@link Tenant} is a specialized {@link UserGroupImpl}, this store mainly needs to keep track of the users in that
 * {@link Tenant}. Hence, the same collection is used for the storage of these user lists, and hence the same methods
 * can be used for maintaining this collection. Additionally, the tenant ID is stored in a separate collection as a
 * "marker" which entries in the user groups collection are actually tenants and not only user groups.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class UserStoreImpl implements UserStore {
    
    private static final long serialVersionUID = -3860868283827473187L;

    private static final Logger logger = Logger.getLogger(UserStoreImpl.class.getName());

    private static final String ACCESS_TOKEN_KEY = "___access_token___";

    private final static String NAME = "MongoDB user store";
    
    private final ConcurrentHashMap<UUID, RoleDefinition> roleDefinitions;

    /**
     * If a valid default tenant name was passed to the constructor, this field will contain a valid
     * {@link UserGroupImpl} object whose name equals that of the default tenant name. It will have been used during
     * role migration where string-based roles are mapped to a corresponding {@link RoleDefinition} and the users with
     * the original role will obtain a corresponding {@link Role} with this default tenant as the
     * {@link Role#getQualifiedForTenant() tenant qualifier}.
     */
    private UserGroup serverGroup;

    private final Map<UUID, UserGroup> userGroups;
    private final Map<String, UserGroup> userGroupsByName;
    private final Map<User, Set<UserGroup>> userGroupsContainingUser;
    
    /**
     * This collection is important in particular to detect changes when {@link #updateUserGroup(UserGroupImpl)} is
     * called.
     */
    private final Map<UserGroup, Set<User>> usersInUserGroups;
    private final Map<RoleDefinition, Set<UserGroup>> roleDefinitionsToUserGroups;
    
    /**
     * Protects access to the maps {@link #userGroupsContainingUser}, {@link #usersInUserGroups}, {@link #userGroups},
     * {@link #userGroupsByName} and {@link #roleDefinitionsToUserGroups}.<br>
     * Lock may be acquired when having a lock for {@linkplain usersLock}. If both locks (no matter if read or write) are
     * required, {@link #usersLock} must always be acquired before acquiring {@link #userGroupsLock}.
     */
    private transient NamedReentrantReadWriteLock userGroupsLock;
    
    /**
     * Protects access to the maps {@link #users}, {@link #usersByEmail}, {@link #roleDefinitionsToUsers},
     * {@link #usersByAccessToken} and {@link #emailForUsername}.<br>
     * Must not be locked when already having a lock for {@linkplain userGroupsLock}. If both locks (no matter if read
     * or write) are required, {@link #usersLock} must always be acquired before acquiring {@link #userGroupsLock}.
     */
    private transient NamedReentrantReadWriteLock usersLock;
    private final Map<String, User> users;
    private final Map<String, Set<User>> usersByEmail;
    private final Map<String, String> emailForUsername;
    private final Map<String, User> usersByAccessToken;
    private final Map<RoleDefinition, Set<User>> roleDefinitionsToUsers;

    private final ConcurrentHashMap<String, Object> settings;
    private final ConcurrentHashMap<String, Class<?>> settingTypes;

    /**
     * Protects access to the maps {@link #preferences}, {@link #preferenceConverters}, {@link #preferenceObjects} and
     * {@link #preferenceListeners}. If access to {@link #userGroupsLock} or {@link #usersLock} is required, these locks
     * must be obtained first in the correct order. This lock always has to be locked last.
     */
    private transient NamedReentrantReadWriteLock preferenceLock;
    
    /**
     * Keys are the usernames, values are the key/value pairs representing the user's preferences
     */
    private final Map<String, Map<String, String>> preferences;

    /**
     * Converter objects to map preference Strings to Objects. The keys must match the keys of the preferences.
     */
    private transient Map<String, PreferenceConverter<?>> preferenceConverters;

    /**
     * This is another view of the String preferences mapped by {@link #preferenceConverters} to Objects. Keys are the
     * usernames, values are the key/value pairs representing the user's preferences.
     */
    private transient Map<String, Map<String, Object>> preferenceObjects;
    
    /**
     * Keys are preferences keys as used by {@link #preferenceObjects}, values are the listeners to inform on changes of
     * the specific preference object for a {@link UserImpl}.
     */
    private transient Map<String, Set<PreferenceObjectListener<?>>> preferenceListeners;
    
    /**
     * Won't be serialized and remains <code>null</code> on the de-serializing end.
     */
    private final transient MongoObjectFactory mongoObjectFactory;
    
    /**
     * Won't be serialized and remains <code>null</code> on the de-serializing end.
     */
    private final transient DomainObjectFactory domainObjectFactory;

    private final String serverGroupName;

    public UserStoreImpl(String defaultServerGroupName) throws UserGroupManagementException, UserManagementException {
        this(PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory(),
                PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(), defaultServerGroupName);
    }

    public UserStoreImpl(final DomainObjectFactory domainObjectFactory, final MongoObjectFactory mongoObjectFactory,
            String defaultServerGroupName) throws UserGroupManagementException, UserManagementException {
        this.serverGroupName = defaultServerGroupName;
        this.domainObjectFactory = domainObjectFactory;
        roleDefinitions = new ConcurrentHashMap<>();
        userGroups = new HashMap<>();
        userGroupsByName = new HashMap<>();
        userGroupsContainingUser = new HashMap<>();
        usersInUserGroups = new HashMap<>();
        roleDefinitionsToUsers = new HashMap<>();
        roleDefinitionsToUserGroups = new HashMap<>();
        users = new HashMap<>();
        usersByEmail = new HashMap<>();
        emailForUsername = new HashMap<>();
        settings = new ConcurrentHashMap<>();
        settingTypes = new ConcurrentHashMap<>();
        usersByAccessToken = new HashMap<>();
        preferences = new HashMap<>();
        preferenceConverters = new HashMap<>();
        preferenceObjects = new HashMap<>();
        preferenceListeners = new HashMap<>();
        usersLock = new NamedReentrantReadWriteLock("Users", /* fair */ false);
        userGroupsLock = new NamedReentrantReadWriteLock("User Groups", /* fair */ false);
        preferenceLock = new NamedReentrantReadWriteLock("Preferences", /* fair */ false);
        this.mongoObjectFactory = mongoObjectFactory;
        if (domainObjectFactory != null) {
            for (Entry<String, Class<?>> e : domainObjectFactory.loadSettingTypes().entrySet()) {
                settingTypes.put(e.getKey(), e.getValue());
            }
            for (Entry<String, Object> e : domainObjectFactory.loadSettings().entrySet()) {
                settings.put(e.getKey(), e.getValue());
            }
            for (Entry<String, Map<String, String>> e : domainObjectFactory.loadPreferences().entrySet()) {
                preferences.put(e.getKey(), e.getValue());
            }
            boolean changed = false;
            changed = changed || initSocialSettingsIfEmpty();
            if (changed) {
                mongoObjectFactory.storeSettingTypes(settingTypes);
                mongoObjectFactory.storeSettings(settings);
            }
            for (RoleDefinition roleDefinition : domainObjectFactory.loadAllRoleDefinitions()) {
                roleDefinitions.put(roleDefinition.getId(), roleDefinition);
            }
            if (roleDefinitions.isEmpty()) {
                logger.info("Empty set of role definitions suggests we are under migration. Creating default roles.");
            }
        }
    }

    @Override
    public UserGroup ensureServerGroupExists() throws UserGroupManagementException {
        return LockUtil.executeWithWriteLockAndResultExpectException(userGroupsLock, () -> {
            serverGroup = getOrCreateServerGroup(serverGroupName);
            return serverGroup;
        });
    }

    /**
     * Do not call this before the security service is ready, as else role definition migration will not work correctly
     */
    public void loadAndMigrateUsers() throws UserStoreManagementException {
        LockUtil.executeWithWriteLockExpectException(usersLock, () -> {
            LockUtil.executeWithWriteLockExpectException(userGroupsLock, () -> {
                final Iterable<UserGroup> userGroups = domainObjectFactory
                        .loadAllUserGroupsAndTenantsWithProxyUsers(roleDefinitions);
                for (UserGroup group : userGroups) {
                    this.userGroups.put(group.getId(), group);
                    userGroupsByName.put(group.getName(), group);
                }
                // do this here, in case the default tenant was just loaded before
                ensureServerGroupExists();
                for (User u : domainObjectFactory.loadAllUsers(roleDefinitions, this::convertToNewRoleModel,
                        this.userGroups, this)) {
                    users.put(u.getName(), u);
                    addToUsersByEmail(u);
                    for (Role roleOfUser : u.getRoles()) {
                        Util.addToValueSet(roleDefinitionsToUsers, roleOfUser.getRoleDefinition(), u);
                    }
                }
                // the users in the groups/tenants are still only proxies; now that the real users have been loaded,
                // replace them based on the username key:
                for (final UserGroup group : this.userGroups.values()) {
                    migrateProxyUsersInGroupToRealUsersByUsername(group);
                    for (final User userInGroup : group.getUsers()) {
                        Util.addToValueSet(usersInUserGroups, group, userInGroup);
                        Util.addToValueSet(userGroupsContainingUser, userInGroup, group);
                    }
                    for (RoleDefinition roleInUserGroup : group.getRoleDefinitionMap().keySet()) {
                        Util.addToValueSet(roleDefinitionsToUserGroups, roleInUserGroup, group);
                    }
                }
            });
            LockUtil.executeWithReadLock(preferenceLock, () -> {
                // FIXME check for non migrated users, those are leftovers that are in some groups but have no user object
                // anymore, remove them from the groups!
                for (Entry<String, Map<String, String>> e : preferences.entrySet()) {
                    if (e.getValue() != null) {
                        final String accessToken = e.getValue().get(ACCESS_TOKEN_KEY);
                        if (accessToken != null) {
                            final User user = users.get(e.getKey());
                            if (user != null) {
                                usersByAccessToken.put(accessToken, user);
                            } else {
                                logger.warning("Couldn't find user \"" + e.getKey()
                                + "\" for which an access token was found in the preferences");
                            }
                        }
                    }
                }
            });
        });
    }
    
    private Role convertToNewRoleModel(String oldRoleName, String username) {
        Role result = null;
        for (final RoleDefinition roleDefinition : roleDefinitions.values()) {
            // migrate old admins to new admin!
            if (roleDefinition.getName().equals(oldRoleName)) {
                final UserGroup groupQualifierForMigratedRole;
                if (AdminRole.getInstance().getId().equals(roleDefinition.getId())
                        && UserStore.ADMIN_USERNAME.equals(username)) {
                    // Special of the global admin's admin role to ensure that one initial user has global admin permissions
                    groupQualifierForMigratedRole = null;
                } else {
                    if (serverGroup == null) {
                        throw new IllegalStateException(
                                "For role migration a valid server group name is required. Set system property "
                                        + UserStore.DEFAULT_SERVER_GROUP_NAME_PROPERTY_NAME + " or provide a server name");
                    }
                    groupQualifierForMigratedRole = serverGroup;
                }
                result = new Role(roleDefinition, groupQualifierForMigratedRole, /* user qualification */ null, /* transitive */ true);
                break;
            }
        }
        return result;
    }

    @Override
    public void ensureDefaultRolesExist() {
        getOrCreateRoleDefinitionByPrototype(AdminRole.getInstance());
        getOrCreateRoleDefinitionByPrototype(UserRole.getInstance());
        for (final PredefinedRoles otherPredefinedRole : PredefinedRoles.values()) {
            final UUID id = otherPredefinedRole.getId();
            final RoleDefinition potentiallyExistingRoleDefinition = getRoleDefinition(id);
            final Set<WildcardPermission> targetPermissions = new HashSet<>();
            for (final String stringPermission : otherPredefinedRole.getPermissions()) {
                targetPermissions.add(new WildcardPermission(stringPermission));
            }
            if (potentiallyExistingRoleDefinition == null) {
                logger.info("Predefined role definition " + otherPredefinedRole + " not found; creating");
                createRoleDefinition(id, otherPredefinedRole.name(), targetPermissions);
            } else if (!targetPermissions.equals(potentiallyExistingRoleDefinition.getPermissions())) {
                setRoleDefinitionPermissions(id, targetPermissions);
            }
        }
    }
    
    private RoleDefinition getOrCreateRoleDefinitionByPrototype(RolePrototype rolePrototype) {
        final UUID id = rolePrototype.getId();
        RoleDefinition roleDefinition = getRoleDefinition(id);
        if (roleDefinition == null) {
            logger.info("No " + rolePrototype.getName() + " role found. Creating default role \""
                    + rolePrototype.getName() + "\" with permission \"" + rolePrototype.getPermissions() + "\"");
            roleDefinition = createRoleDefinition(id, rolePrototype.getName(),
                    rolePrototype.getPermissions());
        } else if(roleDefinition.getPermissions() != null
                && !roleDefinition.getPermissions().equals(rolePrototype.getPermissions())){
            setRoleDefinitionPermissions(id, rolePrototype.getPermissions());
            roleDefinition = getRoleDefinition(id);
        }
        return roleDefinition;
    }
    
    @Override
    public RoleDefinition getRoleDefinitionByPrototype(RolePrototype rolePrototype) {
        final RoleDefinition roleDefinition = getRoleDefinition(rolePrototype.getId());
        if (roleDefinition == null) {
            final String errorMsg = "No " + rolePrototype.getName() + " role definition found by ID "
                    + rolePrototype.getId() + "." + "RoleDefinitions for prototypes are required to exist on usage.";
            logger.severe(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        return roleDefinition;
    }

    @Override
    public String getServerGroupName() {
        return serverGroupName;
    }

    @Override
    public UserGroup getServerGroup() {
        return serverGroup;
    }

    @Override
    public void setServerGroup(UserGroup newServerGroup) {
        this.serverGroup = newServerGroup;
    }

    /**
     * To call this method, the caller must have obtained the write lock of {@link #userGroupsLock}.
     */
    private UserGroup getOrCreateServerGroup(String defaultServerGroupName) throws UserGroupManagementException {
        assert userGroupsLock.isWriteLockedByCurrentThread();
        final UserGroup result;
        if (defaultServerGroupName != null) {
            final UserGroup existingTenant = getUserGroupByName(defaultServerGroupName);
            if (existingTenant == null) {
                logger.info("Couldn't find default tenant " + defaultServerGroupName + "; creating it");
                result = createUserGroup(UUID.randomUUID(), defaultServerGroupName);
            } else {
                result = existingTenant;
            }
        } else {
            result = null;
        }
        return result;
    }

    /**
     * To call this method, the caller must have obtained the write lock of {@link #usersLock} and {@link #userGroupsLock}.
     */
    private void migrateProxyUsersInGroupToRealUsersByUsername(final UserGroup group) {
        assert usersLock.isWriteLockedByCurrentThread();
        assert userGroupsLock.isWriteLockedByCurrentThread();
        // copy user set before looping to avoid concurrent modification exception
        final Set<User> oldUsers = new HashSet<>();
        Util.addAll(group.getUsers(), oldUsers);
        for (final User proxyUser : oldUsers) {
            group.remove(proxyUser);
            final User realUser = users.get(proxyUser.getName());
            if (realUser == null) {
                logger.warning("Couldn't find user " + proxyUser.getName() + " which was part of user group "
                        + group.getName());
            } else {
                group.add(realUser);
            }
        }
    }

    /**
     * used to deserialize the object, thus called during initialization and not subject to concurrency. No locks needed
     */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        preferenceConverters = new HashMap<>();
        preferenceObjects = new HashMap<>();
        preferenceListeners = new HashMap<>();
        usersLock = new NamedReentrantReadWriteLock("Users", /* fair */ false);
        userGroupsLock = new NamedReentrantReadWriteLock("User Groups", /* fair */ false);
        preferenceLock = new NamedReentrantReadWriteLock("Preferences", /* fair */ false);
    }

    protected Object readResolve() {
        LockUtil.executeWithWriteLock(usersLock, () -> {
            for (final User user : getUsers()) {
                if (user instanceof UserImpl) {
                    ((UserImpl) user).setUserGroupProvider(this);
                }
            }
        });
        return this;
    }

    @Override
    public void clear() {
        LockUtil.executeWithWriteLock(usersLock, () -> {
            LockUtil.executeWithWriteLock(userGroupsLock, () -> {
                LockUtil.executeWithWriteLock(preferenceLock, () -> {
                    if (mongoObjectFactory != null) {
                        users.values().forEach(mongoObjectFactory::deleteUser);
                        userGroups.values().forEach(mongoObjectFactory::deleteUserGroup);
                        roleDefinitions.values().forEach(mongoObjectFactory::deleteRoleDefinition);
                        mongoObjectFactory.deleteAllPreferences();
                        mongoObjectFactory.deleteAllSettings();
                    }
                    removeAll();
                });
            });
        });
    }

    /**
     * To call this method, the caller must have obtained the write lock of {@link #usersLock} and {@link #userGroupsLock}.
     */
    private void removeAll() {
        assert usersLock.isWriteLockedByCurrentThread();
        assert userGroupsLock.isWriteLockedByCurrentThread();
        userGroups.clear();
        userGroupsByName.clear();
        userGroupsContainingUser.clear();
        usersInUserGroups.clear();
        roleDefinitionsToUserGroups.clear();
        clearAllPreferenceObjects();
        emailForUsername.clear();
        settings.clear();
        settingTypes.clear();
        users.clear();
        roleDefinitionsToUsers.clear();
        roleDefinitions.clear();
        usersByEmail.clear();
        usersByAccessToken.clear();
    }

    /**
     * To call this method, the caller must have obtained the write lock of {@link #preferenceLock}.
     * 
     * Preference objects can't be simply removed by clearing {@link #preferenceObjects} because listeners can have a
     * state depending on the current preference objects. So we need to notify all listeners about the removal of the
     * notification objects.
     */
    private void clearAllPreferenceObjects() {
        assert preferenceLock.isWriteLockedByCurrentThread();
        final Set<String> usersToProcess = new HashSet<>(preferences.keySet());
        for (String username : usersToProcess) {
            removeAllPreferencesForUser(username);
        }
    }

    @Override
    public void replaceContentsFrom(UserStore newUserStore) {
        clear();
        LockUtil.executeWithWriteLock(usersLock, () -> {
            LockUtil.executeWithWriteLock(userGroupsLock, () -> {
                for (UserGroup group : newUserStore.getUserGroups()) {
                    userGroups.put(group.getId(), group);
                    userGroupsByName.put(group.getName(), group);
                    final HashSet<User> usersInGroup = new HashSet<>();
                    Util.addAll(group.getUsers(), usersInGroup);
                    usersInUserGroups.put(group, usersInGroup);
                    for (final User userInGroup : group.getUsers()) {
                        Util.addToValueSet(userGroupsContainingUser, userInGroup, group);
                    }
                }
                for (RoleDefinition roleDefinition : newUserStore.getRoleDefinitions()) {
                    roleDefinitions.put(roleDefinition.getId(), roleDefinition);
                }
                LockUtil.executeWithWriteLock(preferenceLock, () -> {
                    for (final User user : newUserStore.getUsers()) {
                        user.setUserGroupProvider(this);
                        users.put(user.getName(), user);
                        addToUsersByEmail(user);
                        for (Entry<String, String> userPref : newUserStore.getAllPreferences(user.getName()).entrySet()) {
                            setPreferenceInternalAndUpdatePreferenceObjectIfConverterIsAvailable(user.getName(), userPref.getKey(), userPref.getValue(),
                                    /* don't store; we're a replica, and our persistent store doesn't need to guarantee anything; see bug 5597 */ false);
                            if (userPref.getKey().equals(ACCESS_TOKEN_KEY)) {
                                usersByAccessToken.put(userPref.getValue(), user);
                            }
                        }
                    }
                });
                for (Entry<String, Object> setting : newUserStore.getAllSettings().entrySet()) {
                    settings.put(setting.getKey(), setting.getValue());
                }
                for (Entry<String, Class<?>> settingType : newUserStore.getAllSettingTypes().entrySet()) {
                    settingTypes.put(settingType.getKey(), settingType.getValue());
                }
            });
        });
    }

    @Override
    public Iterable<RoleDefinition> getRoleDefinitions() {
        return new ArrayList<>(roleDefinitions.values());
    }

    @Override
    public RoleDefinition getRoleDefinition(UUID roleId) {
        return roleDefinitions.get(roleId);
    }

    @Override
    public RoleDefinition createRoleDefinition(UUID roleDefinitionId, String displayName,
            Iterable<WildcardPermission> permissions) {
        final RoleDefinition roleDefinition = RoleDefinitionImpl.create(roleDefinitionId, displayName, permissions);
        for (RoleDefinition value : roleDefinitions.values()) {
            if (value.getName().equals(roleDefinition.getName())) {
                throw new IllegalArgumentException("Role Definition with same name already exists");
            }
        }
        roleDefinitions.put(roleDefinitionId, roleDefinition);
        mongoObjectFactory.storeRoleDefinition(roleDefinition);
        return roleDefinition;
    }

    @Override
    public void setRoleDefinitionPermissions(UUID roleDefinitionId, Set<WildcardPermission> permissions) {
        final RoleDefinition roleDefinition = roleDefinitions.get(roleDefinitionId);
        final RoleDefinition newRoleDefinition = new RoleDefinitionImpl(roleDefinitionId, roleDefinition.getName(), permissions);
        mongoObjectFactory.storeRoleDefinition(newRoleDefinition);
        roleDefinition.setPermissions(permissions);
    }

    @Override
    public void addRoleDefinitionPermission(UUID roleId, WildcardPermission permission) {
        RoleDefinition roleDefinition = roleDefinitions.get(roleId);
        Set<WildcardPermission> permissions = roleDefinition.getPermissions();
        permissions.add(permission);
        roleDefinition = new RoleDefinitionImpl(roleId, roleDefinition.getName(), permissions);
        mongoObjectFactory.storeRoleDefinition(roleDefinition);
    }

    @Override
    public void removeRoleDefinitionPermission(UUID roleId, WildcardPermission permission) {
        RoleDefinition roleDefinition = roleDefinitions.get(roleId);
        Set<WildcardPermission> permissions = roleDefinition.getPermissions();
        permissions.remove(permission);
        roleDefinition = new RoleDefinitionImpl(roleId, roleDefinition.getName(), permissions);
        mongoObjectFactory.storeRoleDefinition(roleDefinition);
    }

    @Override
    public void setRoleDefinitionDisplayName(UUID roleId, String displayName) {
        RoleDefinition roleDefinition = roleDefinitions.get(roleId);
        roleDefinition = new RoleDefinitionImpl(roleId, displayName, roleDefinition.getPermissions());
        mongoObjectFactory.storeRoleDefinition(roleDefinition);
    }

    @Override
    public void removeRoleDefinition(RoleDefinition roleDefinition) {
        if(roleDefinition == null) {
            return;
        }
        LockUtil.executeWithWriteLock(usersLock, () -> {
            LockUtil.executeWithWriteLock(userGroupsLock, () -> {
                final Set<User> usersHavingRoleWithRoleDefiniton = roleDefinitionsToUsers.get(roleDefinition);
                if (usersHavingRoleWithRoleDefiniton != null) {
                    for (User user : usersHavingRoleWithRoleDefiniton) {
                        for (Role role : user.getRoles()) {
                            if (role.getRoleDefinition().equals(roleDefinition)) {
                                user.removeRole(role);
                                mongoObjectFactory.storeUser(user);
                            }
                        }
                    }
                    roleDefinitionsToUsers.remove(roleDefinition);
                }
                final Set<UserGroup> userGroupsHavingRoleWithRoleDefinition = roleDefinitionsToUserGroups.get(roleDefinition);
                if (userGroupsHavingRoleWithRoleDefinition != null) {
                    for (UserGroup userGroup : userGroupsHavingRoleWithRoleDefinition) {
                        if (userGroup.getRoleAssociation(roleDefinition)) {
                            userGroup.remove(roleDefinition);
                            mongoObjectFactory.storeUserGroup(userGroup);
                        }
                    }
                    roleDefinitionsToUserGroups.remove(roleDefinition);
                }
                roleDefinitions.remove(roleDefinition.getId());
                mongoObjectFactory.deleteRoleDefinition(roleDefinition);
            });
        });
    }

    @Override
    public boolean setAccessToken(String username, String accessToken) {
       return LockUtil.executeWithWriteLockAndResult(usersLock, () -> {
            final boolean result;
            final User user = getUserByName(username);
            if (user == null) {
                result = false;
            } else {
                result = true;
                final String oldAccessToken = getPreference(username, ACCESS_TOKEN_KEY);
                if (oldAccessToken != null) {
                    usersByAccessToken.remove(oldAccessToken);
                }
                usersByAccessToken.put(accessToken, user);
                setPreference(username, ACCESS_TOKEN_KEY, accessToken);
            }
            return result;
        });
    }

    @Override
    public String getAccessToken(String username) {
        return getPreference(username, ACCESS_TOKEN_KEY);
    }

    @Override
    public void removeAccessToken(String username) {
        LockUtil.executeWithWriteLock(usersLock, () -> {
            User user = users.get(username);
            if (user != null) {
                final String accessToken = getPreference(username, ACCESS_TOKEN_KEY);
                if (accessToken != null) {
                    usersByAccessToken.remove(accessToken);
                }
                // the access token actually existed; now we need to update the preferences
                unsetPreference(username, ACCESS_TOKEN_KEY);
            }
        });
    }

    /**
     * To call this method, the caller must have obtained the write lock of {@link #usersLock}.
     */
    private void addToUsersByEmail(User u) {
        assert usersLock.isWriteLockedByCurrentThread();
        if (u.getEmail() != null && !u.getEmail().isEmpty()) {
            Set<User> set = usersByEmail.get(u.getEmail());
            if (set == null) {
                set = new HashSet<>();
                usersByEmail.put(u.getEmail(), set);
            }
            set.add(u);
            emailForUsername.put(u.getName(), u.getEmail());
        }
    }

    /**
     * To call this method, the caller must have obtained the write lock of {@link #usersLock}.
     */
    private void removeFromUsersByEmail(User u) {
        assert usersLock.isWriteLockedByCurrentThread();
        if (u != null) {
            final String email = emailForUsername.remove(u.getName());
            if (email != null) {
                Set<User> set = usersByEmail.get(email); // this also works if the user's e-mail has changed meanwhile
                if (set != null) {
                    set.remove(u);
                }
            }
        }
    }

    private boolean initSocialSettingsIfEmpty() {
        boolean changed = false;
        for (SocialSettingsKeys ssk : SocialSettingsKeys.values()) {
            if (settingTypes.get(ssk.name()) == null || settings.get(ssk.name()) == null) {
                addSetting(ssk.name(), String.class);
                setSetting(ssk.name(), ssk.getValue());
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Iterable<UserGroup> getUserGroups() {
        return LockUtil.executeWithReadLockAndResult(userGroupsLock, () -> {
            return new ArrayList<>(userGroups.values());
        });
    }

    @Override
    public UserGroup getUserGroupByName(String name) {
        return LockUtil.executeWithReadLockAndResult(userGroupsLock, () -> {
            return name == null ? null : userGroupsByName.get(name);
        });
    }

    @Override
    public Iterable<UserGroup> getUserGroupsWithRoleDefinition(RoleDefinition roleDefinition) {
        return LockUtil.executeWithReadLockAndResult(userGroupsLock, () -> {
            return roleDefinition == null ? null : new HashSet<>(roleDefinitionsToUserGroups.get(roleDefinition));
        });
    }

    @Override
    public UserGroup getUserGroup(UUID id) {
        return LockUtil.executeWithReadLockAndResult(userGroupsLock, () -> {
            return id == null ? null : userGroups.get(id);
        });
    }

    @Override
    public UserGroupImpl createUserGroup(UUID groupId, String name) throws UserGroupManagementException {
        return LockUtil.executeWithWriteLockAndResultExpectException(userGroupsLock, () -> {
            checkGroupNameAndIdUniqueness(groupId, name);
            logger.info("Creating user group: " + groupId + " with name " + name);
            UserGroupImpl group = new UserGroupImpl(groupId, name);
            if (mongoObjectFactory != null) {
                mongoObjectFactory.storeUserGroup(group);
            }
            addGroupToInternalMaps(group);
            return group;
        });
    }
    
    /**
     * To call this method, the caller must have obtained the write lock of {@link #userGroupsLock}.
     */
    private void addGroupToInternalMaps(UserGroup group) {
        assert userGroupsLock.isWriteLockedByCurrentThread();
        userGroups.put(group.getId(), group);
        userGroupsByName.put(group.getName(), group);
    }

    /**
     * To call this method, the caller must have obtained the read lock of {@link #userGroupsLock}.
     */
    private void checkGroupNameAndIdUniqueness(UUID groupId, String name) throws UserGroupManagementException {
        assert userGroupsLock.isWriteLockedByCurrentThread() || userGroupsLock.getReadHoldCount() > 0;
        if (userGroupsByName.containsKey(name)) {
            throw new UserGroupManagementException(UserGroupManagementException.USER_GROUP_ALREADY_EXISTS+": "+name);
        }
        if (userGroups.containsKey(groupId)) {
            throw new UserGroupManagementException(UserGroupManagementException.USER_GROUP_ALREADY_EXISTS+": "+groupId);
        }
    }

    @Override
    public void addUserGroup(UserGroup group) throws UserGroupManagementException {
        LockUtil.executeWithWriteLockExpectException(userGroupsLock, () -> {
            checkGroupNameAndIdUniqueness(group.getId(), group.getName());
            addGroupToInternalMaps(group);
            updateUserGroup(group);
        });
    }

    @Override
    public void updateUserGroup(UserGroup group) {
        logger.info("Updating user group " + group.getName() + " in DB");
        LockUtil.executeWithWriteLock(userGroupsLock, () -> {
            Set<User> usersInGroupBefore = new HashSet<>();
            Util.addAll(usersInUserGroups.get(group), usersInGroupBefore);
            for (final User userNowInUpdatedGroup : group.getUsers()) {
                if (usersInGroupBefore == null || !Util.contains(usersInGroupBefore, userNowInUpdatedGroup)) {
                    // the user was added:
                    Util.addToValueSet(usersInUserGroups, group, userNowInUpdatedGroup);
                    Util.addToValueSet(userGroupsContainingUser, userNowInUpdatedGroup, group);
                }
            }
            for (final User userInGroupBefore : usersInGroupBefore) {
                if (!Util.contains(group.getUsers(), userInGroupBefore)) {
                    // the user was removed
                    Util.removeFromValueSet(usersInUserGroups, group, userInGroupBefore);
                    Util.removeFromValueSet(userGroupsContainingUser, userInGroupBefore, group);
                }
            }
            Util.removeFromAllValueSets(roleDefinitionsToUserGroups, group);
            for (RoleDefinition roleInUserGroup : group.getRoleDefinitionMap().keySet()) {
                Util.addToValueSet(roleDefinitionsToUserGroups, roleInUserGroup, group);
            }
        });
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storeUserGroup(group);
        }
    }
    
    /**
     * To call this method, the caller must have obtained the read lock of {@link #userGroupsLock}.
     */
    private void removeUserFromUserGroup(User user , UserGroup group) {
        assert userGroupsLock.isWriteLockedByCurrentThread();
        group.remove(user);
        Util.removeFromValueSet(usersInUserGroups, group, user);
        Util.removeFromValueSet(userGroupsContainingUser, user, group);
    }
    
    @Override
    public Iterable<UserGroup> getUserGroupsOfUser(User user) {
        return LockUtil.executeWithReadLockAndResult(userGroupsLock, () -> {
            final Set<UserGroup> userGroups = userGroupsContainingUser.get(user);
            return userGroups == null ? Collections.emptySet() : new HashSet<>(userGroups);
        });
    }

    private void deleteUserGroupAndRemoveRelations(UserGroup userGroup) throws UserGroupManagementException {
        assert userGroupsLock.isWriteLockedByCurrentThread();
        if (!userGroups.containsKey(userGroup.getId())) {
            throw new UserGroupManagementException(UserGroupManagementException.USER_GROUP_DOES_NOT_EXIST);
        }
        logger.info("Deleting user group: " + userGroup);
        userGroupsByName.remove(userGroup.getName());
        userGroups.remove(userGroup.getId());
        for (final User userInDeletedGroup : userGroup.getUsers()) {
            Util.removeFromValueSet(userGroupsContainingUser, userInDeletedGroup, userGroup);
        }
        usersInUserGroups.remove(userGroup);
        userGroup.getRoleDefinitionMap().keySet()
                .forEach(role -> Util.removeFromValueSet(roleDefinitionsToUserGroups, role, userGroup));
        deleteUserGroupFromDB(userGroup);
    }

    /**
     * To call this method, the caller must have obtained the write lock of {@link #userGroupsLock}.
     */
    private void deleteUserGroupFromDB(UserGroup userGroup) {
        assert userGroupsLock.isWriteLockedByCurrentThread();
        if (mongoObjectFactory != null) {
            mongoObjectFactory.deleteUserGroup(userGroup);
        }
    }

    @Override
    public User createUser(String name, String email, LockingAndBanning lockingAndBanning, Account... accounts)
            throws UserManagementException {
        return LockUtil.executeWithWriteLockAndResultExpectException(usersLock, () -> {
            checkUsernameUniqueness(name);
            final Map<String, UserGroup> tenantsForServer = new ConcurrentHashMap<>();
            final User user = new UserImpl(name, email, tenantsForServer, /* user group provider */ this, lockingAndBanning, accounts);
            logger.info("Creating user: " + user + " with e-mail " + email);
            addAndStoreUserInternal(user);
            return user;
        });
    }
    
    /**
     * To call this method, the caller must have obtained the write lock of {@link #usersLock}.
     */
    private void addAndStoreUserInternal(User user) {
        assert usersLock.isWriteLockedByCurrentThread();
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storeUser(user);
        }
        users.put(user.getName(), user);
        addToUsersByEmail(user);
    }
    
    /**
     * To call this method, the caller must have obtained the write lock of {@link #usersLock}.
     */
    private void checkUsernameUniqueness(String name) throws UserManagementException {
        assert usersLock.isWriteLockedByCurrentThread();
        if (getUserByName(name) != null) {
            throw new UserManagementException(UserManagementException.USER_ALREADY_EXISTS);
        }
    }
    
    @Override
    public void addUser(User user) throws UserManagementException {
        LockUtil.executeWithWriteLockExpectException(usersLock, () -> {
            checkUsernameUniqueness(user.getName());
            logger.info("Adding user: "+user);
            addAndStoreUserInternal(user);
        });
    }

    @Override
    public void updateUser(User user) {
        LockUtil.executeWithWriteLock(usersLock, () -> {
            logger.info("Updating user " + user + " in DB");
            users.put(user.getName(), user);
            removeFromUsersByEmail(user);
            addToUsersByEmail(user);
            if (mongoObjectFactory != null) {
                mongoObjectFactory.storeUser(user);
            }
        });
    }

    @Override
    public Iterable<User> getUsers() {
        return LockUtil.executeWithReadLockAndResult(usersLock, () -> {
            return new ArrayList<User>(users.values());
        });
    }

    @Override
    public boolean hasUsers() {
        return LockUtil.executeWithReadLockAndResult(usersLock, () -> {
            return !users.isEmpty();
        });
    }

    @Override
    public User getUserByName(String name) {
        final User result;
        if (name == null) {
            result = null;
        } else {
            result = LockUtil.executeWithReadLockAndResult(usersLock, () -> users.get(name));
        }
        return result;
    }

    @Override
    public User getUserByAccessToken(String accessToken) {
        return LockUtil.executeWithReadLockAndResult(usersLock, () -> {
            final User result;
            if (accessToken == null) {
                result = null;
            } else {
                result = usersByAccessToken.get(accessToken);
            }
            return result;
        });
    }

    @Override
    public User getUserByEmail(String email) {
        return LockUtil.executeWithReadLockAndResult(usersLock, () -> {
            final User result;
            if (email == null) {
                result = null;
            } else {
                Set<User> set = usersByEmail.get(email);
                if (set == null || set.isEmpty()) {
                    result = null;
                } else {
                    result = set.iterator().next();
                }
            }
            return result;
        });
    }

    @Override
    public Pair<Boolean, Set<Ownership>> getExistingQualificationsForRoleDefinition(RoleDefinition roleToCheck) {
        return LockUtil.executeWithReadLockAndResult(usersLock, () -> {
            return LockUtil.executeWithReadLockAndResult(userGroupsLock, () -> {
                final Set<Ownership> ownerships = new HashSet<>();
                final Set<User> usersHavingRole = roleDefinitionsToUsers.get(roleToCheck);
                if (usersHavingRole != null) {
                    for (User user : usersHavingRole) {
                        for (Role role : user.getRoles()) {
                            if (!role.getRoleDefinition().equals(roleToCheck)) {
                                // wrong role
                                continue;
                            }
                            if (role.getQualifiedForTenant() == null && role.getQualifiedForUser() == null) {
                                // wildcard rule exists -> return A=true
                                return new Pair<>(true, null);
                            } else {
                                ownerships.add(new Ownership(role.getQualifiedForUser(), role.getQualifiedForTenant()));
                            }
                        }
                    }
                }
                final Set<UserGroup> userGroupsHavingRole = roleDefinitionsToUserGroups.get(roleToCheck);
                if (userGroupsHavingRole != null) {
                    for (UserGroup userGroup : userGroupsHavingRole) {
                        if (userGroup.getRoleDefinitionMap().containsKey(roleToCheck)) {
                            ownerships.add(new Ownership(null, userGroup));
                        }
                    }
                }
                return new Pair<>(false, ownerships);
            });
        });
    }

    /**
     * To call this method, the caller must have obtained the write lock of {@link #usersLock}.
     */
    @Override
    public Set<Pair<User, Role>> getRolesQualifiedByUserGroup(UserGroup groupQualification) {
        return LockUtil.executeWithReadLockAndResult(usersLock, () -> {
            final Set<Pair<User, Role>> result = new HashSet<>();
            for (User user : getUsers()) {
                try {
                    for (Role role : getRolesFromUser(user.getName())) {
                        if (groupQualification.equals(role.getQualifiedForTenant())) {
                            result.add(new Pair<>(user, role));
                        }
                    }
                } catch (UserManagementException e) {
                    // user did not exist -> should not happen
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            }
            return result;
        });
    }

    @Override
    public Iterable<Role> getRolesFromUser(String username) throws UserManagementException {
        return LockUtil.executeWithReadLockAndResultExpectException(usersLock, () ->{
            if (users.get(username) == null) {
                throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
            }
            Iterable<Role> roles = users.get(username).getRoles();
            return Util.addAll(roles, new HashSet<>());
        });
    }

    @Override
    public void addRoleForUser(String name, Role role) throws UserManagementException {
        LockUtil.executeWithWriteLockExpectException(usersLock, () -> {
            final User user = users.get(name);
            if (user == null) {
                throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
            }
            user.addRole(role);
            Util.addToValueSet(roleDefinitionsToUsers, role.getRoleDefinition(), user);
            if (mongoObjectFactory != null) {
                mongoObjectFactory.storeUser(user);
            }
        });
    }

    @Override
    public void removeRoleFromUser(String name, Role role) throws UserManagementException {
        LockUtil.executeWithWriteLockExpectException(usersLock, () -> {
            final User user = users.get(name);
            if (user == null) {
                throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
            }
            user.removeRole(role);
            RoleDefinition roleDefinition = role.getRoleDefinition();
            boolean roleDefinitionStillGrantedToUser = false;
            for (Role roleOfUser : user.getRoles()) {
                if (roleDefinition.equals(roleOfUser.getRoleDefinition())) {
                    roleDefinitionStillGrantedToUser = true;
                    break;
                }
            }
            if (!roleDefinitionStillGrantedToUser) {
                Util.removeFromValueSet(roleDefinitionsToUsers, roleDefinition, user);
            }
            if (mongoObjectFactory != null) {
                mongoObjectFactory.storeUser(users.get(name));
            }
        });
    }

    @Override
    public void addPermissionForUser(String username, WildcardPermission permission) throws UserManagementException {
        LockUtil.executeWithWriteLockExpectException(usersLock, () -> {
            final User user = users.get(username);
            if (user == null) {
                throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
            }
            user.addPermission(permission);
            if (mongoObjectFactory != null) {
                mongoObjectFactory.storeUser(user);
            }
        });
    }

    @Override
    public void removePermissionFromUser(String name, WildcardPermission permission) throws UserManagementException {
        LockUtil.executeWithWriteLockExpectException(usersLock, () -> {
            if (users.get(name) == null) {
                throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
            }
            users.get(name).removePermission(permission);
            if (mongoObjectFactory != null) {
                mongoObjectFactory.storeUser(users.get(name));
            }
        });
    }

    @Override
    public void deleteUser(String name) throws UserManagementException {
        LockUtil.executeWithWriteLockExpectException(usersLock, () -> {
            final User user = users.get(name);
            if (user == null) {
                throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
            }
            logger.info("Deleting user: " + users.get(name).toString());
            if (mongoObjectFactory != null) {
                mongoObjectFactory.deleteUser(user);
            }
            users.remove(name);
            removeFromUsersByAccessToken(user);
            removeFromUsersByEmail(user);
            removeAllQualifiedRolesForUser(user);
            user.getRoles().forEach(role -> Util.removeFromValueSet(roleDefinitionsToUsers, role.getRoleDefinition(), user));
            // also remove from all usergroups
            LockUtil.executeWithWriteLock(userGroupsLock, () -> {
                for (UserGroup userGroup : user.getUserGroups()) {
                    removeUserFromUserGroup(user, userGroup);
                }
            });
        });
        LockUtil.executeWithWriteLock(preferenceLock, () -> removeAllPreferencesForUser(name));
        
    }
    
    private void removeFromUsersByAccessToken(User user){
        assert usersLock.isWriteLockedByCurrentThread();
        final Set<String> entriesToRemove = new HashSet<>();
        for (Entry<String, User> entry : usersByAccessToken.entrySet()) {
            if (entry.getValue().equals(user)) {
                entriesToRemove.add(entry.getKey());
            }
        }
        entriesToRemove.forEach(entry -> usersByAccessToken.remove(entry));
    }

    @Override
    public <T> T getSetting(String key, Class<T> clazz) {
        Class<?> settingClazz = settingTypes.get(key);
        if (settingClazz == null) {
            return null;
        }
        if (!settingClazz.equals(clazz)) {
            throw new IllegalArgumentException("Value for \"" + key + "\" is not of type \"" + clazz.getName() + "\"!");
        }
        return clazz.cast(settings.get(key));
    }

    @Override
    public void addSetting(String key, Class<?> type) {
        settingTypes.put(key, type);
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storeSettingTypes(settingTypes);
        }
    }

    @Override
    public boolean setSetting(String key, Object setting) {
        final boolean result;
        Class<?> clazz = settingTypes.get(key);
        if (clazz == null || !clazz.isInstance(setting)) {
            result = false;
        } else {
            settings.put(key, setting);
            if (mongoObjectFactory != null) {
                mongoObjectFactory.storeSettings(settings);
            }
            result = true;
        }
        return result;
    }

    @Override
    public void setPreference(String username, String key, String value) {
        LockUtil.executeWithWriteLock(preferenceLock, () -> {
            setPreferenceInternalAndUpdatePreferenceObjectIfConverterIsAvailable(username, key, value, /* store */ true);
        });
    }

    private void setPreferenceInternalAndUpdatePreferenceObjectIfConverterIsAvailable(String username, String key, String value, boolean store) {
        assert preferenceLock.isWriteLockedByCurrentThread();
        setPreferenceInternal(username, key, value, store);
        updatePreferenceObjectIfConverterIsAvailable(username, key);
    }

    private void setPreferenceInternal(String username, String key, String value, boolean store) {
        assert preferenceLock.isWriteLockedByCurrentThread();
        Map<String, String> userMap = preferences.get(username);
        if (value == null && userMap != null) {
            userMap.remove(key);
            if (userMap.isEmpty()) {
                preferences.remove(username);
            }
        } else if (value != null) {
            if (userMap == null) {
                userMap = new HashMap<>();
                preferences.put(username, userMap);
            }
            userMap.put(key, value);
        }
        if (store && mongoObjectFactory != null) {
            mongoObjectFactory.storePreferences(username, userMap);
        }
    }

    @Override
    public void unsetPreference(String username, String key) {
        LockUtil.executeWithWriteLock(preferenceLock, () -> {
            Map<String, String> userMap = preferences.get(username);
            if (userMap != null) {
                userMap.remove(key);
                if (mongoObjectFactory != null) {
                    mongoObjectFactory.storePreferences(username, userMap);
                }
            }
            unsetPreferenceObject(username, key);
        });
    }

    @Override
    public String getPreference(String username, String key) {
        return LockUtil.executeWithWriteLockAndResult(preferenceLock, () -> {
            final String result;
            Map<String, String> userMap = preferences.get(username);
            if (userMap != null) {
                result = userMap.get(key);
            } else {
                result = null;
            }
            return result;
        });
    }

    @Override
    public Map<String, String> getAllPreferences(String username) {
        return LockUtil.executeWithReadLockAndResult(preferenceLock, () -> {
            final Map<String, String> userPrefs = preferences.get(username);
            final Map<String, String> result;
            if (userPrefs == null) {
                result = Collections.emptyMap();
            } else {
                result = Collections.unmodifiableMap(userPrefs);
            }
            return result;
        });
    }

    /**
     * To call this method, the caller must have obtained the write lock of {@link #preferenceLock}.
     */
    private void removeAllPreferencesForUser(String username) {
        assert preferenceLock.isWriteLockedByCurrentThread();
        // TODO should we keep the preferences anonymized (e.g. use a UUID as username) to enable better statistics?
        preferences.remove(username);
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storePreferences(username, Collections.<String, String> emptyMap());
        }
        removeAllPreferenceObjectsForUser(username);
    }

    /**
     * To call this method, the caller must have obtained the write lock of {@link #preferenceLock}.
     */
    private void removeAllPreferenceObjectsForUser(String username) {
        assert preferenceLock.isWriteLockedByCurrentThread();
        final Map<String, Object> preferenceObjectsToRemove = preferenceObjects.remove(username);
        if (preferenceObjectsToRemove != null) {
            for (Map.Entry<String, Object> entry : preferenceObjectsToRemove.entrySet()) {
                notifyListenersOnPreferenceObjectChange(username, entry.getKey(), entry.getValue(), null);
            }
        }
    }

    @Override
    public Map<String, Object> getAllSettings() {
        return new HashMap<>(settings);
    }

    @Override
    public Map<String, Class<?>> getAllSettingTypes() {
        return new HashMap<>(settingTypes);
    }

    @Override
    public void registerPreferenceConverter(String preferenceKey, PreferenceConverter<?> converter) {
        LockUtil.executeWithWriteLock(preferenceLock, () -> {
            PreferenceConverter<?> alreadyAssociatedConverter = preferenceConverters.putIfAbsent(preferenceKey, converter);
            if (alreadyAssociatedConverter == null) {
                final Set<String> usersToProcess = new HashSet<>(preferences.keySet());
                for (String user : usersToProcess) {
                    updatePreferenceObjectWithConverter(user, preferenceKey, converter);
                }
            } else {
                logger.log(Level.SEVERE, "PreferenceConverter " + alreadyAssociatedConverter + " for key " + preferenceKey
                        + " is already registered. Converter " + converter + " will not be registered");
            }
        });
    }

    @Override
    public void removePreferenceConverter(String preferenceKey) {
        LockUtil.executeWithWriteLock(preferenceLock, () -> {
            PreferenceConverter<?> preferenceConverterToRemove = preferenceConverters.remove(preferenceKey);
            if (preferenceConverterToRemove != null) {
                final Set<String> usersToProcess = new HashSet<>(preferences.keySet());
                for (String username : usersToProcess) {
                    unsetPreferenceObject(username, preferenceKey);
                }
            } else {
                logger.log(Level.WARNING,
                        "PreferenceConverter for key " + preferenceKey + " should be removed but wasn't registered");
            }
        });
    }

    /**
     * To call this method, the caller must have obtained the write lock of {@link #preferenceLock}.
     */
    private void updatePreferenceObjectIfConverterIsAvailable(String username, String key) {
        assert preferenceLock.isWriteLockedByCurrentThread();
        PreferenceConverter<?> preferenceConverter = preferenceConverters.get(key);
        if (preferenceConverter != null) {
            updatePreferenceObjectWithConverter(username, key, preferenceConverter);
        }
    }

    private void updatePreferenceObjectWithConverter(String username, String key,
            PreferenceConverter<?> preferenceConverter) {
        final String preferenceString = getPreference(username, key);
        if (preferenceString != null) {
            try {
                final Object convertedObject = preferenceConverter.toPreferenceObject(preferenceString);
                setPreferenceObjectInternal(username, key, convertedObject);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Error while converting preference for key " + key + " from String \""
                        + preferenceString + "\"", t);
            }
        }
    }

    private void setPreferenceObjectInternal(String username, String key, final Object convertedObject) {
        assert preferenceLock.isWriteLockedByCurrentThread();
        Map<String, Object> userMap = preferenceObjects.get(username);
        if (userMap == null) {
            userMap = new HashMap<>();
            preferenceObjects.put(username, userMap);
        }
        // if the new preference object is simply null, we remove the entry instead of putting null
        Object oldPreference = convertedObject == null ? userMap.remove(key) : userMap.put(key, convertedObject);
        if (oldPreference != null || convertedObject != null) {
            // preference hasn't changed if it was null and is now null
            notifyListenersOnPreferenceObjectChange(username, key, oldPreference, convertedObject);
        }
    }

    private void unsetPreferenceObject(String username, String key) {
        assert preferenceLock.isWriteLockedByCurrentThread();
        final Map<String, Object> userObjectMap = preferenceObjects.get(username);
        if (userObjectMap != null) {
            Object oldPreference = userObjectMap.remove(key);
            if (oldPreference != null) {
                notifyListenersOnPreferenceObjectChange(username, key, oldPreference, null);
            }
            if (userObjectMap.isEmpty()) {
                preferenceObjects.remove(username);
            }
        }
    }

    @Override
    public <T> T getPreferenceObject(String username, String key) {
        return LockUtil.executeWithReadLockAndResult(preferenceLock, () -> {
            final Object result;
            final Map<String, Object> userMap = preferenceObjects.get(username);
            if (userMap != null) {
                result = userMap.get(key);
            } else {
                result = null;
            }
            @SuppressWarnings("unchecked")
            T resultT = (T) result;
            return resultT;
        });
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getPreferenceObjectsByKey(String key) {
        return LockUtil.executeWithReadLockAndResult(preferenceLock, () -> {
            final Map<String, T> result = new HashMap<>();
            for (Entry<String, Map<String, Object>> userWithPreferences : preferenceObjects.entrySet()) {
                final Map<String, Object> userPreferences = userWithPreferences.getValue();
                final Object userPreference = userPreferences.get(key);
                if(userPreference != null) {
                    result.put(userWithPreferences.getKey(), (T) userPreference);
                }
            }
            return Collections.unmodifiableMap(result);
        });
    }

    @Override
    public String setPreferenceObject(String username, String key, Object preferenceObject)
            throws IllegalArgumentException {
        return LockUtil.executeWithWriteLockAndResultExpectException(preferenceLock, () -> {
            @SuppressWarnings("unchecked")
            PreferenceConverter<Object> preferenceConverter = (PreferenceConverter<Object>) preferenceConverters.get(key);
            if (preferenceConverter == null) {
                throw new IllegalArgumentException(
                        "Tried to set preference for key " + key + " but there is no converter associated!");
            }
            String stringPreference = null;
            if (preferenceObject == null) {
                unsetPreference(username, key);
            } else {
                try {
                    stringPreference = preferenceConverter.toPreferenceString(preferenceObject);
                    setPreferenceInternal(username, key, stringPreference, /* store */ true);
                    setPreferenceObjectInternal(username, key, preferenceObject);
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, "Error while converting preference for key " + key + " from Object \""
                            + preferenceObject + "\"", t);
                }
            }
            return stringPreference;
        });
    }
    
    /**
     * To call this method, the caller must have obtained the read lock of {@link #preferenceLock}.
     */
    private void notifyListenersOnPreferenceObjectChange(String username, String key, Object oldPreference,
            Object newPreference) {
        assert preferenceLock.isWriteLockedByCurrentThread() || preferenceLock.getReadHoldCount() > 0;
        for (PreferenceObjectListener<? extends Object> listener : Util.get(preferenceListeners, key,
                Collections.<PreferenceObjectListener<? extends Object>> emptySet())) {
            @SuppressWarnings("unchecked")
            PreferenceObjectListener<Object> listenerToFire = (PreferenceObjectListener<Object>) listener;
            listenerToFire.preferenceObjectChanged(username, key, oldPreference, newPreference);
        }
    }

    @Override
    public void addPreferenceObjectListener(String key, PreferenceObjectListener<? extends Object> listener,
            boolean fireForAlreadyExistingPreferences) {
        LockUtil.executeWithWriteLock(preferenceLock, () -> {
            Util.addToValueSet(preferenceListeners, key, listener);
            if (fireForAlreadyExistingPreferences) {
                final Set<String> usersToProcess = new HashSet<>(preferences.keySet());
                for (String username : usersToProcess) {
                    Map<String, Object> userMap = preferenceObjects.get(username);
                    if (userMap != null) {
                        Object preferenceObject = userMap.get(key);
                        if (preferenceObject != null) {
                            @SuppressWarnings("unchecked")
                            PreferenceObjectListener<Object> listenerToFire = (PreferenceObjectListener<Object>) listener;
                            listenerToFire.preferenceObjectChanged(username, key, null, preferenceObject);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void removePreferenceObjectListener(PreferenceObjectListener<?> listener) {
        LockUtil.executeWithWriteLock(preferenceLock, () -> {
            Util.removeFromAllValueSets(preferenceListeners, listener);
        });
    }

    private void removeAllQualifiedRolesForUser(User user) {
        LockUtil.executeWithWriteLock(usersLock, () -> {
            for (User checkUser : users.values()) {
                Set<Role> rolesToRemove = new HashSet<>();
                for (Role role : checkUser.getRoles()) {
                    if (Util.equalsWithNull(role.getQualifiedForUser(), user)) {
                        rolesToRemove.add(role);
                    }
                }
                for (Role roleToRremove : rolesToRemove) {
                    try {
                        removeRoleFromUser(checkUser.getName(), roleToRremove);
                    } catch (UserManagementException e) {
                        logger.log(Level.WARNING,
                                "Could not properly update qualified roles on user delete " + roleToRremove);
                    }
                }
            }
        });
    }

    private void removeAllQualifiedRolesForUserGroup(UserGroup userGroup) {
        assert usersLock.isWriteLockedByCurrentThread();
        for (User checkUser : users.values()) {
            Set<Role> rolesToRemove = new HashSet<>();
            for (Role role : checkUser.getRoles()) {
                if (Util.equalsWithNull(role.getQualifiedForTenant(), userGroup)) {
                    rolesToRemove.add(role);
                }
            }
            for (Role roleToRemove : rolesToRemove) {
                try {
                    removeRoleFromUser(checkUser.getName(), roleToRemove);
                } catch (UserManagementException e) {
                    logger.log(Level.WARNING,
                            "Could not properly update qualified roles on userGroup delete " + roleToRemove);
                }
            }
        }
    }

    @Override
    public void deleteUserGroup(UserGroup userGroup) throws UserGroupManagementException {
        LockUtil.executeWithWriteLockExpectException(usersLock, () -> {
            LockUtil.executeWithWriteLockExpectException(userGroupsLock, () -> {
                removeAllQualifiedRolesForUserGroup(userGroup);
                deleteUserGroupAndRemoveRelations(userGroup);
            });
        });
    }

    @Override
    public void setDefaultTennantForUserAndUpdate(User user, UserGroup newDefaultTenant, String serverName) {
        LockUtil.executeWithWriteLock(usersLock, () -> {
            user.setDefaultTenant(newDefaultTenant, serverName);
            updateUser(user);
        });
    }
}
