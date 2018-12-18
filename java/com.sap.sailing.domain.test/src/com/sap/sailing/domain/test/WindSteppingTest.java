package com.sap.sailing.domain.test;

import org.junit.Assert;

import org.junit.Test;

import com.sap.sailing.domain.common.WindSpeedStepping;
import com.sap.sailing.domain.common.impl.WindSpeedSteppingImpl;
import com.sap.sailing.domain.common.impl.WindSpeedSteppingWithMaxDistance;

public class WindSteppingTest {
    
    @Test
    public void testStepping() {
        double[] levels = {2.,4.};
        WindSpeedStepping stepping = new WindSpeedSteppingImpl(levels);
        Assert.assertEquals(0, stepping.getLevelIndexForValue(1));
        Assert.assertEquals(1, stepping.getLevelIndexForValue(5));
        Assert.assertEquals(1, stepping.getLevelIndexForValue(3));
        Assert.assertEquals(2., stepping.getSteppedValueForValue(1.064), 0.005);
        Assert.assertEquals(4., stepping.getSteppedValueForValue(7.8365), 0.005);
    }
    
    @Test
    public void testSteppingWithMaxDistance() {
        double[] levels = {2.,4.};
        WindSpeedSteppingWithMaxDistance stepping = new WindSpeedSteppingWithMaxDistance(levels, 1.0);
        Assert.assertEquals(0, stepping.getLevelIndexForValue(1));
        Assert.assertEquals(1, stepping.getLevelIndexForValue(5));
        Assert.assertEquals(-1, stepping.getLevelIndexForValue(5.01));
        Assert.assertEquals(-1, stepping.getLevelIndexForValue(0.5));
        Assert.assertEquals(-1, stepping.getLevelIndexForValue(8));
        Assert.assertEquals(2.0, stepping.getSteppedValueForValue(1.064), 0.005);
        Assert.assertEquals(-1.0, stepping.getSteppedValueForValue(7.8365), 0.005);
        Assert.assertEquals(1.55, stepping.getHistogramXValue(20, 1.5), 0.0000001);
        Assert.assertEquals(2.55, stepping.getHistogramXValue(20, 2.5), 0.0000001);
    }

}
