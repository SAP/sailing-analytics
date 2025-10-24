package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.util.SmartFutureCache;
import com.sap.sse.util.SmartFutureCache.EmptyUpdateInterval;

/**
 * When the thread reading a value from the cache holds a fair read lock, and another thread is trying to obtain the
 * corresponding write lock, and the cache updating method tries to acquire the read lock as well, lock propagation
 * used to work only if the reader caused the synchronous triggering of the computing method. For asynchronous triggers,
 * a read-read deadlock could occur because no lock propagation was supported from the reading to the computing thread.
 * This test belongs to bug 1344 (http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=1344).<p>
 * 
 * There are two variants to be tested. Either the computing thread runs into the lock before the reader gets to call
 * {@link SmartFutureCache#get(Object, boolean)} and times out trying to obtain the read lock while the reader propagates
 * its locks so the computing thread gets it based on the propagation in the second try; or the computing thread hasn't
 * tried to obtain the lock yet. In this case, the propagation from the reading thread happens first, and the first
 * computing thread's attempt to obtain the read lock will succeed.<p>
 * 
 * Doing white-box testing, further variants include whether or not the reading thread synchronously triggers the computing
 * thread (e.g., with the cache in suspended mode and the reading thread demanding the latest results). If it does, the computing
 * thread will itself propagate the locks from the reading thread to the computing thread. A test should verify that this doesn't
 * conflict with how the lock propagation is triggered pro-actively by the reading thread.
 * 
 * @author Axel Uhl (D043530)
 */
public class SmartFutureCacheDeadlockTest {
    private static final Logger logger = Logger.getLogger(SmartFutureCacheDeadlockTest.class.getName());
    
    private static final String RESULT = "result";
    private static final String CACHE_KEY = "cacheKey";
    private NamedReentrantReadWriteLock lock;
    private Thread computingThread;
    private Object cacheUpdateBegunMonitor;
    private LockingScript reader;
    private Thread readerThread;
    private LockingScript writer;
    private Thread writerThread;
    private SmartFutureCache<String, String, EmptyUpdateInterval> sfc;

    @BeforeEach
    public void setUp() throws InterruptedException {
        cacheUpdateBegunMonitor = new Object();
        lock = new NamedReentrantReadWriteLock("testReadReadDeadlockBetweenGetterAndTrigger", /* fair */ true);
        sfc = new SmartFutureCache<String, String, SmartFutureCache.EmptyUpdateInterval>(
                new SmartFutureCache.AbstractCacheUpdater<String, String, SmartFutureCache.EmptyUpdateInterval>() {
                    @Override
                    public String computeCacheUpdate(String key, EmptyUpdateInterval updateInterval) throws Exception {
                        synchronized (cacheUpdateBegunMonitor) {
                            computingThread = Thread.currentThread();
                            cacheUpdateBegunMonitor.notifyAll();
                        }
                        logger.info("Trying to obtain read lock for "+lock.getName()+" in thread "+computingThread.getName());
                        LockUtil.lockForRead(lock);
                        try {
                            logger.info("Successfully obtained read lock for "+lock.getName()+" in thread "+computingThread.getName());
                            return RESULT;
                        } finally {
                            LockUtil.unlockAfterRead(lock);
                            logger.info("Unlocked read lock for "+lock.getName()+" in thread "+computingThread.getName());
                        }
                    }
                }, "SmartFutureCacheTest.testSuspendAndResume");
        reader = new LockingScript(lock, sfc);
        readerThread = new Thread(reader, "readerThread");
        writer = new LockingScript(lock, sfc);
        writerThread = new Thread(writer, "writerThread");
        readerThread.start();
        writerThread.start();
    }
    
    @Test
    public void testBasicLockingAndUnlockingWithScripts() throws InterruptedException {
        logger.info("starting testBasicLockingAndUnlockingWithScripts");
        assertFalse(Util.contains(lock.getReaders(), readerThread));
        try {
            reader.performAndWait(Command.LOCK_FOR_READ);
            assertTrue(Util.contains(lock.getReaders(), readerThread));
            writer.perform(Command.LOCK_FOR_WRITE);
            assertNull(lock.getWriter());
        } finally {
            reader.performAndWait(Command.UNLOCK_AFTER_READ);
            assertFalse(Util.contains(lock.getReaders(), readerThread));
            writer.performAndWait(Command.UNLOCK_AFTER_WRITE);
        }
    }
    
