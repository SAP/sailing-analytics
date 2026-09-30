package com.sap.sse.common.impl;

import com.sap.sse.common.AbstractPosition;

public class RadianPosition extends AbstractPosition {
    private static final long serialVersionUID = -8488453506845560385L;
    private final double latRad;
    private final double lngRad;
    
    public RadianPosition(double latRad, double lngRad) {
        super();
        this.latRad = latRad;
        this.lngRad = lngRad;
    }

    @Override
    public double getLatRad() {
        return latRad;
    }
    
    @Override
    public double getLngRad() {
        return lngRad;
    }

}
