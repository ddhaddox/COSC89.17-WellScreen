// File: edu/dartmouth/repositories/DailyAppUsageRepository.java
package edu.dartmouth.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.dartmouth.data.AppDatabase;
import edu.dartmouth.data.dao.DailyAppUsageDao;
import edu.dartmouth.data.entities.AppUsageSummary;
import edu.dartmouth.data.entities.DailyAppUsageEntity;

public class DailyAppUsageRepository {
    private final DailyAppUsageDao dailyAppUsageDao;
    private final ExecutorService executorService;

    public DailyAppUsageRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        dailyAppUsageDao = db.dailyAppUsageDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(DailyAppUsageEntity entity) {
        executorService.execute(() -> dailyAppUsageDao.insert(entity));
    }

    public void insertAll(List<DailyAppUsageEntity> entities) {
        executorService.execute(() -> dailyAppUsageDao.insertAll(entities));
    }

    public LiveData<List<DailyAppUsageEntity>> getUsageBetweenLiveData(long startTime, long endTime) {
        return dailyAppUsageDao.getUsageBetweenLiveData(startTime, endTime);
    }

    public long getTotalUsageTimeBetween(long startTime, long endTime) {
        return dailyAppUsageDao.getTotalUsageTimeBetween(startTime, endTime);
    }

    public void deleteOldData(long cutoffTime) {
        executorService.execute(() -> dailyAppUsageDao.deleteOldUsage(cutoffTime));
    }

    public void deleteAllData() {
        executorService.execute(() -> dailyAppUsageDao.deleteAllData());
    }

    public void deleteDataBetween(long startTime, long endTime) {
        executorService.execute(() -> dailyAppUsageDao.deleteDataBetween(startTime, endTime));
    }

    public LiveData<List<AppUsageSummary>> getAggregatedUsageBetweenLiveData(long startTime, long endTime) {
        return dailyAppUsageDao.getAggregatedUsageBetweenLiveData(startTime, endTime);
    }

    public List<DailyAppUsageEntity> getAllAppUsageSync() {
        return dailyAppUsageDao.getAllUsageSync();
    }
}
