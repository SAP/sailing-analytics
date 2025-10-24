package com.sap.sailing.domain.ranking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
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
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
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
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TestSimpleTimeOnTimeRankingWithOneUpwindLeg {
    private RankingMetric tot;
    private DynamicTrackedRace trackedRace;
    private CompetitorWithBoat c1, c2;
    
    private void setUp(TimeOnTimeFactorMapping timeOnTimeFactors, Function<Competitor, Double> timeOnDistanceFactors) {
        CompetitorWithBoat fastCompetitor = TrackBasedTest.createCompetitorWithBoat("FastBoat");
        c1 = fastCompetitor;
        CompetitorWithBoat slowCompetitor = TrackBasedTest.createCompetitorWithBoat("SlowBoat");
        c2 = slowCompetitor;
        trackedRace = createTrackedRace(TrackBasedTest.createCompetitorAndBoatsMap(fastCompetitor, slowCompetitor), timeOnTimeFactors, timeOnDistanceFactors);
        tot = trackedRace.getRankingMetric();
        assertEquals(60, trackedRace.getCourseLength().getNauticalMiles(), 0.01);
        assertSame(RankingMetrics.TIME_ON_TIME_AND_DISTANCE, trackedRace.getTrackedRegatta().getRegatta().getRankingMetricType());
    }
    
    private DynamicTrackedRace createTrackedRace(Map<Competitor,Boat> competitorsAndBoats, TimeOnTimeFactorMapping timeOnTimeFactors, Function<Competitor, Double> timeOnDistanceFactors) {
        final TimePoint timePointForFixes = MillisecondsTimePoint.now();
        BoatClassImpl boatClass = new BoatClassImpl("Some Handicap Boat Class", /* typicallyStartsUpwind */ true);
        Regatta regatta = new RegattaImpl(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE,
                RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()), boatClass, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, /*startDate*/ null, /*endDate*/ null, /* trackedRegattaRegistry */ null,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), "123", /* courseArea */ null,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false,
                TimeOnTimeAndDistanceRankingMetric::new, /* registrationLinkSecret */ UUID.randomUUID().toString());
        TrackedRegatta trackedRegatta = new DynamicTrackedRegattaImpl(regatta);
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        // create a two-lap upwind/downwind course:
        MarkImpl left = new MarkImpl("Left lee gate buoy");
        MarkImpl right = new MarkImpl("Right lee gate buoy");
        ControlPoint leeGate = new ControlPointWithTwoMarksImpl(left, right, "Lee Gate", "Lee Gate");
        Mark windwardMark = new MarkImpl("Windward mark");
        waypoints.add(new WaypointImpl(leeGate));
        waypoints.add(new WaypointImpl(windwardMark));
        Course course = new CourseImpl("Test Course", waypoints);
        RaceDefinition race = new RaceDefinitionImpl("Test Race", course, boatClass, competitorsAndBoats);
        DynamicTrackedRaceImpl trackedRace = new DynamicTrackedRaceImpl(trackedRegatta, race, Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE,
                /* delayToLiveInMillis */ 0,
                /* millisecondsOverWhichToAverageWind */ 30000, /* millisecondsOverWhichToAverageSpeed */ 30000,
                /* delay for wind estimation cache invalidation */ 0, /*useMarkPassingCalculator*/ false,
                tr->new TimeOnTimeAndDistanceRankingMetric(tr,
                        timeOnTimeFactors, // time-on-time
                        c->new MillisecondsDurationImpl((long) (1000.*timeOnDistanceFactors.apply(c)))), mock(RaceLogAndTrackedRaceResolver.class), null, /* markPassingRaceFingerprintRegistry */ null);
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
                new GPSFixMovingImpl(new DegreePosition(0.0, 0), startOfRace, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(45)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(0.0, 0), startOfRace, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(315)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c1).add(
                new GPSFixMovingImpl(new DegreePosition(0.5, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(45)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(0.5, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(315)), /* optionalTrueHeading */ null));
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
                new GPSFixMovingImpl(new DegreePosition(0.0, 0), startOfRace, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(45)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(0.0, 0), startOfRace, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(315)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c1).add(
                new GPSFixMovingImpl(new DegreePosition(1.0, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(45)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(0.5, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(315)), /* optionalTrueHeading */ null));
        // Using a white-box test, assert that the ranking-relevant numbers are sufficiently close to each other
        final RankingMetric.RankingInfo rankingInfo = tot.getRankingInfo(middleOfFirstLeg);
        assertSame(c1, rankingInfo.getCompetitorFarthestAhead());
        RankingMetric.CompetitorRankingInfo c1RI = rankingInfo.getCompetitorRankingInfo().apply(c1);
        RankingMetric.CompetitorRankingInfo c2RI = rankingInfo.getCompetitorRankingInfo().apply(c2);
        assertEquals(c1RI.getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead().asSeconds(),
                c2RI.getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead().asSeconds(),
                /* relative accuracy */ 0.0000001 * c1RI.getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead().asSeconds());
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
                new GPSFixMovingImpl(new DegreePosition(0.0, 0), startOfRace, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(45)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(0.0, 0), startOfRace, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(315)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c1).add(
                new GPSFixMovingImpl(new DegreePosition(0.5, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(45)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(0.5, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(315)), /* optionalTrueHeading */ null));
        // Both boats have climbed half of the first upwind beat; c1 is rated the faster boat (2.0), c2 has time-on-time factor 1.0.
        // Therefore, c2 is expected to lead after applying the corrections.
        Comparator<Competitor> comparator = tot.getRaceRankingComparator(middleOfFirstLeg);
        assertEquals(1, comparator.compare(c1, c2)); // c1 is "greater" than c2; better competitors rank less
    }

    @Test
    public void testTimeOnDistanceWithFactorTwoC1TwiceAsFarAsC2() {
        setUp(c -> 1.0, c -> c==c1 ? 180. : 360.); // c1 is twice as fast (180s instead of 360s to the mile) as c2
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
                new GPSFixMovingImpl(new DegreePosition(0.0, 0), startOfRace, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(45)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(0.0, 0), startOfRace, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(315)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c1).add(
                new GPSFixMovingImpl(new DegreePosition(1.0, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(45)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(0.5, 0), middleOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(315)), /* optionalTrueHeading */ null));
        // Using a white-box test, assert that the ranking-relevant numbers are sufficiently close to each other,
        // in this case .05 seconds per nautical mile for the reciproke VMG measured in seconds per nautical mile
        final RankingMetric.RankingInfo rankingInfo = tot.getRankingInfo(middleOfFirstLeg);
        RankingMetric.CompetitorRankingInfo c1RI = rankingInfo.getCompetitorRankingInfo().apply(c1);
        RankingMetric.CompetitorRankingInfo c2RI = rankingInfo.getCompetitorRankingInfo().apply(c2);
        assertEquals(c1RI.getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead().asSeconds(),
                c2RI.getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead().asSeconds(),
                startOfRace.until(middleOfFirstLeg).asSeconds() / 1000. /* 0.1% accuracy expected */);
    }
}
