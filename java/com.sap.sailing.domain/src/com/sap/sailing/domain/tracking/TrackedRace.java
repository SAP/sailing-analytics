package com.sap.sailing.domain.tracking;

import java.io.Serializable;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;

import com.sap.sailing.domain.base.Buoy;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.DouglasPeucker;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.Util.Pair;

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
public interface TrackedRace extends Serializable {
    final long MAX_TIME_BETWEEN_START_AND_FIRST_MARK_PASSING_IN_MILLISECONDS = 30000;
    
    RaceDefinition getRace();
    
    RaceIdentifier getRaceIdentifier();
    
    /**
     * Computes the estimated start time for this race (not to be confused with the {@link #getStartOfTracking()} time point
     * which is expected to be before the race start time). When there are no {@link MarkPassing}s for the first mark, <code>null</code>
     * is returned. If there are mark passings for
     * the first mark and the start time is less than
     * {@link #MAX_TIME_BETWEEN_START_AND_FIRST_MARK_PASSING_IN_MILLISECONDS} before the first mark passing for the
     * first mark. Otherwise, the first mark passing for the first mark minus
     * {@link #MAX_TIME_BETWEEN_START_AND_FIRST_MARK_PASSING_IN_MILLISECONDS} is returned as the race start time.
     */
    TimePoint getStart();
    
    /**
     * Computing the race end time is tricky. Boats may sink, stop, not finish, although they started the race. We therefore
     * cannot wait for all boats to reach the finish line.
     * 
     * TODO Currently, the time point returned is the time of the last mark passing recorded for the finish line or <code>null</code> if
     * no boat passed the finish line yet.
     */
    TimePoint getAssumedEnd();
    
    /**
     * Shorthand for <code>{@link #getStart()}.{@link TimePoint#compareTo(TimePoint) compareTo(at)} &lt;= 0</code>
     */
    boolean hasStarted(TimePoint at);
    
    Iterable<TrackedLeg> getTrackedLegs();
    
    TrackedLeg getTrackedLeg(Leg leg);
    
    /**
     * Tracking information about the leg <code>competitor</code> is on at <code>timePoint</code>, or
     * <code>null</code> if the competitor hasn't started any leg yet at <code>timePoint</code> or has
     * already finished the race.
     */
    TrackedLegOfCompetitor getCurrentLeg(Competitor competitor, TimePoint timePoint);
    
    /**
     * Tells which leg the leader at <code>timePoint</code> is on
     */
    TrackedLeg getCurrentLeg(TimePoint timePoint);
    
    /**
     * Precondition: waypoint must still be part of {@link #getRace()}.{@link RaceDefinition#getCourse() getCourse()}.
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
     * Tells the leg on which the <code>competitor</code> was at time <code>at</code>.
     * If the competitor hasn't passed the start waypoint yet, <code>null</code> is
     * returned because the competitor was not yet on any leg at that point in time. If
     * the time point happens to be after the last fix received from that competitor,
     * the last known leg for that competitor is returned. If the time point is after the
     * competitor's mark passing for the finish line, <code>null</code> is returned.
     * For all legs except the last, if the time point equals a mark passing time point
     * of the leg's starting waypoint, that leg is returned. For the time point of
     * the mark passing for the finish line, the last leg is returned.
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
     * Computes the rank of <code>competitor</code> in this race. A competitor is ahead of all
     * competitors that are one or more legs behind. Within the same leg, the rank is determined
     * by the windward distance to go and therefore depends on the assumptions of the wind direction
     * for the given <code>timePoint</code>. If the race hasn't {@link #hasStarted(TimePoint) started}
     * yet, the result is undefined.
     */
    int getRank(Competitor competitor, TimePoint timePoint) throws NoWindException;
    
    /**
     * For a competitor, computes the distance (TODO not yet clear whether over ground or
     * projected onto wind direction) into the race <code>secondsIntoTheRace</code> after
     * the race {@link TrackedRace#getStart() started}.
     */
    Distance getStartAdvantage(Competitor competitor, double secondsIntoTheRace);

