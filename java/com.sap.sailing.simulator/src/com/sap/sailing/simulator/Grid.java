package com.sap.sailing.simulator;

import java.io.Serializable;
import java.util.Map;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * The Grid interface defines access to a set of GPS-positions which form a lattice of supporting positions for
 * representing a vectorfield or performing optimization.
 * 
 * @author Christopher Ronnewinkel (D036654)
 * 
 */
public interface Grid extends Serializable {

    static Bearing relativeNorth = new DegreeBearingImpl(0);
    static Bearing relativeSouth = new DegreeBearingImpl(180);
    static Bearing relativeEast = new DegreeBearingImpl(90);
    static Bearing relativeWest = new DegreeBearingImpl(270);

    Map<String, Position> getCorners();

    boolean inBounds(Position P);

    Position[][] generatePositions(int hPoints, int vPoints, int borderY, int borderX);

    public Util.Pair<Integer, Integer> getIndex(Position x);

    public int getResY();

    public int getResX();

    public int getBorderY();

    public int getBorderX();

    Bearing getNorth();

    Bearing getSouth();

    Bearing getEast();

    Bearing getWest();

    Distance getHeight();

    Distance getWidth();

}
