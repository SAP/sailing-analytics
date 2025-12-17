/**
 * 
 */
package com.sap.sse.common.impl.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.TimedLockImpl;

/**
 * 
 */
class TimedLockImplTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {
    }

    /**
     * Test method for {@link com.sap.sse.common.impl.TimedLockImpl#extendLockDuration()}.
     */
    @Test
    void testExtendLockDuration() {
        final TimedLockImpl lock = new TimedLockImpl();
        final Duration firstLockingDelay = lock.getNextLockingDelay();
        // initial locking delay should be greater than 0
        assertTrue(firstLockingDelay.compareTo(Duration.NULL) > 0);
        assertTrue(firstLockingDelay.compareTo(Duration.ofMillis(0)) > 0);
        final TimePoint expectedLockedUntil = TimePoint.now().plus(firstLockingDelay);
        // locking delay should be applied to lockedUntil
        lock.extendLockDuration();
        assertEquals(lock.getLockedUntil(), expectedLockedUntil);
        // locking delay should double
        final Duration secondLockingDelay = firstLockingDelay.plus(firstLockingDelay);
        assertEquals(lock.getNextLockingDelay(), secondLockingDelay);
        // double locking delay should be applied to lockedUntil
        lock.extendLockDuration();
        assertEquals(lock.getLockedUntil(), TimePoint.now().plus(secondLockingDelay));
    }

     /**
     * Test method for {@link com.sap.sse.common.impl.TimedLockImpl#resetLock()}.
     */
     @Test
     void testResetLock() {
         final TimedLockImpl lock = new TimedLockImpl();
         lock.extendLockDuration();
         lock.extendLockDuration();
         lock.extendLockDuration();
         assertTrue(lock.isLocked());
         lock.resetLock();
         assertFalse(lock.isLocked());
     }

}
