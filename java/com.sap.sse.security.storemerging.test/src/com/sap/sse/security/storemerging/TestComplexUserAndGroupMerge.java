package com.sap.sse.security.storemerging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;

public class TestComplexUserAndGroupMerge extends AbstractStoreMergeTest {
    @BeforeEach
    public void setUp() throws IOException, UserStoreManagementException {
        setUp("source_TestComplexUserAndGroupMerge", "target_TestComplexUserAndGroupMerge");
    }
    
    @Test
    public void testImportFromSource1ToTarget1() throws UserStoreManagementException {
        assertNotNull(targetUserStore.getUserByName("admin"));
        assertNotNull(targetUserStore.getUserByName("<all>"));
        assertNotNull(targetUserStore.getUserByName("uhl"));
        assertNotNull(targetUserStore.getUserByName("uhl2"));
        assertNotNull(targetUserStore.getUserByName("axel.uhl"));
        assertNotNull(targetUserStore.getUserByName("axel"));
        assertNotNull(targetUserStore.getUserByName("YCaT"));
        assertNotNull(targetUserStore.getUserByName("YCaT-member1"));
        assertNotNull(targetUserStore.getUserByName("YCaT-member2"));
        final User uhl5InTarget = targetUserStore.getUserByName("uhl5");
        assertNotNull(uhl5InTarget); // an equal-named user also exists in source, but with different e-mail
        final User uhl3InTargetUserStore = targetUserStore.getUserByName("uhl3");
        assertNotNull(uhl3InTargetUserStore);
        assertNull(targetUserStore.getUserByName("uhl4"));
        assertNull(targetUserStore.getUserGroupByName("test-server"));
        assertEquals(1, Util.size(targetUserStore.getUserGroupByName("uhl-tenant").getUsers()));
        assertSame(targetUserStore.getUserByName("uhl"), targetUserStore.getUserGroupByName("uhl-tenant").getUsers().iterator().next());
        final Pair<UserStore, AccessControlStore> sourceStores = merger.importStores(causallyConsistentSessionForSource, cfgForSource, sourceService, defaultCreationGroupNameForSource);
        final UserStore sourceUserStore = sourceStores.getA();
        // Should we need the source access control store, here it is:
        // final AccessControlStore sourceAccessControlStore = sourceStores.getB();
        assertNotNull(sourceUserStore.getUserByName("admin"));
        assertNotNull(sourceUserStore.getUserByName("<all>"));
        assertNotNull(sourceUserStore.getUserByName("uhl"));
        assertNotNull(sourceUserStore.getUserByName("uhl3"));
        assertEquals(targetUserStore.getUserByName("uhl3").getEmail(), sourceUserStore.getUserByName("uhl3").getEmail());
        assertNotNull(sourceUserStore.getUserByName("axel.uhl"));
        assertNotNull(sourceUserStore.getUserByName("uhl4"));
        assertNotNull(sourceUserStore.getUserByName("uhl5")); // an equal-named user also exists in target, but with different e-mail
        assertTrue(StreamSupport
                .stream(sourceUserStore.getUserByName("uhl5").getRoles().spliterator(), /* parallel */ false)
                .filter(r -> r.getName().equals("user") && r.getQualifiedForTenant() != null &&
                r.getQualifiedForTenant().getName().equals("uhl4-tenant"))
                .findAny().isPresent());
        assertNotNull(sourceUserStore.getUserGroupByName("test-server"));
        // check result in target user store:
        assertSame(uhl3InTargetUserStore, targetUserStore.getUserByName("uhl3")); // merged, so no new user in this case
        assertNotNull(targetUserStore.getUserByName("uhl4")); // in particular new user uhl4 is expected to be present now
        // the uhl-tenant group is expected to have been merged: the uhl4 user is part of that group in source1
        assertEquals(3, Util.size(targetUserStore.getUserGroupByName("uhl-tenant").getUsers()));
        assertTrue(Util.contains(targetUserStore.getUserGroupByName("uhl-tenant").getUsers(), targetUserStore.getUserByName("uhl")));
        assertTrue(Util.contains(targetUserStore.getUserGroupByName("uhl-tenant").getUsers(), targetUserStore.getUserByName("uhl3")));
        final UserGroup testServerGroupInTarget = targetUserStore.getUserGroup(UUID.fromString("fbfc1d06-ef0c-40c7-a056-355c68a31162"));
        assertSame(testServerGroupInTarget, targetAccessControlStore
                .getOwnership(testServerGroupInTarget.getIdentifier()).getAnnotation().getTenantOwner());
        final User uhl4InTarget = targetUserStore.getUserByName("uhl4");
        assertTrue(Util.contains(targetUserStore.getUserGroupByName("uhl-tenant").getUsers(), uhl4InTarget));
        assertSame(uhl4InTarget, targetAccessControlStore.getOwnership(uhl4InTarget.getIdentifier()).getAnnotation().getUserOwner());
        final UserGroup testServerGroupInTargetAfterMerge = targetUserStore.getUserGroupByName("test-server");
        assertNotNull(testServerGroupInTargetAfterMerge);
        final Map<RoleDefinition, Boolean> rolesOnTestServerGroup = testServerGroupInTargetAfterMerge.getRoleDefinitionMap();
        final Optional<RoleDefinition> sailingViewerRoleInTarget = StreamSupport.stream(targetUserStore.getRoleDefinitions().spliterator(), /* parallel */ false).
                filter(rd->rd.getName().equals("sailing_viewer")).findAny();
        assertTrue(sailingViewerRoleInTarget.isPresent());
        assertTrue(rolesOnTestServerGroup.containsKey(sailingViewerRoleInTarget.get()));
        assertTrue(rolesOnTestServerGroup.get(sailingViewerRoleInTarget.get()));
        assertSame(uhl5InTarget, targetUserStore.getUserByName("uhl5")); // still the same
        // Now ensure that no merge took place, particularly by validating that no "user" role with uhl4-tenant as group
        // qualification was merged
        assertFalse(StreamSupport
                .stream(targetUserStore.getUserByName("uhl5").getRoles().spliterator(), /* parallel */ false)
                .filter(r -> r.getName().equals("user") && r.getQualifiedForTenant() != null &&
                r.getQualifiedForTenant().getName().equals("uhl4-tenant"))
                .findAny().isPresent());
    }
}
