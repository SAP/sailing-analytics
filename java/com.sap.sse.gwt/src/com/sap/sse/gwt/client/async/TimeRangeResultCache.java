package com.sap.sse.gwt.client.async;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util.Pair;

/**
 * Optimizes requests by trimming their wanted {@link TimeRange} against a cache of (fetched and still outstanding)
 * results. In the context of a {@link TimeRangeActionsExecutor} such a cache is responsible for managing the requests
 * for a single {@code Key}, such as a competitor or a boat class.
 * <p>
 *
 * Before making a request for a time range, the executor has to call
 * {@link #trimAndRegisterRequest(TimeRange, boolean)}.
 *
 * @param <SubResult>
 *            type of the results of the remote calls which are to be cached
 * @see TimeRangeActionsExecutor
 * @author Tim Hessenmüller (D062243)
 */
public class TimeRangeResultCache<SubResult> {
    private static final int TRIM_MAX_ITERATIONS = 20;
    private static final int CACHE_SIZE = 32;

    /**
     * Stores properties of a request that may have been the result of trimming an original request's time ranges. If
     * trimming took place (see {@link TimeRangeResultCache#trimAndRegisterRequest(TimeRange, boolean)}), the trimmed
     * request will have {@link #getChildrenSet() dependencies} on other {@link Request}s whose results it needs to
     * combine with its own result in order to satisfy the original untrimmed request from which it was derived by
     * trimming.
     * <p>
     *
     * Dependent requests are represented as a <b>Petri Net</b>. {@link Request}s are vertices and {@link #childrenSet}
     * and {@link #parentSet} make up doubly-linked edges. {@link SubResult}s are tokens which get handled by a
     * transition function implemented in {@link #notifyActionSuccessIfHasAllResults()}.
     * <p>
     *
     * To keep things organized a "child" {@link Request} will never act on its own but always notify its "parent"
     * {@link Request}s to act on its behalf. For this reason most methods are written from the "parents" perspective.
     * <p>
     *
     * This request keeps track of whether it {@link #hasResult() has received a result}. It furthermore manages a
     * {@link #callback} that will be triggered <b>exactly once</b> when a result has been {@link #setResult(Pair)
     * received} for this request.
     */
    protected class Request {
        private final TimeRangeAsyncAction<?, ?> action;
        private final AsyncCallback<Void> callback;
        private boolean callbackWasCalled = false;
        private final TimeRange actionTimeRange;
        private TimeRange trimmedTimeRange;
        private final Set<Request> parentSet = new HashSet<>();
        private final Set<Request> childrenSet = new HashSet<>();
        private int childrenWithoutResultCounter = 0;
        private boolean hasResult = false;
        private SubResult result = null;
        private boolean responseReceived = false;
        private boolean actionResultReleased = false;

        public Request(TimeRange timeRange, TimeRange trimmedTimeRange, AsyncCallback<Void> callback,
                TimeRangeAsyncAction<?, ?> action, Iterable<TimeRangeResultCache<SubResult>.Request> childrenList) {
            this.actionTimeRange = timeRange;
            this.trimmedTimeRange = trimmedTimeRange;
            this.callback = callback;
            this.action = action;
            addChildren(childrenList);
        }

        /**
         * Adds a {@link List} of {@link Request}s to this one indicating that this {@link Request} is dependent on
         * their {@link SubResult}s.
         * <p>
         * Might immediately call {@link #notifyActionSuccessIfHasAllResults()} if all children have their results.
         *
         * @throws IllegalStateException
         *             if invoked after {@link #callback} has been called.
         */
        private void addChildren(Iterable<Request> requests) throws IllegalStateException {
            if (callbackWasCalled) {
                throw new IllegalStateException("Children may not be added after results have been submitted!");
            }
            childrenWithoutResultCounter++; // This will later be undone and serves the purpose of delaying the call to
                                            // notifyActionSuccessIfHasAllResults until all children have been added
            for (Request request : requests) {
                if (this.equals(request)) {
                    throw new IllegalArgumentException("Cannot add myself as my own child!");
                }
                final boolean notPresentBefore = childrenSet.add(request);
                if (notPresentBefore) {
                    childrenWithoutResultCounter++;
                }
                request.addParent(this); // Will immediately decrement childrenWithoutResultCounter if result is present
            }
            childrenWithoutResultCounter--;
        }

