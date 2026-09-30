package com.sap.sailing.dashboards.gwt.server.util.actions.startlineadvantage;

import java.util.Iterator;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * @author Alexander Ries (D062114)
 *
 */
@GwtIncompatible
public final class StartlineAdvantageByGeometryCalculator {
    
    public static Double calculateStartlineAdvantageByGeometry(TrackedRace trackedRace) {
        Double result = null;
        Course course = trackedRace.getRace().getCourse();
        if (course != null) {
            Waypoint startlineWayPoint = course.getFirstLeg().getFrom();
            Waypoint firstmarkWayPoint = course.getFirstLeg().getTo();
            if (startlineWayPoint != null && firstmarkWayPoint != null) {
                Pair<Position, Position> startlineMarkPositions = retrieveStartlineMarkPositionsFromStartLineWayPoint(trackedRace, startlineWayPoint);
                Position firstMarkPosition = retrieveFirstMarkPositionFromFirstMarkWayPoint(trackedRace, firstmarkWayPoint);
                if (startlineMarkPositions != null && firstMarkPosition != null) {
                    Distance rcToMark = firstMarkPosition.getDistance(startlineMarkPositions.getA());
                    Distance pinToMark = firstMarkPosition.getDistance(startlineMarkPositions.getB());
                    double startlineadvantage =  rcToMark.getMeters() - pinToMark.getMeters();
                    result = Double.valueOf(startlineadvantage);
                }
            }
        }
        return result;
    }
    
    private static Pair<Position, Position> retrieveStartlineMarkPositionsFromStartLineWayPoint(TrackedRace trackedRace, Waypoint startLineWayPoint) {
        Pair<Position, Position> result = null;
        Iterator<Mark> markIterator = startLineWayPoint.getMarks().iterator();
        if (markIterator.hasNext()) {
            Mark startboat = (Mark) markIterator.next();
            if (markIterator.hasNext()) {
                Mark pinEnd = (Mark) markIterator.next();
                TimePoint now = MillisecondsTimePoint.now();
                Position startBoatPosition = getPositionFromMarkAtTimePoint(trackedRace, startboat, now);
                Position pinEndPosition = getPositionFromMarkAtTimePoint(trackedRace, pinEnd, now);
                if(startBoatPosition != null && pinEndPosition != null) {
                    result = new Pair<Position, Position>(startBoatPosition, pinEndPosition);                    
                }
            }
        }
        return result;
    }

    private static Position getPositionFromMarkAtTimePoint(TrackedRace trackedRace, Mark mark, TimePoint timePoint) {
        GPSFixTrack<Mark, GPSFix> fixTrack = trackedRace.getTrack(mark);
        return fixTrack.getEstimatedPosition(timePoint, true);
    }

    private static Position retrieveFirstMarkPositionFromFirstMarkWayPoint(TrackedRace trackedRace, Waypoint firstMarkWayPoint) {
        Position firstMarkPosition = null;
        if (firstMarkWayPoint.getMarks().iterator().hasNext()) {
            Mark firstMark = firstMarkWayPoint.getMarks().iterator().next();
            TimePoint now = MillisecondsTimePoint.now();
            firstMarkPosition = getPositionFromMarkAtTimePoint(trackedRace, firstMark, now);
        }
        return firstMarkPosition;
    }
}
