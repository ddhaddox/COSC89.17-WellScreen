package edu.dartmouth.ui.insights;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.utils.EntryXComparator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import edu.dartmouth.R;
import edu.dartmouth.databinding.FragmentInsightsBinding;

public class InsightsFragment extends Fragment {

    private FragmentInsightsBinding binding;
    private InsightsViewModel insightsViewModel;

    // UI Components
    private LineChart lineChart;
    private CombinedChart combinedChart;
    private BarChart barChart;
    private LineChart lineChartLagged;
    private CombinedChart combinedChartLagged;
    private BarChart barChartLagged;

    private Spinner spinnerFeatureSelection;
    private Spinner spinnerLaggedFeatureSelection;

    private long selectedStartTime;
    private long selectedEndTime;

    // Threshold for average daily app usage (in hours)
    private static final double USAGE_THRESHOLD = 0.1;

    // List of features for selection
    private List<String> featureList;
    private List<String> laggedFeatureList;
    // Selected features
    private String selectedFeature;
    private String selectedLaggedFeature;
    // Results
    private InsightsViewModel.RegressionResult regressionResult;
    private List<InsightsViewModel.CorrelationResult> contemporaneousCorrelations;
    private InsightsViewModel.RegressionResult laggedRegressionResult;
    private List<InsightsViewModel.CorrelationResult> laggedCorrelations;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentInsightsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        insightsViewModel = new ViewModelProvider(this).get(InsightsViewModel.class);

        initializeUI();

        setDefaultTimeRange();
        insightsViewModel.setTimeRange(selectedStartTime, selectedEndTime);

        insightsViewModel.getMergedData().observe(getViewLifecycleOwner(), mergedData -> {
            if (mergedData != null && !mergedData.isEmpty()) {
                // Perform analyses
                InsightsViewModel.SummaryStatistics stats = insightsViewModel.computeDescriptiveStatistics(mergedData, USAGE_THRESHOLD);
                contemporaneousCorrelations = insightsViewModel.computeContemporaneousCorrelations(mergedData, stats);
                regressionResult = insightsViewModel.performContemporaneousRegression(mergedData, stats);

                // Lagged analyses
                laggedCorrelations = insightsViewModel.computeLaggedCorrelations(mergedData, stats);
                laggedRegressionResult = insightsViewModel.performLaggedRegression(mergedData, stats);

                // Update feature lists for selection
                updateFeatureList(contemporaneousCorrelations);
                updateLaggedFeatureList(laggedCorrelations);

                // Update UI with analyses
                updateDescriptiveStatisticsUI(stats);
                updateContemporaneousAnalysesUI(mergedData);
                updateLaggedAnalysesUI(mergedData);

                // Generate insights
                String insights = insightsViewModel.generateInsights(
                        contemporaneousCorrelations,
                        regressionResult,
                        laggedCorrelations,
                        laggedRegressionResult
                );
                binding.textViewInsights.setText(insights);
            } else {
                // Handle no data case
                binding.textViewDescriptiveStats.setText("No data available for the selected time range.");
                // Clear other UI components
                clearCharts();
                binding.textViewContemporaneousCorrelations.setText("");
                binding.textViewLaggedCorrelations.setText("");
                binding.textViewInsights.setText("");
            }
        });

//        insightsViewModel.getRegressionError().observe(getViewLifecycleOwner(), errorMessage -> {
//            if (errorMessage != null && !errorMessage.isEmpty()) {
//                // Display the error message to the user, e.g., via a Toast or Snackbar
//                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
//            }
//        });

