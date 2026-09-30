package com.sap.sse.common.scalablevalue.impl;

import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.MeterDistance;
import com.sap.sse.common.scalablevalue.ScalableValue;
import com.sap.sse.common.scalablevalue.ScalableValueWithDistance;

public class ScalableDistance implements ScalableValueWithDistance<Double, Distance> {
    private final double meters;
    
    public ScalableDistance(Distance distance) {
        this.meters = distance.getMeters();
    }
    
    private ScalableDistance(double meters) {
        this.meters = meters;
    }

    @Override
    public ScalableDistance multiply(double factor) {
        return new ScalableDistance(factor*meters);
    }

    @Override
    public ScalableDistance add(ScalableValue<Double, Distance> t) {
        return new ScalableDistance(meters+t.getValue());
    }

    @Override
    public Distance divide(double divisor) {
        return new MeterDistance(meters / divisor);
    }

    @Override
    public Double getValue() {
        return meters;
    }

    @Override
    public double getDistance(Distance other) {
        return Math.abs(meters-other.getMeters());
    }
    
}
