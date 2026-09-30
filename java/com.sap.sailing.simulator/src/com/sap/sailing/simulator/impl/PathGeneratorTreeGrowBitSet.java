package com.sap.sailing.simulator.impl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
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
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PathGeneratorTreeGrowBitSet extends PathGeneratorBase {

    private static Logger logger = Logger.getLogger("com.sap.sailing");
    private boolean debugMsgOn = false;

    double oobFact = 2.0; // out-of-bounds factor
    int maxTurns = 0;
    boolean upwindLeg = false;
    String initPathStr = "0";
    PathCandidateBitSet bestCand = null;
    long usedTimeStep = 0;
    boolean gridStore = false;
    ArrayList<List<PathCandidateBitSet>> gridPositions = null;
    ArrayList<List<PathCandidateBitSet>> isocPositions = null;
    String gridFile = null;
    final static boolean LEFT = false;
    final static boolean RIGHT = true;

    public PathGeneratorTreeGrowBitSet(SimulationParameters params) {
        this.parameters = params;
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
            this.gridPositions = new ArrayList<List<PathCandidateBitSet>>();
            this.isocPositions = new ArrayList<List<PathCandidateBitSet>>();
        } else {
            this.gridStore = false;
            this.gridPositions = null;
            this.isocPositions = null;
        }
    }

    class SortPathCandsAbsHorizontally implements Comparator<PathCandidateBitSet> {

        @Override
        public int compare(PathCandidateBitSet p1, PathCandidateBitSet p2) {
            if (Math.abs(p1.hrz) == Math.abs(p2.hrz)) {
                return 0;
            } else {
                return (Math.abs(p1.hrz) < Math.abs(p2.hrz) ? -1 : +1);
            }
        }

    }

    class SortPathCandsHorizontally implements Comparator<PathCandidateBitSet> {

        @Override
        public int compare(PathCandidateBitSet p1, PathCandidateBitSet p2) {
            if (p1.hrz == p2.hrz) {
                return 0;
            } else {
                return (p1.hrz < p2.hrz ? -1 : +1);
            } 
        }

    }

    // getter for evaluating best path cand propoerties further
    PathCandidateBitSet getBestCand() {
        return this.bestCand;
    }
    
    long getUsedTimeStep() {
    	return this.usedTimeStep;
    }


    // generate step in one of the possible directions
    // default: L - left, R - right
    TimedPosition getStep(TimedPosition pos, long timeStep, long turnLoss, boolean sameBaseDirection, boolean nextDirection) {

        WindFieldGenerator wf = this.parameters.getWindField();
        TimePoint curTime = pos.getTimePoint();
        Position curPosition = pos.getPosition();
        Wind posWind = wf.getWind(new TimedPositionImpl(curTime, curPosition));

        PolarDiagram pd = this.parameters.getBoatPolarDiagram();
        pd.setWind(posWind);

        // get beat-angle left and right
        Bearing travelBearing = null;
        if (nextDirection == LEFT) {
        	if (this.upwindLeg) {
        		travelBearing = pd.optimalDirectionsUpwind()[0];
        	} else {
        		travelBearing = pd.optimalDirectionsDownwind()[0];        		
        	}
        } else if (nextDirection == RIGHT) {
        	if (this.upwindLeg) {
        		travelBearing = pd.optimalDirectionsUpwind()[1];
        	} else {
        		travelBearing = pd.optimalDirectionsDownwind()[1];        		
        	}
        }

        // determine beat-speed left and right
        SpeedWithBearing travelSpeed = pd.getSpeedAtBearing(travelBearing);

        TimePoint travelTime;
        TimePoint nextTime = new MillisecondsTimePoint(curTime.asMillis()+timeStep);
        if (sameBaseDirection) {
            travelTime = nextTime;
        } else {
            travelTime = new MillisecondsTimePoint(nextTime.asMillis() - turnLoss);
        }

        Position nextPosition = travelSpeed.travelTo(curPosition, curTime, travelTime);
        return new TimedPositionImpl(nextTime, nextPosition);
    }

    // check whether nextDirection is same base direction as previous direction, i.e. no turn
    boolean isSameDirection(int length, boolean prevDirection, boolean nextDirection) {
        return ((nextDirection == prevDirection)||(length <= 1));
    }

    // get path candidate measuring height towards (local, current-apparent) wind
    PathCandidateBitSet getPathCandWind(PathCandidateBitSet path, boolean nextDirection, long timeStep, long turnLoss, Position posStart, Position posEnd, double tgtHeight) {

        boolean prevDirection = path.path.get(path.length-1);
        boolean sameBaseDirection = this.isSameDirection(path.length, prevDirection, nextDirection);

        int turnCount = path.trn;
        if (!sameBaseDirection) {
            turnCount++;
        }

        // calculate next path position (taking turn-loss into account)
        TimedPosition pathPos = this.getStep(path.pos, timeStep, turnLoss, sameBaseDirection, nextDirection);
        
        // determine apparent wind at next path position & time
        Wind posWind = this.parameters.getWindField().getWind(pathPos);
        PolarDiagram pd = this.parameters.getBoatPolarDiagram();
        pd.setWind(posWind);
        Wind appWind = new WindImpl(posWind.getPosition(), posWind.getTimePoint(), pd.getWind());

        // calculate height-position with reference to apparent wind
        Position posHeight = pathPos.getPosition().projectToLineThrough(posEnd, appWind.getBearing());

        // calculate vertical distance as distance of height-position to end
        Bearing bearHeight = posEnd.getBearingGreatCircle(posHeight);
        double bearHeightSide = appWind.getBearing().getDifferenceTo(bearHeight).getDegrees();
        double vrtSide = (this.upwindLeg ? -1.0 : +1.0);
        if (Math.abs(bearHeightSide) > 170.0) {
            vrtSide = (this.upwindLeg ? +1.0 : -1.0);
        }
        double vrtDist = vrtSide*Math.round(posHeight.getDistance(posEnd).getMeters()*1000.0)/1000.0;

        // scale last step to exactly reach height of posEnd (in reference to appWind) and adjust time correspondingly
        boolean reachedEnd = false;
        if ((!path.reached) && (vrtDist > 0.0)) {
            // scale last step so that vrtDist ~ tgtHeight
            Position prevPos = path.pos.getPosition();
            TimePoint prevTime = path.pos.getTimePoint();
            double heightFrac = path.vrt / (path.vrt - vrtDist);
            Position newPos = prevPos.translateGreatCircle(prevPos.getBearingGreatCircle(pathPos.getPosition()), prevPos.getDistance(pathPos.getPosition()).scale(heightFrac));
            long newTimeMillis = Math.round((prevTime.asMillis() + (pathPos.getTimePoint().asMillis() - prevTime.asMillis()) * heightFrac)/1000.0)*1000;
            TimePoint newTime = new MillisecondsTimePoint(newTimeMillis);
            pathPos = new TimedPositionImpl(newTime, newPos);
            reachedEnd = true;
        }
 
       // calculate horizontal side: left or right in reference to race course
        double posSide = 1;
        Bearing posBear = posStart.getBearingGreatCircle(pathPos.getPosition());
        Bearing bearVrt = posStart.getBearingGreatCircle(posEnd);
        double posBearDiff = bearVrt.getDifferenceTo(posBear).getDegrees();
        if ((posBearDiff < 0.0)||(posBearDiff > 180.0)) {
            posSide = -1;
        } else if ((posBearDiff == 0.0)||(posBearDiff == 180.0)) {
            posSide = 0;
        }
        // calculate horizontal distance as distance of height-position to current position
        Position posHeightTrgt = pathPos.getPosition().projectToLineThrough(posStart, bearVrt);
        double hrzDist = Math.round(posSide*posHeightTrgt.getDistance(pathPos.getPosition()).getMeters()*1000.0)/1000.0;

        // extend path-string by step-direction
        BitSet newPath = new BitSet(path.length+1);
        newPath.or(path.path);
        newPath.set(path.length, nextDirection);

        return (new PathCandidateBitSet(pathPos, reachedEnd, vrtDist, hrzDist, turnCount, newPath, path.length+1, nextDirection, appWind));
    }


    // generate path candidates based on beat angles
    List<PathCandidateBitSet> getPathCandsBeatWind(PathCandidateBitSet path, long timeStep, long turnLoss, Position posStart, Position posEnd, double tgtHeight) {

        List<PathCandidateBitSet> result = new ArrayList<PathCandidateBitSet>();
        PathCandidateBitSet newPathCand;

        if (this.maxTurns > 0) {

            boolean prevDirection = path.path.get(path.length-1);

            if ((path.trn < this.maxTurns)||(this.isSameDirection(path.length, prevDirection, LEFT))) {
                newPathCand = getPathCandWind(path, LEFT, timeStep, turnLoss, posStart, posEnd, tgtHeight);
                result.add(newPathCand);
            }

            if ((path.trn < this.maxTurns)||(this.isSameDirection(path.length, prevDirection, RIGHT))) {
                newPathCand = getPathCandWind(path, RIGHT, timeStep, turnLoss, posStart, posEnd, tgtHeight);
                result.add(newPathCand);
            }

        } else {

            // step left
            newPathCand = getPathCandWind(path, LEFT, timeStep, turnLoss, posStart, posEnd, tgtHeight);
            result.add(newPathCand);

            // step right
            newPathCand = getPathCandWind(path, RIGHT, timeStep, turnLoss, posStart, posEnd, tgtHeight);
            result.add(newPathCand);

        }

        return result;
    }

    Util.Pair<List<PathCandidateBitSet>,List<PathCandidateBitSet>> generateCandidate(List<PathCandidateBitSet> oldPaths, long timeStep, long turnLoss, Position posStart, Position posMiddle, Position posEnd, double tgtHeight) {

        List<PathCandidateBitSet> newPathCands;
        List<PathCandidateBitSet> leftPaths = new ArrayList<PathCandidateBitSet>();
        List<PathCandidateBitSet> rightPaths = new ArrayList<PathCandidateBitSet>();
        for(PathCandidateBitSet curPath : oldPaths) {

            if (curPath.reached) {
                continue;
            }
            
            newPathCands = this.getPathCandsBeatWind(curPath, timeStep, turnLoss, posStart, posEnd, tgtHeight);
            for (PathCandidateBitSet curNewPath : newPathCands) {
                // check whether path is *outside* regatta-area
                double distFromMiddleMeters = posMiddle.getDistance(curPath.pos.getPosition()).getMeters();
                if (distFromMiddleMeters > oobFact * tgtHeight) {
                    continue; // ignore curPath
                }

                if (curNewPath.sid == LEFT) {
                    leftPaths.add(curNewPath);
                } else if (curNewPath.sid == RIGHT) {
                    rightPaths.add(curNewPath);
                }

            }

        }

        Util.Pair<List<PathCandidateBitSet>,List<PathCandidateBitSet>> newPaths = new Util.Pair<List<PathCandidateBitSet>,List<PathCandidateBitSet>>(leftPaths, rightPaths);
        return newPaths;
    }


    List<PathCandidateBitSet> filterCandidates(List<PathCandidateBitSet> allCands, double hrzBinWidth) {

        boolean[] filterMap = new boolean[allCands.size()];

        // sort candidates by horizontal distance
        Comparator<PathCandidateBitSet> sortHorizontal = new SortPathCandsHorizontally();
        Collections.sort(allCands, sortHorizontal);

        // start scan with index 0
        int idxL = 0;
        int idxR = 0;

        // for each candidate, check the neighborhoods and identify bad candidates
        for(int idx = 0; idx < allCands.size(); idx++) {

            // current horizontal distance
            double hrzDist = allCands.get(idx).hrz;

            // align left index
            while(Math.abs(hrzDist - allCands.get(idxL).hrz) > hrzBinWidth) {
                idxL++;
            }

            // align right index
            boolean finished = false;
            while(!finished && (idxR < (allCands.size()-1))) {
                if (Math.abs(hrzDist - allCands.get(idxR+1).hrz) <= hrzBinWidth) {
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
                for(int jdx = (idxL+1); jdx <= idxR; jdx++) {
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
        List<PathCandidateBitSet> filterCands = new ArrayList<PathCandidateBitSet>();
        for(int idx=0; idx < allCands.size(); idx++) {
            if (!filterMap[idx]) {
                filterCands.add(allCands.get(idx));
            }
        }

        // return remaining good candidates
        return filterCands;
    }


    List<PathCandidateBitSet> filterIsochrone(List<PathCandidateBitSet> allCands, double hrzBinWidth) {

        boolean[] filterMap = new boolean[allCands.size()];
        for(int idx = 0; idx < allCands.size(); idx++) {
            filterMap[idx] = true;
        }

        // sort candidates by horizontal distance
        Comparator<PathCandidateBitSet> sortHorizontal = new SortPathCandsHorizontally();
        Collections.sort(allCands, sortHorizontal);

        // start scan with index 0
        int idxL = 0;
        int idxR = 0;

        // for each candidate, check the neighborhoods and identify bad candidates
        for(int idx = 0; idx < allCands.size(); idx++) {

            // current horizontal distance
            double hrzDist = allCands.get(idx).hrz;

            // align left index
            while(Math.abs(hrzDist - allCands.get(idxL).hrz) > hrzBinWidth) {
                idxL++;
            }

            // align right index
            boolean finished = false;
            while(!finished && (idxR < (allCands.size()-1))) {
                if (Math.abs(hrzDist - allCands.get(idxR+1).hrz) <= hrzBinWidth) {
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
                for(int jdx = (idxL+1); jdx <= idxR; jdx++) {
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

            for(Integer jdx : vrtIdx) {
                filterMap[jdx] = false;
            }

        } // endfor each candidate

        // collect all good candidates (i.e. filterMap == false)
        List<PathCandidateBitSet> filterCands = new ArrayList<PathCandidateBitSet>();
        for(int idx=0; idx < allCands.size(); idx++) {
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
        //Position startPos = this.parameters.getCourse().get(1);
        //Position endPos = this.parameters.getCourse().get(0);

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
        //Bearing bearHrz = bearVrt.add(new DegreeBearingImpl(90.0));
        Position middlePos = startPos.translateGreatCircle(bearVrt, distStartEnd.scale(0.5));
        
        Bearing bearRCWind = wndStart.getBearing().getDifferenceTo(bearVrt);
        String legType = "downwind";
        this.upwindLeg = false;
        
        if ((Math.abs(bearRCWind.getDegrees()) > 90.0)&&(Math.abs(bearRCWind.getDegrees()) < 270.0)) {
        	legType = "upwind";
            this.upwindLeg = true;
        }
        
        if (debugMsgOn) {
            System.out.println("start : "+startPos.getLatDeg()+", "+startPos.getLngDeg());
            System.out.println("middle: "+middlePos.getLatDeg()+", "+middlePos.getLngDeg());
            System.out.println("end   : "+endPos.getLatDeg()+", "+endPos.getLngDeg());
        }
        logger.info("Leg Direction: "+legType);

        long turnLoss = pd.getTurnLoss(); // time lost when doing a turn
        if (!this.upwindLeg) {
        	turnLoss = turnLoss / 2;
        }
        logger.info("Turnloss :" + turnLoss);

        this.usedTimeStep = turnLoss + 1000; // time-step larger than turn-loss is required (this may be removed by extended handling of turn-loss)
        logger.info("Time step :" + usedTimeStep);
        
        // calculate initial position according to initPathStr
        PathCandidateBitSet initPath = new PathCandidateBitSet(new TimedPositionImpl(currentTime, currentPosition), false, 0.0, 0.0, 0, new BitSet(1), 1, LEFT, wndStart);
        if (initPathStr.length()>1) {
            char nextDirectionChar = '0';
            for(int idx=1; idx<initPathStr.length(); idx++) {
                nextDirectionChar = initPathStr.charAt(idx);
                boolean nextDirection;
                if (nextDirectionChar == 'L') {
                	nextDirection = LEFT;
                } else {
                	nextDirection = RIGHT;
                }
                PathCandidateBitSet newPathCand = getPathCandWind(initPath, nextDirection, usedTimeStep, turnLoss, startPos, endPos, distStartEndMeters);
                initPath = newPathCand;
            }
        }
        List<PathCandidateBitSet> allPaths = new ArrayList<PathCandidateBitSet>();
        List<PathCandidateBitSet> trgPaths = new ArrayList<PathCandidateBitSet>();
        allPaths.add(initPath);


        TimedPosition tstPosition = this.getStep(new TimedPositionImpl(startTime, startPos), usedTimeStep, turnLoss, true, LEFT);
        double tstDist1 = startPos.getDistance(tstPosition.getPosition()).getMeters();
        tstPosition = this.getStep(new TimedPositionImpl(startTime, startPos), usedTimeStep, turnLoss, true, RIGHT);
        double tstDist2 = startPos.getDistance(tstPosition.getPosition()).getMeters();

        double hrzBinSize = (tstDist1 + tstDist2)/6.0; // horizontal bin size in meters
        if (debugMsgOn) {
            System.out.println("Horizontal Bin Size: "+hrzBinSize);
        }

        boolean reachedEnd = false;
        int addSteps = 0;
        int finalSteps = 0; // maximum number of additional steps after first target-path found

        while ((!reachedEnd)||(addSteps<finalSteps)) {

            if (reachedEnd) {
                addSteps++;
            }

            // generate new candidates (inside regatta-area)
            Util.Pair<List<PathCandidateBitSet>,List<PathCandidateBitSet>> newPaths = this.generateCandidate(allPaths, usedTimeStep, turnLoss, startPos, middlePos, endPos, distStartEndMeters);


            // select good candidates
            List<PathCandidateBitSet> leftPaths = this.filterCandidates(newPaths.getA(), hrzBinSize/2.0);
            List<PathCandidateBitSet> rightPaths = this.filterCandidates(newPaths.getB(), hrzBinSize/2.0);

            List<PathCandidateBitSet> nextPaths = new ArrayList<PathCandidateBitSet> ();
            nextPaths.addAll(leftPaths);
            nextPaths.addAll(rightPaths);

            allPaths = nextPaths;

            if (this.gridStore) {

                /*ArrayList<TimedPosition> isoChrone = new ArrayList<TimedPosition>();
                for(PathCand curCand : allPaths) {
                    isoChrone.add(curCand.pos);
                }
                this.gridPositions.add(isoChrone);*/

                this.gridPositions.add(allPaths);

                List<PathCandidateBitSet> isocPaths = this.filterIsochrone(allPaths, hrzBinSize);
                this.isocPositions.add(isocPaths);

            }

            // check if there are still paths in the regatta-area
            if (allPaths.size() > 0) {

                for(PathCandidateBitSet curPath : allPaths) {
                    // terminate path-search if paths are found that are close enough to target
                    //if ((curPath.vrt > distStartEndMeters)) {
                    if (curPath.reached) {
                        //logger.info("\ntPath: " + curPath.path + "\n      Time: " + (Math.round((curPath.pos.getTimePoint().asMillis()-startTime.asMillis())/1000.0/60.0*10.0)/10.0)+", Height: "+curPath.vrt+" of "+(Math.round(startPos.getDistance(endPos).getMeters()*100.0)/100.0)+", Dist: "+curPath.hrz+"m ~ "+(Math.round(curPath.pos.getPosition().getDistance(endPos).getMeters()*100.0)/100.0)+"m");
                        int curBin = (int)Math.round(Math.floor( (curPath.hrz + hrzBinSize/2.0) / hrzBinSize ));
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

            double distResolution = distStartEndMeters*0.01;
            BufferedWriter outputCSV;
            try {
                outputCSV = new BufferedWriter(new FileWriter(this.gridFile+"-grid.csv"));
                outputCSV.write("step; lat; lng; time; side; path; vrt\n");
                outputCSV.write("0; "+startPos.getLatDeg()+"; "+startPos.getLngDeg()+"; "+(startTime.asMillis()/1000)+"; 0; 0; "+(-distStartEndMeters)+"\n");
                outputCSV.write("0; "+endPos.getLatDeg()+"; "+endPos.getLngDeg()+"; "+(startTime.asMillis()/1000)+"; 0; 0; 0\n");
                int stepCount = 0;
                for(List<PathCandidateBitSet> isoChrone : this.gridPositions) {
                    stepCount++;
                    PathCandidateBitSet prevPos = null;
                    for(PathCandidateBitSet isoPos : isoChrone) {

                        if (prevPos != null) {
                            if (prevPos.pos.getPosition().getDistance(isoPos.pos.getPosition()).getMeters() < distResolution) {
                                continue;
                            }
                        }

                        String outStr = ""+stepCount+"; "+isoPos.pos.getPosition().getLatDeg()+"; "+isoPos.pos.getPosition().getLngDeg()+"; "+(isoPos.pos.getTimePoint().asMillis()/1000)+"; "+isoPos.sid;
                        outStr += "; "+isoPos.path+"; "+isoPos.vrt;
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
                outputCSV = new BufferedWriter(new FileWriter(this.gridFile+"-isoc.csv"));
                outputCSV.write("step; lat; lng; time; side; path; vrt\n");
                outputCSV.write("0; "+startPos.getLatDeg()+"; "+startPos.getLngDeg()+"; "+(startTime.asMillis()/1000)+"; 0; 0; "+(-distStartEndMeters)+"\n");
                outputCSV.write("0; "+endPos.getLatDeg()+"; "+endPos.getLngDeg()+"; "+(startTime.asMillis()/1000)+"; 0; 0; 0\n");
                int stepCount = 0;
                for(List<PathCandidateBitSet> isoChrone : this.isocPositions) {
                    stepCount++;
                    PathCandidateBitSet prevPos = null;
                    for(PathCandidateBitSet isoPos : isoChrone) {

                        if (prevPos != null) {
                            if (prevPos.pos.getPosition().getDistance(isoPos.pos.getPosition()).getMeters() < distResolution) {
                                continue;
                            }
                        }

                        String outStr = ""+stepCount+"; "+isoPos.pos.getPosition().getLatDeg()+"; "+isoPos.pos.getPosition().getLngDeg()+"; "+(isoPos.pos.getTimePoint().asMillis()/1000)+"; "+isoPos.sid;
                        outStr += "; "+isoPos.path+"; "+isoPos.vrt;
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
            //trgPaths = allPaths; // TODO: only for testing; remove lateron
            TimedPositionWithSpeed curPosition = new TimedPositionWithSpeedImpl(startTime, startPos, null);
            path.add(curPosition);
            return new PathImpl(path, wf, this.algorithmTimedOut); // return empty path
        }

        // sort target-paths ascending by distance-to-target
        Collections.sort(trgPaths);

        // debug output
        for(PathCandidateBitSet curPath : trgPaths) {
            logger.info("\nPath: " + curPath.path + "\n      Time: " + (curPath.pos.getTimePoint().asMillis()-startTime.asMillis()) +", Height: "+curPath.vrt+" of "+(Math.round(startPos.getDistance(endPos).getMeters()*100.0)/100.0)+", Dist: "+curPath.hrz+"m ~ "+(Math.round(curPath.pos.getPosition().getDistance(endPos).getMeters()*100.0)/100.0)+"m");
            //System.out.print(""+curPath.path+": "+curPath.pos.getTimePoint().asMillis()+", "+curPath.pos.getPosition().getLatDeg()+", "+curPath.pos.getPosition().getLngDeg()+", ");
            //System.out.println(" height:"+curPath.vrt+" of "+startPos.getDistance(endPos).getMeters()+", dist:"+curPath.hrz+" ~ "+curPath.pos.getPosition().getDistance(endPos));
        }

        //
        // fill gwt-path
        //

        // generate intermediate steps
        bestCand = trgPaths.get(0); // target-path ending closest to target
        TimedPositionWithSpeed curPosition = null;
        boolean nextDirection = LEFT;
        boolean prevDirection = LEFT;
        for(int step=0; step<(bestCand.length-1); step++) {

        	nextDirection = bestCand.path.get(step);

            if (step == 0) {

                curPosition = new TimedPositionWithSpeedImpl(startTime, startPos, null);
                path.add(curPosition);

            } else {

                boolean sameBaseDirection = this.isSameDirection(step, prevDirection, nextDirection);
                TimedPosition newPosition = this.getStep(curPosition, usedTimeStep, turnLoss, sameBaseDirection, nextDirection);
                curPosition = new TimedPositionWithSpeedImpl(newPosition.getTimePoint(), newPosition.getPosition(), null);
                path.add(curPosition);

            }

            prevDirection = nextDirection;
        }

        // add final position (rescaled before to end on height of target)
        path.add(new TimedPositionWithSpeedImpl(bestCand.pos.getTimePoint(), bestCand.pos.getPosition(), null));            

        return new PathImpl(path, wf, this.algorithmTimedOut);

    }

}
