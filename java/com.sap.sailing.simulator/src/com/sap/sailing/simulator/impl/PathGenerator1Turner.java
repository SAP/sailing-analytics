package com.sap.sailing.simulator.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PathGenerator1Turner extends PathGeneratorBase {

    class result1Turn {
        public result1Turn(Path[] p, char s, int n) {
            paths = p;
            side = s;
            middle = n;
        }

        Path[] paths;
        char side;
        int middle;
    }

    // private static Logger logger = Logger.getLogger("com.sap.sailing");
    private SimulationParameters simulationParameters;
    private boolean leftSide;
    private result1Turn result;
    private Position evalStartPoint;
    private Position evalEndPoint;
    private TimePoint evalStartTime;
    private long evalTimeStep;
    private int evalStepMax;
    private double evalTolerance;
    private boolean upwindLeg;
    
    public PathGenerator1Turner(SimulationParameters params) {
        simulationParameters = params;
    }

    public void setEvaluationParameters(boolean leftSideVal, Position startPoint, Position endPoint, TimePoint startTime, long timeStep, int stepMax, double tolerance, boolean upwindLeg) {
        this.leftSide = leftSideVal;
        this.evalStartPoint = startPoint;
        this.evalEndPoint = endPoint;
        this.evalStartTime = startTime;
        this.evalTimeStep = timeStep;
        this.evalStepMax = stepMax;
        this.evalTolerance = tolerance;
        this.upwindLeg = upwindLeg;
    }

    public int getMiddle() {
        return this.result.middle;
    }

    @Override
    public Path getPath() {
        this.algorithmStartTime = MillisecondsTimePoint.now();

        WindFieldGenerator windField = simulationParameters.getWindField();
        PolarDiagram polarDiagram = simulationParameters.getBoatPolarDiagram();
        
        Position start;
        if (this.evalStartPoint == null) {
            start = simulationParameters.getCourse().get(0);
        } else {
            start = this.evalStartPoint;            
        }
        Position end;
        if (this.evalEndPoint == null) {
            end = simulationParameters.getCourse().get(1);
        } else {
            end = this.evalEndPoint;            
        }
        
        TimePoint startTime;
        if (this.evalStartTime == null) {
            startTime = windField.getStartTime();// new MillisecondsTimePoint(0);
        } else {
            startTime = this.evalStartTime;
        }

        long turnloss = polarDiagram.getTurnLoss(); // 4000;

        Distance courseLength = start.getDistance(end);
        Bearing bearStart2End = start.getBearingGreatCircle(end);
        Position currentPosition = start;
        TimePoint currentTime = startTime;
        TimePoint nextTime;

        double reachingTolerance;
        if (this.evalTolerance == 0) {
            reachingTolerance = 0.03;
        } else {
            reachingTolerance = this.evalTolerance;
        }
        int stepMax;
        if (this.evalStepMax == 0) {
            stepMax = 800;
        } else {
            stepMax = this.evalStepMax;
        }
        double[] reachTime = new double[stepMax];
        boolean targetFound;
        long timeStep;
        if (this.evalTimeStep == 0) {
            timeStep = windField.getTimeStep().asMillis() / 3;
        } else {
            timeStep = this.evalTimeStep;
        }
        Bearing direction;

        double newDistance;
        double minimumDistance = courseLength.getMeters();
        double overallMinimumDistance = courseLength.getMeters();
        int stepOfOverallMinimumDistance = stepMax;
        LinkedList<TimedPositionWithSpeed> path = null;
        // LinkedList<TimedPositionWithSpeed> prevpath = null;
        // LinkedList<TimedPositionWithSpeed> xpath1 = null;
        // LinkedList<TimedPositionWithSpeed> xpath2 = null;

        LinkedList<TimedPositionWithSpeed> allminpath = null;

        for (int step = 0; step < stepMax; step++) {

            currentPosition = start;
            currentTime = startTime;
            reachTime[step] = courseLength.getMeters();
            targetFound = false;
            minimumDistance = courseLength.getMeters();
            path = new LinkedList<TimedPositionWithSpeed>();
            path.addLast(new TimedPositionWithSpeedImpl(currentTime, currentPosition, null));

            if (this.isTimedOut()) {
                break;
            }
            
            int stepLeft = 0;
            while ((stepLeft < step) && (!targetFound) && (!this.isTimedOut())) {

                SpeedWithBearing currentWind = windField.getWind(new TimedPositionImpl(currentTime, currentPosition));
                //System.out.println("Wind: " + currentWind.getKnots() + "kn, " + currentWind.getBearing().getDegrees() + "deg");
                polarDiagram.setWind(currentWind);
                if (this.upwindLeg) {
                	if (leftSide) {
                		direction = polarDiagram.optimalDirectionsUpwind()[0];
                	} else {
                		direction = polarDiagram.optimalDirectionsUpwind()[1];
                	}
                } else {
                	if (leftSide) {
                		direction = polarDiagram.optimalDirectionsDownwind()[0];
                	} else {
                		direction = polarDiagram.optimalDirectionsDownwind()[1];
                	}
                }
                SpeedWithBearing currSpeed = polarDiagram.getSpeedAtBearing(direction);
                //System.out.println("Boat: " + currSpeed.getKnots() + "kn, " + currSpeed.getBearing().getDegrees() + "deg");
                nextTime = new MillisecondsTimePoint(currentTime.asMillis() + timeStep);
                Position nextPosition = currSpeed.travelTo(currentPosition, currentTime, nextTime);
                //System.out.println("Dist: " + currentPosition.getDistance(nextPosition).getMeters() + "m");
                newDistance = nextPosition.getDistance(end).getMeters();
                if (newDistance < minimumDistance) {
                    minimumDistance = newDistance;
                }
                currentPosition = nextPosition;
                currentTime = nextTime;
                path.addLast(new TimedPositionWithSpeedImpl(currentTime, currentPosition, currentWind));

                if (currentPosition.getDistance(end).getMeters() < reachingTolerance * courseLength.getMeters()) {
                    reachTime[step] = minimumDistance;
                    targetFound = true;
                }
                if (minimumDistance < overallMinimumDistance) {
                    overallMinimumDistance = minimumDistance;
                    stepOfOverallMinimumDistance = step;
                    allminpath = path;
                }
                stepLeft++;
            }

            currentTime = new MillisecondsTimePoint(currentTime.asMillis() + turnloss);

            int stepRight = 0;
            while ((stepRight < (stepMax - step)) && (!targetFound) && (!this.isTimedOut())) {

            	SpeedWithBearing currentWind = windField.getWind(new TimedPositionImpl(currentTime, currentPosition));
            	polarDiagram.setWind(currentWind);
            	if (this.upwindLeg) {
            		if (leftSide) {
            			direction = polarDiagram.optimalDirectionsUpwind()[1];
            		} else {
            			direction = polarDiagram.optimalDirectionsUpwind()[0];
            		}
            	} else {
            		if (leftSide) {
            			direction = polarDiagram.optimalDirectionsDownwind()[1];
            		} else {
            			direction = polarDiagram.optimalDirectionsDownwind()[0];
            		}
            	}

            	SpeedWithBearing currSpeed = polarDiagram.getSpeedAtBearing(direction);
            	nextTime = new MillisecondsTimePoint(currentTime.asMillis() + timeStep);
            	Position nextPosition = currSpeed.travelTo(currentPosition, currentTime, nextTime);
                newDistance = nextPosition.getDistance(end).getMeters();
                /*if (this.evalStartPoint != null) {
                    System.out.println("newDistance: "+newDistance);
                }*/
                if (newDistance < minimumDistance) {
                    minimumDistance = newDistance;
                }
                currentPosition = nextPosition;
                currentTime = nextTime;
                path.addLast(new TimedPositionWithSpeedImpl(currentTime, currentPosition, currentWind));
                /*if (this.evalStartPoint != null) {
                    System.out.println("s: "+step+" dist: "+currentPosition.getDistance(end).getMeters()+" ?<? "+reachingTolerance * courseLength.getMeters());
                }*/
                if (currentPosition.getDistance(end).getMeters() < reachingTolerance * courseLength.getMeters()) {
                    // System.out.println(""+s+":"+path.size()+" dist:"+mindist);
                    Bearing bearPath2End = currentPosition.getBearingGreatCircle(end);
                    double bearDiff = bearPath2End.getDegrees() - bearStart2End.getDegrees();
                    // System.out.println(""+s+": "+mindist+" bearDiff: "+bearDiff);
                    reachTime[step] = minimumDistance * Math.signum(bearDiff);
                    /*if ((prevpath != null) && (Math.signum(reachTime[step]) != Math.signum(reachTime[step - 1]))) {
                        xpath1 = path;
                        xpath2 = prevpath;
                    }
                    prevpath = path;*/
                    if (start.getDistance(currentPosition).getMeters() > start.getDistance(end).getMeters()) {
                        targetFound = true;
                    }
                }
                if (minimumDistance < overallMinimumDistance) {
                    overallMinimumDistance = minimumDistance;
                    stepOfOverallMinimumDistance = step;
                    allminpath = new LinkedList<TimedPositionWithSpeed>(path);
                }
                stepRight++;
            }
        }

        /*
         * for (int i=0; i<reachTime.length; i++) { System.out.println(""+i+": "+reachTime[i]); }
         */

        // PathImpl[] paths = new PathImpl[2];
        // paths[0] = new PathImpl(xpath1,windField);
        // paths[1] = new PathImpl(xpath2, windField);
        PathImpl[] paths = new PathImpl[1];
        paths[0] = new PathImpl(allminpath, windField, this.algorithmTimedOut);
        char side;
        if (leftSide) {
            side = 'L';
        } else {
            side = 'R';
        }
        this.result = new result1Turn(paths, side, stepOfOverallMinimumDistance);

        return result.paths[0];

    }

    private static final double TRESHOLD_MINIMUM_DISTANCE_METERS = 10.0;

    public TimedPositionWithSpeed get1Turner(WindFieldGenerator windField, PolarDiagram polarDiagram, Position start, Position end, TimePoint startTime,
    		boolean leftSide, int stepMax, long timeStep) {

    	// System.out.println("inside get1Turner");
    	// System.out.println("segment: (" + start.getLatDeg() + "," + start.getLngDeg() + ") and (" + end.getLatDeg() +
    	// "," + end.getLngDeg() + ")");
    	// System.out.println("segment length: " + start.getDistance(end).getMeters() + " meters");
    	// System.out.println("starting at " + startTime.asMillis() + " milliseconds");

    	long turnloss = polarDiagram.getTurnLoss(); // 4000;

    	final Distance courseLength = start.getDistance(end);
    	Bearing bearStart2End = start.getBearingGreatCircle(end);
    	Position currentPosition = start;
    	TimePoint currentTime = startTime;
    	// problema e ca starttime e 1970
    	TimePoint nextTime;

    	double[] reachTime = new double[stepMax];
    	boolean targetFound;
    	Bearing direction;

    	double distanceToEnd = 0.0;
    	double newDistance = 0.0;
    	double minimumDistance = courseLength.getMeters();
    	double overallMinimumDistance = courseLength.getMeters();
    	int stepOfOverallMinimumDistance = stepMax;
    	LinkedList<TimedPositionWithSpeed> path = null;

    	LinkedList<TimedPositionWithSpeed> allminpath = null;
    	SpeedWithBearing currentWind = null;
    	SpeedWithBearing currSpeed = null;
    	Position nextPosition = null;

    	for (int step = 0; step < stepMax; step++) {

    		// System.out.println("------------------------------");
    		// System.out.println("step = " + step);

    		currentPosition = start;
    		currentTime = startTime;
    		reachTime[step] = courseLength.getMeters();
    		targetFound = false;
    		minimumDistance = courseLength.getMeters();
    		path = new LinkedList<TimedPositionWithSpeed>();
    		path.addLast(new TimedPositionWithSpeedImpl(currentTime, currentPosition, currSpeed));

                if (this.isTimedOut()) {
                    break;
                }

    		int stepLeft = 0;
    		while ((stepLeft < step) && (!targetFound) && (!this.isTimedOut())) {

    			// System.out.println("stepLeft = " + stepLeft + " targetFound = " + targetFound);

    			currentWind = windField.getWind(new TimedPositionImpl(currentTime, currentPosition));
    			polarDiagram.setWind(currentWind);
    			direction = polarDiagram.optimalDirectionsUpwind()[leftSide ? 0 : 1];

    			currSpeed = polarDiagram.getSpeedAtBearing(direction);
    			nextTime = new MillisecondsTimePoint(currentTime.asMillis() + timeStep);
    			nextPosition = currSpeed.travelTo(currentPosition, currentTime, nextTime);
    			newDistance = nextPosition.getDistance(end).getMeters();

    			if (newDistance < minimumDistance) {
    				minimumDistance = newDistance;
    			}

    			currentPosition = nextPosition;
    			currentTime = nextTime;
    			path.addLast(new TimedPositionWithSpeedImpl(currentTime, currentPosition, currSpeed));

    			// if (currentPosition.getDistance(end).getMeters() < reachingTolerance * courseLength.getMeters()) {
    			distanceToEnd = currentPosition.getDistance(end).getMeters();

    			// System.out.println("distanceToEnd = " + distanceToEnd + " meters");

    			if (distanceToEnd < TRESHOLD_MINIMUM_DISTANCE_METERS) {
    				reachTime[step] = minimumDistance;
    				targetFound = true;
    				if (minimumDistance < overallMinimumDistance) {
    					overallMinimumDistance = minimumDistance;
    					stepOfOverallMinimumDistance = step;
    					allminpath = path;
    				}
    			}
    			stepLeft++;
    		}

    		currentTime = new MillisecondsTimePoint(currentTime.asMillis() + turnloss);

    		int stepRight = 0;
    		while ((stepRight < (stepMax - step)) && (!targetFound) && (!this.isTimedOut())) {

    			// System.out.println("stepRight = " + stepLeft + " targetFound = " + targetFound);

    			currentWind = windField.getWind(new TimedPositionImpl(currentTime, currentPosition));

    			polarDiagram.setWind(currentWind);
    			direction = polarDiagram.optimalDirectionsUpwind()[leftSide ? 1 : 0];

    			currSpeed = polarDiagram.getSpeedAtBearing(direction);
    			nextTime = new MillisecondsTimePoint(currentTime.asMillis() + timeStep);
    			nextPosition = currSpeed.travelTo(currentPosition, currentTime, nextTime);
    			newDistance = nextPosition.getDistance(end).getMeters();

    			if (newDistance < minimumDistance) {
    				minimumDistance = newDistance;
    			}

    			currentPosition = nextPosition;
    			currentTime = nextTime;
    			path.addLast(new TimedPositionWithSpeedImpl(currentTime, currentPosition, currSpeed));

    			// if (currentPosition.getDistance(end).getMeters() < reachingTolerance * courseLength.getMeters()) {
    			distanceToEnd = currentPosition.getDistance(end).getMeters();

    			// System.out.println("distanceToEnd = " + distanceToEnd + " meters");

    			if (distanceToEnd < TRESHOLD_MINIMUM_DISTANCE_METERS) {

    				Bearing bearPath2End = currentPosition.getBearingGreatCircle(end);
    				double bearDiff = bearPath2End.getDegrees() - bearStart2End.getDegrees();

    				reachTime[step] = minimumDistance * Math.signum(bearDiff);

    				if (start.getDistance(currentPosition).getMeters() > start.getDistance(end).getMeters()) {
    					targetFound = true;
    				}
    				if (minimumDistance < overallMinimumDistance) {
    					overallMinimumDistance = minimumDistance;
    					stepOfOverallMinimumDistance = step;
    					allminpath = new LinkedList<TimedPositionWithSpeed>(path);
    				}
    			}
    			stepRight++;
    		}
    	}

    	PathImpl[] paths = new PathImpl[1];
    	paths[0] = new PathImpl(allminpath, windField, this.algorithmTimedOut);
    	this.result = new result1Turn(paths, (leftSide ? 'L' : 'R'), stepOfOverallMinimumDistance);

    	TimedPositionWithSpeed oneTurnerPoint = allminpath.get(stepOfOverallMinimumDistance);
    	if (oneTurnerPoint.getSpeed() == null) {
    		// System.out.println("wind data is null for the one turner point");

    		currentWind = windField.getWind(new TimedPositionImpl(currentTime, currentPosition));

    		oneTurnerPoint = new TimedPositionWithSpeedImpl(oneTurnerPoint.getTimePoint(), oneTurnerPoint.getPosition(), currentWind);
    	}

    	return oneTurnerPoint;

    }

    public List<TimedPositionWithSpeed> getIntersectionOptimalTowardWind(WindFieldGenerator windField, PolarDiagram polarDiagram, Position edgeStart,
            Position edgeEnd, TimedPositionWithSpeed start, boolean leftSide, long timeStepMilliseconds, double minimumDistanceMeters) {

        List<TimedPositionWithSpeed> path = new ArrayList<TimedPositionWithSpeed>();
        TimedPositionWithSpeed nextPoint = start;
        double distanceToLine = 0;
        int maxSteps = 60;

        //while (true) {
        while (maxSteps > 0) {
            nextPoint = travelTo(nextPoint, windField, polarDiagram, leftSide, timeStepMilliseconds, edgeStart, edgeEnd);

            distanceToLine = getDistanceToLine(nextPoint.getPosition(), edgeStart, edgeEnd);

            // System.out.println("distanceToLine = " + distanceToLine + " meters");

            if (distanceToLine <= minimumDistanceMeters) {
                break;
            } else {
                path.add(nextPoint);
            }

            maxSteps--;
        }

        return path;
    }

    private static TimedPositionWithSpeed travelTo(TimedPositionWithSpeed start, WindFieldGenerator windField, PolarDiagram polarDiagram, boolean leftSide,
            long timeStepMilliseconds, Position edgeStart, Position edgeEnd) {

        TimePoint startTimePoint = start.getTimePoint();
        Position startPosition = start.getPosition();

        SpeedWithBearing windSpeedWithBearing = windField.getWind(new TimedPositionImpl(startTimePoint, startPosition));
        polarDiagram.setWind(windSpeedWithBearing);

        // TimePoint endTimePoint = new MillisecondsTimePoint(startTimePoint.asMillis() + timeStepMilliseconds);
        // Bearing[] bearings = polarDiagram.optimalDirectionsUpwind();
        //
        // Bearing bearing1 = bearings[0];
        // SpeedWithBearing boatSpeedWithBearing1 = polarDiagram.getSpeedAtBearing(bearing1);
        // Position endPosition1 = boatSpeedWithBearing1.travelTo(startPosition, startTimePoint, endTimePoint);
        // SpeedWithBearing endWind1 = windField.getWind(new TimedPositionImpl(endTimePoint, endPosition1));
        // TimedPositionWithSpeed end1 = new TimedPositionWithSpeedImpl(endTimePoint, endPosition1, endWind1);
        // double distance1 = getDistanceToLine(endPosition1, edgeStart, edgeEnd);
        //
        // Bearing bearing2 = bearings[1];
        // SpeedWithBearing boatSpeedWithBearing2 = polarDiagram.getSpeedAtBearing(bearing2);
        // Position endPosition2 = boatSpeedWithBearing2.travelTo(startPosition, startTimePoint, endTimePoint);
        // SpeedWithBearing endWind2 = windField.getWind(new TimedPositionImpl(endTimePoint, endPosition2));
        // TimedPositionWithSpeed end2 = new TimedPositionWithSpeedImpl(endTimePoint, endPosition2, endWind2);
        // double distance2 = getDistanceToLine(endPosition2, edgeStart, edgeEnd);
        //
        // return (distance1 < distance2) ? end1 : end2;

        Bearing bearing = polarDiagram.optimalDirectionsUpwind()[leftSide ? 0 : 1];
        // double degrees = bearing.getDegrees();
        // System.out.println("degrees = " + degrees);
        // bearing = new DegreeBearingImpl(degrees + 180);

        SpeedWithBearing boatSpeedWithBearing = polarDiagram.getSpeedAtBearing(bearing);

        TimePoint endTimePoint = new MillisecondsTimePoint(startTimePoint.asMillis() + timeStepMilliseconds);
        Position endPosition = boatSpeedWithBearing.travelTo(startPosition, startTimePoint, endTimePoint);
        Wind endWind = windField.getWind(new TimedPositionImpl(endTimePoint, endPosition));

        TimedPositionWithSpeed end = new TimedPositionWithSpeedImpl(endTimePoint, endPosition, endWind);

        return end;
    }

    public static Position getProjectionOnLine(Position point, Position segment1Start, Position segment1End) {

        double xA = segment1Start.getLatDeg();
        double yA = segment1Start.getLngDeg();

        double xB = segment1End.getLatDeg();
        double yB = segment1End.getLngDeg();

        double xC = point.getLatDeg();
        double yC = point.getLngDeg();

        double m = (yB - yA) / (xB - xA);
        double b = (xB * yA - xA * yB) / (xB - xA);

        double x = (m * yC + xC - m * b) / (m * m + 1);
        double y = m * x + b;

        return new DegreePosition(x, y);
    }

    public static double getDistanceToLine(Position point, Position segment1Start, Position segment1End) {
        return point.getDistance(getProjectionOnLine(point, segment1Start, segment1End)).getMeters();
    }

}
