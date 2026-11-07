package pt.ubi.pdm.quizpenaltis.ui.game;

import pt.ubi.pdm.quizpenaltis.R;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.Random;

/** Ecrã de "cara ou coroa" que decide quem remata primeiro. */
public class CoinTossPhase {

    private static final long RESULT_DISPLAY_MS = 2500;
    private static final long FLIP_DURATION_MS = 1000;
    private static final long FACE_REVEAL_DELAY_MS = 500;
    private static final long POP_DURATION_MS = 200;
    private static final float POP_SCALE = 1.2f;

    public interface CoinTossListener {
        void onCoinTossResult(boolean userWonToss);
    }

    private final Activity activity;
    private final CoinTossListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private final View root;
    private final Button btnHeads;
    private final Button btnTails;
    private final TextView tvTitle;
    private final TextView tvMessage;
    private final TextView tvCoin;

    public CoinTossPhase(Activity activity, View root, CoinTossListener listener) {
        this.activity = activity;
        this.root = root;
        this.listener = listener;
        btnHeads = root.findViewById(R.id.btnHeads);
        btnTails = root.findViewById(R.id.btnTails);
        tvTitle = root.findViewById(R.id.tvCoinTossTitle);
        tvMessage = root.findViewById(R.id.tvCoinTossMessage);
        tvCoin = root.findViewById(R.id.tvCoinAnimation);

        btnHeads.setOnClickListener(v -> onChoice(true));
        btnTails.setOnClickListener(v -> onChoice(false));
    }

    public void show() {
        root.setVisibility(View.VISIBLE);
        resetUI();
    }

    public void hide() {
        root.setVisibility(View.GONE);
    }

    /** Cancela animações pendentes (chamar no onDestroy). */
    public void release() {
        handler.removeCallbacksAndMessages(null);
    }

    private void onChoice(boolean userChoseHeads) {
        setButtonsEnabled(false);

        boolean coinIsHeads = random.nextBoolean();
        boolean userWon = userChoseHeads == coinIsHeads;

        showResult(userChoseHeads, coinIsHeads, userWon);
        handler.postDelayed(() -> listener.onCoinTossResult(userWon), RESULT_DISPLAY_MS);
    }

    private void showResult(boolean userChoseHeads, boolean coinIsHeads, boolean userWon) {
        String userChoice = faceName(userChoseHeads);
        String coinResult = faceName(coinIsHeads);

        tvTitle.setText(R.string.coin_result_title);
        tvMessage.setText(activity.getString(R.string.coin_result_message,
                userChoice, coinResult,
                activity.getString(userWon ? R.string.coin_user_won : R.string.coin_bot_won),
                activity.getString(userWon ? R.string.game_user_starts : R.string.game_bot_starts)));

        animateFlip(coinIsHeads);

        Button chosen = userChoseHeads ? btnHeads : btnTails;
        Button other = userChoseHeads ? btnTails : btnHeads;
        boolean chosenWasRight = userChoseHeads == coinIsHeads;

        chosen.setBackgroundColor(color(chosenWasRight
                ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
        chosen.setText(activity.getString(chosenWasRight ? R.string.coin_choice_ok : R.string.coin_choice_bad,
                faceName(userChoseHeads)));

        other.setBackgroundColor(color(android.R.color.darker_gray));
        other.setText(faceName(!userChoseHeads));
    }

    private void animateFlip(boolean finalIsHeads) {
        tvCoin.animate().rotationYBy(360).setDuration(FLIP_DURATION_MS).start();

        handler.postDelayed(() -> {
            tvCoin.setText(finalIsHeads ? R.string.coin_face_heads : R.string.coin_face_tails);
            tvCoin.animate().scaleX(POP_SCALE).scaleY(POP_SCALE).setDuration(POP_DURATION_MS)
                    .withEndAction(() -> tvCoin.animate().scaleX(1f).scaleY(1f)
                            .setDuration(POP_DURATION_MS).start())
                    .start();
        }, FACE_REVEAL_DELAY_MS);
    }

    private void resetUI() {
        tvTitle.setText(R.string.coin_title);
        tvMessage.setText(R.string.coin_prompt);
        setButtonsEnabled(true);

        btnHeads.setBackgroundColor(color(android.R.color.holo_red_dark));
        btnHeads.setText(R.string.coin_heads);
        btnTails.setBackgroundColor(color(android.R.color.holo_red_dark));
        btnTails.setText(R.string.coin_tails);

        tvCoin.setText(R.string.coin_initial);
        tvCoin.setRotationY(0);
        tvCoin.setScaleX(1f);
        tvCoin.setScaleY(1f);
    }

    private String faceName(boolean heads) {
        return activity.getString(heads ? R.string.coin_heads : R.string.coin_tails);
    }

    private int color(int resId) {
        return ContextCompat.getColor(activity, resId);
    }

    private void setButtonsEnabled(boolean enabled) {
        btnHeads.setEnabled(enabled);
        btnTails.setEnabled(enabled);
    }
}
