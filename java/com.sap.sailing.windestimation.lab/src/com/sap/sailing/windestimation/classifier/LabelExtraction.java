package com.sap.sailing.windestimation.classifier;

import java.util.List;

public interface LabelExtraction<InstanceType> {

    int getY(InstanceType instance);

    default int[] getYVector(List<InstanceType> instances) {
        int[] output = new int[instances.size()];
        int i = 0;
        for (InstanceType instance : instances) {
            output[i++] = getY(instance);
        }
        return output;
    }

}
