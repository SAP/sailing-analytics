package com.sap.sailing.simulator.impl;

import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.simulator.Grid;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreePosition;

/**
 * Implements the {@link Grid} interface by providing a grid of GPS-positions based on the plane approximation of the
 * earth and the corresponding index-calculation to associate an arbitrary GPS-position with the closest
 * grid-GPS-position.
 * 
 * Since the grid has rectangular shape in the plane approximation, we call it rectangular grid. The plane approximation
 * allows for a simple index calculation based on CPU-light operations. If not aligned with North the grid gets slightly
 * distorted leading to asymmetries which may influence the accuracy of path calculations or optimizations.
 * Still, for typical race course diameters of up to 5 nautical miles, it is a very good and fast approximation.
 * 
 * @author Christopher Ronnewinkel (D036654)
 * 
 */
public class RectangularGrid implements Grid {

    private static final long serialVersionUID = 3598121983120213464L;
    private Position rcStart; // start position of race course
    private Position rcEnd; // end position of race course
    private double lngScale; // scale of distances longitude : latitude
    private double[] nvVrt; // vertical normal vector with suitable length
    private double[] nvHor; // horizontal normal vector with suitable length
    private int vPoints; // number of vertical steps
    private int hPoints; // number of horizontal steps
    private int borderY;
    private int borderX;

    private Position northWest;
    // private Position southEast;
    private Position southWest;
    private Position northEast;

    private Position appNorthWest;
    private Position appSouthEast;
    private Position appSouthWest;
    private Position appNorthEast;

    private Bearing north;
    private Bearing south;
    private Bearing east;
    private Bearing west;

    private Distance appWidth;
    private Distance appHeight;

    double tolerance;

    public RectangularGrid(Position p1, Position p2) {

        rcStart = p1;
        rcEnd = p2;

        tolerance = 0.1;

        north = p1.getBearingGreatCircle(p2);
        south = north.reverse();
        east = north.add(relativeEast);
        west = north.add(relativeWest);

        appHeight = p1.getDistance(p2);
        appWidth = appHeight.scale(2);

        // make sure race course stays in the middle
        appNorthWest = p2.translateGreatCircle(west, appHeight);
        appNorthEast = p2.translateGreatCircle(east, appHeight);

        appSouthWest = p1.translateGreatCircle(west, appHeight);
        appSouthEast = p1.translateGreatCircle(east, appHeight);

        Distance diag = appNorthWest.getDistance(appSouthEast);

        Bearing diag1 = appSouthEast.getBearingGreatCircle(appNorthWest);
        northWest = appNorthWest.translateGreatCircle(diag1, diag.scale(tolerance));
        // diag1 = diag1.reverse();
        // southEast = appSouthEast.translateGreatCircle(diag1, diag.scale(tolerance));
        Bearing diag2 = appSouthWest.getBearingGreatCircle(appNorthEast);
        northEast = appNorthEast.translateGreatCircle(diag2, diag.scale(tolerance));
        diag2 = diag2.reverse();
        southWest = appSouthWest.translateGreatCircle(diag2, diag.scale(tolerance));
    }

    @Override
    public Map<String, Position> getCorners() {

        Map<String, Position> map = new HashMap<String, Position>();
        map.put("NorthWest", appNorthWest);
        map.put("SouthWest", appSouthWest);
        map.put("SouthEast", appSouthEast);
        map.put("NorthEast", appNorthEast);

        return map;

    }

