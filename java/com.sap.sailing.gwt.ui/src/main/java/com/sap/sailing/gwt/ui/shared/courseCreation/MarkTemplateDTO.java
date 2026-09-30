package com.sap.sailing.gwt.ui.shared.courseCreation;

import java.util.UUID;

import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sse.common.Color;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.NamedDTO;
import com.sap.sse.security.shared.dto.OwnershipDTO;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.shared.dto.SecurityInformationDTO;

public class MarkTemplateDTO extends NamedDTO implements SecuredDTO {
    private static final long serialVersionUID = -9092124519699246140L;

    private UUID uuid;
    private CommonMarkPropertiesDTO commonMarkProperties;

    private SecurityInformationDTO securityInformation = new SecurityInformationDTO();

    public MarkTemplateDTO() {
        super("");
        commonMarkProperties = new CommonMarkPropertiesDTO();
    }

    public MarkTemplateDTO(UUID uuid, String name, String shortName, Color color, String shape, String pattern,
            MarkType type) {
        super(name);
        this.uuid = uuid;
        commonMarkProperties = new CommonMarkPropertiesDTO(name, shortName, color, shape, pattern, type);
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
    public QualifiedObjectIdentifier getIdentifier() {
        return getPermissionType().getQualifiedObjectIdentifier(getTypeRelativeObjectIdentifier());
    }

    @Override
    public HasPermissions getPermissionType() {
        return SecuredDomainType.MARK_TEMPLATE;
    }

    public TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return getTypeRelativeObjectIdentifier(getUuid().toString());
    }

    public static TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier(String dbId) {
        return new TypeRelativeObjectIdentifier(dbId);
    }

    public UUID getUuid() {
        return uuid;
    }

    public CommonMarkPropertiesDTO getCommonMarkProperties() {
        return commonMarkProperties;
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
        if (!(obj instanceof MarkTemplateDTO)) {
            return false;
        }
        final MarkTemplateDTO other = (MarkTemplateDTO) obj;
        if (uuid == null || other.uuid == null) {
            return false;
        }
        return uuid.equals(other.uuid);
    }

}
