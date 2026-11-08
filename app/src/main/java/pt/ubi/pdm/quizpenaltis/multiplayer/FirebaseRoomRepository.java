package pt.ubi.pdm.quizpenaltis.multiplayer;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import pt.ubi.pdm.quizpenaltis.util.Callback;

/**
 * {@link RoomRepository} sobre a Realtime Database. Fina de propósito (não tem testes unitários):
 * a leitura/validação está em {@link RoomSnapshot} e as permissões em {@code firebase/database.rules.json}.
 * Usar só na thread principal (é onde chegam todos os callbacks do Firebase).
 */
public final class FirebaseRoomRepository implements RoomRepository {

    private static final String TAG = "RoomRepository";
    private static final int CREATE_TRIES = 5;

    private final DatabaseReference rooms;
    private final FirebaseDatabase db;
    private final SecureRandom random = new SecureRandom();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile long serverOffsetMs;
    /** Escuta de .info/serverTimeOffset: só enquanto há uma escuta da sala (não fica pendurada). */
    private ValueEventListener offsetListener;

    /** Sala de onde o anfitrião ainda está à espera (tem onDisconnect().removeValue() registado). */
    private DatabaseReference waitingRoom;

    private Runnable heartbeatTask;
    private String heartbeatCode;
    /** Já se avisou desta série de falhas do sinal de vida (avisa-se uma vez por série). */
    private boolean heartbeatFailing;

    // ---- escuta (uma por caminho; ver listen) ----
    private String listenCode;
    private Role listenRole;
    private Listener listener;
    private int session;
    /** Já se viu a sala existir nesta escuta (para distinguir "apagada" de "sem permissão"). */
    private boolean roomSeen;
    private final Map<String, Object> merged = new HashMap<>();
    private final Map<String, Attached> attached = new LinkedHashMap<>();
    /** Caminho da própria jogada (turns/n/shot|defend) -> caminho da jogada do adversário. */
    private final Map<String, String> opponentOfOwn = new HashMap<>();
    /** Caminhos de jogadas do adversário (só legíveis depois da própria). */
    private final Set<String> opponentPaths = new HashSet<>();
    /** Jogadas do adversário cuja jogada própria já foi aceite pelo servidor. */
    private final Set<String> ownCommitAcked = new HashSet<>();

    private static final class Attached {
        final DatabaseReference ref;
        final ValueEventListener l;
        Attached(DatabaseReference ref, ValueEventListener l) { this.ref = ref; this.l = l; }
    }

    public FirebaseRoomRepository() {
        db = FirebaseConfig.database();
        rooms = db.getReference("rooms");
    }

    private DatabaseReference offsetRef() {
        return db.getReference(".info/serverTimeOffset");
    }

