package com.sap.sse.security.jaxrs.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.SecurityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.sap.sse.security.exceptions.OwnershipException;
import com.sap.sse.security.jaxrs.AbstractSecurityResource;
import com.sap.sse.security.model.GeneralResponse;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.impl.QualifiedObjectIdentifierImpl;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;

@Path(OwnershipResource.RESTSECURITY_OWNERSHIP)
public class OwnershipResource extends AbstractSecurityResource {
    public static final String RESTSECURITY_OWNERSHIP = "/restsecurity/ownership";
    public static final String KEY_OBJECT_ID = "objectId";
    public static final String KEY_OBJECT_TYPE = "objectType";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_GROUP_ID = "groupId";
    public static final String KEY_ACL = "acl";
    public static final String KEY_ACTIONS = "actions";
    public static final String KEY_DISPLAY_NAME = "displayName";

    @Path("{objectType}/{typeRelativeObjectId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/json;charset=UTF-8")
    public Response setOwnership(@PathParam("objectType") String objectType,
            @PathParam("typeRelativeObjectId") String typeRelativeObjectId, String jsonBody) throws OwnershipException {
        final JSONObject json = (JSONObject) JSONValue.parse(jsonBody);
        QualifiedObjectIdentifier identifier = new QualifiedObjectIdentifierImpl(objectType,
                new TypeRelativeObjectIdentifier(typeRelativeObjectId));
        try {
            SecurityUtils.getSubject().checkPermission(identifier.getStringPermission(DefaultActions.CHANGE_OWNERSHIP));
        } catch (Exception ex) {
            throw new OwnershipException("Not permitted to change ownership.", Status.FORBIDDEN);
        }
        final OwnershipAnnotation existingOwnership = getSecurityService().getOwnership(identifier);
        final User user;
        if (json.containsKey(KEY_USERNAME)) {
            final String username = (String) json.get(KEY_USERNAME);
            if (username == null) {
                user = null;
            } else {
                user = getSecurityService().getUserByName(username);
                if (user == null) {
                    // username provided but cannot be resolved
                    throw new OwnershipException("User Not found", Status.BAD_REQUEST);
                }
            }
        } else {
            user = existingOwnership == null ? null : existingOwnership.getAnnotation().getUserOwner();
        }
        final UserGroup userGroup;
        if (json.containsKey(KEY_GROUP_ID)) {
            final String userGroupIdAsString = (String) json.get(KEY_GROUP_ID);
            if (userGroupIdAsString == null) {
                userGroup = null;
            } else {
                userGroup = getSecurityService().getUserGroup(UUID.fromString(userGroupIdAsString));
                if (userGroup == null) {
                    throw new OwnershipException("UserGroup Not found", Status.BAD_REQUEST);
                }
            }
        } else {
            userGroup = existingOwnership == null ? null : existingOwnership.getAnnotation().getTenantOwner();
        }
        if (json.containsKey(KEY_DISPLAY_NAME)) {
            getSecurityService().setOwnership(identifier, user, userGroup, (String) json.get(KEY_DISPLAY_NAME));
        } else {
            getSecurityService().setOwnership(identifier, user, userGroup);
        }
        return Response.ok(new GeneralResponse(true, "Ownership changed successfully").toString()).build();
    }

    @Path("{objectType}/{typeRelativeObjectId}")
    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getOwnership(@PathParam("objectType") String objectType,
            @PathParam("typeRelativeObjectId") String typeRelativeObjectId) throws OwnershipException {
        return getOwnership(objectType, new String[] { typeRelativeObjectId });
    }

    @Path("{objectType}")
    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getOwnershipWithMultiId(@PathParam("objectType") String objectType,
            @QueryParam("id") List<String> compositeTypeRelativeObjectId) throws OwnershipException {
        return getOwnership(objectType, compositeTypeRelativeObjectId.toArray(new String[0]));
    }

    private Response getOwnership(String objectType, final String[] typeRelativeObjectIdArray) {
        QualifiedObjectIdentifier identifier = new QualifiedObjectIdentifierImpl(objectType,
                new TypeRelativeObjectIdentifier(typeRelativeObjectIdArray));
        final OwnershipAnnotation existingOwnership = getSecurityService().getOwnership(identifier);
        final JSONObject result = new JSONObject();
        result.put(KEY_OBJECT_TYPE, objectType);
        result.put(KEY_OBJECT_ID, identifier.getTypeRelativeObjectIdentifier().toString());
        result.put(KEY_GROUP_ID, existingOwnership == null || existingOwnership.getAnnotation().getTenantOwner() == null ? null : existingOwnership.getAnnotation().getTenantOwner().getId().toString());
        result.put(KEY_USERNAME, existingOwnership == null || existingOwnership.getAnnotation().getUserOwner() == null ? null : existingOwnership.getAnnotation().getUserOwner().getName());
        result.put(KEY_DISPLAY_NAME, existingOwnership == null || existingOwnership.getDisplayNameOfAnnotatedObject() == null ? null : existingOwnership.getDisplayNameOfAnnotatedObject());
        return Response.ok(streamingOutput(result)).build();
    }

