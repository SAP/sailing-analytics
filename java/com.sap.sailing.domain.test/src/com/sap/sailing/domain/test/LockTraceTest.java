package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.junit.Ignore;
import org.junit.Test;

import com.sap.sailing.util.impl.LockUtil;
import com.sap.sailing.util.impl.NamedReentrantReadWriteLock;

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
        assertTrue(lock.getReaders().contains(Thread.currentThread()));
        LockUtil.lockForRead(lock);
        assertTrue(lock.getReaders().contains(Thread.currentThread()));
        LockUtil.unlockAfterRead(lock);
        assertTrue(lock.getReaders().contains(Thread.currentThread()));
        LockUtil.unlockAfterRead(lock);
        assertFalse(lock.getReaders().contains(Thread.currentThread()));
        assertEquals(0, lock.getReadHoldCount());
    }
    
    @Ignore
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
    
    @Ignore
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
