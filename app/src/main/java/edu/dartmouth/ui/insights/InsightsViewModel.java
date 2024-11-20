// File: edu/dartmouth/ui/insights/InsightsViewModel.java

package edu.dartmouth.ui.insights;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.dartmouth.data.entities.DailyAppUsageEntity;
import edu.dartmouth.data.entities.DailyCategoryUsageEntity;
import edu.dartmouth.data.entities.DailyScreenTimeEntity;
import edu.dartmouth.data.entities.MPHQ9Entity;
import edu.dartmouth.data.entities.ScreenEventCount;
import edu.dartmouth.repositories.DailyAppUsageRepository;
import edu.dartmouth.repositories.DailyCategoryUsageRepository;
import edu.dartmouth.repositories.DailyScreenTimeRepository;
import edu.dartmouth.repositories.MPHQ9Repository;
import edu.dartmouth.repositories.ScreenEventRepository;

import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

public class InsightsViewModel extends AndroidViewModel {

    // Repositories
    private final MPHQ9Repository mphq9Repository;
    private final DailyScreenTimeRepository dailyScreenTimeRepository;
    private final DailyCategoryUsageRepository dailyCategoryUsageRepository;
    private final DailyAppUsageRepository dailyAppUsageRepository;
    private final ScreenEventRepository screenEventRepository;

    // LiveData sources
    private LiveData<List<MPHQ9Entity>> mphq9Data;
    private LiveData<List<DailyScreenTimeEntity>> screenTimeData;
    private LiveData<List<DailyCategoryUsageEntity>> categoryUsageData;
    private LiveData<List<DailyAppUsageEntity>> appUsageData;
    private LiveData<List<ScreenEventCount>> screenEventCountsData;

    // Merged data
    private final MediatorLiveData<List<MergedData>> mergedData = new MediatorLiveData<>();
    private long startTime;
    private long endTime;

    public InsightsViewModel(@NonNull Application application) {
        super(application);
        mphq9Repository = new MPHQ9Repository(application);
        dailyScreenTimeRepository = new DailyScreenTimeRepository(application);
        dailyCategoryUsageRepository = new DailyCategoryUsageRepository(application);
        dailyAppUsageRepository = new DailyAppUsageRepository(application);
        screenEventRepository = new ScreenEventRepository(application);
    }

    /**
     * Sets the time range for data retrieval and triggers data merging.
     *
     * @param startTime Start timestamp in milliseconds.
     * @param endTime   End timestamp in milliseconds.
     */
    public void setTimeRange(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;

        // Remove previous sources to avoid multiple triggers
        if (mphq9Data != null) mergedData.removeSource(mphq9Data);
        if (screenTimeData != null) mergedData.removeSource(screenTimeData);
        if (categoryUsageData != null) mergedData.removeSource(categoryUsageData);
        if (appUsageData != null) mergedData.removeSource(appUsageData);
        if (screenEventCountsData != null) mergedData.removeSource(screenEventCountsData);

        // Initialize LiveData sources with data from repositories
        mphq9Data = mphq9Repository.getAssessmentsBetween(startTime, endTime);
        screenTimeData = dailyScreenTimeRepository.getScreenTimeBetween(startTime, endTime);
        categoryUsageData = dailyCategoryUsageRepository.getUsageBetweenLiveData(startTime, endTime);
        appUsageData = dailyAppUsageRepository.getUsageBetweenLiveData(startTime, endTime);
        screenEventCountsData = screenEventRepository.getScreenEventCountsBetween(startTime, endTime);

        // Log to verify initialization
        Log.d("InsightsViewModel", "LiveData sources initialized.");
        mergeData();

        // Observe screenEventCountsData for verification
        screenEventCountsData.observeForever(screenEventCounts -> {
            Log.d("InsightsViewModel", "Screen Event Counts Data: " + screenEventCounts);
        });
    }
    /**
     * Returns the merged data as LiveData.
     *
     * @return LiveData containing a list of MergedData.
     */
    public LiveData<List<MergedData>> getMergedData() {
        return mergedData;
    }

    /**
     * Merges data from all sources based on the date.
     */
    private void mergeData() {
        // Add sources to MediatorLiveData
        mergedData.addSource(mphq9Data, mphq9List -> combineData());
        mergedData.addSource(screenTimeData, screenTimeList -> combineData());
        mergedData.addSource(categoryUsageData, categoryUsageList -> combineData());
        mergedData.addSource(appUsageData, appUsageList -> combineData());
        mergedData.addSource(screenEventCountsData, counts -> {
            // Add logging here
            Log.d("InsightsViewModel", "Received new screen event counts: " + counts);
            if (counts != null) {
                for (ScreenEventCount count : counts) {
                    Log.d("InsightsViewModel", "ScreenEventCount - Date: " + count.date + ", Count: " + count.count);
                }
            } else {
                Log.d("InsightsViewModel", "ScreenEventCountsData is null.");
            }
            combineData();
        });
    }

