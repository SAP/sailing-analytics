package com.sap.sse.common.impl;

import com.sap.sse.common.AbstractDistance;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Mile;

public class CentralAngleDistance extends AbstractDistance {
    private static final long serialVersionUID = -388639688376644968L;
    private final double centralAngleRad;
    
    public CentralAngleDistance(double centralAngleRad) {
        super();
        this.centralAngleRad = centralAngleRad;
    }

    @Override
    public double getMeters() {
        return getGeographicalMiles() * Mile.METERS_PER_GEOGRAPHICAL_MILE;
    }

    @Override
    public double getCentralAngleRad() {
        return centralAngleRad;
    }

    @Override
    public double getGeographicalMiles() {
        return getCentralAngleDeg() * 60.;
    }

    @Override
    public Distance scale(double factor) {
        return new CentralAngleDistance(factor*getCentralAngleRad());
    }
}
