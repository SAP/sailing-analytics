package com.sap.sailing.simulator.windfield.impl;

import java.util.logging.Logger;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.simulator.Grid;
import com.sap.sailing.simulator.TimedPosition;
import com.sap.sailing.simulator.windfield.WindControlParameters;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

public class WindFieldGeneratorBlastImpl extends WindFieldGeneratorImpl implements WindFieldGenerator {

    private static final long serialVersionUID = -188939912966537200L;

    private SpeedWithBearing[][] speedWithBearing;

    private double blastSizeProbability = 70;
    private double blastEdgeProbability = 30;
    private double blastBearingMean = 0;
    private double blastBearingVar = 6;
    private double defaultWindSpeed = 0;
    private double defaultWindBearing = 0;
    private SpeedWithBearing defaultSpeedWithBearing;
    /**
     * Number of time units before we cycle through the gusts
     */
    public final int defaultTimeUnits = 20;
    private int timeUnits;

    private static Logger logger = Logger.getLogger(WindFieldGeneratorBlastImpl.class.getName());

    public WindFieldGeneratorBlastImpl(Grid boundary, WindControlParameters windParameters) {
        super(boundary, windParameters);
    }

    @Override
    public void generate(TimePoint start, TimePoint end, Duration step) {
        generate(start, end, step, windParameters.baseWindSpeed, windParameters.baseWindBearing);
    }

    protected void generate(TimePoint start, TimePoint end, Duration step, double defaultSpeed, double defaultBearing) {
        super.generate(start, end, step);
        // TODO Check the defaults
        setDefaultWindSpeed(defaultSpeed);
        setDefaultWindBearing(defaultBearing);
        defaultSpeedWithBearing = new KnotSpeedWithBearingImpl(defaultWindSpeed, new DegreeBearingImpl(
                defaultWindBearing));
        initializeSpeedWithBearing();
    }

    private void initializeSpeedWithBearing() {
        if (positions == null || positions.length < 1) {
            return;
        }

        int nrow = this.boundary.getResY() + 2*this.boundary.getBorderY();
        int ncol = this.boundary.getResX() + 2*this.boundary.getBorderX();

        timeUnits = nrow + defaultTimeUnits;
        if (startTime != null && endTime != null) {
            timeUnits = (int) ((endTime.asMillis() - startTime.asMillis()) / timeStep.asMillis()) + nrow;
            logger.info("Generating blasts for " + timeUnits + " rows & " + ncol + " columns");
        }
        speedWithBearing = new KnotSpeedWithBearingImpl[timeUnits][ncol];

        //System.out.println("Blast Wind:");
        for (int i = 0; i < timeUnits; ++i) {
            for (int j = 0; j < ncol; ++j) {
                if (isBlastSeed()) {
                    logger.fine("["+i+"]["+j+"] is  a blast seed");
                    double blastSpeed = getBlastSpeed();
                    double blastAngle = getBlastAngle();
                    SpeedWithBearing blastSpeedWithBearing = new KnotSpeedWithBearingImpl(blastSpeed,
                            new DegreeBearingImpl(blastAngle+defaultWindBearing));
                    growBlast(i, j, blastSpeedWithBearing);

                } else {
                    if (speedWithBearing[i][j] == null) {
                        speedWithBearing[i][j] = defaultSpeedWithBearing;
                    }
                }
                //System.out.println("speed: "+speedWithBearing[i][j].getMetersPerSecond()+" angle: "+speedWithBearing[i][j].getBearing().getDegrees());
            }
        }
    }

