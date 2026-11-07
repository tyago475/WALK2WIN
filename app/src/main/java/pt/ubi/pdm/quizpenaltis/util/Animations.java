package pt.ubi.pdm.quizpenaltis.util;

import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * Animações curtas e reutilizáveis para dar vida ao jogo.
 * Usam o sistema de animação do Android, por isso respeitam a opção
 * "remover animações" das definições do telemóvel.
 */
public final class Animations {
    private Animations() {}

    /** Aparece a crescer com um pequeno "ressalto" (mensagens, resultados). */
    public static void popIn(View view) {
        view.setScaleX(0.85f);
        view.setScaleY(0.85f);
        view.setAlpha(0.4f);
        view.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(240)
                .setInterpolator(new OvershootInterpolator(1.6f))
                .start();
    }

    /** Surge do nada com ressalto (por exemplo, um ponto da pontuação a encher). */
    public static void pop(View view) {
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.animate()
                .scaleX(1f).scaleY(1f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(3f))
                .start();
    }

    /** Pulso rápido: cresce e volta ao tamanho normal. */
    public static void pulse(View view) {
        view.animate()
                .scaleX(1.06f).scaleY(1.06f)
                .setDuration(120)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                .start();
    }

    /** Abanão horizontal (resposta errada, remate falhado). */
    public static void shake(View view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X,
                0f, -14f, 14f, -10f, 10f, -5f, 5f, 0f);
        animator.setDuration(380);
        animator.start();
    }
}
