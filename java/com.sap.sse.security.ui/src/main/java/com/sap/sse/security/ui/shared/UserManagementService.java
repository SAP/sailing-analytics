package com.sap.sse.security.ui.shared;

import java.util.Collection;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;
import com.sap.sse.security.shared.DefaultRoles;
import com.sap.sse.security.shared.MailException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.ui.oauth.client.CredentialDTO;
import com.sap.sse.security.ui.oauth.shared.OAuthException;

public interface UserManagementService extends RemoteService {
    Collection<UserDTO> getUserList();

    Collection<UserDTO> getFilteredSortedUserList(String filter);

    UserDTO getCurrentUser();

    SuccessInfo login(String username, String password);

    UserDTO createSimpleUser(String name, String email, String password, String validationBaseURL) throws UserManagementException, MailException;
    
    void updateSimpleUserPassword(String name, String oldPassword, String newPassword) throws UserManagementException, MailException;

    void updateSimpleUserEmail(String username, String newEmail, String validationBaseURL) throws UserManagementException, MailException;

    void resetPassword(String username) throws UserManagementException;

    boolean validateEmail(String username, String validationSecret) throws UserManagementException;

    SuccessInfo deleteUser(String username);

    SuccessInfo logout();

    SuccessInfo setRolesForUser(String username, Iterable<String> roles);

    Map<String, String> getSettings();

    Map<String, String> getSettingTypes();

    void setSetting(String key, String clazz, String setting);

    void addSetting(String key, String clazz, String setting);

    /**
     * Permitted only for users with role {@link DefaultRoles#ADMIN} or when the subject's user name matches
     * <code>username</code>.
     * 
     * @param key must not be <code>null</code>
     * @param value must not be <code>null</code>
     */
    void setPreference(String username, String key, String value);

    /**
     * Permitted only for users with role {@link DefaultRoles#ADMIN} or when the subject's user name matches
     * <code>username</code>.
     */
    void unsetPreference(String username, String key);

    /**
     * @return <code>null</code> if no preference for the user identified by <code>username</code> is found
     */
    String getPreference(String username, String key);

    // ------------------------------------------------ OAuth Interface --------------------------------------------------------------

    public String getAuthorizationUrl(CredentialDTO credential) throws OAuthException;

    public UserDTO verifySocialUser(CredentialDTO credential) throws OAuthException;

}
