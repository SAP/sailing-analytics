package com.sap.sailing.domain.common.impl;

import com.sap.sailing.domain.common.NonCardinalBounds;
import com.sap.sailing.domain.common.Position;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;

public class NonCardinalBoundsImpl implements NonCardinalBounds {
    /**
     * Computing spherical trigonometry with double precision can lead to numerical errors. When judging whether a
     * {@link Position} is {@link #contains(Position) contained} in some bounds, a bit of a "leeway" is assumed, to the
     * benefit of a position to be contained in a bounds object. This leeway is a factor multiplied with the size in the
     * dimension being checked ({@link #getHorizontalSize()} for left/right checks, {@link #getVerticalSize()} for
     * top/bottom checks).
     */
    private static final double EPSILON = 0.001;
    private final Position lowerLeft;
    private final Bearing verticalBearing;
    private final Distance verticalSize;
    private final Distance horizontalSize;

    public NonCardinalBoundsImpl(Position lowerLeft, Bearing verticalBearing, Distance verticalSize, Distance horizontalSize) {
        this.lowerLeft = lowerLeft;
        this.verticalBearing = verticalBearing;
        this.verticalSize = verticalSize;
        this.horizontalSize = horizontalSize;
    }

    @Override
    public Position getLowerLeft() {
        return lowerLeft;
    }

    @Override
    public Distance getVerticalSize() {
        return verticalSize;
    }

    @Override
    public Distance getHorizontalSize() {
        return horizontalSize;
    }

    @Override
    public NonCardinalBounds extend(Position p) {
        final NonCardinalBounds result;
        if (this.contains(p)) {
            result = this;
        } else {
            Position newLowerLeft = getLowerLeft();
            Distance newVerticalSize = getVerticalSize();
            Distance newHorizontalSize = getHorizontalSize();
            final Distance horizontalDistanceFromLeft = p.crossTrackError(getLowerLeft(), getVerticalBearing());
            if (horizontalDistanceFromLeft.compareTo(Distance.NULL) < 0) {
                // left of left border; extend to the left by projecting p down to the bottom and adjust horizontal distance
                final Distance verticalDistanceToBottom = p.crossTrackError(getLowerLeft(), getHorizontalBearing());
                newLowerLeft = p.translateGreatCircle(getVerticalBearing(), verticalDistanceToBottom);
                newHorizontalSize = getHorizontalSize().add(horizontalDistanceFromLeft.scale(-1.0));
            } else {
                final Distance horizontalDistanceFromRight = p.crossTrackError(getLowerRight(), getVerticalBearing());
                if (horizontalDistanceFromRight.compareTo(Distance.NULL) > 0) {
                    // right of right border; extend to the right by adjusting horizontal distance
                    newHorizontalSize = getHorizontalSize().add(horizontalDistanceFromRight);
                }            
            }
            final Distance verticalDistanceFromBottom = p.crossTrackError(getLowerLeft(), getHorizontalBearing());
            if (verticalDistanceFromBottom.compareTo(Distance.NULL) > 0) {
                // below bottom; extend to the bottom by projecting p down to the bottom and adjust vertical distance
                final Distance horizontalDistanceToLeft = p.crossTrackError(newLowerLeft, getVerticalBearing());
                newLowerLeft = p.translateGreatCircle(getHorizontalBearing().reverse(), horizontalDistanceToLeft);
                newVerticalSize = getVerticalSize().add(verticalDistanceFromBottom);
            } else {
                final Position newUppperLeft = newLowerLeft.translateGreatCircle(getVerticalBearing(), newVerticalSize);
                final Distance verticalDistanceFromTop = p.crossTrackError(newUppperLeft, getHorizontalBearing());
                if (verticalDistanceFromTop.compareTo(Distance.NULL) < 0) {
                    // above top; extend to the top by adjusting vertical distance
                    newVerticalSize = getVerticalSize().add(verticalDistanceFromTop.scale(-1.0));
                }
            }
            result = NonCardinalBounds.create(newLowerLeft, getVerticalBearing(), newVerticalSize, newHorizontalSize);
        }
        assert result.contains(p);
        return result;
    }

    @Override
    public NonCardinalBounds extend(NonCardinalBounds other) {
        assert contains(this.getLowerLeft());
        assert contains(this.getLowerRight());
        assert contains(this.getUpperLeft());
        assert contains(this.getUpperRight());
        final NonCardinalBounds result = this
                .extend(other.getLowerLeft())
                .extend(other.getLowerRight())
                .extend(other.getUpperLeft())
                .extend(other.getUpperRight());
        assert result.contains(this.getLowerLeft());
        assert result.contains(this.getLowerRight());
        assert result.contains(this.getUpperLeft());
        assert result.contains(this.getUpperRight());
        assert result.contains(other.getLowerLeft());
        assert result.contains(other.getLowerRight());
        assert result.contains(other.getUpperLeft());
        assert result.contains(other.getUpperRight());
        return result;
    }

    /**
     * Checks for {@code p} to be on or "right" of the line from {@link #getLowerLeft() lower left} to {@link #getUpperLeft() upper left},
     * on or "above" the line from {@link #getLowerLeft() lower left} to {@link #getLowerRight() lower right}, and so on.
     */
    @Override
    public boolean contains(Position p) {
        return p.crossTrackError(getLowerLeft(), getVerticalBearing()).compareTo(getVerticalSize().scale(-EPSILON)) >= 0
            && p.crossTrackError(getLowerLeft(), getHorizontalBearing()).compareTo(getHorizontalSize().scale(EPSILON)) <= 0
            && p.crossTrackError(getUpperLeft(), getHorizontalBearing()).compareTo(getHorizontalSize().scale(-EPSILON)) >= 0
            && p.crossTrackError(getLowerRight(), getVerticalBearing()).compareTo(getVerticalSize().scale(EPSILON)) <= 0;
    }

    @Override
    public boolean contains(NonCardinalBounds other) {
        return this.contains(other.getLowerLeft())
            && this.contains(other.getUpperLeft())
            && this.contains(other.getUpperRight())
            && this.contains(other.getLowerRight());
    }

    @Override
    public Bearing getVerticalBearing() {
        return verticalBearing;
    }
    
    @Override
    public Position getCenter() {
        return getLowerLeft().translateGreatCircle(getLowerLeft().getBearingGreatCircle(getUpperRight()), getLowerLeft().getDistance(getUpperRight()).scale(0.5));
    }

    @Override
    public String toString() {
        return "NonCardinalBounds[lowerLeft=" + lowerLeft + ", verticalBearing=" + verticalBearing
                + ", verticalSize=" + verticalSize + ", horizontalSize=" + horizontalSize + "]";
    }
}
