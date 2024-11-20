package edu.dartmouth.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.dartmouth.data.AppDatabase;
import edu.dartmouth.data.dao.ScreenEventDao;
import edu.dartmouth.data.entities.ScreenEventEntity;
import edu.dartmouth.data.entities.ScreenEventCount;

public class ScreenEventRepository {

    private final ScreenEventDao screenEventDao;
    private final ExecutorService executorService;

    public ScreenEventRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.screenEventDao = db.screenEventDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(ScreenEventEntity data) {
        executorService.execute(() -> screenEventDao.insertEvent(data));
    }

    public void insertAll(List<ScreenEventEntity> events) {
        executorService.execute(() -> screenEventDao.insertAll(events));
    }

    public void delete(ScreenEventEntity data) {
        executorService.execute(() -> screenEventDao.deleteEvent(data));
    }

    public LiveData<List<ScreenEventEntity>> getAll() {
        return screenEventDao.getAllEvents();
    }

    public void deleteOldData(long cutoffTime) {
        executorService.execute(() -> screenEventDao.deleteOldEvents(cutoffTime));
    }

    public void deleteAllData() {
        executorService.execute(screenEventDao::deleteAll);
    }

    public List<ScreenEventEntity> getScreenEventsBetweenSync(long startTime, long endTime) {
        return screenEventDao.getScreenEventsBetweenSync(startTime, endTime);
    }

    public LiveData<List<ScreenEventEntity>> getScreenEventsBetween(long startTime, long endTime) {
        return screenEventDao.getScreenEventsBetween(startTime, endTime);
    }

    public LiveData<List<ScreenEventCount>> getScreenEventCountsBetween(long startTime, long endTime) {
        return screenEventDao.getScreenEventCountsBetween(startTime, endTime);
    }

    public void deleteDataBetween(long startTime, long endTime) {
        executorService.execute(() -> screenEventDao.deleteDataBetween(startTime, endTime));
    }

    public List<ScreenEventEntity> getAllScreenEventsSync() {
        return screenEventDao.getAllScreenEventsSync();
    }
}