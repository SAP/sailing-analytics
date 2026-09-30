package com.sap.sailing.gwt.ui.simulator;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.events.click.ClickMapEvent;
import com.google.gwt.maps.client.events.click.ClickMapHandler;
import com.google.gwt.maps.client.mvc.MVCArray;
import com.google.gwt.maps.client.overlays.MapCanvasProjection;
import com.google.gwt.maps.client.overlays.Marker;
import com.google.gwt.maps.client.overlays.MarkerOptions;
import com.google.gwt.maps.client.overlays.OverlayView;
import com.google.gwt.maps.client.overlays.Polyline;
import com.google.gwt.maps.client.overlays.PolylineOptions;
import com.google.gwt.maps.client.overlays.overlayhandlers.OverlayViewMethods;
import com.google.gwt.maps.client.overlays.overlayhandlers.OverlayViewOnDrawHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.ui.client.SimulatorServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.shared.RequestTotalTimeDTO;
import com.sap.sailing.gwt.ui.shared.ResponseTotalTimeDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorUISelectionDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorWindDTO;
import com.sap.sailing.gwt.ui.simulator.racemap.TwoDPoint;
import com.sap.sailing.gwt.ui.simulator.racemap.TwoDSegment;
import com.sap.sailing.gwt.ui.simulator.racemap.TwoDVector;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Mile;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.RadianPosition;
import com.sap.sse.gwt.client.ErrorReporter;

/**
 * This class represents the path polyline overlay on the GWT map. This polyline is constructed with an array of turn
 * points, saved as the turnPoints property.
 * 
 * The GWT polyline offers little to no help in setting programatically a vertices, so I apply every geometry rule onto
 * the internal array of turn points, and then reconstruct the polyline after every movement.
 * 
 * The main method of this class is drawPolylineOnMap, in which a new GWT polyline is created with an update handler
 * that handles every geometry related issue.
 * 
 * @author I077899 Bogdan Mihai
 * 
 */
public class PathPolyline {

    public final static String DEFAULT_COLOR = "#8B0000";
    public final static String END_USER_NAME = "What-If Course";

    private final static int DEFAULT_WEIGHT = 3;
    private final static double DEFAULT_OPACITY = 1.0;
    private final static double SMOOTHNESS_MAX_DEG = 20.0;
    private final static double EPSILON = 1e-10;
    private final static int STEP_DURATION_MILLISECONDS = 2000;
    private final static boolean USE_REAL_AVERAGE_WIND = true;

    private Polyline polyline = null;
    private LatLng[] turnPoints = null;
    private String color = "";
    private int weight = 0;
    private double opacity = 0.0;
    private int selectedBoatClassIndex = 0;
    private int selectedRaceIndex = 0;
    private int selectedCompetitorIndex = 0;
    private int selectedLegIndex = 0;
    private List<SimulatorWindDTO> allPoints = null;
    private SimulatorServiceAsync simulatorService = null;
    private MapWidget map = null;
    protected OverlayView mapView;
    protected MapCanvasProjection mapProjection;
    private ErrorReporter errorReporter = null;
    private boolean warningAlreadyShown = false;
    private SimulatorMap simulatorMap = null;
    private SimulatorMainPanel simulatorMainPanel = null;
    private boolean getTotalTimeFactor = false;
    private double totalTimeFactor = 0.0;

    public static PathPolyline createPathPolyline(List<SimulatorWindDTO> pathPoints, String color, int weight, double opacity, ErrorReporter errorReporter,
            SimulatorServiceAsync simulatorService,
            MapWidget map, SimulatorMap simulatorMap, SimulatorMainPanel simulatorMainPanel, SimulatorUISelectionDTO selection, CoordinateSystem coordinateSystem) {
        List<LatLng> points = new ArrayList<LatLng>();
        for (SimulatorWindDTO pathPoint : pathPoints) {
            if (pathPoint.isTurn) {
                points.add(coordinateSystem.toLatLng(pathPoint.position));
            }
        }
        return new PathPolyline(points.toArray(new LatLng[0]), color, weight, opacity, selection, errorReporter, pathPoints, simulatorService, map,
                simulatorMap, simulatorMainPanel);
    }

    public static PathPolyline createPathPolyline(List<SimulatorWindDTO> pathPoints, ErrorReporter errorReporter, SimulatorServiceAsync simulatorService,
            MapWidget map, SimulatorMap simulatorMap, SimulatorMainPanel simulatorMainPanel, SimulatorUISelectionDTO selection, CoordinateSystem coordinateSystem) {
        return createPathPolyline(pathPoints, DEFAULT_COLOR, DEFAULT_WEIGHT, DEFAULT_OPACITY, errorReporter, simulatorService, map, simulatorMap,
                simulatorMainPanel, selection, coordinateSystem);
    }

    private PathPolyline(LatLng[] points, String color, int weight, double opacity, SimulatorUISelectionDTO selection, ErrorReporter errorReporter,
            List<SimulatorWindDTO> pathPoints, SimulatorServiceAsync simulatorService, MapWidget map, SimulatorMap simulatorMap,
            SimulatorMainPanel simulatorMainPanel) {

        this.turnPoints = points;
        this.color = color;
        this.weight = weight;
        this.opacity = opacity;
        this.allPoints = pathPoints;
        this.simulatorService = simulatorService;
        this.map = map;
        this.mapView = OverlayView.newInstance(map, new OverlayViewOnDrawHandler() {
            @Override
            public void onDraw(OverlayViewMethods methods) {
                mapProjection = methods.getProjection();
            }
        }, null, null);
        this.selectedBoatClassIndex = selection.boatClassIndex;
        this.selectedRaceIndex = selection.raceIndex;
        this.selectedCompetitorIndex = selection.competitorIndex;
        this.selectedLegIndex = selection.legIndex;
        this.errorReporter = errorReporter;
        this.simulatorMap = simulatorMap;
        this.simulatorMainPanel = simulatorMainPanel;

        this.drawPolylineOnMap();
    }

