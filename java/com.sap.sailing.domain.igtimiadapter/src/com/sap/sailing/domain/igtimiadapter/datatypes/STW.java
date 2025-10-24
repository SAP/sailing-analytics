package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.common.impl.KilometersPerHourSpeedImpl;
import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;

/**
 * Speed through water expressed in kilometers per hour.
 * 
 * @see AWS
 * @author Axel Uhl (d043530)
 *
 */
public class STW extends Fix {
    private static final long serialVersionUID = -7854740389524286036L;
    private final Speed speedThroughWater;
    
    public STW(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        this(timePoint, sensor, new KilometersPerHourSpeedImpl(((Number) valuesPerSubindex.get(1)).doubleValue()));
    }

    public STW(TimePoint timePoint, Sensor sensor, Speed speedThroughWater) {
        super(sensor, timePoint);
        this.speedThroughWater = speedThroughWater;
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
