package pt.ubi.pdm.quizpenaltis;

import android.content.Context;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProgressManager {
    private static final String TAG = "ProgressManager";

    /** Passos da semana de demonstração (do dia mais antigo para hoje). */
    public static final int[] TEST_WEEK_STEPS = {4231, 5876, 3210, 7890, 6543, 2345, 8765};

    private AppDatabase database;
    private ExecutorService executor;
    private SimpleDateFormat dateFormat;

    public ProgressManager(Context context) {
        database = AppDatabase.getDatabase(context);
        executor = Executors.newSingleThreadExecutor();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    public void addSteps(int steps) {
        executor.execute(() -> {
            try {
                Date today = getTodayDate();
                UserProgress progress = database.userProgressDao().getProgressByDate(today);

                if (progress == null) {
                    // Criar novo registo para hoje
                    progress = new UserProgress(today, steps);
                    database.userProgressDao().insertProgress(progress);
                    Log.d(TAG, "✅ Novo registo criado com " + steps + " passos");
                } else {
                    // Atualizar passos existentes
                    database.userProgressDao().updateSteps(today, steps);
                    Log.d(TAG, "➕ " + steps + " passos adicionados ao registo existente");
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ Erro ao adicionar passos: " + e.getMessage(), e);
            }
        });
    }

    public void getWeeklyProgress(ProgressCallback callback) {
        executor.execute(() -> {
            try {
                Date endDate = new Date();
                Date startDate = daysAgoAtMidnight(6); // hoje + 6 dias anteriores = 7 dias

                Log.d(TAG, "📅 Buscando dados entre " + dateFormat.format(startDate) + " e " + dateFormat.format(endDate));

                // Query já ordenada por date ASC
                List<UserProgress> progressList = database.userProgressDao().getProgressBetweenDates(startDate, endDate);

                // Calcular total de passos
                Integer totalSteps = database.userProgressDao().getTotalStepsBetweenDates(startDate, endDate);

                WeeklyProgress weeklyProgress = new WeeklyProgress(
                        progressList,
                        totalSteps != null ? totalSteps : 0
                );

                Log.d(TAG, "📊 Dados encontrados: " + progressList.size() + " dias, " + weeklyProgress.getTotalSteps() + " passos totais");

                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        callback.onProgressLoaded(weeklyProgress);
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Erro ao carregar progresso semanal: " + e.getMessage(), e);
            }
        });
    }

    // MÉTODO PARA CRIAR DADOS DE TESTE (SÓ QUANDO CLICAS NO BOTÃO - TEMPORÁRIOS)
    public void createTestWeekData() {
        executor.execute(() -> {
            try {
                Log.d(TAG, "📅 Criando dados de teste TEMPORÁRIOS para demonstração...");

                // Apagar e inserir na MESMA tarefa evita que o apagar corra depois da 1ª inserção
                database.userProgressDao().deleteAllProgress();

                int[] stepsData = TEST_WEEK_STEPS;

                for (int i = 6; i >= 0; i--) {
                    Date date = daysAgoAtMidnight(i);

                    UserProgress progress = new UserProgress(date, stepsData[6-i]);
                    database.userProgressDao().insertProgress(progress);
                    Log.d(TAG, "📝 Dia teste " + (6-i) + ": " + stepsData[6-i] + " passos");
                }

                Log.d(TAG, "✅ Dados de teste TEMPORÁRIOS criados (apenas para demonstração)");

            } catch (Exception e) {
                Log.e(TAG, "❌ Erro ao criar dados de teste: " + e.getMessage(), e);
            }
        });
    }

    private Date getTodayDate() {
        return daysAgoAtMidnight(0);
    }

    /** Meia-noite de há {@code days} dias. Os registos diários usam sempre meia-noite. */
    private Date daysAgoAtMidnight(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public interface ProgressCallback {
        void onProgressLoaded(WeeklyProgress weeklyProgress);
    }

    public void close() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
