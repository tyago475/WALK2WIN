package pt.ubi.pdm.quizpenaltis.steps;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.ui.main.MainActivity;
import pt.ubi.pdm.quizpenaltis.util.Constants;
import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
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
                        getString(R.string.notif_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription(getString(R.string.notif_channel_description));
                channel.setShowBadge(false);
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                Log.e("StepsNotification", "Erro na notificação", e);
            }
        }
    }

    private void updateNotification() {
        try {
            int currentSteps = StepCounter.getTodaySteps(this);
            int maxSteps = Constants.DAILY_STEP_GOAL;
            int percentage = maxSteps > 0 ? (currentSteps * 100) / maxSteps : 0;

            String contentText = getString(R.string.notif_text, currentSteps, maxSteps, percentage);

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.notif_title))
                    .setContentText(contentText)
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
            Log.e("StepsNotification", "Erro na notificação", e);
        }
    }

    private void showRegularNotification() {
        try {
            int currentSteps = StepCounter.getTodaySteps(this);
            int maxSteps = Constants.DAILY_STEP_GOAL;
            int percentage = maxSteps > 0 ? (currentSteps * 100) / maxSteps : 0;

            String contentText = getString(R.string.notif_text, currentSteps, maxSteps, percentage);

            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.notif_title))
                    .setContentText(contentText)
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
            Log.e("StepsNotification", "Erro na notificação", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}