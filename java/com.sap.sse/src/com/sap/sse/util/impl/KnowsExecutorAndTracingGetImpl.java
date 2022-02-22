package com.sap.sse.util.impl;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sse.util.ThreadPoolUtil;

/**
 * Can be used with a {@link Future} task and outputs trace messages to the log after
 * {@link #MILLIS_AFTER_WHICH_TO_TRACE_NON_RETURNING_GET} milliseconds of a {@link #get()} call not returning. When used
 * in conjunction with a {@link NamedTracingScheduledThreadPoolExecutor} such as the ones returned by
 * {@link ThreadPoolUtil}, the task will be
 * 
 * @author Axel Uhl (d043530)
 *
 * @param <V>
 */
public class KnowsExecutorAndTracingGetImpl<V> extends HasTracingGetImpl<V> implements KnowsExecutorAndTracingGet<V> {
    private static final Logger logger = Logger.getLogger(KnowsExecutorAndTracingGetImpl.class.getName());
    private static Field inheritableThreadLocalsField;
    private static Method childValueMethod;
    private static Field threadLocalsField;
    private ThreadPoolExecutor executorThisTaskIsScheduledFor;
    private final WeakHashMap<ThreadLocal<Object>, Object> threadLocalValuesToInherit;
    
    static {
        try {
            inheritableThreadLocalsField = Thread.class.getDeclaredField("inheritableThreadLocals");
            inheritableThreadLocalsField.setAccessible(true);
            childValueMethod = InheritableThreadLocal.class.getDeclaredMethod("childValue", Object.class);
            childValueMethod.setAccessible(true);
            threadLocalsField = Thread.class.getDeclaredField("threadLocals");
            threadLocalsField.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException | NoSuchMethodException e) {
            logger.log(Level.SEVERE,
                    "Problem finding field inheritableThreadLocals; will be unable to forward InheritableThreadLocal values to thread pool threads",
                    e);
        }
    }
    
    /**
     * Copies the values of all {@link InheritableThreadLocal} to the {@link #threadLocalValuesToInherit} map so
     * that they can be set just before this task is run.
     */
    public KnowsExecutorAndTracingGetImpl() {
        final WeakHashMap<ThreadLocal<Object>, Object> threadLocalValuesToInheritValue = new WeakHashMap<>();
        final Thread currentThread = Thread.currentThread();
        handleThreadLocals(currentThread, inheritableThreadLocalsField, (key, value) -> {
            threadLocalValuesToInheritValue.put(key, value);
        });
        threadLocalValuesToInherit = threadLocalValuesToInheritValue;
    }
    
    @Override
    public void setInheritableThreadLocalValues() {
        for (Entry<ThreadLocal<Object>, Object> e : threadLocalValuesToInherit.entrySet()) { 
            e.getKey().set(e.getValue());
        }
    }

    @Override
    public void removeInheritableThreadLocalValues() {
        for (Entry<ThreadLocal<Object>, Object> e : threadLocalValuesToInherit.entrySet()) { 
            e.getKey().remove();
        }
    }

    @Override
    public void removeThreadLocalValues() {
        handleThreadLocals(Thread.currentThread(), threadLocalsField, (key, value) -> key.remove());
    }

    private void handleThreadLocals(final Thread thread, final Field threadLocalSourceField,
            final BiConsumer<ThreadLocal<Object>, Object> valueHandler) {
        try {
            final Object threadLocals = threadLocalSourceField.get(thread);
            if (threadLocals != null) {
                final Method expungeStaleEntries = threadLocals.getClass().getDeclaredMethod("expungeStaleEntries");
                expungeStaleEntries.setAccessible(true);
                expungeStaleEntries.invoke(threadLocals);
                final Field tableField = threadLocals.getClass().getDeclaredField("table");
                tableField.setAccessible(true);
                final Object[] table = (Object[]) tableField.get(threadLocals);
                for (int j = 0; j < table.length; j++) {
                    final WeakReference<?> entry = (WeakReference<?>) table[j];
                    if (entry != null) {
                        @SuppressWarnings("unchecked")
                        final ThreadLocal<Object> key = (ThreadLocal<Object>) entry.get();
                        if (key != null) {
                            final Field valueField = entry.getClass().getDeclaredField("value");
                            valueField.setAccessible(true);
                            final Object value = key instanceof InheritableThreadLocal
                                    ? childValueMethod.invoke(key, valueField.get(entry))
                                    : key.get();
                            valueHandler.accept(key, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Problem accessing field inheritableThreadLocals; will be unable to forward InheritableThreadLocal values to thread pool threads",
                    e);
        }
    }

    @Override
    public Map<ThreadLocal<Object>, Object> getThreadLocalValuesToInherit() {
        return Collections.unmodifiableMap(threadLocalValuesToInherit);
    }

    @Override
    public void setExecutorThisTaskIsScheduledFor(ThreadPoolExecutor executorThisTaskIsScheduledFor) {
        this.executorThisTaskIsScheduledFor = executorThisTaskIsScheduledFor;
    }

    @Override
    protected String getAdditionalTraceInfo() {
        return executorThisTaskIsScheduledFor == null ? "not scheduled"
                : ("scheduled with executor " + executorThisTaskIsScheduledFor);
    }
}
