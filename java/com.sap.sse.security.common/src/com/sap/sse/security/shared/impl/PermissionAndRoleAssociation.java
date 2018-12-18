package com.sap.sse.security.shared.impl;

import com.sap.sse.security.shared.RoleDefinitionImpl;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;

public class PermissionAndRoleAssociation {
    public static TypeRelativeObjectIdentifier get(Role role, User userWithRole) {
        String ownerTenantString = "null";
        UserGroup ownerTenant = role.getQualifiedForTenant();
        if (ownerTenant != null) {
            ownerTenantString = UserGroupImpl.getTypeRelativeObjectIdentifierAsString(ownerTenant);
        }
        String ownerUserString = "null";
        User ownerUser = role.getQualifiedForUser();
        if (ownerUser != null) {
            ownerUserString = SecurityUserImpl.getTypeRelativeObjectIdentifierAsString(ownerUser);
        }
        String roleDefinitionString = RoleDefinitionImpl.getTypeRelativeObjectIdentifierAsString(role.getRoleDefinition());
        String userWithRoleString = SecurityUserImpl.getTypeRelativeObjectIdentifierAsString(userWithRole);
        TypeRelativeObjectIdentifier associationTypeRelativeId = new TypeRelativeObjectIdentifier(userWithRoleString, roleDefinitionString,
                ownerUserString, ownerTenantString);
        return associationTypeRelativeId;
    }
}

