package com.sap.sse.common;

import java.util.HashSet;
import java.util.Set;

/**
 * Maps a given range of values to a full color spectrum. The color spectrum could also be a grey scale. Therefore it
 * needs the max and the min value to be mapped. It uses a {@link ValueRangeFlexibleBoundaries} to get those. The
 * ColorMapper has {@link ColorMapperChangedListener listeners} that will be notified if the color mapping has been
 * changed.
 * 
 * @author D073259 (Alessandro Stoltenberg)
 *
 */
public class ColorMapper implements ValueRangeFlexibleBoundariesChangedListener {
    public static final int MAX_HUE = 315;
    private final ValueRangeFlexibleBoundaries valueRange;
    private double minValue;
    private double maxValue;
    private boolean isGrey;
    private final Set<ColorMapperChangedListener> colorMapperChangedListeners;
    private final ValueSpreader valueSpreader;

    /**
     * Spreads a value that is between 0..1 to the same range 0..1, but may introduce some function
     * in between. Most trivially would be a function f(x)=x that linearly spreads all values out across
     * the spectrum.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    @FunctionalInterface
    public static interface ValueSpreader {
        ValueSpreader LINEAR = v->v;
        ValueSpreader CUBIC_ROOT = v->(Math.pow(Math.abs(2*(v-0.5)), 1./3.)*Math.signum((v-0.5))+1.0)/2.0;
        
        double getSpreadOutValue(double value);
    }
    
    public ColorMapper(ValueRangeFlexibleBoundaries valueRange, boolean isGrey, ValueSpreader valueSpreader) {
        this.valueRange = valueRange;
        this.valueSpreader = valueSpreader;
        this.valueRange.addListener(this);
        minValue = this.valueRange.getMinLeft();
        maxValue = this.valueRange.getMaxRight();
        this.isGrey = isGrey;
        colorMapperChangedListeners = new HashSet<>();
    }
    
    public void setGrey(boolean isGrey) {
        this.isGrey = isGrey;
        notifyListeners();
    }

    /**
     * Returns a string with a rgba representation of a the value mapped to the color spectrum in case {@link #isGrey}
     * <c> = true </c> or the hsl representation of the color in case {@link #isGrey} <c> = false </c>. If called with a
     * value outside the {@link #valueRange} the return will be the minimum or the maximum of the spectrum, depending if
     * the input value is smaller or greater then the {@link #minValue} or {@link #maxValue} of the {@link #valueRange}.
     */
    public String getColor(double value) {
        final String result;
        if (isGrey) {
            if (value < minValue) {
                result = "rgba(255,255,255,0.0)";
            } else if (value > maxValue) {
                result = "rgba(255,255,255,1.0)";
            } else {
                result = "rgba(255,255,255," + valueSpreader.getSpreadOutValue((value - minValue) / (maxValue - minValue)) + ")";
            }
        } else {
            if (value < minValue) {
                result = "hsl("+MAX_HUE+", 100%, 50%)";
            } else if (value > maxValue) {
                result = "hsl(0, 100%, 50%)";
            } else {
                double h = valueSpreader.getSpreadOutValue(1 - (value - minValue) / (maxValue - minValue)) * MAX_HUE;
                result = "hsl(" + Math.round(h) + ", 100%, 50%)";
            }
        }
        return result;
    }
    
    private void updateMinMax() {
        minValue = valueRange.getMinLeft();
        maxValue = valueRange.getMaxRight();
    }
    
    @Override
    public void onValueRangeBoundariesChanged() {
        updateMinMax();
        notifyListeners();
    }
    
    public void addListener(ColorMapperChangedListener listener) {
        colorMapperChangedListeners.add(listener);
    }
    
    public void removeListener(ColorMapperChangedListener listener) {
        colorMapperChangedListeners.remove(listener);
    }
    
    public void notifyListeners() {
        for (ColorMapperChangedListener listener : colorMapperChangedListeners) {
            listener.onColorMappingChanged();
        }
    }
}
