package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;

/**
 * Speed through water
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class STW extends Fix {
    private static final long serialVersionUID = -7854740389524286036L;
    private final Speed speedThroughWater;
    
    public STW(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        super(sensor, timePoint);
        speedThroughWater = new KnotSpeedImpl(((Number) valuesPerSubindex.get(1)).doubleValue());
    }

    public Speed getSpeedThroughWater() {
        return speedThroughWater;
    }

    @Override
    protected String localToString() {
        return "STW: "+getSpeedThroughWater();
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}
