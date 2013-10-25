package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.Mile;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.tracking.impl.CompactGPSFixImpl;
import com.sap.sailing.domain.tracking.impl.CompactGPSFixMovingImpl;

public class PositionTest {
    @Test
    public void testEqualityBetweenCompactAndVerbosePosition() {
        Position p1 = new DegreePosition(49.2, 008.3);
        CompactGPSFixImpl compactFix = new CompactGPSFixImpl(p1, MillisecondsTimePoint.now());
        assertNotSame(p1, compactFix.getPosition());
        assertEquals(p1, compactFix.getPosition());
    }
    
    @Test
    public void testEqualityBetweenCompactAndVerboseSpeedWithBearing() {
        Position p1 = new DegreePosition(49.2, 008.3);
        SpeedWithBearing swb = new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123));
        CompactGPSFixMovingImpl compactFix = new CompactGPSFixMovingImpl(p1, MillisecondsTimePoint.now(), swb);
        assertNotSame(p1, compactFix.getPosition());
        assertEquals(p1, compactFix.getPosition());
        assertNotSame(swb, compactFix.getSpeed());
        assertEquals(swb, compactFix.getSpeed());
    }
    
    @Test
    public void translateTwoDegreesSouth() {
        Distance distance = new NauticalMileDistance(120/Mile.METERS_PER_GEOGRAPHICAL_MILE*Mile.METERS_PER_NAUTICAL_MILE);
        Bearing south = new DegreeBearingImpl(180);
        Position fiveNorthFourEast = new DegreePosition(5, 4);
        Position result = fiveNorthFourEast.translateGreatCircle(south, distance);
        assertEquals(3, result.getLatDeg(), 0.001);
        assertEquals(4, result.getLngDeg(), 0.0000001);
    }
    
    @Test
    public void distanceTest() {
        Position p1 = new DegreePosition(49.2, 008.3);
        Position p2 = new DegreePosition(49.3, 008.2);
        assertTrue(p1.getDistance(p2).getSeaMiles() < 10);
        Position northPole = new DegreePosition(90, 0);
        Position southPole = new DegreePosition(-90, 0);
        assertEquals(20004, Math.abs(southPole.getDistance(northPole).getKilometers()), 0.0001);
    }
    
    @Test
    public void bearingTest() {
        // Note that the bearing is a bearing on a great circle!
        Position p1 = new DegreePosition(49.2, 008.3);
        Position p2 = new DegreePosition(49.3, 008.3);
        assertEquals(0.0, p1.getBearingGreatCircle(p2).getDegrees(), 0.1);
        
        Position p3 = new DegreePosition(49.2, 008.3);
        Position p4 = new DegreePosition(49.2, 008.4);
        assertEquals(90.0, p3.getBearingGreatCircle(p4).getDegrees(), 0.1);

        Position p5 = new DegreePosition(0.0, 008.3);
        Position p6 = new DegreePosition(0.1, 008.4);
        Position p7 = new DegreePosition(0.1, 008.2);
        assertEquals(45.0, p5.getBearingGreatCircle(p6).getDegrees(), 0.1);
        assertEquals(315.0, p5.getBearingGreatCircle(p7).getDegrees(), 0.1);
        assertEquals(135.0, p7.getBearingGreatCircle(p5).getDegrees(), 0.1);
        assertEquals(225.0, p6.getBearingGreatCircle(p5).getDegrees(), 0.1);
    }
    
    @Test
    public void translateTest() {
        Position p1 = new DegreePosition(0, 0);
        // now travel to 123� for 0.001 nautical miles (~ 1.852m)
        Position p2 = p1.translateGreatCircle(new DegreeBearingImpl(123), new NauticalMileDistance(0.001));
        assertEquals(123, p1.getBearingGreatCircle(p2).getDegrees(), 0.0000001);
        assertEquals(0.001, p1.getDistance(p2).getNauticalMiles(), 0.0000001);
    }
    
    @Test
    public void testTranslateGreatCircle() {
        Position p1 = new DegreePosition(49.2, 008.3);
        Position p2 = new DegreePosition(49.3, 008.3);
        assertEquals(0, p1.translateGreatCircle(p1.getBearingGreatCircle(p2), p1.getDistance(p2)).getDistance(p2).getMeters(), 0.00001);
    }
    
    @Test
    public void simpleProjectionTest1() {
        // The simplest thing we can test is projecting coordinates along latitudes or longitudes:
        Position p1 = new DegreePosition(20, 15);
        Position p2 = new DegreePosition(15, 15);
        Position result = p1.projectToLineThrough(p2, new DegreeBearingImpl(90)); // project onto a latitude
                                                                                  // crossing through p2
        assertEquals(15, result.getLatDeg(), 0.0000001);
        assertEquals(15, result.getLngDeg(), 0.0000001);
    }

    @Test
    public void simpleProjectionTest2() {
        Position p1 = new DegreePosition(0, 30);
        Position p2 = new DegreePosition(15, 15);
        Position result = p1.projectToLineThrough(p2, new DegreeBearingImpl(0)); // project onto a longitude
                                                                                 // crossing through p2
        assertEquals(0, result.getLatDeg(), 0.0000001);
        assertEquals(15, result.getLngDeg(), 0.0000001);
    }

    @Test
    public void simpleProjectionTest3() {
        // The simplest thing we can test is projecting coordinates along latitudes or longitudes:
        Position p1 = new DegreePosition(0, 15);
        Position p2 = new DegreePosition(15, 15);
        Position result = p1.projectToLineThrough(p2, new DegreeBearingImpl(270)); // project onto a latitude
                                                                                   // crossing through p2
        assertEquals(15, result.getLatDeg(), 0.0000001);
        assertEquals(15, result.getLngDeg(), 0.0000001);
    }

    @Test
    public void simpleProjectionTest4() {
        Position p1 = new DegreePosition(0, -30);
        Position p2 = new DegreePosition(15, 15);
        Position result = p1.projectToLineThrough(p2, new DegreeBearingImpl(180)); // project onto a longitude
                                                                                   // crossing through p2
        assertEquals(0, result.getLatDeg(), 0.0000001);
        assertEquals(15, result.getLngDeg(), 0.0000001);
    }

    @Test
    public void simpleProjectionTest5() {
        Position p1 = new DegreePosition(1, 0);
        Position p2 = new DegreePosition(1, -0.0001);
        final Bearing bearingFromP1ToP2 = p1.getBearingGreatCircle(p2);
        assertEquals(270, bearingFromP1ToP2.getDegrees(), 0.00001);
        final Bearing bearing45DegNorth = new DegreeBearingImpl(bearingFromP1ToP2.getDegrees()+45);
        Position pNorth = p1.translateGreatCircle(bearing45DegNorth, p1.getDistance(p2).scale(1./Math.sqrt(2.)));
        assertEquals(bearing45DegNorth.getDegrees(), p1.getBearingGreatCircle(pNorth).getDegrees(), 0.0000001);
        Distance alongTrackDistanceFromP1ToPNorth = pNorth.alongTrackDistance(p1, bearingFromP1ToP2);
        assertEquals(0.5*p1.getDistance(p2).getMeters(), alongTrackDistanceFromP1ToPNorth.getMeters(), 0.001);
    }

    @Test
    public void testZeroCrossTrackError() {
        Position p1 = new DegreePosition(20, 15);
        Position p2 = new DegreePosition(15, 15);
        Distance result = p1.absoluteCrossTrackError(p2, new DegreeBearingImpl(0));
        assertEquals(0, result.getMeters(), 0.0000001);
    }
    
    @Test
    public void addBearings() {
        Bearing b1 = new DegreeBearingImpl(123);
        Bearing b2 = new DegreeBearingImpl(234);
        Bearing sum = b1.add(b2);
        assertEquals(123+234, sum.getDegrees(), 0.000000001);
    }

    @Test
    public void addBearingsWithCarry() {
        Bearing b1 = new DegreeBearingImpl(123);
        Bearing b2 = new DegreeBearingImpl(345);
        Bearing sum = b1.add(b2);
        assertEquals(123+345-360, sum.getDegrees(), 0.000000001);
    }

    @Test
    public void bearingDifferenceTest() {
        Bearing b1 = new DegreeBearingImpl(340);
        Bearing b2 = new DegreeBearingImpl(350);
        Bearing b3 = new DegreeBearingImpl(0);
        Bearing b4 = new DegreeBearingImpl(10);
        Bearing b5 = new DegreeBearingImpl(20);
        Bearing b6 = new DegreeBearingImpl(180);
        
        assertEquals(10., b1.getDifferenceTo(b2).getDegrees(), 0.000000001);
        assertEquals(10., b2.getDifferenceTo(b3).getDegrees(), 0.000000001);
        assertEquals(10., b3.getDifferenceTo(b4).getDegrees(), 0.000000001);
        assertEquals(10., b4.getDifferenceTo(b5).getDegrees(), 0.000000001);

        assertEquals(20., b2.getDifferenceTo(b4).getDegrees(), 0.000000001);
        assertEquals(-20., b4.getDifferenceTo(b2).getDegrees(), 0.000000001);
        assertEquals(40., b1.getDifferenceTo(b5).getDegrees(), 0.000000001);
        assertEquals(-40., b5.getDifferenceTo(b1).getDegrees(), 0.000000001);

        assertEquals(180., b3.getDifferenceTo(b6).getDegrees(), 0.000000001);
        assertEquals(-180., b6.getDifferenceTo(b3).getDegrees(), 0.000000001);
    }

}
