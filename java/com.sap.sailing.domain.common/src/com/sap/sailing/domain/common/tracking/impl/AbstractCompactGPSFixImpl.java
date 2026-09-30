package com.sap.sailing.domain.common.tracking.impl;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.AbstractTimePoint;

/**
 * A compact representation of a GPS fix which collects all primitive-typed attributes in one object to avoid
 * memory overhead otherwise created by using too many individual fine-grained objects.<p>
 * 
 * Objects of this type are assumed to be contained in at most one {@link DynamicGPSFixTrackImpl}. It is
 * therefore permissible to cache information about validity to speed up the otherwise expensive
 * {@link DynamicGPSFixTrackImpl#isValid(PartialNavigableSetView, GPSFix)} computation.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public abstract class AbstractCompactGPSFixImpl extends AbstractGPSFixImpl {
    private static final long serialVersionUID = 3943572107870245437L;

    /**
     * Bit mask for {@link #whatIsCached}, telling whether validity is currently cached
     */
    private static final byte IS_VALIDITY_CACHED = 1<<0;

    /**
     * Bit mask for {@link #whatIsCached}, telling whether the estimated speed is currently cached
     */
    private static final byte IS_ESTIMATED_SPEED_CACHED = 1<<1;
    
    /**
     * Bit mask for {@link #whatIsCached}, telling the validity of the fix; only relevant if
     * <code>{@link #whatIsCached}&amp;{@link #IS_VALIDITY_CACHED} != 0</code>
     */
    private static final byte VALIDITY = 1<<2;

    private final long timePointAsMillis;
    
    /**
     * Tells if in the containing {@link DynamicGPSFixTrackImpl} this fix is considered valid. This cache
     * needs to be invalidated as soon as fixes are added to the containing track which may have an impact
     * on this fix's validity. See the bitmask values such as {@link #VALIDITY}, {@link #IS_ESTIMATED_SPEED_CACHED} and
     * {@link #IS_VALIDITY_CACHED}.
     */
    private byte whatIsCached = 0;
    
    private class CompactTimePoint extends AbstractTimePoint implements TimePoint {
        private static final long serialVersionUID = -2470922642359937437L;

        @Override
        public long asMillis() {
            return timePointAsMillis;
        }
    }
    
    public AbstractCompactGPSFixImpl(TimePoint timePoint) {
        timePointAsMillis = timePoint==null?-1:timePoint.asMillis();
    }

    public AbstractCompactGPSFixImpl(GPSFix gpsFix) {
        this(gpsFix.getTimePoint());
    }

    @Override
    public String toString() {
        return getTimePoint() + ": " + getPosition();
    }

    @Override
    public TimePoint getTimePoint() {
        return new CompactTimePoint();
    }

    @Override
    public boolean isValidityCached() {
        return (whatIsCached & IS_VALIDITY_CACHED) != 0;
    }

    @Override
    public boolean isValidCached() {
        assert isValidityCached();
        return (whatIsCached & VALIDITY) != 0;
    }

    @Override
    public synchronized void invalidateCache() {
        whatIsCached &= ~IS_VALIDITY_CACHED;
    }

    @Override
    public void cacheValidity(boolean isValid) {
        if (isValid) {
            whatIsCached |= IS_VALIDITY_CACHED | VALIDITY;
        } else {
            whatIsCached |= IS_VALIDITY_CACHED;
            whatIsCached &= ~VALIDITY;
        }
    }

    @Override
    public boolean isEstimatedSpeedCached() {
        return (whatIsCached & IS_ESTIMATED_SPEED_CACHED) != 0;
    }

    @Override
    public synchronized void invalidateEstimatedSpeedCache() {
        whatIsCached &= ~IS_ESTIMATED_SPEED_CACHED;
    }

    @Override
    public synchronized SpeedWithBearing cacheEstimatedSpeed(SpeedWithBearing estimatedSpeed) {
        whatIsCached |= IS_ESTIMATED_SPEED_CACHED;
        return super.cacheEstimatedSpeed(estimatedSpeed);
    }
}
