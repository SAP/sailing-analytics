package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.PositionWithConfidence;
import com.sap.sailing.domain.base.impl.PositionWithConfidenceImpl;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.confidence.BearingWithConfidence;
import com.sap.sailing.domain.common.confidence.BearingWithConfidenceCluster;
import com.sap.sailing.domain.common.confidence.ConfidenceBasedAverager;
import com.sap.sailing.domain.common.confidence.HasConfidence;
import com.sap.sailing.domain.common.confidence.HasConfidenceAndIsScalable;
import com.sap.sailing.domain.common.confidence.Weigher;
import com.sap.sailing.domain.common.confidence.impl.BearingWithConfidenceImpl;
import com.sap.sailing.domain.common.confidence.impl.ScalableDoubleWithConfidence;
import com.sap.sailing.domain.common.confidence.impl.ScalableWind;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.impl.RadianBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.scalablevalue.impl.ScalablePosition;
import com.sap.sailing.domain.confidence.ConfidenceBasedWindAverager;
import com.sap.sailing.domain.confidence.ConfidenceFactory;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.WindWithConfidenceImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.scalablevalue.ScalableValue;

public class ConfidenceTest {
    private static class ScalableBearing implements ScalableValue<ScalableBearing, Bearing> {
        private final double sin;
        private final double cos;
        
        public ScalableBearing(Bearing bearing) {
            this.sin = Math.sin(bearing.getRadians());
            this.cos = Math.cos(bearing.getRadians());
        }
        
        private ScalableBearing(double sin, double cos) {
            this.sin = sin;
            this.cos = cos;
        }
        
        @Override
        public ScalableValue<ScalableBearing, Bearing> multiply(double factor) {
            return new ScalableBearing(factor*getValue().sin, factor*getValue().cos);
        }

        @Override
        public ScalableValue<ScalableBearing, Bearing> add(ScalableValue<ScalableBearing, Bearing> t) {
            ScalableBearing value = getValue();
            ScalableBearing tValue = t.getValue();
            return new ScalableBearing(value.sin+tValue.sin, value.cos+tValue.cos);
        }

        @Override
        public Bearing divide(double divisor) {
            double angle;
            if (cos == 0) {
                angle = sin >= 0 ? Math.PI / 2 : -Math.PI / 2;
            } else {
                angle = Math.atan2(sin, cos);
            }
            Bearing result = new RadianBearingImpl(angle < 0 ? angle + 2 * Math.PI : angle);
            return result;
        }

        @Override
        public ScalableBearing getValue() {
            return this;
        }
    }
    
    private static class ScalableBearingWithConfidence<RelativeTo> extends ScalableBearing
    implements HasConfidenceAndIsScalable<ScalableBearing, Bearing, RelativeTo> {
        private static final long serialVersionUID = 6609861367777720855L;
        private final double confidence;
        private final RelativeTo relativeTo;
        private final Bearing bearing;
        
        public ScalableBearingWithConfidence(Bearing bearing, double confidence, RelativeTo relativeTo) {
            super(bearing);
            this.confidence = confidence;
            this.bearing = bearing;
            this.relativeTo = relativeTo;
        }

        @Override
        public Bearing getObject() {
            return bearing;
        }

        @Override
        public double getConfidence() {
            return confidence;
        }

        @Override
        public ScalableBearingWithConfidence<RelativeTo> getScalableValue() {
            return this;
        }
        
        @Override
        public RelativeTo getRelativeTo() {
            return relativeTo;
        }
    }
    
