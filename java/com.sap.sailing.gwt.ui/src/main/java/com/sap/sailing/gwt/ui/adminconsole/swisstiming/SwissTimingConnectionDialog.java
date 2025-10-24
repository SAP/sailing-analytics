package com.sap.sailing.gwt.ui.adminconsole.swisstiming;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.SwissTimingConfigurationWithSecurityDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.security.ui.client.UserService;

/**
 * Creates a {@link SwissTimingConfigurationWithSecurityDTO} object. Can be accessed from
 * {@link SwissTimingEventManagementPanel}. The Manage2Sail event ID and the complete URL
 * field update each other mutually upon manual update.
 */
public class SwissTimingConnectionDialog extends DataEntryDialog<SwissTimingConfigurationWithSecurityDTO> {
    private static final StringMessages stringMessages = StringMessages.INSTANCE;

    private Grid grid;

    protected TextBox manage2SailEventIdTextBox;
    protected TextBox manage2SailEventUrlJsonTextBox;
    protected TextBox hostnameTextBox;
    protected TextBox portTextBox;
    protected TextBox updateUrlTextBox;
    protected TextBox updateUsernameTextBox;
    protected PasswordTextBox updatePasswordTextBox;
    protected String name;
    protected String creatorName;
    
    
    private static class EmptyFieldValidator implements Validator<SwissTimingConfigurationWithSecurityDTO> {
        @Override
        public String getErrorMessage(SwissTimingConfigurationWithSecurityDTO valueToValidate) {
            final String result;
            if (valueToValidate.getJsonUrl() == null || valueToValidate.getJsonUrl().trim().isEmpty()) {
                result = stringMessages.pleaseEnterNonEmptyUrl();
            } else {
                result = null;
            }
            return result;
        }
    }

    /**
     * The class creates the UI-dialog create a new {@link SwissTimingConfigurationWithSecurityDTO}.
     */
    public SwissTimingConnectionDialog(final DialogCallback<SwissTimingConfigurationWithSecurityDTO> callback,
            final UserService userService, final ErrorReporter errorReporter) {
        super(stringMessages.swissTimingConnections(), null, stringMessages.ok(), stringMessages.cancel(),
                /* validator */ new EmptyFieldValidator(), /* animationEnabled */true, callback);
        this.ensureDebugId("SwissTimingConnectionEditDialog");
        createUi();
    }