    /**
     * Combines data from all sources into a unified list of MergedData.
     */
    private void combineData() {
        // Retrieve data from LiveData sources
        List<MPHQ9Entity> mphq9List = mphq9Data != null ? mphq9Data.getValue() : null;
        List<DailyScreenTimeEntity> screenTimeList = screenTimeData != null ? screenTimeData.getValue() : null;
        List<DailyCategoryUsageEntity> categoryUsageList = categoryUsageData != null ? categoryUsageData.getValue() : null;
        List<DailyAppUsageEntity> appUsageList = appUsageData != null ? appUsageData.getValue() : null;
        List<ScreenEventCount> screenEventCountList = screenEventCountsData != null ? screenEventCountsData.getValue() : null;

        if (mphq9List == null || mphq9List.isEmpty()) {
            mergedData.setValue(new ArrayList<>());
            return;
        }

        // Use a set to gather all unique dates where we have MPHQ9 scores
        HashSet<Long> uniqueDates = new HashSet<>();
        for (MPHQ9Entity entity : mphq9List) {
            uniqueDates.add(truncateToDate(entity.timestamp));
        }

        // Logging unique dates
        Log.d("InsightsViewModel", "Unique Dates (MPHQ9 Days):");
        for (Long date : uniqueDates) {
            Log.d("InsightsViewModel", "Date: " + formatDate(date));
        }

        // Create maps for each data type with date as the key
        Map<Long, List<MPHQ9Entity>> mphq9Map = new HashMap<>();
        for (MPHQ9Entity entity : mphq9List) {
            long date = truncateToDate(entity.timestamp);
            mphq9Map.computeIfAbsent(date, k -> new ArrayList<>()).add(entity);
        }

        Map<Long, DailyScreenTimeEntity> screenTimeMap = new HashMap<>();
        if (screenTimeList != null) {
            for (DailyScreenTimeEntity entity : screenTimeList) {
                long date = truncateToDate(entity.date);
                screenTimeMap.put(date, entity);
            }
        }

        Map<Long, Map<String, Long>> categoryUsageMap = new HashMap<>();
        if (categoryUsageList != null) {
            for (DailyCategoryUsageEntity entity : categoryUsageList) {
                long date = truncateToDate(entity.date);
                categoryUsageMap
                        .computeIfAbsent(date, k -> new HashMap<>())
                        .put(entity.categoryName, entity.totalTimeInForeground);
            }
        }

        Map<Long, Map<String, Long>> appUsageMap = new HashMap<>();
        if (appUsageList != null) {
            for (DailyAppUsageEntity entity : appUsageList) {
                long date = truncateToDate(entity.date);
                appUsageMap
                        .computeIfAbsent(date, k -> new HashMap<>())
                        .put(entity.packageName, entity.totalTimeInForeground);
            }
        }

        Map<Long, Integer> screenEventCountMap = new HashMap<>();
        if (screenEventCountList != null) {
            for (ScreenEventCount count : screenEventCountList) {
                long date = truncateToDate(count.date);
                screenEventCountMap.merge(date, count.count, Integer::sum);
            }
        }

        // Logging screenEventCountMap after building it
        Log.d("InsightsViewModel", "Screen Event Count Map:");
        for (Map.Entry<Long, Integer> entry : screenEventCountMap.entrySet()) {
            String dateStr = formatDate(entry.getKey());
            Log.d("InsightsViewModel", "Date: " + dateStr + ", Screen Event Count: " + entry.getValue());
        }

        List<MergedData> mergedList = new ArrayList<>();

        for (Long date : uniqueDates) {
            MergedData mergedDataItem = new MergedData();
            mergedDataItem.date = date;

            // Average MPHQ9 scores if multiple per day
            List<MPHQ9Entity> mphq9Entities = mphq9Map.get(date);
            if (mphq9Entities != null && !mphq9Entities.isEmpty()) {
                double sum = 0;
                for (MPHQ9Entity e : mphq9Entities) {
                    sum += e.averageScore;
                }
                float avgScore = (float) (sum / mphq9Entities.size()); // Cast the result to float
                MPHQ9Entity averagedEntity = new MPHQ9Entity();
                averagedEntity.averageScore = avgScore;
                averagedEntity.timestamp = date;
                mergedDataItem.mphq9Entity = averagedEntity;
            }

            mergedDataItem.screenTimeEntity = screenTimeMap.get(date);
            mergedDataItem.categoryUsageMap = categoryUsageMap.get(date);
            mergedDataItem.appUsageMap = appUsageMap.get(date);
            mergedDataItem.screenOnOffCount = screenEventCountMap.getOrDefault(date, 0);
            mergedList.add(mergedDataItem);
        }

        // Sort the mergedList by date ascending
        mergedList.sort((a, b) -> Long.compare(a.date, b.date));

        // Logging screenOnOffCount for each merged data entry
        Log.d("InsightsViewModel", "Merged Data Screen Event Counts for MPHQ Days:");
        for (MergedData mergedDataItem : mergedList) {
            String dateStr = formatDate(mergedDataItem.date);
            int screenCount = mergedDataItem.screenOnOffCount;
            Log.d("InsightsViewModel", "Date: " + dateStr + ", Screen On/Off Count: " + screenCount);
        }

        mergedData.setValue(mergedList);
    }

    /**
     * Truncates a timestamp to the start of the day (midnight).
     *
     * @param timestamp The original timestamp in milliseconds.
     * @return The truncated timestamp in milliseconds.
     */
    private long truncateToDate(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        // Reset time to midnight
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }


