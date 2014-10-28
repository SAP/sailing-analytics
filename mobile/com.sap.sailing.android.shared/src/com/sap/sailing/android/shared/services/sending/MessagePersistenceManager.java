package com.sap.sailing.android.shared.services.sending;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.sap.sailing.android.shared.data.database.MessageTable;
import com.sap.sailing.android.shared.logging.ExLog;
import com.sap.sailing.android.shared.provider.DatabaseProvider;

public class MessagePersistenceManager {

    private final static String TAG = MessagePersistenceManager.class.getName();

    protected Context context;
    protected ContentResolver contentResolver;

    private final MessageRestorer messageRestorer;

    public MessagePersistenceManager(Context context, MessageRestorer messageRestorer) {
        this.context = context;
        this.messageRestorer = messageRestorer;
        this.contentResolver = context.getContentResolver();
    }

    public boolean areIntentsDelayed() {
        return getMessageCount() != 0;
    }

    public void persistIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        String callbackPayload = extras.getString(MessageSendingService.CALLBACK_PAYLOAD);
        String url = extras.getString(MessageSendingService.URL);
        String payload = extras.getString(MessageSendingService.PAYLOAD);
        String callbackClass = extras.getString(MessageSendingService.CALLBACK_CLASS);
        persistMessage(url, callbackPayload, payload, callbackClass);
    }

    private void persistMessage(String url, String callbackPayload, String payload, String callbackClass) {
        ContentValues values = new ContentValues();
        values.put(MessageTable.URL, url);
        values.put(MessageTable.CALLBACK_PAYLOAD, callbackPayload);
        values.put(MessageTable.PAYLOAD, payload);
        values.put(MessageTable.CALLBACK_CLASS_STRING, callbackClass);
        Uri result = contentResolver.insert(DatabaseProvider.MESSAGE_URI, values);
        if (result != null) {
            ExLog.i(context, TAG, "Message saved.");
        }
    }

    public void removeIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        String url = extras.getString(MessageSendingService.URL);
        String callbackPayload = extras.getString(MessageSendingService.CALLBACK_PAYLOAD);
        String payload = extras.getString(MessageSendingService.PAYLOAD);
        String callbackClass = extras.getString(MessageSendingService.CALLBACK_CLASS);
        removeMessage(url, callbackPayload, payload, callbackClass);
    }

    private void removeMessage(String url, String callbackPayload, String payload, String callbackClass) {
        ExLog.i(context, TAG, String.format("Removing message \"%s\".", payload));
        String where = MessageTable.URL + " = ? AND " + MessageTable.CALLBACK_PAYLOAD + " = ? AND "
                + MessageTable.PAYLOAD + " = ? AND " + MessageTable.CALLBACK_CLASS_STRING + " = ?";
        int count = contentResolver.delete(DatabaseProvider.MESSAGE_URI, where, new String[] { url, callbackPayload,
                payload, callbackClass });
        if (count != 0) {
            ExLog.i(context, TAG, "Message removed.");
        }
    }

    public synchronized void removeAllMessages() {
        contentResolver.delete(DatabaseProvider.MESSAGE_URI, null, null);
    }

    public int getMessageCount() {
        int result = 0;
        Cursor cursor = contentResolver.query(DatabaseProvider.MESSAGE_URI, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            result = cursor.getCount();
            cursor.close();
        }
        return result;
    }

    public Cursor getContent() {
        return contentResolver.query(DatabaseProvider.MESSAGE_URI, null, null, null, null);
    }

    public static interface MessageRestorer {
        void restoreMessage(Context context, Intent messageIntent);
    }

    public List<Intent> restoreMessages() {
        List<Intent> delayedIntents = new ArrayList<Intent>();
        Cursor cursor = contentResolver.query(DatabaseProvider.MESSAGE_URI, null, null, null, null);
        if (cursor != null) {
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                String url = cursor.getString(cursor.getColumnIndex(MessageTable.URL));
                String callbackPayload = cursor.getString(cursor.getColumnIndex(MessageTable.CALLBACK_PAYLOAD));
                String payload = cursor.getString(cursor.getColumnIndex(MessageTable.PAYLOAD));
                String callbackClassString = cursor
                        .getString(cursor.getColumnIndex(MessageTable.CALLBACK_CLASS_STRING));

                Class<? extends ServerReplyCallback> callbackClass = null;
                if (callbackClassString != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends ServerReplyCallback> tmp = (Class<? extends ServerReplyCallback>) Class
                                .forName(callbackClassString);
                        callbackClass = tmp;
                    } catch (ClassNotFoundException e) {
                        ExLog.e(context, TAG, "Could not find class for callback name: " + callbackClassString);
                    }
                }

                // We are passing no message id, because we know it used to suppress message sending and
                // we want this message to be sent.
                Intent messageIntent = MessageSendingService.createMessageIntent(context, url, callbackPayload, null,
                        payload, callbackClass);

                if (messageRestorer != null) {
                    messageRestorer.restoreMessage(context, messageIntent);
                }

                if (messageIntent != null) {
                    delayedIntents.add(messageIntent);
                }
            }
            cursor.close();
        }
        ExLog.i(context, TAG, "Restored " + delayedIntents.size() + " messages");
        return delayedIntents;
    }
}
