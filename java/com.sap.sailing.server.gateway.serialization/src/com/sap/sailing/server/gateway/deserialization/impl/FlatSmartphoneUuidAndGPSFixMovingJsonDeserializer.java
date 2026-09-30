package com.sap.sailing.server.gateway.deserialization.impl;

import java.io.IOException;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.FlatSmartphoneUuidAndGPSFixMovingJsonSerializer;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MeterPerSecondSpeedImpl;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

/**
 * Make serialization on the smartphone easier by providing a flat structure rather than nested JSON documents.
 * 
 * @author Fredrik Teschke
 *
 */
public class FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer
        implements JsonDeserializer<Pair<UUID, List<GPSFixMoving>>> {
    private static final Logger logger = Logger.getLogger(FlatSmartphoneUuidAndGPSFixMovingJsonDeserializer.class.getName());
    
    public static final String ACCURACY = "accuracy";

    @Override
    public Pair<UUID, List<GPSFixMoving>> deserialize(JSONObject object) throws JsonDeserializationException {
        UUID device = UUID.fromString(object.get(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.DEVICE_UUID).toString());
        JSONArray jsonFixes = Helpers.getNestedArraySafe(object, FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.FIXES);
        List<GPSFixMoving> fixes = new ArrayList<GPSFixMoving>();
        for (int i = 0; i < jsonFixes.size(); i++) {
            JSONObject fixObject = Helpers.toJSONObjectSafe(jsonFixes.get(i));
            double lonDeg = Double.parseDouble(fixObject.get(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.LON_DEG).toString());
            double latDeg = Double.parseDouble(fixObject.get(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.LAT_DEG).toString());
            long timeMillis = deserializeTimestamp(fixObject);
            double speedMperS = Double.parseDouble(fixObject.get(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.SPEED_M_PER_S).toString());
            double speedKnots = new MeterPerSecondSpeedImpl(speedMperS).getKnots();
            double bearingDeg = Double.parseDouble(fixObject.get(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.BEARING_DEG).toString());
            Double optionalTrueHeadingDeg;
            if (fixObject.containsKey(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.TRUE_HEADING_DEG)) {
                optionalTrueHeadingDeg = Double.parseDouble(fixObject.get(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.TRUE_HEADING_DEG).toString());
            } else if (fixObject.containsKey(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.MAGNETIC_HEADING_DEG)) {
                final TimePoint timePoint = TimePoint.of(timeMillis);
                try {
                    optionalTrueHeadingDeg = new DegreeBearingImpl(Double.parseDouble(fixObject.get(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.MAGNETIC_HEADING_DEG).toString()))
                            .add(DeclinationService.INSTANCE.getDeclination(timePoint, new DegreePosition(latDeg, lonDeg), /* timeout in ms */ 1000)
                                    .getBearingCorrectedTo(timePoint)).getDegrees();
                } catch (NumberFormatException | IOException | ParseException e) {
                    logger.log(Level.WARNING, "Problem obtaining magnetic declination for heading provided in JSON fix", e);
                    optionalTrueHeadingDeg = null;
                }
            } else {
                optionalTrueHeadingDeg = null;
            }
            GPSFixMoving fix = GPSFixMovingImpl.create(lonDeg, latDeg, timeMillis, speedKnots, bearingDeg, optionalTrueHeadingDeg);
            fixes.add(fix);
        }

        return new Pair<UUID, List<GPSFixMoving>>(device, fixes);
    }

    private long deserializeTimestamp(JSONObject fixObject) throws JsonDeserializationException {
        long timeMillis;
        Object timeMillisObj = fixObject.get(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.TIME_MILLIS);
        Object timeIsoObj = fixObject.get(FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.TIME_ISO);
        if (timeMillisObj != null && timeIsoObj != null) {
            throw new JsonDeserializationException("two timestamp fields are filled. Please use only one of both.");
        }
        if (timeMillisObj != null) {
            timeMillis = Long.parseLong(timeMillisObj.toString());
        } else if (timeIsoObj != null) {
            String strIsoTimestamp = timeIsoObj.toString();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ISO_DATE_TIME;
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(strIsoTimestamp, timeFormatter);
            timeMillis = offsetDateTime.toInstant().toEpochMilli();
        } else {
            throw new JsonDeserializationException("no timestamp field provided. Please provide one of these fields: "
                    + FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.TIME_MILLIS + ", "
                    + FlatSmartphoneUuidAndGPSFixMovingJsonSerializer.TIME_ISO);
        }
        return timeMillis;
    }
}
