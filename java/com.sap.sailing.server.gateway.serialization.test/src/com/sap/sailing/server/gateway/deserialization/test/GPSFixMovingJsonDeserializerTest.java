package com.sap.sailing.server.gateway.deserialization.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.server.gateway.deserialization.impl.GPSFixMovingJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.GPSFixMovingJsonSerializer;
import com.sap.sse.shared.json.JsonDeserializationException;

public class GPSFixMovingJsonDeserializerTest {
    private GPSFixMovingJsonSerializer serializer;
    private GPSFixMovingJsonDeserializer deserializer;

    @BeforeEach
    public void setup() {
        serializer = new GPSFixMovingJsonSerializer();
        deserializer = new GPSFixMovingJsonDeserializer();
    }

    @Test
    public void testGPSFixMovingDeserializer() throws ParseException, JsonDeserializationException {
        GPSFixMoving gpsFix = GPSFixMovingImpl.create(8.0, 55.0, System.currentTimeMillis(), 5.0, 14.0, /* optionalTrueHeadingDeg */ 15.0);
        JSONObject gpsFixSerialized = serializer.serialize(gpsFix);
        GPSFixMoving gpsFixDeserialized = deserializer.deserialize(gpsFixSerialized);
        assertEquals(gpsFix, gpsFixDeserialized);
    }
}
