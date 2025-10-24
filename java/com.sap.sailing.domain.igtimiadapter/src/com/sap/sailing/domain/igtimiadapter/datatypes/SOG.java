package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.common.impl.KilometersPerHourSpeedImpl;
import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;

/**
 * Speed over ground expressed in kilometers per hour
 * 
 * @see AWS
 * @author Axel Uhl (d043530)
 *
 */
public class SOG extends Fix {
    private static final long serialVersionUID = 6926121760778539823L;
    private final Speed speedOverGround;
    
    public SOG(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        this(timePoint, sensor, new KilometersPerHourSpeedImpl(((Number) valuesPerSubindex.get(1)).doubleValue()));
    }

    public SOG(TimePoint timePoint, Sensor sensor, Speed speedOverGround) {
        super(sensor, timePoint);
        this.speedOverGround = speedOverGround;
    }

    public Speed getSpeedOverGround() {
        return speedOverGround;
    }

    @Override
    protected String localToString() {
        return "SOG: "+getSpeedOverGround();
    }

    @Override
    public void notify(IgtimiFixReceiver receiver) {
        receiver.received(this);
    }
}
