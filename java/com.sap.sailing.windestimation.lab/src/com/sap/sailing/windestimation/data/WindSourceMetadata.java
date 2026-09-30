package com.sap.sailing.windestimation.data;

import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

public class WindSourceMetadata {

    private final Position firstPosition;
    private final TimePoint startTime;
    private final TimePoint endTime;
    private final double samplingRate;

    public WindSourceMetadata(Position firstPosition, TimePoint startTime, TimePoint endTime, double samplingRate) {
        this.firstPosition = firstPosition;
        this.startTime = startTime;
        this.endTime = endTime;
        this.samplingRate = samplingRate;
    }

    public Position getFirstPosition() {
        return firstPosition;
    }

    public TimePoint getStartTime() {
        return startTime;
    }

    public TimePoint getEndTime() {
        return endTime;
    }

    public double getSamplingRate() {
        return samplingRate;
    }
    
}
