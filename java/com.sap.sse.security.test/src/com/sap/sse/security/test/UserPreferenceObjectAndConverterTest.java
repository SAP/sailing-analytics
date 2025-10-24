package com.sap.sse.security.test;

import java.io.Serializable;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.impl.LockingAndBanningImpl;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;
import com.sap.sse.security.userstore.mongodb.impl.CollectionNames;

public class UserPreferenceObjectAndConverterTest {
    
    private UserStoreImpl store;

    private final String email = "anonymous@sapsailing.com";
    private static final String user1 = "me";
    private static final String user2 = "somebody_else";
    
    private static final JavaIoSerializablePreferenceConverter<SimplePreferenceForSerialization> prefConverter = new JavaIoSerializablePreferenceConverter<>();
    private static final String prefKey1 = "prefKey1";
    private static final SimplePreferenceForSerialization pref1 = new SimplePreferenceForSerialization("test12345", false, 432.567);
    private static final String serializedPref1 = prefConverter.toPreferenceString(pref1);
    private static final String prefKey2 = "prefKey2";
    private static final SimplePreferenceForSerialization pref2 = new SimplePreferenceForSerialization("some sailing value", true, 9.87654321);
    private static final String serializedPref2 = prefConverter.toPreferenceString(pref2);

    @BeforeEach
    public void setUp() throws UnknownHostException, MongoException, UserGroupManagementException, UserManagementException {
        final MongoDBConfiguration dbConfiguration = MongoDBConfiguration.getDefaultTestConfiguration();
        final MongoDBService service = dbConfiguration.getService();
        MongoDatabase db = service.getDB();
        db.getCollection(CollectionNames.USERS.name()).drop();
        db.getCollection(CollectionNames.SETTINGS.name()).drop();
        db.getCollection(CollectionNames.PREFERENCES.name()).drop();
        db.getCollection(CollectionNames.PREFERENCES.name()).drop();
        store = new UserStoreImpl("TestDefaultTenant");
    }

    @Test
    public void noConverterSetTest() {
        store.setPreference(user1, prefKey1, serializedPref1);
        Assertions.assertNull(store.getPreferenceObject(user1, prefKey1));
    }
    
    @Test
    public void converterAlreadyRegisteredWhenSettingPreferenceTest() {
        store.registerPreferenceConverter(prefKey1, prefConverter);
        store.setPreference(user1, prefKey1, serializedPref1);
        Assertions.assertEquals(pref1, store.getPreferenceObject(user1, prefKey1));
    }
    
    @Test
    public void converterSetWhenPreferenceAlreadyRegisteredTest() {
        store.setPreference(user1, prefKey1, serializedPref1);
        store.registerPreferenceConverter(prefKey1, prefConverter);
        Assertions.assertEquals(pref1, store.getPreferenceObject(user1, prefKey1));
    }
    
    @Test
    public void preferencesForTwoUsersTest() {
        store.setPreference(user1, prefKey1, serializedPref1);
        store.setPreference(user2, prefKey1, serializedPref2);
        store.registerPreferenceConverter(prefKey1, prefConverter);
        Assertions.assertEquals(pref1, store.getPreferenceObject(user1, prefKey1));
        Assertions.assertEquals(pref2, store.getPreferenceObject(user2, prefKey1));
    }
    
    @Test
    public void twoPreferencesForOneUserTest() {
        store.setPreference(user1, prefKey1, serializedPref1);
        store.setPreference(user1, prefKey2, serializedPref2);
        store.registerPreferenceConverter(prefKey1, prefConverter);
        store.registerPreferenceConverter(prefKey2, prefConverter);
        Assertions.assertEquals(pref1, store.getPreferenceObject(user1, prefKey1));
        Assertions.assertEquals(pref2, store.getPreferenceObject(user1, prefKey2));
    }
    
    @Test
    public void setPreferenceObjectTest() {
        store.registerPreferenceConverter(prefKey1, prefConverter);
        store.setPreferenceObject(user1, prefKey1, pref1);
        Assertions.assertEquals(serializedPref1, store.getPreference(user1, prefKey1));
        Assertions.assertEquals(pref1, store.getPreferenceObject(user1, prefKey1));
    }
    
    @Test
    public void unsetPreferenceTest() {
        store.registerPreferenceConverter(prefKey1, prefConverter);
        store.setPreferenceObject(user1, prefKey1, pref1);
        store.unsetPreference(user1, prefKey1);
        Assertions.assertNull(store.getPreferenceObject(user1, prefKey1));
    }
    
    @Test
    public void setPreferenceObjectToNullTest() {
        store.registerPreferenceConverter(prefKey1, prefConverter);
        store.setPreferenceObject(user1, prefKey1, pref1);
        store.setPreferenceObject(user1, prefKey1, null);
        Assertions.assertNull(store.getPreferenceObject(user1, prefKey1));
    }
    
    /**
     * There was a bug that caused the preferences not to be removed when a user was deleted.
     * @throws UserGroupManagementException 
     * @throws TenantManagementException 
     */
    @Test
    public void deleteUserWithPreferenceObjectTest() throws UserManagementException, UserGroupManagementException {
        store.createUser(user1, email, new LockingAndBanningImpl());
        store.registerPreferenceConverter(prefKey1, prefConverter);
        store.setPreferenceObject(user1, prefKey1, pref1);
        store.deleteUser(user1);
        Assertions.assertNull(store.getPreferenceObject(user1, prefKey1));
    }
    
    @Test
    public void removeConverterTest() throws UserManagementException, UserGroupManagementException {
        store.createUser(user1, email, new LockingAndBanningImpl());
        store.registerPreferenceConverter(prefKey1, prefConverter);
        store.setPreference(user1, prefKey1, serializedPref1);
        store.removePreferenceConverter(prefKey1);
        Assertions.assertNull(store.getPreferenceObject(user1, prefKey1));
    }

    public static class SimplePreferenceForSerialization implements Serializable {
        private static final long serialVersionUID = -580569932709860895L;

        private String someString;
        private boolean someBoolean;
        private double soumeDouble;

        public SimplePreferenceForSerialization(String someString, boolean someBoolean, double soumeDouble) {
            super();
            this.someString = someString;
            this.someBoolean = someBoolean;
            this.soumeDouble = soumeDouble;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (someBoolean ? 1231 : 1237);
            result = prime * result + ((someString == null) ? 0 : someString.hashCode());
            long temp;
            temp = Double.doubleToLongBits(soumeDouble);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            SimplePreferenceForSerialization other = (SimplePreferenceForSerialization) obj;
            if (someBoolean != other.someBoolean)
                return false;
            if (someString == null) {
                if (other.someString != null)
                    return false;
            } else if (!someString.equals(other.someString))
                return false;
            if (Double.doubleToLongBits(soumeDouble) != Double.doubleToLongBits(other.soumeDouble))
                return false;
            return true;
        }
    }
}
