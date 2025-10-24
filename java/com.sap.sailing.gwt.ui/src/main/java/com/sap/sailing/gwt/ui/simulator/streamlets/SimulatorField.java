package com.sap.sailing.gwt.ui.simulator.streamlets;

import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.maps.client.base.LatLng;
import com.google.gwt.maps.client.base.LatLngBounds;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.gwt.ui.shared.SimulatorWindDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldDTO;
import com.sap.sailing.gwt.ui.shared.WindFieldGenParamsDTO;
import com.sap.sailing.gwt.ui.simulator.StreamletParameters;
import com.sap.sse.common.Duration;

public class SimulatorField implements VectorField {
    private boolean swarmDebug = false;

    private Position rcStart;
    private Position rcEnd;

    private final int resY;
    private final int resX;

    private final int borderY;
    private final int borderX;

    private final Position bdA;
    private final Position bdB;
    private final Position bdC;

    private final double xScale;
    private double motionFactor;
    private double lineBase;
    private double lineScale;
    private final double maxLength;
    private final double particleFactor;

    /**
     * The cosine of the field's average latitude (arithmetic average of NW's latitude and SE's latitude) which represents
     * the average ratio of pixels used per longitude and latitude angle in the Mercator projection. At the equator, this
     * ration is 1; towards the poles it gets less.
     */
    private final double lngScale;

    private final Position nvY;
    private final Position nvX;

    private final double[][][] data;
    private final Date startTime;
    private final Duration timeStep;
    private final CoordinateSystem coordinateSystem;

    public SimulatorField(WindFieldDTO windData, WindFieldGenParamsDTO windParams, StreamletParameters parameters, CoordinateSystem coordinateSystem) {
        this.coordinateSystem = coordinateSystem;
        this.startTime = windParams.getStartTime();
        this.timeStep = windParams.getTimeStep();
        this.rcStart = new DegreePosition(windData.windData.rcStart.getLatDeg(), windData.windData.rcStart.getLngDeg());
        this.rcEnd = new DegreePosition(windData.windData.rcEnd.getLatDeg(), windData.windData.rcEnd.getLngDeg());
        this.resX = windData.windData.resX;
        this.resY = windData.windData.resY;
        this.borderX = windData.windData.borderX;
        this.borderY = windData.windData.borderY;
        final double bdXi = (this.borderY + 0.5) / (this.resY - 1);
        final double bdPhi = 1.0 + 2 * bdXi;
        this.bdA = new DegreePosition(this.rcEnd.getLatDeg() + (this.rcEnd.getLatDeg() - this.rcStart.getLatDeg())
                * bdXi, this.rcEnd.getLngDeg() + (this.rcEnd.getLngDeg() - this.rcStart.getLngDeg()) * bdXi);
        this.bdB = new DegreePosition((this.rcStart.getLatDeg() - this.rcEnd.getLatDeg()) * bdPhi,
                (this.rcStart.getLngDeg() - this.rcEnd.getLngDeg()) * bdPhi);
        this.xScale = windData.windData.xScale;
		this.motionFactor = 0.0007 * parameters.motionScale;
		this.lineBase = parameters.lineBase;
		this.lineScale = parameters.lineScale;
        List<SimulatorWindDTO> gridData = windData.getMatrix();
        int p = 0;
        int imax = windParams.getyRes() + 2 * windParams.getBorderY();
        int jmax = windParams.getxRes() + 2 * windParams.getBorderX();
        int steps = gridData.size() / (imax * jmax);
        this.data = new double[steps][imax][2 * jmax];
        double maxWindSpeed = 0;
        double minWindSpeed = Double.MAX_VALUE;
        // copy the wind data from WindFieldDTO.getMatrix() to the data array, determining min/max wind speed
        for (int s = 0; s < steps; s++) {
            for (int i = 0; i < imax; i++) {
                for (int j = 0; j < jmax; j++) {
                    SimulatorWindDTO wind = gridData.get(p);
                    p++;
                    if (wind.trueWindSpeedInKnots > maxWindSpeed) {
                        maxWindSpeed = wind.trueWindSpeedInKnots;
                    }
                    if (wind.trueWindSpeedInKnots < minWindSpeed) {
                        minWindSpeed = wind.trueWindSpeedInKnots;
                    }
                    this.data[s][i][2 * j + 1] = wind.trueWindSpeedInKnots
                            * Math.cos(wind.trueWindBearingDeg * Math.PI / 180.0);
                    this.data[s][i][2 * j] = wind.trueWindSpeedInKnots
                            * Math.sin(wind.trueWindBearingDeg * Math.PI / 180.0);
                }
            }
        }
        this.maxLength = maxWindSpeed;
        this.particleFactor = 1.0;
        final double latAvg = (this.rcEnd.getLatDeg() + this.rcStart.getLatDeg()) / 2.;
        this.lngScale = Math.cos(latAvg * Math.PI / 180.0);
        final double difLat = this.rcEnd.getLatDeg() - this.rcStart.getLatDeg();
        final double difLng = (this.rcEnd.getLngDeg() - this.rcStart.getLngDeg()) * this.lngScale;
        final double difLen = Math.sqrt(difLat * difLat + difLng * difLng);
        this.nvY = new DegreePosition(difLat / difLen / difLen * (this.resY - 1), difLng / difLen / difLen
                * (this.resY - 1));
        double nrmLat = -difLng / difLen;
        double nrmLng = difLat / difLen;
        this.nvX = new DegreePosition(nrmLat / this.xScale / difLen * (this.resX - 1), nrmLng / this.xScale / difLen
                * (this.resX - 1));
        final Position gvX = new DegreePosition(nrmLat * this.xScale * difLen, nrmLng / this.lngScale * this.xScale * difLen);
        this.bdC = new DegreePosition(gvX.getLatDeg() * (this.resX + 2 * this.borderX - 1) / (this.resX - 1),
                gvX.getLngDeg() * (this.resX + 2 * this.borderX - 1) / (this.resX - 1));
    }

