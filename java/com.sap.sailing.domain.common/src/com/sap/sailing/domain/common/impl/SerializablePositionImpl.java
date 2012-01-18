package com.sap.sailing.domain.common.impl;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SerializablePosition;

public class SerializablePositionImpl implements SerializablePosition {
    private static final long serialVersionUID = -5649921262460723024L;
    
    private double latDeg;
    private double lngDeg;
    
    public SerializablePositionImpl() {}

    public SerializablePositionImpl(double latDeg, double lngDeg) {
        super();
        this.latDeg = latDeg;
        this.lngDeg = lngDeg;
    }

    @Override
    public Position getPosition() {
        return new DegreePosition(latDeg, lngDeg);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(latDeg);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lngDeg);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SerializablePositionImpl other = (SerializablePositionImpl) obj;
        if (Double.doubleToLongBits(latDeg) != Double.doubleToLongBits(other.latDeg))
            return false;
        if (Double.doubleToLongBits(lngDeg) != Double.doubleToLongBits(other.lngDeg))
            return false;
        return true;
    }

}
