package edu.dartmouth.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.dartmouth.data.AppDatabase;
import edu.dartmouth.data.dao.MPHQ9Dao;
import edu.dartmouth.data.entities.MPHQ9Entity;

public class MPHQ9Repository {
    private final MPHQ9Dao mphq9Dao;
    private final ExecutorService executorService;

    public MPHQ9Repository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.mphq9Dao = db.mphq9Dao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(MPHQ9Entity assessment) {
        executorService.execute(() -> mphq9Dao.insertAssessment(assessment));
    }

    public LiveData<List<MPHQ9Entity>> getAllAssessments() {
        return mphq9Dao.getAllAssessments();
    }

    public void deleteAll() {
        executorService.execute(mphq9Dao::deleteAllAssessments);
    }

    public void deleteOld(long cutoffTime) {
        executorService.execute(() -> mphq9Dao.deleteOldAssessments(cutoffTime));
    }

    public LiveData<List<MPHQ9Entity>> getAssessmentsBetween(long startTime, long endTime) {
        return mphq9Dao.getAssessmentsBetween(startTime, endTime);
    }

    public void deleteAssessmentsBetween(long startTime, long endTime) {
        executorService.execute(() -> mphq9Dao.deleteAssessmentsBetween(startTime, endTime));
    }

    public List<MPHQ9Entity> getAllAssessmentsSync() {
        return mphq9Dao.getAllAssessmentsSync();
    }
}
