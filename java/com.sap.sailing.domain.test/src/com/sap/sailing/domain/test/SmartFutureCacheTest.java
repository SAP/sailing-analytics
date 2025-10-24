package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sap.sse.util.SmartFutureCache;
import com.sap.sse.util.SmartFutureCache.CacheUpdater;
import com.sap.sse.util.SmartFutureCache.EmptyUpdateInterval;
import com.sap.sse.util.SmartFutureCache.UpdateInterval;

@Timeout(value=2, unit=TimeUnit.MINUTES)
public class SmartFutureCacheTest {
    @Test
    public void testPerformanceOfGetAndCall() {
        SmartFutureCache<String, String, EmptyUpdateInterval> sfc = new SmartFutureCache<String, String, SmartFutureCache.EmptyUpdateInterval>(
                new SmartFutureCache.AbstractCacheUpdater<String, String, SmartFutureCache.EmptyUpdateInterval>() {
                    @Override
                    public String computeCacheUpdate(String key, EmptyUpdateInterval updateInterval) {
                        return key;
                    }
                }, "SmartFutureCacheTest.testPerformanceOfGetAndCall");
        long start = System.currentTimeMillis();
        for (int i=0; i<100000; i++) {
            sfc.triggerUpdate("humba", /* update interval */ null);
        }
        for (int i=0; i<100000; i++) {
            sfc.get("humba", /* waitForLatest */ false);
        }
        System.out.println("testPerformanceOfGetAndCall took "+(System.currentTimeMillis()-start)+"ms");
    }

    @Test
    public void testExceptionInComputeCacheUpdate() {
        final boolean[] throwException = new boolean[1];
        synchronized (this) { // make sure that other threads see this after the synchronized block has completed
            throwException[0] = true;
        }
        SmartFutureCache<String, String, EmptyUpdateInterval> sfc = new SmartFutureCache<String, String, SmartFutureCache.EmptyUpdateInterval>(
                new SmartFutureCache.AbstractCacheUpdater<String, String, SmartFutureCache.EmptyUpdateInterval>() {
                    @Override
                    public String computeCacheUpdate(String key, EmptyUpdateInterval updateInterval) {
                        synchronized (SmartFutureCacheTest.this) { // make sure we see the latest change to throwException
                            if (throwException[0]) {
                                throw new NullPointerException("Humba");
                            } else {
                                return "Humba";
                            }
                        }
                    }
                }, "SmartFutureCacheTest.testExceptionInComputeCacheUpdate");
        sfc.triggerUpdate("humba", /* update interval */ null);
        try {
            // during the first call, expecting exception
            sfc.get("humba", /* waitForLatest */ true);
            fail("Expected RuntimeException because computeCacheUpdate threw one");
        } catch (RuntimeException expected) {
            assertSame(ExecutionException.class, expected.getCause().getClass());
        }
        throwException[0] = false;
        sfc.triggerUpdate("humba", /* update interval */ null);
        assertEquals("Humba", sfc.get("humba", /* waitForLatest */ true));
    }
    
    @Test
    public void testSuspendAndResume() {
        final boolean[] updateWasCalled = new boolean[1];
        SmartFutureCache<String, String, SmartFutureCache.EmptyUpdateInterval> sfc = new SmartFutureCache<String, String, SmartFutureCache.EmptyUpdateInterval>(
                new SmartFutureCache.AbstractCacheUpdater<String, String, SmartFutureCache.EmptyUpdateInterval>() {
                    @Override
                    public String computeCacheUpdate(String key, EmptyUpdateInterval updateInterval) throws Exception {
                        updateWasCalled[0] = true;
                        return "Humba";
                    }
                }, "SmartFutureCacheTest.testSuspendAndResume");
        sfc.suspend();
        sfc.triggerUpdate("Trala", /* updateInterval */ null);
        assertNull(sfc.get("Trala", /* waitForLatest */ false));
        assertFalse(updateWasCalled[0]);
        assertEquals("Humba", sfc.get("Trala", /* waitForLatest */ true));
        assertTrue(updateWasCalled[0]);
    }

