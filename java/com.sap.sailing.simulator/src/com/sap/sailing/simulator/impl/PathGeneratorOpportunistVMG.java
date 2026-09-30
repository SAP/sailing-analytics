package com.sap.sailing.simulator.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PathGeneratorOpportunistVMG extends PathGeneratorBase {

    private static Logger logger = Logger.getLogger("com.sap.sailing");
    SimulationParameters simulationParameters;
    int maxLeft;
    int maxRight;
    boolean startLeft;

    public PathGeneratorOpportunistVMG(SimulationParameters params) {
        simulationParameters = params;
    }

    public void setEvaluationParameters(int maxLeftVal, int maxRightVal, boolean startLeftVal) {
        this.maxLeft = maxLeftVal;
        this.maxRight = maxRightVal;
        this.startLeft = startLeftVal;
    }

    @Override
    public Path getPath() {
        this.algorithmStartTime = MillisecondsTimePoint.now();

        WindFieldGenerator wf = simulationParameters.getWindField();
        PolarDiagram pd = simulationParameters.getBoatPolarDiagram();
        Position start = simulationParameters.getCourse().get(0);
        Position end = simulationParameters.getCourse().get(1);

        TimePoint startTime = wf.getStartTime();// new MillisecondsTimePoint(0);
        List<TimedPositionWithSpeed> lst = new ArrayList<TimedPositionWithSpeed>();

        Position currentPosition = start;
        TimePoint currentTime = startTime;

        int prevDirection = -1;
        long turnloss = pd.getTurnLoss(); // time lost when doing a turn
        long windpred = 1000; // time used to predict wind, i.e. hypothetical sailors prediction
        double fracFinishPhase = 0.05;

        TimePoint leftTime;
        TimePoint rightTime;

        Wind wndStart = wf.getWind(new TimedPositionWithSpeedImpl(startTime, start, null));
        logger.fine("wndStart speed:" + wndStart.getKnots() + " angle:" + wndStart.getBearing().getDegrees());

        pd.setWind(wndStart);
        Bearing bearTarget = currentPosition.getBearingGreatCircle(end);
        pd.setTargetDirection(bearTarget);
        SpeedWithBearing spdStart = pd.getSpeedAtBearing(bearTarget);

        lst.add(new TimedPositionWithSpeedImpl(startTime, start, spdStart));
        long timeStep = wf.getTimeStep().asMillis();
        logger.info("Time step :" + timeStep);
        // while there is more than 5% of the total distance to the finish

        //
        // StrategicPhase: start & intermediate course until close to target
        //
        SpeedWithBearing slft = null;
        SpeedWithBearing srght = null;
        while ((currentPosition.getDistance(end).compareTo(start.getDistance(end).scale(fracFinishPhase)) > 0)&&(!this.isTimedOut())) {

            long nextTimeVal = currentTime.asMillis() + timeStep;// + 30000;
            TimePoint nextTime = new MillisecondsTimePoint(nextTimeVal);

            Wind cWind = wf.getWind(new TimedPositionWithSpeedImpl(currentTime, currentPosition, null));
            logger.fine("cWind speed:" + cWind.getKnots() + " angle:" + cWind.getBearing().getDegrees());
            pd.setWind(cWind);
            bearTarget = currentPosition.getBearingGreatCircle(end);
            pd.setTargetDirection(bearTarget);

            // get wind of target direction
            Bearing wLft = pd.optimalDirectionsUpwind()[0];
            Bearing wRght = pd.optimalDirectionsUpwind()[1];

            SpeedWithBearing sWLft = pd.getSpeedAtBearing(wLft);
            SpeedWithBearing sWRght = pd.getSpeedAtBearing(wRght);
            logger.fine("left boat speed:" + sWLft.getKnots() + " angle:" + sWLft.getBearing().getDegrees() + "  right boat speed:"
                    + sWRght.getKnots() + " angle:" + sWRght.getBearing().getDegrees());

            TimePoint wTime = new MillisecondsTimePoint(currentTime.asMillis() + windpred);
            Position pWLft = sWLft.travelTo(currentPosition, currentTime, wTime);
            Position pWRght = sWRght.travelTo(currentPosition, currentTime, wTime);

            logger.fine("current Pos:" + currentPosition.getLatDeg() + "," + currentPosition.getLngDeg());
            logger.fine("left    Pos:" + pWLft.getLatDeg() + "," + pWLft.getLngDeg());
            logger.fine("right   Pos:" + pWRght.getLatDeg() + "," + pWRght.getLngDeg());

            Wind lWind = wf.getWind(new TimedPositionWithSpeedImpl(currentTime, pWLft, null));
            logger.fine("lWind speed:" + lWind.getKnots() + " angle:" + lWind.getBearing().getDegrees());
            pd.setWind(lWind);
            bearTarget = currentPosition.getBearingGreatCircle(end);
            pd.setTargetDirection(bearTarget);
            SpeedWithBearing lft = pd.optimalVMGUpwind()[0];
            slft = pd.getSpeedAtBearing(lft.getBearing());

            Wind rWind = wf.getWind(new TimedPositionWithSpeedImpl(currentTime, pWRght, null));
            logger.fine("rWind speed:" + rWind.getKnots() + " angle:" + rWind.getBearing().getDegrees());
            pd.setWind(rWind);
            bearTarget = currentPosition.getBearingGreatCircle(end);
            pd.setTargetDirection(bearTarget);
            SpeedWithBearing rght = pd.optimalVMGUpwind()[1];
            srght = pd.getSpeedAtBearing(rght.getBearing());
            logger.fine("left boat speed:" + slft.getKnots() + " angle:" + slft.getBearing().getDegrees() + "  right boat speed:"
                    + srght.getKnots() + " angle:" + srght.getBearing().getDegrees());

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

            Position plft = slft.travelTo(currentPosition, currentTime, leftTime);
            Position prght = srght.travelTo(currentPosition, currentTime, rightTime);

            if (prevDirection == -1) {

                if (startLeft) {
                    lst.add(new TimedPositionWithSpeedImpl(nextTime, plft, slft));
                    currentPosition = plft;
                    prevDirection = 1;
                } else {
                    lst.add(new TimedPositionWithSpeedImpl(nextTime, prght, srght));
                    currentPosition = prght;
                    prevDirection = 2;
                }

            } else {

                double eps = 0.75;
                double accDivisor = 100.0;
                double leftSpeed = Math.round(lft.getKnots() * accDivisor) / accDivisor;
                double rightSpeed = Math.round(rght.getKnots() * accDivisor) / accDivisor;

                if (prevDirection == 1) {

                    if (leftSpeed > eps * rightSpeed) {
                        lst.add(new TimedPositionWithSpeedImpl(nextTime, plft, slft));
                        currentPosition = plft;
                    } else {
                        lst.add(new TimedPositionWithSpeedImpl(nextTime, prght, srght));
                        currentPosition = prght;
                        prevDirection = 2;
                    }

                } else {

                    if (rightSpeed > eps * leftSpeed) {
                        lst.add(new TimedPositionWithSpeedImpl(nextTime, prght, srght));
                        currentPosition = prght;
                    } else {
                        lst.add(new TimedPositionWithSpeedImpl(nextTime, plft, slft));
                        currentPosition = plft;
                        prevDirection = 1;
                    }

                }

            }

            currentTime = nextTime;
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

            gen1Turner.setEvaluationParameters(true, currentPosition, end, leftGoingTime, wf.getTimeStep().asMillis()
                    / (5 * 3), 100, 0.1, true);
            Path leftPath = gen1Turner.getPath();

            gen1Turner.setEvaluationParameters(false, currentPosition, end, rightGoingTime, wf.getTimeStep().asMillis()
                    / (5 * 3), 100, 0.1, true);
            Path rightPath = gen1Turner.getPath();

            if ((leftPath.getPathPoints() != null) && (rightPath.getPathPoints() != null)) {
                if (leftPath.getPathPoints().get(leftPath.getPathPoints().size() - 1).getTimePoint().asMillis() <= rightPath
                        .getPathPoints().get(rightPath.getPathPoints().size() - 1).getTimePoint().asMillis()) {
                    lst.addAll(leftPath.getPathPoints());
                } else {
                    lst.addAll(rightPath.getPathPoints());
                }
            } else if (leftPath.getPathPoints() != null) {
                lst.addAll(leftPath.getPathPoints());
            } else if (rightPath.getPathPoints() != null) {
                lst.addAll(rightPath.getPathPoints());
            }
        }
        
        return new PathImpl(lst, wf, this.algorithmTimedOut);
    }

 }
