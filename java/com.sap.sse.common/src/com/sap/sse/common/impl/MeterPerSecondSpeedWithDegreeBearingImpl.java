package com.sap.sse.common.impl;

import com.sap.sse.common.Bearing;

public class MeterPerSecondSpeedWithDegreeBearingImpl extends AbstractSpeedWithAbstractBearingImpl {
    private static final long serialVersionUID = -524654796500981303L;
    private final double metersPerSecond;
    private final Bearing bearing;
    
    public MeterPerSecondSpeedWithDegreeBearingImpl(double metersPerSecond, Bearing bearing) {
        this.metersPerSecond = metersPerSecond;
        this.bearing = bearing;
    }
    
    @Override
    public double getMetersPerSecond() {
        return metersPerSecond;
    }

    @Override
    public double getKilometersPerHour() {
        return metersPerSecond*3.6;
    }

    @Override
    public Bearing getBearing() {
        return bearing;
    }
}
