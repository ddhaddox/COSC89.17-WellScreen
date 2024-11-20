package edu.dartmouth.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import edu.dartmouth.data.entities.MPHQ9Entity;

@Dao
public interface MPHQ9Dao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAssessment(MPHQ9Entity assessment);

    @Query("SELECT * FROM mphq9_assessments ORDER BY timestamp DESC")
    LiveData<List<MPHQ9Entity>> getAllAssessments();

    @Query("DELETE FROM mphq9_assessments")
    void deleteAllAssessments();

    @Query("DELETE FROM mphq9_assessments WHERE timestamp < :cutoffTime")
    void deleteOldAssessments(long cutoffTime);

    @Query("SELECT * FROM mphq9_assessments WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    LiveData<List<MPHQ9Entity>> getAssessmentsBetween(long startTime, long endTime);

    @Query("DELETE FROM mphq9_assessments WHERE timestamp BETWEEN :startTime AND :endTime")
    void deleteAssessmentsBetween(long startTime, long endTime);

    @Query("SELECT * FROM mphq9_assessments")
    List<MPHQ9Entity> getAllAssessmentsSync();
}
