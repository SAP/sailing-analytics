package com.sap.sailing.simulator.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.sap.sailing.domain.base.SpeedWithBearing;
import com.sap.sailing.domain.base.impl.KilometersPerHourSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.impl.WindImpl;
import com.sap.sailing.simulator.Boundary;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.TimedPosition;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.WindControlParameters;
import com.sap.sailing.simulator.WindFieldGenerator;

public abstract class WindFieldGeneratorImpl implements WindFieldGenerator {

    protected Boundary boundary;
    protected WindControlParameters windParameters;
    protected Position[][] positions;
    protected Map<Pair<Integer, Integer>, Position> indexPositionMap;

    protected Map<TimePoint, SpeedWithBearing[][]> timeSpeedWithBearingMap;

    protected TimePoint startTime;
    protected TimePoint endTime;
    
    protected int[] gridRes;
    protected Position[] gridAreaGps;
    
    /**
     * TimePoint which constitutes one unit of time
     */
    protected TimePoint timeStep;

    /*
     * private class LatLngComparator implements Comparator<Position> {
     * 
     * @Override public int compare(Position p1, Position p2) {
     * 
     * if (p1.getLatDeg() < p2.getLatDeg()) { return -1; } else if (p1.getLatDeg() == p2.getLngDeg()) { if
     * (p1.getLngDeg() < p2.getLngDeg()) { return -1; } else if (p1.getLngDeg() == p2.getLngDeg()) { return 0; } else {
     * return 1; } } else { return 1; } }
     * 
     * }
     */

    public WindFieldGeneratorImpl(Boundary boundary, WindControlParameters windParameters) {
        this.boundary = boundary;
        this.windParameters = windParameters;
        this.positions = null;
        this.indexPositionMap = new HashMap<Pair<Integer, Integer>, Position>();
    }

    @Override
    public WindControlParameters getWindParameters() {
        return windParameters;
    }
    
    @Override
    public void setBoundary(Boundary boundary) {
        this.boundary = boundary;
    }
    
    @Override
    public Wind getWind(TimedPosition coordinates) {
        KnotSpeedImpl knotSpeedImpl = new KnotSpeedImpl(windParameters.baseWindSpeed);

        double wBearing = windParameters.baseWindBearing
                * (1 + coordinates.getPosition().getDistance(boundary.getCorners().get("NorthWest")).getMeters()
                        / boundary.getHeight().getMeters());
        SpeedWithBearing wspeed = new KilometersPerHourSpeedWithBearingImpl(knotSpeedImpl.getKilometersPerHour(),
                new DegreeBearingImpl(wBearing));

        return new WindImpl(coordinates.getPosition(), coordinates.getTimePoint(), wspeed);

    }

    @Override
    public Boundary getBoundaries() {
        return boundary;
    }

    @Override
    public Position[][] getPositionGrid() {
        return positions;
    }

    @Override
    public Path getLine(TimedPosition seed) {
        
        int maxSteps = 100;
        long timeStep = 10000; // in milliseconds
        
        TimePoint currentTime = seed.getTimePoint();
        Position currentPosition = seed.getPosition();
        LinkedList<TimedPositionWithSpeed> path = new LinkedList<TimedPositionWithSpeed>();
        path.add(new TimedPositionWithSpeedImpl(currentTime, currentPosition, null));
        
        for(int s=0; s<maxSteps; s++) {
         
            Wind currentWind = this.getWind(new TimedPositionImpl(currentTime, currentPosition));
            TimePoint middleTime = currentTime.plus(timeStep/2);
            Position middlePosition = currentWind.travelTo(currentPosition, middleTime, currentTime);
            Wind middleWind = this.getWind(new TimedPositionImpl(middleTime, middlePosition));
            
            TimePoint nextTime = currentTime.plus(timeStep);
            Position nextPosition = middleWind.travelTo(currentPosition, nextTime, currentTime);
            
            path.add(new TimedPositionWithSpeedImpl(nextTime, nextPosition, null));
            
            currentTime = nextTime;
            currentPosition = nextPosition;
        }
        
        return new PathImpl(path, this);
    }

    
    @Override
    public void setPositionGrid(Position[][] positions) {
        this.positions = positions;
        indexPositionMap.clear();
        if (positions == null || positions.length < 1) {
            return;
        }
        for (int i = 0; i < positions.length; ++i) {
            for (int j = 0; j < positions[0].length; ++j) {
                Pair<Integer, Integer> index = new Pair<Integer, Integer>(i, j);
                indexPositionMap.put(index, positions[i][j]);
            }
        }

    }

    public Position getPosition(int i, int j) {
        Pair<Integer, Integer> indexKey = new Pair<Integer, Integer>(i, j);
        return indexPositionMap.get(indexKey);

    }

    public Pair<Integer, Integer> getPositionIndex(Position p) {
        Pair<Integer, Integer> gIdx = boundary.getGridIndex(p);
        if ((gIdx.getA() != null) && (gIdx.getB() != null)) {
            return gIdx;
        } else {
            return null;
        }
    }

    /**
     * 
     * @param t
     * @return time units relative to the startTime where each unit is timeStep long
     */
    public int getTimeIndex(TimePoint t) {
        return (int) ((t.asMillis() - startTime.asMillis()) / timeStep.asMillis());
    }

    @Override
    public void generate(TimePoint start, TimePoint end, TimePoint step) {
        this.startTime = start;
        this.endTime = end;
        this.timeStep = step;
    }

    @Override
    public TimePoint getStartTime() {
        return this.startTime;
    }

    @Override
    public TimePoint getTimeStep() {
        return this.timeStep;
    }

    @Override
    public TimePoint getEndTime() {
        return this.endTime;
    }
    
    // params.getxRes(), params.getyRes()
    @Override
    public int[] getGridResolution() {
        return this.gridRes;
    }

    @Override
    public void setGridResolution(int[] gridRes) {
        this.gridRes = gridRes;
    } 

    @Override
    public Position[] getGridAreaGps() {
        return this.gridAreaGps;
    }

    @Override
    public void setGridAreaGps(Position[] gridAreaGps) {
        this.gridAreaGps = gridAreaGps;
    } 

}
