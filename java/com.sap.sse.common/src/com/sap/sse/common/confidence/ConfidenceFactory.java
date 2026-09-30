package com.sap.sse.common.confidence;

import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.confidence.impl.ConfidenceBasedAveragerFactoryImpl;

public interface ConfidenceFactory {
    ConfidenceFactory INSTANCE = new ConfidenceBasedAveragerFactoryImpl();
    
    /**
     * @param weigher
     *            used to determine the confidence of the elements to be averaged, relative to the reference point given
     *            as parameter to {@link ConfidenceBasedAverager#getAverage(Iterable, Object)}. If <code>null</code>,
     *            1.0 will be assumed as default confidence for all values provided, regardless the reference point
     *            relative to which the average is to be computed
     */
    <ValueType, BaseType, RelativeTo> ConfidenceBasedAverager<ValueType, BaseType, RelativeTo> createAverager(Weigher<RelativeTo> weigher);
    
    <RelativeTo> Weigher<RelativeTo> createConstantWeigher(double constantConfidence);

    /**
     * Creates a weigher for time points. With increasing time difference the weight/confidence decreases exponentially.
     * It is halved every <code>halfConfidenceAfterMilliseconds</code> milliseconds.
     */
    Weigher<TimePoint> createExponentialTimeDifferenceWeigher(long halfConfidenceAfterMilliseconds);

    /**
     * Like {@link #createExponentialTimeDifferenceWeigher(long)}, only that additionally a minimum confidence value is defined.
     * This can be useful for averagers that have trouble with values scaled down with 0.0.
     */
    Weigher<TimePoint> createExponentialTimeDifferenceWeigher(long halfConfidenceAfterMilliseconds, double minimumConfidence);

    Weigher<TimePoint> createHyperbolicTimeDifferenceWeigher(long halfConfidenceAfterMilliseconds);

    Weigher<TimePoint> createStandardDistributionTimeDifferenceWeigher(Duration standardDeviation);

    Weigher<TimePoint> createHyperbolicSquaredTimeDifferenceWeigher(long halfConfidenceAfterMilliseconds);

    /**
     * Constructs a weigher that determines a confidence based on a distance. A {@link Distance#NULL zero distance}
     * will yield a confidence of 1; a distance of <code>halfConfidence</code> will return a confidence of .5, and the
     * confidence will decrease hyperbolically with increasing distance.
     */
    Weigher<Position> createHyperbolicDistanceWeigher(Distance halfConfidence);
}
