package com.sap.sailing.domain.tracking.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.BearingWithConfidence;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Buoy;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseChange;
import com.sap.sailing.domain.base.CourseListener;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.SpeedWithBearing;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.Timed;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BearingWithConfidenceImpl;
import com.sap.sailing.domain.base.impl.DouglasPeucker;
import com.sap.sailing.domain.base.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.MeterDistance;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.EventNameAndRaceName;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NoWindError;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
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
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.GPSTrackListener;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedEvent;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;

public abstract class TrackedRaceImpl implements TrackedRace, CourseListener {
    private static final long serialVersionUID = -4825546964220003507L;

    private static final Logger logger = Logger.getLogger(TrackedRaceImpl.class.getName());

    private static final double PENALTY_CIRCLE_DEGREES_THRESHOLD = 320;

    // TODO make this variable
    private static final long DELAY_FOR_CACHE_CLEARING_IN_MILLISECONDS = 7500;

    private final RaceDefinition race;

    private final TrackedEvent trackedEvent;

    /**
     * By default, all wind sources are used, none are excluded. However, e.g., for performance reasons, particular wind
     * sources such as the track-based estimation wind source, may be excluded by adding them to this set.
     */
    private final Set<WindSource> windSourcesToExclude;

    /**
     * Keeps the oldest timestamp that is fed into this tracked race, either from a boat fix, a buoy fix, a race
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
     * The calculated start times of the legs
     */
    private final List<TimePoint> startTimesOfLegs;

    /**
     * The latest time point contained by any of the events received and processed
     */
    private TimePoint timePointOfNewestEvent;

    /**
     * Time stamp that the event received last from the underlying push service carried on it
     */
    private TimePoint timePointOfLastEvent;

    private long updateCount;

    private final Map<TimePoint, List<Competitor>> competitorRankings;

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
    private final Map<Competitor, Triple<TimePoint, TimePoint, List<Maneuver>>> maneuverCache;

    /**
     * A tracked race can maintain a number of sources for wind information from which a client can select. As all
     * intra-leg computations are done dynamically based on wind information, selecting a different wind information
     * source can alter the intra-leg results. See {@link #currentWindSource}.
     */
    private final Map<WindSource, WindTrack> windTracks;

    private final Map<TimePoint, Wind> directionFromStartToNextMarkCache;

    private final Map<Buoy, GPSFixTrack<Buoy, GPSFix>> buoyTracks;

    protected long millisecondsOverWhichToAverageSpeed;

    private final Map<Buoy, StartToNextMarkCacheInvalidationListener> startToNextMarkCacheInvalidationListeners;

    protected long millisecondsOverWhichToAverageWind;

    private final WindStore windStore;

    private transient Timer cacheInvalidationTimer;

    public TrackedRaceImpl(TrackedEvent trackedEvent, RaceDefinition race, WindStore windStore,
            long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed) {
        this(trackedEvent, race, windStore, millisecondsOverWhichToAverageWind, millisecondsOverWhichToAverageSpeed,
        /* delay for wind estimation cache invalidation */millisecondsOverWhichToAverageWind / 2);
    }

