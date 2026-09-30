package com.sap.sailing.declination;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Timed;

/**
 * Tells the magnetic declination for a position and a given point in time together with an anticipated annual change.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public interface Declination extends Timed {
    Position getPosition();
    
    Bearing getBearing();
    
    Bearing getAnnualChange();
    
    Bearing getBearingCorrectedTo(TimePoint timePoint);
}
