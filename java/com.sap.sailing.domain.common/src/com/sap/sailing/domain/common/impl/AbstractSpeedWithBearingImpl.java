package com.sap.sailing.domain.common.impl;

import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sse.common.Bearing;

public abstract class AbstractSpeedWithBearingImpl extends AbstractSpeedWithAbstractBearingImpl implements SpeedWithBearing {
    private static final long serialVersionUID = -8594305027333573010L;
    /* #gwtnofinal */ private Bearing bearing;

    protected AbstractSpeedWithBearingImpl(final Bearing bearing) {
        this.bearing = bearing;
    }

    @Override
    public Bearing getBearing() {
        return bearing;
    }
}
