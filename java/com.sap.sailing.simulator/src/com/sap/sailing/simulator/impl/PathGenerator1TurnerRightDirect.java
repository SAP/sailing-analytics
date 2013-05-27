package com.sap.sailing.simulator.impl;

import java.util.LinkedList;

import com.sap.sailing.domain.base.SpeedWithBearing;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.simulator.Boundary;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;

public class PathGenerator1TurnerRightDirect extends PathGeneratorBase {

    public PathGenerator1TurnerRightDirect(SimulationParameters params) {
        this.parameters = params;
    }

    @Override
    public Path getPath() {

        // retrieve simulation parameters
        Boundary boundary = new RectangularBoundary(this.parameters.getCourse().get(0), this.parameters
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
                if (!boundary.isWithinBoundaries(currentPosition)) {
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
            return new PathImpl(lst, windField);
        } else {
            return null;
        }

    }

}
