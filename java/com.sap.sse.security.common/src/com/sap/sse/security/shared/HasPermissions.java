package com.sap.sse.security.shared;

import java.util.Arrays;

import com.sap.sse.common.Named;
import com.sap.sse.common.Util;
import com.sap.sse.security.shared.impl.WildcardPermissionEncoder;

/**
 * Represents the "type" of object on which a permission can be granted. In a typical wildcard permission
 * of the form "A:B:C" this represents the first part. For example, if we may want to describe a permission
 * for updating a leaderboard then this may look like this: <code>LEADERBOARD:UPDATE:KW2017 Laser Int.</code> where
 * <code>LEADERBOARD</code> then is the {@link #name()} of this permission, {@link DefaultActions#UPDATE UPDATE} is
 * the operation mode, and <code>"KW2017 Laser Int."</code> is the object identifier that may not be unique outside
 * of the type qualifier represented by this permission ("LEADERBOARD" in the example).<p>
 * 
 * Equality and hash code are defined based on the {@link #getName() name}.
 *
 * @author Axel Uhl (d043530)
 *
 */
public interface HasPermissions extends Named {
    /**
     * Used for the first part ("type") of the permissions constructed for this object. Basis for equality and hash code.
     */
    String getName();
    
    /**
     * @return the actions for which permissions can be defined on the logical, securable type represented by this
     *         object. Only those actions need to be accepted by methods such as {@link #getStringPermission(Action...)},
     *         and it is acceptable for those methods to throw an {@link IllegalArgumentException} if an action is
     *         passed that is not contained in the array returned by this method.
     */
    Action[] getAvailableActions();
    
    /**
     * The {@link Action} from {@link #getAvailableActions()} whose {@link Action#name() name} equals that provided by parameter
     * {@code actionName}, or {@code null} if no such action exists
     */
    default Action getActionByName(String actionName) {
        return Util.first(Util.filter(Arrays.asList(getAvailableActions()), action->action.name().equals(actionName)));
    }

    /**
     * @return {@code true} if and only if objects of this logical type support the {@code action} as one of their
     *         {@link #getAvailableActions() available actions}
     */
    boolean supports(Action action);

    /**
     * If one or more modes are specified, a string permission is rendered that has the
     * {@link Action#getStringPermission() permission strings} of those modes listed in the second wildcard permission
     * component. Otherwise, only the primary permission (representing the object type) with one segment is returned.
     */
    String getStringPermission(Action... actions);

    /**
     * Same as {@link #getStringPermission(Action...)}, only that the result is a {@link WildcardPermission} instead of a
     * {@link String}
     */
    WildcardPermission getPermission(Action... actions);

    /**
     * Produces a string permission for this permission, the <code>mode</code> specified as the second wildcard
     * permission segment, and the <code>objectIdentifier</code> as the third wildcard permission segment. The object
     * identifiers must be unique within the scope defined by this {@link HasPermissions} which represents an object
     * category or type, such as, e.g., "LEADERBOARD."
     * 
     * @param typeRelativeObjectIdentifier
     *            can be any string; this method will take care of encoding the identifiers such that they are legal in
     *            the context of a permission part; see also {@link PermissionStringEncoder}
     */
    String getStringPermissionForTypeRelativeIdentifier(Action action, TypeRelativeObjectIdentifier typeRelativeObjectIdentifier);

    /**
     * Produces a string permission for this permission, the <code>mode</code> specified as the second wildcard
     * permission segment, and the <code>objectIdentifier</code> as the third wildcard permission segment. The object
     * identifiers must be unique within the scope defined by this {@link HasPermissions} which represents an object
     * category or type, such as, e.g., "LEADERBOARD."
     * 
     * @param object
     *            can be any object that can be passed to the identifer strategy to determine the type relative
     *            identifier for this object. What type of object can be passed is defined by the identifer strategy
     */
    String getStringPermissionForObject(Action action, WithQualifiedObjectIdentifier object);

    /**
     * Qualifies the {@code objectIdentifier} which only has to be unique within the scope of the type identified by
     * this permission with this permission's type name. For example, if this permission is for the "LEADERBOARD" type,
     * and the {@code objectIdentifier} is {@code "abc"} then the resulting qualified identifier will be
     * "LEADERBOARD/abc". This assumes that the {@link #name()} method returns only values that do not contain a "/".
     */
    QualifiedObjectIdentifier getQualifiedObjectIdentifier(TypeRelativeObjectIdentifier typeRelativeObjectIdentifier);

    /**
     * Same as {@link #getStringPermissionForTypeRelativeIdentifiers(Action, String...)}, only that the result is a
     * {@link WildcardPermission} instead of a {@link String}
     * 
     */
    WildcardPermission getPermissionForTypeRelativeIdentifier(Action action, TypeRelativeObjectIdentifier objectIdentifiers);

    /**
     * Same as {@link #getStringPermissionForTypeRelativeIdentifiers(Action, String...)}, only that the result is a
     * {@link WildcardPermission} instead of a {@link String}
     * 
     * @param object
     *            can be any object that can be passed to the identifer strategy to determine the type relative
     *            identifier for this object. What type of object can be passed is defined by the identifer strategy
     * 
     */
    WildcardPermission getPermissionForObject(Action action, WithQualifiedObjectIdentifier object);

    /**
     * Same as {@link #getPermissionForTypeRelativeIdentifiers(Action, String...)}, only that this method gets the
     * {@link WildcardPermission}s for all given actions
     */
    WildcardPermission[] getPermissionsForTypeRelativeIdentifier(final Action[] actions, final TypeRelativeObjectIdentifier objectIdentifiers);

    public static interface Action {
        /**
         * Returns the action as represented in the second part of a {@link WildcardPermission}. This shall be a string
         * that will not need further encoding such as by a {@link WildcardPermissionEncoder} so that it can be used
         * in a {@link WildcardPermission} immediately and without change.
         */
        String name();
    }
    
    public static interface SecuredEnum {
        Action getPremiumAction();
    }

    /**
     * A mode of operation on a resource; the typical "CRUD" operations.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    public enum DefaultActions implements Action {
        CREATE, READ, UPDATE, DELETE, CHANGE_OWNERSHIP, CHANGE_ACL;
        
        public static final Action[] MUTATION_ACTIONS = new Action[] { CREATE, UPDATE, DELETE, CHANGE_OWNERSHIP,
                CHANGE_ACL };

        public static final Action[] MUTATION_ACTIONS_FOR_NON_DELETABLE_TYPES = new Action[] { DefaultActions.CREATE,
                DefaultActions.UPDATE, DefaultActions.CHANGE_OWNERSHIP, DefaultActions.CHANGE_ACL };
        
        /**
         * Returns all {@link DefaultActions} plus the {@code actions} passed, combined in one new array
         */
        public static Action[] plus(Action... actions) {
            final Action[] result = new Action[values().length+actions.length];
            System.arraycopy(values(), 0, result, 0, values().length);
            System.arraycopy(actions, 0, result, values().length, actions.length);
            return result;
        }
    }

}
