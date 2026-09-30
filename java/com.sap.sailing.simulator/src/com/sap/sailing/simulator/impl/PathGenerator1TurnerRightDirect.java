package com.sap.sailing.simulator.impl;

import java.util.LinkedList;

import com.sap.sailing.simulator.Grid;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PathGenerator1TurnerRightDirect extends PathGeneratorBase {

    public PathGenerator1TurnerRightDirect(SimulationParameters params) {
        this.parameters = params;
    }

    @Override
    public Path getPath() {
        this.algorithmStartTime = MillisecondsTimePoint.now();

        // retrieve simulation parameters
        Grid boundary = new RectangularGrid(this.parameters.getCourse().get(0), this.parameters
                .getCourse().get(1));// simulationParameters.getBoundaries();
        WindFieldGenerator windField = this.parameters.getWindField();
        PolarDiagram polarDiagram = this.parameters.getBoatPolarDiagram();
        Position start = this.parameters.getCourse().get(0);
        Position end = this.parameters.getCourse().get(1);
        TimePoint startTime = windField.getStartTime();// new MillisecondsTimePoint(0);

        Distance courseLength = start.getDistance(end);

        // the solution path
        LinkedList<TimedPositionWithSpeed> lst = null;
        // the minimal one-turn time

        Long timeResolution = 120000L;
        boolean turned = true;
        // boolean outOfBounds = false;
        Long minTurn = Long.MAX_VALUE;
        int turningStep = 0;

        while (turned) {
            LinkedList<TimedPositionWithSpeed> tempLst = new LinkedList<TimedPositionWithSpeed>();
            Position currentPosition = start;
            TimePoint currentTime = startTime;
            turned = false;
            // outOfBounds = false;
            turningStep++;
            int currentStep = 0;

            while (true) {

                SpeedWithBearing currWind = windField.getWind(new TimedPositionImpl(currentTime, currentPosition));
                polarDiagram.setWind(currWind);
                TimePoint nextTime = new MillisecondsTimePoint(currentTime.asMillis() + timeResolution);
                tempLst.add(new TimedPositionWithSpeedImpl(currentTime, currentPosition, currWind));

                if (currentStep >= turningStep) {
                    turned = true;
                    nextTime = new MillisecondsTimePoint(nextTime.asMillis() + polarDiagram.getTurnLoss());
                }

                if (!turned) {
                    Bearing direction = polarDiagram.optimalDirectionsUpwind()[1];
                    // for(Bearing b: polarDiagram.optimalDirectionsUpwind())
                    // if(polarDiagram.getWindSide(b) == PolarDiagram.WindSide.LEFT)
                    // direction = b;
                    SpeedWithBearing currSpeed = polarDiagram.getSpeedAtBearing(direction);
                    currentPosition = currSpeed.travelTo(currentPosition, currentTime, nextTime);
                }
                if (turned) {
                    Bearing direction1 = currentPosition.getBearingGreatCircle(end);
                    Bearing direction2 = polarDiagram.optimalDirectionsUpwind()[0];
                    // for(Bearing b: polarDiagram.optimalDirectionsUpwind())
                    // if(polarDiagram.getWindSide(b) == PolarDiagram.WindSide.RIGHT)
                    // direction2 = b;
                    SpeedWithBearing currSpeed1 = polarDiagram.getSpeedAtBearing(direction1);
                    SpeedWithBearing currSpeed2 = polarDiagram.getSpeedAtBearing(direction2);
                    Position nextPosition1 = currSpeed1.travelTo(currentPosition, currentTime, nextTime);
                    Position nextPosition2 = currSpeed2.travelTo(currentPosition, currentTime, nextTime);
                    // nextPosition2.
                    if (nextPosition1.getDistance(end).compareTo(nextPosition2.getDistance(end)) < 0
                            && Math.abs(direction1.getDifferenceTo(direction2).getDegrees()) < 45.0) {
                        currentPosition = nextPosition1;
                    } else {
                        currentPosition = nextPosition2;
                    }
                }

                currentStep++;
                // System.out.println(currentStep + "/" + turningStep + "/" + turned);
                currentTime = nextTime;

                if (currentTime.asMillis() > minTurn) {
                    // System.out.println("out of time");
                    break;
                }
                if (!boundary.inBounds(currentPosition)||this.isTimedOut()) {
                    // outOfBounds = true;
                    // System.out.println("out of bounds");
                    break;
                }
                if (currentPosition.getDistance(end).compareTo(courseLength.scale(0.005)) < 0) {
                    minTurn = currentTime.asMillis();
                    lst = new LinkedList<TimedPositionWithSpeed>(tempLst);
                    Bearing directionToEnd = currentPosition.getBearingGreatCircle(end);
                    SpeedWithBearing crtWind = windField.getWind(new TimedPositionImpl(currentTime, currentPosition));
                    polarDiagram.setWind(crtWind);
                    Speed speedToEnd = polarDiagram.getSpeedAtBearing(directionToEnd);
                    Distance distanceToEnd = currentPosition.getDistance(end);
                    Long timeToEnd = (long) (1000.0 * distanceToEnd.getMeters() / speedToEnd.getMetersPerSecond());
                    TimePoint endTime = new MillisecondsTimePoint(currentTime.asMillis() + timeToEnd);
                    lst.addLast(new TimedPositionWithSpeedImpl(endTime, end, crtWind));
                    // System.out.println("end reached!!!");
                    break;
                }

            }

        }

        if (lst != null) {
            // lst.addLast(new TimedPositionWithSpeedImpl(new
            // MillisecondsTimePoint(lst.getLast().getTimePoint().asMillis() + timeResolution), end,
            // lst.getLast().getSpeed()));
            return new PathImpl(lst, windField, this.algorithmTimedOut);
        } else {
            return null;
        }

    }

}
