package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.LatLngBounds;
import com.sap.sailing.domain.common.Bounds;
import com.sap.sailing.domain.common.NonCardinalBounds;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.BoundsImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * Converts between {@link Bounds} and {@link LatLngBounds}.
 * 
 * @author Axel Uhl (D043530)
 */
public class BoundsUtil {
    private final static double SECTOR_BOUNDARY_LIMIT_DEGREES = 0.00001;
    
    /**
     * When calling {@link MapWidget#getBounds()}, the result is the smallest rectlinear bounding box aligned with
     * cardinal direction (N/S/E/W) that contains the map's viewport. For a map with {@code heading} being true-north
     * (0deg), the viewport is aligned with the cardinal directions, and hence the map bounds equal the visible portion
     * of the map. However, if the map is rotated by an angle not evenly divisible by 90deg, the bounds returned by
     * {@link MapWidget#getBounds()} will effectively contain four areas in the corners of these bounds that are not
     * shown on screen. Instead, the viewport is a rotated rectangle inside the cardinally-aligned map bounds rectangle.
     * <p>
     * 
     * This method constructs a {@link NonCardinalBounds} bounding box that represents the visible part of the map only,
     * based on the cardinally-aligned {@code latLngBounds} assumed to come from {@link MapWidget#getBounds()} and the
     * {@code heading}, assumed to be constructed from {@link MapWidget#getHeading()}. The resulting bounds should be
     * contained in the {@code mapBounds}, with its corners being on the edges of those {@code latLngBounds}, give or
     * take minor numerical errors incurred by the limited accuracy of {@code double}-precision numbers.
     * <p>
     * 
     * The {@link CoordinateSystem} is used to transform the map's {@link LatLng} coordinates to real-world
     * {@link Position} coordinates.
     * <p>
     * 
     * The implementation distinguishes between the four {@code mapBounds} edges on which the
     * {@link NonCardinalBounds#getLowerLeft() lower left corner} of the resulting bounds may lie, corresponding to the
     * four 90deg intervals for the heading, with special treatment when very close (see {@link #SECTOR_BOUNDARY_LIMIT_DEGREES})
     * to the sector boundary where the resulting bounds <em>are</em> rectlinear and cardinally aligned again.
     * <p>
     */
    public static NonCardinalBounds getMapBounds(LatLngBounds mapBounds, Bearing mapHeading, CoordinateSystem coordinateSystem) {
        final Position mapBoundsSouthWest = coordinateSystem.getPosition(mapBounds.getSouthWest());
        final Position mapBoundsNorthEast = coordinateSystem.getPosition(mapBounds.getNorthEast());
        final Position mapBoundsNorthWest = new DegreePosition(mapBoundsNorthEast.getLatDeg(), mapBoundsSouthWest.getLngDeg());
        final Position mapBoundsSouthEast = new DegreePosition(mapBoundsSouthWest.getLatDeg(), mapBoundsNorthEast.getLngDeg());
        final Distance horizontalMapBoundsSize = mapBoundsSouthWest.getDistance(mapBoundsSouthEast);
        final Distance verticalMapBoundsSize = mapBoundsSouthEast.getDistance(mapBoundsNorthEast);
        Distance horizontalSizeViewport = null;
        Distance verticalSizeViewport = null;
        Position viewportLowerLeft = null;
        final Position[] mapBoundsCornersClockwise = new Position[] { mapBoundsSouthWest, mapBoundsNorthWest, mapBoundsNorthEast, mapBoundsSouthEast };
        // normalize heading to degree angles in range [0..360)
        while (mapHeading.getDegrees() < 0) {
            mapHeading = mapHeading.add(new DegreeBearingImpl(360)); // translate to a positive angle
        }
        if (mapHeading.getDegrees() < SECTOR_BOUNDARY_LIMIT_DEGREES) {
            // not rotated; use map bounds
            viewportLowerLeft = mapBoundsSouthWest;
            horizontalSizeViewport = horizontalMapBoundsSize;
            verticalSizeViewport = verticalMapBoundsSize;
        } else {
            // determine the sector out of the four possible ones:
            final int sectorIndex = (int) ((mapHeading.getDegrees() - SECTOR_BOUNDARY_LIMIT_DEGREES) / 90.0);
            final Bearing heading = new DegreeBearingImpl(mapHeading.getDegrees() - sectorIndex * 90.0);
            final Bearing currentMapBoundsEdgeBearing = Bearing.NORTH.add(new DegreeBearingImpl(sectorIndex * 90.0));
            Distance lengthOfMapBoundsEdgeHoldingLowerLeft = sectorIndex % 2 == 0 ? verticalMapBoundsSize : horizontalMapBoundsSize;
            Distance lengthOfMapBoundsEdgeNotHoldingLowerLeft = sectorIndex % 2 == 0 ? horizontalMapBoundsSize : verticalMapBoundsSize;
            if (heading.getDegrees() < 90 - SECTOR_BOUNDARY_LIMIT_DEGREES) {
                final double tangensHeading = Math.tan(heading.getRadians());
                final Distance fromMapBoundsCornerToLowerLeftOfViewportBounds = lengthOfMapBoundsEdgeHoldingLowerLeft.scale(tangensHeading*tangensHeading).add(lengthOfMapBoundsEdgeNotHoldingLowerLeft.scale(-tangensHeading))
                        .scale(1.0 / (tangensHeading * tangensHeading + 1));
                horizontalSizeViewport = fromMapBoundsCornerToLowerLeftOfViewportBounds.scale(1.0/Math.sin(heading.getRadians()));
                verticalSizeViewport = lengthOfMapBoundsEdgeHoldingLowerLeft.add(fromMapBoundsCornerToLowerLeftOfViewportBounds.scale(-1.0)).scale(1.0/Math.cos(heading.getRadians()));
                viewportLowerLeft = mapBoundsCornersClockwise[sectorIndex].translateGreatCircle(currentMapBoundsEdgeBearing, fromMapBoundsCornerToLowerLeftOfViewportBounds);
            } else if (heading.getDegrees() < 90 + SECTOR_BOUNDARY_LIMIT_DEGREES) {
                // rotated 90deg clockwise
                viewportLowerLeft = mapBoundsCornersClockwise[(sectorIndex+1)%mapBoundsCornersClockwise.length];
                horizontalSizeViewport = lengthOfMapBoundsEdgeHoldingLowerLeft;
                verticalSizeViewport = lengthOfMapBoundsEdgeNotHoldingLowerLeft;
            }
        }
        return NonCardinalBounds.create(viewportLowerLeft, mapHeading, verticalSizeViewport, horizontalSizeViewport);
    }
    
