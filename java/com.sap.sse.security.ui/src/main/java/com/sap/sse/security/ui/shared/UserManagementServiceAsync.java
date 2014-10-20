package com.sap.sse.security.ui.shared;

import java.util.Collection;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.security.ui.oauth.client.CredentialDTO;

public interface UserManagementServiceAsync {
    void getUserList(AsyncCallback<Collection<UserDTO>> callback);

    void getCurrentUser(AsyncCallback<UserDTO> callback);

    void login(String username, String password, AsyncCallback<SuccessInfo> callback);

    void logout(AsyncCallback<SuccessInfo> callback);

    void createSimpleUser(String name, String email, String password, AsyncCallback<UserDTO> callback);

    void updateSimpleUserPassword(String name, String oldPassword, String newPassword, AsyncCallback<Void> callback);

    void resetPassword(String username, AsyncCallback<Void> callback);
    
    void validateEmail(String username, String validationSecret, AsyncCallback<Boolean> markedAsyncCallback);

    void updateSimpleUserEmail(String username, String newEmail, AsyncCallback<Void> callback);

    void getFilteredSortedUserList(String filter, AsyncCallback<Collection<UserDTO>> callback);

    void setRolesForUser(String username, Iterable<String> roles, AsyncCallback<SuccessInfo> callback);

    void deleteUser(String username, AsyncCallback<SuccessInfo> callback);

    void getSettings(AsyncCallback<Map<String, String>> callback);

    void setSetting(String key, String clazz, String setting, AsyncCallback<Void> callback);

    void getSettingTypes(AsyncCallback<Map<String, String>> callback);
    
  //------------------------------------------------ OAuth Interface ----------------------------------------------------------------------

    void getAuthorizationUrl(CredentialDTO credential, AsyncCallback<String> callback);

    void verifySocialUser(CredentialDTO credential, AsyncCallback<UserDTO> callback);

    void addSetting(String key, String clazz, String setting, AsyncCallback<Void> callback);

}
