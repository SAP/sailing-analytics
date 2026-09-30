package com.sap.sailing.gwt.ui.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sap.sailing.gwt.ui.shared.SimulatorUISelectionDTO;
import com.sap.sailing.gwt.ui.shared.SimulatorWindDTO;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.SimulatorUISelection;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.impl.SimulatorUISelectionImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.RadianPosition;

public class SimulatorServiceUtils {

    public static double EARTH_RADIUS_METERS = 6378137;
    public static double FACTOR_DEG2RAD = 0.0174532925;
    public static double FACTOR_RAD2DEG = 57.2957795;
    public static double FACTOR_KN2MPS = 0.514444;
    public static double FACTOR_MPS2KN = 1.94384;
    public static SpeedWithBearing DEFAULT_AVERAGE_WIND = new KnotSpeedWithBearingImpl(4.5, new DegreeBearingImpl(350));

    /**
     * Converts degress to radians
     */
    public static double degreesToRadians(double degrees) {
        return (degrees * FACTOR_DEG2RAD);
    }

    /**
     * Converts radians to degrees
     */
    public static double radiansToDegrees(double radians) {
        return (radians * FACTOR_RAD2DEG);
    }

    /**
     * Converts knots to meters per second
     */
    public static double knotsToMetersPerSecond(double knots) {
        return knots * FACTOR_KN2MPS;
    }

    /**
     * Converts meters per second to knots
     */
    public static double metersPerSecondToKnots(double metersPerSecond) {
        return metersPerSecond * FACTOR_MPS2KN;
    }

    /**
     * Computes the average value from the given list of SpeedWithBearing objects.
     */
    public static SpeedWithBearing getAverage(SimulatorWindDTO windDTO1, SimulatorWindDTO windDTO2) {
        List<SimulatorWindDTO> windDTOs = new ArrayList<SimulatorWindDTO>();
        windDTOs.add(windDTO1);
        windDTOs.add(windDTO2);
        return SimulatorServiceUtils.getAverage(windDTOs);
    }

    /**
     * Computes the average value from the given list of SpeedWithBearing objects.
     */
    public static SpeedWithBearing getAverage(List<SimulatorWindDTO> windDTOs) {

        double sumOfProductOfSpeedAndCosBearing = 0.0;
        double sumOfProductOfSpeedAndSinBearing = 0.0;
        double windBearingRadians = 0.0;

        for (SimulatorWindDTO windDTO : windDTOs) {
            windBearingRadians = degreesToRadians(windDTO.trueWindBearingDeg);
            sumOfProductOfSpeedAndSinBearing += (windDTO.trueWindSpeedInKnots * Math.sin(windBearingRadians));
            sumOfProductOfSpeedAndCosBearing += (windDTO.trueWindSpeedInKnots * Math.cos(windBearingRadians));
        }
        int count = windDTOs.size();
        double a = sumOfProductOfSpeedAndSinBearing / count;
        double b = sumOfProductOfSpeedAndCosBearing / count;
        double c = radiansToDegrees(Math.atan(a / b));

        double averageBearingDegrees = 0.0;

        if (a > 0 && b >= 0) {
            averageBearingDegrees = c;
        } else if (a < 0 && b >= 0) {
            averageBearingDegrees = 360 + c;
        } else if (a < 0 && b < 0) {
            averageBearingDegrees = 180 + c;
        } else if (a > 0 && b < 0) {
            averageBearingDegrees = 180 - c;
        }

        double averageSpeedKnots = Math.sqrt(a * a + b * b);

        return new KnotSpeedWithBearingImpl(averageSpeedKnots, new DegreeBearingImpl(averageBearingDegrees));
    }

