package edu.dartmouth.data.entities;

import androidx.room.ColumnInfo;

public class CategoryUsageSummary {
    @ColumnInfo(name = "categoryName")
    public String categoryName;

    @ColumnInfo(name = "totalTimeInForeground")
    public long totalTimeInForeground;
}
