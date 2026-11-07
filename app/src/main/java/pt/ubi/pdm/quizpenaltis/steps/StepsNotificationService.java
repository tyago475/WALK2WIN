package pt.ubi.pdm.quizpenaltis.steps;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ServiceCompat;

/**
 * Serviço em primeiro plano que mantém a notificação de passos.
 *
 * Quem o arranca com startForegroundService() tem de ver startForeground() chamado
 * em poucos segundos, em TODAS as versões do Android; senão o sistema fecha a app com
 * ForegroundServiceDidNotStartInTimeException.
 */
public class StepsNotificationService extends Service {
    private static final String TAG = "StepsNotification";

    @Override
    public void onCreate() {
        super.onCreate();
        StepsNotification.ensureChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            // Do Android 14 em diante é obrigatório declarar o tipo do serviço (health, no manifest).
            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    ? ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                    : ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST;
            ServiceCompat.startForeground(this, StepsNotification.ID, StepsNotification.build(this), type);
        } catch (Exception e) {
            // Por exemplo SecurityException se a permissão ACTIVITY_RECOGNITION não foi concedida.
            Log.e(TAG, "Não foi possível passar a primeiro plano", e);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
