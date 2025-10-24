package com.sap.sse.security.ui.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.gwt.client.ServerInfoDTO;
import com.sap.sse.landscape.aws.common.shared.SecuredAwsLandscapeType;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.dto.AccessControlListAnnotationDTO;
import com.sap.sse.security.shared.dto.AccessControlListDTO;
import com.sap.sse.security.shared.dto.RoleDefinitionDTO;
import com.sap.sse.security.shared.dto.RolesAndPermissionsForUserDTO;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.shared.dto.StrippedUserGroupDTO;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.shared.dto.UserGroupDTO;
import com.sap.sse.security.ui.oauth.client.CredentialDTO;
import com.sap.sse.security.ui.shared.SecurityServiceSharingDTO;
import com.sap.sse.security.ui.shared.SuccessInfo;

public interface UserManagementServiceAsync {
    void getAccessControlLists(AsyncCallback<Collection<AccessControlListAnnotationDTO>> callback);

    void getAccessControlListWithoutPruning(QualifiedObjectIdentifier idOfAccessControlledObject,
            AsyncCallback<AccessControlListDTO> updateAclAsyncCallback);

    /**
     * Returns those user groups the requesting user can read
     */
    void getUserGroups(AsyncCallback<Collection<UserGroupDTO>> callback);
    
    void getUserGroupByName(String userGroupName, AsyncCallback<UserGroupDTO> callback);
    
    void getStrippedUserGroupByName(String userGroupName, AsyncCallback<StrippedUserGroupDTO> callback);

    /**
     * Returns those users the requesting user can read
     */
    void getUserList(AsyncCallback<Collection<UserDTO>> callback);

    /**
     * Returns true if a user associated to the given username even if the current user cannot see the existing user.
     */
    void userExists(String username, AsyncCallback<Boolean> callback);

    void getRoleDefinitions(AsyncCallback<ArrayList<RoleDefinitionDTO>> callback);

    void getCurrentUser(AsyncCallback<Triple<UserDTO, UserDTO, ServerInfoDTO>> callback);

    void getSettings(AsyncCallback<Map<String, String>> callback);

    void getSettingTypes(AsyncCallback<Map<String, String>> callback);

    void getPreference(String username, String key, AsyncCallback<String> callback);

    void getPreferences(String username, List<String> keys, final AsyncCallback<Map<String, String>> callback);

    /**
     * Preferences whose key starts with an underscore ({@code _}) are removed from the result.
     */
    void getAllPreferences(String username, final AsyncCallback<Map<String, String>> callback);

    /**
     * * Obtains an access token for the user specified by {@code username}. The caller needs to have role
     * {@link DefaultRoles#ADMIN} or be authorized as the user identified by {@code username} in order to be permitted
     * to retrieve the access token, a new access token will be created and returned.
     */
    void getOrCreateAccessToken(String username, AsyncCallback<String> callback);

    void serializationDummy(TypeRelativeObjectIdentifier typeRelativeObjectIdentifier, HasPermissions hasPermissions,SecuredAwsLandscapeType securedAwsLandscapeType,
            AsyncCallback<SerializationDummy> callback);

    void getRolesAndPermissionsForUser(String username, AsyncCallback<RolesAndPermissionsForUserDTO> callback);

    void userGroupExists(String userGroupName, AsyncCallback<Boolean> callback);
    
    /**
     * Provides information about whether and how the security service to which this RPC service talks is shared
     * across a server landscape with different domain/sub-domain constellations.
     */
    void getSharingConfiguration(AsyncCallback<SecurityServiceSharingDTO> callback);

    void verifySocialUser(CredentialDTO credential, AsyncCallback<Triple<UserDTO, UserDTO, ServerInfoDTO>> markedAsyncCallback);

    void login(String username, String password, AsyncCallback<SuccessInfo> callback);

    void logout(AsyncCallback<SuccessInfo> callback);

    void getAllHasPermissions(AsyncCallback<ArrayList<HasPermissions>> callback);

    /**
     * Adds security information (ownership, ACLs) to the {@link SecuredDTO} passed as parameter, in place. See
     * {@link SecuredDTO#setOwnership(com.sap.sse.security.shared.dto.OwnershipDTO)} and
     * {@link SecuredDTO#setAccessControlList(AccessControlListDTO)}. The DTO augmented this way is returned by the
     * method, hence the security information added will be serialized to the client which can then receive it
     * in the {@code callback} passed.
     */
    void addSecurityInformation(SecuredDTO securedDTO, AsyncCallback<SecuredDTO> callback);
    
    void getCORSFilterConfiguration(AsyncCallback<Pair<Boolean, ArrayList<String>>> callback);
}
