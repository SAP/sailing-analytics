package com.sap.sailing.domain.markpassingcalculation.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.state.impl.RaceStateImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.impl.ReadonlyRacingProcedureFactory;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.configuration.impl.EmptyRegattaConfiguration;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.markpassingcalculation.Candidate;
import com.sap.sailing.domain.markpassingcalculation.CandidateFinder;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingCalculator;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateFinderImpl.AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine;
import com.sap.sailing.domain.test.TrackBasedTest;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.impl.TrackedRaceImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class CandidateFinderImplWhiteBoxTest {
    private static class CandidateFinderWithPublicGetProbabilityOfStartBasedOnOtherCompetitorsStartLineDistances extends CandidateFinderImpl {
        public CandidateFinderWithPublicGetProbabilityOfStartBasedOnOtherCompetitorsStartLineDistances(
                DynamicTrackedRace race) {
            super(race);
        }

        public Double getProbabilityOfStartBasedOnOtherCompetitorsStartLineDistances(
                final List<AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine> distancesToStartLineOfOtherCompetitors, boolean startIsLine) {
            return super.getProbabilityOfStartBasedOnOtherCompetitorsStartLineDistances(distancesToStartLineOfOtherCompetitors, startIsLine);
        }
    }
    
    private CandidateFinderWithPublicGetProbabilityOfStartBasedOnOtherCompetitorsStartLineDistances finder;
    private CompetitorWithBoat competitor;
    private DynamicTrackedRace trackedRace;
    private TimePoint now;
    
    @BeforeEach
    public void setUp() {
        now = MillisecondsTimePoint.now();
        competitor = TrackBasedTest.createCompetitorWithBoat("Competitor");
        Map<Competitor, Boat> competitorAndBoats = TrackBasedTest.createCompetitorAndBoatsMap(competitor);
        trackedRace = TrackBasedTest.createTestTrackedRace("Test Regatta", "Test Race", "505", competitorAndBoats, now, /* useMarkPassingCalculator */ true);
        finder = new CandidateFinderWithPublicGetProbabilityOfStartBasedOnOtherCompetitorsStartLineDistances(trackedRace);
    }
    
    @Test
    public void testOneProbabilityForEmptyList() {
        List<AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine> emptyDistancesToStartLineOfOtherCompetitors = new ArrayList<>();
        final double probability = finder.getProbabilityOfStartBasedOnOtherCompetitorsStartLineDistances(emptyDistancesToStartLineOfOtherCompetitors,
                /* startIsLine */ true);
        assertEquals(1.0, probability, 0.00001);
    }

    @Test
    public void testTwoCandidatesForStartLinePassingAtStartOfRace() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        CandidateFinder finder = getCandidateFinderOfTrackedRace();
        trackedRace.setStartOfTrackingReceived(now.minus(Duration.ONE_MINUTE.times(10)));
        trackedRace.setStartTimeReceived(now.plus(Duration.ONE_MINUTE));
        final TimePoint timeForStartLinePassing = trackedRace.getStartOfRace();
        createStartLinePassing(timeForStartLinePassing);
        waitForMarkPassingCalculatorToFinishComputing();
        final Pair<Iterable<Candidate>, Iterable<Candidate>> candidates = finder.getAllCandidates(competitor);
        assertNotNull(candidates);
        assertFalse(Util.isEmpty(candidates.getA()));
        assertTrue(Util.isEmpty(candidates.getB()));
        final Waypoint startWaypoint = trackedRace.getRace().getCourse().getFirstWaypoint();
        // expecting one distance and one XTE candidate
        assertEquals(2, StreamSupport.stream(candidates.getA().spliterator(), /* parallel */ false).filter(c -> c.getWaypoint() == startWaypoint).count());
    }

    private void waitForMarkPassingCalculatorToFinishComputing() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        final MarkPassingCalculator markPassingCalculator = getMarkPassingCalculatorOfTrackedRace();
        markPassingCalculator.stop();
        markPassingCalculator.waitUntilStopped(10000);
    }

    @Test
    public void testNoCandidatesForStartLinePassingFiveMinutesBeforeStartOfRace() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InterruptedException {
        CandidateFinder finder = getCandidateFinderOfTrackedRace();
        trackedRace.setStartOfTrackingReceived(now.minus(Duration.ONE_MINUTE.times(10)));
        trackedRace.setStartTimeReceived(now.plus(Duration.ONE_MINUTE));
        final TimePoint timeForStartLinePassing = trackedRace.getStartOfRace().minus(Duration.ONE_MINUTE.times(5));
        createStartLinePassing(timeForStartLinePassing);
        waitForMarkPassingCalculatorToFinishComputing();
        final Pair<Iterable<Candidate>, Iterable<Candidate>> candidates = finder.getAllCandidates(competitor);
        assertNotNull(candidates);
        assertTrue(Util.isEmpty(candidates.getA()));
        assertTrue(Util.isEmpty(candidates.getB()));
        final Waypoint startWaypoint = trackedRace.getRace().getCourse().getFirstWaypoint();
        // expecting one distance and one XTE candidate
        assertEquals(0, StreamSupport.stream(candidates.getA().spliterator(), /* parallel */ false).filter(c -> c.getWaypoint() == startWaypoint).count());
    }

    @Test
    public void testTwoCandidatesForStartLinePassingOneMinuteBeforerBlueFlagDown() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InterruptedException {
        CandidateFinder finder = getCandidateFinderOfTrackedRace();
        final RaceLogImpl raceLog = new RaceLogImpl("RaceLog");
        trackedRace.attachRaceLog(raceLog);
        trackedRace.setStartOfTrackingReceived(now.minus(Duration.ONE_MINUTE.times(10)));
        trackedRace.setStartTimeReceived(now.plus(Duration.ONE_MINUTE));
        // blue flag goes down again one minute after start
        final TimePoint finishedTime = now.plus(Duration.ONE_MINUTE.times(2));
        new RaceStateImpl(/* raceLogResolver */ null, raceLog, new LogEventAuthorImpl("Me", 0), new ReadonlyRacingProcedureFactory(
                new EmptyRegattaConfiguration())).setFinishedTime(finishedTime);
        // pass the line a bit after the candidate finder is expected to ignore candidates (5min after blue flag down)
        final TimePoint timeForStartLinePassing = finishedTime.minus(Duration.ONE_MINUTE);
        createStartLinePassing(timeForStartLinePassing);
        waitForMarkPassingCalculatorToFinishComputing();
        final Pair<Iterable<Candidate>, Iterable<Candidate>> candidates = finder.getAllCandidates(competitor);
        assertNotNull(candidates);
        assertFalse(Util.isEmpty(candidates.getA()));
        assertTrue(Util.isEmpty(candidates.getB()));
        final Waypoint startWaypoint = trackedRace.getRace().getCourse().getFirstWaypoint();
        // expecting one distance and one XTE candidate
        assertEquals(2, StreamSupport.stream(candidates.getA().spliterator(), /* parallel */ false).filter(c -> c.getWaypoint() == startWaypoint).count());
    }

    @Test
    public void testNoCandidatesForStartLinePassingOneMinuteAfterBlueFlagDown() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException, InterruptedException {
        CandidateFinder finder = getCandidateFinderOfTrackedRace();
        final RaceLogImpl raceLog = new RaceLogImpl("RaceLog");
        trackedRace.attachRaceLog(raceLog);
        trackedRace.setStartOfTrackingReceived(now.minus(Duration.ONE_MINUTE.times(10)));
        trackedRace.setStartTimeReceived(now.plus(Duration.ONE_MINUTE));
        // blue flag goes down again one minute after start
        final TimePoint finishedTime = now.plus(Duration.ONE_MINUTE.times(2));
        new RaceStateImpl(/* raceLogResolver */ null, raceLog, new LogEventAuthorImpl("Me", 0), new ReadonlyRacingProcedureFactory(
                new EmptyRegattaConfiguration())).setFinishedTime(finishedTime);
        // pass the line a bit after the candidate finder is expected to ignore candidates (5min after blue flag down)
        final TimePoint timeForStartLinePassing = finishedTime.plus(Duration.ONE_MINUTE.times(6));
        createStartLinePassing(timeForStartLinePassing);
        waitForMarkPassingCalculatorToFinishComputing();
        final Pair<Iterable<Candidate>, Iterable<Candidate>> candidates = finder.getAllCandidates(competitor);
        assertNotNull(candidates);
        assertTrue(Util.isEmpty(candidates.getA()));
        assertTrue(Util.isEmpty(candidates.getB()));
        final Waypoint startWaypoint = trackedRace.getRace().getCourse().getFirstWaypoint();
        // expecting one distance and one XTE candidate
        assertEquals(0, StreamSupport.stream(candidates.getA().spliterator(), /* parallel */ false).filter(c -> c.getWaypoint() == startWaypoint).count());
    }

    private CandidateFinder getCandidateFinderOfTrackedRace() throws NoSuchFieldException, IllegalAccessException {
        final MarkPassingCalculator markPassingCalculator = getMarkPassingCalculatorOfTrackedRace();
        final Field finderField = MarkPassingCalculator.class.getDeclaredField("finder");
        finderField.setAccessible(true);
        CandidateFinder finder = (CandidateFinder) finderField.get(markPassingCalculator);
        return finder;
    }

    private MarkPassingCalculator getMarkPassingCalculatorOfTrackedRace()
            throws NoSuchFieldException, IllegalAccessException {
        final Field markPassingCalculatorField = TrackedRaceImpl.class.getDeclaredField("markPassingCalculator");
        markPassingCalculatorField.setAccessible(true);
        final MarkPassingCalculator markPassingCalculator = (MarkPassingCalculator) markPassingCalculatorField.get(trackedRace);
        return markPassingCalculator;
    }

    private void createStartLinePassing(final TimePoint timeForStartLinePassing) {
        final Waypoint startWaypoint = trackedRace.getRace().getCourse().getFirstWaypoint();
        final SpeedWithBearing startLineCrossingSpeed = new KnotSpeedWithBearingImpl(4, new DegreeBearingImpl(325)); // a classical port tack start
        final Position startLinePosition = trackedRace.getApproximatePosition(startWaypoint, timeForStartLinePassing);
        final Position beforeStartPosition = startLinePosition.translateGreatCircle(startLineCrossingSpeed.getBearing().reverse(), startLineCrossingSpeed.travel(Duration.ONE_SECOND));
        final Position atStartPosition = startLinePosition;
        final Position afterStartPosition = startLinePosition.translateGreatCircle(startLineCrossingSpeed.getBearing(), startLineCrossingSpeed.travel(Duration.ONE_SECOND));
        trackedRace.getTrack(competitor).add(new GPSFixMovingImpl(beforeStartPosition, timeForStartLinePassing.minus(Duration.ONE_SECOND), startLineCrossingSpeed, /* optionalTrueHeading */ null));
        trackedRace.getTrack(competitor).add(new GPSFixMovingImpl(atStartPosition, timeForStartLinePassing, startLineCrossingSpeed, /* optionalTrueHeading */ null));
        trackedRace.getTrack(competitor).add(new GPSFixMovingImpl(afterStartPosition, timeForStartLinePassing.plus(Duration.ONE_SECOND), startLineCrossingSpeed, /* optionalTrueHeading */ null));
    }

    @Test
    public void testHighProbabilityForCloseProximityAndNoLine() {
        List<AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine> distancesToStartLineOfOtherCompetitors = new ArrayList<>();
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(3.0, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(5.0, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(2.0, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(8.0, null));
        final double probability = finder.getProbabilityOfStartBasedOnOtherCompetitorsStartLineDistances(distancesToStartLineOfOtherCompetitors,
                /* startIsLine */ false);
        assertTrue(probability > 0.95);
    }

    @Test
    public void testHighProbabilityForCloseProximityExceptOneAndNoLine() {
        List<AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine> distancesToStartLineOfOtherCompetitors = new ArrayList<>();
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(3.0, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(5.0, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(2.0, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(8.0, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(1.0, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(2.5, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(3.2, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(2739.0, null));
        final double probability = finder.getProbabilityOfStartBasedOnOtherCompetitorsStartLineDistances(distancesToStartLineOfOtherCompetitors,
                /* startIsLine */ false);
        assertTrue(probability > 0.55, "Expected probability to exceed 55% but got "+probability);
    }

    @Test
    public void testLowProbabilityForMostFarAwayAndNoLine() {
        List<AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine> distancesToStartLineOfOtherCompetitors = new ArrayList<>();
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(50.0, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(250.0, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(120.0, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(200.0, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(300.0, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(2.5, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(3.2, null));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(2739.0, null));
        final double probability = finder.getProbabilityOfStartBasedOnOtherCompetitorsStartLineDistances(distancesToStartLineOfOtherCompetitors,
                /* startIsLine */ false);
        assertTrue(probability <= 0.1, "Expected probability to be below 10% but got "+probability);
    }

    @Test
    public void testHighProbabilityForLateStartAndStartIsLine() {
        List<AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine> distancesToStartLineOfOtherCompetitors = new ArrayList<>();
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(100.0, -80.));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(120.0, -82.));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(130.0, -81.));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(90.0, -79.));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(140.0, -75.));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(150, -88.));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(135, -77.));
        distancesToStartLineOfOtherCompetitors.add(createDistancePair(20.0, -10.)); // another late starter just across the line
        final double probability = finder.getProbabilityOfStartBasedOnOtherCompetitorsStartLineDistances(distancesToStartLineOfOtherCompetitors,
                /* startIsLine */ true);
        assertTrue(probability >= 0.85, "Expected probability to be above 85% but got "+probability);
    }

    private AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine createDistancePair(double absoluteDistanceToLine,
            Double signedXTEDistanceFromLine) {
        return new AbsoluteGeometricDistanceAndSignedProjectedDistanceToStartLine(
                new MeterDistance(absoluteDistanceToLine), signedXTEDistanceFromLine == null ? null : new MeterDistance(signedXTEDistanceFromLine));
    }
}
