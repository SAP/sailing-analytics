package com.sap.sailing.domain.oceanraceadapter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifierImpl;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.domain.trackimport.GPSFixImporter;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

public class OceanRaceGPSFixImporter implements GPSFixImporter {
    private final static String BEACONS_OBJECT_NAME = "beacons";
    private final static String DATA_ARRAY_NAME = "data";
    private final static String BOAT_NAME_FIELD_NAME = "boat_name";
    private final static String TIMESTAMP_FIELD_NAME = "gpsAtMillis";
    private final static String LATITUDE_FIELD_NAME = "latitude";
    private final static String LONGITUDE_FIELD_NAME = "longitude";
    private final static String SOG_KNOTS_FIELD_NAME = "sogKnots";
    private final static String COG_DEGREES_FIELD_NAME = "cog";
    private final static String SERIAL_FIELD_NAME = "serial";
    private final static String DEVICE_TYPE_FIELD_NAME = "device_type";
    private final static String DEVICE_LOCATION_FIELD_NAME = "device_location";
    
    private final Map<Pair<String, String>, TrackFileImportDeviceIdentifier> deviceIdentifiersBySerialAndSourceName = new HashMap<>();
    
    @Override
    public boolean importFixes(InputStream inputStream, Charset charset, Callback callback,
            boolean inferSpeedAndBearing, String sourceName) throws FormatNotSupportedException, IOException, ParseException {
        final JSONParser jsonParser = new JSONParser();
        final JSONArray boatsArray = (JSONArray) jsonParser.parse(new InputStreamReader(inputStream));
        for (final Object objectForBoat : boatsArray) {
            final JSONObject jsonObjectForBoat = (JSONObject) objectForBoat;
            final String boatName = jsonObjectForBoat.get(BOAT_NAME_FIELD_NAME).toString();
            final JSONArray boatFixes = (JSONArray) ((JSONObject) jsonObjectForBoat.get(BEACONS_OBJECT_NAME)).get(DATA_ARRAY_NAME);
            GPSFixMoving lastFix = null;
            for (final Object boatFixObject : boatFixes) {
                final JSONObject boatFixJsonObject = (JSONObject) boatFixObject;
                final GPSFixMoving nextFix = parseFix(boatFixJsonObject, lastFix, inferSpeedAndBearing);
                final TrackFileImportDeviceIdentifier deviceIdentifier = getDeviceIdentifier(sourceName, boatName, boatFixJsonObject);
                callback.addFix(nextFix, deviceIdentifier);
                lastFix = nextFix;
            }
        }
        return true;
    }

    private TrackFileImportDeviceIdentifier getDeviceIdentifier(String sourceName, String boatName, JSONObject boatFixJsonObject) {
        final String serial = String.format("%s-%s-%s-%s", boatName,
                boatFixJsonObject.get(SERIAL_FIELD_NAME).toString(),
                boatFixJsonObject.get(DEVICE_TYPE_FIELD_NAME).toString(),
                boatFixJsonObject.get(DEVICE_LOCATION_FIELD_NAME).toString());
        final Pair<String, String> key = new Pair<>(serial, sourceName);
        return deviceIdentifiersBySerialAndSourceName.computeIfAbsent(key, k->new TrackFileImportDeviceIdentifierImpl(UUID.randomUUID(), k.getB(), key.getA(), TimePoint.now()));
    }

    private GPSFixMoving parseFix(JSONObject boatFixJsonObject, GPSFixMoving lastFix, boolean inferSpeedAndBearing) {
        final TimePoint timePoint = TimePoint.of(((Number) boatFixJsonObject.get(TIMESTAMP_FIELD_NAME)).longValue());
        final double latDeg = boatFixJsonObject.get(LATITUDE_FIELD_NAME) == null ? 0.0 : Double.valueOf((String) boatFixJsonObject.get(LATITUDE_FIELD_NAME));
        final double lngDeg = boatFixJsonObject.get(LONGITUDE_FIELD_NAME) == null ? 0.0 : Double.valueOf((String) boatFixJsonObject.get(LONGITUDE_FIELD_NAME));
        final Position position = new DegreePosition(latDeg, lngDeg);
        final String cogAsString = (String) boatFixJsonObject.get(COG_DEGREES_FIELD_NAME);
        final String sogAsString = (String) boatFixJsonObject.get(SOG_KNOTS_FIELD_NAME);
        final Bearing cog;
        if (cogAsString != null) {
            cog = new DegreeBearingImpl(Double.valueOf(cogAsString));
        } else if (inferSpeedAndBearing && lastFix != null && lastFix.getPosition() != null) {
            cog = lastFix.getTimePoint().before(timePoint) ? lastFix.getPosition().getBearingGreatCircle(position)
                    : position.getBearingGreatCircle(lastFix.getPosition());
        } else {
            cog = new DegreeBearingImpl(0.0);
        }
        final double sogInKnots;
        if (sogAsString != null) {
            sogInKnots = Double.valueOf(sogAsString);
        } else if (inferSpeedAndBearing && lastFix != null && lastFix.getPosition() != null) {
            sogInKnots = lastFix.getTimePoint().before(timePoint)
                    ? lastFix.getSpeedAndBearingRequiredToReach(new GPSFixImpl(position, timePoint)).getKnots()
                    : new GPSFixImpl(position, timePoint).getSpeedAndBearingRequiredToReach(lastFix).getKnots();
        } else {
            sogInKnots = 0.0;
        }
        return new GPSFixMovingImpl(position, timePoint, new KnotSpeedWithBearingImpl(sogInKnots, cog), /* optionalTrueHeading */ null);
    }

    @Override
    public Iterable<String> getSupportedFileExtensions() {
        return Collections.singleton("json");
    }

    @Override
    public String getType() {
        return "The Ocean Race GPS Fix Importer";
    }

}
