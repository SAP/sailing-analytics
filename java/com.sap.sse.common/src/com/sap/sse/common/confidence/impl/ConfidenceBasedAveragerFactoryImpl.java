package com.sap.sse.common.confidence.impl;

import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.confidence.ConfidenceBasedAverager;
import com.sap.sse.common.confidence.ConfidenceFactory;
import com.sap.sse.common.confidence.Weigher;

public class ConfidenceBasedAveragerFactoryImpl implements ConfidenceFactory {
    @Override
    public <RelativeTo> Weigher<RelativeTo> createConstantWeigher(final double constantConfidence) {
        return new Weigher<RelativeTo>() {
            private static final long serialVersionUID = 8693131975511149792L;

            @Override
            public double getConfidence(RelativeTo fix, RelativeTo request) {
                return constantConfidence;
            }
        };
    }

    @Override
    public <ValueType, BaseType, RelativeTo> ConfidenceBasedAverager<ValueType, BaseType, RelativeTo> createAverager(Weigher<RelativeTo> weigher) {
        return new ConfidenceBasedAveragerImpl<ValueType, BaseType, RelativeTo>(weigher);
    }
    
    @Override
    public Weigher<TimePoint> createHyperbolicTimeDifferenceWeigher(long halfConfidenceAfterMilliseconds) {
        return new HyperbolicTimeDifferenceWeigher(halfConfidenceAfterMilliseconds);
    }
    
    @Override
    public Weigher<TimePoint> createStandardDistributionTimeDifferenceWeigher(Duration standardDeviation) {
        return new StandardDistributionTimeDifferenceWeigher(standardDeviation);
    }
    
    @Override
    public Weigher<Position> createHyperbolicDistanceWeigher(Distance halfConfidence) {
        return new HyperbolicDistanceWeigher(halfConfidence);
    }
    
    @Override
    public Weigher<TimePoint> createHyperbolicSquaredTimeDifferenceWeigher(long halfConfidenceAfterMilliseconds) {
        return new HyperbolicSquaredTimeDifferenceWeigher(halfConfidenceAfterMilliseconds);
    }
    
    @Override
    public Weigher<TimePoint> createExponentialTimeDifferenceWeigher(long halfConfidenceAfterMilliseconds) {
        return new ExponentialTimeDifferenceWeigher(halfConfidenceAfterMilliseconds);
    }

    @Override
    public Weigher<TimePoint> createExponentialTimeDifferenceWeigher(long halfConfidenceAfterMilliseconds, double minimumConfidence) {
        return new ExponentialTimeDifferenceWeigher(halfConfidenceAfterMilliseconds, minimumConfidence);
    }
}
