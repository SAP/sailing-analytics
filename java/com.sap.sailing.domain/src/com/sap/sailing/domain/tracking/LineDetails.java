package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sse.common.TimePoint;

/**
 * For a line such as a start or a finish line, tells the line's length at a given time, which side is
 * {@link NauticalSide#PORT port} and which is {@link NauticalSide#STARBOARD starboard} when approaching the line,
 * and---if wind information is available---its angle to a true wind direction and the advantageous side in approaching
 * direction as well as how much the advantageous side is ahead. The wind-dependent information
 * will all be <code>null</code> if no wind data is available.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public interface LineDetails {
    TimePoint getTimePoint();

    Waypoint getWaypoint();

    Distance getLength();

    Bearing getAbsoluteAngleDifferenceToTrueWind();

    NauticalSide getAdvantageousSideWhileApproachingLine();
    
    Mark getStarboardMarkWhileApproachingLine();
    
    Mark getPortMarkWhileApproachingLine();

    Distance getAdvantage();
}
