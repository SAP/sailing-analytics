package com.sap.sailing.racecommittee.app.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Pair;

import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogChangedVisitor;
import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.domain.abstractlog.race.state.RaceStateEvent;
import com.sap.sailing.domain.abstractlog.race.state.RaceStateEventScheduler;
import com.sap.sailing.domain.abstractlog.race.state.impl.RaceStateEvents;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.DataStore;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.services.polling.RaceLogPollerService;
import com.sap.sailing.racecommittee.app.services.sending.RaceEventSender;
import com.sap.sailing.racecommittee.app.ui.activities.LoginActivity;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogEventSerializer;

public class RaceStateService extends Service {

    private final static String TAG = RaceStateService.class.getName();

    /**
     * Binder for this {@link RaceStateService}.
     */
    public class RaceStateServiceBinder extends Binder {
        public RaceStateService getService() {
            return RaceStateService.this;
        }
    }

    private final static String EXTRAS_SERVICE_ID = RaceStateService.class.getName() + ".serviceId";
    private final static int NOTIFICATION_ID = 42;

    private final IBinder mBinder = new RaceStateServiceBinder();

    private UUID serviceId;

    private AlarmManager alarmManager;

    private int alarmManagerRequestCode = 0;

    private ReadonlyDataManager dataManager;

    private Map<ManagedRace, RaceLogEventVisitor> registeredLogListeners;
    private Map<ManagedRace, RaceStateEventScheduler> registeredStateEventSchedulers;

    private Map<String, List<Pair<PendingIntent, RaceStateEvents>>> managedIntents;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    @Override
    public void onCreate() {
        this.serviceId = UUID.randomUUID();
        this.alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        this.dataManager = DataManager.create(this);

        this.registeredLogListeners = new HashMap<>();
        this.registeredStateEventSchedulers = new HashMap<>();
        this.managedIntents = new HashMap<>();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        setupNotificationBuilder();

        super.onCreate();
        ExLog.i(this, TAG, "Started.");
    }

