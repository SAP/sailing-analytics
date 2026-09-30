package com.sap.sailing.windestimation.data.transformer;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.LabeledManeuverForEstimation;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.ManeuverTypeForClassification;

public class LabeledManeuverForEstimationTransformer implements
        CompetitorTrackTransformer<ConvertableToLabeledManeuverForEstimation, LabeledManeuverForEstimation> {

    private final ManeuverForEstimationTransformer internalTransformer = new ManeuverForEstimationTransformer();

    public LabeledManeuverForEstimation getManeuverForEstimation(ConvertableToLabeledManeuverForEstimation maneuver,
            ConvertableToLabeledManeuverForEstimation previousManeuver,
            ConvertableToLabeledManeuverForEstimation nextManeuver, double speedScalingDivisor, BoatClass boatClass,
            String regattaName, String competitorName) {
        ManeuverForEstimation maneuverForEstimation = internalTransformer.getManeuverForEstimation(maneuver,
                speedScalingDivisor, boatClass, competitorName);
        ManeuverTypeForClassification maneuverType = getManeuverTypeForClassification(maneuver);
        LabeledManeuverForEstimation labelledManeuverForEstimation = new LabeledManeuverForEstimation(
                maneuverForEstimation.getManeuverTimePoint(), maneuverForEstimation.getManeuverPosition(),
                maneuverForEstimation.getMiddleCourse(), maneuverForEstimation.getSpeedWithBearingBefore(),
                maneuverForEstimation.getSpeedWithBearingAfter(), maneuverForEstimation.getCourseChangeInDegrees(),
                maneuverForEstimation.getCourseChangeWithinMainCurveInDegrees(),
                maneuverForEstimation.getMaxTurningRateInDegreesPerSecond(),
                maneuverForEstimation.getDeviationFromOptimalTackAngleInDegrees(),
                maneuverForEstimation.getDeviationFromOptimalJibeAngleInDegrees(),
                maneuverForEstimation.getSpeedLossRatio(), maneuverForEstimation.getSpeedGainRatio(),
                maneuverForEstimation.getLowestSpeedVsExitingSpeedRatio(), maneuverForEstimation.isClean(),
                maneuverForEstimation.getManeuverCategory(), maneuverForEstimation.getScaledSpeedBefore(),
                maneuverForEstimation.getScaledSpeedAfter(), maneuverForEstimation.isMarkPassing(),
                maneuverForEstimation.getBoatClass(), maneuverForEstimation.isMarkPassingDataAvailable(), maneuverType,
                maneuver.getWind(), regattaName, competitorName);
        return labelledManeuverForEstimation;
    }

    protected ManeuverTypeForClassification getManeuverTypeForClassification(
            ConvertableToLabeledManeuverForEstimation maneuver) {
        ManeuverType maneuverType = maneuver.getManeuverTypeForCompleteManeuverCurve();
        switch (maneuverType) {
        case BEAR_AWAY:
            return ManeuverTypeForClassification.BEAR_AWAY;
        case HEAD_UP:
            return ManeuverTypeForClassification.HEAD_UP;
        case PENALTY_CIRCLE:
        case UNKNOWN:
            return null;
        case JIBE:
            return ManeuverTypeForClassification.JIBE;
        case TACK:
            return ManeuverTypeForClassification.TACK;
        }
        throw new IllegalStateException();
    }

    public List<LabeledManeuverForEstimation> getManeuversForEstimation(
            List<ConvertableToLabeledManeuverForEstimation> convertableManeuvers, BoatClass boatClass,
            String regattaName, String competitorName) {
        double speedScalingDivisor = internalTransformer.getSpeedScalingDivisor(convertableManeuvers);
        List<LabeledManeuverForEstimation> maneuversForEstimation = new ArrayList<>();
        ConvertableToLabeledManeuverForEstimation previousManeuver = null;
        ConvertableToLabeledManeuverForEstimation maneuver = null;
        for (ConvertableToLabeledManeuverForEstimation nextManeuver : convertableManeuvers) {
            if (maneuver != null) {
                LabeledManeuverForEstimation maneuverForEstimation = getManeuverForEstimation(maneuver,
                        previousManeuver, nextManeuver, speedScalingDivisor, boatClass, regattaName, competitorName);
                if (maneuverForEstimation != null) {
                    maneuversForEstimation.add(maneuverForEstimation);
                }
            }
            previousManeuver = maneuver;
            maneuver = nextManeuver;
        }
        if (maneuver != null) {
            LabeledManeuverForEstimation maneuverForEstimation = getManeuverForEstimation(maneuver, previousManeuver,
                    null, speedScalingDivisor, boatClass, regattaName, competitorName);
            if (maneuverForEstimation != null) {
                maneuversForEstimation.add(maneuverForEstimation);
            }
        }
        return maneuversForEstimation;
    }

    @Override
    public List<LabeledManeuverForEstimation> transformElements(
            CompetitorTrackWithEstimationData<ConvertableToLabeledManeuverForEstimation> competitorTrackWithElementsToTransform) {
        return getManeuversForEstimation(competitorTrackWithElementsToTransform.getElements(),
                competitorTrackWithElementsToTransform.getBoatClass(),
                competitorTrackWithElementsToTransform.getRegattaName(), competitorTrackWithElementsToTransform.getCompetitorName());
    }

}
