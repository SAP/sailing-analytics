package com.sap.sse.common.impl;

import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimedLock;
import com.sap.sse.common.Util;

public class TimedLockImpl implements TimedLock {
    private static final long serialVersionUID = 3547356744366236677L;
    
    public static final Duration DEFAULT_INITIAL_LOCKING_DELAY = Duration.ONE_SECOND;
    
    /**
     * An always valid time point which may be in the past. If it is in the future,
     * {@link #isLocked()} will return {@code true}.
     */
    private TimePoint lockedUntil;
    
    /**
     * An always valid, non-zero duration that indicates for how long into the future the {@link #lockedUntil} time
     * point will be set in case a {@link #extendLockDuration() failed password authentication} is notified.
     */
    private Duration nextLockingDelay;

    /**
     * Creates an instance that is unlocked and has a "last locking delay" of one second
     */
    public TimedLockImpl() {
        this(TimePoint.BeginningOfTime, DEFAULT_INITIAL_LOCKING_DELAY);
    }
    
    public TimedLockImpl(TimePoint lockedUntil, Duration nextLockingDelay) {
        super();
        this.lockedUntil = lockedUntil;
        this.nextLockingDelay = nextLockingDelay;
    }

    /**
     * Locks for the {@link #nextLockingDelay} and doubles the delay for the next failed attempt.
     */
    @Override
    public void extendLockDuration() {
        lockedUntil = TimePoint.now().plus(nextLockingDelay);
        nextLockingDelay = nextLockingDelay.times(2);
    }

    @Override
    public boolean resetLock() {
        final Duration oldLockingDelay = nextLockingDelay;
        nextLockingDelay = DEFAULT_INITIAL_LOCKING_DELAY;
        final TimePoint oldLockedUntil = lockedUntil;
        lockedUntil = TimePoint.BeginningOfTime;
        return !Util.equalsWithNull(oldLockingDelay, nextLockingDelay) || !Util.equalsWithNull(oldLockedUntil, lockedUntil);
    }

    @Override
    public boolean isLocked() {
        return TimePoint.now().before(lockedUntil);
    }

    @Override
    public TimePoint getLockedUntil() {
        return lockedUntil;
    }

    public Duration getNextLockingDelay() {
        return nextLockingDelay;
    }
    
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        if (isLocked()) {
            result.append("locked until ");
            result.append(getLockedUntil());
        } else {
            result.append("unlocked");
        }
        result.append(", next locking duration: ");
        result.append(getNextLockingDelay());
        return result.toString();
    }
}
