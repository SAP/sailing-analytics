package com.sap.sse.gwt.client.async;

import java.util.logging.Logger;

import com.google.gwt.debug.client.DebugInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * <p>Decorator for asynchronous remote procedure calls which should be marked as pending until they complete.
 *   Such calls will be marked using a counter, which is necessary in UI tests to be able able to tell exactly when an
 *   asynchronous request has finished. With this counter a test which triggers a request (which can cause additional
 *   requests) can wait until the requests have finished. It's guaranteed that the counter is decremented no matter if
 *   the call was successful or not.</p>
 * 
 * <p>Note: Since the counter for pending Ajax requests is incremented as soon as an instance of this class is created
 *   you must not create instances of this class which are not used or assigned to an field for multiple uses!</p>
 * 
 * <p>See {@code com.sap.sailing.selenium.pages.PageObject} and its {@code waitForAjaxRequests...} methods for
 * the consuming side of this.</p>
 * 
 * @param <T>
 *   The type of the return value for the asynchronous remote procedure call.
 * @author
 *   D049941
 */
public final class MarkedAsyncCallback<T> implements AsyncCallback<T> {
    private final static Logger logger = Logger.getLogger(MarkedAsyncCallback.class.getName());
    
    /**
     * <p>The key for the category of global requests. The key for the global category is just an empty string.</p>
     */
    public static final String CATEGORY_GLOBAL = ""; //$NON-NLS-1$
    
    private String category;
    
    private AsyncCallback<T> callback;
    
    /**
     * <p>Creates a marked asynchronous remote procedure call for the global category.</p>
     * 
     * <p>ATTENION: Since the counter for pending Ajax requests is incremented as soon as an instance of this class is
     *   created you must not create instances which are not used or assigned to an field!</p>
     */
    public MarkedAsyncCallback(AsyncCallback<T> callback) {
        this(callback, CATEGORY_GLOBAL);
    }
    
    /**
     * <p>Creates a marked asynchronous remote procedure call for the given category. As soon as the asynchronous remote
     *   procedure call is created, the counter for pending Ajax calls for the given category is incremented.</p>
     * 
     * <p>ATTENION: Since the counter for pending Ajax requests is incremented as soon as an instance of this class is
     *   created you must not create instances which are not used or assigned to an field!</p>
     * 
     * @param category
     *   The category of the asynchronous remote procedure call.
     */
    public MarkedAsyncCallback(AsyncCallback<T> callback, String category) {
        this.callback = callback;
        this.category = category;
        try {
            if (DebugInfo.isDebugIdEnabled()) {
                PendingAjaxCallMarker.incrementPendingAjaxCalls(this.category);
            }
        } catch (Throwable e) {
            if (e.getClass().getSimpleName().equals("ExceptionInInitializerError")
                    || e.getClass().getSimpleName().equals("NoClassDefFoundError")) {
                // we're not in a GWT but probably in a test environment
                logger.info("If this occurs outside of tests, please be alarmed!");
            } else {
                throw e;
            }
        }
    }
    
    /**
     * <p>Called when the asynchronous call fails to complete normally. The concrete handling of the failure is
     *   delegated to the decorated callback. It's guaranteed that the counter for the pending requests is
     *   decremented.</p>
     * 
     * @param cause
     *   The failure encountered while executing the remote procedure call.
     */
    @Override
    public final void onFailure(Throwable cause) {
        try {
            this.callback.onFailure(cause);
        } finally {
            try {
                if (DebugInfo.isDebugIdEnabled()) {
                    PendingAjaxCallMarker.decrementPendingAjaxCalls(this.category);
                }
            } catch (Throwable e) {
                if (e.getClass().getSimpleName().equals("ExceptionInInitializerError")
                        || e.getClass().getSimpleName().equals("NoClassDefFoundError")) {
                    // we're not in a GWT but probably in a test environment
                    logger.info("If this occurs outside of tests, please be alarmed!");
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * <p>Called when the asynchronous call completes successfully. The concrete handling of the result is delegated to
     *   the decorated callback. It's guaranteed that the counter for the pending requests is
     *   decremented.</p>
     * 
     * @param result
     *   The return value of the remote produced call.
     */
    @Override
    public final void onSuccess(T result) {
        try {
            this.callback.onSuccess(result);
        } finally {
            try {
                if (DebugInfo.isDebugIdEnabled()) {
                    PendingAjaxCallMarker.decrementPendingAjaxCalls(this.category);
                }
            } catch (Throwable e) {
                if (e.getClass().getSimpleName().equals("ExceptionInInitializerError")
                        || e.getClass().getSimpleName().equals("NoClassDefFoundError")) {
                    // we're not in a GWT but probably in a test environment
                    logger.info("If this occurs outside of tests, please be alarmed!");
                } else {
                    throw e;
                }
            }
        }
    }
}
