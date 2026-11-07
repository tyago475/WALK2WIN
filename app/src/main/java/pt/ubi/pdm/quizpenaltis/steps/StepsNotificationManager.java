package pt.ubi.pdm.quizpenaltis.steps;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class StepsNotificationManager {
    private Context context;

    public StepsNotificationManager(Context context) {
        this.context = context;
    }

    public void startStepsNotification() {
        Intent serviceIntent = new Intent(context, StepsNotificationService.class);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            context.startForegroundService(serviceIntent);
        } else {

            context.startService(serviceIntent);
        }
    }

    public void updateStepsNotification() {
        Intent serviceIntent = new Intent(context, StepsNotificationService.class);
        context.startService(serviceIntent);
    }

    public void stopStepsNotification() {
        Intent serviceIntent = new Intent(context, StepsNotificationService.class);
        context.stopService(serviceIntent);
    }
}