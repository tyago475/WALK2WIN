package pt.ubi.pdm.quizpenaltis;

/** Números do jogo (dificuldade, tempos) num só sítio. */
public final class GameRules {
    private GameRules() {}

    /** Segundos para responder a uma pergunta. */
    public static final int QUESTION_SECONDS = 10;
    /** A partir de quantos segundos restantes o relógio fica laranja / vermelho. */
    public static final int TIMER_WARNING_SECONDS = 7;
    public static final int TIMER_DANGER_SECONDS = 3;

    /** Probabilidade do computador acertar na pergunta. */
    public static final double BOT_CORRECT_PROBABILITY = 0.7;

    // Pausas (milissegundos) entre fases, para o jogador conseguir ler.
    public static final long START_DELAY_MS = 500;
    public static final long BOT_THINK_DELAY_MS = 1000;
    public static final long ANSWER_FEEDBACK_MS = 1500;
    public static final long SHOT_RESULT_MS = 2000;
    public static final long TRANSITION_MS = 2000;
    public static final long SUDDEN_DEATH_BANNER_MS = 3000;

    /**
     * Quantas das 15 zonas da baliza ficam "verdes" (golo) conforme o custo do jogador.
     * Jogadores mais caros têm mais zonas verdes, por isso é mais fácil marcar.
     */
    public static int greenZonesForCost(int playerCost) {
        if (playerCost >= 1000) return 14;
        if (playerCost >= 900) return 12;
        if (playerCost >= 800) return 10;
        if (playerCost >= 700) return 9;
        if (playerCost >= 600) return 8;
        if (playerCost >= 500) return 6;
        if (playerCost >= 400) return 5;
        if (playerCost >= 300) return 4;
        if (playerCost >= 200) return 2;
        if (playerCost >= 100) return 1;
        return 0;
    }
}
