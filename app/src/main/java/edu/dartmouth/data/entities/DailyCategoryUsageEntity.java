// File: edu/dartmouth/data/entities/DailyCategoryUsageEntity.java
package edu.dartmouth.data.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "daily_category_usage",
        indices = {
                @Index(value = {"categoryName", "date"}, unique = true)
        }
)
public class DailyCategoryUsageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String categoryName;
    public long date; // Start of the day timestamp
    public long totalTimeInForeground; // In milliseconds
}
