package com.sap.sailing.domain.tracking.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.ConfirmedFinishPositioningListFinder;
import com.sap.sailing.domain.abstractlog.race.state.RaceStateChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.BaseRaceStateChangedListener;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.SensorFix;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingCalculator;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.ranking.RankingMetricConstructor;
import com.sap.sailing.domain.shared.tracking.AddResult;
import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sailing.domain.tracking.CourseDesignChangedListener;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicSensorFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.GPSTrackListener;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceAbortedListener;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.SensorFixTrackListener;
import com.sap.sailing.domain.tracking.StartTimeChangedListener;
import com.sap.sailing.domain.tracking.TrackFactory;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.TrackingDataLoader;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;

public class DynamicTrackedRaceImpl extends TrackedRaceImpl implements
DynamicTrackedRace, GPSTrackListener<Competitor, GPSFixMoving> {
    private static final long serialVersionUID = 1092726918239676958L;

    private static final Logger logger = Logger.getLogger(DynamicTrackedRaceImpl.class.getName());

    private transient Set<RaceChangeListener> listeners;

    private boolean raceIsKnownToStartUpwind;

    private boolean delayToLiveInMillisFixed;
    
    private transient DynamicTrackedRaceLogListener logListener;

    private transient Set<CourseDesignChangedListener> courseDesignChangedListeners;
    private transient Set<StartTimeChangedListener> startTimeChangedListeners;
    private transient Set<RaceAbortedListener> raceAbortedListeners;
    private transient Map<TrackingDataLoader, TrackedRaceStatus> loaderStatus = new HashMap<>();

    /**
     * Caches the result from {@link #getResultsFromRaceLogs}. This cache is updated in
     * {@link #updateMarkPassingsAfterRaceLogChanges()} which must be called by
     * {@link DynamicTrackedRaceLogListener} when the race log situation may have changed.
     */
    private Map<Competitor, CompetitorResult> competitorResultsFromRaceLog;

    /**
     * When one or more race logs are attached, changes in those race logs, or in case of dependent start
     * times even changes in other race logs, may impact this race's start time. This listener receives those start
     * time changes, also those from races that this race's start time depends upon, and triggers an update of
     * this race's start time caches. The field is transient, therefore needs no serialization support but needs
     * to be restored during at the time of de-serialization.
     */
    private transient RaceStateChangedListener raceStateBasedStartTimeChangedListener;
    
    /**
     * Flag that indicates if a {@link GPSFix} is already tracked in any {@link GPSFixTrack} for a competitor of this
     * race.
     */
    private final AtomicBoolean gpsFixReceived;

    public DynamicTrackedRaceImpl(TrackedRegatta trackedRegatta, RaceDefinition race, Iterable<Sideline> sidelines,
            WindStore windStore, long delayToLiveInMillis, long millisecondsOverWhichToAverageWind,
            long millisecondsOverWhichToAverageSpeed, long delayForCacheInvalidationOfWindEstimation,
            boolean useInternalMarkPassingAlgorithm, RankingMetricConstructor rankingMetricConstructor,
            RaceLogAndTrackedRaceResolver raceLogResolver, TrackingConnectorInfo trackingConnectorInfo,
            MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry) {
        super(trackedRegatta, race, sidelines, windStore, delayToLiveInMillis, millisecondsOverWhichToAverageWind,
                millisecondsOverWhichToAverageSpeed, delayForCacheInvalidationOfWindEstimation,
                useInternalMarkPassingAlgorithm, rankingMetricConstructor, raceLogResolver, trackingConnectorInfo,
                markPassingRaceFingerprintRegistry);
        raceStateBasedStartTimeChangedListener = createRaceStateStartTimeChangeListener();
        this.competitorResultsFromRaceLog = new HashMap<>();
        this.logListener = new DynamicTrackedRaceLogListener(this);
        if (markPassingCalculator != null) {
            logListener.setMarkPassingUpdateListener(markPassingCalculator.getListener());
        }
        this.courseDesignChangedListeners = new HashSet<>();
        this.startTimeChangedListeners = new HashSet<>();
        this.raceAbortedListeners = new HashSet<>();
        gpsFixReceived = new AtomicBoolean(false);
        this.raceIsKnownToStartUpwind = race.getBoatClass().typicallyStartsUpwind();
        if (!raceIsKnownToStartUpwind) {
            setWindSourcesToExclude(getWindSourcesToExclude()); // implicitly adds COURSE_BASED to the wind sources to exclude
        }
        for (Competitor competitor : getRace().getCompetitors()) {
            DynamicGPSFixTrack<Competitor, GPSFixMoving> track = getTrack(competitor);
            track.addListener(this);
        }
        // default wind tracks are observed because they are created by the superclass constructor using
        // createWindTrack which adds this object as a listener
    }
    
    @Override
    public boolean hasGPSData() {
        return gpsFixReceived.get();
    }

    /**
     * After de-serialization sets a valid {@link #listeners} collection which is transient and therefore
     * hasn't been serialized.
     */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        raceStateBasedStartTimeChangedListener = createRaceStateStartTimeChangeListener();
        // the race states are transient, therefore not serialized; they are re-constructed here when asked for;
        // the transient raceStateBasedStartTimeChangedListener is re-added here, like it was in attachRaceLog(...)
        for (final RaceLog raceLog : attachedRaceLogs.values()) {
            getRaceState(raceLog).addChangedListener(raceStateBasedStartTimeChangedListener);
        }
        listeners = getListeners(); // force creation only if default read object or superclass constructor didn't create it yet
        logListener = new DynamicTrackedRaceLogListener(this);
        courseDesignChangedListeners = new HashSet<>();
        startTimeChangedListeners = new HashSet<>();
        raceAbortedListeners = new HashSet<>();
    }
    
    protected Object readResolve() {
        if (markPassingCalculator != null) {
            logListener.setMarkPassingUpdateListener(markPassingCalculator.getListener());
        }
        return this;
    }

    private RaceStateChangedListener createRaceStateStartTimeChangeListener() {
        return new BaseRaceStateChangedListener() {
            @Override
            public void onStartTimeChanged(ReadonlyRaceState state) {
                final TimePoint oldStartTime = getStartOfRace();
                invalidateStartTime();
                final TimePoint startTimeFromRaceLog = state.getStartTime();
                if (!Util.equalsWithNull(oldStartTime, startTimeFromRaceLog)) {
                    onStartTimeChangedByRaceCommittee(startTimeFromRaceLog);
                }
            }
        };
    }

    /**
     * {@link #raceIsKnownToStartUpwind} (see also {@link #raceIsKnownToStartUpwind()}) is initialized based on the <code>race</code>'s
     * {@link RaceDefinition#getBoatClass()} boat class's {@link BoatClass#typicallyStartsUpwind()} result. It can be changed
     * using {@link #setRaceIsKnownToStartUpwind(boolean)}. Uses <code>millisecondsOverWhichToAverageWind/2</code> for the
     * <code>delayForCacheInvalidationOfWindEstimation</code> argument of the constructor.<p>
     * 
     * Loading wind tracks from the <code>windStore</code> happens asynchronously which means that when the constructor returns,
     * the caller cannot assume that all wind tracks have yet been loaded completely. The caller may call {@link #waitUntilLoadingFromWindStoreComplete()}
     * to wait until all persistent wind sources have been successfully and completely loaded.
     */
    public DynamicTrackedRaceImpl(TrackedRegatta trackedRegatta, RaceDefinition race, Iterable<Sideline> sidelines,
            WindStore windStore, long delayToLiveInMillis, long millisecondsOverWhichToAverageWind,
            long millisecondsOverWhichToAverageSpeed, boolean useInternalMarkPassingAlgorithm,
            RankingMetricConstructor rankingMetricConstructor, RaceLogAndTrackedRaceResolver raceLogResolver, TrackingConnectorInfo trackingConnectorInfo,
            MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry) {
        this(trackedRegatta, race, sidelines, windStore, delayToLiveInMillis,
                millisecondsOverWhichToAverageWind, millisecondsOverWhichToAverageSpeed,
                millisecondsOverWhichToAverageWind / 2, useInternalMarkPassingAlgorithm, rankingMetricConstructor, raceLogResolver, trackingConnectorInfo,
                markPassingRaceFingerprintRegistry);
    }

    @Override
    public boolean recordFix(Competitor competitor, GPSFixMoving fix, boolean onlyWhenInTrackingTimesInterval) {
        final boolean result;
        if (!onlyWhenInTrackingTimesInterval || isWithinStartAndEndOfTracking(fix.getTimePoint())) {
            DynamicGPSFixTrack<Competitor, GPSFixMoving> track = getTrack(competitor);
            if (track != null) {
                if (logger != null && logger.getLevel() != null && logger.getLevel().equals(Level.FINEST)) {
                    logger.finest(""+competitor.getName() + ": " + fix);
                }
                result = track.addGPSFix(fix); // the track notifies this tracked race which in turn notifies its listeners
            } else {
                result = false;
            }
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public void setStatus(TrackedRaceStatus newStatus) {
        TrackedRaceStatus oldStatus = getStatus();
        super.setStatus(newStatus);
        notifyListeners(newStatus, oldStatus);
    }
    
    @Override
    public void onStatusChanged(TrackingDataLoader source, TrackedRaceStatus newStatus) {
        TrackedRaceStatusEnum raceStatus;
        double totalProgress = 1.0;
        if (newStatus.getStatus() == TrackedRaceStatusEnum.REMOVED) {
            synchronized (loaderStatus) {
                this.updateLoaderStatus(source, newStatus);
            }
            raceStatus = newStatus.getStatus();
        } else {
            raceStatus = TrackedRaceStatusEnum.FINISHED;
            synchronized (loaderStatus) {
                this.updateLoaderStatus(source, newStatus);
                if (!loaderStatus.isEmpty()) {
                    double sumOfLoaderProgresses = 0.0;
                    boolean anyError = false;
                    boolean anyLoading = false;
                    boolean allPrepared = true;
                    raceStatus = TrackedRaceStatusEnum.TRACKING;
                    for (TrackedRaceStatus status : loaderStatus.values()) {
                        anyError |= (status.getStatus() == TrackedRaceStatusEnum.ERROR);
                        anyLoading |= (status.getStatus() == TrackedRaceStatusEnum.LOADING);
                        allPrepared &= (status.getStatus() == TrackedRaceStatusEnum.PREPARED);
                        sumOfLoaderProgresses += status.getLoadingProgress();
                    }
                    if (anyError) {
                        raceStatus = TrackedRaceStatusEnum.ERROR;
                    } else if (anyLoading) {
                        raceStatus = TrackedRaceStatusEnum.LOADING;
                        totalProgress = sumOfLoaderProgresses / loaderStatus.size();
                    } else {
                        raceStatus = allPrepared ? TrackedRaceStatusEnum.PREPARED : TrackedRaceStatusEnum.TRACKING;
                    }
                }
            }
        }
        this.setStatus(new TrackedRaceStatusImpl(raceStatus, totalProgress));
    }
   
    private void updateLoaderStatus(TrackingDataLoader loader, TrackedRaceStatus status) {
        if (status.getStatus() == TrackedRaceStatusEnum.FINISHED || status.getStatus() == TrackedRaceStatusEnum.REMOVED) {
            loaderStatus.remove(loader);
        } else {
            loaderStatus.put(loader, status);
        }
    }
    
    @Override
    public void waitForLoadingToFinish() throws InterruptedException {
        synchronized (getStatusNotifier()) {
            while (getStatus().getStatus() == TrackedRaceStatusEnum.PREPARED || getStatus().getStatus() == TrackedRaceStatusEnum.LOADING) {
                try {
                    getStatusNotifier().wait();
                } catch (InterruptedException e) {
                    logger.info("waitUntilNotLoading on tracked race " + this + " interrupted: " + e.getMessage()
                            + ". Continuing to wait.");
                }
            }
        }
    }

    @Override
    public void recordFix(Mark mark, GPSFix fix, boolean onlyWhenInTrackingTimeInterval) {
        final TimePoint fixTimePoint = fix.getTimePoint();
        if (!onlyWhenInTrackingTimeInterval || isWithinStartAndEndOfTracking(fixTimePoint)) {
            getOrCreateTrack(mark).addGPSFix(fix);
        } else {
            logger.finer(() -> "Dropped fix " + fix + " because it is outside the tracking interval "
                    + getStartOfTracking() + ".." + getEndOfTracking());
        }
    }

    /**
     * A time point is considered "in" if a non-{@code null} {@link #getStartOfTracking()} has been defined and the
     * {@code fixTimePoint} is at or after that time point and {@link #getEndOfTracking()}.
     * A <code>null</code> value for {@link #getEndOfTracking} means an open-ended interval.
     */
    @Override
    public boolean isWithinStartAndEndOfTracking(final TimePoint fixTimePoint) {
        return getStartOfTracking() != null && getStartOfTracking().compareTo(fixTimePoint) <= 0 &&
            (getEndOfTracking() == null || getEndOfTracking().compareTo(fixTimePoint) >= 0);
    }

    @Override
    public void setMillisecondsOverWhichToAverageSpeed(long millisecondsOverWhichToAverageSpeed) {
        this.millisecondsOverWhichToAverageSpeed = millisecondsOverWhichToAverageSpeed; 
        for (Competitor competitor : getRace().getCompetitors()) {
            getTrack(competitor).setMillisecondsOverWhichToAverage(millisecondsOverWhichToAverageSpeed);
        }
        for (Waypoint waypoint : getRace().getCourse().getWaypoints()) {
            for (Mark mark : waypoint.getMarks()) {
                getOrCreateTrack(mark).setMillisecondsOverWhichToAverage(millisecondsOverWhichToAverageSpeed);
            }
        }
        updated(/* time point */null);
        triggerManeuverCacheRecalculationForAllCompetitors();
    }

    @Override
    public void setMillisecondsOverWhichToAverageWind(long millisecondsOverWhichToAverageWind) {
        long oldMillisecondsOverWhichToAverageWind = this.millisecondsOverWhichToAverageWind;
        this.millisecondsOverWhichToAverageWind = millisecondsOverWhichToAverageWind;
        for (WindSource windSource : getWindSources()) {
            getOrCreateWindTrack(windSource).setMillisecondsOverWhichToAverage(millisecondsOverWhichToAverageWind);
        }
        updated(/* time point */null);
        triggerManeuverCacheRecalculationForAllCompetitors();
        notifyListenersWindAveragingChanged(oldMillisecondsOverWhichToAverageWind, millisecondsOverWhichToAverageWind);
    }

    @Override
    public void setAndFixDelayToLiveInMillis(long delayToLiveInMillis) {
        if (getDelayToLiveInMillis() != delayToLiveInMillis) {
            super.setDelayToLiveInMillis(delayToLiveInMillis);
            notifyListenersDelayToLiveChanged(delayToLiveInMillis);
        }
        delayToLiveInMillisFixed = true;
    }

    @Override
    public void setDelayToLiveInMillis(long delayToLiveInMillis) {
        if (!delayToLiveInMillisFixed) {
            if (getDelayToLiveInMillis() != delayToLiveInMillis) {
                super.setDelayToLiveInMillis(delayToLiveInMillis);
                notifyListenersDelayToLiveChanged(delayToLiveInMillis);
            }
        } else {
            logger.info("Not setting live delay for race "+getRace().getName()+" to "+delayToLiveInMillis+"ms because delay has been fixed");
        }
    }

    @Override
    public DynamicGPSFixTrack<Competitor, GPSFixMoving> getTrack(Competitor competitor) {
        return (DynamicGPSFixTrack<Competitor, GPSFixMoving>) super.getTrack(competitor);
    }

    @Override
    public DynamicGPSFixTrack<Mark, GPSFix> getOrCreateTrack(Mark mark) {
        return (DynamicGPSFixTrack<Mark, GPSFix>) super.getOrCreateTrack(mark);
    }

    /**
     * In addition to creating the track which is performed by the superclass implementation, this implementation
     * registers a {@link GPSTrackListener} with the mark's track and {@link #notifyListeners(GPSFix, Mark, boolean, AddResult)
     * notifies the listeners} about updates. In previous versions the {@link #updated(TimePoint)} method was
     * <em>not</em> called with the mark fix's time point because mark fixes could have been received also from marks
     * that don't belong to this race. However, we don't support any connector anymore that works this way. Therefore,
     * it is now considered safe to call {@link #updated(TimePoint)} for the mark fix's time point.
     */
    @Override
    protected DynamicGPSFixTrackImpl<Mark> createMarkTrack(Mark mark) {
        final DynamicGPSFixTrackImpl<Mark> result = super.createMarkTrack(mark);
        result.addListener(new GPSTrackListener<Mark, GPSFix>() {
            private static final long serialVersionUID = -2855787105725103732L;

            @Override
            public void gpsFixReceived(GPSFix fix, Mark mark, boolean firstFixInTrack, AddResult addedOrReplaced) {
                final TimePoint fixTimePoint = fix.getTimePoint();
                updated(fixTimePoint);
                final GPSFix lastFixBefore = result.getLastFixBefore(fixTimePoint);
                final GPSFix firstFixAfter = result.getFirstFixAfter(fixTimePoint);
                invalidateDistancesFromStarboardSideOfStartLineProjectedOntoLineCache(TimeRange.create(lastFixBefore==null?null:lastFixBefore.getTimePoint(),
                        firstFixAfter==null?null:firstFixAfter.getTimePoint()));
                triggerManeuverCacheRecalculationForAllCompetitors();
                notifyListeners(fix, mark, firstFixInTrack, addedOrReplaced);
            }

            @Override
            public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage,
                    long newMillisecondsOverWhichToAverage) {
                // nobody can currently listen for the change of the mark speed averaging because mark speed is not a value used
            }

            @Override
            public boolean isTransient() {
                return false;
            }
        });
        return result;
    }

    /**
     * Callers iterating over the result need to synchronize on the resulting collection while iterating
     * to avoid {@link ConcurrentModificationException}s.
     */
    @Override
    protected Set<RaceChangeListener> getListeners() {
        if (listeners == null) {
            listeners = new HashSet<RaceChangeListener>();
        }
        return listeners;
    }

    @Override
    public void addListener(RaceChangeListener listener) {
        synchronized (getListeners()) {
            getListeners().add(listener);
        }
    }
    
    @Override
    public void invalidateStartTime() {
        final TimePoint oldStartOfRace = getStartOfRace();
        super.invalidateStartTime();
        if (!Util.equalsWithNull(oldStartOfRace, getStartOfRace())) {
            notifyListenersStartOfRaceChanged(oldStartOfRace, getStartOfRace());
        }
    }

    @Override
    public void addListener(RaceChangeListener listener, final boolean notifyAboutWindFixesAlreadyLoaded,
            final boolean notifyAboutGPSFixesAlreadyLoaded) {
        if (notifyAboutWindFixesAlreadyLoaded) {
            LockUtil.lockForRead(getLoadingFromWindStoreLock());
        }
        if (notifyAboutGPSFixesAlreadyLoaded) {
            LockUtil.lockForRead(getLoadingFromGPSFixStoreLock());
        }
        try {
            addListener(listener);
            if (notifyAboutWindFixesAlreadyLoaded) {
                // Now notify all wind fixes we can get from the race by now. TrackedRace.getWindSource() delivers all wind
                // sources known so far. If there is a wind track being loaded, it will be separately notified later by
                // DynamicTrackedRaceImpl.createWindTrack(...).
                // Holding the serialization lock 
                for (WindSource windSource : getWindSources()) {
                    if (windSource.getType().canBeStored()) {
                        final WindTrack windTrack = getOrCreateWindTrack(windSource);
                        // replicate all wind fixes that may have been loaded by the wind store
                        windTrack.lockForRead();
                        try {
                            for (Wind wind : windTrack.getRawFixes()) {
                                listener.windDataReceived(wind, windSource);
                            }
                        } finally {
                            windTrack.unlockAfterRead();
                        }
                    }
                }
            }
            if (notifyAboutGPSFixesAlreadyLoaded) {
                for (Mark mark : getMarks()) {
                    GPSFixTrack<Mark, GPSFix> markTrack = getOrCreateTrack(mark);
                    markTrack.lockForRead();
                    try {
                        boolean firstInTrack = true;
                        for (GPSFix fix : markTrack.getRawFixes()) {
                            listener.markPositionChanged(fix, mark, firstInTrack, /* addedOrReplaced */ AddResult.ADDED);
                            firstInTrack = false;
                        }
                    } finally {
                        markTrack.unlockAfterRead();
                    }
                }
                
                for (Competitor competitor : getRace().getCompetitors()) {
                    GPSFixTrack<Competitor, GPSFixMoving> competitorTrack = getTrack(competitor);
                    competitorTrack.lockForRead();
                    try {
                        for (GPSFixMoving fix : competitorTrack.getRawFixes()) {
                            listener.competitorPositionChanged(fix, competitor, /* addedOrReplaced */ AddResult.ADDED);
                        }
                    } finally {
                        competitorTrack.unlockAfterRead();
                    }
                }
            }
        } finally {
            if (notifyAboutWindFixesAlreadyLoaded) {
                LockUtil.unlockAfterRead(getLoadingFromWindStoreLock());
            }
            if (notifyAboutGPSFixesAlreadyLoaded) {
                LockUtil.unlockAfterRead(getLoadingFromGPSFixStoreLock());
            }
        }
    }

    @Override
    public void removeListener(RaceChangeListener listener) {
        synchronized (getListeners()) {
            getListeners().remove(listener);
        }
    }

    /**
     * Adds the {@link WindSourceType#COURSE_BASED} wind source to those to exclude if the race is not
     * {@link #raceIsKnownToStartUpwind known to start with an upwind leg}.
     */
    @Override
    public void setWindSourcesToExclude(final Iterable<? extends WindSource> windSourcesToExclude) {
        final Set<WindSource> effectiveWindSourcesToExclude = new HashSet<>();
        Util.addAll(windSourcesToExclude, effectiveWindSourcesToExclude);
        if (!raceIsKnownToStartUpwind) {
            effectiveWindSourcesToExclude.add(new WindSourceImpl(WindSourceType.COURSE_BASED));
        }
        super.setWindSourcesToExclude(effectiveWindSourcesToExclude);
        notifyListenersWindSourcesToExcludeChanged(effectiveWindSourcesToExclude);
    }

    @Override
    public void setFinishingTime(TimePoint newFinishingTime) {
        final TimePoint oldFinishingTime = getFinishingTime();
        if (!Util.equalsWithNull(newFinishingTime, oldFinishingTime)) {
            logger.info("Finishing time of race " + getRace().getName() + " updated from " + getFinishingTime() + " to " + newFinishingTime);
            super.setFinishingTime(newFinishingTime);
            notifyListenersFinishingTimeChanged(oldFinishingTime, newFinishingTime);
        }
    }

    @Override
    public void setFinishedTime(final TimePoint newFinishedTime) {
        final TimePoint oldFinishedTime = getFinishedTime();
        if (!Util.equalsWithNull(newFinishedTime, oldFinishedTime)) {
            logger.info("Finished time of race " + getRace().getName() + " updated from " + getFinishedTime() + " to " + newFinishedTime);
            super.setFinishedTime(newFinishedTime);
            updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
            notifyListenersFinishedTimeChanged(oldFinishedTime, newFinishedTime);
        }
    }
    
    private void notifyListenersWindSourcesToExcludeChanged(Iterable<? extends WindSource> windSourcesToExclude) {
        notifyListeners(listener -> listener.windSourcesToExcludeChanged(windSourcesToExclude));
    }

    private void notifyListenersStartOfTrackingChanged(TimePoint oldStartOfTracking, TimePoint newStartOfTracking) {
        notifyListeners(listener -> listener.startOfTrackingChanged(oldStartOfTracking, newStartOfTracking));
    }

    private void notifyListenersEndOfTrackingChanged(TimePoint oldEndOfTracking, TimePoint newEndOfTracking) {
        notifyListeners(listener -> listener.endOfTrackingChanged(oldEndOfTracking, newEndOfTracking));
    }

    private void notifyListenersStartTimeReceivedChanged(TimePoint startTimeReceived) {
        notifyListeners(listener -> listener.startTimeReceivedChanged(startTimeReceived));
    }

    private void notifyListenersStartOfRaceChanged(TimePoint oldStartOfRace, TimePoint newStartOfRace) {
        notifyListeners(listener -> listener.startOfRaceChanged(oldStartOfRace, newStartOfRace));
    }

    private void notifyListenersFinishingTimeChanged(TimePoint oldFinishingTime, TimePoint newFinishingTime) {
        notifyListeners(listener -> listener.finishingTimeChanged(oldFinishingTime, newFinishingTime));
    }

    private void notifyListenersFinishedTimeChanged(TimePoint oldFinishedTime, TimePoint newFinishedTime) {
        notifyListeners(listener -> listener.finishedTimeChanged(oldFinishedTime, newFinishedTime));
    }

    private void notifyListenersWaypointAdded(int zeroBasedIndex, Waypoint waypointThatGotAdded) {
        notifyListeners(listener -> listener.waypointAdded(zeroBasedIndex, waypointThatGotAdded));
    }

    private void notifyListenersWaypointRemoved(int zeroBasedIndex, Waypoint waypointThatGotRemoved) {
        notifyListeners(listener -> listener.waypointRemoved(zeroBasedIndex, waypointThatGotRemoved));
    }

    @Override
    public void waypointAdded(int zeroBasedIndex, Waypoint waypointThatGotAdded) {
        super.waypointAdded(zeroBasedIndex, waypointThatGotAdded);
        if (zeroBasedIndex == getRace().getCourse().getNumberOfWaypoints()-1) {
            // a new finishing waypoint was added; make sure that explicit finishing times from
            // the race log are now applied to this new finishing waypoint:
            updateFinishingTimesFromRaceLog();
        }
        notifyListenersWaypointAdded(zeroBasedIndex, waypointThatGotAdded);
    }

    @Override
    public void waypointRemoved(int zeroBasedIndex, Waypoint waypointThatGotRemoved) {
        super.waypointRemoved(zeroBasedIndex, waypointThatGotRemoved);
        if (zeroBasedIndex == getRace().getCourse().getNumberOfWaypoints()) {
            // the previous finishing waypoint was removed, if the course is not empty, a new ; make sure that explicit finishing times from
            // the race log are now applied to this new finishing waypoint:
            updateFinishingTimesFromRaceLog();
        }
        notifyListenersWaypointRemoved(zeroBasedIndex, waypointThatGotRemoved);
    }

    private void notifyListeners(GPSFix fix, Mark mark, boolean firstInTrack, AddResult addedOrReplaced) {
        notifyListeners(listener -> listener.markPositionChanged(fix, mark, firstInTrack, addedOrReplaced));
    }

    private void notifyListeners(GPSFixMoving fix, Competitor competitor, AddResult addedOrReplaced) {
        notifyListeners(listener -> listener.competitorPositionChanged(fix, competitor, addedOrReplaced));
    }

    private void notifyListenersAboutFirstGPSFixReceived() {
        notifyListeners(RaceChangeListener::firstGPSFixReceived);
    }

    private void notifyListeners(TrackedRaceStatus status, TrackedRaceStatus oldStatus) {
        notifyListeners(listener -> listener.statusChanged(status, oldStatus));
    }

    private void notifyListeners(Wind wind, WindSource windSource) {
        notifyListeners(listener -> listener.windDataReceived(wind, windSource));
    }

    private void notifyListenersSpeedAveragingChanged(long oldMillisecondsOverWhichToAverageSpeed, long newMillisecondsOverWhichToAverageSpeed) {
        notifyListeners(listener -> listener.speedAveragingChanged(oldMillisecondsOverWhichToAverageSpeed, newMillisecondsOverWhichToAverageSpeed));
    }

    private void notifyListenersWindAveragingChanged(long oldMillisecondsOverWhichToAverageWind, long newMillisecondsOverWhichToAverageWind) {
        notifyListeners(listener -> listener.windAveragingChanged(oldMillisecondsOverWhichToAverageWind, newMillisecondsOverWhichToAverageWind));
    }

    private void notifyListenersDelayToLiveChanged(long delayToLiveInMillis) {
        notifyListeners(listener -> listener.delayToLiveChanged(delayToLiveInMillis));
    }

    private void notifyListenersWindRemoved(Wind wind, WindSource windSource) {
        notifyListeners(listener -> listener.windDataRemoved(wind, windSource));
    }

    private void notifyListeners(Competitor competitor, Map<Waypoint, MarkPassing> oldMarkPassings, Iterable<MarkPassing> markPassings) {
        notifyListeners(listener -> listener.markPassingReceived(competitor, oldMarkPassings, markPassings));
    }
    
    private void notifyListenersAboutSensorTrackAdded(DynamicSensorFixTrack<Competitor, ?> track) {
        notifyListeners(listener -> listener.competitorSensorTrackAdded(track));
    }
    
    private void notifyListeners(Competitor competitor, String trackName, SensorFix fix, AddResult addedOrReplaced) {
        notifyListeners(listener -> listener.competitorSensorFixAdded(competitor, trackName, fix, addedOrReplaced));
    }

    /**
     * A mark passing produced from a {@link RaceLogFinishPositioningConfirmedEvent} where a
     * {@link CompetitorResult#getFinishingTime() finishing time} has been provided for a competitor. The mark passing
     * may have replaced an original mark passing, as provided by the {@link MarkPassingCalculator} or an external
     * connector, or it may have been provided solely based on the presence of the finishing time in the
     * {@link RaceLogFinishPositioningConfirmedEvent}. This leads to a different implementation of the
     * {@link #getOriginal} implementation.
     * 
     * @author Axel Uhl (D043530)
     */
    private static class MarkPassingFromRaceLogProvidedFinishingTimeImpl extends MarkPassingImpl {
        private static final long serialVersionUID = 2900812554717213461L;
        private final MarkPassing original;

        public MarkPassingFromRaceLogProvidedFinishingTimeImpl(TimePoint timePoint, Waypoint waypoint, Competitor competitor, MarkPassing original) {
            super(timePoint, waypoint, competitor);
            this.original = original;
        }

        /**
         * May return {@code null} in case this mark passing was created based solely on the finishing
         * time taken from a {@link RaceLogFinishPositioningConfirmedEvent}, or may contain the original
         * {@link MarkPassing} that it replaced, otherwise. The original mark passing is expected not to
         * implement this interface.
         */
        @Override
        public MarkPassing getOriginal() {
            return original;
        }
    }
    
    @Override
    public void updateMarkPassings(Competitor competitor, final Iterable<MarkPassing> markPassings) {
        final CompetitorResult resultFromRaceLog = competitorResultsFromRaceLog.get(competitor);
        final Iterable<MarkPassing> markPassingsToUse = createOrUpdateFinishMarkPassingIfRequired(competitor, markPassings, resultFromRaceLog);
        updateMarkPassingsNotConsideringFinishingTimesFromRaceLog(competitor, markPassingsToUse);
    }
    
    @Override
    public void updateMarkPassingsAfterRaceLogChanges() {
        competitorResultsFromRaceLog = getResultsFromRaceLogs();
        updateFinishingTimesFromRaceLog();
    }
    
    /**
     * Iterates over the {@code markPassings} and checks if the finish mark passing needs to be updated based on the
     * {@link CompetitorResult}. In the following cases an updated mark passings {@link Iterable} is returned that
     * contains an update regarding the finish mark passing:
     * 
     * <ul>
     * <li>An original finish mark passing exists, not wrapped by an updated mark passing or wrapped but not having the
     * correct time, or no finish mark passing exists at all, and we have a finishing time in the
     * {@code competitorResult}: then an updated wrapper mark passing is used; if an original finish mark passing
     * existed, it will be used as the wrapper's {@link MarkPassing#getOriginal()} value; otherwise, {@code null} is
     * used as the original.</li>
     * <li>An original finish mark passing exists that is wrapped by an updated finishing time that came from the race
     * log previously, and we have no finishing time in the {@code competitorResult}: then the wrapper is replaced by
     * the original finish mark passing, thus reverting the finishing time to that of the original mark passing.</li>
     * <li>A wrapper finish mark passing exists that has a {@code null} original, meaning it was created solely based
     * on the race log finishing time, and we have no finish time in the {@code competitorResult}: then the synthetic
     * finish mark passing is removed.</li>
     * <li>A mark passing exists for a waypoint that is not (no longer?) the finishing waypoint and that is a wrapper
     * mark passing for a finish time coming from the race log. Such a mark passing is replaced by its original mark
     * passing which may be {@code null} in which case it will not be added to the result.</li>
     * </ul>
     * 
     * If no such modification was required, the unmodified {@code markPassings} object is returned; otherwise, a new
     * {@link Iterable} that contains the modifications regarding the finish mark passing is returned.<p>
     * 
     * Note that if that is a result of {@link #getMarkPassings(Competitor)} or
     * {@link #getMarkPassingsInOrder(Waypoint)} then the caller must hold the corresponding read lock while calling
     * this method.
     */
    private Iterable<MarkPassing> createOrUpdateFinishMarkPassingIfRequired(final Competitor competitor, Iterable<MarkPassing> markPassings, CompetitorResult competitorResult) {
        assert competitorResult == null || competitorResult.getCompetitorId().equals(competitor.getId());
        final Waypoint finish = getRace().getCourse().getLastWaypoint();
        final List<MarkPassing> copyOfMarkPassings = new ArrayList<>();
        boolean neededToCreateOrUpdateFinishMarkPassing = false;
        boolean foundFinishMarkPassing = false;
        for (final MarkPassing originalMarkPassing : markPassings) {
            final MarkPassing originalsOriginal = originalMarkPassing.getOriginal();
            if (originalMarkPassing.getWaypoint() != finish) {
                // take the originalMarkPassing if it is a real mark passing, or unwrap a wrapper
                // mark passing because this is not the finishing waypoint and hence no finishing time-
                // based mark passing wrapper is desired. Should no original mark passing exist for a
                // wrapper, don't add a mark passing.
                if (originalsOriginal != null) {
                    copyOfMarkPassings.add(originalsOriginal);
                }
            } else {
                foundFinishMarkPassing = true;
                final MarkPassing finishMarkPassingToUse;
                if (/* we have a result for the competitor */ competitorResult != null &&
                    /* the result has a valid finishing time */ competitorResult.getFinishingTime() != null) {
                    // we do have a competitor result with a valid finishing time; we shall have a wrapper mark
                    // passing that has the result's finishing time as its time point.
                    // If we already have a wrapper in place with the correct time, nothing needs to be done.
                    // If no wrapper is in place or a wrapper is in place but has the wrong time, a wrapper
                    // for the original is constructed with the time taken from the competitor result.
                    if (/* is it the original mark passing? */ originalsOriginal == originalMarkPassing ||
                        /* is the time incorrect? */ !originalMarkPassing.getTimePoint().equals(competitorResult.getFinishingTime())) {
                        // since we so far only have the original mark passing or a wrapper mark passing with an
                        // incorrect time point, we need to create a wrapper mark passing:
                        finishMarkPassingToUse = new MarkPassingFromRaceLogProvidedFinishingTimeImpl(
                                competitorResult.getFinishingTime(), finish, competitor,
                                originalsOriginal);
                        logger.info(getRace().getName()+": Updating finish mark passing "+originalsOriginal+" to "+finishMarkPassingToUse);
                        neededToCreateOrUpdateFinishMarkPassing = true;
                    } else {
                        // the finish mark passing already is a wrapper with the correct time; no action is required:
                        assert originalsOriginal != originalMarkPassing;
                        assert originalMarkPassing.getTimePoint().equals(competitorResult.getFinishingTime());
                        finishMarkPassingToUse = originalMarkPassing;
                        neededToCreateOrUpdateFinishMarkPassing = false;
                    }
                } else {
                    // we don't have a result for the competitor, or we do have a result but the finishing time is null;
                    // use the original mark passing (null, if none exists) and flag the change in case it is a change
                    finishMarkPassingToUse = originalsOriginal;
                    if (finishMarkPassingToUse != originalMarkPassing) {
                        logger.info(getRace().getName()+": Reverting race log-based finish mark passing "+originalMarkPassing+" to "+finishMarkPassingToUse+
                                " because no finishing time found anymore for that competitor in race log");
                        neededToCreateOrUpdateFinishMarkPassing = true;
                    } else {
                        neededToCreateOrUpdateFinishMarkPassing = false;
                    }
                }
                if (finishMarkPassingToUse != null) {
                    copyOfMarkPassings.add(finishMarkPassingToUse);
                }
            }
        }
        if (!foundFinishMarkPassing && competitorResult != null && competitorResult.getFinishingTime() != null) {
            // need to create a synthetic finish mark passing based on the race log-provided finishing time
            final MarkPassingFromRaceLogProvidedFinishingTimeImpl finishMarkPassingToUse = new MarkPassingFromRaceLogProvidedFinishingTimeImpl(
                    competitorResult.getFinishingTime(), finish, competitor,
                    /* no original finish mark passing exists; synthetic finish mark passing */ null);
            copyOfMarkPassings.add(finishMarkPassingToUse);
            logger.info(getRace().getName()+": Created "+finishMarkPassingToUse+" based on finishing time provided in race log");
            neededToCreateOrUpdateFinishMarkPassing = true;
        }
        return neededToCreateOrUpdateFinishMarkPassing ? copyOfMarkPassings : markPassings;
    }

    private Map<Competitor, CompetitorResult> getResultsFromRaceLogs() {
        final Map<Competitor, CompetitorResult> result = new HashMap<>();
        CompetitorResults results = null; 
        for (final RaceLog raceLog : attachedRaceLogs.values()) {
            results = new ConfirmedFinishPositioningListFinder(raceLog).analyze().getCompetitorResults();
            if (results != null) {
                for (CompetitorResult cr : results) {
                    result.put(getRace().getCompetitorById(cr.getCompetitorId()), cr);
                }
                break;
            }
        }
        return result;
    }

    /**
     * Updates the {@code markPassings} into the {@link #getMarkPassing(Competitor, Waypoint) mark passing data
     * structure for the waypoints passed} and the {@link #getMarkPassings(Competitor) mark passing data structure for
     * the competitor}. The mark passings are used as-is, without considering any
     * {@link CompetitorResult#getFinishingTime() finishing times} for competitors coming from any {@link RaceLog}.
     * See also {@link #updateMarkPassings(Competitor, Iterable)} which <em>does</em> consider those.
     */
    private void updateMarkPassingsNotConsideringFinishingTimesFromRaceLog(Competitor competitor, Iterable<MarkPassing> markPassings) {
        LockUtil.lockForRead(getSerializationLock()); // keep serializer from reading the mark passings collections
        try {
            Map<Waypoint, MarkPassing> oldMarkPassings = new HashMap<Waypoint, MarkPassing>();
            MarkPassing oldStartMarkPassing = null;
            boolean requiresStartTimeUpdate = true;
            final NavigableSet<MarkPassing> markPassingsForCompetitor = getMarkPassings(competitor);
            lockForRead(markPassingsForCompetitor);
            try {
                for (MarkPassing oldMarkPassing : markPassingsForCompetitor) {
                    if (oldStartMarkPassing == null) {
                        oldStartMarkPassing = oldMarkPassing;
                    }
                    oldMarkPassings.put(oldMarkPassing.getWaypoint(), oldMarkPassing);
                }
            } finally {
                unlockAfterRead(markPassingsForCompetitor);
            }
            final NamedReentrantReadWriteLock markPassingsLock = getMarkPassingsLock(markPassingsForCompetitor);
            TimePoint timePointOfLatestEvent = new MillisecondsTimePoint(0);
            // Make sure that clearMarkPassings and the re-adding of the mark passings are non-interruptible by readers.
            // Note that the write lock for the mark passings in order per waypoint is obtained inside
            // clearMarkPassings(...) as well as inside the subsequent for-loop. It is important to always first obtain the mark passings lock
            // for the competitor mark passings before obtaining the lock for the mark passings in order for the waypoint to avoid
            // deadlocks.
            getRace().getCourse().lockForRead();
            LockUtil.lockForWrite(markPassingsLock);
            try {
                clearMarkPassings(competitor);
                for (MarkPassing markPassing : markPassings) {
                    // Now since this caller of this update may not have held the course lock, mark passings
                    // may already be obsolete and for waypoints that no longer exist. Check:
                    if (getRace().getCourse().getIndexOfWaypoint(markPassing.getWaypoint()) >= 0) {
                        // try to find corresponding old start mark passing
                        if (oldStartMarkPassing != null
                                && markPassing.getWaypoint().equals(oldStartMarkPassing.getWaypoint())) {
                            if (markPassing.getTimePoint() != null && oldStartMarkPassing.getTimePoint() != null
                                    && markPassing.getTimePoint().equals(oldStartMarkPassing.getTimePoint())) {
                                requiresStartTimeUpdate = false;
                            }
                        }
                        if (!Util.contains(getRace().getCourse().getWaypoints(), markPassing.getWaypoint())) {
                            StringBuilder courseWaypointsWithID = new StringBuilder();
                            boolean first = true;
                            for (Waypoint courseWaypoint : getRace().getCourse().getWaypoints()) {
                                if (first) {
                                    first = false;
                                } else {
                                    courseWaypointsWithID.append(" -> ");
                                }
                                courseWaypointsWithID.append(courseWaypoint.toString());
                                courseWaypointsWithID.append(" (ID=");
                                courseWaypointsWithID.append(courseWaypoint.getId());
                                courseWaypointsWithID.append(")");
                            }
                            logger.severe("Received mark passing " + markPassing + " for race " + getRace()
                                    + " for waypoint ID" + markPassing.getWaypoint().getId()
                                    + " but the waypoint does not exist in course " + courseWaypointsWithID);
                        } else {
                            markPassingsForCompetitor.add(markPassing);
                        }
                        Collection<MarkPassing> markPassingsInOrderForWaypoint = getOrCreateMarkPassingsInOrderAsNavigableSet(markPassing
                                .getWaypoint());
                        final NamedReentrantReadWriteLock markPassingsLock2 = getMarkPassingsLock(markPassingsInOrderForWaypoint);
                        LockUtil.lockForWrite(markPassingsLock2);
                        try {
                            // The mark passings of competitor have been removed by the call to
                            // clearMarkPassings(competitor) above
                            // from both, the collection that holds the mark passings by waypoint and the one that holds the
                            // mark passings per competitor; so we can simply add here:
                            markPassingsInOrderForWaypoint.add(markPassing);
                        } finally {
                            LockUtil.unlockAfterWrite(markPassingsLock2);
                        }
                        if (markPassing.getTimePoint().compareTo(timePointOfLatestEvent) > 0) {
                            timePointOfLatestEvent = markPassing.getTimePoint();
                        }
                    } else {
                        logger.warning("Received mark passing "+markPassing+
                                " for non-existing waypoint "+markPassing.getWaypoint()+
                                " in race "+getRace().getName()+". Ignoring.");
                    }
                }
            } finally {
                LockUtil.unlockAfterWrite(markPassingsLock);
                getRace().getCourse().unlockAfterRead();
            }
            updated(timePointOfLatestEvent);
            triggerManeuverCacheRecalculation(competitor);
            // update the race times like start, end and the leg times
            if (requiresStartTimeUpdate) {
                invalidateStartTime();
            }
            invalidateMarkPassingTimes();
            invalidateEndTime();
            // notify *after* all mark passings have been re-established; should avoid flicker
            notifyListeners(competitor, oldMarkPassings, markPassings);
        } finally {
            LockUtil.unlockAfterRead(getSerializationLock());
        }
    }

    /**
     * The {@link CompetitorResults} from the race log as cached in {@link #competitorResultsFromRaceLog} may optionally
     * set a finishing time for competitors. Also, depending on how the race log has been modified (e.g., a new pass
     * could have been started or a {@link RaceLogFinishPositioningConfirmedEvent} could have been revoked) it may be
     * possible that a finishing time previously set from the race log now has to be reset to its original state as
     * coming from the tracking provider. This can either mean resetting the mark passing to its
     * {@link MarkPassing#getOriginal() original} version or removing it in case the {@link MarkPassing#getOriginal()}
     * method delivers {@code null}, meaning that this mark passing was solely based on the race log information which
     * now would have disappeared.
     */
    private void updateFinishingTimesFromRaceLog() {
        final Waypoint finish = getRace().getCourse().getLastWaypoint();
        if (finish != null) { // can't do anything for an empty course
            for (final Competitor competitor : getRace().getCompetitors()) {
                final CompetitorResult competitorResult = competitorResultsFromRaceLog.get(competitor);
                final NavigableSet<MarkPassing> markPassings = getMarkPassings(competitor);
                final Iterable<MarkPassing> markPassingsToUse;
                lockForRead(markPassings);
                try {
                    markPassingsToUse = createOrUpdateFinishMarkPassingIfRequired(competitor, markPassings, competitorResult);
                } finally {
                    unlockAfterRead(markPassings);
                }
                if (markPassingsToUse != markPassings) {
                    updateMarkPassingsNotConsideringFinishingTimesFromRaceLog(competitor, markPassingsToUse);
                }
            }
        }
    }

    @Override
    public void lockForRead(Iterable<MarkPassing> markPassings) {
        getRace().getCourse().lockForRead();
        LockUtil.lockForRead(getMarkPassingsLock(markPassings));
    }

    @Override
    public void unlockAfterRead(Iterable<MarkPassing> markPassings) {
        LockUtil.unlockAfterRead(getMarkPassingsLock(markPassings));
        getRace().getCourse().unlockAfterRead();
    }

    /**
     * Removes all mark passings of <code>competitor</code> from both, the {@link #markPassingsForCompetitor}
     * and the {@link #markPassingsForWaypoint} collections.
     */
    private void clearMarkPassings(Competitor competitor) {
        NavigableSet<MarkPassing> markPassings = getMarkPassings(competitor);
        final NamedReentrantReadWriteLock markPassingsLock = getMarkPassingsLock(markPassings);
        LockUtil.lockForWrite(markPassingsLock);
        try {
            Iterator<MarkPassing> mpIter = markPassings.iterator();
            while (mpIter.hasNext()) {
                MarkPassing mp = mpIter.next();
                mpIter.remove();
                Collection<MarkPassing> markPassingsInOrder = getMarkPassingsInOrderAsNavigableSet(mp.getWaypoint());
                LockUtil.lockForWrite(getMarkPassingsLock(markPassingsInOrder));
                try {
                    markPassingsInOrder.remove(mp);
                } finally {
                    LockUtil.unlockAfterWrite(getMarkPassingsLock(markPassingsInOrder));
                }
            }
        } finally {
            LockUtil.unlockAfterWrite(markPassingsLock);
        }
    }

    @Override
    public void setStartTimeReceived(TimePoint startTimeReceived) {
        if (!Util.equalsWithNull(startTimeReceived, getStartTimeReceived())) {
            super.setStartTimeReceived(startTimeReceived);
            notifyListenersStartTimeReceivedChanged(getStartTimeReceived());
        }
    }

    @Override
    public void setStartOfTrackingReceived(TimePoint startOfTrackingReceived) {
        setStartOfTrackingReceived(startOfTrackingReceived, /* waitForGPSFixesToLoad */ false);
    }

    /**
     * Making this method publicly visible for those tests seeing this implementation class
     */
    public void setStartOfTrackingReceived(TimePoint startOfTrackingReceived, final boolean waitForGPSFixesToLoad) {
        super.setStartOfTrackingReceived(startOfTrackingReceived, waitForGPSFixesToLoad);
    }
    
    @Override
    protected void startOfTrackingChanged(final TimePoint oldStartOfTracking, boolean waitForGPSFixesToLoad) {
        super.startOfTrackingChanged(oldStartOfTracking, waitForGPSFixesToLoad);
        if (!Util.equalsWithNull(oldStartOfTracking, getStartOfTracking())) {
            notifyListenersStartOfTrackingChanged(oldStartOfTracking, getStartOfTracking());
        }
    }

    @Override
    protected void endOfTrackingChanged(TimePoint oldEndOfTracking, boolean waitForGPSFixesToLoad) {
        super.endOfTrackingChanged(oldEndOfTracking, waitForGPSFixesToLoad);
        if (!Util.equalsWithNull(oldEndOfTracking, getEndOfTracking())) {
            notifyListenersEndOfTrackingChanged(oldEndOfTracking, getEndOfTracking());
        }
    }

    /**
     * Making this method publicly visible for those tests seeing this implementation class
     */
    public void setEndOfTrackingReceived(final TimePoint endOfTrackingReceived, final boolean waitForGPSFixesToLoad) {
        super.setEndOfTrackingReceived(endOfTrackingReceived, waitForGPSFixesToLoad);
    }

    @Override
    public void setEndOfTrackingReceived(TimePoint endOfTrackingReceived) {
        setEndOfTrackingReceived(endOfTrackingReceived, /* waitForGPSFixesToLoad */ false);
    }

    /**
     * In addition to calling the superclass implementation, for a stored wind track whose fixes were loaded into this
     * new wind track, all listeners are notified about these existing wind fixes using their
     * {@link RaceChangeListener#windDataReceived(Wind, WindSource)} callback method. In particular this replicates all
     * wind fixes that may have been loaded from the wind store for the new track.
     */
    @Override
    protected void notifyWindTrackHasBeenCreatedAndAddedToWindTracks(WindSource windSource, WindTrack windTrack) {
        super.notifyWindTrackHasBeenCreatedAndAddedToWindTracks(windSource, windTrack);
        if (windSource.getType().canBeStored()) {
            // replicate all wind fixes that may have been loaded by the wind store
            windTrack.lockForRead();
            try {
                for (Wind wind : windTrack.getRawFixes()) {
                    notifyListeners(wind, windSource); // Note that this doesn't notify the track's listeners but the tracked race's listeners.
                } // In particular, the wind store won't receive events (again) for the wind fixes it already has.
            } finally {
                windTrack.unlockAfterRead();
            }
        }
    }

    @Override
    public boolean recordWind(Wind wind, WindSource windSource, boolean applyFilter) {
        boolean result = super.recordWind(wind, windSource, applyFilter);
        if (result) {
            notifyListeners(wind, windSource);
        }
        return result;
    }

    @Override
    public void removeWind(Wind wind, WindSource windSource) {
        super.removeWind(wind, windSource);
        notifyListenersWindRemoved(wind, windSource);
    }

    @Override
    public void gpsFixReceived(GPSFixMoving fix, Competitor competitor, boolean firstFixInTrack, AddResult addedOrReplaced) {
        updated(fix.getTimePoint());
        invalidateDistancesFromStarboardSideOfStartLineProjectedOntoLineCache(TimeRange.create(
                fix.getTimePoint().minus(getMillisecondsOverWhichToAverageSpeed()),
                fix.getTimePoint().plus(getMillisecondsOverWhichToAverageSpeed())));
        triggerManeuverCacheRecalculation(competitor);
        notifyListeners(fix, competitor, addedOrReplaced);
        // getAndSet call is atomic which means, that it can be ensured that the listeners are notified only once
        final boolean oldGPSFixReceived = gpsFixReceived.getAndSet(true);
        if (!oldGPSFixReceived) {
            notifyListenersAboutFirstGPSFixReceived();
        }
    }

    @Override
    public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
        notifyListenersSpeedAveragingChanged(oldMillisecondsOverWhichToAverage, newMillisecondsOverWhichToAverage);
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    @Override
    protected TrackedLeg createTrackedLeg(Leg leg) {
        return new TrackedLegImpl(this, leg, getRace().getCompetitors());
    }

    @Override
    public long getMillisecondsOverWhichToAverageSpeed() {
        long result = 0; // default in case there is no competitor
        Iterator<Competitor> compIter = getRace().getCompetitors().iterator();
        if (compIter.hasNext()) {
            DynamicGPSFixTrack<Competitor, GPSFixMoving> someTrack = getTrack(compIter.next());
            result = someTrack.getMillisecondsOverWhichToAverageSpeed();
        }
        return result;
    }

    @Override
    public long getMillisecondsOverWhichToAverageWind() {
        return this.millisecondsOverWhichToAverageWind;
    }

    @Override
    public DynamicTrackedRegatta getTrackedRegatta() {
        return (DynamicTrackedRegatta) super.getTrackedRegatta();
    }

    @Override
    public void setRaceIsKnownToStartUpwind(boolean raceIsKnownToStartUpwind) {
        this.raceIsKnownToStartUpwind = raceIsKnownToStartUpwind;
    }

    @Override
    public boolean raceIsKnownToStartUpwind() {
        return raceIsKnownToStartUpwind;
    }
    
    @Override
    public void attachRaceLog(RaceLog raceLog) {
        logListener.beforeAttaching(raceLog);
        super.attachRaceLog(raceLog);
        logListener.afterAttaching(raceLog);
        getRaceState(raceLog).addChangedListener(raceStateBasedStartTimeChangedListener);
    }
    
    @Override
    public RaceLog detachRaceLog(Serializable identifier) {
        final RaceLog attachedRaceLog = attachedRaceLogs.get(identifier);
        if (attachedRaceLog != null) {
            logListener.beforeDetaching(attachedRaceLog);
        }
        final RaceLog raceLogDetached = super.detachRaceLog(identifier);
        if (attachedRaceLog != null) {
            logListener.afterDetaching(attachedRaceLog);
        }
        assert raceLogDetached == attachedRaceLog;
        synchronized (raceStates) {
            final ReadonlyRaceState raceState = raceStates.remove(attachedRaceLog);
            if (raceState != null) {
                raceState.removeChangedListener(raceStateBasedStartTimeChangedListener);
            }
        }
        return attachedRaceLog;
    }
    
    @Override
    public void addCourseDesignChangedListener(CourseDesignChangedListener listener) {
        this.courseDesignChangedListeners.add(listener);
    }

    @Override
    public void onCourseDesignChangedByRaceCommittee(CourseBase newCourseDesign) {
        try {
            for (CourseDesignChangedListener courseDesignChangedListener : courseDesignChangedListeners) {
                courseDesignChangedListener.courseDesignChanged(newCourseDesign);
            }
        } catch (IOException e) {
            logger.log(Level.INFO, "Exception trying to notify race course design change listeners about course design change", e);
        }
    }

    @Override
    public void onStartTimeChangedByRaceCommittee(TimePoint newStartTime) {
        logger.info("Start time of race "+getRace().getName()+" updated by race committee to "+newStartTime);
        try {
            for (StartTimeChangedListener startTimeChangedListener : startTimeChangedListeners) {
                startTimeChangedListener.startTimeChanged(newStartTime);
            }
        } catch (IOException | URISyntaxException e) {
            logger.log(Level.INFO, "Exception trying to notify race status change listeners about start time change", e);
        }
        updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
    }
    
    @Override
    public void onAbortedByRaceCommittee(Flags flag) {
        try {
            for (RaceAbortedListener raceAbortedListener : raceAbortedListeners) {
                raceAbortedListener.raceAborted(flag);
            }
        } catch (IOException e) {
            logger.log(Level.INFO, "Exception trying to notify race status change listeners about start time change", e);
        }
    }

    @Override
    public void addStartTimeChangedListener(StartTimeChangedListener listener) {
        this.startTimeChangedListeners.add(listener);
    }
    
    
    @Override
    public void removeStartTimeChangedListener(StartTimeChangedListener listener) {
        startTimeChangedListeners.remove(listener);
    }

    @Override
    public void addRaceAbortedListener(RaceAbortedListener listener) {
        this.raceAbortedListeners.add(listener);
    }
    
    @Override
    protected MarkPassingCalculator createMarkPassingCalculator(MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry) {
        // not waiting for initial mark passing creation is essential for not holding up, e.g.,
        // an initial load during replication where it is perfectly fine to obtain the results from
        // mark passing analysis as they become available; for test cases, however, "true" would
        // be a more appropriate choice
        return new MarkPassingCalculator(this, /* doListen */ true, /* waitForInitialMarkPassingCalculation */ false, markPassingRaceFingerprintRegistry); 
    }

    @Override
    public DynamicGPSFixTrack<Mark, GPSFix> getTrack(Mark mark) {
        return (DynamicGPSFixTrack<Mark, GPSFix>) super.getTrack(mark);
    }
    @Override
    public <FixT extends SensorFix, TrackT extends DynamicSensorFixTrack<Competitor, FixT>> TrackT getOrCreateSensorTrack(
            Competitor competitor, String trackName, TrackFactory<TrackT> newTrackFactory) {
        return super.getOrCreateSensorTrack(competitor, trackName, newTrackFactory);
    }
    
    @Override
    public <FixT extends SensorFix, TrackT extends DynamicSensorFixTrack<Competitor, FixT>> TrackT getDynamicSensorTrack(
            Competitor competitor, String trackName) {
        return super.getSensorTrack(competitor, trackName);
    }
    
    @Override
    public void recordSensorFix(Competitor competitor, String trackName, SensorFix fix, boolean onlyWhenInTrackingTimesInterval) {
        if (!onlyWhenInTrackingTimesInterval || isWithinStartAndEndOfTracking(fix.getTimePoint())) {
            DynamicSensorFixTrack<Competitor, SensorFix> track = getSensorTrack(competitor, trackName);
            if (track != null) {
                if (logger != null && logger.getLevel() != null && logger.getLevel().equals(Level.FINEST)) {
                    logger.finest(""+competitor.getName() + ": " + fix);
                }
                track.add(fix); // the track notifies this tracked race which in turn notifies its listeners
            }
        }
    }
    
    @Override
    public void addSensorTrack(Competitor competitor, String trackName, DynamicSensorFixTrack<Competitor, ?> track) {
        super.addSensorTrack(competitor, trackName, track);
    }
    
    @Override
    protected <FixT extends SensorFix> Optional<Runnable> addSensorTrackInternal(Pair<Competitor, String> key,
            DynamicSensorFixTrack<Competitor, FixT> track) {
        super.addSensorTrackInternal(key, track);
        track.addListener(new SensorFixTrackListener<Competitor, FixT>() {
            private static final long serialVersionUID = 6143309919537011377L;

            @Override
            public boolean isTransient() {
                return true;
            }

            @Override
            public void fixReceived(FixT fix, Competitor item, String trackName, boolean firstFixInTrack, AddResult addedOrReplaced) {
                notifyListeners(item, trackName, fix, addedOrReplaced);
            }
        });
        return Optional.of(()->notifyListenersAboutSensorTrackAdded(track));
    }
}
