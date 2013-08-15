package com.sap.sailing.domain.tracking.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.BearingWithConfidence;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseListener;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.Timed;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BearingWithConfidenceImpl;
import com.sap.sailing.domain.base.impl.DouglasPeucker;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.CourseChange;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.NoWindError;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.TimingConstants;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.confidence.ConfidenceBasedWindAverager;
import com.sap.sailing.domain.confidence.ConfidenceFactory;
import com.sap.sailing.domain.confidence.HasConfidence;
import com.sap.sailing.domain.confidence.Weigher;
import com.sap.sailing.domain.confidence.impl.HyperbolicTimeDifferenceWeigher;
import com.sap.sailing.domain.confidence.impl.PositionAndTimePointWeigher;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.analyzing.impl.StartTimeFinder;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.GPSTrackListener;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.util.SmartFutureCache;
import com.sap.sailing.util.SmartFutureCache.AbstractCacheUpdater;
import com.sap.sailing.util.SmartFutureCache.EmptyUpdateInterval;
import com.sap.sailing.util.impl.ArrayListNavigableSet;
import com.sap.sailing.util.impl.LockUtil;
import com.sap.sailing.util.impl.NamedReentrantReadWriteLock;

public abstract class TrackedRaceImpl implements TrackedRace, CourseListener {
    private static final long serialVersionUID = -4825546964220003507L;

    private static final Logger logger = Logger.getLogger(TrackedRaceImpl.class.getName());

    private static final double PENALTY_CIRCLE_DEGREES_THRESHOLD = 270;

    // TODO make this variable
    private static final long DELAY_FOR_CACHE_CLEARING_IN_MILLISECONDS = 7500;

    private final RaceDefinition race;

    private final TrackedRegatta trackedRegatta;

    private TrackedRaceStatus status;

    private final Object statusNotifier;

    /**
     * By default, all wind sources are used, none are excluded. However, e.g., for performance reasons, particular wind
     * sources such as the track-based estimation wind source, may be excluded by adding them to this set.
     */
    private final Set<WindSource> windSourcesToExclude;

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
     * Race start time as announced by the tracking infrastructure
     */
    private TimePoint startTimeReceived;

    /**
     * The calculated race start time
     */
    private TimePoint startTime;

    /**
     * The calculated race end time
     */
    private TimePoint endTime;

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

    private transient Map<TimePoint, List<Competitor>> competitorRankings;

    /**
     * The locks managed here correspond with the {@link #competitorRankings} structure. When
     * {@link #getCompetitorsFromBestToWorst(TimePoint)} starts to compute rankings, it locks the write lock for the
     * time point. Readers use the read lock. Checking / entering a lock into this map uses <code>synchronized</code> on
     * the map itself.
     */
    private transient Map<TimePoint, NamedReentrantReadWriteLock> competitorRankingsLocks;

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
    private transient SmartFutureCache<Competitor, Triple<TimePoint, TimePoint, List<Maneuver>>, EmptyUpdateInterval> maneuverCache;

    /**
     * A tracked race can maintain a number of sources for wind information from which a client can select. As all
     * intra-leg computations are done dynamically based on wind information, selecting a different wind information
     * source can alter the intra-leg results. See {@link #currentWindSource}.
     */
    private final Map<WindSource, WindTrack> windTracks;

    private transient Map<TimePoint, Future<Wind>> directionFromStartToNextMarkCache;

    private final ConcurrentHashMap<Mark, GPSFixTrack<Mark, GPSFix>> markTracks;
    
    private final Map<String, Sideline> courseSidelines;

    protected long millisecondsOverWhichToAverageSpeed;

    private final Map<Mark, StartToNextMarkCacheInvalidationListener> startToNextMarkCacheInvalidationListeners;

    protected long millisecondsOverWhichToAverageWind;

    private transient WindStore windStore;

    private transient Timer cacheInvalidationTimer;
    private transient Object cacheInvalidationTimerLock;

    private transient CombinedWindTrackImpl combinedWindTrack;

    protected transient HashMap<Serializable, RaceLog> attachedRaceLogs;

    /**
     * The time delay to the current point in time in milliseconds.
     */
    private long delayToLiveInMillis;

    /**
     * The constructor loads wind fixes from the {@link #windStore} asynchronously. When completed, this flag is set to
     * <code>true</code>, and all threads currently waiting on this object are notified.
     */
    private boolean windLoadingCompleted;

    private transient CrossTrackErrorCache crossTrackErrorCache;

    /**
     * Serializing an instance of this class has to serialized the various data structures holding the tracked race's
     * state. When a race is currently on, these structures change very frequently, and
     * {@link ConcurrentModificationException}s during serialization will be the norm rather than the exception. To
     * avoid this, all modifications to any data structure that is not in itself synchronized obtains this lock's
     * <em>read</em> lock (note that this may be confusing at first, but we'd like to support many concurrent writers;
     * they each perform their own locking on the individual data structures they write; we only want to lock out a
     * single serialization call which with this lock is represented as the "writer"). The serialization method
     * {@link #writeObject(ObjectOutputStream)} obtains the <em>write</em> lock. Deadlocks are avoided because the
     * serialization, once it obtains this write lock, it keeps serializing and releases the write lock when it's done,
     * without doing any further synchronization or locking.
     */
    private final NamedReentrantReadWriteLock serializationLock;

    private final Map<Iterable<MarkPassing>, NamedReentrantReadWriteLock> locksForMarkPassings;

