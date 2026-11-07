package pt.ubi.pdm.quizpenaltis.ui.game;

import pt.ubi.pdm.quizpenaltis.R;
import pt.ubi.pdm.quizpenaltis.data.questions.QuestionManager;
import pt.ubi.pdm.quizpenaltis.game.DefenseZone;
import pt.ubi.pdm.quizpenaltis.game.GameRules;
import pt.ubi.pdm.quizpenaltis.game.GoalZone;
import pt.ubi.pdm.quizpenaltis.game.PenaltyShootout;
import pt.ubi.pdm.quizpenaltis.model.Player;
import pt.ubi.pdm.quizpenaltis.model.PlayerCatalog;
import pt.ubi.pdm.quizpenaltis.model.Question;
import pt.ubi.pdm.quizpenaltis.util.Animations;
import pt.ubi.pdm.quizpenaltis.util.Constants;
import pt.ubi.pdm.quizpenaltis.util.SystemBars;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Ecrã do jogo. Coordena as fases (moeda → pergunta → remate/defesa → fim);
 * as regras estão em {@link PenaltyShootout} e os números em {@link GameRules}.
 */
public class GameActivity extends AppCompatActivity {

    private enum Phase { COIN_TOSS, QUESTION, SHOOT, DEFEND, RESULT }

    // ----- Views -----
    private TextView tvQuestion, tvTimer;
    private Button[] optionButtons;
    private View layoutQuestion, layoutShoot, layoutDefend, coinTossView, gameOverView, gameHeader;

    // ----- Colaboradores -----
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private QuestionManager questionManager;
    private CoinTossPhase coinToss;
    private ScoreBoard scoreBoard;
    private GameTimer questionTimer;
    private final PenaltyShootout shootout = new PenaltyShootout();

    // ----- Estado do jogo -----
    private Player user;
    private Player bot;
    private int userCost;
    private Phase phase = Phase.COIN_TOSS;
    private boolean userWonToss;
    private boolean isPlayerTurn = true;
    private boolean isProcessingClick = false;
    private Question currentQuestion;
    private List<String> currentOptions = new ArrayList<>();
    private final Set<GoalZone> greenZones = EnumSet.noneOf(GoalZone.class);

    // =========================================================================
    // Ciclo de vida e inicialização
    // =========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        setContentView(R.layout.activity_game);
        SystemBars.applyPadding(findViewById(R.id.gameContent));

        user = PlayerCatalog.byName(getIntent().getStringExtra(Constants.EXTRA_SELECTED_PLAYER));
        userCost = getIntent().getIntExtra(Constants.EXTRA_PLAYER_COST, 500);
        bot = PlayerCatalog.random();

        initializeViews();
        questionManager = new QuestionManager(this);

