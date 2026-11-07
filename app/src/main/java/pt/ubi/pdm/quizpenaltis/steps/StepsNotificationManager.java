package pt.ubi.pdm.quizpenaltis.steps;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;

public class StepsNotificationManager {
    private final Context context;

    public StepsNotificationManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Arranca o serviço em primeiro plano (que mostra a notificação). */
    public void startStepsNotification() {
        Intent serviceIntent = new Intent(context, StepsNotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    /**
     * Atualiza o texto da notificação, só se o serviço já a estiver a mostrar.
     * Não volta a arrancar o serviço: um startService() vindo de segundo plano falha no Android 8+.
     */
    public void updateStepsNotification() {
        if (!isNotificationShowing()) return;
        NotificationManagerCompat compat = NotificationManagerCompat.from(context);
        if (compat.areNotificationsEnabled()) {
            compat.notify(StepsNotification.ID, StepsNotification.build(context));
        }
    }

    public void stopStepsNotification() {
        context.stopService(new Intent(context, StepsNotificationService.class));
    }

    private boolean isNotificationShowing() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return false;
        for (android.service.notification.StatusBarNotification n : manager.getActiveNotifications()) {
            if (n.getId() == StepsNotification.ID) return true;
        }
        return false;
    }
}
