package com.sap.sse.gwt.client.async;

import java.util.concurrent.TimeoutException;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.common.TimePoint;

/**
 * A specific type of {@link AsyncAction} that collaborates with back-end services returning a result of type
 * {@link RetryableActionResult}, a wrapper around a regular result of type {@code T}. Concrete subclasses must
 * implement the {@link #executeOnce(AsyncCallback)} method, like regular {@link AsyncAction} implementations must
 * provide an {@link #execute(AsyncCallback)} method, calling the back-end service method with the callback provided.
 * <p>
 * 
 * Actions of this type can be used with {@link AsyncActionsExecutor#execute(AsyncAction, AsyncCallback)} and
 * {@link AsyncActionsExecutor#execute(AsyncAction, String, AsyncCallback)} like other {@link AsyncAction}s. The
 * {@link AsyncCallback} that is provided to those {@code execute} methods expects a result of type {@code T}. The
 * actual service implementation, however, returns a {@code T} wrapped in an {@link RetryableActionResult} with that
 * {@code T} object inside, or instructions for retry in case no result could be obtained.
 * <p>
 * 
 * Actions of this type may also be used standalone, directly invoking the {@link #execute(AsyncCallback)} method
 * with the client-side callback; the action then will carry out the retry behavior without the dropping/queueing
 * logic provided otherwise by the {@link AsyncActionsExecutor}.<p>
 * 
 * When the {@link RetryableActionResult} specifies that it {@link RetryableActionResult#needsRetry() needs retry}, this
 * action automatically handles the retry logic. It honors the {@link RetryableActionResult#getDurationUntilNextRetry()}
 * duration specified by the service and waits at least this long before retrying. It also honors the
 * {@link #getMaximumNumberOfRetries() maximum number of retries} specified by this action (default 3, subclasses may
 * override). In case of timeout or if exceeding the maximum number of retries, a {@link TimeoutException} is passed to
 * the original callback's {@link AsyncCallback#onFailure(Throwable)} method.
 * <p>
 * 
 * From the {@link AsyncActionsExecutor}'s perspective, this action remains in "pending" state until it has provided
 * a valid result or it has received a failure from the original back-end service, or it has timed out, or it has
 * reached the maximum number of retries without having produced a valid result. This also means that the usual
 * dropping behavior is in place, meaning that if more such actions are submitted to the executor for execution while
 * a "retry" cycle is still ongoing, in-between executions may be dropped.<p>
 * 
 * The action works by wrapping the callback passed to the {@link #execute(AsyncCallback)} method by an instance of the
 * inner {@link Callback} class, which passes failures on to the original callback directly, and which analyzes a
 * "successful" {@link RetryableActionResult} regarding the need for retry, and passing this {@link Callback} wrapper to
 * the {@link #executeOnce(AsyncCallback)} method. If a retry is needed and is possible regarding the maximum number of
 * allowed retries and respecting the timeout time point, the {@link Callback#onSuccess(RetryableActionResult)} method
 * schedules another invocation of {@link #executeOnce(AsyncCallback)}, passing itself as the callback again, using the
 * {@link RetryableActionResult#getDurationUntilNextRetry()} value for the scheduling delay.
 *
 * @author Axel Uhl (d043530)
 *
 * @param <T>
 */
public abstract class RetryableAsyncAction<T> implements AsyncAction<T> {
    private int remainingNumberOfRetries;
    
    private class Callback implements AsyncCallback<RetryableActionResult<T>> {
        private final AsyncCallback<T> callback;
        
        public Callback(AsyncCallback<T> callback) {
            this.callback = callback;
        }

        @Override
        public void onFailure(Throwable caught) {
            callback.onFailure(caught);
        }

        @Override
        public void onSuccess(RetryableActionResult<T> result) {
            if (result.needsRetry()) {
                if (--remainingNumberOfRetries == 0) {
                    callback.onFailure(new TimeoutException("maximum number of retries reached: "+getMaximumNumberOfRetries()));
                    GWT.log("maximum number of retries reached: "+getMaximumNumberOfRetries()+" on action of type "+this.getClass().getName());
                } else if (TimePoint.now().after(getTimeout())) {
                    callback.onFailure(new TimeoutException("action timed out at "+getTimeout()));
                    GWT.log("action of type "+this.getClass().getName()+" timed out");
                } else {
                    GWT.log("action of type " + this.getClass().getName() + " needs retry; scheduling to run again in "
                            + result.getDurationUntilNextRetry()+"; "+remainingNumberOfRetries+" retries left");
                    Scheduler.get().scheduleFixedDelay(()->{
                        executeOnce(this);
                        return false; // schedule only once for now
                    }, (int) result.getDurationUntilNextRetry().asMillis());
                }
            } else {
                callback.onSuccess(result.get());
            }
        }
    }
    
    protected RetryableAsyncAction() {
        this.remainingNumberOfRetries = getMaximumNumberOfRetries();
    }
    
    @Override
    public final void execute(AsyncCallback<T> callback) {
        executeOnce(new Callback(callback));
    }
    
    /**
     * Non-abstract subclasses must implement this method. It "replaces" the {@link #execute(AsyncCallback)} method
     * and is expected to carry out the actual remove service method invocation. That method is expected to return
     * a {@link RetryableActionResult} wrapper around an actual result, which provides the service method a way of
     * saying "I'm not ready yet, but I understand you cannot wait for me, so please try again in X."
     */
    protected abstract void executeOnce(AsyncCallback<RetryableActionResult<T>> callback);
    
    /**
     * @return -1 would mean no limit; this default implementation returns {@code 3}.
     */
    protected int getMaximumNumberOfRetries() {
        return 3;
    }
    
    /**
     * Default: no timeout (run until the "end of time")
     */
    protected TimePoint getTimeout() {
        return TimePoint.EndOfTime;
    }
}
