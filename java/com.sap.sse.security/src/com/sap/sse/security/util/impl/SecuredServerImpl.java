package com.sap.sse.security.util.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HTTP;
import org.apache.shiro.authz.AuthorizationException;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.jaxrs.api.OwnershipResource;
import com.sap.sse.security.jaxrs.api.SecurityResource;
import com.sap.sse.security.jaxrs.api.UserGroupResource;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.util.SecuredServer;
import com.sap.sse.util.HttpUrlConnectionHelper;
import com.sap.sse.util.LaxRedirectStrategyForAllRedirectResponseCodes;

public class SecuredServerImpl implements SecuredServer {
    private static final String BEARER_TOKEN_PREFIX_IN_AUTHORIZATION_HEADER = "Bearer ";

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final Logger logger = Logger.getLogger(SecuredServerImpl.class.getName());

    private final String bearerToken;
    private final URL baseUrl;

    public SecuredServerImpl(URL baseUrl, String bearerToken) {
        super();
        this.baseUrl = baseUrl;
        this.bearerToken = bearerToken;
    }

    @Override
    public URL getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String getBearerToken() {
        return bearerToken;
    }

    protected Pair<Object, Integer> getJsonParsedResponse(final HttpUriRequest request)
            throws IOException, ClientProtocolException, ParseException {
        authenticate(request);
        final HttpClient client = createHttpClient();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final HttpResponse response = client.execute(request);
        final int statusCode = response.getStatusLine().getStatusCode();
        Object jsonParseResult;
        if (statusCode == Response.Status.NO_CONTENT.getStatusCode()
                || Response.Status.fromStatusCode(statusCode) == null
                || Response.Status.fromStatusCode(statusCode).getFamily() != Family.SUCCESSFUL) {
            jsonParseResult = null;
        } else {
            response.getEntity().writeTo(bos);
            try {
                jsonParseResult = new JSONParser()
                        .parse(new InputStreamReader(new ByteArrayInputStream(bos.toByteArray()),
                                HttpUrlConnectionHelper.getCharsetFromHttpEntity(response.getEntity(), "UTF-8")));
            } catch (ParseException e) {
                jsonParseResult = new String(bos.toByteArray());
            }
        }
        return new Pair<>(jsonParseResult, statusCode);
    }

