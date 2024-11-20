// ScreenEventEntity.java
package edu.dartmouth.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "screen_events")
public class ScreenEventEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public boolean isScreenOn;
    public long timestamp;
}
