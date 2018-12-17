package com.sap.sailing.windestimation.model.classifier.twdtransition;

import com.sap.sailing.windestimation.data.TwdTransition;
import com.sap.sailing.windestimation.model.classifier.AbstractClassifiersCache;
import com.sap.sailing.windestimation.model.store.ModelStore;

public class TwdTransitionClassifiersCache extends
        AbstractClassifiersCache<TwdTransition, TwdTransitionClassifierModelMetadata, TwdTransitionClassificationResult> {

    private final boolean enableBoatClassInfo;

    public TwdTransitionClassifiersCache(ModelStore classifierModelStore, long preserveLoadedClassifiersMillis,
            boolean enableBoatClassInfo) {
        super(classifierModelStore, preserveLoadedClassifiersMillis, new TwdTransitionClassifierModelFactory(),
                new TwdTransitionClassificationResultMapper());
        this.enableBoatClassInfo = enableBoatClassInfo;
    }

    @Override
    public TwdTransitionClassifierModelMetadata getContextSpecificModelMetadata(TwdTransition twdTransition) {
        TwdTransitionClassifierModelMetadata twdTrasitionModelMetadata = new TwdTransitionClassifierModelMetadata(null);
        return twdTrasitionModelMetadata;
    }

    public boolean isEnableBoatClassInfo() {
        return enableBoatClassInfo;
    }

}
