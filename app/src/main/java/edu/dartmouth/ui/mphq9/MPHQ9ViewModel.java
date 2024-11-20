package edu.dartmouth.ui.mphq9;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.List;

import edu.dartmouth.data.entities.MPHQ9Entity;
import edu.dartmouth.repositories.MPHQ9Repository;

public class MPHQ9ViewModel extends AndroidViewModel {

    private final MPHQ9Repository repository;
    private final MutableLiveData<TimeRange> timeRange = new MutableLiveData<>();
    private final LiveData<List<MPHQ9Entity>> assessmentsList;

    public MPHQ9ViewModel(@NonNull Application application) {
        super(application);
        repository = new MPHQ9Repository(application.getApplicationContext());
        assessmentsList = Transformations.switchMap(timeRange, range ->
                repository.getAssessmentsBetween(range.startTime, range.endTime));
    }

    /**
     * Sets the time range for fetching assessments.
     *
     * @param startTime Start time in milliseconds.
     * @param endTime   End time in milliseconds.
     */
    public void setTimeRange(long startTime, long endTime) {
        TimeRange newRange = new TimeRange(startTime, endTime);
        if (!newRange.equals(timeRange.getValue())) {
            timeRange.setValue(newRange);
        }
    }

    /**
     * Returns the LiveData list of assessments based on the selected time range.
     *
     * @return LiveData list of MPHQ9Entity.
     */
    public LiveData<List<MPHQ9Entity>> getAssessmentsLiveData() {
        return assessmentsList;
    }

    /**
     * Inserts a new MPHQ9 assessment into the database.
     *
     * @param assessment The MPHQ9Entity to insert.
     */
    public void insert(MPHQ9Entity assessment) {
        repository.insert(assessment);
    }

    /**
     * Deletes all MPHQ9 assessments from the database.
     */
    public void deleteAllAssessments() {
        repository.deleteAll();
    }

    /**
     * Deletes MPHQ9 assessments older than the specified cutoff time.
     *
     * @param cutoffTime The cutoff time in milliseconds.
     */
    public void deleteOldAssessments(long cutoffTime) {
        repository.deleteOld(cutoffTime);
    }

    /**
     * Deletes assessments within the specified time range.
     *
     * @param startTime Start time in milliseconds.
     * @param endTime   End time in milliseconds.
     */
    public void deleteAssessmentsBetween(long startTime, long endTime) {
        repository.deleteAssessmentsBetween(startTime, endTime);
    }

    /**
     * Retrieves all assessments synchronously.
     *
     * @return List of all MPHQ9Entity.
     */
    public List<MPHQ9Entity> getAllAssessmentsSync() {
        return repository.getAllAssessmentsSync();
    }

    /**
     * Internal class to represent a time range.
     */
    private static class TimeRange {
        long startTime;
        long endTime;

        TimeRange(long startTime, long endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TimeRange)) return false;
            TimeRange other = (TimeRange) obj;
            return this.startTime == other.startTime && this.endTime == other.endTime;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(startTime) * 31 + Long.hashCode(endTime);
        }
    }
}
