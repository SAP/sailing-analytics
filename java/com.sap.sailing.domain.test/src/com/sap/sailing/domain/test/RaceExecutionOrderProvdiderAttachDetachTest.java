package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.impl.TrackedRaces;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.TimingConstants;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.RaceExecutionOrderProvider;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRaceImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Covers the linking and unlinking between {@link RaceColumn}s and {@link TrackedRaces}s
 * and tests whether {@link RaceExecutionOrderProvider}s get correctly attached and detached. Also tests
 * late attaching and detaching of {@link RaceExecutionOrderProvider}s in case the {@link RaceExecutionOrderProvider}
 * was <code>null</code> when {@link RaceColumn} was linked to {@link TrackedRace}.
 * 
 * @author Alexander Ries (D062114)
 * @author Axel Uhl (D043530)
 *
 */
public class RaceExecutionOrderProvdiderAttachDetachTest extends TrackBasedTest {
    private FlexibleLeaderboard flexibleLeaderboard;
    private RaceColumnInSeries raceColumnInSeries;
    private Fleet fleet;
    private TrackedRaceImpl trackedRace;
    private Regatta regatta;
    private Series series;

    private final String REGATTA = "TestRegatta";
    private final String RACE = "TestRace";
    private final String FLEET = "TestFleet";
    private final String BOATCLASS = "TestClass";
    private final String SERIES = "TestSeries";
    private final String FLEXIBLELEADERBOARD = "TestFlexibleLeaderboard";
    private final String RACECOLUMN_SERIES = "TestSeriesRaceColumn";
    private final String RACECOLUMN_FLEXIBLELEADERBOARD = "TestFlexibleLeaderboardRaceColumn";

    @Test
    public void testRaceExecutionOrderProviderAttachDetachWithRaceColumn() {
        trackedRace = createTestTrackedRace(REGATTA, RACE, BOATCLASS, Collections.<Competitor,Boat> emptyMap(),
                MillisecondsTimePoint.now(), /* useMarkPassingCalculator */ false);
        flexibleLeaderboard = new FlexibleLeaderboardImpl(FLEXIBLELEADERBOARD,
                new ThresholdBasedResultDiscardingRuleImpl(new int[] { 3, 6 }), new LowPoint(), null);
        flexibleLeaderboard.addRace(trackedRace, RACECOLUMN_FLEXIBLELEADERBOARD, false);
        assertTrue(trackedRace.hasRaceExecutionOrderProvidersAttached());
        flexibleLeaderboard.removeRaceColumn(RACECOLUMN_FLEXIBLELEADERBOARD);
        assertFalse(trackedRace.hasRaceExecutionOrderProvidersAttached());
    }

    @Test
    public void testWindInRegularIntervalWithPreviousRaceStillTracking() {
        final TimePoint startOfFirstRace = MillisecondsTimePoint.now();
        final TimePoint startOfSecondRace = startOfFirstRace.plus(Duration.ONE_MINUTE.times(5));
        DynamicTrackedRaceImpl previousTrackedRace = createTestTrackedRace(REGATTA, RACE, BOATCLASS, Collections.<Competitor,Boat> emptyMap(), startOfFirstRace, /* useMarkPassingCalculator */ false);
        previousTrackedRace.setStartOfTrackingReceived(startOfFirstRace);
        trackedRace = createTestTrackedRace(REGATTA, "TestRace2", BOATCLASS, Collections.<Competitor,Boat> emptyMap(), startOfSecondRace, /* useMarkPassingCalculator */ false);
        flexibleLeaderboard = new FlexibleLeaderboardImpl(FLEXIBLELEADERBOARD,
                new ThresholdBasedResultDiscardingRuleImpl(new int[] { 3, 6 }), new LowPoint(), null);
        flexibleLeaderboard.addRace(previousTrackedRace, RACECOLUMN_FLEXIBLELEADERBOARD+"1", false);
        flexibleLeaderboard.addRace(trackedRace, RACECOLUMN_FLEXIBLELEADERBOARD+"2", false);
        Wind wind = new WindImpl(new DegreePosition(12, 13), startOfSecondRace.plus(Duration.ONE_MINUTE), new KnotSpeedWithBearingImpl(
                /* speedInKnots */18, new DegreeBearingImpl(185)));
        assertTrue(previousTrackedRace.takesWindFixWithTimePoint(wind.getTimePoint())); // previous race has tracking still open and takes the fix
        assertTrue(trackedRace.takesWindFixWithTimePoint(wind.getTimePoint())); // tracked race also needs to take the fix as it falls into the regular tracking interval
    }

