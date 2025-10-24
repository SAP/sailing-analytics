package com.sap.sse.security.userstore.mongodb.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.types.Binary;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.security.interfaces.Social;
import com.sap.sse.security.interfaces.UserImpl;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.Account;
import com.sap.sse.security.shared.Account.AccountType;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.RoleDefinitionImpl;
import com.sap.sse.security.shared.SocialUserAccount;
import com.sap.sse.security.shared.UserGroupProvider;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.UsernamePasswordAccount;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.LockingAndBanning;
import com.sap.sse.security.shared.impl.LockingAndBanningImpl;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.QualifiedObjectIdentifierImpl;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.impl.UserGroupImpl;
import com.sap.sse.security.shared.subscription.Subscription;
import com.sap.sse.security.subscription.SubscriptionData;
import com.sap.sse.security.subscription.SubscriptionDataHandler;
import com.sap.sse.security.userstore.mongodb.DomainObjectFactory;
import com.sap.sse.security.userstore.mongodb.impl.FieldNames.Tenant;

public class DomainObjectFactoryImpl implements DomainObjectFactory {
    private static final Logger logger = Logger.getLogger(DomainObjectFactoryImpl.class.getName());

    private final MongoDatabase db;

    public DomainObjectFactoryImpl(MongoDatabase db) {
        this.db = db;
    }

