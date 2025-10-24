package com.sap.sailing.android.shared.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.os.BuildCompat;

public class NotificationHelper {

    private static final int NOTIFICATION_ID = 13062018;
    private final static String CHANNEL_ID = "default";

    private static Bitmap largeIcon;
    private static int smallIcon;

    public static void prepareNotificationWith(Bitmap appIcon, int notificationIcon) {
        largeIcon = appIcon;
        smallIcon = notificationIcon;
    }

    public static Notification getNotification(Context context, String channelId, CharSequence customTitle,
            String content, PendingIntent intent, int color) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId).setContentText(content)
                .setContentTitle(customTitle).setLargeIcon(largeIcon).setSmallIcon(smallIcon).setOngoing(true)
                .setColor(color).setPriority(Notification.PRIORITY_HIGH);
        if (intent != null) {
            builder.setContentIntent(intent);
        }
        return builder.build();
    }

    public static String getNotificationChannelId() {
        return CHANNEL_ID;
    }

    public static int getNotificationId() {
        return NOTIFICATION_ID;
    }

    public static void createNotificationChannel(Context context, String id, CharSequence name) {
        createNotificationChannel(context, id, name, NotificationManagerCompat.IMPORTANCE_DEFAULT);
    }

    @SuppressWarnings("deprecation")
    public static void createNotificationChannel(Context context, String id, CharSequence name, int importance) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (BuildCompat.isAtLeastO()) {
            NotificationChannel channel = new NotificationChannel(id, name, importance);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
