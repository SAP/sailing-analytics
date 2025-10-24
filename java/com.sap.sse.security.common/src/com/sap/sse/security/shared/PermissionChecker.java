package com.sap.sse.security.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.security.shared.HasPermissions.Action;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;

/**
 * The {@link PermissionChecker} is an implementation of the permission checking algorithm also described in the
 * <a href= "https://wiki.sapsailing.com/wiki/info/security/permission-concept">Concept Wiki article</a>. It checks
 * permissions in four stages where earlier stages overwrite later. See also
 * {@link #isPermitted(WildcardPermission, SecurityUser, Iterable, SecurityUser, Iterable, AbstractOwnership, SecurityAccessControlList)}.
 * 
 * @author Jonas Dann, Axel Uhl (d043530)
 */
public class PermissionChecker {
    public enum PermissionState {
        GRANTED,
        REVOKED,
        NONE
    }
    
    /**
     * {@link AclResolver} is used to resolve all relevant ACLs during meta permission check.
     */
    public interface AclResolver<A extends SecurityAccessControlList<?>, O extends AbstractOwnership<?, ?>> {
        /**
         * Resolves all relevant ACLs during meta permission check and searches for the first ACL to match the
         * {@code filterCondition}. If and only if at least one such ACL is found, {@code null} is returned. As this
         * could happen before having discovered all applicable ACLs for the {@code type} /
         * {@code objectIdentifiersAsStringOrNull} combination, the full set of ACLs isn't known in this case. However,
         * if no ACL matched the {@code filterCondition}, all ACLs applicable to the {@code type} /
         * {@code objectIdentifiersAsStringOrNull} combination have been scanned and can be returned for caching, to be
         * passed in for future invocations for the same combination of type and object ID.
         * <p>
         * 
         * Relevant ACLs are determined on several constraints:
         * <ul>
         * <li>The security type is required to be given. Due to the fact that the meta permission check expands the
         * permission to distinct permissions regarding the type and action part, the effective check always takes place
         * for one type only. This means, the type is always known.
         * <li>An ownership may be given. This is the ownership that can be derived from an expanded permission, if this
         * permission includes an ID. In cases where a qualified permission is granted (e.g. adding a qualified
         * {@link Role} or adding a {@link Role} to a {@link UserGroup}), this ownership can be directly derived by the
         * qualification because the check only affects objects that are appropriately owned. Knowing the ownership
         * means, ACLs are only relevant for the meta permission check if the associated object is owned as defined by
         * the given ownership.
         * <li>A set of stringified object identifiers may be given in addition to the type. If the meta permission to
         * check includes distinct identifiers, we can enumerate the objects based on the type (we always have this one)
         * and identifiers, which means, a direct lookup for the ACLs by {@link QualifiedObjectIdentifier} is possible.
         * </ul>
         * 
         * @param filterCondition
         *            if any of the ACLs found matches this predicate, {@code true} is returned; {@code false} otherwise
         * 
         * @param allAclsForTypeAndObjectIdsOrNull
         *            if not {@code null} these ACLs are assumed to be the full set of ACLs applicable to the combination
         *            of {@code type} and {@code objectIdentifiersAsStringOrNull}. In this case a potentially expensive
         *            computation to determine those can be avoided. If {@code null}, the ACLs need to be determined here.
         * 
         * @return {@code null} if any of the ACLs found passed the {@code filterCondition}; the set of
         *         {@link AccessControlList}s relevant for the combination of {@code type} and
         *         {@code objectIdentifiersAsStringOrNull} otherwise
         */
        Iterable<AccessControlList> resolveDenyingAclsAndCheckIfAnyMatches(O ownershipOrNull, String type, Iterable<String> objectIdentifiersAsStringOrNull,
                Predicate<A> filterCondition, Iterable<AccessControlList> allAclsForTypeAndObjectIdsOrNull);
    }
    
    private static final BiFunction<WildcardPermission, WildcardPermission, Boolean> impliesChecker = WildcardPermission::implies;
    
    private static final BiFunction<WildcardPermission, WildcardPermission, Boolean> impliesAnyChecker = WildcardPermission::impliesAny;
    
    private static <U extends SecurityUser<?, ?, G>, G extends SecurityUserGroup<?>> Iterable<G> getGroupsOfUser(U user) {
        return (Iterable<G>) (user == null ? Collections.<G>emptySet() : user.getUserGroups());
    }
    
