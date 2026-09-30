package com.sap.sailing.gwt.ui.shared;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.Position;

public class WindFieldDTO implements IsSerializable {

    public double curSpeed;
    public double curBearing;
    
    public static class WindData implements IsSerializable {
        public Position rcStart;
        public Position rcEnd;
        public int resX;
        public int resY;
        public int borderX;
        public int borderY;
        public double xScale;
    }
    
    public WindData windData;
    
    private List<SimulatorWindDTO> matrix;
    
    private WindLinesDTO windLinesDTO;

    public List<SimulatorWindDTO> getMatrix() {
        return matrix;
    }

    public void setMatrix(List<SimulatorWindDTO> matrix) {
        this.matrix = matrix;
    }

    public WindLinesDTO getWindLinesDTO() {
        return windLinesDTO;
    }

    public void setWindLinesDTO(WindLinesDTO windLinesDTO) {
        this.windLinesDTO = windLinesDTO;
    }

}
