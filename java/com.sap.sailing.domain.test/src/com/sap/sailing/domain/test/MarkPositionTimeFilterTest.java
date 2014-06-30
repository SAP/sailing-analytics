package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixTrackImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.GPSFixMovingImpl;

public class MarkPositionTimeFilterTest {
    private DynamicTrackedRace trackedRace;
    private MarkImpl m;
    private DynamicGPSFixTrackImpl<Mark> track;
    
    @Before
    public void setUp() {
        trackedRace = mock(DynamicTrackedRaceImpl.class);
        m = new MarkImpl("Test Mark");
        track = new DynamicGPSFixTrackImpl<Mark>(m, /* millisecondsOverWhichToAverage */ 5000);
        when(trackedRace.getOrCreateTrack(m)).thenReturn(track);
        doCallRealMethod().when(trackedRace).recordFix(same(m), (GPSFixMoving) anyObject());
    }
    
    @Test
    public void generalSetupTest() {
        assertSame(track, trackedRace.getOrCreateTrack(m));
    }
    
    @Test
    public void testAddFixForMark() {
        trackedRace.recordFix(m, new GPSFixMovingImpl(new DegreePosition(12, 13), MillisecondsTimePoint.now(),
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123))));
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
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123))));
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
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123))));
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
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123))));
        track.lockForRead();
        try {
            assertTrue(track.getRawFixes().isEmpty());
        } finally {
            track.unlockAfterRead();
        }
        when(trackedRace.getEndOfTracking()).thenReturn(fix.plus(10000));
        trackedRace.recordFix(m, new GPSFixMovingImpl(new DegreePosition(12, 13), fix,
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123))));
        track.lockForRead();
        try {
            assertEquals(1, track.getRawFixes().size());
        } finally {
            track.unlockAfterRead();
        }
    }
}
