package pt.ubi.pdm.quizpenaltis;

import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class StepsNotificationService extends Service {
    private static final int NOTIFICATION_ID = 100;
    private static final String CHANNEL_ID = "steps_progress_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (canUseForegroundService()) {
            updateNotification();
        } else {
            showRegularNotification();
        }
        return START_STICKY;
    }

    private boolean canUseForegroundService() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Progresso de Passos",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Mostra o progresso diário de passos");
                channel.setShowBadge(false);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateNotification() {
        try {
            int currentSteps = StepCounter.getTodaySteps(this);
            int maxSteps = Constants.DAILY_STEP_GOAL;
            int percentage = maxSteps > 0 ? (currentSteps * 100) / maxSteps : 0;

            String stepsText = String.format("%,d / %,d passos", currentSteps, maxSteps);
            String percentageText = percentage + "%";

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("🚶‍♂️ Walk2Win")
                    .setContentText(stepsText + " • " + percentageText)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .build();

            if (canUseForegroundService()) {
                startForeground(NOTIFICATION_ID, notification);
            } else {
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.notify(NOTIFICATION_ID, notification);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showRegularNotification() {
        try {
            int currentSteps = StepCounter.getTodaySteps(this);
            int maxSteps = Constants.DAILY_STEP_GOAL;
            int percentage = maxSteps > 0 ? (currentSteps * 100) / maxSteps : 0;

            String stepsText = String.format("%,d / %,d passos", currentSteps, maxSteps);

            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("🚶‍♂️ Walk2Win")
                    .setContentText(stepsText + " • " + percentage + "%")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(false)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}