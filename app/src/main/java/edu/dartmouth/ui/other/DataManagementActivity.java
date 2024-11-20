package edu.dartmouth.ui.other;

import static edu.dartmouth.MainActivity.isNightMode;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.AutoCompleteTextView; // Changed import here
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import edu.dartmouth.R;
import edu.dartmouth.repositories.ScreenEventRepository;
import edu.dartmouth.repositories.MPHQ9Repository;
import edu.dartmouth.repositories.DailyAppUsageRepository;
import edu.dartmouth.repositories.DailyCategoryUsageRepository;
import edu.dartmouth.repositories.DailyScreenTimeRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DataManagementActivity extends AppCompatActivity {

    private SwitchCompat switchAutoDeleteOldData;
    private AutoCompleteTextView spinnerAutoDeleteInterval;
    private Button btnDeleteAllData;
    private Button btnDeleteCustomRange;

    private EditText etStartDate;
    private EditText etEndDate;

    private DailyAppUsageRepository dailyAppUsageRepository;
    private DailyCategoryUsageRepository dailyCategoryUsageRepository;
    private DailyScreenTimeRepository dailyScreenTimeRepository;
    private ScreenEventRepository screenEventRepository;
    private MPHQ9Repository mphq9Repository;

    // Constants for preference keys
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_AUTO_DELETE_ENABLED = "auto_delete_old_data";
    private static final String KEY_AUTO_DELETE_INTERVAL = "auto_delete_interval";

    // Constants for intervals
    private static final int INTERVAL_ONE_WEEK = 0;
    private static final int INTERVAL_ONE_MONTH = 1;
    private static final int INTERVAL_THREE_MONTHS = 2;
    private static final int INTERVAL_SIX_MONTHS = 3;
    private static final int INTERVAL_ONE_YEAR = 4;
    private static final int INTERVAL_NEVER = 5;
    private Calendar startDateCalendar;
    private Calendar endDateCalendar;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_management);

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Data Management");
        setSupportActionBar(toolbar);

        // Enable the Up button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Manually set the navigation icon tint to ensure visibility
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
            }
        }

        // Initialize UI components
        switchAutoDeleteOldData = findViewById(R.id.switchAutoDeleteOldData);
        spinnerAutoDeleteInterval = findViewById(R.id.spinnerAutoDeleteInterval); // Changed from Spinner
        btnDeleteAllData = findViewById(R.id.btnDeleteAllData);
        btnDeleteCustomRange = findViewById(R.id.btnDeleteCustomRange);

        // Initialize EditText fields
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);

        Context context = getApplicationContext();
        dailyAppUsageRepository = new DailyAppUsageRepository(context);
        dailyCategoryUsageRepository = new DailyCategoryUsageRepository(context);
        dailyScreenTimeRepository = new DailyScreenTimeRepository(context);
        screenEventRepository = new ScreenEventRepository(context);
        mphq9Repository = new MPHQ9Repository(context);

        // Load the saved preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isAutoDeleteEnabled = prefs.getBoolean(KEY_AUTO_DELETE_ENABLED, false);
        int savedIntervalIndex = prefs.getInt(KEY_AUTO_DELETE_INTERVAL, INTERVAL_ONE_WEEK);

        // Set up the SwitchCompat
        switchAutoDeleteOldData.setChecked(isAutoDeleteEnabled);

        // Set up the AutoCompleteTextView as Exposed Dropdown Menu
        String[] timeIntervals = {"1 Week", "1 Month", "3 Months", "6 Months", "1 Year", "Never"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, timeIntervals);
        spinnerAutoDeleteInterval.setAdapter(adapter);
        spinnerAutoDeleteInterval.setText(timeIntervals[savedIntervalIndex], false); // Set initial selection
        spinnerAutoDeleteInterval.setEnabled(isAutoDeleteEnabled); // Enable or disable based on switch

        // Set up listener for the SwitchCompat
        switchAutoDeleteOldData.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the preference
            prefs.edit().putBoolean(KEY_AUTO_DELETE_ENABLED, isChecked).apply();

            // Enable or disable the AutoCompleteTextView
            spinnerAutoDeleteInterval.setEnabled(isChecked);

            Toast.makeText(this, "Automatic deletion is " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        // Set up listener for the AutoCompleteTextView
        spinnerAutoDeleteInterval.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Save the selected interval index
                prefs.edit().putInt(KEY_AUTO_DELETE_INTERVAL, position).apply();
            }
        });

        // Set up listeners for custom date selection fields
        etStartDate.setOnClickListener(v -> showDatePickerDialog(true));
        etEndDate.setOnClickListener(v -> showDatePickerDialog(false));

        // Set up listener for the custom deletion button
        btnDeleteCustomRange.setOnClickListener(v -> confirmDeleteCustomRange());

        // Set up listener for the Delete All Data button
        btnDeleteAllData.setOnClickListener(v -> confirmDeleteAllData());
    }

    /**
     * Shows a DatePickerDialog for selecting start or end date.
     *
     * @param isStartDate true if selecting start date, false for end date
     */
    private void showDatePickerDialog(boolean isStartDate) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0);
            selectedDate.set(Calendar.MILLISECOND, 0);

            if (isStartDate) {
                startDateCalendar = selectedDate;
                etStartDate.setText("Start Date: " + dateFormat.format(selectedDate.getTime()));
            } else {
                endDateCalendar = selectedDate;
                etEndDate.setText("End Date: " + dateFormat.format(selectedDate.getTime()));
            }
        }, year, month, day);

        datePickerDialog.show();
    }

    /**
     * Confirms with the user before deleting data within the selected custom range.
     */
    private void confirmDeleteCustomRange() {
        if (startDateCalendar == null || endDateCalendar == null) {
            Toast.makeText(this, "Please select both start and end dates.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate that start date is before or equal to end date
        if (startDateCalendar.after(endDateCalendar)) {
            Toast.makeText(this, "Start date must be before or equal to end date.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Format dates for display in the confirmation dialog
        String startDateStr = dateFormat.format(startDateCalendar.getTime());
        String endDateStr = dateFormat.format(endDateCalendar.getTime());

        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete data from " + startDateStr + " to " + endDateStr + "? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> deleteCustomRangeData())
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Deletes data within the selected custom range.
     */
    private void deleteCustomRangeData() {
        long startTime = getStartOfDay(startDateCalendar.getTimeInMillis());
        long endTime = getEndOfDay(endDateCalendar.getTimeInMillis());

        // Perform deletion in repositories
        dailyAppUsageRepository.deleteDataBetween(startTime, endTime);
        dailyCategoryUsageRepository.deleteDataBetween(startTime, endTime);
        dailyScreenTimeRepository.deleteDataBetween(startTime, endTime);
        screenEventRepository.deleteDataBetween(startTime, endTime);
        mphq9Repository.deleteAssessmentsBetween(startTime, endTime);

        Toast.makeText(this, "Data from the selected range has been deleted.", Toast.LENGTH_SHORT).show();

        // Reset selected dates and fields
        startDateCalendar = null;
        endDateCalendar = null;
        etStartDate.setText("Select Start Date");
        etEndDate.setText("Select End Date");
    }

    /**
     * Confirms with the user before deleting all data.
     */
    private void confirmDeleteAllData() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete all your data? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> deleteAllData())
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Deletes all data from repositories.
     */
    private void deleteAllData() {
        dailyAppUsageRepository.deleteAllData();
        dailyCategoryUsageRepository.deleteAllData();
        dailyScreenTimeRepository.deleteAllData();
        screenEventRepository.deleteAllData();
        mphq9Repository.deleteAll();

        Toast.makeText(this, "All data has been deleted.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Returns the timestamp representing the start of the day (midnight) for the given timestamp.
     */
    private long getStartOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Returns the timestamp representing the end of the day (23:59:59.999) for the given timestamp.
     */
    private long getEndOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    // Handle the Up button click
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