    /**
     * Gets an array of points from the start to the end with a certain step.
     */
    public static List<Position> getIntermediatePoints(Position startPoint, Position endPoint, double stepSizeMeters) {
        List<Position> result = new ArrayList<Position>();

        // double distance = getDistanceBetween(startPoint, endPoint);
        double distance = startPoint.getDistance(endPoint).getMeters();

        int noOfSteps = (int) (distance / stepSizeMeters) + 1;
        double bearing = getInitialBearing(startPoint, endPoint);

        Position temp = null;

        result.add(startPoint);
        for (int stepIndex = 1; stepIndex < noOfSteps; stepIndex++) {
            temp = getDestinationPoint(startPoint, bearing, stepSizeMeters * stepIndex);
            result.add(temp);
            bearing = getInitialBearing(startPoint, temp);
        }

        return result;
    }

    public static List<Position> getIntermediatePoints2(List<Position> points, double stepSizeMeters) {
        List<Position> newPoints = new ArrayList<>();
        for (Position point : points) {
            newPoints.add(point);
        }
        return SimulatorServiceUtils.getIntermediatePoints(newPoints, stepSizeMeters);
    }

    /**
     * For every segment of two points in the given array, it computes the intermediate points given the certain step
     * size.
     */
    public static List<Position> getIntermediatePoints(List<Position> points, double stepSizeMeters) {

        int noOfPoints = points.size();
        int noOfPointsMinus1 = noOfPoints - 1;
        if (noOfPoints == 0) {
            return new ArrayList<Position>();
        } else if (noOfPoints == 1) {
            return points;
        } else if (noOfPoints == 2) {

            Position startPoint = points.get(0);
            Position endPoint = points.get(1);

            // double distance = getDistanceBetween(startPoint, endPoint);
            double distance = startPoint.getDistance(endPoint).getMeters();
            if (distance <= stepSizeMeters) {
                return points;
            } else {
                List<Position> newPoints = getIntermediatePoints(startPoint, endPoint, stepSizeMeters);
                newPoints.add(endPoint);
                return newPoints;
            }
        }

        List<Position> result = new ArrayList<Position>();

        for (int index = 0; index < noOfPointsMinus1; index++) {
            result.addAll(getIntermediatePoints(points.get(index), points.get(index + 1), stepSizeMeters));
        }

        result.add(points.get(noOfPoints - 1));

        return result;

    }

    /**
     * Computes the time required to travel from the start point to the end point with the specified speed.
     */
    public static double getTimeSeconds(Position startPoint, Position endPoint, double endSpeedMetersPerSecond) {

        // double distance = getDistanceBetween(startPoint, endPoint);
        double distance = startPoint.getDistance(endPoint).getMeters();

        if (distance == 0.0) {
            return 0.0;
        }

        return distance / endSpeedMetersPerSecond;
    }

    /**
     * Returns the total distance between the path composed of the given points
     */
    public static double getTotalDistanceMeters(Position[] points) {

        int noOfPositions = points.length;

        if (noOfPositions == 0 || noOfPositions == 1) {
            return 0.0;
        }

        double result = 0.0;

        for (int index = 0; index < noOfPositions; index++) {

            if (index == noOfPositions - 1) {
                break;
            }

            result += points[index].getDistance(points[index + 1]).getMeters();
            // result += getDistanceBetween(points[index], points[index + 1]);
        }

        return result;
    }

    // /**
    // * Returns the distance from this point to the supplied point, in meters (using Haversine formula).
    // */
    // public static double getDistanceBetween(Position startPoint, Position endPoint) {
    //
    // double lat1 = startPoint.getLatRad();
    // double lon1 = startPoint.getLngRad();
    //
    // double lat2 = endPoint.getLatRad();
    // double lon2 = endPoint.getLngRad();
    //
    // double dLat = lat2 - lat1;
    // double dLon = lon2 - lon1;
    //
    // double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) *
    // Math.sin(dLon / 2);
    // double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    // double d = EARTH_RADIUS_METERS * c;
    //
    // return d;
    // }

    /**
     * Returns the initial bearing from the start point to the end point in degrees
     */
    public static double getInitialBearing(Position startPoint, Position endPoint) {

        double lat1 = startPoint.getLatRad();
        double lat2 = endPoint.getLatRad();

        double dLon = endPoint.getLngRad() - startPoint.getLngRad();

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double bearing = Math.atan2(y, x);

        return (radiansToDegrees(bearing) + 360) % 360;
    }

