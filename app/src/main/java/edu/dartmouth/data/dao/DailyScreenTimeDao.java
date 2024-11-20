// File: edu/dartmouth/data/dao/DailyScreenTimeDao.java
package edu.dartmouth.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import edu.dartmouth.data.entities.DailyScreenTimeEntity;

@Dao
public interface DailyScreenTimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DailyScreenTimeEntity entity);

    @Query("SELECT SUM(totalScreenTime) FROM daily_screen_time WHERE date BETWEEN :startTime AND :endTime")
    LiveData<Long> getTotalScreenTimeBetweenLiveData(long startTime, long endTime);

    @Query("DELETE FROM daily_screen_time WHERE date < :cutoffTime")
    void deleteOldData(long cutoffTime);

    @Query("DELETE FROM daily_screen_time")
    void deleteAllData();

    @Query("SELECT * FROM daily_screen_time WHERE date BETWEEN :startTime AND :endTime ORDER BY date ASC")
    LiveData<List<DailyScreenTimeEntity>> getScreenTimeBetween(long startTime, long endTime);

    @Query("DELETE FROM daily_screen_time WHERE date BETWEEN :startTime AND :endTime")
    void deleteDataBetween(long startTime, long endTime);

    @Query("SELECT * FROM daily_screen_time")
    List<DailyScreenTimeEntity> getAllScreenTimeSync();
}
