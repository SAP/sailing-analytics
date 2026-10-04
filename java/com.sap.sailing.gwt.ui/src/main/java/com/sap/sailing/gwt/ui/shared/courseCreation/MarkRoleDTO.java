package com.sap.sailing.gwt.ui.shared.courseCreation;

import java.util.UUID;

import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.NamedDTO;
import com.sap.sse.security.shared.dto.OwnershipDTO;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.shared.dto.SecurityInformationDTO;

public class MarkRoleDTO extends NamedDTO implements SecuredDTO {

    private static final long serialVersionUID = -5758518891989155229L;

    private UUID uuid;
    private String shortName;
    private SecurityInformationDTO securityInformation = new SecurityInformationDTO();

    public MarkRoleDTO() {
        super("");
    }

    public MarkRoleDTO(final UUID uuid, final String name, String shortName) {
        super(name);
        this.uuid = uuid;
        this.shortName = shortName;
    }

    public UUID getUuid() {
        return uuid;
    }
    
    public String getShortName() {
        return shortName;
    }

    public TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return getTypeRelativeObjectIdentifier(getUuid().toString());
    }

    public static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(String dbId) {
        return new TypeRelativeObjectIdentifier(dbId);
    }

    @Override
    public QualifiedObjectIdentifier getIdentifier() {
        return getPermissionType().getQualifiedObjectIdentifier(getTypeRelativeObjectIdentifier());
    }

    @Override
    public HasPermissions getPermissionType() {
        return SecuredDomainType.MARK_ROLE;
    }

    @Override
    public AccessControlListDTO getAccessControlList() {
        return securityInformation.getAccessControlList();
    }

    @Override
    public OwnershipDTO getOwnership() {
        return securityInformation.getOwnership();
    }

    @Override
    public void setAccessControlList(AccessControlListDTO createAccessControlListDTO) {
        securityInformation.setAccessControlList(createAccessControlListDTO);
    }

    @Override
    public void setOwnership(OwnershipDTO createOwnershipDTO) {
        securityInformation.setOwnership(createOwnershipDTO);
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MarkRoleDTO)) {
            return false;
        }
        final MarkRoleDTO other = (MarkRoleDTO) obj;
        if (uuid == null || other.uuid == null) {
            return false;
        }
        return uuid.equals(other.uuid);
    }

}
