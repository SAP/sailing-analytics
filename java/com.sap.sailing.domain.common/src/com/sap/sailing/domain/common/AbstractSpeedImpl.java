package com.sap.sailing.domain.common;

import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsDurationImpl;

public abstract class AbstractSpeedImpl implements Speed {

    private static final long serialVersionUID = 4910662213901175982L;

    @Override
    public double getBeaufort() {
        return Math.exp(Math.log(getMetersPerSecond()/0.8360) * 2./3.);
    }

    @Override
    public Distance travel(TimePoint t1, TimePoint t2) {
        return new NauticalMileDistance((t2.asMillis() - t1.asMillis()) / 1000. / 3600. * getKnots());
    }
    
    @Override
    public Duration getDuration(Distance distance) {
        return new MillisecondsDurationImpl((long) (1000 * distance.getMeters() / getMetersPerSecond()));
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
        final double metersPerSecond = getMetersPerSecond();
        final double otherMetersPerSecond = speed.getMetersPerSecond();
        return metersPerSecond > otherMetersPerSecond ? 1 : metersPerSecond == otherMetersPerSecond ? 0 : -1;
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

    @Override
    public double getKnots() {
        return getKilometersPerHour() * 1000. / Mile.METERS_PER_NAUTICAL_MILE;
    }
}
