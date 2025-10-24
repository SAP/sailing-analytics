package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * Apparent wind angle, relative to the device's or vessel's orientation, in degrees from 0 to 360, with values 180-360
 * meaning the port side, and values 0-180 meaning starboard side.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class AWA extends Fix {
    private static final long serialVersionUID = -5979975741914782196L;
    private final Bearing apparentWindAngle;
    
    public AWA(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        this(timePoint, sensor, new DegreeBearingImpl(((Number) valuesPerSubindex.get(1)).doubleValue()));
    }

    public AWA(TimePoint timePoint, Sensor sensor, Bearing apparentWindAngle) {
        super(sensor, timePoint);
        this.apparentWindAngle = apparentWindAngle;
    }

    public Bearing getApparentWindAngle() {
        return apparentWindAngle;
    }

    @Override
    protected String localToString() {
        return "AWA: "+getApparentWindAngle();
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}
