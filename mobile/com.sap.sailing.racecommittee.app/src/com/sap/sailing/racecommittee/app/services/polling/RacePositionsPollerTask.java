package com.sap.sailing.racecommittee.app.services.polling;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.content.Context;
import android.os.AsyncTask;

import com.sap.sailing.android.shared.data.http.HttpJsonGetRequest;
import com.sap.sailing.android.shared.data.http.HttpRequest;
import com.sap.sse.common.Util;

public class RacePositionsPollerTask extends AsyncTask<Util.Pair<String, URL>, PollingResult, Void> {

    private final PollingResultListener listener;
    private final Context context;

    public RacePositionsPollerTask(PollingResultListener listener, Context context) {
        this.listener = listener;
        this.context = context;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Void doInBackground(Util.Pair<String, URL>... queries) {
        for (Util.Pair<String, URL> query : queries) {
            if (isCancelled()) {
                return null;
            }

            HttpRequest request = new HttpJsonGetRequest(context, query.getB());
            try {
                InputStream responseStream = request.execute();
                publishProgress(new PollingResult(true,
                        new Util.Pair<>(query.getA(), responseStream)));
            } catch (IOException e) {
                // don't need to close responseStream as it still must
                // be null because the only call that may throw an
                // IOException is the execute() call that would have
                // initialized responseStream
                publishProgress(new PollingResult(false, null));
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(PollingResult... values) {
        listener.onPollingResult(values[0]);
    }

    @Override
    protected void onPostExecute(Void result) {
        listener.onPollingFinished();
    }

    public interface PollingResultListener {
        public void onPollingResult(PollingResult result);

        public void onPollingFinished();
    }
}