    /**
     * Holds merged data from all sources for a specific date.
     */
    public static class MergedData {
        public long date;
        public MPHQ9Entity mphq9Entity;
        public DailyScreenTimeEntity screenTimeEntity;
        public Map<String, Long> categoryUsageMap; // categoryName -> usageTime
        public Map<String, Long> appUsageMap;      // packageName -> usageTime
        public int screenOnOffCount;
    }

    /**
     * Holds summary statistics for various metrics.
     */
    public static class SummaryStatistics {
        public double mphq9Mean;
        public double mphq9Median;
        public double mphq9StdDev;

        public List<AppUsageStat> topAppsUsageStats;
        public Map<String, CategoryUsageStat> categoryUsageStats;

        public double totalScreenTimeMean;
        public double totalScreenTimeMedian;
        public double totalScreenTimeStdDev;
        public double totalScreenTimeSum;

        public double screenOnOffCountMean;
        public double screenOnOffCountMedian;
        public double screenOnOffCountStdDev;
        public int screenOnOffCountSum;
    }



    public static class AppUsageStat {
        public String appName;
        public double meanUsage;
        public double medianUsage;
        public double stdDevUsage;
        public double totalUsage;
    }


    public static class CategoryUsageStat {
        public String categoryName;
        public double meanUsage;
        public double medianUsage;
        public double stdDevUsage;
        public double totalUsage;
    }

    /**
     * Computes descriptive statistics as per the requirements.
     *
     * @param data List of merged data.
     * @return SummaryStatistics object containing computed statistics.
     */
    public SummaryStatistics computeDescriptiveStatistics(List<MergedData> data, double usageThreshold) {
        SummaryStatistics stats = new SummaryStatistics();
        DescriptiveStatistics mphq9Stats = new DescriptiveStatistics();
        DescriptiveStatistics screenOnOffStats = new DescriptiveStatistics();
        DescriptiveStatistics totalScreenTimeStats = new DescriptiveStatistics();

        Map<String, DescriptiveStatistics> appUsageStatsMap = new HashMap<>();
        Map<String, DescriptiveStatistics> categoryUsageStatsMap = new HashMap<>();

        // Collect all app names and category names across all data
        HashSet<String> allAppNames = new HashSet<>();
        HashSet<String> allCategoryNames = new HashSet<>();

        for (MergedData item : data) {
            if (item.appUsageMap != null) {
                allAppNames.addAll(item.appUsageMap.keySet());
            }
            if (item.categoryUsageMap != null) {
                allCategoryNames.addAll(item.categoryUsageMap.keySet());
            }
        }

        // Initialize DescriptiveStatistics for each app and category
        for (String appName : allAppNames) {
            appUsageStatsMap.put(appName, new DescriptiveStatistics());
        }
        for (String categoryName : allCategoryNames) {
            categoryUsageStatsMap.put(categoryName, new DescriptiveStatistics());
        }

        for (MergedData item : data) {
            // Aggregate MPHQ-9 scores
            if (item.mphq9Entity != null) {
                mphq9Stats.addValue(item.mphq9Entity.averageScore);
            }

            // Aggregate Screen Time (convert milliseconds to hours)
            if (item.screenTimeEntity != null) {
                double screenTimeHours = item.screenTimeEntity.totalScreenTime / 3600000.0; // Convert ms to hours
                totalScreenTimeStats.addValue(screenTimeHours);
                stats.totalScreenTimeSum += screenTimeHours; // Accumulate total screen time
            } else {
                totalScreenTimeStats.addValue(0.0);
            }

            // Aggregate Screen On/Off Counts
            screenOnOffStats.addValue(item.screenOnOffCount);
            stats.screenOnOffCountSum += item.screenOnOffCount;

            // Aggregate App Usage (convert milliseconds to hours)
            for (String appName : allAppNames) {
                double usageTimeHours = 0.0;
                if (item.appUsageMap != null) {
                    usageTimeHours = item.appUsageMap.getOrDefault(appName, 0L) / 3600000.0; // Convert ms to hours
                }
                appUsageStatsMap.get(appName).addValue(usageTimeHours);
            }

            // Aggregate Category Usage (convert milliseconds to hours)
            for (String categoryName : allCategoryNames) {
                double usageTimeHours = 0.0;
                if (item.categoryUsageMap != null) {
                    usageTimeHours = item.categoryUsageMap.getOrDefault(categoryName, 0L) / 3600000.0; // Convert ms to hours
                }
                categoryUsageStatsMap.get(categoryName).addValue(usageTimeHours);
            }
        }

        if (mphq9Stats.getN() > 0) {
            stats.mphq9Mean = mphq9Stats.getMean();
            stats.mphq9Median = mphq9Stats.getPercentile(50);
            stats.mphq9StdDev = mphq9Stats.getStandardDeviation();
        }

        if (totalScreenTimeStats.getN() > 0) {
            stats.totalScreenTimeMean = totalScreenTimeStats.getMean();
            stats.totalScreenTimeMedian = totalScreenTimeStats.getPercentile(50);
            stats.totalScreenTimeStdDev = totalScreenTimeStats.getStandardDeviation();
        }

        // Populate Screen On/Off stats
        if (screenOnOffStats.getN() > 0) {
            stats.screenOnOffCountMean = screenOnOffStats.getMean();
            stats.screenOnOffCountMedian = screenOnOffStats.getPercentile(50);
            stats.screenOnOffCountStdDev = screenOnOffStats.getStandardDeviation();
        }

        // Compute usage stats for each app
        List<AppUsageStat> appUsageStatsList = new ArrayList<>();
        for (Map.Entry<String, DescriptiveStatistics> entry : appUsageStatsMap.entrySet()) {
            String appName = entry.getKey();
            DescriptiveStatistics usageStats = entry.getValue();

            AppUsageStat appStat = new AppUsageStat();
            appStat.appName = appName;
            appStat.meanUsage = usageStats.getMean();
            appStat.medianUsage = usageStats.getPercentile(50);
            appStat.stdDevUsage = usageStats.getStandardDeviation();
            appStat.totalUsage = usageStats.getSum();

            appUsageStatsList.add(appStat);
        }

        // Filter apps with average usage above threshold (threshold is in hours)
        List<AppUsageStat> filteredAppUsageStats = new ArrayList<>();
        for (AppUsageStat appStat : appUsageStatsList) {
            if (appStat.meanUsage >= usageThreshold) { // usageThreshold is now in hours
                filteredAppUsageStats.add(appStat);
            }
        }

        // Sort by mean usage descending
        filteredAppUsageStats.sort((a, b) -> Double.compare(b.meanUsage, a.meanUsage));

        // Get top seven most used apps
        stats.topAppsUsageStats = filteredAppUsageStats.subList(0, Math.min(7, filteredAppUsageStats.size()));

        // Compute usage stats for each category
        stats.categoryUsageStats = new HashMap<>();
        for (Map.Entry<String, DescriptiveStatistics> entry : categoryUsageStatsMap.entrySet()) {
            String categoryName = entry.getKey();
            DescriptiveStatistics usageStats = entry.getValue();

            CategoryUsageStat categoryStat = new CategoryUsageStat();
            categoryStat.categoryName = categoryName;
            categoryStat.meanUsage = usageStats.getMean();
            categoryStat.medianUsage = usageStats.getPercentile(50);
            categoryStat.stdDevUsage = usageStats.getStandardDeviation();
            categoryStat.totalUsage = usageStats.getSum();

            stats.categoryUsageStats.put(categoryName, categoryStat);
        }

        return stats;
    }

