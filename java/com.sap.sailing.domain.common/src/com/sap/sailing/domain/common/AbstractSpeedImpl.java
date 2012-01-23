package com.sap.sailing.domain.common;

import com.sap.sailing.domain.common.impl.NauticalMileDistance;

public abstract class AbstractSpeedImpl implements Speed {

    @Override
    public double getBeaufort() {
        return Math.exp(Math.log(getKnots()*1.852/3.6 / 0.8360)*2/3);
    }

    @Override
    public Distance travel(TimePoint t1, TimePoint t2) {
        return new NauticalMileDistance((t2.asMillis() - t1.asMillis()) / 1000. / 3600. * getKnots());
    }
    
    @Override
    public double getMetersPerSecond() {
        return getKnots() * Mile.METERS_PER_SEA_MILE / 3600;
    }

    @Override
    public double getKilometersPerHour() {
        return getKnots() * Mile.METERS_PER_SEA_MILE / 1000;
    }

    @Override
    public int compareTo(Speed speed) {
        return getMetersPerSecond() > speed.getMetersPerSecond() ? 1 : getMetersPerSecond() == speed.getMetersPerSecond() ? 0 : -1;
    }
    
    @Override
    public String toString() {
        return ""+getKnots()+"kn";
    }

    @Override
    public int hashCode() {
        return 31 * (int) getMetersPerSecond();
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof Speed)) {
            return false;
        }
        return getMetersPerSecond() == ((Speed) object).getMetersPerSecond();
    }
}
