package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.Distance;
import com.sap.sse.common.TimePoint;

public class GpsQualityHdop extends Fix {
    private static final long serialVersionUID = -5319766127405258823L;
    private final Distance hdop;
    
    public GpsQualityHdop(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        this(timePoint, sensor, new MeterDistance(((Number) valuesPerSubindex.get(1)).doubleValue()));
    }

    public GpsQualityHdop(TimePoint timePoint, Sensor sensor, Distance hdop) {
        super(sensor, timePoint);
        this.hdop = hdop;
    }

    public Distance getHdop() {
        return hdop;
    }

    @Override
    protected String localToString() {
        return "HDOP: "+getHdop();
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}
