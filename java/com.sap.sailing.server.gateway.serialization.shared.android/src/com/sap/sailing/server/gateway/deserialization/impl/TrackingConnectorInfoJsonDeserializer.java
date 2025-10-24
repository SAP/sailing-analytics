package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sailing.domain.shared.tracking.impl.TrackingConnectorInfoImpl;
import com.sap.sailing.server.gateway.serialization.impl.TrackingConnectorInfoJsonSerializer;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class TrackingConnectorInfoJsonDeserializer implements JsonDeserializer<TrackingConnectorInfo> {

    public TrackingConnectorInfo deserialize(JSONObject object) throws JsonDeserializationException {
        final String connectorName = (String) object.get(TrackingConnectorInfoJsonSerializer.FIELD_TRACKING_CONNECTOR_NAME);
        final String defaultUrlString = (String) object.get(TrackingConnectorInfoJsonSerializer.FIELD_TRACKING_CONNECTOR_DEFAULT_URL);
        final String webUrlString = (String) object.get(TrackingConnectorInfoJsonSerializer.FIELD_WEB_URL);
        final TrackingConnectorInfo trackingConnectorInfo = new TrackingConnectorInfoImpl(connectorName, defaultUrlString, webUrlString);
        return trackingConnectorInfo;
    }
}
