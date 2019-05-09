package com.sap.sse.util;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

import com.sap.sse.util.impl.ThreadPoolUtilImpl;

public interface ThreadPoolUtil {
    ThreadPoolUtil INSTANCE = new ThreadPoolUtilImpl();

    /**
     * Returns a central, fixed-size default thread pool executor that can be used to schedule background tasks. Using
     * this centralized copy ensures that when several application modules require such an executor they get a single
     * one whose dimensions are chosen such that it does not outperform other system-critical tasks such as garbage
     * collection and JITting.
     * <p>
     * 
     * The executor will have slightly less than {@link Thread#NORM_PRIORITY} as its priority, and its threads will all
     * be {@link Thread#setDaemon daemon threads}. The task queue is limited by {@link Integer#MAX_VALUE}, so is
     * virtually unbounded, and therefore no specific rejection handler is configured.
     * <p>
     * 
     * The thread pool uses constant size, so the core size equals the maximum size, and threads will not be terminated
     * once created. Its size is calculated using {@link #getReasonableThreadPoolSize()}.
     */
    ScheduledExecutorService getDefaultBackgroundTaskThreadPoolExecutor();
    
    /**
     * Returns a central, fixed-size default thread pool executor that can be used to schedule foreground tasks. Using
     * this centralized copy ensures that when several application modules require such an executor they get a single
     * one whose dimensions are chosen such that it does not outperform other system-critical tasks such as garbage
     * collection and JITting.
     * <p>
     * 
     * The executor will have {@link Thread#NORM_PRIORITY} as its priority, therefore slightly superseding the priority
     * of {@link #getDefaultBackgroundTaskThreadPoolExecutor()}, and its threads will all be {@link Thread#setDaemon
     * daemon threads}. The task queue is limited by {@link Integer#MAX_VALUE}, so is virtually unbounded, and therefore
     * no specific rejection handler is configured.
     * <p>
     * 
     * The thread pool uses constant size, so the core size equals the maximum size, and threads will not be terminated
     * once created. Its size is calculated using {@link #getReasonableThreadPoolSize()}.
     */
    ScheduledExecutorService getDefaultForegroundTaskThreadPoolExecutor();
    
    /**
     * Returns a new fixed-size thread pool executor that can be used to schedule background tasks. Normally, clients
     * should use {@link #getDefaultBackgroundTaskThreadPoolExecutor()} instead. However, under certain rare
     * circumstances it may be necessary or advisable to create a separate thread pool, e.g., when the computations of
     * tasks submitted to the default thread pool depend on the results of other tasks. Those other tasks then must not
     * be submitted to the same executor as deadlocks may occur.
     * <p>
     * 
     * The same configuration as for {@link #getDefaultBackgroundTaskThreadPoolExecutor()} is used.
     */
    ScheduledExecutorService createBackgroundTaskThreadPoolExecutor(String name);
    
    ScheduledExecutorService createBackgroundTaskThreadPoolExecutor(int size, String name);

    /**
     * In case an application module really requires its own thread pool instead of
     * using {@link #getBackgroundTaskThreadPool}, it should size the thread pool
     * using the value returned by this method. The number returned will scale with
     * the number of {@link Runtime#availableProcessors() CPUs available} and will
     * try to leave some room for system threads such as the garbage collector and
     * JITter.
     */
    int getReasonableThreadPoolSize();

    /**
     * Returns a new fixed-size thread pool executor that can be used to schedule foreground tasks for which quick
     * response times are expected. Normally, clients should use {@link #getDefaultForegroundTaskThreadPoolExecutor()}
     * instead. However, under certain rare circumstances it may be necessary or advisable to create a separate thread
     * pool, e.g., when the computations of tasks submitted to the default thread pool depend on the results of other
     * tasks. Those other tasks then must not be submitted to the same executor as deadlocks may occur.
     * <p>
     * 
     * The same configuration as for {@link #getDefaultForegroundTaskThreadPoolExecutor()} is used.
     */
    ScheduledExecutorService createForegroundTaskThreadPoolExecutor(String name);

    ScheduledExecutorService createForegroundTaskThreadPoolExecutor(int size, String name);
    
    /**
     * Logs all exceptions that occurred during the execution of the {@code futures} provided.
     * 
     * @param messageTemplate
     *            a message string that must contain a single {@code %s} parameter placeholder that will be substituted
     *            by the exception's message.
     */
    void logExceptionsFromFutures(Level logLevel, String messageTemplate, Iterable<? extends Future<?>> futures);
    
    /**
     * Uses the {@code executor}'s {@link ExecutorService#invokeAll(java.util.Collection) invokeAll} method to execute
     * all {@code tasks}. All exceptions that occur are logged, using the {@code logLevel} provided, using as the log
     * message the message template, parameterized with the exception message.
     * 
     * @param messageTemplate
     *            a message string that must contain a single {@code %s} parameter placeholder that will be substituted
     *            by the exception's message.
     */
    <T> List<Future<T>> invokeAllAndLogExceptions(ExecutorService executor, Level logLevel, String messageTemplate, Iterable<? extends Callable<T>> tasks);
}
