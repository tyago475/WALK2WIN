package pt.ubi.pdm.quizpenaltis.multiplayer;

/** Constantes das regras do multiplayer (sem Android). */
public final class MultiplayerRules {
    private MultiplayerRules() {}

    public static final int PROTOCOL_VERSION = 1;
    /** Tempo sem sinal do adversário a partir do qual se considera que desistiu. */
    public static final long FORFEIT_MS = 30_000;
    /** Intervalo entre sinais de vida. */
    public static final long HEARTBEAT_MS = 5_000;
    /** Uma sala parada há mais tempo que isto pode ser reutilizada. */
    public static final long ROOM_REUSE_AFTER_MS = 2 * 60 * 60 * 1000;
    public static final int NAME_MAX_LENGTH = 12;
    public static final String DEFAULT_NAME = "Jogador";
}
