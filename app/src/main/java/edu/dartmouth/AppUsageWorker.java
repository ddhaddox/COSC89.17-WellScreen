// File: edu/dartmouth/AppUsageWorker.java
package edu.dartmouth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.dartmouth.collectors.AppUsageTracker;
import edu.dartmouth.data.entities.DailyScreenTimeEntity;
import edu.dartmouth.data.entities.ScreenEventEntity;
import edu.dartmouth.repositories.DailyAppUsageRepository;
import edu.dartmouth.repositories.DailyCategoryUsageRepository;
import edu.dartmouth.repositories.DailyScreenTimeRepository;
import edu.dartmouth.repositories.ScreenEventRepository;

public class AppUsageWorker extends Worker {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LAST_PROCESSED_DAY = "last_processed_day";
    private static final String TAG = "AppUsageWorker";
    private static final int INTERVAL_ONE_WEEK = 0;
    private static final int INTERVAL_ONE_MONTH = 1;
    private static final int INTERVAL_THREE_MONTHS = 2;
    private static final int INTERVAL_SIX_MONTHS = 3;
    private static final int INTERVAL_ONE_YEAR = 4;
    private static final int INTERVAL_NEVER = 5;

    public AppUsageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        // Check if the user has enabled automatic deletion
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isAutoDeleteEnabled = prefs.getBoolean("auto_delete_old_data", false);

        if (isAutoDeleteEnabled) {
            // Read the selected interval
            int intervalIndex = prefs.getInt("auto_delete_interval", INTERVAL_ONE_WEEK);

            // Calculate the cutoff time based on the selected interval
            long cutoffTime = getCutoffTime(intervalIndex);

            if (intervalIndex != INTERVAL_NEVER) {
                // Initialize repositories
                DailyAppUsageRepository appUsageRepository = new DailyAppUsageRepository(context);
                DailyCategoryUsageRepository categoryUsageRepository = new DailyCategoryUsageRepository(context);
                DailyScreenTimeRepository dailyScreenTimeRepository = new DailyScreenTimeRepository(context);

                // Delete old data
                appUsageRepository.deleteOldData(cutoffTime);
                categoryUsageRepository.deleteOldData(cutoffTime);
                dailyScreenTimeRepository.deleteOldData(cutoffTime);
            }
        }

        // Get last processed day
        long lastProcessedDay = getLastProcessedDay(context);
        long todayStart = getStartOfDay(System.currentTimeMillis());

        if (lastProcessedDay == -1) {
            // If no last processed day, start from one day before today
            lastProcessedDay = todayStart - TimeUnit.DAYS.toMillis(1);
        }

        // Initialize repositories
        DailyAppUsageRepository appUsageRepository = new DailyAppUsageRepository(context);
        DailyCategoryUsageRepository categoryUsageRepository = new DailyCategoryUsageRepository(context);
        DailyScreenTimeRepository dailyScreenTimeRepository = new DailyScreenTimeRepository(context);

        // Loop over each day from last processed day up to today, inclusive
        for (long date = lastProcessedDay; date <= todayStart; date += TimeUnit.DAYS.toMillis(1)) {
            // Collect and save usage details for 'date'
            AppUsageTracker.collectAndSaveUsageDetails(context, date);

            // Compute and save total screen time for 'date'
            computeAndSaveTotalScreenTime(context, date, appUsageRepository, dailyScreenTimeRepository);
        }

        // Update last processed day to todayStart
        setLastProcessedDay(context, todayStart);

