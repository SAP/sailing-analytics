package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Tack;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.annotations.Statistic;
import com.sap.sse.datamining.shared.impl.dto.ClusterDTO;

public interface HasRaceOfCompetitorContext extends HasSomethingOfCompetitorContextWithTrackedRaceContext {
    @Override
    @Connector(scanForStatistics=false)
    HasTrackedRaceContext getTrackedRaceContext();
    
    @Dimension(messageKey="TackAtStart", ordinal=12)
    Tack getTackAtStart() throws NoWindException;
    
    @Connector(messageKey="Boat")
    Boat getBoat();
    
    @Dimension(messageKey="DistanceToStarboardSideAtStartOfCompetitor", ordinal=13)
    ClusterDTO getPercentageClusterForDistanceToStarboardSideAtStart();
    
    @Dimension(messageKey="RelativeScoreInPercent", ordinal=14)
    ClusterDTO getPercentageClusterForRelativeScore();
    
    @Dimension(messageKey="IRM")
    MaxPointsReason getMaxPointsReason();

    @Dimension(messageKey="Discarded")
    boolean isDiscarded();
    
    @Statistic(messageKey="DistanceAtStart", resultDecimals=2, ordinal=0)
    Distance getDistanceToStartLineAtStart();

    @Statistic(messageKey="DistanceToStarboardSideOfStartLineProjectedOntoLineAtStart", resultDecimals=1)
    Distance getDistanceFromStarboardSideOfStartLineProjectedOntoLineAtStartOfRace();

    @Statistic(messageKey="DistanceToNextBoatToPortProjectedToStartLineAtStartOfRace", resultDecimals=1)
    Distance getDistanceToNextBoatToPortProjectedToStartLineAtStartOfRace();

    @Statistic(messageKey="DistanceToNextBoatToStarboardProjectedToStartLineAtStartOfRace", resultDecimals=1)
    Distance getDistanceToNextBoatToStarboardProjectedToStartLineAtStartOfRace();

    @Statistic(messageKey="TotalDistanceToNeighboursProjectedToStartLineAtStartOfRace", resultDecimals=1)
    Distance getTotalDistanceToNeighboursProjectedToStartLineAtStartOfRace();
    
    @Statistic(messageKey="WindwardDistanceToNextBoatToPortAtStartOfRace", resultDecimals=1)
    Distance getWindwardDistanceToNextBoatToPortAtStartOfRace();

    @Statistic(messageKey="WindwardDistanceToNextBoatToStarboardAtStartOfRace", resultDecimals=1)
    Distance getWindwardDistanceToNextBoatToStarboardAtStartOfRace();

    @Statistic(messageKey="TotalWindwardDistanceToNeighboursAtStartOfRace", resultDecimals=1)
    Distance getTotalWindwardDistanceToNeighboursAtStartOfRace();

    @Statistic(messageKey="DistanceToNextBoatToPortAtStartOfRacePerpendicularToStartLine", resultDecimals=1)
    Distance getDistanceToNextBoatToPortPerpendicularToStartLineAtStartOfRace();

    @Statistic(messageKey="DistanceToNextBoatToStarboardAtStartOfRacePerpendicularToStartLine", resultDecimals=1)
    Distance getDistanceToNextBoatToStarboardPerpendicularToStartLineAtStartOfRace();

    @Statistic(messageKey="TotalDistanceToNeighboursPerpendicularToStarLineAtStartOfRace", resultDecimals=1)
    Distance getTotalDistanceToNeighboursPerpendicularToStarLineAtStartOfRace();

    @Statistic(messageKey="NormalizedDistanceToStarboardSideOfStartLineProjectedOntoLineAtStart", resultDecimals=2)
    Double getNormalizedDistanceFromStarboardSideOfStartLineProjectedOntoLineAtStartOfRace();

    @Statistic(messageKey="DistanceToStarboardSideAtStartOfCompetitor", resultDecimals=2)
    Double getNormalizedDistanceToStarboardSideAtStartOfCompetitor();
    
    @Statistic(messageKey="DistanceToStarboardSideAtStartOfCompetitorVsRankAtFirstMark", resultDecimals=1)
    Pair<Double, Integer> getNormalizedDistanceToStarboardSideAtStartOfCompetitorVsRankAtFirstMark();
    
    @Statistic(messageKey="WindwardDistanceToAdvantageousEndOfLineAtStartOfRace", resultDecimals=1, ordinal=1)
    Distance getWindwardDistanceToAdvantageousLineEndAtStartofRace();
    
    @Statistic(messageKey="WindwardDistanceToAdvantageousEndOfLineAtStartOfCompetitor", resultDecimals=1, ordinal=2)
    Distance getWindwardDistanceToAdvantageousLineEndAtStartofCompetitor();

    @Connector(messageKey="SpeedWhenStarting", ordinal=3)
    Speed getSpeedWhenStarting();
    
    @Statistic(messageKey="StartDelay", resultDecimals=1)
    Duration getStartDelay();
    
    @Connector(messageKey="SpeedTenSecondsBeforeStart", ordinal=4)
    Speed getSpeedTenSecondsBeforeStart();
    
    @Connector(messageKey="SpeedTenSecondsAfterStart", ordinal=5)
    Speed getSpeedTenSecondsAfterStartOfRace();
    
    @Statistic(messageKey="RankThirtySecondsAfterStart", resultDecimals=2, ordinal=6)
    Integer getRankThirtySecondsAfterStartOfRace();
    