    private void growBlast(int rowIndex, int colIndex, SpeedWithBearing blastSpeedWithBearing) {

        if (speedWithBearing[rowIndex][colIndex] != null) {
            return;
        }

        int blastSize = (int) Math.min(getBlastSize(), windParameters.maxBlastSize);
        logger.fine("Blast Size:" + blastSize);
        int nrow = timeUnits;
        int ncol = this.boundary.getResX() + 2*this.boundary.getBorderX();
        int hSpanStart = Math.max(0, colIndex - blastSize / 2);
        int hSpanEnd = Math.min(colIndex + blastSize - blastSize / 2, ncol - 1);
        int vSpan = Math.min(rowIndex + blastSize, nrow - 1);
        for (int i = rowIndex; i <= vSpan; ++i) {
            for (int j = hSpanStart; j <= hSpanEnd; ++j) {
                speedWithBearing[i][j] = blastSpeedWithBearing;
            }
        }

        if (hSpanEnd - hSpanStart > 1) {
            for (int i = rowIndex; i <= vSpan; ++i) {
                if (isBlastCell()) {
                    speedWithBearing[i][hSpanStart] = blastSpeedWithBearing;
                } else {
                    speedWithBearing[i][hSpanStart] = defaultSpeedWithBearing;
                }
                if (isBlastCell()) {
                    speedWithBearing[i][hSpanEnd] = blastSpeedWithBearing;
                } else {
                    speedWithBearing[i][hSpanEnd] = defaultSpeedWithBearing;
                }
            }
            for (int j = hSpanStart + 1; j <= hSpanEnd - 1; ++j) {
                if (isBlastCell()) {
                    speedWithBearing[rowIndex][j] = blastSpeedWithBearing;
                } else {
                    speedWithBearing[rowIndex][j] = defaultSpeedWithBearing;
                }
                if (isBlastCell()) {
                    speedWithBearing[vSpan][j] = blastSpeedWithBearing;
                } else {
                    speedWithBearing[vSpan][j] = defaultSpeedWithBearing;
                }
            }

        }
    }

    private boolean isBlastSeed() {
        return windParameters.getBlastRandomStreamManager().getRandomStream(BlastRandomSeedManagerImpl.BlastStream.SEED.name()).nextDouble() < windParameters.blastProbability / 100.0;
    }

    private boolean isBlastCell() {
        return windParameters.getBlastRandomStreamManager().getRandomStream(BlastRandomSeedManagerImpl.BlastStream.CELL.name()).nextDouble() > this.blastEdgeProbability / 100.0;
    }

    private double getBlastSpeed() {
        double bSpeedMean = windParameters.baseWindSpeed * (windParameters.blastWindSpeed / 100.0 - (defaultWindSpeed==0 ? 1. : 0.));
        double bSpeedVar = windParameters.baseWindSpeed * windParameters.blastWindSpeed / 100.0 * windParameters.blastWindSpeedVar / 100.0;
        BlastRandom speedStream = windParameters.getBlastRandomStreamManager().getRandomStream(BlastRandomSeedManagerImpl.BlastStream.SPEED.name());
        return Math.max(0.5*bSpeedMean, Math.min(1.5*bSpeedMean, speedStream.nextGaussian(bSpeedMean, bSpeedVar)));
    }

    private int getBlastSize() {
        BlastRandom sizeStream = windParameters.getBlastRandomStreamManager().getRandomStream(BlastRandomSeedManagerImpl.BlastStream.SIZE.name());
        return (1 + sizeStream.nextGeometric(blastSizeProbability / 100.0));
    }

    private double getBlastAngle() {
        BlastRandom bearingStream = windParameters.getBlastRandomStreamManager().getRandomStream(BlastRandomSeedManagerImpl.BlastStream.BEARING.name());
        return Math.max(-1.5*blastBearingVar, Math.min(1.5*blastBearingVar, bearingStream.nextGaussian(blastBearingMean, blastBearingVar)));
    }

    private SpeedWithBearing getSpeedWithBearing(TimedPosition timedPosition) {
        Position p = timedPosition.getPosition();
        Util.Pair<Integer, Integer> positionIndex = getPositionIndex(p);
        if (positionIndex != null) {
            int rowIndex = positionIndex.getA() + this.boundary.getBorderY();
            int colIndex = positionIndex.getB() + this.boundary.getBorderX();
            int timeIndex = 0;
            if (timedPosition.getTimePoint() != null) {
                timeIndex = getTimeIndex(timedPosition.getTimePoint());
            }
            return speedWithBearing[(rowIndex + timeIndex) % timeUnits][colIndex];
        } else {
            logger.severe("Error finding position " + p);
        }
        return null;
    }

    @Override
    public Wind getWind(TimedPosition timedPosition) {

        return new WindImpl(timedPosition.getPosition(), timedPosition.getTimePoint(),
                getSpeedWithBearing(timedPosition));

    }

    public void setDefaultWindSpeed(double speed) {
        this.defaultWindSpeed = speed;
    }

    public double getDefaultWindSpeed() {
        return this.defaultWindSpeed;
    }

    public void setDefaultWindBearing(double bearing) {
        this.defaultWindBearing = bearing;
    }

    public double getDefaultWindBearing() {
        return this.defaultWindBearing;
    }
}