    private void drawPolylineOnMap() {
        // first call with given sailing setup
        if (polyline == null) {
            getTotalTimeFactor = true;
        } else {
            getTotalTimeFactor = false;
        }
        if (polyline != null) {
            polyline.setMap(null);
        }

        PolylineOptions polylineOptions = PolylineOptions.newInstance();
        polylineOptions.setStrokeColor(color);
        polylineOptions.setStrokeOpacity(opacity);
        polylineOptions.setStrokeWeight(weight);
        polyline = Polyline.newInstance(polylineOptions);

        MVCArray<LatLng> pointsAsArray = MVCArray.newInstance(turnPoints);
        polyline.setPath(pointsAsArray);
        
        // TODO MigrationV3: the Polyline used to support a PolylineLineUpdatedHandler; the new Maps v3 wrapper doesn't; temporarily replacing by a click handler, but a more thorough migration is required here...
        polyline.addClickHandler(new ClickMapHandler() {
            @Override
            public void onEvent(ClickMapEvent event) {

                if (simulatorMainPanel.isPathPolylineFreeMode()) {

                    List<LatLng> newTurnPoints = new ArrayList<LatLng>();
                    for (int index = 0; index < polyline.getPath().getLength(); index++) {
                        newTurnPoints.add(polyline.getPath().get(index));
                    }
                    turnPoints = newTurnPoints.toArray(new LatLng[0]);
                    drawPolylineOnMap();

                } else {

                    final int indexOfMovedPoint = getIndexOfMovedPoint();
                    final int noOfPoints = turnPoints.length;

                    boolean secondPart = false;

                    if (indexOfMovedPoint == 0 || indexOfMovedPoint == noOfPoints - 1 || noOfPoints == 3) {
                        // start and end points cannot be moved!
                        // nor a 3-turns line.
                    } else {

                        Pair newPositionOfMovedPointAndFlag = getNewPositionOfMovedPointCurved(indexOfMovedPoint);
                        LatLng newPositionOfMovedPoint = newPositionOfMovedPointAndFlag.point;
                        Boolean projectionOfAfterEdge = newPositionOfMovedPointAndFlag.flag;

                        LatLng oldPositionOfFirstBeforeMovedPoint = turnPoints[indexOfMovedPoint - 1];
                        LatLng oldPositionOfMovedPoint = turnPoints[indexOfMovedPoint];
                        LatLng oldPositionOfAfterBeforeMovedPoint = turnPoints[indexOfMovedPoint + 1];

                        if (indexOfMovedPoint == 1) {
                            // if the indexOfMovedPoint == 1, only the next point will be changed, as the start one
                            // cannot be moved
                            turnPoints[indexOfMovedPoint + 1] = getNewPositionOfPointAfterMovedCurved(indexOfMovedPoint, newPositionOfMovedPoint);
                            secondPart = true;

                        } else if (indexOfMovedPoint == noOfPoints - 2) {
                            // if indexOfMovedPoint == noOfPoints - 2, only the previous point will be changed, as the
                            // end one cannot be moved.
                            turnPoints[indexOfMovedPoint - 1] = getNewPositionOfPointBeforeMovedCurved(indexOfMovedPoint, newPositionOfMovedPoint);
                            secondPart = false;

                        } else {

                            LatLng possibleNewPositionOfPointBeforeMoved = getNewPositionOfPointBeforeMovedCurved(indexOfMovedPoint, newPositionOfMovedPoint);
                            if (projectionOfAfterEdge
                                    && PathPolyline.equals(possibleNewPositionOfPointBeforeMoved, turnPoints[indexOfMovedPoint - 1], EPSILON) == false) {

                                secondPart = false;

                                //System.out.println("old Point: "+turnPoints[indexOfMovedPoint-1].getLatitude()+", "+turnPoints[indexOfMovedPoint-1].getLongitude());
                                //System.out.println("new Point: "+possibleNewPositionOfPointBeforeMoved.getLatitude()+", "+possibleNewPositionOfPointBeforeMoved.getLongitude());
                                turnPoints[indexOfMovedPoint - 1] = possibleNewPositionOfPointBeforeMoved;

                            }

                            LatLng possibleNewPositionOfPointAfterMoved = getNewPositionOfPointAfterMovedCurved(indexOfMovedPoint, newPositionOfMovedPoint);
                            if (!projectionOfAfterEdge
                                    && PathPolyline.equals(possibleNewPositionOfPointAfterMoved, turnPoints[indexOfMovedPoint + 1], EPSILON) == false) {

                                secondPart = true;
                                turnPoints[indexOfMovedPoint + 1] = possibleNewPositionOfPointAfterMoved;

                            }
                        }

                        turnPoints[indexOfMovedPoint] = newPositionOfMovedPoint;

                        turnPoints = checkIfAnyTurnsMustGo(turnPoints, indexOfMovedPoint, secondPart, oldPositionOfFirstBeforeMovedPoint,
                                oldPositionOfMovedPoint, oldPositionOfAfterBeforeMovedPoint);

                        turnPoints = eliminateSpikes(turnPoints);

                        turnPoints = eliminateTriangles(turnPoints);
                    }

                    drawPolylineOnMap();
                }
            }
        });

        polyline.setMap(map);
        simulatorMap.setPolyline(polyline);
        polyline.setEditable(true);
        // old call: polyline.setEditingEnabled(PolyEditingOptions.newInstance(turnPoints.length - 1));
        
        getTotalTime();
    }

    /**
     * Adds a GWT marker to the map. Used only for debugging purposes.
     */
    @SuppressWarnings("unused")
    private void addMarker(LatLng point, String title) {

        MarkerOptions options = MarkerOptions.newInstance();
        options.setDraggable(false);
        options.setTitle(title);

        Marker marker = Marker.newInstance(options);
        marker.setPosition(point);
        marker.setMap(map);
    }

