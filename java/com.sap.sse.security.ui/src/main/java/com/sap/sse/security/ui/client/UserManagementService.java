package com.sap.sse.security.ui.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.gwt.client.ServerInfoDTO;
import com.sap.sse.landscape.aws.common.shared.SecuredAwsLandscapeType;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.UnauthorizedException;
import com.sap.sse.security.shared.UserManagementException;
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

public interface UserManagementService extends RemoteService {

    Collection<AccessControlListAnnotationDTO> getAccessControlLists()
            throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    Collection<UserGroupDTO> getUserGroups() throws org.apache.shiro.authz.UnauthorizedException;

    UserGroupDTO getUserGroupByName(String userGroupName)
            throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    StrippedUserGroupDTO getStrippedUserGroupByName(String userGroupName)
            throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    Collection<UserDTO> getUserList() throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    Boolean userExists(String username) throws org.apache.shiro.authz.UnauthorizedException;

    ArrayList<RoleDefinitionDTO> getRoleDefinitions() throws org.apache.shiro.authz.UnauthorizedException;

    Triple<UserDTO, UserDTO, ServerInfoDTO> getCurrentUser()
            throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    Map<String, String> getSettings() throws org.apache.shiro.authz.UnauthorizedException;

    Map<String, String> getSettingTypes() throws org.apache.shiro.authz.UnauthorizedException;

    /**
     * @return <code>null</code> if no preference for the user identified by <code>username</code> is found
     */
    String getPreference(String username, String key)
            throws UserManagementException, UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    Map<String, String> getPreferences(String username, List<String> keys)
            throws UserManagementException, UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    Map<String, String> getAllPreferences(String username)
            throws UserManagementException, UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    String getOrCreateAccessToken(String username)
            throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    AccessControlListDTO getAccessControlListWithoutPruning(QualifiedObjectIdentifier idOfAccessControlledObject)
            throws UnauthorizedException, org.apache.shiro.authz.UnauthorizedException;

    SerializationDummy serializationDummy(TypeRelativeObjectIdentifier typeRelativeObjectIdentifier, HasPermissions hasPermissions, SecuredAwsLandscapeType securedAwsLandscapeType)
            throws org.apache.shiro.authz.UnauthorizedException;

    RolesAndPermissionsForUserDTO getRolesAndPermissionsForUser(String username)
            throws UserManagementException, org.apache.shiro.authz.UnauthorizedException;

    Boolean userGroupExists(String userGroupName) throws org.apache.shiro.authz.UnauthorizedException;

    SecurityServiceSharingDTO getSharingConfiguration();
    
    Triple<UserDTO, UserDTO, ServerInfoDTO> verifySocialUser(CredentialDTO credentialDTO);

    SuccessInfo login(String username, String password) throws org.apache.shiro.authz.UnauthorizedException;

    SuccessInfo logout() throws org.apache.shiro.authz.UnauthorizedException;

    ArrayList<HasPermissions> getAllHasPermissions();

    SecuredDTO addSecurityInformation(SecuredDTO securedDTO);
    
    Pair<Boolean, ArrayList<String>> getCORSFilterConfiguration();
}
