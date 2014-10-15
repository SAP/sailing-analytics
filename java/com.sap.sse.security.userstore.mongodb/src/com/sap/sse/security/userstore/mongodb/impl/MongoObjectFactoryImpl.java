package com.sap.sse.security.userstore.mongodb.impl;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.shiro.util.SimpleByteSource;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.sap.sse.security.userstore.mongodb.MongoObjectFactory;
import com.sap.sse.security.userstore.shared.Account;
import com.sap.sse.security.userstore.shared.CollectionNames;
import com.sap.sse.security.userstore.shared.FieldNames;
import com.sap.sse.security.userstore.shared.SocialUserAccount;
import com.sap.sse.security.userstore.shared.User;
import com.sap.sse.security.userstore.shared.Account.AccountType;
import com.sap.sse.security.userstore.shared.FieldNames.Social;
import com.sap.sse.security.userstore.shared.UsernamePasswordAccount;

public class MongoObjectFactoryImpl implements MongoObjectFactory {

    private final DB db;

    public MongoObjectFactoryImpl(DB db) {
        this.db = db;
    }

    @Override
    public void storeUser(User user) {
        DBCollection usersCollection = db.getCollection(CollectionNames.USERS.name());
        usersCollection.ensureIndex(FieldNames.User.NAME.name());
        DBObject dbUser = new BasicDBObject();
        DBObject query = new BasicDBObject(FieldNames.User.NAME.name(), user.getName());
        dbUser.put(FieldNames.User.NAME.name(), user.getName());
        dbUser.put(FieldNames.User.EMAIL.name(), user.getEmail());
        dbUser.put(FieldNames.User.ACCOUNTS.name(), createAccountMapObject(user.getAllAccounts()));
        dbUser.put(FieldNames.User.ROLES.name(), user.getRoles());

        usersCollection.update(query, dbUser, /* upsrt */true, /* multi */false, WriteConcern.SAFE);
    }

    @Override
    public void deleteUser(User user) {
        DBCollection usersCollection = db.getCollection(CollectionNames.USERS.name());
        DBObject dbUser = new BasicDBObject();
        dbUser.put(FieldNames.User.NAME.name(), user.getName());

        usersCollection.remove(dbUser);
    }

    private DBObject createAccountMapObject(Map<AccountType, Account> accounts) {
        DBObject dbAccounts = new BasicDBObject();
        for (Entry<AccountType, Account> e : accounts.entrySet()) {
            dbAccounts.put(e.getKey().name(), createAccountObject(e.getValue()));
        }
        return dbAccounts;
    }

    private DBObject createAccountObject(Account a) {
        DBObject dbAccount = new BasicDBObject();
        if (a instanceof UsernamePasswordAccount) {
            UsernamePasswordAccount upa = (UsernamePasswordAccount) a;
            dbAccount.put(FieldNames.UsernamePassword.NAME.name(), upa.getName());
            dbAccount.put(FieldNames.UsernamePassword.SALTED_PW.name(), upa.getSaltedPassword());
            dbAccount.put(FieldNames.UsernamePassword.SALT.name(), ((SimpleByteSource) upa.getSalt()).getBytes());
        }
        if (a instanceof SocialUserAccount) {
            SocialUserAccount account = (SocialUserAccount) a;
            for (Social s : FieldNames.Social.values()) {
                dbAccount.put(s.name(), account.getProperty(s.name()));
            }
        }
        return dbAccount;
    }

    @Override
    public void storeSettings(Map<String, Object> settings) {
        DBCollection settingCollection = db.getCollection(CollectionNames.SETTINGS.name());
        settingCollection.ensureIndex(FieldNames.Settings.NAME.name());
        DBObject dbSettings = new BasicDBObject();
        DBObject query = new BasicDBObject(FieldNames.Settings.NAME.name(), FieldNames.Settings.VALUES.name());
        dbSettings.put(FieldNames.Settings.NAME.name(), FieldNames.Settings.VALUES.name());
        dbSettings.put(FieldNames.Settings.MAP.name(), createSettingsMapObject(settings));

        settingCollection.update(query, dbSettings, /* upsrt */true, /* multi */false, WriteConcern.SAFE);
    }

    @Override
    public void storeSettingTypes(Map<String, Class<?>> settingTypes) {
        DBCollection settingCollection = db.getCollection(CollectionNames.SETTINGS.name());
        settingCollection.ensureIndex(FieldNames.Settings.NAME.name());
        DBObject dbSettingTypes = new BasicDBObject();
        DBObject query = new BasicDBObject(FieldNames.Settings.NAME.name(), FieldNames.Settings.TYPES.name());
        dbSettingTypes.put(FieldNames.Settings.NAME.name(), FieldNames.Settings.TYPES.name());
        dbSettingTypes.put(FieldNames.Settings.MAP.name(), createSettingTypesMapObject(settingTypes));

        settingCollection.update(query, dbSettingTypes, /* upsrt */true, /* multi */false, WriteConcern.SAFE);
    }

    private DBObject createSettingsMapObject(Map<String, Object> settings) {
        DBObject dbSettings = new BasicDBObject();
        for (Entry<String, Object> e : settings.entrySet()) {
            dbSettings.put(e.getKey(), e.getValue());
        }
        return dbSettings;
    }

    private DBObject createSettingTypesMapObject(Map<String, Class<?>> settingTypes) {
        DBObject dbSettingTypes = new BasicDBObject();
        for (Entry<String, Class<?>> e : settingTypes.entrySet()) {
            dbSettingTypes.put(e.getKey(), e.getValue().getName());
        }
        return dbSettingTypes;
    }
}
