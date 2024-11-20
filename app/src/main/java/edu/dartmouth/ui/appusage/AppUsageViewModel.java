package edu.dartmouth.ui.appusage;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.List;

import edu.dartmouth.data.entities.AppUsageSummary;
import edu.dartmouth.data.entities.CategoryUsageSummary;
import edu.dartmouth.data.entities.DailyAppUsageEntity;
import edu.dartmouth.data.entities.DailyCategoryUsageEntity;
import edu.dartmouth.repositories.DailyAppUsageRepository;
import edu.dartmouth.repositories.DailyCategoryUsageRepository;
import edu.dartmouth.repositories.DailyScreenTimeRepository;

public class AppUsageViewModel extends AndroidViewModel {

    private final MutableLiveData<TimeRange> timeRange = new MutableLiveData<>();

    private final LiveData<List<DailyAppUsageEntity>> dailyAppUsageList;
    private final LiveData<List<DailyCategoryUsageEntity>> dailyCategoryUsageList;
    private final LiveData<Long> dailyScreenTime;

    private final LiveData<List<AppUsageSummary>> aggregatedAppUsageList;
    private final LiveData<List<CategoryUsageSummary>> aggregatedCategoryUsageList;

    private final DailyAppUsageRepository appUsageRepository;
    private final DailyCategoryUsageRepository categoryUsageRepository;
    private final DailyScreenTimeRepository screenTimeRepository;

    public AppUsageViewModel(@NonNull Application application) {
        super(application);
        appUsageRepository = new DailyAppUsageRepository(application.getApplicationContext());
        categoryUsageRepository = new DailyCategoryUsageRepository(application.getApplicationContext());
        screenTimeRepository = new DailyScreenTimeRepository(application.getApplicationContext());

        dailyAppUsageList = Transformations.switchMap(timeRange, range ->
                appUsageRepository.getUsageBetweenLiveData(range.startTime, range.endTime));

        dailyCategoryUsageList = Transformations.switchMap(timeRange, range ->
                categoryUsageRepository.getUsageBetweenLiveData(range.startTime, range.endTime));

        dailyScreenTime = Transformations.switchMap(timeRange, range ->
                screenTimeRepository.getTotalScreenTimeBetweenLiveData(range.startTime, range.endTime));

        // **Aggregated Data**
        aggregatedAppUsageList = Transformations.switchMap(timeRange, range ->
                appUsageRepository.getAggregatedUsageBetweenLiveData(range.startTime, range.endTime));

        aggregatedCategoryUsageList = Transformations.switchMap(timeRange, range ->
                categoryUsageRepository.getAggregatedUsageBetweenLiveData(range.startTime, range.endTime));
    }

    public void setTimeRange(long startTime, long endTime) {
        timeRange.setValue(new TimeRange(startTime, endTime));
    }

    public LiveData<List<DailyAppUsageEntity>> getDailyAppUsageList() {
        return dailyAppUsageList;
    }

    public LiveData<List<DailyCategoryUsageEntity>> getDailyCategoryUsageList() {
        return dailyCategoryUsageList;
    }

    public LiveData<Long> getDailyScreenTime() {
        return dailyScreenTime;
    }

    public LiveData<List<AppUsageSummary>> getAggregatedAppUsageList() {
        return aggregatedAppUsageList;
    }

    public LiveData<List<CategoryUsageSummary>> getAggregatedCategoryUsageList() {
        return aggregatedCategoryUsageList;
    }

    private static class TimeRange {
        long startTime;
        long endTime;

        TimeRange(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}
