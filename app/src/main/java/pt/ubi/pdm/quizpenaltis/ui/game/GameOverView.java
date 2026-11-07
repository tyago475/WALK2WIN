package pt.ubi.pdm.quizpenaltis.ui.game;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.game.PenaltyShootout;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/** O cartão de fim de jogo (vitória / derrota). */
public class GameOverView {
    private final View root;
    private final Context context;

    public GameOverView(View root) {
        this.root = root;
        this.context = root.getContext();
    }

    public void show(PenaltyShootout shootout, String userName, String botName, Runnable onBack) {
        TextView tvTitle = root.findViewById(R.id.tvGameOverTitle);
        TextView tvResult = root.findViewById(R.id.tvGameOverResult);
        TextView tvScore = root.findViewById(R.id.tvGameOverScore);
        TextView tvMessage = root.findViewById(R.id.tvGameOverMessage);
        TextView tvIcon = root.findViewById(R.id.tvGameOverIcon);

        int userScore = shootout.getPlayerScore();
        int botScore = shootout.getBotScore();
        boolean won = shootout.playerWon();

        if (shootout.isSuddenDeath()) {
            tvTitle.setText(R.string.gameover_sd_title);
            if (won) {
                fill(tvResult, tvIcon, tvMessage, R.string.gameover_sd_win, R.color.result_win,
                        R.string.emoji_bolt, context.getString(R.string.gameover_sd_win_message, userName));
            } else {
                fill(tvResult, tvIcon, tvMessage, R.string.gameover_sd_lose, R.color.result_lose,
                        R.string.emoji_broken_heart, context.getString(R.string.gameover_sd_lose_message, botName));
            }
        } else if (won) {
            fill(tvResult, tvIcon, tvMessage, R.string.gameover_win, R.color.result_win,
                    R.string.emoji_trophy, context.getString(R.string.gameover_win_message, userName));
        } else if (userScore < botScore) {
            fill(tvResult, tvIcon, tvMessage, R.string.gameover_lose, R.color.result_lose,
                    R.string.emoji_sad, context.getString(R.string.gameover_lose_message, botName));
        } else {
            fill(tvResult, tvIcon, tvMessage, R.string.gameover_draw, R.color.result_draw,
                    R.string.emoji_handshake, context.getString(R.string.gameover_draw_message));
        }

        tvScore.setText(context.getString(R.string.game_score_format, userScore, botScore));
        root.findViewById(R.id.btnBackToMain).setOnClickListener(v -> onBack.run());
        root.setVisibility(View.VISIBLE);
    }

    private void fill(TextView result, TextView icon, TextView message,
                      int resultText, int resultColor, int iconText, String messageText) {
        result.setText(resultText);
        result.setTextColor(ContextCompat.getColor(context, resultColor));
        icon.setText(iconText);
        message.setText(messageText);
    }
}
