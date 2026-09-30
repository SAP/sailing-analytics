package com.sap.sailing.windestimation.data.transformer;

import java.util.List;

import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;

/**
 * Converts competitor tracks with of type {@link CompleteManeuverCurveWithEstimationData} to competitor tracks with
 * instances of type {@link ManeuverForEstimation} instances.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class CompleteManeuverCurveWithEstimationDataToManeuverForEstimationTransformer
        implements CompetitorTrackTransformer<CompleteManeuverCurveWithEstimationData, ManeuverForEstimation> {

    private final ManeuverForEstimationTransformer internalTransformer = new ManeuverForEstimationTransformer();

    @Override
    public List<ManeuverForEstimation> transformElements(
            CompetitorTrackWithEstimationData<CompleteManeuverCurveWithEstimationData> competitorTrackWithElementsToTransform) {
        List<ConvertableToLabeledManeuverForEstimation> convertableManeuvers = ConvertableManeuverForEstimationAdapterForCompleteManeuverCurveWithEstimationData
                .getConvertableManeuvers(competitorTrackWithElementsToTransform.getElements());
        return internalTransformer.getManeuversForEstimation(convertableManeuvers,
                competitorTrackWithElementsToTransform.getBoatClass(), competitorTrackWithElementsToTransform.getCompetitorName());
    }
}
