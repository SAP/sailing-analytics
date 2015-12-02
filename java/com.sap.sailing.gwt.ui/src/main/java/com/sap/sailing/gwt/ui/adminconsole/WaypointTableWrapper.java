package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.user.cellview.client.TextColumn;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.ControlPointDTO;
import com.sap.sailing.gwt.ui.shared.GateDTO;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.WaypointDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.celltable.RefreshableSelectionModel;

public class WaypointTableWrapper<S extends RefreshableSelectionModel<WaypointDTO>> extends TableWrapper<WaypointDTO, S> {    
    public WaypointTableWrapper(boolean multiSelection, SailingServiceAsync sailingService, final StringMessages stringMessages,
            ErrorReporter errorReporter) {
        super(sailingService, stringMessages, errorReporter, multiSelection, true, null /*EntityIdentityComparator for RefreshableSelectionModel*/);
        
        TextColumn<WaypointDTO> nameColumn = new TextColumn<WaypointDTO>() {
            @Override
            public String getValue(WaypointDTO d) {
                return d.controlPoint.getName();
            }
        };
        table.addColumn(nameColumn, stringMessages.controlPoint() + " " + stringMessages.name());
        
        TextColumn<WaypointDTO> typeColumn = new TextColumn<WaypointDTO>() {
            @Override
            public String getValue(WaypointDTO w) {
                ControlPointDTO d = w.controlPoint;
                if (d instanceof GateDTO) {
                    return "2x " + stringMessages.mark();
                } else if (d instanceof MarkDTO) {
                    return stringMessages.mark();
                } else {
                    return stringMessages.unknown();
                }
            }
        };
        table.addColumn(typeColumn, stringMessages.type());
        
        TextColumn<WaypointDTO> passingInstructionsColumn = new TextColumn<WaypointDTO>() {
            @Override
            public String getValue(WaypointDTO waypoint) {
                return waypoint.passingInstructions != null ? waypoint.passingInstructions.name() : "";
            }
        };
        table.addColumn(passingInstructionsColumn, stringMessages.passingInstructions());
    }
    
    @Override
    public void refresh(Iterable<WaypointDTO> wayPoints) {
        super.refresh(wayPoints);
        ((RefreshableSelectionModel<WaypointDTO>) getSelectionModel()).refreshSelectionModel(wayPoints);
    }
}
