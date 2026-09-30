package com.sap.sailing.simulator.impl;

import com.sap.sailing.simulator.TimedPositionWithSpeed;
import com.sap.sse.common.Position;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;

public class TimedPositionWithSpeedImpl implements TimedPositionWithSpeed {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8946445227256013472L;

	TimePoint timePoint;
	Position position;
	SpeedWithBearing speed;
	
	public TimedPositionWithSpeedImpl(TimePoint tp, Position p, SpeedWithBearing s) {
		timePoint = tp;
		position = p;
		speed = s;
	}
	
	@Override
	public TimePoint getTimePoint() {
		return timePoint;
	}

	@Override
	public Position getPosition() {
		return position;
	}

	@Override
	public SpeedWithBearing getSpeed() {
		return speed;
	}

}
