package com.sap.sailing.domain.common.impl;

import com.sap.sailing.domain.common.Bearing;

public class KnotSpeedWithBearingImpl extends AbstractSpeedWithBearingImpl {
    private static final long serialVersionUID = -2300633070560631053L;
    private final double speedInKnots;
    
    public KnotSpeedWithBearingImpl(double speedInKnots, Bearing bearing) {
        super(bearing);
        this.speedInKnots = speedInKnots;
    }
    
    @Override
    public double getKnots() {
        return speedInKnots;
    }

}
