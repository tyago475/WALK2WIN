package pt.ubi.pdm.quizpenaltis.ui.game;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.game.PenaltyShootout;
import android.widget.ImageView;

/**
 * Os 2 x 5 círculos do cabeçalho do jogo (verde = golo, vermelho = falhou).
 * Só trata do desenho; as regras estão em {@link PenaltyShootout}.
 */
public class ScoreBoard {
    private final ImageView[] playerCircles;
    private final ImageView[] botCircles;
    private int playerIndex = 0;
    private int botIndex = 0;

    public ScoreBoard(ImageView[] playerCircles, ImageView[] botCircles) {
        this.playerCircles = playerCircles;
        this.botCircles = botCircles;
    }

    public void reset() {
        for (ImageView c : playerCircles) c.setImageResource(R.drawable.score_circle_gray);
        for (ImageView c : botCircles) c.setImageResource(R.drawable.score_circle_gray);
        playerIndex = 0;
        botIndex = 0;
    }

    public void recordPlayer(boolean scored) {
        if (playerIndex < playerCircles.length) {
            playerCircles[playerIndex++].setImageResource(drawableFor(scored));
        }
    }

    public void recordBot(boolean scored) {
        if (botIndex < botCircles.length) {
            botCircles[botIndex++].setImageResource(drawableFor(scored));
        }
    }

    private static int drawableFor(boolean scored) {
        return scored ? R.drawable.score_circle_green : R.drawable.score_circle_red;
    }
}
