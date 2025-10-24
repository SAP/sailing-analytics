package com.sap.sse.common.test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.ValueRangeFlexibleBoundaries;
import com.sap.sse.common.ValueRangeFlexibleBoundariesChangedListener;

public class ValueRangeFlexibleBoundariesTest {
    private static final double EPSILON = 0.00000001;
    //Create new valueRange, boundaries should be [-6,-4] and [4,6].
    private ValueRangeFlexibleBoundaries valueRange;
    private double minLeft;
    private double maxRight;
    private ValueRangeFlexibleBoundariesChangedListener listener;
    
    @BeforeEach
    public void setUp() {
        // a ten percent range at each end of an original range of length 10, so length 1 at each end,
        // spread symmetrically around the min/max value
        valueRange = new ValueRangeFlexibleBoundaries(-5.0, 5.0, 0.10, 0.5);
        minLeft = -6.0;
        maxRight = 6.0;
        listener = mock(ValueRangeFlexibleBoundariesChangedListener.class);
        valueRange.addListener(listener);
        verify(listener, times(0)).onValueRangeBoundariesChanged();
    }
    
    @Test
    public void testExceptionForGreaterMinThanMax() {
        try {
            valueRange.setMinMax(6, 5);
            fail("Excpected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    @Test
    public void testCheckIfValueIsInLeftBoundaryRangeAndUpdateIfNecessary() {
        // update left border to 0.0, that means boundaries now should be [-0.5,0.5] and [4.5,5.5]
        // because the new range has length 5, so 10% of that is .5, so the left range is -0.5..0.5
        // and the right range is 4.5..5.5. We expect the update to be delivered by exactly one
        // call to the listener
        valueRange.setMinMax(0.0, 5.0);
        double newMinLeft = -0.5;
        double newMaxRight = 5.5;
        assertEquals(newMinLeft, valueRange.getMinLeft(), EPSILON);
        assertEquals(newMaxRight, valueRange.getMaxRight(), EPSILON);
        verify(listener, times(1)).onValueRangeBoundariesChanged();
    }
    
    @Test
    public void testUpdateWithinBoundaries() {
        valueRange.setMinMax(-5.5, 5.5);
        verify(listener, times(0)).onValueRangeBoundariesChanged();
        valueRange.setMinMax(-4.5, 4.5);
        verify(listener, times(0)).onValueRangeBoundariesChanged();
    }
    
    @Test
    public void testGradualUpdateCrossingBoundariesEventuallyTriggerUpdate() {
        valueRange.setMinMax(-5.5, 5.0);
        verify(listener, times(0)).onValueRangeBoundariesChanged();
        valueRange.setMinMax(-6-EPSILON, 5.0); // just barely cross the *original* range boundary to the left,
        // thus asserting that it wasn't expanded earlier by coming closer (from -5.0 to -5.5) to its end
        verify(listener, times(1)).onValueRangeBoundariesChanged();
        verify(listener, times(1)).onValueRangeBoundariesChanged(); // ensure that calling verify doesn't clean counter
        // and now it *was* expanded to cover -6..5, so the tolerance has become slightly greater, and 6+EPSILON
        // should fit into the new bounds easily
        valueRange.setMinMax(-6-EPSILON, 6+EPSILON);
        verify(listener, times(1)).onValueRangeBoundariesChanged();
    }
    
    @Test
    public void testCheckIfValueIsInRightBoundaryRangeAndUpdateIfNecessary() {
        // update right border to 20, that means the boundaries now should be [-7.5,-2.5] and [17.5,22.5]
        valueRange.setMinMax(-5.0, 20.0);
        Double newMaxRight = 22.5;
        Double newMinLeft = -7.5;
        assertEquals(newMaxRight, (Double) valueRange.getMaxRight());
        assertEquals(newMinLeft, (Double) valueRange.getMinLeft());
        verify(listener, times(1)).onValueRangeBoundariesChanged();
    }
    
    @Test
    public void testMinimumHalfBoundaryWidth() {
        valueRange.setMinMax(5.0, 6.0);
        Double newMaxRight = 6.5;
        Double newMinLeft = 4.5;
        assertEquals(newMaxRight, (Double) valueRange.getMaxRight());
        assertEquals(newMinLeft, (Double) valueRange.getMinLeft());
        verify(listener, times(1)).onValueRangeBoundariesChanged();
    }
    
    @Test
    public void testGetMinLeft() {
        assertEquals(minLeft, valueRange.getMinLeft(), EPSILON);
    }
    
    @Test
    public void testGetMaxRight() {
        assertEquals(maxRight, valueRange.getMaxRight(), EPSILON);
    }
}