    /**
     * This member checks if the point before the moved point must be eliminated or not from the turns array. If so, a
     * new turn array is returned, if not the old one is returned.
     * 
     * @param turnPoints
     *            - the array of turns as LatLng objects
     * @param indexOfMovedPoint
     *            - the index of the moved point
     * @param secondBefore
     *            - the second point before the moved one as a TwoDPoint object
     * @param firstBefore
     *            - the new position of the point before the moved one as a TwoDPoint object
     * @param neww
     *            - the new position of the moved point.
     * @param firstAfter
     *            - the new position of the point after the moved one as a TwoDPoint object
     * @param firstAfterEdge
     *            - the edge after the moved point, defined by the moved point and the point after it, defined as a
     *            TwoDSegment.
     * @returns a LatLng[] array.
     */
    private LatLng[] checkIfPreviousMustGo(LatLng[] turnPoints, int indexOfMovedPoint, TwoDPoint secondBefore, TwoDPoint firstBefore, TwoDPoint neww,
            TwoDPoint firstAfter, TwoDSegment firstAfterEdge) {

        List<LatLng> newTurnPoints = new ArrayList<LatLng>();
        boolean newList = false;

        TwoDVector vector = new TwoDVector(neww, firstBefore);
        TwoDPoint projection = secondBefore.getProjectionByVector(firstAfterEdge, vector);
        TwoDSegment segment = new TwoDSegment(firstAfter, projection);

        if (segment.contains(neww, true) == false) {

            newList = true;

            // the current moved point must be replaced with projection
            // the previous point must be eliminated

            for (int index = 0; index < turnPoints.length; index++) {
                if (index == indexOfMovedPoint) {
                    newTurnPoints.add(this.toLatLng(projection));
                } else if (index != indexOfMovedPoint - 1) {
                    newTurnPoints.add(turnPoints[index]);
                }
            }
        }

        if (newList) {
            return newTurnPoints.toArray(new LatLng[0]);
        } else {
            return turnPoints;
        }
    }

    /**
     * This member checks if the previous two points before the moved point must be eliminated or not from the turns
     * array. If so, a new turn array is returned, if not the old one is returned.
     * 
     * @param turnPoints
     *            - the array of turns as LatLng objects
     * @param indexOfMovedPoint
     *            - the index of the moved point
     * @param secondBefore
     *            - the second point before the moved one as a TwoDPoint object
     * @param newFirstBefore
     *            - the new position of the point before the moved one as a TwoDPoint object
     * @param old
     *            - the old position of the moved point.
     * @param neww
     *            - the new position of the moved point.
     * @param firstAfter
     *            - the new position of the point after the moved one as a TwoDPoint object
     * @param firstAfterEdge
     *            - the edge after the moved point, defined by the moved point and the point after it, defined as a
     *            TwoDSegment.
     * @returns a LatLng[] array.
     */
    private LatLng[] checkIfPreviousTwoMustGo(LatLng[] turnPoints, int indexOfMovedPoint, TwoDPoint secondBefore, TwoDPoint oldFirstBefore,
            TwoDPoint newFirstBefore, TwoDPoint old, TwoDPoint neww, TwoDPoint firstAfter, TwoDSegment firstAfterEdge) {

        List<LatLng> newTurnPoints = new ArrayList<LatLng>();
        boolean newList = false;

        TwoDPoint thirdBefore = this.toTwoDPoint(turnPoints[indexOfMovedPoint - 3]);
        TwoDSegment thirdBeforeEdge = new TwoDSegment(secondBefore, thirdBefore);
        TwoDPoint intersection = thirdBeforeEdge.getIntersection(firstAfterEdge);
        TwoDSegment segment = new TwoDSegment(firstAfter, intersection);

        TwoDSegment firstBeforeEdge = new TwoDSegment(neww, newFirstBefore);
        TwoDPoint intersection2 = thirdBeforeEdge.getIntersection(firstBeforeEdge);
        TwoDSegment segment2 = new TwoDSegment(secondBefore, intersection);

        double distanceToNewFirstAfter = neww.getDistanceTo(firstAfter);
        double distanceToOld = neww.getDistanceTo(old);
        TwoDSegment oldFirstAfterEdge = new TwoDSegment(old, firstAfter);

        if (distanceToNewFirstAfter < distanceToOld && oldFirstAfterEdge.contains(neww, true) == false) {

            if (indexOfMovedPoint + 2 < turnPoints.length) {

                TwoDPoint secondAfter = this.toTwoDPoint(turnPoints[indexOfMovedPoint + 2]);
                TwoDSegment secondAfterEdge = new TwoDSegment(firstAfter, secondAfter);
                TwoDSegment secondBeforeEdge = new TwoDSegment(newFirstBefore, secondBefore);
                TwoDPoint intersection3 = secondBeforeEdge.getIntersection(secondAfterEdge);

                newList = true;

                // the moved point and the next one must be eliminated
                // the previous point must be replaced with intersection3

                for (int index = 0; index < turnPoints.length; index++) {

                    if (index == indexOfMovedPoint - 1) {
                        newTurnPoints.add(this.toLatLng(intersection3));
                    } else if (index != indexOfMovedPoint && index != indexOfMovedPoint + 1) {
                        newTurnPoints.add(turnPoints[index]);
                    }

                }

            } else {

                TwoDVector vector = new TwoDVector(old, oldFirstBefore);
                TwoDSegment secondBeforeEdge = new TwoDSegment(newFirstBefore, secondBefore);
                TwoDPoint intersection3 = firstAfter.getProjectionByVector(secondBeforeEdge, vector);

                newList = true;

                // the moved point must be eliminated
                // the previous point must be replaced with intersection3

                for (int index = 0; index < turnPoints.length; index++) {

                    if (index == indexOfMovedPoint - 1) {
                        newTurnPoints.add(this.toLatLng(intersection3));
                    } else if (index != indexOfMovedPoint) {
                        newTurnPoints.add(turnPoints[index]);
                    }
                }
            }

        } else {
            if (segment.contains(neww, true) == false || segment2.contains(intersection2, true)) {

                newList = true;

                // the moved point must be replaced with intersection
                // the previous two points must be eliminated

                for (int index = 0; index < turnPoints.length; index++) {

                    if (index == indexOfMovedPoint) {
                        newTurnPoints.add(this.toLatLng(intersection));
                    } else if (index != indexOfMovedPoint - 1 && index != indexOfMovedPoint - 2) {
                        newTurnPoints.add(turnPoints[index]);
                    }

                }
            }
        }

        if (newList) {
            return newTurnPoints.toArray(new LatLng[0]);
        } else {
            return turnPoints;
        }
    }

