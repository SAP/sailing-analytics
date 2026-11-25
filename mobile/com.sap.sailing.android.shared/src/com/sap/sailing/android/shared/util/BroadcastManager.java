package com.sap.sailing.android.shared.util;

import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class BroadcastManager {
    private static BroadcastManager mInstance;
    private final Context mContext;
    private Queue<Intent> mQueue;
    private DelayedTask mTask;

    private BroadcastManager(Context context) {
        mContext = context.getApplicationContext();
        mQueue = new LinkedList<>();
    }

    public synchronized static BroadcastManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BroadcastManager(context);
        }
        return mInstance;
    }

    public void addIntent(Intent intent) {
        mQueue.add(intent);

        if (mTask == null || !mTask.isRunning()) {
            mTask = new DelayedTask();
            mTask.execute();
        }
    }

    private class DelayedTask extends AsyncTask<Void, Void, Void> {

        private boolean mIsRunning = false;

        @Override
        protected Void doInBackground(Void... params) {
            mIsRunning = true;
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // nö
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (mQueue.size() > 0) {
                Intent intent = mQueue.remove();
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
                if (mQueue.size() > 0) {
                    mTask = new DelayedTask();
                    mTask.execute();
                }
            }

            mIsRunning = false;
        }

        public boolean isRunning() {
            return mIsRunning;
        }
    }
}
