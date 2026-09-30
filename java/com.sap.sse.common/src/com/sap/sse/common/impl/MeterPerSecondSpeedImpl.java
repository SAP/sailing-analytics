package com.sap.sse.common.impl;

import com.sap.sse.common.AbstractSpeedImpl;
import com.sap.sse.common.Speed;

public class MeterPerSecondSpeedImpl extends AbstractSpeedImpl implements Speed {
    private static final long serialVersionUID = -524654796500981303L;
    private final double metersPerSecond;
    
    public MeterPerSecondSpeedImpl(double metersPerSecond) {
        this.metersPerSecond = metersPerSecond;
    }
    
    @Override
    public double getMetersPerSecond() {
        return metersPerSecond;
    }

    @Override
    public double getKilometersPerHour() {
        return metersPerSecond*3.6;
    }
}
