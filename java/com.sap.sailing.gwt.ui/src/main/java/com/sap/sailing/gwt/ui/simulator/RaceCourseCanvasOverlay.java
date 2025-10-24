package com.sap.sailing.gwt.ui.simulator;

import java.util.logging.Logger;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.events.click.ClickMapEvent;
import com.google.gwt.maps.client.events.click.ClickMapHandler;
import com.google.gwt.maps.client.events.dblclick.DblClickMapEvent;
import com.google.gwt.maps.client.events.dblclick.DblClickMapHandler;
import com.google.gwt.maps.client.events.mousemove.MouseMoveMapEvent;
import com.google.gwt.maps.client.events.mousemove.MouseMoveMapHandler;
import com.google.gwt.maps.client.geometrylib.SphericalUtils;
import com.google.gwt.maps.client.overlays.Marker;
import com.sap.sailing.domain.common.Mile;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.simulator.racemap.FullCanvasOverlay;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;

/**
 * This class implements the layer to display the race course on the map. Currently the course only consists of the
 * start(startPoint) and end point(endPoint) which are captured by a single and double click respectively.
 * 
 * @author Nidhi Sawhney(D054070)
 * 
 */
public class RaceCourseCanvasOverlay extends FullCanvasOverlay {

    private String racecourseColor = "White";
    private double racecourseBuoySize = 5;
    private char mode;
    
    // TODO: make private!!!
    public char raceCourseDirection;
    
    private LatLng startPoint;
    private LatLng endPoint;
    private Marker startMarker;
    private Marker endMarker;

    private HandlerRegistration registeredRaceCourseMouseMoveHandler;
    
    private static Logger logger = Logger.getLogger(RaceCourseCanvasOverlay.class.getName());

    private class RaceCourseMapMouseMoveHandler implements MouseMoveMapHandler {
        @Override
        public void onEvent(MouseMoveMapEvent event) {
            if (startPoint != null) {
                Point s = getMapProjection().fromLatLngToDivPixel(startPoint);
                drawPointWithText(s.getX(), s.getY(), "Start");
                refreshLine(event.getMouseEvent().getLatLng(), "Grey");
            }

        }
    }

    public RaceCourseCanvasOverlay(MapWidget map, int zIndex, char mode, CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);

        this.mode = mode;
        startPoint = null;
        endPoint = null;
        startMarker = null;
        endMarker = null;

        getCanvas().getElement().setClassName("raceCourse");

