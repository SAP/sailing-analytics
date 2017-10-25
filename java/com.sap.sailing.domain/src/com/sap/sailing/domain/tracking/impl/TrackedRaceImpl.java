package com.sap.sailing.domain.tracking.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogDependentStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEndOfTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogGateLineOpeningTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartOfTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.FinishedTimeFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.TrackingTimesFinder;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogGateLineOpeningTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.RaceStateImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.ReadonlyRacingProcedure;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogDefinedMarkAnalyzer;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseListener;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.DouglasPeucker;
import com.sap.sailing.domain.base.impl.SpeedWithConfidenceImpl;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.BearingChangeAnalyzer;
import com.sap.sailing.domain.common.CourseChange;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TargetTimeInfo;
import com.sap.sailing.domain.common.TargetTimeInfo.LegTargetTimeInfo;
import com.sap.sailing.domain.common.TimingConstants;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.abstractlog.TimePointSpecificationFoundInLog;
import com.sap.sailing.domain.common.confidence.BearingWithConfidence;
import com.sap.sailing.domain.common.confidence.BearingWithConfidenceCluster;
import com.sap.sailing.domain.common.confidence.HasConfidence;
import com.sap.sailing.domain.common.confidence.Weigher;
import com.sap.sailing.domain.common.confidence.impl.BearingWithConfidenceImpl;
import com.sap.sailing.domain.common.confidence.impl.HyperbolicTimeDifferenceWeigher;
import com.sap.sailing.domain.common.confidence.impl.PositionAndTimePointWeigher;
import com.sap.sailing.domain.common.confidence.impl.ScalableWind;
import com.sap.sailing.domain.common.impl.CentralAngleDistance;
import com.sap.sailing.domain.common.impl.CourseChangeImpl;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.TargetTimeInfoImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.common.scalablevalue.impl.ScalablePosition;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.SensorFix;
import com.sap.sailing.domain.confidence.ConfidenceBasedWindAverager;
import com.sap.sailing.domain.confidence.ConfidenceFactory;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingCalculator;
import com.sap.sailing.domain.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.ranking.RankingMetric;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sailing.domain.ranking.RankingMetricConstructor;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.DynamicSensorFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrack;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.GPSTrackListener;
import com.sap.sailing.domain.tracking.LineDetails;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.MarkPositionAtTimePointCache;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.RaceExecutionOrderProvider;
import com.sap.sailing.domain.tracking.RaceListener;
import com.sap.sailing.domain.tracking.SensorFixTrack;
import com.sap.sailing.domain.tracking.SpeedWithBearingStep;
import com.sap.sailing.domain.tracking.Track;
import com.sap.sailing.domain.tracking.TrackFactory;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.TrackedRaceWithWindEssentials;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingCache;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.GPSFixTrackImpl.SpeedWithBearingStepImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.IsManagedByCache;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.util.IdentityWrapper;
import com.sap.sse.util.SmartFutureCache;
import com.sap.sse.util.SmartFutureCache.AbstractCacheUpdater;
import com.sap.sse.util.SmartFutureCache.EmptyUpdateInterval;
import com.sap.sse.util.impl.ArrayListNavigableSet;
import com.sap.sse.util.impl.FutureTaskWithTracingGet;

import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;

public abstract class TrackedRaceImpl extends TrackedRaceWithWindEssentials implements CourseListener {
    /**
     * Used in maneuver detection algorithm to approximate the start and end time of maneuver main curve performance. It
     * defines the absolute course change in degrees between bearing steps to ignore in order shorten the approximated
     * span between start and end time of maneuver main curve.
     */
    private static final double ABS_COURSE_CHANGE_IN_DEGREES_TO_IGNORE_BETWEEN_BEARING_STEPS = 0.001;

    private static final long serialVersionUID = -4825546964220003507L;

    private static final Logger logger = Logger.getLogger(TrackedRaceImpl.class.getName());

    // TODO make this variable
    private static final long DELAY_FOR_CACHE_CLEARING_IN_MILLISECONDS = 7500;

    public static final Duration TIME_BEFORE_START_TO_TRACK_WIND_MILLIS = Duration.ONE_MINUTE.times(4); // let wind start four minutes before race
    
    public static final Duration EXTRA_LONG_TIME_BEFORE_START_TO_TRACK_WIND_MILLIS = Duration.ONE_HOUR;

    private TrackedRaceStatus status;

    private final Object statusNotifier;

    /**
     * By default, all wind sources are used, none are excluded. However, e.g., for performance reasons, particular wind
     * sources such as the track-based estimation wind source, may be excluded by adding them to this set.
     */
    private final ConcurrentMap<WindSource, TrackedRaceImpl> windSourcesToExclude;

    /**
     * Keeps the oldest timestamp that is fed into this tracked race, either from a boat fix, a mark fix, a race
     * start/finish or a course definition.
     */
    private TimePoint timePointOfOldestEvent;

    /**
     * The start of tracking time as announced by the tracking infrastructure.
     */
    private TimePoint startOfTrackingReceived;

    /**
     * The end of tracking time as announced by the tracking infrastructure.
     */
    private TimePoint endOfTrackingReceived;

    /**
     * The start and end of tracking inferred via RaceLog, the received timepoint (see above) and mapping intervals.
     * For the precedence order see {@link #updateStartAndEndOfTracking(boolean)}.
     */
    private TimePoint startOfTracking;
    private TimePoint endOfTracking;

    /**
     * Race start time as announced by the tracking infrastructure
     */
    private TimePoint startTimeReceived;
    
    /**
     * The calculated race start time
     */
    private TimePoint startTime;
    
    /**
     * Maintained in lock-step with {@link #startTime}, only that {@code null} will be contained if {@link #startTime}
     * was only inferred from start mark passings and no other, more official, information such as
     * {@link #getStartTimeReceived()} or a start time coming from an attached {@link RaceLog}.
     */
    private TimePoint startTimeWithoutInferenceFromStartMarkPassings;
    
    /**
     * The calculated race end time
     */
    private TimePoint endTime;

    /**
     * The time set by race management ("Blue Flag Down" event) for when the race has finished. This field caches what
     * today comes from the {@link RaceLog}s in the form of {@link RaceLogRaceStatusEvent}s setting the status to
     * {@link RaceLogRaceStatus#FINISHED} and is computed by the {@link DynamicTrackedRaceLogListener#getFinishedTime()}
     * method based on the {@link RaceState}s it manages for all the {@link RaceLog}s currently attached to this race.
     */
    private TimePoint finishedTime;

    /**
     * The first and last passing times of all course waypoints
     */
    private transient List<Pair<Waypoint, Pair<TimePoint, TimePoint>>> markPassingsTimes;

    /**
     * The latest time point contained by any of the events received and processed
     */
    private TimePoint timePointOfNewestEvent;

    /**
     * Time stamp that the event received last from the underlying push service carried on it
     */
    private TimePoint timePointOfLastEvent;

    private long updateCount;

    /**
     * Limit for the cache size in {@link #competitorRankings} and respectively in {@link #competitorRankingsLocks}.
     */
    private static final int MAX_COMPETITOR_RANKINGS_CACHE_SIZE = 10;
    
    private transient LinkedHashMap<TimePoint, List<Competitor>> competitorRankings;

    /**
     * The locks managed here correspond with the {@link #competitorRankings} structure. When
     * {@link #getCompetitorsFromBestToWorst(TimePoint)} starts to compute rankings, it locks the write lock for the
     * time point. Readers use the read lock. Checking / entering a lock into this map uses <code>synchronized</code> on
     * the map itself.
     */
    private transient LinkedHashMap<TimePoint, NamedReentrantReadWriteLock> competitorRankingsLocks;

    /**
     * legs appear in the order in which they appear in the race's course
     */
    private final LinkedHashMap<Leg, TrackedLeg> trackedLegs;

    private final Map<Competitor, GPSFixTrack<Competitor, GPSFixMoving>> tracks;

    private final Map<Competitor, NavigableSet<MarkPassing>> markPassingsForCompetitor;

    /**
     * The mark passing sets used as values are ordered by time stamp.
     */
    private final Map<Waypoint, NavigableSet<MarkPassing>> markPassingsForWaypoint;

    /**
     * Values are the <code>from</code> and <code>to</code> time points between which the maneuvers have been previously
     * computed. Clients wanting to know maneuvers for the competitor outside of this time interval need to (re-)compute
     * them.
     */
    private transient SmartFutureCache<Competitor, com.sap.sse.common.Util.Triple<TimePoint, TimePoint, List<Maneuver>>, EmptyUpdateInterval> maneuverCache;
    
    private transient ConcurrentMap<TimePoint, Future<Wind>> directionFromStartToNextMarkCache;

    protected transient MarkPassingCalculator markPassingCalculator;
    
    private final ConcurrentMap<Mark, GPSFixTrack<Mark, GPSFix>> markTracks;

    /**
     * Mapping of {@link Competitor} to generic {@link DynamicTrack} implementation. Because the same competitor could
     * be mapped to several different tracks, a combined key of competitor object and track name identifier string is
     * used. This identifier is usually defined within the track interface (e.g. see {@link BravoFixTrack#TRACK_NAME}).
     */
    private final Map<Pair<Competitor, String>, DynamicTrack<?>> sensorTracks;
    
    private final Map<String, Sideline> courseSidelines;

    protected long millisecondsOverWhichToAverageSpeed;

    private final Map<Mark, StartToNextMarkCacheInvalidationListener> startToNextMarkCacheInvalidationListeners;

    private transient Timer cacheInvalidationTimer;
    private transient Object cacheInvalidationTimerLock;
    
    /**
     * handled by {@link #suspendAllCachesNotUpdatingWhileLoading()} and {@link #resumeAllCachesNotUpdatingWhileLoading()}.
     */
    private boolean cachesSuspended;
    
    /**
     * Whether during {@link #cachesSuspended suspended caches mode} the maneuver re-calculation was triggered; will lead
     * to triggering the maneuver re-calculation when caches are {@link #resumeAllCachesNotUpdatingWhileLoading() resumed}.
     */
    private boolean triggerManeuverCacheInvalidationForAllCompetitors;

    /**
     * Keys are the {@link RaceLog#getId() IDs} of the race logs that are stored as values.
     */
    protected transient ConcurrentMap<Serializable, RaceLog> attachedRaceLogs;
    
    /**
     * Holds optional race states for the race logs in {@link #attachedRaceLogs}. By using a {@link WeakHashMap},
     * these race states can be garbage-collected when the race log is no longer attached. The race states are created
     * lazily, synchronizing on this weak hash map.
     */
    protected transient WeakHashMap<RaceLog, ReadonlyRaceState> raceStates;

    /**
     * Keys are the {@link RegattaLog#getId() IDs} of the regatta logs that are stored as values.
     */
    protected transient ConcurrentMap<Serializable, RegattaLog> attachedRegattaLogs;
    
    private transient ConcurrentMap<RaceExecutionOrderProvider, RaceExecutionOrderProvider> attachedRaceExecutionOrderProviders;

    /**
     * The time delay to the current point in time in milliseconds.
     */
    private long delayToLiveInMillis;

    private enum LoadingFromStoresState { NOT_STARTED, RUNNING, FINISHED };
    
    /**
     * The constructor loads wind fixes from the {@link #windStore} asynchronously.
     * When completed all threads currently waiting on this object are notified.
     */
    private LoadingFromStoresState loadingFromWindStoreState = LoadingFromStoresState.NOT_STARTED;

    private transient CrossTrackErrorCache crossTrackErrorCache;
    
    /**
     * Wind and loading is started in a background thread during object construction. If a client needs to
     * ensure that wind loading either has terminated or has not yet begun, it can obtain the read lock of
     * this lock. The wind loading procedure will obtain the write lock before it starts loading wind fixes.
     */
    private final NamedReentrantReadWriteLock loadingFromWindStoreLock;
    
    /**
     * @see #loadingFromWindStoreLock but for GPSFixStore
     */
    private final NamedReentrantReadWriteLock loadingFromGPSFixStoreLock;

    private final ConcurrentMap<IdentityWrapper<Iterable<MarkPassing>>, NamedReentrantReadWriteLock> locksForMarkPassings;
    
    /**
     * Caches wind requests for a few seconds to accelerate access in live mode
     */
    private transient ShortTimeWindCache shortTimeWindCache;
    
    private transient PolarDataService polarDataService;

    /**
     * Tells how ranks are to be assigned to the competitors at any time during the race. For one-design boat classes
     * this will usually happen by projecting the competitors to the wind direction for upwind and downwind legs or to
     * the leg's rhumb line for reaching legs, then comparing positions. For handicap races using a time-on-time,
     * time-on-distance, combination thereof or a more complicated scheme such as ORC Performance Curve, the ranking
     * process needs to take into account the competitor-specific correction factors defined in the measurement
     * certificate.
     */
    private final RankingMetric rankingMetric;
    
    /**
     * Required in particular to resolve {@link SimpleRaceLogIdentifier}s that appear in
     * {@link RaceLogDependentStartTimeEvent}s. The usual implementation on the server side is
     * provided by <code>RacingEventService</code> which is not serializable. Therefore, the reference
     * must be established again after de-serialization by invoking {@link #setRaceLogResolver}.
     */
    private transient RaceLogResolver raceLogResolver;
    
    private final NamedReentrantReadWriteLock sensorTracksLock;

    /**
     * Constructs the tracked race with one-design ranking.
     */
    public TrackedRaceImpl(final TrackedRegatta trackedRegatta, RaceDefinition race, final Iterable<Sideline> sidelines,
            final WindStore windStore, long delayToLiveInMillis, final long millisecondsOverWhichToAverageWind,
            long millisecondsOverWhichToAverageSpeed, long delayForWindEstimationCacheInvalidation,
            boolean useInternalMarkPassingAlgorithm, RaceLogResolver raceLogResolver) {
        this(trackedRegatta, race, sidelines, windStore, delayToLiveInMillis, millisecondsOverWhichToAverageWind,
                millisecondsOverWhichToAverageSpeed, delayForWindEstimationCacheInvalidation,
                useInternalMarkPassingAlgorithm, OneDesignRankingMetric::new, raceLogResolver);
    }
    
    /**
     * Constructs the tracked race with a configurable ranking metric.
     * @param rankingMetricConstructor
     *            the function that creates the ranking metric, passing this tracked race as argument. Callers may use a
     *            constructor method reference if the {@link RankingMetric} implementation to instantiate takes a single
     *            {@link TrackedRace} argument.
     */
    public TrackedRaceImpl(final TrackedRegatta trackedRegatta, RaceDefinition race, final Iterable<Sideline> sidelines,
            final WindStore windStore, long delayToLiveInMillis, final long millisecondsOverWhichToAverageWind,
            long millisecondsOverWhichToAverageSpeed, long delayForWindEstimationCacheInvalidation,
            boolean useInternalMarkPassingAlgorithm, RankingMetricConstructor rankingMetricConstructor,
            RaceLogResolver raceLogResolver) {
        super(race, trackedRegatta, windStore, millisecondsOverWhichToAverageWind);
        this.raceLogResolver = raceLogResolver;
        rankingMetric = rankingMetricConstructor.apply(this);
        raceStates = new WeakHashMap<>();
        shortTimeWindCache = new ShortTimeWindCache(this, millisecondsOverWhichToAverageWind / 2);
        locksForMarkPassings = new ConcurrentHashMap<IdentityWrapper<Iterable<MarkPassing>>, NamedReentrantReadWriteLock>();
        attachedRaceLogs = new ConcurrentHashMap<>();
        attachedRegattaLogs = new ConcurrentHashMap<>();
        attachedRaceExecutionOrderProviders = new ConcurrentHashMap<>();
        this.status = new TrackedRaceStatusImpl(TrackedRaceStatusEnum.PREPARED, 0.0);
        this.statusNotifier = new Object[0];
        this.loadingFromWindStoreLock = new NamedReentrantReadWriteLock("Loading from wind store lock for tracked race "
                + race.getName(), /* fair */ false);
        this.loadingFromGPSFixStoreLock = new NamedReentrantReadWriteLock("Loading from GPSFix store lock for tracked race "
                + race.getName(), /* fair */ false);
        this.cacheInvalidationTimerLock = new Object();
        this.updateCount = 0;
        this.windSourcesToExclude = new ConcurrentHashMap<>();
        this.directionFromStartToNextMarkCache = new ConcurrentHashMap<TimePoint, Future<Wind>>();
        this.millisecondsOverWhichToAverageSpeed = millisecondsOverWhichToAverageSpeed;
        this.delayToLiveInMillis = delayToLiveInMillis;
        this.startToNextMarkCacheInvalidationListeners = new ConcurrentHashMap<Mark, TrackedRaceImpl.StartToNextMarkCacheInvalidationListener>();
        this.maneuverCache = createManeuverCache();
        this.markTracks = new ConcurrentHashMap<Mark, GPSFixTrack<Mark, GPSFix>>();
        int i = 0;
        for (Waypoint waypoint : race.getCourse().getWaypoints()) {
            for (Mark mark : waypoint.getMarks()) {
                getOrCreateTrack(mark);
                if (i < 2) {
                    // add cache invalidation listeners for first and second waypoint's marks for
                    // directionFromStartToNextMarkCache
                    addStartToNextMarkCacheInvalidationListener(mark);
                }
            }
            i++;
        }
        courseSidelines = new LinkedHashMap<String, Sideline>();
        for (Sideline sideline : sidelines) {
            courseSidelines.put(sideline.getName(), sideline);
            for (Mark mark : sideline.getMarks()) {
                getOrCreateTrack(mark);
            }
        }
        trackedLegs = new LinkedHashMap<Leg, TrackedLeg>();
        race.getCourse().lockForRead();
        try {
            for (Leg leg : race.getCourse().getLegs()) {
                trackedLegs.put(leg, createTrackedLeg(leg));
            }
            getRace().getCourse().addCourseListener(this);
        } finally {
            race.getCourse().unlockAfterRead();
        }
        markPassingsForCompetitor = new HashMap<Competitor, NavigableSet<MarkPassing>>();
        tracks = new HashMap<Competitor, GPSFixTrack<Competitor, GPSFixMoving>>();
        for (Competitor competitor : race.getCompetitors()) {
            markPassingsForCompetitor.put(competitor, new ConcurrentSkipListSet<MarkPassing>(
                    MarkPassingByTimeComparator.INSTANCE));
            tracks.put(competitor, new DynamicGPSFixMovingTrackImpl<Competitor>(competitor,
                    millisecondsOverWhichToAverageSpeed));
        }
        markPassingsForWaypoint = new ConcurrentHashMap<Waypoint, NavigableSet<MarkPassing>>();
        for (Waypoint waypoint : race.getCourse().getWaypoints()) {
            markPassingsForWaypoint.put(waypoint, new ConcurrentSkipListSet<MarkPassing>(
                    MarkPassingsByTimeAndCompetitorIdComparator.INSTANCE));
        }
        markPassingsTimes = new ArrayList<Pair<Waypoint, Pair<TimePoint, TimePoint>>>();
        this.crossTrackErrorCache = new CrossTrackErrorCache(this);
        loadingFromWindStoreState = LoadingFromStoresState.NOT_STARTED;
        // When this tracked race is to be serialized, wait for the loading from stores to complete.
        new Thread("Mongo wind loader for tracked race " + getRace().getName()) {
            @Override
            public void run() {
                LockUtil.lockForRead(getSerializationLock());
                LockUtil.lockForWrite(getLoadingFromWindStoreLock());
                synchronized (TrackedRaceImpl.this) {
                    loadingFromWindStoreState = LoadingFromStoresState.RUNNING; // indicates that the serialization lock is now safely held
                    TrackedRaceImpl.this.notifyAll();
                }
                try {
                    logger.info("Started loading wind tracks for " + getRace().getName());
                    final Map<? extends WindSource, ? extends WindTrack> loadedWindTracks = windStore.loadWindTracks(
                            trackedRegatta.getRegatta().getName(), TrackedRaceImpl.this, millisecondsOverWhichToAverageWind);
                    windTracks.putAll(loadedWindTracks);
                    updateEventTimePoints(loadedWindTracks.values());
                    logger.info("Finished loading wind tracks for " + getRace().getName() + ". Found " + windTracks.size() + " wind tracks for this race.");
                } finally {
                    synchronized (TrackedRaceImpl.this) {
                        loadingFromWindStoreState = LoadingFromStoresState.FINISHED;
                        TrackedRaceImpl.this.notifyAll();
                    }
                    synchronized (loadingFromWindStoreState) {
                        loadingFromWindStoreState.notifyAll();
                    }
                    LockUtil.unlockAfterWrite(getLoadingFromWindStoreLock());
                    LockUtil.unlockAfterRead(getSerializationLock());
                }
            }
        }.start();
        // by default, a tracked race offers one course-based wind estimation and one track-based wind estimation track;
        // other wind tracks may be added as fixes are received for them and as they are loaded from the persistent
        // store
        WindSource courseBasedWindSource = new WindSourceImpl(WindSourceType.COURSE_BASED);
        windTracks.put(courseBasedWindSource,
                getOrCreateWindTrack(courseBasedWindSource, delayForWindEstimationCacheInvalidation));
        WindSource trackBasedWindSource = new WindSourceImpl(WindSourceType.TRACK_BASED_ESTIMATION);
        windTracks.put(trackBasedWindSource,
                getOrCreateWindTrack(trackBasedWindSource, delayForWindEstimationCacheInvalidation));
        competitorRankings = createCompetitorRankingsCache();
        competitorRankingsLocks = createCompetitorRankingsLockMap();
        if (useInternalMarkPassingAlgorithm) {
            markPassingCalculator = createMarkPassingCalculator();
            this.trackedRegatta.addRaceListener(new RaceListener() {
                @Override
                public void raceAdded(TrackedRace trackedRace) {}
                @Override
                public void raceRemoved(TrackedRace trackedRace) {
                    if (trackedRace == TrackedRaceImpl.this) {
                    // stop mark passing calculator when tracked race is removed:
                    markPassingCalculator.stop();
                }
                }
            });
        } else {
            markPassingCalculator = null;
        }
        sensorTracks = new HashMap<>();
        sensorTracksLock = new NamedReentrantReadWriteLock("sensorTracksLock", true);
        // now wait until wind loading has at least started; then we know that the serialization lock is safely held by the loader
        try {
            waitUntilLoadingFromWindStoreComplete();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Waiting for loading from stores to finish was interrupted", e);
        }
    }

    @Override
    public RankingMetric getRankingMetric() {
        return rankingMetric;
    }
    
    private LinkedHashMap<TimePoint, NamedReentrantReadWriteLock> createCompetitorRankingsLockMap() {
        return new LinkedHashMap<TimePoint, NamedReentrantReadWriteLock>() {
            private static final long serialVersionUID = 6298801656693955386L;
            @Override
            protected boolean removeEldestEntry(Entry<TimePoint, NamedReentrantReadWriteLock> eldest) {
                return size() > MAX_COMPETITOR_RANKINGS_CACHE_SIZE;
            }
        };
    }

    private LinkedHashMap<TimePoint, List<Competitor>> createCompetitorRankingsCache() {
        return new LinkedHashMap<TimePoint, List<Competitor>>() {
            private static final long serialVersionUID = -6044369612727021861L;
            @Override
            protected boolean removeEldestEntry(Entry<TimePoint, List<Competitor>> eldest) {
                return size() > MAX_COMPETITOR_RANKINGS_CACHE_SIZE;
            }
        };
    }

    /**
     * Assuming that the tracks were loaded from the persistent store, this method updates
     * the time stamps that frame the data held by this tracked race. See {@link #timePointOfLastEvent}, {@link #timePointOfNewestEvent}
     * and {@link #timePointOfOldestEvent}.
     */
    private void updateEventTimePoints(Iterable<? extends Track<? extends Timed>> tracks) {
        for (Track<? extends Timed> track : tracks) {
            track.lockForRead();

            try {
                for (Timed fix : track.getRawFixes()) {
                    updated(fix.getTimePoint());
                }
            } finally {
                track.unlockAfterRead();
            }
        }
    }

    /**
     * Object serialization obtains a read lock for the course so that in cannot change while serializing this object.
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        // obtain the course's read lock because a course change during serialization could lead to
        // trackedLegs being inconsistent with getRace().getCourse().getLegs()
        getRace().getCourse().lockForRead();
        try {
            LockUtil.lockForWrite(getSerializationLock());
            try {
                s.defaultWriteObject();
            } finally {
                LockUtil.unlockAfterWrite(getSerializationLock());
            }
        } finally {
            getRace().getCourse().unlockAfterRead();
        }
    }

    /**
     * Deserialization has to be maintained in lock-step with {@link #writeObject(ObjectOutputStream) serialization}.
     * When de-serializing, a possibly remote {@link #windStore} is ignored because it is transient. Instead, an
     * {@link EmptyWindStore} is used for the de-serialized instance.
     */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException, PatchFailedException {
        ois.defaultReadObject();
        getRace().getCourse().addCourseListener(this);
        raceStates = new WeakHashMap<>();
        attachedRaceLogs = new ConcurrentHashMap<>();
        attachedRegattaLogs = new ConcurrentHashMap<>();
        attachedRaceExecutionOrderProviders = new ConcurrentHashMap<>();
        markPassingsTimes = new ArrayList<Pair<Waypoint, Pair<TimePoint, TimePoint>>>();
        // The short time wind cache needs to be there before operations such as maneuver recalculation try to access it
        shortTimeWindCache = new ShortTimeWindCache(this, millisecondsOverWhichToAverageWind / 2);
        cacheInvalidationTimerLock = new Object();
        windStore = EmptyWindStore.INSTANCE;
        competitorRankings = createCompetitorRankingsCache();
        competitorRankingsLocks = createCompetitorRankingsLockMap();
        directionFromStartToNextMarkCache = new ConcurrentHashMap<>();
        crossTrackErrorCache = new CrossTrackErrorCache(this);
        crossTrackErrorCache.invalidate();
        maneuverCache = createManeuverCache();
        // considering the unlikely possibility that the course and this tracked race's internal structures
        // may be inconsistent, e.g., due to non-atomic serialization of course and tracked race; see bug 2223
        adjustStructureToCourse();
        triggerManeuverCacheRecalculationForAllCompetitors();
        logger.info("Deserialized race " + getRace().getName());
    }