    @Test
    public void testBearingClusterSplittingWithDifferentBearingsOrdering() {
        TimePoint timePoint = new MillisecondsTimePoint(1308839544250l);
        BearingWithConfidenceCluster<TimePoint> clusterA = new BearingWithConfidenceCluster<TimePoint>(ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(
        // use a minimum confidence to avoid the bearing to flip to 270deg in case all is zero
                /* milliseconds over which to average */ 30000l, /* minimum confidence */ 0.0000000001));
        for (String a : new String[] {
                "87.0@0.3561978879735175",
                "286.8716453147824@0.7507926558478147",
                "282.55627120464703@0.7643492902545556",
                "286.8605698949788@0.7483842322506868",
                "291.5836361697427@0.7491852169449421",
                "297.0631828865192@0.7488453128197485",
                "283.6400613098378@0.7488453128197485",
                "279.95201024864554@0.7298408190555351",
                "279.77216720379766@0.7283443881177472",
                "283.75770067567913@0.7491852169449421",
                "284.30394138063696@0.7488453128197485",
                "285.5253164529858@0.7491852169449421"
        }) {
            BearingWithConfidence<TimePoint> bearingWithConfidence = parseBearingWithConfidence(a);
            clusterA.add(bearingWithConfidence);
        }
        BearingWithConfidenceCluster<TimePoint>[] splitResultA = clusterA.splitInTwo(45.0, timePoint);
        BearingWithConfidenceCluster<TimePoint> clusterB = new BearingWithConfidenceCluster<TimePoint>(ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(
        // use a minimum confidence to avoid the bearing to flip to 270deg in case all is zero
                /* milliseconds over which to average */ 30000l, /* minimum confidence */ 0.0000000001));
        for (String b : new String[] {
                "282.55627120464703@0.7643492902545556",
                "286.8716453147824@0.7507926558478147",
                "286.8605698949788@0.7483842322506868",
                "285.5253164529858@0.7491852169449421",
                "283.75770067567913@0.7491852169449421",
                "283.6400613098378@0.7488453128197485",
                "279.95201024864554@0.7298408190555351",
                "291.5836361697427@0.7491852169449421",
                "297.0631828865192@0.7488453128197485",
                "279.77216720379766@0.7283443881177472",
                "284.30394138063696@0.7488453128197485",
                "87.0@0.3561978879735175"
        }) {
            BearingWithConfidence<TimePoint> bearingWithConfidence = parseBearingWithConfidence(b);
            clusterB.add(bearingWithConfidence);
        }
        BearingWithConfidenceCluster<TimePoint>[] splitResultB = clusterB.splitInTwo(45.0, timePoint);
        assertEquals(11, Math.max(splitResultA[0].size(), splitResultA[1].size()));
        assertEquals(1, Math.min(splitResultA[0].size(), splitResultA[1].size()));
        assertEquals(11, Math.max(splitResultB[0].size(), splitResultB[1].size()));
        assertEquals(1, Math.min(splitResultB[0].size(), splitResultB[1].size()));
        assertEquals((splitResultA[0].size()<splitResultA[1].size()?splitResultA[0]:splitResultA[1]).getAverage(timePoint).getObject().getDegrees(),
                (splitResultB[0].size()<splitResultB[1].size()?splitResultB[0]:splitResultB[1]).getAverage(timePoint).getObject().getDegrees(), 0.0000001);
    }
    
    private BearingWithConfidence<TimePoint> parseBearingWithConfidence(String a) {
        String[] bearingAndConfidence = a.split("@");
        double degBearing = Double.valueOf(bearingAndConfidence[0]);
        double confidence = Double.valueOf(bearingAndConfidence[1]);
        return new BearingWithConfidenceImpl<TimePoint>(new DegreeBearingImpl(degBearing), confidence, new MillisecondsTimePoint(1308839544250l));
    }

    @Test
    public void testLinearWeigherHalfTime() {
        Weigher<TimePoint> w = ConfidenceFactory.INSTANCE.createHyperbolicTimeDifferenceWeigher(1000);
        double confidence = w.getConfidence(new MillisecondsTimePoint(1000), new MillisecondsTimePoint(0));
        assertEquals(0.5, confidence, 0.00000001);
    }
    
    @Test
    public void testLinearWeigherZeroTimeDifference() {
        Weigher<TimePoint> w = ConfidenceFactory.INSTANCE.createHyperbolicTimeDifferenceWeigher(1000);
        double confidence = w.getConfidence(new MillisecondsTimePoint(1000), new MillisecondsTimePoint(1000));
        assertEquals(1.0, confidence, 0.00000001);
    }
    
    @Test
    public void testAveragingWithEmptyListYieldsNull() {
        ConfidenceBasedAverager<Double, Double, TimePoint> averager = ConfidenceFactory.INSTANCE.createAverager(null);
        Set<HasConfidenceAndIsScalable<Double, Double, TimePoint>> emptySet = Collections.emptySet();
        HasConfidence<Double, Double, TimePoint> average = averager.getAverage(emptySet, null);
        assertNull(average);
    }

    @Test
    public void testAveragingWithNullArrayYieldsNull() {
        ConfidenceBasedAverager<Double, Double, TimePoint> averager = ConfidenceFactory.INSTANCE.createAverager(null);
        HasConfidence<Double, Double, TimePoint> average = averager.getAverage((Iterable<? extends HasConfidenceAndIsScalable<Double, Double, TimePoint>>) null, null);
        assertNull(average);
    }

