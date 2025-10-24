package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.NonCardinalBounds;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class NonCardinalBoundsTest {
    @Test
    public void testSpecialCaseCardinalBounds() {
        final NonCardinalBounds bounds = NonCardinalBounds.create(
                new DegreePosition(0, 0),
                Bearing.NORTH,
                /* verticalSize */ new NauticalMileDistance(1),
                /* horizontalSize */ new NauticalMileDistance(1));
        final Position leftOutside = new DegreePosition(1./120., -1./120.);
        final Position topOutside = new DegreePosition(1./30., 1./120.);
        final Position rightOutside = new DegreePosition(1./120., 1./30.);
        final Position bottomOutside = new DegreePosition(-1./120., 1./120.);
        assertFalse(bounds.contains(leftOutside));
        assertFalse(bounds.contains(topOutside));
        assertFalse(bounds.contains(rightOutside));
        assertFalse(bounds.contains(bottomOutside));
        assertTrue(bounds.contains(new DegreePosition(1./120., 1./120.)));
    }
    
    @Test
    public void testExtendByPosition() {
        final NonCardinalBounds bounds = NonCardinalBounds.create(
                new DegreePosition(49, 8),
                new DegreeBearingImpl(30),
                /* verticalSize */ new NauticalMileDistance(1),
                /* horizontalSize */ new NauticalMileDistance(1));
        final Position leftOutside = new DegreePosition(49.+1./120., 8); // left of the 30deg inclined left border
        assertFalse(bounds.contains(leftOutside));
        final NonCardinalBounds extendedBounds = bounds.extend(leftOutside);
        assertTrue(extendedBounds.contains(leftOutside));
    }

    @Test
    public void testExtendByOtherBounds() {
        final NonCardinalBounds bounds1 = NonCardinalBounds.create(
                new DegreePosition(49, 8),
                new DegreeBearingImpl(30),
                /* verticalSize */ new NauticalMileDistance(1),
                /* horizontalSize */ new NauticalMileDistance(1));
        final NonCardinalBounds bounds2 = NonCardinalBounds.create(
                new DegreePosition(49, 8),
                new DegreeBearingImpl(60),
                /* verticalSize */ new NauticalMileDistance(1),
                /* horizontalSize */ new NauticalMileDistance(1));
        assertFalse(bounds1.contains(bounds2));
        assertFalse(bounds2.contains(bounds1));
        final NonCardinalBounds extendedBounds = bounds1.extend(bounds2);
        assertTrue(extendedBounds.contains(bounds1));
        assertTrue(extendedBounds.contains(bounds2));
    }
    
    @Test
    public void testExtendsByAnotherPosition() {
        final Position p = new DegreePosition(54.432300981134176, 10.18096293322742);
        final Bearing heading = new DegreeBearingImpl(247);
        final NonCardinalBounds bounds = NonCardinalBounds.create(p, heading);
        final DegreePosition extensionPosition = new DegreePosition(54.43435299675912, 10.19982498139143);
        final NonCardinalBounds extendedBounds = bounds.extend(extensionPosition);
        assertTrue(extendedBounds.contains(extensionPosition));
        assertTrue(extendedBounds.contains(p));
    }
}
