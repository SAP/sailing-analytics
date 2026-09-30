package com.sap.sailing.racecommittee.app.domain.coursedesign;

import com.sap.sse.common.Position;
import com.sap.sse.common.Positioned;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;

public interface Located extends Positioned {
    Bearing getBearingFrom(Position other);

    Distance getDistanceFromPosition(Position other);
}