        return root;
    }

    /**
     * Initializes the UI components and sets up event listeners.
     */
    private void initializeUI() {
        // Initialize Spinner
        ArrayAdapter<CharSequence> adapterSpinner = ArrayAdapter.createFromResource(getContext(),
                R.array.time_range_options_insights, android.R.layout.simple_spinner_item);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerTimeRange.setAdapter(adapterSpinner);

        binding.spinnerTimeRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String selectedOption = adapterView.getItemAtPosition(position).toString();
                switch (selectedOption) {
                    case "Forever":
                        setTimeRangeToForever();
                        break;
                    case "Last Year":
                        setTimeRangeToLastYear();
                        break;
                    case "Last 30 Days":
                        setTimeRangeToLastNDays(30);
                        break;
                    case "Last 7 Days":
                        setTimeRangeToLastNDays(7);
                        break;
                    case "Today":
                        setTimeRangeToToday();
                        break;
                    case "Custom":
                        showCustomDatePicker();
                        return;
                    default:
                        setDefaultTimeRange();
                        break;
                }
                insightsViewModel.setTimeRange(selectedStartTime, selectedEndTime);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Do nothing
            }
        });

        // Initialize Charts
        lineChart = binding.lineChart;
        combinedChart = binding.combinedChart;
        barChart = binding.barChart;

        lineChartLagged = binding.lineChartLagged;
        combinedChartLagged = binding.combinedChartLagged;
        barChartLagged = binding.barChartLagged;

        setupCharts();

        spinnerFeatureSelection = binding.spinnerFeatureSelection;
        spinnerLaggedFeatureSelection = binding.spinnerLaggedFeatureSelection;

        spinnerFeatureSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                selectedFeature = featureList.get(position);
                updateContemporaneousAnalysesUI(insightsViewModel.getMergedData().getValue());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        spinnerLaggedFeatureSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                selectedLaggedFeature = laggedFeatureList.get(position);
                updateLaggedAnalysesUI(insightsViewModel.getMergedData().getValue());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void setupCharts() {
        // Configure LineChart
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.getLegend().setEnabled(true);

        lineChartLagged.getDescription().setEnabled(false);
        lineChartLagged.setTouchEnabled(true);
        lineChartLagged.setDragEnabled(true);
        lineChartLagged.setScaleEnabled(true);
        lineChartLagged.setPinchZoom(true);
        lineChartLagged.getLegend().setEnabled(true);

        // Configure CombinedChart
        combinedChart.getDescription().setEnabled(false);
        combinedChart.setTouchEnabled(true);
        combinedChart.setDragEnabled(true);
        combinedChart.setScaleEnabled(true);
        combinedChart.setPinchZoom(true);
        combinedChart.getLegend().setEnabled(true);

        combinedChartLagged.getDescription().setEnabled(false);
        combinedChartLagged.setTouchEnabled(true);
        combinedChartLagged.setDragEnabled(true);
        combinedChartLagged.setScaleEnabled(true);
        combinedChartLagged.setPinchZoom(true);
        combinedChartLagged.getLegend().setEnabled(true);

        // Configure BarChart
        barChart.getDescription().setEnabled(false);
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);
        barChart.setPinchZoom(true);
        barChart.getLegend().setEnabled(true);

        // Configure Lagged BarChart
        barChartLagged.getDescription().setEnabled(false);
        barChartLagged.setTouchEnabled(true);
        barChartLagged.setDragEnabled(true);
        barChartLagged.setScaleEnabled(true);
        barChartLagged.setPinchZoom(true);
        barChartLagged.getLegend().setEnabled(true);
    }

    private void updateFeatureList(List<InsightsViewModel.CorrelationResult> correlations) {
        featureList = new ArrayList<>();
        if (correlations != null) {
            for (InsightsViewModel.CorrelationResult result : correlations) {
                featureList.add(result.variableName);
            }
        }

        // If the list is empty, show "Not enough data"
        if (featureList.isEmpty()) {
            featureList.add("Not enough data");
        }
        // Update Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, featureList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFeatureSelection.setAdapter(adapter);

        if (featureList.size() > 0) {
            selectedFeature = featureList.get(0);
        }
    }

    private void updateLaggedFeatureList(List<InsightsViewModel.CorrelationResult> laggedCorrelations) {
        laggedFeatureList = new ArrayList<>();
        if (laggedCorrelations != null) {
            for (InsightsViewModel.CorrelationResult result : laggedCorrelations) {
                String variableName = result.variableName;
                if (variableName.startsWith("Lagged ")) {
                    variableName = variableName.substring(7);
                }
                laggedFeatureList.add(variableName);
            }
        }

        // If the list is empty, show "Not enough data"
        if (laggedFeatureList.isEmpty()) {
            laggedFeatureList.add("Not enough data");
        }

        // Update Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, laggedFeatureList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLaggedFeatureSelection.setAdapter(adapter);

        if (laggedFeatureList.size() > 0) {
            selectedLaggedFeature = laggedFeatureList.get(0);
        }
    }


    private void updateDescriptiveStatisticsUI(InsightsViewModel.SummaryStatistics stats) {
        StringBuilder statsText = new StringBuilder();

        // MPHQ-9 Scores
        statsText.append("<b>Depression Assessment Scores:</b><br>")
                .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Mean: %.2f<br>", stats.mphq9Mean))
                .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Median: %.2f<br>", stats.mphq9Median))
                .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Standard Deviation: %.2f<br><br>", stats.mphq9StdDev));

        // Total Screen Time
        statsText.append("<b>Total Screen Time:</b><br>")
                .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Total: %.2f hours<br>", stats.totalScreenTimeSum))
                .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Mean: %.2f hours/day<br>", stats.totalScreenTimeMean))
                .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Median: %.2f hours/day<br>", stats.totalScreenTimeMedian))
                .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Standard Deviation: %.2f hours<br><br>", stats.totalScreenTimeStdDev));

        // Screen On/Off Events
        statsText.append("<b>Screen On/Off Events:</b><br>")
                .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Total: %d events<br>", stats.screenOnOffCountSum))
                .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Mean: %.2f events/day<br>", stats.screenOnOffCountMean))
                .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Median: %.2f events/day<br>", stats.screenOnOffCountMedian))
                .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Standard Deviation: %.2f events<br><br>", stats.screenOnOffCountStdDev));

        // Top Seven Most Used Apps
        statsText.append("<b>Top 7 Most Used Apps:</b><br>");
        int appIndex = 1;
        for (InsightsViewModel.AppUsageStat appStat : stats.topAppsUsageStats) {
            statsText.append(String.format("&nbsp; %d. <u>%s</u>:<br>", appIndex++, appStat.appName))
                    .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Total Usage: %.2f hours<br>", appStat.totalUsage))
                    .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Mean Usage: %.2f hours/day<br>", appStat.meanUsage))
                    .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Median Usage: %.2f hours/day<br>", appStat.medianUsage))
                    .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Standard Deviation: %.2f hours<br><br>", appStat.stdDevUsage));
        }

        // App Category Usage
        statsText.append("<b>App Category Usage (Sorted by Mean Usage):</b><br>");

        // Extract and sort categories by meanUsage descending
        List<InsightsViewModel.CategoryUsageStat> sortedCategories = new ArrayList<>(stats.categoryUsageStats.values());
        sortedCategories.sort((a, b) -> Double.compare(b.meanUsage, a.meanUsage));

        int categoryIndex = 1;
        for (InsightsViewModel.CategoryUsageStat categoryStat : sortedCategories) {
            statsText.append(String.format("&nbsp; %d. <u>%s</u>:<br>", categoryIndex++, categoryStat.categoryName))
                    .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Total Usage: %.2f hours<br>", categoryStat.totalUsage))
                    .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Mean Usage: %.2f hours/day<br>", categoryStat.meanUsage))
                    .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Median Usage: %.2f hours/day<br>", categoryStat.medianUsage))
                    .append(String.format("&nbsp;&nbsp;&nbsp;&nbsp;Standard Deviation: %.2f hours<br><br>", categoryStat.stdDevUsage));
        }

        // Set formatted text
        binding.textViewDescriptiveStats.setText(Html.fromHtml(statsText.toString(), Html.FROM_HTML_MODE_LEGACY));
    }


    private void updateContemporaneousAnalysesUI(List<InsightsViewModel.MergedData> data) {
        if (data == null || selectedFeature == null) return;

        // Update Line Chart with depression scores and selected feature over time
        updateLineChart(data);

        // Update Scatter Chart with depression scores vs selected feature
        updateScatterChart(data);

        // Update Bar Chart with regression coefficients
        updateBarChart(regressionResult); // Use stored regressionResult

        // Update correlations text
        updateContemporaneousCorrelationsUI(contemporaneousCorrelations); // Use stored correlations
    }

    private void updateLaggedAnalysesUI(List<InsightsViewModel.MergedData> data) {
        if (data == null || selectedLaggedFeature == null) return;

        // Update Lagged Line Chart
        updateLaggedLineChart(data);

        // Update Lagged Scatter Chart
        updateLaggedScatterChart(data);

        // Update Lagged Bar Chart
        updateLaggedBarChart(laggedRegressionResult);

        // Update Lagged Correlations text
        updateLaggedCorrelationsUI(laggedCorrelations);
    }

    private void updateLineChart(List<InsightsViewModel.MergedData> data) {
        // Prepare entries for line chart
        List<Entry> mphq9Entries = new ArrayList<>();
        List<Entry> featureEntries = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        for (int i = 0; i < data.size(); i++) {
            InsightsViewModel.MergedData item = data.get(i);
            dates.add(android.text.format.DateFormat.format("MM/dd", item.date).toString());

            if (item.mphq9Entity != null) {
                mphq9Entries.add(new Entry(i, (float) item.mphq9Entity.averageScore));
            } else {
                mphq9Entries.add(new Entry(i, Float.NaN));
            }

            float featureValue = getFeatureValue(item, selectedFeature);
            featureEntries.add(new Entry(i, featureValue));
        }

        LineDataSet mphq9DataSet = new LineDataSet(mphq9Entries, "Depression Scores");
        mphq9DataSet.setLineWidth(2f);
        mphq9DataSet.setCircleRadius(4f);
        mphq9DataSet.setDrawValues(false);

        LineDataSet featureDataSet = new LineDataSet(featureEntries, selectedFeature);
        featureDataSet.setColor(Color.BLACK);
        featureDataSet.setCircleColor(Color.BLACK);
        featureDataSet.setLineWidth(2f);
        featureDataSet.setCircleRadius(4f);
        featureDataSet.setDrawValues(false);

        LineData lineData = new LineData(mphq9DataSet, featureDataSet);
        lineChart.setData(lineData);

        // X-axis labels (dates)
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setDrawGridLines(false);

        lineChart.invalidate();
    }

    private void updateScatterChart(List<InsightsViewModel.MergedData> data) {
        List<Entry> scatterEntries = new ArrayList<>();

        for (InsightsViewModel.MergedData item : data) {
            if (item.mphq9Entity != null) {
                float mphq9Score = (float) item.mphq9Entity.averageScore;
                float featureValue = getFeatureValue(item, selectedFeature);
                scatterEntries.add(new Entry(featureValue, mphq9Score));
            }
        }

        if (scatterEntries.isEmpty()) {
            combinedChart.clear();
            combinedChart.invalidate();
            return;
        }

        ScatterDataSet scatterDataSet = new ScatterDataSet(scatterEntries, "Depression Scores vs " + selectedFeature);
        scatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        scatterDataSet.setDrawValues(false);
        scatterDataSet.setScatterShapeHoleColor(Color.BLACK);
        scatterDataSet.setScatterShapeHoleRadius(3f);

        // Add regression line
        LineDataSet regressionLine = getRegressionLine(scatterEntries);

        CombinedData combinedData = new CombinedData();
        combinedData.setData(new ScatterData(scatterDataSet));

        if (regressionLine != null) {
            combinedData.setData(new LineData(regressionLine));
        }

        combinedChart.setData(combinedData);

        // X and Y axes configuration
        XAxis xAxis = combinedChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        combinedChart.getLegend().setEnabled(false);

        combinedChart.setContentDescription("Scatter plot showing the relationship between Depression Scores and " + selectedFeature);

        combinedChart.invalidate();
    }

    private void updateLaggedLineChart(List<InsightsViewModel.MergedData> data) {
        // Prepare entries for lagged line chart
        List<Entry> mphq9Entries = new ArrayList<>();
        List<Entry> laggedFeatureEntries = new ArrayList<>();
        List<String> dates = new ArrayList<>();

        for (int i = 1; i < data.size(); i++) { // Start from 1 to account for lag
            InsightsViewModel.MergedData currentItem = data.get(i);
            InsightsViewModel.MergedData previousItem = data.get(i - 1); // Lagged by 1

            dates.add(android.text.format.DateFormat.format("MM/dd", currentItem.date).toString());

            if (currentItem.mphq9Entity != null) {
                mphq9Entries.add(new Entry(i - 1, (float) currentItem.mphq9Entity.averageScore));
            } else {
                mphq9Entries.add(new Entry(i - 1, Float.NaN));
            }

            float laggedFeatureValue = getLaggedFeatureValue(previousItem, selectedLaggedFeature);
            laggedFeatureEntries.add(new Entry(i - 1, laggedFeatureValue));
        }

        LineDataSet mphq9DataSet = new LineDataSet(mphq9Entries, "Depression Scores");
        mphq9DataSet.setLineWidth(2f);
        mphq9DataSet.setCircleRadius(4f);
        mphq9DataSet.setDrawValues(false);

        LineDataSet laggedFeatureDataSet = new LineDataSet(laggedFeatureEntries, selectedLaggedFeature + " (Lagged)");
        laggedFeatureDataSet.setColor(Color.BLACK);
        laggedFeatureDataSet.setCircleColor(Color.BLACK);
        laggedFeatureDataSet.setLineWidth(2f);
        laggedFeatureDataSet.setCircleRadius(4f);
        laggedFeatureDataSet.setDrawValues(false);

        LineData lineData = new LineData(mphq9DataSet, laggedFeatureDataSet);
        lineChartLagged.setData(lineData);

        // X-axis labels (dates)
        XAxis xAxis = lineChartLagged.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setDrawGridLines(false);

        lineChartLagged.invalidate(); // Refresh chart
    }

    private void updateLaggedScatterChart(List<InsightsViewModel.MergedData> data) {
        List<Entry> scatterEntries = new ArrayList<>();

        for (int i = 1; i < data.size(); i++) { // Start from 1 to account for lag
            InsightsViewModel.MergedData currentItem = data.get(i);
            InsightsViewModel.MergedData previousItem = data.get(i - 1); // Lagged by 1

            if (currentItem.mphq9Entity != null) {
                float mphq9Score = (float) currentItem.mphq9Entity.averageScore;
                float laggedFeatureValue = getLaggedFeatureValue(previousItem, selectedLaggedFeature);
                scatterEntries.add(new Entry(laggedFeatureValue, mphq9Score));
            }
        }

        if (scatterEntries.isEmpty()) {
            combinedChartLagged.clear();
            combinedChartLagged.invalidate();
            return;
        }

        ScatterDataSet scatterDataSet = new ScatterDataSet(scatterEntries, "Depression Scores vs " + selectedLaggedFeature + " (Lagged)");
        scatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        scatterDataSet.setDrawValues(false);
        scatterDataSet.setScatterShapeHoleColor(Color.BLACK);
        scatterDataSet.setScatterShapeHoleRadius(3f);

        // Add regression line
        LineDataSet regressionLine = getRegressionLine(scatterEntries);

        CombinedData combinedData = new CombinedData();
        combinedData.setData(new ScatterData(scatterDataSet));

        if (regressionLine != null) {
            combinedData.setData(new LineData(regressionLine));
        }

        combinedChartLagged.setData(combinedData);

        // X and Y axes configuration
        XAxis xAxis = combinedChartLagged.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        combinedChart.getLegend().setEnabled(false);

        combinedChartLagged.setContentDescription("Scatter plot showing the relationship between Depression Scores and " + selectedLaggedFeature + " (Lagged)");

        combinedChartLagged.invalidate();
    }

    private void updateBarChart(InsightsViewModel.RegressionResult regResult) {
        if (regResult.coefficients == null || regResult.variableNames == null || regResult.variableNames.length == 0) {
            barChart.clear();
            barChart.invalidate();
            return;
        }

        List<BarEntry> barEntries = new ArrayList<>();
        List<String> variableNames = new ArrayList<>();

        for (int i = 0; i < regResult.variableNames.length; i++) {
            barEntries.add(new BarEntry(i, (float) regResult.coefficients[i]));
            variableNames.add(regResult.variableNames[i]);
        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "Regression Coefficients");
        barDataSet.setColor(Color.MAGENTA);
        barDataSet.setValueTextSize(12f);
        barDataSet.setDrawValues(true);

        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);

        // X-axis labels
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(variableNames));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setDrawGridLines(false);

        barChart.invalidate();
    }

    private void updateLaggedBarChart(InsightsViewModel.RegressionResult regResult) {
        if (regResult.coefficients == null || regResult.variableNames == null || regResult.variableNames.length == 0) {
            barChartLagged.clear();
            barChartLagged.invalidate();
            return;
        }

        List<BarEntry> barEntries = new ArrayList<>();
        List<String> variableNames = new ArrayList<>();

        for (int i = 0; i < regResult.variableNames.length; i++) {
            barEntries.add(new BarEntry(i, (float) regResult.coefficients[i]));
            variableNames.add(regResult.variableNames[i]);
        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "Lagged Regression Coefficients");
        barDataSet.setColor(Color.CYAN);
        barDataSet.setValueTextSize(12f);
        barDataSet.setDrawValues(true);

        BarData barData = new BarData(barDataSet);
        barChartLagged.setData(barData);

        // X-axis labels
        XAxis xAxis = barChartLagged.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(variableNames));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setDrawGridLines(false);

        barChartLagged.invalidate();
    }

    private void updateContemporaneousCorrelationsUI(List<InsightsViewModel.CorrelationResult> correlations) {
        StringBuilder correlationText = new StringBuilder();

        if (correlations.isEmpty()) {
            correlationText.append("No correlation data available.");
        } else {
            // Sort correlations by absolute value in descending order
            correlations.sort((o1, o2) -> Double.compare(Math.abs(o2.correlationCoefficient), Math.abs(o1.correlationCoefficient)));

            for (InsightsViewModel.CorrelationResult result : correlations) {
                // Make the feature name bold using HTML <b> tag
                String featureBold = String.format("<b>%s</b>", result.variableName);

                // Interpret the correlation coefficient
                String interpretation = interpretCorrelation(result.correlationCoefficient);

                // Combine the feature name, interpretation, and correlation coefficient
                String formattedLine = String.format("%s: %s<br><br>", featureBold, interpretation);

                correlationText.append(formattedLine);
            }
        }

        // Set the formatted HTML text to the TextView
        binding.textViewContemporaneousCorrelations.setText(Html.fromHtml(correlationText.toString(), Html.FROM_HTML_MODE_LEGACY));
    }

    private void updateLaggedCorrelationsUI(List<InsightsViewModel.CorrelationResult> laggedCorrelations) {
        StringBuilder correlationText = new StringBuilder();

        if (laggedCorrelations.isEmpty()) {
            correlationText.append("No lagged correlation data available.");
        } else {
            // Sort lagged correlations by absolute value in descending order
            laggedCorrelations.sort((o1, o2) -> Double.compare(Math.abs(o2.correlationCoefficient), Math.abs(o1.correlationCoefficient)));

            for (InsightsViewModel.CorrelationResult result : laggedCorrelations) {
                // Make the feature name bold using HTML <b> tag
                String featureBold = String.format("<b>%s</b>", result.variableName);

                // Interpret the correlation coefficient
                String interpretation = interpretCorrelation(result.correlationCoefficient);

                // Combine the feature name, interpretation, and correlation coefficient
                String formattedLine = String.format("%s: %s<br><br>", featureBold, interpretation);

                correlationText.append(formattedLine);
            }
        }

        // Set the formatted HTML text to the TextView
        binding.textViewLaggedCorrelations.setText(Html.fromHtml(correlationText.toString(), Html.FROM_HTML_MODE_LEGACY));
    }

    /**
     * Interprets the correlation coefficient into a user-friendly description.
     *
     * @param coefficient The Pearson correlation coefficient.
     * @return A string describing the relationship.
     */
    private String interpretCorrelation(double coefficient) {
        String description;

        if (coefficient > 0.9) {
            description = "Very strong positive relationship";
        } else if (coefficient > 0.7) {
            description = "Strong positive relationship";
        } else if (coefficient > 0.5) {
            description = "Moderate positive relationship";
        } else if (coefficient > 0.3) {
            description = "Weak positive relationship";
        } else if (coefficient > 0.1) {
            description = "Very weak positive relationship";
        } else if (coefficient < -0.9) {
            description = "Very strong negative relationship";
        } else if (coefficient < -0.7) {
            description = "Strong negative relationship";
        } else if (coefficient < -0.5) {
            description = "Moderate negative relationship";
        } else if (coefficient < -0.3) {
            description = "Weak negative relationship";
        } else if (coefficient < -0.1) {
            description = "Very weak negative relationship";
        } else {
            description = "No significant relationship";
        }

        description += String.format(" (%.2f).", coefficient);

        return description;
    }

    private void clearCharts() {
        lineChart.clear();
        combinedChart.clear();
        barChart.clear();
        lineChartLagged.clear();
        combinedChartLagged.clear();
        barChartLagged.clear();

        lineChart.invalidate();
        combinedChart.invalidate();
        barChart.invalidate();
        lineChartLagged.invalidate();
        combinedChartLagged.invalidate();
        barChartLagged.invalidate();
    }

    /**
     * Generates a regression line for scatter charts.
     *
     * @param entries List of scatter plot entries.
     * @return LineDataSet representing the regression line.
     */
    private LineDataSet getRegressionLine(List<Entry> entries) {
        if (entries.size() < 2) {
            return null;
        }

        // Sort entries by x-value
        Collections.sort(entries, new EntryXComparator());

        // Prepare arrays for regression
        int n = entries.size();
        double[] x = new double[n];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = entries.get(i).getX();
            y[i] = entries.get(i).getY();
        }

        // Compute regression coefficients
        double xMean = 0;
        double yMean = 0;
        for (int i = 0; i < n; i++) {
            xMean += x[i];
            yMean += y[i];
        }
        xMean /= n;
        yMean /= n;

        double numerator = 0;
        double denominator = 0;
        for (int i = 0; i < n; i++) {
            numerator += (x[i] - xMean) * (y[i] - yMean);
            denominator += (x[i] - xMean) * (x[i] - xMean);
        }

        double slope = numerator / denominator;
        double intercept = yMean - slope * xMean;

        // Create regression line entries
        List<Entry> regressionEntries = new ArrayList<>();
        float minX = entries.get(0).getX();
        float maxX = entries.get(entries.size() - 1).getX();

        regressionEntries.add(new Entry(minX, (float) (slope * minX + intercept)));
        regressionEntries.add(new Entry(maxX, (float) (slope * maxX + intercept)));

        LineDataSet lineDataSet = new LineDataSet(regressionEntries, "Regression Line");
        lineDataSet.setLineWidth(3f);
        lineDataSet.setDrawValues(false);
        lineDataSet.setDrawCircles(false);

        return lineDataSet;
    }

    /**
     * Helper method to extract feature value from MergedData based on feature name.
     *
     * @param item        MergedData object.
     * @param featureName Name of the feature.
     * @return The value of the feature.
     */
    private float getFeatureValue(InsightsViewModel.MergedData item, String featureName) {
        if (featureName.equals("Total Screen Time")) {
            if (item.screenTimeEntity != null) {
                // Convert from milliseconds to hours
                return (float) item.screenTimeEntity.totalScreenTime / 3600000.0f; // 1 hour = 3600000 milliseconds
            } else {
                return 0f;
            }
        } else if (featureName.equals("Screen On/Off Events")) {
            return item.screenOnOffCount;
        } else if (featureName.startsWith("App: ")) {
            String appName = featureName.substring(5);
            if (item.appUsageMap != null) {
                Long usageTime = item.appUsageMap.get(appName);
                return usageTime != null ? usageTime / 3600000.0f : 0f; // Convert to hours
            } else {
                return 0f;
            }
        } else if (featureName.startsWith("Category: ")) {
            String categoryName = featureName.substring(10);
            if (item.categoryUsageMap != null) {
                Long usageTime = item.categoryUsageMap.get(categoryName);
                return usageTime != null ? usageTime / 3600000.0f : 0f; // Convert to hours
            } else {
                return 0f;
            }
        } else {
            return 0f;
        }
    }

    /**
     * Helper method to extract lagged feature value from MergedData based on feature name.
     *
     * @param item        MergedData object (previous day's data).
     * @param featureName Name of the lagged feature.
     * @return The value of the lagged feature.
     */
    private float getLaggedFeatureValue(InsightsViewModel.MergedData item, String featureName) {
        if (featureName.equals("Total Screen Time")) {
            if (item.screenTimeEntity != null) {
                // Convert from milliseconds to hours
                return (float) item.screenTimeEntity.totalScreenTime / 3600000.0f; // 1 hour = 3600000 milliseconds
            } else {
                return 0f;
            }
        } else if (featureName.equals("Screen On/Off Events")) {
            return item.screenOnOffCount;
        } else if (featureName.startsWith("App: ")) {
            String appName = featureName.substring(5);
            if (item.appUsageMap != null) {
                Long usageTime = item.appUsageMap.get(appName);
                return usageTime != null ? usageTime / 3600000.0f : 0f; // Convert to hours
            } else {
                return 0f;
            }
        } else if (featureName.startsWith("Category: ")) {
            String categoryName = featureName.substring(10);
            if (item.categoryUsageMap != null) {
                Long usageTime = item.categoryUsageMap.get(categoryName);
                return usageTime != null ? usageTime / 3600000.0f : 0f; // Convert to hours
            } else {
                return 0f;
            }
        } else {
            return 0f;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Time range methods
    private void setDefaultTimeRange() {
        setTimeRangeToForever();
    }

    private void setTimeRangeToForever() {
        selectedStartTime = 0; // Epoch time
        selectedEndTime = System.currentTimeMillis();
    }

    private void setTimeRangeToLastYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, -1);
        selectedStartTime = calendar.getTimeInMillis();
        selectedEndTime = System.currentTimeMillis();
    }

    private void setTimeRangeToLastNDays(int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -days);
        selectedStartTime = calendar.getTimeInMillis();
        selectedEndTime = System.currentTimeMillis();
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

    private void showCustomDatePicker() {
        final Calendar startCalendar = Calendar.getInstance();
        final Calendar endCalendar = Calendar.getInstance();

        // Start Date Picker
        DatePickerDialog startDatePicker = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            startCalendar.set(year, month, dayOfMonth, 0, 0, 0);

            // End Date Picker after selecting start date
            DatePickerDialog endDatePicker = new DatePickerDialog(getContext(), (view1, year1, month1, dayOfMonth1) -> {
                endCalendar.set(year1, month1, dayOfMonth1, 23, 59, 59);
                selectedStartTime = startCalendar.getTimeInMillis();
                selectedEndTime = endCalendar.getTimeInMillis();
                insightsViewModel.setTimeRange(selectedStartTime, selectedEndTime);
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
}
