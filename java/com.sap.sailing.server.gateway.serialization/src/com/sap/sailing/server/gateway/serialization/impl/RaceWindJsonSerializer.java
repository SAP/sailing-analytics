package com.sap.sailing.server.gateway.serialization.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class RaceWindJsonSerializer extends AbstractTrackedRaceDataJsonSerializer {
    public static final String WIND_SOURCES = "windSources";
    public static final String TYPE = "type";
    public static final String FIRST_POSITION = "firstPosition";
    public static final String START_TIME_POINT = "startUnixTime";
    public static final String END_TIME_POINT = "endUnixTime";
    public static final String SAMPLING_RATE = "samplingRate";
    public static final String FIXES = "fixes";

    private final MongoDbFriendlyPositionJsonSerializer positionSerializer = new MongoDbFriendlyPositionJsonSerializer();
    private final WindJsonSerializer windSerializer = new WindJsonSerializer(positionSerializer);

    @Override
    public JSONObject serialize(TrackedRace trackedRace) {
        final TimePoint finalFrom = trackedRace.getStartOfRace();
        final TimePoint finalTo = Util.getEarliestOfTimePoints(trackedRace.getEndOfTracking(), trackedRace.getEndOfRace());
        final Set<WindSource> windSourcesToExclude = trackedRace.getWindSourcesToExclude();
        List<WindSource> highQualityWindSources = trackedRace.getWindSources().stream()
                .filter(windSource -> windSource.getType() == WindSourceType.EXPEDITION
                        && !windSourcesToExclude.contains(windSource))
                .collect(Collectors.toList());
        JSONArray windSourcesJson = new JSONArray();
        TimePoint earliestTimePoint = null;
        TimePoint latestTimePoint = null;
        Position firstPosition = null;
        double bestSamplingRate = Double.MAX_VALUE;
        for (WindSource windSource : highQualityWindSources) {
            final WindTrack windTrack = trackedRace.getOrCreateWindTrack(windSource);
            windTrack.lockForRead();
            try {
                Iterator<Wind> iterator = windTrack.getFixes(finalFrom, true, finalTo, true).iterator();
                if (iterator.hasNext()) {
                    Wind fix = iterator.next();
                    if (fix.getPosition() != null) {
                        JSONArray windFixesJson = new JSONArray();
                        TimePoint firstWindSourceTimePoint = fix.getTimePoint();
                        TimePoint lastWindSourceTimePoint = null;
                        Position firstWindSourcePosition = fix.getPosition();
                        if (firstPosition == null) {
                            firstPosition = firstWindSourcePosition;
                        }
                        if (earliestTimePoint == null || earliestTimePoint.after(fix.getTimePoint())) {
                            earliestTimePoint = fix.getTimePoint();
                        }
                        do {
                            JSONObject windJson = windSerializer.serialize(fix);
                            windFixesJson.add(windJson);
                            if (iterator.hasNext()) {
                                fix = iterator.next();
                            } else {
                                lastWindSourceTimePoint = fix.getTimePoint();
                                if (latestTimePoint == null || latestTimePoint.before(fix.getTimePoint())) {
                                    latestTimePoint = fix.getTimePoint();
                                }
                                fix = null;
                            }
                        } while (fix != null);
                        Duration duration = firstWindSourceTimePoint.until(lastWindSourceTimePoint);
                        JSONObject windSourceJson = new JSONObject();
                        windSourceJson.put(TYPE, windSource.getType().name());
                        windSourceJson.put(FIRST_POSITION, positionSerializer.serialize(firstWindSourcePosition));
                        windSourceJson.put(START_TIME_POINT, firstWindSourceTimePoint.asMillis());
                        windSourceJson.put(END_TIME_POINT, lastWindSourceTimePoint.asMillis());
                        double samplingRate = duration.asSeconds() > 0 ? windFixesJson.size() / duration.asSeconds()
                                : 0;
                        windSourceJson.put(SAMPLING_RATE, samplingRate);
                        windSourceJson.put(FIXES, windFixesJson);
                        windSourcesJson.add(windSourceJson);
                        if (bestSamplingRate > samplingRate) {
                            bestSamplingRate = samplingRate;
                        }
                    }
                }
            } finally {
                windTrack.unlockAfterRead();
            }
        }
        final JSONObject result = new JSONObject();
        if (!windSourcesJson.isEmpty()) {
            result.put(FIRST_POSITION, positionSerializer.serialize(firstPosition));
            result.put(START_TIME_POINT, earliestTimePoint.asMillis());
            result.put(END_TIME_POINT, latestTimePoint.asMillis());
            result.put(SAMPLING_RATE, bestSamplingRate);
            result.put(WIND_SOURCES, windSourcesJson);
        }
        return result;
    }
}
