package com.sap.sailing.gwt.ui.simulator;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.TextAlign;
import com.google.gwt.core.client.GWT;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.events.click.ClickMapEvent;
import com.google.gwt.maps.client.events.click.ClickMapHandler;
import com.google.gwt.maps.client.events.mouseout.MouseOutMapEvent;
import com.google.gwt.maps.client.events.mouseout.MouseOutMapHandler;
import com.google.gwt.maps.client.overlays.Circle;
import com.google.gwt.maps.client.overlays.CircleOptions;
import com.sap.sailing.gwt.ui.client.shared.racemap.ImageTransformer;
import com.sap.sailing.gwt.ui.simulator.racemap.FullCanvasOverlay;
import com.sap.sailing.gwt.ui.simulator.util.SimulatorResources;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;

public class RegattaAreaCanvasOverlay extends FullCanvasOverlay {

    private static SimulatorResources resources = GWT.create(SimulatorResources.class);

    private RaceCourseCanvasOverlay raceCourseCanvasOverlay;
    private VenueDescriptor venue; 
    private CourseAreaDescriptor currentCourseArea = null;
    
    private boolean pan;
    private double raceBearing = 0.0;
    private double diffBearing = 0.0;

    private ImageTransformer windRoseBackground;
    private ImageTransformer windRoseNeedle;
    private int windBackgroundOffset = 20;
    private int windNeedleOffset = 30;
    
    public RegattaAreaCanvasOverlay(MapWidget map, int zIndex, char event) {
        super(map, zIndex);
        this.venue = VenueDescriptorFactory.createVenue(event);

        windRoseBackground = new ImageTransformer(resources.windRoseBackground());
        windRoseNeedle = new ImageTransformer(resources.windRoseNeedle());
    }

    @Override
    public void addToMap() {
        super.addToMap();
        
        // TODO MigrationV3: 
//        getPane().add(windRoseBackground.getCanvas(), getWidgetPosLeft() + windBackgroundOffset, getWidgetPosTop());
//        getPane().add(windRoseNeedle.getCanvas(), getWidgetPosLeft() + windBackgroundOffset + windNeedleOffset,
//                getWidgetPosTop() + windNeedleOffset);

        int counter = 1;
        for (CourseAreaDescriptor courseArea : venue.getCourseAreas()) {
            drawCircleFromRadius(counter++, courseArea);
        }
        
        map.panTo(venue.getCenterPos());
    }

    @Override
    protected void draw() {
        super.draw();

        if(mapProjection != null) {
            // TODO MigrationV3: 
//            getPane().setWidgetPosition(windRoseBackground.getCanvas(), getWidgetPosLeft() + windBackgroundOffset,
//                    getWidgetPosTop());
//            getPane().setWidgetPosition(windRoseNeedle.getCanvas(),
//                    getWidgetPosLeft() + windBackgroundOffset + windNeedleOffset, getWidgetPosTop() + windNeedleOffset);

            clearCanvas();
            drawRegattaAreas();
        }
    }

    private void clearCanvas() {
        canvas.getContext2d().clearRect(0.0 /* canvas.getAbsoluteLeft() */, 0.0/* canvas.getAbsoluteTop() */,
                canvas.getCoordinateSpaceWidth(), canvas.getCoordinateSpaceHeight());
    }

    protected void drawRegattaAreas() {
        windRoseBackground.drawTransformedImage(0.0, 1.0);
        windRoseNeedle.drawTransformedImage(raceBearing + 180.0, 1.0);

        LatLng cPos = LatLng.newInstance(54.4344, 10.19659167);
        Point centerPoint = mapProjection.fromLatLngToDivPixel(cPos);
        Point borderPoint = mapProjection.fromLatLngToDivPixel(this.getEdgePoint(cPos, 0.015));
        double pxStroke = Math.pow(2.0, (getMap().getZoom() - 10.0) / 2.0);

        final Context2d context2d = canvas.getContext2d();
        context2d.setLineWidth(3);
        context2d.setStrokeStyle("Black");

        for (CourseAreaDescriptor courseArea : venue.getCourseAreas()) {
            centerPoint = mapProjection.fromLatLngToDivPixel(courseArea.getCenterPos());
            borderPoint = mapProjection.fromLatLngToDivPixel(courseArea.getEdgePos());
            drawCourseArea(courseArea.getName(), context2d, centerPoint, borderPoint, courseArea.getColor(), courseArea.getColorText(),
                    pxStroke);
        }
    }

    // TODO MigrationV3: This is not the right place to create Circles being not drawn to the canvas -> move to SimulatorMap
    public void drawCircleFromRadius(int regIdx, CourseAreaDescriptor courseArea) {
        CircleOptions circleOptions = CircleOptions.newInstance();
        circleOptions.setStrokeColor("white");
        circleOptions.setStrokeWeight(1);
        circleOptions.setStrokeOpacity(0.0);
        circleOptions.setFillColor("green");
        circleOptions.setFillOpacity(0.0);
        circleOptions.setCenter(courseArea.getCenterPos());
        Circle circle = Circle.newInstance(circleOptions);
        circle.setRadius(courseArea.getRadius());
        
        final int regIdxFinal = regIdx;

        circle.addClickHandler(new ClickMapHandler() {
            public void onEvent(ClickMapEvent e) {
                // System.out.println("Click: "+currentRegArea.name);
                CourseAreaDescriptor newRegArea = venue.getCourseAreas().get(regIdxFinal);

                if (newRegArea != currentCourseArea) {
                    currentCourseArea = newRegArea;
                    // simulatorMap.clearOverlays();
                    updateRaceCourse(0, 0);
                    raceCourseCanvasOverlay.draw();
                }

                pan = true;
                map.panTo(currentCourseArea.getCenterPos());
            }
        });

        circle.setMap(getMap());
        getMap().addMouseOutMoveHandler(new MouseOutMapHandler() {
            public void onEvent(MouseOutMapEvent event) {
                if (pan) {
                    pan = false;
                    if (map.getZoom() < 14) {
                        map.setZoom(14);
                    }
                }

            };
        });
    }

