package com.sap.sse.security.shared;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.sap.sse.common.Util;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;

/**
 * Equality ({@link #equals(Object)} and {@link #hashCode()} are based solely on the {@link #getId() ID}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class RoleDefinitionImpl implements RoleDefinition {
    private static final long serialVersionUID = -402472324567793082L;

    private UUID id;
    private String name;
    private Set<WildcardPermission> permissions;

    protected RoleDefinitionImpl() {} // for GWT serialization only

    public RoleDefinitionImpl(UUID id, String name) {
        this(id, name, new HashSet<WildcardPermission>());
    }

    public RoleDefinitionImpl(UUID id, String name, Iterable<WildcardPermission> permissions) {
        this.id = id;
        this.name = name;
        this.permissions = new HashSet<>();
        Util.addAll(permissions, this.permissions);
    }

    public static RoleDefinition create(UUID id, String name, Iterable<WildcardPermission> permissions) {
        final RoleDefinition result;
        if (Util.equalsWithNull(id, AdminRole.getInstance().getId())) {
            result = AdminRole.getInstance();
        } else if (Util.equalsWithNull(id, UserRole.getInstance().getId())) {
            result = UserRole.getInstance();
        } else {
            result = new RoleDefinitionImpl(id, name, permissions);
        }
        return result;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String newName) {
        this.name = newName;
    }

    @Override
    public Set<WildcardPermission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    @Override
    public void setPermissions(Iterable<WildcardPermission> permissions) {
        this.permissions = new HashSet<>();
        Util.addAll(permissions, this.permissions);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RoleDefinitionImpl other = (RoleDefinitionImpl) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return name + " (permissions: " + permissions + ")";
    }

    @Override
    public HasPermissions getType() {
        return SecuredSecurityTypes.ROLE_DEFINITION;
    }

    @Override
    public QualifiedObjectIdentifier getIdentifier() {
        return getType().getQualifiedObjectIdentifier(getTypeRelativeObjectIdentifier());
    }

    public TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return getTypeRelativeObjectIdentifier(this);
    }

    public static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(RoleDefinition roleDefinition) {
        return new TypeRelativeObjectIdentifier(getTypeRelativeObjectIdentifierAsString(roleDefinition));
    }

    public static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(UUID uuid) {
        return new TypeRelativeObjectIdentifier(uuid.toString());
    }

    public static String getTypeRelativeObjectIdentifierAsString(RoleDefinition roleDefinition) {
        return roleDefinition.getId().toString();
    }
}
