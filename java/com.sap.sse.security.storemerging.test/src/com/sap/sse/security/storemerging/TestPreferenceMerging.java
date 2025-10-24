package com.sap.sse.security.storemerging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.UserStoreManagementException;

public class TestPreferenceMerging extends AbstractStoreMergeTest {
    private final static String PREF_A = "a";
    private final static String PREF_B = "b";
    private final static String PREF_C = "c";
    
    @BeforeEach
    public void setUp() throws IOException, UserStoreManagementException {
        // using another test's set-up as a dummy, mainly to get the admin user object
        setUp("source_TestRoleHandlingInUserGroupMerge", "target_TestRoleHandlingInUserGroupMerge");
    }
    
    @Test
    public void testPreferenceMerging() throws UserStoreManagementException {
        // assertions against unmodified target
        assertNull(targetUserStore.getPreference("admin", PREF_A));
        assertNull(targetUserStore.getPreference("admin", PREF_B));
        assertNull(targetUserStore.getPreference("admin", PREF_C));
        // add PREF_A to target store
        targetUserStore.setPreference("admin", PREF_A, "a_target");
        targetUserStore.setPreference("admin", PREF_B, "b_target");
        final Pair<UserStore, AccessControlStore> sourceStores = readSourceStores();
        final UserStore sourceUserStore = sourceStores.getA();
        final AccessControlStore sourceAccessControlStore = sourceStores.getB();
        // assertions against unmodified source
        assertNull(sourceUserStore.getPreference("admin", PREF_A));
        assertNull(sourceUserStore.getPreference("admin", PREF_B));
        assertNull(sourceUserStore.getPreference("admin", PREF_C));
        sourceUserStore.setPreference("admin", PREF_B, "b_source");
        sourceUserStore.setPreference("admin", PREF_C, "c_source");
        mergeSourceIntoTarget(sourceUserStore, sourceAccessControlStore);
        // assertions for merge result
        // no PREF_A in source, so target value remains unchanged
        assertEquals("a_target", targetUserStore.getPreference("admin", PREF_A));
        // PREF_B value in target already exists; it shall not be overwritten by a merge
        assertEquals("b_target", targetUserStore.getPreference("admin", PREF_B));
        // no PREF_C existed in target; the source value is expected to be merged into the target
        assertEquals("c_source", targetUserStore.getPreference("admin", PREF_C));
    }
}
