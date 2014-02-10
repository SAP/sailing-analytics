package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.TypeBasedJsonDeserializer;

public class GPSFixMovingJsonDeserializer extends TypeBasedJsonDeserializer<GPSFixMoving> {
    public static final String TYPE = "GPSFixMoving";
    
    public static final String FIELD_BEARING_DEG = "bearing_deg";
    public static final String FIELD_SPEED_KNOTS = "speed_knots";

    @Override
    protected GPSFixMoving deserializeAfterCheckingType(JSONObject object) throws JsonDeserializationException {        
        double bearingDeg = (Double) object.get(FIELD_BEARING_DEG);
        double speedKnots = (Double) object.get(FIELD_SPEED_KNOTS);

        JSONObject clone = (JSONObject) object.clone();
        clone.put(TypeBasedJsonDeserializer.FIELD_TYPE, GPSFixJsonDeserializer.TYPE);
        GPSFix baseFix = new GPSFixJsonDeserializer()
        .deserialize(object);

        // TODO do we have to use corrected course here? (corrected by magnetic
        // variation)
        Bearing bearing = new DegreeBearingImpl(bearingDeg);
        SpeedWithBearing speed = new KnotSpeedWithBearingImpl(
                speedKnots, bearing);

        GPSFixMoving fix = new GPSFixMovingImpl(baseFix.getPosition(),
                baseFix.getTimePoint(), speed);

        return fix;
    }

    @Override
    protected String getType() {
        return TYPE;
    }
}
