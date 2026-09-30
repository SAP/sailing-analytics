package com.sap.sse.common.confidence.impl;

import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.confidence.ConfidenceFactory;
import com.sap.sse.common.confidence.Weigher;

/**
 * A weigher that uses a {@link Position} and a {@link TimePoint} to compute a confidence based on
 * time and space distance. If the <code>fix</code> or the <code>request</code> parameter in a call
 * to {@link #getConfidence(com.sap.sse.common.Util.Pair, com.sap.sse.common.Util.Pair)} have a <code>null</code>
 * {@link Position} then no distance-based confidence is considered, and only the time difference is taken
 * into account. Otherwise, the time-based confidence and the distance-based confidence are multiplied to
 * result in the total confidence.<p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class PositionAndTimePointWeigher implements Weigher<Util.Pair<Position, TimePoint>> {
    private final static String USE_POSITION_SYSTEM_PROPERTY_NAME = "spatialWind";
    private static final long serialVersionUID = -262428237738496818L;
    private final Weigher<TimePoint> timeWeigher;
    private final Weigher<Position> distanceWeigher;
    private final boolean usePosition;

    public PositionAndTimePointWeigher(Duration halfConfidenceAfter, Distance halfConfidenceDistance) {
        timeWeigher = ConfidenceFactory.INSTANCE.createHyperbolicTimeDifferenceWeigher(
                /* use as standard deviation */ halfConfidenceAfter.asMillis());
        distanceWeigher = ConfidenceFactory.INSTANCE.createHyperbolicDistanceWeigher(halfConfidenceDistance);
        this.usePosition = Boolean.valueOf(System.getProperty(USE_POSITION_SYSTEM_PROPERTY_NAME, "true"));
    }
    
    @Override
    public double getConfidence(Util.Pair<Position, TimePoint> fix, Util.Pair<Position, TimePoint> reference) {
        final double timeConfidence = timeWeigher.getConfidence(fix.getB(), reference.getB());
        final double distanceConfidence;
        // For now, we make the use of the position for spatial wind field calculation optional. It has to be turned
        // on by providing a system property using -DspatialWind=true
        if (usePosition && reference != null) {
            if (fix.getA() != null && reference.getA() != null) {
                distanceConfidence = distanceWeigher.getConfidence(fix.getA(), reference.getA());
            } else {
                distanceConfidence = 0.1;
            }
        } else {
            distanceConfidence = 1;
        }
        return timeConfidence * distanceConfidence;
    }
}