    @Override
    public boolean inBounds(Position p) {

        Position northProjection = p.projectToLineThrough(northWest, getEast());
        Position southProjection = p.projectToLineThrough(southWest, getEast());
        Position westProjection = p.projectToLineThrough(northWest, getNorth());
        Position eastProjection = p.projectToLineThrough(northEast, getNorth());

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

        double xscale = 1.5;

        double alat = (rcEnd.getLatDeg() + rcStart.getLatDeg()) / 2.;
        lngScale = Math.cos(alat * Math.PI / 180.);

        double[] dVrt = new double[2];
        dVrt[0] = rcEnd.getLatDeg() - rcStart.getLatDeg();
        dVrt[1] = rcEnd.getLngDeg() - rcStart.getLngDeg();

        double[] dscVrt = new double[2];
        dscVrt[0] = dVrt[0];
        dscVrt[1] = dVrt[1] * lngScale;

        double lscVrt = Math.sqrt(dscVrt[0] * dscVrt[0] + dscVrt[1] * dscVrt[1]);

        nvVrt = new double[2];
        nvVrt[0] = dscVrt[0] / lscVrt / lscVrt * (vPoints - 1);
        nvVrt[1] = dscVrt[1] / lscVrt / lscVrt * (vPoints - 1);

        double[] nscHor = new double[2];
        nscHor[0] = -dscVrt[1] / lscVrt;
        nscHor[1] = dscVrt[0] / lscVrt;

        nvHor = new double[2];
        nvHor[0] = nscHor[0] / xscale / lscVrt * (hPoints - 1);
        nvHor[1] = nscHor[1] / xscale / lscVrt * (hPoints - 1);

        double[] nHor = new double[2];
        nHor[0] = nscHor[0];
        nHor[1] = nscHor[1] / lngScale;

        Position[][] grid = new Position[vPoints + 2 * borderY][hPoints + 2 * borderX];
        Position pv;
        for (int i = -borderY; i < (vPoints + borderY); i++) {

            if (i == 0) {
                pv = rcStart;
            } else if (i == vPoints - 1) {
                pv = rcEnd;
            } else {
                pv = new DegreePosition(rcStart.getLatDeg() + i / (vPoints - 1.0) * dVrt[0], rcStart.getLngDeg() + i
                        / (vPoints - 1.0) * dVrt[1]);
            }

            int j = 0;
            // left side
            while (j < (hPoints + 2 * borderX) / 2) {
                grid[i + borderY][j] = new DegreePosition(pv.getLatDeg() - ((hPoints + 2 * borderX - 1) / 2. - j)
                        / (hPoints - 1) * nHor[0] * xscale * lscVrt, pv.getLngDeg()
                        - ((hPoints + 2 * borderX - 1) / 2. - j) / (hPoints - 1) * nHor[1] * xscale * lscVrt);
                j++;
            }

            // middle
            if ((hPoints + 2 * borderX) % 2 == 1) {
                grid[i + borderY][j] = pv;
                j++;
            }

            // right side
            while (j < (hPoints + 2 * borderX)) {
                grid[i + borderY][j] = new DegreePosition(pv.getLatDeg() + (j - (hPoints + 2 * borderX - 1) / 2.)
                        / (hPoints - 1) * nHor[0] * xscale * lscVrt, pv.getLngDeg()
                        + (j - (hPoints + 2 * borderX - 1) / 2.) / (hPoints - 1) * nHor[1] * xscale * lscVrt);
                j++;
            }
        }

        return grid;
    }

    public Util.Pair<Integer, Integer> getIndex(Position x) {

        double[] scX = new double[2];
        scX[0] = x.getLatDeg() - rcStart.getLatDeg();
        scX[1] = (x.getLngDeg() - rcStart.getLngDeg()) * lngScale;

        double sPrd = scX[0] * nvHor[0] + scX[1] * nvHor[1];

        int vIdx = Math.min(Math.max(-this.borderY, (int) Math.round(scX[0] * nvVrt[0] + scX[1] * nvVrt[1])), vPoints
                - 1 + this.borderY);
        int hIdx = Math.min(Math.max(-this.borderX, (int) Math.round(sPrd + (hPoints - 1) / 2.)), hPoints - 1
                + this.borderX);

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
        return north;
    }

    @Override
    public Bearing getSouth() {
        return south;
    }

    @Override
    public Bearing getEast() {
        return east;
    }

    @Override
    public Bearing getWest() {
        return west;
    }

    @Override
    public Distance getWidth() {
        return appWidth;
    }

    @Override
    public Distance getHeight() {
        return appHeight;
    }

}