    public static Bounds getAsBounds(LatLngBounds latLngBounds) {
        return new BoundsImpl(new DegreePosition(latLngBounds.getSouthWest().getLatitude(), latLngBounds.getSouthWest().getLongitude()),
                new DegreePosition(latLngBounds.getNorthEast().getLatitude(), latLngBounds.getNorthEast().getLongitude()));
    }
    
    public static LatLngBounds getAsLatLngBounds(Bounds bounds) {
        return LatLngBounds.newInstance(LatLng.newInstance(bounds.getSouthWest().getLatDeg(), bounds.getSouthWest().getLngDeg()),
                LatLng.newInstance(bounds.getNorthEast().getLatDeg(), bounds.getNorthEast().getLngDeg()));
    }
    
    public static Position getAsPosition(LatLng latLngPosition) {
        return new DegreePosition(latLngPosition.getLatitude(), latLngPosition.getLongitude());
    }

    public static Bounds getAsBounds(Position position) {
        return new BoundsImpl(position, position);
    }
    
    public static LatLngBounds getAsBounds(LatLng position) {
        return LatLngBounds.newInstance(position, position);
    }
    
    public static boolean contains(LatLngBounds doesOrDoesNotContain, LatLngBounds containedOrNotContained) {
        return isCrossesDateLine(doesOrDoesNotContain) == isCrossesDateLine(containedOrNotContained)
                && doesOrDoesNotContain.contains(containedOrNotContained.getNorthEast())
                && doesOrDoesNotContain.contains(containedOrNotContained.getSouthWest());
    }

    public static boolean isCrossesDateLine(LatLngBounds latLngBounds) {
        return latLngBounds.getNorthEast().getLongitude() < latLngBounds.getSouthWest().getLongitude();
    }
    
    public static LatLngBounds intersect(LatLngBounds b1, LatLngBounds other) {
        final double maxSouthLatDeg = Math.max(b1.getSouthWest().getLatitude(), other.getSouthWest().getLatitude());
        final double minNorthLatDeg = Math.min(b1.getNorthEast().getLatitude(), other.getNorthEast().getLatitude());
        final double westDeg;
        if (containsLngDeg(b1, other.getSouthWest().getLongitude())) {
            westDeg = other.getSouthWest().getLongitude();
        } else if (containsLngDeg(other, b1.getSouthWest().getLongitude())) {
            westDeg = b1.getSouthWest().getLongitude();
        } else {
            // no intersection
            westDeg = b1.getSouthWest().getLongitude();
        }
        final double eastDeg;
        if (containsLngDeg(b1, other.getNorthEast().getLongitude())) {
            eastDeg = other.getNorthEast().getLongitude();
        } else if (containsLngDeg(other, b1.getNorthEast().getLongitude())) {
            eastDeg = b1.getNorthEast().getLongitude();
        } else {
            // no intersection
            eastDeg = b1.getSouthWest().getLongitude();
        }
        return LatLngBounds.newInstance(LatLng.newInstance(maxSouthLatDeg, westDeg), LatLng.newInstance(minNorthLatDeg, eastDeg));
    }
    
    private static boolean containsLngDeg(LatLngBounds bounds, double lngDeg) {
        return spansLngDeg(bounds.getSouthWest().getLongitude(), bounds.getNorthEast().getLongitude(), lngDeg);
    }

    private static boolean spansLngDeg(double westLngDeg, double eastLngDeg, double lngDeg) {
        return isCrossingDateLine(westLngDeg, eastLngDeg)
                ? (lngDeg >= westLngDeg && lngDeg <= 180) || (lngDeg >= -180 && lngDeg <= eastLngDeg)
                : westLngDeg <= lngDeg && lngDeg <= eastLngDeg;
    }

    private static boolean isCrossingDateLine(double westLngDeg, double eastLngDeg) {
        return westLngDeg > eastLngDeg;
    }
}
