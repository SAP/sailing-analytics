package com.sap.sailing.domain.igtimiadapter;

import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;

public interface HasStartAndEndTime {
    TimePoint getEndTime();

    TimePoint getStartTime();
    
    default TimeRange getTimeRange() {
        return TimeRange.create(getStartTime(), getEndTime());
    }
}