    /**
     * Computes correlations between app usage and MPHQ-9 scores.
     *
     * @param data                  List of merged data.
     * @param filteredAppUsageStats List of AppUsageStat with apps above usage threshold.
     * @return Map of app package names to correlation coefficients.
     */
    private Map<String, Double> computeAppCorrelations(List<MergedData> data, List<AppUsageStat> filteredAppUsageStats) {
        Map<String, Double> appCorrelations = new HashMap<>();

        // Initialize data structures
        List<Double> mphq9Scores = new ArrayList<>();
        Map<String, List<Double>> appUsageLists = new HashMap<>();
        HashSet<String> appNames = new HashSet<>();
        for (AppUsageStat appStat : filteredAppUsageStats) {
            appNames.add(appStat.appName);
            appUsageLists.put(appStat.appName, new ArrayList<>());
        }

        // Collect data day by day
        for (MergedData item : data) {
            if (item.mphq9Entity != null) {
                mphq9Scores.add((double) item.mphq9Entity.averageScore);

                for (String appName : appNames) {
                    double usageTime = 0.0;
                    if (item.appUsageMap != null) {
                        usageTime = item.appUsageMap.getOrDefault(appName, 0L);
                    }
                    appUsageLists.get(appName).add(usageTime);
                }
            }
        }

        double[] mphq9Array = mphq9Scores.stream().mapToDouble(Double::doubleValue).toArray();

        // Compute correlations
        for (String appName : appNames) {
            List<Double> usageList = appUsageLists.get(appName);
            if (usageList.size() >= 2) {
                double[] usageArray = usageList.stream().mapToDouble(Double::doubleValue).toArray();
                double correlation = new PearsonsCorrelation().correlation(usageArray, mphq9Array);
                appCorrelations.put(appName, correlation);
            }
        }

        return appCorrelations;
    }

    /**
     * Holds the results of correlation analyses.
     */
    public static class CorrelationResult {
        public String variableName;
        public double correlationCoefficient;
    }

