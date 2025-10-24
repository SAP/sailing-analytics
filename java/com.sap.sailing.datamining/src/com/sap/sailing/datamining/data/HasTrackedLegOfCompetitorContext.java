package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.annotations.Statistic;
import com.sap.sse.datamining.shared.impl.dto.ClusterDTO;

/**
 * A "slice" of a competitor's tracked leg; slicing may happen in the retriever, perhaps influenced by some
 * configuration parameters, e.g., based on windward distance sailed, time spent, or rhumb line distance sailed.
 * A full leg is a special case of this.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface HasTrackedLegOfCompetitorContext extends HasSomethingOfCompetitorContextWithTrackedRaceContext {
    @Connector(scanForStatistics=false)
    HasTrackedLegContext getTrackedLegContext();
    
    default TrackedRace getTrackedRace() {
        return getTrackedLegContext().getTrackedRaceContext().getTrackedRace();
    }
    
    TrackedLegOfCompetitor getTrackedLegOfCompetitor();
    
    default HasTrackedRaceContext getTrackedRaceContext() {
        return getTrackedLegContext().getTrackedRaceContext();
    }
    
    @Dimension(messageKey="RelativeScoreInRaceInPercent", ordinal=12)
    ClusterDTO getPercentageClusterForRelativeScoreInRace();
    
    @Connector(messageKey="Boat")
    Boat getBoat();
    
    @Statistic(messageKey="DistanceTraveled", resultDecimals=2, ordinal=0)
    Distance getDistanceTraveled();
    
    @Statistic(messageKey="SpeedAverage", resultDecimals=2, ordinal=0)
    Double getSpeedAverageInKnots();
    
    @Statistic(messageKey="RankGainsOrLosses", resultDecimals=2, ordinal=1)
    Integer getRankGainsOrLosses();
    
    @Statistic(messageKey="RelativeScore", resultDecimals=2, ordinal=2)
    Double getRelativeRank();
    
    @Statistic(messageKey="AbsoluteRank", resultDecimals=0, ordinal=3)
    Integer getRankAtSliceFinish();
    
    @Statistic(messageKey="AbsoluteRankAfterFirstQuarter", resultDecimals=0, ordinal=4)
    Integer getRankAfterFirstQuarter();
    
    @Statistic(messageKey="AbsoluteRankAfterSecondQuarter", resultDecimals=0, ordinal=5)
    Integer getRankAfterSecondQuarter();
    
    @Statistic(messageKey="AbsoluteRankAfterThirdQuarter", resultDecimals=0, ordinal=6)
    Integer getRankAfterThirdQuarter();
    
    @Statistic(messageKey="AbsoluteRankAverageAcrossFirstThreeQuarters", resultDecimals=2, ordinal=7)
    Double getRankAverageAcrossFirstThreeQuarters();
    
    @Statistic(messageKey="AbsoluteRankRhumbLineBasedAfterFirstQuarter", resultDecimals=0, ordinal=8)
    Integer getRankRhumbLineBasedAfterFirstQuarter();
    
    @Statistic(messageKey="AbsoluteRankRhumbLineBasedAfterSecondQuarter", resultDecimals=0, ordinal=9)
    Integer getRankRhumbLineBasedAfterSecondQuarter();
    
    @Statistic(messageKey="AbsoluteRankRhumbLineBasedAfterThirdQuarter", resultDecimals=0, ordinal=10)
    Integer getRankRhumbLineBasedAfterThirdQuarter();
    
    @Statistic(messageKey="AbsoluteRankRhumbLineBasedAverageAcrossFirstThreeQuarters", resultDecimals=2, ordinal=11)
    Double getRankRhumbLineBasedAverageAcrossFirstThreeQuarters();
    
    @Statistic(messageKey="AbsoluteRankPredictionErrorAfterFirstQuarter", resultDecimals=2, ordinal=12)
    Double getRankPredictionErrorAfterFirstQuarter();
    
    @Statistic(messageKey="AbsoluteRankPredictionErrorAfterSecondQuarter", resultDecimals=2, ordinal=13)
    Double getRankPredictionErrorAfterSecondQuarter();
    
    @Statistic(messageKey="AbsoluteRankPredictionErrorAfterThirdQuarter", resultDecimals=2, ordinal=14)
    Double getRankPredictionErrorAfterThirdQuarter();
    
    @Statistic(messageKey="AbsoluteRankPredictionErrorAcrossFirstThreeQuarters", resultDecimals=2, ordinal=15)
    Double getRankPredictionErrorAcrossFirstThreeQuarters();
    
    @Statistic(messageKey="AbsoluteRankPredictionErrorRhumbLineBasedAfterFirstQuarter", resultDecimals=2, ordinal=16)
    Double getRankPredictionErrorRhumbLineBasedAfterFirstQuarter();
    
    @Statistic(messageKey="AbsoluteRankPredictionErrorRhumbLineBasedAfterSecondQuarter", resultDecimals=2, ordinal=17)
    Double getRankPredictionErrorRhumbLineBasedAfterSecondQuarter();
    
    @Statistic(messageKey="AbsoluteRankPredictionErrorRhumbLineBasedAfterThirdQuarter", resultDecimals=2, ordinal=18)
    Double getRankPredictionErrorRhumbLineBasedAfterThirdQuarter();
    
    @Statistic(messageKey="AbsoluteRankPredictionErrorRhumbLineBasedAcrossFirstThreeQuarters", resultDecimals=2, ordinal=19)
    Double getRankPredictionErrorRhumbLineBasedAcrossFirstThreeQuarters();
    
    @Statistic(messageKey="TimeSpentInSeconds", resultDecimals=0, ordinal=4)
    Long getTimeTakenInSeconds();

    @Statistic(messageKey="timeSpentFoiling", resultDecimals=1)
    Duration getTimeSpentFoiling();

    @Statistic(messageKey="FoilingDistance", resultDecimals=1)
    Distance getDistanceSpentFoiling();
    
    @Statistic(messageKey="NumberOfManeuvers", resultDecimals=0)
    Integer getNumberOfManeuvers();
    
    @Statistic(messageKey="NumberOfJibes", resultDecimals=0)
    Integer getNumberOfJibes();
    
    @Statistic(messageKey="NumberOfTacks", resultDecimals=0)
    Integer getNumberOfTacks();
    
    @Statistic(messageKey="VMG", resultDecimals=2)
    Speed getVelocityMadeGood();
    
    @Statistic(messageKey="RatioDurationLongVsShortTack", resultDecimals=2)
    double getRatioDurationLongVsShortTack();
    
    @Statistic(messageKey="RatioDistanceLongVsShortTack", resultDecimals=2)
    double getRatioDistanceLongVsShortTack();
}