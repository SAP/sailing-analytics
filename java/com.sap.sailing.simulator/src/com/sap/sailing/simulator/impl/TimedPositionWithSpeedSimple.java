package com.sap.sailing.simulator.impl;

import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;

public class TimedPositionWithSpeedSimple implements TimedPositionWithSpeed {

	/**
	 * 
	 */
	private static final long serialVersionUID = 73150541143821154L;

	Position position;
	
	public TimedPositionWithSpeedSimple(Position p) {
		position = p;
	}
	
	@Override
	public TimePoint getTimePoint() {
		return null;
	}

	@Override
	public Position getPosition() {
		return position;
	}

	@Override
	public SpeedWithBearing getSpeed() {
		return (SpeedWithBearing) Speed.NULL;
	}

}