        return Result.success();
    }

    private long getCutoffTime(int intervalIndex) {
        switch (intervalIndex) {
            case INTERVAL_ONE_WEEK:
                return getStartOfDayDaysAgo(7);
            case INTERVAL_ONE_MONTH:
                return getStartOfDayMonthsAgo(1);
            case INTERVAL_THREE_MONTHS:
                return getStartOfDayMonthsAgo(3);
            case INTERVAL_SIX_MONTHS:
                return getStartOfDayMonthsAgo(6);
            case INTERVAL_ONE_YEAR:
                return getStartOfDayYearsAgo(1);
            case INTERVAL_NEVER:
                return -1;
            default:
                return -1;
        }
    }

    private long getStartOfDayDaysAgo(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DATE, -daysAgo);
        return calendar.getTimeInMillis();
    }

    private long getStartOfDayMonthsAgo(int monthsAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MONTH, -monthsAgo);
        return calendar.getTimeInMillis();
    }

    private long getStartOfDayYearsAgo(int yearsAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.YEAR, -yearsAgo);
        return calendar.getTimeInMillis();
    }

    private void computeAndSaveTotalScreenTime(Context context, long date,
                                               DailyAppUsageRepository appUsageRepository,
                                               DailyScreenTimeRepository dailyScreenTimeRepository) {
        long dayStart = date;
        long dayEnd = dayStart + TimeUnit.DAYS.toMillis(1);

        long totalScreenTime = computeTotalScreenTime(context, dayStart, dayEnd);

        // Fallback if no screen events are found
        if (totalScreenTime == 0) {
            totalScreenTime = computeTotalScreenTimefromAppUsage(context, dayStart, dayEnd, appUsageRepository);
        }

        // Save total screen time
        DailyScreenTimeEntity dailyScreenTimeEntity = new DailyScreenTimeEntity();
        dailyScreenTimeEntity.date = dayStart;
        dailyScreenTimeEntity.totalScreenTime = totalScreenTime;

        dailyScreenTimeRepository.insert(dailyScreenTimeEntity);
    }

    /**
     * Computes total screen time based on app usage data.
     *
     * @param context             The application context.
     * @param dayStart            The start timestamp of the day (midnight).
     * @param dayEnd              The end timestamp of the day.
     * @param appUsageRepository  The repository to access app usage data.
     * @return The total screen time in milliseconds.
     */
    private long computeTotalScreenTimefromAppUsage(Context context, long dayStart, long dayEnd,
                                                    DailyAppUsageRepository appUsageRepository) {
        // Retrieve the total foreground time of all apps within the day
        long totalForegroundTime = appUsageRepository.getTotalUsageTimeBetween(dayStart, dayEnd);

        // Cap total screen time to the day's duration
        long dayDuration = dayEnd - dayStart;
        long estimatedScreenTime = Math.min(totalForegroundTime, dayDuration);

        return estimatedScreenTime;
    }

    /**
     * Computes the total screen time based on screen on/off events.
     *
     * @param context  The application context.
     * @param dayStart The start timestamp of the day (midnight).
     * @param dayEnd   The end timestamp of the day.
     * @return The total screen time in milliseconds.
     */
    private long computeTotalScreenTime(Context context, long dayStart, long dayEnd) {
        ScreenEventRepository screenEventRepository = new ScreenEventRepository(context);
        List<ScreenEventEntity> events = screenEventRepository.getScreenEventsBetweenSync(dayStart, dayEnd);

        // Sort events by timestamp
        events.sort(Comparator.comparingLong(event -> event.timestamp));

        long totalScreenTime = 0;
        Long lastScreenOnTime = null;
        boolean screenOn = false;

        for (ScreenEventEntity event : events) {
            if (event.isScreenOn) {
                if (!screenOn) { // Prevent duplicate screen on events
                    screenOn = true;
                    lastScreenOnTime = event.timestamp;
                }
            } else {
                if (screenOn && lastScreenOnTime != null) {
                    totalScreenTime += event.timestamp - lastScreenOnTime;
                    screenOn = false;
                    lastScreenOnTime = null;
                }
            }
        }

        // Handle if screen is still on at the end of the day
        if (screenOn && lastScreenOnTime != null) {
            totalScreenTime += dayEnd - lastScreenOnTime;
        }
        return totalScreenTime;
    }

    private long getLastProcessedDay(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_PROCESSED_DAY, -1);
    }

    private void setLastProcessedDay(Context context, long dayStart) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(KEY_LAST_PROCESSED_DAY, dayStart);
        editor.apply();
    }

    private long getStartOfDayOneWeekAgo() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DATE, -7);
        return calendar.getTimeInMillis();
    }

    private long getStartOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        // Reset time to midnight
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
