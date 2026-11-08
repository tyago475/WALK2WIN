package pt.ubi.pdm.quizpenaltis.game;

import pt.ubi.pdm.quizpenaltis.R;
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

    private static final int GOAL_COLUMNS = 5;
    private static final int MIDDLE_COLUMN = 2;
    private static final int LEFT_COLUMNS_END = 1;
    private static final int LAST_ROW = 2;

    /** A zona de defesa que cobre um remate (multiplayer): coluna central = CENTER, senão o canto do lado. */
    public static DefenseZone covering(GoalZone shot) {
        int row = shot.ordinal() / GOAL_COLUMNS;
        int col = shot.ordinal() % GOAL_COLUMNS;
        if (col == MIDDLE_COLUMN) return CENTER;
        boolean left = col <= LEFT_COLUMNS_END;
        if (row == LAST_ROW) return left ? BOTTOM_LEFT : BOTTOM_RIGHT;
        return left ? TOP_LEFT : TOP_RIGHT;
    }
}
