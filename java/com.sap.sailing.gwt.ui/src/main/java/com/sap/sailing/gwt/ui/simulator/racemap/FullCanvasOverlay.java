package com.sap.sailing.gwt.ui.simulator.racemap;

import java.util.logging.Logger;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.events.center.CenterChangeMapEvent;
import com.google.gwt.maps.client.events.center.CenterChangeMapHandler;
import com.google.gwt.maps.client.overlays.overlayhandlers.OverlayViewOnDrawHandler;
import com.google.gwt.user.client.ui.RequiresResize;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.shared.SimulatorWindDTO;
import com.sap.sailing.gwt.ui.shared.racemap.CanvasOverlayV3;
import com.sap.sailing.gwt.ui.simulator.streamlets.Vector;

/**
 * This class extends {@link CanvasOverlayV3} to provide the functionality that the canvas always covers the
 * full viewable area of the map
 * 
 * @author Nidhi Sawhney(D054070)
 * @author Christopher Ronnewinkel (D036654)
 *
 */
public abstract class FullCanvasOverlay extends CanvasOverlayV3 implements RequiresResize {

    /** the x coordinate where the canvas is placed */
    protected double widgetPosLeft = 0;
    
    /** the y coordinate where the canvas element is placed */
    protected double widgetPosTop = 0;
    
    protected Vector diffPx;

    /** Cached map width. A lookup of this value takes multiple up to 5ms **/
    protected Integer mapWidth;
    /** Cached map height. A lookup of this value takes multiple up to 5ms **/
    protected Integer mapHeight;

    public String pointColor = "Red";
    public String textColor = "Black";
    
    private boolean mapProjectionSet = false;
    
    protected static Logger logger = Logger.getLogger(FullCanvasOverlay.class.getName());
    
