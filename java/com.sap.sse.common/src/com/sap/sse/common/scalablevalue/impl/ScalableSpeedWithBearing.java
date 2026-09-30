package com.sap.sse.common.scalablevalue.impl;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.DoubleTriple;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.RadianBearingImpl;
import com.sap.sse.common.scalablevalue.ScalableValue;

/**
 * Separately scales speed and bearing. Instead of considering speed and bearing a single vector that can be scaled, the
 * bearing is scaled separately, and the speed is scaled as a scalar value independently of the bearing. This is
 * particularly useful for {@link Wind} scaling where it makes more sense to average the wind speed independently of the
 * wind direction / bearing than adding up the "wind vectors" and averaging, which would reduce the resulting wind speed
 * for constant wind speeds across all fixes with different directions.<p>
 * 
 * The triple used for scaling uses the speed in knots as the first component, the sine as the second and the
 * cosine as the third value.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class ScalableSpeedWithBearing implements ScalableValue<DoubleTriple, SpeedWithBearing> {
    private final double speedInKnots;
    private final double sin;
    private final double cos;
    
    public ScalableSpeedWithBearing(SpeedWithBearing speedWithBearing) {
        this(speedWithBearing.getKnots(), Math.sin(speedWithBearing.getBearing()
                .getRadians()), Math.cos(speedWithBearing.getBearing().getRadians()));
    }
    
    public ScalableSpeedWithBearing(Speed speed, double sin, double cos) {
        this(speed.getKnots(), sin, cos);
    }
    
    public ScalableSpeedWithBearing(double speedInKnots, double sin, double cos) {
        this.speedInKnots = speedInKnots;
        this.sin = sin;
        this.cos = cos;
    }

    @Override
    public ScalableSpeedWithBearing multiply(double factor) {
        return new ScalableSpeedWithBearing(factor*speedInKnots, factor*sin, factor*cos);
    }

    @Override
    public ScalableSpeedWithBearing add(ScalableValue<DoubleTriple, SpeedWithBearing> t) {
        final DoubleTriple tValue = t.getValue();
        return new ScalableSpeedWithBearing(speedInKnots + tValue.getA(), sin+tValue.getB(), cos+tValue.getC());
    }

    @Override
    public SpeedWithBearing divide(double divisor) {
        Speed newSpeed = new KnotSpeedImpl(speedInKnots / divisor);
        double angle;
        if (cos == 0) {
            angle = sin >= 0 ? Math.PI / 2 : -Math.PI / 2;
        } else {
            angle = Math.atan2(sin, cos);
        }
        Bearing bearing = new RadianBearingImpl(angle < 0 ? angle + 2 * Math.PI : angle);
        return new KnotSpeedWithBearingImpl(newSpeed.getKnots(), bearing);
    }

    @Override
    public DoubleTriple getValue() {
        return new DoubleTriple(speedInKnots, sin, cos);
    }
    
}