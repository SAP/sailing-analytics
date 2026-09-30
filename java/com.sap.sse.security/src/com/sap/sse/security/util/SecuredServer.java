package com.sap.sse.security.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.parser.ParseException;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.WildcardPermission;
import com.sap.sse.security.shared.impl.UserGroup;

/**
 * Represents a remote instance of a server process or an entire application replica set with a master and zero or more
 * replicas, running at least this web bundle, reachable under the {@code /security/api} URL path, exposing the REST
 * API of this bundle. In short, this is a Java facade for a REST API.
 * <p>
 * 
 * @author Axel Uhl (d043530)
 */
public interface SecuredServer {
    String SECURITY_API_PREFIX = "security/api";

    /**
     * The server's base URL, ending with a slash "/" character
     */
    URL getBaseUrl();
    
    String getBearerToken();

    /**
     * Looks for a group with the name provided in parameter {@code userGroupName}. If found, the group's {@link UUID} is
     * returned, otherwise {@code null}.
     */
    UUID getUserGroupIdByName(String userGroupName) throws MalformedURLException, ClientProtocolException, IOException, ParseException, IllegalAccessException;

    /**
     * @return a pair of the owning user group's UUID and the owning user's name 
     */
    Pair<UUID, String> getGroupAndUserOwner(HasPermissions type, TypeRelativeObjectIdentifier typeRelativeObjectId)
            throws ClientProtocolException, IOException, ParseException;
    
    /**
     * Set group (if {@code groupId} is present) and user (if {@code username} is present) ownership of the object
     * identified by {@code type} and {@code typeRelativeObjectId}, optionally also its display name, to the group
     * identified by {@code groupId} and the user identified by {@code username}. If this fails for any reason that
     * manifests itself in a non-2XX HTTP response code, an {@link IllegalArgumentException} is thrown.
     */
    void setGroupAndUserOwner(HasPermissions type, TypeRelativeObjectIdentifier typeRelativeObjectId,
            Optional<String> displayName, Optional<UUID> groupId, Optional<String> username)
            throws MalformedURLException, ClientProtocolException, IOException, ParseException;
    
    void deleteOwnership(HasPermissions type, TypeRelativeObjectIdentifier typeRelativeObjectId)
            throws MalformedURLException, ClientProtocolException, IOException, ParseException;
    
    void deleteAccessControlLists(HasPermissions type, TypeRelativeObjectIdentifier typeRelativeObjectId)
            throws MalformedURLException, ClientProtocolException, IOException, ParseException;

    Iterable<Pair<WildcardPermission, Boolean>> hasPermissions(Iterable<WildcardPermission> permissions) throws UnsupportedEncodingException, MalformedURLException, ClientProtocolException, IOException, ParseException;
    /**
     * The name of the user authenticated by the credentials used by this facade object.
     */
    String getUsername() throws MalformedURLException, ClientProtocolException, IOException, ParseException;

    /**
     * If the user authenticated for this server is permitted to update the user group identified by the {@code userGroupId}, the
     * current user is added to the group. If the current user is already part of the group, this method does nothing.
     */
    default void addCurrentUserToGroup(UUID userGroupId) throws ClientProtocolException, IOException, ParseException {
        addUserToGroup(userGroupId, getUsername());
    }

    /**
     * If the user authenticated for this server is permitted to update the user group identified by the
     * {@code userGroupId}, the user specified by {@code username} is added to the group. If the current user is already
     * part of the group, this method does nothing.
     */
    void addUserToGroup(UUID userGroupId, String username) throws ClientProtocolException, IOException, ParseException;
    
    /**
     * If the user authenticated for this server is permitted to update the user group identified by the {@code userGroupId}, the
     * current user is removed from the group. If the current user is not part of the group, this method does nothing.
     */
    default void removeCurrentUserFromGroup(UUID userGroupId) throws ClientProtocolException, MalformedURLException, IOException, ParseException {
        removeUserFromGroup(userGroupId, getUsername());
    }

    /**
     * If the user authenticated for this server is permitted to update the user group identified by the {@code userGroupId}, the
     * user specified by {@code username} is removed from the group. If the current user is not part of the group, this method does nothing.
     */
    void removeUserFromGroup(UUID userGroupId, String username) throws ClientProtocolException, IOException, ParseException;

    /**
     * Create a user group named {@code serverGroupName} if no group by that name exists yet. The group will be owned by the
     * user authenticated for this server, and the user will be added to the group. The {@code user} role qualified to the new
     * group will be assigned to the calling subject ({@code user:{name}}) as a "transitive" role assignment, allowing
     * the user to grant that role also to other users, in turn.
     */
    UUID createUserGroupAndAddCurrentUser(String serverGroupName) throws ClientProtocolException, IOException, ParseException, IllegalAccessException;

    Iterable<String> getNamesOfUsersInGroup(UUID userGroupId) throws ClientProtocolException, IOException, ParseException;

    /**
     * Adds the role identified by {@code roleId} to the user identified by {@code username} and makes it transitive or
     * non-transitive based on the value of the {@code transitive} parameter.
     */
    void addRoleToUser(UUID roleId, String username, UUID qualifiedForGroupWithId, String qualifiedForUserWithName,
            boolean transitive) throws MalformedURLException, ClientProtocolException, IOException, ParseException;

    public static interface RoleDescriptor {
        UUID getRoleDefinitionId();
        UUID getQualifiedForGroupWithId();
        String getQualifiedForUserWithName();
        Boolean isTransitive();
    }
    
    /**
     * @param username may be {@code null} which will then fetch the roles of the user currently authenticated
     */
    Iterable<RoleDescriptor> getRoles(String username) throws ClientProtocolException, IOException, ParseException;

    /**
     * Obtains the access control lists defined for the object identified by {@code type} and
     * {@code typeRelativeObjectId}.
     * 
     * @return a valid, non-{@code null} map which may be empty. It does support the {@code null} key which then means
     *         the "<Any>" virtual group of which implicitly all users are a member. The keys of the map returned identify
     *         {@link UserGroup} objects, the value sets contain the names of the actions allowed/disallowed for the group
     *         identified by the corresponding key. Disallowed actions are indicated by a leading exclamation mark, as in
     *         {@code "!UPDATE"}.
     */
    Map<UUID, Set<String>> getAccessControlLists(HasPermissions type, TypeRelativeObjectIdentifier typeRelativeObjectId)
            throws ClientProtocolException, IOException, ParseException;

    /**
     * Updates the access control lists for the object identified by {@code type} and {@code typeRelativeObjectId}.
     * 
     * @param actionsPerGroup
     *            a valid, non-{@code null} map which may be empty. It does support the {@code null} key which then
     *            means the "<Any>" virtual group of which implicitly all users are a member. The keys of the map
     *            returned identify {@link UserGroup} objects, the value sets contain the names of the actions
     *            allowed/disallowed for the group identified by the corresponding key. Disallowed actions are indicated
     *            by a leading exclamation mark, as in {@code "!UPDATE"}.
     */
    void setAccessControlLists(HasPermissions type, TypeRelativeObjectIdentifier typeRelativeObjectId,
            Map<UUID, Set<String>> actionsPerGroup) throws ClientProtocolException, IOException, ParseException;
}
