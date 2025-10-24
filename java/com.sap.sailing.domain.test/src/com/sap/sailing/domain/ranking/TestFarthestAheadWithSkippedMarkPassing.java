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
import com.sap.sailing.domain.base.impl.DynamicCompetitorWithBoat;
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
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
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
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * A test for the case described in bug 2976: when a competitor has skipped a mark passing but then got one for a
 * later waypoint, that competitor may still be the one "farthest ahead." Still, we seem to have observed a case where
 * under these circumstances the competitor was not considered "farthest ahead" and instead another with a contiguous
 * mark passing list was preferred.<p>
 * 
 * This test case establishes such a situation: two competitors, one with contiguous mark passings for the first two
 * waypoints but not having sailed as far as the other in the second leg; the other competitor skipped the first mark
 * passing, has one for the second waypoint and leads by windward distance in the second leg.<p>
 * 
 * Since the problem occurred with the {@link OneDesignRankingMetric}, we'll use that one for the set-up here.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class TestFarthestAheadWithSkippedMarkPassing {
    private RankingMetric rankingMetric;
    private DynamicTrackedRace trackedRace;
    private CompetitorWithBoat c1, c2;
    private Waypoint start;
    private Waypoint windward;
    private Waypoint finish;
    
    private void setUp(TimeOnTimeFactorMapping timeOnTimeFactors, Function<Competitor, Duration> timeOnDistanceAllowances) {
        DynamicCompetitorWithBoat fastCompetitor = (DynamicCompetitorWithBoat) TrackBasedTest.createCompetitorWithBoat("FastBoat");
        fastCompetitor.setTimeOnTimeFactor(timeOnTimeFactors.apply(fastCompetitor));
        fastCompetitor.setTimeOnDistanceAllowancePerNauticalMile(timeOnDistanceAllowances.apply(fastCompetitor));
        c1 = fastCompetitor;
        DynamicCompetitorWithBoat slowCompetitor = (DynamicCompetitorWithBoat) TrackBasedTest.createCompetitorWithBoat("SlowBoat");
        slowCompetitor.setTimeOnTimeFactor(timeOnTimeFactors.apply(slowCompetitor));
        slowCompetitor.setTimeOnDistanceAllowancePerNauticalMile(timeOnDistanceAllowances.apply(slowCompetitor));
        c2 = slowCompetitor;
        trackedRace = createTrackedRace(TrackBasedTest.createCompetitorAndBoatsMap(fastCompetitor, slowCompetitor));
        rankingMetric = trackedRace.getRankingMetric();
        assertEquals(120, trackedRace.getCourseLength().getNauticalMiles(), 0.02);
        assertSame(RankingMetrics.ONE_DESIGN, trackedRace.getTrackedRegatta().getRegatta().getRankingMetricType());
    }
    
    private DynamicTrackedRace createTrackedRace(Map<Competitor, Boat> competitorsAndBoats) {
        final TimePoint timePointForFixes = MillisecondsTimePoint.now();
        BoatClassImpl boatClass = new BoatClassImpl("Some Handicap Boat Class", /* typicallyStartsUpwind */ true);
        Regatta regatta = new RegattaImpl(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE,
                RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()), boatClass, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /*startDate*/ null, /*endDate*/ null, /* trackedRegattaRegistry */ null,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), "123", /* courseArea */ null,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false,
                OneDesignRankingMetric::new, /* registrationLinkSecret */ UUID.randomUUID().toString());
        TrackedRegatta trackedRegatta = new DynamicTrackedRegattaImpl(regatta);
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        // create a two-lap upwind/downwind course:
        MarkImpl left = new MarkImpl("Left lee gate buoy");
        MarkImpl right = new MarkImpl("Right lee gate buoy");
        ControlPoint leeGate = new ControlPointWithTwoMarksImpl(left, right, "Lee Gate", "Lee Gate");
        Mark windwardMark = new MarkImpl("Windward mark");
        start = new WaypointImpl(leeGate);
        waypoints.add(start);
        windward = new WaypointImpl(windwardMark);
        waypoints.add(windward);
        finish = new WaypointImpl(leeGate);
        waypoints.add(finish);
        Course course = new CourseImpl("Test Course", waypoints);
        RaceDefinition race = new RaceDefinitionImpl("Test Race", course, boatClass, competitorsAndBoats);
        DynamicTrackedRaceImpl trackedRace = new DynamicTrackedRaceImpl(trackedRegatta, race, Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE,
                /* delayToLiveInMillis */ 0,
                /* millisecondsOverWhichToAverageWind */ 30000, /* millisecondsOverWhichToAverageSpeed */ 30000,
                /* delay for wind estimation cache invalidation */ 0, /*useMarkPassingCalculator*/ false,
                tr->new OneDesignRankingMetric(tr), mock(RaceLogAndTrackedRaceResolver.class), null, /* markPassingRaceFingerprintRegistry */ null);
        // in this simplified artificial course, the top mark is exactly north of the right leeward gate
        DegreePosition topPosition = new DegreePosition(1, 0);
        trackedRace.getOrCreateTrack(left).addGPSFix(new GPSFixImpl(new DegreePosition(0, -0.000001), timePointForFixes));
        trackedRace.getOrCreateTrack(right).addGPSFix(new GPSFixImpl(new DegreePosition(0, 0.000001), timePointForFixes));
        trackedRace.getOrCreateTrack(windwardMark).addGPSFix(new GPSFixImpl(topPosition, timePointForFixes));
        trackedRace.recordWind(new WindImpl(topPosition, timePointForFixes, new KnotSpeedWithBearingImpl(
                /* speedInKnots */14.7, new DegreeBearingImpl(180))), new WindSourceImpl(WindSourceType.WEB));
        return trackedRace;
    }


    /**
     * A competitor having only the start mark passing shall be ranked farthest ahead if the only other competitor
     * has no start mark passing.
     */
    @Test
    public void testWithOneMarkPassingOnly() {
        setUp(c -> c==c1 ? 2.0 : 1.0, c -> Duration.NULL);
        final TimePoint startOfRace = MillisecondsTimePoint.now();
        final TimePoint middleOfFirstLeg = startOfRace.plus(Duration.ONE_HOUR.times(3));
        trackedRace.updateMarkPassings(
                c1,
                Arrays.<MarkPassing> asList(new MarkPassingImpl(startOfRace, start, c1)));
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
        final RankingInfo rankingInfo = rankingMetric.getRankingInfo(middleOfFirstLeg);
        final Competitor farthestAhead = rankingInfo.getCompetitorFarthestAhead();
        assertSame(c1, farthestAhead);
        Comparator<Competitor> comparator = rankingMetric.getRaceRankingComparator(middleOfFirstLeg);
        assertEquals(-1, comparator.compare(c1, c2)); // c1 is "less" than c2; better competitors rank less
    }

    /**
     * C2 skipped the start mark passing but got a valid passing for the next waypoint. In the second leg, C2 leads
     * by windward distance and should therefore be listed as the competitor farthest ahead by the ranking metric.
     */
    @Test
    public void testWithTwoMarkPassingsForC1AndSkippedStartForC2() {
        setUp(c -> c==c1 ? 2.0 : 1.0, c -> Duration.NULL);
        final TimePoint startOfRace = MillisecondsTimePoint.now();
        final TimePoint endOfFirstLeg = startOfRace.plus(Duration.ONE_HOUR.times(3));
        final TimePoint middleOfSecondLeg = endOfFirstLeg.plus(Duration.ONE_MINUTE.times(10));
        trackedRace.updateMarkPassings(
                c1,
                Arrays.<MarkPassing> asList(new MarkPassingImpl(startOfRace, start, c1), new MarkPassingImpl(endOfFirstLeg, windward, c1)));
        trackedRace.updateMarkPassings(
                c2,
                Arrays.<MarkPassing> asList(/* skipping start waypoint */ new MarkPassingImpl(endOfFirstLeg, windward, c2)));
        // they both start at the start line
        trackedRace.getTrack(c1).add(
                new GPSFixMovingImpl(new DegreePosition(0.0, 0), startOfRace, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(45)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(0.0, 0), startOfRace, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(315)), /* optionalTrueHeading */ null));
        // both round the windward mark at the same time
        trackedRace.getTrack(c1).add(
                new GPSFixMovingImpl(new DegreePosition(1.0, 0), endOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(45)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(1.0, 0), endOfFirstLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(315)), /* optionalTrueHeading */ null));
        // c2 is further down the wind already at the middle of the second leg:
        trackedRace.getTrack(c1).add(
                new GPSFixMovingImpl(new DegreePosition(0.5, 0), middleOfSecondLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(180)), /* optionalTrueHeading */ null));
        trackedRace.getTrack(c2).add(
                new GPSFixMovingImpl(new DegreePosition(0.2, 0), middleOfSecondLeg, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(180)), /* optionalTrueHeading */ null));
        // Assert that c2 is the boat farthest ahead
        final RankingInfo rankingInfo = rankingMetric.getRankingInfo(middleOfSecondLeg);
        assertSame(c2, rankingInfo.getCompetitorFarthestAhead());
        Comparator<Competitor> comparator = rankingMetric.getRaceRankingComparator(middleOfSecondLeg);
        assertEquals(1, comparator.compare(c1, c2)); // c1 is "less" than c2; better competitors rank less
    }
}