        handler.postDelayed(this::performCoinToss, GameRules.START_DELAY_MS);
    }

    private void initializeViews() {
        ((TextView) findViewById(R.id.tvPlayer1)).setText(user.getName());
        ((TextView) findViewById(R.id.tvPlayer2)).setText(bot.getName());
        ((ImageView) findViewById(R.id.ivPlayer1)).setImageResource(user.getImageResource());
        ((ImageView) findViewById(R.id.ivPlayer2)).setImageResource(bot.getImageResource());

        tvQuestion = findViewById(R.id.tvQuestion);
        tvTimer = findViewById(R.id.tvTimer);
        optionButtons = new Button[]{
                findViewById(R.id.btnOption1), findViewById(R.id.btnOption2), findViewById(R.id.btnOption3)
        };
        for (Button b : optionButtons) b.setOnClickListener(this::onOptionClick);

        gameHeader = findViewById(R.id.gameHeader);
        layoutQuestion = findViewById(R.id.layoutQuestion);
        layoutShoot = findViewById(R.id.layoutShoot);
        layoutDefend = findViewById(R.id.layoutDefend);
        coinTossView = findViewById(R.id.coinTossPhase);
        gameOverView = findViewById(R.id.gameOverPhase);

        scoreBoard = new ScoreBoard(
                findImageViews(R.id.score_circle_1_1, R.id.score_circle_1_2, R.id.score_circle_1_3,
                        R.id.score_circle_1_4, R.id.score_circle_1_5),
                findImageViews(R.id.score_circle_2_1, R.id.score_circle_2_2, R.id.score_circle_2_3,
                        R.id.score_circle_2_4, R.id.score_circle_2_5));
        scoreBoard.reset();

        for (GoalZone zone : GoalZone.values()) {
            findViewById(zone.viewId).setOnClickListener(v -> onGoalZoneClick(zone));
        }
        for (DefenseZone zone : DefenseZone.values()) {
            findViewById(zone.viewId).setOnClickListener(v -> onDefenseZoneClick(zone));
        }

        questionTimer = new GameTimer(GameRules.QUESTION_SECONDS, new GameTimer.Listener() {
            @Override public void onTick(int secondsRemaining) { showTimer(secondsRemaining); }
            @Override public void onFinish() { showTimer(0); handleTimeUp(); }
        });

        coinToss = new CoinTossPhase(this, coinTossView, this::startGameAfterCoinToss);
        hideAllPhases();
    }

    private ImageView[] findImageViews(int... ids) {
        ImageView[] views = new ImageView[ids.length];
        for (int i = 0; i < ids.length; i++) views[i] = findViewById(ids[i]);
        return views;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        questionTimer.cancel();
        coinToss.release();
        questionManager.close();
    }

    private void later(long delayMs, Runnable action) {
        handler.postDelayed(action, delayMs);
    }

    // =========================================================================
    // Cara ou coroa
    // =========================================================================

    private void performCoinToss() {
        phase = Phase.COIN_TOSS;
        hideAllPhases();
        gameHeader.setVisibility(View.GONE);
        coinToss.show();
    }

    private void startGameAfterCoinToss(boolean userWon) {
        userWonToss = userWon;
        isPlayerTurn = userWon;
        coinToss.hide();

        Toast.makeText(this, userWon ? R.string.game_user_starts : R.string.game_bot_starts,
                Toast.LENGTH_SHORT).show();
        startNewRound();
    }

    // =========================================================================
    // Fases visíveis
    // =========================================================================

    private void hideAllPhases() {
        for (View v : new View[]{layoutQuestion, layoutShoot, layoutDefend, coinTossView, gameOverView}) {
            v.setVisibility(View.GONE);
        }
    }

    private void showOnly(View phaseView) {
        hideAllPhases();
        phaseView.setVisibility(View.VISIBLE);
        gameHeader.setVisibility(View.VISIBLE);
        tvTimer.setVisibility(phaseView == layoutQuestion ? View.VISIBLE : View.GONE);
    }

    // =========================================================================
    // Ronda de perguntas
    // =========================================================================

    private void startNewRound() {
        if (checkGameEnd()) return;

        phase = Phase.QUESTION;
        showOnly(layoutQuestion);
        greenZones.clear();

        currentQuestion = questionManager.getRandomQuestion();
        currentOptions = questionManager.getShuffledOptions(currentQuestion);

        tvQuestion.setText(currentQuestion.getQuestion());
        tvQuestion.setBackgroundResource(R.drawable.ds_banner_neutral);
        for (int i = 0; i < optionButtons.length; i++) {
            optionButtons[i].setText(currentOptions.get(i));
        }

        if (isPlayerTurn) {
            setOptionsEnabled(true);
            startQuestionTimer();
        } else {
            setOptionsEnabled(false);
            later(GameRules.BOT_THINK_DELAY_MS, this::startBotTurn);
        }

        resetButtonColors();
        resetAllCircles();
        resetAllRectangles();
    }

    private void startQuestionTimer() {
        showTimer(GameRules.QUESTION_SECONDS);
        questionTimer.start();
    }

    private void showTimer(int secondsRemaining) {
        tvTimer.setText(getString(R.string.game_timer_format, secondsRemaining));
        int background, textColor;
        if (secondsRemaining <= GameRules.TIMER_DANGER_SECONDS) {
            background = R.drawable.ds_timer_danger;
            textColor = R.color.text_primary;
        } else if (secondsRemaining <= GameRules.TIMER_WARNING_SECONDS) {
            background = R.drawable.ds_timer_warning;
            textColor = R.color.text_on_gold;
        } else {
            background = R.drawable.ds_timer_ok;
            textColor = R.color.text_on_gold;
        }
        tvTimer.setBackgroundResource(background);
        int color = ContextCompat.getColor(this, textColor);
        tvTimer.setTextColor(color);
        androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(tvTimer, android.content.res.ColorStateList.valueOf(color));
        // Nos últimos segundos o relógio "bate" a cada tick
        if (secondsRemaining <= GameRules.TIMER_DANGER_SECONDS && secondsRemaining > 0 && phase == Phase.QUESTION) {
            Animations.pulse(tvTimer);
        }
    }

    private void resetTimer() {
        questionTimer.cancel();
        showTimer(GameRules.QUESTION_SECONDS);
    }

    private void handleTimeUp() {
        if (phase != Phase.QUESTION || !isPlayerTurn) return;

        showMessage(getString(R.string.game_time_up_banner), R.drawable.ds_banner_warning);
        setOptionsEnabled(false);
        highlightCorrectAnswer();
        Toast.makeText(this, R.string.game_time_up_toast, Toast.LENGTH_LONG).show();

        later(GameRules.SHOT_RESULT_MS, () -> {
            registerPlayerShot(false);
            if (checkGameEnd()) return;
            showTransition(R.string.transition_user_missed, R.string.emoji_cross, this::startBotTurn);
        });
    }

    public void onOptionClick(View view) {
        if (phase != Phase.QUESTION || !isPlayerTurn || isProcessingClick) return;

        isProcessingClick = true;
        questionTimer.cancel();

        Button clicked = (Button) view;
        boolean correct = currentQuestion.isCorrectOption(clicked.getText().toString());
        setOptionsEnabled(false);

        if (correct) {
            setAnswerState(clicked, AnswerState.CORRECT);
            Animations.pulse(clicked);
            showMessage(getString(R.string.game_answer_correct), R.drawable.ds_banner_success);
            later(GameRules.ANSWER_FEEDBACK_MS, () -> {
                startShootPhase();
                isProcessingClick = false;
            });
        } else {
            setAnswerState(clicked, AnswerState.WRONG);
            Animations.shake(clicked);
            highlightCorrectAnswer();
            showMessage(getString(R.string.game_answer_wrong), R.drawable.ds_banner_error);
            later(GameRules.ANSWER_FEEDBACK_MS, () -> {
                registerPlayerShot(false);
                isProcessingClick = false;
                if (checkGameEnd()) return;
                showTransition(R.string.transition_user_missed, R.string.emoji_cross, this::startBotTurn);
            });
        }
    }

    private enum AnswerState { NEUTRAL, DISABLED, CORRECT, WRONG, REVEAL }

    private void setAnswerState(Button button, AnswerState state) {
        int background;
        switch (state) {
            case DISABLED: background = R.drawable.ds_answer_disabled; break;
            case CORRECT: background = R.drawable.ds_answer_correct; break;
            case WRONG: background = R.drawable.ds_answer_wrong; break;
            case REVEAL: background = R.drawable.ds_answer_reveal; break;
            default: background = R.drawable.ds_answer_neutral;
        }
        button.setBackgroundResource(background);
    }

    private void setOptionsEnabled(boolean enabled) {
        for (Button b : optionButtons) {
            b.setEnabled(enabled);
            setAnswerState(b, enabled ? AnswerState.NEUTRAL : AnswerState.DISABLED);
        }
    }

    private void resetButtonColors() {
        for (Button b : optionButtons) setAnswerState(b, AnswerState.NEUTRAL);
    }

    private void highlightCorrectAnswer() {
        for (int i = 0; i < optionButtons.length; i++) {
            if (currentQuestion.isCorrectOption(currentOptions.get(i))) {
                setAnswerState(optionButtons[i], AnswerState.REVEAL);
                return;
            }
        }
    }

    // =========================================================================
    // Vez do computador
    // =========================================================================

    private void startBotTurn() {
        if (checkGameEnd()) return;

        isPlayerTurn = false;
        boolean botAnsweredCorrectly = random.nextDouble() < GameRules.BOT_CORRECT_PROBABILITY;

        if (botAnsweredCorrectly) {
            showMessage(getString(R.string.game_bot_answered_right, bot.getName()), R.drawable.ds_banner_warning);
            later(GameRules.ANSWER_FEEDBACK_MS, this::startDefendPhase);
        } else {
            showMessage(getString(R.string.game_bot_answered_wrong, bot.getName()), R.drawable.ds_banner_info);
            later(GameRules.ANSWER_FEEDBACK_MS, () -> {
                registerBotShot(false);
                if (checkGameEnd()) return;
                showTransition(R.string.transition_bot_missed, R.string.emoji_party, this::startPlayerTurn);
            });
        }
    }

    private void startPlayerTurn() {
        isPlayerTurn = true;
        startNewRound();
    }

    /** Mostra um ecrã de transição por instantes e depois executa {@code next}. */
    private void showTransition(@StringRes int message, @StringRes int icon, Runnable next) {
        View transition = getLayoutInflater().inflate(R.layout.layout_phase_transition, null);
        ((TextView) transition.findViewById(R.id.tvTransitionMessage)).setText(message);
        ((TextView) transition.findViewById(R.id.tvTransitionIcon)).setText(icon);

        ViewGroup content = findViewById(android.R.id.content);
        content.addView(transition);
        hideAllPhases();

        later(GameRules.TRANSITION_MS, () -> {
            content.removeView(transition);
            next.run();
        });
    }

    // =========================================================================
    // Remate do jogador
    // =========================================================================

    private void startShootPhase() {
        phase = Phase.SHOOT;
        showOnly(layoutShoot);
        resetAllCircles();
        resetTimer();

        List<GoalZone> zones = new ArrayList<>(EnumSet.allOf(GoalZone.class));
        Collections.shuffle(zones);
        greenZones.clear();
        greenZones.addAll(zones.subList(0, GameRules.greenZonesForCost(userCost)));
        isProcessingClick = false;
    }

    private void onGoalZoneClick(GoalZone zone) {
        if (isProcessingClick) return;
        isProcessingClick = true;

        if (phase != Phase.SHOOT) {
            isProcessingClick = false;
            return;
        }
        phase = Phase.RESULT;

        boolean scored = greenZones.contains(zone);
        resetAllCircles();
        ((ImageView) findViewById(zone.viewId)).setImageResource(
                scored ? R.drawable.goal_circle_chosen_green : R.drawable.goal_circle_chosen_red);

        later(GameRules.SHOT_RESULT_MS, () -> {
            registerPlayerShot(scored);
            if (scored) {
                showMessage(getString(R.string.game_shot_goal, user.getName()), R.drawable.ds_banner_success);
            } else {
                showMessage(getString(R.string.game_shot_miss, user.getName()), R.drawable.ds_banner_error);
            }
            isProcessingClick = false;
            if (checkGameEnd()) return;
            showTransition(scored ? R.string.transition_user_scored : R.string.transition_user_missed,
                    scored ? R.string.emoji_ball : R.string.emoji_cross, this::startBotTurn);
        });
    }

    // =========================================================================
    // Defesa do jogador
    // =========================================================================

    private void startDefendPhase() {
        phase = Phase.DEFEND;
        showOnly(layoutDefend);
        resetAllRectangles();
        resetTimer();
        isProcessingClick = false;
    }

    private void onDefenseZoneClick(DefenseZone chosen) {
        if (isProcessingClick) return;
        isProcessingClick = true;

        if (phase != Phase.DEFEND) {
            isProcessingClick = false;
            return;
        }
        phase = Phase.RESULT;

        DefenseZone botShot = DefenseZone.random();
        boolean defended = chosen == botShot;
        showDefenseResult(chosen, botShot, defended);

        later(GameRules.SHOT_RESULT_MS, () -> {
            registerBotShot(!defended);
            if (defended) {
                showMessage(getString(R.string.game_defense_saved, bot.getName()), R.drawable.ds_banner_info);
            } else {
                showMessage(getString(R.string.game_defense_conceded, bot.getName()), R.drawable.ds_banner_error);
            }
            isProcessingClick = false;
            if (checkGameEnd()) return;
            showTransition(defended ? R.string.transition_bot_missed : R.string.transition_bot_scored,
                    defended ? R.string.emoji_party : R.string.emoji_ball, this::startPlayerTurn);
        });
    }

    private void showDefenseResult(DefenseZone chosen, DefenseZone botShot, boolean defended) {
        resetAllRectangles();
        findViewById(botShot.viewId).setBackgroundResource(R.drawable.defense_red);
        if (defended) findViewById(chosen.viewId).setBackgroundResource(R.drawable.defense_green);

        String text = getString(R.string.game_defense_summary,
                bot.getName(), getString(botShot.nameRes), getString(chosen.nameRes),
                getString(defended ? R.string.game_defense_right : R.string.game_defense_wrong));
        showMessage(text, defended ? R.drawable.ds_banner_info : R.drawable.ds_banner_error);
    }

    // =========================================================================
    // Pontuação e fim de jogo
    // =========================================================================

    private void registerPlayerShot(boolean scored) {
        shootout.recordPlayerShot(scored);
        scoreBoard.recordPlayer(scored);
    }

    private void registerBotShot(boolean scored) {
        shootout.recordBotShot(scored);
        scoreBoard.recordBot(scored);
    }

    /** @return true se o jogo mudou de estado (acabou ou foi para morte súbita) e a ronda deve parar. */
    private boolean checkGameEnd() {
        switch (shootout.evaluate()) {
            case FINISHED:
                endGame();
                return true;
            case SUDDEN_DEATH:
                startSuddenDeath();
                return true;
            default:
                return false;
        }
    }

    private void startSuddenDeath() {
        shootout.startSuddenDeath();
        scoreBoard.reset();

        showMessage(getString(R.string.game_sudden_death_banner, PenaltyShootout.REGULAR_PENALTIES,
                user.getName(), shootout.getPlayerScore(), shootout.getBotScore(), bot.getName()),
                R.drawable.ds_banner_special);
        Toast.makeText(this, R.string.game_sudden_death_toast, Toast.LENGTH_LONG).show();

        later(GameRules.SUDDEN_DEATH_BANNER_MS, () -> {
            isPlayerTurn = userWonToss;
            startNewRound();
        });
    }

    private void endGame() {
        questionTimer.cancel();
        hideAllPhases();
        gameHeader.setVisibility(View.GONE);
        new GameOverView(gameOverView).show(shootout, user.getName(), bot.getName(), this::finish);
    }

    // =========================================================================
    // Pequenos auxiliares de UI
    // =========================================================================

    private void showMessage(String message, @DrawableRes int bannerRes) {
        tvQuestion.setText(message);
        tvQuestion.setBackgroundResource(bannerRes);
        Animations.popIn(tvQuestion);
    }

    private void resetAllCircles() {
        for (GoalZone zone : GoalZone.values()) {
            ((ImageView) findViewById(zone.viewId)).setImageResource(R.drawable.goal_circle);
        }
    }

    private void resetAllRectangles() {
        for (DefenseZone zone : DefenseZone.values()) {
            findViewById(zone.viewId).setBackgroundResource(R.drawable.defense_normal);
        }
    }
}
