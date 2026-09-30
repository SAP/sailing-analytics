package com.sap.sailing.simulator.windfield.impl;

import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.impl.KilometersPerHourSpeedWithBearingImpl;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.simulator.Grid;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.TimedPosition;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.impl.PathImpl;
import com.sap.sailing.simulator.impl.TimedPositionImpl;
import com.sap.sailing.simulator.impl.TimedPositionWithSpeedImpl;
import com.sap.sailing.simulator.windfield.WindControlParameters;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;

public abstract class WindFieldGeneratorImpl implements WindFieldGenerator {

    private static final long serialVersionUID = -6366698648104363491L;
    protected Grid boundary;
    protected WindControlParameters windParameters;
    protected Position[][] positions;
  
    protected Map<TimePoint, SpeedWithBearing[][]> timeSpeedWithBearingMap;

    protected TimePoint startTime;
    protected TimePoint endTime;
    
    protected int[] gridRes;
    protected Position[] gridAreaGps;
    
    /**
     * TimePoint which constitutes one unit of time
     */
    protected Duration timeStep;

    private static Logger logger = Logger.getLogger("com.sap.sailing.windfield");

    public WindFieldGeneratorImpl(Grid boundary, WindControlParameters windParameters) {
    	this.boundary = boundary;
    	this.windParameters = windParameters;
    	this.positions = null;
    }

    @Override
    public WindControlParameters getWindParameters() {
    	return windParameters;
    }

    @Override
    public void setBoundary(Grid boundary) {
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
    public Grid getGrid() {
    	return boundary;
    }

    @Override
    public Position[][] getPositionGrid() {
    	return positions;
    }

    @Override
    public Path getLine(TimedPosition seed, boolean forward) {

    	long timeStep = 10000; // in milliseconds

    	TimePoint currentTime = seed.getTimePoint();
    	TimePoint startTime = seed.getTimePoint();
    	Position currentPosition = seed.getPosition();
    	LinkedList<TimedPositionWithSpeed> path = new LinkedList<TimedPositionWithSpeed>();
    	path.add(new TimedPositionWithSpeedImpl(currentTime, currentPosition, null));

    	while(boundary.inBounds(currentPosition)) {
    		Wind currentWind = this.getWind(new TimedPositionImpl(startTime, currentPosition));
    		TimePoint middleTime = currentTime.plus(timeStep/2);
    		Position middlePosition;
    		if (!forward) {
    			middlePosition = currentWind.travelTo(currentPosition, middleTime, currentTime);
    		} else {
    			middlePosition = currentWind.travelTo(currentPosition, currentTime, middleTime);
    		}
    		Wind middleWind = this.getWind(new TimedPositionImpl(startTime, middlePosition));

    		TimePoint nextTime = currentTime.plus(timeStep);
    		Position nextPosition;
    		if (!forward) {
    			nextPosition = middleWind.travelTo(currentPosition, nextTime, currentTime);
    		} else {
    			nextPosition = middleWind.travelTo(currentPosition, currentTime, nextTime);
    		}

    		path.add(new TimedPositionWithSpeedImpl(nextTime, nextPosition, null));

    		currentTime = nextTime;
    		currentPosition = nextPosition;
    	}

    	logger.info("Added wind line with " + path.size()  + "points");

    	return new PathImpl(path, this, false /* algorithmTimedOut */);
    }


    @Override
    public void setPositionGrid(Position[][] positions) {
    	this.positions = positions;
    }

    public Position getPosition(int i, int j) {
    	return this.positions[i][j];
    }

    public Util.Pair<Integer, Integer> getPositionIndex(Position p) {
    	Util.Pair<Integer, Integer> gIdx = boundary.getIndex(p);
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
    public void generate(TimePoint start, TimePoint end, Duration step) {
        this.startTime = start;
        this.endTime = end;
        this.timeStep = step;
    }

    @Override
    public TimePoint getStartTime() {
        return this.startTime;
    }

    @Override
    public Duration getTimeStep() {
        return this.timeStep;
    }

    @Override
    public TimePoint getEndTime() {
        return this.endTime;
    }
    
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
