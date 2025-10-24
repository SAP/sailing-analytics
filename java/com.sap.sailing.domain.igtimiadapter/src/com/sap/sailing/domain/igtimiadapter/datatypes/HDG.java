package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * True heading; the difference to {@link HDGM} is the local declination at the point in time the measurement was made
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class HDG extends Fix {
    private static final long serialVersionUID = -2400420107580758551L;
    private final Bearing trueHeading;
    
    public HDG(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        this(timePoint, sensor, new DegreeBearingImpl((((Number) valuesPerSubindex.get(1)).doubleValue()+360.)%360.));
    }

    public HDG(TimePoint timePoint, Sensor sensor, Bearing trueHeading) {
        super(sensor, timePoint);
        this.trueHeading = trueHeading;
    }

    public Bearing getTrueHeading() {
        return trueHeading;
    }

    @Override
    protected String localToString() {
        return "HDG: "+getTrueHeading();
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}
