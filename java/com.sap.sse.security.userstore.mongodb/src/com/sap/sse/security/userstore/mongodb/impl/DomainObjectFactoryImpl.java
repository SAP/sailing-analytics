package com.sap.sse.security.userstore.mongodb.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.sap.sse.security.AccessControlList;
import com.sap.sse.security.AccessControlListStore;
import com.sap.sse.security.AccessControlListWithStore;
import com.sap.sse.security.Social;
import com.sap.sse.security.Tenant;
import com.sap.sse.security.User;
import com.sap.sse.security.UserStore;
import com.sap.sse.security.shared.Account;
import com.sap.sse.security.shared.Account.AccountType;
import com.sap.sse.security.shared.SocialUserAccount;
import com.sap.sse.security.shared.UsernamePasswordAccount;
import com.sap.sse.security.userstore.mongodb.DomainObjectFactory;

public class DomainObjectFactoryImpl implements DomainObjectFactory {
    private static final Logger logger = Logger.getLogger(DomainObjectFactoryImpl.class.getName());

    private final DB db;

    public DomainObjectFactoryImpl(DB db) {
        this.db = db;
    }
    
    @Override
    public Iterable<AccessControlList> loadAllAccessControlLists(UserStore userStore, AccessControlListStore aclStore) {
        ArrayList<AccessControlList> result = new ArrayList<>();
        DBCollection aclCollection = db.getCollection(CollectionNames.ACCESS_CONTROL_LISTS.name());
        try {
            for (DBObject o : aclCollection.find()) {
                result.add(loadAccessControlList(o, userStore, aclStore));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load ACLs.");
            logger.log(Level.SEVERE, "loadAllAccessControlLists", e);
        }
        return result;
    }
    
    @Override
    public AccessControlList loadAccessControlList(String name, UserStore userStore, AccessControlListStore aclStore) {
        DBObject query = new BasicDBObject();
        query.put(FieldNames.AccessControlList.NAME.name(), name);
        DBCursor cursor = db.getCollection(CollectionNames.ACCESS_CONTROL_LISTS.name()).find(query);
        if (cursor.hasNext()) {
            return loadAccessControlList(cursor.next(), userStore, aclStore);
        }
        return null;
    }

    public AccessControlList loadAccessControlList(DBObject aclDBObject, UserStore userStore, AccessControlListStore aclStore) {
        final String name = (String) aclDBObject.get(FieldNames.AccessControlList.NAME.name());
        final String owner = (String) aclDBObject.get(FieldNames.AccessControlList.OWNER.name());
        @SuppressWarnings("unchecked")
        Map<String, Set<String>> permissionMap = (Map<String, Set<String>>) aclDBObject.get(FieldNames.AccessControlList.PERMISSION_MAP.name());
        AccessControlList result = new AccessControlListWithStore(name, owner, permissionMap, userStore, aclStore);
        return result;
    }
    
    @Override
    public Iterable<Tenant> loadAllTenants(AccessControlListStore aclStore) {
        ArrayList<Tenant> result = new ArrayList<>();
        DBCollection tenantCollection = db.getCollection(CollectionNames.TENANTS.name());
        try {
            for (DBObject o : tenantCollection.find()) {
                result.add(loadTenant(o, aclStore));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load tenants.");
            logger.log(Level.SEVERE, "loadAllTenants", e);
        }
        return result;
    }
    
    private Tenant loadTenant(DBObject tenantDBObject, AccessControlListStore aclStore) {
        final String name = (String) tenantDBObject.get(FieldNames.Tenant.NAME.name());
        Set<String> users = new HashSet<String>();
        BasicDBList usersO = (BasicDBList) tenantDBObject.get(FieldNames.Tenant.USERS.name());
        if (usersO != null) {
            for (Object o : usersO) {
                users.add((String) o);
            }
        }
        Tenant result = new Tenant(name, aclStore.getAccessControlListByName(name));
        for (String user : users) {
            result.add(user);
        }
        return result;
    }

    @Override
    public Iterable<User> loadAllUsers() {
        ArrayList<User> result = new ArrayList<>();
        DBCollection userCollection = db.getCollection(CollectionNames.USERS.name());
        try {
            for (DBObject o : userCollection.find()) {
                result.add(loadUser(o));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load users.");
            logger.log(Level.SEVERE, "loadAllUsers", e);
        }
        return result;
    }
    
    @Override
    public User loadUser(String name) {
        User result;
        BasicDBObject query = new BasicDBObject();
        query.put(FieldNames.User.NAME.name(), name);
        DBCollection userCollection = db.getCollection(CollectionNames.USERS.name());
        DBObject userDBObject = userCollection.findOne(query);
        if (userDBObject != null) {
            result = loadUser(userDBObject);
        } else {
            result = null;
        }
        return result;
    }
    
    private User loadUser(DBObject userDBObject) {
        final String name = (String) userDBObject.get(FieldNames.User.NAME.name());
        final String email = (String) userDBObject.get(FieldNames.User.EMAIL.name());
        final String fullName = (String) userDBObject.get(FieldNames.User.FULLNAME.name());
        final String company = (String) userDBObject.get(FieldNames.User.COMPANY.name());
        final String localeRaw = (String)  userDBObject.get(FieldNames.User.LOCALE.name());
        final Locale locale = localeRaw != null ? Locale.forLanguageTag(localeRaw) : null; 
        Boolean emailValidated = (Boolean) userDBObject.get(FieldNames.User.EMAIL_VALIDATED.name());
        String passwordResetSecret = (String) userDBObject.get(FieldNames.User.PASSWORD_RESET_SECRET.name());
        String validationSecret = (String) userDBObject.get(FieldNames.User.VALIDATION_SECRET.name());
        Set<String> roles = new HashSet<String>();
        Set<String> permissions = new HashSet<String>();
        BasicDBList rolesO = (BasicDBList) userDBObject.get(FieldNames.User.ROLES.name());
        if (rolesO != null) {
            for (Object o : rolesO) {
                roles.add((String) o);
            }
        }
        BasicDBList permissionsO = (BasicDBList) userDBObject.get(FieldNames.User.PERMISSIONS.name());
        if (permissionsO != null) {
            for (Object o : permissionsO) {
                permissions.add((String) o);
            }
        }
        DBObject accountsMap = (DBObject) userDBObject.get(FieldNames.User.ACCOUNTS.name());
        Map<AccountType, Account> accounts = createAccountMapFromdDBObject(accountsMap);
        User result = new User(name, email, fullName, company, locale, emailValidated==null?false:emailValidated, passwordResetSecret, validationSecret, accounts.values());
        for (String role : roles) {
            result.addRole(role);
        }
        for (String permission : permissions) {
            result.addPermission(permission);
        }
        return result;
    }

    private Map<AccountType, Account> createAccountMapFromdDBObject(DBObject accountsMap) {
        Map<AccountType, Account> accounts = new HashMap<>();
        Map<?, ?> accountsM = (Map<?, ?>) accountsMap.toMap();
        for (Entry<?, ?> e : accountsM.entrySet()){
            AccountType type = AccountType.valueOf((String) e.getKey());
            Account account = createAccountFromDBObject((DBObject) e.getValue(), type);
            accounts.put(type, account);
        }
        return accounts;
    }

    private Account createAccountFromDBObject(DBObject dbAccount, final AccountType type) {
        switch (type) {
        case USERNAME_PASSWORD:
            String name = (String) dbAccount.get(FieldNames.UsernamePassword.NAME.name());
            String saltedPassword = (String) dbAccount.get(FieldNames.UsernamePassword.SALTED_PW.name());
            byte[] salt = (byte[]) dbAccount.get(FieldNames.UsernamePassword.SALT.name());
            return new UsernamePasswordAccount(name, saltedPassword, salt);
            //TODO [D056866] add other Account-types
        case SOCIAL_USER:
            SocialUserAccount socialUserAccount = new SocialUserAccount();
            for (Social s : Social.values()){
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
        DBCollection settingsCollection = db.getCollection(CollectionNames.SETTINGS.name());

        try {
            BasicDBObject query = new BasicDBObject();
            query.put(FieldNames.Settings.NAME.name(), FieldNames.Settings.VALUES.name());
            DBObject settingDBObject = (DBObject) settingsCollection.findOne(query);
            if (settingDBObject != null) {
                result = loadSettingMap(settingDBObject);
            }
            else {
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
        DBCollection settingsCollection = db.getCollection(CollectionNames.PREFERENCES.name());
        try {
            for (Object o : settingsCollection.find()) {
                DBObject usernameAndPreferencesMap = (DBObject) o;
                Map<String, String> userMap = loadPreferencesMap((BasicDBList) usernameAndPreferencesMap.get(FieldNames.Preferences.KEYS_AND_VALUES.name()));
                String username = (String) usernameAndPreferencesMap.get(FieldNames.Preferences.USERNAME.name());
                result.put(username, userMap);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load settings.");
            logger.log(Level.SEVERE, "loadSettings", e);
        }
        return result;
    }

    private Map<String, String> loadPreferencesMap(BasicDBList preferencesDBObject) {
        Map<String, String> result = new HashMap<>();
        for (Object o : preferencesDBObject) {
            DBObject keyValue = (DBObject) o;
            String key = (String) keyValue.get(FieldNames.Preferences.KEY.name());
            String value = (String) keyValue.get(FieldNames.Preferences.VALUE.name());
            result.put(key, value);
        }
        return result;
    }

    private Map<String, Object> loadSettingMap(DBObject settingDBObject) {
        Map<String, Object> result = new HashMap<>();
        Map<?, ?> map = ((DBObject) settingDBObject.get(FieldNames.Settings.MAP.name())).toMap();
        for (Entry<?, ?> e : map.entrySet()){
            String key = (String) e.getKey();
            Object value = e.getValue();
            result.put(key, value);
        }
        return result;
    }

    @Override
    public Map<String, Class<?>> loadSettingTypes() {
        Map<String, Class<?>> result = new HashMap<String, Class<?>>();
        DBCollection settingsCollection = db.getCollection(CollectionNames.SETTINGS.name());

        try {
            BasicDBObject query = new BasicDBObject();
            query.put(FieldNames.Settings.NAME.name(), FieldNames.Settings.TYPES.name());
            DBObject settingTypesDBObject = settingsCollection.findOne(query);
            if (settingTypesDBObject != null) {
                result = loadSettingTypesMap(settingTypesDBObject);
            }
            else {
                logger.info("No stored setting types found!");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to MongoDB, unable to load setting types.");
            logger.log(Level.SEVERE, "loadSettingTypes", e);
        }

        return result;
    }

    private Map<String, Class<?>> loadSettingTypesMap(DBObject settingTypesDBObject) {
        Map<String, Class<?>> result = new HashMap<>();
        Map<?, ?> map = ((DBObject) settingTypesDBObject.get(FieldNames.Settings.MAP.name())).toMap();
        for (Entry<?, ?> e : map.entrySet()){
            String key = (String) e.getKey();
            Class<?> value = null;
            try {
                value = Class.forName((String) e.getValue());
            } catch (ClassNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            result.put(key, value);
        }
        return result;
    }
}
