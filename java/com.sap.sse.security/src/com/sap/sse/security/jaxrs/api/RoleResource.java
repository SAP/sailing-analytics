package com.sap.sse.security.jaxrs.api;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.authz.UnauthorizedException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sse.security.jaxrs.AbstractSecurityResource;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;

@Path("/restsecurity/role")
public class RoleResource extends AbstractSecurityResource {

    private static final String KEY_PERMISSIONS = "permissions";
    private static final String KEY_ROLE_ID = "roleId";
    private static final String KEY_ROLE_NAME = "roleName";

    @POST
    @Produces("application/json;charset=UTF-8")
    public Response createRole(@FormParam(KEY_ROLE_NAME) String roleName) {
        final String roleDefinitionIdAsString = UUID.randomUUID().toString();
        final RoleDefinition role = getSecurityService().setOwnershipWithoutCheckPermissionForObjectCreationAndRevertOnError(
                SecuredSecurityTypes.ROLE_DEFINITION, new TypeRelativeObjectIdentifier(roleDefinitionIdAsString),
                roleName, new Callable<RoleDefinition>() {
                    @Override
                    public RoleDefinition call() throws Exception {
                        return getSecurityService().createRoleDefinition(UUID.fromString(roleDefinitionIdAsString), roleName);
                    }
                });
        final Response resp;
        if (role == null) {
            resp = Response.status(Status.INTERNAL_SERVER_ERROR).entity("Role creation failed.").build();
        } else {
            final JSONObject jsonResult = new JSONObject();
            jsonResult.put(KEY_PERMISSIONS, new JSONArray());
            jsonResult.put(KEY_ROLE_ID, role.getId().toString());
            jsonResult.put(KEY_ROLE_NAME, role.getName());
            resp = Response.status(Status.CREATED).entity(streamingOutput(jsonResult)).build();
        }
        return resp;
    }

    @Path("{roleId}")
    @DELETE
    @Produces("text/plain;charset=UTF-8")
    public Response deleteRole(@PathParam(KEY_ROLE_ID) String roleId) {
        Response resp;
        try {
            // parse UUID
            final UUID roleUUID = UUID.fromString(roleId);
            // get role definition from role id
            final RoleDefinition roleDefinition = getSecurityService().getRoleDefinition(roleUUID);
            // null check role definition
            if (roleDefinition == null) {
                resp = Response.status(Status.NOT_FOUND).entity(String.format("No role with id '%s' found.", roleUUID))
                        .build();
            } else {
                // check delete permission and remove role
                getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(roleDefinition, () -> {
                    getSecurityService().deleteRoleDefinition(roleDefinition);
                });
                resp = Response.status(Status.NO_CONTENT).build();
            }
        } catch (IllegalArgumentException e) {
            resp = Response.status(Status.BAD_REQUEST).entity("Invalid roleId.").build();
        } catch (UnauthorizedException e) {
            resp = Response.status(Status.UNAUTHORIZED).build();
        }
        return resp;
    }

    @Path("{roleId}")
    @PUT
    @Produces("text/plain;charset=UTF-8")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRole(@PathParam(KEY_ROLE_ID) String roleId, String json) {
        Response resp;
        try {
            // parse UUID
            final UUID roleUUID = UUID.fromString(roleId);
            // get role definition from role id
            final RoleDefinition roleDefinition = getSecurityService().getRoleDefinition(roleUUID);
            // null check role definition
            if (roleDefinition == null) {
                resp = Response.status(Status.NOT_FOUND).entity(String.format("No role with id '%s' found.", roleUUID))
                        .build();
            } else {
                // check update permission on role
                getSecurityService().checkCurrentUserUpdatePermission(roleDefinition);
                // create permission objects
                final Set<WildcardPermission> permissions = new HashSet<>();
                final JSONObject body = (JSONObject) new JSONParser().parse(json);
                @SuppressWarnings("unchecked")
                final Iterable<String> permissionStrings = (Iterable<String>) body.get(KEY_PERMISSIONS);
                for (final String permissionString : permissionStrings) {
                    permissions.add(new WildcardPermission(permissionString));
                }
                // check only those meta-permissions which changed
                final Set<WildcardPermission> addedPermissions = new HashSet<>(roleDefinition.getPermissions());
                addedPermissions.removeAll(permissions);
                final Set<WildcardPermission> removedPermissions = new HashSet<>(permissions);
                removedPermissions.removeAll(roleDefinition.getPermissions());
                if (!getSecurityService().hasUserAllWildcardPermissionsForAlreadyRealizedQualifications(roleDefinition,
                        addedPermissions)) {
                    resp = Response.status(Status.UNAUTHORIZED)
                            .entity("Not permitted to grant permissions for role " + roleDefinition.getName()).build();
                } else if (!getSecurityService().hasUserAllWildcardPermissionsForAlreadyRealizedQualifications(roleDefinition,
                        removedPermissions)) {
                    resp = Response.status(Status.UNAUTHORIZED)
                            .entity("Not permitted to revoke permissions for role " + roleDefinition.getName()).build();
                } else {
                    // update role definitino with new permissions
                    roleDefinition.setPermissions(permissions);
                    final String roleName = (String) body.get(KEY_ROLE_NAME);
                    if (roleName != null) {
                        roleDefinition.setName(roleName);
                    }
                    getSecurityService().updateRoleDefinition(roleDefinition);
                    resp = Response.ok().build();
                }
            }
        } catch (IllegalArgumentException | ParseException e) {
            resp = Response.status(Status.BAD_REQUEST).entity("Invalid roleId.").build();
        } catch (UnauthorizedException e) {
            resp = Response.status(Status.UNAUTHORIZED).build();
        }
        return resp;
    }

    @GET
    @Path("{roleId}")
    @Produces("application/json;charset=UTF-8")
    public Response getRole(@PathParam(KEY_ROLE_ID) String roleId) {
        Response resp;
        try {
            // parse UUID
            final UUID roleUUID = UUID.fromString(roleId);
            // get role definition from role id
            final RoleDefinition roleDefinition = getSecurityService().getRoleDefinition(roleUUID);
            // null check role definition
            if (roleDefinition == null) {
                resp = Response.status(Status.NOT_FOUND).entity(String.format("No role with id '%s' found.", roleUUID))
                        .build();
            } else {
                // check read permission on role
                getSecurityService().checkCurrentUserReadPermission(roleDefinition);
                // build json result with permissions and id
                final JSONObject jsonResult = new JSONObject();
                final JSONArray jsonPermissions = new JSONArray();
                for (final WildcardPermission permission : roleDefinition.getPermissions()) {
                    jsonPermissions.add(permission.toString());
                }
                jsonResult.put(KEY_PERMISSIONS, jsonPermissions);
                jsonResult.put(KEY_ROLE_ID, roleId);
                jsonResult.put(KEY_ROLE_NAME, roleDefinition.getName());
                resp = Response.ok(streamingOutput(jsonResult)).build();
            }
        } catch (IllegalArgumentException e) {
            resp = Response.status(Status.BAD_REQUEST).entity("Invalid roleId.").build();
        } catch (UnauthorizedException e) {
            resp = Response.status(Status.UNAUTHORIZED).build();
        }
        return resp;
    }
}
