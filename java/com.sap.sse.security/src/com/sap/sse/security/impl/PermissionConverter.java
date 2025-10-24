package com.sap.sse.security.impl;

import java.util.List;
import java.util.Set;

import org.apache.shiro.authz.Permission;

import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.PermissionToObjectIdConverter;

/**
 * Can convert Shiro {@link Permission} objects into {@link WildcardPermission} objects. With that and the help of a
 * {@link PermissionToObjectIdConverter} it can extract an object ID as {@link String} from either such permission which
 * can then be used to parameterize the {@link AccessControlStore}, e.g., to look up ownership and ACL information for
 * an object for which permissions are requested.
 * <p>
 * 
 * Note that the Shiro permissions are all lowercase. All parts of a
 * {@link org.apache.shiro.authz.permission.WildcardPermission} will always be mapped to lowercase before any further
 * processing. Keep this in mind when passing object IDs through permission parts.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class PermissionConverter {
    private final PermissionToObjectIdConverter poc = new PermissionToObjectIdConverter();
    
    public WildcardPermission getWildcardPermission(Permission permission) {
        final WildcardPermission result;
        result = new WildcardPermission(permission.toString());
        return result;
    }

    /**
     * Splits the permission along ":" occurrences
     */
    public List<Set<String>> getPermissionParts(WildcardPermission permission) {
        return permission.getParts();
    }
    
    /**
     * From the permission type (first element) and the comma-separated list of relative object IDs in the third element
     * assembles IDs as strings that can be used, e.g., to look up ACLs or ownerships for the object for which the
     * {@code permission} is requested.
     */
    public Iterable<String> getObjectIdsAsString(WildcardPermission permission) {
        return poc.getObjectIdsAsString(permission);
    }
}
