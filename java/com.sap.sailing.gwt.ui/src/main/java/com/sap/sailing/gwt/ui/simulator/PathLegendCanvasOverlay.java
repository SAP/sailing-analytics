package com.sap.sailing.gwt.ui.simulator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.TextMetrics;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.maps.client.MapWidget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.simulator.racemap.FullCanvasOverlay;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;
import com.sap.sse.common.AbstractBearing;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * Class to draw the legend for the different paths on the map.
 * 
 * @author Nidhi Sawhney (D054070)
 *
 */
public class PathLegendCanvasOverlay extends FullCanvasOverlay {
    
    private final StringMessages stringMessages;

    private List<PathCanvasOverlay> pathOverlays;

    private double curSpeed;
    private double curBearing;
    private char mode;

    /**
     * Offset where the legend starts
     */
    private int xOffset = 10;
    private int yOffset = 20;

    private double rectWidth = 20;
    private double rectHeight = 20;

    private String textColor = "Black";
    private String textFont = "10pt 'Open Sans'";
    private String algorithmTimedOutText = "out-of-bounds";
    private String mixedLegText = "ambiguous wind";

    public PathLegendCanvasOverlay(MapWidget map, int zIndex, char mode, CoordinateSystem coordinateSystem,
            StringMessages stringMessages) {
        super(map, zIndex, coordinateSystem);
        this.stringMessages = stringMessages;
    	this.mode = mode;
    	if (this.mode == SailingSimulatorConstants.ModeEvent) {
    	    yOffset = 170;
    	}
        setPathOverlays(null);
        this.algorithmTimedOutText = stringMessages.simulationLegendAlgorithmTimedOutText();
        this.mixedLegText = stringMessages.simulationLegendMixedLegText();
    }

    @Override
    protected void draw() {
        if (getMapProjection() != null && pathOverlays != null && pathOverlays.size() > 0) {
            boolean containsPolyline = false;
            for (PathCanvasOverlay overlay : this.pathOverlays) {
                if (overlay.getName().equals(PathPolyline.END_USER_NAME)) {
                    containsPolyline = true;
                    break;
                }
            }
            if (containsPolyline) {
                List<PathCanvasOverlay> result = new ArrayList<PathCanvasOverlay>();
                int indexOfPolyline = 0;
                for (int index = 0; index < this.pathOverlays.size(); index++) {
                    if (this.pathOverlays.get(index).getName().equals(PathPolyline.END_USER_NAME)) {
                        indexOfPolyline = index;
                    } else {
                        result.add(this.pathOverlays.get(index));
                    }
                }
                result.add(0, this.pathOverlays.get(indexOfPolyline));
                this.pathOverlays = result;
            }
            setCanvasSettings();
            int index = 0;
            Context2d context2d = canvas.getContext2d();
            context2d.setFont(textFont);
            TextMetrics txtmet;
            txtmet = context2d.measureText("00:00:00");
            double timewidth = txtmet.getWidth();
            double txtmaxwidth = 0.0;
            boolean containsTimeOut = false;
            boolean containsMixedLeg = false;
            for (PathCanvasOverlay path : pathOverlays) {
                if (path.getAlgorithmTimedOut()) {
                    containsTimeOut = true;
                }
                if (path.getMixedLeg()) {
                    containsMixedLeg = true;
                }
                txtmet = context2d.measureText(path.getName());
                txtmaxwidth = Math.max(txtmaxwidth, txtmet.getWidth());
            }
            double newwidth = 0;
            double deltaTime = 0;
            double deltaMixedLeg = 0;
            double deltaTimeOut = 0;
            double mixedLegWidth = 0;
            if (containsMixedLeg) {
                txtmet = context2d.measureText(mixedLegText);
                mixedLegWidth = txtmet.getWidth();
                newwidth = Math.max(timewidth, mixedLegWidth);
            }
            double timeOutWidth = 0;
            if (containsTimeOut) {
                txtmet = context2d.measureText(algorithmTimedOutText);
                timeOutWidth = txtmet.getWidth();
                newwidth = Math.max(newwidth, timeOutWidth);
            }
            if (containsMixedLeg||containsTimeOut) {
                deltaTime = newwidth - timewidth;
                deltaMixedLeg = newwidth - mixedLegWidth;
                deltaTimeOut = newwidth - timeOutWidth;
                timewidth = newwidth;
            }
            for (PathCanvasOverlay path : pathOverlays) {
                String timeText = (path.getMixedLeg() ? mixedLegText : (path.getAlgorithmTimedOut() ? algorithmTimedOutText : getFormattedTime(path.getPathDurationMillis())));
                drawRectangleWithText(xOffset, yOffset + (pathOverlays.size()-1-index) * rectHeight, path.getPathColor(),
                        path.getName(), timeText, txtmaxwidth, timewidth, (path.getMixedLeg()?deltaMixedLeg:(path.getAlgorithmTimedOut()?deltaTimeOut:deltaTime)));
                index++;
            }
            //
            // TODO: draw current arrow
            //
            AbstractBearing curBear = new DegreeBearingImpl(this.curBearing);
            //Context2d context2d = canvas.getContext2d();
            if (this.curSpeed >= 0.0) {
                //drawScaledArrow(windDTO, dbi.getRadians(), index, true);
                double cFactor = 12.0;
                double cWidth = Math.max(1., 1. + (cFactor * PathPolyline.knotsToMetersPerSecond(this.curSpeed) / 3.0));
                double cLength = Math.max(10., 10. + (cFactor * 2. * PathPolyline.knotsToMetersPerSecond(this.curSpeed)));
                double cX = xOffset + (rectWidth + 15.0 + txtmaxwidth + timewidth)/2.0;
                double cY = yOffset + (pathOverlays.size()-1) * rectHeight + 75.0;
                context2d.setGlobalAlpha(0.80);
                context2d.setFillStyle("white");
                double bgWidth = 120.0;
                double bgHeight = 70.0;
                if (this.curSpeed == 0.0) {
                	bgHeight = 20.0;
                }
                context2d.fillRect(cX - bgWidth/2.0, cY - 25.0 - 15.0, bgWidth, bgHeight);
                context2d.setGlobalAlpha(1.0);
                context2d.setFont(textFont);
                context2d.setFillStyle(textColor);
                //TextMetrics txtmet;
                String cText = stringMessages.current() + ": " + stringMessages.knotsValue(curSpeed);
                txtmet = context2d.measureText(cText);
                double txtwidth = txtmet.getWidth();
                context2d.fillText(cText, cX-(txtwidth/2.0), cY-25);
                if (this.curSpeed > 0.0) {
                    drawArrowPx(cX, cY, curBear.getRadians(), cLength, cWidth, true, "Green");
                }
            }
        }
    }

