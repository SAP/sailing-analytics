package com.sap.sailing.racecommittee.app.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Pair;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.util.NotificationHelper;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogChangedVisitor;
import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.domain.abstractlog.race.state.RaceStateEvent;
import com.sap.sailing.domain.abstractlog.race.state.RaceStateEventScheduler;
import com.sap.sailing.domain.abstractlog.race.state.impl.RaceStateEventImpl;
import com.sap.sailing.domain.abstractlog.race.state.impl.RaceStateEvents;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.services.polling.RaceLogPollingService;
import com.sap.sailing.racecommittee.app.services.sending.RaceEventSender;
import com.sap.sailing.racecommittee.app.ui.activities.LoginActivity;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogEventSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class RaceStateService extends Service {

    private final static String TAG = RaceStateService.class.getName();

    private ReadonlyDataManager dataManager;
    private Map<ManagedRace, RaceLogEventVisitor> registeredLogListeners;
    private Map<ManagedRace, RaceStateEventScheduler> registeredStateEventSchedulers;
    private EventScheduler eventScheduler;

    /**
     * Abstracts the differences between different Android versions when it comes to scheduling
     * an event to occur at a certain point in time. On older Android versions, an AlarmManager
     * is a useful way, and depending on the detailed age of the version,
     * {@link AlarmManager#setExact} or {@link AlarmManager#set} will have to be used, as this
     * has implications for the permissions the app needs to have.</p>
     *
     * On newer versions of Android (starting with Android 13), a {@link Handler#postAtTime}
     * call is to be preferred. Alarms can be canceled again using the handle returned by
     * the method scheduling it.
     */
    private interface EventScheduler {
        void registerRace(ManagedRace race);

        boolean isRaceRegistered(ManagedRace race);

        void cancelAllEvents();

        void cancelAllEventsForRace(String id);

        void schedule(ManagedRace race, RaceStateEvent event);

        int getNumberOfMonitoredRaces();

        void clearByEventName(ManagedRace race, RaceStateEvents stateEventName);

        void clearAllAlarms(String raceId);
    }

    private abstract class AlarmManagerEventScheduler implements EventScheduler {

        private final AlarmManager alarmManager;
        private int alarmManagerRequestCode;

        /**
         * keys are the race IDs
         */
        private final Map<String, List<Pair<PendingIntent, RaceStateEvents>>> managedIntents;

        protected AlarmManagerEventScheduler() {
            this.alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            this.managedIntents = new HashMap<>();
            this.alarmManagerRequestCode = 0;
        }

        protected PendingIntent createAlarmPendingIntent(ManagedRace managedRace, RaceStateEvent event) {
            Intent intent = new Intent().setClass(RaceStateService.this, RaceStateService.class);
            intent.setAction(AppConstants.ACTION_ALARM_ACTION);
            intent.putExtra(AppConstants.EXTRA_RACE_ID, managedRace.getId());
            intent.putExtra(AppConstants.EXTRA_TIME_POINT_MILLIS, event.getTimePoint().asMillis());
            intent.putExtra(AppConstants.EXTRA_EVENT_NAME, event.getEventName().name());
            int flags;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
            } else {
                flags = PendingIntent.FLAG_UPDATE_CURRENT;
            }
            return PendingIntent.getService(RaceStateService.this, alarmManagerRequestCode++, intent, flags);
        }

        protected List<Pair<PendingIntent, RaceStateEvents>> getManagedIntents(ManagedRace race) {
            return managedIntents.get(race.getId());
        }

        protected AlarmManager getAlarmManager() {
            return alarmManager;
        }

        @Override
        public void cancelAllEvents() {
            for (List<Pair<PendingIntent, RaceStateEvents>> intents : managedIntents.values()) {
                for (Pair<PendingIntent, RaceStateEvents> intentPair : intents) {
                    alarmManager.cancel(intentPair.first);
                }
            }
            managedIntents.clear();
        }

        @Override
        public void cancelAllEventsForRace(String raceId) {
            List<Pair<PendingIntent, RaceStateEvents>> intents = managedIntents.get(raceId);
            if (intents != null) {
                for (Pair<PendingIntent, RaceStateEvents> intentPair : intents) {
                    alarmManager.cancel(intentPair.first);
                }
                managedIntents.remove(raceId);
            } else {
                ExLog.w(RaceStateService.this, TAG, "Couldn't find any managed intents for race " + raceId);
            }
        }

        @Override
        public void clearByEventName(ManagedRace race, RaceStateEvents stateEventName) {
            final List<Pair<PendingIntent, RaceStateEvents>> intents = managedIntents.get(race.getId());
            Pair<PendingIntent, RaceStateEvents> toBeRemoved = null;
            if (intents != null) {
                for (Pair<PendingIntent, RaceStateEvents> intentPair : intents) {
                    if (intentPair.second.equals(stateEventName)) {
                        toBeRemoved = intentPair;
                        break;
                    }
                }
            }
            if (toBeRemoved != null) {
                alarmManager.cancel(toBeRemoved.first);
                intents.remove(toBeRemoved);
                ExLog.i(RaceStateService.this, TAG, String.format("Removed alarm for event named %s.", stateEventName));
            } else {
                ExLog.i(RaceStateService.this, TAG, String.format("Unable to remove alarm for event named %s (not found).", stateEventName));
            }
        }

        @Override
        public int getNumberOfMonitoredRaces() {
            return managedIntents.size();
        }

        @Override
        public void registerRace(ManagedRace race) {
            managedIntents.put(race.getId(), new ArrayList<>());
        }

        @Override
        public boolean isRaceRegistered(ManagedRace race) {
            return managedIntents.containsKey(race.getId());
        }

        @Override
        public void clearAllAlarms(String raceId) {
            List<Pair<PendingIntent, RaceStateEvents>> intents = managedIntents.get(raceId);
            if (intents == null) {
                ExLog.w(RaceStateService.this, TAG, "There are no intents for race " + raceId);
                return;
            }
            for (Pair<PendingIntent, RaceStateEvents> intentPair : intents) {
                alarmManager.cancel(intentPair.first);
            }
            intents.clear();
            ExLog.w(RaceStateService.this, TAG, "All intents cleared for race " + raceId);
        }
    }

    private class ExactAlarmManagerEventScheduler extends AlarmManagerEventScheduler {
        @Override
        public void schedule(ManagedRace race, RaceStateEvent event) {
            final PendingIntent intent = createAlarmPendingIntent(race, event);
            final List<Pair<PendingIntent, RaceStateEvents>> intents = getManagedIntents(race);
            if (intents != null) {
                intents.add(Pair.create(intent, event.getEventName()));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                getAlarmManager().setExact(AlarmManager.RTC_WAKEUP, event.getTimePoint().asMillis(), intent);
            }
            ExLog.i(RaceStateService.this, TAG, "The alarm " + event.getEventName() + " will be fired at " + event.getTimePoint());
        }

    }

    private class NonExactAlarmManagerEventScheduler extends AlarmManagerEventScheduler {
        @Override
        public void schedule(ManagedRace race, RaceStateEvent event) {
            PendingIntent intent = createAlarmPendingIntent(race, event);
            final List<Pair<PendingIntent, RaceStateEvents>> intents = getManagedIntents(race);
            if (intents != null) {
                intents.add(Pair.create(intent, event.getEventName()));
            }
            getAlarmManager().set(AlarmManager.RTC_WAKEUP, event.getTimePoint().asMillis(), intent);
            ExLog.i(RaceStateService.this, TAG, "The alarm " + event.getEventName() + " will be fired at " + event.getTimePoint());
        }
    }

    private class HandlerEventScheduler implements EventScheduler {
        private final Handler mHandler;
        /**
         * keys are race IDs
         */
        private final Map<String, Set<RaceStateEvents>> alarmsToRun;

        public HandlerEventScheduler() {
            alarmsToRun = new HashMap<>();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mHandler = Handler.createAsync(Looper.getMainLooper());
            } else {
                throw new RuntimeException("This constructor must only be invoked for build versions >= "+Build.VERSION_CODES.P);
            }
        }

        @Override
        public void schedule(ManagedRace race, RaceStateEvent event) {
            Util.addToValueSet(alarmsToRun, race.getId(), event.getEventName());
            final long inHowManyMillis = TimePoint.now().until(event.getTimePoint()).asMillis();
            mHandler.postAtTime(()->{
                final Set<RaceStateEvents> alarmsToRunForRace = alarmsToRun.get(race.getId());
                if (alarmsToRunForRace != null && alarmsToRunForRace.contains(event.getEventName())) {
                    processAlarm(race, event);
                }
            }, SystemClock.uptimeMillis() + inHowManyMillis);
            ExLog.i(RaceStateService.this, TAG, "The alarm " + event.getEventName() + " will be fired at " + event.getTimePoint());
        }

        @Override
        public void registerRace(ManagedRace race) {
            if (!alarmsToRun.containsKey(race.getId())) {
                alarmsToRun.put(race.getId(), new HashSet<>());
            }
        }

        @Override
        public boolean isRaceRegistered(ManagedRace race) {
            return alarmsToRun.containsKey(race.getId());
        }

        @Override
        public void cancelAllEventsForRace(String id) {
            alarmsToRun.remove(id);
        }

        @Override
        public void cancelAllEvents() {
            alarmsToRun.clear();
        }

        @Override
        public int getNumberOfMonitoredRaces() {
            return alarmsToRun.size();
        }

        @Override
        public void clearByEventName(ManagedRace race, RaceStateEvents stateEventName) {
            final Set<RaceStateEvents> alarmsForRace = alarmsToRun.get(race.getId());
            if (alarmsForRace != null) {
                alarmsForRace.remove(stateEventName);
            }
        }

        @Override
        public void clearAllAlarms(String raceId) {
            final Set<RaceStateEvents> alarmsForRace = alarmsToRun.get(raceId);
            if (alarmsForRace != null) {
                alarmsForRace.clear();
            }
        }
    }

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            eventScheduler = new HandlerEventScheduler();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            eventScheduler = new ExactAlarmManagerEventScheduler();
        } else {
            eventScheduler = new NonExactAlarmManagerEventScheduler();
        }
        this.dataManager = DataManager.create(this);

        this.registeredLogListeners = new HashMap<>();
        this.registeredStateEventSchedulers = new HashMap<>();
        ExLog.i(this, TAG, "Started.");
    }

    private Notification setupNotification(String customContent) {
        // Starting in Android 8.0 (API level 26), all notifications must be assigned to a channel
        CharSequence name = getText(R.string.service_info);
        NotificationHelper.createNotificationChannel(this, NotificationHelper.getNotificationChannelId(), name);

        Intent launcherIntent = new Intent(this, LoginActivity.class);
        launcherIntent.setAction(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        int flags;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = PendingIntent.FLAG_IMMUTABLE;
        } else {
            flags = 0;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launcherIntent, flags);
        CharSequence title = getText(R.string.service_info);
        String content = customContent != null ? customContent : getString(R.string.service_text_no_races);
        int color = getResources().getColor(R.color.constant_sap_blue_1);
        return NotificationHelper.getNotification(this, NotificationHelper.getNotificationChannelId(), title, content, contentIntent, color);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            ExLog.i(this, TAG, "Restarted.");
        } else {
            handleStartCommand(intent, startId);
        }

        // We want this service to continue running until it is explicitly
        // stopped, therefore return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterAllRaces();
        stopForeground(true); // TODO is this still needed if we don't call startForeground anymore?
    }

    private void handleStartCommand(Intent intent, int startId) {
        String action = intent.getAction();
        ExLog.i(this, TAG, String.format("Command action '%s' received.", action));

        if (action == null) {
            return;
        }

        if (AppConstants.ACTION_CLEAR_RACES.equals(action)) {
            handleClearRaces();
            stopSelf(startId);
        } else {
            String id = intent.getStringExtra(AppConstants.EXTRA_RACE_ID);
            ManagedRace race = dataManager.getDataStore().getRace(id);
            if (race == null) {
                ExLog.w(this, TAG, "No race for id " + id);
                return;
            }

            switch (action) {
                case AppConstants.ACTION_REGISTER_RACE:
                    registerRace(race);
                    break;
                case AppConstants.ACTION_UNREGISTER_RACE:
                    unregisterRace(race);
                    break;
                case AppConstants.ACTION_ALARM_ACTION:
                    long timePoint = intent.getLongExtra(AppConstants.EXTRA_TIME_POINT_MILLIS, 0);
                    String eventName = intent.getStringExtra(AppConstants.EXTRA_EVENT_NAME);
                    RaceStateEvent event = new RaceStateEventImpl(new MillisecondsTimePoint(timePoint),
                            RaceStateEvents.valueOf(eventName));
                    processAlarm(race, event);
                    break;
            }
        }
    }

    private void unregisterAllRaces() {
        Intent intent = new Intent(this, RaceLogPollingService.class);
        intent.setAction(AppConstants.ACTION_POLLING_STOP);
        startService(intent);
        for (Entry<ManagedRace, RaceLogEventVisitor> entry : registeredLogListeners.entrySet()) {
            entry.getKey().getState().getRaceLog().removeListener(entry.getValue());
        }
        registeredLogListeners.clear();
        for (Entry<ManagedRace, RaceStateEventScheduler> entry : registeredStateEventSchedulers.entrySet()) {
            entry.getKey().getState().setStateEventScheduler(null);
        }
        registeredStateEventSchedulers.clear();
        eventScheduler.cancelAllEvents();
        ExLog.i(this, TAG, "All races unregistered.");
    }

    private void unregisterRace(ManagedRace race) {
        Intent intent = new Intent(this, RaceLogPollingService.class);
        intent.setAction(AppConstants.ACTION_POLLING_RACE_REMOVE);
        intent.putExtra(AppConstants.EXTRA_RACE_ID, race.getId());
        startService(intent);
        race.getState().getRaceLog().removeAllListeners();
        registeredLogListeners.remove(race);
        race.getState().setStateEventScheduler(null);
        registeredStateEventSchedulers.remove(race);
        eventScheduler.cancelAllEventsForRace(race.getId());
        ExLog.i(this, TAG, "Race " + race.getId() + " unregistered");
        updateNotification();
    }

    private void unregisterRace(@Nullable String raceId) {
        ManagedRace raceToUnregister = null;
        for (ManagedRace race : registeredStateEventSchedulers.keySet()) {
            if (race.getId().equals(raceId)) {
                raceToUnregister = race;
            }
        }

        if (raceToUnregister != null) {
            unregisterRace(raceToUnregister);
        }
    }

    private void handleClearRaces() {
        unregisterAllRaces();
        ExLog.i(this, TAG, "handleClearRaces: Cleared all races.");
    }

    private void updateNotification() {
        int numRaces = eventScheduler.getNumberOfMonitoredRaces();
        String content = getString(R.string.service_text_num_races, numRaces);
        Notification notification = setupNotification(content);
        NotificationManagerCompat.from(getApplicationContext()).notify(NotificationHelper.getNotificationId(), notification);
    }

    private void registerRace(ManagedRace race) {
        ExLog.i(this, TAG, "Trying to register race " + race.getId());
        if (!eventScheduler.isRaceRegistered(race)) {
            RaceState state = race.getState();
            eventScheduler.registerRace(race);
            // Register on event additions...
            JsonSerializer<RaceLogEvent> eventSerializer = RaceLogEventSerializer
                    .create(new CompetitorJsonSerializer());
            RaceEventSender sender = new RaceEventSender(this, eventSerializer, race);
            RaceLogChangedVisitor logListener = new RaceLogChangedVisitor(sender);
            state.getRaceLog().addListener(logListener);
            // ... register on state changes...
            RaceStateEventScheduler stateEventScheduler = new RaceStateEventSchedulerOnService(this, race);
            state.setStateEventScheduler(stateEventScheduler);
            // ... and register for polling!
            Intent intent = new Intent(this, RaceLogPollingService.class);
            intent.setAction(AppConstants.ACTION_POLLING_RACE_ADD);
            intent.putExtra(AppConstants.EXTRA_RACE_ID, race.getId());
            startService(intent);
            registeredLogListeners.put(race, logListener);
            registeredStateEventSchedulers.put(race, stateEventScheduler);
            ExLog.i(this, TAG, "Race " + race.getId() + " registered.");
        } else {
            ExLog.w(this, TAG, "Race " + race.getId() + " was already registered. Cleaning up.");
            unregisterRace(race.getId());
            registerRace(race);
        }
        updateNotification();
    }

    void setAlarm(ManagedRace race, RaceStateEvent event) {
        eventScheduler.schedule(race, event);
    }

    private void processAlarm(ManagedRace race, RaceStateEvent event) {
        ExLog.i(this, TAG, String.format("Processing %s", event));
        race.getState().processStateEvent(event);
        clearAlarmByName(race, event.getEventName());
    }

    void clearAlarmByName(ManagedRace race, RaceStateEvents stateEventName) {
        eventScheduler.clearByEventName(race, stateEventName);
    }

    void clearAllAlarms(ManagedRace race) {
        eventScheduler.clearAllAlarms(race.getId());
    }
}
