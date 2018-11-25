package com.sap.sailing.windestimation.classifier.smile;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.windestimation.classifier.AbstractClassificationModel;
import com.sap.sailing.windestimation.classifier.ClassifierPersistenceException;
import com.sap.sailing.windestimation.classifier.ContextSpecificModelMetadata;
import com.sap.sailing.windestimation.classifier.ModelMetadata;
import com.sap.sailing.windestimation.classifier.PreprocessingConfig;
import com.sap.sailing.windestimation.classifier.TrainableClassificationModel;
import com.sap.sailing.windestimation.classifier.store.PersistenceSupport;

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
        super(new ModelMetadata<>(preprocessingConfig, contextSpecificModelMetadata));
    }

    @Override
    public void train(double[][] x, int[] y) {
        trainingStarted();
        ModelMetadata<InstanceType, T> modelMetadata = getModelMetadata();
        PreprocessingConfig preprocessingConfig = modelMetadata.getPreprocessingConfig();
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
        ModelMetadata<InstanceType, T> modelMetadata = getModelMetadata();
        if (scaler != null) {
            x = scaler.transform(x);
        }
        if (pca != null) {
            x = pca.project(x);
        }
        double[] likelihoods = new double[modelMetadata.getContextSpecificModelMetadata()
                .getNumberOfPossibleTargetValues()];
        internalModel.predict(x, likelihoods);
        return likelihoods;
    }

    @Override
    public SmilePersistenceSupport getPersistenceSupport() {
        return new SmilePersistenceSupport();
    }

    private class SmilePersistenceSupport implements PersistenceSupport<TrainableClassificationModel<InstanceType, T>> {
        @Override
        public String getPersistenceKey() {
            StringBuilder key = new StringBuilder();
            key.append("classifier_smile_");
            key.append(AbstractSmileClassificationModel.this.getClass().getSimpleName());
            key.append("-");
            key.append(getModelMetadata().getContextSpecificModelMetadata().getId());
            return key.toString();
        }

        @Override
        public void saveToStream(OutputStream output) throws ClassifierPersistenceException {
            try (ObjectOutputStream serializer = new ObjectOutputStream(output)) {
                serializer.writeObject(AbstractSmileClassificationModel.this);
            } catch (IOException e) {
                throw new ClassifierPersistenceException(e);
            }
        }

        @Override
        public AbstractSmileClassificationModel<InstanceType, T> loadFromStream(InputStream input)
                throws ClassifierPersistenceException {
            try (ObjectInputStream deserializer = DomainFactory.INSTANCE
                    .createObjectInputStreamResolvingAgainstThisFactory(input)) {
                @SuppressWarnings("unchecked")
                AbstractSmileClassificationModel<InstanceType, T> loadedModel = (AbstractSmileClassificationModel<InstanceType, T>) deserializer
                        .readObject();
                return loadedModel;
            } catch (ClassNotFoundException | IOException e) {
                throw new ClassifierPersistenceException(e);
            }
        };
    }

}
