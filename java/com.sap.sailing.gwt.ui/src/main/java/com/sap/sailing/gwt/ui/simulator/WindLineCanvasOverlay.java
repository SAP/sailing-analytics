package com.sap.sailing.gwt.ui.simulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.logging.Logger;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.shared.WindLinesDTO;
import com.sap.sailing.gwt.ui.simulator.racemap.FullCanvasOverlay;
import com.sap.sailing.gwt.ui.simulator.util.LineSegment;
import com.sap.sse.gwt.client.player.Timer;

public class WindLineCanvasOverlay extends FullCanvasOverlay implements TimeListenerWithStoppingCriteria {

    private WindLinesDTO windLinesDTO;

    private String lineColor = "Black";
    private final Timer timer;

    private Position[] corners;
    private LineSegment[] boundary;
    private Position center;
  
    private static Logger logger = Logger.getLogger(WindLineCanvasOverlay.class.getName());

    public WindLineCanvasOverlay(MapWidget map, int zIndex, final Timer timer, CoordinateSystem coordinateSystem) {
        super(map, zIndex, coordinateSystem);
        
        this.timer = timer;
        windLinesDTO = null;
        corners = null;
        boundary = null;
    }

    @Override
    public void timeChanged(final Date newTime, Date oldTime) {
        final Map<Position, SortedMap<Long, List<Position>>> windLinesMap = windLinesDTO.getWindLinesMap();

        if (windLinesMap == null) {
            return;
        }
         clear();
        
        final Context2d context2d = canvas.getContext2d();
        context2d.setGlobalAlpha(0.2);
        //context2d.setGlobalCompositeOperation(Composite.LIGHTER) ;
        
        int index = 0;
        for (final Entry<Position, SortedMap<Long, List<Position>>> entry : windLinesMap.entrySet()) {
            List<Position> positionToDraw = new ArrayList<>();
            final SortedMap<Long, List<Position>> headMap = (entry.getValue().headMap(newTime.getTime() + 1));
            if (!headMap.isEmpty()) {
                positionToDraw = headMap.get(headMap.lastKey());
            }
            logger.info("In WindLineCanvasOverlay.drawWindField drawing " + positionToDraw.size() + " points"
                    + " @ " + newTime);
            drawWindLine(positionToDraw, ++index);
        }
    }

