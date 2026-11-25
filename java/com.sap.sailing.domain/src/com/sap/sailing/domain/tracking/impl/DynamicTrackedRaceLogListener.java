package com.sap.sailing.domain.tracking.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogCourseDesignChangedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogDependentStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEndOfTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogExcludeWindSourcesEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFixedMarkPassingEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogPassChangeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogRevokeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartOfTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogSuppressedMarkPassingsEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogWindFixEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.AbortingFlagFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.ExcludedWindSourcesFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.LastPublishedCourseDesignFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.MarkPassingDataFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.WindFixesFinder;
import com.sap.sailing.domain.abstractlog.race.impl.BaseRaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.BaseRaceStateChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.impl.ReadonlyRaceStateImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingUpdateListener;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.RaceLogWindFixDeclinationHelper;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Triple;

/**
 * Listens for changes on a {@link RaceLog} and forwards the relevant ones to a {@link TrackedRace}. Examples: start time changes;
 * fixed mark passings changes; course design changes; wind fixes entered by the race committee.<p>
 * 
 * For each {@link RaceLog} {@link #beforeAttaching(RaceLog) attached}, a {@link ReadonlyRaceState} is constructed that is used to observe
 * some properties in an easier to use form than observing the {@link RaceLog}s directly. For example, the {@link ReadonlyRaceState}
 * can tell the {@link ReadonlyRaceState#getFinishedTime() finished time} and it is possible to receive change notifications for
 * this value.
 */
public class DynamicTrackedRaceLogListener extends BaseRaceLogEventVisitor {

    private static final Logger logger = Logger.getLogger(DynamicTrackedRaceLogListener.class.getName());

    /**
     * Race logs as keys, {@link ReadonlyRaceState} objects wrapping their respective key race log as values
     */
    private ConcurrentMap<RaceLog, ReadonlyRaceState> raceLogs = new ConcurrentHashMap<>();

    private DynamicTrackedRace trackedRace;

    private final WindSource raceCommitteeWindSource;

    private MarkPassingDataFinder markPassingDataFinder;
    
    private MarkPassingUpdateListener markPassingUpdateListener;

    public DynamicTrackedRaceLogListener(DynamicTrackedRace trackedRace) {
        this.trackedRace = trackedRace;
        raceCommitteeWindSource = new WindSourceImpl(WindSourceType.RACECOMMITTEE);
    }

    public void beforeAttaching(RaceLog raceLog) {
        if (raceLog == null) {
            logger.severe("Trying to add " + this + " as listener to a null race log for tracked race " + trackedRace.getRace());
        } else {
            raceLog.addListener(this);
            final ReadonlyRaceState raceState = ReadonlyRaceStateImpl.getOrCreate(trackedRace.getRaceLogResolver(), raceLog);
            raceLogs.put(raceLog, raceState);
            raceState.addChangedListener(new BaseRaceStateChangedListener() {
                @Override
                public void onFinishedTimeChanged(ReadonlyRaceState state) {
                    trackedRace.setFinishedTime(state.getFinishedTime());
                }
                @Override
                public void onFinishingTimeChanged(ReadonlyRaceState state) {
                    trackedRace.setFinishingTime(state.getFinishingTime());
                }
            });
        }
    }

    public void afterAttaching(RaceLog raceLog) {
        trackedRace.invalidateStartTime();
        trackedRace.invalidateEndTime();
        analyze(raceLog);
    }

    /**
     * @return the first non-{@code null} {@link ReadonlyRaceState#getFinishingTime() finishing time} that is found in any
     *         of the {@link RaceState}s constructed for all the {@link RaceLog}s observed by this listener, or {@code null}
     *         if no {@link ReadonlyRaceState} exists that returns a non-{@code null} finishing time.
     */
    public TimePoint getFinishingTime() {
        TimePoint result = null;
        for (final ReadonlyRaceState raceState : raceLogs.values()) {
            result = raceState.getFinishingTime();
            if (result != null) {
                break;
            }
        }
        return result;
    }

    /**
     * @return the first non-{@code null} {@link ReadonlyRaceState#getFinishedTime() finished time} that is found in any
     *         of the {@link RaceState}s constructed for all the {@link RaceLog}s observed by this listener, or {@code null}
     *         if no {@link ReadonlyRaceState} exists that returns a non-{@code null} finished time.
     */
    public TimePoint getFinishedTime() {
        TimePoint result = null;
        for (final ReadonlyRaceState raceState : raceLogs.values()) {
            result = raceState.getFinishedTime();
            if (result != null) {
                break;
            }
        }
        return result;
    }

    private LastPublishedCourseDesignFinder createCourseDesignFinder(RaceLog raceLog) {
        return new LastPublishedCourseDesignFinder(raceLog, /* onlyCoursesWithValidWaypointList */ false); // we also want by-name courses to forward, e.g., to TracTrac
    }

    private AbortingFlagFinder createAbortingFlagFinder(RaceLog raceLog) {
        return new AbortingFlagFinder(raceLog);
    }

    private StartTimeFinder createStartTimeFinder(RaceLog raceLog) {
        return new StartTimeFinder(trackedRace.getRaceLogResolver(), raceLog);
    }

