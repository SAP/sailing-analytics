package com.sap.sailing.gwt.ui.shared.courseCreation;

import java.util.ArrayList;
import java.util.UUID;

import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sse.common.Color;
import com.sap.sse.common.Util;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.NamedDTO;
import com.sap.sse.security.shared.dto.OwnershipDTO;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.shared.dto.SecurityInformationDTO;

public class MarkPropertiesDTO extends NamedDTO implements SecuredDTO {
    private static final long serialVersionUID = 8386491876862591638L;

    // not using iterable because of GWT serialization
    private ArrayList<String> tags = new ArrayList<>();

    private UUID uuid;
    private String positioningType;
    private CommonMarkPropertiesDTO commonMarkProperties;
    private SecurityInformationDTO securityInformation = new SecurityInformationDTO();

    public MarkPropertiesDTO() {
        super("");
        commonMarkProperties = new CommonMarkPropertiesDTO();
    }

    public MarkPropertiesDTO(UUID uuid, String name, Iterable<String> tags, String shortName, Color color, String shape,
            String pattern, MarkType type, String positioningType) {
        super(name);
        this.uuid = uuid;
        Util.addAll(tags, this.tags);
        commonMarkProperties = new CommonMarkPropertiesDTO(name, shortName, color, shape, pattern, type);
        this.positioningType = positioningType;
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
        return SecuredDomainType.MARK_PROPERTIES;
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

    public Iterable<String> getTags() {
        return tags;
    }

    public CommonMarkPropertiesDTO getCommonMarkProperties() {
        return commonMarkProperties;
    }

    public String getPositioningType() {
        return positioningType;
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
        if (!(obj instanceof MarkPropertiesDTO)) {
            return false;
        }
        final MarkPropertiesDTO other = (MarkPropertiesDTO) obj;
        if (uuid == null || other.uuid == null) {
            return false;
        }
        return uuid.equals(other.uuid);
    }
}
