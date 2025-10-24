package com.sap.sailing.gwt.ui.adminconsole.swisstiming;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.SwissTimingArchiveConfigurationWithSecurityDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.security.ui.client.UserService;

/**
 * Creates a {@link SwissTimingArchiveConfigurationWithSecurityDTO} object. Can be accessed from
 * {@link SwissTimingReplayConnectorPanel}
 */
public class SwissTimingArchivedConnectionDialog
        extends DataEntryDialog<SwissTimingArchiveConfigurationWithSecurityDTO> {
    private static final StringMessages stringMessages = StringMessages.INSTANCE;
    private Grid grid;

    protected TextBox jsonURLTextBox;
    protected String creatorName;

    private static class EmptyTextValidator implements Validator<SwissTimingArchiveConfigurationWithSecurityDTO> {
        @Override
        public String getErrorMessage(SwissTimingArchiveConfigurationWithSecurityDTO valueToValidate) {
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
     * The class creates the UI-dialog to create a new {@link SwissTimingArchiveConfigurationWithSecurityDTO}.
     */
    public SwissTimingArchivedConnectionDialog(
            final DialogCallback<SwissTimingArchiveConfigurationWithSecurityDTO> callback,
            final UserService userService, final ErrorReporter errorReporter) {
        super(stringMessages.swissTimingAchivedConnection(), null, stringMessages.ok(), stringMessages.cancel(),
                /* validator */ new EmptyTextValidator(), /* animationEnabled */true, callback);
        this.ensureDebugId("SwissTimingArchivedConnectionDialog");
        createUi();
    }

    private void createUi() {
        grid = new Grid(2, 2);
        grid.setWidget(0, 0, new Label(stringMessages.details() + ":"));
        // JSON URL
        Label jsonURLLabel = new Label(stringMessages.jsonUrl() + ":");
        jsonURLTextBox = createTextBox("");
        jsonURLTextBox.ensureDebugId("JsonURLTextBox");
        jsonURLTextBox.setVisibleLength(100);
        // validation: User should not create empty connections
        grid.setWidget(1, 0, jsonURLLabel);
        grid.setWidget(1, 1, jsonURLTextBox);
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return jsonURLTextBox;
    }

    @Override
    protected SwissTimingArchiveConfigurationWithSecurityDTO getResult() {
        final String jsonURL = jsonURLTextBox.getValue();
        return new SwissTimingArchiveConfigurationWithSecurityDTO(jsonURL, creatorName);
    }

    @Override
    protected Widget getAdditionalWidget() {
        return grid;
    }

}
