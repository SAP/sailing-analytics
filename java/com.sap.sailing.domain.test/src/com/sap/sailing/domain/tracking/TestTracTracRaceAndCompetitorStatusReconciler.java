package com.sap.sailing.domain.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.AbortingFlagFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFlagEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogPassChangeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.impl.RaceAndCompetitorStatusWithRaceLogReconciler;
import com.sap.sailing.server.impl.RaceLogScoringReplicator;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.RacingEventServiceOperation;
import com.sap.sse.common.Color;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.metering.CPUMeter;
import com.tractrac.model.lib.api.event.ICompetitor;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.model.lib.api.event.IRaceCompetitor;
import com.tractrac.model.lib.api.event.RaceCompetitorStatusType;
import com.tractrac.model.lib.api.event.RaceStatusType;

/**
 * See also bug 5154 and {@link RaceAndCompetitorStatusWithRaceLogReconciler}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class TestTracTracRaceAndCompetitorStatusReconciler {
    private static final String TEST_LEADERBOARD_NAME = "test";
    private static final String R1 = "R1";
    private AbstractLogEventAuthor author;
    private TrackedRace trackedRace;
    private IRace tractracRace;
    private RaceLog raceLog;
    private RaceAndCompetitorStatusWithRaceLogReconcilerWithPublicResultFetcher reconciler;
    private TimePoint startOfPass;
    private IRaceCompetitor tractracRaceCompetitor;
    private ICompetitor tractracCompetitor;
    private Competitor competitor;
    private FlexibleLeaderboard leaderboard;
    private static class RaceAndCompetitorStatusWithRaceLogReconcilerWithPublicResultFetcher extends RaceAndCompetitorStatusWithRaceLogReconciler {
        public RaceAndCompetitorStatusWithRaceLogReconcilerWithPublicResultFetcher(DomainFactory domainFactory,
                RaceLogResolver raceLogResolver, IRace tractracRace) {
            super(domainFactory, raceLogResolver, tractracRace);
        }

        @Override
        public Pair<CompetitorResult, TimePoint> getRaceLogResultAndCreationTimePointForCompetitor(
                RaceLog raceLog, Competitor competitor) {
            return super.getRaceLogResultAndCreationTimePointForCompetitor(raceLog, competitor);
        }
    }
    
    @BeforeEach
    public void setUp() {
        author = new LogEventAuthorImpl("me", 1);
        startOfPass = MillisecondsTimePoint.now();
        tractracRace = mock(IRace.class);
        when(tractracRace.getStatus()).thenReturn(RaceStatusType.RACING);
        trackedRace = mock(TrackedRace.class);
        when(trackedRace.getRaceLogResolver()).thenReturn(new RaceLogAndTrackedRaceResolver() {
            @Override
            public RaceLog resolve(SimpleRaceLogIdentifier identifier) {
                return null;
            }

            @Override
            public TrackedRace resolveTrackedRace(SimpleRaceLogIdentifier identifier) {
                return null;
            }
        });
        tractracCompetitor = mock(ICompetitor.class);
        tractracRaceCompetitor = mock(IRaceCompetitor.class);
        when(tractracRaceCompetitor.getRace()).thenReturn(tractracRace);
        when(tractracRaceCompetitor.getCompetitor()).thenReturn(tractracCompetitor);
        final String competitorName = "The Competitor";
        final UUID competitorId = UUID.randomUUID();
        DynamicBoat b = new BoatImpl(competitorId, competitorName + "'s boat", new BoatClassImpl("505", /* typicallyStartsUpwind */true), null, null);
        competitor = DomainFactory.INSTANCE.getBaseDomainFactory().getOrCreateCompetitorWithBoat(
                competitorId, competitorName, "TC", Color.RED, null, null, new TeamImpl("STG", Collections.singleton(
                        new PersonImpl(competitorName, new NationalityImpl("GER"),
                        /* dateOfBirth */null, "This is famous " + competitorName)), new PersonImpl("Rigo van Maas",
                        new NationalityImpl("NED"),
                        /* dateOfBirth */null, "This is Rigo, the coach")), 
                        /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, b, /* store */ false);
        when(tractracCompetitor.getId()).thenReturn((UUID) competitor.getId());
        final RaceDefinition raceDefinition = mock(RaceDefinitionImpl.class);
        when(raceDefinition.getCompetitors()).thenReturn(Collections.singleton(competitor));
        when(trackedRace.getRace()).thenReturn(raceDefinition);
        final TrackedRegatta trackedRegatta = mock(TrackedRegatta.class);
        when(trackedRegatta.getCPUMeter()).thenReturn(CPUMeter.create());
        when(trackedRace.getTrackedRegatta()).thenReturn(trackedRegatta);
        leaderboard = new FlexibleLeaderboardImpl(TEST_LEADERBOARD_NAME, new ThresholdBasedResultDiscardingRuleImpl(new int[0]), new LowPoint(), new CourseAreaImpl("area A", new UUID(1200, 1200) , new DegreePosition(100, 100), new MeterDistance(100)));
        leaderboard.addRace(trackedRace, R1, /* medalRace */ false);
        raceLog = leaderboard.getRacelog(R1, LeaderboardNameConstants.DEFAULT_FLEET_NAME);
        when(trackedRace.getAttachedRaceLogs()).thenReturn(Collections.singleton(raceLog));
        reconciler = new RaceAndCompetitorStatusWithRaceLogReconcilerWithPublicResultFetcher(DomainFactory.INSTANCE, new RaceLogResolver() {
            @Override
            public RaceLog resolve(SimpleRaceLogIdentifier identifier) {
                return raceLog;
            }
        }, tractracRace);
        final RacingEventService racingEventService = mock(RacingEventService.class);
        when(racingEventService.getLeaderboardByName(TEST_LEADERBOARD_NAME)).thenReturn(leaderboard);
        when(racingEventService.getBaseDomainFactory()).thenReturn(com.sap.sailing.domain.base.DomainFactory.INSTANCE);
        when(racingEventService.apply(ArgumentMatchers.any(RacingEventServiceOperation.class))).thenAnswer(
                invocation -> {
                    final RacingEventServiceOperation<?> operation = invocation.getArgument(0);
                    return operation.applyTo(racingEventService);
                });
        final RaceLogScoringReplicator raceLogScoringReplicator = new RaceLogScoringReplicator(racingEventService);
        leaderboard.getRaceColumnByName(R1).addRaceColumnListener(raceLogScoringReplicator);
        raceLog.add(new RaceLogPassChangeEventImpl(startOfPass, author, /* pPassId */ 1));
    }
    
    @Test
    public void testAbandonInFirstPassAndRestart() {
        assertNull(new AbortingFlagFinder(raceLog).analyze());
        when(tractracRace.getStatus()).thenReturn(RaceStatusType.ABANDONED);
        final TimePoint abandonTimePoint = startOfPass.plus(Duration.ONE_MINUTE);
        when(tractracRace.getStatusLastChangedTime()).thenReturn(abandonTimePoint.asMillis());
        reconciler.reconcileRaceStatus(tractracRace, trackedRace);
        final RaceLogFlagEvent abandonFlagEvent = new AbortingFlagFinder(raceLog).analyze();
        assertNotNull(abandonFlagEvent);
        assertSame(Flags.NOVEMBER, abandonFlagEvent.getUpperFlag());
        assertTrue(abandonFlagEvent.isDisplayed());
        assertEquals(abandonTimePoint, abandonFlagEvent.getLogicalTimePoint());
        // now change status back to START
        when(tractracRace.getStatus()).thenReturn(RaceStatusType.START);
        final TimePoint restartTimePoint = startOfPass.plus(Duration.ONE_MINUTE.times(2));
        when(tractracRace.getStatusLastChangedTime()).thenReturn(restartTimePoint.asMillis());
        reconciler.reconcileRaceStatus(tractracRace, trackedRace);
        final RaceLogFlagEvent newAbandonFlagEvent = new AbortingFlagFinder(raceLog).analyze();
        assertNull(newAbandonFlagEvent);
        assertEquals(3, raceLog.getCurrentPassId()); // abandoning also creates the next pass immediately
        reconciler.reconcileRaceStatus(tractracRace, trackedRace);
        assertEquals(3, raceLog.getCurrentPassId()); // assert that reconciliation is idempotent and does not add another pass
    }

    @Test
    public void testManualAbandonButLaterTracTracGeneralRecall() {
        final TimePoint manualAbortTimePoint = startOfPass.plus(Duration.ONE_MINUTE);
        raceLog.add(new RaceLogFlagEventImpl(manualAbortTimePoint, author, /* pass id */ 1, Flags.NOVEMBER, /* lower flag */ null, /* isDisplayed */ true));
        raceLog.add(new RaceLogPassChangeEventImpl(manualAbortTimePoint, author, /* pass id */ 2));
        when(tractracRace.getStatus()).thenReturn(RaceStatusType.GENERAL_RECALL);
        final TimePoint generalRecallTimePoint = manualAbortTimePoint.plus(Duration.ONE_MINUTE);
        when(tractracRace.getStatusLastChangedTime()).thenReturn(generalRecallTimePoint.asMillis());
        reconciler.reconcileRaceStatus(tractracRace, trackedRace);
        final AbortingFlagFinder abortingFlagFinder = new AbortingFlagFinder(raceLog);
        final RaceLogFlagEvent generalRecallFlagEvent = abortingFlagFinder.analyze();
        assertNotNull(generalRecallFlagEvent);
        assertSame(Flags.FIRSTSUBSTITUTE, generalRecallFlagEvent.getUpperFlag());
        assertTrue(generalRecallFlagEvent.isDisplayed());
        assertEquals(generalRecallTimePoint, generalRecallFlagEvent.getLogicalTimePoint());
        assertEquals(2, generalRecallFlagEvent.getPassId());
        assertEquals(3, raceLog.getCurrentPassId());
        reconciler.reconcileRaceStatus(tractracRace, trackedRace); // assert that reconciliation is idempotent
        assertEquals(3, raceLog.getCurrentPassId());
        assertSame(generalRecallFlagEvent, abortingFlagFinder.analyze());
    }

    @Test
    public void testManualAbandonAndEarlierTracTracGeneralRecallWillBeIgnored() {
        final TimePoint manualAbortTimePoint = startOfPass.plus(Duration.ONE_MINUTE);
        raceLog.add(new RaceLogFlagEventImpl(manualAbortTimePoint, author, /* pass id */ 1, Flags.NOVEMBER, /* lower flag */ null, /* isDisplayed */ true));
        raceLog.add(new RaceLogPassChangeEventImpl(manualAbortTimePoint, author, /* pass id */ 2));
        when(tractracRace.getStatus()).thenReturn(RaceStatusType.GENERAL_RECALL);
        final TimePoint generalRecallTimePoint = manualAbortTimePoint.minus(Duration.ONE_MINUTE.times(2));
        when(tractracRace.getStatusLastChangedTime()).thenReturn(generalRecallTimePoint.asMillis());
        reconciler.reconcileRaceStatus(tractracRace, trackedRace);
        final AbortingFlagFinder abortingFlagFinder = new AbortingFlagFinder(raceLog);
        final RaceLogFlagEvent abortFlagEvent = abortingFlagFinder.analyze();
        assertNotNull(abortFlagEvent);
        assertSame(Flags.NOVEMBER, abortFlagEvent.getUpperFlag());
        assertTrue(abortFlagEvent.isDisplayed());
        assertEquals(manualAbortTimePoint, abortFlagEvent.getLogicalTimePoint());
        assertEquals(1, abortFlagEvent.getPassId());
        assertEquals(2, raceLog.getCurrentPassId());
        reconciler.reconcileRaceStatus(tractracRace, trackedRace); // assert that reconciliation is idempotent
        assertEquals(2, raceLog.getCurrentPassId());
        assertSame(abortFlagEvent, abortingFlagFinder.analyze());
    }

    @Test
    public void testManualAbandonThenNewStartTimeShouldNotCauseChangeForTracTracStatusNONE() {
        final TimePoint manualAbortTimePoint = startOfPass.plus(Duration.ONE_MINUTE);
        final TimePoint newStartTimePoint = startOfPass.plus(Duration.ONE_MINUTE.times(2));
        raceLog.add(new RaceLogFlagEventImpl(manualAbortTimePoint, author, /* pass id */ 1, Flags.NOVEMBER, /* lower flag */ null, /* isDisplayed */ true));
        raceLog.add(new RaceLogPassChangeEventImpl(manualAbortTimePoint, author, /* pass id */ 2));
        raceLog.add(new RaceLogStartTimeEventImpl(newStartTimePoint, author, /* pass id */ 2, newStartTimePoint, /* courseAreaId */ null));
        when(tractracRace.getStatus()).thenReturn(RaceStatusType.NONE);
        when(trackedRace.getRaceLogResolver()).thenReturn(new RaceLogAndTrackedRaceResolver() {
            @Override
            public RaceLog resolve(SimpleRaceLogIdentifier identifier) {
                return null;
            }

            @Override
            public TrackedRace resolveTrackedRace(SimpleRaceLogIdentifier identifier) {
                return null;
            }
        });
        final TimePoint noneStatusTimePoint = manualAbortTimePoint.plus(Duration.ONE_MINUTE.times(1));
        when(tractracRace.getStatusLastChangedTime()).thenReturn(noneStatusTimePoint.asMillis());
        final AbortingFlagFinder abortingFlagFinder = new AbortingFlagFinder(raceLog);
        final RaceLogFlagEvent abortFlagEvent = abortingFlagFinder.analyze();
        assertNotNull(abortFlagEvent);
        assertSame(Flags.NOVEMBER, abortFlagEvent.getUpperFlag());
        assertTrue(abortFlagEvent.isDisplayed());
        assertEquals(manualAbortTimePoint, abortFlagEvent.getLogicalTimePoint());
        assertEquals(1, abortFlagEvent.getPassId());
        assertEquals(2, raceLog.getCurrentPassId());
        reconciler.reconcileRaceStatus(tractracRace, trackedRace);
        assertEquals(2, raceLog.getCurrentPassId()); // assert that the NONE state did not add a new pass
        assertSame(abortFlagEvent, abortingFlagFinder.analyze());
    }

    @Test
    public void testManualAbandonThenNoStartTimeInNextPassYetMayCauseNewPassChangeForTracTracStatusNONE() {
        final TimePoint manualAbortTimePoint = startOfPass.plus(Duration.ONE_MINUTE);
        raceLog.add(new RaceLogFlagEventImpl(manualAbortTimePoint, author, /* pass id */ 1, Flags.NOVEMBER, /* lower flag */ null, /* isDisplayed */ true));
        raceLog.add(new RaceLogPassChangeEventImpl(manualAbortTimePoint, author, /* pass id */ 2));
        when(tractracRace.getStatus()).thenReturn(RaceStatusType.NONE);
        final TimePoint noneStatusTimePoint = manualAbortTimePoint.plus(Duration.ONE_MINUTE.times(1));
        when(tractracRace.getStatusLastChangedTime()).thenReturn(noneStatusTimePoint.asMillis());
        final AbortingFlagFinder abortingFlagFinder = new AbortingFlagFinder(raceLog);
        final RaceLogFlagEvent abortFlagEvent = abortingFlagFinder.analyze();
        assertNotNull(abortFlagEvent);
        assertSame(Flags.NOVEMBER, abortFlagEvent.getUpperFlag());
        assertTrue(abortFlagEvent.isDisplayed());
        assertEquals(manualAbortTimePoint, abortFlagEvent.getLogicalTimePoint());
        assertEquals(1, abortFlagEvent.getPassId());
        assertEquals(2, raceLog.getCurrentPassId());
        reconciler.reconcileRaceStatus(tractracRace, trackedRace);
        assertTrue(raceLog.getCurrentPassId() >= 2); // if NONE advanced an empty pass by another one we don't mind
    }

    @Test
    public void testIRMUpdateFromTracTracMapsToRaceLogCompetitorResult() {
        // emulate we received a BFD for a competitor a second after the start of the pass
        final TimePoint resultTimePoint = startOfPass.plus(Duration.ONE_SECOND);
        when(tractracRaceCompetitor.getStatusLastChangedTime()).thenReturn(resultTimePoint.asMillis());
        when(tractracRaceCompetitor.getStatus()).thenReturn(RaceCompetitorStatusType.BFD);
        final RaceLog raceLog = trackedRace.getAttachedRaceLogs().iterator().next();
        reconciler.reconcileCompetitorStatus(tractracRaceCompetitor, trackedRace);
        {
            final Pair<CompetitorResult, TimePoint> raceLogBasedResult = reconciler.getRaceLogResultAndCreationTimePointForCompetitor(raceLog, competitor);
            assertNotNull(raceLogBasedResult);
            assertEquals(resultTimePoint, raceLogBasedResult.getB());
            assertEquals(MaxPointsReason.BFD, raceLogBasedResult.getA().getMaxPointsReason());
        }
        // now simulate an "outdated" TracTrac event and assert that it has no impact on a newer result:
        final TimePoint outdatedResultTimePoint = startOfPass.plus(Duration.ONE_SECOND.divide(2));
        when(tractracRaceCompetitor.getStatusLastChangedTime()).thenReturn(outdatedResultTimePoint.asMillis());
        when(tractracRaceCompetitor.getStatus()).thenReturn(RaceCompetitorStatusType.DNF);
        reconciler.reconcileCompetitorStatus(tractracRaceCompetitor, trackedRace);
        {
            final Pair<CompetitorResult, TimePoint> raceLogBasedResult = reconciler.getRaceLogResultAndCreationTimePointForCompetitor(raceLog, competitor);
            assertNotNull(raceLogBasedResult);
            assertEquals(resultTimePoint, raceLogBasedResult.getB()); // still expecting to see the result from the later time point
            assertEquals(MaxPointsReason.BFD, raceLogBasedResult.getA().getMaxPointsReason());
        }
        // now simulate a newer TracTrac event and assert that it updates the result:
        final TimePoint newerResultTimePoint = startOfPass.plus(Duration.ONE_SECOND.times(2));
        when(tractracRaceCompetitor.getStatusLastChangedTime()).thenReturn(newerResultTimePoint.asMillis());
        when(tractracRaceCompetitor.getStatus()).thenReturn(RaceCompetitorStatusType.DNC);
        reconciler.reconcileCompetitorStatus(tractracRaceCompetitor, trackedRace);
        {
            final Pair<CompetitorResult, TimePoint> raceLogBasedResult = reconciler.getRaceLogResultAndCreationTimePointForCompetitor(raceLog, competitor);
            assertNotNull(raceLogBasedResult);
            assertEquals(newerResultTimePoint, raceLogBasedResult.getB()); // expecting to see the result from the newer time point
            assertEquals(MaxPointsReason.DNC, raceLogBasedResult.getA().getMaxPointsReason());
        }
        // now simulate a yet newer TracTrac event that is expected to reset the "IRM" / MaxPointsReason:
        final TimePoint yetNewerResultTimePoint = startOfPass.plus(Duration.ONE_SECOND.times(3));
        when(tractracRaceCompetitor.getStatusLastChangedTime()).thenReturn(yetNewerResultTimePoint.asMillis());
        when(tractracRaceCompetitor.getStatus()).thenReturn(RaceCompetitorStatusType.NO_DATA);
        reconciler.reconcileCompetitorStatus(tractracRaceCompetitor, trackedRace);
        {
            final Pair<CompetitorResult, TimePoint> raceLogBasedResult = reconciler.getRaceLogResultAndCreationTimePointForCompetitor(raceLog, competitor);
            assertNotNull(raceLogBasedResult);
            assertEquals(yetNewerResultTimePoint, raceLogBasedResult.getB()); // expecting to see the result from the yet newer time point
            assertEquals(MaxPointsReason.NONE, raceLogBasedResult.getA().getMaxPointsReason());
        }
    }

    @Test
    public void testOfficialRankUpdateToRaceLogCompetitorResult() {
        // emulate we received a valid official rank for a competitor a second after the start of the pass
        final TimePoint resultTimePoint = startOfPass.plus(Duration.ONE_SECOND);
        when(tractracRaceCompetitor.getStatusLastChangedTime()).thenReturn(resultTimePoint.asMillis());
        when(tractracRaceCompetitor.getOfficialRank()).thenReturn(42);
        when(tractracRaceCompetitor.getStatus()).thenReturn(RaceCompetitorStatusType.NO_DATA);
        reconciler.reconcileCompetitorStatus(tractracRaceCompetitor, trackedRace);
        {
            final Pair<CompetitorResult, TimePoint> raceLogBasedResult = reconciler.getRaceLogResultAndCreationTimePointForCompetitor(raceLog, competitor);
            assertNotNull(raceLogBasedResult);
            assertEquals(resultTimePoint, raceLogBasedResult.getB());
            assertEquals(MaxPointsReason.NONE, raceLogBasedResult.getA().getMaxPointsReason());
            assertEquals(42, raceLogBasedResult.getA().getOneBasedRank());
        }
        // now simulate an "outdated" TracTrac event and assert that it has no impact on a newer result:
        final TimePoint outdatedResultTimePoint = startOfPass.plus(Duration.ONE_SECOND.divide(2));
        when(tractracRaceCompetitor.getStatusLastChangedTime()).thenReturn(outdatedResultTimePoint.asMillis());
        when(tractracRaceCompetitor.getOfficialRank()).thenReturn(43);
        reconciler.reconcileCompetitorStatus(tractracRaceCompetitor, trackedRace);
        {
            final Pair<CompetitorResult, TimePoint> raceLogBasedResult = reconciler.getRaceLogResultAndCreationTimePointForCompetitor(raceLog, competitor);
            assertNotNull(raceLogBasedResult);
            assertEquals(resultTimePoint, raceLogBasedResult.getB());
            assertEquals(MaxPointsReason.NONE, raceLogBasedResult.getA().getMaxPointsReason());
            assertEquals(42, raceLogBasedResult.getA().getOneBasedRank());
        }
        // now simulate a newer TracTrac event and assert that it updates the result:
        final TimePoint newerResultTimePoint = startOfPass.plus(Duration.ONE_SECOND.times(2));
        when(tractracRaceCompetitor.getStatusLastChangedTime()).thenReturn(newerResultTimePoint.asMillis());
        when(tractracRaceCompetitor.getOfficialRank()).thenReturn(44);
        reconciler.reconcileCompetitorStatus(tractracRaceCompetitor, trackedRace);
        {
            final Pair<CompetitorResult, TimePoint> raceLogBasedResult = reconciler.getRaceLogResultAndCreationTimePointForCompetitor(raceLog, competitor);
            assertNotNull(raceLogBasedResult);
            assertEquals(newerResultTimePoint, raceLogBasedResult.getB());
            assertEquals(MaxPointsReason.NONE, raceLogBasedResult.getA().getMaxPointsReason());
            assertEquals(44, raceLogBasedResult.getA().getOneBasedRank());
        }
        // now simulate a yet newer TracTrac event that resets the official rank to 0, stating that there is no official rank yet
        final TimePoint yetNewerResultTimePoint = startOfPass.plus(Duration.ONE_SECOND.times(3));
        when(tractracRaceCompetitor.getStatusLastChangedTime()).thenReturn(yetNewerResultTimePoint.asMillis());
        when(tractracRaceCompetitor.getOfficialRank()).thenReturn(0);
        reconciler.reconcileCompetitorStatus(tractracRaceCompetitor, trackedRace);
        {
            final Pair<CompetitorResult, TimePoint> raceLogBasedResult = reconciler.getRaceLogResultAndCreationTimePointForCompetitor(raceLog, competitor);
            assertNotNull(raceLogBasedResult);
            assertEquals(yetNewerResultTimePoint, raceLogBasedResult.getB());
            assertEquals(MaxPointsReason.NONE, raceLogBasedResult.getA().getMaxPointsReason());
            assertEquals(0, raceLogBasedResult.getA().getOneBasedRank());
        }      
    }
    
    @Test
    public void testOfficialNullFinishTimeAndZeroRank() {
        final TimePoint resultTimePoint = startOfPass.plus(Duration.ONE_SECOND.times(1));
        when(tractracRaceCompetitor.getStatus()).thenReturn(RaceCompetitorStatusType.NO_DATA);
        when(tractracRaceCompetitor.getStatusLastChangedTime()).thenReturn(resultTimePoint.asMillis());
        when(tractracRaceCompetitor.getOfficialRank()).thenReturn(0);
        when(trackedRace.getFinishingTime()).thenReturn(null);
        reconciler.reconcileCompetitorStatus(tractracRaceCompetitor, trackedRace);
        {
            // assert scorecorrection msg afterwards is unchanged.
            assertNull(leaderboard.getScoreCorrection().getComment());
            assertNull(leaderboard.getScoreCorrection().getTimePointOfLastCorrectionsValidity());
        }
    }

    @Test
    public void testOfficialFinishTimeAndValidRankUpdateScoreCorrectionMetadata() {
        final TimePoint resultTimePoint = startOfPass.plus(Duration.ONE_SECOND.times(1));
        when(tractracRaceCompetitor.getStatus()).thenReturn(RaceCompetitorStatusType.FINISH_CONFIRMED);
        when(tractracRaceCompetitor.getStatusLastChangedTime()).thenReturn(resultTimePoint.asMillis());
        when(tractracRaceCompetitor.getOfficialRank()).thenReturn(4);
        when(trackedRace.getFinishingTime()).thenReturn(null);
        reconciler.reconcileCompetitorStatus(tractracRaceCompetitor, trackedRace);
        {
            // assert scorecorrection msg afterwards is unchanged.
            assertEquals("Results of race "+R1+" have been updated.", leaderboard.getScoreCorrection().getComment());
            assertNotNull(leaderboard.getScoreCorrection().getTimePointOfLastCorrectionsValidity());
        }
    }
}
