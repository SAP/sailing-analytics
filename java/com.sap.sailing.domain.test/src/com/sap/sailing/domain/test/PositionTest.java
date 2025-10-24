package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.Mile;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.impl.CompactionNotPossibleException;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixMovingImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PositionTest {
    @Test
    public void testDistanceAcrossDateline() {
        Position p1 = new DegreePosition(0, 179);
        Position p2 = new DegreePosition(0, -179);
        assertEquals(120., p1.getDistance(p2).getNauticalMiles(), 0.1);
        assertEquals(120., p2.getDistance(p1).getNauticalMiles(), 0.1);
    }
    
    @Test
    public void testEqualityBetweenCompactAndVerbosePosition() {
        Position p1 = new DegreePosition(49.2, 008.3);
        VeryCompactGPSFixImpl compactFix = new VeryCompactGPSFixImpl(p1, MillisecondsTimePoint.now());
        assertNotSame(p1, compactFix.getPosition());
        PositionAssert.assertPositionEquals(p1, compactFix.getPosition(), 0.000001);
    }
    
    @Test
    public void testCompactFixRange() {
        final TimePoint now = MillisecondsTimePoint.now();
        assertCompactFixHasEqualLatLng(now, 0., 0.);
        assertCompactFixHasEqualLatLng(now, 90., 0.);
        assertCompactFixHasEqualLatLng(now, -90., 0.);
        assertCompactFixHasEqualLatLng(now, 0., 180.);
        assertCompactFixHasEqualLatLng(now, 0., -180.);
        assertCompactFixHasEqualLatLng(now, 45., 45.);
        assertCompactFixHasEqualLatLng(now, 0., -45.);
        assertCompactFixHasEqualLatLng(now, -45., 45.);
        assertCompactFixHasEqualLatLng(now, 0., 45.);
    }
    
    private void assertCompactFixHasEqualLatLng(final TimePoint now, final double latDeg, final double lngDeg) {
        GPSFix fix = new VeryCompactGPSFixImpl(new DegreePosition(latDeg, lngDeg), now);
        assertEquals(latDeg, fix.getPosition().getLatDeg(), 0.0000001);
        assertEquals(lngDeg, fix.getPosition().getLngDeg(), 0.0000001);
    }
    
    @Test
    public void testEqualityBetweenCompactAndVerboseSpeedWithBearing() throws CompactionNotPossibleException {
        Position p1 = new DegreePosition(49.2, 008.3);
        SpeedWithBearing swb = new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123));
        VeryCompactGPSFixMovingImpl compactFix = new VeryCompactGPSFixMovingImpl(p1, MillisecondsTimePoint.now(), swb, /* optionalTrueHeading */ null);
        assertNotSame(p1, compactFix.getPosition());
        PositionAssert.assertPositionEquals(p1, compactFix.getPosition(), 0.000001);
        assertNotSame(swb, compactFix.getSpeed());
        PositionAssert.assertSpeedEquals(swb, compactFix.getSpeed(), /* bearing deg delta */ 0.1, /* knot speed delta */ 0.1);
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
    public void radTest() {
        assertEquals(180, new MeterDistance(20005000).getCentralAngleDeg(), 0.01);
        assertEquals(Math.PI, new MeterDistance(20005000).getCentralAngleRad(), 0.01);
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
    public void negativeAlongTrackDistanceTest() {
        Position p1 = new DegreePosition(1, 0);
        Position p2 = new DegreePosition(1, -0.0001);
        final Bearing bearingFromP1ToP2 = p1.getBearingGreatCircle(p2);
        assertEquals(-p1.getDistance(p2).getMeters(), p1.alongTrackDistance(p2, bearingFromP1ToP2).getMeters(), 0.001);
    }

    @Test
    public void testZeroCrossTrackError() {
        Position p1 = new DegreePosition(20, 15);
        Position p2 = new DegreePosition(15, 15);
        Distance result = p1.absoluteCrossTrackError(p2, new DegreeBearingImpl(0));
        assertEquals(0, result.getMeters(), 0.0000001);
    }
    
    @Test
    public void testSignedCrossTrackErrorToRight() {
        Position p1 = new DegreePosition(20, 16);
        Position p2 = new DegreePosition(15, 15);
        Distance result = p1.crossTrackError(p2, new DegreeBearingImpl(0));
        assertTrue(result.getMeters() > 0);
    }
    
    @Test
    public void testSignedCrossTrackErrorToLeft() {
        Position p1 = new DegreePosition(20, 14);
        Position p2 = new DegreePosition(15, 15);
        Distance result = p1.crossTrackError(p2, new DegreeBearingImpl(0));
        assertTrue(result.getMeters() < 0);
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
    @Test
    public void distanceToLineTest(){
    	double delta = 0.000000001;
    	Position left = new DegreePosition(-1, -1);
    	Position right = new DegreePosition(1, 1);
    	Position p1 = new DegreePosition(0,0);
    	Position p2 = new DegreePosition(1, -1);
    	Position p3 = new DegreePosition(-2, 0);
    	Position p4 = new DegreePosition(5, 0);
    	Position p6 = new DegreePosition(0, -2);
    	assertEquals(p2.getDistanceToLine(left, right).getMeters(), p2.crossTrackError(left, left.getBearingGreatCircle(right)).getMeters(), delta);
    	assertEquals(p3.getDistanceToLine(left, right).getMeters(), p3.getDistance(left).getMeters(), delta);
    	assertEquals(p4.getDistanceToLine(left, right).getMeters(), p4.getDistance(right).scale(-1).getMeters(), delta);
    	assertEquals(p1.getDistanceToLine(p3, p4).getMeters(), 0, delta);
    	assertEquals(Math.abs(p4.getDistanceToLine(p1, p3).getMeters()), p4.getDistance(p1).getMeters(), delta);
    	assertTrue(right.getDistanceToLine(p3, p4).getMeters()>0);
    	assertTrue(left.getDistanceToLine(p3, p4).getMeters()<0);
    	assertEquals(p6.getDistanceToLine(p1, p4).getMeters(), p6.getDistance(p1).scale(-1).getMeters(), delta);
    	assertEquals(right.getDistanceToLine(p1, p6).getMeters(), right.getDistance(p1).getMeters(), delta);
    }
    
    @Test
    public void testQuickApproximateDistance1() {
        Position p1 = new DegreePosition(0, 0);
        Position p2 = new DegreePosition(1, 0);
        assertEquals(60., p1.getQuickApproximateNauticalMileDistance(p2), 0.001);
    }

    @Test
    public void testQuickApproximateDistance2() {
        Position p1 = new DegreePosition(0, 0);
        Position p2 = new DegreePosition(0, 1);
        assertEquals(60., p1.getQuickApproximateNauticalMileDistance(p2), 0.001);
    }

    @Test
    public void testQuickApproximateDistanceDiagonalNearEquator() {
        Position p1 = new DegreePosition(0, 0);
        Position p2 = new DegreePosition(0.1, 0.1);
        assertEquals(p1.getDistance(p2).getNauticalMiles(), p1.getQuickApproximateNauticalMileDistance(p2), 0.01 * p1.getDistance(p2).getNauticalMiles());
    }

    @Test
    public void testQuickApproximateDistanceDiagonalFarFromEquator() {
        Position p1 = new DegreePosition(49, 8);
        Position p2 = new DegreePosition(50, 9);
        assertEquals(p1.getDistance(p2).getNauticalMiles(), p1.getQuickApproximateNauticalMileDistance(p2), 0.01 * p1.getDistance(p2).getNauticalMiles());
    }

    @Test
    public void testQuickApproximateLongDistanceDiagonalFarFromEquator() {
        Position p1 = new DegreePosition(20, -4);
        Position p2 = new DegreePosition(50, 9);
        assertEquals(p1.getDistance(p2).getNauticalMiles(), p1.getQuickApproximateNauticalMileDistance(p2), 0.01 * p1.getDistance(p2).getNauticalMiles());
    }
    
    @Test
    public void performanceComparisonQuickAndFullDistance() {
        Position p1 = new DegreePosition(20, -4);
        Position p2 = new DegreePosition(50, 9);
        long startFull = System.currentTimeMillis();
        int numberOfExecutions = 1000000;
        for (int i=0; i<numberOfExecutions; i++) {
            p1.getDistance(p2);
        }
        long endFull = System.currentTimeMillis();
        for (int i=0; i<numberOfExecutions; i++) {
            p1.getQuickApproximateNauticalMileDistance(p2);
        }
        long endQuick = System.currentTimeMillis();
        System.out.println("Full: "+(endFull-startFull)+"ms; Quick: "+(endQuick-endFull)+"ms");
    }
    
    @Test
    public void noOpRotationAndTranslation() {
        Position p1 = new DegreePosition(1, 1);
        Position unchanged = p1.getLocalCoordinates(new DegreePosition(0, 0), new DegreeBearingImpl(90));
        assertEquals(1., unchanged.getLatDeg(), 0.0000001);
        assertEquals(1., unchanged.getLngDeg(), 0.0000001);
    }

    /**
     * The new coordinate system has its center at (0, 0) and an equator running north from there. Relative to this new
     * coordinate system, the position (0, 1) will seem 1deg south of this new local equator, exactly south of the (0,
     * 0) position. Therefore, the result of {@link Position#getLocalCoordinates(Position, Bearing)} is expected to be
     * (-1, 0).
     */
    @Test
    public void simpleRotation() {
        Position p1 = new DegreePosition(0, 1);
        Position rotatedBy90DegreesCounterClockWise = p1.getLocalCoordinates(new DegreePosition(0, 0), new DegreeBearingImpl(0));
        assertEquals(-1, rotatedBy90DegreesCounterClockWise.getLatDeg(), 0.000001);
        assertEquals(0, rotatedBy90DegreesCounterClockWise.getLngDeg(), 0.000001);
    }

    @Test
    public void oddRotation() {
        Position p1 = new DegreePosition(0, 1);
        Position rotatedBy90DegreesCounterClockWise = p1.getLocalCoordinates(new DegreePosition(0, 0), new DegreeBearingImpl(45));
        assertEquals(-Math.sqrt(1./2.), rotatedBy90DegreesCounterClockWise.getLatDeg(), 0.001);
        assertEquals(Math.sqrt(1./2.), rotatedBy90DegreesCounterClockWise.getLngDeg(), 0.001);
    }

    @Test
    public void testStringRepresentation1() {
        Position p = new DegreePosition(0, 0);
        assertEquals("N00°00.000' E000°00.000'", p.getAsDegreesAndDecimalMinutesWithCardinalPoints());
    }

    @Test
    public void testStringRepresentation2() {
        Position p = new DegreePosition(1, 1);
        assertEquals("N01°00.000' E001°00.000'", p.getAsDegreesAndDecimalMinutesWithCardinalPoints());
    }

    @Test
    public void testStringRepresentation3() {
        Position p = new DegreePosition(-1, -1);
        assertEquals("S01°00.000' W001°00.000'", p.getAsDegreesAndDecimalMinutesWithCardinalPoints());
    }

    @Test
    public void testStringRepresentation4() {
        Position p = new DegreePosition(-11, -11);
        assertEquals("S11°00.000' W011°00.000'", p.getAsDegreesAndDecimalMinutesWithCardinalPoints());
    }

    @Test
    public void testStringRepresentation5() {
        Position p = new DegreePosition(-11, -111);
        assertEquals("S11°00.000' W111°00.000'", p.getAsDegreesAndDecimalMinutesWithCardinalPoints());
    }

    @Test
    public void testStringRepresentation6() {
        Position p = new DegreePosition(-11.0 - 1.05 / 60.0, -111.0 - 1.05 / 60.0);
        assertEquals("S11°01.050' W111°01.050'", p.getAsDegreesAndDecimalMinutesWithCardinalPoints());
    }

    @Test
    public void testStringRepresentation7() {
        Position p = new DegreePosition(1.5, 1.5);
        assertEquals("N01°30.000' E001°30.000'", p.getAsDegreesAndDecimalMinutesWithCardinalPoints());
    }
    
    @Test
    public void testStringRepresentation8() {
        Position p = new DegreePosition(59.999999999/60.0, 59.999999999/60.0);
        assertEquals("N01°00.000' E001°00.000'", p.getAsDegreesAndDecimalMinutesWithCardinalPoints());
    }

    @Test
    public void testStringRepresentation9() {
        Position p = new DegreePosition(54.569283, 10.005400);
        assertEquals("N54°34.157' E010°00.324'", p.getAsDegreesAndDecimalMinutesWithCardinalPoints());
    }

    @Test
    public void testStringRepresentation10() {
        Position p = new DegreePosition(53.569127762780646, 10.004243641469529);
        assertEquals("N53°34.148' E010°00.255'", p.getAsDegreesAndDecimalMinutesWithCardinalPoints());
    }
}
