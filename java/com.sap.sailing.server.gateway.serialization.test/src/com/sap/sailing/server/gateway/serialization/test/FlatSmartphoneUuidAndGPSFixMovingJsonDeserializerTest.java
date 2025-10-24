package com.sap.sailing.server.gateway.serialization.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.server.gateway.deserialization.impl.FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class FlatSmartphoneUuidAndGPSFixMovingJsonDeserializerTest {
    @Test
    public void deserialize() throws JsonDeserializationException, ParseException {
        String json = "{\n" + 
                "  \"deviceUuid\" : \"af855a56-9726-4a9c-a77e-da955bd289bf\",\n" + 
                "  \"fixes\" : [\n" + 
                "    {\n" + 
                "      \"timestamp\" : 14144160080000,\n" + 
                "      \"latitude\" : 54.325246,\n" + 
                "      \"longitude\" : 10.148556,\n" + 
                "      \"speed\" : 3.61,\n" + 
                "      \"course\" : 258.11\n" + 
                "    },\n" + 
                "    {\n" + 
                "      \"timestamp\" : 14144168490000,\n" + 
                "      \"latitude\" : 55.12456,\n" + 
                "      \"longitude\" : 8.03456,\n" + 
                "      \"speed\" : 5.1,\n" + 
                "      \"course\" : 14.2\n" + 
                "    },\n" +
                "    {\n" + 
                "      \"timestamp-iso\" : \"2418-03-18T15:41:30Z\",\n" + 
                "      \"latitude\" : -67.672456,\n" + 
                "      \"longitude\" : -2.03456,\n" + 
                "      \"speed\" : 19.3,\n" + 
                "      \"course\" : 359.99,\n" + 
                "      \"hdt\" : 358.57\n" + 
                "    }\n" +
                "  ]\n" + 
                "}";
        JsonDeserializer<Pair<UUID, List<GPSFixMoving>>> deserializer = 
                new FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer();
        Pair<UUID, List<GPSFixMoving>> result = 
                deserializer.deserialize(Helpers.toJSONObjectSafe(JSONValue.parseWithException(json)));
        assertThat("uuid", result.getA(), equalTo(UUID.fromString("af855a56-9726-4a9c-a77e-da955bd289bf")));
        assertThat("number of fixes", result.getB().size(), equalTo(3));
        TimePoint tp3 = result.getB().get(2).getTimePoint();
        assertThat(tp3.asMillis(), equalTo(14144168490000L));
        assertThat(result.getB().get(2).getOptionalTrueHeading().getDegrees(), equalTo(358.57));
    }
    
    @Test
    public void containsBothTimestampFields() throws JsonDeserializationException, ParseException {
        String json = "{\n" + 
                "  \"deviceUuid\" : \"af855a56-9726-4a9c-a77e-da955bd289bf\",\n" + 
                "  \"fixes\" : [\n" + 
                "    {\n" + 
                "      \"timestamp-iso\" : \"2418-03-18T17:34:30Z\",\n" +
                "      \"timestamp\" : \"14144168490000\",\n" +
                "      \"latitude\" : -67.672456,\n" + 
                "      \"longitude\" : -2.03456,\n" + 
                "      \"speed\" : 19.3,\n" + 
                "      \"course\" : 359.99,\n" + 
                "    }\n" +
                "  ]\n" + 
                "}";
        JsonDeserializer<Pair<UUID, List<GPSFixMoving>>> deserializer = 
                new FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer();
        try {
                deserializer.deserialize(Helpers.toJSONObjectSafe(JSONValue.parseWithException(json)));
                Assertions.fail();
        } catch (JsonDeserializationException e) {
            assertThat(e.getMessage(),equalTo("two timestamp fields are filled. Please use only one of both."));
        }
    }
    
    @Test
    public void wrongFormat_millis() throws JsonDeserializationException, ParseException {
        String json = "{\n" + 
                "  \"deviceUuid\" : \"af855a56-9726-4a9c-a77e-da955bd289bf\",\n" + 
                "  \"fixes\" : [\n" + 
                "    {\n" + 
                "      \"timestamp\" : \"2418-03-18T17:34:30Z\",\n" +
                "      \"latitude\" : -67.672456,\n" + 
                "      \"longitude\" : -2.03456,\n" + 
                "      \"speed\" : 19.3,\n" + 
                "      \"course\" : 359.99,\n" + 
                "    }\n" +
                "  ]\n" + 
                "}";
        JsonDeserializer<Pair<UUID, List<GPSFixMoving>>> deserializer = 
                new FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer();
        try {
                deserializer.deserialize(Helpers.toJSONObjectSafe(JSONValue.parseWithException(json)));
                Assertions.fail();
        } catch (NumberFormatException e) {
            assertThat(e.getMessage(),equalTo("For input string: \"2418-03-18T17:34:30Z\""));
        }
    }
    
    @Test
    public void wrongFormat_ISO() throws JsonDeserializationException, ParseException {
        String json = "{\n" + 
                "  \"deviceUuid\" : \"af855a56-9726-4a9c-a77e-da955bd289bf\",\n" + 
                "  \"fixes\" : [\n" + 
                "    {\n" + 
                "      \"timestamp-iso\" : \"14144168490000\",\n" +
                "      \"latitude\" : -67.672456,\n" + 
                "      \"longitude\" : -2.03456,\n" + 
                "      \"speed\" : 19.3,\n" + 
                "      \"course\" : 359.99,\n" + 
                "    }\n" +
                "  ]\n" + 
                "}";
        JsonDeserializer<Pair<UUID, List<GPSFixMoving>>> deserializer = 
                new FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer();
        try {
                deserializer.deserialize(Helpers.toJSONObjectSafe(JSONValue.parseWithException(json)));
                Assertions.fail();
        } catch (DateTimeParseException e) {
            assertThat(e.getMessage(),equalTo("Text '14144168490000' could not be parsed at index 0"));
        }
    }
}