    @Override
    public void setCanvasSettings() {
    	super.setCanvasSettings();	
        canvas.getElement().getStyle().setZIndex(100000); // make legend float on-top of all overlays
    }

    public List<PathCanvasOverlay> getPathOverlays() {
        return pathOverlays;
    }

    public void setPathOverlays(List<PathCanvasOverlay> pathOverlays) {

        this.pathOverlays = pathOverlays;
    }

    public void addPathOverlay(PathCanvasOverlay pathOverlay) {
        if (this.pathOverlays == null) {
            this.pathOverlays = new ArrayList<PathCanvasOverlay>();
        }

        boolean found = false;

        if (pathOverlay.getName().equals(PathPolyline.END_USER_NAME)) {
            for (PathCanvasOverlay overlay : this.pathOverlays) {
                if (overlay.getName().equals(PathPolyline.END_USER_NAME)) {
                    overlay.setTotalTimeMilliseconds(pathOverlay.getTotalTimeMilliseconds());
                    found = true;
                    break;
                }
            }
        }

        if (found == false) {
            this.pathOverlays.add(pathOverlay);
        }
    }

    protected void drawRectangle(double x, double y, String color) {
        Context2d context2d = canvas.getContext2d();
        context2d.setFillStyle(color);
        context2d.setLineWidth(3);
        context2d.fillRect(x, y, rectWidth, rectHeight);
    }

    protected void drawRectangleWithText(double x, double y, String color, String text, String time, double textmaxwidth, double timewidth, double xdelta) {

        double offset = 3.0;

        Context2d context2d = canvas.getContext2d();
        context2d.setFont(textFont);
        drawRectangle(x, y, color);
        context2d.setGlobalAlpha(0.80);
        context2d.setFillStyle("white");
        context2d.fillRect(x + rectWidth, y, 20.0 + textmaxwidth + timewidth, rectHeight);
        context2d.setGlobalAlpha(1.0);
        context2d.setFillStyle(textColor);
        context2d.fillText(text, x + rectWidth + 5.0, y + 12.0 + offset);
        context2d.fillText(time, x + rectWidth + textmaxwidth + xdelta + 15.0, y + 12.0 + offset);
    }

    protected String getFormattedTime(long pathTime) {
        TimeZone gmt = TimeZone.createTimeZone(0);
        Date timeDiffDate = new Date(pathTime);
        String pathTimeStr = DateTimeFormat.getFormat(DateTimeFormat.PredefinedFormat.HOUR24_MINUTE_SECOND).format(
                timeDiffDate, gmt);
        return pathTimeStr;
    }
    
    public void setCurrent(double curSpeed, double curBearing) {
        this.curSpeed = curSpeed;
        this.curBearing = curBearing;
    }
    
}
