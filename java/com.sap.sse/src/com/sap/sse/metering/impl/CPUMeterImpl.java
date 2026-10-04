package com.sap.sse.metering.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.concurrent.RunnableWithException;
import com.sap.sse.concurrent.RunnableWithResult;
import com.sap.sse.concurrent.RunnableWithResultAndException;
import com.sap.sse.metering.CPUMeter;

public class CPUMeterImpl implements CPUMeter {
    private static final String NULL = "___null___" + new Random().nextDouble();
    private static final ThreadMXBean threadMxBean;
    private final ConcurrentMap<String, AtomicLong> totalCPUTimePerKeyInNanos;
    private final ConcurrentMap<String, AtomicLong> totalCPUTimePerKeyInUserModeInNanos;
    
    static {
        threadMxBean = ManagementFactory.getThreadMXBean();
        threadMxBean.setThreadCpuTimeEnabled(true);
    }
    
    public CPUMeterImpl() {
        totalCPUTimePerKeyInNanos = new ConcurrentHashMap<>();
        totalCPUTimePerKeyInUserModeInNanos = new ConcurrentHashMap<>();
    }
    
    private String escapeNull(String key) {
        return key == null ? NULL : key;
    }
    
    private String unescapeNull(String key) {
        return key == NULL ? null : key;
    }
    
    @Override
    public void runWithCPUMeter(Runnable runnable, String key) {
        callWithCPUMeter((RunnableWithResult<Void>) ()->{
            runnable.run();
            return null;
        });
    }
    
    @Override
    public <T> T callWithCPUMeter(RunnableWithResult<T> callable, String key) {
        return callWithCPUMeterWithException((RunnableWithResultAndException<T, RuntimeException>) ()->{
            return callable.run();
        }, key);
    }

    @Override
    public <T, E extends Throwable> T callWithCPUMeterWithException(RunnableWithResultAndException<T, E> callable, String key) throws E {
        final String nullEscapedKey = escapeNull(key);
        final long cpuAtStartInNanos = threadMxBean.getCurrentThreadCpuTime();
        final long cpuUserTimeAtStartInNanos = threadMxBean.getCurrentThreadUserTime();
        final T result = callable.run();
        totalCPUTimePerKeyInNanos.computeIfAbsent(nullEscapedKey, k->new AtomicLong()).addAndGet(threadMxBean.getCurrentThreadCpuTime()-cpuAtStartInNanos);
        totalCPUTimePerKeyInUserModeInNanos.computeIfAbsent(nullEscapedKey, k->new AtomicLong()).addAndGet(threadMxBean.getCurrentThreadUserTime()-cpuUserTimeAtStartInNanos);
        return result;
    }

    @Override
    public <E extends Exception> void runWithCPUMeter(RunnableWithException<E> runnableWithException, String key) throws E {
        try {
            callWithCPUMeterWithException(()->{
                runnableWithException.run();
                return null;
            }, key);
        } catch (Exception e) {
            @SuppressWarnings("unchecked") // the runnable can only throw an E
            final E ex = (E) e;
            throw ex;
        }
    }

    @Override
    public Duration getTotalCPUTime() {
        long nanosSum = 0l;
        for (final Entry<String, AtomicLong> e : totalCPUTimePerKeyInNanos.entrySet()) {
            nanosSum += e.getValue().longValue();
        }
        return new MillisecondsDurationImpl(nanosSum / 1_000_000l);
    }

    @Override
    public Duration getTotalCPUTimeInUserMode() {
        long nanosSum = 0l;
        for (final Entry<String, AtomicLong> e : totalCPUTimePerKeyInUserModeInNanos.entrySet()) {
            nanosSum += e.getValue().longValue();
        }
        return new MillisecondsDurationImpl(nanosSum / 1_000_000l);
    }

    @Override
    public Duration getTotalCPUTime(String key) {
        return new MillisecondsDurationImpl(totalCPUTimePerKeyInNanos.getOrDefault(escapeNull(key), new AtomicLong()).longValue() / 1_000_000l);
    }

    @Override
    public Duration getTotalCPUTimeInUserMode(String key) {
        return new MillisecondsDurationImpl(totalCPUTimePerKeyInUserModeInNanos.getOrDefault(escapeNull(key), new AtomicLong()).longValue() / 1_000_000l);
    }

    @Override
    public Map<String, Duration> getTotalCPUTimesByKey() {
        final Map<String, Duration> result = new HashMap<>();
        for (final Entry<String, AtomicLong> e: totalCPUTimePerKeyInNanos.entrySet()) {
            result.put(unescapeNull(e.getKey()), new MillisecondsDurationImpl(e.getValue().longValue() / 1_000_000l));
        }
        return result;
    }

    @Override
    public Map<String, Duration> getTotalCPUTimesInUserModeByKey() {
        final Map<String, Duration> result = new HashMap<>();
        for (final Entry<String, AtomicLong> e: totalCPUTimePerKeyInUserModeInNanos.entrySet()) {
            result.put(unescapeNull(e.getKey()), new MillisecondsDurationImpl(e.getValue().longValue() / 1_000_000l));
        }
        return result;
    }
}
