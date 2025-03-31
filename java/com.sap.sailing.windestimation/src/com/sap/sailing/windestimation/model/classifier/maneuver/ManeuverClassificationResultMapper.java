package com.sap.sailing.windestimation.model.classifier.maneuver;

import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.model.classifier.ClassificationResultMapper;

/**
 * Maps the maneuver classifications to {@link ManeuverWithProbabilisticTypeClassification}.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class ManeuverClassificationResultMapper implements
        ClassificationResultMapper<ManeuverForEstimation, ManeuverClassifierModelContext, ManeuverWithProbabilisticTypeClassification> {

    @Override
    public ManeuverWithProbabilisticTypeClassification mapToClassificationResult(double[] likelihoods,
            ManeuverForEstimation maneuver, ManeuverClassifierModelContext modelContext) {
        likelihoods = modelContext.getLikelihoodsPerManeuverTypeOrdinal(likelihoods);
        ManeuverWithProbabilisticTypeClassification maneuverClassificationResult = new ManeuverWithProbabilisticTypeClassification(
                maneuver, likelihoods);
        return maneuverClassificationResult;
    }

}
