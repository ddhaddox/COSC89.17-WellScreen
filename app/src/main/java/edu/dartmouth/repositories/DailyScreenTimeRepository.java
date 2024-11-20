// File: edu/dartmouth/repositories/DailyScreenTimeRepository.java
package edu.dartmouth.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.dartmouth.data.AppDatabase;
import edu.dartmouth.data.dao.DailyScreenTimeDao;
import edu.dartmouth.data.entities.DailyScreenTimeEntity;

public class DailyScreenTimeRepository {
    private final DailyScreenTimeDao dailyScreenTimeDao;
    private final ExecutorService executorService;

    public DailyScreenTimeRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        dailyScreenTimeDao = db.dailyScreenTimeDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<Long> getTotalScreenTimeBetweenLiveData(long startTime, long endTime) {
        return dailyScreenTimeDao.getTotalScreenTimeBetweenLiveData(startTime, endTime);
    }

    public LiveData<List<DailyScreenTimeEntity>> getScreenTimeBetween(long startTime, long endTime) {
        return dailyScreenTimeDao.getScreenTimeBetween(startTime, endTime);
    }

    public void insert(DailyScreenTimeEntity entity) {
        executorService.execute(() -> dailyScreenTimeDao.insert(entity));
    }

    public void deleteOldData(long cutoffTime) {
        executorService.execute(() -> dailyScreenTimeDao.deleteOldData(cutoffTime));
    }

    public void deleteAllData() {
        executorService.execute(() -> dailyScreenTimeDao.deleteAllData());
    }

    public void deleteDataBetween(long startTime, long endTime) {
        executorService.execute(() -> dailyScreenTimeDao.deleteDataBetween(startTime, endTime));
    }

    public List<DailyScreenTimeEntity> getAllScreenTimeSync() {
        return dailyScreenTimeDao.getAllScreenTimeSync();
    }
}