    @Test
    public void testBasicCaching() throws InterruptedException {
        logger.info("starting testBasicCaching");
        assertNull(computingThread);
        sfc.triggerUpdate(CACHE_KEY, /* updateInterval */ null);
        String result = sfc.get(CACHE_KEY, /* waitForLatest */ true);
        assertEquals(RESULT, result);
        synchronized (cacheUpdateBegunMonitor) {
            if (computingThread == null) {
                cacheUpdateBegunMonitor.wait(/* timeout in milliseconds */ 10000); // expectedly, the cache update will run, setting the computingThread field
            }
        }
        assertNotNull(computingThread);
    }
    
    @Test
    public void testReadReadDeadlockBetweenGetterAndTriggerInSynchronousScenario() throws InterruptedException {
        logger.info("starting testReadReadDeadlockBetweenGetterAndTriggerInSynchronousScenario");
        long start = System.currentTimeMillis();
        sfc.suspend();
        sfc.triggerUpdate(CACHE_KEY, /* update interval */ null); // queues the update, but get(CACHE_KEY, true) will now trigger recalculation synchronously
        reader.performAndWait(Command.LOCK_FOR_READ);
        writer.perform(Command.LOCK_FOR_WRITE);
        // in suspended mode, the following will trigger a re-calculation immediately,
        // and the locks from the readerThread will be propagated to the computing thread
        reader.performAndWait(Command.GET_LATEST_FROM_CACHE);
        synchronized (cacheUpdateBegunMonitor) {
            if (computingThread == null) {
                cacheUpdateBegunMonitor.wait(/* timeout in milliseconds */ 10000); // expectedly, the cache update will run, setting the computingThread field
            }
        }
        try {
            assertNotNull(computingThread);
        } finally {
            reader.performAndWait(Command.UNLOCK_AFTER_READ); // this shall unblock the writer
            writer.performAndWait(Command.UNLOCK_AFTER_WRITE);
            assertTrue(System.currentTimeMillis()-start < 5000); // must not take longer than 5s, otherwise a locking conflict must have occurred;
        }
        // see also LockUtil.NUMBER_OF_SECONDS_TO_WAIT_FOR_LOCK
    }
    
    @Test
    public void testRenetranceOfReadLockHeldAfterWriterTriesToGetFairLock() throws InterruptedException {
        logger.info("starting testRenetranceOfReadLockHeldAfterWriterTriesToGetFairLock");
        long start = System.currentTimeMillis();
        reader.performAndWait(Command.LOCK_FOR_READ);
        writer.perform(Command.LOCK_FOR_WRITE);
        reader.performAndWait(Command.LOCK_FOR_READ);
        reader.performAndWait(Command.UNLOCK_AFTER_READ);
        try {
            assertNull(lock.getWriter());
        } finally {
            reader.performAndWait(Command.UNLOCK_AFTER_READ); // this shall unblock the writer
            writer.performAndWait(Command.UNLOCK_AFTER_WRITE);
            assertTrue(System.currentTimeMillis()-start < 5000); // must not take longer than 5s, otherwise a locking conflict must have occurred;
        }
        // see also LockUtil.NUMBER_OF_SECONDS_TO_WAIT_FOR_LOCK
    }
    
    @Test
    public void testReadReadDeadlockBetweenGetterAndTriggerInAsynchronousScenario() throws InterruptedException {
        logger.info("starting testReadReadDeadlockBetweenGetterAndTriggerInAsynchronousScenario");
        long start = System.currentTimeMillis();
        try {
            reader.performAndWait(Command.LOCK_FOR_READ);
            writer.perform(Command.LOCK_FOR_WRITE);
            writer.waitUntilWaitingForLock();
            sfc.triggerUpdate(CACHE_KEY, /* update interval */null); // starts the update which
            // in suspended mode, the following will trigger a re-calculation immediately,
            // and the locks from the readerThread will be propagated to the computing thread
            reader.performAndWait(Command.GET_LATEST_FROM_CACHE);
            synchronized (cacheUpdateBegunMonitor) {
                if (computingThread == null) {
                    cacheUpdateBegunMonitor.wait(/* timeout in milliseconds */ 10000); // expectedly, the cache update will run, setting the computingThread field
                }
            }
            assertNotNull(computingThread);
        } finally {
            reader.performAndWait(Command.UNLOCK_AFTER_READ); // this shall unblock the writer
            writer.performAndWait(Command.UNLOCK_AFTER_WRITE);
            assertTrue(System.currentTimeMillis() - start < 10000); // must not take longer than 5s, otherwise a locking
                                                                    // conflict must have occurred;
        }
        // see also LockUtil.NUMBER_OF_SECONDS_TO_WAIT_FOR_LOCK
    }
    
