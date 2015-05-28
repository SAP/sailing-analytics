package com.sap.sailing.domain.tracking;

import java.io.Serializable;
import java.util.List;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public interface TrackedLegOfCompetitor extends Serializable {
    Leg getLeg();

    Competitor getCompetitor();

    /**
     * How much time did the {@link #getCompetitor competitor} spend in this {@link #getLeg() leg} at
     * <code>timePoint</code>? If the competitor hasn't started the leg yet at <code>timePoint</code>, <code>null</code>
     * is returned. If the competitor has finished the leg already at <code>timePoint</code>, the time it took the
     * competitor to complete the leg is returned. If the competitor didn't finish the leg before the end of tracking,
     * <code>null</code> is returned because this indicates that the tracker stopped sending valid data.
     */
    Duration getTime(TimePoint timePoint);

    /**
     * The distance over ground traveled by the competitor in this leg up to <code>timePoint</code>. If
     * <code>timePoint</code> is before the competitor started this leg, a {@link Distance#NULL zero} distance is
     * returned. If the <code>timePoint</code> is after the time point at which the competitor finished this leg (if the
     * respective mark passing has already been received), the total distance traveled in this leg is returned. If the
     * time point is after the last fix but the competitor hasn't finished the leg yet, the distance traveled up to the
     * position at which the competitor is estimated to be at <code>timePoint</code> is used.
     */
    Distance getDistanceTraveled(TimePoint timePoint);

    /**
     * When a race uses a gate start, competitors are free to choose their start time point within the gate opening
     * time. During this time a pathfinder boat, also called "rabbit," progresses on port tack until the gate launch
     * time is over. After the gate launch time, competitors may still start until the gate closes, but that's usually
     * not a useful option because starting after the gate launch time usually means losing time towards the next mark.
     * <p>
     * 
     * Depending on when a competitor starts, they will have different distances to sail: early starters a bit more,
     * late starters a bit less. To normalize and make comparable, for the first leg of a gate start race, this method
     * adds the distance between the competitor and the port side of the start line at the time point when the competitor
     * starts.
     */
    Distance getDistanceTraveledConsideringGateStart(TimePoint timePoint);
    
    /**
     * Estimates how much the competitor still has to go to the end waypoint of this leg, projected onto the wind
     * direction. If the competitor already finished this leg, a zero, non-<code>null</code> distance will result.
     * If the competitor hasn't started the leg yet, the full leg distance is returned. For reaching legs or when
     * no wind information is available, the projection onto the leg's direction will be used instead of wind
     * projection.
     */
    Distance getWindwardDistanceToGo(TimePoint timePoint, WindPositionMode windPositionMode);

    /**
     * Same as {@link #getWindwardDistanceToGo(TimePoint, WindPositionMode)}, only that a cache for wind and leg
     * type / bearing can be passed.
     */
    Distance getWindwardDistanceToGo(TimePoint timePoint, WindPositionMode legMiddle, WindLegTypeAndLegBearingCache cache);

    /**
     * Computes an approximation for the average velocity made good (windward / leeward speed) of this leg's competitor at
     * <code>timePoint</code>. If the competitor hasn't started the leg yet, <code>null</code> is returned. If the competitor
     * has already finished the leg, the average over the whole leg is computed, otherwise the average for the time interval
     * from the start of the leg up to <code>timePoint</code>.<p>
     * 
     * The approximation uses the wind direction of <code>timePoint</code> at the middle between start and end waypoint or of
     * the time point when the competitor completed the leg if that was before <code>timePoint</code>. Note that this does not
     * account for changing winds during the leg.
     */
    Speed getAverageVelocityMadeGood(TimePoint timePoint);

    /**
     * Computes the competitor's average speed over ground for this leg from the beginning of the leg up to time
     * <code>timePoint</code> or at the time of the last event received for the race in case <code>timePoint</code> is
     * after the time when the last fix for this competitor was received. If the competitor already completed the leg at
     * <code>timePoint</code> and the respective mark passing event was already received, the average speed over ground
     * for the entire leg (and no further) is computed.
     */
    Speed getAverageSpeedOverGround(TimePoint timePoint);

    /**
     * @return <code>null</code> if the competitor hasn't started this leg yet, otherwise the fix where the maximum speed was
     * achieved and the speed value. In case you provide <code>timepoint</code> that is greater than the time point of the
     * end of this leg the provided value will be ignored and replaced by the timepoint of the end of the leg.
     */
    Util.Pair<GPSFixMoving, Speed> getMaximumSpeedOverGround(TimePoint timePoint);

    /**
     * Infers the maneuvers of the competitor up to <code>timePoint</code> on this leg. If the competitor hasn't started
     * the leg at the time point specified, an empty list is returned. If the time point is after the competitor has
     * finished this leg, all of the competitor's maneuvers during this leg will be reported in chronological order. The
     * list may be empty if no maneuvers happened between the point in time when the competitor started the leg and
     * <code>timePoint</code>.<p>
     * 
     * Note that the mark passing maneuver at leg start and finish are not guaranteed to be part of this leg's maneuvers. They
     * may be part of the respective adjacent leg, depending on the maneuver's time point which may be slightly before, at, or
     * after the corresponding mark passing event.
     */
    List<Maneuver> getManeuvers(TimePoint timePoint, boolean waitForLatest) throws NoWindException;
    
    /**
     * @param waitForLatest TODO
     * @return <code>null</code> if the competitor hasn't started this leg yet
     */
    Integer getNumberOfTacks(TimePoint timePoint, boolean waitForLatest) throws NoWindException;

    /**
     * @param waitForLatest TODO
     * @return <code>null</code> if the competitor hasn't started this leg yet
     */
    Integer getNumberOfJibes(TimePoint timePoint, boolean waitForLatest) throws NoWindException;

    /**
     * @param waitForLatest TODO
     * @return <code>null</code> if the competitor hasn't started this leg yet
     */
    Integer getNumberOfPenaltyCircles(TimePoint timePoint, boolean waitForLatest) throws NoWindException;

    /**
     * Computes the competitor's rank within this leg. If the competitor has already finished this leg at
     * <code>timePoint</code>, the rank is determined by comparing to all other competitors that also finished this leg.
     * If not yet finished, the rank is i+j+1 where i is the number of competitors that already finished the leg, and j
     * is the number of competitors whose wind-projected distance to the leg's end waypoint is shorter than that of
     * <code>competitor</code>.
     * <p>
     * 
     * The wind projection is only an approximation of a more exact "advantage line" and in particular doesn't account
     * for crossing the lay line.
     */
    int getRank(TimePoint timePoint);

    /**
     * Same as {@link #getRank(TimePoint)} with the additional option to provide a cache
     * that can help avoid redundant calculations of wind and leg data.
     */
    int getRank(TimePoint timePoint, WindLegTypeAndLegBearingCache cache);

    /**
     * Computes the gap in seconds to the leader / winner of this leg. Returns <code>null</code> in case this leg's
     * competitor hasn't started the leg yet.
     * @param rankingInfo TODO
     */
    Duration getGapToLeader(TimePoint timePoint, RankingInfo rankingInfo, WindPositionMode windPositionMode);
    
    /**
     * Same as {@link #getGapToLeader(TimePoint, RankingInfo, WindPositionMode)}, only that a cache for wind and leg type data is used.
     * @param rankingInfo TODO
     */
    Duration getGapToLeader(TimePoint timePoint, WindPositionMode windPositionMode, RankingInfo rankingInfo, WindLegTypeAndLegBearingCache cache);

    /**
     * If a caller already went through the effort of computing the leg's leader at <code>timePoint</code>, it
     * can share this knowledge to speed up computation as compared to {@link #getGapToLeader(TimePoint, RankingInfo, WindPositionMode)}.
     * @param rankingInfo TODO
     */
    Duration getGapToLeader(TimePoint timePoint, Competitor leaderInLegAtTimePoint, RankingInfo rankingInfo, WindPositionMode windPositionMode) throws NoWindException;

    /**
     * Same as {@link #getGapToLeader(TimePoint, Competitor, RankingInfo, WindPositionMode)}, only that an additional cache is used
     * to avoid redundant evaluations of leg types and wind field information across various calculations that
     * all can use the same basic information.
     * @param rankingInfo TODO
     */
    Duration getGapToLeader(TimePoint timePoint, Competitor leaderInLegAtTimePoint,
            WindPositionMode windPositionMode, RankingInfo rankingInfo, WindLegTypeAndLegBearingCache cache);

    boolean hasStartedLeg(TimePoint timePoint);
    
    boolean hasFinishedLeg(TimePoint timePoint);
    
    /**
     * @return <code>null</code> if the competitor hasn't yet started this leg; the time point when the competitor passed
     * the start waypoint of this leg otherwise
     */
    TimePoint getStartTime();
    
    TimePoint getFinishTime();

    /**
     * Returns <code>null</code> in case this leg's competitor hasn't started the leg yet. If in the leg at
     * <code>timePoint</code>, returns the VMG value for this time point. If the competitor has already finished the
     * leg, the VMG at the time when the competitor finished the leg is returned.
     */
    Speed getVelocityMadeGood(TimePoint timePoint, WindPositionMode windPositionMode) throws NoWindException;

    /**
     * Same as {@link #getVelocityMadeGood(TimePoint, WindPositionMode)}, only that a cache for wind data and leg type and bearing
     * is passed.
     */
    Speed getVelocityMadeGood(TimePoint at, WindPositionMode windPositionMode, WindLegTypeAndLegBearingCache cache);


    /**
     * Returns <code>null</code> in case this leg's competitor hasn't started the leg yet.
     */
    Duration getEstimatedTimeToNextMark(TimePoint timePoint, WindPositionMode windPositionMode);

    /**
     * Same as {@link #getEstimatedTimeToNextMark(TimePoint, WindPositionMode)}, only that a cache for leg type calculation is passed.
     */
    Duration getEstimatedTimeToNextMark(TimePoint timePoint, WindPositionMode windPositionMode, WindLegTypeAndLegBearingCache cache) throws NoWindException;

    /**
     * Returns <code>null</code> in case this leg's competitor hasn't started the leg yet. If in the leg at
     * <code>timePoint</code>, returns the current speed over ground for this time point. If the competitor has already
     * finished the leg, the speed over ground at the time the competitor finished the leg is returned.
     */
    SpeedWithBearing getSpeedOverGround(TimePoint at);

    /**
     * Computes the distance along the wind track to the wind-projected position of the race's overall leader. If leader
     * and competitor are in the same leg, this is simply the windward distance. If the leader is already one or more
     * legs ahead, it's the competitor's winward distance to go plus the windward distance between the marks of all legs
     * that the leader completed after this competitor's leg plus the windard distance between the leader and the
     * leader's leg's start.
     * <p>
     * 
     * If the leg is neither an {@link LegType#UPWIND upwind} nor a {@link LegType#DOWNWIND downwind} leg, the geometric
     * distance between this leg's competitor and the leader is returned. Note that this can lead to a situation where
     * the distance to leader is unrelated to the {@link #getWindwardDistanceToGo(TimePoint, WindPositionMode) distance to go} which is
     * used for ranking.
     * 
     * @param rankingInfo materialized ranking information that is pre-calculated to avoid expensive redundant work
     */
    Distance getWindwardDistanceToOverallLeader(TimePoint timePoint, WindPositionMode windPositionMode, RankingInfo rankingInfo);

    /**
     * Same as {@link #getWindwardDistanceToOverallLeader(TimePoint, WindPositionMode, RankingInfo)}, only that a cache for leg type
     * calculation is passed.
     * @param rankingInfo TODO
     */
    Distance getWindwardDistanceToOverallLeader(TimePoint timePoint, WindPositionMode windPositionMode, RankingInfo rankingInfo, WindLegTypeAndLegBearingCache cache);

    /**
     * Computes the average absolute cross track error for this leg. The cross track error for each fix is taken to be a
     * positive number, thereby ignoring whether the competitor was left or right of the course middle line. If you
     * provide this method with a {@link TimePoint} greater than the time the mark passing of the leg end mark has
     * occurred then the time point of the mark passing of the leg end mark will be taken into account.
     */
    Distance getAverageAbsoluteCrossTrackError(TimePoint timePoint, boolean waitForLatestAnalysis) throws NoWindException;

    /**
     * Computes the average signed cross track error for this leg. The cross track error for each fix is taken to be a
     * positive number in case the competitor was on the right side of the course middle line (looking in the direction
     * of this leg), and a negative number in case the competitor was on the left side of the course middle line. If you
     * provide this method with a {@link TimePoint} greater than the time the mark passing of the leg end mark has
     * occurred then the time point of the mark passing of the leg end mark will be taken into account.
     */
    Distance getAverageSignedCrossTrackError(TimePoint timePoint, boolean waitForLatestAnalysis) throws NoWindException;

    /**
     * Computes the maneuver loss as the distance projected onto the average course between entering and exiting the
     * maneuver that the boat lost compared to not having maneuvered. With this distance measure, the competitors speed
     * and bearing before the maneuver, as defined by <code>timePointBeforeManeuver</code> is extrapolated until
     * <code>timePointAfterManeuver</code>, and the resulting extrapolated position's "windward distance" is compared to
     * the competitor's actual position at that time. This distance is returned as the result of this method.
     */
    Distance getManeuverLoss(TimePoint timePointBeforeManeuver, TimePoint maneuverTimePoint, TimePoint timePointAfterManeuver) throws NoWindException;

    TrackedLeg getTrackedLeg();

    /**
     * Computes the angle between the competitors direction and the wind's "from" direction. The angle's direction is chosen such that
     * it can be added to the boat's course over ground to arrive at the wind's {@link Wind#getFrom() "from"} direction. Example: wind
     * from the north (0deg), boat's course over ground 90deg (moving east), then the bearing returned is -90deg.
     */
    Bearing getBeatAngle(TimePoint at) throws NoWindException;

    /**
     * Same as {@link #getBeatAngle}, only that additionally a cache is provided that can allow the method to use
     * cached wind and leg type values.
     */
    Bearing getBeatAngle(TimePoint at, WindLegTypeAndLegBearingCache cache) throws NoWindException;

    /**
     * Like {@link #getAverageVelocityMadeGood(TimePoint)}, only with an additional cache argument that allows the method to
     * use already computed values for wind and leg type, potentially also updating the cache as it goes.
     */
    Speed getAverageVelocityMadeGood(TimePoint timePoint, WindLegTypeAndLegBearingCache cache);

    /**
     * If the {@link #getCompetitor() competitor} hasn't started the {@link #getTrackedLeg() leg} yet at
     * <code>timePoint</code>, <code>null</code> is returned. Otherwise, if <code>timePoint</code> is before the finishing
     * of the leg, it is returned unchanged; else the time point at which the competitor has finished the leg is returned.
     * If the competitor hasn't finished the leg, <code>timePoint</code> or the end of the race's tracking is returned,
     * whichever is earlier.
     */
    TimePoint getTimePointNotAfterFinishingOfLeg(TimePoint timePoint);

}
