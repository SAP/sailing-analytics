package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.server.gateway.deserialization.TypeBasedJsonDeserializer;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.shared.json.JsonDeserializationException;

public class GPSFixMovingJsonDeserializer extends TypeBasedJsonDeserializer<GPSFixMoving> {
    public static final String TYPE = "GPSFixMoving";
    
    public static final String FIELD_BEARING_DEG = "bearing_deg";
    public static final String FIELD_SPEED_KNOTS = "speed_knots";
    public static final Object FIELD_TRUE_HEADING_DEG = "true_heading_deg";

    @Override
    protected GPSFixMoving deserializeAfterCheckingType(JSONObject object) throws JsonDeserializationException {        
        final double bearingDeg = (Double) object.get(FIELD_BEARING_DEG);
        final double speedKnots = (Double) object.get(FIELD_SPEED_KNOTS);
        final JSONObject clone = (JSONObject) object.clone();
        clone.put(TypeBasedJsonDeserializer.FIELD_TYPE, GPSFixJsonDeserializer.TYPE);
        final GPSFix baseFix = new GPSFixJsonDeserializer().deserialize(clone);
        final Bearing bearing = new DegreeBearingImpl(bearingDeg);
        final SpeedWithBearing speed = new KnotSpeedWithBearingImpl(
                speedKnots, bearing);
        final Bearing optionalTrueHeading;
        if (object.containsKey(FIELD_TRUE_HEADING_DEG)) {
            optionalTrueHeading = new DegreeBearingImpl((Double) object.get(FIELD_TRUE_HEADING_DEG));
        } else {
            optionalTrueHeading = null;
        }
        final GPSFixMoving fix = new GPSFixMovingImpl(baseFix.getPosition(),
                baseFix.getTimePoint(), speed, optionalTrueHeading);
        return fix;
    }

    @Override
    protected String getType() {
        return TYPE;
    }
}
