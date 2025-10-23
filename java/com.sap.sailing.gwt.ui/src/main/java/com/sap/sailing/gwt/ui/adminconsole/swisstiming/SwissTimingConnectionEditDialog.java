package com.sap.sailing.gwt.ui.adminconsole.swisstiming;

import com.sap.sailing.gwt.ui.shared.SwissTimingConfigurationWithSecurityDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.security.ui.client.UserService;

/**
 * Edits a {@link SwissTimingConfigurationWithSecurityDTO} object. Can be accessed from
 * {@link SwissTimingConnectionTableWrapper}.The Manage2Sail event ID and the complete URL
 * field update each other mutually upon manual update.
 */
public class SwissTimingConnectionEditDialog extends SwissTimingConnectionDialog {
    /**
     * The class creates the UI-dialog to edit a selected {@link SwissTimingConfigurationWithSecurityDTO}.
     * 
     * @param dtoToEdit
     *            The 'dtoToEdit' parameter contains the {@link SwissTimingConfigurationWithSecurityDTO} which should be edited.
     */
    public SwissTimingConnectionEditDialog(final SwissTimingConfigurationWithSecurityDTO dtoToEdit,
            final DialogCallback<SwissTimingConfigurationWithSecurityDTO> callback, final UserService userService,
            final ErrorReporter errorReporter) {
        super(callback, userService, errorReporter);
        setData(dtoToEdit);
        manage2SailEventUrlJsonTextBox.setReadOnly(true);
    }

    private void setData(final SwissTimingConfigurationWithSecurityDTO dtoToEdit) {
        name = dtoToEdit.getName();
        creatorName = dtoToEdit.getCreatorName();
        manage2SailEventUrlJsonTextBox.setText(dtoToEdit.getJsonUrl());
        hostnameTextBox.setText(dtoToEdit.getHostname());
        portTextBox.setText(dtoToEdit.getPort() == null ? "" : ("" + dtoToEdit.getPort()));
        updateUrlTextBox.setText(dtoToEdit.getUpdateURL());
        if (!Util.hasLength(dtoToEdit.getApiToken())) {
            updateApiTokenTextBox.setText("");
            if (dtoToEdit.isApiTokenAvailable()) {
                updateApiTokenTextBox.getElement().setAttribute("placeholder", "********");
            }
        } else {
            updateApiTokenTextBox.setText(dtoToEdit.getApiToken());
        }
        validateAndUpdate();
    }
}
