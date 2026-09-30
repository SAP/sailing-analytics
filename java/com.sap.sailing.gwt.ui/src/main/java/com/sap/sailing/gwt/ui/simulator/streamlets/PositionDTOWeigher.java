package com.sap.sailing.gwt.ui.simulator.streamlets;

import java.util.Date;

import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.confidence.Weigher;
import com.sap.sse.common.impl.DegreePosition;

/**
 * A weigher that uses a {@link DegreePosition} and a {@link Date} to compute a confidence based on spatial
 * distance. If the <code>fix</code> or the <code>request</code> parameter in a call to
 * {@link #getConfidence(com.sap.sse.common.Util.Pair, com.sap.sse.common.Util.Pair)} have a <code>null</code>
 * {@link Position} then no distance-based confidence is considered, and only the time difference is taken into account.
 * Otherwise, the time-based confidence and the distance-based confidence are multiplied to result in the total
 * confidence.
 * <p>
 * 
 * For calculating the distances, this weigher uses an approximation based on the euclidian mapping of latitude and
 * longitude, assuming that when multiplying the longitudes with cos(lat) we can approximately apply cartesian geometry.
 * <p>
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class PositionDTOWeigher implements Weigher<Position> {
    private static final long serialVersionUID = -262428237738496818L;
    private final double halfConfidenceDistanceNauticalMiles;
    private final AverageLatitudeProvider averageLatitudeDegProvider;
    
    public static interface AverageLatitudeProvider {
        double getCosineOfAverageLatitude();
    }
    
    public PositionDTOWeigher(Distance halfConfidenceDistance, AverageLatitudeProvider averageLatitudeDegProvider) {
        this.halfConfidenceDistanceNauticalMiles = halfConfidenceDistance.getNauticalMiles();
        this.averageLatitudeDegProvider = averageLatitudeDegProvider;
    }

    private double getCosineOfAverageLatitude() {
        return averageLatitudeDegProvider.getCosineOfAverageLatitude();
    }
    
    @Override
    public double getConfidence(Position fix, Position request) {
        final double distanceConfidence;
        if (fix != null && request != null) {
            double x = getApproximateNauticalMileDistance(fix, request);
            double c = halfConfidenceDistanceNauticalMiles;
            double y = c;
            distanceConfidence = c / (x + y);
        } else {
            distanceConfidence = 0.0001; // if we have no information about the position, let's not distort an
                                         // otherwise spatially-resolved field
        }
        return distanceConfidence;
    }

    private double getApproximateNauticalMileDistance(Position p1, Position p2) {
        final double latDiffDeg = Math.abs(p1.getLatDeg() - p2.getLatDeg());
        final double normalizedLngDiffDeg = getCosineOfAverageLatitude() * Math.abs(p1.getLngDeg() - p2.getLngDeg());
        // One degree of latitude or one degree of longitude at the equator each correspond to 60 nautical miles.
        return Math.sqrt(latDiffDeg*latDiffDeg + normalizedLngDiffDeg*normalizedLngDiffDeg) * 60.;
    }
}
