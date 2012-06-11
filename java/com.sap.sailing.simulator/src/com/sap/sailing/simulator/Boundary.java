package com.sap.sailing.simulator;

import java.util.List;
import java.util.Map;

import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.Util.Pair;

public interface Boundary {
	
	static Bearing TRUENORTH = new DegreeBearingImpl(0);
	static Bearing TRUESOUTH = new DegreeBearingImpl(180);
	static Bearing TRUEEAST = new DegreeBearingImpl(90);
	static Bearing TRUEWEST = new DegreeBearingImpl(270);
	
	double getTolerance();
	
	Map<String, Position> getCorners();
	
	boolean isWithinBoundaries(Position P);
	
	//List<Position> extractLattice(int hPoints, int vPoints);
	Position[][] extractGrid(int hPoints, int vPoints);
	//List<Position> extractLattice(Distance hStep, Distance vstep); 
	public Pair<Integer,Integer> getGridIndex(Position x);

	Bearing getNorth();
	Bearing getSouth();
	Bearing getEast();
	Bearing getWest();
	
	Distance getHeight();
	Distance getWidth();
	
	Map<String,Double> getRelativeCoordinates(Position p);
	Position getRelativePoint(double x, double y);
	
}
