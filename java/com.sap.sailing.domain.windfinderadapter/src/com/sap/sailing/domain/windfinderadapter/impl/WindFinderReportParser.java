package com.sap.sailing.domain.windfinderadapter.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.windfinder.ReviewedSpotsCollection;
import com.sap.sailing.domain.windfinder.Spot;
import com.sap.sse.common.Position;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class WindFinderReportParser {
    /**
     * Example: 2017-11-13T15:32:00+01:00
     */
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");

    public WindFinderReportParser() {
        super();
    }
    
    /**
     * @param position The position of the station from which messages are to be parsed
     */
    Wind parse(Position position, JSONObject jsonOfSingleMeasurement) throws NumberFormatException, ParseException {
        final Wind result;
        if (jsonOfSingleMeasurement.get("dtl") != null &&
            jsonOfSingleMeasurement.get("ws") != null &&
            jsonOfSingleMeasurement.get("wd") != null) {
            final Date date;
            synchronized (dateFormat) {
                date = dateFormat.parse(jsonOfSingleMeasurement.get("dtl").toString());
            }
            result = new WindImpl(position, new MillisecondsTimePoint(date),
                new KnotSpeedWithBearingImpl(Double.parseDouble(jsonOfSingleMeasurement.get("ws").toString()),
                        new DegreeBearingImpl(Double.parseDouble(jsonOfSingleMeasurement.get("wd").toString())).reverse()));
        } else {
            result = null;
        }
        return result;
    }
    
    /**
     * @param position The position of the station from which messages are to be parsed
     */
    Iterable<Wind> parse(Position position, JSONArray jsonOfSeveralMeasurements) throws NumberFormatException, ParseException {
        final List<Wind> result = new ArrayList<>();
        for (final Object jsonOfSingleMeasurement : jsonOfSeveralMeasurements) {
            final Wind wind = parse(position, (JSONObject) jsonOfSingleMeasurement);
            if (wind != null) {
                result.add(wind);
            }
        }
        return result;
    }
    
    Spot parseSpot(JSONObject jsonOfSingleSpot, ReviewedSpotsCollection reviewedSpotsCollection) {
        return new SpotImpl(jsonOfSingleSpot.get("n").toString(),
                jsonOfSingleSpot.get("id").toString(),
                jsonOfSingleSpot.get("kw").toString(),
                jsonOfSingleSpot.get("c") == null ? null : jsonOfSingleSpot.get("c").toString(),
                new DegreePosition(((Number) jsonOfSingleSpot.get("lat")).doubleValue(),
                                ((Number) jsonOfSingleSpot.get("lon")).doubleValue()), this, reviewedSpotsCollection);
    }
    
    Iterable<Spot> parseSpots(JSONArray jsonOfMultipleSpots, ReviewedSpotsCollection reviewedSpotsCollection) {
        return Util.map(jsonOfMultipleSpots, jsonOfSingleSpot->parseSpot((JSONObject) jsonOfSingleSpot, reviewedSpotsCollection));
    }
}
