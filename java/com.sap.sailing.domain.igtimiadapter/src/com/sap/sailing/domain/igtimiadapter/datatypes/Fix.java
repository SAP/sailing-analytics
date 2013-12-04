package com.sap.sailing.domain.igtimiadapter.datatypes;

import com.sap.sailing.domain.base.Timed;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.igtimiadapter.Sensor;

public abstract class Fix implements Timed {
    private static final long serialVersionUID = -486253194440558670L;
    private final TimePoint timePoint;
    private final Sensor sensor;

    protected Fix(Sensor sensor, TimePoint timePoint) {
        super();
        this.timePoint = timePoint;
        this.sensor = sensor;
    }
    
    public Type getType() {
        return Type.getType(this.getClass());
    }

    @Override
    public TimePoint getTimePoint() {
        return timePoint;
    }

    public Sensor getSensor() {
        return sensor;
    }
    
    abstract protected String localToString();
    
    @Override
    public String toString() {
        return localToString() + " at "+getTimePoint()+" from "+getSensor();
    }
}
