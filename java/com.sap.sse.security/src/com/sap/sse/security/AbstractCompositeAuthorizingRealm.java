package com.sap.sse.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.osgi.framework.BundleContext;

import com.sap.sse.security.impl.Activator;
import com.sap.sse.security.impl.PermissionConverter;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.PermissionChecker;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.util.ServiceTrackerFactory;

/**
 * This class implements a realm that combines Access Control Lists, Role Based Permission Modeling and
 * Ownership for users as well as tenants.
 * 
 * This class overrides all the methods of {@link AuthorizingRealm} that call doGetAuthorizationInfo. Since
 * this realm is highly dynamic, doGetAuthorizationInfo cannot be easily implemented and is thus never called.
 * 
 * @author Jonas Dann
 * @author Axel Uhl (D043530)
 */
public abstract class AbstractCompositeAuthorizingRealm extends AuthorizingRealm {
    private static final Logger logger = Logger.getLogger(AbstractCompositeAuthorizingRealm.class.getName());
    private final Future<UserStore> userStore;
    private final Future<AccessControlStore> accessControlStore;

    /**
     * In a non-OSGi test environment, having Shiro instantiate this class with a default constructor makes it difficult
     * to get access to the user store implementation which may live in a bundle that this bundle has no direct access
     * to. Therefore, test cases must set the UserStore implementation by invoking {@link #setTestUserStore} before the
     * default constructor is invoked.
     */
    private static UserStore testUserStore;
    private static AccessControlStore testAccessControlStore;

    public static void setTestStores(UserStore theTestUserStore, AccessControlStore theTestAccessControlStore) {
        testUserStore = theTestUserStore;
        testAccessControlStore = theTestAccessControlStore;
    }

    public AbstractCompositeAuthorizingRealm() {
        super();
        setCachingEnabled(false); // always grab fresh authorization info from the user store
        // we want case-sensitive permission strings:
        setPermissionResolver(wildcardString->new org.apache.shiro.authz.permission.WildcardPermission(wildcardString, /* caseSensitive */ true));
        BundleContext context = Activator.getContext();
        if (context != null) {
            userStore = ServiceTrackerFactory.createServiceFuture(context, UserStore.class);
            accessControlStore = ServiceTrackerFactory.createServiceFuture(context, AccessControlStore.class);
        } else {
            userStore = null;
            accessControlStore = null;
        }
    }

    protected UserStore getUserStore() {
        UserStore result;
        if (testUserStore != null) {
            result = testUserStore;
        } else {
            try {
                result = userStore.get();
            } catch (InterruptedException | ExecutionException e) {
                result = null;
                logger.log(Level.SEVERE, "Error retrieving user store", e);
            }
        }
        return result;
    }
    
    protected AccessControlStore getAccessControlStore() {
        AccessControlStore result;
        if (testAccessControlStore != null) {
            result = testAccessControlStore;
        } else {
            try {
                result = accessControlStore.get();
            } catch (InterruptedException | ExecutionException e) {
                result = null;
                logger.log(Level.SEVERE, "Error retrieving access control list store", e);
            }
        }
        return result;
    }

    @Override
    public boolean isPermitted(PrincipalCollection principals, Permission perm) {
        String username = (String) principals.getPrimaryPrincipal();
        final User user = getUserStore().getUserByName(username);
        final WildcardPermission wildcardPermission = new PermissionConverter().getWildcardPermission(perm);
        List<Set<String>> parts = wildcardPermission.getParts();
        final boolean result;
        if (parts.size() > 2 && !parts.get(2).isEmpty()) {
            boolean allChecksPassed = true;
            // if multiple object IDs are provided as several sub-parts, permission is checked for each of them, and the total
            // permission will be considered granted if the check has succeeded for all objects
            for (final QualifiedObjectIdentifier objectIdentifier : wildcardPermission.getQualifiedObjectIdentifiers()) {
                final OwnershipAnnotation ownership = getAccessControlStore().getOwnership(objectIdentifier);
                final AccessControlListAnnotation acl = getAccessControlStore().getAccessControlList(objectIdentifier);
                allChecksPassed = isPermittedForUser(wildcardPermission, user, ownership, acl);
                if (!allChecksPassed) {
                    break;
                }
            }
            result = allChecksPassed;
        } else {
            result = isPermittedForUser(wildcardPermission, user, /* ownership */ null, /* acl */ null);
        }
        return result;
    }

    private boolean isPermittedForUser(WildcardPermission wildcardPermission, User user, OwnershipAnnotation ownership,
            AccessControlListAnnotation acl) {
        User allUser = getUserStore().getUserByName(SecurityService.ALL_USERNAME);
        return PermissionChecker.isPermitted(wildcardPermission, user, allUser,
                ownership == null ? null : ownership.getAnnotation(), acl == null ? null : acl.getAnnotation());
    }
    
    @Override
    public boolean[] isPermitted(PrincipalCollection principals, List<Permission> permissions) {
        boolean[] result = new boolean[permissions.size()];
        for (int i = 0; i < permissions.size(); i++) {
            result[i] = isPermitted(principals, permissions.get(i));
        }
        return result;
    }

    @Override
    public boolean isPermittedAll(PrincipalCollection principals, Collection<Permission> permissions) {
        if (permissions != null && !permissions.isEmpty()) {
            for (Permission permission : permissions) {
                if (!isPermitted(principals, permission)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void checkPermission(PrincipalCollection principals, Permission permission) throws AuthorizationException {
        if (!isPermitted(principals, permission)) {
            String msg = "User is not permitted [" + permission + "]";
            throw new UnauthorizedException(msg);
        }
    }

    @Override
    public void checkPermissions(PrincipalCollection principals, Collection<Permission> permissions) throws AuthorizationException {
        if (permissions != null && !permissions.isEmpty()) {
            for (Permission permission : permissions) {
                checkPermission(principals, permission);
            }
        }
    }
    
    @Override
    public boolean hasRole(PrincipalCollection principals, String roleName) {
        String user = (String) principals.getPrimaryPrincipal();
        try {
            for (Role role : getUserStore().getRolesFromUser(user)) {
                if (role.getName().equals(roleName)) {
                    return true;
                }
            }
        } catch (UserManagementException e) {
            logger.log(Level.SEVERE, "User " + user + " does not exist.", e);
            return false;
        }
        return false;
    }

    @Override
    public boolean[] hasRoles(PrincipalCollection principals, List<String> roleIdentifiers) {
        boolean[] result = new boolean[roleIdentifiers.size()];
        for (int i = 0; i < roleIdentifiers.size(); i++) {
            result[i] = hasRole(principals, roleIdentifiers.get(i));
        }
        return result;
    }
    
    @Override
    public boolean hasAllRoles(PrincipalCollection principals, Collection<String> roleIdentifiers) {
        if (roleIdentifiers != null && !roleIdentifiers.isEmpty()) {
            for (String role : roleIdentifiers) {
                if (!hasRole(principals, role)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public void checkRole(PrincipalCollection principals, String role) throws AuthorizationException {
        if (!hasRole(principals, role)) {
            String msg = "User does not have role [" + role + "]";
            throw new UnauthorizedException(msg);
        }
    }
    
    @Override
    public void checkRoles(PrincipalCollection principals, Collection<String> roles) throws AuthorizationException {
        if (roles != null && !roles.isEmpty()) {
            for (String role : roles) {
                checkRole(principals, role);
            }
        }
    }
    
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        // As all the public methods of AuthorizingRealm are overridden to not use this, this should never be called.
        throw new UnsupportedOperationException("Call to doGetAuthorizationInfo(PrincipalCollection principals. This should never happen!)");
    }
}
