package com.sap.sse.security.shared;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Grants and revokes permissions to a set of actions for an object identified by an ID provided as String on a
 * per-{@link UserGroup} basis. This way, there should usually be at most one instance of this type defined for one
 * object to which access is controlled. The sets of actions are keyed by the {@link UserGroup} to which they are
 * granted/revoked. An action would, e.g., be something like "UPDATE" in the permission
 * EVENT:UPDATE:84730-74837-47384-ab987f9. Note that nothing but the action is required because the ACL pertains to a
 * single object such that the type (e.g., "EVENT") as well as the object ID as described by
 * {@link #getIdOfAccessControlledObjectAsString()} are known and don't need to and make no sense to be specified.
 * <p>
 * 
 * It is allowed to use the {@code "*"} wildcard as an action string, granting access to all actions on the object
 * concerned. The same logic for permission implication as on {@link WildcardPermission} is applied when matching
 * a particular action passed to the {@link #hasPermission(SecurityUser, String, Iterable)} method against the
 * actions allowed by this ACL.<p>
 * 
 * The actions to be permitted or forbidden are provided as strings. To forbid a action, the action string is prefixed
 * with an exclamation mark '!'.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface AccessControlList extends Serializable {
    PermissionChecker.PermissionState hasPermission(SecurityUser user, String action, Iterable<? extends UserGroup> groupsOfWhichUserIsMember);

    String getIdOfAccessControlledObjectAsString();

    String getDisplayNameOfAccessControlledObject();

    /**
     * @return allowed actions are represented simply as strings and may contain the wildcard string {@code "*"} to
     * represent "all actions;" action strings starting with an exclamation mark {@code '!'} represent actions
     * the key user group is denied.
     */
    Map<UserGroup, Set<String>> getActionsByUserGroup();

    /**
     * @param actionToAllow
     *            the action to be permitted. The wildcard string {@code "*"} can be used to grant permission for all
     *            possible actions to the {@code userGroup}. Prefixing the action by an exclamation mark character
     *            {@code '!'} denies the action that follows. Multiple leading exclamation marks toggle accordingly.
     * @return {@code true} if the permission was added; {@code false} if the permission was already in this ACL and
     *         therefore didn't need to be added
     * @see #denyPermission(UserGroup, String)
     */
    boolean addPermission(UserGroup userGroup, String actionToAllow);
   

    /**
     * @param actionToDeny
     *            the action to be denied. The wildcard string {@code "*"} can be used to deny permission for all
     *            possible actions for the {@code userGroup}. Prefixing the action by an exclamation mark character
     *            {@code '!'} instead allows the action that follows. Multiple leading exclamation marks toggle
     *            accordingly.
     * @return {@code true} if the denial was added; {@code false} if the denial was already in this ACL and therefore
     *         didn't need to be added
     * @see #addPermission(UserGroup, String)
     */
    boolean denyPermission(UserGroup userGroup, String actionToDeny);

    /**
     * Removes a permission denial from those permissions denied for the user group. If the action starts with an {@code "!"}
     * exclamation mark, the exclamation mark is stripped, and {@link #removePermission(UserGroup, String)} is invoked with
     * the remaining string.
     * 
     * @return {@code true} if the permission was removed; {@code false} if the permission was not in this ACL and
     *         therefore didn't need to be removed
     */
    boolean removeDenial(UserGroup userGroup, String substring);

    /**
     * Removes a permission from those permissions granted to the user group. If the action starts with an {@code "!"}
     * exclamation mark, the exclamation mark is stripped, and {@link #removeDenial(UserGroup, String)} is invoked with
     * the remaining string.
     * 
     * @return {@code true} if the permission was removed; {@code false} if the permission was not in this ACL and
     *         therefore didn't need to be removed
     */
    boolean removePermission(UserGroup userGroup, String action);

    void setPermissions(UserGroup userGroup, Set<String> actions);

}