    @AfterEach
    public void tearDown() throws InterruptedException {
        reader.perform(Command.EXIT);
        writer.perform(Command.EXIT);
        readerThread.join();
        writerThread.join();
        assertFalse(reader.isRunning());
        assertFalse(writer.isRunning());
        assertTrue(Util.isEmpty(lock.getReaders()));
        assertNull(lock.getWriter());
    }
    
    private enum Command { EXIT, LOCK_FOR_READ, LOCK_FOR_WRITE, UNLOCK_AFTER_READ, UNLOCK_AFTER_WRITE, TRIGGER_CACHE_UPDATE,
        GET_FROM_CACHE, GET_LATEST_FROM_CACHE }

    private static class LockingScript implements Runnable {
        private static final Logger logger = Logger.getLogger(LockingScript.class.getName());
        private boolean running;
        
        /**
         * Number of commands in {@link #commandQueue}, plus 1 if there is currently one command already taken
         * from the queue and being processed. Don't access this directly to know if there are still requests pending
         * or being processed. Synchronization happens on {@link #commandQueue} object with notifications upon every change.
         */
        private int queuedAndProcessing;
        
        private boolean waitingForLock;
        private final SmartFutureCache<String, String, EmptyUpdateInterval> sfc;
        private final NamedReentrantReadWriteLock lock;
        private final BlockingQueue<Command> commandQueue;

        public LockingScript(NamedReentrantReadWriteLock lock, SmartFutureCache<String, String, EmptyUpdateInterval> sfc) {
            this.lock = lock;
            this.commandQueue = new LinkedBlockingQueue<Command>();
            this.sfc = sfc;
        }
        
        public void waitUntilWaitingForLock() throws InterruptedException {
            synchronized (this) {
                while (!waitingForLock) {
                    wait();
                }
            }
            Thread.sleep(10); // sleep a little to make it incredibly likely that the LockUtil.lockFor...(...) method now really acquires the lock
        }

        public void perform(Command command) {
            synchronized (commandQueue) {
                commandQueue.offer(command);
                queuedAndProcessing++;
                commandQueue.notifyAll();
            }
        }
        
        public void performAndWait(Command command) throws InterruptedException {
            synchronized (commandQueue) {
                commandQueue.offer(command);
                queuedAndProcessing++;
                while (queuedAndProcessing > 0) {
                    commandQueue.wait();
                    commandQueue.notifyAll();
                }
            }
        }
        
        public boolean isRunning() {
            return running;
        }
        
        @Override
        public void run() {
            synchronized (this) {
                running = true;
                this.notifyAll();
            }
            try {
                Command command;
                while ((command = commandQueue.take()) != Command.EXIT) {
                    logger.info("Took command "+command.name()+" in thread "+Thread.currentThread().getName());
                    switch (command) {
                    case LOCK_FOR_READ:
                        synchronized (this) {
                            waitingForLock = true;
                            notifyAll(); // as good as it gets; the lock statement is still outside the synchronized block
                        }
                        LockUtil.lockForRead(lock);
                        waitingForLock = false;
                        break;
                    case LOCK_FOR_WRITE:
                        synchronized (this) {
                            waitingForLock = true;
                            notifyAll(); // as good as it gets; the lock statement is still outside the synchronized block
                        }
                        LockUtil.lockForWrite(lock);
                        waitingForLock = false;
                        break;
                    case UNLOCK_AFTER_READ:
                        LockUtil.unlockAfterRead(lock);
                        break;
                    case UNLOCK_AFTER_WRITE:
                        LockUtil.unlockAfterWrite(lock);
                        break;
                    case EXIT:
                        throw new RuntimeException("We excluded this in the while condition; what's going on here...?");
                    case GET_FROM_CACHE:
                        sfc.get(CACHE_KEY, /* waitForLatest */ false);
                        break;
                    case GET_LATEST_FROM_CACHE:
                        sfc.get(CACHE_KEY, /* waitForLatest */ true);
                        break;
                    case TRIGGER_CACHE_UPDATE:
                        sfc.triggerUpdate(CACHE_KEY, /* updateInterval */ null);
                        break;
                    }
                    logger.info("Done processing command "+command.name()+" in thread "+Thread.currentThread().getName());
                    synchronized (commandQueue) {
                        queuedAndProcessing--;
                        commandQueue.notifyAll(); // unblock wait in performAndWait
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Someone interrupted us", e);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Exception in LockingScript.run() for thread "+Thread.currentThread().getName(), ex);
                throw ex;
            } finally {
                running = false;
            }
            logger.info("Took and processed command "+Command.EXIT.name()+" in thread "+Thread.currentThread().getName());
        }
    }

}
