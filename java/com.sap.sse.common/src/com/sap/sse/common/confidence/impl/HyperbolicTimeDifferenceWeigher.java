package com.sap.sse.common.confidence.impl;

import com.sap.sse.common.TimePoint;
import com.sap.sse.common.confidence.Weigher;

public class HyperbolicTimeDifferenceWeigher implements Weigher<TimePoint> {
    private static final long serialVersionUID = 4378168079868145134L;
    private final long halfConfidenceAfterMilliseconds;
    
    public HyperbolicTimeDifferenceWeigher(long halfConfidenceAfterMilliseconds) {
        this.halfConfidenceAfterMilliseconds = halfConfidenceAfterMilliseconds;
    }

    /**
     * Postconditions:
     * <pre>
     *   |fix-request|=halfConfidenceAfterMilliseconds ==&gt; result==0.5
     *   0 &lt;= result &lt;= 1
     *   fix==request ==&gt; result==1
     * </pre>
     * 
     * This implies the following formula for result := f(x):
     * 
     * <pre>
     * for x := |fix-request| with x&gt;=0:
     *   f(x) := c/(x+y)
     *   f(0) = 1 = c/y   ==&gt; c = y
     *   f(halfConfidenceAfterMilliseconds) = 0.5 = c/(halfConfidenceAfterMilliseconds+y)
     *   =&gt;  c/y = 2c/(halfConfidenceAfterMilliseconds+y)
     *   &lt;=&gt; 1/y = 2/(halfConfidenceAfterMilliseconds+y)
     *   &lt;=&gt; halfConfidenceAfterMilliseconds + y = 2y
     *   &lt;=&gt; y = halfConfidenceAfterMilliseconds
     *   =&gt;  c = halfConfidenceAfterMilliseconds
     * </pre>
     */
    @Override
    public double getConfidence(TimePoint fix, TimePoint request) {
        double x = Math.abs(fix.asMillis() - request.asMillis());
        double c = halfConfidenceAfterMilliseconds;
        double y = halfConfidenceAfterMilliseconds;
        return c/(x+y);
    }
}
