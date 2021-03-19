package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.impl.FlatSmartphoneUuidAndGPSFixMovingJsonSerializer;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class FlatGPSFixJsonDeserializer implements
        JsonDeserializer<GPSFix> {

    @Override
    public GPSFix deserialize(JSONObject object) throws JsonDeserializationException {
        try {
            double lonDeg = Double.parseDouble(object.get(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.LON_DEG).toString());
            double latDeg = Double.parseDouble(object.get(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.LAT_DEG).toString());
            long timeMillis = Long.parseLong(object.get(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.TIME_MILLIS).toString());
            return GPSFixImpl.create(lonDeg, latDeg, timeMillis);
        } catch (NumberFormatException e){
            throw new JsonDeserializationException(e);
        }
    }
}
