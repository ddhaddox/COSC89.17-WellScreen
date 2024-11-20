// File: edu/dartmouth/SyntheticDataGenerator.java
package edu.dartmouth.testing;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import edu.dartmouth.data.entities.DailyAppUsageEntity;
import edu.dartmouth.data.entities.DailyScreenTimeEntity;
import edu.dartmouth.data.entities.MPHQ9Entity;
import edu.dartmouth.data.entities.ScreenEventEntity;
import edu.dartmouth.repositories.DailyAppUsageRepository;
import edu.dartmouth.repositories.DailyScreenTimeRepository;
import edu.dartmouth.repositories.MPHQ9Repository;
import edu.dartmouth.repositories.ScreenEventRepository;

public class SyntheticDataGenerator {

    private static final String TAG = "SyntheticDataGenerator";
    private Context context;

    public SyntheticDataGenerator(Context context) {
        this.context = context;
    }

    public void generateSyntheticData() {
        clearAllData();
        generateSyntheticScreenEvents();
        generateSyntheticAppUsage();
        generateSyntheticScreenTime();
        generateSyntheticMPHQ9Assessments();
    }

    private void clearAllData() {
        ScreenEventRepository screenEventRepository = new ScreenEventRepository(context);
        DailyAppUsageRepository appUsageRepository = new DailyAppUsageRepository(context);
        DailyScreenTimeRepository screenTimeRepository = new DailyScreenTimeRepository(context);
        MPHQ9Repository mphq9Repository = new MPHQ9Repository(context);

        screenEventRepository.deleteAllData();
        appUsageRepository.deleteAllData();
        screenTimeRepository.deleteAllData();
        mphq9Repository.deleteAll();

        Log.d(TAG, "All data cleared.");
    }

    private void generateSyntheticScreenEvents() {
        ScreenEventRepository repository = new ScreenEventRepository(context);
        Random random = new Random();
        long now = System.currentTimeMillis();
        long oneDayMillis = TimeUnit.DAYS.toMillis(1);
        long startTime = now - TimeUnit.DAYS.toMillis(20); // Number of days to generate data for

        for (long day = startTime; day <= now; day += oneDayMillis) {
            long dayStart = getStartOfDay(day);
            long dayEnd = dayStart + oneDayMillis;

            long currentTime = dayStart;

            while (currentTime < dayEnd) {
                // Random interval between events (e.g., 5 to 60 minutes)
                long interval = TimeUnit.MINUTES.toMillis(5 + random.nextInt(55));
                currentTime += interval;

                if (currentTime >= dayEnd) {
                    break;
                }

                // Create screen on event
                ScreenEventEntity onEvent = new ScreenEventEntity();
                onEvent.timestamp = currentTime;
                onEvent.isScreenOn = true;
                repository.insert(onEvent);

                // Random screen on duration (1 to 15 minutes)
                long onDuration = TimeUnit.MINUTES.toMillis(1 + random.nextInt(14));
                currentTime += onDuration;

                if (currentTime >= dayEnd) {
                    // Screen off event at day end
                    ScreenEventEntity offEvent = new ScreenEventEntity();
                    offEvent.timestamp = dayEnd;
                    offEvent.isScreenOn = false;
                    repository.insert(offEvent);
                    break;
                }

                // Create screen off event
                ScreenEventEntity offEvent = new ScreenEventEntity();
                offEvent.timestamp = currentTime;
                offEvent.isScreenOn = false;
                repository.insert(offEvent);
            }
        }

        Log.d(TAG, "Synthetic screen events generated.");
    }

    private static class AppInfo {
        String packageName;
        String categoryName;

        AppInfo(String packageName, String categoryName) {
            this.packageName = packageName;
            this.categoryName = categoryName;
        }
    }