    @Test
    public void testUnsuspended() throws InterruptedException {
        final boolean[] updateWasCalled = new boolean[1];
        final boolean[] cacheWasCalled = new boolean[1];
        final boolean[] mayProceed = new boolean[1];
        
        SmartFutureCache<String, String, SmartFutureCache.EmptyUpdateInterval> sfc = new SmartFutureCache<String, String, SmartFutureCache.EmptyUpdateInterval>(
                new SmartFutureCache.AbstractCacheUpdater<String, String, SmartFutureCache.EmptyUpdateInterval>() {
                    @Override
                    public String computeCacheUpdate(String key, EmptyUpdateInterval updateInterval) throws Exception {
                        synchronized (mayProceed) {
                            while (!mayProceed[0]) {
                                try {
                                    mayProceed.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } // wait until test driver has checked the un-updated cache
                            }
                        }
                        updateWasCalled[0] = true;
                        return "Humba";
                    }
                }, "SmartFutureCacheTest.testUnsuspended") {
                    @Override
                    protected void cache(String key, String value) {
                        super.cache(key, value);
                        synchronized (cacheWasCalled) {
                            cacheWasCalled[0] = true;
                            cacheWasCalled.notifyAll();
                        }
                    }
        };
        sfc.triggerUpdate("Trala", /* updateInterval */ null); // will be held up in computeCacheUpdate
        assertNull(sfc.get("Trala", /* waitForLatest */ false));
        synchronized (mayProceed) {
            mayProceed[0] = true;
            mayProceed.notifyAll(); // let cache update proceed
        }
        synchronized (cacheWasCalled) {
            while (!cacheWasCalled[0]) {
                cacheWasCalled.wait(); // and wait for update to have updated the cache
            }
        }
        assertTrue(updateWasCalled[0]); // update must have been called by now because we already waited for the cache to have been updated
        assertEquals("Humba", sfc.get("Trala", /* waitForLatest */ false));
        assertEquals("Humba", sfc.get("Trala", /* waitForLatest */ true));
    }
    
    private static class FromAToBUpdateInterval implements UpdateInterval<FromAToBUpdateInterval> {
        private final int a;
        private final int b;
        
        public FromAToBUpdateInterval(int a, int b) {
            super();
            this.a = a;
            this.b = b;
        }

        @Override
        public FromAToBUpdateInterval join(FromAToBUpdateInterval otherUpdateInterval) {
            return new FromAToBUpdateInterval(Math.min(getA(), otherUpdateInterval.getA()), Math.max(getB(), otherUpdateInterval.getB()));
        }

        public int getA() {
            return a;
        }

        public int getB() {
            return b;
        }

        @Override
        public String toString() {
            return "["+getA()+", "+getB()+"]";
        }
    }

    @Test
    public void testJoiningOfUpdateIntervalsWhileSuspended() {
        final boolean[] updateWasCalled = new boolean[1];
        SmartFutureCache<String, Integer, FromAToBUpdateInterval> sfc = new SmartFutureCache<String, Integer, FromAToBUpdateInterval>(
                new SmartFutureCache.AbstractCacheUpdater<String, Integer, FromAToBUpdateInterval>() {
                    @Override
                    public Integer computeCacheUpdate(String key, FromAToBUpdateInterval updateInterval) throws Exception {
                        updateWasCalled[0] = true;
                        return updateInterval.getA() + updateInterval.getB();
                    }
                }, "SmartFutureCacheTest.testJoiningOfUpdateIntervalsWhileSuspended");
        sfc.suspend();
        sfc.triggerUpdate("Trala", new FromAToBUpdateInterval(42, 48));
        assertNull(sfc.get("Trala", /* waitForLatest */ false));
        assertFalse(updateWasCalled[0]);
        sfc.triggerUpdate("Trala", new FromAToBUpdateInterval(43, 49)); // should result in update interval 42..49
        assertEquals(Integer.valueOf(91), sfc.get("Trala", /* waitForLatest */ true));
        assertTrue(updateWasCalled[0]);
    }

