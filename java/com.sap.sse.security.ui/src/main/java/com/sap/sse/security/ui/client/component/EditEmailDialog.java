package com.sap.sse.security.ui.client.component;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserManagementServiceAsync;
import com.sap.sse.security.ui.client.i18n.StringMessages;

public class EditEmailDialog extends AbstractUserDialog {
    public EditEmailDialog(final StringMessages stringMessages, UserManagementServiceAsync userManagementService, UserDTO user, DialogCallback<UserData> callback) {
        super(stringMessages, stringMessages.editEmail(), stringMessages.editEmail(), userManagementService,
                user, /* no validator; any address is considered valid */ null, callback);
    }

    @Override
    protected Widget getAdditionalWidget() {
        Grid result = new Grid(2, 2);
        result.setWidget(0, 0, new Label(getStringMessages().username()));
        result.setWidget(0, 1, getNameBox());
        getNameBox().setEnabled(false);
        result.setWidget(1, 0, new Label(getStringMessages().email()));
        result.setWidget(1, 1, getEmailBox());
        return result;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return getEmailBox();
    }
}
