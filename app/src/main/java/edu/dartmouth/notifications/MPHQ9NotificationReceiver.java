// File: edu/dartmouth/notifications/MPHQ9NotificationReceiver.java
package edu.dartmouth.notifications;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import edu.dartmouth.R;
import edu.dartmouth.ui.mphq9.MPHQ9Activity;

public class MPHQ9NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Retrieve the unique notification ID from the intent
        int notificationId = intent.getIntExtra("notification_id", 0);

        // Create an intent to open MPHQ9Activity
        Intent mphq9Intent = new Intent(context, MPHQ9Activity.class);
        mphq9Intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                mphq9Intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_assignment_24)
                .setContentTitle("MPHQ-9 Assessment")
                .setContentText("Please complete your daily MPHQ-9 assessment.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent) // Set the intent that will fire when the user taps the notification
                .setAutoCancel(true); // Automatically remove the notification when tapped

        // Show the notification using the unique notification ID
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, builder.build());
    }
}