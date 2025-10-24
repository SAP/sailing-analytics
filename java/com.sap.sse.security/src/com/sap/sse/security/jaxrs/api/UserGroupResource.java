package com.sap.sse.security.jaxrs.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.subject.SimplePrincipalCollection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.sap.sse.common.Util;
import com.sap.sse.security.jaxrs.AbstractSecurityResource;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.shared.impl.UserGroupImpl;
import com.sun.jersey.api.client.ClientResponse.Status;

@Path(UserGroupResource.RESTSECURITY_USERGROUP)
public class UserGroupResource extends AbstractSecurityResource {
    public static final String RESTSECURITY_USERGROUP = "/restsecurity/usergroup";
    public static final String KEY_ROLE_NAME = "roleName";
    public static final String KEY_FOR_ALL = "forAll";
    public static final String KEY_ROLE_ID = "roleId";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_ROLES = "roles";
    public static final String KEY_USERS = "users";
    public static final String KEY_GROUP_NAME = "groupName";
    public static final String KEY_GROUP_ID = "groupId";
    public static final String USER = "/user";

    @Path("{groupId}")
    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getUserGroup(@PathParam(KEY_GROUP_ID) String userGroupId) {
        Response response;
        try {
            final UUID groupId = UUID.fromString(userGroupId);
            final UserGroup usergroup = getSecurityService().getUserGroup(groupId);
            response = handleExistingUserGroup(usergroup);
        } catch (IllegalArgumentException e) {
            response = Response.status(Status.BAD_REQUEST).entity("Invalid group id.").build();
        }
        return response;
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getUserGroupByName(@QueryParam(KEY_GROUP_NAME) String userGroupName) {
        Response response;
        if (userGroupName != null) {
            final UserGroup usergroup = getSecurityService().getUserGroupByName(userGroupName);
            response = handleExistingUserGroup(usergroup);
        } else {
            response = Response.status(Status.BAD_REQUEST).entity("Please specify a groupname.").build();
        }

        return response;
    }

    @GET
    @Path("list/groupsUserIsPartOf")
    @Produces("application/json;charset=UTF-8")
    public Response getUserGroupsCurrentUserIsPartOf() throws ParseException {
        Response response = null;
        User user = getSecurityService().getCurrentUser();
        if (user != null) {
            JSONObject root = new JSONObject();
            JSONArray groups = new JSONArray();
            root.put("groupsUserIsPartOf", groups);
            for (UserGroup group : user.getUserGroups()) {
                groups.add(convertUserGroupToJson(group, /* includingUSers */ false, /* includingRoles */ false));
            }
            response = Response.ok(streamingOutput(root)).build();
        } else {
            response = Response.status(401).build();
        }
        return response;
    }

    @GET
    @Path("list/readable")
    @Produces("application/json;charset=UTF-8")
    public Response getReadableUserGroupsForCurrentUser(@QueryParam("userName") String userName) {
        Response response = null;
        final User user = userName != null ? getSecurityService().getUserByName(userName) : getSecurityService().getCurrentUser();
        if (user != null && getSecurityService().hasCurrentUserReadPermission(user)) {
            final List<UserGroup> userGroups = new ArrayList<>();
            getSecurityService().getUserGroupList().forEach(ug -> {
                if (getSecurityService().getSecurityManager().isPermitted(
                        new SimplePrincipalCollection(user.getName(), user.getName()),
                        ug.getIdentifier().getPermission(DefaultActions.READ).toString())) {
                    userGroups.add(ug);
                }
            });
            final JSONObject root = new JSONObject();
            final JSONArray userGroupsJson = new JSONArray();
            userGroups.forEach(ug -> userGroupsJson
                    .add(convertUserGroupToJson(ug, /* includingUsers */ false, /* includingRoles */ true)));
            response = Response.ok(root.toJSONString()).build();
            root.put("readableGroups", userGroupsJson);
            response = Response.ok().entity(streamingOutput(root)).build();
        } else {
            response = Response.status(Status.UNAUTHORIZED).build();
        }
        return response;
    }

    private Response handleExistingUserGroup(final UserGroup usergroup) {
        Response response;
        if (usergroup == null) {
            response = Response.status(Status.BAD_REQUEST).entity("Usergroup with this id does not exist.").build();
        } else {
            if (getSecurityService().hasCurrentUserReadPermission(usergroup)) {
                final JSONObject jsonResult = convertUserGroupToJson(usergroup, /* includingUsers */ true,
                        /* includingRoles */ true);
                response = Response.ok(streamingOutput(jsonResult)).build();
            } else {
                response = Response.status(Status.UNAUTHORIZED).build();
            }
        }
        return response;
    }

    /** Returns a json object with id, groupname, roles and user names (filtered to thouse the current user can see). */
    private JSONObject convertUserGroupToJson(final UserGroup usergroup, boolean includingUsers,
            boolean includingRoles) {
        final JSONObject jsonResult = new JSONObject();
        jsonResult.put(KEY_GROUP_ID, usergroup.getId().toString());
        jsonResult.put(KEY_GROUP_NAME, usergroup.getName());
        if (includingUsers) {
            final JSONArray jsonUsersInGroup = new JSONArray();
            for (final User user : usergroup.getUsers()) {
                // filter users
                if (getSecurityService().hasCurrentUserReadPermission(user)) {
                    jsonUsersInGroup.add(user.getId());
                }
            }
            jsonResult.put(KEY_USERS, jsonUsersInGroup);
        }
        if (includingRoles) {
            final JSONArray jsonRolesOfGroup = new JSONArray();
            for (final Map.Entry<RoleDefinition, Boolean> roleDefinition : usergroup.getRoleDefinitionMap()
                    .entrySet()) {
                // filter users
                if (getSecurityService().hasCurrentUserReadPermission(roleDefinition.getKey())) {
                    JSONObject roleJson = new JSONObject();
                    roleJson.put(KEY_ROLE_ID, roleDefinition.getKey().getId().toString());
                    roleJson.put(KEY_ROLE_NAME, roleDefinition.getKey().getName());
                    roleJson.put(KEY_FOR_ALL, roleDefinition.getValue());
                    jsonRolesOfGroup.add(roleJson);
                }
            }
            jsonResult.put(KEY_ROLES, jsonRolesOfGroup);
        }
        return jsonResult;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/json;charset=UTF-8")
    public Response createUserGroup(String jsonBody) {
        final Response response;
        final JSONObject json = (JSONObject) JSONValue.parse(jsonBody);
        final String groupName = (String) json.get(KEY_GROUP_NAME);
        final UserGroup existingUserGroup = getSecurityService().getUserGroupByName(groupName);
        if (existingUserGroup != null) {
            response = Response.status(Status.BAD_REQUEST).entity("Usergroup with this name already exists.").build();
        } else {
            UUID newTenantId = UUID.randomUUID();
            UserGroup group = getSecurityService().setOwnershipWithoutCheckPermissionForObjectCreationAndRevertOnError(
                    SecuredSecurityTypes.USER_GROUP, UserGroupImpl.getTypeRelativeObjectIdentifier(newTenantId),
                    groupName, () -> {
                        UserGroup userGroup;
                        try {
                            userGroup = getSecurityService().createUserGroup(newTenantId, groupName);
                        } catch (UserGroupManagementException e) {
                            throw new UserGroupManagementException(e.getMessage());
                        }
                        return userGroup;
                    });
            if (group == null) {
                response = Response.status(Status.INTERNAL_SERVER_ERROR).entity("Could not create user group.").build();
            } else {
                response = Response.status(Status.CREATED).entity(streamingOutput(convertUserGroupToJson(group, /* includingUsers */ true, /* includingRoles */ true))).build();
            }
        }
        return response;
    }

    @Path("{groupId}")
    @DELETE
    @Produces("application/json;charset=UTF-8")
    public Response deleteUserGroup(@PathParam(KEY_GROUP_ID) String userGroupId) {
        Response response;
        try {
            final UUID groupId = UUID.fromString(userGroupId);
            final UserGroup usergroup = getSecurityService().getUserGroup(groupId);

            if (usergroup == null) {
                response = Response.status(Status.BAD_REQUEST).entity("Usergroup with this name does not exist.")
                        .build();
            } else {
                getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(usergroup, () -> {
                    getSecurityService().deleteUserGroup(usergroup);
                });
                response = Response.status(Status.NO_CONTENT).build();
            }
        } catch (IllegalArgumentException e) {
            response = Response.status(Status.BAD_REQUEST).entity("Invalid group id.").build();
        }

        return response;
    }

    @Path("{groupId}"+USER+"/{username}")
    @PUT
    @Produces("application/json;charset=UTF-8")
    public Response addUserToUserGroup(@PathParam(KEY_GROUP_ID) String userGroupId,
            @PathParam(KEY_USERNAME) String username) {
        Response response;
        try {
            final UUID groupId = UUID.fromString(userGroupId);
            final UserGroup usergroup = getSecurityService().getUserGroup(groupId);

            if (usergroup == null) {
                response = Response.status(Status.BAD_REQUEST).entity("Usergroup with this name does not exist.")
                        .build();
            } else {
                final User user = getSecurityService().getUserByName(username);
                if (user == null) {
                    response = Response.status(Status.BAD_REQUEST).entity("User with this name does not exist.")
                            .build();
                } else {
                    if (getSecurityService().hasCurrentUserReadPermission(usergroup)) {
                        if (Util.contains(usergroup.getUsers(), user)) {
                            response = Response.status(Status.BAD_REQUEST).entity("User is already in this group.")
                                    .build();
                        } else {
                            if (getSecurityService().hasCurrentUserUpdatePermission(usergroup)) {
                                if (!getSecurityService().hasCurrentUserMetaPermissionsOfRoleDefinitionsWithQualification(
                                        usergroup.getRoleDefinitionMap().keySet(), new Ownership(null, usergroup))) {
                                    response = Response.status(Status.UNAUTHORIZED).build();
                                } else {
                                    getSecurityService().addUserToUserGroup(usergroup, user);
                                    response = Response.ok().build();
                                }
                            } else {
                                response = Response.status(Status.UNAUTHORIZED).build();
                            }
                        }
                    } else {
                        response = Response.status(Status.UNAUTHORIZED).build();
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            response = Response.status(Status.BAD_REQUEST).entity("Invalid group id.").build();
        }
        return response;
    }

    @Path("{groupId}/user/{username}")
    @DELETE
    @Produces("application/json;charset=UTF-8")
    public Response deleteUserFromUserGroup(@PathParam(KEY_GROUP_ID) String userGroupId,
            @PathParam(KEY_USERNAME) String username) {
        Response response;
        try {
            final UUID groupId = UUID.fromString(userGroupId);
            final UserGroup usergroup = getSecurityService().getUserGroup(groupId);
            if (usergroup == null) {
                response = Response.status(Status.BAD_REQUEST).entity("Usergroup with this name does not exist.")
                        .build();
            } else {
                final User user = getSecurityService().getUserByName(username);
                if (user == null) {
                    response = Response.status(Status.BAD_REQUEST).entity("User with this name does not exist.")
                            .build();
                } else {
                    if (getSecurityService().hasCurrentUserReadPermission(usergroup)) {
                        if (!Util.contains(usergroup.getUsers(), user)) {
                            response = Response.status(Status.BAD_REQUEST).entity("User is not in this group.").build();
                        } else {
                            if (getSecurityService().hasCurrentUserUpdatePermission(usergroup)) {
                                getSecurityService().removeUserFromUserGroup(usergroup, user);
                                response = Response.status(Status.NO_CONTENT).build();
                            } else {
                                response = Response.status(Status.UNAUTHORIZED).build();
                            }

                        }
                    } else {
                        response = Response.status(Status.UNAUTHORIZED).build();
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            response = Response.status(Status.BAD_REQUEST).entity("Invalid group id.").build();
        }
        return response;
    }

    @Path("{groupId}/role/{roleId}")
    @PUT
    @Produces("application/json;charset=UTF-8")
    public Response addRoleToUserGroup(@PathParam(KEY_GROUP_ID) String userGroupId,
            @PathParam(KEY_ROLE_ID) String roleIdString, String body) {
        Response response;
        try {
            final UUID groupId = UUID.fromString(userGroupId);
            final UUID roleId = UUID.fromString(roleIdString);
            final UserGroup usergroup = getSecurityService().getUserGroup(groupId);
            if (usergroup == null) {
                response = Response.status(Status.BAD_REQUEST).entity(
                        String.format("Usergroup with ID %s does not exist.", userGroupId)).build();
            } else {
                final RoleDefinition role = getSecurityService().getRoleDefinition(roleId);
                if (role == null) {
                    response = Response.status(Status.BAD_REQUEST).entity(String.format("Role with ID %s does not exist.", roleId)).build();
                } else {
                    if (getSecurityService().hasCurrentUserUpdatePermission(usergroup)
                            && getSecurityService().hasCurrentUserReadPermission(role)) {
                        final Boolean forAll = usergroup.getRoleAssociation(role);
                        JSONObject jsonBody = (JSONObject) JSONValue.parse(body);
                        boolean roleForAll = Boolean.valueOf((String) jsonBody.get(KEY_FOR_ALL));
                        if (forAll == null || forAll != roleForAll) {
                            if (!getSecurityService().hasCurrentUserMetaPermissionsOfRoleDefinitionWithQualification(role,
                                    new Ownership(null, usergroup))) {
                                response = Response.status(Status.UNAUTHORIZED)
                                        .entity(String.format("Not permitted to add role %s to group %s",
                                                role.getName(), usergroup.getName())).build();
                            } else {
                                getSecurityService().putRoleDefinitionToUserGroup(usergroup, role, roleForAll);
                                response = Response.ok().build();
                            }
                        } else {
                            response = Response.status(Status.BAD_REQUEST)
                                    .entity(String.format("Role %s was already added to group %s.", role.getName(),
                                            usergroup.getName())).build();
                        }
                    } else {
                        response = Response.status(Status.UNAUTHORIZED).build();
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            response = Response.status(Status.BAD_REQUEST).entity("Invalid role or group id.").build();
        }
        return response;
    }

    @Path("{groupId}/role/{roleId}")
    @DELETE
    @Produces("application/json;charset=UTF-8")
    public Response deleteRoleFromUserGroup(@PathParam(KEY_GROUP_ID) String userGroupId,
            @PathParam(KEY_ROLE_ID) String roleIdString) {
        Response response;
        try {
            final UUID groupId = UUID.fromString(userGroupId);
            final UserGroup usergroup = getSecurityService().getUserGroup(groupId);
            final UUID roleId = UUID.fromString(roleIdString);
            if (usergroup == null) {
                response = Response.status(Status.BAD_REQUEST).entity("Usergroup with this name does not exist.")
                        .build();
            } else {
                final RoleDefinition role = getSecurityService().getRoleDefinition(roleId);
                if (role == null) {
                    response = Response.status(Status.BAD_REQUEST).entity("Role with this id does not exist.").build();
                } else {
                    if (getSecurityService().hasCurrentUserUpdatePermission(usergroup)
                            && getSecurityService().hasCurrentUserReadPermission(role)) {
                        if (usergroup.getRoleAssociation(role) != null) {
                            if (!getSecurityService().hasCurrentUserMetaPermissionsOfRoleDefinitionWithQualification(role,
                                    new Ownership(null, usergroup))) {
                                response = Response
                                        .status(Status.UNAUTHORIZED).entity(String
                                                .format("Not permitted to remove role %s from group", role.getName()))
                                        .build();
                            } else {
                                getSecurityService().removeRoleDefintionFromUserGroup(usergroup, role);
                                response = Response.status(Status.NO_CONTENT).build();
                            }
                        } else {
                            response = Response.status(Status.BAD_REQUEST)
                                    .entity("Role is currently not added to the group.").build();
                        }
                    } else {
                        response = Response.status(Status.UNAUTHORIZED).build();
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            response = Response.status(Status.BAD_REQUEST).entity("Invalid role or group id.").build();
        }
        return response;
    }

    @POST
    @Produces("application/json;charset=UTF-8")
    @Path("setDefaultTenantForCurrentServerAndUser")
    public Response setDefaultTenantForCurrentServerAndUser(@QueryParam("tenantGroup") UUID tenantId)
            throws ParseException {
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
}