    @Test
    public void testJoiningOfUpdateIntervalsWhenBeingResumed() throws InterruptedException {
        final boolean[] updateWasCalled = new boolean[1];
        final boolean[] cacheWasCalled = new boolean[1];
        SmartFutureCache<String, Integer, FromAToBUpdateInterval> sfc = new SmartFutureCache<String, Integer, FromAToBUpdateInterval>(
                        new SmartFutureCache.AbstractCacheUpdater<String, Integer, FromAToBUpdateInterval>() {
                            @Override
                            public Integer computeCacheUpdate(String key, FromAToBUpdateInterval updateInterval) throws Exception {
                        updateWasCalled[0] = true;
                        return updateInterval.getA() + updateInterval.getB();
                    }
                }, "SmartFutureCacheTest.testJoiningOfUpdateIntervalsWhenBeingResumed") {
                    @Override
                    protected void cache(String key, Integer value) {
                        super.cache(key, value);
                        synchronized (cacheWasCalled) {
                            cacheWasCalled[0] = true;
                            cacheWasCalled.notifyAll();
                        }
                    }
        };
        sfc.suspend();
        sfc.triggerUpdate("Trala", new FromAToBUpdateInterval(42, 48));
        assertNull(sfc.get("Trala", /* waitForLatest */ false));
        assertFalse(updateWasCalled[0]);
        sfc.triggerUpdate("Trala", new FromAToBUpdateInterval(43, 49)); // should result in update interval 42..49
        assertFalse(updateWasCalled[0]);
        sfc.resume();
        synchronized (cacheWasCalled) {
            while (!cacheWasCalled[0]) {
                cacheWasCalled.wait(); // and wait for update to have updated the cache
            }
        }
        assertTrue(updateWasCalled[0]);
        assertEquals(Integer.valueOf(91), sfc.get("Trala", /* waitForLatest */ true));
    }
    
    @Disabled // used only to solve bug 1314; it's hard to test re-cycling of tasks reliably without depending on timing issues
    @Test
    public void testOverloadingCacheWithUpdateRequests() throws InterruptedException {
        final Set<String> updateWasCalled = new ConcurrentSkipListSet<String>();
        final boolean[] cacheWasCalled = new boolean[1];
        final int[] computeCacheUpdateCount = new int[1];
        SmartFutureCache<String, Integer, FromAToBUpdateInterval> sfc = new SmartFutureCache<String, Integer, FromAToBUpdateInterval>(
                new SmartFutureCache.AbstractCacheUpdater<String, Integer, FromAToBUpdateInterval>() {
                    @Override
                    public Integer computeCacheUpdate(String key, FromAToBUpdateInterval updateInterval)
                            throws Exception {
                        updateWasCalled.add(key);
                        computeCacheUpdateCount[0]++;
                        Thread.sleep(10); // pretending a long calculation
                        return updateInterval.getA() + updateInterval.getB();
                    }
                }, "SmartFutureCacheTest.testJoiningOfUpdateIntervalsWhenBeingResumed") {
            @Override
            protected void cache(String key, Integer value) {
                super.cache(key, value);
                synchronized (cacheWasCalled) {
                    cacheWasCalled[0] = true;
                    cacheWasCalled.notifyAll();
                }
            }
        };
        String[] strings = new String[100];
        for (int i=0; i<strings.length; i++) {
            strings[i] = Integer.toString(i);
        }
        Random random = new Random();
        // Trigger very many updates shortly after each other, causing many re-calculations to be scheduled.
        final int numberOfUpdateTriggers = 1000000;
        Set<Integer> updatesTriggeredFor = new HashSet<Integer>();
        for (int i=0; i<numberOfUpdateTriggers; i++) {
            final int nextInt = random.nextInt(strings.length);
            sfc.triggerUpdate(strings[nextInt], new FromAToBUpdateInterval(i, nextInt));
            updatesTriggeredFor.add(nextInt);
        }
        System.out.println(updatesTriggeredFor.size());
        System.out.println(updatesTriggeredFor);
        System.out.println(updateWasCalled.size());
        System.out.println(updateWasCalled);
        System.out.println(computeCacheUpdateCount[0]);
        System.out.println("Tasks re-used: "+sfc.getSmartFutureCacheTaskReuseCounter());
        Thread.sleep(500);
        System.out.println(updateWasCalled.size());
        System.out.println(updateWasCalled);
        System.out.println(computeCacheUpdateCount[0]);
        System.out.println("Tasks re-used: "+sfc.getSmartFutureCacheTaskReuseCounter());
        Thread.sleep(500);
        System.out.println(updateWasCalled.size());
        System.out.println(updateWasCalled);
        System.out.println(computeCacheUpdateCount[0]);
        System.out.println("Tasks re-used: "+sfc.getSmartFutureCacheTaskReuseCounter());
        assertEquals(updatesTriggeredFor.size(), updateWasCalled.size());
    }
    
