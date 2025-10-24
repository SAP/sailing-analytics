package com.sap.sailing.domain.tractracadapter.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult.MergeState;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogPassChangeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogRevokeEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.AbortingFlagFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.AbstractFinishPositioningListFinder.CompetitorResultsAndTheirCreationTimePoints;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.impl.BaseRaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultImpl;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultsImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFinishPositioningConfirmedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFlagEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogPassChangeEventImpl;
import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.RaceStateImpl;
import com.sap.sailing.domain.abstractlog.race.state.impl.ReadonlyRaceStateImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.model.lib.api.event.IRaceCompetitor;
import com.tractrac.model.lib.api.event.RaceCompetitorStatusType;
import com.tractrac.model.lib.api.event.RaceStatusType;

/**
 * A service that understands the different {@link IRaceCompetitor#getStatus() competitor statuses} and the
 * {@link IRace#getStatus() race status} and can reconcile them with the {@link RaceLog} of a {@link TrackedRace} such
 * that afterwards the {@link RaceLog} is guaranteed to describe the competitor status accordingly. When the
 * reconciliation is requested and the {@link RaceLog} already represents the competitor status appropriately, no
 * changes will be applied to the race log.<p>
 * 
 * When you create an object of this type, make sure to inform it about race logs being attached to / detached from
 * the {@link TrackedRace} once the tracked race is known.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class RaceAndCompetitorStatusWithRaceLogReconciler {
    private static final Logger logger = Logger.getLogger(RaceAndCompetitorStatusWithRaceLogReconciler.class.getName());
    private final DomainFactory domainFactory;
    private final RaceLogResolver raceLogResolver;
    private final LogEventAuthorImpl raceLogEventAuthor;
    private final IRace tractracRace;
    private final Map<Pair<TrackedRace, RaceLog>, RaceLogListener> raceLogListeners;
    private OfficialCompetitorUpdateProvider officialCompetitorUpdateProvider;
    private final static Map<RaceStatusType, Flags> flagForRaceStatus;
    
    /**
     * When this reconciler adds {@link RaceLogFinishPositioningConfirmedEvent} events to the race log on which it listens with its
     * {@link RaceLogListener}, in order to avoid endless recursion those events are added to this thread-safe queue. When any of these
     * events then expectedly reaches this reconciler's race log listener, it is removed from this queue again to clean up and
     * avoid leaking.
     */
    private final ConcurrentLinkedQueue<RaceLogEvent> raceLogEventsAddedToRaceLogByMyself;
    
    static {
        flagForRaceStatus = new HashMap<>();
        flagForRaceStatus.put(RaceStatusType.ABANDONED, Flags.NOVEMBER);
        flagForRaceStatus.put(RaceStatusType.POSTPONED, Flags.AP);
        flagForRaceStatus.put(RaceStatusType.GENERAL_RECALL, Flags.FIRSTSUBSTITUTE);
    }
    
    /**
     * Handles those race log events that may have an impact on the reconciliation process, including revocations and
     * pass changes, and invokes
     * {@link RaceAndCompetitorStatusWithRaceLogReconciler#reconcileCompetitorStatus(IRaceCompetitor, TrackedRace)} or
     * {@link RaceAndCompetitorStatusWithRaceLogReconciler#reconcileRaceStatus(IRace, TrackedRace)} or both, depending
     * on the type of even. Instances of this type are registered with race logs because the enclosing
     * {@link RaceAndCompetitorStatusWithRaceLogReconciler} object has to get informed about race log attachments/detachments
     * in its {@link RaceAndCompetitorStatusWithRaceLogReconciler#raceLogAttached(TrackedRace, RaceLog)} and
     * {@link RaceAndCompetitorStatusWithRaceLogReconciler#raceLogDetached(TrackedRace, RaceLog)} methods.<p>
     * 
     * In order to avoid endless recursions for events added to the race log by the enclosing reconciler itself, a
     * thread-safe collection of those events added by the enclosing reconciler is maintained in
     * {@link RaceAndCompetitorStatusWithRaceLogReconciler#raceLogEventsAddedToRaceLogByMyself}. Each {@code visit}
     * method overridden by this listener must check for the event received in and remove it from that collection,
     * and in case it was contained in the collection ignore it, so as to avoid the endless recursion. See also bug 5565
     * for details.
     * 
     * @author Axel Uhl (D043530)
     *
     */
    private class RaceLogListener extends BaseRaceLogEventVisitor {
        private final RaceLog raceLog;
        private final TrackedRace trackedRace;
        
        public RaceLogListener(TrackedRace trackedRace, RaceLog raceLog) {
            super();
            this.raceLog = raceLog;
            this.trackedRace = trackedRace;
            reconcileRaceStatus(tractracRace, trackedRace);
            reconcileAllCompetitors(trackedRace);
        }

        private void reconcileAllCompetitors(TrackedRace trackedRace) {
            for (final Competitor competitor : trackedRace.getRace().getCompetitors()) {
                if (competitor.getId() instanceof UUID) {
                    final IRaceCompetitor raceCompetitor = tractracRace.getRaceCompetitor((UUID) competitor.getId());
                    reconcileCompetitorStatus(raceCompetitor, trackedRace);
                }
            }
        }

        @Override
        public void visit(RaceLogFlagEvent event) {
            if (!raceLogEventsAddedToRaceLogByMyself.remove(event)) {
                reconcileRaceStatus(tractracRace, trackedRace);
            }
        }

        @Override
        public void visit(RaceLogPassChangeEvent event) {
            if (!raceLogEventsAddedToRaceLogByMyself.remove(event)) {
                reconcileRaceStatus(tractracRace, trackedRace);
                reconcileAllCompetitors(trackedRace);
            }
        }

        @Override
        public void visit(RaceLogFinishPositioningConfirmedEvent event) {
            if (!raceLogEventsAddedToRaceLogByMyself.remove(event)) {
                reconcileCompetitorsWithResults(event);
            }
        }

        @Override
        public void visit(RaceLogRevokeEvent event) {
            if (!raceLogEventsAddedToRaceLogByMyself.remove(event)) {
                final RaceLogEvent revokedEvent;
                raceLog.lockForRead();
                try {
                    revokedEvent = raceLog.getEventById(event.getRevokedEventId());
                } finally {
                    raceLog.unlockAfterRead();
                }
                if (revokedEvent != null) {
                    if (revokedEvent instanceof RaceLogFinishPositioningConfirmedEvent) {
                        final RaceLogFinishPositioningConfirmedEvent revokedResultsEvent = (RaceLogFinishPositioningConfirmedEvent) revokedEvent;
                        reconcileCompetitorsWithResults(revokedResultsEvent);
                    } else if (revokedEvent instanceof RaceLogFlagEvent || revokedEvent instanceof RaceLogPassChangeEvent) {
                        reconcileRaceStatus(tractracRace, trackedRace);
                    }
                }
            }
        }

        private void reconcileCompetitorsWithResults(final RaceLogFinishPositioningConfirmedEvent resultsEvent) {
            for (final CompetitorResult competitorResult : resultsEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons()) {
                final Serializable competitorId = competitorResult.getCompetitorId();
                if (competitorId instanceof UUID) {
                    reconcileCompetitorStatus(tractracRace.getRaceCompetitor((UUID) competitorId), trackedRace);
                }
            }
        }
    }
    
    public RaceAndCompetitorStatusWithRaceLogReconciler(DomainFactory domainFactory, RaceLogResolver raceLogResolver, IRace tractracRace) {
        super();
        this.raceLogEventsAddedToRaceLogByMyself = new ConcurrentLinkedQueue<>();
        this.domainFactory = domainFactory;
        this.raceLogResolver = raceLogResolver;
        this.tractracRace = tractracRace;
        raceLogListeners = Collections.synchronizedMap(new HashMap<>());
        raceLogEventAuthor = new LogEventAuthorImpl(getClass().getName(), 1); // equally important as race officer on water
    }
    
    public void raceLogAttached(TrackedRace trackedRace, RaceLog raceLog) {
        final RaceLogListener listener = new RaceLogListener(trackedRace, raceLog);
        raceLog.addListener(listener);
        raceLogListeners.put(new Pair<>(trackedRace, raceLog), listener);
    }
    
    public void raceLogDetached(TrackedRace trackedRace, RaceLog raceLog) {
        final RaceLogListener listener = raceLogListeners.remove(new Pair<>(trackedRace, raceLog));
        if (listener != null) {
            raceLog.removeListener(listener);
        }
    }

    /**
     * The following race status types are currently available on the TracTrac side (see {@link RaceStatusType}):
     * <ul>
     * <li>NONE(0)</li>
     * <li>START(4)</li>
     * <li>RACING(5)</li>
     * <li>UNOFFICIAL(7)</li>
     * <li>ABANDONED(8)</li>
     * <li>OFFICIAL(9)</li>
     * <li>GENERAL_RECALL(10)</li>
     * <li>POSTPONED(11)</li>
     * </ul>
     * We are interested in status transitions that need to be reflected by an "N" ("November", abort), "AP" (answering
     * pennant, postponement), or 1st substitute (general recall) flag status in the race log. The TracTrac-provided
     * status transition has a time stamp on it (see {@link IRace#getStatusTime()}), and so would any aborting flag
     * event in a {@link RaceLog} as well as any pass change event. If the TracTrac status time is later than the last
     * race log-based aborting flag event from the current pass and the TracTrac status is none of {@code ABANDONED},
     * {@code GENERAL_RECALL} or {@code POSTPONED}, a new pass will be established in the race log. If the TracTrac
     * status is any of {@code ABANDONED}, {@code GENERAL_RECALL} or {@code POSTPONED}, and the race log has not the
     * matching aborting flag in the current pass, and the TracTrac status update time is later than the last race log
     * status, the corresponding race log event that represents {@code ABANDONED}, {@code GENERAL_RECALL} or
     * {@code POSTPONED}, respectively, will be appended to the {@link #getDefaultRaceLog(TrackedRace) default race
     * log}.
     * <p>
     * 
     * There is no API currently that allows us to determine the start mode flag. Manual intervention would be required
     * if a non-default start mode flag is to be shown.
     * <p>
     * 
     * If multiple race logs are attached, a "default" race log will be determined, e.g., based on the one that already
     * has the most events in it. See {@link #getDefaultRaceLog}.
     */
    public void reconcileRaceStatus(IRace tractracRace, TrackedRace trackedRace) {
        final RaceStatusType raceStatus = tractracRace.getStatus();
        final TimePoint raceStatusUpdateTime = TimePoint.of(tractracRace.getStatusLastChangedTime());
        RaceLogFlagEvent abortingFlagEvent = null;
        for (final RaceLog raceLog : trackedRace.getAttachedRaceLogs()) {
            if (abortingFlagEvent == null) {
                final ReadonlyRaceState raceState = ReadonlyRaceStateImpl.getOrCreate(trackedRace.getRaceLogResolver(), raceLog);
                final RaceLogRaceStatus status = raceState.getStatus();
                if (status.isAbortingFlagFromPreviousPassValid()) {
                    final AbortingFlagFinder abortingFlagFinder = new AbortingFlagFinder(raceLog);
                    abortingFlagEvent = abortingFlagFinder.analyze();
                }
            }
        }
        for (final RaceLog raceLog : trackedRace.getAttachedRaceLogs()) {
            final boolean resultsAreOfficial = ReadonlyRaceStateImpl.getOrCreate(raceLogResolver, raceLog).isResultsAreOfficial();
            if (raceStatus == RaceStatusType.OFFICIAL && !resultsAreOfficial) {
                logger.info("Race status for race "+trackedRace.getName()+" is OFFICIAL with TracTrac and we have it as not official so far. Scheduling update.");
                final Runnable setResultsAreOfficial = ()->{
                    logger.info("Setting race status for race "+trackedRace.getName()+" to OFFICIAL now");
                    RaceStateImpl.create(raceLogResolver, raceLog, raceLogEventAuthor).setResultsAreOfficial(raceStatusUpdateTime);
                };
                if (officialCompetitorUpdateProvider != null) {
                    logger.info("Enqueuing the setting of race status for race "+trackedRace.getName()+
                            " to OFFICIAL until all competitor updates have been handled");
                    officialCompetitorUpdateProvider.runWhenNoMoreOfficialCompetitorUpdatesPending(setResultsAreOfficial);
                } else {
                    setResultsAreOfficial.run();
                }
            }
            if (abortingFlagEvent != null && !isAbortedState(raceStatus) && raceStatusUpdateTime.after(abortingFlagEvent.getLogicalTimePoint())) {
                logger.info("RaceLog considered race "+trackedRace.getName()+" as aborted ("+abortingFlagEvent+"), and the TracTrac race status "+raceStatus+
                        " from "+raceStatusUpdateTime+" suggests it's not aborted. Starting a new pass.");
                startNewPass(raceStatusUpdateTime, raceLog);
            } else if (isAbortedState(raceStatus) &&
                    (abortingFlagEvent == null || (!abortingFlagMatches(raceStatus, abortingFlagEvent.getUpperFlag()) &&
                                                   raceStatusUpdateTime.after(abortingFlagEvent.getLogicalTimePoint())))) {
                final Flags upperFlag = flagForRaceStatus.get(raceStatus);
                logger.info("RaceLog considered race "+trackedRace.getName()+" as NOT aborted, and the TracTrac race status "+raceStatus+
                        " from "+raceStatusUpdateTime+" suggests it's aborted. Adding abort flag event "+upperFlag+", starting a new pass.");
                final RaceLogFlagEventImpl flagEvent = new RaceLogFlagEventImpl(raceStatusUpdateTime, raceLogEventAuthor,
                        raceLog.getCurrentPassId(), upperFlag, /* lower flag */ Flags.NONE,
                        /* is displayed */ true);
                addRaceLogEventAndPreventRecursion(raceLog, flagEvent);
                startNewPass(raceStatusUpdateTime, raceLog);
            }
        }
    }
    
    private void addRaceLogEventAndPreventRecursion(RaceLog raceLog, RaceLogEvent event) {
        raceLogEventsAddedToRaceLogByMyself.add(event);
        raceLog.add(event);
    }

    protected void startNewPass(final TimePoint timePointForStartOfNewPass, final RaceLog raceLog) {
        final RaceLogPassChangeEventImpl passChangeEvent = new RaceLogPassChangeEventImpl(timePointForStartOfNewPass,
                raceLogEventAuthor, raceLog.getCurrentPassId() + 1);
        addRaceLogEventAndPreventRecursion(raceLog, passChangeEvent);
    }

    private boolean abortingFlagMatches(RaceStatusType raceStatus, Flags upperFlag) {
        return flagForRaceStatus.get(raceStatus) == upperFlag;
    }

    private boolean isAbortedState(RaceStatusType raceStatus) {
        return raceStatus == RaceStatusType.ABANDONED || raceStatus == RaceStatusType.GENERAL_RECALL || raceStatus == RaceStatusType.POSTPONED;
    }
    
    /**
     * Maps TracTrac competitor status to the local domain model's penalty codes
     * 
     * @return {@code null} in case the TracTrac code cannot be mapped to any {@link MaxPointsReason} reasonably
     */
    private MaxPointsReason getMaxPointsReason(RaceCompetitorStatusType raceCompetitorStatusType) {
        MaxPointsReason result;
        if (raceCompetitorStatusType == null) {
            result = MaxPointsReason.NONE;
        } else {
            switch (raceCompetitorStatusType) {
            // TODO we expect NSC and perhaps also TLE to show up; support the mapping to MaxPointsReason.NSC / TLE, respectively when they appear
            case ABANDONED:
                result = MaxPointsReason.NONE; // TODO bug 5154: find out what ABANDONED means and if/how we can translate it to a MaxPointsReason
                break;
            case BFD:
                result = MaxPointsReason.BFD;
                break;
            case DISQUALIFIED:
                result = MaxPointsReason.DSQ;
                break;
            case DNC:
                result = MaxPointsReason.DNC;
                break;
            case DNF:
                result = MaxPointsReason.DNF;
                break;
            case DONT_RACE:
                result = MaxPointsReason.DNS; // TODO bug 5154: find out what DONT_RACE means and if/how we can translate it to a MaxPointsReason; is it DNS?
                break;
            case FIN:
                result = MaxPointsReason.NONE; // TODO bug 5154: find out what FIN means and if/how we can translate it to a MaxPointsReason; does it mean the competitor finished properly?
                break;
            case FINISH_CONFIRMED:
                result = MaxPointsReason.NONE; // TODO bug 5154: find out what FINISH_CONFIRMED means and if/how we can translate it to a MaxPointsReason; does it mean the competitor finished properly?
                break;
            case MIS:
                result = MaxPointsReason.NONE; // TODO bug 5154: find out what MIS means and if/how we can translate it to a MaxPointsReason; does it mean the competitor is "missing?"
                break;
            case NO_COLLECT:
                result = MaxPointsReason.NONE; // TODO bug 5154: find out what NO_COLLECT means and if/how we can translate it to a MaxPointsReason
                break;
            case NO_DATA:
                result = MaxPointsReason.NONE;
                break;
            case OCS:
                result = MaxPointsReason.OCS;
                break;
            case RACING:
                result = MaxPointsReason.NONE;
                break;
            case RETIRED:
                result = MaxPointsReason.RET;
                break;
            case UFD:
                result = MaxPointsReason.UFD;
                break;
            case NSC:
                result = MaxPointsReason.NSC;
                break;
            case TLE:
                result = MaxPointsReason.TLE;
                break;
            case SCP:
                result = MaxPointsReason.SCP;
                break;
            case STP:
                result = MaxPointsReason.STP;
                break;
            case DCT:
                result = MaxPointsReason.DCT;
                break;
            case DNE:
                result = MaxPointsReason.DNE;
                break;
            case RCT:
                result = MaxPointsReason.RCT;
                break;
            default:
                result = MaxPointsReason.NONE;
                break;
            }
        }
        return result;
    }

    /**
     * If an official finish time or rank exists on the {@link IRaceCompetitor} but not in any of the {@link TrackedRace}'s
     * {@link RaceLog}-based results, or the corresponding TracTrac information is newer than the race log-based result,
     * a new race log entry of type {@link RaceLogFinishPositioningListConfirmedEvent} will be created that describes
     * the results as obtained from the {@link IRaceCompetitor}'s {@link IRaceCompetitor#getOfficialFinishTime()} and
     * {@link IRaceCompetitor#getOfficialRank()} methods. If the {@link IRaceCompetitor} has a status update time but
     * empty results and there is a {@link CompetitorResult} for that competitor coming from the race log, that competitor
     * result will be "invalidated" / "revoked" by adding a new {@link CompetitorResult} at the end of the race log that
     * has an empty finishing time and zero rank.<p>
     * 
     * See also <a href="https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5154">bug 5154</a> for a discussion
     * about the functionality expected, and see also {@code TestTracTracRaceAndCompetitorStatusReconciler} for the
     * corresponding tests expressing the expected results in the form of test cases.
     */
    public void reconcileCompetitorStatus(IRaceCompetitor raceCompetitor, TrackedRace trackedRace) {
        final int officialRank = raceCompetitor.getOfficialRank();
        final long officialFinishingTime = raceCompetitor.getOfficialFinishTime();
        final long timePointForStatusEvent = raceCompetitor.getStatusLastChangedTime();
        logger.info("Received a status change for competitor " + raceCompetitor + " in race "+trackedRace.getRaceIdentifier()+
                ": officialRank: " + officialRank+
                ", officialFinishingTime: "+officialFinishingTime+
                ", competitorStatus: "+raceCompetitor.getStatus()+
                ", statusTime: " + timePointForStatusEvent);
        if (timePointForStatusEvent != 0) {
            // there is an official result for the competitor on TracTrac's side
            // find out if we already have this information represented in the race log(s) and if not if the TracTrac information is newer:
            final Competitor competitor = domainFactory.resolveCompetitor(raceCompetitor.getCompetitor());
            if (competitor == null) {
                logger.warning("Received a competitor status update from TracTrac for a competitor in race "+raceCompetitor.getRace()+
                        " we don't know: "+ raceCompetitor.getCompetitor()+"; ignoring.");
            } else {
                final RaceCompetitorStatusType competitorStatus = raceCompetitor.getStatus();
                final MaxPointsReason officialMaxPointsReason = getMaxPointsReason(competitorStatus);
                final MillisecondsTimePoint officialResultTime = new MillisecondsTimePoint(timePointForStatusEvent);
                for (final RaceLog raceLog : trackedRace.getAttachedRaceLogs()) {
                final Pair<CompetitorResult, TimePoint> resultFromRaceLogAndItsCreationTimePoint = getRaceLogResultAndCreationTimePointForCompetitor(raceLog, competitor);
                    if (resultFromRaceLogAndItsCreationTimePoint == null
                            || ((resultFromRaceLogAndItsCreationTimePoint.getA().getOneBasedRank() != officialRank
                             || ((resultFromRaceLogAndItsCreationTimePoint.getA().getFinishingTime() == null) ? 0
                                    : resultFromRaceLogAndItsCreationTimePoint.getA().getFinishingTime().asMillis()) != officialFinishingTime
                             || resultFromRaceLogAndItsCreationTimePoint.getA().getMaxPointsReason() != officialMaxPointsReason)
                            && resultFromRaceLogAndItsCreationTimePoint.getB().before(officialResultTime))) {
                        logger.info("Applying official competitor result because its time point "+officialResultTime
                                +" is newer than the latest result for that competitor from the race log "+
                                (resultFromRaceLogAndItsCreationTimePoint==null?"null":resultFromRaceLogAndItsCreationTimePoint.getB()));
                        // We have received an update from TracTrac, rank or finishing time or penalty varies and is newer
                        // than the last thing we see in the race log (including we may not have anything in the race
                        // log for the results of that competitor at all yet).
                        // --> Write the TracTrac result to the race log, where 0 for the "official rank" maps to 0 of our
                        // one-based rank; and 0 as official finishing time maps to null as the official finishing time:
                        final CompetitorResult resultForRaceLog = new CompetitorResultImpl(competitor.getId(), competitor.getName(),
                                competitor.getShortName(),
                                /* boat name: */ ((competitor.hasBoat() ? ((CompetitorWithBoat) competitor).getBoat().getName() : competitor.getShortInfo())),
                                /* boatSailId */ ((competitor.hasBoat() ? ((CompetitorWithBoat) competitor).getBoat().getSailID() : competitor.getShortInfo())),
                                officialRank, officialMaxPointsReason, /* score null means let the scoring system calculate it */ null,
                                officialFinishingTime == 0 ? null : new MillisecondsTimePoint(officialFinishingTime),
                                        "Official results from TracTrac connector", MergeState.OK);
                        final CompetitorResults resultsForRaceLog = new CompetitorResultsImpl();
                        resultsForRaceLog.add(resultForRaceLog);
                        final RaceLogFinishPositioningConfirmedEventImpl raceLogEvent = new RaceLogFinishPositioningConfirmedEventImpl(
                                officialResultTime, officialResultTime, raceLogEventAuthor,
                                UUID.randomUUID(), raceLog.getCurrentPassId(), resultsForRaceLog);
                        // Avoid an endless recursion in case this event triggers this reconciler's race log listener; see bug 5565
                        addRaceLogEventAndPreventRecursion(raceLog, raceLogEvent);
                        logger.info("Added the following result to the race log of " + trackedRace.getRaceIdentifier()
                                + " for competitor " + raceCompetitor + ": " + resultForRaceLog);
                    } else {
                        logger.info("Did not produce a new competitor result for competitor " + raceCompetitor + " in race "
                                + trackedRace.getRaceIdentifier() + " because existing result "
                                + resultFromRaceLogAndItsCreationTimePoint + " was not older than status time "
                                + officialResultTime + " or was an equal result");
                    }
                }
            }
        } else {
            logger.info("Ignoring status change for competitor " + raceCompetitor + " in race "+trackedRace.getRaceIdentifier()+" because the status time was 0");
        }
    }

    protected Pair<CompetitorResult, TimePoint> getRaceLogResultAndCreationTimePointForCompetitor(RaceLog raceLog,
            final Competitor competitor) {
        Pair<CompetitorResult, TimePoint> resultFromRaceLogAndItsCreationTimePoint = null;
        final ReadonlyRaceState raceState = ReadonlyRaceStateImpl.getOrCreate(raceLogResolver, raceLog);
        final CompetitorResultsAndTheirCreationTimePoints results = raceState.getConfirmedFinishPositioningList();
        if (results.getCompetitorResults() != null) {
            final Optional<CompetitorResult> result = StreamSupport.stream(results.getCompetitorResults().spliterator(), /* parallel */ false)
                    .filter(r -> Util.equalsWithNull(competitor.getId(), r.getCompetitorId())).findAny();
            if (result.isPresent()) {
                resultFromRaceLogAndItsCreationTimePoint = new Pair<>(result.get(), results.getCreationTimePointOfResultForCompetitorWithId(result.get().getCompetitorId()));
            }
        }
        return resultFromRaceLogAndItsCreationTimePoint;
    }

    /**
     * To be called by the object later calling {@link #reconcileCompetitorStatus(IRaceCompetitor, TrackedRace)}. This
     * way, this object can tell when the race is to be {@link RaceState#setResultsAreOfficial(TimePoint) moved to
     * OFFICIAL state} whether any updates are pending and defer the race state transition to that moment.
     * 
     * @see OfficialCompetitorUpdateProvider#runWhenNoMoreOfficialCompetitorUpdatesPending(Runnable)
     */
    public void setOfficialCompetitorUpdateProvider(OfficialCompetitorUpdateProvider officialCompetitorUpdateProvider) {
        this.officialCompetitorUpdateProvider = officialCompetitorUpdateProvider;
    }
}