    /**
     * Computes contemporaneous Pearson correlations between top 7 most used apps and MPHQ-9 scores.
     *
     * @param data  List of merged data.
     * @param stats SummaryStatistics object containing top 7 apps.
     * @return List of CorrelationResult.
     */
    public List<CorrelationResult> computeContemporaneousCorrelations(List<MergedData> data, SummaryStatistics stats) {
        List<CorrelationResult> correlationResults = new ArrayList<>();

        // Prepare data lists
        List<Double> mphq9Scores = new ArrayList<>();
        List<Double> screenTimes = new ArrayList<>();
        List<Double> screenOnOffCounts = new ArrayList<>();
        Map<String, List<Double>> appUsageLists = new HashMap<>();
        Map<String, List<Double>> categoryUsageLists = new HashMap<>();

        // Use top 7 apps from the statistics
        List<AppUsageStat> topApps = stats.topAppsUsageStats;

        HashSet<String> appNames = new HashSet<>();
        for (AppUsageStat appStat : topApps) {
            appNames.add(appStat.appName);
            appUsageLists.put(appStat.appName, new ArrayList<>());
        }

        HashSet<String> allCategories = new HashSet<>();

        // Collect all category names across the data
        for (MergedData item : data) {
            if (item.categoryUsageMap != null) {
                allCategories.addAll(item.categoryUsageMap.keySet());
            }
        }

        // Process data day by day; build aligned lists
        for (MergedData item : data) {
            if (item.mphq9Entity != null) {
                mphq9Scores.add((double) item.mphq9Entity.averageScore);

                // Screen time
                if (item.screenTimeEntity != null) {
                    screenTimes.add((double) item.screenTimeEntity.totalScreenTime);
                } else {
                    screenTimes.add(0.0);
                }

                // Screen on/off counts
                screenOnOffCounts.add((double) item.screenOnOffCount);

                // App usage
                for (String appName : appNames) {
                    double usageTime = 0.0;
                    if (item.appUsageMap != null) {
                        usageTime = item.appUsageMap.getOrDefault(appName, 0L);
                    }
                    appUsageLists.get(appName).add(usageTime);
                }

                // Category usage
                for (String categoryName : allCategories) {
                    double usageTime = 0.0;
                    if (item.categoryUsageMap != null) {
                        usageTime = item.categoryUsageMap.getOrDefault(categoryName, 0L);
                    }
                    categoryUsageLists.computeIfAbsent(categoryName, k -> new ArrayList<>()).add(usageTime);
                }
            }
        }

        double[] mphq9Array = mphq9Scores.stream().mapToDouble(Double::doubleValue).toArray();

        // Compute correlations for screen time
        double[] screenTimeArray = screenTimes.stream().mapToDouble(Double::doubleValue).toArray();
        if (screenTimeArray.length >= 2) {
            double screenTimeCorrelation = new PearsonsCorrelation().correlation(screenTimeArray, mphq9Array);
            CorrelationResult screenTimeResult = new CorrelationResult();
            screenTimeResult.variableName = "Total Screen Time";
            screenTimeResult.correlationCoefficient = screenTimeCorrelation;
            correlationResults.add(screenTimeResult);
        }

        // Compute correlations for screen on/off counts
        double[] screenOnOffArray = screenOnOffCounts.stream().mapToDouble(Double::doubleValue).toArray();
        if (screenOnOffArray.length >= 2) {
            double screenOnOffCorrelation = new PearsonsCorrelation().correlation(screenOnOffArray, mphq9Array);
            CorrelationResult screenOnOffResult = new CorrelationResult();
            screenOnOffResult.variableName = "Screen On/Off Events";
            screenOnOffResult.correlationCoefficient = screenOnOffCorrelation;
            correlationResults.add(screenOnOffResult);
        }

        // Compute correlations for top 7 apps
        for (String appName : appNames) {
            List<Double> usageList = appUsageLists.get(appName);
            if (usageList.size() >= 2) {
                double[] usageArray = usageList.stream().mapToDouble(Double::doubleValue).toArray();
                double correlation = new PearsonsCorrelation().correlation(usageArray, mphq9Array);
                CorrelationResult appResult = new CorrelationResult();
                appResult.variableName = "App: " + appName;
                appResult.correlationCoefficient = correlation;
                correlationResults.add(appResult);
            }
        }

        // Compute correlations for categories
        for (String categoryName : categoryUsageLists.keySet()) {
            List<Double> usageList = categoryUsageLists.get(categoryName);
            if (usageList.size() >= 2) {
                double[] usageArray = usageList.stream().mapToDouble(Double::doubleValue).toArray();
                double correlation = new PearsonsCorrelation().correlation(usageArray, mphq9Array);
                CorrelationResult categoryResult = new CorrelationResult();
                categoryResult.variableName = "Category: " + categoryName;
                categoryResult.correlationCoefficient = correlation;
                correlationResults.add(categoryResult);
            }
        }

        return correlationResults;
    }