    private void generateSyntheticAppUsage() {
        DailyAppUsageRepository appUsageRepository = new DailyAppUsageRepository(context);
        Random random = new Random();
        long now = System.currentTimeMillis();
        long oneDayMillis = TimeUnit.DAYS.toMillis(1);
        long startTime = now - TimeUnit.DAYS.toMillis(7); // 7 days ago

        List<AppInfo> appList = new ArrayList<>();
        appList.add(new AppInfo("com.socialmedia.app", "Social"));
        appList.add(new AppInfo("com.messaging.app", "Communication"));
        appList.add(new AppInfo("com.news.app", "News"));
        appList.add(new AppInfo("com.games.app", "Games"));
        appList.add(new AppInfo("com.video.app", "Entertainment"));
        appList.add(new AppInfo("com.productivity.app", "Productivity"));

        for (long day = startTime; day <= now; day += oneDayMillis) {
            long dayStart = getStartOfDay(day);

            for (AppInfo app : appList) {
                // Random total usage between 0 and 2 hours
                long totalTimeInForeground = TimeUnit.MINUTES.toMillis(random.nextInt(120));

                if (totalTimeInForeground > 0) {
                    DailyAppUsageEntity appEntity = new DailyAppUsageEntity();
                    appEntity.packageName = app.packageName;
                    appEntity.categoryName = app.categoryName;
                    appEntity.date = dayStart;
                    appEntity.totalTimeInForeground = totalTimeInForeground;
                    appUsageRepository.insert(appEntity);
                }
            }
        }

        Log.d(TAG, "Synthetic app usage data generated.");
    }

    private void generateSyntheticScreenTime() {
        DailyScreenTimeRepository dailyScreenTimeRepository = new DailyScreenTimeRepository(context);
        Random random = new Random();
        long now = System.currentTimeMillis();
        long oneDayMillis = TimeUnit.DAYS.toMillis(1);
        long startTime = now - TimeUnit.DAYS.toMillis(7); // Number of days to gen data

        for (long day = startTime; day <= now; day += oneDayMillis) {
            long dayStart = getStartOfDay(day);

            // Random total screen time between 1 and 5 hours
            long totalScreenTime = TimeUnit.MINUTES.toMillis(60 + random.nextInt(240));

            DailyScreenTimeEntity entity = new DailyScreenTimeEntity();
            entity.date = dayStart;
            entity.totalScreenTime = totalScreenTime;

            dailyScreenTimeRepository.insert(entity);
        }

        Log.d(TAG, "Synthetic screen time data generated.");
    }

    private void generateSyntheticMPHQ9Assessments() {
        MPHQ9Repository mphq9Repository = new MPHQ9Repository(context);
        Random random = new Random();
        long now = System.currentTimeMillis();
        long oneDayMillis = TimeUnit.DAYS.toMillis(1);
        long startTime = now - TimeUnit.DAYS.toMillis(7); // Number of days to gen data

        for (long day = startTime; day <= now; day += oneDayMillis) {
            long assessmentTime = day + TimeUnit.HOURS.toMillis(9 + random.nextInt(12)); // Random time during the day

            MPHQ9Entity entity = new MPHQ9Entity();
            entity.timestamp = assessmentTime;

            // Random scores between 0 and 100
            entity.q1 = random.nextInt(101);
            entity.q2 = random.nextInt(101);
            entity.q3 = random.nextInt(101);
            entity.q4 = random.nextInt(101);
            entity.q5 = random.nextInt(101);
            entity.q6 = random.nextInt(101);
            entity.q7 = random.nextInt(101);
            entity.q8 = random.nextInt(101);
            entity.q9 = random.nextInt(101);

            // Calculate average score
            entity.averageScore = (entity.q1 + entity.q2 + entity.q3 + entity.q4 + entity.q5 + entity.q6 + entity.q7 + entity.q8 + entity.q9) / 9.0f;

            mphq9Repository.insert(entity);
        }

        Log.d(TAG, "Synthetic MPHQ9 assessments generated.");
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
