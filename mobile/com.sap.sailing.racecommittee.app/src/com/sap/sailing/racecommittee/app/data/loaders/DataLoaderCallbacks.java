package com.sap.sailing.racecommittee.app.data.loaders;

import android.content.Context;
import android.os.Bundle;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.Loader;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.racecommittee.app.data.clients.LoadClient;

/**
 * <p>
 * {@link LoaderCallbacks} wrapper to be used with {@link LoadClient}s.
 * </p>
 *
 * <p>
 * On result {@link DataLoaderResult#isSuccessful()} is checked. If true
 * {@link LoadClient#onLoadSucceeded(Object, boolean)} is called; other {@link LoadClient#onLoadFailed(Exception)} is
 * called.
 * </p>
 *
 * <p>
 * There is currently no handling of a {@link Loader}'s reset.
 * </p>
 *
 * @param <T> result type.
 */
public class DataLoaderCallbacks<T> implements LoaderCallbacks<DataLoaderResult<T>> {
    private static String TAG = DataLoaderCallbacks.class.getName();

    public interface LoaderCreator<T> {
        Loader<DataLoaderResult<T>> create(int id, Bundle args) throws Exception;
    }

    private LoadClient<T> clientCallback;
    private LoaderCreator<T> loaderCreator;
    private final Context context;

    public DataLoaderCallbacks(LoadClient<T> clientCallback, LoaderCreator<T> loaderCreator, Context context) {
        this.clientCallback = clientCallback;
        this.loaderCreator = loaderCreator;
        this.context = context;
    }

    @Override
    public Loader<DataLoaderResult<T>> onCreateLoader(int id, Bundle args) {
        try {
            return loaderCreator.create(id, args);
        } catch (Exception e) {
            ExLog.ex(context, TAG, e);
        }
        throw new IllegalStateException("Exception while creating a loader.");
    }

    @Override
    public void onLoadFinished(Loader<DataLoaderResult<T>> loader, DataLoaderResult<T> result) {

        if (result.isSuccessful()) {
            clientCallback.onLoadSucceeded(result.getResult(), result.isResultCached());
        } else {
            clientCallback.onLoadFailed(result.getException());
        }
    }

    @Override
    public void onLoaderReset(Loader<DataLoaderResult<T>> loader) {
        // currently we ignore loader resets...
    }

}
