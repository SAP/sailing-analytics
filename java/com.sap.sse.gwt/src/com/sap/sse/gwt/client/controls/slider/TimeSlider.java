package com.sap.sse.gwt.client.controls.slider;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.DOM;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.controls.slider.TimeTicksCalculator.NormalizedInterval;
import com.sap.sse.gwt.client.controls.slider.TimeTicksCalculator.TickPosition;

/**
 * A slider bar whose values are assumed to be Unix time stamps (milliseconds since the epoch, Jan 1st 1970 00:00:00 UTC).
 * The ticks are correspondingly rendered as HH:mm time points.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class TimeSlider extends SliderBar {

    private final TimeTicksCalculator calc = new TimeTicksCalculator();

    private List<TickPosition> calculatedTimeTicks;
    
    private boolean isZoomed;
    
    private int visibleLabelsInterval = 1;
    
    private final int TICKCOUNT = 10;

    public TimeSlider() {
        calculatedTimeTicks = new ArrayList<TickPosition>();
        isZoomed = false;
    }
    
    private void calculateTicks() {
        if (!isMinMaxInitialized())
            return;

        calculatedTimeTicks.clear();
        
        long minMaxDiffInMs = maxValue.longValue() - minValue.longValue();
        long tickInterval = minMaxDiffInMs / TICKCOUNT; 
        NormalizedInterval normalizedTimeTickInterval = calc.normalizeTimeTickInterval(tickInterval);
        calculatedTimeTicks = calc.calculateTimeTicks(normalizedTimeTickInterval, minValue.longValue(), maxValue.longValue(), 1);
    }
    
    /**
     * Draw the ticks along the line.
     */
    protected void drawTicks() {
        if (!isAttached() || !isMinMaxInitialized())
            return;

        // Draw the ticks
        int lineWidth = lineElement.getOffsetWidth();
        // Create the ticks or make them visible
        for (int i = 0; i < calculatedTimeTicks.size(); i++) {
            TickPosition tickPosition = calculatedTimeTicks.get(i);
            
            Element tick = null;
            if (i < tickElements.size()) {
                tick = tickElements.get(i);
            } else { // Create the new tick
                tick = DOM.createDiv();
                tick.getStyle().setPosition(Position.ABSOLUTE);
                tick.getStyle().setDisplay(Display.NONE);
                DOM.appendChild(getElement(), tick);
                tickElements.add(tick);
            }
            if (enabled) {
                tick.setPropertyString("className", "gwt-SliderBar-tick");
            } else {
                tick.setPropertyString("className", "gwt-SliderBar-tick gwt-SliderBar-tick-disabled");
            }
            // Position the tick and make it visible
            tick.getStyle().setVisibility(Visibility.HIDDEN);
            tick.getStyle().setProperty("display", "");
            int tickWidth = tick.getOffsetWidth();
            if (!maxValue.equals(minValue)) {
                long pos = (tickPosition.getPosition().getTime() - minValue.longValue()) * lineWidth
                        / (maxValue.longValue() - minValue.longValue());
                int tickLeftOffset = lineLeftOffset + (int) pos - (tickWidth / 2);
                tickLeftOffset = Math.min(tickLeftOffset, lineLeftOffset + lineWidth - tickWidth);
                tick.getStyle().setLeft(tickLeftOffset, Unit.PX);
                tick.getStyle().setVisibility(isTickInVisibleRange(tickPosition) ? Visibility.VISIBLE : Visibility.HIDDEN);
            }
        }

        // Hide unused ticks
        for (int i = calculatedTimeTicks.size(); i < tickElements.size(); i++) {
            tickElements.get(i).getStyle().setDisplay(Display.NONE);
        }
    }


    /**
     * Draw the labels along the line.
     */
    protected void drawTickLabels() {
        if (!isAttached() || !isMinMaxInitialized())
            return;

        // Draw the tick labels
        int lineWidth = lineElement.getOffsetWidth();
            // Create the labels or make them visible
            Double previousValue = null;
            for (int i = 0, ticksInVisibleRange = 0; i < calculatedTimeTicks.size(); i++) {
                TickPosition tickPosition = calculatedTimeTicks.get(i);
                Element label = null;
                if (i < tickLabelElements.size()) {
                    label = tickLabelElements.get(i);
                } else { // Create the new label
                    label = DOM.createDiv();
                    label.getStyle().setPosition(Position.ABSOLUTE);
                    label.getStyle().setDisplay(Display.NONE);
                    if (enabled) {
                        label.setPropertyString("className", "gwt-SliderBar-ticklabel");
                    } else {
                        label.setPropertyString("className", "gwt-SliderBar-ticklabel-disabled");
                    }
                    DOM.appendChild(getElement(), label);
                    tickLabelElements.add(label);
                }

                // Set the label text
                double value = tickPosition.getPosition().getTime();
                label.getStyle().setVisibility(Visibility.HIDDEN);
                label.getStyle().setProperty("display", "");
                label.setPropertyString("innerHTML", formatTickLabel(value, previousValue));

                // Move to the left so the label width is not clipped by the shell
                label.getStyle().setLeft(0, Unit.PX);

                // Position the label and make it visible
                int labelWidth = label.getOffsetWidth();
                if (!maxValue.equals(minValue)) {
                    long pos = (tickPosition.getPosition().getTime() - minValue.longValue()) * lineWidth
                            / (maxValue.longValue() - minValue.longValue());
                    int labelLeftOffset = lineLeftOffset + (int) pos - (labelWidth / 2);
                    labelLeftOffset = Math.min(labelLeftOffset, lineLeftOffset + lineWidth - labelWidth);
                    labelLeftOffset = Math.max(labelLeftOffset, lineLeftOffset);
                    label.getStyle().setLeft(labelLeftOffset, Unit.PX);
                    boolean visible = isTickInVisibleRange(tickPosition) && ticksInVisibleRange++ % visibleLabelsInterval == 0;
                    label.getStyle().setVisibility(visible ? Visibility.VISIBLE : Visibility.HIDDEN);
                }
                previousValue = value;
            }

            // Hide unused labels
            for (int i = calculatedTimeTicks.size(); i < tickLabelElements.size(); i++) {
                tickLabelElements.get(i).getStyle().setDisplay(Display.NONE);
            }
    }
    
    private boolean isTickInVisibleRange(TickPosition tickPosition) {
        long tickValue = tickPosition.getPosition().getTime(); 
        return tickValue >= minValue && tickValue <= maxValue;
    }

    public void clearMarkersAndLabelsAndTicks() {
        clearMarkers();
        for (Element elem : tickLabelElements) {
            elem.getStyle().setDisplay(Display.NONE);
            elem.getStyle().setVisibility(Visibility.HIDDEN);
        }
        for (Element elem : tickElements) {
            elem.getStyle().setDisplay(Display.NONE);
            elem.getStyle().setVisibility(Visibility.HIDDEN);
        }

        for (Element elem : markerLabelElements) {
            elem.getStyle().setDisplay(Display.NONE);
            elem.getStyle().setVisibility(Visibility.HIDDEN);
        }
        for (Element elem : markerElements) {
            elem.getStyle().setDisplay(Display.NONE);
            elem.getStyle().setVisibility(Visibility.HIDDEN);
        }
    }
    
    /**
     * Draw the knob where it is supposed to be relative to the line.
     */
    protected void drawKnob() {
        if (!isAttached() || !isMinMaxInitialized())
            return;
        
        Element knobElement = knobImage.getElement();
        if (curValue != null && minValue != null && maxValue != null && curValue >= minValue && curValue <= maxValue) {
            // Move the knob to the correct position
            int lineWidth = lineElement.getOffsetWidth();
            int knobWidth = knobElement.getOffsetWidth();
            int knobLeftOffset = (int) (lineLeftOffset + (getKnobPercent() * lineWidth) - (knobWidth / 2));
            knobLeftOffset = Math.min(knobLeftOffset, lineLeftOffset + lineWidth - (knobWidth / 2) - 1);
            
            knobElement.getStyle().setLeft(knobLeftOffset, Unit.PX);
            knobElement.getStyle().setVisibility(Visibility.VISIBLE);
            knobElement.getStyle().setProperty("display", "");
        } else {
            knobElement.getStyle().setDisplay(Display.NONE);
            knobElement.getStyle().setVisibility(Visibility.HIDDEN);
        }
    }
    
    public void setMaxValue(Double maxValue, boolean fireEvent) {
        if (!isZoomed) {
            super.setMaxValue(maxValue, fireEvent);
        } else {
            this.maxValue = maxValue;
        }
    }

    public void setMinValue(Double minValue, boolean fireEvent) {   
        if (!isZoomed) {
            super.setMinValue(minValue, fireEvent);
        } else {
            this.minValue = minValue;
        }  
    }
    
    @Override
    public boolean setMinAndMaxValue(Double minValue, Double maxValue, boolean fireEvent) {
        final boolean result;
        if (!isZoomed) {
            result = super.setMinAndMaxValue(minValue, maxValue, fireEvent);
        } else {
            boolean minChanged = !Util.equalsWithNull(this.minValue, minValue);
            this.minValue = minValue;
            boolean maxChanged = !Util.equalsWithNull(this.maxValue, maxValue);
            this.maxValue = maxValue;
            result = minChanged || maxChanged;
        }
        return result;
    }
    
    @Override
    protected void onMinMaxValueChanged(boolean fireEvent) {
        calculateTicks();
        super.onMinMaxValueChanged(fireEvent);
    }

    public void setStepSize(double stepSize, boolean fireEvent) {
        if(!isZoomed) {
            super.setStepSize(stepSize, fireEvent);
        } else {
            this.stepSize = stepSize;
        }
    }

    public boolean isZoomed() {
        return isZoomed;
    }

    public void setZoomed(boolean isZoomed) {
        this.isZoomed = isZoomed;
    }

    @Override
    public void onResize() {
        visibleLabelsInterval = (TICKCOUNT / Math.max(getOffsetWidth() / 35, 1)) + 1;
        super.onResize();
    }
}