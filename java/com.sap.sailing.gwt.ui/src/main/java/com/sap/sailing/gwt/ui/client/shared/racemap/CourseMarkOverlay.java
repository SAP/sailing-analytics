package com.sap.sailing.gwt.ui.client.shared.racemap;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.base.Size;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.gwt.ui.shared.CoursePositionsDTO;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.racemap.CanvasOverlayV3;
import com.sap.sailing.gwt.ui.shared.racemap.MarkVectorGraphics;
import com.sap.sailing.gwt.ui.shared.racemap.MarkVectorGraphicsFactory;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Util;

/**
 * A google map overlay based on a HTML5 canvas for drawing course marks (canvases) and the buoy zone if the mark is a buoy.
 */
public class CourseMarkOverlay extends CanvasOverlayV3 {
    /**
     * The course mark to draw
     */
    private final MarkDTO mark;
    
    private CoursePositionsDTO coursePositionsDTO;

    private Position position;

    private Distance buoyZoneRadius;
    
    private boolean showBuoyZone;

    private final int MIN_BUOYZONE_RADIUS_IN_PX = 25;

    private final MarkVectorGraphics markVectorGraphics;

    private Map<Double, Util.Pair<Double, Size>> markScaleAndSizePerZoomCache; 
    
    private Double lastWidth;
    private Double lastHeight;
    private Double lastScaleFactor;
    private Boolean lastShowBuoyZone;
    private Boolean lastIsSelected;
    private Distance lastBuoyZoneRadius;
    
    private MarkVectorGraphicsFactory markVectorGraphicsFactory;

    public CourseMarkOverlay(MapWidget map, int zIndex, MarkDTO markDTO, CoordinateSystem coordinateSystem, CoursePositionsDTO coursePositionsDTO) {
        super(map, zIndex, coordinateSystem);
        this.mark = markDTO;
        this.coursePositionsDTO = coursePositionsDTO;
        this.position = markDTO.position;
        this.buoyZoneRadius = new MeterDistance(0.0);
        this.showBuoyZone = false;
        this.markScaleAndSizePerZoomCache = new HashMap<>();
        this.markVectorGraphicsFactory = new MarkVectorGraphicsFactory();
        this.markVectorGraphics = markVectorGraphicsFactory.getMarkVectorGraphics(markDTO);
        setCanvasSize(50, 50);
    }
    
    @Override
    protected void draw() {
        if (getMapProjection() != null && mark != null && position != null) {
            double zoom = map.getZoom();
            Util.Pair<Double, Size> markScaleAndSize = markScaleAndSizePerZoomCache.get(zoom);
            if (markScaleAndSize == null) {
                markScaleAndSize = getMarkScaleAndSize(position);
                markScaleAndSizePerZoomCache.put(zoom, markScaleAndSize);
            }
            double markSizeScaleFactor = markScaleAndSize.getA();
            getCanvas().setTitle(getTitle());
            // calculate canvas size
            double canvasWidth = markScaleAndSize.getB().getWidth();
            double canvasHeight = markScaleAndSize.getB().getHeight();
            double buoyZoneRadiusInPixel = -1;
            if (showBuoyZone && isMarkWithBuoyZone(mark)) {
                buoyZoneRadiusInPixel = calculateRadiusOfBoundingBoxInPixels(getMapProjection(), position,
                        buoyZoneRadius);
                if (buoyZoneRadiusInPixel > MIN_BUOYZONE_RADIUS_IN_PX) {
                    canvasWidth = (buoyZoneRadiusInPixel + 1) * 2;
                    canvasHeight = (buoyZoneRadiusInPixel + 1) * 2;
                }
            }
            if (needToDraw(showBuoyZone, isSelected, buoyZoneRadius, canvasWidth, canvasHeight, markSizeScaleFactor)) {
                setCanvasSize((int) canvasWidth, (int) canvasHeight);
                Context2d context2d = getCanvas().getContext2d();
                // draw the course mark
                markVectorGraphics.drawMarkToCanvas(context2d, isSelected, canvasWidth, canvasHeight, markSizeScaleFactor);
                // draw the buoy zone
                if (showBuoyZone && isMarkWithBuoyZone(mark) && buoyZoneRadiusInPixel > MIN_BUOYZONE_RADIUS_IN_PX) {
                    CssColor grayTransparentColor = CssColor.make("rgba(37,158,255,0.75)");
                    // this translation is important for drawing lines with a real line width of 1 pixel
                    context2d.setStrokeStyle(grayTransparentColor);
                    context2d.setLineWidth(1.0);
                    context2d.beginPath();
                    context2d.arc(buoyZoneRadiusInPixel + 1, buoyZoneRadiusInPixel + 1, buoyZoneRadiusInPixel, 0,
                            Math.PI * 2, true);
                    context2d.closePath();
                    context2d.stroke();
                }
                lastBuoyZoneRadius = buoyZoneRadius;
                lastScaleFactor = markSizeScaleFactor;
                lastShowBuoyZone = showBuoyZone;
                lastIsSelected = isSelected;
                lastWidth = canvasWidth;
                lastHeight = canvasHeight;
            }
            final Bearing rotation = markVectorGraphics.getRotationInDegrees(coursePositionsDTO);
            if (rotation != null) {
                updateDrawingAngleAndSetCanvasRotation(coordinateSystem.mapDegreeBearing(rotation.getDegrees()));
            }
            Point buoyPositionInPx = getMapProjection().fromLatLngToDivPixel(coordinateSystem.toLatLng(position));
            if (showBuoyZone && isMarkWithBuoyZone(mark) && buoyZoneRadiusInPixel > MIN_BUOYZONE_RADIUS_IN_PX) {
                setCanvasPosition(buoyPositionInPx.getX() - buoyZoneRadiusInPixel, buoyPositionInPx.getY() - buoyZoneRadiusInPixel);
            } else {
                setCanvasPosition(buoyPositionInPx.getX() - canvasWidth / 2.0, buoyPositionInPx.getY() - canvasHeight / 2.0);
            }
        }
    }

