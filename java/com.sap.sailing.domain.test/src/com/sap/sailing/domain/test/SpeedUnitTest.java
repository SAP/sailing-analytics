package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MeterPerSecondSpeedImpl;
import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class SpeedUnitTest {
    @Test
    public void testMeterPerSecondSpeedToKnots() {
        Speed s = new MeterPerSecondSpeedImpl(1852./3600.);
        assertEquals(1, s.getKnots(), 0.0000001);
    }
    
    @Test
    public void testSpeedWithBearingAddInverse() {
        SpeedWithBearing s1 = new KnotSpeedWithBearingImpl(12., new DegreeBearingImpl(0));
        SpeedWithBearing s2 = new KnotSpeedWithBearingImpl(12., new DegreeBearingImpl(180));
        SpeedWithBearing sum = s1.add(s2);
        assertEquals(0.0, sum.getKnots(), 0.0000001);
    }

    @Test
    public void testSpeedWithBearingAddCollinear() {
        SpeedWithBearing s1 = new KnotSpeedWithBearingImpl(12., new DegreeBearingImpl(123));
        SpeedWithBearing s2 = new KnotSpeedWithBearingImpl(15., new DegreeBearingImpl(123));
        SpeedWithBearing sum = s1.add(s2);
        assertEquals(27.0, sum.getKnots(), 0.01);
        assertEquals(123, sum.getBearing().getDegrees(), 0.0000001);
    }

    @Test
    public void testSpeedWithBearingAddOrthogonal() {
        SpeedWithBearing s1 = new KnotSpeedWithBearingImpl(12., new DegreeBearingImpl(20));
        SpeedWithBearing s2 = new KnotSpeedWithBearingImpl(12., new DegreeBearingImpl(110));
        SpeedWithBearing sum = s1.add(s2);
        assertEquals(Math.sqrt(12*12+12*12), sum.getKnots(), 0.01);
        assertEquals(65., sum.getBearing().getDegrees(), 0.0000001);
    }
    
    @Test
    public void testSpeedWithBearingAddExample() {
        SpeedWithBearing s1 = new KnotSpeedWithBearingImpl(10., new DegreeBearingImpl(225)); // SW
        SpeedWithBearing s2 = new KnotSpeedWithBearingImpl(10./Math.sqrt(2.0), new DegreeBearingImpl(0)); // back to original lat by traveling N
        SpeedWithBearing sum = s1.add(s2);
        assertEquals(10./Math.sqrt(2.0), sum.getKnots(), 0.01);
        assertEquals(270., sum.getBearing().getDegrees(), 0.0000001);
    }
}
