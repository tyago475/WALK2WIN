package pt.ubi.pdm.quizpenaltis;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.util.Log;

public class StepCounter {
    private static final String TAG = "StepCounter";
    private static final String STEPS_TODAY = "steps_today";
    private static final String LAST_RESET_DATE = "last_reset_date";
    private static final String INITIAL_STEP_COUNT = "initial_step_count";
    private static final String LAST_STEP_COUNT = "last_step_count";

    public static int getTodaySteps(Context context) {
        checkDailyReset(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int steps = prefs.getInt(STEPS_TODAY, 0);
        Log.d(TAG, "📊 getTodaySteps() retornando: " + steps);
        return steps;
    }

    public static void addSteps(Context context, int steps) {
        Log.d(TAG, "➕ addSteps() adicionando: " + steps + " passos");
        checkDailyReset(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int currentSteps = prefs.getInt(STEPS_TODAY, 0);
        int newTotal = currentSteps + steps;
        prefs.edit().putInt(STEPS_TODAY, newTotal).apply();
        Log.d(TAG, "💾 Steps guardados: " + currentSteps + " + " + steps + " = " + newTotal);
    }

    // MÉTODO CORRETO para usar com TYPE_STEP_COUNTER
    public static void updateFromStepCounter(Context context, int totalStepsSinceReboot) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        checkDailyReset(context);

        int initialSteps = prefs.getInt(INITIAL_STEP_COUNT, 0);
        int lastSteps = prefs.getInt(LAST_STEP_COUNT, 0);

        Log.d(TAG, "🔄 updateFromStepCounter() - Total desde reboot: " + totalStepsSinceReboot +
                ", Initial: " + initialSteps + ", Last: " + lastSteps);

        // Se é a primeira vez, guarda o valor inicial
        if (initialSteps == 0) {
            prefs.edit()
                    .putInt(INITIAL_STEP_COUNT, totalStepsSinceReboot)
                    .putInt(LAST_STEP_COUNT, totalStepsSinceReboot)
                    .apply();
            Log.d(TAG, "🎯 Primeira leitura - initialSteps set para: " + totalStepsSinceReboot);
            return;
        }

        // Calcula os novos passos desde a última leitura
        int newSteps = totalStepsSinceReboot - lastSteps;

        // Proteção contra valores negativos (reinício do sensor)
        if (newSteps < 0) {
            Log.w(TAG, "⚠️ Reinício do sensor detetado! Recalculando...");
            newSteps = totalStepsSinceReboot; // Começa de novo
            prefs.edit()
                    .putInt(INITIAL_STEP_COUNT, totalStepsSinceReboot)
                    .putInt(LAST_STEP_COUNT, totalStepsSinceReboot)
                    .apply();
        }

        if (newSteps > 0) {
            Log.d(TAG, "🎯 Novos passos detetados: " + newSteps);
            addSteps(context, newSteps);
        }

        // Atualiza o último valor lido
        prefs.edit().putInt(LAST_STEP_COUNT, totalStepsSinceReboot).apply();
    }

    private static void checkDailyReset(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String lastDate = prefs.getString(LAST_RESET_DATE, "");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Log.d(TAG, "📅 Check daily reset: última data = " + lastDate + ", hoje = " + today);

        if (!lastDate.equals(today)) {
            Log.d(TAG, "🔄 Novo dia detectado - resetando contador de passos");
            prefs.edit()
                    .putInt(STEPS_TODAY, 0)
                    .putString(LAST_RESET_DATE, today)
                    .putInt(INITIAL_STEP_COUNT, 0) // Reseta também o contador do sensor
                    .putInt(LAST_STEP_COUNT, 0)
                    .apply();
        }
    }

    public static int getTodayPoints(Context context) {
        int steps = getTodaySteps(context);
        int points = (steps / 1000) * 100;
        Log.d(TAG, "💰 getTodayPoints(): " + steps + " passos = " + points + " pontos");
        return points;
    }

    // MÉTODO PARA RESETAR MANUALMENTE (para testes)
    public static void resetSteps(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .putInt(STEPS_TODAY, 0)
                .putInt(INITIAL_STEP_COUNT, 0)
                .putInt(LAST_STEP_COUNT, 0)
                .apply();
        Log.d(TAG, "🧹 Steps resetados manualmente");
    }
}