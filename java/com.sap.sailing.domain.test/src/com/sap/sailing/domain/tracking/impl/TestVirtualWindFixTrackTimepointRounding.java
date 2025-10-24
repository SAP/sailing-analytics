package com.sap.sailing.domain.tracking.impl;

import java.util.NavigableSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sse.common.TimePoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestVirtualWindFixTrackTimepointRounding {
    private static final int RESOLUTION_IN_MILLIS = 1000;
    private MyVirtualWindFixesAsNavigableSet virtualWindFixes;
    
    private static class MyVirtualWindFixesAsNavigableSet extends VirtualWindFixesAsNavigableSet {
        private static final long serialVersionUID = 1L;

        public MyVirtualWindFixesAsNavigableSet() {
            this(mock(TrackedRace.class));
        }
        
        private MyVirtualWindFixesAsNavigableSet(TrackedRace trackedRace) {
            super(null, trackedRace, RESOLUTION_IN_MILLIS);
            when(trackedRace.getTimePointOfNewestEvent()).thenReturn(TimePoint.now());
        }
        
        @Override
        protected Wind getWind(Position p, TimePoint timePoint) {
            return null;
        }

        @Override
        protected NavigableSet<Wind> createSubset(WindTrack track, TrackedRace trackedRace, TimePoint from,
                TimePoint to) {
            return null;
        }

        @Override
        protected TimePoint higherToResolution(TimePoint t) {
            return super.higherToResolution(t);
        }

        @Override
        protected TimePoint lowerToResolution(TimePoint t) {
            return super.lowerToResolution(t);
        }

        @Override
        protected TimePoint ceilingToResolution(TimePoint t) {
            return super.ceilingToResolution(t);
        }

        @Override
        protected TimePoint floorToResolution(TimePoint t) {
            return super.floorToResolution(t);
        }
    }
    
    @BeforeEach
    public void setUp() {
        final TrackedRace trackedRace = mock(TrackedRace.class);
        when(trackedRace.getTimePointOfNewestEvent()).thenReturn(TimePoint.now());
        virtualWindFixes = new MyVirtualWindFixesAsNavigableSet();
    }
    
    @Test
    public void testLoweringBeginningOfEpoch() {
        final TimePoint epoch = TimePoint.of(0);
        final TimePoint lower = virtualWindFixes.lowerToResolution(epoch);
        assertNotEquals(epoch, lower);
        assertTrue(epoch.compareTo(lower) > 0);
    }

    @Test
    public void testLoweringOneTickAfterEpoch() {
        final TimePoint oneTickAfterEpoch = TimePoint.of(RESOLUTION_IN_MILLIS);
        final TimePoint lower = virtualWindFixes.lowerToResolution(oneTickAfterEpoch);
        assertNotEquals(oneTickAfterEpoch, lower);
        assertEquals(TimePoint.of(0), lower);
    }

    @Test
    public void testLoweringBeforefEpoch() {
        final TimePoint beforeEpoch = TimePoint.of(-10);
        final TimePoint lower = virtualWindFixes.lowerToResolution(beforeEpoch);
        assertNotEquals(beforeEpoch, lower);
        assertTrue(beforeEpoch.compareTo(lower) > 0);
    }

    @Test
    public void testFloorBeforeEpoch() {
        final TimePoint beforeEpoch = TimePoint.of(-10);
        final TimePoint lower = virtualWindFixes.floorToResolution(beforeEpoch);
        assertNotEquals(beforeEpoch, lower);
        assertTrue(beforeEpoch.compareTo(lower) > 0);
    }

    @Test
    public void testFloorAfterEpoch() {
        final TimePoint beforeEpoch = TimePoint.of(10);
        final TimePoint lower = virtualWindFixes.floorToResolution(beforeEpoch);
        assertNotEquals(beforeEpoch, lower);
        assertTrue(beforeEpoch.compareTo(lower) > 0);
        assertEquals(TimePoint.of(0), lower);
    }

    @Test
    public void testFloorAtBeginningOfEpoch() {
        final TimePoint beginningOfEpoch = TimePoint.of(0);
        final TimePoint floor = virtualWindFixes.floorToResolution(beginningOfEpoch);
        assertEquals(beginningOfEpoch, floor);
    }

    @Test
    public void testCeilingBeforeEpoch() {
        final TimePoint beforeEpoch = TimePoint.of(-RESOLUTION_IN_MILLIS-1);
        final TimePoint higher = virtualWindFixes.ceilingToResolution(beforeEpoch);
        assertEquals(TimePoint.of(-RESOLUTION_IN_MILLIS), higher);
        assertTrue(beforeEpoch.compareTo(higher) < 0);
    }

    @Test
    public void testCeilingAtBeginningOfEpoch() {
        final TimePoint epoch = TimePoint.of(0);
        final TimePoint ceil = virtualWindFixes.ceilingToResolution(epoch);
        assertEquals(epoch, ceil);
    }

    @Test
    public void testCeilingAfterEpoch() {
        final TimePoint afterEpoch = TimePoint.of(RESOLUTION_IN_MILLIS-1);
        final TimePoint higher = virtualWindFixes.ceilingToResolution(afterEpoch);
        assertEquals(TimePoint.of(RESOLUTION_IN_MILLIS), higher);
        assertTrue(afterEpoch.compareTo(higher) < 0);
    }

    @Test
    public void testHigherBeginningOfEpoch() {
        final TimePoint epoch = TimePoint.of(0);
        final TimePoint higher = virtualWindFixes.higherToResolution(epoch);
        assertNotEquals(epoch, higher);
        assertTrue(epoch.compareTo(higher) < 0);
    }

    @Test
    public void testHigherBeforeEpoch() {
        final TimePoint beforeEpoch = TimePoint.of(-RESOLUTION_IN_MILLIS-1);
        final TimePoint higher = virtualWindFixes.higherToResolution(beforeEpoch);
        assertNotEquals(beforeEpoch, higher);
        assertTrue(beforeEpoch.compareTo(higher) < 0);
        assertEquals(TimePoint.of(-RESOLUTION_IN_MILLIS), higher);
    }

    @Test
    public void testHigherOneTickBeforeEpoch() {
        final TimePoint beforeEpoch = TimePoint.of(-RESOLUTION_IN_MILLIS);
        final TimePoint higher = virtualWindFixes.higherToResolution(beforeEpoch);
        assertEquals(TimePoint.of(0), higher);
    }

    @Test
    public void testHigherAfterEpoch() {
        final TimePoint afterEpoch = TimePoint.of(RESOLUTION_IN_MILLIS/2);
        final TimePoint higher = virtualWindFixes.higherToResolution(afterEpoch);
        assertNotEquals(afterEpoch, higher);
        assertTrue(afterEpoch.compareTo(higher) < 0);
    }

    @Test
    public void testHigherOneTickAfterEpoch() {
        final TimePoint afterEpoch = TimePoint.of(RESOLUTION_IN_MILLIS);
        final TimePoint higher = virtualWindFixes.higherToResolution(afterEpoch);
        assertEquals(TimePoint.of(2*RESOLUTION_IN_MILLIS), higher);
    }
}
