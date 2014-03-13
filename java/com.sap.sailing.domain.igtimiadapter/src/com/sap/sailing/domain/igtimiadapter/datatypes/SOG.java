package com.sap.sailing.domain.igtimiadapter.datatypes;

import java.util.Map;

import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.igtimiadapter.IgtimiFixReceiver;
import com.sap.sailing.domain.igtimiadapter.Sensor;

/**
 * Speed over ground
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class SOG extends Fix {
    private static final long serialVersionUID = 6926121760778539823L;
    private final Speed speedOverGround;
    
    public SOG(TimePoint timePoint, Sensor sensor, Map<Integer, Object> valuesPerSubindex) {
        super(sensor, timePoint);
        speedOverGround = new KnotSpeedImpl(((Number) valuesPerSubindex.get(1)).doubleValue());
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
