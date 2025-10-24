package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * Course over ground
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class COG extends Fix {
    private static final long serialVersionUID = 1105936972187917331L;
    private final Bearing courseOverGround;
    
    public COG(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        this(timePoint, sensor, new DegreeBearingImpl(((Number) valuesPerSubindex.get(1)).doubleValue())); 
    }

    public COG(TimePoint timePoint, Sensor sensor, Bearing courseOverGround) {
        super(sensor, timePoint);
        this.courseOverGround = courseOverGround;
    }

    public Bearing getCourseOverGround() {
        return courseOverGround;
    }

    @Override
    protected String localToString() {
        return "COG: "+getCourseOverGround();
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}
