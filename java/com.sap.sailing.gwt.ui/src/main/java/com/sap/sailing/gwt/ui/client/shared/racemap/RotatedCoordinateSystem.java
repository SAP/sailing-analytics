package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.maps.client.base.LatLng;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * Rotates a coordinate space regarding the calculation of bearings. The rotation is defined by a new equator direction.
 * Here, 90deg would keep the usual equator direction. The coordinate system is fixed once constructed, so its rotation
 * parameters cannot be altered. The rationale behind this is to force clients to re-do all the mappings done so far
 * because inconsistencies would result if over time positions and bearings are mapped through different
 * transformations, yet displayed together.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class RotatedCoordinateSystem implements CoordinateSystem {
    private final Bearing rotationAngle;
    
    /**
     * The {@link #rotationAngle}'s {@link Bearing#getDegrees() degrees} value, for faster mapping in case of
     * <code>double</code> degree values being provided to {@link #mapDegreeBearing(double)}.
     */
    private final double rotationAngleInDegrees;
    
    /**
     * @param equator
     *            the bearing to which to map the equator; 90deg east would give the default equator direction.
     */
    public RotatedCoordinateSystem(Bearing equator) {
        super();
        rotationAngle = equator.getDifferenceTo(new DegreeBearingImpl(90));
        rotationAngleInDegrees = rotationAngle.getDegrees();
    }

    @Override
    public Position getPosition(LatLng p) {
        return new DegreePosition(p.getLatitude(), p.getLongitude());
    }

    @Override
    public Bearing map(Bearing bearing) {
        return bearing;
    }

    @Override
    public double mapDegreeBearing(double trueBearingInDegrees) {
        return (trueBearingInDegrees + rotationAngleInDegrees) % 360.;
    }

    @Override
    public LatLng toLatLng(Position position) {
        return LatLng.newInstance(position.getLatDeg(), position.getLngDeg());
    }
}
