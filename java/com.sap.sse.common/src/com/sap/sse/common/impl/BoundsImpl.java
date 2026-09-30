package com.sap.sse.common.impl;

import com.sap.sse.common.Bounds;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;

public class BoundsImpl implements Bounds {
    private final Position sw;
    private final Position ne;
    private final boolean crossesDateLine;
    
    public BoundsImpl(Position sw, Position ne) {
        super();
        this.sw = sw;
        this.ne = ne;
        crossesDateLine = ne.getLngDeg() < sw.getLngDeg();
    }
    
    public BoundsImpl(Position position) {
        this(position, position);
    }

    @Override
    public Position getNorthWest() {
        return new DegreePosition(getNorthEast().getLatDeg(), getSouthWest().getLngDeg());
    }

    @Override
    public Position getSouthEast() {
        return new DegreePosition(getSouthWest().getLatDeg(), getNorthEast().getLngDeg());
    }

    @Override
    public Position getNorthEast() {
        return ne;
    }

    @Override
    public Position getSouthWest() {
        return sw;
    }

    /**
     * Considering whether or not this bounds object crosses the date line, calculates the width in degrees longitude
     * going from <code>west</code> to <code>east</code>. The result is always a non-negative number.
     */
    private double getDistanceDeg(final double west, final double east) {
        final double diff = east-west;
        final double result;
        if (isCrossingDateLine(west, east)) {
            result = 360+diff; // diff is negative in this case
        } else {
            result = diff;
        }
        return result;
    }
    
    @Override
    public Bounds intersect(Bounds other) {
        final double maxSouthLatDeg = Math.max(getSouthWest().getLatDeg(), other.getSouthWest().getLatDeg());
        final double minNorthLatDeg = Math.min(getNorthEast().getLatDeg(), other.getNorthEast().getLatDeg());
        final double westDeg;
        if (containsLngDeg(other.getSouthWest().getLngDeg())) {
            westDeg = other.getSouthWest().getLngDeg();
        } else if (other.containsLngDeg(getSouthWest().getLngDeg())) {
            westDeg = getSouthWest().getLngDeg();
        } else {
            // no intersection
            westDeg = getSouthWest().getLngDeg();
        }
        final double eastDeg;
        if (containsLngDeg(other.getNorthEast().getLngDeg())) {
            eastDeg = other.getNorthEast().getLngDeg();
        } else if (other.containsLngDeg(getNorthEast().getLngDeg())) {
            eastDeg = getNorthEast().getLngDeg();
        } else {
            // no intersection
            eastDeg = getSouthWest().getLngDeg();
        }
        return new BoundsImpl(new DegreePosition(maxSouthLatDeg, westDeg), new DegreePosition(minNorthLatDeg, eastDeg));
    }

    @Override
    public Bounds union(Bounds other) {
        final double minLatDeg = Math.min(getSouthWest().getLatDeg(), other.getSouthWest().getLatDeg());
        final double maxLatDeg = Math.max(getNorthEast().getLatDeg(), other.getNorthEast().getLatDeg());
        // For lng, we can go left or right around the earth; which way results in the smaller bounds? We have four options:
        final double[] west = new double[] { getSouthWest().getLngDeg(), other.getSouthWest().getLngDeg() };
        final double[] east = new double[] { getNorthEast().getLngDeg(), other.getNorthEast().getLngDeg() };
        double minLngDegDistance = Double.MAX_VALUE;
        double bestWest = 0;
        double bestEast = 0;
        for (int w=0; w<2; w++) {
            for (int e=0; e<2; e++) {
                double currentLngDegDistance;
                if (spansLngDeg(west[w], east[e], west[1 - w]) && spansLngDeg(west[w], east[e], east[1 - e])
                        && (currentLngDegDistance = Math.abs(getDistanceDeg(west[w], east[e]))) < minLngDegDistance) {
                    minLngDegDistance = currentLngDegDistance;
                    bestWest = west[w];
                    bestEast = east[e];
                }
            }
        }
        return new BoundsImpl(new DegreePosition(minLatDeg, bestWest), new DegreePosition(maxLatDeg, bestEast));
    }
    
    @Override
    public Bounds extend(Bounds other) {
        return union(other);
    }
    
    @Override
    public Bounds extend(Position p) {
        return union(new BoundsImpl(p, p));
    }

    private boolean spansLngDeg(double westLngDeg, double eastLngDeg, double lngDeg) {
        return isCrossingDateLine(westLngDeg, eastLngDeg)
                ? (lngDeg >= westLngDeg && lngDeg <= 180) || (lngDeg >= -180 && lngDeg <= eastLngDeg)
                : westLngDeg <= lngDeg && lngDeg <= eastLngDeg;
    }

    private boolean isCrossingDateLine(double westLngDeg, double eastLngDeg) {
        return westLngDeg > eastLngDeg;
    }

    @Override
    public boolean intersects(Bounds other) {
        return (containsLatDeg(other.getSouthWest().getLatDeg()) || containsLatDeg(other.getNorthEast().getLatDeg()) ||
                other.containsLatDeg(getSouthWest().getLatDeg()) || other.containsLatDeg(getNorthEast().getLatDeg()))
            && (containsLngDeg(other.getSouthWest().getLngDeg()) || containsLngDeg(other.getNorthEast().getLngDeg()) ||
                other.containsLngDeg(getSouthWest().getLngDeg()) || other.containsLngDeg(getNorthEast().getLngDeg()));
    }
    
    @Override
    public boolean containsLatDeg(double latDeg) {
        return latDeg >= getSouthWest().getLatDeg() && latDeg <= getNorthEast().getLatDeg();
    }

    @Override
    public boolean containsLngDeg(double lngDeg) {
        return spansLngDeg(getSouthWest().getLngDeg(), getNorthEast().getLngDeg(), lngDeg);
    }
    
    @Override
    public boolean contains(Position other) {
        return
                    // lat contained:
                other.getLatDeg() >= getSouthWest().getLatDeg() && other.getLatDeg() <= getNorthEast().getLatDeg() &&
                    // lng contained:
                (isCrossesDateLine()
                    // between SW and date line...
                    ? (other.getLngDeg() <= 180 && other.getLngDeg() >= getSouthWest().getLngDeg()) ||
                    // ...or between date line and NE
                      (other.getLngDeg() >= -180 && other.getLngDeg() <= getNorthEast().getLngDeg())
                    // these bounds are not crossing the date line; simple numeric comparison
                    : other.getLngDeg() >= getSouthWest().getLngDeg() && other.getLngDeg() <= getNorthEast().getLngDeg())
                ;
    }

    @Override
    public boolean contains(Bounds other) {
        return isCrossesDateLine() == other.isCrossesDateLine() && contains(other.getNorthEast()) && contains(other.getSouthWest());
    }

    @Override
    public boolean isCrossesDateLine() {
        return crossesDateLine;
    }
    
    @Override
    public boolean isEmpty() {
        return getSouthWest().getLatDeg() >= getNorthEast().getLatDeg() ||
               getSouthWest().getLngDeg() == getNorthEast().getLngDeg();
    }
    
    @Override
    public Distance getDiameter() {
        return getNorthWest().getDistance(getSouthEast());
    }

    @Override
    public String toString() {
        return "{SW: "+getSouthWest().toString()+", NE: "+getNorthEast().toString()+"}";
    }
}
