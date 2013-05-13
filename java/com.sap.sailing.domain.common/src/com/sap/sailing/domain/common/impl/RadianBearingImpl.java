package com.sap.sailing.domain.common.impl;

import com.sap.sailing.domain.common.AbstractBearing;
import com.sap.sailing.domain.common.Bearing;

public class RadianBearingImpl extends AbstractBearing implements Bearing {
    private static final long serialVersionUID = 5044142324540650696L;
    private final double bearingRad;
    
    public RadianBearingImpl(double bearingRad) {
        super();
        this.bearingRad = bearingRad - 2*Math.PI * (int) (bearingRad/2./Math.PI);
    }

    @Override
    public double getDegrees() {
        return getRadians() / Math.PI * 180.;
    }

    @Override
    public double getRadians() {
        return bearingRad;
    }

}
