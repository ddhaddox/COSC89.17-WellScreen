// File: edu/dartmouth/notifications/NotificationScheduler.java
package edu.dartmouth.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.List;

public class NotificationScheduler {

    private Context context;
    private AlarmManager alarmManager;

    public NotificationScheduler(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Schedules notifications based on the provided list of times.
     *
     * @param timeList List of Calendar instances representing notification times.
     */
    public void scheduleNotifications(List<Calendar> timeList) {
        cancelExistingNotifications();

        for (int i = 0; i < timeList.size(); i++) {
            Calendar calendar = (Calendar) timeList.get(i).clone();

            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            Intent intent = new Intent(context, MPHQ9NotificationReceiver.class);
            intent.putExtra("notification_id", i);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    i,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (alarmManager != null) {
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                );
            }
        }
    }

    /**
     * Cancels all existing scheduled notifications.
     */
    public void cancelExistingNotifications() {
        // Assuming a maximum of 10 notifications
        for (int i = 0; i < 10; i++) {
            Intent intent = new Intent(context, MPHQ9NotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    i,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null && alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }
}