    @Test
    public void testWindInExtendedLeadIntervalWithPreviousRaceStillTracking() {
        final TimePoint startOfFirstRace = MillisecondsTimePoint.now();
        final TimePoint endOfFirstRace = startOfFirstRace.plus(Duration.ONE_SECOND);
        final TimePoint startOfSecondRace = startOfFirstRace.plus(Duration.ONE_HOUR);
        DynamicTrackedRaceImpl previousTrackedRace = createTestTrackedRace(REGATTA, RACE, BOATCLASS, Collections.<Competitor,Boat> emptyMap(), startOfFirstRace, /* useMarkPassingCalculator */ false);
        previousTrackedRace.setStartOfTrackingReceived(startOfFirstRace);
        previousTrackedRace.setEndOfTrackingReceived(endOfFirstRace); // a very short race...
        trackedRace = createTestTrackedRace(REGATTA, "TestRace2", BOATCLASS, Collections.<Competitor,Boat> emptyMap(), startOfSecondRace, /* useMarkPassingCalculator */ false);
        flexibleLeaderboard = new FlexibleLeaderboardImpl(FLEXIBLELEADERBOARD,
                new ThresholdBasedResultDiscardingRuleImpl(new int[] { 3, 6 }), new LowPoint(), null);
        flexibleLeaderboard.addRace(previousTrackedRace, RACECOLUMN_FLEXIBLELEADERBOARD+"1", false);
        flexibleLeaderboard.addRace(trackedRace, RACECOLUMN_FLEXIBLELEADERBOARD+"2", false);
        // the wind fix is after the grace period of the first race's end, so won't be accepted by it, but within the extended
        // time range before the second race, so shall be accepted by it.
        Wind wind = new WindImpl(new DegreePosition(12, 13),
                endOfFirstRace.plus(TimingConstants.IS_LIVE_GRACE_PERIOD_IN_MILLIS).plus(Duration.ONE_MINUTE), // fix is after the grace period
                new KnotSpeedWithBearingImpl(/* speedInKnots */18, new DegreeBearingImpl(185)));
        assertFalse(previousTrackedRace.takesWindFixWithTimePoint(wind.getTimePoint())); // previous race has tracking closed one second after it started and doesn't accept the fix
        assertTrue(trackedRace.takesWindFixWithTimePoint(wind.getTimePoint())); // tracked race also needs to take the fix as it falls into the regular tracking interval
    }

    @Test
    public void testWindInExtendedLeadIntervalWithNoPreviousRace() {
        final TimePoint startOfFirstRace = MillisecondsTimePoint.now();
        final TimePoint endOfFirstRace = startOfFirstRace.plus(Duration.ONE_SECOND);
        final TimePoint startOfSecondRace = startOfFirstRace.plus(Duration.ONE_HOUR);
        trackedRace = createTestTrackedRace(REGATTA, "TestRace2", BOATCLASS, Collections.<Competitor,Boat> emptyMap(), startOfSecondRace, /* useMarkPassingCalculator */ false);
        flexibleLeaderboard = new FlexibleLeaderboardImpl(FLEXIBLELEADERBOARD,
                new ThresholdBasedResultDiscardingRuleImpl(new int[] { 3, 6 }), new LowPoint(), null);
        flexibleLeaderboard.addRace(trackedRace, RACECOLUMN_FLEXIBLELEADERBOARD, false);
        // the wind fix is within the extended time range before the second race, and there is no previous race, so shall be accepted by it
        Wind wind = new WindImpl(new DegreePosition(12, 13),
                endOfFirstRace.plus(TimingConstants.IS_LIVE_GRACE_PERIOD_IN_MILLIS).plus(Duration.ONE_MINUTE), // fix is after the grace period
                new KnotSpeedWithBearingImpl(/* speedInKnots */18, new DegreeBearingImpl(185)));
        assertTrue(trackedRace.takesWindFixWithTimePoint(wind.getTimePoint()));
    }

