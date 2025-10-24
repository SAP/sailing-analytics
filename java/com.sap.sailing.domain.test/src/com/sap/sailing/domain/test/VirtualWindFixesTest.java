package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.CombinedWindAsNavigableSet;
import com.sap.sailing.domain.tracking.impl.CombinedWindTrackImpl;
import com.sap.sailing.domain.tracking.impl.VirtualWindFixesAsNavigableSet;
import com.sap.sse.InvalidDateException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.util.DateParser;

/**
 * Tests for {@link VirtualWindFixesAsNavigableSet}, based mainly on bug 4063.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class VirtualWindFixesTest {
    private CombinedWindTrackImpl windTrack;
    private CombinedWindAsNavigableSet virtualSet;
    private TrackedRace trackedRace;
    private TimePoint oddStart;
    private TimePoint evenStart;
    
    @BeforeEach
    public void setUp() throws InvalidDateException {
        oddStart = new MillisecondsTimePoint(DateParser.parse("2017-02-15T09:59:59.9+01:00"));
        evenStart = new MillisecondsTimePoint(DateParser.parse("2017-02-15T10:00:00+01:00"));
        trackedRace = mock(TrackedRace.class);
        RaceDefinition race = mock(RaceDefinition.class);
        when(race.getName()).thenReturn("Race Name");
        when(trackedRace.getRace()).thenReturn(race);
        when(trackedRace.getStartOfRace()).thenReturn(oddStart);
        when(trackedRace.getWind(any(), any())).thenAnswer(invocation->new WindImpl(
                (Position) invocation.getArguments()[0], (TimePoint) invocation.getArguments()[1],
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(123))));
        windTrack = new CombinedWindTrackImpl(trackedRace, /* baseConfidence */ 0.9) {
            private static final long serialVersionUID = 123L;

            @Override
            protected CombinedWindAsNavigableSet createVirtualInternalRawFixes(TrackedRace trackedRace) {
                virtualSet = new CombinedWindAsNavigableSet(windTrack, trackedRace, 10000l) {
                    private static final long serialVersionUID = -8903939477027009728L;

                    @Override
                    protected CombinedWindTrackImpl getTrack() {
                        return windTrack;
                    }
                };
                return virtualSet;
            }
        };
        
    }

    @Test
    public void testSubsetWithInclusiveEnd() {
        windTrack.lockForRead();
        try {
            final Iterable<Wind> fixes = windTrack.getFixes(evenStart, /* fromInclusive */ true,
                            evenStart.plus(virtualSet.getResolutionInMilliseconds()), /* toInclusive */ true);
            assertEquals(2, Util.size(fixes));
            Wind wind1 = fixes.iterator().next();
            assertEquals(evenStart, wind1.getTimePoint());
            Wind wind2 = Util.get(fixes, 1);
            assertEquals(evenStart.plus(virtualSet.getResolutionInMilliseconds()), wind2.getTimePoint());
        } finally {
            windTrack.unlockAfterRead();
        }
    }

    @Test
    public void testSubsetWithInclusiveEndAndExclusiveStart() {
        windTrack.lockForRead();
        try {
            final Iterable<Wind> fixes = windTrack.getFixes(evenStart, /* fromInclusive */ false,
                            evenStart.plus(virtualSet.getResolutionInMilliseconds()), /* toInclusive */ true);
            assertEquals(1, Util.size(fixes));
            Wind wind1 = fixes.iterator().next();
            assertEquals(evenStart.plus(virtualSet.getResolutionInMilliseconds()), wind1.getTimePoint());
        } finally {
            windTrack.unlockAfterRead();
        }
    }

    @Test
    public void testSubset() {
        windTrack.lockForRead();
        try {
            final Iterable<Wind> fixes = windTrack.getFixes(evenStart, /* fromInclusive */ true,
                            evenStart.plus(virtualSet.getResolutionInMilliseconds()), /* toInclusive */ false);
            assertEquals(1, Util.size(fixes));
            Wind wind = fixes.iterator().next();
            assertEquals(evenStart, wind.getTimePoint());
        } finally {
            windTrack.unlockAfterRead();
        }
    }

    @Test
    public void testSubsetWithExclusiveEndButToIsSlightlyAfterSecondFix() {
        windTrack.lockForRead();
        try {
            final Iterable<Wind> fixes = windTrack.getFixes(evenStart, /* fromInclusive */ true,
                            evenStart.plus(virtualSet.getResolutionInMilliseconds()).plus(1), /* toInclusive */ false);
            assertEquals(2, Util.size(fixes));
            Wind wind1 = fixes.iterator().next();
            assertEquals(evenStart, wind1.getTimePoint());
            Wind wind2 = Util.get(fixes, 1);
            assertEquals(evenStart.plus(virtualSet.getResolutionInMilliseconds()), wind2.getTimePoint());
        } finally {
            windTrack.unlockAfterRead();
        }
    }
}
