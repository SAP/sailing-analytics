package com.sap.sailing.windestimation.model.regressor;

import com.sap.sailing.windestimation.model.AbstractModelCache;
import com.sap.sailing.windestimation.model.ContextSpecificModelMetadata;
import com.sap.sailing.windestimation.model.store.ModelStore;

public abstract class AbstractRegressorCache<InstanceType, T extends ContextSpecificModelMetadata<InstanceType>>
        extends AbstractModelCache<InstanceType, T, TrainableRegressorModel<InstanceType, T>> {

    public AbstractRegressorCache(ModelStore modelStore, long preserveLoadedModelsMillis,
            RegressionModelFactory<InstanceType, T> modelFactory) {
        super(modelStore, preserveLoadedModelsMillis, modelFactory);
    }

}
