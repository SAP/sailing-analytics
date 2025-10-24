package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.HashMap;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.base.Size;
import com.sap.sailing.domain.common.FixType;
import com.sap.sailing.gwt.ui.shared.GPSFixDTO;
import com.sap.sailing.gwt.ui.shared.racemap.CanvasOverlayV3;
import com.sap.sailing.gwt.ui.shared.racemap.FixVectorGraphics;
import com.sap.sse.common.Util;

public class FixOverlay extends CanvasOverlayV3 {
    private GPSFixDTO fix;
    private final FixVectorGraphics fixVectorGraphics;
    private final HashMap<Double, Util.Pair<Double,Size>> fixScaleAndSizePerZoomCache;
    
    private Double lastWidth;
    private Double lastHeight;
    private Double lastScaleFactor;
    
    public FixOverlay(MapWidget map, int zIndex, GPSFixDTO fixDTO, FixType type, String color, CoordinateSystem coordinateSystem, String tooltip) {
        super(map, zIndex, coordinateSystem);
        fix = fixDTO;
        fixVectorGraphics = new FixVectorGraphics(type, color);
        fixScaleAndSizePerZoomCache = new HashMap<>();
        setCanvasSize(50, 50);
        getCanvas().setTitle(tooltip);
    }

    @Override
    protected void draw() {
        if (getMapProjection() != null && fix != null) {
            double zoom = map.getZoom();
            Util.Pair<Double, Size> fixScaleAndSize = fixScaleAndSizePerZoomCache.get(zoom);
            if (fixScaleAndSize == null) {
                fixScaleAndSize = getFixScaleAndSize();
                fixScaleAndSizePerZoomCache.put(zoom, fixScaleAndSize);
            }
            double fixSizeScaleFactor = fixScaleAndSize.getA();
            // calculate canvas size
            double canvasWidth = fixScaleAndSize.getB().getWidth();
            double canvasHeight = fixScaleAndSize.getB().getHeight();
            if (needToDraw(canvasWidth, canvasHeight, fixSizeScaleFactor)) {
                setCanvasSize((int) canvasWidth, (int) canvasHeight);
                Context2d context2d = getCanvas().getContext2d();
                // draw the fix
                fixVectorGraphics.drawMarkToCanvas(context2d, canvasWidth, canvasHeight, fixSizeScaleFactor);
                lastScaleFactor = fixSizeScaleFactor;
                lastWidth = canvasWidth;
                lastHeight = canvasHeight;
            }
            setLatLngPosition(coordinateSystem.toLatLng(fix.position));
            Point fixPositionInPx = getMapProjection().fromLatLngToDivPixel(coordinateSystem.toLatLng(fix.position));
            setCanvasPosition(fixPositionInPx.getX() - canvasWidth / 2.0, fixPositionInPx.getY() - canvasHeight / 2.0);
        }
    }
    
    /**
     * Compares the drawing parameters to the <code>last...</code>. If anything has
     * changed, the result is <code>true</code>.
     */
    private boolean needToDraw(double width, double height, double scaleFactor) {
        return lastScaleFactor == null || lastScaleFactor != scaleFactor ||
               lastWidth == null || lastWidth != width ||
               lastHeight == null || lastHeight != height;
    }
    
    public Util.Pair<Double, Size> getFixScaleAndSize() {
        double minFixHeight = 20;
        // the original fix vector graphics is too small (2.1m x 1.5m) for higher zoom levels
        // therefore we scale the fixes with factor 2 by default
        double buoyScaleFactor = 2.0;
        Size fixSizeInPixel = calculateBoundingBox(getMapProjection(), fix.position,
                fixVectorGraphics.getFixWidth().scale(buoyScaleFactor), fixVectorGraphics.getFixHeight().scale(buoyScaleFactor));
        double fixHeightInPixel = fixSizeInPixel.getHeight();
        if(fixHeightInPixel < minFixHeight)
            fixHeightInPixel = minFixHeight;
        // The coordinates of the canvas drawing methods are based on the 'centimeter' unit (1px = 1cm).
        // To calculate the display real fix size the scale factor from canvas units to the real   
        double fixSizeScaleFactor = fixHeightInPixel / (fixVectorGraphics.getFixHeight().scale(100).getMeters());
        return new Util.Pair<Double, Size>(fixSizeScaleFactor, Size.newInstance(fixHeightInPixel * 2.0, fixHeightInPixel * 2.0));
    }
    
    public GPSFixDTO getGPSFixDTO() {
        return fix;
    }
    
    public void setGPSFixDTO(GPSFixDTO fix) {
        this.fix = fix;
        draw();
    }
    
    public String getColor() {
        return fixVectorGraphics.getColor();
    }

    public FixType getType() {
        return fixVectorGraphics.getType();
    }

    public CoordinateSystem getCoordinateSystem() {
        return coordinateSystem;
    }
    
    
}