        if (this.mode == SailingSimulatorConstants.ModeFreestyle) {

        	map.addClickHandler(new ClickMapHandler() {
        		@Override
        		public void onEvent(ClickMapEvent event) {
        			if (startPoint == null) {
        				startPoint = event.getMouseEvent().getLatLng();
        				logger.fine("Clicked startPoint here " + startPoint);
        				if (startPoint != null) {

        					setCanvasSettings();
        					// drawCanvas();
        					setStartPoint(startPoint);
        					registeredRaceCourseMouseMoveHandler = getMap().addMouseMoveHandler(new RaceCourseMapMouseMoveHandler());
        				}
        			}
        		}
        	});

        	map.addDblClickHandler(new DblClickMapHandler() {
        		@Override
        		public void onEvent(DblClickMapEvent event) {
        			if (isSelected) {
        				endPoint = event.getMouseEvent().getLatLng();
        				logger.info("Clicked endPoint " + "here " + endPoint);
        				if (endPoint != null) {
        					setEndPoint(endPoint);
        					center();
        					registeredRaceCourseMouseMoveHandler.removeHandler();
        				}
        			}
        		}
        	});
        }

    }

    public void reset() {
        startPoint = null;
        endPoint = null;

        if (startMarker != null) {
            startMarker.setMap((MapWidget) null);
            startMarker = null;
        }
        if (endMarker != null) {
            endMarker.setMap((MapWidget) null);
            endMarker = null;
        }
        setCanvasSettings();
    }

    public boolean isCourseSet() {
        return startPoint != null && endPoint != null;
    }

    public void setStartEndPoint(LatLng startPoint, LatLng endPoint) {
    	double zoomLevel = map.getZoom();
    	racecourseBuoySize = 1.0 + (5.0 - 1.0)*(zoomLevel - 10.0)/(14.0 - 10.0);
        setStartPoint(startPoint);
        setEndPoint(endPoint);        
    }

    private void setStartPoint(LatLng startPoint) {
        this.startPoint = startPoint;
    }

    private void setEndPoint(LatLng endPoint) {
        this.endPoint = endPoint;
    }

    @Override
    protected void draw() {
        if (getMapProjection() != null && startPoint != null && endPoint != null) {
            setCanvasSettings();
            setStartEndPoint(startPoint, endPoint);
            if (startPoint != null) {
                Point point = getMapProjection().fromLatLngToDivPixel(startPoint);
                if (racecourseBuoySize > 0.0) {
                	drawCircleWithText(point.getX() - getWidgetPosLeft(), point.getY() - getWidgetPosTop(), racecourseBuoySize,
                			racecourseColor, "Start");
                }
                if (startMarker != null) {
                    startMarker.setPosition(startPoint);
                } else {
                    // default markers are too large cluttering race display
                    /*
                     * startMarker = new Marker(startPoint); map.addOverlay(startMarker); /*
                     * startMarker.addMarkerMouseOverHandler(new MarkerMouseOverHandler () {
                     * 
                     * @Override public void onMouseOver(MarkerMouseOverEvent event) { InfoWindowContent content = new
                     * InfoWindowContent("Start"); map.getInfoWindow().open(startMarker, content); }
                     * 
                     * });
                     * 
                     * startMarker.addMarkerMouseOutHandler(new MarkerMouseOutHandler () {
                     * 
                     * @Override public void onMouseOut(MarkerMouseOutEvent event) { map.getInfoWindow().close(); }
                     * 
                     * });
                     */
                }
            }
            if (endPoint != null) {
            	Point point = getMapProjection().fromLatLngToDivPixel(endPoint);
            	if (racecourseBuoySize > 0.0) {
            		drawCircleWithText(point.getX() - getWidgetPosLeft(), point.getY() - getWidgetPosTop(), racecourseBuoySize,
            				racecourseColor, "End");
            	}
                if (endMarker != null) {
                    endMarker.setPosition(endPoint);
                } else {
                    // default markers are too large cluttering race display
                    /*
                     * endMarker = new Marker(endPoint); map.addOverlay(endMarker);
                     * 
                     * endMarker.addMarkerMouseOverHandler(new RaceCourseMarkerMouseOverHandler());
                     * 
                     * endMarker.addMarkerMouseOutHandler(new MarkerMouseOutHandler() {
                     * 
                     * @Override public void onMouseOut(MarkerMouseOutEvent event) { map.getInfoWindow().close(); }
                     * 
                     * });
                     */
                }
            }
            drawLine(endPoint, racecourseColor);
        }
    }

    /**
     * Draw a line on the canvas from the startPoint to current point given by mouse location with a canvas refresh
     */
    private void refreshLine(LatLng currentPoint, String color) {
        setCanvasSettings();
        drawLine(currentPoint, color);
    }

    private void drawLine(LatLng currentPoint, String color) {
        if (startPoint != null) {
            Point s = getMapProjection().fromLatLngToDivPixel(startPoint);
            Point e = getMapProjection().fromLatLngToDivPixel(currentPoint);
            Context2d context2d = canvas.getContext2d();
            context2d.setGlobalAlpha(0.4f);
            drawLine(s.getX() - getWidgetPosLeft(), s.getY() - getWidgetPosTop(), e.getX() - getWidgetPosLeft(),
                    e.getY() - getWidgetPosTop(), 2.0, color);
            context2d.setGlobalAlpha(1.0f);
            double distanceInNmi = SphericalUtils.computeDistanceBetween(startPoint, currentPoint) / Mile.METERS_PER_NAUTICAL_MILE;
            canvas.setTitle("Distance (nmi)  " + NumberFormat.getFormat("0.00").format(distanceInNmi));
        }
    }

    private void center() {
        double cLat = (startPoint.getLatitude() + ((endPoint.getLatitude() - startPoint.getLatitude()) % 180.) / 2. + 90.) % 180. - 90.;
        double cLon = (startPoint.getLongitude() + ((endPoint.getLongitude() - startPoint.getLongitude()) % 360.) / 2. + 180.) % 360. - 180;
        // startPoint and endPoint are already in the map's coordinate space, so no further mapping through coordinateSystem is required here
        LatLng centerPoint = LatLng.newInstance(cLat, cLon);
        getMap().panTo(centerPoint);
    }

    public LatLng getStartPoint() {
        return startPoint;
    }

    public LatLng getEndPoint() {
        return endPoint;
    }
}