    /**
     * Like
     * {@link #isPermitted(WildcardPermission, SecurityUser, SecurityUser, AbstractOwnership, SecurityAccessControlList, Iterable)},
     * determining the groups of the {@code user} and the {@code allUser} through {@link User#getUserGroups()}. See also
     * {@link #getGroupsOfUser(SecurityUser)}.
     * 
     * @param permission
     *            Permission of the form "data_object_type:action:instance_id". The instance id can be omitted when a
     *            general permission for the data object type is asked after (e.g. "event:create"). If the action
     *            contains more than one sub-part (divided by {@link WildcardPermission#SUBPART_DIVIDER_TOKEN}) then
     *            this is considered an error, and an {@link IllegalArgumentException} will be thrown.
     * @param ownership
     *            may be {@code null}, causing user- or tenant-parameterized roles and no user ownership override to be
     *            applied
     * @param acl
     *            may be {@code null} in which case no ACL-specific checks are performed
     */
    public static <RD extends RoleDefinition, R extends AbstractRole<RD, G, UR>, O extends AbstractOwnership<G, UR>, UR extends UserReference, U extends SecurityUser<RD, R, G>, G extends SecurityUserGroup<RD>, A extends SecurityAccessControlList<G>>
    boolean isPermitted(WildcardPermission permission, U user, U allUser, O ownership, A acl) {
        return isPermitted(permission, user, getGroupsOfUser(user), allUser, getGroupsOfUser(allUser), ownership, acl, null);
    }
    
    /**
     * Like
     * {@link #isPermitted(WildcardPermission, SecurityUser, SecurityUser, AbstractOwnership, SecurityAccessControlList)},
     * Can be parameterized with additional Roles, to check whether the user would be granted access if he aquired these roles. 
     * 
     * @param permission
     *            Permission of the form "data_object_type:action:instance_id". The instance id can be omitted when a
     *            general permission for the data object type is asked after (e.g. "event:create"). If the action
     *            contains more than one sub-part (divided by {@link WildcardPermission#SUBPART_DIVIDER_TOKEN}) then
     *            this is considered an error, and an {@link IllegalArgumentException} will be thrown.
     * @param ownership
     *            may be {@code null}, causing user- or tenant-parameterized roles and no user ownership override to be
     *            applied
     * @param acl
     *            may be {@code null} in which case no ACL-specific checks are performed
     * @param additionalRoles
     *            may be {@code null} in which case no additional Roles are considered.
     */
    public static <RD extends RoleDefinition, R extends AbstractRole<RD, G, UR>, O extends AbstractOwnership<G, UR>, UR extends UserReference, U extends SecurityUser<RD, R, G>, G extends SecurityUserGroup<RD>, A extends SecurityAccessControlList<G>>
    boolean isPermitted(WildcardPermission permission, U user, U allUser, O ownership, A acl, Iterable<R> additionalRoles) {
        return isPermitted(permission, user, getGroupsOfUser(user), allUser, getGroupsOfUser(allUser), ownership, acl, additionalRoles);
    }

    /**
     * The following checks are performed. If a step cannot decide authoritatively, the preliminary result remains as
     * {@link PermissionState#NONE}, and the subsequent steps are performed to find the result. If a step computes a
     * non-{@code NONE} result, it is returned.
     * <ol>
     * <li>ACL check (see {@link #checkAcl(WildcardPermission, Iterable, Iterable, SecurityAccessControlList)}): If the
     * object's {@code acl} passed to this method explicitly allows or denies access to the {@code user}'s group or to
     * the {@code allUser}'s group then this is the result.</li>
     * <li>User permissions (see {@link User#getPermissions()}): If any of the explicit permissions assigned to the
     * {@code user} or the {@code allUser} passed to this method match or imply the {@code permission} requested, the
     * result is {@link PermissionState#GRANTED}, leading to {@code true} being returned.</li>
     * <li>If an {@code ownership} exists for the object being accessed and has a valid group ownership, that group's
     * roles are evaluated; if any of those roles implies the {@code permission} requested to the users of that group
     * and the {@code user} or the {@code allUser} belongs to that group, or if the role implies the {@code permission}
     * requested to all users, the result is {@link PermissionState#GRANTED}, and {@code true} will be returned.</li>
     * <li>If any of the roles assigned to the {@code user} or the {@code allUser}---considering their optional user/group
     * qualificiations based on the {@code ownership}---implies the {@code permission} requested, the result is
     * {@link PermissionState#GRANTED}, and {@code true} will be returned.</li>
     * </ol>
     * 
     * @param permission
     *            Permission of the form "data_object_type:action:instance_id". The instance id can be omitted when a
     *            general permission for the data object type is asked after (e.g. "event:create"). If the action
     *            contains more than one sub-part (divided by {@link WildcardPermission#SUBPART_DIVIDER_TOKEN}) then
     *            this is considered an error, and an {@link IllegalArgumentException} will be thrown.
     * @param ownership
     *            may be {@code null}, causing user- or tenant-parameterized roles and no user ownership override to be
     *            applied
     * @param acl
     *            may be {@code null} in which case no ACL-specific checks are performed
     */
    public static <RD extends RoleDefinition, R extends AbstractRole<RD, G, UR>, O extends AbstractOwnership<G, UR>, UR extends UserReference, U extends SecurityUser<RD, R, G>, G extends SecurityUserGroup<RD>, A extends SecurityAccessControlList<G>>
    boolean isPermitted(WildcardPermission permission, U user, Iterable<G> groupsOfWhichUserIsMember, U allUser,
            Iterable<G> groupsOfWhichAllUserIsMember, O ownership, A acl, Iterable<R> additionalRoles) {
        PermissionState result = checkAcl(permission, groupsOfWhichUserIsMember, groupsOfWhichAllUserIsMember, acl);
        // anonymous can only grant it if not already decided by acl
        if (result == PermissionState.NONE) {
            PermissionState anonymous = checkUserPermissions(permission, allUser, groupsOfWhichAllUserIsMember,
                    ownership, impliesChecker, additionalRoles, /* matchOnlyNonQualifiedRolesIfNoOwnershipIsGiven */ true,
                    /* matchOnlyTransitiveRoles */ false);
            if (anonymous == PermissionState.GRANTED) {
                result = anonymous;
            }
        }
        if (result == PermissionState.NONE) {
            result = checkUserPermissions(permission, user, groupsOfWhichUserIsMember, ownership, impliesChecker, additionalRoles,
                    /* matchOnlyNonQualifiedRolesIfNoOwnershipIsGiven */ true, /* matchOnlyNonTransitiveRoles */ false);
        }
        return result == PermissionState.GRANTED;
    }

