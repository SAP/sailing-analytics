package com.sap.sse.security.jaxrs.api;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.security.Action;
import com.sap.sse.security.SecurityUrlPathProvider;
import com.sap.sse.security.jaxrs.AbstractSecurityResource;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.PermissionAndRoleAssociation;
import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.util.HttpRequestUtils;
import com.sun.jersey.api.client.ClientResponse.Status;

@Path(SecurityResource.RESTSECURITY)
public class SecurityResource extends AbstractSecurityResource {
    private static final Logger logger = Logger.getLogger(SecurityResource.class.getName());
    
    public static final String USERS_WITH_PERMISSION_METHOD = "/users_with_permission";
    public static final String HELLO_METHOD = "/hello";
    public static final String CHANGE_PASSWORD_METHOD = "/change_password";
    public static final String FORGOT_PASSWORD_METHOD = "/forgot_password";
    public static final String CREATE_USER_METHOD = "/create_user";
    public static final String USER_METHOD = "/user";
    public static final String HAS_PERMISSION_METHOD = "/has_permission";
    public static final String REMOVE_ACCESS_TOKEN_METHOD = "/remove_access_token";
    public static final String LOGOUT_METHOD = "/logout";
    /**
     * The path to put behind the application's prefix {@code /security/api} to reach this resource
     */
    public static final String RESTSECURITY = "/restsecurity";
    public static final String COMPANY = "company";
    public static final String FULL_NAME = "fullName";
    public static final String EMAIL = "email";
    private static final String SECURITY_UI_URL_PATH = "/security/ui/";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String PERMISSION = "permission";
    public static final String GRANTED = "granted";
    public static final String ACCESS_TOKEN_METHOD = "/"+ACCESS_TOKEN;
    public static final String ADD_ROLE_TO_USER_METHOD = "/add_role_to_user";
    public static final String ROLE_DEFINITION_ID = "role_definition_id";
    public static final String QUALIFYING_GROUP_ID = "qualifying_group_id";
    public static final String QUALIFYING_USERNAME = "qualifying_username";
    public static final String TRANSITIVE = "transitive";
    public static final String GET_ROLES_FOR_USER_METHOD = "/get_roles_for_user";
    public static final Object ROLE_NAME = "role_name";
    public static final Object OWNING_GROUP_ID = "owning_group_id";
    public static final Object OWNING_USER_NAME = "owning_user_name";

    /**
     * Can be used to figure out the current subject. Accepts the GET method. If the subject is
     * authenticated, the service will respond with a "Hello &lt;subjectname&gt;" message, otherwise
     * with a generic "Hello!".
     */
    @GET
    @Path(HELLO_METHOD)
    @Produces("application/json;charset=UTF-8")
    public Response sayHello() {
        return doSayHello();
    }
    
    @GET
    @Path(USERS_WITH_PERMISSION_METHOD)
    @Produces("text/plain;charset=UTF-8")
    public Response getUsersWithPermission(@QueryParam(PERMISSION) String permission) {
        final TimePoint start = TimePoint.now();
        try {
            final WildcardPermission wildcardPermission = new WildcardPermission(permission);
            final Iterable<User> usersWithPermission = getSecurityService().getUsersWithPermissions(wildcardPermission);
            final JSONArray usernames = new JSONArray();
            for (final User userWithPermission : usersWithPermission) {
                if (getSecurityService().hasCurrentUserReadPermission(userWithPermission)) {
                    usernames.add(userWithPermission.getName());
                }
            }
            logger.fine(()->"Request for users with permission took "+start.until(TimePoint.now()));
            return Response.status(Status.OK).entity(streamingOutput(usernames)).build();
        } catch (Exception e) {
            return Response.status(Status.PRECONDITION_FAILED).entity(e.getMessage()).build();
        }
    }

