package com.sap.sailing.windestimation.classifier.smile;

import com.sap.sailing.windestimation.classifier.ContextSpecificModelMetadata;
import com.sap.sailing.windestimation.classifier.ModelMetadata;
import com.sap.sailing.windestimation.classifier.PreprocessingConfig.PreprocessingConfigBuilder;

import smile.classification.NeuralNetwork;

public class NeuralNetworkManeuverClassifier<InstanceType, T extends ContextSpecificModelMetadata<InstanceType>>
        extends AbstractSmileManeuverClassificationModel<InstanceType, T> {

    private static final long serialVersionUID = -3364152319152090775L;

    public NeuralNetworkManeuverClassifier(T contextSpecificModelMetadata) {
        super(new PreprocessingConfigBuilder().scaling().build(), contextSpecificModelMetadata);
    }

    @Override
    protected NeuralNetwork trainInternalModel(double[][] x, int[] y) {
        ModelMetadata<InstanceType, T> modelMetadata = getModelMetadata();
        int k = modelMetadata.getContextSpecificModelMetadata().getNumberOfPossibleTargetValues();
        int numberOfInputFeatures = x[0].length;
        NeuralNetwork net;
        int outputUnits = k == 2 ? 1 : k;
        int[] numUnits = new int[] { numberOfInputFeatures, 100, 100, outputUnits };
        if (k == 2) {
            net = new NeuralNetwork(NeuralNetwork.ErrorFunction.CROSS_ENTROPY,
                    NeuralNetwork.ActivationFunction.LOGISTIC_SIGMOID, numUnits);
        } else {
            net = new NeuralNetwork(NeuralNetwork.ErrorFunction.CROSS_ENTROPY, NeuralNetwork.ActivationFunction.SOFTMAX,
                    numUnits);
        }

        for (int i = 0; i < 20; i++) {
            net.learn(x, y);
        }
        return net;
    }

}
