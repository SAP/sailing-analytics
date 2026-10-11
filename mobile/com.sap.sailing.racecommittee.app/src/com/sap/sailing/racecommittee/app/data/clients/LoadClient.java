package com.sap.sailing.racecommittee.app.data.clients;

import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

/**
 * Interface to hide complexity of {@link LoaderCallbacks#onLoadFinished(Loader, Object)}. Keep in mind that all
 * restrictions that apply on the LoaderCallback's method also apply to all methods of this interface.
 *
 * @param <T> loaded data type.
 */
public interface LoadClient<T> {

    /**
     * Called when a {@link Loader} returned a failure to its {@link LoaderCallbacks#onLoadFinished(Loader, Object)}.
     *
     * @param reason the loading has failed.
     */
    void onLoadFailed(Exception reason);

    /**
     * Called when a {@link Loader} returned successfully to its {@link LoaderCallbacks#onLoadFinished(Loader, Object)}.
     *
     * @param data that was loaded.
     * @param isCached <code>true</code> if returned data is cached data.
     */
    void onLoadSucceeded(T data, boolean isCached);
}
