package com.sap.sse.security.ui.authentication;

import java.util.function.Consumer;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.authentication.app.AuthenticationContext;
import com.sap.sse.security.ui.shared.SuccessInfo;

/**
 * Interface which provides common methods used for authentication management.
 * Implementing classes should provide internal failure handling.
 */
public interface AuthenticationManager {
    
    /**
     * Creates a new account with the given parameters.
     * 
     * @param name
     *            the (unique) username
     * @param email
     *            the user's email address
     * @param password
     *            the desired password
     * @param fullName
     *            the user's full name (optional)
     * @param locale
     *            the locale of user which is being registered
     * @param company
     *            the company the user belongs to (optional)
     * @param callback
     *            callback which is invoked, if account creating was successful (must not be <code>null</code>)
     */
    void createAccount(String name, String email, String password, String fullName, String locale, String company,
            SuccessCallback<UserDTO> callback);
    
    /**
     * Requests a password reset for the given username or email address.
     * 
     * @param username
     *            the username to reset password for
     * @param eMailAddress
     *            the email address to reset password for
     * @param callback
     *            callback which is invoked, if password reset request was successful (must not be <code>null</code>)
     */
    void requestPasswordReset(String username, String eMailAddress, SuccessCallback<Void> callback);
    
    /**
     * Login with the given username and password.
     * 
     * @param username
     *            the username to login with
     * @param password
     *            the password to login with
     * @param callback
     *            callback which is invoked, if login was successful (must not be <code>null</code>)
     */
    void login(String username, String password, SuccessCallback<SuccessInfo> callback);
    
    /**
     * Perform logout.
     */
    void logout();
    
    void updateUserProperties(String fullName, String company, String localeName, Boolean didOptOutFeatureAndCommunityEmails,
            String defaultTenantIdAsString, AsyncCallback<UserDTO> callback);

    /**
     * Provide the {@link AuthenticationContext} for the current user 
     * 
     * @return an {@link AuthenticationContext} instance
     */
    AuthenticationContext getAuthenticationContext();
    
    /**
     * Callback interface with is invoked in case of a successfully executed operation.
     * 
     * @param <T> The type of the value provided in the {@link #onSuccess(Object)} method
     */
    public interface SuccessCallback<T> {
        
        /**
         * @param result the operations result object
         */
        void onSuccess(T result);
    }

    /**
     * This triggers a check if a hint should be shown for non authenticated users to advertise the possibility to
     * create and account and log in. When such a hint should be shown, the given {@link Consumer} is called. The
     * {@link Runnable} given to this {@link Consumer} needs to be called when the user dismissed the hint or looks at
     * the detailed information.<br>
     * The given hideUserHintCallback is called when a user is authenticated while the hint is shown, which means, the
     * hint needs to be closed immediately.
     */
    void checkNewUserPopup(Runnable hideUserHintCallback, Consumer<Runnable> showUserHintCallback);
}
