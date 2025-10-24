package com.sap.sse.security.storemerging;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;

public class TestExceptionWhenGroupDroppedButPermissionWasDeniedInACL extends AbstractStoreMergeTest {
    @BeforeEach
    public void setUp() throws IOException, UserStoreManagementException {
        setUp("source_TestExceptionWhenGroupDroppedButPermissionWasDeniedInACL", "target_TestExceptionWhenGroupDroppedButPermissionWasDeniedInACL");
    }
    
    @Test
    public void testExceptionWhenGroupDroppedButPermissionWasDeniedInACL() throws UserStoreManagementException {
        final String NAME_OF_USER_WITH_ACL = "aaa";
        final String NAME_OF_GROUP_TO_BE_DROPPED = "GroupToBeDropped";
        // *********** assertions against unmodified target ***********
        final User targetAaa = targetUserStore.getUserByName(NAME_OF_USER_WITH_ACL);
        assertNull(targetAaa);
        final UserGroup targetAaaTenant = targetUserStore.getUserGroupByName(NAME_OF_USER_WITH_ACL+SecurityService.TENANT_SUFFIX);
        assertNull(targetAaaTenant);
        final UserGroup targetGroupToBeDropped = targetUserStore.getUserGroupByName(NAME_OF_GROUP_TO_BE_DROPPED);
        assertNotNull(targetGroupToBeDropped);
        // *********** reading source stores ***********
        final Pair<UserStore, AccessControlStore> sourceStores = readSourceStores();
        final UserStore sourceUserStore = sourceStores.getA();
        final AccessControlStore sourceAccessControlStore = sourceStores.getB();
        // *********** assertions against unmodified source ***********
        final UserGroup sourceGroupToBeDropped = sourceUserStore.getUserGroupByName(NAME_OF_GROUP_TO_BE_DROPPED);
        assertNotSame(sourceGroupToBeDropped, targetGroupToBeDropped);
        assertNotEquals(sourceGroupToBeDropped.getId(), targetGroupToBeDropped.getId());
        final User sourceAaa = sourceUserStore.getUserByName(NAME_OF_USER_WITH_ACL);
        assertNotNull(sourceAaa);
        final AccessControlListAnnotation sourceAaaACL = sourceAccessControlStore.getAccessControlList(sourceAaa.getIdentifier());
        assertNotNull(sourceAaaACL);
        // we expect the source user "aaa" to have an ACL on it that disallows READ for GroupToBeDropped:
        assertTrue(sourceAaaACL.getAnnotation().getActionsByUserGroup().get(sourceGroupToBeDropped).contains("!READ"));
        // *********** merge ***********
        try {
            mergeSourceIntoTarget(sourceUserStore, sourceAccessControlStore);
            fail("Expected an IllegalStateException to be thrown because a source ACL denies a permission for a group that was dropped");
        } catch (IllegalStateException e) {
            // this is expected
        }
        // *********** assertions for merge result ***********
    }
}