    private Response doSayHello() {
        final Subject subject = SecurityUtils.getSubject();
        final JSONObject result = new JSONObject();
        result.put("principal", subject.getPrincipal().toString());
        result.put("authenticated", subject.isAuthenticated());
        result.put("remembered", subject.isRemembered());
        return Response.ok(streamingOutput(result), MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * Can be used to figure out the current subject. Accepts the POST method. If the subject is
     * authenticated, the service will respond with a "Hello &lt;subjectname&gt;" message, otherwise
     * with a generic "Hello!".
     */
    @POST
    @Path(HELLO_METHOD)
    @Produces("text/plain;charset=UTF-8")
    public Response sayHelloPost() {
        return doSayHello();
    }
    
    @POST
    @Path(CHANGE_PASSWORD_METHOD)
    @Produces("text/plain;charset=UTF-8")
    public Response changePassword(@FormParam(USERNAME) String username, @FormParam(PASSWORD) String password) {
        if (!getSecurityService().hasCurrentUserUpdatePermission(getSecurityService().getUserByName(username))) {
            return Response.status(Status.UNAUTHORIZED).build();
        } else {
            try {
                getSecurityService().updateSimpleUserPassword(username, password);
                return Response.ok().build();
            } catch (UserManagementException e) {
                return Response.status(Status.PRECONDITION_FAILED).entity(e.getMessage()).build();
            }
        }
    }

    @POST
    @Path(FORGOT_PASSWORD_METHOD)
    @Produces("text/plain;charset=UTF-8")
    public Response forgotPassword(@Context UriInfo uriInfo, @QueryParam(USERNAME) String username,
            @QueryParam(EMAIL) String email, @QueryParam("application") String application) {
        try {
            final User user;
            if (username != null) {
                user = getSecurityService().getUserByName(username);
            } else if (email != null) {
                user = getSecurityService().getUserByEmail(email);
            } else {
                return Response.status(Status.PRECONDITION_FAILED).entity("username or email must be provided").build();
            }
            if (user == null) {
                return Response.status(Status.PRECONDITION_FAILED).entity("user not found").build();
            } else {
                getSecurityService().resetPassword(user.getName(), getPasswordResetURL(uriInfo, application));
                return Response.ok().build();
            }
        } catch (UserManagementException | MailException e) {
            return Response.status(Status.PRECONDITION_FAILED).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path(CREATE_USER_METHOD)
    @Produces("text/plain;charset=UTF-8")
    public Response createUser(@Context UriInfo uriInfo,
            @QueryParam(USERNAME) String queryUsername, @FormParam(USERNAME) String formUsername,
            @QueryParam(EMAIL) String queryEmail, @FormParam(EMAIL) String formEmail,
            @QueryParam(PASSWORD) String queryPassword, @FormParam(PASSWORD) String formPassword,
            @QueryParam(FULL_NAME) String queryFullName, @FormParam(FULL_NAME) String formFullName,
            @QueryParam(COMPANY) String queryCompany, @FormParam(COMPANY) String formCompany,
            @Context HttpServletRequest request) {
        try {
            final String clientIP = HttpRequestUtils.getClientIP(request);
            User user = getSecurityService().checkPermissionForUserCreationAndRevertOnErrorForUserCreation(queryUsername,
                    new Callable<User>() {
                        @Override
                        public User call() throws Exception {
                            final String validationBaseURL = getEmailValidationBaseURL(uriInfo);
                            final String usernameToUse = preferFirstIfNotNullOrElseSecond(formUsername, queryUsername);
                            final String passwordToUse = preferFirstIfNotNullOrElseSecond(formPassword, queryPassword);
                            final String emailToUse = preferFirstIfNotNullOrElseSecond(formEmail, queryEmail);
                            final String fullNameToUse = preferFirstIfNotNullOrElseSecond(formFullName, queryFullName);
                            final String companyToUse = preferFirstIfNotNullOrElseSecond(formCompany, queryCompany);
                            User newUser = getSecurityService().createSimpleUser(usernameToUse, emailToUse, passwordToUse, fullNameToUse, companyToUse,
                                    Locale.ENGLISH, validationBaseURL, getSecurityService().getDefaultTenantForCurrentUser(), clientIP, /* enforce strong password */ true);
                            SecurityUtils.getSubject().login(new UsernamePasswordToken(usernameToUse, passwordToUse));
                            return newUser;
                        }

                        private String preferFirstIfNotNullOrElseSecond(String first, String second) {
                            final String result;
                            if (first != null) {
                                result = first;
                            } else {
                                result = second;
                            }
                            return result;
                        }
                    });
            return respondWithAccessTokenForUser(user.getName());
        } catch (Exception e) {
            return Response.status(Status.PRECONDITION_FAILED).entity(e.getMessage()).build();
        }
    }

    private String getEmailValidationBaseURL(UriInfo uriInfo) {
        final String urlPath = SECURITY_UI_URL_PATH+"EmailValidation.html";
        return getContextUrl(uriInfo, urlPath);
    }

    private String getPasswordResetURL(UriInfo uriInfo, String application) {
        final SecurityUrlPathProvider securityUrlPathProvider = getSecurityUrlPathProvider(application);
        final String urlPath = securityUrlPathProvider.getPasswordResetUrlPath();
        return getContextUrl(uriInfo, urlPath);
    }

    private String getContextUrl(UriInfo uriInfo, final String urlPath) {
        final String validationBaseURL = uriInfo.getBaseUri().getScheme()+"://"+uriInfo.getBaseUri().getHost()+
                (uriInfo.getBaseUri().getPort() == -1 ? "" : (":"+uriInfo.getBaseUri().getPort()))+urlPath;
        return validationBaseURL;
    }

    @GET
    @Path(USER_METHOD)
    @Produces("application/json;charset=UTF-8")
    public Response getUser(@QueryParam(USERNAME) String username) {
        final Subject subject = SecurityUtils.getSubject();
        final User user = getSecurityService().getUserByName(username == null ? subject.getPrincipal().toString() : username);
        if (user == null) {
            return Response.status(Status.PRECONDITION_FAILED).entity("User "+username+" not known").build();
        } else if (getSecurityService().hasCurrentUserReadPermission(user) || getSecurityService()
                .hasCurrentUserOneOfExplicitPermissions(user, SecuredSecurityTypes.PublicReadableActions.READ_PUBLIC)) {
            JSONObject result = new JSONObject();
            result.put(USERNAME, user.getName());
            if (getSecurityService().hasCurrentUserReadPermission(user)) {
                result.put(FULL_NAME, user.getFullName());
                result.put(EMAIL, user.getEmail());
                result.put(COMPANY, user.getCompany());
            }
            return Response.ok(streamingOutput(result)).build();
        } else {
            return Response.status(Status.UNAUTHORIZED).build();
        }
    }
    
    @DELETE
    @Path(USER_METHOD)
    @Produces("text/plain;charset=UTF-8")
    public Response deleteUser(@QueryParam(USERNAME) String username) {
        User user = getSecurityService().getUserByName(username);
        if (user != null) {
            return getSecurityService().checkPermissionAndDeleteOwnershipForObjectRemoval(user, () -> {
                try {
                    getSecurityService().deleteUser(username);
                    return Response.ok().build();
                } catch (UserManagementException e) {
                    return Response.status(Status.PRECONDITION_FAILED).entity(e.getMessage()).build();
                }
            });
        } else {
            return Response.status(Status.PRECONDITION_FAILED).entity("unknown id").build();
        }
    }
    
    @PUT
    @Path(ADD_ROLE_TO_USER_METHOD)
    @Produces("text/plain;charset=UTF-8")
    public Response addRoleToUser(@QueryParam(USERNAME) String username, @QueryParam(ROLE_DEFINITION_ID) String roleDefinitionId,
            @QueryParam(QUALIFYING_GROUP_ID) String qualifyingGroupId, @QueryParam(QUALIFYING_USERNAME) String qualifyingUserName,
            @QueryParam(TRANSITIVE) Boolean transitive) {
        final Response response;
        try {
            // get user for which to add a role
            final User user = getSecurityService().getUserByName(username);
            if (user == null) {
                response = Response.status(Status.NOT_FOUND).entity("User not found").build();
            } else {
                // get user for which the role is qualified, if one exists
                final User qualifiedForUser = qualifyingUserName == null ? null : getSecurityService().getUserByName(qualifyingUserName);
                if (qualifyingUserName != null && qualifiedForUser == null) {
                    response = Response.status(Status.NOT_FOUND).entity("Qualifying user not found").build();
                } else {
                    // get the group tenant the role is qualified for if one exists
                    final UserGroup qualifyingGroup = qualifyingGroupId == null ? null : getSecurityService().getUserGroup(UUID.fromString(qualifyingGroupId));
                    if (qualifyingGroupId != null && qualifyingGroup == null) {
                        response = Response.status(Status.NOT_FOUND).entity("Qualifying group not found").build();
                    } else {
                        final Role role = getSecurityService().getOrThrowRoleFromIDsAndCheckMetaPermissions(
                                roleDefinitionId == null ? null : UUID.fromString(roleDefinitionId),
                                qualifyingGroup == null ? null : qualifyingGroup.getId(),
                                qualifiedForUser == null ? null : qualifiedForUser.getName(), transitive);
                        final TypeRelativeObjectIdentifier associationTypeIdentifier = PermissionAndRoleAssociation.get(role, user);
                        final String message = "User "+SecurityUtils.getSubject().getPrincipal()+" added role " + role.getName() + " for user " + username;
                        getSecurityService().setOwnershipWithoutCheckPermissionForObjectCreationAndRevertOnError(
                                SecuredSecurityTypes.ROLE_ASSOCIATION, associationTypeIdentifier,
                                associationTypeIdentifier.toString(), new Action() {
                                    @Override
                                    public void run() throws Exception {
                                        final QualifiedObjectIdentifier qualifiedObjectAssociationIdentifier = SecuredSecurityTypes.ROLE_ASSOCIATION
                                                .getQualifiedObjectIdentifier(associationTypeIdentifier);
                                        getSecurityService().addToAccessControlList(qualifiedObjectAssociationIdentifier,
                                                null, DefaultActions.READ.name());
                                        getSecurityService().addRoleForUser(user, role);
                                        logger.info(message);
                                    }
                                });
                        response = Response.ok().build();
                    }
                }
            }
            return response;
        } catch (UserManagementException e) {
            return Response.status(Status.PRECONDITION_FAILED).entity(e.getMessage()).build();
        }
    }
    
    @GET
    @Path(GET_ROLES_FOR_USER_METHOD)
    @Produces("application/json;charset=UTF-8")
    public Response getRoles(@QueryParam(USERNAME) String username) {
        final JSONArray result = new JSONArray();
        final User user = username == null ? getSecurityService().getCurrentUser() : getSecurityService().getUserByName(username);
        if (user != null) {
            getSecurityService().checkCurrentUserReadPermission(user);
            for (final Role role : user.getRoles()) {
                final JSONObject roleJson = new JSONObject();
                result.add(roleJson);
                final TypeRelativeObjectIdentifier associationTypeIdentifier = PermissionAndRoleAssociation.get(role, user);
                final QualifiedObjectIdentifier qualifiedObjectIdentifierForRoleAssociation = SecuredSecurityTypes.ROLE_ASSOCIATION.getQualifiedObjectIdentifier(associationTypeIdentifier);
                if (getSecurityService().hasCurrentUserAnyPermission(qualifiedObjectIdentifierForRoleAssociation.getPermission(DefaultActions.READ))) {
                    final OwnershipAnnotation ownership = getSecurityService().getOwnership(qualifiedObjectIdentifierForRoleAssociation);
                    roleJson.put(ROLE_DEFINITION_ID, role.getRoleDefinition().getIdAsString());
                    roleJson.put(ROLE_NAME, role.getRoleDefinition().getName());
                    roleJson.put(OWNING_GROUP_ID, ownership == null ? null : ownership.getAnnotation() == null ? null : ownership.getAnnotation().getTenantOwner() == null ? null : ownership.getAnnotation().getTenantOwner().getId().toString());
                    roleJson.put(OWNING_USER_NAME, ownership == null ? null : ownership.getAnnotation() == null ? null : ownership.getAnnotation().getUserOwner() == null ? null : ownership.getAnnotation().getUserOwner().getName());
                    roleJson.put(QUALIFYING_GROUP_ID, role.getQualifiedForTenant() == null ? null : role.getQualifiedForTenant().getId().toString());
                    roleJson.put(QUALIFYING_USERNAME, role.getQualifiedForUser() == null ? null : role.getQualifiedForUser().getName());
                    roleJson.put(TRANSITIVE, role.isTransitive());
                }
            }
        }
        return Response.ok(streamingOutput(result)).build();
    }
    
    @PUT
    @Path(USER_METHOD)
    @Produces("text/plain;charset=UTF-8")
    public Response updateUser(@Context UriInfo uriInfo, @QueryParam(USERNAME) String username,
            @QueryParam(EMAIL) String email, @QueryParam(FULL_NAME) String fullName,
            @QueryParam(COMPANY) String company) {
        if (!getSecurityService().hasCurrentUserUpdatePermission(getSecurityService().getUserByName(username))) {
            return Response.status(Status.UNAUTHORIZED).build();
        } else {
            try {
                final User user = getSecurityService().getUserByName(username);
                if (user == null) {
                    return Response.status(Status.PRECONDITION_FAILED).entity("User "+username+" not known").build();
                } else {
                    getSecurityService().updateUserProperties(username, fullName, company, user.getLocale());
                    if (!Util.equalsWithNull(user.getEmail(), email)) {
                        getSecurityService().updateSimpleUserEmail(username, email, getEmailValidationBaseURL(uriInfo));
                    }
                    return Response.ok().build();
                }
            } catch (UserManagementException e) {
                return Response.status(Status.PRECONDITION_FAILED).entity(e.getMessage()).build();
            }
        }
    }
    
    @GET
    @Path(ACCESS_TOKEN_METHOD)
    @Produces("application/json;charset=UTF-8")
    public Response accessToken() {
        return respondWithAccessTokenForAuthenticatedSubject();
    }

    @POST
    @Path(ACCESS_TOKEN_METHOD)
    @Produces("application/json;charset=UTF-8")
    public Response accessTokenPost() {
        return respondWithAccessTokenForAuthenticatedSubject();
    }
    
    @GET
    @Path(LOGOUT_METHOD)
    @Produces("application/json;charset=UTF-8")
    public Response logout() {
        return logoutPost();
    }

    @POST
    @Path(LOGOUT_METHOD)
    @Produces("application/json;charset=UTF-8")
    public Response logoutPost() {
        getSecurityService().logout();
        return Response.ok().build();
    }

    @GET
    @Path(REMOVE_ACCESS_TOKEN_METHOD)
    @Produces("application/json;charset=UTF-8")
    public Response removeAccessToken() {
        return removeAccessTokenPost();
    }

    @POST
    @Path(REMOVE_ACCESS_TOKEN_METHOD)
    @Produces("application/json;charset=UTF-8")
    public Response removeAccessTokenPost() {
        final Response result;
        final Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal != null) {
            final String username = principal.toString();
            result = respondToRemoveAccessTokenForUser(username);
        } else {
            result = Response.status(Status.UNAUTHORIZED).build();
        }
        return result;
    }

    public Response respondToRemoveAccessTokenForUser(final String username) {
        final Response result;
        getSecurityService().removeAccessToken(username);
        result = respondWithAccessTokenForAuthenticatedSubject();
        return result;
    }
    
    @GET
    @Path(HAS_PERMISSION_METHOD)
    @Produces("application/json;charset=UTF-8")
    public Response getPermission(@QueryParam(PERMISSION) final List<String> permissionsAsStrings) {
        final JSONArray result = new JSONArray();
        for (final String permissionAsString : permissionsAsStrings) {
            final JSONObject entry = new JSONObject();
            result.add(entry);
            entry.put(PERMISSION, permissionAsString);
            entry.put(GRANTED, SecurityUtils.getSubject().isPermitted(permissionAsString));
        }
        return Response.ok(streamingOutput(result), MediaType.APPLICATION_JSON_TYPE).build(); 
    }

    private Response respondWithAccessTokenForAuthenticatedSubject() {
        final Response result;
        final Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal != null) {
            final String username = principal.toString();
            result = respondWithAccessTokenForUser(username);
        } else {
            result = Response.status(Status.UNAUTHORIZED).build();
        }
        return result;
    }

    Response respondWithAccessTokenForUser(final String username) {
        JSONObject response = new JSONObject();
        response.put(USERNAME, username);
        getSecurityService().checkCurrentUserReadPermission(getSecurityService().getUserByName(username));
        String accessToken;
        if (getSecurityService().hasCurrentUserUpdatePermission(getSecurityService().getUserByName(username))) {
            accessToken = getSecurityService().getOrCreateAccessToken(username);
        } else {
            accessToken = getSecurityService().getAccessToken(username);
            if (accessToken == null) {
                throw new AuthorizationException(
                        "No access token was found and the permission to create one is lacking.");
            }
        }
        response.put(ACCESS_TOKEN, accessToken);
        return Response.ok(streamingOutput(response), MediaType.APPLICATION_JSON_TYPE).build();
    }
}