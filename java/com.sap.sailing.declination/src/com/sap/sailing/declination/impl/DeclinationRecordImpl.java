package com.sap.sailing.declination.impl;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sse.common.TimePoint;

public class DeclinationRecordImpl implements Declination {
    private static final long serialVersionUID = 6918630656182340186L;
    private final Position position;
    private final TimePoint timePoint;
    private final Bearing bearing;
    private final Bearing annualChange;
    public DeclinationRecordImpl(Position position, TimePoint timePoint, Bearing bearing, Bearing annualChange) {
        super();
        this.position = position;
        this.timePoint = timePoint;
        this.bearing = bearing;
        this.annualChange = annualChange;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public TimePoint getTimePoint() {
        return timePoint;
    }

    @Override
    public Bearing getBearing() {
        return bearing;
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
