package com.sap.sse.gwt.client.async;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.common.Duration;

/**
 * Use this as the return type of a GWT RPC service method that may not have the correct answer to the request readily
 * available, but may so after some longer-running computation. The service method can use this result type to inform
 * the calling client that a valid response may be available at a later point in time, asking the client to re-try the
 * request. For this, the service method constructs an instance of this type using the {@link #retry(Duration)} factory
 * method, telling the duration after which the client may have more luck getting a valid response when trying the
 * request again.
 * <p>
 * 
 * If a valid result is available, the service method shall use {@link #withResult(Object)} to create and then return an
 * instance with a valid result, telling the client that no more re-tries are required.
 * <p>
 * 
 * This way, such a service method can always return some response quickly, as it should.
 * <p>
 * 
 * Clients would usually call service methods returning objects of this type using a
 * {@link RetryableAsyncAction}{@code <T>}, either directly invoking its
 * {@link RetryableAsyncAction#execute(AsyncCallback)} method with an {@link AsyncCallback}{@code <T>} as callback, or
 * as a "droppable" action, such that the client specifies an {@link AsyncCallback}{@code <T>} to the
 * {@link AsyncActionsExecutor#execute(AsyncAction, AsyncCallback)} or
 * {@link AsyncActionsExecutor#execute(AsyncAction, String, AsyncCallback)} method, whereas the actual RPC method to be
 * invoked by the action has a result type of {@link RetryableActionResult}{@code <T>}.
 * <p>
 * 
 * Clients can also call service methods using this as a return type directly, using an
 * {@link AsyncCallback}{@code <RetryableActionResult<T>>} but they would have to handle any re-try and result
 * extraction logic themselves, so this is not the recommended usage.
 * <p>
 * 
 * The service that returns such a result is expected to either deliver the valid result, or to provide instructions for
 * retrying, in particular how long to wait until the next retry. Use in conjunction with {@link RetryableAsyncAction},
 * such that the client specifies an {@link AsyncCallback}{@code <T>} to the
 * {@link AsyncActionsExecutor#execute(AsyncAction, AsyncCallback)} or
 * {@link AsyncActionsExecutor#execute(AsyncAction, String, AsyncCallback)} method, whereas the actual RPC method to be
 * invoked by the action has a result type of {@link RetryableActionResult}{@code <T>}.
 * <p>
 * 
 * Timeout behavior should be controlled by the client, e.g., the {@link RetryableAsyncAction} itself that provides the
 * {@link AsyncActionsExecutor} with timeout information.
 * 
 * @author Axel Uhl (d043530)
 *
 * @param <T>
 *            the actual result type; in other words, the type of the "payload"
 */
public class RetryableActionResult<T> implements Serializable {
    private static final long serialVersionUID = -927895877316370853L;
    private boolean needsRetry;
    private T result;
    private Duration durationUntilNextRetry;
    
    @Deprecated
    RetryableActionResult() { // for GWT serialization only
    }
    
    public static <T> RetryableActionResult<T> withResult(T result) {
        return new RetryableActionResult<>(/* needs retry */ false, result, /* duration until next retry */ null);
    }
    
    public static <T> RetryableActionResult<T> retry(Duration durationUntilNextRetry) {
        return new RetryableActionResult<>(/* needs retry */ true, /* result */ null, durationUntilNextRetry);
    }
    
    private RetryableActionResult(boolean needsRetry, T result, Duration durationUntilNextRetry) {
        super();
        this.needsRetry = needsRetry;
        this.result = result;
        this.durationUntilNextRetry = durationUntilNextRetry;
    }

    public boolean needsRetry() {
        return needsRetry;
    }
    
    /**
     * Precondition: {@code !}{@link #needsRetry()}
     * 
     * @return the actual result of the service invocation; undefined if {@link #needsRetry()} returns {@code true}
     */
    public T get() {
        return result;
    }
    
    /**
     * Precondition: {@link #needsRetry()}{@code == true}
     * 
     * @return the duration that {@link AsyncActionsExecutor} shall wait before it tries to re-try the request
     */
    public Duration getDurationUntilNextRetry() {
        return durationUntilNextRetry;
    }
}