    @Test
    public void testAveragingWithTwoWindsOneOfWhichHasUseSpeedFalse() {
        WindWithConfidence<TimePoint> d1 = new WindWithConfidenceImpl<TimePoint>(new WindImpl(new DegreePosition(0, 0),
                new MillisecondsTimePoint(0), new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(90))), /* confidence */
                0.5, /* relativeTo */new MillisecondsTimePoint(0), /* useSpeed */ true);
        WindWithConfidence<TimePoint> d2 = new WindWithConfidenceImpl<TimePoint>(new WindImpl(new DegreePosition(1, 0),
                new MillisecondsTimePoint(20), new KnotSpeedWithBearingImpl(20, new DegreeBearingImpl(180))), /* confidence */
                0.5, /* relativeTo */new MillisecondsTimePoint(20), /* useSpeed */ false);
        ConfidenceBasedWindAverager<TimePoint> averager = ConfidenceFactory.INSTANCE
                .createWindAverager(ConfidenceFactory.INSTANCE.createHyperbolicTimeDifferenceWeigher(1000));
        List<WindWithConfidence<TimePoint>> list = Arrays.asList(d1, d2);
        HasConfidence<ScalableWind, Wind, TimePoint> average = averager.getAverage(list, new MillisecondsTimePoint(10));
        assertEquals(0.5, average.getObject().getPosition().getLatDeg(), 0.00000001);
        assertEquals(0.0, average.getObject().getPosition().getLngDeg(), 0.00000001);
        assertEquals(10, average.getObject().getTimePoint().asMillis());
        assertEquals(10, average.getObject().getKnots(), 0.00000001);
        assertEquals(135, average.getObject().getBearing().getDegrees(), 0.00000001);
    }

    @Test
    public void testAveragingOneWindWithUseSpeedFalse() {
        WindWithConfidence<TimePoint> d1 = new WindWithConfidenceImpl<TimePoint>(new WindImpl(new DegreePosition(1, 0),
                new MillisecondsTimePoint(20), new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(180))), /* confidence */
                0.5, /* relativeTo */new MillisecondsTimePoint(20), /* useSpeed */ false);
        ConfidenceBasedWindAverager<TimePoint> averager = ConfidenceFactory.INSTANCE
                .createWindAverager(ConfidenceFactory.INSTANCE.createHyperbolicTimeDifferenceWeigher(1000));
        List<WindWithConfidence<TimePoint>> list = Arrays.asList(d1);
        HasConfidence<ScalableWind, Wind, TimePoint> average = averager.getAverage(list, new MillisecondsTimePoint(20));
        assertEquals(1.0, average.getObject().getPosition().getLatDeg(), 0.00000001);
        assertEquals(0.0, average.getObject().getPosition().getLngDeg(), 0.00000001);
        assertEquals(20, average.getObject().getTimePoint().asMillis());
        assertEquals(10, average.getObject().getKnots(), 0.00000001);
        assertEquals(180, average.getObject().getBearing().getDegrees(), 0.00000001);
    }

    @Test
    public void testAveragingWithFourWindsTwoOfWhichHaveUseSpeedFalse() {
        WindWithConfidence<TimePoint> d1 = new WindWithConfidenceImpl<TimePoint>(new WindImpl(new DegreePosition(1, 0),
                new MillisecondsTimePoint(20), new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(180))), /* confidence */
                0.5, /* relativeTo */new MillisecondsTimePoint(20), /* useSpeed */ true);
        WindWithConfidence<TimePoint> d2 = new WindWithConfidenceImpl<TimePoint>(new WindImpl(new DegreePosition(1, 0),
                new MillisecondsTimePoint(20), new KnotSpeedWithBearingImpl(20, new DegreeBearingImpl(180))), /* confidence */
                0.5, /* relativeTo */new MillisecondsTimePoint(20), /* useSpeed */ true);
        WindWithConfidence<TimePoint> d3 = new WindWithConfidenceImpl<TimePoint>(new WindImpl(new DegreePosition(1, 0),
                new MillisecondsTimePoint(20), new KnotSpeedWithBearingImpl(30, new DegreeBearingImpl(180))), /* confidence */
                0.5, /* relativeTo */new MillisecondsTimePoint(20), /* useSpeed */ false);
        WindWithConfidence<TimePoint> d4 = new WindWithConfidenceImpl<TimePoint>(new WindImpl(new DegreePosition(1, 0),
                new MillisecondsTimePoint(20), new KnotSpeedWithBearingImpl(40, new DegreeBearingImpl(180))), /* confidence */
                0.5, /* relativeTo */new MillisecondsTimePoint(20), /* useSpeed */ false);
        ConfidenceBasedWindAverager<TimePoint> averager = ConfidenceFactory.INSTANCE
                .createWindAverager(ConfidenceFactory.INSTANCE.createHyperbolicTimeDifferenceWeigher(1000));
        List<WindWithConfidence<TimePoint>> list = Arrays.asList(d1, d2, d3, d4);
        HasConfidence<ScalableWind, Wind, TimePoint> average = averager.getAverage(list, new MillisecondsTimePoint(20));
        assertEquals(1.0, average.getObject().getPosition().getLatDeg(), 0.00000001);
        assertEquals(0.0, average.getObject().getPosition().getLngDeg(), 0.00000001);
        assertEquals(20, average.getObject().getTimePoint().asMillis());
        assertEquals(15, average.getObject().getKnots(), 0.00000001);
        assertEquals(180, average.getObject().getBearing().getDegrees(), 0.00000001);
    }

    @Test
    public void testAveragingWithTwoWinds() {
        WindWithConfidence<TimePoint> d1 = new WindWithConfidenceImpl<TimePoint>(new WindImpl(new DegreePosition(0, 0),
                new MillisecondsTimePoint(0), new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(90))), /* confidence */
                0.5, /* relativeTo */new MillisecondsTimePoint(0), /* useSpeed */ true);
        WindWithConfidence<TimePoint> d2 = new WindWithConfidenceImpl<TimePoint>(new WindImpl(new DegreePosition(1, 0),
                new MillisecondsTimePoint(20), new KnotSpeedWithBearingImpl(20, new DegreeBearingImpl(180))), /* confidence */
                0.5, /* relativeTo */new MillisecondsTimePoint(20), /* useSpeed */ true);
        ConfidenceBasedWindAverager<TimePoint> averager = ConfidenceFactory.INSTANCE
                .createWindAverager(ConfidenceFactory.INSTANCE.createHyperbolicTimeDifferenceWeigher(1000));
        List<WindWithConfidence<TimePoint>> list = Arrays.asList(d1, d2);
        HasConfidence<ScalableWind, Wind, TimePoint> average = averager.getAverage(list, new MillisecondsTimePoint(10));
        assertEquals(0.5, average.getObject().getPosition().getLatDeg(), 0.00000001);
        assertEquals(0.0, average.getObject().getPosition().getLngDeg(), 0.00000001);
        assertEquals(10, average.getObject().getTimePoint().asMillis());
        assertEquals(15, average.getObject().getKnots(), 0.00000001);
        assertEquals(135, average.getObject().getBearing().getDegrees(), 0.00000001);
    }

    @Test
    public void testAveragingWithTwoDoubles() {
        ScalableDoubleWithConfidence<TimePoint> d1 = new ScalableDoubleWithConfidence<TimePoint>(1., 0.5, null);
        ScalableDoubleWithConfidence<TimePoint> d2 = new ScalableDoubleWithConfidence<TimePoint>(2., 0.5, null);
        ConfidenceBasedAverager<Double, Double, TimePoint> averager = ConfidenceFactory.INSTANCE.createAverager(null);
        List<ScalableDoubleWithConfidence<TimePoint>> list = Arrays.asList(d1, d2);
        HasConfidence<Double, Double, TimePoint> average = averager.getAverage(list, null);
        assertEquals(1.5, average.getObject(), 0.00000001);
    }

    @Test
    public void testAveragingWithThreeDoubles() {
        ScalableDoubleWithConfidence<TimePoint> d1 = new ScalableDoubleWithConfidence<TimePoint>(1., 1., null);
        ScalableDoubleWithConfidence<TimePoint> d2 = new ScalableDoubleWithConfidence<TimePoint>(2., 1., null);
        ScalableDoubleWithConfidence<TimePoint> d3 = new ScalableDoubleWithConfidence<TimePoint>(3., 2., null);
        ConfidenceBasedAverager<Double, Double, TimePoint> averager = ConfidenceFactory.INSTANCE.createAverager(null);
        List<ScalableDoubleWithConfidence<TimePoint>> list = Arrays.asList(d1, d2, d3);
        HasConfidence<Double, Double, TimePoint> average = averager.getAverage(list, null);
        assertEquals(2.25, average.getObject(), 0.00000001);
    }
    
    @Test
    public void testAveragingWithTwoBearings() {
        ScalableBearingWithConfidence<TimePoint> d1 = new ScalableBearingWithConfidence<TimePoint>(new DegreeBearingImpl(350.), 1., null);
        ScalableBearingWithConfidence<TimePoint> d2 = new ScalableBearingWithConfidence<TimePoint>(new DegreeBearingImpl(10.), 1., null);
        ConfidenceBasedAverager<ScalableBearing, Bearing, TimePoint> averager = ConfidenceFactory.INSTANCE.createAverager(null);
        List<ScalableBearingWithConfidence<TimePoint>> list = Arrays.asList(d1, d2);
        HasConfidence<ScalableBearing, Bearing, TimePoint> average = averager.getAverage(list, null);
        assertEquals(0, average.getObject().getDegrees(), 0.00000001);
    }
    
    @Test
    public void testAveragingWithThreeBearings() {
        ScalableBearingWithConfidence<TimePoint> d1 = new ScalableBearingWithConfidence<TimePoint>(new DegreeBearingImpl(350.), 1., null);
        ScalableBearingWithConfidence<TimePoint> d2 = new ScalableBearingWithConfidence<TimePoint>(new DegreeBearingImpl(10.), 1., null);
        ScalableBearingWithConfidence<TimePoint> d3 = new ScalableBearingWithConfidence<TimePoint>(new DegreeBearingImpl(20.), 2., null);
        ConfidenceBasedAverager<ScalableBearing, Bearing, TimePoint> averager = ConfidenceFactory.INSTANCE.createAverager(null);
        List<ScalableBearingWithConfidence<TimePoint>> list = Arrays.asList(d1, d2, d3);
        HasConfidence<ScalableBearing, Bearing, TimePoint> average = averager.getAverage(list, null);
        assertEquals(10, average.getObject().getDegrees(), 0.1);
    }

    @Test
    public void testAveragingTwoPositions() {
        PositionWithConfidence<TimePoint> p1 = new PositionWithConfidenceImpl<TimePoint>(new DegreePosition(0, 45), 0.9, null);
        PositionWithConfidence<TimePoint> p2 = new PositionWithConfidenceImpl<TimePoint>(new DegreePosition(0, -45), 0.9, null);
        ConfidenceBasedAverager<ScalablePosition, Position, TimePoint> averager = ConfidenceFactory.INSTANCE.createAverager(null);
        List<PositionWithConfidence<TimePoint>> list = Arrays.asList(p1, p2);
        HasConfidence<ScalablePosition, Position, TimePoint> average = averager.getAverage(list, null);
        assertEquals(0, average.getObject().getLatDeg(), 0.1);
        assertEquals(0, average.getObject().getLngDeg(), 0.1);
    }
    
    @Test
    public void testPositionBasedConfidence() {
        // at 1000 meter distance, confidence should be .5; at distance 0, confidence shall be 1.
        Weigher<Position> weigher = ConfidenceFactory.INSTANCE.createHyperbolicDistanceWeigher(new MeterDistance(1000));
        Position p = new DegreePosition(12, 13);
        assertEquals(1., weigher.getConfidence(p, p), 0.00000001);
        Position p2 = p.translateGreatCircle(new DegreeBearingImpl(123), new MeterDistance(1000));
        assertEquals(.5, weigher.getConfidence(p, p2), 0.001); // somewhat more tolerant; using quick distance in weigher
        Position p3 = p.translateGreatCircle(new DegreeBearingImpl(123), new NauticalMileDistance(1000));
        final double threshold = 0.001;
        assertTrue(weigher.getConfidence(p, p3) < threshold,
                "Expected confidence in 1000nm distance to be less than "+threshold+" but was "+weigher.getConfidence(p, p3));
    }
    
    @Test
    public void testPositionWithConfidenceScalingByOne() {
        assertScaleAndDownscalePosition(new DegreePosition(0, 45), 1.);
    }

    @Test
    public void testPositionWithConfidenceScalingByTwo() {
        assertScaleAndDownscalePosition(new DegreePosition(0, 45), 2.);
        assertScaleAndDownscalePosition(new DegreePosition(45, 0), 2.);
        assertScaleAndDownscalePosition(new DegreePosition(45, 90), 2.);
    }

    private void assertScaleAndDownscalePosition(DegreePosition position, double scale) {
        PositionWithConfidence<TimePoint> p1 = new PositionWithConfidenceImpl<TimePoint>(position, 0.9, null);
        ScalableValue<ScalablePosition, Position> scaledPosition = p1.getScalableValue().multiply(scale);
        Position downscaledPosition = scaledPosition.divide(scale);
        assertEquals(position.getLatDeg(), downscaledPosition.getLatDeg(), 0.000001);
        assertEquals(position.getLngDeg(), downscaledPosition.getLngDeg(), 0.000001);
    }

    @Test
    public void testAveragingTwoPositionsToNorthPole() {
        PositionWithConfidence<TimePoint> p1 = new PositionWithConfidenceImpl<TimePoint>(new DegreePosition(45, 90), 0.9, null);
        PositionWithConfidence<TimePoint> p2 = new PositionWithConfidenceImpl<TimePoint>(new DegreePosition(45, -90), 0.9, null);
        ConfidenceBasedAverager<ScalablePosition, Position, TimePoint> averager = ConfidenceFactory.INSTANCE.createAverager(null);
        List<PositionWithConfidence<TimePoint>> list = Arrays.asList(p1, p2);
        HasConfidence<ScalablePosition, Position, TimePoint> average = averager.getAverage(list, null);
        assertEquals(90, average.getObject().getLatDeg(), 0.1);
        assertEquals(0, average.getObject().getLngDeg(), 0.1);
    }

    @Test
    public void testAveragingThreePositions() {
        PositionWithConfidence<TimePoint> p1 = new PositionWithConfidenceImpl<TimePoint>(new DegreePosition(49, 8), 0.9, null);
        PositionWithConfidence<TimePoint> p2 = new PositionWithConfidenceImpl<TimePoint>(new DegreePosition(49, 9), 0.9, null);
        PositionWithConfidence<TimePoint> p3 = new PositionWithConfidenceImpl<TimePoint>(new DegreePosition(50, 8.5), 0.9, null);
        ConfidenceBasedAverager<ScalablePosition, Position, TimePoint> averager = ConfidenceFactory.INSTANCE.createAverager(null);
        List<PositionWithConfidence<TimePoint>> list = Arrays.asList(p1, p2, p3);
        HasConfidence<ScalablePosition, Position, TimePoint> average = averager.getAverage(list, null);
        assertTrue(average.getObject().getLatDeg() > 49);
        assertTrue(average.getObject().getLatDeg() < 50);
        assertEquals(8.5, average.getObject().getLngDeg(), 0.000000001);
    }

    // --------------- tests including reference time point using exponential confidences ----------------
    @Test
    public void testConfidenceBasedAveragingWithEmptyListYieldsNull() {
        ConfidenceBasedAverager<Double, Double, TimePoint> averager = ConfidenceFactory.INSTANCE
                .createAverager(ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(1000));
        Set<HasConfidenceAndIsScalable<Double, Double, TimePoint>> emptySet = Collections.emptySet();
        HasConfidence<Double, Double, TimePoint> average = averager.getAverage(emptySet, null);
        assertNull(average);
    }

    @Test
    public void testConfidenceBasedAveragingWithNullArrayYieldsNull() {
        ConfidenceBasedAverager<Double, Double, TimePoint> averager = ConfidenceFactory.INSTANCE
                .createAverager(ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(1000));
        HasConfidence<Double, Double, TimePoint> average = averager.getAverage((Iterable<? extends HasConfidenceAndIsScalable<Double, Double, TimePoint>>) null, null);
        assertNull(average);
    }

    @Test
    public void testConfidenceBasedAveragingInTheMiddleOfTwoDoubles() {
        ScalableDoubleWithConfidence<TimePoint> d1 = new ScalableDoubleWithConfidence<TimePoint>(1., 0.5, new MillisecondsTimePoint(1000));
        ScalableDoubleWithConfidence<TimePoint> d2 = new ScalableDoubleWithConfidence<TimePoint>(2., 0.5, new MillisecondsTimePoint(3000));
        ConfidenceBasedAverager<Double, Double, TimePoint> averager = ConfidenceFactory.INSTANCE
                .createAverager(ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(1000));
        List<ScalableDoubleWithConfidence<TimePoint>> list = Arrays.asList(d1, d2);
        HasConfidence<Double, Double, TimePoint> average = averager.getAverage(list, new MillisecondsTimePoint(2000));
        assertEquals(1.5, average.getObject(), 0.00000001);
    }

    @Test
    public void testConfidenceBasedAveragingAtOneOfTwoDoubles() {
        ScalableDoubleWithConfidence<TimePoint> d1 = new ScalableDoubleWithConfidence<TimePoint>(1., 0.5, new MillisecondsTimePoint(1000));
        ScalableDoubleWithConfidence<TimePoint> d2 = new ScalableDoubleWithConfidence<TimePoint>(2., 0.5, new MillisecondsTimePoint(3000));
        Weigher<TimePoint> weigher = ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(1000);
        assertEquals(0.25, weigher.getConfidence(new MillisecondsTimePoint(3000), new MillisecondsTimePoint(1000)), 0.0000001);
        ConfidenceBasedAverager<Double, Double, TimePoint> averager = ConfidenceFactory.INSTANCE
                .createAverager(weigher);
        List<ScalableDoubleWithConfidence<TimePoint>> list = Arrays.asList(d1, d2);
        HasConfidence<Double, Double, TimePoint> average = averager.getAverage(list, new MillisecondsTimePoint(1000));
        assertEquals(1.2, average.getObject(), 0.00000001);
    }

    @Test
    public void testConfidenceBasedAveragingWithThreeDoubles() {
        ScalableDoubleWithConfidence<TimePoint> d1 = new ScalableDoubleWithConfidence<TimePoint>(1., 1.,
                new MillisecondsTimePoint(1000)); // confidence 4/4 when viewed for time point 1000
        ScalableDoubleWithConfidence<TimePoint> d2 = new ScalableDoubleWithConfidence<TimePoint>(2., 1.,
                new MillisecondsTimePoint(2000)); // confidence 2/4 when viewed for time point 1000
        ScalableDoubleWithConfidence<TimePoint> d3 = new ScalableDoubleWithConfidence<TimePoint>(3., 2.,
                new MillisecondsTimePoint(3000)); // confidence 1/4 when viewed for time point 1000
        ConfidenceBasedAverager<Double, Double, TimePoint> averager = ConfidenceFactory.INSTANCE
                .createAverager(ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(1000));
        List<ScalableDoubleWithConfidence<TimePoint>> list = Arrays.asList(d1, d2, d3);
        HasConfidence<Double, Double, TimePoint> average = averager.getAverage(list, new MillisecondsTimePoint(1000));
        assertEquals((1.*4.*1. + 2.*2.*1. + 3.*1.*2.)/8., average.getObject(), 0.00000001);
    }

    @Test
    public void testConfidenceBasedAveragingWithTwoBearings() {
        ScalableBearingWithConfidence<TimePoint> d1 = new ScalableBearingWithConfidence<TimePoint>(
                new DegreeBearingImpl(350.), 1., new MillisecondsTimePoint(1000)); // confidence 2/4 when viewed for time point 2000
        ScalableBearingWithConfidence<TimePoint> d2 = new ScalableBearingWithConfidence<TimePoint>(
                new DegreeBearingImpl(10.), 1., new MillisecondsTimePoint(2000)); // confidence 4/4 when viewed for time point 2000
        ConfidenceBasedAverager<ScalableBearing, Bearing, TimePoint> averager = ConfidenceFactory.INSTANCE
                .createAverager(ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(1000));
        List<ScalableBearingWithConfidence<TimePoint>> list = Arrays.asList(d1, d2);
        HasConfidence<ScalableBearing, Bearing, TimePoint> average = averager.getAverage(list, new MillisecondsTimePoint(2000));
        assertEquals((2.*10+1.*-10.)/3., average.getObject().getDegrees(), 0.1);
    }

    @Test
    public void testConfidenceBasedAveragingWithThreeBearings() {
        ScalableBearingWithConfidence<TimePoint> d1 = new ScalableBearingWithConfidence<TimePoint>(
                new DegreeBearingImpl(350.), 1., new MillisecondsTimePoint(1000)); // confidence 2/4 when viewed for time point 2000
        ScalableBearingWithConfidence<TimePoint> d2 = new ScalableBearingWithConfidence<TimePoint>(
                new DegreeBearingImpl(10.), 1., new MillisecondsTimePoint(2000)); // confidence 4/4 when viewed for time point 2000
        ScalableBearingWithConfidence<TimePoint> d3 = new ScalableBearingWithConfidence<TimePoint>(
                new DegreeBearingImpl(20.), 2., new MillisecondsTimePoint(3000)); // confidence 2/4 when viewed for time point 2000
        ConfidenceBasedAverager<ScalableBearing, Bearing, TimePoint> averager = ConfidenceFactory.INSTANCE
                .createAverager(ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(1000));
        List<ScalableBearingWithConfidence<TimePoint>> list = Arrays.asList(d1, d2, d3);
        HasConfidence<ScalableBearing, Bearing, TimePoint> average = averager.getAverage(list, new MillisecondsTimePoint(2000));
        assertEquals(10, average.getObject().getDegrees(), 0.1);
    }

    @Test
    public void testConfidenceBasedAveragingTwoPositions() {
        PositionWithConfidence<TimePoint> p1 = new PositionWithConfidenceImpl<TimePoint>(new DegreePosition(0, 45),
                0.9, new MillisecondsTimePoint(2000)); // confidence 2/4 when viewed for time point 1000
        PositionWithConfidence<TimePoint> p2 = new PositionWithConfidenceImpl<TimePoint>(new DegreePosition(0, -45),
                0.9, new MillisecondsTimePoint(1000)); // confidence 4/4 when viewed for time point 1000
        ConfidenceBasedAverager<ScalablePosition, Position, TimePoint> averager = ConfidenceFactory.INSTANCE
                .createAverager(ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(1000));
        List<PositionWithConfidence<TimePoint>> list = Arrays.asList(p1, p2);
        HasConfidence<ScalablePosition, Position, TimePoint> average = averager.getAverage(list, new MillisecondsTimePoint(1000));
        assertEquals(0, average.getObject().getLatDeg(), 0.1);
        // note that the weight varies rather with atan(x) than with x
        assertEquals(Math.atan2(-1, 3)/Math.PI*180., average.getObject().getLngDeg(), 0.0001);
    }

    @Test
    public void testConfidenceBasedAveragingTwoPositionsToNorthPole() {
        PositionWithConfidence<TimePoint> p1 = new PositionWithConfidenceImpl<TimePoint>(new DegreePosition(45, 90),
                0.9, new MillisecondsTimePoint(1000)); // confidence 4/4 when viewed for time point 1000
        PositionWithConfidence<TimePoint> p2 = new PositionWithConfidenceImpl<TimePoint>(new DegreePosition(45, -90),
                0.9, new MillisecondsTimePoint(2000)); // confidence 2/4 when viewed for time point 1000
        ConfidenceBasedAverager<ScalablePosition, Position, TimePoint> averager = ConfidenceFactory.INSTANCE
                .createAverager(ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(1000));
        List<PositionWithConfidence<TimePoint>> list = Arrays.asList(p1, p2);
        HasConfidence<ScalablePosition, Position, TimePoint> average = averager.getAverage(list, new MillisecondsTimePoint(1000));
        assertEquals(Math.atan2(3, 1)/Math.PI*180., average.getObject().getLatDeg(), 0.1);
        assertEquals(90, average.getObject().getLngDeg(), 0.1);
    }

    @Test
    public void testConfidenceBasedAveragingThreePositions() {
        PositionWithConfidence<TimePoint> p1 = new PositionWithConfidenceImpl<TimePoint>(new DegreePosition(49, 8),
                0.9, new MillisecondsTimePoint(1000)); // confidence 4/4 when viewed for time point 1000
        PositionWithConfidence<TimePoint> p2 = new PositionWithConfidenceImpl<TimePoint>(new DegreePosition(49, 9),
                0.9, new MillisecondsTimePoint(2000)); // confidence 2/4 when viewed for time point 1000
        PositionWithConfidence<TimePoint> p3 = new PositionWithConfidenceImpl<TimePoint>(new DegreePosition(50, 8.5),
                0.9, new MillisecondsTimePoint(3000)); // confidence 1/4 when viewed for time point 1000
        ConfidenceBasedAverager<ScalablePosition, Position, TimePoint> averager = ConfidenceFactory.INSTANCE
                .createAverager(ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(1000));
        List<PositionWithConfidence<TimePoint>> list = Arrays.asList(p1, p2, p3);
        // asking for time 1000, we're expecting to be closer to (49, 8) than to the other fixes
        HasConfidence<ScalablePosition, Position, TimePoint> average = averager.getAverage(list, new MillisecondsTimePoint(1000));
        assertTrue(average.getObject().getLatDeg() > 49);
        assertTrue(average.getObject().getLatDeg() < 49.15);
        assertEquals((8.0*4/4 + 9.0*2/4 + 8.5*1/4)/(7./4.), average.getObject().getLngDeg(), 0.01);
    }
    
    @Test
    public void simpleApproximateScalableBearingDistanceTest1() {
        com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing sb = new com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing(new DegreeBearingImpl(100));
        Bearing b = new DegreeBearingImpl(280);
        assertEquals(360./Math.PI, sb.getApproximateDegreeDistanceTo(b), 0.001);
    }

    @Test
    public void simpleApproximateScalableBearingDistanceTest2() {
        com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing sb = new com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing(new DegreeBearingImpl(0));
        Bearing b = new DegreeBearingImpl(180);
        assertEquals(360./Math.PI, sb.getApproximateDegreeDistanceTo(b), 0.001);
    }

    @Test
    public void simpleApproximateScalableBearingDistanceTest3() {
        com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing sb = new com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing(new DegreeBearingImpl(100));
        Bearing b = new DegreeBearingImpl(101);
        assertEquals(1, sb.getApproximateDegreeDistanceTo(b), 0.01);
    }

    @Test
    public void simpleApproximateScalableBearingDistanceTest4() {
        com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing sb = new com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing(new DegreeBearingImpl(0));
        Bearing b = new DegreeBearingImpl(1);
        assertEquals(1, sb.getApproximateDegreeDistanceTo(b), 0.01);
    }

    @Test
    public void scaledApproximateScalableBearingDistanceTest1() {
        com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing sb = new com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing(new DegreeBearingImpl(100)).
                multiply(123);
        Bearing b = new DegreeBearingImpl(280);
        assertEquals(360./Math.PI, sb.getApproximateDegreeDistanceTo(b), 0.001);
    }

    @Test
    public void scaledApproximateScalableBearingDistanceTest2() {
        com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing sb = new com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing(new DegreeBearingImpl(0)).
                multiply(123);
        Bearing b = new DegreeBearingImpl(180);
        assertEquals(360./Math.PI, sb.getApproximateDegreeDistanceTo(b), 0.001);
    }

    @Test
    public void scaledApproximateScalableBearingDistanceTest3() {
        com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing sb = new com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing(new DegreeBearingImpl(100)).
        multiply(123);
        Bearing b = new DegreeBearingImpl(101);
        assertEquals(1, sb.getApproximateDegreeDistanceTo(b), 0.01);
    }

    @Test
    public void scaledApproximateScalableBearingDistanceTest4() {
        com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing sb = new com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing(new DegreeBearingImpl(0)).
        multiply(123);
        Bearing b = new DegreeBearingImpl(1);
        assertEquals(1, sb.getApproximateDegreeDistanceTo(b), 0.01);
    }

}
