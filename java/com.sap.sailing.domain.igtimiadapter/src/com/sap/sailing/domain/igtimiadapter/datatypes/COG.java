package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.igtimiadapter.Sensor;

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
        super(sensor, timePoint);
        courseOverGround = new DegreeBearingImpl(((Number) valuesPerSubindex.get(1)).doubleValue());
    }

    public Bearing getCourseOverGround() {
        return courseOverGround;
    }

    @Override
    protected String localToString() {
        return "COG: "+getCourseOverGround();
    }
}
