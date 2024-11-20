// File: edu/dartmouth/notifications/NotificationUtils.java
package edu.dartmouth.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationUtils {

    public static final String CHANNEL_ID = "MPHQ9_CHANNEL";
    private static final String CHANNEL_NAME = "MPHQ9 Notifications";
    private static final String CHANNEL_DESC = "Notifications for MPHQ-9 assessments";

    /**
     * Creates the notification channel for MPHQ-9 notifications.
     *
     * @param context Application context
     */
    public static void createMPHQ9NotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
