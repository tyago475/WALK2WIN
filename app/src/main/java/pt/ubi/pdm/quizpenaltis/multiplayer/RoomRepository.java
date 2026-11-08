package pt.ubi.pdm.quizpenaltis.multiplayer;

import pt.ubi.pdm.quizpenaltis.util.Callback;

/**
 * Acesso à sala online ({@code rooms/{CODE}}), para o ecrã e o adversário remoto não dependerem do Firebase.
 * Cada escrita corresponde a uma só escrita atómica aceite pelas regras ({@code firebase/database.rules.json}).
 * Escritas recusadas são comunicadas a {@link Listener#onError} (se houver uma escuta ativa).
 */
public interface RoomRepository {

    enum JoinResult { OK, NOT_FOUND, FULL, WRONG_VERSION, ERROR }

    interface Listener {
        /** Estado atual da sala (o que se pode ler até agora). Sala apagada: {@code status() == null}. */
        void onRoom(RoomSnapshot s);
        void onError(String message);
    }

    /**
     * Cria uma sala nova com {@code meta} e {@code players/host} ({@code me.uid} tem de ser o uid da sessão).
     * Devolve o código, ou null se falhar. Enquanto espera, a sala é apagada se o anfitrião se desligar.
     */
    void createRoom(PlayerInfo me, Callback<String> code);

    /** Lê {@code meta} e só escreve se a sala existir, estiver à espera, sem convidado e com a mesma versão. */
    void joinRoom(String code, PlayerInfo me, Callback<JoinResult> result);

    /** {@code players/{me}/ready = true} (só na sala de espera). */
    void setReady(String code, Role me);

    /** Anfitrião: {@code meta.status = "playing"}, só se os dois estiverem prontos. */
    void startMatch(String code);

    /**
     * Como {@link #startMatch(String)}, com o resultado: false se algum não estiver pronto ou as regras
     * recusarem (p.ex. sinal de vida de algum dos dois com mais de 30 s).
     */
    void startMatch(String code, Callback<Boolean> started);

    /**
     * Convidado: {@code toss.guestChoice} ("heads"/"tails"); anfitrião: {@code toss.first} ("host"/"guest").
     * {@code toss.first} NÃO é confiável para o convidado (é o anfitrião que lança a moeda).
     */
    void writeToss(String code, Role writer, String value);

    /** Anfitrião: {@code turns/n = {shooter, q}}. O convidado deve verificar a ordem dos turnos. */
    void openTurn(String code, int n, Role shooter, int q);

    /** Atirador: {@code turns/n/answer} (0..2, ou -1 = tempo esgotado). NÃO verificada pelas regras. */
    void writeAnswer(String code, int turn, int optionIndex);

    /**
     * Atirador: {@code turns/n/shot = {zone, nonce}}. Só depois de esta escrita ser aceite se começa a escutar
     * {@code turns/n/defend} (as regras só o revelam depois do remate).
     */
    void writeShot(String code, int n, int zone, long nonce);

    /**
     * Defensor: {@code turns/n/defend = {zone, nonce}}. Só depois de esta escrita ser aceite se começa a escutar
     * {@code turns/n/shot}.
     */
    void writeDefend(String code, int n, int zone, long nonce);

    /**
     * {@code meta = {winner: me, reason, status: "finished"}} numa só escrita. {@code reason}: "score" ou
     * "forfeit". Uma vitória por "score" do adversário NÃO é confiável: recalcular o marcador.
     */
    void claimWin(String code, Role me, String reason);

    /** {@code players/{me}/seen = hora do servidor}. */
    void heartbeat(String code, Role me);

    /** Sinal de vida a cada {@link MultiplayerRules#HEARTBEAT_MS}, desde a sala de espera até ao fim. */
    void startHeartbeat(String code, Role me);

    void stopHeartbeat();

    /**
     * Escuta a sala. Como as regras dão leitura folha a folha, isto junta várias escutas (meta, players, toss,
     * e os campos de cada turno à medida que abrem). Substitui a escuta anterior.
     */
    void listen(String code, Listener l);

    void stopListening();

    /** Sai da sala: para o sinal de vida e a escuta; o anfitrião numa sala em espera apaga-a. */
    void leave(String code, Role me);

    /** Anfitrião: apaga a sala (no fim da partida). */
    void deleteRoom(String code);

    /** Hora do servidor em ms (relógio local + {@code .info/serverTimeOffset}). */
    long serverNow();
}
