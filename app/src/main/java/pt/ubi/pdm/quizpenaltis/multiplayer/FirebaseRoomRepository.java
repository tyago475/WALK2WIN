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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.ubi.pdm.quizpenaltis.util.Callback;

/**
 * {@link RoomRepository} sobre a Realtime Database. Fina de propósito (não tem testes unitários):
 * a leitura/validação está em {@link RoomSnapshot} e as permissões em {@code firebase/database.rules.json}.
 * Todos os callbacks do Firebase chegam na thread principal.
 */
public final class FirebaseRoomRepository implements RoomRepository {

    private static final String TAG = "RoomRepository";
    private static final int CREATE_TRIES = 5;

    private final DatabaseReference rooms;
    private final SecureRandom random = new SecureRandom();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile long serverOffsetMs;

    /** Sala de onde o anfitrião ainda está à espera (tem onDisconnect().removeValue() registado). */
    private DatabaseReference waitingRoom;

    private Runnable heartbeatTask;

    // ---- escuta (uma por caminho; ver listen) ----
    private String listenCode;
    private Listener listener;
    private int session;
    /** Já se viu a sala existir nesta escuta (para distinguir "apagada" de "sem permissão"). */
    private boolean roomSeen;
    private final Map<String, Object> merged = new HashMap<>();
    private final Set<String> attachedPaths = new HashSet<>();
    private final List<Attached> attached = new ArrayList<>();

    private static final class Attached {
        final DatabaseReference ref;
        final ValueEventListener l;
        Attached(DatabaseReference ref, ValueEventListener l) { this.ref = ref; this.l = l; }
    }

    public FirebaseRoomRepository() {
        FirebaseDatabase db = FirebaseConfig.database();
        rooms = db.getReference("rooms");
        db.getReference(".info/serverTimeOffset").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                Object v = s.getValue();
                if (v instanceof Number) serverOffsetMs = ((Number) v).longValue();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        });
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

    /** Escreve o remate/defesa e só depois de aceite passa a escutar as duas jogadas do turno. */
    private void commit(String code, int n, String field, int zone, long nonce) {
        Map<String, Object> v = new HashMap<>();
        v.put("zone", zone);
        v.put("nonce", nonce);
        turnRef(code, n).child(field).setValue(v, (error, r) -> {
            if (error != null) {
                report(field + " " + n + ": " + error.getMessage());
                return;
            }
            if (code.equals(listenCode)) {
                attach("turns/" + n + "/shot");
                attach("turns/" + n + "/defend");
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
        room(code).child("players").child(me.key()).child("seen").setValue(ServerValue.TIMESTAMP);
    }

    @Override
    public void startHeartbeat(String code, Role me) {
        stopHeartbeat();
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
    }

    @Override
    public long serverNow() {
        return System.currentTimeMillis() + serverOffsetMs;
    }

    // ---- escuta ----

    /**
     * As regras não deixam ler a sala nem "turns" de uma vez. Escuta-se cada folha: meta, players, toss e,
     * por turno, shooter/q/answer (o turno n+1 começa a ser escutado quando o n abre). shot/defend só depois
     * de escrever a própria jogada ({@link #commit}).
     */
    @Override
    public void listen(String code, Listener l) {
        stopListening();
        listenCode = code;
        listener = l;
        attach("meta");
        attach("players");
        attach("toss");
        attach("turns/0/shooter");
    }

    @Override
    public void stopListening() {
        for (Attached a : attached) a.ref.removeEventListener(a.l);
        attached.clear();
        attachedPaths.clear();
        merged.clear();
        roomSeen = false;
        listenCode = null;
        listener = null;
        session++;
    }

    private void attach(String path) {
        if (listenCode == null || !attachedPaths.add(path)) return;
        final int mySession = session;
        DatabaseReference ref = room(listenCode).child(path);
        ValueEventListener l = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                if (mySession != session) return;
                Object value = s.getValue();
                put(path, value);
                if ("meta".equals(path) && value != null) roomSeen = true;
                if (value != null && path.endsWith("/shooter")) onTurnOpened(turnOf(path));
                if (listener != null) listener.onRoom(RoomSnapshot.parse(merged));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                if (mySession != session) return;
                // Sala apagada: as outras escutas perdem a permissão; o onRoom com status null já avisou.
                if (roomSeen && merged.get("meta") == null && !"meta".equals(path)) return;
                report("escuta " + path + ": " + e.getMessage());
            }
        };
        ref.addValueEventListener(l);
        attached.add(new Attached(ref, l));
    }

    private void onTurnOpened(int n) {
        attach("turns/" + n + "/q");
        attach("turns/" + n + "/answer");
        if (n < RoomSnapshot.MAX_TURN) attach("turns/" + (n + 1) + "/shooter");
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
