package com.sap.sailing.domain.common.tracking.impl;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;

public abstract class AbstractGPSFixImpl implements GPSFix {
    private static final long serialVersionUID = 9037068515469957639L;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getPosition() == null) ? 0 : getPosition().hashCode());
        result = prime * result + ((getTimePoint() == null) ? 0 : getTimePoint().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof GPSFix))
            return false;
        GPSFix other = (GPSFix) obj;
        if (getPosition() == null) {
            if (other.getPosition() != null)
                return false;
        } else if (!getPosition().equals(other.getPosition()))
            return false;
        if (getTimePoint() == null) {
            if (other.getTimePoint() != null)
                return false;
        } else if (!getTimePoint().equals(other.getTimePoint()))
            return false;
        return true;
    }

    @Override
    public SpeedWithBearing getSpeedAndBearingRequiredToReach(GPSFix to) {
        Distance distance = getPosition().getDistance(to.getPosition());
        Bearing bearing = getPosition().getBearingGreatCircle(to.getPosition());
        Speed speed = distance.inTime(to.getTimePoint().asMillis()-getTimePoint().asMillis());
        return new KnotSpeedWithBearingImpl(speed.getKnots(), bearing);
    }

    /**
     * Subclasses overriding this method also need to override {@link #isValid} with a useful implementation.
     */
    @Override
    public boolean isValidityCached() {
        return false;
    }
    
    /**
     * Only evaluated if {@link #isValidityCached()} returns <code>true</code>. Therefore, subclassess that
     * override {@link #isValidityCached()} also need to override this method accordingly.
     */
    @Override
    public boolean isValidCached() {
        return false;
    }
    
    @Override
    public void invalidateCache() {
    }
    
    @Override
    public void cacheValidity(boolean isValid) {
    }

    /**
     * Subclasses overriding this method also need to override {@link #getCachedEstimatedSpeed()} with a useful implementation.
     */
    @Override
    public boolean isEstimatedSpeedCached() {
        return false;
    }

    /**
     * Only evaluated if {@link #isEstimatedSpeedCached()} returns <code>true</code>. Therefore, subclassess that
     * override {@link #isEstimatedSpeedCached()} also need to override this method accordingly.
     */
    @Override
    public SpeedWithBearing getCachedEstimatedSpeed() {
        return null;
    }

    @Override
    public void invalidateEstimatedSpeedCache() {
    }

    @Override
    public SpeedWithBearing cacheEstimatedSpeed(SpeedWithBearing estimatedSpeed) {
        return estimatedSpeed;
    }
}
