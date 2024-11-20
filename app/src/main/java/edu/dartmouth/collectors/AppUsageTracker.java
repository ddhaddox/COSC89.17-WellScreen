// File: edu/dartmouth/collectors/AppUsageTracker.java
package edu.dartmouth.collectors;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.dartmouth.data.entities.DailyAppUsageEntity;
import edu.dartmouth.data.entities.DailyCategoryUsageEntity;
import edu.dartmouth.repositories.DailyAppUsageRepository;
import edu.dartmouth.repositories.DailyCategoryUsageRepository;

public class AppUsageTracker {
    private static final String TAG = "AppUsageTracker";
    private static final String PREFS_NAME = "AppUsagePrefs";
    private static final String KEY_LAST_COLLECTION_TIME = "last_collection_time";
    private static final long DEFAULT_COLLECTION_INTERVAL = 60 * 60 * 1000L;

    public static void collectAndSaveUsageDetails(Context context) {
        collectAndSaveUsageDetails(context, System.currentTimeMillis());
    }

    public static void collectAndSaveUsageDetails(Context context, long date) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager == null) {
            Log.e(TAG, "UsageStatsManager is null. Usage stats permission might not be granted.");
            return;
        }

        TimeRange dayRange = getDayTimeRange(date);
        List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                dayRange.start,
                dayRange.end
        );

        if (usageStatsList == null || usageStatsList.isEmpty()) {
            Log.d(TAG, "No usage stats available for date: " + dayRange.start);
            return;
        }

        try {
            ProcessedUsageData processedData = processUsageStats(context, usageStatsList, dayRange.start);
            saveProcessedData(context, processedData);

            // Update collection time only for current day
            if (isCurrentDay(date)) {
                updateLastCollectionTime(context, dayRange.end);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing usage stats", e);
        }
    }

    private static ProcessedUsageData processUsageStats(Context context, List<UsageStats> usageStatsList, long dayStart) {
        List<DailyAppUsageEntity> appUsageEntities = new ArrayList<>();
        Map<String, Long> categoryUsageMap = new HashMap<>();

        for (UsageStats stats : usageStatsList) {
            if (stats.getTotalTimeInForeground() <= 0) continue;

            DailyAppUsageEntity appEntity = createAppUsageEntity(context, stats, dayStart);
            appUsageEntities.add(appEntity);

            // Aggregate category usage
            categoryUsageMap.merge(
                    appEntity.categoryName,
                    appEntity.totalTimeInForeground,
                    Long::sum
            );
        }

        return new ProcessedUsageData(appUsageEntities, categoryUsageMap);
    }

    private static DailyAppUsageEntity createAppUsageEntity(Context context, UsageStats stats, long dayStart) {
        DailyAppUsageEntity entity = new DailyAppUsageEntity();
        entity.packageName = stats.getPackageName();
        entity.date = dayStart;
        entity.totalTimeInForeground = stats.getTotalTimeInForeground();
        entity.categoryName = AppCategoryHelper.getAppCategory(context, stats.getPackageName());
        return entity;
    }

    private static void saveProcessedData(Context context, ProcessedUsageData data) {
        DailyAppUsageRepository appUsageRepository = new DailyAppUsageRepository(context);
        DailyCategoryUsageRepository categoryUsageRepository = new DailyCategoryUsageRepository(context);

        if (!data.appUsageEntities.isEmpty()) {
            appUsageRepository.insertAll(data.appUsageEntities);
        }

        if (!data.categoryUsageMap.isEmpty()) {
            List<DailyCategoryUsageEntity> categoryEntities = createCategoryEntities(
                    data.categoryUsageMap,
                    data.appUsageEntities.get(0).date
            );
            categoryUsageRepository.insertAll(categoryEntities);
        }
    }

    private static List<DailyCategoryUsageEntity> createCategoryEntities(Map<String, Long> categoryUsageMap, long date) {
        List<DailyCategoryUsageEntity> entities = new ArrayList<>();
        for (Map.Entry<String, Long> entry : categoryUsageMap.entrySet()) {
            DailyCategoryUsageEntity entity = new DailyCategoryUsageEntity();
            entity.categoryName = entry.getKey();
            entity.date = date;
            entity.totalTimeInForeground = entry.getValue();
            entities.add(entity);
        }
        return entities;
    }

    private static TimeRange getDayTimeRange(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );

        LocalDateTime startOfDay = dateTime.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        return new TimeRange(
                startOfDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                endOfDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        );
    }

    private static boolean isCurrentDay(long timestamp) {
        TimeRange currentDayRange = getDayTimeRange(System.currentTimeMillis());
        return timestamp >= currentDayRange.start && timestamp < currentDayRange.end;
    }

    private static void updateLastCollectionTime(Context context, long time) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_COLLECTION_TIME, time)
                .apply();
    }

    public static long getLastCollectionTime(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_COLLECTION_TIME, 0L);
    }

    private static class TimeRange {
        final long start;
        final long end;

        TimeRange(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }

    private static class ProcessedUsageData {
        final List<DailyAppUsageEntity> appUsageEntities;
        final Map<String, Long> categoryUsageMap;

        ProcessedUsageData(List<DailyAppUsageEntity> appUsageEntities, Map<String, Long> categoryUsageMap) {
            this.appUsageEntities = appUsageEntities;
            this.categoryUsageMap = categoryUsageMap;
        }
    }
}