    /**
     * This member checks if the next two points after the moved point must be eliminated or not from the turns array.
     * If so, a new turn array is returned, if not the old one is returned.
     * 
     * @param turnPoints
     *            - the array of turns as LatLng objects
     * @param indexOfMovedPoint
     *            - the index of the moved point
     * @param firstBefore
     *            - the new position of the point before the moved one as a TwoDPoint object
     * @param old
     *            - the old position of the moved point.
     * @param neww
     *            - the new position of the moved point.
     * @param newFirstAfter
     *            - the new position of the point after the moved one as a TwoDPoint object
     * @param secondAfter
     *            - the second point after the moved one as a TwoDPoint object
     * @param firstBeforeEdge
     *            - the edge before the moved point, defined by the moved point and the point before it, defined as a
     *            TwoDSegment.
     * @returns a LatLng[] array.
     */
    private LatLng[] checkIfNextTwoMustGo(LatLng[] turnPoints, int indexOfMovedPoint, TwoDPoint firstBefore, TwoDPoint old, TwoDPoint neww,
            TwoDPoint oldFirstAfter, TwoDPoint newFirstAfter, TwoDPoint secondAfter, TwoDSegment firstBeforeEdge) {

        List<LatLng> newTurnPoints = new ArrayList<LatLng>();
        boolean newList = false;

        TwoDPoint thirdAfter = this.toTwoDPoint(turnPoints[indexOfMovedPoint + 3]);
        TwoDSegment thirdAfterEdge = new TwoDSegment(secondAfter, thirdAfter);
        TwoDPoint intersection = thirdAfterEdge.getIntersection(firstBeforeEdge);
        TwoDSegment segment = new TwoDSegment(firstBefore, intersection);

        TwoDSegment firstAfterEdge = new TwoDSegment(neww, newFirstAfter);
        TwoDPoint intersection2 = thirdAfterEdge.getIntersection(firstAfterEdge);
        TwoDSegment segment2 = new TwoDSegment(secondAfter, intersection);

        double distanceToNewFirstBefore = neww.getDistanceTo(firstBefore);
        double distanceToOld = neww.getDistanceTo(old);
        TwoDSegment oldFirstBeforeEdge = new TwoDSegment(old, firstBefore);

        if (distanceToNewFirstBefore < distanceToOld && oldFirstBeforeEdge.contains(neww, true) == false) {

            if (indexOfMovedPoint - 2 >= 0) {

                TwoDPoint secondBefore = this.toTwoDPoint(turnPoints[indexOfMovedPoint - 2]);
                TwoDSegment secondBeforeEdge = new TwoDSegment(secondBefore, firstBefore);
                TwoDSegment secondAfterEdge = new TwoDSegment(newFirstAfter, secondAfter);
                TwoDPoint intersection3 = secondAfterEdge.getIntersection(secondBeforeEdge);

                newList = true;

                // the moved point and the previous one must be eliminated
                // the next point must be replaced with intersection3

                for (int index = 0; index < turnPoints.length; index++) {

                    if (index == indexOfMovedPoint + 1) {
                        newTurnPoints.add(this.toLatLng(intersection3));
                    } else if (index != indexOfMovedPoint && index != indexOfMovedPoint - 1) {
                        newTurnPoints.add(turnPoints[index]);
                    }

                }

            } else {

                TwoDVector vector = new TwoDVector(old, oldFirstAfter);
                TwoDSegment secondAfterEdge = new TwoDSegment(newFirstAfter, secondAfter);
                TwoDPoint intersection3 = firstBefore.getProjectionByVector(secondAfterEdge, vector);

                newList = true;

                // the current moved point must be eliminated
                // the next point must be replaced with intersection3

                for (int index = 0; index < turnPoints.length; index++) {

                    if (index == indexOfMovedPoint + 1) {
                        newTurnPoints.add(this.toLatLng(intersection3));
                    } else if (index != indexOfMovedPoint) {
                        newTurnPoints.add(turnPoints[index]);
                    }
                }
            }

        } else {

            if (segment.contains(neww, true) == false || segment2.contains(intersection2, true)) {

                newList = true;

                // the current moved point must be replaced with intersection
                // the next two points must be eliminated

                for (int index = 0; index < turnPoints.length; index++) {

                    if (index == indexOfMovedPoint) {
                        newTurnPoints.add(this.toLatLng(intersection));
                    } else if (index != indexOfMovedPoint + 1 && index != indexOfMovedPoint + 2) {
                        newTurnPoints.add(turnPoints[index]);
                    }

                }
            }
        }

        if (newList) {
            return newTurnPoints.toArray(new LatLng[0]);
        } else {
            return turnPoints;
        }
    }

    /**
     * This member checks if the point after the moved point must be eliminated or not from the turns array. If so, a
     * new turn array is returned, if not the old one is returned.
     * 
     * @param turnPoints
     *            - the array of turns as LatLng objects
     * @param indexOfMovedPoint
     *            - the index of the moved point
     * @param firstBefore
     *            - the new position of the point before the moved one as a TwoDPoint object
     * @param neww
     *            - the new position of the moved point.
     * @param firstAfter
     *            - the new position of the point after the moved one as a TwoDPoint object
     * @param secondAfter
     *            - the second after point after the moved one as a TwoDPoint object
     * @param firstBeforeEdge
     *            - the edge before the moved point, defined by the moved point and the point before it, defined as a
     *            TwoDSegment.
     * @returns a LatLng[] array.
     */
    private LatLng[] checkIfNextMustGo(LatLng[] turnPoints, int indexOfMovedPoint, TwoDPoint firstBefore, TwoDPoint neww, TwoDPoint firstAfter,
            TwoDPoint secondAfter, TwoDSegment firstBeforeEdge) {

        List<LatLng> newTurnPoints = new ArrayList<LatLng>();
        boolean newList = false;

        TwoDVector vector = new TwoDVector(neww, firstAfter);
        TwoDPoint projection = secondAfter.getProjectionByVector(firstBeforeEdge, vector);
        TwoDSegment segment = new TwoDSegment(firstBefore, projection);

        if (segment.contains(neww, true) == false) {

            newList = true;

            // the current moved point must be replaced with projection
            // the next point must be eliminated

            for (int index = 0; index < turnPoints.length; index++) {
                if (index == indexOfMovedPoint) {
                    newTurnPoints.add(this.toLatLng(projection));
                } else if (index != indexOfMovedPoint + 1) {
                    newTurnPoints.add(turnPoints[index]);
                }
            }
        }

        if (newList) {
            return newTurnPoints.toArray(new LatLng[0]);
        } else {
            return turnPoints;
        }
    }

