package com.sap.sailing.domain.base.impl;

import com.sap.sailing.domain.common.Bearing;

public abstract class AbstractBearing implements Bearing {
    @Override
    public Bearing reverse() {
        if (getDegrees() >= 180) {
            return new DegreeBearingImpl(getDegrees()-180);
        } else {
            return new DegreeBearingImpl(getDegrees()+180);
        }
    }
    
    @Override
    public Bearing add(Bearing diff) {
        double newDeg = getDegrees() + diff.getDegrees();
        if (newDeg > 360) {
            newDeg -= 360;
        } else if (newDeg < 0) {
            newDeg += 360;
        }
        return new DegreeBearingImpl(newDeg);
    }

    @Override
    public Bearing getDifferenceTo(Bearing b) {
        double diff = b.getDegrees() - getDegrees();
        if (diff < -180) {
            diff += 360;
        } else if (diff > 180) {
            diff -= 360;
        }
        return new DegreeBearingImpl(diff);
    }

    @Override
    public Bearing middle(Bearing other) {
        Bearing result = new DegreeBearingImpl((getDegrees() + other.getDegrees()) / 2.0);
        if (Math.abs(getDegrees()-other.getDegrees()) > 180.) {
            result = result.reverse();
        }
        return result;
    }

    @Override
    public String toString() {
        return ""+getDegrees()+"�";
    }
    
    @Override
    public int hashCode() {
        return (int) (1023 ^ Double.doubleToLongBits(getDegrees()));
    }
    
    @Override
    public boolean equals(Object object) {
        return object != null && object instanceof Bearing && getDegrees() == ((Bearing) object).getDegrees();
    }
}