    /**
     * When the {@link TrackedRace} object and the {@link RaceDefinition} and in particular its {@link CourseImpl} objects are not
     * atomically serialized, inconsistencies may occur during de-serialization. In particular, the tracked race's leg-oriented
     * structures may not consistently reflect the course's leg sequence because a course update could have happened between
     * course serialization and tracked race serialization.<p>
     * 
     * To fix this, the list of waypoints as found in this tracked race's leg-oriented structures, compared to the course's
     * waypoint list, produces a patch that can be applied to this tracked race, resulting in the necessary
     * {@link #waypointAdded(int, Waypoint)} and {@link #waypointRemoved(int, Waypoint)} calls.
     */
    private void adjustStructureToCourse() throws PatchFailedException {
        final TrackedRaceAsWaypointList trackedRaceAsWaypointList = new TrackedRaceAsWaypointList(this);
        Patch<Waypoint> diff = DiffUtils.diff(trackedRaceAsWaypointList, getRace().getCourse().getWaypoints());
        if (!diff.isEmpty()) {
            logger.warning("Found inconsistency between race's course ("+getRace().getCourse()+
                    ") and TrackedRace's structures in "+this+"; fixing");
        }
        diff.applyToInPlace(trackedRaceAsWaypointList);
    }

    @Override
    public synchronized void waitUntilLoadingFromWindStoreComplete() throws InterruptedException {
        while (loadingFromWindStoreState != LoadingFromStoresState.FINISHED) {
            wait();
        }
    }

    @Override
    public synchronized void waitForLoadingToFinish() throws InterruptedException {
    }

    private SmartFutureCache<Competitor, com.sap.sse.common.Util.Triple<TimePoint, TimePoint, List<Maneuver>>, EmptyUpdateInterval> createManeuverCache() {
        return new SmartFutureCache<Competitor, com.sap.sse.common.Util.Triple<TimePoint, TimePoint, List<Maneuver>>, EmptyUpdateInterval>(
                new AbstractCacheUpdater<Competitor, com.sap.sse.common.Util.Triple<TimePoint, TimePoint, List<Maneuver>>, EmptyUpdateInterval>() {
                    
                    @Override
                    public com.sap.sse.common.Util.Triple<TimePoint, TimePoint, List<Maneuver>> computeCacheUpdate(Competitor competitor,
                            EmptyUpdateInterval updateInterval) throws NoWindException {
                        return computeManeuvers(competitor);
                    }
                }, /* nameForLocks */"Maneuver cache for race " + getRace().getName());
    }
    
    /**
     * Precondition: race has already been set, e.g., in constructor before this method is called
     */
    abstract protected TrackedLeg createTrackedLeg(Leg leg);

    public RegattaAndRaceIdentifier getRaceIdentifier() {
        return new RegattaNameAndRaceName(getTrackedRegatta().getRegatta().getName(), getRace().getName());
    }

    @Override
    public NavigableSet<MarkPassing> getMarkPassings(Competitor competitor) {
        return getMarkPassings(competitor, /* waitForLatestUpdates */ false);
    }
    
    @Override
    public NavigableSet<MarkPassing> getMarkPassings(Competitor competitor, boolean waitForLatestUpdates) {
        if (waitForLatestUpdates && markPassingCalculator != null) {
            markPassingCalculator.lockForRead();
        }
        try {
            return markPassingsForCompetitor.get(competitor);
        } finally {
            if (waitForLatestUpdates && markPassingCalculator != null) {
                markPassingCalculator.unlockForRead();
            }
        }
    }

    protected NavigableSet<MarkPassing> getMarkPassingsInOrderAsNavigableSet(Waypoint waypoint) {
        return markPassingsForWaypoint.get(waypoint);
    }

    @Override
    public WindStore getWindStore() {
        return windStore;
    }

    @Override
    public NavigableSet<MarkPassing> getMarkPassingsInOrder(Waypoint waypoint) {
        return getMarkPassingsInOrderAsNavigableSet(waypoint);
    }

    protected NavigableSet<MarkPassing> getOrCreateMarkPassingsInOrderAsNavigableSet(Waypoint waypoint) {
        NavigableSet<MarkPassing> result = getMarkPassingsInOrderAsNavigableSet(waypoint);
        if (result == null) {
            result = createMarkPassingsCollectionForWaypoint(waypoint);
        }
        return result;
    }

    protected NavigableSet<MarkPassing> createMarkPassingsCollectionForWaypoint(Waypoint waypoint) {
        final ConcurrentSkipListSet<MarkPassing> result = new ConcurrentSkipListSet<MarkPassing>(
                MarkPassingsByTimeAndCompetitorIdComparator.INSTANCE);
        LockUtil.lockForRead(getSerializationLock());
        try {
            markPassingsForWaypoint.put(waypoint, result);
        } finally {
            LockUtil.unlockAfterRead(getSerializationLock());
        }
        return result;
    }

    @Override
    public TimePoint getStartOfTracking() {
        return startOfTracking;
    }
    
    /**
     * Monitor object to synchronize access to the {@link #updateStartAndEndOfTracking(boolean)} method. See bug 3922.
     */
    private final Serializable updateStartAndEndOfTrackingMonitor = "updateStartAndEndOfTrackingMonitor";
    
    /**
     * Updates the start and end of tracking in the following precedence order:
     * 
     * <ol>
     * <li>start/end of tracking in Racelog</li>
     * <li>manually set start/end of tracking via {@link #setStartOfTrackingReceived(TimePoint, boolean)} and {@link #setEndOfTrackingReceived(TimePoint, boolean)}</li>
     * <li>start/end of race in Racelog -/+ {@link #START_TRACKING_THIS_MUCH_BEFORE_RACE_START}/{@link #STOP_TRACKING_THIS_MUCH_AFTER_RACE_FINISH}</li>
     * </ol>
     */
    public void updateStartAndEndOfTracking(boolean waitForGPSFixesToLoad) {
        final TimePoint oldStartOfTracking;
        final TimePoint oldEndOfTracking;
        synchronized (updateStartAndEndOfTrackingMonitor) {
            final Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog> trackingTimesFromRaceLog = this.getTrackingTimesFromRaceLogs();
            oldStartOfTracking = getStartOfTracking();
            oldEndOfTracking = getEndOfTracking();
            boolean startOfTrackingFound = false;
            boolean endOfTrackingFound = false;
            // check race log
            if (trackingTimesFromRaceLog != null) {
                if (trackingTimesFromRaceLog.getA() != null) {
                    startOfTracking = trackingTimesFromRaceLog.getA().getTimePoint();
                    startOfTrackingFound = true;
                }
                if (trackingTimesFromRaceLog.getB() != null) {
                    endOfTracking = trackingTimesFromRaceLog.getB().getTimePoint();
                    endOfTrackingFound = true;
                }
            }
            // check "received" variants coming from a connector directly
            if (!startOfTrackingFound || !endOfTrackingFound) {
                if (startOfTrackingReceived != null && !startOfTrackingFound) {
                    startOfTrackingFound = true;
                    startOfTracking = startOfTrackingReceived;
                }
                if (endOfTrackingReceived != null && !endOfTrackingFound) {
                    endOfTrackingFound = true;
                    endOfTracking = endOfTrackingReceived;
                }
            }
            // check for start/finished times in race log and add a few minutes on the ends
            if (!startOfTrackingFound || !endOfTrackingFound) {
                if (!startOfTrackingFound && getStartOfRace() != null) {
                    startOfTracking = getStartOfRace().minus(START_TRACKING_THIS_MUCH_BEFORE_RACE_START);
                    startOfTrackingFound = true;
                }
                if (!endOfTrackingFound && getFinishedTime() != null) {
                    endOfTracking = getFinishedTime().plus(STOP_TRACKING_THIS_MUCH_AFTER_RACE_FINISH);
                    endOfTrackingFound = true;
                }
            }
        }
        startOfTrackingChanged(oldStartOfTracking, waitForGPSFixesToLoad);
        endOfTrackingChanged(oldEndOfTracking, waitForGPSFixesToLoad);
    }

    @Override
    public TimePoint getEndOfTracking() {
        return endOfTracking;
    }

    public void invalidateStartTime() {
        updateStartOfRaceCacheFields();
        updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
    }

    public void invalidateEndTime() {
        endTime = null;
        updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
    }

    protected void invalidateMarkPassingTimes() {
        synchronized (markPassingsTimes) {
            markPassingsTimes.clear();
        }
        updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
    }
    
    /**
     * The race log supports the event types {@link RaceLogStartOfTrackingEvent} and
     * {@link RaceLogEndOfTrackingEvent}. These are to take precedence over any other start/end of
     * tracking specification (see bug 3196). This method uses the {@link TrackingTimesFinder} to
     * analyze all {@link #attachedRaceLogs race logs attached} to find tracking times specifications.
     * If no tracking times specification is found at all, <code>null</code> is returned. Note that
     * even when a valid pair is returned, the components may be <code>null</code>. This may either
     * indicate that no event for that part of the tracking interval was found, or that an event
     * was found that explicitly specified {@code null} to force an open interval on that end.
     */
    @Override
    public Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog> getTrackingTimesFromRaceLogs() {
        for (final RaceLog raceLog : attachedRaceLogs.values()) {
            Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog> result = new TrackingTimesFinder(raceLog).analyze();
            if (result != null) {
                return result;
            }
        }
        return null;
    }
    
    @Override
    public Pair<TimePoint, TimePoint> getStartAndFinishedTimeFromRaceLogs() {
        //Only one of the RaceLogs should have valid data, so one doesn't have to compare all the 
        //start/finished time values in order to find the earliest startTime/latestFinishedTime
        for (final RaceLog raceLog : attachedRaceLogs.values()) {
            TimePoint startTime = new StartTimeFinder(raceLogResolver, raceLog).analyze().getStartTime();
            TimePoint finishedTime = new FinishedTimeFinder(raceLog).analyze();
            if (startTime != null || finishedTime != null){
                return new Util.Pair<TimePoint, TimePoint>(startTime, finishedTime);
            }
        }
        return null;        
    }

    /**
     * Calculates the start time of the race from various sources. The highest precedence take the {@link #attachedRaceLogs race logs},
     * followed by the field {@link #startTimeReceived} which can explicitly be set using {@link #setStartTimeReceived(TimePoint)}.
     * If that does not provide any start time either, a start time is attempted to be inferred from the time points
     * of the start mark passing events.
     */
    @Override
    public TimePoint getStartOfRace() {
        return getStartOfRace(/* inferred */ true);
    }

    @Override
    public TimePoint getStartOfRace(boolean inferred) {
        final TimePoint result;
        if (inferred) {
            result = startTime;
        } else {
            result = startTimeWithoutInferenceFromStartMarkPassings;
        }
        return result;
    }

    /**
     * monitor for {@link #updateStartOfRaceCacheFields()}; has to be serializable, therefore {@link String}
     * and not {@link Object}.
     */
    private final String updateStartOfRaceCacheFieldsMonitor = "";
    protected void updateStartOfRaceCacheFields() {
        synchronized (updateStartOfRaceCacheFieldsMonitor) {
            TimePoint newStartTime = null;
            TimePoint newStartTimeWithoutInferenceFromStartMarkPassings = null;
            for (RaceLog raceLog : attachedRaceLogs.values()) {
                logger.finest(()->"Analyzing race log "+raceLog+" for race "+this.getRace().getName());
                newStartTime = new StartTimeFinder(raceLogResolver, raceLog).analyze().getStartTime();
                if (newStartTime != null) {
                    newStartTimeWithoutInferenceFromStartMarkPassings = newStartTime;
                    final TimePoint finalNewStartTime = newStartTime;
                    logger.finest(()->"Found start time "+finalNewStartTime+" in race log "+raceLog+" for race "+this.getRace().getName());
                    break;
                }
            }
            if (newStartTime == null) {
                logger.finest(()->"No start time found in race logs for race "+getRace().getName());
                newStartTime = getStartTimeReceived();
                if (newStartTime != null) {
                    newStartTimeWithoutInferenceFromStartMarkPassings = newStartTime;
                }
                // If not null, check if the first mark passing for the start line is too much after the
                // startTimeReceived; if so, return an adjusted, later start time.
                // If no official start time was received, try to estimate the start time using the mark passings for
                // the start line.
                final Waypoint firstWaypoint;
                if (getTrackedRegatta().getRegatta().useStartTimeInference() && (firstWaypoint = getRace().getCourse().getFirstWaypoint()) != null) {
                    // in this "if" branch update only startTime, not startTimeWithoutInferenceFromStartMarkPassings
                    if (startTimeReceived != null) {
                        // plausibility check for start time received, based on start mark passings; if no boat started within
                        // a grace period of MAX_TIME_BETWEEN_START_AND_FIRST_MARK_PASSING_IN_MILLISECONDS after the start time
                        // received then the startTimeReceived is believed to be wrong
                        TimePoint timeOfFirstMarkPassing = getFirstPassingTime(firstWaypoint);
                        if (timeOfFirstMarkPassing != null) {
                            long startTimeReceived2timeOfFirstMarkPassingFirstMark = timeOfFirstMarkPassing.asMillis()
                                    - startTimeReceived.asMillis();
                            if (startTimeReceived2timeOfFirstMarkPassingFirstMark > MAX_TIME_BETWEEN_START_AND_FIRST_MARK_PASSING_IN_MILLISECONDS) {
                                newStartTime = new MillisecondsTimePoint(timeOfFirstMarkPassing.asMillis()
                                        - MAX_TIME_BETWEEN_START_AND_FIRST_MARK_PASSING_IN_MILLISECONDS);
                                final TimePoint finalNewStartTime = newStartTime;
                                logger.finest(()->"Using start mark passings for start time of race "+this.getRace().getName()+": "+finalNewStartTime);
                            } else {
                                newStartTime = startTimeReceived;
                                final TimePoint finalNewStartTime = newStartTime;
                                logger.finest(()->"Using start mark received for race "+this.getRace().getName()+": "+finalNewStartTime);
                            }
                        }
                    } else {
                        final NavigableSet<MarkPassing> markPassingsForFirstWaypointInOrder = getMarkPassingsInOrderAsNavigableSet(firstWaypoint);
                        if (markPassingsForFirstWaypointInOrder != null) {
                            newStartTime = calculateStartOfRaceFromMarkPassings(markPassingsForFirstWaypointInOrder,
                                    getRace().getCompetitors());
                            if (newStartTime != null && logger.isLoggable(Level.FINEST)) {
                                logger.finest("Using start mark passings for start time of race "+this.getRace().getName()+": "+newStartTime);
                            }
                        }
                    }
                }
            }
            startTime = newStartTime;
            startTimeWithoutInferenceFromStartMarkPassings = newStartTimeWithoutInferenceFromStartMarkPassings;
        }
    }

    /**
     * Calculates the end time of the race from the mark passings of the last course waypoint
     */
    @Override
    public TimePoint getEndOfRace() {
        if (endTime == null) {
            endTime = getLastPassingOfFinishLine();
        }
        return endTime;
    }

    @Override
    public TimePoint getFinishedTime() {
        return finishedTime;
    }
    
    protected void setFinishedTime(final TimePoint newFinishedTime) {
        finishedTime = newFinishedTime;
    }
    
    private TimePoint getLastPassingOfFinishLine() {
        TimePoint passingTime = null;
        final Waypoint lastWaypoint = getRace().getCourse().getLastWaypoint();
        if (lastWaypoint != null) {
            NavigableSet<MarkPassing> markPassingsInOrder = getMarkPassingsInOrder(lastWaypoint);
            if (markPassingsInOrder != null) {
                lockForRead(markPassingsInOrder);
                try {
                    final MarkPassing last = markPassingsInOrder.isEmpty() ? null : markPassingsInOrder.last();
                    if (last != null) {
                        passingTime = last.getTimePoint();
                    }
                } finally {
                    unlockAfterRead(markPassingsInOrder);
                }
            }
        }
        return passingTime;
    }

    private TimePoint getFirstPassingTime(Waypoint waypoint) {
        NavigableSet<MarkPassing> markPassingsInOrder = getMarkPassingsInOrderAsNavigableSet(waypoint);
        MarkPassing firstMarkPassing = null;
        if (markPassingsInOrder != null) {
            lockForRead(markPassingsInOrder);
            try {
                if (!markPassingsInOrder.isEmpty()) {
                    firstMarkPassing = markPassingsInOrder.first();
                }
            } finally {
                unlockAfterRead(markPassingsInOrder);
            }
        }
        TimePoint timeOfFirstMarkPassing = null;
        if (firstMarkPassing != null) {
            timeOfFirstMarkPassing = firstMarkPassing.getTimePoint();
        }
        return timeOfFirstMarkPassing;
    }

    /**
     * Determines the largest group of competitors that started within a one-minute time period and returns the time
     * point of the earliest start mark passing within that group.
     */
    private TimePoint calculateStartOfRaceFromMarkPassings(NavigableSet<MarkPassing> markPassings,
            Iterable<Competitor> competitors) {
        TimePoint startOfRace = null;
        // Find the first mark passing within the largest cluster crossing the line within one minute.
        lockForRead(markPassings);
        try {
            if (markPassings != null) {
                int largestStartGroupWithinOneMinuteSize = 0;
                MarkPassing startOfLargestGroupSoFar = null;
                int candiateGroupSize = 0;
                MarkPassing candidateForStartOfLargestGroupSoFar = null;
                Iterator<MarkPassing> iterator = markPassings.iterator();
                // sweep over all start mark passings and for each element find the number of competitors that passed
                // the start up to one minute later;
                // pick the start mark passing of the competitor leading the largest such group
                while (iterator.hasNext()) {
                    MarkPassing currentMarkPassing = iterator.next();
                    if (candidateForStartOfLargestGroupSoFar == null) {
                        // first start mark passing
                        candidateForStartOfLargestGroupSoFar = currentMarkPassing;
                        candiateGroupSize = 1;
                        startOfLargestGroupSoFar = currentMarkPassing;
                        largestStartGroupWithinOneMinuteSize = 1;
                    } else {
                        if (candidateForStartOfLargestGroupSoFar.getTimePoint().until(currentMarkPassing.getTimePoint()).compareTo(Duration.ONE_MINUTE) <= 0) {
                            // currentMarkPassing is within one minute of candidateForStartOfLargestGroupSoFar; extend
                            // candidate group...
                            candiateGroupSize++;
                            if (candiateGroupSize > largestStartGroupWithinOneMinuteSize) {
                                // ...and remember as best fit if greater than largest group so far
                                startOfLargestGroupSoFar = candidateForStartOfLargestGroupSoFar;
                                largestStartGroupWithinOneMinuteSize = candiateGroupSize;
                            }
                        } else {
                            // currentMarkPassing is more than a minute after candidateForStartOfLargestGroupSoFar;
                            // advance candidateForStartOfLargestGroupSoFar and reduce group size counter, until
                            // candidateForStartOfLargestGroupSoFar is again within the one-minute interval; may catch
                            // up all the way to currentMarkPassing if that was more than a minute after its predecessor
                            while (candidateForStartOfLargestGroupSoFar.getTimePoint().until(currentMarkPassing.getTimePoint()).compareTo(Duration.ONE_MINUTE) > 0) {
                                candidateForStartOfLargestGroupSoFar = markPassings
                                        .higher(candidateForStartOfLargestGroupSoFar);
                                candiateGroupSize--;
                            }
                        }
                    }
                }
                startOfRace = startOfLargestGroupSoFar == null ? null : startOfLargestGroupSoFar.getTimePoint();
            }
        } finally {
            unlockAfterRead(markPassings);
        }
        return startOfRace;
    }

    @Override
    public boolean hasStarted(TimePoint at) {
        return getStartOfRace() != null && getStartOfRace().compareTo(at) <= 0;
    }
    
    @Override
    public boolean isLive(TimePoint at) {
        final TimePoint startOfLivePeriod;
        final TimePoint endOfLivePeriod;
        if (!hasGPSData() || !hasWindData()) {
            startOfLivePeriod = null;
            endOfLivePeriod = null;
        } else {
            if (getStartOfRace() == null) {
                startOfLivePeriod = getStartOfTracking();
            } else {
                startOfLivePeriod = getStartOfRace().minus(TimingConstants.PRE_START_PHASE_DURATION_IN_MILLIS);
            }
            if (getEndOfRace() == null) {
                if (getTimePointOfNewestEvent() != null) {
                    endOfLivePeriod = getTimePointOfNewestEvent().plus(
                            TimingConstants.IS_LIVE_GRACE_PERIOD_IN_MILLIS).plus(getDelayToLiveInMillis());
                } else {
                    endOfLivePeriod = null;
                }
            } else {
                endOfLivePeriod = getEndOfRace().plus(TimingConstants.IS_LIVE_GRACE_PERIOD_IN_MILLIS);
            }
        }
    
        // if an empty timepoint is given then take the start of the race
        if (at == null) {
            at = startOfLivePeriod.plus(1);
        }
        
        // whenLastTrackedRaceWasLive is null if there is no tracked race for fleet, or the tracked race hasn't started yet at the server time
        // when this DTO was assembled, or there were no GPS or wind data
        final boolean result =
                startOfLivePeriod != null &&
                endOfLivePeriod != null &&
                !startOfLivePeriod.after(at) &&
                !at.after(endOfLivePeriod);
        return result;
    }

    @Override
    public RaceDefinition getRace() {
        return race;
    }

    @Override
    public Iterable<TrackedLeg> getTrackedLegs() {
        // ensure that no course modification is carried out while copying the tracked legs
        getRace().getCourse().lockForRead();
        try {
            return new ArrayList<TrackedLeg>(trackedLegs.values());
        } finally {
            getRace().getCourse().unlockAfterRead();
        }
    }

    @Override
    public Iterable<Pair<Waypoint, Pair<TimePoint, TimePoint>>> getMarkPassingsTimes() {
        getRace().getCourse().lockForRead(); // ensure the list of waypoints doesn't change while we're updating the
                                             // markPassingTimes structure
        try {
            synchronized (markPassingsTimes) {
                if (markPassingsTimes.isEmpty()) {
                    // Remark: sometimes it can happen that a mark passing with a wrong time stamp breaks the right time
                    // order of the waypoint times
                    Date previousLegPassingTime = null;
                    for (Waypoint waypoint : getRace().getCourse().getWaypoints()) {
                        TimePoint firstPassingTime = null;
                        TimePoint lastPassingTime = null;
                        NavigableSet<MarkPassing> markPassings = getMarkPassingsInOrderAsNavigableSet(waypoint);
                        if (markPassings != null && !markPassings.isEmpty()) {
                            // ensure the leg times are in the right time order; there may perhaps be left-overs for
                            // marks to be reached later that
                            // claim it has been passed in the past which may have been an accidental tracker
                            // read-out;
                            // the results of getMarkPassingsInOrder(to) has by definition an ascending time-point
                            // ordering
                            lockForRead(markPassings);
                            try {
                                for (MarkPassing currentMarkPassing : markPassings) {
                                    Date currentPassingDate = currentMarkPassing.getTimePoint().asDate();
                                    if (previousLegPassingTime == null
                                            || currentPassingDate.after(previousLegPassingTime)) {
                                        firstPassingTime = currentMarkPassing.getTimePoint();
                                        previousLegPassingTime = currentPassingDate;
                                        break;
                                    }
                                }
                            } finally {
                                unlockAfterRead(markPassings);
                            }
                        }
                        Pair<TimePoint, TimePoint> timesPair = new Pair<TimePoint, TimePoint>(firstPassingTime,
                                lastPassingTime);
                        markPassingsTimes.add(new Pair<Waypoint, Pair<TimePoint, TimePoint>>(waypoint, timesPair));
                    }
                }
                return markPassingsTimes;
            }
        } finally {
            getRace().getCourse().unlockAfterRead();
        }
    }

    @Override
    public Distance getDistanceTraveledIncludingGateStart(Competitor competitor, TimePoint timePoint) {
        return getDistanceTraveled(competitor, timePoint, /* consider gate start */ true);
    }

    @Override
    public Distance getDistanceTraveled(Competitor competitor, TimePoint timePoint) {
        return getDistanceTraveled(competitor, timePoint, /* consider gate start */ false);
    }
    
    private Distance getDistanceTraveled(Competitor competitor, TimePoint timePoint, boolean considerGateStart) {
        final Distance result;
        NavigableSet<MarkPassing> markPassings = getMarkPassings(competitor);
        try {
            lockForRead(markPassings);
            if (markPassings.isEmpty()) {
                result = null;
            } else {
                TimePoint end = timePoint;
                final TrackedLegOfCompetitor trackedLegOfCompetitor;
                if (markPassings.last().getWaypoint() == getRace().getCourse().getLastWaypoint()
                        && timePoint.compareTo(markPassings.last().getTimePoint()) > 0) {
                    // competitor has finished race at or before the requested time point; use time point of crossing the finish line
                    end = markPassings.last().getTimePoint();
                } else {
                    final TimePoint endOfTracking = getEndOfTracking();
                    if ((trackedLegOfCompetitor=getTrackedLeg(competitor, timePoint)) == null ||
                            (endOfTracking != null && !trackedLegOfCompetitor.hasFinishedLeg(endOfTracking)
                            && (timePoint.after(endOfTracking) || getStatus().getStatus() == TrackedRaceStatusEnum.FINISHED))) {
                        // If the race is no longer tracking and hence no more data can be expected, and the competitor
                        // hasn't finished a leg after the requested time point, no valid distance traveled can be determined
                        // for the competitor in this race the the time point requested
                        end = null;
                    }
                }
                if (end == null) {
                    result = null;
                } else {
                    final Distance preResult = getTrack(competitor).getDistanceTraveled(markPassings.first().getTimePoint(), end);
                    if (considerGateStart && preResult != null) {
                        result = preResult.add(getAdditionalGateStartDistance(competitor, timePoint));
                    } else {
                        result = preResult;
                    }
                }
            }
            return result;
        } finally {
            unlockAfterRead(markPassings);
        }
    }

    @Override
    public GPSFixTrack<Competitor, GPSFixMoving> getTrack(Competitor competitor) {
        return tracks.get(competitor);
    }

    @Override
    public TrackedLeg getTrackedLegFinishingAt(Waypoint endOfLeg) {
        getRace().getCourse().lockForRead();
        try {
            int indexOfWaypoint = getRace().getCourse().getIndexOfWaypoint(endOfLeg);
            if (indexOfWaypoint == -1) {
                throw new IllegalArgumentException("Waypoint " + endOfLeg + " not found in " + getRace().getCourse());
            } else if (indexOfWaypoint == 0) {
                throw new IllegalArgumentException("Waypoint " + endOfLeg + " isn't end of any leg in "
                        + getRace().getCourse());
            }
            return trackedLegs.get(race.getCourse().getLegs().get(indexOfWaypoint - 1));
        } finally {
            getRace().getCourse().unlockAfterRead();
        }
    }

