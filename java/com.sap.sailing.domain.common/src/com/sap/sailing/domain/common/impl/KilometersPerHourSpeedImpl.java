package com.sap.sailing.domain.common.impl;

import com.sap.sailing.domain.common.AbstractSpeedImpl;
import com.sap.sailing.domain.common.Mile;
import com.sap.sailing.domain.common.Speed;

public class KilometersPerHourSpeedImpl extends AbstractSpeedImpl implements Speed {
    private static final long serialVersionUID = -524654796500981303L;
    private final double speedInKilometersPerHour;
    
    public KilometersPerHourSpeedImpl(double speedInKilometersPerHour) {
        this.speedInKilometersPerHour = speedInKilometersPerHour;
    }
    
    @Override
    public double getKnots() {
        return getKilometersPerHour() * 1000. / Mile.METERS_PER_NAUTICAL_MILE;
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
