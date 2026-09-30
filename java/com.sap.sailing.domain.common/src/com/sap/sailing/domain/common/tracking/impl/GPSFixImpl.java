package com.sap.sailing.domain.common.tracking.impl;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Equality / hash code are based on {@link #getPosition()}'s and {@link #getTimePoint()}'s equality / hash code.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class GPSFixImpl extends AbstractGPSFixImpl implements GPSFix {
    private static final long serialVersionUID = -368671632334748334L;
    private final Position position;
    private final TimePoint timePoint;
    
    public GPSFixImpl(Position position, TimePoint timePoint) {
        super();
        this.position = position;
        this.timePoint = timePoint;
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
    public String toString() {
        return getTimePoint()+": "+getPosition();
    }
    
    public static GPSFixImpl create(double lonDeg, double latDeg, long timeMillis) {
        return new GPSFixImpl(new DegreePosition(latDeg, lonDeg), new MillisecondsTimePoint(timeMillis));
    }
}