    public TrackedRaceImpl(TrackedEvent trackedEvent, RaceDefinition race, WindStore windStore,
            long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed,
            long delayForWindEstimationCacheInvalidation) {
        super();
        this.updateCount = 0;
        this.race = race;
        this.windStore = windStore;
        this.windSourcesToExclude = new HashSet<WindSource>();
        this.directionFromStartToNextMarkCache = new HashMap<TimePoint, Wind>();
        this.millisecondsOverWhichToAverageSpeed = millisecondsOverWhichToAverageSpeed;
        this.millisecondsOverWhichToAverageWind = millisecondsOverWhichToAverageWind;
        this.startToNextMarkCacheInvalidationListeners = new HashMap<Buoy, TrackedRaceImpl.StartToNextMarkCacheInvalidationListener>();
        this.maneuverCache = new HashMap<Competitor, Util.Triple<TimePoint, TimePoint, List<Maneuver>>>();
        this.buoyTracks = new HashMap<Buoy, GPSFixTrack<Buoy, GPSFix>>();
        int i = 0;
        for (Waypoint waypoint : race.getCourse().getWaypoints()) {
            for (Buoy buoy : waypoint.getBuoys()) {
                getOrCreateTrack(buoy);
                if (i < 2) {
                    // add cache invalidation listeners for first and second waypoint's buoys for
                    // directionFromStartToNextMarkCache
                    addStartToNextMarkCacheInvalidationListener(buoy);
                }
            }
            i++;
        }
        trackedLegs = new LinkedHashMap<Leg, TrackedLeg>();
        synchronized (race.getCourse()) {
            for (Leg leg : race.getCourse().getLegs()) {
                trackedLegs.put(leg, createTrackedLeg(leg));
            }
            getRace().getCourse().addCourseListener(this);
        }
        markPassingsForCompetitor = new HashMap<Competitor, NavigableSet<MarkPassing>>();
        tracks = new HashMap<Competitor, GPSFixTrack<Competitor, GPSFixMoving>>();
        for (Competitor competitor : race.getCompetitors()) {
            markPassingsForCompetitor.put(competitor, new ConcurrentSkipListSet<MarkPassing>(
                    MarkPassingByTimeComparator.INSTANCE));
            tracks.put(competitor, new DynamicGPSFixMovingTrackImpl<Competitor>(competitor,
                    millisecondsOverWhichToAverageSpeed));
        }
        markPassingsForWaypoint = new HashMap<Waypoint, NavigableSet<MarkPassing>>();
        for (Waypoint waypoint : race.getCourse().getWaypoints()) {
            markPassingsForWaypoint.put(waypoint, new ConcurrentSkipListSet<MarkPassing>(
                    MarkPassingByTimeComparator.INSTANCE));
        }
        startTimesOfLegs = new ArrayList<TimePoint>();
        windTracks = new HashMap<WindSource, WindTrack>();
        windTracks.putAll(windStore.loadWindTracks(trackedEvent, this, millisecondsOverWhichToAverageWind));
        // by default, a tracked race offers one course-based wind estimation, one track-based wind estimation track and
        // one "WEB" track for manual or REST-based wind reception; other wind tracks may be added as fixes are received
        // for them.
        WindSource courseBasedWindSource = new WindSourceImpl(WindSourceType.COURSE_BASED);
        windTracks.put(courseBasedWindSource, windStore.getWindTrack(trackedEvent, this, courseBasedWindSource,
                millisecondsOverWhichToAverageWind, delayForWindEstimationCacheInvalidation));
        WindSource trackBasedWindSource = new WindSourceImpl(WindSourceType.TRACK_BASED_ESTIMATION);
        windTracks.put(trackBasedWindSource, windStore.getWindTrack(trackedEvent, this, trackBasedWindSource,
                millisecondsOverWhichToAverageWind, delayForWindEstimationCacheInvalidation));
        this.trackedEvent = trackedEvent;
        competitorRankings = new HashMap<TimePoint, List<Competitor>>();
    }

    /**
     * Precondition: race has already been set, e.g., in constructor before this methocd is called
     */
    abstract protected TrackedLeg createTrackedLeg(Leg leg);

    public RaceIdentifier getRaceIdentifier() {
        return new EventNameAndRaceName(getTrackedEvent().getEvent().getName(), getRace().getName());
    }

    @Override
    public NavigableSet<MarkPassing> getMarkPassings(Competitor competitor) {
        return markPassingsForCompetitor.get(competitor);
    }

    protected NavigableSet<MarkPassing> getMarkPassingsInOrderAsNavigableSet(Waypoint waypoint) {
        return markPassingsForWaypoint.get(waypoint);
    }

    @Override
    public Iterable<MarkPassing> getMarkPassingsInOrder(Waypoint waypoint) {
        return getMarkPassingsInOrderAsNavigableSet(waypoint);
    }

    @Override
    public TimePoint getStartOfTracking() {
        return startOfTrackingReceived;
    }

    @Override
    public TimePoint getEndOfTracking() {
        return endOfTrackingReceived;
    }

    protected void invalidateStartTime() {
        startTime = null;
    }

    protected void invalidateEndTime() {
        endTime = null;
    }

    protected void invalidateLegTimes() {
        startTimesOfLegs.clear();
    }

    /**
     * Calculates the start time of the race from various sources
     */
    @Override
    public TimePoint getStart() {
        if (startTime == null) {
            startTime = startTimeReceived;
            // If not null, check if the first mark passing for the start line is too much after the startTimeReceived;
            // if so, return an adjusted, later start time.
            // If no official start time was received, try to estimate the start time using the mark passings for the
            // start line.
            if (startTimeReceived != null) {
                TimePoint timeOfFirstMarkPassing = getFirstPassingTime(getRace().getCourse().getFirstWaypoint());
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
                startTime = calculateStartOfRaceFromMarkPassings(getMarkPassingsInOrderAsNavigableSet(getRace()
                        .getCourse().getFirstWaypoint()), getRace().getCompetitors());
            }
        }
        return startTime;
    }

