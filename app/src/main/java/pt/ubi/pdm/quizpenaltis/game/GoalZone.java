package pt.ubi.pdm.quizpenaltis.game;

import pt.ubi.pdm.quizpenaltis.R;
import androidx.annotation.IdRes;

/** As 15 posições da baliza onde o jogador pode rematar (3 filas x 5 colunas). */
public enum GoalZone {
    TOP_LEFT(R.id.circle_1_1), TOP_LEFT_CENTER(R.id.circle_1_2), TOP_CENTER(R.id.circle_1_3),
    TOP_RIGHT_CENTER(R.id.circle_1_4), TOP_RIGHT(R.id.circle_1_5),
    MIDDLE_LEFT(R.id.circle_2_1), MIDDLE_CENTER_LEFT(R.id.circle_2_2), MIDDLE_CENTER(R.id.circle_2_3),
    MIDDLE_CENTER_RIGHT(R.id.circle_2_4), MIDDLE_RIGHT(R.id.circle_2_5),
    BOTTOM_LEFT(R.id.circle_3_1), BOTTOM_LEFT_CENTER(R.id.circle_3_2), BOTTOM_CENTER(R.id.circle_3_3),
    BOTTOM_RIGHT_CENTER(R.id.circle_3_4), BOTTOM_RIGHT(R.id.circle_3_5);

    @IdRes public final int viewId;

    GoalZone(@IdRes int viewId) {
        this.viewId = viewId;
    }
}
