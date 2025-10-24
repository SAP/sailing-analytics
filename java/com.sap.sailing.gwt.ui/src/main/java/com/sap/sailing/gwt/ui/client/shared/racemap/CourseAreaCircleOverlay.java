package com.sap.sailing.gwt.ui.client.shared.racemap;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.TextAlign;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.core.client.GWT;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.overlays.overlayhandlers.OverlayViewMethods;
import com.google.gwt.maps.client.overlays.overlayhandlers.OverlayViewOnAddHandler;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.racemap.CanvasOverlayV3;

/**
 * A google map overlay based on a HTML5 canvas for drawing a course area circle with a center point that has
 * the course area name written next to it.
 */
public class CourseAreaCircleOverlay extends CanvasOverlayV3 {
    private static final double DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH = 3.0;
    private static final CssColor DEFAULT_COURSE_AREA_CIRCLE_COLOR = CssColor.make("rgba(37,158,255,0.75)");
    private static final int CENTER_CROSS_SIZE_IN_PIXELS = 10;
    
    /**
     * The course area whose circle to draw
     */
    private final CourseAreaDTO courseArea;
    private final StringMessages stringMessages;
    
    public CourseAreaCircleOverlay(MapWidget map, int zIndex, CourseAreaDTO courseArea, CoordinateSystem coordinateSystem, StringMessages stringMessages) {
        super(map, zIndex, coordinateSystem);
        this.courseArea = courseArea;
        this.stringMessages = stringMessages;
        setCanvasSize(50, 50);
    }
    
    @Override
    protected void draw() {
        if (getMapProjection() != null && courseArea != null && getPosition() != null && courseArea.getRadius() != null) {
            getCanvas().setTitle(getTitle());
            // calculate canvas size
            final double courseAreaRadiusInPixel = calculateRadiusOfBoundingBoxInPixels(getMapProjection(), courseArea.getCenterPosition(), courseArea.getRadius());
            final int canvasEdgeLength = 2*(int) courseAreaRadiusInPixel + (int) DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH;
            if (canvasEdgeLength >= 2<<12 || canvasEdgeLength*canvasEdgeLength > 1<<26) {
                GWT.log("Course area circle canvas for "+courseArea.getName()+" would get too large ("+
                        canvasEdgeLength+"x"+canvasEdgeLength+", area "+canvasEdgeLength*canvasEdgeLength+". Not drawing.");
                setCanvasSize(0, 0);
            } else {
                setCanvasSize(2*(int) courseAreaRadiusInPixel + (int) DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH, 2*(int) courseAreaRadiusInPixel + (int) DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH);
                Context2d context2d = getCanvas().getContext2d();
                // draw the course area circle
                // this translation is important for drawing lines with a real line width of 1 pixel
                context2d.setStrokeStyle(DEFAULT_COURSE_AREA_CIRCLE_COLOR);
                context2d.setFillStyle(DEFAULT_COURSE_AREA_CIRCLE_COLOR);
                context2d.setLineWidth(DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH);
                context2d.beginPath();
                context2d.arc(courseAreaRadiusInPixel + 1, courseAreaRadiusInPixel + 1, courseAreaRadiusInPixel, 0, Math.PI * 2, true);
                context2d.closePath();
                context2d.stroke();
                context2d.setLineWidth(1.0); // draw only a fine 1px center cross
                context2d.moveTo(courseAreaRadiusInPixel+(int) DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH/2-CENTER_CROSS_SIZE_IN_PIXELS/2, courseAreaRadiusInPixel+(int) DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH/2);
                context2d.lineTo(courseAreaRadiusInPixel+(int) DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH/2+CENTER_CROSS_SIZE_IN_PIXELS/2, courseAreaRadiusInPixel+(int) DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH/2);
                context2d.moveTo(courseAreaRadiusInPixel+(int) DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH/2, courseAreaRadiusInPixel+(int) DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH/2-CENTER_CROSS_SIZE_IN_PIXELS/2);
                context2d.lineTo(courseAreaRadiusInPixel+(int) DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH/2, courseAreaRadiusInPixel+(int) DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH/2+CENTER_CROSS_SIZE_IN_PIXELS/2);
                context2d.stroke();
                context2d.setTextAlign(TextAlign.CENTER);
                context2d.setFont("16px arial");
                context2d.fillText(courseArea.getName(), courseAreaRadiusInPixel+ (int) DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH/2, courseAreaRadiusInPixel+ (int) DEFAULT_COURSE_AREA_CIRCLE_LINE_WIDTH/2+2*CENTER_CROSS_SIZE_IN_PIXELS);
                Point courseAreaPositionInPx = getMapProjection().fromLatLngToDivPixel(coordinateSystem.toLatLng(getPosition()));
                setCanvasPosition(courseAreaPositionInPx.getX() - courseAreaRadiusInPixel, courseAreaPositionInPx.getY() - courseAreaRadiusInPixel);
            }
        }
    }

    @Override
    protected OverlayViewOnAddHandler getOnAddHandler() {
        return new OverlayViewOnAddHandler() {
            @Override
            public void onAdd(OverlayViewMethods methods) {
                methods.getPanes().getMapPane().appendChild(canvas.getElement());
                CourseAreaCircleOverlay.this.onAttach();
            }
        };
    }

    private String getTitle() {
        return stringMessages.courseArea()+": "+courseArea.getName();
    }
    
    public CourseAreaDTO getCourseArea() {
        return courseArea;
    }

    /**
     * The real-world position where the mark displayed by this overlay is located
     */
    public Position getPosition() {
        return getCourseArea().getCenterPosition();
    }

    /**
     * The {@link LatLng} position where the mark is shown on the map. This is transformed through the
     * {@link CoordinateSystem} in place for the {@link RaceMap}.
     */
    public LatLng getMarkLatLngPosition() {
        return coordinateSystem.toLatLng(getPosition());
    }
}
