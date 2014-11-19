package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.TimePoint;

public class GpsAltitude extends Fix {
    private static final long serialVersionUID = -5740764665236002412L;
    private final Distance altitude;
    
    public GpsAltitude(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        super(sensor, timePoint);
        altitude = new MeterDistance(((Number) valuesPerSubindex.get(1)).doubleValue());
    }

    public Distance getAltitude() {
        return altitude;
    }

    @Override
    protected String localToString() {
        return "Altitude "+getAltitude();
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}