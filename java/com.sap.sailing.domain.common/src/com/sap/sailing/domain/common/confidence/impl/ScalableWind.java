package com.sap.sailing.domain.common.confidence.impl;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sse.common.DoubleTriple;
import com.sap.sse.common.confidence.impl.LazyDividedScaledPosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.scalablevalue.ScalableValue;
import com.sap.sse.common.scalablevalue.impl.ScalablePosition;
import com.sap.sse.common.scalablevalue.impl.ScalableSpeedWithBearing;

/**
 * Wind values are scaled by separately scaling their speed and bearing, and separately scaling their time point, and
 * separately scaling their position. For the separate speed/bearing scaling see also {@link ScalableSpeedWithBearing}.<p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class ScalableWind implements ScalableValue<ScalableWind, Wind> {
    private final boolean scalablePositionNull;
    private final double scalablePositionX;
    private final double scalablePositionY;
    private final double scalablePositionZ;
    private final double scaledTimePointSumInMilliseconds;
    private final double speedWithBearingValueA;
    private final double speedWithBearingValueB;
    private final double speedWithBearingValueC;
    private final boolean useSpeed;
    
    public ScalableWind(Wind wind, boolean useSpeed) {
        this(wind.getPosition() == null ? null : new ScalablePosition(wind.getPosition()), wind.getTimePoint().asMillis(), new ScalableSpeedWithBearing(wind), useSpeed);
    }
    
    private ScalableWind(ScalablePosition scalablePosition, double scaledTimePointSumInMilliseconds,
            ScalableSpeedWithBearing scalableSpeedWithBearing, boolean useSpeed) {
        super();
        if (scalablePosition == null) {
            scalablePositionNull = true;
            this.scalablePositionX = 0.0;
            this.scalablePositionY = 0.0;
            this.scalablePositionZ = 0.0;
        } else {
            scalablePositionNull = false;
            final DoubleTriple scalablePositionTriple = scalablePosition.getValueAsTriple();
            this.scalablePositionX = scalablePositionTriple.getA();
            this.scalablePositionY = scalablePositionTriple.getB();
            this.scalablePositionZ = scalablePositionTriple.getC();
        }
        this.scaledTimePointSumInMilliseconds = scaledTimePointSumInMilliseconds;
        final DoubleTriple scalableSpeedWithBearingTriple = scalableSpeedWithBearing.getValue();
        this.speedWithBearingValueA = scalableSpeedWithBearingTriple.getA();
        this.speedWithBearingValueB = scalableSpeedWithBearingTriple.getB();
        this.speedWithBearingValueC = scalableSpeedWithBearingTriple.getC();
        this.useSpeed = useSpeed;
    }
    
    public boolean useSpeed() {
        return useSpeed;
    }

    @Override
    public ScalableWind multiply(double factor) {
        return new ScalableWind(getScalablePosition() == null ? null : getScalablePosition().multiply(factor),
                factor * scaledTimePointSumInMilliseconds, getScalableSpeedWithBearing().multiply(factor), useSpeed);
    }

    /**
     * If only one of <code>this</code> and <code>t</code> has <code>true</code> for {@link #useSpeed}, then its speed
     * is assumed for the other object's speed, effectively ignoring the other wind fix's speed as desired by setting
     * {@link #useSpeed} to <code>false</code>. The resulting object's {@link #useSpeed} is <code>true</code>, if at
     * least one of <code>this<code> and <code>t</code> has a <code>true</code> value for {@link #useSpeed}.
     */
    @Override
    public ScalableWind add(ScalableValue<ScalableWind, Wind> t) {
        final ScalablePosition scalablePosition = getScalablePosition();
        final ScalablePosition tValueScalablePosition = t.getValue().getScalablePosition();
        return new ScalableWind(scalablePosition == null ? tValueScalablePosition : scalablePosition.add(tValueScalablePosition), scaledTimePointSumInMilliseconds
                + t.getValue().scaledTimePointSumInMilliseconds, this.getScalableSpeedWithBearing().add(t.getValue().getScalableSpeedWithBearing()),
                useSpeed || t.getValue().useSpeed);
    }

    @Override
    public ScalableWind getValue() {
        return this;
    }

    @Override
    public Wind divide(double divisor) {
        return new WindImpl(getScalablePosition() == null ? null : new LazyDividedScaledPosition(getScalablePosition(), divisor),
                new MillisecondsTimePoint(
                        (long) (scaledTimePointSumInMilliseconds / divisor)), getScalableSpeedWithBearing().divide(divisor));
    }

    private ScalableSpeedWithBearing getScalableSpeedWithBearing() {
        return new ScalableSpeedWithBearing(speedWithBearingValueA, speedWithBearingValueB, speedWithBearingValueC);
    }

    private ScalablePosition getScalablePosition() {
        return scalablePositionNull ? null : new ScalablePosition(scalablePositionX, scalablePositionY, scalablePositionZ);
    }
}
