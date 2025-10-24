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
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;

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
import com.sap.sailing.domain.abstractlog.race.state.impl.ReadonlyRaceStateImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.ReadonlyRacingProcedure;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogDefinedMarkAnalyzer;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.CPUMeteringType;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseListener;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RegattaListener;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.SpeedWithConfidenceImpl;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RaceTimesCalculationUtil;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
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
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.TargetTimeInfoImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.common.scalablevalue.impl.ScalablePosition;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.SensorFix;
import com.sap.sailing.domain.confidence.ConfidenceBasedWindAverager;
import com.sap.sailing.domain.confidence.ConfidenceFactory;
import com.sap.sailing.domain.leaderboard.Leaderboard.RankComparableRank;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.leaderboard.impl.CompetitorAndRankComparable;
import com.sap.sailing.domain.leaderboard.impl.RankAndRankComparable;
import com.sap.sailing.domain.maneuverdetection.IncrementalManeuverDetector;
import com.sap.sailing.domain.maneuverdetection.ManeuverDetector;
import com.sap.sailing.domain.maneuverdetection.ShortTimeAfterLastHitCache;
import com.sap.sailing.domain.maneuverdetection.impl.IncrementalManeuverDetectorImpl;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingCalculator;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.orc.ORCPerformanceCurveRankingMetric;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.ranking.RankingMetric;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sailing.domain.shared.tracking.AddResult;
import com.sap.sailing.domain.shared.tracking.LineDetails;
import com.sap.sailing.domain.shared.tracking.Track;
import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sailing.domain.shared.tracking.impl.LineDetailsImpl;
import com.sap.sailing.domain.shared.tracking.impl.TimedComparator;
import com.sap.sailing.domain.ranking.RankingMetricConstructor;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.DynamicSensorFixTrack;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.GPSTrackListener;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.MarkPositionAtTimePointCache;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.RaceExecutionOrderProvider;
import com.sap.sailing.domain.tracking.RaceListener;
import com.sap.sailing.domain.tracking.SensorFixTrack;
import com.sap.sailing.domain.tracking.TrackFactory;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.TrackedRaceWithWindEssentials;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindSummary;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.windestimation.IncrementalWindEstimation;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.IsManagedByCache;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.shared.util.impl.ApproximateTime;
import com.sap.sse.shared.util.impl.ArrayListNavigableSet;
import com.sap.sse.util.IdentityWrapper;
import com.sap.sse.util.SmartFutureCache;
import com.sap.sse.util.SmartFutureCache.AbstractCacheUpdater;
import com.sap.sse.util.SmartFutureCache.EmptyUpdateInterval;
import com.sap.sse.util.impl.FutureTaskWithTracingGet;

import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;

