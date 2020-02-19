package com.sap.sailing.server.gateway.jaxrs.api;

import java.util.Map.Entry;
import java.util.UUID;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.authz.AuthorizationException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.common.Util;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;

@Path("/v1/usergroups")
public class UserGroupsResource extends AbstractSailingServerResource {
    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getUserGroupsCurrentUserIsPartOf()
            throws ParseException, JsonDeserializationException {
        Response response = null;
        User user = getService().getSecurityService().getCurrentUser();
        if (user != null) {
            JSONObject root = new JSONObject();
            JSONArray groups = new JSONArray();
            root.put("groupsUserIsPartOf", groups);
            for (UserGroup group : user.getUserGroups()) {
                JSONObject groupJson = new JSONObject();
                groupJson.put("uuid", group.getId().toString());
                groupJson.put("name", group.getName());
                groups.add(groupJson);
            }
            response = Response.ok(root.toJSONString()).build();
        } else {
            response = Response.status(401).build();
        }
        return response;
    }

    @POST
    @Produces("application/json;charset=UTF-8")
    @Path("setDefaultTenantForCurrentServerAndUser")
    public Response setDefaultTenantForCurrentServerAndUser(@QueryParam("tenantGroup") UUID tenantId)
            throws ParseException, JsonDeserializationException {
        Response response = null;
        User user = getSecurityService().getCurrentUser();
        if (user != null) {
            getSecurityService().setDefaultTenantForCurrentServerForUser(user.getName(), tenantId);
            response = Response.ok().build();
        } else {
            response = Response.status(401).build();
        }
        return response;
    }

    @POST
    @Path("addAnyUserToGroup")
    @Produces("application/json;charset=UTF-8")
    public Response addGroupToUserWithoutPermissionOnUser(@QueryParam("userName") String userName,
            @QueryParam("groupId") UUID userGroupId) {
        Response response = null;
        final User user = getSecurityService().getUserByName(userName);
        if (user != null) {
            final UserGroup userGroup = getSecurityService().getUserGroup(userGroupId);
            if (userGroup != null) {
                if (!Util.contains(userGroup.getUsers(), user)) {
                    if (getSecurityService().hasCurrentUserUpdatePermission(userGroup)) {
                        if (!getSecurityService().hasCurrentUserMetaPermissionsOfRoleDefinitionsWithQualification(
                                userGroup.getRoleDefinitionMap().keySet(), new Ownership(null, userGroup))) {
                            response = Response.status(Status.UNAUTHORIZED).build();
                        } else {
                            getSecurityService().addUserToUserGroup(userGroup, user);
                            response = Response.ok().build();
                        }
                    } else {
                        response = Response.status(Status.UNAUTHORIZED).build();
                    }
                } else {
                    response = Response.status(Status.BAD_REQUEST).entity("User is already in this group.").build();
                }
            } else {
                response = Response.status(Status.BAD_REQUEST).entity("User Group does not exist.").build();
            }
        } else {
            response = Response.status(Status.BAD_REQUEST).entity("User does not exist.").build();
        }
        return response;
    }

    @GET
    @Path("{userGroupId}/roles")
    @Produces("application/json;charset=UTF-8")
    public Response getUserGroup(@PathParam("userGroupId") UUID userGroupId)
            throws ParseException, JsonDeserializationException {
        Response response = null;
        UserGroup userGroup = getService().getSecurityService().getUserGroup(userGroupId);
        getSecurityService().checkCurrentUserReadPermission(userGroup);
        if (userGroup != null) {
            JSONObject root = new JSONObject();
            root.put("uuid", userGroup.getId().toString());
            root.put("name", userGroup.getName());
            JSONArray roles = new JSONArray();
            for (Entry<RoleDefinition, Boolean> roleDef : userGroup.getRoleDefinitionMap().entrySet()) {
                JSONObject role = new JSONObject();
                role.put("roleId", roleDef.getKey().getId().toString());
                role.put("name", roleDef.getKey().getName());
                role.put("all", roleDef.getValue());
                roles.add(role);
            }
            root.put("roles", roles);
            response = Response.ok(root.toJSONString()).build();
        } else {
            response = Response.status(401).build();
        }
        return response;
    }
    
    @DELETE
    @Path("{userGroupId}/roles")
    @Produces("application/json;charset=UTF-8")
    public Response getUserGroup(@PathParam("userGroupId") UUID userGroupId, @QueryParam("roleId") UUID roleId)
            throws ParseException, JsonDeserializationException {
        Response response = null;
        UserGroup userGroup = getService().getSecurityService().getUserGroup(userGroupId);
        RoleDefinition roleDefinitions = getSecurityService().getRoleDefinition(roleId);
        if (userGroup != null && roleDefinitions != null) {
            getSecurityService().checkCurrentUserUpdatePermission(userGroup);
            // do not check read on role, to allow removal of not anymore visible roles
            getSecurityService().removeRoleDefintionFromUserGroup(userGroup, roleDefinitions);
            response = Response.ok().build();
        } else {
            response = Response.status(401).build();
        }
        return response;
    }

    @PUT
    @Path("{userGroupId}/roles")
    @Produces("application/json;charset=UTF-8")
    public Response addRoleToUserGroup(@PathParam("userGroupId") UUID userGroupId, @QueryParam("roleId") UUID roleId,
            @QueryParam("forAll") Boolean forAll) throws ParseException, JsonDeserializationException {
        Response response = null;
        UserGroup userGroup = getService().getSecurityService().getUserGroup(userGroupId);
        RoleDefinition roleDefinition = getSecurityService().getRoleDefinition(roleId);
        if (userGroup != null && roleDefinition != null) {
            getSecurityService().checkCurrentUserUpdatePermission(userGroup);
            getSecurityService().checkCurrentUserReadPermission(roleDefinition);
            if (!getSecurityService().hasCurrentUserMetaPermissionsOfRoleDefinitionWithQualification(roleDefinition,
                    new Ownership(null, userGroup))) {
                throw new AuthorizationException("Not permitted to add role definition to group");
            }
            getSecurityService().putRoleDefinitionToUserGroup(userGroup, roleDefinition, forAll);
            response = Response.ok().build();
        } else {
            response = Response.status(401).build();
        }
        return response;
    }
}