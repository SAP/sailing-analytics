package com.sap.sailing.domain.tracking;

import java.io.Serializable;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TackType;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public interface TrackedLegOfCompetitor extends Serializable {
    Leg getLeg();

    Competitor getCompetitor();

    Boat getBoat();

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
     * projection. When for the {@code timePoint} given the competitor hasn't finished the leg yet but has
     * already passed the approximate windward / along course position of the leg's end, a negative distance
     * will result. 
     */
    Distance getWindwardDistanceToGo(TimePoint timePoint, WindPositionMode windPositionMode);

    /**
     * Same as {@link #getWindwardDistanceToGo(TimePoint, WindPositionMode)}, only that a cache for wind and leg
     * type / bearing can be passed.
     */
    Distance getWindwardDistanceToGo(TimePoint timePoint, WindPositionMode windPositionMode, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * Like
     * {@link #getWindwardDistanceToGo(TimePoint, WindPositionMode, WindLegTypeAndLegBearingAndORCPerformanceCurveCache)},
     * but with the possibility to fix a {@link LegType} for analysis.
     * 
     * @param legTypeOrNull
     *            if {@code null}, the result is as specified for
     *            {@link #getWindwardDistanceToGo(TimePoint, WindPositionMode, WindLegTypeAndLegBearingAndORCPerformanceCurveCache)}, and
     *            the leg type will be inferred from the wind field at the middle of the leg for the given {@code timePoint}.
     *            In all other cases, the {@link LegType} specified will be assumed for determining the distance; in particular,
     *            for {@link LegType#REACHING reaching} legs, projection to the rhumb line instead of to the wind will be
     *            used.
     */
    Distance getWindwardDistanceToGo(LegType legTypeOrNull, TimePoint timePoint, WindPositionMode windPositionMode,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

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
     * Computes the competitor's average ride height for this leg from the beginning of the leg up to time
     * <code>timePoint</code>. If the competitor already completed the leg at <code>timePoint</code> and the respective
     * mark passing event was already received, the average ride height for the entire leg (and no further) is computed.
     */
    Distance getAverageRideHeight(TimePoint timePoint);

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
    Iterable<Maneuver> getManeuvers(TimePoint timePoint, boolean waitForLatest) throws NoWindException;
    
    /**
     * @return <code>null</code> if the competitor hasn't started this leg yet
     */
    Integer getNumberOfTacks(TimePoint timePoint, boolean waitForLatest) throws NoWindException;

    /**
     * @return <code>null</code> if the competitor hasn't started this leg yet
     */
    Integer getNumberOfJibes(TimePoint timePoint, boolean waitForLatest) throws NoWindException;

    /**
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
    int getRank(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * Computes the gap in seconds to the leader / winner of this leg. Returns <code>null</code> in case this leg's
     * competitor hasn't started the leg yet.
     */
    Duration getGapToLeader(TimePoint timePoint, RankingInfo rankingInfo, WindPositionMode windPositionMode);
    
    /**
     * Same as {@link #getGapToLeader(TimePoint, RankingInfo, WindPositionMode)}, only that a cache for wind and leg type data is used.
     */
    Duration getGapToLeader(TimePoint timePoint, WindPositionMode windPositionMode, RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * If a caller already went through the effort of computing the leg's leader at <code>timePoint</code>, it
     * can share this knowledge to speed up computation as compared to {@link #getGapToLeader(TimePoint, RankingInfo, WindPositionMode)}.
     */
    Duration getGapToLeader(TimePoint timePoint, Competitor leaderInLegAtTimePoint, RankingInfo rankingInfo, WindPositionMode windPositionMode) throws NoWindException;

    /**
     * Same as {@link #getGapToLeader(TimePoint, Competitor, RankingInfo, WindPositionMode)}, only that an additional cache is used
     * to avoid redundant evaluations of leg types and wind field information across various calculations that
     * all can use the same basic information.
     */
    Duration getGapToLeader(TimePoint timePoint, Competitor leaderInLegAtTimePoint,
            WindPositionMode windPositionMode, RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    boolean hasStartedLeg(TimePoint timePoint);
    
    boolean hasFinishedLeg(TimePoint timePoint);
    
    /**
     * @return <code>null</code> if the competitor hasn't yet started this leg; the time point when the competitor passed
     * the start waypoint of this leg otherwise
     */
    TimePoint getStartTime();
    
    /**
     * @return <code>null</code> if the competitor hasn't finished this leg yet; the time point when the competitor passed
     * the end waypoint of this leg otherwise
     */
    TimePoint getFinishTime();

    /**
     * Returns <code>null</code> in case this leg's competitor hasn't started the leg yet. If in the leg at
     * <code>timePoint</code>, returns the VMG value for this time point. If the competitor has already finished the
     * leg, the VMG at the time when the competitor finished the leg is returned.
     */
    Speed getVelocityMadeGood(TimePoint timePoint, WindPositionMode windPositionMode);

    /**
     * Same as {@link #getVelocityMadeGood(TimePoint, WindPositionMode)}, only that a cache for wind data and leg type and bearing
     * is passed.
     */
    SpeedWithBearing getVelocityMadeGood(TimePoint at, WindPositionMode windPositionMode, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);


    /**
     * Returns <code>null</code> in case this leg's competitor hasn't started the leg yet.
     */
    Duration getEstimatedTimeToNextMark(TimePoint timePoint, WindPositionMode windPositionMode);

    /**
     * Same as {@link #getEstimatedTimeToNextMark(TimePoint, WindPositionMode)}, only that a cache for leg type calculation is passed.
     */
    Duration getEstimatedTimeToNextMark(TimePoint timePoint, WindPositionMode windPositionMode, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * Returns <code>null</code> in case this leg's competitor hasn't started the leg yet. If in the leg at
     * <code>timePoint</code>, returns the current speed over ground for this time point. If the competitor has already
     * finished the leg, the speed over ground at the time the competitor finished the leg is returned.
     */
    SpeedWithBearing getSpeedOverGround(TimePoint at);
    
    /**
     * Returns <code>null</code> in case this leg's competitor hasn't started the leg yet. If in the leg at
     * <code>timePoint</code>, returns the current ride height for this time point. If the competitor has already
     * finished the leg, the ride height at the time the competitor finished the leg is returned.
     */
    Distance getRideHeight(TimePoint at);

    Bearing getHeel(TimePoint at);

    Bearing getPitch(TimePoint at);
    
    Distance getDistanceFoiled(TimePoint at);
    
    Duration getDurationFoiled(TimePoint at);
    
    /**
     * Computes the distance along the wind track to the wind-projected position of the race's overall leader. If leader
     * and competitor are in the same leg, this is simply the windward distance. If the leader is already one or more
     * legs ahead, it's the competitor's windward distance to go plus the windward distance between the marks of all
     * legs that the leader completed after this competitor's leg plus the windward distance between the leader and the
     * leader's leg's start.
     * <p>
     * 
     * If the leg is neither an {@link LegType#UPWIND upwind} nor a {@link LegType#DOWNWIND downwind} leg, the geometric
     * distance between this leg's competitor and the leader is returned. Note that this can lead to a situation where
     * the distance to leader is unrelated to the {@link #getWindwardDistanceToGo(TimePoint, WindPositionMode) distance
     * to go} which is used for ranking.
     * <p>
     * 
     * If at {@code timePoint} the {@link #getCompetitor() competitor} is not yet sailing in the {@link #getLeg() leg},
     * {@code null} will result. If at {@code timePoint} the {@link #getCompetitor() competitor} has already finished the
     * {@link #getLeg() leg}, the time point at which the competitor finished the leg is used as basis for the calculation
     * instead of {@code timePoint}. In particular, the leader is then determined for that finishing time point, and the
     * leader's position is determined for that finishing time point, too. This way, the result will---apart from any
     * out of order fix deliveries or course changes---remain constant for any later time point.
     * 
     * @param rankingInfo
     *            materialized ranking information that is pre-calculated to avoid expensive redundant work
     */
    Distance getWindwardDistanceToCompetitorFarthestAhead(TimePoint timePoint, WindPositionMode windPositionMode, RankingInfo rankingInfo);

    /**
     * Same as {@link #getWindwardDistanceToCompetitorFarthestAhead(TimePoint, WindPositionMode, RankingInfo)}, only that a cache for leg type
     * calculation is passed.
     */
    Distance getWindwardDistanceToCompetitorFarthestAhead(TimePoint timePoint, WindPositionMode windPositionMode, RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * Computes the average absolute cross track error for this leg. The cross track error for each fix is taken to be a
     * positive number, thereby ignoring whether the competitor was left or right of the course middle line. If you
     * provide this method with a {@link TimePoint} greater than the time the mark passing of the leg end mark has
     * occurred then the time point of the mark passing of the leg end mark will be taken into account.
     */
    Distance getAverageAbsoluteCrossTrackError(TimePoint timePoint, boolean waitForLatestAnalysis);
    
    /**
     * Computes the current absolute cross-track error (positive sign or zero, regardless of whether the competitor
     * is right or left of the course middle line of the current leg).
     * 
     * @return {@code null} if the competitor has not started or already finished the leg
     */
    Distance getAbsoluteCrossTrackError(TimePoint timePoint);

    /**
     * Computes the average signed cross track error for this leg. The cross track error for each fix is taken to be a
     * positive number in case the competitor was on the right side of the course middle line (looking in the direction
     * of this leg), and a negative number in case the competitor was on the left side of the course middle line. If you
     * provide this method with a {@link TimePoint} greater than the time the mark passing of the leg end mark has
     * occurred then the time point of the mark passing of the leg end mark will be taken into account.
     */
    Distance getAverageSignedCrossTrackError(TimePoint timePoint, boolean waitForLatestAnalysis);

    /**
     * Computes the current signed cross-track error (negative sign means left of the course middle line looking in the direction
     * of the leg; positive sign means right of the course middle line).
     * 
     * @return {@code null} if the competitor has not started or already finished the leg
     */
    Distance getSignedCrossTrackError(TimePoint timePoint);
    
    /**
     * Instead of projecting onto the course middle line connecting start and end of the leg,
     * projects onto the wind axis, attached to the leg's end waypoint.
     */
    Distance getUnsignedCrossTrackErrorToWindAxis(TimePoint timePoint);

    /**
     * Instead of projecting onto the course middle line connecting start and end of the leg,
     * projects onto the wind axis, attached to the leg's end waypoint.
     */ 
    Distance getSignedCrossTrackErrorToWindAxis(TimePoint timePoint);

    TrackedLeg getTrackedLeg();

    /**
     * Like {@link #getAverageVelocityMadeGood(TimePoint)}, only with an additional cache argument that allows the method to
     * use already computed values for wind and leg type, potentially also updating the cache as it goes.
     */
    Speed getAverageVelocityMadeGood(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * If the {@link #getCompetitor() competitor} hasn't started the {@link #getTrackedLeg() leg} yet at
     * <code>timePoint</code>, <code>null</code> is returned. Otherwise, if <code>timePoint</code> is before the finishing
     * of the leg, it is returned unchanged; else the time point at which the competitor has finished the leg is returned.
     * If the competitor hasn't finished the leg, <code>timePoint</code> or the end of the race's tracking is returned,
     * whichever is earlier.
     */
    TimePoint getTimePointNotAfterFinishingOfLeg(TimePoint timePoint);
    
    /**
     * For upwind: If the difference between COG and next waypoint direction is smaller than the one between COG and wind direction
     * {@link TackType#LONGTACK long tack} is returned; if it is greater or equal {@link TackType#SHORTTACK short tack}.
     * For downwind: Similar to upwind but instead of "wind direction", one use the opposite direction. So where the wind is blowing to.
     * For reaching: Similar to upwind but instead of comparing to "COG and wind direction", one use 10°. To see more differences.
     * 
     * @return {@code null} if {@link TimePoint} does not fit with current leg, or waypoint is {@code null}, or wind is
     *         {@code null}, or competitor position is {@code null}.
     */
    TackType getTackType(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) throws NoWindException;

    /**
     * Same as {@link #getTackType(TimePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache)}, only that an no-op cache will be used
     * for this single call. Good, e.g., for test cases.
     */
    default TackType getTackType(TimePoint timePoint) throws NoWindException {
        return getTackType(timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    Double getExpeditionAWA(TimePoint at);
    Double getExpeditionAWS(TimePoint at);
    Double getExpeditionTWA(TimePoint at);
    Double getExpeditionTWS(TimePoint at);
    Double getExpeditionTWD(TimePoint at);
    Double getExpeditionTargTWA(TimePoint at);
    Double getExpeditionBoatSpeed(TimePoint at);
    Double getExpeditionTargBoatSpeed(TimePoint at);
    Double getExpeditionSOG(TimePoint at);
    Double getExpeditionCOG(TimePoint at);
    Double getExpeditionForestayLoad(TimePoint at);
    Double getExpeditionRake(TimePoint at);
    Double getExpeditionCourseDetail(TimePoint at);
    Double getExpeditionHeading(TimePoint at);
    Double getExpeditionVMG(TimePoint at);
    Double getExpeditionVMGTargVMGDelta(TimePoint at);
    Double getExpeditionRateOfTurn(TimePoint at);
    Double getExpeditionRudderAngle(TimePoint at);
    Double getExpeditionTargetHeel(TimePoint at);
    Double getExpeditionTimeToPortLayline(TimePoint at);
    Double getExpeditionTimeToStbLayline(TimePoint at);
    Double getExpeditionDistToPortLayline(TimePoint at);
    Double getExpeditionDistToStbLayline(TimePoint at);
    Double getExpeditionTimeToGunInSeconds(TimePoint at);
    Double getExpeditionTimeToCommitteeBoat(TimePoint at);
    Double getExpeditionTimeToPin(TimePoint at);
    Double getExpeditionTimeToBurnToLineInSeconds(TimePoint at);
    Double getExpeditionTimeToBurnToCommitteeBoat(TimePoint at);
    Double getExpeditionTimeToBurnToPin(TimePoint at);
    Double getExpeditionDistanceToCommitteeBoat(TimePoint at);
    Double getExpeditionDistanceToPinDetail(TimePoint at);
    Double getExpeditionDistanceBelowLineInMeters(TimePoint at);
    Double getExpeditionLineSquareForWindDirection(TimePoint at);
    Double getExpeditionBaroIfAvailable(TimePoint at);
    Double getExpeditionLoadSIfAvailable(TimePoint at);
    Double getExpeditionLoadPIfAvailable(TimePoint at);
    Double getExpeditionJibCarPortIfAvailable(TimePoint at);
    Double getExpeditionJibCarStbdIfAvailable(TimePoint at);
    Double getExpeditionMastButtIfAvailable(TimePoint at);
    Double getExpeditionKickerTensionIfAvailable(TimePoint at);
    Double getAverageExpeditionAWA(TimePoint at);
    Double getAverageExpeditionAWS(TimePoint at);
    Double getAverageExpeditionTWA(TimePoint at);
    Double getAverageExpeditionTWS(TimePoint at);
    Double getAverageExpeditionTWD(TimePoint at);
    Double getAverageExpeditionTargTWA(TimePoint at);
    Double getAverageExpeditionBoatSpeed(TimePoint at);
    Double getAverageExpeditionTargBoatSpeed(TimePoint at);
    Double getAverageExpeditionSOG(TimePoint at);
    Double getAverageExpeditionCOG(TimePoint at);
    Double getAverageExpeditionForestayLoad(TimePoint at);
    Double getAverageExpeditionRake(TimePoint at);
    Double getAverageExpeditionCourseDetail(TimePoint at);
    Double getAverageExpeditionHeading(TimePoint at);
    Double getAverageExpeditionVMG(TimePoint at);
    Double getAverageExpeditionVMGTargVMGDelta(TimePoint at);
    Double getAverageExpeditionRateOfTurn(TimePoint at);
    Double getAverageExpeditionRudderAngle(TimePoint at);
    Double getAverageExpeditionTargetHeel(TimePoint at);
    Double getAverageExpeditionTimeToPortLayline(TimePoint at);
    Double getAverageExpeditionTimeToStbLayline(TimePoint at);
    Double getAverageExpeditionDistToPortLayline(TimePoint at);
    Double getAverageExpeditionDistToStbLayline(TimePoint at);
    Double getAverageExpeditionTimeToGunInSeconds(TimePoint at);
    Double getAverageExpeditionTimeToCommitteeBoat(TimePoint at);
    Double getAverageExpeditionTimeToPin(TimePoint at);
    Double getAverageExpeditionTimeToBurnToLineInSeconds(TimePoint at);
    Double getAverageExpeditionTimeToBurnToCommitteeBoat(TimePoint at);
    Double getAverageExpeditionTimeToBurnToPin(TimePoint at);
    Double getAverageExpeditionDistanceToCommitteeBoat(TimePoint at);
    Double getAverageExpeditionDistanceToPinDetail(TimePoint at);
    Double getAverageExpeditionDistanceBelowLineInMeters(TimePoint at);
    Double getAverageExpeditionLineSquareForWindDirection(TimePoint at);
    Double getAverageExpeditionBaroIfAvailable(TimePoint at);
    Double getAverageExpeditionLoadSIfAvailable(TimePoint at);
    Double getAverageExpeditionLoadPIfAvailable(TimePoint at);
    Double getAverageExpeditionJibCarPortIfAvailable(TimePoint at);
    Double getAverageExpeditionJibCarStbdIfAvailable(TimePoint at);
    Double getAverageExpeditionMastButtIfAvailable(TimePoint at);
    Double getAverageExpeditionKickerTensionIfAvailable(TimePoint at);
}
