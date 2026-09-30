package com.sap.sailing.simulator.impl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PointOfSail;
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
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PathGeneratorTreeGrow360 extends PathGeneratorBase {

    private static final Logger logger = Logger.getLogger(com.sap.sailing.simulator.impl.PathGeneratorTreeGrow360.class.getName());
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
    Distance endLineWidth = null;
    int endMatchCriterion;

    public PathGeneratorTreeGrow360(SimulationParameters params) {
        PolarDiagram polarDiagramClone = new PolarDiagramBase((PolarDiagramBase)params.getBoatPolarDiagram());
        this.parameters = new SimulationParametersImpl(params.getCourse(), params.getStartLine(), params.getEndLine(), polarDiagramClone, params.getWindField(),
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
    TimedPosition getStep(TimedPosition pos, Wind posWind, Position posEnd, long timeStep, long turnLoss, boolean sameBaseDirection,
            char nextDirection) throws SparseSimulationDataException {

        TimePoint curTime = pos.getTimePoint();
        Position curPosition = pos.getPosition();

        PolarDiagram polarDiagram = this.parameters.getBoatPolarDiagram();
        polarDiagram.setWind(posWind);

        // get beat-angle left and right
        Bearing travelBearing = null;
        SpeedWithBearing travelSpeed = null;
        if (nextDirection == 'L') {
            travelBearing = polarDiagram.optimalDirectionsUpwind()[0];
            travelSpeed = polarDiagram.getSpeedAtBearing(travelBearing);
        }
        if (nextDirection == 'r') {
            travelBearing = polarDiagram.optimalDirectionsDownwind()[0];
            travelSpeed = polarDiagram.getSpeedAtBearing(travelBearing);
        }
        if (nextDirection == 'R') {
            travelBearing = polarDiagram.optimalDirectionsUpwind()[1];
            travelSpeed = polarDiagram.getSpeedAtBearing(travelBearing);
        }
        if (nextDirection == 'l') {
            travelBearing = polarDiagram.optimalDirectionsDownwind()[1];
            travelSpeed = polarDiagram.getSpeedAtBearing(travelBearing);
        }
        if ((nextDirection == 'D')||(nextDirection == 'E')) {
            travelBearing = curPosition.getBearingGreatCircle(posEnd);
            if (polarDiagram.hasCurrent()) {
                travelSpeed = polarDiagram.getSpeedAtBearingOverGround(travelBearing);
            } else {
                travelSpeed = polarDiagram.getSpeedAtBearing(travelBearing);
            }
        }
        
        if ((travelBearing == null)||(travelSpeed == null)) {
            if (travelBearing == null) {
                logger.severe("Travel Bearing for NextDirection '" + nextDirection + "' is NULL. This must NOT happen.");
            }
            if (travelSpeed == null) {
                logger.severe("Travel Speed for NextDirection '" + nextDirection + "' is NULL. This must NOT happen.");
            }
            throw new SparseSimulationDataException();
        }     

        if ((travelSpeed.getKnots() == 0)&&(!polarDiagram.hasCurrent())) {
            logger.severe("Travel Speed for NextDirection '" + nextDirection + "' is ZERO. This must NOT happen.");            
            throw new SparseSimulationDataException();
        }
        
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
        char tmpPrevDirection = getBaseDirection(prevDirection);
        char tmpNextDirection = getBaseDirection(nextDirection);
        return ((tmpNextDirection == tmpPrevDirection) || (tmpPrevDirection == '0'));
    }

    char getOppositeDirection(char direction) {
        char oppositeDirection = ' ';
        switch (direction) {
        case 'L':
            oppositeDirection = 'R';
            break;
        case 'R':
            oppositeDirection = 'L';
            break;
        case 'l':
            oppositeDirection = 'r';
            break;
        case 'r':
            oppositeDirection = 'l';
            break;
        }
        return oppositeDirection;
    }
    
    
    char getBaseDirection(char direction) {
        char tmpDirection = direction;
        return (tmpDirection=='D'?'L':(tmpDirection=='E'?'R':(tmpDirection=='l'?'L':(tmpDirection=='r'?'R':tmpDirection))));
    }
    
    // get path candidate measuring height towards (local, current-apparent) wind
    PathCandidate getPathCandWind(PathCandidate path, char nextDirection, long timeStep, long turnLoss,
            Position posStart, Position posEnd, double tgtHeight) throws SparseSimulationDataException {

        char prevDirection = path.path.charAt(path.path.length() - 1);
        boolean sameBaseDirection = this.isSameDirection(prevDirection, nextDirection);

        int turnCount = path.trn;
        if (!sameBaseDirection) {
            turnCount++;
        }

        // calculate next path position (taking turn-loss into account)
        TimedPosition pathPos = this.getStep(path.pos, path.wind, posEnd, timeStep, turnLoss, sameBaseDirection, nextDirection);

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

        return (new PathCandidate(pathPos, reachedEnd, vrtDist, hrzDist, turnCount, pathStr, getBaseDirection(nextDirection), posWind, path.start));
    }

    // generate path candidates based on bearing to target
    List<PathCandidate> getPathCandsBeatWind(PathCandidate path, long timeStep, long turnLoss, Position posStart,
            Position posEnd, double tgtHeight) throws SparseSimulationDataException {
        
        // determine bearing of target
        Bearing bearTarget = path.pos.getPosition().getBearingGreatCircle(posEnd);
        PolarDiagram polarDiagram = this.parameters.getBoatPolarDiagram();
        polarDiagram.setWind(path.wind);
        // compare target bearing to upwind bearings
        Bearing[] bearOptimalUpwind = polarDiagram.optimalDirectionsUpwind();
        Bearing upwindLeftRight = bearOptimalUpwind[0].getDifferenceTo(bearOptimalUpwind[1]);
        Bearing upwindLeftTarget = bearOptimalUpwind[0].getDifferenceTo(bearTarget);
        PointOfSail pointOfSail = PointOfSail.REACHING;
        char reachingSide = ' ';
        // check whether boat is in "tacking area"
        if ((upwindLeftTarget.getDegrees() >= -1) && (upwindLeftTarget.getDegrees() <= upwindLeftRight.getDegrees()+1)) {
            logger.finest("point-of-sail: tacking (diffLeftTarget: " + upwindLeftTarget.getDegrees() + ", diffLeftRight: "
                    + upwindLeftRight.getDegrees() + ", " + path.path + ")");
            pointOfSail = PointOfSail.TACKING;
        } else {
            Bearing[] bearOptimalDownwind = polarDiagram.optimalDirectionsDownwind();
            Bearing downwindLeftRight = bearOptimalDownwind[0].getDifferenceTo(bearOptimalDownwind[1]);
            Bearing downwindLeftTarget = bearOptimalDownwind[0].getDifferenceTo(bearTarget);
            // check whether boat is in "non-sailable area"
            if ((downwindLeftTarget.getDegrees() >= -1) && (downwindLeftTarget.getDegrees() <= downwindLeftRight.getDegrees()+1)) {
                logger.finest("point-of-sail: jibing (diffLeftTarget: " + downwindLeftTarget.getDegrees()
                        + ", diffLeftRight: " + downwindLeftRight.getDegrees() + ", " + path.path + ")");
                pointOfSail = PointOfSail.JIBING;
            } else {
                Bearing windBoat = path.wind.getBearing().getDifferenceTo(bearTarget);
                if (windBoat.getDegrees() > 0) {
                    reachingSide = 'D'; // left-sided reaching
                } else {
                    reachingSide = 'E'; // right-sided reaching
                }
            }
        }
        
        List<PathCandidate> result = new ArrayList<PathCandidate>();
        PathCandidate newPathCand;

        if (this.maxTurns > 0) {

            char prevDirection = path.path.charAt(path.path.length() - 1);
            
            if (pointOfSail == PointOfSail.TACKING) {
                // left step
                if ((path.trn < this.maxTurns) || (this.isSameDirection(prevDirection, 'L'))) {
                    newPathCand = getPathCandWind(path, 'L', timeStep, turnLoss, posStart, posEnd, tgtHeight);
                    result.add(newPathCand);
                }
                // right step
                if ((path.trn < this.maxTurns) || (this.isSameDirection(prevDirection, 'R'))) {
                    newPathCand = getPathCandWind(path, 'R', timeStep, turnLoss, posStart, posEnd, tgtHeight);
                    result.add(newPathCand);
                }
            }

            if (pointOfSail == PointOfSail.JIBING) {
                // left step
                if ((path.trn < this.maxTurns) || (this.isSameDirection(prevDirection, 'l'))) {
                    newPathCand = getPathCandWind(path, 'l', timeStep, turnLoss, posStart, posEnd, tgtHeight);
                    result.add(newPathCand);
                }
                // right step
                if ((path.trn < this.maxTurns) || (this.isSameDirection(prevDirection, 'r'))) {
                    newPathCand = getPathCandWind(path, 'r', timeStep, turnLoss, posStart, posEnd, tgtHeight);
                    result.add(newPathCand);
                }
            }

            if (pointOfSail == PointOfSail.REACHING) {
                // direct step (to target)
                if ((path.trn < this.maxTurns) || (this.isSameDirection(prevDirection, reachingSide))) {
                    newPathCand = getPathCandWind(path, reachingSide, timeStep, turnLoss, posStart, posEnd, tgtHeight);
                    result.add(newPathCand);
                }
                // continuing step (may be required to reach target at all)
                if ((prevDirection != reachingSide) && (prevDirection != '0')) {
                    newPathCand = getPathCandWind(path, prevDirection, timeStep, turnLoss, posStart, posEnd, tgtHeight);
                    result.add(newPathCand);
                }
                // opposite step (in order to be accurate for symmetric cases)
                char oppositeDirection = this.getOppositeDirection(prevDirection);
                if ((path.trn < this.maxTurns) && (oppositeDirection != ' ') && (prevDirection != '0')) {
                    newPathCand = getPathCandWind(path, oppositeDirection, timeStep, turnLoss, posStart, posEnd, tgtHeight);
                    result.add(newPathCand);
                }
            }

        } else {

            if (pointOfSail == PointOfSail.TACKING) {
                // left step
                newPathCand = getPathCandWind(path, 'L', timeStep, turnLoss, posStart, posEnd, tgtHeight);
                result.add(newPathCand);
                // right step
                newPathCand = getPathCandWind(path, 'R', timeStep, turnLoss, posStart, posEnd, tgtHeight);
                result.add(newPathCand);
            }

            if (pointOfSail == PointOfSail.JIBING) {
                // left step
                newPathCand = getPathCandWind(path, 'l', timeStep, turnLoss, posStart, posEnd, tgtHeight);
                result.add(newPathCand);
                // right step
                newPathCand = getPathCandWind(path, 'r', timeStep, turnLoss, posStart, posEnd, tgtHeight);
                result.add(newPathCand);
            }

            if (pointOfSail == PointOfSail.REACHING) {
                // direct step (to target)
                newPathCand = getPathCandWind(path, reachingSide, timeStep, turnLoss, posStart, posEnd, tgtHeight);
                result.add(newPathCand);
                // continuing step (may be required to reach target at all)
                char prevDirection = path.path.charAt(path.path.length() - 1);
                if ((prevDirection != reachingSide) && (prevDirection != '0')) {
                    newPathCand = getPathCandWind(path, prevDirection, timeStep, turnLoss, posStart, posEnd, tgtHeight);
                    result.add(newPathCand);
                }
                // opposite step (in order to be accurate for symmetric cases)
                char oppositeDirection = this.getOppositeDirection(prevDirection);
                if ((oppositeDirection != ' ') && (prevDirection != '0')) {
                    newPathCand = getPathCandWind(path, oppositeDirection, timeStep, turnLoss, posStart, posEnd, tgtHeight);
                    result.add(newPathCand);
                }
            }

        }

        return result;
    }

    Util.Pair<List<PathCandidate>, List<PathCandidate>> generateCandidate(List<PathCandidate> oldPaths, long timeStep,
            long turnLoss, Position posStart, Position posMiddle, Position posEnd, double tgtHeight) throws SparseSimulationDataException {

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
    public Path getPath() throws SparseSimulationDataException {
        this.algorithmStartTime = MillisecondsTimePoint.now();
        
        WindFieldGenerator wf = this.parameters.getWindField();
        PolarDiagram polarDiagram = this.parameters.getBoatPolarDiagram();

        Position startPos = this.parameters.getCourse().get(0);
        Position endPos = this.parameters.getCourse().get(1);
        Bearing bearVrt = startPos.getBearingGreatCircle(endPos);

        List<Position> endLine = this.parameters.getEndLine();
        if (endLine != null) {
            Bearing bearEndLine = endLine.get(0).getBearingGreatCircle(endLine.get(1));
            double diffEndLineVrt = bearVrt.getDifferenceTo(bearEndLine).getDegrees();
            Position endPositionLeft;
            Position endPositionRight;
            if ((diffEndLineVrt > 0) && (diffEndLineVrt < 180)) {
                endPositionLeft = endLine.get(0);
                endPositionRight = endLine.get(1);
            } else {
                endPositionLeft = endLine.get(1);
                endPositionRight = endLine.get(0);
            }
            if (initPathStr.length() > 1) {
                if (initPathStr.charAt(1) == 'L') {
                    endPos = endPositionLeft;
                } else {
                    endPos = endPositionRight;
                }
                endLineWidth = null;
            } else {
                endLineWidth = endPositionLeft.getDistance(endPositionRight);
            }
            bearVrt = startPos.getBearingGreatCircle(endPos);
        }
        
        TimePoint startTime = wf.getStartTime();// new MillisecondsTimePoint(0);
        List<TimedPositionWithSpeed> path = new ArrayList<TimedPositionWithSpeed>();

        Position currentPosition = startPos;
        TimePoint currentTime = startTime;

        Distance distStartEnd = startPos.getDistance(endPos);
        double distStartEndMeters = distStartEnd.getMeters();

        Wind wndStart = wf.getWind(new TimedPositionWithSpeedImpl(startTime, startPos, null));
        logger.finest("wndStart speed:" + wndStart.getKnots() + " angle:" + wndStart.getBearing().getDegrees());
        polarDiagram.setWind(wndStart);
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

        long turnLoss = polarDiagram.getTurnLoss(); // time lost when doing a turn
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
        List<PathCandidate> initPaths = new ArrayList<PathCandidate>();
        List<PathCandidate> allPaths = new ArrayList<PathCandidate>();
        List<PathCandidate> trgPaths = new ArrayList<PathCandidate>();
        List<Position> startLine = this.parameters.getStartLine();
        
        // generate start-line for testing
        /*Bearing bearRightToStart = startPos.getBearingGreatCircle(endPos).add(new DegreeBearingImpl(80));        
        Position exampleLineRight = startPos.translateGreatCircle(bearRightToStart, new MeterDistance(70.0));
        Position exampleLineLeft = startPos.translateGreatCircle(bearRightToStart.reverse(), new MeterDistance(70.0));
        startLine = new ArrayList<Position>();
        startLine.add(exampleLineLeft);
        startLine.add(exampleLineRight);*/
        
        // check if start-line has two marks as expected; if not fall back to start-position
        if ((startLine != null) && (startLine.size() > 2)) {
            startLine = null;
        }
        if (startLine == null) {
            // initialize with a single path from start position
            PathCandidate initPath = new PathCandidate(new TimedPositionImpl(currentTime, currentPosition), false, 0.0, 0.0, 0, "0", '0', wndStart, startPos);
            initPaths.add(initPath);
        } else {
            Bearing bearLine = startLine.get(0).getBearingGreatCircle(startLine.get(1));
            double diffLineVrt = bearVrt.getDifferenceTo(bearLine).getDegrees();
            Position startPositionLeft;
            Position startPositionRight;
            if ((diffLineVrt > 0) && (diffLineVrt < 180)) {
                startPositionLeft = startLine.get(0);
                startPositionRight = startLine.get(1);
            } else {
                startPositionLeft = startLine.get(1);
                startPositionRight = startLine.get(0);                
            }
            // initialize with a multiple paths along the start line
            if (startLine.size() == 2) {
                if ((this.maxTurns == 1) && (initPathStr.length() > 1)) {
                    PathCandidate initPath;
                    if (initPathStr.charAt(1) == 'L') {
                        initPath = new PathCandidate(new TimedPositionImpl(currentTime, startPositionLeft), false, 0.0, 0.0, 0, "0", '0', wndStart, startPositionLeft);
                    } else {
                        initPath = new PathCandidate(new TimedPositionImpl(currentTime, startPositionRight), false, 0.0, 0.0, 0, "0", '0', wndStart, startPositionRight);
                    }
                    initPaths.add(initPath);
                } else {
                    Bearing bearStartLine = startPositionLeft.getBearingGreatCircle(startPositionRight);
                    int nParts = 10;
                    for(int idx=0; idx<=nParts; idx++) {
                        Distance deltaStartLine = startPositionLeft.getDistance(startPositionRight).scale(((double)idx)/nParts);
                        Position tmpPosition = startPositionLeft.translateGreatCircle(bearStartLine, deltaStartLine);
                        Wind tmpWind = wf.getWind(new TimedPositionWithSpeedImpl(currentTime, tmpPosition, null));
                        PathCandidate initPath = new PathCandidate(new TimedPositionImpl(currentTime, tmpPosition), false, 0.0, 0.0, 0, "0", '0', tmpWind, tmpPosition);
                        initPaths.add(initPath);
                    }
                }
            }
        }
        // construct initialization steps, e.g. first left-going step for left-going 1-turner
        if (initPathStr.length() > 1) {
            String initPathStrCaps = initPathStr.toUpperCase();
            if (!this.upwindLeg) {
                initPathStrCaps = initPathStrCaps.replace('L', 'r');
                initPathStrCaps = initPathStrCaps.replace('R', 'l');
            }
            for (PathCandidate cand : initPaths) {
                char nextDirection = '0';
                PathCandidate tmpCand1 = cand;
                for (int idx = 1; idx < initPathStrCaps.length(); idx++) {
                    nextDirection = initPathStrCaps.charAt(idx);
                    tmpCand1 = getPathCandWind(tmpCand1, nextDirection, usedTimeStep, turnLoss, startPos, endPos, distStartEndMeters);
                }
                allPaths.add(tmpCand1);
            }
        } else {
            allPaths = initPaths;
        }
        
        TimedPosition tstPosition = this.getStep(new TimedPositionImpl(startTime, startPos), wndStart, endPos, usedTimeStep, turnLoss, true, (this.upwindLeg?'L':'l'));
        double tstDist1 = startPos.getDistance(tstPosition.getPosition()).getMeters();
        tstPosition = this.getStep(new TimedPositionImpl(startTime, startPos), wndStart, endPos, usedTimeStep, turnLoss, true, (this.upwindLeg?'R':'r'));
        double tstDist2 = startPos.getDistance(tstPosition.getPosition()).getMeters();

        double hrzBinSize = (tstDist1 + tstDist2) / 6.0; // horizontal bin size in meters
        if (debugMsgOn) {
            System.out.println("Horizontal Bin Size: " + hrzBinSize);
        }
        
        if (endLineWidth != null) {
            endMatchCriterion = ((int)Math.round(endLineWidth.getMeters() / hrzBinSize)) / 2;
        } else {
            endMatchCriterion = 0;
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
                        if ((Math.abs(curBin) <= endMatchCriterion)) {
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
            return new PathImpl(path, wf, true /* out-of-bounds spatially||timely */, false /* mixed leg */); // return empty path
        }

        // sort target-paths ascending by distance-to-target
        Collections.sort(trgPaths);

        // debug output
        for (PathCandidate curPath : trgPaths) {
            logger.finest("\nPath: " + curPath.path + " (" + curPath.trn + ")\n      Time: "
                    + (curPath.pos.getTimePoint().asMillis() - startTime.asMillis()) + ", Height: " + curPath.vrt
                    + " of " + (Math.round(startPos.getDistance(endPos).getMeters() * 100.0) / 100.0) + ", Dist: "
                    + curPath.hrz + "m ~ "
                    + (Math.round(curPath.pos.getPosition().getDistance(endPos).getMeters() * 100.0) / 100.0) + "m");
        }

        //
        // reconstruct best path
        //
        bestCand = trgPaths.get(0); // target-path ending closest to target
        long endTime = bestCand.pos.getTimePoint().asMillis();
        TimedPositionWithSpeed curPosition = null;
        char nextDirection = '0';
        char prevDirection = '0';
        for (int step = 0; step < (bestCand.path.length() - 1); step++) {

            nextDirection = bestCand.path.charAt(step);

            if (nextDirection == '0') {

                curPosition = new TimedPositionWithSpeedImpl(startTime, bestCand.start, null);
                path.add(curPosition);

            } else {

                boolean sameBaseDirection = this.isSameDirection(prevDirection, nextDirection);
                Wind curWind = wf.getWind(curPosition);
                TimedPosition newPosition = this.getStep(curPosition, curWind, endPos, usedTimeStep, turnLoss,
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

        // identify type of course
        boolean containsTacks = bestCand.path.contains("L")||bestCand.path.contains("R");
        boolean containsJibes = bestCand.path.contains("l")||bestCand.path.contains("r");
        
        
        // maximum turn time for one-turner simulation, otherwise zero
        long maxTurnTime = 0;
        if (this.maxTurns == 1) {
            int turnMiddle = 1000;
            if (bestCand != null) {
                if (bestCand.path.charAt(1) == (this.upwindLeg?'L':'l')) {
                    turnMiddle = bestCand.getIndexOfTurnLR();
                } else {
                    turnMiddle = bestCand.getIndexOfTurnRL();
                }
                maxTurnTime = turnMiddle * this.usedTimeStep;
            }
        }
        
        // logging information about best found course
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(bestCand.pos.getTimePoint().asMillis() - startTime.asMillis());
        SimpleDateFormat racetimeFormat = new SimpleDateFormat("HH:mm:ss:SSS");
        racetimeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String racetimeFormatted = racetimeFormat.format(cal.getTime());
        logger.fine("Start Condition: " + this.initPathStr + "\nPath: " + bestCand.path +"\n      Time: "
                +  racetimeFormatted + ", Distance: "
                + String.format("%.2f", Math.round(bestCand.pos.getPosition().getDistance(endPos).getMeters() * 100.0) / 100.0) + " meters"
                + ", " + bestCand.trn + " Turn" + (bestCand.trn>1?"s":""));
        
        return new PathImpl(path, wf, maxTurnTime, this.algorithmTimedOut, containsTacks&&containsJibes&&(this.maxTurns==1));
    }

}
