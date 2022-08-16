package com.sap.sse.util.impl;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

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
    private ThreadPoolExecutor executorThisTaskIsScheduledFor;
    
    /**
     * Captures any Shiro {@link Subject} through the use of {@link SecurityUtils#getSubject()}. If an
     * {@link IllegalStateException} is raised, the {@link #subject} is set to {@code null}.
     */
    public KnowsExecutorAndTracingGetImpl() {
    }

    private static Optional<Subject> getSubjectOrNull() {
        Optional<Subject> mySubject;
        try {
            mySubject = Optional.of(SecurityUtils.getSubject());
        } catch (IllegalStateException e) {
            mySubject = Optional.empty();
        }
        return mySubject;
    }
    
    public static <T> Callable<T> associateWithSubjectIfAny(Callable<T> callable) {
        return getSubjectOrNull().map(subject->subject.associateWith(callable)).orElse(callable);
    }
    
    public static Runnable associateWithSubjectIfAny(Runnable runnable) {
        return getSubjectOrNull().map(subject->subject.associateWith(runnable)).orElse(runnable);
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
