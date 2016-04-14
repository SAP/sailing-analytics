package com.sap.sse.security.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.sap.sse.security.UserStore;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;

public class UserStoreTest {
    private final UserStore userStore = new UserStoreImpl(null, null);
    private final String username = "abc";
    private final String email = "e@mail.com";
    private final String accessToken = "ak";
    private final String prefKey = "pk";
    private final String prefValue = "pv";
    
    @Before
    public void setUp() throws UserManagementException {
        userStore.createUser(username, email);
        userStore.setAccessToken(username, accessToken);
        userStore.setPreference(username, prefKey, prefValue);
    }
    
    @Test
    public void testClear() throws UserManagementException {
        assertEquals(prefValue, userStore.getPreference(username, prefKey));
        assertEquals(username, userStore.getUserByAccessToken(accessToken).getName());
        userStore.clear();
        assertNull(userStore.getPreference(username, prefKey));
        assertNull(userStore.getUserByAccessToken(accessToken));
        assertNull(userStore.getAccessToken(username));
    }

    @Test
    public void testUpdate() throws UserManagementException {
        UserStore newUserStore = new UserStoreImpl(null, null);
        newUserStore.replaceContentsFrom(userStore);
        assertEquals(prefValue, newUserStore.getPreference(username, prefKey));
        assertEquals(username, newUserStore.getUserByAccessToken(accessToken).getName());
    }
}
