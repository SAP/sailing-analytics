package com.sap.sailing.gwt.ui.simulator.streamlets;

import java.util.HashSet;
import java.util.Set;

/**
 * This class maps a given range of values to a full color spectrum. The color spectrum could also be a grey scale.
 * Therefore it needs the max and the min value to be mapped. It uses a {@link ValueRangeBoundaries} to get those.
 * The ColorMapper has listeners that will be notified if the colormapping has been changed.
 * 
 * @author D073259
 *
 */
public class ColorMapper implements ValueRangeBoundariesChangedListener {
    private final ValueRangeBoundaries valueRange;
    private double minValue;
    private double maxValue;
    private boolean isGrey;
    private final Set<ColorMapperChangedListener> colorMapperChangedListeners;

    public ColorMapper(ValueRangeBoundaries valueRange, boolean isGrey) {
        this.valueRange = valueRange;
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

    public String getColor(double value) {
        if (isGrey) {
            return "rgba(255,255,255," + Math.min(1.0, (value - minValue) / (maxValue - minValue)) + ")";
        } else {
            double h = (1 - (value - minValue) / (maxValue - minValue)) * 240;
            return "hsl(" + Math.round(h) + ", 100%, 50%)";
        }
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