        /**
         * Obtains all other requests (excluding {@code this} request) of which all or parts of their results are
         * required to combine them with the results of this trimmed request into a result that reflects the response to
         * the original request {@link #action} from which this request was derived by trimming.
         */
        public Set<Request> getChildrenSet() {
            return Collections.unmodifiableSet(childrenSet);
        }
        
        /**
         * Clears {@link #childrenSet} and evicts the removed {@link Request}s from
         * {@link TimeRangeResultCache#requestCache} if they are no longer needed.
         */
        private void releaseChildren() {
            for (Request child : childrenSet) {
                child.removeParent(this);
                if (child.canBeEvicted()) {
                    TimeRangeResultCache.this.evictRequestFromCache(child);
                }
            }
            childrenSet.clear();
        }

        /**
         * Obtains all {@link Request}s that still depend on this one. Those {@link Request}s will be notified by this
         * one once it receives its {@link SubResult}.
         */
        public Set<Request> getParentSet() {
            return parentSet;
        }

        /**
         * Adds a new parent to {@link #parentSet}.
         *
         * @param parent
         *            {@link Request} to add as a parent. Must not be {@code this}.
         */
        private void addParent(Request parent) {
            if (this.equals(parent)) {
                throw new IllegalArgumentException("Cannot add itself as parent!");
            }
            final boolean notPresentBefore = parentSet.add(parent);
            if (notPresentBefore && hasResult) {
                parent.onChildSuccess();
            }
        }

        private void removeParent(Request parent) {
            parentSet.remove(parent);
        }

        /**
         * This method is to be called if {@code this} {@link Request} has received its {@link SubResult}. This
         * {@link Request} will subsequently notify its parent {@link Request}s.
         * <p>
         * If all children have their {@link SubResult}s the {@link #callback} will be notified.
         *
         * @param result
         *            this {@link Request}'s {@link SubResult}
         */
        public void onSuccess(final SubResult result) {
            responseReceived = true;
            setResult(result);
            notifyActionSuccessIfHasAllResults();
            final HashSet<Request> parents = new HashSet<>(parentSet);
            parents.forEach(parent -> parent.onChildSuccess());
            evictIfNoLongerNeeded();
        }

        /**
         * This method is to be called if an error occurred while getting {@code this} {@link Request}'s {@link SubResult}.
         * After calling the {@link #callback} the parents will be notified.
         *
         * @param caught
         *            {@link Throwable} that occurred
         */
        public void onFailure(final Throwable caught) {
            responseReceived = true;
            notifyActionFailure(caught);
            final HashSet<Request> parents = new HashSet<>(parentSet); // onChildFailure will remove elements from parentSet
            parents.forEach(parent -> parent.onChildFailure(caught));
            evictIfNoLongerNeeded();
        }

        /**
         * To be called by a child when it receives its {@link SubResult} via {@link #onSuccess(Object)}.
         * Note that the child's sub-result may be a part only of what the child has requested; yet,
         * the dependency to the child has been taking into account only the child's immediate "trimmed"
         * request and not any of the child's transitive dependencies. Therefore, the child's own request
         * success is what we're interested in here.
         */
        private void onChildSuccess() {
            childrenWithoutResultCounter--;
            notifyActionSuccessIfHasAllResults();
        }

        /**
         * To be called by a child when it encounters an error and is informed of this in {@link #onFailure(Throwable)}.
         *
         * @param caught
         *            {@link Throwable} that occurred
         */
        private void onChildFailure(Throwable caught) {
            notifyActionFailure(new Throwable("Child encountered an error", caught));
        }

        /**
         * Notifies {@link #callback} <b>once</b> if all needed {@link SubResult}s are available.<p>
         * Implements the transition function used by this Petri Net.
         */
        private void notifyActionSuccessIfHasAllResults() {
            if (!callbackWasCalled && hasResult && childrenWithoutResultCounter <= 0) {
                callbackWasCalled = true;
                callback.onSuccess(null);
            }
        }

