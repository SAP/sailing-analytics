package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sse.common.Position;
import com.sap.sse.common.Positioned;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Timed;
import com.sap.sse.datamining.annotations.Connector;

public interface HasManeuver extends Positioned, Timed {
    
    @Connector
    Maneuver getManeuver();

    @Override
    default TimePoint getTimePoint() {
        return getManeuver().getTimePoint();
    }

    @Override
    default Position getPosition() {
        return getManeuver().getPosition();
    }
}
