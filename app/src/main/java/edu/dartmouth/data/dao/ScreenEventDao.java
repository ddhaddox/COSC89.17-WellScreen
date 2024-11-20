package edu.dartmouth.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import androidx.room.Transaction;

import java.util.List;

import edu.dartmouth.data.entities.ScreenEventCount;
import edu.dartmouth.data.entities.ScreenEventEntity;

@Dao
public interface ScreenEventDao {
    @Insert
    void insertEvent(ScreenEventEntity event);

    @Insert
    void insertAll(List<ScreenEventEntity> events);

    @Transaction
    default void insertAllWithTransaction(List<ScreenEventEntity> events) {
        insertAll(events);
    }

    @Query("SELECT * FROM screen_events ORDER BY timestamp DESC")
    LiveData<List<ScreenEventEntity>> getAllEvents();

    @Delete
    void deleteEvent(ScreenEventEntity event);

    @Query("SELECT * FROM screen_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    LiveData<List<ScreenEventEntity>> getScreenEventsInRange(long startTime, long endTime);

    @Query("DELETE FROM screen_events WHERE timestamp < :cutoffTime")
    void deleteOldEvents(long cutoffTime);

    @Query("DELETE FROM screen_events")
    void deleteAll();

    @Query("SELECT * FROM screen_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    List<ScreenEventEntity> getScreenEventsBetweenSync(long startTime, long endTime);

    @Query("SELECT * FROM screen_events WHERE timestamp BETWEEN :startTime AND :endTime")
    LiveData<List<ScreenEventEntity>> getScreenEventsBetween(long startTime, long endTime);

    @Query("SELECT ((timestamp / 86400000) * 86400000) as date, COUNT(*) as count FROM screen_events WHERE timestamp BETWEEN :startTime AND :endTime GROUP BY date")
    LiveData<List<ScreenEventCount>> getScreenEventCountsBetween(long startTime, long endTime);

    @Query("DELETE FROM screen_events WHERE timestamp BETWEEN :startTime AND :endTime")
    void deleteDataBetween(long startTime, long endTime);

    @Query("SELECT * FROM screen_events")
    List<ScreenEventEntity> getAllScreenEventsSync();
}