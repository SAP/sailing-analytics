package com.sap.sailing.domain.yellowbrickadapter.impl;

import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Timed;

public class TeamPosition implements Timed {
    private static final long serialVersionUID = -1909415489360940954L;

    private final SpeedWithBearing motionVector;
    private final TimePoint timePoint;
    private final long id;
    private final Position position;

    public TeamPosition(SpeedWithBearing motionVector, TimePoint timePoint, long id, Position position) {
        super();
        this.motionVector = motionVector;
        this.timePoint = timePoint;
        this.id = id;
        this.position = position;
    }

    @Override
    public TimePoint getTimePoint() {
        return timePoint;
    }

    public SpeedWithBearing getMotionVector() {
        return motionVector;
    }

    public long getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }
    
    public GPSFixMoving getGPSFixMoving() {
        return new GPSFixMovingImpl(getPosition(), getTimePoint(), getMotionVector(), /* optionalTrueHeading */ null);
    }
    
    @Override
    public String toString() {
        return ""+id+":"+getGPSFixMoving();
    }
}
