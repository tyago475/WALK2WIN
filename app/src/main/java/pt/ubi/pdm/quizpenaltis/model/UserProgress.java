// UserProgress.java
package pt.ubi.pdm.quizpenaltis.model;

import pt.ubi.pdm.quizpenaltis.data.db.DateConverter;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

@Entity(tableName = "user_progress")
@TypeConverters(DateConverter.class)
public class UserProgress {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private java.util.Date date;
    private int steps;

    public UserProgress(java.util.Date date, int steps) {
        this.date = date;
        this.steps = steps;
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public java.util.Date getDate() { return date; }
    public void setDate(java.util.Date date) { this.date = date; }

    public int getSteps() { return steps; }
    public void setSteps(int steps) { this.steps = steps; }
}