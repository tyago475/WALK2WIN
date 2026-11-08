package pt.ubi.pdm.quizpenaltis.multiplayer;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import pt.ubi.pdm.quizpenaltis.multiplayer.LobbyFlow.Action;
import pt.ubi.pdm.quizpenaltis.multiplayer.LobbyFlow.Stage;

public class LobbyFlowTest {

    private static final long NOW = 1_000_000L;

    private static Map<String, Object> player(boolean ready, long seen) {
        Map<String, Object> p = new HashMap<>();
        p.put("uid", "u");
        p.put("name", "Ana");
        p.put("player", "Messi");
        p.put("cost", 900L);
        p.put("ready", ready);
        p.put("seen", seen);
        return p;
    }

    private static RoomSnapshot room(String status, Map<String, Object> host, Map<String, Object> guest) {
        Map<String, Object> room = new HashMap<>();
        if (status != null) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("status", status);
            meta.put("host", "h");
            meta.put("v", 1L);
            room.put("meta", meta);
        }
        Map<String, Object> players = new HashMap<>();
        if (host != null) players.put("host", host);
        if (guest != null) players.put("guest", guest);
        room.put("players", players);
        return RoomSnapshot.parse(room);
    }

    @Test
    public void stages() {
        assertEquals(Stage.WAITING_FRIEND, LobbyFlow.stageOf(room("waiting", player(false, NOW), null)));
        assertEquals(Stage.FRIEND_JOINED,
                LobbyFlow.stageOf(room("waiting", player(false, NOW), player(false, NOW))));
        assertEquals(Stage.STARTING, LobbyFlow.stageOf(room("playing", player(true, NOW), player(true, NOW))));
        assertEquals(Stage.CLOSED, LobbyFlow.stageOf(room(null, null, null)));
    }

    @Test
    public void aloneDoesNothing() {
        LobbyFlow f = new LobbyFlow(Role.HOST, 900);
        assertEquals(Action.NONE, f.decide(room("waiting", player(false, NOW), null), 1000, NOW));
    }

    @Test
    public void readyOnlyWithBalanceAndOnlyOnce() {
        LobbyFlow f = new LobbyFlow(Role.GUEST, 900);
        RoomSnapshot both = room("waiting", player(false, NOW), player(false, NOW));
        assertEquals(Action.SET_READY, f.decide(both, 900, NOW));
        assertEquals(Action.NONE, f.decide(both, 900, NOW));
    }

    @Test
    public void cannotPayDoesNotSetReady() {
        LobbyFlow f = new LobbyFlow(Role.GUEST, 900);
        RoomSnapshot both = room("waiting", player(false, NOW), player(false, NOW));
        assertEquals(Action.CANNOT_PAY, f.decide(both, 899, NOW));
        assertEquals(Action.NONE, f.decide(both, 1000, NOW));
    }

    @Test
    public void hostStartsOnlyWhenBothReady() {
        LobbyFlow f = new LobbyFlow(Role.HOST, 900);
        assertEquals(Action.SET_READY,
                f.decide(room("waiting", player(false, NOW), player(false, NOW)), 900, NOW));
        assertEquals(Action.NONE,
                f.decide(room("waiting", player(true, NOW), player(false, NOW)), 900, NOW));
        RoomSnapshot ready = room("waiting", player(true, NOW), player(true, NOW));
        assertEquals(Action.START_MATCH, f.decide(ready, 900, NOW));
        assertEquals(Action.NONE, f.decide(ready, 900, NOW));
        f.startRefused();
        assertEquals(Action.START_MATCH, f.decide(ready, 900, NOW));
    }

    @Test
    public void guestNeverStarts() {
        LobbyFlow f = new LobbyFlow(Role.GUEST, 900);
        f.decide(room("waiting", player(false, NOW), player(false, NOW)), 900, NOW);
        assertEquals(Action.NONE, f.decide(room("waiting", player(true, NOW), player(true, NOW)), 900, NOW));
    }

    @Test
    public void chargesExactlyOnceWhenPlaying() {
        LobbyFlow f = new LobbyFlow(Role.GUEST, 900);
        RoomSnapshot playing = room("playing", player(true, NOW), player(true, NOW));
        assertEquals(Action.CHARGE_AND_LAUNCH, f.decide(playing, 900, NOW));
        assertEquals(Action.NONE, f.decide(playing, 900, NOW));
        assertEquals(Action.NONE, f.decide(playing, 900, NOW));
    }

    @Test
    public void staleOpponentIsGone() {
        LobbyFlow f = new LobbyFlow(Role.HOST, 900);
        long stale = NOW - MultiplayerRules.FORFEIT_MS - 1;
        assertEquals(Action.OPPONENT_GONE,
                f.decide(room("waiting", player(false, NOW), player(false, stale)), 900, NOW));
    }

    @Test
    public void roomClosedOnlyAfterSeen() {
        LobbyFlow f = new LobbyFlow(Role.GUEST, 900);
        assertEquals(Action.NONE, f.decide(room(null, null, null), 900, NOW));
        f.decide(room("waiting", player(false, NOW), player(false, NOW)), 900, NOW);
        assertEquals(Action.ROOM_CLOSED, f.decide(room(null, null, null), 900, NOW));
    }

    @Test
    public void finishedClosesTheLobby() {
        LobbyFlow f = new LobbyFlow(Role.GUEST, 900);
        assertEquals(Action.ROOM_CLOSED, f.decide(room("finished", player(true, NOW), player(true, NOW)), 900, NOW));
    }
}
