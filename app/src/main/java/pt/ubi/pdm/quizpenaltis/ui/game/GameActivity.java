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
import pt.ubi.pdm.quizpenaltis.ui.game.opponent.BotOpponent;
import pt.ubi.pdm.quizpenaltis.ui.game.opponent.Opponent;
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

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Ecrã do jogo. Coordena as fases (moeda → pergunta → remate/defesa → fim);
 * as regras estão em {@link PenaltyShootout} e os números em {@link GameRules}.
 * O que o adversário faz (perguntas, respostas, remates) vem do {@link Opponent}.
 */
public class GameActivity extends AppCompatActivity {

    private enum Phase { COIN_TOSS, QUESTION, SHOOT, DEFEND, RESULT }

    // ----- Views -----
    private TextView tvQuestion, tvTimer;
    private Button[] optionButtons;
    private View layoutQuestion, layoutShoot, layoutDefend, coinTossView, gameOverView, gameHeader;

    // ----- Colaboradores -----
    private final Handler handler = new Handler(Looper.getMainLooper());
    private QuestionManager questionManager;
    private Opponent opponent;
    private CoinTossPhase coinToss;
    private ScoreBoard scoreBoard;
    private GameTimer questionTimer;
    private final PenaltyShootout shootout = new PenaltyShootout();

    // ----- Estado do jogo -----
    private Player user;
    private int userCost;
    /** Número da ronda atual (uma pergunta); -1 antes da primeira. */
    private int turn = -1;
    private Phase phase = Phase.COIN_TOSS;
    private boolean userWonToss;
    private boolean isPlayerTurn = true;
    private boolean isProcessingClick = false;
    private Question currentQuestion;
    private List<String> currentOptions = new ArrayList<>();

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
        questionManager = new QuestionManager(this);
        opponent = new BotOpponent(questionManager, userCost);

        initializeViews();

