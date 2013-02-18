package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sailing.domain.tracking.impl.WindImpl;
import com.sap.sailing.domain.tracking.impl.WindTrackImpl;

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
        assertEquals(0., average.getBearing().getDifferenceTo(new DegreeBearingImpl(0)).getDegrees(), 0.0001);
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
            assertEquals(expectedWind, actualWind);
        } finally {
            track.unlockAfterRead();
        }
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
        assertEquals(20, track.getAveragedWind(pos, new MillisecondsTimePoint(1)).getKnots(), 0.00000001);
        // interval uses the two fixes to the left (0, 1000)=1000 and three to the right (2000, 10000, 30000)=28000
        assertEquals(20, track.getAveragedWind(pos, new MillisecondsTimePoint(1001)).getKnots(), 0.00000001);
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
        assertEquals(10, estimate.getKnots(), 0.000000001);
        assertEquals(123, estimate.getBearing().getDegrees(), 0.000000001);
    }

    @Test
    public void testSingleElementExtrapolation() {
        WindTrack track = new WindTrackImpl(30000 /* 30s averaging interval */, /* useSpeed */ true, "TestWindTrack");
        DegreePosition pos = new DegreePosition(0, 0);
        Wind wind = new WindImpl(pos, new MillisecondsTimePoint(0), new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(123)));
        track.add(wind);
        // we only have one measurement; this should be extrapolated because it's our best guess
        Wind estimate = track.getAveragedWind(pos, new MillisecondsTimePoint(1000));
        assertEquals(10, estimate.getKnots(), 0.000000001);
        assertEquals(123, estimate.getBearing().getDegrees(), 0.000000001);
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
        assertEquals(10, estimate.getKnots(), 0.000000001);
        assertEquals(123, estimate.getBearing().getDegrees(), 0.000000001);
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
        assertEquals(15, estimate.getKnots(), 0.000000001);
        assertEquals(100, estimate.getBearing().getDegrees(), 0.00000001);
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
        assertEquals(10, estimate.getKnots(), 0.000000001);
        assertEquals(105, estimate.getBearing().getDegrees(), 0.6); // some tolerance needed because of time-based confidence
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
        assertEquals((wind1.getKnots() + wind2.getKnots() + wind3.getKnots()) / 3, result.getKnots(), 0.000000001);
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
        ControlPoint startFinish = domainFactory.createGate(startFinishLeft, startFinishRight, "Start/Finish");
        ControlPoint top = domainFactory.getOrCreateMark("Top");
        Waypoint w1 = domainFactory.createWaypoint(startFinish);
        Waypoint w2 = domainFactory.createWaypoint(top);
        Waypoint w3 = domainFactory.createWaypoint(startFinish);
        Competitor competitor = new CompetitorImpl(123, "Test Competitor", new TeamImpl("STG", Collections.singleton(
                new PersonImpl("Test Competitor", new NationalityImpl("GER"),
                /* dateOfBirth */null, "This is famous " + "Test Competitor")), new PersonImpl("Rigo van Maas",
                new NationalityImpl("NED"),
                /* dateOfBirth */null, "This is Rigo, the coach")), new BoatImpl("Test Competitor" + "'s boat",
                new BoatClassImpl("505", /* typicallyStartsUpwind */true), null));
        final BoatClass boatClass = domainFactory.getOrCreateBoatClass("ESS40");
        DynamicTrackedRace trackedRace = new DynamicTrackedRaceImpl(new DynamicTrackedRegattaImpl(
                new RegattaImpl("Test Regatta", boatClass,
                /* trackedRegattaRegistry */ null, domainFactory.createScoringScheme(ScoringSchemeType.LOW_POINT), "123")),
                new RaceDefinitionImpl("Test Race",
                        new CourseImpl("Test Course", Arrays.asList(new Waypoint[] { w1, w2, w3 })),
                        boatClass, Collections.singleton(competitor)),
                        EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 1000,
                        /* millisecondsOverWhichToAverageWind */ 30000,
                        /* millisecondsOverWhichToAverageSpeed */ 30000);
        TimePoint start = MillisecondsTimePoint.now();
        TimePoint topMarkRounding = start.plus(30000);
        TimePoint finish = topMarkRounding.plus(30000);
        trackedRace.updateMarkPassings(competitor, Arrays.asList(new MarkPassing[] {
            new MarkPassingImpl(start, w1, competitor),    
            new MarkPassingImpl(topMarkRounding, w2, competitor),    
            new MarkPassingImpl(finish, w3, competitor)    
        }));
        assertFalse(boatClass.typicallyStartsUpwind());
        assertNull(trackedRace.getEstimatedWindDirection(new DegreePosition(0, 0), MillisecondsTimePoint.now()));
    }
}
