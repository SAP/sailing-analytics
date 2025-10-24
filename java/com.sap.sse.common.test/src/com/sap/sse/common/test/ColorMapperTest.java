package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.ColorMapper;
import com.sap.sse.common.ColorMapper.ValueSpreader;
import com.sap.sse.common.ColorMapperChangedListener;
import com.sap.sse.common.ValueRangeFlexibleBoundaries;

public class ColorMapperTest {
    private static final double EPSILON = 0.00000001;
    ColorMapper colorMapper;
    ColorMapperChangedListener listener;
    ValueRangeFlexibleBoundaries valueRange;
    Set<Integer> valueSet;
    Set<String> colorSet;

    @BeforeEach
    public void setUp() {
        valueRange = new ValueRangeFlexibleBoundaries(-5.0, 5.0, 0.0, 0.0);
        colorMapper = new ColorMapper(valueRange, false, ValueSpreader.LINEAR);
        listener = mock(ColorMapperChangedListener.class);
        colorMapper.addListener(listener);
        valueSet = new HashSet<>();
        updateValueSet(valueSet);
        colorSet = new HashSet<>();
        fillColorSet();
        
    }

    public void updateValueSet(Set<Integer> valueSet) {
        valueSet.clear();
        for (int i = (int) Math.round(valueRange.getMinLeft()); i <= (int) Math.round(valueRange.getMaxRight()); i++) {
            valueSet.add(i);
        }
    }

    public void fillColorSet() {
        for (int i = 0; i <= ColorMapper.MAX_HUE; i++) {
            colorSet.add("hsl(" + i + ", 100%, 50%)");
        }
    }
    
    @Test
    public void testBasicOperation() {
        // MinLeft = -5 && MaxRight = 5
        colorMapper.setGrey(false);
        // Below lower bound
        assertEquals("hsl("+ColorMapper.MAX_HUE+", 100%, 50%)", colorMapper.getColor(-6));
        // At lower bound
        assertEquals("hsl("+ColorMapper.MAX_HUE+", 100%, 50%)", colorMapper.getColor(-5));
        // Mid range
        assertEquals("hsl("+Math.round((double) ColorMapper.MAX_HUE/2.0)+", 100%, 50%)", colorMapper.getColor(0));
        // At upper bound
        assertEquals("hsl(0, 100%, 50%)", colorMapper.getColor(5));
        // Above upper bound
        assertEquals("hsl(0, 100%, 50%)", colorMapper.getColor(6));
    }
    
    @Test
    public void testForValueOutOfBoundaries() {
        assertEquals("hsl("+ColorMapper.MAX_HUE+", 100%, 50%)", colorMapper.getColor(valueRange.getMinLeft() - EPSILON));
        assertEquals("hsl(0, 100%, 50%)", colorMapper.getColor(valueRange.getMaxRight() + EPSILON));
    }

    @Test
    public void testGetColorColored() {
        // first set the color and verify listener will be notified
        colorMapper.setGrey(false);
        verify(listener, times(1)).onColorMappingChanged();
        // Check for each value in valueSet that getColor() returns a color within the colorSet.
        for (int i : valueSet) {
            assertTrue(colorSet.contains(colorMapper.getColor(i)));
        }
    }

    @Test
    public void changeValueRangeAndTestGetColorColored() {
        valueRange.setMinMax(12.0, 26.0);
        // verify Listener
        verify(listener, times(1)).onColorMappingChanged();
        updateValueSet(valueSet);
        // check if new values are within the colorSet
        for (int i : valueSet) {
            assertTrue(colorSet.contains(colorMapper.getColor(i)));
        }
    }
}
