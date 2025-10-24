package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.TimePoint;

public class GpsQualityIndicator extends Fix {
    private static final long serialVersionUID = 1112428061064399194L;
    private final int quality;
    
    public GpsQualityIndicator(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        this(timePoint, sensor, ((Number) valuesPerSubindex.get(1)).intValue());
    }

    public GpsQualityIndicator(TimePoint timePoint, Sensor sensor, int quality) {
        super(sensor, timePoint);
        this.quality = quality;
    }

    public int getQuality() {
        return quality;
    }

    @Override
    protected String localToString() {
        return "GPS Quality: "+getQuality();
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}
