package pt.ubi.pdm.quizpenaltis.steps;

import pt.ubi.pdm.quizpenaltis.ui.main.MainActivity;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.content.Context;
import android.util.Log;

public class StepTrackingService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private Sensor stepDetectorSensor;
    private boolean isTracking = false;
    private StepsNotificationManager notificationManager;

    private static final String TAG = "StepTrackingService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() - Iniciando serviço");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

            Log.d(TAG, "TYPE_STEP_COUNTER disponível: " + (stepSensor != null));
            Log.d(TAG, "TYPE_STEP_DETECTOR disponível: " + (stepDetectorSensor != null));
        }

        notificationManager = new StepsNotificationManager(this);
        Log.d(TAG, "NotificationManager inicializado");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() - Iniciando tracking");
        // Foreground (health): sem isto o Android pode matar o serviço em segundo plano e os passos deixam de contar.
        if (!StepsNotification.enterForeground(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startStepTracking();
        return START_STICKY;
    }

    private void startStepTracking() {
        if (stepSensor != null && !isTracking) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            isTracking = true;
            Log.d(TAG, "TYPE_STEP_COUNTER registrado com sucesso");
            Log.d(TAG, "AGORA ANDE COM O TELEMÓVEL PARA TESTAR!");
        } else if (stepDetectorSensor != null && !isTracking) {
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
            isTracking = true;
            Log.d(TAG, "TYPE_STEP_DETECTOR registrado como alternativa");
            Log.d(TAG, "AGORA ANDE COM O TELEMÓVEL PARA TESTAR!");
        } else {
            Log.e(TAG, "Nenhum sensor de passos disponível!");
            Log.d(TAG, "Use o botão TESTAR na MainActivity para adicionar passos manualmente");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalStepsSinceReboot = (int) event.values[0];

            Log.d(TAG, "TYPE_STEP_COUNTER: " + totalStepsSinceReboot + " passos totais desde reboot");

            // USA O MÉTODO CORRETO que calcula os passos do dia
            StepCounter.updateFromStepCounter(this, totalStepsSinceReboot);

            int storedSteps = StepCounter.getTodaySteps(this);
            Log.d(TAG, "Passos de HOJE: " + storedSteps);

            if (notificationManager != null) {
                notificationManager.updateStepsNotification();
                Log.d(TAG, "Notificação atualizada");
            }

        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            Log.d(TAG, "TYPE_STEP_DETECTOR: PASSO INDIVIDUAL DETETADO!");

            // Para STEP_DETECTOR, adiciona 1 passo normalmente
            StepCounter.addSteps(this, 1);

            int storedSteps = StepCounter.getTodaySteps(this);
            Log.d(TAG, "STEP_DETECTOR - Passos totais HOJE: " + storedSteps);

            if (notificationManager != null) {
                notificationManager.updateStepsNotification();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Precisão do sensor alterada: " + accuracy);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() - Parando serviço");
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
}