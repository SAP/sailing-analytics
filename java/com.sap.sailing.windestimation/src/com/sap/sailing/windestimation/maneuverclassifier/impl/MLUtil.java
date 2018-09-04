package com.sap.sailing.windestimation.maneuverclassifier.impl;

import java.util.List;

import com.sap.sailing.windestimation.data.LabelledManeuverForEstimation;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;

public class MLUtil {

    private MLUtil() {
    }

    public static double[] getInputVectorAsDoubleArray(ManeuverForEstimation maneuver,
            ManeuverFeatures maneuverFeatures) {
        double[] inputVector = new double[getNumberOfInputFeatures(maneuverFeatures)];
        int i = 0;
        inputVector[i++] = Math.abs(maneuver.getCourseChangeInDegrees());
        inputVector[i++] = maneuver.getSpeedLossRatio();
        inputVector[i++] = maneuver.getSpeedGainRatio();
        inputVector[i++] = maneuver.getMaxTurningRateInDegreesPerSecond();
        if (maneuverFeatures.isPolarsInformation()) {
            inputVector[i++] = maneuver.getDeviationFromOptimalTackAngleInDegrees();
            inputVector[i++] = maneuver.getDeviationFromOptimalJibeAngleInDegrees();
        }
        if (maneuverFeatures.isScaledSpeed()) {
            inputVector[i++] = maneuver.getScaledSpeedBefore();
            inputVector[i++] = maneuver.getScaledSpeedAfter();
        }
        return inputVector;
    }

    public static double[][] getInputMatrixAsDoubleArray(List<ManeuverForEstimation> maneuvers,
            ManeuverFeatures maneuverFeatures) {
        double[][] inputMatrix = new double[maneuvers.size()][];
        int i = 0;
        for (ManeuverForEstimation maneuver : maneuvers) {
            inputMatrix[i++] = getInputVectorAsDoubleArray(maneuver, maneuverFeatures);
        }
        return inputMatrix;
    }

    public static int[] getOutputAsIntArray(List<ManeuverForEstimation> maneuvers,
            int[] supportedManeuverTypesMapping) {
        int[] output = new int[maneuvers.size()];
        int i = 0;
        for (ManeuverForEstimation maneuver : maneuvers) {
            output[i] = supportedManeuverTypesMapping[((LabelledManeuverForEstimation) maneuver).getManeuverType()
                    .ordinal()];
        }
        return output;
    }

    public static int getNumberOfInputFeatures(ManeuverFeatures maneuverFeatures) {
        int numberOfFeatures = 4;
        if (maneuverFeatures.isPolarsInformation()) {
            numberOfFeatures += 2;
        }
        if (maneuverFeatures.isScaledSpeed()) {
            numberOfFeatures += 2;
        }
        return numberOfFeatures;
    }

}
