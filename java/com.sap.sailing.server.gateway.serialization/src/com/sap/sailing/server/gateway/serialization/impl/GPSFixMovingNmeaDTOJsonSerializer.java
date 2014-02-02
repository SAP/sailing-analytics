package com.sap.sailing.server.gateway.serialization.impl;

import net.sf.marineapi.nmea.parser.RMCParser;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.server.gateway.deserialization.impl.GPSFixNmeaDTOJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;

public class GPSFixMovingNmeaDTOJsonSerializer implements JsonSerializer<GPSFixMoving> {

    @Override
    public JSONObject serialize(GPSFixMoving object) {
        JSONObject result = new JSONObject();

        RMCParser parser = GPSFixNmeaDTOJsonSerializer.getParser(object);

        double bearing = object.getSpeed().getBearing().getDegrees();
        double speed = object.getSpeed().getKnots();

        parser.setCourse(bearing);
        parser.setSpeed(speed);

        String nmea = parser.toSentence();

        result.put(GPSFixNmeaDTOJsonDeserializer.FIELD_NMEA, nmea);
        result.put(GPSFixNmeaDTOJsonDeserializer.FIELD_TIME, object.getTimePoint().asMillis());

        return result;
    }

}