    protected void updateRaceCourse(int type, double bearing) {

        if (type == 1) {
            raceBearing = bearing;
            windRoseNeedle.drawTransformedImage(raceBearing + 180.0, 1.0);
        } else if (type == 2) {
            diffBearing = bearing;
        }
        if (currentCourseArea != null) {
            // simulatorMap.getMainPanel().setUpdateButtonEnabled(true);
            LatLng startPoint;
            LatLng endPoint;
            if (raceCourseCanvasOverlay.raceCourseDirection == SailingSimulatorConstants.LegTypeDownwind) {
                startPoint = getDistantPoint(currentCourseArea.getCenterPos(), 0.9 * currentCourseArea.getRadius(), 0.0 + raceBearing - diffBearing);
                endPoint = getDistantPoint(currentCourseArea.getCenterPos(), 0.9 * currentCourseArea.getRadius(), 180.0 + raceBearing - diffBearing);
            } else {
                startPoint = getDistantPoint(currentCourseArea.getCenterPos(), 0.9 * currentCourseArea.getRadius(), 180.0 + raceBearing - diffBearing);
                endPoint = getDistantPoint(currentCourseArea.getCenterPos(), 0.9 * currentCourseArea.getRadius(), 0.0 + raceBearing - diffBearing);
            }
            raceCourseCanvasOverlay.setStartEndPoint(startPoint, endPoint);
        } else {
            // simulatorMap.getMainPanel().setUpdateButtonEnabled(false);
        }
    }

    protected void drawCourseArea(String name, Context2d context2d, Point centerPoint, Point borderPoint,
            String color, String colorText, double pxStroke) {

        Point diffPoint = Point.newInstance(centerPoint.getX() - borderPoint.getX(),
                centerPoint.getY() - borderPoint.getY());
        double pxRadius = Math.sqrt(diffPoint.getX() * diffPoint.getX() + diffPoint.getY() * diffPoint.getY());

        context2d.setGlobalAlpha(1.0f);
        context2d.setFillStyle("#DEDEDE");
        context2d.setLineWidth(pxStroke);
        context2d.setStrokeStyle(color);
        context2d.beginPath();
        context2d.arc(centerPoint.getX() - this.getWidgetPosLeft(), centerPoint.getY() - this.getWidgetPosTop(),
                pxRadius, 0.0, 2 * Math.PI);
        context2d.closePath();
        context2d.fill();
        context2d.stroke();

        context2d.setGlobalAlpha(0.4f);
        context2d.setFillStyle(color);
        context2d.beginPath();
        context2d.arc(centerPoint.getX() - this.getWidgetPosLeft(), centerPoint.getY() - this.getWidgetPosTop(),
                pxRadius, 0.0, 2 * Math.PI);
        context2d.closePath();
        context2d.fill();

        if (getMap().getZoom() >= 11) {
            context2d.setGlobalAlpha(0.8f);
            context2d.setFillStyle(colorText);

            double fontsize = 14.0 + (32.0 - 14.0) * (getMap().getZoom() - 11.0) / (14.0 - 11.0);
            context2d.setFont("normal " + fontsize + "px Calibri");
            // TextMetrics txtmet = context2d.measureText(name);
            context2d.setTextAlign(TextAlign.CENTER);
            context2d.fillText(name, centerPoint.getX() - this.getWidgetPosLeft(), centerPoint.getY() + 0.32 * fontsize
                    - this.getWidgetPosTop());

            context2d.setGlobalAlpha(1.0f);
        }

    }

    protected LatLng getEdgePoint(LatLng pos, double dist) {
        return getDistantPoint(pos, dist, 0.0);
    }

    protected LatLng getDistantPoint(LatLng pos, double dist, double degBear) {

        double lat1 = pos.getLatitude() / 180. * Math.PI;
        double lon1 = pos.getLongitude() / 180. * Math.PI;

        double brng = degBear * Math.PI / 180;

        double R = 6371;
        double d = 1.852 * dist;
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / R) + Math.cos(lat1) * Math.sin(d / R) * Math.cos(brng));
        double lon2 = lon1
                + Math.atan2(Math.sin(brng) * Math.sin(d / R) * Math.cos(lat1),
                        Math.cos(d / R) - Math.sin(lat1) * Math.sin(lat2));
        lon2 = (lon2 + 3 * Math.PI) % (2 * Math.PI) - Math.PI; // normalize to -180� ... +180�*/

        double lat2deg = lat2 / Math.PI * 180;
        double lon2deg = lon2 / Math.PI * 180;

        LatLng result = LatLng.newInstance(lat2deg, lon2deg);

        return result;
    }

    public void setRaceCourseCanvas(RaceCourseCanvasOverlay rcCanvas) {
        this.raceCourseCanvasOverlay = rcCanvas;
    }

}
