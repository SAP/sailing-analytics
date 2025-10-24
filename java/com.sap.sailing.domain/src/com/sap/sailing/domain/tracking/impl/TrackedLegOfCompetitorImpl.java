package com.sap.sailing.domain.tracking.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.TackType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.BravoFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * Provides a convenient view on the tracked leg, projecting to a single competitor's performance.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class TrackedLegOfCompetitorImpl implements TrackedLegOfCompetitor {
    private static final long serialVersionUID = -7060076837717432808L;
    private static final Bearing MAX_REACHING_TOLERANCE_AWAY_FROM_WAYPOINT = new DegreeBearingImpl(10);
    private final TrackedLegImpl trackedLeg;
    private final Competitor competitor;
    private final Boat boat;
    
    public TrackedLegOfCompetitorImpl(TrackedLegImpl trackedLeg, Competitor competitor, Boat boat) {
        this.trackedLeg = trackedLeg;
        this.competitor = competitor;
        this.boat = boat;
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
    public Boat getBoat() {
        return boat;
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
    public Distance getWindwardDistanceToGo(LegType legTypeOrNull, TimePoint timePoint, WindPositionMode windPositionMode, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Distance result;
        if (hasFinishedLeg(timePoint)) {
            result = Distance.NULL;
        } else {
            result = getWindwardDistanceTo(legTypeOrNull, getLeg().getTo(), timePoint, windPositionMode, cache);
        }
        return result;
    }
    
    @Override
    public Distance getWindwardDistanceToGo(TimePoint timePoint, WindPositionMode windPositionMode, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return getWindwardDistanceToGo(/* legTypeOrNull == null means infer leg type from wind and leg geometry */ null,
                timePoint, windPositionMode, cache);
    }

    @Override
    public Distance getWindwardDistanceToGo(TimePoint timePoint, WindPositionMode windPositionMode) {
        return getWindwardDistanceToGo(timePoint, windPositionMode, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    /**
     * If the current {@link #getLeg() leg} is +/- {@link LegType#UPWIND_DOWNWIND_TOLERANCE_IN_DEG} degrees collinear
     * with the wind's bearing, the competitor's position is projected onto the line crossing <code>waypoint</code> (its
     * approximate position) in the wind's bearing, and the distance from the projection to the <code>waypoint</code> is
     * returned. Otherwise, it is assumed that the leg is neither an upwind nor a downwind leg, and hence the
     * along-track distance to <code>waypoint</code> is returned. A cache for wind and leg type / bearing can be passed
     * to avoid their redundant calculation during a single round-trip.
     * <p>
     * 
     * If no wind information is available, again the along-track distance to <code>waypoint</code> is returned.
     * <p>
     * 
     * If the competitor's position or the waypoint's position cannot be determined, <code>null</code> is returned.
     * <code>null</code> is also returned if the leg's bearing cannot be determined because for at least one of its two
     * waypoints no mark has a known position.
     * <p>
     * 
     * The distance returned may turn negative if the competitor sailed past the approximate windward position of the
     * leg's {@link Leg#getTo() end waypoint} but hasn't finished the leg yet.
     * 
     * @param legTypeOrNull
     *            if {@code null}, the leg type will be determined for the {@code at} time point based on the wind at
     *            the middle of the leg and the leg's geometry at that time point. Otherwise, the leg type specified
     *            will be used; in particular, for {@link LegType#UPWIND} and {@link LegType#DOWNWIND}, projection to
     *            the wind direction at the leg middle will be used; for {@link LegType#REACHING}, projection to the
     *            rhumb line will be used.
     * 
     */
    private Distance getWindwardDistanceTo(LegType legTypeOrNull, Waypoint waypoint, TimePoint at,
            WindPositionMode windPositionMode, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Distance result;
        Position estimatedPosition = getTrackedRace().getTrack(getCompetitor()).getEstimatedPosition(at, /* extrapolate */ false);
        if (!hasStartedLeg(at) || estimatedPosition == null) {
            // covers the case with no fixes for this leg yet, also if the mark passing has already been received
            estimatedPosition = getTrackedRace().getOrCreateTrack(getLeg().getFrom().getMarks().iterator().next())
                    .getEstimatedPosition(at, /* extrapolate */ false);
        }
        if (estimatedPosition == null) { // may happen if mark positions haven't been received yet
            result = null;
        } else {
            final Position approximateWaypointPosition = getTrackedRace().getApproximatePosition(waypoint, at);
            if (approximateWaypointPosition == null) {
                result = null;
            } else {
                result = getTrackedLeg().getWindwardDistance(legTypeOrNull, estimatedPosition, approximateWaypointPosition, at, windPositionMode, cache);
            }
        }
        return result;
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
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
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
    public int getRank(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
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
    public Speed getAverageVelocityMadeGood(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
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
    public Distance getWindwardDistanceToCompetitorFarthestAhead(TimePoint timePoint, WindPositionMode windPositionMode, final RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        assert rankingInfo.getTimePoint().equals(timePoint); // the ranking info must be for timePoint
        Distance result;
        final TimePoint competitorLegStartTime = getStartTime();
        if (competitorLegStartTime != null && !timePoint.before(competitorLegStartTime)) {
            // only deliver a result if the competitor has started the leg at or before timePoint
            final TimePoint competitorLegFinishTime = getFinishTime();
            final TimePoint effectiveTimePoint;
            final RankingInfo effectiveRankingInfo;
            if (competitorLegFinishTime != null && timePoint.after(competitorLegFinishTime)) {
                // the competitor finished the leg before timePoint; use the finishing time point for all calculations,
                // including determining who is farthest ahead at that point in time:
                effectiveTimePoint = competitorLegFinishTime;
                effectiveRankingInfo = getTrackedRace().getRankingMetric().getRankingInfo(effectiveTimePoint);
            } else {
                effectiveTimePoint = timePoint;
                effectiveRankingInfo = rankingInfo;
            }
            final Competitor competitorFarthestAhead = effectiveRankingInfo.getCompetitorFarthestAhead();
            if (competitorFarthestAhead == getCompetitor()) {
                result = Distance.NULL;
            } else {
                final TrackedLegOfCompetitor leaderLeg = getTrackedRace().getCurrentLeg(competitorFarthestAhead, effectiveTimePoint);
                Position leaderPosition = getTrackedRace().getTrack(competitorFarthestAhead).getEstimatedPosition(effectiveTimePoint, /* extrapolate */ false);
                Position currentPosition = getTrackedRace().getTrack(getCompetitor()).getEstimatedPosition(effectiveTimePoint, /* extrapolate */ false);
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
                                // if the leaderLeg is null, the leader has already finished the race
                                if (leaderLeg == null || leg != leaderLeg.getLeg()) {
                                    // add distance to next mark because the leader is not in the same leg (but ahead because it's the leader)
                                    Position nextMarkPosition = cache.getApproximatePosition(getTrackedRace(), leg.getTo(), effectiveTimePoint);
                                    if (nextMarkPosition == null) {
                                        result = null;
                                        break;
                                    } else {
                                        Distance distanceToNextMark = getTrackedRace().getTrackedLeg(leg)
                                                .getAbsoluteWindwardDistance(currentPosition, nextMarkPosition, effectiveTimePoint, windPositionMode, cache);
                                        if (distanceToNextMark != null) {
                                            result = result.add(distanceToNextMark);
                                        } else {
                                            result = null;
                                            break;
                                        }
                                    }
                                    currentPosition = nextMarkPosition;
                                } else {
                                    // we're now in the same leg with leader; compute windward distance to leader
                                    final Distance absoluteWindwardDistance = getTrackedRace().getTrackedLeg(leg)
                                            .getAbsoluteWindwardDistance(currentPosition, leaderPosition, effectiveTimePoint, windPositionMode, cache);
                                    if (absoluteWindwardDistance != null) {
                                        result = result.add(absoluteWindwardDistance);
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
                } else {
                    result = null;
                }
            }
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Distance getAverageAbsoluteCrossTrackError(TimePoint timePoint, boolean waitForLatestAnalysis) {
        final Distance result;
        final MarkPassing legStart = getMarkPassingForLegStart();
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
    public Distance getAverageSignedCrossTrackError(TimePoint timePoint, boolean waitForLatestAnalysis) {
        final Distance result;
        final MarkPassing legStartMarkPassing = getMarkPassingForLegStart();
        if (legStartMarkPassing != null) {
            TimePoint legStart = legStartMarkPassing.getTimePoint();
            final TimePoint to = getTimePointNotAfterFinishingOfLeg(timePoint);
            if (to != null) {
                result = getTrackedRace().getAverageSignedCrossTrackError(competitor, legStart, to, /* upwindOnly */ false, waitForLatestAnalysis);
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }
    
    private Distance getSomeCrossTrackError(TimePoint timePoint, BiFunction<TrackedLeg, Position, Distance> crossTrackCalculatorAtTimePoint) {
        final Distance result;
        final GPSFixTrack<Competitor, GPSFixMoving> track = getTrackedRace().getTrack(getCompetitor());
        if (track != null) {
            final Position estimatedPosition = track.getEstimatedPosition(timePoint, /* extrapolate */ true);
            if (estimatedPosition != null) {
                result = crossTrackCalculatorAtTimePoint.apply(getTrackedLeg(), estimatedPosition);
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Distance getAbsoluteCrossTrackError(TimePoint timePoint) {
        return getSomeCrossTrackError(timePoint, (trackedLeg, estimatedPosition)->getTrackedLeg().getAbsoluteCrossTrackError(estimatedPosition, timePoint));
    }

    @Override
    public Distance getSignedCrossTrackError(TimePoint timePoint) {
        return getSomeCrossTrackError(timePoint, (trackedLeg, estimatedPosition)->getTrackedLeg().getSignedCrossTrackError(estimatedPosition, timePoint));
    }

    @Override
    public Distance getUnsignedCrossTrackErrorToWindAxis(TimePoint timePoint) {
        return getSomeCrossTrackError(timePoint, (trackedLeg, estimatedPosition)->getTrackedLeg().getUnsignedCrossTrackErrorToWindAxis(estimatedPosition, timePoint));
    }

    @Override
    public Distance getSignedCrossTrackErrorToWindAxis(TimePoint timePoint) {
        return getSomeCrossTrackError(timePoint, (trackedLeg, estimatedPosition)->getTrackedLeg().getSignedCrossTrackErrorToWindAxis(estimatedPosition, timePoint));
    }

    @Override
    public Duration getGapToLeader(TimePoint timePoint, final Competitor leaderInLegAtTimePoint,
            final RankingInfo rankingInfo, WindPositionMode windPositionMode) throws NoWindException {
        return getGapToLeader(timePoint, leaderInLegAtTimePoint, windPositionMode, rankingInfo, new LeaderboardDTOCalculationReuseCache(timePoint));
    }
    
    @Override
    public Duration getGapToLeader(TimePoint timePoint, final Competitor leaderInLegAtTimePoint,
            WindPositionMode windPositionMode, final RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
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
    public Duration getGapToLeader(final TimePoint timePoint, WindPositionMode windPositionMode, final RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return getGapToLeader(timePoint, ()->getTrackedLeg().getLeader(hasFinishedLeg(timePoint) ? getFinishTime() : timePoint),
                windPositionMode, rankingInfo, new LeaderboardDTOCalculationReuseCache(timePoint));
    }
    
    private Duration getGapToLeader(TimePoint timePoint, LeaderGetter leaderGetter, WindPositionMode windPositionMode, RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
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
    public SpeedWithBearing getVelocityMadeGood(TimePoint at, WindPositionMode windPositionMode, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
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
    public Duration getEstimatedTimeToNextMark(TimePoint timePoint, WindPositionMode windPositionMode, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
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
        Double result = null;
        final Bearing valueOrNull = getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getRudderIfAvailable);
        if(valueOrNull != null) {
            result = valueOrNull.getDegrees();
        }
        return result;
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
    public Double getExpeditionTimeToGunInSeconds(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTimeToGunInSecondsIfAvailable);
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
    public Double getExpeditionTimeToBurnToLineInSeconds(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionTimeToBurnToLineInSecondsIfAvailable);
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
    public Double getExpeditionDistanceBelowLineInMeters(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionDistanceBelowLineInMetersIfAvailable);
    }

    @Override
    public Double getExpeditionLineSquareForWindDirection(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionLineSquareForWindIfAvailable);
    }
    
    @Override
    public Double getExpeditionBaroIfAvailable(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionBaroIfAvailable);
    }

    @Override
    public Double getExpeditionLoadSIfAvailable(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionLoadSIfAvailable);
    }

    @Override
    public Double getExpeditionLoadPIfAvailable(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionLoadPIfAvailable);
    }

    @Override
    public Double getExpeditionJibCarPortIfAvailable(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionJibCarPortIfAvailable);
    }

    @Override
    public Double getExpeditionJibCarStbdIfAvailable(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionJibCarStbdIfAvailable);
    }

    @Override
    public Double getExpeditionMastButtIfAvailable(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionMastButtIfAvailable);
    }
    
    @Override
    public Double getExpeditionKickerTensionIfAvailable(TimePoint at) {
        return getExpeditionValueFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getExpeditionKickerTensionIfAvailable);
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
    public Double getAverageExpeditionTimeToGunInSeconds(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTimeToGunInSecondsIfAvailable);
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
    public Double getAverageExpeditionTimeToBurnToLineInSeconds(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionTimeToBurnToLineInSecondsIfAvailable);
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
    public Double getAverageExpeditionDistanceBelowLineInMeters(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionDistanceBelowLineInMetersIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionLineSquareForWindDirection(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionLineSquareForWindIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionBaroIfAvailable(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionBaroIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionLoadSIfAvailable(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionLoadSIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionLoadPIfAvailable(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionLoadPIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionJibCarPortIfAvailable(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionJibCarPortIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionJibCarStbdIfAvailable(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionJibCarStbdIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionMastButtIfAvailable(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionMastButtIfAvailable);
    }
    
    @Override
    public Double getAverageExpeditionKickerTensionIfAvailable(TimePoint at) {
        return getAverageExpeditionValueWithTimeRangeFromBravoFixTrackIfLegIsStarted(at, BravoFixTrack::getAverageExpeditionKickerTensionIfAvailable);
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
    
    @Override
    public TackType getTackType(TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) throws NoWindException {
        final TackType result;
        final MarkPassing start = getMarkPassingForLegStart();
        final MarkPassing end = getMarkPassingForLegEnd();
        if (start != null && !timePoint.before(start.getTimePoint()) && (end == null || timePoint.before(end.getTimePoint()))) {
            // TODO: missing solution for cases with PassingInstruction Offset and FixedBearing
            final Position waypointPosition = cache.getApproximatePosition(getTrackedRace(), getLeg().getTo(), timePoint);
            final Wind wind = cache.getWind(getTrackedRace(), competitor, timePoint);
            final Position competitorPosition = getTrackedRace().getTrack(competitor).getEstimatedPosition(timePoint, /* extrapolate */ true);
            if (waypointPosition != null && wind != null && competitorPosition != null) {
                final LegType legType = cache.getLegType(getTrackedLeg(), timePoint);
                final Bearing windBearing = legType == LegType.UPWIND ? wind.getFrom() : wind.getBearing();
                final SpeedWithBearing cogSog = getSpeedOverGround(timePoint);
                if (cogSog != null) {
                    final Bearing cog = cogSog.getBearing();
                    final Bearing bearingToWaypoint = competitorPosition.getBearingGreatCircle(waypointPosition);
                    // on reaching legs we won't compare to COG/Wind difference but to a fixed threshold, assuming
                    // that not sailing straight towards the next waypoint is a risk, conceptually making it the short tack
                    final Bearing diffWindToBoat = legType == LegType.REACHING ? MAX_REACHING_TOLERANCE_AWAY_FROM_WAYPOINT :
                        windBearing.getDifferenceTo(cog).abs();
                    final Bearing diffMarkToBoat = bearingToWaypoint.getDifferenceTo(cog).abs();
                    if (diffMarkToBoat.compareTo(diffWindToBoat) < 0) {
                        result = TackType.LONGTACK;
                    } else {
                        result = TackType.SHORTTACK;
                    }
                } else {
                    // no COG/SOG could be inferred
                    result = null;
                }
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }
}