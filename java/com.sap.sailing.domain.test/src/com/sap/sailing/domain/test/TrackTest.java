package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.impl.MeterPerSecondSpeedWithDegreeBearingImpl;
import com.sap.sailing.domain.common.scalablevalue.impl.ScalableBearing;
import com.sap.sailing.domain.common.scalablevalue.impl.ScalableSpeed;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.CompactionNotPossibleException;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixMovingImpl;
import com.sap.sailing.domain.shared.tracking.impl.TimeRangeCache;
import com.sap.sailing.domain.shared.tracking.impl.TrackImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixMovingTrackImpl;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixTrackImpl;
import com.sap.sailing.domain.tracking.impl.GPSFixTrackImpl;
import com.sap.sailing.domain.tracking.impl.MaxSpeedCache;
import com.sap.sse.common.AbstractBearing;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TrackTest {
    private static final int MILLIS_BETWEEN_FIXES = 3000000;
    private DynamicGPSFixTrack<Boat, GPSFixMoving> track;
    private GPSFixMovingImpl gpsFix1;
    private GPSFixMovingImpl gpsFix2;
    private GPSFixMovingImpl gpsFix3;
    private GPSFixMovingImpl gpsFix4;
    private GPSFixMovingImpl gpsFix5;

    @BeforeEach
    public void setUp() throws InterruptedException {
        track = new DynamicGPSFixMovingTrackImpl<Boat>(new BoatImpl("123", "MyFirstBoat", new BoatClassImpl("505", /* typicallyStartsUpwind */
        true), null), /* millisecondsOverWhichToAverage */5000, /* no smoothening */null);
        TimePoint now1 = MillisecondsTimePoint.now();
        TimePoint now2 = addMillisToTimepoint(now1, MILLIS_BETWEEN_FIXES);
        DegreePosition position1 = new DegreePosition(1, 2);
        DegreePosition position2 = new DegreePosition(1, 3);
        gpsFix1 = new GPSFixMovingImpl(
                position1, now1, new KnotSpeedWithBearingImpl(position1.getDistance(position2)
                        .inTime(now2.asMillis() - now1.asMillis()).getKnots(),
                        new DegreeBearingImpl(90)), /* optionalTrueHeading */ null);
        gpsFix2 = new GPSFixMovingImpl(position2, now2, new KnotSpeedWithBearingImpl(position1.getDistance(position2)
                .inTime(now2.asMillis() - gpsFix1.getTimePoint().asMillis()).getKnots(), new DegreeBearingImpl(90)), /* optionalTrueHeading */ null);
        TimePoint now3 = addMillisToTimepoint(now2, MILLIS_BETWEEN_FIXES);
        Position position3 = new DegreePosition(1, 4);
        gpsFix3 = new GPSFixMovingImpl(
                position3, now3, new KnotSpeedWithBearingImpl(position2.getDistance(position3)
                        .inTime(now3.asMillis() - gpsFix2.getTimePoint().asMillis()).getKnots(),
                        new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        TimePoint now4 = addMillisToTimepoint(now3, MILLIS_BETWEEN_FIXES);
        Position position4 = new DegreePosition(3, 4);
        gpsFix4 = new GPSFixMovingImpl(
                position4, now4, new KnotSpeedWithBearingImpl(position3.getDistance(position4)
                        .inTime(now4.asMillis() - gpsFix3.getTimePoint().asMillis()).getKnots(),
                        new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        TimePoint now5 = addMillisToTimepoint(now4, MILLIS_BETWEEN_FIXES);
        Position position5 = new DegreePosition(5, 4);
        gpsFix5 = new GPSFixMovingImpl(position5, now5, new KnotSpeedWithBearingImpl(position4.getDistance(position5)
                .inTime(now5.asMillis() - gpsFix4.getTimePoint().asMillis()).getKnots(), new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        track.addGPSFix(gpsFix1);
        track.addGPSFix(gpsFix2);
        track.addGPSFix(gpsFix3);
        track.addGPSFix(gpsFix4);
        track.addGPSFix(gpsFix5);
    }
    
    @Test
    public void bearingInterpolationTest() {
        TimePoint betweenFirstAndSecond = gpsFix1.getTimePoint().plus(gpsFix1.getTimePoint().until(gpsFix2.getTimePoint()).divide(2));
        Bearing bearingBetweenFirstAndSecond = track.getInterpolatedValue(betweenFirstAndSecond, f->new ScalableBearing(f.getSpeed().getBearing()));
        assertEquals(bearingBetweenFirstAndSecond.getDegrees(), gpsFix1.getSpeed().getBearing().middle(gpsFix2.getSpeed().getBearing()).getDegrees(), 0.00001);
    }
    
    @Test
    public void speedInterpolationTest() {
        TimePoint betweenFirstAndSecond = gpsFix1.getTimePoint().plus(gpsFix1.getTimePoint().until(gpsFix2.getTimePoint()).divide(2));
        Speed speedBetweenFirstAndSecond = track.getInterpolatedValue(betweenFirstAndSecond, f->new ScalableSpeed(f.getSpeed()));
        assertEquals(speedBetweenFirstAndSecond.getKnots(), (gpsFix1.getSpeed().getKnots()+gpsFix2.getSpeed().getKnots())/2, 0.01);
    }
    
    /**
     * Tests  for a incorrect method of estimating Positions
     */
    @Test
    public void positionEstimationTest() {
        track = new DynamicGPSFixMovingTrackImpl<Boat>(
                new BoatImpl("123", "MyFirstBoat", new BoatClassImpl("505", true), null), 5000, null);
        DegreePosition p1 = new DegreePosition(0, 0);
        DegreePosition p2 = new DegreePosition(90, 0);
        TimePoint t1 = MillisecondsTimePoint.now();
        TimePoint t2 = t1.plus(30);
        gpsFix1 = new GPSFixMovingImpl(p1, t1, new KnotSpeedWithBearingImpl(0, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        gpsFix2 = new GPSFixMovingImpl(p2, t2, new KnotSpeedWithBearingImpl(0, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        track.addGPSFix(gpsFix1);
        track.addGPSFix(gpsFix2);
        track.lockForRead();
        try {
            assertEquals(0, track.getEstimatedPosition(t1, true).getLatDeg(), 0.00000001);
            assertEquals(30, track.getEstimatedPosition(t1.plus(10), true).getLatDeg(), 0.00000001);
            assertEquals(45, track.getEstimatedPosition(t1.plus(15), true).getLatDeg(), 0.00000001);
            assertEquals(60, track.getEstimatedPosition(t1.plus(20), true).getLatDeg(), 0.00000001);
            assertEquals(90, track.getEstimatedPosition(t1.plus(30), true).getLatDeg(), 0.00000001);
        } finally {
            track.unlockAfterRead();
        }
    }
    /**
     * Introducing a new feature on {@link GPSFixTrack} that allows clients to find positions to a sequence of
     * {@link Timed} objects in ascending order, this method compares those results to the ordinary explicit calls
     * to {@link GPSFixTrack#getEstimatedPosition(TimePoint, boolean)}.
     */
    @SuppressWarnings("serial")
    @Test
    public void testGetEstimatedPositionSingleVsIteratedWithSmallerSteps() {
        TimePoint start = gpsFix1.getTimePoint().minus((gpsFix5.getTimePoint().asMillis()-gpsFix1.getTimePoint().asMillis())/2);
        TimePoint end = gpsFix5.getTimePoint().plus((gpsFix5.getTimePoint().asMillis()-gpsFix1.getTimePoint().asMillis())/2);
        List<Timed> timeds = new ArrayList<>();
        for (TimePoint t = start; !t.after(end); t = t.plus((gpsFix5.getTimePoint().asMillis()-gpsFix1.getTimePoint().asMillis())/10)) {
            final TimePoint finalT = t;
            timeds.add(new Timed() {public TimePoint getTimePoint() { return finalT; }});
        }
        assertEqualEstimatedPositionsSingleVsIterated(timeds, /* extrapolate */ true);
        assertEqualEstimatedPositionsSingleVsIterated(timeds, /* extrapolate */ false);
    }

    @SuppressWarnings("serial")
    @Test
    public void testGetEstimatedPositionSingleVsIteratedWithLargerSteps() {
        TimePoint start = gpsFix1.getTimePoint().minus((gpsFix5.getTimePoint().asMillis()-gpsFix1.getTimePoint().asMillis())/2);
        TimePoint end = gpsFix5.getTimePoint().plus((gpsFix5.getTimePoint().asMillis()-gpsFix1.getTimePoint().asMillis())/2);
        List<Timed> timeds = new ArrayList<>();
        for (TimePoint t = start; !t.after(end); t = t.plus(gpsFix5.getTimePoint().asMillis()-gpsFix1.getTimePoint().asMillis())) {
            final TimePoint finalT = t;
            timeds.add(new Timed() {public TimePoint getTimePoint() { return finalT; }});
        }
        assertEqualEstimatedPositionsSingleVsIterated(timeds, /* extrapolate */ true);
        assertEqualEstimatedPositionsSingleVsIterated(timeds, /* extrapolate */ false);
    }

    private void assertEqualEstimatedPositionsSingleVsIterated(List<Timed> timeds, boolean extrapolate) {
        List<Position> positions1 = new ArrayList<>();
        for (Timed timed : timeds) {
            positions1.add(track.getEstimatedPosition(timed.getTimePoint(), extrapolate));
        }
        List<Position> positions2 = new ArrayList<>();
        track.lockForRead();
        try {
            for (Iterator<Position> pIter = track.getEstimatedPositions(timeds, extrapolate); pIter.hasNext();) {
                positions2.add(pIter.next());
            }
        } finally {
            track.unlockAfterRead();
        }
        Iterator<Position> p1Iter = positions1.iterator();
        Iterator<Position> p2Iter = positions2.iterator();
        while (p1Iter.hasNext()) {
            assertTrue(p2Iter.hasNext());
            Position p1 = p1Iter.next();
            Position p2 = p2Iter.next();
            assertEquals(p1.getLatDeg(), p2.getLatDeg(), 0.000000001, "Diff between "+p1+" and "+p2);
            assertEquals(p1.getLngDeg(), p2.getLngDeg(), 0.000000001, "Diff between "+p1+" and "+p2);
        }
    }
    
    /**
     * A test regarding bug 968. Three subsequent fixes at the same position with increasing time stamps each, then
     * jumping to the next position for three seconds. Let's see what distance and SOG do...
     * <p>
     * 
     * TODO bug 1504 The challenge with this is that the equal fixes all have a wrong speed value compared to at least
     * one of their neighbors. They are hence all considered outliers, and no fix remains. There is however a possible
     * sub-sequence of fixes that constitutes a sequence of all-valid fixes when only valid fixes are checked for
     * validity with their valid neighbors. But this, at first glance, sounds like an ugly NP-complete problem: find the
     * longest possible sub-sequence such that all fixes in the sub-sequence are valid w.r.t. their neighbors.
     */
    @Test
    public void testJumpyFixes() {
        DynamicGPSFixTrack<Object, GPSFixMoving> track = new DynamicGPSFixMovingTrackImpl<Object>(new Object(),
                /* millisecondsOverWhichToAverage */ 30000l);
        TimePoint start = MillisecondsTimePoint.now();
        TimePoint now = start;
        final DegreePosition startPos = new DegreePosition(0, 0);
        Position pos = startPos;
        SpeedWithBearing speed = new KnotSpeedWithBearingImpl(40, new DegreeBearingImpl(90));
        int NUMBER_OF_FIXES_AT_SAME_POSITION = 3;
        int NUMBER_OF_REAL_FIXES = 10;
        for (int i = 0; i < NUMBER_OF_REAL_FIXES; i++) {
            addFixesWithSamePositionButProgressingTime(track, now, pos, speed, NUMBER_OF_FIXES_AT_SAME_POSITION);
            track.getDistanceTraveled(start, now); // cause DistanceCache to cache a result
            TimePoint nextNow = now.plus(NUMBER_OF_FIXES_AT_SAME_POSITION * 1000);
            pos = pos.translateGreatCircle(speed.getBearing(), speed.travel(now, nextNow));
            now = nextNow;
        }
        assertEquals(speed.travel(start, now.minus(NUMBER_OF_FIXES_AT_SAME_POSITION * 1000)).getMeters(),
                track.getDistanceTraveled(start, now).getMeters(), 0.01);
    }

    private void addFixesWithSamePositionButProgressingTime(DynamicGPSFixTrack<Object, GPSFixMoving> track,
            TimePoint start, Position pos, SpeedWithBearing speed, int numberOfFixes) {
        TimePoint now = start;
        for (int i = 0; i < numberOfFixes; i++) {
            GPSFixMoving fix1 = new GPSFixMovingImpl(pos, now, speed, /* optionalTrueHeading */ null);
            track.addGPSFix(fix1);
            now = now.plus(1000);
        }
    }

    /**
     * See bug 2626; this test tries to provoke a race condition by subclassing {@link MaxSpeedCache} and overriding
     * {@link MaxSpeedCache#cache} so that it can synchronize fix additions to the track with a
     * {@link MaxSpeedCache#getMaxSpeed(TimePoint, TimePoint)} call to the cache.
     */
    @Test
    public void testMaxSpeedCacheRaceCondition() throws InterruptedException, BrokenBarrierException, TimeoutException {
        final CyclicBarrier cacheBarrier = new CyclicBarrier(2);
        final CyclicBarrier cacheDone = new CyclicBarrier(2);
        
        DynamicGPSFixMovingTrackImpl<Object> track = new DynamicGPSFixMovingTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */ 30000l) {
            private static final long serialVersionUID = 1L;

            @Override
            protected MaxSpeedCache<Object, GPSFixMoving> createMaxSpeedCache() {
                return new MaxSpeedCache<Object, GPSFixMoving>(this) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected Pair<GPSFixMoving, Speed> computeMaxSpeed(TimePoint from, TimePoint to) {
                        Pair<GPSFixMoving, Speed> result = super.computeMaxSpeed(from, to);
                        try {
                            Thread.sleep(1000); // just wait a bit; can't lock really because that would cause a deadlock
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return result;
                    }

                    @Override
                    protected void cache(TimePoint from, TimePoint to, Pair<GPSFixMoving, Speed> fixAtMaxSpeed) {
                        try {
                            cacheBarrier.await();
                        } catch (InterruptedException | BrokenBarrierException e) {
                            throw new RuntimeException(e);
                        }
                        super.cache(from, to, fixAtMaxSpeed);
                        try {
                            cacheDone.await();
                        } catch (InterruptedException | BrokenBarrierException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }
        };
        GPSFixMoving fix1 = new GPSFixMovingImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(0), new KnotSpeedWithBearingImpl(
                1, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
        track.addGPSFix(fix1);
        // The following getMaximumSpeedOverGround call will trigger a computeMaxSpeed(...) and a cache(...) call
        new Thread(()->
            assertEquals(1., track.getMaximumSpeedOverGround(new MillisecondsTimePoint(0), new MillisecondsTimePoint(7200000)).
                getB().getKnots(), 0.01)).start(); // produces a cache entry that ends
        // now don't release the cacheBarrier as yet but add more fixes
        new Thread(() -> {
            GPSFixMoving fix2 = new GPSFixMovingImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(3600000),
                    new KnotSpeedWithBearingImpl(2, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
            track.addGPSFix(fix2);
        }).start();
        new Thread(() -> {
            GPSFixMoving fix3 = new GPSFixMovingImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(7200000),
                    new KnotSpeedWithBearingImpl(1, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
            track.addGPSFix(fix3);
        }).start();
        new Thread(() -> {
            GPSFixMoving fix4 = new GPSFixMovingImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(10800000),
                    new KnotSpeedWithBearingImpl(1, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
            track.addGPSFix(fix4);
        }).start();
        cacheBarrier.await(); // releasing the creation of the cache entry from way above; this would now add a stale entry
        // that would have been invalidated by all the GPS fixes above
        cacheDone.await(); // wait for the caching to have completed
        final double maxSpeed[] = new double[1];
        final CyclicBarrier testDone = new CyclicBarrier(2);
        new Thread(() -> {
            maxSpeed[0] = track.getMaximumSpeedOverGround(new MillisecondsTimePoint(0), new MillisecondsTimePoint(7200000)).
                    getB().getKnots();
            try {
                testDone.await();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).start();
        cacheBarrier.await(20, TimeUnit.SECONDS);
        cacheDone.await(20, TimeUnit.SECONDS);
        testDone.await();
        assertEquals(2., maxSpeed[0], 0.1);
    }

    @Test
    public void testMaxSpeedForNonMovingTrackWithUpperTimeLimit() {
        DynamicGPSFixTrack<Object, GPSFix> track = new DynamicGPSFixTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */ 30000l);
        GPSFix fix1 = new GPSFixImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(0));
        track.addGPSFix(fix1);
        GPSFix fix2 = new GPSFixImpl(new DegreePosition(1./60., 0), new MillisecondsTimePoint(3600000)); // 1nm in one hour = 1kt
        track.addGPSFix(fix2);
        GPSFix fix3 = new GPSFixImpl(new DegreePosition(3./60., 0), new MillisecondsTimePoint(7200000)); // 2nm in one hour = 2kts
        track.addGPSFix(fix3);
        GPSFix fix4 = new GPSFixImpl(new DegreePosition(4./60., 0), new MillisecondsTimePoint(10800000)); // 1nm in one hour = 1kt
        track.addGPSFix(fix4);
        assertEquals(1., track.getMaximumSpeedOverGround(new MillisecondsTimePoint(0), new MillisecondsTimePoint(3600000)).
                getB().getKnots(), 0.001);
    }

    @Test
    public void testMaxSpeedForNonMovingTrack() {
        DynamicGPSFixTrack<Object, GPSFix> track = new DynamicGPSFixTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */ 30000l);
        GPSFix fix1 = new GPSFixImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(0));
        track.addGPSFix(fix1);
        GPSFix fix2 = new GPSFixImpl(new DegreePosition(1./60., 0), new MillisecondsTimePoint(3600000)); // 1nm in one hour = 1kt
        track.addGPSFix(fix2);
        GPSFix fix3 = new GPSFixImpl(new DegreePosition(3./60., 0), new MillisecondsTimePoint(7200000)); // 2nm in one hour = 2kts
        track.addGPSFix(fix3);
        GPSFix fix4 = new GPSFixImpl(new DegreePosition(4./60., 0), new MillisecondsTimePoint(10800000)); // 1nm in one hour = 1kt
        track.addGPSFix(fix4);
        assertEquals(2., track.getMaximumSpeedOverGround(new MillisecondsTimePoint(0), new MillisecondsTimePoint(10800000)).
                getB().getKnots(), 0.001);
    }

    @Test
    public void testMaxSpeedForMovingTrackWithFixAtIntervalBoundary() {
        DynamicGPSFixMovingTrackImpl<Object> track = new DynamicGPSFixMovingTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */ 30000l);
        GPSFixMoving fix1 = new GPSFixMovingImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(0), new KnotSpeedWithBearingImpl(
                1, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
        track.addGPSFix(fix1);
        GPSFixMoving fix2 = new GPSFixMovingImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(3600000), new KnotSpeedWithBearingImpl(
                2, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
        track.addGPSFix(fix2);
        GPSFixMoving fix3 = new GPSFixMovingImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(7200000), new KnotSpeedWithBearingImpl(
                1, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
        track.addGPSFix(fix3);
        assertEquals(2., track.getMaximumSpeedOverGround(new MillisecondsTimePoint(0), new MillisecondsTimePoint(7200000)).
                getB().getKnots(), 0.01); // produces a cache entry that ends 
        GPSFixMoving fix4 = new GPSFixMovingImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(10800000), new KnotSpeedWithBearingImpl(
                1, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
        track.addGPSFix(fix4);
        assertEquals(2., track.getMaximumSpeedOverGround(new MillisecondsTimePoint(0), new MillisecondsTimePoint(10800000)).
                getB().getKnots(), 0.01);
    }
    
    @Test
    public void testMaxSpeedForFixOverwrittenWithLowerSpeed() {
        DynamicGPSFixMovingTrackImpl<Object> track = new DynamicGPSFixMovingTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */ 30000l);
        GPSFixMoving slow = new GPSFixMovingImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(3600000), new KnotSpeedWithBearingImpl(
                1, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
        GPSFixMoving fast = new GPSFixMovingImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(3600000), new KnotSpeedWithBearingImpl(
                2, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
        track.addGPSFix(fast);
        assertEquals(2., track.getMaximumSpeedOverGround(new MillisecondsTimePoint(0), new MillisecondsTimePoint(10800000)).
                getB().getKnots(), 0.01);
        track.addGPSFix(slow);
        track.lockForRead();
        try {
            assertEquals(1, track.getFixes().size());
        } finally {
            track.unlockAfterRead();
        }
        assertEquals(1., track.getMaximumSpeedOverGround(new MillisecondsTimePoint(0), new MillisecondsTimePoint(10800000)).
                getB().getKnots(), 0.01);
    }
    
    @Test
    public void testMaxSpeedForMovingTrackWithFixAtIntervalBoundaryAfterQuery() {
        DynamicGPSFixMovingTrackImpl<Object> track = new DynamicGPSFixMovingTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */ 30000l);
        GPSFixMoving fix1 = new GPSFixMovingImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(0), new KnotSpeedWithBearingImpl(
                1, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
        track.addGPSFix(fix1);
        GPSFixMoving fix2 = new GPSFixMovingImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(3600000), new KnotSpeedWithBearingImpl(
                2, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
        track.addGPSFix(fix2);
        assertEquals(2., track.getMaximumSpeedOverGround(new MillisecondsTimePoint(0), new MillisecondsTimePoint(7200000)).
                getB().getKnots(), 0.01); // produces a cache entry that ends 
        GPSFixMoving fix3 = new GPSFixMovingImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(7200000), new KnotSpeedWithBearingImpl(
                1, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
        track.addGPSFix(fix3);
        GPSFixMoving fix4 = new GPSFixMovingImpl(new DegreePosition(0, 0), new MillisecondsTimePoint(10800000), new KnotSpeedWithBearingImpl(
                1, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
        track.addGPSFix(fix4);
        assertEquals(2., track.getMaximumSpeedOverGround(new MillisecondsTimePoint(0), new MillisecondsTimePoint(10800000)).
                getB().getKnots(), 0.01);
    }
    
    /**
     * The DistanceCache must not contain any intervals pointing backwards because otherwise an endless recursion will
     * result during the next cache look-up. This test performs a {@link GPSFixTrack#getDistanceTraveled(TimePoint, TimePoint)} with
     * <code>from</code> later than <code>to</code> and ensures that no reversed entry is written to the cache.
     */
    @Test
    public void testDistanceTraveledBackwardsQuery() {
        final Set<com.sap.sse.common.Util.Triple<TimePoint, TimePoint, Distance>> cacheEntries = new HashSet<>();
        final TimeRangeCache<Distance> distanceCache = new TimeRangeCache<Distance>("test-DistanceCache") {
            @Override
            public void cache(TimePoint from, TimePoint to, Distance distance) {
                super.cache(from, to, distance);
                cacheEntries.add(new com.sap.sse.common.Util.Triple<>(from, to, distance));
            }
            
        };
        DynamicGPSFixTrack<Object, GPSFix> track = new DynamicGPSFixTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */ 30000l) {
            private static final long serialVersionUID = -7277196393160609503L;
            @Override
            protected TimeRangeCache<Distance> getDistanceCache() {
                return distanceCache;
            }
        };
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint earlier = now.minus(10000);
        TimePoint later = now.plus(10000);
        Distance result = track.getDistanceTraveled(now, earlier);
        assertEquals(Distance.NULL, result);
        assertTrue(cacheEntries.isEmpty());
        Distance nextResult = track.getDistanceTraveled(now, later);
        assertEquals(Distance.NULL, nextResult);
    }
    
    @Test
    public void testBearingAveragingAcrossZeroDegrees() {
        DynamicGPSFixTrack<Object, GPSFix> track = new DynamicGPSFixTrackImpl<Object>(null, /* millisecondsOverWhichToAverage */ 5000);
        TimePoint t1 = new MillisecondsTimePoint(1000);
        TimePoint t2 = new MillisecondsTimePoint(2000);
        TimePoint t3 = new MillisecondsTimePoint(3000);
        GPSFix f1 = new GPSFixImpl(new DegreePosition(0, 0), t1);
        GPSFix f2 = new GPSFixImpl(new DegreePosition(0.00001, 0.00001), t2);
        GPSFix f3 = new GPSFixImpl(new DegreePosition(0.00002, 0), t3);
        track.addGPSFix(f1);
        track.addGPSFix(f2);
        track.addGPSFix(f3);
        SpeedWithBearing average = track.getEstimatedSpeed(t2);
        PositionAssert.assertBearingEquals(new DegreeBearingImpl(0), average.getBearing(), 0.1);
    }
    
    @Test
    public void testFixValidEvenIfFixProvidedSpeedIsOutrageous() {
        DynamicGPSFixTrack<Object, GPSFixMoving> track = new DynamicGPSFixMovingTrackImpl<Object>(null, /* millisecondsOverWhichToAverage */ 5000l);
        TimePoint t1 = new MillisecondsTimePoint(1000);
        TimePoint t2 = new MillisecondsTimePoint(2000);
        TimePoint t3 = new MillisecondsTimePoint(3000);
        GPSFixMoving f1 = new GPSFixMovingImpl(new DegreePosition(1./3600.*1./60., 0), t1, new KnotSpeedWithBearingImpl(1, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        GPSFixMoving f2 = new GPSFixMovingImpl(new DegreePosition(2./3600.*1./60., 0), t2, new KnotSpeedWithBearingImpl(150, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null); // outrageous speed; to be ignored by getEstimatedSpeed
        GPSFixMoving f3 = new GPSFixMovingImpl(new DegreePosition(3./3600.*1./60., 0), t3, new KnotSpeedWithBearingImpl(1, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        track.addGPSFix(f1);
        track.addGPSFix(f2);
        track.addGPSFix(f3);
        PositionAssert.assertGPSFixEquals(f2, track.getFirstFixAtOrAfter(f2.getTimePoint()), /* pos deg delta */ 0.00001, /* bearing deg delta */ 0.01, /* knot delta */ 0.01); // expect the fix to still be valid, but only its provided speed shall be ignored
    }
    
    @Test
    public void testFixInvalidEvenIfPositionInferredSpeedIsOutrageous() {
        DynamicGPSFixTrack<Object, GPSFixMoving> track = new DynamicGPSFixMovingTrackImpl<Object>(null, /* millisecondsOverWhichToAverage */ 5000l);
        TimePoint t1 = new MillisecondsTimePoint(1000);
        TimePoint t2 = new MillisecondsTimePoint(2000);
        TimePoint t3 = new MillisecondsTimePoint(3000);
        GPSFixMoving f1 = new GPSFixMovingImpl(new DegreePosition(1./3600.*1./60., 0), t1, new KnotSpeedWithBearingImpl(1, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        GPSFixMoving f2 = new GPSFixMovingImpl(new DegreePosition(150./3600.*1./60., 0), t2, new KnotSpeedWithBearingImpl(150, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null); // outrageous speed; to be ignored by getEstimatedSpeed
        GPSFixMoving f3 = new GPSFixMovingImpl(new DegreePosition(3./3600.*1./60., 0), t3, new KnotSpeedWithBearingImpl(1, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        track.addGPSFix(f1);
        track.addGPSFix(f2);
        track.addGPSFix(f3);
        PositionAssert.assertGPSFixEquals(f3, track.getFirstFixAtOrAfter(f2.getTimePoint()), /* pos deg delta */ 0.00001, /* bearing deg delta */ 0.01, /* knot delta */ 0.01); // expect the fix to still be valid, but only its provided speed shall be ignored
    }
    
    @Test
    public void testFixInvalidBecauseProvidedSpeedIsTooDifferent() {
        DynamicGPSFixTrack<Object, GPSFixMoving> track = new DynamicGPSFixMovingTrackImpl<Object>(null, /* millisecondsOverWhichToAverage */ 5000l);
        TimePoint t1 = new MillisecondsTimePoint(1000);
        TimePoint t2 = new MillisecondsTimePoint(2000);
        TimePoint t3 = new MillisecondsTimePoint(3000);
        GPSFixMoving f1 = new GPSFixMovingImpl(new DegreePosition(1./3600.*1./60., 0), t1, new KnotSpeedWithBearingImpl(1, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        GPSFixMoving f2 = new GPSFixMovingImpl(new DegreePosition(2./3600.*1./60., 0), t2, new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null); // outrageous speed; to be ignored by getEstimatedSpeed
        GPSFixMoving f3 = new GPSFixMovingImpl(new DegreePosition(3./3600.*1./60., 0), t3, new KnotSpeedWithBearingImpl(1, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        track.addGPSFix(f1);
        track.addGPSFix(f2);
        track.addGPSFix(f3);
        // expect the f2 fix to be invalid because provided speed is too different
        assertEquals(0, f3.getPosition().getDistance(track.getFirstFixAtOrAfter(f2.getTimePoint()).getPosition()).getMeters(), 0.01);
    }
    
    @Test
    public void testIgnoringFixProvidedSpeedIfItIsOutrageous() {
        DynamicGPSFixTrack<Object, GPSFixMoving> track = new DynamicGPSFixMovingTrackImpl<Object>(null, /* millisecondsOverWhichToAverage */ 5000l);
        TimePoint t1 = new MillisecondsTimePoint(1000);
        TimePoint t2 = new MillisecondsTimePoint(2000);
        TimePoint t3 = new MillisecondsTimePoint(3000);
        GPSFixMoving f1 = new GPSFixMovingImpl(new DegreePosition(1./3600.*1./60., 0), t1, new KnotSpeedWithBearingImpl(1, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        GPSFixMoving f2 = new GPSFixMovingImpl(new DegreePosition(2./3600.*1./60., 0), t2, new KnotSpeedWithBearingImpl(150, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null); // outrageous speed; to be ignored by getEstimatedSpeed
        GPSFixMoving f3 = new GPSFixMovingImpl(new DegreePosition(3./3600.*1./60., 0), t3, new KnotSpeedWithBearingImpl(1, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        track.addGPSFix(f1);
        track.addGPSFix(f2);
        track.addGPSFix(f3);
        PositionAssert.assertGPSFixEquals(f2, track.getFirstFixAtOrAfter(f2.getTimePoint()), /* pos deg delta */ 0.00001, /* bearing deg delta */ 0.01, /* knot delta */ 0.01); // expect the fix to still be valid, but only its provided speed shall be ignored
        SpeedWithBearing average = track.getEstimatedSpeed(t2);
        assertEquals(1, average.getKnots(), 0.01);
    }
    
    @Test
    public void testBearingAveragingAcrossZeroDegreesWithGPSFixMoving() {
        DynamicGPSFixMovingTrackImpl<Object> track = new DynamicGPSFixMovingTrackImpl<Object>(null, /* millisecondsOverWhichToAverage */ 5000);
        TimePoint t1 = new MillisecondsTimePoint(1000);
        TimePoint t2 = new MillisecondsTimePoint(2000);
        TimePoint t3 = new MillisecondsTimePoint(3000);
        GPSFixMoving f1 = new GPSFixMovingImpl(new DegreePosition(0, 0), t1, new KnotSpeedWithBearingImpl(1, new DegreeBearingImpl(45)), /* optionalTrueHeading */ null);
        GPSFixMoving f2 = new GPSFixMovingImpl(new DegreePosition(0.00001, 0.00001), t2, new KnotSpeedWithBearingImpl(1, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        GPSFixMoving f3 = new GPSFixMovingImpl(new DegreePosition(0.00002, 0), t3, new KnotSpeedWithBearingImpl(1, new DegreeBearingImpl(315)), /* optionalTrueHeading */ null);
        track.addGPSFix(f1);
        track.addGPSFix(f2);
        track.addGPSFix(f3);
        SpeedWithBearing average = track.getRawEstimatedSpeed(t2);
        PositionAssert.assertBearingEquals(new DegreeBearingImpl(0), average.getBearing(), /* deg delta */ 0.1);
    }
    
    /**
     * Bug #70: modify a track while iterating over a subset of it; ensure that this causes a
     * {@link ConcurrentModificationException}.
     */
    @Test
    public void testAddingWhileIteratingOverSubset() throws InterruptedException, IllegalArgumentException,
            IllegalAccessException, SecurityException, NoSuchFieldException {
        Field fixesField = TrackImpl.class.getDeclaredField("fixes");
        fixesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        NavigableSet<GPSFixMoving> fixes = (NavigableSet<GPSFixMoving>) fixesField.get(track);
        SortedSet<GPSFixMoving> subset = fixes.subSet(gpsFix2, gpsFix5);
        assertEquals(3, subset.size());
        Iterator<GPSFixMoving> subsetIter = subset.iterator();
        // start iteration
        GPSFixMoving firstOfSubset = subsetIter.next();
        PositionAssert.assertGPSFixEquals(gpsFix2, firstOfSubset, /* pos deg delta */ 0.00001, /* bearing deg delta */ 0.01, /* knot delta */ 0.01);
        // now add a fix:
        TimePoint now6 = addMillisToTimepoint(gpsFix5.getTimePoint(), 3);
        track.addGPSFix(new GPSFixMovingImpl(
                new DegreePosition(6, 5), now6, new KnotSpeedWithBearingImpl(2, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null));
        try {
            GPSFixMoving secondOfSubset = subsetIter.next();
            PositionAssert.assertGPSFixEquals(gpsFix3, secondOfSubset, /* pos deg delta */ 0.000001, /* bearing deg delta */ 0.01, /* knot delta */ 0.01);
            fail("adding a fix interferes with iteration over subSet of track's fixes");
        } catch (ConcurrentModificationException e) {
            // this is what we expected
        }
    }
    
   private TimePoint addMillisToTimepoint(TimePoint p, long millis) {
       return p.plus(millis);
   }

    @Test
    public void testIterate() {
        track.lockForRead();
        try {
            Iterator<GPSFixMoving> i = track.getRawFixes().iterator();
            int count;
            for (count = 0; i.hasNext(); count++) {
                i.next();
            }
            assertEquals(5, count);
        } finally {
            track.unlockAfterRead();
        }
    }
    
    @Test
    public void testOrdering() {
        long lastMillis = 0;
        GPSFix lastFix = null;
        boolean first = true;
        track.lockForRead();
        try {
            for (Iterator<GPSFixMoving> i = track.getRawFixes().iterator(); i.hasNext(); first = false) {
                GPSFixMoving fix = i.next();
                long millis = fix.getTimePoint().asMillis();
                if (!first) {
                    assertTrue(millis > lastMillis);
                    TimePoint inBetweenTimePoint = new MillisecondsTimePoint((millis + lastMillis) / 2);
                    assertEquals(lastFix, track.getLastRawFixBefore(inBetweenTimePoint));
                    assertEquals(lastFix, track.getLastRawFixAtOrBefore(inBetweenTimePoint));
                    assertEquals(fix, track.getFirstRawFixAfter(inBetweenTimePoint));
                    assertEquals(fix, track.getFirstRawFixAtOrAfter(inBetweenTimePoint));

                    assertEquals(lastFix, track.getLastRawFixAtOrBefore(lastFix.getTimePoint()));
                    assertEquals(fix, track.getFirstRawFixAtOrAfter(fix.getTimePoint()));

                    assertEquals(lastFix, track.getLastRawFixBefore(fix.getTimePoint()));
                    assertEquals(fix, track.getLastRawFixAtOrBefore(fix.getTimePoint()));
                    assertEquals(fix, track.getFirstRawFixAfter(lastFix.getTimePoint()));
                    assertEquals(lastFix, track.getFirstRawFixAtOrAfter(lastFix.getTimePoint()));
                }
                lastMillis = millis;
                lastFix = fix;
            }
        } finally {
            track.unlockAfterRead();
        }
    }
    
    @Test
    public void assertEstimatedPositionBeforeStartIsStart() {
        track.lockForRead();
        try {
            GPSFixMoving start = track.getRawFixes().iterator().next();
            TimePoint oneNanoBeforeStart = new MillisecondsTimePoint(start.getTimePoint().asMillis() - 1);
            assertEquals(start.getPosition(), track.getEstimatedPosition(oneNanoBeforeStart, false));
        } finally {
            track.unlockAfterRead();
        }
    }
    
    @Test
    public void testSimpleInterpolation() {
        long lastMillis = 0;
        GPSFix lastFix = null;
        boolean first = true;
        track.lockForRead();
        try {
            for (Iterator<GPSFixMoving> i = track.getRawFixes().iterator(); i.hasNext(); first = false) {
                GPSFixMoving fix = i.next();
                long millis = fix.getTimePoint().asMillis();
                if (!first) {
                    TimePoint inBetweenTimePoint = new MillisecondsTimePoint((millis + lastMillis) / 2);
                    Position interpolatedPosition = track.getEstimatedRawPosition(inBetweenTimePoint, false);
                    Distance d1 = lastFix.getPosition().getDistance(interpolatedPosition);
                    Distance d2 = interpolatedPosition.getDistance(fix.getPosition());
                    // the interpolated point should be on the great circle, not open a "triangle"
                    assertEquals(lastFix.getPosition().getDistance(fix.getPosition()).getMeters(),
                            d1.getMeters() + d2.getMeters(), 0.00001);
                }
                lastMillis = millis;
                lastFix = fix;
            }
        } finally {
            track.unlockAfterRead();
        }
    }
    
    @Test
    public void testSimpleExtrapolation() {
        GPSFix fix = track.getLastRawFix();
        long millis = fix.getTimePoint().asMillis();
        GPSFix lastFix = track.getLastRawFixBefore(fix.getTimePoint());
        long lastMillis = lastFix.getTimePoint().asMillis();
        TimePoint afterTimePoint = new MillisecondsTimePoint(millis + (millis-lastMillis));
        Position extrapolatedPosition = track.getEstimatedPosition(afterTimePoint, true);
        assertEquals(0.5, fix.getPosition().getDistance(extrapolatedPosition).getMeters()
                / lastFix.getPosition().getDistance(extrapolatedPosition).getMeters(), 0.01);
    }
    
    @Test
    public void testExtrapolationDoesntHappenIfSuppressed() {
        GPSFix fix = track.getLastRawFix();
        long millis = fix.getTimePoint().asMillis();
        GPSFix lastFix = track.getLastRawFixBefore(fix.getTimePoint());
        long lastMillis = lastFix.getTimePoint().asMillis();
        TimePoint afterTimePoint = new MillisecondsTimePoint(millis + (millis-lastMillis));
        Position extrapolatedPosition = track.getEstimatedPosition(afterTimePoint, false);
        assertEquals(extrapolatedPosition, fix.getPosition());
    }
    
    @Test
    public void testDistanceTraveled() {
        List<Distance> distances = new ArrayList<Distance>();
        List<GPSFixMoving> fixes = new ArrayList<GPSFixMoving>();
        boolean first = true;
        GPSFixMoving oldFix = null;
        track.lockForRead();
        try {
            for (GPSFixMoving fix : track.getRawFixes()) {
                fixes.add(fix);
                if (first) {
                    first = false;
                } else {
                    Distance d = oldFix.getPosition().getDistance(fix.getPosition());
                    distances.add(d);
                }
                oldFix = fix;
            }
            for (int i = 0; i < fixes.size(); i++) {
                for (int j = i; j < fixes.size(); j++) {
                    double distanceSumInNauticalMiles = 0;
                    for (int k = i; k < j; k++) {
                        distanceSumInNauticalMiles += distances.get(k).getNauticalMiles();
                    }
                    // travel fully from fix #i to fix #j and require the segment distances to sum up equal
                    double nauticalMilesFromIToJ = track.getRawDistanceTraveled(fixes.get(i).getTimePoint(),
                            fixes.get(j).getTimePoint()).getNauticalMiles();
                    assertEquals(distanceSumInNauticalMiles, nauticalMilesFromIToJ, 0.0000001);
                    if (j > i) {
                        // now skip half a segment at the beginning:
                        double nauticalMilesFromHalfAfterIToJ = track.getRawDistanceTraveled(
                                new MillisecondsTimePoint((fixes.get(i).getTimePoint().asMillis() + fixes.get(i + 1)
                                        .getTimePoint().asMillis()) / 2), fixes.get(j).getTimePoint())
                                .getNauticalMiles();
                        assertTrue(nauticalMilesFromHalfAfterIToJ < distanceSumInNauticalMiles,
                                "for i=" + i + ", j=" + j + ": " + nauticalMilesFromHalfAfterIToJ + "<"
                                        + distanceSumInNauticalMiles);
                        if (i > 0) {
                            // now skip half a segment before the beginning:
                            double nauticalMilesFromHalfBeforeIToJ = track.getRawDistanceTraveled(
                                    new MillisecondsTimePoint((fixes.get(i).getTimePoint().asMillis() + fixes
                                            .get(i - 1).getTimePoint().asMillis()) / 2), fixes.get(j).getTimePoint())
                                    .getNauticalMiles();
                            assertTrue(nauticalMilesFromHalfBeforeIToJ > distanceSumInNauticalMiles);
                        }
                        // now skip half a segment at the end:
                        double nauticalMilesFromIToHalfBeforeJ = track.getRawDistanceTraveled(
                                fixes.get(i).getTimePoint(),
                                new MillisecondsTimePoint((fixes.get(j).getTimePoint().asMillis() + fixes.get(j - 1)
                                        .getTimePoint().asMillis()) / 2)).getNauticalMiles();
                        assertTrue(nauticalMilesFromIToHalfBeforeJ < distanceSumInNauticalMiles);
                        if (j < fixes.size() - 1) {
                            // now skip half a segment before the beginning:
                            double nauticalMilesFromIToHalfAfterJ = track.getRawDistanceTraveled(
                                    fixes.get(i).getTimePoint(),
                                    new MillisecondsTimePoint((fixes.get(j).getTimePoint().asMillis() + fixes
                                            .get(j + 1).getTimePoint().asMillis()) / 2)).getNauticalMiles();
                            assertTrue(nauticalMilesFromIToHalfAfterJ > distanceSumInNauticalMiles,
                                    "for i=" + i + ", j=" + j + ": " + nauticalMilesFromIToHalfAfterJ + ">"
                                            + distanceSumInNauticalMiles);
                        }
                    }
                }
            }
        } finally {
            track.unlockAfterRead();
        }
    }
    
    @Test
    public void testDistanceTraveledOnSmoothenedTrackThenAddingOutlier() {
        final Set<TimePoint> invalidationCalls = new HashSet<TimePoint>();
        final TimeRangeCache<Distance> distanceCache = new TimeRangeCache<Distance>("test-DistanceCache") {
            @Override
            public void invalidateAllAtOrLaterThan(TimePoint timePoint) {
                super.invalidateAllAtOrLaterThan(timePoint);
                invalidationCalls.add(timePoint);
            }
        };
        DynamicGPSFixTrack<Object, GPSFix> track = new DynamicGPSFixTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */ 30000l) {
            private static final long serialVersionUID = -7277196393160609503L;
            @Override
            protected TimeRangeCache<Distance> getDistanceCache() {
                return distanceCache;
            }
        };
        final int timeBetweenFixesInMillis = 1000;
        Bearing bearing = new DegreeBearingImpl(123);
        Speed speed = new KnotSpeedImpl(7);
        Position p = new DegreePosition(0, 0);
        final TimePoint now = MillisecondsTimePoint.now();
        TimePoint start = now;
        final int steps = 10;
        TimePoint next = null;
        for (int i=0; i<steps; i++) {
            GPSFix fix = new GPSFixImpl(p, start);
            track.addGPSFix(fix);
            next = start.plus(timeBetweenFixesInMillis);
            p = p.translateGreatCircle(bearing, speed.travel(start, next));
            start = next;
            bearing = new DegreeBearingImpl(bearing.getDegrees() + 1);
        }
        invalidationCalls.clear();
        assertEquals(speed.getMetersPerSecond()*(steps-1), track.getDistanceTraveled(now, start).getMeters(), 0.02);
        final com.sap.sse.common.Util.Pair<TimePoint, com.sap.sse.common.Util.Pair<TimePoint, Distance>> fullIntervalCacheEntry = distanceCache.getEarliestFromAndResultAtOrAfterFrom(now,  start);
        assertNotNull(fullIntervalCacheEntry); // no more entry for "to"-value start in cache
        assertEquals(start, fullIntervalCacheEntry.getA());
        assertEquals(now, fullIntervalCacheEntry.getB().getA());
        TimePoint timePointForOutlier = new MillisecondsTimePoint(now.asMillis() + ((int) steps/2) * timeBetweenFixesInMillis + timeBetweenFixesInMillis/2);
        Position outlierPosition = new DegreePosition(90, 90);
        GPSFix outlier = new GPSFixImpl(outlierPosition, timePointForOutlier);
        track.addGPSFix(outlier);
        assertEquals(1, invalidationCalls.size());
        TimePoint timePointOfLastFixBeforeOutlier = track.getLastFixBefore(timePointForOutlier).getTimePoint();
        assertTrue(invalidationCalls.iterator().next().after(timePointOfLastFixBeforeOutlier)); // outlier doesn't turn its preceding element into an outlier
        assertNull(distanceCache.getEarliestFromAndResultAtOrAfterFrom(now,  start)); // no more entry for "to"-value start in cache
        invalidationCalls.clear();
        final TimePoint timePointOfLastOriginalFix = track.getLastRawFix().getTimePoint();
        assertEquals(speed.getMetersPerSecond() * (steps - 1),
                track.getDistanceTraveled(now, timePointOfLastOriginalFix).getMeters(), 0.02);
        final com.sap.sse.common.Util.Pair<TimePoint, com.sap.sse.common.Util.Pair<TimePoint, Distance>> newFullIntervalCacheEntry = distanceCache
                .getEarliestFromAndResultAtOrAfterFrom(now, timePointOfLastOriginalFix);
        assertNotNull(newFullIntervalCacheEntry); // no more entry for "to"-value start in cache
        assertEquals(timePointOfLastOriginalFix, newFullIntervalCacheEntry.getA());
        assertEquals(now, newFullIntervalCacheEntry.getB().getA());
        TimePoint timePointForLateOutlier = new MillisecondsTimePoint(now.asMillis() + (steps-1)*timeBetweenFixesInMillis + timeBetweenFixesInMillis/2);
        Position lateOutlierPosition = new DegreePosition(-90, 90);
        GPSFix lateOutlier = new GPSFixImpl(lateOutlierPosition, timePointForLateOutlier);
        // adding the outlier invalidates the fix just before the outlier because it now has a single successor that is in range but not reachable
        track.addGPSFix(lateOutlier);
        assertEquals(1, invalidationCalls.size());
        TimePoint timePointOfLastRawFixBeforeLateOutlier = track.getLastRawFixBefore(timePointForLateOutlier).getTimePoint();
        // assert that adding the outlier invalidated the distance cache starting from the raw fix before the late outlier
        // because its validity changed from true to false
        assertTrue(invalidationCalls.iterator().next().equals(timePointOfLastRawFixBeforeLateOutlier));
        invalidationCalls.clear();
        // expect the invalidation to have started after the single cache entry, so the cache entry still has to be there:
        final com.sap.sse.common.Util.Pair<TimePoint, com.sap.sse.common.Util.Pair<TimePoint, Distance>> stillPresentFullIntervalCacheEntry = distanceCache
                .getEarliestFromAndResultAtOrAfterFrom(now, timePointOfLastOriginalFix);
        assertNull(stillPresentFullIntervalCacheEntry); // no more entry for "to"-value start in cache because new temporary outlier lies in previously cached interval
        GPSFix polishedLastFix = track.getLastFixBefore(new MillisecondsTimePoint(Long.MAX_VALUE)); // get the last smoothened fix...
        // ...which now is expected to be two fixes before lateOutlier because lateOutlier has previous fixes within the time range
        // but none of them is reachable with max speed, and the raw fix right before lateOutlier is currently temporarily an outlier too
        assertEquals(track.getLastRawFixBefore(timePointOfLastRawFixBeforeLateOutlier), polishedLastFix);
        track.lockForRead();
        try {
            assertEquals(steps-1, Util.size(track.getFixes())); // the lateOutlier and its predecessor are currently detected as outlier, so it's not steps+1 
            // which would include the lateOutlier, but it's already only steps-1 valid fixes.
        } finally {
            track.unlockAfterRead();
        }
        // now add another "normal" fix, making the lateOutlier really an outlier; invalidation starts 1ms after the last previously valid
        // fix before the fix added, or at the earliest fix that changes its validity, whichever is earlier. In this case the earlier time point
        // is 1ms after the last *valid* fix before the fix inserted because that's the fix two before the lateOutlier, and that fix and all other
        // earlier fixes don't change their validity
        GPSFix fix = new GPSFixImpl(p, start); // the "overshoot" from the previous loop can be used to generate the next "regular" fix
        track.addGPSFix(fix);
        assertEquals(1, invalidationCalls.size());
        // the lateOutlier's predecessor now is expected to have changed validity again, causing a distance cache invalidation at its time
        assertEquals(track.getLastFixBefore(timePointOfLastRawFixBeforeLateOutlier).getTimePoint().plus(1), invalidationCalls.iterator().next());
        assertTrue(timePointForLateOutlier.compareTo(fix.getTimePoint()) < 0);
        // expect the invalidation to have started at the fix before the outlier, leaving the previous result ending at the fix right before the outlier intact
        final com.sap.sse.common.Util.Pair<TimePoint, com.sap.sse.common.Util.Pair<TimePoint, Distance>> stillStillPresentFullIntervalCacheEntry = distanceCache
                .getEarliestFromAndResultAtOrAfterFrom(now, timePointOfLastOriginalFix);
        assertNull(stillStillPresentFullIntervalCacheEntry); // still nothing in the cache
        track.lockForRead();
        try {
            assertEquals(steps+1, Util.size(track.getFixes())); // the one "normal" late fix is added on top of the <steps> fixes, but the two outliers should now be removed
        } finally {
            track.unlockAfterRead();
        }
        GPSFix polishedLastFix2 = track.getLastFixBefore(new MillisecondsTimePoint(Long.MAX_VALUE)); // get the last smoothened fix...
        PositionAssert.assertGPSFixEquals(fix, polishedLastFix2, /* pos deg delta */ 0.000001);
        assertEquals(speed.getMetersPerSecond()*steps, track.getDistanceTraveled(now, start).getMeters(), 0.01);
    }
    
    /**
     * A test case for bug 968. If distances are requested for time intervals ending after the last GPS fix, the position
     * is estimated to be at the last fix known. When another fix arrives, the positions for these time points can now be
     * interpolated, leading to different results and hence to a cache inconsistency in case the cache isn't flushed for the
     * interval between the last and the newest fix. This test case adds a few fixes, then asks a distance traveled for an
     * interval ending after the last fix, then adds another fix at a later time point and asks the distance again.
     */
    @Test
    public void testMoreFrequentDistanceComputationThanGPSFixReception() {
        DynamicGPSFixTrack<Object, GPSFixMoving> track = new DynamicGPSFixMovingTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */ 30000l);
        final int timeBetweenFixesInMillis = 1000;
        Bearing bearing = new DegreeBearingImpl(123);
        SpeedWithBearing speed = new KnotSpeedWithBearingImpl(7, bearing);
        Position p = new DegreePosition(0, 0);
        final TimePoint now = MillisecondsTimePoint.now();
        TimePoint start = now;
        final int steps = 10;
        TimePoint next = null;
        GPSFixMoving fix = null;
        for (int i=0; i<steps; i++) {
            fix = new GPSFixMovingImpl(p, start, speed, /* optionalTrueHeading */ null);
            track.addGPSFix(fix);
            next = start.plus(timeBetweenFixesInMillis);
            p = p.translateGreatCircle(bearing, speed.travel(start, next));
            start = next;
            bearing = new DegreeBearingImpl(bearing.getDegrees() + 1);
        }
        Distance distance1 = track.getDistanceTraveled(now, next.minus(timeBetweenFixesInMillis));
        Distance distance2 = track.getDistanceTraveled(now, next.minus(2*timeBetweenFixesInMillis/3));
        Distance distance3 = track.getDistanceTraveled(now, next.minus(timeBetweenFixesInMillis/3));
        Distance distance4 = track.getDistanceTraveled(now, next);
        assertEquals(distance1, distance2); // no progress after time point "next" because no further fixes are known
        assertEquals(distance1, distance3); // no progress after time point "next" because no further fixes are known
        assertEquals(distance1, distance4); // no progress after time point "next" because no further fixes are known
        // now add one more fix
        fix = new GPSFixMovingImpl(p, start, speed, /* optionalTrueHeading */ null);
        track.addGPSFix(fix);
        Distance distance1_new = track.getDistanceTraveled(now, next.minus(timeBetweenFixesInMillis));
        Distance distance2_new = track.getDistanceTraveled(now, next.minus(2*timeBetweenFixesInMillis/3));
        Distance distance3_new = track.getDistanceTraveled(now, next.minus(timeBetweenFixesInMillis/3));
        Distance distance4_new = track.getDistanceTraveled(now, next);
        assertEquals(distance1, distance1_new);
        assertFalse(distance2.equals(distance2_new));
        assertFalse(distance3.equals(distance3_new));
        assertFalse(distance4.equals(distance4_new));
        Distance toReachInOneThirdOfTimeBetweenFixesInMillis = speed.travel(next, next.plus(timeBetweenFixesInMillis/3));
        assertEquals(toReachInOneThirdOfTimeBetweenFixesInMillis.getMeters(), distance2_new.getMeters()-distance1_new.getMeters(), 0.01);
        assertEquals(toReachInOneThirdOfTimeBetweenFixesInMillis.getMeters(), distance3_new.getMeters()-distance2_new.getMeters(), 0.01);
        assertEquals(toReachInOneThirdOfTimeBetweenFixesInMillis.getMeters(), distance4_new.getMeters()-distance3_new.getMeters(), 0.01);
    }

    /**
     * Bug 3022 describes a {@link StackOverflowError} that occurs during distance calculations on a {@link GPSFixTrackImpl}.
     * This test tries to reproduce this behavior, as a starting point for a fix. The recursion happened while a long-distance
     * race was running. Can there be a certain fix insertion pattern that, combined with a certain request pattern,
     * produces degenerated cache contents? The race had a live delay of 660s. The trackers were configured to sample every
     * 120s and to send every 600s. This could mean that for some 60s the live requests were "ahead" of the trackers.
     */
    @Test
    public void testDistanceCacheIncrementalFillUpAndRecursion() {
        TimePoint start = MillisecondsTimePoint.now();
        TimePoint fixTime = start;
        TimePoint queryTime = fixTime;
        Position p = new DegreePosition(0, 0);
        Bearing cog = new DegreeBearingImpl(123);
        final Duration queryInterval = Duration.ONE_SECOND;
        final Duration readAhead = Duration.ONE_MINUTE;
        final Duration samplingInterval = Duration.ONE_SECOND.times(120);
        final Duration transmissionInterval = Duration.ONE_MINUTE.times(10);
        final Duration raceDuration = Duration.ONE_HOUR.times(48);
        final Speed speed = new KnotSpeedImpl(12);
        final Distance distancePerSample = speed.travel(fixTime, fixTime.plus(samplingInterval));
        final TimeRangeCache<Distance> distanceCache = new TimeRangeCache<>("test-DistanceCache");
        DynamicGPSFixTrack<Object, GPSFix> track = new DynamicGPSFixTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */ 30000l) {
            private static final long serialVersionUID = -7277196393160609503L;
            @Override
            protected TimeRangeCache<Distance> getDistanceCache() {
                return distanceCache;
            }
        };
        while (fixTime.before(start.plus(raceDuration))) {
            for (int i=0; i<transmissionInterval.divide(samplingInterval); i++) {
                // produce a bulk of fixes
                GPSFix fix = new GPSFixImpl(p, fixTime);
                track.add(fix);
                fixTime = fixTime.plus(samplingInterval);
                p = p.translateGreatCircle(cog, distancePerSample);
            }
            Distance lastDistance = Distance.NULL;
            // now query the distance with constant "from" and increasing "to" where "to" moves until 60s after tp
            while (queryTime.before(fixTime.plus(readAhead))) {
                final Distance d = track.getDistanceTraveled(start, queryTime);
                assertTrue(d.compareTo(lastDistance) >= 0);
                lastDistance = d;
                queryTime = queryTime.plus(queryInterval);
            }
        }
        assertTrue(distanceCache.size() <= TimeRangeCache.MAX_SIZE);
    }
    
    @Test
    public void testDistanceCacheAccessForPartialStrip() {
        final Set<TimePoint> invalidationCalls = new HashSet<TimePoint>();
        final TimeRangeCache<Distance> distanceCache = new TimeRangeCache<Distance>("test-DistanceCache") {
            @Override
            public void invalidateAllAtOrLaterThan(TimePoint timePoint) {
                super.invalidateAllAtOrLaterThan(timePoint);
                invalidationCalls.add(timePoint);
            }
        };
        DynamicGPSFixTrack<Object, GPSFix> track = new DynamicGPSFixTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */ 30000l) {
            private static final long serialVersionUID = -7277196393160609503L;
            @Override
            protected TimeRangeCache<Distance> getDistanceCache() {
                return distanceCache;
            }
        };
        final int timeBetweenFixesInMillis = 1000;
        Bearing bearing = new DegreeBearingImpl(123);
        Speed speed = new KnotSpeedImpl(7);
        Position p = new DegreePosition(0, 0);
        final TimePoint now = MillisecondsTimePoint.now();
        TimePoint start = now;
        final int steps = 10;
        TimePoint next = null;
        for (int i=0; i<steps; i++) {
            GPSFix fix = new GPSFixImpl(p, start);
            track.addGPSFix(fix);
            next = start.plus(timeBetweenFixesInMillis);
            p = p.translateGreatCircle(bearing, speed.travel(start, next));
            start = next;
            bearing = new DegreeBearingImpl(bearing.getDegrees() + 1);
        }
        invalidationCalls.clear();
        final TimePoint stripFrom = now.plus(timeBetweenFixesInMillis);
        final TimePoint stripTo = start.minus(2*timeBetweenFixesInMillis);
        assertEquals(speed.getMetersPerSecond()*(steps-3),
                track.getDistanceTraveled(stripFrom, stripTo).getMeters(), 0.01);
        assertEquals(speed.getMetersPerSecond()*(steps-1), track.getDistanceTraveled(now, start).getMeters(), 0.02);
        assertTrue(invalidationCalls.isEmpty());
        // expect a cache entry exactly for the strip's boundaries
        com.sap.sse.common.Util.Pair<TimePoint, com.sap.sse.common.Util.Pair<TimePoint, Distance>> stripCacheEntry = distanceCache.getEarliestFromAndResultAtOrAfterFrom(stripFrom, stripTo);
        assertEquals(stripTo, stripCacheEntry.getA());
        assertEquals(stripFrom, stripCacheEntry.getB().getA());
        // expect a cache entry exactly for the full boundaries
        com.sap.sse.common.Util.Pair<TimePoint, com.sap.sse.common.Util.Pair<TimePoint, Distance>> fullCacheEntry = distanceCache.getEarliestFromAndResultAtOrAfterFrom(now, start);
        assertEquals(start, fullCacheEntry.getA());
        assertEquals(now, fullCacheEntry.getB().getA());
    }
    
    @Test
    public void testDistanceTraveledOnInBetweenSectionFromFixToFix() {
        track.lockForRead();
        try {
            // take second and third fix and compute distance between them
            Iterator<GPSFixMoving> iter = track.getRawFixes().iterator();
            iter.next(); // skip first;
            GPSFix second = iter.next();
            GPSFix third = iter.next();
            assertEquals(second.getPosition().getDistance(third.getPosition()),
                    track.getRawDistanceTraveled(second.getTimePoint(), third.getTimePoint()));
        } finally {
            track.unlockAfterRead();
        }
    }
    
    @Test
    public void testFarFutureFixNotUsedDuringEstimation() {
        TimePoint normalFixesTime = null;
        track.lockForRead();
        try {
            Iterator<GPSFixMoving> iter = track.getRawFixes().iterator();
            for (int i = 0; i < 2; i++) {
                normalFixesTime = iter.next().getTimePoint();
            }
            assertNotNull(normalFixesTime);
        } finally {
            track.unlockAfterRead();
        }
        SpeedWithBearing estimatedSpeed = track.getEstimatedSpeed(normalFixesTime);
        GPSFixMovingImpl gpsFixFarInTheFuture = new GPSFixMovingImpl(
                new DegreePosition(89, 180), new MillisecondsTimePoint(
                        System.currentTimeMillis()+10000000l), new KnotSpeedWithBearingImpl(200000, new DegreeBearingImpl(0)), /* optionalTrueHeading */ null);
        track.addGPSFix(gpsFixFarInTheFuture);
        Position estimatedPosNew = track.getEstimatedPosition(normalFixesTime, /* extrapolate */ false);
        // expecting to get the coordinates of gpsFix2's position
        assertEquals(gpsFix2.getPosition().getLatDeg(), estimatedPosNew.getLatDeg(), 0.5);
        assertEquals(gpsFix2.getPosition().getLngDeg(), estimatedPosNew.getLngDeg(), 0.5);
        SpeedWithBearing estimatedSpeedNew = track.getEstimatedSpeed(normalFixesTime);
        PositionAssert.assertSpeedEquals(estimatedSpeed, estimatedSpeedNew, /* bearing deg delta */ 0.1, /* knot speed delta */ 0.1);
    }
    
    @Test
    public void testValidCheckForFixThatSaysItsAsFastAsItWas() {
        DynamicGPSFixMovingTrackImpl<Boat> myTrack = new DynamicGPSFixMovingTrackImpl<Boat>(new BoatImpl("123", "MyFirstBoat", new BoatClassImpl("505", /* typicallyStartsUpwind */
                        true), null), /* millisecondsOverWhichToAverage */5000, /* no smoothening */null);
        TimePoint now1 = MillisecondsTimePoint.now();
        TimePoint now2 = addMillisToTimepoint(now1, 1000); // 1s
        AbstractBearing bearing = new DegreeBearingImpl(90);
        Position position1 = new DegreePosition(1, 2);
        Position position2 = position1.translateGreatCircle(bearing, new MeterDistance(1));
        GPSFixMovingImpl myGpsFix1 = new GPSFixMovingImpl(position1, now1, new MeterPerSecondSpeedWithDegreeBearingImpl(1, bearing), /* optionalTrueHeading */ null);
        GPSFixMovingImpl myGpsFix2 = new GPSFixMovingImpl(position2, now2, new MeterPerSecondSpeedWithDegreeBearingImpl(1, bearing), /* optionalTrueHeading */ null);
        myTrack.addGPSFix(myGpsFix1);
        myTrack.addGPSFix(myGpsFix2);
        myTrack.lockForRead();
        try {
            assertEquals(2, myTrack.getFixes().size()); // expecting both fixes to be valid
        } finally {
            myTrack.unlockAfterRead();
        }
    }
    
    @Test
    public void testValidCheckForFixThatSaysItsFasterThanItActuallyWas() {
        DynamicGPSFixMovingTrackImpl<Boat> myTrack = new DynamicGPSFixMovingTrackImpl<Boat>(new BoatImpl("123", "MyFirstBoat", new BoatClassImpl("505", /* typicallyStartsUpwind */
                        true), null), /* millisecondsOverWhichToAverage */5000, /* no smoothening */null);
        TimePoint now1 = MillisecondsTimePoint.now();
        TimePoint now2 = addMillisToTimepoint(now1, 1000); // 1s
        AbstractBearing bearing = new DegreeBearingImpl(90);
        Position position1 = new DegreePosition(1, 2);
        Position position2 = position1.translateGreatCircle(bearing, new MeterDistance(1));
        GPSFixMovingImpl myGpsFix1 = new GPSFixMovingImpl(position1, now1, new MeterPerSecondSpeedWithDegreeBearingImpl(10, bearing), /* optionalTrueHeading */ null);
        GPSFixMovingImpl myGpsFix2 = new GPSFixMovingImpl(position2, now2, new MeterPerSecondSpeedWithDegreeBearingImpl(10, bearing), /* optionalTrueHeading */ null);
        myTrack.addGPSFix(myGpsFix1);
        myTrack.addGPSFix(myGpsFix2);
        myTrack.lockForRead();
        try {
            assertTrue(myTrack.getFixes().size() < 2); // at least one fix must be classified as outlier
        } finally {
            myTrack.unlockAfterRead();
        }
    }
    
    @Test
    public void testValidCheckForFixThatSaysItsSlowerThanItActuallyWas() {
        DynamicGPSFixMovingTrackImpl<Boat> myTrack = new DynamicGPSFixMovingTrackImpl<Boat>(new BoatImpl("123", "MyFirstBoat", new BoatClassImpl("505", /* typicallyStartsUpwind */
                        true), null), /* millisecondsOverWhichToAverage */5000, /* no smoothening */null);
        TimePoint now1 = MillisecondsTimePoint.now();
        TimePoint now2 = addMillisToTimepoint(now1, 1000); // 1s
        AbstractBearing bearing = new DegreeBearingImpl(90);
        Position position1 = new DegreePosition(1, 2);
        Position position2 = position1.translateGreatCircle(bearing, new MeterDistance(1));
        GPSFixMovingImpl myGpsFix1 = new GPSFixMovingImpl(position1, now1, new MeterPerSecondSpeedWithDegreeBearingImpl(0.1, bearing), /* optionalTrueHeading */ null);
        GPSFixMovingImpl myGpsFix2 = new GPSFixMovingImpl(position2, now2, new MeterPerSecondSpeedWithDegreeBearingImpl(0.1, bearing), /* optionalTrueHeading */ null);
        myTrack.addGPSFix(myGpsFix1);
        myTrack.addGPSFix(myGpsFix2);
        myTrack.lockForRead();
        try {
            assertTrue(myTrack.getFixes().size() < 2); // at least one fix must be classified as outlier
        } finally {
            myTrack.unlockAfterRead();
        }
    }
    
    @Test
    public void testInvalidationIntervalBeginningForPositionEstimation() {
        track.lockForRead();
        final GPSFixMoving firstFixSoFar;
        try {
            firstFixSoFar = track.getFixes().iterator().next();
        } finally {
            track.unlockAfterRead();
        }
        assertNotNull(firstFixSoFar);
        TimePoint beginningOfTime = new MillisecondsTimePoint(0);
        Position positionAtBeginningOfTime = track.getEstimatedPosition(beginningOfTime, /* extrapolate */false);
        long timespan = 2 /* hours */ * 3600 /* seconds/hour */ * 1000 /* millis/s */;
        SpeedWithBearing speed = new KnotSpeedWithBearingImpl(45, new DegreeBearingImpl(123));
        TimePoint slightlyBeforeFirstFix = firstFixSoFar.getTimePoint().minus(timespan);
        Position newPosition = firstFixSoFar.getPosition().translateGreatCircle(speed.getBearing().reverse(),
                speed.travel(firstFixSoFar.getTimePoint(), firstFixSoFar.getTimePoint().plus(timespan)));
        GPSFixMoving newFirstFix = new GPSFixMovingImpl(newPosition, slightlyBeforeFirstFix, speed, /* optionalTrueHeading */ null);
        track.addGPSFix(newFirstFix);
        TimeRange intervalAffected = track.getEstimatedPositionTimePeriodAffectedBy(newFirstFix);
        Position newPositionAtBeginningOfTime = track.getEstimatedPosition(beginningOfTime, /* extrapolate */false);
        assertFalse(newPositionAtBeginningOfTime.equals(positionAtBeginningOfTime));
        assertTrue(!intervalAffected.from().after(beginningOfTime));
    }

    @Test
    public void testInvalidationIntervalEndForPositionEstimation() {
        TimePoint endOfTime = new MillisecondsTimePoint(Long.MAX_VALUE);
        final GPSFixMoving lastFixSoFar = track.getLastFixBefore(endOfTime);
        assertNotNull(lastFixSoFar);
        Position positionAtEndOfTime = track.getEstimatedPosition(endOfTime, /* extrapolate */false);
        long timespan = 2 /* hours */ * 3600 /* seconds/hour */ * 1000 /* millis/s */;
        SpeedWithBearing speed = new KnotSpeedWithBearingImpl(45, new DegreeBearingImpl(123));
        TimePoint slightlyAfterLastFix = lastFixSoFar.getTimePoint().plus(timespan);
        Position newPosition = lastFixSoFar.getPosition().translateGreatCircle(speed.getBearing(),
                speed.travel(lastFixSoFar.getTimePoint(), lastFixSoFar.getTimePoint().plus(timespan)));
        GPSFixMoving newLastFix = new GPSFixMovingImpl(newPosition, slightlyAfterLastFix, speed, /* optionalTrueHeading */ null);
        track.addGPSFix(newLastFix);
        TimeRange intervalAffected = track.getEstimatedPositionTimePeriodAffectedBy(newLastFix);
        Position newPositionAtEndOfTime = track.getEstimatedPosition(endOfTime, /* extrapolate */false);
        assertFalse(newPositionAtEndOfTime.equals(positionAtEndOfTime));
        assertTrue(!intervalAffected.to().before(endOfTime));
    }

    @Test
    public void testInvalidationIntervalForPositionEstimationForEmptyTrack() {
        TimePoint now = MillisecondsTimePoint.now();
        DynamicGPSFixMovingTrackImpl<Object> myTrack = new DynamicGPSFixMovingTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */5000);
        TimePoint beginningOfTime = new MillisecondsTimePoint(0);
        Position positionAtBeginningOfTime = myTrack.getEstimatedPosition(beginningOfTime, /* extrapolate */false);
        TimePoint endOfTime = new MillisecondsTimePoint(Long.MAX_VALUE);
        Position positionAtEndOfTime = myTrack.getEstimatedPosition(endOfTime, /* extrapolate */false);
        assertNull(positionAtBeginningOfTime);
        assertNull(positionAtEndOfTime);
        GPSFixMoving newFix = new GPSFixMovingImpl(new DegreePosition(12, 34), now, new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
        myTrack.addGPSFix(newFix);
        TimeRange intervalAffected = myTrack.getEstimatedPositionTimePeriodAffectedBy(newFix);
        Position newPositionAtBeginningOfTime = myTrack.getEstimatedPosition(beginningOfTime, /* extrapolate */false);
        Position newPositionAtEndOfTime = myTrack.getEstimatedPosition(now, /* extrapolate */false);
        assertFalse(newPositionAtBeginningOfTime.equals(positionAtBeginningOfTime));
        assertTrue(!intervalAffected.from().after(beginningOfTime));
        assertFalse(newPositionAtEndOfTime.equals(positionAtEndOfTime));
        assertTrue(!intervalAffected.to().before(endOfTime));
    }

    @Test
    public void testSpeedEstimationForCourseChangeAtFix() throws ParseException {
        /*
         * See bug 1065, first test data set: [2011-06-23T16:08:27.000+0200: (54.49155,10.184947) with
         * 5.399568034557236kn to 291.0ï¿½, 2011-06-23T16:08:31.000+0200: (54.491597,10.184808) with 5.939524838012959kn
         * to 296.0ï¿½, 2011-06-23T16:08:37.000+0200: (54.491572999999995,10.184669999999999) with 3.7796976241900646kn
         * to 210.0ï¿½, 2011-06-23T16:08:39.000+0200: (54.491523,10.184602) with 5.939524838012959kn to 219.0ï¿½,
         * 2011-06-23T16:08:43.000+0200: (54.491457999999994,10.18451) with 4.859611231101512kn to 225.0ï¿½,
         * 2011-06-23T16:08:45.000+0200: (54.491386999999996,10.184455) with 5.939524838012959kn to 196.0ï¿½]
         * 
         * let the track estimate the speed at 16:08:37 at
         * 
         * 5.469864974239993kn to 215.50791079454874ï¿½
         */
        DynamicGPSFixMovingTrackImpl<Object> myTrack = new DynamicGPSFixMovingTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */5000);
        GPSFixMoving fix1 = createFix("2011-06-23T16:08:27.000+0200", 54.49155, 10.184947, 5.399568034557236, 291.0);
        myTrack.addGPSFix(fix1);
        GPSFixMoving fix2 = createFix("2011-06-23T16:08:31.000+0200", 54.491597,10.184808, 5.939524838012959, 296.0);
        myTrack.addGPSFix(fix2);
        GPSFixMoving fix3 = createFix("2011-06-23T16:08:37.000+0200", 54.491572999999995,10.184669999999999, 3.7796976241900646, 210.0);
        myTrack.addGPSFix(fix3);
        GPSFixMoving fix4 = createFix("2011-06-23T16:08:39.000+0200", 54.491523, 10.184602, 5.939524838012959, 219.0);
        myTrack.addGPSFix(fix4);
        GPSFixMoving fix5 = createFix("2011-06-23T16:08:43.000+0200", 54.491457999999994, 10.18451, 4.859611231101512, 225.0);
        myTrack.addGPSFix(fix5);
        GPSFixMoving fix6 = createFix("2011-06-23T16:08:45.000+0200", 54.491386999999996, 10.184455, 5.939524838012959, 196.0);
        myTrack.addGPSFix(fix6);
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        SpeedWithBearing estimatedSpeedWithBearing = myTrack.getEstimatedSpeed(new MillisecondsTimePoint(dateFormatter.parse("2011-06-23T16:08:37.000+0200")));
        assertEquals(225., estimatedSpeedWithBearing.getBearing().getDegrees(), 5);
        assertEquals(5.0, estimatedSpeedWithBearing.getKnots(), 0.2);
    }

    @Test
    public void testSpeedEstimationForCourseChangeBetweenFixes() throws ParseException {
        /*
         * See bug 1065, first test data set: [2012-10-21T15:00:32.000+0200: (43.692862999999996,7.267875) with
         * 16.73866090712743kn to 294.0Â°, 2012-10-21T15:00:34.000+0200: (43.692937,7.267683) with 16.73866090712743kn
         * to 299.0Â°, 2012-10-21T15:00:36.000+0200: (43.693017,7.267487999999999) with 16.73866090712743kn to 300.0Â°,
         * 2012-10-21T15:00:38.000+0200: (43.693103,7.267297999999999) with 16.73866090712743kn to 300.0Â°,
         * 2012-10-21T15:00:40.000+0200: (43.693191999999996,7.267112) with 16.73866090712743kn to 303.0Â°,
         * 2012-10-21T15:00:43.000+0200: (43.693283,7.2669179999999995) with 18.898488120950326kn to 302.0Â°]
         * 2012-10-21T15:00:51.000+0200: (43.693245,7.266183) with 11.339092872570195kn to 237.0Â°
         */
        DynamicGPSFixMovingTrackImpl<Object> myTrack = new DynamicGPSFixMovingTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */5000);
        GPSFixMoving fix1 = createFix("2012-10-21T15:00:32.000+0200", 43.692862999999996,7.267875, 16.73866090712743, 294.0);
        myTrack.addGPSFix(fix1);
        GPSFixMoving fix2 = createFix("2012-10-21T15:00:34.000+0200", 43.692937, 7.267683, 16.73866090712743, 299.0);
        myTrack.addGPSFix(fix2);
        GPSFixMoving fix3 = createFix("2012-10-21T15:00:36.000+0200", 43.693017, 7.267487999999999, 16.73866090712743, 300.0);
        myTrack.addGPSFix(fix3);
        GPSFixMoving fix4 = createFix("2012-10-21T15:00:38.000+0200", 43.693103, 7.267297999999999, 16.73866090712743, 300.0);
        myTrack.addGPSFix(fix4);
        GPSFixMoving fix5 = createFix("2012-10-21T15:00:40.000+0200", 43.693191999999996, 7.267112, 16.73866090712743, 303.0);
        myTrack.addGPSFix(fix5);
        GPSFixMoving fix6 = createFix("2012-10-21T15:00:43.000+0200", 43.693283, 7.2669179999999995, 18.898488120950326, 302.0);
        myTrack.addGPSFix(fix6);
        GPSFixMoving fix7 = createFix("2012-10-21T15:00:51.000+0200", 43.693245, 7.266183, 11.339092872570195, 237.0);
        myTrack.addGPSFix(fix7);
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        SpeedWithBearing estimatedSpeedWithBearing = myTrack.getEstimatedSpeed(new MillisecondsTimePoint(dateFormatter.parse("2012-10-21T15:00:45.000+0200")));
        assertEquals(275., estimatedSpeedWithBearing.getBearing().getDegrees(), 5);
        assertEquals(15.9, estimatedSpeedWithBearing.getKnots(), 0.2);
    }

    private GPSFixMoving createFix(String isoDateTime, double lat, double lng, double knotSpeed, double bearingDeg) throws ParseException {
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        TimePoint timePoint = new MillisecondsTimePoint(dateFormatter.parse(isoDateTime));
        Position position = new DegreePosition(lat, lng);
        SpeedWithBearing speed = new KnotSpeedWithBearingImpl(knotSpeed, new DegreeBearingImpl(bearingDeg));
        GPSFixMoving fix = new GPSFixMovingImpl(position, timePoint, speed, /* optionalTrueHeading */ null);
        return fix;
    }
    
    @Test
    public void testFrequency() {
        assertEquals(MILLIS_BETWEEN_FIXES, track.getAverageIntervalBetweenFixes().asMillis());
        assertEquals(MILLIS_BETWEEN_FIXES, track.getAverageIntervalBetweenRawFixes().asMillis());
    }
    
    @Test
    public void testEstimatedSpeedCaching() throws CompactionNotPossibleException {
        GPSFixMoving originalFix = new GPSFixMovingImpl(new DegreePosition(12, 34), MillisecondsTimePoint.now(),
                new KnotSpeedWithBearingImpl(9, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null);
        GPSFixMoving fix = new VeryCompactGPSFixMovingImpl(originalFix);
        assertFalse(fix.isEstimatedSpeedCached());
        final KnotSpeedWithBearingImpl estimatedSpeed = new KnotSpeedWithBearingImpl(9.1, new DegreeBearingImpl(124));
        fix.cacheEstimatedSpeed(estimatedSpeed);
        assertTrue(fix.isEstimatedSpeedCached());
        SpeedWithBearing cachedEstimatedSpeed = fix.getCachedEstimatedSpeed();
        PositionAssert.assertSpeedEquals(estimatedSpeed, cachedEstimatedSpeed, /* bearing deg delta */ 0.1, /* knot speed delta */ 0.1);
        fix.invalidateEstimatedSpeedCache();
        assertFalse(fix.isEstimatedSpeedCached());
    }
    
    @Test
    public void testEstimatedSpeedCachingOnTrack() {
        GPSFixMoving compactFix3 = track.getFirstFixAtOrAfter(gpsFix3.getTimePoint());
        assertFalse(compactFix3.isEstimatedSpeedCached());
        SpeedWithBearing fix3EstimatedSpeed = track.getEstimatedSpeed(gpsFix3.getTimePoint());
        assertTrue(compactFix3.isEstimatedSpeedCached());
        PositionAssert.assertSpeedEquals(fix3EstimatedSpeed, compactFix3.getCachedEstimatedSpeed(), /* bearing deg delta */ 0.1, /* knot speed delta */ 0.1);
        PositionAssert.assertSpeedEquals(fix3EstimatedSpeed, track.getEstimatedSpeed(gpsFix3.getTimePoint()),
                /* bearing deg delta */ 0.1, /* knot speed delta */ 0.1); // fetch again from the cache
        // assuming that all test fixes are within a few milliseconds and the averaging interval is much larger than that,
        // adding a single fix in the middle should invalidate the cache
        track.add(new GPSFixMovingImpl(gpsFix3.getPosition(), gpsFix3.getTimePoint().plus(1), gpsFix3.getSpeed(), /* optionalTrueHeading */ null));
        assertFalse(compactFix3.isEstimatedSpeedCached());
    }
}
