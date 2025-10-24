package com.sap.sailing.gwt.ui.shared.racemap;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.TextMetrics;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.controls.ControlPosition;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.gwt.ui.client.DetailTypeFormatter;
import com.sap.sailing.gwt.ui.client.NumberFormatterFactory;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.simulator.racemap.FullCanvasOverlay;
import com.sap.sse.common.ColorMapper;
import com.sap.sse.common.ValueRangeFlexibleBoundaries;

public class DetailTypeMetricOverlay extends FullCanvasOverlay {
    
    private final String textColor = "Black";
    private final String textFont = "10pt 'Open Sans'";
    /**
     * Directly controls the width of the drawn box.
     */
    private int width = 260;
    /**
     * Directly controls the height of the drawn box.
     */
    private double lineAmount = 5;
    /**
     * Controls the height of each individual line.
     */
    private double lineHeight = 13;
    /**
     * Margin space to leave along the boxes edges.
     */
    private double lineMargin = 8;
    private final StringMessages stringMessages;
    private Canvas metricLegend;
    
    private ValueRangeFlexibleBoundaries valueRange;
    private ColorMapper colorMapper;
    
    private String detailTypeAndUnit = "";

    public DetailTypeMetricOverlay(MapWidget map, int zIndex, CoordinateSystem coordinateSystem, StringMessages stringMessages) {
        super(map, zIndex, coordinateSystem);
        this.stringMessages = stringMessages;
    }
    
    /**
     * Creates the needed HTML divs and canvas if supported.
     * @param map {@link MapWidget} to draw on.
     */
    protected void createMetricLegend(MapWidget map) {
        metricLegend = Canvas.createIfSupported();
        metricLegend.setStyleName("MapMetricLegend");
        metricLegend.setTitle("Metric Legend");
        map.setControls(ControlPosition.TOP_CENTER, metricLegend);
        metricLegend.getParent().setStyleName("MapMetricLegendParentDiv");
    }

    @Override
    public void setVisible(boolean isVisible) {
        super.setVisible(isVisible);
        if (metricLegend != null) {
            metricLegend.setVisible(isVisible);
        }
    }

    @Override
    protected void draw() {
        if (getMapProjection() != null) {
            super.setCanvasSettings();
            drawLegend();
        }
    }

    @Override
    protected void drawCenterChanged() {
        draw();
    }

    /**
     * Clears the canvas.
     */
    public void clearCanvas() {
        if (metricLegend != null) {
            Context2d g = this.getCanvas().getContext2d();
            double w = metricLegend.getOffsetWidth();
            double h = metricLegend.getOffsetHeight();
            g = metricLegend.getContext2d();
            g.clearRect(0, 0, w, h);            
        }
    }

    /**
     * Updates the information displayed.
     * @param valueRange {@link ValueRangeFlexibleBoundaries} to take min and max values from.
     * @param colorMapper {@link ColorMapper} to generate a spectrum.
     * @param detailType {@link DetailType} to show metric and unit.
     */
    public void updateLegend(ValueRangeFlexibleBoundaries valueRange, ColorMapper colorMapper, DetailType detailType) {
        this.valueRange = valueRange;
        this.colorMapper = colorMapper;
        if (detailType != null) {
            this.detailTypeAndUnit = DetailTypeFormatter.format(detailType) + " - " + DetailTypeFormatter.getUnit(detailType);
        } else {
            this.detailTypeAndUnit = "";
        }
        draw();
    }

    /**
     * Draws the legend and creates a canvas if one does not exist already.
     */
    public void drawLegend() {
        if (isVisible()) {
            if (metricLegend == null) {
                createMetricLegend(map);
            }
            Context2d context2d = metricLegend.getContext2d();
            // Calculate sizes
            int canvasHeight = (int) Math.ceil(lineHeight * lineAmount + lineMargin * 2);
            double lineWidth = width - 2 * lineMargin;
            setCanvasSize(metricLegend, width, canvasHeight);
            // Draw background
            context2d.setGlobalAlpha(0.75);
            drawRectangle(context2d, 0, 0, width, canvasHeight, "white");
            // Draw text
            context2d.setGlobalAlpha(1.0);
            drawTextCentered(context2d, lineMargin, lineToYOffset(0), lineWidth, stringMessages.tailColor(), textColor);
            drawText(context2d, lineMargin * 2, lineToYOffset(1), lineWidth - 2 * lineMargin, detailTypeAndUnit, textColor);
            // Draw spectrum
            drawSpectrum(context2d, lineMargin * 2, lineToYOffset(2) - 6, lineWidth - 2 * lineMargin);
        }
    }

