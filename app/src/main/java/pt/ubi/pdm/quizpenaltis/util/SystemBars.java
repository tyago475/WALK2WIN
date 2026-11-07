package pt.ubi.pdm.quizpenaltis.util;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * No Android 15+ as apps desenham por baixo da barra de estado e da barra de navegação.
 * Isto afasta o conteúdo dessas barras, mantendo o fundo a ocupar o ecrã inteiro.
 */
public final class SystemBars {
    private SystemBars() {}

    /** Põe, no padding da view, o espaço ocupado pelas barras do sistema (somado ao padding original). */
    public static void applyPadding(View root) {
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