    private void attachOffset() {
        if (offsetListener != null) return;
        offsetListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                Object v = s.getValue();
                if (v instanceof Number) serverOffsetMs = ((Number) v).longValue();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        };
        offsetRef().addValueEventListener(offsetListener);
    }

    private void detachOffset() {
        if (offsetListener != null) offsetRef().removeEventListener(offsetListener);
        offsetListener = null;
    }

    private DatabaseReference room(String code) {
        return rooms.child(code);
    }

    // ---- criar / entrar ----

    @Override
    public void createRoom(PlayerInfo me, Callback<String> code) {
        tryCreate(me, code, 1);
    }

    private void tryCreate(PlayerInfo me, Callback<String> result, int attempt) {
        String code = RoomCode.generate(random);
        Map<String, Object> meta = new HashMap<>();
        meta.put("host", me.uid);
        meta.put("status", RoomSnapshot.STATUS_WAITING);
        meta.put("v", MultiplayerRules.PROTOCOL_VERSION);
        meta.put("created", ServerValue.TIMESTAMP);
        Map<String, Object> players = new HashMap<>();
        players.put(Role.HOST.key(), playerRecord(me));
        Map<String, Object> value = new HashMap<>();
        value.put("meta", meta);
        value.put("players", players);

        // set na sala inteira: numa recriação (sala com mais de 2 h) não fica nada da antiga.
        DatabaseReference ref = room(code);
        ref.setValue(value, (error, r) -> {
            if (error == null) {
                ref.onDisconnect().removeValue();
                waitingRoom = ref;
                result.on(code);
            } else if (error.getCode() == DatabaseError.PERMISSION_DENIED && attempt < CREATE_TRIES) {
                // O mais provável é o código já estar ocupado: tenta outro.
                tryCreate(me, result, attempt + 1);
            } else {
                Log.w(TAG, "createRoom falhou: " + error.getMessage());
                result.on(null);
            }
        });
    }

    @Override
    public void joinRoom(String code, PlayerInfo me, Callback<JoinResult> result) {
        if (!RoomCode.isValid(code)) {
            result.on(JoinResult.NOT_FOUND);
            return;
        }
        room(code).child("meta").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                result.on(JoinResult.ERROR);
                return;
            }
            Map<String, Object> wrapper = new HashMap<>();
            wrapper.put("meta", task.getResult().getValue());
            RoomSnapshot s = RoomSnapshot.parse(wrapper);
            if (s.status() == null) {
                result.on(JoinResult.NOT_FOUND);
            } else if (s.version() == null || s.version() != MultiplayerRules.PROTOCOL_VERSION) {
                result.on(JoinResult.WRONG_VERSION);
            } else if (s.guestUid() != null || !RoomSnapshot.STATUS_WAITING.equals(s.status())) {
                result.on(JoinResult.FULL);
            } else {
                Map<String, Object> update = new HashMap<>();
                update.put("meta/guest", me.uid);
                update.put("players/" + Role.GUEST.key(), playerRecord(me));
                room(code).updateChildren(update, (error, r) -> {
                    if (error != null) Log.w(TAG, "joinRoom falhou: " + error.getMessage());
                    // Recusa típica: alguém entrou entretanto.
                    result.on(error == null ? JoinResult.OK
                            : error.getCode() == DatabaseError.PERMISSION_DENIED ? JoinResult.FULL
                            : JoinResult.ERROR);
                });
            }
        });
    }

    private static Map<String, Object> playerRecord(PlayerInfo me) {
        Map<String, Object> p = new HashMap<>();
        p.put("uid", me.uid);
        p.put("name", PlayerName.sanitize(me.name));
        p.put("player", me.player);
        p.put("cost", me.cost);
        p.put("ready", false);
        p.put("seen", ServerValue.TIMESTAMP);
        return p;
    }

    // ---- sala de espera ----

    @Override
    public void setReady(String code, Role me) {
        write(room(code).child("players").child(me.key()).child("ready"), true, "setReady");
    }

    @Override
    public void startMatch(String code) {
        startMatch(code, started -> { });
    }

    @Override
    public void startMatch(String code, Callback<Boolean> started) {
        room(code).child("players").get().addOnCompleteListener(task -> {
            Map<String, Object> wrapper = new HashMap<>();
            if (task.isSuccessful() && task.getResult() != null) {
                wrapper.put("players", task.getResult().getValue());
            }
            RoomSnapshot s = RoomSnapshot.parse(wrapper);
            if (s.host() == null || s.guest() == null || !s.host().ready || !s.guest().ready) {
                started.on(false);
                return;
            }
            DatabaseReference ref = room(code);
            ref.child("meta").child("status").setValue(RoomSnapshot.STATUS_PLAYING, (error, r) -> {
                if (error == null) {
                    // A partida começou: sair já não apaga a sala (quem fica ganha por desistência).
                    ref.onDisconnect().cancel();
                    if (ref.equals(waitingRoom)) waitingRoom = null;
                } else {
                    report("startMatch: " + error.getMessage());
                }
                started.on(error == null);
            });
        });
    }

    @Override
    public void writeToss(String code, Role writer, String value) {
        String field = writer == Role.GUEST ? "guestChoice" : "first";
        write(room(code).child("toss").child(field), value, "writeToss");
    }

    // ---- turnos ----

    @Override
    public void openTurn(String code, int n, Role shooter, int q) {
        Map<String, Object> turn = new HashMap<>();
        turn.put("shooter", shooter.key());
        turn.put("q", q);
        write(turnRef(code, n), turn, "openTurn");
    }

    @Override
    public void writeAnswer(String code, int turn, int optionIndex) {
        write(turnRef(code, turn).child("answer"), optionIndex, "writeAnswer");
    }

    @Override
    public void writeShot(String code, int n, int zone, long nonce) {
        commit(code, n, "shot", zone, nonce);
    }

    @Override
    public void writeDefend(String code, int n, int zone, long nonce) {
        commit(code, n, "defend", zone, nonce);
    }

    /**
     * Escreve o remate/defesa. A escuta da jogada do adversário não depende disto: abre quando a própria
     * jogada aparece (ver {@link #onTurnOpened}). Aqui só se volta a tentar (se essa escuta tiver sido
     * recusada por chegar ao servidor antes da escrita) e se avisa se ninguém está a escutar esta sala.
     */
    private void commit(String code, int n, String field, int zone, long nonce) {
        Map<String, Object> v = new HashMap<>();
        v.put("zone", zone);
        v.put("nonce", nonce);
        String opponentPath = "turns/" + n + "/" + ("shot".equals(field) ? "defend" : "shot");
        turnRef(code, n).child(field).setValue(v, (error, r) -> {
            if (error != null) {
                report(field + " " + n + ": " + error.getMessage());
            } else if (!code.equals(listenCode)) {
                report(field + " " + n + " escrito sem escuta ativa da sala " + code
                        + ": a jogada do adversario nao vai ser vista (chamar listen)");
            } else {
                ownCommitAcked.add(opponentPath);
                opponentPaths.add(opponentPath);
                attach(opponentPath);
            }
        });
    }

    private DatabaseReference turnRef(String code, int n) {
        return room(code).child("turns").child(String.valueOf(n));
    }

    // ---- fim ----

    @Override
    public void claimWin(String code, Role me, String reason) {
        Map<String, Object> update = new HashMap<>();
        update.put("winner", me.key());
        update.put("reason", reason);
        update.put("status", RoomSnapshot.STATUS_FINISHED);
        room(code).child("meta").updateChildren(update, (error, r) -> {
            if (error != null) report("claimWin: " + error.getMessage());
        });
    }

    @Override
    public void leave(String code, Role me) {
        stopHeartbeat();
        stopListening();
        if (me != Role.HOST) return;
        room(code).child("meta").child("status").get().addOnCompleteListener(task -> {
            Object status = task.isSuccessful() && task.getResult() != null
                    ? task.getResult().getValue() : null;
            if (RoomSnapshot.STATUS_WAITING.equals(status)) deleteRoom(code);
        });
    }

    @Override
    public void deleteRoom(String code) {
        if (code.equals(heartbeatCode)) stopHeartbeat();
        DatabaseReference ref = room(code);
        ref.onDisconnect().cancel();
        if (ref.equals(waitingRoom)) waitingRoom = null;
        ref.removeValue((error, r) -> {
            if (error != null) Log.w(TAG, "deleteRoom falhou: " + error.getMessage());
        });
    }

    // ---- sinal de vida ----

    @Override
    public void heartbeat(String code, Role me) {
        room(code).child("players").child(me.key()).child("seen").setValue(ServerValue.TIMESTAMP,
                (error, r) -> {
                    if (error == null) {
                        heartbeatFailing = false;
                    } else if (!heartbeatFailing) {
                        heartbeatFailing = true;
                        report("sinal de vida: " + error.getMessage());
                    }
                });
    }

    @Override
    public void startHeartbeat(String code, Role me) {
        stopHeartbeat();
        heartbeatCode = code;
        heartbeatTask = new Runnable() {
            @Override public void run() {
                heartbeat(code, me);
                handler.postDelayed(this, MultiplayerRules.HEARTBEAT_MS);
            }
        };
        heartbeatTask.run();
    }

    @Override
    public void stopHeartbeat() {
        if (heartbeatTask != null) handler.removeCallbacks(heartbeatTask);
        heartbeatTask = null;
        heartbeatCode = null;
        heartbeatFailing = false;
    }

    @Override
    public long serverNow() {
        return System.currentTimeMillis() + serverOffsetMs;
    }

    // ---- escuta ----

    /**
     * As regras não deixam ler a sala nem "turns" de uma vez. Escuta-se cada folha: meta, players, toss e,
     * por turno, shooter/q/answer (o turno n+1 começa a ser escutado quando o n abre) e a própria jogada;
     * a do adversário só quando a própria existir. Pode voltar a chamar-se a meio de um turno.
     */
    @Override
    public void listen(String code, Role me, Listener l) {
        stopListening();
        listenCode = code;
        listenRole = me;
        listener = l;
        attachOffset();
        attach("meta");
        attach("players");
        attach("toss");
        attach("turns/0/shooter");
    }

    @Override
    public void stopListening() {
        for (Attached a : attached.values()) a.ref.removeEventListener(a.l);
        attached.clear();
        opponentOfOwn.clear();
        opponentPaths.clear();
        ownCommitAcked.clear();
        merged.clear();
        roomSeen = false;
        listenCode = null;
        listenRole = null;
        listener = null;
        detachOffset();
        session++;
    }

    private void attach(String path) {
        if (listenCode == null || attached.containsKey(path)) return;
        final int mySession = session;
        final String code = listenCode;
        DatabaseReference ref = room(code).child(path);
        ValueEventListener l = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (mySession != session) return;
                Object value = s.getValue();
                put(path, value);
                if ("meta".equals(path)) {
                    if (value != null) roomSeen = true;
                    else if (roomSeen && code.equals(heartbeatCode)) stopHeartbeat(); // sala apagada
                }
                if (value instanceof String && path.endsWith("/shooter")) {
                    onTurnOpened(turnOf(path), Role.fromKey((String) value));
                }
                // A própria jogada existe: as regras já deixam ler a do adversário.
                String opponent = opponentOfOwn.get(path);
                if (value != null && opponent != null) attach(opponent);
                if (listener != null) listener.onRoom(RoomSnapshot.parse(merged));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                if (mySession != session) return;
                attached.remove(path);
                // Escuta da jogada do adversário recusada por chegar ao servidor antes da própria escrita:
                // o commit volta a abri-la quando a escrita for aceite.
                if (opponentPaths.contains(path) && !ownCommitAcked.contains(path)) return;
                // Sala apagada: as outras escutas perdem a permissão; o onRoom com status null já avisou.
                if (roomSeen && merged.get("meta") == null && !"meta".equals(path)) return;
                report("escuta " + path + ": " + e.getMessage());
            }
        };
        attached.put(path, new Attached(ref, l));
        ref.addValueEventListener(l);
    }

    private void onTurnOpened(int n, Role shooter) {
        String base = "turns/" + n + "/";
        attach(base + "q");
        attach(base + "answer");
        if (n < RoomSnapshot.MAX_TURN) attach("turns/" + (n + 1) + "/shooter");
        String own = TurnSnapshot.commitField(listenRole, shooter);
        String opponent = TurnSnapshot.opponentCommitField(listenRole, shooter);
        if (own == null) return;
        // A própria jogada lê-se sempre; a do adversário só depois de a própria existir.
        opponentOfOwn.put(base + own, base + opponent);
        opponentPaths.add(base + opponent);
        attach(base + own);
    }

    private static int turnOf(String shooterPath) {
        return Integer.parseInt(shooterPath.split("/")[1]);
    }

    /** Põe o valor no mapa juntado, em {@code a/b/c}; null apaga a folha. */
    @SuppressWarnings("unchecked")
    private void put(String path, Object value) {
        String[] parts = path.split("/");
        Map<String, Object> node = merged;
        for (int i = 0; i < parts.length - 1; i++) {
            Object child = node.get(parts[i]);
            if (!(child instanceof Map)) {
                child = new HashMap<String, Object>();
                node.put(parts[i], child);
            }
            node = (Map<String, Object>) child;
        }
        String leaf = parts[parts.length - 1];
        if (value == null) node.remove(leaf); else node.put(leaf, value);
    }

    // ---- escritas simples ----

    private void write(DatabaseReference ref, Object value, String what) {
        ref.setValue(value, (error, r) -> {
            if (error != null) report(what + ": " + error.getMessage());
        });
    }

    private void report(String message) {
        Log.w(TAG, message);
        if (listener != null) listener.onError(message);
    }
}