    private CloseableHttpClient createHttpClient() {
        return HttpClientBuilder.create().setRedirectStrategy(new LaxRedirectStrategyForAllRedirectResponseCodes())
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build()).build();
    }

    private void authenticate(HttpRequest request) {
        if (bearerToken != null) {
            request.setHeader(AUTHORIZATION_HEADER, BEARER_TOKEN_PREFIX_IN_AUTHORIZATION_HEADER + bearerToken);
        }
    }
    
    protected void authenticate(ClientUpgradeRequest websocketUpgradeRequest) {
        if (bearerToken != null) {
            websocketUpgradeRequest.setHeader(AUTHORIZATION_HEADER, BEARER_TOKEN_PREFIX_IN_AUTHORIZATION_HEADER + bearerToken);
        }
    }
    
    @Override
    public UUID getUserGroupIdByName(String userGroupName) throws ClientProtocolException, IOException, ParseException, IllegalAccessException {
        final URL getUserGroupIdByNameUrl = new URL(getBaseUrl(), SECURITY_API_PREFIX + UserGroupResource.RESTSECURITY_USERGROUP
                + "?" + UserGroupResource.KEY_GROUP_NAME+"="+userGroupName);
        final HttpGet getRequest = new HttpGet(getUserGroupIdByNameUrl.toString());
        final Pair<Object, Integer> result = getJsonParsedResponse(getRequest);
        final UUID groupId;
        if (result.getB() >= 200 && result.getB() < 300) {
            final JSONObject groupJson = (JSONObject) result.getA();
            groupId = groupJson == null ? null : UUID.fromString(groupJson.get(UserGroupResource.KEY_GROUP_ID).toString());
        } else if (result.getB() == Response.Status.FORBIDDEN.getStatusCode() || result.getB() == Response.Status.UNAUTHORIZED.getStatusCode()) {
            throw new AuthorizationException("Not allowed to access group "+userGroupName+": "+result.getA());
        } else {
            groupId = null;
        }
        return groupId;
    }

    @Override
    public Pair<UUID, String> getGroupAndUserOwner(HasPermissions type, TypeRelativeObjectIdentifier typeRelativeObjectId) throws ClientProtocolException, IOException, ParseException {
        final URL getGroupAndUserOwnerUrl = new URL(getBaseUrl(), SECURITY_API_PREFIX + OwnershipResource.RESTSECURITY_OWNERSHIP
                + "/" + type.getName() + "/" + typeRelativeObjectId.toString());
        final HttpGet getRequest = new HttpGet(getGroupAndUserOwnerUrl.toString());
        final JSONObject ownershipJson = (JSONObject) getJsonParsedResponse(getRequest).getA();
        final Object groupIdValue = ownershipJson.get(OwnershipResource.KEY_GROUP_ID);
        final UUID groupId = groupIdValue == null ? null : UUID.fromString(groupIdValue.toString());
        final Object usernameValue = ownershipJson.get(OwnershipResource.KEY_USERNAME);
        final String username = usernameValue == null ? null : usernameValue.toString();
        return new Pair<>(groupId, username);
    }
    
    @Override
    public void setGroupAndUserOwner(HasPermissions type, TypeRelativeObjectIdentifier typeRelativeObjectId,
            Optional<String> displayName, Optional<UUID> groupId, Optional<String> username) throws ClientProtocolException, IOException, ParseException {
        final URL setGroupAndUserOwnerUrl = new URL(getBaseUrl(),
                SECURITY_API_PREFIX + OwnershipResource.RESTSECURITY_OWNERSHIP + "/"
                        + type.getName() + "/" + typeRelativeObjectId.toString());
        final HttpPut putRequest = new HttpPut(setGroupAndUserOwnerUrl.toString());
        final JSONObject ownershipJson = new JSONObject();
        username.map(un->ownershipJson.put(OwnershipResource.KEY_USERNAME, un));
        groupId.map(gid->ownershipJson.put(OwnershipResource.KEY_GROUP_ID, gid.toString()));
        displayName.map(dn->ownershipJson.put(OwnershipResource.KEY_DISPLAY_NAME, dn));
        final HttpEntity entity = new StringEntity(ownershipJson.toJSONString(), "UTF-8");
        putRequest.setHeader(HTTP.CONTENT_TYPE, "application/json");
        putRequest.setEntity(entity);
        authenticate(putRequest);
        final CloseableHttpResponse response = createHttpClient().execute(putRequest);
        if (response.getStatusLine().getStatusCode() >= 300) {
            throw new IllegalArgumentException(response.getStatusLine().getReasonPhrase());
        }
    }
    
    @Override
    public Map<UUID, Set<String>> getAccessControlLists(HasPermissions type, TypeRelativeObjectIdentifier typeRelativeObjectId) throws ClientProtocolException, IOException, ParseException {
        final URL getGroupAndUserOwnerUrl = new URL(getBaseUrl(), SECURITY_API_PREFIX + OwnershipResource.RESTSECURITY_OWNERSHIP
                + "/" + type.getName() + "/" + typeRelativeObjectId.toString() + "/" + OwnershipResource.KEY_ACL);
        final HttpGet getRequest = new HttpGet(getGroupAndUserOwnerUrl.toString());
        final JSONObject aclJson = (JSONObject) getJsonParsedResponse(getRequest).getA();
        final Map<UUID, Set<String>> result = new HashMap<>();
        final JSONArray actionsByUserGroups = (JSONArray) aclJson.get(OwnershipResource.KEY_ACL);
        for (final Object actionsByUserGroup : actionsByUserGroups) {
            final JSONObject actionsByUserGroupJson = (JSONObject) actionsByUserGroup;
            final Object groupIdAsString = actionsByUserGroupJson.get(OwnershipResource.KEY_GROUP_ID);
            final UUID groupId = groupIdAsString == null ? null : UUID.fromString(groupIdAsString.toString());
            final JSONArray actions = (JSONArray) actionsByUserGroupJson.get(OwnershipResource.KEY_ACTIONS);
            final Set<String> actionStringSet = new HashSet<>();
            for (final Object action : actions) {
                actionStringSet.add(action.toString());
            }
            result.put(groupId, actionStringSet);
        }
        return result;
    }
    
    @Override
    public void setAccessControlLists(HasPermissions type, TypeRelativeObjectIdentifier typeRelativeObjectId,
            Map<UUID, Set<String>> actionsPerGroup) throws ClientProtocolException, IOException, ParseException {
        final URL setGroupAndUserOwnerUrl = new URL(getBaseUrl(),
                SECURITY_API_PREFIX + OwnershipResource.RESTSECURITY_OWNERSHIP + "/"
                        + type.getName() + "/" + typeRelativeObjectId.toString() + "/" + OwnershipResource.KEY_ACL);
        final HttpPut putRequest = new HttpPut(setGroupAndUserOwnerUrl.toString());
        final JSONObject aclJson = new JSONObject();
        final JSONArray actionsByUserGroupJson = new JSONArray();
        aclJson.put(OwnershipResource.KEY_ACL, actionsByUserGroupJson);
        for (final Entry<UUID, Set<String>> e : actionsPerGroup.entrySet()) {
            final JSONObject groupIdAndPermissions = new JSONObject();
            groupIdAndPermissions.put(OwnershipResource.KEY_GROUP_ID, e.getKey() == null ? null : e.getKey().toString());
            final JSONArray actionsJson = new JSONArray();
            actionsJson.addAll(e.getValue());
            groupIdAndPermissions.put(OwnershipResource.KEY_ACTIONS, actionsJson);
            actionsByUserGroupJson.add(groupIdAndPermissions);
        }
        final HttpEntity entity = new StringEntity(aclJson.toJSONString(), "UTF-8");
        putRequest.setHeader(HTTP.CONTENT_TYPE, "application/json");
        putRequest.setEntity(entity);
        authenticate(putRequest);
        final CloseableHttpResponse response = createHttpClient().execute(putRequest);
        if (response.getStatusLine().getStatusCode() >= 300) {
            throw new IllegalArgumentException(response.getStatusLine().getReasonPhrase());
        }
    }
    
    @Override
    public Iterable<Pair<WildcardPermission, Boolean>> hasPermissions(Iterable<WildcardPermission> permissions) throws ClientProtocolException, IOException, ParseException {
        final StringBuilder sb = new StringBuilder(SECURITY_API_PREFIX + SecurityResource.RESTSECURITY + SecurityResource.HAS_PERMISSION_METHOD + "?");
        for (final WildcardPermission permission : permissions) {
            sb.append(SecurityResource.PERMISSION);
            sb.append('=');
            sb.append(URLEncoder.encode(permission.toString(), "UTF-8"));
            sb.append('&');
        }
        sb.delete(sb.length()-1, sb.length());
        final URL getPermissionsUrl = new URL(getBaseUrl(), sb.toString());
        final HttpGet getRequest = new HttpGet(getPermissionsUrl.toString());
        final JSONArray permissionsJson = (JSONArray) getJsonParsedResponse(getRequest).getA();
        final List<Pair<WildcardPermission, Boolean>> result = new ArrayList<>();
        for (final Object o : permissionsJson) {
            final JSONObject permissionAndGranted = (JSONObject) o;
            final String permissionAsString = permissionAndGranted.get(SecurityResource.PERMISSION).toString();
            final Boolean permissionGranted = (Boolean) permissionAndGranted.get(SecurityResource.GRANTED);
            result.add(new Pair<>(new WildcardPermission(permissionAsString), permissionGranted));
        }
        return result;
    }

    @Override
    public String getUsername() throws ClientProtocolException, IOException, ParseException {
        final URL getUsernameUrl = new URL(getBaseUrl(), SECURITY_API_PREFIX + SecurityResource.RESTSECURITY + SecurityResource.ACCESS_TOKEN_METHOD);
        final HttpGet getRequest = new HttpGet(getUsernameUrl.toString());
        final JSONObject accessTokenJson = (JSONObject) getJsonParsedResponse(getRequest).getA();
        final Object usernameValue = accessTokenJson.get(SecurityResource.USERNAME);
        final String username = usernameValue == null ? null : usernameValue.toString();
        return username;
    }

    @Override
    public Iterable<String> getNamesOfUsersInGroup(UUID userGroupId) throws ClientProtocolException, IOException, ParseException {
        final URL addUserToGroupUrl = new URL(getBaseUrl(), SECURITY_API_PREFIX + UserGroupResource.RESTSECURITY_USERGROUP + "/"+userGroupId.toString());
        final HttpGet getRequest = new HttpGet(addUserToGroupUrl.toString());
        final Pair<Object, Integer> result = getJsonParsedResponse(getRequest);
        if (result.getB() == Response.Status.FORBIDDEN.getStatusCode() || result.getB() == Response.Status.UNAUTHORIZED.getStatusCode()) {
            throw new AuthorizationException("Not allowed to access group with ID "+userGroupId+": "+result.getA());
        }
        final JSONObject userGroupJson = (JSONObject) result.getA();
        final JSONArray usersInGroup = (JSONArray) userGroupJson.get(UserGroupResource.KEY_USERS);
        return Util.map(usersInGroup, u->u.toString());
    }

    @Override
    public void addUserToGroup(UUID userGroupId, String username) throws ClientProtocolException, IOException, ParseException {
        if (!Util.contains(getNamesOfUsersInGroup(userGroupId), username)) {
            final URL addUserToGroupUrl = new URL(getBaseUrl(), SECURITY_API_PREFIX + UserGroupResource.RESTSECURITY_USERGROUP +
                    "/"+userGroupId.toString()+UserGroupResource.USER+"/"+username);
            final HttpPut putRequest = new HttpPut(addUserToGroupUrl.toString());
            final Pair<Object, Integer> result = getJsonParsedResponse(putRequest);
            final Integer status = result.getB();
            if (status == Response.Status.FORBIDDEN.getStatusCode() || status == Response.Status.UNAUTHORIZED.getStatusCode()) {
                throw new AuthorizationException("Not allowed to access group with ID "+userGroupId+": "+result.getA());
            } else if (status < 200 || status >= 300) {
                throw new IllegalArgumentException("Couldn't add user "+username+" to user group with ID "+userGroupId+": "+result.getA());
            }
        }
    }

    @Override
    public void removeUserFromGroup(UUID userGroupId, String username) throws ClientProtocolException, IOException, ParseException {
        if (Util.contains(getNamesOfUsersInGroup(userGroupId), username)) {
            final URL removeUserFromGroupUrl = new URL(getBaseUrl(), SECURITY_API_PREFIX + UserGroupResource.RESTSECURITY_USERGROUP +
                    "/"+userGroupId.toString()+UserGroupResource.USER+"/"+username);
            final HttpDelete putRequest = new HttpDelete(removeUserFromGroupUrl.toString());
            final Pair<Object, Integer> result = getJsonParsedResponse(putRequest);
            final Integer status = result.getB();
            if (status == Response.Status.FORBIDDEN.getStatusCode() || status == Response.Status.UNAUTHORIZED.getStatusCode()) {
                throw new AuthorizationException("Not allowed to access group with ID "+userGroupId+": "+result.getA());
            } else if (status < 200 || status >= 300) {
                throw new IllegalArgumentException("Couldn't remove user "+username+" from user group with ID "+userGroupId+": "+result.getA());
            }
        }
    }

    @Override
    public UUID createUserGroupAndAddCurrentUser(String userGroupName) throws ClientProtocolException, IOException, ParseException, IllegalAccessException {
        final UUID result;
        if (getUserGroupIdByName(userGroupName) == null) {
            final JSONObject paramPayload = new JSONObject();
            paramPayload.put(UserGroupResource.KEY_GROUP_NAME, userGroupName);
            final URL createUserGroupUrl = new URL(getBaseUrl(), SECURITY_API_PREFIX + UserGroupResource.RESTSECURITY_USERGROUP);
            final HttpPut putRequest = new HttpPut(createUserGroupUrl.toString());
            putRequest.setEntity(new StringEntity(paramPayload.toJSONString(), "UTF-8"));
            putRequest.setHeader(HTTP.CONTENT_TYPE, "application/json");
            final Pair<Object, Integer> response = getJsonParsedResponse(putRequest);
            if (response.getA() instanceof JSONObject) {
                final JSONObject userGroupJson = (JSONObject) response.getA();
                final UUID newGroupId = UUID.fromString(userGroupJson.get(UserGroupResource.KEY_GROUP_ID).toString());
                result = newGroupId;
            } else if (response.getB() == Response.Status.FORBIDDEN.getStatusCode() || response.getB() == Response.Status.UNAUTHORIZED.getStatusCode()) {
                throw new AuthorizationException("Not allowed to create group "+userGroupName+": "+response.getA());
            } else {
                throw new IllegalArgumentException("Error trying to create user group "+userGroupName+": "+response.getA());
            }
        } else {
            logger.warning("User group name "+userGroupName+" already exists on server "+getBaseUrl()+". Not creating again.");
            result = null;
        }
        return result;
    }

    @Override
    public void addRoleToUser(UUID roleId, String username, UUID qualifiedForGroupWithId, String qualifiedForUserWithName, boolean transitive) throws ClientProtocolException, IOException, ParseException {
        final URL addRoleToUserUrl = new URL(getBaseUrl(), SECURITY_API_PREFIX + SecurityResource.RESTSECURITY +
                SecurityResource.ADD_ROLE_TO_USER_METHOD
                + "?" + SecurityResource.USERNAME+"="+username
                + "&" + SecurityResource.ROLE_DEFINITION_ID+"="+roleId.toString()
                + (qualifiedForGroupWithId == null ? "" : ("&" + SecurityResource.QUALIFYING_GROUP_ID+"="+qualifiedForGroupWithId.toString()))
                + (qualifiedForUserWithName == null ? "" : ("&" + SecurityResource.QUALIFYING_USERNAME+"="+qualifiedForUserWithName))
                + "&" + SecurityResource.TRANSITIVE+"="+transitive);
        final HttpPut putRequest = new HttpPut(addRoleToUserUrl.toString());
        final Pair<Object, Integer> result = getJsonParsedResponse(putRequest);
        final Integer status = result.getB();
        if (status == Response.Status.FORBIDDEN.getStatusCode() || status == Response.Status.UNAUTHORIZED.getStatusCode()) {
            throw new AuthorizationException("Not allowed to add role with ID "+roleId+" to user "+username+": "+result.getA());
        } else if (status < 200 || status >= 300) {
            throw new IllegalArgumentException("Couldn't add role with ID "+roleId+ " to user "+username+": "+result.getA());
        }
    }
    
    @Override
    public Iterable<RoleDescriptor> getRoles(String username) throws ClientProtocolException, IOException, ParseException {
        final URL getRolesForUserUrl = new URL(getBaseUrl(), SECURITY_API_PREFIX + SecurityResource.RESTSECURITY +
                SecurityResource.GET_ROLES_FOR_USER_METHOD
                + (username == null ? "" : ("?" + SecurityResource.USERNAME+"="+username)));
        final HttpGet getRequest = new HttpGet(getRolesForUserUrl.toString());
        final Pair<Object, Integer> result = getJsonParsedResponse(getRequest);
        final Integer status = result.getB();
        if (status == Response.Status.FORBIDDEN.getStatusCode() || status == Response.Status.UNAUTHORIZED.getStatusCode()) {
            throw new AuthorizationException("Not allowed to get roles for user "+username+" to user "+username+": "+result.getA());
        } else if (status < 200 || status >= 300) {
            throw new IllegalArgumentException("Couldn't get roles from user "+username+": "+result.getA());
        }
        return Util.map((JSONArray) result.getA(), jsonRole->new RoleDescriptor() {
            final JSONObject jsonRoleObject = (JSONObject) jsonRole;

            @Override
            public UUID getRoleDefinitionId() {
                return UUID.fromString(jsonRoleObject.get(SecurityResource.ROLE_DEFINITION_ID).toString());
            }

            @Override
            public UUID getQualifiedForGroupWithId() {
                return jsonRoleObject.get(SecurityResource.QUALIFYING_GROUP_ID) == null ? null : UUID.fromString(jsonRoleObject.get(SecurityResource.QUALIFYING_GROUP_ID).toString());
            }

            @Override
            public String getQualifiedForUserWithName() {
                return jsonRoleObject.get(SecurityResource.QUALIFYING_USERNAME) == null ? null : jsonRoleObject.get(SecurityResource.QUALIFYING_USERNAME).toString();
            }

            @Override
            public Boolean isTransitive() {
                return jsonRoleObject.get(SecurityResource.TRANSITIVE) == null ? null : (Boolean) jsonRoleObject.get(SecurityResource.TRANSITIVE);
            }
        });
    }

    @Override
    public String toString() {
        return getBaseUrl().toString();
    }
}
