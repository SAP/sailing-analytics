package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sap.sse.util.impl.ThreadFactoryWithPriority;

/**
 * Tests that deal with the question whether a thread pool may be "depleted" by threads throwing exceptions and hence leading
 * to abnormal worker thread termination.
 * 
 * @author Axel Uhl (D043530)
 *
 */
@Timeout(value=10, unit=TimeUnit.SECONDS)
public class ThreadPoolWithExceptionsTest {
    private static final Logger logger = Logger.getLogger(ThreadPoolWithExceptionsTest.class.getName());
    
    final int THREAD_POOL_SIZE = 100;
    
    private ThreadPoolExecutor executor;
    
    @BeforeEach
    public void setUp() {
        executor = new ThreadPoolExecutor(/* corePoolSize */ THREAD_POOL_SIZE,
                /* maximumPoolSize */ THREAD_POOL_SIZE,
                /* keepAliveTime */ 60, TimeUnit.SECONDS,
                /* workQueue */ new LinkedBlockingQueue<Runnable>(),
                /* thread factory */ new ThreadFactoryWithPriority(Thread.NORM_PRIORITY, /* daemon */ true));
    }
    
    @Test
    public void tryToDepletePoolByTasksThatThrowExceptions() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger(0);
        for (int i=0; i<2*THREAD_POOL_SIZE; i++) {
            final Runnable r = () -> {
                final int count = counter.incrementAndGet();
                logger.info("Worked thread executing job #"+count+": "+Thread.currentThread());
                synchronized (ThreadPoolWithExceptionsTest.this) {
                    ThreadPoolWithExceptionsTest.this.notifyAll();
                }
                throw new NullPointerException();
            };
            executor.execute(r);
        }
        synchronized (this) {
            while (counter.get() < 2*THREAD_POOL_SIZE) {
                this.wait();
            }
        }
        assertEquals(2*THREAD_POOL_SIZE, counter.get());
    }
}