    public void setMarkPassingUpdateListener(MarkPassingUpdateListener listener) {
        markPassingUpdateListener = listener;
    }

    /**
     * Retrieves all wind fixes available in the race log and adds them to the wind track for RACECOMMITTEEE
     * 
     * @param raceLog
     *            The race log from which the available wind fixes shall be retrieved.
     */
    private void initializeWindTrack(RaceLog raceLog) {
        WindFixesFinder windFixesFinder = new WindFixesFinder(raceLog);
        for (RaceLogWindFixEvent raceLogWindFixEvent : windFixesFinder.analyze()) {
            Wind wind = new RaceLogWindFixDeclinationHelper().getOptionallyDeclinationCorrectedWind(raceLogWindFixEvent);
            trackedRace.recordWind(wind, raceCommitteeWindSource, /* applyFilter */ false);
        }
    }

    public void beforeDetaching(RaceLog raceLog) {
        // Gets called whenever a RaceColumn was already linked to a TrackedRace on linkage.
        // Maybe we need to reset the status of the old race somehow? There might be no new
        // TrackedRace to be linked...
        // Maybe something like this is needed:
        // TODO:
        // ??? trackedRace.setStatus(new TrackedRaceStatusImpl(TrackedRaceStatusEnum.PREPARED, 0.0));
        if (raceLog != null) {
            raceLog.removeListener(this);
            raceLogs.remove(raceLog);
            if (markPassingUpdateListener != null) {
                removeMarkPassingEvents();
            }
        }
    }


    public void afterDetaching(RaceLog raceLog) {
        if (raceLog != null) {
            trackedRace.invalidateStartTime();
            removeAllWindFixesFromWindTrack(raceLog);
            trackedRace.updateMarkPassingsAfterRaceLogChanges();
        }
    }

    /**
     * Removes all wind fixes that are in the RaceLog from the RACECOMMITTEE wind source
     * 
     * @param raceLog
     *            the RaceLog of which the wind fixes shall be removed
     */
    private void removeAllWindFixesFromWindTrack(RaceLog raceLog) {
        WindFixesFinder windFixesFinder = new WindFixesFinder(raceLog);
        for (RaceLogWindFixEvent raceLogWindFixEvent : windFixesFinder.analyze()) {
            Wind wind = new RaceLogWindFixDeclinationHelper().getOptionallyDeclinationCorrectedWind(raceLogWindFixEvent);
            trackedRace.removeWind(wind, raceCommitteeWindSource);
        }
    }

    private void removeMarkPassingEvents() {
        for (Triple<Competitor, Integer, TimePoint> triple : markPassingDataFinder.analyze()) {
            if (triple.getC() == null) {
                markPassingUpdateListener.removeSuppressedPassing(triple.getA());
            } else {
                markPassingUpdateListener.removeFixedPassing(triple.getA(), triple.getB());
            }
        }
    }

    private void analyze(RaceLog raceLog) {
        trackedRace.setFinishingTime(getFinishingTime());
        trackedRace.setFinishedTime(getFinishedTime());
        analyzeCourseDesign(null);
        initializeWindTrack(raceLog);
        updateWindSourcesToExclude();
        if (markPassingUpdateListener != null) {
            markPassingDataFinder = new MarkPassingDataFinder(raceLog);
            analyzeMarkPassings();
        }
        trackedRace.updateMarkPassingsAfterRaceLogChanges();
    }

    private void analyzeCourseDesign(CourseBase courseBaseProvidedByEvent) {
        CourseBase courseDesign = null;
        for (RaceLog raceLog : raceLogs.keySet()) {
            courseDesign = createCourseDesignFinder(raceLog).analyze();
            if (courseDesign != null) {
                break;
            }
        }
        if (courseDesign == null) {
            courseDesign = courseBaseProvidedByEvent;
        }
        // On the initial analyze step after attaching the RaceLog there might be no course design.
        if (courseDesign != null) {
            // Because this code can be triggered by an obsolete (delayed) event...
            // ... onCourseDesignChangedByRaceCommittee() might be called more than once.
            trackedRace.onCourseDesignChangedByRaceCommittee(courseDesign);
        } else {
            logger.info("Could not find any course design update on race log of " + trackedRace.getRace().getName()
                    + "! Not sending out any events.");
        }
    }

    private void analyzeStartTime(TimePoint startTimeProvidedByEvent) {
        /* start time will be set by StartTimeFinder in TrackedRace.getStartOfRace() */
        trackedRace.invalidateStartTime();
        TimePoint startTime = null;
        for (RaceLog raceLog : raceLogs.keySet()) {
            startTime = createStartTimeFinder(raceLog).analyze().getStartTime();
            if (startTime != null) {
                break;
            }
        }
        if (startTime == null) {
            startTime  = startTimeProvidedByEvent;
        }
        if (startTime != null) {
            /* invoke listeners with received start time, this will also trigger tractrac update */
            trackedRace.onStartTimeChangedByRaceCommittee(startTime);
        } else {
            logger.info("Could not find any valid start time on race log of " + trackedRace.getRace().getName()
                    + "! Not sending out any events.");
        }
    }

