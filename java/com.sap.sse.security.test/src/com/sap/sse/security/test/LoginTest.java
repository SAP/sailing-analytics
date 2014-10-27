package com.sap.sse.security.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.shiro.SecurityUtils;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.DB;
import com.mongodb.MongoException;
import com.sap.sailing.mongodb.MongoDBConfiguration;
import com.sap.sailing.mongodb.MongoDBService;
import com.sap.sse.security.SecurityServiceImpl;
import com.sap.sse.security.UsernamePasswordRealm;
import com.sap.sse.security.impl.Activator;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;
import com.sap.sse.security.userstore.mongodb.impl.CollectionNames;

public class LoginTest {
    private UserStoreImpl store;

    @Before
    public void setUp() throws UnknownHostException, MongoException {
        final MongoDBConfiguration dbConfiguration = MongoDBConfiguration.getDefaultTestConfiguration();
        final MongoDBService service = dbConfiguration.getService();
        DB db = service.getDB();
        db.getCollection(CollectionNames.SETTINGS.name()).drop();
        db.getCollection(CollectionNames.PREFERENCES.name()).drop();
        db.getCollection(CollectionNames.PREFERENCES.name()).drop();
        store = new UserStoreImpl();
        UsernamePasswordRealm.setTestUserStore(store);
        Activator.setTestUserStore(store);
        new SecurityServiceImpl(store, /* mailProperties */ new Properties());
    }

    @Test
    public void testGetUser() {
        assertNotNull("Subject should not be null: ", SecurityUtils.getSubject());
    }
    
    @Test
    public void setPreferencesTest() {
        store.setPreference("me", "key", "value");
        UserStoreImpl store2 = new UserStoreImpl();
        assertEquals("value", store2.getPreference("me", "key"));
    }

    @Test
    public void setAndUnsetPreferencesTest() {
        store.setPreference("me", "key", "value");
        store.unsetPreference("me", "key");
        UserStoreImpl store2 = new UserStoreImpl();
        assertNull(store2.getPreference("me", "key"));
    }
}
