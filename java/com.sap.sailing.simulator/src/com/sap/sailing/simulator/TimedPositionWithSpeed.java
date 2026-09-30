package com.sap.sailing.simulator;

import com.sap.sse.common.SpeedWithBearing;

public interface TimedPositionWithSpeed extends TimedPosition {

	SpeedWithBearing getSpeed();
	
}