    /**
     * Computes contemporaneous multiple regression analysis using top 7 most used apps.
     *
     * @param data  List of merged data.
     * @param stats SummaryStatistics object containing top 7 apps.
     * @return RegressionResult containing coefficients and R-squared.
     */
    public RegressionResult performContemporaneousRegression(List<MergedData> data, SummaryStatistics stats) {
        // Prepare data
        List<Double> mphq9Scores = new ArrayList<>();
        List<double[]> predictorsList = new ArrayList<>();
        List<String> variableNames = new ArrayList<>();

        // Collect predictors
        Map<String, Integer> appIndexMap = new HashMap<>();
        int index = 0;

        // Use top 7 apps from the statistics
        List<AppUsageStat> topApps = stats.topAppsUsageStats;

        // Initialize app index map and variable names
        for (AppUsageStat appStat : topApps) {
            String appName = appStat.appName;
            appIndexMap.put(appName, index++);
            variableNames.add("App: " + appName);
        }

        // Collect data
        for (MergedData item : data) {
            if (item.mphq9Entity != null) {
                mphq9Scores.add((double) item.mphq9Entity.averageScore);
                double[] predictors = new double[appIndexMap.size() + 2]; // +2 for screen time and screen on/off

                // Screen time
                if (item.screenTimeEntity != null) {
                    predictors[0] = item.screenTimeEntity.totalScreenTime;
                } else {
                    predictors[0] = 0.0;
                }

                // Screen on/off counts
                predictors[1] = item.screenOnOffCount;

                // App usage
                for (String appName : appIndexMap.keySet()) {
                    int idx = appIndexMap.get(appName) + 2; // +2 offset
                    double usageTime = 0.0;
                    if (item.appUsageMap != null) {
                        usageTime = item.appUsageMap.getOrDefault(appName, 0L);
                    }
                    predictors[idx] = usageTime;
                }

                predictorsList.add(predictors);
            }
        }

        int numberOfPredictors = appIndexMap.size() + 2; // Apps + screen time + screen on/off
        int numberOfDataPoints = mphq9Scores.size();

        // Validation check
        if (numberOfDataPoints <= numberOfPredictors) {
            Log.w("InsightsViewModel", "Insufficient data for regression: " + numberOfDataPoints + " data points, " + numberOfPredictors + " predictors.");
            // Inform the user via LiveData or another mechanism
            regressionErrorLiveData.postValue("Insufficient data to perform regression analysis.");
            return new RegressionResult(); // Return empty result
        }

        double[] y = mphq9Scores.stream().mapToDouble(Double::doubleValue).toArray();
        double[][] x = predictorsList.toArray(new double[0][]);

        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.setNoIntercept(true);
        try {
            regression.newSampleData(y, x);
            double[] coefficients = regression.estimateRegressionParameters();
            double rSquared = regression.calculateRSquared();

            RegressionResult result = new RegressionResult();
            result.coefficients = coefficients;
            result.rSquared = rSquared;

            // Add variable names for screen time and screen on/off counts
            variableNames.add(0, "Screen Time");
            variableNames.add(1, "Screen On/Off Events");
            result.variableNames = variableNames.toArray(new String[0]);

            return result;
        } catch (MathIllegalArgumentException e) {
            Log.e("InsightsViewModel", "Regression failed: " + e.getMessage());
            // Inform the user via LiveData or another mechanism
            regressionErrorLiveData.postValue("Regression analysis failed due to insufficient data.");
            return new RegressionResult();
        } catch (Exception e) {
            Log.e("InsightsViewModel", "Unexpected error during regression: " + e.getMessage());
            // Inform the user via LiveData or another mechanism
            regressionErrorLiveData.postValue("An unexpected error occurred during regression analysis.");
            return new RegressionResult();
        }
    }

    /**
     * Computes lagged Pearson correlations between top 7 most used apps and MPHQ-9 scores.
     *
     * @param data  List of merged data.
     * @param stats SummaryStatistics object containing top 7 apps.
     * @return List of CorrelationResult.
     */
    public List<CorrelationResult> computeLaggedCorrelations(List<MergedData> data, SummaryStatistics stats) {
        List<CorrelationResult> correlationResults = new ArrayList<>();

        // Prepare data lists
        List<Double> mphq9Scores = new ArrayList<>();
        List<Double> laggedScreenTimes = new ArrayList<>();
        List<Double> laggedScreenOnOffCounts = new ArrayList<>();
        Map<String, List<Double>> laggedAppUsageLists = new HashMap<>();
        Map<String, List<Double>> laggedCategoryUsageLists = new HashMap<>();

        // Use top 7 apps from the statistics
        List<AppUsageStat> topApps = stats.topAppsUsageStats;

        HashSet<String> appNames = new HashSet<>();
        for (AppUsageStat appStat : topApps) {
            appNames.add(appStat.appName);
            laggedAppUsageLists.put(appStat.appName, new ArrayList<>());
        }

        HashSet<String> allCategories = new HashSet<>();

        // Collect all category names across the data
        for (MergedData item : data) {
            if (item.categoryUsageMap != null) {
                allCategories.addAll(item.categoryUsageMap.keySet());
            }
        }

        // Create a map from date to MergedData for easy access
        Map<Long, MergedData> dateToDataMap = new HashMap<>();
        for (MergedData item : data) {
            dateToDataMap.put(item.date, item);
        }

        // For each date with MPH-Q9 data, get the phone usage data from the previous day
        for (MergedData item : data) {
            if (item.mphq9Entity != null) {
                long currentDate = item.date;
                long previousDate = currentDate - 24 * 60 * 60 * 1000; // Subtract one day in milliseconds

                MergedData laggedData = dateToDataMap.get(previousDate);

                mphq9Scores.add((double) item.mphq9Entity.averageScore);

                // Lagged screen time
                if (laggedData != null && laggedData.screenTimeEntity != null) {
                    laggedScreenTimes.add((double) laggedData.screenTimeEntity.totalScreenTime);
                } else {
                    laggedScreenTimes.add(0.0);
                }

                // Lagged screen on/off counts
                if (laggedData != null) {
                    laggedScreenOnOffCounts.add((double) laggedData.screenOnOffCount);
                } else {
                    laggedScreenOnOffCounts.add(0.0);
                }

                // Lagged app usage
                for (String appName : appNames) {
                    double usageTime = 0.0;
                    if (laggedData != null && laggedData.appUsageMap != null) {
                        usageTime = laggedData.appUsageMap.getOrDefault(appName, 0L);
                    }
                    laggedAppUsageLists.get(appName).add(usageTime);
                }

                // Lagged category usage
                for (String categoryName : allCategories) {
                    double usageTime = 0.0;
                    if (laggedData != null && laggedData.categoryUsageMap != null) {
                        usageTime = laggedData.categoryUsageMap.getOrDefault(categoryName, 0L);
                    }
                    laggedCategoryUsageLists.computeIfAbsent(categoryName, k -> new ArrayList<>()).add(usageTime);
                }
            }
        }

        double[] mphq9Array = mphq9Scores.stream().mapToDouble(Double::doubleValue).toArray();

        // Compute correlations for lagged screen time
        double[] laggedScreenTimeArray = laggedScreenTimes.stream().mapToDouble(Double::doubleValue).toArray();
        if (laggedScreenTimeArray.length >= 2) {
            double screenTimeCorrelation = new PearsonsCorrelation().correlation(laggedScreenTimeArray, mphq9Array);
            CorrelationResult screenTimeResult = new CorrelationResult();
            screenTimeResult.variableName = "Lagged Total Screen Time";
            screenTimeResult.correlationCoefficient = screenTimeCorrelation;
            correlationResults.add(screenTimeResult);
        }

        // Compute correlations for lagged screen on/off counts
        double[] laggedScreenOnOffArray = laggedScreenOnOffCounts.stream().mapToDouble(Double::doubleValue).toArray();
        if (laggedScreenOnOffArray.length >= 2) {
            double screenOnOffCorrelation = new PearsonsCorrelation().correlation(laggedScreenOnOffArray, mphq9Array);
            CorrelationResult screenOnOffResult = new CorrelationResult();
            screenOnOffResult.variableName = "Lagged Screen On/Off Events";
            screenOnOffResult.correlationCoefficient = screenOnOffCorrelation;
            correlationResults.add(screenOnOffResult);
        }

        // Compute correlations for lagged apps
        for (String appName : appNames) {
            List<Double> usageList = laggedAppUsageLists.get(appName);
            if (usageList.size() >= 2) {
                double[] usageArray = usageList.stream().mapToDouble(Double::doubleValue).toArray();
                double correlation = new PearsonsCorrelation().correlation(usageArray, mphq9Array);
                CorrelationResult appResult = new CorrelationResult();
                appResult.variableName = "Lagged App: " + appName;
                appResult.correlationCoefficient = correlation;
                correlationResults.add(appResult);
            }
        }

        // Compute correlations for lagged categories
        for (String categoryName : laggedCategoryUsageLists.keySet()) {
            List<Double> usageList = laggedCategoryUsageLists.get(categoryName);
            if (usageList.size() >= 2) {
                double[] usageArray = usageList.stream().mapToDouble(Double::doubleValue).toArray();
                double correlation = new PearsonsCorrelation().correlation(usageArray, mphq9Array);
                CorrelationResult categoryResult = new CorrelationResult();
                categoryResult.variableName = "Lagged Category: " + categoryName;
                categoryResult.correlationCoefficient = correlation;
                correlationResults.add(categoryResult);
            }
        }

        return correlationResults;
    }