    /**
     * Calculates the y offset needed to draw something from a line number.
     * @param line {@code int} line number.
     * @return {@code double} offset on y axis from the top of the canvas.
     */
    private double lineToYOffset(int line) {
        return lineMargin + (lineHeight * ++line);
    }

    /**
     * Sets the canvas size.
     * @param canvas {@link Canvas} to set size of.
     * @param canvasWidth {@code int} width to set.
     * @param canvasHeight {@code int} height to set.
     */
    protected void setCanvasSize(Canvas canvas, int canvasWidth, int canvasHeight) {
        canvas.setWidth(String.valueOf(canvasWidth));
        canvas.setHeight(String.valueOf(canvasHeight));
        canvas.setCoordinateSpaceWidth(canvasWidth);
        canvas.setCoordinateSpaceHeight(canvasHeight);
    }

    /**
     * Draws a rectangle.
     * @param context2d {@link Context2d} to draw on.
     * @param fromX {@code double} upper-left corner x axis position.
     * @param fromY {@code double} upper-left corner y axis position.
     * @param toX {@code double} lower-right corner x axis position.
     * @param toY {@code double} lower-right corner y axis position.
     * @param color {@link String} color to use while drawing.
     */
    protected void drawRectangle(Context2d context2d, double fromX, double fromY, double toX, double toY, String color) {
        context2d.setFillStyle(color);
        context2d.setLineWidth(3);
        context2d.fillRect(fromX, fromY, toX, toY);
    }

    protected void drawText(Context2d context2d, double x, double y, double maxWidth, String text, String color) {
        context2d.setFillStyle(color);
        context2d.setFont(textFont);
        context2d.fillText(text, x, y, maxWidth);
    }
    
    protected void drawTextCentered(Context2d context2d, double x, double y, double width, String text, String color) {
        context2d.setFont(textFont);
        TextMetrics metrics = context2d.measureText(text);
        double offset = Math.max((width - metrics.getWidth()) / 2.0, 0.0);
        drawText(context2d, x + offset, y, width - 2 * offset, text, color);
    }
    
    protected void drawSpectrum(Context2d context2d, double x, double y, double width) {
        if (valueRange == null || colorMapper == null) return;
        final double min = valueRange.getMinLeft();
        final double spread = valueRange.getMaxRight() - min;
        final int maxDigits = (int) Math.ceil(Math.log10(valueRange.getMaxRight()));
        final int decimals = maxDigits - 3 <= 0 ? -(maxDigits - 3) : 0;
        final int scale_spread;
        if (spread < 3) {
            scale_spread = (int) width / 2;
        } else if (spread < 15) {
            scale_spread = (int) width / 4;
        } else {
            scale_spread = Math.max(30, (int) width / 6);
        }
        final double h = 15;
        String label;
        TextMetrics txtmet;
        final NumberFormat numberFormatOneDecimal = NumberFormatterFactory.getDecimalFormat(decimals);
        for (int idx = 0; idx <= width; idx++) {
            final double speedSteps = min + idx * (spread) / width;
            context2d.setFillStyle(colorMapper.getColor(speedSteps));
            context2d.beginPath();
            context2d.fillRect(x + idx, y, 1, h);
            context2d.closePath();
            context2d.stroke();
            if (idx % scale_spread == 0) {
                context2d.setStrokeStyle(textColor);
                context2d.setLineWidth(1.0);
                context2d.beginPath();
                context2d.moveTo(x + idx, y + h);
                context2d.lineTo(x + idx, y + h + 7.0);
                context2d.closePath();
                context2d.stroke();
                context2d.setFillStyle(textColor);
                label = numberFormatOneDecimal.format(speedSteps);
                txtmet = context2d.measureText(label);
                context2d.fillText(label, x + idx - txtmet.getWidth() / 2.0, y + h + 8.0 + 8.0);
            }
        }
    }
}
