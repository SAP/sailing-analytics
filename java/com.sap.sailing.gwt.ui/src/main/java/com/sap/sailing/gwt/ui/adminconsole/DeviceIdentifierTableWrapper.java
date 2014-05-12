package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.MultiSelectionModel;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.DeviceIdentifierDTO;

public class DeviceIdentifierTableWrapper extends TableWrapper<DeviceIdentifierDTO, MultiSelectionModel<DeviceIdentifierDTO>> {

    public DeviceIdentifierTableWrapper(SailingServiceAsync sailingService, StringMessages stringMessages,
            ErrorReporter errorReporter) {
        super(sailingService, stringMessages, errorReporter, new MultiSelectionModel<DeviceIdentifierDTO>());
        
        TextColumn<DeviceIdentifierDTO> typeColumn = new TextColumn<DeviceIdentifierDTO>() {
            @Override
            public String getValue(DeviceIdentifierDTO object) {
                return object.deviceType;
            }
        };
        
        TextColumn<DeviceIdentifierDTO> idColumn = new TextColumn<DeviceIdentifierDTO>() {
            @Override
            public String getValue(DeviceIdentifierDTO object) {
                return object.deviceId;
            }
        };
        
        table.addColumn(typeColumn, stringMessages.deviceType());
        table.addColumn(idColumn, stringMessages.deviceId());
    }

}
