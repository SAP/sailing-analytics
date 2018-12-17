package com.sap.sse.security.shared.dto;

import java.io.Serializable;

import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.Ownership;

/**
 * Data transfer object wrapping security information such as {@link AccessControlList access control list} and
 * {@link Ownership ownership} of and {@link SecuredObject secured object}.
 */
public class SecurityInformationDTO implements SecuredDTO, Serializable {

    private static final long serialVersionUID = -292250850983164293L;

    private AccessControlListDTO accessControlList;
    private OwnershipDTO ownership;
    private HasPermissions permissionType;
    private TypeRelativeObjectIdentifier typeRelativeObjectIdentifier;

    @Override
    public final AccessControlListDTO getAccessControlList() {
        return accessControlList;
    }

    @Override
    public final OwnershipDTO getOwnership() {
        return ownership;
    }

    @Override
    public final void setAccessControlList(final AccessControlListDTO accessControlList) {
        this.accessControlList = accessControlList;
    }

    @Override
    public final void setOwnership(final OwnershipDTO ownership) {
        this.ownership = ownership;
    }

    @Override
    public TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return typeRelativeObjectIdentifier;
    }

    public void setTypeRelativeObjectIdentifier(final TypeRelativeObjectIdentifier typeRelativeObjectIdentifier) {
        this.typeRelativeObjectIdentifier = typeRelativeObjectIdentifier;
   }

    @Override
    public HasPermissions getType() {
        return permissionType;
    }

    public void setType(final HasPermissions permissionType) {
         this.permissionType = permissionType;
    }
}
