package edu.dartmouth.data.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "mphq9_assessments",
        indices = {
                @Index(value = {"timestamp"}, unique = false)
        }
)
public class MPHQ9Entity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    // Timestamp when the assessment was submitted
    public long timestamp;

    // Responses to MPHQ-9 questions (0-100)
    public int q1;
    public int q2;
    public int q3;
    public int q4;
    public int q5;
    public int q6;
    public int q7;
    public int q8;
    public int q9;

    // Average score (0-100)
    public float averageScore;
}