    @Override
    public TrackedLeg getTrackedLegStartingAt(Waypoint startOfLeg) {
        getRace().getCourse().lockForRead();
        try {
            int indexOfWaypoint = getRace().getCourse().getIndexOfWaypoint(startOfLeg);
            if (indexOfWaypoint == -1) {
                throw new IllegalArgumentException("Waypoint " + startOfLeg + " not found in " + getRace().getCourse());
            } else if (indexOfWaypoint == getRace().getCourse().getNumberOfWaypoints() - 1) {
                throw new IllegalArgumentException("Waypoint " + startOfLeg + " isn't start of any leg in "
                        + getRace().getCourse());
            }
            return trackedLegs.get(race.getCourse().getLeg(indexOfWaypoint));
        } finally {
            getRace().getCourse().unlockAfterRead();
        }
    }

    @Override
    public TrackedLegOfCompetitor getTrackedLeg(Competitor competitor, TimePoint at) {
        NavigableSet<MarkPassing> roundings = getMarkPassings(competitor);
        lockForRead(roundings);
        try {
            NavigableSet<MarkPassing> localRoundings = new ArrayListNavigableSet<>(roundings.size(),
                    new TimedComparator());
            localRoundings.addAll(roundings);
        } finally {
            unlockAfterRead(roundings);
        }
        TrackedLegOfCompetitor result = null;
        if (roundings != null) {
            TrackedLeg trackedLeg;
            // obtain last waypoint before obtaining mark passings monitor because obtaining the last waypoint
            // obtains the read lock for the course
            final Waypoint lastWaypoint = getRace().getCourse().getLastWaypoint();
            MarkPassing lastBeforeOrAt = roundings.floor(new DummyMarkPassingWithTimePointOnly(at));
            // already finished the race?
            if (lastBeforeOrAt != null) {
                // and not at or after last mark passing
                if (lastWaypoint != lastBeforeOrAt.getWaypoint()) {
                    trackedLeg = getTrackedLegStartingAt(lastBeforeOrAt.getWaypoint());
                } else {
                    // exactly *at* last mark passing?
                    if (!roundings.isEmpty() && at.equals(roundings.last().getTimePoint())) {
                        // exactly at finish line; return last leg
                        trackedLeg = getTrackedLegFinishingAt(lastBeforeOrAt.getWaypoint());
                    } else {
                        // no, then we're after the last mark passing
                        trackedLeg = null;
                    }
                }
            } else {
                // before beginning of race
                trackedLeg = null;
            }
            if (trackedLeg != null) {
                result = trackedLeg.getTrackedLeg(competitor);
            }
        }
        return result;
    }

    public TrackedLeg getTrackedLeg(Leg leg) {
        getRace().getCourse().lockForRead();
        try {
            return trackedLegs.get(leg);
        } finally {
            getRace().getCourse().unlockAfterRead();
        }
    }

    @Override
    public TrackedLegOfCompetitor getTrackedLeg(Competitor competitor, Leg leg) {
        final TrackedLeg trackedLeg = getTrackedLeg(leg);
        return trackedLeg == null ? null : trackedLeg.getTrackedLeg(competitor);
    }

    @Override
    public long getUpdateCount() {
        return updateCount;
    }

    @Override
    public int getRankDifference(Competitor competitor, Leg leg, TimePoint timePoint) {
        int previousRank;
        if (leg == getRace().getCourse().getFirstLeg()) {
            // first leg; report rank difference from 0
            previousRank = 0;
        } else {
            TrackedLeg previousLeg = getTrackedLegFinishingAt(leg.getFrom());
            previousRank = previousLeg.getTrackedLeg(competitor).getRank(timePoint);
        }
        int currentRank = getTrackedLeg(competitor, leg).getRank(timePoint);
        return currentRank - previousRank;
    }

    @Override
    public int getRank(Competitor competitor) throws NoWindException {
        return getRank(competitor, MillisecondsTimePoint.now());
    }

