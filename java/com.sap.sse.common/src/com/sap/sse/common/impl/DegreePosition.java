package com.sap.sse.common.impl;

import com.sap.sse.common.AbstractPosition;

public class DegreePosition extends AbstractPosition {
    private static final long serialVersionUID = 2060676561122615530L;
    private final double lat;
    private final double lng;
    
    public DegreePosition(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    @Override
    public double getLatDeg() {
        return lat;
    }

    @Override
    public double getLngDeg() {
        return lng;
    }
}
