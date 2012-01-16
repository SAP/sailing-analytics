package com.sap.sailing.domain.confidence;

public interface ScalableValue<ValueType, AveragesTo> {
    ScalableValue<ValueType, AveragesTo> multiply(double factor);

    ScalableValue<ValueType, AveragesTo> add(ScalableValue<ValueType, AveragesTo> t);
    
    AveragesTo divide(double divisor, double confidence);
    
    ValueType getValue();
}
