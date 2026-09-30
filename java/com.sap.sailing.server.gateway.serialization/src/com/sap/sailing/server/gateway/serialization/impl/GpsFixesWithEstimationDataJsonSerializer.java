package com.sap.sailing.server.gateway.serialization.impl;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.maneuverdetection.TrackTimeInfo;
import com.sap.sailing.domain.maneuverdetection.impl.ManeuverDetectorImpl;
import com.sap.sailing.domain.maneuverdetection.impl.ManeuverDetectorWithEstimationDataSupportDecoratorImpl;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;


/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */

public class GpsFixesWithEstimationDataJsonSerializer implements CompetitorTrackElementsJsonSerializer {
    public static final String GPS_FIXES = "gpsFixes";
    public static final String BOAT_CLASS = "boatClass";
    public static final String COMPETITOR_NAME = "competitorName";
    public static final String AVG_INTERVAL_BETWEEN_FIXES_IN_SECONDS = "avgIntervalBetweenFixesInSeconds";
    public static final String DISTANCE_TRAVELLED_IN_METERS = "distanceTravelledInMeters";
    public static final String START_TIME_POINT = "startUnixTime";
    public static final String END_TIME_POINT = "endUnixTime";
    public static final String WIND = "wind";
    public static final String RELATIVE_BEARING_TO_NEXT_MARK = "relativeBearingToNextMark";
    public static final String CLOSEST_DISTANCE_TO_MARK = "closestDistanceToMarkInMeters";

    private final GPSFixMovingJsonSerializer gpsFixMovingJsonSerializer;
    private final boolean addWind;
    private final boolean addNextWaypoint;
    private final ManeuverWindJsonSerializer windJsonSerializer;
    private final Boolean smoothFixes;

    public GpsFixesWithEstimationDataJsonSerializer(GPSFixMovingJsonSerializer gpsFixMovingJsonSerializer,
            ManeuverWindJsonSerializer windJsonSerializer, boolean addWind, boolean addNextWaypoint,
            Boolean smoothFixes) {
        this.gpsFixMovingJsonSerializer = gpsFixMovingJsonSerializer;
        this.windJsonSerializer = windJsonSerializer;
        this.addWind = addWind;
        this.addNextWaypoint = addNextWaypoint;
        this.smoothFixes = smoothFixes;
    }

    @Override
    public JSONArray serialize(TrackedRace trackedRace, Competitor competitor, TimePoint from, TimePoint to,
            TrackTimeInfo trackTimeInfo) {
        final JSONArray gpsFixesWithEstimationData = new JSONArray();
        ManeuverDetectorImpl maneuverDetector = new ManeuverDetectorImpl(trackedRace, competitor);
        ManeuverDetectorWithEstimationDataSupportDecoratorImpl estimationDataSupportDecoratorImpl = new ManeuverDetectorWithEstimationDataSupportDecoratorImpl(
                maneuverDetector, null);
        final GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
        track.lockForRead();
        try {
            for (GPSFixMoving gpsFix : track.getFixes(from, true, to, true)) {
                SpeedWithBearing speedWithBearing = smoothFixes ? track.getEstimatedSpeed(gpsFix.getTimePoint())
                        : gpsFix.getSpeed();
                JSONObject serializedGpsFix = gpsFixMovingJsonSerializer.serialize(gpsFix, speedWithBearing);
                if (addWind) {
                    Wind wind = trackedRace.getWind(gpsFix.getPosition(), gpsFix.getTimePoint());
                    JSONObject serializedWind = wind == null ? null : windJsonSerializer.serialize(wind);
                    serializedGpsFix.put(WIND, serializedWind);
                }
                if (addNextWaypoint) {
                    Distance closestDistanceToMark = estimationDataSupportDecoratorImpl
                            .getClosestDistanceToMark(gpsFix.getTimePoint());
                    Bearing relativeBearingToNextMark = speedWithBearing == null ? null
                            : estimationDataSupportDecoratorImpl.getRelativeBearingToNextMark(gpsFix.getTimePoint(),
                                    speedWithBearing.getBearing());
                    serializedGpsFix.put(CLOSEST_DISTANCE_TO_MARK,
                            closestDistanceToMark == null ? null : closestDistanceToMark.getMeters());
                    serializedGpsFix.put(RELATIVE_BEARING_TO_NEXT_MARK,
                            relativeBearingToNextMark == null ? null : relativeBearingToNextMark.getDegrees());
                }
                gpsFixesWithEstimationData.add(serializedGpsFix);
            }
        } finally {
            track.unlockAfterRead();
        }
        return gpsFixesWithEstimationData;
    }

}
