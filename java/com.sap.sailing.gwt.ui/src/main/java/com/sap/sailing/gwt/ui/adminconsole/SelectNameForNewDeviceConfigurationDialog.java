package com.sap.sailing.gwt.ui.adminconsole;

import java.util.List;

import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.DeviceConfigurationDTO;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class SelectNameForNewDeviceConfigurationDialog extends DataEntryDialog<String> {
    public static class MatcherValidator implements Validator<String> {
        private List<? extends DeviceConfigurationDTO> allConfigurations;
        private final StringMessages stringMessages;
    
        public MatcherValidator(List<? extends DeviceConfigurationDTO> allConfigurations,
                StringMessages stringMessages) {
            this.allConfigurations = allConfigurations;
            this.stringMessages = stringMessages;
        }
    
        @Override
        public String getErrorMessage(String nameForNewDeviceConfiguration) {
            if (nameForNewDeviceConfiguration.isEmpty()) {
                return stringMessages.enterDeviceIdentifierName();
            }
            for (DeviceConfigurationDTO existingConfiguration : allConfigurations) {
                if (existingConfiguration.name.equals(nameForNewDeviceConfiguration)) {
                    return stringMessages.thereIsAlreadyAConfigurationForThisDevice();
                }
            }
            return null;
        }
    }

    private final TextBox identifierBox;
    private final StringMessages stringMessages;
    
    public SelectNameForNewDeviceConfigurationDialog(StringMessages stringMessages,
            Validator<String> validator, com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback<String> callback) {
        super(stringMessages.createDeviceConfiguration(), stringMessages.forWhichDeviceShouldConfigurationApply(), 
                stringMessages.create(), stringMessages.cancel(), validator, callback);
        this.ensureDebugId("SelectNameForNewDeviceConfigurationDialog");
        this.stringMessages = stringMessages;
        this.identifierBox = createTextBox("");
        this.identifierBox.ensureDebugId("raceManagerDeviceName");
    }
    
    @Override
    protected Widget getAdditionalWidget() {
        Grid grid = new Grid(1, 2);
        grid.setWidget(0, 0, createLabel(stringMessages.raceManagerDeviceName()));
        grid.setWidget(0, 1, identifierBox);
        return grid;
    }

    @Override
    protected Focusable getInitialFocusWidget() {
        return identifierBox;
    }

    @Override
    protected String getResult() {
        return identifierBox.getValue();
    }
}