    @Path("{objectType}/{typeRelativeObjectId}/"+KEY_ACL)
    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getAccessControlLists(@PathParam("objectType") String objectType,
            @PathParam("typeRelativeObjectId") String typeRelativeObjectId) throws OwnershipException {
        QualifiedObjectIdentifier identifier = new QualifiedObjectIdentifierImpl(objectType,
                new TypeRelativeObjectIdentifier(typeRelativeObjectId));
        final AccessControlListAnnotation acl = getSecurityService().getAccessControlList(identifier);
        final JSONObject result = new JSONObject();
        result.put(KEY_OBJECT_TYPE, objectType);
        result.put(KEY_OBJECT_ID, typeRelativeObjectId);
        result.put(KEY_DISPLAY_NAME, acl == null || acl.getDisplayNameOfAnnotatedObject() == null ? null : acl.getDisplayNameOfAnnotatedObject());
        final JSONArray actionsByUserGroup = new JSONArray();
        result.put(KEY_ACL, actionsByUserGroup);
        if (acl != null && acl.getAnnotation() != null) {
            for (final Entry<UserGroup, Set<String>> e : acl.getAnnotation().getActionsByUserGroup().entrySet()) {
                final JSONObject actionsForGroup = new JSONObject();
                actionsByUserGroup.add(actionsForGroup);
                actionsForGroup.put(KEY_GROUP_ID, e.getKey() == null ? null : e.getKey().getId().toString());
                final JSONArray actions = new JSONArray();
                actions.addAll(e.getValue());
                actionsForGroup.put(KEY_ACTIONS, actions);
            }
        }
        return Response.ok(streamingOutput(result)).build();
    }

    @Path("{objectType}/{typeRelativeObjectId}/"+KEY_ACL)
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/json;charset=UTF-8")
    public Response setAccessControlLists(@PathParam("objectType") String objectType,
            @PathParam("typeRelativeObjectId") String typeRelativeObjectId, String jsonBody) throws OwnershipException {
        final JSONObject json = (JSONObject) JSONValue.parse(jsonBody);
        final JSONArray actionsByUserGroupJson = (JSONArray) json.get(KEY_ACL);
        QualifiedObjectIdentifier identifier = new QualifiedObjectIdentifierImpl(objectType,
                new TypeRelativeObjectIdentifier(typeRelativeObjectId));
        try {
            SecurityUtils.getSubject().checkPermission(identifier.getStringPermission(DefaultActions.CHANGE_ACL));
        } catch (Exception ex) {
            throw new OwnershipException("Not permitted to change ownership.", Status.FORBIDDEN);
        }
        final Map<UserGroup, Set<String>> actionsByUserGroup = new HashMap<>();
        for (final Object o : actionsByUserGroupJson) {
            final JSONObject permissionsForGroup = (JSONObject) o;
            final String groupIdAsString = (String) permissionsForGroup.get(KEY_GROUP_ID);
            final UserGroup group;
            if (groupIdAsString == null) {
                group = null;
            } else {
                group = getSecurityService().getUserGroup(UUID.fromString(groupIdAsString));
                if (group == null) {
                    throw new OwnershipException(String.format("UserGroup with ID %s Not found", groupIdAsString), Status.BAD_REQUEST);
                }
            }
            final JSONArray actions = (JSONArray) permissionsForGroup.get(KEY_ACTIONS);
            final Set<String> actionsForGroup = new HashSet<>();
            for (final Object action : actions) {
                actionsForGroup.add(action.toString());
            }
            actionsByUserGroup.put(group, actionsForGroup);
        }
        if (json.containsKey(KEY_DISPLAY_NAME)) {
            final String displayName = (String) json.get(KEY_DISPLAY_NAME);
            getSecurityService().overrideAccessControlList(identifier, actionsByUserGroup, displayName);
        } else {
            getSecurityService().overrideAccessControlList(identifier, actionsByUserGroup);
        }
        return Response.ok(new GeneralResponse(true, "ACL changed successfully").toString()).build();
    }
}
