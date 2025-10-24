package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.TimePoint;

public class BatteryLevel extends Fix {
    private static final long serialVersionUID = -8499201867081319088L;
    private final double percentage;
    
    public BatteryLevel(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        this(timePoint, sensor, ((Number) valuesPerSubindex.get(1)).doubleValue());
    }

    public BatteryLevel(TimePoint timePoint, Sensor sensor, double percentage) {
        super(sensor, timePoint);
        this.percentage = percentage;
    }

    public double getPercentage() {
        return percentage;
    }

    @Override
    protected String localToString() {
        return "Battery: "+getPercentage()+"%";
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}
