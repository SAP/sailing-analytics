package com.sap.sailing.windestimation.classifier.smile;

import com.sap.sailing.windestimation.classifier.AbstractClassificationModel;
import com.sap.sailing.windestimation.classifier.PreprocessingConfig;
import com.sap.sailing.windestimation.model.ContextSpecificModelMetadata;
import com.sap.sailing.windestimation.model.store.SerializationBasedPersistenceSupport;

import smile.classification.SoftClassifier;
import smile.feature.Standardizer;
import smile.projection.PCA;

public abstract class AbstractSmileClassificationModel<InstanceType, T extends ContextSpecificModelMetadata<InstanceType>>
        extends AbstractClassificationModel<InstanceType, T> {

    private static final long serialVersionUID = 1037686504611915506L;

    private Standardizer scaler = null;
    private PCA pca = null;
    private SoftClassifier<double[]> internalModel = null;

    public AbstractSmileClassificationModel(PreprocessingConfig preprocessingConfig, T contextSpecificModelMetadata) {
        super(preprocessingConfig, contextSpecificModelMetadata);
    }

    @Override
    public void train(double[][] x, int[] y) {
        trainingStarted();
        PreprocessingConfig preprocessingConfig = getPreprocessingConfig();
        scaler = null;
        if (preprocessingConfig.isScaling()) {
            scaler = new Standardizer(false);
            scaler.learn(x);
            x = scaler.transform(x);
        }
        pca = null;
        if (preprocessingConfig.isPca()) {
            pca = new PCA(x);
            if (preprocessingConfig.isPcaComponents()) {
                pca.setProjection(preprocessingConfig.getNumberOfPcaComponents());
            } else if (preprocessingConfig.isPcaPercentage()) {
                pca.setProjection(preprocessingConfig.getPercentageValue());
            }
            x = pca.project(x);
        }
        internalModel = trainInternalModel(x, y);
        trainingFinishedSuccessfully();
    }

    protected abstract SoftClassifier<double[]> trainInternalModel(double[][] x, int[] y);

    @Override
    public double[] classifyWithProbabilities(double[] x) {
        if (!isModelReady()) {
            throw new IllegalStateException("The classification model is not trained");
        }
        if (scaler != null) {
            x = scaler.transform(x);
        }
        if (pca != null) {
            x = pca.project(x);
        }
        double[] likelihoods = new double[getContextSpecificModelMetadata().getNumberOfPossibleTargetValues()];
        internalModel.predict(x, likelihoods);
        return likelihoods;
    }

    @Override
    public SerializationBasedPersistenceSupport getPersistenceSupport() {
        return new SerializationBasedPersistenceSupport(this);
    }

}
