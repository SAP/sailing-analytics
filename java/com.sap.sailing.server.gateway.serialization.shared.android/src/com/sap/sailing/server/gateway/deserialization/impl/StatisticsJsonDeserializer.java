package com.sap.sailing.server.gateway.deserialization.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.statistics.Statistics;
import com.sap.sailing.domain.statistics.impl.StatisticsImpl;
import com.sap.sailing.server.gateway.serialization.impl.StatisticsJsonSerializer;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.MeterDistance;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;

public class StatisticsJsonDeserializer implements JsonDeserializer<Statistics> {
    
    private final CompetitorJsonDeserializer competitorJsonDeserializer;

    public static StatisticsJsonDeserializer create(SharedDomainFactory<?> baseDomainFactory) {
        return new StatisticsJsonDeserializer(CompetitorJsonDeserializer.create(baseDomainFactory));
    }
    
    private StatisticsJsonDeserializer(CompetitorJsonDeserializer competitorJsonDeserializer) {
        this.competitorJsonDeserializer = competitorJsonDeserializer;
    }

    @Override
    public Statistics deserialize(JSONObject object) throws JsonDeserializationException {
        int competitors = getIntValue(object, StatisticsJsonSerializer.FIELD_NUMBER_OF_COMPETITORS);
        int regattas = getIntValue(object, StatisticsJsonSerializer.FIELD_NUMBER_OF_REGATTAS);
        int races = getIntValue(object, StatisticsJsonSerializer.FIELD_NUMBER_OF_RACES);
        int trackedRaces = getIntValue(object, StatisticsJsonSerializer.FIELD_NUMBER_OF_TRACKED_RACES);
        long gpsFixes = getLongValue(object, StatisticsJsonSerializer.FIELD_NUMBER_OF_GPS_FIXES);
        long windFixes = getLongValue(object, StatisticsJsonSerializer.FIELD_NUMBER_OF_WIND_FIXES);
        Distance distance = new MeterDistance(getDoubleValue(object, StatisticsJsonSerializer.FIELD_DISTANCE_TRAVELED_IN_METERS));
        JSONObject maxSpeedObject = (JSONObject) object.get(StatisticsJsonSerializer.FIELD_MAX_SPEED);
        final Triple<Competitor, Speed, TimePoint> maxSpeed;
        if (maxSpeedObject != null) {
            final Competitor fastestCompetitor = competitorJsonDeserializer.deserialize((JSONObject) maxSpeedObject.get(StatisticsJsonSerializer.FIELD_FASTEST_COMPETITOR));
            final Speed fastestCompetitorSpeed = new KnotSpeedImpl(getDoubleValue(maxSpeedObject, StatisticsJsonSerializer.FIELD_FASTEST_COMPETITOR_SPEED_IN_KNOTS));
            final TimePoint timePoint = new MillisecondsTimePoint(getLongValue(maxSpeedObject, StatisticsJsonSerializer.FIELD_TIMEPOINT_MILLIS));
            maxSpeed = new Triple<Competitor, Speed, TimePoint>(fastestCompetitor, fastestCompetitorSpeed, timePoint);
        } else {
            maxSpeed = null;
        }
        return new StatisticsImpl(competitors, regattas, races, trackedRaces, gpsFixes, windFixes, distance, maxSpeed);
    }

    private int getIntValue(JSONObject object, String field) {
        return ((Number) object.get(field)).intValue();
    }
    
    private long getLongValue(JSONObject object, String field) {
        return ((Number) object.get(field)).longValue();
    }
    
    private double getDoubleValue(JSONObject object, String field) {
        return ((Number) object.get(field)).doubleValue();
    }
}
