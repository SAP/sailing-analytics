package com.sap.sailing.server.gateway.serialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sse.shared.json.JsonSerializer;

public class TrackingConnectorInfoJsonSerializer implements JsonSerializer<TrackingConnectorInfo> {
    public static final String FIELD_TRACKING_CONNECTOR_NAME = "trackingConnectorName";
    public static final String FIELD_TRACKING_CONNECTOR_DEFAULT_URL = "trackingConnectorDefaultUrl";
    public static final String FIELD_WEB_URL = "webUrl";

    @Override
    public JSONObject serialize(TrackingConnectorInfo trackingConnectorInfo) {
        JSONObject result = new JSONObject();
        result.put(FIELD_TRACKING_CONNECTOR_NAME, trackingConnectorInfo.getTrackingConnectorName());
        result.put(FIELD_TRACKING_CONNECTOR_DEFAULT_URL, trackingConnectorInfo.getTrackingConnectorDefaultUrl());
        result.put(FIELD_WEB_URL, trackingConnectorInfo.getWebUrl());
        return result;
    }
}
