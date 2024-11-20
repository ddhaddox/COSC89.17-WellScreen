package edu.dartmouth.ui.other;

import static edu.dartmouth.MainActivity.isNightMode;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import edu.dartmouth.R;

public class PermissionsActivity extends AppCompatActivity {

    private Button btnRequestUsagePermissions;
    private Button btnRequestBatteryOptimizations;
    private Button btnRequestForegroundServicePermission;
    private TextView txtUsagePermissionStatus;
    private TextView txtBatteryOptimizationStatus;
    private TextView txtForegroundServicePermissionStatus;
    private Button btnRequestNotificationPermission;
    private TextView txtNotificationPermissionStatus;

    private ActivityResultLauncher<String> requestForegroundServicePermissionLauncher;
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Permissions");
        setSupportActionBar(toolbar);

        // Enable the Up button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Manually set the navigation icon tint to ensure visibility
            toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.white));
        }

        btnRequestUsagePermissions = findViewById(R.id.btnRequestUsagePermissions);
        btnRequestBatteryOptimizations = findViewById(R.id.btnRequestBatteryOptimizations);
        btnRequestForegroundServicePermission = findViewById(R.id.btnRequestForegroundServicePermission);
        txtUsagePermissionStatus = findViewById(R.id.txtUsagePermissionStatus);
        txtBatteryOptimizationStatus = findViewById(R.id.txtBatteryOptimizationStatus);
        txtForegroundServicePermissionStatus = findViewById(R.id.txtForegroundServicePermissionStatus);
        btnRequestNotificationPermission = findViewById(R.id.btnRequestNotificationPermission);
        txtNotificationPermissionStatus = findViewById(R.id.txtNotificationPermissionStatus);

        initializePermissionLauncher();

        updatePermissionStatuses();

        btnRequestUsagePermissions.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        });

        btnRequestBatteryOptimizations.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Battery optimization settings not available on this device.", Toast.LENGTH_SHORT).show();
            }
        });

        btnRequestForegroundServicePermission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Adjust API level as needed
                requestForegroundServicePermissionLauncher.launch(Manifest.permission.FOREGROUND_SERVICE);
            } else {
                Toast.makeText(this, "Foreground Service Permission not required on this device.", Toast.LENGTH_SHORT).show();
            }
        });

        btnRequestNotificationPermission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Toast.makeText(this, "Notification Permission not required on this device.", Toast.LENGTH_SHORT).show();
            }
        });

        View lineUnderToolbar = findViewById(R.id.lineUnderToolbar);

        if (isNightMode(this)) {
            lineUnderToolbar.setVisibility(View.VISIBLE);
        } else {
            lineUnderToolbar.setVisibility(View.GONE);
        }
    }

    private void initializePermissionLauncher() {
        requestForegroundServicePermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        // Permission is granted
                        updatePermissionStatuses();
                    } else {
                        // Permission denied
                        Toast.makeText(this, "Foreground Service Permission Denied.", Toast.LENGTH_SHORT).show();
                    }
                });

        requestNotificationPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        updatePermissionStatuses();
                    } else {
                        Toast.makeText(this, "Notification Permission Denied.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatuses();
    }

    private void updatePermissionStatuses() {
        if (hasUsageStatsPermission()) {
            txtUsagePermissionStatus.setText("App Usage Permission Granted.");
            btnRequestUsagePermissions.setEnabled(false);
        } else {
            txtUsagePermissionStatus.setText("App Usage Permission NOT Granted.");
            btnRequestUsagePermissions.setEnabled(true);
        }

        if (isIgnoringBatteryOptimizations()) {
            txtBatteryOptimizationStatus.setText("Battery Optimization Disabled.");
            btnRequestBatteryOptimizations.setEnabled(false);
        } else {
            txtBatteryOptimizationStatus.setText("Battery Optimization Enabled.");
            btnRequestBatteryOptimizations.setEnabled(true);
        }

        if (hasForegroundServicePermission()) {
            txtForegroundServicePermissionStatus.setText("Foreground Service Permission Granted.");
            btnRequestForegroundServicePermission.setEnabled(false);
        } else {
            txtForegroundServicePermissionStatus.setText("Foreground Service Permission NOT Granted.");
            btnRequestForegroundServicePermission.setEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                txtNotificationPermissionStatus.setText("Notification Permission Granted.");
                btnRequestNotificationPermission.setEnabled(false);
            } else {
                txtNotificationPermissionStatus.setText("Notification Permission NOT Granted.");
                btnRequestNotificationPermission.setEnabled(true);
            }
        } else {
            txtNotificationPermissionStatus.setText("Notification Permission Not Required.");
            btnRequestNotificationPermission.setEnabled(false);
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    private boolean hasForegroundServicePermission() {
        if (Build.VERSION.SDK_INT >= 34) {
            return checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC) == PackageManager.PERMISSION_GRANTED;
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED;
    }

    // Handle the Up button click
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
