package com.sap.sse.security.ui.userprofile.shared.userdetails;

import com.google.gwt.user.client.ui.ListBox;
import com.sap.sse.security.ui.shared.UserDTO;

/**
 * View definition of the user profile details page. This view is used to implement desktop and mobile views using a
 * shared presenter implementation.
 */
public interface UserDetailsView {
    
    void setPresenter(Presenter presenter);
    
    void setUser(UserDTO user);
    
    void clearPasswordFields();
    
    public interface Presenter {
        void handleSaveChangesRequest(String fullName, String company, String locale, String defaultTenantIdAsString);
        void handleEmailChangeRequest(String email);
        void handlePasswordChangeRequest(String oldPassword, String newPassword, String newPasswordConfirmation);
        void fillTenants(ListBox tenantListBox);
    }
}
