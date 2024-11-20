package edu.dartmouth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Start the ScreenStateService
            Intent serviceIntent = new Intent(context, ScreenStateService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            // Reschedule the AppUsageWorker
            PeriodicWorkRequest appUsageWorkRequest = new PeriodicWorkRequest.Builder(
                    AppUsageWorker.class, 1, TimeUnit.DAYS)
                    .build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "AppUsageCollectionWork",
                    ExistingPeriodicWorkPolicy.KEEP,
                    appUsageWorkRequest);
        }
    }
}