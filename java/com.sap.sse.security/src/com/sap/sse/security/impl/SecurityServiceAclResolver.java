package com.sap.sse.security.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.sap.sse.common.Util;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.interfaces.AccessControlStore;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.PermissionChecker.AclResolver;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.impl.AccessControlList;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.QualifiedObjectIdentifierImpl;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;

/**
 * An {@link AclResolver} implementation backed by the {@link SecurityService}'s {@link AccessControlStore}. It makes
 * use of an explicit ACL set passed, e.g., from a cache, or otherwise will use the {@link AccessControlStore} to
 * determine the ACLs that apply based on the object type, optional object IDs, as well as ownerships and that have at
 * least one denying entry. Shortcut evaluation is applied in both cases (provided or computed ACLs), looking for the
 * first ACL that matches a predicate. If such an ACL is found, {@code null} is returned. Otherwise, in the provided
 * case, the ACL set provided is returned unmodified, whereas in the computed case, the full ACL set for the context of
 * ownership, type and object IDs computed is returned.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class SecurityServiceAclResolver implements AclResolver<AccessControlList, Ownership> {
    private final AccessControlStore accessControlStore;

    SecurityServiceAclResolver(AccessControlStore accessControlStore) {
        this.accessControlStore = accessControlStore;
    }

    @Override
    public Iterable<AccessControlList> resolveDenyingAclsAndCheckIfAnyMatches(
            Ownership ownership, String type, Iterable<String> objectIdentifiersAsString,
            Predicate<AccessControlList> filterCondition, Iterable<AccessControlList> allDenyingAclsForTypeAndObjectIdsOrNull) {
        final Iterable<AccessControlList> result;
        if (allDenyingAclsForTypeAndObjectIdsOrNull == null) {
            result = enumerateAndCheckApplicableDenyingAcls(ownership, type, objectIdentifiersAsString, filterCondition);
        } else {
            for (AccessControlList acl : allDenyingAclsForTypeAndObjectIdsOrNull) {
                if (filterCondition.test(acl)) {
                    return null;
                }
            }
            result = allDenyingAclsForTypeAndObjectIdsOrNull;
        }
        return result;
    }
    
    private Iterable<AccessControlList> enumerateAndCheckApplicableDenyingAcls(Ownership ownershipSpecification, String type,
            Iterable<String> objectIdentifiersAsString, Predicate<AccessControlList> filterCondition) {
        assert type != null;
        final List<AccessControlList> result = new LinkedList<>();
        if (objectIdentifiersAsString != null && !Util.isEmpty(objectIdentifiersAsString)) {
            // the object IDs are known; look at the ACLs of those objects and search for those with denials, then apply filterCondition
            for (final String id : objectIdentifiersAsString) {
                final QualifiedObjectIdentifierImpl idOfAccessControlledObject = new QualifiedObjectIdentifierImpl(type, new TypeRelativeObjectIdentifier(id));
                final AccessControlListAnnotation acl = accessControlStore.getAccessControlList(idOfAccessControlledObject);
                if (acl != null && Util.filter(acl.getAnnotation().getDeniedActions().entrySet(), e->!e.getValue().isEmpty()).iterator().hasNext() &&
                        (ownershipSpecification == null || doesOwnershipSpecificationMatchThatOfObject(ownershipSpecification, idOfAccessControlledObject))) {
                    // found at least one denied action for at least one group and where the optional ownership specification, if provided,
                    // matches the access-controlled object's ownership
                    if (filterCondition.test(acl.getAnnotation())) {
                        return null;
                    } else {
                        result.add(acl.getAnnotation());
                    }
                }
            }
        } else {
            final Map<UserGroup, Set<QualifiedObjectIdentifier>> denyingAclsForType = accessControlStore.getAccessControlListsWithDenials(type);
            if (denyingAclsForType != null) {
                if (ownershipSpecification != null) {
                    for (final Set<QualifiedObjectIdentifier> qoids : denyingAclsForType.values()) {
                        for (final AccessControlList acl : Util.map(Util.filter(qoids,
                                    qoid->doesOwnershipSpecificationMatchThatOfObject(ownershipSpecification, qoid)),
                                    qoid->accessControlStore.getAccessControlList(qoid).getAnnotation())) {
                            if (filterCondition.test(acl)) {
                                return null;
                            } else {
                                result.add(acl);
                            }
                        }
                    }
                } else {
                    // no ownership constraints specified; use all denying ACLs for the type
                    for (final Set<QualifiedObjectIdentifier> qoids : denyingAclsForType.values()) {
                        for (final QualifiedObjectIdentifier qoid : qoids) {
                            final AccessControlListAnnotation acla = accessControlStore.getAccessControlList(qoid);
                            if (acla != null && filterCondition.test(acla.getAnnotation())) {
                                return null;
                            } else {
                                result.add(acla.getAnnotation());
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
    
    private boolean doesOwnershipSpecificationMatchThatOfObject(Ownership ownershipSpecification, QualifiedObjectIdentifier qoid) {
        final OwnershipAnnotation ownershipOfObject = accessControlStore.getOwnership(qoid);
        final User userOwnerSpecification = ownershipSpecification.getUserOwner();
        final UserGroup tenantOwnerSpecification = ownershipSpecification.getTenantOwner();
        return (userOwnerSpecification == null || (ownershipOfObject != null && Util.equalsWithNull(ownershipOfObject.getAnnotation().getUserOwner(), userOwnerSpecification)))
            && (tenantOwnerSpecification == null || (ownershipOfObject != null && Util.equalsWithNull(ownershipOfObject.getAnnotation().getTenantOwner(), tenantOwnerSpecification)));
    }
}