    @Test
    public void testWindInExtendedLeadIntervalWithPreviousRaceLongAgo() {
        final TimePoint startOfFirstRace = MillisecondsTimePoint.now();
        final TimePoint endOfFirstRace = startOfFirstRace.plus(Duration.ONE_SECOND);
        final TimePoint startOfSecondRace = startOfFirstRace.plus(TrackedRaceImpl.EXTRA_LONG_TIME_BEFORE_START_TO_TRACK_WIND_MILLIS.times(2));
        DynamicTrackedRaceImpl previousTrackedRace = createTestTrackedRace(REGATTA, RACE, BOATCLASS, Collections.<Competitor,Boat> emptyMap(), startOfFirstRace, /* useMarkPassingCalculator */ false);
        previousTrackedRace.setStartOfTrackingReceived(startOfFirstRace);
        previousTrackedRace.setEndOfTrackingReceived(endOfFirstRace); // a very short race...
        trackedRace = createTestTrackedRace(REGATTA, "TestRace2", BOATCLASS, Collections.<Competitor,Boat> emptyMap(), startOfSecondRace, /* useMarkPassingCalculator */ false);
        flexibleLeaderboard = new FlexibleLeaderboardImpl(FLEXIBLELEADERBOARD,
                new ThresholdBasedResultDiscardingRuleImpl(new int[] { 3, 6 }), new LowPoint(), null);
        flexibleLeaderboard.addRace(previousTrackedRace, RACECOLUMN_FLEXIBLELEADERBOARD+"1", false);
        flexibleLeaderboard.addRace(trackedRace, RACECOLUMN_FLEXIBLELEADERBOARD+"2", false);
        // the wind fix is outside the extended time range before the second race, and the previous race doesn't accept it; shall not be accepted
        Wind wind = new WindImpl(new DegreePosition(12, 13),
                endOfFirstRace.plus(TimingConstants.IS_LIVE_GRACE_PERIOD_IN_MILLIS).plus(Duration.ONE_MINUTE), // fix is after the grace period
                new KnotSpeedWithBearingImpl(/* speedInKnots */18, new DegreeBearingImpl(185)));
        assertFalse(previousTrackedRace.takesWindFixWithTimePoint(wind.getTimePoint())); // fix is after first race's end plus grace period
        assertFalse(trackedRace.takesWindFixWithTimePoint(wind.getTimePoint()));
    }

    @Test
    public void testWindInExtendedLeadIntervalButStillRecordedByPreviousRace() {
        final TimePoint startOfFirstRace = MillisecondsTimePoint.now();
        final TimePoint endOfFirstRace = startOfFirstRace.plus(Duration.ONE_SECOND);
        final TimePoint startOfSecondRace = startOfFirstRace.plus(TrackedRaceImpl.EXTRA_LONG_TIME_BEFORE_START_TO_TRACK_WIND_MILLIS.divide(2));
        DynamicTrackedRaceImpl previousTrackedRace = createTestTrackedRace(REGATTA, RACE, BOATCLASS, Collections.<Competitor,Boat> emptyMap(), startOfFirstRace, /* useMarkPassingCalculator */ false);
        previousTrackedRace.setStartOfTrackingReceived(startOfFirstRace);
        previousTrackedRace.setEndOfTrackingReceived(endOfFirstRace); // a very short race...
        trackedRace = createTestTrackedRace(REGATTA, "TestRace2", BOATCLASS, Collections.<Competitor,Boat> emptyMap(), startOfSecondRace, /* useMarkPassingCalculator */ false);
        flexibleLeaderboard = new FlexibleLeaderboardImpl(FLEXIBLELEADERBOARD,
                new ThresholdBasedResultDiscardingRuleImpl(new int[] { 3, 6 }), new LowPoint(), null);
        flexibleLeaderboard.addRace(previousTrackedRace, RACECOLUMN_FLEXIBLELEADERBOARD+"1", false);
        flexibleLeaderboard.addRace(trackedRace, RACECOLUMN_FLEXIBLELEADERBOARD+"2", false);
        // the wind fix is inside the extended time range before the second race, but the previous race still accepts it; shall not be accepted
        Wind wind = new WindImpl(new DegreePosition(12, 13),
                startOfFirstRace.plus(Duration.ONE_SECOND), // fix is recorded by previous race
                new KnotSpeedWithBearingImpl(/* speedInKnots */18, new DegreeBearingImpl(185)));
        assertTrue(previousTrackedRace.takesWindFixWithTimePoint(wind.getTimePoint())); // fix is after first race's end plus grace period
        assertFalse(trackedRace.takesWindFixWithTimePoint(wind.getTimePoint()));
    }

