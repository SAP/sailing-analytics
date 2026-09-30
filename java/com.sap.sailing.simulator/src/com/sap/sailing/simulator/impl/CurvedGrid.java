package com.sap.sailing.simulator.impl;

import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.simulator.Grid;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.Util;

/**
 * Implements the {@link Grid} interface by providing a grid of GPS-positions based on great circles and the
 * corresponding index-calculation to associate an arbitrary GPS-position with the closest grid-GPS-position.
 * 
 * Since the grid is constructed in spherical coordinates, we call it the curved grid. It stays perfectly symmetric no
 * matter along which bearing to North it is aligned with.
 * 
 * @author Christopher Ronnewinkel (D036654)
 * 
 */
public class CurvedGrid implements Grid {

    private static final long serialVersionUID = 3598121983120213464L;
    private Position rcStart; // start position of race course
    private Position rcEnd; // end position of race course
    private int vPoints; // number of vertical steps
    private int hPoints; // number of horizontal steps
    private int borderY;
    private int borderX;

    private Position gridNorthWest;
    private Position gridSouthEast;
    private Position gridSouthWest;
    private Position gridNorthEast;

    private Bearing gridNorth;
    private Bearing gridSouth;
    private Bearing gridEast;
    private Bearing gridWest;

    private Distance gridWidth;
    private double xscale = 1.5;
    private Distance gridHeight;

    public CurvedGrid(Position p1, Position p2) {

        rcStart = p1;
        rcEnd = p2;

        gridNorth = p1.getBearingGreatCircle(p2);
        gridSouth = gridNorth.reverse();
        gridEast = gridNorth.add(relativeEast);
        gridWest = gridNorth.add(relativeWest);

        gridHeight = p1.getDistance(p2);
        gridWidth = gridHeight.scale(2);

        gridNorthWest = p2.translateGreatCircle(gridWest, gridHeight);
        gridNorthEast = p2.translateGreatCircle(gridEast, gridHeight);

        gridSouthWest = p1.translateGreatCircle(gridWest, gridHeight);
        gridSouthEast = p1.translateGreatCircle(gridEast, gridHeight);

    }

    @Override
    public Map<String, Position> getCorners() {

        Map<String, Position> map = new HashMap<String, Position>();
        map.put("NorthWest", gridNorthWest);
        map.put("SouthWest", gridSouthWest);
        map.put("SouthEast", gridSouthEast);
        map.put("NorthEast", gridNorthEast);

        return map;

    }

    @Override
    public boolean inBounds(Position p) {

        Position northProjection = p.projectToLineThrough(gridNorthWest, getEast());
        Position southProjection = p.projectToLineThrough(gridSouthWest, getEast());
        Position westProjection = p.projectToLineThrough(gridNorthWest, getNorth());
        Position eastProjection = p.projectToLineThrough(gridNorthEast, getNorth());

        Distance northSouth = northProjection.getDistance(southProjection);
        Distance eastWest = eastProjection.getDistance(westProjection);

        return (northSouth.compareTo(p.getDistance(northProjection)) >= 0)
                && (northSouth.compareTo(p.getDistance(southProjection)) >= 0)
                && (eastWest.compareTo(p.getDistance(eastProjection)) >= 0)
                && (eastWest.compareTo(p.getDistance(westProjection)) >= 0);

    }

    @Override
    public Position[][] generatePositions(int hPoints, int vPoints, int borderY, int borderX) {

        this.vPoints = vPoints;
        this.hPoints = hPoints;

        this.borderY = borderY;
        this.borderX = borderX;

        Distance vStep = gridHeight.scale(1.0 / (vPoints - 1));
        Distance hStep = gridHeight.scale(xscale / (hPoints - 1));

        Position[][] grid = new Position[vPoints + 2 * borderY][hPoints + 2 * borderX];
        Position pv;
        for (int i = -borderY; i < (vPoints + borderY); i++) {

            if (i == 0) {
                pv = rcStart;
            } else if (i == vPoints - 1) {
                pv = rcEnd;
            } else {
                pv = rcStart.translateGreatCircle(gridNorth, vStep.scale(i));
            }

            int j = 0;
            // left side
            while (j < (hPoints + 2 * borderX) / 2) {
                grid[i + borderY][j] = pv.translateGreatCircle(gridWest,
                        hStep.scale((hPoints + 2 * borderX - 1) / 2. - j));
                j++;
            }

            // middle
            if ((hPoints + 2 * borderX) % 2 == 1) {
                grid[i + borderY][j] = pv;
                j++;
            }

            // right side
            while (j < (hPoints + 2 * borderX)) {
                grid[i + borderY][j] = pv.translateGreatCircle(gridEast,
                        hStep.scale(j - (hPoints + 2 * borderX - 1) / 2.));
                j++;
            }
        }

        return grid;
    }

    public Util.Pair<Integer, Integer> getIndex(Position x) {

        double vFlt = 0;
        double hFlt = 0;

        if (!x.equals(rcStart)) {
            Position h = x.projectToLineThrough(rcStart, gridNorth);
            vFlt = vPoints * rcStart.getDistance(h).getMeters() / gridHeight.getMeters();
            int sign = +1;
            if (Math.abs(h.getBearingGreatCircle(x).getDifferenceTo(gridWest).getDegrees()) < 90.0) {
                sign = -1;
            }
            hFlt = sign * (hPoints - 1) * x.getDistance(h).getMeters() / gridHeight.getMeters() / xscale;
        }

        int vIdx = Math.min(Math.max(-this.borderY, (int) Math.round(vFlt) + this.borderY), vPoints - 1 + this.borderY);
        int hIdx = Math.min(Math.max(-this.borderX, (int) Math.round((hPoints - 1) / 2. + hFlt)), hPoints - 1 + this.borderX);

        return new Util.Pair<Integer, Integer>(vIdx, hIdx);
    }

    @Override
    public int getResY() {
        return this.vPoints;
    }

    @Override
    public int getResX() {
        return this.hPoints;
    }

    @Override
    public int getBorderY() {
        return this.borderY;
    }

    @Override
    public int getBorderX() {
        return this.borderX;
    }

    @Override
    public Bearing getNorth() {
        return gridNorth;
    }

    @Override
    public Bearing getSouth() {
        return gridSouth;
    }

    @Override
    public Bearing getEast() {
        return gridEast;
    }

    @Override
    public Bearing getWest() {
        return gridWest;
    }

    @Override
    public Distance getWidth() {
        return gridWidth;
    }

    @Override
    public Distance getHeight() {
        return gridHeight;
    }

}
