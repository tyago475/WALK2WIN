package pt.ubi.pdm.quizpenaltis.ui.game;

import android.widget.ImageView;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.util.Animations;

/**
 * Os 2 x 5 pontos do cabeçalho do jogo (verde = golo, vermelho = falhou).
 * Só trata do desenho; as regras estão em PenaltyShootout.
 */
public class ScoreBoard {
    private final ImageView[] playerDots;
    private final ImageView[] botDots;
    private int playerIndex = 0;
    private int botIndex = 0;

    public ScoreBoard(ImageView[] playerDots, ImageView[] botDots) {
        this.playerDots = playerDots;
        this.botDots = botDots;
    }

    public void reset() {
        for (ImageView dot : playerDots) dot.setImageResource(R.drawable.ds_score_empty);
        for (ImageView dot : botDots) dot.setImageResource(R.drawable.ds_score_empty);
        playerIndex = 0;
        botIndex = 0;
    }

    public void recordPlayer(boolean scored) {
        if (playerIndex < playerDots.length) fill(playerDots[playerIndex++], scored);
    }

    public void recordBot(boolean scored) {
        if (botIndex < botDots.length) fill(botDots[botIndex++], scored);
    }

    private static void fill(ImageView dot, boolean scored) {
        dot.setImageResource(scored ? R.drawable.ds_score_goal : R.drawable.ds_score_miss);
        Animations.pop(dot);
    }
}
