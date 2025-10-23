package com.sap.sailing.gwt.ui.adminconsole.tractrac;

import com.sap.sailing.gwt.ui.shared.TracTracConfigurationWithSecurityDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.security.ui.client.UserService;

/**
 * Edits a {@link TracTracConfigurationWithSecurityDTO} object. Can be accessed from
 * {@link TracTracConnectionTableWrapper}
 */
public class TracTracConnectionEditDialog extends TracTracConnectionDialog {
    /**
     * The class creates the UI-dialog to edit the selected {@link TracTracConfigurationWithSecurityDTO}.
     * 
     * @param dtoToEdit
     *            The 'dtoToEdit' parameter contains the {@link TracTracConfigurationWithSecurityDTO} which should be edited.
     */
    public TracTracConnectionEditDialog(final TracTracConfigurationWithSecurityDTO dtoToEdit,
            final DialogCallback<TracTracConfigurationWithSecurityDTO> callback, final UserService userService,
            final ErrorReporter errorReporter) {
        super(callback, userService, errorReporter);
        setData(dtoToEdit);
        jsonURLTextBox.setReadOnly(true);
    }

    private void setData(final TracTracConfigurationWithSecurityDTO dtotoEdit) {
        name = dtotoEdit.getName();
        creatorName = dtotoEdit.getCreatorName();
        storedURITextBox.setText(dtotoEdit.getStoredDataURI()==null?"":dtotoEdit.getStoredDataURI());
        liveURITextBox.setText(dtotoEdit.getLiveDataURI()==null?"":dtotoEdit.getLiveDataURI());
        jsonURLTextBox.setText(dtotoEdit.getJsonUrl());
        tracTracUpdateURITextBox.setText(dtotoEdit.getUpdateURI()==null?"":dtotoEdit.getUpdateURI());
        if (!Util.hasLength(dtotoEdit.getTracTracApiToken())) {
            tracTracApiTokenTextBox.setText("");;
            if (dtotoEdit.isTracTracApiTokenAvailable()) {
                tracTracApiTokenTextBox.getElement().setAttribute("placeholder", "********");
            }
        } else {
            tracTracApiTokenTextBox.setText(dtotoEdit.getTracTracApiToken());
        }
        super.getOkButton().setEnabled(!jsonURLTextBox.getText().isEmpty());
    }
}
