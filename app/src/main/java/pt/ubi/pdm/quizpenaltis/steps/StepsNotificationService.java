package pt.ubi.pdm.quizpenaltis.steps;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Serviço em primeiro plano que mantém a notificação de passos.
 *
 * Quem o arranca com startForegroundService() tem de ver startForeground() chamado
 * em poucos segundos, em TODAS as versões do Android; senão o sistema fecha a app com
 * ForegroundServiceDidNotStartInTimeException.
 */
public class StepsNotificationService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!StepsNotification.enterForeground(this)) {
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