    private LatLng getInnerPosition(double factX, double factY) {
        double latDeg = this.bdA.getLatDeg() + factY * this.bdB.getLatDeg() + factX * this.bdC.getLatDeg();
        double lngDeg = this.bdA.getLngDeg() + factY * this.bdB.getLngDeg() + factX * this.bdC.getLngDeg();
        LatLng result = LatLng.newInstance(latDeg, lngDeg);
        if (swarmDebug && (!this.inBounds(result))) {
            GWT.log("random-position: out of bounds");
        }
        return result;
    }

    @Override
    public boolean inBounds(Position p) {
        Index idx = this.getIndex(p);
        boolean inBool = (idx.x >= 0) && (idx.x < (this.resX + 2 * this.borderX)) && (idx.y >= 0)
                && (idx.y < (this.resY + 2 * this.borderY));
        return inBool;
    }

    @Override
    public boolean inBounds(LatLng p) {
        return inBounds(coordinateSystem.getPosition(p));
    }

    private Vector interpolate(Position p, Date at) {
        int step = (int) ((at.getTime() - startTime.getTime()) / timeStep.asMillis());
        if (step >= this.data.length) {
            step = this.data.length - 1;
        }
        Neighbors idx = getNeighbors(p);
        if (swarmDebug
                && ((idx.xTop >= (this.resX + 2 * this.borderX)) || (idx.yTop >= (this.resY + 2 * this.borderY)))) {
            GWT.log("interpolate: out of range: " + idx.xTop + "  " + idx.yTop);
        }
        final double[][] dataAtStep = this.data[step];
        double avgX = dataAtStep[idx.yBot][2 * idx.xBot] * (1 - idx.yMod) * (1 - idx.xMod)
                + dataAtStep[idx.yTop][2 * idx.xBot] * idx.yMod * (1 - idx.xMod)
                + dataAtStep[idx.yBot][2 * idx.xTop] * (1 - idx.yMod) * idx.xMod
                + dataAtStep[idx.yTop][2 * idx.xTop] * idx.yMod * idx.xMod;
        double avgY = dataAtStep[idx.yBot][2 * idx.xBot + 1] * (1 - idx.yMod) * (1 - idx.xMod)
                + dataAtStep[idx.yTop][2 * idx.xBot + 1] * idx.yMod * (1 - idx.xMod)
                + dataAtStep[idx.yBot][2 * idx.xTop + 1] * (1 - idx.yMod) * idx.xMod
                + dataAtStep[idx.yTop][2 * idx.xTop + 1] * idx.yMod * idx.xMod;
        return new Vector(avgX, avgY);
    }

    @Override
    public Vector getVector(LatLng p, Date at) {
        return this.interpolate(coordinateSystem.getPosition(p), at);
    }

