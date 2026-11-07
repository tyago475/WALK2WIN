package pt.ubi.pdm.quizpenaltis;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = Constants.REQUEST_ACTIVITY_RECOGNITION;
    private static final int NOTIFICATION_PERMISSION_CODE = Constants.REQUEST_POST_NOTIFICATIONS;
    private static final int MAX_STEPS = Constants.DAILY_STEP_GOAL;

    private TextView tvSteps, tvPoints, tvPercentage;
    private ProgressBar progressBarSteps;
    private Button btnPlay, btnTest, btnProgress, btnTestWeek, btnHelp;
    private StepsNotificationManager stepsNotificationManager;
    private ProgressManager progressManager;

    private SensorManager sensorManager;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

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

        if (pontosDisponiveis < Constants.MIN_POINTS_TO_PLAY) {
            Toast.makeText(this, getString(R.string.main_need_points, Constants.MIN_POINTS_TO_PLAY), Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, PlayerSelectionActivity.class);
        intent.putExtra(Constants.EXTRA_AVAILABLE_POINTS, pontosDisponiveis);
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

        // 1. Adicionar passos ao StepCounter
        StepCounter.addSteps(this, Constants.TEST_STEPS_INCREMENT);

        // 2. Adicionar passos ao ProgressManager para HOJE
        if (progressManager != null) {
            progressManager.addSteps(Constants.TEST_STEPS_INCREMENT);
        }

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

    private void atualizarDisplay() {
        int passos = StepCounter.getTodaySteps(this);
        int pontos = Constants.stepsToPoints(passos);
        int passosParaBarra = Math.min(passos, MAX_STEPS);
        int percentagem = (passosParaBarra * 100) / MAX_STEPS;

        tvSteps.setText(getString(R.string.main_steps_display, passos, MAX_STEPS));
        tvPoints.setText(getString(R.string.main_points_display, pontos));
        tvPercentage.setText(getString(R.string.main_percentage, percentagem));

        progressBarSteps.setProgress(passosParaBarra);
        updateProgressBarColor(passosParaBarra);

        Log.d(TAG, "📊 Display atualizado: " + passos + " passos, " + pontos + " pontos");
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

    private void checkPermissions() {
        Log.d(TAG, "🔐 Verificando permissões...");

        // 🔥 PEDIR PERMISSÃO ACTIVITY_RECOGNITION (OBRIGATÓRIA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "📋 A PEDIR permissão ACTIVITY_RECOGNITION...");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        ACTIVITY_RECOGNITION_REQUEST_CODE);
                return;
            } else {
                Log.d(TAG, "✅ Permissão ACTIVITY_RECOGNITION JÁ CONCEDIDA");
            }
        }

        // Permissão para notificações (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "📋 A pedir permissão POST_NOTIFICATIONS");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
                return;
            } else {
                Log.d(TAG, "✅ Permissão POST_NOTIFICATIONS já concedida");
            }
        }

        Log.d(TAG, "✅ Todas as permissões concedidas - iniciando serviços");
        startServices();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "📋 Resultado da permissão: " + requestCode);

        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "🎉 PERMISSÃO ACTIVITY_RECOGNITION CONCEDIDA!");
                Toast.makeText(this, R.string.main_permission_granted, Toast.LENGTH_SHORT).show();
                startServices();
            } else {
                Log.e(TAG, "❌ PERMISSÃO ACTIVITY_RECOGNITION NEGADA!");
                Toast.makeText(this, R.string.main_permission_denied, Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ Permissão POST_NOTIFICATIONS concedida");
                startServices();
            }
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
        Log.d(TAG, "🛑 MainActivity onDestroy()");
        if (progressManager != null) {
            progressManager.close();
        }
    }
}