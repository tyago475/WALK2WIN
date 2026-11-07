package pt.ubi.pdm.quizpenaltis;

import android.os.CountDownTimer;

/** Contagem decrescente de uma pergunta, a avisar a cada segundo. */
public class GameTimer {

    public interface Listener {
        void onTick(int secondsRemaining);
        void onFinish();
    }

    private final int totalSeconds;
    private final Listener listener;
    private CountDownTimer timer;

    public GameTimer(int totalSeconds, Listener listener) {
        this.totalSeconds = totalSeconds;
        this.listener = listener;
    }

    public void start() {
        cancel();
        timer = new CountDownTimer(totalSeconds * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                listener.onTick((int) (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                timer = null;
                listener.onFinish();
            }
        };
        timer.start();
    }

    public void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
