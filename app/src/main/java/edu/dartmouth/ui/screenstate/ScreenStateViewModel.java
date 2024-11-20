package edu.dartmouth.ui.screenstate;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.List;

import edu.dartmouth.data.entities.ScreenEventEntity;
import edu.dartmouth.repositories.ScreenEventRepository;

public class ScreenStateViewModel extends AndroidViewModel {

    private final ScreenEventRepository repository;
    private final MutableLiveData<TimeRange> timeRange = new MutableLiveData<>();
    private final LiveData<List<ScreenEventEntity>> screenEventsList;

    public ScreenStateViewModel(@NonNull Application application) {
        super(application);
        repository = new ScreenEventRepository(application.getApplicationContext());
        screenEventsList = Transformations.switchMap(timeRange, range ->
                repository.getScreenEventsBetween(range.startTime, range.endTime));
    }

    /**
     * Sets the time range for fetching screen events.
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
     * Returns the LiveData list of screen events based on the selected time range.
     *
     * @return LiveData list of ScreenEventEntity.
     */
    public LiveData<List<ScreenEventEntity>> getScreenEventsLiveData() {
        return screenEventsList;
    }

    /**
     * Inserts a new ScreenEventEntity into the database.
     *
     * @param event The ScreenEventEntity to insert.
     */
    public void insert(ScreenEventEntity event) {
        repository.insert(event);
    }

    /**
     * Deletes a specific ScreenEventEntity from the database.
     *
     * @param event The ScreenEventEntity to delete.
     */
    public void deleteEvent(ScreenEventEntity event) {
        repository.delete(event);
    }

    /**
     * Deletes all screen events from the database.
     */
    public void deleteAllScreenEvents() {
        repository.deleteAllData();
    }

    /**
     * Deletes screen events older than the specified cutoff time.
     *
     * @param cutoffTime The cutoff time in milliseconds.
     */
    public void deleteOldScreenEvents(long cutoffTime) {
        repository.deleteOldData(cutoffTime);
    }

    /**
     * Deletes screen events within the specified time range.
     *
     * @param startTime Start time in milliseconds.
     * @param endTime   End time in milliseconds.
     */
    public void deleteScreenEventsBetween(long startTime, long endTime) {
        repository.deleteDataBetween(startTime, endTime);
    }

    /**
     * Retrieves all screen events synchronously.
     *
     * @return List of all ScreenEventEntity.
     */
    public List<ScreenEventEntity> getAllScreenEventsSync() {
        return repository.getAllScreenEventsSync();
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