/**
 * Note to subclasses: you have to override {@link #readResolve()} and invoke
 * {@code super.readResolve()} in the override to ensure that this class's
 * implementation gets called.<p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public abstract class TrackedRaceImpl extends TrackedRaceWithWindEssentials implements CourseListener {

    private static final long serialVersionUID = -4825546964220003507L;

    private static final Logger logger = Logger.getLogger(TrackedRaceImpl.class.getName());

    private static final long DELAY_FOR_CACHE_CLEARING_IN_MILLISECONDS = 7500;

    public static final Duration TIME_BEFORE_START_TO_TRACK_WIND_MILLIS = Duration.ONE_MINUTE.times(4); // let wind start four minutes before race

    public static final Duration EXTRA_LONG_TIME_BEFORE_START_TO_TRACK_WIND_MILLIS = Duration.ONE_HOUR;

    private TrackedRaceStatus status;

    private final TrackingConnectorInfo trackingConnectorInfo;

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
     * The time set by race management ("Blue Flag Up" event) for when the first competitor has finished.
     */
    private TimePoint finishingTime;

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

    private transient LinkedHashMap<TimePoint, LinkedHashMap<Competitor, RankAndRankComparable>> competitorRankings;

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
    private transient SmartFutureCache<Competitor, List<Maneuver>, EmptyUpdateInterval> maneuverCache;

    /**
     * The values of this map are used by the {@link #approximate(Competitor, Distance, TimePoint, TimePoint)} method and
     * maintain state to accelerate the {@link #approximate(Competitor, Distance, TimePoint, TimePoint)} method, also in
     * live scenarios when the contents of the competitors' {@link #tracks} changes dynamically.
     */
    private final Map<Competitor, CourseChangeBasedTrackApproximation> maneuverApproximators;

    private transient ConcurrentMap<TimePoint, Future<Wind>> directionFromStartToNextMarkCache;

    protected transient MarkPassingCalculator markPassingCalculator;

    private final ConcurrentMap<Mark, GPSFixTrack<Mark, GPSFix>> markTracks;

    /**
     * Mapping of {@link Competitor} to generic {@link DynamicSensorFixTrack} implementation. Because the same competitor could
     * be mapped to several different tracks, a combined key of competitor object and track name identifier string is
     * used. This identifier is usually defined within the track interface (e.g. see {@link BravoFixTrack#TRACK_NAME}).
     */
    private final Map<Pair<Competitor, String>, DynamicSensorFixTrack<Competitor, ?>> sensorTracks;

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

    private transient volatile IncrementalWindEstimation windEstimation;

    private transient ShortTimeAfterLastHitCache<Competitor, IncrementalManeuverDetector> maneuverDetectorPerCompetitorCache;

    /**
     * Tells how ranks are to be assigned to the competitors at any time during the race. For one-design boat classes
     * this will usually happen by projecting the competitors to the wind direction for upwind and downwind legs or to
     * the leg's rhumb line for reaching legs, then comparing positions. For handicap races using a time-on-time,
     * time-on-distance, combination thereof or a more complicated scheme such as ORC Polar Curve, the ranking
     * process needs to take into account the competitor-specific correction factors defined in the measurement
     * certificate.<p>
     * 
     * The RankingMetric for all {@link TrackedRace} within one {@link Regatta} must be the same. 
     */
    private final RankingMetric rankingMetric;

    /**
     * Required in particular to resolve {@link SimpleRaceLogIdentifier}s that appear in
     * {@link RaceLogDependentStartTimeEvent}s. The usual implementation on the server side is
     * provided by <code>RacingEventService</code> which is not serializable. Therefore, the reference
     * must be established again after de-serialization by invoking {@link #setRaceLogResolver}.
     */
    private transient RaceLogAndTrackedRaceResolver raceLogResolver;

    private final NamedReentrantReadWriteLock sensorTracksLock;

    private static final int MAX_DISTANCES_FROM_STARBOARD_SIDE_OF_START_LINE_PROJECTED_ONTO_LINE_CACHE_SIZE = 10;
    private transient ConcurrentMap<TimePoint, SortedMap<Competitor, Distance>> distancesFromStarboardSideOfStartLineProjectedOntoLineCache;
    private transient ConcurrentMap<TimePoint, TimePoint> distancesFromStarboardSideOfStartLineProjectedOntoLineCacheLastAccessTimes;

    /**
     * When a regatta's {@link Regatta#useStartTimeInference()} or {@link Regatta#isControlTrackingFromStartAndFinishTimes()}
     * changes, the tracking start/end times need to be recalculated. This regatta listener handles this. It has to be
     * added to the tracked race's underlying regatta at construction time and after de-serialization (regatta listeners
     * are transient and are not serialized together with the regatta).
     *
     * @author Axel Uhl (D043530)
     *
     */
    private class TimingUpdaterCallback implements RegattaListener {
        @Override
        public void useStartTimeInferenceChanged(Regatta regatta, boolean newUseStartTimeInference) {
            updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
        }

        @Override
        public void controlTrackingFromStartAndFinishTimesChanged(Regatta regatta,
                boolean newControlTrackingFromStartAndFinishTimes) {
            updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
        }
    }

    /**
     * Constructs the tracked race with one-design ranking.
     */
    public TrackedRaceImpl(final TrackedRegatta trackedRegatta, RaceDefinition race, final Iterable<Sideline> sidelines,
            final WindStore windStore, long delayToLiveInMillis, final long millisecondsOverWhichToAverageWind,
            long millisecondsOverWhichToAverageSpeed, long delayForWindEstimationCacheInvalidation,
            boolean useInternalMarkPassingAlgorithm, RaceLogAndTrackedRaceResolver raceLogResolver, TrackingConnectorInfo trackingConnectorInfo,
            MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry) {
        this(trackedRegatta, race, sidelines, windStore, delayToLiveInMillis, millisecondsOverWhichToAverageWind,
                millisecondsOverWhichToAverageSpeed, delayForWindEstimationCacheInvalidation,
                useInternalMarkPassingAlgorithm, OneDesignRankingMetric::new, raceLogResolver, trackingConnectorInfo,
                markPassingRaceFingerprintRegistry);
    }

    /**
     * Constructs the tracked race with a configurable ranking metric. All {@link TrackedRace}s use the same
     * {@link RankingMetric}. This is given by the fact that this constructor is only called in the
     * {@link DynamicTrackedRaceImpl#DynamicTrackedRaceImpl(TrackedRegatta, RaceDefinition, Iterable, WindStore, long, long, long, long, boolean, RankingMetricConstructor, RaceLogAndTrackedRaceResolver, TrackingConnectorInfo)}
     * constructor which itself is only called from
     * {@link TrackedRegattaImpl#createTrackedRace(RaceDefinition, Iterable, WindStore, long, long, long, com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet, boolean, RaceLogAndTrackedRaceResolver, Optional, TrackingConnectorInfo)}
     * 
     * @param rankingMetricConstructor
     *            the function that creates the ranking metric, passing this tracked race as argument. Callers may use a
     *            constructor method reference if the {@link RankingMetric} implementation to instantiate takes a single
     *            {@link TrackedRace} argument.
     */
    public TrackedRaceImpl(final TrackedRegatta trackedRegatta, RaceDefinition race, final Iterable<Sideline> sidelines,
            final WindStore windStore, long delayToLiveInMillis, final long millisecondsOverWhichToAverageWind,
            long millisecondsOverWhichToAverageSpeed, long delayForWindEstimationCacheInvalidation,
            boolean useInternalMarkPassingAlgorithm, RankingMetricConstructor rankingMetricConstructor,
            RaceLogAndTrackedRaceResolver raceLogResolver, TrackingConnectorInfo trackingConnectorInfo,
            MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry) {
        super(race, trackedRegatta, windStore, millisecondsOverWhichToAverageWind);
        distancesFromStarboardSideOfStartLineProjectedOntoLineCache = new ConcurrentHashMap<>();
        distancesFromStarboardSideOfStartLineProjectedOntoLineCacheLastAccessTimes = new ConcurrentHashMap<>();
        registerRegattaListener();
        this.raceLogResolver = raceLogResolver;
        this.trackingConnectorInfo = trackingConnectorInfo;
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
        this.maneuverDetectorPerCompetitorCache = createManeuverDetectorCache();
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
        markPassingsForCompetitor = new HashMap<>();
        tracks = new HashMap<>();
        maneuverApproximators = new HashMap<>();
        for (Competitor competitor : race.getCompetitors()) {
            markPassingsForCompetitor.put(competitor, new ConcurrentSkipListSet<MarkPassing>(MarkPassingByTimeComparator.INSTANCE));
            final DynamicGPSFixMovingTrackImpl<Competitor> track = new DynamicGPSFixMovingTrackImpl<Competitor>(competitor, millisecondsOverWhichToAverageSpeed);
            tracks.put(competitor, track);
            maneuverApproximators.put(competitor, new CourseChangeBasedTrackApproximation(track, race.getBoatOfCompetitor(competitor).getBoatClass()));
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
                    for (final WindSource windSource : loadedWindTracks.keySet()) {
                        updateWindSourcesByType(windSource);
                    }
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
            markPassingCalculator = createMarkPassingCalculator(markPassingRaceFingerprintRegistry);
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
            }, /* Not relevant For replication */ Optional.empty(), /* synchronous */ false);
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
        rankingMetric = rankingMetricConstructor.apply(this);
    }

    protected void invalidateDistancesFromStarboardSideOfStartLineProjectedOntoLineCache(TimeRange timeRangeToInvalidate) {
        if (!distancesFromStarboardSideOfStartLineProjectedOntoLineCache.isEmpty()) {
            for (final Iterator<Entry<TimePoint, SortedMap<Competitor, Distance>>> i=distancesFromStarboardSideOfStartLineProjectedOntoLineCache.entrySet().iterator(); i.hasNext(); ) {
                final Entry<TimePoint, SortedMap<Competitor, Distance>> next = i.next();
                if (timeRangeToInvalidate.includes(next.getKey())) {
                    i.remove();
                    distancesFromStarboardSideOfStartLineProjectedOntoLineCacheLastAccessTimes.remove(next.getKey());
                }
            }
        }
    }

    @Override
    public boolean recordWind(Wind wind, WindSource windSource, boolean applyFilter) {
        final boolean result;
        if (!applyFilter || takesWindFixWithTimePoint(wind.getTimePoint())) {
            WindTrack windTrack = getOrCreateWindTrack(windSource);
            result = windTrack.add(wind);
            if (result) {
                updated(wind.getTimePoint());
                triggerManeuverCacheRecalculationForAllCompetitors();
            }
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public void removeWind(Wind wind, WindSource windSource) {
        getOrCreateWindTrack(windSource).remove(wind);
        updated(/* time point */null); // wind events shouldn't advance race time
        triggerManeuverCacheRecalculationForAllCompetitors();
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

    private LinkedHashMap<TimePoint, LinkedHashMap<Competitor, RankAndRankComparable>> createCompetitorRankingsCache() {
        return new LinkedHashMap<TimePoint, LinkedHashMap<Competitor, RankAndRankComparable>>() {
            private static final long serialVersionUID = -6044369612727021861L;
            @Override
            protected boolean removeEldestEntry(Entry<TimePoint, LinkedHashMap<Competitor, RankAndRankComparable>> eldest) {
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
        for (final Track<? extends Timed> track : tracks) {
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
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        distancesFromStarboardSideOfStartLineProjectedOntoLineCache = new ConcurrentHashMap<>();
        distancesFromStarboardSideOfStartLineProjectedOntoLineCacheLastAccessTimes = new ConcurrentHashMap<>();
        getRace().getCourse().addCourseListener(this);
        for (DynamicSensorFixTrack<Competitor, ?> sensorTrack : sensorTracks.values()) {
            sensorTrack.addedToTrackedRace(this);
        }
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
        maneuverDetectorPerCompetitorCache = createManeuverDetectorCache();
        maneuverCache = createManeuverCache();
        logger.info("Deserialized race " + getRace().getName());
    }
    
    @Override
    public void initializeAfterDeserialization() {
        crossTrackErrorCache = new CrossTrackErrorCache(this); // this invokes this.addListener(crossTrackErrorCache)
        // which is handled by the subclass which may not yet be
        // fully initialized; see also bug 6039
        // considering the unlikely possibility that the course and this tracked race's internal structures
        // may be inconsistent, e.g., due to non-atomic serialization of course and tracked race; see bug 2223
        try {
            adjustStructureToCourse();
        } catch (PatchFailedException e) {
            throw new RuntimeException(e);
        } // a bit unclean: this also tries to work on the DynamicTrackedRaceImpl which isn't fully initialized yet; see also bug6039
        triggerManeuverCacheRecalculationForAllCompetitors();  // a bit unclean: this also tries to work on the DynamicTrackedRaceImpl which isn't fully initialized yet; see also bug6039
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

    public void waitForManeuverDetectionToFinish() {
        for (Competitor competitor : getRace().getCompetitors()) {
            getManeuvers(competitor, true);
        }
    }

    private ShortTimeAfterLastHitCache<Competitor, IncrementalManeuverDetector> createManeuverDetectorCache() {
        return new ShortTimeAfterLastHitCache<Competitor, IncrementalManeuverDetector>(
                /* preserve how many milliseconds */ 600000,
                competitor -> new IncrementalManeuverDetectorImpl(TrackedRaceImpl.this, competitor, windEstimation));
    }

    private SmartFutureCache<Competitor, List<Maneuver>, EmptyUpdateInterval> createManeuverCache() {
        return new SmartFutureCache<Competitor, List<Maneuver>, EmptyUpdateInterval>(
                new AbstractCacheUpdater<Competitor, List<Maneuver>, EmptyUpdateInterval>() {
                    @Override
                    public List<Maneuver> computeCacheUpdate(Competitor competitor, EmptyUpdateInterval updateInterval)
                            throws NoWindException {
                        return getTrackedRegatta().callWithCPUMeterWithException(()->{
                            Duration averageIntervalBetweenRawFixes = getTrack(competitor).getAverageIntervalBetweenRawFixes();
                            if (averageIntervalBetweenRawFixes != null) {
                                ManeuverDetector maneuverDetector;
                                // FIXME The LowGPSSamplingRateManeuverDetectorImpl doesn't work very well; it recognizes many tacks only as bear-away and doesn't seem to have any noticeable benefits... See ORC Worlds 2019 ORC A Long Offshore
    //                            if (averageIntervalBetweenRawFixes.asSeconds() >= 30) {
    //                                maneuverDetector = new LowGPSSamplingRateManeuverDetectorImpl(TrackedRaceImpl.this, competitor);
    //                            } else {
                                    maneuverDetector = maneuverDetectorPerCompetitorCache.getValue(competitor);
    //                            }
                                List<Maneuver> maneuvers = computeManeuvers(competitor, maneuverDetector);
                                return maneuvers;
                            } else {
                                return Collections.emptyList();
                            }
                        }, CPUMeteringType.MANEUVER_DETECTION.name());
                    }
                }, /* nameForLocks */ "Maneuver cache for race " + getRace().getName());
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
    private final Serializable updateStartAndEndOfTrackingMonitor = ""+new Random().nextDouble();

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
                if (!startOfTrackingFound && getStartOfRace() != null && getTrackedRegatta().getRegatta().isControlTrackingFromStartAndFinishTimes()) {
                    startOfTracking = getStartOfRace().minus(START_TRACKING_THIS_MUCH_BEFORE_RACE_START);
                    startOfTrackingFound = true;
                }
                if (!endOfTrackingFound && getFinishedTime() != null && getTrackedRegatta().getRegatta().isControlTrackingFromStartAndFinishTimes()) {
                    endOfTracking = getFinishedTime().plus(STOP_TRACKING_THIS_MUCH_AFTER_RACE_FINISH);
                    endOfTrackingFound = true;
                }
            }
            if (!startOfTrackingFound) {
                startOfTracking = null;
            }
            if (!endOfTrackingFound) {
                endOfTracking = null;
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
    private final String updateStartOfRaceCacheFieldsMonitor = ""+new Random().nextDouble();

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
    public TimePoint getFinishingTime() {
        return finishingTime;
    }

    protected void setFinishingTime(final TimePoint newFinishingTime) {
        finishingTime = newFinishingTime;
        updated(newFinishingTime);
    }

    @Override
    public TimePoint getFinishedTime() {
        return finishedTime;
    }

    protected void setFinishedTime(final TimePoint newFinishedTime) {
        finishedTime = newFinishedTime;
        updated(newFinishedTime);
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
        Date timePoint = null;
        if (at != null) {
            timePoint = at.asDate();
        } else {
            if (startOfTracking != null) {
                timePoint = startOfTracking.asDate();
            } else if (startTime != null) {
                timePoint = startTime.minus(TimingConstants.PRE_START_PHASE_DURATION_IN_MILLIS).plus(1).asDate();
            }
        }

        if (hasGPSData() && hasWindData()) {
            Util.Pair<Date, Date> minMax = RaceTimesCalculationUtil.calculateRaceMinMax(timePoint,
                    startOfTracking != null ? startOfTracking.asDate() : null,
                    startTime != null ? startTime.asDate() : null,
                    finishingTime != null ? finishingTime.asDate() : null,
                    finishedTime != null ? finishedTime.asDate() : null,
                    endTime != null ? endTime.asDate() : null,
                    endOfTracking != null ? endOfTracking.asDate() : null,
                    TimingConstants.PRE_START_PHASE_DURATION_IN_MILLIS,
                    RaceTimesCalculationUtil.MAX_TIME_AFTER_RACE_END,
                    TimingConstants.IS_LIVE_GRACE_PERIOD_IN_MILLIS);

            // We are live if at is in between min and max
            if (minMax.getA() != null && minMax.getB() != null) {
                return !minMax.getA().after(at.asDate()) && !at.asDate().after(minMax.getB());
            }
        }
        return false;
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
        return getValueFromStartToTimePointOrEnd(competitor, timePoint,
                (from, to)->{
                    final Distance result;
                    final Distance preResult = getTrack(competitor).getDistanceTraveled(from, to);
                    if (considerGateStart && preResult != null) {
                        result = preResult.add(getAdditionalGateStartDistance(competitor, timePoint));
                    } else {
                        result = preResult;
                    }
                    return result;
                });
    }

    @Override
    public Distance getDistanceFoiled(Competitor competitor, TimePoint timePoint) {
        return getBravoValue(competitor, timePoint, BravoFixTrack::getDistanceSpentFoiling);
    }

    @Override
    public Duration getDurationFoiled(Competitor competitor, TimePoint timePoint) {
        return getBravoValue(competitor, timePoint, BravoFixTrack::getTimeSpentFoiling);
    }

    @FunctionalInterface
    private static interface BravoFromToValueCalculator<T> {
        T getValue(BravoFixTrack<Competitor> bravoFixTrack, TimePoint from, TimePoint to);
    }

    private <T> T getBravoValue(Competitor competitor, TimePoint timePoint, BravoFromToValueCalculator<T> bravoValueCalculator) {
        return getValueFromStartToTimePointOrEnd(competitor, timePoint,
                (from, to)->{
                    final T result;
                    final BravoFixTrack<Competitor> bravoFixTrack = getSensorTrack(competitor, BravoFixTrack.TRACK_NAME);
                    if (bravoFixTrack != null) {
                        result = bravoValueCalculator.getValue(bravoFixTrack, from, to);
                    } else {
                        result = null;
                    }
                    return result;
                });
    }

    private <T> T getValueFromStartToTimePointOrEnd(Competitor competitor, TimePoint timePoint, Track.TimeRangeValueCalculator<T> valueCalculator) {
        final T result;
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
                    result = valueCalculator.calculate(markPassings.first().getTimePoint(), end);
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
        final TrackedLeg result;
        getRace().getCourse().lockForRead();
        try {
            int indexOfWaypoint = getRace().getCourse().getIndexOfWaypoint(endOfLeg);
            if (indexOfWaypoint == -1) {
                throw new IllegalArgumentException("Waypoint " + endOfLeg + " not found in " + getRace().getCourse());
            } else if (indexOfWaypoint == 0) {
                result = null;
            } else {
                result = trackedLegs.get(race.getCourse().getLegs().get(indexOfWaypoint - 1));
            }
            return result;
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
        TrackedLegOfCompetitor result = null;
        NavigableSet<MarkPassing> roundings = getMarkPassings(competitor);
        if (roundings != null) {
            NavigableSet<MarkPassing> localRoundings;
            lockForRead(roundings);
            try {
                localRoundings = new ArrayListNavigableSet<>(roundings.size(), new TimedComparator());
                localRoundings.addAll(roundings);
            } finally {
                unlockAfterRead(roundings);
            }
            TrackedLeg trackedLeg;
            // obtain last waypoint before obtaining mark passings monitor because obtaining the last waypoint
            // obtains the read lock for the course
            final Waypoint lastWaypoint = getRace().getCourse().getLastWaypoint();
            MarkPassing lastBeforeOrAt = localRoundings.floor(new DummyMarkPassingWithTimePointOnly(at));
            // already finished the race?
            if (lastBeforeOrAt != null) {
                // and not at or after last mark passing
                if (lastWaypoint != lastBeforeOrAt.getWaypoint()) {
                    trackedLeg = getTrackedLegStartingAt(lastBeforeOrAt.getWaypoint());
                } else {
                    // exactly *at* last mark passing?
                    if (!localRoundings.isEmpty() && at.equals(localRoundings.last().getTimePoint())) {
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
    public Competitor getOverallLeader(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return Util.first(getCompetitorsFromBestToWorst(timePoint, cache));
    }

    private boolean hasZeroRankBecauseNoMarkPassingsAtOrBeforeTimePoint(Competitor competitor, TimePoint timePoint) {
        final boolean hasZeroRankBecauseNoMarkPassingsAtOrBeforeTimePoint;
        final NavigableSet<MarkPassing> markPassings = getMarkPassings(competitor);
        if (markPassings.isEmpty()) {
            hasZeroRankBecauseNoMarkPassingsAtOrBeforeTimePoint = true;
        } else {
            final boolean hasNoMarkPassingAtOrBeforeTimePoint;
            lockForRead(markPassings);
            try {
                hasNoMarkPassingAtOrBeforeTimePoint = markPassings.floor(new DummyMarkPassingWithTimePointOnly(timePoint)) == null;
            } finally {
                unlockAfterRead(markPassings);
            }
            hasZeroRankBecauseNoMarkPassingsAtOrBeforeTimePoint = hasNoMarkPassingAtOrBeforeTimePoint;
        }
        return hasZeroRankBecauseNoMarkPassingsAtOrBeforeTimePoint;
    }
    
    @Override
    public int getRank(Competitor competitor, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final int result;
        if (hasZeroRankBecauseNoMarkPassingsAtOrBeforeTimePoint(competitor, timePoint)) {
            result = 0;
        } else {
            final RankAndRankComparable rankAndRankComparable = getCompetitorsFromBestToWorstAndRankAndRankComparable(timePoint, cache).get(competitor);
            if (competitor == null) {
                result = 0;
            } else {
                result = rankAndRankComparable.getRank();
            }
        }
        return result;
    }

    @Override
    public Boat getBoatOfCompetitor(Competitor competitor) {
        return getRace().getBoatOfCompetitor(competitor);
    }

    @Override
    public Competitor getCompetitorOfBoat(Boat boat) {
        if (boat == null) {
            return null;
        }
        for (Map.Entry<Competitor, Boat> competitorWithBoat : getRace().getCompetitorsAndTheirBoats().entrySet()) {
            if (boat.equals(competitorWithBoat.getValue())) {
                return competitorWithBoat.getKey();
            }
        }
        return null;
    }

    @Override
    public Iterable<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint) {
        return getCompetitorsFromBestToWorst(timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    @Override
    public Iterable<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return getCompetitorsFromBestToWorstAndRankAndRankComparable(timePoint, new LeaderboardDTOCalculationReuseCache(timePoint)).keySet();
    }

    @Override
    public LinkedHashMap<Competitor, RankAndRankComparable> getCompetitorsFromBestToWorstAndRankAndRankComparable(TimePoint timePoint) {
        return getCompetitorsFromBestToWorstAndRankAndRankComparable(timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    @Override
    public List<CompetitorAndRankComparable> getCompetitorsFromBestToWorstAndRankComparable(TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return getCompetitorsFromBestToWorstAndRankAndRankComparable(timePoint, cache).entrySet()
                .stream().map(e -> new CompetitorAndRankComparable(e.getKey(), e.getValue().getRankComparable())).collect(Collectors.toList());
                
    }

    @Override
    public List<CompetitorAndRankComparable> getCompetitorsFromBestToWorstAndRankComparable(TimePoint timePoint) {
        return getCompetitorsFromBestToWorstAndRankAndRankComparable(timePoint).entrySet()
                .stream().map(e -> new CompetitorAndRankComparable(e.getKey(), e.getValue().getRankComparable())).collect(Collectors.toList());
    }
    
    @Override
    public LinkedHashMap<Competitor, RankAndRankComparable> getCompetitorsFromBestToWorstAndRankAndRankComparable(TimePoint unadjustedTimePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final TimePoint timePoint;
        // normalize the time point to get cache hits when asking for time points that are later than
        // the last time point affected by any event received for this tracked race
        if (Util.compareToWithNull(unadjustedTimePoint, getTimePointOfNewestEvent(), /* nullIsLess */ false) <= 0) {
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
        LinkedHashMap<Competitor, RankAndRankComparable> rankedCompetitors;
        synchronized (competitorRankings) {
            rankedCompetitors = competitorRankings.get(timePoint);
        }
        if (rankedCompetitors == null) {
            LockUtil.lockForWrite(readWriteLock);
            try {
                rankedCompetitors = competitorRankings.get(timePoint); // try again; maybe a writer released the
                                                                       // write lock after updating the cache
                if (rankedCompetitors == null) {
                    // RaceRankComparator requires course read lock
                    getRace().getCourse().lockForRead();
                    try {
                        // TODO bug5147: here the ranking metrics need to return the RankComparables so that they could be ranked accordingly.
                        // encapsulate sorting providing etc. in a method of the Ranking metric. To Update this cache
                        final Comparator<Competitor> comparator = getRankingMetric().getRaceRankingComparator(timePoint, cache);
                        final List<Competitor> tempList = new ArrayList<Competitor>();
                        for (Competitor c : getRace().getCompetitors()) {
                            tempList.add(c);
                        }
                        Collections.sort(tempList, comparator);
                        final Iterator<Competitor> it = tempList.iterator();
                        rankedCompetitors = new LinkedHashMap<>();
                        for (int i = 1; it.hasNext(); i++) {
                            final Competitor competitor = it.next();
                            final int rank = hasZeroRankBecauseNoMarkPassingsAtOrBeforeTimePoint(competitor, timePoint) ? 0 : i;
                            rankedCompetitors.put(competitor, new RankAndRankComparable(rank, /* TODO bug5147 */ new RankComparableRank(rank)));
                        }
                    } finally {
                        getRace().getCourse().unlockAfterRead();
                    }
                    synchronized (competitorRankings) {
                        competitorRankings.put(timePoint, rankedCompetitors);
                    }
                }
            } finally {
                LockUtil.unlockAfterWrite(readWriteLock);
            }
        }
        return rankedCompetitors;
    }

    @Override
    public Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalysis) {
        return getAverageAbsoluteCrossTrackError(competitor, timePoint, waitForLatestAnalysis, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    @Override
    public Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalysis,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
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
    public Distance getAverageSignedCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalysis) {
        return getAverageSignedCrossTrackError(competitor, timePoint, waitForLatestAnalysis, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    @Override
    public Distance getAverageSignedCrossTrackError(Competitor competitor, TimePoint timePoint,
            boolean waitForLatestAnalyses, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
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
    public Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint from, TimePoint to, boolean upwindOnly, boolean waitForLatestAnalysis) {
        Distance result;
        result = crossTrackErrorCache.getAverageAbsoluteCrossTrackError(competitor, from, to, upwindOnly, waitForLatestAnalysis);
        return result;
    }

    @Override
    public Distance getAverageSignedCrossTrackError(Competitor competitor, TimePoint from, TimePoint to, boolean upwindOnly, boolean waitForLatestAnalysis) {
        Distance result;
        result = crossTrackErrorCache.getAverageSignedCrossTrackError(competitor, from, to, upwindOnly, waitForLatestAnalysis);
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
        if (!old.equals(new HashSet<>(getWindSourcesToExclude()))) {
            clearAllCachesExceptManeuvers();
            triggerManeuverCacheRecalculationForAllCompetitors();
        }
    }

    @Override
    public WindWithConfidence<Pair<Position, TimePoint>> getWindWithConfidence(Position position, TimePoint at,
            Set<WindSource> windSourcesToExclude) {
        final WindWithConfidence<Pair<Position, TimePoint>> windWithConfidence = shortTimeWindCache
                .getWindWithConfidence(position, roundToDuration(at, Duration.ONE_SECOND), windSourcesToExclude);
        return windWithConfidence;
    }

    private TimePoint roundToDuration(TimePoint t, Duration roundTo) {
        final long roundToMillis = roundTo.asMillis();
        final long half = roundToMillis/2;
        return new MillisecondsTimePoint((t.asMillis()+half) / roundToMillis * roundToMillis);
    }

    public WindWithConfidence<Pair<Position, TimePoint>> getWindWithConfidenceUncached(Position p, TimePoint at,
            Iterable<WindSource> windSourcesToExclude) {
        boolean canUseSpeedOfAtLeastOneWindSource = false;
        Weigher<Pair<Position, TimePoint>> weigher = new PositionAndTimePointWeigher(
        /* halfConfidenceAfterMilliseconds */WindTrack.WIND_HALF_CONFIDENCE_DURATION, WindTrack.WIND_HALF_CONFIDENCE_DISTANCE);
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

    /**
     * A function that starts with 0.1 as confidence if the number of boats in the smallest cluster is 1; growing
     * steadily and converging towards 1.0 as the number grows towards positive infinity (or {@link Integer#MAX_VALUE}
     * to be more precise). The derivative at {@code numberOfBoatsInSmallestCluster==1} is 0.1 (so as if it was doubling
     * on its way from one to two boats). Modeling this as the function {@code f(n) := 1 + b/(n+c)} we get a solution
     * for b and c such that {@code c=8} and {@code b = -0.9-0.9*c = -0.9-7.2 = -8.1} and hence
     *
     * <pre>
     *     f(n) := 1 - 8.1/(n+8)
     * </pre>
     *
     * @param numberOfBoatsInSmallestCluster
     *            must not be less than 1
     */
    private double getConfidenceMultiplierForClusterSize(int numberOfBoatsInSmallestCluster) {
        return 1.0 - 8.1/(8.0 + numberOfBoatsInSmallestCluster);
    }

    @Override
    public WindWithConfidence<TimePoint> getEstimatedWindDirectionWithConfidence(TimePoint timePoint) {
        final DummyMarkPassingWithTimePointOnly dummyMarkPassingForNow = new DummyMarkPassingWithTimePointOnly(timePoint);
        final Weigher<TimePoint> weigher = ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(
        // use a minimum confidence to avoid the bearing to flip to 270deg in case all is zero
                getMillisecondsOverWhichToAverageSpeed(), /* minimum confidence */0.0000000001);
        final Map<LegType, Pair<BearingWithConfidenceCluster<TimePoint>, ScalablePosition>> bearings = clusterBearingsByLegType(
                timePoint, dummyMarkPassingForNow, weigher);
        // use the minimum confidence of the four "quadrants" as the result's confidence
        BearingWithConfidenceImpl<TimePoint> reversedUpwindAverage = null;
        double confidence = 0;
        BearingWithConfidence<TimePoint> resultBearing = null;
        ScalablePosition scaledPosition = null;
        int numberOfFixesConsideredForScaledPosition = 0;
        final Set<WindSource> estimationExcluded = new HashSet<>();
        estimationExcluded.addAll(getWindSources(WindSourceType.TRACK_BASED_ESTIMATION));
        estimationExcluded.addAll(getWindSources(WindSourceType.COURSE_BASED));
        if (bearings != null) {
            // TODO factor out the commonalities between UPWIND and DOWNWIND to reduce code duplication
            int upwindNumberOfRelevantBoats = 0;
            int numberOfFixesUpwind = bearings.get(LegType.UPWIND).getA().size();
            if (numberOfFixesUpwind > 0) {
                final ScalablePosition upwindPosition = bearings.get(LegType.UPWIND).getB();
                final Pair<Double, Double> minimumAngleBetweenDifferentTacksUpwindWithConfidence = getMinimumAngleBetweenDifferentTacksUpwind(getWind(
                        upwindPosition.divide(numberOfFixesUpwind), timePoint, estimationExcluded));
                final BearingWithConfidenceCluster<TimePoint>[] bearingClustersUpwind = bearings
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
                            * getRace().getBoatClass().getUpwindWindEstimationConfidence()
                            * getConfidenceMultiplierForClusterSize(upwindNumberOfRelevantBoats)
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
                            * getRace().getBoatClass().getDownwindWindEstimationConfidence()
                            * getConfidenceMultiplierForClusterSize(downwindNumberOfRelevantBoats)
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
                    TrackedLeg trackedLeg = leg.getTrackedLeg();
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

    @Override
    public Tack getTack(Position where, TimePoint timePoint, Bearing boatBearing) throws NoWindException {
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
        return maneuverApproximators.get(competitor).approximate(from, to);
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

    public void triggerManeuverCacheRecalculation(final Competitor competitor) {
        if (cachesSuspended) {
            triggerManeuverCacheInvalidationForAllCompetitors = true;
        } else {
            maneuverCache.triggerUpdate(competitor, /* updateInterval */null);
        }
    }

    private List<Maneuver> computeManeuvers(Competitor competitor, ManeuverDetector maneuverDetector)
            throws NoWindException {
        logger.finest("computeManeuvers(" + competitor.getName() + ") called in tracked race " + this);
        long startedAt = System.currentTimeMillis();
        // compute the maneuvers for competitor
        List<Maneuver> result = maneuverDetector.detectManeuvers();
        logger.finest("computeManeuvers(" + competitor.getName() + ") called in tracked race " + this + " took "
                + (System.currentTimeMillis() - startedAt) + "ms");
        return result;
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
        List<Maneuver> allManeuvers = maneuverCache.get(competitor, waitForLatest);
        List<Maneuver> result;
        if (allManeuvers == null) {
            result = Collections.emptyList();
        } else {
            result = extractInterval(from, to, allManeuvers);
        }
        return result;
    }

    @Override
    public Iterable<Maneuver> getManeuvers(Competitor competitor, boolean waitForLatest) {
        List<Maneuver> allManeuvers = maneuverCache.get(competitor, waitForLatest);
        List<Maneuver> result;
        if (allManeuvers == null) {
            result = Collections.emptyList();
        } else {
            result = allManeuvers;
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
        public void gpsFixReceived(GPSFix fix, Mark mark, boolean firstFixInTrack, AddResult addedOrReplaced) {
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
            WindPositionMode windPositionMode, RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
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
        logger.info("Setting live delay for race "+getRace().getName()+" to "+delayToLiveInMillis+"ms");
        this.delayToLiveInMillis = delayToLiveInMillis;
    }

    @Override
    public TrackedRaceStatus getStatus() {
        return status;
    }

    /**
     * Changes to the {@link #status} variable are synchronized on the {@link #statusNotifier} field
     */
    protected Object getStatusNotifier() {
        return statusNotifier;
    }

    @Override
    public void runSynchronizedOnStatus(Runnable runnable) {
        synchronized (getStatusNotifier()) {
            runnable.run();
        }
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
            competitorTrack.suspendValidityAndMaxSpeedCaching();
        }
        for (GPSFixTrack<Mark, GPSFix> markTrack : markTracks.values()) {
            markTrack.suspendValidityAndMaxSpeedCaching();
        }
        if (markPassingCalculator != null) {
            markPassingCalculator.suspend();
        }
        crossTrackErrorCache.suspend();
        maneuverCache.suspend();
    }

    private void resumeAllCachesNotUpdatingWhileLoading() {
        cachesSuspended = false;
        shortTimeWindCache.clearCache();
        for (GPSFixTrack<Competitor, GPSFixMoving> competitorTrack : tracks.values()) {
            competitorTrack.resumeValidityAndMaxSpeedCaching();
        }
        for (GPSFixTrack<Mark, GPSFix> markTrack : markTracks.values()) {
            markTrack.resumeValidityAndMaxSpeedCaching();
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
    public boolean hasFinishedLoading() {
        synchronized (getStatusNotifier()) {
            final TrackedRaceStatusEnum status = getStatus().getStatus();
            return hasFinishedLoading(status);
        }
    }

    private boolean hasFinishedLoading(TrackedRaceStatusEnum status) {
        return (status != TrackedRaceStatusEnum.PREPARED && status != TrackedRaceStatusEnum.LOADING && status != TrackedRaceStatusEnum.ERROR);
    }

    @Override
    public void runWhenDoneLoading(final Runnable runnable) {
        synchronized (getStatusNotifier()) {
            if (!hasFinishedLoading()) {
                addListener(new AbstractRaceChangeListener() {
                    @Override
                    public void statusChanged(TrackedRaceStatus newStatus, TrackedRaceStatus oldStatus) {
                        logger.info("race "+TrackedRaceImpl.this+" went from "+oldStatus+" to "+newStatus);
                        if (hasFinishedLoading(newStatus.getStatus())) {
                            logger.info("race "+TrackedRaceImpl.this+" is considered having finished loading; running "+runnable);
                            removeListener(this);
                            runnable.run();
                        }
                    }
                });
            } else {
                runnable.run();
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
                result = ReadonlyRaceStateImpl.getOrCreate(raceLogResolver, raceLog);
                raceStates.put(raceLog, result);
            }
        }
        return result;
    }
    
    @Override
    public UUID getCourseAreaId() {
        for (final RaceLog raceLog : getAttachedRaceLogs()) {
            final ReadonlyRaceState raceStateForRaceLog = getRaceState(raceLog);
            final UUID courseAreaId = raceStateForRaceLog.getCourseAreaId();
            if (courseAreaId != null) {
                return courseAreaId;
            }
        }
        return null;
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
        return attachedRegattaLogs == null ? Collections.emptySet() : new HashSet<>(attachedRegattaLogs.values());
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
        return getSomethingMillisecondsBeforeRaceStart(competitor, millisecondsBeforeRaceStart, this::getDistanceToStartLine);
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

    private TimePoint getTimePointMillisecondsBeforeStart(long millisecondsBeforeRaceStart) {
        final TimePoint result;
        if (getStartOfRace() == null) {
            result = null;
        } else {
            result = new MillisecondsTimePoint(getStartOfRace().asMillis() - millisecondsBeforeRaceStart);
        }
        return result;
    }

    private <T> T getSomethingMillisecondsBeforeRaceStart(Competitor competitor, long millisecondsBeforeRaceStart, BiFunction<Competitor, TimePoint, T> dataSupplier) {
        final TimePoint timePoint = getTimePointMillisecondsBeforeStart(millisecondsBeforeRaceStart);
        final T result = timePoint == null ? null : dataSupplier.apply(competitor, timePoint);
        return result;
    }

    @Override
    public Distance getWindwardDistanceToFavoredSideOfStartLine(Competitor competitor, long millisecondsBeforeRaceStart) {
        return getSomethingMillisecondsBeforeRaceStart(competitor, millisecondsBeforeRaceStart, this::getWindwardDistanceToFavoredSideOfStartLine);
    }

    @Override
    public Distance getWindwardDistanceToFavoredSideOfStartLine(Competitor competitor, long millisecondsBeforeRaceStart, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return getSomethingMillisecondsBeforeRaceStart(competitor, millisecondsBeforeRaceStart, (c, t)->getWindwardDistanceToFavoredSideOfStartLine(c, t, cache));
    }

    @Override
    public Distance getWindwardDistanceToFavoredSideOfStartLine(Competitor competitor, TimePoint timePoint) {
        return getWindwardDistanceToFavoredSideOfStartLine(competitor, timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    @Override
    public Distance getWindwardDistanceToFavoredSideOfStartLine(Competitor competitor, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Waypoint startWaypoint = getRace().getCourse().getFirstWaypoint();
        final Distance result;
        if (startWaypoint == null) {
            result = null;
        } else {
            final Position competitorPosition = getTrack(competitor).getEstimatedPosition(timePoint, /* extrapolate */ false);
            final Position referenceStartPosition;
            if (Util.size(startWaypoint.getControlPoint().getMarks()) == 1) {
                referenceStartPosition = getApproximatePosition(startWaypoint, timePoint);
            } else {
                final LineDetails startLine = getStartLine(timePoint);
                if (startLine == null) {
                    referenceStartPosition = null;
                } else {
                    referenceStartPosition = startLine.getAdvantageousMarkPosition();
                }
            }
            if (competitorPosition == null) {
                result = null;
            } else {
                if (referenceStartPosition == null) {
                    result = null;
                } else {
                    final TrackedLeg firstLeg = getTrackedLegStartingAt(startWaypoint);
                    if (firstLeg == null) {
                        result = null;
                    } else {
                        result = firstLeg.getWindwardDistance(competitorPosition, referenceStartPosition, timePoint, WindPositionMode.LEG_MIDDLE, cache);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Speed getSpeed(Competitor competitor, long millisecondsBeforeRaceStart) {
        final Speed result;
        if (getStartOfRace() == null) {
            result = null;
        } else {
            TimePoint beforeStart = new MillisecondsTimePoint(getStartOfRace().asMillis() - millisecondsBeforeRaceStart);
            result = getTrack(competitor).getEstimatedSpeed(beforeStart);
        }
        return result;
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

    @Override
    public Distance getDistanceFromStarboardSideOfStartLineProjectedOntoLine(Competitor competitor, TimePoint timePoint) {
        final Distance result;
        final Pair<Bearing, Position> startLineBearingAndStarboardMarkPosition = getStartLineBearingAndStarboardMarkPosition(timePoint);
        if (startLineBearingAndStarboardMarkPosition.getA() == null || startLineBearingAndStarboardMarkPosition.getB() == null) {
            result = null;
        } else {
            final Position competitorPosition = getTrack(competitor).getEstimatedPosition(timePoint, /* extrapolate */ true);
            if (competitorPosition == null) {
                result = null;
            } else {
                final Position competitorPositionProjectedOntoLine = competitorPosition.projectToLineThrough(startLineBearingAndStarboardMarkPosition.getB(), startLineBearingAndStarboardMarkPosition.getA());
                result = competitorPositionProjectedOntoLine.getDistance(startLineBearingAndStarboardMarkPosition.getB());
            }
        }
        return result;
    }

    @Override
    public Pair<Bearing, Position> getStartLineBearingAndStarboardMarkPosition(TimePoint timePoint) {
        final LineDetails startLine = getStartLine(timePoint);
        final Bearing lineBearing;
        final Position starboardMarkPosition;
        if (startLine == null) { // single mark?
            starboardMarkPosition = getStarboardMarkOfStartlinePosition(timePoint);
            if (starboardMarkPosition == null) {
                lineBearing = null;
            } else {
                final Iterable<TrackedLeg> trackedLegs = getTrackedLegs();
                if (trackedLegs == null || !trackedLegs.iterator().hasNext()) {
                    lineBearing = null;
                } else {
                    final Bearing bearingFirstLeg = trackedLegs.iterator().next().getLegBearing(timePoint);
                    if (bearingFirstLeg == null) {
                        lineBearing = null;
                    } else {
                        lineBearing = bearingFirstLeg.add(new DegreeBearingImpl(270));
                    }
                }
            }
        } else {
            lineBearing = startLine.getBearingFromStarboardToPortWhenApproachingLine();
            starboardMarkPosition = startLine.getStarboardMarkPosition();
        }
        final Pair<Bearing, Position> startLineBearingAndStarboardMarkPosition = new Pair<>(lineBearing, starboardMarkPosition);
        return startLineBearingAndStarboardMarkPosition;
    }

    @Override
    public SortedMap<Competitor, Distance> getDistancesFromStarboardSideOfStartLineProjectedOntoLine(TimePoint timePoint,
            BiFunction<Competitor, TimePoint, MaxPointsReason> maxPointsReasonSupplier) {
        final SortedMap<Competitor, Distance> result = distancesFromStarboardSideOfStartLineProjectedOntoLineCache.computeIfAbsent(timePoint, tp->{
            final Map<Competitor, Distance> distances = new HashMap<>();
            for (final Competitor competitor : getRace().getCompetitors()) {
                final MaxPointsReason penaltyCode = maxPointsReasonSupplier.apply(competitor, timePoint);
                if (penaltyCode != MaxPointsReason.DNC && penaltyCode != MaxPointsReason.DNS) {
                    distances.put(competitor, getDistanceFromStarboardSideOfStartLineProjectedOntoLine(competitor, tp));
                }
            }
            final TreeMap<Competitor, Distance> map = new TreeMap<>((c1, c2)->distances.get(c1).compareTo(distances.get(c2)));
            map.putAll(distances);
            return map;
        });
        distancesFromStarboardSideOfStartLineProjectedOntoLineCacheLastAccessTimes.put(timePoint, ApproximateTime.approximateNow());
        if (distancesFromStarboardSideOfStartLineProjectedOntoLineCache.size() > MAX_DISTANCES_FROM_STARBOARD_SIDE_OF_START_LINE_PROJECTED_ONTO_LINE_CACHE_SIZE) {
            final TimePoint keyLeastRecentlyAccessed = distancesFromStarboardSideOfStartLineProjectedOntoLineCacheLastAccessTimes.entrySet().stream()
                    .max((e1, e2)->e1.getValue().compareTo(e2.getValue())).get().getKey();
            distancesFromStarboardSideOfStartLineProjectedOntoLineCache.remove(keyLeastRecentlyAccessed);
            distancesFromStarboardSideOfStartLineProjectedOntoLineCacheLastAccessTimes.remove(keyLeastRecentlyAccessed);
        }
        return result;
    }

    @Override
    public Competitor getNextCompetitorToPortOnStartLine(Competitor relativeTo, TimePoint timePoint,
            BiFunction<Competitor, TimePoint, MaxPointsReason> maxPointsReasonSupplier) {
        final SortedMap<Competitor, Distance> competitorsSortedByDistanceFromStarboardSideOfStartLineProjectedOntoLine =
                getDistancesFromStarboardSideOfStartLineProjectedOntoLine(timePoint, maxPointsReasonSupplier);
        final Competitor competitorImmediatelyToPort;
        final Distance competitorDistance = competitorsSortedByDistanceFromStarboardSideOfStartLineProjectedOntoLine.get(relativeTo);
        if (competitorDistance == null) {
            competitorImmediatelyToPort = null;
        } else {
            final SortedMap<Competitor, Distance> competitorsFurtherToPortIncludingSelf = competitorsSortedByDistanceFromStarboardSideOfStartLineProjectedOntoLine.tailMap(relativeTo);
            final Iterator<Entry<Competitor, Distance>> iterator = competitorsFurtherToPortIncludingSelf.entrySet().iterator();
            iterator.next(); // skip the "own" competitor ("self" / getCompetitor())
            if (iterator.hasNext()) {
                competitorImmediatelyToPort = iterator.next().getKey();
            } else {
                competitorImmediatelyToPort = null;
            }
        }
        return competitorImmediatelyToPort;
    }

    @Override
    public Competitor getNextCompetitorToStarboardOnStartLine(Competitor relativeTo, TimePoint timePoint,
            BiFunction<Competitor, TimePoint, MaxPointsReason> maxPointsReasonSupplier) {
        final SortedMap<Competitor, Distance> competitorsSortedByDistanceFromStarboardSideOfStartLineProjectedOntoLine =
                getDistancesFromStarboardSideOfStartLineProjectedOntoLine(timePoint, maxPointsReasonSupplier);
        final Competitor competitorImmediatelyToStarboard;
        final Distance competitorDistance = competitorsSortedByDistanceFromStarboardSideOfStartLineProjectedOntoLine.get(relativeTo);
        if (competitorDistance == null) {
            competitorImmediatelyToStarboard = null;
        } else {
            final SortedMap<Competitor, Distance> competitorsFurtherToStarboard = competitorsSortedByDistanceFromStarboardSideOfStartLineProjectedOntoLine.headMap(relativeTo);
            if (competitorsFurtherToStarboard != null && !competitorsFurtherToStarboard.isEmpty()) {
                competitorImmediatelyToStarboard = competitorsFurtherToStarboard.lastKey();
            } else {
                competitorImmediatelyToStarboard = null;
            }
        }
        return competitorImmediatelyToStarboard;

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
                    portMarkWhileApproachingLine, starboardMarkWhileApproachingLine,
                    portMarkPositionWhileApproachingLine, starboardMarkPositionWhileApproachingLine);
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
        final TimePoint fromTimePoint = getStartOfRace()==null?getStartOfTracking():getStartOfRace();
        final TimePoint toTimePoint = getEndOfRace()==null?getTimePointOfNewestEvent():getEndOfRace();
        final SpeedWithConfidence<TimePoint> result;
        if (fromTimePoint != null && toTimePoint != null) {
            result = getAverageWindSpeedWithConfidence(fromTimePoint, toTimePoint,
                    (int) ((toTimePoint.asMillis() - fromTimePoint.asMillis()) / resolutionInMillis));
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public SpeedWithConfidence<TimePoint> getAverageWindSpeedWithConfidenceWithNumberOfSamples(int numberOfFixes) {
        final TimePoint fromTimePoint = getStartOfRace()==null?getStartOfTracking():getStartOfRace();
        final TimePoint toTimePoint = getEndOfRace()==null?getTimePointOfNewestEvent():getEndOfRace();
        final SpeedWithConfidence<TimePoint> result;
        if (fromTimePoint != null && toTimePoint != null) {
            result = getAverageWindSpeedWithConfidence(fromTimePoint, toTimePoint, numberOfFixes);
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public SpeedWithConfidence<TimePoint> getAverageWindSpeedWithConfidence(TimePoint fromTimePoint, TimePoint toTimePoint, int numberOfFixes) {
        SpeedWithConfidence<TimePoint> result = null;
        if (toTimePoint != null) {
            List<WindSource> windSourcesToDeliver = new ArrayList<WindSource>();
            WindSourceImpl windSource = new WindSourceImpl(WindSourceType.COMBINED);
            windSourcesToDeliver.add(windSource);
            double sumWindSpeed = 0.0;
            double sumWindSpeedConfidence = 0.0;
            int speedCounter = 0;
            WindTrack windTrack = getOrCreateWindTrack(windSource);
            TimePoint timePoint = fromTimePoint;
            final int resolutionInMillis = (int) ((toTimePoint.asMillis()-fromTimePoint.asMillis())/numberOfFixes);
            for (int i = 0; i < numberOfFixes && timePoint.compareTo(toTimePoint) < 0; i++) {
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

    protected abstract MarkPassingCalculator createMarkPassingCalculator(MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry);

    @Override
    public boolean isUsingMarkPassingCalculator() {
        return markPassingCalculator!=null;
    }

    @Override
    public Position getCenterOfCourse(TimePoint at) {
        int count = 0;
        ScalablePosition sum = null;
        final MarkPositionAtTimePointCache cache = new MarkPositionAtTimePointCacheImpl(this, at);
        final Set<Pair<ControlPoint, ControlPoint>> visited = new HashSet<>();
        final Course course = getRace().getCourse();
        course.lockForRead();
        try {
            for (final Leg leg : course.getLegs()) {
                final Pair<ControlPoint, ControlPoint> visitedKey = new Pair<>(leg.getFrom().getControlPoint(), leg.getTo().getControlPoint());
                if (!visited.contains(visitedKey)) {
                    visited.add(visitedKey);
                    visited.add(new Pair<>(visitedKey.getB(), visitedKey.getA())); // leg middle is symmetrical
                    final Distance legDistance = getTrackedLeg(leg).getGreatCircleDistance(at, cache);
                    final Position legMiddle = getTrackedLeg(leg).getMiddleOfLeg(at, cache);
                    if (legMiddle != null) {
                        // scale the leg's middle position with the leg's length; time spent varies with length
                        ScalablePosition p = new ScalablePosition(legMiddle).multiply(legDistance.getMeters());
                        if (sum == null) {
                            sum = p;
                        } else {
                            sum = sum.add(p);
                        }
                        count++;
                    }
                }
            }
        } finally {
            course.unlockAfterRead();
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
            for (final RaceLog raceLog : attachedRaceLogs.values()) {
                raceLog.lockForRead();
                try {
                    for (RaceLogEvent raceLogEvent: raceLog.getRawFixes()) {
                        if (raceLogEvent.getClass().equals(RaceLogGateLineOpeningTimeEventImpl.class)){
                            RaceLogGateLineOpeningTimeEvent raceLogGateLineOpeningTimeEvent = (RaceLogGateLineOpeningTimeEvent) raceLogEvent;
                            result = raceLogGateLineOpeningTimeEvent.getGateLineOpeningTimes().getGolfDownTime();
                        }
                    }
                } finally {
                    raceLog.unlockAfterRead();
                }
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
            final Position estimatedCompetitorPositionAtStart = getTrack(competitor).getEstimatedPosition(competitorLegStartTime, /* extrapolate */false);
            if (estimatedCompetitorPositionAtStart != null && portSideOfStartLinePosition != null) {
                result = portSideOfStartLinePosition.getDistance(estimatedCompetitorPositionAtStart);
            } else {
                result = Distance.NULL;
            }
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
    public Duration getTimeSailedSinceRaceStart(Competitor competitor, TimePoint timePoint) {
        return getRankingMetric().getActualTimeSinceStartOfRace(competitor, timePoint);
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
        if (polarDataService != null && windEstimation != null) {
            updateManeuversAndWindWithNewWindEstimation(windEstimation, windEstimation);
        }
    }

    @Override
    public void setWindEstimation(IncrementalWindEstimation windEstimation) {
        IncrementalWindEstimation previousWindEstimation = this.windEstimation;
        if (previousWindEstimation != windEstimation) {
            updateManeuversAndWindWithNewWindEstimation(windEstimation, previousWindEstimation);
        }
    }

    private void updateManeuversAndWindWithNewWindEstimation(IncrementalWindEstimation windEstimation,
            IncrementalWindEstimation previousWindEstimation) {
        WindSource windSource = new WindSourceImpl(WindSourceType.MANEUVER_BASED_ESTIMATION);
        windTracks.remove(windSource);
        if (windEstimation != null) {
            windTracks.put(windSource, windEstimation.getWindTrack());
        }
        updateWindSourcesByType(windSource);
        this.windEstimation = windEstimation;
        // TODO Make more efficient by reusing the state of incremental maneuver detectors. The already computed
        // complete maneuver curves can be fed directly into the windEstimation.
        maneuverDetectorPerCompetitorCache.clearCache();
        shortTimeWindCache.clearCache();
        triggerManeuverCacheRecalculationForAllCompetitors();
    }

    /**
     * Obtains the {@link #raceLogResolver}.
     */
    @Override
    public RaceLogAndTrackedRaceResolver getRaceLogResolver() {
        return raceLogResolver;
    }

    public void setRaceLogResolver(RaceLogAndTrackedRaceResolver raceLogResolver) {
        this.raceLogResolver = raceLogResolver;
    }

    /**
     * When given the opportunity to resolve after de-serialization, grabs the {@link RaceLogResolver} from the
     * {@link SharedDomainFactory} because the field is transient and needs filling after de-serialization.
     */
    @Override
    public IsManagedByCache<DomainFactory> resolve(DomainFactory domainFactory) {
        this.raceLogResolver = domainFactory.getRaceLogResolver();
        return this;
    }

    /**
     * Regatta listeners are transient only; so after de-serialization we have to re-establish the regatta listener
     * that is responsible for updating tracking times when the rules for how this works have changed on the regatta.
     */
    public void registerRegattaListener() {
        trackedRegatta.getRegatta().addRegattaListener(new TimingUpdaterCallback());
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
        Optional<Runnable> executeAfterReleasingLock = Optional.empty();
        TrackT result;
        LockUtil.lockForWrite(sensorTracksLock);
        try {
            result = getTrackInternal(key);
            if (result == null && tracks.containsKey(competitor)) {
                // A track is only added if the given Competitor is known to participate in this race
                result = newTrackFactory.get();
                executeAfterReleasingLock = addSensorTrackInternal(key, result);
            }
        } finally {
            LockUtil.unlockAfterWrite(sensorTracksLock);
        }
        executeAfterReleasingLock.ifPresent(r->r.run());
        return result;
    }

    protected void addSensorTrack(Competitor competitor, String trackName, DynamicSensorFixTrack<Competitor, ?> track) {
        Pair<Competitor, String> key = new Pair<>(competitor, trackName);
        Optional<Runnable> executeAfterReleasingLock = Optional.empty();
        LockUtil.lockForWrite(sensorTracksLock);
        try {
            if (getTrackInternal(key) != null) {
                if (logger != null && logger.getLevel() != null && logger.getLevel().equals(Level.WARNING)) {
                    logger.warning(SensorFixTrack.class.getName() + " already exists for competitor: "
                            + competitor.getName() + "; trackName: " + trackName);
                }
            } else {
                executeAfterReleasingLock = this.addSensorTrackInternal(key, track);
            }
        } finally {
            LockUtil.unlockAfterWrite(sensorTracksLock);
        }
        executeAfterReleasingLock.ifPresent(r->r.run());
    }

    /**
     * To call this method, the caller must have obtained the write lock of {@link #sensorTracksLock}.
     * Optionally, the method may return a {@link Runnable} to execute after the lock has been released.
     * This may, e.g., be a routine that notifies listeners. Callers are responsible for invoking this
     * {@link Runnable} <em>after</em> releasing the write lock.
     */
    protected <FixT extends SensorFix> Optional<Runnable> addSensorTrackInternal(Pair<Competitor, String> key,
            DynamicSensorFixTrack<Competitor, FixT> track) {
        assert sensorTracksLock.isWriteLockedByCurrentThread();
        sensorTracks.put(key, track);
        track.addedToTrackedRace(this);
        return Optional.empty();
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
        return attachedRaceLogs == null ? Collections.emptySet() : new HashSet<>(attachedRaceLogs.values());
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


    @Override
    public SpeedWithBearing getVelocityMadeGood(Competitor competitor, TimePoint timePoint,
            WindPositionMode windPositionMode, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        TrackedLegOfCompetitor trackedLeg = getTrackedLeg(competitor, timePoint);
        final SpeedWithBearing result;
        if (trackedLeg != null) {
            result = trackedLeg.getVelocityMadeGood(timePoint, windPositionMode, cache);
        } else {
            // check if wind information is available; if so, compute a VMG only based on wind data:
            if (windPositionMode == WindPositionMode.LEG_MIDDLE) {
                result = null;
            } else {
                final Wind wind = getWind(windPositionMode, /* trackedLeg */ null, competitor, timePoint, cache);
                result = projectOnto(getTrack(competitor).getEstimatedSpeed(timePoint), wind.getBearing());
            }
        }
        return result;
    }

    SpeedWithBearing projectOnto(SpeedWithBearing speed, Bearing projectToBearing) {
        final SpeedWithBearing result;
        if (speed != null && speed.getBearing() != null && projectToBearing != null) {
            double cos = Math.cos(speed.getBearing().getRadians() - projectToBearing.getRadians());
            if (cos < 0) {
                projectToBearing = projectToBearing.reverse();
            }
            result = new KnotSpeedWithBearingImpl(Math.abs(speed.getKnots() * cos), projectToBearing);
        } else {
            result = null;
        }
        return result;
    }

    /**
     * @param trackedLeg
     *            The caller is expected to obtain the {@link #getTrackedLeg(Competitor, TimePoint) tracked leg} the
     *            {@code competitor} is sailing in at time point {@code at}. If {@code null}, any
     *            non-{@link WindPositionMode#EXACT exact} wind position mode will use {@code null} for the wind
     *            position, defaulting to the "COMBINED" wind source at the middle of the course. In particular, it then
     *            obviously makes no real sense to request {@link WindPositionMode#LEG_MIDDLE} because no leg is known.
     */
    Wind getWind(WindPositionMode windPositionMode, TrackedLegImpl trackedLeg, Competitor competitor, TimePoint at, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Wind wind;
        if (windPositionMode == WindPositionMode.EXACT) {
            wind = cache.getWind(this, competitor, at);
        } else {
            wind = getWind(trackedLeg == null ? null :
                    trackedLeg.getEffectiveWindPosition(
                            () -> getTrack(competitor)
                                    .getEstimatedPosition(at, false), at, windPositionMode), at);
        }
        return wind;
    }

    @Override
    public PolarDataService getPolarDataService() {
        return polarDataService;
    }

    @Override
    public WindSummary getWindSummary() {
        Speed minTrueWindSpeed = null;
        Speed maxTrueWindSpeed = null;
        final TimePoint finishedTime = getFinishedTime();
        final TimePoint endOfRace = getEndOfRace();
        final TimePoint finishTime = finishedTime == null ?
                endOfRace == null ? MillisecondsTimePoint.now().minus(getDelayToLiveInMillis()) : endOfRace : finishedTime;
        final TimePoint newestEvent = getTimePointOfNewestEvent();
        final TimePoint toTimePoint;
        if (newestEvent != null && newestEvent.before(finishTime)) {
            toTimePoint = newestEvent;
        } else {
            toTimePoint = finishTime;
        }
        final BearingWithConfidenceCluster<TimePoint> bwcc = new BearingWithConfidenceCluster<TimePoint>(
                new Weigher<TimePoint>() {
                    private static final long serialVersionUID = -5779398785058438328L;
                    @Override
                    public double getConfidence(TimePoint fix, TimePoint request) {
                        return 1;
                    }
                });
        final TimePoint middleOfRace = getStartOfRace().plus(getStartOfRace().until(toTimePoint).divide(2));
        List<TimePoint> pointsToGetWind = Arrays.asList(getStartOfRace(), middleOfRace, toTimePoint);
        for (TimePoint timePoint : pointsToGetWind) {
            WindWithConfidence<com.sap.sse.common.Util.Pair<Position, TimePoint>> averagedWindWithConfidence =
                    getWindWithConfidence(getCenterOfCourse(timePoint), timePoint);
            final WindWithConfidence<com.sap.sse.common.Util.Pair<Position, TimePoint>> windFixToUse;
            if (averagedWindWithConfidence != null && averagedWindWithConfidence.getObject().getKnots() >= 0.05d) {
                windFixToUse = averagedWindWithConfidence;
            } else {
                windFixToUse = null;
            }
            if (windFixToUse != null) {
                final Wind wind = windFixToUse.getObject();
                bwcc.add(new BearingWithConfidenceImpl<TimePoint>(wind.getBearing(), windFixToUse.getConfidence(), timePoint));
                if (minTrueWindSpeed == null || minTrueWindSpeed.compareTo(wind) > 0) {
                    minTrueWindSpeed = wind;
                }
                if (maxTrueWindSpeed == null || maxTrueWindSpeed.compareTo(wind) < 0) {
                    maxTrueWindSpeed = wind;
                }
            }
        }
        final WindSummary result;
        if (minTrueWindSpeed != null && maxTrueWindSpeed != null) {
            BearingWithConfidence<TimePoint> average = bwcc.getAverage(middleOfRace);
            result = new WindSummaryImpl(average.getObject().reverse(), minTrueWindSpeed,
                    maxTrueWindSpeed);
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public TrackingConnectorInfo getTrackingConnectorInfo() {
        return trackingConnectorInfo;
    }
    
    @Override
    public Double getPercentTargetBoatSpeed(Competitor competitor, TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache)
            throws NotEnoughDataHasBeenAddedException, MaxIterationsExceededException, FunctionEvaluationException {
        Double result;
        if (getRankingMetric().getType() == RankingMetrics.ONE_DESIGN) {
            final PolarDataService polarDataService = getPolarDataService();
            if (polarDataService != null) {
                final GPSFixTrack<Competitor, GPSFixMoving> competitorTrack = getTrack(competitor);
                final Wind wind = getWind(competitorTrack.getEstimatedPosition(timePoint, /* extrapolate */ true), timePoint);
                final Bearing twa = getTWA(competitor, timePoint, cache);
                if (twa != null) {
                    try {
                        final SpeedWithConfidence<Void> targetSpeed = polarDataService.getSpeed(getBoatOfCompetitor(competitor).getBoatClass(), wind, twa);
                        final Speed sog = competitorTrack.getEstimatedSpeed(timePoint);
                        result = targetSpeed != null && targetSpeed.getObject() != null && sog != null ? 100.0 * sog.getKnots() / targetSpeed.getObject().getKnots() : null;
                    } catch (NotEnoughDataHasBeenAddedException e) {
                        result = null;
                    }
                } else {
                    result = null;
                }
            } else {
                result = null;
            }
        } else if (getRankingMetric() instanceof ORCPerformanceCurveRankingMetric) {
            final ORCPerformanceCurveRankingMetric orcRankingMetric = (ORCPerformanceCurveRankingMetric) getRankingMetric();
            final GPSFixTrack<Competitor, GPSFixMoving> competitorTrack = getTrack(competitor);
            final Wind wind = getWind(competitorTrack.getEstimatedPosition(timePoint, /* extrapolate */ true), timePoint);
            final Speed impliedWind = orcRankingMetric.getImpliedWind(competitor, timePoint, cache);
            result = impliedWind == null || wind == null ? null : impliedWind.divide(wind)*100.0;
        } else {
            result = null;
        }
        return result;
    }
}
