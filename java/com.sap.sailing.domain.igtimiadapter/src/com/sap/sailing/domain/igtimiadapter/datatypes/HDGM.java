package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * Magnetic heading
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class HDGM extends Fix {
    private static final long serialVersionUID = -3344091983385126284L;
    private final Bearing magneticHeading;
    
    public HDGM(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        this(timePoint, sensor, new DegreeBearingImpl((((Number) valuesPerSubindex.get(1)).doubleValue()+360.)%360.));
    }

    public HDGM(TimePoint timePoint, Sensor sensor, Bearing magneticHeading) {
        super(sensor, timePoint);
        this.magneticHeading = magneticHeading;
    }

    public Bearing getMagneticHeading() {
        return magneticHeading;
    }

    @Override
    protected String localToString() {
        return "HDGM: "+getMagneticHeading();
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}
