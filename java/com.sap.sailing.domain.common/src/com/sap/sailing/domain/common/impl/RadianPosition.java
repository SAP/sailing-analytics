package com.sap.sailing.domain.common.impl;

import com.sap.sailing.domain.common.AbstractPosition;

public class RadianPosition extends AbstractPosition {
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
