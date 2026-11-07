// UserProgressDao.java
package pt.ubi.pdm.quizpenaltis.data.db;

import pt.ubi.pdm.quizpenaltis.model.UserProgress;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface UserProgressDao {

    @Insert
    void insertProgress(UserProgress progress);

    @Query("SELECT * FROM user_progress WHERE date = :date")
    UserProgress getProgressByDate(java.util.Date date);

    @Query("SELECT * FROM user_progress ORDER BY date DESC LIMIT 30")
    List<UserProgress> getLast30DaysProgress();

    @Query("SELECT * FROM user_progress WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    List<UserProgress> getProgressBetweenDates(java.util.Date startDate, java.util.Date endDate);

    @Query("UPDATE user_progress SET steps = steps + :newSteps WHERE date = :date")
    void updateSteps(java.util.Date date, int newSteps);

    @Query("SELECT SUM(steps) FROM user_progress WHERE date >= :startDate AND date <= :endDate")
    Integer getTotalStepsBetweenDates(java.util.Date startDate, java.util.Date endDate);

    @Query("DELETE FROM user_progress")
    void deleteAllProgress();
}