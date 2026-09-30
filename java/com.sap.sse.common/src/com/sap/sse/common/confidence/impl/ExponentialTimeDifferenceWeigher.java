package com.sap.sse.common.confidence.impl;

import com.sap.sse.common.TimePoint;
import com.sap.sse.common.confidence.Weigher;

/**
 * The weigher computes an exponentially-decreasing weight based on the time difference of two {@link TimePoint}s.
 * The weigher can be configured by the time difference after which the confidence is halved. A minimum confidence can
 * optionally be set.
 * 
 * @author Axel Uhl (d043530)
 */
public class ExponentialTimeDifferenceWeigher implements Weigher<TimePoint> {
    private static final long serialVersionUID = 1832731495731693670L;

    private final static double logHalf = Math.log(0.5);
    
    private final long halfConfidenceAfterMilliseconds;
    
    private final double minimumConfidence;
    
    public ExponentialTimeDifferenceWeigher(long halfConfidenceAfterMilliseconds, double minimumConfidence) {
        this.halfConfidenceAfterMilliseconds = halfConfidenceAfterMilliseconds;
        this.minimumConfidence = minimumConfidence;
    }

    public ExponentialTimeDifferenceWeigher(long halfConfidenceAfterMilliseconds) {
        this(halfConfidenceAfterMilliseconds, /* minimumConfidence */ 0);
    }

    @Override
    public double getConfidence(TimePoint fix, TimePoint request) {
        return Math.max(minimumConfidence,
                Math.exp(logHalf * ((double) (Math.abs(request.asMillis() - fix.asMillis())) / (double) halfConfidenceAfterMilliseconds)));
    }

}
