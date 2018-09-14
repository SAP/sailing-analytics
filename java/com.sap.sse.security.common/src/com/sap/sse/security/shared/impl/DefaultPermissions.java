package com.sap.sse.security.shared.impl;

import com.sap.sse.security.shared.WildcardPermission;

public enum DefaultPermissions implements com.sap.sse.security.shared.Permission {
    // back-end permissions
    USER,
    ;
    
    // TODO once we can use Java8 here, move this up into a "default" method on the Permission interface
    @Override
    public String getStringPermission(com.sap.sse.security.shared.Permission.Mode... modes) {
        final String result;
        if (modes==null || modes.length==0) {
            result = name();
        } else {
            final StringBuilder modesString = new StringBuilder();
            boolean first = true;
            for (com.sap.sse.security.shared.Permission.Mode mode : modes) {
                if (first) {
                    first = false;
                } else {
                    modesString.append(',');
                }
                modesString.append(mode.getStringPermission());
            }
            result = name()+":"+modesString.toString();
        }
        return result;
    }

    @Override
    public WildcardPermission getPermission(com.sap.sse.security.shared.Permission.Mode... modes) {
        return new WildcardPermission(getStringPermission(modes), /* case sensitive */ true);
    }

    // TODO once we can use Java8 here, move this up into a "default" method on the Permission interface
    @Override
    public String getStringPermissionForObjects(com.sap.sse.security.shared.Permission.Mode mode, String... objectIdentifiers) {
        final WildcardPermissionEncoder permissionEncoder = new WildcardPermissionEncoder();
        final StringBuilder result = new StringBuilder(getStringPermission(mode));
        if (objectIdentifiers!=null && objectIdentifiers.length>0) {
            result.append(':');
            boolean first = true;
            for (String objectIdentifier : objectIdentifiers) {
                if (first) {
                    first = false;
                } else {
                    result.append(',');
                }
                result.append(permissionEncoder.encodeAsPermissionPart(getQualifiedObjectIdentifier(objectIdentifier)));
            }
        }
        return result.toString();
    }
    
    @Override
    public String getQualifiedObjectIdentifier(String objectIdentifier) {
        return name()+QUALIFIER_SEPARATOR+objectIdentifier;
    }

    @Override
    public WildcardPermission getPermissionForObjects(com.sap.sse.security.shared.Permission.Mode mode, String... objectIdentifiers) {
        return new WildcardPermission(getStringPermissionForObjects(mode, objectIdentifiers), /* case sensitive */ true);
    }
}
