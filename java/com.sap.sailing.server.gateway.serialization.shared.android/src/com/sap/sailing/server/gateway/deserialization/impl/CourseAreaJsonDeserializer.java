package com.sap.sailing.server.gateway.deserialization.impl;

import java.io.Serializable;
import java.util.UUID;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.server.gateway.serialization.impl.CourseAreaJsonSerializer;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.MeterDistance;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;
import com.sap.sse.shared.util.impl.UUIDHelper;

public class CourseAreaJsonDeserializer implements JsonDeserializer<CourseArea> {
    private SharedDomainFactory<?> factory;

    public CourseAreaJsonDeserializer(SharedDomainFactory<?> factory) {
        this.factory = factory;
    }

    public CourseArea deserialize(JSONObject object)
            throws JsonDeserializationException {
        String name = (String) object.get(CourseAreaJsonSerializer.FIELD_NAME);
        Serializable id = (Serializable) object.get(CourseAreaJsonSerializer.FIELD_ID);
        final Position centerPosition;
        final Distance radius;
        final JSONObject centerPositionJson = (JSONObject) object.get(CourseAreaJsonSerializer.FIELD_CENTER_POSITION);
        if (centerPositionJson != null) {
            centerPosition = new PositionJsonDeserializer().deserialize(centerPositionJson);
        } else {
            centerPosition = null;
        }
        final Number radiusNumber = (Number) object.get(CourseAreaJsonSerializer.FIELD_RADIUS_IN_METERS);
        if (radiusNumber != null) {
            radius = new MeterDistance(radiusNumber.doubleValue());
        } else {
            radius = null;
        }
        final CourseArea result = factory.getOrCreateCourseArea((UUID) UUIDHelper.tryUuidConversion(id), name, centerPosition, radius);
        return result;
    }
}
