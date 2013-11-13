package com.sap.sailing.racecommittee.app.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventVisitor;
import com.sap.sailing.domain.racelog.impl.RaceLogChangedVisitor;
import com.sap.sailing.domain.racelog.state.RaceState2;
import com.sap.sailing.domain.racelog.state.RaceStateEvent;
import com.sap.sailing.domain.racelog.state.RaceStateEventScheduler;
import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.DataManager;
import com.sap.sailing.racecommittee.app.data.ReadonlyDataManager;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.logging.ExLog;
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
    
    private Map<Serializable, List<PendingIntent>> managedIntents;
    
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    @Override
    public void onCreate() {
        this.serviceId = UUID.randomUUID();
        this.alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        this.dataManager = DataManager.create(this);
        
        this.registeredLogListeners = new HashMap<ManagedRace, RaceLogEventVisitor>();
        this.registeredStateEventSchedulers = new HashMap<ManagedRace, RaceStateEventScheduler>();
        this.managedIntents = new HashMap<Serializable, List<PendingIntent>>();
        
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        setupNotificationBuilder();
        
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
        super.onCreate();
        ExLog.i(TAG, "Started.");
    }

    private void setupNotificationBuilder() {
        Intent launcherIntent = new Intent(this, LoginActivity.class);
        launcherIntent.setAction(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launcherIntent, 0);
        notificationBuilder = new NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.sap_sailing_app_icon)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.sap_sailing_app_icon))
            .setContentTitle(getText(R.string.service_info))
            .setContentText(getText(R.string.service_text_no_races))
            .setContentIntent(contentIntent)
            .setOngoing(true);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            ExLog.i(TAG, "Restarted.");
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
        for (Entry<ManagedRace, RaceLogEventVisitor> entry : registeredLogListeners.entrySet()) {
            entry.getKey().getState2().getRaceLog().removeListener(entry.getValue());
        }
        
        for (Entry<ManagedRace, RaceStateEventScheduler> entry : registeredStateEventSchedulers.entrySet()) {
            entry.getKey().getState2().setStateEventScheduler(null);
        }
        
        for (List<PendingIntent> intents : managedIntents.values()) {
            for (PendingIntent intent : intents) {
                alarmManager.cancel(intent);
            }
        }
        managedIntents.clear();
        
        ExLog.i(TAG, "All races unregistered.");
    }

    private void handleStartCommand(Intent intent) {
        String action = intent.getAction();
        ExLog.i(TAG, String.format("Command action '%s' received.", action));
        
        if (AppConstants.INTENT_ACTION_CLEAR_RACES.equals(action)) {
            handleClearRaces(intent);
            return;
        }
        
        if (AppConstants.INTENT_ACTION_REGISTER_RACE.equals(action)) {
            handleRegisterRace(intent);
            return;
        }
        
        if (!serviceId.equals(intent.getSerializableExtra(EXTRAS_SERVICE_ID))) {
            ExLog.w(TAG, "Received event for different service version.");
            return;
        }
        
        Serializable id = intent.getSerializableExtra(AppConstants.RACE_ID_KEY);
        ManagedRace race = dataManager.getDataStore().getRace(id);
        if (race == null) {
            ExLog.w(TAG, "No race for id " + id);
            return;
        }
        
        if (AppConstants.INTENT_ACTION_ALARM_ACTION.equals(action)) {
            RaceStateEvent stateEvent = (RaceStateEvent) intent.getExtras().getSerializable(AppConstants.EXTRAS_RACE_STATE_EVENT);
            race.getState2().processStateEvent(stateEvent);
            managedIntents.get(race.getId()).remove(intent);
            return;
        }
    }

    private void handleClearRaces(Intent intent) {
        unregisterAllRaces();
        clearAllRaces();
        
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder
                .setNumber(0)
                .setContentText(getString(R.string.service_text_no_races))
                .build());
    }

    private void clearAllRaces() {
        dataManager.getDataStore().getRaces().clear();
        ExLog.i(TAG, "Cleared all races.");
    }

    private void handleRegisterRace(Intent intent) {
        ManagedRace race = getRaceFromIntent(intent);
        if (race == null) {
            ExLog.i(TAG, "Intent did not carry valid race information.");
            return;
        }
        registerRace(race);
        
        int numRaces = managedIntents.keySet().size();
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder
                .setNumber(numRaces)
                .setContentText(String.format(getString(R.string.service_text_num_races), numRaces))
                .build());
    }

    private ManagedRace getRaceFromIntent(Intent intent) {
        if (intent.getExtras() == null || !intent.getExtras().containsKey(AppConstants.RACE_ID_KEY)) {
            return null;
        }

        Serializable raceId = intent.getExtras().getSerializable(AppConstants.RACE_ID_KEY);
        return dataManager.getDataStore().getRace(raceId);
    }

    private void registerRace(final ManagedRace race) {
        ExLog.i(TAG, "Trying to register race " + race.getId());
        
        if (!managedIntents.containsKey(race.getId())) {
            RaceState2 state = race.getState2();
            managedIntents.put(race.getId(), new ArrayList<PendingIntent>());

            // Register on event additions...
            JsonSerializer<RaceLogEvent> eventSerializer = RaceLogEventSerializer.create(new CompetitorJsonSerializer());
            RaceEventSender sender = new RaceEventSender(this, eventSerializer, race);
            RaceLogChangedVisitor logListener = new RaceLogChangedVisitor(sender);
            state.getRaceLog().addListener(logListener);

            // ... register on state changes!
            RaceStateEventScheduler stateEventScheduler = new RaceStateEventSchedulerOnService(this, race);
            state.setStateEventScheduler(stateEventScheduler);

            this.registeredLogListeners.put(race, logListener);
            this.registeredStateEventSchedulers.put(race, stateEventScheduler);
            
            ExLog.i(TAG, "Race " + race.getId() + " registered.");
        } else {
            ExLog.w(TAG, "Race " + race.getId() + " was already registered. Ignoring.");
        }
    }

    private PendingIntent createAlarmPendingIntent(ManagedRace managedRace, RaceStateEvent event) {
        Intent intent = new Intent(AppConstants.INTENT_ACTION_ALARM_ACTION);
        intent.putExtra(EXTRAS_SERVICE_ID, serviceId);
        intent.putExtra(AppConstants.RACE_ID_KEY, managedRace.getId());
        intent.putExtra(AppConstants.EXTRAS_RACE_STATE_EVENT, event);
        return PendingIntent.getService(this, alarmManagerRequestCode++, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    
    public void setAlarm(ManagedRace race, RaceStateEvent event) {
        PendingIntent intent = createAlarmPendingIntent(race, event);
        managedIntents.get(race.getId()).add(intent);
        alarmManager.set(AlarmManager.RTC_WAKEUP, event.getTimePoint().asMillis(), intent);
        ExLog.i(TAG, "The alarm " + event.getEventName() + " will be fired at " + event.getTimePoint());
    }

    public void clearAlarm(ManagedRace race, RaceStateEvent event) {
        PendingIntent intent = createAlarmPendingIntent(race, event);
        List<PendingIntent> intents = managedIntents.get(race.getId());
        intents.remove(intent);
        alarmManager.cancel(intent);
    }
    
    public void clearAllAlarms(ManagedRace race) {
        Serializable raceId = race.getId();
        List<PendingIntent> intents = managedIntents.get(raceId);
        
        if (intents == null) {
            ExLog.w(TAG, "There are no intents for race " + raceId);
            return;
        }
        
        for (PendingIntent pendingIntent : intents) {
            alarmManager.cancel(pendingIntent);
        }
        
        intents.clear();
        ExLog.w(TAG, "All intents cleared for race " + raceId);
    }
}
