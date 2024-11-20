// File: edu/dartmouth/data/dao/DailyAppUsageDao.java
package edu.dartmouth.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import edu.dartmouth.data.entities.AppUsageSummary;
import edu.dartmouth.data.entities.DailyAppUsageEntity;

@Dao
public interface DailyAppUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DailyAppUsageEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DailyAppUsageEntity> entities);

    @Query("SELECT * FROM daily_app_usage WHERE date BETWEEN :startTime AND :endTime")
    LiveData<List<DailyAppUsageEntity>> getUsageBetweenLiveData(long startTime, long endTime);

    @Query("SELECT SUM(totalTimeInForeground) FROM daily_app_usage WHERE date BETWEEN :startTime AND :endTime")
    long getTotalUsageTimeBetween(long startTime, long endTime);

    @Query("DELETE FROM daily_app_usage WHERE date < :cutoffTime")
    void deleteOldUsage(long cutoffTime);

    @Query("DELETE FROM daily_app_usage")
    void deleteAllData();

    @Query("DELETE FROM daily_app_usage WHERE date BETWEEN :startTime AND :endTime")
    void deleteDataBetween(long startTime, long endTime);

    @Query("SELECT packageName, categoryName, SUM(totalTimeInForeground) as totalTimeInForeground " +
            "FROM daily_app_usage WHERE date BETWEEN :startTime AND :endTime " +
            "GROUP BY packageName, categoryName ORDER BY totalTimeInForeground DESC")
    LiveData<List<AppUsageSummary>> getAggregatedUsageBetweenLiveData(long startTime, long endTime);

    @Query("SELECT * FROM daily_app_usage")
    List<DailyAppUsageEntity> getAllUsageSync();
}
