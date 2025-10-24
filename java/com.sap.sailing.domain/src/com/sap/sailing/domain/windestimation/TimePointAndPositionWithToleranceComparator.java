package com.sap.sailing.domain.windestimation;

import java.io.Serializable;
import java.util.Comparator;

import com.sap.sailing.domain.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

/**
 * Comparator which compares a pair composed of a position and time point. The special feature of this comparator is
 * that it takes inaccuracy of double values when comparing longitude and latitude by introducing a tolerance threshold.<p>
 * 
 * The comparators of this type implement a total ordering. The primary criterion is the time point; secondary is the latitude,
 * considering the tolerance; the third ordering criterion is the longitude, again considering the tolerance.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class TimePointAndPositionWithToleranceComparator implements Comparator<Pair<Position, TimePoint>>, Serializable {

    private static final long serialVersionUID = -3240742353261384636L;
    private static final double DOUBLE_TOLERANCE = 0.0000001;

    @Override
    public int compare(Pair<Position, TimePoint> o1, Pair<Position, TimePoint> o2) {
        int timePointComparison = o1.getB().compareTo(o2.getB());
        if (timePointComparison != 0) {
            return timePointComparison;
        }
        double latDiff = o1.getA().getLatDeg() - o2.getA().getLatDeg();
        if (Math.abs(latDiff) > DOUBLE_TOLERANCE) {
            return latDiff < 0 ? -1 : 1;
        }
        double lngDiff = o1.getA().getLngDeg() - o2.getA().getLngDeg();
        if (Math.abs(lngDiff) > DOUBLE_TOLERANCE) {
            return lngDiff < 0 ? -1 : 1;
        }
        return 0;
    }
}