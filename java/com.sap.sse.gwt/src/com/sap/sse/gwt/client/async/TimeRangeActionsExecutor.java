package com.sap.sse.gwt.client.async;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util.Pair;

/**
 * An Executor for efficient, asynchronous execution of {@link TimeRangeAsyncAction}s. This executor operates on
 * {@link TimeRangeAsyncAction}s which specify the wanted {@link TimeRange} by {@link Key} with
 * {@link TimeRangeAsyncAction#getTimeRanges()} and are expected to return a {@code Result} when their
 * {@link TimeRangeAsyncAction#execute(Map, AsyncCallback)} is invoked. The {@link Key} could, e.g., be a
 * competitor or a boat class.
 * <p>
 * 
 * This executor manages and optimizes concurrent requests for equal {@code Key}s that have overlapping time ranges.
 * This way, redundant requests for the same data can thus be avoided. This executor collects the results of the overlapping
 * concurrent requests and uses them to satisfy each individual client request as if they had all been executed in their
 * entirety.
 * <p>
 * 
 * Each of the {@link TimeRange}s is trimmed against the {@link TimeRangeResultCache cache} of returned and outstanding
 * requests for the respective {@code Key}. After having received the server responses for all time ranges required to
 * satisfy a {@link TimeRangeAsyncAction} the trimmed results are combined and the {@link TimeRangeAsyncCallback
 * callback} is invoked.
 * <p>
 * 
 * @param <Result>
 *            Type returned by the combined call. See {@link TimeRangeAsyncAction}. Holds a compound result for several
 *            keys, such as for several competitors or several boats. A {@link TimeRangeAsyncCallback} object is
 *            expected to be able to {@link TimeRangeAsyncCallback#unzipResult(Object) split} such a {@code Result}
 *            object into the {@code Key}s and {@code SubResult}s, as well as to
 *            {@link TimeRangeAsyncCallback#joinSubResults(TimeRange, List) join} these sub-results of the individual
 *            results for trimmed requests into a sub-result for the full time range requested for one {@code Key}.
 * @param <SubResult>
 *            Type representing an individual part or channel of a compound {@link Result}, specific to one {@code Key},
 *            as obtained by {@link TimeRangeAsyncCallback#unzipResult(Object) splitting} a compound {@code Result}.
 * @param <Key>
 *            Type used to index {@link SubResult}s.
 * @see TimeRangeAsyncAction
 * @see TimeRangeAsyncCallback
 *
 * @author Tim Hessenmüller (D062243)
 */
public class TimeRangeActionsExecutor<Result, SubResult, Key> extends AbstractActionsExecutor {
    /**
     * Callback called by {@link TimeRangeActionsExecutor#executor} upon receiving an answer from the server for a
     * potentially trimmed request. The compound {@code Result} is {@link TimeRangeAsyncCallback#unzipResult(Object)
     * split} into the {@code SubResult}s per {@code Key} and disseminated to the respective
     * {@link TimeRangeResultCache} objects.
     */
    private final class ExecutorCallback implements AsyncCallback<Result> {
        private final TimeRangeAsyncAction<Result, Key> action;
        private final TimeRangeAsyncCallback<Result, SubResult, Key> callback;
        private boolean callbackWasCalled = false;
        private final Map<Key, TimeRange> requestedTimeRangeMap;
        private final Set<Key> subResultsReadySet;

        private ExecutorCallback(TimeRangeAsyncAction<Result, Key> action,
                Map<Key, TimeRange> requestedTimeRanges,
                TimeRangeAsyncCallback<Result, SubResult, Key> callback) {
            this.action = action;
            this.callback = callback;
            this.requestedTimeRangeMap = new HashMap<>(requestedTimeRanges);
            this.subResultsReadySet = new HashSet<>(requestedTimeRanges.size());
        }

        @Override
        public void onSuccess(Result result) {
            // from the compound result extract the potentially trimmed sub-results per key
            final Map<Key, SubResult> unzippedResultMap = result != null ? callback.unzipResult(result) : Collections.emptyMap();
            for (Key key : requestedTimeRangeMap.keySet()) {
                final SubResult trimmedSubResultForKey = unzippedResultMap.get(key);
                final TimeRangeResultCache<SubResult> cache = getSubResultCache(key);
                cache.registerResult(action, trimmedSubResultForKey);
            }
            numberOfPendingActions--;
            notifyListeners();
        }

        @Override
        public void onFailure(Throwable caught) {
            requestedTimeRangeMap.keySet().forEach(key -> getSubResultCache(key).registerFailure(action, caught));
            if (!callbackWasCalled) {
                callback.onFailure(caught);
                callbackWasCalled = true;
            }
            numberOfPendingActions--;
            notifyListeners();
        }

