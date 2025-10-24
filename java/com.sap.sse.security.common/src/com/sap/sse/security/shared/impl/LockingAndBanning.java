package com.sap.sse.security.shared.impl;

import java.io.Serializable;

import com.sap.sse.common.TimePoint;

/**
 * Holds information about a user's log-on history which is then used to decide whether the user account should be
 * locked temporarily or permanently for certain forms of authentication.
 * <p>
 * 
 * For example, failed password authentication requests shall be logged by the realm using calls to
 * {@link #failedPasswordAuthentication()}, successful ones with {@link #successfulPasswordAuthentication()}. Using the
 * {@link #isAuthenticationLocked()} method, a realm can determine if the user account to which this object belongs
 * shall currently accept password authentication.
 * <p>
 * 
 * A possible strategy for an implementation could be to add an increasing delay for each failed password
 * authentication, but reduce or clear the delay after a successful password authentication.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface LockingAndBanning extends Serializable {
    void failedPasswordAuthentication();

    /**
     * @return {@code true} if this locking and banning record changed due to this call
     */
    boolean successfulPasswordAuthentication();

    boolean isAuthenticationLocked();

    TimePoint getLockedUntil();
}
