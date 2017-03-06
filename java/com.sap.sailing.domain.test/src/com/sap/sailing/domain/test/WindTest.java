package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.confidence.Weigher;
import com.sap.sailing.domain.common.confidence.impl.PositionAndTimePointWeigher;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.tracking.impl.CompactWindImpl;
import com.sap.sailing.domain.common.tracking.impl.CompactionNotPossibleException;
import com.sap.sailing.domain.confidence.ConfidenceBasedWindAverager;
import com.sap.sailing.domain.confidence.ConfidenceFactory;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sailing.domain.tracking.impl.WindTrackImpl;
import com.sap.sailing.domain.tracking.impl.WindWithConfidenceImpl;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class WindTest {
    private static final int AVERAGING_INTERVAL_MILLIS = 30000 /* 30s averaging interval */;

    /**
     * Tests that averaging also works across the 0deg mark
     */
    @Test
    public void testAveragingWind() throws InterruptedException {
        WindTrack track = new WindTrackImpl(AVERAGING_INTERVAL_MILLIS, /* useSpeed */ true, "TestWindTrack");
        TimePoint t1 = MillisecondsTimePoint.now();
        TimePoint t2 = new MillisecondsTimePoint(t1.asMillis()+10);
        TimePoint middle = new MillisecondsTimePoint((t1.asMillis()+t2.asMillis())/2);
        DegreePosition pos = new DegreePosition(0, 0);
        Wind wind1 = new WindImpl(pos, t1, new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(355)));
        Wind wind2 = new WindImpl(pos, t2, new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(5)));
        track.add(wind1);
        track.add(wind2);
        Wind average = track.getAveragedWind(pos, middle);
        PositionAssert.assertBearingEquals(new DegreeBearingImpl(0), average.getBearing().getDifferenceTo(new DegreeBearingImpl(0)), 0.01);
    }
    
    @Test
    public void testMultipleWindFixesWithSameTimestampInSameWindTrack() {
        WindTrack track = new WindTrackImpl(AVERAGING_INTERVAL_MILLIS, /* useSpeed */ true, "TestWindTrack");
        TimePoint now = MillisecondsTimePoint.now();
        DegreePosition pos1 = new DegreePosition(0, 0);
        DegreePosition pos2 = new DegreePosition(1, 1);
        Wind wind1 = new WindImpl(pos1, now, new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(0)));
        Wind wind2 = new WindImpl(pos2, now, new KnotSpeedWithBearingImpl(20, new DegreeBearingImpl(0)));
        track.add(wind1);
        track.add(wind2);
        track.lockForRead();
        try {
            assertEquals(2, Util.size(track.getFixes()));
            Set<Wind> expectedWind = new HashSet<Wind>();
            expectedWind.add(wind1);
            expectedWind.add(wind2);
            Set<Wind> actualWind = new HashSet<Wind>();
            for (Wind wind : track.getFixes()) {
                actualWind.add(wind);
            }
            PositionAssert.assertWindEquals(expectedWind, actualWind, /* pos deg delta */ 0.000001, /* bearing deg delta */ 0.1, /* knot speed delta */ 0.1);
        } finally {
            track.unlockAfterRead();
        }
    }
    
    @Test
    public void testWindEquality() throws CompactionNotPossibleException {
        Position p1 = new DegreePosition(1, 2);
        Position p2 = new DegreePosition(4, 5);
        TimePoint t1 = MillisecondsTimePoint.now();
        TimePoint t2 = t1.plus(5);
        SpeedWithBearing s1 = new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(3));
        SpeedWithBearing s2 = new KnotSpeedWithBearingImpl(22, new DegreeBearingImpl(123));
        Wind w1 = new WindImpl(p1, t1, s1);
        Wind w2 = new CompactWindImpl(w1);
        PositionAssert.assertWindEquals(w1, w2, /* pos deg delta */ 0.000001, /* bearing deg delta */ 0.1, /* knot speed delta */ 0.1);
        assertEquals(w1.hashCode(), w2.hashCode());
        Wind w3 = new WindImpl(p2, t2, s2);
        assertFalse(w1.equals(w3));
        assertFalse(w2.equals(w3));
    }
    
    @Test
    public void testEmptyTrackYieldsNullAsWindEstimate() {
        WindTrack track = new WindTrackImpl(AVERAGING_INTERVAL_MILLIS, /* useSpeed */ true, "TestWindTrack");
        assertNull(track.getAveragedWind(new DegreePosition(0, 0), MillisecondsTimePoint.now()));
    }

    /**
     * If the wind track has areas with no data, and wind information is requested for such an interval,
     * it is essential to still average over the {@link #AVERAGING_INTERVAL_MILLIS} interval, even if the
     * interval is further away than {@link #AVERAGING_INTERVAL_MILLIS}.
     */
    @Test
    public void testAveragingOfSparseWindTrack() {
        WindTrack track = new WindTrackImpl(AVERAGING_INTERVAL_MILLIS, /* useSpeed */ true, "TestWindTrack");
        DegreePosition pos = new DegreePosition(0, 0);
        Wind wind1 = new WindImpl(pos, new MillisecondsTimePoint(0), new KnotSpeedWithBearingImpl(20, new DegreeBearingImpl(0)));
        Wind wind2 = new WindImpl(pos, new MillisecondsTimePoint(1000), new KnotSpeedWithBearingImpl(20, new DegreeBearingImpl(0)));
        Wind wind3 = new WindImpl(pos, new MillisecondsTimePoint(2000), new KnotSpeedWithBearingImpl(20, new DegreeBearingImpl(0)));
        Wind wind4 = new WindImpl(pos, new MillisecondsTimePoint(10000), new KnotSpeedWithBearingImpl(20, new DegreeBearingImpl(0)));
        Wind wind5 = new WindImpl(pos, new MillisecondsTimePoint(30000), new KnotSpeedWithBearingImpl(20, new DegreeBearingImpl(0)));
        Wind wind6 = new WindImpl(pos, new MillisecondsTimePoint(40000), new KnotSpeedWithBearingImpl(130, new DegreeBearingImpl(0)));
        Wind wind7 = new WindImpl(pos, new MillisecondsTimePoint(50000), new KnotSpeedWithBearingImpl(170, new DegreeBearingImpl(0)));
        track.add(wind1);
        track.add(wind2);
        track.add(wind3);
        track.add(wind4);
        track.add(wind5);
        track.add(wind6);
        track.add(wind7);
        
        // interval does bearely reach 20's burst because 0 has 0 length and 1000..30000 has 29000 length
        PositionAssert.assertSpeedEquals(new KnotSpeedImpl(20), track.getAveragedWind(pos, new MillisecondsTimePoint(1)), 0.01);
        // interval uses the two fixes to the left (0, 1000)=1000 and three to the right (2000, 10000, 30000)=28000
        PositionAssert.assertSpeedEquals(new KnotSpeedImpl(20), track.getAveragedWind(pos, new MillisecondsTimePoint(1001)), 0.01);
        // in the middle of the "hole", fetches (0, 1000, 2000, 10000)=10000 and (30000, 40000)=10000, so 20000ms worth of wind
        final double averageFor20000 = track.getAveragedWind(pos, new MillisecondsTimePoint(20000)).getKnots();
        // value is hard to predict exactly because time difference-based confidences rate fixes closer to 20000ms higher than those further away
        assertEquals(35, averageFor20000, 5);
        // right of the middle of the "hole", fetches (0, 1000, 2000, 10000)=10000 and (30000, 40000, 50000)=20000
        final double averageFor20500 = track.getAveragedWind(pos, new MillisecondsTimePoint(20500)).getKnots();
        assertEquals(37, averageFor20500, 5);
        assertTrue(averageFor20500 > averageFor20000);
    }
    
    @Test
    public void testSingleElementWindTrack() {
        WindTrack track = new WindTrackImpl(AVERAGING_INTERVAL_MILLIS, /* useSpeed */ true, "TestWindTrack");
        DegreePosition pos = new DegreePosition(0, 0);
        Wind wind = new WindImpl(pos, new MillisecondsTimePoint(0), new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(123)));
        track.add(wind);
        Wind estimate = track.getAveragedWind(pos, new MillisecondsTimePoint(0));
        PositionAssert.assertSpeedEquals(new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(123)), estimate, /* deg bearing delta */ 0.1, /* knot speed delta */ 0.01);
    }

    @Test
    public void testSingleElementExtrapolation() {
        WindTrack track = new WindTrackImpl(30000 /* 30s averaging interval */, /* useSpeed */ true, "TestWindTrack");
        DegreePosition pos = new DegreePosition(0, 0);
        Wind wind = new WindImpl(pos, new MillisecondsTimePoint(0), new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(123)));
        track.add(wind);
        // we only have one measurement; this should be extrapolated because it's our best guess
        Wind estimate = track.getAveragedWind(pos, new MillisecondsTimePoint(1000));
        PositionAssert.assertSpeedEquals(new KnotSpeedImpl(10), estimate, 0.01);
        PositionAssert.assertBearingEquals(new DegreeBearingImpl(123), estimate.getBearing(), 0.01);
    }

    @Test
    public void testSingleElementExtrapolationBeyondThreshold() {
        WindTrack track = new WindTrackImpl(30000 /* 30s averaging interval */, /* useSpeed */ true, "TestWindTrack");
        DegreePosition pos = new DegreePosition(0, 0);
        Wind wind = new WindImpl(pos, new MillisecondsTimePoint(0), new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(123)));
        track.add(wind);
        // we only have one measurement; this should be extrapolated because it's our best guess even if
        // the last measurement was longer ago than our smoothening interval
        Wind estimate = track.getAveragedWind(pos, new MillisecondsTimePoint(AVERAGING_INTERVAL_MILLIS+1000));
        PositionAssert.assertSpeedEquals(new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(123)), estimate, /* deg bearing delta */ 0.1, /* knot speed delta */ 0.01);
    }

    @Test
    public void testTwoElementWindTrackSameBearing() {
        WindTrack track = new WindTrackImpl(30000 /* 30s averaging interval */, /* useSpeed */ true, "TestWindTrack");
        DegreePosition pos = new DegreePosition(0, 0);
        Wind wind1 = new WindImpl(pos, new MillisecondsTimePoint(0), new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(100)));
        track.add(wind1);
        Wind wind2 = new WindImpl(pos, new MillisecondsTimePoint(1000), new KnotSpeedWithBearingImpl(20, new DegreeBearingImpl(100)));
        track.add(wind2);
        Wind estimate = track.getAveragedWind(pos, new MillisecondsTimePoint(500));
        PositionAssert.assertSpeedEquals(new KnotSpeedWithBearingImpl(15, new DegreeBearingImpl(100)), estimate, /* deg bearing delta */ 0.1, /* knot speed delta */ 0.01);
    }

    @Test
    public void testTwoElementWindTrackDifferentBearing() {
        WindTrack track = new WindTrackImpl(30000 /* 30s averaging interval */, /* useSpeed */ true, "TestWindTrack");
        DegreePosition pos = new DegreePosition(0, 0);
        Wind wind1 = new WindImpl(pos, new MillisecondsTimePoint(0), new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(110)));
        track.add(wind1);
        Wind wind2 = new WindImpl(pos, new MillisecondsTimePoint(1000), new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(100)));
        track.add(wind2);
        Wind estimate = track.getAveragedWind(pos, new MillisecondsTimePoint(2000));
        PositionAssert.assertSpeedEquals(new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(105)), estimate, /* deg bearing delta, some tolerance needed because of time-based confidence */ 0.6, /* knot speed delta */ 0.01);
    }
    
    @Test
    public void testUsingNewerThanRequestedIfCloserThanOlder() throws ParseException {
        /*
           Imagine the following wind measurements:
           
        2009-07-11T13:45:00.000+0200@null: 10.0kn from 278.0� avg(30000ms): 2009-07-11T13:45:00.000+0200@null: 10.0kn from 278.0°
        2009-07-11T13:45:05.000+0200@null: 10.0kn from 265.0� avg(30000ms): 2009-07-11T13:45:05.000+0200@null: 10.0kn from 265.0°
        2009-07-12T17:31:40.000+0200@null: 10.0kn from 260.0� avg(30000ms): 2009-07-12T17:31:40.000+0200@null: 10.0kn from 260.0°
        
           Now assume a query for 2009-07-12T17:31:38 which is closest to the newest entry but (much) more than
           the averaging interval after the previous entry (2009-07-11T13:45:05.000). This test ensures that
           the WindTrack uses the newer entry even though it's after the time point requested because it's
           much closer, and the previous entry would be out of the averaging interval anyway.
        */
        SimpleDateFormat df = new SimpleDateFormat("yyyy-DD-mm'T'hh:mm:ss");
        Wind wind1 = new WindImpl(null, new MillisecondsTimePoint(df.parse("2009-07-11T13:45:00").getTime()),
                new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(98)));
        Wind wind2 = new WindImpl(null, new MillisecondsTimePoint(df.parse("2009-07-11T13:45:05").getTime()),
                new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(85)));
        Wind wind3 = new WindImpl(null, new MillisecondsTimePoint(df.parse("2009-07-11T17:31:40").getTime()),
                new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(80)));
        WindTrack track = new WindTrackImpl(/* millisecondsOverWhichToAverage */ 30000, /* useSpeed */ true, "TestWindTrack");
        track.add(wind1);
        track.add(wind2);
        track.add(wind3);
        TimePoint timePoint = new MillisecondsTimePoint(df.parse("2009-07-11T17:31:38").getTime());
        Wind result = track.getAveragedWind(null, timePoint);
        // expectation: take two from left (because they are closer than AVERAGING_INTERVAL_MILLIS apart), one from right side:
        assertEquals((wind1.getKnots() + wind2.getKnots() + wind3.getKnots()) / 3, result.getKnots(), 0.01);
        assertEquals(80., result.getBearing().getDegrees(), 5);
    }
    
    @Test
    public void testWindwardDistanceForReachingLeg() {
        // TODO construct a tiny course with one upwind, one reaching and one downwind leg, a race with two competitors;
        // TODO create a tracked race with corresponding WEB wind source; put leader in downwind, trailer in upwind and
        // TODO test that windward distance includes the full reaching leg's length
    }
    
    /**
     * See bug 943. Wind estimation should return <code>null</code> instead of causing exceptions in case there
     * is no wind data to bootstrap the estimator with.
     */
    @Test
    public void testWindEstimationReturnsNullIfNoUpwindStartAndNoOtherWindDataAvailable() {
        DomainFactory domainFactory = DomainFactory.INSTANCE;
        Mark startFinishLeft = domainFactory.getOrCreateMark("Start/Finish left");
        Mark startFinishRight = domainFactory.getOrCreateMark("Start/Finish right");
        ControlPoint startFinish = domainFactory.createControlPointWithTwoMarks(startFinishLeft, startFinishRight, "Start/Finish");
        ControlPoint top = domainFactory.getOrCreateMark("Top");
        Waypoint w1 = domainFactory.createWaypoint(startFinish, /*passingInstruction*/ null);
        Waypoint w2 = domainFactory.createWaypoint(top, /*passingInstruction*/ null);
        Waypoint w3 = domainFactory.createWaypoint(startFinish, /*passingInstruction*/ null);
        Competitor competitor = new CompetitorImpl(123, "Test Competitor", Color.RED, null, null, new TeamImpl("STG", Collections.singleton(
                        new PersonImpl("Test Competitor", new NationalityImpl("GER"),
                        /* dateOfBirth */null, "This is famous " + "Test Competitor")), new PersonImpl("Rigo van Maas",
                        new NationalityImpl("NED"),
                        /* dateOfBirth */null, "This is Rigo, the coach")), new BoatImpl("Test Competitor" + "'s boat",
                new BoatClassImpl("505", /* typicallyStartsUpwind */true), null), /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        final BoatClass boatClass = domainFactory.getOrCreateBoatClass("ESS40");
        DynamicTrackedRace trackedRace = new DynamicTrackedRaceImpl(new DynamicTrackedRegattaImpl(
                new RegattaImpl(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE,
                RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()), boatClass, /*startDate*/ null, /*endDate*/ null,
                	/* trackedRegattaRegistry */ null, domainFactory.createScoringScheme(ScoringSchemeType.LOW_POINT), "123", null)),
                new RaceDefinitionImpl("Test Race",
                        new CourseImpl("Test Course", Arrays.asList(new Waypoint[] { w1, w2, w3 })),
                        boatClass, Collections.singleton(competitor)), Collections.<Sideline> emptyList(),
                EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 1000,
                        /* millisecondsOverWhichToAverageWind */ 30000,
                        /* millisecondsOverWhichToAverageSpeed */ 30000, /*useMarkPassingCalculator*/ false, OneDesignRankingMetric::new,
                        mock(RaceLogResolver.class));
        TimePoint start = MillisecondsTimePoint.now();
        TimePoint topMarkRounding = start.plus(30000);
        TimePoint finish = topMarkRounding.plus(30000);
        trackedRace.updateMarkPassings(competitor, Arrays.asList(new MarkPassing[] {
            new MarkPassingImpl(start, w1, competitor),    
            new MarkPassingImpl(topMarkRounding, w2, competitor),    
            new MarkPassingImpl(finish, w3, competitor)    
        }));
        assertFalse(boatClass.typicallyStartsUpwind());
        assertNull(trackedRace.getEstimatedWindDirection(MillisecondsTimePoint.now()));
    }

    @Test
    public void testWindAveragingBasedOnPosition() {
        Weigher<Pair<Position, TimePoint>> timeWeigherThatPretendsToAlsoWeighPositions = new PositionAndTimePointWeigher(
        /* halfConfidenceAfterMilliseconds */10000l, new MeterDistance(1000));
        ConfidenceBasedWindAverager<Pair<Position, TimePoint>> averager = ConfidenceFactory.INSTANCE
                .createWindAverager(timeWeigherThatPretendsToAlsoWeighPositions);
        TimePoint now = MillisecondsTimePoint.now();
        final DegreePosition p1 = new DegreePosition(0, 0);
        WindWithConfidence<Pair<Position, TimePoint>> w1 = new WindWithConfidenceImpl<>(
                new WindImpl(p1, now, new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(90))),
                /* confidence */ 1.0, new Pair<Position, TimePoint>(p1, now), /* useSpeed */ true);
        final Position p2 = p1.translateGreatCircle(new DegreeBearingImpl(0), new MeterDistance(1000));
        WindWithConfidence<Pair<Position, TimePoint>> w2 = new WindWithConfidenceImpl<>(
                new WindImpl(p2, now, new KnotSpeedWithBearingImpl(20, new DegreeBearingImpl(0))),
                /* confidence */ 1.0, new Pair<Position, TimePoint>(p2, now), /* useSpeed */ true);
        List<WindWithConfidence<Pair<Position, TimePoint>>> fixes = new ArrayList<>();
        fixes.add(w1);
        fixes.add(w2);
        WindWithConfidence<Pair<Position, TimePoint>> averageAtP1 = averager.getAverage(fixes, new Pair<Position, TimePoint>(p1, now));
        WindWithConfidence<Pair<Position, TimePoint>> averageAtP2 = averager.getAverage(fixes, new Pair<Position, TimePoint>(p2, now));
        // first careful assertion: at p1 where wind is from the west (to the east), wind should be further from the
        // west (further to the east) than at p2 where wind is from the south (to the north).
        final Bearing averageAtP1Bearing = averageAtP1.getObject().getBearing();
        assertTrue(Math.abs(averageAtP1Bearing.getDifferenceTo(w1.getObject().getBearing()).getDegrees()) <
                Math.abs(averageAtP1Bearing.getDifferenceTo(w2.getObject().getBearing()).getDegrees()));
        // ...and vice versa
        final Bearing averageAtP2Bearing = averageAtP2.getObject().getBearing();
        assertTrue(Math.abs(averageAtP2Bearing.getDifferenceTo(w2.getObject().getBearing()).getDegrees()) <
                Math.abs(averageAtP2Bearing.getDifferenceTo(w1.getObject().getBearing()).getDegrees()));
        // expect the arithmetic average at the point which is in the middle between the two positions
        Position middleBetweenP1AndP2 = p1.translateGreatCircle(p1.getBearingGreatCircle(p2), p1.getDistance(p2).scale(.5));
        WindWithConfidence<Pair<Position, TimePoint>> averageAtMiddleBetweenP1AndP2 = averager.getAverage(fixes,
                new Pair<Position, TimePoint>(middleBetweenP1AndP2, now));
        assertEquals(15, averageAtMiddleBetweenP1AndP2.getObject().getKnots(), 0.00001);
        assertEquals(45, averageAtMiddleBetweenP1AndP2.getObject().getBearing().getDegrees(), 0.00001);
        
        // now try again with distance 2km and demand the difference between the position-dependent readings to be greater
        final Position p3 = p1.translateGreatCircle(new DegreeBearingImpl(0), new MeterDistance(2000));
        WindWithConfidence<Pair<Position, TimePoint>> w3 = new WindWithConfidenceImpl<>(
                new WindImpl(p3, now, new KnotSpeedWithBearingImpl(20, new DegreeBearingImpl(0))),
                /* confidence */ 1.0, new Pair<Position, TimePoint>(p3, now), /* useSpeed */ true);
        List<WindWithConfidence<Pair<Position, TimePoint>>> fixesFurtherApart = new ArrayList<>();
        fixesFurtherApart.add(w1);
        fixesFurtherApart.add(w3);
        WindWithConfidence<Pair<Position, TimePoint>> averageFurtherApartAtP1 = averager.getAverage(fixesFurtherApart, new Pair<Position, TimePoint>(p1, now));
        WindWithConfidence<Pair<Position, TimePoint>> averageAtP3 = averager.getAverage(fixesFurtherApart, new Pair<Position, TimePoint>(p3, now));
        assertTrue(averageFurtherApartAtP1.getObject().getKnots() < averageAtP1.getObject().getKnots());
        assertTrue(averageAtP3.getObject().getKnots() > averageAtP2.getObject().getKnots());
        assertTrue(Math.abs(averageFurtherApartAtP1.getObject().getBearing().getDifferenceTo(w1.getObject().getBearing()).getDegrees())
                < Math.abs(averageAtP1.getObject().getBearing().getDifferenceTo(w1.getObject().getBearing()).getDegrees()));
        assertTrue(Math.abs(averageAtP3.getObject().getBearing().getDifferenceTo(w3.getObject().getBearing()).getDegrees())
                < Math.abs(averageAtP2.getObject().getBearing().getDifferenceTo(w3.getObject().getBearing()).getDegrees()));
    }
}
