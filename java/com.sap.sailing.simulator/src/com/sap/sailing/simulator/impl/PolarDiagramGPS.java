package com.sap.sailing.simulator.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;

public class PolarDiagramGPS extends PolarDiagramBase {
    private static final Logger logger = Logger.getLogger(PolarDiagramGPS.class.getName());

    private static final long serialVersionUID = -9219705955440602679L;
    private double avgSpeedInKnots;
    private final double WIND_DIRECTION_STEPS_IN_DEGREES = 10;

    public PolarDiagramGPS(BoatClass boatClass, PolarDataService polarData) throws SparseSimulationDataException {
        this.boatClass = boatClass;
        List<Speed> windSpeeds = new ArrayList<Speed>();
        List<Bearing> beatAngles = new ArrayList<Bearing>();
        List<Speed> beatSpeed = new ArrayList<Speed>();
        // boat speeds keyed by true wind angle; the value list entries are expected to correspond to the windSpeeds entries at the respective index:
        Map<Bearing, List<Speed>> speeds = new HashMap<Bearing, List<Speed>>();
        List<Speed> jibeSpeed = new ArrayList<Speed>();
        List<Bearing> jibeAngles = new ArrayList<Bearing>();
        // initialize wind speeds
        windSpeeds.add(new KnotSpeedImpl(0.0));
        windSpeeds.add(new KnotSpeedImpl(6.0));
        windSpeeds.add(new KnotSpeedImpl(8.0));
        windSpeeds.add(new KnotSpeedImpl(10.0));
        windSpeeds.add(new KnotSpeedImpl(12.0));
        windSpeeds.add(new KnotSpeedImpl(14.0));
        windSpeeds.add(new KnotSpeedImpl(16.0));
        windSpeeds.add(new KnotSpeedImpl(18.0));
        windSpeeds.add(new KnotSpeedImpl(20.0));
        windSpeeds.add(new KnotSpeedImpl(22.0));
        windSpeeds.add(new KnotSpeedImpl(24.0));
        // initialize beat-angles and -speeds
        SpeedWithBearing beatPort;
        SpeedWithBearing beatStar;
        int avgCount = 0;
        avgSpeedInKnots = 0;
        for (int i = 0; i < windSpeeds.size(); i++) {
            final Speed windSpeed = windSpeeds.get(i);
            for (double twaInDegrees = 0; twaInDegrees <= 180; twaInDegrees += WIND_DIRECTION_STEPS_IN_DEGREES) {
                final Bearing portBearing = new DegreeBearingImpl(-twaInDegrees);
                final List<Speed> boatSpeedLineForWindSpeedPort = speeds.computeIfAbsent(portBearing, key->new ArrayList<>());
                try {
                    Speed portSpeed = polarData.getSpeed(boatClass, windSpeed, portBearing).getObject();
                    if (portSpeed.getKnots() < 0) {
                        boatSpeedLineForWindSpeedPort.add(Speed.NULL);
                    } else {
                        boatSpeedLineForWindSpeedPort.add(portSpeed);
                    }
                } catch (NotEnoughDataHasBeenAddedException e) {
                    logger.fine(()->"No polar data for boat class "+boatClass.getName()+" for wind speed "+windSpeed+
                            " at true wind angle "+portBearing);
                    boatSpeedLineForWindSpeedPort.add(Speed.NULL);
                }
                final Bearing starboardBearing = new DegreeBearingImpl(twaInDegrees);
                final List<Speed> boatSpeedLineForWindSpeedStarboard = speeds.computeIfAbsent(starboardBearing, key->new ArrayList<>());
                try {
                    Speed starboardSpeed = polarData.getSpeed(boatClass, windSpeed, starboardBearing).getObject();
                    if (starboardSpeed.getKnots() < 0) {
                        boatSpeedLineForWindSpeedStarboard.add(Speed.NULL);
                    } else {
                        boatSpeedLineForWindSpeedStarboard.add(starboardSpeed);
                    }
                } catch (NotEnoughDataHasBeenAddedException e) {
                    logger.fine(()->"No polar data for boat class "+boatClass.getName()+" for wind speed "+windSpeed+
                            " at true wind angle "+starboardBearing);
                    boatSpeedLineForWindSpeedStarboard.add(Speed.NULL);
                }
            }
            try {
                beatPort = polarData.getAverageSpeedWithTrueWindAngle(this.boatClass, windSpeed, LegType.UPWIND,
                        Tack.PORT).getObject();
                beatStar = polarData.getAverageSpeedWithTrueWindAngle(this.boatClass, windSpeed, LegType.UPWIND,
                        Tack.STARBOARD).getObject();
            } catch (NotEnoughDataHasBeenAddedException e) {
                beatPort = null;
                beatStar = null;
            }
            if (beatStar != null) {
                double beatStarAngle = Math.abs((beatStar.getBearing().getDegrees() + 180.0) % 360.0 - 180.0);
                double beatPortAngle = Math.abs((beatPort.getBearing().getDegrees() + 180.0) % 360.0 - 180.0);
                Bearing avgBeatAngle = new DegreeBearingImpl((beatStarAngle + beatPortAngle) / 2.0);
                beatAngles.add(avgBeatAngle);
                Speed avgBeatSpeed = new KnotSpeedImpl((beatStar.getKnots() + beatPort.getKnots()) / 2.0);
                beatSpeed.add(avgBeatSpeed);
                avgSpeedInKnots += avgBeatSpeed.getKnots();
                avgCount++;
            } else {
                beatAngles.add(null);
                beatSpeed.add(null);
            }
        }
        avgSpeedInKnots /= avgCount;
        // initialize jibe-angles and -speeds
        SpeedWithBearing jibePort;
        SpeedWithBearing jibeStar;
        for (int i = 0; i < windSpeeds.size(); i++) {
            try {
                jibePort = polarData.getAverageSpeedWithTrueWindAngle(this.boatClass, windSpeeds.get(i),
                        LegType.DOWNWIND, Tack.PORT).getObject();
                jibeStar = polarData.getAverageSpeedWithTrueWindAngle(this.boatClass, windSpeeds.get(i),
                        LegType.DOWNWIND, Tack.STARBOARD).getObject();
            } catch (NotEnoughDataHasBeenAddedException e) {
                jibePort = null;
                jibeStar = null;
            }
            if (jibeStar != null) {
                double jibeStarAngle = Math.abs((jibeStar.getBearing().getDegrees() + 180.0) % 360.0 - 180.0);
                double jibePortAngle = Math.abs((jibePort.getBearing().getDegrees() + 180.0) % 360.0 - 180.0);
                Bearing avgJibeAngle = new DegreeBearingImpl((jibeStarAngle + jibePortAngle) / 2.0);
                jibeAngles.add(avgJibeAngle);
                Speed avgJibeSpeed = new KnotSpeedImpl((jibeStar.getKnots() + jibePort.getKnots()) / 2.0);
                jibeSpeed.add(avgJibeSpeed);
            } else {
                jibeAngles.add(null);
                jibeSpeed.add(null);
            }
        }
        NavigableMap<Speed, NavigableMap<Bearing, Speed>> mapSpeedTable = new TreeMap<Speed, NavigableMap<Bearing, Speed>>();
        NavigableMap<Speed, Bearing> mapBeatAngles = new TreeMap<Speed, Bearing>();
        NavigableMap<Speed, Bearing> mapJibeAngles = new TreeMap<Speed, Bearing>();
        NavigableMap<Speed, Speed> mapBeatSOG = new TreeMap<Speed, Speed>();
        NavigableMap<Speed, Speed> mapJibeSOG = new TreeMap<Speed, Speed>();
        Speed windSpeed = null;
        Speed boatSpeed = null;
        NavigableMap<Bearing, Speed> speedTableLine = null;
        for (int index = 0; index < windSpeeds.size(); index++) {
            windSpeed = windSpeeds.get(index);
            if (windSpeed.getKnots() == 0.0) {
                mapBeatSOG.put(new KnotSpeedImpl(0.0), new KnotSpeedImpl(0.0));
                mapJibeSOG.put(new KnotSpeedImpl(0.0), new KnotSpeedImpl(0.0));
            }
            speedTableLine = new TreeMap<Bearing, Speed>(bearingComparator);
            for (Entry<Bearing, List<Speed>> entry : speeds.entrySet()) {
                if (index >= entry.getValue().size()) {
                    continue;
                }
                boatSpeed = entry.getValue().get(index);
                if (boatSpeed != Speed.NULL) {
                    speedTableLine.put(entry.getKey(), boatSpeed);
                }
            }
            mapSpeedTable.put(windSpeed, speedTableLine);
            if (beatAngles.get(index) != null) {
                mapBeatAngles.put(windSpeed, beatAngles.get(index));
            }
            if (jibeAngles.get(index) != null) {
                mapJibeAngles.put(windSpeed, jibeAngles.get(index));
            }
            if (beatSpeed.get(index) != null) {
                mapBeatSOG.put(windSpeed, beatSpeed.get(index));
            }
            if (jibeSpeed.get(index) != null) {
                mapJibeSOG.put(windSpeed, jibeSpeed.get(index));
            }
        }
        if ((mapBeatAngles.size() <= 1)||(mapBeatSOG.size() <= 1)||(mapJibeAngles.size() <= 1)||(mapJibeSOG.size() <= 1)) {
            throw new SparseSimulationDataException();
        }
        super.speedTable = mapSpeedTable;
        super.beatAngles = mapBeatAngles;
        super.jibeAngles = mapJibeAngles;
        super.beatSOG = mapBeatSOG;
        super.jibeSOG = mapJibeSOG;
        // make sure the speeds for the tacking and gybing angles at each wind speed are present in speedTable;
        // this makes sure that no interpolation needs to happen between a static, wind speed-independent set
        // of wind angles for which speeds have been entered into the speedTable before
        for (Speed s : super.speedTable.keySet()) {
            if (super.beatAngles.containsKey(s) && !super.speedTable.get(s).containsKey(super.beatAngles.get(s))) {
                super.speedTable.get(s).put(super.beatAngles.get(s), super.beatSOG.get(s));
            }
            if (super.jibeAngles.containsKey(s) && !super.speedTable.get(s).containsKey(super.jibeAngles.get(s))) {
                super.speedTable.get(s).put(super.jibeAngles.get(s), super.jibeSOG.get(s));
            }
        }
    }

    public double getAvgSpeedInKnots() {
        return this.avgSpeedInKnots;
    }
}
