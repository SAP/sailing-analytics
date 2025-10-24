package com.sap.sse.security.storemerging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.UserStoreManagementException;

public class TestDefaultUserGroupMerge extends AbstractStoreMergeTest {
    @BeforeEach
    public void setUp() throws IOException, UserStoreManagementException {
        setUp("source_TestDefaultUserGroupMerge", "target_TestDefaultUserGroupMerge");
    }
    
    @Test
    public void testDefaultUserGroupMerge() throws UserStoreManagementException {
        // assertions against unmodified target
        assertNotNull(targetUserStore.getUserGroupByName("aaa-tenant"));
        assertNotNull(targetUserStore.getUserGroupByName("bbb-tenant"));
        assertTrue(targetUserStore.getUserGroupByName("aaa-tenant").contains(targetUserStore.getUserByName("aaa")));
        assertFalse(targetUserStore.getUserGroupByName("aaa-tenant").contains(targetUserStore.getUserByName("bbb")));
        assertTrue(targetUserStore.getUserGroupByName("bbb-tenant").contains(targetUserStore.getUserByName("bbb")));
        assertFalse(targetUserStore.getUserGroupByName("bbb-tenant").contains(targetUserStore.getUserByName("aaa")));
        final Pair<UserStore, AccessControlStore> sourceStores = readSourceStores();
        final UserStore sourceUserStore = sourceStores.getA();
        final AccessControlStore sourceAccessControlStore = sourceStores.getB();
        // assertions against unmodified source
        assertNotNull(sourceUserStore.getUserGroupByName("aaa-tenant"));
        assertNotNull(sourceUserStore.getUserGroupByName("bbb-tenant"));
        assertTrue(sourceUserStore.getUserGroupByName("aaa-tenant").contains(sourceUserStore.getUserByName("aaa")));
        assertTrue(sourceUserStore.getUserGroupByName("aaa-tenant").contains(sourceUserStore.getUserByName("bbb")));
        assertTrue(sourceUserStore.getUserGroupByName("bbb-tenant").contains(sourceUserStore.getUserByName("bbb")));
        assertFalse(sourceUserStore.getUserGroupByName("bbb-tenant").contains(sourceUserStore.getUserByName("aaa")));
        mergeSourceIntoTarget(sourceUserStore, sourceAccessControlStore);
        // assertions for merge result
        assertTrue(targetUserStore.getUserGroupByName("aaa-tenant").contains(targetUserStore.getUserByName("aaa")));
        // Now the bbb user is expected to be part of the aaa-tenant group in target because
        // the aaa-tenant groups was considered identical and hence their user lists were merged
        assertTrue(targetUserStore.getUserGroupByName("aaa-tenant").contains(targetUserStore.getUserByName("bbb")));
        assertTrue(targetUserStore.getUserGroupByName("bbb-tenant").contains(targetUserStore.getUserByName("bbb")));
        assertFalse(targetUserStore.getUserGroupByName("bbb-tenant").contains(targetUserStore.getUserByName("aaa")));
    }
}
