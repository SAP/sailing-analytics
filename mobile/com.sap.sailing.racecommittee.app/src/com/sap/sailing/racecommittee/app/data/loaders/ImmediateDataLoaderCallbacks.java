package com.sap.sailing.racecommittee.app.data.loaders;

import java.util.concurrent.Callable;

import com.sap.sailing.racecommittee.app.data.clients.LoadClient;

import android.content.Context;
import android.os.Bundle;
import androidx.loader.content.Loader;

/**
 * Simple wrapper for a {@link DataLoaderCallbacks} creating an {@link ImmediateDataLoader}.
 * 
 * @param <T>
 *            the result type.
 */
public class ImmediateDataLoaderCallbacks<T> extends DataLoaderCallbacks<T> {
    public ImmediateDataLoaderCallbacks(final Context context, LoadClient<T> clientCallback,
            final Callable<T> loadingFunc) {
        super(clientCallback, new LoaderCreator<T>() {
            @Override
            public Loader<DataLoaderResult<T>> create(int id, Bundle args) throws Exception {
                return new ImmediateDataLoader<T>(context, loadingFunc);
            }
        }, context);
    }
}
