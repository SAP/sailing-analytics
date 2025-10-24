package com.sap.sse.security.shared.dto;

import com.sap.sse.security.shared.AbstractOwnership;

public class OwnershipDTO extends AbstractOwnership<StrippedUserGroupDTO, StrippedUserDTO> {
    private static final long serialVersionUID = -6379054499434958440L;

    @Deprecated // for GWT serialization only
    private OwnershipDTO() {
        super(null, null);
    }

    public OwnershipDTO(StrippedUserDTO userOwner, StrippedUserGroupDTO tenantOwner) {
        super(userOwner, tenantOwner);
    }
}