    @Override
    public Competitor getOverallLeader(TimePoint timePoint) {
        return getOverallLeader(timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }
    
    @Override
    public Competitor getOverallLeader(TimePoint timePoint, WindLegTypeAndLegBearingCache cache) {
        Competitor result = null;
        List<Competitor> ranks = getCompetitorsFromBestToWorst(timePoint, cache);
        if (ranks != null && !ranks.isEmpty()) {
            result = ranks.iterator().next();
        }
        return result;
    }

    @Override
    public int getRank(Competitor competitor, TimePoint timePoint) {
        int result;
        final NavigableSet<MarkPassing> markPassings = getMarkPassings(competitor);
        if (markPassings.isEmpty()) {
            result = 0;
        } else {
            final boolean hasMarkPassingAtOrBeforeTimePoint;
            lockForRead(markPassings);
            try {
                hasMarkPassingAtOrBeforeTimePoint = markPassings.floor(new DummyMarkPassingWithTimePointOnly(timePoint)) == null;
            } finally {
                unlockAfterRead(markPassings);
            }
            if (hasMarkPassingAtOrBeforeTimePoint) {
                // no mark passing at or before timePoint; competitor has not started / participated yet
                result = 0;
            } else {
                result = getCompetitorsFromBestToWorst(timePoint).indexOf(competitor) + 1;
            }
        }
        return result;
    }

    @Override
    public List<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint) {
        return getCompetitorsFromBestToWorst(timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    @Override
    public List<Competitor> getCompetitorsFromBestToWorst(TimePoint unadjustedTimePoint, WindLegTypeAndLegBearingCache cache) {
        final TimePoint timePoint;
        // normalize the time point to get cache hits when asking for time points that are later than
        // the last time point affected by any event received for this tracked race
        if (Util.compareToWithNull(unadjustedTimePoint, getTimePointOfNewestEvent(), /* nullIsLess */ true) <= 0) {
            timePoint = unadjustedTimePoint;
        } else {
            timePoint = getTimePointOfNewestEvent();
        }
        NamedReentrantReadWriteLock readWriteLock;
        synchronized (competitorRankingsLocks) {
            readWriteLock = competitorRankingsLocks.get(timePoint);
            if (readWriteLock == null) {
                readWriteLock = new NamedReentrantReadWriteLock("competitor rankings for race " + getRace().getName()
                        + " for time point " + timePoint, /* fair */false);
                competitorRankingsLocks.put(timePoint, readWriteLock);
            }
        }
        List<Competitor> rankedCompetitors;
        synchronized (competitorRankings) {
            rankedCompetitors = competitorRankings.get(timePoint);
        }
        if (rankedCompetitors == null) {
            LockUtil.lockForWrite(readWriteLock);
            try {
                if (rankedCompetitors == null) {
                    rankedCompetitors = competitorRankings.get(timePoint); // try again; maybe a writer released the
                                                                           // write lock after updating the cache
                    if (rankedCompetitors == null) {
                        // RaceRankComparator requires course read lock
                        getRace().getCourse().lockForRead();
                        try {
                            Comparator<Competitor> comparator = getRankingMetric().getRaceRankingComparator(timePoint, cache);
                            rankedCompetitors = new ArrayList<Competitor>();
                            for (Competitor c : getRace().getCompetitors()) {
                                rankedCompetitors.add(c);
                            }
                            Collections.sort(rankedCompetitors, comparator);
                        } finally {
                            getRace().getCourse().unlockAfterRead();
                        }
                        synchronized (competitorRankings) {
                            competitorRankings.put(timePoint, rankedCompetitors);
                        }
                    }
                }
            } finally {
                LockUtil.unlockAfterWrite(readWriteLock);
            }
        }
        return rankedCompetitors;
    }

    @Override
    public Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalysis)
            throws NoWindException {
        return getAverageAbsoluteCrossTrackError(competitor, timePoint, waitForLatestAnalysis, new LeaderboardDTOCalculationReuseCache(timePoint));
    }
    
    @Override
    public Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalysis,
            WindLegTypeAndLegBearingCache cache)
            throws NoWindException {
        NavigableSet<MarkPassing> markPassings = getMarkPassings(competitor);
        TimePoint from = null;
        lockForRead(markPassings);
        try {
            if (markPassings != null && !markPassings.isEmpty()) {
                from = markPassings.iterator().next().getTimePoint();
            }
        } finally {
            unlockAfterRead(markPassings);
        }
        Distance result;
        if (from != null) {
            result = getAverageAbsoluteCrossTrackError(competitor, from, timePoint, /* upwindOnly */true, waitForLatestAnalysis);
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Distance getAverageSignedCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalysis)
            throws NoWindException {
        return getAverageSignedCrossTrackError(competitor, timePoint, waitForLatestAnalysis, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    @Override
    public Distance getAverageSignedCrossTrackError(Competitor competitor, TimePoint timePoint,
            boolean waitForLatestAnalyses, WindLegTypeAndLegBearingCache cache) throws NoWindException {
        NavigableSet<MarkPassing> markPassings = getMarkPassings(competitor);
        TimePoint from = null;
        lockForRead(markPassings);
        try {
            if (markPassings != null && !markPassings.isEmpty()) {
                from = markPassings.iterator().next().getTimePoint();
            }
        } finally {
            unlockAfterRead(markPassings);
        }
        Distance result;
        if (from != null) {
            result = getAverageSignedCrossTrackError(competitor, from, timePoint, /* upwindOnly */true, waitForLatestAnalyses);
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint from, TimePoint to, boolean upwindOnly,
            boolean waitForLatestAnalysis) throws NoWindException {
        Distance result;
        result = crossTrackErrorCache
                .getAverageAbsoluteCrossTrackError(competitor, from, to, upwindOnly, waitForLatestAnalysis);
        return result;
    }

    @Override
    public Distance getAverageSignedCrossTrackError(Competitor competitor, TimePoint from, TimePoint to, boolean upwindOnly,
            boolean waitForLatestAnalysis) throws NoWindException {
        Distance result;
        result = crossTrackErrorCache
                .getAverageSignedCrossTrackError(competitor, from, to, upwindOnly, waitForLatestAnalysis);
        return result;
    }

    @Override
    public Distance getAverageRideHeight(Competitor competitor, TimePoint timePoint) {
        final Distance result;
        BravoFixTrack<Competitor> track = getSensorTrack(competitor, BravoFixTrack.TRACK_NAME);
        final Leg firstLeg;
        final TrackedLegOfCompetitor firstTrackedLeg;
        if (track != null && (firstLeg = getRace().getCourse().getFirstLeg()) != null && (firstTrackedLeg = getTrackedLeg(competitor, firstLeg)).hasStartedLeg(timePoint)) {
            final TrackedLegOfCompetitor lastTrackedLeg = getTrackedLegFinishingAt(getRace().getCourse().getLastWaypoint()).getTrackedLeg(competitor);
            TimePoint endTimePoint = lastTrackedLeg.hasFinishedLeg(timePoint) ? lastTrackedLeg.getFinishTime() : timePoint;
            result = track.getAverageRideHeight(firstTrackedLeg.getStartTime(), endTimePoint);
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public TrackedLegOfCompetitor getCurrentLeg(Competitor competitor, TimePoint timePoint) {
        // If the mark passing that starts a leg happened exactly at timePoint, the MarkPassingByTimeComparator won't consider
        // them equal because 
        NavigableSet<MarkPassing> competitorMarkPassings = markPassingsForCompetitor.get(competitor);
        DummyMarkPassingWithTimePointAndCompetitor markPassingTimePoint = new DummyMarkPassingWithTimePointAndCompetitor(timePoint, competitor);
        TrackedLegOfCompetitor result = null;
        if (!competitorMarkPassings.isEmpty()) {
            final Course course = getRace().getCourse();
            course.lockForRead();
            try {
                MarkPassing lastMarkPassingAtOfBeforeTimePoint = competitorMarkPassings.floor(markPassingTimePoint);
                if (lastMarkPassingAtOfBeforeTimePoint != null) {
                    Waypoint waypointPassedLastAtOrBeforeTimePoint = lastMarkPassingAtOfBeforeTimePoint.getWaypoint();
                    // don't return a leg if competitor has already finished last leg and therefore the race
                    if (waypointPassedLastAtOrBeforeTimePoint != course.getLastWaypoint()) {
                        result = getTrackedLegStartingAt(waypointPassedLastAtOrBeforeTimePoint).getTrackedLeg(competitor);
                    }
                }
            } finally {
                course.unlockAfterRead();
            }
        }
        return result;
    }

    @Override
    public TrackedLeg getCurrentLeg(TimePoint timePoint) {
        Waypoint lastWaypointPassed = null;
        int indexOfLastWaypointPassed = -1;
        for (Map.Entry<Waypoint, NavigableSet<MarkPassing>> entry : markPassingsForWaypoint.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                MarkPassing first = entry.getValue().first();
                // Did the mark passing happen at or before the requested time point?
                if (first.getTimePoint().compareTo(timePoint) <= 0) {
                    int indexOfWaypoint = getRace().getCourse().getIndexOfWaypoint(entry.getKey());
                    if (indexOfWaypoint > indexOfLastWaypointPassed) {
                        indexOfLastWaypointPassed = indexOfWaypoint;
                        lastWaypointPassed = entry.getKey();
                    }
                }
            }
        }
        TrackedLeg result = null;
        if (lastWaypointPassed != null && lastWaypointPassed != getRace().getCourse().getLastWaypoint()) {
            result = getTrackedLegStartingAt(lastWaypointPassed);
        }
        return result;
    }

    @Override
    public int getLastLegStarted(TimePoint timePoint) {
        int result = 0;
        int indexOfLastWaypointPassed = -1;
        int legCount = race.getCourse().getLegs().size();
        for (Map.Entry<Waypoint, NavigableSet<MarkPassing>> entry : markPassingsForWaypoint.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                MarkPassing first = entry.getValue().first();
                // Did the mark passing happen at or before the requested time point?
                if (first.getTimePoint().compareTo(timePoint) <= 0) {
                    int indexOfWaypoint = getRace().getCourse().getIndexOfWaypoint(entry.getKey());
                    if (indexOfWaypoint > indexOfLastWaypointPassed) {
                        indexOfLastWaypointPassed = indexOfWaypoint;
                    }
                }
            }
        }
        if(indexOfLastWaypointPassed >= 0) {
            result = indexOfLastWaypointPassed+1 < legCount ? indexOfLastWaypointPassed+1 : legCount;  
        }
        return result;
    }
    
    @Override
    public MarkPassing getMarkPassing(Competitor competitor, Waypoint waypoint) {
        final Iterable<MarkPassing> markPassings = getMarkPassingsInOrder(waypoint);
        if (markPassings != null) {
            lockForRead(markPassings);
            try {
                for (MarkPassing markPassing : markPassings) {
                    if (markPassing.getCompetitor() == competitor) {
                        return markPassing;
                    }
                }
            } finally {
                unlockAfterRead(markPassings);
            }
        }
        return null;
    }

    /**
     * This method was a synchronization bottleneck when it was using a regular HashMap for {@link #markTracks}. It is
     * frequently used, and the most frequent case is that the <code>get</code> call on {@link #markTracks} succeeds
     * with a non-<code>null</code> result. To improve performance for this case, {@link #markTracks} now is a
     * {@link ConcurrentHashMap} that can be read while writes are going on without locking or synchronization. Only if
     * the <code>get</code> call does not provide a result, the entire procedure is repeated, this time with
     * synchronization to avoid duplicate track creation for the same mark.
     */
    @Override
    public GPSFixTrack<Mark, GPSFix> getOrCreateTrack(Mark mark) {
        return getOrCreateTrack(mark, true);
    }
    
    @Override
    public GPSFixTrack<Mark, GPSFix> getTrack(Mark mark) {
        return getOrCreateTrack(mark, false);
    }
    
    private GPSFixTrack<Mark, GPSFix> getOrCreateTrack(Mark mark, boolean createIfNotExistent) {
        GPSFixTrack<Mark, GPSFix> result = markTracks.get(mark);
        if (result == null) {
            // try again, this time with more expensive synchronization
            synchronized (markTracks) {
                LockUtil.lockForRead(getSerializationLock());
                try {
                    result = markTracks.get(mark);
                    if (result == null && createIfNotExistent) {
                        result = createMarkTrack(mark);
                        markTracks.put(mark, result);
                    }
                } finally {
                    LockUtil.unlockAfterRead(getSerializationLock());
                }
            }
        }
        return result;
    }

    protected DynamicGPSFixTrackImpl<Mark> createMarkTrack(Mark mark) {
        return new DynamicGPSFixTrackImpl<Mark>(mark, millisecondsOverWhichToAverageSpeed);
    }

    @Override
    public Position getApproximatePosition(Waypoint waypoint, TimePoint timePoint, MarkPositionAtTimePointCache markPositionCache) {
        assert timePoint.equals(markPositionCache.getTimePoint());
        assert this == markPositionCache.getTrackedRace();
        Position result = null;
        for (Mark mark : waypoint.getMarks()) {
            Position nextPos = markPositionCache.getEstimatedPosition(mark);
            if (result == null) {
                result = nextPos;
            } else if (nextPos != null) {
                result = result.translateGreatCircle(result.getBearingGreatCircle(nextPos), result.getDistance(nextPos)
                        .scale(0.5));
            }
        }
        return result;
    }

    @Override
    public boolean hasWindData() {
        boolean result = false;
        Course course = getRace().getCourse();
        TimePoint timepoint = getStartOfRace();
        if (timepoint == null) {
            timepoint = getStartOfTracking();
        }
        if (timepoint != null) {
            Position position = null;
            for (Waypoint waypoint : course.getWaypoints()) {
                position = getApproximatePosition(waypoint, timepoint);
                if (position != null) {
                    break;
                }
            }
            // position may be null if no waypoint's position is known; in that case, a "Global" wind value will be looked up
            Wind wind = getWind(position, timepoint);
            if (wind != null) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Checks whether the {@link Wind#getTimePoint()} is in range of start and end {@link TimePoint}s plus extra time
     * for wind recording. If, based on a {@link RaceExecutionOrderProvider}, there is no previous race that takes the
     * wind fix, an extended time range lead (see {@link TrackedRaceImpl#EXTRA_LONG_TIME_BEFORE_START_TO_TRACK_WIND_MILLIS})
     * is used to record wind even a long time before the race start.<p>
     * 
     * A race does not record wind when both, {@link #getStartOfTracking()} and {@link #getStartOfRace()} are <code>null</code>.
     * Wind is not recorded when it is after the later of {@link #getEndOfRace()} and {@link #getEndOfTracking()} and one of the
     * two is not <code>null</code>.
     */
    @Override
    public boolean takesWindFixWithTimePoint(TimePoint timePoint) {
        final Set<TrackedRace> visited = new HashSet<>();
        visited.add(this);
        return takesWindFixWithTimePointRecursively(timePoint, visited);
    }
    
    /**
     * @param visited
     *            used to avoid endless recursion if cyclic predecessor relations are delivered by a
     *            {@link RaceExecutionOrderProvider}
     */
    @Override
    public boolean takesWindFixWithTimePointRecursively(TimePoint windFixTimePoint, Set<TrackedRace> visited) {
        final boolean result;
        final TimePoint earliestStartTimePoint = Util.getEarliestOfTimePoints(getStartOfRace(), getStartOfTracking());
        final TimePoint latestEndTimePoint = Util.getLatestOfTimePoints(getEndOfRace(), getEndOfTracking());
        if (earliestStartTimePoint != null) {
            // first check if the fix meets the criteria set by the latestEndTimePoint: either the latestEndTimePoint is null, meaning an
            // open interval which will continue to accept late wind fixes, or the fix time point is before the latestEndTimePoint plus a grace
            // interval:
            if (latestEndTimePoint == null || windFixTimePoint.minus(TimingConstants.IS_LIVE_GRACE_PERIOD_IN_MILLIS).before(latestEndTimePoint)) {
                // then check, if fix is accepted anyway because it's after earliestStartTimePoint.minus(TIME_BEFORE_START_TO_TRACK_WIND_MILLIS)
                // and before latestEndTimePoint.plus(IS_LIVE_GRACE_PERIOD_IN_MILLIS) or latestEndTimePoint is null. In this case, no expensive
                // recursive check whether previous races take the fix are required.
                if (windFixTimePoint.plus(TIME_BEFORE_START_TO_TRACK_WIND_MILLIS).after(earliestStartTimePoint)) {
                    result = true;
                } else {
                    // if the fix is older than even the extended lead interval would accept, don't accept the fix:
                    if (windFixTimePoint.plus(EXTRA_LONG_TIME_BEFORE_START_TO_TRACK_WIND_MILLIS).before(earliestStartTimePoint)) {
                        result = false;
                    } else {
                        // the fix is in the critical interval between EXTRA_LONG_TIME_BEFORE_START_TO_TRACK_WIND_MILLIS and
                        // TIME_BEFORE_START_TO_TRACK_WIND_MILLIS before the earliestStartTimePoint; the fix shall only be accepted
                        // if no previous race exists that accepts it
                        result = noPreviousRaceTakesWindFixWithTimePoint(windFixTimePoint, visited);
                    }
                }
            } else {
                result = false; // don't accept the fix if it's after the latest end time point plus some grace interval
            }
        } else {
            result = false; // don't accept a fix if we don't have any start time information about the race
        }
        return result;
    }
    
    private boolean noPreviousRaceTakesWindFixWithTimePoint(TimePoint timePoint, Set<TrackedRace> visited) {
        final boolean result;
        Set<TrackedRace> previousRacesInExecutionOrder = getPreviousRacesFromAttachedRaceExecutionOrderProviders();
        if (previousRacesInExecutionOrder == null || !previousRacesInExecutionOrder.stream().filter(tr ->
                        visited.add(tr) && tr.takesWindFixWithTimePointRecursively(timePoint, visited)).findAny().isPresent()) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public Wind getWind(Position p, TimePoint at) {
        return getWind(p, at, getWindSourcesToExclude());
    }

    @Override
    public Wind getWind(Position p, TimePoint at, Set<WindSource> windSourcesToExclude) {
        final WindWithConfidence<Pair<Position, TimePoint>> windWithConfidence = getWindWithConfidence(p, at,
                windSourcesToExclude);
        return windWithConfidence == null ? null : windWithConfidence.getObject();
    }

    @Override
    public WindWithConfidence<Pair<Position, TimePoint>> getWindWithConfidence(Position p, TimePoint at) {
        return getWindWithConfidence(p, at, getWindSourcesToExclude());
    }

    @Override
    public Set<WindSource> getWindSourcesToExclude() {
        return Collections.unmodifiableSet(windSourcesToExclude.keySet());
    }

    @Override
    public void setWindSourcesToExclude(Iterable<? extends WindSource> windSourcesToExclude) {
        Set<WindSource> old = new HashSet<>(getWindSourcesToExclude());
        LockUtil.lockForRead(getSerializationLock());
        try {
            this.windSourcesToExclude.clear();
            for (WindSource windSourceToExclude : windSourcesToExclude) {
                this.windSourcesToExclude.put(windSourceToExclude, this);
            }
        } finally {
            LockUtil.unlockAfterRead(getSerializationLock());
        }
        if (!old.equals(this.windSourcesToExclude)) {
            clearAllCachesExceptManeuvers();
            triggerManeuverCacheRecalculationForAllCompetitors();
        }
    }

    @Override
    public WindWithConfidence<Pair<Position, TimePoint>> getWindWithConfidence(Position p, TimePoint at,
            Set<WindSource> windSourcesToExclude) {
        return shortTimeWindCache.getWindWithConfidence(p, at, windSourcesToExclude);
    }
    
    public WindWithConfidence<Pair<Position, TimePoint>> getWindWithConfidenceUncached(Position p, TimePoint at,
            Iterable<WindSource> windSourcesToExclude) {
        boolean canUseSpeedOfAtLeastOneWindSource = false;
        Weigher<Pair<Position, TimePoint>> weigher = new PositionAndTimePointWeigher(
        /* halfConfidenceAfterMilliseconds */WindTrack.WIND_HALF_CONFIDENCE_TIME_MILLIS, WindTrack.WIND_HALF_CONFIDENCE_DISTANCE);
        ConfidenceBasedWindAverager<Pair<Position, TimePoint>> averager = ConfidenceFactory.INSTANCE
                .createWindAverager(weigher);
        List<WindWithConfidence<Pair<Position, TimePoint>>> windFixesWithConfidences = new ArrayList<WindWithConfidence<Pair<Position, TimePoint>>>();
        for (WindSource windSource : getWindSources()) {
            // TODO consider parallelizing and consider caching
            if (!Util.contains(windSourcesToExclude, windSource)) {
                WindTrack track = getOrCreateWindTrack(windSource);
                WindWithConfidence<Pair<Position, TimePoint>> windWithConfidence = track.getAveragedWindWithConfidence(p, at);
                if (windWithConfidence != null) {
                    windFixesWithConfidences.add(windWithConfidence);
                    canUseSpeedOfAtLeastOneWindSource = canUseSpeedOfAtLeastOneWindSource
                            || windSource.getType().useSpeed();
                }
            }
        }
        HasConfidence<ScalableWind, Wind, Pair<Position, TimePoint>> average = averager.getAverage(
                windFixesWithConfidences, new Pair<Position, TimePoint>(p, at));
        WindWithConfidence<Pair<Position, TimePoint>> result = average == null ? null
                : new WindWithConfidenceImpl<Pair<Position, TimePoint>>(average.getObject(), average.getConfidence(),
                        new Pair<Position, TimePoint>(p, at), canUseSpeedOfAtLeastOneWindSource);
        return result;
    }

    @Override
    public Wind getDirectionFromStartToNextMark(final TimePoint at) {
        Future<Wind> future;
        FutureTask<Wind> newFuture = null;
        future = directionFromStartToNextMarkCache.get(at);
        if (future == null) {
            synchronized (directionFromStartToNextMarkCache) {
                future = directionFromStartToNextMarkCache.get(at);
                if (future == null) {
                    newFuture = new FutureTaskWithTracingGet<Wind>("getDirectionFromStartToNextMark for "+this, new Callable<Wind>() {
                        @Override
                        public Wind call() {
                            Wind result;
                            Leg firstLeg = getRace().getCourse().getFirstLeg();
                            if (firstLeg != null) {
                                Position firstLegEnd = getApproximatePosition(firstLeg.getTo(), at);
                                Position firstLegStart = getApproximatePosition(firstLeg.getFrom(), at);
                                if (firstLegStart != null && firstLegEnd != null) {
                                    result = new WindImpl(firstLegStart, at, new KnotSpeedWithBearingImpl(0.0,
                                            firstLegEnd.getBearingGreatCircle(firstLegStart)));
                                } else {
                                    result = null;
                                }
                            } else {
                                result = null;
                            }
                            return result;
                        }
                    });
                    directionFromStartToNextMarkCache.put(at, newFuture);
                }
            }
        }
        if (newFuture != null) {
            newFuture.run();
            future = newFuture;
        }
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TimePoint getTimePointOfOldestEvent() {
        return timePointOfOldestEvent;
    }

    @Override
    public TimePoint getTimePointOfNewestEvent() {
        return timePointOfNewestEvent;
    }

    @Override
    public TimePoint getTimePointOfLastEvent() {
        return timePointOfLastEvent;
    }

    /**
     * @param timeOfEvent
     *            may be <code>null</code> meaning to only unblock waiters but not update any time points
     */
    protected void updated(TimePoint timeOfEvent) {
        updateCount++;
        clearAllCachesExceptManeuvers();
        if (timeOfEvent != null) {
            if (timePointOfNewestEvent == null || timePointOfNewestEvent.compareTo(timeOfEvent) < 0) {
                timePointOfNewestEvent = timeOfEvent;
            }
            if (timePointOfOldestEvent == null || timePointOfOldestEvent.compareTo(timeOfEvent) > 0) {
                timePointOfOldestEvent = timeOfEvent;
            }
            timePointOfLastEvent = timeOfEvent;
        }
        synchronized (this) {
            notifyAll();
        }
    }

    protected void setStartTimeReceived(TimePoint start) {
        if (!Util.equalsWithNull(start, startTimeReceived)) {
            this.startTimeReceived = start;
            invalidateStartTime();
            invalidateMarkPassingTimes();
        }
    }

    @Override
    public TimePoint getStartTimeReceived() {
        return startTimeReceived;
    }

    protected void setStartOfTrackingReceived(final TimePoint startOfTracking, final boolean waitForGPSFixesToLoad) {
        this.startOfTrackingReceived = startOfTracking;
        updateStartAndEndOfTracking(waitForGPSFixesToLoad);
    }
    
    protected void startOfTrackingChanged(final TimePoint oldStartOfTracking, boolean waitForGPSFixesToLoad) {
    }

    protected void setEndOfTrackingReceived(final TimePoint endOfTracking, final boolean waitForGPSFixesToLoad) {
        this.endOfTrackingReceived = endOfTracking;
        updateStartAndEndOfTracking(waitForGPSFixesToLoad);
    }
    
    protected void endOfTrackingChanged(final TimePoint oldEndOfTracking, boolean waitForGPSFixesToLoad) {
    }

    /**
     * Schedules the clearing of the caches. If a cache clearing is already scheduled, this is a no-op.
     */
    private void clearAllCachesExceptManeuvers() {
        synchronized (cacheInvalidationTimerLock) {
            // TODO bug 3864: schedule a task with a background thread executor instead of affording a new Timer for each race
            if (cacheInvalidationTimer == null) {
                cacheInvalidationTimer = new Timer("Cache invalidation timer for TrackedRaceImpl "
                        + getRace().getName(), /* isDaemon */ true);
                cacheInvalidationTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (cacheInvalidationTimerLock) {
                            cacheInvalidationTimer.cancel();
                            cacheInvalidationTimer = null;
                        }
                        synchronized (competitorRankings) {
                            competitorRankings.clear();
                        }
                        synchronized (competitorRankingsLocks) {
                            competitorRankingsLocks.clear();
                        }
                    }
                }, DELAY_FOR_CACHE_CLEARING_IN_MILLISECONDS);
            }
        }
    }

    @Override
    public synchronized void waitForNextUpdate(int sinceUpdate) throws InterruptedException {
        while (updateCount <= sinceUpdate) {
            wait(); // ...until updated(...) notifies us
        }
    }

    @Override
    public void waypointAdded(int zeroBasedIndex, Waypoint waypointThatGotAdded) {
        logger.info("waypoint at zero-based index "+zeroBasedIndex+" ("+waypointThatGotAdded+") added; updating tracked race "+this+
                "'s data structures...");
        // expecting to hold the course's write lock
        invalidateMarkPassingTimes();
        LockUtil.lockForRead(getSerializationLock());
        try {
            // assuming that getRace().getCourse()'s write lock is held by the current thread
            updateStartToNextMarkCacheInvalidationCacheListenersAfterWaypointAdded(zeroBasedIndex, waypointThatGotAdded);
            getOrCreateMarkPassingsInOrderAsNavigableSet(waypointThatGotAdded);
            for (Mark mark : waypointThatGotAdded.getMarks()) {
                getOrCreateTrack(mark);
            }
            // a waypoint got added; this means that a leg got added as well; but we shouldn't claim we know where
            // in the leg list of the course the leg was added; that's an implementation secret of CourseImpl. So try:
            LinkedHashMap<Leg, TrackedLeg> reorderedTrackedLegs = new LinkedHashMap<Leg, TrackedLeg>();
            List<Leg> newLegs = getRace().getCourse().getLegs();
            for (Leg leg : newLegs) {
                TrackedLeg trackedLeg = trackedLegs.get(leg);
                if (trackedLeg != null) {
                    reorderedTrackedLegs.put(leg, trackedLeg);
                } else {
                    reorderedTrackedLegs.put(leg, createTrackedLeg(leg));
                }
            }
            // now ensure that the iteration order is in sync with the leg iteration order
            trackedLegs.clear();
            for (Map.Entry<Leg, TrackedLeg> entry : reorderedTrackedLegs.entrySet()) {
                trackedLegs.put(entry.getKey(), entry.getValue());
                entry.getValue().waypointsMayHaveChanges();
            }
            updated(/* time point */null); // no maneuver cache invalidation required because we don't yet have mark
            // passings for new waypoint
            logger.info("done updating tracked race "+this+"'s data structures...");
        } finally {
            LockUtil.unlockAfterRead(getSerializationLock());
        }
    }

    private void updateStartToNextMarkCacheInvalidationCacheListenersAfterWaypointAdded(int zeroBasedIndex,
            Waypoint waypointThatGotAdded) {
        if (zeroBasedIndex < 2) {
            // the observing listener on any previous mark will be GCed; we need to ensure
            // that the cache is recomputed
            clearDirectionFromStartToNextMarkCache();
            Iterator<Waypoint> waypointsIter = getRace().getCourse().getWaypoints().iterator();
            waypointsIter.next(); // skip first
            if (waypointsIter.hasNext()) {
                waypointsIter.next(); // skip second
                if (waypointsIter.hasNext()) {
                    Waypoint oldSecond = waypointsIter.next();
                    stopAndRemoveStartToNextMarkCacheInvalidationListener(oldSecond);
                }
            }
        }
        addStartToNextMarkCacheInvalidationListener(waypointThatGotAdded);
    }

    private void clearDirectionFromStartToNextMarkCache() {
        synchronized (directionFromStartToNextMarkCache) {
            directionFromStartToNextMarkCache.clear();
        }
    }

    private void addStartToNextMarkCacheInvalidationListener(Waypoint waypoint) {
        for (Mark mark : waypoint.getMarks()) {
            addStartToNextMarkCacheInvalidationListener(mark);
        }
    }

    private void addStartToNextMarkCacheInvalidationListener(Mark mark) {
        GPSFixTrack<Mark, GPSFix> track = getOrCreateTrack(mark);
        StartToNextMarkCacheInvalidationListener listener = new StartToNextMarkCacheInvalidationListener(track);
        LockUtil.lockForRead(getSerializationLock());
        try {
            startToNextMarkCacheInvalidationListeners.put(mark, listener);
        } finally {
            LockUtil.unlockAfterRead(getSerializationLock());
        }
        track.addListener(listener);
    }

    private void stopAndRemoveStartToNextMarkCacheInvalidationListener(Waypoint waypoint) {
        for (Mark mark : waypoint.getMarks()) {
            stopAndRemoveStartToNextMarkCacheInvalidationListener(mark);
        }
    }

    private void stopAndRemoveStartToNextMarkCacheInvalidationListener(Mark mark) {
        StartToNextMarkCacheInvalidationListener listener = startToNextMarkCacheInvalidationListeners.get(mark);
        if (listener != null) {
            listener.stopListening();
            LockUtil.lockForRead(getSerializationLock());
            try {
                startToNextMarkCacheInvalidationListeners.remove(mark);
            } finally {
                LockUtil.unlockAfterRead(getSerializationLock());
            }
        }
    }

    @Override
    public void waypointRemoved(int zeroBasedIndex, Waypoint waypointThatGotRemoved) {
        logger.info("waypoint at zero-based index "+zeroBasedIndex+" ("+waypointThatGotRemoved+") removed; updating tracked race "+this+
                "'s data structures...");
        // expecting to hold the course's write lock
        invalidateMarkPassingTimes();
        LockUtil.lockForRead(getSerializationLock());
        try {
            // assuming that getRace().getCourse()'s write lock is held by the current thread
            updateStartToNextMarkCacheInvalidationCacheListenersAfterWaypointRemoved(zeroBasedIndex,
                    waypointThatGotRemoved);
            Leg toRemove = null;
            Leg last = null;
            int i = 0;
            for (Map.Entry<Leg, TrackedLeg> e : trackedLegs.entrySet()) {
                last = e.getKey();
                if (i == zeroBasedIndex) {
                    toRemove = e.getKey();
                    break;
                }
                i++;
            }
            if (toRemove == null && !trackedLegs.isEmpty()) {
                // last waypoint removed
                toRemove = last;
            }
            if (toRemove != null) {
                logger.info("Removing tracked leg at zero-based index " + zeroBasedIndex + " from tracked race "
                        + getRace().getName());
                LinkedHashMap<Leg, TrackedLeg> newTrackedLegs = new LinkedHashMap<>();
                for (Map.Entry<Leg, TrackedLeg> trackedLegsEntry : trackedLegs.entrySet()) {
                    if (trackedLegsEntry.getKey() == toRemove) {
                        break;
                    } else {
                        newTrackedLegs.put(trackedLegsEntry.getKey(), trackedLegsEntry.getValue());
                    }
                }
                trackedLegs.clear();
                trackedLegs.putAll(newTrackedLegs);
                List<Leg> newLegs = getRace().getCourse().getLegs();
                for (int j = zeroBasedIndex; j < newLegs.size(); j++) {
                    trackedLegs.put(newLegs.get(j), createTrackedLeg(newLegs.get(j)));
                }
                updated(/* time point */null);
            }
            // remove all corresponding markpassings if a waypoint has been removed
            NavigableSet<MarkPassing> markPassingsRemoved;
            markPassingsRemoved = markPassingsForWaypoint.remove(waypointThatGotRemoved);
            for (NavigableSet<MarkPassing> markPassingsForOneCompetitor : markPassingsForCompetitor.values()) {
                if (!markPassingsForOneCompetitor.isEmpty()) {
                    final Competitor competitor = markPassingsForOneCompetitor.iterator().next().getCompetitor();
                    LockUtil.lockForWrite(getMarkPassingsLock(markPassingsForOneCompetitor));
                    try {
                        markPassingsForOneCompetitor.removeAll(markPassingsRemoved);
                    } finally {
                        LockUtil.unlockAfterWrite(getMarkPassingsLock(markPassingsForOneCompetitor));
                    }
                    triggerManeuverCacheRecalculation(competitor);
                }
            }
            logger.info("done updating tracked race "+this+"'s data structures...");
        } finally {
            LockUtil.unlockAfterRead(getSerializationLock());
        }
    }

    protected NamedReentrantReadWriteLock getMarkPassingsLock(Iterable<MarkPassing> markPassings) {
        final IdentityWrapper<Iterable<MarkPassing>> markPassingsIdentity = new IdentityWrapper<>(markPassings);
        NamedReentrantReadWriteLock lock = locksForMarkPassings.get(markPassingsIdentity);
        if (lock == null) {
            synchronized (locksForMarkPassings) {
                lock = locksForMarkPassings.get(markPassingsIdentity);
                if (lock == null) {
                    lock = new NamedReentrantReadWriteLock(
                            "mark passings lock for tracked race " + getRace().getName(), /* fair */false);
                    locksForMarkPassings.put(markPassingsIdentity, lock);
                }
            }
        }
        return lock;
    }

    private void updateStartToNextMarkCacheInvalidationCacheListenersAfterWaypointRemoved(int zeroBasedIndex,
            Waypoint waypointThatGotRemoved) {
        if (zeroBasedIndex < 2) {
            // the observing listener on any previous mark will be GCed; we need to ensure
            // that the cache is recomputed
            clearDirectionFromStartToNextMarkCache();
            stopAndRemoveStartToNextMarkCacheInvalidationListener(waypointThatGotRemoved);
            Iterator<Waypoint> waypointsIter = getRace().getCourse().getWaypoints().iterator();
            if (waypointsIter.hasNext()) { // catches the case of a course being empty
                waypointsIter.next(); // skip first
                if (waypointsIter.hasNext()) {
                    waypointsIter.next(); // skip second
                    if (waypointsIter.hasNext()) {
                        Waypoint newSecond = waypointsIter.next();
                        addStartToNextMarkCacheInvalidationListener(newSecond);
                    }
                }
            }
        }
    }

    @Override
    public TrackedRegatta getTrackedRegatta() {
        return trackedRegatta;
    }

    @Override
    public Wind getEstimatedWindDirection(TimePoint timePoint) {
        WindWithConfidence<TimePoint> estimatedWindWithConfidence = getEstimatedWindDirectionWithConfidence(timePoint);
        return estimatedWindWithConfidence == null ? null : estimatedWindWithConfidence.getObject();
    }

    @Override
    public WindWithConfidence<TimePoint> getEstimatedWindDirectionWithConfidence(TimePoint timePoint) {
        DummyMarkPassingWithTimePointOnly dummyMarkPassingForNow = new DummyMarkPassingWithTimePointOnly(timePoint);
        Weigher<TimePoint> weigher = ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(
        // use a minimum confidence to avoid the bearing to flip to 270deg in case all is zero
                getMillisecondsOverWhichToAverageSpeed(), /* minimum confidence */0.0000000001);
        Map<LegType, Pair<BearingWithConfidenceCluster<TimePoint>, ScalablePosition>> bearings = clusterBearingsByLegType(
                timePoint, dummyMarkPassingForNow, weigher);
        // use the minimum confidence of the four "quadrants" as the result's confidence
        BearingWithConfidenceImpl<TimePoint> reversedUpwindAverage = null;
        int upwindNumberOfRelevantBoats = 0;
        double confidence = 0;
        BearingWithConfidence<TimePoint> resultBearing = null;
        ScalablePosition scaledPosition = null;
        int numberOfFixesConsideredForScaledPosition = 0;
        Set<WindSource> estimationExcluded = new HashSet<>();
        estimationExcluded.addAll(getWindSources(WindSourceType.TRACK_BASED_ESTIMATION));
        estimationExcluded.addAll(getWindSources(WindSourceType.COURSE_BASED));
        if (bearings != null) {
            int numberOfFixesUpwind = bearings.get(LegType.UPWIND).getA().size();
            if (numberOfFixesUpwind > 0) {
                ScalablePosition upwindPosition = bearings.get(LegType.UPWIND).getB();
                Pair<Double, Double> minimumAngleBetweenDifferentTacksUpwindWithConfidence = getMinimumAngleBetweenDifferentTacksUpwind(getWind(
                        upwindPosition.divide(numberOfFixesUpwind), timePoint, estimationExcluded));
                BearingWithConfidenceCluster<TimePoint>[] bearingClustersUpwind = bearings
                        .get(LegType.UPWIND)
                        .getA()
                        .splitInTwo(
                                minimumAngleBetweenDifferentTacksUpwindWithConfidence.getA(),
                                timePoint);
                if (!bearingClustersUpwind[0].isEmpty() && !bearingClustersUpwind[1].isEmpty()) {
                    BearingWithConfidence<TimePoint> average0 = bearingClustersUpwind[0].getAverage(timePoint);
                    BearingWithConfidence<TimePoint> average1 = bearingClustersUpwind[1].getAverage(timePoint);
                    upwindNumberOfRelevantBoats = Math.min(bearingClustersUpwind[0].size(),
                            bearingClustersUpwind[1].size());
                    confidence = Math.min(average0.getConfidence(), average1.getConfidence())
                            * getRace().getBoatClass().getUpwindWindEstimationConfidence(upwindNumberOfRelevantBoats)
                            * minimumAngleBetweenDifferentTacksUpwindWithConfidence.getB();
                    reversedUpwindAverage = new BearingWithConfidenceImpl<TimePoint>(average0.getObject()
                            .middle(average1.getObject()).reverse(), confidence, timePoint);
                    scaledPosition = upwindPosition;
                    numberOfFixesConsideredForScaledPosition += bearings.get(LegType.UPWIND).getA().size();
                }
            }
            BearingWithConfidenceImpl<TimePoint> downwindAverage = null;
            int downwindNumberOfRelevantBoats = 0;
            int numberOfFixesDownwind = bearings.get(LegType.DOWNWIND).getA().size();
            if (numberOfFixesDownwind > 0) {
                ScalablePosition downwindPosition = bearings.get(LegType.DOWNWIND).getB();
                Pair<Double, Double> minimumAngleBetweenDifferentTacksDownwindWithConfidence = getMinimumAngleBetweenDifferentTacksDownwind(getWind(
                        downwindPosition.divide(numberOfFixesDownwind), timePoint, estimationExcluded));
                BearingWithConfidenceCluster<TimePoint>[] bearingClustersDownwind = bearings
                        .get(LegType.DOWNWIND)
                        .getA()
                        .splitInTwo(
                                minimumAngleBetweenDifferentTacksDownwindWithConfidence.getA(),
                                timePoint);
                if (!bearingClustersDownwind[0].isEmpty() && !bearingClustersDownwind[1].isEmpty()) {
                    BearingWithConfidence<TimePoint> average0 = bearingClustersDownwind[0].getAverage(timePoint);
                    BearingWithConfidence<TimePoint> average1 = bearingClustersDownwind[1].getAverage(timePoint);
                    downwindNumberOfRelevantBoats = Math.min(bearingClustersDownwind[0].size(),
                            bearingClustersDownwind[1].size());
                    confidence = Math.min(average0.getConfidence(), average1.getConfidence())
                            * getRace().getBoatClass().getDownwindWindEstimationConfidence(
                                    downwindNumberOfRelevantBoats)
                            * minimumAngleBetweenDifferentTacksDownwindWithConfidence.getB();
                    downwindAverage = new BearingWithConfidenceImpl<TimePoint>(average0.getObject().middle(
                            average1.getObject()), confidence, timePoint);
                    if (scaledPosition == null) {
                        scaledPosition = downwindPosition;
                    } else {
                        scaledPosition.add(downwindPosition);
                    }
                    numberOfFixesConsideredForScaledPosition += bearings.get(LegType.DOWNWIND).getA().size();
                }
            }
            BearingWithConfidenceCluster<TimePoint> resultCluster = new BearingWithConfidenceCluster<TimePoint>(weigher);
            assert upwindNumberOfRelevantBoats == 0 || reversedUpwindAverage != null;
            if (upwindNumberOfRelevantBoats > 0) {
                resultCluster.add(reversedUpwindAverage);
            }
            assert downwindNumberOfRelevantBoats == 0 || downwindAverage != null;
            if (downwindNumberOfRelevantBoats > 0) {
                resultCluster.add(downwindAverage);
            }
            resultBearing = resultCluster.getAverage(timePoint);
        }
        final Position position;
        if (scaledPosition == null) {
            position = null;
        } else {
            position = scaledPosition.divide(numberOfFixesConsideredForScaledPosition);
        }
        return resultBearing == null ? null : new WindWithConfidenceImpl<TimePoint>(new WindImpl(position, timePoint,
                new KnotSpeedWithBearingImpl(/* speedInKnots, not to be used */ 0, resultBearing.getObject())),
                resultBearing.getConfidence(), resultBearing.getRelativeTo(), /* useSpeed */false);
    }

    /**
     * Using the competitor tracks, the competitors are clustered into those going upwind and those going downwind at
     * <code>timePoint</code>. The result provides a {@link BearingWithConfidenceCluster} for all leg types, but only
     * those for {@link LegType#UPWIND} and {@link LegType#DOWNWIND} will actually contain values. In addition
     * to the bearing clusters, a {@link ScalablePosition} is returned as the second part of each {@link Pair} returned
     * for each leg type. That is the "sum" of all competitor positions at which a speed/bearing was added to the respective
     * bearing cluster. To obtain an average position for the cluster, the {@link ScalablePosition} can be
     * {@link ScalablePosition#divide(double) divided} by the {@link BearingWithConfidenceCluster#size() size} of
     * the bearing cluster.
     */
    private Map<LegType, Pair<BearingWithConfidenceCluster<TimePoint>, ScalablePosition>> clusterBearingsByLegType(TimePoint timePoint,
            DummyMarkPassingWithTimePointOnly dummyMarkPassingForNow, Weigher<TimePoint> weigher) {
        Weigher<TimePoint> weigherForMarkPassingProximity = new HyperbolicTimeDifferenceWeigher(
                getMillisecondsOverWhichToAverageSpeed() * 5);
        Map<LegType, BearingWithConfidenceCluster<TimePoint>> bearings = new HashMap<>();
        Map<LegType, ScalablePosition> scaledCentersOfGravity = new HashMap<>();
        for (LegType legType : LegType.values()) {
            bearings.put(legType, new BearingWithConfidenceCluster<TimePoint>(weigher));
            scaledCentersOfGravity.put(legType, null);
        }
        Map<TrackedLeg, LegType> legTypesCache = new HashMap<TrackedLeg, LegType>();
        getRace().getCourse().lockForRead(); // ensure the course doesn't change, particularly lose the leg we're
                                             // interested in, while we're running
        try {
            for (Competitor competitor : getRace().getCompetitors()) {
                TrackedLegOfCompetitor leg;
                try {
                    leg = getTrackedLeg(competitor, timePoint);
                } catch (IllegalArgumentException iae) {
                    logger.warning("Caught " + iae + " during wind estimation; ignoring seemingly broken leg");
                    logger.log(Level.SEVERE, "clusterBearingsByLegType", iae);
                    // supposedly, we got a "Waypoint X isn't start of any leg in Y" exception; leg not found
                    leg = null;
                }
                // if bearings was set to null this indicates there was an exception; no need for further calculations,
                // return null
                if (bearings != null && leg != null) {
                    TrackedLeg trackedLeg = getTrackedLeg(leg.getLeg());
                    LegType legType;
                    try {
                        legType = legTypesCache.get(trackedLeg);
                        if (legType == null) {
                            legType = trackedLeg.getLegType(timePoint);
                            legTypesCache.put(trackedLeg, legType);
                        }
                        if (legType != LegType.REACHING) {
                            GPSFixTrack<Competitor, GPSFixMoving> track = getTrack(competitor);
                            if (!track.hasDirectionChange(timePoint,
                                    /* be even more conservative than maneuver detection to really try to get "straight line" behavior */
                                    getManeuverDegreeAngleThreshold()/2.)) {
                                SpeedWithBearingWithConfidence<TimePoint> estimatedSpeedWithConfidence = track
                                        .getEstimatedSpeed(timePoint, weigher);
                                if (estimatedSpeedWithConfidence != null
                                        && estimatedSpeedWithConfidence.getObject() != null &&
                                        // Mark passings may be missing or far off. This can lead to boats apparently
                                        // going "backwards" regarding the leg's direction; ignore those
                                        isNavigatingForward(estimatedSpeedWithConfidence.getObject().getBearing(),
                                                trackedLeg, timePoint)) {
                                    // additionally to generally excluding maneuvers, reduce confidence around mark
                                    // passings:
                                    NavigableSet<MarkPassing> markPassings = getMarkPassings(competitor);
                                    double markPassingProximityConfidenceReduction = 1.0;
                                    lockForRead(markPassings);
                                    try {
                                        NavigableSet<MarkPassing> prevMarkPassing = markPassings.headSet(
                                                dummyMarkPassingForNow, /* inclusive */true);
                                        NavigableSet<MarkPassing> nextMarkPassing = markPassings.tailSet(
                                                dummyMarkPassingForNow, /* inclusive */true);
                                        if (prevMarkPassing != null && !prevMarkPassing.isEmpty()) {
                                            markPassingProximityConfidenceReduction *= Math.max(0.0,
                                                    1.0 - weigherForMarkPassingProximity.getConfidence(prevMarkPassing
                                                            .last().getTimePoint(), timePoint));
                                        }
                                        if (nextMarkPassing != null && !nextMarkPassing.isEmpty()) {
                                            markPassingProximityConfidenceReduction *= Math.max(0.0,
                                                    1.0 - weigherForMarkPassingProximity.getConfidence(nextMarkPassing
                                                            .first().getTimePoint(), timePoint));
                                        }
                                    } finally {
                                        unlockAfterRead(markPassings);
                                    }
                                    BearingWithConfidence<TimePoint> bearing = new BearingWithConfidenceImpl<TimePoint>(
                                            estimatedSpeedWithConfidence.getObject() == null ? null
                                                    : estimatedSpeedWithConfidence.getObject().getBearing(),
                                            markPassingProximityConfidenceReduction
                                                    * estimatedSpeedWithConfidence.getConfidence(),
                                            estimatedSpeedWithConfidence.getRelativeTo());
                                    BearingWithConfidenceCluster<TimePoint> bearingClusterForLegType = bearings.get(legType);
                                    bearingClusterForLegType.add(bearing);
                                    final Position position = track.getEstimatedPosition(timePoint, /* extrapolate */ false);
                                    final ScalablePosition scalablePosition = new ScalablePosition(position);
                                    final ScalablePosition scaledCenterOfGravitySoFar = scaledCentersOfGravity.get(legType);
                                    final ScalablePosition newScaledCenterOfGravity;
                                    if (scaledCenterOfGravitySoFar == null) {
                                        newScaledCenterOfGravity = scalablePosition;
                                    } else {
                                        newScaledCenterOfGravity = scaledCenterOfGravitySoFar.add(scalablePosition);
                                    }
                                    scaledCentersOfGravity.put(legType, newScaledCenterOfGravity);
                                }
                            }
                        }
                    } catch (NoWindException e) {
                        logger.fine("Unable to determine leg type for race " + getRace().getName()
                                + " while trying to estimate wind (Background: I've got a NoWindException)");
                        bearings = null;
                    }
                }
            }
        } finally {
            getRace().getCourse().unlockAfterRead();
        }
        final Map<LegType, Pair<BearingWithConfidenceCluster<TimePoint>, ScalablePosition>> result;
        if (bearings == null) {
            result = null;
        } else {
            result = new HashMap<>();
            for (LegType legType : LegType.values()) {
                result.put(legType, new Pair<>(bearings.get(legType), scaledCentersOfGravity.get(legType)));
            }
        }
        return result;
    }

    /**
     * Checks if the <code>bearing</code> generally moves in the direction that the <code>trackedLeg</code> has at time
     * point <code>at</code>.
     */
    private boolean isNavigatingForward(Bearing bearing, TrackedLeg trackedLeg, TimePoint at) {
        Bearing legBearing = trackedLeg.getLegBearing(at);
        return Math.abs(bearing.getDifferenceTo(legBearing).getDegrees()) < 90;
    }

    /**
     * This is probably best explained by example. If the wind bearing is from port to starboard, the situation looks
     * like this:
     * 
     * <pre>
     *                                 ^
     *                 Wind            | Boat
     *               ----------->      |
     *                                 |
     * 
     * </pre>
     * 
     * In this case, the boat gets the wind from port, so the result has to be {@link Tack#PORT}. The angle between the
     * boat's heading (which we can only approximate by the boat's course over ground) and the wind bearing in this case
     * is 90 degrees. <code>wind.{@link Bearing#getDifferenceTo(Bearing) getDifferenceTo}(boat)</code> in this case will
     * return a bearing representing -90 degrees.
     * <p>
     * 
     * If the wind is blowing the other way, the angle returned by {@link Bearing#getDifferenceTo(Bearing)} will
     * correspond to +90 degrees. In other words, a negative angle means starboard tack, a positive angle represents
     * port tack.
     * <p>
     * 
     * For the unlikely case of 0 degrees difference, {@link Tack#STARBOARD} will result.
     * 
     * @return <code>null</code> in case the boat's bearing cannot be determined for <code>timePoint</code>
     * @throws NoWindException
     */
    @Override
    public Tack getTack(Competitor competitor, TimePoint timePoint) throws NoWindException {
        final SpeedWithBearing estimatedSpeed = getTrack(competitor).getEstimatedSpeed(timePoint);
        Tack result = null;
        if (estimatedSpeed != null) {
            result = getTack(getTrack(competitor).getEstimatedPosition(timePoint, /* extrapolate */false), timePoint,
                    estimatedSpeed.getBearing());
        }
        return result;
    }

    /**
     * This is probably best explained by example. If the wind bearing is from port to starboard, the situation looks
     * like this:
     * 
     * <pre>
     *                                 ^
     *                 Wind            | Boat
     *               ----------->      |
     *                                 |
     * 
     * </pre>
     * 
     * In this case, the boat gets the wind from port, so the result has to be {@link Tack#PORT}. The angle between the
     * boat's heading (which we can only approximate by the boat's course over ground) and the wind bearing in this case
     * is 90 degrees. <code>wind.{@link Bearing#getDifferenceTo(Bearing) getDifferenceTo}(boat)</code> in this case will
     * return a bearing representing -90 degrees.
     * <p>
     * 
     * If the wind is blowing the other way, the angle returned by {@link Bearing#getDifferenceTo(Bearing)} will
     * correspond to +90 degrees. In other words, a negative angle means starboard tack, a positive angle represents
     * port tack.
     * <p>
     * 
     * For the unlikely case of 0 degrees difference, {@link Tack#STARBOARD} will result.
     * 
     * @return <code>null</code> in case the boat's bearing cannot be determined for <code>timePoint</code>
     */
    @Override
    public Tack getTack(SpeedWithBearing estimatedSpeed, Wind wind, TimePoint timePoint) {
        Tack result = null;
        if (estimatedSpeed != null) {
            result = getTack(wind, estimatedSpeed.getBearing());
        }
        return result;
    }

    /**
     * Based on the wind direction at <code>timePoint</code> and at position <code>where</code>, compares the
     * <code>boatBearing</code> to the wind's bearing at that time and place and determined the tack.
     * 
     * @throws NoWindException
     *             in case the wind cannot be determined because without a wind direction, the tack cannot be determined
     *             either
     */
    private Tack getTack(Position where, TimePoint timePoint, Bearing boatBearing) throws NoWindException {
        final Wind wind = getWind(where, timePoint);
        if (wind == null) {
            throw new NoWindException("Can't determine wind direction in position " + where + " at " + timePoint
                    + ", therefore cannot determine tack");
        }
        return getTack(wind, boatBearing);
    }


    /**
     * Based on the wind, compares the <code>boatBearing</code> to the wind's bearing at
     * that time and place and determined the tack.
     */
    private Tack getTack(Wind wind, Bearing boatBearing) {
        Bearing windBearing = wind.getBearing();
        Bearing difference = windBearing.getDifferenceTo(boatBearing);
        return difference.getDegrees() <= 0 ? Tack.PORT : Tack.STARBOARD;
    }

    @Override
    public String toString() {
        return "TrackedRace for " + getRace();
    }

    @Override
    public Iterable<GPSFixMoving> approximate(Competitor competitor, Distance maxDistance, TimePoint from, TimePoint to) {
        DouglasPeucker<Competitor, GPSFixMoving> douglasPeucker = new DouglasPeucker<Competitor, GPSFixMoving>(
                getTrack(competitor));
        return douglasPeucker.approximate(maxDistance, from, to);
    }

    protected void triggerManeuverCacheRecalculationForAllCompetitors() {
        if (cachesSuspended) {
            triggerManeuverCacheInvalidationForAllCompetitors = true;
        } else {
            final List<Competitor> shuffledCompetitors = new ArrayList<>();
            for (Competitor competitor : (getRace().getCompetitors())) {
                shuffledCompetitors.add(competitor);
            }
            Collections.shuffle(shuffledCompetitors);
            for (Competitor competitor : shuffledCompetitors) {
                triggerManeuverCacheRecalculation(competitor);
            }
        }
    }

    protected void triggerManeuverCacheRecalculation(final Competitor competitor) {
        if (cachesSuspended) {
            triggerManeuverCacheInvalidationForAllCompetitors = true;
        } else {
            maneuverCache.triggerUpdate(competitor, /* updateInterval */null);
        }
    }

    private com.sap.sse.common.Util.Triple<TimePoint, TimePoint, List<Maneuver>> computeManeuvers(Competitor competitor) throws NoWindException {
        logger.finest("computeManeuvers(" + competitor.getName() + ") called in tracked race " + this);
        long startedAt = System.currentTimeMillis();
        // compute the maneuvers for competitor
        com.sap.sse.common.Util.Triple<TimePoint, TimePoint, List<Maneuver>> result = null;
        NavigableSet<MarkPassing> markPassings = getMarkPassings(competitor);
        TimePoint extendedFrom = null;
        MarkPassing crossedFinishLine = null;
        // getLastWaypoint() will wait for a read lock on the course; do this outside the synchronized block to avoid
        // deadlocks
        final Waypoint lastWaypoint = getRace().getCourse().getLastWaypoint();
        if (lastWaypoint != null) {
            lockForRead(markPassings);
            try {
                if (markPassings != null && !markPassings.isEmpty()) {
                    extendedFrom = markPassings.iterator().next().getTimePoint();
                    crossedFinishLine = getMarkPassing(competitor, lastWaypoint);
                }
            } finally {
                unlockAfterRead(markPassings);
            }
        }
        if (extendedFrom == null) {
            GPSFixMoving firstRawFix = getTrack(competitor).getFirstRawFix();
            if (firstRawFix != null) {
                extendedFrom = firstRawFix.getTimePoint();
            }
        }
        if (extendedFrom != null) {
            TimePoint extendedTo;
            if (crossedFinishLine != null) {
                extendedTo = crossedFinishLine.getTimePoint();
            } else {
                final GPSFixMoving lastRawFix = getTrack(competitor).getLastRawFix();
                if (lastRawFix != null) {
                    extendedTo = lastRawFix.getTimePoint();
                } else {
                    extendedTo = null;
                }
            }
            if (extendedTo != null) {
                try {
                    List<Maneuver> extendedResultForCache = detectManeuvers(competitor, extendedFrom, extendedTo, /* ignoreMarkPassings */
                            false);
                    result = new com.sap.sse.common.Util.Triple<TimePoint, TimePoint, List<Maneuver>>(extendedFrom,
                            extendedTo, extendedResultForCache);
                } catch (NoWindException ex) {
                    // Catching the NoWindException here without letting it propagate thru other handlers.
                    // This is mainly to avoid having logs flooded with stack traces. It is safe to catch
                    // it here because we can assume that this exception does not hide any severe problem
                    // other than that there is no wind. Because maneuvers are mostly computed using a
                    // future cache and need wind (like getTack) they will often fail before the wind has been
                    // loaded from database. We can safely return null here because we can be sure that
                    // cache will be updated when new fixes are fed into the stream therefore leading to a
                    // recomputation.
                    logger.fine("NoWindException during computation of maneuvers for " + competitor.getName());
                }
            }
        } // else competitor has no fixes to consider; remove any maneuver cache entry
        logger.finest("computeManeuvers(" + competitor.getName() + ") called in tracked race " + this + " took "
                + (System.currentTimeMillis() - startedAt) + "ms");
        return result;
    }
    
    /**
     * @param ignoreMarkPassings
     *            When <code>true</code>, no {@link ManeuverType#MARK_PASSING} maneuvers will be identified, and the
     *            fact that a mark passing would split up what else may be a penalty circle is ignored. This is helpful
     *            for recursive calls, e.g., after identifying a tack and a jibe around a mark passing and trying to
     *            identify for the time before and after the mark passing which maneuvers exist on which side of the
     *            passing.
     * @return a valid but possibly empty list
     * @see #detectManeuvers(Competitor, List, boolean, TimePoint, TimePoint)
     */
    private List<Maneuver> detectManeuvers(Competitor competitor, TimePoint from, TimePoint to, boolean ignoreMarkPassings) throws NoWindException {
        return detectManeuvers(
                competitor, approximate(competitor, getRace().getBoatClass().getMaximumDistanceForCourseApproximation(),
                from, to), ignoreMarkPassings, from, to);
    }

    /**
     * Tries to detect maneuvers on the <code>competitor</code>'s track based on a number of approximating fixes. The
     * fixes contain bearing information, but this is not the bearing leading to the next approximation fix but the
     * bearing the boat had at the time of the approximating fix which is taken from the original track.
     * <p>
     * 
     * The time period assumed for a maneuver duration is taken from the
     * {@link BoatClass#getApproximateManeuverDurationInMilliseconds() boat class}. If no maneuver is detected, an empty
     * list is returned. Maneuvers can only be expected to be detected if at least three fixes are provided in
     * <code>approximatedFixesToAnalyze</code>. For the inner approximating fixes (all except the first and the last
     * approximating fix), their course changes according to the approximated path (and not the underlying actual
     * tracked fixes) are computed. Subsequent course changes to the same direction are then grouped. Those in closer
     * timely distance than {@link #getApproximateManeuverDurationInMilliseconds()} (including single course changes
     * that have no surrounding other course changes to group) are grouped into one {@link Maneuver}.
     * 
     * @param ignoreMarkPassings
     *            When <code>true</code>, no {@link ManeuverType#MARK_PASSING} maneuvers will be identified, and the
     *            fact that a mark passing would split up what else may be a penalty circle is ignored. This is helpful
     *            for recursive calls, e.g., after identifying a tack and a jibe around a mark passing and trying to
     *            identify for the time before and after the mark passing which maneuvers exist on which side of the
     *            passing.
     * @param earliestManeuverStart
     *            maneuver start will not be before this time point; if a maneuver is found whose time point is at or
     *            after this time point, no matter how close it is, its start regarding speed and course into the
     *            maneuver and the leg before the maneuver is not taken from an earlier time point, even if half the
     *            maneuver duration before the maneuver time point were before this time point.
     * @param latestManeuverEnd
     *            maneuver end will not be after this time point; if a maneuver is found whose time point is at or
     *            before this time point, no matter how close it is, its end regarding speed and course out of the
     *            maneuver and the leg after the maneuver is not taken from a later time point, even if half the
     *            maneuver duration after the maneuver time point were after this time point.
     * 
     * @return an empty list if no maneuver is detected for <code>competitor</code> between <code>from</code> and
     *         <code>to</code>, or else the list of maneuvers detected.
     */
    private List<Maneuver> detectManeuvers(Competitor competitor, Iterable<GPSFixMoving> approximatingFixesToAnalyze,
            boolean ignoreMarkPassings, TimePoint earliestManeuverStart, TimePoint latestManeuverEnd)
            throws NoWindException {
        List<Maneuver> result = new ArrayList<Maneuver>();
        if (Util.size(approximatingFixesToAnalyze) > 2) {
            List<Pair<GPSFixMoving, CourseChange>> courseChangeSequenceInSameDirection = new ArrayList<Pair<GPSFixMoving, CourseChange>>();
            Iterator<GPSFixMoving> approximationPointsIter = approximatingFixesToAnalyze.iterator();
            GPSFixMoving previous = approximationPointsIter.next();
            GPSFixMoving current = approximationPointsIter.next();
            // the bearings in these variables are between approximation points
            SpeedWithBearing speedWithBearingOnApproximationFromPreviousToCurrent = previous
                    .getSpeedAndBearingRequiredToReach(current);
            SpeedWithBearing speedWithBearingOnApproximationFromCurrentToNext; // will certainly be assigned because iter's collection's size > 2
            do {
                GPSFixMoving next = approximationPointsIter.next();
                // traveling on great circle segments from one approximation point to the next
                speedWithBearingOnApproximationFromCurrentToNext = current.getSpeedAndBearingRequiredToReach(next);
                // compute course change on "approximation track"
                // FIXME bug 2009: when a maneuver (particularly a penalty circle) is executed at high turn rates, approximations may lead to turns >180deg, hence inferred to turn the wrong way; need to loop across the non-approximated fixes here!
                CourseChange courseChange = speedWithBearingOnApproximationFromPreviousToCurrent
                        .getCourseChangeRequiredToReach(speedWithBearingOnApproximationFromCurrentToNext);
                Bearing courseChangeOnOriginalFixes = getCourseChange(competitor, previous.getTimePoint(), next.getTimePoint());
                // check for the case where the course change between the approximation fixes may have been >180deg by comparing the direction
                // of the course change on the approximation points with the direction of the course change during the same time range on the
                // original fixes (see also bug 2009):
                if (Math.abs(courseChangeOnOriginalFixes.getDegrees()) > 180 &&
                    Math.signum(courseChange.getCourseChangeInDegrees()) != Math.signum(courseChangeOnOriginalFixes.getDegrees())) {
                    courseChange = new CourseChangeImpl(-Math.signum(courseChange.getCourseChangeInDegrees())*(360.0-Math.abs(courseChange.getCourseChangeInDegrees())),
                            courseChange.getSpeedChangeInKnots());
                }
                Pair<GPSFixMoving, CourseChange> courseChangeAtFix = new Pair<GPSFixMoving, CourseChange>(current,
                        courseChange);
                if (!courseChangeSequenceInSameDirection.isEmpty()
                        && Math.signum(courseChangeSequenceInSameDirection.get(0).getB().getCourseChangeInDegrees()) != Math
                                .signum(courseChange.getCourseChangeInDegrees())) {
                    // course change in different direction; cluster the course changes in same direction so far, then
                    // start new list
                    List<Maneuver> maneuvers = groupChangesInSameDirectionIntoManeuvers(competitor,
                            courseChangeSequenceInSameDirection,
                            ignoreMarkPassings, earliestManeuverStart, latestManeuverEnd);
                    result.addAll(maneuvers);
                    courseChangeSequenceInSameDirection.clear();
                }
                courseChangeSequenceInSameDirection.add(courseChangeAtFix);
                previous = current;
                current = next;
                speedWithBearingOnApproximationFromPreviousToCurrent = speedWithBearingOnApproximationFromCurrentToNext;
            } while (approximationPointsIter.hasNext());
            if (!courseChangeSequenceInSameDirection.isEmpty()) {
                result.addAll(groupChangesInSameDirectionIntoManeuvers(competitor,
                        courseChangeSequenceInSameDirection,
                        ignoreMarkPassings, earliestManeuverStart, latestManeuverEnd));
            }
        }
        return result;
    }

    /**
     * On <code>competitor</code>'s track iterates the fixes starting after <code>startExclusive</code> until
     * <code>endExclusive</code> or any later fix has been reached and sums up the direction change as a "bearing." A
     * negative sign means a direction change to port, a positive sign means a direction change to starboard.
     */
    private Bearing getCourseChange(Competitor competitor, TimePoint startExclusive, TimePoint endExclusive) {
        Bearing directionChangeInDegrees = new DegreeBearingImpl(0);
        GPSFixTrack<Competitor, GPSFixMoving> track = getTrack(competitor);
        track.lockForRead();
        try {
            GPSFixMoving previous = null;
            GPSFixMoving fix = null;
            for (Iterator<GPSFixMoving> i=track.getFixesIterator(startExclusive, /* inclusive */ false);
                 i.hasNext() && (previous == null || !previous.getTimePoint().after(endExclusive));
                 previous = fix) {
                fix = i.next();
                if (previous != null) {
                    directionChangeInDegrees = new DegreeBearingImpl(directionChangeInDegrees.getDegrees()
                            + previous.getSpeed().getBearing().getDifferenceTo(fix.getSpeed().getBearing())
                                    .getDegrees());
                }
            }
        } finally {
            track.unlockAfterRead();
        }
        return directionChangeInDegrees;
    }

    /**
     * Fetches results from {@link #maneuverCache}. The cache is updated asynchronously after relevant updates have been
     * received (see {@link #triggerManeuverCacheRecalculation(Competitor)} and
     * {@link #triggerManeuverCacheRecalculationForAllCompetitors()}). Callers can choose whether to wait for any
     * ongoing updates by using the <code>waitForLatest</code> parameter. From the cache the interval requested is then
     * {@link #extractInterval(TimePoint, TimePoint, List) extracted}.
     * 
     * @param waitForLatest
     *            if <code>true</code>, any currently ongoing maneuver recalculation for <code>competitor</code> is
     *            waited for before returning the result; otherwise, whatever is in the {@link #maneuverCache} for
     *            <code>competitor</code>, reduced to the interval requested, will be returned.
     */
    @Override
    public Iterable<Maneuver> getManeuvers(Competitor competitor, TimePoint from, TimePoint to, boolean waitForLatest) {
        com.sap.sse.common.Util.Triple<TimePoint, TimePoint, List<Maneuver>> allManeuvers = maneuverCache.get(competitor, waitForLatest);
        List<Maneuver> result;
        if (allManeuvers == null) {
            result = Collections.emptyList();
        } else {
            result = extractInterval(from, to, allManeuvers.getC());
        }
        return result;
    }

    private <T extends Timed> List<T> extractInterval(TimePoint from, TimePoint to, List<T> listOfTimed) {
        List<T> result = new LinkedList<T>();
        for (T timed : listOfTimed) {
            if (timed.getTimePoint().compareTo(from) >= 0 && timed.getTimePoint().compareTo(to) <= 0) {
                result.add(timed);
            }
        }
        return result;
    }

    /**
     * Groups the {@link CourseChange} sequence into groups where the times of the fixes at which the course changes
     * took place are no further apart than {@link #getApproximateManeuverDurationInMilliseconds()} milliseconds or
     * where the distances of those course changes are less than three hull lengths apart. For those, a single
     * {@link Maneuver} object is created and added to the resulting list. The maneuver sums up the direction changes of
     * the individual {@link CourseChange} objects. This can result in direction changes of more than 180 degrees in one
     * direction which may, e.g., represent a penalty circle or a mark rounding maneuver. As the maneuver's time point,
     * the average time point of the course changes that went into the maneuver construction is used.
     * <p>
     * @param courseChangeSequenceInSameDirection
     *            all expected to have equal {@link CourseChange#to()} values
     * @param ignoreMarkPassings
     *            When <code>true</code>, no {@link ManeuverType#MARK_PASSING} maneuvers will be identified, and the
     *            fact that a mark passing would split up what else may be a penalty circle is ignored. This is helpful
     *            for recursive calls, e.g., after identifying a tack and a jibe around a mark passing and trying to
     *            identify for the time before and after the mark passing which maneuvers exist on which side of the
     *            passing.
     * @param earliestManeuverStart
     *            maneuver start will not be before this time point; if a maneuver is found whose time point is at or
     *            after this time point, no matter how close it is, its start regarding speed and course into the
     *            maneuver and the leg before the maneuver is not taken from an earlier time point, even if half the
     *            maneuver duration before the maneuver time point were before this time point.
     * @param latestManeuverEnd
     *            maneuver end will not be after this time point; if a maneuver is found whose time point is at or
     *            before this time point, no matter how close it is, its end regarding speed and course out of the
     *            maneuver and the leg after the maneuver is not taken from a later time point, even if half the
     *            maneuver duration after the maneuver time point were after this time point.
     * 
     * @return a non-<code>null</code> list
     */
    private List<Maneuver> groupChangesInSameDirectionIntoManeuvers(Competitor competitor,
            List<Pair<GPSFixMoving, CourseChange>> courseChangeSequenceInSameDirection,
            boolean ignoreMarkPassings, TimePoint earliestManeuverStart,
            TimePoint latestManeuverEnd) throws NoWindException {
        List<Maneuver> result = new ArrayList<Maneuver>();
        List<Pair<GPSFixMoving, CourseChange>> group = new ArrayList<Pair<GPSFixMoving, CourseChange>>();
        if (!courseChangeSequenceInSameDirection.isEmpty()) {
            Distance threeHullLengths = competitor.getBoat().getBoatClass().getHullLength().scale(3);
            Iterator<Pair<GPSFixMoving, CourseChange>> iter = courseChangeSequenceInSameDirection.iterator();
            double totalCourseChangeInDegrees = 0.0;
            do {
                Pair<GPSFixMoving, CourseChange> currentFixAndCourseChange = iter.next();
                if (!group.isEmpty()
                        // TODO use different maneuver times for upwind / reaching / downwind / cross-leg (mark passing)
                        // group contains complete maneuver if the next fix is too late or too far away to belong to the
                        // same maneuver
                        // FIXME penalty circles slow down the boat so much that time limit may get exceeded although
                        // distance limit is matched
                        && currentFixAndCourseChange.getA().getTimePoint().asMillis()
                                - group.get(group.size() - 1).getA().getTimePoint().asMillis() > getApproximateManeuverDurationInMilliseconds()
                        && currentFixAndCourseChange.getA().getPosition()
                                .getDistance(group.get(group.size() - 1).getA().getPosition())
                                .compareTo(threeHullLengths) > 0) {
                    // if next is more then approximate maneuver duration later or further apart than two hull lengths,
                    // turn the current group into a maneuver and add to result
                    Util.addAll(createManeuverFromGroupOfCourseChanges(competitor,
                            group, totalCourseChangeInDegrees < 0 ? NauticalSide.PORT : NauticalSide.STARBOARD, earliestManeuverStart, latestManeuverEnd), result);
                    group.clear();
                    totalCourseChangeInDegrees = 0.0;
                }
                totalCourseChangeInDegrees += currentFixAndCourseChange.getB().getCourseChangeInDegrees();
                group.add(currentFixAndCourseChange);
                // change
            } while (iter.hasNext());
            if (!group.isEmpty()) {
                Util.addAll(createManeuverFromGroupOfCourseChanges(competitor, group,
                        totalCourseChangeInDegrees < 0 ? NauticalSide.PORT : NauticalSide.STARBOARD, earliestManeuverStart, latestManeuverEnd), result);
            }
        }
        return result;
    }

    private Iterable<Maneuver> createManeuverFromGroupOfCourseChanges(Competitor competitor, List<Pair<GPSFixMoving, CourseChange>> group,
            NauticalSide maneuverDirection, TimePoint earliestManeuverStart, TimePoint latestManeuverEnd) throws NoWindException {
        List<Maneuver> result = new ArrayList<>();
        TimePoint earliestTimePointBeforeManeuver = Collections.max(Arrays.asList(new MillisecondsTimePoint(group.get(0).getA().getTimePoint()
                .asMillis() - getApproximateManeuverDurationInMilliseconds() / 2), earliestManeuverStart));
        TimePoint latestTimePointAfterManeuver = Collections.min(Arrays.asList(new MillisecondsTimePoint(group.get(group.size() - 1).getA()
                .getTimePoint().asMillis() + getApproximateManeuverDurationInMilliseconds() / 2), latestManeuverEnd));
        
        ManeuverDetailsWithBearingSteps maneuverMainCurveDetails = computeManeuverMainCurveDetails(competitor, earliestTimePointBeforeManeuver, latestTimePointAfterManeuver, maneuverDirection);
        ManeuverCurveDetails maneuverDetails = computeManeuverDetails(competitor, maneuverMainCurveDetails, earliestManeuverStart, latestManeuverEnd);
        final GPSFixTrack<Competitor, GPSFixMoving> competitorTrack = getTrack(competitor);
        Position maneuverPosition = competitorTrack.getEstimatedPosition(maneuverDetails.getTimePoint(), /* extrapolate */false);
        final Wind wind = getWind(maneuverPosition, maneuverDetails.getTimePoint());
        final Tack tackAfterManeuver = wind == null ? null : getTack(maneuverPosition, maneuverDetails.getTimePointAfter(), maneuverDetails.getSpeedWithBearingAfter().getBearing());
        ManeuverType maneuverType;
        Distance maneuverLoss = null;
        // the TrackedLegOfCompetitor variables may be null, e.g., in case the time points are before or after the race
        TrackedLegOfCompetitor legBeforeManeuver = getTrackedLeg(competitor, maneuverMainCurveDetails.getTimePointBefore());
        TrackedLegOfCompetitor legAfterManeuver = getTrackedLeg(competitor, maneuverMainCurveDetails.getTimePointAfter());
        Waypoint waypointPassed = null; // set for MARK_PASSING maneuvers only
        NauticalSide sideToWhichWaypointWasPassed = null; // set for MARK_PASSING maneuvers only
        // check for mask passing first; a tacking / jibe-setting mark rounding thus takes precedence over being
        // detected as a penalty circle
        final TimePoint markPassingTimePoint;
        if (legBeforeManeuver != legAfterManeuver
                // a maneuver at the start line is not to be considered a MARK_PASSING maneuver; show a tack as a tack
                && legAfterManeuver != null
                && legAfterManeuver.getLeg().getFrom() != getRace().getCourse().getFirstWaypoint()) {
            waypointPassed = legAfterManeuver.getLeg().getFrom();
            MarkPassing markPassing = getMarkPassing(competitor, waypointPassed);
            markPassingTimePoint = markPassing != null ? markPassing.getTimePoint() : maneuverDetails.getTimePoint();
            Position markPassingPosition = markPassing != null ? competitorTrack.getEstimatedPosition(markPassingTimePoint, /* extrapolate */false) : maneuverPosition;
            sideToWhichWaypointWasPassed = maneuverDirection;
            // produce an additional mark passing maneuver; continue to analyze to catch jibe sets and kiwi drops
            result.add(new MarkPassingManeuverImpl(ManeuverType.MARK_PASSING, tackAfterManeuver, markPassingPosition,
                    maneuverLoss, markPassingTimePoint, maneuverDetails.getTimePointBefore(),
                    maneuverDetails.getTimePointAfter(), maneuverDetails.getSpeedWithBearingBefore(),
                    maneuverDetails.getSpeedWithBearingAfter(), maneuverDetails.getTotalCourseChangeInDegrees(),
                    maneuverMainCurveDetails.getTimePointBefore(), maneuverMainCurveDetails.getTimePointAfter(),
                    maneuverMainCurveDetails.getTotalCourseChangeInDegrees(), waypointPassed,
                    sideToWhichWaypointWasPassed));
        } else {
            markPassingTimePoint = null;
        }
        BearingChangeAnalyzer bearingChangeAnalyzer = BearingChangeAnalyzer.INSTANCE;
        final Bearing courseBeforeManeuver = maneuverMainCurveDetails.getSpeedWithBearingBefore().getBearing();
        final Bearing courseAfterManeuver = maneuverMainCurveDetails.getSpeedWithBearingAfter().getBearing();
        final double mainCurveTotalCourseChangeInDegrees = maneuverMainCurveDetails.getTotalCourseChangeInDegrees();
        int numberOfJibes = wind == null ? 0 : bearingChangeAnalyzer.didPass(courseBeforeManeuver, mainCurveTotalCourseChangeInDegrees, courseAfterManeuver, wind.getBearing());
        int numberOfTacks = wind == null ? 0 : bearingChangeAnalyzer.didPass(courseBeforeManeuver, mainCurveTotalCourseChangeInDegrees, courseAfterManeuver, wind.getFrom());
        if (markPassingTimePoint != null && (numberOfTacks + numberOfJibes > 0)) {
            // In case of a mark passing we need to split the maneuver analysis into the phase before and after
            // the mark passing. First of all, this is important to identify the correct maneuver time point for
            // each tack and jibe, second it is essential to call a penalty which is only the case if the tack and
            // the jibe are on the same side of the mark passing; otherwise this may have been a jibe set or a
            // kiwi drop.
            // Therefore, we recursively detect the maneuvers for the segment before and the segment after the
            // mark passing and add the results to our result.
            result.addAll(detectManeuvers(competitor, maneuverDetails.getTimePointBefore(), markPassingTimePoint.minus(1), /* ignoreMarkPassings */ true));
            result.addAll(detectManeuvers(competitor, markPassingTimePoint.plus(1), maneuverDetails.getTimePointAfter(), /* ignoreMarkPassings */ true));
        } else {
            // Either there was no mark passing, or the mark passing was not accompanied by a tack or a jibe.
            // For the first tack/jibe combination (they must alternate because the course changes in the same direction and
            // the wind is considered sufficiently stable to not allow for two successive tacks or two successive jibes)
            // we create a PENALTY_CIRCLE maneuver and recurse for the time interval after the first penalty circle has completed.
            if (numberOfTacks>0 && numberOfJibes>0 && markPassingTimePoint == null) {
                TimePoint firstPenaltyCircleCompletedAt = getTimePointOfCompletionOfFirstPenaltyCircle(
                        competitor, maneuverMainCurveDetails.getTimePointBefore(), courseBeforeManeuver,
                        maneuverMainCurveDetails.getManeuverBearingSteps(), wind);
                if (firstPenaltyCircleCompletedAt == null) {
                    // This should really not happen!
                    logger.warning(
                            "Maneuver detection has failed to process penalty circle maneuver correctly, because getTimePointOfCompletionOfFirstPenaltyCircle() returned null. Race-Id: "
                                    + getRace().getId() + ", Competitor: " + competitor.getName()
                                    + ", Time point before maneuver: " + maneuverDetails.getTimePointBefore());
                    // Use already detected maneuver details as fallback data to prevent Nullpointer
                    firstPenaltyCircleCompletedAt = maneuverDetails.getTimePointAfter();
                }
                maneuverType = ManeuverType.PENALTY_CIRCLE;
                if (legBeforeManeuver != null) {
                    maneuverLoss = legBeforeManeuver.getManeuverLoss(maneuverDetails.getTimePointBefore(),
                            maneuverDetails.getTimePoint(), firstPenaltyCircleCompletedAt);
                }
                ManeuverDetailsWithBearingSteps refinedPenaltyMainCurveDetails = computeManeuverMainCurveDetails(
                        competitor, maneuverMainCurveDetails.getTimePointBefore(), firstPenaltyCircleCompletedAt,
                        maneuverDirection);
                ManeuverCurveDetails refinedPenaltyDetails = computeManeuverDetails(competitor,
                        refinedPenaltyMainCurveDetails, maneuverDetails.getTimePointBefore(), firstPenaltyCircleCompletedAt);
                Position penaltyPosition = competitorTrack.getEstimatedPosition(refinedPenaltyDetails.getTimePoint(),
                        /* extrapolate */ false);
                final Maneuver maneuver = new ManeuverImpl(maneuverType, tackAfterManeuver, penaltyPosition,
                        maneuverLoss, refinedPenaltyDetails.getTimePoint(), refinedPenaltyDetails.getTimePointBefore(),
                        refinedPenaltyDetails.getTimePointAfter(), refinedPenaltyDetails.getSpeedWithBearingBefore(),
                        refinedPenaltyDetails.getSpeedWithBearingAfter(),
                        refinedPenaltyDetails.getTotalCourseChangeInDegrees(),
                        refinedPenaltyMainCurveDetails.getTimePointBefore(),
                        refinedPenaltyMainCurveDetails.getTimePointAfter(),
                        refinedPenaltyMainCurveDetails.getTotalCourseChangeInDegrees());
                result.add(maneuver);
                // after we've "consumed" one tack and one jibe, recursively find more maneuvers if tacks and/or jibes
                // remain
                if (numberOfTacks > 1 || numberOfJibes > 1) {
                    result.addAll(detectManeuvers(competitor, firstPenaltyCircleCompletedAt,
                            maneuverDetails.getTimePointAfter(), /* ignoreMarkPassings */ true));
                }
            } else {
                if (numberOfTacks > 0) {
                    maneuverType = ManeuverType.TACK;
                    if (legBeforeManeuver != null) {
                        maneuverLoss = legBeforeManeuver.getManeuverLoss(maneuverDetails.getTimePointBefore(),
                                maneuverDetails.getTimePoint(), maneuverDetails.getTimePointAfter());
                    }
                } else if (numberOfJibes > 0) {
                    maneuverType = ManeuverType.JIBE;
                    if (legBeforeManeuver != null) {
                        maneuverLoss = legBeforeManeuver.getManeuverLoss(maneuverDetails.getTimePointBefore(),
                                maneuverDetails.getTimePoint(), maneuverDetails.getTimePointAfter());
                    }
                } else {
                    if (wind != null) {
                        // heading up or bearing away
                        Bearing windBearing = wind.getBearing();
                        Bearing toWindBeforeManeuver = windBearing
                                .getDifferenceTo(maneuverMainCurveDetails.getSpeedWithBearingBefore().getBearing());
                        Bearing toWindAfterManeuver = windBearing
                                .getDifferenceTo(maneuverMainCurveDetails.getSpeedWithBearingAfter().getBearing());
                        maneuverType = Math.abs(toWindBeforeManeuver.getDegrees()) < Math.abs(toWindAfterManeuver
                                .getDegrees()) ? ManeuverType.HEAD_UP : ManeuverType.BEAR_AWAY;
                        // treat maneuver main curve details as main maneuver details, because the detected maneuver is
                        // either HEAD_UP or BEAR_AWAY
                        maneuverDetails = maneuverMainCurveDetails;
                    } else {
                        // no wind information; marking as UNKNOWN
                        maneuverType = ManeuverType.UNKNOWN;
                        if (legBeforeManeuver != null) {
                            maneuverLoss = legBeforeManeuver.getManeuverLoss(maneuverDetails.getTimePointBefore(),
                                    maneuverDetails.getTimePoint(), maneuverDetails.getTimePointAfter());
                        }
                    }
                }
                if (Math.floor(maneuverMainCurveDetails.getTotalCourseChangeInDegrees()) != 0) {
                    final Maneuver maneuver = new ManeuverImpl(maneuverType, tackAfterManeuver, maneuverPosition,
                            maneuverLoss, maneuverDetails.getTimePoint(), maneuverDetails.getTimePointBefore(),
                            maneuverDetails.getTimePointAfter(), maneuverDetails.getSpeedWithBearingBefore(),
                            maneuverDetails.getSpeedWithBearingAfter(), maneuverDetails.getTotalCourseChangeInDegrees(),
                            maneuverMainCurveDetails.getTimePointBefore(), maneuverMainCurveDetails.getTimePointAfter(),
                            maneuverMainCurveDetails.getTotalCourseChangeInDegrees());
                    result.add(maneuver);
                }
            }
        }
        return result;
    }

    /**
     * Starting at <code>timePointBeforeManeuver</code>, and assuming that the group of
     * <code>approximatedFixesAndCourseChanges</code> contains at least a tack and a jibe, finds the approximated fix's
     * time point at which one tack and one jibe have been completed and for which the total course change is as close
     * as possible to 360°.
     */
    private TimePoint getTimePointOfCompletionOfFirstPenaltyCircle(Competitor competitor,
            TimePoint timePointBeforeManeuver, Bearing courseBeforeManeuver,
            Iterable<SpeedWithBearingStep> maneuverBearingSteps, Wind wind) {
        double totalCourseChangeInDegrees = 0;
        double bestTotalCourseChangeInDegrees = 0; // this should be as close as possible to 360� after one tack and one
                                                   // gybe
        BearingChangeAnalyzer bearingChangeAnalyzer = BearingChangeAnalyzer.INSTANCE;
        Bearing newCourse = courseBeforeManeuver;
        TimePoint result = null;
        boolean firstEntry = true;
        for (SpeedWithBearingStep fixAndCourseChange : maneuverBearingSteps) {
            if (firstEntry) {
                firstEntry = false;
                continue;
            }
            totalCourseChangeInDegrees += fixAndCourseChange.getCourseChangeInDegrees();
            newCourse = newCourse.add(new DegreeBearingImpl(fixAndCourseChange.getCourseChangeInDegrees()));
            int numberOfJibes = bearingChangeAnalyzer.didPass(courseBeforeManeuver, totalCourseChangeInDegrees,
                    newCourse, wind.getBearing());
            int numberOfTacks = bearingChangeAnalyzer.didPass(courseBeforeManeuver, totalCourseChangeInDegrees,
                    newCourse, wind.getFrom());
            if (numberOfJibes > 0 && numberOfTacks > 0) {
                if (numberOfJibes > 1 || numberOfTacks > 1) {
                    // It could be that one or both numbers increased to greater than 1 from 0. In this case
                    // we want to find the point between the completion of the penalty and the next maneuver
                    // which increases one of the counters to 2 and use that time point as the result:
                    if (result == null) {
                        // It could be that both numbers increased, and one was 1 before, so now we have 1 and 2. But
                        // we can't split it up finer than two fixes, so we'll use the time point between the last two
                        // fixes
                        // instead:
                        result = fixAndCourseChange.getTimePoint();
                    }
                    break; // don't continue into a subsequent tack/gybe sailed in conjunction with the penalty or
                           // starting the next circle
                }
                if (Math.abs(360 - Math.abs(totalCourseChangeInDegrees)) < (Math
                        .abs(360 - Math.abs(bestTotalCourseChangeInDegrees)))) {
                    bestTotalCourseChangeInDegrees = totalCourseChangeInDegrees;
                    result = fixAndCourseChange.getTimePoint();
                } else {
                    break; // not getting closer but further away from 360�
                }
            }
        }
        return result;
    }

    /**
     * Computes the details of the main curve of maneuver such as maneuver entering and exiting time point with speed
     * and bearing, time point of maneuver climax, total course change, and relevant maneuver bearing steps. The
     * maneuver section with the highest (starboard maneuvers)/lowest (port side maneuvers) sum of differences between
     * bearing steps is defined as the main curve section.
     */
    private ManeuverDetailsWithBearingSteps computeManeuverMainCurveDetails(Competitor competitor,
            TimePoint timePointBeforeManeuver, TimePoint timePointAfterManeuver, NauticalSide maneuverDirection) {
        GPSFixTrack<Competitor, GPSFixMoving> track = getTrack(competitor);
        Duration gpsInterval = track.getAverageIntervalBetweenRawFixes();
        Duration intervalBetweenSteps = gpsInterval.asMillis() > 1000 ? Duration.ONE_SECOND : gpsInterval;
        Iterable<SpeedWithBearingStep> stepsToAnalyze = track.getSpeedWithBearingSteps(timePointBeforeManeuver,
                timePointAfterManeuver, intervalBetweenSteps);
        TimePoint maneuverTimePoint = computeManeuverTimePoint(stepsToAnalyze, maneuverDirection);
        ManeuverEnteringAndExitingDetails maneuverMainCurveEnteringAndExitingDetails = computeEnteringAndExitingDetailsOfManeuverMainCurve(
                maneuverTimePoint, stepsToAnalyze, maneuverDirection);
        List<SpeedWithBearingStep> maneuverMainCurveSpeedWithBearingSteps = getSpeedWithBearingStepsWithinTimeRange(
                stepsToAnalyze, maneuverMainCurveEnteringAndExitingDetails.getTimePointBefore(),
                maneuverMainCurveEnteringAndExitingDetails.getTimePointAfter());
        double totalCourseChangeInDegrees = 0;
        for (SpeedWithBearingStep speedWithBearingStep : maneuverMainCurveSpeedWithBearingSteps) {
            totalCourseChangeInDegrees += speedWithBearingStep.getCourseChangeInDegrees();
        }
        return new ManeuverDetailsWithBearingSteps(maneuverMainCurveEnteringAndExitingDetails.getTimePointBefore(),
                maneuverMainCurveEnteringAndExitingDetails.getTimePointAfter(), maneuverTimePoint,
                maneuverMainCurveEnteringAndExitingDetails.getSpeedWithBearingBefore(),
                maneuverMainCurveEnteringAndExitingDetails.getSpeedWithBearingAfter(), totalCourseChangeInDegrees,
                maneuverMainCurveSpeedWithBearingSteps);
    }
    
    /**
     * Computes the details of maneuver such as maneuver entering and exiting time point with speed and bearing, time
     * point of maneuver climax and total course change. The provided details of maneuver main curve are used as minimal
     * maneuver section which gets expanded by analyzing the speed trend before and after the main curve of maneuver.
     * The goal is to determine the maneuver entering and exiting time points such that the speed and course values
     * ideally represent stable segments leading into and out of the maneuver. It is assumed that before maneuver the
     * speed starts to slow down. Thus, in order to approximate the beginning time point of the maneuver, the speed
     * maximum is determined throughout chronological iteration of speed steps starting from time point of main curve
     * beginning towards the left. Similar to this, the exiting time point of maneuver is approximated by speed maximum
     * determination throughout chronological iteration of speed steps starting from time of main curve finish towards
     * the right.
     */
    private ManeuverCurveDetails computeManeuverDetails(Competitor competitor,
            ManeuverDetailsWithBearingSteps maneuverMainCurveDetails, TimePoint earliestManeuverStart,
            TimePoint latestManeuverEnd) {
        ComputedManeuverSectionExtension beforeManeuverSectionExtension = expandBeforeManeuverSectionBySpeedTrendAnalysis(
                competitor, maneuverMainCurveDetails, earliestManeuverStart);
        ComputedManeuverSectionExtension afterManeuverSectionExtension = expandAfterManeuverSectionBySpeedTrendAnalysis(
                competitor, maneuverMainCurveDetails, latestManeuverEnd);
        double totalCourseChangeInDegrees = beforeManeuverSectionExtension.getTotalCourseChangeInDegreesExtension()
                + maneuverMainCurveDetails.getTotalCourseChangeInDegrees()
                + afterManeuverSectionExtension.getTotalCourseChangeInDegreesExtension();
        return new ManeuverCurveDetails(beforeManeuverSectionExtension.getExtensionTimePoint(),
                afterManeuverSectionExtension.getExtensionTimePoint(), maneuverMainCurveDetails.getTimePoint(),
                beforeManeuverSectionExtension.getSpeedWithBearingAtExtensionTimePoint(),
                afterManeuverSectionExtension.getSpeedWithBearingAtExtensionTimePoint(), totalCourseChangeInDegrees);
    }

    /**
     * Performs a search for speed maximum which starts at the time point of main curve start (in the following
     * referenced as {@code t} and lasts at least ({@link BoatClass#getApproximateManeuverDurationInMilliseconds()
     * maneuver duration} / 2) and maximal {@link BoatClass#getApproximateManeuverDurationInMilliseconds() maneuver
     * duration} back in time (chronological iteration towards the left). In interval
     * {@code [t - minimal search duration; t]} global speed maximum is searched, whereas in interval
     * {@code [t - maximal search duration; t - minimal search duration)} the search continues only if the speed keeps
     * rising.
     * 
     * @see #computeManeuverDetails(Competitor, ManeuverDetailsWithBearingSteps, TimePoint, TimePoint)
     */
    private ComputedManeuverSectionExtension expandBeforeManeuverSectionBySpeedTrendAnalysis(Competitor competitor,
            ManeuverDetailsWithBearingSteps maneuverMainCurveDetails, TimePoint earliestManeuverStart) {
        Duration approximateManeuverDuration = competitor.getBoat().getBoatClass().getApproximateManeuverDuration();
        Duration minDurationForSpeedTrendAnalysis = approximateManeuverDuration.divide(2.0);
        Duration maxDurationForSpeedTrendAnalysis = approximateManeuverDuration;

        GPSFixTrack<Competitor, GPSFixMoving> track = getTrack(competitor);
        TimePoint latestTimePointForSpeedTrendAnalysis = maneuverMainCurveDetails.getTimePointBefore();
        TimePoint earliestTimePointForSpeedTrendAnalysis = latestTimePointForSpeedTrendAnalysis
                .minus(maxDurationForSpeedTrendAnalysis);
        if (earliestTimePointForSpeedTrendAnalysis.before(earliestManeuverStart)) {
            earliestTimePointForSpeedTrendAnalysis = earliestManeuverStart;
        }
        TimePoint timePointSinceGlobalMaximumSearch = latestTimePointForSpeedTrendAnalysis
                .minus(minDurationForSpeedTrendAnalysis);
        Iterable<SpeedWithBearingStep> stepsToAnalyze = track.getSpeedWithBearingSteps(
                earliestTimePointForSpeedTrendAnalysis, latestTimePointForSpeedTrendAnalysis,
                track.getAverageIntervalBetweenRawFixes());
        double previousSpeedInKnots = 0;
        double maxSpeedInKnots = 0;
        SpeedWithBearingStep stepWithMaxSpeed = null;
        //When the iteration starts, the first time stamps will be between [t - maximal search duration; t -
        // minimal search duration).
        boolean localMinimumSearch = true;
        double courseChangeTillMainCurveInDegrees = 0;

        // the view for chronological iteration to the left is reproduced by the following code with iteration to the
        // right.
        for (SpeedWithBearingStep speedWithBearingStep : stepsToAnalyze) {
            double speedInKnots = speedWithBearingStep.getSpeedWithBearing().getKnots();
            if (localMinimumSearch) {
                // If the speed of a step is higher than the speed of its preceding step, it concludes that the next
                // step from this step to the right, which is the preceding step of this iteration, causes a speed
                // decrease. And because the time point of the preceding step of this iteration is in interval [t -
                // maximal search duration; t -
                // minimal search duration), it must be ignored. This is achieved by setting the current step as the
                // step with speed maximum
                if (previousSpeedInKnots <= speedInKnots) {
                    maxSpeedInKnots = speedInKnots;
                    stepWithMaxSpeed = speedWithBearingStep;
                    courseChangeTillMainCurveInDegrees = 0;
                } else {
                    courseChangeTillMainCurveInDegrees += speedWithBearingStep.getCourseChangeInDegrees();
                }
                if (!speedWithBearingStep.getTimePoint().before(timePointSinceGlobalMaximumSearch)) {
                    //Indicate that for the next steps global maximum search should be performed
                    localMinimumSearch = false;
                }
            } else if (maxSpeedInKnots <= speedInKnots) {
                //New global maximum has been found
                maxSpeedInKnots = speedInKnots;
                stepWithMaxSpeed = speedWithBearingStep;
                courseChangeTillMainCurveInDegrees = 0;
            } else {
                courseChangeTillMainCurveInDegrees += speedWithBearingStep.getCourseChangeInDegrees();
            }
            previousSpeedInKnots = speedInKnots;
        }
        if (stepWithMaxSpeed == null) {
            return new ComputedManeuverSectionExtension(maneuverMainCurveDetails.getTimePointBefore(),
                    maneuverMainCurveDetails.getSpeedWithBearingBefore(), 0);
        } else {
            return new ComputedManeuverSectionExtension(stepWithMaxSpeed.getTimePoint(),
                    stepWithMaxSpeed.getSpeedWithBearing(), courseChangeTillMainCurveInDegrees);
        }
    }

    /**
     * Performs a search for speed maximum which starts at the time point of main curve finish (in the following
     * referenced as {@code t} and lasts at least {@link BoatClass#getApproximateManeuverDurationInMilliseconds()
     * maneuver duration} and maximal ({@link BoatClass#getApproximateManeuverDurationInMilliseconds() maneuver
     * duration} * 3) forward in time (chronological iteration towards the right). In interval {@code [t; t + minimal search duration]}
     * global speed maximum is searched, whereas in interval {@code (t + minimal search duration; t + maximal search duration]}
     * the search continues only if the speed keeps rising.
     * 
     * @see #computeManeuverDetails(Competitor, ManeuverDetailsWithBearingSteps, TimePoint, TimePoint)
     */
    private ComputedManeuverSectionExtension expandAfterManeuverSectionBySpeedTrendAnalysis(Competitor competitor,
            ManeuverDetailsWithBearingSteps maneuverMainCurveDetails, TimePoint latestManeuverEnd) {
        Duration approximateManeuverDuration = competitor.getBoat().getBoatClass().getApproximateManeuverDuration();
        Duration minDurationForSpeedTrendAnalysis = approximateManeuverDuration;
        Duration maxDurationForSpeedTrendAnalysis = approximateManeuverDuration.times(3.0);

        GPSFixTrack<Competitor, GPSFixMoving> track = getTrack(competitor);
        TimePoint earliestTimePointForSpeedTrendAnalysis = maneuverMainCurveDetails.getTimePointAfter();
        TimePoint latestTimePointForSpeedTrendAnalysis = earliestTimePointForSpeedTrendAnalysis
                .plus(maxDurationForSpeedTrendAnalysis);
        if (latestTimePointForSpeedTrendAnalysis.after(latestManeuverEnd)) {
            latestTimePointForSpeedTrendAnalysis = latestManeuverEnd;
        }
        TimePoint timePointBeforeLocalMaximumSearch = earliestTimePointForSpeedTrendAnalysis
                .plus(minDurationForSpeedTrendAnalysis);
        Iterable<SpeedWithBearingStep> stepsToAnalyze = track.getSpeedWithBearingSteps(
                earliestTimePointForSpeedTrendAnalysis, latestTimePointForSpeedTrendAnalysis,
                track.getAverageIntervalBetweenRawFixes());
        double previousSpeedInKnots = 0;
        double maxSpeedInKnots = 0;
        SpeedWithBearingStep stepWithMaxSpeed = null;
        double courseChangeSinceMainCurveBeforeSpeedMaximumInDegrees = 0;
        double courseChangeAfterStepWithSpeedMaximum = 0;

        for (SpeedWithBearingStep speedWithBearingStep : stepsToAnalyze) {
            courseChangeAfterStepWithSpeedMaximum += speedWithBearingStep.getCourseChangeInDegrees();
            double speedInKnots = speedWithBearingStep.getSpeedWithBearing().getKnots();
            if (speedWithBearingStep.getTimePoint().after(timePointBeforeLocalMaximumSearch)
                    && previousSpeedInKnots > speedInKnots) {
                // We are in interval (t + minimal search duration; t + maximal search duration] and the speed starts
                // decreasing
                // => abort further search
                break;
            } else {
                // Otherwise find the step with the highest speed
                if (maxSpeedInKnots < speedInKnots) {
                    maxSpeedInKnots = speedInKnots;
                    stepWithMaxSpeed = speedWithBearingStep;
                    courseChangeSinceMainCurveBeforeSpeedMaximumInDegrees += courseChangeAfterStepWithSpeedMaximum;
                    courseChangeAfterStepWithSpeedMaximum = 0;
                }
            }
            previousSpeedInKnots = speedInKnots;
        }
        if (stepWithMaxSpeed == null) {
            return new ComputedManeuverSectionExtension(maneuverMainCurveDetails.getTimePointAfter(),
                    maneuverMainCurveDetails.getSpeedWithBearingAfter(), 0);
        } else {
            return new ComputedManeuverSectionExtension(stepWithMaxSpeed.getTimePoint(),
                    stepWithMaxSpeed.getSpeedWithBearing(), courseChangeSinceMainCurveBeforeSpeedMaximumInDegrees);
        }
    }

    /**
     * Computes entering and exiting time point with speed and bearing for the main curve of maneuver. The strategy here
     * is to cut away bearing steps from the left and right in order to reach a maximal course change corresponding to
     * the target maneuver direction.
     * 
     * @param maneuverTimePoint
     *            The computed time point of maneuver
     * @param bearingStepsToAnalyze
     *            The bearing steps contained within maneuver
     * @param maneuverDirection
     *            The nautical direction of the maneuver
     * @return The computed entering and exiting time point with its speeds with bearings for the main curve
     */
    private ManeuverEnteringAndExitingDetails computeEnteringAndExitingDetailsOfManeuverMainCurve(
            TimePoint maneuverTimePoint, Iterable<SpeedWithBearingStep> bearingStepsToAnalyze,
            NauticalSide maneuverDirection) {
        double totalCourseChangeSignum = maneuverDirection == NauticalSide.PORT ? -1 : 1;
        double maxCourseChangeInDegrees = 0;
        double currentCourseChangeInDegrees = 0;
        // Refine the time point before and after maneuver by checking whether the total course changed before maneuver
        // time point may be increased or kept unchanged if we cut off bearing steps one by one from the left and right.
        TimePoint refinedTimePointBeforeManeuver = null;
        SpeedWithBearing refinedSpeedWithBearingBeforeManeuver = null;
        TimePoint refinedTimePointAfterManeuver = null;
        SpeedWithBearing refinedSpeedWithBearingAfterManeuver = null;
        for (SpeedWithBearingStep entry : bearingStepsToAnalyze) {
            currentCourseChangeInDegrees += entry.getCourseChangeInDegrees();
            TimePoint timePoint = entry.getTimePoint();
            if (timePoint.after(maneuverTimePoint)) {
                // Check whether the totalCourseChange gets better with the added course change of current bearing
                // step, considering the target sign of the course change
                if (maxCourseChangeInDegrees
                        * totalCourseChangeSignum <= currentCourseChangeInDegrees * totalCourseChangeSignum
                                - ABS_COURSE_CHANGE_IN_DEGREES_TO_IGNORE_BETWEEN_BEARING_STEPS) {
                    maxCourseChangeInDegrees = currentCourseChangeInDegrees;
                    refinedTimePointAfterManeuver = timePoint;
                    refinedSpeedWithBearingAfterManeuver = entry.getSpeedWithBearing();
                }
            } else {
                // Check whether the course change is performed in the target direction of maneuver. If the direction
                // sign does not match, or the course change is nearly zero => cut the bearing step from the left
                if (ABS_COURSE_CHANGE_IN_DEGREES_TO_IGNORE_BETWEEN_BEARING_STEPS >= currentCourseChangeInDegrees
                        * totalCourseChangeSignum) {
                    currentCourseChangeInDegrees = 0;
                    refinedTimePointBeforeManeuver = timePoint;
                    refinedSpeedWithBearingBeforeManeuver = entry.getSpeedWithBearing();
                }
            }
        }
        if (refinedTimePointBeforeManeuver == null) {
            // Should not occur, if bearingStepsToAnalyze.size() > 0 and first BearingStep.getCourseChangeInDegrees() ==
            // 0
            throw new IllegalArgumentException("bearingStepsToAnalyze must not be empty");
        }
        if (refinedSpeedWithBearingAfterManeuver == null) {
            // Can only occur, when after maneuver time point different direction compared to the analyzed maneuver is
            // sailed. Thus, the resulting time point until the cut operation should be performed is the maneuver time
            // point itself.
            for (SpeedWithBearingStep entry : bearingStepsToAnalyze) {
                if (!entry.getTimePoint().before(maneuverTimePoint)) {
                    refinedTimePointAfterManeuver = entry.getTimePoint();
                    refinedSpeedWithBearingAfterManeuver = entry.getSpeedWithBearing();
                    break;
                }
            }
        }
        ManeuverEnteringAndExitingDetails maneuverEnteringAndExitingDetails = new ManeuverEnteringAndExitingDetails(
                refinedTimePointBeforeManeuver, refinedTimePointAfterManeuver, refinedSpeedWithBearingBeforeManeuver,
                refinedSpeedWithBearingAfterManeuver);
        return maneuverEnteringAndExitingDetails;
    }

    /**
     * Gets a new list with bearing steps which are lying between provided time range (inclusive the boundaries).
     */
    private List<SpeedWithBearingStep> getSpeedWithBearingStepsWithinTimeRange(
            Iterable<SpeedWithBearingStep> bearingStepsToAnalyze, TimePoint timePointBefore, TimePoint timePointAfter) {
        List<SpeedWithBearingStep> maneuverBearingSteps = new ArrayList<>();
        for (SpeedWithBearingStep entry : bearingStepsToAnalyze) {
            if (entry.getTimePoint().after(timePointAfter)) {
                break;
            }
            if (!entry.getTimePoint().before(timePointBefore)) {
                if (maneuverBearingSteps.isEmpty()) {
                    // First bearing step supposed to have 0 as course change as
                    // it does not have any previous steps with bearings to compute bearing difference.
                    // If the condition is not met, the existing code which uses ManeuverBearingStep class will break.
                    entry = new SpeedWithBearingStepImpl(entry.getTimePoint(), entry.getSpeedWithBearing(), 0.0);
                }
                maneuverBearingSteps.add(entry);
            }
        }
        return maneuverBearingSteps;
    }

    /**
     * Computes the maneuver time point as the time point along between maneuver start and end where the competitor's
     * track has greatest change in course.
     */
    private TimePoint computeManeuverTimePoint(Iterable<SpeedWithBearingStep> maneuverBearingSteps,
            NauticalSide maneuverDirection) {
        double totalCourseChangeSignum = maneuverDirection == NauticalSide.PORT ? -1 : 1;
        double maxAngleSpeedInDegreesPerMillisecond = 0;
        TimePoint maneuverTimePoint = null;
        TimePoint lastTimePoint = null;
        for (SpeedWithBearingStep entry : maneuverBearingSteps) {
            TimePoint timePoint = entry.getTimePoint();
            if (lastTimePoint != null) {
                double courseChangeAngleInDegrees = entry.getCourseChangeInDegrees();
                if (Math.signum(courseChangeAngleInDegrees) == totalCourseChangeSignum) {
                    double angleSpeedInDegreesPerMillisecond = Math.abs(
                            courseChangeAngleInDegrees / (double) (timePoint.asMillis() - lastTimePoint.asMillis()));
                    if (angleSpeedInDegreesPerMillisecond > maxAngleSpeedInDegreesPerMillisecond) {
                        maxAngleSpeedInDegreesPerMillisecond = angleSpeedInDegreesPerMillisecond;
                        maneuverTimePoint = lastTimePoint;
                    }
                }
            } else {
                // default value of maneuver point is the beginning timepoint of the maneuver
                maneuverTimePoint = entry.getTimePoint();
            }
            lastTimePoint = timePoint;
        }
        return maneuverTimePoint;
    }

    /**
     * Fetches the boat class-specific parameter
     */
    private double getManeuverDegreeAngleThreshold() {
        return getRace().getBoatClass().getManeuverDegreeAngleThreshold();
    }

    private Pair<Double, Double> getMinimumAngleBetweenDifferentTacksDownwind(Wind wind) {
        Pair<Double, Double> result;
        double defaultAngle = getRace().getBoatClass().getMinimumAngleBetweenDifferentTacksDownwind();
        double threshold = 20;
        result = usePolarsIfPossible(wind, defaultAngle, LegType.DOWNWIND, threshold);
        return result;
    }
    
    private Pair<Double, Double> getMinimumAngleBetweenDifferentTacksUpwind(Wind wind) {
        Pair<Double, Double> result;
        double defaultAngle = getRace().getBoatClass().getMinimumAngleBetweenDifferentTacksUpwind();
        double threshold = 10;
        result = usePolarsIfPossible(wind, defaultAngle, LegType.UPWIND, threshold);
        return result;
    }

    private Pair<Double, Double> usePolarsIfPossible(Wind wind, double defaultAngle, LegType legType, double threshold) {
        Pair<Double, Double> result;
        if (polarDataService != null) {
            try {
                BearingWithConfidence<Void> average = polarDataService.getManeuverAngle(getRace().getBoatClass(),
                        legType == LegType.DOWNWIND ? ManeuverType.JIBE : ManeuverType.TACK, wind);
                double averageAngleInDegMinusThreshold = average.getObject().getDegrees() - threshold;
                if (averageAngleInDegMinusThreshold < defaultAngle) {
                    result = new Pair<Double, Double>(defaultAngle, 0.1);
                } else {
                    result = new Pair<Double, Double>(averageAngleInDegMinusThreshold, average.getConfidence());
                }
            } catch (NotEnoughDataHasBeenAddedException | IllegalArgumentException e) {
                result = new Pair<Double, Double>(defaultAngle, 0.1);
            }
        } else {
            result = new Pair<Double, Double>(defaultAngle, 0.1);
        }
        return result;
    }

    private long getApproximateManeuverDurationInMilliseconds() {
        return getRace().getBoatClass().getApproximateManeuverDurationInMilliseconds();
    }

    private class StartToNextMarkCacheInvalidationListener implements GPSTrackListener<Mark, GPSFix> {
        private static final long serialVersionUID = 3540278554797445085L;
        private final GPSFixTrack<Mark, GPSFix> listeningTo;

        public StartToNextMarkCacheInvalidationListener(GPSFixTrack<Mark, GPSFix> listeningTo) {
            this.listeningTo = listeningTo;
        }

        public void stopListening() {
            listeningTo.removeListener(this);
        }

        @Override
        public void gpsFixReceived(GPSFix fix, Mark mark, boolean firstFixInTrack) {
            clearDirectionFromStartToNextMarkCache();
        }

        @Override
        public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
        }

        @Override
        public boolean isTransient() {
            return false;
        }
    }

    @Override
    public Distance getWindwardDistanceToCompetitorFarthestAhead(Competitor competitor, TimePoint timePoint, WindPositionMode windPositionMode) {
        final TrackedLegOfCompetitor trackedLeg = getTrackedLeg(competitor, timePoint);
        return trackedLeg == null ? null : trackedLeg.getWindwardDistanceToCompetitorFarthestAhead(timePoint, windPositionMode,
                getRankingMetric().getRankingInfo(timePoint));
    }

    @Override
    public Distance getWindwardDistanceToCompetitorFarthestAhead(Competitor competitor, TimePoint timePoint,
            WindPositionMode windPositionMode, RankingInfo rankingInfo, WindLegTypeAndLegBearingCache cache) {
        final TrackedLegOfCompetitor trackedLeg = getTrackedLeg(competitor, timePoint);
        return trackedLeg == null ? null : trackedLeg.getWindwardDistanceToCompetitorFarthestAhead(timePoint, windPositionMode, rankingInfo, cache);
    }

    @Override
    public Iterable<Mark> getMarks() {
        while (true) {
            try {
                return new HashSet<Mark>(markTracks.keySet());
            } catch (ConcurrentModificationException cme) {
                logger.info("Caught " + cme + "; trying again.");
            }
        }
    }

    @Override
    public Iterable<Sideline> getCourseSidelines() {
        return new ArrayList<Sideline>(courseSidelines.values());
    }

    @Override
    public long getDelayToLiveInMillis() {
        return delayToLiveInMillis;
    }

    protected void setDelayToLiveInMillis(long delayToLiveInMillis) {
        this.delayToLiveInMillis = delayToLiveInMillis;
    }

    @Override
    public TrackedRaceStatus getStatus() {
        return status;
    }

    /**
     * Changes to the {@link #status} variable are synchronized on the {@link #statusNotifier} field.
     * 
     * @return
     */
    protected Object getStatusNotifier() {
        return statusNotifier;
    }

    protected void setStatus(TrackedRaceStatus newStatus) {
        assert newStatus != null;
        final TrackedRaceStatusEnum oldStatus;
        synchronized (getStatusNotifier()) {
            oldStatus = getStatus().getStatus();
            this.status = newStatus;
            getStatusNotifier().notifyAll();
        }
        if (newStatus.getStatus() == TrackedRaceStatusEnum.LOADING && oldStatus != TrackedRaceStatusEnum.LOADING) {
            suspendAllCachesNotUpdatingWhileLoading();
        } else if (oldStatus == TrackedRaceStatusEnum.LOADING && newStatus.getStatus() != TrackedRaceStatusEnum.LOADING && newStatus.getStatus() != TrackedRaceStatusEnum.REMOVED) {
            resumeAllCachesNotUpdatingWhileLoading();
        }
    }

    private void suspendAllCachesNotUpdatingWhileLoading() {
        cachesSuspended = true;
        for (GPSFixTrack<Competitor, GPSFixMoving> competitorTrack : tracks.values()) {
            competitorTrack.suspendValidityCaching();
        }
        for (GPSFixTrack<Mark, GPSFix> markTrack : markTracks.values()) {
            markTrack.suspendValidityCaching();
        }
        if (markPassingCalculator != null) {
            markPassingCalculator.suspend();
        }
        crossTrackErrorCache.suspend();
        maneuverCache.suspend();
    }

    private void resumeAllCachesNotUpdatingWhileLoading() {
        cachesSuspended = false;
        for (GPSFixTrack<Competitor, GPSFixMoving> competitorTrack : tracks.values()) {
            competitorTrack.resumeValidityCaching();
        }
        for (GPSFixTrack<Mark, GPSFix> markTrack : markTracks.values()) {
            markTrack.resumeValidityCaching();
        }
        if (markPassingCalculator != null) {
            markPassingCalculator.resume();
        }
        crossTrackErrorCache.resume();
        if (triggerManeuverCacheInvalidationForAllCompetitors) {
            triggerManeuverCacheRecalculationForAllCompetitors();
        }
        maneuverCache.resume();
    }

    /**
     * Waits on the current ("old") status object which is notified in {@link #setStatus(TrackedRaceStatus)} when the
     * status is changed. The change as well as the check synchronize on the old status object.
     */
    @Override
    public void waitUntilNotLoading() {
        synchronized (getStatusNotifier()) {
            while (getStatus().getStatus() == TrackedRaceStatusEnum.LOADING) {
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
    public void attachRaceLog(RaceLog raceLog) {
        synchronized (TrackedRaceImpl.this) {
            attachedRaceLogs.put(raceLog.getId(), raceLog);
            notifyAll();
            invalidateStartTime();
        }
        notifyListenersWhenAttachingRaceLog(raceLog);
    }

    @Override
    public void attachRaceExecutionProvider(RaceExecutionOrderProvider raceExecutionOrderProvider) {
        if (raceExecutionOrderProvider != null && !attachedRaceExecutionOrderProviders.containsKey(raceExecutionOrderProvider)) {
            attachedRaceExecutionOrderProviders.put(raceExecutionOrderProvider, raceExecutionOrderProvider);
        }
    }

    protected Set<TrackedRace> getPreviousRacesFromAttachedRaceExecutionOrderProviders() {
        final Set<TrackedRace> result;
        if (attachedRaceExecutionOrderProviders != null) {
            result = attachedRaceExecutionOrderProviders.values().stream().map(reop->reop.getPreviousRacesInExecutionOrder(this)).collect(HashSet::new, (r, e)->r.addAll(e), (r, e)->r.addAll(e));
        } else {
            result = Collections.emptySet();
        }
        return result;
    }
    
    @Override
    public void detachRaceExecutionOrderProvider(RaceExecutionOrderProvider raceExecutionOrderProvider) {
        if (raceExecutionOrderProvider != null) {
            attachedRaceExecutionOrderProviders.remove(raceExecutionOrderProvider);
        }
    }
    
    public boolean hasRaceExecutionOrderProvidersAttached(){
        return !attachedRaceExecutionOrderProviders.isEmpty();
    }

    protected ReadonlyRaceState getRaceState(RaceLog raceLog) {
        ReadonlyRaceState result;
        synchronized (raceStates) {
            result = raceStates.get(raceLog);
            if (result == null) {
                result = RaceStateImpl.create(raceLogResolver, raceLog);
                raceStates.put(raceLog, result);
            }
        }
        return result;
    }
    
    @Override
    public void attachRegattaLog(RegattaLog regattaLog) {
        LockUtil.lockForRead(getSerializationLock());
        synchronized (TrackedRaceImpl.this) {
            if (attachedRegattaLogs != null) {
                attachedRegattaLogs.put(regattaLog.getId(), regattaLog);
            }
            notifyListenersWhenAttachingRegattaLog(regattaLog);
            // informListenersAboutAttachedRegattaLog(regattaLog);
            TrackedRaceImpl.this.notifyAll();
        }
        LockUtil.unlockAfterRead(getSerializationLock());
        updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
    }
    
    @Override
    public Iterable<RegattaLog> getAttachedRegattaLogs() {
        return new HashSet<>(attachedRegattaLogs.values());
    }

    @Override
    public RaceLog detachRaceLog(Serializable identifier) {
        final RaceLog raceLog = this.attachedRaceLogs.remove(identifier);
        notifyListenersWhenDetachingRaceLog(raceLog);
        updateStartOfRaceCacheFields();
        updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
        return raceLog;
    }

    @Override
    public RaceLog getRaceLog(Serializable identifier) {
        return attachedRaceLogs.get(identifier);
    }
    
    @Override
    public Distance getDistanceToStartLine(Competitor competitor, long millisecondsBeforeRaceStart) {
        final Distance result;
        if (getStartOfRace() == null) {
            result = null;
        } else {
            TimePoint beforeStart = new MillisecondsTimePoint(getStartOfRace().asMillis() - millisecondsBeforeRaceStart);
            result = getDistanceToStartLine(competitor, beforeStart);
        }
        return result;
    }

    @Override
    public Distance getDistanceToStartLine(Competitor competitor, TimePoint timePoint) {
        Waypoint startWaypoint = getRace().getCourse().getFirstWaypoint();
        final Distance result;
        if (startWaypoint == null) {
            result = null;
        } else {
            Position competitorPosition = getTrack(competitor).getEstimatedPosition(timePoint, /* extrapolate */ false);
            if (competitorPosition == null) {
                result = null;
            } else {
                Iterable<Mark> marks = startWaypoint.getControlPoint().getMarks();
                Iterator<Mark> marksIterator = marks.iterator();
                Mark first = marksIterator.next();
                Position firstPosition = getOrCreateTrack(first).getEstimatedPosition(timePoint, /* extrapolate */ false);
                if (firstPosition == null) {
                    result = null;
                } else {
                    if (marksIterator.hasNext()) {
                        // it's a line / gate
                        Mark second = marksIterator.next();
                        Position secondPosition = getOrCreateTrack(second).getEstimatedPosition(timePoint, /* extrapolate */ false);
                        if (secondPosition == null) {
                            result = null;
                        } else {
                            final Bearing lineBearingGreatCircleFromFirstToSecond = firstPosition.getBearingGreatCircle(secondPosition);
                            // if the competitor is outside of the line when projected orthogonally, compute the distance to
                            // the nearest of the line's marks (see also bug 1952):
                            final Bearing bearingFromFirstToCompetitor = firstPosition.getBearingGreatCircle(competitorPosition);
                            final Bearing angleBetweenFromFirstToCompetitorAndLine = lineBearingGreatCircleFromFirstToSecond.getDifferenceTo(bearingFromFirstToCompetitor);
                            if (angleBetweenFromFirstToCompetitorAndLine.getDegrees() < -90 || angleBetweenFromFirstToCompetitorAndLine.getDegrees() > 90) {
                                // competitor's orthogonal projection onto the line's extension is outside of the line's ends on the side
                                // of the first mark; use distance between competitor and first mark:
                                result = competitorPosition.getDistance(firstPosition);
                            } else {
                                final Bearing bearingFromSecondToCompetitor = secondPosition.getBearingGreatCircle(competitorPosition);
                                final Bearing angleBetweenFromSecondToCompetitorAndReversedLine = lineBearingGreatCircleFromFirstToSecond.reverse().getDifferenceTo(bearingFromSecondToCompetitor);
                                if (angleBetweenFromSecondToCompetitorAndReversedLine.getDegrees() < -90 || angleBetweenFromSecondToCompetitorAndReversedLine.getDegrees() > 90) {
                                    // competitor's orthogonal projection onto the line's extension is outside of the line's ends on the side
                                    // of the first mark; use distance between competitor and first mark:
                                    result = competitorPosition.getDistance(secondPosition);
                                } else {
                                    Position competitorProjectedOntoStartLine = competitorPosition.projectToLineThrough(
                                            firstPosition, lineBearingGreatCircleFromFirstToSecond);
                                    result = competitorPosition.getDistance(competitorProjectedOntoStartLine);
                                }
                            }
                        }
                    } else {
                        result = competitorPosition.getDistance(firstPosition);
                    }
                }
            }
        }
        return result;
    }
    
    @Override
    public Speed getSpeed(Competitor competitor, long millisecondsBeforeRaceStart) {
        if (getStartOfRace() == null) {
            return null;
        }

        TimePoint beforeStart = new MillisecondsTimePoint(getStartOfRace().asMillis() - millisecondsBeforeRaceStart);
        return getTrack(competitor).getEstimatedSpeed(beforeStart);
    }

    @Override
    public Distance getDistanceFromStarboardSideOfStartLineWhenPassingStart(Competitor competitor) {
        final Distance result;
        TrackedLegOfCompetitor firstTrackedLegOfCompetitor = getTrackedLeg(competitor, getRace().getCourse().getFirstLeg());
        TimePoint competitorStartTime = firstTrackedLegOfCompetitor.getStartTime();
        if (competitorStartTime != null) {
            Position competitorPositionWhenPassingStart = getTrack(competitor).getEstimatedPosition(
                    competitorStartTime, /* extrapolate */false);
            final Position starboardMarkPosition = getStarboardMarkOfStartlinePosition(competitorStartTime);
            if (competitorPositionWhenPassingStart != null && starboardMarkPosition != null) {
                result = starboardMarkPosition == null ? null : competitorPositionWhenPassingStart.getDistance(starboardMarkPosition);
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Distance getDistanceFromStarboardSideOfStartLine(Competitor competitor, TimePoint timePoint) {
        final Distance result;
        if (timePoint != null) {
            Position competitorPositionWhenPassingStart = getTrack(competitor).getEstimatedPosition(
                    timePoint, /* extrapolate */false);
            final Position starboardMarkPosition = getStarboardMarkOfStartlinePosition(timePoint);
            if (competitorPositionWhenPassingStart != null && starboardMarkPosition != null) {
                result = starboardMarkPosition == null ? null : competitorPositionWhenPassingStart.getDistance(starboardMarkPosition);
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }
    
    /**
     * Based on the bearing from the start waypoint to the next mark, identifies which of the two marks of the start
     * line is on starboard. If the start waypoint has only one mark, that mark is returned. If the start line has two
     * marks but the course has no other waypoint,
     * <code>null<code> is returned. If the course has no waypoints at all, <code>null</code> is returned.<p>
     * 
     * The method has protected visibility largely for testing purposes.
     */
    protected Mark getStarboardMarkOfStartlineOrSingleStartMark(TimePoint at) {
        Waypoint startWaypoint = getRace().getCourse().getFirstWaypoint();
        final Mark result;
        if (startWaypoint != null) {
            LineMarksWithPositions startLine = getLineMarksAndPositions(at, startWaypoint);
            if (startLine != null) {
                result = startLine.getStarboardMarkWhileApproachingLine();
            } else {
                if (startWaypoint != null && startWaypoint.getMarks().iterator().hasNext()) {
                    result = startWaypoint.getMarks().iterator().next();
                } else {
                    result = null;
                }
            }
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Based on the bearing from the start waypoint to the
     * next mark, identifies which of the two marks of the start line is on starboard and returns its position. If the start waypoint has only
     * one mark, that mark is returned. If the start line has two marks but the course has no other waypoint,
     * <code>null<code> is returned. If the course has no waypoints at all, <code>null</code> is returned.
     */
    private Position getStarboardMarkOfStartlinePosition(TimePoint at) {
        Mark starboardMark = getStarboardMarkOfStartlineOrSingleStartMark(at);
        if (starboardMark != null) {
            return getOrCreateTrack(starboardMark).getEstimatedPosition(at, /*extrapolate*/ false);
        }
        return null;
    }

    protected NamedReentrantReadWriteLock getLoadingFromWindStoreLock() {
        return loadingFromWindStoreLock;
    }

    public NamedReentrantReadWriteLock getLoadingFromGPSFixStoreLock() {
        return loadingFromGPSFixStoreLock;
    }
    
    private static class LineMarksWithPositions {
        private final Position portMarkPositionWhileApproachingLine;
        private final Position starboardMarkPositionWhileApproachingLine;
        private final Mark starboardMarkWhileApproachingLine;
        private final Mark portMarkWhileApproachingLine;
        protected LineMarksWithPositions(Position portMarkPositionWhileApproachingLine,
                Position starboardMarkPositionWhileApproachingLine, Mark starboardMarkWhileApproachingLine,
                Mark portMarkWhileApproachingLine) {
            this.portMarkPositionWhileApproachingLine = portMarkPositionWhileApproachingLine;
            this.starboardMarkPositionWhileApproachingLine = starboardMarkPositionWhileApproachingLine;
            this.starboardMarkWhileApproachingLine = starboardMarkWhileApproachingLine;
            this.portMarkWhileApproachingLine = portMarkWhileApproachingLine;
        }
        public Position getPortMarkPositionWhileApproachingLine() {
            return portMarkPositionWhileApproachingLine;
        }
        public Position getStarboardMarkPositionWhileApproachingLine() {
            return starboardMarkPositionWhileApproachingLine;
        }
        public Mark getStarboardMarkWhileApproachingLine() {
            return starboardMarkWhileApproachingLine;
        }
        public Mark getPortMarkWhileApproachingLine() {
            return portMarkWhileApproachingLine;
        }
    }
    
    /**
     * If the <code>waypoint</code> is not a line, or no position can be determined for one of its marks at <code>timePoint</code>,
     * <code>null</code> is returned. If no wind information is available but required to compute the advantage, <code>null</code> values
     * are returned in those fields that depend on wind data. If the <code>waypoint</code> is <code>null</code>
     * or is the only waypoint, <code>null</code> is returned because no reasonable statement can be
     * made about the direction from which the line is to be passed.
     */
    private LineDetails getLineLengthAndAdvantage(TimePoint timePoint, Waypoint waypoint) {
        LineMarksWithPositions marksAndPositions = getLineMarksAndPositions(timePoint, waypoint);
        LineDetails result = null;
        if (marksAndPositions != null) {
            final TrackedLeg legDeterminingDirection = getLegDeterminingDirectionInWhichToPassWaypoint(waypoint);
            final Mark portMarkWhileApproachingLine = marksAndPositions.getPortMarkWhileApproachingLine();
            final Mark starboardMarkWhileApproachingLine = marksAndPositions.getStarboardMarkWhileApproachingLine();
            final Position portMarkPositionWhileApproachingLine = marksAndPositions.getPortMarkPositionWhileApproachingLine();
            final Position starboardMarkPositionWhileApproachingLine = marksAndPositions.getStarboardMarkPositionWhileApproachingLine();
            final Bearing differenceToCombinedWind;
            final NauticalSide advantageousSideWhileApproachingLine;
            final Distance distanceAdvantage;
            Wind combinedWind = getWind(starboardMarkPositionWhileApproachingLine, timePoint);
            if (combinedWind != null) {
                differenceToCombinedWind = portMarkPositionWhileApproachingLine.getBearingGreatCircle(
                        starboardMarkPositionWhileApproachingLine).getDifferenceTo(combinedWind.getFrom());
                Distance windwardDistanceFromFirstToSecondMark;
                windwardDistanceFromFirstToSecondMark = legDeterminingDirection.getWindwardDistance(
                        portMarkPositionWhileApproachingLine, starboardMarkPositionWhileApproachingLine, timePoint,
                        WindPositionMode.EXACT);
                final Position worseMarkPosition;
                final Position betterMarkPosition;
                final int indexOfWaypoint = getRace().getCourse().getIndexOfWaypoint(waypoint);
                final boolean isStartLine = indexOfWaypoint == 0;
                if ((isStartLine && windwardDistanceFromFirstToSecondMark.getMeters() > 0)
                        || (!isStartLine && windwardDistanceFromFirstToSecondMark.getMeters() < 0)) {
                    // first mark is worse than second mark
                    worseMarkPosition = portMarkPositionWhileApproachingLine;
                    betterMarkPosition = starboardMarkPositionWhileApproachingLine;
                } else {
                    // second mark is worse than first mark
                    worseMarkPosition = starboardMarkPositionWhileApproachingLine;
                    betterMarkPosition = portMarkPositionWhileApproachingLine;
                }
                if (windwardDistanceFromFirstToSecondMark.getMeters() >= 0) {
                    distanceAdvantage = windwardDistanceFromFirstToSecondMark;
                } else {
                    distanceAdvantage = new CentralAngleDistance(
                            -windwardDistanceFromFirstToSecondMark.getCentralAngleRad());
                }
                if (betterMarkPosition.crossTrackError(worseMarkPosition,
                        legDeterminingDirection.getLegBearing(timePoint)).getCentralAngleRad() > 0) {
                    advantageousSideWhileApproachingLine = NauticalSide.STARBOARD;
                } else {
                    advantageousSideWhileApproachingLine = NauticalSide.PORT;
                }
            } else { // no wind information
                differenceToCombinedWind = null;
                advantageousSideWhileApproachingLine = null;
                distanceAdvantage = null;
            }
            result = new LineDetailsImpl(timePoint, waypoint,
                    portMarkPositionWhileApproachingLine.getDistance(starboardMarkPositionWhileApproachingLine),
                    differenceToCombinedWind, advantageousSideWhileApproachingLine, distanceAdvantage,
                    portMarkWhileApproachingLine, starboardMarkWhileApproachingLine);
        }
        return result;
    }

    /**
     * For a waypoint that is assumed to be a line, determines which mark is to port when approaching the waypoint and which one
     * is to starboard. Additionally, the mark positions at the time point specified is returned.
     */
    private LineMarksWithPositions getLineMarksAndPositions(TimePoint timePoint, Waypoint waypoint) {
        final LineMarksWithPositions result;
        List<Position> markPositions = new ArrayList<>();
        int numberOfMarks = 0;
        boolean allMarksHavePositions = true;
        if (waypoint != null) {
            for (Mark lineMark : waypoint.getMarks()) {
                numberOfMarks++;
                final Position estimatedMarkPosition = getOrCreateTrack(lineMark).getEstimatedPosition(timePoint, /* extrapolate */ false);
                if (estimatedMarkPosition != null) {
                    markPositions.add(estimatedMarkPosition);
                } else {
                    allMarksHavePositions = false;
                }
            }
            final List<Leg> legs = getRace().getCourse().getLegs();
            // need at least one leg to make sense of a line
            if (!legs.isEmpty()) {
                if (allMarksHavePositions && numberOfMarks == 2) {
                    final TrackedLeg legDeterminingDirection = getLegDeterminingDirectionInWhichToPassWaypoint(waypoint);
                    final Bearing legBearing;
                    if (legDeterminingDirection == null || (legBearing = legDeterminingDirection.getLegBearing(timePoint)) == null) {
                        result = null;
                    } else {
                        Distance crossTrackErrorOfMark0OnLineFromMark1ToNextWaypoint = markPositions.get(0)
                                .crossTrackError(markPositions.get(1), legBearing);
                        final Position portMarkPositionWhileApproachingLine;
                        final Position starboardMarkPositionWhileApproachingLine;
                        final Mark starboardMarkWhileApproachingLine;
                        final Mark portMarkWhileApproachingLine;
                        if (crossTrackErrorOfMark0OnLineFromMark1ToNextWaypoint.getMeters() < 0) {
                            portMarkWhileApproachingLine = Util.get(waypoint.getMarks(), 0);
                            portMarkPositionWhileApproachingLine = markPositions.get(0);
                            starboardMarkWhileApproachingLine = Util.get(waypoint.getMarks(), 1);
                            starboardMarkPositionWhileApproachingLine = markPositions.get(1);
                        } else {
                            portMarkWhileApproachingLine = Util.get(waypoint.getMarks(), 1);
                            portMarkPositionWhileApproachingLine = markPositions.get(1);
                            starboardMarkWhileApproachingLine = Util.get(waypoint.getMarks(), 0);
                            starboardMarkPositionWhileApproachingLine = markPositions.get(0);
                        }
                        result = new LineMarksWithPositions(portMarkPositionWhileApproachingLine,
                                starboardMarkPositionWhileApproachingLine, starboardMarkWhileApproachingLine,
                                portMarkWhileApproachingLine);
                    }
                } else {
                    result = null; // either the position(s) or one or more marks is/are unknown, or the waypoint is not a two-mark waypoint
                }
            } else {
                result = null; // the waypoint was the only waypoint, so no leg exists to determine approaching direction
            }
        } else {
            result = null; // waypoint was null
        }
        return result;
    }

    private TrackedLeg getLegDeterminingDirectionInWhichToPassWaypoint(Waypoint waypoint) {
        final TrackedLeg legDeterminingDirection;
        final int indexOfWaypoint = getRace().getCourse().getIndexOfWaypoint(waypoint);
        final boolean isStartLine = indexOfWaypoint == 0;
        legDeterminingDirection = getTrackedLeg(getRace().getCourse().getLegs().get(isStartLine ? 0
                : indexOfWaypoint - 1));
        return legDeterminingDirection;
    }


    @Override
    public LineDetails getStartLine(TimePoint at) {
        return getLineLengthAndAdvantage(at, getRace().getCourse().getFirstWaypoint());
    }

    @Override
    public LineDetails getFinishLine(TimePoint at) {
        return getLineLengthAndAdvantage(at, getRace().getCourse().getLastWaypoint());
    }

    @Override
    public SpeedWithConfidence<TimePoint> getAverageWindSpeedWithConfidence(long resolutionInMillis) {
        SpeedWithConfidence<TimePoint> result = null;
        if (getEndOfRace() != null) {
            TimePoint fromTimePoint = getStartOfRace();
            TimePoint toTimePoint = getEndOfRace();

            List<WindSource> windSourcesToDeliver = new ArrayList<WindSource>();
            WindSourceImpl windSource = new WindSourceImpl(WindSourceType.COMBINED);
            windSourcesToDeliver.add(windSource);

            double sumWindSpeed = 0.0;
            double sumWindSpeedConfidence = 0.0;
            int speedCounter = 0;

            int numberOfFixes = (int) ((toTimePoint.asMillis() - fromTimePoint.asMillis()) / resolutionInMillis);
            WindTrack windTrack = getOrCreateWindTrack(windSource);
            TimePoint timePoint = fromTimePoint;
            for (int i = 0; i < numberOfFixes && toTimePoint != null && timePoint.compareTo(toTimePoint) < 0; i++) {
                WindWithConfidence<Pair<Position, TimePoint>> averagedWindWithConfidence = windTrack
                        .getAveragedWindWithConfidence(null, timePoint);
                if (averagedWindWithConfidence != null) {
                    double windSpeedinKnots = averagedWindWithConfidence.getObject().getKnots();
                    double confidence = averagedWindWithConfidence.getConfidence();

                    sumWindSpeed += windSpeedinKnots;
                    sumWindSpeedConfidence += confidence;

                    speedCounter++;
                }
                timePoint = new MillisecondsTimePoint(timePoint.asMillis() + resolutionInMillis);
            }
            if (speedCounter > 0) {
                Speed averageWindSpeed = new KnotSpeedImpl(sumWindSpeed / speedCounter);
                double averageWindSpeedConfidence = sumWindSpeedConfidence / speedCounter;
                result = new SpeedWithConfidenceImpl<TimePoint>(averageWindSpeed, averageWindSpeedConfidence, toTimePoint);
            }
        } 
        return result;
    }

    @Override
    public Distance getCourseLength() {
        Distance d = Distance.NULL;
        for (TrackedLeg trackedLeg : getTrackedLegs()) {
            d = d.add(trackedLeg.getWindwardDistance());
        }
        return d;
    }
    
    @Override
    public Speed getSpeedWhenCrossingStartLine(Competitor competitor) {
        NavigableSet<MarkPassing> competitorMarkPassings = getMarkPassings(competitor);
        Speed competitorSpeedWhenPassingStart = null;
        lockForRead(competitorMarkPassings);
        try {
            if (!competitorMarkPassings.isEmpty()) {
                TimePoint competitorStartTime = competitorMarkPassings.first().getTimePoint();
                competitorSpeedWhenPassingStart = getTrack(competitor).getEstimatedSpeed(
                        competitorStartTime);
            }
        } finally {
            unlockAfterRead(competitorMarkPassings);
        }
        return competitorSpeedWhenPassingStart;
    }
    
    protected abstract MarkPassingCalculator createMarkPassingCalculator();
    
    @Override
    public boolean isUsingMarkPassingCalculator() {
        return markPassingCalculator!=null;
    }

    @Override
    public Position getCenterOfCourse(TimePoint at) {
        int count = 0;
        ScalablePosition sum = null;
        final MarkPositionAtTimePointCache cache = new MarkPositionAtTimePointCacheImpl(this, at);
        for (Waypoint waypoint : getRace().getCourse().getWaypoints()) {
            final Position waypointPosition = getApproximatePosition(waypoint, at, cache);
            if (waypointPosition != null) {
                ScalablePosition p = new ScalablePosition(waypointPosition);
                if (sum == null) {
                    sum = p;
                } else {
                    sum = sum.add(p);
                }
            }
        }
        final Position result;
        if (sum == null) {
            result = null;
        } else {
            result = sum.divide(count);
        }
        return result;
    }

    /**
     * @return the waypoints known by this race, based on the key set of {@link #markPassingsForWaypoint}. This key set
     *         is updated by {@link #waypointAdded(int, Waypoint)} and {@link #waypointRemoved(int, Waypoint)} and hence
     *         is consistent with the {@link Course}'s waypoint list after the callback methods have returned. The
     *         iteration order of the elements returned is undefined and in particular is <em>not</em> guaranteed to be
     *         related to the {@link Course}'s waypoint order.
     */
    Iterable<Waypoint> getWaypoints() {
        return markPassingsForWaypoint.keySet();
    }

    @Override
    public Boolean isGateStart() {
        Boolean result = null;
        for (RaceLog raceLog : attachedRaceLogs.values()) {
            ReadonlyRaceState raceState = getRaceState(raceLog);
            ReadonlyRacingProcedure procedure = raceState.getRacingProcedureNoFallback();
            if (procedure != null && procedure.getType() != null) {
                result = procedure.getType() == RacingProcedureType.GateStart;
                break;
            }
        }
        return result;
    }
    
    @Override
    public long getGateStartGolfDownTime() {
        long result = 0;
        Boolean isGateStart = isGateStart();
        if (isGateStart != null && isGateStart.booleanValue() == true) {
            for (RaceLog raceLog : attachedRaceLogs.values()) {
                raceLog.lockForRead();
                for(RaceLogEvent raceLogEvent: raceLog.getRawFixes()) {
                    if(raceLogEvent.getClass().equals(RaceLogGateLineOpeningTimeEventImpl.class)){
                        RaceLogGateLineOpeningTimeEvent raceLogGateLineOpeningTimeEvent = (RaceLogGateLineOpeningTimeEvent) raceLogEvent;
                        result = raceLogGateLineOpeningTimeEvent.getGateLineOpeningTimes().getGolfDownTime();
                    }
                }
                raceLog.unlockAfterRead();
            }
        }
        return result;
    }

    @Override
    public Distance getAdditionalGateStartDistance(Competitor competitor, TimePoint timePoint) {
        final Distance result;
        final Leg startLeg = getRace().getCourse().getFirstLeg();
        final TrackedLegOfCompetitor competitorLeg;
        if (startLeg != null && isGateStart() == Boolean.TRUE && (competitorLeg=getTrackedLeg(competitor, startLeg)).hasStartedLeg(timePoint)) {
            TimePoint competitorLegStartTime = competitorLeg.getStartTime();
            final Mark portMarkOfStartLine = getStartLine(competitorLegStartTime).getPortMarkWhileApproachingLine();
            final Position portSideOfStartLinePosition = getOrCreateTrack(portMarkOfStartLine)
                    .getEstimatedPosition(competitorLegStartTime, /* extrapolate */true);
            result = portSideOfStartLinePosition.getDistance(getTrack(competitor).getEstimatedPosition(competitorLegStartTime, /* extrapolate */false));
        } else {
            result = Distance.NULL;
        }
        return result;
    }
    
    @Override
    public TargetTimeInfo getEstimatedTimeToComplete(final TimePoint timepoint) throws NotEnoughDataHasBeenAddedException,
            NoWindException {
       if (polarDataService == null) {
            throw new NotEnoughDataHasBeenAddedException("Target time estimation failed. No polar service available.");
        }
        Duration durationOfAllLegs = Duration.NULL;
        TimePoint current = timepoint;
        final List<LegTargetTimeInfo> legTargetTimes = new ArrayList<>();
        for (TrackedLeg leg : trackedLegs.values()) {
            final MarkPositionAtTimePointCache markPositionCache = new MarkPositionAtTimePointCacheImpl(this, current);
            LegTargetTimeInfo legTargetTime = leg.getEstimatedTimeAndDistanceToComplete(polarDataService, current, markPositionCache);
            legTargetTimes.add(legTargetTime);
            durationOfAllLegs = durationOfAllLegs.plus(legTargetTime.getExpectedDuration());
            current = current.plus(legTargetTime.getExpectedDuration()); // simulate the next leg with the wind as of the projected finishing time of the previous leg
        }
        return new TargetTimeInfoImpl(legTargetTimes);
    }
    
    @Override
    public Distance getEstimatedDistanceToComplete(final TimePoint timepoint)
            throws NotEnoughDataHasBeenAddedException, NoWindException {
        if (polarDataService == null) {
            throw new NotEnoughDataHasBeenAddedException("Target time estimation failed. No polar service available.");
        }
        Distance distanceOfAllLegs = Distance.NULL;
        TimePoint current = timepoint;
        final List<LegTargetTimeInfo> legTargetTimes = new ArrayList<>();
        for (TrackedLeg leg : trackedLegs.values()) {
            final MarkPositionAtTimePointCache markPositionCache = new MarkPositionAtTimePointCacheImpl(this, current);
            LegTargetTimeInfo legTargetTime = leg.getEstimatedTimeAndDistanceToComplete(polarDataService, current,
                    markPositionCache);
            legTargetTimes.add(legTargetTime);
            distanceOfAllLegs = distanceOfAllLegs.add(legTargetTime.getExpectedDistance());
            current = current.plus(legTargetTime.getExpectedDuration()); // simulate the next leg with the wind as of
                                                                         // the projected finishing time of the previous
                                                                         // leg
        }
        return distanceOfAllLegs;
    }

    @Override
    public void setPolarDataService(PolarDataService polarDataService) {
        this.polarDataService = polarDataService;
    }

    /**
     * Obtains the {@link #raceLogResolver}.
     */
    @Override
    public RaceLogResolver getRaceLogResolver() {
        return raceLogResolver;
    }

    public void setRaceLogResolver(RaceLogResolver raceLogResolver) {
        this.raceLogResolver = raceLogResolver;
    }

    /**
     * When given the opportunity to resolve after de-serialization, grabs the {@link RaceLogResolver} from the
     * {@link SharedDomainFactory} because the field is transient and needs filling after de-serialization.
     */
    @Override
    public IsManagedByCache<SharedDomainFactory> resolve(SharedDomainFactory domainFactory) {
        this.raceLogResolver = domainFactory.getRaceLogResolver();
        return this;
    }
    
    @Override
    public Iterable<Mark> getMarksFromRegattaLogs() {
         final Set<Mark> result = new HashSet<>();
         for (RegattaLog log : attachedRegattaLogs.values()) {
             result.addAll(new RegattaLogDefinedMarkAnalyzer(log).analyze());
         }
         return result;
    }
    
    public <FixT extends SensorFix, TrackT extends SensorFixTrack<Competitor, FixT>> TrackT getSensorTrack(
            Competitor competitor, String trackName) {
        Pair<Competitor, String> key = new Pair<>(competitor, trackName);
        LockUtil.lockForRead(sensorTracksLock);
        try {
            return getTrackInternal(key);
        } finally {
            LockUtil.unlockAfterRead(sensorTracksLock);
        }
    }
    
    @Override
    public <FixT extends SensorFix, TrackT extends SensorFixTrack<Competitor, FixT>> Iterable<TrackT> getSensorTracks(
            String trackName) {
        return LockUtil.<Iterable<TrackT>>executeWithReadLockAndResult(sensorTracksLock, () -> {
            final Set<TrackT> result = new HashSet<>();
            for (Competitor competitor : tracks.keySet()) {
                final Pair<Competitor, String> key = new Pair<>(competitor, trackName);
                final TrackT track = getTrackInternal(key);
                if (track != null) {
                    result.add(track);
                }
            }
            return result;
        });
    }

    protected <FixT extends SensorFix, TrackT extends DynamicSensorFixTrack<Competitor, FixT>> TrackT getOrCreateSensorTrack(
            Competitor competitor, String trackName, TrackFactory<TrackT> newTrackFactory) {
        Pair<Competitor, String> key = new Pair<>(competitor, trackName);
        LockUtil.lockForWrite(sensorTracksLock);
        try {
            TrackT result = getTrackInternal(key);
            if (result == null && tracks.containsKey(competitor)) {
                // A track is only added if the given Competitor is known to participate in this race
                result = newTrackFactory.get();
                addSensorTrackInternal(key, result);
            }
            return result;
        } finally {
            LockUtil.unlockAfterWrite(sensorTracksLock);
        }
    }
    
    protected void addSensorTrack(Competitor competitor, String trackName, DynamicSensorFixTrack<Competitor, ?> track) {
        Pair<Competitor, String> key = new Pair<>(competitor, trackName);
        LockUtil.lockForWrite(sensorTracksLock);
        try {
            if(getTrackInternal(key) != null) {
                if (logger != null && logger.getLevel() != null && logger.getLevel().equals(Level.WARNING)) {
                    logger.warning(SensorFixTrack.class.getName() + " already exists for competitor: "
                            + competitor.getName() + "; trackName: " + trackName);
                }
            } else {
                this.addSensorTrackInternal(key, track);
            }
        } finally {
            LockUtil.unlockAfterWrite(sensorTracksLock);
        }
    }
    
    protected <FixT extends SensorFix> void addSensorTrackInternal(Pair<Competitor, String> key,
            DynamicSensorFixTrack<Competitor, FixT> track) {
        sensorTracks.put(key, track);
    }

    @SuppressWarnings("unchecked")
    private <TrackT extends SensorFixTrack<Competitor, ?>> TrackT getTrackInternal(Pair<Competitor, String> key) {
        return (TrackT) sensorTracks.get(key);
    }
    
    protected abstract Set<RaceChangeListener> getListeners();

    protected void notifyListeners(Consumer<RaceChangeListener> notifyAction) {
        RaceChangeListener[] listeners;
        synchronized (getListeners()) {
            listeners = getListeners().toArray(new RaceChangeListener[getListeners().size()]);
        }
        for (RaceChangeListener listener : listeners) {
            try {
                notifyAction.accept(listener);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "RaceChangeListener " + listener + " threw exception " + e.getMessage());
                logger.log(Level.SEVERE, "notifyListeners(Consumer<RaceChangeListener> notifyAction", e);
            }
        }
    }
    
    private void notifyListenersWhenAttachingRegattaLog(RegattaLog regattaLog) {
        notifyListeners(listener -> listener.regattaLogAttached(regattaLog));
    }
    
    private void notifyListenersWhenAttachingRaceLog(RaceLog raceLog) {
        notifyListeners(listener -> listener.raceLogAttached(raceLog));
    }
    
    private void notifyListenersWhenDetachingRaceLog(RaceLog raceLog) {
        notifyListeners(listener -> listener.raceLogDetached(raceLog));
    }

    public void lockForSerializationRead() {
        LockUtil.lockForRead(getSerializationLock());
    }
    
    public void unlockAfterSerializationRead() {
        LockUtil.unlockAfterRead(getSerializationLock());
    }
    
    @Override
    public Iterable<RaceLog> getAttachedRaceLogs() {
        return new HashSet<>(attachedRaceLogs.values());
    }
    
    @Override
    public Speed getAverageSpeedOverGround(Competitor competitor, TimePoint timePoint) {
        Speed result = null;
        Duration totalTimeSailedInRace = Duration.NULL;
        Distance totalDistanceSailedInRace = Distance.NULL;
        for (TrackedLeg legGeneral : getTrackedLegs()) {
            TrackedLegOfCompetitor leg = legGeneral.getTrackedLeg(competitor);
            if (leg != null && leg.hasStartedLeg(timePoint)) {
                totalDistanceSailedInRace = totalDistanceSailedInRace.add(leg.getDistanceTraveled(timePoint));
                totalTimeSailedInRace = totalTimeSailedInRace.plus(leg.getTime(timePoint));
            }
        }
        if (!totalTimeSailedInRace.equals(Duration.NULL) && !totalDistanceSailedInRace.equals(Distance.NULL)) {
            result = totalDistanceSailedInRace.inTime(totalTimeSailedInRace);
        }
        return result;
    }
    
    private static class ComputedManeuverSectionExtension {
        private final TimePoint extensionTimePoint;
        private final SpeedWithBearing speedWithBearingAtExtensionTimePoint;
        private final double totalCourseChangeInDegreesExtension;

        public ComputedManeuverSectionExtension(TimePoint extensionTimePoint,
                SpeedWithBearing speedWithBearingAtExtensionTimePoint, double totalCourseChangeInDegreesExtension) {
            this.extensionTimePoint = extensionTimePoint;
            this.speedWithBearingAtExtensionTimePoint = speedWithBearingAtExtensionTimePoint;
            this.totalCourseChangeInDegreesExtension = totalCourseChangeInDegreesExtension;
        }

        public TimePoint getExtensionTimePoint() {
            return extensionTimePoint;
        }

        public SpeedWithBearing getSpeedWithBearingAtExtensionTimePoint() {
            return speedWithBearingAtExtensionTimePoint;
        }

        public double getTotalCourseChangeInDegreesExtension() {
            return totalCourseChangeInDegreesExtension;
        }
    }

    private static class ManeuverEnteringAndExitingDetails {
        private final TimePoint timePointBefore;
        private final TimePoint timePointAfter;
        private final SpeedWithBearing speedWithBearingBefore;
        private final SpeedWithBearing speedWithBearingAfter;

        public ManeuverEnteringAndExitingDetails(TimePoint timePointBefore, TimePoint timePointAfter,
                SpeedWithBearing speedWithBearingBefore, SpeedWithBearing speedWithBearingAfter) {
            this.timePointBefore = timePointBefore;
            this.timePointAfter = timePointAfter;
            this.speedWithBearingBefore = speedWithBearingBefore;
            this.speedWithBearingAfter = speedWithBearingAfter;
        }

        /**
         * Gets the computed time point of maneuver start.
         * 
         * @return The time point of maneuver start
         */
        public TimePoint getTimePointBefore() {
            return timePointBefore;
        }

        /**
         * Gets the computed time point of maneuver end.
         * 
         * @return The time point of maneuver end
         */
        public TimePoint getTimePointAfter() {
            return timePointAfter;
        }

        /**
         * Gets the speed with bearing at maneuver start.
         * 
         * @return The speed with bearing at maneuver start
         */
        public SpeedWithBearing getSpeedWithBearingBefore() {
            return speedWithBearingBefore;
        }

        /**
         * Gets the speed with bearing at maneuver end.
         * 
         * @return The speed with bearing at maneuver end
         */
        public SpeedWithBearing getSpeedWithBearingAfter() {
            return speedWithBearingAfter;
        }
    }

    private static class ManeuverCurveDetails extends ManeuverEnteringAndExitingDetails {
        private final TimePoint timePoint;
        private final double totalChangeInDegrees;

        public ManeuverCurveDetails(TimePoint timePointBefore, TimePoint timePointAfter, TimePoint timePoint,
                SpeedWithBearing speedWithBearingBefore, SpeedWithBearing speedWithBearingAfter,
                double totalCourseChangeInDegrees) {
            super(timePointBefore, timePointAfter, speedWithBearingBefore, speedWithBearingAfter);
            this.timePoint = timePoint;
            this.totalChangeInDegrees = totalCourseChangeInDegrees;
        }

        /**
         * Gets the computed time point of the corresponding maneuver. The time point refers to a position within
         * maneuver, where the highest course change has been recorded.
         * 
         * @return The computed maneuver time point
         */
        public TimePoint getTimePoint() {
            return timePoint;
        }

        /**
         * Gets the total course change performed within maneuver in degrees. The port side course changes are negative.
         * 
         * @return The total course change in degrees
         */
        public double getTotalCourseChangeInDegrees() {
            return totalChangeInDegrees;
        }
    }

    private static class ManeuverDetailsWithBearingSteps extends ManeuverCurveDetails {
        private final List<SpeedWithBearingStep> maneuverBearingSteps;

        public ManeuverDetailsWithBearingSteps(TimePoint timepointBefore, TimePoint timepointAfter, TimePoint timepoint,
                SpeedWithBearing speedWithBearingBefore, SpeedWithBearing speedWithBearingAfter,
                double totalCourseChangeInDegrees, List<SpeedWithBearingStep> maneuverBearingSteps) {
            super(timepointBefore, timepointAfter, timepoint, speedWithBearingBefore, speedWithBearingAfter,
                    totalCourseChangeInDegrees);
            this.maneuverBearingSteps = maneuverBearingSteps;
        }

        /**
         * Gets the list of bearing steps which was used for maneuver details computation.
         * 
         * @return The bearing steps of maneuver
         */
        public List<SpeedWithBearingStep> getManeuverBearingSteps() {
            return maneuverBearingSteps;
        }
    }
}
