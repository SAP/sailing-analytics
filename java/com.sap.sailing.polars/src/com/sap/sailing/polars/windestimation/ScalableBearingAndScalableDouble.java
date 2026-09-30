package com.sap.sailing.polars.windestimation;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.scalablevalue.ScalableDouble;
import com.sap.sse.common.scalablevalue.ScalableValue;
import com.sap.sse.common.scalablevalue.ScalableValueWithDistance;
import com.sap.sse.common.scalablevalue.impl.ScalableBearing;

/**
 * Two {@link Bearings} combined in one scalable object, like a vector of two bearings. {@link #add(ScalableValue)
 * Addition} works component wise, and so does {@link #multiply(double) multiplication} and {@link #divide(double)
 * division}. The {@link #getDistance(Pair) distance} is calculated as the Euklidian two-dimensional distance between
 * the two bearing pairs, with "degrees" as conceptual unit. For example, another object of this type that is 2 and 3
 * degrees apart from this object will have a distance of {@link Math#sqrt(double) Math.sqrt(13)}.
 * <p>
 * 
 * The {@link #getDistance(Pair)} implementation uses {@link ScalableBearing#getApproximateDegreeDistanceTo(Bearing)} on
 * the scalable bearing part of this object. This is only an approximation but works a lot faster and is still
 * monotonous. It works well for small angles.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ScalableBearingAndScalableDouble implements
        ScalableValueWithDistance<Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>> {
    private final ScalableBearing b1;
    private final ScalableDouble b2;

    public ScalableBearingAndScalableDouble(Bearing b1, double b2) {
        this(new ScalableBearing(b1), new ScalableDouble(b2));
    }

    private ScalableBearingAndScalableDouble(ScalableBearing b1, ScalableDouble b2) {
        super();
        this.b1 = b1;
        this.b2 = b2;
    }

    @Override
    public Pair<Bearing, Double> divide(double divisor) {
        return new Pair<>(b1.divide(divisor), b2.divide(divisor));
    }

    @Override
    public Pair<ScalableBearing, ScalableDouble> getValue() {
        return new Pair<>(b1, b2);
    }

    @Override
    public double getDistance(Pair<Bearing, Double> other) {
        final double b1DiffDeg = b1.getApproximateDegreeDistanceTo(other.getA());
        final double b2DiffDeg = Math.abs(b2.divide(1) - other.getB());
        return Math.sqrt(b1DiffDeg * b1DiffDeg + b2DiffDeg * b2DiffDeg);
    }

    @Override
    public ScalableBearingAndScalableDouble add(
            ScalableValue<Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>> t) {
        return new ScalableBearingAndScalableDouble(b1.add(t.getValue().getA()), b2.add(t.getValue().getB()));
    }

    @Override
    public ScalableBearingAndScalableDouble multiply(double factor) {
        return new ScalableBearingAndScalableDouble(b1.multiply(factor), b2.multiply(factor));
    }
}
