package com.sap.sse.common.scalablevalue;

public class ScalableDoubleWithConfidence<RelativeTo> extends ScalableDouble implements HasConfidenceAndIsScalable<Double, Double, RelativeTo> {
    private static final long serialVersionUID = 1042652394404557792L;
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
