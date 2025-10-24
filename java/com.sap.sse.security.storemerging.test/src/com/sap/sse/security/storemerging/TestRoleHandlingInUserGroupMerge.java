package com.sap.sse.security.storemerging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.impl.UserGroup;

public class TestRoleHandlingInUserGroupMerge extends AbstractStoreMergeTest {
    @BeforeEach
    public void setUp() throws IOException, UserStoreManagementException {
        setUp("source_TestRoleHandlingInUserGroupMerge", "target_TestRoleHandlingInUserGroupMerge");
    }
    
    @Test
    public void testImportFromSource1ToTarget1() throws UserStoreManagementException {
        final RoleDefinition sailingViewerRoleInTargetStore = StreamSupport.stream(targetUserStore.getRoleDefinitions().spliterator(), /* parallel */ false).
                filter(rd->rd.getName().equals("sailing_viewer")).findAny().get();
        // assertions against unmodified target
        for (final UserGroup userGroup : targetUserStore.getUserGroups()) {
            if (userGroup.getName().contains("Into")) {
                switch (userGroup.getName().split("Into")[1]) {
                case "Without":
                    assertTrue(userGroup.getRoleDefinitionMap().isEmpty());
                    break;
                case "WithForGroup":
                    assertTrue(userGroup.getRoleDefinitionMap().containsKey(sailingViewerRoleInTargetStore));
                    assertFalse(userGroup.getRoleDefinitionMap().get(sailingViewerRoleInTargetStore));
                    break;
                case "WithForAll":
                    assertTrue(userGroup.getRoleDefinitionMap().containsKey(sailingViewerRoleInTargetStore));
                    assertTrue(userGroup.getRoleDefinitionMap().get(sailingViewerRoleInTargetStore));
                    break;
                }
            }
        }
        final Pair<UserStore, AccessControlStore> sourceStores = readSourceStores();
        final UserStore sourceUserStore = sourceStores.getA();
        final AccessControlStore sourceAccessControlStore = sourceStores.getB();
        // assertions against unmodified source
        final RoleDefinition sailingViewerRoleInSourceStore = StreamSupport.stream(sourceUserStore.getRoleDefinitions().spliterator(), /* parallel */ false).
                filter(rd->rd.getName().equals("sailing_viewer")).findAny().get();
        for (final UserGroup userGroup : sourceUserStore.getUserGroups()) {
            if (userGroup.getName().contains("Into")) {
                switch (userGroup.getName().split("Into")[0]) {
                case "Without":
                    assertTrue(userGroup.getRoleDefinitionMap().isEmpty());
                    break;
                case "WithForGroup":
                    assertTrue(userGroup.getRoleDefinitionMap().containsKey(sailingViewerRoleInSourceStore));
                    assertFalse(userGroup.getRoleDefinitionMap().get(sailingViewerRoleInSourceStore));
                    break;
                case "WithForAll":
                    assertTrue(userGroup.getRoleDefinitionMap().containsKey(sailingViewerRoleInSourceStore));
                    assertTrue(userGroup.getRoleDefinitionMap().get(sailingViewerRoleInSourceStore));
                    break;
                }
            }
        }
        mergeSourceIntoTarget(sourceUserStore, sourceAccessControlStore);
        // assertions for merge result
        for (final UserGroup userGroup : targetUserStore.getUserGroups()) {
            if (userGroup.getName().contains("Into")) {
                final String[] sourceAndTarget = userGroup.getName().split("Into");
                // the sailing_viewer role is expected to be present if at least one group had it
                final boolean expectingSailingViewerRolePresent =
                        !sourceAndTarget[0].equals("Without") || !sourceAndTarget[1].equals("Without");
                assertEquals(expectingSailingViewerRolePresent, userGroup.getRoleDefinitionMap().containsKey(sailingViewerRoleInTargetStore));
                if (expectingSailingViewerRolePresent) {
                    // if at least one of them was for all, the merge result is expected to be for all:
                    assertEquals(sourceAndTarget[0].equals("WithForAll") || sourceAndTarget[1].equals("WithForAll"),
                            userGroup.getRoleDefinitionMap().get(sailingViewerRoleInTargetStore));
                }
            }
        }
    }
}
