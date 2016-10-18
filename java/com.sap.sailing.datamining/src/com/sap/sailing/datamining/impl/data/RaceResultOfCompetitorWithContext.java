package com.sap.sailing.datamining.impl.data;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.datamining.data.HasLeaderboardContext;
import com.sap.sailing.datamining.data.HasRaceResultOfCompetitorContext;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.scalablevalue.impl.ScalableSpeed;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RaceResultOfCompetitorWithContext implements HasRaceResultOfCompetitorContext {
    private final static Logger logger = Logger.getLogger(RaceResultOfCompetitorWithContext.class.getName());
    
    private final HasLeaderboardContext leaderboardWithContext;
    private final RaceColumn raceColumn;
    private final Competitor competitor;
    private final PolarDataService polarDataService;

    public RaceResultOfCompetitorWithContext(HasLeaderboardContext leaderboardWithContext, RaceColumn raceColumn,
            Competitor competitor, PolarDataService polarDataService) {
        this.leaderboardWithContext = leaderboardWithContext;
        this.raceColumn = raceColumn;
        this.competitor = competitor;
        this.polarDataService = polarDataService;
    }

    @Override
    public HasLeaderboardContext getLeaderboardContext() {
        return leaderboardWithContext;
    }

    private Leaderboard getLeaderboard() {
        return getLeaderboardContext().getLeaderboard();
    }

    @Override
    public Competitor getCompetitor() {
        return competitor;
    }

    @Override
    public String getCompetitorSearchTag() {
        return getCompetitor().getSearchTag();
    }

    @Override
    public double getRelativeRank() {
        Leaderboard leaderboard = getLeaderboard();
        final TimePoint now = MillisecondsTimePoint.now();
        double competitorCount = Util.size(leaderboard.getCompetitors());
        double points = leaderboard.getTotalPoints(competitor, raceColumn, now);
        double relativeLowPoints = leaderboard.getScoringScheme().isHigherBetter() ?
                competitorCount - points : points;
        final double result = relativeLowPoints / competitorCount;
        return result;
    }
    
    @Override
    public double getAbsoluteRank() {
        try {
            return getLeaderboard().getTotalRankOfCompetitor(competitor, MillisecondsTimePoint.now());
        } catch (NoWindException e) {
            throw new IllegalStateException("No wind calculating the absoulte rank", e);
        }
    }

    @Override
    public int getAverageWindSpeedInRoundedBeaufort() {
        Speed exactResult = getAverageWindSpeed();
        return (int) Math.round(exactResult.getBeaufort());
    }

    /**
     * If there is no tracked race for the competitor or the race has no wind data, <code>null</code> is returned.
     * Otherwise, the average wind speed for the competitor is sampled in a one-minute interval throughout the race
     * duration.
     */
    private Speed getAverageWindSpeed() {
        final Speed result;
        TrackedRace trackedRace = raceColumn.getTrackedRace(getCompetitor());
        if (trackedRace == null) {
            result = null;
        } else {
            final ScalableSpeed[] windSpeedSum = new ScalableSpeed[1];
            windSpeedSum[0] = new ScalableSpeed(Speed.NULL);
            final long[] count = new long[1];
            final List<Leg> legs = trackedRace.getRace().getCourse().getLegs();
            if (legs.isEmpty()) {
                result = null;
            } else {
                final Leg firstLeg = legs.get(0);
                final Leg lastLeg = legs.get(legs.size() - 1);
                GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(getCompetitor());
                TimePoint started = trackedRace.getTrackedLeg(getCompetitor(), firstLeg).getStartTime();
                TimePoint finished = trackedRace.getTrackedLeg(getCompetitor(), lastLeg).getFinishTime();
                TimePoint from = started == null ? trackedRace.getStartOfRace() : started;
                TimePoint to = finished == null ? trackedRace.getEndOfRace() : finished;
                for (TimePoint timePoint = from; !timePoint.after(to); timePoint = timePoint.plus(Duration.ONE_MINUTE)) {
                    final Position position;
                    if (track != null) {
                        position = track.getEstimatedPosition(timePoint, /* extrapolate */false);
                    } else {
                        position = trackedRace.getCenterOfCourse(timePoint);
                    }
                    final WindWithConfidence<Pair<Position, TimePoint>> wind = trackedRace.getWindWithConfidence(
                            position, timePoint);
                    if (wind != null) {
                        if (wind.useSpeed()) {
                            windSpeedSum[0] = windSpeedSum[0].add(new ScalableSpeed(wind.getObject()));
                            count[0]++;
                        } else {
                            if (!track.hasDirectionChange(timePoint, /* minimumDegreeDifference */10)) {
                                // TODO try to estimate wind speed using polar data service, wind direction and COG/SOG
                                SpeedWithBearing cog = track.getEstimatedSpeed(timePoint);
                                final TrackedLegOfCompetitor currentLeg = trackedRace.getCurrentLeg(competitor, timePoint);
                                if (currentLeg != null) {
                                    LegType legType;
                                    try {
                                        legType = trackedRace.getTrackedLeg(currentLeg.getLeg()).getLegType(timePoint);
                                        final Tack tack = trackedRace.getTack(competitor, timePoint);
                                        if (tack != null) {
                                            Set<SpeedWithBearingWithConfidence<Void>> estimatedWindSpeeds = polarDataService
                                                    .getAverageTrueWindSpeedAndAngleCandidates(trackedRace.getRace()
                                                            .getBoatClass(), cog, legType, tack);
                                            if (!estimatedWindSpeeds.isEmpty()) {
                                                estimatedWindSpeeds.stream().max((a,b)->(int) Math.signum(a.getConfidence()-b.getConfidence())).ifPresent(swbwc-> {
                                                    windSpeedSum[0] = windSpeedSum[0].add(new ScalableSpeed(swbwc.getObject()));
                                                    count[0]++;
                                                });
                                            }
                                        }
                                    } catch (NoWindException e) {
                                        logger.log(Level.FINEST, "Can't determine wind direction, so no tack nor leg type known", e);
                                    }
                                }
                            }
                        }
                    }
                }
                if (count[0] > 0) {
                    result = windSpeedSum[0].divide(count[0]);
                } else {
                    result = null;
                }
            }
        }
        return result;
    }

    @Override
    public String getRegattaName() {
        Leaderboard leaderboard = getLeaderboard();;
        final String result = leaderboard.getName();
        return result;
    }

    @Override
    public Boolean isPodiumFinish() {
        Leaderboard leaderboard = getLeaderboard();
        final TimePoint now = MillisecondsTimePoint.now();
        double points = leaderboard.getTotalPoints(competitor, raceColumn, now);
        if (leaderboard.getScoringScheme().isHigherBetter()) {
            double competitorCount = Util.size(leaderboard.getCompetitors());
            return points >= (competitorCount - 2.05);
        } else {
            return points <= 3.05;
        }
    }

    @Override
    public Boolean isWin() {
        Leaderboard leaderboard = getLeaderboard();
        final TimePoint now = MillisecondsTimePoint.now();
        double points = leaderboard.getTotalPoints(competitor, raceColumn, now);
        if (leaderboard.getScoringScheme().isHigherBetter()) {
            double competitorCount = Util.size(leaderboard.getCompetitors());
            return points >= (competitorCount - 0.05);
        } else {
            return points <= 1.05;
        }
    }
    
}
