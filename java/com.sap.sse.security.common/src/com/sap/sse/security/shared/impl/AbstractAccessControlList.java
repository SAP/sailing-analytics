package com.sap.sse.security.shared.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sap.sse.common.Util;
import com.sap.sse.security.shared.PermissionChecker;
import com.sap.sse.security.shared.PermissionChecker.PermissionState;
import com.sap.sse.security.shared.SecurityAccessControlList;
import com.sap.sse.security.shared.SecurityUserGroup;
import com.sap.sse.security.shared.WildcardPermission;

public abstract class AbstractAccessControlList<G extends SecurityUserGroup<?>>
        implements SecurityAccessControlList<G> {
    private static final long serialVersionUID = -8587238587604749862L;

    /**
     * Maps from {@link UserGroupImpl} to the actions allowed for this group on the
     * {@link #idOfAccessControlledObjectAsString object to which this ACL belongs}. The {@link WildcardPermission}
     * objects stored in the value sets represent only the action part, not the type or instance part. The
     * {@link WildcardPermission} abstraction is used for its wildcard implication logic. The
     * {@link #hasPermission(String, Iterable)} method will construct a {@link WildcardPermission} from
     * the action requested, and this permission will then be matched against the permissions in this map's value sets.
     * <p>
     * 
     * Actions allowed for the {@code null} key are considered applicable regardless of the group(s) the user is part
     * of. Those permissions therefore apply even if the user is not part of any group, in particular if it's the
     * "anonymous" user.
     * <p>
     * 
     * Note that no negated actions are part of this map. See also {@link #deniedActionsByUserGroup}.
     */
    private Map<G, Set<WildcardPermission>> allowedActionsByUserGroup;
    
    /**
     * Maps from {@link UserGroupImpl} to the actions denied for this group on the
     * {@link #idOfAccessControlledObjectAsString object to which this ACL belongs}. The {@link WildcardPermission}
     * objects stored in the value sets represent only the action part, not the type or instance part. The
     * {@link WildcardPermission} abstraction is used for its wildcard implication logic. The
     * {@link #hasPermission(String, Iterable)} method will construct a {@link WildcardPermission} from
     * the action requested, and this permission will then be matched against the permissions in this map's value sets.
     * <p>
     * 
     * Actions denied for the {@code null} key are considered denied regardless of the group(s) the user is part of.
     * Those permissions therefore are denied even if the user is not part of any group, in particular if it's the
     * "anonymous" user.
     * <p>
     * 
     * Note that no negated actions are part of this map. See also {@link #allowedActionsByUserGroup}.
     */
    private Map<G, Set<WildcardPermission>> deniedActionsByUserGroup;

    protected AbstractAccessControlList(Map<G, Set<String>> permissionMap) {
        this.allowedActionsByUserGroup = new HashMap<>();
        this.deniedActionsByUserGroup = new HashMap<>();
        for (final Entry<G, Set<String>> permissionMapEntry : permissionMap.entrySet()) {
            setPermissions(permissionMapEntry.getKey(), permissionMapEntry.getValue());
        }
    }

    @Override
    public PermissionChecker.PermissionState hasPermission(String action, Iterable<G> groupsOfWhichUserIsMember) {
        PermissionState result = PermissionState.NONE;
        final WildcardPermission requestedAction = new WildcardPermission(action);
        // special handling for the "null" group key which implies the permissions granted/denied to all users regardless their group memberships:
        if (allowedActionsByUserGroup.containsKey(null) && doesAnyPermissionImplyRequestedAction(allowedActionsByUserGroup.get(null), requestedAction)) {
            result = PermissionState.GRANTED;
        }
        if (deniedActionsByUserGroup.containsKey(null) && doesAnyPermissionImplyRequestedAction(deniedActionsByUserGroup.get(null), requestedAction)) {
            result = PermissionState.REVOKED;
        } else { // no need for further checks if already revoked
            for (final G userGroupOfWhichUserIsMember : groupsOfWhichUserIsMember) {
                if (result == PermissionState.NONE && doesAnyPermissionImplyRequestedAction(
                        allowedActionsByUserGroup.get(userGroupOfWhichUserIsMember), requestedAction)) {
                    result = PermissionState.GRANTED;
                }
                if (doesAnyPermissionImplyRequestedAction(deniedActionsByUserGroup.get(userGroupOfWhichUserIsMember), requestedAction)) {
                    result = PermissionState.REVOKED;
                    break;
                }
            }
        }
        return result;
    }
    
    private boolean doesAnyPermissionImplyRequestedAction(Set<WildcardPermission> permissions, WildcardPermission requestedAction) {
        if (permissions != null) {
            for (final WildcardPermission allowedOrDeniedAction : permissions) {
                if (allowedOrDeniedAction.implies(requestedAction)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public Map<G, Set<String>> getActionsByUserGroup() {
        final Map<G, Set<String>> result = new HashMap<>();
        for (final Entry<G, Set<WildcardPermission>> allowedEntry : allowedActionsByUserGroup.entrySet()) {
            for (final WildcardPermission permission : allowedEntry.getValue()) {
                Util.addToValueSet(result, allowedEntry.getKey(), permission.toString());
            }
        }
        for (final Entry<G, Set<WildcardPermission>> allowedEntry : deniedActionsByUserGroup.entrySet()) {
            for (final WildcardPermission permission : allowedEntry.getValue()) {
                Util.addToValueSet(result, allowedEntry.getKey(), invertAction(permission.toString()));
            }
        }
        return result;
    }
    
    @Override
    public Set<String> getAllowedActions(UserGroup group) {
        return Util.asSet(Util.map(
                allowedActionsByUserGroup.get(group)==null?Collections.emptySet():allowedActionsByUserGroup.get(group),
                        wp->wp.toString()));
    }

    @Override
    public Map<G, Set<WildcardPermission>> getAllowedActions() {
        return Collections.unmodifiableMap(allowedActionsByUserGroup);
    }

    @Override
    public Set<String> getDeniedActions(UserGroup group) {
        return Util.asSet(Util.map(
                deniedActionsByUserGroup.get(group)==null?Collections.emptySet():deniedActionsByUserGroup.get(group),
                        wp->wp.toString()));
    }

    @Override
    public Map<G, Set<WildcardPermission>> getDeniedActions() {
        return Collections.unmodifiableMap(deniedActionsByUserGroup);
    }

    private boolean isDeniedAction(String action) {
        return SecurityAccessControlList.isDeniedAction(action);
    }
    
    /**
     * Removes a leading ! if there is one; otherwise prefixes the action with a !
     */
    private String invertAction(String action) {
        return SecurityAccessControlList.invertAction(action);
    }
    
    private boolean denyPermission(G userGroup, String action) {
        final boolean result;
        if (isDeniedAction(action)) {
            result = addPermission(userGroup, invertAction(action));
        } else {
            result = Util.addToValueSet(deniedActionsByUserGroup, userGroup, new WildcardPermission(action));
        }
        return result;
    }
    
    @Override
    public boolean addPermission(G userGroup, String action) {
        final boolean result;
        if (isDeniedAction(action)) {
            result = denyPermission(userGroup, invertAction(action));
        } else {
            result = Util.addToValueSet(allowedActionsByUserGroup, userGroup, new WildcardPermission(action));
        }
        return result;
    }

    @Override
    public boolean removePermission(G userGroup, String action) {
        final boolean result;
        if (isDeniedAction(action)) {
            result = removeDenial(userGroup, invertAction(action));
        } else {
            result = Util.removeFromValueSet(allowedActionsByUserGroup, userGroup, new WildcardPermission(action));
        }
        return result;
    }

    private boolean removeDenial(G userGroup, String action) {
        final boolean result;
        if (isDeniedAction(action)) {
            result = removeDenial(userGroup, invertAction(action));
        } else {
            result = Util.removeFromValueSet(deniedActionsByUserGroup, userGroup, new WildcardPermission(action));
        }
        return result;
    }

    @Override
    public void setPermissions(G userGroup, Set<String> actions) {
        allowedActionsByUserGroup.remove(userGroup);
        deniedActionsByUserGroup.remove(userGroup);
        for (final String actionAsString : actions) {
            if (isDeniedAction(actionAsString)) {
                denyPermission(userGroup, invertAction(actionAsString));
            } else {
                addPermission(userGroup, actionAsString);
            }
        }
    }

    @Override
    public String toString() {
        return "AccessControlList [actionsByUserGroup=" + getActionsByUserGroup() + "]";
    }

}
