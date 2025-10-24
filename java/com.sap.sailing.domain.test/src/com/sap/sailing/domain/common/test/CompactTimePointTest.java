package com.sap.sailing.domain.common.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.tracking.impl.CompactionNotPossibleException;
import com.sap.sailing.domain.common.tracking.impl.VeryCompactGPSFixMovingImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;

public class CompactTimePointTest {
    @Test
    public void add8000MillisecondsToCompactTimePoint() throws CompactionNotPossibleException {
        final VeryCompactGPSFixMovingImpl fix = new VeryCompactGPSFixMovingImpl(new DegreePosition(49, 8), TimePoint.of(1752486600000l), null, null);
        final TimePoint later = fix.getTimePoint().plus(8000);
        assertTrue(later.after(fix.getTimePoint()));
        assertTrue(TimeRange.create(fix.getTimePoint(), later).getDuration().equals(Duration.ONE_MILLISECOND.times(8000)));
    }
}