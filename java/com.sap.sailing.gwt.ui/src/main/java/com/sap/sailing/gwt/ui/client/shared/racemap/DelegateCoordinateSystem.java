package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.maps.client.base.LatLng;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Position;

/**
 * Holds a {@link CoordinateSystem} that can be exchanged, using {@link #setCoordinateSystem}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class DelegateCoordinateSystem implements CoordinateSystem {
    private CoordinateSystem coordinateSystem;
    
    public DelegateCoordinateSystem(CoordinateSystem coordinateSystem) {
        super();
        this.coordinateSystem = coordinateSystem;
    }

    public CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }

    public void setCoordinateSystem(CoordinateSystem coordinateSystem) {
        this.coordinateSystem = coordinateSystem;
    }

    @Override
    public Position map(Position position) {
        return coordinateSystem.map(position);
    }

    @Override
    public Bearing map(Bearing bearing) {
        return coordinateSystem.map(bearing);
    }

    @Override
    public double mapDegreeBearing(double trueBearingInDegrees) {
        return coordinateSystem.mapDegreeBearing(trueBearingInDegrees);
    }

    @Override
    public LatLng toLatLng(Position position) {
        return coordinateSystem.toLatLng(position);
    }

    @Override
    public Position getPosition(LatLng p) {
        return coordinateSystem.getPosition(p);
    }

}
