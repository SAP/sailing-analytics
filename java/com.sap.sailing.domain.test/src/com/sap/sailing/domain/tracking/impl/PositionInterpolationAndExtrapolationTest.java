package com.sap.sailing.domain.tracking.impl;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.test.PositionAssert;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public abstract class PositionInterpolationAndExtrapolationTest<FixType extends GPSFix> {
    private GPSFixTrackImpl<Object, FixType> track;
    protected TimePoint now;
    protected Position p1;
    protected Position p2;
    
    protected void setUp() {
        now = MillisecondsTimePoint.now();
        p1 = new DegreePosition(1, 1);
        p2 = new DegreePosition(1.1, 1); // that's 6 nautical miles north of p1; to get there in one hour we need a speed of 6kts traveling north
    }
    
    protected void assertPos(Position p, boolean extrapolate) {
        PositionAssert.assertPositionEquals(p, getTrack().getEstimatedPosition(now, extrapolate), /* deg delta */ 0.0005);
    }

    protected GPSFixTrack<Object, FixType> getTrack() {
        return track;
    }

    protected void setTrack(GPSFixTrackImpl<Object, FixType> track) {
        this.track = track;
    }
}
