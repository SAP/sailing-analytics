package com.sap.sse.common.confidence.impl;

import com.sap.sse.common.confidence.HasConfidence;

public class HasConfidenceImpl<ValueType, BaseType, RelativeTo> implements HasConfidence<ValueType, BaseType, RelativeTo> {
    private static final long serialVersionUID = -1635823148449693024L;
    private final double confidence;
    private final RelativeTo relativeTo;
    private final BaseType object;
    
    public HasConfidenceImpl(BaseType object, double confidence, RelativeTo relativeTo) {
        this.confidence = confidence;
        this.relativeTo = relativeTo;
        this.object = object;
    }

    @Override
    public double getConfidence() {
        return confidence;
    }
    
    @Override
    public RelativeTo getRelativeTo() {
        return relativeTo;
    }
    
    @Override
    public BaseType getObject() {
        return object;
    }

    @Override
    public String toString() {
        return ""+getObject()+"@"+getConfidence();
    }
}
