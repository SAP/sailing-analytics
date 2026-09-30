package com.sap.sailing.simulator.impl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.TimedPosition;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PathGeneratorTreeGrow extends PathGeneratorBase {

    private static final Logger logger = Logger.getLogger(com.sap.sailing.simulator.impl.PathGeneratorTreeGrow.class.getName());
    private boolean debugMsgOn = false;
    
    double oobFact = 2.0; // out-of-bounds factor
    int maxTurns = 0;
    boolean upwindLeg = false;
    String initPathStr = "0";
    PathCandidate bestCand = null;
    long usedTimeStep = 0;
    boolean gridStore = false;
    ArrayList<List<PathCandidate>> gridPositions = null;
    ArrayList<List<PathCandidate>> isocPositions = null;
    String gridFile = null;

    public PathGeneratorTreeGrow(SimulationParameters params) {
        PolarDiagram polarDiagramClone = new PolarDiagramBase((PolarDiagramBase)params.getBoatPolarDiagram());
        this.parameters = new SimulationParametersImpl(params.getCourse(), polarDiagramClone, params.getWindField(),
                params.getSimuStep(), params.getMode(), params.showOmniscient(), params.showOpportunist(), params.getLegType());
    }

    public void setEvaluationParameters(String startDirection, int maxTurns, String gridFile) {
        if (startDirection != null) {
            this.initPathStr = "0" + startDirection;
        } else {
            this.initPathStr = "0";
        }
        this.maxTurns = maxTurns;
        this.gridFile = gridFile;
        if (this.gridFile != null) {
            this.gridStore = true;
            this.gridPositions = new ArrayList<List<PathCandidate>>();
            this.isocPositions = new ArrayList<List<PathCandidate>>();
        } else {
            this.gridStore = false;
            this.gridPositions = null;
            this.isocPositions = null;
        }
    }

    class SortPathCandsAbsHorizontally implements Comparator<PathCandidate> {

        @Override
        public int compare(PathCandidate p1, PathCandidate p2) {
            if (Math.abs(p1.hrz) == Math.abs(p2.hrz)) {
                return 0;
            } else {
                return (Math.abs(p1.hrz) < Math.abs(p2.hrz) ? -1 : +1);
            }
        }

    }

    class SortPathCandsHorizontally implements Comparator<PathCandidate> {

        @Override
        public int compare(PathCandidate p1, PathCandidate p2) {
            if (p1.hrz == p2.hrz) {
                return 0;
            } else {
                return (p1.hrz < p2.hrz ? -1 : +1);
            }
        }

    }

    // getter for evaluating best path cand propoerties further
    PathCandidate getBestCand() {
        return this.bestCand;
    }

    long getUsedTimeStep() {
        return this.usedTimeStep;
    }

    // generate step in one of the possible directions
    // default: L - left, R - right
    // extended: M - wide left, S - wide right
    TimedPosition getStep(TimedPosition pos, Wind posWind, long timeStep, long turnLoss, boolean sameBaseDirection,
            char nextDirection) {

        double offDeg = 3.0;
        TimePoint curTime = pos.getTimePoint();
        Position curPosition = pos.getPosition();

        PolarDiagram pd = this.parameters.getBoatPolarDiagram();
        pd.setWind(posWind);

        // get beat-angle left and right
        Bearing travelBearing = null;
        Bearing tmpBearing = null;
        if (nextDirection == 'L') {
            if (this.upwindLeg) {
                travelBearing = pd.optimalDirectionsUpwind()[0];
            } else {
                travelBearing = pd.optimalDirectionsDownwind()[0];
            }
        } else if (nextDirection == 'R') {
            if (this.upwindLeg) {
                travelBearing = pd.optimalDirectionsUpwind()[1];
            } else {
                travelBearing = pd.optimalDirectionsDownwind()[1];
            }
        } else if (nextDirection == 'M') {
            if (this.upwindLeg) {
                tmpBearing = pd.optimalDirectionsUpwind()[0];
                travelBearing = tmpBearing.add(new DegreeBearingImpl(-offDeg));
            } else {
                tmpBearing = pd.optimalDirectionsDownwind()[0];
                travelBearing = tmpBearing.add(new DegreeBearingImpl(-offDeg));
            }
        } else if (nextDirection == 'S') {
            if (this.upwindLeg) {
                tmpBearing = pd.optimalDirectionsUpwind()[1];
                travelBearing = tmpBearing.add(new DegreeBearingImpl(+offDeg));
            } else {
                tmpBearing = pd.optimalDirectionsDownwind()[1];
                travelBearing = tmpBearing.add(new DegreeBearingImpl(+offDeg));
            }
        }

        // determine beat-speed left and right
        SpeedWithBearing travelSpeed = pd.getSpeedAtBearing(travelBearing);

        TimePoint travelTime;
        TimePoint nextTime = new MillisecondsTimePoint(curTime.asMillis() + timeStep);
        if (sameBaseDirection) {
            travelTime = nextTime;
        } else {
            travelTime = new MillisecondsTimePoint(nextTime.asMillis() - turnLoss);
        }

        Position nextPosition = travelSpeed.travelTo(curPosition, curTime, travelTime);
        return new TimedPositionImpl(nextTime, nextPosition);
    }

    // check whether nextDirection is same base direction as previous direction, i.e. no turn
    boolean isSameDirection(char prevDirection, char nextDirection) {
        return ((nextDirection == prevDirection) || (prevDirection == '0'));
    }

    // get path candidate measuring height towards (local, current-apparent) wind
    PathCandidate getPathCandWind(PathCandidate path, char nextDirection, long timeStep, long turnLoss,
            Position posStart, Position posEnd, double tgtHeight) {

        char prevDirection = path.path.charAt(path.path.length() - 1);
        boolean sameBaseDirection = this.isSameDirection(prevDirection, nextDirection);

        int turnCount = path.trn;
        if (!sameBaseDirection) {
            turnCount++;
        }

        // calculate next path position (taking turn-loss into account)
        TimedPosition pathPos = this.getStep(path.pos, path.wind, timeStep, turnLoss, sameBaseDirection, nextDirection);

        // determine apparent wind at next path position & time
        Wind posWind = this.parameters.getWindField().getWind(pathPos);

        // calculate height-position with reference to target
        Bearing bearVrt = posStart.getBearingGreatCircle(posEnd);
        Position posHeight = pathPos.getPosition().projectToLineThrough(posEnd, bearVrt.reverse());

        // calculate vertical distance as distance of height-position to end
        Bearing bearHeight = posEnd.getBearingGreatCircle(posHeight);
        double bearHeightSide = bearVrt.reverse().getDifferenceTo(bearHeight).getDegrees();
        double vrtSide = -1.0;
        if (Math.abs(bearHeightSide) > 170.0) {
            vrtSide = +1.0;
        }
        double vrtDist = vrtSide * Math.round(posHeight.getDistance(posEnd).getMeters() * 1000.0) / 1000.0;

        // scale last step to exactly reach height of posEnd (in reference to target) and adjust time correspondingly
        boolean reachedEnd = false;
        if ((!path.reached) && (vrtDist > 0.0)) {
            // scale last step so that vrtDist ~ tgtHeight
            Position prevPos = path.pos.getPosition();
            TimePoint prevTime = path.pos.getTimePoint();
            double heightFrac = path.vrt / (path.vrt - vrtDist);
            Position newPos = prevPos.translateGreatCircle(prevPos.getBearingGreatCircle(pathPos.getPosition()),
                    prevPos.getDistance(pathPos.getPosition()).scale(heightFrac));
            long newTimeMillis = Math.round((prevTime.asMillis() + (pathPos.getTimePoint().asMillis() - prevTime
                    .asMillis()) * heightFrac) / 1000.0) * 1000;
            TimePoint newTime = new MillisecondsTimePoint(newTimeMillis);
            pathPos = new TimedPositionImpl(newTime, newPos);
            reachedEnd = true;
        }

        // calculate horizontal side: left or right in reference to race course
        double posSide = 1;
        Bearing posBear = posStart.getBearingGreatCircle(pathPos.getPosition());
        double posBearDiff = bearVrt.getDifferenceTo(posBear).getDegrees();
        if ((posBearDiff < 0.0) || (posBearDiff > 180.0)) {
            posSide = -1;
        } else if ((posBearDiff == 0.0) || (posBearDiff == 180.0)) {
            posSide = 0;
        }
        // calculate horizontal distance as distance of height-position to current position
        Position posHeightTrgt = pathPos.getPosition().projectToLineThrough(posStart, bearVrt);
        double hrzDist = Math.round(posSide * posHeightTrgt.getDistance(pathPos.getPosition()).getMeters() * 1000.0) / 1000.0;

        // extend path-string by step-direction
        String pathStr = path.path + nextDirection;

        return (new PathCandidate(pathPos, reachedEnd, vrtDist, hrzDist, turnCount, pathStr, nextDirection, posWind, null));
    }

    // generate path candidates based on beat angles
    List<PathCandidate> getPathCandsBeatWind(PathCandidate path, long timeStep, long turnLoss, Position posStart,
            Position posEnd, double tgtHeight) {

        List<PathCandidate> result = new ArrayList<PathCandidate>();
        PathCandidate newPathCand;

        if (this.maxTurns > 0) {

            char prevDirection = path.path.charAt(path.path.length() - 1);
            if ((path.trn < this.maxTurns) || (this.isSameDirection(prevDirection, 'L'))) {
                newPathCand = getPathCandWind(path, 'L', timeStep, turnLoss, posStart, posEnd, tgtHeight);
                result.add(newPathCand);
            }

            if ((path.trn < this.maxTurns) || (this.isSameDirection(prevDirection, 'R'))) {
                newPathCand = getPathCandWind(path, 'R', timeStep, turnLoss, posStart, posEnd, tgtHeight);
                result.add(newPathCand);
            }

        } else {

            // step left
            newPathCand = getPathCandWind(path, 'L', timeStep, turnLoss, posStart, posEnd, tgtHeight);
            result.add(newPathCand);

            // step right
            newPathCand = getPathCandWind(path, 'R', timeStep, turnLoss, posStart, posEnd, tgtHeight);
            result.add(newPathCand);

        }

        return result;
    }

    Util.Pair<List<PathCandidate>, List<PathCandidate>> generateCandidate(List<PathCandidate> oldPaths, long timeStep,
            long turnLoss, Position posStart, Position posMiddle, Position posEnd, double tgtHeight) {

        List<PathCandidate> newPathCands;
        List<PathCandidate> leftPaths = new ArrayList<PathCandidate>();
        List<PathCandidate> rightPaths = new ArrayList<PathCandidate>();
        for (PathCandidate curPath : oldPaths) {

            if (curPath.reached) {
                continue;
            }

            newPathCands = this.getPathCandsBeatWind(curPath, timeStep, turnLoss, posStart, posEnd, tgtHeight);
            for (PathCandidate curNewPath : newPathCands) {
                // check whether path is *outside* regatta-area
                double distFromMiddleMeters = posMiddle.getDistance(curPath.pos.getPosition()).getMeters();
                if (distFromMiddleMeters > oobFact * tgtHeight) {
                    continue; // ignore curPath
                }

                if (curNewPath.sid == 'L') {
                    leftPaths.add(curNewPath);
                } else if (curNewPath.sid == 'R') {
                    rightPaths.add(curNewPath);
                }

            }

        }

        Util.Pair<List<PathCandidate>, List<PathCandidate>> newPaths = new Util.Pair<List<PathCandidate>, List<PathCandidate>>(
                leftPaths, rightPaths);
        return newPaths;
    }

    List<PathCandidate> filterCandidates(List<PathCandidate> allCands, double hrzBinWidth) {

        boolean[] filterMap = new boolean[allCands.size()];

        // sort candidates by horizontal distance
        Comparator<PathCandidate> sortHorizontal = new SortPathCandsHorizontally();
        Collections.sort(allCands, sortHorizontal);

        // start scan with index 0
        int idxL = 0;
        int idxR = 0;

        // for each candidate, check the neighborhoods and identify bad candidates
        for (int idx = 0; idx < allCands.size(); idx++) {

            // current horizontal distance
            double hrzDist = allCands.get(idx).hrz;

            // align left index
            while (Math.abs(hrzDist - allCands.get(idxL).hrz) > hrzBinWidth) {
                idxL++;
            }

            // align right index
            boolean finished = false;
            while (!finished && (idxR < (allCands.size() - 1))) {
                if (Math.abs(hrzDist - allCands.get(idxR + 1).hrz) <= hrzBinWidth) {
                    idxR++;
                } else {
                    finished = true;
                }
            }

            // search maximum height
            // in neighborhood idxL, ..., idxR

            // init max for search
            int vrtIdx = idxL;
            double vrtMax = allCands.get(vrtIdx).vrt;
            filterMap[vrtIdx] = false;

            // evaluate remainder of neighborhood
            if (idxL < idxR) {
                for (int jdx = (idxL + 1); jdx <= idxR; jdx++) {
                    if (allCands.get(jdx).vrt > vrtMax) {
                        // reset previous max candidate
                        filterMap[vrtIdx] = true;
                        // keep max height
                        vrtMax = allCands.get(jdx).vrt;
                        // keep max index
                        vrtIdx = jdx;
                        // set current max candidate
                        filterMap[vrtIdx] = false;
                    } else {
                        filterMap[jdx] = true;
                    }
                }
            }

        } // endfor each candidate

        // collect all good candidates (i.e. filterMap == false)
        List<PathCandidate> filterCands = new ArrayList<PathCandidate>();
        for (int idx = 0; idx < allCands.size(); idx++) {
            if (!filterMap[idx]) {
                filterCands.add(allCands.get(idx));
            }
        }

        // return remaining good candidates
        return filterCands;
    }

    List<PathCandidate> filterIsochrone(List<PathCandidate> allCands, double hrzBinWidth) {

        boolean[] filterMap = new boolean[allCands.size()];
        for (int idx = 0; idx < allCands.size(); idx++) {
            filterMap[idx] = true;
        }

        // sort candidates by horizontal distance
        Comparator<PathCandidate> sortHorizontal = new SortPathCandsHorizontally();
        Collections.sort(allCands, sortHorizontal);

        // start scan with index 0
        int idxL = 0;
        int idxR = 0;

        // for each candidate, check the neighborhoods and identify bad candidates
        for (int idx = 0; idx < allCands.size(); idx++) {

            // current horizontal distance
            double hrzDist = allCands.get(idx).hrz;

            // align left index
            while (Math.abs(hrzDist - allCands.get(idxL).hrz) > hrzBinWidth) {
                idxL++;
            }

            // align right index
            boolean finished = false;
            while (!finished && (idxR < (allCands.size() - 1))) {
                if (Math.abs(hrzDist - allCands.get(idxR + 1).hrz) <= hrzBinWidth) {
                    idxR++;
                } else {
                    finished = true;
                }
            }

            // search maximum height
            // in neighborhood idxL, ..., idxR

            // init max for search
            ArrayList<Integer> vrtIdx = new ArrayList<Integer>();
            vrtIdx.add(idxL);
            double vrtMax = allCands.get(idxL).vrt;

            // evaluate remainder of neighborhood
            if (idxL < idxR) {
                for (int jdx = (idxL + 1); jdx <= idxR; jdx++) {
                    if (allCands.get(jdx).vrt > vrtMax) {
                        // keep max height
                        vrtMax = allCands.get(jdx).vrt;
                        // keep max index
                        vrtIdx = new ArrayList<Integer>();
                        vrtIdx.add(jdx);
                    } else if (allCands.get(jdx).vrt == vrtMax) {
                        // add further max indexes
                        vrtIdx.add(jdx);
                    }
                }
            }

            for (Integer jdx : vrtIdx) {
                filterMap[jdx] = false;
            }

        } // endfor each candidate

        // collect all good candidates (i.e. filterMap == false)
        List<PathCandidate> filterCands = new ArrayList<PathCandidate>();
        for (int idx = 0; idx < allCands.size(); idx++) {
            if (!filterMap[idx]) {
                filterCands.add(allCands.get(idx));
            }
        }

        // return remaining good candidates
        return filterCands;
    }

    @Override
    public Path getPath() {
        this.algorithmStartTime = MillisecondsTimePoint.now();
        
        WindFieldGenerator wf = this.parameters.getWindField();
        PolarDiagram pd = this.parameters.getBoatPolarDiagram();

        Position startPos = this.parameters.getCourse().get(0);
        Position endPos = this.parameters.getCourse().get(1);

        // test downwind: exchange start and end
        // Position startPos = this.parameters.getCourse().get(1);
        // Position endPos = this.parameters.getCourse().get(0);

        TimePoint startTime = wf.getStartTime();// new MillisecondsTimePoint(0);
        List<TimedPositionWithSpeed> path = new ArrayList<TimedPositionWithSpeed>();

        Position currentPosition = startPos;
        TimePoint currentTime = startTime;

        Distance distStartEnd = startPos.getDistance(endPos);
        double distStartEndMeters = distStartEnd.getMeters();

        Wind wndStart = wf.getWind(new TimedPositionWithSpeedImpl(startTime, startPos, null));
        logger.fine("wndStart speed:" + wndStart.getKnots() + " angle:" + wndStart.getBearing().getDegrees());
        pd.setWind(wndStart);
        Bearing bearVrt = startPos.getBearingGreatCircle(endPos);
        // Bearing bearHrz = bearVrt.add(new DegreeBearingImpl(90.0));
        Position middlePos = startPos.translateGreatCircle(bearVrt, distStartEnd.scale(0.5));

        String legType = "none";
        if (this.parameters.getLegType() == null) {
            Bearing bearRCWind = wndStart.getBearing().getDifferenceTo(bearVrt);
            legType = "downwind";
            this.upwindLeg = false;
            if ((Math.abs(bearRCWind.getDegrees()) > 90.0) && (Math.abs(bearRCWind.getDegrees()) < 270.0)) {
                legType = "upwind";
                this.upwindLeg = true;
            }
        } else {
            if (this.parameters.getLegType() == LegType.UPWIND) {
                legType = "upwind";
                this.upwindLeg = true;
            } else {
                legType = "downwind";
                this.upwindLeg = false;
            }
        }

        if (debugMsgOn) {
            System.out.println("start : " + startPos.getLatDeg() + ", " + startPos.getLngDeg());
            System.out.println("middle: " + middlePos.getLatDeg() + ", " + middlePos.getLngDeg());
            System.out.println("end   : " + endPos.getLatDeg() + ", " + endPos.getLngDeg());
        }
        logger.fine("Leg Direction: " + legType);

        long turnLoss = pd.getTurnLoss(); // time lost when doing a turn
        if (!this.upwindLeg) {
            turnLoss = turnLoss / 2;
        }
        logger.fine("Turnloss :" + turnLoss);

        if ((this.parameters.getSimuStep() != null) && (this.parameters.getSimuStep().asMillis() > turnLoss + 1000)) {
            this.usedTimeStep = this.parameters.getSimuStep().asMillis();
        } else {
            this.usedTimeStep = turnLoss + 1000; // time-step larger than turn-loss is required (this may be removed by
                                                 // extended handling of turn-loss)
        }
        logger.fine("Time step :" + usedTimeStep);

        // calculate initial position according to initPathStr
        PathCandidate initPath = new PathCandidate(new TimedPositionImpl(currentTime, currentPosition), false, 0.0, 0.0, 0, "0", '0', wndStart, null);
        if (initPathStr.length() > 1) {
            char nextDirection = '0';
            for (int idx = 1; idx < initPathStr.length(); idx++) {
                nextDirection = initPathStr.charAt(idx);
                PathCandidate newPathCand = getPathCandWind(initPath, nextDirection, usedTimeStep, turnLoss, startPos,
                        endPos, distStartEndMeters);
                initPath = newPathCand;
            }
        }
        List<PathCandidate> allPaths = new ArrayList<PathCandidate>();
        List<PathCandidate> trgPaths = new ArrayList<PathCandidate>();
        allPaths.add(initPath);

        TimedPosition tstPosition = this.getStep(new TimedPositionImpl(startTime, startPos), wndStart, usedTimeStep,
                turnLoss, true, 'L');
        double tstDist1 = startPos.getDistance(tstPosition.getPosition()).getMeters();
        tstPosition = this.getStep(new TimedPositionImpl(startTime, startPos), wndStart, usedTimeStep, turnLoss, true,
                'R');
        double tstDist2 = startPos.getDistance(tstPosition.getPosition()).getMeters();

        double hrzBinSize = (tstDist1 + tstDist2) / 6.0; // horizontal bin size in meters
        if (debugMsgOn) {
            System.out.println("Horizontal Bin Size: " + hrzBinSize);
        }

        boolean reachedEnd = false;
        int addSteps = 0;
        int finalSteps = 0; // maximum number of additional steps after first target-path found

        while ((!reachedEnd) || (addSteps < finalSteps)) {

            if (reachedEnd) {
                addSteps++;
            }

            // generate new candidates (inside regatta-area)
            Util.Pair<List<PathCandidate>, List<PathCandidate>> newPaths = this.generateCandidate(allPaths,
                    usedTimeStep, turnLoss, startPos, middlePos, endPos, distStartEndMeters);

            // select good candidates
            List<PathCandidate> leftPaths = this.filterCandidates(newPaths.getA(), hrzBinSize / 2.0);
            List<PathCandidate> rightPaths = this.filterCandidates(newPaths.getB(), hrzBinSize / 2.0);

            List<PathCandidate> nextPaths = new ArrayList<PathCandidate>();
            nextPaths.addAll(leftPaths);
            nextPaths.addAll(rightPaths);

            allPaths = nextPaths;

            if (this.gridStore) {

                /*
                 * ArrayList<TimedPosition> isoChrone = new ArrayList<TimedPosition>(); for(PathCand curCand : allPaths)
                 * { isoChrone.add(curCand.pos); } this.gridPositions.add(isoChrone);
                 */

                this.gridPositions.add(allPaths);

                List<PathCandidate> isocPaths = this.filterIsochrone(allPaths, hrzBinSize);
                this.isocPositions.add(isocPaths);

            }
            
            // check if there are still paths in the regatta-area
            if (allPaths.size() > 0) {

                for (PathCandidate curPath : allPaths) {
                    // terminate path-search if paths are found that are close enough to target
                    // if ((curPath.vrt > distStartEndMeters)) {
                    if (curPath.reached) {
                        // logger.fine("\ntPath: " + curPath.path + "\n      Time: " +
                        // (Math.round((curPath.pos.getTimePoint().asMillis()-startTime.asMillis())/1000.0/60.0*10.0)/10.0)+", Height: "+curPath.vrt+" of "+(Math.round(startPos.getDistance(endPos).getMeters()*100.0)/100.0)+", Dist: "+curPath.hrz+"m ~ "+(Math.round(curPath.pos.getPosition().getDistance(endPos).getMeters()*100.0)/100.0)+"m");
                        int curBin = (int) Math.round(Math.floor((curPath.hrz + hrzBinSize / 2.0) / hrzBinSize));
                        if ((Math.abs(curBin) <= 4)) {
                            reachedEnd = true;
                            trgPaths.add(curPath); // add path to list of target-paths
                        }
                    }
                }

            } else {
                // terminate path-search as no path inside regatta-area are left
                reachedEnd = true;
            }

            // check for time-out
            if (this.isTimedOut()) {
                reachedEnd = true;
            }
            
        } // main while-loop

        if (this.gridStore) {

            double distResolution = distStartEndMeters * 0.01;
            BufferedWriter outputCSV;
            try {
                outputCSV = new BufferedWriter(new FileWriter(this.gridFile + "-grid.csv"));
                outputCSV.write("step; lat; lng; time; side; path; vrt\n");
                outputCSV.write("0; " + startPos.getLatDeg() + "; " + startPos.getLngDeg() + "; "
                        + (startTime.asMillis() / 1000) + "; 0; 0; " + (-distStartEndMeters) + "\n");
                outputCSV.write("0; " + endPos.getLatDeg() + "; " + endPos.getLngDeg() + "; "
                        + (startTime.asMillis() / 1000) + "; 0; 0; 0\n");
                int stepCount = 0;
                for (List<PathCandidate> isoChrone : this.gridPositions) {
                    stepCount++;
                    PathCandidate prevPos = null;
                    for (PathCandidate isoPos : isoChrone) {

                        if (prevPos != null) {
                            if (prevPos.pos.getPosition().getDistance(isoPos.pos.getPosition()).getMeters() < distResolution) {
                                continue;
                            }
                        }

                        String outStr = "" + stepCount + "; " + isoPos.pos.getPosition().getLatDeg() + "; "
                                + isoPos.pos.getPosition().getLngDeg() + "; "
                                + (isoPos.pos.getTimePoint().asMillis() / 1000) + "; " + isoPos.sid;
                        outStr += "; " + isoPos.path + "; " + isoPos.vrt;
                        outStr += "\n";
                        outputCSV.write(outStr);

                        prevPos = isoPos;
                    }
                }
                outputCSV.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                outputCSV = new BufferedWriter(new FileWriter(this.gridFile + "-isoc.csv"));
                outputCSV.write("step; lat; lng; time; side; path; vrt\n");
                outputCSV.write("0; " + startPos.getLatDeg() + "; " + startPos.getLngDeg() + "; "
                        + (startTime.asMillis() / 1000) + "; 0; 0; " + (-distStartEndMeters) + "\n");
                outputCSV.write("0; " + endPos.getLatDeg() + "; " + endPos.getLngDeg() + "; "
                        + (startTime.asMillis() / 1000) + "; 0; 0; 0\n");
                int stepCount = 0;
                for (List<PathCandidate> isoChrone : this.isocPositions) {
                    stepCount++;
                    PathCandidate prevPos = null;
                    for (PathCandidate isoPos : isoChrone) {

                        if (prevPos != null) {
                            if (prevPos.pos.getPosition().getDistance(isoPos.pos.getPosition()).getMeters() < distResolution) {
                                continue;
                            }
                        }

                        String outStr = "" + stepCount + "; " + isoPos.pos.getPosition().getLatDeg() + "; "
                                + isoPos.pos.getPosition().getLngDeg() + "; "
                                + (isoPos.pos.getTimePoint().asMillis() / 1000) + "; " + isoPos.sid;
                        outStr += "; " + isoPos.path + "; " + isoPos.vrt;
                        outStr += "\n";
                        outputCSV.write(outStr);

                        prevPos = isoPos;
                    }
                }
                outputCSV.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        // if no target-paths were found, return empty path
        if (trgPaths.size() == 0) {
            // trgPaths = allPaths; // TODO: only for testing; remove lateron
            TimedPositionWithSpeed curPosition = new TimedPositionWithSpeedImpl(startTime, startPos, null);
            path.add(curPosition);
            return new PathImpl(path, wf, true /* out-of-bounds spatially||timely */); // return empty path
        }

        // sort target-paths ascending by distance-to-target
        Collections.sort(trgPaths);

        // debug output
        for (PathCandidate curPath : trgPaths) {
            logger.fine("\nPath: " + curPath.path + "\n      Time: "
                    + (curPath.pos.getTimePoint().asMillis() - startTime.asMillis()) + ", Height: " + curPath.vrt
                    + " of " + (Math.round(startPos.getDistance(endPos).getMeters() * 100.0) / 100.0) + ", Dist: "
                    + curPath.hrz + "m ~ "
                    + (Math.round(curPath.pos.getPosition().getDistance(endPos).getMeters() * 100.0) / 100.0) + "m");
            // System.out.print(""+curPath.path+": "+curPath.pos.getTimePoint().asMillis()+", "+curPath.pos.getPosition().getLatDeg()+", "+curPath.pos.getPosition().getLngDeg()+", ");
            // System.out.println(" height:"+curPath.vrt+" of "+startPos.getDistance(endPos).getMeters()+", dist:"+curPath.hrz+" ~ "+curPath.pos.getPosition().getDistance(endPos));
        }

        //
        // fill gwt-path
        //

        // generate intermediate steps
        bestCand = trgPaths.get(0); // target-path ending closest to target
        long endTime = bestCand.pos.getTimePoint().asMillis();
        TimedPositionWithSpeed curPosition = null;
        char nextDirection = '0';
        char prevDirection = '0';
        for (int step = 0; step < (bestCand.path.length() - 1); step++) {

            nextDirection = bestCand.path.charAt(step);

            if (nextDirection == '0') {

                curPosition = new TimedPositionWithSpeedImpl(startTime, startPos, null);
                path.add(curPosition);

            } else {

                boolean sameBaseDirection = this.isSameDirection(prevDirection, nextDirection);
                Wind curWind = wf.getWind(curPosition);
                TimedPosition newPosition = this.getStep(curPosition, curWind, usedTimeStep, turnLoss,
                        sameBaseDirection, nextDirection);
                if (newPosition.getTimePoint().asMillis() < endTime) {
                    curPosition = new TimedPositionWithSpeedImpl(newPosition.getTimePoint(), newPosition.getPosition(),
                            null);
                    path.add(curPosition);
                }
            }

            prevDirection = nextDirection;
        }

        // add final position (rescaled before to end on height of target)
        path.add(new TimedPositionWithSpeedImpl(bestCand.pos.getTimePoint(), bestCand.pos.getPosition(), null));

        // maximum turn time for one-turner simulation, otherwise zero
        long maxTurnTime = 0;
        if (this.maxTurns == 1) {
            int turnMiddle = 1000;
            if (bestCand != null) {
                if (bestCand.path.charAt(1) == 'L') {
                    turnMiddle = bestCand.getIndexOfTurnLR();
                } else {
                    turnMiddle = bestCand.getIndexOfTurnRL();
                }
                maxTurnTime = turnMiddle * this.usedTimeStep;
            }
        }
        return new PathImpl(path, wf, maxTurnTime, this.algorithmTimedOut, false);
    }

}
