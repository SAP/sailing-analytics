package com.sap.sailing.datamining.impl.data;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.datamining.Activator;
import com.sap.sailing.datamining.SailingClusterGroups;
import com.sap.sailing.datamining.data.HasLeaderboardContext;
import com.sap.sailing.datamining.data.HasRaceResultOfCompetitorContext;
import com.sap.sailing.datamining.data.HasTrackedRaceContext;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.scalablevalue.impl.ScalableSpeed;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.shared.impl.dto.ClusterDTO;

/**
 * Equality is based on the combination of {@link #getLeaderboard() leaderboard}, {@link #raceColumn} and
 * {@link #getCompetitor() competitor}.
 */
public class RaceResultOfCompetitorWithContext implements HasRaceResultOfCompetitorContext {
    private final static Logger logger = Logger.getLogger(RaceResultOfCompetitorWithContext.class.getName());
    
    private final HasLeaderboardContext leaderboardWithContext;
    private final RaceColumn raceColumn;
    private final Competitor competitor;
    private final PolarDataService polarDataService;
    private final HasTrackedRaceContext trackedRaceContext;

    public RaceResultOfCompetitorWithContext(HasLeaderboardContext leaderboardWithContext, RaceColumn raceColumn,
            Competitor competitor, PolarDataService polarDataService, HasTrackedRaceContext trackedRaceContext) {
        this.leaderboardWithContext = leaderboardWithContext;
        this.raceColumn = raceColumn;
        this.competitor = competitor;
        this.polarDataService = polarDataService;
        this.trackedRaceContext = trackedRaceContext;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((competitor == null) ? 0 : competitor.hashCode());
        result = prime * result + ((leaderboardWithContext == null) ? 0 : leaderboardWithContext.getLeaderboard().hashCode());
        result = prime * result + ((raceColumn == null) ? 0 : raceColumn.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RaceResultOfCompetitorWithContext other = (RaceResultOfCompetitorWithContext) obj;
        if (competitor == null) {
            if (other.competitor != null)
                return false;
        } else if (!competitor.equals(other.competitor))
            return false;
        if (leaderboardWithContext == null) {
            if (other.leaderboardWithContext != null)
                return false;
        } else if (!leaderboardWithContext.getLeaderboard().equals(other.leaderboardWithContext.getLeaderboard()))
            return false;
        if (raceColumn == null) {
            if (other.raceColumn != null)
                return false;
        } else if (!raceColumn.equals(other.raceColumn))
            return false;
        return true;
    }

    @Override
    public HasLeaderboardContext getLeaderboardContext() {
        return leaderboardWithContext;
    }
    
    @Override
    public HasTrackedRaceContext getTrackedRaceContext() {
        return trackedRaceContext;
    }

    private Leaderboard getLeaderboard() {
        return getLeaderboardContext().getLeaderboard();
    }

    @Override
    public Competitor getCompetitor() {
        return competitor;
    }

    @Override
    public Boat getBoat() {
        Boat boatOfCompetitor = getTrackedRaceContext().getTrackedRace().getBoatOfCompetitor(getCompetitor());
        return boatOfCompetitor;
    }

    @Override
    public String getRegattaName() {
        Leaderboard leaderboard = getLeaderboard();;
        final String result = leaderboard.getName();
        return result;
    }
    
    @Override
    public ClusterDTO getPercentageClusterForRelativeScore() {
        Double relativeScore = getRelativeRank();
        if (relativeScore == null) {
            return null;
        }
        SailingClusterGroups clusterGroups = Activator.getClusterGroups();
        Cluster<Double> cluster = clusterGroups.getPercentageClusterGroup().getClusterFor(relativeScore);
        return new ClusterDTO(clusterGroups.getPercentageClusterFormatter().format(cluster));
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
    public Double getRelativeRank() {
        final Leaderboard leaderboard = getLeaderboard();
        final TimePoint now = MillisecondsTimePoint.now();
        final double competitorCountInRace;
        final TrackedRace trackedRace = raceColumn.getTrackedRace(competitor);
        double highest = -1000;
        double lowest = 1000;
        if (trackedRace != null) {
            competitorCountInRace = Util.size(trackedRace.getRace().getCompetitors());
        } else {
            // no tracked race; try to determine the number of competitors based on the scores in the column:
            double maxNonPenaltyScoreInColumn = 0.0;
            final LeaderboardDTOCalculationReuseCache cache = new LeaderboardDTOCalculationReuseCache(now);
            for (final Competitor c : leaderboard.getCompetitors()) {
                final MaxPointsReason maxPointsReason = leaderboard.getMaxPointsReason(c, raceColumn, now);
                if (maxPointsReason != null && maxPointsReason == MaxPointsReason.NONE) {
                    final Double totalPoints = leaderboard.getTotalPoints(c, raceColumn, now, cache);
                    if (leaderboard.getScoringScheme().isHigherBetter()) {
                        // high-point scheme
                        // remember highest and lowest point and subtract to get competitor count better
                        if (totalPoints > highest) highest = totalPoints;
                        if (totalPoints < lowest) lowest = totalPoints;
                        // Add 1 because last boat gets 1 point
                        double diff = highest - lowest + 1;
                        if (totalPoints != null && maxNonPenaltyScoreInColumn < diff) {
                            maxNonPenaltyScoreInColumn = diff;
                        }
                    } else {
                        // low-point scheme
                        if (totalPoints != null && maxNonPenaltyScoreInColumn < totalPoints) {
                            maxNonPenaltyScoreInColumn = totalPoints;
                        }
                        // add 1 competitor if a 'n/a' (no score is entered in this race for the competitor) is found in the leaderboard
                        if(totalPoints == null) {
                            maxNonPenaltyScoreInColumn += 1;
                        }
                    }
                }
            }
            competitorCountInRace = maxNonPenaltyScoreInColumn;
        }
        final Double points = leaderboard.getTotalPoints(competitor, raceColumn, now);
        if (points != null && competitorCountInRace != 0.0) {
            double relativeLowPoints = leaderboard.getScoringScheme().isHigherBetter() ?
                    // calculate the points for first rank (doesn't necessarily need to be the number
                    // of competitors!) and subtract the points
                    leaderboard.getScoringScheme().getScoreForRank(leaderboard, raceColumn, competitor,
                            /* rank */ 1, // which score does the winner get in this high-point scheme?
                            /* numberOfCompetitorsInRaceFetcher */ ()->Util.size(raceColumn.getAllCompetitors()),
                            /* numberOfCompetitorsInLeaderboardFetcher */ leaderboard.getNumberOfCompetitorsInLeaderboardFetcher(),
                            now) - points
                    : points;
            final double result = relativeLowPoints / competitorCountInRace;
            return result;
        }
        return null;
    }
    
    @Override
    public Double getAbsoluteRank() {
        return getLeaderboard().getTotalPoints(competitor, raceColumn, MillisecondsTimePoint.now());
    }

    @Override
    public MaxPointsReason getMaxPointsReason() {
        return getLeaderboard().getMaxPointsReason(competitor, raceColumn, MillisecondsTimePoint.now());
    }

    @Override
    public boolean isDiscarded() {
        return getTrackedRaceContext().getLeaderboardContext().getLeaderboard().isDiscarded(competitor,
                getTrackedRaceContext().getRaceColumn(), MillisecondsTimePoint.now());
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
        final TimePoint now = MillisecondsTimePoint.now();
        return getLeaderboard().isWin(competitor, raceColumn, now);
    }
    
}
