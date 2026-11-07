package pt.ubi.pdm.quizpenaltis;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";

    private TextView tvQuestion, tvTimer;
    private Button btnOption1, btnOption2, btnOption3;
    private LinearLayout layoutQuestion;
    private ConstraintLayout layoutShoot;
    private ConstraintLayout layoutDefend;

    // Sistema de score
    private ImageView ivPlayer1, ivPlayer2;
    private ImageView[] playerScoreCircles = new ImageView[5];
    private ImageView[] botScoreCircles = new ImageView[5];
    private int currentPlayerScoreIndex = 0;
    private int currentBotScoreIndex = 0;

    private QuestionManager questionManager;
    private Question currentQuestion;
    private List<String> currentOptions;

    // Variáveis do jogo
    private int playerScore = 0;
    private int botScore = 0;
    private int currentRound = 1;
    private int totalRounds = 5;
    private String selectedPlayer;
    private String botPlayer;
    private int playerPoints;

    // Timer
    private CountDownTimer questionTimer;
    private static final long QUESTION_TIME = 10000;
    private static final long TIMER_INTERVAL = 1000;
    private boolean isTimerRunning = false;

    // Estados do jogo
    private enum GamePhase {COIN_TOSS, QUESTION, SHOOT, DEFEND, RESULT}
    private GamePhase currentPhase = GamePhase.COIN_TOSS;
    private boolean isPlayerTurn = true;
    private boolean userWonToss = false;

    private ProgressManager progressManager;

    // Lógica real de penáltis
    private int playerShots = 0;
    private int botShots = 0;
    private boolean isSuddenDeath = false;
    private static final int REGULAR_PENALTIES = 5;

    // Coin Toss
    private CoinTossPhase coinTossManager;

    // Arrays de IDs
    private final int[] circleIds = {
            R.id.circle_1_1, R.id.circle_1_2, R.id.circle_1_3, R.id.circle_1_4, R.id.circle_1_5,
            R.id.circle_2_1, R.id.circle_2_2, R.id.circle_2_3, R.id.circle_2_4, R.id.circle_2_5,
            R.id.circle_3_1, R.id.circle_3_2, R.id.circle_3_3, R.id.circle_3_4, R.id.circle_3_5
    };

    private final int[] rectangleIds = {
            R.id.rect_top_left, R.id.rect_top_right, R.id.rect_center,
            R.id.rect_bottom_left, R.id.rect_bottom_right
    };

    private final String[] allAreas = {
            "top_left", "top_left_center", "top_center", "top_right_center", "top_right",
            "middle_left", "middle_center_left", "middle_center", "middle_center_right", "middle_right",
            "bottom_left", "bottom_left_center", "bottom_center", "bottom_right_center", "bottom_right"
    };

    private ArrayList<String> currentGreenAreas = new ArrayList<>();
    private boolean isProcessingClick = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_game);

        selectedPlayer = getIntent().getStringExtra("SELECTED_PLAYER");
        playerPoints = getIntent().getIntExtra("PLAYER_COST", 500);
        botPlayer = getRandomBotPlayer();

        initializeViews();
        setupQuestionManager();
        progressManager = new ProgressManager(this);

        new Handler().postDelayed(() -> {
            performCoinToss();
        }, 500);
    }

    private void initializeViews() {
        TextView tvPlayer1 = findViewById(R.id.tvPlayer1);
        TextView tvPlayer2 = findViewById(R.id.tvPlayer2);

        // Quiz views
        tvQuestion = findViewById(R.id.tvQuestion);
        btnOption1 = findViewById(R.id.btnOption1);
        btnOption2 = findViewById(R.id.btnOption2);
        btnOption3 = findViewById(R.id.btnOption3);
        layoutQuestion = findViewById(R.id.layoutQuestion);

        // Timer view do header
        tvTimer = findViewById(R.id.tvTimer);

        // Shoot/Defend views
        layoutShoot = findViewById(R.id.layoutShoot);
        layoutDefend = findViewById(R.id.layoutDefend);

        // Sistema de score
        ivPlayer1 = findViewById(R.id.ivPlayer1);
        ivPlayer2 = findViewById(R.id.ivPlayer2);

        // Sistema de score - Círculos
        playerScoreCircles[0] = findViewById(R.id.score_circle_1_1);
        playerScoreCircles[1] = findViewById(R.id.score_circle_1_2);
        playerScoreCircles[2] = findViewById(R.id.score_circle_1_3);
        playerScoreCircles[3] = findViewById(R.id.score_circle_1_4);
        playerScoreCircles[4] = findViewById(R.id.score_circle_1_5);

        botScoreCircles[0] = findViewById(R.id.score_circle_2_1);
        botScoreCircles[1] = findViewById(R.id.score_circle_2_2);
        botScoreCircles[2] = findViewById(R.id.score_circle_2_3);
        botScoreCircles[3] = findViewById(R.id.score_circle_2_4);
        botScoreCircles[4] = findViewById(R.id.score_circle_2_5);

        // Configurar textos
        tvPlayer1.setText(selectedPlayer);
        tvPlayer2.setText(botPlayer);

        // Configurar fotos dos jogadores
        setPlayerImages();
        resetScoreCircles();

        // Configurar listeners
        setupGoalCircles();
        setupDefenseRectangles();

        // Configurar onClick listeners
        btnOption1.setOnClickListener(this::onOptionClick);
        btnOption2.setOnClickListener(this::onOptionClick);
        btnOption3.setOnClickListener(this::onOptionClick);

        // Inicializar Coin Toss Manager
        coinTossManager = new CoinTossPhase(this, new CoinTossPhase.CoinTossListener() {
            @Override
            public void onCoinTossResult(boolean userWonToss) {
                startGameAfterCoinToss(userWonToss);
            }
        });

        hideAllPhases();
    }

    // =========================================================================
    // SISTEMA DE COIN TOSS
    // =========================================================================

    private void performCoinToss() {
        currentPhase = GamePhase.COIN_TOSS;
        hideAllPhases();

        runOnUiThread(() -> {
            View coinTossLayout = findViewById(R.id.coinTossPhase);
            if (coinTossLayout != null) {
                coinTossLayout.setVisibility(View.VISIBLE);
            }
        });

        if (coinTossManager != null) {
            coinTossManager.show();
        } else {
            startGameAfterCoinToss(true);
        }
    }

    private void startGameAfterCoinToss(boolean userWonToss) {
        this.userWonToss = userWonToss;
        currentPhase = GamePhase.QUESTION;
        isPlayerTurn = userWonToss;

        // Reset das variáveis do jogo
        playerScore = 0;
        botScore = 0;
        currentRound = 1;
        isSuddenDeath = false;
        playerShots = 0;
        botShots = 0;
        totalRounds = REGULAR_PENALTIES;

        if (coinTossManager != null) {
            coinTossManager.hide();
        }

        String starterMessage = userWonToss ?
                "Você começa primeiro!" : "Computador começa primeiro!";
        Toast.makeText(this, starterMessage, Toast.LENGTH_SHORT).show();

        startNewRound();
    }

    private void hideAllPhases() {
        runOnUiThread(() -> {
            if (layoutQuestion != null) layoutQuestion.setVisibility(View.GONE);
            if (layoutShoot != null) layoutShoot.setVisibility(View.GONE);
            if (layoutDefend != null) layoutDefend.setVisibility(View.GONE);

            View coinTossPhase = findViewById(R.id.coinTossPhase);
            if (coinTossPhase != null) coinTossPhase.setVisibility(View.GONE);

            View gameOverPhase = findViewById(R.id.gameOverPhase);
            if (gameOverPhase != null) gameOverPhase.setVisibility(View.GONE);
        });
    }

    private String getRandomBotPlayer() {
        String[] botPlayers = {
                "Maguire", "Ricardo Esgaio", "Taremi", "João Félix", "João Moutinho",
                "Lamine Yamal", "Nuno Mendes", "Kylian Mbappe", "Messi", "CR7"
        };
        return botPlayers[(int) (Math.random() * botPlayers.length)];
    }

    private void setupQuestionManager() {
        questionManager = new QuestionManager(this);
    }

    // =========================================================================
    // SISTEMA DE TIMER VISUAL
    // =========================================================================

    private void startQuestionTimer() {
        stopTimer();

        // Reset visual do timer
        runOnUiThread(() -> {
            if (tvTimer != null) {
                tvTimer.setText("⏱️ 10s");
                tvTimer.setBackgroundColor(Color.parseColor("#00FF00")); // Verde
            }
        });

        questionTimer = new CountDownTimer(QUESTION_TIME, TIMER_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                updateTimerDisplay(secondsRemaining);
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                runOnUiThread(() -> {
                    if (tvTimer != null) {
                        tvTimer.setText("⏱️ 0s");
                        tvTimer.setBackgroundColor(Color.parseColor("#FF0000")); // Vermelho
                    }
                });
                handleTimeUp();
            }
        };

        isTimerRunning = true;
        questionTimer.start();
    }

    private void updateTimerDisplay(int secondsRemaining) {
        runOnUiThread(() -> {
            if (tvTimer != null) {
                tvTimer.setText("⏱️ " + secondsRemaining + "s");

                // Mudar cor conforme o tempo diminui
                if (secondsRemaining <= 3) {
                    tvTimer.setBackgroundColor(Color.parseColor("#FF0000")); // Vermelho
                } else if (secondsRemaining <= 7) {
                    tvTimer.setBackgroundColor(Color.parseColor("#FFA500")); // Laranja
                } else {
                    tvTimer.setBackgroundColor(Color.parseColor("#00FF00")); // Verde
                }
            }
        });
    }

    private void handleTimeUp() {
        if (currentPhase == GamePhase.QUESTION && isPlayerTurn) {
            runOnUiThread(() -> {
                showQuestionResult("⏰ TEMPO ESGOTADO!", "#FF9800");
                setOptionsEnabled(false);
                highlightCorrectAnswer();

                Toast.makeText(GameActivity.this, "Tempo esgotado!", Toast.LENGTH_LONG).show();

                new Handler().postDelayed(() -> {
                    updatePlayerScoreCircle(false);
                    playerShots++;

                    if (checkGameEnd()) return;
                    showBotTurnTransition(false);
                }, 2000);
            });
        }
    }

    private void stopTimer() {
        if (questionTimer != null) {
            questionTimer.cancel();
            questionTimer = null;
            isTimerRunning = false;
        }
    }

    private void resetTimer() {
        stopTimer();
        runOnUiThread(() -> {
            if (tvTimer != null) {
                tvTimer.setText("⏱️ 10s");
                tvTimer.setBackgroundColor(Color.parseColor("#00FF00")); // Verde
            }
        });
    }

    // =========================================================================
    // SISTEMA DE TRANSIÇÕES
    // =========================================================================

    private void showBotTurnTransition(boolean userScored) {
        runOnUiThread(() -> {
            View transitionView = getLayoutInflater().inflate(R.layout.layout_phase_transition, null);
            TextView tvMessage = transitionView.findViewById(R.id.tvTransitionMessage);
            TextView tvIcon = transitionView.findViewById(R.id.tvTransitionIcon);

            if (userScored) {
                tvMessage.setText("GOLAÇO!\nVAI DEFENDER");
                tvIcon.setText("⚽");
            } else {
                tvMessage.setText("FALHANÇO!\nVAI DEFENDER");
                tvIcon.setText("❌");
            }

            ViewGroup rootView = findViewById(android.R.id.content);
            rootView.addView(transitionView);
            hideAllPhases();

            new Handler().postDelayed(() -> {
                rootView.removeView(transitionView);
                startBotTurn();
            }, 2000);
        });
    }

    private void showPlayerTurnTransition(boolean botScored) {
        runOnUiThread(() -> {
            View transitionView = getLayoutInflater().inflate(R.layout.layout_phase_transition, null);
            TextView tvMessage = transitionView.findViewById(R.id.tvTransitionMessage);
            TextView tvIcon = transitionView.findViewById(R.id.tvTransitionIcon);

            if (botScored) {
                tvMessage.setText("ADVERSÁRIO MARCOU!\nSUA VEZ");
                tvIcon.setText("⚽");
            } else {
                tvMessage.setText("DEFENDEU!\nSUA VEZ");
                tvIcon.setText("🎉");
            }

            ViewGroup rootView = findViewById(android.R.id.content);
            rootView.addView(transitionView);
            hideAllPhases();

            new Handler().postDelayed(() -> {
                rootView.removeView(transitionView);
                startPlayerTurn();
            }, 2000);
        });
    }

    // =========================================================================
    // SISTEMA DE SCORE
    // =========================================================================

    private void setPlayerImages() {
        int playerImageRes = getPlayerImageResource(selectedPlayer);
        int botImageRes = getPlayerImageResource(botPlayer);
        ivPlayer1.setImageResource(playerImageRes);
        ivPlayer2.setImageResource(botImageRes);
    }

    private int getPlayerImageResource(String playerName) {
        switch (playerName) {
            case "CR7": return R.drawable.cr7;
            case "Messi": return R.drawable.messi;
            case "Kylian Mbappe": return R.drawable.mbappe;
            case "Nuno Mendes": return R.drawable.nuno_mendes;
            case "Lamine Yamal": return R.drawable.lamine_yamal;
            case "João Moutinho": return R.drawable.joao_moutinho;
            case "João Félix": return R.drawable.joao_felix;
            case "Taremi": return R.drawable.taremi;
            case "Ricardo Esgaio": return R.drawable.ricardo_esgaio;
            case "Maguire": return R.drawable.maguire;
            default: return R.drawable.cr7;
        }
    }

    private void resetScoreCircles() {
        runOnUiThread(() -> {
            for (ImageView circle : playerScoreCircles) {
                if (circle != null) circle.setImageResource(R.drawable.score_circle_gray);
            }
            for (ImageView circle : botScoreCircles) {
                if (circle != null) circle.setImageResource(R.drawable.score_circle_gray);
            }
            currentPlayerScoreIndex = 0;
            currentBotScoreIndex = 0;
        });
    }

    private void updatePlayerScoreCircle(boolean scored) {
        runOnUiThread(() -> {
            if (currentPlayerScoreIndex < playerScoreCircles.length) {
                ImageView circle = playerScoreCircles[currentPlayerScoreIndex];
                if (circle != null) {
                    int drawableRes = scored ? R.drawable.score_circle_green : R.drawable.score_circle_red;
                    circle.setImageResource(drawableRes);
                    currentPlayerScoreIndex++;
                }
            }
        });
    }

    private void updateBotScoreCircle(boolean scored) {
        runOnUiThread(() -> {
            if (currentBotScoreIndex < botScoreCircles.length) {
                ImageView circle = botScoreCircles[currentBotScoreIndex];
                if (circle != null) {
                    int drawableRes = scored ? R.drawable.score_circle_green : R.drawable.score_circle_red;
                    circle.setImageResource(drawableRes);
                    currentBotScoreIndex++;
                }
            }
        });
    }

    // =========================================================================
    // CONTROLE DE LAYOUTS
    // =========================================================================

    private void showQuizPhase() {
        runOnUiThread(() -> {
            hideAllPhases();
            layoutQuestion.setVisibility(View.VISIBLE);
            tvQuestion.setBackgroundColor(Color.WHITE);
        });
    }

    private void showShootPhase() {
        runOnUiThread(() -> {
            hideAllPhases();
            layoutShoot.setVisibility(View.VISIBLE);
            resetAllCircles();
            resetTimer();

            int greenCircles = calculateExactGreenCircles();
            generateGreenAreas(greenCircles);
            isProcessingClick = false;
        });
    }

    private void showDefendPhase() {
        runOnUiThread(() -> {
            hideAllPhases();
            layoutDefend.setVisibility(View.VISIBLE);
            resetAllRectangles();
            resetTimer();
            isProcessingClick = false;
        });
    }

    // =========================================================================
    // LÓGICA DO JOGO
    // =========================================================================

    private void startNewRound() {
        if (checkGameEnd()) return;

        currentPhase = GamePhase.QUESTION;
        showQuizPhase();
        currentGreenAreas.clear();

        currentQuestion = questionManager.getRandomQuestion();
        if (currentQuestion == null) {
            currentQuestion = getFallbackQuestion();
            Log.e("GameActivity", "QuestionManager retornou null, usando fallback question");
        }

        currentOptions = questionManager.getShuffledOptions(currentQuestion);

        runOnUiThread(() -> {
            if (currentQuestion == null) currentQuestion = getFallbackQuestion();

            tvQuestion.setText(currentQuestion.getQuestion());
            tvQuestion.setBackgroundColor(Color.WHITE);

            if (currentOptions == null || currentOptions.size() < 3) {
                currentOptions = new ArrayList<>();
                currentOptions.add(currentQuestion.getOptionA());
                currentOptions.add(currentQuestion.getOptionB());
                currentOptions.add(currentQuestion.getOptionC());
                Collections.shuffle(currentOptions);
            }

            btnOption1.setText(currentOptions.get(0));
            btnOption2.setText(currentOptions.get(1));
            btnOption3.setText(currentOptions.get(2));

            if (isPlayerTurn) {
                setOptionsEnabled(true);
                resetTimer();
                startQuestionTimer();
            } else {
                setOptionsEnabled(false);
                new Handler().postDelayed(this::startBotTurn, 1000);
            }

            resetButtonColors();
            resetAllCircles();
            resetAllRectangles();
        });
    }

    private Question getFallbackQuestion() {
        return new Question(
                "Qual é a capital de Portugal?",
                "A) Porto",
                "B) Lisboa",
                "C) Coimbra",
                "B"
        );
    }

    private void startPlayerTurn() {
        if (checkGameEnd()) return;

        isPlayerTurn = true;
        currentPhase = GamePhase.QUESTION;
        showQuizPhase();

        currentQuestion = questionManager.getRandomQuestion();
        if (currentQuestion == null) {
            currentQuestion = getFallbackQuestion();
            Log.e("GameActivity", "QuestionManager retornou null em startPlayerTurn");
        }

        currentOptions = questionManager.getShuffledOptions(currentQuestion);

        runOnUiThread(() -> {
            if (currentQuestion == null) currentQuestion = getFallbackQuestion();

            tvQuestion.setText(currentQuestion.getQuestion());
            tvQuestion.setBackgroundColor(Color.WHITE);

            if (currentOptions == null || currentOptions.size() < 3) {
                currentOptions = new ArrayList<>();
                currentOptions.add(currentQuestion.getOptionA());
                currentOptions.add(currentQuestion.getOptionB());
                currentOptions.add(currentQuestion.getOptionC());
                Collections.shuffle(currentOptions);
            }

            btnOption1.setText(currentOptions.get(0));
            btnOption2.setText(currentOptions.get(1));
            btnOption3.setText(currentOptions.get(2));

            setOptionsEnabled(true);
            resetButtonColors();
            resetTimer();
            startQuestionTimer();
        });
    }

    // =========================================================================
    // SISTEMA DE PERGUNTAS E RESPOSTAS
    // =========================================================================

    void setOptionsEnabled(boolean enabled) {
        runOnUiThread(() -> {
            btnOption1.setEnabled(enabled);
            btnOption2.setEnabled(enabled);
            btnOption3.setEnabled(enabled);

            if (!enabled) {
                btnOption1.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                btnOption2.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                btnOption3.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            } else {
                resetButtonColors();
            }
        });
    }

    private void resetButtonColors() {
        runOnUiThread(() -> {
            btnOption1.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_neutral));
            btnOption2.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_neutral));
            btnOption3.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_neutral));
        });
    }

    public void onOptionClick(View view) {
        if (currentPhase != GamePhase.QUESTION || !isPlayerTurn || isProcessingClick) return;

        isProcessingClick = true;
        stopTimer();

        Button clickedButton = (Button) view;
        String selectedAnswer = clickedButton.getText().toString();
        boolean answeredCorrectly = isAnswerCorrect(selectedAnswer);

        if (answeredCorrectly) {
            clickedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.success_green));
            showQuestionResult("✅ Resposta certa!", "#4CAF50");

            new Handler().postDelayed(() -> {
                startShootPhase();
                isProcessingClick = false;
            }, 1500);
        } else {
            clickedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.error_dark_red));
            highlightCorrectAnswer();
            showQuestionResult("❌ Resposta errada", "#DC5E5E");

            new Handler().postDelayed(() -> {
                updatePlayerScoreCircle(false);
                playerShots++;

                if (checkGameEnd()) {
                    isProcessingClick = false;
                    return;
                }

                showBotTurnTransition(false);
                isProcessingClick = false;
            }, 1500);
        }

        setOptionsEnabled(false);
    }

    private boolean isAnswerCorrect(String selectedAnswer) {
        if (currentQuestion == null) return false;
        String selectedLetter = selectedAnswer.substring(0, 1);
        String correctAnswer = currentQuestion.getCorrectAnswer();
        return selectedLetter.equals(correctAnswer);
    }

    private void highlightCorrectAnswer() {
        runOnUiThread(() -> {
            String correctAnswer = currentQuestion.getCorrectAnswer();
            if (currentOptions.get(0).contains(correctAnswer)) {
                btnOption1.setBackgroundColor(ContextCompat.getColor(this, R.color.correct_gold));
            } else if (currentOptions.get(1).contains(correctAnswer)) {
                btnOption2.setBackgroundColor(ContextCompat.getColor(this, R.color.correct_gold));
            } else if (currentOptions.get(2).contains(correctAnswer)) {
                btnOption3.setBackgroundColor(ContextCompat.getColor(this, R.color.correct_gold));
            }
        });
    }

    private void startBotTurn() {
        if (checkGameEnd()) return;

        isPlayerTurn = false;
        boolean botAnsweredCorrectly = simulateBotAnswer();

        if (botAnsweredCorrectly) {
            showQuestionResult(botPlayer + " acertou!", "#FF9800");
            new Handler().postDelayed(() -> startDefendPhase(), 1500);
        } else {
            showQuestionResult(botPlayer + " errou!", "#2196F3");
            new Handler().postDelayed(() -> {
                updateBotScoreCircle(false);
                botShots++;
                if (checkGameEnd()) return;
                showPlayerTurnTransition(false);
            }, 1500);
        }
    }

    private boolean simulateBotAnswer() {
        return Math.random() < 0.7;
    }

    private void startShootPhase() {
        currentPhase = GamePhase.SHOOT;
        showShootPhase();
    }

    private void startDefendPhase() {
        currentPhase = GamePhase.DEFEND;
        showDefendPhase();
    }

    // =========================================================================
    // CONTROLE DOS CÍRCULOS E RETÂNGULOS
    // =========================================================================

    private void setupGoalCircles() {
        for (int circleId : circleIds) {
            ImageView circle = findViewById(circleId);
            if (circle != null) {
                circle.setOnClickListener(v -> {
                    if (isProcessingClick) return;
                    isProcessingClick = true;
                    String areaTag = (String) v.getTag();
                    executePenaltyShot(areaTag);
                });
            }
        }
    }

    private void setupDefenseRectangles() {
        for (int rectId : rectangleIds) {
            View rectangle = findViewById(rectId);
            if (rectangle != null) {
                rectangle.setOnClickListener(v -> {
                    if (isProcessingClick) return;
                    isProcessingClick = true;
                    String areaTag = (String) v.getTag();
                    executeDefense(areaTag);
                });
            }
        }
    }

    // =========================================================================
    // LÓGICA DOS PENÁLTIS
    // =========================================================================

    private void executePenaltyShot(String areaTag) {
        if (currentPhase != GamePhase.SHOOT) {
            isProcessingClick = false;
            return;
        }

        currentPhase = GamePhase.RESULT;
        boolean isGreenArea = currentGreenAreas.contains(areaTag);
        showSingleRedCircle(areaTag, isGreenArea);

        new Handler().postDelayed(() -> {
            if (isGreenArea) {
                playerScore++;
                updatePlayerScoreCircle(true);
                showQuestionResult("⚽ GOLO! " + selectedPlayer + " marcou!", "#4CAF50");
            } else {
                updatePlayerScoreCircle(false);
                showQuestionResult("❌ " + selectedPlayer + " falhou!", "#F44336");
            }

            playerShots++;

            if (checkSuddenDeathActivation()) {
                startSuddenDeath();
                isProcessingClick = false;
                return;
            }

            if (checkGameEnd()) {
                isProcessingClick = false;
                return;
            }

            showBotTurnTransition(isGreenArea);
            isProcessingClick = false;
        }, 2000);
    }

    private void executeDefense(String defendedArea) {
        if (currentPhase != GamePhase.DEFEND) {
            isProcessingClick = false;
            return;
        }

        currentPhase = GamePhase.RESULT;
        String botShotArea = getRandomRectangleArea();
        boolean defendedSuccessfully = isDefenseSuccessful(defendedArea, botShotArea);
        showDefenseResult(defendedArea, botShotArea, defendedSuccessfully);

        new Handler().postDelayed(() -> {
            if (defendedSuccessfully) {
                showQuestionResult("🧤 Defendeu! Parou o " + botPlayer + "!", "#2196F3");
                updateBotScoreCircle(false);
            } else {
                botScore++;
                updateBotScoreCircle(true);
                showQuestionResult("⚽ Golo do " + botPlayer + "!", "#F44336");
            }

            botShots++;

            if (checkSuddenDeathActivation()) {
                startSuddenDeath();
                isProcessingClick = false;
                return;
            }

            if (checkGameEnd()) {
                isProcessingClick = false;
                return;
            }

            showPlayerTurnTransition(!defendedSuccessfully);
            isProcessingClick = false;
        }, 2000);
    }

    private void showSingleRedCircle(String chosenArea, boolean isGreenArea) {
        resetAllCircles();
        int chosenCircleId = getCircleIdFromTag(chosenArea);
        if (chosenCircleId != -1) {
            ImageView chosenCircle = findViewById(chosenCircleId);
            if (chosenCircle != null) {
                if (isGreenArea) {
                    chosenCircle.setImageResource(R.drawable.goal_circle_chosen_green);
                } else {
                    chosenCircle.setImageResource(R.drawable.goal_circle_chosen_red);
                }
            }
        }
    }

    private boolean checkSuddenDeathActivation() {
        boolean bothCompletedRegularPenalties = playerShots >= REGULAR_PENALTIES && botShots >= REGULAR_PENALTIES;
        boolean isTied = playerScore == botScore;
        boolean notInSuddenDeathYet = !isSuddenDeath;
        return bothCompletedRegularPenalties && isTied && notInSuddenDeathYet;
    }

    private void showDefenseResult(String defendedArea, String shotArea, boolean defended) {
        resetAllRectangles();

        int shotRectId = getRectangleIdFromTag(shotArea);
        if (shotRectId != -1) {
            View shotRect = findViewById(shotRectId);
            if (shotRect != null) shotRect.setBackgroundResource(R.drawable.defense_red);
        }

        int defendRectId = getRectangleIdFromTag(defendedArea);
        if (defendRectId != -1) {
            View defendRect = findViewById(defendRectId);
            if (defendRect != null && defended) {
                defendRect.setBackgroundResource(R.drawable.defense_green);
            }
        }

        String defenseText = String.format("%s chutou: %s\nVocê defendeu: %s\n%s",
                botPlayer, getRectangleAreaName(shotArea), getRectangleAreaName(defendedArea),
                defended ? "✅ Defesa certa!" : "❌ Defesa errada!");
        showQuestionResult(defenseText, defended ? "#2196F3" : "#F44336");
    }

    // =========================================================================
    // LÓGICA DOS PENÁLTIS - MORTE SÚBITA
    // =========================================================================

    private boolean checkGameEnd() {
        if (isSuddenDeath) return checkSuddenDeathEnd();
        else return checkRegularPenaltiesEnd();
    }

    private boolean checkRegularPenaltiesEnd() {
        // CASO 1: Ambos já bateram todos os 5 penáltis
        if (playerShots >= REGULAR_PENALTIES && botShots >= REGULAR_PENALTIES) {
            if (playerScore == botScore) {
                startSuddenDeath();
                return true;
            } else {
                endGame();
                return true;
            }
        }

        // CASO 2: Player não pode mais alcançar o bot
        int remainingPlayerShots = REGULAR_PENALTIES - playerShots;
        int playerMaxPossibleScore = playerScore + remainingPlayerShots;
        if (playerMaxPossibleScore < botScore) {
            endGame();
            return true;
        }

        // CASO 3: Bot não pode mais alcançar o player
        int remainingBotShots = REGULAR_PENALTIES - botShots;
        int botMaxPossibleScore = botScore + remainingBotShots;
        if (botMaxPossibleScore < playerScore) {
            endGame();
            return true;
        }

        return false;
    }

    private boolean checkSuddenDeathEnd() {
        if (playerShots == botShots && playerShots > 0) {
            if (playerScore != botScore) {
                endGame();
                return true;
            }
        }
        return false;
    }

    private void startSuddenDeath() {
        isSuddenDeath = true;
        currentRound = 1;
        playerShots = 0;
        botShots = 0;
        resetScoreCircles();

        runOnUiThread(() -> {
            String suddenDeathMessage = String.format("EMPATE APÓS 5 PENÁLTIS!\n%s %d × %d %s\n\n⚡ MORTE SÚBITA ATIVADA! ⚡\nCada penálti é decisivo!",
                    selectedPlayer, playerScore, botScore, botPlayer);

            showQuestionResult(suddenDeathMessage, "#FF6B00");
            Toast.makeText(this, "MORTE SÚBITA ATIVADA! Cada penálti é decisivo!", Toast.LENGTH_LONG).show();

            new Handler().postDelayed(() -> {
                isPlayerTurn = userWonToss;
                startNewRound();
            }, 3000);
        });
    }

    // =========================================================================
    // SISTEMA DE PROBABILIDADES
    // =========================================================================

    private int calculateExactGreenCircles() {
        if (playerPoints >= 1000) return 14;
        if (playerPoints >= 900) return 12;
        if (playerPoints >= 800) return 10;
        if (playerPoints >= 700) return 9;
        if (playerPoints >= 600) return 8;
        if (playerPoints >= 500) return 6;
        if (playerPoints >= 400) return 5;
        if (playerPoints >= 300) return 4;
        if (playerPoints >= 200) return 2;
        if (playerPoints >= 100) return 1;
        return 0;
    }

    private void generateGreenAreas(int greenCircles) {
        currentGreenAreas.clear();
        ArrayList<String> shuffledAreas = new ArrayList<>();
        for (String area : allAreas) shuffledAreas.add(area);
        Collections.shuffle(shuffledAreas);

        for (int i = 0; i < greenCircles && i < shuffledAreas.size(); i++) {
            currentGreenAreas.add(shuffledAreas.get(i));
        }
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    private String getRandomRectangleArea() {
        String[] areas = {"top_left", "top_right", "center", "bottom_left", "bottom_right"};
        return areas[(int) (Math.random() * areas.length)];
    }

    private boolean isDefenseSuccessful(String defendedArea, String shotArea) {
        return defendedArea.equals(shotArea);
    }

    private void resetAllRectangles() {
        for (int rectId : rectangleIds) {
            View rectangle = findViewById(rectId);
            if (rectangle != null) rectangle.setBackgroundResource(R.drawable.defense_normal);
        }
    }

    private void resetAllCircles() {
        for (int circleId : circleIds) {
            ImageView circle = findViewById(circleId);
            if (circle != null) circle.setImageResource(R.drawable.goal_circle);
        }
    }

    private int getRectangleIdFromTag(String tag) {
        switch (tag) {
            case "top_left": return R.id.rect_top_left;
            case "top_right": return R.id.rect_top_right;
            case "center": return R.id.rect_center;
            case "bottom_left": return R.id.rect_bottom_left;
            case "bottom_right": return R.id.rect_bottom_right;
            default: return -1;
        }
    }

    private int getCircleIdFromTag(String tag) {
        switch (tag) {
            case "top_left": return R.id.circle_1_1;
            case "top_left_center": return R.id.circle_1_2;
            case "top_center": return R.id.circle_1_3;
            case "top_right_center": return R.id.circle_1_4;
            case "top_right": return R.id.circle_1_5;
            case "middle_left": return R.id.circle_2_1;
            case "middle_center_left": return R.id.circle_2_2;
            case "middle_center": return R.id.circle_2_3;
            case "middle_center_right": return R.id.circle_2_4;
            case "middle_right": return R.id.circle_2_5;
            case "bottom_left": return R.id.circle_3_1;
            case "bottom_left_center": return R.id.circle_3_2;
            case "bottom_center": return R.id.circle_3_3;
            case "bottom_right_center": return R.id.circle_3_4;
            case "bottom_right": return R.id.circle_3_5;
            default: return -1;
        }
    }

    private String getRectangleAreaName(String area) {
        switch (area) {
            case "top_left": return "Canto Superior Esquerdo";
            case "top_right": return "Canto Superior Direito";
            case "center": return "Centro";
            case "bottom_left": return "Canto Inferior Esquerdo";
            case "bottom_right": return "Canto Inferior Direito";
            default: return "Área Desconhecida";
        }
    }

    private void showQuestionResult(String message, String color) {
        runOnUiThread(() -> {
            tvQuestion.setText(message);
            tvQuestion.setBackgroundColor(Color.parseColor(color));
        });
    }

    private void endGame() {
        runOnUiThread(() -> {
            hideAllPhases();
            View gameOverView = findViewById(R.id.gameOverPhase);
            gameOverView.setVisibility(View.VISIBLE);

            TextView tvGameOverTitle = gameOverView.findViewById(R.id.tvGameOverTitle);
            TextView tvGameOverResult = gameOverView.findViewById(R.id.tvGameOverResult);
            TextView tvGameOverScore = gameOverView.findViewById(R.id.tvGameOverScore);
            TextView tvGameOverMessage = gameOverView.findViewById(R.id.tvGameOverMessage);
            TextView tvGameOverIcon = gameOverView.findViewById(R.id.tvGameOverIcon);
            Button btnBackToMain = gameOverView.findViewById(R.id.btnBackToMain);

            boolean playerWon = playerScore > botScore;

            if (isSuddenDeath) {
                tvGameOverTitle.setText("MORTE SÚBITA");
                if (playerWon) {
                    tvGameOverResult.setText("VITÓRIA NA MORTE SÚBITA!");
                    tvGameOverResult.setTextColor(Color.parseColor("#FFD700"));
                    tvGameOverIcon.setText("⚡");
                    tvGameOverMessage.setText(selectedPlayer + " foi decisivo!\nGanhou na morte súbita!");
                } else {
                    tvGameOverResult.setText("DERROTA NA MORTE SÚBITA!");
                    tvGameOverResult.setTextColor(Color.parseColor("#FF5252"));
                    tvGameOverIcon.setText("💔");
                    tvGameOverMessage.setText(botPlayer + " foi mais forte!\nPerdeu na morte súbita!");
                }
            } else {
                if (playerWon) {
                    tvGameOverResult.setText("VOCÊ VENCEU!");
                    tvGameOverResult.setTextColor(Color.parseColor("#FFD700"));
                    tvGameOverIcon.setText("🏆");
                    tvGameOverMessage.setText(selectedPlayer + " foi decisivo!\nParabéns pela vitória!");
                } else if (playerScore < botScore) {
                    tvGameOverResult.setText("VOCÊ PERDEU!");
                    tvGameOverResult.setTextColor(Color.parseColor("#FF5252"));
                    tvGameOverIcon.setText("😞");
                    tvGameOverMessage.setText("Mais sorte na próxima!\n" + botPlayer + " foi melhor.");
                } else {
                    tvGameOverResult.setText("EMPATE!");
                    tvGameOverResult.setTextColor(Color.parseColor("#2196F3"));
                    tvGameOverIcon.setText("🤝");
                    tvGameOverMessage.setText("Foi um jogo equilibrado!\nMorte súbita!");
                }
            }

            tvGameOverScore.setText(playerScore + " × " + botScore);
            btnBackToMain.setOnClickListener(v -> finish());

            CardView cardBackButton = findViewById(R.id.cardBackButton);
            if (cardBackButton != null) cardBackButton.setVisibility(View.GONE);

            Log.d(TAG, "Jogo terminado: " + (playerWon ? "VITÓRIA" : "DERROTA") + " | Resultado: " + playerScore + "-" + botScore);
            stopTimer();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (questionManager != null) questionManager.close();
        if (progressManager != null) progressManager.close();
    }
}