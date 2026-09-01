package com.sap.sailing.simulator.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;

import com.sap.sailing.simulator.Grid;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.TimedPosition;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.windfield.WindField;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PathGeneratorDynProgForward extends PathGeneratorBase {

    public PathGeneratorDynProgForward(SimulationParameters params) {
        this.parameters = params;
    }

    //
    // dynamic programming approach with forward iteration along start-end line
    //

    // find minimum duration in alternate steps from h1=hidx to the calling h2
    private DPDuration findMinDur(ArrayList<DPDuration> alldur) {
        DPDuration mindur = alldur.get(0);
        for (int i = 0; i < alldur.size(); i++) {
            if (alldur.get(i).duration < mindur.duration) {
                mindur = alldur.get(i);
            }
        }

        return mindur;
    }

    // calculate duration of step from position p1 to position p2 at time curtime from side of wind side1
    private DPDuration calcDuration(TimePoint curtime, int side1, Position p1, Position p2, WindField windField,
            PolarDiagram polarDiagram) {

        // set turnloss to be approximately 4sec (until polardiagram has getTurnloss() method)
        long turnloss = polarDiagram.getTurnLoss(); // 4000;

        // System.out.println("p1: "+p1+" p2:"+p2);

        // take wind of starting position p1 at current time
        TimedPosition currentTimedPosition = new TimedPositionImpl(curtime, p1);
        SpeedWithBearing currentWind = windField.getWind(currentTimedPosition);
        // alternate approach: take wind of target position p2 at current time
        // TimedPosition windTimedPosition = new TimedPositionImpl(curtime, p2);
        // SpeedWithBearing currentWind = windField.getWind(windTimedPosition);

        // System.out.println("wind: "+currentWind.getKnots()+", "+currentWind.getBearing().getDegrees()+"°");

        // set polar diagram to current wind
        polarDiagram.setWind(currentWind);

        // calculate bearing, distance and speed from p1 to p2
        Bearing bearingToP = p1.getBearingGreatCircle(p2);
        Distance distanceToP = p1.getDistance(p2);
        Speed speedToP = polarDiagram.getSpeedAtBearingOverGround(bearingToP);
        // System.out.println("p1 to p2: angle: "+bearingToP.getDegrees()+"° dist: "+distanceToP.getMeters()+"m speed: "+speedToP.getMetersPerSecond()+"m/s");

        // add time delta for sailing from p1 to p2 to current time
        double deltat;
        if (speedToP.getMetersPerSecond() <= 0.1) {
            deltat = 86400; // set a high value for small times
        } else {
            deltat = (distanceToP.getMeters() / speedToP.getMetersPerSecond());
        }
        long timeToP = curtime.asMillis() + (long) (deltat) * 1000;
        // System.out.println("time: "+deltat/60.+"min");

        // identify turn based on change of sign of relative bearing of p1 to p2 towards the wind
        Bearing relativeBearing = currentWind.getBearing().reverse().getDifferenceTo(bearingToP);
        int side2;
        if (relativeBearing.getDegrees() == 0) {
            side2 = 0;
        } else {
            side2 = (int) Math.signum(relativeBearing.getDegrees());
        }
        if ((side1 != 0) && (side2 != 0) && (side1 == -side2)) {
            timeToP = timeToP + turnloss;
        }

        // return arrival time at p2 and wind side at p2 as durP instance
        DPDuration durToP = new DPDuration(timeToP, side2, 0, 0);

        return durToP;
    }

    // duration parameters: duration in millis, side of the wind, referencing horizontal grid column hidx,
    // and path index pidx (pidx currently unused, maybe used for structuring in groups of equivalent time, see
    // prototype R-script)
    class DPDuration {
        public DPDuration(long dur, int sid, int h, int p) {
            duration = dur;
            side = sid;
            hidx = h;
            pidx = p;
        }

        long duration;
        int side;
        int hidx;
        int pidx;
    }

    // location parameters: horizontal grid index idx, duration dur to reach this point (within path)
    class DPLocation {
        public DPLocation(int i, long d) {
            idx = i;
            dur = d;
        }

        int idx;
        long dur;
    }

    // @Override
    @Override
    public Path getPath() {
        this.algorithmStartTime = MillisecondsTimePoint.now();

        // retrieve simulation parameters
        Grid boundary = new RectangularGrid(this.parameters.getCourse().get(0), this.parameters
                .getCourse().get(1));// simulationParameters.getBoundaries();
        WindFieldGenerator windField = this.parameters.getWindField();
        PolarDiagram polarDiagram = this.parameters.getBoatPolarDiagram();
        // Position start = simulationParameters.getCourse().get(0);
        // Position end = simulationParameters.getCourse().get(1);
        TimePoint startTime = windField.getStartTime();

        // the optimal path
        LinkedList<TimedPositionWithSpeed> optPath = new LinkedList<TimedPositionWithSpeed>();

        // initiate grid: since performance is good, make it somewhat larger than what we do with dijkstra
        int spatialGridsizeVertical = 21; //(int) Math.round(1.5 *
        // simulationParameters.getProperty("Djikstra.gridv[int]").intValue()); // number
        // of
        // vertical
        // grid
        // steps
        // Formula: sgridh ~ xscale*sgridv/tan(beatangle*pi/180)
        int spatialGridsizeHorizontal = 23*6; //(41-1)*5; //(int) Math.round(2 *
        // simulationParameters.getProperty("Djikstra.gridh[int]").intValue()); // number
        // of
        // horizontal
        // grid
        // steps

        // make horizontal grid size uneven, to have an index for the middle line
        if (spatialGridsizeHorizontal % 2 == 0) {
            spatialGridsizeHorizontal++;
        }

        // generate grid positions using sgridh and sgridv
        Position[][] sailGrid = boundary.generatePositions(spatialGridsizeHorizontal, spatialGridsizeVertical, 0, 0);

        // optimization grid indexes:
        // vertical steps: 0, ..., v-1
        // horizontal steps: -gridh, ..., 0, ..., +gridh
        int optimizationGridsizeVertical = spatialGridsizeVertical;
        int optimizationGridsizeHorizontal = spatialGridsizeHorizontal / 2;

        ArrayList<DPDuration> alldur;
        ArrayList<ArrayList<Vector<DPLocation>>> paths = new ArrayList<ArrayList<Vector<DPLocation>>>();
        ArrayList<DPDuration> duras = new ArrayList<DPDuration>();

        // in each vertical step, paths having the same minimum duration are kept
        double mintol = 1.0; // threshold of closeness to realy minimum duration for keeping paths
        int maxeqpaths = 2; // maximum nmber of equal paths to keep [currently set to low values, to save Java heap
        // memory]

        // loop over vertical steps
        for (int idxv = 0; idxv < (optimizationGridsizeVertical - 1); idxv++) {

            // System.out.println("  vstep: "+idxv);

            ArrayList<ArrayList<Vector<DPLocation>>> pathsnew = new ArrayList<ArrayList<Vector<DPLocation>>>();
            ArrayList<DPDuration> durasnew = new ArrayList<DPDuration>();

            // get start boundary correct
            int range1;
            if (idxv == 0) {
                range1 = 0;
            } else {
                range1 = optimizationGridsizeHorizontal;
            }

            // get end boundary correct
            int range2;
            if (idxv == (optimizationGridsizeVertical - 2)) {
                range2 = 0;
            } else {
                range2 = optimizationGridsizeHorizontal;
            }

            // target loop: idxh2
            for (int idxh2 = -range2; idxh2 <= range2; idxh2++) {

                alldur = new ArrayList<DPDuration>();

                if (idxv > 0) { // if not at start of the grid

                    // origin loop: idxh1
                    for (int idxh1 = -range1; idxh1 <= range1; idxh1++) {

                        if (this.isTimedOut()) {
                            break;
                        }
                        
                        // calculate the time to go from [idxv,idxh1] to [idxv+1,idxh2]
                        DPDuration durationh1h2 = calcDuration(new MillisecondsTimePoint(
                                duras.get(idxh1 + range1).duration), duras.get(idxh1 + range1).side,
                                sailGrid[idxv][idxh1 + optimizationGridsizeHorizontal], sailGrid[idxv + 1][idxh2
                                                                                                           + optimizationGridsizeHorizontal], windField, polarDiagram);

                        // *forbidden* angles
                        if (durationh1h2.duration < 0) {
                            continue;
                        }

                        // remember durations of current vertical step in alldur
                        durationh1h2.hidx = idxh1 + range1;
                        alldur.add(durationh1h2);

                    } // horizontal origin loop: idxh1

                    if (idxv != (optimizationGridsizeVertical - 2)) { // if not at end of the grid

                        // find minimum duration in alldur
                        DPDuration mindur = findMinDur(alldur);

                        // remember minimum duration for reaching [idxv,idxh2]
                        DPDuration tdur = new DPDuration(mindur.duration, mindur.side, idxh2
                                + optimizationGridsizeHorizontal, 0);
                        durasnew.add(tdur);

                        // keep all (see mintol, maxeqpaths) paths with minimum duration for reaching [idxv,idxh2]
                        int k = 0;
                        for (int j = 0; j < alldur.size(); j++) {

                            if (this.isTimedOut()) {
                                break;
                            }
                            
                            if ((alldur.get(j).duration <= mintol * mindur.duration) && (k < maxeqpaths)) {
                                ArrayList<Vector<DPLocation>> npaths;
                                if ((pathsnew.size() > 0)
                                        && (pathsnew.size() == (idxh2 + optimizationGridsizeHorizontal + 1))) {
                                    npaths = pathsnew.get(idxh2 + optimizationGridsizeHorizontal);
                                } else {
                                    npaths = new ArrayList<Vector<DPLocation>>();
                                    pathsnew.add(npaths);
                                }
                                ArrayList<Vector<DPLocation>> tpaths = paths.get(alldur.get(j).hidx);
                                for (int i = 0; i < tpaths.size(); i++) {
                                    Vector<DPLocation> tpath;
                                    tpath = new Vector<DPLocation>(tpaths.get(i)); // (Vector<DPLocation>)
                                    // (tpaths.get(i)).clone();
                                    tpath.add(new DPLocation(idxh2, mindur.duration));
                                    npaths.add(tpath);
                                }
                                k++;
                            }

                        }

                    } else {

                        // find minimum in alldur
                        DPDuration mindur = findMinDur(alldur);

                        // remember minimum duration for reaching [idxv,idxh2]
                        DPDuration tdur = new DPDuration(mindur.duration, mindur.side, 0, 0);
                        durasnew.add(tdur);

                        // keep all (see mintol, maxeqpaths) paths with minimum duration for reaching [idxv,idxh2]
                        int k = 0;
                        for (int j = 0; j < alldur.size(); j++) {

                            if (this.isTimedOut()) {
                                break;
                            }

                            if ((alldur.get(j).duration <= mintol * mindur.duration) && (k < maxeqpaths)) {
                                ArrayList<Vector<DPLocation>> npaths;
                                if ((pathsnew.size() > 0) && (pathsnew.size() == (idxh2 + range2 + 1))) {
                                    npaths = pathsnew.get(idxh2 + range2);
                                } else {
                                    npaths = new ArrayList<Vector<DPLocation>>();
                                    pathsnew.add(npaths);
                                }
                                ArrayList<Vector<DPLocation>> tpaths = paths.get(alldur.get(j).hidx);
                                for (int i = 0; i < tpaths.size(); i++) {
                                    Vector<DPLocation> tpath;
                                    tpath = new Vector<DPLocation>(tpaths.get(i)); // (Vector<DPLocation>)
                                    // (tpaths.get(i)).clone();
                                    tpath.add(new DPLocation(0, mindur.duration));
                                    npaths.add(tpath);
                                }
                                k++;
                            }

                        }
                    }

                } else { // start of grid

                    Vector<DPLocation> tpath = new Vector<DPLocation>();
                    tpath.add(new DPLocation(0, startTime.asMillis()));
                    ArrayList<Vector<DPLocation>> tpaths = new ArrayList<Vector<DPLocation>>();
                    tpaths.add(tpath);
                    pathsnew.add(tpaths);
                    DPDuration tdur = calcDuration(startTime, 0, sailGrid[0][optimizationGridsizeHorizontal],
                            sailGrid[1][idxh2 + optimizationGridsizeHorizontal], windField, polarDiagram);
                    tdur.hidx = idxh2 + optimizationGridsizeHorizontal;
                    durasnew.add(tdur);
                    tpath.add(new DPLocation(idxh2, tdur.duration));
                }

            } // horizontal target loop: idxh2

            paths = pathsnew;
            duras = durasnew;

            /*
             * if (idxv == (gridv-2)) { System.out.print("    optimal path:"); for(int i=0; i<1;i++) {//paths.size();
             * i++) { //System.out.print(""+duras.get(i).duration+" - "); for(int j=0; j<1;j++) {//paths.get(i).size();
             * j++) { int kmax = paths.get(i).get(j).size();
             * System.out.print(""+((paths.get(i).get(j).get(kmax-1).dur-paths
             * .get(i).get(j).get(0).dur)/1000./60.)+"min: "); for(int k=0;k<kmax; k++) {
             * System.out.print(" "+paths.get(i).get(j).get(k).idx); } System.out.println(); } } }
             */

        }

        
        if (!this.isTimedOut()) {
            // generate timed position path
            for (int i = 0; i < paths.size(); i++) {
                // for(int j=0; j<paths.get(i).size(); j++) {
                for (int j = 0; j < 1; j++) { // restrict to first path
                    for (int k = 0; k < paths.get(i).get(j).size(); k++) {
                        Position p = sailGrid[k][paths.get(i).get(j).get(k).idx + optimizationGridsizeHorizontal];
                        TimePoint t = new MillisecondsTimePoint(paths.get(i).get(j).get(k).dur);
                        // System.out.println("deltaT: "+(t.asMillis()-startTime.asMillis())/1000.);
                        optPath.add(new TimedPositionWithSpeedImpl(t, p, null));
                    }
                }
            }
        }

        return new PathImpl(optPath, windField, this.algorithmTimedOut);
    }

}
