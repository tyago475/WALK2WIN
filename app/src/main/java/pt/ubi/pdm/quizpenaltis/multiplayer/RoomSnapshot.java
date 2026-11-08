package pt.ubi.pdm.quizpenaltis.multiplayer;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Leitura tolerante de uma sala ({@code rooms/{CODE}}) vinda do Firebase (Map/List/Long/Boolean/String).
 * {@link #parse} nunca lança: o que faltar ou vier fora do formato/intervalo fica null (ou é ignorado).
 * Os dados vêm do adversário: ver os avisos em {@link #winner()} e {@link #first()}.
 */
public final class RoomSnapshot {

    /** Maior número de turno aceite (as regras aceitam 0..999). */
    static final int MAX_TURN = 999;
    static final int MAX_QUESTION = 9999;
    static final int MAX_SHOT_ZONE = 14;
    static final int MAX_DEFEND_ZONE = 4;

    public static final String STATUS_WAITING = "waiting";
    public static final String STATUS_PLAYING = "playing";
    public static final String STATUS_FINISHED = "finished";
    public static final String REASON_SCORE = "score";
    public static final String REASON_FORFEIT = "forfeit";
    public static final String HEADS = "heads";
    public static final String TAILS = "tails";

    private String status;
    private String hostUid;
    private String guestUid;
    private Integer version;
    private Long created;
    private Role winner;
    private String reason;
    private PlayerInfo host;
    private PlayerInfo guest;
    private String guestChoice;
    private Role first;
    private final TreeMap<Integer, TurnSnapshot> turns = new TreeMap<>();

    private RoomSnapshot() {}

    public static RoomSnapshot parse(Object firebaseValue) {
        RoomSnapshot s = new RoomSnapshot();
        try {
            Map<?, ?> room = asMap(firebaseValue);
            if (room == null) return s;
            s.readMeta(asMap(room.get("meta")));
            Map<?, ?> players = asMap(room.get("players"));
            if (players != null) {
                s.host = readPlayer(asMap(players.get(Role.HOST.key())));
                s.guest = readPlayer(asMap(players.get(Role.GUEST.key())));
            }
            Map<?, ?> toss = asMap(room.get("toss"));
            if (toss != null) {
                s.guestChoice = oneOf(toss.get("guestChoice"), HEADS, TAILS);
                s.first = role(toss.get("first"));
            }
            s.readTurns(room.get("turns"));
        } catch (RuntimeException ignored) {
            // Dados hostis que escaparam às verificações: fica o que já se leu.
        }
        return s;
    }

    private void readMeta(Map<?, ?> meta) {
        if (meta == null) return;
        status = oneOf(meta.get("status"), STATUS_WAITING, STATUS_PLAYING, STATUS_FINISHED);
        hostUid = string(meta.get("host"));
        guestUid = string(meta.get("guest"));
        Long v = integer(meta.get("v"));
        version = v != null && v >= 0 && v <= Integer.MAX_VALUE ? (int) (long) v : null;
        created = integer(meta.get("created"));
        winner = role(meta.get("winner"));
        reason = oneOf(meta.get("reason"), REASON_SCORE, REASON_FORFEIT);
    }

    private static PlayerInfo readPlayer(Map<?, ?> p) {
        if (p == null) return null;
        Integer cost = ranged(p.get("cost"), 0, Integer.MAX_VALUE);
        Long seen = integer(p.get("seen"));
        return new PlayerInfo(
                string(p.get("uid")),
                PlayerName.sanitize(string(p.get("name"))),
                string(p.get("player")),
                cost != null ? cost : 0,
                Boolean.TRUE.equals(p.get("ready")),
                seen != null && seen > 0 ? seen : 0L);
    }

    private void readTurns(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (int n = 0; n < list.size() && n <= MAX_TURN; n++) putTurn(n, list.get(n));
            return;
        }
        Map<?, ?> map = asMap(value);
        if (map == null) return;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            Integer n = turnNumber(e.getKey());
            if (n != null) putTurn(n, e.getValue());
        }
    }

    private void putTurn(int n, Object value) {
        Map<?, ?> t = asMap(value);
        if (t == null) return;
        Map<?, ?> shot = asMap(t.get("shot"));
        Map<?, ?> defend = asMap(t.get("defend"));
        turns.put(n, new TurnSnapshot(
                role(t.get("shooter")),
                ranged(t.get("q"), 0, MAX_QUESTION),
                ranged(t.get("answer"), -1, 2),
                shot != null ? ranged(shot.get("zone"), 0, MAX_SHOT_ZONE) : null,
                shot != null ? nonce(shot.get("nonce")) : null,
                defend != null ? ranged(defend.get("zone"), 0, MAX_DEFEND_ZONE) : null,
                defend != null ? nonce(defend.get("nonce")) : null));
    }

    /** Aceita só "0".."999" sem zeros à esquerda (como as regras). */
    private static Integer turnNumber(Object key) {
        if (!(key instanceof String)) return null;
        String k = (String) key;
        if (k.isEmpty() || k.length() > 3 || (k.length() > 1 && k.charAt(0) == '0')) return null;
        for (int i = 0; i < k.length(); i++) {
            if (k.charAt(i) < '0' || k.charAt(i) > '9') return null;
        }
        return Integer.parseInt(k);
    }

    // ---- conversões tolerantes ----

    private static Map<?, ?> asMap(Object o) {
        return o instanceof Map ? (Map<?, ?>) o : null;
    }

    private static String string(Object o) {
        return o instanceof String ? (String) o : null;
    }

    private static String oneOf(Object o, String... allowed) {
        for (String a : allowed) {
            if (a.equals(o)) return a;
        }
        return null;
    }

    private static Role role(Object o) {
        return o instanceof String ? Role.fromKey((String) o) : null;
    }

    /** Número inteiro (Long/Integer, ou Double sem parte decimal); null caso contrário. */
    private static Long integer(Object o) {
        if (o instanceof Long || o instanceof Integer || o instanceof Short || o instanceof Byte) {
            return ((Number) o).longValue();
        }
        if (o instanceof Double || o instanceof Float) {
            double d = ((Number) o).doubleValue();
            if (d == Math.rint(d) && Math.abs(d) <= Nonce.MAX) return (long) d;
        }
        return null;
    }

    private static Integer ranged(Object o, int min, int max) {
        Long v = integer(o);
        return v != null && v >= min && v <= max ? (int) (long) v : null;
    }

    private static Long nonce(Object o) {
        Long v = integer(o);
        return v != null && Nonce.isValid(v) ? v : null;
    }

    // ---- getters ----

    /** "waiting" | "playing" | "finished", ou null (sala não existe / apagada / inválida). */
    public String status() { return status; }
    /** uid do anfitrião em {@code meta.host}. */
    public String hostUid() { return hostUid; }
    /** uid do convidado em {@code meta.guest} (null enquanto ninguém entrou). */
    public String guestUid() { return guestUid; }
    /** {@code meta.v} (versão do protocolo). */
    public Integer version() { return version; }
    public Long created() { return created; }

    /**
     * Vencedor declarado em {@code meta.winner}. Com {@link #reason()} == "score" NÃO é confiável
     * (as regras não contam golos): cada telemóvel recalcula o marcador a partir dos turnos.
     * Com "forfeit" as regras verificaram o sinal de vida do adversário na hora do servidor.
     */
    public Role winner() { return winner; }
    /** "score" | "forfeit" | null. */
    public String reason() { return reason; }

    public PlayerInfo host() { return host; }
    public PlayerInfo guest() { return guest; }
    public PlayerInfo player(Role r) { return r == Role.HOST ? host : r == Role.GUEST ? guest : null; }

    /** "heads" | "tails" | null (escolha do convidado). */
    public String guestChoice() { return guestChoice; }

    /**
     * Quem remata primeiro ({@code toss.first}), escrito pelo anfitrião. NÃO é confiável: o anfitrião
     * pode escrever o que quiser; o convidado não consegue verificar o lançamento da moeda.
     */
    public Role first() { return first; }

    /** O turno {@code n}, ou null se ainda não foi aberto (ou não é válido). */
    public TurnSnapshot turn(int n) { return turns.get(n); }

    /** Maior número de turno aberto, ou -1 se não houver nenhum. */
    public int highestTurn() { return turns.isEmpty() ? -1 : turns.lastKey(); }
}
