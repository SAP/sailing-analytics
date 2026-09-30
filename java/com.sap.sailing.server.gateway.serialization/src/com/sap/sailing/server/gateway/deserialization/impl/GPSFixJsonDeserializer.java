package com.sap.sailing.server.gateway.deserialization.impl;

import java.util.Date;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.server.gateway.deserialization.TypeBasedJsonDeserializer;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;

public class GPSFixJsonDeserializer extends TypeBasedJsonDeserializer<GPSFix> {
    public static final String TYPE = "GPSFix";
    
    public static final String FIELD_LAT_DEG = "lat_deg";
    public static final String FIELD_LON_DEG = "lon_deg";
    public static final String FIELD_TIME = "unixtime";


    @Override
    protected GPSFix deserializeAfterCheckingType(JSONObject object) throws JsonDeserializationException {
        Date time = new Date((Long) object.get(FIELD_TIME));
        double latDeg = (Double) object.get(FIELD_LAT_DEG);
        double lonDeg = (Double) object.get(FIELD_LON_DEG);

        Position position = new DegreePosition(latDeg, lonDeg);
        TimePoint timePoint = new MillisecondsTimePoint(time);

        GPSFix fix = new GPSFixImpl(position, timePoint);

        return fix;
    }


    @Override
    protected String getType() {
        return TYPE;
    }

}