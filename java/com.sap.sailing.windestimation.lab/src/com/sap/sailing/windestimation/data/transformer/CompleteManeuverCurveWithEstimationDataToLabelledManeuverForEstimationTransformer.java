package com.sap.sailing.windestimation.data.transformer;

import java.util.List;

import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.LabeledManeuverForEstimation;

public class CompleteManeuverCurveWithEstimationDataToLabelledManeuverForEstimationTransformer
        implements CompetitorTrackTransformer<CompleteManeuverCurveWithEstimationData, LabeledManeuverForEstimation> {

    private final LabeledManeuverForEstimationTransformer internalTransformer = new LabeledManeuverForEstimationTransformer();

    @Override
    public List<LabeledManeuverForEstimation> transformElements(
            CompetitorTrackWithEstimationData<CompleteManeuverCurveWithEstimationData> competitorTrackWithElementsToTransform) {
        List<ConvertableToLabeledManeuverForEstimation> convertableManeuvers = ConvertableManeuverForEstimationAdapterForCompleteManeuverCurveWithEstimationData
                .getConvertableManeuvers(competitorTrackWithElementsToTransform.getElements());
        return internalTransformer.getManeuversForEstimation(convertableManeuvers,
                competitorTrackWithElementsToTransform.getBoatClass(),
                competitorTrackWithElementsToTransform.getRegattaName(), competitorTrackWithElementsToTransform.getCompetitorName());
    }

}
