package com.sap.sse.security.ui.client.component;

import com.google.gwt.user.client.ui.Focusable;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserManagementServiceAsync;
import com.sap.sse.security.ui.client.i18n.StringMessages;

public class ChangePasswordDialog extends AbstractUserDialog {
    public ChangePasswordDialog(StringMessages stringMessages, UserManagementServiceAsync userManagementService, UserDTO user,
            com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback<UserData> callback) {
        super(stringMessages, stringMessages.changePassword(), userManagementService, user, callback,
                /* checkUsernameForExistence */ false);
        getNameBox().setEnabled(false);
        getEmailBox().setEnabled(false);
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return getPwBox();
    }
}
