package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.maps.client.base.LatLng;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sse.common.Bearing;

public class IdentityCoordinateSystem implements CoordinateSystem {
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
        return trueBearingInDegrees;
    }

    @Override
    public LatLng toLatLng(Position position) {
        return LatLng.newInstance(position.getLatDeg(), position.getLngDeg());
    }
}
