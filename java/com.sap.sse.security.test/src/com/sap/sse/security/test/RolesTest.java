package com.sap.sse.security.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sse.security.shared.dto.RoleDTO;
import com.sap.sse.security.shared.dto.StrippedRoleDefinitionDTO;
import com.sap.sse.security.shared.dto.StrippedUserDTO;
import com.sap.sse.security.shared.dto.StrippedUserGroupDTO;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.UserGroupImpl;

public class RolesTest {
    @Test
    public void testRoleToString() {
        final StrippedRoleDefinitionDTO roleDefinition = new StrippedRoleDefinitionDTO(UUID.randomUUID(), "role",
                new ArrayList<>());
        final Role role = new Role(roleDefinition, true);
        final UserGroupImpl tenant = new UserGroupImpl(UUID.randomUUID(), "tenant");
        final StrippedUserGroupDTO tenantDTO = new StrippedUserGroupDTO(UUID.randomUUID(), "tenant");
        final StrippedUserDTO user = new StrippedUserDTO("user");
        assertEquals("role", role.toString());
        final Role role2 = new Role(roleDefinition, tenant, /* user qualification */ null, true);
        assertEquals("role:tenant", role2.toString());
        final RoleDTO role3 = new RoleDTO(roleDefinition, tenantDTO, user, true);
        assertEquals("role:tenant:user", role3.toString());
        final RoleDTO role4 = new RoleDTO(roleDefinition, /* tenant qualification */ null, user, true);
        assertEquals("role::user", role4.toString());
    }
}
