package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.function.IntConsumer;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class CPUTimeMeasurementTest {
    private static final Logger logger = Logger.getLogger(CPUTimeMeasurementTest.class.getName());

    private final static ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
    
    @BeforeAll
    public static void setUp() {
        logger.info("CPU time enabled by default: "+threadMxBean.isThreadCpuTimeEnabled());
        if (!threadMxBean.isThreadCpuTimeEnabled()) {
            threadMxBean.setThreadCpuTimeEnabled(true);
        }
    }
    
    @Disabled("Use this to find out whether there is a significant penalty for CPU metering; it seems there isn't...")
    @Test
    public void measurePerformancePenalty() {
        final int AMOUNT_OF_WORK = 10_000_000;
        useSingleCPUOnCurrentThread(AMOUNT_OF_WORK); // warm up
        final long durationWithCpuTimeEnabled = useSingleCPUOnCurrentThread(AMOUNT_OF_WORK);
        threadMxBean.setThreadCpuTimeEnabled(false);
        final long durationWithCpuTimeDisabled = useSingleCPUOnCurrentThread(AMOUNT_OF_WORK);
        logger.info("work duration with CPU time keeping enabled/disabled: "+durationWithCpuTimeEnabled+"/"+durationWithCpuTimeDisabled
                +" ("+((double) durationWithCpuTimeEnabled / (double) durationWithCpuTimeDisabled)+")");
        assertFalse((double) durationWithCpuTimeEnabled > 1.5*durationWithCpuTimeDisabled);
    }
    
    @Test
    public void testSingleThreadedWorkloadOffByLessThanFactorTwo() {
        final int AMOUNT_OF_WORK = 10_000_000;
        final double FACTOR_THRESHOLD = 2.0;
        final long cpuTimeBefore = threadMxBean.getCurrentThreadCpuTime();
        final long durationWithCpuTimeEnabledInMillis = useSingleCPUOnCurrentThread(AMOUNT_OF_WORK);
        final long cpuTimeConsumedInNanos = threadMxBean.getCurrentThreadCpuTime() - cpuTimeBefore;
        final long cpuTimeConsumedInMillis = cpuTimeConsumedInNanos / 1_000_000;
        final double factor = (double) Math.max(durationWithCpuTimeEnabledInMillis, cpuTimeConsumedInMillis) / (double) Math.min(durationWithCpuTimeEnabledInMillis, cpuTimeConsumedInMillis);
        logger.info("CPU time measured: "+cpuTimeConsumedInMillis+"ms; duration measured: "+durationWithCpuTimeEnabledInMillis+"ms; factor "+factor);
        assertTrue((double) Math.max(durationWithCpuTimeEnabledInMillis, cpuTimeConsumedInMillis) / (double) Math.min(durationWithCpuTimeEnabledInMillis, cpuTimeConsumedInMillis) < FACTOR_THRESHOLD,
                "off by more than factor "+FACTOR_THRESHOLD+" ("+factor+"): cpuTimeConsumed="+cpuTimeConsumedInMillis+"; actual duration: "+durationWithCpuTimeEnabledInMillis);
    }
    
    @Test
    public void testMultiThreadedWorkloadOffByLessThanFactorTwo() throws InterruptedException {
        final int AMOUNT_OF_WORK = 10_000_000;
        final double FACTOR_THRESHOLD = 2.0;
        final int NUMBER_OF_THREADS = 2;
        final long[] cpuTimeConsumedInMillis = new long[NUMBER_OF_THREADS];
        final long[] durationWithCpuTimeEnabledInMillis = new long[NUMBER_OF_THREADS];
        final Thread[] threads = new Thread[NUMBER_OF_THREADS];
        final long start = System.currentTimeMillis();
        IntStream.range(0, NUMBER_OF_THREADS).forEach(new IntConsumer() {
            @Override
            public void accept(final int i) {
                threads[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final long cpuTimeBefore = threadMxBean.getCurrentThreadCpuTime();
                        durationWithCpuTimeEnabledInMillis[i] = useSingleCPUOnCurrentThread(AMOUNT_OF_WORK);
                        final long cpuTimeConsumedInNanos = threadMxBean.getCurrentThreadCpuTime() - cpuTimeBefore;
                        cpuTimeConsumedInMillis[i] = cpuTimeConsumedInNanos / 1_000_000;
                        final double factor = (double) Math.max(durationWithCpuTimeEnabledInMillis[i], cpuTimeConsumedInMillis[i]) /
                                (double) Math.min(durationWithCpuTimeEnabledInMillis[i], cpuTimeConsumedInMillis[i]);
                        logger.info("CPU time measured by thread "+i+": "+cpuTimeConsumedInMillis[i]+"ms; duration measured: "+durationWithCpuTimeEnabledInMillis[i]+"ms; factor "+factor);
                    }
                });
                threads[i].start();
            }
        });
        for (final Thread t : threads) {
            t.join();
        }
        final long duration = System.currentTimeMillis()-start;
        long cpuTimeSumMillis = 0;
        for (final long cpuTimeMillis : cpuTimeConsumedInMillis) {
            cpuTimeSumMillis += cpuTimeMillis;
        }
        final long averageCpuTimeMillis = cpuTimeSumMillis / NUMBER_OF_THREADS;
        // we assume that running each thread did take CPU time in the area of overall duration, with true parallelism
        final double factor = (double) Math.max(duration, averageCpuTimeMillis) / (double) Math.min(duration, averageCpuTimeMillis);
        logger.info("CPU time average: "+averageCpuTimeMillis+"ms; duration measured: "+duration+"ms; factor "+factor);
        assertTrue((double) Math.max(duration, averageCpuTimeMillis) / (double) Math.min(duration, averageCpuTimeMillis) < FACTOR_THRESHOLD,
                "off by more than factor "+FACTOR_THRESHOLD+" ("+factor+"): cpuTimeConsumed="+averageCpuTimeMillis+"; actual duration: "+duration);
    }
    
    private long useSingleCPUOnCurrentThread(int howMuch) {
        final long start = System.currentTimeMillis();
        for (int i=0; i<howMuch; i++) {
            final StringBuilder sb = new StringBuilder();
            sb.append(i);
            sb.append("some string");
            if (sb.length() > 15) {
                sb.delete(sb.length()-3, sb.length());
            }
        }
        return System.currentTimeMillis() - start;
    }
}
