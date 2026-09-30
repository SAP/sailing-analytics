package com.sap.sailing.domain.swisstimingadapter;

import java.io.Serializable;

import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;

/**
 * Data fix transmitted periodically from the SwissTiming Sail Master system, telling boat position and speed
 * data.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface Fix {
    Serializable getTrackedObjectId();
    
    TrackerType getTrackerType();
    
    Long getAgeOfDataInMilliseconds();
    
    Position getPosition();
    
    SpeedWithBearing getSpeed();
    
    Integer getNextMarkIndex();
    
    Integer getRank();
    
    Speed getAverageSpeedOverGroundPerLeg();
    
    Speed getVelocityMadeGood();
    
    Distance getDistanceToLeader();
    
    Distance getDistanceToNextMark();

    /**
     * Obtains a disqualification reason which is one of { None, BFD, DNS, DNF, DNC, DSQ, OCS, RAF, SCP, ZFP }. May be
     * <code>null</code>.
     */
    String getBoatIRM();
}
