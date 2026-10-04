package com.sap.sse.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.shiro.subject.Subject;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.util.SmartFutureCache.UpdateInterval;
import com.sap.sse.util.impl.HasTracingGet;
import com.sap.sse.util.impl.HasTracingGetImpl;
import com.sap.sse.util.impl.KnowsExecutor;
import com.sap.sse.util.impl.KnowsExecutorAndTracingGet;
import com.sap.sse.util.impl.KnowsExecutorAndTracingGetImpl;

/**
 * A cache for which a background update can be triggered. Readers can decide whether they want to wait for any ongoing
 * background update or read the latest cached value for a key. An update trigger can provide an optional parameter for
 * the update which may, e.g., control the interval of the cached value to update. When an update is triggered and
 * another update is already running, the update is queued. If there already is an update queued for the same key, the
 * optional update parameters are {@link UpdateInterval#join(UpdateInterval) "joined"} (for example, the two update
 * intervals are joined to form one interval which incorporates both original update intervals).
 * <p>
 * 
 * A {@link CacheUpdater} needs to be passed to the constructor which carries out the actual calculation whose values
 * are to be cached. The {@link CacheUpdater} interface assumes that a cache update may be computed in two steps: first,
 * a value is computed for a key and an update interval which may be computationally expensive. Then, in a second step,
 * the new value is combined with the previous cache value for the same key and update interval. The default
 * implementation of {@link CacheUpdater#provideNewCacheValue(Object, Object, Object, UpdateInterval)} simply returns
 * the <code>computedCacheUpdate</code> parameter which is the result computed by
 * {@link CacheUpdater#computeCacheUpdate(Object, UpdateInterval)} before.
 * <p>
 * 
 * The cache only knows about results computed based on a {@link #triggerUpdate(Object, UpdateInterval)} call. The
 * {@link #get(Object, boolean)} method itself will not trigger a computation if no cache value exists for the request.
 * Therefore, the {@link #triggerUpdate(Object, UpdateInterval)} calls need to ensure that all data expected to be
 * managed by this cache---specifically the area spanned by the update interval---is covered.
 * <p>
 * 
 * There may be situations, such as during a start-up phase, where the automatic and immediate re-calculation is not
 * desirable, particularly because during such a phase the number of re-calculations scheduled perhaps by far outweighs
 * the number of {@link #get(Object, boolean)} requests. In such a phase it is smarter to suspend the automatic
 * re-calculation and defer it until a {@link #get(Object, boolean)} request actually happens. For this purpose,
 * the {@link #suspend} and {@link #resume} methods can be used. No matter the suspend/resume state, the {@link #get(Object, boolean)}
 * method will always respond in line with the {@link #triggerUpdate(Object, UpdateInterval)} calls, only that re-calculations
 * are not immediately started when in suspended mode, and {@link #get(Object, boolean) get(key, false)} will not trigger a
 * re-calculation at all. When resuming, any pending recalculations triggered so far are scheduled for immediate execution such
 * that subsequent {@link #get(Object, boolean) get(key, true)} calls will wait for their completion.
 * 
 * @param <K>
 *            the key type for which values of type <code>V</code> are cached
 * @param <V>
 *            the value type of which instances are cached for particular keys of type <code>K</code>
 * @param <U>
 *            a parameter type for the cache update method for a single key, such that the parameters of multiple queued
 *            requests for the same key can be joined into one for a faster update
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class SmartFutureCache<K, V, U extends UpdateInterval<U>> {
    private static final Logger logger = Logger.getLogger(SmartFutureCache.class.getName());
    
    /**
     * Holds the tasks that have been added to an {@link Executor} already for execution and that, as long as a client
     * holds the object monitor / lock on this map, aren't cancelled. Note, however, that once the lock is released,
     * the Futures may be cancelled in case a cache replacement has taken place. Clients can prevent this by calling
     * {@link FutureTaskWithCancelBlocking#dontCancel()} on the future while holding the lock on
     * {@link #ongoingManeuverCacheRecalculations}. This will let {@link Future#cancel(boolean)} return <code>false</code>
     * should it be called on that Future.
     */
    private final ConcurrentMap<K, FutureTaskWithCancelBlocking> ongoingRecalculations;
    
    /**
     * The results cached by this {@link SmartFutureCache}. While a concurrent map, most access is guarded by
     * {@link #locksForKeys}.
     */
    private final ConcurrentMap<K, V> cache;
    
    /**
     * Caches exceptions that occurred while computing an update for the respective key. If an exception is stored
     * in here for a key, this takes precedence over any value in {@link #cache} for that same key, although ideally
     * when putting an exception into this map, that key should be removed from {@link #cache} for consistency.<p>
     * 
     * Access synchronization/locking follows exactly the same principle as for {@link #cache}.
     */
    private final ConcurrentMap<K, Throwable> cachedExceptions;
    
    /**
     * Note that this needs to have more than one thread because there may be calculations used for cache updates that
     * need to wait for other cache updates to finish. If those were all to be handled by a single thread, deadlocks
     * would occur. Remember that there may still be single-core machines, so the factor with which
     * <code>availableProcessors</code> is multiplied needs to be greater than one at least.
     */
    private final static ScheduledExecutorService recalculator = ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor();

    private final CacheUpdater<K, V, U> cacheUpdateComputer;
    
    private final ConcurrentMap<K, NamedReentrantReadWriteLock> locksForKeys;
    
    private final String nameForLocks;
    
    /**
     * See {@link #suspend} and {@link #resume}.
     */
    private boolean suspended;
    
    private final Map<K, Pair<U, Set<SettableFuture<Future<V>>>>> triggeredAndNotYetScheduled;
    
    private int smartFutureCacheTaskReuseCounter;
    
    /**
     * An immutable "interval" description for a cache update
     * 
     * @author Axel Uhl (D043530)
     *
     */
    public static interface UpdateInterval<U extends UpdateInterval<U>> {
        /**
         * Produces a new immutable update interval that "contains" both, this and <code>otherUpdateInterval</code> according
         * to the semantics of the specific implementation.
         */
        U join(U otherUpdateInterval);
    }
    
    public static class EmptyUpdateInterval implements UpdateInterval<EmptyUpdateInterval> {
        @Override
        public EmptyUpdateInterval join(EmptyUpdateInterval otherUpdateInterval) {
            return null;
        }
    }
    
    /**
     * For a key and an optional update interval can compute a new value to be stored in the cache.
     * @author Axel Uhl (D043530)
     *
     * @param <K> the cache's key type
     * @param <V> the cache's value type
     * @param <U> the update interval type
     */
    public static interface CacheUpdater<K, V, U extends UpdateInterval<U>> {
        /**
         * Called by a background task to perform the potentially expensive update computations. The cache is not
         * updated with the results immediately. Instead, the result of this operation is later passed to
         * {@link #provideNewCacheValue(Object, Object)} with the cache entry for <code>key</code> locked for writing.
         * 
         * @param updateInterval
         *            if <code>null</code>, the result must reflect the entire current data on which the cache is based,
         *            like the "infinite" interval
         */
        V computeCacheUpdate(K key, U updateInterval) throws Exception;
        
        /**
         * Expected to deliver an updated cache value quickly (compared to the potentially much more expensive
         * {@link #computeCacheUpdate(Object, UpdateInterval)} method which is run in a background task and doesn't lock
         * the cache for readers).
         * 
         * @param key
         *            the key for which to deliver the cache update
         * @param oldCacheValue
         *            the value associated with <code>key</code> up to now; may be <code>null</code>
         * @param computedCacheUpdate
         *            the result of {@link #computeCacheUpdate(Object, UpdateInterval)} called for <code>key</code>. A
         *            trivial implementation may simply return <code>computeCacheUpdate</code> if no further changes are
         *            required. However, an implementation may take the opportunity to update the result of
         *            {@link SmartFutureCache#get(Object, boolean)} called with <code>key</code> and <code>false</code>
         *            to obtain the current cache value for <code>key</code> and incrementally update it with
         *            <code>computedCacheUpdate</code> instead of constructing a new cache value which again may be
         *            fairly expensive.
         */
        V provideNewCacheValue(K key, V oldCacheValue, V computedCacheUpdate, U updateInterval);
    }

    public static abstract class AbstractCacheUpdater<K, V, U extends UpdateInterval<U>> implements CacheUpdater<K, V, U> {
        @Override
        public V provideNewCacheValue(K key, V oldCacheValue, V computedCacheUpdate, U updateInterval) {
            return computedCacheUpdate;
        }
    }
    
    private static class SettableCallable<V> implements Callable<V> {
        private Callable<V> callable;
        
        public void setCallable(Callable<V> callable) {
            this.callable = callable;
        }
        
        @Override
        public V call() throws Exception {
            return callable.call();
        }
        
        @Override
        public String toString() {
            return callable==null?"null":callable.toString();
        }
    }
    
    /**
     * Once a client has fetched such a Future from {@link SmartFutureCache#ongoingRecalculations} while
     * holding the object monitor of {@link SmartFutureCache#ongoingRecalculations}, the client knows that
     * the Future hasn't been cancelled yet. To avoid that the Future is cancelled after the client has fetched it from
     * {@link SmartFutureCache#ongoingRecalculations}, the client can call {@link #dontCancel()} on this
     * future. After that, calls to {@link #cancel(boolean)} will return <code>false</code> immediately and the Future
     * will be executed as originally scheduled.<p>
     * 
     * A task of this sort will always {@link Subject#associateWith(Runnable) associate} any current {@link Subject}
     * to the task when it is executed.
     * 
     * @author Axel Uhl (D043530)
     * 
     */
    private class FutureTaskWithCancelBlocking extends FutureTask<V> implements KnowsExecutor, Callable<V> {
        private final KnowsExecutorAndTracingGet<V> tracingGetHelper;
        
        private boolean runningAndReadUpdateInterval;
        
        private final K key;

        private U updateInterval;

        private final boolean callerWaitsSynchronouslyForResult;

        private final Thread callerThread;

        private Thread executingThread;

        /**
         * When outside <code>synchronized(this)</code> and {@link #call()} hasn't returned, holds the set of threads
         * currently waiting in {@link #get()}.
         */
        private Set<Thread> gettingThreads;

        public FutureTaskWithCancelBlocking(final K key, final U updateInterval,
                final boolean callerWaitsSynchronouslyForResult, final Thread callerThread) {
            this(new SettableCallable<V>(), key, updateInterval, callerWaitsSynchronouslyForResult, callerThread);
        }
        
        public FutureTaskWithCancelBlocking(SettableCallable<V> callable, final K key, final U updateInterval,
                final boolean callerWaitsSynchronouslyForResult, final Thread callerThread) {
            super(ThreadPoolUtil.INSTANCE.associateWithSubjectIfAny(callable));
            tracingGetHelper = new KnowsExecutorAndTracingGetImpl<>();
            callable.setCallable(this);
            this.gettingThreads = new HashSet<>();
            this.key = key;
            this.updateInterval = updateInterval;
            this.callerWaitsSynchronouslyForResult = callerWaitsSynchronouslyForResult;
            this.callerThread = callerThread;
        }

        /**
         * Tries to update this task's update interval so as to re-used an already scheduled task for a given key.
         * The update can only be performed if the task hasn't read the update interval after it has been started.
         * 
         * @return <code>true</code> if the update was applied successfully before the task read the update interval; this means
         * that if the task is not cancelled, it will use the new update interval. If <code>false</code> is returned, the task has already
         * read the update interval, and a change to the update interval for this task is no longer possible. The caller will probably
         * have to schedule a new task with the new update interval.
         */
        public synchronized boolean tryToUpdateUpdateInterval(U newUpdateInterval) {
            final boolean result;
            if (runningAndReadUpdateInterval) {
                result = false;
            } else {
                result = true;
                updateInterval = newUpdateInterval;
            }
            return result;
        }
        
        @Override
        public V get() throws InterruptedException, ExecutionException {
            // propagate locks to executing thread if it has already entered call() because we'll wait for it; only propagate if the executing thread hasn't
            // done this by itself because of synchronous execution. Unpropagate needs to happen in the executing thread
            // because there is no synchronization around the return of the call() method and the continuing of this get() method.
            synchronized (this) {
                final Thread propagatedToExecutingThread = executingThread;
                final boolean callHasTerminatedWhenGetWasCalled = runningAndReadUpdateInterval && propagatedToExecutingThread == null;
                if (!callHasTerminatedWhenGetWasCalled) {
                    gettingThreads.add(Thread.currentThread()); // call() will remove the gettingThreads before it returns and deal with lock unpropagation
                    if (propagatedToExecutingThread != null) {
                        // Turn this logging on in case you need to debug the SmartFutureCache. Otherwise, log level detection is too expensive here.
                        // logger.finest("propagating lock set from " + Thread.currentThread().getName() + " to "
                        //        + propagatedToExecutingThread.getName());
                        LockUtil.propagateLockSetTo(propagatedToExecutingThread);
                    }
                }
            }
            return tracingGetHelper.callGetAndTraceAfterEachTimeout(this);
        }

        @Override
        public V call() {
            final U updateInterval;
            final Set<Thread> locksPropagatedFromGettingThreads;
            synchronized (this) {
                updateInterval = getUpdateInterval();
                locksPropagatedFromGettingThreads = new HashSet<Thread>(gettingThreads);
                executingThread = Thread.currentThread();
                if (!locksPropagatedFromGettingThreads.isEmpty()) {
                    // get() was called and cannot have returned yet because call() hasn't returned; propagate locks from getting thread
                    for (Thread locksPropagatedFromGettingThread : locksPropagatedFromGettingThreads) {
                        LockUtil.propagateLockSetFrom(locksPropagatedFromGettingThread);
                        // Turn this logging on in case you need to debug the SmartFutureCache. Otherwise, log level detection is too expensive here.
                        // logger.finest("propagating lock set from " + locksPropagatedFromGettingThread.getName()
                        //        + " to " + executingThread.getName());
                    }
                }
                // make sure we don't propagate from the same thread twice in case gettingThread == callerThread
                if (callerWaitsSynchronouslyForResult && !locksPropagatedFromGettingThreads.contains(callerThread)) {
                    // Turn this logging on in case you need to debug the SmartFutureCache. Otherwise, log level detection is too expensive here.
                    // logger.finest("propagating lock set from "+callerThread.getName()+" to "+executingThread.getName()+
                    //        " due to synchronous execution");
                    LockUtil.propagateLockSetFrom(callerThread);
                }
                runningAndReadUpdateInterval = true;
            }
            try {
                try {
                    V preResult = cacheUpdateComputer.computeCacheUpdate(key, updateInterval);
                    final NamedReentrantReadWriteLock lock = getOrCreateLockForKey(key);
                    LockUtil.lockForWrite(lock);
                    try {
                        V result = cacheUpdateComputer.provideNewCacheValue(key, cache.get(key), preResult,
                                updateInterval);
                        cache(key, result);
                        return result;
                    } finally {
                        LockUtil.unlockAfterWrite(lock);
                    }
                } catch (Exception e) { // important to update cachedExceptions before removing key from ongoingRecalculations,
                    // so that get(K, boolean) always either still finds the FutureTask which then throws an exception upon get(),
                    // or finds the exception that was thrown in cachedExceptions.
                    final NamedReentrantReadWriteLock lock = getOrCreateLockForKey(key);
                    LockUtil.lockForWrite(lock);
                    try {
                        remove(key);
                        cachedExceptions.put(key, e);
                    } finally {
                        LockUtil.unlockAfterWrite(lock);
                    }
                    logger.log(Level.SEVERE, "SmartFutureCache.FutureTaskWithCancelBlocking.call", e);
                    throw new RuntimeException(e);
                } finally {
                    synchronized (ongoingRecalculations) {
                        boolean newTaskScheduled = false;
                        Pair<U, Set<SettableFuture<Future<V>>>> queued = triggeredAndNotYetScheduled.get(key);
                        if (!suspended || (queued != null && !queued.getB().isEmpty())) {
                            if (queued != null) {
                                triggeredAndNotYetScheduled.remove(key);
                                Future<V> future = schedule(key, queued.getA(), /* callerWaitsSynchronouslyForResult */ false);
                                newTaskScheduled = true;
                                for (SettableFuture<Future<V>> futureToSet : queued.getB()) {
                                    futureToSet.set(future);
                                }
                            }
                        }
                        if (!newTaskScheduled) {
                            ongoingRecalculations.remove(key);
                        }
                    }
                }
            } finally {
                synchronized (this) {
                    for (Thread locksPropagatedFromGettingThread : gettingThreads) {
                        // Turn this logging on in case you need to debug the SmartFutureCache. Otherwise, log level detection is too expensive here.
                        // logger.finest("unpropagating lock set from "+locksPropagatedFromGettingThread.getName()+" to "+executingThread.getName());
                        LockUtil.unpropagateLockSetFrom(locksPropagatedFromGettingThread);
                    }
                    executingThread = null;
                }
                if (callerWaitsSynchronouslyForResult && !gettingThreads.contains(callerThread)) {
                    // Turn this logging on in case you need to debug the SmartFutureCache. Otherwise, log level detection is too expensive here.
                    // logger.finest("unpropagating lock set from "+callerThread.getName()+" to "+Thread.currentThread().getName()+
                    //        " due to synchronous execution");
                    LockUtil.unpropagateLockSetFrom(callerThread);
                }
                gettingThreads.clear();
            }
        }

        public U getUpdateInterval() {
            return updateInterval;
        }

        @Override
        public void setExecutorThisTaskIsScheduledFor(ThreadPoolExecutor executorThisTaskIsScheduledFor) {
            tracingGetHelper.setExecutorThisTaskIsScheduledFor(executorThisTaskIsScheduledFor);
        }
        
        @Override
        public String toString() {
            return getClass().getName()+" [key="+key+", updateInterval="+updateInterval+", cacheUpdateComputer="+cacheUpdateComputer+"]";
        }
    }
    
    public SmartFutureCache(CacheUpdater<K, V, U> cacheUpdateComputer, String nameForLocks) {
        this.ongoingRecalculations = new ConcurrentHashMap<K, FutureTaskWithCancelBlocking>();
        this.cache = new ConcurrentHashMap<>();
        this.cachedExceptions = new ConcurrentHashMap<>();
        this.cacheUpdateComputer = cacheUpdateComputer;
        this.locksForKeys = new ConcurrentHashMap<K, NamedReentrantReadWriteLock>();
        this.nameForLocks = nameForLocks;
        this.triggeredAndNotYetScheduled = new HashMap<>();
    }
    
    public Optional<Integer> getTaskQueueSize() {
        return ThreadPoolUtil.INSTANCE.getQueueLength(recalculator);
    }
    
    private NamedReentrantReadWriteLock getOrCreateLockForKey(K key) {
        synchronized (locksForKeys) {
            NamedReentrantReadWriteLock result = locksForKeys.get(key);
            if (result == null) {
                result = new NamedReentrantReadWriteLock(nameForLocks+" for key "+key, /* fair */ false);
                locksForKeys.put(key, result);
            }
            return result;
        }
    }
    
    public void suspend() {
        logger.finest("suspending cache "+nameForLocks);
        synchronized (ongoingRecalculations) {
            suspended = true;
        }
    }
    
    public void resume() {
        synchronized (ongoingRecalculations) {
            suspended = false;
            logger.finest("resuming cache "+nameForLocks);
            for (Iterator<Entry<K, Pair<U, Set<SettableFuture<Future<V>>>>>> i=triggeredAndNotYetScheduled.entrySet().iterator(); i.hasNext(); ) {
                Entry<K, Pair<U, Set<SettableFuture<Future<V>>>>> e = i.next();
                logger.finest(()->"while resuming "+nameForLocks+", triggering update for key "+e.getKey()+" with update interval "+e.getValue());
                triggerUpdate(e.getKey(), e.getValue().getA());
                i.remove();
            }
        }
    }
    
    private void queue(final K key, final U updateInterval, SettableFuture<Future<V>> setWhenDone) {
        Pair<U, Set<SettableFuture<Future<V>>>> oldUpdateInterval = triggeredAndNotYetScheduled.get(key);
        U joinedUpdateInterval = joinUpdateIntervals(updateInterval, oldUpdateInterval==null?null:oldUpdateInterval.getA());
        Set<SettableFuture<Future<V>>> newSetWhenDone = new HashSet<>();
        if (oldUpdateInterval != null) {
            newSetWhenDone.addAll(oldUpdateInterval.getB());
        }
        if (setWhenDone != null) {
            newSetWhenDone.add(setWhenDone);
        }
        triggeredAndNotYetScheduled.put(key, new Pair<>(joinedUpdateInterval, newSetWhenDone));
    }
    
    /**
     * Triggers a cache update for <code>key</code> for the <code>updateInterval</code> specified. If a re-calculation
     * for this key is already scheduled, this method will try to update its update interval by
     * {@link UpdateInterval#join(UpdateInterval) joining} the new with the existing one. If that doesn't work because
     * the task has already been started and has already read its update interval, this method will schedule a new task
     * with the extended update interval.
     * <p>
     * 
     * If the running task has a different setting for the caller's waiting for the task, the task will be canceled
     * (which may or may not work), and a new task with the joined update interval is scheduled.
     */
    public void triggerUpdate(final K key, U updateInterval) {
        // establish and maintain the following invariant: after lock on ongoingRecalculations is released,
        // no Future contained in it is in cancelled state
        synchronized (ongoingRecalculations) {
            if (suspended) {
                queue(key, updateInterval, /* future to set when done */ null);
            } else {
                scheduleRecalculationIfNotScheduledOrRunningOtherwiseUpdateScheduledTaskOrQueueIfAlreadyRunning(key, updateInterval,
                        /* callerWaitsSynchronouslyForResult */ false);
            }
        }
    }

    private U joinUpdateIntervals(U updateInterval, final U oldUpdateInterval) {
        final U joinedUpdateInterval;
        if (oldUpdateInterval == null) {
            joinedUpdateInterval = updateInterval;
        } else {
            if (updateInterval == null) {
                joinedUpdateInterval = oldUpdateInterval;
            } else {
                joinedUpdateInterval = updateInterval.join(oldUpdateInterval);
            }
        }
        return joinedUpdateInterval;
    }

    private static class SettableFuture<T> implements Future<T> {
        private T value;
        private boolean isSet;
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return isSet;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            synchronized (this) {
                while (!isSet) {
                    wait();
                }
            }
            return value;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            synchronized (this) {
                while (!isSet) {
                    wait(TimeUnit.MILLISECONDS.convert(timeout, unit));
                    if (!isSet) {
                        throw new TimeoutException();
                    }
                }
            }
            return value;
        }
        
        public void set(T value) {
            synchronized (this) {
                this.value = value;
                isSet = true;
                notifyAll();
            }
        }
        
        public String toString() {
            final String result;
            synchronized (this) {
                if (isSet) {
                    result = value==null?"null":value.toString();
                } else {
                    result = "unset";
                }
            }
            return result;
        }
    }
    
    private static class TransitiveFuture<T> implements Future<T> {
        private final SettableFuture<Future<T>> future;
        private final HasTracingGet<T> tracingGetHelper;
        
        protected TransitiveFuture(SettableFuture<Future<T>> future) {
            super();
            this.future = future;
            this.tracingGetHelper = new HasTracingGetImpl<T>() {
                @Override
                protected String getAdditionalTraceInfo() {
                    return "transitive future "+TransitiveFuture.this.future.toString();
                }
            };
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            try {
                return future.get().cancel(mayInterruptIfRunning);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isCancelled() {
            try {
                return future.get().isCancelled();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public boolean isDone() {
            try {
                return future.get().isDone();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return tracingGetHelper.callGetAndTraceAfterEachTimeout(this);
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit).get(timeout, unit);
        }
    }

    /**
     * Either calls {@link #schedule(Object, UpdateInterval, boolean)} to create and execute the
     * future or, if there is a currently executing future with this key, "remember" the call in the map
     * {@link #triggeredWhileExecutingForSameKey}. This map is then looked into in the finally block of
     * {@link FutureTaskWithCancelBlocking#call()}.
     * 
     * @param callerWaitsSynchronouslyForResult if <code>true</code>, if a future task needs to be created,
     * it will already inherit the calling thread's locks, and the method will always return a future task.
     */
    private Future<V> scheduleRecalculationIfNotScheduledOrRunningOtherwiseUpdateScheduledTaskOrQueueIfAlreadyRunning(final K key,
            final U updateInterval, final boolean callerWaitsSynchronouslyForResult) {
        final Future<V> result;
        synchronized (ongoingRecalculations) {
            if (ongoingRecalculations.containsKey(key)) {
                FutureTaskWithCancelBlocking scheduledOrRunning = ongoingRecalculations.get(key);
                // a future is already scheduled for the key; try to adjust the update interval
                boolean reuseExistingFuture = scheduledOrRunning.tryToUpdateUpdateInterval(updateInterval);
                if (reuseExistingFuture) {
                    result = scheduledOrRunning;
                    // Turn this logging on in case you need to debug the SmartFutureCache. Otherwise, log level detection is too expensive here.
                    // logger.finest("re-using existing future task on cache "+nameForLocks+" for key "+key);
                    smartFutureCacheTaskReuseCounter++;
                } else {
                    final SettableFuture<Future<V>> nestedFuture;
                    if (callerWaitsSynchronouslyForResult) {
                        nestedFuture = new SettableFuture<Future<V>>();
                        result = new TransitiveFuture<>(nestedFuture);
                    } else {
                        nestedFuture = null;
                        result = null;
                    }
                    queue(key, updateInterval, nestedFuture);
                }
            } else {
                result = schedule(key, updateInterval, callerWaitsSynchronouslyForResult);
            }
        }
        return result;
    }

    /**
     * Creates a {@link FutureTask} for the (re-)calculation of the cache entry for <code>key</code> across update
     * interval <code>joinedUpdateInterval</code>, enters it into {@link #ongoingRecalculations} and schedules its
     * execution with {@link #recalculator}. The method synchronizes on {@link #ongoingRecalculations}.
     * 
     * !!! Callers need to hold monitor of {@link #ongoingRecalculations}
     * 
     * @param callerWaitsSynchronouslyForResult
     *            if <code>true</code>, this allows the future to assume the caller's locks. See also
     *            {@link LockUtil#propagateLockSetFrom(Thread)}. This can be helpful to avoid read-read deadlocks
     *            in conjunction with fair {@link ReentrantReadWriteLock}s.
     */
    private Future<V> schedule(final K key, final U joinedUpdateInterval, final boolean callerWaitsSynchronouslyForResult) {
        final Thread callerThread = Thread.currentThread();
        final FutureTaskWithCancelBlocking future = new FutureTaskWithCancelBlocking(key, joinedUpdateInterval,
                callerWaitsSynchronouslyForResult, callerThread);
        // Turn this logging on in case you need to debug the SmartFutureCache. Otherwise, log level detection
        // is too expensive here.
        // logger.finest("creating future task on cache "+nameForLocks+" for key "+key);
        ongoingRecalculations.put(key, future);
        // the FutureTaskWithCancelBlocking associates any current Subject with the task while it is executed
        recalculator.execute(future);
        return future;
    }
    
    /**
     * Fetches a value for <code>key</code> from the cache. If no {@link #triggerUpdate(Object, UpdateInterval)} for the
     * <code>key</code> has ever happened, <code>null</code> will be returned. Otherwise, depending on
     * <code>waitForLatest</code> the result is taken from the cache straight away (<code>waitForLatest==false</code>)
     * or, if a re-calculation for the <code>key</code> is still ongoing, the result of that ongoing re-calculation is
     * returned. When {@link #remove(Object)} has been called for the {@code key} and no update has finished computing
     * since then, this method will also return {@code null} in case {@code waitForLatest} is {@code false}.
     * <p>
     * 
     * @param waitForLatest
     *            if {@code true}, the call may block, waiting for a scheduled or ongoing task to complete. Note: the
     *            tasks updating this cache are scheduled with the
     *            {@link ThreadPoolUtil#getDefaultBackgroundTaskThreadPoolExecutor() default background thread pool}
     *            which may contain many tasks during certain phases of the life cycle of the process; e.g., during
     *            start-up of the ARCHIVE server, several hundred thousand tasks may be enqueued with that background
     *            executor, so that it may take minutes or even hours until a task is actually picked up and run.
     *            Therefore, no foreground / HTTP request thread should ever use {@code true} for this parameter. It may
     *            otherwise block all of the HTTP request threads. See also bug 6223.
     * 
     * @throws a
     *             {@link RuntimeException} with the {@link RuntimeException#getCause() cause} set to the exception that
     *             occurred during the last {@link CacheUpdater#computeCacheUpdate(Object, UpdateInterval)} invocation,
     *             if any; this happens regardless of whether the calculation was still ongoing and waited for, or if
     *             not waiting for any current execution.
     */
    public V get(final K key, boolean waitForLatest) {
        final V value;
        final Future<V> future;
        if (waitForLatest) {
            synchronized (ongoingRecalculations) {
                final Pair<U, Set<SettableFuture<Future<V>>>> triggeredSinceLastCacheUpdate = triggeredAndNotYetScheduled.remove(key);
                if (triggeredSinceLastCacheUpdate != null) {
                    future = scheduleRecalculationIfNotScheduledOrRunningOtherwiseUpdateScheduledTaskOrQueueIfAlreadyRunning(key, triggeredSinceLastCacheUpdate.getA(),
                            /* callerWaitsSynchronouslyForResult: if a new task is scheduled, we'll wait for it by calling future.get() below */ true);
                } else {
                    future = ongoingRecalculations.get(key);
                }
            }
            if (future != null) {
                try {
                    value = future.get(); // will throw an ExecutionException if re-calculation threw an exception
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "get", e);
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    logger.log(Level.SEVERE, "get", e);
                    if (e.getCause() instanceof RuntimeException) {
                        throw (RuntimeException) e.getCause();
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                try {
                    value = readCache(key);
                } catch (RuntimeException e) {
                    logger.log(Level.SEVERE, "get", e);
                    throw e;
                }
            }
        } else {
            try {
                value = readCache(key);
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "get", e);
                throw e;
            }
        }
        return value;
    }

    private V readCache(final K key) {
        final V value;
        final NamedReentrantReadWriteLock lock = getOrCreateLockForKey(key);
        LockUtil.lockForRead(lock);
        try {
            final Throwable exception = cachedExceptions.get(key);
            if (exception != null) {
                throw new RuntimeException(exception);
            }
            value = cache.get(key);
        } finally {
            LockUtil.unlockAfterRead(lock);
        }
        return value;
    }

    public Set<K> keySet() {
        return cache.keySet();
    }

    protected void cache(final K key, V value) {
        cachedExceptions.remove(key);
        if (value == null) {
            cache.remove(key);
            locksForKeys.remove(key);
        } else {
            cache.put(key, value);
        }
    }

    /**
     * For debugging and testing; tells how many times a task got re-cycled with a new update interval instead of canceling it
     * and scheduling a new one. See also bug 1314.
     */
    public int getSmartFutureCacheTaskReuseCounter() {
        return smartFutureCacheTaskReuseCounter;
    }

    /**
     * Removes the key from the cache. If any updates are still running, they may again insert the key into the cache.
     * Until new updates for the {@code key} are computed, {@link #get(Object, boolean)} will return {@code null} for
     * the {@code key}.
     */
    public void remove(K key) {
        cache(key, null);
    }
}
