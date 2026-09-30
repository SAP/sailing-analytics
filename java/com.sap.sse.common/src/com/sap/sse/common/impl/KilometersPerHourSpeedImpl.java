package com.sap.sse.common.impl;

import com.sap.sse.common.AbstractSpeedImpl;
import com.sap.sse.common.Speed;

public class KilometersPerHourSpeedImpl extends AbstractSpeedImpl implements Speed {
    private static final long serialVersionUID = -524654796500981303L;
    private final double speedInKilometersPerHour;
    
    public KilometersPerHourSpeedImpl(double speedInKilometersPerHour) {
        this.speedInKilometersPerHour = speedInKilometersPerHour;
    }
    
    @Override
    public double getMetersPerSecond() {
        return getKilometersPerHour() / 3.6;
    }

    @Override
    public double getKilometersPerHour() {
        return speedInKilometersPerHour;
    }
}
