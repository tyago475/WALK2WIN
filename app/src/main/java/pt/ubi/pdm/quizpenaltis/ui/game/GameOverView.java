package pt.ubi.pdm.quizpenaltis.ui.game;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.game.PenaltyShootout;
import pt.ubi.pdm.quizpenaltis.util.Animations;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

/** O ecrã de fim de jogo (vitória / derrota): medalha, resultado, marcador e botão de voltar. */
public class GameOverView {
    private final View root;
    private final Context context;

    public GameOverView(View root) {
        this.root = root;
        this.context = root.getContext();
    }

    public void show(PenaltyShootout shootout, String userName, String botName, Runnable onBack) {
        TextView tvTitle = root.findViewById(R.id.tvGameOverTitle);
        int userScore = shootout.getPlayerScore();
        int botScore = shootout.getBotScore();
        boolean won = shootout.playerWon();

        if (shootout.isSuddenDeath()) {
            tvTitle.setText(R.string.gameover_sd_title);
            if (won) {
                fill(R.string.gameover_sd_win, R.color.gold_500, R.drawable.icon_bolt, R.drawable.ds_coin,
                        R.color.text_on_gold, context.getString(R.string.gameover_sd_win_message, userName));
            } else {
                fill(R.string.gameover_sd_lose, R.color.feedback_error, R.drawable.icon_close,
                        R.drawable.ds_badge_bad, R.color.text_primary,
                        context.getString(R.string.gameover_sd_lose_message, botName));
            }
        } else if (won) {
            tvTitle.setText(R.string.gameover_title);
            fill(R.string.gameover_win, R.color.gold_500, R.drawable.icon_emoji_events, R.drawable.ds_coin,
                    R.color.text_on_gold, context.getString(R.string.gameover_win_message, userName));
        } else if (userScore < botScore) {
            tvTitle.setText(R.string.gameover_title);
            fill(R.string.gameover_lose, R.color.feedback_error, R.drawable.icon_close,
                    R.drawable.ds_badge_bad, R.color.text_primary,
                    context.getString(R.string.gameover_lose_message, botName));
        } else {
            tvTitle.setText(R.string.gameover_title);
            fill(R.string.gameover_draw, R.color.feedback_info, R.drawable.icon_flag,
                    R.drawable.ds_badge_info, R.color.text_primary,
                    context.getString(R.string.gameover_draw_message));
        }

        ((TextView) root.findViewById(R.id.tvGameOverScore))
                .setText(context.getString(R.string.game_score_format, userScore, botScore));
        ((TextView) root.findViewById(R.id.tvGameOverPlayers))
                .setText(context.getString(R.string.gameover_players, userName, botName));
        root.findViewById(R.id.btnBackToMain).setOnClickListener(v -> onBack.run());
        root.setVisibility(View.VISIBLE);

        Animations.pop(root.findViewById(R.id.gameOverBadge));
        Animations.popIn(root.findViewById(R.id.tvGameOverResult));
    }

    private void fill(@StringRes int resultText, @ColorRes int resultColor, @DrawableRes int icon,
                      @DrawableRes int badge, @ColorRes int iconColor, String messageText) {
        TextView result = root.findViewById(R.id.tvGameOverResult);
        result.setText(resultText);
        result.setTextColor(ContextCompat.getColor(context, resultColor));

        root.findViewById(R.id.gameOverBadge).setBackgroundResource(badge);
        ImageView iconView = root.findViewById(R.id.ivGameOverIcon);
        iconView.setImageResource(icon);
        ImageViewCompat.setImageTintList(iconView,
                ColorStateList.valueOf(ContextCompat.getColor(context, iconColor)));

        ((TextView) root.findViewById(R.id.tvGameOverMessage)).setText(messageText);
    }
}
