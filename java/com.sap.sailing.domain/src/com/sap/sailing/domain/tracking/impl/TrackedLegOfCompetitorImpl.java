package com.sap.sailing.domain.tracking.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.tracking.BravoFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingCache;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

/**
 * Provides a convenient view on the tracked leg, projecting to a single competitor's performance.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class TrackedLegOfCompetitorImpl implements TrackedLegOfCompetitor {
    private static final long serialVersionUID = -7060076837717432808L;
    private final TrackedLegImpl trackedLeg;
    private final Competitor competitor;
    
    public TrackedLegOfCompetitorImpl(TrackedLegImpl trackedLeg, Competitor competitor) {
        this.trackedLeg = trackedLeg;
        this.competitor = competitor;
    }

    @Override
    public TrackedLegImpl getTrackedLeg() {
        return trackedLeg;
    }

    @Override
    public Competitor getCompetitor() {
        return competitor;
    }

    @Override
    public Leg getLeg() {
        return trackedLeg.getLeg();
    }
    
    private TrackedRaceImpl getTrackedRace() {
        return getTrackedLeg().getTrackedRace();
    }

    @Override
    public TimePoint getTimePointNotAfterFinishingOfLeg(TimePoint timePoint) {
        final TimePoint result;
        MarkPassing passedStartWaypoint = getTrackedRace().getMarkPassing(getCompetitor(),
                getTrackedLeg().getLeg().getFrom());
        if (passedStartWaypoint != null && !passedStartWaypoint.getTimePoint().after(timePoint)) {
            MarkPassing passedEndWaypoint = getMarkPassingForLegEnd();
            if (passedEndWaypoint != null && timePoint.after(passedEndWaypoint.getTimePoint())) {
                // the query asks for a time point after the competitor has finished the leg; return the total leg time
                result = passedEndWaypoint.getTimePoint();
            } else {
                if (getTrackedRace().getEndOfTracking() != null && timePoint.after(getTrackedRace().getEndOfTracking())) {
                    result = getTrackedRace().getEndOfTracking();
                } else {
                    result = timePoint;
                }
            }
        } else {
            result = null;
        }
        return result;
    }
    
    @Override
    public Duration getTime(TimePoint timePoint) {
        final Duration result;
        MarkPassing passedStartWaypoint = getMarkPassingForLegStart();
        if (passedStartWaypoint == null) {
            result = null;
        } else {
            final TimePoint timePointNotAfterFinishingOfLeg = getTimePointNotAfterFinishingOfLeg(timePoint);
            result = timePointNotAfterFinishingOfLeg == null ? null : passedStartWaypoint.getTimePoint().until(timePointNotAfterFinishingOfLeg);
        }
        return result;
    }

    @Override
    public Distance getDistanceTraveled(TimePoint timePoint) {
        MarkPassing legStart = getMarkPassingForLegStart();
        if (legStart == null) {
            return null;
        } else {
            MarkPassing legEnd = getMarkPassingForLegEnd();
            TimePoint end = timePoint;
            if (legEnd != null && timePoint.compareTo(legEnd.getTimePoint()) > 0) {
                // timePoint is after leg finish; take leg end and end time point
                end = legEnd.getTimePoint();
            }
            return getTrackedRace().getTrack(getCompetitor()).getDistanceTraveled(legStart.getTimePoint(), end);
        }
    }
    
    @Override
    public Distance getDistanceTraveledConsideringGateStart(TimePoint timePoint) {
        final Distance result;
        final Distance preResult = getDistanceTraveled(timePoint);
        final Waypoint from = getLeg().getFrom();
        if (preResult != null && from == getTrackedRace().getRace().getCourse().getFirstWaypoint()) {
            result = preResult.add(getTrackedRace().getAdditionalGateStartDistance(getCompetitor(), timePoint));
        } else {
            result = preResult;
        }
        return result;
    }

    private MarkPassing getMarkPassingForLegStart() {
        MarkPassing legStart = getTrackedRace().getMarkPassing(getCompetitor(), getLeg().getFrom());
        return legStart;
    }

    private MarkPassing getMarkPassingForLegEnd() {
        MarkPassing legEnd = getTrackedRace().getMarkPassing(getCompetitor(), getLeg().getTo());
        return legEnd;
    }

    @Override
    public Speed getAverageSpeedOverGround(TimePoint timePoint) {
        Speed result;
        MarkPassing legStart = getMarkPassingForLegStart();
        if (legStart == null) {
            result = null;
        } else {
            TimePoint timePointToUse;
            if (hasFinishedLeg(timePoint)) {
                timePointToUse = getMarkPassingForLegEnd().getTimePoint();
            } else {
                // use time point of latest fix if before timePoint, otherwise timePoint
                GPSFixMoving lastFix = getTrackedRace().getTrack(getCompetitor()).getLastRawFix();
                if (lastFix == null) {
                    // No fix at all? Then we can't determine any speed 
                    timePointToUse = null;
                } else if (lastFix.getTimePoint().compareTo(timePoint) < 0) {
                    timePointToUse = lastFix.getTimePoint();
                } else {
                    timePointToUse = timePoint;
                }
            }
            if (timePointToUse != null) {
                Distance d = getDistanceTraveled(timePointToUse);
                result = d.inTime(legStart.getTimePoint().until(timePointToUse));
            } else {
                result = null;
            }
        }
        return result;
    }

    @Override
    public Distance getAverageRideHeight(TimePoint timePoint) {
        MarkPassing legStart = getMarkPassingForLegStart();
        if (legStart != null) {
            BravoFixTrack<Competitor> track = getTrackedRace()
                    .<BravoFix, BravoFixTrack<Competitor>> getSensorTrack(getCompetitor(), BravoFixTrack.TRACK_NAME);
            if (track != null) {
                TimePoint endTimePoint = hasFinishedLeg(timePoint) ? getMarkPassingForLegEnd().getTimePoint() : timePoint;
                return track.getAverageRideHeight(legStart.getTimePoint(), endTimePoint);
            }
        }
        return null;
    }

    @Override
    public Util.Pair<GPSFixMoving, Speed> getMaximumSpeedOverGround(TimePoint timePoint) {
        // fetch all fixes on this leg so far and determine their maximum speed
        MarkPassing legStart = getMarkPassingForLegStart();
        if (legStart == null) {
            return null;
        }
        MarkPassing legEnd = getMarkPassingForLegEnd();
        TimePoint to;
        if (legEnd == null || legEnd.getTimePoint().compareTo(timePoint) >= 0) {
            to = timePoint;
        } else {
            to = legEnd.getTimePoint();
        }
        GPSFixTrack<Competitor, GPSFixMoving> track = getTrackedRace().getTrack(getCompetitor());
        return track.getMaximumSpeedOverGround(legStart.getTimePoint(), to);
    }

    
    @Override
    public Distance getWindwardDistanceToGo(TimePoint timePoint, WindPositionMode windPositionMode, WindLegTypeAndLegBearingCache cache) {
        if (hasFinishedLeg(timePoint)) {
            return Distance.NULL;
        } else {
            Distance result = null;
            for (Mark mark : getLeg().getTo().getMarks()) {
                Distance d = getWindwardDistanceTo(mark, timePoint, windPositionMode, cache);
                if (result == null || d != null && d.compareTo(result) < 0) {
                    result = d;
                }
            }
            return result;
        }
    }

    @Override
    public Distance getWindwardDistanceToGo(TimePoint timePoint, WindPositionMode windPositionMode) {
        return getWindwardDistanceToGo(timePoint, windPositionMode, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    /**
     * If the current {@link #getLeg() leg} is +/- {@link LegType#UPWIND_DOWNWIND_TOLERANCE_IN_DEG} degrees collinear with the
     * wind's bearing, the competitor's position is projected onto the line crossing <code>mark</code> in the wind's
     * bearing, and the distance from the projection to the <code>mark</code> is returned. Otherwise, it is assumed that
     * the leg is neither an upwind nor a downwind leg, and hence the true distance to <code>mark</code> is returned. A
     * cache for wind and leg type / bearing can be passed to avoid their redundant calculation during a single
     * round-trip.
     * <p>
     * 
     * If no wind information is available, again the true geometrical distance to <code>mark</code> is returned.
     * <p>
     * 
     * If the competitor's position or the mark's position cannot be determined, <code>null</code> is returned.
     * <code>null</code> is also returned if the leg's bearing cannot be determined because for at least one of its two
     * waypoints no mark has a known position.
     */
    private Distance getWindwardDistanceTo(Mark mark, TimePoint at, WindPositionMode windPositionMode, WindLegTypeAndLegBearingCache cache) {
        Position estimatedPosition = getTrackedRace().getTrack(getCompetitor()).getEstimatedPosition(at, false);
        if (!hasStartedLeg(at) || estimatedPosition == null) {
            // covers the case with no fixes for this leg yet, also if the mark passing has already been received
            estimatedPosition = getTrackedRace().getOrCreateTrack(getLeg().getFrom().getMarks().iterator().next())
                    .getEstimatedPosition(at, false);
        }
        if (estimatedPosition == null) { // may happen if mark positions haven't been received yet
            return null;
        }
        final Position estimatedMarkPosition = getTrackedRace().getOrCreateTrack(mark).getEstimatedPosition(at, false);
        if (estimatedMarkPosition == null) {
            return null;
        }
        return getTrackedLeg().getAbsoluteWindwardDistance(estimatedPosition, estimatedMarkPosition, at, windPositionMode, cache);
    }

    /**
     * Projects <code>speed</code> onto the wind direction for upwind/downwind legs to see how fast a boat travels
     * "along the wind's direction." For reaching legs (neither upwind nor downwind), the speed is projected onto the
     * leg's direction.
     * 
     * @param speed
     *            if {@code null} then {@code null} will be returned
     * @param windPositionMode
     *            see {@link #getWind(Position, TimePoint, Set)}
     * 
     * @throws NoWindException
     *             in case the wind direction is not known
     */
    private SpeedWithBearing getWindwardSpeed(SpeedWithBearing speed, final TimePoint at, WindPositionMode windPositionMode,
            WindLegTypeAndLegBearingCache cache) {
        final SpeedWithBearing result;
        if (speed != null) {
            Bearing projectToBearing;
            try {
                if (cache.getLegType(getTrackedLeg(), at) != LegType.REACHING) {
                    final Wind wind = getTrackedRace().getWind(windPositionMode, getTrackedLeg(), getCompetitor(), at, cache);
                    if (wind == null) {
                        // This is not really likely to happen because wind==null would have let the call
                        // to cache.getLegType(...) fail with a NoWindException
                        throw new NoWindException("Need at least wind direction to determine windward speed");
                    }
                    projectToBearing = wind.getBearing();
                } else {
                    projectToBearing = cache.getLegBearing(getTrackedLeg(), at);
                }
            } catch (NoWindException nwe) {
                // as fallback in the absence of wind information, project to leg bearing
                projectToBearing = cache.getLegBearing(getTrackedLeg(), at);
            }
            if (speed.getBearing() != null && projectToBearing != null) {
                double cos = Math.cos(speed.getBearing().getRadians() - projectToBearing.getRadians());
                if (cos < 0) {
                    projectToBearing = projectToBearing.reverse();
                }
                result = new KnotSpeedWithBearingImpl(Math.abs(speed.getKnots() * cos), projectToBearing);
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Calculates the competitor's rank at {@code timePoint} based on the {@link WindPositionMode#LEG_MIDDLE} wind
     * direction for upwind and downwind legs, or based on the leg's rhumb line for reaching legs.
     */
    @Override
    public int getRank(TimePoint timePoint) {
        return getRank(timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }
    
    @Override
    public int getRank(TimePoint timePoint, WindLegTypeAndLegBearingCache cache) {
        int result = 0;
        if (hasStartedLeg(timePoint)) {
            List<TrackedLegOfCompetitor> competitorTracksByRank = getTrackedLeg().getCompetitorTracksOrderedByRank(timePoint, cache);
            result = competitorTracksByRank.indexOf(this)+1;
        }
        return result;
    }

    @Override
    public Speed getAverageVelocityMadeGood(TimePoint timePoint) {
        return getAverageVelocityMadeGood(timePoint, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    @Override
    public Speed getAverageVelocityMadeGood(TimePoint timePoint, WindLegTypeAndLegBearingCache cache) {
        Speed result = null;
        MarkPassing start = getMarkPassingForLegStart();
        if (start != null && start.getTimePoint().compareTo(timePoint) <= 0) {
            MarkPassing end = getMarkPassingForLegEnd();
            final TimePoint to;
            if (end != null && timePoint.compareTo(end.getTimePoint()) >= 0) {
                to = end.getTimePoint();
            } else {
                to = timePoint;
            }
            final Position endPos = getTrackedRace().getTrack(getCompetitor()).getEstimatedPosition(to, /* extrapolate */false);
            if (endPos != null) {
                final Position startPos = getTrackedRace().getTrack(getCompetitor()).getEstimatedPosition(start.getTimePoint(), false);
                if (startPos != null) {
                    Distance d = getTrackedLeg().getAbsoluteWindwardDistance(startPos, endPos, to,
                            WindPositionMode.EXACT, cache);
                    result = d == null ? null : d.inTime(to.asMillis() - start.getTimePoint().asMillis());
                }
            }
        }
        return result;
    }

    
    @Override
    public Integer getNumberOfTacks(TimePoint timePoint, boolean waitForLatest) throws NoWindException {
        Integer result = null;
        if (hasStartedLeg(timePoint)) {
            Iterable<Maneuver> maneuvers = getManeuvers(timePoint, waitForLatest);
            result = 0;
            for (Maneuver maneuver : maneuvers) {
                if (maneuver.getType() == ManeuverType.TACK) {
                    result++;
                }
            }
        }
        return result;
    }

    @Override
    public Iterable<Maneuver> getManeuvers(TimePoint timePoint, boolean waitForLatest) throws NoWindException {
        final Iterable<Maneuver> maneuvers;
        MarkPassing legStart = getMarkPassingForLegStart();
        if (legStart == null) {
            maneuvers = Collections.emptyList();
        } else {
            TimePoint start = legStart.getTimePoint();
            MarkPassing legEnd = getMarkPassingForLegEnd();
            TimePoint end = timePoint;
            if (legEnd != null && timePoint.compareTo(legEnd.getTimePoint()) > 0) {
                // timePoint is after leg finish; take leg end and end time point
                end = legEnd.getTimePoint();
            }
            maneuvers = getTrackedRace().getManeuvers(getCompetitor(),
                    start, end, waitForLatest);
        }
        return maneuvers;
    }

    @Override
    public Integer getNumberOfJibes(TimePoint timePoint, boolean waitForLatest) throws NoWindException {
        Integer result = null;
        if (hasStartedLeg(timePoint)) {
            Iterable<Maneuver> maneuvers = getManeuvers(timePoint, waitForLatest);
            result = 0;
            for (Maneuver maneuver : maneuvers) {
                if (maneuver.getType() == ManeuverType.JIBE) {
                    result++;
                }
            }
        }
        return result;
    }

    @Override
    public Integer getNumberOfPenaltyCircles(TimePoint timePoint, boolean waitForLatest) throws NoWindException {
        Integer result = null;
        if (hasStartedLeg(timePoint)) {
            Iterable<Maneuver> maneuvers = getManeuvers(timePoint, waitForLatest);
            result = 0;
            for (Maneuver maneuver : maneuvers) {
                if (maneuver.getType() == ManeuverType.PENALTY_CIRCLE) {
                    result++;
                }
            }
        }
        return result;
    }

    @Override
    public Distance getWindwardDistanceToCompetitorFarthestAhead(TimePoint timePoint, WindPositionMode windPositionMode, final RankingInfo rankingInfo) {
        return getWindwardDistanceToCompetitorFarthestAhead(timePoint, windPositionMode, rankingInfo, new LeaderboardDTOCalculationReuseCache(timePoint));
    }
    
    @Override
    public Distance getWindwardDistanceToCompetitorFarthestAhead(TimePoint timePoint, WindPositionMode windPositionMode, final RankingInfo rankingInfo, WindLegTypeAndLegBearingCache cache) {
        // FIXME bug 607 it seems the following fetches the leader of this leg, not the overall leader; validate!!! Use getTrackedRace().getRanks() instead
        Competitor competitorFarthestAhead = rankingInfo.getCompetitorFarthestAhead();
        TrackedLegOfCompetitor leaderLeg = getTrackedRace().getCurrentLeg(competitorFarthestAhead, timePoint);
        Distance result = null;
        Position leaderPosition = getTrackedRace().getTrack(competitorFarthestAhead).getEstimatedPosition(timePoint, /* extrapolate */ false);
        Position currentPosition = getTrackedRace().getTrack(getCompetitor()).getEstimatedPosition(timePoint, /* extrapolate */ false);
        if (leaderPosition != null && currentPosition != null) {
            result = Distance.NULL;
            boolean foundCompetitorsLeg = false;
            getTrackedRace().getRace().getCourse().lockForRead();
            try {
                for (Leg leg : getTrackedRace().getRace().getCourse().getLegs()) {
                    if (leg == getLeg()) {
                        foundCompetitorsLeg = true;
                    }
                    if (foundCompetitorsLeg) {
                        // if the leaderLeg is null, the leader has already arrived
                        if (leaderLeg == null || leg != leaderLeg.getLeg()) {
                            // add distance to next mark
                            Position nextMarkPosition = getTrackedRace().getApproximatePosition(leg.getTo(), timePoint);
                            if (nextMarkPosition == null) {
                                result = null;
                                break;
                            } else {
                                Distance distanceToNextMark = getTrackedRace().getTrackedLeg(leg)
                                        .getAbsoluteWindwardDistance(currentPosition, nextMarkPosition, timePoint, windPositionMode, cache);
                                if (distanceToNextMark != null) {
                                    result = new MeterDistance(result.getMeters() + distanceToNextMark.getMeters());
                                } else {
                                    result = null;
                                    break;
                                }
                            }
                            currentPosition = nextMarkPosition;
                        } else {
                            // we're now in the same leg with leader; compute windward distance to leader
                            final Distance absoluteWindwardDistance = getTrackedRace().getTrackedLeg(leg)
                                    .getAbsoluteWindwardDistance(currentPosition, leaderPosition, timePoint, windPositionMode, cache);
                            if (absoluteWindwardDistance != null) {
                                result = new MeterDistance(result.getMeters() + absoluteWindwardDistance.getMeters());
                            } else {
                                result = null;
                            }
                            break;
                        }
                    }
                }
            } finally {
                getTrackedRace().getRace().getCourse().unlockAfterRead();
            }
        }
        return result;
    }

    @Override
    public Distance getAverageAbsoluteCrossTrackError(TimePoint timePoint, boolean waitForLatestAnalysis) throws NoWindException {
        final Distance result;
        MarkPassing legStart = getMarkPassingForLegStart();
        if (legStart != null) {
            final TimePoint to = getTimePointNotAfterFinishingOfLeg(timePoint);
            if (to != null) {
                result = getTrackedRace().getAverageAbsoluteCrossTrackError(competitor, legStart.getTimePoint(), to,
                        /* upwindOnly */ false, waitForLatestAnalysis);
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Distance getAverageSignedCrossTrackError(TimePoint timePoint, boolean waitForLatestAnalysis) throws NoWindException {
        final Distance result;
        final MarkPassing legStartMarkPassing = getMarkPassingForLegStart();
        if (legStartMarkPassing != null) {
            TimePoint legStart = legStartMarkPassing.getTimePoint();
            final TimePoint to = getTimePointNotAfterFinishingOfLeg(timePoint);
            result = getTrackedRace().getAverageSignedCrossTrackError(competitor, legStart, to, /* upwindOnly */ false, waitForLatestAnalysis);
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Duration getGapToLeader(TimePoint timePoint, final Competitor leaderInLegAtTimePoint,
            final RankingInfo rankingInfo, WindPositionMode windPositionMode) throws NoWindException {
        return getGapToLeader(timePoint, leaderInLegAtTimePoint, windPositionMode, rankingInfo, new LeaderboardDTOCalculationReuseCache(timePoint));
    }
    
    @Override
    public Duration getGapToLeader(TimePoint timePoint, final Competitor leaderInLegAtTimePoint,
            WindPositionMode windPositionMode, final RankingInfo rankingInfo, WindLegTypeAndLegBearingCache cache) {
        return getGapToLeader(timePoint, ()->leaderInLegAtTimePoint, windPositionMode, rankingInfo, cache);
    }

    @FunctionalInterface
    private static interface LeaderGetter {
        Competitor getLeader();
    }

    @Override
    public Duration getGapToLeader(final TimePoint timePoint, final RankingInfo rankingInfo, WindPositionMode windPositionMode) {
        return getGapToLeader(timePoint, windPositionMode, rankingInfo, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    @Override
    public Duration getGapToLeader(final TimePoint timePoint, WindPositionMode windPositionMode, final RankingInfo rankingInfo, WindLegTypeAndLegBearingCache cache) {
        return getGapToLeader(timePoint, ()->getTrackedLeg().getLeader(hasFinishedLeg(timePoint) ? getFinishTime() : timePoint),
                windPositionMode, rankingInfo, new LeaderboardDTOCalculationReuseCache(timePoint));
    }
    
    private Duration getGapToLeader(TimePoint timePoint, LeaderGetter leaderGetter, WindPositionMode windPositionMode, RankingInfo rankingInfo, WindLegTypeAndLegBearingCache cache) {
        // If a competitor already completed this leg, compute the estimated arrival time at the
        // end of this leg and compare to the first mark passing for the end of this leg; if this leg's competitor also already
        // finished the leg, return the difference between this competitor's leg completion time point and the leader's completion
        // time point; else, calculate the windward distance to the leader and divide by
        // the windward speed
        // See also bug1080: using the average VMG instead of the current VMG may produce better results
        Speed windwardSpeed = getAverageVelocityMadeGood(timePoint, cache);
        // Has our competitor started the leg already? If not, we won't be able to compute a gap
        if (hasStartedLeg(timePoint)) {
            Iterable<MarkPassing> markPassingsInOrder = getTrackedRace().getMarkPassingsInOrder(getLeg().getTo());
            if (markPassingsInOrder != null) {
                MarkPassing firstMarkPassing = null;
                getTrackedRace().lockForRead(markPassingsInOrder);
                try {
                    Iterator<MarkPassing> markPassingsForLegEnd = markPassingsInOrder.iterator();
                    if (markPassingsForLegEnd.hasNext()) {
                        firstMarkPassing = markPassingsForLegEnd.next();
                    }
                } finally {
                    getTrackedRace().unlockAfterRead(markPassingsInOrder);
                }
                if (firstMarkPassing != null) {
                    // someone has already finished the leg
                    TimePoint whenLeaderFinishedLeg = firstMarkPassing.getTimePoint();
                    // Was it before the requested timePoint?
                    if (whenLeaderFinishedLeg.compareTo(timePoint) <= 0) {
                        // Has our competitor also already finished this leg?
                        if (hasFinishedLeg(timePoint)) {
                            // Yes, so the gap is the time period between the time points at which the leader and
                            // our competitor finished this leg.
                            return whenLeaderFinishedLeg.until(getMarkPassingForLegEnd().getTimePoint()); 
                        } else {
                            if (windwardSpeed == null) {
                                return null;
                            } else {
                                // leader has finished already; our competitor hasn't
                                Distance windwardDistanceToGo = getWindwardDistanceToGo(timePoint, windPositionMode);
                                Duration durationSinceLeaderPassedMarkToTimePoint = whenLeaderFinishedLeg.until(timePoint);
                                return windwardSpeed.getDuration(windwardDistanceToGo).plus(durationSinceLeaderPassedMarkToTimePoint);
                            }
                        }
                    }
                }
                // no-one has finished this leg yet at timePoint
                Competitor leader = leaderGetter.getLeader();
                // Maybe our competitor is the leader. Check:
                if (leader == getCompetitor()) {
                    return Duration.NULL; // the leader's gap to the leader
                } else {
                    if (windwardSpeed == null) {
                        return null;
                    } else {
                        // no, we're not the leader, so compute our windward distance and divide by our current VMG
                        Position ourEstimatedPosition = getTrackedRace().getTrack(getCompetitor())
                                .getEstimatedPosition(timePoint, false);
                        Position leaderEstimatedPosition = getTrackedRace().getTrack(leader).getEstimatedPosition(
                                timePoint, false);
                        if (ourEstimatedPosition == null || leaderEstimatedPosition == null) {
                            return null;
                        } else {
                            Distance windwardDistanceToGo = getTrackedLeg().getAbsoluteWindwardDistance(ourEstimatedPosition,
                                    leaderEstimatedPosition, timePoint, windPositionMode);
                            return windwardSpeed.getDuration(windwardDistanceToGo);
                        }
                    }
                }
            }
        }
        // else our competitor hasn't started the leg yet, so we can't compute a gap since we don't
        // have a speed estimate; leave result == null
        return null;
    }

    @Override
    public boolean hasStartedLeg(TimePoint timePoint) {
        MarkPassing markPassingForLegStart = getMarkPassingForLegStart();
        return markPassingForLegStart != null && markPassingForLegStart.getTimePoint().compareTo(timePoint) <= 0;
    }

    @Override
    public boolean hasFinishedLeg(TimePoint timePoint) {
        MarkPassing markPassingForLegEnd = getMarkPassingForLegEnd();
        return markPassingForLegEnd != null && markPassingForLegEnd.getTimePoint().compareTo(timePoint) <= 0;
    }
    
    @Override
    public TimePoint getStartTime() {
        MarkPassing markPassingForLegStart = getMarkPassingForLegStart();
        return markPassingForLegStart == null ? null : markPassingForLegStart.getTimePoint();
    }

    @Override
    public TimePoint getFinishTime() {
        MarkPassing markPassingForLegEnd = getMarkPassingForLegEnd();
        return markPassingForLegEnd == null ? null : markPassingForLegEnd.getTimePoint();
    }

    @Override
    public Speed getVelocityMadeGood(TimePoint at, WindPositionMode windPositionMode) {
        return getVelocityMadeGood(at, windPositionMode, new LeaderboardDTOCalculationReuseCache(at));
    }
    
    @Override
    public SpeedWithBearing getVelocityMadeGood(TimePoint at, WindPositionMode windPositionMode, WindLegTypeAndLegBearingCache cache) {
        if (hasStartedLeg(at)) {
            TimePoint timePoint;
            if (hasFinishedLeg(at)) {
                // use the leg finishing time point
                timePoint = getMarkPassingForLegEnd().getTimePoint();
            } else {
                timePoint = at;
            }
            SpeedWithBearing speedOverGround = getSpeedOverGround(timePoint);
            return speedOverGround == null ? null : getWindwardSpeed(speedOverGround, timePoint, windPositionMode, cache);
        } else {
            return null;
        }
    }

    @Override
    public SpeedWithBearing getSpeedOverGround(TimePoint at) {
        if (hasStartedLeg(at)) {
            TimePoint timePoint;
            if (hasFinishedLeg(at)) {
                // use the leg finishing time point
                timePoint = getMarkPassingForLegEnd().getTimePoint();
            } else {
                timePoint = at;
            }
            return getTrackedRace().getTrack(getCompetitor()).getEstimatedSpeed(timePoint);
        } else {
            return null;
        }
    }
    
    @Override
    public Bearing getHeel(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getHeel);
    }

    @Override
    public Bearing getPitch(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getPitch);
    }

    @Override
    public Distance getRideHeight(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getRideHeight);
    }
    
    @Override
    public Distance getDistanceFoiled(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getDistanceSpentFoiling);
    }

    @Override
    public Duration getDurationFoiled(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getTimeSpentFoiling);
    }

    @Override
    public Duration getEstimatedTimeToNextMark(TimePoint timePoint, WindPositionMode windPositionMode) {
        return getEstimatedTimeToNextMark(timePoint, windPositionMode, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    @Override
    public Duration getEstimatedTimeToNextMark(TimePoint timePoint, WindPositionMode windPositionMode, WindLegTypeAndLegBearingCache cache) {
        final Duration result;
        if (hasFinishedLeg(timePoint)) {
            result = Duration.NULL;
        } else {
            if (hasStartedLeg(timePoint)) {
                Distance windwardDistanceToGo = getWindwardDistanceToGo(timePoint, windPositionMode);
                Speed vmg = getVelocityMadeGood(timePoint, windPositionMode, cache);
                result = vmg == null ? null : vmg.getDuration(windwardDistanceToGo);
            } else {
                result = null;
            }
        }
        return result;
    }

    @Override
    public Distance getManeuverLoss(TimePoint timePointWhenSpeedStartedToDrop, TimePoint maneuverTimePoint,
            TimePoint timePointWhenSpeedLevelledOffAfterManeuver) {
        assert timePointWhenSpeedStartedToDrop != null;
        assert timePointWhenSpeedLevelledOffAfterManeuver != null;
        Distance result;
        final GPSFixTrack<Competitor, GPSFixMoving> track = getTrackedRace().getTrack(getCompetitor());
        SpeedWithBearing speedWhenSpeedStartedToDrop = track.getEstimatedSpeed(timePointWhenSpeedStartedToDrop);
        if (speedWhenSpeedStartedToDrop != null) {
            SpeedWithBearing speedAfterManeuver = track.getEstimatedSpeed(timePointWhenSpeedLevelledOffAfterManeuver);
            if (speedAfterManeuver != null) {
                // For upwind/downwind legs, find the mean course between inbound and outbound course and project actual and
                // extrapolated positions onto it:
                Bearing middleManeuverAngle = speedWhenSpeedStartedToDrop.getBearing().middle(speedAfterManeuver.getBearing());
                // extrapolate maximum speed before maneuver to time point of maximum speed after maneuver and project resulting position
                // onto the average maneuver course; compare to the projected position actually reached at the time point of maximum speed after
                // maneuver:
                Position positionWhenSpeedStartedToDrop = track.getEstimatedPosition(timePointWhenSpeedStartedToDrop, /* extrapolate */ false);
                Position extrapolatedPositionAtTimePointOfMaxSpeedAfterManeuver = 
                        speedWhenSpeedStartedToDrop.travelTo(positionWhenSpeedStartedToDrop, timePointWhenSpeedStartedToDrop, timePointWhenSpeedLevelledOffAfterManeuver);
                Position actualPositionAtTimePointOfMaxSpeedAfterManeuver = track.getEstimatedPosition(timePointWhenSpeedLevelledOffAfterManeuver, /* extrapolate */ false);
                Position projectedExtrapolatedPositionAtTimePointOfMaxSpeedAfterManeuver =
                        extrapolatedPositionAtTimePointOfMaxSpeedAfterManeuver.projectToLineThrough(positionWhenSpeedStartedToDrop, middleManeuverAngle);
                Position projectedActualPositionAtTimePointOfMaxSpeedAfterManeuver =
                        actualPositionAtTimePointOfMaxSpeedAfterManeuver.projectToLineThrough(positionWhenSpeedStartedToDrop, middleManeuverAngle);
                result = projectedActualPositionAtTimePointOfMaxSpeedAfterManeuver.getDistance(projectedExtrapolatedPositionAtTimePointOfMaxSpeedAfterManeuver);
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Bearing getBeatAngle(TimePoint at) throws NoWindException {
        return getBeatAngle(at, new LeaderboardDTOCalculationReuseCache(at));
    }
    
    @Override
    public Bearing getBeatAngle(TimePoint at, WindLegTypeAndLegBearingCache cache) throws NoWindException {
        Bearing beatAngle = null;
        Bearing projectToBearing;
        Wind wind = cache.getWind(getTrackedRace(), getCompetitor(), at);
        if (wind == null) {
            throw new NoWindException("Need at least wind direction to determine windward speed");
        }
        projectToBearing = wind.getFrom();
        SpeedWithBearing speed = getSpeedOverGround(at);
        if (speed != null) {
            beatAngle = speed.getBearing().getDifferenceTo(projectToBearing);
        }
        return beatAngle;
    }
    

    @Override
    public String toString() {
        return "TrackedLegOfCompetitor for "+getCompetitor()+" in leg "+getLeg();
    }

    @Override
    public Double getExpeditionAWA(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionAWAIfAvailable);
    }

    @Override
    public Double getExpeditionAWS(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionAWSIfAvailable);
    }

    @Override
    public Double getExpeditionTWA(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTWAIfAvailable);
    }

    @Override
    public Double getExpeditionTWS(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTWSIfAvailable);
    }

    @Override
    public Double getExpeditionTWD(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTWDIfAvailable);
    }

    @Override
    public Double getExpeditionTargTWA(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTargTWAIfAvailable);
    }

    @Override
    public Double getExpeditionBoatSpeed(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionBoatSpeedIfAvailable);
    }

    @Override
    public Double getExpeditionTargBoatSpeed(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTargBoatSpeedIfAvailable);
    }

    @Override
    public Double getExpeditionSOG(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionSOGIfAvailable);
    }

    @Override
    public Double getExpeditionCOG(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionCOGIfAvailable);
    }

    @Override
    public Double getExpeditionForestayLoad(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionForestayLoadIfAvailable);
    }

    @Override
    public Double getExpeditionRake(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionRakeIfAvailable);
    }

    @Override
    public Double getExpeditionCourseDetail(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionCourseDetailIfAvailable);
    }

    @Override
    public Double getExpeditionHeading(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionHeadingIfAvailable);
    }

    @Override
    public Double getExpeditionVMG(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionVMGIfAvailable);
    }

    @Override
    public Double getExpeditionVMGTargVMGDelta(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionVMGTargVMGDeltaIfAvailable);
    }

    @Override
    public Double getExpeditionRateOfTurn(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionRateOfTurnIfAvailable);
    }

    @Override
    public Double getExpeditionRudderAngle(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionRudderAngleIfAvailable);
    }

    @Override
    public Double getExpeditionHeel(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionHeelIfAvailable);
    }

    @Override
    public Double getExpeditionTargetHeel(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTargetHeelIfAvailable);
    }

    @Override
    public Double getExpeditionTimeToPortLayline(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTimeToPortLaylineIfAvailable);
    }

    @Override
    public Double getExpeditionTimeToStbLayline(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTimeToStbLaylineIfAvailable);
    }

    @Override
    public Double getExpeditionDistToPortLayline(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionDistToPortLaylineIfAvailable);
    }

    @Override
    public Double getExpeditionDistToStbLayline(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionDistToStbLaylineIfAvailable);
    }

    @Override
    public Duration getExpeditionTimeToGUN(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTimeToGUNIfAvailable);
    }

    @Override
    public Double getExpeditionTimeToCommitteeBoat(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTimeToCommitteeBoatIfAvailable);
    }

    @Override
    public Double getExpeditionTimeToPin(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTimeToPinIfAvailable);
    }

    @Override
    public Duration getExpeditionTimeToBurnToLine(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTimeToBurnToLineIfAvailable);
    }

    @Override
    public Double getExpeditionTimeToBurnToCommitteeBoat(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTimeToBurnToCommitteeBoatIfAvailable);
    }

    @Override
    public Double getExpeditionTimeToBurnToPin(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTimeToBurnToPinIfAvailable);
    }

    @Override
    public Double getExpeditionDistanceToCommitteeBoat(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionDistanceToCommitteeBoatIfAvailable);
    }

    @Override
    public Double getExpeditionDistanceToPinDetail(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionDistanceToPinDetailIfAvailable);
    }

    @Override
    public Double getExpeditionDistanceBelowLine(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionDistanceBelowLineIfAvailable);
    }

    @Override
    public Double getExpeditionLineSquareForWindDirection(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionLineSquareForWindIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionAWA(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionAWAIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionAWS(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionAWSIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionTWA(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTWAIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionTWS(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTWSIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionTWD(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTWDIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionTargTWA(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTargTWAIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionBoatSpeed(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionBoatSpeedIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionTargBoatSpeed(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTargBoatSpeedIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionSOG(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionSOGIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionCOG(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionCOGIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionForestayLoad(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionForestayLoadIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionRake(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionRakeIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionCourseDetail(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionCourseDetailIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionHeading(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionHeadingIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionVMG(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionVMGIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionVMGTargVMGDelta(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionVMGTargVMGDeltaIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionRateOfTurn(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionRateOfTurnIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionRudderAngle(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionRudderAngleIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionHeel(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionHeelIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionTargetHeel(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTargetHeelIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionTimeToPortLayline(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTimeToPortLaylineIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionTimeToStbLayline(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTimeToStbLaylineIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionDistToPortLayline(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionDistToPortLaylineIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionDistToStbLayline(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionDistToStbLaylineIfAvailable);
    }
    
    @Override
    public Duration getAverageExpeditionTimeToGUN(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTimeToGUNIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionTimeToCommitteeBoat(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTimeToCommitteeBoatIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionTimeToPin(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTimeToPinIfAvailable);
    }
    
    @Override
    public Duration getAverageExpeditionTimeToBurnToLine(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTimeToBurnToLineIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionTimeToBurnToCommitteeBoat(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTimeToBurnToCommitteeBoatIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionTimeToBurnToPin(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTimeToBurnToPinIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionDistanceToCommitteeBoat(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionDistanceToCommitteeBoatIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionDistanceToPinDetail(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionDistanceToPinDetailIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionDistanceBelowLine(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionDistanceBelowLineIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionLineSquareForWindDirection(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionLineSquareForWindIfAvailable);
    }
    
    private <R> R getExpeditionValueFromBravoFixTrackIfLegIsStarted(TimePoint at, BiFunction<BravoFixTrack<Competitor>, TimePoint, R> valueExtractor) {
        final R result;
        if (hasStartedLeg(at)) {
            TimePoint timePoint = hasFinishedLeg(at) ? getMarkPassingForLegEnd().getTimePoint() : at;
            BravoFixTrack<Competitor> track = getTrackedRace()
                    .<BravoFix, BravoFixTrack<Competitor>> getSensorTrack(competitor, BravoFixTrack.TRACK_NAME);
            result = track == null ? null : valueExtractor.apply(track, timePoint);
        } else {
            result = null;
        }
        return result;
    }
    
    private <R> R getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(TimePoint at, BravoTrackValueExtractor<R> valueExtractor) {
        if (hasStartedLeg(at)) {
            BravoFixTrack<Competitor> track = getTrackedRace()
                    .<BravoFix, BravoFixTrack<Competitor>> getSensorTrack(getCompetitor(), BravoFixTrack.TRACK_NAME);
            if (track != null) {
                TimePoint endTimePoint = hasFinishedLeg(at) ? getMarkPassingForLegEnd().getTimePoint() : at;
                return valueExtractor.getValue(track, getMarkPassingForLegStart().getTimePoint(), endTimePoint);
            }
        }
        return null;
    }
    
    private interface BravoTrackValueExtractor<R> {
        R getValue(BravoFixTrack<Competitor> track, TimePoint from, TimePoint to);
    }
}
