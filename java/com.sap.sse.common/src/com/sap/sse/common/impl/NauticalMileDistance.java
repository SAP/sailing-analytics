package com.sap.sse.common.impl;

import com.sap.sse.common.AbstractDistance;
import com.sap.sse.common.Distance;

public class NauticalMileDistance extends AbstractDistance {
    private static final long serialVersionUID = 3771473789235934127L;
    private final double nauticalMiles;
    
    public NauticalMileDistance(double nauticalMiles) {
        super();
        this.nauticalMiles = nauticalMiles;
    }

    @Override
    public double getNauticalMiles() {
        return nauticalMiles;
    }

    @Override
    public double getCentralAngleDeg() {
        return getGeographicalMiles() / 60.;
    }

    @Override
    public Distance scale(double factor) {
        return new NauticalMileDistance(factor * nauticalMiles);
    }

}
