package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.impl.PositionWithConfidenceImpl;
import com.sap.sailing.domain.common.BearingCluster;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.confidence.BearingWithConfidence;
import com.sap.sailing.domain.common.confidence.BearingWithConfidenceCluster;
import com.sap.sailing.domain.common.confidence.ConfidenceBasedAverager;
import com.sap.sailing.domain.common.confidence.ConfidenceFactory;
import com.sap.sailing.domain.common.confidence.HasConfidence;
import com.sap.sailing.domain.common.confidence.impl.BearingWithConfidenceImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.RadianBearingImpl;
import com.sap.sailing.domain.common.scalablevalue.impl.ScalablePosition;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class BearingTest {
    @Test
    public void testDegreeBearingsGreaterThan360Deg() {
        Bearing bearing = new DegreeBearingImpl(355);
        assertEquals(355, bearing.getDegrees(), 0.000000001);
        Bearing bearing2 = new DegreeBearingImpl(365);
        assertEquals(5, bearing2.getDegrees(), 0.000000001);
    }

    @Test
    public void testRadianBearingsGreaterThan360Deg() {
        Bearing bearing = new RadianBearingImpl(355./180.*Math.PI);
        assertEquals(355, bearing.getDegrees(), 0.000000001);
        Bearing bearing2 = new RadianBearingImpl(365./180.*Math.PI);
        assertEquals(5, bearing2.getDegrees(), 0.000000001);
    }

    @Test
    public void testEmptyBearingWithConfidenceClusterHasNullAverage() {
        assertNull(new BearingWithConfidenceCluster<Void>(null).getAverage(null));
    }
    
    @Test
    public void testNegativeAngle() {
        assertEquals(-10., new DegreeBearingImpl(-10.).getDegrees(), 0.000001);
    }
    
    @Test
    public void testNegativeAngleOverflow() {
        assertEquals(-10., new DegreeBearingImpl(-370.).getDegrees(), 0.000001);
    }
    
    @Test
    public void testZeroConfidenceLeadsToNullBearingAverage() {
        BearingWithConfidenceCluster<Void> cluster = new BearingWithConfidenceCluster<Void>(null);
        cluster.add(new BearingWithConfidenceImpl<Void>(new DegreeBearingImpl(355), /* confidence */ 0., null));
        assertNull(cluster.getAverage(null));
    }
    
    @Test
    public void testZeroConfidenceLeadsToNullPositionAverage() {
        ConfidenceBasedAverager<ScalablePosition, Position, Void> averager = ConfidenceFactory.INSTANCE.createAverager(null);
        HasConfidence<ScalablePosition, Position, Void> average = averager.getAverage(
                Collections.singleton(new PositionWithConfidenceImpl<Void>(new DegreePosition(123, 12), /* confidence */
                        0.0, null)), null);
        assertNull(average.getObject());
    }
    
    @Test
    public void testBearingWithConfidenceClusterSplit() {
        BearingWithConfidenceCluster<Void> cluster = new BearingWithConfidenceCluster<Void>(/* weigher */ null);
        for (double bearingInDegrees : new double[] { 32.31650532600039, 16.99636033752683, 37.59302174779672,
                27.2860810183163, 319.47157698009613, 325.1617832132204, 31.678409742672212, 35.00547108150359,
                23.934778873669256, 29.76599976685808, 33.19487072661667, 19.0, 33.29318052266396, 32.7371445230587,
                38.26627143611533 }) {
            cluster.add(new BearingWithConfidenceImpl<Void>(new DegreeBearingImpl(bearingInDegrees), /* confidence */ 0.9, /* relativeTo */ null));
        }
        BearingWithConfidenceCluster<Void>[] splitResult = cluster.splitInTwo(/* minimumDegreeDifferenceBetweenTacks */ 15, null);
        assertEquals(2, splitResult.length);
        assertNotNull(splitResult[0]);
        assertNotNull(splitResult[1]);
        assertFalse(splitResult[0].isEmpty());
        assertFalse(splitResult[1].isEmpty());
        assertEquals(322, splitResult[0].getAverage(null).getObject().getDegrees(), 1);
        assertEquals(30, splitResult[1].getAverage(null).getObject().getDegrees(), 1);
    }
    
    @Test
    public void testBearingWithConfidenceClusterAverageAcrossZero() {
        BearingWithConfidenceCluster<Void> cluster = new BearingWithConfidenceCluster<Void>(/* weigher */ null);
        BearingWithConfidence<Void> b1 = new BearingWithConfidenceImpl<Void>(new DegreeBearingImpl(355), /* confidence */ 0.9, null);
        BearingWithConfidence<Void> b2 = new BearingWithConfidenceImpl<Void>(new DegreeBearingImpl(5), /* confidence */ 0.9, null);
        cluster.add(b1);
        cluster.add(b2);
        Bearing average = cluster.getAverage(null).getObject();
        assertEquals(0, average.getDegrees(), 0.00000001);
    }
    
    @Test
    public void testBearingWithConfidenceClusterAverageWithZeroSinus() {
        BearingWithConfidenceCluster<Void> cluster = new BearingWithConfidenceCluster<Void>(/* weigher */ null);
        BearingWithConfidence<Void> b1 = new BearingWithConfidenceImpl<Void>(new DegreeBearingImpl(0), /* confidence */ 0.9, null);
        BearingWithConfidence<Void> b2 = new BearingWithConfidenceImpl<Void>(new DegreeBearingImpl(0), /* confidence */ 0.9, null);
        cluster.add(b1);
        cluster.add(b2);
        Bearing average = cluster.getAverage(null).getObject();
        assertEquals(0, average.getDegrees(), 0.00000001);
    }
    
    @Test
    public void testBearingWithConfidenceClusterAverageWithZeroSinusAndNegativeCosinus() {
        BearingWithConfidenceCluster<Void> cluster = new BearingWithConfidenceCluster<Void>(/* weigher */ null);
        BearingWithConfidence<Void> b1 = new BearingWithConfidenceImpl<Void>(new DegreeBearingImpl(180), /* confidence */ 0.9, null);
        BearingWithConfidence<Void> b2 = new BearingWithConfidenceImpl<Void>(new DegreeBearingImpl(180), /* confidence */ 0.9, null);
        cluster.add(b1);
        cluster.add(b2);
        Bearing average = cluster.getAverage(null).getObject();
        assertEquals(180, average.getDegrees(), 0.00000001);
    }
    
    @Test
    public void testBearingWithConfidenceClusterAverageWithZeroCosinus() {
        BearingWithConfidenceCluster<Void> cluster = new BearingWithConfidenceCluster<Void>(/* weigher */ null);
        BearingWithConfidence<Void> b1 = new BearingWithConfidenceImpl<Void>(new DegreeBearingImpl(90), /* confidence */ 0.9, null);
        BearingWithConfidence<Void> b2 = new BearingWithConfidenceImpl<Void>(new DegreeBearingImpl(90), /* confidence */ 0.9, null);
        cluster.add(b1);
        cluster.add(b2);
        Bearing average = cluster.getAverage(null).getObject();
        assertEquals(90, average.getDegrees(), 0.00000001);
    }
    
    @Test
    public void testBearingWithConfidenceClusterAverageWithZeroCosinusAndNegativeSinus() {
        BearingWithConfidenceCluster<Void> cluster = new BearingWithConfidenceCluster<Void>(/* weigher */ null);
        BearingWithConfidence<Void> b1 = new BearingWithConfidenceImpl<Void>(new DegreeBearingImpl(270), /* confidence */ 0.9, null);
        BearingWithConfidence<Void> b2 = new BearingWithConfidenceImpl<Void>(new DegreeBearingImpl(270), /* confidence */ 0.9, null);
        cluster.add(b1);
        cluster.add(b2);
        Bearing average = cluster.getAverage(null).getObject();
        assertEquals(270, average.getDegrees(), 0.00000001);
    }
    
    // ---------------- Classical BearingCluster -----------------
    
    @Test
    public void testEmptyClusterHasNullAverage() {
        assertNull(new BearingCluster().getAverage());
    }
    
    @Test
    public void testBearingClusterSplit() {
        BearingCluster cluster = new BearingCluster();
        for (double bearingInDegrees : new double[] { 32.31650532600039, 16.99636033752683, 37.59302174779672,
                27.2860810183163, 319.47157698009613, 325.1617832132204, 31.678409742672212, 35.00547108150359,
                23.934778873669256, 29.76599976685808, 33.19487072661667, 19.0, 33.29318052266396, 32.7371445230587,
                38.26627143611533 }) {
            cluster.add(new DegreeBearingImpl(bearingInDegrees));
        }
        BearingCluster[] splitResult = cluster.splitInTwo(/* minimumDegreeDifferenceBetweenTacks */ 15);
        assertEquals(2, splitResult.length);
        assertNotNull(splitResult[0]);
        assertNotNull(splitResult[1]);
        assertFalse(splitResult[0].isEmpty());
        assertFalse(splitResult[1].isEmpty());
        assertEquals(322, splitResult[0].getAverage().getDegrees(), 1);
        assertEquals(30, splitResult[1].getAverage().getDegrees(), 1);
    }
    
    @Test
    public void testBearingClusterAverageAcrossZero() {
        BearingCluster cluster = new BearingCluster();
        Bearing b1 = new DegreeBearingImpl(355);
        Bearing b2 = new DegreeBearingImpl(5);
        cluster.add(b1);
        cluster.add(b2);
        Bearing average = cluster.getAverage();
        assertEquals(0, average.getDegrees(), 0.00000001);
    }
    
    @Test
    public void testBearingClusterAverageWithZeroSinus() {
        BearingCluster cluster = new BearingCluster();
        Bearing b1 = new DegreeBearingImpl(0);
        Bearing b2 = new DegreeBearingImpl(0);
        cluster.add(b1);
        cluster.add(b2);
        Bearing average = cluster.getAverage();
        assertEquals(0, average.getDegrees(), 0.00000001);
    }
    
    @Test
    public void testBearingClusterAverageWithZeroSinusAndNegativeCosinus() {
        BearingCluster cluster = new BearingCluster();
        Bearing b1 = new DegreeBearingImpl(180);
        Bearing b2 = new DegreeBearingImpl(180);
        cluster.add(b1);
        cluster.add(b2);
        Bearing average = cluster.getAverage();
        assertEquals(180, average.getDegrees(), 0.00000001);
    }
    
    @Test
    public void testBearingClusterAverageWithZeroCosinus() {
        BearingCluster cluster = new BearingCluster();
        Bearing b1 = new DegreeBearingImpl(90);
        Bearing b2 = new DegreeBearingImpl(90);
        cluster.add(b1);
        cluster.add(b2);
        Bearing average = cluster.getAverage();
        assertEquals(90, average.getDegrees(), 0.00000001);
    }
    
    @Test
    public void testBearingClusterAverageWithZeroCosinusAndNegativeSinus() {
        BearingCluster cluster = new BearingCluster();
        Bearing b1 = new DegreeBearingImpl(270);
        Bearing b2 = new DegreeBearingImpl(270);
        cluster.add(b1);
        cluster.add(b2);
        Bearing average = cluster.getAverage();
        assertEquals(270, average.getDegrees(), 0.00000001);
    }
    
    @Test
    public void testDoubleModulo() {
        final double angle1 = 12.34;
        final double angle2 = 362.45;
        assertEquals(14.79, (angle1 + angle2) % 360., 0.00000000001);
    }
}
