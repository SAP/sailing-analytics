package com.sap.sailing.racecommittee.app.domain.coursedesign;

import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sse.common.Position;
import com.sap.sse.common.impl.MeterDistance;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class PositionedMarkImpl extends MarkImpl implements PositionedMark {
    private static final long serialVersionUID = -7903960088124343841L;
    private Position position;

    public PositionedMarkImpl(String name, Position position) {
        this(name, position, MarkType.BUOY);
    }

    public PositionedMarkImpl(String name, Position position, MarkType markType) {
        super(name, name, markType, null, null, null);
        this.position = position;
    }

    @Override
    public Position getPosition() {
        return this.position;
    }

    @Override
    public Bearing getBearingFrom(Position other) {
        double lat1R = other.getLatRad();
        double lat2R = getPosition().getLatRad();
        double dLngR = getPosition().getLngRad() - other.getLngRad();
        double a = Math.sin(dLngR) * Math.cos(lat2R);
        double b = Math.cos(lat1R) * Math.sin(lat2R) - Math.sin(lat1R) * Math.cos(lat2R) * Math.cos(dLngR);
        double bearingD = Math.toDegrees(Math.atan2(a, b));
        double bearingResult = bearingD % 360;
        if (bearingResult < 0)
            bearingResult += 360;
        return new DegreeBearingImpl(bearingResult);
    }

    @Override
    public Distance getDistanceFromPosition(Position other) {
        final double earthRadiusInMeters = 6371000.0;
        double lat1R = getPosition().getLatRad();
        double lat2R = other.getLatRad();
        double dLatR = Math.abs(lat2R - lat1R);
        double dLngR = Math.abs(other.getLngRad() - getPosition().getLngRad());
        double a = Math.sin(dLatR / 2) * Math.sin(dLatR / 2)
                + Math.cos(lat1R) * Math.cos(lat2R) * Math.sin(dLngR / 2) * Math.sin(dLngR / 2);
        double dR = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return new MeterDistance(earthRadiusInMeters).scale(dR);
    }

}
