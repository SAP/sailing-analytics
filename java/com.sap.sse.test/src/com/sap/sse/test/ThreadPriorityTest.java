package com.sap.sse.test;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.sap.sse.util.ThreadPoolUtil;

/**
 * Various sources report different things about Java thread priorities and how they are mapped to operating system
 * thread priorities. If we want to support differently prioritized thread pools through {@link ThreadPoolUtil}, it
 * would be great if we had a good understanding of this this works and what the effects are.<p>
 * 
 * This test runs threads with the various Java thread priorities and lets them do trivial work, counting progress
 * over a fixed period of time. The different progresses of all threads are then output.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class ThreadPriorityTest {
    private static final Logger logger = Logger.getLogger(ThreadPriorityTest.class.getName());
    
    private static class Counter implements Runnable {
        private long progress;
        private volatile boolean stopped;
        
        @Override
        public void run() {
            while (!stopped) {
                progress++;
            }
        }
        
        public void stop() {
            stopped = true;
        }
        
        public long getProgress() {
            return progress;
        }
    }
    
    @Test
    public void testThreadPriority() throws InterruptedException {
        Counter[] tasks = new Counter[Thread.MAX_PRIORITY+1];
        for (int i=Thread.MIN_PRIORITY; i<=Thread.MAX_PRIORITY; i++) {
            tasks[i] = new Counter();
            final Thread thread = new Thread(tasks[i]);
            thread.setPriority(i);
            thread.start();
        }
        Thread.sleep(10000l);
        for (int i=Thread.MIN_PRIORITY; i<=Thread.MAX_PRIORITY; i++) {
            tasks[i].stop();
        }
        for (int i=Thread.MIN_PRIORITY; i<=Thread.MAX_PRIORITY; i++) {
            logger.info("Progress thread with priority "+i+" got to "+tasks[i].getProgress());
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        new ThreadPriorityTest().testThreadPriority();
    }
}
