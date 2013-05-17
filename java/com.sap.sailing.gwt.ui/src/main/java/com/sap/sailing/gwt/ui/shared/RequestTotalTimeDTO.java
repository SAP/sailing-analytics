package com.sap.sailing.gwt.ui.shared;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.common.dto.PositionDTO;

public class RequestTotalTimeDTO implements IsSerializable {

    public SimulatorUISelectionDTO selection = null;
    public List<SimulatorWindDTO> allPoints = null;
    public List<PositionDTO> turnPoints = null;
    public boolean useRealAverageWindSpeed = false;
    public int stepDurationMilliseconds = 0;

    public RequestTotalTimeDTO() {
        this.selection = null;
        this.allPoints = new ArrayList<SimulatorWindDTO>();
        this.turnPoints = new ArrayList<PositionDTO>();
        this.useRealAverageWindSpeed = true;
        this.stepDurationMilliseconds = 2000;
    }

    public RequestTotalTimeDTO(SimulatorUISelectionDTO selection, int stepDurationMilliseconds, List<SimulatorWindDTO> allPoints, List<PositionDTO> turnPoints,
            boolean useRealAverageWindSpeed) {
        this.selection = selection;
        this.allPoints = allPoints;
        this.turnPoints = turnPoints;
        this.useRealAverageWindSpeed = useRealAverageWindSpeed;
        this.stepDurationMilliseconds = stepDurationMilliseconds;
    }
}
