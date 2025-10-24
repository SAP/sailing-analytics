package com.sap.sailing.domain.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class ScalableBearingTest {
    @Test
    public void testTrivialScaling() {
        Bearing b = new DegreeBearingImpl(123);
        ScalableBearing sb = new ScalableBearing(b);
        Bearing reducedScalableBearing = sb.divide(1);
        assertEquals(b.getDegrees(), reducedScalableBearing.getDegrees(), 0.0001);
    }
}
