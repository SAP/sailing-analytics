package com.sap.sailing.windestimation.data;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;

/**
 * Maneuver class which is used in context of wind estimation for maneuver classification and further aggregation. This
 * class contains all the features which are required by maneuver classifier models to estimate the corresponding
 * maneuver type without knowledge about TWD.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class ManeuverForEstimation extends SimpleManeuverForEstimationImpl {

    private final double courseChangeInDegrees;
    private final double courseChangeWithinMainCurveInDegrees;
    private final double maxTurningRateInDegreesPerSecond;
    private final Double deviationFromOptimalTackAngleInDegrees;
    private final Double deviationFromOptimalJibeAngleInDegrees;
    private final double speedLossRatio;
    private final double speedGainRatio;
    private final double lowestSpeedVsExitingSpeedRatio;
    private final ManeuverCategory maneuverCategory;
    private final double scaledSpeedBefore;
    private final double scaledSpeedAfter;
    private final boolean markPassing;
    private final BoatClass boatClass;
    private final boolean markPassingDataAvailable;
    private final String competitorName;

    public ManeuverForEstimation(TimePoint maneuverTimePoint, Position maneuverPosition, Bearing middleCourse,
            SpeedWithBearing speedWithBearingBefore, SpeedWithBearing speedWithBearingAfter,
            double courseChangeInDegrees, double courseChangeWithinMainCurveInDegrees,
            double maxTurningRateInDegreesPerSecond, Double deviationFromOptimalTackAngleInDegrees,
            Double deviationFromOptimalJibeAngleInDegrees, double speedLossRatio, double speedGainRatio,
            double lowestSpeedVsExitingSpeedRatio, boolean clean, ManeuverCategory maneuverCategory,
            double scaledSpeedBefore, double scaledSpeedAfter, boolean markPassing, BoatClass boatClass,
            boolean markPassingDataAvailable, String competitorName) {
        super(maneuverTimePoint, maneuverPosition, middleCourse, speedWithBearingBefore, speedWithBearingAfter, clean, boatClass);
        this.courseChangeInDegrees = courseChangeInDegrees;
        this.courseChangeWithinMainCurveInDegrees = courseChangeWithinMainCurveInDegrees;
        this.maxTurningRateInDegreesPerSecond = maxTurningRateInDegreesPerSecond;
        this.deviationFromOptimalTackAngleInDegrees = deviationFromOptimalTackAngleInDegrees;
        this.deviationFromOptimalJibeAngleInDegrees = deviationFromOptimalJibeAngleInDegrees;
        this.speedLossRatio = speedLossRatio;
        this.speedGainRatio = speedGainRatio;
        this.lowestSpeedVsExitingSpeedRatio = lowestSpeedVsExitingSpeedRatio;
        this.maneuverCategory = maneuverCategory;
        this.scaledSpeedBefore = scaledSpeedBefore;
        this.scaledSpeedAfter = scaledSpeedAfter;
        this.markPassing = markPassing;
        this.boatClass = boatClass;
        this.markPassingDataAvailable = markPassingDataAvailable;
        this.competitorName = competitorName;
    }

    public double getCourseChangeInDegrees() {
        return courseChangeInDegrees;
    }

    public double getCourseChangeWithinMainCurveInDegrees() {
        return courseChangeWithinMainCurveInDegrees;
    }

    public double getMaxTurningRateInDegreesPerSecond() {
        return maxTurningRateInDegreesPerSecond;
    }

    public Double getDeviationFromOptimalTackAngleInDegrees() {
        return deviationFromOptimalTackAngleInDegrees;
    }

    public Double getDeviationFromOptimalJibeAngleInDegrees() {
        return deviationFromOptimalJibeAngleInDegrees;
    }

    public double getSpeedLossRatio() {
        return speedLossRatio;
    }

    public double getSpeedGainRatio() {
        return speedGainRatio;
    }

    public double getLowestSpeedVsExitingSpeedRatio() {
        return lowestSpeedVsExitingSpeedRatio;
    }

    public ManeuverCategory getManeuverCategory() {
        return maneuverCategory;
    }

    public double getScaledSpeedBefore() {
        return scaledSpeedBefore;
    }

    public double getScaledSpeedAfter() {
        return scaledSpeedAfter;
    }

    public boolean isMarkPassing() {
        return markPassing;
    }

    public BoatClass getBoatClass() {
        return boatClass;
    }

    public boolean isMarkPassingDataAvailable() {
        return markPassingDataAvailable;
    }

    public String getCompetitorName() {
        return competitorName;
    }

    @Override
    public String toString() {
        return "Maneuver at " + getManeuverTimePoint() + ", "
                + getManeuverPosition() + ", middleCourse=" + getMiddleCourse() + ", courseChangeInDegrees=" + courseChangeInDegrees;
    }
}
