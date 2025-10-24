package com.sap.sse.security.storemerging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;

public class TestOwnershipImport extends AbstractStoreMergeTest {
    @BeforeEach
    public void setUp() throws IOException, UserStoreManagementException {
        setUp("source_TestOwnershipImport", "target_TestOwnershipImport");
    }
    
    @Test
    public void testTestOwnershipImport() throws UserStoreManagementException {
        final String NAME_OF_USER_TO_BE_DROPPED = "UserToBeDropped";
        final String NAME_OF_GROUP_TO_BE_DROPPED = "GroupToBeDropped";
        final String NAME_OF_GROUP_OWNED_BY_DROPPED_GROUP_BUT_KEPT_USER = "OwnedByDroppedGroupButKeptUser";
        final String NAME_OF_GROUP_OWNED_BY_DROPPED_GROUP_AND_DROPPED_USER = "OwnedByDroppedGroupAndDroppedUser";
        final String NAME_OF_GROUP_OWNED_BY_KEPT_GROUP_AND_KEPT_USER = "OwnedByKeptGroupAndKeptUser";
        final String NAME_OF_GROUP_OWNED_BY_KEPT_GROUP_AND_DROPPED_USER = "OwnedByKeptGroupAndDroppedUser";
        // *********** assertions against unmodified target ***********
        final User targetUserToBeDropped = targetUserStore.getUserByName(NAME_OF_USER_TO_BE_DROPPED);
        assertNotNull(targetUserToBeDropped);
        final UserGroup targetGroupToBeDropped = targetUserStore.getUserGroupByName(NAME_OF_GROUP_TO_BE_DROPPED);
        assertNotNull(targetGroupToBeDropped);
        final User targetAdmin = targetUserStore.getUserByName("admin");
        assertNotNull(targetAdmin);
        final Ownership targetAdminOwnership = targetAccessControlStore.getOwnership(targetAdmin.getIdentifier()).getAnnotation();
        assertNotNull(targetAdminOwnership);
        assertNull(targetAdminOwnership.getTenantOwner()); // target admin has no group owner...
        assertSame(targetAdmin, targetAdminOwnership.getUserOwner()); // ...and owns itself
        // *********** reading source stores ***********
        final Pair<UserStore, AccessControlStore> sourceStores = readSourceStores();
        final UserStore sourceUserStore = sourceStores.getA();
        final AccessControlStore sourceAccessControlStore = sourceStores.getB();
        // *********** assertions against unmodified source ***********
        final UserGroup sourceGroupToBeDropped = sourceUserStore.getUserGroupByName(NAME_OF_GROUP_TO_BE_DROPPED);
        assertNotNull(sourceGroupToBeDropped);
        assertNotSame(sourceGroupToBeDropped, targetGroupToBeDropped);
        assertNotEquals(sourceGroupToBeDropped.getId(), targetGroupToBeDropped.getId());
        final User sourceUserToBeDropped = sourceUserStore.getUserByName(NAME_OF_USER_TO_BE_DROPPED);
        assertNotNull(sourceUserToBeDropped);
        assertNotEquals(sourceUserToBeDropped.getEmail(), targetUserToBeDropped.getEmail());
        final User sourceAdmin = sourceUserStore.getUserByName("admin");
        assertNotNull(sourceAdmin);
        final Ownership sourceAdminOwnership = sourceAccessControlStore.getOwnership(sourceAdmin.getIdentifier()).getAnnotation();
        assertNotNull(sourceAdminOwnership);
        // source admin has admin-tenant as group owner...
        assertSame(sourceUserStore.getUserGroupByName("admin"+SecurityService.TENANT_SUFFIX), sourceAdminOwnership.getTenantOwner());
        assertSame(sourceAdmin, sourceAdminOwnership.getUserOwner()); // ...and owns itself
        final UserGroup sourceOwnedByDroppedGroupButKeptUser = sourceUserStore.getUserGroupByName(NAME_OF_GROUP_OWNED_BY_DROPPED_GROUP_BUT_KEPT_USER);
        final UserGroup sourceOwnedByDroppedGroupAndDroppedUser = sourceUserStore.getUserGroupByName(NAME_OF_GROUP_OWNED_BY_DROPPED_GROUP_AND_DROPPED_USER);
        final UserGroup sourceOwnedByKeptGroupAndKeptUser = sourceUserStore.getUserGroupByName(NAME_OF_GROUP_OWNED_BY_KEPT_GROUP_AND_KEPT_USER);
        final UserGroup sourceOwnedByKeptGroupAndDroppedUser = sourceUserStore.getUserGroupByName(NAME_OF_GROUP_OWNED_BY_KEPT_GROUP_AND_DROPPED_USER);
        assertNotNull(sourceOwnedByDroppedGroupButKeptUser);
        assertOwnership(sourceOwnedByDroppedGroupButKeptUser, NAME_OF_GROUP_TO_BE_DROPPED, "admin", sourceUserStore, sourceAccessControlStore);
        assertNotNull(sourceOwnedByDroppedGroupAndDroppedUser);
        assertOwnership(sourceOwnedByDroppedGroupAndDroppedUser, NAME_OF_GROUP_TO_BE_DROPPED, NAME_OF_USER_TO_BE_DROPPED, sourceUserStore, sourceAccessControlStore);
        assertNotNull(sourceOwnedByKeptGroupAndKeptUser);
        assertOwnership(sourceOwnedByKeptGroupAndKeptUser, "admin-tenant", "admin", sourceUserStore, sourceAccessControlStore);
        assertNotNull(sourceOwnedByKeptGroupAndDroppedUser);
        assertOwnership(sourceOwnedByKeptGroupAndDroppedUser, "admin-tenant", NAME_OF_USER_TO_BE_DROPPED, sourceUserStore, sourceAccessControlStore);
        // *********** merge ***********
        mergeSourceIntoTarget(sourceUserStore, sourceAccessControlStore);
        // *********** assertions for merge result ***********
        // expecting the admin ownership to remain unchanged: the source's group ownership (admin-tenant) is
        // expected to NOT be merged into the existing ownership in target:
        assertNull(targetAccessControlStore.getOwnership(targetAdmin.getIdentifier()).getAnnotation().getTenantOwner());
        assertSame(targetUserToBeDropped, targetUserStore.getUserByName(NAME_OF_USER_TO_BE_DROPPED));
        assertSame(targetGroupToBeDropped, targetUserStore.getUserGroupByName(NAME_OF_GROUP_TO_BE_DROPPED));
        // Now check that all groups and all their ownerships were imported correctly:
        final UserGroup targetOwnedByDroppedGroupButKeptUser = targetUserStore.getUserGroupByName(NAME_OF_GROUP_OWNED_BY_DROPPED_GROUP_BUT_KEPT_USER);
        final UserGroup targetOwnedByDroppedGroupAndDroppedUser = targetUserStore.getUserGroupByName(NAME_OF_GROUP_OWNED_BY_DROPPED_GROUP_AND_DROPPED_USER);
        final UserGroup targetOwnedByKeptGroupAndKeptUser = targetUserStore.getUserGroupByName(NAME_OF_GROUP_OWNED_BY_KEPT_GROUP_AND_KEPT_USER);
        final UserGroup targetOwnedByKeptGroupAndDroppedUser = targetUserStore.getUserGroupByName(NAME_OF_GROUP_OWNED_BY_KEPT_GROUP_AND_DROPPED_USER);
        assertNotNull(targetOwnedByDroppedGroupButKeptUser);
        assertOwnership(targetOwnedByDroppedGroupButKeptUser, null, "admin", targetUserStore, targetAccessControlStore);
        assertNotNull(targetOwnedByDroppedGroupAndDroppedUser);
        assertOwnership(targetOwnedByDroppedGroupAndDroppedUser, null, null, targetUserStore, targetAccessControlStore);
        assertNotNull(targetOwnedByKeptGroupAndKeptUser);
        assertOwnership(targetOwnedByKeptGroupAndKeptUser, "admin-tenant", "admin", targetUserStore, targetAccessControlStore);
        assertNotNull(targetOwnedByKeptGroupAndDroppedUser);
        assertOwnership(targetOwnedByKeptGroupAndDroppedUser, "admin-tenant", null, targetUserStore, targetAccessControlStore);
    }

    protected void assertOwnership(WithQualifiedObjectIdentifier sourceOwnedByDroppedGroupButKeptUser, String groupName,
            String userName, UserStore userStore, AccessControlStore accessControlStore) {
        final OwnershipAnnotation ownership = accessControlStore.getOwnership(sourceOwnedByDroppedGroupButKeptUser.getIdentifier());
        if (groupName == null && userName == null) {
            assertNull(ownership);
        } else {
            if (groupName == null) {
                assertNull(ownership.getAnnotation().getTenantOwner());
            } else {
                assertEquals(groupName, ownership.getAnnotation().getTenantOwner().getName());
            }
            if (userName == null) {
                assertNull(ownership.getAnnotation().getUserOwner());
            } else {
                assertEquals(userName, ownership.getAnnotation().getUserOwner().getName());
            }
        }
    }
}
