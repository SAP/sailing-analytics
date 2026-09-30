package com.sap.sailing.windestimation.data;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.Wind;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;

public class LabeledManeuverForEstimation extends ManeuverForEstimation {

    private final ManeuverTypeForClassification maneuverType;
    private final Wind wind;
    private final String regattaName;

    public LabeledManeuverForEstimation(TimePoint maneuverTimePoint, Position maneuverPosition, Bearing middleCourse,
            SpeedWithBearing speedWithBearingBefore, SpeedWithBearing speedWithBearingAfter,
            double courseChangeInDegrees, double courseChangeWithinMainCurveInDegrees,
            double maxTurningRateInDegreesPerSecond, Double deviationFromOptimalTackAngleInDegrees,
            Double deviationFromOptimalJibeAngleInDegrees, double speedLossRatio, double speedGainRatio,
            double lowestSpeedVsExitingSpeedRatio, boolean clean, ManeuverCategory maneuverCategory,
            double scaledSpeedBefore, double scaledSpeedAfter, boolean markPassing, BoatClass boatClass,
            boolean markPassingDataAvailable, ManeuverTypeForClassification maneuverType, Wind wind,
            String regattaName, String competitorName) {
        super(maneuverTimePoint, maneuverPosition, middleCourse, speedWithBearingBefore, speedWithBearingAfter,
                courseChangeInDegrees, courseChangeWithinMainCurveInDegrees, maxTurningRateInDegreesPerSecond,
                deviationFromOptimalTackAngleInDegrees, deviationFromOptimalJibeAngleInDegrees, speedLossRatio,
                speedGainRatio, lowestSpeedVsExitingSpeedRatio, clean, maneuverCategory, scaledSpeedBefore,
                scaledSpeedAfter, markPassing, boatClass, markPassingDataAvailable, competitorName);
        this.maneuverType = maneuverType;
        this.wind = wind;
        this.regattaName = regattaName;
    }

    public ManeuverTypeForClassification getManeuverType() {
        return maneuverType;
    }

    public Wind getWind() {
        return wind;
    }

    public String getRegattaName() {
        return regattaName;
    }

}
