package com.sap.sse.security.userstore.mongodb;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

import org.apache.shiro.SecurityUtils;

import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.security.PreferenceConverter;
import com.sap.sse.security.PreferenceObjectListener;
import com.sap.sse.security.SocialSettingsKeys;
import com.sap.sse.security.UserImpl;
import com.sap.sse.security.UserStore;
import com.sap.sse.security.shared.Account;
import com.sap.sse.security.shared.AdminRole;
import com.sap.sse.security.shared.Role;
import com.sap.sse.security.shared.RoleImpl;
import com.sap.sse.security.shared.SecurityUser;
import com.sap.sse.security.shared.Tenant;
import com.sap.sse.security.shared.TenantManagementException;
import com.sap.sse.security.shared.User;
import com.sap.sse.security.shared.UserGroup;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.TenantImpl;
import com.sap.sse.security.shared.impl.UserGroupImpl;

/**
 * An implementation of the {@link UserStore} interface, intended to store its state durably in a MongoDB instance.
 * A de-serialized copy, however, will have its {@link #mongoObjectFactory} field set to <code>null</code> and will
 * therefore not perform any changes to the database. This is also the reason why all access to the
 * {@link #mongoObjectFactory} field needs to be <code>null</code>-safe.<p>
 * 
 * The storage pattern for {@link UserGroup} and {@link Tenant} objects deserves some explanation. As a {@link Tenant}
 * is a specialized {@link UserGroup}, this store mainly needs to keep track of the users in that {@link Tenant}. Hence,
 * the same collection is used for the storage of these user lists, and hence the same methods can be used for
 * maintaining this collection. Additionally, the tenant ID is stored in a separate collection as a "marker" which
 * entries in the user groups collection are actually tenants and not only user groups.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class UserStoreImpl implements UserStore {
    private static final long serialVersionUID = -3860868283827473187L;

    private static final Logger logger = Logger.getLogger(UserStoreImpl.class.getName());

    private static final String ACCESS_TOKEN_KEY = "___access_token___";

    private String name = "MongoDB user store";
    
    private final ConcurrentHashMap<UUID, Tenant> tenants;
    private final ConcurrentHashMap<String, Tenant> tenantsByName;
    private final ConcurrentHashMap<UUID, UserGroup> userGroups;
    private final ConcurrentHashMap<String, UserGroup> userGroupsByName;
    
    /**
     * Protects access to the two maps {@link #userGroupsContainingUser} and {@link #usersInUserGroups} which implement
     * an efficient lookup for the m:n association between {@link UserGroup#getUsers()} and {@link SecurityUser}. The
     * collections also contain the relationships for the specialized {@link Tenant} objects which are not part of
     * {@link #userGroups} but of {@link #tenants}.
     */
    private final NamedReentrantReadWriteLock userGroupsUserCacheLock = new NamedReentrantReadWriteLock("User Groups Cache", /* fair */ false);
    private final ConcurrentHashMap<SecurityUser, Set<UserGroup>> userGroupsContainingUser;
    private final ConcurrentHashMap<UserGroup, Set<SecurityUser>> usersInUserGroups;
    
    private final ConcurrentHashMap<String, UserImpl> users;
    private final ConcurrentHashMap<String, Set<UserImpl>> usersByEmail;
    private final ConcurrentHashMap<UUID, Role> roles;

    private final ConcurrentHashMap<String, User> usersByAccessToken;
    private final ConcurrentHashMap<String, String> emailForUsername;
    private final ConcurrentHashMap<String, Object> settings;
    private final ConcurrentHashMap<String, Class<?>> settingTypes;
    
    /**
     * Keys are the usernames, values are the key/value pairs representing the user's preferences
     */
    private final ConcurrentHashMap<String, Map<String, String>> preferences;
    
    /**
     * Converter objects to map preference Strings to Objects.
     * The keys must match the keys of the preferences. 
     */
    private transient ConcurrentHashMap<String, PreferenceConverter<?>> preferenceConverters;
    
    /**
     * This is another view of the String preferences mapped by {@link #preferenceConverters} to Objects.
     * Keys are the usernames, values are the key/value pairs representing the user's preferences.
     */
    private transient ConcurrentHashMap<String, Map<String, Object>> preferenceObjects;
    
    /**
     * Keys are preferences keys as used by {@link #preferenceObjects}, values are the listeners to inform on changes of
     * the specific preference object for a {@link UserImpl}.
     */
    private transient Map<String, Set<PreferenceObjectListener<?>>> listeners;
    
    /**
     * To be used for locking when working with {@link #listeners}.
     */
    private transient NamedReentrantReadWriteLock listenersLock;
    
    /**
     * Won't be serialized and remains <code>null</code> on the de-serializing end.
     */
    private final transient MongoObjectFactory mongoObjectFactory;

    public UserStoreImpl() {
        this(PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory(), PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory());
    }
    
    public UserStoreImpl(final DomainObjectFactory domainObjectFactory, final MongoObjectFactory mongoObjectFactory) {
        tenants = new ConcurrentHashMap<>();
        tenantsByName = new ConcurrentHashMap<>();
        roles = new ConcurrentHashMap<>();
        userGroups = new ConcurrentHashMap<>();
        userGroupsByName = new ConcurrentHashMap<>();
        userGroupsContainingUser = new ConcurrentHashMap<>();
        usersInUserGroups = new ConcurrentHashMap<>();
        users = new ConcurrentHashMap<>();
        usersByEmail = new ConcurrentHashMap<>();
        emailForUsername = new ConcurrentHashMap<>();
        settings = new ConcurrentHashMap<>();
        settingTypes = new ConcurrentHashMap<>();
        usersByAccessToken = new ConcurrentHashMap<>();
        preferences = new ConcurrentHashMap<>();
        preferenceConverters = new ConcurrentHashMap<>();
        preferenceObjects = new ConcurrentHashMap<>();
        listeners = new HashMap<>();
        listenersLock = new NamedReentrantReadWriteLock(
                UserStoreImpl.class.getSimpleName() + " lock for listeners collection", false);
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
            for (Role role : domainObjectFactory.loadAllRoles()) {
                roles.put(UUID.fromString(role.getId().toString()), role);
            }
            for (UserImpl u : domainObjectFactory.loadAllUsers(roles)) {
                users.put(u.getName(), u);
                addToUsersByEmail(u);
            }
            final Pair<Iterable<UserGroup>, Iterable<Tenant>> userGroupsAndTenants = domainObjectFactory.loadAllUserGroupsAndTenants(users);
            for (UserGroup group : userGroupsAndTenants.getA()) {
                userGroups.put(group.getId(), group);
                userGroupsByName.put(group.getName(), group);
            }
            for (final Tenant tenant : userGroupsAndTenants.getB()) {
                tenants.put(tenant.getId(), tenant);
            }
            // the users loaded have dummy default tenant objects which have only their ID set;
            // replace them with the real Tenant objects loaded only now
            for (final User userWithDummyDefaultTenant : users.values()) {
                if (userWithDummyDefaultTenant.getDefaultTenant() != null) {
                    userWithDummyDefaultTenant.setDefaultTenant(getTenant(userWithDummyDefaultTenant.getDefaultTenant().getId()));
                }
            }
            for (Entry<String, Map<String, String>> e : preferences.entrySet()) {
                if (e.getValue() != null) {
                    final String accessToken = e.getValue().get(ACCESS_TOKEN_KEY);
                    if (accessToken != null) {
                        final User user = users.get(e.getKey());
                        if (user != null) {
                            usersByAccessToken.put(accessToken, user);
                        } else {
                            logger.warning("Couldn't find user \""+e.getKey()+"\" for which an access token was found in the preferences");
                        }
                    }
                }
            }
        }
    }
    
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        preferenceConverters = new ConcurrentHashMap<>();
        preferenceObjects = new ConcurrentHashMap<>();
        listeners = new HashMap<>();
        listenersLock = new NamedReentrantReadWriteLock(
                UserStoreImpl.class.getSimpleName() + " lock for listeners collection", false);
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    @Override
    public void clear() {
        tenants.clear();
        tenantsByName.clear();
        userGroups.clear();
        userGroupsByName.clear();
        LockUtil.lockForWrite(userGroupsUserCacheLock);
        try {
            userGroupsContainingUser.clear();
            usersInUserGroups.clear();
        } finally {
            LockUtil.unlockAfterWrite(userGroupsUserCacheLock);
        }
        clearAllPreferenceObjects();
        emailForUsername.clear();
        settings.clear();
        settingTypes.clear();
        users.clear();
        roles.clear();
        usersByEmail.clear();
        usersByAccessToken.clear();
    }

    /**
     * Preference objects can't be simply removed by clearing {@link #preferenceObjects} because listeners can have a
     * state depending on the current preference objects. So we need to notify all listeners about the removal of the
     * notification objects.
     */
    private void clearAllPreferenceObjects() {
        final Set<String> usersToProcess = new HashSet<>(preferences.keySet());
        for (String username : usersToProcess) {
            removeAllPreferencesForUser(username);
        }
    }

    @Override
    public void replaceContentsFrom(UserStore newUserStore) {
        clear();
        for (Tenant tenant : newUserStore.getTenants()) {
            tenants.put(tenant.getId(), tenant);
            tenantsByName.put(tenant.getName(), tenant);
        }
        LockUtil.lockForWrite(userGroupsUserCacheLock);
        try {
            for (UserGroup group : newUserStore.getUserGroups()) {
                userGroups.put(group.getId(), group);
                userGroupsByName.put(group.getName(), group);
                final HashSet<SecurityUser> usersInGroup = new HashSet<>();
                Util.addAll(group.getUsers(), usersInGroup);
                usersInUserGroups.put(group, usersInGroup);
                for (final SecurityUser userInGroup : group.getUsers()) {
                    Util.addToValueSet(userGroupsContainingUser, userInGroup, group);
                }
            }
        } finally {
            LockUtil.unlockAfterWrite(userGroupsUserCacheLock);
        }
        for (Role role : newUserStore.getRoles()) {
            roles.put(role.getId(), role);
        }
        for (UserImpl user : newUserStore.getUsers()) {
            users.put(user.getName(), user);
            addToUsersByEmail(user);
            for (Entry<String, String> userPref : newUserStore.getAllPreferences(user.getName()).entrySet()) {
                setPreference(user.getName(), userPref.getKey(), userPref.getValue());
                if (userPref.getKey().equals(ACCESS_TOKEN_KEY)) {
                    usersByAccessToken.put(userPref.getValue(), user);
                }
            }
        }
        for (Entry<String, Object> setting : newUserStore.getAllSettings().entrySet()) {
            settings.put(setting.getKey(), setting.getValue());
        }
        for (Entry<String, Class<?>> settingType : newUserStore.getAllSettingTypes().entrySet()) {
            settingTypes.put(settingType.getKey(), settingType.getValue());
        }
    }

    @Override
    public Iterable<Role> getRoles() {
        return new ArrayList<>(roles.values());
    }

    @Override
    public Role getRole(UUID roleId) {
        return roles.get(roleId);
    }

    @Override
    public Role createRole(UUID roleId, String displayName, Iterable<WildcardPermission> permissions) {
        Role role = new RoleImpl(roleId, displayName, permissions);
        roles.put(roleId, role);
        mongoObjectFactory.storeRole(role);
        return role;
    }

    @Override
    public void setRolePermissions(UUID roleId, Set<WildcardPermission> permissions) {
        Role role = roles.get(roleId);
        role = new RoleImpl(roleId, role.getName(), permissions);
        mongoObjectFactory.storeRole(role);
    }

    @Override
    public void addRolePermission(UUID roleId, WildcardPermission permission) {
        Role role = roles.get(roleId);
        Set<WildcardPermission> permissions = role.getPermissions();
        permissions.add(permission);
        role = new RoleImpl(roleId, role.getName(), permissions);
        mongoObjectFactory.storeRole(role);
    }

    @Override
    public void removeRolePermission(UUID roleId, WildcardPermission permission) {
        Role role = roles.get(roleId);
        Set<WildcardPermission> permissions = role.getPermissions();
        permissions.remove(permission);
        role = new RoleImpl(roleId, role.getName(), permissions);
        mongoObjectFactory.storeRole(role);
    }

    @Override
    public void setRoleDisplayName(UUID roleId, String displayName) {
        Role role = roles.get(roleId);
        role = new RoleImpl(roleId, displayName, role.getPermissions());
        mongoObjectFactory.storeRole(role);
    }

    @Override
    public void removeRole(Role role) {
        mongoObjectFactory.deleteRole(role);
        roles.remove(role.getId());
    }

    @Override
    public boolean setAccessToken(String username, String accessToken) {
        final boolean result;
        final UserImpl user = getUserByName(username);
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
    }

    @Override
    public String getAccessToken(String username) {
        // TODO replace check for admin role by a check for the read permission for the access token which then has to be implied for the user by the user role
        // only the user or an administrator may request a user's access token
        final Object principal = SecurityUtils.getSubject().getPrincipal();
        if (SecurityUtils.getSubject().hasRole(AdminRole.getInstance().getName()) ||
            (principal != null && principal.toString().equals(username))) {
            return getPreference(username, ACCESS_TOKEN_KEY);
        } else {
            throw new org.apache.shiro.authz.AuthorizationException("Only admin role or owner can retrieve access token");
        }
    }

    @Override
    public void removeAccessToken(String username) {
        // TODO replace check for admin role by a check for the read permission for the access token which then has to be implied for the user by the user role
        // only the user or an administrator may request a user's access token
        if (SecurityUtils.getSubject().hasRole(AdminRole.getInstance().getName()) ||
            SecurityUtils.getSubject().getPrincipal().toString().equals(username)) {
            SecurityUser user = users.get(username);
            if (user != null) {
                final String accessToken = getPreference(username, ACCESS_TOKEN_KEY);
                if (accessToken != null) {
                    usersByAccessToken.remove(accessToken);
                }
                // the access token actually existed; now we need to update the preferences
                unsetPreference(username, ACCESS_TOKEN_KEY);
            }
        } else {
            throw new org.apache.shiro.authz.AuthorizationException("Only admin role or owner can retrieve access token");
        }
    }

    private void addToUsersByEmail(UserImpl u) {
        if (u.getEmail() != null && !u.getEmail().isEmpty()) {
            Set<UserImpl> set = usersByEmail.get(u.getEmail());
            if (set == null) {
                set = new HashSet<>();
                usersByEmail.put(u.getEmail(), set);
            }
            set.add(u);
            emailForUsername.put(u.getName(), u.getEmail());
        }
    }

    private void removeFromUsersByEmail(UserImpl u) {
        if (u != null) {
            final String email = emailForUsername.remove(u.getName());
            if (email != null) {
                Set<UserImpl> set = usersByEmail.get(email); // this also works if the user's e-mail has changed meanwhile
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
        return name;
    }
    
    @Override
    public Iterable<UserGroup> getUserGroups() {
        return new ArrayList<>(userGroups.values());
    }
    
    @Override
    public UserGroup getUserGroupByName(String name) {
        return userGroupsByName.get(name);
    }

    @Override
    public UserGroup getUserGroup(UUID id) {
        return userGroups.get(id);
    }
    
    @Override
    public UserGroup createUserGroup(UUID groupId, String name) throws UserGroupManagementException {
        if (userGroups.contains(groupId)) {
            throw new UserGroupManagementException(UserGroupManagementException.USER_GROUP_ALREADY_EXISTS);
        }
        logger.info("Creating user group: " + groupId + " with name "+name);
        UserGroup group = new UserGroupImpl(groupId, name);
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storeUserGroup(group);
        }
        userGroups.put(groupId, group);
        userGroupsByName.put(name, group);
        return group;
    }

    @Override
    public void updateUserGroup(UserGroup group) {
        logger.info("Updating user group " + group.getName() + " in DB");
        LockUtil.lockForWrite(userGroupsUserCacheLock);
        try {
            Set<SecurityUser> usersInGroupBefore = new HashSet<>();
            Util.addAll(usersInUserGroups.get(group), usersInGroupBefore);
            for (final SecurityUser userNowInUpdatedGroup : group.getUsers()) {
                if (usersInGroupBefore == null || !Util.contains(usersInGroupBefore, userNowInUpdatedGroup)) {
                    // the user was added:
                    Util.addToValueSet(usersInUserGroups, group, userNowInUpdatedGroup);
                    Util.addToValueSet(userGroupsContainingUser, userNowInUpdatedGroup, group);
                }
            }
            for (final SecurityUser userInGroupBefore : usersInGroupBefore) {
                if (!Util.contains(group.getUsers(), userInGroupBefore)) {
                    // the user was removed
                    Util.removeFromValueSet(usersInUserGroups, group, userInGroupBefore);
                    Util.removeFromValueSet(userGroupsContainingUser, userInGroupBefore, group);
                }
            }
        } finally {
            LockUtil.unlockAfterWrite(userGroupsUserCacheLock);
        }
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storeUserGroup(group);
        }
    }

    @Override
    public Iterable<UserGroup> getUserGroupsOfUser(SecurityUser user) {
        final Iterable<UserGroup> preResult;
        LockUtil.lockForRead(userGroupsUserCacheLock);
        try {
            preResult = userGroupsContainingUser.get(user);
        } finally {
            LockUtil.unlockAfterRead(userGroupsUserCacheLock);
        }
        return preResult == null ? Collections.<UserGroup>emptySet() : preResult;
    }

    @Override
    public void deleteUserGroup(UserGroup userGroup) throws UserGroupManagementException {
        if (!userGroups.containsKey(userGroup.getId())) {
            throw new UserGroupManagementException(UserGroupManagementException.USER_GROUP_DOES_NOT_EXIST);
        }
        logger.info("Deleting user group: " + userGroup);
        userGroupsByName.remove(userGroup.getName());
        userGroups.remove(userGroup.getId());
        LockUtil.lockForWrite(userGroupsUserCacheLock);
        try {
            for (final SecurityUser userInDeletedGroup : userGroup.getUsers()) {
                Util.removeFromValueSet(userGroupsContainingUser, userInDeletedGroup, userGroup);
            }
            usersInUserGroups.remove(userGroup);
        } finally {
            LockUtil.unlockAfterWrite(userGroupsUserCacheLock);
        }
        deleteUserGroupFromDB(userGroup);
    }

    private void deleteUserGroupFromDB(UserGroup userGroup) {
        if (mongoObjectFactory != null) {
            mongoObjectFactory.deleteUserGroup(userGroup);
        }
    }
    
    @Override
    public Iterable<Tenant> getTenants() {
        return new HashSet<>(tenants.values());
    }

    @Override
    public Tenant getTenantByName(String name) {
        return name == null ? null : tenantsByName.get(name);
    }
    
    @Override
    public Tenant getTenant(UUID tenantId) {
        return tenantId == null ? null : tenants.get(tenantId);
    }

    @Override
    public Tenant createTenant(UUID tenantId, String name) throws TenantManagementException, UserGroupManagementException {
        if (tenants.containsKey(tenantId)) {
            throw new TenantManagementException(TenantManagementException.TENANT_ALREADY_EXISTS);
        }
        logger.info("Creating tenant: " + tenantId);
        Tenant tenant = new TenantImpl(tenantId, name);
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storeTenant(tenant);
        }
        updateUserGroup(tenant);
        tenants.put(tenantId, tenant);
        tenantsByName.put(tenant.getName(), tenant);
        return tenant;
    }

    @Override
    public void updateTenant(Tenant tenant) {
        updateUserGroup(tenant);
        logger.info("Updating tenant " + tenant.getId() + " in DB");
    }

    private void deleteTenantId(Tenant tenant) throws TenantManagementException {
        if (!tenants.contains(tenant)) {
            throw new TenantManagementException(TenantManagementException.TENANT_DOES_NOT_EXIST);
        }
        logger.info("Deleting tenant: " + tenant);
        tenants.remove(tenant.getId());
        tenantsByName.remove(tenant.getName());
        if (mongoObjectFactory != null) {
            mongoObjectFactory.deleteTenant(tenant);
        }
    }
    
    @Override
    public void deleteTenant(Tenant tenant) throws TenantManagementException, UserGroupManagementException {
        deleteTenantId(tenant);
        if (mongoObjectFactory != null) {
            mongoObjectFactory.deleteUserGroup(tenant);
        }
    }

    @Override
    public UserImpl createUser(String name, String email, Tenant defaultTenant, Account... accounts) throws UserManagementException {
        if (getUserByName(name) != null) {
            throw new UserManagementException(UserManagementException.USER_ALREADY_EXISTS);
        }
        UserImpl user = new UserImpl(name, email, defaultTenant, accounts);
        logger.info("Creating user: " + user + " with e-mail "+email);
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storeUser(user);
        }
        users.put(name, user);
        addToUsersByEmail(user);
        return user;
    }

    @Override
    public void updateUser(UserImpl user) {
        logger.info("Updating user "+user+" in DB");
        users.put(user.getName(), user);
        removeFromUsersByEmail(user);
        addToUsersByEmail(user);
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storeUser(user);
        }
    }

    @Override
    public Iterable<UserImpl> getUsers() {
        return new ArrayList<UserImpl>(users.values());
    }

    @Override
    public boolean hasUsers() {
        return !users.isEmpty();
    }

    @Override
    public UserImpl getUserByName(String name) {
        final UserImpl result;
        if (name == null) {
            result = null;
        } else {
            result = users.get(name);
        }
        return result;
    }

    @Override
    public User getUserByAccessToken(String accessToken) {
        final User result;
        if (accessToken == null) {
            result = null;
        } else {
            result = usersByAccessToken.get(accessToken);
        }
        return result;
    }

    @Override
    public UserImpl getUserByEmail(String email) {
        final UserImpl result;
        if (email == null) {
            result = null;
        } else {
            Set<UserImpl> set = usersByEmail.get(email);
            if (set == null || set.isEmpty()) {
                result = null;
            } else {
                result = set.iterator().next();
            }
        }
        return result;
    }

    @Override
    public Iterable<Role> getRolesFromUser(String username) throws UserManagementException {
        if (users.get(username) == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        return users.get(username).getRoles();
    }

    @Override
    public void addRoleForUser(String name, Role role) throws UserManagementException {
        final UserImpl user = users.get(name);
        if (user == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        user.addRole(role);
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storeUser(user);
        }
    }

    @Override
    public void removeRoleFromUser(String name, Role role) throws UserManagementException {
        if (users.get(name) == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        users.get(name).removeRole(role);
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storeUser(users.get(name));
        }
    }

    @Override
    public Iterable<WildcardPermission> getPermissionsFromUser(String username) throws UserManagementException {
        if (users.get(username) == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        return users.get(username).getPermissions();
    }

    @Override
    public void addPermissionForUser(String username, WildcardPermission permission) throws UserManagementException {
        final UserImpl user = users.get(username);
        if (user == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        user.addPermission(permission);
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storeUser(user);
        }
    }

    @Override
    public void removePermissionFromUser(String name, WildcardPermission permission) throws UserManagementException {
        if (users.get(name) == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        users.get(name).removePermission(permission);
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storeUser(users.get(name));
        }
    }

    @Override
    public void deleteUser(String name) throws UserManagementException {
        if (users.get(name) == null) {
            throw new UserManagementException(UserManagementException.USER_DOES_NOT_EXIST);
        }
        logger.info("Deleting user: " + users.get(name).toString());
        if (mongoObjectFactory != null) {
            mongoObjectFactory.deleteUser(users.get(name));
        }
        removeFromUsersByEmail(users.remove(name));
        removeAllPreferencesForUser(name);
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
        setPreferenceInternal(username, key, value);
        updatePreferenceObjectIfConverterIsAvailable(username, key);
    }

    private void setPreferenceInternal(String username, String key, String value) {
        Map<String, String> userMap = preferences.get(username);
        if (userMap == null) {
            synchronized (preferences) {
                // only synchronize when necessary
                userMap = preferences.get(username);
                if (userMap == null) {
                    userMap = new ConcurrentHashMap<>();
                    preferences.put(username, userMap);
                }
            }
        }
        if(value == null) {
            userMap.remove(key);
        } else {
            userMap.put(key, value);
        }
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storePreferences(username, userMap);
        }
    }

    @Override
    public void unsetPreference(String username, String key) {
        Map<String, String> userMap = preferences.get(username);
        if (userMap != null) {
            userMap.remove(key);
            if (mongoObjectFactory != null) {
                mongoObjectFactory.storePreferences(username, userMap);
            }
        }
        unsetPreferenceObject(username, key);
    }

    @Override
    public String getPreference(String username, String key) {
        final String result;
        Map<String, String> userMap = preferences.get(username);
        if (userMap != null) {
            result = userMap.get(key);
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Map<String, String> getAllPreferences(String username) {
        final Map<String, String> userPrefs = preferences.get(username);
        final Map<String, String> result;
        if (userPrefs == null) {
            result = Collections.emptyMap();
        } else {
            result = Collections.unmodifiableMap(userPrefs);
        }
        return result;
    }

    private void removeAllPreferencesForUser(String username) {
        // TODO should we keep the preferences anonymized (e.g. use a UUID as username) to enable better statistics?
        synchronized (preferences) {
            preferences.remove(username);
        }
        if (mongoObjectFactory != null) {
            mongoObjectFactory.storePreferences(username, Collections.<String, String>emptyMap());
        }
        removeAllPreferenceObjectsForUser(username);
    }

    private void removeAllPreferenceObjectsForUser(String username) {
        Map<String, Object> preferenceObjectsToRemove;
        synchronized (preferenceObjects) {
            preferenceObjectsToRemove = preferenceObjects.remove(username);
        }
        if(preferenceObjectsToRemove != null) {
            for(Map.Entry<String, Object> entry: preferenceObjectsToRemove.entrySet()) {
                notifyListenersOnPreferenceObjectChange(username, entry.getKey(), entry.getValue(), null);
            }
        }
    }

    @Override
    public Map<String, Object> getAllSettings() {
        return settings;
    }

    @Override
    public Map<String, Class<?>> getAllSettingTypes() {
        return settingTypes;
    }
    
    @Override
    public void registerPreferenceConverter(String preferenceKey, PreferenceConverter<?> converter) {
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
    }
    
    @Override
    public void removePreferenceConverter(String preferenceKey) {
        PreferenceConverter<?> preferenceConverterToRemove = preferenceConverters.remove(preferenceKey);
        if (preferenceConverterToRemove != null) {
            final Set<String> usersToProcess = new HashSet<>(preferences.keySet());
            for (String username : usersToProcess) {
                unsetPreferenceObject(username, preferenceKey);
            }
        } else {
            logger.log(Level.WARNING, "PreferenceConverter for key " + preferenceKey
                    + " should be removed but wasn't registered");
        }
        
    }

    private void updatePreferenceObjectIfConverterIsAvailable(String username, String key) {
        PreferenceConverter<?> preferenceConverter = preferenceConverters.get(key);
        if (preferenceConverter != null) {
            updatePreferenceObjectWithConverter(username, key, preferenceConverter);
        }
    }

    private void updatePreferenceObjectWithConverter(String username, String key, PreferenceConverter<?> preferenceConverter) {
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
        Map<String, Object> userMap = preferenceObjects.get(username);
        if (userMap == null) {
            synchronized (preferenceObjects) {
                // only synchronize when necessary
                userMap = preferenceObjects.get(username);
                if (userMap == null) {
                    userMap = new ConcurrentHashMap<>();
                    preferenceObjects.put(username, userMap);
                }
            }
        }
        // if the new preference object is simply null, we remove the entry instead of putting null
        Object oldPreference = convertedObject == null ? userMap.remove(key) : userMap.put(key, convertedObject);
        if (oldPreference != null || convertedObject != null) {
            // preference hasn't changed if it was null and is now null
            notifyListenersOnPreferenceObjectChange(username, key, oldPreference, convertedObject);
        }
    }

    private void unsetPreferenceObject(String username, String key) {
        Map<String, Object> userObjectMap = preferenceObjects.get(username);
        if (userObjectMap != null) {
            Object oldPreference = userObjectMap.remove(key);
            if(oldPreference != null) {
                notifyListenersOnPreferenceObjectChange(username, key, oldPreference, null);
            }
        }
    }

    @Override
    public <T> T getPreferenceObject(String username, String key) {
        final Object result;
        Map<String, Object> userMap = preferenceObjects.get(username);
        if (userMap != null) {
            result = userMap.get(key);
        } else {
            result = null;
        }
        @SuppressWarnings("unchecked")
        T resultT = (T) result;
        return resultT;
    }
    
    @Override
    public String setPreferenceObject(String username, String key, Object preferenceObject)
            throws IllegalArgumentException {
        @SuppressWarnings("unchecked")
        PreferenceConverter<Object> preferenceConverter = (PreferenceConverter<Object>) preferenceConverters.get(key);
        if (preferenceConverter == null) {
            throw new IllegalArgumentException("Setting preference for key "+key+" but there is no converter associated!");
        }
        String stringPreference = null;
        if (preferenceObject == null) {
            unsetPreference(username, key);
        } else {
            try {
                stringPreference = preferenceConverter.toPreferenceString(preferenceObject);
                setPreferenceInternal(username, key, stringPreference);
                setPreferenceObjectInternal(username, key, preferenceObject);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Error while converting preference for key " + key + " from Object \""
                        + preferenceObject + "\"", t);
            }
        }
        return stringPreference;
    }

    private void notifyListenersOnPreferenceObjectChange(String username, String key, Object oldPreference,
            Object newPreference) {
        LockUtil.lockForRead(listenersLock);
        try {
            for (PreferenceObjectListener<? extends Object> listener : Util.get(listeners, key,
                    Collections.<PreferenceObjectListener<? extends Object>> emptySet())) {
                @SuppressWarnings("unchecked")
                PreferenceObjectListener<Object> listenerToFire = (PreferenceObjectListener<Object>) listener;
                listenerToFire.preferenceObjectChanged(username, key, oldPreference, newPreference);
            }
        } finally {
            LockUtil.unlockAfterRead(listenersLock);
        }
    }

    @Override
    public void addPreferenceObjectListener(String key, PreferenceObjectListener<? extends Object> listener,
            boolean fireForAlreadyExistingPreferences) {
        LockUtil.lockForWrite(listenersLock);
        try {
            Util.addToValueSet(listeners, key, listener);
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
        } finally {
            LockUtil.unlockAfterWrite(listenersLock);
        }
    }

    @Override
    public void removePreferenceObjectListener(PreferenceObjectListener<?> listener) {
        LockUtil.lockForWrite(listenersLock);
        try {
            Util.removeFromAllValueSets(listeners, listener);
        } finally {
            LockUtil.unlockAfterWrite(listenersLock);
        }
    }
}