    private void analyzeMarkPassings() {
        for (Triple<Competitor, Integer, TimePoint> triple : markPassingDataFinder.analyze()) {
            if (triple.getC() == null) {
                markPassingUpdateListener.addSuppressedPassing(triple.getA(), triple.getB());
            } else {
                markPassingUpdateListener.addFixedPassing(triple.getA(), triple.getB(), triple.getC());
            }
        }
    }

    @Override
    public void visit(RaceLogPassChangeEvent event) {
        trackedRace.invalidateStartTime(); // this will notify RaceStateListeners in case the start time changes by the event
        /* reset start time */
        trackedRace.onStartTimeChangedByRaceCommittee(null);
        RaceLogFlagEvent abortingFlag = null;
        for (RaceLog raceLog : raceLogs.keySet()) {
            abortingFlag = createAbortingFlagFinder(raceLog).analyze();
            if (abortingFlag != null) {
                break;
            }
        }
        if (abortingFlag != null) {
            // previous pass was aborted; notify TracTrac
            trackedRace.onAbortedByRaceCommittee(abortingFlag.getUpperFlag());
        }
        trackedRace.updateMarkPassingsAfterRaceLogChanges();
    }

    @Override
    public void visit(RaceLogStartTimeEvent event) {
        analyzeStartTime(event.getStartTime());
    }

    @Override
    public void visit(RaceLogCourseDesignChangedEvent event) {
        analyzeCourseDesign(event.getCourseDesign());
    }

    /**
     * We don't know whether this event is the one that is now valid; it could have a priority too low, or it could have
     * a time point too early and thus superseded by a later event of the same type. There may also be other race logs.
     * Therefore, re-evaluate the current situation "from scratch."
     */
    @Override
    public void visit(RaceLogFinishPositioningConfirmedEvent event) {
        trackedRace.updateMarkPassingsAfterRaceLogChanges();
    }

    @Override
    public void visit(RaceLogWindFixEvent event) {
        Wind wind = new RaceLogWindFixDeclinationHelper().getOptionallyDeclinationCorrectedWind(event);
        // add the wind fix to the race committee WindTrack
        trackedRace.recordWind(wind, raceCommitteeWindSource);
    }

    @Override
    public void visit(RaceLogFixedMarkPassingEvent event) {
        if (markPassingUpdateListener != null) {
            markPassingUpdateListener.addFixedPassing(event.getInvolvedCompetitors().get(0), event.getZeroBasedIndexOfPassedWaypoint(),
                    event.getTimePointOfFixedPassing());
        }
    }

    @Override
    public void visit(RaceLogSuppressedMarkPassingsEvent event) {
        if (markPassingUpdateListener != null) {
            markPassingUpdateListener.addSuppressedPassing(event.getInvolvedCompetitors().get(0),
                    event.getZeroBasedIndexOfFirstSuppressedWaypoint());
        }
    }

    @Override
    public void visit(final RaceLogRevokeEvent event) {
        RaceLogEvent revokedEvent = null;
        for (final RaceLog log : raceLogs.keySet()) {
            try {
                log.lockForRead();
                revokedEvent = log.getEventById(event.getRevokedEventId());
                if (revokedEvent != null) {
                    break;
                }
            } finally {
                log.unlockAfterRead();
            }
        }
        if (revokedEvent instanceof RaceLogStartOfTrackingEvent || revokedEvent instanceof RaceLogEndOfTrackingEvent) {
            trackedRace.updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
        } else if (markPassingUpdateListener != null) {
            if (revokedEvent instanceof RaceLogSuppressedMarkPassingsEvent) {
                markPassingUpdateListener.removeSuppressedPassing(revokedEvent.getInvolvedCompetitors().get(0));
            } else if (revokedEvent instanceof RaceLogFixedMarkPassingEvent) {
                markPassingUpdateListener.removeFixedPassing(revokedEvent.getInvolvedCompetitors().get(0),
                        ((RaceLogFixedMarkPassingEvent) revokedEvent).getZeroBasedIndexOfPassedWaypoint());
            }
        }
    }

    @Override
    public void visit(RaceLogDependentStartTimeEvent event) {
        analyzeStartTime(null);
    }
    
    @Override
    public void visit(RaceLogStartOfTrackingEvent event) {
        trackedRace.updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
    }
    
    @Override
    public void visit(RaceLogEndOfTrackingEvent event) {
        trackedRace.updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
    }

    @Override
    public void visit(RaceLogRaceStatusEvent event) {
        if (event.getNextStatus().equals(RaceLogRaceStatus.FINISHED)){
            trackedRace.invalidateEndTime();
        }
    }

    @Override
    public void visit(RaceLogExcludeWindSourcesEvent event) {
        updateWindSourcesToExclude();
    }

    private void updateWindSourcesToExclude() {
        for (RaceLog raceLog : raceLogs.keySet()) {
            final Iterable<WindSource> windSourcesToExclude = new ExcludedWindSourcesFinder(raceLog).analyze();
            if (windSourcesToExclude != null) {
                trackedRace.setWindSourcesToExclude(windSourcesToExclude);
            }
        }
    }
}
