package com.sap.sse.common.scalablevalue.impl;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.DoublePair;
import com.sap.sse.common.impl.RadianBearingImpl;
import com.sap.sse.common.scalablevalue.ScalableValue;
import com.sap.sse.common.scalablevalue.ScalableValueWithDistance;

public class ScalableBearing implements ScalableValueWithDistance<DoublePair, Bearing> {
    private final double sin;
    private final double cos;
    
    public ScalableBearing(Bearing bearing) {
        this.sin = Math.sin(bearing.getRadians());
        this.cos = Math.cos(bearing.getRadians());
    }
    
    private ScalableBearing(double sin, double cos) {
        this.sin = sin;
        this.cos = cos;
    }
    
    @Override
    public ScalableBearing multiply(double factor) {
        DoublePair pair = getValue();
        return new ScalableBearing(factor*pair.getA(), factor*pair.getB());
    }

    @Override
    public ScalableBearing add(ScalableValue<DoublePair, Bearing> t) {
        DoublePair value = getValue();
        DoublePair tValue = t.getValue();
        return new ScalableBearing(value.getA()+tValue.getA(), value.getB()+tValue.getB());
    }

    /**
     * If the combined confidence was 0.0, no {@link Bearing} object can reasonably be computed; hence, <code>null</code>
     * is returned in such cases.
     */
    @Override
    public Bearing divide(double divisor) {
        Bearing result;
        if (sin == 0 && cos == 0) {
            result = null;
        } else {
            double angle;
            if (cos == 0) {
                angle = sin >= 0 ? Math.PI / 2 : -Math.PI / 2;
            } else {
                angle = Math.atan2(sin, cos);
            }
            result = new RadianBearingImpl(angle < 0 ? angle + 2 * Math.PI : angle);
        }
        return result;
    }

    @Override
    public DoublePair getValue() {
        return new DoublePair(sin, cos);
    }

    @Override
    public double getDistance(Bearing other) {
        return Math.abs(divide(1).getDifferenceTo(other).getDegrees());
    }

    /**
     * Computing the difference to a {@link bearing}, as implemented by {@link #getDistance(Bearing)} is correct but
     * slow because the {@link Math#atan2(double)} function is used to {@link ScalableBearing#divide(double) bring the
     * scalable bearing down to a bearing}. A less precise, yet much faster to compute and still monotonous distance
     * measure computes the length of the line segment connecting this bearing's projection onto a circle with the
     * projection of the bearing <code>a</code> to the same circle. The distance returned by this method will provide
     * almost equal values for very small differences (below 1deg) but significantly smaller values for big differences.
     * 
     * @return an approximate difference in degrees, fairly accurate for small differences, but returning 360.0/PI
     *         (approximately 114) instead of 180 for the maximum difference
     */
    public double getApproximateDegreeDistanceTo(Bearing a) {
        double scale = Math.sqrt(sin*sin + cos*cos);
        double x = cos/scale;
        double y = sin/scale;
        double ax = Math.cos(a.getRadians());
        double ay = Math.sin(a.getRadians());
        double normalDistance = Math.sqrt((x-ax)*(x-ax) + (y-ay)*(y-ay));
        return normalDistance / Math.PI * 180.;
    }
}
