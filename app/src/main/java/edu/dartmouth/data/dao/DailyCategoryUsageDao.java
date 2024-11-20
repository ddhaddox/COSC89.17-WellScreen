// File: edu/dartmouth/data/dao/DailyCategoryUsageDao.java
package edu.dartmouth.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import edu.dartmouth.data.entities.CategoryUsageSummary;
import edu.dartmouth.data.entities.DailyCategoryUsageEntity;

@Dao
public interface DailyCategoryUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DailyCategoryUsageEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<DailyCategoryUsageEntity> entities);

    @Query("SELECT * FROM daily_category_usage WHERE date BETWEEN :startTime AND :endTime")
    LiveData<List<DailyCategoryUsageEntity>> getUsageBetweenLiveData(long startTime, long endTime);

    @Query("DELETE FROM daily_category_usage WHERE date < :cutoffTime")
    void deleteOldData(long cutoffTime);

    @Query("DELETE FROM daily_category_usage")
    void deleteAllData();

    @Query("DELETE FROM daily_category_usage WHERE date BETWEEN :startTime AND :endTime")
    void deleteDataBetween(long startTime, long endTime);

    @Query("SELECT categoryName, SUM(totalTimeInForeground) as totalTimeInForeground FROM daily_category_usage WHERE date BETWEEN :startTime AND :endTime GROUP BY categoryName ORDER BY totalTimeInForeground DESC")
    LiveData<List<CategoryUsageSummary>> getAggregatedUsageBetweenLiveData(long startTime, long endTime);

    @Query("SELECT * FROM daily_category_usage")
    List<DailyCategoryUsageEntity> getAllCategoryUsageSync();
}
