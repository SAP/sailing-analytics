package com.sap.sailing.domain.test;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixTrackImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class MarkPositionTimeFilterTest {
    private DynamicTrackedRace trackedRace;
    private MarkImpl m;
    private DynamicGPSFixTrackImpl<Mark> track;
    
    @BeforeEach
    public void setUp() {
        trackedRace = mock(DynamicTrackedRaceImpl.class);
        final TimePoint startOfTracking = MillisecondsTimePoint.now();
        m = new MarkImpl("Test Mark");
        track = new DynamicGPSFixTrackImpl<Mark>(m, /* millisecondsOverWhichToAverage */ 5000);
        when(trackedRace.getOrCreateTrack(m)).thenReturn(track);
        when(trackedRace.getStartOfTracking()).thenReturn(startOfTracking);
        doCallRealMethod().when(trackedRace).recordFix(same(m), (GPSFixMoving) any(), anyBoolean());
        doCallRealMethod().when(trackedRace).isWithinStartAndEndOfTracking(any());
    }
    
    @Test
    public void generalSetupTest() {
        assertSame(track, trackedRace.getOrCreateTrack(m));
    }
    
    @Test
    public void testAddFixForMark() {
        trackedRace.recordFix(m, new GPSFixMovingImpl(new DegreePosition(12, 13), MillisecondsTimePoint.now(),
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null), /* onlyWhenInTrackingInterval */ true);
        track.lockForRead();
        try {
            assertEquals(1, track.getRawFixes().size());
        } finally {
            track.unlockAfterRead();
        }
    }

    @Test
    public void testAddFixForMarkWithinTrackingTimeRange() {
        TimePoint start = MillisecondsTimePoint.now();
        TimePoint fix = start.plus(10000);
        TimePoint end = fix.plus(10000);
        when(trackedRace.getStartOfTracking()).thenReturn(start);
        when(trackedRace.getEndOfTracking()).thenReturn(end);
        trackedRace.recordFix(m, new GPSFixMovingImpl(new DegreePosition(12, 13), fix,
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null), /* onlyWhenInTrackingInterval */ true);
        track.lockForRead();
        try {
            assertEquals(1, track.getRawFixes().size());
        } finally {
            track.unlockAfterRead();
        }
    }

    @Test
    public void testAddFixForMarkOutsideTrackingTimeRange() {
        TimePoint start = MillisecondsTimePoint.now();
        TimePoint end = start.plus(10000);
        TimePoint fix = end.plus(10000);
        when(trackedRace.getStartOfTracking()).thenReturn(start);
        when(trackedRace.getEndOfTracking()).thenReturn(end);
        trackedRace.recordFix(m, new GPSFixMovingImpl(new DegreePosition(12, 13), fix,
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null), /* onlyWhenInTrackingInterval */ true);
        track.lockForRead();
        try {
            assertTrue(track.getRawFixes().isEmpty());
        } finally {
            track.unlockAfterRead();
        }
    }

    @Test
    public void testAddFixForMarkFirstOutsideTrackingTimeRangeThenWithin() {
        TimePoint start = MillisecondsTimePoint.now();
        TimePoint end = start.plus(10000);
        TimePoint fix = end.plus(10000);
        when(trackedRace.getStartOfTracking()).thenReturn(start);
        when(trackedRace.getEndOfTracking()).thenReturn(end);
        trackedRace.recordFix(m, new GPSFixMovingImpl(new DegreePosition(12, 13), fix,
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null), /* onlyWhenInTrackingInterval */ true);
        track.lockForRead();
        try {
            assertTrue(track.getRawFixes().isEmpty());
        } finally {
            track.unlockAfterRead();
        }
        when(trackedRace.getEndOfTracking()).thenReturn(fix.plus(10000));
        trackedRace.recordFix(m, new GPSFixMovingImpl(new DegreePosition(12, 13), fix,
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123)), /* optionalTrueHeading */ null), /* onlyWhenInTrackingInterval */ true);
        track.lockForRead();
        try {
            assertEquals(1, track.getRawFixes().size());
        } finally {
            track.unlockAfterRead();
        }
    }
}
