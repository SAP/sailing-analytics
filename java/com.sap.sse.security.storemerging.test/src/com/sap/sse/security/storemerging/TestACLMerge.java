package com.sap.sse.security.storemerging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;

public class TestACLMerge extends AbstractStoreMergeTest {
    @BeforeEach
    public void setUp() throws IOException, UserStoreManagementException {
        setUp("source_TestACLMerge", "target_TestACLMerge");
    }
    
    /**
     * Validates that an ACL for an object for which the target has an ACL is merged correctly: groups for which no
     * action permissions have been specified in the target are added; action permission sets for groups for which the
     * target already has permissions are merged<p>
     * 
     * As a "drive-by," this test also validates that a default creation group for a server for which the
     * target already has one set for a user is not overwritten, and that default creation group settings
     * are added if the target user has no default creation group set yet for that server name.
     */
    @Test
    public void testACLMerge() throws UserStoreManagementException {
        final String NAME_OF_USER_TO_BE_DROPPED = "userWithACL";
        final String NAME_OF_NEW_GROUP_WITH_NEW_ACTIONS = "newGroup";
        // *********** assertions against unmodified target ***********
        final User targetUserWithACL = targetUserStore.getUserByName(NAME_OF_USER_TO_BE_DROPPED);
        assertNotNull(targetUserWithACL);
        final UserGroup targetUserWithACLTenant = targetUserStore.getUserGroupByName(NAME_OF_USER_TO_BE_DROPPED+SecurityService.TENANT_SUFFIX);
        assertNotNull(targetUserWithACLTenant);
        assertSame(targetUserWithACLTenant, targetUserWithACL.getDefaultTenant("unknown server name"));
        assertNull(targetUserWithACL.getDefaultTenant("ARCHIVE"));
        assertNull(targetUserStore.getUserGroupByName(NAME_OF_NEW_GROUP_WITH_NEW_ACTIONS));
        final User targetAdmin = targetUserStore.getUserByName("admin");
        final UserGroup targetAdminTenant = targetUserStore.getUserGroupByName("admin"+SecurityService.TENANT_SUFFIX);
        assertNotNull(targetAdmin);
        final AccessControlList targetUserWithAclACL = targetAccessControlStore.getAccessControlList(targetUserWithACL.getIdentifier()).getAnnotation();
        assertNotNull(targetUserWithAclACL);
        assertTrue(targetUserWithAclACL.getActionsByUserGroup().get(null).contains("READ_PUBLIC"));
        assertTrue(targetUserWithAclACL.getActionsByUserGroup().get(null).contains("!DELETE"));
        assertTrue(targetUserWithAclACL.getActionsByUserGroup().get(targetAdminTenant).contains("UPDATE"));
        assertFalse(targetUserWithAclACL.getActionsByUserGroup().get(targetAdminTenant).contains("DELETE"));
        // *********** reading source stores ***********
        final Pair<UserStore, AccessControlStore> sourceStores = readSourceStores();
        final UserStore sourceUserStore = sourceStores.getA();
        final AccessControlStore sourceAccessControlStore = sourceStores.getB();
        // *********** assertions against unmodified source ***********
        final User sourceUserWithACL = sourceUserStore.getUserByName(NAME_OF_USER_TO_BE_DROPPED);
        assertNotNull(sourceUserWithACL);
        final UserGroup sourceUserWithACLTenant = sourceUserStore.getUserGroupByName(NAME_OF_USER_TO_BE_DROPPED+SecurityService.TENANT_SUFFIX);
        assertNotNull(sourceUserWithACLTenant);
        assertNotNull(sourceUserStore.getUserGroupByName(NAME_OF_NEW_GROUP_WITH_NEW_ACTIONS));
        final User sourceAdmin = sourceUserStore.getUserByName("admin");
        final UserGroup sourceAdminTenant = sourceUserStore.getUserGroupByName("admin"+SecurityService.TENANT_SUFFIX);
        assertNotNull(sourceAdmin);
        assertNotNull(sourceAdminTenant);
        assertSame(sourceAdminTenant, sourceUserWithACL.getDefaultTenant("unknown server name"));
        assertSame(sourceUserWithACLTenant, sourceUserWithACL.getDefaultTenant("ARCHIVE"));
        final UserGroup sourceNewGroup = sourceUserStore.getUserGroupByName(NAME_OF_NEW_GROUP_WITH_NEW_ACTIONS);
        assertNotNull(sourceNewGroup);
        final AccessControlList sourceUserWithAclACL = sourceAccessControlStore.getAccessControlList(sourceUserWithACL.getIdentifier()).getAnnotation();
        assertNotNull(sourceUserWithAclACL);
        assertFalse(sourceUserWithAclACL.getActionsByUserGroup().get(null).contains("READ_PUBLIC"));
        assertTrue(sourceUserWithAclACL.getActionsByUserGroup().get(null).contains("READ"));
        assertFalse(sourceUserWithAclACL.getActionsByUserGroup().get(null).contains("!DELETE"));
        assertFalse(sourceUserWithAclACL.getActionsByUserGroup().get(sourceAdminTenant).contains("UPDATE"));
        assertTrue(sourceUserWithAclACL.getActionsByUserGroup().get(sourceAdminTenant).contains("DELETE"));
        assertTrue(sourceUserWithAclACL.getActionsByUserGroup().get(sourceAdminTenant).contains("CHANGE_ACL"));
        assertTrue(sourceUserWithAclACL.getActionsByUserGroup().get(sourceNewGroup).contains("CHANGE_OWNERSHIP"));
        assertTrue(sourceUserWithAclACL.getActionsByUserGroup().get(sourceNewGroup).contains("!FORCE_OVERWRITE_PASSWORD"));
        // *********** merge ***********
        mergeSourceIntoTarget(sourceUserStore, sourceAccessControlStore);
        // *********** assertions for merge result ***********
        final User newTargetUserWithACL = targetUserStore.getUserByName(NAME_OF_USER_TO_BE_DROPPED);
        assertNotNull(newTargetUserWithACL);
        assertNotNull(targetUserStore.getUserGroupByName(NAME_OF_NEW_GROUP_WITH_NEW_ACTIONS));
        final UserGroup targetNewGroup = sourceUserStore.getUserGroupByName(NAME_OF_NEW_GROUP_WITH_NEW_ACTIONS);
        assertNotNull(targetNewGroup);
        final AccessControlList newTargetUserWithAclACL = targetAccessControlStore.getAccessControlList(newTargetUserWithACL.getIdentifier()).getAnnotation();
        assertNotNull(newTargetUserWithAclACL);
        assertTrue(newTargetUserWithAclACL.getActionsByUserGroup().get(null).contains("READ_PUBLIC"));
        assertTrue(newTargetUserWithAclACL.getActionsByUserGroup().get(null).contains("READ"));
        assertTrue(newTargetUserWithAclACL.getActionsByUserGroup().get(null).contains("!DELETE"));
        assertTrue(newTargetUserWithAclACL.getActionsByUserGroup().get(targetAdminTenant).contains("UPDATE"));
        assertTrue(newTargetUserWithAclACL.getActionsByUserGroup().get(targetAdminTenant).contains("DELETE"));
        assertTrue(newTargetUserWithAclACL.getActionsByUserGroup().get(targetAdminTenant).contains("CHANGE_ACL"));
        assertTrue(newTargetUserWithAclACL.getActionsByUserGroup().get(targetNewGroup).contains("CHANGE_OWNERSHIP"));
        assertTrue(newTargetUserWithAclACL.getActionsByUserGroup().get(targetNewGroup).contains("!FORCE_OVERWRITE_PASSWORD"));
        // validate that default creation group/tenant is not touched in target for existing "unknown server name"
        // and merged for new "ARCHIVE" record:
        assertSame(targetUserWithACLTenant, newTargetUserWithACL.getDefaultTenant("unknown server name"));
        assertSame(targetUserWithACLTenant, newTargetUserWithACL.getDefaultTenant("ARCHIVE"));
    }
}
