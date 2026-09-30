package com.sap.sailing.declination.impl;

import com.sap.sailing.declination.Declination;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class DeclinationRecordImpl extends AbstractDeclinationRecord implements Declination {
    private static final long serialVersionUID = 6918630656182340186L;
    private final Bearing annualChange;
    
    public DeclinationRecordImpl(Position position, TimePoint timePoint, Bearing bearing, Bearing annualChange) {
        super(position, timePoint, bearing);
        this.annualChange = annualChange;
    }

    @Override
    public Bearing getAnnualChange() {
        return annualChange;
    }
    
    @Override
    public Bearing getBearingCorrectedTo(TimePoint timePoint) {
        return new DegreeBearingImpl(getBearing().getDegrees() + getAnnualChange().getDegrees()
                * (timePoint.asMillis() - getTimePoint().asMillis()) / 1000 /* s *// 3600 /* h *// 24 /* days *// 365);
    }

    @Override
    public String toString() {
        return ""+getTimePoint()+"@"+getPosition()+": "+getBearing()+", "+getAnnualChange()+"/year";
    }
}