    @Statistic(messageKey="RankAfterHalfOfTheFirstLeg", resultDecimals=2, ordinal=7)
    Double getRankAfterHalfOfTheFirstLeg();
    
    @Statistic(messageKey="RankAtFirstMark", resultDecimals=2, ordinal=8)
    Integer getRankAtFirstMark();
    
    @Statistic(messageKey="RankGainsOrLossesBetweenFirstMarkAndFinish", resultDecimals=2, ordinal=9)
    Integer getRankGainsOrLossesBetweenFirstMarkAndFinish();
    
    @Statistic(messageKey="NumberOfManeuvers", resultDecimals=0, ordinal=10)
    int getNumberOfManeuvers();
    
    @Statistic(messageKey="NumberOfTacks", resultDecimals=0, ordinal=11)
    int getNumberOfTacks();

    @Statistic(messageKey="NumberOfJibes", resultDecimals=0, ordinal=12)
    int getNumberOfJibes();

    @Statistic(messageKey="NumberOfPenaltyCircles", resultDecimals=0, ordinal=13)
    int getNumberOfPenaltyCircles();
    
    @Statistic(messageKey="DistanceTraveled", resultDecimals=1)
    Distance getDistanceTraveled();
    
    @Statistic(messageKey="LineLengthAtStart", resultDecimals=1)
    Distance getLineLengthAtStart();

    @Statistic(messageKey="AbsoluteWindwardDistanceToStarboardSideAtStartOfCompetitor", resultDecimals=2)
    Distance getAbsoluteWindwardDistanceToStarboardSideAtStartOfCompetitor();
    
    @Statistic(messageKey="DistanceToStarboardSideAtStartOfCompetitorVsFinalRank", resultDecimals=2)
    Pair<Double, Integer> getRelativeDistanceToStarboardSideAtStartOfCompetitorVsFinalRank();
    
    @Statistic(messageKey="WindwardDistanceToAdvantageousEndOfLineAtStartOfRaceVsRelativeDistanceToAdvantageousEndOfLine", resultDecimals=2)
    Pair<Double, Double> getWindwardDistanceToAdvantageousEndOfLineAtStartOfRaceVsRelativeDistanceToAdvantageousEndOfLineAtStartOfRace();
    
    @Statistic(messageKey="RelativeDistanceToAdvantageousEndOfLineAtStartOfRace", resultDecimals=2)
    Double getRelativeDistanceToAdvantageousEndOfLineAtStartOfRace();
    
    @Statistic(messageKey="RelativeDistanceToAdvantageousEndOfLineAtStartOfCompetitor", resultDecimals=2)
    Double getRelativeDistanceToAdvantageousEndOfLineAtStartOfCompetitor();
    
    @Statistic(messageKey="RaceDuration")
    Duration getDuration();
    
    /**
     * The time between the start of the race and the {@link #getCompetitor() competitor}'s first tack;
     * {@code null} if no tack is found for the competitor in the race.
     */
    @Statistic(messageKey="DurationFromRaceStartToFirstTack")
    Duration getDurationFromStartToFirstTack();
    
    @Statistic(messageKey="DistanceToStarboardSideAtStartOfRace", resultDecimals=2)
    public Double getRelativeDistanceToStarboardSideAtStartOfRace();

    @Statistic(messageKey="VMG5SecondsBeforeStartOfRace", resultDecimals=2)
    public Speed getVMG5SecondsBeforeStartOfRace();

    @Statistic(messageKey="VMGAtStartOfRace", resultDecimals=2)
    public Speed getVMGAtStartOfRace();

    @Statistic(messageKey="VMG5SecondsAfterStartOfRace", resultDecimals=2)
    public Speed getVMG5SecondsAfterStartOfRace();
    
    @Statistic(messageKey="DistanceToAdvantageousSideAtStartOfRaceVsRankAtFirstMark", resultDecimals=2)
    public Pair<Double, Integer> getRelativeDistanceToAdvantageousSideAtStartOfRaceVsRankAtFirstMark();

    @Statistic(messageKey="RankAtFirstMarkVsFinalRank", resultDecimals=2)
    public Pair<Integer, Integer> getRankAtFirstMarkVsFinalRank();
    
    @Statistic(messageKey="RankSixtySecondsAfterStart", resultDecimals=2)
    public Integer getRankSixtySecondsAfterStartOfRace();
    
    @Statistic(messageKey="RankNinetySecondsAfterStart", resultDecimals=2)
    public Integer getRankNinetySecondsAfterStartOfRace();
    
    @Statistic(messageKey="FinalRank", resultDecimals=2, ordinal=8)
    public Integer getFinalRank();
    
    @Statistic(messageKey="DistanceToAdvantageousSideAtStartOfRaceVsFinalRank", resultDecimals=2)
    Pair<Double, Integer> getRelativeDistanceToAdvantageousSideAtStartOfRaceVsFinalRank();
    
    @Statistic(messageKey="AverageRaceWindSpeed")
    Speed getAverageRaceWindSpeed();

    @Statistic(messageKey="RatioDurationLongVsShortTack", resultDecimals=2)
    public double getRatioDurationLongVsShortTack();
    
    @Statistic(messageKey="RatioDistanceLongVsShortTack", resultDecimals=2)
    public double getRatioDistanceLongVsShortTack();
}
