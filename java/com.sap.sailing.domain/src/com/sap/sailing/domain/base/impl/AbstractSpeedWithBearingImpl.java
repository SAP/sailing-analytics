package com.sap.sailing.domain.base.impl;

import com.sap.sailing.domain.base.SpeedWithBearing;
import com.sap.sailing.domain.common.Bearing;

public abstract class AbstractSpeedWithBearingImpl extends AbstractSpeedWithAbstractBearingImpl implements SpeedWithBearing {
    private static final long serialVersionUID = -8594305027333573010L;
    private final Bearing bearing;
    
    protected AbstractSpeedWithBearingImpl(Bearing bearing) {
        this.bearing = bearing;
    }

    @Override
    public Bearing getBearing() {
        return bearing;
    }

}