    @Test
    public void testRaceExecutionOrderProviderAttachDetachWithRaceCollumnInSeries() {
        createTestSetupWithRegattaAndSeries(/* linkSeriesToRegatta */true);
        raceColumnInSeries.setTrackedRace(fleet, trackedRace);
        assertTrue(trackedRace.hasRaceExecutionOrderProvidersAttached());
        raceColumnInSeries.releaseTrackedRace(fleet);
        assertFalse(trackedRace.hasRaceExecutionOrderProvidersAttached());
    }

    @Test
    public void testRaceExecutionOrderProviderAttachDetachWhenSeriesRegattaIsSetAndRemovedAfterTrackedRaceHaveBeenSetToRaceColumns() {
        createTestSetupWithRegattaAndSeries(/* linkSeriesToRegatta */false);
        raceColumnInSeries.setTrackedRace(fleet, trackedRace);
        assertFalse(trackedRace.hasRaceExecutionOrderProvidersAttached());
        series.setRegatta(regatta);
        assertTrue(trackedRace.hasRaceExecutionOrderProvidersAttached());
        series.setRegatta(null);
        assertFalse(trackedRace.hasRaceExecutionOrderProvidersAttached());
    }

    @Test
    public void testThatTrackedRaceReceivesRaceLogWhenSeriesIsLinkedToRegatta() {
        createTestSetupWithRegattaAndSeries(/* linkSeriesToRegatta */false);
        raceColumnInSeries.setTrackedRace(fleet, trackedRace);
        assertNull(raceColumnInSeries.getRaceLog(fleet));
        regatta.addSeries(series);
        final RaceLog raceLog = raceColumnInSeries.getRaceLog(fleet);
        assertNotNull(raceLog);
        assertNotNull(trackedRace.getRaceLog(raceLog.getId()));
    }

