package com.sap.sse.common.scalablevalue.impl;

import com.sap.sse.common.DoubleTriple;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.RadianPosition;
import com.sap.sse.common.scalablevalue.ScalableValue;
import com.sap.sse.common.scalablevalue.ScalableValueWithDistance;

public class ScalablePosition implements ScalableValueWithDistance<ScalablePosition, Position> {
    private final double x, y, z;
    
    public ScalablePosition(Position position) {
        this(Math.cos(position.getLatRad()) * Math.cos(position.getLngRad()),
                Math.cos(position.getLatRad()) * Math.sin(position.getLngRad()),
                Math.sin(position.getLatRad()));
    }
    
    /**
     * Use the three components obtained from {@link #getValueAsTriple()}
     */
    public ScalablePosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    @Override
    public ScalablePosition multiply(double factor) {
        return new ScalablePosition(factor*x, factor*y, factor*z);
    }

    @Override
    public ScalablePosition add(ScalableValue<ScalablePosition, Position> t) {
        ScalablePosition result;
        if (t == null) {
            result = this;
        } else {
            result = new ScalablePosition(x+t.getValue().x, y+t.getValue().y, z+t.getValue().z);
        }
        return result;
    }

    /**
     * If combined confidence is 0.0 (all coordinate components are 0.0), <code>null</code> is returned because no
     * position can reasonably be constructed out of nowhere.
     */
    @Override
    public Position divide(double divisor) {
        Position result;
        if (x == 0 && y == 0 && z == 0) {
            result = null;
        } else {
            // don't need to scale down; atan2 is agnostic regarding scaling factors
            double hyp = Math.sqrt(x * x + y * y);
            double latRad = Math.atan2(z, hyp);
            double lngRad = Math.atan2(y, x);
            result = new RadianPosition(latRad, lngRad);
        }
        return result;
    }

    @Override
    public ScalablePosition getValue() {
        return this;
    }

    @Override
    public double getDistance(Position other) {
        return divide(1).getDistance(other).getCentralAngleDeg();
    }
    
    public DoubleTriple getValueAsTriple() {
        return new DoubleTriple(x, y, z);
    }
}