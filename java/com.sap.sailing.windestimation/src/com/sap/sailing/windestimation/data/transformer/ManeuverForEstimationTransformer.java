package com.sap.sailing.windestimation.data.transformer;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.LabelledManeuverForEstimation;
import com.sap.sailing.windestimation.data.ManeuverCategory;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.maneuverclassifier.ManeuverTypeForClassification;

public class ManeuverForEstimationTransformer
        extends AbstractCompleteManeuverCurveWithEstimationDataTransformer<ManeuverForEstimation> {

    @Override
    public List<ManeuverForEstimation> transformElements(
            CompetitorTrackWithEstimationData<CompleteManeuverCurveWithEstimationData> competitorTrackWithElementsToTransform) {
        double speedScalingDivisor = getSpeedScalingDivisor(competitorTrackWithElementsToTransform.getElements());
        List<ManeuverForEstimation> maneuversForEstimation = new ArrayList<>();
        CompleteManeuverCurveWithEstimationData previousManeuver = null;
        CompleteManeuverCurveWithEstimationData maneuver = null;
        for (CompleteManeuverCurveWithEstimationData nextManeuver : competitorTrackWithElementsToTransform
                .getElements()) {
            if (maneuver != null) {
                ManeuverForEstimation maneuverForEstimation = getManeuverForEstimation(maneuver, previousManeuver,
                        nextManeuver, speedScalingDivisor, competitorTrackWithElementsToTransform.getBoatClass(), "");
                if (maneuverForEstimation != null) {
                    maneuversForEstimation.add(maneuverForEstimation);
                }
            }
            previousManeuver = maneuver;
            maneuver = nextManeuver;
        }
        if (maneuver != null) {
            ManeuverForEstimation maneuverForEstimation = getManeuverForEstimation(maneuver, previousManeuver, null,
                    speedScalingDivisor, competitorTrackWithElementsToTransform.getBoatClass(), "");
            if (maneuverForEstimation != null) {
                maneuversForEstimation.add(maneuverForEstimation);
            }
        }
        return maneuversForEstimation;
    }

    private ManeuverForEstimation getManeuverForEstimation(CompleteManeuverCurveWithEstimationData maneuver,
            CompleteManeuverCurveWithEstimationData previousManeuver,
            CompleteManeuverCurveWithEstimationData nextManeuver, double speedScalingDivisor, BoatClass boatClass, String regattaName) {
        ManeuverCategory maneuverCategory = getManeuverCategory(maneuver);
        ManeuverTypeForClassification maneuverType = getManeuverTypeForClassification(maneuver, maneuverCategory);
        double speedLossRatio = maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingBefore().getKnots() > 0
                ? maneuver.getCurveWithUnstableCourseAndSpeed().getLowestSpeed().getKnots()
                        / maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingBefore().getKnots()
                : 0;
        double speedGainRatio = maneuver.getMainCurve().getHighestSpeed().getKnots() > 0
                ? maneuver.getMainCurve().getSpeedWithBearingBefore().getKnots()
                        / maneuver.getMainCurve().getHighestSpeed().getKnots()
                : 0;
        double lowestSpeedVsExitingSpeedRatio = maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingAfter()
                .getKnots() > 0
                        ? maneuver.getCurveWithUnstableCourseAndSpeed().getLowestSpeed().getKnots()
                                / maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingAfter().getKnots()
                        : 0;
        Double deviationFromOptimalTackAngleInDegrees = maneuver.getTargetTackAngleInDegrees() == null ? null
                : Math.abs(maneuver.getCurveWithUnstableCourseAndSpeed().getDirectionChangeInDegrees())
                        - maneuver.getTargetTackAngleInDegrees();
        Double deviationFromOptimalJibeAngleInDegrees = maneuver.getTargetJibeAngleInDegrees() == null ? null
                : Math.abs(maneuver.getCurveWithUnstableCourseAndSpeed().getDirectionChangeInDegrees())
                        - maneuver.getTargetJibeAngleInDegrees();
        boolean clean = isManeuverClean(maneuver, previousManeuver, nextManeuver);
        double scaledSpeedBeforeInKnots = maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingBefore()
                .getKnots() / speedScalingDivisor;
        double scaledSpeedAfterInKnots = maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingAfter()
                .getKnots() / speedScalingDivisor;
        boolean cleanBefore = isSegmentBetweenManeuversEligibleForPolarsCollection(previousManeuver, maneuver);
        boolean cleanAfter = isSegmentBetweenManeuversEligibleForPolarsCollection(maneuver, nextManeuver);
        ManeuverForEstimation maneuverForEstimation;
        Double relativeBearingToNextMarkBeforeManeuverInDegrees = maneuver
                .getRelativeBearingToNextMarkBeforeManeuver() == null ? null
                        : maneuver.getRelativeBearingToNextMarkBeforeManeuver().getDegrees();
        Double relativeBearingToNextMarkAfterManeuverInDegrees = maneuver
                .getRelativeBearingToNextMarkAfterManeuver() == null ? null
                        : maneuver.getRelativeBearingToNextMarkAfterManeuver().getDegrees();
        if (maneuver.getWind() == null) {
            maneuverForEstimation = new ManeuverForEstimation(maneuver.getTimePoint(), maneuver.getPosition(),
                    maneuver.getCurveWithUnstableCourseAndSpeed().getMiddleCourse(),
                    maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingBefore(),
                    maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingAfter(),
                    maneuver.getMainCurve().getLowestSpeed().getBearing(),
                    maneuver.getCurveWithUnstableCourseAndSpeed().getAverageSpeedWithBearingBefore(),
                    maneuver.getCurveWithUnstableCourseAndSpeed().getAverageSpeedWithBearingAfter(),
                    maneuver.getCurveWithUnstableCourseAndSpeed().getDirectionChangeInDegrees(),
                    maneuver.getMainCurve().getDirectionChangeInDegrees(),
                    maneuver.getMainCurve().getMaxTurningRateInDegreesPerSecond(),
                    deviationFromOptimalTackAngleInDegrees, deviationFromOptimalJibeAngleInDegrees, speedLossRatio,
                    speedGainRatio, lowestSpeedVsExitingSpeedRatio, clean, cleanBefore, cleanAfter, maneuverCategory,
                    scaledSpeedBeforeInKnots, scaledSpeedAfterInKnots, boatClass, maneuver.isMarkPassing(),
                    relativeBearingToNextMarkBeforeManeuverInDegrees, relativeBearingToNextMarkAfterManeuverInDegrees, regattaName);
        } else {
            maneuverForEstimation = new LabelledManeuverForEstimation(maneuver.getTimePoint(), maneuver.getPosition(),
                    maneuver.getCurveWithUnstableCourseAndSpeed().getMiddleCourse(),
                    maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingBefore(),
                    maneuver.getCurveWithUnstableCourseAndSpeed().getSpeedWithBearingAfter(),
                    maneuver.getMainCurve().getLowestSpeed().getBearing(),
                    maneuver.getCurveWithUnstableCourseAndSpeed().getAverageSpeedWithBearingBefore(),
                    maneuver.getCurveWithUnstableCourseAndSpeed().getAverageSpeedWithBearingAfter(),
                    maneuver.getCurveWithUnstableCourseAndSpeed().getDirectionChangeInDegrees(),
                    maneuver.getMainCurve().getDirectionChangeInDegrees(),
                    maneuver.getMainCurve().getMaxTurningRateInDegreesPerSecond(),
                    deviationFromOptimalTackAngleInDegrees, deviationFromOptimalJibeAngleInDegrees, speedLossRatio,
                    speedGainRatio, lowestSpeedVsExitingSpeedRatio, clean, cleanBefore, cleanAfter, maneuverCategory,
                    scaledSpeedBeforeInKnots, scaledSpeedAfterInKnots, boatClass, maneuver.isMarkPassing(),
                    relativeBearingToNextMarkBeforeManeuverInDegrees, relativeBearingToNextMarkAfterManeuverInDegrees, regattaName,
                    maneuverType, maneuver.getWind());
        }
        return maneuverForEstimation;
    }

    protected ManeuverTypeForClassification getManeuverTypeForClassification(
            CompleteManeuverCurveWithEstimationData maneuver, ManeuverCategory maneuverCategory) {
        switch (maneuverCategory) {
        case _180:
        case _360:
        case SMALL:
        case WIDE:
            return ManeuverTypeForClassification.OTHER;
        case MARK_PASSING:
        case REGULAR:
            ManeuverType maneuverType = maneuver.getManeuverTypeForCompleteManeuverCurve();
            switch (maneuverType) {
            case BEAR_AWAY:
            case HEAD_UP:
            case PENALTY_CIRCLE:
            case UNKNOWN:
                return ManeuverTypeForClassification.OTHER;
            case JIBE:
                return ManeuverTypeForClassification.JIBE;
            case TACK:
                return ManeuverTypeForClassification.TACK;
            }
        }
        throw new IllegalStateException();
    }

}
