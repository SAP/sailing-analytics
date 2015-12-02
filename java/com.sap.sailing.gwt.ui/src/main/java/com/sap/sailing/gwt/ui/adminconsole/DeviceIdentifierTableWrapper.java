package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.user.cellview.client.TextColumn;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.DeviceIdentifierDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;

public class DeviceIdentifierTableWrapper extends TableWrapper<DeviceIdentifierDTO, RefreshableMultiSelectionModel<DeviceIdentifierDTO>> {

    public DeviceIdentifierTableWrapper(SailingServiceAsync sailingService, StringMessages stringMessages,
            ErrorReporter errorReporter) {
        super(sailingService, stringMessages, errorReporter, /* multiSelection */ true, /* enablePager */ false, null /*EntityIdentityComparator for RefreshableSelectionModel*/);
        
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
