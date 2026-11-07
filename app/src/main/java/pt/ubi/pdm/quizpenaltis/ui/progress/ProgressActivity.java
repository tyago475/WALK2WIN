package pt.ubi.pdm.quizpenaltis.ui.progress;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.data.progress.ProgressManager;
import pt.ubi.pdm.quizpenaltis.model.UserProgress;
import pt.ubi.pdm.quizpenaltis.model.WeeklyProgress;
import pt.ubi.pdm.quizpenaltis.util.Constants;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProgressActivity extends AppCompatActivity implements ProgressManager.ProgressCallback {

    private static final String TAG = "ProgressActivity";

    private CustomBarChart stepsChart;
    private TextView tvTotalSteps, tvAverageSteps, tvBestDaySteps, tvDailyGoal, tvPeriodo;
    private TextView tvEmptyState;
    private ProgressManager progressManager;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        Log.d(TAG, "📊 ProgressActivity iniciada");

        progressManager = new ProgressManager(this);
        initializeViews();

        // CARREGAR APENAS DADOS REAIS (não carrega dados de teste automáticos)
        loadProgressData();
    }

    private void initializeViews() {
        // Inicializar gráfico
        stepsChart = findViewById(R.id.stepsChart);

        // Inicializar TextViews das estatísticas de passos
        tvTotalSteps = findViewById(R.id.tvTotalSteps);
        tvAverageSteps = findViewById(R.id.tvAverageSteps);
        tvBestDaySteps = findViewById(R.id.tvBestDaySteps);
        tvDailyGoal = findViewById(R.id.tvDailyGoal);
        tvPeriodo = findViewById(R.id.tvPeriodo);

        // TextView para estado vazio
        tvEmptyState = findViewById(R.id.tvEmptyState);

        // Botão voltar
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        Log.d(TAG, "✅ Views inicializadas");
    }

    private void loadProgressData() {
        Log.d(TAG, "🔄 A carregar dados de progresso da base de dados...");
        progressManager.getWeeklyProgress(this);
    }

    @Override
    public void onProgressLoaded(WeeklyProgress weeklyProgress) {
        Log.d(TAG, "📊 Callback recebido: " + weeklyProgress.getDailyProgress().size() + " dias, " +
                weeklyProgress.getTotalSteps() + " passos totais");

        runOnUiThread(() -> {
            // VERIFICAR SE HÁ DADOS VÁLIDOS
            boolean hasValidData = weeklyProgress.getDailyProgress().size() > 0 && weeklyProgress.getTotalSteps() > 0;

            if (hasValidData) {
                // TEM DADOS - mostrar gráfico e estatísticas
                showProgressData(weeklyProgress);

                // 🔥 AVISO DE QUE SÃO DADOS TEMPORÁRIOS
                if (isTestData(weeklyProgress)) {
                    Toast.makeText(this, R.string.progress_demo_data, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.progress_real_data, Toast.LENGTH_SHORT).show();
                }

                Log.d(TAG, "✅ Mostrando dados do ProgressManager");
            } else {
                // NÃO TEM DADOS - mostrar estado vazio
                showEmptyState();
                Log.d(TAG, "📭 Sem dados - mostrando estado vazio");
            }
        });
    }

    // 🔥 MÉTODO PARA DETETAR SE SÃO DADOS DE TESTE
    private boolean isTestData(WeeklyProgress progress) {
        // Verifica se os dados correspondem ao padrão de teste
        if (progress.getDailyProgress().size() != 7) return false;

        int[] testPattern = ProgressManager.TEST_WEEK_STEPS;
        List<UserProgress> sorted = new ArrayList<>(progress.getDailyProgress());

        // CORREÇÃO: Usar Collections.sort em vez de List.sort para compatibilidade com API 21
        Collections.sort(sorted, new Comparator<UserProgress>() {
            @Override
            public int compare(UserProgress p1, UserProgress p2) {
                return p1.getDate().compareTo(p2.getDate());
            }
        });

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getSteps() != testPattern[i]) {
                return false;
            }
        }
        return true;
    }

    private void showEmptyState() {
        // ESCONDER gráfico e estatísticas
        if (stepsChart != null) stepsChart.setVisibility(android.view.View.GONE);
        if (tvTotalSteps != null) tvTotalSteps.setVisibility(android.view.View.GONE);
        if (tvAverageSteps != null) tvAverageSteps.setVisibility(android.view.View.GONE);
        if (tvBestDaySteps != null) tvBestDaySteps.setVisibility(android.view.View.GONE);
        if (tvDailyGoal != null) tvDailyGoal.setVisibility(android.view.View.GONE);
        if (tvPeriodo != null) tvPeriodo.setVisibility(android.view.View.GONE);

        // MOSTRAR mensagem de estado vazio
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(android.view.View.VISIBLE);
            tvEmptyState.setText(R.string.progress_empty_state);
        }
    }

    private void showProgressData(WeeklyProgress progress) {
        // ESCONDER mensagem de estado vazio
        if (tvEmptyState != null) tvEmptyState.setVisibility(android.view.View.GONE);

        // MOSTRAR gráfico e estatísticas
        if (stepsChart != null) stepsChart.setVisibility(android.view.View.VISIBLE);
        if (tvTotalSteps != null) tvTotalSteps.setVisibility(android.view.View.VISIBLE);
        if (tvAverageSteps != null) tvAverageSteps.setVisibility(android.view.View.VISIBLE);
        if (tvBestDaySteps != null) tvBestDaySteps.setVisibility(android.view.View.VISIBLE);
        if (tvDailyGoal != null) tvDailyGoal.setVisibility(android.view.View.VISIBLE);
        if (tvPeriodo != null) tvPeriodo.setVisibility(android.view.View.VISIBLE);

        // ATUALIZAR estatísticas
        updateStats(progress);

        // ATUALIZAR gráfico
        updateStepsChart(progress.getDailyProgress());

        Log.d(TAG, "📈 Dados de progresso mostrados com sucesso");
    }

    private void updateStats(WeeklyProgress progress) {
        int totalSteps = progress.getTotalSteps();
        int averageSteps = progress.getDailyProgress().size() > 0 ?
                totalSteps / progress.getDailyProgress().size() : 0;

        // Atualizar estatísticas
        tvTotalSteps.setText(getString(R.string.progress_number, totalSteps));
        tvAverageSteps.setText(getString(R.string.progress_number, averageSteps));

        // Encontrar melhor dia de passos
        String bestDaySteps = findBestStepsDay(progress.getDailyProgress());
        tvBestDaySteps.setText(bestDaySteps);

        // Meta diária fixa
        tvDailyGoal.setText(getString(R.string.progress_number, Constants.DAILY_STEP_GOAL));

        // Atualizar período
        int days = progress.getDailyProgress().size();
        tvPeriodo.setText(getResources().getQuantityString(R.plurals.progress_period, days, days));

        Log.d(TAG, "📊 Estatísticas: Total=" + totalSteps + ", Média=" + averageSteps + ", Melhor=" + bestDaySteps);
    }

    private void updateStepsChart(List<UserProgress> dailyProgress) {
        if (dailyProgress.isEmpty()) {
            Log.d(TAG, "ℹ️  Sem dados para o gráfico");
            return;
        }

        List<CustomBarChart.ChartData> chartData = new ArrayList<>();


        String[] portugueseDays = getResources().getStringArray(R.array.weekdays_short);


        List<UserProgress> sortedProgress = new ArrayList<>(dailyProgress);


        Collections.sort(sortedProgress, new Comparator<UserProgress>() {
            @Override
            public int compare(UserProgress p1, UserProgress p2) {
                return p1.getDate().compareTo(p2.getDate());
            }
        });

        for (int i = 0; i < sortedProgress.size(); i++) {
            UserProgress progress = sortedProgress.get(i);
            String dayLabel = portugueseDays[i % portugueseDays.length];
            chartData.add(new CustomBarChart.ChartData(dayLabel, progress.getSteps()));
            Log.d(TAG, "📈 " + dayLabel + ": " + progress.getSteps() + " passos");
        }

        if (stepsChart != null) {
            stepsChart.setData(chartData, getString(R.string.progress_chart_title), getString(R.string.progress_chart_y_label));
            Log.d(TAG, "✅ Gráfico atualizado com " + chartData.size() + " dias");
        }
    }

    private String findBestStepsDay(List<UserProgress> dailyProgress) {
        if (dailyProgress.isEmpty()) return "0";

        int bestSteps = 0;
        for (UserProgress progress : dailyProgress) {
            if (progress.getSteps() > bestSteps) {
                bestSteps = progress.getSteps();
            }
        }

        return getString(R.string.progress_number, bestSteps);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "🔄 onResume() - Recarregando dados");
        // Recarregar dados sempre que a activity retorna ao foreground
        loadProgressData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "🛑 onDestroy()");
    }
}