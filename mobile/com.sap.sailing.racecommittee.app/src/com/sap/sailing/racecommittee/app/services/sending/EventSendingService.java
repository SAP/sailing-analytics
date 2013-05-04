package com.sap.sailing.racecommittee.app.services.sending;

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.logging.ExLog;
import com.sap.sailing.racecommittee.app.receiver.ConnectivityChangedReceiver;
import com.sap.sailing.racecommittee.app.services.sending.EventSenderTask.EventSendingListener;

/*
 * Sending an event to the webservice
 * 
 * Usage:
 * 
 * Intent i = new Intent(AppConstants.SEND_EVENT_ACTION);
 * i.putExtra(AppConstants.EXTRAS_JSON_KEY, JsonUtils.getObjectAsString('someEventObject'));
 * i.putExtra(AppConstants.RACE_UUID_KEY, 'raceuuid');
 * context.startService(i);
 */
public class EventSendingService extends Service implements EventSendingListener {

    protected final static String TAG = EventSendingService.class.getName();

    private ConnectivityManager connectivityManager;
    private Handler handler;
    private final IBinder mBinder = new EventSendingBinder();
    private EventPersistenceManager persistenceManager;
    private boolean isHandlerSet;
    
    private EventSendingServiceLogger serviceLogger = new EventSendingServiceLogger() {
        @Override public void onEventSentSuccessful() { }
        @Override public void onEventSentFailed() { }
    };
    
    public interface EventSendingServiceLogger {
        public void onEventSentSuccessful();
        public void onEventSentFailed();
    }

    public void setEventSendingServiceLogger(EventSendingServiceLogger logger) {
        serviceLogger = logger;
    }

    public class EventSendingBinder extends Binder {
        public EventSendingService getService() {
            return EventSendingService.this;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    private Date lastSuccessfulSend;

    public int getDelayedIntentsCount() {
        return persistenceManager.getEventCount();
    }

    public Date getLastSuccessfulSend() {
        return lastSuccessfulSend;
    }

    /**
     * creates an intent that contains the event to be sent and the race id which shall be sent to the backend
     * 
     * @param context
     *            the context of the app
     * @param race
     *            the race for which the event was created
     * @param serializedEvent
     *            the event serizalized to JSON
     * @return the intent that shall be sent to the EventSendingService
     */
    public static Intent createEventIntent(Context context, ManagedRace race, Serializable serializedEvent) {
        String url = String.format(
                "%s/sailingserver/rc/racelog?leaderboard=%s&raceColumn=%s&fleet=%s", 
                AppConstants.getServerBaseURL(context),
                URLEncoder.encode(race.getRaceGroup().getName()),
                URLEncoder.encode(race.getName()),
                URLEncoder.encode(race.getFleet().getName()));
        
        return createEventIntent(context, url, race.getId(), serializedEvent);
    }
    
    public static Intent createEventIntent(Context context, String url, Serializable raceId, Serializable serializedEvent) {
        Intent eventIntent = new Intent(context.getString(R.string.intentActionSendEvent));
        eventIntent.putExtra(AppConstants.RACE_ID_KEY, raceId);
        eventIntent.putExtra(AppConstants.EXTRAS_SERIALIZED_EVENT, serializedEvent);
        eventIntent.putExtra(AppConstants.EXTRAS_URL, url);
        ExLog.i(TAG, "Created event " + eventIntent + " for sending to backend");
        return eventIntent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        persistenceManager = new EventPersistenceManager(this);

        handler = new Handler();
        isHandlerSet = false;

        if (persistenceManager.areIntentsDelayed()) {
            handleDelayedEvents();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            ExLog.i(TAG, "Service is restarted.");
            return START_STICKY;
        }

        ExLog.i(TAG, "Service is called by following intent: " + intent.getAction());
        handleCommand(intent, startId);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void handleCommand(Intent intent, int startId) {
        String action = intent.getAction();
        if (action.equals(getString(R.string.intentActionSendSavedIntents))) {
            handleDelayedEvents();
        } else if (action.equals(getString(R.string.intentActionSendEvent))) {
            handleSendEvents(intent);
        }
    }

    private void handleSendEvents(Intent intent) {
        ExLog.i(TAG, String.format("Trying to send an event..."));
        if (!isConnected()) {
            ExLog.i(TAG, String.format("Send aborted because there is no connection."));
            persistenceManager.persistIntent(intent);
            ConnectivityChangedReceiver.enable(this);
            serviceLogger.onEventSentFailed();
        } else {
            sendEvent(intent);
        }
    }

    private void handleDelayedEvents() {
        ExLog.i(TAG, String.format("Trying to resend stored events..."));
        
        isHandlerSet = false;
        if (!isConnected()) {
            ExLog.i(TAG, String.format("Resend aborted because there is no connection."));
            ConnectivityChangedReceiver.enable(this);
            serviceLogger.onEventSentFailed();
        } else {
            sendDelayedEvents();
        }
    }

    private void sendDelayedEvents() {
        List<Intent> delayedIntents = persistenceManager.restoreEvents();
        ExLog.i(TAG, String.format("Resending %d events...", delayedIntents.size()));
        
        for (Intent intent : delayedIntents)
            sendEvent(intent);
    }

    private void sendEvent(Intent intent) {
        if (!AppConstants.isSendingActive(this)) {
            ExLog.i(TAG, "Sending deactivated. Event will not be sent to server.");
        } else {
            EventSenderTask task = new EventSenderTask(this);
            task.execute(intent);
        }
    }

    @Override
    public void onResult(Intent intent, boolean success) {
        if (!success) {
            ExLog.w(TAG, "Error while posting intent to server. Will persist intent...");
            persistenceManager.persistIntent(intent);
            if (!isHandlerSet) {
                SendDelayedEventsCaller delayedCaller = new SendDelayedEventsCaller(this);
                handler.postDelayed(delayedCaller, 1000 * 30); //after 30 sec, try the sending again
                isHandlerSet = true;
            }
            
            serviceLogger.onEventSentFailed();
        } else {
            ExLog.i(TAG, "Event successfully send.");
            if (persistenceManager.areIntentsDelayed()) {
                persistenceManager.removeIntent(intent);
            }
            lastSuccessfulSend = Calendar.getInstance().getTime();
            
            serviceLogger.onEventSentSuccessful();
        }

    }

    /**
     * checks if there is network connectivity
     * 
     * @return connectivity check value
     */
    private boolean isConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null) {
            return false;
        }
        return activeNetwork.isConnected();
    }
}
