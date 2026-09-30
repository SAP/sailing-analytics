package com.sap.sailing.simulator.impl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.windfield.WindField;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PathImpl implements Path, Serializable {

    private static final long serialVersionUID = -6354445155884413937L;
    private List<TimedPositionWithSpeed> pathPoints;
    private WindField windField;
    private int turnCount;
    private long maxTurnTime;
    private boolean algorithmTimedOut;
    private boolean mixedLeg = false;

    private static final double TRESHOLD_DEGREES = 25.0;
    private static final double THRESHOLD_DISTANCE_METERS = 15.0;

    public PathImpl(List<TimedPositionWithSpeed> pointsList, WindField wf, boolean algorithmTimedOut) {
        this.pathPoints = pointsList;
        this.windField = wf;
        this.turnCount = 0;
        this.maxTurnTime = 0;
        this.algorithmTimedOut = algorithmTimedOut;
    }

    public PathImpl(List<TimedPositionWithSpeed> pointsList, WindField wf, boolean algorithmTimedOut, boolean mixedLeg) {
        this.pathPoints = pointsList;
        this.windField = wf;
        this.turnCount = 0;
        this.maxTurnTime = 0;
        this.algorithmTimedOut = algorithmTimedOut;
        this.mixedLeg = mixedLeg;
    }

    public PathImpl(List<TimedPositionWithSpeed> pointsList, WindField wf, long maxTurnTime, boolean algorithmTimedOut, boolean mixedLeg) {
        this.pathPoints = pointsList;
        this.windField = wf;
        this.turnCount = 0;
        this.maxTurnTime = maxTurnTime;
        this.algorithmTimedOut = algorithmTimedOut;
        this.mixedLeg = mixedLeg;
    }

    public PathImpl(List<TimedPositionWithSpeed> pointsList, WindField wf, int turnCount, boolean algorithmTimedOut) {
        this.pathPoints = pointsList;
        this.windField = wf;
        this.turnCount = turnCount;
        this.maxTurnTime = 0;
        this.algorithmTimedOut = algorithmTimedOut;
    }

    @Override
    public boolean getAlgorithmTimedOut() {
        return this.algorithmTimedOut;
    }

    @Override
    public boolean getMixedLeg() {
        return this.mixedLeg;
    }

    @Override
    public List<TimedPositionWithSpeed> getPathPoints() {
        return this.pathPoints;
    }
    
    @Override
    public int getTurnCount() {
        return this.turnCount;
    }

    @Override
    public void setPathPoints(List<TimedPositionWithSpeed> pointsList) {
        this.pathPoints = pointsList;
    }

    @Override
    public long getMaxTurnTime() {
        return this.maxTurnTime;
    }
    
    @Override
    public void setMaxTurnTime(long maxTurnTime) {
        this.maxTurnTime = maxTurnTime;
    }
    
    @Override
    public Path getEvenTimedPath(long timestep) {
        return new PathImpl(this.getEvenTimedPathAsList(timestep), this.windField, this.algorithmTimedOut, this.mixedLeg);
    }

    private List<TimedPositionWithSpeed> getEvenTimedPathAsList(long timeStep) {

        TimePoint startTime = this.pathPoints.get(0).getTimePoint();
        TimePoint endTime = this.pathPoints.get(this.pathPoints.size() - 1).getTimePoint();

        List<TimedPositionWithSpeed> path = new ArrayList<TimedPositionWithSpeed>();

        TimedPositionWithSpeed startPoint = this.pathPoints.get(0);
        SpeedWithBearing startWind = null;
        if (this.windField != null) {
            startWind = this.windField.getWind(new TimedPositionImpl(startPoint.getTimePoint(), startPoint
                    .getPosition()));
            startPoint = new TimedPositionWithSpeedImpl(startPoint.getTimePoint(), startPoint.getPosition(), startWind);
        }
        path.add(startPoint);
        TimePoint nextTimePoint = new MillisecondsTimePoint(startTime.asMillis() + timeStep);
        List<TimedPositionWithSpeed> points = new ArrayList<TimedPositionWithSpeed>();

        int idx = 1;
        while (idx < this.pathPoints.size()) {

            if ((this.pathPoints.get(idx).getTimePoint().asMillis() >= nextTimePoint.asMillis())||(idx == this.pathPoints.size()-1)) {

                // for the last step, next-time-point needs to be aligned to the time-range of the course
                if (nextTimePoint.after(endTime)) {
                	nextTimePoint = endTime;
                }
                
                // reached point after next timestep
                TimedPositionWithSpeed p1 = this.pathPoints.get(idx - 1);
                TimedPositionWithSpeed p2 = this.pathPoints.get(idx);
                Distance dist = p1.getPosition().getDistance(p2.getPosition());
                double scale1 = nextTimePoint.asMillis() - p1.getTimePoint().asMillis();
                double scale2 = p2.getTimePoint().asMillis() - p1.getTimePoint().asMillis();
                Position nextPosition = p1.getPosition().translateGreatCircle(
                        p1.getPosition().getBearingGreatCircle(p2.getPosition()), dist.scale(scale1 / scale2));
                SpeedWithBearing nextWind = null;
                if (this.windField != null) {
                    nextWind = this.windField.getWind(new TimedPositionImpl(nextTimePoint, nextPosition));
                } else {
                    double nextWindSpeed = p1.getSpeed().getKnots()
                            + (p2.getSpeed().getKnots() - p1.getSpeed().getKnots()) * scale1 / scale2;
                    double nextWindAngle = p1.getSpeed().getBearing().getDegrees()
                            + (p2.getSpeed().getBearing().getDegrees() - p1.getSpeed().getBearing().getDegrees())
                            * scale1 / scale2;
                    SpeedWithBearing nextWindSpeedWithBearing = new KnotSpeedWithBearingImpl(nextWindSpeed,
                            new DegreeBearingImpl(nextWindAngle));
                    nextWind = new WindImpl(nextPosition, nextTimePoint, nextWindSpeedWithBearing);
                }
                TimedPositionWithSpeed nextPoint = new TimedPositionWithSpeedImpl(nextTimePoint, nextPosition,
                        nextWind);

                // distance scale: percentage of distance between previous point and next point
                TimedPositionWithSpeed prevPoint = path.get(path.size() - 1);
                double scaleDist = 0.01 * nextPoint.getPosition().getDistance(prevPoint.getPosition())
                        .getMeters();

                // evaluate collected points to potentially find turn/corner (up to two)
                double prevSide = 0;
                int maxCnt = 0;
                ArrayList<TimedPositionWithSpeed> maxPoint = new ArrayList<TimedPositionWithSpeed>();
                maxPoint.add(path.get(0));
                ArrayList<Double> maxDist = new ArrayList<Double>();
                maxDist.add(Double.valueOf(0));
                Bearing nextBear = prevPoint.getPosition().getBearingGreatCircle(nextPoint.getPosition());
                for (int jdx = 0; jdx < points.size(); jdx++) {

                    Position pcur = points.get(jdx).getPosition();
                    Position ptmp = pcur.projectToLineThrough(prevPoint.getPosition(), nextBear);
                    double side = Math.signum(nextBear.getDifferenceTo(prevPoint.getPosition().getBearingGreatCircle(pcur)).getDegrees());
                    boolean sideChange = (prevSide != 0)&&(side != prevSide);
                    double lineDist = Math.round(ptmp.getDistance(pcur).getMeters()*1000.0)/1000.0;
                    if (sideChange) {
                        maxCnt++;
                        maxDist.add(Double.valueOf(0));
                        maxPoint.add(path.get(0));
                    }
                    if (lineDist > maxDist.get(maxCnt)) {
                        maxPoint.set(maxCnt, points.get(jdx));
                        maxDist.set(maxCnt, lineDist);
                    }
                    prevSide = side;
                }
                for(int cnt=0; cnt<=maxCnt; cnt++) {
                    if (maxDist.get(cnt) > scaleDist) {
                        // add intermediate corner point
                        SpeedWithBearing maxWind = null;
                        if (this.windField != null) {
                            maxWind = this.windField.getWind(new TimedPositionImpl(maxPoint.get(cnt).getTimePoint(), maxPoint.get(cnt).getPosition()));
                            maxPoint.set(cnt, new TimedPositionWithSpeedImpl(maxPoint.get(cnt).getTimePoint(), maxPoint.get(cnt).getPosition(), maxWind));
                        }
                        path.add(maxPoint.get(cnt));
                    }
                }

                // add next even timed point
                path.add(nextPoint);

                // clear list of intermediate points
                points = new ArrayList<TimedPositionWithSpeed>();

                // increase nextTimePoint by timeStep
                nextTimePoint = nextTimePoint.plus(timeStep);
            }

            // collect points between previous timestep and next timestep
            points.add(this.pathPoints.get(idx));

            if (this.pathPoints.get(idx).getTimePoint().asMillis() < nextTimePoint.asMillis()) {
                idx++;
            }

        }

        if (path.get(path.size() - 1).getTimePoint().asMillis() < endTime.asMillis()) {
            TimedPositionWithSpeed endPoint = this.pathPoints.get(this.pathPoints.size() - 1);
            SpeedWithBearing endWind = null;
            if (this.windField != null) {
                endWind = this.windField
                        .getWind(new TimedPositionImpl(endPoint.getTimePoint(), endPoint.getPosition()));
                endPoint = new TimedPositionWithSpeedImpl(endPoint.getTimePoint(), endPoint.getPosition(), endWind);
            }
            path.add(endPoint);
        }

        return path;
    }

    @Override
    public void setWindField(WindField wf) {

        this.windField = wf;

    }

    // @Override
    public TimedPositionWithSpeed getPositionAtTime(TimePoint t) {

        if (t.compareTo(this.pathPoints.get(0).getTimePoint()) == 0) {
            return this.pathPoints.get(0);
        }
        if (t.compareTo(this.pathPoints.get(this.pathPoints.size() - 1).getTimePoint()) >= 0) {
            return this.pathPoints.get(this.pathPoints.size() - 1);
        }

        TimedPositionWithSpeed p1 = null;
        TimedPositionWithSpeed p2 = null;
        for (TimedPositionWithSpeed p : this.pathPoints) {
            if (p.getTimePoint().compareTo(t) >= 0) {
                p2 = p;
                p1 = this.pathPoints.get(this.pathPoints.indexOf(p) - 1);
                break;
            }
        }

        double t1 = 1000.0 * p1.getTimePoint().asMillis();
        double t2 = 1000.0 * p2.getTimePoint().asMillis();
        double t0 = 1000.0 * t.asMillis();

        Distance dist = p1.getPosition().getDistance(p2.getPosition());
        Position p0 = p1.getPosition().translateGreatCircle(
                p1.getPosition().getBearingGreatCircle(p2.getPosition()), dist.scale((t0 - t1) / (t2 - t1)));
        SpeedWithBearing windAtPoint = this.windField.getWind(new TimedPositionImpl(t, p0));

        return new TimedPositionWithSpeedImpl(t, p0, windAtPoint);
    }

    // @Override
    public List<TimedPositionWithSpeed> getEvenTimedPoints(long milliseconds) {

        if (milliseconds == 0) {
            return null;
        }

        List<TimedPositionWithSpeed> lst = new ArrayList<TimedPositionWithSpeed>();
        TimePoint t = this.pathPoints.get(0).getTimePoint();
        TimePoint lastPoint = this.pathPoints.get(this.pathPoints.size() - 1).getTimePoint();

        while ((t.compareTo(lastPoint) <= 0) && (lst.size() < 200)) { // paths with more than 200 points lead to
            // performance issues
            lst.add(this.getPositionAtTime(t));
            t = new MillisecondsTimePoint(t.asMillis() + milliseconds);
        }
        if (t.compareTo(lastPoint) > 0) { // without this, the path may not reach end
            lst.add(this.getPositionAtTime(t));
        }
        // if (!lst.contains(pathPoints.get(pathPoints.size()-1)))
        // lst.add(pathPoints.get(pathPoints.size()-1));

        return lst;
    }

    // not implemented yet!
    // @Override
    public List<TimedPositionWithSpeed> getEvenDistancedPoints(Distance dist) {
        return null;
    }

    @Override
    public List<TimedPositionWithSpeed> getTurns() {
        if (this.pathPoints == null) {
            return null;
        }

        List<TimedPositionWithSpeed> list = new ArrayList<TimedPositionWithSpeed>();

        if (this.pathPoints.isEmpty()) {
            return list;
        }

        int noOfPoints = this.pathPoints.size();
        TimedPositionWithSpeed previousPoint = null;
        TimedPositionWithSpeed currentPoint = null;
        TimedPositionWithSpeed nextPoint = null;

        for (int index = 0; index < noOfPoints; index++) {

            currentPoint = this.pathPoints.get(index);

            if (index == 0 || index == (noOfPoints - 1)) {
                list.add(currentPoint);
            } else {

                previousPoint = this.pathPoints.get(index - 1);
                nextPoint = this.pathPoints.get(index + 1);

                if (isTurn(currentPoint.getPosition(), previousPoint.getPosition(), nextPoint.getPosition())) {
                    list.add(currentPoint);
                }
            }
        }

        return eliminateVeryCloseTurns(list, THRESHOLD_DISTANCE_METERS);
    }

    public static List<TimedPositionWithSpeed> eliminateVeryCloseTurns(List<TimedPositionWithSpeed> turns, double tresholdDistanceMeters) {

        List<TimedPositionWithSpeed> goodTurns = new ArrayList<TimedPositionWithSpeed>();

        int noOfTurnsMinus1 = turns.size() - 1;

        boolean clusterFound = false;
        int clusterStart = -1;
        int clusterEnd = -1;

        for (int index = 0; index < noOfTurnsMinus1; index++) {
            if (turns.get(index).getPosition().getDistance(turns.get(index + 1).getPosition()).getMeters() < tresholdDistanceMeters) {
                if (clusterFound == false) {
                    clusterFound = true;
                    clusterStart = index;
                }
                clusterEnd = index + 1;
            } else {
                if (clusterFound) {
                    clusterFound = false;
                    goodTurns.add(turns.get((clusterStart + clusterEnd) / 2));
                } else {
                    goodTurns.add(turns.get(index));
                }
            }
        }

        goodTurns.add(turns.get(turns.size() - 1));

        return goodTurns;
    }

    public static boolean saveToGpxFile(Path path, String fileName) {
        if (path == null) {
            return false;
        }
        List<TimedPositionWithSpeed> pathPoints = path.getPathPoints();
        if (pathPoints == null || pathPoints.isEmpty()) {
            return false;
        }
        TimedPositionWithSpeed timedPoint = null;
        Position point = null;
        int noOfPoints = pathPoints.size();
        Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuffer buffer = new StringBuffer();
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\r\n<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\r\n\t<trk>\r\n\t\t<name>GPSPolyPath</name>\r\n\t\t<trkseg>");
        for (int index = 0; index < noOfPoints; index++) {
            timedPoint = pathPoints.get(index);
            point = timedPoint.getPosition();
            buffer.append("\r\n\t\t\t<trkpt lat=\"" + point.getLatDeg() + "\" lon=\"" + point.getLngDeg()
                    + "\">\r\n\t\t\t\t<ele>0</ele>\r\n\t\t\t\t<time>"
                    + formatter.format(timedPoint.getTimePoint().asDate()) + "</time>\r\n\t\t\t</trkpt>");
        }
        buffer.append("\r\n\t\t</trkseg>\r\n\t</trk>\r\n</gpx>\r\n");
        String content = buffer.toString();
        try {
            FileWriter writer = new FileWriter(fileName);
            BufferedWriter output = new BufferedWriter(writer, 32768);
            try {
                output.write(content);
            } finally {
                output.close();
                writer.close();
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    private static boolean isTurn(Position currentPosition, Position previousPoint, Position nextPoint) {
        final Bearing b1 = previousPoint.getBearingGreatCircle(currentPosition);
        final Bearing b2 = currentPosition.getBearingGreatCircle(nextPoint);
        double diff = b1.getDifferenceTo(b2).getDegrees();

        if (diff < 0) {
            diff = 360 + diff;
        }

        return !((diff >= 0 && diff <= TRESHOLD_DEGREES) || (diff >= (180 - TRESHOLD_DEGREES) && diff <= (180 + TRESHOLD_DEGREES)) || (diff >= (360 - TRESHOLD_DEGREES) && diff <= 360));
    }

    @Override
    public TimePoint getFinalTime() {
        TimePoint finalTime = null;
        if (this.pathPoints != null) {
            if (this.pathPoints.size() > 0) {
                finalTime = this.pathPoints.get(this.pathPoints.size()-1).getTimePoint();
            }
        }
        return finalTime;
    }
}
