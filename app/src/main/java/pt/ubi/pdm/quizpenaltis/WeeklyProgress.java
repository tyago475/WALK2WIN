package pt.ubi.pdm.quizpenaltis;

import java.util.List;

/** Resumo dos últimos dias: lista diária + total de passos. */
public class WeeklyProgress {
    private List<UserProgress> dailyProgress;
    private int totalSteps;

    public WeeklyProgress(List<UserProgress> dailyProgress, int totalSteps) {
        this.dailyProgress = dailyProgress;
        this.totalSteps = totalSteps;
    }

    // Getters
    public List<UserProgress> getDailyProgress() { return dailyProgress; }
    public int getTotalSteps() { return totalSteps; }
}