        /**
         * Called by a corresponding {@link TimeRangeResultCache#Request} once it has collected all needed
         * {@link SubResult}s. If this was the last key of all keys requested for which the result was delivered, the
         * potentially several sub-results per each key requested are retrieved from the cache and are
         * {@link TimeRangeAsyncCallback#joinSubResults(TimeRange, List) joined} into a single {@code SubResult} per key
         * (joining the time ranges of the results of potentially several trimmed requests) which are then
         * {@link TimeRangeAsyncCallback#zipSubResults(Map) combined} into a single compound {@code Result} object.
         * <p>
         * 
         * This {@code Result} object is then sent to the {@link #callback}'s
         * {@link TimeRangeAsyncCallback#onSuccess(Object)} method.
         *
         * @param key
         *            {@link Key} of the {@link TimeRangeResultCache#Request}
         */
        public void onSubResultSuccess(Key key) {
            if (!callbackWasCalled) {
                subResultsReadySet.add(key);
                if (subResultsReadySet.size() == requestedTimeRangeMap.size()) {
                    final Map<Key, SubResult> unzippedResult = new HashMap<>(subResultsReadySet.size());
                    for (Key k : subResultsReadySet) {
                        final TimeRange requestedTimeRange = requestedTimeRangeMap.get(k);
                        final List<Pair<TimeRange, SubResult>> subResults = getSubResultCache(k).getResults(action);
                        unzippedResult.put(k, callback.joinSubResults(requestedTimeRange, subResults));
                    }
                    callback.onSuccess(callback.zipSubResults(unzippedResult));
                    callbackWasCalled = true;
                }
            }
        }

        /**
         * Called by a corresponding {@link TimeRangeResultCache#Request} once an error occurs.
         *
         * @param caught
         *            {@link Throwable} that occurred
         */
        public void onSubResultFailure(Key key, Throwable caught) {
            if (!callbackWasCalled) {
                for (Key k : requestedTimeRangeMap.keySet()) {
                    if (!key.equals(k)) {
                        getSubResultCache(k).removeRequest(action);
                    }
                }
                callback.onFailure(caught);
                callbackWasCalled = true;
            }
        }
    }

    private final Map<Key, TimeRangeResultCache<SubResult>> cacheMap = new HashMap<>();
    
    /**
     * Executes a {@link TimeRangeAsyncAction} and returns the results to a {@link TimeRangeAsyncCallback}. Calls
     * {@link #execute(TimeRangeAsyncAction, AsyncCallback, boolean)} with {@code forceTimeRange} set to {@code false},
     * thus allowing for cached requests to help trim the new request so as to avoid overlapping redundant requests.
     *
     * @param action
     *            {@link TimeRangeAsyncAction} to execute.
     * @param callback
     *            {@link TimeRangeAsyncCallback} to return results to.
     * @see #execute(TimeRangeAsyncAction, TimeRangeAsyncCallback, boolean)
     */
    public void execute(TimeRangeAsyncAction<Result, Key> action,
            TimeRangeAsyncCallback<Result, SubResult, Key> callback) {
        execute(action, callback, /* forceTimeRange */ false);
    }

    /**
     * Executes a {@link TimeRangeAsyncAction} and returns the results to a {@link TimeRangeAsyncCallback}.
     *
     * @param action
     *            {@link TimeRangeAsyncAction} to execute.
     * @param callback
     *            {@link TimeRangeAsyncCallback} to return results to.
     * @param forceTimeRange
     *            if {@code false} the request will be optimized by trimming its time range; otherwise it will be
     *            executed unchanged
     * @see #execute(TimeRangeAsyncAction, TimeRangeAsyncCallback)
     */
    public void execute(TimeRangeAsyncAction<Result, Key> action,
            TimeRangeAsyncCallback<Result, SubResult, Key> callback, boolean forceTimeRange) {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null!");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null!");
        }
        final Map<Key, TimeRange> requestedTimeRanges = action.getTimeRanges();
        final ExecutorCallback execCallback = new ExecutorCallback(action, requestedTimeRanges, callback);
        final Map<Key, TimeRange> trimmedTimeRanges = new HashMap<>(requestedTimeRanges.size());
        for (final Map.Entry<Key, TimeRange> subRequest : requestedTimeRanges.entrySet()) {
            final TimeRangeResultCache<SubResult> cache = getSubResultCache(subRequest.getKey());
            final TimeRange potentiallyTrimmedTimeRange = cache.trimAndRegisterRequest(subRequest.getValue(), forceTimeRange, action,
                    new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            execCallback.onSubResultFailure(subRequest.getKey(), caught);
                        }
                        @Override
                        public void onSuccess(Void result) {
                            execCallback.onSubResultSuccess(subRequest.getKey());
                        }
            });
            if (potentiallyTrimmedTimeRange != null) {
                trimmedTimeRanges.put(subRequest.getKey(), potentiallyTrimmedTimeRange);
            }
        }
        numberOfPendingActions++;
        notifyListeners();
        action.execute(trimmedTimeRanges, execCallback);
    }

    private TimeRangeResultCache<SubResult> getSubResultCache(Key key) {
        return cacheMap.computeIfAbsent(key, k->new TimeRangeResultCache<>());
    }
}
