package com.sap.sailing.datamining.impl.data;

import com.sap.sailing.datamining.data.HasCompleteManeuverCurveWithEstimationDataContext;
import com.sap.sailing.datamining.data.HasRaceOfCompetitorContext;
import com.sap.sailing.datamining.shared.ManeuverSettings;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Duration;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.datamining.shared.impl.dto.ClusterDTO;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class CompleteManeuverCurveWithEstimationDataWithContext
        implements HasCompleteManeuverCurveWithEstimationDataContext {

    private final CompleteManeuverCurveWithEstimationData maneuverWithEstimationData;
    private final HasRaceOfCompetitorContext raceOfCompetitorContext;
    private final ManeuverSettings maneuverSettings;
    private final CompleteManeuverCurveWithEstimationData previousManeuverCurve;
    private final CompleteManeuverCurveWithEstimationData nextManeuverCurve;

    public CompleteManeuverCurveWithEstimationDataWithContext(HasRaceOfCompetitorContext raceOfCompetitorContext,
            CompleteManeuverCurveWithEstimationData maneuverWithEstimationData, ManeuverSettings maneuverSettings,
            CompleteManeuverCurveWithEstimationData previousManeuverCurve,
            CompleteManeuverCurveWithEstimationData nextManeuverCurve) {
        this.raceOfCompetitorContext = raceOfCompetitorContext;
        this.maneuverWithEstimationData = maneuverWithEstimationData;
        this.maneuverSettings = maneuverSettings;
        this.previousManeuverCurve = previousManeuverCurve;
        this.nextManeuverCurve = nextManeuverCurve;
    }

    @Override
    public HasRaceOfCompetitorContext getRaceOfCompetitorContext() {
        return raceOfCompetitorContext;
    }

    @Override
    public Double getManeuverStartSpeedDeviationRatioFromAvgStatistic() {
        SpeedWithBearing speedWithBearingBeforeManeuver = getManeuverCurveBoundariesForAnalysis()
                .getSpeedWithBearingBefore();
        SpeedWithBearing averageSpeedWithBearingBeforeManeuver = maneuverWithEstimationData
                .getCurveWithUnstableCourseAndSpeed().getAverageSpeedWithBearingBefore();
        if (speedWithBearingBeforeManeuver != null && averageSpeedWithBearingBeforeManeuver != null) {
            if (speedWithBearingBeforeManeuver.getKnots() < averageSpeedWithBearingBeforeManeuver.getKnots()) {
                return averageSpeedWithBearingBeforeManeuver.getKnots() / speedWithBearingBeforeManeuver.getKnots();
            } else {
                return speedWithBearingBeforeManeuver.getKnots() / averageSpeedWithBearingBeforeManeuver.getKnots();
            }
        }
        return null;
    }

    @Override
    public Double getManeuverStartCogDeviationFromAvgInDegreesStatistic() {
        SpeedWithBearing speedWithBearingBeforeManeuver = getManeuverCurveBoundariesForAnalysis()
                .getSpeedWithBearingBefore();
        SpeedWithBearing averageSpeedWithBearingBeforeManeuver = maneuverWithEstimationData
                .getCurveWithUnstableCourseAndSpeed().getAverageSpeedWithBearingBefore();
        if (speedWithBearingBeforeManeuver != null && averageSpeedWithBearingBeforeManeuver != null) {
            return Math.abs(averageSpeedWithBearingBeforeManeuver.getBearing()
                    .getDifferenceTo(speedWithBearingBeforeManeuver.getBearing()).getDegrees());
        }
        return null;
    }

    @Override
    public Double getManeuverEndSpeedDeviationRatioFromAvgStatistic() {
        SpeedWithBearing speedWithBearingAfterManeuver = getManeuverCurveBoundariesForAnalysis()
                .getSpeedWithBearingAfter();
        SpeedWithBearing averageSpeedWithBearingAfterManeuver = maneuverWithEstimationData
                .getCurveWithUnstableCourseAndSpeed().getAverageSpeedWithBearingAfter();
        if (speedWithBearingAfterManeuver != null && averageSpeedWithBearingAfterManeuver != null) {
            if (speedWithBearingAfterManeuver.getKnots() < averageSpeedWithBearingAfterManeuver.getKnots()) {
                return averageSpeedWithBearingAfterManeuver.getKnots() / speedWithBearingAfterManeuver.getKnots();
            } else {
                return speedWithBearingAfterManeuver.getKnots() / averageSpeedWithBearingAfterManeuver.getKnots();
            }
        }
        return null;
    }

    @Override
    public Double getManeuverEndCogDeviationFromAvgInDegreesStatistic() {
        SpeedWithBearing speedWithBearingAfterManeuver = maneuverWithEstimationData.getCurveWithUnstableCourseAndSpeed()
                .getSpeedWithBearingAfter();
        SpeedWithBearing averageSpeedWithBearingAfterManeuver = maneuverWithEstimationData
                .getCurveWithUnstableCourseAndSpeed().getAverageSpeedWithBearingAfter();
        if (speedWithBearingAfterManeuver != null && averageSpeedWithBearingAfterManeuver != null) {
            return Math.abs(averageSpeedWithBearingAfterManeuver.getBearing()
                    .getDifferenceTo(speedWithBearingAfterManeuver.getBearing()).getDegrees());
        }
        return null;
    }

    @Override
    public Double getDurationToNextManeuverInSecondsStatistic() {
        Duration duration = maneuverWithEstimationData.getCurveWithUnstableCourseAndSpeed()
                .getDurationFromManeuverEndToNextManeuverStart();
        if (duration != null) {
            return duration.asSeconds();
        }
        return null;
    }

    @Override
    public Double getDurationFromPreviousManeuverInSecondsStatistic() {
        Duration duration = maneuverWithEstimationData.getCurveWithUnstableCourseAndSpeed()
                .getDurationFromPreviousManeuverEndToManeuverStart();
        if (duration != null) {
            return duration.asSeconds();
        }
        return null;
    }

    public boolean isNextManeuverAtLeastOneSecondInFront() {
        return maneuverWithEstimationData.getCurveWithUnstableCourseAndSpeed()
                .getDurationFromManeuverEndToNextManeuverStart() != null
                && maneuverWithEstimationData.getCurveWithUnstableCourseAndSpeed()
                        .getDurationFromManeuverEndToNextManeuverStart().asSeconds() >= 1;
    }

    @Override
    public boolean isPreviousManeuverAtLeastOneSecondBehind() {
        return maneuverWithEstimationData.getCurveWithUnstableCourseAndSpeed()
                .getDurationFromPreviousManeuverEndToManeuverStart() != null
                && maneuverWithEstimationData.getCurveWithUnstableCourseAndSpeed()
                        .getDurationFromPreviousManeuverEndToManeuverStart().asSeconds() >= 1;
    }

    @Override
    public CompleteManeuverCurveWithEstimationData getCompleteManeuverCurveWithEstimationData() {
        return maneuverWithEstimationData;
    }

    @Override
    public ClusterDTO getJibingCount() {
        return new ClusterDTO(maneuverWithEstimationData.getJibingCount() + "");
    }

    @Override
    public ClusterDTO getTackingCount() {
        return new ClusterDTO(maneuverWithEstimationData.getTackingCount() + "");
    }

    @Override
    public double getAbsTwaAtMaxTurningRate() {
        return getAbsTwaFromCourse(maneuverWithEstimationData.getMainCurve().getCourseAtMaxTurningRate());
    }

    @Override
    public double getAbsTwaAtLowestSpeed() {
        return getAbsTwaFromCourse(maneuverWithEstimationData.getMainCurve().getLowestSpeed().getBearing());
    }

    @Override
    public double getAbsTwaAtHighestSpeed() {
        return getAbsTwaFromCourse(maneuverWithEstimationData.getMainCurve().getHighestSpeed().getBearing());
    }

    private double getAbsTwaFromCourse(Bearing course) {
        double twa = maneuverWithEstimationData.getWind().getFrom().getDifferenceTo(course).getDegrees();
        return Math.abs(twa);
    }

    @Override
    public NauticalSide getToSide() {
        return maneuverWithEstimationData.getMainCurve().getDirectionChangeInDegrees() < 0 ? NauticalSide.PORT
                : NauticalSide.STARBOARD;
    }

    private ManeuverCurveBoundaries getManeuverCurveBoundariesForAnalysis() {
        return maneuverSettings.isMainCurveAnalysis() ? maneuverWithEstimationData.getMainCurve()
                : maneuverWithEstimationData.getCurveWithUnstableCourseAndSpeed();
    }

    @Override
    public Double getEnteringAbsTWA() {
        return getAbsTwaFromCourse(getManeuverCurveBoundariesForAnalysis().getSpeedWithBearingBefore().getBearing());
    }

    @Override
    public Double getExitingAbsTWA() {
        return getAbsTwaFromCourse(getManeuverCurveBoundariesForAnalysis().getSpeedWithBearingAfter().getBearing());
    }

    @Override
    public ManeuverType getTypeOfPreviousManeuver() {
        return previousManeuverCurve == null ? ManeuverType.UNKNOWN
                : previousManeuverCurve.getManeuverTypeForCompleteManeuverCurve();
    }

    @Override
    public ManeuverType getTypeOfNextManeuver() {
        return nextManeuverCurve == null ? ManeuverType.UNKNOWN
                : nextManeuverCurve.getManeuverTypeForCompleteManeuverCurve();
    }

    @Override
    public Double getAbsoluteDirectionChangeInDegrees() {
        return Math.abs(getManeuverCurveBoundariesForAnalysis().getDirectionChangeInDegrees());
    }

    @Override
    public Double getRelativeBearingToNextMarkBeforeManeuver() {
        return maneuverWithEstimationData.getRelativeBearingToNextMarkBeforeManeuver() == null ? null
                : maneuverWithEstimationData.getRelativeBearingToNextMarkBeforeManeuver().getDegrees();
    }

    @Override
    public Double getRelativeBearingToNextMarkAfterManeuver() {
        return maneuverWithEstimationData.getRelativeBearingToNextMarkAfterManeuver() == null ? null
                : maneuverWithEstimationData.getRelativeBearingToNextMarkAfterManeuver().getDegrees();
    }

    public TimePoint getTimePointBeforeForAnalysis() {
        return getManeuverCurveBoundariesForAnalysis().getTimePointBefore();
    }

    public TimePoint getTimePointAfterForAnalysis() {
        return getManeuverCurveBoundariesForAnalysis().getTimePointAfter();
    }

    @Override
    public Double getManeuverEnteringSpeed() {
        return getManeuverCurveBoundariesForAnalysis().getSpeedWithBearingBefore().getKnots();
    }

    @Override
    public Double getManeuverExitingSpeed() {
        return getManeuverCurveBoundariesForAnalysis().getSpeedWithBearingAfter().getKnots();
    }

    @Override
    public double getAbsTwaAtManeuverMiddle() {
        ManeuverCurveBoundaries boundariesForAnalysis = getManeuverCurveBoundariesForAnalysis();
        Bearing middleCourse = boundariesForAnalysis.getSpeedWithBearingBefore().getBearing()
                .add(new DegreeBearingImpl(boundariesForAnalysis.getDirectionChangeInDegrees() / 2));
        return getAbsTwaFromCourse(middleCourse);
    }

    @Override
    public double getGpsSamplingRate() {
        return maneuverSettings.isMainCurveAnalysis()
                ? maneuverWithEstimationData.getMainCurve().getGpsFixesCount()
                        / maneuverWithEstimationData.getMainCurve().getDuration().asSeconds()
                : maneuverWithEstimationData.getCurveWithUnstableCourseAndSpeed().getGpsFixesCount()
                        / maneuverWithEstimationData.getCurveWithUnstableCourseAndSpeed().getDuration().asSeconds();
    }

    @Override
    public Double getAbsRelativeBearingToNextMarkBeforeManeuver() {
        Double bearing = getRelativeBearingToNextMarkBeforeManeuver();
        return bearing == null ? null : Math.abs(bearing);
    }

    @Override
    public Double getAbsRelativeBearingToNextMarkAfterManeuver() {
        Double bearing = getRelativeBearingToNextMarkAfterManeuver();
        return bearing == null ? null : Math.abs(bearing);
    }

}
