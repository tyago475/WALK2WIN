package pt.ubi.pdm.quizpenaltis.multiplayer;

import pt.ubi.pdm.quizpenaltis.util.Callback;

/**
 * Acesso à sala online ({@code rooms/{CODE}}), para o ecrã e o adversário remoto não dependerem do Firebase.
 * Cada escrita corresponde a uma só escrita atómica aceite pelas regras ({@code firebase/database.rules.json}).
 * Escritas recusadas são comunicadas a {@link Listener#onError} (se houver uma escuta ativa).
 * <p>Usar só na thread principal; todos os callbacks ({@link Callback}, {@link Listener}) chegam na thread
 * principal.
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
     * Atirador: {@code turns/n/shot = {zone, nonce}}, com {@code nonce = }{@link Nonce#random()}.
     * A defesa do adversário só é escutada depois de o próprio remate existir (as regras só a revelam aí);
     * isto é feito pela escuta de {@link #listen}, que tem de estar ativa nesta sala (senão: onError).
     */
    void writeShot(String code, int n, int zone, long nonce);

    /**
     * Defensor: {@code turns/n/defend = {zone, nonce}}, com {@code nonce = }{@link Nonce#random()}.
     * O remate do adversário só é escutado depois de a própria defesa existir.
     */
    void writeDefend(String code, int n, int zone, long nonce);

    /**
     * {@code meta = {winner: me, reason, status: "finished"}} numa só escrita. {@code reason}: "score" ou
     * "forfeit". Uma vitória por "score" do adversário NÃO é confiável: recalcular o marcador.
     */
    void claimWin(String code, Role me, String reason);

    /** {@code players/{me}/seen = hora do servidor}. Falhas: um onError por série de falhas seguidas. */
    void heartbeat(String code, Role me);

    /**
     * Sinal de vida a cada {@link MultiplayerRules#HEARTBEAT_MS}, desde a sala de espera até ao fim.
     * Para sozinho quando a escuta vê a sala apagada e em {@link #deleteRoom}.
     */
    void startHeartbeat(String code, Role me);

    void stopHeartbeat();

    /**
     * Escuta a sala como {@code me}. Como as regras dão leitura folha a folha, isto junta várias escutas
     * (meta, players, toss, e os campos de cada turno à medida que abrem). Em cada turno escuta a própria
     * jogada ({@code shot} se {@code me} remata, {@code defend} se defende) e só quando ela existir passa a
     * escutar a do adversário. Substitui a escuta anterior; pode voltar a chamar-se a meio de um turno.
     * <p>Chamar só depois de {@link #createRoom}/{@link #joinRoom} terem dado certo: antes disso não se é membro
     * e as escutas de players/toss/turns são recusadas (e o Firebase cancela-as de vez).
     * Enquanto escuta, {@link #serverNow()} acompanha a hora do servidor.
     */
    void listen(String code, Role me, Listener l);

    void stopListening();

    /** Sai da sala: para o sinal de vida e a escuta; o anfitrião numa sala em espera apaga-a. */
    void leave(String code, Role me);

    /** Anfitrião: apaga a sala (no fim da partida). */
    void deleteRoom(String code);

    /**
     * Hora do servidor em ms (relógio local + {@code .info/serverTimeOffset}). O desvio só é atualizado
     * enquanto há uma escuta ativa ({@link #listen}); fora disso usa o último conhecido.
     */
    long serverNow();
}
