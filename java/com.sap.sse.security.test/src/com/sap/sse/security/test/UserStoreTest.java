package com.sap.sse.security.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.impl.LockingAndBanningImpl;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;

public class UserStoreTest {
    private final UserStore userStore;
    private final String username = "abc";
    private final String email = "e@mail.com";
    private final String accessToken = "ak";
    private final String prefKey = "pk";
    private final String prefValue = "pv";
    
    public UserStoreTest() throws UserGroupManagementException, UserManagementException {
        userStore = new UserStoreImpl(null, null, "TestDefaultTenant");
    }
    
    @BeforeEach
    public void setUp() throws UserManagementException, UserGroupManagementException {
        userStore.createUser(username, email, new LockingAndBanningImpl());
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
    }

    @Test
    public void testUpdate() throws UserManagementException, UserGroupManagementException {
        UserStore newUserStore = new UserStoreImpl(null, null, "TestDefaultTenant");
        newUserStore.replaceContentsFrom(userStore);
        assertEquals(prefValue, newUserStore.getPreference(username, prefKey));
        assertEquals(username, newUserStore.getUserByAccessToken(accessToken).getName());
    }
    
    /**
     * There was a bug that caused the preferences not to be removed when a user was deleted.
     */
    @Test
    public void testDeleteUserWithPreference() throws UserManagementException {
        userStore.deleteUser(username);
        assertNull(userStore.getPreference(username, prefKey), prefValue);
    }
}