    private void createUi() {
        grid = new Grid(8, 2);
        grid.setWidget(0, 0, new Label(stringMessages.details() + ":"));
        // Manage2SailEventId
        final Label manage2SailEventIdLabel = new Label(stringMessages.manage2SailEventIdBox() + ":");
        manage2SailEventIdLabel.setTitle(stringMessages.leaveEmptyForDefault());
        manage2SailEventIdTextBox = createTextBox("");
        manage2SailEventIdTextBox.ensureDebugId("Manage2SailEventIdTextBox");
        manage2SailEventIdTextBox.setVisibleLength(40);
        manage2SailEventIdTextBox.setTitle(stringMessages.manage2SailEventIdBox());
        // update url with changed event id
        manage2SailEventIdTextBox.addKeyUpHandler(e -> updateUrlFromEventId(manage2SailEventIdTextBox.getText()));
        grid.setWidget(1, 0, manage2SailEventIdLabel);
        grid.setWidget(1, 1, manage2SailEventIdTextBox);
        // Manage2SailEventUrl
        final Label manage2SailEventUrlJsonLabel = new Label(stringMessages.manage2SailEventURLBox() + ":");
        manage2SailEventUrlJsonLabel.setTitle(stringMessages.leaveEmptyForDefault());
        manage2SailEventUrlJsonTextBox = createTextBox("");
        manage2SailEventUrlJsonTextBox.ensureDebugId("Manage2SailEventUrlJsonTextBox");
        manage2SailEventUrlJsonTextBox.setVisibleLength(40);
        manage2SailEventUrlJsonTextBox.setTitle(stringMessages.manage2SailEventURLBox());
        grid.setWidget(2, 0, manage2SailEventUrlJsonLabel);
        grid.setWidget(2, 1, manage2SailEventUrlJsonTextBox);
        // Hostname
        final Label hostnameLabel = new Label(stringMessages.hostname() + ":");
        manage2SailEventUrlJsonLabel.setTitle(stringMessages.leaveEmptyForDefault());
        hostnameTextBox = createTextBox("");
        hostnameTextBox.ensureDebugId("HostnameTextBox");
        hostnameTextBox.setVisibleLength(40);
        hostnameTextBox.setTitle(stringMessages.hostname());
        grid.setWidget(3, 0, hostnameLabel);
        grid.setWidget(3, 1, hostnameTextBox);
        // Port
        final Label portLabel = new Label(stringMessages.manage2SailPort() + ":");
        portLabel.setTitle(stringMessages.leaveEmptyForDefault());
        portTextBox = createTextBox("");
        portTextBox.ensureDebugId("PortTextBox");
        portTextBox.setVisibleLength(40);
        portTextBox.setTitle(stringMessages.manage2SailPort());
        grid.setWidget(4, 0, portLabel);
        grid.setWidget(4, 1, portTextBox);
        // Update URL
        final Label updateUrlLabel = new Label(stringMessages.swissTimingUpdateURL() + ":");
        updateUrlLabel.setTitle(stringMessages.leaveEmptyForDefault());
        updateUrlTextBox = createTextBox("");
        updateUrlTextBox.ensureDebugId("UpdateUrlTextBox");
        updateUrlTextBox.setVisibleLength(40);
        updateUrlTextBox.setTitle(stringMessages.swissTimingUpdateURL());
        grid.setWidget(5, 0, updateUrlLabel);
        grid.setWidget(5, 1, updateUrlTextBox);
        // Update Username
        final Label updateUsernameLabel = new Label(stringMessages.swissTimingUpdateUsername() + ":");
        updateUsernameLabel.setTitle(stringMessages.leaveEmptyForDefault());
        updateUsernameTextBox = createTextBox("");
        updateUsernameTextBox.ensureDebugId("UpdateUsernameTextBox");
        updateUsernameTextBox.setVisibleLength(40);
        updateUsernameTextBox.setTitle(stringMessages.swissTimingUpdateUsername());
        grid.setWidget(6, 0, updateUsernameLabel);
        grid.setWidget(6, 1, updateUsernameTextBox);
        // Update Password
        final Label updatePasswordLabel = new Label(stringMessages.swissTimingUpdatePassword() + ":");
        updatePasswordLabel.setTitle(stringMessages.leaveEmptyForDefault());
        updatePasswordTextBox = createPasswordTextBox("");
        updatePasswordTextBox.ensureDebugId("UpdatePasswordTextBox");
        updatePasswordTextBox.setVisibleLength(40);
        updatePasswordTextBox.setTitle(stringMessages.swissTimingUpdatePassword());
        grid.setWidget(7, 0, updatePasswordLabel);
        grid.setWidget(7, 1, updatePasswordTextBox);
    }

    /**
     * This function tries to infer a valid JsonUrl for any input given that matches the pattern of an event Id from
     * M2S. If there is an event id detected the Json Url gets updated and the event Id textbox is filled with the
     * detected event Id. The ID pattern is defined in {@link eventIdPattern}.
     */
    private void updateUrlFromEventId(String eventIdText) {
        final String result = SwissTimingEventIdUrlUtil.getUrlFromEventId(eventIdText);
        if (result != null) {
            manage2SailEventUrlJsonTextBox.setValue(result, /* fire events */ false);
            validateAndUpdate();
        }
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return manage2SailEventUrlJsonTextBox;
    }

    @Override
    protected SwissTimingConfigurationWithSecurityDTO getResult() {
        Integer port = null;
        if (!portTextBox.getText().isEmpty()) {
            try {
                port = Integer.parseInt(portTextBox.getText());
            } catch (NumberFormatException e) {
                // port will be null.
            }
        }

        return new SwissTimingConfigurationWithSecurityDTO(name,
                manage2SailEventUrlJsonTextBox.getValue(), hostnameTextBox.getValue(), port,
                updateUrlTextBox.getValue(), updateUsernameTextBox.getValue(), updatePasswordTextBox.getValue(),
                creatorName);
    }

    @Override
    protected Widget getAdditionalWidget() {
        return grid;
    }
}
