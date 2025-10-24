package com.sap.sse.util.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.subject.Subject;

import com.sap.sse.common.Util;
import com.sap.sse.util.ThreadPoolUtil;

public class ThreadPoolUtilImpl implements ThreadPoolUtil {
    private static final Logger logger = Logger.getLogger(ThreadPoolUtilImpl.class.getName());
    private static final int REASONABLE_THREAD_POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors()-1, 3);

    private final ScheduledExecutorService defaultBackgroundTaskThreadPoolExecutor;
    private final ScheduledExecutorService defaultForegroundTaskThreadPoolExecutor;
    
    public ThreadPoolUtilImpl() {
        defaultBackgroundTaskThreadPoolExecutor = createBackgroundTaskThreadPoolExecutor("Default background executor");
        defaultForegroundTaskThreadPoolExecutor = createForegroundTaskThreadPoolExecutor(2*REASONABLE_THREAD_POOL_SIZE, "Default foreground executor");
    }
    
    @Override
    public ScheduledExecutorService getDefaultBackgroundTaskThreadPoolExecutor() {
        return defaultBackgroundTaskThreadPoolExecutor;
    }

    @Override
    public ScheduledExecutorService getDefaultForegroundTaskThreadPoolExecutor() {
        return defaultForegroundTaskThreadPoolExecutor;
    }

    @Override
    public ScheduledExecutorService createBackgroundTaskThreadPoolExecutor(String name) {
        return createThreadPoolExecutor(name, Thread.NORM_PRIORITY-1);
    }

    @Override
    public ScheduledExecutorService createBackgroundTaskThreadPoolExecutor(int size, String name) {
        return createThreadPoolExecutor(name, Thread.NORM_PRIORITY-1, size, /* executeExistingDelayedTasksAfterShutdownPolicy */ false);
    }

    @Override
    public ScheduledExecutorService createBackgroundTaskThreadPoolExecutor(int size, String name,
            boolean executeExistingDelayedTasksAfterShutdownPolicy) {
        return createThreadPoolExecutor(name, Thread.NORM_PRIORITY-1, size, executeExistingDelayedTasksAfterShutdownPolicy);
    }

    @Override
    public ScheduledExecutorService createForegroundTaskThreadPoolExecutor(String name) {
        return createThreadPoolExecutor(name, Thread.NORM_PRIORITY);
    }

    @Override
    public ScheduledExecutorService createForegroundTaskThreadPoolExecutor(int size, String name) {
        return createThreadPoolExecutor(name, Thread.NORM_PRIORITY, size, /* executeExistingDelayedTasksAfterShutdownPolicy */ false);
    }

    private ScheduledExecutorService createThreadPoolExecutor(String name, final int priority) {
        return createThreadPoolExecutor(name, priority, /* corePoolSize */ REASONABLE_THREAD_POOL_SIZE, /* executeExistingDelayedTasksAfterShutdownPolicy */ false);
    }

    private ScheduledExecutorService createThreadPoolExecutor(String name, final int priority, final int size, boolean executeExistingDelayedTasksAfterShutdownPolicy) {
        final NamedTracingScheduledThreadPoolExecutor result = new NamedTracingScheduledThreadPoolExecutor(
                name, /* corePoolSize */ size, new ThreadFactoryWithPriority(name, priority, /* daemon */ true));
        result.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return result;
    }

    @Override
    public int getReasonableThreadPoolSize() {
        return REASONABLE_THREAD_POOL_SIZE;
    }

    @Override
    public void logExceptionsFromFutures(Level logLevel, String messageTemplate, Iterable<? extends Future<?>> futures) {
        for (final Future<?> result : futures) {
            try {
                result.get();
            } catch (ExecutionException e) {
                Throwable t = e.getCause();
                logger.log(logLevel, String.format(messageTemplate, t.getMessage()), t);
            } catch (InterruptedException e) {
                logger.log(logLevel, String.format(messageTemplate, e.getMessage()), e);
            }
        }
    }

    @Override
    public <T> List<Future<T>> invokeAllAndLogExceptions(ExecutorService executor, Level logLevel, String messageTemplate,
            Iterable<? extends Callable<T>> tasks) {
        final List<Callable<T>> tasksAsList = new ArrayList<>();
        Util.addAll(tasks, tasksAsList);
        List<Future<T>> result = null;
        try {
            result = executor.invokeAll(tasksAsList);
            logExceptionsFromFutures(logLevel, messageTemplate, result);
        } catch (InterruptedException e) {
            logger.log(logLevel, String.format(messageTemplate, e.getMessage()), e);
        }
        return result;
    }

    private Optional<Subject> getSubjectOrNull() {
        Optional<Subject> mySubject;
        try {
            mySubject = Optional.ofNullable(SecurityUtils.getSubject());
        } catch (IllegalStateException | UnavailableSecurityManagerException e) {
            mySubject = Optional.empty();
        }
        return mySubject;
    }

    @Override
    public Runnable associateWithSubjectIfAny(Runnable runnable) {
        return getSubjectOrNull().map(subject->subject.associateWith(runnable)).orElse(runnable);
    }

    @Override
    public <T> Callable<T> associateWithSubjectIfAny(Callable<T> callable) {
        return getSubjectOrNull().map(subject->subject.associateWith(callable)).orElse(callable);
    }
}
