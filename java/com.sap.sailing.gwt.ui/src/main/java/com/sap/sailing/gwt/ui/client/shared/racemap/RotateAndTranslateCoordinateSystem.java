package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.maps.client.base.LatLng;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * Translates and rotates a coordinate space. The translation is specified by providing the position
 * to which to map {lat: 0, lng: 0}. The rotation is defined by a new equator direction. Here, 90deg would
 * keep the usual equator direction. The coordinate system is fixed once constructed, so its translation
 * and rotation parameters cannot be altered. The rationale behind this is to force clients to re-do all
 * the mappings done so far because inconsistencies would result if over time positions and bearings are
 * mapped through different transformations, yet displayed together.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class RotateAndTranslateCoordinateSystem implements CoordinateSystem {
    private final Position zeroZero;
    private final Bearing equator;
    private final Bearing rotationAngle;
    private final static Position ZERO_ZERO = new DegreePosition(0, 0);
    private final static Bearing EQUATOR_BEARING = new DegreeBearingImpl(90);
    
    /**
     * The {@link #rotationAngle}'s {@link Bearing#getDegrees() degrees} value, for faster mapping in case of
     * <code>double</code> degree values being provided to {@link #mapDegreeBearing(double)}.
     */
    private final double rotationAngleInDegrees;
    
    /**
     * @param zeroZero
     *            where to map {lat: 0, lng: 0} to in this coordinate system
     * @param equator
     *            the bearing to which to map the equator; 90deg east would give the default equator direction.
     */
    public RotateAndTranslateCoordinateSystem(Position zeroZero, Bearing equator) {
        super();
        this.zeroZero = zeroZero;
        this.equator = equator;
        rotationAngle = equator.getDifferenceTo(new DegreeBearingImpl(90));
        rotationAngleInDegrees = rotationAngle.getDegrees();
    }

    @Override
    public Position getPosition(LatLng p) {
        final Position mapped = new DegreePosition(p.getLatitude(), p.getLongitude());
        return mapped.getTargetCoordinates(/* local origin */ ZERO_ZERO, /* local equator bearing */ EQUATOR_BEARING, zeroZero, equator);
    }

    @Override
    public Bearing map(Bearing bearing) {
        return bearing.add(rotationAngle);
    }

    @Override
    public double mapDegreeBearing(double trueBearingInDegrees) {
        return (trueBearingInDegrees + rotationAngleInDegrees) % 360.;
    }

    @Override
    public LatLng toLatLng(Position position) {
        final Position mapped = position.getLocalCoordinates(zeroZero, equator);
        return LatLng.newInstance(mapped.getLatDeg(), mapped.getLngDeg());
    }
}
