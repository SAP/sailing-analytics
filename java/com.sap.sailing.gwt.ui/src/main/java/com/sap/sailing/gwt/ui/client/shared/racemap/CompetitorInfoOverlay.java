package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.canvas.dom.client.TextMetrics;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.gwt.ui.shared.racemap.CanvasOverlayV3;
import com.sap.sse.common.Color;

/**
 * A google map overlay based on a HTML5 canvas for drawing a competitor info on a map at a given position.
 * 
 * @author Frank Mittag
 */
public class CompetitorInfoOverlay extends CanvasOverlayV3 {

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
    private int defaultInfoBoxHeight = 14;
    public static final int X_TEXT_COORDINATE = 8;
    public static final int Y_TEXT_COORDINATE = 14;

    private Color competitorColor; 

    public CompetitorInfoOverlay(MapWidget map, int zIndex, Color competitorColor, String infoText, CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);
        this.competitorColor = competitorColor;
        this.infoText = infoText;
        canvasWidth = 100;
        canvasHeight = 100;
        setCanvasSize(canvasWidth, canvasHeight);
        canvas.addStyleName("competitorInfo-Canvas");
    }

    @Override
    protected void draw() {
        if (getMapProjection() != null && position != null) {
            LatLng latLngPosition = coordinateSystem.toLatLng(position);
            Context2d ctx = getCanvas().getContext2d();
            CssColor grayTransparentColor = CssColor.make("rgba(255,255,255,0.75)");
            ctx.setFont("12px bold Verdana sans-serif");
            String[] textLines = infoText.split("\n");
            TextMetrics measureText = ctx.measureText(findLargestLine(textLines));
            double largestLineWidth = measureText.getWidth();
            infoBoxHeight = defaultInfoBoxHeight + textLines.length * 10;
            canvasWidth = (int)largestLineWidth + infoBoxHeight;
            setCanvasSize(canvasWidth, canvasHeight);
            ctx.save();
            ctx.clearRect(0,  0,  canvasWidth, canvasHeight);
            ctx.setLineWidth(1.0);
            ctx.setFillStyle(grayTransparentColor);
            if (competitorColor != null) {
                ctx.setStrokeStyle(competitorColor.getAsHtml());
            } else {
                ctx.setStrokeStyle("#888888");
            }
            ctx.beginPath();
            ctx.moveTo(0,0);
            ctx.lineTo(0,101);
            ctx.stroke();
            ctx.beginPath();
            ctx.moveTo(1.0,1.0);
            ctx.lineTo(canvasWidth,1.0);
            double lenghtOfTheLastLine = ctx.measureText(textLines[textLines.length - 1]).getWidth();
            int bottomLineWidth = 2 * X_TEXT_COORDINATE + (int) lenghtOfTheLastLine;
            ctx.lineTo(bottomLineWidth, infoBoxHeight);
            ctx.lineTo(1.0,infoBoxHeight);
            ctx.closePath();
            ctx.fill();
            ctx.stroke();
            ctx.beginPath();
            ctx.setFillStyle("black");
            drawText(textLines, ctx);
            ctx.stroke();
            ctx.restore();
            Point objectPositionInPx = getMapProjection().fromLatLngToDivPixel(latLngPosition);
            setCanvasPosition(objectPositionInPx.getX(), objectPositionInPx.getY() - canvasHeight);
        }
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
     * Updates the text to show
     */
    public void setInfoText(String infoText) {
        this.infoText = infoText;
    }
    
    private void drawText(String[] lines, Context2d ctx) {
        for (int i = 0; i < lines.length; i++) {
            ctx.fillText(lines[i], X_TEXT_COORDINATE, Y_TEXT_COORDINATE + i * 12);
        }
    }

    private String findLargestLine(String[] lines) {
        int index = 0;
        int maxLength = lines[0].length();
        for (int i = 0; i < lines.length; i++) {
            if (maxLength < lines[i].length()) {
                index = i;
                maxLength = lines[i].length();
            }
        }
        return lines[index];
    }
}