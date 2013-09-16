package com.sap.sailing.racecommittee.app.services.sending;

import android.content.Context;
import android.content.Intent;

import com.sap.sailing.racecommittee.app.AppConstants;
import com.sap.sailing.racecommittee.app.logging.ExLog;

public class SendDelayedEventsCaller implements Runnable {

    private final static String TAG = "SendDelayedEventsCaller";
    private Context context;

    public SendDelayedEventsCaller(Context context) {
        this.context = context;
    }

    public void run() {
        ExLog.i(TAG, "The Event Sending Service is called to send possibly delayed intents");
        Intent sendSavedIntent = new Intent(AppConstants.INTENT_ACTION_SEND_SAVED_INTENTS);
        context.startService(sendSavedIntent);
    }
}
