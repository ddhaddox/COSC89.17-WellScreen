// File: edu/dartmouth/MainActivity.java
package edu.dartmouth;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.dartmouth.collectors.ScreenStateReceiver;
import edu.dartmouth.databinding.ActivityMainBinding;
import edu.dartmouth.notifications.MPHQ9NotificationReceiver;
import edu.dartmouth.notifications.NotificationScheduler;
import edu.dartmouth.notifications.NotificationUtils;
import edu.dartmouth.ui.other.PermissionsActivity;

import org.json.JSONArray;
import org.json.JSONException;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ScreenStateReceiver screenStateReceiver;
    private ActivityResultLauncher<String> requestForegroundServiceDataSyncPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Generate encryption key
        edu.dartmouth.KeyManager.generateKey(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up the Toolbar as the ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_insights, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Schedule App Usage Data Collection
        scheduleAppUsageCollection();

        // Initialize permission launcher
        initializePermissionLauncher();

        // Check and request permissions
        if (!hasAllPermissions()) {
            // Redirect to PermissionsActivity
            Intent intent = new Intent(this, PermissionsActivity.class);
            startActivity(intent);
        } else {
            // Start the ScreenStateService
            startScreenStateService();
        }
        // Create notification channels
        NotificationUtils.createMPHQ9NotificationChannel(this);

        // Schedule MPHQ-9 notifications based on user settings
        scheduleUserNotifications();

        View lineUnderToolbar = findViewById(R.id.lineUnderToolbar);
        View lineAboveNavBar = findViewById(R.id.lineAboveNavBar);

        if (isNightMode(this)) {
            lineUnderToolbar.setVisibility(View.VISIBLE);
            lineAboveNavBar.setVisibility(View.VISIBLE);
        } else {
            lineUnderToolbar.setVisibility(View.GONE);
            lineAboveNavBar.setVisibility(View.GONE);
        }
    }

    /**
     * Initializes the ActivityResultLauncher for requesting FOREGROUND_SERVICE_DATA_SYNC permission.
     */
    private void initializePermissionLauncher() {
        requestForegroundServiceDataSyncPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        // Permission granted, start the service
                        startScreenStateService();
                        Toast.makeText(this, "Foreground Service Permission Granted.", Toast.LENGTH_SHORT).show();
                    } else {
                        // Permission denied, inform the user
                        Toast.makeText(this, "Foreground Service Permission Denied. The app may not function correctly.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Registers the ScreenStateReceiver dynamically.
     */
    private void registerScreenStateReceiver() {
        if (screenStateReceiver == null) {
            screenStateReceiver = new ScreenStateReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(screenStateReceiver, filter);
        }
    }

    /**
     * Starts the ScreenStateService as a foreground service.
     */
    private void startScreenStateService() {
        Intent serviceIntent = new Intent(this, ScreenStateService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    /**
     * Schedules periodic app usage data collection using WorkManager.
     */
    private void scheduleAppUsageCollection() {
        // Schedule the worker to run once a day
        PeriodicWorkRequest appUsageWorkRequest = new PeriodicWorkRequest.Builder(
                AppUsageWorker.class, 1, TimeUnit.DAYS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "AppUsageCollectionWork",
                ExistingPeriodicWorkPolicy.KEEP,
                appUsageWorkRequest);
    }

    /**
     * Checks if all required permissions are granted.
     *
     * @return true if all permissions are granted, false otherwise
     */
    private boolean hasAllPermissions() {
        return hasUsageStatsPermission() && isIgnoringBatteryOptimizations() && hasForegroundServiceDataSyncPermission();
    }

    /**
     * Checks if the app has Usage Stats permission.
     *
     * @return true if granted, false otherwise
     */
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    /**
     * Checks if the app is ignoring battery optimizations.
     *
     * @return true if ignoring, false otherwise
     */
    private boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true; // Battery optimizations are not enforced on devices below API 23
    }

    /**
     * Checks if the app has FOREGROUND_SERVICE_DATA_SYNC permission.
     *
     * @return true if granted, false otherwise
     */
    private boolean hasForegroundServiceDataSyncPermission() {
        if (Build.VERSION.SDK_INT >= 34) {
            return checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= 34) {
            if (!hasForegroundServiceDataSyncPermission()) {
                requestForegroundServiceDataSyncPermissionLauncher.launch(Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC);
            }
        }
        scheduleUserNotifications();
    }

    /**
     * Schedules notifications based on user-defined settings.
     */
    private void scheduleUserNotifications() {
        SharedPreferences prefs = getSharedPreferences("notification_prefs", MODE_PRIVATE);
        String timesJson = prefs.getString("notification_times", null);
        int maxNotifications = prefs.getInt("notification_count", 3);

        List<Calendar> timeList = new ArrayList<>();

        if (timesJson != null) {
            try {
                JSONArray jsonArray = new JSONArray(timesJson);
                for (int i = 0; i < jsonArray.length() && i < maxNotifications; i++) {
                    long timeInMillis = jsonArray.getLong(i);
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(timeInMillis);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    timeList.add(cal);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        NotificationScheduler scheduler = new NotificationScheduler(this);
        scheduler.scheduleNotifications(timeList);
    }

    public static boolean isNightMode(Context context) {
        int nightModeFlags =
                context.getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}
