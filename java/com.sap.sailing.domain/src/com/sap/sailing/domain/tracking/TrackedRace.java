package com.sap.sailing.domain.tracking;

import java.io.Serializable;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedure;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDefineMarkEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEvent;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TargetTimeInfo;
import com.sap.sailing.domain.common.TimingConstants;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.abstractlog.TimePointSpecificationFoundInLog;
import com.sap.sailing.domain.common.dto.TrackedRaceDTO;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.SensorFix;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingCalculator;
import com.sap.sailing.domain.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.ranking.RankingMetric;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sailing.domain.tracking.impl.NonCachingMarkPositionAtTimePointCache;
import com.sap.sailing.domain.tracking.impl.TrackedRaceImpl;
import com.sap.sailing.domain.windestimation.IncrementalWindEstimation;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.IsManagedByCache;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.shared.HasPermissions;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.WithQualifiedObjectIdentifier;

/**
 * Live tracking data of a single race. The race follows a defined {@link Course} with a sequence of {@link Leg}s. The
 * course may change over time as the race committee decides to change it. Therefore, a {@link TrackedRace} instance
 * {@link Course#addCourseListener(com.sap.sailing.domain.base.CourseListener) observes} the race {@link Course} for
 * such changes. The tracking information of a leg can be requested either for all competitors (see
 * {@link #getTrackedLegs()} and {@link #getTrackedLeg(Leg)}) or for a single competitor (see
 * {@link #getTrackedLeg(Competitor, Leg)}).
 * <p>
 * 
 * The overall race standings can be requested in terms of a competitor's ranking. More detailed information about what
 * happens / happened within a leg is available from {@link TrackedLeg} and {@link TrackedLegOfCompetitor}.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public interface TrackedRace
        extends Serializable, IsManagedByCache<SharedDomainFactory>, WithQualifiedObjectIdentifier {
    final Duration START_TRACKING_THIS_MUCH_BEFORE_RACE_START = Duration.ONE_MINUTE.times(5);
    final Duration STOP_TRACKING_THIS_MUCH_AFTER_RACE_FINISH = Duration.ONE_SECOND.times(30);

    final long MAX_TIME_BETWEEN_START_AND_FIRST_MARK_PASSING_IN_MILLISECONDS = 30000;

    final long DEFAULT_LIVE_DELAY_IN_MILLISECONDS = 5000;

    RaceDefinition getRace();

    RegattaAndRaceIdentifier getRaceIdentifier();
    
    /**
     * Tells how ranks are to be assigned to the competitors at any time during the race. For one-design boat classes
     * this will usually happen by projecting the competitors to the wind direction for upwind and downwind legs or to
     * the leg's rhumb line for reaching legs, then comparing positions. For handicap races using a time-on-time,
     * time-on-distance, combination thereof or a more complicated scheme such as ORC Performance Curve, the ranking
     * process needs to take into account the competitor-specific correction factors defined in the measurement
     * certificate.
     */
    RankingMetric getRankingMetric();

    /**
     * Computes the estimated start time for this race (not to be confused with the {@link #getStartOfTracking()} time
     * point which is expected to be before the race start time). The highest precedence take the
     * {@link #attachedRaceLogs race logs} and their start time events, followed by the field {@link #startTimeReceived}
     * which can explicitly be set using {@link #setStartTimeReceived(TimePoint)}. When there are no {@link MarkPassing}s
     * for the first mark, <code>null</code> is returned. If there are mark passings for the first mark and the start
     * time is less than {@link #MAX_TIME_BETWEEN_START_AND_FIRST_MARK_PASSING_IN_MILLISECONDS} before the first mark
     * passing for the first mark. Otherwise, the first mark passing for the first mark minus
     * {@link #MAX_TIME_BETWEEN_START_AND_FIRST_MARK_PASSING_IN_MILLISECONDS} is returned as the race start time.
     * <p>
     * 
     * If no start time can be determined this way, <code>null</code> is returned.
     */
    TimePoint getStartOfRace();
    
    /**
     * Like {@link #getStartOfRace()}, but ignoring any inference from start mark passings in case {@code inferred} is
     * {@code false}. In this case, if no official start time was set, e.g., in the {@link RaceLog} or explicitly using
     * {@link #getStartTimeReceived()}, {@code null} will be returned by this method even if start mark passings are
     * present.
     */
    TimePoint getStartOfRace(boolean inferred);

    /**
     * @return the time point taken from a valid, non-revoked {@link RaceLogRaceStatusEvent} that transfers the race
     * into status {@link RaceLogRaceStatus#FINISHING} or {@code null} if no such event is found.
     */
    TimePoint getFinishingTime();

    /**
     * @return the time point taken from a valid, non-revoked {@link RaceLogRaceStatusEvent} that transfers the race
     *         into status {@link RaceLogRaceStatus#FINISHED} from any of the {@link RaceLog}s attached to this race,
     *         or {@code null} if no such event is found.
     *         
     * @see {@link RaceChangeListener#finishedTimeChanged(TimePoint, TimePoint)}
     */
    TimePoint getFinishedTime();

    /**
     * Determine the race end time is tricky. Boats may sink, stop, not finish, although they started the race. We
     * therefore cannot wait for all boats to reach the finish line. The following rules are used to calculate the
     * endOfRace:
     * <ol>
     * <li>Returns <code>null</code> if no boat passed the finish line</li>
     * <li>Returns time of the last mark passing recorded for the finish line</li>
     * <li>TODO: Returns the time of the first passing of the finish line + the target window (defined in the
     * competition rules) if a target window has been defined for the race</li>
     */
    TimePoint getEndOfRace();

    /**
     * Returns a list of the first and last mark passing times of all course waypoints. Callers wanting to iterate over
     * the result must <code>synchronize</code> on the result.
     */
    Iterable<Util.Pair<Waypoint, Util.Pair<TimePoint, TimePoint>>> getMarkPassingsTimes();

    /**
     * Shorthand for <code>{@link #getStart()}.{@link TimePoint#compareTo(TimePoint) compareTo(at)} &lt;= 0</code>
     */
    boolean hasStarted(TimePoint at);
   
    /**
     * A race is considered "live" if it
     * {@link TrackedRaceDTO#hasGPSData has GPS data} and {@link TrackedRaceDTO#hasWindData wind data} and if the
     * <code>at</code> time point is between the start and the end of the race.
     * <p>
     * 
     * The pre-start phase of a race is interesting also in live mode. Therefore, if a {@link TrackedRace#getStartOfRace start
     * time} is available for the race, the {@link TimingConstants#PRE_START_PHASE_DURATION_IN_MILLIS} is subtracted
     * from the actual start time so that the pre-start phase also counts as live. If no start time is known for the
     * race, but a {@link TrackedRace#getStartOfTracking start of tracking time} is known, it is used as the start of
     * the "live" interval.
     * <p>
     * 
     * If an {@link TrackedRace#getEndOfRace end time} is already known for the race,
     * {@link TimingConstants#IS_LIVE_GRACE_PERIOD_IN_MILLIS} is added to that and the result is taken to be the end of
     * the "live" period. If no end time is known but a {@link TrackedRace#getTimePointOfNewestEvent} is set, again the
     * {@link TimingConstants#IS_LIVE_GRACE_PERIOD_IN_MILLIS} is added to that to mark the end of the "live" interval.
     * <p>
     * 
     * @param at
     *            the time point at which to determine whether the race for <code>fleet</code>
     *            is/was live. A <code>null</code> value will use the start time of the race.
     * @return
     */
    boolean isLive(TimePoint at);

    /**
     * Clients can safely iterate over the iterable returned because it's a non-live copy of the tracked legs of this
     * tracked race. This implies that should an update to the underlying list of waypoints in this race's
     * {@link Course} take place after this method has returned, then this won't be reflected in the result returned.
     * Callers should obtain the {@link Course#lockForRead() course's read lock} while using the result of this call if
     * they want to ensure that no course update is applied concurrently.
     */
    Iterable<TrackedLeg> getTrackedLegs();

    TrackedLeg getTrackedLeg(Leg leg);

    /**
     * Tracking information about the leg <code>competitor</code> is on at <code>timePoint</code>, or <code>null</code>
     * if the competitor hasn't started any leg yet at <code>timePoint</code> or has already finished the race.
     */
    TrackedLegOfCompetitor getCurrentLeg(Competitor competitor, TimePoint timePoint);

    /**
     * Tells which leg the leader at <code>timePoint</code> is on
     */
    TrackedLeg getCurrentLeg(TimePoint timePoint);

    /**
     * Tells the number of the last started leg at <code>timePoint</code>
     * The leg number is 0 before the start, the number of the current leg during the race
     * and the number of the last leg at the end of the race even if the race has finished. 
     */
    int getLastLegStarted(TimePoint timePoint);
    
    /**
     * Precondition: waypoint must still be part of {@link #getRace()}.{@link RaceDefinition#getCourse() getCourse()}.
     * Returns {@code null} for the first waypoint of the course. If the waypoint is not part of the course, an
     * {@link IllegalArgumentException} will be thrown.
     */
    TrackedLeg getTrackedLegFinishingAt(Waypoint endOfLeg);

    /**
     * Precondition: waypoint must still be part of {@link #getRace()}.{@link RaceDefinition#getCourse() getCourse()}.
     */
    TrackedLeg getTrackedLegStartingAt(Waypoint startOfLeg);

    /**
     * The raw, updating feed of a single competitor participating in this race
     */
    GPSFixTrack<Competitor, GPSFixMoving> getTrack(Competitor competitor);

    /**
     * {@link SensorFixTrack}s provide timed sensor data in addition to GPSFixes that are hold in {@link GPSFixTrack}s.
     * In contrast to {@link GPSFixTrack}s there is a 1:n relation of competitors to tracks by introducing track names.
     * So every type of track has an associated name. With this construct you can have track implementations that
     * provide specific functionality based on the contained fix type.
     * 
     * @param competitor the competitor to get the track for
     * @param trackName the name of the track to get
     * @return the track associated to the given Competitor and name or <code>null</code> if there is none.
     */
    <FixT extends SensorFix, TrackT extends SensorFixTrack<Competitor, FixT>> TrackT getSensorTrack(Competitor competitor, String trackName);
    
    /**
     * Returns all contained {@link SensorFixTrack SensorFixTracks} for the given trackName and associated to any competitor.
     */
    <FixT extends SensorFix, TrackT extends SensorFixTrack<Competitor, FixT>> Iterable<TrackT> getSensorTracks(String trackName);

    /**
     * Tells the leg on which the <code>competitor</code> was at time <code>at</code>. If the competitor hasn't passed
     * the start waypoint yet, <code>null</code> is returned because the competitor was not yet on any leg at that point
     * in time. If the time point happens to be after the last fix received from that competitor, the last known leg for
     * that competitor is returned. If the time point is after the competitor's mark passing for the finish line,
     * <code>null</code> is returned. For all legs except the last, if the time point equals a mark passing time point
     * of the leg's starting waypoint, that leg is returned. For the time point of the mark passing for the finish line,
     * the last leg is returned.
     */
    TrackedLegOfCompetitor getTrackedLeg(Competitor competitor, TimePoint at);

    TrackedLegOfCompetitor getTrackedLeg(Competitor competitor, Leg leg);

    /**
     * @return a sequential number counting the updates that occurred to this tracked race. Callers may use this to ask
     *         for updates newer than such a sequence number.
     */
    long getUpdateCount();

    int getRankDifference(Competitor competitor, Leg leg, TimePoint timePoint);

    /**
     * Computes the rank of the competitor in this race for the current time.
     */
    int getRank(Competitor competitor) throws NoWindException;

    /**
     * Computes the rank of <code>competitor</code> in this race. A competitor is ahead of all competitors that are one
     * or more legs behind. Within the same leg, the rank is determined by the windward distance to go and therefore
     * depends on the assumptions of the wind direction for the given <code>timePoint</code>. If the race hasn't
     * {@link #hasStarted(TimePoint) started} yet, the result is undefined.
     * 
     * @return <code>0</code> in case the competitor hasn't participated in the race; a rank starting with
     *         <code>1</code> where rank <code>1</code> identifies the leader otherwise
     */
    default int getRank(Competitor competitor, TimePoint timePoint) {
        return getRank(competitor, timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    int getRank(Competitor competitor, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * For the given waypoint lists the {@link MarkPassing} events that describe which competitor passed the waypoint at
     * which point in time. This can, e.g., be used to sort those competitors who already finished a leg within the leg
     * that ends with <code>waypoint</code>. The remaining competitors need to be ordered by the advantage line-related
     * distance to the waypoint.
     * 
     * @return the iterable sequence of {@link MarkPassing}s as described above. If the caller wants to iterate on the
     *         resulting collection, the caller needs to invoke {@link #lockForRead(Iterable)} with the collection
     *         returned as parameter because insertions into the competitor's mark passing collection will obtain the
     *         corresponding write lock.
     */
    Iterable<MarkPassing> getMarkPassingsInOrder(Waypoint waypoint);

    /**
     * Obtains the {@link MarkPassing} for <code>competitor</code> passing <code>waypoint</code>. If no such mark
     * passing has been reported (yet), <code>null</code> is returned.
     */
    MarkPassing getMarkPassing(Competitor competitor, Waypoint waypoint);

    /**
     * Yields the track describing <code>mark</code>'s movement over time; never <code>null</code> because a new track
     * will be created in case no track was present for <code>mark</code> so far.
     * 
     * @see #getTrack(Mark)
     */
    GPSFixTrack<Mark, GPSFix> getOrCreateTrack(Mark mark);
    
    /**
     * Yields the track describing <code>mark</code>'s movement over time; <code>null</code> if no track exists for
     * <code>mark</code> so far.
     * 
     * @see #getOrCreateTrack(Mark)
     */
    GPSFixTrack<Mark, GPSFix> getTrack(Mark mark);

    /**
     * Retrieves all marks assigned to the race. They are not necessarily part of the race course.
     */
    Iterable<Mark> getMarks();

    /**
     * Retrieves all course side lines assigned to the race.
     */
    Iterable<Sideline> getCourseSidelines();

    /**
     * If the <code>waypoint</code> only has one {@link #getMarks() mark}, its position at time <code>timePoint</code>
     * is returned. Otherwise, the center of gravity between the mark positions is computed and returned.
     */
    default Position getApproximatePosition(Waypoint waypoint, TimePoint timePoint) {
        return getApproximatePosition(waypoint, timePoint, new NonCachingMarkPositionAtTimePointCache(this, timePoint));
    }
    
    /**
     * Same as {@link #getApproximatePosition(Waypoint, TimePoint)}, but giving the caller the possibility to pass a
     * cache of mark positions and related information that can help speed up compound operations requiring frequent
     * access to the same marks in the same race for the same time point.
     * 
     * @param markPositionCache
     *            a cache for this {@link MarkPositionAtTimePointCache#getTrackedRace() race} and the
     *            {@link MarkPositionAtTimePointCache#getTimePoint() timePoint} passed
     */
    Position getApproximatePosition(Waypoint waypoint, TimePoint timePoint,
            MarkPositionAtTimePointCache markPositionCache);

    /**
     * Checks whether the {@link Wind#getTimePoint()} is in range of start and end {@link TimePoint}s plus extra time
     * for wind recording. If, based on a {@link RaceExecutionOrderProvider}, there is no previous race that takes the
     * wind fix, an extended time range lead (see
     * {@link TrackedRaceImpl#EXTRA_LONG_TIME_BEFORE_START_TO_TRACK_WIND_MILLIS}) is used to record wind even a long
     * time before the race start.
     * <p>
     * 
     * A race does not record wind when both, {@link #getStartOfTracking()} and {@link #getStartOfRace()} are
     * <code>null</code>. Wind is not recorded when it is after the later of {@link #getEndOfRace()} and
     * {@link #getEndOfTracking()} and one of the two is not <code>null</code>.
     * <p>
     * 
     * This default implementation returns true which may be useful for tests and mocked implementations; however, real
     * implementations shall override this and provide a meaningful implementation according to the specification given
     * above.
     */
    default boolean takesWindFixWithTimePoint(TimePoint timePoint) {
        return true;
    }

    default boolean takesWindFixWithTimePointRecursively(TimePoint windFixTimePoint, Set<TrackedRace> visited) {
        return true;
    }

    /**
     * Same as {@link #getWind(Position, TimePoint, Set) getWind(p, at, Collections.emptyList())}
     */
    Wind getWind(Position p, TimePoint at);

    /**
     * Obtains estimated interpolated wind information for a given position and time point. The information is taken
     * from all wind sources available except for those listed in <code>windSourcesToExclude</code>, using the
     * confidences of the wind values provided by the various sources during averaging.
     */
    Wind getWind(Position p, TimePoint at, Set<WindSource> windSourcesToExclude);

    /**
     * Retrieves the wind sources used so far by this race that have the specified <code>type</code> as their
     * {@link WindSource#getType() type}. Always returns a non-<code>null</code> iterable which may be empty in case the
     * race does not use any wind source of the specified type (yet).<p>
     * 
     * It is possible to ask for the {@link WindSourceType#COMBINED} and {@link WindSourceType#LEG_MIDDLE} types and
     * get a non-empty result although those sources are never returned by {@link #getWindSources()}.
     */
    Set<WindSource> getWindSources(WindSourceType type);

    /**
     * Retrieves all wind sources known to this race, including those {@link #getWindSourcesToExclude() to exclude}.
     * Callers can freely iterate because a copied collection is returned. The {@link WindSourceType#COMBINED} wind source
     * as well as the {@link WindSourceType#LEG_MIDDLE} sources are never part of the result.
     */
    Set<WindSource> getWindSources();

    /**
     * Same as {@link #getOrCreateWindTrack(WindSource, long) getOrCreateWindTrack(windSource,
     * getMillisecondsOverWhichToAverageWind())}.
     */
    WindTrack getOrCreateWindTrack(WindSource windSource);

    WindTrack getOrCreateWindTrack(WindSource windSource, long delayForWindEstimationCacheInvalidation);

    /**
     * Waits until {@link #getUpdateCount()} is after <code>sinceUpdate</code>.
     */
    void waitForNextUpdate(int sinceUpdate) throws InterruptedException;

    /**
     * Time stamp of the start of the actual tracking. The value can be null (e.g. if we have not received any signal
     * from the tracking infrastructure)
     */
    TimePoint getStartOfTracking();

    /**
     * Time stamp of the end of the actual tracking. The value can be null (e.g. if we have not received any signal from
     * the tracking infrastructure)
     */
    TimePoint getEndOfTracking();

    /**
     * Regardless of the order in which events were received, this method returns the latest time point contained by any
     * of the events received and processed.
     */
    TimePoint getTimePointOfNewestEvent();

    /**
     * Regardless of the order in which events were received, this method returns the oldest time point contained by any
     * of the events received and processed.
     */
    TimePoint getTimePointOfOldestEvent();

    /**
     * @return the mark passings for <code>competitor</code> in this race received so far; the mark passing objects are
     *         returned such that their {@link MarkPassing#getWaypoint() waypoints} are ordered in the same way they are
     *         ordered in the race's {@link Course}. Note, that this doesn't necessarily guarantee ascending time
     *         points, particularly if premature mark passings have been detected accidentally as can be the case with
     *         some tracking providers such as TracTrac. If the caller wants to iterate on the resulting collection or
     *         construct a {@link SortedSet#headSet(Object)} or {@link SortedSet#tailSet(Object)} and then iterate over
     *         that, the caller needs to invoke {@link #lockForRead(Iterable)} with the collection returned as parameter
     *         because insertions into the competitor's mark passing collection will obtain the corresponding write
     *         lock.
     */
    NavigableSet<MarkPassing> getMarkPassings(Competitor competitor);
    
    /**
     * Returns competitor's mark passings.
     * 
     * @param waitForLatestUpdates
     *            if any mark passing updates are pending because some calculations are currently going on and updates
     *            haven't all been processed yet then the call will block until these updates have been processed in
     *            case this parameter is set to {@code true}. For this the method uses a lock on the
     *            {@link MarkPassingCalculator }to block the thread until all calculations will be finished.
     */
    NavigableSet<MarkPassing> getMarkPassings(Competitor competitor, boolean waitForLatestUpdates);

    /**
     * This obtains the course's read lock before asking for the read lock for the <code>markPassings</code> structure.
     * See also bug 1370 (http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=1370). This is necessary because the
     * code that executes a course update will first ask the course's write lock and then relay execution to the
     * course change listeners among which there is a {@link TrackedRace} which will then update the mark passings
     * for all competitors and therefore will need to ask the write lock for those. If the thread calling this method
     * first obtains the mark passings read lock and later, while holding on to that lock, asks for the course's read
     * lock, a deadlock may result.<p>
     * 
     * Furthermore, when trying to acquire both, a lock for the {@link #getMarkPassings(Competitor) mark passings for a competitor}
     * and a lock for the {@link #getMarkPassingsInOrder(Waypoint) mark passings for a waypoint, this needs to happen in exactly
     * this order, or a deadlock may result.<p>
     * 
     * The {@link #unlockAfterRead(Iterable)} method will symmetrically unlock the course's read lock after releasing the
     * read lock for the mark passings.
     */
    void lockForRead(Iterable<MarkPassing> markPassings);

    /**
     * Releases the read lock for the mark passings and then the read lock for the course.
     * 
     * @see #lockForRead(Iterable)
     */
    void unlockAfterRead(Iterable<MarkPassing> markPassings);

    /**
     * Time stamp that the event received last from the underlying push service carried on it. Note that these times may
     * not increase monotonically.
     */
    TimePoint getTimePointOfLastEvent();

    long getMillisecondsOverWhichToAverageSpeed();

    long getMillisecondsOverWhichToAverageWind();

    /**
     * Gets the current delay of incoming events to the real time of the events in milliseconds
     */
    long getDelayToLiveInMillis();

    /**
     * Estimates the wind direction based on the observed boat courses at the time given for the position provided. The
     * estimate is based on the assumption that the boats which are on an upwind or a downwind leg sail with very
     * similar angles on the starboard and the port side. There should be clusters of courses which are close to each
     * other (within a threshold of, say, +/- 5 degrees), whereas for the upwind group there should be two clusters with
     * angles about 90 degrees apart; similarly, for the downwind leg there should be two clusters, only that the
     * general jibing angle may vary more, based on the wind speed and the boat class.
     * <p>
     * 
     * Boats {@link GPSFixTrack#hasDirectionChange(TimePoint, double) currently maneuvering} are not considered for this
     * analysis.
     * <p>
     * 
     * This wind direction should not be used directly to compute the leg's wind direction and hence the {@link LegType
     * leg type} because an endless recursion may result: an implementation of this method signature will need to know
     * whether a leg is an upwind or downwind leg for which it has to know where the wind is coming from.
     * 
     * @return <code>null</code> if no sufficient boat track information is available or leg type identification (upwind
     *         vs. downwind) is not possible; a valid {@link Wind} fix otherwise whose bearing is inferred from the boat
     *         courses and whose speed in knots is currently a rough indication of how many boats' courses contributed
     *         to determining the bearing. If in the future we have data about polar diagrams specific to boat classes,
     *         we may be able to also infer the wind speed from the boat tracks.
     */
    Wind getEstimatedWindDirection(TimePoint timePoint);

    /**
     * Determines whether the <code>competitor</code> is sailing on port or starboard tack at the <code>timePoint</code>
     * requested. Note that this will have to retrieve information about the wind. This, in turn, can lead to the
     * current thread obtaining the monitor of the various wind tracks, and, if the
     * {@link WindSource#TRACK_BASED_ESTIMATION} source is used, also the monitors of the competitors' GPS tracks.
     */
    Tack getTack(Competitor competitor, TimePoint timePoint) throws NoWindException;
    
    /**
     * Based on the wind direction at <code>timePoint</code> and at position <code>where</code>, compares the
     * <code>boatBearing</code> to the wind's bearing at that time and place and determined the tack.
     * 
     * @throws NoWindException
     *             in case the wind cannot be determined because without a wind direction, the tack cannot be determined
     *             either
     */
    Tack getTack(Position where, TimePoint timePoint, Bearing boatBearing) throws NoWindException;

    /**
     * Determines whether the <code>competitor</code> is sailing on port or starboard tack at the <code>timePoint</code>
     * requested.
     * <p>
     * This method outperforms {@link #getTack(Competitor, TimePoint)}, based on being passed an already calculated wind
     * for the given time and competitor position as well as the competitors speed and course over ground.
     * <p>
     * This method will acquire the read lock for the competitor's track.
     */
    Tack getTack(SpeedWithBearing speedWithBearing, Wind wind, TimePoint timePoint);

    TrackedRegatta getTrackedRegatta();

    /**
     * Computes a default wind direction based on the direction of the first leg at time <code>at</code>, with a default
     * speed of zero knots. Note that this wind direction can only be used if {@link #raceIsKnownToStartUpwind()} returns
     * <code>true</code>.
     * 
     * @param at
     *            usually the {@link #getStart() start time} should be used; if no valid start time is provided, the
     *            current time point may serve as a default
     * @return <code>null</code> in case the first leg's direction cannot be determined, e.g., because the necessary
     *         mark positions are not known (yet)
     */
    Wind getDirectionFromStartToNextMark(TimePoint at);

    /**
     * Traverses the competitor's {@link GPSFixTrack track} between {@code from} and {@code to} (both inclusive) and
     * returns those fixes where significant changes in the course over ground (COG) are observed, indicating a possibly
     * relevant maneuver. The {@link SpeedWithBearing#getBearing() COG} change is calculated over a time window the size
     * of the typical maneuver duration, but at least covering two fixes in order to also cover the case of low sampling
     * rates. If in any such window the COG change exceeds the threshold, the window is extended as far as the COG
     * change grows, then from the extended window the fix with the highest COG change to its successor is returned. The
     * next window analysis will start after the end of the current window, avoiding duplicates in the result.
     * <p>
     * 
     * If the precondition that the {@code competitor} must be {@link RaceDefinition#getCompetitors() part of} the
     * {@link #getRace() race} isn't met, a {@code NullPointerException} will result.
     */
    Iterable<GPSFixMoving> approximate(Competitor competitor, Distance maxDistance, TimePoint from, TimePoint to);

    /**
     * @return a non-<code>null</code> but perhaps empty list of the maneuvers that <code>competitor</code> performed in
     *         this race between <code>from</code> and <code>to</code>. Depending on <code>waitForLatest</code> the
     *         result is taken from the cache straight away (<code>waitForLatest==false</code>) or, if a re-calculation
     *         for the <code>key</code> is still ongoing, the result of that ongoing re-calculation is returned.
     */
    Iterable<Maneuver> getManeuvers(Competitor competitor, TimePoint from, TimePoint to, boolean waitForLatest);
    
    /**
     * @return a non-<code>null</code> but perhaps empty list of the maneuvers that <code>competitor</code> performed in
     *         this race. Depending on <code>waitForLatest</code> the result is taken from the cache straight away
     *         (<code>waitForLatest==false</code>) or, if a re-calculation for the <code>key</code> is still ongoing,
     *         the result of that ongoing re-calculation is returned.
     */
    Iterable<Maneuver> getManeuvers(Competitor competitor, boolean waitForLatest);

    /**
     * @return <code>true</code> if this race is known to start with an {@link LegType#UPWIND upwind} leg. If this is
     *         the case, the wind estimation may default to using the first leg's direction at race start time as the
     *         direction the wind comes from.
     */
    boolean raceIsKnownToStartUpwind();

    /**
     * Many calculations require valid wind data. In order to prevent NoWindException's to be handled by those
     * calculation this method can be used to check whether the tracked race has sufficient wind information available.
     * 
     * @return <code>true</code> if {@link #getWind(Position, TimePoint)} delivers a (not null) wind fix.
     */
    boolean hasWindData();

    /**
     * 
     * @return <code>true</code> if at least one GPS fix for one of the competitors is available for this race.
     */
    boolean hasGPSData();

    /**
     * Adds a race change listener to the set of listeners that will be notified about changes to this race. The
     * listener won't be serialized together with this object.
     */
    void addListener(RaceChangeListener listener);

    /**
     * Like {@link #addListener(RaceChangeListener)}, but notifies the listener about the wind fixes known so far by the
     * tracked race. This runs synchronized with the otherwise asynchronous loading of wind tracks, triggered by the
     * constructor of the {@link TrackedRace} implementation classes. This procedure guarantees that eventually the
     * listener will have received a notification for all wind fixes, regardless of whether they were already loaded at
     * the time the listener is registered or they are loaded after the registration has completed.<p>
     * 
     * The same is true for the GPS fixes for marks and competitors.
     */
    void addListener(RaceChangeListener listener, boolean notifyAboutWindFixesAlreadyLoaded,
            boolean notifyAboutGPSFixesAlreadyLoaded);

    void removeListener(RaceChangeListener listener);
    
    /**
     * @return <code>null</code> if there are no mark passings for the <code>competitor</code> in this race
     * or if the competitor has not finished one of the legs in the race.
     */
    Distance getDistanceTraveled(Competitor competitor, TimePoint timePoint);

    /**
     * See {@link TrackedLegOfCompetitor#getDistanceTraveledConsideringGateStart(TimePoint)}
     */
    Distance getDistanceTraveledIncludingGateStart(Competitor competitor, TimePoint timePoint);

    /**
     * @return <code>null</code> if there are no mark passings for the <code>competitor</code> in this race
     * or if the competitor has not finished one of the legs in the race.
     */
    Distance getDistanceFoiled(Competitor competitor, TimePoint timePoint);

    /**
     * @return <code>null</code> if there are no mark passings for the <code>competitor</code> in this race
     * or if the competitor has not finished one of the legs in the race.
     */
    Duration getDurationFoiled(Competitor competitor, TimePoint timePoint);

    /**
     * See {@link TrackedLegOfCompetitor#getWindwardDistanceToCompetitorFarthestAhead(TimePoint, WindPositionMode, RankingInfo)}
     */
    Distance getWindwardDistanceToCompetitorFarthestAhead(Competitor competitor, TimePoint timePoint, WindPositionMode windPositionMode);

    /**
     * Same as {@link #getWindwardDistanceToCompetitorFarthestAhead(Competitor, TimePoint, WindPositionMode)}, only with an
     * additional cache to speed up wind and leg type and leg bearing calculations in case of multiple similar look-ups
     * for the same time point.
     * 
     * @param rankingInfo
     *            materialized ranking information that is expensive to calculate, avoiding redundant calculations
     */
    Distance getWindwardDistanceToCompetitorFarthestAhead(Competitor competitor, TimePoint timePoint,
            WindPositionMode windPositionMode, RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * Calls {@link #getWindWithConfidence(Position, TimePoint, Iterable)} and excludes those wind sources listed in
     * {@link #getWindSourcesToExclude}.
     */
    WindWithConfidence<Util.Pair<Position, TimePoint>> getWindWithConfidence(Position p, TimePoint at);

    /**
     * Lists those wind sources which by default are not considered in {@link #getWind(Position, TimePoint)} and
     * {@link #getWindWithConfidence(Position, TimePoint)}.
     */
    Set<WindSource> getWindSourcesToExclude();

    /**
     * Loops over this tracked race's wind sources and from each asks its averaged wind for the position <code>p</code>
     * and time point <code>at</code>, using the particular wind source's averaging interval. The confidences delivered
     * by each wind source are used during computing the averaged result across the wind sources. The result has the
     * averaged confidence attached.
     */
    WindWithConfidence<Util.Pair<Position, TimePoint>> getWindWithConfidence(Position p, TimePoint at,
            Set<WindSource> windSourcesToExclude);

    /**
     * Same as {@link #getEstimatedWindDirection(TimePoint)}, but propagates the confidence of the wind
     * estimation, relative to the <code>timePoint</code> for which the request is made, in the result. The
     * {@link Wind#getPosition() position} of all {@link Wind} fixes returned is <code>null</code>.
     */
    WindWithConfidence<TimePoint> getEstimatedWindDirectionWithConfidence(TimePoint timePoint);

    /**
     * After the call returns, {@link #getWindSourcesToExclude()} returns an iterable that equals
     * <code>windSourcesToExclude</code>
     */
    void setWindSourcesToExclude(Iterable<? extends WindSource> windSourcesToExclude);

    /**
     * Computes the average cross-track error for the legs with type {@link LegType#UPWIND}.
     * 
     * @param waitForLatestAnalysis
     *            if <code>true</code> and any cache update is currently going on, wait for the update to complete and
     *            then fetch the updated value; otherwise, serve this requests from whatever is currently in the cache
     */
    Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalysis)
            throws NoWindException;

    /**
     * Same as {@link #getAverageAbsoluteCrossTrackError(Competitor, TimePoint, boolean)}, only that a cache for leg type,
     * wind on leg and leg bearing is provided.
     */
    Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalyses,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) throws NoWindException;
    
    Distance getAverageAbsoluteCrossTrackError(Competitor competitor, TimePoint from, TimePoint to, boolean upwindOnly,
            boolean waitForLatestAnalyses) throws NoWindException;

    Distance getAverageSignedCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalysis)
            throws NoWindException;

    /**
     * Same as {@link #getAverageSignedCrossTrackError(Competitor, TimePoint, boolean)}, only that a cache for leg type,
     * wind direction and leg bearing is provided.
     */
    Distance getAverageSignedCrossTrackError(Competitor competitor, TimePoint timePoint, boolean waitForLatestAnalyses,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) throws NoWindException;

    Distance getAverageSignedCrossTrackError(Competitor competitor, TimePoint from, TimePoint to, boolean upwindOnly,
            boolean waitForLatestAnalysis) throws NoWindException;

    public Distance getAverageRideHeight(Competitor competitor, TimePoint timePoint);

    WindStore getWindStore();

    Competitor getOverallLeader(TimePoint timePoint);
    
    Competitor getOverallLeader(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    Boat getBoatOfCompetitor(Competitor competitor);
    
    Competitor getCompetitorOfBoat(Boat boat);
    
    /**
     * Returns the competitors of this tracked race, according to their ranking. Competitors whose
     * {@link #getRank(Competitor)} is 0 will be sorted "worst".
     */
    List<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint);

    /**
     * Same as {@link #getCompetitorsFromBestToWorst(TimePoint)}, using a cache for wind, leg type and leg
     * bearing values.
     */
    List<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * When provided with a {@link WindStore} during construction, the tracked race will
     * asynchronously load the wind data for this tracked race from the wind store and the GPS store in a
     * background thread and update this tracked race with the results. Clients that want to wait for the wind
     * loading process to complete can do so by calling this method which will block until the wind loading has
     * completed.
     */
    void waitUntilLoadingFromWindStoreComplete() throws InterruptedException;

    /**
     * Whenever a {@link RegattaLog} is attached, fixes are loaded from the {@link SensorFixStore} for all mappings
     * found in the {@code RegattaLog} in a separate thread. This method blocks if there is such a thread loading
     * fixes, until that thread is finished.
     */
    void waitForLoadingToFinish() throws InterruptedException;
    
    /**
     * Returns the current status of the {@link TrackedRace}. This consists of one of the {@link TrackedRaceStatusEnum}
     * values plus a progress for LOADING state.<br>
     * Due to the fact that multiple loaders can exist that load data into the {@link TrackedRace}, the returned status
     * is a composite of those loader statuses. When a loader is finished, its status isn't tracked anymore. This causes
     * the overall progress to not be guaranteed to be monotonic (progress may jump to a lower percentage when one loader
     * that had a progress of 100% is finished and thus removed).
     * 
     * @see TrackedRaceStatus
     * @see DynamicTrackedRace#onStatusChanged(TrackingDataLoader, TrackedRaceStatus)
     */
    TrackedRaceStatus getStatus();

    /**
     * If the {@link #getStatus() status} is currently {@link TrackedRaceStatusEnum#LOADING}, blocks until the status changes to any
     * other status.
     */
    void waitUntilNotLoading();

    /**
     * Detaches the race log associated with this {@link TrackedRace}.
     * 
     * @return the race log detached or {@code null} if no race log can be found by the {@code identifier}
     */
    RaceLog detachRaceLog(Serializable identifier);
    
    /**
     * Detaches the link {@link RaceExecutionOrderProvider}
     */
    void detachRaceExecutionOrderProvider(RaceExecutionOrderProvider raceExecutionOrderProvider);
    
    /**
     * Attaches the passed race log with this {@link TrackedRace}.
     * This causes fixes from the {@link SensorFixStore} to be loaded for such {@link RegattaLogDeviceMappingEvent RegattaLogDeviceMappingEvents}
     * that are present in the {@link RaceLog raceLog}. This loading is offloaded into a separate thread, that blocks
     * serialization until it is finished. If multiple race logs are attached, the loading process is
     * forced to be serialized.
     * To guarantee that a the fixes for a race log have been fully loaded before continuing,
     * {@link #waitForLoadingToFinish()} can be used.
     * @param raceLog to be attached.
     */
    void attachRaceLog(RaceLog raceLog);
    
    /**
     * Attaches the passed {@link RegattaLog} with this {@link TrackedRace}.
     * This also causes fixes from the {@link SensorFixStore} to be loaded (see {@link #attachRaceLog(RaceLog)} for details).
     */
    void attachRegattaLog(RegattaLog regattaLog);
    
    /**
     * @return all currently attached {@link RegattaLog}s or an empty Iterable if there aren't any
     */
    Iterable<RegattaLog> getAttachedRegattaLogs();
    
    /**
     * Attaches a {@link RaceExecutionOrderProvider} to make a {@link TrackedRace} aware
     * which races are scheduled around it in the execution order of a {@link Regatta}.
     * */
    void attachRaceExecutionProvider(RaceExecutionOrderProvider raceExecutionOrderProvider);
    
    /**
     * Returns the attached race log event track for this race if any.
     * Otherwise <code>null</code>.
     */
    RaceLog getRaceLog(Serializable identifier);
    
    /**
     * A setter for the listener on course design changes suggested by one of the {@link RaceLog RaceLog} attached to this
     * race. The listener is mostly part of the tracking provider adapter.
     * 
     * @param listener
     *            the listener to operate with.
     */
    void addCourseDesignChangedListener(CourseDesignChangedListener listener);
    
    void addStartTimeChangedListener(StartTimeChangedListener listener);
    
    void removeStartTimeChangedListener(StartTimeChangedListener listener);

    void addRaceAbortedListener(RaceAbortedListener listener);

    /**
     * Tells how far the given <code>competitor</code> was from the start line at the time point of the given seconds before the start.
     * <p>
     * 
     * The distance to the line is calculated by projecting the competitor's position onto the line orthogonally and
     * computing the distance of the projected position and the competitor's position.
     * <p>
     * 
     * Should the course be empty, <code>null</code> is returned. If the course's first waypoint is not a line or gate,
     * the geometric distance between the first waypoint and the competitor's position at <code>timePoint</code> is
     * returned. If the competitor's position cannot be determined, <code>null</code> is returned.
     */
    Distance getDistanceToStartLine(Competitor competitor, long millisecondsBeforeRaceStart);

    /**
     * Tells how far the given <code>competitor</code> was from the start line at the given <code>timePoint</code>.
     * Using the {@link #getStartOfRace() race start time} for <code>timePoint</code>, this tells the competitor's
     * distance to the line when the race was started.
     * <p>
     * 
     * The distance to the line is calculated by projecting the competitor's position onto the line orthogonally and
     * computing the distance of the projected position and the competitor's position.
     * <p>
     * 
     * Should the course be empty, <code>null</code> is returned. If the course's first waypoint is not a line or gate,
     * the geometric distance between the first waypoint and the competitor's position at <code>timePoint</code> is
     * returned. If the competitor's position cannot be determined, <code>null</code> is returned.
     */
    Distance getDistanceToStartLine(Competitor competitor, TimePoint timePoint);

    /**
     * When the <code>competitor</code> has started, this method returns the distance to the starboard end of the start line
     * or---if the start waypoint was a single mark---the distance to the single start mark at the time the competitor started.
     * If the competitor hasn't started yet, <code>null</code> is returned.
     */
    Distance getDistanceFromStarboardSideOfStartLineWhenPassingStart(Competitor competitor);
    
    /**
     * At the given timepoint and for the competitor, this method returns the distance to the starboard end of the start line
     * or---if the start waypoint was a single mark---the distance to the single start mark at the timepoint.
     * If the competitor hasn't started yet, <code>null</code> is returned.
     * 
     */
    Distance getDistanceFromStarboardSideOfStartLine(Competitor competitor, TimePoint timePoint);
    
    /**
     * The estimated speed of the competitor at the time point of the given seconds before the start of race. 
     */
    Speed getSpeed(Competitor competitor, long millisecondsBeforeRaceStart);
    
    /**
     * The speed of the competitor when crossing the start line. It will return null if there are no recorded
     * mark passings for this competitor (competitor did not yet or never pass the start line).
     */
    Speed getSpeedWhenCrossingStartLine(Competitor competitor);

    /**
     * Start time received by the tracking infrastructure. To determine real start time use {@link #getStartOfRace()}.
     */
    TimePoint getStartTimeReceived();
    
    /**
     * @return <code>null</code> if the start waypoint does not have two marks or the course
     * is empty or the start waypoint is the only waypoint
     */
    LineDetails getStartLine(TimePoint at);
    
    /**
     * @return <code>null</code> if the finish waypoint does not have two marks or the course
     * is empty or the finish waypoint is the only waypoint
     */
    LineDetails getFinishLine(TimePoint at);
    
    /**
     * Length of course if there are mark passings for competitors.
     */
    Distance getCourseLength();
    
    /**
     * The average wind speed with confidence for this race. It uses the timepoint of the race end as
     * a reference point.
     */
    SpeedWithConfidence<TimePoint> getAverageWindSpeedWithConfidence(long resolutionInMillis);
    
    /**
     * Computes the center point of the course's marks at the given time point.
     */
    Position getCenterOfCourse(TimePoint at);

    /**
     * If the {@link RacingProcedure} defined by any of the {@link #attachedRaceLogs attached} {@link RaceLog}s
     * has type {@link RacingProcedureType#GateStart}, this method returns <code>true</code>, <code>false</code> for
     * any other type found. If no type is found, e.g., because no race log is currently attached to this tracked race,
     * <code>null</code> is returned, meaning that the type is not known.
     */
    Boolean isGateStart();
    
    /**
     * Returns the time in milliseconds when the line was closed with lowering flag {@link Flags#GOLF} if {@link #isGateStart()} is <code>true</code>.
     * If flag was not raised or {@link #isGateStart()} is <code>false</code> it returns <code>null</code>. 
     */
    long getGateStartGolfDownTime();
    
    /**
     * If the race was started with a gate start (see {@link #isGateStart()}, this method returns the distance between
     * the competitor's starting position and the port side of the start line (pin end); otherwise, returns a zero
     * distance.
     */
    Distance getAdditionalGateStartDistance(Competitor competitor, TimePoint timePoint);

    boolean isUsingMarkPassingCalculator();
    
    /**
     * Calculates the estimated time it takes a competitor to sail the race, from start to finish.
     * 
     * @param timepoint
     *            Used for positions of marks and wind information; note that sometimes the marks are not in place yet
     *            when the race starts and that a windward mark may be collected already before the race finishes.
     * 
     * @return estimated time it takes to complete the race, plus more useful information about how this result came about
     * 
     * @throws NotEnoughDataHasBeenAddedException
     *             thrown if not enough polar data has been added or polar data service is not available
     * @throws NoWindException
     */
    TargetTimeInfo getEstimatedTimeToComplete(TimePoint timepoint) throws NotEnoughDataHasBeenAddedException, NoWindException;

    /**
     * Determine the time sailed for the {@code competitor} at {@code timePoint} in this race. This ignores whether or
     * not the race has recorded a start mark passing for the {@code competitor}. If no finish mark passing is found
     * either, the duration between the {@link #getStartOfRace() race start time} and {@code timePoint} is returned;
     * otherwise the duration between the {@link #getStartOfRace() race start time} and the time when the
     * {@code competitor} finished the race. If there is no mark passing for {@code competitor} for the last waypoint or
     * no {@link TrackedRace#getStartOfRace()} is known, {@code null} is returned.
     */
    default Duration getTimeSailedSinceRaceStart(Competitor competitor, TimePoint timePoint) {
        return null;
    }

    /**
     * Calculates the estimated distance it takes a competitor to sail the race, from start to finish.
     * 
     * @param timepoint
     *            Used for positions of marks and wind information; note that sometimes the marks are not in place yet
     *            when the race starts and that a windward mark may be collected already before the race finishes.
     * 
     * @return estimated time it takes to complete the race, plus more useful information about how this result came
     *         about
     * 
     * @throws NotEnoughDataHasBeenAddedException
     *             thrown if not enough polar data has been added or polar data service is not available
     * @throws NoWindException
     */
    Distance getEstimatedDistanceToComplete(TimePoint now) throws NotEnoughDataHasBeenAddedException, NoWindException;

    void setPolarDataService(PolarDataService polarDataService);

    default RaceLogResolver getRaceLogResolver() {
        return null;
    }

    default Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog> getTrackingTimesFromRaceLogs() {
        return null;
    }
    
    default Pair<TimePoint, TimePoint> getStartAndFinishedTimeFromRaceLogs() {
        return null;
    }

    /**
     * Returns all marks found in the {@link #markTracks} map and the mark device mappings and mark
     * definition events in all attached race and regatta logs. Note that usually a device mapping should
     * exist for a mark only if that mark is also defined by a {@link RegattaLogDefineMarkEvent}, so for
     * this standard case, adding the marks from the device mark mappings would be redundant.
     */
    default Iterable<Mark> getMarksFromRegattaLogs() {
        return getMarks();
    }
    
    /**
     * Updates the start and end of tracking in the following precedence order:
     * 
     * <ol>
     * <li>start/end of tracking in Racelog</li>
     * <li>manually set start/end of tracking via {@link #setStartOfTrackingReceived(TimePoint, boolean)} and {@link #setEndOfTrackingReceived(TimePoint, boolean)}</li>
     * <li>start/end of race in Racelog -/+ {@link #START_TRACKING_THIS_MUCH_BEFORE_RACE_START}/{@link #STOP_TRACKING_THIS_MUCH_AFTER_RACE_FINISH}</li>
     * </ol>
     */
    public void updateStartAndEndOfTracking(boolean waitForGPSFixesToLoad);
    
    default void lockForSerializationRead() {
    }
    
    default void unlockAfterSerializationRead() {
    }
    
    /**
     * @return all currently attached {@link RaceLog}s or an empty Iterable if there aren't any
     */
    Iterable<RaceLog> getAttachedRaceLogs();

    /**
     * Computes the average speed over ground for a {@link Competitor} based on times and distances for all
     * {@link TrackedLegOfCompetitor legs} the competitor {@link TrackedLegOfCompetitor#hasStartedLeg(TimePoint) has
     * started} already at {@code timePoint}.
     * 
     * @param timePoint
     *            time point up and until to compute the speed
     */
    Speed getAverageSpeedOverGround(Competitor competitor, TimePoint timePoint);

    
    /**
     * Computes the competitor's speed projected onto the wind (if wind data is available and the competitor is not
     * between start and finish (not racing) or not on a {@link LegType#REACHING reaching} leg; if outside a race and no
     * wind information is available, {@code null} is returned. Otherwise, the speed at {@code timePoint} is projected
     * onto the course. The wind direction at the {@link WindPositionMode#EXACT exact} competitor position at
     * {@code timePoint} is used for the calculation.
     */
    default SpeedWithBearing getVelocityMadeGood(Competitor competitor, TimePoint timePoint) {
        return getVelocityMadeGood(competitor, timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    /**
     * Like {@link #getVelocityMadeGood(Competitor, TimePoint)}, but allowing callers to specify a {@link WindPositionMode}
     * other than the default {@link WindPositionMode#EXACT}. If {@link WindPositionMode#LEG_MIDDLE} is used and the
     * competitor is not currently sailing on a leg (hasn't started or has already finished), {@code null} is returned.
     */
    default SpeedWithBearing getVelocityMadeGood(Competitor competitor, TimePoint timePoint, WindPositionMode windPositionMode) {
        return getVelocityMadeGood(competitor, timePoint, windPositionMode, new LeaderboardDTOCalculationReuseCache(timePoint));
    }
    
    
    /**
     * Computes the angle between the competitors direction and the wind's "from" direction. The angle's direction is chosen such that
     * it can be added to the boat's course over ground to arrive at the wind's {@link Wind#getFrom() "from"} direction. Example: wind
     * from the north (0deg), boat's course over ground 90deg (moving east), then the bearing returned is -90deg.
     */
    default Bearing getTWA(Competitor competitor, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        Bearing twa = null;
        final GPSFixTrack<Competitor, GPSFixMoving> sogTrack = this.getTrack(competitor);
        if (sogTrack != null) {
            SpeedWithBearing speedOverGround = sogTrack.getEstimatedSpeed(timePoint);
            Wind wind = cache.getWind(this, competitor, timePoint);
            if (wind != null && speedOverGround != null) {
                final Bearing projectToDirection = wind.getFrom();
                twa = speedOverGround.getBearing().getDifferenceTo(projectToDirection);
            }
        }
        return twa;
    }
    
    /**
     * Same as {@link #getTWA}, only that additionally a cache is provided that can allow the method to use
     * cached wind and leg type values.
     */
    default Bearing getTWA(Competitor competitor, TimePoint at){
        return getTWA(competitor, at, new LeaderboardDTOCalculationReuseCache(at));
    }
    
    /**
     * Like {@link #getVelocityMadeGood(Competitor, TimePoint)}, but allowing callers to specify a cache that can
     * accelerate requests for wind directions, the leg type and the competitor's current leg's bearing.
     */
    default SpeedWithBearing getVelocityMadeGood(Competitor competitor, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return getVelocityMadeGood(competitor, timePoint, WindPositionMode.EXACT, cache);
    }

    /**
     * Like {@link #getVelocityMadeGood(Competitor, TimePoint)}, but allowing callers to specify a
     * {@link WindPositionMode} other than the default {@link WindPositionMode#EXACT} as well as a cache that can
     * accelerate requests for wind directions, the leg type and the competitor's current leg's bearing. If
     * {@link WindPositionMode#LEG_MIDDLE} is used and the competitor is not currently sailing on a leg (hasn't started
     * or has already finished), {@code null} is returned.
     */
    SpeedWithBearing getVelocityMadeGood(Competitor competitor, TimePoint timePoint, WindPositionMode windPositionMode,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    boolean recordWind(Wind wind, WindSource windSource, boolean applyFilter);

    void removeWind(Wind wind, WindSource windSource);

    /**
     * Gets polar service which is currently set in this tracked race instance.
     * @see #setPolarDataService(PolarDataService)
     */
    PolarDataService getPolarDataService();

    /**
     * Sets wind estimation for this tracked race instance. The previous wind estimation with its wind source and wind
     * track are completely removed from this tracked race instance. If not {@code null}, the wind estimation is set and
     * configured accordingly so that it gets supplied by maneuver detector with new maneuvers in order to produce a
     * wind track with estimated wind. An appropriate wind source of type
     * {@link WindSourceType#MANEUVER_BASED_ESTIMATION} with corresponding wind track is added to the tracked race. If
     * {@code null}, the wind estimation will be disabled for the tracked race. After the call of this method, maneuver
     * cache and wind cache will be reset and its recalculation will be triggered.
     */
    void setWindEstimation(IncrementalWindEstimation windEstimation);

    /**
     * Obtains a quick, rough summary of the wind conditions during this race, based on a few wind samples at the
     * beginning, in the middle and at the end of the race. This is summarized in a min and max wind speed as well
     * as a single average wind direction.
     */
    WindSummary getWindSummary();
    
    @Override
    default QualifiedObjectIdentifier getIdentifier() {
        return getIdentifier(getRaceIdentifier());
    }
    
    public static QualifiedObjectIdentifier getIdentifier(RegattaAndRaceIdentifier regattaAndRaceId) {
        return getSecuredDomainType().getQualifiedObjectIdentifier(regattaAndRaceId.getTypeRelativeObjectIdentifier());
    }

    default TypeRelativeObjectIdentifier getTypeRelativeObjectIdentifier() {
        return getRaceIdentifier().getTypeRelativeObjectIdentifier();
    }

    @Override
    default String getName() {
        return getRaceIdentifier().getRaceName() + "@" + getRaceIdentifier().getRegattaName();
    }

    @Override
    default HasPermissions getType() {
        return getSecuredDomainType();
    }
    
    public static HasPermissions getSecuredDomainType() {
        return SecuredDomainType.TRACKED_RACE;
    }
}
