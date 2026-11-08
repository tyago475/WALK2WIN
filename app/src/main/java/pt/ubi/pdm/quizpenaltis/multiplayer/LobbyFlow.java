package pt.ubi.pdm.quizpenaltis.multiplayer;

/**
 * Decisões da sala de espera, sem Android: dado o estado da sala, diz o que mostrar e o que fazer a seguir.
 * Nada é cobrado antes de {@code status == "playing"}; a cobrança ({@link Action#CHARGE_AND_LAUNCH}) sai
 * exatamente UMA vez por instância, mesmo que o estado "playing" chegue várias vezes.
 * <p>Uma instância por sala; usar só na thread principal.
 */
public final class LobbyFlow {

    /** O que mostrar. */
    public enum Stage { WAITING_FRIEND, FRIEND_JOINED, STARTING, CLOSED }

    /** O que fazer. */
    public enum Action {
        NONE,
        /** Há saldo: escrever {@code ready}. */
        SET_READY,
        /** Anfitrião, os dois prontos: {@code startMatch}. */
        START_MATCH,
        /** {@code playing}: cobrar o jogador e abrir o jogo. */
        CHARGE_AND_LAUNCH,
        /** O saldo já não chega para o jogador escolhido: sair sem escrever {@code ready}. */
        CANNOT_PAY,
        /** O adversário deixou de dar sinal de vida na sala de espera. */
        OPPONENT_GONE,
        /** A sala foi apagada ou terminou. */
        ROOM_CLOSED
    }

    private final Role me;
    private final int cost;

    private boolean seenRoom;
    private boolean readyDecided;
    private boolean startInFlight;
    private boolean launched;

    public LobbyFlow(Role me, int cost) {
        this.me = me;
        this.cost = cost;
    }

    public static Stage stageOf(RoomSnapshot s) {
        String status = s.status();
        if (status == null || RoomSnapshot.STATUS_FINISHED.equals(status)) return Stage.CLOSED;
        if (RoomSnapshot.STATUS_PLAYING.equals(status)) return Stage.STARTING;
        return s.host() != null && s.guest() != null ? Stage.FRIEND_JOINED : Stage.WAITING_FRIEND;
    }

    /**
     * @param balance   saldo atual lido do {@code StepCounter} (nunca copiado de outro ecrã)
     * @param serverNow hora do servidor em ms ({@link RoomRepository#serverNow()})
     */
    public Action decide(RoomSnapshot s, int balance, long serverNow) {
        if (launched) return Action.NONE;
        String status = s.status();
        if (status == null) return seenRoom ? Action.ROOM_CLOSED : Action.NONE;
        seenRoom = true;
        if (RoomSnapshot.STATUS_FINISHED.equals(status)) return Action.ROOM_CLOSED;
        if (RoomSnapshot.STATUS_PLAYING.equals(status)) {
            launched = true;
            return Action.CHARGE_AND_LAUNCH;
        }

        PlayerInfo own = s.player(me);
        PlayerInfo other = s.player(me.other());
        if (own == null || other == null) return Action.NONE;

        if (other.seen > 0 && ForfeitRule.hasForfeited(serverNow, other.seen)) return Action.OPPONENT_GONE;

        if (!readyDecided) {
            readyDecided = true;
            return balance >= cost ? Action.SET_READY : Action.CANNOT_PAY;
        }
        if (me == Role.HOST && own.ready && other.ready && !startInFlight) {
            startInFlight = true;
            return Action.START_MATCH;
        }
        return Action.NONE;
    }

    /** O startMatch foi recusado (p.ex. sinal de vida antigo): volta a tentar no próximo estado. */
    public void startRefused() {
        startInFlight = false;
    }

    /** Já se mandou cobrar e abrir o jogo. */
    public boolean launched() {
        return launched;
    }
}
