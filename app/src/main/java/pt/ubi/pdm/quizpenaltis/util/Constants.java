package pt.ubi.pdm.quizpenaltis.util;

/**
 * Valores partilhados pela app. Mudar um número aqui muda-o em todo o lado.
 */
public final class Constants {
    private Constants() {}

    // ----- Passos e pontos -----
    /** Meta diária de passos (barra de progresso, notificação e gráfico). */
    public static final int DAILY_STEP_GOAL = 10000;
    /** Quantos passos valem 1 ponto. */
    public static final int STEPS_PER_POINT = 10;
    /** Passos adicionados pelo botão de teste. */
    public static final int TEST_STEPS_INCREMENT = 1000;

    // ----- Chaves de Intent -----
    public static final String EXTRA_SELECTED_PLAYER = "SELECTED_PLAYER";
    public static final String EXTRA_PLAYER_COST = "PLAYER_COST";

    // ----- Modo de jogo (um jogador contra o bot, ou dois jogadores online) -----
    public static final String EXTRA_MODE = "GAME_MODE";
    public static final String MODE_SINGLE = "single";
    public static final String MODE_MULTI = "multi";
    /** Código da sala (só no modo múltiplo). */
    public static final String EXTRA_ROOM_CODE = "ROOM_CODE";
    /** {@code Role.key()} do jogador neste telemóvel: "host" ou "guest" (só no modo múltiplo). */
    public static final String EXTRA_ROLE = "ROLE";

    /** Converte passos em pontos. É a única fórmula da app. */
    public static int stepsToPoints(int steps) {
        return steps / STEPS_PER_POINT;
    }
}
