package pt.ubi.pdm.quizpenaltis;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;

import java.util.Random;

/** As 5 zonas que o jogador pode defender. */
public enum DefenseZone {
    TOP_LEFT(R.id.rect_top_left, R.string.zone_top_left),
    TOP_RIGHT(R.id.rect_top_right, R.string.zone_top_right),
    CENTER(R.id.rect_center, R.string.zone_center),
    BOTTOM_LEFT(R.id.rect_bottom_left, R.string.zone_bottom_left),
    BOTTOM_RIGHT(R.id.rect_bottom_right, R.string.zone_bottom_right);

    @IdRes public final int viewId;
    @StringRes public final int nameRes;

    DefenseZone(@IdRes int viewId, @StringRes int nameRes) {
        this.viewId = viewId;
        this.nameRes = nameRes;
    }

    private static final Random RANDOM = new Random();

    public static DefenseZone random() {
        DefenseZone[] all = values();
        return all[RANDOM.nextInt(all.length)];
    }
}
