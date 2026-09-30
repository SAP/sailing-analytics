package com.sap.sailing.windestimation.data.serialization;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.server.gateway.deserialization.impl.BoatClassJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorTrackWithEstimationDataJsonSerializer;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.MeterDistance;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class CompetitorTrackWithEstimationDataJsonDeserializer<T> {

    private final JSONParser jsonParser = new JSONParser();
    private final BoatClassJsonDeserializer boatClassJsonDeserializer;
    private final JsonDeserializer<T> competitorTrackElementsJsonDeserializer;

    public CompetitorTrackWithEstimationDataJsonDeserializer(BoatClassJsonDeserializer boatClassJsonDeserializer,
            JsonDeserializer<T> competitorTrackElementsJsonDeserializer) {
        this.boatClassJsonDeserializer = boatClassJsonDeserializer;
        this.competitorTrackElementsJsonDeserializer = competitorTrackElementsJsonDeserializer;
    }

    public CompetitorTrackWithEstimationData<T> deserialize(JSONObject jsonObject, String regattaName, String raceName)
            throws JsonDeserializationException {
        String competitorName = (String) jsonObject
                .get(CompetitorTrackWithEstimationDataJsonSerializer.COMPETITOR_NAME);
        Double avgIntervalBetweenFixesInSeconds = (Double) jsonObject
                .get(CompetitorTrackWithEstimationDataJsonSerializer.AVG_INTERVAL_BETWEEN_FIXES_IN_SECONDS);
        Object boatClassObj = jsonObject.get(CompetitorTrackWithEstimationDataJsonSerializer.BOAT_CLASS);
        BoatClass boatClass;
        try {
            boatClass = boatClassJsonDeserializer.deserialize(getJSONObject(boatClassObj.toString()));
        } catch (ParseException e) {
            throw new JsonDeserializationException(e);
        }
        Double distanceTravelledInMeters = (Double) jsonObject
                .get(CompetitorTrackWithEstimationDataJsonSerializer.DISTANCE_TRAVELLED_IN_METERS);
        Long startUnixTime = (Long) jsonObject.get(CompetitorTrackWithEstimationDataJsonSerializer.START_TIME_POINT);
        Long endUnixTime = (Long) jsonObject.get(CompetitorTrackWithEstimationDataJsonSerializer.END_TIME_POINT);
        Long markPassingsCount = (Long) jsonObject
                .get(CompetitorTrackWithEstimationDataJsonSerializer.MARK_PASSINGS_COUNT);
        Long waypointsCount = (Long) jsonObject.get(CompetitorTrackWithEstimationDataJsonSerializer.WAYPOINTS_COUNT);

        JSONArray elementsJson = (JSONArray) jsonObject.get(CompetitorTrackWithEstimationDataJsonSerializer.ELEMENTS);
        List<T> completeManeuverCurves = new ArrayList<>(elementsJson.size());
        for (Object maneuverCurveObj : elementsJson) {
            T elements;
            try {
                elements = competitorTrackElementsJsonDeserializer
                        .deserialize(getJSONObject(maneuverCurveObj.toString()));
            } catch (ParseException e) {
                throw new JsonDeserializationException(e);
            }
            completeManeuverCurves.add(elements);
        }
        CompetitorTrackWithEstimationData<T> competitorTrackWithEstimationData = new CompetitorTrackWithEstimationData<>(
                regattaName, raceName, competitorName, boatClass, completeManeuverCurves,
                avgIntervalBetweenFixesInSeconds,
                distanceTravelledInMeters == null ? Distance.NULL : new MeterDistance(distanceTravelledInMeters),
                startUnixTime == null ? null : new MillisecondsTimePoint(startUnixTime),
                endUnixTime == null ? null : new MillisecondsTimePoint(endUnixTime), markPassingsCount.intValue(),
                waypointsCount.intValue());
        return competitorTrackWithEstimationData;
    }

    private JSONObject getJSONObject(String json) throws ParseException {
        return (JSONObject) jsonParser.parse(json);
    }

}
