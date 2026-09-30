package com.sap.sailing.selenium.api.coursetemplate;

import org.json.simple.JSONObject;

import com.sap.sailing.selenium.api.core.JsonWrapper;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class DeviceMapping extends JsonWrapper {
    public static final String FIELD_DEVICE_TYPE = "trackingDeviceType";
    public static final String FIELD_TRACKING_DEVICE_HASH = "trackingDeviceHash";
    public static final String FIELD_MAPPED_FROM = "trackingDeviceMappedFromMillis";
    public static final String FIELD_MAPPED_TO = "trackingDeviceMappedToMillis";
    public static final String FIELD_TRACKING_DEVICE_LAST_KNOWN_POSITION = "trackingDeviceLastKnownPosition";
    private static final String FIELD_LATITUDE_DEG = "lat_deg";
    private static final String FIELD_LONGITUDE_DEG = "lon_deg";

    public DeviceMapping(JSONObject json) {
        super(json);
    }

    public String getHash() {
        return get(FIELD_TRACKING_DEVICE_HASH);
    }

    public String getType() {
        return get(FIELD_DEVICE_TYPE);
    }

    public TimePoint getMappedFrom() {
        final Number mappedFromMillis = get(FIELD_MAPPED_FROM);
        return mappedFromMillis == null ? null : new MillisecondsTimePoint(mappedFromMillis.longValue());
    }

    public TimePoint getMappedTo() {
        final Number mappedToMillis = get(FIELD_MAPPED_TO);
        return mappedToMillis == null ? null : new MillisecondsTimePoint(mappedToMillis.longValue());
    }

    public Position getLastKnownPosition() {
        final JSONObject positionJson = (JSONObject) get(FIELD_TRACKING_DEVICE_LAST_KNOWN_POSITION);
        return positionJson != null
                ? new DegreePosition(((Number) positionJson.get(FIELD_LATITUDE_DEG)).doubleValue(),
                        ((Number) positionJson.get(FIELD_LONGITUDE_DEG)).doubleValue())
                : null;
    }
}
