package com.sap.sse.security.userstore.mongodb.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;

import com.mongodb.BasicDBList;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.sap.sse.security.interfaces.Social;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.Account;
import com.sap.sse.security.shared.Account.AccountType;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.SocialUserAccount;
import com.sap.sse.security.shared.UsernamePasswordAccount;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.LockingAndBanningImpl;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.subscription.Subscription;
import com.sap.sse.security.subscription.SubscriptionDataHandler;
import com.sap.sse.security.userstore.mongodb.MongoObjectFactory;

public class MongoObjectFactoryImpl implements MongoObjectFactory {
    private static final Logger logger = Logger.getLogger(MongoObjectFactoryImpl.class.getName());
    private final MongoDatabase db;
    final MongoCollection<org.bson.Document> settingCollection;

    public MongoObjectFactoryImpl(MongoDatabase db) {
        this.db = db;
        settingCollection = db.getCollection(CollectionNames.PREFERENCES.name());
        for (Document index : settingCollection.listIndexes()) {
            final Object key = index.get("key");
            if (key instanceof Document) {
                final Document keyDocument = (Document) key;
                if (keyDocument.size() == 1 && keyDocument.containsKey(FieldNames.Preferences.USERNAME.name()) && !index.getBoolean("unique", false)) {
                    settingCollection.dropIndex(index.getString("name"));
                    break;
                }
            }
        }
        try {
            settingCollection.createIndex(new Document(FieldNames.Preferences.USERNAME.name(), 1), new IndexOptions().name("uniquebyusername").unique(true).background(false));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "There are duplicate keys in the "+CollectionNames.PREFERENCES.name()+
                    " collection. Unique index cannot be created. Consider cleaning up.", e);
        }
    }

    @Override
    public MongoDatabase getDatabase() {
        return db;
    }
    
    @Override
    public void storeAccessControlList(AccessControlListAnnotation acl) {
        MongoCollection<org.bson.Document> aclCollection = db.getCollection(CollectionNames.ACCESS_CONTROL_LISTS.name());
        aclCollection.createIndex(new Document(FieldNames.AccessControlList.OBJECT_ID.name(), 1));
        Document dbACL = new Document();
        Document query = new Document(FieldNames.AccessControlList.OBJECT_ID.name(), acl.getIdOfAnnotatedObject().toString());
        dbACL.put(FieldNames.AccessControlList.OBJECT_ID.name(), acl.getIdOfAnnotatedObject().toString());
        dbACL.put(FieldNames.AccessControlList.OBJECT_DISPLAY_NAME.name(), acl.getDisplayNameOfAnnotatedObject());
        BasicDBList permissionMap = new BasicDBList();
        for (Entry<UserGroup, Set<String>> entry : acl.getAnnotation().getActionsByUserGroup().entrySet()) {
            Document permissionMapEntry = new Document();
            permissionMapEntry.put(FieldNames.AccessControlList.PERMISSION_MAP_USER_GROUP_ID.name(),
                    entry.getKey() == null ? null : entry.getKey().getId());
            final BasicDBList dbActions = new BasicDBList();
            dbActions.addAll(entry.getValue());
            permissionMapEntry.put(FieldNames.AccessControlList.PERMISSION_MAP_ACTIONS.name(), dbActions);
            permissionMap.add(permissionMapEntry);
        }
        dbACL.put(FieldNames.AccessControlList.PERMISSION_MAP.name(), permissionMap);
        aclCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, dbACL, new ReplaceOptions().upsert(true));
    }
    
    @Override
    public void deleteAccessControlList(QualifiedObjectIdentifier idOfAccessControlledObject, AccessControlList acl) {
        MongoCollection<org.bson.Document> aclCollection = db.getCollection(CollectionNames.ACCESS_CONTROL_LISTS.name());
        Document dbACL = new Document();
        dbACL.put(FieldNames.AccessControlList.OBJECT_ID.name(), idOfAccessControlledObject.toString());
        aclCollection.deleteOne(dbACL);
    }

    @Override
    public void deleteAllAccessControlLists() {
        final MongoCollection<org.bson.Document> aclCollection = db
                .getCollection(CollectionNames.ACCESS_CONTROL_LISTS.name());
        aclCollection.deleteMany(new Document());
    }

    @Override
    public void storeOwnership(OwnershipAnnotation owner) {
        MongoCollection<org.bson.Document> ownershipCollection = db.getCollection(CollectionNames.OWNERSHIPS.name());
        ownershipCollection.createIndex(new Document(FieldNames.Ownership.OBJECT_ID.name(), 1));
        Document dbOwnership = new Document();
        Document query = new Document(FieldNames.Ownership.OBJECT_ID.name(), owner.getIdOfAnnotatedObject().toString());
        dbOwnership.put(FieldNames.Ownership.OBJECT_ID.name(), owner.getIdOfAnnotatedObject().toString());
        dbOwnership.put(FieldNames.Ownership.OWNER_USERNAME.name(), owner.getAnnotation().getUserOwner()==null?null:owner.getAnnotation().getUserOwner().getName());
        dbOwnership.put(FieldNames.Ownership.TENANT_OWNER_ID.name(), owner.getAnnotation().getTenantOwner()==null?null:owner.getAnnotation().getTenantOwner().getId());
        dbOwnership.put(FieldNames.Ownership.OBJECT_DISPLAY_NAME.name(), owner.getDisplayNameOfAnnotatedObject());
        ownershipCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, dbOwnership, new ReplaceOptions().upsert(true));
    }

    @Override
    public void deleteOwnership(QualifiedObjectIdentifier ownedObjectId, Ownership ownership) {
        MongoCollection<org.bson.Document> ownershipCollection = db.getCollection(CollectionNames.OWNERSHIPS.name());
        Document dbOwnership = new Document();
        dbOwnership.put(FieldNames.Ownership.OBJECT_ID.name(), ownedObjectId.toString());
        ownershipCollection.deleteOne(dbOwnership);
    }

    @Override
    public void deleteAllOwnerships() {
        final MongoCollection<org.bson.Document> ownershipCollection = db
                .getCollection(CollectionNames.OWNERSHIPS.name());
        ownershipCollection.deleteMany(new Document());
    }

    @Override
    public void storeRoleDefinition(RoleDefinition role) {
        MongoCollection<org.bson.Document> roleCollection = db.getCollection(CollectionNames.ROLES.name());
        roleCollection.createIndex(new Document(FieldNames.Role.ID.name(), 1));
        Document dbRole = new Document();
        Document query = new Document(FieldNames.Role.ID.name(), role.getId().toString());
        dbRole.put(FieldNames.Role.ID.name(), role.getId().toString());
        dbRole.put(FieldNames.Role.NAME.name(), role.getName());
        HashSet<String> stringPermissions = new HashSet<>();
        for (WildcardPermission permission : role.getPermissions()) {
            stringPermissions.add(permission.toString());
        }
        dbRole.put(FieldNames.Role.PERMISSIONS.name(), stringPermissions);
        roleCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, dbRole, new ReplaceOptions().upsert(true));
    }

    @Override
    public void deleteRoleDefinition(RoleDefinition role) {
        MongoCollection<org.bson.Document> roleCollection = db.getCollection(CollectionNames.ROLES.name());
        Document dbRole = new Document();
        dbRole.put(FieldNames.Role.ID.name(), role.getId().toString());
        roleCollection.deleteOne(dbRole);
    }
    
    private Document storeRole(Role role) {
        final Document result = new Document();
        result.put(FieldNames.Role.ID.name(), role.getRoleDefinition().getId());
        result.put(FieldNames.Role.NAME.name(), role.getRoleDefinition().getName()); // for human readability only
        result.put(FieldNames.Role.QUALIFYING_TENANT_ID.name(), role.getQualifiedForTenant()==null?null:role.getQualifiedForTenant().getId());
        result.put(FieldNames.Role.QUALIFYING_TENANT_NAME.name(), role.getQualifiedForTenant()==null?null:role.getQualifiedForTenant().getName());
        result.put(FieldNames.Role.QUALIFYING_USERNAME.name(), role.getQualifiedForUser()==null?null:role.getQualifiedForUser().getName());
        result.put(FieldNames.Role.TRANSITIVE.name(), role.isTransitive());
        return result;
    }
    
    @Override
    public void storeUserGroup(UserGroup group) {
        MongoCollection<org.bson.Document> userGroupCollection = db.getCollection(CollectionNames.USER_GROUPS.name());
        userGroupCollection.createIndex(new Document(FieldNames.UserGroup.ID.name(), 1));
        Document dbUserGroup = new Document();
        Document query = new Document(FieldNames.UserGroup.ID.name(), group.getId());
        dbUserGroup.put(FieldNames.UserGroup.ID.name(), group.getId());
        dbUserGroup.put(FieldNames.UserGroup.NAME.name(), group.getName());
        BasicDBList dbUsernames = new BasicDBList();
        for (User user : group.getUsers()) {
            if (user != null) {
                dbUsernames.add(user.getName());
            }
        }
        dbUserGroup.put(FieldNames.UserGroup.USERNAMES.name(), dbUsernames);
        BasicDBList dbRoleDefinitionMap = new BasicDBList();
        for (Entry<RoleDefinition, Boolean> entry : group.getRoleDefinitionMap().entrySet()) {
            Document dbRoleDef = new Document();
            dbRoleDef.put(FieldNames.UserGroup.ROLE_DEFINITION_MAP_ROLE_ID.name(), entry.getKey().getId());
            dbRoleDef.put(FieldNames.UserGroup.ROLE_DEFINITION_MAP_FOR_ALL.name(), entry.getValue());
            dbRoleDefinitionMap.add(dbRoleDef);
        }
        dbUserGroup.put(FieldNames.UserGroup.ROLE_DEFINITION_MAP.name(), dbRoleDefinitionMap);
        userGroupCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, dbUserGroup, new ReplaceOptions().upsert(true));
    }
    
    @Override
    public void deleteUserGroup(UserGroup userGroup) {
        MongoCollection<org.bson.Document> userGroupCollection = db.getCollection(CollectionNames.USER_GROUPS.name());
        Document dbUserGroup = new Document();
        dbUserGroup.put(FieldNames.UserGroup.ID.name(), userGroup.getId());
        userGroupCollection.deleteOne(dbUserGroup);
    }

    @Override
    public void storeUser(User user) {
        MongoCollection<org.bson.Document> usersCollection = db.getCollection(CollectionNames.USERS.name());
        usersCollection.createIndex(new Document(FieldNames.User.NAME.name(), 1));
        Document dbUser = new Document();
        Document query = new Document(FieldNames.User.NAME.name(), user.getName());
        dbUser.put(FieldNames.User.NAME.name(), user.getName());
        dbUser.put(FieldNames.User.EMAIL.name(), user.getEmail());
        dbUser.put(FieldNames.User.FULLNAME.name(), user.getFullName());
        dbUser.put(FieldNames.User.COMPANY.name(), user.getCompany());
        dbUser.put(FieldNames.User.LOCALE.name(), user.getLocale() != null ? user.getLocale().toLanguageTag() : null);
        dbUser.put(FieldNames.User.EMAIL_VALIDATED.name(), user.isEmailValidated());
        dbUser.put(FieldNames.User.PASSWORD_RESET_SECRET.name(), user.getPasswordResetSecret());
        dbUser.put(FieldNames.User.VALIDATION_SECRET.name(), user.getValidationSecret());
        dbUser.put(FieldNames.User.ACCOUNTS.name(), createAccountMapObject(user.getAllAccounts()));
        if (user.getLockingAndBanning() instanceof LockingAndBanningImpl) {
            final LockingAndBanningImpl lockingAndBanning = ((LockingAndBanningImpl) user.getLockingAndBanning());
            dbUser.put(FieldNames.User.LOCKED_UNTIL_MILLIS.name(), lockingAndBanning.getLockedUntil().asMillis());
            dbUser.put(FieldNames.User.NEXT_LOCKING_DURATION_MILLIS.name(), lockingAndBanning.getNextLockingDelay().asMillis());
        } else {
            logger.warning("Expected user locking/banning to be of type "+LockingAndBanningImpl.class.getSimpleName()
                    +" but was of type "+user.getLockingAndBanning().getClass().getSimpleName()+"; not storing to DB");
        }
        BasicDBList dbRoles = new BasicDBList();
        for (Role role : user.getRoles()) {
            dbRoles.add(storeRole(role));
        }
        dbUser.put(FieldNames.User.ROLE_IDS.name(), dbRoles);
        BasicDBList dbPermissions = new BasicDBList();
        for (WildcardPermission permission : user.getPermissions()) {
            dbPermissions.add(permission.toString());
        }
        dbUser.put(FieldNames.User.PERMISSIONS.name(), dbPermissions);
        List<Object> defaultTennants = new BasicDBList();
        for (Entry<String, UserGroup> entries : user.getDefaultTenantMap().entrySet()) {
            Document tenant = new Document();
            tenant.put(FieldNames.User.DEFAULT_TENANT_SERVER.name(), entries.getKey());
            tenant.put(FieldNames.User.DEFAULT_TENANT_GROUP.name(), entries.getValue().getId());
            defaultTennants.add(tenant);
        }
        dbUser.put(FieldNames.User.DEFAULT_TENANT_IDS.name(), defaultTennants);
        dbUser.put(FieldNames.User.SUBSCRIPTIONS.name(), createSubscriptions(user.getSubscriptions()));
        usersCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, dbUser, new ReplaceOptions().upsert(true));
    }
    
    @Override
    public void deleteUser(User user) {
        MongoCollection<org.bson.Document> usersCollection = db.getCollection(CollectionNames.USERS.name());
        Document dbUser = new Document();
        dbUser.put(FieldNames.User.NAME.name(), user.getName());
        usersCollection.deleteOne(dbUser);
    }

    private Document createAccountMapObject(Map<AccountType, Account> accounts) {
        Document dbAccounts = new Document();
        for (Entry<AccountType, Account> e : accounts.entrySet()) {
            dbAccounts.put(e.getKey().name(), createAccountObject(e.getValue()));
        }
        return dbAccounts;
    }

    private Document createAccountObject(Account a) {
        Document dbAccount = new Document();
        if (a instanceof UsernamePasswordAccount) {
            UsernamePasswordAccount upa = (UsernamePasswordAccount) a;
            dbAccount.put(FieldNames.UsernamePassword.NAME.name(), upa.getName());
            dbAccount.put(FieldNames.UsernamePassword.SALTED_PW.name(), upa.getSaltedPassword());
            dbAccount.put(FieldNames.UsernamePassword.SALT.name(), upa.getSalt());
        }
        if (a instanceof SocialUserAccount) {
            SocialUserAccount account = (SocialUserAccount) a;
            for (Social s : Social.values()) {
                dbAccount.put(s.name(), account.getProperty(s.name()));
            }
        }
        return dbAccount;
    }

    @Override
    public void storeSettings(Map<String, Object> settings) {
        MongoCollection<org.bson.Document> settingCollection = db.getCollection(CollectionNames.SETTINGS.name());
        settingCollection.createIndex(new Document(FieldNames.Settings.NAME.name(), 1));
        Document dbSettings = new Document();
        Document query = new Document(FieldNames.Settings.NAME.name(), FieldNames.Settings.VALUES.name());
        dbSettings.put(FieldNames.Settings.NAME.name(), FieldNames.Settings.VALUES.name());
        dbSettings.put(FieldNames.Settings.MAP.name(), createSettingsMapObject(settings));
        settingCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, dbSettings, new ReplaceOptions().upsert(true));
    }

    @Override
    public void deleteAllSettings() {
        final MongoCollection<org.bson.Document> settingsCollection = db
                .getCollection(CollectionNames.SETTINGS.name());
        settingsCollection.deleteMany(new Document());
    }

    @Override
    public void storePreferences(String username, Map<String, String> userMap) {
        BasicDBList dbSettings = new BasicDBList();
        for (Entry<String, String> e : userMap.entrySet()) {
            Document entry = new Document();
            entry.put(FieldNames.Preferences.KEY.name(), e.getKey());
            entry.put(FieldNames.Preferences.VALUE.name(), e.getValue());
            dbSettings.add(entry);
        }
        Document query = new Document(FieldNames.Preferences.USERNAME.name(), username);
        Document update = new Document(FieldNames.Preferences.KEYS_AND_VALUES.name(), dbSettings);
        update.put(FieldNames.Preferences.USERNAME.name(), username);
        settingCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, update, new ReplaceOptions().upsert(true));
    }

    @Override
    public void deleteAllPreferences() {
        final MongoCollection<org.bson.Document> preferencesCollection = db
                .getCollection(CollectionNames.PREFERENCES.name());
        preferencesCollection.deleteMany(new Document());
    }

    @Override
    public void storeSettingTypes(Map<String, Class<?>> settingTypes) {
        MongoCollection<org.bson.Document> settingCollection = db.getCollection(CollectionNames.SETTINGS.name());
        settingCollection.createIndex(new Document(FieldNames.Settings.NAME.name(), 1));
        Document dbSettingTypes = new Document();
        Document query = new Document(FieldNames.Settings.NAME.name(), FieldNames.Settings.TYPES.name());
        dbSettingTypes.put(FieldNames.Settings.NAME.name(), FieldNames.Settings.TYPES.name());
        dbSettingTypes.put(FieldNames.Settings.MAP.name(), createSettingTypesMapObject(settingTypes));
        settingCollection.withWriteConcern(WriteConcern.ACKNOWLEDGED).replaceOne(query, dbSettingTypes, new ReplaceOptions().upsert(true));
    }

    private Document createSettingsMapObject(Map<String, Object> settings) {
        Document dbSettings = new Document();
        for (Entry<String, Object> e : settings.entrySet()) {
            dbSettings.put(e.getKey(), e.getValue());
        }
        return dbSettings;
    }

    private Document createSettingTypesMapObject(Map<String, Class<?>> settingTypes) {
        Document dbSettingTypes = new Document();
        for (Entry<String, Class<?>> e : settingTypes.entrySet()) {
            dbSettingTypes.put(e.getKey(), e.getValue().getName());
        }
        return dbSettingTypes;
    }

    private BasicDBList createSubscriptions(Iterable<Subscription> subscriptions) {
        final BasicDBList result;
        if (subscriptions != null) {
            result = new BasicDBList();
            for (final Subscription subscription : subscriptions) {
                final Document doc = new Document();
                final SubscriptionDataHandler subscriptionDataHandler = Activator.getSubscriptionDataHandler(subscription.getProviderName());
                doc.putAll(subscriptionDataHandler.toMap(subscription));
                result.add(doc);
            }
        } else {
            result = null;
        }
        return result;
    }
}