    private void createTestSetupWithRegattaAndSeries(boolean linkSeriesToRegatta) {
        trackedRace = createTestTrackedRace(REGATTA, RACE, BOATCLASS, Collections.<Competitor,Boat> emptyMap(),
                MillisecondsTimePoint.now(), /* useMarkPassingCalculator */ false);
        fleet = new FleetImpl(FLEET);
        Set<Fleet> fleets = new HashSet<>();
        fleets.add(fleet);
        series = new SeriesImpl(SERIES, false, /* isFleetsCanRunInParallel */ true, fleets, new HashSet<String>(), null);
        Set<Series> seriesSet = new HashSet<>();
        if (linkSeriesToRegatta) {
            seriesSet.add(series);
        }
        BoatClass boatClass = new BoatClassImpl(BOATCLASS, true);
        raceColumnInSeries = series.addRaceColumn(RACECOLUMN_SERIES, null);
        ScoringScheme scoringScheme = new LowPoint();
        regatta = new RegattaImpl(RegattaImpl.getDefaultName(REGATTA, boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, /* startDate */null, /* endDate */null, 
                seriesSet, false, scoringScheme, UUID.randomUUID(), null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
    }
    
    /**
     * See bug 3173. When a race is linked more than once to a fleet in a series, the "previous race"
     * definition's transitive closure may become cyclic, and a race may be considered its own
     * direct or transitive predecessor. This can lead to endless recursion in the {@link TrackedRace#takesWindFix(Wind)}
     * implementation which traverses a race's predecessors recursively.
     */
    @Test
    public void testCyclicPreviousRacesSequence() {
        createTestSetupWithRegattaAndSeries(/* linkSeriesToRegatta */ true);
        final RaceColumnInSeries r1 = series.addRaceColumn("R1", /* trackedRegattaRegistry */ null);
        final RaceColumnInSeries r2 = series.addRaceColumn("R2", /* trackedRegattaRegistry */ null);
        final RaceColumnInSeries r3 = series.addRaceColumn("R2", /* trackedRegattaRegistry */ null);
        final RaceColumnInSeries r4 = series.addRaceColumn("R2", /* trackedRegattaRegistry */ null);
        final TimePoint startOfFirstRace = MillisecondsTimePoint.now();
        final TimePoint endOfFirstRace = startOfFirstRace.plus(Duration.ONE_MINUTE);
        final TimePoint startOfSecondRace = endOfFirstRace.plus(Duration.ONE_MINUTE);
        final TimePoint endOfSecondRace = startOfSecondRace.plus(Duration.ONE_MINUTE);
        final TimePoint startOfThirdRace = endOfSecondRace.plus(Duration.ONE_MINUTE);
        final TimePoint endOfThirdRace = startOfThirdRace.plus(Duration.ONE_MINUTE);
        final TrackedRace tr1 = createTrackedRace("FirstRace", startOfFirstRace, endOfFirstRace);
        final TrackedRace tr2 = createTrackedRace("SecondRace", startOfSecondRace, endOfSecondRace);
        final TrackedRace tr3 = createTrackedRace("ThirdRace", startOfThirdRace, endOfThirdRace);
        r1.setTrackedRace(fleet, tr3);
        r2.setTrackedRace(fleet, tr1);
        r3.setTrackedRace(fleet, tr2);
        r4.setTrackedRace(fleet, tr3); // produce a cycle because now transitively tr1 has itself as a predecessor
        assertTrue(tr3.takesWindFixWithTimePoint(new WindImpl(new DegreePosition(12, 13),
                startOfThirdRace.minus(TrackedRaceImpl.EXTRA_LONG_TIME_BEFORE_START_TO_TRACK_WIND_MILLIS.divide(2)),
                new KnotSpeedWithBearingImpl(/* speedInKnots */18, new DegreeBearingImpl(185))).getTimePoint()));
    }

    /**
     * See bug 3173. When a race is linked more than once to a fleet in a series, the "previous race"
     * definition's transitive closure may become cyclic, and a race may be considered its own
     * direct or transitive predecessor. This can lead to endless recursion in the {@link TrackedRace#takesWindFix(Wind)}
     * implementation which traverses a race's predecessors recursively.
     */
    @Test
    public void testCyclicPreviousRacesSequenceUsingSingleRace() {
        createTestSetupWithRegattaAndSeries(/* linkSeriesToRegatta */ true);
        final RaceColumnInSeries r1 = series.addRaceColumn("R1", /* trackedRegattaRegistry */ null);
        final RaceColumnInSeries r2 = series.addRaceColumn("R2", /* trackedRegattaRegistry */ null);
        final TimePoint startOfFirstRace = MillisecondsTimePoint.now();
        final TimePoint endOfFirstRace = startOfFirstRace.plus(Duration.ONE_MINUTE);
        final TrackedRace tr1 = createTrackedRace("FirstRace", startOfFirstRace, endOfFirstRace);
        r1.setTrackedRace(fleet, tr1);
        r2.setTrackedRace(fleet, tr1);
        assertTrue(tr1.takesWindFixWithTimePoint(new WindImpl(new DegreePosition(12, 13),
                startOfFirstRace.minus(TrackedRaceImpl.EXTRA_LONG_TIME_BEFORE_START_TO_TRACK_WIND_MILLIS.divide(2)),
                new KnotSpeedWithBearingImpl(/* speedInKnots */18, new DegreeBearingImpl(185))).getTimePoint()));
    }

    private DynamicTrackedRace createTrackedRace(final String name, final TimePoint startOfRace, final TimePoint endOfRace) {
        DynamicTrackedRace trackedRace = createTestTrackedRace(REGATTA, name, BOATCLASS, Collections.<Competitor,Boat> emptyMap(), startOfRace, /* useMarkPassingCalculator */ false);
        trackedRace.setStartOfTrackingReceived(startOfRace);
        trackedRace.setEndOfTrackingReceived(endOfRace);
        return trackedRace;
    }
}
