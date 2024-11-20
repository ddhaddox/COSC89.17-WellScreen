package edu.dartmouth.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.dartmouth.R;
import edu.dartmouth.data.entities.DailyScreenTimeEntity;
import edu.dartmouth.data.entities.MPHQ9Entity;
import edu.dartmouth.ui.mphq9.MPHQ9Activity;
import edu.dartmouth.ui.other.DataManagementActivity;
import edu.dartmouth.ui.other.ViewDataActivity;

public class HomeFragment extends Fragment {

    private MaterialButton btnViewData;
    private MaterialButton btnCompletePHQ9;
    private Spinner spinnerDataType;
    private Spinner spinnerTimePeriod;
    private Button btnGenerateChart;
    private LineChart lineChart;
    private ProgressBar progressBar;
    private HomeViewModel homeViewModel;
    private String selectedDataType = "Total Screen Time";
    private String selectedTimePeriod = "Past Week";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        btnViewData = root.findViewById(R.id.btnViewData);
        btnCompletePHQ9 = root.findViewById(R.id.btnCompleteMPHQ9);

        btnViewData.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ViewDataActivity.class);
            startActivity(intent);
        });

        btnCompletePHQ9.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MPHQ9Activity.class);
            startActivity(intent);
        });

        spinnerDataType = root.findViewById(R.id.spinnerDataType);
        spinnerTimePeriod = root.findViewById(R.id.spinnerTimePeriod);
        btnGenerateChart = root.findViewById(R.id.btnGenerateChart);
        lineChart = root.findViewById(R.id.lineChart);
        progressBar = root.findViewById(R.id.progressBar);

        setupSpinners();

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        btnGenerateChart.setOnClickListener(v -> generateChart());

        // Gen default chart
        selectedDataType = "Total Screen Time";
        selectedTimePeriod = "Past Week";
        generateChart();

        return root;
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> dataTypeAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.data_types,
                android.R.layout.simple_spinner_item
        );
        dataTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDataType.setAdapter(dataTypeAdapter);

        int defaultDataTypePosition = dataTypeAdapter.getPosition("Total Screen Time");
        spinnerDataType.setSelection(defaultDataTypePosition);
        selectedDataType = "Total Screen Time";

        spinnerDataType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDataType = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDataType = "Total Screen Time";
            }
        });

        ArrayAdapter<CharSequence> timePeriodAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.time_periods,
                android.R.layout.simple_spinner_item
        );
        timePeriodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimePeriod.setAdapter(timePeriodAdapter);

        int defaultTimePeriodPosition = timePeriodAdapter.getPosition("Past Week");
        spinnerTimePeriod.setSelection(defaultTimePeriodPosition);
        selectedTimePeriod = "Past Week";

        spinnerTimePeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTimePeriod = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedTimePeriod = "Past Week";
            }
        });
    }

    private void generateChart() {
        progressBar.setVisibility(View.VISIBLE);

        // Calculate start and end based on selectedTimePeriod
        long currentTime = System.currentTimeMillis();
        long startTime = calculateStartTime(selectedTimePeriod, currentTime);

        // Fetch data based on selectedDataType and timePeriod
        if (selectedDataType.equals("Depression Assessment Average Scores")) {
            fetchMPHQ9Data(startTime, currentTime);
        } else if (selectedDataType.equals("Total Screen Time")) {
            fetchScreenTimeData(startTime, currentTime);
        } else {
            Toast.makeText(getContext(), "Invalid Data Type Selected", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Calculates the start time based on the selected time period.
     */
    private long calculateStartTime(String timePeriod, long currentTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTime);

        switch (timePeriod) {
            case "Past Week":
                calendar.add(Calendar.DAY_OF_YEAR, -7);
                break;
            case "Past Month":
                calendar.add(Calendar.MONTH, -1);
                break;
            case "Past Year":
                calendar.add(Calendar.YEAR, -1);
                break;
            default:
                calendar.add(Calendar.DAY_OF_YEAR, -7);
        }

        return calendar.getTimeInMillis();
    }

    /**
     * Fetches MPHQ-9 data between startTime and endTime.
     */
    private void fetchMPHQ9Data(long startTime, long endTime) {
        homeViewModel.getMPHQ9Between(startTime, endTime).observe(getViewLifecycleOwner(), assessments -> {
            progressBar.setVisibility(View.GONE);
            if (assessments == null || assessments.isEmpty()) {
                Toast.makeText(getContext(), "No depression assessment data available for the selected period.", Toast.LENGTH_SHORT).show();
                lineChart.clear();
                lineChart.invalidate();
                return;
            }

            List<Entry> entries = new ArrayList<>();
            List<String> dateLabels = new ArrayList<>();

            // Sort assessments by timestamp
            assessments.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));

            for (int i = 0; i < assessments.size(); i++) {
                MPHQ9Entity assessment = assessments.get(i);
                entries.add(new Entry(i, assessment.averageScore));
                dateLabels.add(formatDate(assessment.timestamp, selectedTimePeriod));
            }

            displayChart(entries, dateLabels, "Depression Assessment Average Scores");
        });
    }

    /**
     * Fetches Screen Time data between startTime and endTime.
     */
    private void fetchScreenTimeData(long startTime, long endTime) {
        homeViewModel.getScreenTimeBetween(startTime, endTime).observe(getViewLifecycleOwner(), screenTimeEntities -> {
            progressBar.setVisibility(View.GONE);
            if (screenTimeEntities == null || screenTimeEntities.isEmpty()) {
                Toast.makeText(getContext(), "No Screen Time data available for the selected period.", Toast.LENGTH_SHORT).show();
                lineChart.clear();
                lineChart.invalidate();
                return;
            }

            List<Entry> entries = new ArrayList<>();
            List<String> dateLabels = new ArrayList<>();

            // Sort screenTimeEntities by date
            screenTimeEntities.sort((a, b) -> Long.compare(a.date, b.date));

            for (int i = 0; i < screenTimeEntities.size(); i++) {
                DailyScreenTimeEntity entity = screenTimeEntities.get(i);
                // Convert totalScreenTime from milliseconds to minutes
                float screenTimeMinutes = entity.totalScreenTime / 60000f;
                entries.add(new Entry(i, screenTimeMinutes));
                dateLabels.add(formatDate(entity.date, selectedTimePeriod));
            }

            displayChart(entries, dateLabels, "Total Screen Time (Minutes)");
        });
    }

    /**
     * Displays the line chart with provided entries and labels.
     */
    private void displayChart(List<Entry> entries, List<String> dateLabels, String label) {
        // Create LineDataSet
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(getResources().getColor(R.color.textColorPrimary));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawValues(false);
        dataSet.setValueTextColor(getResources().getColor(R.color.textColorPrimary));

        // Create LineData
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Customize Description
        Description description = new Description();
        description.setText("Trend over " + selectedTimePeriod);
        description.setTextColor(getResources().getColor(R.color.textColorPrimary));
        lineChart.setDescription(description);

        // Customize X-Axis with Labels
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setTextColor(getResources().getColor(R.color.textColorPrimary));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dateLabels));

        // Customize Y-Axis
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(R.color.textColorPrimary));
        leftAxis.setDrawGridLines(true);
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Enable Legend
        lineChart.getLegend().setEnabled(false);
        lineChart.setExtraBottomOffset(10f);

        // Animate Chart
        lineChart.animateX(1000);

        // Refresh Chart
        lineChart.invalidate();
    }

    /**
     * Formats timestamp based on time period for X-Axis labels.
     */
    private String formatDate(long timestamp, String timePeriod) {
        SimpleDateFormat sdf;
        switch (timePeriod) {
            case "Past Week":
                sdf = new SimpleDateFormat("E", Locale.getDefault()); // e.g., Mon, Tue
                break;
            case "Past Month":
                sdf = new SimpleDateFormat("MM/dd", Locale.getDefault()); // e.g., 09/21
                break;
            case "Past Year":
                sdf = new SimpleDateFormat("MMM", Locale.getDefault()); // e.g., Sep
                break;
            default:
                sdf = new SimpleDateFormat("MM/dd", Locale.getDefault());
        }
        return sdf.format(new Date(timestamp));
    }
}
