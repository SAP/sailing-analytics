package com.sap.sailing.gwt.ui.adminconsole.tractrac;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.TracTracConfigurationWithSecurityDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.security.ui.client.UserService;

/**
 * Creates a {@link TracTracConfigurationWithSecurityDTO} object. Can be accessed from
 * {@link TracTracEventManagementPanel}<p>
 * 
 * When the password field is empty, {@code null} will be set as the value of the
 * {@link TracTracConfigurationWithSecurityDTO#getTracTracPassword() password field} in the
 * result which shall be interpreted as "don't update" because TracTrac does not support
 * empty passwords.
 */
public class TracTracConnectionDialog extends DataEntryDialog<TracTracConfigurationWithSecurityDTO> {
    private static final StringMessages stringMessages = StringMessages.INSTANCE;
    private Grid grid;

    protected TextBox storedURITextBox;
    protected TextBox liveURITextBox;
    protected TextBox jsonURLTextBox;
    protected TextBox tracTracUpdateURITextBox;
    protected TextBox tractracUsernameTextBox;
    protected PasswordTextBox tractracPasswordTextBox;
    protected String creatorName;
    protected String name;

    /**
     * The class creates the UI-dialog to create a {@link TracTracConfigurationWithSecurityDTO}.
     */
    public TracTracConnectionDialog(
            final DialogCallback<TracTracConfigurationWithSecurityDTO> callback, final UserService userService,
            final ErrorReporter errorReporter) {
        super(stringMessages.tracTracConnection(), /* message */null, stringMessages.ok(), stringMessages.cancel(),
                /* validator */ null, /* animationEnabled */true, callback);
        this.ensureDebugId("TracTracConnectionDialog");
        createUi();
    }

    private void createUi() {
        grid = new Grid(7, 2);
        grid.setWidget(0, 0, new Label(stringMessages.details() + ":"));
        Label liveURILabel = new Label(stringMessages.liveUri() + ":");
        liveURILabel.setTitle(stringMessages.leaveEmptyForDefault());
        liveURITextBox = createTextBox("");
        liveURITextBox.ensureDebugId("LiveURITextBox");
        liveURITextBox.setVisibleLength(40);
        liveURITextBox.setTitle(stringMessages.leaveEmptyForDefault());
        grid.setWidget(1, 0, liveURILabel);
        grid.setWidget(1, 1, liveURITextBox);
        Label storedURILabel = new Label(stringMessages.storedUri() + ":");
        storedURILabel.setTitle(stringMessages.leaveEmptyForDefault());
        storedURITextBox = createTextBox("");
        storedURITextBox.ensureDebugId("StoredURITextBox");
        storedURITextBox.setVisibleLength(40);
        storedURITextBox.setTitle(stringMessages.leaveEmptyForDefault());
        grid.setWidget(2, 0, storedURILabel);
        grid.setWidget(2, 1, storedURITextBox);
        // JSON URL
        Label jsonURLLabel = new Label(stringMessages.jsonUrl() + ":");
        jsonURLTextBox = createTextBox("");
        jsonURLTextBox.ensureDebugId("JsonURLTextBox");
        jsonURLTextBox.setVisibleLength(100);
        // validation: User should not create empty connections
        jsonURLTextBox.addKeyUpHandler(e -> super.getOkButton().setEnabled(!jsonURLTextBox.getText().isEmpty()));
        grid.setWidget(3, 0, jsonURLLabel);
        grid.setWidget(3, 1, jsonURLTextBox);
        // Course design Update URL
        Label tracTracUpdateURLLabel = new Label(stringMessages.tracTracUpdateUrl() + ":");
        tracTracUpdateURITextBox = createTextBox("");
        tracTracUpdateURITextBox.ensureDebugId("TracTracUpdateURITextBox");
        tracTracUpdateURITextBox.setVisibleLength(100);
        tracTracUpdateURITextBox.setTitle(stringMessages.leaveEmptyForDefault());
        grid.setWidget(4, 0, tracTracUpdateURLLabel);
        grid.setWidget(4, 1, tracTracUpdateURITextBox);
        // TracTrac Username
        tractracUsernameTextBox = createTextBox("");
        tractracUsernameTextBox.ensureDebugId("TracTracUsernameTextBox");
        tractracUsernameTextBox.setVisibleLength(40);
        grid.setWidget(5, 0, new Label(stringMessages.tractracUsername() + ":"));
        grid.setWidget(5, 1, tractracUsernameTextBox);
        // TracTrac Password
        tractracPasswordTextBox = createPasswordTextBox("");
        tractracPasswordTextBox.ensureDebugId("TracTracPasswordTextBox");
        tractracPasswordTextBox.setVisibleLength(40);
        grid.setWidget(6, 0, new Label(stringMessages.tractracPassword() + ":"));
        grid.setWidget(6, 1, tractracPasswordTextBox);
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return jsonURLTextBox;
    }

    @Override
    protected TracTracConfigurationWithSecurityDTO getResult() {
        final String jsonURL = jsonURLTextBox.getValue();
        final String liveDataURI = liveURITextBox.getValue();
        final String storedDataURI = storedURITextBox.getValue();
        final String courseDesignUpdateURI = tracTracUpdateURITextBox.getValue();
        final String tractracUsername = tractracUsernameTextBox.getValue();
        final String tractracPassword = Util.hasLength(tractracPasswordTextBox.getValue()) ? tractracPasswordTextBox.getValue() : null;
        return new TracTracConfigurationWithSecurityDTO(name, jsonURL, liveDataURI, storedDataURI,
                courseDesignUpdateURI, tractracUsername, tractracPassword, creatorName);
    }

    @Override
    protected Widget getAdditionalWidget() {
        return grid;
    }

}