        /**
         * Notifies {@link #callback} of an error that occurred.
         *
         * @param caught
         *            {@link Throwable} that was encountered.
         */
        private void notifyActionFailure(Throwable caught) {
            if (!callbackWasCalled) {
                callbackWasCalled = true;
                callback.onFailure(caught);
                releaseChildrenAndEvictSelf();
            }
        }

        /**
         * Determines if this {@link Request} can be evicted from the cache.
         * A per-key notification does not mean the action has retrieved its result yet: a multi-key action may
         * still be waiting for other keys. Explicitly released requests must also remain until their own response
         * arrives, even if a dependency has already failed and completed the action's callback.
         * @return {@code true} if it can be evicted
         */
        protected boolean canBeEvicted() {
            return actionResultReleased && responseReceived && parentSet.isEmpty();
        }

        /**
         * Releases the action's claim on its results, calls {@link #releaseChildren()}, and removes this request
         * from the cache if no longer needed.
         */
        protected void releaseChildrenAndEvictSelf() {
            actionResultReleased = true;
            releaseChildren();
            evictIfNoLongerNeeded();
        }

        private void evictIfNoLongerNeeded() {
            if (canBeEvicted()) {
                TimeRangeResultCache.this.evictRequestFromCache(this);
            }
        }

        /**
         * Collects and returns the needed {@link SubResult}s to complete the {@link #action}.
         *
         * @throws IllegalStateException
         *             if the callback has not been notified yet or if the results are being retrieved a second time
         */
        public List<Pair<TimeRange, SubResult>> getActionResult() throws IllegalStateException {
            if (!callbackWasCalled) {
                throw new IllegalStateException("Callback was not called yet");
            }
            if (actionResultReleased) {
                throw new IllegalStateException(
                        "Results have already been retrieved or released");
            }
            final List<Pair<TimeRange, SubResult>> actionResults;
            if (getTrimmedTimeRange() == null) { // true if this Request is completely covered by others
                actionResults = new ArrayList<>(childrenSet.size());
            } else {
                actionResults = new ArrayList<>(childrenSet.size() + 1);
                actionResults.add(new Pair<>(getTrimmedTimeRange(), getResult())); // add this request's own result...
            }
            for (Request child : childrenSet) { // ...as well as all children's results
                if (!child.hasResult()) {
                    throw new IllegalStateException("Child request has no result although one is expected to be there.");
                }
                actionResults.add(new Pair<>(child.getTrimmedTimeRange(), child.getResult()));
            }
            releaseChildrenAndEvictSelf();
            return actionResults;
        }

        /**
         * Obtains the {@link SubResult} of this {@link Request}.
         * @return {@code null} if {@link #hasResult()} is {@code false}
         */
        public SubResult getResult() {
            return result;
        }

        /**
         * Sets the result for this request's total time range; {@link #hasResult()} will return {@code true} after this
         * method returns, and {@link #getResult()} will return {@code result} from then on.
         */
        private void setResult(SubResult result) {
            this.hasResult = true;
            this.result = result;
        }

        /**
         * @return {@code true} if a {@link SubResult} has been set.
         */
        public boolean hasResult() {
            return hasResult;
        }

        /**
         * @return {@code true} if {@link #onFailure(Throwable)} has been called.
         */
        public boolean hasFailed() {
            return callbackWasCalled && !hasResult;
        }

        /**
         * Obtains the {@link TimeRange} originally requested by the {@link #action}.
         */
        public TimeRange getActionTimeRange() {
            return actionTimeRange;
        }

        /**
         * Obtains the {@link TimeRange} that {@link #getActionTimeRange()} was trimmed to due to other {@link Request}s
         * in cache.
         */
        public TimeRange getTrimmedTimeRange() {
            return trimmedTimeRange;
        }
    }