    private void setupNotificationBuilder() {
        Intent launcherIntent = new Intent(this, LoginActivity.class);
        launcherIntent.setAction(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launcherIntent, 0);
        notificationBuilder = new NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_boat_white_24dp)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
            .setContentTitle(getText(R.string.service_info))
            .setContentText(getString(R.string.service_text_no_races))
            .setContentIntent(contentIntent)
            .setColor(getResources().getColor(R.color.constant_sap_blue_1))
            .setOngoing(true);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            ExLog.i(this, TAG, "Restarted.");
        } else {
            handleStartCommand(intent);
        }

        // We want this service to continue running until it is explicitly
        // stopped, therefore return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterAllRaces();

        // ... and remove from status bar!
        stopForeground(true);
        super.onDestroy();
    }

    private void unregisterAllRaces() {
        Intent intent = new Intent(this, RaceLogPollerService.class);
        intent.setAction(AppConstants.INTENT_ACTION_POLLER_STOP);
        startService(intent);

        for (Entry<ManagedRace, RaceLogEventVisitor> entry : registeredLogListeners.entrySet()) {
            entry.getKey().getState().getRaceLog().removeListener(entry.getValue());
        }
        registeredLogListeners.clear();

        for (Entry<ManagedRace, RaceStateEventScheduler> entry : registeredStateEventSchedulers.entrySet()) {
            entry.getKey().getState().setStateEventScheduler(null);
        }
        registeredStateEventSchedulers.clear();

        for (List<Pair<PendingIntent, RaceStateEvents>> intents : managedIntents.values()) {
            for (Pair<PendingIntent,RaceStateEvents> intentPair : intents) {
                alarmManager.cancel(intentPair.first);
            }
        }
        managedIntents.clear();

        ExLog.i(this, TAG, "All races unregistered.");
    }

    private void unregisterRace(ManagedRace race) {
        Intent intent = new Intent(this, RaceLogPollerService.class);
        intent.setAction(AppConstants.INTENT_ACTION_POLLER_RACE_REMOVE);
        intent.putExtra(AppConstants.INTENT_ACTION_EXTRA, race.getId());
        startService(intent);

        race.getState().getRaceLog().removeAllListeners();
        registeredLogListeners.remove(race);

        race.getState().setStateEventScheduler(null);
        registeredStateEventSchedulers.remove(race);

        List<Pair<PendingIntent, RaceStateEvents>> intents = managedIntents.get(race.getId());
        for (Pair<PendingIntent, RaceStateEvents> intentPair : intents) {
            alarmManager.cancel(intentPair.first);
        }
        managedIntents.remove(race.getId());

        ExLog.i(this, TAG, "Race " + race.getId() + " unregistered");
    }

    private void unregisterRace(@Nullable String raceId) {
        ManagedRace raceToUnregister = null;
        for (ManagedRace race: registeredStateEventSchedulers.keySet()) {
            if (race.getId().equals(raceId)) {
                raceToUnregister = race;
            }
        }

        if (raceToUnregister != null) {
            unregisterRace(raceToUnregister);
        }
    }

    private void handleStartCommand(Intent intent) {
        String action = intent.getAction();
        ExLog.i(this, TAG, String.format("Command action '%s' received.", action));

        if (AppConstants.INTENT_ACTION_CLEAR_RACES.equals(action)) {
            handleClearRaces();
            return;
        }

        if (AppConstants.INTENT_ACTION_CLEANUP_RACES.equals(action)) {
            handleCleanupRaces();
            return;
        }

        if (AppConstants.INTENT_ACTION_REGISTER_RACE.equals(action)) {
            handleRegisterRace(intent);
            return;
        }

        if (AppConstants.INTENT_ACTION_UNREGISTER_RACE.equals(action)) {
            handleUnregisterRace(intent);
            return;
        }

        if (!serviceId.equals(intent.getSerializableExtra(EXTRAS_SERVICE_ID))) {
            ExLog.w(this, TAG, "Received event for different service version.");
            return;
        }

        String id = intent.getStringExtra(AppConstants.RACE_ID_KEY);
        ManagedRace race = dataManager.getDataStore().getRace(id);
        if (race == null) {
            ExLog.w(this, TAG, "No race for id " + id);
            return;
        }

        if (AppConstants.INTENT_ACTION_ALARM_ACTION.equals(action)) {
            RaceStateEvent stateEvent = (RaceStateEvent) intent.getExtras().getSerializable(AppConstants.EXTRAS_RACE_STATE_EVENT);
            ExLog.i(this, TAG, String.format("Processing %s", stateEvent.toString()));
            race.getState().processStateEvent(stateEvent);
            clearAlarmByName(race, stateEvent.getEventName());
            return;
        }
    }

    private void handleClearRaces() {
        unregisterAllRaces();
        DataStore dataStore = dataManager.getDataStore();
        dataStore.getRaces().clear();
        dataStore.setEventUUID(null);
        dataStore.setCourseUUID(null);
        ExLog.i(this, TAG, "handleClearRaces: Cleared all races.");
        stopForeground(true);
    }

    private class RemoveRaceAction implements Runnable {

        private final ManagedRace managedRace;

        public RemoveRaceAction(@NonNull ManagedRace race) {
            managedRace = race;
        }

        @Override
        public void run() {
            unregisterRace(managedRace);
        }
    }

    private void handleCleanupRaces() {
        Set<RemoveRaceAction> actions = new HashSet<>();
        for (String raceId : managedIntents.keySet()) {
            if (!inDataStore(raceId)) {
                for (ManagedRace race : registeredLogListeners.keySet()) {
                    if (race.getId().equals(raceId)) {
                        actions.add(new RemoveRaceAction(race));
                    }
                }
            }
        }
        for (RemoveRaceAction action : actions) {
            action.run();
        }
        updateNotification();
    }

    private boolean inDataStore(String raceId) {
        for (ManagedRace race : dataManager.getDataStore().getRaces()) {
            if (race.getId().equals(raceId)) {
                return true;
            }
        }
        return false;
    }

    private void handleRegisterRace(Intent intent) {
        ManagedRace race = getRaceFromIntent(intent);
        if (race == null) {
            ExLog.w(this, TAG, "Intent did not carry valid race information.");
            return;
        }
        registerRace(race);

        updateNotification();
    }

    private void handleUnregisterRace(Intent intent) {
        ManagedRace race = getRaceFromIntent(intent);
        if (race == null) {
            ExLog.w(this, TAG, "Intent did not carry valid race information");
            return;
        }
        unregisterRace(race);

        updateNotification();
    }

    private void updateNotification() {
        int numRaces = managedIntents.keySet().size();
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder
                .setContentText(getString(R.string.service_text_num_races, numRaces))
                .build());
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    private ManagedRace getRaceFromIntent(Intent intent) {
        if (intent.getExtras() == null || !intent.getExtras().containsKey(AppConstants.RACE_ID_KEY)) {
            return null;
        }
        String raceId = intent.getExtras().getString(AppConstants.RACE_ID_KEY);
        return dataManager.getDataStore().getRace(raceId);
    }

    private void registerRace(final ManagedRace race) {
        ExLog.i(this, TAG, "Trying to register race " + race.getId());

        if (!managedIntents.containsKey(race.getId())) {
            RaceState state = race.getState();
            managedIntents.put(race.getId(), new ArrayList<Pair<PendingIntent, RaceStateEvents>>());

            // Register on event additions...
            JsonSerializer<RaceLogEvent> eventSerializer = RaceLogEventSerializer.create(new CompetitorJsonSerializer());
            RaceEventSender sender = new RaceEventSender(this, eventSerializer, race);
            RaceLogChangedVisitor logListener = new RaceLogChangedVisitor(sender);
            state.getRaceLog().addListener(logListener);

            // ... register on state changes...
            RaceStateEventScheduler stateEventScheduler = new RaceStateEventSchedulerOnService(this, race);
            state.setStateEventScheduler(stateEventScheduler);

            // ... and register for polling!
            Intent intent = new Intent(this, RaceLogPollerService.class);
            intent.setAction(AppConstants.INTENT_ACTION_POLLER_RACE_ADD);
            intent.putExtra(AppConstants.INTENT_ACTION_EXTRA, race.getId());
            startService(intent);

            registeredLogListeners.put(race, logListener);
            registeredStateEventSchedulers.put(race, stateEventScheduler);

            ExLog.i(this, TAG, "Race " + race.getId() + " registered.");
        } else {
            ExLog.w(this, TAG, "Race " + race.getId() + " was already registered. Cleaning up.");
            unregisterRace(race.getId());
            registerRace(race);
        }
    }

    private PendingIntent createAlarmPendingIntent(ManagedRace managedRace, RaceStateEvent event) {
        Intent intent = new Intent(this, RaceStateService.class);
        intent.setAction(AppConstants.INTENT_ACTION_ALARM_ACTION);
        intent.putExtra(EXTRAS_SERVICE_ID, serviceId.toString());
        intent.putExtra(AppConstants.RACE_ID_KEY, managedRace.getId());
        intent.putExtra(AppConstants.EXTRAS_RACE_STATE_EVENT, event);
        return PendingIntent.getService(this, alarmManagerRequestCode++, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void setAlarm(ManagedRace race, RaceStateEvent event) {
        PendingIntent intent = createAlarmPendingIntent(race, event);
        managedIntents.get(race.getId()).add(Pair.create(intent, event.getEventName()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, event.getTimePoint().asMillis(), intent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, event.getTimePoint().asMillis(), intent);
        }
        ExLog.i(this, TAG, "The alarm " + event.getEventName() + " will be fired at " + event.getTimePoint());
    }

    public void clearAlarmByName(ManagedRace race, RaceStateEvents stateEventName) {
        List<Pair<PendingIntent, RaceStateEvents>> intents = managedIntents.get(race.getId());
        Pair<PendingIntent, RaceStateEvents> toBeRemoved = null;
        for (Pair<PendingIntent, RaceStateEvents> intentPair : intents) {
            if (intentPair.second.equals(stateEventName)) {
                toBeRemoved = intentPair;
                break;
            }
        }
        if (toBeRemoved != null) {
            alarmManager.cancel(toBeRemoved.first);
            intents.remove(toBeRemoved);
            ExLog.i(this, TAG, String.format("Removed alarm for event named %s.", stateEventName));
        } else {
            ExLog.i(this, TAG, String.format("Unable to remove alarm for event named %s (not found).", stateEventName));
        }
    }

    public void clearAllAlarms(ManagedRace race) {
        List<Pair<PendingIntent, RaceStateEvents>> intents = managedIntents.get(race.getId());

        if (intents == null) {
            ExLog.w(this, TAG, "There are no intents for race " + race.getId());
            return;
        }

        for (Pair<PendingIntent, RaceStateEvents> intentPair : intents) {
            alarmManager.cancel(intentPair.first);
        }

        intents.clear();
        ExLog.w(this, TAG, "All intents cleared for race " + race.getId());
    }
}
