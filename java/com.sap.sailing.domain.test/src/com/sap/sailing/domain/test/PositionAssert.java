package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Set;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;

/**
 * Contains equality assertions for "compact" fixes, allowing for deltas to be specified, assuming
 * that the "compact" version of a fix may vary slightly from its original.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class PositionAssert {
    public static void assertPositionEquals(Position p1, Position p2, double degreeDelta) {
        assertEquals(p1.getLatDeg(), p2.getLatDeg(), degreeDelta);
        assertEquals(p1.getLngDeg(), p2.getLngDeg(), degreeDelta);
    }

    public static void assertGPSFixEquals(GPSFixMoving f1, GPSFixMoving f2, double positionDegreeDelta, double bearingDegreeDelta, double knotDelta) {
        assertGPSFixEquals((GPSFix) f1, (GPSFix) f2, positionDegreeDelta);
        assertSpeedEquals(f1.getSpeed(), f2.getSpeed(), bearingDegreeDelta, knotDelta);
    }

    public static void assertBearingEquals(Bearing b1, Bearing b2, double bearingDegreeDelta) {
        assertEquals(b1.getDegrees(), b2.getDegrees(), bearingDegreeDelta);
    }

    public static void assertGPSFixEquals(GPSFix f1, GPSFix f2, double positionDegreeDelta) {
        assertPositionEquals(f1.getPosition(), f2.getPosition(), positionDegreeDelta);
        assertEquals(f1.getTimePoint(), f2.getTimePoint());
    }

    public static void assertWindEquals(Wind w1, Wind w2, double posDegDelta, double bearingDegreeDelta, double knotSpeedDelta) {
        assertEquals(w1.getTimePoint(), w2.getTimePoint());
        assertPositionEquals(w1.getPosition(), w2.getPosition(), posDegDelta);
        assertBearingEquals(w1.getBearing(), w2.getBearing(), bearingDegreeDelta);
        assertSpeedEquals(w1, w2, bearingDegreeDelta, knotSpeedDelta);
    }

    public static void assertSpeedEquals(Speed s1, Speed s2, double knotSpeedDelta) {
        assertEquals(s1.getKnots(), s2.getKnots(), knotSpeedDelta);
    }

    public static void assertSpeedEquals(SpeedWithBearing s1, SpeedWithBearing s2, double bearingDegreeDelta, double knotSpeedDelta) {
        assertBearingEquals(s1.getBearing(), s2.getBearing(), bearingDegreeDelta);
        assertSpeedEquals((Speed) s1, (Speed) s2, knotSpeedDelta);
    }

    public static void assertWindEquals(Set<Wind> expectedWinds, Set<Wind> actualWinds, double posDegDelta,
            double bearingDegreeDelta, double knotSpeedDelta) {
        for (final Wind expectedWind : expectedWinds) {
            boolean found = false;
            for (final Wind actualWind : actualWinds) {
                try {
                    assertWindEquals(expectedWind, actualWind, posDegDelta, bearingDegreeDelta, knotSpeedDelta);
                    found = true;
                    break;
                } catch (AssertionError e) {
                    // not equals
                }
            }
            if (!found) {
                fail("Expected to find "+expectedWind+" in "+actualWinds+" but didn't");
            }
        }
    }
}
