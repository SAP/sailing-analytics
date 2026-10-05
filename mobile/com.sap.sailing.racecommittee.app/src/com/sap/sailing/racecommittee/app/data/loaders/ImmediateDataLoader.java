package com.sap.sailing.racecommittee.app.data.loaders;

import java.util.concurrent.Callable;

import android.content.Context;
import androidx.loader.content.Loader;

/**
 * A {@link Loader} that (immediately) returns a {@link Callable}'s result. All results are announced as non-cached.
 * 
 * @param <T>
 *            result type.
 */
public class ImmediateDataLoader<T> extends Loader<DataLoaderResult<T>> {

    private Callable<T> resultCallable;

    public ImmediateDataLoader(Context context, Callable<T> resultCallable) {
        super(context);
        this.resultCallable = resultCallable;
    }

    @Override
    protected void onStartLoading() {
        publishResult();
    }

    @Override
    protected void onForceLoad() {
        publishResult();
    }

    private void publishResult() {
        try {
            // We act as a never caching DataLoader to ensure that it looks like remote access!
            deliverResult(new DataLoaderResult<T>(resultCallable.call(), false));
        } catch (Exception e) {
            deliverResult(new DataLoaderResult<T>(e));
        }
    }
}
