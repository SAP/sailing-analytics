package com.sap.sailing.domain.maneuverdetection.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.maneuverdetection.ApproximatedFixesCalculator;
import com.sap.sailing.domain.maneuverdetection.ManeuverDetector;
import com.sap.sailing.domain.maneuverdetection.TrackTimeInfo;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.ManeuverCurveBoundariesImpl;
import com.sap.sailing.domain.tracking.impl.ManeuverWithCoarseGrainedBoundariesImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.CourseChange;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

/**
 * Maneuver detector implementation for GPS tracks with extremely low sampling rate such as 1 fix per 30 seconds.
 * 
 * FIXME The LowGPSSamplingRateManeuverDetectorImpl doesn't work very well; it recognizes many tacks only as bear-away and doesn't seem to have any noticeable benefits... See ORC Worlds 2019 ORC A Long Offshore
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class LowGPSSamplingRateManeuverDetectorImpl extends AbstractManeuverDetectorImpl implements ManeuverDetector {

    public LowGPSSamplingRateManeuverDetectorImpl(TrackedRace trackedRace, Competitor competitor) {
        super(trackedRace, competitor);
    }

    @Override
    public List<Maneuver> detectManeuvers() {
        List<Maneuver> result = new ArrayList<>();
        TrackTimeInfo startAndEndTimePoints = getTrackTimeInfo();
        if (startAndEndTimePoints != null) {
            ApproximatedFixesCalculator approximatedFixesCalculator = new ApproximatedFixesCalculatorImpl(trackedRace,
                    competitor);
            Iterable<GPSFixMoving> approximatedFixes = approximatedFixesCalculator.approximate(
                    startAndEndTimePoints.getTrackStartTimePoint(), startAndEndTimePoints.getTrackEndTimePoint());
            if (Util.size(approximatedFixes) > 2) {
                Iterator<GPSFixMoving> approximationPointsIter = approximatedFixes.iterator();
                GPSFixMoving previous = approximationPointsIter.next();
                GPSFixMoving current = approximationPointsIter.next();
                // the bearings in these variables are between approximation points
                do {
                    GPSFixMoving next = approximationPointsIter.next();
                    SpeedWithBearing speedWithBearingOnApproximationFromPreviousToCurrent = previous
                            .getSpeedAndBearingRequiredToReach(current);
                    SpeedWithBearing speedWithBearingOnApproximationFromCurrentToNext = current
                            .getSpeedAndBearingRequiredToReach(next);
                    CourseChange courseChange = speedWithBearingOnApproximationFromPreviousToCurrent
                            .getCourseChangeRequiredToReach(speedWithBearingOnApproximationFromCurrentToNext);
                    speedWithBearingOnApproximationFromPreviousToCurrent = speedWithBearingOnApproximationFromCurrentToNext;
                    Maneuver maneuver = createManeuverFromGroupOfCourseChanges(competitor,
                            speedWithBearingOnApproximationFromPreviousToCurrent, current,
                            speedWithBearingOnApproximationFromCurrentToNext, courseChange.getCourseChangeInDegrees());
                    result.add(maneuver);
                    previous = current;
                    current = next;
                } while (approximationPointsIter.hasNext());
            }
        }
        return result;
    }

    private Maneuver createManeuverFromGroupOfCourseChanges(Competitor competitor,
            SpeedWithBearing speedWithBearingOnApproximationAtBeginning, GPSFixMoving currentFix,
            SpeedWithBearing speedWithBearingOnApproximationAtEnd, double totalCourseChangeInDegrees) {
        TimePoint maneuverTimePoint = currentFix.getTimePoint();
        Position maneuverPosition = currentFix.getPosition();
        final Wind wind = trackedRace.getWind(maneuverPosition, maneuverTimePoint);
        Tack tackAfterManeuver = null;
        try {
            tackAfterManeuver = wind == null ? null
                    : trackedRace.getTack(maneuverPosition, maneuverTimePoint,
                            speedWithBearingOnApproximationAtEnd.getBearing());
        } catch (NoWindException e) {
        }
        ManeuverType maneuverType;
        SpeedWithBearing lowestSpeed;
        SpeedWithBearing highestSpeed;
        if (speedWithBearingOnApproximationAtBeginning.compareTo(speedWithBearingOnApproximationAtBeginning) < 0) {
            lowestSpeed = speedWithBearingOnApproximationAtBeginning;
            highestSpeed = speedWithBearingOnApproximationAtEnd;
        } else {
            lowestSpeed = speedWithBearingOnApproximationAtEnd;
            highestSpeed = speedWithBearingOnApproximationAtBeginning;
        }
        ManeuverCurveBoundaries maneuverCurve = new ManeuverCurveBoundariesImpl(
                maneuverTimePoint.minus(getApproximateManeuverDuration().divide(2)),
                maneuverTimePoint.plus(getApproximateManeuverDuration().times(3.0)),
                speedWithBearingOnApproximationAtBeginning, speedWithBearingOnApproximationAtEnd,
                totalCourseChangeInDegrees, lowestSpeed, highestSpeed);

        if (wind != null) {
            if (getNumberOfTacks(maneuverCurve, wind) > 0) {
                maneuverType = ManeuverType.TACK;
            } else if (getNumberOfJibes(maneuverCurve, wind) > 0) {
                maneuverType = ManeuverType.JIBE;
            } else {
                // heading up or bearing away
                Bearing windBearing = wind.getBearing();
                Bearing toWindBeforeManeuver = windBearing
                        .getDifferenceTo(speedWithBearingOnApproximationAtBeginning.getBearing());
                Bearing toWindAfterManeuver = windBearing
                        .getDifferenceTo(speedWithBearingOnApproximationAtEnd.getBearing());
                maneuverType = Math.abs(toWindBeforeManeuver.getDegrees()) < Math.abs(toWindAfterManeuver.getDegrees())
                        ? ManeuverType.HEAD_UP
                        : ManeuverType.BEAR_AWAY;
            }
        } else {
            // no wind information; marking as UNKNOWN
            maneuverType = ManeuverType.UNKNOWN;
        }
        Maneuver maneuver = new ManeuverWithCoarseGrainedBoundariesImpl(maneuverType, tackAfterManeuver,
                maneuverPosition, maneuverTimePoint, maneuverCurve);
        return maneuver;
    }

}
