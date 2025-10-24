package com.sap.sse.common.test;

import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.TimeRangeImpl;

public class TimeTestHelpers {

    protected static TimeRange create(Long from, Long toExclusive) {
        return new TimeRangeImpl(from==null?null:new MillisecondsTimePoint(from), toExclusive==null?null:new MillisecondsTimePoint(toExclusive));
    }

    protected static TimeRange create(int from, int toExclusive) {
        return create((long) from, (long) toExclusive);
    }

    protected static TimeRange create(int from, long toExclusive) {
        return create((long) from, toExclusive);
    }

    protected static TimeRange create(long from, int toExclusive) {
        return create(from, (long) toExclusive);
    }

    protected static TimePoint create(long millis) {
        return new MillisecondsTimePoint(millis);
    }

}
