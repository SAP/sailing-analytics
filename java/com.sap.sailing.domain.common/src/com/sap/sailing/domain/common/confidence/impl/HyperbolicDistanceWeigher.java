package com.sap.sailing.domain.common.confidence.impl;

import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.confidence.Weigher;

public class HyperbolicDistanceWeigher implements Weigher<Position> {
    private static final long serialVersionUID = 5521299690322077224L;

    private final Distance halfConfidence;
    
    public HyperbolicDistanceWeigher(Distance halfConfidence) {
        this.halfConfidence = halfConfidence;
    }

    /**
     * Postconditions:
     * <pre>
     *   |fix-request|=halfConfidence ==&gt; result==0.5
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
     *   f(halfConfidence) = 0.5 = c/(halfConfidence+y)
     *   =&gt;  c/y = 2c/(halfConfidence+y)
     *   &lt;=&gt; 1/y = 2/(halfConfidence+y)
     *   &lt;=&gt; halfConfidence + y = 2y
     *   &lt;=&gt; y = halfConfidence
     *   =&gt;  c = halfConfidence
     * </pre>
     */
    @Override
    public double getConfidence(Position fix, Position request) {
        double x = fix.getDistance(request).getMeters();
        double c = halfConfidence.getMeters();
        double y = c;
        return c/(x+y);
    }
}
