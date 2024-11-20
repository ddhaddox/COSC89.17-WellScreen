package edu.dartmouth.data.entities;

import androidx.room.ColumnInfo;

public class AppUsageSummary {
    @ColumnInfo(name = "packageName")
    public String packageName;

    @ColumnInfo(name = "categoryName")
    public String categoryName;

    @ColumnInfo(name = "totalTimeInForeground")
    public long totalTimeInForeground;
}
