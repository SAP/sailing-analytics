package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.test.measurements.Measurement;
import com.sap.sailing.domain.test.measurements.MeasurementCase;
import com.sap.sailing.domain.test.measurements.MeasurementXMLFile;
import com.sap.sse.common.Util;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;

public class LockTraceTest {
    private static class LockingThread extends Thread {
        private boolean ownsLock;
        
        public LockingThread(String name) {
            super(name);
        }
        
        public synchronized boolean ownsLock() {
            return ownsLock;
        }
        
        public synchronized void obtainLockAndWait(Lock lock) throws InterruptedException {
            lock.lock();
            ownsLock = true;
            notifyAll();
            wait();
        }

        public synchronized void waitUntilLockIsObtained() throws InterruptedException {
            while (!ownsLock()) {
                wait();
            }
        }
    }
    
    @Test
    public void testReentrantReadLocking() {
        NamedReentrantReadWriteLock lock = new NamedReentrantReadWriteLock("testReentrantReadLocking-Lock", /* fair */ true);
        LockUtil.lockForRead(lock);
        assertTrue(Util.contains(lock.getReaders(), Thread.currentThread()));
        assertEquals(1, Util.size(lock.getReaders()));
        LockUtil.lockForRead(lock);
        assertTrue(Util.contains(lock.getReaders(), Thread.currentThread()));
        assertEquals(2, Util.size(lock.getReaders()));
        LockUtil.unlockAfterRead(lock);
        assertTrue(Util.contains(lock.getReaders(), Thread.currentThread()));
        assertEquals(1, Util.size(lock.getReaders()));
        LockUtil.unlockAfterRead(lock);
        assertFalse(Util.contains(lock.getReaders(), Thread.currentThread()));
        assertEquals(0, lock.getReadHoldCount());
        assertEquals(0, Util.size(lock.getReaders()));
    }
    
    @Test
    public void testReentrantWriteLockingWithInBetweenReadLocking() {
        NamedReentrantReadWriteLock lock = new NamedReentrantReadWriteLock("testReentrantWriteLockingWithInBetweenReadLocking-Lock", /* fair */ false);
        LockUtil.lockForWrite(lock);
        assertTrue(lock.getWriter() == Thread.currentThread());
        LockUtil.lockForRead(lock); // reentrant read while holding write
        assertTrue(Util.contains(lock.getReaders(), Thread.currentThread()));
        LockUtil.lockForWrite(lock); // reentrant write
        assertTrue(lock.getWriter() == Thread.currentThread());
        assertTrue(Util.contains(lock.getReaders(), Thread.currentThread()));
        LockUtil.unlockAfterRead(lock);
        assertFalse(Util.contains(lock.getReaders(), Thread.currentThread()));
        assertEquals(0, lock.getReadHoldCount());
        assertTrue(lock.getWriter() == Thread.currentThread());
        assertFalse(Util.contains(lock.getReaders(), Thread.currentThread()));
        assertEquals(0, lock.getReadHoldCount());
        LockUtil.unlockAfterWrite(lock);
        assertTrue(lock.getWriter() == Thread.currentThread());
        LockUtil.unlockAfterWrite(lock);
        assertFalse(lock.getWriter() == Thread.currentThread());
    }

    @Test
    public void testLockingPerformance() throws IOException {
        NamedReentrantReadWriteLock lock = new NamedReentrantReadWriteLock("Lock", /* fair */ true);
        long start = System.currentTimeMillis();
        final int count = 100000;
        for (int i=0; i<count; i++) {
            LockUtil.lockForRead(lock);
            LockUtil.unlockAfterRead(lock);
            LockUtil.lockForWrite(lock);
            LockUtil.unlockAfterWrite(lock);
        }
        MeasurementXMLFile performanceReport = new MeasurementXMLFile(this.getClass());
        MeasurementCase performanceReportCase = performanceReport.addCase(getClass().getSimpleName());
        performanceReportCase.addMeasurement(new Measurement("Obtaining and releasing "+count+" read and write locks in ms",
                System.currentTimeMillis()-start));
        performanceReport.write();

        System.out.println("Took "+(System.currentTimeMillis()-start)+"ms");
    }
    
    @Disabled
    @Test
    public void testLockTraceForMultipleReaders() throws InterruptedException {
        NamedReentrantReadWriteLock lock1 = new NamedReentrantReadWriteLock("Lock1", /* fair */ true);
        lock1.readLock().lock();
        Object o = createAndStartLockingThreadReturningObjectToNotifyInOrderToReleaseLockAndTerminateThread(lock1.readLock());
        lock1.writeLock().lock();
        boolean itWorked = lock1.writeLock().tryLock(1000000000000000000l, TimeUnit.MILLISECONDS);
        System.out.println(itWorked);
        lock1.readLock().unlock();
        synchronized (o) {
            o.notifyAll();
        }
    }
    
    @Disabled
    @Test
    public void testDeadlockDetectionWithJMX() throws InterruptedException {
        NamedReentrantReadWriteLock lock1 = new NamedReentrantReadWriteLock("Lock1", /* fair */ true);
        NamedReentrantReadWriteLock lock2 = new NamedReentrantReadWriteLock("Lock2", /* fair */ true);
        lock1.readLock().lock();
        // let other thread get write lock on lock2
        LockingThread o = createAndStartLockingThreadReturningObjectToNotifyInOrderToCauseDeadlock(lock2.writeLock(), lock1.writeLock());
        o.waitUntilLockIsObtained();
        // now let other thread continue so it tries to obtain the write lock on lock1 which it can't get because we own the read lock
        synchronized (o) {
            o.notifyAll();
        }
        // and complete the deadlock by asking lock2's read lock
        lock2.readLock().lock();
        lock1.readLock().unlock();
        lock2.readLock().unlock();
    }
    
    private Object createAndStartLockingThreadReturningObjectToNotifyInOrderToReleaseLockAndTerminateThread(final Lock lock) {
        final Thread thread = new Thread("Thread to lock "+lock) {
            public void run() {
                lock.lock();
                synchronized (Thread.currentThread()) {
                    try {
                        Thread.currentThread().wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                lock.unlock();
            }
        };
        thread.start();
        return thread;
    }
    
    private LockingThread createAndStartLockingThreadReturningObjectToNotifyInOrderToCauseDeadlock(final Lock lock1, final Lock lock2) {
        final LockingThread thread = new LockingThread("Thread to lock "+lock1+" and then "+lock2) {
            public void run() {
                try {
                    obtainLockAndWait(lock1);
                } catch (InterruptedException e1) {
                    throw new RuntimeException(e1);
                }
                lock2.lock();
            }
        };
        thread.start();
        return thread;
    }

    public static void main(String[] args) throws InterruptedException {
        new LockTraceTest().testDeadlockDetectionWithJMX();
    }
}
