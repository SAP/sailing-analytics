package com.sap.sailing.android.buoy.positioning.app.util;

import java.io.InputStream;

import com.sap.sailing.android.buoy.positioning.app.R;
import com.sap.sailing.android.shared.services.sending.ServerReplyCallback;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

public class PingServerReplyCallback implements ServerReplyCallback {

    private static final String TAG = PingServerReplyCallback.class.getName();

    // Empty constructor for MessageSendingService
    public PingServerReplyCallback() {

    }

    @Override
    public void processResponse(Context context, InputStream inputStream, String callbackPayload) {
        Log.d(TAG, "Context origin:");
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(new Intent(context.getString(R.string.ping_reached_server)));
    }
}