    private Index getIndex(Position p) {
        // calculate grid indexes
        final double latOffset = p.getLatDeg() - this.rcStart.getLatDeg();
        final double lngOffset = (p.getLngDeg() - this.rcStart.getLngDeg()) * this.lngScale;
        // closest grid point
        long yIdx = Math.round(latOffset * this.nvY.getLatDeg() + lngOffset * this.nvY.getLngDeg()) + this.borderY;
        long xIdx = Math.round(latOffset * this.nvX.getLatDeg() + lngOffset * this.nvX.getLngDeg() + (this.resX - 1) / 2.) + this.borderX;
        return new Index(xIdx, yIdx);
    }

    private Neighbors getNeighbors(Position p) {
        // calculate grid indexes
        Position posR = new DegreePosition(p.getLatDeg() - this.rcStart.getLatDeg(),
                (p.getLngDeg() - this.rcStart.getLngDeg()) * this.lngScale);

        // surrounding grid points
        double yFlt = posR.getLatDeg() * this.nvY.getLatDeg() + posR.getLngDeg() * this.nvY.getLngDeg() + this.borderY;
        double xFlt = posR.getLatDeg() * this.nvX.getLatDeg() + posR.getLngDeg() * this.nvX.getLngDeg()
                + (this.resX - 1) / 2. + this.borderX;
        double yBot = Math.floor(yFlt);
        double xBot = Math.floor(xFlt);
        double yTop = Math.ceil(yFlt);
        double xTop = Math.ceil(xFlt);
        if (xBot < 0) {
            xBot = 0;
            xFlt = xBot;
            xTop = 1;
        }
        if (yBot < 0) {
            yBot = 0;
            yFlt = yBot;
            yTop = 1;
        }
        if (xTop >= (this.resX + 2 * this.borderX)) {
            xTop = this.resX + 2 * this.borderX - 1;
            xFlt = xTop;
            xBot = xTop - 1;
        }
        if (yTop >= (this.resY + 2 * this.borderY)) {
            yTop = this.resY + 2 * this.borderY - 1;
            yFlt = yTop;
            yBot = yTop - 1;
        }
        double yMod = yFlt - yBot;
        double xMod = xFlt - xBot;
        return new Neighbors((int) xTop, (int) yTop, (int) xBot, (int) yBot, xMod, yMod);
    }

    @Override
    public double getMotionScale(double zoomLevel) {
        return this.motionFactor * Math.pow(1.6, Math.min(1.0, 6.0 - zoomLevel));
    }

    @Override
    public double getParticleWeight(LatLng p, Vector v) {
        return v == null ? 0 : (v.length() / this.maxLength + 0.1);
    }

    @Override
    public double getLineWidth(double speed) {
        /*
         * absolute linewidth speed == 12kn => linewidth 1.5 speed == 24kn => linewidth 3.0 speed == 6kn => linewidth
         * 0.75
         */
        return Math.max(0.01, Math.round((lineBase + lineScale * (speed - 12.0) / 8.0) * 100.0) / 100.0);
    }

    @Override
    public LatLngBounds getFieldCorners() {
        // FIXME this is not date line-safe. If the field crosses +180°E (the international date line), this will fail
        LatLng fieldNE = this.getInnerPosition(+0.5, 1.0);
        LatLng fieldSW = this.getInnerPosition(-0.5, 0.0);
        LatLng fieldSE = this.getInnerPosition(+0.5, 0.0);
        LatLng fieldNW = this.getInnerPosition(-0.5, 1.0);
        double minLat = Math.min(Math.min(fieldNE.getLatitude(), fieldSW.getLatitude()),
                Math.min(fieldNW.getLatitude(), fieldSE.getLatitude()));
        double minLng = Math.min(Math.min(fieldNE.getLongitude(), fieldSW.getLongitude()),
                Math.min(fieldNW.getLongitude(), fieldSE.getLongitude()));
        LatLng sw = LatLng.newInstance(minLat, minLng);
        double maxLat = Math.max(Math.max(fieldNE.getLatitude(), fieldSW.getLatitude()),
                Math.max(fieldNW.getLatitude(), fieldSE.getLatitude()));
        double maxLng = Math.max(Math.max(fieldNE.getLongitude(), fieldSW.getLongitude()),
                Math.max(fieldNW.getLongitude(), fieldSE.getLongitude()));
        LatLng ne = LatLng.newInstance(maxLat, maxLng);
        return LatLngBounds.newInstance(sw, ne);
    }

    @Override
    public double getParticleFactor() {
        return this.particleFactor;
    }
    
}
