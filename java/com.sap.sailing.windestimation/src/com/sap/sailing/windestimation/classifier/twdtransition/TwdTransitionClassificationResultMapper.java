package com.sap.sailing.windestimation.classifier.twdtransition;

import com.sap.sailing.windestimation.classifier.ClassificationResultMapper;
import com.sap.sailing.windestimation.data.TwdTransition;

public class TwdTransitionClassificationResultMapper implements
        ClassificationResultMapper<TwdTransition, TwdTransitionClassifierModelMetadata, TwdTransitionClassificationResult> {

    @Override
    public TwdTransitionClassificationResult mapToClassificationResult(double[] likelihoods, TwdTransition instance,
            TwdTransitionClassifierModelMetadata contextSpecificModelMetadata) {
        TwdTransitionClassificationResult result = new TwdTransitionClassificationResult(likelihoods[1],
                likelihoods[0]);
        return result;
    }

}
