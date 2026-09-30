package com.sap.sailing.simulator;

import java.io.Serializable;
import java.util.NavigableMap;
import java.util.Set;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.Util.Pair;

public interface PolarDiagram extends Serializable {
    enum WindSide {
        RIGHT, LEFT, UPWIND, DOWNWIND
    };

    // scales
    void setSpeedScale(double scaleSpeed);
    double getSpeedScale();
    void setBearingScale(double scaleBearing);
    double getBearingScale();

    // wind
    SpeedWithBearing getWind();
    void setWind(SpeedWithBearing newWind);

    // current
    void initializeSOGwithCurrent();
    void setCurrent(SpeedWithBearing newCurrent);
    SpeedWithBearing getCurrent();
    boolean hasCurrent();

    // target direction
    //     default is target bearing = 0;
    //     for target bearing != 0, optimal angles and VMG are specifically calculated 
    Bearing getTargetDirection();
    void setTargetDirection(Bearing newTargetDirection);
    
    // boat
    Pair<PointOfSail, BoatDirection> getPointOfSail(Bearing bearTarget);
    SpeedWithBearing getSpeedAtBearing(Bearing bearing);
    SpeedWithBearing getSpeedAtBearingOverGround(Bearing bearing);
    SpeedWithBearing[] optimalVMGUpwind();
    Bearing[] optimalDirectionsUpwind();
    Bearing[] optimalDirectionsDownwind();
    long getTurnLoss();
    WindSide getWindSide(Bearing bearing);

    // polar raw data
    NavigableMap<Speed, NavigableMap<Bearing, Speed>> polarDiagramPlot(Double bearingStep, Set<Speed> extraSpeeds);
    NavigableMap<Speed, NavigableMap<Bearing, Speed>> polarDiagramPlot(Double bearingStep);
    NavigableMap<Speed, NavigableMap<Bearing, Speed>> getSpeedTable();
    NavigableMap<Speed, Bearing> getBeatAngles();
    NavigableMap<Speed, Bearing> getJibeAngles();
    NavigableMap<Speed, Speed> getBeatSOG();
    NavigableMap<Speed, Speed> getJibeSOG();
}