    /**
     * Performs lagged multiple regression analysis using top 7 most used apps.
     *
     * @param data  List of merged data.
     * @param stats SummaryStatistics object containing top 7 apps.
     * @return RegressionResult containing coefficients and R-squared.
     */
    public RegressionResult performLaggedRegression(List<MergedData> data, SummaryStatistics stats) {
        // Prepare data
        List<Double> mphq9Scores = new ArrayList<>();
        List<double[]> predictorsList = new ArrayList<>();
        List<String> variableNames = new ArrayList<>();

        // Collect predictors
        Map<String, Integer> appIndexMap = new HashMap<>();
        int index = 0;

        // Use top 7 apps from the statistics
        List<AppUsageStat> topApps = stats.topAppsUsageStats;

        // Initialize app index map and variable names
        for (AppUsageStat appStat : topApps) {
            String appName = appStat.appName;
            appIndexMap.put(appName, index++);
            variableNames.add("Lagged App: " + appName);
        }

        // Create a map from date to MergedData for easy access
        Map<Long, MergedData> dateToDataMap = new HashMap<>();
        for (MergedData item : data) {
            dateToDataMap.put(item.date, item);
        }

        // Collect data
        for (MergedData item : data) {
            if (item.mphq9Entity != null) {
                long currentDate = item.date;
                long previousDate = currentDate - 24 * 60 * 60 * 1000; // Subtract one day in milliseconds

                MergedData laggedData = dateToDataMap.get(previousDate);

                mphq9Scores.add((double) item.mphq9Entity.averageScore);
                double[] predictors = new double[appIndexMap.size() + 2]; // +2 for lagged screen time and lagged screen on/off

                // Lagged Screen time
                if (laggedData != null && laggedData.screenTimeEntity != null) {
                    predictors[0] = laggedData.screenTimeEntity.totalScreenTime;
                } else {
                    predictors[0] = 0.0;
                }

                // Lagged Screen on/off counts
                if (laggedData != null) {
                    predictors[1] = laggedData.screenOnOffCount;
                } else {
                    predictors[1] = 0.0;
                }

                // Lagged App usage
                for (String appName : appIndexMap.keySet()) {
                    int idx = appIndexMap.get(appName) + 2; // +2 offset
                    double usageTime = 0.0;
                    if (laggedData != null && laggedData.appUsageMap != null) {
                        usageTime = laggedData.appUsageMap.getOrDefault(appName, 0L);
                    }
                    predictors[idx] = usageTime;
                }

                predictorsList.add(predictors);
            }
        }

        int numberOfPredictors = appIndexMap.size() + 2; // Apps + lagged screen time + lagged screen on/off
        int numberOfDataPoints = mphq9Scores.size();

        // Validation check
        if (numberOfDataPoints <= numberOfPredictors) {
            Log.w("InsightsViewModel", "Insufficient data for lagged regression: " + numberOfDataPoints + " data points, " + numberOfPredictors + " predictors.");
            // Inform the user via LiveData or another mechanism
            regressionErrorLiveData.postValue("Insufficient data to perform lagged regression analysis.");
            return new RegressionResult(); // Return empty result
        }

        double[] y = mphq9Scores.stream().mapToDouble(Double::doubleValue).toArray();
        double[][] x = predictorsList.toArray(new double[0][]);

        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.setNoIntercept(true);
        try {
            regression.newSampleData(y, x);
            double[] coefficients = regression.estimateRegressionParameters();
            double rSquared = regression.calculateRSquared();

            RegressionResult result = new RegressionResult();
            result.coefficients = coefficients;
            result.rSquared = rSquared;

            // Add variable names for lagged screen time and lagged screen on/off counts
            variableNames.add(0, "Lagged Screen Time");
            variableNames.add(1, "Lagged Screen On/Off Events");
            result.variableNames = variableNames.toArray(new String[0]);

            return result;
        } catch (MathIllegalArgumentException e) {
            Log.e("InsightsViewModel", "Lagged Regression failed: " + e.getMessage());
            // Inform the user via LiveData or another mechanism
            regressionErrorLiveData.postValue("Lagged regression analysis failed due to insufficient data.");
            return new RegressionResult();
        } catch (Exception e) {
            Log.e("InsightsViewModel", "Unexpected error during lagged regression: " + e.getMessage());
            // Inform the user via LiveData or another mechanism
            regressionErrorLiveData.postValue("An unexpected error occurred during lagged regression analysis.");
            return new RegressionResult();
        }
    }