    @Override
    public boolean shallStop() {
        final Map<Position, SortedMap<Long, List<Position>>> positionTimePointPositionDTOMap = windLinesDTO.getWindLinesMap();

        if (!this.isVisible() || positionTimePointPositionDTOMap == null || timer == null
                || positionTimePointPositionDTOMap.isEmpty()) {
            return true;
        }
        final Set<Position> positions = positionTimePointPositionDTOMap.keySet();
        if (positions != null && !positions.isEmpty()) {
            /**
             * Just check for one position as it would be the same for all the other positions
             */
            final SortedMap<Long, List<Position>> timePointPositionDTOMap = positionTimePointPositionDTOMap.get(positions
                    .iterator().next());
            if (timePointPositionDTOMap.lastKey() < timer.getTime().getTime()) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public WindLinesDTO getWindLinesDTO() {
        return windLinesDTO;
    }

    public void setWindLinesDTO(final WindLinesDTO windLinesDTO) {
        this.windLinesDTO = windLinesDTO;
    }

    @Override
    public void addToMap() {
        super.addToMap();

        if (timer != null) {
            timer.addTimeListener(this);
        }
    }

    @Override
    public void removeFromMap() {
        super.removeFromMap();
        
        if (timer != null) {
            timer.removeTimeListener(this);
        }
    }

    @Override
    protected void draw() {
        super.draw();
        if (getMapProjection() != null && windLinesDTO != null) {
            clear();
            drawWindLine();
        }
    }

    private void clear() {
        canvas.getContext2d().clearRect(0.0 /* canvas.getAbsoluteLeft() */, 0.0/* canvas.getAbsoluteTop() */,
                canvas.getCoordinateSpaceWidth(), canvas.getCoordinateSpaceHeight());
    }

    protected void drawWindLine() {
        if (timer != null) {
            timeChanged(timer.getTime(), null);
        }
    }

    protected void drawWindLine(final List<Position> positionList, final int index) {
        if (positionList == null) {
            return;
        }
        final int numPoints = positionList.size();
        if (numPoints < 1) {
            return;
        }
        final String title = "Wind line at " + numPoints + " points.";
        getCanvas().setTitle(title);
        final Iterator<Position> positionDTOIter = positionList.iterator();
        Position prevPosition = null;
        while (positionDTOIter.hasNext()) {
            final Position position = positionDTOIter.next();
            if (prevPosition != null) {
                if (checkPointInGrid(prevPosition)  && checkPointInGrid(position) ) {
                    drawLine(prevPosition, position);
                } else { 
                    final Position pointOnBoundary = getPointOnBoundary(prevPosition, position);
                    if (pointOnBoundary != null) {
                        drawLine(prevPosition, pointOnBoundary);
                    }
                }
            }
            prevPosition = position;
        }
    }

    private Position getPointOnBoundary(final Position p1, final Position p2) {
        if (boundary != null) {
            final LineSegment line = new LineSegment(p1.getLatDeg(), p1.getLngDeg(), p2.getLatDeg(), p2.getLngDeg());
            com.sap.sailing.gwt.ui.simulator.util.LineSegment.Point p = line.intersect(boundary[0]);
            if (p != null) {
                final Position pDTO = new DegreePosition(p.getX(), p.getY());
                return pDTO;
            }
            p = line.intersect(boundary[2]);
            if (p != null) {
                final Position pDTO = new DegreePosition(p.getX(), p.getY());
                return pDTO;
            }
            p = line.intersect(boundary[1]);
            if (p != null) {
                final Position pDTO = new DegreePosition(p.getX(), p.getY());
                return pDTO;
            }
            p = line.intersect(boundary[3]);
            if (p != null) {
                final Position pDTO = new DegreePosition(p.getX(), p.getY());
                return pDTO;
            }
        }
        return null;
    }

    private void drawLine(final Position p1, final Position p2) {
        final double weight = 1.0;
        LatLng positionLatLng = coordinateSystem.toLatLng(p1);
        Point canvasPositionInPx = getMapProjection().fromLatLngToDivPixel(positionLatLng);
        final double x1 = canvasPositionInPx.getX() - this.getWidgetPosLeft();
        final double y1 = canvasPositionInPx.getY() - this.getWidgetPosTop();
        positionLatLng = coordinateSystem.toLatLng(p2);
        canvasPositionInPx = getMapProjection().fromLatLngToDivPixel(positionLatLng);
        final double x2 = canvasPositionInPx.getX() - this.getWidgetPosLeft();
        final double y2 = canvasPositionInPx.getY() - this.getWidgetPosTop();
        drawLine(x1, y1, x2, y2, weight, lineColor);
    }

    public void setGridCorners(final Position[] gridCorners) {
        this.corners = gridCorners;
        if (corners != null && corners.length == 4) {
            this.boundary = new LineSegment[4];

            boundary[0] = new LineSegment(corners[0].getLatDeg(), corners[0].getLngDeg(), corners[1].getLatDeg(), corners[1].getLngDeg());
            boundary[1] = new LineSegment(corners[1].getLatDeg(), corners[1].getLngDeg(), corners[2].getLatDeg(), corners[2].getLngDeg());
            boundary[2] = new LineSegment(corners[2].getLatDeg(), corners[2].getLngDeg(), corners[3].getLatDeg(), corners[3].getLngDeg());
            boundary[3] = new LineSegment(corners[3].getLatDeg(), corners[3].getLngDeg(), corners[0].getLatDeg(), corners[0].getLngDeg());
            
            center = getCenter();
        }
    }

    private Point getPointInDivPixel(final Position p) {
        LatLng pLatLng = LatLng.newInstance(p.getLatDeg(), p.getLngDeg());
        Point canvasPositionInPx = getMapProjection().fromLatLngToDivPixel(pLatLng);
        return Point.newInstance(canvasPositionInPx.getX(), canvasPositionInPx.getY());
    }
    
    @Override
    public void setCanvasSettings() {
        if (corners != null && corners.length == 4) {
            final Point corner0 = getPointInDivPixel(corners[0]);
            final Point corner1 = getPointInDivPixel(corners[1]);
            final Point corner3 = getPointInDivPixel(corners[3]);
            final int canvasWidth = (int) Math.sqrt(Math.pow(corner0.getX() - corner1.getX(), 2)
                    + Math.pow(corner0.getY() - corner1.getY(), 2));
            final int canvasHeight = (int) Math.sqrt(Math.pow(corner3.getX() - corner0.getX(), 2)
                    + Math.pow(corner3.getY() - corner0.getY(), 2));
            final int canvasRadius = (int) Math.sqrt(canvasWidth * canvasWidth / 4 + canvasHeight * canvasHeight / 4);
            canvas.setSize("" + 2 * canvasRadius + "px", "" + 2 * canvasRadius + "px");
            canvas.setCoordinateSpaceWidth(2 * canvasRadius); canvas.setCoordinateSpaceHeight(2 * canvasRadius);
            final Point anchorPoint = getAnchorPoint();
            setWidgetPosLeft(anchorPoint.getX());
            setWidgetPosTop(anchorPoint.getY());
            setCanvasPosition(anchorPoint.getX(), anchorPoint.getY());
        }
    }

    private Point getAnchorPoint() {
        if (corners != null) {
            final List<Double> xlist = new LinkedList<Double>();
            final List<Double> ylist = new LinkedList<Double>();
            for (int i = 0; i < corners.length; ++i) {
                final Point corner = getPointInDivPixel(corners[i]);
                xlist.add(corner.getX());
                ylist.add(corner.getY());
            }
            return Point.newInstance(Collections.min(xlist), Collections.min(ylist));
        }
        return null;

    }

    private Position getCenter() {
        final Position center;
        if (corners != null && corners.length == 4) {
            center = new DegreePosition(
                    (corners[0].getLatDeg() + corners[1].getLatDeg() + corners[2].getLatDeg() + corners[3].getLatDeg()) / 4.0,
                    (corners[0].getLngDeg() + corners[1].getLngDeg() + corners[2].getLngDeg() + corners[3].getLngDeg()) / 4.0);
        } else {
            center = null;
        }
        return center;
    }

    private boolean checkPointInGrid(final Position point) {
        if (center != null ) {
            final Position pointOnBoundary = getPointOnBoundary(center,point);
            if (pointOnBoundary == null) { // Line through center and the point does not intersect
                return true;
            } else {
                if (pointOnBoundary.equals(point)) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }

}
