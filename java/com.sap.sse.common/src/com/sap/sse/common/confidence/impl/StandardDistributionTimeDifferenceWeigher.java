package com.sap.sse.common.confidence.impl;

import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.confidence.Weigher;

public class StandardDistributionTimeDifferenceWeigher implements Weigher<TimePoint> {
    private static final long serialVersionUID = 4378168079868145134L;
    private final double standardDeviationAsMillis;
    private final double oneDividedByStandardDeviationTimesSquareRootOfTwoPi;
    
    public StandardDistributionTimeDifferenceWeigher(Duration standardDeviation) {
        this.standardDeviationAsMillis = standardDeviation.asMillis();
        this.oneDividedByStandardDeviationTimesSquareRootOfTwoPi = 1/(this.standardDeviationAsMillis*Math.sqrt(2*Math.PI));
    }

    @Override
    public double getConfidence(TimePoint fix, TimePoint request) {
        double xMinusMu = Math.abs(fix.asMillis() - request.asMillis());
        double xMinusMuDividedByStandardDeviation = xMinusMu/standardDeviationAsMillis;
        return oneDividedByStandardDeviationTimesSquareRootOfTwoPi * Math.exp(-0.5*xMinusMuDividedByStandardDeviation*xMinusMuDividedByStandardDeviation);
    }
}