    private boolean isMarkWithBuoyZone(MarkDTO mark) {
        return mark.type == null || mark.type == MarkType.BUOY || mark.type == MarkType.STARTBOAT || 
                mark.type == MarkType.FINISHBOAT || mark.type == MarkType.LANDMARK;
    }
    
    /**
     * Compares the drawing parameters to {@link #lastLegType} and the other <code>last...</code>. If anything has
     * changed, the result is <code>true</code>.
     */
    private boolean needToDraw(boolean showBuoyZone, boolean isSelected, Distance buoyZoneRadius, double width, double height, double scaleFactor) {
        return lastShowBuoyZone == null || lastShowBuoyZone != showBuoyZone ||
               lastIsSelected == null || lastIsSelected != isSelected ||
               lastBuoyZoneRadius == null || !lastBuoyZoneRadius.equals(buoyZoneRadius) ||
               lastScaleFactor == null || lastScaleFactor != scaleFactor ||
               lastWidth == null || lastWidth != width ||
               lastHeight == null || lastHeight != height;
    }

    public Util.Pair<Double, Size> getMarkScaleAndSize(Position markPosition) {
        double minMarkHeight = 20;
        // the original buoy vector graphics is too small (2.1m x 1.5m) for higher zoom levels
        // therefore we scale the buoys with factor 2 by default
        double buoyScaleFactor = 2.0;
        Size markSizeInPixel = calculateBoundingBox(getMapProjection(), markPosition,
                markVectorGraphics.getMarkWidth().scale(buoyScaleFactor), markVectorGraphics.getMarkHeight().scale(buoyScaleFactor));
        double markHeightInPixel = markSizeInPixel.getHeight();
        if (markHeightInPixel < minMarkHeight) {
            markHeightInPixel = minMarkHeight;
        }
        // The coordinates of the canvas drawing methods are based on the 'centimeter' unit (1px = 1cm).
        // To calculate the display real mark size the scale factor from canvas units to the real   
        double markSizeScaleFactor = markHeightInPixel / markVectorGraphics.getMarkHeight().scale(100).getMeters();
        return new Util.Pair<Double, Size>(markSizeScaleFactor, Size.newInstance(markHeightInPixel * 2.0, markHeightInPixel * 2.0));
    }

    private String getTitle() {
        return mark.getName();
    }
    
    public boolean isShowBuoyZone() {
        return showBuoyZone;
    }

    public void setShowBuoyZone(boolean showBuoyZone) {
        this.showBuoyZone = showBuoyZone;
    }
    
    public void setCourse(CoursePositionsDTO coursePositionsDTO) {
        this.coursePositionsDTO = coursePositionsDTO;
    }

    public MarkDTO getMark() {
        return mark;
    }

    public void setMarkPosition(Position position, long transitionTimeInMillis) {
        updateTransition(transitionTimeInMillis);
        this.position = position;
    }
    
    public void setMarkPosition(LatLng positionLatLng) {
        position = coordinateSystem.getPosition(positionLatLng);
        draw();
    }
    
    /**
     * The real-world position where the mark displayed by this overlay is located
     */
    public Position getPosition() {
        return this.position;
    }

    /**
     * The {@link LatLng} position where the mark is shown on the map. This is transformed through the
     * {@link CoordinateSystem} in place for the {@link RaceMap}.
     */
    public LatLng getMarkLatLngPosition() {
        return coordinateSystem.toLatLng(position);
    }

    public Distance getBuoyZoneRadius() {
        return buoyZoneRadius;
    }

    public void setBuoyZoneRadius(Distance buoyZoneRadius) {
        this.buoyZoneRadius = buoyZoneRadius;
    }
}
