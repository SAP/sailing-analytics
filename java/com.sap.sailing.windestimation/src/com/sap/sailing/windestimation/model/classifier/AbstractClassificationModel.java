package com.sap.sailing.windestimation.model.classifier;

import com.sap.sailing.windestimation.model.AbstractTrainableModel;
import com.sap.sailing.windestimation.model.ModelContext;

public abstract class AbstractClassificationModel<InstanceType, T extends ModelContext<InstanceType>>
        extends AbstractTrainableModel<InstanceType, T> implements TrainableClassificationModel<InstanceType, T> {

    private static final long serialVersionUID = -3283338628850173316L;
    private final PreprocessingConfig preprocessingConfig;

    public AbstractClassificationModel(PreprocessingConfig preprocessingConfig, T modelContext) {
        super(modelContext);
        this.preprocessingConfig = preprocessingConfig;
    }

    @Override
    public PreprocessingConfig getPreprocessingConfig() {
        return preprocessingConfig;
    }

}
