// File: edu/dartmouth/data/entities/DailyScreenTimeEntity.java
package edu.dartmouth.data.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "daily_screen_time",
        indices = {
                @Index(value = {"date"}, unique = true)
        }
)
public class DailyScreenTimeEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public long date; // Start of the day timestamp
    public long totalScreenTime; // In milliseconds
}
