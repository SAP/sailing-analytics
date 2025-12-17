package com.sap.sse.security.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.impl.TimedLockImpl;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.UserStoreManagementException;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;

public class RoleDefinitionsTest {
    private static final String TEST_ROLE = "testRole";
    private final UserStore userStore;
    private final UUID testRoleUUID = UUID.randomUUID();
    private final UUID testGroupUUID = UUID.randomUUID();
    private final String groupName = "abc_Group";
    private final String username = "abc";
    private final String email = "e@mail.com";
    private User user;
    private RoleDefinition roleDefinition;
    private UserGroup userGroup;

    public RoleDefinitionsTest() throws UserStoreManagementException {
        userStore = new UserStoreImpl("TestDefaultTenant");
    }

    @BeforeEach
    public void doBefore() throws UserStoreManagementException {
        userStore.clear();
        user = userStore.createUser(username, email, new TimedLockImpl());
        roleDefinition = userStore.createRoleDefinition(testRoleUUID, TEST_ROLE, Collections.emptySet());
        userGroup = userStore.createUserGroup(testGroupUUID, groupName);
    }
    
    private void addRoleToUser(User user, RoleDefinition role) throws UserManagementException {
        userStore.addRoleForUser(user.getName(), new Role(role, true));
    }
    
    private void addRoleToUserGroup(UserGroup userGroup, RoleDefinition role) throws UserManagementException {
        userGroup.put(role, true);
        userStore.updateUserGroup(userGroup);
    }
        

    @Test
    public void ensureRoleDefinitionRelationsAreEstablishedOnCreation() throws UserStoreManagementException {
        addRoleToUser(user, roleDefinition);
        addRoleToUserGroup(userGroup, roleDefinition);
        boolean roleDefinitionPresent = false;
        for (Role role : userStore.getRolesFromUser(username)) {
            if (role.getRoleDefinition().equals(roleDefinition)) {
                roleDefinitionPresent = true;
            }

        }
        assertTrue(roleDefinitionPresent);
        assertTrue(userGroup.isRoleAssociated(roleDefinition));
    }

    @Test
    public void testRoleDefinitionDeletion() throws UserStoreManagementException {
        userStore.removeRoleDefinition(roleDefinition);
        assertTrue(userStore.getRoleDefinition(roleDefinition.getId()) == null);
    }

    @Test
    public void ensureRoleDefinitionRelationsArePrunedOnDeletion() throws UserManagementException {
        addRoleToUser(user, roleDefinition);
        addRoleToUserGroup(userGroup,roleDefinition);
        userStore.removeRoleDefinition(roleDefinition);
        assertTrue(user.getRoles().iterator().hasNext() == false);
        Boolean roleAssociation = userGroup.getRoleAssociation(roleDefinition);
        assertTrue(roleAssociation == null);
    }
}
