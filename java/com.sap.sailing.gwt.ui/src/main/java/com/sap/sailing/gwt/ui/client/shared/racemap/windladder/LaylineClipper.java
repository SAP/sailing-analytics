package com.sap.sailing.gwt.ui.client.shared.racemap.windladder;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.Point;
import com.google.gwt.maps.client.overlays.MapCanvasProjection;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sailing.gwt.ui.shared.WaypointDTO;
import com.sap.sse.common.Position;

public class LaylineClipper {
    protected final List<Position> markPositions = new ArrayList<>(1);
    protected PassingInstruction passingInstruction;
    protected LegType legType;
    /**
     * Maneuver Angle in reference to wind (i.e. half of the maneuver angle)
     */
    protected Double maneuverAngleRadians;

    public void update(WaypointDTO waypointDto, LegType legType, double maneuverAngleRadians) {
        markPositions.clear();
        if (waypointDto != null) {
            for (MarkDTO mark : waypointDto.controlPoint.getMarks()) {
                markPositions.add(mark.position);
            }
            passingInstruction = waypointDto.passingInstructions;
        }
        this.legType = legType;
        this.maneuverAngleRadians = maneuverAngleRadians / 2.0;
    }

    public void clippingPath(double windRotation, int width, int height, Context2d ctx, MapCanvasProjection projection) {
        if (!markPositions.isEmpty() && (legType == LegType.UPWIND || legType == LegType.DOWNWIND)) {
            final int size = markPositions.size();
            Point[] points = new Point[size];
            for (int i = 0; i < size; i++) {
                final Position pos = markPositions.get(i);
                // projection.fromLatLngToDivPixel returns position in reference to div; 0,0 being centered
                points[i] = projection.fromLatLngToDivPixel(LatLng.newInstance(pos.getLatDeg(), pos.getLngDeg()));
            }
            final int leftMarkIndex, rightMarkIndex;
            if (points.length == 1) {
                leftMarkIndex = 0;
                rightMarkIndex = 0;
            } else {
                if (points[0].getX() < points[1].getX()) {
                    leftMarkIndex = 0;
                    rightMarkIndex = 1;
                } else {
                    leftMarkIndex = 1;
                    rightMarkIndex = 0;
                }
            }
            ctx.translate(width / 2, height / 2);
            ctx.beginPath();
            final double length = 10000; //TODO Calculate correct value
            // Left layline to left mark //FIXME Add 2 more points to allow for maneuver angles >=90°
            final Point start;
            if (legType == LegType.UPWIND) {
                start = laylineEndPoint(points[leftMarkIndex], length, -maneuverAngleRadians - windRotation);
            } else {
                start = laylineEndPoint(points[leftMarkIndex], length, -maneuverAngleRadians - windRotation + Math.PI);
            }
            ctx.moveTo(start.getX(), start.getY());
            ctx.lineTo(points[leftMarkIndex].getX(), points[leftMarkIndex].getY());
            // Left mark to right mark (if applicable)
            if (leftMarkIndex != rightMarkIndex) {
                ctx.lineTo(points[rightMarkIndex].getX(), points[rightMarkIndex].getY());
            }
            // Right mark to right layline
            final Point end;
            if (legType == LegType.UPWIND) {
                end = laylineEndPoint(points[rightMarkIndex], length, maneuverAngleRadians - windRotation);
            } else {
                end = laylineEndPoint(points[rightMarkIndex], length, maneuverAngleRadians - windRotation + Math.PI);
            }
            ctx.lineTo(end.getX(), end.getY());
            ctx.closePath();
        }
    }

    protected Point laylineEndPoint(Point refPoint, double length, double radians) {
        // Clockwise rotation starting at up position
        final double x = length * Math.sin(radians);
        final double y = length * Math.cos(radians);
        return Point.newInstance(refPoint.getX() + x, refPoint.getY() + y);
    }
}
