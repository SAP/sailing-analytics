package com.sap.sse.security.shared;

import com.sap.sse.security.shared.impl.SecuredSecurityTypes;

public class IPAddress implements WithQualifiedObjectIdentifier {
    private static final long serialVersionUID = 8016397230668484898L;
    private final String ipAddress;

    public IPAddress(final String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String getName() {
        return ipAddress;
    }

    @Override
    public QualifiedObjectIdentifier getIdentifier() {
        return getPermissionType().getQualifiedObjectIdentifier(new TypeRelativeObjectIdentifier(ipAddress));
    }

    @Override
    public HasPermissions getPermissionType() {
        return SecuredSecurityTypes.LOCKED_IP;
    }

}