    /**
     * For the given waypoint lists the {@link MarkPassing} events that describe which competitor passed the waypoint at
     * which point in time. This can, e.g., be used to sort those competitors who already finished a leg within the leg
     * that ends with <code>waypoint</code>. The remaining competitors need to be ordered by the advantage line-related
     * distance to the waypoint.
     * 
     * @return the iterable sequence of {@link MarkPassing}s as described above. To iterate on the resulting collection
     *         the caller needs to synchronize on the iterable returned because insertions into the underlying
     *         collection will also synchronize on that collection.
     */
    Iterable<MarkPassing> getMarkPassingsInOrder(Waypoint waypoint);

    /**
     * Obtains the {@link MarkPassing} for <code>competitor</code> passing <code>waypoint</code>. If no such
     * mark passing has been reported (yet), <code>null</code> is returned.
     */
    MarkPassing getMarkPassing(Competitor competitor, Waypoint waypoint);

    /**
     * Yields the track describing <code>buoy</code>'s movement over time; never <code>null</code> because a
     * new track will be created in case no track was present for <code>buoy</code> so far.
     */
    GPSFixTrack<Buoy, GPSFix> getOrCreateTrack(Buoy buoy);

    /**
     * If the <code>waypoint</code> only has one {@link #getBuoys() buoy}, its position at time <code>timePoint</code>
     * is returned. Otherwise, the center of gravity between the buoys' positions is computed and returned.
     */
    Position getApproximatePosition(Waypoint waypoint, TimePoint timePoint);
    
    /**
     * Same as {@link #getWind(Position, TimePoint, Iterable) getWind(p, at, Collections.emptyList())}
     */
    Wind getWind(Position p, TimePoint at);

    /**
     * Obtains estimated interpolated wind information for a given position and time point. The information is taken
     * from all wind sources available except for those listed in <code>windSourcesToExclude</code>, using the confidences
     * of the wind values provided by the various sources during averaging.
     */
    Wind getWind(Position p, TimePoint at, Iterable<WindSource> windSourcesToExclude);

    /**
     * Retrieves the wind sources used so far by this race that have the specified <code>type</code> as their
     * {@link WindSource#getType() type}. Always returns a non-<code>null</code> iterable which may be empty in case the
     * race does not use any wind source of the specified type (yet). Additional sources may be returned after
     * 
     */
    Iterable<WindSource> getWindSources(WindSourceType type);

    /**
     * Retrieves all wind sources used by this race.
     */
    Iterable<WindSource> getWindSources();

    WindTrack getOrCreateWindTrack(WindSource windSource);

    /**
     * Waits until {@link #getUpdateCount()} is after <code>sinceUpdate</code>.
     */
    void waitForNextUpdate(int sinceUpdate) throws InterruptedException;

    /**
     * Time stamp of the start of the actual tracking.
     * The value can be null (e.g. if we have not received any signal from the tracking infrastructure) 
     */
    TimePoint getStartOfTracking();

    /**
     * Time stamp of the end of the actual tracking.
     * The value can be null (e.g. if we have not received any signal from the tracking infrastructure) 
     */
    TimePoint getEndOfTracking();

    /**
     * Regardless of the order in which events were received, this method returns the latest time point contained by any of
     * the events received and processed.
     */
    TimePoint getTimePointOfNewestEvent();

    /**
     * Regardless of the order in which events were received, this method returns the oldest time point contained by any of
     * the events received and processed.
     */
    TimePoint getTimePointOfOldestEvent();

    /**
     * @return the mark passings for <code>competitor</code> in this race received so far; the mark passing objects are
     * returned such that their {@link MarkPassing#getWaypoint() waypoints} are ordered in the same way they are ordered
     * in the race's {@link Course}. Note, that this doesn't necessarily guarantee ascending time points, particularly
     * if premature mark passings have been detected accidentally as can be the case with some tracking providers such
     * as TracTrac. If the caller wants to iterate on the resulting collection or construct a {@link SortedSet#headSet(Object)}
     * or {@link SortedSet#tailSet(Object)} and then iterate over that, the caller needs to synchronize on the
     * collection returned because insertions into the competitor's mark passing collection will also synchronize
     * on that collection.
     */
    NavigableSet<MarkPassing> getMarkPassings(Competitor competitor);

