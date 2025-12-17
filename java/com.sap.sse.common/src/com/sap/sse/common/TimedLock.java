package com.sap.sse.common;

import java.io.Serializable;

/**
 * Holds information about a user's log-on history which is then used to decide whether the user account should be
 * locked temporarily or permanently for certain forms of authentication.
 * <p>
 * 
 * For example, failed password authentication requests shall be logged by the realm using calls to
 * {@link #extendLockDuration()}, successful ones with {@link #resetLock()}. Using the
 * {@link #isLocked()} method, a realm can determine if the user account to which this object belongs
 * shall currently accept password authentication.
 * <p>
 * 
 * A possible strategy for an implementation could be to add an increasing delay for each failed password
 * authentication, but reduce or clear the delay after a successful password authentication.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface TimedLock extends Serializable {
    void extendLockDuration();

    /**
     * @return {@code true} if this locking and banning record changed due to this call
     */
    boolean resetLock();

    boolean isLocked();

    TimePoint getLockedUntil();
}
