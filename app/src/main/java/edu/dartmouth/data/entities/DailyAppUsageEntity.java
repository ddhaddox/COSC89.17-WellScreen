// File: edu/dartmouth/data/entities/DailyAppUsageEntity.java
package edu.dartmouth.data.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "daily_app_usage",
        indices = {
                @Index(value = {"packageName", "date"}, unique = true)
        }
)
public class DailyAppUsageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String packageName;
    public long date; // Start of the day timestamp (e.g., midnight)
    public long totalTimeInForeground; // In milliseconds
    public String categoryName;
}