    public TrackedRaceImpl(final TrackedRegatta trackedRegatta, RaceDefinition race, final Iterable<Sideline> sidelines, final WindStore windStore,
            long delayToLiveInMillis, final long millisecondsOverWhichToAverageWind,
            long millisecondsOverWhichToAverageSpeed, long delayForWindEstimationCacheInvalidation) {
        super();
        locksForMarkPassings = new IdentityHashMap<>();
        attachedRaceLogs = new HashMap<>();
        this.status = new TrackedRaceStatusImpl(TrackedRaceStatusEnum.PREPARED, 0.0);
        this.statusNotifier = new Object[0];
        this.serializationLock = new NamedReentrantReadWriteLock("Serialization lock for tracked race "
                + race.getName(), /* fair */true);
        this.cacheInvalidationTimerLock = new Object();
        this.updateCount = 0;
        this.race = race;
        this.windStore = windStore;
        this.windSourcesToExclude = new HashSet<WindSource>();
        this.directionFromStartToNextMarkCache = new HashMap<TimePoint, Future<Wind>>();
        this.millisecondsOverWhichToAverageSpeed = millisecondsOverWhichToAverageSpeed;
        this.millisecondsOverWhichToAverageWind = millisecondsOverWhichToAverageWind;
        this.delayToLiveInMillis = delayToLiveInMillis;
        this.startToNextMarkCacheInvalidationListeners = new ConcurrentHashMap<Mark, TrackedRaceImpl.StartToNextMarkCacheInvalidationListener>();
        this.maneuverCache = createManeuverCache();
        this.markTracks = new ConcurrentHashMap<Mark, GPSFixTrack<Mark, GPSFix>>();
        this.crossTrackErrorCache = new CrossTrackErrorCache(this);
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
                    MarkPassingByTimeComparator.INSTANCE));
        }
        markPassingsTimes = new ArrayList<Pair<Waypoint, Pair<TimePoint, TimePoint>>>();
        windTracks = new ConcurrentHashMap<WindSource, WindTrack>();
        new Thread("Wind loader for tracked race " + getRace().getName()) {
            @Override
            public void run() {
                // When this tracked race is to be serialized, wait for the loading of the wind tracks to complete.
                // It seems sufficiently unlikely that serialization is requested after the constructor succeeds and
                // before this thread obtains the lock.
                LockUtil.lockForRead(serializationLock);
                try {
                    final Map<? extends WindSource, ? extends WindTrack> loadedWindTracks = windStore.loadWindTracks(
                            trackedRegatta, TrackedRaceImpl.this, millisecondsOverWhichToAverageWind);
                    windTracks.putAll(loadedWindTracks);
                } finally {
                    LockUtil.unlockAfterRead(serializationLock);
                    synchronized (TrackedRaceImpl.this) {
                        windLoadingCompleted = true;
                        TrackedRaceImpl.this.notifyAll();
                    }
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
        this.trackedRegatta = trackedRegatta;
        competitorRankings = new HashMap<TimePoint, List<Competitor>>();
        competitorRankingsLocks = new HashMap<TimePoint, NamedReentrantReadWriteLock>();
    }

    /**
     * Object serialization obtains a read lock for the course so that in cannot change while serializing this object.
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        LockUtil.lockForWrite(serializationLock);
        try {
            s.defaultWriteObject();
        } finally {
            LockUtil.unlockAfterWrite(serializationLock);
        }
    }

    /**
     * Deserialization has to be maintained in lock-step with {@link #writeObject(ObjectOutputStream) serialization}.
     * When de-serializing, a possibly remote {@link #windStore} is ignored because it is transient. Instead, an
     * {@link EmptyWindStore} is used for the de-serialized instance.
     */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        attachedRaceLogs = new HashMap<>();
        markPassingsTimes = new ArrayList<Pair<Waypoint, Pair<TimePoint, TimePoint>>>();
        cacheInvalidationTimerLock = new Object();
        windStore = EmptyWindStore.INSTANCE;
        competitorRankings = new HashMap<TimePoint, List<Competitor>>();
        competitorRankingsLocks = new HashMap<TimePoint, NamedReentrantReadWriteLock>();
        directionFromStartToNextMarkCache = new HashMap<TimePoint, Future<Wind>>();
        crossTrackErrorCache = new CrossTrackErrorCache(this);
        maneuverCache = createManeuverCache();
    }

    @Override
    public synchronized void waitUntilWindLoadingComplete() throws InterruptedException {
        while (!windLoadingCompleted) {
            wait();
        }
    }

    private SmartFutureCache<Competitor, Triple<TimePoint, TimePoint, List<Maneuver>>, EmptyUpdateInterval> createManeuverCache() {
        return new SmartFutureCache<Competitor, Triple<TimePoint, TimePoint, List<Maneuver>>, EmptyUpdateInterval>(
                new AbstractCacheUpdater<Competitor, Triple<TimePoint, TimePoint, List<Maneuver>>, EmptyUpdateInterval>() {
                    @Override
                    public Triple<TimePoint, TimePoint, List<Maneuver>> computeCacheUpdate(Competitor competitor,
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
        return markPassingsForCompetitor.get(competitor);
    }

    protected NavigableSet<MarkPassing> getMarkPassingsInOrderAsNavigableSet(Waypoint waypoint) {
        return markPassingsForWaypoint.get(waypoint);
    }

    @Override
    public WindStore getWindStore() {
        return windStore;
    }

    @Override
    public Iterable<MarkPassing> getMarkPassingsInOrder(Waypoint waypoint) {
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
                MarkPassingByTimeComparator.INSTANCE);
        LockUtil.lockForRead(serializationLock);
        try {
            markPassingsForWaypoint.put(waypoint, result);
        } finally {
            LockUtil.unlockAfterRead(serializationLock);
        }
        return result;
    }

    @Override
    public TimePoint getStartOfTracking() {
        return startOfTrackingReceived;
    }

    @Override
    public TimePoint getEndOfTracking() {
        return endOfTrackingReceived;
    }

    public void invalidateStartTime() {
        startTime = null;
    }

    public void invalidateEndTime() {
        endTime = null;
    }

    protected void invalidateMarkPassingTimes() {
        synchronized (markPassingsTimes) {
            markPassingsTimes.clear();
        }
    }

    /**
     * Calculates the start time of the race from various sources. The highest precedence take the {@link #attachedRaceLogs race logs},
     * followed by the field {@link #startTimeReceived} which can explicitly be set using {@link #setStartTimeReceived(TimePoint)}.
     * If that does not provide any start time either, a start time is attempted to be inferred from the time points
     * of the start mark passing events.
     */
    @Override
    public TimePoint getStartOfRace() {
        if (startTime == null) {
            for (RaceLog raceLog : attachedRaceLogs.values()) {
                startTime = new StartTimeFinder(raceLog).analyze();
                if (startTime != null) {
                    break;
                }
            }
            if (startTime == null) {
                startTime = startTimeReceived;
                // If not null, check if the first mark passing for the start line is too much after the
                // startTimeReceived; if so, return an adjusted, later start time.
                // If no official start time was received, try to estimate the start time using the mark passings for
                // the start line.
                final Waypoint firstWaypoint = getRace().getCourse().getFirstWaypoint();
                if (firstWaypoint != null) {
                    if (startTimeReceived != null) {
                        TimePoint timeOfFirstMarkPassing = getFirstPassingTime(firstWaypoint);
                        if (timeOfFirstMarkPassing != null) {
                            long startTimeReceived2timeOfFirstMarkPassingFirstMark = timeOfFirstMarkPassing.asMillis()
                                    - startTimeReceived.asMillis();
                            if (startTimeReceived2timeOfFirstMarkPassingFirstMark > MAX_TIME_BETWEEN_START_AND_FIRST_MARK_PASSING_IN_MILLISECONDS) {
                                startTime = new MillisecondsTimePoint(timeOfFirstMarkPassing.asMillis()
                                        - MAX_TIME_BETWEEN_START_AND_FIRST_MARK_PASSING_IN_MILLISECONDS);
                            } else {
                                startTime = startTimeReceived;
                            }
                        }
                    } else {
                        final NavigableSet<MarkPassing> markPassingsForFirstWaypointInOrder = getMarkPassingsInOrderAsNavigableSet(firstWaypoint);
                        if (markPassingsForFirstWaypointInOrder != null) {
                            startTime = calculateStartOfRaceFromMarkPassings(markPassingsForFirstWaypointInOrder,
                                    getRace().getCompetitors());
                        }
                    }
                }
            }
        }
        return startTime;
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

    private TimePoint getLastPassingOfFinishLine() {
        TimePoint passingTime = null;
        final Waypoint lastWaypoint = getRace().getCourse().getLastWaypoint();
        if (lastWaypoint != null) {
            Iterable<MarkPassing> markPassingsInOrder = getMarkPassingsInOrder(lastWaypoint);
            if (markPassingsInOrder != null) {
                lockForRead(markPassingsInOrder);
                try {
                    for (MarkPassing passingFinishLine : markPassingsInOrder) {
                        passingTime = passingFinishLine.getTimePoint();
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

    private TimePoint calculateStartOfRaceFromMarkPassings(NavigableSet<MarkPassing> markPassings,
            Iterable<Competitor> competitors) {
        TimePoint startOfRace = null;
        // Find the first mark passing within the largest cluster crossing the line within one minute.
        final long ONE_MINUTE_IN_MILLIS = 60 * 1000;
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
                        if (currentMarkPassing.getTimePoint().asMillis()
                                - candidateForStartOfLargestGroupSoFar.getTimePoint().asMillis() <= ONE_MINUTE_IN_MILLIS) {
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
                            // advance
                            // candidateForStartOfLargestGroupSoFar and reduce group size counter, until
                            // candidateForStartOfLargestGroupSoFar
                            // is again within the one-minute interval; may catch up all the way to currentMarkPassing
                            // if that was
                            // more than a minute after its predecessor
                            while (currentMarkPassing.getTimePoint().asMillis()
                                    - candidateForStartOfLargestGroupSoFar.getTimePoint().asMillis() > ONE_MINUTE_IN_MILLIS) {
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
        final Date startOfLivePeriod;
        final Date endOfLivePeriod;
        if (!hasGPSData() || !hasWindData()) {
            startOfLivePeriod = null;
            endOfLivePeriod = null;
        } else {
            if (getStartOfRace() == null) {
                startOfLivePeriod = getStartOfTracking().asDate();
            } else {
                startOfLivePeriod = new Date(getStartOfRace().asMillis() - TimingConstants.PRE_START_PHASE_DURATION_IN_MILLIS);
            }
            if (getEndOfRace() == null) {
                if (getTimePointOfNewestEvent() != null) {
                    endOfLivePeriod = new Date(getTimePointOfNewestEvent().asMillis()
                            + TimingConstants.IS_LIVE_GRACE_PERIOD_IN_MILLIS);
                } else {
                    endOfLivePeriod = null;
                }
            } else {
                endOfLivePeriod = new Date(getEndOfRace().asMillis() + TimingConstants.IS_LIVE_GRACE_PERIOD_IN_MILLIS);
            }
        }
    
        // if an empty timepoint is given then take the start of the race
        if (at == null) {
            at = new MillisecondsTimePoint(startOfLivePeriod.getTime()+1);
        }
        
        // whenLastTrackedRaceWasLive is null if there is no tracked race for fleet, or the tracked race hasn't started yet at the server time
        // when this DTO was assembled, or there were no GPS or wind data
        final boolean result =
                startOfLivePeriod != null &&
                endOfLivePeriod != null &&
                startOfLivePeriod.getTime() <= at.asMillis() &&
                at.asMillis() <= endOfLivePeriod.getTime();
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
    public Distance getDistanceTraveled(Competitor competitor, TimePoint timePoint) {
        NavigableSet<MarkPassing> markPassings = getMarkPassings(competitor);
        if (markPassings.isEmpty()) {
            return null;
        } else {
            TimePoint end = timePoint;
            if (markPassings.last().getWaypoint() == getRace().getCourse().getLastWaypoint()
                    && timePoint.compareTo(markPassings.last().getTimePoint()) > 0) {
                // competitor has finished race; use time point of crossing the finish line
                end = markPassings.last().getTimePoint();
            }
            return getTrack(competitor).getDistanceTraveled(markPassings.first().getTimePoint(), end);
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
                throw new IllegalArgumentException("Waypoint " + endOfLeg + " isn't start of any leg in "
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
            } else if (indexOfWaypoint == Util.size(getRace().getCourse().getWaypoints()) - 1) {
                throw new IllegalArgumentException("Waypoint " + startOfLeg + " isn't start of any leg in "
                        + getRace().getCourse());
            }
            return trackedLegs.get(race.getCourse().getLegs().get(indexOfWaypoint));
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
    public Competitor getOverallLeader(TimePoint timePoint) throws NoWindException {
        try {
            Competitor result = null;
            List<Competitor> ranks = getCompetitorsFromBestToWorst(timePoint);
            if (ranks != null && !ranks.isEmpty()) {
                result = ranks.iterator().next();
            }
            return result;
        } catch (NoWindError e) {
            throw e.getCause();
        }
    }

    @Override
    public int getRank(Competitor competitor, TimePoint timePoint) throws NoWindException {
        try {
            int result;
            if (getMarkPassings(competitor).isEmpty()) {
                result = 0;
            } else {
                result = getCompetitorsFromBestToWorst(timePoint).indexOf(competitor) + 1;
            }
            return result;
        } catch (NoWindError e) {
            throw e.getCause();
        }
    }

    @Override
    public List<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint) throws NoWindException {
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
        final boolean lockForWrite;
        synchronized (competitorRankings) {
            rankedCompetitors = competitorRankings.get(timePoint);
            if (rankedCompetitors == null) {
                lockForWrite = true;
            } else {
                lockForWrite = false;
            }
        }
        if (lockForWrite) {
            LockUtil.lockForWrite(readWriteLock);
        } else {
            LockUtil.lockForRead(readWriteLock);
        }
        try {
            if (rankedCompetitors == null) {
                rankedCompetitors = competitorRankings.get(timePoint); // try again; maybe a writer released the write
                                                                       // lock after updating the cache
                if (rankedCompetitors == null) {
                    RaceRankComparator comparator = new RaceRankComparator(this, timePoint);
                    rankedCompetitors = new ArrayList<Competitor>();
                    for (Competitor c : getRace().getCompetitors()) {
                        rankedCompetitors.add(c);
                    }
                    Collections.sort(rankedCompetitors, comparator);
                    synchronized (competitorRankings) {
                        competitorRankings.put(timePoint, rankedCompetitors);
                    }
                }
            }
            return rankedCompetitors;
        } finally {
            if (lockForWrite) {
                LockUtil.unlockAfterWrite(readWriteLock);
            } else {
                LockUtil.unlockAfterRead(readWriteLock);
            }
        }
    }

    @Override
    public Distance getAverageCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalysis)
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
            result = getAverageCrossTrackError(competitor, from, timePoint, /* upwindOnly */true, waitForLatestAnalysis);
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Distance getAverageCrossTrackError(Competitor competitor, TimePoint from, TimePoint to, boolean upwindOnly,
            boolean waitForLatestAnalysis) throws NoWindException {
        Distance result;
        result = crossTrackErrorCache
                .getAverageCrossTrackError(competitor, from, to, upwindOnly, waitForLatestAnalysis);
        return result;
    }

    @Override
    public TrackedLegOfCompetitor getCurrentLeg(Competitor competitor, TimePoint timePoint) {
        NavigableSet<MarkPassing> competitorMarkPassings = markPassingsForCompetitor.get(competitor);
        DummyMarkPassingWithTimePointOnly markPassingTimePoint = new DummyMarkPassingWithTimePointOnly(timePoint);
        TrackedLegOfCompetitor result = null;
        if (!competitorMarkPassings.isEmpty()) {
            MarkPassing lastMarkPassingAtOfBeforeTimePoint = competitorMarkPassings.floor(markPassingTimePoint);
            if (lastMarkPassingAtOfBeforeTimePoint != null) {
                Waypoint waypointPassedLastAtOrBeforeTimePoint = lastMarkPassingAtOfBeforeTimePoint.getWaypoint();
                // don't return a leg if competitor has already finished last leg and therefore the race
                if (waypointPassedLastAtOrBeforeTimePoint != getRace().getCourse().getLastWaypoint()) {
                    result = getTrackedLegStartingAt(waypointPassedLastAtOrBeforeTimePoint).getTrackedLeg(competitor);
                }
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
    public Distance getStartAdvantage(Competitor competitor, double secondsIntoTheRace) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MarkPassing getMarkPassing(Competitor competitor, Waypoint waypoint) {
        final NavigableSet<MarkPassing> markPassings = getMarkPassings(competitor);
        if (markPassings != null) {
            lockForRead(markPassings);
            try {
                for (MarkPassing markPassing : markPassings) {
                    if (markPassing.getWaypoint() == waypoint) {
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
        GPSFixTrack<Mark, GPSFix> result = markTracks.get(mark);
        if (result == null) {
            // try again, this time with more expensive synchronization
            synchronized (markTracks) {
                LockUtil.lockForRead(serializationLock);
                try {
                    result = markTracks.get(mark);
                    if (result == null) {
                        result = createMarkTrack(mark);
                        markTracks.put(mark, result);
                    }
                } finally {
                    LockUtil.unlockAfterRead(serializationLock);
                }
            }
        }
        return result;
    }

    protected DynamicGPSFixTrackImpl<Mark> createMarkTrack(Mark mark) {
        return new DynamicGPSFixTrackImpl<Mark>(mark, millisecondsOverWhichToAverageSpeed);
    }

    @Override
    public Position getApproximatePosition(Waypoint waypoint, TimePoint timePoint) {
        Position result = null;
        for (Mark mark : waypoint.getMarks()) {
            Position nextPos = getOrCreateTrack(mark).getEstimatedPosition(timePoint, /* extrapolate */false);
            if (result == null) {
                result = nextPos;
            } else if (nextPos != null) {
                result = result.translateGreatCircle(result.getBearingGreatCircle(nextPos), result.getDistance(nextPos)
                        .scale(0.5));
            }
        }
        return result;
    }

    /**
     * For wind sources of the special type {@link WindSourceType#COMBINED}, emits a new {@link CombinedWindTrackImpl}
     * which will not be added to {@link #windTracks} and will not lead to the wind source being listed in
     * {@link #getWindSources()} or {@link #getWindSources(WindSourceType)}. For all other wind sources, checks
     * {@link #windTracks} for the respective source. If found, it's returned; otherwise the wind track is created
     * through the {@link #windStore} using {@link #createWindTrack(WindSource, long)} and added to {@link #windTracks}
     * before being returned.
     * 
     * @param delayForWindEstimationCacheInvalidation
     *            if <code>-1</code> and the parameter is accessed, it will be replaced by
     *            {@link #getMillisecondsOverWhichToAverageWind()}/2
     */
    @Override
    public WindTrack getOrCreateWindTrack(WindSource windSource, long delayForWindEstimationCacheInvalidation) {
        WindTrack result;
        if (windSource.getType() == WindSourceType.COMBINED) {
            if (combinedWindTrack == null) {
                combinedWindTrack = new CombinedWindTrackImpl(this, WindSourceType.COMBINED.getBaseConfidence());
            }
            result = combinedWindTrack;
        } else {
            synchronized (windTracks) {
                result = windTracks.get(windSource);
                if (result == null) {
                    result = createWindTrack(windSource,
                            delayForWindEstimationCacheInvalidation == -1 ? getMillisecondsOverWhichToAverageWind() / 2
                                    : delayForWindEstimationCacheInvalidation);
                    LockUtil.lockForRead(serializationLock);
                    try {
                        windTracks.put(windSource, result);
                    } finally {
                        LockUtil.unlockAfterRead(serializationLock);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public WindTrack getOrCreateWindTrack(WindSource windSource) {
        return getOrCreateWindTrack(windSource, -1);
    }

    /**
     * Creates a wind track for the <code>windSource</code> specified and stores it in {@link #windTracks}. The
     * averaging interval is set according to the averaging interval set for all other wind sources, or the default if
     * no other wind source exists yet.
     */
    protected WindTrack createWindTrack(WindSource windSource, long delayForWindEstimationCacheInvalidation) {
        WindTrack result = windStore.getWindTrack(trackedRegatta, this, windSource, millisecondsOverWhichToAverageWind,
                delayForWindEstimationCacheInvalidation);
        return result;
    }

    @Override
    public boolean hasWindData() {
        boolean result = false;
        Course course = getRace().getCourse();
        Waypoint firstWaypoint = course.getFirstWaypoint();
        TimePoint timepoint = startTime != null ? startTime : startOfTrackingReceived;
        if (firstWaypoint != null && timepoint != null) {
            Position position = getApproximatePosition(firstWaypoint, timepoint);
            if (position != null) {
                Wind wind = getWind(position, timepoint);
                if (wind != null) {
                    result = true;
                }
            }
        }
        return result;
    }

    @Override
    public boolean hasGPSData() {
        boolean result = false;
        if (!tracks.values().isEmpty()) {
            for (GPSFixTrack<Competitor, GPSFixMoving> gpsTrack : tracks.values()) {
                if (gpsTrack.getFirstRawFix() != null) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public Wind getWind(Position p, TimePoint at) {
        return getWind(p, at, getWindSourcesToExclude());
    }

    @Override
    public Wind getWind(Position p, TimePoint at, Iterable<WindSource> windSourcesToExclude) {
        final WindWithConfidence<Pair<Position, TimePoint>> windWithConfidence = getWindWithConfidence(p, at,
                windSourcesToExclude);
        return windWithConfidence == null ? null : windWithConfidence.getObject();
    }

    @Override
    public WindWithConfidence<Pair<Position, TimePoint>> getWindWithConfidence(Position p, TimePoint at) {
        return getWindWithConfidence(p, at, getWindSourcesToExclude());
    }

    @Override
    public Iterable<WindSource> getWindSourcesToExclude() {
        synchronized (windSourcesToExclude) {
            return Collections.unmodifiableCollection(windSourcesToExclude);
        }
    }

    @Override
    public void setWindSourcesToExclude(Iterable<? extends WindSource> windSourcesToExclude) {
        Set<WindSource> old = new HashSet<>(this.windSourcesToExclude);
        synchronized (this.windSourcesToExclude) {
            LockUtil.lockForRead(serializationLock);
            try {
                this.windSourcesToExclude.clear();
                for (WindSource windSourceToExclude : windSourcesToExclude) {
                    this.windSourcesToExclude.add(windSourceToExclude);
                }
            } finally {
                LockUtil.unlockAfterRead(serializationLock);
            }
        }
        if (!old.equals(this.windSourcesToExclude)) {
            clearAllCachesExceptManeuvers();
            triggerManeuverCacheRecalculationForAllCompetitors();
        }
    }

    @Override
    public WindWithConfidence<Pair<Position, TimePoint>> getWindWithConfidence(Position p, TimePoint at,
            Iterable<WindSource> windSourcesToExclude) {
        boolean canUseSpeedOfAtLeastOneWindSource = false;
        Weigher<Pair<Position, TimePoint>> timeWeigherThatPretendsToAlsoWeighPositions = new PositionAndTimePointWeigher(
        /* halfConfidenceAfterMilliseconds */10000l);
        ConfidenceBasedWindAverager<Pair<Position, TimePoint>> averager = ConfidenceFactory.INSTANCE
                .createWindAverager(timeWeigherThatPretendsToAlsoWeighPositions);
        List<WindWithConfidence<Pair<Position, TimePoint>>> windFixesWithConfidences = new ArrayList<WindWithConfidence<Pair<Position, TimePoint>>>();
        for (WindSource windSource : getWindSources()) {
            // TODO consider parallelizing and consider caching
            if (!Util.contains(windSourcesToExclude, windSource)) {
                WindTrack track = getOrCreateWindTrack(windSource);
                WindWithConfidence<Pair<Position, TimePoint>> windWithConfidence = track.getAveragedWindWithConfidence(
                        p, at);
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
        synchronized (directionFromStartToNextMarkCache) {
            future = directionFromStartToNextMarkCache.get(at);
            if (future == null) {
                newFuture = new FutureTask<Wind>(new Callable<Wind>() {
                    @Override
                    public Wind call() {
                        Wind result;
                        Leg firstLeg = getRace().getCourse().getFirstLeg();
                        if (firstLeg != null) {
                            Position firstLegEnd = getApproximatePosition(firstLeg.getTo(), at);
                            Position firstLegStart = getApproximatePosition(firstLeg.getFrom(), at);
                            if (firstLegStart != null && firstLegEnd != null) {
                                result = new WindImpl(firstLegStart, at, new KnotSpeedWithBearingImpl(1.0,
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
        if ((start == null) != (startTimeReceived == null) || (start != null && !start.equals(startTimeReceived))) {
            this.startTimeReceived = start;
            invalidateStartTime();
            invalidateMarkPassingTimes();
        }
    }

    protected TimePoint getStartTimeReceived() {
        return startTimeReceived;
    }

    protected void setStartOfTrackingReceived(TimePoint startOfTracking) {
        this.startOfTrackingReceived = startOfTracking;
    }

    protected void setEndOfTrackingReceived(TimePoint endOfTracking) {
        this.endOfTrackingReceived = endOfTracking;
    }

    /**
     * Schedules the clearing of the caches. If a cache clearing is already scheduled, this is a no-op.
     */
    private void clearAllCachesExceptManeuvers() {
        synchronized (cacheInvalidationTimerLock) {
            if (cacheInvalidationTimer == null) {
                cacheInvalidationTimer = new Timer("Cache invalidation timer for TrackedRaceImpl "
                        + getRace().getName());
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
        // expecting to hold the course's write lock
        invalidateMarkPassingTimes();
        LockUtil.lockForRead(serializationLock);
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
        } finally {
            LockUtil.unlockAfterRead(serializationLock);
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
        LockUtil.lockForRead(serializationLock);
        try {
            startToNextMarkCacheInvalidationListeners.put(mark, listener);
        } finally {
            LockUtil.unlockAfterRead(serializationLock);
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
            LockUtil.lockForRead(serializationLock);
            try {
                startToNextMarkCacheInvalidationListeners.remove(mark);
            } finally {
                LockUtil.unlockAfterRead(serializationLock);
            }
        }
    }

    @Override
    public void waypointRemoved(int zeroBasedIndex, Waypoint waypointThatGotRemoved) {
        invalidateMarkPassingTimes();
        LockUtil.lockForRead(serializationLock);
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
        } finally {
            LockUtil.unlockAfterRead(serializationLock);
        }
    }

    protected NamedReentrantReadWriteLock getMarkPassingsLock(Iterable<MarkPassing> markPassings) {
        synchronized (locksForMarkPassings) {
            NamedReentrantReadWriteLock lock = locksForMarkPassings.get(markPassings);
            if (lock == null) {
                lock = new NamedReentrantReadWriteLock("mark passings lock for tracked race " + getRace().getName(), /* fair */
                        false);
                locksForMarkPassings.put(markPassings, lock);
            }
            return lock;
        }
    }

    private void updateStartToNextMarkCacheInvalidationCacheListenersAfterWaypointRemoved(int zeroBasedIndex,
            Waypoint waypointThatGotRemoved) {
        if (zeroBasedIndex < 2) {
            // the observing listener on any previous mark will be GCed; we need to ensure
            // that the cache is recomputed
            clearDirectionFromStartToNextMarkCache();
            stopAndRemoveStartToNextMarkCacheInvalidationListener(waypointThatGotRemoved);
            Iterator<Waypoint> waypointsIter = getRace().getCourse().getWaypoints().iterator();
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

    @Override
    public TrackedRegatta getTrackedRegatta() {
        return trackedRegatta;
    }

    @Override
    public Wind getEstimatedWindDirection(Position position, TimePoint timePoint) {
        WindWithConfidence<TimePoint> estimatedWindWithConfidence = getEstimatedWindDirectionWithConfidence(position,
                timePoint);
        return estimatedWindWithConfidence == null ? null : estimatedWindWithConfidence.getObject();
    }

    @Override
    public WindWithConfidence<TimePoint> getEstimatedWindDirectionWithConfidence(Position position, TimePoint timePoint) {
        DummyMarkPassingWithTimePointOnly dummyMarkPassingForNow = new DummyMarkPassingWithTimePointOnly(timePoint);
        Weigher<TimePoint> weigher = ConfidenceFactory.INSTANCE.createExponentialTimeDifferenceWeigher(
        // use a minimum confidence to avoid the bearing to flip to 270deg in case all is zero
                getMillisecondsOverWhichToAverageSpeed(), /* minimum confidence */0.0000000001);
        Map<LegType, BearingWithConfidenceCluster<TimePoint>> bearings = clusterBearingsByLegType(timePoint, position,
                dummyMarkPassingForNow, weigher);
        // use the minimum confidence of the four "quadrants" as the result's confidence
        BearingWithConfidenceImpl<TimePoint> reversedUpwindAverage = null;
        int upwindNumberOfRelevantBoats = 0;
        double confidence = 0;
        int numberOfBoatsRelevantForEstimate = 0;
        BearingWithConfidence<TimePoint> resultBearing = null;
        if (bearings != null) {
            BearingWithConfidenceCluster<TimePoint>[] bearingClustersUpwind = bearings.get(LegType.UPWIND).splitInTwo(
                    getMinimumAngleBetweenDifferentTacksUpwind(), timePoint);
            if (!bearingClustersUpwind[0].isEmpty() && !bearingClustersUpwind[1].isEmpty()) {
                BearingWithConfidence<TimePoint> average0 = bearingClustersUpwind[0].getAverage(timePoint);
                BearingWithConfidence<TimePoint> average1 = bearingClustersUpwind[1].getAverage(timePoint);
                upwindNumberOfRelevantBoats = Math
                        .min(bearingClustersUpwind[0].size(), bearingClustersUpwind[1].size());
                confidence = Math.min(average0.getConfidence(), average1.getConfidence())
                        * getRace().getBoatClass().getUpwindWindEstimationConfidence(upwindNumberOfRelevantBoats);
                reversedUpwindAverage = new BearingWithConfidenceImpl<TimePoint>(average0.getObject()
                        .middle(average1.getObject()).reverse(), confidence, timePoint);
            }
            BearingWithConfidenceImpl<TimePoint> downwindAverage = null;
            int downwindNumberOfRelevantBoats = 0;
            BearingWithConfidenceCluster<TimePoint>[] bearingClustersDownwind = bearings.get(LegType.DOWNWIND)
                    .splitInTwo(getMinimumAngleBetweenDifferentTacksDownwind(), timePoint);
            if (!bearingClustersDownwind[0].isEmpty() && !bearingClustersDownwind[1].isEmpty()) {
                BearingWithConfidence<TimePoint> average0 = bearingClustersDownwind[0].getAverage(timePoint);
                BearingWithConfidence<TimePoint> average1 = bearingClustersDownwind[1].getAverage(timePoint);
                downwindNumberOfRelevantBoats = Math.min(bearingClustersDownwind[0].size(),
                        bearingClustersDownwind[1].size());
                confidence = Math.min(average0.getConfidence(), average1.getConfidence())
                        * getRace().getBoatClass().getDownwindWindEstimationConfidence(downwindNumberOfRelevantBoats);
                downwindAverage = new BearingWithConfidenceImpl<TimePoint>(average0.getObject().middle(
                        average1.getObject()), confidence, timePoint);
            }
            numberOfBoatsRelevantForEstimate = upwindNumberOfRelevantBoats + downwindNumberOfRelevantBoats;
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
        return resultBearing == null ? null : new WindWithConfidenceImpl<TimePoint>(new WindImpl(null, timePoint,
                new KnotSpeedWithBearingImpl(
                /* speedInKnots */numberOfBoatsRelevantForEstimate, resultBearing.getObject())),
                resultBearing.getConfidence(), resultBearing.getRelativeTo(), /* useSpeed */false);
    }

    // TODO confidences need to be computed not only based on timePoint but also on position: boats far away don't
    // contribute as confidently as boats close by
    private Map<LegType, BearingWithConfidenceCluster<TimePoint>> clusterBearingsByLegType(TimePoint timePoint,
            Position position, DummyMarkPassingWithTimePointOnly dummyMarkPassingForNow, Weigher<TimePoint> weigher) {
        Weigher<TimePoint> weigherForMarkPassingProximity = new HyperbolicTimeDifferenceWeigher(
                getMillisecondsOverWhichToAverageSpeed() * 5);
        Map<LegType, BearingWithConfidenceCluster<TimePoint>> bearings = new HashMap<LegType, BearingWithConfidenceCluster<TimePoint>>();
        for (LegType legType : LegType.values()) {
            bearings.put(legType, new BearingWithConfidenceCluster<TimePoint>(weigher));
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
                            if (!track.hasDirectionChange(timePoint, getManeuverDegreeAngleThreshold())) {
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
                                    BearingWithConfidenceCluster<TimePoint> bearingClusterForLegType = bearings
                                            .get(legType);
                                    bearingClusterForLegType.add(bearing);
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
        return bearings;
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
     * Based on the wind direction at <code>timePoint</code> and at position <code>where</code>, compares the
     * <code>boatBearing</code> to the wind's bearing at that time and place and determined the tack.
     * 
     * @throws NoWindException
     *             in case the wind cannot be determined because without a wind direction, the tack cannot be determined
     *             either
     */
    private Tack getTack(Position where, TimePoint timePoint, Bearing boatBearing) throws NoWindException {
        final Wind wind = getWind(where, timePoint);
        Tack result;
        if (wind == null) {
            throw new NoWindException("Can't determine wind direction in position " + where + " at " + timePoint
                    + ", therefore cannot determine tack");
        }
        Bearing windBearing = wind.getBearing();
        Bearing difference = windBearing.getDifferenceTo(boatBearing);
        result = difference.getDegrees() <= 0 ? Tack.PORT : Tack.STARBOARD;
        return result;
    }

    @Override
    public String toString() {
        return "TrackedRace for " + getRace();
    }

    @Override
    public List<GPSFixMoving> approximate(Competitor competitor, Distance maxDistance, TimePoint from, TimePoint to) {
        DouglasPeucker<Competitor, GPSFixMoving> douglasPeucker = new DouglasPeucker<Competitor, GPSFixMoving>(
                getTrack(competitor));
        return douglasPeucker.approximate(maxDistance, from, to);
    }

    protected void triggerManeuverCacheRecalculationForAllCompetitors() {
        final List<Competitor> shuffledCompetitors = new ArrayList<>();
        for (Competitor competitor : (getRace().getCompetitors())) {
            shuffledCompetitors.add(competitor);
        }
        Collections.shuffle(shuffledCompetitors);
        for (Competitor competitor : shuffledCompetitors) {
            triggerManeuverCacheRecalculation(competitor);
        }
    }

    protected void triggerManeuverCacheRecalculation(final Competitor competitor) {
        maneuverCache.triggerUpdate(competitor, /* updateInterval */null);
    }

    private Triple<TimePoint, TimePoint, List<Maneuver>> computeManeuvers(Competitor competitor) throws NoWindException {
        logger.fine("computeManeuvers(" + competitor.getName() + ") called in tracked race " + this);
        long startedAt = System.currentTimeMillis();
        // compute the maneuvers for competitor
        Triple<TimePoint, TimePoint, List<Maneuver>> result = null;
        NavigableSet<MarkPassing> markPassings = getMarkPassings(competitor);
        boolean markPassingsNotEmpty;
        TimePoint extendedFrom = null;
        MarkPassing crossedFinishLine = null;
        // getLastWaypoint() will wait for a read lock on the course; do this outside the synchronized block to avoid
        // deadlocks
        final Waypoint lastWaypoint = getRace().getCourse().getLastWaypoint();
        lockForRead(markPassings);
        try {
            markPassingsNotEmpty = markPassings != null && !markPassings.isEmpty();
            if (markPassingsNotEmpty) {
                extendedFrom = markPassings.iterator().next().getTimePoint();
                crossedFinishLine = getMarkPassing(competitor, lastWaypoint);
            }
        } finally {
            unlockAfterRead(markPassings);
        }
        if (markPassingsNotEmpty) {
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
                    List<Maneuver> extendedResultForCache = detectManeuvers(
                            competitor,
                            approximate(competitor,
                                    getRace().getBoatClass().getMaximumDistanceForCourseApproximation(), extendedFrom,
                                    extendedTo));
                    result = new Triple<TimePoint, TimePoint, List<Maneuver>>(extendedFrom, extendedTo,
                            extendedResultForCache);
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
            } // else competitor has no fixes to consider; remove any maneuver cache entry
        } // else competitor hasn't started yet; remove any maneuver cache entry
        logger.fine("computeManeuvers(" + competitor.getName() + ") called in tracked race " + this + " took "
                + (System.currentTimeMillis() - startedAt) + "ms");
        return result;
    }

    /**
     * Tries to detect maneuvers on the <code>competitor</code>'s track based on a number of approximating fixes. The
     * fixes contain bearing information, but this is not the bearing leading to the next approximation fix but the
     * bearing the boat had at the time of the approximating fix which is taken from the original track.
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
     * @return an empty list if no maneuver is detected for <code>competitor</code> between <code>from</code> and
     *         <code>to</code>, or else the list of maneuvers detected.
     */
    private List<Maneuver> detectManeuvers(Competitor competitor, List<GPSFixMoving> approximatingFixesToAnalyze)
            throws NoWindException {
        List<Maneuver> result = new ArrayList<Maneuver>();
        if (approximatingFixesToAnalyze.size() > 2) {
            List<Pair<GPSFixMoving, CourseChange>> courseChangeSequenceInSameDirection = new ArrayList<Pair<GPSFixMoving, CourseChange>>();
            Iterator<GPSFixMoving> approximationPointsIter = approximatingFixesToAnalyze.iterator();
            GPSFixMoving previous = approximationPointsIter.next();
            GPSFixMoving current = approximationPointsIter.next();
            // the bearings in these variables are between approximation points
            SpeedWithBearing speedWithBearingOnApproximationFromPreviousToCurrent = previous
                    .getSpeedAndBearingRequiredToReach(current);
            SpeedWithBearing speedWithBearingOnApproximationAtBeginningOfUnidirectionalCourseChanges = speedWithBearingOnApproximationFromPreviousToCurrent;
            SpeedWithBearing speedWithBearingOnApproximationFromCurrentToNext; // will certainly be assigned because
            // iter's collection's size > 2
            do {
                GPSFixMoving next = approximationPointsIter.next();
                speedWithBearingOnApproximationFromCurrentToNext = current.getSpeedAndBearingRequiredToReach(next);
                // compute course change on "approximation track"
                CourseChange courseChange = speedWithBearingOnApproximationFromPreviousToCurrent
                        .getCourseChangeRequiredToReach(speedWithBearingOnApproximationFromCurrentToNext);
                Pair<GPSFixMoving, CourseChange> courseChangeAtFix = new Pair<GPSFixMoving, CourseChange>(current,
                        courseChange);
                if (!courseChangeSequenceInSameDirection.isEmpty()
                        && Math.signum(courseChangeSequenceInSameDirection.get(0).getB().getCourseChangeInDegrees()) != Math
                                .signum(courseChange.getCourseChangeInDegrees())) {
                    // course change in different direction; cluster the course changes in same direction so far, then
                    // start new list
                    List<Maneuver> maneuvers = groupChangesInSameDirectionIntoManeuvers(competitor,
                            speedWithBearingOnApproximationAtBeginningOfUnidirectionalCourseChanges,
                            courseChangeSequenceInSameDirection);
                    result.addAll(maneuvers);
                    courseChangeSequenceInSameDirection.clear();
                    speedWithBearingOnApproximationAtBeginningOfUnidirectionalCourseChanges = speedWithBearingOnApproximationFromPreviousToCurrent;
                }
                courseChangeSequenceInSameDirection.add(courseChangeAtFix);
                previous = current;
                current = next;
                speedWithBearingOnApproximationFromPreviousToCurrent = speedWithBearingOnApproximationFromCurrentToNext;
            } while (approximationPointsIter.hasNext());
            if (!courseChangeSequenceInSameDirection.isEmpty()) {
                result.addAll(groupChangesInSameDirectionIntoManeuvers(competitor,
                        speedWithBearingOnApproximationAtBeginningOfUnidirectionalCourseChanges,
                        courseChangeSequenceInSameDirection));
            }
        }
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
    public List<Maneuver> getManeuvers(Competitor competitor, TimePoint from, TimePoint to, boolean waitForLatest)
            throws NoWindException {
        Triple<TimePoint, TimePoint, List<Maneuver>> allManeuvers = maneuverCache.get(competitor, waitForLatest);
        List<Maneuver> result;
        if (allManeuvers == null) {
            result = Collections.emptyList();
        } else {
            result = extractInterval(from, to, allManeuvers.getC());
        }
        return result;
    }

    private <T extends Timed> List<T> extractInterval(TimePoint from, TimePoint to, List<T> listOfTimed) {
        List<T> result;
        result = new LinkedList<T>();
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
     * where the distances of those course changes are less than two hull lengths apart. For those, a single
     * {@link Maneuver} object is created and added to the resulting list. The maneuver sums up the direction changes of
     * the individual {@link CourseChange} objects. This can result in direction changes of more than 180 degrees in one
     * direction which may, e.g., represent a penalty circle or a mark rounding maneuver. As the maneuver's time point,
     * the average time point of the course changes that went into the maneuver construction is used.
     * <p>
     * 
     * @param speedWithBearingOnApproximationAtBeginning
     *            the speed/bearing before the first approximating fix passed in
     *            <code>courseChangeSequenceInSameDirection</code>
     * @param courseChangeSequenceInSameDirection
     *            all expected to have equal {@link CourseChange#to()} values
     * 
     * @return a non-<code>null</code> list
     */
    private List<Maneuver> groupChangesInSameDirectionIntoManeuvers(Competitor competitor,
            SpeedWithBearing speedWithBearingOnApproximationAtBeginning,
            List<Pair<GPSFixMoving, CourseChange>> courseChangeSequenceInSameDirection) throws NoWindException {
        List<Maneuver> result = new ArrayList<Maneuver>();
        List<Pair<GPSFixMoving, CourseChange>> group = new ArrayList<Pair<GPSFixMoving, CourseChange>>();
        if (!courseChangeSequenceInSameDirection.isEmpty()) {
            Distance threeHullLengths = competitor.getBoat().getBoatClass().getHullLength().scale(3);
            SpeedWithBearing beforeGroupOnApproximation = speedWithBearingOnApproximationAtBeginning; // speed/bearing
                                                                                                      // before group
            SpeedWithBearing beforeCurrentCourseChangeOnApproximation = beforeGroupOnApproximation; // speed/bearing
                                                                                                    // before current
                                                                                                    // course change
            Iterator<Pair<GPSFixMoving, CourseChange>> iter = courseChangeSequenceInSameDirection.iterator();
            double totalCourseChangeInDegrees = 0.0;
            long totalMilliseconds = 0l;
            SpeedWithBearing afterCurrentCourseChange = null; // sure to be set because iter's collection is not empty
            // and the first use requires group not to be empty which can only happen after the first group.add
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
                    Maneuver maneuver = createManeuverFromGroupOfCourseChanges(competitor, beforeGroupOnApproximation,
                            group, afterCurrentCourseChange, totalCourseChangeInDegrees, totalMilliseconds);
                    result.add(maneuver);
                    group.clear();
                    totalCourseChangeInDegrees = 0.0;
                    totalMilliseconds = 0l;
                    beforeGroupOnApproximation = beforeCurrentCourseChangeOnApproximation;
                }
                afterCurrentCourseChange = beforeCurrentCourseChangeOnApproximation
                        .applyCourseChange(currentFixAndCourseChange.getB());
                totalMilliseconds += currentFixAndCourseChange.getA().getTimePoint().asMillis();
                totalCourseChangeInDegrees += currentFixAndCourseChange.getB().getCourseChangeInDegrees();
                group.add(currentFixAndCourseChange);
                beforeCurrentCourseChangeOnApproximation = afterCurrentCourseChange; // speed/bearing after course
                // change
            } while (iter.hasNext());
            if (!group.isEmpty()) {
                result.add(createManeuverFromGroupOfCourseChanges(competitor, beforeGroupOnApproximation, group,
                        afterCurrentCourseChange, totalCourseChangeInDegrees, totalMilliseconds));
            }
        }
        return result;
    }

    private Maneuver createManeuverFromGroupOfCourseChanges(Competitor competitor,
            SpeedWithBearing speedWithBearingOnApproximationAtBeginning, List<Pair<GPSFixMoving, CourseChange>> group,
            SpeedWithBearing speedWithBearingOnApproximationAtEnd, double totalCourseChangeInDegrees,
            long totalMilliseconds) throws NoWindException {
        MillisecondsTimePoint timePointBeforeManeuver = new MillisecondsTimePoint(group.get(0).getA().getTimePoint()
                .asMillis()
                - getApproximateManeuverDurationInMilliseconds() / 2);
        MillisecondsTimePoint timePointAfterManeuver = new MillisecondsTimePoint(group.get(group.size() - 1).getA()
                .getTimePoint().asMillis()
                + getApproximateManeuverDurationInMilliseconds() / 2);
        TimePoint maneuverTimePoint = computeManeuverTimepoint(competitor, timePointBeforeManeuver,
                timePointAfterManeuver);
        Position maneuverPosition = getTrack(competitor)
                .getEstimatedPosition(maneuverTimePoint, /* extrapolate */false);
        Tack tackBeforeManeuver = getTack(maneuverPosition, timePointBeforeManeuver,
                speedWithBearingOnApproximationAtBeginning.getBearing());
        Tack tackAfterManeuver = getTack(maneuverPosition, timePointAfterManeuver,
                speedWithBearingOnApproximationAtEnd.getBearing());
        ManeuverType maneuverType;
        Distance maneuverLoss = null;
        // the TrackedLegOfCompetitor variables may be null, e.g., in case the time points are before or after the race
        TrackedLegOfCompetitor legBeforeManeuver = getTrackedLeg(competitor, timePointBeforeManeuver);
        TrackedLegOfCompetitor legAfterManeuver = getTrackedLeg(competitor, timePointAfterManeuver);
        Waypoint waypointPassed = null; // set for MARK_PASSING maneuvers only
        NauticalSide sideToWhichWaypointWasPassed = null; // set for MARK_PASSING maneuvers only
        // check for mask passing first; a tacking / jibe-setting mark rounding thus takes precedence over being
        // detected as a penalty circle
        if (legBeforeManeuver != legAfterManeuver
                // a maneuver at the start line is not to be considered a MARK_PASSING maneuver; show a tack as a tack
                && legAfterManeuver != null
                && legAfterManeuver.getLeg().getFrom() != getRace().getCourse().getFirstWaypoint()) {
            maneuverType = ManeuverType.MARK_PASSING;
            waypointPassed = legAfterManeuver.getLeg().getFrom();
            sideToWhichWaypointWasPassed = totalCourseChangeInDegrees < 0 ? NauticalSide.PORT : NauticalSide.STARBOARD;
        } else if (Math.abs(totalCourseChangeInDegrees) > PENALTY_CIRCLE_DEGREES_THRESHOLD) {
            maneuverType = ManeuverType.PENALTY_CIRCLE;
            if (legBeforeManeuver != null) {
                maneuverLoss = legBeforeManeuver.getManeuverLoss(timePointBeforeManeuver, maneuverTimePoint,
                        timePointAfterManeuver);
            }
        } else {
            if (tackBeforeManeuver != tackAfterManeuver) {
                LegType legType = legBeforeManeuver != null ? getTrackedLeg(legBeforeManeuver.getLeg()).getLegType(
                        timePointBeforeManeuver) : legAfterManeuver != null ? getTrackedLeg(legAfterManeuver.getLeg())
                        .getLegType(timePointAfterManeuver) : null;
                if (legType != null) {
                    // tack or jibe
                    switch (legType) {
                    case UPWIND:
                        maneuverType = ManeuverType.TACK;
                        if (legBeforeManeuver != null) {
                            maneuverLoss = legBeforeManeuver.getManeuverLoss(timePointBeforeManeuver,
                                    maneuverTimePoint, timePointAfterManeuver);
                        }
                        break;
                    case DOWNWIND:
                        maneuverType = ManeuverType.JIBE;
                        if (legBeforeManeuver != null) {
                            maneuverLoss = legBeforeManeuver.getManeuverLoss(timePointBeforeManeuver,
                                    maneuverTimePoint, timePointAfterManeuver);
                        }
                        break;
                    default:
                        maneuverType = ManeuverType.UNKNOWN;
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("Unknown maneuver for "
                                    + competitor
                                    + " at "
                                    + maneuverTimePoint
                                    + (legBeforeManeuver != null ? " on reaching leg " + legBeforeManeuver.getLeg()
                                            : " before start"));
                        }
                        break;
                    }
                } else {
                    maneuverType = ManeuverType.UNKNOWN;
                    logger.fine("Can't determine leg type because tracked legs for competitor " + competitor
                            + " cannot be determined for time points " + timePointBeforeManeuver + " and "
                            + timePointAfterManeuver);
                }
            } else {
                // heading up or bearing away
                Wind wind = getWind(maneuverPosition, maneuverTimePoint);
                Bearing windBearing = wind.getBearing();
                Bearing toWindBeforeManeuver = windBearing.getDifferenceTo(speedWithBearingOnApproximationAtBeginning
                        .getBearing());
                Bearing toWindAfterManeuver = windBearing.getDifferenceTo(speedWithBearingOnApproximationAtEnd
                        .getBearing());
                maneuverType = Math.abs(toWindBeforeManeuver.getDegrees()) < Math.abs(toWindAfterManeuver.getDegrees()) ? ManeuverType.HEAD_UP
                        : ManeuverType.BEAR_AWAY;
            }
        }
        final Maneuver maneuver;
        if (maneuverType == ManeuverType.MARK_PASSING) {
            maneuver = new MarkPassingManeuverImpl(maneuverType, tackAfterManeuver, maneuverPosition,
                    maneuverTimePoint, speedWithBearingOnApproximationAtBeginning,
                    speedWithBearingOnApproximationAtEnd, totalCourseChangeInDegrees, maneuverLoss, waypointPassed,
                    sideToWhichWaypointWasPassed);
        } else {
            maneuver = new ManeuverImpl(maneuverType, tackAfterManeuver, maneuverPosition, maneuverTimePoint,
                    speedWithBearingOnApproximationAtBeginning, speedWithBearingOnApproximationAtEnd,
                    totalCourseChangeInDegrees, maneuverLoss);
        }
        return maneuver;
    }

    /**
     * Computes the maneuver time point as the time point along between maneuver start and end where the competitor's
     * track has greatest change in course.
     */
    private TimePoint computeManeuverTimepoint(Competitor competitor, MillisecondsTimePoint timePointBeforeManeuver,
            MillisecondsTimePoint timePointAfterManeuver) {
        TimePoint result = timePointBeforeManeuver;
        GPSFixTrack<Competitor, GPSFixMoving> track = getTrack(competitor);
        GPSFixMoving lastFix = null;
        double maxAngleSpeedInDegreesPerSecond = 0;
        track.lockForRead();
        try {
            for (Iterator<GPSFixMoving> i = track.getFixesIterator(timePointBeforeManeuver, /* inclusive */true); i
                    .hasNext();) {
                GPSFixMoving fix = i.next();
                if (fix.getTimePoint().after(timePointAfterManeuver)) {
                    break;
                }
                if (lastFix != null) {
                    Bearing courseAtLastFix = track.getEstimatedSpeed(lastFix.getTimePoint()).getBearing();
                    Bearing courseAtFix = track.getEstimatedSpeed(fix.getTimePoint()).getBearing();
                    double angleSpeedInDegreesPerSecond = Math.abs((courseAtFix.getDifferenceTo(courseAtLastFix)
                            .getDegrees())
                            / (double) (fix.getTimePoint().asMillis() - lastFix.getTimePoint().asMillis()));
                    if (angleSpeedInDegreesPerSecond > maxAngleSpeedInDegreesPerSecond) {
                        maxAngleSpeedInDegreesPerSecond = angleSpeedInDegreesPerSecond;
                        result = lastFix.getTimePoint();
                    }
                }
                lastFix = fix;
            }
        } finally {
            track.unlockAfterRead();
        }
        return result;
    }

    /**
     * Fetches the boat class-specific parameter
     */
    private double getManeuverDegreeAngleThreshold() {
        return getRace().getBoatClass().getManeuverDegreeAngleThreshold();
    }

    private double getMinimumAngleBetweenDifferentTacksDownwind() {
        return getRace().getBoatClass().getMinimumAngleBetweenDifferentTacksDownwind();
    }

    private double getMinimumAngleBetweenDifferentTacksUpwind() {
        return getRace().getBoatClass().getMinimumAngleBetweenDifferentTacksUpwind();
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
        public void gpsFixReceived(GPSFix fix, Mark mark) {
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
    public Distance getWindwardDistanceToOverallLeader(Competitor competitor, TimePoint timePoint)
            throws NoWindException {
        final TrackedLegOfCompetitor trackedLeg = getTrackedLeg(competitor, timePoint);
        return trackedLeg == null ? null : trackedLeg.getWindwardDistanceToOverallLeader(timePoint);
    }

    @Override
    public Iterable<WindSource> getWindSources(WindSourceType type) {
        Set<WindSource> result = new HashSet<WindSource>();
        for (WindSource windSource : getWindSources()) {
            if (windSource.getType() == type) {
                result.add(windSource);
            }
        }
        return result;
    }

    @Override
    public Iterable<WindSource> getWindSources() {
        while (true) {
            try {
                return new HashSet<WindSource>(windTracks.keySet());
            } catch (ConcurrentModificationException cme) {
                logger.info("Caught " + cme + "; trying again.");
            }
        }
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
        } else if (oldStatus == TrackedRaceStatusEnum.LOADING && newStatus.getStatus() != TrackedRaceStatusEnum.LOADING) {
            resumeAllCachesNotUpdatingWhileLoading();
        }

    }

    private void suspendAllCachesNotUpdatingWhileLoading() {
        crossTrackErrorCache.suspend();
        maneuverCache.suspend();
    }

    private void resumeAllCachesNotUpdatingWhileLoading() {
        crossTrackErrorCache.resume();
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
        this.attachedRaceLogs.put(raceLog.getId(), raceLog);
    }

    @Override
    public void detachRaceLog(Serializable identifier) {
        this.attachedRaceLogs.remove(identifier);
    }
    
    @Override
    public void detachAllRaceLogs() {
        this.attachedRaceLogs.clear();
    }

    @Override
    public RaceLog getRaceLog(Serializable identifier) {
        return attachedRaceLogs.get(identifier);
    }

    @Override
    public Distance getDistanceToStartLine(Competitor competitor, TimePoint timePoint) {
        Waypoint startWaypoint = getRace().getCourse().getFirstWaypoint();
        final Distance result;
        if (startWaypoint == null) {
            result = null;
        } else {
            Position competitorPosition = getTrack(competitor).getEstimatedPosition(timePoint, /* extrapolate */false);
            if (competitorPosition == null) {
                result = null;
            } else {
                Iterable<Mark> marks = startWaypoint.getControlPoint().getMarks();
                Iterator<Mark> marksIterator = marks.iterator();
                Mark first = marksIterator.next();
                Position firstPosition = getOrCreateTrack(first).getEstimatedPosition(timePoint, /* extrapolate */false);
                if (firstPosition == null) {
                    result = null;
                } else {
                    if (marksIterator.hasNext()) {
                        // it's a line / gate
                        Mark second = marksIterator.next();
                        Position secondPosition = getOrCreateTrack(second).getEstimatedPosition(timePoint, /* extrapolate */
                                false);
                        final Bearing bearingGreatCircle = firstPosition.getBearingGreatCircle(secondPosition);
                        if (bearingGreatCircle == null) {
                            result = null;
                        } else {
                            Position competitorProjectedOntoStartLine = competitorPosition.projectToLineThrough(
                                    firstPosition, bearingGreatCircle);
                            result = competitorPosition.getDistance(competitorProjectedOntoStartLine);
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

    /**
     * The method has protected scope in order to allow for testing. Based on the bearing from the start waypoint to the
     * next mark, identifies which of the two marks of the start line is on starboard. If the start waypoint has only
     * one mark, that mark is returned. If the start line has two marks but the course has no other waypoint,
     * <code>null<code> is returned. If the course has no waypoints at all, <code>null</code> is returned.
     */
    protected Position getStarboardMarkOfStartlinePosition(TimePoint at) {
        final Position starboardMarkPosition;
        final Course course = getRace().getCourse();
        Iterator<Waypoint> waypointsIter = course.getWaypoints().iterator();
        if (waypointsIter.hasNext()) {
            Waypoint startWaypoint = waypointsIter.next();
            if (Util.size(startWaypoint.getMarks()) == 1) {
                starboardMarkPosition = getOrCreateTrack(startWaypoint.getMarks().iterator().next())
                        .getEstimatedPosition(at, /* extrapolate */false);
            } else {
                if (waypointsIter.hasNext()) {
                    final Position approximatePositionOfStart = getApproximatePosition(startWaypoint, at);
                    // find out which of the marks is the starboard side of the line by checking the angle
                    // between competitor's COG and bearing to mark
                    Iterator<Mark> markIter = startWaypoint.getMarks().iterator();
                    Mark mark1 = markIter.next();
                    Mark mark2 = markIter.next();
                    final Position estimatedPositionMark1 = getOrCreateTrack(mark1).getEstimatedPosition(at, /* extrapolate */false);
                    final Position estimatedPositionMark2 = getOrCreateTrack(mark2).getEstimatedPosition(at, /* extrapolate */false);
                    Bearing bearingFromMark1ToMark2 = estimatedPositionMark1.getBearingGreatCircle(estimatedPositionMark2);
                    Waypoint nextWaypoint = waypointsIter.next();
                    Bearing bearingFromStartToNextWaypoint = approximatePositionOfStart
                            .getBearingGreatCircle(getApproximatePosition(nextWaypoint, at));
                    Bearing diffBetweenFromMark1ToMark2AndNextWaypoint = bearingFromMark1ToMark2.getDifferenceTo(bearingFromStartToNextWaypoint);
                    if (diffBetweenFromMark1ToMark2AndNextWaypoint.getDegrees() > 0) {
                        starboardMarkPosition = estimatedPositionMark1;
                    } else {
                        starboardMarkPosition = estimatedPositionMark2;
                    }
                } else {
                    // only one waypoint in course; cannot determine bearing to next mark
                    starboardMarkPosition = null;
                }
            }
        } else {
            // only one waypoint in course; can't determine bearing to next waypoint
            starboardMarkPosition = null;
        }
        return starboardMarkPosition;
    }
}
