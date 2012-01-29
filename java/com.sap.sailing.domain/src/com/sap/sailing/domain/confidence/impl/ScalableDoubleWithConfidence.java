package com.sap.sailing.domain.confidence.impl;

import com.sap.sailing.domain.confidence.HasConfidenceAndIsScalable;

public class ScalableDoubleWithConfidence<RelativeTo> extends ScalableDouble implements HasConfidenceAndIsScalable<Double, Double, RelativeTo> {
    private final double confidence;
    private final RelativeTo relativeTo;
    
    public ScalableDoubleWithConfidence(double d, double confidence, RelativeTo relativeTo) {
        super(d);
        this.confidence = confidence;
        this.relativeTo = relativeTo;
    }
    
    @Override
    public Double getObject() {
        return getValue();
    }

    @Override
    public RelativeTo getRelativeTo() {
        return relativeTo;
    }
    
    @Override
    public double getConfidence() {
        return confidence;
    }

    @Override
    public ScalableDouble getScalableValue() {
        return this;
    }
}
