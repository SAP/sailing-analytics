package com.sap.sailing.gwt.ui.client.shared.charts;

import org.moxieapps.gwt.highcharts.client.Point;

public final class ChartPointRecalculator {
    
    private ChartPointRecalculator() { }
    
    public static Point stayClosestToPreviousPoint(Point previousPoint, Point newPoint) {
        double previousY = previousPoint.getY().doubleValue();
        double newY = newPoint.getY().doubleValue();
        double bestNewY = newY;
        boolean hasBeenMoved;
        do {
            hasBeenMoved = false;
            double deltaPreviousYToNewY = Math.abs(previousY - bestNewY);
            double deltaPreviousYToNewYUp = Math.abs(previousY - (bestNewY + 360));
            double deltaPreviousYToNewYDown = Math.abs(previousY - (bestNewY - 360));
            
            if (deltaPreviousYToNewYUp < deltaPreviousYToNewY ||
                deltaPreviousYToNewYDown < deltaPreviousYToNewY) {
                bestNewY = deltaPreviousYToNewYUp <= deltaPreviousYToNewYDown ?
                           bestNewY + 360 : bestNewY - 360;
                hasBeenMoved = true;
            }
        } while (hasBeenMoved);
        return new Point(newPoint.getX(), bestNewY);
    }
    
    public static Point stayClosestToPreviousPointWithDeltaLimit(Point previousPoint, Point newPoint, int limit) {
        double previousY = previousPoint.getY().doubleValue();
        double newY = newPoint.getY().doubleValue();

        // at max correct once
        double bestNewY = newY;
        double deltaPreviousYToNewY = Math.abs(previousY - bestNewY);
        double deltaPreviousYToNewYUp = Math.abs(previousY - (bestNewY + 360));
        double deltaPreviousYToNewYDown = Math.abs(previousY - (bestNewY - 360));

        Point result = null;
        if (deltaPreviousYToNewYUp < deltaPreviousYToNewY || deltaPreviousYToNewYDown < deltaPreviousYToNewY) {
            bestNewY = deltaPreviousYToNewYUp <= deltaPreviousYToNewYDown ? bestNewY + 360 : bestNewY - 360;
            //prevent moving to far away from 0-360 range, jump if necessary
            if (bestNewY > (360 + limit) || bestNewY < -limit) {
                result = newPoint;
            } else {
                result = new Point(newPoint.getX(), bestNewY);
            }
        } else {
            result = newPoint;
        }
        return result;
    }

    public static Point keepOverallDeltaMinimal(Double yMin, Double yMax, Point newPoint) {
        double y = newPoint.getY().doubleValue();

        if (yMax != null && yMin != null && (y < yMin || y > yMax)) {
            double deltaMin = Math.abs(yMin - y);
            double deltaMax = Math.abs(yMax - y);

            double yDown = y - 360;
            double deltaMinDown = Math.abs(yMin - yDown);

            double yUp = y + 360;
            double deltaMaxUp = Math.abs(yMax - yUp);

            if (isRecalculationNeeded(deltaMin, deltaMax, deltaMinDown, deltaMaxUp)) {
                y = deltaMaxUp <= deltaMinDown ? yUp : yDown;
                return new Point(newPoint.getX(), y);
            }
        }
        
        return newPoint;
    }

    private static boolean isRecalculationNeeded(double deltaMin, double deltaMax, double deltaMinDown, double deltaMaxUp) {
        return (deltaMinDown < deltaMin || deltaMinDown < deltaMax) || (deltaMaxUp < deltaMin || deltaMaxUp < deltaMax);
    }
}
