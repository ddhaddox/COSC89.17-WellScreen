package edu.dartmouth.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import edu.dartmouth.data.entities.DailyScreenTimeEntity;
import edu.dartmouth.data.entities.MPHQ9Entity;
import edu.dartmouth.repositories.DailyScreenTimeRepository;
import edu.dartmouth.repositories.MPHQ9Repository;

public class HomeViewModel extends AndroidViewModel {

    private final DailyScreenTimeRepository screenTimeRepository;
    private final MPHQ9Repository mphq9Repository;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        screenTimeRepository = new DailyScreenTimeRepository(application);
        mphq9Repository = new MPHQ9Repository(application);
    }

    /**
     * Fetches Daily Screen Time between the specified timestamps.
     */
    public LiveData<List<DailyScreenTimeEntity>> getScreenTimeBetween(long startTime, long endTime) {
        return screenTimeRepository.getScreenTimeBetween(startTime, endTime);
    }

    /**
     * Fetches MPHQ-9 Assessments between the specified timestamps.
     */
    public LiveData<List<MPHQ9Entity>> getMPHQ9Between(long startTime, long endTime) {
        return mphq9Repository.getAssessmentsBetween(startTime, endTime);
    }
}
