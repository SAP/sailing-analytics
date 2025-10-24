package com.sap.sailing.gwt.ui.adminconsole.yellowbrick;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.adminconsole.TracTracEventManagementPanel;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.TracTracConfigurationWithSecurityDTO;
import com.sap.sailing.gwt.ui.shared.YellowBrickConfigurationWithSecurityDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.security.ui.client.UserService;

/**
 * Creates a {@link TracTracConfigurationWithSecurityDTO} object. Can be accessed from
 * {@link TracTracEventManagementPanel}. We don't expect to see a password sent from the server
 * to the client, so we won't even bother to insert any password into the password field as it is
 * inherently insecure (see also bug 5907). Instead, a password field left empty will lead to
 * a {@code null} value in the {@link #getResult() resulting} {@link YellowBrickConfigurationWithSecurityDTO}'s
 * {@link YellowBrickConfigurationWithSecurityDTO#getPassword() password} field, and the server shall
 * take this as an indication to not modify any existing password.
 */
public class YellowBrickConfigurationDialog extends DataEntryDialog<YellowBrickConfigurationWithSecurityDTO> {
    private static final StringMessages stringMessages = StringMessages.INSTANCE;
    private Grid grid;

    protected TextBox nameTextBox;
    protected TextBox raceURLTextBox;
    protected TextBox usernameTextBox;
    protected PasswordTextBox passwordTextBox;
    protected String creatorName;

    /**
     * The class creates the UI-dialog to create a {@link TracTracConfigurationWithSecurityDTO}.
     */
    public YellowBrickConfigurationDialog(
            final DialogCallback<YellowBrickConfigurationWithSecurityDTO> callback, final UserService userService,
            final ErrorReporter errorReporter) {
        super(stringMessages.yellowBrickConfiguration(), /* message */null, stringMessages.ok(), stringMessages.cancel(),
                /* validator */ null, /* animationEnabled */true, callback);
        this.ensureDebugId("TracTracConnectionDialog");
        createUi();
    }

    private void createUi() {
        grid = new Grid(7, 2);
        grid.setWidget(0, 0, new Label(stringMessages.details() + ":"));
        // Name
        Label nameLabel = new Label(stringMessages.name() + ":");
        nameTextBox = createTextBox("");
        nameTextBox.ensureDebugId("NameTextBox");
        nameTextBox.setVisibleLength(100);
        grid.setWidget(2, 0, nameLabel);
        grid.setWidget(2, 1, nameTextBox);
        // Race URL
        Label raceURLLabel = new Label(stringMessages.raceUrl() + ":");
        raceURLTextBox = createTextBox("");
        raceURLTextBox.ensureDebugId("RaceURLTextBox");
        raceURLTextBox.setVisibleLength(100);
        // validation: User should not create empty connections
        raceURLTextBox.addKeyUpHandler(e -> super.getOkButton().setEnabled(!raceURLTextBox.getText().isEmpty()));
        grid.setWidget(3, 0, raceURLLabel);
        grid.setWidget(3, 1, raceURLTextBox);
        // TracTrac Username
        usernameTextBox = createTextBox("");
        usernameTextBox.ensureDebugId("YellowBrickUsernameTextBox");
        usernameTextBox.setVisibleLength(40);
        grid.setWidget(5, 0, new Label(stringMessages.username() + ":"));
        grid.setWidget(5, 1, usernameTextBox);
        // TracTrac Password
        passwordTextBox = createPasswordTextBox("");
        passwordTextBox.ensureDebugId("YellowBrickPasswordTextBox");
        passwordTextBox.setVisibleLength(40);
        grid.setWidget(6, 0, new Label(stringMessages.password() + ":"));
        grid.setWidget(6, 1, passwordTextBox);
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return raceURLTextBox;
    }

    @Override
    protected YellowBrickConfigurationWithSecurityDTO getResult() {
        return new YellowBrickConfigurationWithSecurityDTO(nameTextBox.getValue(),
                raceURLTextBox.getValue(), usernameTextBox.getValue(),
                Util.hasLength(passwordTextBox.getValue()) ? passwordTextBox.getValue() : null,
                creatorName);
    }

    @Override
    protected Widget getAdditionalWidget() {
        return grid;
    }

}
