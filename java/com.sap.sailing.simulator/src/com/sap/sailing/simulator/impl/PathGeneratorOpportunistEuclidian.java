package com.sap.sailing.simulator.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.LegType;
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
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PathGeneratorOpportunistEuclidian extends PathGeneratorBase {

    private static Logger logger = Logger.getLogger("com.sap.sailing");
    SimulationParameters simulationParameters;
    int turns;
    int maxLeft;
    int maxRight;
    MaximumTurnTimes maxTurnTimes;
    boolean startLeft;
    boolean upwindLeg = false;

    public PathGeneratorOpportunistEuclidian(SimulationParameters params) {
        PolarDiagram polarDiagramClone = new PolarDiagramBase((PolarDiagramBase)params.getBoatPolarDiagram());
        simulationParameters = new SimulationParametersImpl(params.getCourse(), polarDiagramClone, params.getWindField(),
                params.getSimuStep(), params.getMode(), params.showOmniscient(), params.showOpportunist(), params.getLegType());
    }

    public void setEvaluationParameters(int maxLeft, int maxRight, boolean startLeft) {
        this.maxLeft = maxLeft;
        this.maxRight = maxRight;
        this.startLeft = startLeft;
    }

    public void setEvaluationParameters(MaximumTurnTimes maxTurnTimes, boolean startLeft) {
        this.maxTurnTimes = maxTurnTimes;
        this.startLeft = startLeft;
    }

    public int getTurns() {
        return turns;
    }

    @Override
    public Path getPath() {
        this.algorithmStartTime = MillisecondsTimePoint.now();

        WindFieldGenerator wf = simulationParameters.getWindField();
        PolarDiagram pd = simulationParameters.getBoatPolarDiagram();

        Position start = simulationParameters.getCourse().get(0);
        Position end = simulationParameters.getCourse().get(1);

        // test downwind: exchange start and end
        // Position start = simulationParameters.getCourse().get(1);
        // Position end = simulationParameters.getCourse().get(0);

        TimePoint startTime = wf.getStartTime();// new MillisecondsTimePoint(0);
        List<TimedPositionWithSpeed> path = new ArrayList<TimedPositionWithSpeed>();

        Position currentPosition = start;
        TimePoint currentTime = startTime;
        double currentHeight = start.getDistance(end).getMeters();

        int stepsLeft = 0;
        int stepsRight = 0;
        boolean allLeft = true;
        boolean allRight = true;

        int prevDirection = -1;
        long turnloss = pd.getTurnLoss(); // 4000; // time lost when doing a turn
        long windpred = 1000; // time used to predict wind, i.e. hypothetical sailors prediction
        double fracFinishPhase = 0.05;

        TimePoint leftTime;
        TimePoint rightTime;

        Wind wndStart = wf.getWind(new TimedPositionWithSpeedImpl(startTime, start, null));
        logger.finest("wndStart speed:" + wndStart.getKnots() + " angle:" + wndStart.getBearing().getDegrees());
        pd.setWind(wndStart);
        Bearing bearStart = currentPosition.getBearingGreatCircle(end);
        // SpeedWithBearing spdStart = pd.getSpeedAtBearing(bearStart);
        path.add(new TimedPositionWithSpeedImpl(startTime, start, wndStart));

        long timeStep = wf.getTimeStep().asMillis() / 2;
        logger.fine("Time step :" + timeStep);
        // while there is more than 5% of the total distance to the finish

        String legType = "none";
        if (this.simulationParameters.getLegType() == null) {
            Bearing bearRCWind = wndStart.getBearing().getDifferenceTo(bearStart);
            legType = "downwind";
            this.upwindLeg = false;
            if ((Math.abs(bearRCWind.getDegrees()) > 90.0) && (Math.abs(bearRCWind.getDegrees()) < 270.0)) {
                legType = "upwind";
                this.upwindLeg = true;
            }
        } else {
            if (this.simulationParameters.getLegType() == LegType.UPWIND) {
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
            turnloss = turnloss / timeStepScale;
        }

        if (maxTurnTimes != null) {
            if ((maxTurnTimes.left > 0) || (maxTurnTimes.right > 0)) {
                this.maxLeft = (int) Math.floor((double) maxTurnTimes.left / (double) timeStep);
                this.maxRight = (int) Math.floor((double) maxTurnTimes.right / (double) timeStep);
            }
        }
        logger.fine("Leg Direction: " + legType);

        //
        // StrategicPhase: start & intermediate course until close to target
        //
        turns = 0;
        SpeedWithBearing slft = null;
        SpeedWithBearing srght = null;
        while ((currentHeight > 0)
                && (currentPosition.getDistance(end).compareTo(start.getDistance(end).scale(fracFinishPhase)) > 0)
                && (path.size() < 500) && (!this.isTimedOut())) {

            // TimePoint nextTime = new MillisecondsTimePoint(currentTime.asMillis() + 30000);

            long nextTimeVal = currentTime.asMillis() + timeStep;// + 30000;
            TimePoint nextTime = new MillisecondsTimePoint(nextTimeVal);

            Wind cWind = wf.getWind(new TimedPositionWithSpeedImpl(currentTime, currentPosition, null));
            logger.finest("cWind speed:" + cWind.getKnots() + " angle:" + cWind.getBearing().getDegrees());
            // System.out.println("Start WindBear: " + (cWind.getBearing().getDegrees() - bearStart.getDegrees()));
            pd.setWind(cWind);

            // get wind of direction
            Bearing wLft;
            Bearing wRght;
            if (this.upwindLeg) {
                wLft = pd.optimalDirectionsUpwind()[0];
                wRght = pd.optimalDirectionsUpwind()[1];
            } else {
                wLft = pd.optimalDirectionsDownwind()[0];
                wRght = pd.optimalDirectionsDownwind()[1];
            }
            // Bearing wDirect = currentPosition.getBearingGreatCircle(end);

            // SpeedWithBearing sWDirect = pd.getSpeedAtBearing(wDirect);
            SpeedWithBearing sWLft = pd.getSpeedAtBearing(wLft);
            SpeedWithBearing sWRght = pd.getSpeedAtBearing(wRght);
            logger.finest("left boat speed:" + sWLft.getKnots() + " angle:" + sWLft.getBearing().getDegrees()
                    + "  right boat speed:" + sWRght.getKnots() + " angle:" + sWRght.getBearing().getDegrees());

            TimePoint wTime = new MillisecondsTimePoint(currentTime.asMillis() + windpred);
            // Position pWDirect = sWDirect.travelTo(currentPosition, currentTime, wTime);
            Position pWLft = sWLft.travelTo(currentPosition, currentTime, wTime);
            Position pWRght = sWRght.travelTo(currentPosition, currentTime, wTime);

            logger.finest("current Pos:" + currentPosition.getLatDeg() + "," + currentPosition.getLngDeg());
            logger.finest("left    Pos:" + pWLft.getLatDeg() + "," + pWLft.getLngDeg());
            logger.finest("right   Pos:" + pWRght.getLatDeg() + "," + pWRght.getLngDeg());

            // calculate next step
            // Wind dWind = wf.getWind(new TimedPositionWithSpeedImpl(currentTime, pWDirect, null));
            // logger.finest("dWind speed:" + dWind.getKnots() + " angle:" + dWind.getBearing().getDegrees());
            // pd.setWind(dWind);
            // Bearing direct = currentPosition.getBearingGreatCircle(end);
            // SpeedWithBearing sdirect = pd.getSpeedAtBearing(direct);

            Wind lWind = wf.getWind(new TimedPositionWithSpeedImpl(currentTime, pWLft, null));
            logger.finest("lWind speed:" + lWind.getKnots() + " angle:" + lWind.getBearing().getDegrees());
            // System.out.println("Left WindBear: " + (lWind.getBearing().getDegrees() - bearStart.getDegrees()));
            pd.setWind(lWind);
            Bearing lft;
            if (this.upwindLeg) {
                lft = pd.optimalDirectionsUpwind()[0];
            } else {
                lft = pd.optimalDirectionsDownwind()[0];
            }
            slft = pd.getSpeedAtBearing(lft);

            Wind rWind = wf.getWind(new TimedPositionWithSpeedImpl(currentTime, pWRght, null));
            logger.finest("rWind speed:" + rWind.getKnots() + " angle:" + rWind.getBearing().getDegrees());
            // System.out.println("Right WindBear: " + (rWind.getBearing().getDegrees() - bearStart.getDegrees()));
            pd.setWind(rWind);
            Bearing rght;
            if (this.upwindLeg) {
                rght = pd.optimalDirectionsUpwind()[1];
            } else {
                rght = pd.optimalDirectionsDownwind()[1];
            }
            srght = pd.getSpeedAtBearing(rght);

            // System.out.println("Bearings : " + (lft.getDegrees() - bearStart.getDegrees()) + " "
            // + (rght.getDegrees() - bearStart.getDegrees()));

            logger.finest("left boat speed:" + slft.getKnots() + " angle:" + slft.getBearing().getDegrees()
                    + "  right boat speed:" + srght.getKnots() + " angle:" + srght.getBearing().getDegrees());

            /*
             * if (prevDirection == 0) { directTime = new MillisecondsTimePoint(nextTimeVal); leftTime = new
             * MillisecondsTimePoint(nextTimeVal - turnloss); rightTime = new MillisecondsTimePoint(nextTimeVal -
             * turnloss); } else
             */
            if (prevDirection == 1) {
                leftTime = new MillisecondsTimePoint(nextTimeVal);
                rightTime = new MillisecondsTimePoint(nextTimeVal - turnloss);
            } else if (prevDirection == 2) {
                leftTime = new MillisecondsTimePoint(nextTimeVal - turnloss);
                rightTime = new MillisecondsTimePoint(nextTimeVal);
            } else {
                leftTime = new MillisecondsTimePoint(nextTimeVal);
                rightTime = new MillisecondsTimePoint(nextTimeVal);
            }

            // Position pdirect = sdirect.travelTo(currentPosition, currentTime, directTime);
            Position plft = slft.travelTo(currentPosition, currentTime, leftTime);
            Position prght = srght.travelTo(currentPosition, currentTime, rightTime);

            // System.out.println("RelBearLeft : " + (slft.getBearing().getDegrees() - bearStart.getDegrees()));
            // System.out.println("RelBearRight: " + (srght.getBearing().getDegrees() - bearStart.getDegrees()));

            // Distance ddirect = pdirect.getDistance(end);
            Distance dlft = plft.getDistance(end);
            Distance drght = prght.getDistance(end);

            double lDistCM = Math.round(dlft.getMeters() * 1000.) / 1000.;
            double rDistCM = Math.round(drght.getMeters() * 1000.) / 1000.;

            /*
             * if (ddirect.compareTo(dlft) <= 0 && ddirect.compareTo(drght) <= 0) { lst.add(new
             * TimedPositionWithSpeedImpl(nextTime, pdirect, sdirect)); currentPosition = pdirect; prevDirection = 0; }
             */

            if (prevDirection == -1) {

                if (startLeft) {
                    allRight = false;
                    path.add(new TimedPositionWithSpeedImpl(nextTime, plft, lWind));
                    currentPosition = plft;
                    if (prevDirection == 2) {
                        allLeft = false;
                        turns++;
                    } else {
                        stepsLeft++;
                    }
                    prevDirection = 1;
                } else {
                    allLeft = false;
                    path.add(new TimedPositionWithSpeedImpl(nextTime, prght, rWind));
                    currentPosition = prght;
                    if (prevDirection == 1) {
                        allRight = false;
                        turns++;
                    } else {
                        stepsRight++;
                    }
                    prevDirection = 2;
                }

            } else {
                // System.out.println("Distance Left - Right: "+lDistCM+" - "+ rDistCM);
                if (((lDistCM <= rDistCM) && (!allLeft || (stepsLeft < maxLeft)))
                        || (allRight && (stepsRight >= maxRight))) {
                    path.add(new TimedPositionWithSpeedImpl(nextTime, plft, lWind));
                    currentPosition = plft;
                    if (prevDirection == 2) {
                        allLeft = false;
                        turns++;
                    } else {
                        stepsLeft++;
                    }
                    prevDirection = 1;
                } else {
                    // if (((drght.compareTo(dlft) < 0)&&(stepsRight < maxRight))||(stepsLeft >= maxLeft)) {
                    path.add(new TimedPositionWithSpeedImpl(nextTime, prght, rWind));
                    currentPosition = prght;
                    if (prevDirection == 1) {
                        allRight = false;
                        turns++;
                    } else {
                        stepsRight++;
                    }
                    prevDirection = 2;
                }
            }

            currentTime = nextTime;
            Position posHeight = currentPosition.projectToLineThrough(start, bearStart);
            currentHeight = start.getDistance(end).getMeters() - posHeight.getDistance(start).getMeters();
            // System.out.println("Height to Target: "+height);
        }

        if (!this.isTimedOut()) {
            //
            // FinishPhase: get 1-turners to finalize course
            //
            PathGenerator1Turner gen1Turner = new PathGenerator1Turner(simulationParameters);
            TimePoint leftGoingTime;
            TimePoint rightGoingTime;
            if (prevDirection == 1) {
                leftGoingTime = currentTime;
                rightGoingTime = new MillisecondsTimePoint(currentTime.asMillis() + turnloss);
            } else {
                leftGoingTime = new MillisecondsTimePoint(currentTime.asMillis() + turnloss);
                rightGoingTime = currentTime;
            }

            gen1Turner.setEvaluationParameters(true, currentPosition, end, leftGoingTime, timeStep / (5 * 3), 100, 0.1,
                    this.upwindLeg);
            Path leftPath = gen1Turner.getPath();

            gen1Turner.setEvaluationParameters(false, currentPosition, end, rightGoingTime, timeStep / (5 * 3), 100,
                    0.1, this.upwindLeg);
            Path rightPath = gen1Turner.getPath();

            if ((leftPath.getPathPoints() != null) && (rightPath.getPathPoints() != null)) {
                if (leftPath.getPathPoints().get(leftPath.getPathPoints().size() - 1).getTimePoint().asMillis() <= rightPath
                        .getPathPoints().get(rightPath.getPathPoints().size() - 1).getTimePoint().asMillis()) {
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

        return new PathImpl(path, wf, getTurns(), this.algorithmTimedOut);

    }

}