    /**
     * This member checks if any of the previous points before the moved point or next points after the moved point must
     * be eliminated or not. If so, a new array of turn points is returned, if not, the old array is returned.
     * 
     * @param turnPoints
     *            - the array of turn points as LatLng objects
     * @param indexOfMovedPoint
     *            - the index of the moved point
     * @param secondPart
     *            - this flag shows which part of the turn points array must be checked for changes. If secondPart ==
     *            false, then this method will check for either previous point or previous two points to be eliminated
     *            or not. If secondPart == true, then this method will check for either next point or next two points to
     *            be eliminated or not.
     * @param old_MP
     *            - The old position of the moved point as a LatLng object
     * @returns a LatLng[] array.
     */
    private LatLng[] checkIfAnyTurnsMustGo(LatLng[] turnPoints, int indexOfMovedPoint, boolean secondPart, LatLng oldPositionOfFirstBeforeMovedPoint,
            LatLng oldPositionOfMovedPoint, LatLng oldPositionOfAfterBeforeMovedPoint) {

        LatLng[] result = null;

        TwoDPoint oldFirstBefore = this.toTwoDPoint(oldPositionOfFirstBeforeMovedPoint);
        TwoDPoint old = this.toTwoDPoint(oldPositionOfMovedPoint);
        TwoDPoint oldFirstAfter = this.toTwoDPoint(oldPositionOfAfterBeforeMovedPoint);

        TwoDPoint neww = this.toTwoDPoint(turnPoints[indexOfMovedPoint]);

        TwoDPoint newFirstBefore = this.toTwoDPoint(turnPoints[indexOfMovedPoint - 1]);
        TwoDPoint newFirstAfter = this.toTwoDPoint(turnPoints[indexOfMovedPoint + 1]);

        if (secondPart) {

            // check the points AFTER the moved point

            TwoDPoint secondAfter = this.toTwoDPoint(turnPoints[indexOfMovedPoint + 2]);
            TwoDSegment firstBeforeEdge = new TwoDSegment(newFirstBefore, neww);

            if (indexOfMovedPoint == turnPoints.length - 3) {  // if only one point between moved and end

                // check if only the next point must be eliminated
                result = this.checkIfNextMustGo(turnPoints, indexOfMovedPoint, newFirstBefore, neww, newFirstAfter, secondAfter, firstBeforeEdge);

            } else {

                // check if the next 2 points must be eliminated
                result = this.checkIfNextTwoMustGo(turnPoints, indexOfMovedPoint, newFirstBefore, old, neww, oldFirstAfter, newFirstAfter, secondAfter,
                        firstBeforeEdge);
            }

        } else {

            // check the points BEFORE the moved point

            TwoDPoint secondBefore = toTwoDPoint(turnPoints[indexOfMovedPoint - 2]);
            TwoDSegment firstAfterEdge = new TwoDSegment(neww, newFirstAfter);

            if (indexOfMovedPoint == 2) { // if only one point between moved and start

                // check if only the previous point must be eliminated
                result = this.checkIfPreviousMustGo(turnPoints, indexOfMovedPoint, secondBefore, newFirstBefore, neww, newFirstAfter, firstAfterEdge);

            } else {

                // check if the previous 2 points must be eliminated
                result = this.checkIfPreviousTwoMustGo(turnPoints, indexOfMovedPoint, secondBefore, oldFirstBefore, newFirstBefore, old, neww, newFirstAfter,
                        firstAfterEdge);
            }

        }

        return result;
    }

