package com.sap.sailing.simulator.impl;

import com.sap.sailing.simulator.TimedPosition;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

public class TimedPositionImpl implements TimedPosition {

	/**
	 * 
	 */
	private static final long serialVersionUID = -675796846985362731L;
    private transient int hashCode;

	TimePoint timePoint;
	Position position;
	
	public TimedPositionImpl(TimePoint tp, Position p) {
		timePoint = tp;
		position = p;
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
	public boolean equals(Object other) {
		TimedPosition that = (TimedPosition) other;
		return (this.position.equals(that.getPosition())&&(this.timePoint.equals(that.getTimePoint())));
	}
	
    @Override
    public int hashCode( ) {
        if ( hashCode == 0 ) {
            hashCode = 17;
            hashCode = 37 * hashCode + ( timePoint != null ? timePoint.hashCode( ) : 0 );
            hashCode = 37 * hashCode + ( position != null ? position.hashCode( ) : 0 );
        }
        return hashCode;
    }

}
