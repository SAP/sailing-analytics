package com.sap.sse.common.scalablevalue.impl;

import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.SecondsDurationImpl;
import com.sap.sse.common.scalablevalue.ScalableValue;
import com.sap.sse.common.scalablevalue.ScalableValueWithDistance;

public class ScalableDuration implements ScalableValueWithDistance<Double, Duration> {
    private final double seconds;
    
    public ScalableDuration(Duration duration) {
        this.seconds = duration.asSeconds();
    }
    
    private ScalableDuration(double seconds) {
        this.seconds = seconds;
    }

    @Override
    public ScalableDuration multiply(double factor) {
        return new ScalableDuration(factor*seconds);
    }

    @Override
    public ScalableDuration add(ScalableValue<Double, Duration> t) {
        return new ScalableDuration(seconds+t.getValue());
    }

    @Override
    public Duration divide(double divisor) {
        return new SecondsDurationImpl(seconds / divisor);
    }

    @Override
    public Double getValue() {
        return seconds;
    }

    @Override
    public double getDistance(Duration other) {
        return Math.abs(seconds-other.asSeconds());
    }
    
    @Override
    public String toString() {
        return divide(1).toString();
    }
}
