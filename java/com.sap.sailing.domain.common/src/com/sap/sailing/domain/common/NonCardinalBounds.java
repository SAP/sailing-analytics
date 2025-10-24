package com.sap.sailing.domain.common;

import com.sap.sailing.domain.common.impl.NonCardinalBoundsImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * A surface area on a sphere, defined by one corner {@link Position}, a {@link Bearing} for the "vertical" axis, and
 * two {@link Distance}s for the two vectors, one pointing in the heading for the vertical axis, the other pointing
 * 90deg clockwise of it, in the "horizontal" direction. Such bounds are different from {@link Bounds} which can have
 * their edges point only in the cardinal directions (N/W/S/E). Hence, these {@link NonCardinalBounds} can be used,
 * e.g., to describe the visible area of a map that is not shown north-up but has a non-zero heading set.
 * <p>
 * 
 * These bounds can check for a {@link Position} whether or not it is contained. Furthermore, the bounds may be
 * {@link #extend(Position) extended} by another {@link Position} (a no-op if that {@link Position} is already
 * {@link #contains(Position) contained} in these bounds.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface NonCardinalBounds {
    static final Bearing NINETY_DEGREE_ANGLE = new DegreeBearingImpl(90);
    
    static NonCardinalBounds create(Position lowerLeft, Bearing verticalBearing, Distance verticalSize, Distance horizontalSize) {
        return new NonCardinalBoundsImpl(lowerLeft, verticalBearing, verticalSize, horizontalSize);
    }
    
    /**
     * Produces a bounds object with zero size that only contains the position {@code p}.
     */
    static NonCardinalBounds create(Position p, Bearing verticalBearing) {
        return create(p, verticalBearing, Distance.NULL, Distance.NULL);
    }
    
    Position getLowerLeft();

    default Position getUpperLeft() {
        return getLowerLeft().translateGreatCircle(getVerticalBearing(), getVerticalSize());
    }

    default Position getUpperRight() {
        return getUpperLeft().translateGreatCircle(getHorizontalBearing(), getHorizontalSize());
    }

    default Position getLowerRight() {
        return getLowerLeft().translateGreatCircle(getHorizontalBearing(), getHorizontalSize());
    }

    Distance getVerticalSize();

    Distance getHorizontalSize();

    /**
     * Extends these bounds such that {@code p} is {@link #contains(Position) contained} in the resulting bounds. If
     * {@code p} is already contained in these bounds, these bounds are returned unchanged. The resulting bounds have
     * the same {@link #getHorizontalBearing() horizontal} and {@link #getVerticalBearing() vertical} headings as
     * these bounds.
     */
    NonCardinalBounds extend(Position p);

    /**
     * Returns bounds that {@link #contains(NonCardinalBounds) contain} both, these and the {@code other} bounds. If the
     * {@code other} bounds are already contained in these bounds, these bounds are returned unchanged. The bounds
     * returned will have the same {@link #getHorizontalBearing() horizontal} and {@link #getVerticalBearing() vertical}
     * headings as these bounds.
     */
    NonCardinalBounds extend(NonCardinalBounds other);

    boolean contains(Position p);

    boolean contains(NonCardinalBounds other);

    Bearing getVerticalBearing();

    default Bearing getHorizontalBearing() {
        return getVerticalBearing().add(NINETY_DEGREE_ANGLE);
    }

    Position getCenter();
}
