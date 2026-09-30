package com.sap.sailing.declination.impl;

import com.sap.sailing.declination.Declination;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

public abstract class AbstractDeclinationRecord implements Declination {
    private static final long serialVersionUID = 2701783831406402231L;
    private final Position position;
    private final TimePoint timePoint;
    private final Bearing bearing;
    
    public AbstractDeclinationRecord(Position position, TimePoint timePoint, Bearing bearing) {
        this.position = position;
        this.timePoint = timePoint;
        this.bearing = bearing;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public TimePoint getTimePoint() {
        return timePoint;
    }

    @Override
    public Bearing getBearing() {
        return bearing;
    }

    @Override
    public String toString() {
        return ""+getTimePoint()+"@"+getPosition()+": "+getBearing();
    }
}