    /**
     * The {@link Request} cache. The keys are the actions for which requests are effectively made. The corresponding
     * value {@link Request} object captures {@link Request#getChildrenSet() requests on which the request depends}
     * after having been trimmed.
     */
    private final Map<TimeRangeAsyncAction<?, ?>, Request> requestCache = new HashMap<>(CACHE_SIZE);

    private void evictRequestFromCache(TimeRangeAsyncAction<?, ?> action) {
        requestCache.remove(action);
    }

    private void evictRequestFromCache(Request request) {
        evictRequestFromCache(request.action);
    }

    int getCacheSize() {
        return requestCache.size();
    }

    /**
     * Trims the requested {@link TimeRange} against results that are already cached or requested and registers the
     * trimmed {@link TimeRange} with the cache as a request that will be made and that therefore shall already be used
     * to trim subsequent requests. Trimming considers only the effective, trimmed time ranges of the requests in the cache and
     * hence, no transitive dependencies across requests will be created.
     *
     * @param toTrim
     *            {@link TimeRange} to request
     * @param forceTimeRange
     *            if {@code false} the request will be optimized with cached results
     * @param action
     *            {@link TimeRangeAsyncAction} that this request belongs to
     * @param callback
     *            {@link AsyncCallback} which will be called exactly once when all needed {@link SubResult}s are ready.<br>
     *            The {@link SubResult}s can be retrieved using {@link #getResults(TimeRangeAsyncAction)}. Can be used,
     *            e.g., to notify the {@code TimeRangeActionsExecutor.ExecutorCallback}'s {@code onSubResultSuccess} method.
     * @return {@link TimeRange} to effectively request (potentially trimmed from {@code toTrim}) or {@code null} if no
     *         request is to be made since the results are cached.
     */
    public TimeRange trimAndRegisterRequest(TimeRange toTrim, boolean forceTimeRange, TimeRangeAsyncAction<?, ?> action,
            AsyncCallback<Void> callback) {
        final Request request = trimTimeRangeAndAttachDeps(toTrim, callback, action, forceTimeRange);
        // cache the request which is expected to not yet have a result
        requestCache.put(action, request);
        // null signals that the result is already present
        return request.getTrimmedTimeRange();
    }

    /**
     * Registers that a request has returned with a result and collects all other cached results that are needed
     * to construct a complete, time-contiguous {@link SubResult}.
     *
     * @param action
     *            {@link TimeRangeAsyncAction} that the request corresponds to
     * @param subResult
     *            {@link SubResult} of the trimmed request; this result will be cached
     */
    public void registerResult(TimeRangeAsyncAction<?, ?> action, SubResult subResult) {
        final Request request = requestCache.get(action);
        if (request == null) {
            throw new IllegalArgumentException("Attempted to register result for non-existent request: " + action.toString());
        }
        request.onSuccess(subResult); // notifies all callbacks that were waiting for this result
    }

    /**
     * Removes the failed request from cache.
     *
     * @param action
     *            corresponding {@link TimeRangeAsyncAction}
     * @param cause
     *            {@link Throwable} that occurred
     */
    public void registerFailure(TimeRangeAsyncAction<?, ?> action, Throwable cause) {
        final Request request = requestCache.get(action);
        if (request != null) {
            request.onFailure(cause);
        }
    }

    /**
     * Retrieves the needed {@link SubResult}s to answer the {@code action}.
     * <p>
     * This method is <b>only to be called once</b> after the {@link AsyncCallback} passed to
     * {@link #trimAndRegisterRequest(TimeRange, boolean, TimeRangeAsyncAction, AsyncCallback)} was called indicating
     * that the {@link SubResult}s are ready.
     *
     * @param action
     *            corresponding {@link TimeRangeAsyncAction}
     * @return {@link Pair}s of {@link TimeRange}s and {@link SubResult}s which together cover the {@link TimeRange}
     *         requested by {@code action}
     */
    public List<Pair<TimeRange, SubResult>> getResults(TimeRangeAsyncAction<?, ?> action) throws IllegalArgumentException {
        final Request request = requestCache.get(action);
        if (request == null) {
            throw new IllegalArgumentException("No request found for action: " + action.toString());
        }
        return request.getActionResult();
    }

