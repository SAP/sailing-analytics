package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.TimePoint;

public class GpsLatLong extends Fix {
    private static final long serialVersionUID = 5056284867725893553L;
    private final Position position;
    
    public GpsLatLong(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        this(timePoint, sensor, getPosition(valuesPerSubindex));
    }

    public GpsLatLong(TimePoint timePoint, Sensor sensor, Position position) {
        super(sensor, timePoint);
        this.position = position;
    }

    private static Position getPosition(Map<Integer, Object> valuesPerSubindex) {
        final double longitudeInDegrees = ((Number) valuesPerSubindex.get(1)).doubleValue();
        final double latitudeInDegrees = ((Number) valuesPerSubindex.get(2)).doubleValue();
        return new DegreePosition(latitudeInDegrees, longitudeInDegrees);
    }
    
    public Position getPosition() {
        return position;
    }

    @Override
    protected String localToString() {
        return position.toString();
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}
