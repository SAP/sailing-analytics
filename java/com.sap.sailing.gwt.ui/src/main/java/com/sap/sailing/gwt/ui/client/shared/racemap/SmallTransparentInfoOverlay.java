package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.canvas.dom.client.TextMetrics;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.gwt.ui.shared.racemap.CanvasOverlayV3;

/**
 * A google map overlay based on a HTML5 canvas for drawing a short textual information close to some object on a map
 * with a given position.
 * 
 * @author Frank Mittag
 * @author Axel Uhl
 */
public class SmallTransparentInfoOverlay extends CanvasOverlayV3 {

    /**
     * The text to display in the canvas
     */
    private String infoText;

    /**
     * The current GPS fix of the boat position of the competitor.
     */
    private Position position;

    private int canvasWidth;
    private int canvasHeight;
    private int infoBoxHeight;
    private int infoBoxWidth;
    private double cornerRadius;
    
    private final double fontScalingFactor;
    
    /* Background colors */
    private static final CssColor GREYTRANSPARENT_COLOR = CssColor.make("rgba(255,255,255,0.75)");
    private final CssColor greyWithCustomTransparency;

    public SmallTransparentInfoOverlay(MapWidget map, int zIndex, String infoText, CoordinateSystem coordinateSystem) {
        this(map, zIndex, infoText, coordinateSystem, /* fontScalingFactor */ 1.0, GREYTRANSPARENT_COLOR);
    }
    
    public SmallTransparentInfoOverlay(MapWidget map, int zIndex, String infoText, CoordinateSystem coordinateSystem, double fontScalingFactor) {
        this(map, zIndex, infoText, coordinateSystem, fontScalingFactor, GREYTRANSPARENT_COLOR);
    }
    
    public SmallTransparentInfoOverlay(MapWidget map, int zIndex, String infoText, CoordinateSystem coordinateSystem, CssColor cssColor) {
        this(map, zIndex, infoText, coordinateSystem, /* fontScalingFactor */ 1.0, cssColor);
    }
    
    public SmallTransparentInfoOverlay(MapWidget map, int zIndex, String infoText, CoordinateSystem coordinateSystem,
            double fontScalingFactor, CssColor cssColor) {
        super(map, zIndex, coordinateSystem);
        this.infoText = infoText;
        this.fontScalingFactor = fontScalingFactor;
        canvasWidth = 20;
        canvasHeight = 45;
        infoBoxWidth = 20;
        infoBoxHeight = 20;
        cornerRadius = 4;
        if (getCanvas() != null) {
            getCanvas().setWidth(String.valueOf(canvasWidth));
            getCanvas().setHeight(String.valueOf(canvasHeight));
            getCanvas().setCoordinateSpaceWidth(canvasWidth);
            getCanvas().setCoordinateSpaceHeight(canvasHeight);
        }
        greyWithCustomTransparency = cssColor;
    }

    @Override
    protected void draw() {
        final int LINE_SPACING = 3;
        final int BOTTOM_MARGIN = 8;
        final int LEFT_MARGIN = 8;
        final int RIGHT_MARGIN = 9;
        final int POLE_LENGTH = 25;
        if (getMapProjection() != null && position != null) {
            LatLng latLngPosition = coordinateSystem.toLatLng(position);
            Context2d context2d = getCanvas().getContext2d();
            final int fontSizeInPx = (int) (fontScalingFactor*(12+2*Math.max(0, map.getZoom()-15)));
            final int LINE_HEIGHT = fontSizeInPx+LINE_SPACING;
            context2d.setFont(""+fontSizeInPx+"px Roboto, Arial, sans-serif");
            double textWidth = 0;
            infoBoxHeight = BOTTOM_MARGIN;
            for (final String line : infoText.split("\n")) {
                TextMetrics measureText = context2d.measureText(line);
                if (measureText.getWidth() > textWidth) {
                    textWidth = measureText.getWidth();
                }
                infoBoxHeight += LINE_HEIGHT;
            }
            canvasWidth = (int) textWidth + LEFT_MARGIN + RIGHT_MARGIN;
            infoBoxWidth = canvasWidth;
            canvasHeight = infoBoxHeight + POLE_LENGTH;
            getCanvas().setWidth(String.valueOf(canvasWidth));
            getCanvas().setCoordinateSpaceWidth(canvasWidth);
            getCanvas().setHeight(String.valueOf(canvasHeight));
            getCanvas().setCoordinateSpaceHeight(canvasHeight);
            // Change origin and dimensions to match true size (a stroke makes the shape a bit larger)
            context2d.setFillStyle(greyWithCustomTransparency);
            drawRoundedRect(context2d, cornerRadius / 2, cornerRadius / 2, infoBoxWidth - cornerRadius, infoBoxHeight
                    - cornerRadius, cornerRadius);
            // this translation is important for drawing lines with a real line width of 1 pixel
            context2d.translate(-0.5, -0.5);
            context2d.setStrokeStyle(greyWithCustomTransparency);
            context2d.setLineWidth(1.0);
            context2d.beginPath();
            context2d.moveTo(cornerRadius / 2, infoBoxHeight / 2);
            context2d.lineTo(cornerRadius / 2, canvasHeight);
            context2d.stroke();
            context2d.translate(0.0, 0.0);
            context2d.beginPath();
            context2d.setFillStyle("black");
            context2d.setFont(""+fontSizeInPx+"px Roboto, Arial, sans-serif");
            int y = LINE_HEIGHT;
            for (final String line : infoText.split("\n")) {
                context2d.fillText(line, LEFT_MARGIN, y);
                y += LINE_HEIGHT;
            }
            context2d.stroke();
            Point objectPositionInPx = getMapProjection().fromLatLngToDivPixel(latLngPosition);
            setCanvasPosition(objectPositionInPx.getX(), objectPositionInPx.getY() - canvasHeight);
        }
    }

    public static void drawRoundedRect(Context2d context, double x, double y, double w, double h, double r) {
        context.beginPath();
        context.moveTo(x + r, y);
        context.lineTo(x + w - r, y);
        context.quadraticCurveTo(x + w, y, x + w, y + r);
        context.lineTo(x + w, y + h - r);
        context.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
        context.lineTo(x + r, y + h);
        context.quadraticCurveTo(x, y + h, x, y + h - r);
        context.lineTo(x, y + r);
        context.quadraticCurveTo(x, y, x + r, y);
        context.stroke();
        context.fill();
    }

    /**
     * @param position the new position of the overlay
     * @param timeForPositionTransitionMillis use -1 to not animate the position transition, e.g., during map zoom or non-play
     */

    public void setPosition(Position position, long timeForPositionTransitionMillis) {
        updateTransition(timeForPositionTransitionMillis);
        this.position = position;
    }
    
    /**
     * Updates the text to show and re-draws the canvas
     */
    public void setInfoText(String infoText) {
        this.infoText = infoText;
    }
}