    @Test
    public void testIfNoNewFuturesAreRunForTheSameKeyWhileCurrentTaskIsSleeping() throws InterruptedException, BrokenBarrierException {
        final AtomicInteger callCounter = new AtomicInteger(0);
        CyclicBarrier barrier = new CyclicBarrier(2);
        CacheUpdater<Integer, Integer, EmptyUpdateInterval> cacheUpdater = new CacheUpdater<Integer, Integer, SmartFutureCache.EmptyUpdateInterval>() {
            @Override
            public Integer computeCacheUpdate(Integer key, EmptyUpdateInterval updateInterval) throws Exception {
                synchronized (callCounter) {
                    if (key == 1) {
                        callCounter.incrementAndGet();
                    }
                }
                barrier.await();
                barrier.await();
                barrier.await();
                // For this test case value will be same as key, when updated.
                return key;
            }

            @Override
            public Integer provideNewCacheValue(Integer key, Integer oldCacheValue, Integer computedCacheUpdate,
                    EmptyUpdateInterval updateInterval) {
                return computedCacheUpdate;
            }
        };
        SmartFutureCache<Integer, Integer, EmptyUpdateInterval> testCache = new SmartFutureCache<Integer, Integer, SmartFutureCache.EmptyUpdateInterval>(
                cacheUpdater, "SmartFutureTestCacheLock");
        testCache.triggerUpdate(1, null);
        barrier.await();
        assertEquals(1, callCounter.get());
        testCache.triggerUpdate(1, null);
        barrier.await();
        // Counter should still be one here, since first future is still sleeping at this point
        assertEquals(1, callCounter.get());
        barrier.await();
        
        // Now the first future should be done, and the second update should have been called, so the counter should be 2
        barrier.await();
        assertEquals(2, callCounter.get());
        barrier.await();
        barrier.await();
    }
    
    @Test
    public void testTriggerAndGetWithWaitForLatestWithRunningFutureOnSuspendedCache() throws InterruptedException, BrokenBarrierException {
        final AtomicInteger callCounter = new AtomicInteger(0);
        CyclicBarrier barrier = new CyclicBarrier(2);
        CacheUpdater<Integer, Integer, EmptyUpdateInterval> cacheUpdater = new CacheUpdater<Integer, Integer, SmartFutureCache.EmptyUpdateInterval>() {
            @Override
            public Integer computeCacheUpdate(Integer key, EmptyUpdateInterval updateInterval) throws Exception {
                synchronized (callCounter) {
                    if (key == 1) {
                        callCounter.incrementAndGet();
                    }
                }
                barrier.await();
                barrier.await();
                barrier.await();
                return callCounter.get();
            }

            @Override
            public Integer provideNewCacheValue(Integer key, Integer oldCacheValue, Integer computedCacheUpdate,
                    EmptyUpdateInterval updateInterval) {
                return computedCacheUpdate;
            }
        };
        SmartFutureCache<Integer, Integer, EmptyUpdateInterval> testCache = new SmartFutureCache<Integer, Integer, SmartFutureCache.EmptyUpdateInterval>(
                cacheUpdater, "SmartFutureTestCacheLock");
        testCache.triggerUpdate(1, null);
        barrier.await();
        assertEquals(1, callCounter.get());
        testCache.triggerUpdate(1, null);
        testCache.suspend();
        CyclicBarrier getResultBarrier = new CyclicBarrier(2);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                //FIXME find a way to guarantee that get is called, while the first computeUpdate Future is still running
                int result = testCache.get(1, true);
                assertEquals(2, result);
                try {
                    getResultBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
        barrier.await();
        barrier.await();
        barrier.await();
        assertEquals(2, callCounter.get());
        //Trigger again. This update should not retrigger add another future, since cache is disabled and no
        // get with waitForLatest is called afterwards
        testCache.triggerUpdate(1, null);
        barrier.await();
        barrier.await();
        //Wait for get Thread to finish with getting and asserting
        getResultBarrier.await();
        assertEquals(2, callCounter.get());
        //Check if no compute Thread is waiting at barrier.
        boolean timeOut = false;
        try {
            barrier.await(10, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            timeOut = true;
        }
        assertTrue(timeOut);
    }
}
