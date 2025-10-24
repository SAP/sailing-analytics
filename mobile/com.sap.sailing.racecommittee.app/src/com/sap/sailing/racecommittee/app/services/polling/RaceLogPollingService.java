package com.sap.sailing.racecommittee.app.services.polling;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.services.sending.MessageSendingService;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.AppPreferences;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.DataStore;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RaceLogPollingService extends Service
        implements AppPreferences.PollingActiveChangedListener, RaceLogPollingTask.PollingResultListener {

    private static final String TAG = RaceLogPollingService.class.getName();

    private AlarmManager mAlarm;
    private Handler mHandler;
    /**
     * When using the {@link #mHandler}, record the time for which the last {@link #poll()}}
     * call was scheduled. When new polls are requested to be scheduled, only request a new
     * poll if there isn't already one scheduled (-1), or the last scheduled poll should have
     * run more than the polling interval ago (maybe we missed a cycle...).
     */
    private long mLastPostedPollTimeMillis = -1;
    private PendingIntent mPendingIntent;
    private AppPreferences mAppPreferences;
    private DataStore mDataStore;
    private Map<String, URL> mRaces;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mHandler = Handler.createAsync(Looper.getMainLooper());
        } else {
            mAlarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        }
        mAppPreferences = AppPreferences.on(this);
        mAppPreferences.registerPollingActiveChangedListener(this);
        mRaces = new HashMap<>();
        mDataStore = DataManager.create(this).getDataStore();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            ExLog.i(this, TAG, "Restarted");
        } else {
            String action = intent.getAction();
            String raceId = intent.getStringExtra(AppConstants.EXTRA_RACE_ID);
            if (AppConstants.ACTION_POLLING_STOP.equals(action)) {
                stopSelf(startId);
            } else if (AppConstants.ACTION_POLLING_RACE_ADD.equals(action)) {
                if (TextUtils.isEmpty(raceId)) {
                    ExLog.i(this, TAG, AppConstants.EXTRA_RACE_ID + " was null for " + action);
                } else {
                    registerRace(raceId);
                }
            } else if (AppConstants.ACTION_POLLING_RACE_REMOVE.equals(action)) {
                if (TextUtils.isEmpty(raceId)) {
                    ExLog.i(this, TAG, AppConstants.EXTRA_RACE_ID + " was null for " + action);
                } else {
                    unregisterRace(raceId);
                }
            } else if (AppConstants.ACTION_POLLING_POLL.equals(action)) {
                poll();
            }
        }

        // We want this service to continue running until it is explicitly
        // stopped, therefore return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mRaces.clear();
        if (mAppPreferences != null) {
            mAppPreferences.unregisterPollingActiveChangedListener(this);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            if (mAlarm != null && mPendingIntent != null) {
                mAlarm.cancel(mPendingIntent);
            }
        }
    }

    @Override
    public void onPollingActiveChanged(boolean isActive) {
        if (isActive) {
            scheduleNextPoll();
            ExLog.i(this, TAG,
                    "Polling has been activated, will start in " + mAppPreferences.getPollingIntervalInSeconds() + " seconds.");
        } else {
            ExLog.i(this, TAG, "Polling has been deactivated, next polling attempt will be aborted.");
        }
    }

    @Override
    public void onPollingFinished() {
        if (!mRaces.isEmpty()) {
            scheduleNextPoll();
        } else {
            stopSelf();
        }
    }

    /**
     * calculates for the given race id the poll url and add it to the map
     *
     * @param raceId race id
     */
    private void registerRace(String raceId) {
        ExLog.i(this, TAG, "registerRace: " + raceId);
        ManagedRace race = getManagedRace(raceId);
        if (!mRaces.containsKey(raceId) && race != null) {
            try {
                mRaces.put(raceId, createURL(race));
            } catch (MalformedURLException | UnsupportedEncodingException e) {
                ExLog.e(this, TAG,
                        String.format("Unable to create polling URL for race %s: %s", race.getId(), e.getMessage()));
            }
            scheduleNextPoll();
        }
    }

    /**
     * Remove the {@link ManagedRace}, so it will no longer be part of the polling
     *
     * @param raceId race id
     */
    private void unregisterRace(String raceId) {
        ExLog.i(this, TAG, "unregisterRace: " + raceId);
        if (mRaces.containsKey(raceId)) {
            mRaces.remove(raceId);
            scheduleNextPoll();
        }
    }

    /**
     * Find the {@link ManagedRace} for the given race id in the {@link DataStore}
     *
     * @param raceId race id
     * @return {@link ManagedRace} if found, else null
     */
    private ManagedRace getManagedRace(String raceId) {
        ManagedRace result = null;
        for (ManagedRace race : mDataStore.getRaces()) {
            if (race.getId().equals(raceId)) {
                result = race;
                break;
            }
        }
        return result;
    }

    private URL createURL(ManagedRace race) throws MalformedURLException, UnsupportedEncodingException {
        return new URL(MessageSendingService.getRaceLogEventSendAndReceiveUrl(this, race.getRaceGroup().getName(),
                race.getName(), race.getFleet().getName()));
    }

    private void poll() {
        ExLog.i(this, TAG, "Polling for server-side race log changes...");
        if (mAppPreferences.isPollingActive()) {
            List<Util.Pair<String, URL>> queries = getPollingQueries();
            RaceLogPollingTask task = new RaceLogPollingTask(this, this);
            @SuppressWarnings("unchecked")
            Util.Pair<String, URL>[] param = queries.toArray(new Util.Pair[0]);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, param);
            mLastPostedPollTimeMillis = -1;
        }
    }

    private List<Util.Pair<String, URL>> getPollingQueries() {
        List<Util.Pair<String, URL>> queries = new ArrayList<>();
        for (Map.Entry<String, URL> entry : mRaces.entrySet()) {
            queries.add(new Util.Pair<>(entry.getKey(), entry.getValue()));
        }
        return queries;
    }

    /**
     * calculate the new alarm for polling with current polling interval from preferences
     */
    private void scheduleNextPoll() {
        if (mAppPreferences.isPollingActive() && !mRaces.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (mLastPostedPollTimeMillis == -1
                        || mLastPostedPollTimeMillis < SystemClock.uptimeMillis() - 1000L * mAppPreferences.getPollingIntervalInSeconds()) {
                    mLastPostedPollTimeMillis = SystemClock.uptimeMillis() + 1000L * mAppPreferences.getPollingIntervalInSeconds();
                    mHandler.postAtTime(this::poll, mLastPostedPollTimeMillis);
                }
            } else {
                final long time = MillisecondsTimePoint.now().asMillis() + (1000L * mAppPreferences.getPollingIntervalInSeconds());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (mPendingIntent != null) {
                        mAlarm.cancel(mPendingIntent);
                    }
                    Intent intent = new Intent(this, this.getClass());
                    intent.setAction(AppConstants.ACTION_POLLING_POLL);
                    int flags;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        flags = PendingIntent.FLAG_IMMUTABLE;
                    } else {
                        flags = 0;
                    }
                    mPendingIntent = PendingIntent.getService(this, 0, intent, flags);
                    mAlarm.setExact(AlarmManager.RTC_WAKEUP, time, mPendingIntent);
                } else {
                    mAlarm.set(AlarmManager.RTC_WAKEUP, time, mPendingIntent);
                }
            }
        }
    }
}