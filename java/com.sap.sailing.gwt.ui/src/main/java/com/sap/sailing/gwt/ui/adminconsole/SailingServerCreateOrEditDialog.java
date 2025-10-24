package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RemoteSailingServerReferenceDTO;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class SailingServerCreateOrEditDialog extends DataEntryDialog<RemoteSailingServerReferenceDTO> {
    private StringMessages stringConstants;
    private TextBox nameTextBox;
    private TextBox urlTextBox;
    
    private static class SailingServerValidator implements Validator<RemoteSailingServerReferenceDTO> {
        private StringMessages stringConstants;
        private Iterable<RemoteSailingServerReferenceDTO> existingSailingServers;

        public SailingServerValidator(StringMessages stringConstants, Iterable<RemoteSailingServerReferenceDTO> existingSailingServers) {
            this.stringConstants = stringConstants;
            this.existingSailingServers = existingSailingServers;
        }

        @Override
        public String getErrorMessage(RemoteSailingServerReferenceDTO serverToValidate) {
            String errorMessage = null;
            boolean nameNotEmpty = serverToValidate.getName() != null && serverToValidate.getName().length() > 0;
            boolean urlNotEmpty = serverToValidate.getUrl() != null && serverToValidate.getUrl().length() > 0;
            boolean unique = true;
            for (RemoteSailingServerReferenceDTO event : existingSailingServers) {
                if (event.getName().equals(serverToValidate.getName())) {
                    unique = false;
                    break;
                }
            }
            if (!nameNotEmpty) {
                errorMessage = stringConstants.pleaseEnterAName();
            } else if (!urlNotEmpty) {
                errorMessage = stringConstants.pleaseEnterNonEmptyUrl();
            } else if (!unique) {
                errorMessage = stringConstants.eventWithThisNameAlreadyExists();
            }
            return errorMessage;
        }
    }

    public SailingServerCreateOrEditDialog(Iterable<RemoteSailingServerReferenceDTO> existingSailingServers,
            StringMessages stringConstants, DialogCallback<RemoteSailingServerReferenceDTO> callback) {
        this(existingSailingServers, null, false, stringConstants, callback);
    }

    public SailingServerCreateOrEditDialog(Iterable<RemoteSailingServerReferenceDTO> existingSailingServers, RemoteSailingServerReferenceDTO serverToEdit, StringMessages stringConstants,
            DialogCallback<RemoteSailingServerReferenceDTO> callback) {
    	this(existingSailingServers, serverToEdit, true, stringConstants, callback);
    }

    private SailingServerCreateOrEditDialog(Iterable<RemoteSailingServerReferenceDTO> existingSailingServers, RemoteSailingServerReferenceDTO serverToEdit, boolean isEditMode, StringMessages stringConstants,
            DialogCallback<RemoteSailingServerReferenceDTO> callback) {
        super("Sailing Server", null, stringConstants.ok(), stringConstants.cancel(),
        		new SailingServerValidator(stringConstants, existingSailingServers), callback);
        this.stringConstants = stringConstants;
        nameTextBox = createTextBox(isEditMode ? serverToEdit.getName() : null);
        nameTextBox.setVisibleLength(50);
        urlTextBox = createTextBox(isEditMode ? serverToEdit.getUrl() : null);
        urlTextBox.setVisibleLength(100);
        nameTextBox.setEnabled(!isEditMode);
    }

    @Override
    protected RemoteSailingServerReferenceDTO getResult() {
    	return new RemoteSailingServerReferenceDTO(nameTextBox.getText(), urlTextBox.getText());
    }

    @Override
    protected Widget getAdditionalWidget() {
        final VerticalPanel panel = new VerticalPanel();
        Widget additionalWidget = super.getAdditionalWidget();
        if (additionalWidget != null) {
            panel.add(additionalWidget);
        }
        Grid formGrid = new Grid(2, 2);
        panel.add(formGrid);
        formGrid.setWidget(0, 0, new Label(stringConstants.name() + ":"));
        formGrid.setWidget(0, 1, nameTextBox);
        formGrid.setWidget(1, 0, new Label(stringConstants.url() + ":"));
        formGrid.setWidget(1, 1, urlTextBox);
        return panel;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return nameTextBox;
    }
}
