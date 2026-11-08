package pt.ubi.pdm.quizpenaltis.multiplayer;

/** Desistência por inatividade do adversário. */
public final class ForfeitRule {
    private ForfeitRule() {}

    public static boolean hasForfeited(long serverNowMs, long opponentSeenMs) {
        return serverNowMs - opponentSeenMs > MultiplayerRules.FORFEIT_MS;
    }
}