    /**
     * Informs a {@link Request} that its {@link SubResult}s are no longer needed.
     *
     * @param action
     *            corresponding {@link TimeRangeAsyncAction}
     */
    public void removeRequest(TimeRangeAsyncAction<?, ?> action) throws IllegalArgumentException {
        final Request request = requestCache.get(action);
        if (request == null) {
            throw new IllegalArgumentException("No request found for action: " + action.toString());
        }
        request.releaseChildrenAndEvictSelf();
    }

    /**
     * Create a potentially trimmed request based on cached results, returning a {@link Request} with a potentially
     * trimmed time range that reflects which part(s) is/are missing from the cache. For simplicity {@code toTrim} will
     * currently not be split up into multiple TimeRanges if a part in the middle is cached.
     *
     * @param toTrim
     *            the time range to try to trim; can lead to attaching dependencies to the resulting request that
     *            reflect of which other requests the results are required in order to fulfill the request
     * @return a request with a {@link Request#getTrimmedTimeRange() trimmed time range} that may be smaller than
     *         {@code toTrim} if parts of the range requested were found in the cache; in that case, corresponding
     *         dependencies have been added to the request. The returned request's {@link Request#getTrimmedTimeRange()
     *         trimmed time range} will be {@code null} if no request to the server is to be made.
     */
    private Request trimTimeRangeAndAttachDeps(final TimeRange toTrim, AsyncCallback<Void> callback,
            TimeRangeAsyncAction<?, ?> action, boolean forceTimeRange) {
        // TODO There is a lot of potential for improvements here
        TimeRange potentiallyTrimmed = toTrim;
        final List<Request> rangesToTrimWithAsList = new LinkedList<>(requestCache.values());
        final List<Request> childrenList = new ArrayList<>();
        iterationsLoop: for (int i = 0; i < TRIM_MAX_ITERATIONS; i++) {
            boolean rangeWasTrimmedThisIteration = false;
            final Iterator<Request> iter = rangesToTrimWithAsList.iterator();
            while (iter.hasNext()) {
                final Request element = iter.next();
                final TimeRange trimWith = element.getTrimmedTimeRange();
                if (trimWith != null && !element.hasFailed()) {
                    final boolean rangeAfter = potentiallyTrimmed.from().after(trimWith.to()) || potentiallyTrimmed.from().equals(trimWith.to());
                    final boolean fromAfter = potentiallyTrimmed.from().after(trimWith.from());
                    final boolean fromIncluded = fromAfter || potentiallyTrimmed.from().equals(trimWith.from());
                    final boolean rangeBefore = potentiallyTrimmed.to().before(trimWith.from()) || potentiallyTrimmed.to().equals(trimWith.from());
                    final boolean toBefore = potentiallyTrimmed.to().before(trimWith.to());
                    final boolean toIncluded = toBefore || potentiallyTrimmed.to().equals(trimWith.to());
                    if (rangeAfter || rangeBefore) {
                        // potentiallyTrimmed and trimWith do not touch at all; Do not consider in following iterations 
                        iter.remove();
                    } else if (fromIncluded && toIncluded) {
                        // potentiallyTrimmed is completely included in trimWith
                        childrenList.add(element);
                        potentiallyTrimmed = null;
                        break iterationsLoop; // potentiallyTrimmed has been completely trimmed away
                    } else if (fromIncluded || toIncluded) {
                        // potentiallyTrimmed overlaps trimWith but only on one side i.e. trimWith is not included in potentiallyTrimmed
                        childrenList.add(element);
                        potentiallyTrimmed = potentiallyTrimmed.subtract(trimWith).iterator().next();
                        iter.remove();
                        rangeWasTrimmedThisIteration = true;
                    }
                } else {
                    // trimWith is null; Do not consider in following iterations
                    iter.remove();
                }
            }
            if (!rangeWasTrimmedThisIteration) {
                break iterationsLoop;
            }
        }
        final Request request = new Request(toTrim, potentiallyTrimmed, callback, action, childrenList);
        return request;
    }
}
