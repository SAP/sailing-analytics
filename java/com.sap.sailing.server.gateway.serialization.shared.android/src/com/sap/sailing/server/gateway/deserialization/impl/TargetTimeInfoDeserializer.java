package com.sap.sailing.server.gateway.deserialization.impl;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.TargetTimeInfo;
import com.sap.sailing.domain.common.TargetTimeInfo.LegTargetTimeInfo;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.TargetTimeInfoImpl;
import com.sap.sailing.server.gateway.serialization.impl.TargetTimeInfoSerializer;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MeterDistance;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class TargetTimeInfoDeserializer implements JsonDeserializer<TargetTimeInfo> {
    final JsonDeserializer<Wind> windDeserializer;
    
    public TargetTimeInfoDeserializer(JsonDeserializer<Wind> windDeserializer) {
        super();
        this.windDeserializer = windDeserializer;
    }

    @Override
    public TargetTimeInfo deserialize(JSONObject object) throws JsonDeserializationException {
        final List<LegTargetTimeInfo> legInfos = new ArrayList<>();
        final JSONArray legsAsJson = (JSONArray) object.get(TargetTimeInfoSerializer.LEGS);
        for (final Object legAsObject : legsAsJson) {
            final JSONObject legAsJson = (JSONObject) legAsObject;
            final Duration expectedDuration = new MillisecondsDurationImpl(((Number) legAsJson.get(TargetTimeInfoSerializer.LEG_DURATION_MILLIS)).longValue());
            final TimePoint expectedStartTime = new MillisecondsTimePoint(((Number) legAsJson.get(TargetTimeInfoSerializer.LEG_START_TIME_MILLIS)).longValue());
            final Bearing legBearing = new DegreeBearingImpl(((Number) legAsJson.get(TargetTimeInfoSerializer.LEG_BEARING_DEGREES)).doubleValue());
            final Distance legDistance = new MeterDistance(((Number) legAsJson.get(TargetTimeInfoSerializer.LEG_DISTANCE_METERS)).doubleValue());
            final LegType legType = LegType.valueOf((String) legAsJson.get(TargetTimeInfoSerializer.LEG_TYPE));
            final Wind legWind = windDeserializer.deserialize((JSONObject) legAsJson.get(TargetTimeInfoSerializer.LEG_WIND));
            final LegTargetTimeInfo legInfo = new TargetTimeInfoImpl.LegTargetTimeInfoImpl(legDistance, legWind,
                    legBearing, expectedDuration, expectedStartTime, legType, Distance.NULL);
            legInfos.add(legInfo);
        }
        return new TargetTimeInfoImpl(legInfos);
    }
}
