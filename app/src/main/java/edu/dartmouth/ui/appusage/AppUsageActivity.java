package edu.dartmouth.ui.appusage;

import android.app.AppOpsManager;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.dartmouth.R;
import edu.dartmouth.ui.other.PermissionsActivity;
import edu.dartmouth.AppUsageWorker;
import edu.dartmouth.data.entities.AppUsageSummary;
import edu.dartmouth.data.entities.CategoryUsageSummary;

public class AppUsageActivity extends AppCompatActivity {

    private AppUsageViewModel appUsageViewModel;
    private RecyclerView recyclerViewAppUsage;
    private RecyclerView recyclerViewCategoryUsage;
    private AppUsageAdapter appUsageAdapter;
    private CategoryUsageAdapter categoryUsageAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AutoCompleteTextView spinnerTimeRange;
    private TextView textViewTotalUsage;
    private long selectedStartTime;
    private long selectedEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_usage);

        // Set up Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("App Usage");
        setSupportActionBar(toolbar);

        // Enable Up button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Manually set navigation icon tint to ensure visibility
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
            }
        }

        // Check for Usage Stats Permission
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Usage Access Permission not granted. Please grant it.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, PermissionsActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize UI components
        initializeUI();

        // Initialize the ViewModel
        appUsageViewModel = new ViewModelProvider(this).get(AppUsageViewModel.class);

        // Set default time range to Today
        setDefaultTimeRange();

        // Set the spinner's text to "Today" to reflect the default selection
        setDefaultSpinnerSelection();

        // Fetch and observe data
        fetchAndObserveData();
    }


    private void initializeUI() {
        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshData);

        // Initialize AutoCompleteTextView
        spinnerTimeRange = findViewById(R.id.spinnerTimeRange);
        ArrayAdapter<CharSequence> adapterSpinner = ArrayAdapter.createFromResource(this,
                R.array.time_range_options, android.R.layout.simple_dropdown_item_1line);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinnerTimeRange.setAdapter(adapterSpinner);

        spinnerTimeRange.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = parent.getItemAtPosition(position).toString();
                switch (selectedOption) {
                    case "Today":
                        setTimeRangeToToday();
                        break;
                    case "Last 7 Days":
                        setTimeRangeToLastNDays(7);
                        break;
                    case "Last 30 Days":
                        setTimeRangeToLastNDays(30);
                        break;
                    case "Custom":
                        showCustomDatePicker();
                        break;
                    default:
                        setDefaultTimeRange();
                        break;
                }
                fetchAndObserveData();
            }
        });

        // Initialize TextView for Total Usage
        textViewTotalUsage = findViewById(R.id.textViewTotalUsage);

        // Set up RecyclerView for Category Usage
        recyclerViewCategoryUsage = findViewById(R.id.recyclerViewCategoryUsage);
        recyclerViewCategoryUsage.setLayoutManager(new LinearLayoutManager(this));
        categoryUsageAdapter = new CategoryUsageAdapter();
        recyclerViewCategoryUsage.setAdapter(categoryUsageAdapter);

        // Set up RecyclerView for App Usage
        recyclerViewAppUsage = findViewById(R.id.recyclerViewAppUsage);
        recyclerViewAppUsage.setLayoutManager(new LinearLayoutManager(this));
        appUsageAdapter = new AppUsageAdapter();
        recyclerViewAppUsage.setAdapter(appUsageAdapter);
    }

    private void setDefaultSpinnerSelection() {
        String today = getResources().getStringArray(R.array.time_range_options)[0];
        spinnerTimeRange.setText(today, false);
    }

    private void fetchAndObserveData() {
        // Observe Total Screen Time
        appUsageViewModel.getDailyScreenTime().observe(this, new Observer<Long>() {
            @Override
            public void onChanged(Long totalScreenTime) {
                if (totalScreenTime != null) {
                    textViewTotalUsage.setText("Total Usage: " + formatUsageTime(totalScreenTime));
                } else {
                    textViewTotalUsage.setText("Total Usage: N/A");
                }
            }
        });

        // **Observe Aggregated Category Usage**
        appUsageViewModel.getAggregatedCategoryUsageList().observe(this, new Observer<List<CategoryUsageSummary>>() {
            @Override
            public void onChanged(List<CategoryUsageSummary> categoryUsageSummaries) {
                swipeRefreshLayout.setRefreshing(false);
                if (categoryUsageSummaries == null || categoryUsageSummaries.isEmpty()) {
                    Toast.makeText(AppUsageActivity.this, "No category usage data available for the selected time range.", Toast.LENGTH_SHORT).show();
                    categoryUsageAdapter.setCategoryUsageList(new ArrayList<>());
                } else {
                    categoryUsageAdapter.setCategoryUsageList(categoryUsageSummaries);
                }
            }
        });

        // Observe Aggregated App Usage
        appUsageViewModel.getAggregatedAppUsageList().observe(this, new Observer<List<AppUsageSummary>>() {
            @Override
            public void onChanged(List<AppUsageSummary> appUsageSummaries) {
                swipeRefreshLayout.setRefreshing(false);
                if (appUsageSummaries == null || appUsageSummaries.isEmpty()) {
                    Toast.makeText(AppUsageActivity.this, "No app usage data available for the selected time range.", Toast.LENGTH_SHORT).show();
                    appUsageAdapter.setAppUsageList(new ArrayList<>());
                } else {
                    appUsageAdapter.setAppUsageList(appUsageSummaries);
                }
            }
        });

        appUsageViewModel.setTimeRange(selectedStartTime, selectedEndTime);
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void refreshData() {
        // Show the refreshing animation
        swipeRefreshLayout.setRefreshing(true);

        // Trigger the worker to collect new data
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(AppUsageWorker.class).build();
        WorkManager.getInstance(this).enqueue(workRequest);

        // Delay fetching data to allow the worker to complete
        new Handler().postDelayed(() -> {
            // Update the time range and fetch data
            appUsageViewModel.setTimeRange(selectedStartTime, selectedEndTime);
        }, 2000); // Delay 2 sec
    }

    private void setDefaultTimeRange() {
        setTimeRangeToToday();
    }

    private void setTimeRangeToToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        selectedStartTime = calendar.getTimeInMillis();

        calendar = Calendar.getInstance();
        selectedEndTime = calendar.getTimeInMillis();
    }

    private void setTimeRangeToLastNDays(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -days);
        selectedStartTime = calendar.getTimeInMillis();

        calendar = Calendar.getInstance();
        selectedEndTime = calendar.getTimeInMillis();
    }

    private void showCustomDatePicker() {
        final Calendar startCalendar = Calendar.getInstance();
        final Calendar endCalendar = Calendar.getInstance();

        // Start Date Picker
        DatePickerDialog startDatePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            startCalendar.set(year, month, dayOfMonth, 0, 0, 0);

            // End Date Picker
            DatePickerDialog endDatePicker = new DatePickerDialog(this, (view1, year1, month1, dayOfMonth1) -> {
                endCalendar.set(year1, month1, dayOfMonth1, 23, 59, 59);
                selectedStartTime = startCalendar.getTimeInMillis();
                selectedEndTime = endCalendar.getTimeInMillis();
                fetchAndObserveData();
            }, startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH));
            endDatePicker.setTitle("Select End Date");
            endDatePicker.getDatePicker().setMinDate(startCalendar.getTimeInMillis());
            endDatePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
            endDatePicker.show();
        }, startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH));
        startDatePicker.setTitle("Select Start Date");
        startDatePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
        startDatePicker.show();
    }

    private String formatUsageTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%02dh:%02dm:%02ds", hours, minutes % 60, seconds % 60);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}