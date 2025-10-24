package com.sap.sailing.domain.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.WindSpeedStepping;
import com.sap.sailing.domain.common.impl.WindSpeedSteppingImpl;
import com.sap.sailing.domain.common.impl.WindSpeedSteppingWithMaxDistance;

public class WindSteppingTest {
    
    @Test
    public void testStepping() {
        double[] levels = {2.,4.};
        WindSpeedStepping stepping = new WindSpeedSteppingImpl(levels);
        Assertions.assertEquals(0, stepping.getLevelIndexForValue(1));
        Assertions.assertEquals(1, stepping.getLevelIndexForValue(5));
        Assertions.assertEquals(1, stepping.getLevelIndexForValue(3));
        Assertions.assertEquals(2., stepping.getSteppedValueForValue(1.064), 0.005);
        Assertions.assertEquals(4., stepping.getSteppedValueForValue(7.8365), 0.005);
    }
    
    @Test
    public void testSteppingWithMaxDistance() {
        double[] levels = {2.,4.};
        WindSpeedSteppingWithMaxDistance stepping = new WindSpeedSteppingWithMaxDistance(levels, 1.0);
        Assertions.assertEquals(0, stepping.getLevelIndexForValue(1));
        Assertions.assertEquals(1, stepping.getLevelIndexForValue(5));
        Assertions.assertEquals(-1, stepping.getLevelIndexForValue(5.01));
        Assertions.assertEquals(-1, stepping.getLevelIndexForValue(0.5));
        Assertions.assertEquals(-1, stepping.getLevelIndexForValue(8));
        Assertions.assertEquals(2.0, stepping.getSteppedValueForValue(1.064), 0.005);
        Assertions.assertEquals(-1.0, stepping.getSteppedValueForValue(7.8365), 0.005);
        Assertions.assertEquals(1.55, stepping.getHistogramXValue(20, 1.5), 0.0000001);
        Assertions.assertEquals(2.55, stepping.getHistogramXValue(20, 2.5), 0.0000001);
    }

}