    void removeWind(Wind wind, WindSource windSource);

    /**
     * Time stamp that the event received last from the underlying push service carried on it.
     * Note that these times may not increase monotonically.
     */
    TimePoint getTimePointOfLastEvent();

    long getMillisecondsOverWhichToAverageSpeed();

    long getMillisecondsOverWhichToAverageWind();

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
    Wind getEstimatedWindDirection(Position position, TimePoint timePoint);
    
    /**
     * Determines whether the <code>competitor</code> is sailing on port or starboard tack at the
     * <code>timePoint</code> requested. Note that this will have to retrieve information about the wind.
     * This, in turn, can lead to the current thread obtaining the monitor of the various wind tracks,
     * and, if the {@link WindSource#TRACK_BASED_ESTIMATION} source is used, also the monitors of the
     * competitors' GPS tracks.
     */
    Tack getTack(Competitor competitor, TimePoint timePoint);

    TrackedEvent getTrackedEvent();

    /**
     * Computes a default wind direction based on the direction of the first leg at time <code>at</code>, with a default
     * speed of one knot. Note that this wind direction can only be used if {@link #raceIsKnownToStartUpwind()} returns
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
     * Uses a {@link DouglasPeucker Douglas-Peucker} algorithm to approximate this track's fixes starting at
     * time <code>from</code> until time point <code>to</code> such that the maximum distance between the
     * track's fixes and the approximation is at most <code>maxDistance</code>.
     */
    List<GPSFixMoving> approximate(Competitor competitor, Distance maxDistance, TimePoint from, TimePoint to);

    /**
     * @return a non-<code>null</code> but perhaps empty list of the maneuvers that <code>competitor</code> performed in
     *         this race between <code>from</code> and <code>to</code>.
     */
    List<Maneuver> getManeuvers(Competitor competitor, TimePoint from, TimePoint to) throws NoWindException;

    /**
     * @return <code>true</code> if this race is known to start with an {@link LegType#UPWIND upwind} leg.
     * If this is the case, the wind estimation may default to using the first leg's direction at race start
     * time as the direction the wind comes from.
     */
    boolean raceIsKnownToStartUpwind();
    
    void addListener(RaceChangeListener listener);

    Distance getDistanceTraveled(Competitor competitor, TimePoint timePoint);

    Distance getWindwardDistanceToOverallLeader(Competitor competitor, TimePoint timePoint) throws NoWindException;
    
    /**
     * Calls {@link #getWindWithConfidence(Position, TimePoint, Iterable)} and excludes those wind sources listed in
     * {@link #getWindSourcesToExclude}.
     */
    WindWithConfidence<Pair<Position, TimePoint>> getWindWithConfidence(Position p, TimePoint at);
    
    /**
     * Lists those wind sources which by default are not considered in {@link #getWind(Position, TimePoint)} and
     * {@link #getWindWithConfidence(Position, TimePoint)}.
     */
    Iterable<WindSource> getWindSourcesToExclude();

    /**
     * Loops over this tracked race's wind sources and from each asks its averaged wind for the position <code>p</code>
     * and time point <code>at</code>, using the particular wind source's averaging interval. The confidences delivered
     * by each wind source are used during computing the averaged result across the wind sources. The result has the averaged
     * confidence attached.
     */
    WindWithConfidence<Pair<Position, TimePoint>> getWindWithConfidence(Position p, TimePoint at,
            Iterable<WindSource> windSourcesToExclude);

    /**
     * Same as {@link #getEstimatedWindDirection(Position, TimePoint)}, but propagates the confidence of the wind
     * estimation, relative to the <code>timePoint</code> for which the request is made, in the result.
     */
    WindWithConfidence<TimePoint> getEstimatedWindDirectionWithConfidence(Position position, TimePoint timePoint);

    /**
     * After the call returns, {@link #getWindSourcesToExclude()} returns an iterable that equals <code>windSourcesToExclude</code>
     */
    void setWindSourcesToExclude(Iterable<WindSource> windSourcesToExclude);
}
