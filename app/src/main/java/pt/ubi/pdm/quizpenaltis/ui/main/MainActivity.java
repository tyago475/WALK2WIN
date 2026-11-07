package pt.ubi.pdm.quizpenaltis.ui.main;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.model.PlayerCatalog;
import pt.ubi.pdm.quizpenaltis.data.progress.ProgressManager;
import pt.ubi.pdm.quizpenaltis.steps.StepCounter;
import pt.ubi.pdm.quizpenaltis.steps.StepTrackingService;
import pt.ubi.pdm.quizpenaltis.steps.StepsNotificationManager;
import pt.ubi.pdm.quizpenaltis.ui.help.HelpActivity;
import pt.ubi.pdm.quizpenaltis.ui.player.PlayerSelectionActivity;
import pt.ubi.pdm.quizpenaltis.ui.progress.ProgressActivity;
import pt.ubi.pdm.quizpenaltis.util.Constants;
import pt.ubi.pdm.quizpenaltis.util.SystemBars;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import pt.ubi.pdm.quizpenaltis.BuildConfig;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;


import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int MAX_STEPS = Constants.DAILY_STEP_GOAL;

    private TextView tvSteps, tvPoints, tvPercentage;
    private ProgressBar progressBarSteps;
    private Button btnPlay, btnTest, btnProgress, btnTestWeek, btnHelp;
    private StepsNotificationManager stepsNotificationManager;
    private ProgressManager progressManager;

    private static final long DISPLAY_ANIMATION_MS = 700;
    // O que o ecrã está a mostrar; o próximo valor anima a partir daqui
    private int shownSteps = 0;
    private int shownPoints = 0;
    private ValueAnimator displayAnimator;

    private SensorManager sensorManager;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        SystemBars.applyPadding(findViewById(R.id.mainScroll));

        Log.d(TAG, "🚀 MainActivity onCreate()");

        initializeViews();
        setupClickListeners();

        // Verificar sensores disponíveis
        checkSensors();

        // INICIALIZAR NOTIFICAÇÃO E PROGRESS MANAGER
        stepsNotificationManager = new StepsNotificationManager(this);
        progressManager = new ProgressManager(this);

        // Verificar permissões e estado
        checkPermissions();
        checkCurrentSteps();

        Log.d(TAG, "✅ MainActivity inicializada com sucesso");
    }

    private void checkSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            boolean hasStepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null;
            boolean hasStepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null;

            Log.d(TAG, "📱 Sensores disponíveis:");
            Log.d(TAG, "   TYPE_STEP_COUNTER: " + hasStepCounter);
            Log.d(TAG, "   TYPE_STEP_DETECTOR: " + hasStepDetector);

            if (hasStepCounter || hasStepDetector) {
                String sensorType = hasStepCounter ? "STEP_COUNTER" : "STEP_DETECTOR";
                Log.d(TAG, "🎯 Sensor " + sensorType + " disponível - Pode testar andando!");
                Toast.makeText(this, R.string.main_sensor_detected, Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "📝 Nenhum sensor - Use botão TESTAR");
                Toast.makeText(this, R.string.main_sensor_missing, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkCurrentSteps() {
        int currentSteps = StepCounter.getTodaySteps(this);
        Log.d(TAG, "📊 Passos atuais: " + currentSteps);
    }

    private void initializeViews() {
        tvSteps = findViewById(R.id.tvSteps);
        tvPoints = findViewById(R.id.tvPoints);
        tvPercentage = findViewById(R.id.tvPercentage);
        progressBarSteps = findViewById(R.id.progressBarSteps);
        btnPlay = findViewById(R.id.btnPlay);
        btnTest = findViewById(R.id.btnTest);
        btnProgress = findViewById(R.id.btnProgress);
        btnTestWeek = findViewById(R.id.btnTestWeek);
        btnHelp = findViewById(R.id.btnHelp);

        progressBarSteps.setMax(MAX_STEPS);

        // Botões de demonstração só existem nas versões de debug
        if (BuildConfig.DEBUG) {
            btnTest.setText(getString(R.string.main_debug_add_steps, Constants.TEST_STEPS_INCREMENT));
        } else {
            findViewById(R.id.debugRow).setVisibility(android.view.View.GONE);
        }

        Log.d(TAG, "✅ Views inicializadas");
    }

    private void setupClickListeners() {
        btnPlay.setOnClickListener(v -> navigateToPlayerSelection());
        btnTest.setOnClickListener(v -> simulateTestSteps());
        btnProgress.setOnClickListener(v -> navigateToProgress());
        btnTestWeek.setOnClickListener(v -> simulateTestWeek());
        btnHelp.setOnClickListener(v -> navigateToHelp());

        Log.d(TAG, "✅ Listeners configurados");
    }

    private void navigateToPlayerSelection() {
        int pontosDisponiveis = StepCounter.getTodayPoints(this);
        Log.d(TAG, "🎮 Indo para PlayerSelection com " + pontosDisponiveis + " pontos");

        int minimo = PlayerCatalog.cheapestCost();
        if (pontosDisponiveis < minimo) {
            Toast.makeText(this, getString(R.string.main_need_points, minimo), Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, PlayerSelectionActivity.class);
        startActivity(intent);
    }

    private void navigateToProgress() {
        Log.d(TAG, "📊 Indo para ProgressActivity");
        Intent intent = new Intent(MainActivity.this, ProgressActivity.class);
        startActivity(intent);
    }

    private void navigateToHelp() {
        Log.d(TAG, "❓ Indo para HelpActivity");
        Intent intent = new Intent(MainActivity.this, HelpActivity.class);
        startActivity(intent);
    }

    // BOTÃO TESTAR ORIGINAL (adiciona passos manualmente)
    private void simulateTestSteps() {
        Log.d(TAG, "🧪 Simulando 1000 passos...");

        // O StepCounter guarda os passos de hoje e regista-os também na base de dados
        StepCounter.addSteps(this, Constants.TEST_STEPS_INCREMENT);


        atualizarDisplay();

        if (stepsNotificationManager != null) {
            stepsNotificationManager.updateStepsNotification();
        }

        int currentSteps = StepCounter.getTodaySteps(this);
        Log.d(TAG, "✅ 1000 passos adicionados! Total: " + currentSteps);
        Toast.makeText(this, getString(R.string.main_test_steps_added, Constants.TEST_STEPS_INCREMENT, currentSteps), Toast.LENGTH_SHORT).show();
    }

    // ✅ BOTÃO: TESTAR SEMANA (apenas dados temporários para mostrar ao professor)
    private void simulateTestWeek() {
        Log.d(TAG, "📅 Criando dados de teste TEMPORÁRIOS...");

        if (progressManager != null) {
            progressManager.createTestWeekData();
        }

        atualizarDisplay();
        Log.d(TAG, "✅ Dados de teste TEMPORÁRIOS criados!");
        Toast.makeText(this, R.string.main_test_week_created, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "🔄 MainActivity onResume()");
        atualizarDisplay();

        if (stepsNotificationManager != null) {
            stepsNotificationManager.updateStepsNotification();
        }

        // Verificar estado atual
        checkCurrentSteps();
    }

    /** Mostra passos, barra e pontos; os números "sobem" a partir do que estava no ecrã. */
    private void atualizarDisplay() {
        final int targetSteps = StepCounter.getTodaySteps(this);
        final int targetPoints = StepCounter.getTodayPoints(this); // saldo: ganhos − gastos

        if (displayAnimator != null) displayAnimator.cancel();

        if (targetSteps == shownSteps && targetPoints == shownPoints) {
            render(targetSteps, targetPoints);
            return;
        }

        final int fromSteps = shownSteps;
        final int fromPoints = shownPoints;
        displayAnimator = ValueAnimator.ofFloat(0f, 1f);
        displayAnimator.setDuration(DISPLAY_ANIMATION_MS);
        displayAnimator.setInterpolator(new DecelerateInterpolator());
        displayAnimator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            render(Math.round(fromSteps + (targetSteps - fromSteps) * t),
                    Math.round(fromPoints + (targetPoints - fromPoints) * t));
        });
        displayAnimator.start();

        // Guarda já o destino: se o ecrã for atualizado outra vez a meio, anima a partir daí
        shownSteps = targetSteps;
        shownPoints = targetPoints;
        Log.d(TAG, "📊 Display atualizado: " + targetSteps + " passos, " + targetPoints + " pontos");
    }

    private void render(int steps, int points) {
        int barSteps = Math.min(steps, MAX_STEPS);
        tvSteps.setText(getString(R.string.main_steps_display, steps, MAX_STEPS));
        tvPoints.setText(getString(R.string.main_points_display, points));
        tvPercentage.setText(getString(R.string.main_percentage, (barSteps * 100) / MAX_STEPS));
        progressBarSteps.setProgress(barSteps);
        updateProgressBarColor(barSteps);
    }

    private void updateProgressBarColor(int steps) {
        int progress = (steps * 100) / MAX_STEPS;
        if (progress >= 100) {
            progressBarSteps.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.progress_goal_reached)));
        } else if (progress >= 75) {
            progressBarSteps.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.progress_high)));
        } else {
            progressBarSteps.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.progress_normal)));
        }
    }

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean sensorAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACTIVITY_RECOGNITION))
                        || hasPermission(Manifest.permission.ACTIVITY_RECOGNITION);
                if (sensorAllowed) {
                    Toast.makeText(this, R.string.main_permission_granted, Toast.LENGTH_SHORT).show();
                    startServices();
                } else {
                    Toast.makeText(this, R.string.main_permission_denied, Toast.LENGTH_LONG).show();
                }
            });

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /** Pede de uma só vez as permissões que faltam; se já estiverem todas, arranca os serviços. */
    private void checkPermissions() {
        List<String> missing = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && !hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)) {
            missing.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (missing.isEmpty()) {
            startServices();
        } else {
            permissionLauncher.launch(missing.toArray(new String[0]));
        }
    }

    private void startServices() {
        Log.d(TAG, "🚀 Iniciando serviços...");
        startStepTrackingService();
        startStepsNotification();
    }

    private void startStepTrackingService() {
        Log.d(TAG, "🔄 Iniciando StepTrackingService");
        Intent serviceIntent = new Intent(this, StepTrackingService.class);
        startService(serviceIntent);
    }

    private void startStepsNotification() {
        if (stepsNotificationManager != null) {
            stepsNotificationManager.startStepsNotification();
            Log.d(TAG, "🔔 StepsNotification iniciado");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (displayAnimator != null) displayAnimator.cancel();
        Log.d(TAG, "🛑 MainActivity onDestroy()");
    }
}