    /**
     * Returns bearing arriving at the end point from the start point; The bearing will differ from the initial bearing
     * by varying degrees according to distance and latitude
     */
    public static double getFinalBearing(Position startPoint, Position endPoint) {

        double lat1 = startPoint.getLatRad();
        double lat2 = endPoint.getLatRad();

        double dLon = endPoint.getLngRad() - startPoint.getLngRad();

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double bearing = Math.atan2(y, x);

        return (radiansToDegrees(bearing) + 180) % 360;
    }

    /**
     * Returns the midpoint between this point and the supplied point.
     */
    public static Position getMidpointBetween(Position startPoint, Position endPoint) {

        double lat1 = startPoint.getLatRad();
        double lon1 = startPoint.getLngRad();
        double lat2 = endPoint.getLatRad();

        double dLon = endPoint.getLngRad() - lon1;

        double Bx = Math.cos(lat2) * Math.cos(dLon);
        double By = Math.cos(lat2) * Math.sin(dLon);

        double lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
        double lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);
        lon3 = (lon3 + 3 * Math.PI) % (2 * Math.PI) - Math.PI;

        return new RadianPosition(lat3, lon3);
    }

    /**
     * Returns the destination point from this point having travelled the given distance, in meters on the given initial
     * bearing (bearing may vary before destination is reached)
     */
    public static Position getDestinationPoint(Position startPoint, double bearingDegrees, double distanceMeters) {

        double distance = distanceMeters / EARTH_RADIUS_METERS;
        double bearing = degreesToRadians(bearingDegrees);
        double lat1 = startPoint.getLatRad();
        double lon1 = startPoint.getLngRad();

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distance) + Math.cos(lat1) * Math.sin(distance) * Math.cos(bearing));
        double lon2 = lon1 + Math.atan2(Math.sin(bearing) * Math.sin(distance) * Math.cos(lat1), Math.cos(distance) - Math.sin(lat1) * Math.sin(lat2));
        lon2 = (lon2 + 3 * Math.PI) % (2 * Math.PI) - Math.PI;

        return new RadianPosition(lat2, lon2);
    }

    public static double getSign(Position p1, Position p2, Position p3) {
        return (p1.getLatDeg() - p3.getLatDeg()) * (p2.getLngDeg() - p3.getLngDeg()) - (p2.getLatDeg() - p3.getLatDeg()) * (p1.getLngDeg() - p3.getLngDeg());
    }

    public static boolean isPointInsideTriangle(Position point, Position corner1, Position corner2, Position corner3) {

        boolean b1, b2, b3;

        b1 = getSign(point, corner1, corner2) < 0.0d;
        b2 = getSign(point, corner2, corner3) < 0.0d;
        b3 = getSign(point, corner3, corner1) < 0.0d;

        return (b1 == b2) && (b2 == b3);
    }

    public static boolean areTowardsSameDirection(Bearing b1, Bearing b2) {
        boolean isB1TowardsNorth = (b1.getDegrees() < 90.0 || b1.getDegrees() > 270.0);
        boolean isB2TowardsNorth = (b2.getDegrees() < 90.0 || b2.getDegrees() > 270.0);

        return !(isB1TowardsNorth ^ isB2TowardsNorth);
    }

    public static boolean equals(Position first, Position second, double delta) {
        double latDiff = Math.abs(first.getLatDeg() - second.getLatDeg());
        double lngDiff = Math.abs(first.getLngDeg() - second.getLngDeg());

        return latDiff <= delta && lngDiff <= delta;
    }

    public static int getIndexOfClosest(List<TimedPositionWithSpeed> items, TimedPositionWithSpeed item) {
        int count = items.size();

        List<Double> diff_lat = new ArrayList<Double>();
        List<Double> diff_lng = new ArrayList<Double>();
        List<Long> diff_timepoint = new ArrayList<Long>();

        for (int index = 0; index < count; index++) {
            diff_lat.add(Math.abs(items.get(index).getPosition().getLatDeg() - item.getPosition().getLatDeg()));
            diff_lng.add(Math.abs(items.get(index).getPosition().getLngDeg() - item.getPosition().getLngDeg()));
            diff_timepoint.add(Math.abs(items.get(index).getTimePoint().asMillis() - item.getTimePoint().asMillis()));
        }

        double min_diff_lat = Collections.min(diff_lat);
        double min_max_diff_lat = min_diff_lat + Collections.max(diff_lat);

        double min_diff_lng = Collections.min(diff_lng);
        double min_max_diff_lng = min_diff_lng + Collections.max(diff_lng);

        long min_diff_timepoint = Collections.min(diff_timepoint);
        double min_max_diff_timepoint = min_diff_timepoint + Collections.max(diff_timepoint);

        List<Double> norm_diff_lat = new ArrayList<Double>();
        List<Double> norm_diff_lng = new ArrayList<Double>();
        List<Double> norm_diff_timepoint = new ArrayList<Double>();

        for (int index = 0; index < count; index++) {
            norm_diff_lat.add((diff_lat.get(index) - min_diff_lat) / min_max_diff_lat);
            norm_diff_lng.add((diff_lng.get(index) - min_diff_lng) / min_max_diff_lng);
            norm_diff_timepoint.add((diff_timepoint.get(index) - min_diff_timepoint) / min_max_diff_timepoint);
        }

        List<Double> deltas = new ArrayList<Double>();

        for (int index = 0; index < count; index++) {
            deltas.add(Math.sqrt(Math.pow(norm_diff_lat.get(index), 2) + Math.pow(norm_diff_lng.get(index), 2) + Math.pow(norm_diff_timepoint.get(index), 2)));
        }

        int result = 0;
        double min = deltas.get(0);

        for (int index = 0; index < count; index++) {
            if (deltas.get(index) < min) {
                result = index;
                min = deltas.get(index);
            }
        }

        return result;
    }

    public static SpeedWithBearing getWindAtTimepoint(long timepointAsMillis, Path gpsTrack) {
        List<TimedPositionWithSpeed> pathPoints = gpsTrack.getPathPoints();
        int noOfPathPoints = pathPoints.size();
        List<Double> diffs = new ArrayList<Double>();
        for (int index = 0; index < noOfPathPoints; index++) {
            diffs.add(Double.valueOf(Math.abs(pathPoints.get(index).getTimePoint().asMillis() - timepointAsMillis)));
        }
        int indexOfMinDiff = diffs.indexOf(Collections.min(diffs));
        return pathPoints.get(indexOfMinDiff).getSpeed();
    }

    public static List<SimulatorWindDTO> toSimulatorWindDTOList(List<TimedPositionWithSpeed> points) {

        List<SimulatorWindDTO> result = new ArrayList<SimulatorWindDTO>();

        for (TimedPositionWithSpeed point : points) {
            result.add(toSimulatorWindDTO(point));
        }

        return result;
    }

    public static SimulatorWindDTO toSimulatorWindDTO(TimedPositionWithSpeed point) {
        Position position = point.getPosition();
        SpeedWithBearing windSpeedWithBearing = point.getSpeed();
        TimePoint timePoint = point.getTimePoint();
        double latDeg = position.getLatDeg();
        double lngDeg = position.getLngDeg();
        double windSpeedKn = windSpeedWithBearing.getKnots();
        double windBearingDeg = windSpeedWithBearing.getBearing().getDegrees();
        return new SimulatorWindDTO(latDeg, lngDeg, windSpeedKn, windBearingDeg, timePoint);
    }

    public static SimulatorUISelection toSimulatorUISelection(SimulatorUISelectionDTO selection) {
        return new SimulatorUISelectionImpl(selection.boatClassIndex, selection.raceIndex, selection.competitorIndex, selection.legIndex);
    }

}