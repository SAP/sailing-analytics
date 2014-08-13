package com.sap.sailing.domain.common.impl;

import com.sap.sailing.domain.common.AbstractBearing;
import com.sap.sailing.domain.common.Bearing;


public class DegreeBearingImpl extends AbstractBearing implements Bearing {
    private static final long serialVersionUID = -8045400378221073451L;
    private final double bearingDeg;
    /**
     * 
     * @param bearingDeg if a mount degrees over 360, then it will be mod 360 degrees
     */
    public DegreeBearingImpl(double bearingDeg) {
        super();
        this.bearingDeg = bearingDeg - 360*(int) (bearingDeg/360.);
    }

    @Override
    public double getDegrees() {
        return bearingDeg;
    }
    
}
