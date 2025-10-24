package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.annotations.Statistic;

public interface HasManeuverContext extends HasManeuver, HasTrackedLegOfCompetitor, HasWindOnTrackedLegOfCompetitor {
    @Connector(scanForStatistics = false)
    HasTrackedLegOfCompetitorContext getTrackedLegOfCompetitorContext();

    @Dimension(messageKey = "TackBeforeManeuver")
    Tack getTackBeforeManeuver();
    
    @Dimension(messageKey = "TypeOfPreviousManeuver")
    ManeuverType getTypeOfPreviousManeuver();
    
    @Dimension(messageKey = "TypeOfNextManeuver")
    ManeuverType getTypeOfNextManeuver();

    @Statistic(messageKey = "AbsoluteDirectionChange", resultDecimals = 2, ordinal = 3)
    Double getAbsoluteDirectionChangeInDegrees();

    @Statistic(messageKey = "ManeuverLoss", resultDecimals = 0, ordinal = 4)
    Distance getManeuverLossDistanceLost();
    
    @Statistic(messageKey = "WindSpeedVsManeuverLoss", resultDecimals = 2, ordinal = 4)
    Pair<Double, Double> getWindSpeedVsManeuverLoss();

    @Statistic(messageKey = "AbsTWAAtManeuverClimax", ordinal = 7)
    Double getAbsTWAAtManeuverClimax();

    @Statistic(messageKey = "ManeuverEnteringAbsTWA", ordinal = 5)
    Double getEnteringAbsTWA();

    @Statistic(messageKey = "ManeuverExitingAbsTWA", ordinal = 6)
    Double getExitingAbsTWA();

    @Statistic(messageKey = "ManeuverDurationInSeconds", ordinal = 8, resultDecimals = 2)
    Double getManeuverDurationInSeconds();

    @Statistic(messageKey = "EnteringManeuverSpeedMinusExitingSpeed", ordinal = 19, resultDecimals = 4)
    Double getEnteringManeuverSpeedMinusExitingSpeed();

    @Statistic(messageKey = "RatioBetweenManeuverEnteringAndExitingSpeed", ordinal = 20, resultDecimals = 4)
    Double getRatioBetweenManeuverEnteringAndExitingSpeed();

    @Statistic(messageKey = "ManeuverEnteringSpeedInKnots", ordinal = 17, resultDecimals = 2)
    Double getManeuverEnteringSpeed();

    @Statistic(messageKey = "ManeuverExitingSpeedInKnots", ordinal = 18, resultDecimals = 2)
    Double getManeuverExitingSpeed();

    @Statistic(messageKey = "DurationBetweenStableSpeedWithCourseAndMainCurveBeginningInSeconds", ordinal = 20, resultDecimals = 2)
    Double getDurationBetweenStableSpeedWithCourseAndMainCurveBeginningSeconds();

    @Statistic(messageKey = "SpeedRatioBetweenStableSpeedWithCourseAndMainCurveBeginning", ordinal = 21, resultDecimals = 2)
    Double getSpeedRatioBetweenStableSpeedWithCourseAndMainCurveBeginning();

    @Statistic(messageKey = "AbsCourseDifferenceBetweenStableSpeedWithCourseAndMainCurveBeginningInDegrees", ordinal = 22, resultDecimals = 2)
    Double getAbsCourseDifferenceBetweenStableSpeedWithCourseAndMainCurveBeginningInDegrees();

    @Statistic(messageKey = "DurationBetweenStableSpeedWithCourseAndMainCurveEndInSeconds", ordinal = 23, resultDecimals = 2)
    Double getDurationBetweenStableSpeedWithCourseAndMainCurveEndInSeconds();

    @Statistic(messageKey = "SpeedRatioBetweenStableSpeedWithCourseAndMainCurveEnd", ordinal = 24, resultDecimals = 2)
    Double getSpeedRatioBetweenStableSpeedWithCourseAndMainCurveEnd();

    @Statistic(messageKey = "AbsCourseDifferenceBetweenStableSpeedWithCourseAndMainCurveEndInDegrees", ordinal = 25, resultDecimals = 2)
    Double getAbsCourseDifferenceBetweenStableSpeedWithCourseAndMainCurveEndInDegrees();
    
    @Statistic(messageKey = "LowestSpeed", ordinal = 26, resultDecimals = 2)
    Speed getLowestSpeed();
    
    @Statistic(messageKey = "SpeedChange", ordinal = 27, resultDecimals = 2)
    Speed getSpeedDifference();
    
    @Statistic(messageKey = "MaximumTurningRate", ordinal = 28, resultDecimals = 2)
    double getMaximimumTurningRateInDegreesPerSecond();
    
    @Statistic(messageKey = "AverageTurningRate", ordinal = 29, resultDecimals = 2)
    double getAverageTurningRateInDegreesPerSecond();
    
    TimePoint getTimePointBeforeForAnalysis();

    TimePoint getTimePointAfterForAnalysis();

    double getDirectionChangeInDegreesForAnalysis();

}