    /**
     * This member computes the angle between two vectors with the same origin
     * 
     * @param head1
     *            - the head of the first vector
     * @param origin
     *            - the common origin of the two vectors
     * @param head2
     *            - the head of the second vector
     * @returns the angle between these two vectors in degrees.
     */
    private double getAngleDegreesBetween(LatLng head1, LatLng origin, LatLng head2) {

        TwoDVector first = new TwoDVector(this.toTwoDPoint(origin), this.toTwoDPoint(head1));
        TwoDVector second = new TwoDVector(this.toTwoDPoint(origin), this.toTwoDPoint(head2));

        double dotProduct = first.dotProduct(second);

        double length1 = first.getNorm();
        double length2 = second.getNorm();

        double denominator = length1 * length2;

        double product = denominator != 0.0 ? dotProduct / denominator : 0.0;

        double angle = Math.toDegrees(Math.acos(product));
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    /**
     * Eliminates the spikes from the array of turns. In order to do so, it will compare the "inside" angle defined by
     * each pair of 3 consecutive turns with a default value. If this angle is smaller, than the top of it will be
     * removed.
     * 
     * @param turnPoints
     *            - the array of turn points.
     * @returns a LatLng object - the new array of turn points.
     */
    private LatLng[] eliminateSpikes(LatLng[] turnPoints) {

        if (turnPoints.length < 4) {
            return turnPoints;
        }

        List<LatLng> points = new ArrayList<LatLng>();

        TwoDSegment beforeEdge = null;
        TwoDSegment afterEdge = null;

        int newIndex = -1;
        TwoDPoint newAtIndex = null;

        for (int index = 2; index < turnPoints.length - 1; index++) {

            if (this.getAngleDegreesBetween(turnPoints[index - 1], turnPoints[index], turnPoints[index + 1]) < SMOOTHNESS_MAX_DEG) {

                beforeEdge = new TwoDSegment(toTwoDPoint(turnPoints[index - 2]), toTwoDPoint(turnPoints[index - 1]));
                afterEdge = new TwoDSegment(toTwoDPoint(turnPoints[index]), toTwoDPoint(turnPoints[index + 1]));

                newAtIndex = afterEdge.getIntersection(beforeEdge);

                newIndex = index;
            }
        }

        for (int index = 0; index < turnPoints.length; index++) {
            if (index == newIndex - 1) {
                continue;
            } else if (index == newIndex) {
                points.add(toLatLng(newAtIndex));
            } else {
                points.add(turnPoints[index]);
            }
        }

        return points.toArray(new LatLng[0]);
    }

    /**
     * Eliminates the triangles that might appear on the course.
     * 
     * @param turnPoints
     *            - the array of turn points.
     * @returns a LatLng object - the new array of turn points.
     */
    private LatLng[] eliminateTriangles(LatLng[] turnPoints) {

        int noOfPoints = turnPoints.length;

        if (noOfPoints < 4) {
            return turnPoints;
        }

        LatLng first = null;
        LatLng second = null;
        LatLng third = null;
        LatLng fourth = null;

        for (int index = 0; (index + 3) < noOfPoints; index++) {
            first = turnPoints[index];
            second = turnPoints[index + 1];
            third = turnPoints[index + 2];
            fourth = turnPoints[index + 3];

            if (TwoDPoint.areIntersecting(toTwoDPoint(first), toTwoDPoint(second), toTwoDPoint(third), toTwoDPoint(fourth))) {

                TwoDSegment firstSegment = new TwoDSegment(toTwoDPoint(first), toTwoDPoint(second));
                TwoDSegment secondSegment = new TwoDSegment(toTwoDPoint(third), toTwoDPoint(fourth));
                List<LatLng> newTurnPoints = new ArrayList<LatLng>();

                for (int index2 = 0; index2 < noOfPoints; index2++) {
                    if (index2 == index + 1) {
                        continue;
                    } else if (index2 == index + 2) {
                        newTurnPoints.add(toLatLng(firstSegment.getIntersection(secondSegment)));
                    } else {
                        newTurnPoints.add(turnPoints[index2]);
                    }
                }

                return newTurnPoints.toArray(new LatLng[0]);
            }
        }

        return turnPoints;
    }


    private Position intersectCurved(Position pos1, Bearing bear1, Position pos2, Bearing bear2) {

        double lat1 = pos1.getLatRad();
        double lon1 = pos1.getLngRad();

        double lat2 = pos2.getLatRad();
        double lon2 = pos2.getLngRad();

        //System.out.println("p1: "+(lat1*180/Math.PI)+", "+(lon1*180/Math.PI));
        //System.out.println("p2: "+(lat2*180/Math.PI)+", "+(lon2*180/Math.PI));

        Double lat3, lon3, ang3;

        double crs13 = bear1.getRadians();
        double crs23 = bear2.getRadians();

        double dst12=2.0*Math.asin(Math.sqrt(Math.pow(Math.sin((lat1-lat2)/2.0), 2.0)+Math.cos(lat1)*Math.cos(lat2)*Math.pow(Math.sin((lon1-lon2)/2.0), 2.0)));

        double crs12, crs21;
        if (Math.sin(lon2-lon1) > 0.0) {
            crs12 = Math.acos((Math.sin(lat2)-Math.sin(lat1)*Math.cos(dst12))/(Math.sin(dst12)*Math.cos(lat1)));
            crs21 = 2.0*Math.PI-Math.acos((Math.sin(lat1)-Math.sin(lat2)*Math.cos(dst12))/(Math.sin(dst12)*Math.cos(lat2)));
        } else {
            crs12 = 2.0*Math.PI-Math.acos((Math.sin(lat2)-Math.sin(lat1)*Math.cos(dst12))/(Math.sin(dst12)*Math.cos(lat1)));
            crs21 = Math.acos((Math.sin(lat1)-Math.sin(lat2)*Math.cos(dst12))/(Math.sin(dst12)*Math.cos(lat2)));
        }

        double ang1 = ((crs13-crs12+Math.PI)%(2.0*Math.PI))-Math.PI;
        double ang2 = ((crs21-crs23+Math.PI)%(2.0*Math.PI))-Math.PI;

        if ((Math.sin(ang1) == 0.0)&&(Math.sin(ang2) == 0.0)) {
            //"infinity of intersections"
            lat3 = null;
            lon3 = null;
        } else {
            ang3=Math.acos(-Math.cos(ang1)*Math.cos(ang2)+Math.sin(ang1)*Math.sin(ang2)*Math.cos(dst12));
            double dst13 = Math.atan2(Math.sin(dst12)*Math.sin(ang1)*Math.sin(ang2),Math.cos(ang2)+Math.cos(ang1)*Math.cos(ang3));
            lat3=Math.asin(Math.sin(lat1)*Math.cos(dst13)+Math.cos(lat1)*Math.sin(dst13)*Math.cos(crs13));
            double dlon=Math.atan2(Math.sin(crs13)*Math.sin(dst13)*Math.cos(lat1),Math.cos(dst13)-Math.sin(lat1)*Math.sin(lat3));
            lon3=((lon1+dlon+Math.PI)%(2.0*Math.PI))-Math.PI;
        }

        //System.out.println("p3: "+(lat3*180/Math.PI)+", "+(lon3*180/Math.PI));

        if (Math.abs(lon3-(lon1+lon2)/2.0) > Math.PI/2.0) {
            lat3 = -lat3;
            lon3 = Math.PI + lon3;
        }

        //System.out.println("p4: "+(lat3*180/Math.PI)+", "+(lon3*180/Math.PI)+"\n");

        Position intersectPoint = new RadianPosition(lat3, lon3);

        return intersectPoint;
    }





    /**
     * This member computes the correct new position of the moved point. In order to do so, it will take into
     * consideration the "before edge" - the edge defined by the old position of the moved point and the previous point,
     * and the "after edge" - the edge defined by the old position of the moved point and the next point. After this, it
     * will compute the projections of the proposed new position of the moved point on both of these edges, and it will
     * chose the one closer to it.
     * 
     * @param indexOfMovedPoint
     *            - the index of the moved turn point.
     * @returns a LatLng object - the correct new position of the moved point.
     */
    @SuppressWarnings("unused")
    private LatLng getNewPositionOfMovedPoint(int indexOfMovedPoint) {

        TwoDPoint firstBefore = toTwoDPoint(turnPoints[indexOfMovedPoint - 1]);
        TwoDPoint oldPositionMovedPoint = toTwoDPoint(turnPoints[indexOfMovedPoint]);
        TwoDPoint firstAfter = toTwoDPoint(turnPoints[indexOfMovedPoint + 1]);

        TwoDPoint newPositionMovedPointBeforeFix = toTwoDPoint(polyline.getPath().get(indexOfMovedPoint));

        TwoDSegment beforeEdge = new TwoDSegment(oldPositionMovedPoint, firstBefore);
        double distanceToBeforeEdge = newPositionMovedPointBeforeFix.getDistanceTo(beforeEdge);

        TwoDSegment afterEdge = new TwoDSegment(oldPositionMovedPoint, firstAfter);
        double distanceToAfterEdge = newPositionMovedPointBeforeFix.getDistanceTo(afterEdge);

        LatLng projectionOnBeforeEdge = this.toLatLng(newPositionMovedPointBeforeFix.getProjection(beforeEdge));
        LatLng projectionOfAfterEdge = this.toLatLng(newPositionMovedPointBeforeFix.getProjection(afterEdge));

        if (indexOfMovedPoint == 1) {
            return projectionOnBeforeEdge;
        } else if (indexOfMovedPoint == this.turnPoints.length - 2) {
            return projectionOfAfterEdge;
        } else {
            return (distanceToBeforeEdge < distanceToAfterEdge) ? projectionOnBeforeEdge : projectionOfAfterEdge;
        }
    }

    private Pair getNewPositionOfMovedPointCurved(int indexOfMovedPoint) {

        Position firstBefore = toPosition(turnPoints[indexOfMovedPoint - 1]);
        Position oldPositionMovedPoint = toPosition(turnPoints[indexOfMovedPoint]);
        Position firstAfter = toPosition(turnPoints[indexOfMovedPoint + 1]);

        Position newPositionMovedPointBeforeFix = toPosition(polyline.getPath().get(indexOfMovedPoint));

        // bearings of edges before and after
        Bearing bearBefore = firstBefore.getBearingGreatCircle(oldPositionMovedPoint);
        Bearing bearAfter = firstAfter.getBearingGreatCircle(oldPositionMovedPoint);

        System.out.println("bearBefore: "+bearBefore.getDegrees());
        System.out.println("bearAfter : "+bearAfter.getDegrees());

        // projection positions on edges before and after
        Position projectionOnBeforeEdge = newPositionMovedPointBeforeFix.projectToLineThrough(firstBefore, bearBefore);
        Position projectionOnAfterEdge = newPositionMovedPointBeforeFix.projectToLineThrough(firstAfter, bearAfter);

        // distances between new position and edges, i.e. projection positions
        double distanceToBeforeEdge = newPositionMovedPointBeforeFix.getDistance(projectionOnBeforeEdge).getMeters();
        double distanceToAfterEdge = newPositionMovedPointBeforeFix.getDistance(projectionOnAfterEdge).getMeters();

        LatLng projLatLngOnBeforeEdge = LatLng.newInstance(projectionOnBeforeEdge.getLatDeg(), projectionOnBeforeEdge.getLngDeg());
        LatLng projLatLngOnAfterEdge = LatLng.newInstance(projectionOnAfterEdge.getLatDeg(), projectionOnAfterEdge.getLngDeg());

        if (indexOfMovedPoint == 1) {
            return new Pair(projLatLngOnBeforeEdge, false);
        } else if (indexOfMovedPoint == this.turnPoints.length - 2) {
            return new Pair(projLatLngOnAfterEdge, true);
        } else {
            return (distanceToBeforeEdge < distanceToAfterEdge) ? new Pair(projLatLngOnBeforeEdge, false) : new Pair(projLatLngOnAfterEdge, true);
        }
    }

    /**
     * This member computes the correct new position of the point after the moved one. In order to do so, it will
     * compute the projection by vector of the new position of the moved point on the line defined by the first after
     * point and the second after point, considering the vector starting from the old position of the moved point to the
     * first after point.
     * 
     * @param indexOfMovedPoint
     *            - the index of the moved point.
     * @param newPositionOfMovedPoint
     *            - the new position of the moved point
     * @returns a LatLng object - the correct new position of the point after the moved one.
     */
    @SuppressWarnings("unused")
    private LatLng getNewPositionOfPointAfterMoved(int indexOfMovedPoint, LatLng newPositionOfMovedPoint) {

        TwoDPoint oldPositionOfMovedPoint = toTwoDPoint(this.turnPoints[indexOfMovedPoint]);
        TwoDPoint firstAfter = toTwoDPoint(this.turnPoints[indexOfMovedPoint + 1]);
        TwoDPoint secondAfter = toTwoDPoint(this.turnPoints[indexOfMovedPoint + 2]);

        TwoDSegment firstAfterEdge = new TwoDSegment(firstAfter, secondAfter);
        TwoDVector afterVector = new TwoDVector(oldPositionOfMovedPoint, firstAfter);

        return this.toLatLng(this.toTwoDPoint(newPositionOfMovedPoint).getProjectionByVector(firstAfterEdge, afterVector));
    }

    private LatLng getNewPositionOfPointAfterMovedCurved(int indexOfMovedPoint, LatLng newLatLngOfMovedPoint) {

        Position newPositionOfMovedPoint = toPosition(newLatLngOfMovedPoint);

        Position oldPositionOfMovedPoint = toPosition(this.turnPoints[indexOfMovedPoint]);
        Position firstAfter = toPosition(this.turnPoints[indexOfMovedPoint + 1]);
        Position secondAfter = toPosition(this.turnPoints[indexOfMovedPoint + 2]);

        // bearings of fist and second after edges
        Bearing bearFirstAfter = oldPositionOfMovedPoint.getBearingGreatCircle(firstAfter);
        Bearing bearSecondAfter = secondAfter.getBearingGreatCircle(firstAfter);

        // intersection point
        Position intersectPoint = this.intersectCurved(newPositionOfMovedPoint, bearFirstAfter, secondAfter, bearSecondAfter);

        return LatLng.newInstance(intersectPoint.getLatDeg(), intersectPoint.getLngDeg());
    }


    /**
     * This member computes the correct new position of the point before the moved one. In order to do so, it will
     * compute the projection by vector of the new position of the moved point on the line defined by the first before
     * point and the second before point, considering the vector starting from the old position of the moved point to
     * the first before point.
     * 
     * @param indexOfMovedPoint
     *            - the index of the moved point.
     * @param newPositionOfMovedPoint
     *            - the new position of the moved point
     * @returns a LatLng object - the correct new position of the point before the moved one.
     */
    @SuppressWarnings("unused")
    private LatLng getNewPositionOfPointBeforeMoved(int indexOfMovedPoint, LatLng newPositionOfMovedPoint) {

        TwoDPoint firstBefore = toTwoDPoint(this.turnPoints[indexOfMovedPoint - 1]);
        TwoDPoint secondBefore = toTwoDPoint(this.turnPoints[indexOfMovedPoint - 2]);
        TwoDPoint oldPositionOfMovedPoint = toTwoDPoint(this.turnPoints[indexOfMovedPoint]);

        TwoDSegment firstBeforeEdge = new TwoDSegment(secondBefore, firstBefore);
        TwoDVector beforeVector = new TwoDVector(oldPositionOfMovedPoint, firstBefore);

        return this.toLatLng(this.toTwoDPoint(newPositionOfMovedPoint).getProjectionByVector(firstBeforeEdge, beforeVector));
    }

    private LatLng getNewPositionOfPointBeforeMovedCurved(int indexOfMovedPoint, LatLng newLatLngOfMovedPoint) {

        Position newPositionOfMovedPoint = toPosition(newLatLngOfMovedPoint);

        Position firstBefore = toPosition(this.turnPoints[indexOfMovedPoint - 1]);
        Position secondBefore = toPosition(this.turnPoints[indexOfMovedPoint - 2]);
        Position oldPositionOfMovedPoint = toPosition(this.turnPoints[indexOfMovedPoint]);

        // bearings of fist and second after edges
        Bearing bearSecondBefore = secondBefore.getBearingGreatCircle(firstBefore);
        Bearing bearFirstBefore = oldPositionOfMovedPoint.getBearingGreatCircle(firstBefore);

        System.out.println("bear2Before: "+bearSecondBefore.getDegrees());
        System.out.println("bear1Before: "+bearFirstBefore.getDegrees());

        // intersection point
        Position intersectPoint = this.intersectCurved(newPositionOfMovedPoint, bearFirstBefore, secondBefore, bearSecondBefore);
        System.out.println("interSect: "+intersectPoint.getLatDeg()+", "+intersectPoint.getLngDeg());

        return LatLng.newInstance(intersectPoint.getLatDeg(), intersectPoint.getLngDeg());
    }

    /**
     * Returns the index of the moved point by comparing each position of the "saved" array of turn points and the
     * polyline array of vertices.
     * 
     * @returns the index of the moved point.
     */
    private int getIndexOfMovedPoint() {
        int index = 0;
        for (; index < turnPoints.length; index++) {
            if (PathPolyline.equals(turnPoints[index], polyline.getPath().get(index), EPSILON) == false) {
                break;
            }
        }
        return index;
    }

    /**
     * Converts a LatLng object to a TwoD object. Considering that a LatLng represent a point on a sphere, and that the
     * TwoDPoint represent one on a plane, it will use the MapWidget approximation to a container pixel in order to do
     * so.
     * 
     * @param latLng
     *            - the LatLng object to be converted to a TwoDPoint
     * @returns a TwoDPoint object.
     */
    private TwoDPoint toTwoDPoint(LatLng latLng) {
        Point point = this.mapProjection.fromLatLngToContainerPixel(latLng);
        return new TwoDPoint(point.getX(), point.getY());
    }

    private Position toPosition(LatLng latLng) {
        Position pos = new DegreePosition(latLng.getLatitude(), latLng.getLongitude());
        return pos;
    }

    /**
     * Converts a TwoDPoint object to a LatLng object. Considering that a TwoDPoint represent a point on a plane and
     * that a LatLng object represents one on a sphere, it will use the MapWidget aproximation to a container pixel in
     * order to do so.
     * 
     * @param point
     *            - the TwoDPoint object to be converted to a LatLng
     * @returns a LatLng object.
     */
    private LatLng toLatLng(TwoDPoint point) {
        return this.mapProjection.fromContainerPixelToLatLng(Point.newInstance((int) point.getX(), (int) point.getY()));
    }

    /**
     * Converts a LatLng object to a PositionDTO.
     * 
     * @param position
     *            - the LatLng object used
     * @returns a PositionDTO object.
     */
    private static Position toPositionDTO(LatLng position) {
        return new DegreePosition(position.getLatitude(), position.getLongitude());
    }

    /**
     * Checks if two LatLng points are equal by comparing their latitude and longitude with a certain epsilon (0.0001);
     * 
     * @param first
     *            - the first LatLng object
     * @param second
     *            - the second LatLng object
     * @returns a boolean, true for equality, false otherwise.
     */
    private static boolean equals(LatLng first, LatLng second, double epsilon) {

        double latDiff = Math.abs(first.getLatitude() - second.getLatitude());
        double lngDiff = Math.abs(first.getLongitude() - second.getLongitude());

        return latDiff <= epsilon && lngDiff <= epsilon;
    }

    private void getTotalTime() {
        List<Position> turnPointsAsPositionDTO = new ArrayList<>();
        for (LatLng point : this.turnPoints) {
            turnPointsAsPositionDTO.add(toPositionDTO(point));
        }
        RequestTotalTimeDTO requestData = new RequestTotalTimeDTO(new SimulatorUISelectionDTO(this.selectedBoatClassIndex, this.selectedRaceIndex,
                this.selectedCompetitorIndex, this.selectedLegIndex), STEP_DURATION_MILLISECONDS, this.allPoints, turnPointsAsPositionDTO,
                USE_REAL_AVERAGE_WIND);
        this.simulatorService.getTotalTime(requestData, new AsyncCallback<ResponseTotalTimeDTO>() {
            @Override
            public void onFailure(Throwable error) {
                errorReporter.reportError(StringMessages.INSTANCE.errorLoadingTotalTime(error.getMessage()));
            }

            @Override
            public void onSuccess(ResponseTotalTimeDTO receiveData) {
                String notificationMessage = receiveData.notificationMessage;
                if (notificationMessage != "" && notificationMessage.length() != 0 && warningAlreadyShown == false) {
                    errorReporter.reportError(notificationMessage, true);
                    warningAlreadyShown = true;
                }
                if (getTotalTimeFactor) {
                    totalTimeFactor = receiveData.factorSim2GPS;
                }
                long totalTime = Math.round(receiveData.totalTimeSeconds / totalTimeFactor);
                simulatorMap.addLegendOverlayForPathPolyline(totalTime * 1000);
                simulatorMap.redrawLegendCanvasOverlay();
            }
        });
    }

    /**
     * Setter for the boatClassIndex property.
     */
    public void setBoatClassID(int boatClassIndex) {
        this.selectedBoatClassIndex = boatClassIndex;
    }

    /**
     * Converts knots to meters per second
     */
    public static double knotsToMetersPerSecond(double knots) {
        return knots * Mile.METERS_PER_SECOND_PER_KNOT;
    }

    private class Pair {
        public LatLng point;
        public Boolean flag;

        public Pair(LatLng point, boolean flag) {
            this.flag = flag;
            this.point = point;
        }
    }
}
