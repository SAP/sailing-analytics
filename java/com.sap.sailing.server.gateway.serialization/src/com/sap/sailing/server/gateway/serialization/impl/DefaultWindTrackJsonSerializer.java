package com.sap.sailing.server.gateway.serialization.impl;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.util.RoundingUtil;

public class DefaultWindTrackJsonSerializer implements WindTrackJsonSerializer {
    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_PUBLICATION_URL = "publicationUrl";
    public static final String FIELD_VENUE = "venue";

    private final TimePoint fromTime;
    private final TimePoint toTime;
    private final WindSource windSource;
    
    /**
     * -1 means unlimited.
     */
    private final int maxNumberOfFixes;
    
    public DefaultWindTrackJsonSerializer(int maxNumberOfFixes, TimePoint fromTime, TimePoint toTime, WindSource windSource) {
        super();
        this.maxNumberOfFixes = maxNumberOfFixes;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.windSource = windSource;
    }
    
    public JSONObject serialize(final WindTrack windTrack) {
        JSONObject result = new JSONObject();
        JSONArray jsonWindFixes = new JSONArray();
        // quickly extract relevant fixes to hold locks as shortly as possible; process later
        ArrayList<Wind> fixes = new ArrayList<>();
        windTrack.lockForRead();
        try {
            Iterator<Wind> windIter = windTrack.getFixesIterator(fromTime, /* inclusive */true, toTime, /* inclusive */ false);
            int count = 0;
            while ((maxNumberOfFixes == -1 || count++<maxNumberOfFixes) && windIter.hasNext()) {
                fixes.add(windIter.next());
            }
        } finally {
            windTrack.unlockAfterRead();
        }
        for (Wind wind : fixes) {
            JSONObject jsonWind = new JSONObject();
            jsonWind.put("trueBearing-deg", RoundingUtil.bearingDecimalFormatter.format(wind.getBearing().getDegrees()));
            jsonWind.put("speed-kts", RoundingUtil.speedDecimalFormatter.format(wind.getKnots()));
            jsonWind.put("speed-m/s", RoundingUtil.speedDecimalFormatter.format(wind.getMetersPerSecond()));
            if (wind.getTimePoint() != null) {
                jsonWind.put("timepoint-ms", wind.getTimePoint().asMillis());
                final Wind averagedWind = windTrack.getAveragedWind(wind.getPosition(), wind.getTimePoint());
                jsonWind.put("dampenedTrueBearing-deg", RoundingUtil.bearingDecimalFormatter.format(averagedWind.getBearing().getDegrees()));
                jsonWind.put("dampenedSpeed-kts", RoundingUtil.speedDecimalFormatter.format(averagedWind.getKnots()));
                jsonWind.put("dampenedSpeed-m/s", RoundingUtil.speedDecimalFormatter.format(averagedWind.getMetersPerSecond()));
            }
            if (wind.getPosition() != null) {
                jsonWind.put("lat-deg", RoundingUtil.latLngDecimalFormatter.format(wind.getPosition().getLatDeg()));
                jsonWind.put("lng-deg", RoundingUtil.latLngDecimalFormatter.format(wind.getPosition().getLngDeg()));
            }
            jsonWindFixes.add(jsonWind);
        }
        result.put(windSource.getType() + (windSource.getId() != null ? "-"+windSource.getId().toString() : ""), jsonWindFixes);
        return result;
    }
}