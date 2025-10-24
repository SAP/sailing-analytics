package com.sap.sailing.nmeaconnector.impl;

import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.nmeaconnector.TimedSpeedWithBearing;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.TimePoint;

/**
 * Can be used as a wind vector in a {@code Track} because it has a {@link TimePoint}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class KnotSpeedWithBearingAndTimepoint extends KnotSpeedWithBearingImpl implements TimedSpeedWithBearing {
    private static final long serialVersionUID = -627441174825774742L;
    private final TimePoint timePoint;

    public KnotSpeedWithBearingAndTimepoint(TimePoint timePoint, double speedInKnots, Bearing bearing) {
        super(speedInKnots, bearing);
        this.timePoint = timePoint;
    }

    @Override
    public TimePoint getTimePoint() {
        return timePoint;
    }
    
    @Override
    public String toString() {
        return getTimePoint().toString()+": "+super.toString();
    }
}
