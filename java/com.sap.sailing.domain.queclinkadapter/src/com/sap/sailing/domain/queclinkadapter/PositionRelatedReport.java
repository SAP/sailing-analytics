package com.sap.sailing.domain.queclinkadapter;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.TimePoint;

public interface PositionRelatedReport {

    Distance getOdometer();

    int getCellId();

    int getLocationAreaCode();

    short getMobileNetworkCode();

    short getMobileCountryCode();

    TimePoint getValidityTime();

    Position getPosition();

    Distance getAltitude();

    SpeedWithBearing getCogAndSog();

    byte getHdop();
    
}
