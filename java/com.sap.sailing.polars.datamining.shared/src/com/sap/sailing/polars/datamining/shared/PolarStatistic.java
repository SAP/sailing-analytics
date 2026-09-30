package com.sap.sailing.polars.datamining.shared;

import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;

public interface PolarStatistic {

    public SpeedWithBearing getBoatSpeed();

    public Speed getWindSpeed();

    public double getTrueWindAngleDeg();
    
    public PolarDataMiningSettings getSettings();

}
