package com.sap.sailing.domain.tracking.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sse.common.Distance;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TrackedLegTest {
    
    private TrackedLegImpl trackedLegImpl;
    private DynamicTrackedRaceImpl dynamicTrackedRace;
    private Leg leg;
    
    @BeforeEach
    public void setUp() {
        leg = Mockito.mock(Leg.class);
        dynamicTrackedRace = Mockito.mock(DynamicTrackedRaceImpl.class);
        trackedLegImpl = new TrackedLegImpl(dynamicTrackedRace, leg, Collections.<Competitor>emptyList());
    }
    
    @Test
    public void testEquidistantSectionsOfLeg() {
        TimePoint at = new MillisecondsTimePoint(1000);
        Waypoint start = new WaypointImpl(DomainFactory.INSTANCE.getOrCreateMark("mark1"));
        Waypoint finish = new WaypointImpl(DomainFactory.INSTANCE.getOrCreateMark("mark2"));
        Mockito.when(leg.getFrom()).thenReturn(start);
        Mockito.when(leg.getTo()).thenReturn(finish);
        final Position startPos = new DegreePosition(1, 2);
        Mockito.when(dynamicTrackedRace.getApproximatePosition(start, at)).thenReturn(startPos);
        final Position finishPos = new DegreePosition(2, 5);
        Mockito.when(dynamicTrackedRace.getApproximatePosition(finish, at)).thenReturn(finishPos);
        final int TEN = 10;
        Iterable<Position> positions10 = trackedLegImpl.getEquidistantSectionsOfLeg(at, TEN);
        assertEquals(TEN, Util.stream(positions10).count());
        assertPositionsOnGreatCircle(startPos, finishPos, positions10);
        final int FIFTEEN = 15;
        Iterable<Position> positions15 = trackedLegImpl.getEquidistantSectionsOfLeg(at, FIFTEEN);
        assertEquals(FIFTEEN, Util.stream(positions15).count());
        assertPositionsOnGreatCircle(startPos, finishPos, positions15);
        final int HUNDRED = 100;
        Iterable<Position> positions100 = trackedLegImpl.getEquidistantSectionsOfLeg(at, HUNDRED);
        assertEquals(HUNDRED, Util.stream(positions100).count());
        assertPositionsOnGreatCircle(startPos, finishPos, positions100);
    }

    private void assertPositionsOnGreatCircle(final Position startPos, final Position finishPos,
            Iterable<Position> positions) {
        Distance firstDistance = null;
        Position lastPosition = null;
        for (final Position p : positions) {
            assertTrue(p.getDistanceToLine(startPos, finishPos).getMeters() < 1);
            if (firstDistance == null && lastPosition != null) {
                firstDistance = lastPosition.getDistance(p);
            } else {
                if (lastPosition != null) {
                    assertEquals(firstDistance.getMeters(), lastPosition.getDistance(p).getMeters(), 0.1);
                }
            }
            lastPosition = p;
        }
    }

}
