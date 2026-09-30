package com.sap.sailing.gwt.ui.shared;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.Position;

public class WindDTO implements IsSerializable {
    public Double trueWindSpeedInMetersPerSecond;
    public Double trueWindSpeedInKnots;
    public Double trueWindBearingDeg;
    public Double trueWindFromDeg;
    public Double dampenedTrueWindSpeedInMetersPerSecond;
    public Double dampenedTrueWindSpeedInKnots;
    public Double dampenedTrueWindBearingDeg;
    public Double dampenedTrueWindFromDeg;
    public Position position;
    
    /**
     * The point in time when these wind values have been measured. The value can be null in case the values are derived
     * or calculated from other values
     */
    public Long measureTimepoint;
    
    /**
     * The point in time for which the calculation of these values has been requested.  
     */
    public Long requestTimepoint;
    
    /**
     * The confidence of the wind values
     */
    public Double confidence;
    
    public WindDTO() {}
}
