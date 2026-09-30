package com.sap.sailing.simulator.windfield.impl;

import java.util.List;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.simulator.Grid;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.TimedPosition;
import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sailing.simulator.windfield.WindControlParameters;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Duration;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

public class WindFieldGeneratorMeasured extends WindFieldGeneratorImpl implements WindFieldGenerator {

    private static final Logger LOGGER = Logger.getLogger("com.sap.sailing.simulator");

    private static final long serialVersionUID = -7436152672809530764L;

    protected Path gpsWind;

    //    public WindFieldGeneratorMeasured() {
    //        super();
    //    }

    public WindFieldGeneratorMeasured(Grid boundary, WindControlParameters windParameters) {
        super(boundary, windParameters);
    }

    public void setGPSWind(Path gpsWind) {
        this.gpsWind = gpsWind;
    }

    @Override
    public void generate(TimePoint start, TimePoint end, Duration step) {
        super.generate(start, end, step);
    }

    @Override
    public Wind getWind(TimedPosition timedPosition) {

        TimePoint timePoint = timedPosition.getTimePoint();
        TimedPositionWithSpeed p1 = null;
        TimedPositionWithSpeed p2 = null;
        List<TimedPositionWithSpeed> pathPoints = gpsWind.getPathPoints();
        for(TimedPositionWithSpeed p : pathPoints) {
            p2 = p;
            if (p.getTimePoint().after(timePoint)) {

                break;
            }
            p1 = p;
        }

        String errorMessage = null;
        if (p1 == null) {
            errorMessage = "ERROR: couldn't find the before time point, interpolation will now fail!";
            System.out.println(errorMessage);
            LOGGER.severe(errorMessage);
        }

        if (p2 == null) {
            errorMessage = "ERROR: couldn't find the after time point, interpolation will now fail!";
            System.out.println(errorMessage);
            LOGGER.severe(errorMessage);
        }

        // TODO: interpolate between p1 and p2
        // TODO: check for race with true wind measurement; current test race has everyone 1kn wind speed
        //System.out.println("bear p1: "+p1.getSpeed().getBearing().getDegrees()+"  p2: "+p2.getSpeed().getBearing().getDegrees());

        SpeedWithBearing s1 = p1.getSpeed();
        if (s1 == null) {
            errorMessage = "ERROR: p1.getSpeed() is null!";
            System.out.println(errorMessage);
            LOGGER.severe(errorMessage);
        }

        Bearing b1 = s1.getBearing();
        if (b1 == null) {
            errorMessage = "ERROR: p1.getSpeed().getBearing(); is null!";
            System.out.println(errorMessage);
            LOGGER.severe(errorMessage);
        }

        SpeedWithBearing s2 = p2.getSpeed();
        if (s2 == null) {
            errorMessage = "ERROR: p2.getSpeed() is null!";
            System.out.println(errorMessage);
            LOGGER.severe(errorMessage);
        }

        Bearing b2 = s2.getBearing();
        if (b2 == null) {
            errorMessage = "ERROR: p2.getSpeed().getBearing(); is null!";
            System.out.println(errorMessage);
            LOGGER.severe(errorMessage);
        }

        Bearing midBear = new DegreeBearingImpl((b1.getDegrees() + b2.getDegrees()) / 2.);
        SpeedWithBearing speedWithBearing = new KnotSpeedWithBearingImpl((p1.getSpeed().getKnots()+p2.getSpeed().getKnots())/2., midBear);

        return new WindImpl(timedPosition.getPosition(), timedPosition.getTimePoint(), speedWithBearing);

    }
}
