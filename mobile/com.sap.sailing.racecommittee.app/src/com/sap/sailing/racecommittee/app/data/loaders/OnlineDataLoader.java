package com.sap.sailing.racecommittee.app.data.loaders;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import com.sap.sailing.android.shared.data.http.HttpGetRequest;
import com.sap.sailing.android.shared.data.http.HttpRequest;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.racecommittee.app.data.handlers.DataHandler;
import com.sap.sailing.racecommittee.app.data.parsers.DataParser;

import android.content.Context;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;

/**
 * <p>
 * A {@link Loader} that loads data by accessing a remote resource.
 * </p>
 * <p/>
 * <p>
 * An {@link OnlineDataLoader} may return cached results as announced by its {@link DataHandler}. Call
 * {@link OnlineDataLoader#forceLoad()} to ensure that the remote resource is checked for new data.
 * </p>
 * <p/>
 * <p>
 * The data returned by the remote resource is parsed by the given {@link DataParser} and may be cached through your
 * implementation of {@link DataHandler#onResult(Object, boolean)}.
 * </p>
 *
 * @param <T>
 *            result type.
 */
public class OnlineDataLoader<T> extends AsyncTaskLoader<DataLoaderResult<T>> {
    private static final String TAG = OnlineDataLoader.class.getName();

    protected DataParser<T> dataParser;
    protected DataHandler<T> dataHandler;
    protected HttpRequest httpRequest;

    /**
     * Initializes a new {@link OnlineDataLoader} which initiates HTTP GET requests to load the remote data.
     */
    public OnlineDataLoader(Context context, URL requestUrl, DataParser<T> dataParser, DataHandler<T> dataHandler) {
        this(context, new HttpGetRequest(requestUrl, context), dataParser, dataHandler);
    }

    /**
     * Initializes a new {@link OnlineDataLoader} which executes the given {@link HttpRequest} to load the remote data.
     */
    public OnlineDataLoader(Context context, HttpRequest request, DataParser<T> dataParser,
            DataHandler<T> dataHandler) {
        super(context);
        this.httpRequest = request;
        this.dataParser = dataParser;
        this.dataHandler = dataHandler;

        ExLog.i(getContext(), TAG, String.format("Loader created: %s", Integer.toHexString(this.hashCode())));
    }

    @Override
    protected void onStartLoading() {
        if (dataHandler.hasCachedResults()) {
            ExLog.i(getContext(), TAG,
                    String.format("Using cached results... %s", Integer.toHexString(this.hashCode())));
            deliverResult(new DataLoaderResult<T>(dataHandler.getCachedResults(), true));
        } else {
            ExLog.i(getContext(), TAG,
                    String.format("No cached results. Forcing load now %s", Integer.toHexString(this.hashCode())));
            forceLoad();
        }
    }

    @Override
    protected void onForceLoad() {
        ExLog.i(getContext(), TAG, String.format("Forcing load %s", Integer.toHexString(this.hashCode())));
        super.onForceLoad();
    }

    @Override
    public void deliverResult(DataLoaderResult<T> result) {
        if (result.isSuccessful()) {
            dataHandler.onResult(result.getResult(), result.isResultCached());
        }
        super.deliverResult(result);
    }

    @Override
    public DataLoaderResult<T> loadInBackground() {
        try {
            return new DataLoaderResult<T>(loadDataInBackground(), false);
        } catch (Exception e) {
            ExLog.ex(getContext(), TAG, e);
            return new DataLoaderResult<T>(e);
        }
    }

    private T loadDataInBackground() throws Exception {
        Reader reader = null;
        try {
            InputStream inputStream = httpRequest.execute();
            reader = new InputStreamReader(inputStream);
            return dataParser.parse(reader);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

}