    private static <A extends SecurityAccessControlList<G>, G extends SecurityUserGroup<?>> PermissionState checkAcl(
            WildcardPermission permission, Iterable<G> groupsOfWhichUserIsMember, Iterable<G> groupsOfWhichAllUserIsMember, A acl) {
        List<Set<String>> parts = permission.getParts();
        return checkAcl(permission, groupsOfWhichUserIsMember, groupsOfWhichAllUserIsMember, acl, parts);
    }

    private static <G extends SecurityUserGroup<?>, A extends SecurityAccessControlList<G>> PermissionState checkAcl(
            WildcardPermission permission, Iterable<G> groupsOfWhichUserIsMember,
            Iterable<G> groupsOfWhichAllUserIsMember, A acl, List<Set<String>> parts) {
        // permission has at least data object type and action as parts
        // and data object part only has one sub-part
        if (parts.get(0).size() != 1) {
            throw new WrongPermissionFormatException(permission);
        }
        PermissionState result = PermissionState.NONE;
        // 1. check ACL
        if (acl != null) {
            // if no specific action is requested then this translates to a request for all permissions ("*")
            final String action;
            if (parts.size() < 2) {
                action = WildcardPermission.WILDCARD_TOKEN;
            } else {
                if (parts.get(1).size() > 1) {
                    throw new IllegalArgumentException("Permission to check must not have more than one sub-part: "+parts.get(1));
                }
                action = (String) parts.get(1).toArray()[0];
            }
            Set<G> allGroups = new HashSet<>();
            Util.addAll(groupsOfWhichUserIsMember, allGroups);
            Util.addAll(groupsOfWhichAllUserIsMember, allGroups);
            result = acl.hasPermission(action, allGroups);
        }
        return result;
    }

    /**
     * Checks if a user has a specific role either for a given ownership or globally if no ownership exists.
     */
    public static <RD extends RoleDefinition, R extends AbstractRole<RD, G, U>, O extends AbstractOwnership<G, U>, U extends SecurityUser<RD, R, G>, G extends SecurityUserGroup<RD>, A extends SecurityAccessControlList<G>>
    boolean ownsUserASpecificRole(U user, U allUser, O ownership, String requiredRoleName) {
        assert requiredRoleName != null;
        return ownsUserASpecificRole(user, ownership, requiredRoleName)
                || ownsUserASpecificRole(allUser, ownership, requiredRoleName);

    }