        handler.postDelayed(this::performCoinToss, GameRules.START_DELAY_MS);
    }

    private void initializeViews() {
        ((TextView) findViewById(R.id.tvPlayer1)).setText(user.getName());
        ((TextView) findViewById(R.id.tvPlayer2)).setText(opponent.displayName());
        ((ImageView) findViewById(R.id.ivPlayer1)).setImageResource(user.getImageResource());
        ((ImageView) findViewById(R.id.ivPlayer2)).setImageResource(opponent.player().getImageResource());

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
        opponent.close();
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
        // Até a pergunta chegar (no bot é imediato) não se pode responder.
        setOptionsEnabled(false);

        final int round = ++turn;
        opponent.questionFor(round, question -> {
            if (isDestroyed() || round != turn || phase != Phase.QUESTION) return;
            showQuestion(question);
        });
    }

    private void showQuestion(Question question) {
        currentQuestion = question;
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
            later(GameRules.BOT_THINK_DELAY_MS, this::startOpponentTurn);
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

        opponent.onUserAnswered(turn, Opponent.NO_ANSWER);
        showMessage(getString(R.string.game_time_up_banner), R.drawable.ds_banner_warning);
        setOptionsEnabled(false);
        highlightCorrectAnswer();

        later(GameRules.SHOT_RESULT_MS, () -> {
            registerPlayerShot(false);
            if (checkGameEnd()) return;
            showTransition(Transition.USER_MISSED, this::startOpponentTurn);
        });
    }

    public void onOptionClick(View view) {
        if (phase != Phase.QUESTION || !isPlayerTurn || isProcessingClick) return;

        isProcessingClick = true;
        questionTimer.cancel();

        Button clicked = (Button) view;
        String answer = clicked.getText().toString();
        boolean correct = currentQuestion.isCorrectOption(answer);
        opponent.onUserAnswered(turn, originalOptionIndex(answer));
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
                showTransition(Transition.USER_MISSED, this::startOpponentTurn);
            });
        }
    }

    /** Posição da resposta na pergunta original (0 = A, 1 = B, 2 = C); as opções no ecrã estão baralhadas. */
    private int originalOptionIndex(String answer) {
        String[] original = {currentQuestion.getOptionA(), currentQuestion.getOptionB(), currentQuestion.getOptionC()};
        for (int i = 0; i < original.length; i++) {
            if (original[i].equals(answer)) return i;
        }
        return Opponent.NO_ANSWER;
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
    // Vez do adversário
    // =========================================================================

    private void startOpponentTurn() {
        if (checkGameEnd()) return;

        isPlayerTurn = false;
        opponent.opponentAnswers(turn, correct -> {
            if (isDestroyed()) return;
            if (correct) {
                showMessage(getString(R.string.game_bot_answered_right, opponent.displayName()), R.drawable.ds_banner_warning);
                later(GameRules.ANSWER_FEEDBACK_MS, this::startDefendPhase);
            } else {
                showMessage(getString(R.string.game_bot_answered_wrong, opponent.displayName()), R.drawable.ds_banner_info);
                later(GameRules.ANSWER_FEEDBACK_MS, () -> {
                    registerBotShot(false);
                    if (checkGameEnd()) return;
                    showTransition(Transition.BOT_WRONG_ANSWER, this::startPlayerTurn);
                });
            }
        });
    }

    private void startPlayerTurn() {
        isPlayerTurn = true;
        startNewRound();
    }

    /** As transições entre jogadas: título, o que vem a seguir, e a medalha (verde = bom para ti). */
    private enum Transition {
        USER_SCORED(R.string.transition_user_scored, R.string.transition_next_defend,
                R.drawable.icon_sports_soccer, R.drawable.ds_badge_good),
        USER_MISSED(R.string.transition_user_missed, R.string.transition_next_defend,
                R.drawable.icon_close, R.drawable.ds_badge_bad),
        USER_SAVED(R.string.transition_saved, R.string.transition_next_shoot,
                R.drawable.icon_front_hand, R.drawable.ds_badge_good),
        BOT_SCORED(R.string.transition_bot_scored, R.string.transition_next_shoot,
                R.drawable.icon_sports_soccer, R.drawable.ds_badge_bad),
        BOT_WRONG_ANSWER(R.string.transition_bot_missed, R.string.transition_next_shoot,
                R.drawable.icon_check, R.drawable.ds_badge_good);

        @StringRes final int headline;
        @StringRes final int next;
        @DrawableRes final int icon;
        @DrawableRes final int badge;

        Transition(int headline, int next, int icon, int badge) {
            this.headline = headline;
            this.next = next;
            this.icon = icon;
            this.badge = badge;
        }
    }

    /** Mostra uma transição por instantes (o cabeçalho com a pontuação fica visível) e depois executa {@code next}. */
    private void showTransition(Transition transition, Runnable next) {
        showOverlay(getString(transition.headline), getString(transition.next),
                transition.icon, transition.badge, GameRules.TRANSITION_MS, next);
    }

    private void showOverlay(String headline, String subtitle, @DrawableRes int icon,
                             @DrawableRes int badge, long durationMs, Runnable next) {
        View overlay = getLayoutInflater().inflate(R.layout.layout_phase_transition, null);
        ((TextView) overlay.findViewById(R.id.tvTransitionMessage)).setText(headline);
        ((TextView) overlay.findViewById(R.id.tvTransitionSubtitle)).setText(subtitle);
        ((ImageView) overlay.findViewById(R.id.ivTransitionIcon)).setImageResource(icon);
        View badgeView = overlay.findViewById(R.id.transitionBadge);
        badgeView.setBackgroundResource(badge);

        ViewGroup container = findViewById(R.id.mainContainer);
        container.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        hideAllPhases();
        tvTimer.setVisibility(View.GONE);

        Animations.pop(badgeView);
        Animations.popIn(overlay.findViewById(R.id.tvTransitionMessage));

        later(durationMs, () -> {
            container.removeView(overlay);
            next.run();
        });
    }

    // =========================================================================
    // Remate do jogador
    // =========================================================================

    private void startShootPhase() {
        phase = Phase.SHOOT;
        showOnly(layoutShoot);
        resetShootBanner();
        resetAllCircles();
        resetTimer();
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

        opponent.userShoots(turn, zone, scored -> {
            if (isDestroyed()) return;
            showShotResult(zone, scored);
        });
    }

    private void showShotResult(GoalZone zone, boolean scored) {
        resetAllCircles();
        View target = findViewById(zone.viewId);

        // A bola voa até à zona; só então se vê o resultado.
        Animations.ballFlight(findViewById(R.id.ballIndicator), findViewById(R.id.goalArea),
                target, GameRules.BALL_FLIGHT_MS, () -> {
                    if (isDestroyed()) return;
                    target.setBackgroundResource(scored ? R.drawable.ds_zone_goal : R.drawable.ds_zone_miss);
                    Animations.popIn(target);
                    showResultBanner(findViewById(R.id.cardInstruction), findViewById(R.id.tvShootBanner),
                            scored ? R.string.banner_goal : R.string.banner_shot_miss,
                            scored ? R.drawable.ds_banner_success : R.drawable.ds_banner_error);
                    if (!scored) Animations.shake(target);
                    registerPlayerShot(scored);

                    later(GameRules.SHOT_RESULT_MS, () -> {
                        isProcessingClick = false;
                        if (checkGameEnd()) return;
                        showTransition(scored ? Transition.USER_SCORED : Transition.USER_MISSED,
                                this::startOpponentTurn);
                    });
                });
    }

    // =========================================================================
    // Defesa do jogador
    // =========================================================================

    private void startDefendPhase() {
        phase = Phase.DEFEND;
        showOnly(layoutDefend);
        resetDefendBanner();
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

        opponent.userDefends(turn, chosen, outcome -> {
            if (isDestroyed()) return;
            showDefense(chosen, outcome.opponentShot, outcome.defended);
        });
    }

    private void showDefense(DefenseZone chosen, DefenseZone botShot, boolean defended) {
        // A tua escolha fica assinalada a dourado e o remate do adversário voa até à zona dele.
        resetAllRectangles();
        findViewById(chosen.viewId).setBackgroundResource(R.drawable.ds_zone_defend_chosen);

        Animations.ballFlight(findViewById(R.id.defendBall), findViewById(R.id.defendGoalArea),
                findViewById(botShot.viewId), GameRules.BALL_FLIGHT_MS, () -> {
                    if (isDestroyed()) return;
                    showDefenseResult(chosen, botShot, defended);
                    registerBotShot(!defended);

                    later(GameRules.SHOT_RESULT_MS, () -> {
                        isProcessingClick = false;
                        if (checkGameEnd()) return;
                        showTransition(defended ? Transition.USER_SAVED : Transition.BOT_SCORED,
                                this::startPlayerTurn);
                    });
                });
    }

    private void showDefenseResult(DefenseZone chosen, DefenseZone botShot, boolean defended) {
        View shotZone = findViewById(botShot.viewId);
        shotZone.setBackgroundResource(defended
                ? R.drawable.ds_zone_defend_saved : R.drawable.ds_zone_defend_conceded);
        Animations.popIn(shotZone);

        showResultBanner(findViewById(R.id.cardDefendInstruction), findViewById(R.id.tvDefendBanner),
                defended ? R.string.banner_saved : R.string.banner_conceded,
                defended ? R.drawable.ds_banner_success : R.drawable.ds_banner_error);

        TextView detail = findViewById(R.id.tvDefendDetail);
        detail.setText(getString(R.string.game_defense_summary,
                opponent.displayName(), getString(botShot.nameRes), getString(chosen.nameRes)));
        detail.setVisibility(View.VISIBLE);
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

        showOverlay(getString(R.string.sudden_death_title),
                getString(R.string.sudden_death_subtitle, PenaltyShootout.REGULAR_PENALTIES,
                        shootout.getPlayerScore(), shootout.getBotScore()),
                R.drawable.icon_bolt, R.drawable.ds_badge_special, GameRules.SUDDEN_DEATH_BANNER_MS, () -> {
                    isPlayerTurn = userWonToss;
                    startNewRound();
                });
    }

    private void endGame() {
        questionTimer.cancel();
        hideAllPhases();
        gameHeader.setVisibility(View.GONE);
        new GameOverView(gameOverView).show(shootout, user.getName(), opponent.displayName(), this::finish);
    }

    // =========================================================================
    // Pequenos auxiliares de UI
    // =========================================================================

    private void showMessage(String message, @DrawableRes int bannerRes) {
        tvQuestion.setText(message);
        tvQuestion.setBackgroundResource(bannerRes);
        Animations.popIn(tvQuestion);
    }

    /** Troca a instrução pelo banner de resultado (GOLO!, DEFENDIDO!, ...), que "salta" para dentro. */
    private void showResultBanner(View instruction, TextView banner, @StringRes int text, @DrawableRes int background) {
        instruction.setVisibility(View.INVISIBLE);
        banner.setText(text);
        banner.setBackgroundResource(background);
        banner.setVisibility(View.VISIBLE);
        Animations.popIn(banner);
    }

    /** Volta a mostrar a instrução e esconde o banner e a bola. */
    private void resetShootBanner() {
        findViewById(R.id.cardInstruction).setVisibility(View.VISIBLE);
        findViewById(R.id.tvShootBanner).setVisibility(View.INVISIBLE);
        findViewById(R.id.ballIndicator).setVisibility(View.INVISIBLE);
    }

    private void resetDefendBanner() {
        findViewById(R.id.cardDefendInstruction).setVisibility(View.VISIBLE);
        findViewById(R.id.tvDefendBanner).setVisibility(View.INVISIBLE);
        findViewById(R.id.tvDefendDetail).setVisibility(View.INVISIBLE);
        findViewById(R.id.defendBall).setVisibility(View.INVISIBLE);
    }

    private void resetAllCircles() {
        for (GoalZone zone : GoalZone.values()) {
            findViewById(zone.viewId).setBackgroundResource(R.drawable.ds_zone_idle);
        }
    }

    private void resetAllRectangles() {
        for (DefenseZone zone : DefenseZone.values()) {
            findViewById(zone.viewId).setBackgroundResource(R.drawable.ds_zone_defend_idle);
        }
    }
}
