package com.sap.sailing.domain.test;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.jupiter.api.Test;

import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;

/**
 * {@link ReentrantReadWriteLock} can produce a lock between readers and other readers if the lock
 * is <em>fair</em> and a writer applies between the first read lock acquisition and the second read
 * lock acquisition attempt. We try to enhance {@link LockUtil} such that it is possible to pass
 * a "lock set" from one thread to the next, such that when a thread tries to acquire a read lock
 * that is already in the thread's lock set, it doesn't really acquire the lock but only increments
 * a counter. This will then avoid the read-read deadlock.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ReadReadDeadlockTest {
    @Test
    public void testReadReadDeadlock() throws InterruptedException {
        final Object notifier = new Object();
        final NamedReentrantReadWriteLock lock = new NamedReentrantReadWriteLock("testReadReadDeadlock", /* fair */ true);
        final boolean[] t1ObtainedReadLock = new boolean[1];
        final Thread t1 = new Thread("t1") {
            public void run() {
                LockUtil.lockForRead(lock);
                try {
                    synchronized (notifier) {
                        t1ObtainedReadLock[0] = true;
                        notifier.notifyAll();
                        try {
                            notifier.wait(); // wait for t2 to have announced acquiring the write lock
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    Thread.sleep(100); // wait until t2 really has requested the write lock
                    final Thread myT1 = Thread.currentThread();
                    // Now spawn a new thread and wait for its termination synchronously.
                    // This guarantees that t1 will hold on to the read lock until t3's run method
                    // returns. Therefore, t3 doesn't necessarily need to apply for the read lock again.
                    final Thread t3 = new Thread("t3") {
                        public void run() {
                            LockUtil.propagateLockSetFrom(myT1);
                            // without lock set propagation, this will cause a deadlock
                            LockUtil.lockForRead(lock);
                            try {
                                System.out.println("Got read lock in t3");
                            } finally {
                                LockUtil.unlockAfterRead(lock);
                            }
                            LockUtil.unpropagateLockSetFrom(myT1);
                        }
                    };
                    t3.start();
                    try {
                        t3.join(); // joins only if lock set propagation works
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } catch (InterruptedException e1) {
                    throw new RuntimeException(e1);
                } finally {
                    LockUtil.unlockAfterRead(lock); // this will let t2 continue and terminate
                }
            }
        };
        final Thread t2 = new Thread("t2") {
            public void run() {
                // wait until t1 has obtained the read lock
                synchronized (notifier) {
                    while (!t1ObtainedReadLock[0]) {
                        try {
                            notifier.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                try {
                    synchronized (notifier) {
                        notifier.notifyAll();
                    }
                    // safe to lock after notifying because waiters can only continue once
                    // this method has left the synchronized(notifier) block
                    LockUtil.lockForWrite(lock); // will wait for t1 which owns the read lock
                } finally {
                    LockUtil.unlockAfterWrite(lock);
                }
            }
        };
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
