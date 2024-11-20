package edu.dartmouth.ui.mphq9;

import static edu.dartmouth.MainActivity.isNightMode;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import edu.dartmouth.R;
import edu.dartmouth.data.entities.MPHQ9Entity;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MPHQ9ListActivity extends AppCompatActivity {

    private MPHQ9ViewModel mphq9ViewModel;
    private RecyclerView recyclerView;
    private MPHQ9Adapter adapter;
    private AutoCompleteTextView spinnerTimeRange;
    private TextInputLayout timeRangeInputLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView textViewTotalAssessments;

    private long selectedStartTime;
    private long selectedEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mphq9_list);

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("MPHQ-9 Assessments");
        setSupportActionBar(toolbar);

        // Enable the Up button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Manually set the navigation icon tint to ensure visibility
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setTint(getResources().getColor(R.color.white));
            }
        }

        // Initialize UI components
        initializeUI();

        // Set up ViewModel
        mphq9ViewModel = new ViewModelProvider(this).get(MPHQ9ViewModel.class);

        // Set default time range
        setDefaultTimeRange();

        // Observe data
        observeData();

        // Fetch initial data
        refreshData();

        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshData();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void initializeUI() {
        spinnerTimeRange = findViewById(R.id.spinnerTimeRange);
        timeRangeInputLayout = findViewById(R.id.timeRangeInputLayout);
        String[] timeRangeOptions = {"Today", "Last 7 Days", "Last 30 Days", "Custom"};
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, timeRangeOptions);
        spinnerTimeRange.setAdapter(adapterSpinner);

        spinnerTimeRange.setOnItemClickListener((parent, view, position, id) -> {
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
            refreshData();
        });

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewMPHQ9);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MPHQ9Adapter();
        recyclerView.setAdapter(adapter);

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Initialize Total Assessments TextView
        textViewTotalAssessments = findViewById(R.id.textViewTotalAssessments);
    }

    private void observeData() {
        mphq9ViewModel.getAssessmentsLiveData().observe(this, assessments -> {
            if (assessments != null && !assessments.isEmpty()) {
                adapter.setMPHQ9List(assessments);
                textViewTotalAssessments.setText("Total Assessments: " + assessments.size());
            } else {
                Toast.makeText(MPHQ9ListActivity.this, "No MPHQ-9 assessments available for the selected time range.", Toast.LENGTH_SHORT).show();
                adapter.setMPHQ9List(new ArrayList<>());
                textViewTotalAssessments.setText("Total Assessments: 0");
            }
        });
    }

    private void refreshData() {
        // Update data in ViewModel
        mphq9ViewModel.setTimeRange(selectedStartTime, selectedEndTime);
    }

    private void setDefaultTimeRange() {
        setTimeRangeToToday();
        spinnerTimeRange.setText("Today", false);
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

        DatePickerDialog startDatePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            startCalendar.set(year, month, dayOfMonth, 0, 0, 0);
            DatePickerDialog endDatePicker = new DatePickerDialog(this, (view1, year1, month1, dayOfMonth1) -> {
                endCalendar.set(year1, month1, dayOfMonth1, 23, 59, 59);
                selectedStartTime = startCalendar.getTimeInMillis();
                selectedEndTime = endCalendar.getTimeInMillis();
                refreshData();
            }, startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH));
            endDatePicker.setTitle("Select End Date");
            endDatePicker.show();
        }, startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.DAY_OF_MONTH));
        startDatePicker.setTitle("Select Start Date");
        startDatePicker.show();
    }

    /**
     * Handle the Up button click in the Toolbar
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
