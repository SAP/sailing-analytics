package com.sap.sse.common.scalablevalue.impl;

import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.scalablevalue.ScalableValue;
import com.sap.sse.common.scalablevalue.ScalableValueWithDistance;

public class ScalableSpeed implements ScalableValueWithDistance<Double, Speed> {
    private final double knots;
    
    public ScalableSpeed(Speed speed) {
        this.knots = speed.getKnots();
    }
    
    private ScalableSpeed(double knots) {
        this.knots = knots;
    }

    @Override
    public ScalableSpeed multiply(double factor) {
        return new ScalableSpeed(factor*knots);
    }

    @Override
    public ScalableSpeed add(ScalableValue<Double, Speed> t) {
        return new ScalableSpeed(knots+t.getValue());
    }

    @Override
    public Speed divide(double divisor) {
        return new KnotSpeedImpl(knots / divisor);
    }

    @Override
    public Double getValue() {
        return knots;
    }

    @Override
    public double getDistance(Speed other) {
        return Math.abs(knots-other.getKnots());
    }
    
}
