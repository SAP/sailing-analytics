package com.sap.sailing.domain.ranking;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.racelog.tracking.EmptyGPSFixStore;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.test.TrackBasedTest;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TestSimpleTimeOnTimeRankingWithOneUpwindLeg {
    private TimeOnTimeAndDistanceRankingMetric tot;
    private DynamicTrackedRace trackedRace;
    private Competitor c1, c2;
    
    private void setUp(Function<Competitor, Double> timeOnTimeFactors, Function<Competitor, Double> timeOnDistanceFactors) {
        c1 = TrackBasedTest.createCompetitor("FastBoat");
        c2 = TrackBasedTest.createCompetitor("SlowBoat");
        trackedRace = createTrackedRace(tot, Arrays.asList(c1, c2), timeOnTimeFactors, timeOnDistanceFactors);
        tot = (TimeOnTimeAndDistanceRankingMetric) trackedRace.getRankingMetric();
        assertEquals(60, trackedRace.getCourseLength().getNauticalMiles(), 0.01);
    }
    
    private DynamicTrackedRace createTrackedRace(RankingMetric rankingMetric, Iterable<Competitor> competitors, Function<Competitor, Double> timeOnTimeFactors, Function<Competitor, Double> timeOnDistanceFactors) {
        final TimePoint timePointForFixes = MillisecondsTimePoint.now();
        BoatClassImpl boatClass = new BoatClassImpl("Some Handicap Boat Class", /* typicallyStartsUpwind */ true);
        Regatta regatta = new RegattaImpl(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE,
                RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()), boatClass, /*startDate*/ null, /*endDate*/ null, /* trackedRegattaRegistry */ null,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), "123", null);
        TrackedRegatta trackedRegatta = new DynamicTrackedRegattaImpl(regatta);
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        // create a two-lap upwind/downwind course:
        MarkImpl left = new MarkImpl("Left lee gate buoy");
        MarkImpl right = new MarkImpl("Right lee gate buoy");
        ControlPoint leeGate = new ControlPointWithTwoMarksImpl(left, right, "Lee Gate");
        Mark windwardMark = new MarkImpl("Windward mark");
        waypoints.add(new WaypointImpl(leeGate));
        waypoints.add(new WaypointImpl(windwardMark));
        Course course = new CourseImpl("Test Course", waypoints);
        RaceDefinition race = new RaceDefinitionImpl("Test Race", course, boatClass, competitors);
        DynamicTrackedRaceImpl trackedRace = new DynamicTrackedRaceImpl(trackedRegatta, race, Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE,
                        EmptyGPSFixStore.INSTANCE, /* delayToLiveInMillis */ 0,
                /* millisecondsOverWhichToAverageWind */ 30000, /* millisecondsOverWhichToAverageSpeed */ 30000,
                /* delay for wind estimation cache invalidation */ 0, /*useMarkPassingCalculator*/ false,
                tr->new TimeOnTimeAndDistanceRankingMetric(tr,
                        timeOnTimeFactors, // time-on-time
                        timeOnDistanceFactors));
        // in this simplified artificial course, the top mark is exactly north of the right leeward gate
        DegreePosition topPosition = new DegreePosition(1, 0);
        trackedRace.getOrCreateTrack(left).addGPSFix(new GPSFixImpl(new DegreePosition(0, -0.000001), timePointForFixes));
        trackedRace.getOrCreateTrack(right).addGPSFix(new GPSFixImpl(new DegreePosition(0, 0.000001), timePointForFixes));
        trackedRace.getOrCreateTrack(windwardMark).addGPSFix(new GPSFixImpl(topPosition, timePointForFixes));
        trackedRace.recordWind(new WindImpl(topPosition, timePointForFixes, new KnotSpeedWithBearingImpl(
                /* speedInKnots */14.7, new DegreeBearingImpl(180))), new WindSourceImpl(WindSourceType.WEB));
        return trackedRace;
    }


    @Test
    public void testTimeOnTimeWithFactorTwoBoatsAtEqualHeight() {
        setUp(c -> c==c1 ? 2.0 : 1.0, c -> 0.0);
        final TimePoint startOfRace = MillisecondsTimePoint.now();
        final TimePoint middleOfFirstLeg = startOfRace.plus(Duration.ONE_HOUR.times(3));
        trackedRace.updateMarkPassings(
                c1,
                Arrays.<MarkPassing> asList(new MarkPassingImpl(startOfRace, trackedRace.getRace().getCourse()
                        .getFirstWaypoint(), c1)));
        trackedRace.updateMarkPassings(
                c2,
                Arrays.<MarkPassing> asList(new MarkPassingImpl(startOfRace, trackedRace.getRace().getCourse()
                        .getFirstWaypoint(), c2)));
        trackedRace.getTrack(c1).add(
                new GPSFixMovingImpl(new DegreePosition(0.5, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(45))));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(0.5, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(315))));
        // Both boats have climbed half of the first upwind beat; c1 is rated the faster boat (2.0), c2 has time-on-time factor 1.0.
        // Therefore, c2 is expected to lead after applying the corrections.
        Comparator<Competitor> comparator = tot.getRaceRankingComparator(middleOfFirstLeg);
        assertEquals(1, comparator.compare(c1, c2)); // c1 is "greater" than c2; better competitors rank less
    }

    @Test
    public void testTimeOnTimeWithFactorTwoC1TwiceAsFarAsC2() {
        setUp(c -> c==c1 ? 2.0 : 1.0, c -> 0.0);
        final TimePoint startOfRace = MillisecondsTimePoint.now();
        final TimePoint middleOfFirstLeg = startOfRace.plus(Duration.ONE_HOUR.times(3));
        trackedRace.updateMarkPassings(
                c1,
                Arrays.<MarkPassing> asList(new MarkPassingImpl(startOfRace, trackedRace.getRace().getCourse()
                        .getFirstWaypoint(), c1)));
        trackedRace.updateMarkPassings(
                c2,
                Arrays.<MarkPassing> asList(new MarkPassingImpl(startOfRace, trackedRace.getRace().getCourse()
                        .getFirstWaypoint(), c2)));
        trackedRace.getTrack(c1).add(
                new GPSFixMovingImpl(new DegreePosition(1.0, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(45))));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(0.5, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(315))));
        // Using a white-box test, assert that the ranking-relevant numbers are sufficiently close to each other
        assertEquals(tot.getAverageCorrectedReciprokeVMGAsSecondsPerNauticalMile(c1, middleOfFirstLeg),
                tot.getAverageCorrectedReciprokeVMGAsSecondsPerNauticalMile(c2, middleOfFirstLeg), 0.00001);
    }

    @Test
    public void testTimeOnDistanceWithFactorTwoBoatsAtEqualHeight() {
        setUp(c -> 1.0, c -> c==c1 ? 350. : 700.); // c1 is twice as fast (350s instead of 700s to the mile) as c2
        final TimePoint startOfRace = MillisecondsTimePoint.now();
        final TimePoint middleOfFirstLeg = startOfRace.plus(Duration.ONE_HOUR.times(3));
        trackedRace.updateMarkPassings(
                c1,
                Arrays.<MarkPassing> asList(new MarkPassingImpl(startOfRace, trackedRace.getRace().getCourse()
                        .getFirstWaypoint(), c1)));
        trackedRace.updateMarkPassings(
                c2,
                Arrays.<MarkPassing> asList(new MarkPassingImpl(startOfRace, trackedRace.getRace().getCourse()
                        .getFirstWaypoint(), c2)));
        trackedRace.getTrack(c1).add(
                new GPSFixMovingImpl(new DegreePosition(0.5, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(45))));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(0.5, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(315))));
        // Both boats have climbed half of the first upwind beat; c1 is rated the faster boat (2.0), c2 has time-on-time factor 1.0.
        // Therefore, c2 is expected to lead after applying the corrections.
        Comparator<Competitor> comparator = tot.getRaceRankingComparator(middleOfFirstLeg);
        assertEquals(1, comparator.compare(c1, c2)); // c1 is "greater" than c2; better competitors rank less
    }

    @Test
    public void testTimeOnDistanceWithFactorTwoC1TwiceAsFarAsC2() {
        setUp(c -> 1.0, c -> c==c1 ? 180. : 360.); // c1 is twice as fast (350s instead of 700s to the mile) as c2
        final TimePoint startOfRace = MillisecondsTimePoint.now();
        final TimePoint middleOfFirstLeg = startOfRace.plus(Duration.ONE_HOUR.times(3));
        trackedRace.updateMarkPassings(
                c1,
                Arrays.<MarkPassing> asList(new MarkPassingImpl(startOfRace, trackedRace.getRace().getCourse()
                        .getFirstWaypoint(), c1)));
        trackedRace.updateMarkPassings(
                c2,
                Arrays.<MarkPassing> asList(new MarkPassingImpl(startOfRace, trackedRace.getRace().getCourse()
                        .getFirstWaypoint(), c2)));
        trackedRace.getTrack(c1).add(
                new GPSFixMovingImpl(new DegreePosition(1.0, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(45))));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(0.5, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(315))));
        // Using a white-box test, assert that the ranking-relevant numbers are sufficiently close to each other,
        // in this case .05 seconds per nautical mile for the reciproke VMG measured in seconds per nautical mile
        assertEquals(tot.getAverageCorrectedReciprokeVMGAsSecondsPerNauticalMile(c1, middleOfFirstLeg),
                tot.getAverageCorrectedReciprokeVMGAsSecondsPerNauticalMile(c2, middleOfFirstLeg), 0.05);
    }

}