    /**
     * Calculates the end time of the race from the mark passings of the last course waypoint
     */
    @Override
    public TimePoint getAssumedEnd() {
        if (endTime == null) {
            Iterable<MarkPassing> markPassingsInOrder = getMarkPassingsInOrder(getRace().getCourse().getLastWaypoint());
            synchronized (markPassingsInOrder) {
                for (MarkPassing passingFinishLine : markPassingsInOrder) {
                    endTime = passingFinishLine.getTimePoint();
                }
            }
        }
        return endTime;
    }

    private TimePoint getFirstPassingTime(Waypoint waypoint) {
        NavigableSet<MarkPassing> markPassingsInOrder = getMarkPassingsInOrderAsNavigableSet(waypoint);
        MarkPassing firstMarkPassing = null;
        synchronized (markPassingsInOrder) {
            if (!markPassingsInOrder.isEmpty()) {
                firstMarkPassing = markPassingsInOrder.first();
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
        synchronized (markPassings) {
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
        }
        return startOfRace;
    }

    @Override
    public boolean hasStarted(TimePoint at) {
        return getStart() != null && getStart().compareTo(at) <= 0;
    }

    protected void setStartTimeReceived(TimePoint start) {
        this.startTimeReceived = start;
        invalidateStartTime();
        invalidateLegTimes();
    }

    @Override
    public RaceDefinition getRace() {
        return race;
    }

    @Override
    public Iterable<TrackedLeg> getTrackedLegs() {
        return trackedLegs.values();
    }

    @Override
    public Iterable<TimePoint> getStartTimesOfTrackedLegs() {
        if (startTimesOfLegs.isEmpty()) {
            int legNumber = 1;
            // Remark: sometimes it can happen that a mark passing with a wrong time stamp breaks the right time order
            // of the leg times
            Date previousLegPassingTime = null;
            for (TrackedLeg trackedLeg : trackedLegs.values()) {
                if (legNumber == 1) {
                    // For the first leg the use of "firstPassingDate" is not correct,
                    // because boats can pass the start line before the actual start;
                    // therefore we are using the calculated start time here
                    TimePoint startOfRace = getStart();
                    if (startOfRace != null) {
                        startTimesOfLegs.add(startOfRace);
                    }
                }
                Waypoint to = trackedLeg.getLeg().getTo();
                NavigableSet<MarkPassing> markPassings = getMarkPassingsInOrderAsNavigableSet(to);
                if (markPassings != null && !markPassings.isEmpty()) {
                    // ensure the leg times are in the right time order; there may perhaps be left-overs for marks to be
                    // reached later that
                    // claim it has been passed in the past which may have been an accidental tracker read-out;
                    // the results of getMarkPassingsInOrder(to) has by definition an ascending time-point ordering
                    synchronized (markPassings) {
                        for (MarkPassing currentMarkPassing : markPassings) {
                            Date currentPassingDate = currentMarkPassing.getTimePoint().asDate();
                            if (previousLegPassingTime == null || currentPassingDate.after(previousLegPassingTime)) {
                                startTimesOfLegs.add(currentMarkPassing.getTimePoint());
                                previousLegPassingTime = currentPassingDate;
                                break;
                            }
                        }
                    }
                }
                legNumber++;
            }
        }

        return startTimesOfLegs;
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
        int indexOfWaypoint = getRace().getCourse().getIndexOfWaypoint(endOfLeg);
        if (indexOfWaypoint == -1) {
            throw new IllegalArgumentException("Waypoint " + endOfLeg + " not found in " + getRace().getCourse());
        } else if (indexOfWaypoint == 0) {
            throw new IllegalArgumentException("Waypoint " + endOfLeg + " isn't start of any leg in "
                    + getRace().getCourse());
        }
        return trackedLegs.get(race.getCourse().getLegs().get(indexOfWaypoint - 1));
    }

    @Override
    public TrackedLeg getTrackedLegStartingAt(Waypoint startOfLeg) {
        int indexOfWaypoint = getRace().getCourse().getIndexOfWaypoint(startOfLeg);
        if (indexOfWaypoint == -1) {
            throw new IllegalArgumentException("Waypoint " + startOfLeg + " not found in " + getRace().getCourse());
        } else if (indexOfWaypoint == Util.size(getRace().getCourse().getWaypoints()) - 1) {
            throw new IllegalArgumentException("Waypoint " + startOfLeg + " isn't start of any leg in "
                    + getRace().getCourse());
        }
        return trackedLegs.get(race.getCourse().getLegs().get(indexOfWaypoint));
    }

    @Override
    public TrackedLegOfCompetitor getTrackedLeg(Competitor competitor, TimePoint at) {
        NavigableSet<MarkPassing> roundings = markPassingsForCompetitor.get(competitor);
        TrackedLegOfCompetitor result = null;
        if (roundings != null) {
            MarkPassing lastBeforeOrAt = roundings.floor(new DummyMarkPassingWithTimePointOnly(at));
            TrackedLeg trackedLeg;
            // already finished the race?
            if (lastBeforeOrAt != null) {
                // and not at or after last mark passing
                if (getRace().getCourse().getLastWaypoint() != lastBeforeOrAt.getWaypoint()) {
                    trackedLeg = getTrackedLegStartingAt(lastBeforeOrAt.getWaypoint());
                } else {
                    // exactly *at* last mark passing?
                    if (at.equals(roundings.last().getTimePoint())) {
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
        return trackedLegs.get(leg);
    }

    @Override
    public TrackedLegOfCompetitor getTrackedLeg(Competitor competitor, Leg leg) {
        return getTrackedLeg(leg).getTrackedLeg(competitor);
    }

    @Override
    public long getUpdateCount() {
        return updateCount;
    }

    @Override
    public int getRankDifference(Competitor competitor, Leg leg, TimePoint timePoint) {
        int previousRank;
        if (leg == getRace().getCourse().getLegs().iterator().next()) {
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
    public synchronized int getRank(Competitor competitor) throws NoWindException {
        return getRank(competitor, MillisecondsTimePoint.now());
    }

    @Override
    public synchronized int getRank(Competitor competitor, TimePoint timePoint) throws NoWindException {
        try {
            synchronized (competitorRankings) {
                List<Competitor> rankedCompetitors = competitorRankings.get(timePoint);
                if (rankedCompetitors == null) {
                    RaceRankComparator comparator = new RaceRankComparator(this, timePoint);
                    rankedCompetitors = new ArrayList<Competitor>();
                    for (Competitor c : getRace().getCompetitors()) {
                        rankedCompetitors.add(c);
                    }
                    Collections.sort(rankedCompetitors, comparator);
                    competitorRankings.put(timePoint, rankedCompetitors);
                }
                return rankedCompetitors.indexOf(competitor) + 1;
            }
        } catch (NoWindError e) {
            throw e.getCause();
        }
    }

    @Override
    public Distance getAverageCrossTrackError(Competitor competitor, TimePoint timePoint) throws NoWindException {
        double distanceInMeters = 0;
        int count = 0;
        GPSFixTrack<Competitor, GPSFixMoving> track = getTrack(competitor);
        GPSFixMoving fix = null;
        for (Leg leg : getRace().getCourse().getLegs()) {
            final TrackedLeg trackedLeg = getTrackedLeg(leg);
            if (trackedLeg.getLegType(timePoint) == LegType.UPWIND) {
                final MarkPassing legStartMarkPassing = getMarkPassing(competitor, leg.getFrom());
                if (legStartMarkPassing != null) {
                    TimePoint legStart = legStartMarkPassing.getTimePoint();
                    final MarkPassing legEndMarkPassing = getMarkPassing(competitor, leg.getTo());
                    Iterator<GPSFixMoving> fixIter = track.getFixesIterator(legStart, /* inclusive */true);
                    while (fixIter.hasNext()
                            && (fix == null || ((legEndMarkPassing == null || fix.getTimePoint().compareTo(
                                    legEndMarkPassing.getTimePoint()) < 0)
                                    && fix.getTimePoint().compareTo(timePoint) < 0))) {
                        fix = fixIter.next();
                        if (fix.getTimePoint().compareTo(timePoint) < 0) {
                            Distance xte = trackedLeg.getCrossTrackError(fix.getPosition(), fix.getTimePoint());
                            distanceInMeters += xte.getMeters();
                            count++;
                        }
                    }
                }
            }
            if (fix != null && fix.getTimePoint().compareTo(timePoint) >= 0) {
                break;
            }
        }
        return new MeterDistance(distanceInMeters / count);
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
            for (MarkPassing markPassing : markPassings) {
                if (markPassing.getWaypoint() == waypoint) {
                    return markPassing;
                }
            }
        }
        return null;
    }

    @Override
    public GPSFixTrack<Buoy, GPSFix> getOrCreateTrack(Buoy buoy) {
        synchronized (buoyTracks) {
            GPSFixTrack<Buoy, GPSFix> result = buoyTracks.get(buoy);
            if (result == null) {
                result = createBuoyTrack(buoy);
                buoyTracks.put(buoy, result);
            }
            return result;
        }
    }

    protected DynamicGPSFixTrackImpl<Buoy> createBuoyTrack(Buoy buoy) {
        return new DynamicGPSFixTrackImpl<Buoy>(buoy, millisecondsOverWhichToAverageSpeed);
    }

    @Override
    public Position getApproximatePosition(Waypoint waypoint, TimePoint timePoint) {
        Position result = null;
        for (Buoy buoy : waypoint.getBuoys()) {
            Position nextPos = getOrCreateTrack(buoy).getEstimatedPosition(timePoint, /* extrapolate */false);
            if (result == null) {
                result = nextPos;
            } else {
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
     * through the {@link #windStore} using {@link #createWindTrack(WindSource)} and added to {@link #windTracks} before
     * being returned.
     */
    @Override
    public WindTrack getOrCreateWindTrack(WindSource windSource) {
        WindTrack result;
        if (windSource.getType() == WindSourceType.COMBINED) {
            result = new CombinedWindTrackImpl(this, WindSourceType.COMBINED.getBaseConfidence());
        } else {
            synchronized (windTracks) {
                result = windTracks.get(windSource);
                if (result == null) {
                    result = createWindTrack(windSource);
                    windTracks.put(windSource, result);
                }
            }
        }
        return result;
    }

    /**
     * Creates a wind track for the <code>windSource</code> specified and stores it in {@link #windTracks}. The
     * averaging interval is set according to the averaging interval set for all other wind sources, or the default if
     * no other wind source exists yet.
     */
    protected WindTrack createWindTrack(WindSource windSource) {
        return windStore.getWindTrack(trackedEvent, this, windSource, millisecondsOverWhichToAverageWind,
                getMillisecondsOverWhichToAverageWind() / 2);
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
        return Collections.unmodifiableCollection(windSourcesToExclude);
    }

    @Override
    public void setWindSourcesToExclude(Iterable<WindSource> windSourcesToExclude) {
        this.windSourcesToExclude.clear();
        for (WindSource windSourceToExclude : windSourcesToExclude) {
            this.windSourcesToExclude.add(windSourceToExclude);
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
            // TODO consider parallelizing
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
    public Wind getDirectionFromStartToNextMark(TimePoint at) {
        Wind result;
        synchronized (directionFromStartToNextMarkCache) {
            result = directionFromStartToNextMarkCache.get(at);
            if (result == null) {
                Leg firstLeg = getRace().getCourse().getLegs().iterator().next();
                Position firstLegEnd = getApproximatePosition(firstLeg.getTo(), at);
                Position firstLegStart = getApproximatePosition(firstLeg.getFrom(), at);
                if (firstLegStart != null && firstLegEnd != null) {
                    result = new WindImpl(firstLegStart, at, new KnotSpeedWithBearingImpl(1.0,
                            firstLegEnd.getBearingGreatCircle(firstLegStart)));
                    final Wind finalResult = result;
                    directionFromStartToNextMarkCache.put(at, finalResult);
                } else {
                    result = null;
                }
            }
        }
        return result;
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
    protected synchronized void updated(TimePoint timeOfEvent) {
        updateCount++;
        clearAllCaches();
        if (timeOfEvent != null) {
            if (timePointOfNewestEvent == null || timePointOfNewestEvent.compareTo(timeOfEvent) < 0) {
                timePointOfNewestEvent = timeOfEvent;
            }
            if (timePointOfOldestEvent == null || timePointOfOldestEvent.compareTo(timeOfEvent) > 0) {
                timePointOfOldestEvent = timeOfEvent;
            }
            timePointOfLastEvent = timeOfEvent;
        }
        notifyAll();
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
    private synchronized void clearAllCaches() {
        if (cacheInvalidationTimer == null) {
            cacheInvalidationTimer = new Timer("Cache invalidation timer for TrackedRaceImpl " + getRace().getName());
            cacheInvalidationTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (TrackedRaceImpl.this) {
                        cacheInvalidationTimer = null;
                        synchronized (competitorRankings) {
                            competitorRankings.clear();
                        }
                        synchronized (maneuverCache) {
                            maneuverCache.clear();
                        }
                    }
                }
            }, DELAY_FOR_CACHE_CLEARING_IN_MILLISECONDS);
        }
    }

    @Override
    public synchronized void waitForNextUpdate(int sinceUpdate) throws InterruptedException {
        while (updateCount <= sinceUpdate) {
            wait(); // ...until updated(...) notifies us
        }
    }

    @Override
    public synchronized void waypointAdded(int zeroBasedIndex, Waypoint waypointThatGotAdded) {
        updateStartToNextMarkCacheInvalidationCacheListenersAfterWaypointAdded(zeroBasedIndex, waypointThatGotAdded);
        markPassingsForWaypoint.put(waypointThatGotAdded, new ConcurrentSkipListSet<MarkPassing>(
                MarkPassingByTimeComparator.INSTANCE));
        for (Buoy buoy : waypointThatGotAdded.getBuoys()) {
            getOrCreateTrack(buoy);
        }
        // a waypoint got added; this means that a leg got added as well; but we shouldn't claim we know where
        // in the leg list of the course the leg was added; that's an implementation secret of CourseImpl. So try:
        LinkedHashMap<Leg, TrackedLeg> reorderedTrackedLegs = new LinkedHashMap<Leg, TrackedLeg>();
        for (Leg leg : getRace().getCourse().getLegs()) {
            if (!trackedLegs.containsKey(leg)) {
                // no tracked leg for leg yet:
                TrackedLeg newTrackedLeg = createTrackedLeg(leg);
                reorderedTrackedLegs.put(leg, newTrackedLeg);
            } else {
                reorderedTrackedLegs.put(leg, trackedLegs.get(leg));
            }
        }
        // now ensure that the iteration order is in sync with the leg iteration order
        trackedLegs.clear();
        for (Map.Entry<Leg, TrackedLeg> entry : reorderedTrackedLegs.entrySet()) {
            trackedLegs.put(entry.getKey(), entry.getValue());
        }
        updated(/* time point */null);
    }

    private void updateStartToNextMarkCacheInvalidationCacheListenersAfterWaypointAdded(int zeroBasedIndex,
            Waypoint waypointThatGotAdded) {
        if (zeroBasedIndex < 2) {
            // the observing listener on any previous buoy will be GCed; we need to ensure
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
        for (Buoy buoy : waypoint.getBuoys()) {
            addStartToNextMarkCacheInvalidationListener(buoy);
        }
    }

    private void addStartToNextMarkCacheInvalidationListener(Buoy buoy) {
        GPSFixTrack<Buoy, GPSFix> track = getOrCreateTrack(buoy);
        StartToNextMarkCacheInvalidationListener listener = new StartToNextMarkCacheInvalidationListener(track);
        startToNextMarkCacheInvalidationListeners.put(buoy, listener);
        track.addListener(listener);
    }

    private void stopAndRemoveStartToNextMarkCacheInvalidationListener(Waypoint waypoint) {
        for (Buoy buoy : waypoint.getBuoys()) {
            stopAndRemoveStartToNextMarkCacheInvalidationListener(buoy);
        }
    }

    private void stopAndRemoveStartToNextMarkCacheInvalidationListener(Buoy buoy) {
        StartToNextMarkCacheInvalidationListener listener = startToNextMarkCacheInvalidationListeners.get(buoy);
        if (listener != null) {
            listener.stopListening();
            startToNextMarkCacheInvalidationListeners.remove(buoy);
        }
    }

    @Override
    public synchronized void waypointRemoved(int zeroBasedIndex, Waypoint waypointThatGotRemoved) {
        updateStartToNextMarkCacheInvalidationCacheListenersAfterWaypointRemoved(zeroBasedIndex, waypointThatGotRemoved);
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
            trackedLegs.remove(toRemove);
            updated(/* time point */null);
        }
    }

    private void updateStartToNextMarkCacheInvalidationCacheListenersAfterWaypointRemoved(int zeroBasedIndex,
            Waypoint waypointThatGotRemoved) {
        if (zeroBasedIndex < 2) {
            // the observing listener on any previous buoy will be GCed; we need to ensure
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
    public TrackedEvent getTrackedEvent() {
        return trackedEvent;
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
                double downwindConfidence = Math.min(average0.getConfidence(), average1.getConfidence());
                downwindNumberOfRelevantBoats = Math.min(bearingClustersDownwind[0].size(),
                        bearingClustersDownwind[1].size());
                confidence = Math.min(confidence, downwindConfidence)
                        * getRace().getBoatClass().getDownwindWindEstimationConfidence(downwindNumberOfRelevantBoats);
                downwindAverage = new BearingWithConfidenceImpl<TimePoint>(average0.getObject().middle(
                        average1.getObject()), downwindConfidence, timePoint);
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
        for (Competitor competitor : getRace().getCompetitors()) {
            TrackedLegOfCompetitor leg = getTrackedLeg(competitor, timePoint);
            if (leg != null) {
                TrackedLeg trackedLeg = getTrackedLeg(leg.getLeg());
                LegType legType;
                try {
                    legType = trackedLeg.getLegType(timePoint);
                    if (legType != LegType.REACHING) {
                        GPSFixTrack<Competitor, GPSFixMoving> track = getTrack(competitor);
                        if (!track.hasDirectionChange(timePoint, getManeuverDegreeAngleThreshold())) {
                            SpeedWithBearingWithConfidence<TimePoint> estimatedSpeedWithConfidence = track
                                    .getEstimatedSpeed(timePoint, weigher);
                            if (estimatedSpeedWithConfidence != null
                                    && estimatedSpeedWithConfidence.getObject() != null &&
                                    // Mark passings may be missing or far off. This can lead to boats apparently going
                                    // "backwards" regarding the leg's direction; ignore those
                                    isNavigatingForward(estimatedSpeedWithConfidence.getObject().getBearing(),
                                            trackedLeg, timePoint)) {
                                // additionally to generally excluding maneuvers, reduce confidence around mark
                                // passings:
                                NavigableSet<MarkPassing> markPassings = getMarkPassings(competitor);
                                double markPassingProximityConfidenceReduction = 1.0;
                                synchronized (markPassings) {
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
                    logger.warning("Unable to determine leg type for race " + getRace().getName()
                            + " while trying to estimate wind");
                    bearings = null;
                }
            }
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
     * In this case, the boat's sails will be on the starboard side, so the result has to be {@link Tack#STARBOARD}. The
     * angle between the boat's heading (which we can only approximate by the boat's bearing) and the wind bearing in
     * this case is 90 degrees. <code>wind.{@link Bearing#getDifferenceTo(Bearing) getDifferenceTo}(boat)</code> in this
     * case will return a bearing representing -90 degrees.
     * <p>
     * 
     * If the wind is blowing the other way, the angle returned by {@link Bearing#getDifferenceTo(Bearing)} will
     * correspond to +90 degrees. In other words, a negative angle means starboard tack, a positive angle represents
     * port tack.
     * <p>
     * 
     * For the unlikely case of 0 degrees difference, {@link Tack#STARBOARD} will result.
     */
    @Override
    public Tack getTack(Competitor competitor, TimePoint timePoint) {
        return getTack(getTrack(competitor).getEstimatedPosition(timePoint, /* extrapolate */false), timePoint,
                getTrack(competitor).getEstimatedSpeed(timePoint).getBearing());
    }

    /**
     * Based on the wind direction at <code>timePoint</code> and at position <code>where</code>, compares the
     * <code>boatBearing</code> to the wind's bearing at that time and place and determined the tack.
     */
    private Tack getTack(Position where, TimePoint timePoint, Bearing boatBearing) {
        Bearing wind = getWind(where, timePoint).getBearing();
        Bearing difference = wind.getDifferenceTo(boatBearing);
        return difference.getDegrees() <= 0 ? Tack.STARBOARD : Tack.PORT;
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

    /**
     * Caches results in {@link #maneuverCache}. The cache is {@link #clearAllCaches() invalidated} by any
     * {@link #updated(TimePoint) update}. Therefore, it is mainly useful for completed races. The cache tries to grow
     * the time interval for which the maneuvers of a competitor have been computed. If <code>from</code> and
     * <code>to</code> are within an interval already cached, the interval requested is
     * {@link #extractInterval(TimePoint, TimePoint, List) extracted} from the maneuver list cached. Otherwise, the
     * cached interval (empty in case no maneuvers were cached for <code>competitor</code> yet) is extended to include
     * <code>from..to</code> by computing and caching the maneuvers for the new, extended interval. From the resulting
     * extended maneuver list the interval requested is then {@link #extractInterval(TimePoint, TimePoint, List)
     * extracted}.
     */
    @Override
    public List<Maneuver> getManeuvers(Competitor competitor, TimePoint from, TimePoint to) throws NoWindException {
        List<Maneuver> result;
        Triple<TimePoint, TimePoint, List<Maneuver>> fromToAndManeuvers;
        synchronized (maneuverCache) {
            fromToAndManeuvers = maneuverCache.get(competitor);
        }
        if (fromToAndManeuvers != null && from.compareTo(fromToAndManeuvers.getA()) >= 0
                && to.compareTo(fromToAndManeuvers.getB()) <= 0) {
            // cached maneuver list contains interval requested
            result = extractInterval(from, to, fromToAndManeuvers.getC());
        } else {
            TimePoint extendedFrom = fromToAndManeuvers == null ? from
                    : from.compareTo(fromToAndManeuvers.getA()) <= 0 ? from : fromToAndManeuvers.getA();
            TimePoint extendedTo = fromToAndManeuvers == null ? to : to.compareTo(fromToAndManeuvers.getB()) >= 0 ? to
                    : fromToAndManeuvers.getB();
            List<Maneuver> extendedResultForCache = detectManeuvers(
                    competitor,
                    approximate(competitor, getRace().getBoatClass().getMaximumDistanceForCourseApproximation(),
                            extendedFrom, extendedTo));
            result = extractInterval(from, to, extendedResultForCache);
            synchronized (maneuverCache) {
                maneuverCache.put(competitor, new Triple<TimePoint, TimePoint, List<Maneuver>>(extendedFrom,
                        extendedTo, extendedResultForCache));
            }
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
            Distance twoHullLengths = competitor.getBoat().getBoatClass().getHullLength().scale(2);
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
                        && currentFixAndCourseChange.getA().getTimePoint().asMillis()
                                - group.get(group.size() - 1).getA().getTimePoint().asMillis() > getApproximateManeuverDurationInMilliseconds()
                        && currentFixAndCourseChange.getA().getPosition()
                                .getDistance(group.get(group.size() - 1).getA().getPosition())
                                .compareTo(twoHullLengths) > 0) {
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
        TimePoint maneuverTimePoint = new MillisecondsTimePoint(totalMilliseconds / group.size());
        Position maneuverPosition = getTrack(competitor)
                .getEstimatedPosition(maneuverTimePoint, /* extrapolate */false);
        MillisecondsTimePoint timePointBeforeManeuver = new MillisecondsTimePoint(group.get(0).getA().getTimePoint()
                .asMillis()
                - getApproximateManeuverDurationInMilliseconds() / 2);
        MillisecondsTimePoint timePointAfterManeuver = new MillisecondsTimePoint(group.get(group.size() - 1).getA()
                .getTimePoint().asMillis()
                + getApproximateManeuverDurationInMilliseconds() / 2);
        Tack tackBeforeManeuver = getTack(maneuverPosition, timePointBeforeManeuver,
                speedWithBearingOnApproximationAtBeginning.getBearing());
        Tack tackAfterManeuver = getTack(maneuverPosition, timePointAfterManeuver,
                speedWithBearingOnApproximationAtEnd.getBearing());
        // the TrackedLegOfCompetitor variables may be null, e.g., in case the time points are before or after the race
        TrackedLegOfCompetitor legBeforeManeuver = getTrackedLeg(competitor, timePointBeforeManeuver);
        TrackedLegOfCompetitor legAfterManeuver = getTrackedLeg(competitor, timePointAfterManeuver);
        ManeuverType maneuverType;
        if (Math.abs(totalCourseChangeInDegrees) > PENALTY_CIRCLE_DEGREES_THRESHOLD) {
            maneuverType = ManeuverType.PENALTY_CIRCLE;
        } else if (legBeforeManeuver != legAfterManeuver
                &&
                // a maneuver at the start line is not to be considered a MARK_PASSING maneuver; show a tack as a tack
                legAfterManeuver != null
                && legAfterManeuver.getLeg().getFrom() != getRace().getCourse().getFirstWaypoint()) {
            maneuverType = ManeuverType.MARK_PASSING;
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
                        break;
                    case DOWNWIND:
                        maneuverType = ManeuverType.JIBE;
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
        Maneuver maneuver = new ManeuverImpl(maneuverType, tackAfterManeuver, maneuverPosition, maneuverTimePoint,
                speedWithBearingOnApproximationAtBeginning, speedWithBearingOnApproximationAtEnd,
                totalCourseChangeInDegrees);
        return maneuver;
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

    private class StartToNextMarkCacheInvalidationListener implements GPSTrackListener<Buoy> {
        private static final long serialVersionUID = 3540278554797445085L;
        private final GPSFixTrack<Buoy, GPSFix> listeningTo;

        public StartToNextMarkCacheInvalidationListener(GPSFixTrack<Buoy, GPSFix> listeningTo) {
            this.listeningTo = listeningTo;
        }

        public void stopListening() {
            listeningTo.removeListener(this);
        }

        @Override
        public void gpsFixReceived(GPSFix fix, Buoy buoy) {
            clearDirectionFromStartToNextMarkCache();
        }

        @Override
        public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
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
        return windTracks.keySet();
    }

}