    public FullCanvasOverlay(MapWidget map, int zIndex, CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);
        getMap().addCenterChangeHandler(new CenterChangeMapHandler() {
            @Override
            public void onEvent(CenterChangeMapEvent event) {
                drawCenterChanged();
            };
        });
        diffPx = new Vector(0, 0);
    }
    
    @Override
    protected OverlayViewOnDrawHandler getOnDrawHandler() {
        return methods->{
            if (!mapProjectionSet && methods.getProjection() != null) {
                Scheduler.get().scheduleFixedDelay(()->{
                    mapProjectionSet = true;
                    setCanvasSettings();
                    super.getOnDrawHandler().onDraw(methods);
                    return false; // don't repeat
                }, /* delay */ 1000);
            } else {
                super.getOnDrawHandler().onDraw(methods);
            }
        };
    }

    /**
     *  Set the canvas to be the size of the map and set it to the top left corner of the map 
     */
    public void setCanvasSettings() {
        if (mapWidth == null) {
            mapWidth = getMap().getDiv().getClientWidth();
        }
        if (mapHeight == null) {
            mapHeight = getMap().getDiv().getClientHeight();
        }
        canvas.setWidth(String.valueOf(Math.max(1, mapWidth)));
        canvas.setHeight(String.valueOf(Math.max(1, mapHeight)));
        canvas.setCoordinateSpaceWidth(Math.max(1, mapWidth));
        canvas.setCoordinateSpaceHeight(Math.max(1, mapHeight));
        if (getMapProjection() != null) {
            final Point upperLeftCorner = getMapProjection().fromLatLngToDivPixel(getMapProjection().fromContainerPixelToLatLng(Point.newInstance(0, 0)));
            setWidgetPosLeft(Math.round(upperLeftCorner.getX()));
            setWidgetPosTop(Math.round(upperLeftCorner.getY()));
            setCanvasPosition(getWidgetPosLeft(), getWidgetPosTop());
        }
    }

    @Override
    public void onResize() {
        mapWidth = null;
        mapHeight = null;
    	// improve browser performance by deferred scheduling of redraws
        Scheduler.get().scheduleDeferred(this::draw);
    }

    @Override
    public void addToMap() {
        if (map != null) {
            map.addResizeHandler(e->onResize());
        }
        super.addToMap();
    }

    protected void drawCenterChanged() {
    	draw();
    }
    
    @Override
    protected void draw() {
        if (!mapProjectionSet && getMapProjection() != null) {
            // Reset the canvas, e.g. onMove() of map or onResize() of window
            setCanvasSettings();
        }
    }

    /**
     * Draw a point on the canvas
     * @param x coordinate wrt the canvas
     * @param y coordinate wrt the canvas
     */
    protected void drawPoint(double x, double y) {
        Context2d context2d = canvas.getContext2d();
        context2d.setStrokeStyle(pointColor);
        context2d.setLineWidth(3);
        context2d.beginPath();
        context2d.moveTo(x-1, y-1);
        context2d.lineTo(x+1, y+1);
        context2d.closePath();
        context2d.stroke();  
    }
    
    /**
     * Draw a point and a text on the canvas
     * @param x coordinate wrt the canvas
     * @param y coordinate wrt the canvas
     * @param text to be displayed at the point on the canvas
     */
    protected void drawPointWithText(double x, double y, String text) {
        Context2d context2d = canvas.getContext2d();
        drawPoint(x, y);
        if (getMap().getZoom() >= 11) {
            context2d.setFillStyle(textColor);
            context2d.fillText(text, x, y);
        }
    }
    
    /**
     * Draw a circle centred at x,y with given radius
     */
    protected void drawCircle(double x, double y, double radius, String color) {
        Context2d context2d = canvas.getContext2d();
        context2d.setLineWidth(3);
        context2d.setStrokeStyle(color);
        context2d.beginPath();
        context2d.arc(x,y,radius,0,2*Math.PI);
        context2d.closePath();
        context2d.stroke();
    }
        
    /**
     * Draw a circle centred at x,y with given radius
     * @param x
     * @param y
     * @param text
     */
    protected void drawCircleWithText(double x, double y, double radius, String color, String text) {
        Context2d context2d = canvas.getContext2d();
        context2d.setGlobalAlpha(0.9f);
        drawCircle(x, y,radius,color);
        context2d.setGlobalAlpha(1.0f);
        if (getMap().getZoom() >= 11) {
            context2d.setFillStyle(textColor);
            double fontsize = 9.0 + (12.0 - 9.0) * (getMap().getZoom() - 10.0) / (12.0 - 10.0);
            context2d.setFont("normal " + fontsize + "px Calibri");
            context2d.fillText(text, x + 0.7 * fontsize, y + 0.3 * fontsize);
            // System.out.println("ZoomLevel: "+getMap().getZoomLevel()+", Fontsize: "+fontsize);
        }
    }
    
    /**
     * Draw a line from (x,y) to (x1,y1) on the canvas
     * @param x
     * @param y
     * @param x1
     * @param y1
     */
    protected void drawLine(double x, double y, double x1, double y1, double weight, String color) {
        Context2d context2d  = canvas.getContext2d();
        context2d.setLineWidth(weight);
        context2d.setStrokeStyle(color);
        context2d.beginPath();
        context2d.moveTo(x, y);
        context2d.lineTo(x1, y1);
        context2d.closePath();
        context2d.stroke();
    }
    
    /**
     * Debug function to draw the boundaries of the canvas
     */
    protected void drawCanvas() {
        Context2d context2d  = canvas.getContext2d();
        context2d.setStrokeStyle("Black");
        context2d.setLineWidth(3);
       
        context2d.beginPath();
        context2d.moveTo(0,0);
        context2d.lineTo(0, canvas.getCoordinateSpaceHeight());
        context2d.closePath();
        context2d.stroke();
        
        context2d.beginPath();
        context2d.moveTo(0,0);
        context2d.lineTo(canvas.getCoordinateSpaceWidth(),0);
        context2d.closePath();
        context2d.stroke();
        
        context2d.beginPath();   
        context2d.moveTo(canvas.getCoordinateSpaceWidth(),0);
        context2d.lineTo(canvas.getCoordinateSpaceWidth(), canvas.getCoordinateSpaceHeight());
        context2d.closePath();
        context2d.stroke();
        
        context2d.beginPath();   
        context2d.moveTo(0, canvas.getCoordinateSpaceHeight());
        context2d.lineTo(canvas.getCoordinateSpaceWidth(), canvas.getCoordinateSpaceHeight());
        context2d.closePath();
        context2d.stroke();
    }

    protected void drawArrow(final SimulatorWindDTO windDTO, final double angle, final double length, final double weight, String color, final int index, final boolean drawHead) {
        String msg = "Wind @ P" + index + ": time : " + windDTO.timepoint + " speed: " + windDTO.trueWindSpeedInKnots
                + "knots " + windDTO.trueWindBearingDeg;
        logger.fine(msg);
        Position position = windDTO.position;
        LatLng positionLatLng = coordinateSystem.toLatLng(position);
        Point canvasPositionInPx = getMapProjection().fromLatLngToDivPixel(positionLatLng);
        double x = canvasPositionInPx.getX() - getWidgetPosLeft();
        double y = canvasPositionInPx.getY() - getWidgetPosTop();
        //windFieldPoints.put(new ToolTip(x, y), windDTO);
        drawArrowPx(x, y, angle, length, weight, drawHead, color);
    }

    protected void drawArrowPx(double x, double y, double angle, double length, double weight, boolean drawHead, String color) {
        final double dx = length * Math.sin(angle);
        final double dy = -length * Math.cos(angle);
        final double x1 = x + dx / 2;
        final double y1 = y + dy / 2;
        drawLine(x - dx / 2, y - dy / 2, x1, y1, weight, color);
        final double theta = Math.atan2(-dy, dx);
        final double hLength = Math.max(6.,6.+(10./(60.-10.))*Math.max(length-6.,0));
        logger.finer("headlength: "+hLength+", arrowlength: "+length);
        if (drawHead) {
            drawHead(x1, y1, theta, hLength, weight, color);
        }
    }

    protected void drawHead(final double x, final double y, final double theta, final double headLength, final double weight, String color) {
        double t = theta + (Math.PI / 4);
        if (t > Math.PI) {
            t -= 2 * Math.PI;
        }
        double t2 = theta - (Math.PI / 4);
        if (t2 <= (-Math.PI)) {
            t2 += 2 * Math.PI;
        }
        final double x1 = (x - Math.cos(t) * headLength);
        final double y1 = (y + Math.sin(t) * headLength);
        final double x1o = (x + Math.cos(t) * weight/2);
        final double y1o = (y - Math.sin(t) * weight/2);
        final double x2 = (x - Math.cos(t2) * headLength);
        final double y2 = (y + Math.sin(t2) * headLength);
        final double x2o = (x + Math.cos(t2) * weight/2);
        final double y2o = (y - Math.sin(t2) * weight/2);
        drawLine(x1o, y1o, x1, y1, weight, color);
        drawLine(x2o, y2o, x2, y2, weight, color);
    }

    public double getWidgetPosLeft() {
        return widgetPosLeft;
    }

    public void setWidgetPosLeft(double widgetPosLeft) {
        this.widgetPosLeft = widgetPosLeft;
    }

    public double getWidgetPosTop() {
        return widgetPosTop;
    }

    public void setWidgetPosTop(double widgetPosTop) {
        this.widgetPosTop = widgetPosTop;
    }
    
    public Vector getDiffPx() {
        return diffPx;
    }
}
