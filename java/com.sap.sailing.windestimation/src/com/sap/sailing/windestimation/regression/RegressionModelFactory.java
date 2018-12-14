package com.sap.sailing.windestimation.regression;

import com.sap.sailing.windestimation.model.ContextSpecificModelMetadata;
import com.sap.sailing.windestimation.model.ModelFactory;

public interface RegressionModelFactory<InstanceType, T extends ContextSpecificModelMetadata<InstanceType>>
        extends ModelFactory<InstanceType, T, TrainableRegressionModel<InstanceType, T>> {

}
