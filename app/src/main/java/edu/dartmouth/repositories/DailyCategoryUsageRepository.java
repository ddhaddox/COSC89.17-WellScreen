// File: edu/dartmouth/repositories/DailyCategoryUsageRepository.java
package edu.dartmouth.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.dartmouth.data.AppDatabase;
import edu.dartmouth.data.dao.DailyCategoryUsageDao;
import edu.dartmouth.data.entities.CategoryUsageSummary;
import edu.dartmouth.data.entities.DailyCategoryUsageEntity;

public class DailyCategoryUsageRepository {
    private final DailyCategoryUsageDao dailyCategoryUsageDao;
    private final ExecutorService executorService;

    public DailyCategoryUsageRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        dailyCategoryUsageDao = db.dailyCategoryUsageDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(DailyCategoryUsageEntity entity) {
        executorService.execute(() -> dailyCategoryUsageDao.insert(entity));
    }

    public void insertAll(List<DailyCategoryUsageEntity> entities) {
        executorService.execute(() -> dailyCategoryUsageDao.insertAll(entities));
    }

    public LiveData<List<DailyCategoryUsageEntity>> getUsageBetweenLiveData(long startTime, long endTime) {
        return dailyCategoryUsageDao.getUsageBetweenLiveData(startTime, endTime);
    }

    public void deleteOldData(long cutoffTime) {
        executorService.execute(() -> dailyCategoryUsageDao.deleteOldData(cutoffTime));
    }

    public void deleteAllData() {
        executorService.execute(() -> dailyCategoryUsageDao.deleteAllData());
    }

    public void deleteDataBetween(long startTime, long endTime) {
        executorService.execute(() -> dailyCategoryUsageDao.deleteDataBetween(startTime, endTime));
    }

    public LiveData<List<CategoryUsageSummary>> getAggregatedUsageBetweenLiveData(long startTime, long endTime) {
        return dailyCategoryUsageDao.getAggregatedUsageBetweenLiveData(startTime, endTime);
    }

    public List<DailyCategoryUsageEntity> getAllCategoryUsageSync() {
        return dailyCategoryUsageDao.getAllCategoryUsageSync();
    }
}
