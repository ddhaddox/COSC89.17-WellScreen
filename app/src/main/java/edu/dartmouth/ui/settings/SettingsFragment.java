package edu.dartmouth.ui.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import edu.dartmouth.databinding.FragmentSettingsBinding;
import edu.dartmouth.repositories.DailyAppUsageRepository;
import edu.dartmouth.repositories.DailyCategoryUsageRepository;
import edu.dartmouth.repositories.DailyScreenTimeRepository;
import edu.dartmouth.repositories.MPHQ9Repository;
import edu.dartmouth.repositories.ScreenEventRepository;
import edu.dartmouth.testing.SyntheticDataGenerator;
import edu.dartmouth.ui.notifications.ManageNotificationsActivity;
import edu.dartmouth.ui.other.AddCustomDataActivity;
import edu.dartmouth.ui.other.DataManagementActivity;
import edu.dartmouth.ui.other.DataPolicyActivity;
import edu.dartmouth.ui.other.PermissionsActivity;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    // Settings Buttons
    private MaterialButton btnManagePermissions;
    private MaterialButton btnManageData;
    private MaterialButton btnDataPolicy;
    private MaterialButton btnManageNotifications;
    private MaterialButton btnGenerateSyntheticData;
    private MaterialButton btnExportData;
    private MaterialButton btnAddCustomData;

    // Repositories
    private DailyAppUsageRepository dailyAppUsageRepository;
    private DailyCategoryUsageRepository dailyCategoryUsageRepository;
    private DailyScreenTimeRepository dailyScreenTimeRepository;
    private MPHQ9Repository mphq9Repository;
    private ScreenEventRepository screenEventRepository;

    // Permission Launcher for WRITE_EXTERNAL_STORAGE (Deprecated in API 29+)
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // Launcher for creating a document (Exporting Data)
    private ActivityResultLauncher<String> createFileLauncher;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize repositories
        initializeRepositories();

        // Initialize Buttons
        initializeSettingsButtons();

        // Initialize Permission Launcher
        initializePermissionLauncher();

        // Initialize Create File Launcher
        initializeCreateFileLauncher();

        return root;
    }

    private void initializeRepositories() {
        Context context = requireContext();
        dailyAppUsageRepository = new DailyAppUsageRepository(context);
        dailyCategoryUsageRepository = new DailyCategoryUsageRepository(context);
        dailyScreenTimeRepository = new DailyScreenTimeRepository(context);
        mphq9Repository = new MPHQ9Repository(context);
        screenEventRepository = new ScreenEventRepository(context);
    }

    private void initializeSettingsButtons() {
        // Initialize Manage Permissions Button
        btnManagePermissions = binding.btnManagePermissions;
        btnManagePermissions.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PermissionsActivity.class);
            startActivity(intent);
        });

        // Initialize Manage Data Button
        btnManageData = binding.btnManageData;
        btnManageData.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DataManagementActivity.class);
            startActivity(intent);
        });

        // Initialize Data Policy Button
        btnDataPolicy = binding.btnDataPolicy;
        btnDataPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DataPolicyActivity.class);
            startActivity(intent);
        });

        // Initialize Manage Notifications Button
        btnManageNotifications = binding.btnManageNotifications;
        btnManageNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ManageNotificationsActivity.class);
            startActivity(intent);
        });

        // Initialize Generate Synthetic Data Button
        btnGenerateSyntheticData = binding.btnGenerateSyntheticData;
        btnGenerateSyntheticData.setOnClickListener(v -> {
            // Generate synthetic data
            SyntheticDataGenerator generator = new SyntheticDataGenerator(getContext());
            generator.generateSyntheticData();
            Toast.makeText(getContext(), "Synthetic data generated", Toast.LENGTH_SHORT).show();
        });

        // Initialize Export Data Button
        btnExportData = binding.btnExportData;
        btnExportData.setOnClickListener(v -> handleExportDataClick());

        // Initialize Custom Data Button
        btnAddCustomData = binding.btnAddCustomData;
        btnAddCustomData.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddCustomDataActivity.class);
            startActivity(intent);
        });
    }

    private void initializePermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        initiateDataExport();
                    } else {
                        Toast.makeText(getContext(),
                                "Permission denied. Cannot export data.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void initializeCreateFileLauncher() {
        createFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/csv"),
                uri -> {
                    if (uri != null) {
                        exportDataToUri(uri);
                    } else {
                        Toast.makeText(getContext(),
                                "Export canceled.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void handleExportDataClick() {
        Context context = getContext();
        if (context == null) return;
        initiateDataExport();
    }

    private void initiateDataExport() {
        // Prompt user to choose a location and filename
        createFileLauncher.launch("exported_data.csv");
    }

    private void exportDataToUri(Uri uri) {
        Context context = getContext();
        if (context == null) return;

        new Thread(() -> {
            try {
                // Fetch data from repositories
                StringBuilder csvBuilder = new StringBuilder();

                // Export Daily App Usage
                csvBuilder.append("Daily App Usage\n");
                csvBuilder.append("Package Name,Date,Total Time in Foreground (ms),Category Name\n");
                List<edu.dartmouth.data.entities.DailyAppUsageEntity> appUsageList =
                        dailyAppUsageRepository.getAllAppUsageSync();
                for (edu.dartmouth.data.entities.DailyAppUsageEntity usage : appUsageList) {
                    csvBuilder.append(escapeCsv(usage.packageName))
                            .append(",")
                            .append(usage.date)
                            .append(",")
                            .append(usage.totalTimeInForeground)
                            .append(",")
                            .append(escapeCsv(usage.categoryName))
                            .append("\n");
                }

                // Export Daily Category Usage
                csvBuilder.append("\nDaily Category Usage\n");
                csvBuilder.append("Category Name,Date,Total Time in Foreground (ms)\n");
                List<edu.dartmouth.data.entities.DailyCategoryUsageEntity> categoryUsageList =
                        dailyCategoryUsageRepository.getAllCategoryUsageSync();
                for (edu.dartmouth.data.entities.DailyCategoryUsageEntity usage : categoryUsageList) {
                    csvBuilder.append(escapeCsv(usage.categoryName))
                            .append(",")
                            .append(usage.date)
                            .append(",")
                            .append(usage.totalTimeInForeground)
                            .append("\n");
                }

                // Export Daily Screen Time
                csvBuilder.append("\nDaily Screen Time\n");
                csvBuilder.append("Date,Total Screen Time (ms)\n");
                List<edu.dartmouth.data.entities.DailyScreenTimeEntity> screenTimeList =
                        dailyScreenTimeRepository.getAllScreenTimeSync();
                for (edu.dartmouth.data.entities.DailyScreenTimeEntity screenTime : screenTimeList) {
                    csvBuilder.append(screenTime.date)
                            .append(",")
                            .append(screenTime.totalScreenTime)
                            .append("\n");
                }

                // Export MPHQ9 Assessments
                csvBuilder.append("\nMPHQ9 Assessments\n");
                csvBuilder.append("Timestamp,Q1,Q2,Q3,Q4,Q5,Q6,Q7,Q8,Q9,Average Score\n");
                List<edu.dartmouth.data.entities.MPHQ9Entity> mphq9List =
                        mphq9Repository.getAllAssessmentsSync();
                for (edu.dartmouth.data.entities.MPHQ9Entity assessment : mphq9List) {
                    csvBuilder.append(assessment.timestamp)
                            .append(",")
                            .append(assessment.q1)
                            .append(",")
                            .append(assessment.q2)
                            .append(",")
                            .append(assessment.q3)
                            .append(",")
                            .append(assessment.q4)
                            .append(",")
                            .append(assessment.q5)
                            .append(",")
                            .append(assessment.q6)
                            .append(",")
                            .append(assessment.q7)
                            .append(",")
                            .append(assessment.q8)
                            .append(",")
                            .append(assessment.q9)
                            .append(",")
                            .append(assessment.averageScore)
                            .append("\n");
                }

                // Export Screen Events
                csvBuilder.append("\nScreen Events\n");
                csvBuilder.append("Event,Timestamp\n");
                List<edu.dartmouth.data.entities.ScreenEventEntity> screenEventList =
                        screenEventRepository.getAllScreenEventsSync();
                for (edu.dartmouth.data.entities.ScreenEventEntity event : screenEventList) {
                    csvBuilder.append(event.isScreenOn ? "Screen On" : "Screen Off")
                            .append(",")
                            .append(event.timestamp)
                            .append("\n");
                }

                // Write CSV data to the selected Uri
                try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                    writer.write(csvBuilder.toString());
                }

                // Notify success on the main thread
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(context, "Data exported successfully.", Toast.LENGTH_LONG).show()
                );

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(context, "Failed to export data.", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    /**
     * Escapes CSV special characters by wrapping the field in quotes if necessary.
     * @param field The CSV field.
     * @return The escaped CSV field.
     */
    private String escapeCsv(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            field = field.replace("\"", "\"\"");
            field = "\"" + field + "\"";
        }
        return field;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