    @Override
    public Iterable<AccessControlListAnnotation> loadAllAccessControlLists(UserStore userStore) {
        ArrayList<AccessControlListAnnotation> result = new ArrayList<>();
        MongoCollection<org.bson.Document> aclCollection = db
                .getCollection(CollectionNames.ACCESS_CONTROL_LISTS.name());
        try {
            for (Document o : aclCollection.find()) {
                result.add(loadAccessControlList(o, userStore));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load ACLs.");
            logger.log(Level.SEVERE, "loadAllAccessControlLists", e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private AccessControlListAnnotation loadAccessControlList(Document aclDBObject, UserStore userStore) {
        final QualifiedObjectIdentifier id = QualifiedObjectIdentifierImpl
                .fromDBWithoutEscaping((String) aclDBObject.get(FieldNames.AccessControlList.OBJECT_ID.name()));
        final String displayName = (String) aclDBObject.get(FieldNames.AccessControlList.OBJECT_DISPLAY_NAME.name());
        List<Object> dbPermissionMap = ((List<Object>) aclDBObject
                .get(FieldNames.AccessControlList.PERMISSION_MAP.name()));
        Map<UserGroup, Set<String>> permissionMap = new HashMap<>();
        for (Object dbPermissionMapEntryO : dbPermissionMap) {
            Document dbPermissionMapEntry = (Document) dbPermissionMapEntryO;
            final UUID userGroupKey = (UUID) dbPermissionMapEntry
                    .get(FieldNames.AccessControlList.PERMISSION_MAP_USER_GROUP_ID.name());
            final UserGroup userGroup = userStore.getUserGroup(userGroupKey);
            Set<String> actions = new HashSet<>();
            for (Object o : (List<Object>) dbPermissionMapEntry
                    .get(FieldNames.AccessControlList.PERMISSION_MAP_ACTIONS.name())) {
                actions.add(o.toString());
            }
            permissionMap.put(userGroup, actions);
        }
        AccessControlListAnnotation result = new AccessControlListAnnotation(new AccessControlList(permissionMap), id,
                displayName);
        return result;
    }

    @Override
    public Iterable<OwnershipAnnotation> loadAllOwnerships(UserStore userStore) {
        ArrayList<OwnershipAnnotation> result = new ArrayList<>();
        MongoCollection<org.bson.Document> ownershipCollection = db.getCollection(CollectionNames.OWNERSHIPS.name());
        try {
            for (Document o : ownershipCollection.find()) {
                result.add(loadOwnership(o, userStore));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load ownerships.");
            logger.log(Level.SEVERE, "loadAllOwnerships", e);
        }
        return result;
    }

    private OwnershipAnnotation loadOwnership(Document ownershipDBObject, UserStore userStore) {
        String escapedId = (String) ownershipDBObject.get(FieldNames.Ownership.OBJECT_ID.name());
        final QualifiedObjectIdentifier idOfOwnedObject = QualifiedObjectIdentifierImpl
                .fromDBWithoutEscaping(escapedId);
        final String displayNameOfOwnedObject = (String) ownershipDBObject
                .get(FieldNames.Ownership.OBJECT_DISPLAY_NAME.name());
        final String userOwnerName = (String) ownershipDBObject.get(FieldNames.Ownership.OWNER_USERNAME.name());
        final UUID tenantOwnerId = (UUID) ownershipDBObject.get(FieldNames.Ownership.TENANT_OWNER_ID.name());
        final User userOwner = userStore.getUserByName(userOwnerName);
        final UserGroup tenantOwner = userStore.getUserGroup(tenantOwnerId);
        return new OwnershipAnnotation(new Ownership(userOwner, tenantOwner), idOfOwnedObject,
                displayNameOfOwnedObject);
    }

    @Override
    public Iterable<RoleDefinition> loadAllRoleDefinitions() {
        ArrayList<RoleDefinition> result = new ArrayList<>();
        MongoCollection<org.bson.Document> roleCollection = db.getCollection(CollectionNames.ROLES.name());
        try {
            for (Document o : roleCollection.find()) {
                result.add(loadRoleDefinition(o));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load role definitions.");
            logger.log(Level.SEVERE, "loadAllRoleDefinitions", e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private RoleDefinition loadRoleDefinition(Document roleDefinitionDBObject) {
        final String id = (String) roleDefinitionDBObject.get(FieldNames.Role.ID.name());
        final String displayName = (String) roleDefinitionDBObject.get(FieldNames.Role.NAME.name());
        final Set<WildcardPermission> permissions = new HashSet<>();
        for (Object o : (List<Object>) (roleDefinitionDBObject.get(FieldNames.Role.PERMISSIONS.name()))) {
            permissions.add(new WildcardPermission(o.toString()));
        }
        return new RoleDefinitionImpl(UUID.fromString(id), displayName, permissions);
    }

    @Override
    public Iterable<UserGroup> loadAllUserGroupsAndTenantsWithProxyUsers(
            Map<UUID, RoleDefinition> roleDefinitionsById) {
        Set<UserGroup> userGroups = new HashSet<>();
        MongoCollection<org.bson.Document> userGroupCollection = db.getCollection(CollectionNames.USER_GROUPS.name());
        try {
            for (Document o : userGroupCollection.find()) {
                final UserGroup userGroup = loadUserGroupWithProxyUsers(o, roleDefinitionsById);
                userGroups.add(userGroup);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load user groups.");
            logger.log(Level.SEVERE, "loadAllUserGroups", e);
        }
        return userGroups;
    }

    private UserGroup loadUserGroupWithProxyUsers(Document groupDBObject,
            Map<UUID, RoleDefinition> roleDefinitionsById) {
        final UUID id = (UUID) groupDBObject.get(FieldNames.UserGroup.ID.name());
        final String name = (String) groupDBObject.get(FieldNames.UserGroup.NAME.name());
        Set<User> users = new HashSet<>();
        @SuppressWarnings("unchecked")
        List<Object> usersO = (List<Object>) groupDBObject.get(FieldNames.UserGroup.USERNAMES.name());
        if (usersO != null) {
            for (Object o : usersO) {
                users.add(new UserProxy((String) o));
            }
        }
        final Map<RoleDefinition, Boolean> roleDefinitionMap = new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Object> dbRoleDefinitionMap = (List<Object>) groupDBObject
                .get(FieldNames.UserGroup.ROLE_DEFINITION_MAP.name());
        if (dbRoleDefinitionMap != null) {
            for (Object roleDefO : dbRoleDefinitionMap) {
                final Document roleDefEntry = (Document) roleDefO;
                final UUID roleDefId = roleDefEntry.get(FieldNames.UserGroup.ROLE_DEFINITION_MAP_ROLE_ID.name(),
                        UUID.class);
                final Boolean forAll = roleDefEntry.getBoolean(FieldNames.UserGroup.ROLE_DEFINITION_MAP_FOR_ALL.name());
                final RoleDefinition roleDefinition = roleDefinitionsById.get(roleDefId);
                if (roleDefinition != null) {
                    roleDefinitionMap.put(roleDefinition, forAll);
                } else {
                    logger.severe("RoleDefinition with ID "+roleDefId+" which the user group "+name+" with ID "+
                            id+" grants cannot be found");
                }
            }
        }
        return new UserGroupImpl(id, name, users, roleDefinitionMap);
    }

    /**
     * @param defaultTenantForRoleMigration
     *            when a string-based role is found on the user object it will be mapped to a {@link Role} object
     *            pointing to an equal-named {@link RoleDefinition} from the {@code roleDefinitionsById} map, with a
     *            {@link Role#getQualifiedForTenant() tenant qualification} as defined by this parameter; if this
     *            parameter is {@code null}, role migration will throw an exception.
     * @param userGroups
     *            the user groups to resolve tenant IDs against for users' default tenants as well as role tenant
     *            qualifiers
     * @return the user objects returned have a fully resolved default tenant as well as fully-resolved role tenant/user
     *         qualifiers; the {@link Tenant} objects passed in the {@code tenants} map may still have an empty user
     *         group that is filled later.
     */
    @Override
    public Iterable<User> loadAllUsers(Map<UUID, RoleDefinition> roleDefinitionsById,
            RoleMigrationConverter roleMigrationConverter, Map<UUID, UserGroup> userGroups,
            UserGroupProvider userGroupProvider) throws UserManagementException {
        Map<String, User> result = new HashMap<>();
        MongoCollection<org.bson.Document> userCollection = db.getCollection(CollectionNames.USERS.name());
        try {
            for (Document o : userCollection.find()) {
                User userWithProxyRoleUserQualifier = loadUserWithProxyRoleUserQualifiers(o, roleDefinitionsById,
                        roleMigrationConverter, userGroups, userGroupProvider);
                result.put(userWithProxyRoleUserQualifier.getName(), userWithProxyRoleUserQualifier);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load users.");
            logger.log(Level.SEVERE, "loadAllUsers", e);
        }
        resolveRoleUserQualifiers(result);
        return result.values();
    }

    private void resolveRoleUserQualifiers(Map<String, User> users) throws UserManagementException {
        for (final User user : users.values()) {
            final Set<Role> userRoles = new HashSet<>();
            Util.addAll(user.getRoles(), userRoles); // avoid concurrent modification exception
            for (final Role roleWithUserQualifierProxy : userRoles) {
                final User userQualifierProxy = roleWithUserQualifierProxy.getQualifiedForUser();
                if (userQualifierProxy != null) {
                    final User resolvedUserQualifier = users.get(userQualifierProxy.getName());
                    user.removeRole(roleWithUserQualifierProxy);
                    if (resolvedUserQualifier == null) {
                        logger.severe("Unable to resolve user named "+userQualifierProxy.getName()+
                                " which serves as a role qualifier for role "+roleWithUserQualifierProxy.getName()+
                                " for user "+user.getName()+". Removing role.");
                    } else {
                        user.addRole(new Role(roleWithUserQualifierProxy.getRoleDefinition(),
                                roleWithUserQualifierProxy.getQualifiedForTenant(), resolvedUserQualifier,
                                roleWithUserQualifierProxy.isTransitive()));
                    }
                }
            }
        }
    }

    /**
     * @param defaultTenantForRoleMigration
     *            when a string-based role is found on the user object it will be mapped to a {@link Role} object
     *            pointing to an equal-named {@link RoleDefinition} from the {@code roleDefinitionsById} map, with a
     *            {@link Role#getQualifiedForTenant() tenant qualification} as defined by this parameter; if this
     *            parameter is {@code null}, role migration will throw an exception.
     * @param tenants
     *            the tenants to resolve tenant IDs against for users' default tenants as well as role tenant qualifiers
     * @return the user objects returned have dummy objects for their {@link UserImpl#getRoles() roles'}
     *         {@link Role#getQualifiedForUser() user qualifier} where only the username is set properly to identify the
     *         user in the calling method where ultimately all users will be known.
     */
    private User loadUserWithProxyRoleUserQualifiers(Document userDBObject,
            Map<UUID, RoleDefinition> roleDefinitionsById, RoleMigrationConverter roleMigrationConverter,
            Map<UUID, UserGroup> tenants, UserGroupProvider userGroupProvider) {
        final String username = (String) userDBObject.get(FieldNames.User.NAME.name());
        final String email = (String) userDBObject.get(FieldNames.User.EMAIL.name());
        final String fullName = (String) userDBObject.get(FieldNames.User.FULLNAME.name());
        final String company = (String) userDBObject.get(FieldNames.User.COMPANY.name());
        final String localeRaw = (String) userDBObject.get(FieldNames.User.LOCALE.name());
        final Locale locale = localeRaw != null ? Locale.forLanguageTag(localeRaw) : null;
        final Boolean emailValidated = (Boolean) userDBObject.get(FieldNames.User.EMAIL_VALIDATED.name());
        final String passwordResetSecret = (String) userDBObject.get(FieldNames.User.PASSWORD_RESET_SECRET.name());
        final String validationSecret = (String) userDBObject.get(FieldNames.User.VALIDATION_SECRET.name());
        final Long lockedUntilMillis = userDBObject.getLong(FieldNames.User.LOCKED_UNTIL_MILLIS.name());
        final Long nextLockingDurationMillis = userDBObject.getLong(FieldNames.User.NEXT_LOCKING_DURATION_MILLIS.name());
        final LockingAndBanning lockingAndBanning = new LockingAndBanningImpl(
                lockedUntilMillis == null ? TimePoint.BeginningOfTime : TimePoint.of(lockedUntilMillis),
                nextLockingDurationMillis == null ? LockingAndBanningImpl.DEFAULT_INITIAL_LOCKING_DELAY : Duration.ofMillis(nextLockingDurationMillis));
        final Set<Role> roles = new HashSet<>();
        final Set<String> permissions = new HashSet<>();
        final List<?> rolesO = (List<?>) userDBObject.get(FieldNames.User.ROLE_IDS.name());
        boolean rolesMigrated = false; // if a role needs migration, user needs an update in the DB
        if (rolesO != null) {
            for (Object o : rolesO) {
                final Role role = loadRoleWithProxyUserQualifier((Document) o, roleDefinitionsById, tenants);
                if (role != null) {
                    roles.add(role);
                } else {
                    logger.warning(
                            "Role with ID " + o + " that used to be assigned to user " + username + " not found");
                }
            }
        } else {
            // migration of old name-based, non-entity roles:
            // try to find an equal-named role in the set of role definitions and create a role
            // that is qualified by the default tenant; for this a default tenant must exist because
            // otherwise a user would obtain global rights by means of migration which must not happen.
            logger.info("Migrating roles of user " + username);
            List<?> roleNames = (List<?>) userDBObject.get("ROLES");
            if (roleNames != null) {
                logger.info("Found old roles " + roleNames + " for user " + username);
                for (Object o : roleNames) {
                    final Role convertedRole = roleMigrationConverter.convert(o.toString(), username);
                    if (convertedRole != null) {
                        logger.info("Found role " + convertedRole.getRoleDefinition() + " for old role " + o.toString()
                                + " for user " + username);
                        // we do not do role associations, to stay similar as before, meaning that all admins can
                        // edit the roles. Without this we would need to determine which admin (if
                        // multiple present) should own this association.
                        roles.add(convertedRole);
                        rolesMigrated = true;
                    } else {
                        logger.warning("Role " + o.toString() + " for user " + username
                                + " not found during migration. User will no longer be in this role.");
                    }
                }
            }
        }
        Iterable<?> permissionsO = (Iterable<?>) userDBObject.get(FieldNames.User.PERMISSIONS.name());
        if (permissionsO != null) {
            for (Object o : permissionsO) {
                permissions.add((String) o);
            }
        }
        final Map<String, UserGroup> defaultTenant = new ConcurrentHashMap<>();
        final List<?> defaultTenantIds = (List<?>) userDBObject.get(FieldNames.User.DEFAULT_TENANT_IDS.name());
        if (defaultTenantIds != null) {
            for (Object singleDefaultTenant : defaultTenantIds) {
                Document singleDefaultTenantObj = (Document) singleDefaultTenant;
                String serverName = singleDefaultTenantObj.getString(FieldNames.User.DEFAULT_TENANT_SERVER.name());
                UUID groupId = (UUID) singleDefaultTenantObj.get(FieldNames.User.DEFAULT_TENANT_GROUP.name());
                UserGroup tenantOfGroup = tenants.get(groupId);
                if (tenantOfGroup == null) {
                    logger.warning("Couldn't find tenant for user " + username + ". The tenant was identified by ID "
                            + groupId + " but no tenant with that ID was found");
                } else {
                    defaultTenant.put(serverName, tenantOfGroup);
                }
            }
        }
        Document accountsMap = (Document) userDBObject.get(FieldNames.User.ACCOUNTS.name());
        Map<AccountType, Account> accounts = createAccountMapFromdDBObject(accountsMap);
        User result = new UserImpl(username, email, fullName, company, locale,
                emailValidated == null ? false : emailValidated, passwordResetSecret, validationSecret, defaultTenant,
                accounts.values(), userGroupProvider, lockingAndBanning);
        for (final Role role : roles) {
            result.addRole(role);
        }
        for (String permission : permissions) {
            result.addPermission(new WildcardPermission(permission));
        }
        if (rolesMigrated) {
            // update the user object after roles have been migrated;
            // the default tenant is only a dummy object but should be sufficient
            // for the DB update because, as for the read process, the write process
            // is also only interested in the object's ID
            new MongoObjectFactoryImpl(db).storeUser(result);
        }
        final List<?> subscriptionDocs = (List<?>) userDBObject.get(FieldNames.User.SUBSCRIPTIONS.name());
        result.setSubscriptions(loadSubscriptions(subscriptionDocs));
        return result;
    }

    private Role loadRoleWithProxyUserQualifier(Document rolesO, Map<UUID, RoleDefinition> roleDefinitionsById,
            Map<UUID, UserGroup> userGroups) {
        final RoleDefinition roleDefinition = roleDefinitionsById.get(rolesO.get(FieldNames.Role.ID.name()));
        final Role result;
        if (roleDefinition == null) {
            result = null;
        } else {
            final UUID qualifyingTenantId = (UUID) rolesO.get(FieldNames.Role.QUALIFYING_TENANT_ID.name());
            UserGroup qualifyingGroup = null;
            if (qualifyingTenantId != null && (qualifyingGroup = userGroups.get(qualifyingTenantId)) == null) {
                logger.severe("Unable to resolve tenant with ID "+qualifyingTenantId+
                        " which serves as a role qualifier for role "+roleDefinition.getName()+
                        "; dropping role");
                result = null;
            } else {
                final User proxyQualifyingUser = rolesO.get(FieldNames.Role.QUALIFYING_USERNAME.name()) == null ? null
                        : new UserProxy((String) rolesO.get(FieldNames.Role.QUALIFYING_USERNAME.name())); // if user proxy later cannot be resolved, role will be dropped
                final boolean transitive = rolesO.getBoolean(FieldNames.Role.TRANSITIVE.name(), true);
                result = new Role(roleDefinition, qualifyingGroup, proxyQualifyingUser, transitive);
            }
        }
        return result;
    }

    private Map<AccountType, Account> createAccountMapFromdDBObject(Document accountsMap) {
        Map<AccountType, Account> accounts = new HashMap<>();
        for (Entry<?, ?> e : accountsMap.entrySet()) {
            AccountType type = AccountType.valueOf((String) e.getKey());
            Account account = createAccountFromDBObject((Document) e.getValue(), type);
            accounts.put(type, account);
        }
        return accounts;
    }

    private Account createAccountFromDBObject(Document dbAccount, final AccountType type) {
        switch (type) {
        case USERNAME_PASSWORD:
            String name = (String) dbAccount.get(FieldNames.UsernamePassword.NAME.name());
            String saltedPassword = (String) dbAccount.get(FieldNames.UsernamePassword.SALTED_PW.name());
            Binary salt = (Binary) dbAccount.get(FieldNames.UsernamePassword.SALT.name());
            return new UsernamePasswordAccount(name, saltedPassword, salt.getData());
        // TODO [D056866] add other Account-types
        case SOCIAL_USER:
            SocialUserAccount socialUserAccount = new SocialUserAccount();
            for (Social s : Social.values()) {
                socialUserAccount.setProperty(s.name(), (String) dbAccount.get(s.name()));
            }
            return socialUserAccount;
        default:
            return null;
        }
    }

    @Override
    public Map<String, Object> loadSettings() {
        Map<String, Object> result = new HashMap<>();
        MongoCollection<org.bson.Document> settingsCollection = db.getCollection(CollectionNames.SETTINGS.name());
        try {
            Document query = new Document();
            query.put(FieldNames.Settings.NAME.name(), FieldNames.Settings.VALUES.name());
            Document settingDBObject = settingsCollection.find(query).first();
            if (settingDBObject != null) {
                result = loadSettingMap(settingDBObject);
            } else {
                logger.info("No stored settings found!");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load settings.");
            logger.log(Level.SEVERE, "loadSettings", e);
        }
        return result;
    }

    @Override
    public Map<String, Map<String, String>> loadPreferences() {
        Map<String, Map<String, String>> result = new HashMap<>();
        MongoCollection<org.bson.Document> settingsCollection = db.getCollection(CollectionNames.PREFERENCES.name());
        try {
            for (Object o : settingsCollection.find()) {
                Document usernameAndPreferencesMap = (Document) o;
                Map<String, String> userMap = loadPreferencesMap(
                        (Iterable<?>) usernameAndPreferencesMap.get(FieldNames.Preferences.KEYS_AND_VALUES.name()));
                String username = (String) usernameAndPreferencesMap.get(FieldNames.Preferences.USERNAME.name());
                result.put(username, userMap);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load settings.");
            logger.log(Level.SEVERE, "loadSettings", e);
        }
        return result;
    }

    private Map<String, String> loadPreferencesMap(Iterable<?> preferencesDBObject) {
        Map<String, String> result = new HashMap<>();
        for (Object o : preferencesDBObject) {
            Document keyValue = (Document) o;
            String key = (String) keyValue.get(FieldNames.Preferences.KEY.name());
            String value = (String) keyValue.get(FieldNames.Preferences.VALUE.name());
            result.put(key, value);
        }
        return result;
    }

    private Map<String, Object> loadSettingMap(Document settingDBObject) {
        Map<String, Object> result = new HashMap<>();
        Map<?, ?> map = ((Document) settingDBObject.get(FieldNames.Settings.MAP.name()));
        for (Entry<?, ?> e : map.entrySet()) {
            String key = (String) e.getKey();
            Object value = e.getValue();
            result.put(key, value);
        }
        return result;
    }

    @Override
    public Map<String, Class<?>> loadSettingTypes() {
        Map<String, Class<?>> result = new HashMap<String, Class<?>>();
        MongoCollection<Document> settingsCollection = db.getCollection(CollectionNames.SETTINGS.name());
        try {
            Document query = new Document();
            query.put(FieldNames.Settings.NAME.name(), FieldNames.Settings.TYPES.name());
            Document settingTypesDBObject = settingsCollection.find(query).first();
            if (settingTypesDBObject != null) {
                result = loadSettingTypesMap(settingTypesDBObject);
            } else {
                logger.info("No stored setting types found!");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load setting types.");
            logger.log(Level.SEVERE, "loadSettingTypes", e);
        }
        return result;
    }

    private Map<String, Class<?>> loadSettingTypesMap(Document settingTypesDBObject) {
        Map<String, Class<?>> result = new HashMap<>();
        Map<?, ?> map = (Document) settingTypesDBObject.get(FieldNames.Settings.MAP.name());
        for (Entry<?, ?> e : map.entrySet()) {
            String key = (String) e.getKey();
            Class<?> value = null;
            try {
                value = Class.forName((String) e.getValue());
            } catch (ClassNotFoundException e1) {
                logger.log(Level.WARNING, "Exception trying to load settings types map", e);
            }
            result.put(key, value);
        }
        return result;
    }

    private Subscription[] loadSubscriptions(List<?> subscriptionsDoc) {
        final Subscription[] subscriptions;
        if (subscriptionsDoc != null) {
            subscriptions = new Subscription[subscriptionsDoc.size()];
            int i = 0;
            for (Object o : subscriptionsDoc) {
                final Document doc = (Document) o;
                Map<String, Object> data = new HashMap<String, Object>();
                for (Entry<String, Object> entry : doc.entrySet()) {
                    data.put(entry.getKey(), entry.getValue());
                }
                SubscriptionData subscriptionData = new SubscriptionData(data);
                final SubscriptionDataHandler subscriptionDataHandler = Activator.getSubscriptionDataHandler(subscriptionData.getProviderName());
                subscriptions[i++] = subscriptionDataHandler.toSubscription(subscriptionData);
            }
        } else {
            subscriptions = null;
        }
        return subscriptions;
    }
}
