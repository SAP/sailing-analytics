package com.sap.sailing.gwt.ui.shared;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.Position;

public class GPSFixDTO implements IsSerializable {
    public Date timepoint;
    public Position position;
    
    public GPSFixDTO() {}

    public GPSFixDTO(Date timepoint, Position position) {
        super();
        this.timepoint = timepoint;
        this.position = position;
    }
}
