package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.util.AutoCloseableReadLock;
import com.sap.sse.util.AutoCloseableWriteLock;

public class LockAsResourceTest {
    @Test
    public void testTryWithReadLockAsResource() {
        NamedReentrantReadWriteLock lock = new NamedReentrantReadWriteLock("Test Lock", /* fair */ false);
        try (AutoCloseableReadLock readlock = new AutoCloseableReadLock(lock)) {
            assertEquals(1, lock.getReadHoldCount());
        }
        assertEquals(0, lock.getReadHoldCount());
    }

    @Test
    public void testTryWithWriteLockAsResource() {
        NamedReentrantReadWriteLock lock = new NamedReentrantReadWriteLock("Test Lock", /* fair */ false);
        try (AutoCloseableWriteLock readlock = new AutoCloseableWriteLock(lock)) {
            assertEquals(1, lock.getWriteHoldCount());
        }
        assertEquals(0, lock.getWriteHoldCount());
    }
}
