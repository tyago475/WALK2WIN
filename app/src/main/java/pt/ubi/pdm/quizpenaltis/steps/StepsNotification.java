package pt.ubi.pdm.quizpenaltis.steps;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.ui.main.MainActivity;
import pt.ubi.pdm.quizpenaltis.util.Constants;

/** Constrói a notificação permanente com os passos de hoje (usada pelo serviço e pelas atualizações). */
final class StepsNotification {
    static final int ID = 100;
    private static final String CHANNEL_ID = "steps_progress_channel";

    private StepsNotification() {}

    static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(context.getString(R.string.notif_channel_description));
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    /**
     * Passa o serviço a primeiro plano (tipo health) com a notificação de passos.
     * Quem arranca um serviço com startForegroundService() TEM de chamar isto em poucos segundos.
     *
     * @return false se não foi possível (por exemplo, sem a permissão ACTIVITY_RECOGNITION)
     */
    static boolean enterForeground(Service service) {
        try {
            ensureChannel(service);
            // Do Android 14 em diante é obrigatório declarar o tipo do serviço (health, no manifest).
            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    ? ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                    : ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST;
            ServiceCompat.startForeground(service, ID, build(service), type);
            return true;
        } catch (Exception e) {
            Log.e("StepsNotification", "Não foi possível passar a primeiro plano", e);
            return false;
        }
    }

    static Notification build(Context context) {
        int steps = StepCounter.getTodaySteps(context);
        int goal = Constants.DAILY_STEP_GOAL;
        int percentage = (steps * 100) / goal;

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notif_title))
                .setContentText(context.getString(R.string.notif_text, steps, goal, percentage))
                .setSmallIcon(R.drawable.icon_directions_walk)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }
}
