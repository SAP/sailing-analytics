package com.sap.sailing.simulator.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.simulator.BoatDirection;
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
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PathGeneratorOpportunistEuclidian360 extends PathGeneratorBase {

    private static Logger logger = Logger.getLogger("com.sap.sailing");
    int turns;
    boolean startLeft;
    boolean upwindLeg = false;

    public PathGeneratorOpportunistEuclidian360(SimulationParameters params) {
        PolarDiagram polarDiagramClone = new PolarDiagramBase((PolarDiagramBase)params.getBoatPolarDiagram());
        parameters = new SimulationParametersImpl(params.getCourse(), params.getStartLine(), params.getEndLine(), polarDiagramClone, params.getWindField(),
                params.getSimuStep(), params.getMode(), params.showOmniscient(), params.showOpportunist(), params.getLegType());
    }

    public void setEvaluationParameters(boolean startLeft) {
        this.startLeft = startLeft;
    }

    public int getTurns() {
        return turns;
    }
    
    public boolean isSameBaseDirection(BoatDirection direction1, BoatDirection direction2) {
        boolean result = isBaseDirectionLeft(direction1)&&isBaseDirectionLeft(direction2);
        result |= isBaseDirectionRight(direction1)&&isBaseDirectionRight(direction2);
        result |= (direction1 == BoatDirection.NONE) || (direction2 == BoatDirection.NONE);
        return result;
    }
    
    public boolean isBaseDirectionLeft(BoatDirection direction) {
        return (direction == BoatDirection.BEAT_LEFT)||(direction == BoatDirection.JIBE_LEFT)||(direction == BoatDirection.REACH_LEFT);
    }

    public boolean isBaseDirectionRight(BoatDirection direction) {
        return (direction == BoatDirection.BEAT_RIGHT)||(direction == BoatDirection.JIBE_RIGHT)||(direction == BoatDirection.REACH_RIGHT);
    }

    
    @Override
    public Path getPath() throws SparseSimulationDataException {
        this.algorithmStartTime = MillisecondsTimePoint.now();

        WindFieldGenerator wf = parameters.getWindField();
        PolarDiagram polarDiagram = parameters.getBoatPolarDiagram();

        Position startPos = parameters.getCourse().get(0);
        Position endPos = parameters.getCourse().get(1);
        Bearing bearVrt = startPos.getBearingGreatCircle(endPos);

        List<Position> startLine = this.parameters.getStartLine();
        if (startLine != null) {
            Bearing bearStartLine = startLine.get(0).getBearingGreatCircle(startLine.get(1));
            double diffStartLineVrt = bearVrt.getDifferenceTo(bearStartLine).getDegrees();
            Position startPositionLeft;
            Position startPositionRight;
            if ((diffStartLineVrt > 0) && (diffStartLineVrt < 180)) {
                startPositionLeft = startLine.get(0);
                startPositionRight = startLine.get(1);
            } else {
                startPositionLeft = startLine.get(1);
                startPositionRight = startLine.get(0);
            }
            if (startLeft == true) {
                startPos = startPositionLeft;
            } else {
                startPos = startPositionRight;
            }
            bearVrt = startPos.getBearingGreatCircle(endPos);
        }

        TimePoint startTime = wf.getStartTime();
        List<TimedPositionWithSpeed> path = new ArrayList<TimedPositionWithSpeed>();
        String pathStr;

        Position currentPosition = startPos;
        TimePoint currentTime = startTime;
        double currentHeight = startPos.getDistance(endPos).getMeters();

        BoatDirection prevDirection = BoatDirection.NONE;
        BoatDirection prevPrevDirection = BoatDirection.NONE;
        long turnLoss = polarDiagram.getTurnLoss(); // time lost when doing a turn
        double fracFinishPhase = 0.05;

        TimePoint travelTimeLeft;
        TimePoint travelTimeRight;

        Wind wndStart = wf.getWind(new TimedPositionWithSpeedImpl(startTime, startPos, null));
        logger.finest("wndStart speed:" + wndStart.getKnots() + " angle:" + wndStart.getBearing().getDegrees());
        polarDiagram.setWind(wndStart);
        Bearing bearStart = currentPosition.getBearingGreatCircle(endPos);
        path.add(new TimedPositionWithSpeedImpl(startTime, startPos, wndStart));
        pathStr = "0";

        long timeStep;
        if (this.parameters.getSimuStep() != null) {
            // time-step larger than turn-loss is required (this may be removed by
            // extended handling of turn-loss)
            if (this.parameters.getSimuStep().asMillis() > 2*turnLoss) {
                timeStep = this.parameters.getSimuStep().asMillis();
            } else {
                timeStep = 2*turnLoss;
            }
        } else {
            timeStep = wf.getTimeStep().asMillis() / 2; 
        }
        logger.fine("Time step :" + timeStep);

        String legType = "none";
        if (this.parameters.getLegType() == null) {
            Bearing bearRCWind = wndStart.getBearing().getDifferenceTo(bearStart);
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

        int timeStepScale = 1;
        if (!this.upwindLeg) {
            timeStepScale = 2;
            timeStep = timeStep / timeStepScale;
            turnLoss = turnLoss / timeStepScale;
        }

        logger.fine("Leg Direction: " + legType);

        //
        // StrategicPhase: start & intermediate course until close to target
        //
        turns = 0;
        while ((currentHeight > startPos.getDistance(endPos).getMeters()*fracFinishPhase)
                && (path.size() < 500) && (!this.isTimedOut())) {

            long nextTimeVal = currentTime.asMillis() + timeStep;
            TimePoint nextTime = new MillisecondsTimePoint(nextTimeVal);

            // get bearing to target            
            Bearing bearTarget = currentPosition.getBearingGreatCircle(endPos);
            // set wind at current position
            Wind currentWind = wf.getWind(new TimedPositionWithSpeedImpl(currentTime, currentPosition, null));
            logger.finest("cWind speed:" + currentWind.getKnots() + " angle:" + currentWind.getBearing().getDegrees());
            polarDiagram.setWind(currentWind);
            // get point-of-sail and reaching-side
            Pair<PointOfSail, BoatDirection> pointOfSailAndReachingSide = polarDiagram.getPointOfSail(bearTarget);
            PointOfSail pointOfSail = pointOfSailAndReachingSide.getA();
            BoatDirection reachingSide = pointOfSailAndReachingSide.getB();
            
            if ((pointOfSail == PointOfSail.TACKING)||(pointOfSail == PointOfSail.JIBING)) {
                // get optimal bearings at current position
                Bearing bearLeft;
                Bearing bearRight;
                if (pointOfSail == PointOfSail.TACKING) {
                    bearLeft = polarDiagram.optimalDirectionsUpwind()[0];
                    bearRight = polarDiagram.optimalDirectionsUpwind()[1];
                } else {
                    bearLeft = polarDiagram.optimalDirectionsDownwind()[1];
                    bearRight = polarDiagram.optimalDirectionsDownwind()[0];
                }
                // get boat speed at current position
                SpeedWithBearing boatSpeedLeft = polarDiagram.getSpeedAtBearing(bearLeft);
                if (boatSpeedLeft.getKnots() == 0) {
                    logger.severe("Travel Speed for NextDirection '" + "L" + "' is ZERO. This must NOT happen.");            
                    throw new SparseSimulationDataException();
                }
                SpeedWithBearing boatSpeedRight = polarDiagram.getSpeedAtBearing(bearRight);
                if (boatSpeedRight.getKnots() == 0) {
                    logger.severe("Travel Speed for NextDirection '" + "R" + "' is ZERO. This must NOT happen.");            
                    throw new SparseSimulationDataException();
                }
                logger.finest("left boat speed:" + boatSpeedLeft.getKnots() + " angle:" + boatSpeedLeft.getBearing().getDegrees()
                        + "  right boat speed:" + boatSpeedRight.getKnots() + " angle:" + boatSpeedRight.getBearing().getDegrees());

                // get travel-time taking turn-loss into account
                if (isBaseDirectionLeft(prevDirection)) {
                    travelTimeLeft = new MillisecondsTimePoint(nextTimeVal);
                    travelTimeRight = new MillisecondsTimePoint(nextTimeVal - turnLoss);
                } else if (isBaseDirectionRight(prevDirection)) {
                    travelTimeLeft = new MillisecondsTimePoint(nextTimeVal - turnLoss);
                    travelTimeRight = new MillisecondsTimePoint(nextTimeVal);
                } else {
                    travelTimeLeft = new MillisecondsTimePoint(nextTimeVal);
                    travelTimeRight = new MillisecondsTimePoint(nextTimeVal);
                }
                
                // get next boat positions by traveling left and right
                Position nextBoatPositionLeft = boatSpeedLeft.travelTo(currentPosition, currentTime, travelTimeLeft);
                Position nextBoatPositionRight = boatSpeedRight.travelTo(currentPosition, currentTime, travelTimeRight);
                // calculate distance to target left and right
                Distance targetDistanceLeft = nextBoatPositionLeft.getDistance(endPos);
                Distance targetDistanceRight = nextBoatPositionRight.getDistance(endPos);
                double targetDistanceMetersLeft = Math.round(targetDistanceLeft.getMeters() * 1000.) / 1000.;
                double targetDistanceMetersRight = Math.round(targetDistanceRight.getMeters() * 1000.) / 1000.;
                prevPrevDirection = prevDirection;
                
                if ((prevDirection == BoatDirection.NONE) && (startLine == null)) {
                    
                    if (startLeft) {
                        if (pointOfSail == PointOfSail.TACKING) {
                            path.add(new TimedPositionWithSpeedImpl(nextTime, nextBoatPositionLeft, currentWind));
                            pathStr += "L";
                            currentPosition = nextBoatPositionLeft;
                            prevDirection = BoatDirection.BEAT_LEFT;
                        } else {
                            path.add(new TimedPositionWithSpeedImpl(nextTime, nextBoatPositionRight, currentWind));
                            pathStr += "r";
                            currentPosition = nextBoatPositionRight;
                            prevDirection = BoatDirection.JIBE_RIGHT;                        
                        }
                    } else {
                        if (pointOfSail == PointOfSail.TACKING) {
                            path.add(new TimedPositionWithSpeedImpl(nextTime, nextBoatPositionRight, currentWind));
                            pathStr += "R";
                            currentPosition = nextBoatPositionRight;
                            prevDirection = BoatDirection.BEAT_RIGHT;
                        } else {
                            path.add(new TimedPositionWithSpeedImpl(nextTime, nextBoatPositionLeft, currentWind));
                            pathStr += "l";
                            currentPosition = nextBoatPositionLeft;
                            prevDirection = BoatDirection.JIBE_LEFT;                        
                        }
                    }

                } else {

                    if (targetDistanceMetersLeft <= targetDistanceMetersRight) {
                        path.add(new TimedPositionWithSpeedImpl(nextTime, nextBoatPositionLeft, currentWind));
                        currentPosition = nextBoatPositionLeft;
                        if (isBaseDirectionRight(prevDirection)) {
                            turns++;
                        }
                        if (pointOfSail == PointOfSail.TACKING) {
                            prevDirection = BoatDirection.BEAT_LEFT;
                            pathStr += "L";
                        } else {
                            prevDirection = BoatDirection.JIBE_LEFT;
                            pathStr += "l";
                        }
                    } else {
                        path.add(new TimedPositionWithSpeedImpl(nextTime, nextBoatPositionRight, currentWind));
                        currentPosition = nextBoatPositionRight;
                        if (isBaseDirectionLeft(prevDirection)) {
                            turns++;
                        }
                        if (pointOfSail == PointOfSail.TACKING) {
                            prevDirection = BoatDirection.BEAT_RIGHT;
                            pathStr += "R";
                        } else {
                            prevDirection = BoatDirection.JIBE_RIGHT;
                            pathStr += "r";
                        }
                    }
                }
            // endif ((pointOfSail == PointOfSail.TACKING)||(pointOfSail == PointOfSail.JIBING))
            } else if (pointOfSail == PointOfSail.REACHING) {

                TimePoint travelTimeReach;
                // get travel-time taking turn-loss into account
                if (isSameBaseDirection(reachingSide, prevDirection)) {
                    travelTimeReach = new MillisecondsTimePoint(nextTimeVal);
                } else {
                    travelTimeReach = new MillisecondsTimePoint(nextTimeVal - turnLoss);
                }
                // get boat speed for reach
                SpeedWithBearing boatSpeedTarget;
                if (polarDiagram.hasCurrent()) {
                    boatSpeedTarget = polarDiagram.getSpeedAtBearingOverGround(bearTarget);
                } else {
                    boatSpeedTarget = polarDiagram.getSpeedAtBearing(bearTarget);                    
                }
                if ((boatSpeedTarget.getKnots() == 0)&&(!polarDiagram.hasCurrent())) {
                    logger.severe("Travel Speed for NextDirection '" + (reachingSide==BoatDirection.REACH_LEFT?"D":"E") + "' is ZERO. This must NOT happen.");            
                    throw new SparseSimulationDataException();
                }
                // get next boat positions by traveling reach
                Position nextBoatPositionReach = boatSpeedTarget.travelTo(currentPosition, currentTime, travelTimeReach);
                path.add(new TimedPositionWithSpeedImpl(nextTime, nextBoatPositionReach, currentWind));
                currentPosition = nextBoatPositionReach;
                if (!isSameBaseDirection(reachingSide, prevDirection)) {
                    turns++;
                }
                
                prevPrevDirection = prevDirection;
                prevDirection = reachingSide;
                if (reachingSide == BoatDirection.REACH_LEFT) {
                    pathStr += "D";
                } else {
                    pathStr += "E";
                }
            }
            
            currentTime = nextTime;
            Position posHeight = currentPosition.projectToLineThrough(startPos, bearStart);
            currentHeight = startPos.getDistance(endPos).getMeters() - posHeight.getDistance(startPos).getMeters();
        }

        // remove last position, if already too close to target for finish-phase
        if (currentHeight < startPos.getDistance(endPos).getMeters()*fracFinishPhase/2) {
            path.remove(path.size()-1);
            currentTime = path.get(path.size()-1).getTimePoint();
            currentPosition = path.get(path.size()-1).getPosition();
            prevDirection = prevPrevDirection;
        } else {
            // get bearing to target            
            Bearing nextBearTarget = currentPosition.getBearingGreatCircle(endPos);
            // get point-of-sail and reaching-side
            Pair<PointOfSail, BoatDirection> nextPointOfSailAndReachingSide = polarDiagram.getPointOfSail(nextBearTarget);
            PointOfSail nextPointOfSail = nextPointOfSailAndReachingSide.getA();
            if (nextPointOfSail == PointOfSail.REACHING) {
                path.remove(path.size()-1);
                currentTime = path.get(path.size()-1).getTimePoint();
                currentPosition = path.get(path.size()-1).getPosition();
                prevDirection = prevPrevDirection;                
            }
        }

        if (!this.isTimedOut()) {
            //
            // FinishPhase: get 1-turners to finalize course
            //
            PathGenerator1Turner360 generator1Turner = new PathGenerator1Turner360(parameters);
            TimePoint leftTurningTime;
            TimePoint rightTurningTime;
            if (isBaseDirectionLeft(prevDirection)) {
                leftTurningTime = currentTime;
                rightTurningTime = new MillisecondsTimePoint(currentTime.asMillis() + turnLoss);
            } else {
                leftTurningTime = new MillisecondsTimePoint(currentTime.asMillis() + turnLoss);
                rightTurningTime = currentTime;
            }

            long finishTimeStep = Math.max(500, timeStep / 10);
            int finishStepsLeft = (int) Math.round(5*(path.get(path.size()-1).getTimePoint().asMillis() - path.get(0).getTimePoint().asMillis()) / (1-fracFinishPhase) * fracFinishPhase / finishTimeStep);
            generator1Turner.setEvaluationParameters(true, currentPosition, endPos, leftTurningTime, finishTimeStep, finishStepsLeft, 0.2, this.upwindLeg);
            Path leftPath = generator1Turner.getPath();

            int finishStepsRight = (int) Math.round(5*(path.get(path.size()-1).getTimePoint().asMillis() - path.get(0).getTimePoint().asMillis()) / (1-fracFinishPhase) * fracFinishPhase / finishTimeStep);
            generator1Turner.setEvaluationParameters(false, currentPosition, endPos, rightTurningTime, finishTimeStep, finishStepsRight, 0.2, this.upwindLeg);
            Path rightPath = generator1Turner.getPath();

            if ((leftPath.getPathPoints() != null) && (rightPath.getPathPoints() != null)) {
                if (leftPath.getFinalTime().asMillis() <= rightPath.getFinalTime().asMillis()) {
                    path.addAll(leftPath.getPathPoints());
                } else {
                    path.addAll(rightPath.getPathPoints());
                }
            } else if (leftPath.getPathPoints() != null) {
                path.addAll(leftPath.getPathPoints());
            } else if (rightPath.getPathPoints() != null) {
                path.addAll(rightPath.getPathPoints());
            }
        }

        // logging information about best found course
        TimedPosition finalPosition = path.get(path.size()-1);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(finalPosition.getTimePoint().asMillis() - startTime.asMillis());
        SimpleDateFormat racetimeFormat = new SimpleDateFormat("HH:mm:ss:SSS");
        racetimeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String racetimeFormatted = racetimeFormat.format(cal.getTime());
        logger.fine("Start Condition: 0"+(this.startLeft?"L":"R") +"\nPath: " + pathStr +"\n      Time: "
                +  racetimeFormatted + ", Distance: "
                + String.format("%.2f", Math.round(finalPosition.getPosition().getDistance(endPos).getMeters() * 100.0) / 100.0) + " meters"
                + ", " + this.turns + " Turn" + (this.turns>1?"s":""));

        return new PathImpl(path, wf, getTurns(), this.algorithmTimedOut);
    }
}
