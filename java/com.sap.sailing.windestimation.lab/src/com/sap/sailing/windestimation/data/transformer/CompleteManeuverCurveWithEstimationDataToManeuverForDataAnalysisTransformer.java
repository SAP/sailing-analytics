package com.sap.sailing.windestimation.data.transformer;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.ManeuverCategory;
import com.sap.sailing.windestimation.data.ManeuverForDataAnalysis;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.ManeuverTypeForDataAnalysis;

public class CompleteManeuverCurveWithEstimationDataToManeuverForDataAnalysisTransformer
        implements CompetitorTrackTransformer<CompleteManeuverCurveWithEstimationData, ManeuverForDataAnalysis> {

    private final ManeuverForEstimationTransformer internalTransformer = new ManeuverForEstimationTransformer();

    @Override
    public List<ManeuverForDataAnalysis> transformElements(
            CompetitorTrackWithEstimationData<CompleteManeuverCurveWithEstimationData> competitorTrackWithElementsToTransform) {
        List<ConvertableToLabeledManeuverForEstimation> convertableManeuvers = ConvertableManeuverForEstimationAdapterForCompleteManeuverCurveWithEstimationData
                .getConvertableManeuvers(competitorTrackWithElementsToTransform.getElements());
        double speedScalingDivisor = internalTransformer.getSpeedScalingDivisor(convertableManeuvers);
        List<ManeuverForDataAnalysis> maneuversForClassification = new ArrayList<>();
        CompleteManeuverCurveWithEstimationData previousManeuver = null;
        CompleteManeuverCurveWithEstimationData maneuver = null;
        for (CompleteManeuverCurveWithEstimationData nextManeuver : competitorTrackWithElementsToTransform
                .getElements()) {
            if (maneuver != null) {
                ManeuverForDataAnalysis maneuverForClassification = getManeuverForDataAnalysis(maneuver,
                        previousManeuver, nextManeuver, speedScalingDivisor,
                        competitorTrackWithElementsToTransform.getBoatClass(), competitorTrackWithElementsToTransform.getCompetitorName());
                if (maneuverForClassification != null) {
                    maneuversForClassification.add(maneuverForClassification);
                }
            }
            previousManeuver = maneuver;
            maneuver = nextManeuver;
        }
        if (maneuver != null) {
            ManeuverForDataAnalysis maneuverForClassification = getManeuverForDataAnalysis(maneuver, previousManeuver,
                    null, speedScalingDivisor, competitorTrackWithElementsToTransform.getBoatClass(), competitorTrackWithElementsToTransform.getCompetitorName());
            if (maneuverForClassification != null) {
                maneuversForClassification.add(maneuverForClassification);
            }
        }
        return maneuversForClassification;
    }

    private ManeuverForDataAnalysis getManeuverForDataAnalysis(CompleteManeuverCurveWithEstimationData maneuver,
            CompleteManeuverCurveWithEstimationData previousManeuver,
            CompleteManeuverCurveWithEstimationData nextManeuver, double speedScalingDivisor, BoatClass boatClass, String competitorName) {
        if (maneuver.getWind() == null) {
            return null;
        }
        Double courseChangeInDegreesWithinTurningSectionOfPreviousManeuver = previousManeuver == null ? null
                : previousManeuver.getMainCurve().getDirectionChangeInDegrees();
        Double courseChangeInDegreesWithinTurningSectionOfNextManeuver = nextManeuver == null ? null
                : nextManeuver.getMainCurve().getDirectionChangeInDegrees();
        ManeuverForEstimation maneuverForEstimation = internalTransformer
                .getManeuverForEstimation(
                        new ConvertableManeuverForEstimationAdapterForCompleteManeuverCurveWithEstimationData(maneuver,
                                courseChangeInDegreesWithinTurningSectionOfPreviousManeuver,
                                courseChangeInDegreesWithinTurningSectionOfNextManeuver),
                        speedScalingDivisor, boatClass, competitorName);
        ManeuverTypeForDataAnalysis maneuverType = getManeuverTypeForDataAnalysis(maneuver);
        double absoluteTotalCourseChangeInDegrees = Math.abs(maneuverForEstimation.getCourseChangeInDegrees());
        double absoluteTotalCourseChangeWithinMainCurveInDegrees = Math
                .abs(maneuverForEstimation.getCourseChangeWithinMainCurveInDegrees());
        double speedInSpeedOutRatio = maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingAfter()
                .getKnots() < 0.1 ? 0
                        : maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingBefore().getKnots()
                                / maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingAfter().getKnots();
        double oversteeringInDegrees = Math
                .abs(maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingAfter().getBearing()
                        .getDifferenceTo(maneuver.getMainCurve().getSpeedWithBearingAfter().getBearing()).getDegrees());
        double speedLossRatio = maneuverForEstimation.getSpeedLossRatio();
        double speedGainRatio = maneuverForEstimation.getSpeedGainRatio();
        double lowestSpeedVsExitingSpeedRatio = maneuverForEstimation.getLowestSpeedVsExitingSpeedRatio();
        Double deviationFromOptimalTackAngleInDegrees = maneuverForEstimation
                .getDeviationFromOptimalTackAngleInDegrees();
        Double deviationFromOptimalJibeAngleInDegrees = maneuverForEstimation
                .getDeviationFromOptimalJibeAngleInDegrees();
        Double relativeBearingToNextMarkBeforeManeuverInDegrees = maneuver
                .getRelativeBearingToNextMarkBeforeManeuver() == null ? null
                        : maneuver.getRelativeBearingToNextMarkBeforeManeuver().getDegrees();
        Double relativeBearingToNextMarkAfterManeuverInDegrees = maneuver
                .getRelativeBearingToNextMarkAfterManeuver() == null ? null
                        : maneuver.getRelativeBearingToNextMarkAfterManeuver().getDegrees();
        double mainCurveDurationInSeconds = maneuver.getMainCurve().getDuration().asSeconds();
        double maneuverDurationInSeconds = maneuver.getCurveWithUnstableCourseAndSpeed().getDuration().asSeconds();
        double recoveryPhaseDurationInSeconds = maneuver.getMainCurve().getTimePointAfter()
                .until(maneuver.getCurveWithUnstableCourseAndSpeed().getTimePointAfter()).asSeconds();
        boolean clean = maneuverForEstimation.isClean();
        ManeuverCategory maneuverCategory = maneuverForEstimation.getManeuverCategory();
        double twaBeforeInDegrees = maneuver.getWind().getFrom()
                .getDifferenceTo(maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingBefore().getBearing())
                .getDegrees();
        double twaAfterInDegrees = maneuver.getWind().getFrom()
                .getDifferenceTo(maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingAfter().getBearing())
                .getDegrees();
        double twsInKnots = maneuver.getWind().getKnots();
        double speedBeforeInKnots = maneuverForEstimation.getSpeedWithBearingBefore().getKnots();
        double speedAfterInKnots = maneuverForEstimation.getSpeedWithBearingAfter().getKnots();
        double twaAtMiddleCourseInDegrees = maneuver.getWind().getFrom()
                .getDifferenceTo(maneuver.getCurveWithUnstableCourseAndSpeed().getMiddleCourse()).getDegrees();
        double twaAtMiddleCourseMainCurveInDegrees = maneuver.getWind().getFrom()
                .getDifferenceTo(maneuver.getMainCurve().getMiddleCourse()).getDegrees();
        double twaAtLowestSpeedInDegrees = maneuver.getWind().getFrom()
                .getDifferenceTo(maneuver.getMainCurve().getLowestSpeed().getBearing()).getDegrees();
        double twaAtMaxTurningRateInDegrees = maneuver.getWind().getFrom()
                .getDifferenceTo(maneuver.getMainCurve().getCourseAtMaxTurningRate()).getDegrees();
        boolean starboardManeuver = maneuverForEstimation.getCourseChangeWithinMainCurveInDegrees() > 0;
        ManeuverForDataAnalysis maneuverForClassification = new ManeuverForDataAnalysis(maneuverType,
                absoluteTotalCourseChangeInDegrees, absoluteTotalCourseChangeWithinMainCurveInDegrees,
                speedInSpeedOutRatio, oversteeringInDegrees, speedLossRatio, speedGainRatio,
                lowestSpeedVsExitingSpeedRatio, maneuverForEstimation.getMaxTurningRateInDegreesPerSecond(),
                deviationFromOptimalTackAngleInDegrees, deviationFromOptimalJibeAngleInDegrees,
                relativeBearingToNextMarkBeforeManeuverInDegrees, relativeBearingToNextMarkAfterManeuverInDegrees,
                mainCurveDurationInSeconds, maneuverDurationInSeconds, recoveryPhaseDurationInSeconds, clean,
                maneuverCategory, twaBeforeInDegrees, twaAfterInDegrees, twsInKnots, speedBeforeInKnots,
                speedAfterInKnots, twaAtMiddleCourseInDegrees, twaAtMiddleCourseMainCurveInDegrees,
                twaAtLowestSpeedInDegrees, twaAtMaxTurningRateInDegrees, starboardManeuver,
                speedBeforeInKnots / speedScalingDivisor, speedAfterInKnots / speedScalingDivisor,
                maneuver.isMarkPassing());
        return maneuverForClassification;
    }

    protected ManeuverTypeForDataAnalysis getManeuverTypeForDataAnalysis(
            CompleteManeuverCurveWithEstimationData maneuver) {
        ManeuverType maneuverType = maneuver.getManeuverTypeForCompleteManeuverCurve();
        switch (maneuverType) {
        case BEAR_AWAY:
            return ManeuverTypeForDataAnalysis.BEAR_AWAY;
        case HEAD_UP:
            return ManeuverTypeForDataAnalysis.HEAD_UP;
        case PENALTY_CIRCLE:
            return ManeuverTypeForDataAnalysis._360;
        case UNKNOWN:
            return null;
        case JIBE:
            return Math.abs(maneuver.getMainCurve().getDirectionChangeInDegrees()) > 120
                    ? ManeuverTypeForDataAnalysis._180_JIBE
                    : ManeuverTypeForDataAnalysis.JIBE;
        case TACK:
            return Math.abs(maneuver.getMainCurve().getDirectionChangeInDegrees()) > 120
                    ? ManeuverTypeForDataAnalysis._180_TACK
                    : ManeuverTypeForDataAnalysis.TACK;
        }
        throw new IllegalStateException();
    }

}