    /**
     * Holds the results of multiple regression analysis.
     */
    public static class RegressionResult {
        public double[] coefficients = new double[0]; // Initialize to empty array
        public double rSquared = 0.0;                // Default value
        public String[] variableNames = new String[0]; // Initialize to empty array
    }

    /**
     * Generates actionable insights based on the analyses.
     *
     * @param correlations      List of contemporaneous correlation results.
     * @param regression        Contemporaneous RegressionResult object.
     * @param laggedCorrelations List of lagged correlation results.
     * @param laggedRegression   Lagged RegressionResult object.
     * @return A string containing actionable insights.
     */
    public String generateInsights(List<CorrelationResult> correlations, RegressionResult regression,
                                   List<CorrelationResult> laggedCorrelations, RegressionResult laggedRegression) {
        StringBuilder insights = new StringBuilder();

        insights.append("Hey there! We've analyzed your recent data and found some interesting patterns that might help you understand your well-being better.\n\n");

        // Analyze significant contemporaneous correlations
        for (CorrelationResult corr : correlations) {
            double absCorr = Math.abs(corr.correlationCoefficient);
            if (absCorr >= 0.5) {
                String direction = corr.correlationCoefficient > 0 ? "increase" : "decrease";
                insights.append(String.format("✨ It seems that when you use %s more, your depression scores tend to %s.\n", corr.variableName, direction));
            }
        }

        // Analyze significant regression coefficients
        if (regression.coefficients != null && regression.variableNames != null) {
            for (int i = 0; i < regression.variableNames.length; i++) {
                double coeff = regression.coefficients[i];
                if (Math.abs(coeff) > 0.1) {
                    String direction = coeff > 0 ? "increase" : "decrease";
                    insights.append(String.format("📈 An increase in %s might lead to an %s in your depression scores.\n", regression.variableNames[i], direction));
                }
            }
        }

        insights.append("\nBased on this, you might consider:\n");

        // Provide behavioral suggestions
        for (CorrelationResult corr : correlations) {
            double absCorr = Math.abs(corr.correlationCoefficient);
            if (absCorr >= 0.5) {
                String direction = corr.correlationCoefficient > 0 ? "reducing" : "increasing";
                String action = corr.variableName.startsWith("App: ") ? "usage of " + corr.variableName.substring(5) :
                        corr.variableName.startsWith("Category: ") ? "time spent on " + corr.variableName.substring(10) :
                                corr.variableName.toLowerCase();
                insights.append(String.format("📝 Try %s your %s to see if it positively affects your mood.\n", direction, action));
            }
        }

        insights.append("\nRemember, small changes can make a big difference! If you have concerns about your mental health, consider reaching out to a professional.\n");

        return insights.toString();
    }

    /**
     * Formats a timestamp to a readable date string (e.g., "2024-04-27").
     *
     * @param timestamp The timestamp in milliseconds.
     * @return A formatted date string.
     */
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // LiveData for regression errors
    private final MutableLiveData<String> regressionErrorLiveData = new MutableLiveData<>();

    /**
     * Exposes regression error messages as LiveData.
     *
     * @return LiveData containing regression error messages.
     */
    public LiveData<String> getRegressionError() {
        return regressionErrorLiveData;
    }

}