    private static <RD extends RoleDefinition, R extends AbstractRole<RD, G, U>, O extends AbstractOwnership<G, U>, U extends SecurityUser<RD, R, G>, G extends SecurityUserGroup<RD>, A extends SecurityAccessControlList<G>>
    boolean ownsUserASpecificRole(U user, O ownership, String requiredRoleName) {
        if (user == null) {
            return false;
        }
        for (R roleOwnedByUser : user.getRoles()) {
            if (roleOwnedByUser.getName().equals(requiredRoleName)) {
                if (roleOwnedByUser.getQualifiedForTenant() == null && roleOwnedByUser.getQualifiedForUser() == null) {
                    // the role is not qualified by a user or group which means it counts for any ownership
                    return true;
                }
                if (ownership == null || (ownership.getTenantOwner() == null && ownership.getUserOwner() == null)) {
                    // the role is qualified but there is now ownership which means the user needs the unqualified
                    // version of the role
                    continue;
                }
                if (roleOwnedByUser.getQualifiedForTenant() != null
                        && roleOwnedByUser.getQualifiedForTenant().equals(ownership.getTenantOwner())) {
                    return true;
                }
                if (roleOwnedByUser.getQualifiedForUser() != null
                        && roleOwnedByUser.getQualifiedForUser().equals(ownership.getUserOwner())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * This is not the fully featured permission check!<br>
     * This method checks if a user owns all permissions implied by the given {@link WildcardPermission}. If a user
     * wants to grant another user a specific permissions, he/she needs to own all permissions that may be implied by the
     * granted permission.<br>
     * The regular permission check only succeeds if a requested permission is completely implied by a granted
     * permission. This is fine when checking for a specific permission like USER:UPDATE:my_user. If one user grants a
     * meta permission like USER:*:* for another user this would mean the current user needs an equivalent meta
     * permission or one that is even more general like *:*:* or USER,USER_GROUP:*:*. In contrast to that, a user who
     * owns the permissions USER:*:* and USER_GROUP:*:* would---with the <em>regular</em> permission check---not be able
     * to grant USER,USER_GROUP:*:* because this meta permission would not be implied by a single permission held by the
     * current user.<br>
     * To solve this we use the given {@link HasPermissions} instances to resolve wildcards for the type and action
     * parts and construct distinct permissions to check. This means the given {@link HasPermissions} need to be a
     * complete list for the running system. Otherwise we potentially do not check for all required types if the given
     * {@link WildcardPermission} has a wildcard as type part.
     */
    public static <RD extends RoleDefinition, R extends AbstractRole<RD, G, UR>, O extends AbstractOwnership<G, UR>, UR extends UserReference, U extends SecurityUser<RD, R, G>, G extends SecurityUserGroup<RD>, A extends SecurityAccessControlList<G>>
    boolean checkMetaPermission(WildcardPermission permission,
            Iterable<? extends HasPermissions> allPermissionTypes, U user, U allUser, O ownership, AclResolver<A, O> aclResolver) {
        return checkMetaPermissionInternal(permission, allPermissionTypes, user, allUser, wp -> ownership, aclResolver);
    }
    
    /**
     * This is not the fully featured permission check!<br>
     * See {@link #checkMetaPermission(WildcardPermission, Iterable, SecurityUser, SecurityUser, AbstractOwnership)} for
     * more information about the check.<br>
     * This version of the meta permission check relies on dynamic resolution of ownerships via the given
     * ownershipResolver. Due to the absence of ownership information this check is not possible in the UI.
     * 
     * @param ownershipResolver
     *            due to the fact that complex {@link WildcardPermission WildcardPermissions} are expanded using
     *            {@link #expandSingleWildcardPermissionToDistinctPermissions(WildcardPermission, Iterable, boolean)},
     *            for permissions containing a single id or a list of distinct ids (no wildcard in the id part) this
     *            results in expanded permissions that can be used to resolve to a single
     *            {@link QualifiedObjectIdentifier} which in fact allows the surrounding system to resolve an
     *            {@link Ownership} for those permissions. Using this, it is possible to restrict the permission check
     *            to the effectively existing owning qualification. If no {@link Ownership} can be resolved, the check
     *            can not be restricted to an owning qualification and must pass unrestricted in this consequence.
     */
    public static <RD extends RoleDefinition, R extends AbstractRole<RD, G, UR>, O extends AbstractOwnership<G, UR>, UR extends UserReference, U extends SecurityUser<RD, R, G>, G extends SecurityUserGroup<RD>, A extends SecurityAccessControlList<G>>
    boolean checkMetaPermissionWithOwnershipResolution(
            WildcardPermission permission, Iterable<? extends HasPermissions> allPermissionTypes, U user, U allUser,
            Function<QualifiedObjectIdentifier, O> ownershipResolver, AclResolver<A, O> aclResolver) {
        return checkMetaPermissionInternal(permission, allPermissionTypes, user, allUser, wp -> {
            final Iterable<QualifiedObjectIdentifier> qualifiedObjectIdentifiers = wp.getQualifiedObjectIdentifiers();
            final O ownership;
            Iterator<QualifiedObjectIdentifier> iterator = qualifiedObjectIdentifiers.iterator();
            if (!iterator.hasNext()) {
                ownership = null;
            } else {
                QualifiedObjectIdentifier firstIdentifier = iterator.next();
                if (iterator.hasNext() || firstIdentifier == null) {
                    // No single distinct identifier
                    ownership = null;
                } else {
                    ownership = ownershipResolver.apply(firstIdentifier);
                }
            }
            return ownership;
        }, aclResolver);
    }
    
    private static <RD extends RoleDefinition, R extends AbstractRole<RD, G, UR>, O extends AbstractOwnership<G, UR>, UR extends UserReference, U extends SecurityUser<RD, R, G>, G extends SecurityUserGroup<RD>, A extends SecurityAccessControlList<G>>
    boolean checkMetaPermissionInternal(
            WildcardPermission permission,
            Iterable<? extends HasPermissions> allPermissionTypes, U user, U allUser,
            Function<WildcardPermission, O> ownershipResolver, AclResolver<A, O> aclResolver) {
        assert permission != null;
        assert allPermissionTypes != null;
        final Set<WildcardPermission> effectivePermissionsToCheck = expandSingleWildcardPermissionToDistinctPermissions(
                permission, allPermissionTypes, /* expand multiple to single object IDs */ true);
        final Iterable<G> groupsOfUser = getGroupsOfUser(user);
        final Iterable<G> groupsOfAllUser = getGroupsOfUser(allUser);
        // for resolveAcls, all effectivePermissionsToCheck can be collated based on their types and object IDs; the action can be ignored;
        // this map remembers for which type/object-id combinations the check has already been performed
        final Map<Triple<String, Set<String>, O>, Iterable<AccessControlList>> typesAndIdsAndOwnershipsWithFullSetOfAcls = new HashMap<>();
        for (WildcardPermission effectiveWildcardPermissionToCheck : effectivePermissionsToCheck) {
            // Ownership may be null in case of an unqualified meta permission check
            // That's the case when e.g. assigning an unqualified role to a user
            final O ownership = ownershipResolver.apply(effectiveWildcardPermissionToCheck);
            final List<Set<String>> wildcardPermissionParts = effectiveWildcardPermissionToCheck.getParts();
            // expanded permissions always have a single non-wildcard type part
            final String type = wildcardPermissionParts.get(0).iterator().next();
            final boolean identifierWildcard;
            final Set<String> identifiers;
            if (wildcardPermissionParts.size() >= 3) {
                identifiers = wildcardPermissionParts.get(2);
                identifierWildcard = WildcardPermission.WILDCARD_TOKEN.equals(identifiers.iterator().next());
            } else {
                identifiers = Collections.emptySet();
                identifierWildcard = true;
            }
            final Triple<String, Set<String>, O> collationKeyBasedOnTypeAndObjectIdAndOwnership = new Triple<>(type, identifiers, ownership);
            final Iterable<AccessControlList> denyingAclsFromPreviousRunForSameTypeAndObjectIds =
                    typesAndIdsAndOwnershipsWithFullSetOfAcls.get(collationKeyBasedOnTypeAndObjectIdAndOwnership);
            final Iterable<AccessControlList> denyingAcls = aclResolver.resolveDenyingAclsAndCheckIfAnyMatches(ownership, type,
                    identifierWildcard ? null : identifiers,
                    acl->checkAcl(effectiveWildcardPermissionToCheck, groupsOfUser, groupsOfAllUser, acl) == PermissionState.REVOKED,
                    denyingAclsFromPreviousRunForSameTypeAndObjectIds);
            if (denyingAcls == null) { // this means an ACL was found that REVOKED the permission
                return false;
            } else if (denyingAclsFromPreviousRunForSameTypeAndObjectIds == null) {
                // now we have the full scan of ACLs for the type/object-id combination; cache it
                typesAndIdsAndOwnershipsWithFullSetOfAcls.put(collationKeyBasedOnTypeAndObjectIdAndOwnership, denyingAcls);
            }
            if (checkUserPermissions(effectiveWildcardPermissionToCheck, user, groupsOfUser, ownership, impliesChecker,
                    /* additionalRoles */ null, /* matchOnlyNonQualifiedRolesIfNoOwnershipIsGiven */ true,
                    /* matchOnlyTransitiveRoles */ true) != PermissionState.GRANTED
                    && checkUserPermissions(effectiveWildcardPermissionToCheck, allUser, groupsOfAllUser, ownership,
                            impliesChecker, /* additionalRoles */ null,
                            /* matchOnlyNonQualifiedRolesIfNoOwnershipIsGiven */ true,
                            /* matchOnlyTransitiveRoles */ true) != PermissionState.GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * WildcardPermissions can be pretty complex and allow wildcards for all parts. With the knowledge of all specific
     * permissions that are implied by a WildcardPermission, we can do more distinct checks that do not require a user
     * to have permission with an equivalent wildcard.
     * 
     * With the specific knowledge of all existing secured types and their associated actions, we can:
     * <ul>
     * <li>Resolve * for the type to a set of existing types</li>
     * <li>Filter a list of action per declared type (pruning of impossible permissions)</li>
     * </ul>
     * 
     * A user having distinct permissions for all existing types, will in fact be able to succeed a permission check for
     * *, although, he does not explicitly have * as a permission.
     * 
     * The following rules apply for the expansion of permissions:
     * <ul>
     * <li>A list of types is expanded to single type permissions. Explicitly declared but unknown types are not
     * pruned.</li>
     * <li>A type wildcard is expanded to single permissions for all known types</li>
     * <li>A list of actions is expanded to single action permissions. For known types, the set of actions is pruned to
     * the effectively existing actions. For unknown types, the action list is preserved.</li>
     * <li>A action wildcard is expanded to single action permissions per known type or a wildcard for unknown
     * types.</li>
     * </ul>
     * 
     * @param expandToSingleIds
     *            if true, a list of ids is also expanded to distinct permissions instead of a permission having a list
     *            of ids. This is useful when it is necessary to resolve e.g. type identifiers from a
     *            {@link WildcardPermission}.
     */
    private static Set<WildcardPermission> expandSingleWildcardPermissionToDistinctPermissions(WildcardPermission permission,
            Iterable<? extends HasPermissions> allPermissionTypes, boolean expandToSingleIds) {
        List<Set<String>> parts = permission.getParts();
        final Set<String> typeParts;
        final boolean isTypePartWildcard;
        if (parts.size() >= 1) {
            typeParts = parts.get(0);
            isTypePartWildcard = typeParts.isEmpty() || typeParts.contains(WildcardPermission.WILDCARD_TOKEN);
        } else {
            typeParts = null;
            isTypePartWildcard = true;
        }
        final Set<String> actionParts;
        final boolean isActionPartWildcard;
        if (parts.size() >= 2) {
            actionParts = parts.get(1);
            isActionPartWildcard = actionParts.isEmpty() || actionParts.contains(WildcardPermission.WILDCARD_TOKEN);
        } else {
            actionParts = null;
            isActionPartWildcard = true;
        }
        final Set<String> idParts;
        final boolean isIdPartWildcard;
        if (parts.size() >= 3) {
            idParts = parts.get(2);
            isIdPartWildcard = idParts.isEmpty() || idParts.contains(WildcardPermission.WILDCARD_TOKEN);
        } else {
            idParts = null;
            isIdPartWildcard = true;
        }
        Map<String, HasPermissions> allPermissionTypesByName = new HashMap<>();
        for (HasPermissions hasPermissions : allPermissionTypes) {
            allPermissionTypesByName.put(hasPermissions.getName(), hasPermissions);
        }
        final Set<String> effectiveIdPartsToCheck;
        if (isIdPartWildcard) {
            effectiveIdPartsToCheck = Collections.singleton("");
        } else if (expandToSingleIds) {
            effectiveIdPartsToCheck = idParts;
        } else {
            effectiveIdPartsToCheck = Collections.singleton(Util.joinStrings(WildcardPermission.SUBPART_DIVIDER_TOKEN, idParts));
        }
        final Set<String> effectiveTypePartsToCheck;
        if (isTypePartWildcard) {
            effectiveTypePartsToCheck = allPermissionTypesByName.keySet();
        } else {
            effectiveTypePartsToCheck = typeParts;
        }
        final Set<WildcardPermission> effectivePermissionsToCheck = new HashSet<>();
        for (String typePart : effectiveTypePartsToCheck) {
            HasPermissions hasPermissions = allPermissionTypesByName.get(typePart);
            final Set<String> effectiveActionPartsToCheck;
            if (hasPermissions == null) {
                effectiveActionPartsToCheck = actionParts;
            } else {
                effectiveActionPartsToCheck = new HashSet<>();
                for (Action action : hasPermissions.getAvailableActions()) {
                    if (isActionPartWildcard || actionParts.contains(action.name())) {
                        effectiveActionPartsToCheck.add(action.name());
                    }
                }
            }
            if (effectiveActionPartsToCheck != null) {
                for (String actionPart : effectiveActionPartsToCheck) {
                    for (String idPart : effectiveIdPartsToCheck) {
                        String idSuffix = idPart.isEmpty() ? "" : WildcardPermission.PART_DIVIDER_TOKEN + idPart;
                        effectivePermissionsToCheck.add(new WildcardPermission(
                                typePart + WildcardPermission.PART_DIVIDER_TOKEN + actionPart + idSuffix));
                    }
                }
            }
        }
        return effectivePermissionsToCheck;
    }
    
    /**
     * Checks if a user has at least one permission implied by the given {@link WildcardPermission}.<br>
     * This is e.g. useful to decide if a user can see a specific tab/button/... in the UI. For example a user who has
     * Permission USER:UPDATE:abc may see the user management UI. A generic Permission check for USER:UPDATE will fail
     * because only users that have an unqualified version of a permission would pass that permission check. This means
     * we need to check the permission for any possibly existing object ID.
     */
    public static <RD extends RoleDefinition, R extends AbstractRole<RD, G, UR>, O extends AbstractOwnership<G, UR>, UR extends UserReference, U extends SecurityUser<RD, R, G>, G extends SecurityUserGroup<RD>, A extends SecurityAccessControlList<G>>
    boolean hasUserAnyPermission(WildcardPermission permission, Iterable<? extends HasPermissions> allPermissionTypes, U user, U allUser, O ownership) {
        assert permission != null;
        assert allPermissionTypes != null;
        final Set<WildcardPermission> effectivePermissionsToCheck = expandSingleWildcardPermissionToDistinctPermissions(permission, allPermissionTypes, false);
        for (WildcardPermission effectiveWildcardPermissionToCheck : effectivePermissionsToCheck) {
            if (checkUserPermissions(effectiveWildcardPermissionToCheck, user, getGroupsOfUser(user), ownership, impliesAnyChecker, /* additionalRoles */ null,
                    /* matchOnlyNonQualifiedRolesIfNoOwnershipIsGiven */ false, /* matchOnlyTransitiveRoles */false) == PermissionState.GRANTED
                    || checkUserPermissions(effectiveWildcardPermissionToCheck, allUser, getGroupsOfUser(allUser), ownership, impliesAnyChecker, /* additionalRoles */ null,
                            /* matchOnlyNonQualifiedRolesIfNoOwnershipIsGiven */ false, /* matchOnlyTransitiveRoles */ false) == PermissionState.GRANTED) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Like
     * {@link #isPermitted(WildcardPermission, SecurityUser, Iterable, SecurityUser, Iterable, AbstractOwnership, SecurityAccessControlList)},
     * only without the ACL check. As a matter of fact, this method is being used by
     * {@link #isPermitted(WildcardPermission, SecurityUser, Iterable, SecurityUser, Iterable, AbstractOwnership, SecurityAccessControlList)}
     * for all but the ACL check.
     * 
     * @param permissionChecker
     *            The permissionChecker is the one that checks if one permission implies another permission. This means,
     *            it just calls {@link WildcardPermission#implies(WildcardPermission)} in most cases. In case of an any
     *            check this needs to call {@link WildcardPermission#impliesAny(WildcardPermission)).
     * @param matchOnlyNonQualifiedRolesIfNoOwnershipIsGiven
     *            If {@code false} and no ownership is given, all associated {@link Role Roles} of a user are checked.
     *            For a normal permission check, this needs to be {@code true}, because qualified {@link Role Roles} can
     *            not grant unqualified permissions. In case of an any check it is necessary to include all qualified
     *            roles of an user, because even a qualified {@link Role} may grant any of the included permissions. in
     *            this case, the parameter is intended to be {@code false}.
     */
    private static <RD extends RoleDefinition, R extends AbstractRole<RD, G, UR>, O extends AbstractOwnership<G, UR>, UR extends UserReference, U extends SecurityUser<RD, R, G>, G extends SecurityUserGroup<RD>, A extends SecurityAccessControlList<G>>
    PermissionState checkUserPermissions(
            WildcardPermission permission, U user, Iterable<G> groupsOfWhichUserIsMember, O ownership,
            BiFunction<WildcardPermission, WildcardPermission, Boolean> permissionChecker, Iterable<R> additionalRoles, 
            boolean matchOnlyNonQualifiedRolesIfNoOwnershipIsGiven, boolean matchOnlyTransitiveRoles) {
        PermissionState result = PermissionState.NONE;
        // 1. check direct permissions
        if (result == PermissionState.NONE && user != null) { // no direct permissions for anonymous users
            for (WildcardPermission directPermission : user.getPermissions()) {
                if (permissionChecker.apply(directPermission, permission)) {
                    result = PermissionState.GRANTED;
                    break;
                }
            }
        }
        // 2. check group permissions
        if (ownership != null && ownership.getTenantOwner() != null) {
            final G tenantOwner = ownership.getTenantOwner();
            final boolean userIsMemberOfGroup = Util.contains(groupsOfWhichUserIsMember, tenantOwner);
            if (isPermissionGrantedByGroup(permission, tenantOwner, userIsMemberOfGroup, permissionChecker)) {
                result = PermissionState.GRANTED;
            }
        } else if (!matchOnlyNonQualifiedRolesIfNoOwnershipIsGiven) {
            for (G groupToCheck : groupsOfWhichUserIsMember) {
                if (isPermissionGrantedByGroup(permission, groupToCheck, true, permissionChecker)) {
                    result = PermissionState.GRANTED;
                    break;
                }
            }
        }
        // 3. check role permissions
        if (result == PermissionState.NONE && user != null) { // an anonymous user does not have any roles
            final Iterable<R> userRoles = user.getRoles();
            final HashSet<R> withAdditionalRoles = new HashSet<R>();
            userRoles.forEach(withAdditionalRoles::add);
            if (additionalRoles != null) {
                additionalRoles.forEach(withAdditionalRoles::add);
            }
            for (R role : withAdditionalRoles) {
                if (implies(role, permission, ownership, permissionChecker,
                        matchOnlyNonQualifiedRolesIfNoOwnershipIsGiven, matchOnlyTransitiveRoles)) {
                    result = PermissionState.GRANTED;
                    break;
                }
            }
        }
        return result;
    }
    
    private static <RD extends RoleDefinition, U extends SecurityUser<RD, ?, G>, G extends SecurityUserGroup<RD>> boolean isPermissionGrantedByGroup(
            WildcardPermission permission, G groupToCheck, boolean userIsMemberOfGroup,
            BiFunction<WildcardPermission, WildcardPermission, Boolean> permissionChecker) {
        for (Map.Entry<RD, Boolean> entry : groupToCheck.getRoleDefinitionMap().entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue()) || userIsMemberOfGroup) {
                final RD roleDefinition = entry.getKey();
                for (WildcardPermission grantedPermission : roleDefinition.getPermissions()) {
                    if (permissionChecker.apply(grantedPermission, permission)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * @param role
     *            the role; it may have a {@link Role#getQualifiedForTenant() tenant} and/or
     *            {@link Role#getQualifiedForUser() user} qualification which means the role's permissions are granted
     *            only if the {@code ownership}'s {@link Ownership#getTenantOwner() tenant} and/or
     *            {@link Ownership#getUserOwner() user} owner match the role qualification. E.g.:
     *            {@code event-admin:tw2016 -> {event:edit:*, regatta:edit:*}} This role would grant the user edit
     *            permission for every event and regatta where the tenant owner is "tw2016".
     * @param permission
     *            the permission for which to check whether it shall be granted to the user for the object whose
     *            {@code ownership} information is provided. E.g. "regatta:edit:tw2016-dyas" (would return true if the
     *            object "tw2016-dyas" would have "tw2016" as the tenant owner in case the role is qualified with the
     *            "tw2016" tenant)
     * @param ownership
     *            Ownership of the data object for which the {@code permission} is requested
     * @param permissionChecker
     *            The permissionChecker is the one that checks if one permission implies another permission. This means,
     *            it just calls {@link WildcardPermission#implies(WildcardPermission)} in most cases. In case of an any
     *            check this needs to call {@link WildcardPermission#impliesAny(WildcardPermission)).
     * @param matchOnlyNonQualifiedRolesIfNoOwnershipIsGiven
     *            If {@code false} and no ownership is given, all associated {@link Role Roles} of an user are checked.
     *            For a normal permission check, this needs to be {@code true}, because Qualified {@link Role Roles}
     *            can not grant unqualified permissions. In case of an any check it is necessary to include all
     *            qualified roles of an user, because even a qualified {@link Role} may grant any of the included
     *            permissions.
     */
    private static <RD extends RoleDefinition, R extends AbstractRole<RD, G, UR>, O extends AbstractOwnership<G, UR>, UR extends UserReference, U extends SecurityUser<RD, R, G>, G extends SecurityUserGroup<RD>, A extends SecurityAccessControlList<G>>
    boolean implies(
            R role, WildcardPermission permission, O ownership,
            BiFunction<WildcardPermission, WildcardPermission, Boolean> permissionChecker,
           boolean matchOnlyNonQualifiedRolesIfNoOwnershipIsGiven,
           boolean matchOnlyTransitiveRoles) {
       final boolean roleIsTenantQualified = role.getQualifiedForTenant() != null;
       final boolean roleIsUserQualified = role.getQualifiedForUser() != null;
       final boolean permissionsApply;
       boolean result;
       // It is possible that we have an ownership with null tenantOwner and null userOwner. This should be handled like no ownership was given.
       final boolean ownershipIsGiven = ownership != null && (ownership.getTenantOwner() != null || ownership.getUserOwner() != null);
       if (matchOnlyTransitiveRoles && !role.isTransitive()) {
           return false;
       }
       if (roleIsTenantQualified || roleIsUserQualified) {
           if (!ownershipIsGiven) {
               permissionsApply = !matchOnlyNonQualifiedRolesIfNoOwnershipIsGiven; // qualifications cannot be verified as no ownership info is provided; permissions do not apply
           } else {
               permissionsApply = (!roleIsTenantQualified || Util.equalsWithNull(role.getQualifiedForTenant(), ownership.getTenantOwner())) &&
                                  (!roleIsUserQualified || isSameUser(role.getQualifiedForUser(), ownership.getUserOwner()));
           }
       } else {
           permissionsApply = true; // permissions apply without qualifications
       }
       if (permissionsApply) {
           result = false; // if the role grants no permissions at all or no permission implies the one requested for, access is not granted
            for (WildcardPermission rolePermission : role.getPermissions()) {
                if (permissionChecker.apply(rolePermission, permission)) {
                    result = true;
                    break;
                }
            }
        } else {
            result = false;
        }
        return result;
    }
   
    /**
     * There are cases where need to check instances of different implementations if they represent the same user.
     */
    public static boolean isSameUser(UserReference user1, UserReference user2) {
        if (user1 == user2) {
            return true;
        }
        if (user1 == null || user2 == null) {
            return false;
        }
        return user1.getName().equals(user2.getName());
    }
}